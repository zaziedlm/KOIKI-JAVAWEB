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

P2-B2 B2-4では同じaggregateへ`IdentityAdministrationFixtureTest` 6 testsと
`IdentityAdministrationAutoConfigurationContextTest` 2 testsを追加する。user lifecycle、password policyと
compromised password拒否、Role / Permission変更、external identityの完全一致、optimistic version、Business Audit失敗および
password、User Role、Role Permission、external unlinkのSession invalidator失敗時rollbackをPostgreSQL上で確認する。
さらにaccount disableはSecurity Audit失敗後もSession失効とdisableを完了し、management unlockはAudit失敗時にrollbackする。
context testでは必須4依存の欠落時にQueryを維持して管理beanを構成せず、全依存充足時だけ構成することを確認する。
aggregateはT0〜T4の56 testsとなる。
fixture invalidatorが確認するのは同期SPIの呼出しとtransaction結果までであり、Spring Session row削除と旧Cookie拒否はP2-B3に残す。

P2-B2 B2-5では同じaggregateをcloseout evidenceとして再実行し、正式Identity JAR、Maven実行log、全Surefire reportへ
private key、credential、SOURCE HMAC key、email形式PII、Authorization headerが残らないことを追加検査する。
Maven出力は画面表示前に検査し、検証失敗時も生成済みJAR、log、reportを再検査してから失敗を通知する。
DB row / Auditの非露出はfixtureの意味的assertionで確認し、合成credentialを含むfixture JARは正式release unitと
隔離Maven repositoryへ入らないことを検査する。root `clean verify`とNullAway正負fixtureは別commandで実行し、
Identityの公開baseline / japicmpはGate B以降のOwner reviewへ残す。

P2-B3 B3-3のSession invalidation / logout aggregateは次で検証する。

```powershell
pwsh -NoProfile -File build-support/security-foundation-verification/verify-p2-b3-session-core.ps1
```

B3-2では新規optional `koiki-starter-session-jdbc`を正式release unitへstageし、Spring Session JDBCのschema initializerを
`never`、table名を`koiki_session`、Web cleanupを無効、flush / save modeを`ON_SAVE` / `ON_SET_ATTRIBUTE`とする境界を検査する。
PostgreSQL上ではFramework Flywayが管理する2 tableだけを使用し、migration再実行no-op、save前にはrowを書かないこと、
読取だけでは属性rowを書き換えず属性変更時だけ更新することを観測する。さらに実local認証で生成した
`FrameworkPrincipal`を保存・復元し、DBのprincipal indexがimmutable user ID、復元後credentialが空であることを確認する。
B3-3では既存`UserSessionInvalidator`をSpring Session JDBCのimmutable principal indexへ接続し、対象userだけの全Session失効、
MockMvcによるServlet filter chain上のlogin / logout、Session row削除、Cookie失効、同一processでの旧Cookie拒否およびSession invalidate障害時の
local SecurityContext / credential / Cookie消去とsafe failureを確認する。
aggregateは既存Security / Audit / Identity回帰を含むT0〜T5 66 tests、Session Public Java型0件、KOIKI固有property 0件、
Spring標準property 9件、2 table、依存境界、正式artifact / log / reportのsecret / PII非露出および一時領域cleanupを検査する。
package済み2 process継続 / 別processでの旧Cookie拒否 / 実store障害はB3-4、maintenance cleanup / single executionはB3-5に残す。

B3-4のT6 two-process / Identity mutation sliceは次で検証する。

```powershell
pwsh -NoProfile -File build-support/security-foundation-verification/verify-p2-b3-session-two-process.ps1
```

正式release unitを隔離Maven repositoryへstageした後、非配布fixtureを別invocationでpackageする。同じ1個の
executable JARをloopback上の2 processとして起動し、process Aでの標準Form Login、process BでのSession継続、
process A停止後のprocess B継続、およびimmutable principal indexをHTTP / DB / process状態から確認する。さらに操作ごとに
fresh fixture stateを作り、disable、password、user Role、Role Permission、external unlinkをPublic `IdentityAdministration`で
実行する。A由来の対象旧CookieがBで拒否され、対象Session rowが削除される一方、専用control/admin Sessionが継続すること、
および各Identity状態遷移を確認する。Session DELETE権限障害、proxy下Cookie属性は後続B3-4 sliceで追加する。
