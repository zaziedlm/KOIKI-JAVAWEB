# D2-03：VT実CI系統・適用ガイド・正本整合

**状態：** 実装・ローカル確認済み。実CI／DoD4-11は未判定。

**開始指示（2026-10-26）：** OwnerはD2-02結果を確認し「D2-03へ進めましょう」と指示した。
限定結果・制限を次工程の入力とする。D2全体・正式releaseの認定とは区別する。
作業branchは`docs/d2-01-java25-vt-verification-plan-20261026`、基点は`1284ba08a9e0cb44c67487dc02148e378699ceb4`。

## 1. 最終変更の範囲

| 対象 | 変更 |
|---|---|
| `.github/workflows/virtual-threads.yml` | Ubuntu24.04の専用jobを一つ追加。既存CI・runtime互換系統は変更しない |
| VT Tooling | bundle生成・checksum／source検証・43項目集約・所有資源cleanupをPython標準libraryで再現可能にする。CIの現在commitを照合し、旧D2-02の固定commit依存をCIへ持ち込まない |
| VT適用ガイド | Java21 build／Java25 opt-in、既定無効、Hikari入力、context／pool／受付制御・検証限界を整理 |
| グランドデザイン§21.5／§23.3 | VT CIをJava25 runtimeへ統一。同一Java21成果物の利用、将来版の個別確認、executorごとのcontext確認を明確化 |
| 統合計画・提供機能ガイド | D2-02のOwner確認とD2-03の実装／実CI待ちを反映 |

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
| 費用の扱い | Ubuntu runner一台分の実時間。PR・main更新・手動実行の回数と各jobの実行時間に応じる。実時間・課金planは未確認で、初回runの実績を記録する |
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

## 5. 実CIへ進むための成果固定・公開

現在はD2-01〜03の差分が未commit。ローカル確認済みの差分を承認後に固定し、対象branchへpushし、
main向けdraft PRで新VT checkと既存required checksを実行する。実CIのrun URL、event commit、6 invocation／43項目、
JDK patch版、artifact ID／digest、cleanup、job実時間・結論を本記録へ反映する。

`AGENTS.md` §「Phase3では…」項目16は「Ownerの個別承認なしにremote push / PR / merge、ruleset変更、workflow dispatchまたはsnapshot publishを行わない」と定める。
今回のD2-03開始指示に基づく実装・ローカル確認を先に完了し、成果固定commit・push・draft PRを具体的な差分として確認する。
merge・ruleset変更・release・publishはこの段階へ含めない。

## 6. DoD判定の記録欄

| 判定対象 | 状態 |
|---|---|
| D2-02限定結果・制限 | Owner確認済み、D2-03へ進行指示 |
| D2-03 workflow／ガイド／正本 | 実装・ローカル6構成／43項目確認済み |
| 新VT checkの実CI | 未実施、run URLなし |
| DoD4-11 | OPEN／実CI結果待ち |
| D2全体・案件の本格採用 | CI結果と明示した条件・制限を入力に判断。既定無効を維持 |
| D1等の残課題 | 早期拒否／ERROR dispatch相関、逼迫時403、他executor等を継続追跡 |

利用条件は[VT適用ガイド](virtual-threads-adoption-guide.md)、実装・ローカルの根拠は[D2-02結果](../architecture/validation/phase4-d2-02-java25-virtual-threads-results.md)を参照する。
