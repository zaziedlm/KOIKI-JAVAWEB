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
    p.add_argument("--version", default="0.1.0-preview.2")
    args = p.parse_args()
    script = Path(__file__).resolve().parents[1] / "framework-distribution/prepare-koiki.ps1"
    version = args.version
    results = []
    with tempfile.TemporaryDirectory(prefix="koiki-e1-rejections-") as temp:
        work = Path(temp)

        def invoke(name, archive, expected, message, remote=False, repository=None, expect_success=False):
            target = repository or work / name
            command = f"& {quote(script)} -Version {quote(version)} -ExpectedSha256 {quote(expected)} -LocalRepository {quote(target)} -Maven {quote(args.maven)} -Offline"
            if remote:
                command = "function gh { $global:LASTEXITCODE=42 }; " + command + " -Repository OWNER/koiki-artifacts"
            else:
                command += f" -LocalArchive {quote(archive)}"
            command = "[Console]::OutputEncoding = [Text.UTF8Encoding]::new(); " + command
            proc = subprocess.run([args.pwsh, "-NoProfile", "-Command", command], text=True, encoding="utf-8",
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
        prefix = f"koiki-framework-{version}/"
        def mutate(name, transform):
            destination = work / f"{name}.zip"
            with zipfile.ZipFile(args.archive) as source:
                contents = {item.filename: source.read(item) for item in source.infolist()}
            transform(contents)
            with zipfile.ZipFile(destination, "w") as target:
                for name, content in contents.items():
                    target.writestr(name, content)
            return destination

        def tamper(contents, suffix):
            matching = [name for name in contents if name.endswith('/' + suffix)]
            assert len(matching) == 1
            contents[matching[0]] += b"changed"

        modified = mutate("modified-main", lambda content: tamper(content, f"koiki-starter-api-{version}.jar"))
        if not args.registration_only:
            invoke("internal-payload-rejection", modified, digest(modified), "Payload mismatch")
            for classifier, artifact in (("sources", "koiki-starter-api"), ("javadoc", "koiki-starter-session-jdbc")):
                suffix = f"{artifact}-{version}-{classifier}.jar"
                changed = mutate(f"modified-{classifier}", lambda contents, suffix=suffix: tamper(contents, suffix))
                invoke(f"{classifier}-tamper-rejection", changed, digest(changed), "Payload mismatch")
                def remove(contents, suffix=suffix):
                    names = [n for n in contents if n.endswith('/' + suffix)]
                    assert len(names) == 1
                    del contents[names[0]]
                missing = mutate(f"missing-{classifier}", remove)
                invoke(f"{classifier}-missing-rejection", missing, digest(missing), "Missing distribution file")

            def unknown_classifier(contents):
                key = prefix + "manifest.json"
                manifest = json.loads(contents[key])
                matching = [r for r in manifest["files"] if r["path"].endswith(f"koiki-starter-api-{version}-sources.jar")]
                assert len(matching) == 1
                old = matching[0]["path"]
                new = old.replace("-sources.jar", "-tests.jar")
                contents[prefix + new] = contents.pop(prefix + old)
                for record in manifest["files"] + [r for a in manifest["artifacts"] for r in a["files"]]:
                    if record["path"] == old:
                        record["path"], record["classifier"] = new, "tests"
                contents[key] = json.dumps(manifest).encode()
            unknown = mutate("unknown-classifier", unknown_classifier)
            invoke("unexpected-classifier-rejection", unknown, digest(unknown), "Unexpected classifier payload")

            def old_schema(contents):
                key = prefix + "manifest.json"
                manifest = json.loads(contents[key])
                manifest["schemaVersion"] = 1
                contents[key] = json.dumps(manifest).encode()
            old = mutate("old-schema", old_schema)
            invoke("old-schema-rejection", old, digest(old), "Manifest schema mismatch")
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
        for classifier in ("sources", "javadoc"):
            artifact = "koiki-starter-session-jdbc"
            payload = args.repository / "org/koikifw" / artifact / version / f"{artifact}-{version}-{classifier}.jar"
            assert payload.resolve().is_relative_to(args.repository.resolve())
            original = payload.read_bytes()
            try:
                payload.write_bytes(original + b"conflict")
                invoke(f"installed-{classifier}-conflict", args.archive, digest(args.archive), "Conflicting installed coordinate", repository=args.repository)
                assert payload.read_bytes() == original + b"conflict"
                assert json.loads(marker.read_bytes()) == json.loads(original_marker)
            finally:
                payload.write_bytes(original)
            try:
                payload.unlink()
                invoke(f"missing-installed-{classifier}-recovery", args.archive, digest(args.archive), "", repository=args.repository, expect_success=True)
                assert payload.read_bytes() == original
                assert json.loads(marker.read_bytes()) == json.loads(original_marker)
            finally:
                if not payload.exists():
                    payload.write_bytes(original)
    print(json.dumps({"checks": results, "newNetworkCalls": 0}, indent=2))


if __name__ == "__main__":
    main()
