#!/usr/bin/env python3
"""Freeze/verify the consumer and clean only recorded, positively identified children."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import shutil
import signal
import subprocess
import time
import uuid
import zipfile


def digest(path):
    return hashlib.sha256(Path(path).read_bytes()).hexdigest()


def sources(repo):
    root = repo / 'build-support/sync-observability-verification'
    return {str(p.relative_to(repo)): digest(p) for p in sorted(root.rglob('*'))
            if p.is_file() and 'target' not in p.parts and '__pycache__' not in p.parts}


def git(repo, *args):
    return subprocess.check_output(['git', '-C', str(repo), *args], text=True).strip()


def verify(path):
    data = json.loads(path.read_text())
    repo = Path(data['repository'])
    assert git(repo, 'rev-parse', 'HEAD') == data['commit'], 'Source commit changed'
    assert sources(repo) == data['sources'], 'Tooling sources changed'
    assert digest(data['jar']) == data['jarSha256'], 'Frozen JAR changed'
    assert digest(data['starter']) == data['starterSha256'], 'Starter changed'
    assert not git(repo, 'diff', '--name-only', 'HEAD', '--', 'koiki-parent', 'koiki-dependencies-bom',
                   'koiki-starters', 'koiki-libraries', 'koiki-reference-app', 'pom.xml'), 'Formal source changed'
    return data


def cleanup(data, output):
    evidence = output / 'evidence'
    unexpected = []
    for path in evidence.glob('*-ownership.properties'):
        props = dict(line.split('=', 1) for line in path.read_text().splitlines()
                     if line and not line.startswith('#') and '=' in line)
        assert props['owner'] == data['owner'], 'Foreign owner'
        assert props['jar'] == data['jar'], 'Foreign JAR'
        pid = int(props['pid'])
        proc = Path('/proc') / str(pid)
        if not proc.exists():
            continue
        args = (proc / 'cmdline').read_bytes().split(b'\0')
        if data['jar'].encode() not in args:
            raise RuntimeError('PID no longer identifies this app; refusing signal')
        fields = (proc / 'stat').read_text().rsplit(')', 1)[1].split()
        boot = int(next(line.split()[1] for line in Path('/proc/stat').read_text().splitlines()
                        if line.startswith('btime ')))
        start = 1000 * (boot + int(fields[19]) / os.sysconf('SC_CLK_TCK'))
        assert abs(start - int(props['startMillis'])) < 2000, 'PID start time mismatch'
        unexpected.append(pid)
        os.kill(pid, signal.SIGTERM)
        until = time.monotonic() + 10
        while proc.exists() and time.monotonic() < until:
            time.sleep(.1)
        if proc.exists():
            # Recheck command identity before escalation.
            assert data['jar'].encode() in (proc / 'cmdline').read_bytes().split(b'\0')
            os.kill(pid, signal.SIGKILL)
    (output / 'resource-cleanup.json').write_text(json.dumps({
        'owner': data['owner'], 'unexpectedChildren': unexpected,
        'normalCleanup': not unexpected}, indent=2) + '\n')
    assert not unexpected, 'Unexpected owned app remained; cleaned but run is not PASS'


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('action', choices=['prepare', 'verify', 'cleanup'])
    parser.add_argument('--repository', type=Path, default=Path.cwd())
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    output = args.output.resolve()
    manifest = output / 'artifact-manifest.json'
    if args.action == 'prepare':
        assert not manifest.exists(), 'Do not overwrite an execution epoch'
        repo = args.repository.resolve()
        for name in ['JAVA_TOOL_OPTIONS', 'JDK_JAVA_OPTIONS', '_JAVA_OPTIONS']:
            assert not os.environ.get(name), 'Unexpected injected JVM options'
        original = repo / 'build-support/sync-observability-verification/app/target/sync-observability-app-0.1.0-SNAPSHOT.jar'
        starter = repo / 'koiki-starters/koiki-starter-observability/target/koiki-starter-observability-0.1.0-SNAPSHOT.jar'
        with zipfile.ZipFile(original) as jar:
            app = jar.read('BOOT-INF/classes/org/koikifw/buildsupport/d1/D1Application.class')
            assert int.from_bytes(app[6:8], 'big') == 65, 'Consumer must target Java21'
            nested = jar.read('BOOT-INF/lib/koiki-starter-observability-0.1.0-SNAPSHOT.jar')
            assert hashlib.sha256(nested).hexdigest() == digest(starter), 'Embedded starter differs'
            runtime = sorted(n for n in jar.namelist() if n.startswith('BOOT-INF/lib/') and n.endswith('.jar'))
            assert not any('micrometer-registry-otlp' in n for n in runtime), 'Unexpected metric export'
            assert any('opentelemetry-exporter-otlp-' in n for n in runtime), 'Missing OTLP export'
        bundle = output / 'artifacts'
        bundle.mkdir(parents=True)
        frozen = bundle / original.name
        shutil.copyfile(original, frozen)
        data = {'schema': 1, 'repository': str(repo), 'commit': git(repo, 'rev-parse', 'HEAD'),
                'tree': git(repo, 'rev-parse', 'HEAD^{tree}'),
                'dirty': bool(git(repo, 'status', '--porcelain')),
                'trackedDiffSha256': hashlib.sha256(subprocess.check_output(['git', '-C', str(repo), 'diff', 'HEAD'])).hexdigest(),
                'owner': uuid.uuid4().hex, 'jar': str(frozen), 'jarSha256': digest(frozen),
                'starter': str(starter), 'starterSha256': digest(starter),
                'sources': sources(repo), 'runtimeJars': runtime}
        manifest.write_text(json.dumps(data, indent=2) + '\n')
        verify(manifest)
        print('Frozen Java21 consumer, embedded starter, source identity verified; dirty=' + str(data['dirty']))
    elif args.action == 'verify':
        verify(manifest)
        print('Source and JAR identity verified')
    else:
        data = json.loads(manifest.read_text())
        cleanup(data, output)
        print('No remaining owned app processes')


if __name__ == '__main__':
    main()
