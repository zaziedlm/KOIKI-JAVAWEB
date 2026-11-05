"""Build/start a fresh Reference project, seed hand-check users, and leave it running."""
import argparse
from html.parser import HTMLParser
import http.cookiejar
import json
from pathlib import Path
import re
import subprocess
import urllib.parse
import urllib.request


class CsrfForm(HTMLParser):
    token = None

    def handle_starttag(self, tag, attrs):
        values = dict(attrs)
        if tag == 'input' and values.get('name') == '_csrf':
            self.token = values.get('value')


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--work-dir', required=True, type=Path)
    args = parser.parse_args()
    work = args.work_dir.resolve(strict=True)
    record = json.loads((work / 'artifact.json').read_text())
    if not re.fullmatch(r'd3-[a-z0-9-]+', record['project']) or not record['reference_spa']:
        parser.error('Expected a prepared Reference-only d3 project')
    for name in ('manual-ready.json', 'manual-failure.txt', 'results.json', 'failure.txt'):
        if (work / name).exists():
            parser.error('Existing result; use a fresh work-dir and project')
    recipe = Path(__file__).resolve().parent
    secrets = [p.read_text() for p in (work / 'secrets').iterdir()]
    compose = ['docker', 'compose', '--project-name', record['project'], '--env-file', str(work / 'compose.env'),
               '-f', str(recipe / 'compose.yaml')]

    def sanitize(text):
        for secret in secrets:
            text = text.replace(secret, '[REDACTED]')
        return text

    def run(command, data=None, timeout=60):
        result = subprocess.run(command, input=data, capture_output=True, text=True, encoding='utf-8', timeout=timeout)
        if result.returncode:
            raise RuntimeError(sanitize(result.stderr[-2500:] or result.stdout[-2500:]))
        return result.stdout.strip()

    label = 'label=com.docker.compose.project=' + record['project']
    for command in (['docker', 'ps', '-aq', '--filter', label],
                    ['docker', 'volume', 'ls', '-q', '--filter', label],
                    ['docker', 'network', 'ls', '-q', '--filter', label]):
        if run(command):
            parser.error('Project has existing resources; choose a new project')
    base = 'http://127.0.0.1:' + str(record['port'])
    password = (work / 'secrets/login-password').read_text().strip()
    if not re.fullmatch('[a-f0-9]{48}', password):
        parser.error('Unexpected generated fixture password format')
    try:
        build = run(compose + ['build', 'app'], timeout=180)
        (work / 'manual-build.txt').write_text(sanitize(build), encoding='utf-8')
        run(compose + ['up', '-d', '--wait', '--wait-timeout', '180'], timeout=210)
        psql = compose + ['exec', '-T', 'db', 'psql', '-v', 'ON_ERROR_STOP=1', '-d', 'koiki', '-At']
        run(psql + ['-U', 'postgres'], 'create extension if not exists pgcrypto;')
        for name in ('seed.sql', 'manual-seed.sql'):
            run(psql + ['-U', 'koiki'], (recipe / name).read_text().replace('__LOGIN_PASSWORD__', password))
        actual_hash = run(compose + ['exec', '-T', 'app', 'sha256sum', '/app/app.jar']).split()[0]
        if actual_hash != record['jar_sha256']:
            raise RuntimeError('Image JAR hash mismatch')
        for username, path in (('d3-applicant@example.test', '/spa-api/v1/expense-options'),
                               ('d3-approver@example.test', '/expenses/approvals')):
            opener = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(http.cookiejar.CookieJar()))
            with opener.open(base + '/login', timeout=10) as response:
                form = CsrfForm()
                form.feed(response.read().decode())
            if not form.token:
                raise RuntimeError('Login CSRF token missing')
            data = urllib.parse.urlencode(dict(username=username, password=password, _csrf=form.token)).encode()
            with opener.open(base + '/login', data=data, timeout=15) as response:
                if response.url != base + '/':
                    raise RuntimeError('Fixture login failed')
            with opener.open(base + path, timeout=10) as response:
                if response.status != 200 or response.url != base + path:
                    raise RuntimeError('Fixture route failed')
        status = run(compose + ['ps', '--format', 'json'])
        (work / 'manual-status.jsonl').write_text(status + '\n', encoding='utf-8')
        handoff = dict(project=record['project'], url=base + '/login', spa=base + '/spa/',
                       applicant='d3-applicant@example.test', approver='d3-approver@example.test',
                       password_file=str(work / 'secrets/login-password'), jar_sha256=actual_hash,
                       state='RUNNING_FOR_MANUAL_CHECK', http_login_checks='PASS; both users and selected routes')
        (work / 'manual-ready.json').write_text(json.dumps(handoff, indent=2) + '\n', encoding='utf-8')
        print(json.dumps(handoff, indent=2))
    except Exception as error:
        (work / 'manual-failure.txt').write_text(sanitize(str(error)), encoding='utf-8')
        try:
            logs = run(compose + ['logs', '--no-color'], timeout=15)
            (work / 'manual-failure-sanitized.log').write_text(sanitize(logs), encoding='utf-8')
        finally:
            run(compose + ['down', '--volumes', '--timeout', '40'], timeout=90)
        raise


if __name__ == '__main__':
    main()
