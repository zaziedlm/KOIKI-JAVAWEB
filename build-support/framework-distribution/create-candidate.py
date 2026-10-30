#!/usr/bin/env python3
"""Build a fixed KOIKI distribution in a new staging directory; never publish."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import xml.etree.ElementTree as ET
import zipfile

NS = {"m": "http://maven.apache.org/POM/4.0.0"}


def sha(path):
    return hashlib.file_digest(path.open("rb"), "sha256").hexdigest()


def git(root, *args):
    return subprocess.check_output(["git", "-C", str(root), *args], text=True).strip()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--version", required=True)
    parser.add_argument("--output", required=True, type=Path)
    parser.add_argument("--maven-repository", required=True, type=Path)
    parser.add_argument("--offline", action="store_true")
    args = parser.parse_args()
    if not re.fullmatch(r"\d+\.\d+\.\d+(?:-preview\.\d+)?", args.version):
        parser.error("Use a fixed x.y.z or x.y.z-preview.N version")
    root = Path(__file__).resolve().parents[2]
    output = args.output.resolve()
    if output.exists() or output.is_relative_to(root):
        parser.error("Output must be new and outside the source checkout")
    ids = (Path(__file__).parent / "publish-unit.txt").read_text().splitlines()
    reactor = ET.parse(root / "pom.xml").getroot()
    modules = {}
    for node in reactor.findall("m:modules/m:module", NS):
        pom = root / node.text / "pom.xml"
        model = ET.parse(pom).getroot()
        artifact = model.findtext("m:artifactId", namespaces=NS)
        if artifact in ids:
            modules[artifact] = (node.text, model.findtext("m:packaging", "jar", NS))
    if len(ids) != 14 or set(modules) != set(ids):
        raise ValueError("The approved 14-coordinate inventory does not match the reactor")
    output.mkdir(parents=True)
    stage = output / "source"
    paths = git(root, "ls-files", "-z", "--cached", "--others", "--exclude-standard").split("\0")
    source_files = {}
    for name in sorted(set(paths)):
        if not name:
            continue
        original = root / name
        if original.is_symlink():
            raise ValueError(f"Source symlink requires explicit review: {name}")
        if original.is_file():
            target = stage / name
            target.parent.mkdir(parents=True, exist_ok=True)
            shutil.copy2(original, target)
            source_files[name] = sha(original)
    provenance = {
        "commit": git(root, "rev-parse", "HEAD"),
        "tree": git(root, "rev-parse", "HEAD^{tree}"),
        "dirty": bool(git(root, "status", "--porcelain")),
        "files": source_files,
    }
    (output / "source-inventory.json").write_text(json.dumps(provenance, indent=2) + "\n")
    generated = {}
    for artifact, (module, _) in modules.items():
        pom = stage / module / "pom.xml"
        # Replace only explicitly KOIKI-owned version references in the selected POMs.
        content = pom.read_text()
        changed = content.replace("<version>0.1.0-SNAPSHOT</version>", f"<version>{args.version}</version>")
        if changed == content:
            raise ValueError(f"No baseline version in {module}")
        pom.write_text(changed)
        generated[f"{module}/pom.xml"] = {"before": sha(root / module / "pom.xml"), "after": sha(pom)}
    # The staging reactor selects only the distribution modules. It is not a payload POM.
    text = (stage / "pom.xml").read_text()
    replacement = "<modules>\n" + "".join(f"    <module>{modules[a][0]}</module>\n" for a in ids) + "  </modules>"
    (stage / "pom.xml").write_text(re.sub(r"<modules>.*?</modules>", replacement, text, flags=re.S))
    generated["pom.xml"] = {"before": sha(root / "pom.xml"), "after": sha(stage / "pom.xml")}
    (output / "generated-poms.json").write_text(json.dumps(generated, indent=2) + "\n")
    command = [str(stage / "mvnw"), "-B", "-ntp", f"-Dmaven.repo.local={args.maven_repository.resolve()}",
               "-DargLine=-Xmx512m", "clean", "package"]
    if args.offline:
        command.insert(1, "-o")
    with (output / "build.log").open("w") as log:
        result = subprocess.run(command, cwd=stage, stdout=log, stderr=subprocess.STDOUT, timeout=2400)
    if result.returncode:
        raise RuntimeError(f"Candidate build failed ({result.returncode}); see {output / 'build.log'}")
    bundle = output / f"koiki-framework-{args.version}"
    bundle.mkdir()
    artifacts = []
    files = []
    for artifact in ids:
        module, packaging = modules[artifact]
        relative = Path("maven-repository/org/koikifw") / artifact / args.version
        pom = stage / module / "pom.xml"
        if "0.1.0-SNAPSHOT" in pom.read_text():
            raise ValueError(f"Stale KOIKI version in {artifact}")
        coordinates = {"groupId": "org.koikifw", "artifactId": artifact, "version": args.version,
                       "packaging": packaging, "files": []}
        payloads = [(pom, f"{artifact}-{args.version}.pom")]
        if packaging == "jar":
            jar = stage / module / "target" / f"{artifact}-{args.version}.jar"
            with zipfile.ZipFile(jar) as z:
                metadata = z.read(f"META-INF/maven/org.koikifw/{artifact}/pom.properties").decode()
                if f"version={args.version}" not in metadata:
                    raise ValueError(f"Embedded version mismatch: {artifact}")
            payloads.append((jar, jar.name))
        for source, name in payloads:
            dest = bundle / relative / name
            dest.parent.mkdir(parents=True, exist_ok=True)
            shutil.copy2(source, dest)
            entry = {"path": dest.relative_to(bundle).as_posix(), "sha256": sha(dest), "size": dest.stat().st_size}
            coordinates["files"].append(entry)
            files.append(entry)
        artifacts.append(coordinates)
    for name in ["prepare-koiki.ps1", "README.md"]:
        shutil.copy2(Path(__file__).parent / name, bundle / name)
        item = bundle / name
        files.append({"path": name, "sha256": sha(item), "size": item.stat().st_size})
    manifest = {"schemaVersion": 1, "version": args.version, "releaseTag": f"framework-{args.version}",
                "sourceCommit": provenance["commit"], "sourceTree": provenance["tree"],
                "sourceDirty": provenance["dirty"], "sourceInventorySha256": sha(output / "source-inventory.json"),
                "generatedPomsSha256": sha(output / "generated-poms.json"),
                "buildJava": subprocess.check_output(["java", "-version"], stderr=subprocess.STDOUT, text=True).strip(),
                "buildMaven": subprocess.check_output([str(stage / "mvnw"), "-version"], cwd=stage, text=True).strip(),
                "knownLimits": "Development preview; see README and version-specific acceptance evidence.",
                "artifacts": artifacts, "files": files}
    (bundle / "manifest.json").write_text(json.dumps(manifest, indent=2) + "\n")
    hashes = files + [{"path": "manifest.json", "sha256": sha(bundle / "manifest.json")}]
    (bundle / "SHA256SUMS").write_text("".join(f"{x['sha256']}  {x['path']}\n" for x in hashes))
    archive = output / f"{bundle.name}.zip"
    with zipfile.ZipFile(archive, "w", zipfile.ZIP_DEFLATED) as z:
        for f in sorted(bundle.rglob("*")):
            if f.is_file():
                z.write(f, f.relative_to(output).as_posix())
    (output / "SHA256SUMS").write_text(f"{sha(archive)}  {archive.name}\n")
    print(json.dumps({"archive": str(archive), "sha256": sha(archive), "coordinates": len(artifacts), "payloads": 26}))


if __name__ == "__main__":
    main()
