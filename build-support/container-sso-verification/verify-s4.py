"""Independent two-client SSO. Never run the transient helper beside the four-service runtime."""
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

spec=importlib.util.spec_from_file_location('s4_migration',RECIPE/'migrate-bff.py')
migration=importlib.util.module_from_spec(spec)
spec.loader.exec_module(migration)


def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--work-dir',type=Path,required=True)
    args=parser.parse_args()
    work,record,compose=load(args.work_dir)
    if record['phase']!='S4':parser.error('S4 work required')
    identity=preflight(work,record,compose)
    start=time.monotonic()
    write(work/'execution.json',dict(project=record['project'],started_with_empty_project=True,started_epoch=time.time(),image=identity,stage='S4'))
    result=dict(schema=1,stage='S4',status='RUNNING',source_sha=record['source_sha'],checkpoints=[],observations={},not_executed=['S5','refresh','backchannel logout','separate Origin/process isolation'])
    obs=result['observations'];profile=compose+['--profile','helper'];browser=None

    def run(args,data=None,timeout=60):
        if time.monotonic()-start>1500:raise RuntimeError('S4 budget exhausted; cleanup reserved')
        return command(work,args,data=data,timeout=timeout).stdout.strip()

    def db(role,query):return sql(work,compose,role,role,query).stdout.strip()

    def http(url):
        with urllib.request.urlopen(url,timeout=15) as response:return json.load(response)

    def passed(name,detail):
        result['checkpoints'].append(dict(id=name,status='PASS',details=detail));write(work/'results.json',result);print('PASS '+name,flush=True)

    try:
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
            browser=subprocess.Popen(['node',str(RECIPE/'browser-s4.mjs'),str(work)],stdout=log,stderr=subprocess.STDOUT,text=True)
            deadline=time.monotonic()+180
            while not(work/'await-independent.json').exists():
                if browser.poll() is not None:raise RuntimeError('Browser stopped: '+redact(work,(work/'browser-run.log').read_text())[-1000:])
                if time.monotonic()>deadline:raise RuntimeError('Browser journey timed out')
                time.sleep(0.3)
            independent=json.loads((work/'await-independent.json').read_text())
            for name in ['a_hash','b_hash']:assert re.fullmatch('[a-f0-9]{64}',independent[name])
            assert independent['a_hash']!=independent['b_hash']
            rows=db('koiki_bff',"SELECT client_id||':'||session_id_hash||':'||subject||':'||encryption_algorithm||':'||octet_length(encryption_iv)||':'||octet_length(encryption_tag) FROM bff_session ORDER BY client_id;").splitlines()
            assert len(rows)==2
            for rp,row in zip(['a','b'],rows):assert row==rp+':'+independent[rp+'_hash']+':d3210000-0000-4000-8000-000000000001:aes-256-gcm:12:16'
            obs['independent_sessions']=rows
            write(work/'independent-go.json',dict(db_client_bindings_pass=True))
            browser.wait(timeout=120)
            assert browser.returncode==0,'Browser failed: '+redact(work,(work/'browser-run.log').read_text())[-1000:]
        browser=None
        observed=json.loads((work/'browser.json').read_text());obs['browser']=observed
        passed('S4-V01','A browser login; B anonymous before prompt=none, no credential input, distinct A/B Cookies and encrypted DB client-bound Sessions')
        state_hash=observed['pending_a_state_hash'];assert re.fullmatch('[a-f0-9]{64}',state_hash)
        assert db('koiki_bff',"SELECT client_id||':'||(consumed_at IS NULL) FROM bff_auth_transaction WHERE state_hash='"+state_hash+"';")=='a:true'
        assert observed['cold_error']=='login_required'
        passed('S4-V02','Cold context prompt=none returns login_required without B Session; A pending transaction with copied binding rejected by B and remains unconsumed')
        assert db('koiki_bff',"SELECT client_id||':'||session_id_hash FROM bff_session;")=='b:'+observed['b_session_hash']
        assert all(observed['local_logout'].values())
        assert observed['sso_logout']['new_sso_error']=='login_required' and observed['sso_logout']['existing_b_jwt_accepted_at_observation']
        passed('S4-V03','A local logout preserves B/IdP; fresh A silent SSO succeeds; A SSO logout prevents new silent SSO while existing B Session/JWT remains usable at observation')
        expected={'b_before_sso':401,'a_cookie_as_b':401,'b_own_cookie':404,'a_csrf_on_b':403,'a_transaction_as_b':400,'a_after_local_logout':401,'b_after_a_local_logout':404,'unapproved_logout_return':400,'sso_logout_missing_csrf':403,'logout_wrong_state':400,'logout_wrong_client':400,'logout_state_as_auth':400,'idp_unregistered_return':400,'logout_return_replay':400,'b_jwt_after_sso_logout':404}
        assert observed['statuses']==expected
        assert db('koiki_app','SELECT count(*) FROM kkref_expense_request;')=='0'
        assert db('koiki_app',"SELECT count(*) FROM koiki_audit_event WHERE audit_type='BUSINESS';")=='0'
        obs['business_writes']=0
        obs['transaction_counts']=db('koiki_bff',"SELECT client_id||':'||count(*)||':'||count(consumed_at) FROM bff_auth_transaction GROUP BY client_id ORDER BY client_id;").splitlines()
        final_containers=json.loads(run(['docker','inspect',*run(compose+['ps','-q']).splitlines()]))
        assert len(final_containers)==4 and all(not c['State']['OOMKilled'] and c['RestartCount']==0 for c in final_containers)
        assert sum(c['HostConfig']['NanoCpus'] for c in final_containers)<=4_000_000_000
        assert sum(c['HostConfig']['Memory'] for c in final_containers)<=5120*1024**2
        obs['final_runtime']=dict(containers=4,restarts=0,oom=False,cpu_limit=4,memory_limit_mib=5120)
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
                if result['status']=='PASS_PENDING_CLEANUP':passed('S4-V04','A/B Cookie/DB binding; wrong logout state/client/kind/replay, fixed return input and IdP URI rejection; owned cleanup and generated secrets/realm deletion');result['status']='PASS'
            except Exception as error:result['status']='HOLD';result['cleanup_failure']=redact(work,str(error))
            result['elapsed_seconds']=round(time.monotonic()-start,3);write(work/'results.json',result)
    if result['status']!='PASS':raise SystemExit(1)


if __name__=='__main__':main()
