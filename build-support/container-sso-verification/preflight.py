"""Shared S1 ownership, command, redaction and preflight routines (Tooling only)."""
import argparse
import hashlib
import json
from pathlib import Path
import re
import subprocess

RECIPE = Path(__file__).resolve().parent
ROOT = RECIPE.parents[1]


def digest(path):
    return hashlib.sha256(Path(path).read_bytes()).hexdigest()


def tree_digest(directory):
    root=Path(directory)
    result=hashlib.sha256()
    for path in sorted(p for p in root.rglob('*') if p.is_file()):
        result.update(str(path.relative_to(root)).replace('\\','/').encode())
        result.update(bytes.fromhex(digest(path)))
    return result.hexdigest()


def write(path, value):
    Path(path).write_text(json.dumps(value, indent=2, ensure_ascii=False) + '\n', encoding='utf-8')


def redact(work, value):
    for path in (work / 'secrets').glob('*'):
        if path.is_file():
            secret = path.read_text().strip()
            if secret:
                value = value.replace(secret, '[REDACTED]')
    value = re.sub(r'eyJ[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+', '[REDACTED-JWT]', value)
    return value


def command(work, args, data=None, timeout=60, check=True):
    r = subprocess.run(args, input=data, capture_output=True, text=True, encoding='utf-8', timeout=timeout)
    if check and r.returncode:
        raise RuntimeError(redact(work, r.stderr[-4000:] or r.stdout[-4000:]))
    return r


def load(path):
    work = Path(path).resolve(strict=True)
    record = json.loads((work / 'manifest.json').read_text())
    if work.parent != ROOT / '.tmp' or record['work'] != str(work):
        raise ValueError('Expected direct owned work directory under repository .tmp')
    stage = record.get('phase', 'S1')
    if stage not in ['S1', 'S2', 'S3', 'S4'] or not re.fullmatch(r'd3-sso-' + stage.lower() + r'-\d{8}-[a-z0-9]+', record['project']):
        raise ValueError('Wrong stage/project')
    if work.is_symlink() or (work / 'manifest.json').is_symlink():
        raise ValueError('Symlink work is forbidden')
    for name, expected in record['recipe_hashes'].items():
        if digest(work / 'recipe' / name) != expected:
            raise ValueError('Prepared recipe changed: ' + name)
    compose = ['docker', 'compose', '--project-name', record['project'], '--env-file', str(work / 'compose.env'),
               '-f', str(work / 'recipe/compose.yaml')]
    if stage in ['S2', 'S3', 'S4']:
        compose += ['-f', str(work / 'recipe/compose-s2.yaml')]
    if stage in ['S3', 'S4']:
        compose += ['-f', str(work / 'recipe/compose-s3.yaml')]
    if stage=='S4':compose += ['-f',str(work/'recipe/compose-s4.yaml')]
    return work, record, compose


def resources(work, project):
    label = 'label=com.docker.compose.project=' + project
    return {kind: command(work, args).stdout.splitlines() for kind, args in {
        'containers': ['docker', 'ps', '-aq', '--filter', label],
        'volumes': ['docker', 'volume', 'ls', '-q', '--filter', label],
        'networks': ['docker', 'network', 'ls', '--format', '{{.Name}}', '--filter', label]}.items()}


def sql(work, compose, role, database, query, check=True):
    # Generated credentials stay in files/stdin; never put their values into argv or evidence.
    if role not in ['koiki_app', 'koiki_bff', 'koiki_keycloak'] or database not in ['koiki_app', 'koiki_bff', 'koiki_keycloak']:
        raise ValueError('Unapproved DB/role')
    shell = 'export PGPASSWORD="$(cat /run/secrets/' + role + ')"; exec psql -h 127.0.0.1 -U ' + role + ' -d ' + database + ' -v ON_ERROR_STOP=1 -Atq'
    return command(work, compose + ['exec', '-T', 'db', 'sh', '-ec', shell], data=query, check=check)


def preflight(work, record, compose):
    for name, expected in record['source_files'].items():
        if digest(ROOT / name) != expected:
            raise ValueError('Execution source changed after prepare: ' + name)
    if any(resources(work, record['project']).values()):
        raise RuntimeError('Refusing a pre-existing project, before cleanup is armed')
    if (work / 'execution.json').exists() or (work / 'results.json').exists():
        raise RuntimeError('Work already executed; use fresh work')
    assert digest(work / 'context/app.jar') == record['jar_sha256']
    assert digest(RECIPE / 'frontend/package-lock.json') == record['lock_sha256']
    selected = compose + (['--profile', 'helper'] if record['phase'] in ['S2', 'S3', 'S4'] else [])
    command(work, selected + ['config', '--quiet'])
    config = json.loads(command(work, selected + ['config', '--format', 'json']).stdout)
    assert set(config['services']) == set(record['permitted_resources']['services'])
    assert config['services']['backend']['tmpfs'] == ['/tmp:size=134217728,mode=1777'], 'tmpfs must be one absolute mount specification'
    for name, service in config['services'].items():
        if name in ['db', 'backend'] or record['phase'] == 'S1':
            assert not service.get('ports'), 'DB/backend must not publish host ports'
        else:
            ports = service.get('ports', [])
            expected = '18080' if name == 'keycloak' else '13000'
            assert len(ports) == 1 and ports[0]['host_ip'] == '127.0.0.1' and str(ports[0]['published']) == expected
        assert not service.get('external_links')
    assert record['versions']['images']['postgres']['index_digest'] in config['services']['db']['image']
    image = json.loads(command(work, ['docker', 'image', 'inspect', record['image']]).stdout)[0]
    assert image['Config']['Labels']['org.koikifw.d3.source-jar-sha256'] == record['jar_sha256']
    assert image['Os'] == 'linux' and image['Architecture'] == 'amd64'
    assert image['Config']['User'] == '10001:10001'
    if record['phase'] in ['S2', 'S3', 'S4']:
        assert digest(work / 'realm/koiki-d3.json') == record['realm_sha256']
        for name, expected in record['helper_hashes'].items():
            assert digest(work / 'helper' / name) == expected
        for name in ['node', 'keycloak']:
            item = json.loads(command(work, ['docker', 'image', 'inspect', record['versions']['images'][name]['reference']]).stdout)[0]
            assert item['Architecture'] == 'amd64' and item['Os'] == 'linux'
    if record['phase'] in ['S3', 'S4']:
        assert tree_digest(work / 'frontend-context') == record['frontend_sha256']
        front=json.loads(command(work,['docker','image','inspect',record['frontend_image']]).stdout)[0]
        assert front['Config']['Labels']['org.koikifw.d3.frontend-sha256']==record['frontend_sha256']
        assert front['Config']['User']=='10001:10001'
    if record['phase']=='S4':
        realm=json.loads((work/'realm/koiki-d3.json').read_text())
        for rp in ['a','b']:
            client=next(c for c in realm['clients'] if c['clientId']=='koiki-d3-'+rp)
            assert client['redirectUris']==['http://localhost:13000/'+rp+'/auth/callback']
            assert client['attributes']['post.logout.redirect.uris']=='http://localhost:13000/'+rp+'/auth/logout-return'
        assert config['services']['frontend']['environment']['BFF_RPS']=='a,b'
    return dict(image_id=image['Id'], configuration='S1:2 services/no host ports; S2:3 services+transient helper/loopback only', platform='linux/amd64')


def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('--work-dir', type=Path, required=True)
    a = p.parse_args()
    work, record, compose = load(a.work_dir)
    print(json.dumps(preflight(work, record, compose)))


if __name__ == '__main__':
    main()
