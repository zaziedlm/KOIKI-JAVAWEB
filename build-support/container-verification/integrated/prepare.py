"""Stage an explicit executable JAR; never build it or mutate a customer's POM."""
import argparse
import base64
import hashlib
import json
from pathlib import Path
import re
import secrets
import shutil
import zipfile


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--jar', type=Path, required=True)
    parser.add_argument('--work-dir', type=Path, required=True)
    parser.add_argument('--project', required=True)
    parser.add_argument('--port', type=int, default=18081)
    parser.add_argument('--require-reference-spa', action='store_true')
    args = parser.parse_args()
    if not re.fullmatch(r'd3-[a-z0-9-]+', args.project) or not 1024 <= args.port <= 65535:
        parser.error('Use project d3-<lowercase-name> and port 1024..65535')
    jar = args.jar.resolve(strict=True)
    work = args.work_dir.resolve()
    if work.exists():
        parser.error('work-dir must be new; do not overwrite secrets or evidence')
    with zipfile.ZipFile(jar) as archive:
        names = archive.namelist()
        if 'BOOT-INF/classes/' not in names or 'META-INF/MANIFEST.MF' not in names:
            parser.error('Expected an executable Spring Boot JAR')
        if args.require_reference_spa:
            if 'META-INF/resources/spa/index.html' not in names:
                parser.error('Missing Reference SPA; build with b1-session-spa')
            build_record = json.loads(archive.read('META-INF/koiki-reference-spa-build.json'))
            for name, digest in build_record['assets'].items():
                if hashlib.sha256(archive.read('META-INF/resources/spa/' + name)).hexdigest() != digest:
                    parser.error('SPA asset differs from embedded build record')
            if any('spring-modulith-events-jdbc-' in name for name in names):
                parser.error('Async JAR is outside the integrated verification scope')
    context = work / 'context'
    secret_dir = work / 'secrets'
    context.mkdir(parents=True)
    secret_dir.mkdir()
    source = Path(__file__).resolve().parent
    for name in ('Dockerfile', '.dockerignore', 'probe.sh'):
        (context / name).write_bytes((source / name).read_bytes().replace(b'\r\n', b'\n'))
    shutil.copyfile(jar, context / 'app.jar')
    for name in ('pg-admin-password', 'app-db-password', 'login-password'):
        (secret_dir / name).write_text(secrets.token_hex(24), encoding='utf-8')
    (secret_dir / 'source-hmac').write_text(base64.b64encode(secrets.token_bytes(32)).decode(), encoding='utf-8')
    values = {
        'COMPOSE_PROJECT_NAME': args.project,
        'D3_APP_IMAGE': args.project + ':local',
        'D3_BUILD_CONTEXT': context.as_posix(),
        'D3_SECRET_DIR': secret_dir.as_posix(),
        'D3_HTTP_PORT': str(args.port),
    }
    (work / 'compose.env').write_text(''.join(f'{key}="{value}"\n' for key, value in values.items()), encoding='utf-8')
    record = dict(project=args.project, port=args.port, jar_sha256=hashlib.sha256(jar.read_bytes()).hexdigest(),
                  source_jar=str(jar), reference_spa=args.require_reference_spa)
    (work / 'artifact.json').write_text(json.dumps(record, indent=2) + '\n', encoding='utf-8')
    print(json.dumps(record))


if __name__ == '__main__':
    main()
