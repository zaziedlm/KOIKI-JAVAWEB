"""Client A BFF only. Never run the transient helper beside the four-service runtime."""
import argparse
import importlib.util
import json
from pathlib import Path
import re
import subprocess
import time
import urllib.request
from cleanup import cleanup
from preflight import RECIPE, command, load, preflight, redact, sql, write

spec=importlib.util.spec_from_file_location('s3_migration',RECIPE/'migrate-bff.py')
migration=importlib.util.module_from_spec(spec)
spec.loader.exec_module(migration)


def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--work-dir',type=Path,required=True)
    args=parser.parse_args()
    work,record,compose=load(args.work_dir)
    if record['phase']!='S3':parser.error('S3 work required')
    identity=preflight(work,record,compose)
    start=time.monotonic()
    write(work/'execution.json',dict(project=record['project'],started_with_empty_project=True,started_epoch=time.time(),image=identity,stage='S3'))
    result=dict(schema=1,stage='S3',status='RUNNING',source_sha=record['source_sha'],checkpoints=[],observations={},not_executed=['S4','S5','Client B','refresh'])
    obs=result['observations'];profile=compose+['--profile','helper'];browser=None

    def run(args,data=None,timeout=60):
        if time.monotonic()-start>2400:raise RuntimeError('S3 budget exhausted; cleanup reserved')
        return command(work,args,data=data,timeout=timeout).stdout.strip()

    def db(role,query):return sql(work,compose,role,role,query).stdout.strip()

    def http(url):
        with urllib.request.urlopen(url,timeout=15) as response:return json.load(response)

    def passed(name,detail):
        result['checkpoints'].append(dict(id=name,status='PASS',details=detail));write(work/'results.json',result);print('PASS '+name,flush=True)

    try:
        missing=command(work,compose+['-f',str(work/'recipe/compose-key-missing.yaml'),'run','--rm','-T','--no-deps','frontend'],timeout=45,check=False)
        assert missing.returncode==1 and 'Required BFF runtime secret is missing or invalid' in missing.stderr+missing.stdout
        obs['missing_key_negative']=dict(exit_code=missing.returncode,status='startup refused before server start')
        run(compose+['up','-d','--wait','--wait-timeout','240','db','keycloak','backend'],timeout=270)
        migration.migrate(work,compose)
        db('koiki_app',(work/'recipe/reference-seed.sql').read_text())
        assert db('koiki_app','SELECT count(*) FROM koiki_user;')=='4'
        run(profile+['up','-d','--no-deps','helper'],timeout=45)
        deadline=time.monotonic()+40
        while time.monotonic()<deadline:
            try:obs['internal_idp_probe']=http('http://127.0.0.1:13000/health');break
            except OSError:time.sleep(2)
        else:raise RuntimeError('Internal probe failed')
        assert obs['internal_idp_probe']['issuer']=='http://idp.localhost:18080/realms/koiki-d3'
        run(profile+['stop','helper'],timeout=30);run(profile+['rm','-f','helper'])
        assert not run(profile+['ps','-aq','helper'])
        obs['probe_exclusive_with_frontend']=True
        run(compose+['up','-d','--no-deps','--wait','--wait-timeout','100','frontend'],timeout=120)
        containers=json.loads(run(['docker','inspect',*run(compose+['ps','-q']).splitlines()]))
        assert len(containers)==4
        assert {c['Config']['Labels']['com.docker.compose.service'] for c in containers}=={'db','backend','keycloak','frontend'}
        front=next(c for c in containers if c['Config']['Labels']['com.docker.compose.service']=='frontend')
        assert front['Config']['User']=='10001:10001' and front['HostConfig']['ReadonlyRootfs']
        assert front['HostConfig']['Memory']==1024*1024**2 and front['HostConfig']['NanoCpus']==1_000_000_000
        assert all(not c['State']['OOMKilled'] for c in containers)
        obs['runtime_ids']={c['Config']['Labels']['com.docker.compose.service']:dict(id=c['Id'],image_id=c['Image']) for c in containers}
        assert http('http://127.0.0.1:13000/health/ready')=={'status':'UP'}
        with (work/'browser-run.log').open('w') as log:
            browser=subprocess.Popen(['node',str(RECIPE/'browser-s3.mjs'),str(work)],stdout=log,stderr=subprocess.STDOUT,text=True)
            deadline=time.monotonic()+180
            while not(work/'await-expiry.json').exists():
                if browser.poll() is not None:raise RuntimeError('Browser stopped before expiry coordination: '+redact(work,(work/'browser-run.log').read_text())[-1000:])
                if time.monotonic()>deadline:raise RuntimeError('Browser journey timed out')
                time.sleep(0.3)
            await_record=json.loads((work/'await-expiry.json').read_text());session_hash=await_record['session_id_hash']
            assert re.fullmatch('[a-f0-9]{64}',session_hash)
            obs['session_before_expiry']=db('koiki_bff',"SELECT version||':'||(last_seen_at>created_at)||':'||extract(epoch from (absolute_expires_at-created_at))::int||':'||encryption_algorithm||':'||octet_length(encryption_iv)||':'||octet_length(encryption_tag) FROM bff_session WHERE client_id='a' AND session_id_hash='"+session_hash+"';")
            pieces=obs['session_before_expiry'].split(':');assert int(pieces[0])>=1 and pieces[1:] == ['true','3600','aes-256-gcm','12','16']
            assert db('koiki_bff','SELECT count(*) FROM bff_session;')=='1'
            assert db('koiki_bff','SELECT count(*) FROM bff_auth_transaction WHERE consumed_at IS NOT NULL;')=='2'
            assert db('koiki_bff',"SELECT count(*) FROM bff_auth_transaction WHERE consumed_at IS NULL;")=='0'
            obs['auth_transaction_counts']=dict(consumed=2,unconsumed=0)
            expired=db('koiki_bff',"UPDATE bff_session SET created_at=current_timestamp-interval '2 hours',last_seen_at=current_timestamp-interval '1 hour',idle_expires_at=current_timestamp-interval '1 second' WHERE client_id='a' AND session_id_hash='"+session_hash+"' RETURNING 1;")
            assert expired=='1'
            write(work/'expiry-go.json',dict(test_owned_idle_expiry_set=True))
            browser.wait(timeout=40)
            assert browser.returncode==0,'Browser failed: '+redact(work,(work/'browser-run.log').read_text())[-1000:]
        browser=None
        observed=json.loads((work/'browser.json').read_text());obs['browser']=observed
        assert observed['statuses']['expired_session_api']==401
        assert db('koiki_bff','SELECT count(*) FROM bff_session;')=='0'
        passed('S3-V01','real login/JWKS, client-bound encrypted Session, HttpOnly Cookie, SSR identity, idle/absolute/token expiry controls and Session update')
        request_id=observed['request_id'];assert re.fullmatch('[a-f0-9-]{36}',request_id)
        assert db('koiki_app',"SELECT status||':'||claimed_amount||':'||applicant_user_id FROM kkref_expense_request;")=='SUBMITTED:1200:d3200000-0000-4000-8000-000000000001'
        assert db('koiki_app',"SELECT count(*)||':'||sum(amount) FROM kkref_expense_line;")=='1:1200'
        assert db('koiki_app',"SELECT count(*) FROM koiki_audit_event WHERE audit_type='BUSINESS' AND action='SUBMIT_EXPENSE' AND result='SUCCESS' AND actor_id='d3200000-0000-4000-8000-000000000001' AND resource_id='"+request_id+"';")=='1'
        passed('S3-V02','Browser create/detail/submit/reload; SUBMITTED:1200, owner, line total and exactly one submit Audit; no business POST retry')
        expected={'open_redirect_input':400,'login_missing_csrf':403,'state_mismatch':400,'callback_replay':400,'missing_csrf':403,'wrong_csrf':403,'wrong_origin':403,'content_type':415,'body_limit':413,'proxy_target':404,'rp_b':404,'logout_missing_csrf':403,'after_logout_api':401,'expired_session_api':401}
        assert observed['statuses']==expected
        passed('S3-V03','state mismatch/replay, CSRF/Origin/content/body/target/rp boundaries; local logout removes Session/Cookie; idle-expired Session deleted')
        assert obs['missing_key_negative']['exit_code']==1 and observed['checked_client_bundles']>0
        passed('S3-V04','SSR/RSC/API private no-store, HTML/RSC/client bundles no JWT or opaque Session ID; missing encryption key refuses startup')
        result['status']='PASS_PENDING_CLEANUP'
    except Exception as error:
        result['status']='FAIL';result['failure']=redact(work,type(error).__name__+': '+str(error));print(result['failure'],flush=True)
    finally:
        if browser and browser.poll() is None:
            browser.terminate()
            try:browser.wait(timeout=10)
            except subprocess.TimeoutExpired:browser.kill();browser.wait(timeout=5)
        try:
            raw=command(work,profile+['logs','--no-color'],timeout=20).stdout;safe=redact(work,raw)
            (work/'runtime-redacted.log').write_text(safe)
            if raw!=safe:result['status']='FAIL';result['failure']='Runtime credential/token exposure detected; saved log redacted'
        finally:
            try:
                cleanup(work,record,profile)
                if result['status']=='PASS_PENDING_CLEANUP':passed('S3-V05','unit boundary tests recorded; owned cleanup, generated secrets/realm deletion and redacted evidence complete');result['status']='PASS'
            except Exception as error:result['status']='HOLD';result['cleanup_failure']=redact(work,str(error))
            result['elapsed_seconds']=round(time.monotonic()-start,3);write(work/'results.json',result)
    if result['status']!='PASS':raise SystemExit(1)


if __name__=='__main__':main()
