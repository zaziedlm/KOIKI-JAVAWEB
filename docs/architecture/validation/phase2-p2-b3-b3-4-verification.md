# Phase 2 P2-B3 B3-4 Two-process continuity verification

## 1. Status and scope

- **Verification date:** 2026年9月8日
- **Work package:** `P2-B3 / B3-4`
- **Status:** `IN PROGRESS — THIRD SLICE VERIFIED`
- **Ownership:** Tooling（非配布T6 fixture / process Harness）
- **Baseline:** B3-C1〜C10、B3-2、B3-3 Architecture Owner承認済み

本記録は、package済み同一JARの2 process Session継続、片系停止後の継続、および5種のIdentity mutationによる
別processでの旧Cookie拒否、および実Session DELETE権限障害を外部観測した証拠である。proxy下Cookie属性は未実装であり、
B3-4全体の完了はclaimしない。cleanup / single executionはB3-5へ維持する。

## 2. Implemented Tooling boundary

`build-support/security-foundation-verification/session-two-process-fixture`へ、B3-4専用のSpring Boot executable fixtureを
追加した。fixtureは次の境界を持つ。

- 正式release unitを隔離Maven repositoryへstageした後、別Maven invocationでpackageする。
- Root Reactor、BOM、`koiki-testing`および隔離release repositoryへfixture artifactをinstallしない。
- loopbackだけへbindし、readiness、実行時生成identityのsetup / reset、認証済みprincipal観測、および
  `IdentityAdministration`を呼ぶ管理endpointだけを提供する。
- 標準Form LoginとCSRF tokenを使用し、Session Cookie形式や認証filterを独自実装しない。
- Spring標準`DefaultCookieSerializer`をfixture beanとして公開し、Session Starterが提供する`HttpSecurity` logout customizerを
  fixtureの明示Security chainへ合成する。
- 管理endpointはfixture専用`IDENTITY:ADMIN` Permissionと通常のCSRF保護を要求する。
- fixture-owned Customer migrationはT6に必要なAudit tableだけを持ち、Framework migrationへ昇格させない。
- credential、bootstrap key、user ID、email、role / permission IDはHarnessが実行ごとに生成する。

`verify-p2-b3-session-two-process.ps1`は、GUID付きOS一時directoryへisolated repository、fixture build、process logを
閉じ込める。同じ1個のfixture JARをprocess A / Bで起動し、Cookieはmemory上の`CookieContainer`だけに保持する。

## 3. Two-process and Identity mutation observations

次を実PostgreSQL 17とnetwork越しHTTPで確認した。

1. 正式release unit 14 artifactを隔離repositoryへstageし、fixtureをJava 21、Error Prone / NullAway有効の独立Consumerとして
   packageできる。
2. process Aがclean DBへFramework Identity / Session migrationとfixture-owned Audit migrationを適用した後、object ownershipを
   PostgreSQL管理userへ移し、A / BはDMLだけを付与された非owner app roleで継続する。
3. 同じSHA-256のpackage済みJARを異なるloopback portでprocess A / Bとして起動し、両方のbounded readinessが成功する。
4. Aの標準Form Login pageからCSRF tokenを取得し、実行時生成credentialによるloginが302となり、Session Cookieを1件確立する。
5. `koiki_session`は1 rowで、`principal_name`がemailではなく実行時生成したimmutable Framework user IDと一致する。
6. A由来CookieをA / Bへ提示すると、双方が同じFramework user IDと`ORDER:READ` Permissionを返す。
7. Harnessが開始したprocess Aだけを停止した後もprocess Bは生存し、同じCookieで認証済みrequestが継続する。
8. 実行前後でfixture JAR hashが変化せず、正式KOIKI JARにfixture packageが混入しない。
9. disable、password変更、user Role revoke、Role Permission revoke、external unlinkを、それぞれreset後のfresh stateで
   Public `IdentityAdministration`経由で実行する。
10. 各mutationではprocess Aが発行した対象userの旧Cookieをprocess Bへ提示するとloginへredirectされ、対象userの
    `koiki_session` principal rowが0件になる一方、別Roleのcontrol/admin Sessionはprocess Bで継続する。
11. disableは`DISABLED / version 1`、password変更はuser / credential両version 1と旧password拒否・新password成功、
    user Role revokeはrelation 0件 / user version 1をDBから確認する。
12. Role Permission revokeは同一Roleを持つ2 userの旧CookieとSession rowをともに失効し、relation 0件 / Role version 1を
    確認する。
13. external unlinkはlink 0件 / user version 1となり、代替認証手段としてlocal password credential 1件を維持する。
14. 管理connectionから非owner app roleの`koiki_session` DELETEだけをrevokeし、SELECTと
    `koiki_session_attributes` DELETEを維持した狭い実store障害を作る。
15. disable、password変更、user Role revoke、Role Permission revoke、external unlinkをそれぞれfresh stateで再実行すると
    すべて非成功responseとなり、対象Identityのstatus / version / relation / credential hashとmutation固有Auditがrollbackする。
16. 各失敗ケースで対象旧Sessionと別Roleのcontrol/admin Sessionが継続する。Role Permissionでは同一Roleの2 userとも継続する。
17. logoutの永続削除失敗は成功redirectにならず、current clientのCookie jarからSession Cookieを消去する。一方、永続Session rowと
    コピー済み旧Cookieは残り得ることを確認し、それらまで失効したとはclaimしない。
18. operator向けsafe markerをprocess logで確認し、responseへJDBC URL、SQL、framework class、stack traceを露出しない。
19. `finally`を含む権限復旧後、コピー済みSessionの通常logoutが成功し、対象principal rowを0件にできる。

Harnessの最終結果は次のとおりである。

```text
Phase 2 P2-B3 two-process store-failure slice succeeded:
A/B continuity, five normal and five DELETE-failure Identity mutations,
logout safe failure/recovery, control continuity, and B continuity after A stop.
```

## 4. Regression, sensitive data and cleanup

承認済みB3-3 aggregateを同じworktreeで再実行した。

```text
Formal release staging: Reactor 14 / 14 SUCCESS
Security / Audit / Identity / Session fixture: T0-T5 66 / 66 SUCCESS
Failures: 0, Errors: 0, Skipped: 0
```

Harnessはfixture JAR、Maven log、process A / B stdout / stderrをprivate key、credential assignment、
email形式PII、Authorization header、Session Cookie value patternで検査した。成功表示はprocess停止、container削除、scan、
一時directory削除の完了後だけ出力する。

最終確認は次のとおりである。

```text
Running containers: 0
koiki-session-two-process-* temporary directories: 0
koiki-session-core-* temporary directories: 0
workspace fixture target: absent
```

実装途中のHTTP client失敗を含むnegative executionでも、Harnessが開始したprocess、PostgreSQL containerおよび一時directoryが
回収された。PostgreSQL起動直後のadmin接続はbounded retryと単一transactionにし、無期限waitまたは部分的なrole作成を残さない。

## 5. Remaining B3-4 work

次のsliceでは、proxy下Cookie属性を直接HTTP loopback継続試験と分離して確認する。

B3-5の`SessionCleanup`、maintenance lifecycle、advisory lock、競合 / crash recoveryは先行しない。
