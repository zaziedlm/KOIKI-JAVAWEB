# Phase 4 継続作業・残タスク棚卸し（2026-10-16）

状態：**現行mainとの文書・source照合に基づく棚卸し案 / OWNER REVIEW INPUT**。残タスクの選定・着手順を相談する入口とする。実装開始、追加検証、既存DoDの達成認定を行う文書ではない。

## 1. 継続作業の基点

- 作業branch：`docs/phase4-continuation-task-inventory-20261016`。
- 分岐元：`main` / `af9fa01c56df372559db99e23f495323114f8f93`（PR #51 merge）。直前の端末検証終了時にGitHub mainと一致。
- 同HEADのlocal tag：`checkpoint/v0.1.0-phase4-s1-r3-main-20261016`を`git tag --points-at`で確認。今回remote tagの存在・同一性は照合していない。
- Owner指示：Phase 4継続に先立ち残タスクを丁寧に洗い出す。新branchを作り、端末環境の検証記録を引き継ぐ。
- 引継ぎ文書：[この端末でのmain開発再開検証](../architecture/validation/main-local-development-readiness-20261016.md)。branch切替時に未追跡文書をそのまま保持した。検証対象sourceは分岐元mainであり、後から作ったbranchで再実行した結果とは扱わない。
- Ownership：本書はFramework側の計画文書。将来のcodeはReference／Framework／Tooling／Customer別に対象を確定する。
- 初回棚卸しはbranch作成・文書整理・既存source読取に限定した。その後、承認文書10件はOwnerが`c749e44`へcommitし、個別承認済み接続実証を新branchで実施した。現行結果と次作業は§6。remote操作は行っていない。

以下は2026-10-16時点のRepository内で確認した状態。Customer・運用platformの外部状況は未取得をOPENとし、過去の予定日から完了を推定しない。

## 2. 再実装・再実行対象に戻さない成立済みの範囲

| 対象 | 確認した到達点 | 適用限界・根拠 |
| --- | --- | --- |
| main統合 | PR #47のB2／Framework R0、#48のR1/R2、#49のR3-A/R3-B、#50の統合検証記録、#51のfixture訂正が現main履歴に存在 | 古い「次はcommit／PR」記述は当時の履歴。今の残タスクにそのまま加えない |
| Reference基盤・B1/B2・通常build分離 | 保存・認可・Audit、scope/TTL、Tooling読取、保護付き初回issue/readと通常build分離の限定受入 | [目的・DoD対応表](phase4-s1-purpose-dod-stage-mapping-20261009.md)、[B2通常build対策結果](../architecture/validation/phase4-s1-b2-normal-build-remediation-20261009.md)。肯定consume/close・配送全体の受入ではない |
| R0・R1・R2 | 規約限定改訂、正常非同期、失敗分離・通常保護の限定結果はOwner受入済み | [R1結果](../architecture/validation/phase4-s1-r1-implementation-and-early-verification-20261012.md)、[R2結果§5](../architecture/validation/phase4-s1-r2-selected-seven-case-results-and-acceptance-input-20261013.md#5-r2限定結果のowner受入承認2026-10-13)。実provider配信を含まない |
| R3-A | 隔離全停止型・単一対象の復旧／拒否と通常保護の限定結果をOwner受入 | [R3-A結果§23](../architecture/validation/phase4-s1-r3a-implementation-and-a01-stop-20261013.md#23-r3-a限定結果のowner受入承認2026-10-14)。A04dは既占有lock拒否、A04aは拒否復旧子から独立probeへの送信0。実運用の全instance停止や広い競合保証とは分ける |
| R3-B | 受理観測log・照会契約、E07/E09、E10の選択枝、人手HOLD・直接回帰・通常保護の限定結果をOwner受入 | [R3-B結果§22〜23](../architecture/validation/phase4-s1-r3b-source-preflight-and-blocking-review-20261014.md#23-r3-b限定結果のowner受入2026-10-14)。B03a自動完了FAILとE08残枝を保持。R3全体COMPLETEではない |
| 統合後・fixture訂正 | main上の通常／選択非同期の成立記録。短縮temp pathとfixture登録競合をtest-onlyで訂正し、別端末で専用92件・最終root成立の記録あり | [タグ前検証](../architecture/validation/phase4-s1-main-pretag-verification-stop-20261016.md)、[専用92件結果§6〜9](../architecture/validation/phase4-s1-dedicated92-source-d38d6b6-and-fixture-stop-20261016.md)。当時のHEAD＋差分fingerprintを後のmerge hashへ読み替えない |
| この端末の通常開発環境 | main `af9fa01`でroot clean verify 305 PASS＋専用92 skip、Critical Journey 1 PASS、NullAway／Feature Template成立 | [端末検証記録](../architecture/validation/main-local-development-readiness-20261016.md)。この端末では専用92件・非同期全件・全CIを再実行していない。初回E2Eの実行引数不備と訂正後PASSを別保存 |

## 3. S1の残タスク

優先列は設計整理の目安。各行を別々の実行枠にするとも、すべてを初回接続実証の開始前に解決するとも決めない。現在選定・承認された限定範囲は§6と接続実証開始票を参照する。

| ID / 優先 | 残タスク・現在の不足 | 次に作る判断材料／出口 | Ownership・着手条件 |
| --- | --- | --- | --- |
| S1-01 / 先行 | **B03a / E10aのFAILED処置**。provider受理後応答喪失でFAILED、Reference観測logなし。人手HOLDは成立、publication完了は未達 | 自動復旧Aの必要性を再評価し、人の照合から解決・再開／正式エスカレーションまでの選択肢、状態遷移・誤対象防止・受理証拠・完了条件を比較する。既存FAILの単純再実行や期待値の変更を解決としない | Referenceの復旧契約、Toolingの独立証拠、判断はOwner。[A/B比較§7](phase4-s1-r3b-failed-publication-limited-correction-owner-review-20261014.md#7-owner採用判断2026-10-14)でR4と再判断する方針 |
| S1-02 / 先行 | **R4の操作主体・認可・Audit・permit接続**。基盤と単発復旧は存在するが、正式な復旧操作としての接続は未達 | 既存consume→recover一回→結果Auditの限定実証はOwner受入・`652e504`へ固定済み。正式operator入口・委譲・close・本番運用は引き続き不足として扱う | Reference所有。未登録Application1件＋test/fixture2件の限定結果のみ受入済み。正式入口の開始承認ではない。新受付tableは保留 |
| S1-03 / 先行 | **証拠耐久保管と使用時保護**。B1/B2の限定凍結・初回issue/readを送信時までの保護へ延長したとは認定できない | 初回は[人手HOLD案§7](phase4-s1-r4-human-hold-handoff-guide-20261016.md#7-既存記録で必要十分かの点検2026-10-16)で既存記録を点検。照合材料は既存記録、禁止事項/担当/参照は既存運用記録へ補う結論・運用上の制限をOwner承認済み（同文書§8）。正式復旧のB-C01〜10・肯定proof・使用中保護/耐久保管全体は未解決で別判断 | Reference／Tooling境界をreview。`NotificationFoundationConfiguration`の既定ConsumptionProof/ClosureProofはemptyであり、肯定fallbackを加えない |
| S1-04 / 先行 | **D11/D12と歴史対象・別publication**。現行closeの現在対象要求、別publication横断の論理通知抑止、照合後の変更窓が残る | 歴史対象閉鎖と配送成功を分け、対象削除／attempt変化・別publication・旧worker継続時の結果を表にする。保護できない条件のHOLD・担当・次確認を定める | [段階B契約§3〜4](phase4-s1-stage-b-operational-integration-contract-review-draft-20261008.md)。既存一意制約・短いTTL・lock保有だけで保証済みにしない |
| S1-05 / 次 | **E08の重複・並行残枝**。Reference同論理通知の逐次再要求、stub同key並行要求、通常listenerと復旧processの並走は残存 | 3枝を分け、同key/payload/宛先、別通知の非抑止、受理数、競合位置と独立観測を定義。全停止型でlistener並走を対象外にするなら適用条件・残課題を明示して判断 | Reference／Tooling。D01のstub逐次契約とC04の別通知相関を代替証拠にしない。[R3-B§18・23](../architecture/validation/phase4-s1-r3b-source-preflight-and-blocking-review-20261014.md#18-17後の残ケースと判断境界2026-10-14) |
| S1-06 / 次 | **E11〜14の運用統制**。元listener生存、停止証拠不正／再起動、複数復旧者、lock喪失の各枝はR3の拒否例だけでは不足 | 全停止型を運用上どう維持するか、全instance・起動世代・drain・再配置抑止・遅延要求を確認。送信前／中／受理後のlock喪失と旧worker抑止を分ける | Reference／Tooling＋限定環境管理。実運用platform保証は別入力。稼働中型へ移るなら新しい保護モデルをreview |
| S1-07 / 次 | **E15〜17の監視・運用**。FAILED件数／滞留起点、alert、認可再送、試行上限・timeout、Audit障害、代行・解除の統合証拠が不足 | metricの定義・sink、担当・連絡先・代行、許容復旧時間、送信前／後のAudit失敗と受理不明、上限到達後の照合手順を具体化する | Reference／Tooling、運用条件はOwner。timeoutを送信取消し・未受理確定と扱わない |
| S1-08 / 後段設計を早期接続 | **R5保持・パージ / E18〜19**。publication・provider key・受理log・permit・操作証拠の寿命整合と単一実行パージが未達 | 保持期間、期限超過COMPLETEDのみの削除、FAILED/未処理/未解決HOLDの保全、二重起動／lock喪失／復旧競合・復元を設計する。C3 Batchとの共通基盤採否を別判断 | Reference／Tooling。保持値・削除権限・基盤・対象をreview。C3全体開始を自動前提にしない |
| S1-09 / 後段設計を早期接続 | **R5追跡・観測回復 / E20〜21**。相関値の採取を、HTTP→非同期→別process復旧の全追跡・実trace・sink回復と同一視できない | event/publication/key/permit/attempt/process世代の対応と非露出・非漏えい、log/traceの証拠範囲を整理。観測不能時のHOLDから回復後の新証拠による解決までを定める | Reference／Tooling、D1同期との共通準備を一度だけ計上。実演計画の要求とDoD原文を分けて判定 |
| S1-10 / 各段階出口 | **DoDと残リスクの統合判定**。R0〜R3の限定受入、枝ごとのPASS/FAIL/HOLDとDoDの成立範囲が多数の履歴文書に分散 | 下表をE01〜21・S-01〜11・現行method・source/artifact/証拠へ細分化し、不足枝・人の判断・実provider条件を集約。S1の出口候補をOwnerへ提示する | Framework側文書／Architecture判断。case一覧は網羅証明ではなく改訂可能。限定PASSの加算だけで全体PASSとしない |

### 3.1 S1対象DoDの現行判定材料

| DoD | 成立済み材料 | 未成立・次の判定 |
| --- | --- | --- |
| 4-1 非同期通知と承認非rollback | R1正常経路、R2失敗分離、通常構成保護を限定受入 | stub受理の適用範囲とメール／実provider条件を明記して最終判定。全体DoDの正式PASSとはしていない |
| 4-2 停止後配信 | R3-A選択復旧、R3-B E07/B03b等の限定成立 | E10a完遂、運用上の停止真正性・再開統制・許容時間と残枝の扱い。**未達を保持** |
| 4-3 二重送信防止 | 安定key、stub契約、受理照会と選択HOLD/再送の材料 | B03a完遂とE08逐次／並行／並走、key保持と別publicationの残枝。**未達を保持** |
| 4-4 FAILED監視と再送手順 | permit・Audit基盤、選択失敗／HOLDの証拠 | 正式操作と監視・alert・認可/Audit・終端照合の統合実演 |
| 4-5 単一実行パージ | 既存基盤・設計入力 | R5の保持／削除／単一実行／競合の実証 |
| 4-12 非同期相関・log追跡 | R1の相関、R3の対象・process／受理識別 | E20/E21とsinkを含む証拠集約。MDC markerと実traceを区別 |

根拠：[Grand Design §27.8](../architecture/grand-design/KOIKI-JavaWeb-FW_グランドデザイン_v0.2.md)、[目的・DoD・段階対応](phase4-s1-purpose-dod-stage-mapping-20261009.md)、[E01〜21実演計画](phase4-s1-dod-demonstration-plan-20261005.md)。古い「全case未実施」は作成時点の履歴で、後続の選択結果を否定しない。

## 4. S1以外に残るPhase 4・採用支援

以下は[Phase 4見直し草案§3・4.1](KOIKI-JavaWeb-FW_Phase4実施計画_見直し草案_v0.1.md)と[他package分解](phase4-other-packages-breakdown-draft-20261005.md)を照合した計画上の残項目。S1の限定Gateをこれらの開始許可へ広げない。

| ID / 対象 | 残作業と判断材料 | 待ち条件・Ownership |
| --- | --- | --- |
| P4-C0/C1 / MyBatis accounting / 4-6 | adoption trigger、分離モデル、楽観lock・共通error、規約・fixture・依存/Public API影響をreview | **trigger待ち／DEFERRED維持**。Architecture／Reference。SEPARATED・Rule25〜27/30〜37等を先行追加しない |
| P4-B1 / Session SPA＋MVC / 4-8・4-9 | 最小frontend、Session API、CSRF double-submit・経路分離、認証/権限/競合とbrowser実演 | Reference＋Framework Security。Customer Next.js/BFFで代替達成としない。frontend・依存・build/配置・開始条件を判断 |
| P4-C2 / 外部API耐障害性 / 4-7 | 代表操作、timeout/retry/流量制限、受理不明、資源・transaction境界、Resilience4jの採否 | Reference／Tooling。顧客非依存の模擬操作は設計候補。library・SLA・副作用semanticsを別review |
| P4-C3 / non-Web Batch / 4-10 | job・永続metadata・単一実行・二重起動拒否・停止後restart/rerun、非HTTP主体とAudit | Reference／Tooling。既存Batchは別Repositoryで現HEAD/互換性未再確認。正式依存・schema・運用Ownerを判断 |
| P4-C4 / File・Object Storage | 代表use case、受付/公開/DBの確定順、部分file・重複・path/権限・容量・停止時照合 | Reference／Tooling、実format/storageはCustomer入力。local fileだけでObject Storage成果物を充足しない |
| P4-D1同期 / OpenTelemetry | 同期境界の実trace/exporter/sink、log/metric照合、context非漏えい・sink障害 | Framework／Tooling。S1-R5と共有準備・残差を分ける |
| P4-D2 / Virtual Threads / 4-11 | 既定無効を維持しopt-in条件、Java21 build／25 runtime互換・負荷/context・実CIを比較 | Framework／Tooling。CI変更・費用・実行枠は別判断。local成功だけで4-11 PASSにしない |
| P4-D3 / Container・ECS | 固定JAR/image、build/run/health/diagnosis・停止再起動、secret/network/migration/資源 | Reference／Tooling＋platform。localと実ECSを別段階にし、account・権限・費用・配布/deployを個別判断 |
| P4-B2 / Customer REST・BFF | API/DTO/error/version、認証profile、IdP・audience/scope、logout、受入Evidenceとgap審査 | **実案件入力待ち**、Customer主導。Frameworkは既存契約説明と再現可能なgap審査。未取得の案件事実を埋めない |
| P4-04 / SAML、P4-EDGE | 認証終端・KOIKIへ届くcredentialを確認し、直接SAML/broker/OIDC/edgeの採否・必要Adapter・実環境検証を判断 | Customer／platform入力＋Architecture判断。SAMLは当初成果物として後続割当を明示。ALB採用だけでAdapter必須としない |
| P4-E1 / 正式受渡し・release | 対象inventory、Maven座標/version/checksum、配布repository・support、独立Consumer受取と診断 | release/platform Owner＋実チーム受入。内部snapshot・isolated stageを正式releaseへ読み替えない。Phase5を先行しない |
| P4-OPT / Authorization Server・Oracle | 需要・採否・脅威/運用、DB実環境・license/driver等を個別Gateへ | **明示trigger待ち／必須DoD外**。未採用を完了・不要へ置換しない |
| AD-01 / 実チーム受入・AR-D10 | Repository topology、依存選択、build/run/operation/diagnosis、finding/cleanup、受入判断の非機密Evidenceを取得 | Framework準備・worksheet承認と実チーム受入を区別。[AR6契約§8〜9](../architecture/validation/pre-phase4-p4-ar6-actual-project-handoff-contract.md)。P4-AR6/AR7の文書承認を実チーム受入完了と読まない |
| AD-02 / Linux・WSL | 実チームの先行試行結果・失敗条件を受け取り、環境ガイドと再現手順へ反映 | [Framework側作業順序案](pre-phase4-framework-independent-work-review-20260930.md)。この端末のWindows PASSは代替しない。今回参照範囲で実チーム完了Evidenceは確認できていない |
| AD-03 / 日常開発stage | 固定Framework commitごとの開発者専用Maven stage案の採否、IDE/CLI統一・更新/切替/診断と試用を判断 | [比較・推奨案](daily-development-workflow-options-20261001.md)は文書承認済み。方式採用・Tooling実装・チーム試用は別判断 |
| AD-04 / Customer依存制御 | POM誤選択対策、型宣言と検査の責務、effective POM/graph・逸脱検出・例外/保守を具体化 | [比較・検証方式案§8](dependency-control-design-options-20261002.md#8-整理の経緯作業保留と再開方針2026-10-02)で**検証作業保留をOwner了解**。共有・認識合わせ結果とCustomer POM検討のタイミングを確認し、勝手にV1検証へ再開しない |
| AD-05 / Framework開発者のartifact準備 | 用途別準備一覧、診断・取得/local install・offline確認、更新/不足/権限拒否からの復旧と非機密manifestを整える | [artifact準備検討案§7・9](framework-local-artifact-readiness-options-20261006.md)。一端末・選択Toolingの最小LOCAL PASSと恒常方式の採用・実装を区別。この端末のroot/E2E PASSで全Tooling準備完了としない |

## 5. 継続を始める前の文書・証拠整理

| ID | 整理対象 | 今回の扱い・出口 |
| --- | --- | --- |
| DOC-01 | 現行入口と履歴の区別 | AGENTSと両Skillの先頭に今回の条件付き承認を反映済み。目的対応表などの旧案内は履歴と区別する。旧受入記録を消さず、今回の範囲を拡張しない |
| DOC-02 | case→method→DoDとepoch | 全体対応の集約はS1-10へ残し、初回実証では承認済みQ01〜07と直接回帰の証拠を作る。既知B03a FAIL、別method HOLD PASS、専用skip/実行、端末のepochを混同しない |
| DOC-03 | 通常／専用／非同期の検証入口 | 既知FAILを含む専用class全件を誤選択しない。README・実行flag・method選択・通常JARとasync JAR・予定skipの関係を点検し、CI追加が必要なら別提案にする |
| DOC-04 | Git外証拠とcheckpoint | 別端末のtmp/JAR/XML/manifestの必要保全・取得可否を確認。この端末の証拠所在とhashを保持。local checkpointは§1のとおり存在するため新規作成を残タスクにしない。remoteで必要な保全・tag同一性は別途照合対象。tag・pushは別判断 |
| DOC-05 | 全体計画・Gate残判断 | CP-F0/OR/F-4/F-5/S0/S1/S2の旧草案を現在の限定判断へ対応付け、既決と保留を分ける。P4-F設置／個別限定開始と、Phase4全体開始・正式採用・実チーム受入・正式受渡しを区別し、必要な正本改訂案を提示する |
| DOC-06 | 次回開始票 | 選定対象について変更path/Ownership、必要契約・依存/migration/API影響、case・回帰、環境・権限・資源/時間/raw/再実行上限・停止/cleanupを新たに定める。旧R1〜R3の予算・runner・個別権限を流用しない |

## 6. 現在の継続作業と判断

| 項目 | 現在の状態 |
| --- | --- |
| 運用方針 | 場合の数を運用状態に展開せず、最低限の区分・再投入抑止・人の確認/保留を優先する。人為ミスの完全排除は目標にしない |
| 追加受付table案 | **Owner判断で保留**。採用優位が未確認であり、旧受付案の12 file・15件/24件・90分は実行根拠にしない。[保留票](phase4-s1-r4-intake-read-limited-start-owner-review-20261016.md)に判断理由と旧案を残す |
| 現行R4設計 | [一件の復旧レビュー](phase4-s1-r4-failed-resolution-purpose-and-boundary-review-20261016.md)で確認→許可→実行→結果確認を既存permit/消費/Audit/単発復旧へ対応付けた。追加tableなしの呼出し接続に絞る |
| 接続実証の承認 | [開始票§7](phase4-s1-r4-consume-recovery-connection-limited-start-review-20261016.md#7-owner承認記録と網羅性の扱い2026-10-16)で**条件付き限定開始をOwner承認**。新規3 file・Q01〜Q07/直接回帰4件/root1回、新規60分（cleanup5分予約）。実DBのconsume/Auditとtest用復旧境界を使い、実配信・FAILED完遂とは分ける |
| 網羅性 | 七ケースは初回の有限集合。Ownerは不足の可能性を認識し都度追加/見直しを想定。範囲・件数・path・予算を変える追加実行はその時点で判断する |
| 限定実証の結果 | Owner文書commit`c749e44`から新branch `feature/phase4-s1-r4-consume-recovery-connection`を作成し、fresh preflight後に新規3 fileを作成。[結果](../architecture/validation/phase4-s1-r4-consume-recovery-connection-20261016.md)は新規7件/直接回帰4件PASS、root305実行PASS＋99 skip、cleanup成立 |
| FAILED対応の現行判断 | [R4レビュー§10](phase4-s1-r4-failed-resolution-purpose-and-boundary-review-20261016.md#10-aの設計候補留保人手hold維持のowner承認2026-10-16)で**Aは限定実証の設計候補に留め、現段階の正式運用判断は人の照合・担当付きHOLDを維持する方針をOwner承認**。運用配備済みの認定ではなく、FAILED完遂/E10a/DoD未達を維持 |
| 次の作業 | 接続実証はOwner受入・`652e504`へ固定済み。方式比較/設計・方針承認はOwnerが`2dc6d9a`へ固定済み。次に[人手HOLD引継ぎ案](phase4-s1-r4-human-hold-handoff-guide-20261016.md)で、対象一件の最小記録・再投入禁止・担当/確認先を整理した（Owner確認済み、運用先/実担当等のOPEN維持を了解）。引継ぎ案は`142e06f`へ固定済み。追加§7の結論と運用上の不十分を含む制限をOwnerが承認（§8）。現在は今回の文書差分の点検・成果固定待ち。Aの開始票作成・実装/検証を自動で進めない。成果commit・remoteは別判断 |

AGENTSと両Skillの現行入口には接続実証の限定承認を反映済み。目的対応表などに残るR2時点の案内・古い引継ぎは、後続受入/現行開始票と区別して参照する。C01の詳細識別は[技術参照](phase4-s1-r4-c01-operation-binding-review-20261016.md)に留め、全項目の実装や追加受付機能を開始条件にしない。

現在は承認範囲のcode/test作成・限定検証まで終了し、限定結果はOwner受入済み。新規3 fileと結果/受入反映文書はOwnerが`652e504`へcommit済みで、remote操作は未実施。結果受入対象は接続順/非再実行・通常構成保護のみで、正式認証入口・FAILED整合・歴史close・本番運用・R4/DoD/Phase4全体は残課題である。

本書は残タスクの入口であり、完了率・残工数総額・網羅性を確定しない。古い概算には完了分が含まれるため残予算として転用せず、対象を選んだ後に再見積する。
