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

P2-B3のT0〜T5 Session core / invalidation / cleanup contract aggregateは次で検証する。

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
aggregateは既存Security / Audit / Identity回帰を含むT0〜T5 72 tests、Session Public Java型3件、KOIKI固有property 0件、
Spring標準property 9件、2 table、依存境界、正式artifact / log / reportのsecret / PII非露出および一時領域cleanupを検査する。
package済み2 process継続 / 別processでの旧Cookie拒否 / 実store障害はB3-4 Harnessが所有する。

B3-4のT6 two-process / Identity mutation / store failure sliceは次で検証する。

```powershell
pwsh -NoProfile -File build-support/security-foundation-verification/verify-p2-b3-session-two-process.ps1
```

正式release unitを隔離Maven repositoryへstageした後、非配布fixtureを別invocationでpackageする。同じ1個の
executable JARをloopback上の2 processとして起動し、process Aでの標準Form Login、process BでのSession継続、
process A停止後のprocess B継続、およびimmutable principal indexをHTTP / DB / process状態から確認する。さらに操作ごとに
fresh fixture stateを作り、disable、password、user Role、Role Permission、external unlinkをPublic `IdentityAdministration`で
実行する。A由来の対象旧CookieがBで拒否され、対象Session rowが削除される一方、専用control/admin Sessionが継続すること、
および各Identity状態遷移を確認する。加えて、非owner app roleの`koiki_session` DELETEだけを一時失効し、
5 mutationすべてのsafe failure、Identity / Business Audit
rollback、既存Session継続、およびlogoutのlocal Cookie消去 / 非成功結果を確認する。権限復旧後の正常logoutも確認する。
さらにfixture processだけでSpringのforwarded header処理を有効にし、直接HTTPでは`Secure`なし、
`X-Forwarded-Proto=https`では`Secure`あり、両方で`HttpOnly` / `SameSite=Lax`となるSession Cookie属性を比較する。

B3-5のT6 non-web cleanup / single execution sliceは次で検証する。

```powershell
pwsh -NoProfile -File build-support/security-foundation-verification/verify-p2-b3-session-cleanup-process.ps1
```

同一package済みJARを`WebApplicationType.NONE`で1回実行し、期限切れSessionだけの削除、winner exit `0`、
contender exit `10`、副作用1回、winnerのOS kill後のPostgreSQL advisory lock解放とretry exit `0`を外部観測する。
遅延triggerと観測tableはHarnessが一時PostgreSQLだけへ作り、production source / migrationへ入れない。

B3-6のlocal closeoutは次で検証する。

```powershell
pwsh -NoProfile -File build-support/security-foundation-verification/verify-p2-b3-session-closeout.ps1
```

cleanな同一HEADでcore、B3-4 Web T6、B3-5 non-web T6を3回連続実行し、各round後のprocess / container /
一時directory cleanupを確認する。その後root `clean verify`とNullAway positive / expected negative / restoreを実行する。
各Harnessが所有する正式artifact / dependency / Public API inventoryとsensitive-output scanを再利用し、
workflow追加またはrequired check変更は行わない。

P2-B4 B4-4のpackage済みReference journeyは次で検証する。

```powershell
pwsh -NoProfile -File build-support/security-foundation-verification/verify-p2-b4-reference-journey.ps1
```

正式Framework release unitの14 projectを隔離Maven repositoryへstageした後、Reference applicationを別invocationで
packageする。Harness所有の一時PostgreSQLとruntime生成したadmin / target / control identityを使い、標準Form Loginから
`IDENTITY_ADMIN`のassign / revoke、Business Audit、optimistic version、対象userの全Session失効、admin / control Sessionの
継続、および再login後のauthorization反映をHTTP / DB / Cookieから確認する。permission不足のcontrol userによる
valid CSRF付きdirect POSTは403となり、Identity / Audit / Sessionが変化しないことを確認する。

Business Audit INSERT権限とSpring Session DELETE権限を個別に一時失効し、いずれも503の固定応答、Identity / Auditの
rollback、既存対象Sessionの継続を検査する。一時audit tableとidentity / credentialはHarnessだけが生成し、Referenceの
production migration、fixture route、固定credential、failure switchにはしない。ReferenceのBOM分離、正式release repository
非収載、Public API / source / template / route inventory、deferred dependency非追加、sensitive-output非露出、所有process / container /
一時directory cleanupも同じ実行で検査する。

B4-5のP2-B1〜B4 local closeoutは次で検証する。

```powershell
pwsh -NoProfile -File build-support/security-foundation-verification/verify-p2-b4-closeout.ps1
```

cleanな同一HEADで、B1 Audit専用contract、B2 Identity専用contract、B1 / B2回帰を含むB3 T0〜T5 core、
B3 two-process、B3 non-web cleanup、B4 package済みReference journeyを3回連続実行する。各round後にHarness所有container、一時directory、
非配布fixture targetのcleanupとHEAD / worktree不変を確認する。その後Root Reactor 15 projectと
NullAway positive / expected negative / restoreを実行する。各子Harnessのartifact / dependency /
Public API / migration inventoryとsensitive-output検査を再利用する。

このlocal closeoutはworkflow、remote environmentまたはrequired checkを変更しない。Architecture Owner承認後だけ、
Gate BでMilestone B aggregateのCI候補化、Public API / japicmp方針およびrequired化を別途reviewする。

Gate Bでは同じcloseout scriptを`Local Identity Session Audit Integration` jobのlocal / CI共通正本として再利用する。
通常実行は一次失敗を保持しつつ最終repository / residual-resource検査を必ず試行する。CIの`if: always()`最終stepでは、
次のinspection-only commandを使用し、aggregate stepの失敗後にもclean worktree、expected HEAD、所有container、
外部起動したJava child process、一時directoryおよび非配布fixture targetを独立して検査する。process検査はJava processに限定し、
WindowsではCIM、Linuxでは`/proc`から一時path markerとの一致を確認してPIDだけを扱い、command line本体を出力しない。repository内JARを起動するB4 Reference processには、
Harness生成の非機密JVM markerを付与し、同じ検査へ接続する。このmarkerをproduct codeまたは公開propertyとして利用しない。

```powershell
pwsh -NoProfile -File build-support/security-foundation-verification/verify-p2-b4-closeout.ps1 `
  -InspectOnly -ExpectedHead <commit-sha>
```

CI job timeoutは60分、aggregate stepは52分、最終inspectionは5分とする。timeout / cancelにより最終inspection自体を
完了できなかった場合はcleanup成功とみなさない。同一final HEADでremote 3回連続成功と全回cleanup成功を確認し、
2026年9月10日のArchitecture Owner承認後、main rulesetのrequired checkへ追加した。Customer業務アプリのCIは、
利用Starter、業務リスク、構成およびデプロイ形態に応じて別途軽量化・段階化する。

P2-C1 C1-2のAudit production migration / static inventoryは次で検証する。

```powershell
pwsh -NoProfile -File build-support/security-foundation-verification/verify-p2-c1-migration-static.ps1
```

scriptはAudit Starterの`V2026090300`を含むpackage済みAudit / Identity / Session JDBC Starter JARを直接検査し、
Framework migration 3件の一意性と依存順、owner artifactごとの配置、合計11 table、Audit Entityと一致する列契約、
fixture table / failure constraint / 未使用indexの非混入を確認する。focused Audit reactorだけを実行し、実PostgreSQL上の
clean install 4 profileとPhase 1b supported upgradeは次のC1-3 Harnessが所有する。

P2-C1 C1-3のPostgreSQL clean install / supported upgrade aggregateは次で検証する。

```powershell
pwsh -NoProfile -File build-support/security-foundation-verification/verify-p2-c1-postgresql.ps1
```

scriptは正式Framework release unitを隔離Maven repositoryへstageし、package済みStarter resourceと
PostgreSQL 17を使ってAudit only、Identity、Session JDBC、package済みReferenceの4 profileを検証する。
各profileではFramework table、Flyway version / checksum / history分離、列 / constraint / index、再起動no-opを確認し、
Referenceがmigrationを所有せずSession依存closureを利用することもpackage済みJARから検査する。

supported upgradeでは承認済みPhase 1b baseline `40d16f9dbf26a7ba88ac13b2e3728075e0eff2a7`のCustomer migrationを
変更せずに適用し、既存history、checksum、tableおよびseed rowを保持したまま現行Framework migration 3件を追加する。
失敗系ではFramework失敗時のCustomer非実行、Customer失敗後のFramework history保持、Customer checksum不一致時の
startup failure、Session initializer強制有効化の拒否と`SPRING_SESSION`非生成を確認する。
Harness、fixture SQL / Javaおよび一時credentialは非配布Toolingに限定し、container、process、一時repository、
fixture / Reference targetを終了時にcleanupする。

P2-C1 C1-4 closeoutは、cleanな同一HEADで次を順に実行する。

```powershell
pwsh -NoProfile -File build-support/security-foundation-verification/verify-p2-c1-migration-static.ps1
pwsh -NoProfile -File build-support/security-foundation-verification/verify-p2-c1-postgresql.ps1
pwsh -NoProfile -File build-support/security-foundation-verification/verify-p2-b4-closeout.ps1 `
  -ExpectedHead <commit-sha>
```

C1 static inventoryとPostgreSQL aggregateに加え、既存Gate B closeoutを再利用してB1〜B4を3ラウンド連続実行し、
Root Reactor、Null Safety positive / expected negative / restore、Public API / artifact / dependency inventory、
sensitive-outputおよびcleanupを確認する。最終的なrepository / residual-resource検査だけを独立再実行する場合は、
同じ`<commit-sha>`を指定して`verify-p2-b4-closeout.ps1 -InspectOnly`を使用する。

P2-C2 C2-2のformal package manifest / isolated stagingは次で検証する。

```powershell
pwsh -NoProfile -File build-support/security-foundation-verification/verify-p2-c2-package-static.ps1
```

`p2-c2-formal-release-unit.txt`を正本として、Referenceを除くformal Framework release unit 14 projectsを
空の隔離Maven repositoryへstageする。Root ReactorはReferenceを含む15 projectsのまま、BOM管理対象は11 JARのまま維持し、
staged coordinates、POM / JAR packaging、Reference / Tooling / Customer migration / source template非混入を検査する。
隔離repositoryは成功・失敗のどちらでも終了時にcleanupする。C2-2ではConsumer、Public API baseline、snapshot publish、
OpenRewrite prototypeまたはCIを追加・実行しない。

P2-C2 C2-3のRoot Reactor外Consumerは次で検証する。

```powershell
pwsh -NoProfile -File build-support/security-foundation-verification/verify-p2-c2-consumer.ps1
```

既存Gate A Consumerのbaselineを変更せず、その配下の独立PostgreSQL Consumerを空の隔離Maven repositoryから
build / test / packageする。ConsumerはPublic APIだけでIdentity作成 / 照会、Business AuditおよびSession cleanupを利用し、
同一package済みJARをJava 21 / 25とPostgreSQL 17で実行し、別Web processでpublic / authenticated / unmatched routeと
Security Headerも確認する。Framework migration 3件 / 11 table、Customer history分離、
Customer側のbaseline 1行＋migration 1行、Java 25再起動no-op、Session initializer強制有効化のstartup failure、
正式release unit非混入、sensitive-outputおよび
container / temporary repository / fixture target cleanupを外部観測する。

P2-C2 C2-4の全JAR Public API baseline candidateは次で検証する。

```powershell
pwsh -NoProfile -File build-support/security-foundation-verification/verify-p2-c2-public-api.ps1
```

C2-2 manifestの11 JARをpackage済みartifactから走査し、24 Public Java型の型種別、継承、constructor、field、method、
generic型、例外、enum値、annotation metadata / default値およびJSpecify nullnessを正規化する。Public型0件のJARも
artifact sectionとして固定し、internal packageを外部APIから除外する。synthetic fixtureではinternal追加の許容、
nullness-only変更の検出、および既存japicmp fixtureによるpublic戻り値破壊 / 未承認追加の期待failureを確認する。
既存Phase 1a published baseline、required jobおよびremote stateは変更しない。
