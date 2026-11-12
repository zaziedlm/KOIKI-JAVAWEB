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
    return subprocess.check_output(["git", "-C", str(root), *args], text=True, encoding="utf-8").strip()


def check_classifier(path, classifier, module, forbidden_paths):
    """Check actual source/document bytes, not merely classifier filenames."""
    with zipfile.ZipFile(path) as archive:
        names = archive.namelist()
        if len(names) != len(set(names)) or any(".." in Path(n).parts or n.startswith("/") or "\\" in n for n in names):
            raise ValueError(f"Unsafe or duplicate classifier entry: {path.name}")
        if classifier == "sources":
            java_root = module / "src/main/java"
            expected = {p.relative_to(java_root).as_posix(): p for p in java_root.rglob("*.java")}
            actual = {n for n in names if n.endswith(".java")}
            if not expected or actual != set(expected):
                raise ValueError(f"Source inventory mismatch: {path.name}")
            for name, source in expected.items():
                if archive.read(name) != source.read_bytes():
                    raise ValueError(f"Source content mismatch: {path.name}/{name}")
            if any(not n.endswith("/") and n not in expected and not n.startswith("META-INF/") for n in names):
                raise ValueError(f"Non-source payload in {path.name}")
        else:
            if "index.html" not in names or any("internal" in n.split("/") for n in names):
                raise ValueError(f"Missing Javadoc or internal package in {path.name}")
            expected_docs = []
            java_root = module / "src/main/java"
            for source in java_root.rglob("*.java"):
                relative = source.relative_to(java_root)
                if "internal" not in relative.parts and source.name != "package-info.java":
                    content = source.read_text(encoding="utf-8")
                    if re.search(r"\bpublic\s+(?:(?:final|abstract|sealed|non-sealed)\s+)*(?:class|interface|enum|record|@interface)\s+", content):
                        expected_docs.append(relative.with_suffix(".html").as_posix())
            if not expected_docs or not set(expected_docs).issubset(names):
                raise ValueError(f"Public API Javadoc coverage mismatch: {path.name}")
        for name in names:
            data = archive.read(name)
            if any(prefix in data for prefix in forbidden_paths) or b"-----BEGIN PRIVATE KEY-----" in data:
                raise ValueError(f"Local path or private key in {path.name}/{name}")
            if classifier == "javadoc" and name == "element-list" and b".internal" in data:
                raise ValueError(f"Internal package in {path.name}/element-list")


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
    ids = (Path(__file__).parent / "publish-unit.txt").read_text(encoding="utf-8").splitlines()
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
    config = json.loads((Path(__file__).parent / "classifier-inventory.json").read_text(encoding="utf-8"))
    source_ids, doc_ids = config["sources"], config["javadoc"]
    expected_sources = {a for a, (_, kind) in modules.items() if kind == "jar" and a != "koiki-testing"}
    if len(source_ids) != 11 or set(source_ids) != expected_sources or len(doc_ids) != 5 or len(set(doc_ids)) != 5 or not set(doc_ids) <= set(source_ids):
        raise ValueError("The approved A1 classifier inventory does not match the reactor")
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
            # CopyFile2 needs extended paths for long fixture names on Windows.
            source_path = str(original)
            target_path = str(target)
            if os.name == "nt":
                source_path = source_path if source_path.startswith("\\\\?\\") else "\\\\?\\" + source_path
                target_path = target_path if target_path.startswith("\\\\?\\") else "\\\\?\\" + target_path
            shutil.copy2(source_path, target_path)
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
        content = pom.read_text(encoding="utf-8")
        changed = content.replace("<version>0.1.0-SNAPSHOT</version>", f"<version>{args.version}</version>")
        if changed == content:
            raise ValueError(f"No baseline version in {module}")
        pom.write_text(changed, encoding="utf-8", newline="\n")
        generated[f"{module}/pom.xml"] = {"before": sha(root / module / "pom.xml"), "after": sha(pom)}
    # The staging reactor selects only the distribution modules. It is not a payload POM.
    text = (stage / "pom.xml").read_text(encoding="utf-8")
    replacement = "<modules>\n" + "".join(f"    <module>{modules[a][0]}</module>\n" for a in ids) + "  </modules>"
    (stage / "pom.xml").write_text(re.sub(r"<modules>.*?</modules>", replacement, text, flags=re.S), encoding="utf-8", newline="\n")
    generated["pom.xml"] = {"before": sha(root / "pom.xml"), "after": sha(stage / "pom.xml")}
    (output / "generated-poms.json").write_text(json.dumps(generated, indent=2) + "\n")
    wrapper = stage / ("mvnw.cmd" if os.name == "nt" else "mvnw")
    command = [str(wrapper), "-B", "-ntp", f"-Dmaven.repo.local={args.maven_repository.resolve()}",
               "-DargLine=-Xmx512m", "clean", "install"]
    if args.offline:
        command.insert(1, "-o")
    with (output / "build.log").open("w") as log:
        result = subprocess.run(command, cwd=stage, stdout=log, stderr=subprocess.STDOUT, timeout=2400)
    if result.returncode:
        raise RuntimeError(f"Candidate build failed ({result.returncode}); see {output / 'build.log'}")
    # Install is local to the explicit producer cache. A separate selected-module
    # invocation can then resolve preview dependencies without documenting internal modules.
    original_jars = {a: sha(stage / m / "target" / f"{a}-{args.version}.jar")
                     for a, (m, kind) in modules.items() if kind == "jar"}
    prefix = [str(wrapper), "-B", "-ntp", f"-Dmaven.repo.local={args.maven_repository.resolve()}"]
    if args.offline:
        prefix.append("-o")
    for classifier, selected, goal, options in (
        ("sources", source_ids, f"org.apache.maven.plugins:maven-source-plugin:{config['sourcePlugin']}:jar-no-fork",
         ["-Dmaven.source.excludeResources=true"]),
        ("javadoc", doc_ids, f"org.apache.maven.plugins:maven-javadoc-plugin:{config['javadocPlugin']}:jar",
         ["-DexcludePackageNames=*.internal:*.internal.*", f"-Ddoclint={config['doclint']}",
          "-DdetectLinks=false", "-DdetectJavaApiLink=false", "-Dnotimestamp=true", "-Dencoding=UTF-8", "-Ddocencoding=UTF-8", "-Dcharset=UTF-8"]),
    ):
        command = prefix + ["-pl", ",".join(modules[a][0] for a in selected), goal] + options
        with (output / f"{classifier}-build.log").open("w", encoding="utf-8") as log:
            result = subprocess.run(command, cwd=stage, stdout=log, stderr=subprocess.STDOUT, timeout=900)
        if result.returncode:
            raise RuntimeError(f"{classifier} generation failed ({result.returncode}); see {classifier}-build.log")
    for artifact, original_hash in original_jars.items():
        if sha(stage / modules[artifact][0] / "target" / f"{artifact}-{args.version}.jar") != original_hash:
            raise ValueError(f"Classifier generation changed the main JAR: {artifact}")
    (output / "main-jar-comparison.json").write_text(json.dumps({"before": original_jars, "afterIdentical": True}, indent=2) + "\n", encoding="utf-8")
    forbidden_paths = []
    for directory in (root, stage, Path.home(), args.maven_repository.resolve()):
        for spelling in (str(directory), directory.as_posix(), str(directory).replace("\\", "\\\\")):
            forbidden_paths.append(spelling.encode("utf-8"))
    bundle = output / f"koiki-framework-{args.version}"
    bundle.mkdir()
    artifacts = []
    files = []
    for artifact in ids:
        module, packaging = modules[artifact]
        relative = Path("maven-repository/org/koikifw") / artifact / args.version
        pom = stage / module / "pom.xml"
        if "0.1.0-SNAPSHOT" in pom.read_text(encoding="utf-8"):
            raise ValueError(f"Stale KOIKI version in {artifact}")
        coordinates = {"groupId": "org.koikifw", "artifactId": artifact, "version": args.version,
                       "packaging": packaging, "files": []}
        payloads = [(pom, f"{artifact}-{args.version}.pom", None)]
        if packaging == "jar":
            jar = stage / module / "target" / f"{artifact}-{args.version}.jar"
            with zipfile.ZipFile(jar) as z:
                metadata = z.read(f"META-INF/maven/org.koikifw/{artifact}/pom.properties").decode()
                if f"version={args.version}" not in metadata:
                    raise ValueError(f"Embedded version mismatch: {artifact}")
            payloads.append((jar, jar.name, None))
        for classifier, selected in (("sources", source_ids), ("javadoc", doc_ids)):
            if artifact in selected:
                attached = stage / module / "target" / f"{artifact}-{args.version}-{classifier}.jar"
                check_classifier(attached, classifier, stage / module, forbidden_paths)
                payloads.append((attached, attached.name, classifier))
        for source, name, classifier in payloads:
            dest = bundle / relative / name
            dest.parent.mkdir(parents=True, exist_ok=True)
            shutil.copy2(source, dest)
            entry = {"path": dest.relative_to(bundle).as_posix(), "classifier": classifier, "sha256": sha(dest), "size": dest.stat().st_size}
            coordinates["files"].append(entry)
            files.append(entry)
        artifacts.append(coordinates)
    for name in ["prepare-koiki.ps1", "README.md"]:
        shutil.copy2(Path(__file__).parent / name, bundle / name)
        item = bundle / name
        files.append({"path": name, "sha256": sha(item), "size": item.stat().st_size})
    if sum(len(a["files"]) for a in artifacts) != 42 or len(files) != 44:
        raise ValueError("Expected A1: 42 payloads and 44 manifest files")
    manifest = {"schemaVersion": 2, "version": args.version, "releaseTag": f"framework-{args.version}",
                "sourceCommit": provenance["commit"], "sourceTree": provenance["tree"],
                "sourceDirty": provenance["dirty"], "sourceInventorySha256": sha(output / "source-inventory.json"),
                "generatedPomsSha256": sha(output / "generated-poms.json"),
                "buildJava": subprocess.check_output(["java", "-version"], stderr=subprocess.STDOUT, text=True).strip(),
                "buildMaven": subprocess.check_output([str(wrapper), "-version"], cwd=stage, text=True, encoding="utf-8").strip(),
                "knownLimits": "Development preview; see README and version-specific acceptance evidence.",
                "documentationBuild": config, "artifacts": artifacts, "files": files}
    (bundle / "manifest.json").write_text(json.dumps(manifest, indent=2) + "\n")
    hashes = files + [{"path": "manifest.json", "sha256": sha(bundle / "manifest.json")}]
    (bundle / "SHA256SUMS").write_text("".join(f"{x['sha256']}  {x['path']}\n" for x in hashes))
    archive = output / f"{bundle.name}.zip"
    with zipfile.ZipFile(archive, "w", zipfile.ZIP_DEFLATED) as z:
        for f in sorted(bundle.rglob("*")):
            if f.is_file():
                z.write(f, f.relative_to(output).as_posix())
    (output / "SHA256SUMS").write_text(f"{sha(archive)}  {archive.name}\n")
    print(json.dumps({"archive": str(archive), "sha256": sha(archive), "coordinates": len(artifacts), "payloads": 42}))


if __name__ == "__main__":
    main()
