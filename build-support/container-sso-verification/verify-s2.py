"""Finite real-IdP S2 verification; never advance into BFF or SSO work."""
import argparse
import importlib.util
import json
from pathlib import Path
import subprocess
import time
import urllib.error
import urllib.request
from cleanup import cleanup
from preflight import RECIPE, command, digest, load, preflight, redact, resources, sql, write

spec = importlib.util.spec_from_file_location('s2_bff_migration', RECIPE / 'migrate-bff.py')
migration = importlib.util.module_from_spec(spec)
spec.loader.exec_module(migration)


def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('--work-dir', type=Path, required=True)
    a = p.parse_args()
    work, record, compose = load(a.work_dir)
    if record['phase'] != 'S2':
        p.error('S2 work required')
    identity = preflight(work, record, compose)
    started = time.monotonic()
    write(work / 'execution.json', dict(project=record['project'], started_with_empty_project=True,
                                      started_epoch=time.time(), image=identity, stage='S2'))
    result = dict(schema=1, stage='S2', status='RUNNING', source_sha=record['source_sha'],
                  checkpoints=[], observations={}, not_executed=['S3', 'S4', 'S5', 'BFF', '2-client SSO'])
    obs = result['observations']
    helper_compose = compose + ['--profile', 'helper']

    def run(args, data=None, timeout=60):
        if time.monotonic() - started > 2100:
            raise RuntimeError('S2 execution deadline reached; cleanup reserved')
        return command(work, args, data=data, timeout=timeout).stdout.strip()

    def db(role, query):
        return sql(work, compose, role, role, query).stdout.strip()

    def helper(path, method='GET'):
        assert path in ['/health', '/results', '/exercise', '/denials', '/forget']
        req = urllib.request.Request('http://127.0.0.1:13000' + path, method=method)
        with urllib.request.urlopen(req, timeout=40) as response:
            return json.load(response)

    def passed(name, detail):
        result['checkpoints'].append(dict(id=name, status='PASS', details=detail))
        write(work / 'results.json', result)
        print('PASS ' + name, flush=True)

    def business_snapshot():
        return db('koiki_app', "SELECT md5(coalesce(string_agg(row_to_json(t)::text,',' ORDER BY expense_request_id),'')) FROM kkref_expense_request t; SELECT md5(coalesce(string_agg(row_to_json(t)::text,',' ORDER BY expense_line_id),'')) FROM kkref_expense_line t; SELECT md5(coalesce(string_agg(row_to_json(t)::text,',' ORDER BY event_id),'')) FROM koiki_audit_event t WHERE audit_type='BUSINESS' AND result='SUCCESS';")

    try:
        run(compose + ['up', '-d', '--wait', '--wait-timeout', '240', 'db', 'keycloak', 'backend'], timeout=270)
        migration.migrate(work, compose)
        db('koiki_app', (work / 'recipe/reference-seed.sql').read_text())
        assert db('koiki_app', 'SELECT count(*) FROM koiki_user;') == '4'
        assert int(db('koiki_keycloak', "SELECT count(*) FROM pg_tables WHERE schemaname='public';")) > 0
        realm_settings = db('koiki_keycloak', "SELECT name,access_token_lifespan,sso_idle_timeout,sso_max_lifespan,revoke_refresh_token,refresh_token_max_reuse FROM realm WHERE name='koiki-d3';")
        obs['realm_settings'] = realm_settings
        assert realm_settings == 'koiki-d3|300|1800|7200|t|0'
        # Imported protected attributes and client modes are observable in the generated config.
        realm = json.loads((work / 'realm/koiki-d3.json').read_text())
        assert realm['clientSessionIdleTimeout'] == 1200 and realm['clientSessionMaxLifespan'] == 5400
        for client in realm['clients']:
            assert client['standardFlowEnabled'] and not client['directAccessGrantsEnabled'] and not client['serviceAccountsEnabled']
            assert client['attributes']['pkce.code.challenge.method'] == 'S256'
        obs['generated_realm_sha256'] = record['realm_sha256']
        actual_profiles = db('koiki_keycloak', "SELECT value FROM component_config WHERE name='kc.user.profile.config';").splitlines()
        assert any(next((a for a in json.loads(profile).get('attributes', []) if a['name'] == 'koiki_user_id'), {}).get('permissions', {}).get('edit') == ['admin'] for profile in actual_profiles)
        obs['koiki_user_id_edit_permission'] = 'admin only; persisted user-profile component checked'
        client_timeouts = db('koiki_keycloak', "SELECT c.client_id||':'||a.name||'='||a.value FROM client_attributes a JOIN client c ON a.client_id=c.id WHERE c.client_id IN ('koiki-d3-a','koiki-d3-wrong-audience') AND a.name IN ('client.session.idle.timeout','client.session.max.lifespan') ORDER BY c.client_id,a.name;").splitlines()
        assert client_timeouts == [name + ':' + setting for name in ['koiki-d3-a', 'koiki-d3-wrong-audience'] for setting in ['client.session.idle.timeout=1200', 'client.session.max.lifespan=5400']]
        obs['client_timeouts'] = client_timeouts
        run(helper_compose + ['up', '-d', '--no-deps', 'helper'], timeout=60)
        deadline = time.monotonic() + 45
        while time.monotonic() < deadline:
            try:
                obs['helper_discovery'] = helper('/health')
                break
            except (OSError, RuntimeError):
                time.sleep(2)
        else:
            raise RuntimeError('Helper internal health/discovery timeout')
        assert obs['helper_discovery']['issuer'] == 'http://idp.localhost:18080/realms/koiki-d3'
        assert obs['helper_discovery']['keycloak'] == 'UP' and obs['helper_discovery']['jwks_key_count'] > 0
        ids = run(helper_compose + ['ps', '-q']).splitlines()
        containers = json.loads(run(['docker', 'inspect', *ids]))
        assert len(containers) == 4
        for c in containers:
            service = c['Config']['Labels']['com.docker.compose.service']
            assert service in record['permitted_resources']['services']
            if service in ['db', 'backend']:
                assert not c['HostConfig']['PortBindings']
            assert not c['State']['OOMKilled']
        obs['runtime_ids'] = {c['Config']['Labels']['com.docker.compose.service']: {'id': c['Id'], 'image_id': c['Image']} for c in containers}
        browser = run(['node', str(RECIPE / 'browser-s2.mjs'), str(work)], timeout=180)
        assert 'S2_BROWSER_PASS 6' in browser
        claims = helper('/results')
        assert len(claims['completed']) == 6 and claims['pending'] == 0
        for case in claims['completed']:
            assert case['success']
            assert case['id_token_signature'] == 'JWKS/library verified'
            c = case['access_claims']
            assert c['iss'] == obs['helper_discovery']['issuer'] and c['token_use'] == 'access'
            assert c['exp'] - c['iat'] == 300
            assert c['koiki_user_id'].startswith('d3200000-0000-4000-8000-')
            aud = c['aud'] if isinstance(c['aud'], list) else [c['aud']]
            assert ('koiki-reference-api' in aud) == (case['case'] != 'wrong-audience')
            assert ('expense.apply' in c.get('scope', '').split()) == (case['case'] != 'no-scope')
            id_aud = case['id_claims']['aud']
            assert 'koiki-reference-api' not in (id_aud if isinstance(id_aud, list) else [id_aud])
        obs['claim_observations'] = claims['completed']
        obs['browser'] = json.loads((work / 'browser.json').read_text())
        passed('S2-V01', 'real Code+PKCE/nonce/state, 6 browser logins, canonical issuer/JWKS, product tables/realm/mappers/token settings')

        exercise = helper('/exercise', 'POST')
        obs['normal_api'] = exercise
        assert [exercise.get(k) for k in ['create', 'detail', 'submit']] == [201, 200, 204], exercise
        request_id = exercise['request_id']
        assert db('koiki_app', "SELECT status||':'||claimed_amount||':'||applicant_user_id FROM kkref_expense_request;") == 'SUBMITTED:1200:d3200000-0000-4000-8000-000000000001'
        assert db('koiki_app', 'SELECT count(*)||\':\'||sum(amount) FROM kkref_expense_line;') == '1:1200'
        audits = db('koiki_app', "SELECT actor_id||':'||action||':'||result||':'||resource_id FROM koiki_audit_event WHERE audit_type='BUSINESS' ORDER BY occurred_at;")
        assert 'd3200000-0000-4000-8000-000000000001:SUBMIT_EXPENSE:SUCCESS:' + request_id in audits
        assert db('koiki_app', "SELECT count(*) FROM koiki_audit_event WHERE audit_type='BUSINESS' AND action='SUBMIT_EXPENSE' AND result='SUCCESS';") == '1'
        obs['business_audit'] = audits.splitlines()
        passed('S2-V02', '201 create -> 200 own detail -> 204 submit; SUBMITTED:1200, line total/user/SUBMIT Audit match')

        before = business_snapshot()
        denial = helper('/denials', 'POST')
        expected = {'wrong-audience': 401, 'id-token': 401, 'no-scope': 403, 'disabled': 401, 'unprivileged': 403, 'second-user': 404}
        assert {r['case']: r['status'] for r in denial['denied']} == expected
        for r in denial['denied']:
            if r['case'] != 'second-user':
                assert r['post_status'] == expected[r['case']]
        assert business_snapshot() == before
        obs['denials'] = denial['denied']
        obs['business_state_unchanged_after_denials'] = True
        passed('S2-V03', '6 denial conditions / 11 requests; business row/line/success Audit hashes unchanged')
        forgotten = helper('/forget', 'POST')
        assert forgotten == dict(tokens_retained=0, transactions_retained=0)
        obs['helper_forget'] = forgotten
        run(helper_compose + ['stop', 'helper'], timeout=30)
        helper_id = obs['runtime_ids']['helper']['id']
        item = json.loads(run(['docker', 'inspect', helper_id]))[0]
        assert not item['State']['Running'] and item['State']['ExitCode'] == 0
        obs['helper_exit'] = dict(exit_code=item['State']['ExitCode'], oom_killed=item['State']['OOMKilled'])
        result['status'] = 'PASS_PENDING_CLEANUP'
    except Exception as error:
        result['status'] = 'FAIL'
        result['failure'] = redact(work, type(error).__name__ + ': ' + str(error))
        print(result['failure'], flush=True)
    finally:
        try:
            logs = command(work, helper_compose + ['logs', '--no-color'], timeout=20).stdout
            safe = redact(work, logs)
            (work / 'runtime-redacted.log').write_text(safe, encoding='utf-8')
            import re
            if safe != logs or re.search(r'eyJ[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+', safe):
                result['status'] = 'FAIL'
                result['failure'] = 'Credential/token appeared in runtime log'
        except Exception as error:
            result['status'] = 'HOLD'
            result['failure'] = redact(work, str(error))
        finally:
            try:
                cleanup(work, record, helper_compose)
                if result['status'] == 'PASS_PENDING_CLEANUP':
                    passed('S2-V04', 'transient helper exited 0 and forgot tokens; owned cleanup, generated secrets/realm removed, redacted evidence saved')
                    result['status'] = 'PASS'
            except Exception as error:
                result['status'] = 'HOLD'
                result['cleanup_failure'] = redact(work, str(error))
            result['elapsed_seconds'] = round(time.monotonic() - started, 3)
            write(work / 'results.json', result)
    if result['status'] != 'PASS':
        raise SystemExit(1)


if __name__ == '__main__':
    main()
