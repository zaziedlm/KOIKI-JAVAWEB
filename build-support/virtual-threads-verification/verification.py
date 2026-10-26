#!/usr/bin/env python3
"""Prepare, verify, summarize and clean the non-distributed VT verification bundle."""

import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import signal
import struct
import subprocess
import sys
import time
import uuid
import xml.etree.ElementTree as ET
import zipfile


CONFIGURATIONS = {
    "A-native": {"N01"}, "A-observed": {f"V{i:02}" for i in range(1, 13)},
    "B-native": {"N01"}, "B-observed": {f"V{i:02}" for i in range(1, 13)} | {"L01", "L02"},
    "C-native": {"N01"}, "C-observed": {f"V{i:02}" for i in range(1, 13)} | {"L01", "L02"},
}


def require(condition, message):
    if not condition:
        raise ValueError(message)


def sha256(path):
    with Path(path).open("rb") as stream:
        return hashlib.file_digest(stream, "sha256").hexdigest()


def command(*args, cwd=None):
    return subprocess.check_output(args, cwd=cwd, text=True, stderr=subprocess.PIPE).strip()


def write_json(path, value):
    Path(path).write_text(json.dumps(value, indent=2, ensure_ascii=False) + "\n")


def class_major(path, entry):
    with zipfile.ZipFile(path) as archive:
        data = archive.read(entry)
    require(data[:4] == b"\xca\xfe\xba\xbe", "Invalid class file")
    return struct.unpack(">H", data[6:8])[0]


def prepare(args):
    repository = args.repository.resolve()
    commit = command("git", "rev-parse", "HEAD", cwd=repository)
    require(commit == args.expected_commit, "Checkout does not match the expected commit")
    dirty = bool(command("git", "status", "--porcelain", "--untracked-files=all", cwd=repository))
    require(not args.allow_dirty or os.environ.get("GITHUB_ACTIONS") != "true", "CI cannot allow dirty source")
    require(not dirty or args.allow_dirty, "Freeze a clean source commit before CI verification")
    require(not any(os.environ.get(key) for key in ["JAVA_TOOL_OPTIONS", "JDK_JAVA_OPTIONS", "_JAVA_OPTIONS"]),
            "Unexpected inherited JVM options")
    properties = subprocess.run(
        [str(args.java21_home / "bin/java"), "-XshowSettings:properties", "-version"],
        capture_output=True, text=True, check=True).stderr
    require(re.search(r"java\.specification\.version\s*=\s*21\b", properties), "Build JDK must be Java 21")
    version = re.search(r"java\.runtime\.version\s*=\s*(\S+)", properties)
    require(version is not None, "Missing build runtime version")
    output = args.output.resolve()
    output.mkdir(parents=True, exist_ok=True)
    artifacts = output / "artifacts"
    artifacts.mkdir()  # Never overwrite a previous epoch.
    own = {
        "reference": (repository / "koiki-reference-app/target/koiki-reference-app-0.1.0-SNAPSHOT.jar",
                      "BOOT-INF/classes/org/koikifw/reference/ReferenceApplication.class"),
        "observer": (repository / "build-support/virtual-threads-verification/observer/target/virtual-threads-observer-0.1.0-SNAPSHOT.jar",
                     "org/koikifw/buildsupport/vt/observer/VtObservationAutoConfiguration.class"),
    }
    manifest = {"schemaVersion": 1, "sourceCommit": commit,
                "sourceTree": command("git", "rev-parse", "HEAD^{tree}", cwd=repository),
                "workingTreeDirty": dirty, "buildJavaFeature": 21,
                "buildJavaVersion": version[1], "runOwner": uuid.uuid4().hex,
                "observationRuntime": [], "observationEmbedded": []}
    for name, (source, entry) in own.items():
        require(class_major(source, entry) == 65, f"{name} is not Java 21 bytecode")
        destination = artifacts / source.name
        shutil.copyfile(source, destination)
        manifest[name + "Jar"] = str(destination)
        manifest[name + "Sha256"] = sha256(destination)
    with zipfile.ZipFile(manifest["referenceJar"]) as reference:
        embedded = {}
        for entry in reference.namelist():
            if entry.startswith("BOOT-INF/lib/") and entry.endswith(".jar"):
                match = re.fullmatch(r"(.+)-(\d+\..+)\.jar", Path(entry).name)
                require(match is not None, "Unrecognized embedded library name")
                embedded[match[1]] = (match[2], entry)
        coordinates = set()
        for text in args.classpath.read_text().strip().split(os.pathsep):
            source = Path(text).resolve()
            parts = source.relative_to(args.maven_repository.resolve()).parts
            require(len(parts) >= 4, "Runtime artifact is outside the Maven repository layout")
            group, artifact, version = ".".join(parts[:-3]), parts[-3], parts[-2]
            coordinate = f"{group}:{artifact}:{version}"
            require((group, artifact) not in coordinates, "Duplicate runtime coordinate")
            coordinates.add((group, artifact))
            require(source.name == f"{artifact}-{version}.jar", "Unexpected classifier or runtime filename")
            digest = sha256(source)
            if artifact in embedded:
                previous, entry = embedded[artifact]
                require(previous == version, f"Runtime version conflict for {artifact}")
                require(hashlib.sha256(reference.read(entry)).hexdigest() == digest, f"Runtime payload conflict for {artifact}")
                manifest["observationEmbedded"].append({"coordinate": coordinate, "embeddedEntry": entry, "sha256": digest})
            else:
                if artifact == "koiki-starter-observability":
                    source = repository / "koiki-starters/koiki-starter-observability/target" / source.name
                    require(sha256(source) == digest, "Local Starter differs from this build")
                destination = artifacts / source.name
                require(not destination.exists(), "Artifact filename collision")
                shutil.copyfile(source, destination)
                manifest["observationRuntime"].append({"coordinate": coordinate, "jar": str(destination), "sha256": digest})
    sources = [p for p in (repository / "build-support/virtual-threads-verification").rglob("*")
               if p.is_file() and "target" not in p.parts and "__pycache__" not in p.parts]
    manifest["toolingSources"] = {str(p.relative_to(repository)): sha256(p) for p in sorted(sources)}
    manifest["sourceRepository"] = str(repository)
    write_json(output / "artifact-manifest.json", manifest)
    print(f"Prepared {len(manifest['observationRuntime'])} additional and {len(manifest['observationEmbedded'])} reused JARs; source {commit}")


def verify_manifest(path, expected_commit, allow_dirty=False):
    manifest = json.loads(Path(path).read_text())
    require(manifest.get("schemaVersion") == 1, "Unsupported manifest schema")
    require(manifest.get("sourceCommit") == expected_commit and re.fullmatch(r"[0-9a-f]{40}", expected_commit), "Manifest commit mismatch")
    require(not allow_dirty or os.environ.get("GITHUB_ACTIONS") != "true", "CI cannot allow dirty source")
    require(not manifest["workingTreeDirty"] or allow_dirty, "Manifest describes dirty source")
    require(manifest["buildJavaFeature"] == 21, "Manifest build JDK mismatch")
    require(re.fullmatch(r"[0-9a-f]{32}", manifest["runOwner"]) is not None, "Invalid resource owner")
    source = Path(manifest["sourceRepository"])
    require(bool(manifest["toolingSources"]), "No frozen Tooling source")
    for relative, expected in manifest["toolingSources"].items():
        path = (source / relative).resolve()
        require(path.is_relative_to(source.resolve()), "Source path escapes the checkout")
        require(sha256(path) == expected, "Tooling source changed after preparation")
    for name, entry in [("reference", "BOOT-INF/classes/org/koikifw/reference/ReferenceApplication.class"),
                        ("observer", "org/koikifw/buildsupport/vt/observer/VtObservationAutoConfiguration.class")]:
        require(sha256(manifest[name + "Jar"]) == manifest[name + "Sha256"], f"{name} JAR hash mismatch")
        require(class_major(manifest[name + "Jar"], entry) == 65, f"{name} bytecode mismatch")
    required = {"org.koikifw:koiki-starter-observability", "io.micrometer:context-propagation",
                "org.springframework.boot:spring-boot-actuator-autoconfigure"}
    seen = set()
    for row in manifest["observationRuntime"]:
        parts = row["coordinate"].split(":")
        require(len(parts) == 3, "Invalid runtime coordinate")
        coordinate = ":".join(parts[:2])
        require(coordinate not in seen, "Duplicate runtime coordinate")
        seen.add(coordinate)
        require(Path(row["jar"]).name == f"{parts[1]}-{parts[2]}.jar", "Runtime filename mismatch")
        require(sha256(row["jar"]) == row["sha256"], "Runtime JAR hash mismatch")
    require(required <= seen, "Required observation runtime is absent")
    return manifest


def summarize(args):
    manifest = verify_manifest(args.manifest, args.expected_commit, args.allow_dirty)
    reports = list(args.reports.glob("TEST-*.xml"))
    require(bool(reports), "No JUnit reports")
    totals = {key: 0 for key in ["tests", "failures", "errors", "skipped"]}
    for path in reports:
        root = ET.parse(path).getroot()
        for key in totals:
            totals[key] += int(root.get(key, 0))
    require(totals == {"tests": 6, "failures": 0, "errors": 0, "skipped": 0}, "CI requires six successful invocations, no resumed or diagnostic run")
    result = {"sourceCommit": manifest["sourceCommit"], "junit": totals, "configurations": {}}
    for name, expected in CONFIGURATIONS.items():
        folder = args.evidence / name
        actual = (folder / "checkpoints.txt").read_text().splitlines()
        require(set(actual) == {point + " PASS" for point in expected} and len(actual) == len(expected), f"Incomplete checkpoints: {name}")
        require((folder / "cleanup.txt").is_file(), f"Cleanup evidence missing: {name}")
        row = {"checkpoints": len(expected), "cleanup": True}
        if name.endswith("observed"):
            records = [line.split("|") for line in (folder / "observations.tsv").read_text().splitlines()]
            handlers = [line for line in records if line[0] == "H"]
            require(bool(handlers) and all(line[3] == str(name.startswith("C-")).lower() for line in handlers), f"Unexpected request thread: {name}")
            require(all(line[2] == "true" for line in records if line[0] == "F"), "Context restoration failed")
            require(not any(line[0] == "FAIL" for line in records) and records[-1][0] == "END", "Observer output incomplete")
            row["handlerCount"] = len(handlers)
            if name[0] in "BC":
                summary = (folder / "jfr-summary.txt").read_text()
                for event in ["VirtualThreadSubmitFailed", "InitialEnvironmentVariable", "InitialSystemProperty", "ProcessStart"]:
                    match = re.search(r"(?m)^\s*jdk\." + event + r"\s+(\d+)", summary)
                    require(match is not None and int(match[1]) == 0, f"Unsafe or failed JFR event: {event}")
        result["configurations"][name] = row
    result["logicalCheckpoints"] = sum(row["checkpoints"] for row in result["configurations"].values())
    require(result["logicalCheckpoints"] == 43, "Expected 43 logical checkpoints")
    write_json(args.evidence.parent / "summary.json", result)
    print("Six invocations and 43 checkpoints passed; immutable artifacts verified")


def process_matches(pid, expected_jar, start_ms):
    """Require both this run's unique JAR argument and the recorded process birth time."""
    process = Path("/proc") / str(pid)
    if not process.exists():
        return False
    argv = (process / "cmdline").read_bytes().split(b"\0")
    require(str(expected_jar).encode() in argv, "PID belongs to another command; refusing to signal")
    stat = (process / "stat").read_text().rsplit(")", 1)[1].split()
    boot = int(re.search(r"(?m)^btime (\d+)$", Path("/proc/stat").read_text())[1])
    actual_ms = (boot + int(stat[19]) / os.sysconf("SC_CLK_TCK")) * 1000
    require(abs(actual_ms - start_ms) < 2000, "PID was reused; refusing to signal")
    return True


def cleanup(args):
    manifest = json.loads(args.manifest.read_text())
    owner = manifest.get("runOwner", "")
    require(re.fullmatch(r"[0-9a-f]{32}", owner) is not None, "Cannot clean an unverifiable resource owner")
    unexpected = []
    sessions = set()
    for file in args.evidence.glob("*/database-ownership.json"):
        row = json.loads(file.read_text())
        require(row["owner"] == owner, "Database ownership mismatch")
        if row.get("sessionId"):
            sessions.add(row["sessionId"])
    for file in args.evidence.glob("*/ownership.txt"):
        row = dict(line.split("=", 1) for line in file.read_text().splitlines())
        require(row.get("owner") == owner, "Process ownership mismatch")
        pid = int(row["pid"])
        if process_matches(pid, Path(manifest["referenceJar"]), int(row["processStartMillis"])):
            unexpected.append(f"app:{pid}")
            os.kill(pid, signal.SIGTERM)
            for _ in range(50):
                if not Path(f"/proc/{pid}").exists():
                    break
                time.sleep(.1)
            if Path(f"/proc/{pid}").exists():
                require(process_matches(pid, Path(manifest["referenceJar"]), int(row["processStartMillis"])), "Process identity changed")
                os.kill(pid, signal.SIGKILL)
    containers = command("docker", "ps", "-aq", "--filter", f"label=org.koikifw.vt.owner={owner}").split()
    for container in containers:
        labels = json.loads(command("docker", "inspect", "--format", "{{json .Config.Labels}}", container))
        require(labels.get("org.koikifw.vt.owner") == owner, "Container owner changed")
        if labels.get("org.testcontainers.sessionId"):
            sessions.add(labels["org.testcontainers.sessionId"])
        unexpected.append(f"database:{container}")
        subprocess.run(["docker", "rm", "-f", container], check=True, stdout=subprocess.DEVNULL)
    for session in sessions:
        require(re.fullmatch(r"[A-Za-z0-9-]{1,80}", session) is not None, "Invalid Testcontainers session")
        ids = command("docker", "ps", "-aq", "--filter", f"name=^/testcontainers-ryuk-{session}$").split()
        for container in ids:
            image = command("docker", "inspect", "--format", "{{.Config.Image}}", container)
            require(re.search(r"(^|/)ryuk(:|@)", image) is not None, "Unexpected helper image")
            subprocess.run(["docker", "rm", "-f", container], check=True, stdout=subprocess.DEVNULL)
    write_json(args.evidence.parent / "resource-cleanup.json", {"owner": owner, "unexpectedResources": unexpected,
               "testcontainersSessions": sorted(sessions), "ownedResourcesCleaned": True})
    require(not unexpected, "Unexpected residual resources were cleaned; verification remains failed")
    print("No remaining owned app or database resources; owned helper cleanup complete")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    sub = parser.add_subparsers(dest="operation", required=True)
    prep = sub.add_parser("prepare")
    prep.add_argument("--repository", type=Path, required=True)
    prep.add_argument("--output", type=Path, required=True)
    prep.add_argument("--classpath", type=Path, required=True)
    prep.add_argument("--maven-repository", type=Path, required=True)
    prep.add_argument("--java21-home", type=Path, required=True)
    prep.add_argument("--expected-commit", required=True)
    prep.add_argument("--allow-dirty", action="store_true")
    for name in ["verify", "summarize", "cleanup"]:
        item = sub.add_parser(name)
        item.add_argument("--manifest", type=Path, required=True)
        if name != "cleanup":
            item.add_argument("--expected-commit", required=True)
            item.add_argument("--allow-dirty", action="store_true")
        if name != "verify":
            item.add_argument("--evidence", type=Path, required=True)
        if name == "summarize":
            item.add_argument("--reports", type=Path, required=True)
    args = parser.parse_args()
    if args.operation == "verify":
        verify_manifest(args.manifest, args.expected_commit, args.allow_dirty)
        print("Manifest and artifact identity verified")
    else:
        globals()[args.operation](args)


if __name__ == "__main__":
    try:
        main()
    except (ValueError, OSError, subprocess.CalledProcessError, zipfile.BadZipFile) as error:
        print(f"VT verification failed: {error}", file=sys.stderr)
        sys.exit(1)
