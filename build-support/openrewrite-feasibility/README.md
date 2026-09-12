# P2-C2 C2-6 OpenRewrite feasibility

This directory is Tooling-owned, Root Reactor-external and non-distributed. It demonstrates one synthetic
KOIKI-owned API type move and does not provide the Phase 5 `koiki-migration-recipes` artifact.

```powershell
pwsh -NoProfile -File build-support/openrewrite-feasibility/verify-p2-c2-openrewrite.ps1
```

The verifier builds the prototype recipe, copies the synthetic old Consumer to a GUID-scoped temporary directory,
applies the recipe, compares the result with the tracked expected source, runs the recipe a second time to prove
idempotence, and compiles/tests the transformed Consumer against the current `koiki-starter-identity` artifact.
It also checks the manual-actions report, release-unit exclusion and temporary-output cleanup.

The prototype does not cover Spring Boot migrations, real Customer source, historical KOIKI versions, automatic
dependency removal, all KOIKI API/property changes, remote publication or required CI.
