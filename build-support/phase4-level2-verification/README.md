# Phase 4 Level 2 verification fixture

Tooling-owned, non-distributed PL2 fixture. It is outside the Root Reactor and is not a Framework,
Reference Application, Customer Application, or release artifact. The Maven profiles `jdbc` (default)
and `jpa` compare Spring Modulith publication stores against the same PostgreSQL schema and JPA
business transaction. Production migration and API ownership are not decided by this fixture.

Validation covers publication state and restart recovery, provider-side idempotency after a
listener failure or a process kill after accepted send, FAILED metrics and resubmission,
completed-publication purge, scheduled stale publication marking, and selection of publication
registry trigger annotations.
Correlation across async processing and the Rule 28 /
proposed Rule 29 boundary remain open. Results belong in
`docs/architecture/validation/phase4-pl2-level2-verification.md`.

Run each profile independently using the repository Maven Wrapper:

```powershell
.\mvnw.cmd -f build-support/phase4-level2-verification/pom.xml -Pjdbc verify
.\mvnw.cmd -f build-support/phase4-level2-verification/pom.xml -Pjpa verify
```

Tests use a disposable `postgres:17-alpine` Testcontainers instance. Docker access is required.
