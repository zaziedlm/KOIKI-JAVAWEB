"""Build only S1's Reference JAR and two explicit probe security tests."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess
import xml.etree.ElementTree as ET
import zipfile

ROOT = Path(__file__).resolve().parents[2]


def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('--output', type=Path, required=True)
    p.add_argument('--settings', type=Path)
    p.add_argument('--toolchains', type=Path)
    a = p.parse_args()
    out = a.output.resolve()
    out.mkdir(parents=True, exist_ok=False)
    versions = {}
    for tool, flags, pattern in [('java', ['-version'], r'version "21\.'),
                                  ('javac', ['-version'], r'javac 21\.'),
                                  ('node', ['--version'], r'^v24\.19\.0$'),
                                  ('npm.cmd' if os.name == 'nt' else 'npm', ['--version'], r'^11\.9\.0$')]:
        r = subprocess.run([tool, *flags], capture_output=True, text=True, check=True, timeout=20)
        value = (r.stdout + r.stderr).strip()
        if not re.search(pattern, value):
            raise RuntimeError('Wrong toolchain: ' + tool)
        versions[tool] = value
    command = [str(ROOT / ('mvnw.cmd' if os.name == 'nt' else 'mvnw'))]
    for flag, value in [('-s', a.settings), ('-t', a.toolchains)]:
        if value:
            command += [flag, str(value.resolve(strict=True))]
    command += ['-B', '-ntp', '-pl', 'koiki-reference-app', '-am', '-P', 'd3-container', 'clean', 'package',
                '-Dtest=ReferenceContainerSecurityConfigurationTest', '-Dsurefire.failIfNoSpecifiedTests=false',
                '-DargLine=-Xmx768m']
    (out / 'command.json').write_text(json.dumps(dict(command=command, tools=versions), indent=2) + '\n')
    with (out / 'build.log').open('w', encoding='utf-8') as log:
        subprocess.run(command, cwd=ROOT, stdout=log, stderr=subprocess.STDOUT, check=True, timeout=1200)
    reports = ROOT / 'koiki-reference-app/target/surefire-reports'
    suites = [ET.parse(f).getroot() for f in reports.glob('TEST-*.xml')]
    assert sum(int(s.get('tests', 0)) for s in suites) == 2
    assert sum(int(s.get(k, 0)) for s in suites for k in ['failures', 'errors', 'skipped']) == 0
    jar = ROOT / 'koiki-reference-app/target/koiki-reference-app-0.1.0-SNAPSHOT.jar'
    with zipfile.ZipFile(jar) as z:
        assert not any('/spa/' in n or 'spring-modulith-events-jdbc-' in n for n in z.namelist())
    record = dict(jar=str(jar), jar_sha256=hashlib.sha256(jar.read_bytes()).hexdigest(),
                  tests=2, failures=0, errors=0, skipped=0, profile='d3-container')
    (out / 'build-result.json').write_text(json.dumps(record, indent=2) + '\n')
    print(json.dumps(record))


if __name__ == '__main__':
    main()
