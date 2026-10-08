# Phase 4 Level 2 verification fixture

Tooling-owned, non-distributed PL2 fixture. It is outside the Root Reactor and is not a Framework,
Reference Application, Customer Application, or release artifact. The Maven profiles `jdbc` (default)
and `jpa` compare Spring Modulith publication stores against the same PostgreSQL schema and JPA
business transaction. Production migration and API ownership are not decided by this fixture.

Validation covers PUBLISHED and PROCESSING publication state and restart recovery, provider-side
idempotency after a listener failure or a process kill after accepted send, FAILED metrics and resubmission,
completed-publication purge, scheduled stale publication marking, and selection of publication
registry trigger annotations. A fixture-level resubmission filter demonstrates a completion-attempt
limit. The FAILED age gauge measures time since original publication, not time since FAILED transition.
The multi-JVM characterization test observes two processes entering the same listener for one
publication during restart replay; a passing test records that gap, not an exclusive-delivery guarantee.
The PUBLISHED crash test uses a conditional fixture executor to stop before async listener invocation;
its initial completion-attempt count is already one, so the count is not a listener-entry indicator.
A separate Tooling-only candidate disables automatic restart replay and runs explicit recovery under
a PostgreSQL session advisory lock. Its integration test checks recovery-worker contention, process
kill release, and clean release. It does not prove safety against a still-running ordinary listener.
Separate characterization tests show that an explicit recovery can enter a live ordinary listener,
and that losing only the lock's PostgreSQL session allows a second recovery while the first JVM
continues. Passing characterization tests record those gaps; they do not certify safe delivery.
An opt-in guarded recovery probe requires an external stop-confirmation file bound to publication ID,
event ID, observed status, and attempt count. It rejects missing confirmation and stale DB observations.
While waiting for completion, it polls its lock connection and halts its own JVM on loss.
The file is an operator assertion, not proof that the original listener stopped; polling also leaves
a short interval before halt. These fixture checks do not establish distributed fencing.
Production tracing and exporter correlation remain open. `Rule28And29CandidateTest` compares the current
distributed Rule 28 / Rule 1 behavior with Tooling-only Level 2 Rule 28 and direct-dependency Rule 29
candidates. It requires no Docker; the candidate does not define a Framework rule-selection API.
The indirect listener to Use Case to Port to outbound Adapter route remains a review and runtime-test
responsibility. Results belong in
`docs/architecture/validation/phase4-pl2-level2-verification.md`.

V3 adds a Tooling-only Flyway V2 marker for the time a publication enters FAILED. It compares
FAILED transition age with publication age, and checks event/publication IDs across the initial
async listener and a job-triggered retry. A single reused listener thread and captured Logback MDC
events verify that one request's context does not leak into the next. The trace ID in this fixture
is a synthetic MDC marker; the test does not exercise an OpenTelemetry tracer, exporter, alert, or
separate-JVM correlation. The marker column, PostgreSQL trigger, executor, and metric names are
not production migration or Framework contracts.

The V5 integration test compares two migration placements: KOIKI-owned publication schema followed
by Application business schema, and Application-owned publication plus business schema after a KOIKI
marker migration. It runs Flyway directly with the Data Starter's KOIKI-first order and separate
history names, checks an independent KOIKI upgrade and PostgreSQL rollback of a failed migration.
It does not run the Data Starter itself, define a production migration, benchmark stores, or select
the schema owner.

Run each profile independently using the repository Maven Wrapper:

```powershell
.\mvnw.cmd -f build-support/phase4-level2-verification/pom.xml -Pjdbc verify
.\mvnw.cmd -f build-support/phase4-level2-verification/pom.xml -Pjpa verify
```

Run the V4 architecture candidate alone without Docker:

```powershell
.\mvnw.cmd -f build-support/phase4-level2-verification/pom.xml -Pjdbc -Dtest=Rule28And29CandidateTest test
```

The V4 test uses the locally installed `koiki-archunit-rules` snapshot. On a clean Maven cache,
install the current Root Reactor artifact before running this standalone Tooling module:

```powershell
.\mvnw.cmd -pl koiki-archunit-rules -am -DskipTests install
```

Tests use a disposable `postgres:17-alpine` Testcontainers instance. Docker access is required.

## S1 B-1 limited read verification

B1 fixtures are test-only and outside the Root Reactor. They read supplier-owned views with a
SELECT-only role; they never invoke Reference permit operations or external delivery.
The six B1 classes require explicit `koiki.b1.resource-limits.enabled=true`. They remain disabled
in ordinary, unselected Tooling runs. No B1 helper, SQL, or reader entry belongs in the ordinary JAR.

Compile offline with existing test-only support profiles; runtime uses `jdbc` alone and direct goals:

```powershell
.\mvnw.cmd -o -B -ntp -f build-support/phase4-level2-verification/pom.xml '-Pjdbc,s1-contract,s1-web' -DskipTests test-compile
.\mvnw.cmd -o -B -ntp -f build-support/phase4-level2-verification/pom.xml -Pjdbc '-Dkoiki.b1.resource-limits.enabled=true' '-DargLine=-Xmx768m' '-DforkCount=1' '-DreuseForks=false' '-Djunit.jupiter.execution.parallel.enabled=false' '-Dtest=B1TargetReadTest' surefire:test
```

Set `MAVEN_OPTS=-Xmx768m` only for each run and restore it in `finally`. Execute one class at a time,
reserve its time and cleanup budget, preserve sanitized failure evidence, and confirm all managed
children, pools, DB/Ryuk, ports, and temporary files have ended before starting the next class.
The integration class uses `-Dit.test=B1ReadConnectionIT` with direct
`failsafe:integration-test failsafe:verify`. Preserve raw outside `target` before any clean package.

**Current status:** the bounded verification is complete: B1 52 cases, selected PL2 regression 6,
Reference related 143, existing Reference 99, and packaged E2E 1 all pass with cleanup confirmed.
Clean packages exclude the fixture. Earlier failures are preserved, including the Agent runner
pool override mistake corrected under Owner-approved sections 27/28. The B1 bounded implementation
and verification results are COMPLETE / OWNER APPROVED (2026-10-08, Evidence section 24).
B-2, production use, distribution, and remote actions require their own decisions.
See [start/stop contract](../../docs/development/phase4-s1-stage-b1-read-connection-limited-start-review-20261008.md)
and [case, resource, failure, cleanup evidence](../../docs/architecture/validation/phase4-s1-stage-b1-read-connection-20261008.md).
Neither observed termination nor a provider row proves drain, distributed fencing, safe retry,
complete coverage, B-2 readiness, or production delivery safety. D11/D12 and I05 remain explicit gaps.

## S1 B-2 initial issue/read verification

B2 connects a Reference-owned, explicitly enabled JDBC Adapter to a frozen Tooling-owned source.
The fixture stops its ordinary child, disables the writer roles, rejects remaining source sessions,
and exposes a SELECT-only view. The Reference protection scope surrounds the existing issue
transaction through commit or rollback. The ordinary configuration remains disabled; consume/close
evidence remains disconnected. Issuing a permit does not prove safe delivery or authorize resend.

The bounded collection is 54 new cases and 301 regression cases. See the
[approved start/stop contract](../../docs/development/phase4-s1-stage-b2-issue-read-limited-start-review-20261008.md)
and [source, commands, failures and cleanup evidence](../../docs/architecture/validation/phase4-s1-stage-b2-issue-read-20261008.md)
for current results and the Owner-approved corrections. Tooling helpers, SQL and acceptance tests
are outside the ordinary JAR; the five new Reference main types intentionally belong to its JAR.

**Current status (2026-10-08):** bounded verification complete; Owner result accepted (Evidence section 19).
New 54 and regression 301 cases qualify, including the first packaged E2E execution and final
cleanup audit. Saved 54 + 189 cases retain their non-impact conditions; aligned 111 cases were
preserved and checked, followed by E2E 1. These are separate artifact epochs, not 355 reruns in
one classpath. The aligned Web MVC cache (`347542...`) is retained under section 30 approval.
The Reference package includes its five intended main types and excludes fixtures. Original
failures remain preserved. See Evidence sections 17–20 and
`tmp/b2-verification-0321079-20261008/E2E-finish/source-after-install.json` for current hash inputs.

`scripts/verify-s1-b2.ps1` is a bounded session runner, not a fresh-clone bootstrap. Its Reference
stage uses the approved isolated source/output at `tmp/b2-isolated-compile-r2-0321079-20261008/`.
The process bridge uses the preflight JDBC classpath under `tmp/b2-preflight-0321079-20261008/`.
Regression commands and approved continuation are recorded under
`tmp/b2-runtime-route-r2-0321079-20261008/`; per-class raw is kept separately under
`tmp/b2-verification-0321079-20261008/`. Existing run directories are never overwritten.

To revisit evidence, inspect the class XML, exit/result metadata, source/artifact hashes and
approval chain without running Maven. To execute again, first confirm the source, JDK/Maven,
offline dependencies, Docker, cached images/browser, explicit case list, remaining budget and
fresh run paths against the approved envelope. A new full run requires a new bounded execution
decision; the failed-attempt retry allowance does not renew itself. Preserve original target/JAR
and isolated versions; do not copy isolated classes back or change compiler settings.

The fixture does not establish production fencing, drain, privileged administrator exclusion,
backup/DR, provider delivery, complete coverage, or formal adoption. Historical target closure
and cross-publication suppression remain outside this initial issue/read boundary. Framework
API/Rules, distribution, CI/remote and Phase 4 overall retain their separate approval boundaries.

B2受入成果のsource固定対象・証拠所在・再見／再実行条件は
[fixed-results handoff](../../docs/development/phase4-s1-stage-b2-fixed-results-next-session-handoff-20261008.md)
と[hash/evidence manifest](../../docs/architecture/validation/phase4-s1-stage-b2-fixed-evidence-manifest-20261008.json)を参照する。
