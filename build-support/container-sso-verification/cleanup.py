"""Remove only the resources of an S1 execution whose ownership was recorded."""
import argparse
import json
from pathlib import Path
from preflight import load, resources, command, write


def cleanup(work, record, compose):
    execution = json.loads((work / 'execution.json').read_text())
    if execution['project'] != record['project'] or not execution['started_with_empty_project']:
        raise RuntimeError('Cleanup is not armed for this project')
    before = resources(work, record['project'])
    for cid in before['containers']:
        item = json.loads(command(work, ['docker', 'inspect', cid]).stdout)[0]
        labels = item['Config']['Labels']
        if labels['com.docker.compose.service'] not in record['permitted_resources']['services'] or Path(labels['com.docker.compose.project.working_dir']).resolve() != work / 'recipe':
            raise RuntimeError('Container ownership mismatch')
    if not set(before['volumes']) <= {record['permitted_resources']['volume']} or not set(before['networks']) <= {record['permitted_resources']['network']}:
        raise RuntimeError('Volume/network ownership mismatch')
    command(work, compose + ['down', '--volumes', '--timeout', '40'], timeout=120)
    after = resources(work, record['project'])
    if any(after.values()):
        write(work / 'cleanup.json', dict(status='HOLD', remaining=after))
        raise RuntimeError('Owned resources remain; secrets retained')
    for path in (work / 'secrets').iterdir():
        if path.is_symlink() or not path.is_file():
            raise RuntimeError('Unexpected secret path')
        path.unlink()
    (work / 'secrets').rmdir()
    if (work / 'realm/koiki-d3.json').exists():
        (work / 'realm/koiki-d3.json').unlink()
        (work / 'realm').rmdir()
    result = dict(status='PASS', before=before, remaining=after, generated_secrets_deleted=True)
    write(work / 'cleanup.json', result)
    return result


if __name__ == '__main__':
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('--work-dir', type=Path, required=True)
    a = p.parse_args()
    print(json.dumps(cleanup(*load(a.work_dir))))
