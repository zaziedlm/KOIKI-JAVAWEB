# D2-03：VT実CI系統・適用ガイド・正本整合

**状態（2026-10-30）：** main統合・実CI成功、DoD 4-11技術PASS。説明済みの限定範囲についてD2-03結果／DoD 4-11をOWNER ACCEPTEDとして記録（§7）。D2全体・正式提供・本番採用へ拡張しない。

**開始指示（2026-10-26）：** OwnerはD2-02結果を確認し「D2-03へ進めましょう」と指示した。
限定結果・制限を次工程の入力とする。D2全体・正式releaseの認定とは区別する。
作業branchは`docs/d2-01-java25-vt-verification-plan-20261026`、基点は`1284ba08a9e0cb44c67487dc02148e378699ceb4`。
文書追随branchは`docs/d2-vt-ci-results-and-dod-alignment-20261027`、基点はmainの`f3c79d8376d93d9f977ff2263c768b5ce32fc3ed`。§4はローカル検証時点の履歴、§5〜7を実CI・判定・受入の現行入口とする。

## 1. 最終変更の範囲

| 対象 | 変更 |
|---|---|
| `.github/workflows/virtual-threads.yml` | Ubuntu24.04の専用jobを一つ追加。既存CI・runtime互換系統は変更しない |
| VT Tooling | bundle生成・checksum／source検証・43項目集約・所有資源cleanupをPython標準libraryで再現可能にする。CIの現在commitを照合し、旧D2-02の固定commit依存をCIへ持ち込まない |
| VT適用ガイド | Java21 build／Java25 opt-in、既定無効、Hikari入力、context／pool／受付制御・検証限界を整理 |
| グランドデザイン§21.5／§23.3 | VT CIをJava25 runtimeへ統一。同一Java21成果物の利用、将来版の個別確認、executorごとのcontext確認を明確化 |
| 統合計画・提供機能ガイド | D2-02のOwner確認とD2-03のmain実CI成功・DoD技術判定と§7の限定受入を反映 |

正式Framework／ReferenceのJava・公開API・依存・migration・既定設定は変更しない。
Root Reactor／publish unitへToolingを追加しない。実CI成功前にDoD4-11をPASSへ更新しない。

## 2. CI設計と実行枠

| 項目 | 設定 |
|---|---|
| Check名 | `Virtual Threads Compatibility (Java 25)` |
| Trigger | pull_request、main push、workflow_dispatch（手動実行） |
| runner／並列数 | Ubuntu24.04一台、専用job一つ。runtime matrixを並列展開しない |
| JDK | Maven・compile・harnessはTemurin21、child runtimeは21／25。実patch版をmanifest／観測へ記録 |
| build | Java21でroot package一回（testは既存CIへ分担）、BOM／Parent・生成済みObservabilityのローカル登録、Tooling package。Java25再compileなし |
| 判定 | A21無効／B25無効／C25有効の通常・観測6 invocation／43 checkpoint。途中再開・診断・SKIPをCIの成功へ使わない |
| 時間上限 | job25分、build10分、比較10分（step上限12分）、cleanup2分 |
| 資源 | 最大Maven1＋harness1＋app1、各heap768 MiB。PG1／1 GiB・CPU1、Ryuk最大1、DB client8以内 |
| 費用の扱い | Ubuntu runner一台分の実時間。PR・main更新・手動実行の回数と各jobの実行時間に応じる。§5の対象jobは2分58秒。課金plan・実請求額は未確認 |
| Artifact | commit／run attemptで識別、保持一日。固定JAR・manifest・集計／JUnit／観測／負荷／cleanup。生app log・生JFRはupload対象外 |
| Required checks | 既存8件を維持。追加・ruleset変更は今回行わない。VT系統の失敗はD2受入・release判断で未解決とする |

Owner指示（2026-10-26）により、スケジュール実行を外し、PR・main更新・手動実行を残す構成へ訂正した。
手動起動はworkflowがdefault branch（main）に存在することが前提。新規branchの初回実CIはpushしたbranchのPRから確認する。
実行はread-onlyの`contents: read`とし、checkout認証を保存せず、publish・deploy・write tokenを使わない。

## 3. Artifact・source・cleanupのガード

`verification.py prepare`はconsumer runtimeを解決したclasspathから、Reference内と同じ版・payloadを再利用する。
衝突・classifier違い・filename重複を拒否し、Reference／observer／追加runtimeを固定bundleへコピーする。
source commit・tree・dirty状態、JDK21／class major65、Tooling source hash、全JAR checksumをmanifestへ記録する。

CIは`GITHUB_SHA`とcheckout／manifestの一致を要求し、dirty sourceを許可しない。ローカルの`--allow-dirty`は
未commit差分の経路確認に限定し、CIでは指定自体を拒否する。D2-02の旧manifestは履歴再現用として残すがCIでは拒否する。
検証前後にJAR・Tooling sourceを再照合する。

containerへrun固有owner labelを付け、DB ID・image ID・Testcontainers sessionを保存する。
子processはPID・起動時刻・固定JAR引数で識別し、同じownerのPGとRyukだけcleanupする。
他のPIDやcontainerを一括停止しない。予期しないapp／PGが残った場合はcleanupしてもcheckを失敗に保つ。
失敗時も既知の証拠・JUnitを保存し、環境変数・接続秘密・生app logをartifactへ載せない。

## 4. ローカル確認と実CIを分ける

ローカル確認はクラウド環境の現在branch・未commit差分で行い、manifestにdirty=trueを保持する。
D2-02で生成済みの同じJava21 Reference JARを使い、正式コード・既存POMを変えていないため通常rootを反復しない。
新しいCI用manifest／commit照合・所有資源記録と同じ専用経路を一回確認する。

現在までに、改変JAR、誤commit、Java25 bytecode、dirty CI、必須runtime欠落・重複、source改変、
他processへのsignal、5 invocation等による不十分なCI判定を拒否する9 guard testが成功した。
actionlint1.7.7（公式release checksum照合済み）とYAML parseを通過。ToolingはJava21でcompile／test-compile成功。
専用経路を2026-10-26 18:50 JSTに完了した。JUnit **6 PASS／0 FAIL／0 ERROR／0 SKIP**、
**43論理checkpoint成立**、各構成と所有資源全体のcleanup成立、予期しない残存資源0。
専用Maven実行は約1分53秒。これはクラウド環境のローカル実績で、GitHub runnerの実時間ではない。

| 識別・環境 | ローカル実績 |
|---|---|
| source commit | `1284ba08a9e0cb44c67487dc02148e378699ceb4`＋未commit差分、manifestのdirty=true |
| JDK | Temurin21.0.12.1+1／25.0.5+8 |
| runtime依存 | consumer解決33 JAR、Reference内再利用19、外付け追加14 |
| 構成別checkpoint | A-native 1、A-observed 12、B-native 1、B-observed 14、C-native 1、C-observed 14 |
| handler観測 | A 49／B 5048はplatform thread、C 5048はvirtual thread。context復元判定成立 |
| 集約・cleanup | `local-ci-route/summary.json`、`local-ci-route/resource-cleanup.json` |

9 guard testとworkflow lintも成功した。actionlintはshellcheck連携を無効にして実行した。
通常rootの既存実績449 PASS／100 SKIPはD2-02記録を参照し、今回再実行していない。

証拠は`/workspace/koiki-cloud/logs/d2-03-20261026/`へ保存する。実CI結果・clean commitの証拠とは区別する。

## 5. main統合と実CIの確認記録（2026-10-27）

D2-01〜03の成果は`0af5d7711e33a60d244769ed3179b20d934f1d44`へ固定され、
[PR #55](https://github.com/zaziedlm/KOIKI-JAVAWEB/pull/55)で2026-10-26 21:26:32 JSTにmainへ統合された（merge commit `831af4a7f98246baf8f37f4893ca70c924f5fa92`）。
続く[PR #56](https://github.com/zaziedlm/KOIKI-JAVAWEB/pull/56)はChromium導入分割・E2E時間上限・Session fixtureの失敗診断を調整し、2026-10-27 00:58:09 JSTにmainへ統合された。
今回の判定対象はそのmain commit `f3c79d8376d93d9f977ff2263c768b5ce32fc3ed`。ローカルmain・origin/main・GitHub mainの一致を読み取り確認した。

| 識別・結果 | main実CIの確認内容 |
|---|---|
| Workflow／event | `Virtual Threads Compatibility`／`push`（main）、run `40354524562`、attempt 1 |
| Run／job | [実行結果・ログ](https://github.com/zaziedlm/KOIKI-JAVAWEB/actions/runs/40354524562)、[Virtual Threads Compatibility (Java 25)](https://github.com/zaziedlm/KOIKI-JAVAWEB/actions/runs/40354524562/job/121683269464)。いずれも`success` |
| Source | `f3c79d8376d93d9f977ff2263c768b5ce32fc3ed`。固定source／JAR照合stepと集約時の再照合が成功 |
| Runner／JDK | Ubuntu24.04、Temurin21.0.12+1でbuild／harness、child runtimeは21.0.12+1／25.0.4+1。§4のローカル25.0.5+8とは別の実績 |
| 比較構成 | A：Java21 VT無効、B：Java25 VT無効、C：Java25 VT有効。それぞれ通常／観測の計6 invocation。同じ固定Java21成果物を使い、runtime側で再compileしない |
| JUnit | `Tests run: 6, Failures: 0, Errors: 0, Skipped: 0`、test elapsed 109.9秒 |
| 集約 | `Six invocations and 43 checkpoints passed; immutable artifacts verified`。A-native 1／A-observed 12／B-native 1／B-observed 14／C-native 1／C-observed 14 |
| 実効thread／context | 集約成功は観測構成のhandlerがA／Bではplatform、Cではvirtualであること、context復元、観測終端の成立を含む（`verification.py summarize`） |
| Cleanup | `No remaining owned app or database resources; owned helper cleanup complete`。予期しないapp／DB残存0、所有helper cleanup成功 |
| 時間 | job開始2026-10-27 00:58:14 JST、完了01:01:12 JST（2分58秒）。build step53秒、比較step1分52秒。上限25分以内。課金額は未確認 |
| Artifact | [ID 12665626336](https://github.com/zaziedlm/KOIKI-JAVAWEB/actions/runs/40354524562/artifacts/12665626336)、`koiki-vt-f3c79d8376d93d9f977ff2263c768b5ce32fc3ed-1`、69,625,198 bytes |
| Artifact digest | `sha256:726f2cdf8836e37412c01f359f0a8257a57637e66ee78bb076c38ab95f80987c`（uploadログ・GitHub APIのdigestが一致） |
| Artifact期限 | GitHub API上の期限は2026-10-28 01:01:07 JST（保持一日）。長期保存用証拠ではない |

今回はGitHub run／job・ログ・artifactメタデータを読み取り照合した。artifact ZIPのダウンロード・内容の再検証や動的検証の再実行は行っていない。
6件・43項目・source／JAR同一性・cleanupの根拠は、上記ログと、それを成功条件とするmain上のworkflow／`verification.py`の照合である。

同じmain commitの[通常CI run 40354524594](https://github.com/zaziedlm/KOIKI-JAVAWEB/actions/runs/40354524594)と
[Java Runtime Compatibility run 40354524583](https://github.com/zaziedlm/KOIKI-JAVAWEB/actions/runs/40354524583)も`success`。
同commitの[後続Java Runtime Compatibility run 40380144880](https://github.com/zaziedlm/KOIKI-JAVAWEB/actions/runs/40380144880)も`success`を確認した。
PR #55直後のmain `831af4a`ではVT系統は成功したが、[通常CI run 40322562414](https://github.com/zaziedlm/KOIKI-JAVAWEB/actions/runs/40322562414)は`failure`だった。
この履歴は保持し、後続`f3c79d8`の成功で過去結果を上書きしない。

今回の文書追随依頼は結果の整理・技術判定を記録する指示として扱う。PR mergeをD2-03結果／DoDの明示的なOwner受入へ読み替えない。

## 6. DoD 4-11の技術判定とOwner受入

[グランドデザイン§27.8](../architecture/grand-design/KOIKI-JavaWeb-FW_グランドデザイン_v0.2.md)のDoD 4-11は「Virtual Threads を有効にした CI 検証系統が通る」。
§21.5／§23.3のJava21 build・Java25 opt-in条件に従う専用CIが、§5の固定main commitでVT有効構成を含む6件／43項目を成功させた。
したがって、**DoD 4-11の技術条件はPASS**と判定する。ローカル結果やproperty指定だけを根拠にした判定ではない。

| 判定対象 | 状態 |
|---|---|
| D2-02限定結果・制限 | Owner確認済み、D2-03へ進行指示 |
| D2-03 workflow／ガイド／正本 | PR #55でmain統合済み。実CI結果を§5へ反映 |
| 新VT checkの実CI | PASS／main `f3c79d8`、run `40354524562`、6件／43項目・cleanup成功 |
| DoD 4-11の技術条件 | PASS／上記実CIを根拠に2026-10-27文書照合で判定 |
| D2-03結果／DoD 4-11のOwner受入 | OWNER ACCEPTED（2026-10-30、§7の限定範囲）。2026-10-27時点では判断待ちだった |
| D2全体・案件の本格採用 | 全体完了のOwner判断は未記録。CI結果と明示した条件・制限を入力に判断。既定無効を維持 |
| D1等の残課題 | 早期拒否／ERROR dispatch相関、逼迫時403、他executor等を継続追跡 |

2026-10-27時点はOwner受入判断待ちだった。2026-10-30の受入記録指示を§7へ追記し、現行状態は同節に従う。
required checks追加・ruleset変更、全executor／非同期／Batch・将来Java版・本番SLOの保証、正式release／support付き配布、Phase4全体の認定へ拡張しない。

利用条件は[VT適用ガイド](virtual-threads-adoption-guide.md)、実装・ローカルの根拠は[D2-02結果](../architecture/validation/phase4-d2-02-java25-virtual-threads-results.md)を参照する。

## 7. 限定結果・DoD 4-11のOwner受入記録（2026-10-30）

Ownerへの説明では、同じJava21生成JARのJava21無効／Java25無効／有効、通常・観測6構成／43項目、
実効thread・選定業務／認証／context／接続解放・source／JAR照合・cleanupの成立と、適用ガイド§3.1の制限を受入対象として提示した。
その後Ownerは、現在branchで「受入判断の記録・古い状態表示の訂正と、次のD1計画整理 と順に進めましょう」と指示した。
本指示を、上記説明済みの限定範囲に対する受入記録の指示として扱い、**D2-03結果／DoD 4-11：OWNER ACCEPTED**と記録する。
過去のPR mergeだけを受入根拠にしたものではない。

- 基礎Evidence：§5のmain `f3c79d8376d93d9f977ff2263c768b5ce32fc3ed`／[run 40354524562](https://github.com/zaziedlm/KOIKI-JAVAWEB/actions/runs/40354524562)。6 PASS、43項目、source／JAR照合・cleanup成立。
- 統合後確認：文書統合main `b2ca253fd12842f0e0e89c827c04c6ca35aa3a29`／[VT run 40641625269](https://github.com/zaziedlm/KOIKI-JAVAWEB/actions/runs/40641625269)も成功。今回の文書編集で新たな実行・artifact再検証は行っていない。
- 受入記録branch：`docs/d1-sync-observability-plan-20261030`、基点main `b2ca253`。記録はローカル差分で、commit／remote反映は別操作。
- 維持する条件：Java21 build、Java25 opt-in、VT既定無効、同期Webの選定経路。早期拒否の相関欠落・DB逼迫時403は既知制限として残す。
- 対象外：全executor・S1非同期・Batch・将来Java版・本番SLO、D2全体の無条件保証、required化、正式release／support付き配布、Phase4全体認定。

次は[D1同期観測の計画整理](phase4-d1-sync-observability-plan-20261030.md)。現状整理に続き、Owner了承に基づきconsumer側の依存・設定を先行する限定接続案を§8〜13へ具体化した。Framework変更が必要なら理由・最小差分を提示する。今回の編集は文書のみ。非同期の設計・運用の深掘りは採用案件の具体化時に行う。
