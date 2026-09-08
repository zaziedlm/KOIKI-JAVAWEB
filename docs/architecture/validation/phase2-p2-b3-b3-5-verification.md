# Phase 2 P2-B3 B3-5 Session cleanup / single execution verification

## 1. Status and scope

- **Verification date:** 2026年9月8日
- **Work package:** `P2-B3 / B3-5`
- **Status:** `COMPLETE — READY FOR ARCHITECTURE OWNER REVIEW`
- **Ownership:** Framework（Session cleanup contract / PostgreSQL internal adapter）、Tooling（非配布T6 fixture / process Harness）
- **Baseline:** B3-C1〜C10、B3-2 / B3-3 Architecture Owner承認済み、B3-4実装完了

本記録は、期限切れSession cleanupのPublic API、Spring標準cleanup呼出し、PostgreSQL session advisory lockによる
single execution、およびnon-web maintenance processの競合 / crash recoveryを実装・外部観測した証拠である。
B3-5 exit criteriaを満たした実装証拠としてOwner reviewへ提示する。T0〜T6 aggregate closeoutと3回連続安定性はB3-6へ維持する。

## 2. Implemented Framework boundary

`koiki-starter-session-jdbc`へ、承認済みinventoryどおり次の3型だけを追加した。

- `SessionCleanup#cleanUpExpiredSessions()`
- `SessionCleanupResult`の`COMPLETED` / `CONTENDED`
- safe固定messageだけを持ち、vendor / SQL / lock key / causeを公開しない`SessionCleanupException`

内部`KoikiPostgresqlSessionCleanup`は、固定した2整数のinternal keyを使って専用JDBC connection上で
`pg_try_advisory_lock(int, int)`を非待機実行する。取得時だけSpring公開API
`JdbcIndexedSessionRepository.cleanUpExpiredSessions()`を同期実行し、成功・失敗とも同じconnectionで明示unlockしてcloseする。
競合時はcleanupを呼ばず`CONTENDED`を返す。汎用lock API、削除件数、Customer入力key、新しいKOIKI property、独自DELETE SQLは追加していない。

Spring Boot 4.1.1の`JdbcSessionAutoConfiguration`がServlet Web限定であることを実測したため、Starterは
`SessionRepository`が存在しないcontextだけでSpring標準`JdbcIndexedSessionRepository`を構成する。
table、cleanup cron、flush / save modeは承認済み値を固定し、timeoutは標準`spring.session.timeout`を使用する。
Servlet WebではBoot構成済みrepositoryへback offし、Web / non-web双方へ`SessionCleanup` use caseを提供するが自動実行しない。

## 3. T5 contract and PostgreSQL observations

累積T0〜T5 fixtureへ次を追加した。

1. Public型、enum値、final exception、固定message / cause非保持がinventoryと一致する。
2. lock取得時だけSpring repository cleanupを呼び、明示unlockと専用connection closeを行う。
3. lock競合時は即時`CONTENDED`となり、repository cleanupとunlockを呼ばない。
4. connectionまたはcleanup failureをsafeな`SessionCleanupException`へ変換し、cleanup failure後もunlock / closeする。
5. non-web ApplicationContextへJDBC repositoryと`SessionCleanup`を提供する。
6. 実PostgreSQL上で期限切れSessionとそのattributeだけを削除し、active Sessionとattributeを維持する。

検証結果は次のとおりである。

```text
Formal release staging: Reactor 14 / 14 SUCCESS
Security / Audit / Identity / Session fixture: T0-T5 72 / 72 SUCCESS
Failures: 0, Errors: 0, Skipped: 0
```

## 4. T6 packaged non-web process observations

`session-cleanup-process-fixture`はRoot Reactor、BOM、`koiki-testing`およびrelease repositoryへinstallしないTooling-owned
executable JARである。`SpringApplication`を`WebApplicationType.NONE`へ固定し、1回だけPublic `SessionCleanup`を呼ぶ。
Application-owned exit mappingは`COMPLETED=0`、`CONTENDED=10`、`SessionCleanupException=1`である。

`verify-p2-b3-session-cleanup-process.ps1`は、正式release unitを隔離repositoryへstageしてからfixtureを別invocationでpackageし、
実PostgreSQL 17と同一package済みJARの独立processを使って次を確認した。

1. clean DBでnon-web bootstrap processがmigrationとcleanupを完了し、exit `0`となる。
2. Harness-owned遅延trigger下でwinnerがadvisory lockを保持中、contenderは待機せずexit `10`となり、winnerはexit `0`となる。
3. 期限切れrow / attributeだけが削除され、active row / attributeが残り、DELETE side effectは1回だけである。
4. advisory lock保持中のwinnerを正確なOS PIDで停止すると、PostgreSQL接続終了後にlockが消失する。
5. killされたcleanup transactionはrollbackし、期限切れrowとattributeおよびside-effect countを変更しない。
6. Harness-only遅延triggerを除去したretryはexit `0`となり、期限切れだけを削除する。
7. process logにembedded server起動markerがなく、正式KOIKI JARにfixture packageが混入しない。
8. Maven / process logにprivate key、credential assignment、email形式PII、Authorization header、Session Cookie値を残さない。
9. すべてのprocess、container、GUID付きOS一時directoryを`finally`で回収した後だけ成功を表示する。

Harnessの最終結果は次のとおりである。

```text
Phase 2 P2-B3 B3-5 Session cleanup / single execution verification succeeded
(expired-only cleanup, exit 0/10, contention, crash recovery).
```

遅延trigger、観測table、固定待機は一時PostgreSQL内のHarness専用test mechanismであり、正式artifact、production migration、
Framework Public APIには含めていない。production sourceへscheduler、sleep、failure switch、test endpoint、固定port / credentialを追加していない。

## 5. Review boundary and next work

B3-5はArchitecture Owner review待ちであり、本記録だけでOwner承認済みとはclaimしない。

B3-6ではT0〜T6 aggregateを3回連続実行し、artifact / dependency / Public API inventory、sensitive output、cleanup状態を再確認して
P2-B3 closeout evidenceを作成する。Milestone B CI接続 / required化はGate B reviewまで留保し、Reference `identity`はP2-B4より前へ進めない。
