#!/usr/bin/env python3
"""Run one isolated E1 Consumer DB journey; always remove the owned container."""
import argparse
import json
import os
from pathlib import Path
import secrets
import shutil
import subprocess
import threading
import time
import uuid


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--repository", required=True, type=Path)
    parser.add_argument("--output", required=True, type=Path)
    parser.add_argument("--offline", action="store_true")
    args = parser.parse_args()
    root = Path(__file__).resolve().parents[2]
    output = args.output.resolve()
    if output.exists():
        parser.error("Use a new evidence directory")
    output.mkdir(parents=True)
    consumer = output / "consumer"
    shutil.copytree(root / "build-support/framework-distribution-consumer", consumer,
                    ignore=shutil.ignore_patterns("target"))
    name = "koiki-e1-" + uuid.uuid4().hex[:12]
    env = os.environ.copy()
    admin_password = secrets.token_hex(24)
    env["POSTGRES_PASSWORD"] = admin_password
    env["PGPASSWORD"] = admin_password
    application_password = secrets.token_hex(24)
    env["E1_DB_PASSWORD"] = application_password
    stopped = threading.Event()
    samples = []
    created = False
    monitor = None
    status = {"container": name, "status": "RUNNING", "cleanup": "PENDING"}

    def docker(*command, input_text=None, check=True):
        return subprocess.run(["docker", "--host=unix:///var/run/docker.sock", *command],
                              env=env, input=input_text, text=True, stdout=subprocess.PIPE,
                              stderr=subprocess.PIPE, timeout=25, check=check)

    def sample():
        while not stopped.is_set():
            try:
                r = docker("exec", "-e", "PGPASSWORD", name, "psql", "-U", "postgres", "-d", "e1", "-Atc",
                           "select count(*) from pg_stat_activity where backend_type='client backend'", check=False)
                if r.returncode == 0:
                    samples.append({"epoch": time.time(), "dbClientsIncludingMonitor": int(r.stdout.strip())})
            except (subprocess.SubprocessError, ValueError):
                pass
            stopped.wait(2)

    try:
        docker("run", "-d", "--name", name, "--label", "koiki.verification=e1",
               "-e", "POSTGRES_PASSWORD", "-e", "POSTGRES_DB=e1",
               "-p", "127.0.0.1::5432", "postgres:17-alpine", "-c", "max_connections=16")
        created = True
        for _ in range(30):
            if docker("exec", name, "pg_isready", "-U", "postgres", "-d", "e1", check=False).returncode == 0:
                break
            time.sleep(1)
        else:
            raise RuntimeError("Owned PostgreSQL did not become ready")
        sql = (f"CREATE ROLE e1_consumer LOGIN PASSWORD '{application_password}' NOSUPERUSER NOCREATEDB NOCREATEROLE;\n"
               "REVOKE ALL ON DATABASE e1 FROM PUBLIC;\n"
               "GRANT CONNECT ON DATABASE e1 TO e1_consumer;\n"
               "REVOKE CREATE ON SCHEMA public FROM PUBLIC;\n"
               "GRANT USAGE, CREATE ON SCHEMA public TO e1_consumer;\n")
        docker("exec", "-i", "-e", "PGPASSWORD", name, "psql", "-v", "ON_ERROR_STOP=1", "-U", "postgres", "-d", "e1", input_text=sql)
        port = docker("port", name, "5432/tcp").stdout.strip().rsplit(":", 1)[1]
        env["E1_JDBC_URL"] = f"jdbc:postgresql://127.0.0.1:{port}/e1"
        monitor = threading.Thread(target=sample, daemon=True)
        monitor.start()
        base = [str(root / "mvnw"), "-B", "-ntp", f"-Dmaven.repo.local={args.repository.resolve()}"]
        if args.offline:
            base.append("-o")
        command = base + ["-DargLine=-Xmx512m", "-DforkCount=1", "-Djunit.jupiter.execution.parallel.enabled=false", "clean", "verify"]
        with (output / "consumer.log").open("w") as log:
            code = subprocess.run(command, cwd=consumer, env=env, stdout=log, stderr=subprocess.STDOUT, timeout=600).returncode
        if code:
            raise RuntimeError(f"Consumer failed ({code}); inspect consumer.log")
        with (output / "dependency-tree.log").open("w") as log:
            code = subprocess.run(base + ["org.apache.maven.plugins:maven-dependency-plugin:3.8.1:tree", "-Dincludes=org.koikifw"],
                                  cwd=consumer, env=env, stdout=log, stderr=subprocess.STDOUT, timeout=180).returncode
        if code:
            raise RuntimeError("Consumer dependency tree failed")
        if samples and max(x["dbClientsIncludingMonitor"] for x in samples) > 14:
            raise RuntimeError("DB client budget exceeded")
        status["status"] = "PASS"
    except Exception as error:
        status["status"] = "FAIL"
        # Avoid rendering CalledProcessError command/stdin: database setup contains fixture passwords.
        status["error"] = type(error).__name__ if isinstance(error, subprocess.SubprocessError) else str(error)
        raise
    finally:
        stopped.set()
        if monitor:
            monitor.join(30)
        if created:
            result = docker("rm", "-f", name, check=False)
            inspect = docker("inspect", name, check=False)
            status["cleanup"] = "PASS" if result.returncode == 0 and inspect.returncode != 0 else "FAIL"
        else:
            status["cleanup"] = "NOT_CREATED"
        status["dbClientSamples"] = samples
        (output / "result.json").write_text(json.dumps(status, indent=2) + "\n")
        print(json.dumps({k: v for k, v in status.items() if k != "dbClientSamples"}))
        if status["cleanup"] == "FAIL":
            raise RuntimeError("Owned container cleanup failed")


if __name__ == "__main__":
    main()
