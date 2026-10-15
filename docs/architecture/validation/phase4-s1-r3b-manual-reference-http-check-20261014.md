# Phase 4 S1 R3-B：Reference app の手動HTTP確認（2026-10-14）

状態：**限定手動確認 PASS／Owner受入判断前**。remote push・PR・merge前に、Architecture OwnerがReference appをHTTP操作し、非同期通知の動作をログとDBでも確認したいと指示した。実ブラウザではなく、AgentによるHTTP操作とログ照合を選択した。本記録は[R3-B限定結果のOwner受入](phase4-s1-r3b-source-preflight-and-blocking-review-20261014.md#23-r3-b限定結果のowner受入2026-10-14)に追加する独立の手動確認であり、既存の選択caseを再実行・上書きしない。

## 対象と方法

- branch `feature/phase4-s1-reference-unknown-recovery-r3b`、source commit `f51492d27216a893bd9dbe77ccec08f217cdea3d`、開始前のGit作業ツリーはclean。
- 固定async JAR `tmp/r3b-artifacts-20261014/reference-async.jar`、SHA-256 `7A09B3F5B242477D31FDFFC4D8894CBD7A4C059C0D0BB4BAAB3A72F95F937B34`を使用。再packageせず、通常JARも起動しない。
- 既存のcompiled `AsyncReferenceFixture`を基盤に、`tmp/r3b-manual-reference-20261014/ManualReferenceWalkthrough.java`を一時作成・compile。Git管理source・通常設定は変更しない。fixture所有の使い捨てPostgreSQL `postgres:17-alpine` 1個、loopback provider stub、loopback認証fixture、Reference子processを起動する。Java heap 768 MiB、PostgreSQL memory 1 GiB／CPU 1、DB `max_connections=16`。開始前のDocker稼働containerは0、空きmemory 14,913,124 KiB、C空きdisk 534,627,573,760 bytesだった。通常sandboxからMaven依存JAR／Docker named pipeへのアクセスが拒否されたため、AGENTS.mdの承認済み権限付き実行手順を使用した。
- 新規申請をBearerで作成・提出し、既存Session login／CSRF付きMVC endpointで承認を1件だけ実行。承認前に申請 `SUBMITTED`／version 2、対象publication 0、stub到着・受理0を確認。承認応答後に、独立読取接続で申請・Audit、publication、Reference受理観測log、stub受理台帳を照合した。実providerへの通信、故障注入・復旧process起動は行わない。

## 実測結果

| 項目 | 結果 |
|---|---|
| 申請ID | `a59afdd7-04e5-4231-82bc-e3ba55b29b27` |
| MVC承認HTTP | `302`、応答時刻 `2026-10-14T08:51:03.458546700Z` |
| 経費申請・Audit | `APPROVED`／version 3、`APPROVE_EXPENSE`成功Audit 1件 |
| publication | `cca4bccb-0b31-4de7-8d8c-321a3157d4e1`、`COMPLETED`／試行1、完了時刻 `2026-10-14T08:51:03.527328Z` |
| provider stub | 到着1件、受理1件、受理ID `685a7e9d-af2f-440e-901a-5c882cf77194` |
| Reference受理観測log | 同じ通知keyの受理ID・event ID・recipient ID・payload SHA-256がstub受理台帳と一致 |
| 結果・cleanup | 一回の手動HTTP経路はPASS。今回のReference子PID `25112`は終了し、Docker稼働container0。保護Java PID `23472`／`24668`／`25088`は生存、Git作業ツリーはsource変更なし |

同一端末の時刻では、承認HTTP応答の約69 ms後にpublication完了が記録された。この1件は、承認応答と通知完了が別の時点であることを示す観測であり、一般的な遅延・順序保証や実provider配送保証には拡張しない。実ブラウザ操作の成立も主張しない。

## 保全証拠と境界

`tmp/r3b-manual-reference-20261014/observation.txt`のSHA-256は`336F0ED78A63C92A8C59C53BF579EF81C40B91D898A50534729E6A3DF6A21A86`、redacted Reference子log `reference-child-redacted.log`は`7252D79609A7BA022BF8B158A55901DCE10D5DF2F4D29AAC95DAF7F746495790`。起動・操作用の一時Java source／class、DB、認証fixture、raw JAR／logはGitで移送されない。必要ならhash付きで別途保全する。本Markdownだけが新しいGit文書候補であり、local commit・remote push・PR・mergeは別判断とする。

このPASSは固定async JARとloopback providerの正常1件に限る。R3-BのB03a自動完了FAILと人手HOLD、E08残枝、DoD 4-2／4-3未達、実provider・正式operator運用未実装は維持する。R3-B／R3全体の`COMPLETE`やPhase 4全体開始へ読み替えない。
