# Phase 2 P2-B3 B3-4 Two-process continuity verification

## 1. Status and scope

- **Verification date:** 2026年9月8日
- **Work package:** `P2-B3 / B3-4`
- **Status:** `IN PROGRESS — FIRST SLICE VERIFIED`
- **Ownership:** Tooling（非配布T6 fixture / process Harness）
- **Baseline:** B3-C1〜C10、B3-2、B3-3 Architecture Owner承認済み

本記録は、B3-4の最初の実装sliceとして、package済み同一JARの2 process Session継続と片系停止後の継続を
外部観測した証拠である。Identity mutation matrix、実Session DELETE権限障害およびproxy下Cookie属性は未実装であり、
B3-4全体の完了はclaimしない。cleanup / single executionはB3-5へ維持する。

## 2. Implemented Tooling boundary

`build-support/security-foundation-verification/session-two-process-fixture`へ、B3-4専用のSpring Boot executable fixtureを
追加した。fixtureは次の境界を持つ。

- 正式release unitを隔離Maven repositoryへstageした後、別Maven invocationでpackageする。
- Root Reactor、BOM、`koiki-testing`および隔離release repositoryへfixture artifactをinstallしない。
- loopbackだけへbindし、readiness、実行時生成identityのsetup、認証済みprincipal観測だけを提供する。
- 標準Form LoginとCSRF tokenを使用し、Session Cookie形式や認証filterを独自実装しない。
- fixture-owned Customer migrationはT6に必要なAudit tableだけを持ち、Framework migrationへ昇格させない。
- credential、bootstrap key、user ID、email、role / permission IDはHarnessが実行ごとに生成する。

`verify-p2-b3-session-two-process.ps1`は、GUID付きOS一時directoryへisolated repository、fixture build、process logを
閉じ込める。同じ1個のfixture JARをprocess A / Bで起動し、Cookieはmemory上の`CookieContainer`だけに保持する。

## 3. First-slice observations

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

Harnessの最終結果は次のとおりである。

```text
Phase 2 P2-B3 first two-process slice succeeded:
A login, B continuity, and B continuity after A stop.
```

## 4. Regression, sensitive data and cleanup

承認済みB3-3 aggregateを同じworktreeで再実行した。

```text
Formal release staging: Reactor 14 / 14 SUCCESS
Security / Audit / Identity / Session fixture: T0-T5 66 / 66 SUCCESS
Failures: 0, Errors: 0, Skipped: 0
```

first-slice Harnessはfixture JAR、Maven log、process A / B stdout / stderrをprivate key、credential assignment、
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

次のsliceでは、control userを含むfixture stateを操作ごとに再作成し、既存Public `IdentityAdministration`経由で次を外部確認する。

- account disable
- password変更
- user Role変更
- Role Permission変更
- external unlink

各操作で対象旧Cookie拒否、対象Session row削除、control Session継続およびIdentity状態更新を確認する。その後、app roleの
Session table `DELETE`権限だけを一時失効させ、Identity mutation / Business Audit rollbackとlogout local消去 / 非成功結果を
確認する。proxy下Cookie属性は直接HTTP loopback継続試験と分離して追加する。

B3-5の`SessionCleanup`、maintenance lifecycle、advisory lock、競合 / crash recoveryは先行しない。
