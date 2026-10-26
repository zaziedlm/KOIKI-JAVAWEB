"""Meaningful negative guards for CI artifact identity and cleanup ownership."""
import argparse
import copy
import json
import os
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch
import zipfile

import verification as vt


class VerificationGuards(unittest.TestCase):
    def setUp(self):
        self.directory = tempfile.TemporaryDirectory()
        self.root = Path(self.directory.name)
        self.commit = "a" * 40
        self.manifest = {"schemaVersion": 1, "sourceCommit": self.commit, "workingTreeDirty": False,
                         "buildJavaFeature": 21, "runOwner": "b" * 32, "observationRuntime": []}
        (self.root / "source.py").write_text("frozen source\n")
        self.manifest["sourceRepository"] = str(self.root)
        self.manifest["toolingSources"] = {"source.py": vt.sha256(self.root / "source.py")}
        for name, entry in [("reference", "BOOT-INF/classes/org/koikifw/reference/ReferenceApplication.class"),
                            ("observer", "org/koikifw/buildsupport/vt/observer/VtObservationAutoConfiguration.class")]:
            jar = self.root / (name + ".jar")
            with zipfile.ZipFile(jar, "w") as archive:
                archive.writestr(entry, b"\xca\xfe\xba\xbe\x00\x00\x00\x41")
            self.manifest[name + "Jar"] = str(jar)
            self.manifest[name + "Sha256"] = vt.sha256(jar)
        for coordinate in ["org.koikifw:koiki-starter-observability:0.1.0-SNAPSHOT",
                           "io.micrometer:context-propagation:1.2.1",
                           "org.springframework.boot:spring-boot-actuator-autoconfigure:4.1.1"]:
            _, artifact, version = coordinate.split(":")
            jar = self.root / f"{artifact}-{version}.jar"
            with zipfile.ZipFile(jar, "w") as archive:
                archive.writestr("marker", coordinate)
            self.manifest["observationRuntime"].append({"coordinate": coordinate, "jar": str(jar), "sha256": vt.sha256(jar)})
        self.path = self.root / "manifest.json"
        self.save()

    def tearDown(self):
        self.directory.cleanup()

    def save(self):
        vt.write_json(self.path, self.manifest)

    def test_positive_identity(self):
        self.assertEqual(vt.verify_manifest(self.path, self.commit)["sourceCommit"], self.commit)

    def test_jar_tamper_rejected(self):
        Path(self.manifest["referenceJar"]).write_bytes(b"tampered")
        with self.assertRaisesRegex(ValueError, "hash mismatch"):
            vt.verify_manifest(self.path, self.commit)

    def test_other_commit_rejected(self):
        with self.assertRaisesRegex(ValueError, "commit mismatch"):
            vt.verify_manifest(self.path, "c" * 40)

    def test_java25_bytecode_rejected_even_if_hash_updated(self):
        jar = Path(self.manifest["referenceJar"])
        with zipfile.ZipFile(jar, "w") as archive:
            archive.writestr("BOOT-INF/classes/org/koikifw/reference/ReferenceApplication.class", b"\xca\xfe\xba\xbe\x00\x00\x00\x45")
        self.manifest["referenceSha256"] = vt.sha256(jar)
        self.save()
        with self.assertRaisesRegex(ValueError, "bytecode mismatch"):
            vt.verify_manifest(self.path, self.commit)

    def test_dirty_ci_cannot_be_overridden(self):
        self.manifest["workingTreeDirty"] = True
        self.save()
        with self.assertRaisesRegex(ValueError, "dirty source"):
            vt.verify_manifest(self.path, self.commit)
        with patch.dict(os.environ, {"GITHUB_ACTIONS": "true"}):
            with self.assertRaisesRegex(ValueError, "CI cannot allow"):
                vt.verify_manifest(self.path, self.commit, allow_dirty=True)

    def test_missing_or_duplicate_runtime_rejected(self):
        original = copy.deepcopy(self.manifest)
        self.manifest["observationRuntime"].pop()
        self.save()
        with self.assertRaisesRegex(ValueError, "runtime is absent"):
            vt.verify_manifest(self.path, self.commit)
        self.manifest = original
        self.manifest["observationRuntime"].append(self.manifest["observationRuntime"][0])
        self.save()
        with self.assertRaisesRegex(ValueError, "Duplicate runtime"):
            vt.verify_manifest(self.path, self.commit)

    def test_other_process_is_not_signalled(self):
        with self.assertRaisesRegex(ValueError, "another command"):
            vt.process_matches(os.getpid(), self.root / "another-run.jar", 0)

    def test_source_change_rejected(self):
        (self.root / "source.py").write_text("modified source\n")
        with self.assertRaisesRegex(ValueError, "source changed"):
            vt.verify_manifest(self.path, self.commit)

    def test_resumed_or_diagnostic_run_cannot_satisfy_ci(self):
        reports = self.root / "reports"
        reports.mkdir()
        (reports / "TEST-one.xml").write_text('<testsuite tests="5" failures="0" errors="0" skipped="0"/>')
        args = argparse.Namespace(manifest=self.path, expected_commit=self.commit, allow_dirty=False,
                                  reports=reports, evidence=self.root / "evidence")
        with self.assertRaisesRegex(ValueError, "six successful"):
            vt.summarize(args)


if __name__ == "__main__":
    unittest.main()
