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
Correlation across async processing and the Rule 28 /
proposed Rule 29 boundary remain open. Results belong in
`docs/architecture/validation/phase4-pl2-level2-verification.md`.

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

Tests use a disposable `postgres:17-alpine` Testcontainers instance. Docker access is required.
