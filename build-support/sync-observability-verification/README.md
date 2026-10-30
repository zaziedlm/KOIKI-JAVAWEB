# Synchronous observability verification (Tooling only)

Independent reactor. Neither this app nor its OTLP receiver belongs to a Framework
release, the normal Reference, or the root reactor. The app is loopback only and
has no production authentication, business, DB, async, or Collector-backend contract.

The app adds standard Boot4.1.1 OpenTelemetry tracing dependencies to the existing
KOIKI Observability Starter. There is no custom span generation or exporter.
The harness decodes real HTTP/protobuf OTLP with a test-only protocol model.
It runs the SAME Java21 app JAR in three sequential child processes:
Java21 export disabled, Java21 enabled, Java25 enabled with virtual threads.
One JUnit invocation must produce eight main and two VT smoke checkpoints.

Requires JAVA21_HOME/JAVA25_HOME and Maven on Java21. Stage the existing KOIKI
BOM/Parent and the unmodified Observability Starter in the local Maven repository.
Compile/package with tests skipped, inspect runtime dependency versions and the
protobuf test-model resolution, then freeze source and binary identity. No Docker.

```bash
./mvnw -B -ntp -f build-support/sync-observability-verification/pom.xml -DskipTests package
python3 build-support/sync-observability-verification/verification.py prepare --output "$D1_OUTPUT"
timeout --signal=TERM --kill-after=20s 600s ./mvnw -B -ntp \
  -f build-support/sync-observability-verification/harness/pom.xml \
  -DskipTests=false -Dtest=D1ConnectionTest -Dkoiki.d1.enabled=true \
  -Dkoiki.d1.manifest="$D1_OUTPUT/artifact-manifest.json" surefire:test
# Always run cleanup, even when Maven fails; do not automatically retry.
python3 build-support/sync-observability-verification/verification.py cleanup --output "$D1_OUTPUT"
python3 build-support/sync-observability-verification/verification.py verify --output "$D1_OUTPUT"
```

`D1_OUTPUT` must be an absolute NEW run directory outside the repository.
Keep MAVEN_OPTS and Surefire/child heaps <=512MiB. Scope: HTTP <=100, concurrent
requests <=4, OTLP requests <=100, decoded spans <=500, body <=1MiB,
app log <=2MiB per child, total evidence <=32MiB (frozen artifacts excluded).
Record the approved 40-minute execution budget. Unexpected survivors, source or
JAR mismatch, missing checkpoints, skipped verification, or synthetic secret
leakage prevent acceptance. Export outages may lose spans; recovery checks NEW
spans, not guaranteed replay. Raw environment and real secrets must not be captured.

A default Maven test run skips this opt-in test. That SKIP is not a verification
success. A failure preserves checkpoints already passed and bounded evidence;
there is no resume/retry logic. See the D1 plan §§8–13 and the execution result
for the scope, ownership, known limitations, resource cleanup, and actual outcome.

Initial execution: O04 passed, then the owned-child stdout reader treated normal
pipe closure during shutdown as an error. The helper is corrected and compiled;
no additional dynamic run has been performed. See [the result](../../docs/architecture/validation/phase4-d1-sync-observability-results-20261030.md)
for the preserved failed epoch and the proposed single additional execution.

The Owner-approved additional run passed O04 and O01, then failed O02 because
Micrometer's returned HTTP500 (no Java exception) produced `status=500`,
`outcome=SERVER_ERROR`, `exception=none`, and OTel span status UNSET. The old test
incorrectly required ERROR and OTel HTTP semantic attribute names. The harness now
checks the pinned convention's exact attributes plus HTTP/trace/log correlation.
Compilation passed; this expectation correction has NOT been dynamically rerun.
Both failed epochs and cleanup evidence are retained. All ten checkpoints remain
unverified; do not combine partial successes or infer backend/async guarantees.

Final bounded run (`status-aligned-execution`, 2026-10-30): JUnit 1 PASS,
0 failures/errors/skips; all TEN logical checkpoints passed in the same run.
Cleanup and source/JAR identity checks passed, with 35 HTTP requests and 10 OTLP
requests. The pinned consumer JAR is identical across all epochs. The archived
source manifest belongs to that execution; later README updates are documentation.
Prior failures remain historical. Framework/Reference/root/CI are unchanged.
The limited technical result awaits Owner acceptance; no further run is implied.
