"""Export only reviewed S1 evidence paths; never archive .tmp wholesale."""
import argparse
import hashlib
import json
from pathlib import Path
import re
import zipfile
from preflight import load, write

ALLOWLIST = ['manifest.json', 'execution.json', 'results.json', 'cleanup.json', 'runtime-redacted.log']


def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('--work-dir', type=Path, required=True)
    p.add_argument('--output-dir', type=Path, required=True)
    a = p.parse_args()
    work, record, compose = load(a.work_dir)
    if (work / 'secrets').exists():
        p.error('Export only after generated-secret cleanup and redaction check')
    cleanup = json.loads((work / 'cleanup.json').read_text())
    if cleanup['status'] != 'PASS':
        p.error('Cleanup not established')
    out = a.output_dir.resolve()
    out.mkdir(parents=True, exist_ok=True)
    archive = out / (record['project'] + '-evidence.zip')
    if archive.exists():
        p.error('Refusing to overwrite an exported archive')
    payload = {}
    for name in ALLOWLIST:
        path = work / name
        if path.is_symlink() or not path.is_file():
            p.error('Missing or symlink allowlist entry: ' + name)
        content = path.read_text(encoding='utf-8')
        if re.search(r'eyJ[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+|-----BEGIN .*PRIVATE KEY-----|(?i:authorization:\s*bearer|set-cookie:)', content):
            p.error('Potential credential in reviewed export: ' + name)
        payload[name] = content.encode()
    inventory = {n: dict(bytes=len(v), sha256=hashlib.sha256(v).hexdigest()) for n, v in payload.items()}
    payload['archive-manifest.json'] = (json.dumps(inventory, indent=2) + '\n').encode()
    with zipfile.ZipFile(archive, 'x', compression=zipfile.ZIP_DEFLATED) as z:
        for name, content in payload.items():
            z.writestr(name, content)
    summary = dict(file=str(archive), bytes=archive.stat().st_size,
                   sha256=hashlib.sha256(archive.read_bytes()).hexdigest(), entries=list(payload),
                   status='EXPORTED; Windows receipt/checksum not confirmed')
    write(out / (record['project'] + '-export.json'), summary)
    print(json.dumps(summary, indent=2))


if __name__ == '__main__':
    main()
