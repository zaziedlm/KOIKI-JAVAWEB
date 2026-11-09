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
    p.add_argument('--stage', choices=['S1', 'S2', 'S3', 'S4'], default='S1')
    a = p.parse_args()
    work = a.work_dir.resolve()
    if not re.fullmatch(r'd3-sso-' + a.stage.lower() + r'-\d{8}-[a-z0-9]+', a.project):
        p.error('Project must match selected stage and date')
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
    secret_names = ['pg_admin', 'koiki_app', 'koiki_bff', 'koiki_keycloak', 'source_hmac']
    if a.stage in ['S2', 'S3', 'S4']:
        secret_names += ['client_a', 'client_wrong', 'login_password']
    if a.stage in ['S3', 'S4']:
        secret_names += ['bff_key']
    if a.stage=='S4':secret_names += ['client_b']
    for name in secret_names:
        value = base64.b64encode(secrets.token_bytes(32)).decode() if name in ['source_hmac', 'bff_key'] else secrets.token_hex(24)
        path = work / 'secrets' / name
        path.write_text(value)
        # Compose local secrets are bind mounts; UID10001/PG must be able to read files.
        # Both ancestor directories remain 0700 on the host.
        path.chmod(0o444)
    recipe = work / 'recipe'
    recipe.mkdir()
    recipe_names = ['compose.yaml', 'init-db.sh', 'bff-migration.sql', 'reference-seed.sql', 'versions.json']
    if a.stage in ['S2', 'S3', 'S4']:
        recipe_names += ['compose-s2.yaml', 'realm-template.json']
    if a.stage in ['S3', 'S4']:
        recipe_names += ['compose-s3.yaml', 'compose-key-missing.yaml']
    if a.stage=='S4':recipe_names += ['compose-s4.yaml']
    for name in recipe_names:
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
    helper_hashes = {}
    if a.stage in ['S2', 'S3', 'S4']:
        env.update(D3_KEYCLOAK_IMAGE=versions['images']['keycloak']['reference'], D3_NODE_IMAGE=versions['images']['node']['reference'])
        realm_object=json.loads((RECIPE / 'realm-template.json').read_text())
        if a.stage!='S4':realm_object['clients']=[c for c in realm_object['clients'] if c['clientId']!='koiki-d3-b']
        realm=json.dumps(realm_object)
        if a.stage=='S4':realm=realm.replace('__CLIENT_B_SECRET__',(work/'secrets/client_b').read_text())
        for marker, name in [('__CLIENT_A_SECRET__', 'client_a'), ('__CLIENT_WRONG_SECRET__', 'client_wrong'), ('__LOGIN_PASSWORD__', 'login_password')]:
            realm = realm.replace(marker, (work / 'secrets' / name).read_text())
        (work / 'realm').mkdir(mode=0o755)
        (work / 'realm/koiki-d3.json').write_text(realm)
        (work / 'realm/koiki-d3.json').chmod(0o644)
        helper = work / 'helper'
        (helper / 'frontend/lib').mkdir(parents=True)
        for src, dst in [(RECIPE / 'verify-oidc.ts', helper / 'verify-oidc.ts'),
                         (RECIPE / 'frontend/lib/oidc.ts', helper / 'frontend/lib/oidc.ts'),
                         (RECIPE / 'frontend/package.json', helper / 'frontend/package.json')]:
            shutil.copyfile(src, dst)
        modules = RECIPE / 'frontend/node_modules'
        queue, copied = ['openid-client'], set()
        while queue:
            name = queue.pop()
            if name in copied:
                continue
            copied.add(name)
            package = json.loads((modules / name / 'package.json').read_text())
            shutil.copytree(modules / name, helper / 'frontend/node_modules' / name)
            queue.extend(package.get('dependencies', {}))
        for path in [helper, *helper.rglob('*')]:
            path.chmod(0o755 if path.is_dir() else 0o644)
            if path.is_file():
                helper_hashes[str(path.relative_to(helper))] = digest(path)
    frontend_sha = None
    if a.stage in ['S3', 'S4']:
        frontend = work / 'frontend-context'
        frontend.mkdir()
        shutil.copytree(RECIPE / 'frontend/.next/standalone', frontend / 'standalone')
        shutil.copytree(RECIPE / 'frontend/.next/static', frontend / 'static')
        shutil.copyfile(RECIPE / 'Dockerfile.frontend', frontend / 'Dockerfile.frontend')
        shutil.copyfile(RECIPE / 'frontend/startup.mjs', frontend / 'startup.mjs')
        (frontend / '.dockerignore').write_text('*\n!Dockerfile.frontend\n!standalone/\n!standalone/**\n!static/\n!static/**\n!startup.mjs\n')
        from preflight import tree_digest
        frontend_sha = tree_digest(frontend)
        env.update(D3_FRONTEND_IMAGE=a.project + ':frontend', D3_FRONTEND_SHA256=frontend_sha)
    (work / 'compose.env').write_text(''.join(f'{k}="{v}"\n' for k, v in env.items()))
    inventory = {str(f.relative_to(ROOT)): digest(f) for f in RECIPE.rglob('*')
                 if f.is_file() and not any(p in f.parts for p in ['node_modules', '__pycache__', '.next']) and not f.name.endswith('.tsbuildinfo') and f.name!='next-env.d.ts'}
    record = dict(schema=1, phase=a.stage, project=a.project, work=str(work), image=env['D3_IMAGE'],
                  source_sha=subprocess.check_output(['git', 'rev-parse', 'HEAD'], cwd=ROOT, text=True).strip(),
                  source_files=inventory, jar_sha256=jar_hash, lock_sha256=digest(RECIPE / 'frontend/package-lock.json'),
                  versions=versions, recipe_hashes={f.name: digest(f) for f in recipe.iterdir()},
                  permitted_resources=dict(services=['db', 'backend'], network=a.project + '_database',
                                           volume=a.project + '_database'),
                  limits=dict(build_minutes=60, dynamic_minutes=40, cleanup_reserved_minutes=10,
                              runtime_cpu=2, runtime_memory_mib=2560, pg_connections=32),
                  validation=dict(direct_tests=2, checkpoint_ids=['S1-V01', 'S1-V02', 'S1-V03', 'S1-V04', 'S1-V05']))
    if a.stage in ['S2', 'S3', 'S4']:
        record['helper_hashes'] = helper_hashes
        record['realm_sha256'] = digest(work / 'realm/koiki-d3.json')
        record['permitted_resources']['services'] = ['db', 'backend', 'keycloak', 'helper']
        record['limits'] = dict(build_minutes=20, dynamic_minutes=45, cleanup_reserved_minutes=10,
                                runtime_cpu=3.5, runtime_memory_mib=4608, pg_connections=32)
        record['validation'] = dict(direct_tests=23, checkpoint_ids=['S2-V01', 'S2-V02', 'S2-V03', 'S2-V04'])
    if a.stage in ['S3', 'S4']:
        record['frontend_sha256'] = frontend_sha
        record['frontend_image'] = env['D3_FRONTEND_IMAGE']
        record['permitted_resources']['services'].append('frontend')
        record['limits'] = dict(build_minutes=40,dynamic_minutes=50,cleanup_reserved_minutes=10,
                                runtime_cpu=4,runtime_memory_mib=5120,probe_cpu=0.5,probe_exclusive_with_frontend=True,pg_connections=32)
        record['validation'] = dict(direct_tests=0,java_unchanged=True,unit_boundaries=21,checkpoint_ids=['S3-V01','S3-V02','S3-V03','S3-V04','S3-V05'])
    if a.stage=='S4':
        record['limits'].update(build_minutes=25,dynamic_minutes=35)
        record['authorization']='Owner: S4へ進めましょう; Framework非改変の同一集合修正後再実行許可を継承; commit/remote/S5は対象外'
        record['validation_commands']=['node --experimental-strip-types --test frontend/test/*.test.mjs:29 cases,0 skip','python3 -m unittest discover -s . -p test_s1.py -v:5 cases','node node_modules/next/dist/bin/next build:1 successful build','docker compose build frontend:1 invocation','python3 verify-s4.py --work-dir <fresh>:S4-V01..04,worker1,contexts<=2']
        record['validation']=dict(direct_tests=0,java_unchanged=True,unit_boundaries=29,checkpoint_ids=['S4-V01','S4-V02','S4-V03','S4-V04'])
    write(work / 'manifest.json', record)
    print(json.dumps(dict(project=a.project, jar_sha256=jar_hash, work=str(work))))


if __name__ == '__main__':
    main()
