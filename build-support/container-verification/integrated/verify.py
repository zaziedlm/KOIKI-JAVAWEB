"""Finite Reference-only verification of an already staged, owned Compose project."""
import argparse
import hashlib
import json
from pathlib import Path
import re
import subprocess
import time
import urllib.error
import urllib.request
import zipfile


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--work-dir', type=Path, required=True)
    parser.add_argument('--classpath', type=Path, required=True)
    parser.add_argument('--java', required=True)
    args = parser.parse_args()
    work = args.work_dir.resolve(strict=True)
    recipe = Path(__file__).resolve().parent
    record = json.loads((work / 'artifact.json').read_text())
    if not re.fullmatch('[a-f0-9]{64}', record.get('jar_sha256', '')):
        parser.error('Missing or invalid source JAR SHA-256')
    if not re.fullmatch(r'd3-[a-z0-9-]+', record['project']) or not record['reference_spa']:
        parser.error('Expected prepared Reference-only d3 project')
    if (work / 'results.json').exists() or (work / 'failure.txt').exists():
        parser.error('Existing evidence: use a new work-dir/project for another attempt')
    compose = ['docker', 'compose', '--project-name', record['project'], '--env-file', str(work / 'compose.env'),
               '-f', str(recipe / 'compose.yaml')]
    secret_values = [p.read_text() for p in (work / 'secrets').iterdir()]
    observed = []
    results = []
    base = 'http://127.0.0.1:' + str(record['port'])

    def sanitized(value):
        for secret in secret_values:
            value = value.replace(secret, '[REDACTED]')
        return value

    def run(command, data=None, timeout=120):
        result = subprocess.run(command, input=data, capture_output=True, text=True, encoding='utf-8', timeout=timeout)
        if result.returncode:
            raise RuntimeError(sanitized(result.stderr[-2000:] or result.stdout[-2000:]))
        return result.stdout.strip()

    def checkpoint(name):
        results.append(name)
        print('PASS ' + name, flush=True)

    def http(path, expected, method='GET'):
        req = urllib.request.Request(base + path, method=method)
        try:
            with urllib.request.urlopen(req, timeout=8) as response:
                code, body = response.status, response.read().decode()
        except urllib.error.HTTPError as response:
            code, body = response.code, response.read().decode()
        if code != expected:
            raise RuntimeError(f'{method} {path}: HTTP {code}, expected {expected}')
        if path.startswith('/actuator/health/') and code == 200:
            assert json.loads(body) == {'status': 'UP'}, 'Probe exposed components/details'
        return body

    def ready():
        deadline = time.monotonic() + 150
        while time.monotonic() < deadline:
            try:
                http('/actuator/health/readiness', 200)
                return
            except (OSError, RuntimeError):
                time.sleep(2)
        raise RuntimeError('Application readiness timeout')

    def sql(query, user='koiki'):
        return run(compose + ['exec', '-T', 'db', 'psql', '-v', 'ON_ERROR_STOP=1', '-U', user,
                               '-d', 'koiki', '-At'], data=query)

    # Refuse before entering finally: cleanup must never target a pre-existing project.
    label = 'label=com.docker.compose.project=' + record['project']
    for command in (['docker', 'ps', '-aq', '--filter', label],
                    ['docker', 'volume', 'ls', '-q', '--filter', label],
                    ['docker', 'network', 'ls', '-q', '--filter', label]):
        if run(command):
            parser.error('Compose project already owns resources; choose a new project')

    try:
        startup = time.monotonic()
        run(compose + ['up', '-d', '--wait', '--wait-timeout', '180'], timeout=210)
        startup_seconds = round(time.monotonic() - startup, 3)
        ready()
        checkpoint('DC02 startup-migrations-ready')
        ids = run(compose + ['ps', '-q']).splitlines()
        assert len(ids) == 2
        inspected = json.loads(run(['docker', 'inspect', *ids]))
        app = next(c for c in inspected if c['Config']['Labels']['com.docker.compose.service'] == 'app')
        assert app['Config']['User'] == '10001:10001' and app['HostConfig']['ReadonlyRootfs']
        assert app['HostConfig']['Memory'] == 1536 * 1024**2
        assert app['Path'] == 'java'
        image = json.loads(run(['docker', 'image', 'inspect', app['Image']]))[0]
        actual_hash = image['Config']['Labels'].get('org.koikifw.d3.source-jar-sha256')
        assert actual_hash == record['jar_sha256']
        assert app['HostConfig']['PidsLimit'] == 160
        assert app['Config']['Healthcheck']['StartInterval'] == 2_000_000_000
        assert 'JAVA_TOOL_OPTIONS=-Xmx768m -XX:+ExitOnOutOfMemoryError' in app['Config']['Env']
        for container in inspected:
            assert container['HostConfig']['LogConfig'] == dict(
                Type='json-file', Config={'max-size': '10m', 'max-file': '3'})
        thread_status = run(compose + ['exec', '-T', 'app', 'cat', '/proc/1/status'])
        threads = int(re.search(r'^Threads:\s+(\d+)', thread_status, re.MULTILINE).group(1))
        pids_current = int(run(compose + ['exec', '-T', 'app', 'cat', '/sys/fs/cgroup/pids.current']))
        assert threads < 160 and pids_current < 160
        runtime_jar = work / 'runtime-app.jar'
        run(['docker', 'cp', app['Id'] + ':/app/app.jar', str(runtime_jar)])
        with zipfile.ZipFile(runtime_jar) as archive:
            properties = archive.read('application-d3-container.properties').decode()
            assert 'server.tomcat.threads.max=40' in properties.splitlines()
            assert 'META-INF/resources/spa/index.html' in archive.namelist()
        observed.append(dict(app_id=app['Id'], image_id=app['Image'], source_jar_sha256=actual_hash,
                             identity_check='image-label', compose_up_wait_seconds=startup_seconds,
                             startup_threads=threads, startup_pids=pids_current,
                             packaged_tomcat_threads_max=40))
        checkpoint('DC01 nonroot-readonly-pid1-artifact')
        http('/actuator/health/liveness', 200)
        http('/actuator/env', 401)
        http('/actuator/health', 401)
        # Real server error dispatch reaches the fail-closed fallback (401).
        # The isolated filter-chain test observes the initial CSRF rejection (403).
        http('/actuator/health/readiness', 401, 'POST')
        http('/spa-api/v1/expense-requests', 401)
        assert sql("select rolsuper from pg_roles where rolname='koiki';") == 'f'
        assert sql('select count(*) from koiki_flyway_history where success;') != '0'
        assert sql('select count(*) from kkref_flyway_history where success;') != '0'
        checkpoint('DC04 probes-default-deny-db-role')
        sql('create extension if not exists pgcrypto;', 'postgres')
        password = (work / 'secrets/login-password').read_text()
        fixture = (recipe / 'seed.sql').read_text().replace('__LOGIN_PASSWORD__', password)
        sql(fixture)
        cp = args.classpath.resolve(strict=True).read_text().strip()
        browser = run([args.java, '--class-path', cp, str(recipe / 'ContainerSpaJourney.java'),
                       base, str(work / 'secrets/login-password'), str(work / 'spa.png')], timeout=120)
        (work / 'browser.txt').write_text(sanitized(browser), encoding='utf-8')
        assert 'D3_BROWSER_PASS' in browser
        checkpoint('DC03 browser-login-spa-create-csrf-submit-reload-logout')
        assert sql("select status||':'||claimed_amount from kkref_expense_request;") == 'SUBMITTED:1200'
        assert sql("select count(*) from koiki_audit_event where audit_type='BUSINESS' and action='SUBMIT_EXPENSE';") == '1'
        assert sql("select count(*) from pg_namespace where nspname='kkref_notification_async';") == '0'
        before = sql('select count(*) from koiki_flyway_history; select count(*) from kkref_flyway_history;')
        run(compose + ['restart', 'app'], timeout=60)
        ready()
        assert sql("select status||':'||claimed_amount from kkref_expense_request;") == 'SUBMITTED:1200'
        assert before == sql('select count(*) from koiki_flyway_history; select count(*) from kkref_flyway_history;')
        checkpoint('DC07 sigterm-restart-db-audit-migration-preserved')
        run(compose + ['stop', 'db'], timeout=35)
        http('/actuator/health/readiness', 503)
        http('/actuator/health/liveness', 200)
        run(compose + ['start', 'db'], timeout=30)
        ready()
        assert sql("select count(*) from koiki_audit_event where audit_type='BUSINESS' and action='SUBMIT_EXPENSE';") == '1'
        checkpoint('DC06 db-stop-live-ready-recovery-no-resubmit')
    except Exception as error:
        (work / 'failure.txt').write_text(sanitized(str(error)), encoding='utf-8')
        raise
    finally:
        try:
            logs = run(compose + ['logs', '--no-color'], timeout=15)
            (work / 'runtime-sanitized.log').write_text(sanitized(logs), encoding='utf-8')
            assert all(secret not in logs for secret in secret_values), 'Secret appeared in runtime log'
        finally:
            run(compose + ['down', '--volumes', '--remove-orphans', '--timeout', '40'], timeout=90)
            assert not run(['docker', 'ps', '-aq', '--filter', 'label=com.docker.compose.project=' + record['project']])
            assert not run(['docker', 'volume', 'ls', '-q', '--filter', 'label=com.docker.compose.project=' + record['project']])
            assert not run(['docker', 'network', 'ls', '-q', '--filter', 'label=com.docker.compose.project=' + record['project']])
            checkpoint('DC08 owned-containers-volumes-networks-cleanup')
            (work / 'results.json').write_text(json.dumps(dict(checkpoints=results, artifacts=observed), indent=2) + '\n')


if __name__ == '__main__':
    main()
