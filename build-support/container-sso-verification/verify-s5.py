"""Finite S5 isolation/fault/restart driver. Own resources only, no POST retry."""
import argparse
import hashlib
import importlib.util
import json
from pathlib import Path
import re
import subprocess
import time
import urllib.request
import zipfile
from cleanup import cleanup
from preflight import RECIPE, ROOT,command,load,preflight,redact,sql,write


def module(name,file):
    spec=importlib.util.spec_from_file_location(name,RECIPE/file);result=importlib.util.module_from_spec(spec);spec.loader.exec_module(result);return result
migration=module('base_migration','migrate-bff.py');refresh_migration=module('refresh_migration','migrate-refresh.py')


def main():
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('--work-dir',type=Path,required=True);args=parser.parse_args()
    work,record,compose=load(args.work_dir)
    if record['phase']!='S5':parser.error('S5 work required')
    identity=preflight(work,record,compose);identity['frontend_image_id']=json.loads(command(work,['docker','image','inspect',record['frontend_image']]).stdout)[0]['Id'];started=time.monotonic();profile=compose+['--profile','helper'];browser=None;paused=False
    write(work/'execution.json',dict(project=record['project'],started_with_empty_project=True,started_epoch=time.time(),image=identity,stage='S5'))
    result=dict(schema=1,stage='S5',status='RUNNING',source_sha=record['source_sha'],checkpoints=[],observations={},not_executed=['Windows manual replay','production TLS/key rotation','backchannel logout','separate Origin/process SSO','inflight business write termination/drain'])
    obs=result['observations']
    def run(args,data=None,timeout=60):
        if time.monotonic()-started>3000:raise RuntimeError('S5 execution budget exhausted; cleanup reserved')
        return command(work,args,data=data,timeout=timeout).stdout.strip()
    def db(role,query):return sql(work,compose,role,role,query).stdout.strip()
    def http(path):
        try:
            with urllib.request.urlopen('http://127.0.0.1:13000'+path,timeout=25) as response:return response.status,json.load(response)
        except urllib.error.HTTPError as error:return error.code,json.load(error)
    def ready(timeout=100):
        end=time.monotonic()+timeout
        while time.monotonic()<end:
            try:
                if http('/health/ready')==(200,{'status':'UP'}):return
            except OSError:pass
            time.sleep(1)
        raise RuntimeError('BFF did not recover readiness')
    def wait(name,timeout=240):
        deadline=time.monotonic()+timeout
        while not(work/(name+'.json')).exists():
            if browser.poll() is not None:raise RuntimeError('Browser stopped at '+name+': '+redact(work,(work/'browser-run.log').read_text())[-1300:])
            if time.monotonic()>deadline:raise RuntimeError('Browser rendezvous timed out at '+name)
            time.sleep(0.1)
        return json.loads((work/(name+'.json')).read_text())
    def go(name):write(work/(name+'-go.json'),dict(proceed=True))
    def hashvalue(value):assert re.fullmatch('[a-f0-9]{64}',value);return value
    def force_expiry(value,rp='a'):
        assert rp in ['a','b'];sid=hashvalue(value);assert db('koiki_bff',"UPDATE bff_session SET access_token_expires_at=current_timestamp-interval '1 second' WHERE client_id='"+rp+"' AND session_id_hash='"+sid+"' RETURNING 1;")=='1'
    def session_record(value,rp='a'):
        assert rp in ['a','b'];return db('koiki_bff',"SELECT version||':'||md5(protected_tokens::text)||':'||refresh_pending FROM bff_session WHERE client_id='"+rp+"' AND session_id_hash='"+hashvalue(value)+"';")
    def events():return int(db('koiki_keycloak',"SELECT count(*) FROM event_entity WHERE type='REFRESH_TOKEN' AND client_id='koiki-d3-a';"))
    def service_id(name):return run(compose+['ps','-aq',name])
    def stop(name):
        cid=service_id(name);before=time.monotonic();run(['docker','stop','--time','40',cid],timeout=45);duration=round(time.monotonic()-before,3)
        state=json.loads(run(['docker','inspect',cid]))[0]['State'];entry=dict(service=name,container_id=cid,signal='SIGTERM',seconds=duration,exit_code=state['ExitCode'],forced_kill=state['ExitCode']==137,oom=state['OOMKilled'])
        assert not state['Running'] and state['ExitCode'] in [0,143] and not state['OOMKilled'],entry
        return entry
    def start(name,timeout=160):run(compose+['up','-d','--no-deps','--wait','--wait-timeout',str(timeout),name],timeout=timeout+20)
    def business():return db('koiki_app',"SELECT (SELECT count(*) FROM kkref_expense_request)||':'||(SELECT count(*) FROM kkref_expense_line)||':'||(SELECT count(*) FROM koiki_audit_event WHERE audit_type='BUSINESS');")
    def persistence():
        return dict(business=db('koiki_app',"SELECT expense_request_id||':'||status||':'||claimed_amount||':'||version FROM kkref_expense_request ORDER BY expense_request_id;"),
                    business_counts=business(),sessions=db('koiki_bff',"SELECT client_id||':'||session_id_hash||':'||md5(protected_tokens::text)||':'||refresh_pending FROM bff_session ORDER BY client_id,session_id_hash;"),
                    schema=db('koiki_bff','SELECT version||\':\'||checksum FROM bff_schema_history ORDER BY version;'),
                    realm=db('koiki_keycloak',"SELECT id||':'||name FROM realm WHERE name='koiki-d3';"))
    def passed(name,detail):result['checkpoints'].append(dict(id=name,status='PASS',details=detail));write(work/'results.json',result);print('PASS '+name,flush=True)
    try:
        # Reconfirm the exact Java decoder implementation used by the unchanged executable JAR.
        jose=work/'runtime-libraries/spring-security-oauth2-jose-7.1.1.jar';jose.parent.mkdir()
        with zipfile.ZipFile(work/'context/app.jar') as jar:
            embedded=jar.read('BOOT-INF/lib/'+jose.name);jose.write_bytes(embedded)
        assert hashlib.sha256(jose.read_bytes()).hexdigest()==hashlib.sha256(embedded).hexdigest()
        javap=run(['javap','-c','-p','-classpath',str(jose),'org.springframework.security.oauth2.jwt.JwtTimestampValidator']);assert 'long 60l' in javap and 'DEFAULT_MAX_CLOCK_SKEW' in javap
        validators=run(['javap','-c','-p','-classpath',str(jose),'org.springframework.security.oauth2.jwt.JwtValidators']);assert 'JwtTimestampValidator' in validators and 'createDefaultWithIssuer' in validators
        obs['java_timestamp_validator']=dict(spring_security='7.1.1',default_skew_seconds=60,jose_sha256=hashlib.sha256(jose.read_bytes()).hexdigest(),same_library_in_executable_jar=True)
        run(compose+['up','-d','--wait','--wait-timeout','240','db','keycloak','backend'],timeout=270)
        migration.migrate(work,compose);v2=refresh_migration.migrate(work,compose).stdout;assert refresh_migration.migrate(work,compose).stdout==v2;obs['v2_idempotence_checked']=True;db('koiki_app',(work/'recipe/reference-seed.sql').read_text());assert business()=='0:0:0'
        obs['initialization']=dict(seed_applications=1,bff_migrations=[1,2],repeat_on_restart=False)
        settings=db('koiki_keycloak',"SELECT access_token_lifespan||':'||revoke_refresh_token||':'||refresh_token_max_reuse||':'||sso_idle_timeout||':'||sso_max_lifespan FROM realm WHERE name='koiki-d3';")
        assert settings=='60:true:0:1800:7200',settings;obs['effective_realm_settings']=settings
        client_timeouts=db('koiki_keycloak',"SELECT c.client_id||':'||a.name||'='||a.value FROM client_attributes a JOIN client c ON a.client_id=c.id WHERE c.client_id IN ('koiki-d3-a','koiki-d3-b') AND a.name IN ('client.session.idle.timeout','client.session.max.lifespan') ORDER BY c.client_id,a.name;").splitlines()
        assert client_timeouts==[name+':'+setting for name in ['koiki-d3-a','koiki-d3-b'] for setting in ['client.session.idle.timeout=1200','client.session.max.lifespan=5400']]
        obs['effective_client_timeouts']=client_timeouts
        host_time=int(time.time());java_time=int(run(compose+['exec','-T','backend','date','+%s']));db_time=int(float(db('koiki_app','SELECT extract(epoch from clock_timestamp());')));assert abs(host_time-java_time)<=2 and abs(host_time-db_time)<=2
        obs['utc_clocks']=dict(host_epoch=host_time,java_container_epoch=java_time,db_epoch=db_time)
        run(profile+['up','-d','--no-deps','helper']);deadline=time.monotonic()+40
        while time.monotonic()<deadline:
            try:
                if http('/health')[0]==200:break
            except OSError:pass
            time.sleep(1)
        else:raise RuntimeError('Helper readiness failed')
        with (work/'helper-browser.log').open('w') as log:
            browser=subprocess.Popen(['node',str(RECIPE/'browser-s5-helper.mjs'),str(work)],stdout=log,stderr=subprocess.STDOUT,text=True);browser.wait(timeout=170)
            assert browser.returncode==0,'Helper failed: '+redact(work,(work/'helper-browser.log').read_text())[-800:]
        browser=None;helper=json.loads((work/'helper.json').read_text());obs['direct_jwt_and_rotation']=helper
        assert helper['rotation']['old_refresh_rejected'] and helper['after']['status']==401 and helper['after']['observed_epoch']>=helper['after']['exp']+65
        run(profile+['stop','helper']);run(profile+['rm','-f','helper']);assert not run(profile+['ps','-aq','helper']);obs['probe_exclusive_with_frontend']=True
        start('frontend',100);ready();assert len(run(compose+['ps','-q']).splitlines())==4
        with (work/'browser-run.log').open('w') as log:
            browser=subprocess.Popen(['node',str(RECIPE/'browser-s5.mjs'),str(work)],stdout=log,stderr=subprocess.STDOUT,text=True)
            independent=wait('parallel-ready');sid=hashvalue(independent['a_hash']);force_expiry(sid);before=session_record(sid).split(':');count=events();go('parallel-ready');wait('parallel-done')
            after=session_record(sid).split(':');assert events()-count==1 and before[1]!=after[1] and after[2]=='false' and int(after[0])-int(before[0])==4
            obs['parallel_refresh']=dict(requests=2,issuer_refresh_events=1,version_delta=4,ciphertext_changed=True,pending=False,old_refresh_resends=0);go('parallel-done')
            race=wait('race-ready');race_hash=hashvalue(race['session_hash']);force_expiry(race_hash);race_events=events();run(['docker','pause',service_id('keycloak')]);paused=True;go('race-ready');wait('race-started')
            deadline=time.monotonic()+3
            while not session_record(race_hash).endswith(':true'):
                if time.monotonic()>deadline:raise RuntimeError('Refresh pending marker not observed')
                time.sleep(0.05)
            go('race-started');wait('race-logout-done');assert not session_record(race_hash);run(['docker','unpause',service_id('keycloak')]);paused=False;go('race-logout-done');wait('race-done');assert not session_record(race_hash) and events()-race_events==1
            obs['logout_race']=dict(real_successful_refresh_exchange=1,session_rows_after=0,cookie_deleted=True);go('race-done')
            wait('idp-stop-ready');force_expiry(sid);business_before=business();obs['idp_fault_stop']=stop('keycloak');go('idp-stop-ready');wait('idp-stop-done');assert not session_record(sid);start('keycloak');ready();assert business()==business_before;go('idp-stop-done')
            wait('backend-stop-ready');business_before=business();obs['backend_fault_stop']=stop('backend');go('backend-stop-ready');wait('backend-stop-done');assert business()==business_before;start('backend');ready();go('backend-stop-done')
            wait('db-stop-ready');business_before=business();obs['db_fault_stop']=stop('db');go('db-stop-ready');wait('db-stop-done');start('db',60);ready(120);assert business()==business_before;go('db-stop-done')
            restart=wait('restart-ready');pending_hash=hashvalue(restart['pending_hash']);assert db('koiki_bff',"UPDATE bff_session SET refresh_pending=true WHERE client_id='a' AND session_id_hash='"+pending_hash+"' RETURNING 1;")=='1'
            snapshot=persistence();key_hash=hashlib.sha256((work/'secrets/bff_key').read_bytes()).hexdigest();obs['termination']=[stop(name) for name in ['frontend','backend','keycloak']]
            start('keycloak');start('backend');start('frontend',100);ready();assert persistence()==snapshot and hashlib.sha256((work/'secrets/bff_key').read_bytes()).hexdigest()==key_hash
            obs['persistence']=dict(identical_before_after=snapshot,same_key=True,no_reseed=True,no_migration_repeat=True)
            previous_events=events();go('restart-ready');wait('pending-recovery-done');assert not session_record(pending_hash) and events()==previous_events;obs['pending_recovery']=dict(fixture='test-owned durable pending marker set before SIGTERM',session_deleted=True,issuer_exchanges=0);go('pending-recovery-done')
            wait('restart-done');go('restart-done');sso=wait('sso-refresh-ready');b_hash=hashvalue(sso['b_hash']);assert session_record(b_hash,'b');force_expiry(b_hash,'b')
            b_errors=int(db('koiki_keycloak',"SELECT count(*) FROM event_entity WHERE type='REFRESH_TOKEN_ERROR' AND client_id='koiki-d3-b';"));go('sso-refresh-ready');wait('sso-refresh-done');assert not session_record(b_hash,'b')
            assert int(db('koiki_keycloak',"SELECT count(*) FROM event_entity WHERE type='REFRESH_TOKEN_ERROR' AND client_id='koiki-d3-b';"))-b_errors==1
            obs['sso_logout_remaining_b']=dict(existing_jwt_http=200,refresh_http=401,silent_login_error='login_required',issuer_refresh_error_events=1,b_session_removed=True);go('sso-refresh-done');browser.wait(timeout=120);assert browser.returncode==0,'Browser failed: '+redact(work,(work/'browser-run.log').read_text())[-1500:]
        browser=None;observed=json.loads((work/'browser.json').read_text());obs['browser']=observed
        assert observed['parallel_statuses']==[200,200] and observed['statuses']['refresh_logout_race']==401
        passed('S5-V01','parallel two requests => one real issuer refresh; durable pending mark, no old-token retry; isolated old-token rejection; real successful refresh/logout race and pending-after-restart rejection; unknown outcome unit stub')
        assert observed['statuses']['other_detail']==404 and helper['after']['status']==401
        passed('S5-V02','two real users SSR/RSC/API/cache/Session isolation; independent helper directly sends unrefreshed JWT to Java at exp+60-second verified skew+5 seconds =>401')
        assert observed['statuses']['idp_down_login']==503 and observed['statuses']['idp_down_refresh']==401 and observed['statuses']['idp_down_existing_jwt']==404 and observed['statuses']['backend_down_post'] in [503,504] and observed['backend_down_business_post_attempts']==1
        passed('S5-V03','IdP stop login/refresh fail; existing JWT path remains usable at observation; backend-down POST once fails bounded without mutation or automatic resend; dependencies recover')
        assert observed['statuses']['db_down_ready']==503 and observed['statuses']['db_down_live']==200 and observed['statuses']['db_down_api']==503
        containers=json.loads(run(['docker','inspect',*run(compose+['ps','-q']).splitlines()]));assert len(containers)==4 and all(c['RestartCount']==0 and not c['State']['OOMKilled'] for c in containers)
        assert sum(c['HostConfig']['Memory'] for c in containers)<=5120*1024**2 and sum(c['HostConfig']['NanoCpus'] for c in containers)<=4_000_000_000
        obs['runtime_ids']={c['Config']['Labels']['com.docker.compose.service']:dict(id=c['Id'],image_id=c['Image'],restarts=c['RestartCount'],oom=c['State']['OOMKilled']) for c in containers}
        passed('S5-V04','DB stop => ready503/live200/API503; same processes recover with zero unplanned restarts, OOM or duplicate writes')
        passed('S5-V05','Node/Java/Keycloak SIGTERM measured, exit0/143 without forced kill; same volume/key preserves business/Audit/Session/realm/schema before reads; decrypt and reject durable pending fixture after restart')
        request_id=observed['smoke_request_id'];assert re.fullmatch('[a-f0-9-]{36}',request_id)
        assert business()=='2:2:1'
        assert db('koiki_app',"SELECT status||':'||claimed_amount||':'||applicant_user_id FROM kkref_expense_request WHERE expense_request_id='"+request_id+"';")=='SUBMITTED:2300:d3200000-0000-4000-8000-000000000001'
        assert db('koiki_app',"SELECT count(*) FROM koiki_audit_event WHERE audit_type='BUSINESS' AND action='SUBMIT_EXPENSE' AND result='SUCCESS' AND resource_id='"+request_id+"';")=='1'
        obs['final_business']=dict(requests=2,lines=2,submit_success_audit=1,fixture_draft_amount=1200,smoke_submitted_amount=2300,failed_post_retried=False)
        obs['coverage']={'KS01':'S1-V01..05 / S3-V04 inherited; S5 preflight','KS02':'S2-V01 inherited; S5 fresh realm/DB/ready','KS03':'S3-V02 inherited; S5 final browser smoke','KS04':'S4-V01..02 inherited; S5 positive SSO smoke','KS05':'S2-V03 inherited; S5 other-user404 and direct JWT expiry','KS06':'S3-V03..04 / S4-V02..04 inherited; S5 two-user SSR/RSC/API/cache','KS07':'S4-V03 inherited; S5-V01..02','KS08':'S5-V03..04','KS09':'S5-V05','KS10':'all stages cleanup/export; S5-V06'}
        result['status']='PASS_PENDING_CLEANUP'
    except Exception as error:result['status']='FAIL';result['failure']=redact(work,type(error).__name__+': '+str(error));print(result['failure'],flush=True)
    finally:
        if browser and browser.poll() is None:
            browser.terminate()
            try:browser.wait(timeout=10)
            except subprocess.TimeoutExpired:browser.kill();browser.wait(timeout=5)
        if paused:command(work,profile+['unpause','keycloak'],check=False)
        try:
            raw=command(work,profile+['logs','--no-color'],timeout=30).stdout;safe=redact(work,raw);(work/'runtime-redacted.log').write_text(safe)
            if raw!=safe:result['status']='FAIL';result['failure']='Runtime credential/token exposure detected; retained log redacted'
        finally:
            try:
                cleanup(work,record,profile)
                if result['status']=='PASS_PENDING_CLEANUP':passed('S5-V06','final artifact A create/detail/submit and A-to-B SSO, DB/Audit exact counts, no secret exposure, coverage trace and owned cleanup complete');result['status']='PASS'
            except Exception as error:result['status']='HOLD';result['cleanup_failure']=redact(work,str(error))
            result['elapsed_seconds']=round(time.monotonic()-started,3);write(work/'results.json',result)
    if result['status']!='PASS':raise SystemExit(1)


if __name__=='__main__':main()
