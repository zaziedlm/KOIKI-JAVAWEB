"""Initialize one fresh S5 work for Owner manual use; retain it after readiness."""
import argparse
import importlib.util
import http.cookiejar
import json
from pathlib import Path
import socket
import time
import urllib.request

from preflight import RECIPE, command, load, preflight, redact, sql, write


def migration(name, filename):
    spec = importlib.util.spec_from_file_location(name, RECIPE / filename)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--work-dir', type=Path, required=True)
    args = parser.parse_args()
    work, record, compose = load(args.work_dir)
    if record['phase'] != 'S5' or record.get('stop_diagnostics'):
        parser.error('Normal S5 work required')
    if (work / 'manual-state.json').exists():
        parser.error('Never reinitialize a manual environment')
    for port in [13000, 18080]:
        with socket.socket() as probe:
            probe.bind(('127.0.0.1', port))
    identity = preflight(work, record, compose)
    write(work / 'execution.json', dict(project=record['project'],
          started_with_empty_project=True, started_epoch=time.time(), image=identity,
          stage='MANUAL', authorization='Owner: 方式2の手動環境起動・Owner打鍵を進めます'))
    state = dict(status='STARTING', project=record['project'], owner_typing='PENDING',
                 retain=True, automatic_cleanup=False, seed_applications=0)
    write(work / 'manual-state.json', state)
    try:
        command(work, compose + ['up', '-d', '--wait', '--wait-timeout', '240',
                                  'db', 'keycloak', 'backend'], timeout=280)
        migration('manual_base', 'migrate-bff.py').migrate(work, compose)
        migration('manual_refresh', 'migrate-refresh.py').migrate(work, compose)
        sql(work, compose, 'koiki_app', 'koiki_app',
            (work / 'recipe/reference-seed.sql').read_text())
        state['seed_applications'] = 1
        command(work, compose + ['up', '-d', '--no-deps', '--wait',
                                  '--wait-timeout', '120', 'frontend'], timeout=150)
        checks = {}
        opener = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(http.cookiejar.CookieJar()))
        for path in ['/health/ready', '/a', '/b']:
            with opener.open('http://localhost:13000' + path, timeout=20) as response:
                assert response.status == 200
                checks[path] = response.status
        # Store the complete service inventory independently of Compose's JSON stream format.
        inventory = command(work, compose + ['ps', '--format', '{{.Service}} {{.State}} {{.Health}}']).stdout
        assert len(inventory.strip().splitlines()) == 4
        write(work / 'manual-services.json', dict(inventory=inventory))
        state.update(status='READY', ready_epoch=time.time(), public_http=checks,
                     source_sha=record['source_sha'], jar_sha256=record['jar_sha256'],
                     frontend_sha256=record['frontend_sha256'],
                     urls=['http://localhost:13000/a', 'http://localhost:13000/b'],
                     users=['applicant-a', 'applicant-b'],
                     password_file=str(work / 'secrets/login_password'),
                     post_typing_review='read-only business/DB/Audit/log inspection')
        write(work / 'manual-ready.json', state)
        print(json.dumps(state, ensure_ascii=False))
    except Exception as error:
        # Preserve an incomplete environment for explicit inspection; never reseed/retry it.
        state.update(status='HOLD', error=redact(work, str(error)))
        raise
    finally:
        write(work / 'manual-state.json', state)


if __name__ == '__main__':
    main()
