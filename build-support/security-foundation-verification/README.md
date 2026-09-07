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

P2-B1のT4 Audit transaction aggregateは次で検証する。

```powershell
pwsh -NoProfile -File build-support/security-foundation-verification/verify-p2-b1-audit-transaction.ps1
```

T4ではfixture-owned schemaとPostgreSQL Testcontainersを使い、Business auditの同一transaction、Security auditの
独立transaction、保存失敗時のrollback / fail-closed / continue + alert、transaction外呼出拒否、correlationおよび
actor / payload非露出をDB rowで観測する。scriptはT0〜T4の31 tests、承認済みPublic API 6型、正式Audit JARの
internal境界、production / test dependency、非配布fixture、secret / PII非露出および一時領域cleanupを検査する。

P2-B2 B2-2のIdentity core / migration aggregateは次で検証する。

```powershell
pwsh -NoProfile -File build-support/security-foundation-verification/verify-p2-b2-identity-core.ps1
```

B2-2ではPostgreSQL Testcontainers上でIdentity production migrationを適用し、8 table、再実行no-op、canonical email、
Role / Permission、FK、external identity link、login attempt CHECK、versionおよびraw secret非保存を観測する。
scriptはT0〜T4の39 tests、承認済みIdentity Public API 10型、error code 5件、正式JAR内migration / internal境界、
PostgreSQL用Flyway module、reset / Spring Session非混入および非配布fixture境界を検査する。

P2-B2 B2-3では同じaggregateへ`IdentityAuthenticationFixtureTest` 6 testsと
`IdentityAuthenticationAutoConfigurationContextTest` 3 testsを追加する。永続Userによる認証、
legacy hash upgrade、credential消去、automatic unlock、unknown / bad / disabled / lockedのgeneric failure、並行ACCOUNT lock、
HMAC化したSOURCE block、SOURCE遮断DB読取障害のgeneric failure、およびSecurity Audit失敗時のfail-closed / 防御状態維持を
PostgreSQL上で確認する。
context testではlocal認証の既定OFF、`EXTERNAL`でのHMAC非要求とACCOUNT保護bean維持、`APPLICATION`のHMAC設定不備による
startup failureを確認する。aggregateはT0〜T4の48 testsとなり、Identityの公開設定12件とAuto Configuration imports 2件も
完全一致で検査する。
