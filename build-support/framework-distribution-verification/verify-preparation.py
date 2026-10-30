#!/usr/bin/env python3
"""Finite rejection checks against the real preparation entry point (no network)."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess
import tempfile
import zipfile


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def quote(value):
    return "'" + str(value).replace("'", "''") + "'"


def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument("--pwsh", required=True)
    p.add_argument("--maven", required=True)
    p.add_argument("--archive", required=True, type=Path)
    p.add_argument("--repository", required=True, type=Path,
                   help="An already prepared, isolated repository for reuse/conflict checks")
    p.add_argument("--registration-only", action="store_true",
                   help="Run only reuse/conflict checks; preserve earlier rejection evidence")
    args = p.parse_args()
    script = Path(__file__).resolve().parents[1] / "framework-distribution/prepare-koiki.ps1"
    version = "0.1.0-preview.1"
    results = []
    with tempfile.TemporaryDirectory(prefix="koiki-e1-rejections-") as temp:
        work = Path(temp)

        def invoke(name, archive, expected, message, remote=False, repository=None, expect_success=False):
            target = repository or work / name
            command = f"& {quote(script)} -Version {quote(version)} -ExpectedSha256 {quote(expected)} -LocalRepository {quote(target)} -Maven {quote(args.maven)}"
            if remote:
                command = "function gh { $global:LASTEXITCODE=42 }; " + command + " -Repository OWNER/koiki-artifacts"
            else:
                command += f" -LocalArchive {quote(archive)}"
            proc = subprocess.run([args.pwsh, "-NoProfile", "-Command", command], text=True,
                                  stdout=subprocess.PIPE, stderr=subprocess.STDOUT, timeout=90)
            passed = (proc.returncode == 0) if expect_success else (proc.returncode != 0 and message in proc.stdout)
            if not passed:
                raise AssertionError(f"{name}: exit={proc.returncode}\n{proc.stdout}")
            if not repository and target.exists():
                raise AssertionError(f"{name}: wrote installation destination before verification")
            results.append({"name": name, "status": "PASS", "exitCode": proc.returncode})
            print(f"{name} PASS", flush=True)

        if not args.registration_only:
            invoke("zip-hash-rejection", args.archive, "0" * 64, "Archive SHA-256 mismatch")
        modified = work / "modified.zip"
        with zipfile.ZipFile(args.archive) as source, zipfile.ZipFile(modified, "w") as dest:
            for entry in source.infolist():
                content = source.read(entry)
                if entry.filename.endswith("/koiki-starter-api-0.1.0-preview.1.jar"):
                    content += b"changed"
                dest.writestr(entry, content)
        if not args.registration_only:
            invoke("internal-payload-rejection", modified, digest(modified), "Payload mismatch")
        traversal = work / "traversal.zip"
        with zipfile.ZipFile(args.archive) as source, zipfile.ZipFile(traversal, "w") as dest:
            for entry in source.infolist():
                dest.writestr(entry, source.read(entry))
            dest.writestr(f"koiki-framework-{version}/../outside.txt", "must not extract")
        if not args.registration_only:
            invoke("archive-path-rejection", traversal, digest(traversal), "Unsafe archive path")
            invoke("download-failure-rejection", None, digest(args.archive), "Release asset download failed", remote=True)
        marker = args.repository / ".koiki-distributions" / f"{version}.json"
        original_marker = marker.read_bytes()
        invoke("identical-distribution-reuse", args.archive, digest(args.archive), "", repository=args.repository, expect_success=True)
        assert json.loads(marker.read_bytes()) == json.loads(original_marker)
        pom = args.repository / "org/koikifw/koiki-parent" / version / f"koiki-parent-{version}.pom"
        original = pom.read_bytes()
        try:
            pom.write_bytes(original + b"\n<!-- conflicting content -->\n")
            invoke("installed-coordinate-conflict", args.archive, digest(args.archive), "Conflicting installed coordinate", repository=args.repository)
            assert pom.read_bytes() == original + b"\n<!-- conflicting content -->\n"
            assert json.loads(marker.read_bytes()) == json.loads(original_marker)
        finally:
            pom.write_bytes(original)
    print(json.dumps({"checks": results, "newNetworkCalls": 0}, indent=2))


if __name__ == "__main__":
    main()
