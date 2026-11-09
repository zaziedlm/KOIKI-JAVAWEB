"""Prepare a fresh S1 work; never reuse another project's database or credentials."""
import argparse
import base64
import json
from pathlib import Path
import re
import secrets
import shutil
import subprocess
import zipfile
from preflight import ROOT, RECIPE, digest, write, resources


def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('--work-dir', type=Path, required=True)
    p.add_argument('--project', required=True)
    p.add_argument('--jar', type=Path, required=True)
    a = p.parse_args()
    work = a.work_dir.resolve()
    if not re.fullmatch(r'd3-sso-s1-\d{8}-[a-z0-9]+', a.project):
        p.error('Expected d3-sso-s1-YYYYMMDD-suffix')
    if work.parent != ROOT / '.tmp' or work.exists() or '\n' in str(work) or '"' in str(work):
        p.error('Expected a new work directly under repository .tmp')
    jar = a.jar.resolve(strict=True)
    with zipfile.ZipFile(jar) as z:
        assert 'BOOT-INF/classes/application-d3-container.properties' in z.namelist()
        assert not any('/spa/' in n or 'spring-modulith-events-jdbc-' in n for n in z.namelist())
    versions = json.loads((RECIPE / 'versions.json').read_text())
    for image in versions['images'].values():
        assert re.search(r'@sha256:[a-f0-9]{64}$', image['reference'])
        assert image['platform'] == 'linux/amd64'
    work.mkdir(mode=0o700, parents=True)
    (work / 'secrets').mkdir(mode=0o700)
    assert not any(resources(work, a.project).values()), 'Project already owns resources'
    for name in ['pg_admin', 'koiki_app', 'koiki_bff', 'koiki_keycloak', 'source_hmac']:
        value = base64.b64encode(secrets.token_bytes(32)).decode() if name == 'source_hmac' else secrets.token_hex(24)
        path = work / 'secrets' / name
        path.write_text(value)
        # Compose local secrets are bind mounts; UID10001/PG must be able to read files.
        # Both ancestor directories remain 0700 on the host.
        path.chmod(0o444)
    recipe = work / 'recipe'
    recipe.mkdir()
    for name in ['compose.yaml', 'init-db.sh', 'bff-migration.sql', 'reference-seed.sql', 'versions.json']:
        (recipe / name).write_bytes((RECIPE / name).read_bytes().replace(b'\r\n', b'\n'))
        (recipe / name).chmod(0o644)
    context = work / 'context'
    context.mkdir()
    shutil.copyfile(jar, context / 'app.jar')
    shutil.copyfile(RECIPE / 'Dockerfile.backend', context / 'Dockerfile')
    shutil.copyfile(RECIPE / '.dockerignore', context / '.dockerignore')
    jar_hash = digest(jar)
    env = dict(COMPOSE_PROJECT_NAME=a.project, D3_WORK=str(work).replace('\\', '/'),
               D3_IMAGE=a.project + ':local', D3_JAR_SHA256=jar_hash,
               D3_JAVA_IMAGE=versions['images']['java']['reference'],
               D3_PG_IMAGE=versions['images']['postgres']['reference'])
    (work / 'compose.env').write_text(''.join(f'{k}="{v}"\n' for k, v in env.items()))
    inventory = {str(f.relative_to(ROOT)): digest(f) for f in RECIPE.rglob('*')
                 if f.is_file() and not any(p in f.parts for p in ['node_modules', '__pycache__'])}
    record = dict(schema=1, phase='S1', project=a.project, work=str(work), image=env['D3_IMAGE'],
                  source_sha=subprocess.check_output(['git', 'rev-parse', 'HEAD'], cwd=ROOT, text=True).strip(),
                  source_files=inventory, jar_sha256=jar_hash, lock_sha256=digest(RECIPE / 'frontend/package-lock.json'),
                  versions=versions, recipe_hashes={f.name: digest(f) for f in recipe.iterdir()},
                  permitted_resources=dict(services=['db', 'backend'], network=a.project + '_database',
                                           volume=a.project + '_database'),
                  limits=dict(build_minutes=60, dynamic_minutes=40, cleanup_reserved_minutes=10,
                              runtime_cpu=2, runtime_memory_mib=2560, pg_connections=32),
                  validation=dict(direct_tests=2, checkpoint_ids=['S1-V01', 'S1-V02', 'S1-V03', 'S1-V04', 'S1-V05']))
    write(work / 'manifest.json', record)
    print(json.dumps(dict(project=a.project, jar_sha256=jar_hash, work=str(work))))


if __name__ == '__main__':
    main()
