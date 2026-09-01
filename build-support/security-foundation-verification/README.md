# Security Foundation Verification

Phase 2 Security FoundationのT0〜T6を累積するTooling-owned非配布Harness。
Root Reactor、正式release unit、BOM、snapshot publishおよび`koiki-testing`には含めない。

P2-A1 dependency baselineとA1-4 T0 / T1 security boundaryは次で検証する。

```powershell
pwsh -NoProfile -File build-support/security-foundation-verification/verify-p2-a1-security-foundation.ps1
```

scriptは正式release unitをtemporaryな隔離Maven repositoryへstageし、fixtureのtest dependency、
production / test dependency tree、除外dependency、正式Security artifactおよび非配布境界を検査する。

fixture test sourceは、Servlet / 非Web / KOIKI無効 / Customer chain合成 / 明示置換のT0 context行列と、
401 / 403、CSRF、Security Header、未一致deny、credential非fallbackのT1 HTTP観測を所有する。
test route、test user、credential markerは正式Starterや`koiki-testing`へ昇格させない。
aggregateはHTTP assertionに加え、正式JAR、fixture JAR、Surefire reportを秘密値・private key・PII patternで走査する。
さらにA1-5 contractとして、Public API inventoryの完全一致、正式JAR内classのinternal限定、公開property metadata 0件、
Auto Configuration imports 1件を検査する。
