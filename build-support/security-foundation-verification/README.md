# Security Foundation Verification

Phase 2 Security FoundationのT0〜T6を累積するTooling-owned非配布Harness。
Root Reactor、正式release unit、BOM、snapshot publishおよび`koiki-testing`には含めない。

P2-A3までのdependency baselineとT0 / T1 / T2 / T3 security boundaryは次で検証する。

```powershell
pwsh -NoProfile -File build-support/security-foundation-verification/verify-p2-a3-oidc-bearer.ps1
```

scriptは正式release unitをtemporaryな隔離Maven repositoryへstageし、fixtureのtest dependency、
production / test dependency tree、除外dependency、正式Security artifactおよび非配布境界を検査する。

fixture test sourceは、Servlet / 非Web / KOIKI無効 / Customer chain合成 / 明示置換のT0 context行列と、
401 / 403、CSRF、Security Header、未一致deny、credential非fallbackのT1 HTTP観測を所有する。
T2ではSpring標準Form Login、email canonicalization、generic failure、HTTP Session fixation protection、
Roleから展開したPermissionによるURL / Method Security、およびController迂回direct invocation拒否を観測する。
T3ではephemeral issuer / keyだけをtest sourceで生成し、Spring標準OIDC Login、Bearer JWT、
profile分離、credential fallback拒否およびCORS negative pathを観測する。
test route、test user、credential markerは正式Starterや`koiki-testing`へ昇格させない。
aggregateはHTTP assertionに加え、正式JAR、fixture JAR、Surefire reportを秘密値・private key・PII patternで走査する。
さらにA1-5 contractとして、Public API inventoryの完全一致、正式JAR内classのinternal限定、公開property metadata 0件、
Auto Configuration imports 1件を検査する。

Gate Aのlocal aggregateは次で検証する。

```powershell
pwsh -NoProfile -File build-support/security-foundation-verification/verify-gate-a-security-foundation.ps1
```

Gate AではP2-A3 cumulative acceptanceに加え、正式release unitを別の隔離Maven repositoryへstageし、
Root Reactor外のCustomer-like Consumerをbuild / test / packageする。同一Consumer JARをJava 21 / 25で実行し、
Public API正負fixture、root verify、runtime dependency境界、secret non-exposureおよび一時領域cleanupを確認する。
published baselineとのPublic API compatibilityとremote required checksは、Owner承認後のPR境界で実行する。
