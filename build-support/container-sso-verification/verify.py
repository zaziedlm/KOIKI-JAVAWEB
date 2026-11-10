"""S1 only: five finite checkpoint groups, sanitized evidence and owned cleanup."""
import argparse
import importlib.util
import json
from pathlib import Path
import time
from cleanup import cleanup
from preflight import RECIPE, command, load, preflight, redact, resources, sql, write

spec = importlib.util.spec_from_file_location('bff_migration', RECIPE / 'migrate-bff.py')
migration = importlib.util.module_from_spec(spec)
spec.loader.exec_module(migration)
DATABASES = ['koiki_app', 'koiki_bff', 'koiki_keycloak']


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--work-dir', type=Path, required=True)
    args = parser.parse_args()
    work, record, compose = load(args.work_dir)
    identity = preflight(work, record, compose)  # refuse foreign ownership before arming finally
    started = time.monotonic()
    write(work / 'execution.json', dict(project=record['project'], started_with_empty_project=True,
                                      started_epoch=time.time(), image=identity))
    result = dict(schema=1, stage='S1', status='RUNNING', source_sha=record['source_sha'],
                  checkpoints=[], observations={}, not_executed=['S2', 'S3', 'S4', 'S5', 'browser journey'])
    checks = result['checkpoints']
    obs = result['observations']

    def run(args, data=None, timeout=60):
        assert time.monotonic() - started < 1800, 'Runtime execution budget exhausted (cleanup reserved)'
        return command(work, args, data=data, timeout=timeout).stdout.strip()

    def db(role, query):
        assert time.monotonic() - started < 1800
        return sql(work, compose, role, role, query).stdout.strip()

    def passed(name, details):
        checks.append(dict(id=name, status='PASS', details=details))
        write(work / 'results.json', result)
        print('PASS ' + name, flush=True)

    def http(path):
        assert path in ['/actuator/health/liveness', '/actuator/health/readiness', '/actuator/health', '/actuator/env']
        request = 'exec 3<>/dev/tcp/127.0.0.1/8080; printf "GET ' + path + ' HTTP/1.1\\r\\nHost: localhost\\r\\nConnection: close\\r\\n\\r\\n" >&3; cat <&3'
        import base64
        raw = base64.b64decode(run(compose + ['exec', '-T', 'backend', '/bin/bash', '-ec', request + ' | base64'], timeout=15))
        # http.client handles HTTP chunking as well as Content-Length without trusting TCP boundaries.
        import http.client
        import io
        class Socket:
            def makefile(self, *args, **kwargs):
                return io.BytesIO(raw)
        response = http.client.HTTPResponse(Socket())
        response.begin()
        return response.status, response.read().decode()

    try:
        run(compose + ['up', '-d', '--wait', '--wait-timeout', '180'], timeout=210)
        ids = run(compose + ['ps', '-q']).splitlines()
        inspected = json.loads(run(['docker', 'inspect', *ids]))
        assert len(inspected) == 2
        for item in inspected:
            assert item['Config']['Labels']['com.docker.compose.project'] == record['project']
            assert not item['HostConfig']['PortBindings']
            assert item['HostConfig']['NanoCpus'] == 1_000_000_000
        backend = next(i for i in inspected if i['Config']['Labels']['com.docker.compose.service'] == 'backend')
        pg = next(i for i in inspected if i['Config']['Labels']['com.docker.compose.service'] == 'db')
        assert backend['Image'] == identity['image_id']
        assert backend['Config']['User'] == '10001:10001' and backend['HostConfig']['ReadonlyRootfs']
        assert backend['HostConfig']['Memory'] == 1536 * 1024**2
        assert pg['HostConfig']['Memory'] == 1024 * 1024**2
        pg_image = json.loads(run(['docker', 'image', 'inspect', pg['Image']]))[0]
        assert any(record['versions']['images']['postgres']['index_digest'] in d for d in pg_image['RepoDigests'])
        assert {Path(m['Destination']).name for m in backend['Mounts'] if m['Destination'].startswith('/run/secrets/')} == {
            'spring.datasource.password', 'koiki.identity.login-attempt.source-hmac-key'}
        obs['runtime_ids'] = dict(backend=backend['Id'], postgres=pg['Id'], backend_image=backend['Image'], postgres_image=pg['Image'])
        obs['ownership'] = resources(work, record['project'])
        passed('S1-V01', 'source/JAR/lock/digest identities; two owned services; no host-published ports')

        role_checks, rejected = [], []
        for role in DATABASES:
            properties = db(role, "SELECT rolsuper,rolcreatedb,rolcreaterole,rolreplication,rolbypassrls FROM pg_roles WHERE rolname=current_user;")
            assert properties == 'f|f|f|f|f'
            assert db(role, 'SELECT pg_get_userbyid(datdba)=current_user FROM pg_database WHERE datname=current_database();') == 't'
            # Finite runtime/DDL privilege check, rolled back (including the empty Keycloak DB).
            assert db(role, "BEGIN; CREATE TABLE d3_runtime_probe(value integer); INSERT INTO d3_runtime_probe VALUES(7); SELECT value FROM d3_runtime_probe; ROLLBACK;") == '7'
            role_checks.append(dict(role=role, own_db_operation='PASS', flags=properties))
            for other in DATABASES:
                if other == role:
                    continue
                r = sql(work, compose, role, other, 'SELECT 1;', check=False)
                assert r.returncode and 'permission denied for database' in r.stderr
                rejected.append(dict(role=role, database=other, result='CONNECT denied'))
        assert db('koiki_app', "SELECT count(*) FROM pg_database d, LATERAL aclexplode(d.datacl) a WHERE d.datname IN ('koiki_app','koiki_bff','koiki_keycloak') AND a.grantee=0 AND a.privilege_type='CONNECT';") == '0'
        assert db('koiki_app', 'SHOW max_connections;') == '32'
        obs['roles'] = role_checks
        obs['cross_db_denials'] = rejected
        obs['pool_observation'] = db('koiki_app', "SELECT coalesce(usename,'(internal)')||':'||count(*) FROM pg_stat_activity GROUP BY usename ORDER BY usename NULLS FIRST;").splitlines()
        passed('S1-V02', '3 non-superuser owner/runtime roles; 3 rolled-back DDL/DML checks; all 6 cross-DB connections denied; PUBLIC CONNECT absent')

        migration_result = migration.migrate(work, compose).stdout.strip()
        obs['bff_migration'] = migration_result
        assert db('koiki_app', 'SELECT count(*) FROM koiki_flyway_history WHERE success;') == '3'
        assert db('koiki_app', 'SELECT count(*) FROM kkref_flyway_history WHERE success;') == '4'
        db('koiki_app', (work / 'recipe/reference-seed.sql').read_text())
        assert db('koiki_app', 'SELECT count(*) FROM koiki_user;') == '4'
        assert db('koiki_app', 'SELECT count(*) FROM koiki_user_role;') == '3'
        assert db('koiki_app', 'SELECT count(*) FROM kkref_user_department_assignment;') == '4'
        expected = ['bff_auth_transaction', 'bff_schema_history', 'bff_session']
        assert db('koiki_bff', "SELECT tablename FROM pg_tables WHERE schemaname='public' ORDER BY tablename;").splitlines() == expected
        assert db('koiki_keycloak', "SELECT count(*) FROM pg_tables WHERE schemaname NOT IN ('pg_catalog','information_schema');") == '0'
        obs['bff_columns'] = db('koiki_bff', "SELECT table_name||'.'||column_name||':'||data_type||':'||is_nullable FROM information_schema.columns WHERE table_schema='public' ORDER BY table_name,ordinal_position;").splitlines()
        obs['bff_indexes'] = db('koiki_bff', "SELECT indexdef FROM pg_indexes WHERE schemaname='public' ORDER BY tablename,indexname;").splitlines()
        obs['seed'] = db('koiki_app', "SELECT user_id||':'||status FROM koiki_user ORDER BY user_id;").splitlines()
        passed('S1-V03', 'KOIKI 3 + Reference 4 migrations, 4 users + permissions/master seed, exactly 3 BFF tables; Keycloak product tables=0')

        before = db('koiki_bff', 'SELECT version||checksum||applied_at::text FROM bff_schema_history ORDER BY version;')
        migration.migrate(work, compose)
        assert before == db('koiki_bff', 'SELECT version||checksum||applied_at::text FROM bff_schema_history ORDER BY version;')
        try:
            migration.migrate(work, compose, (work / 'recipe/bff-migration.sql').read_text() + '\n-- deliberate mismatch\n')
        except RuntimeError as error:
            assert 'BFF migration checksum mismatch' in str(error)
        else:
            raise AssertionError('Checksum mismatch was not rejected')
        assert before == db('koiki_bff', 'SELECT version||checksum||applied_at::text FROM bff_schema_history ORDER BY version;')
        def snapshot():
            return db('koiki_app', "SELECT md5(string_agg(row_to_json(t)::text,',' ORDER BY user_id)) FROM koiki_user t; SELECT version||':'||checksum||':'||installed_on::text FROM koiki_flyway_history ORDER BY installed_rank; SELECT version||':'||checksum||':'||installed_on::text FROM kkref_flyway_history ORDER BY installed_rank;")
        old = snapshot()
        run(compose + ['restart', 'backend'], timeout=60)
        deadline = time.monotonic() + 150
        while time.monotonic() < deadline:
            try:
                if http('/actuator/health/readiness') == (200, '{"status":"UP"}'):
                    break
            except (RuntimeError, OSError):
                pass
            time.sleep(2)
        else:
            raise RuntimeError('Backend restart readiness timed out')
        assert snapshot() == old
        obs['restart_snapshot_unchanged'] = True
        passed('S1-V04', 'idempotent BFF reapply, checksum mismatch rejected without mutation, Java restart preserves users/histories without reseeding')

        statuses = {}
        for path in ['/actuator/health/liveness', '/actuator/health/readiness']:
            status, body = http(path)
            assert status == 200 and json.loads(body) == {'status': 'UP'}
            statuses[path] = status
        for path in ['/actuator/health', '/actuator/env']:
            status, body = http(path)
            assert status == 401
            statuses[path] = status
        obs['http_statuses'] = statuses
        obs['elapsed_before_cleanup_seconds'] = round(time.monotonic() - started, 3)
        result['status'] = 'PASS_PENDING_CLEANUP'
    except Exception as error:
        result['status'] = 'FAIL'
        result['failure'] = redact(work, type(error).__name__ + ': ' + str(error))
        print(result['failure'], flush=True)
    finally:
        try:
            raw = command(work, compose + ['logs', '--no-color'], timeout=20).stdout
            safe = redact(work, raw)
            (work / 'runtime-redacted.log').write_text(safe, encoding='utf-8')
            if raw != safe:
                result['status'] = 'FAIL'
                result['failure'] = 'Generated credential exposed in runtime logs (redacted in evidence)'
        except Exception as error:
            result['status'] = 'HOLD'
            result['failure'] = redact(work, str(error))
        finally:
            try:
                cleanup(work, record, compose)
                if result['status'] == 'PASS_PENDING_CLEANUP':
                    passed('S1-V05', 'live/ready UP without details; other Actuator 401; owned cleanup and generated-secret deletion complete')
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
