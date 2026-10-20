# Phase 4 継続作業・残タスク棚卸し（2026-10-16）

状態：**継続branchの承認・成果固定を反映した残タスク再整理 / OWNER REVIEW INPUT**。現行方針と次の対象は§8、成果と残課題は§6〜7、詳細は§3〜5。実装開始、追加検証、既存DoDの達成認定を行う文書ではない。

## 0. 現行の進め方（Owner補足反映、2026-10-17）

非同期系は現時点の実装・限定検証と運用方針で今回の作業を区切る。復旧の難しい部分は人による照合・担当付きHOLDで補完し、当初計画のP4-B1：Session SPA＋MVC Referenceへ移行する。追加GET・復旧機構・提供資料の整備を移行条件にしない。

限定受入済みの成果、DoD未達、運用上のOPENを保持する。具体的な不足が判明した場合に必要範囲だけ再開を判断する。詳細は§8。

**2026-10-20追記：** Ownerは、P4-B1の調査・設計を進めつつ、直接影響するレビュー指摘だけを実装・受入より先に解決する方針に合意した。[レビュー対応との統合作業計画](phase4-review-integrated-task-plan-20261020.md)で、B1の調査・設計とF1／F2／F3／F5の基盤是正、他package・採用支援への割当を作業単位に整理した。直近は同計画I-01／B1-01。詳細WBSは計画案であり、レビュー全件・S1全体の解消をB1開始条件にしない。

同日のI-01／B1-01調査結果とB1-02〜03の具体仕様案は[B1設計記録](phase4-b1-session-spa-mvc-design-20261020.md)へ集約した。後続の[Cloud環境点検](../architecture/validation/phase4-r00-cloud-environment-preflight-20261020.md)で保存済み構成を有効化し、ソース無変更のclean test-compile・規約7件PASS、fresh import違反0を確認。R-00の実行準備は成立し、次はproduction検査対象とfixtureの役割を整理する。是正実装・B1機能実演・DoD受入は未実施。

次の作業範囲はB1設計記録§7.4へ具体化した。R-00の既存test1 fileでproduction対象を明示し、直接回帰7件を確認する案。その限定結果を整理してR-01の例外境界へ進む。今回の範囲整理ではcode変更・検証の再実行を行っていない。

その後、OwnerのR-00着手指示に基づきtest1 fileを変更し、[production限定importの結果](../architecture/validation/phase4-r00-production-import-results-20261020.md)でclean compile・既存7件PASS、両importのtest由来0を確認した。R-00の限定変更・検証は完了。次はR-01の例外境界であり、B1機能・全体DoDの受入は未実施のまま維持する。

## 1. 継続作業の基点

- 初回棚卸しbranch：`docs/phase4-continuation-task-inventory-20261016`。
- 現作業branch：`feature/phase4-s1-r4-consume-recovery-connection`。再整理前のclean HEADは`1ca43821988d8e2bc0f958ec46fe52b2212c431e`。最新FAILED監視方針までOwnerがcommit済み。今回remote同期の確認はしていない。
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
| consume→単発復旧の接続実証 | 新規7件/直接回帰4件PASS、root305実行PASS＋99 skip、cleanup成立。Q04表現訂正を含みOwner受入、`652e504`へ固定済み | [実証記録§5](../architecture/validation/phase4-s1-r4-consume-recovery-connection-20261016.md#5-限定結果のowner受入2026-10-16)。未登録Applicationと限定fixtureの結果。正式入口・FAILED完遂・実配信の受入ではない |

## 3. S1の残タスク

優先列は初回棚卸し時の目安。現在の方針整理済み/入力待ち/技術的未達は§6〜7で分ける。全行を直ちに実装する計画ではなく、実行済み開始票を追加実行の根拠にも使わない。

| ID / 優先 | 残タスク・現在の不足 | 次に作る判断材料／出口 | Ownership・着手条件 |
| --- | --- | --- | --- |
| S1-01 / 方針整理済み・技術未達 | **B03a / E10aのFAILED処置**。人手HOLDは成立、publication完了は未達 | [R4レビュー§10](phase4-s1-r4-failed-resolution-purpose-and-boundary-review-20261016.md#10-aの設計候補留保人手hold維持のowner承認2026-10-16)でAは限定設計候補に留め、人の照合・担当付きHOLD維持をOwner承認、`2dc6d9a`へ固定。比較のやり直しを次タスクにしない。FAILED完遂は未達のまま | Reference／Tooling。Aの実装/実証は開始しない。必要性が生じた時に費用/不足/新規限定範囲を別判断 |
| S1-02 / 先行 | **R4の操作主体・認可・Audit・permit接続**。基盤と単発復旧は存在するが、正式な復旧操作としての接続は未達 | 既存consume→recover一回→結果Auditの限定実証はOwner受入・`652e504`へ固定済み。正式operator入口・委譲・close・本番運用は引き続き不足として扱う | Reference所有。未登録Application1件＋test/fixture2件の限定結果のみ受入済み。正式入口の開始承認ではない。新受付tableは保留 |
| S1-03 / 先行 | **証拠耐久保管と使用時保護**。B1/B2の限定凍結・初回issue/readを送信時までの保護へ延長したとは認定できない | 初回は[人手HOLD案§7](phase4-s1-r4-human-hold-handoff-guide-20261016.md#7-既存記録で必要十分かの点検2026-10-16)で既存記録を点検。照合材料は既存記録、禁止事項/担当/参照は既存運用記録へ補う結論・運用上の制限をOwner承認済み（同文書§8）。正式復旧のB-C01〜10・肯定proof・使用中保護/耐久保管全体は未解決で別判断 | Reference／Tooling境界をreview。`NotificationFoundationConfiguration`の既定ConsumptionProof/ClosureProofはemptyであり、肯定fallbackを加えない |
| S1-04 / 先行 | **D11/D12と歴史対象・別publication**。現行closeの現在対象要求、別publication横断の論理通知抑止、照合後の変更窓が残る | [人手HOLD案§9](phase4-s1-r4-human-hold-handoff-guide-20261016.md#9-未閉鎖permitの保証範囲と別publicationの扱い2026-10-16)で同publication/期限経過・対象変更/消失・別publication・停止不明を整理し、結論・制限をOwner承認済み（同文書§10）。未閉鎖維持と人手関連付けの方針であり、歴史closeや横断抑止は新設しない | [段階B契約§3〜4](phase4-s1-stage-b-operational-integration-contract-review-draft-20261008.md)。既存一意制約・短いTTL・lock保有だけで保証済みにしない |
| S1-05 / 次 | **E08の重複・並行残枝**。Reference同論理通知の逐次再要求、stub同key並行要求、通常listenerと復旧processの並走は残存 | 初回は[逐次再要求のsource整理](phase4-s1-e08-sequential-replay-boundary-review-20261016.md)の方針・制限を運用へ提供する方向でOwner承認（同文書§6）。同keyの受理一意性・通知POST到着・Referenceのevent一致/完了は別保証。追加§7で同key並行/stub単一thread、通常listenerと復旧lockの非共有を整理し、方針・制限をOwner承認済み（同文書§8）。全停止型と不明時HOLDを維持し、並行/並走と逐次全経路の実証は残す | Reference／Tooling。D01のstub逐次契約とC04の別通知相関を代替証拠にしない。[R3-B§18・23](../architecture/validation/phase4-s1-r3b-source-preflight-and-blocking-review-20261014.md#18-17後の残ケースと判断境界2026-10-14) |
| S1-06 / 次 | **E11〜14の運用統制**。元listener生存、停止証拠不正／再起動、複数復旧者、lock喪失の各枝はR3の拒否例だけでは不足 | [全停止型の最小確認案](phase4-s1-full-stop-operational-boundary-review-20261016.md)で対象範囲・元実行終了・非並走維持・再確認/引継ぎの四点を整理し、方針・制限をOwner承認済み（同文書§6）。条件不明/変化時HOLD、通常再開は別判断。実platform・D12/fencing全体は残す | Reference／Tooling＋限定環境管理。実運用platform保証は別入力。稼働中型へ移るなら新しい保護モデルをreview |
| S1-07 / 次 | **E15〜17の監視・運用**。FAILED件数／滞留起点、alert、認可再送、試行上限・timeout、Audit障害、代行・解除の統合証拠が不足 | 初回は[FAILED監視・引継ぎ案](phase4-s1-failed-monitoring-handoff-boundary-review-20261016.md)でFAILED件数・publication年齢・初回検知からの未解決時間を区別し、観測不能/担当未引受けの扱いを整理し、運用時適用の方針と制限をOwner承認済み（同文書§6）。追加§7でAudit失敗・上限・timeoutを既存接続/人手HOLDへ対応付け、方針・制限をOwner承認済み（同文書§8）。閾値/担当/sink、E15〜17の実動作は残す | Reference／Tooling、運用条件はOwner。timeoutを送信取消し・未受理確定と扱わない |
| S1-08 / 後段設計を早期接続 | **R5保持・パージ / E18〜19**。publication・provider key・受理log・permit・操作証拠の寿命整合と単一実行パージが未達 | 初回は[保持/パージ境界案](phase4-s1-r5-retention-purge-boundary-review-20261016.md)で保護対象・COMPLETED削除候補の条件・記録間の寿命を整理し、方針・制限をOwner承認済み（同文書§6）。日数/権限/競合/復元はOPEN、実装/パージ実行なし。C3 Batchは別判断 | Reference／Tooling。保持値・削除権限・基盤・対象をreview。C3全体開始を自動前提にしない |
| S1-09 / 後段設計を早期接続 | **R5追跡・観測回復 / E20〜21**。相関値の採取を、HTTP→非同期→別process復旧の全追跡・実trace・sink回復と同一視できない | 初回は[追跡/観測回復案](phase4-s1-r5-correlation-observation-recovery-review-20261016.md)で既存IDの照合経路、HOLD hashの意味、MDCと実traceの区別、回復後の新証拠照合を整理し、運用設計反映を前提に方針・制限をOwner承認済み（同文書§6）。実trace/非漏えい/sink実証は残す | Reference／Tooling、D1同期との共通準備を一度だけ計上。実演計画の要求とDoD原文を分けて判定 |
| S1-10 / 各段階出口 | **非同期基本機能提供に向けた現品質と残課題**。R0〜R3の限定受入、枝ごとのPASS/FAIL/HOLDとDoDの成立範囲が多数の履歴文書に分散 | [提供準備材料](phase4-s1-dod-integrated-status-owner-review-20261016.md)へOwner補足を反映。非同期基本機能を提供し案件開発に並行して充足するため、現品質・保証範囲・人手補完・残課題を集約し、追加整備は今回区切る。DoD/未達も継続管理 | Framework側文書／Architecture判断。case一覧は網羅証明ではなく改訂可能。限定PASSの加算だけで全体PASSとしない |

### 3.1 S1対象DoDの現行判定材料

| DoD | 成立済み材料 | 未成立・次の判定 |
| --- | --- | --- |
| 4-1 非同期通知と承認非rollback | R1正常経路、R2失敗分離、通常構成保護を限定受入 | stub受理の適用範囲とメール／実provider条件を明記して最終判定。全体DoDの正式PASSとはしていない |
| 4-2 停止後配信 | R3-A選択復旧、R3-B E07/B03b等の限定成立 | E10a完遂、運用上の停止真正性・再開統制・許容時間と残枝の扱い。**未達を保持** |
| 4-3 二重送信防止 | 安定key、stub契約、受理照会と選択HOLD/再送の材料 | B03a完遂とE08逐次／並行／並走、key保持と別publicationの残枝。**未達を保持** |
| 4-4 FAILED監視と再送手順 | permit・Audit基盤、選択失敗/HOLD、consume接続限定結果。監視値の区別と人手引継ぎ方針はOwner承認 | 正式操作と監視・alert・認可/Audit・終端照合の統合実演。方針承認を実運用/実証成立へ読み替えない |
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
| DOC-01 | 現行入口と履歴の区別 | AGENTSと両Skillに接続実証の受入入口を反映済み。後続方針は個別承認文書と本書§6へ集約。目的対応表などの旧案内は履歴と区別し、旧受入記録を消さず範囲を拡張しない |
| DOC-02 | case→method→DoDとepoch | Q01〜07と直接回帰の証拠は作成・限定受入済み。全体対応の集約はS1-10へ残す。既知B03a FAIL、別method HOLD PASS、専用skip/実行、端末のepochを混同しない |
| DOC-03 | 通常／専用／非同期の検証入口 | 既知FAILを含む専用class全件を誤選択しない。README・実行flag・method選択・通常JARとasync JAR・予定skipの関係を点検し、CI追加が必要なら別提案にする |
| DOC-04 | Git外証拠とcheckpoint | 別端末のtmp/JAR/XML/manifestの必要保全・取得可否を確認。この端末の証拠所在とhashを保持。local checkpointは§1のとおり存在するため新規作成を残タスクにしない。remoteで必要な保全・tag同一性は別途照合対象。tag・pushは別判断 |
| DOC-05 | 全体計画・Gate残判断 | CP-F0/OR/F-4/F-5/S0/S1/S2の旧草案を現在の限定判断へ対応付け、既決と保留を分ける。P4-F設置／個別限定開始と、Phase4全体開始・正式採用・実チーム受入・正式受渡しを区別し、必要な正本改訂案を提示する |
| DOC-06 | 次回開始票 | 選定対象について変更path/Ownership、必要契約・依存/migration/API影響、case・回帰、環境・権限・資源/時間/raw/再実行上限・停止/cleanupを新たに定める。旧R1〜R3の予算・runner・個別権限を流用しない |

## 6. 方針整理済み・成果固定済みの範囲

以下は各限定文書のOwner判断とcommitを集約した現行入口。文書方針の承認と実証/運用成立は分ける。

| 項目 | 現在の判断・成果固定 | 残る境界 |
| --- | --- | --- |
| consume→recover一回→結果Audit | 限定結果Owner受入、`652e504`。新規7/回帰4/rootの結果は§2 | 正式入口/実配信/FAILED完遂とは別 |
| FAILED受理済み処置 | Aは限定設計候補、人の照合・担当付きHOLD維持を承認、`2dc6d9a` | Aの実装/実証は進めない。E10a未達 |
| 人手HOLD引継ぎ | 最小記録・引継ぎ内容をOwner確認、OPEN維持了解、`142e06f` | 実担当/記録先/引継ぎ実施は未確定 |
| 既存記録の再利用 | 結論と運用上の不十分を含む制限を承認、`dbbe399` | 新table/新Audit/新保管基盤なし。保管/権限/保持はOPEN |
| 未閉鎖permit・別publication | 未閉鎖維持と人手関連付けの方針/制限を承認、`493af09` | 歴史closeを新設しない。横断抑止・D12は未達 |
| E08逐次/並行/並走 | 逐次方針/運用提供方向`959e48a`、並行/並走の全停止/HOLD方針`586db9b`を承認 | 新しい再要求/並行復旧機構なし。E08実証は未達 |
| 全停止型の最低確認 | 確認不足は開始せず、条件変化時は追加要求なしで照合する方針を承認、`66783e7` | 実platformの停止維持/通常再開条件はOPEN |
| FAILED監視・引継ぎ | 観測値の区別と運用時適用を承認、`1ca4382` | 監視配備/運用開始/alert実証とは別 |

追加受付tableはOwner判断で保留を維持する。新状態管理や全組合せ検証を前提にせず、不足が具体化した時に必要な範囲を判断する。運用へ提供する方針は承認済みだが、他者への送付・正式引渡し・配備を実施した記録ではない。

## 7. 次に残る仕事の区分

### 7.1 今回までの方針整理と現在位置

| 対象 | 次の最小整理 | 今回の状態 |
| --- | --- | --- |
| S1-07の残り：E16/E17 | 送信前/後のAudit失敗、試行上限、timeout、通知不達/担当不在を既存consume・結果Audit・HOLDへ対応付ける。既存PASSの範囲と未実証の統合運用を分ける | [監視/引継ぎ文書§7](phase4-s1-failed-monitoring-handoff-boundary-review-20261016.md#7-audit失敗試行上限timeoutの最小処置2026-10-16)に対応付けと結論を追記し、Owner承認済み（同文書§8）。実証未達は維持し、追加再送機構は作らない |
| S1-08：R5保持/パージ | 未解決HOLD・未閉鎖permit・provider key/受理記録/publicationの保持関係と、何を削除対象にできないかを整理 | [保持/パージ境界案](phase4-s1-r5-retention-purge-boundary-review-20261016.md)で人手HOLD方針との対応と削除できない条件を整理し、Owner承認済み（同文書§6）。日数/削除権限/実装/パージ実行は別判断 |
| S1-09：追跡/観測回復 | 既存ID・Audit・HOLD証拠で追える範囲と、sink/実trace・観測回復の不足を整理 | [追跡/観測回復案](phase4-s1-r5-correlation-observation-recovery-review-20261016.md)の方針・制限を運用設計へ反映する前提でOwner承認済み（同文書§6）。E20/E21・実sink実証は未完 |
| S1-10：DoD統合判定 | 限定結果/運用方針/未達を実案件の利用範囲へ対応付け、基本機能提供と案件に並行した必要整備を判断 | [提供準備材料](phase4-s1-dod-integrated-status-owner-review-20261016.md)を基本機能提供・案件に並行した充足の観点へ修正。method/hash/epochの索引と未達は保持するが、一括充足を開発向け提供の条件にしない |

S1-07残り、保持/パージ、追跡/観測回復の方針整理はそれぞれOwner承認済みで、`0294ee7`、`505b947`、`bbcf3de`へ固定済み。S1-10は既存結果・現品質・残課題を集約した。非同期系の追加整備は今回区切り、次はP4-B1の差分確認へ進む。

### 7.2 運用/実案件入力を待つもの

- 一件の記録先、主担当/代行/責任者、連絡先、確認間隔/期限、監視閾値とsink。
- 対象instance/起動世代、停止/drain、再起動抑止、送出済み要求の照合、通常再開の具体手段。
- 実providerの同key/内容一致・並行受理・保持期限・照会/受理証拠、実配信の受入条件。
- 証拠保管先、権限、保持/backup/復元、削除権限。手順案だけで運用体制成立とはしない。

具体的な運用値を推測で補わず、提供/受入側の入力が得られた時に不足を限定して更新する。現時点で各担当へ連絡・実操作を依頼したわけではない。

### 7.3 未実装・未実証のまま残すもの

FAILED完遂/E10a、正式operator入口/認証/肯定proof、歴史close、別publication横断抑止、D12/fencing、E08逐次全経路/並行/並走、E11〜17の統合実動作、R5削除競合/復元、実provider/trace等は、各受入済み限定範囲を除き未達を維持する。

これらは今回の方針整理で免除・不要とした項目ではない。必要性が具体化した時に実装/実証の限定開始を別判断する。S1全体完了と開発向け提供を分け、利用経路で必要な品質を確認し、具体的な必要が判明した場合に整備の対象を選び直す。S1全体の未達は正確に残すが、その一括解消を提供の一律条件にはしない。旧開始票の残予算・runner・PASSを流用しない。

S1以外のPhase4/採用支援は§4のまま残る。MyBatis等のtrigger待ち、Customer/IdP/BFF/実チーム受入、依存制御の検証保留を勝手に再開しない。remote操作・成果commitは別判断。

本書は残タスクの入口であり、完了率・残工数総額・網羅性を確定しない。今回の再整理は文書更新だけで、追加実装・検証・運用操作は行っていない。

### 7.4 残量の見方（2026-10-17）

[読取経路案](phase4-s1-publication-read-path-minimal-design-review-20261017.md)の§8〜9はOwner確認済み。人手の読取・照合・HOLD引継ぎの参照として保持する。GET実装は進めず、実担当/権限/記録先等のOPENは§7.2に残す。

S1以外は§4の表で**Phase4が12区分、採用支援が5区分、計17区分**。各行は大きさの異なる作業群で、trigger待ち・保留・実案件入力待ち・任意項目を含む。独立タスクの総数や残工数ではなく、すべてを直近に実装する意味でもない。方針整理済みの項目を再び未着手として数えない。次の作業は§8に従う。

## 8. 非同期系の区切りと当初Phase4残タスクへの移行（2026-10-17）

### 8.1 Owner判断と区切り

Ownerは、非同期系の実装には区切りをつけ、リカバリーに苦慮する部分は人系の対応で補完することを優先し、当初Phase4の他の残タスクへ移行すると指示した。これを現在の作業方針とする。

- 現時点の実装・限定検証の受入と、既存の運用方針・読取経路案§9を成果として残す。復旧が不明な一件は人が照合し、担当付きHOLDへ戻す。確認不足から再投入を推定しない。
- 非同期系の追加実装・追加実証・設計の深掘りは今回ここで区切る。追加受付table、受理済みFAILED処置A、GET/専用読取権限、scope更新、追加復旧機構を進めない。組込み手順の追加整理も移行の前提にしない。
- §7.2の運用入力、§7.3の技術残課題、DoD未達は記録として保持する。S1全体完了・本番運用準備完了・正式受渡しを認定するものではない。
- 実案件で具体的な不足・不具合・復旧上の要求が判明した時だけ、必要な対象と確認範囲を選び直す。他packageの作業中にS1全体の再開・全件検証へ広げない。

### 8.2 他の残タスクへの移行順

当初計画[§4.1](KOIKI-JavaWeb-FW_Phase4実施計画_見直し草案_v0.1.md#41-gate後の実行work-package候補)はB1/C2/C4/D2の独立した設計を候補としており、他packageの前にS1全体を完遂する順序にはしていない。

| 扱い | 対象 | 次に確認すること |
| --- | --- | --- |
| 次に進む対象 | **P4-B1：Session SPA＋MVC Reference** | 当初DoD 4-8/4-9と既存MVC/REST/Session/CSRF実装との差分を確認し、最小frontend・対象操作・配置・検証範囲を絞る。Customer Next.js/BFFや認証基盤全体の作り直しへ広げない |
| B1の後に選ぶ独立候補 | P4-C2外部API耐障害性、P4-C4 File/Object Storage、P4-D2 Virtual Threads、P4-D1同期観測、P4-D3 Container | §4の残作業を使い、対象と必要入力を確認して一つずつ選ぶ。非同期復旧の続編として扱わない |
| 固有の確認が必要 | P4-C3 Batch | 既存Batch Repositoryの状態・互換性とReference jobの責務を確認する。非同期パージ実装を今回の再開条件にしない |
| 待ち・保留を維持 | MyBatis、Customer BFF/IdP/SAML等、正式release、optional項目、採用支援の保留項目 | trigger・実案件入力・個別判断を待ち、作業移行だけを理由に開始しない |

次は**P4-B1の当初要求・既存実装・不足の確認**へ進む。ここで決めたのは次の設計調査対象であり、frontend方式・依存・Security変更や実装/検証開始を決定したものではない。新しい開始票を連続作成する前に、既存資料を使って変更の必要性と最小範囲を短く示す。

### 8.3 直近コミットからP4-B1へ至る経緯（2026-10-17確認）

**実装の限定成果を固定し、復旧の難しい部分を人手で補完する境界を整理した。その成果と残課題を保持して非同期系の今回作業を区切り、当初計画のP4-B1へ移行する。** これを以下の履歴の現在の読み方とする。

確認範囲は、統合済みmainの基点`af9fa01`の後から、このbranchのHEAD `bbcf3ded5b836c9c43c9ac969e45639d6b262ffa`までの13コミット。`git log --reverse --stat af9fa01..bbcf3de`、各時点の文書本文・承認節、変更pathを照合した。履歴の書換え、過去結果の再実行、Git外の証拠現物の再照合は行っていない。

| 順序 / commit（いずれも2026-10-16） | 固定された内容 | 現在につながる判断 |
| --- | --- | --- |
| 1. `c749e44` | consume接続実証の限定開始票と関連する検討資料 | 採用範囲はconsume→単発復旧の一回呼出し接続。追加受付tableを含む過去案も文書に残るが、同commitへの収録だけを採用/開始承認とは扱わない |
| 2. `652e504` | 未登録Reference serviceとtest/fixture、接続実証・Owner受入 | consume正常復帰後の一回呼出しと非再実行の限定成果を固定。正式運用入口・FAILED完遂・実provider保証は含めない。**今回確認範囲の実装変更はここで区切られている** |
| 3. `2dc6d9a` | FAILED処置の方式比較、受理済み一件に限定したAの設計候補、人手HOLD方針の承認 | Aは候補留保。実装を進めず、人の照合・担当付きHOLDを維持する判断を既に採用 |
| 4. `142e06f` → `dbbe399` | 人手HOLD引継ぎ案、既存記録で補える範囲と不足の確認 | 新table/新Auditを増やさず、対象・証拠・禁止事項・担当・次確認を既存記録へ残す。実担当/保管先等はOPEN |
| 5. `493af09` | 未閉鎖permitの保証範囲と別publicationの扱い | 未閉鎖を維持し、別publicationは人が関連付ける。歴史closeや横断抑止機構を新設しない |
| 6. `959e48a` → `586db9b` | 逐次再要求、並行復旧・通常listener並走の境界 | 既存key/不一致拒否を維持。新しい再要求・並行復旧機構を追加せず、全停止型と不明時HOLDを採る |
| 7. `66783e7` | 全停止型の最小確認とHOLD境界 | 確認不足なら開始しない。開始後に条件が崩れたら追加要求を出さず、人の照合へ戻す |
| 8. `1ca4382` → `0294ee7` | FAILED観測値・引継ぎ、Audit失敗・上限・timeoutの扱い、残タスク整理 | 取得不能を正常扱いせず、自動再投入や新しいretry/counterを追加しない。消費/副作用/記録の確定範囲を分ける |
| 9. `505b947` | 保持/パージ境界 | 未解決HOLD等の必要記録を保全。不明なら削除しない。パージ基盤の追加・実行はしていない |
| 10. `bbcf3de` | 既存IDによる追跡・観測回復の境界 | 既存記録で照合し、不足を推測で結ばない。観測回復を再投入や案件解決へ直結させず、新しい相関/trace基盤を追加しない |

`652e504`後の11コミットの変更pathはすべて`docs/development/`内であり、production code・test・migration・依存の追加変更ではない。この期間は非同期機能を拡大し続けた実装履歴ではなく、既存機構の限界を確認し、人手補完と非再投入の方針を具体化した記録と位置付ける。個々の承認範囲・未達・OPENは各正本のまま保持する。

一方、HEAD後の未コミット作業では、現品質/DoDの集約から読取GET・権限/scope・提供準備を追加の次工程として検討した。これらは上記11コミットに記録された追加実装ではなく、GET等の正式採用や実装開始も成立していない。人手の読取・照合・HOLD手順は参照として残すが、追加機能や資料の整備を必須の継続工程にはしない。現在の変更文書は、この結論にそろえて整理した。

P4-B1へ移る理由は、非同期系の未達が解消したからではなく、**限定成果を固定し、残る復旧課題を人手補完・必要時再検討として扱う区切りをOwnerが定めたため**である。S1全体の完遂を他packageの開始条件にせず、当初計画で独立候補に位置付くSession SPA＋MVCへ進む。P4-B1ではDoD 4-8/4-9と既存MVC/REST/Session/CSRFの差分から最小範囲を定め、非同期復旧の残課題を抱き合わせない。

この追記は履歴の解釈と次の作業への引継ぎを明文化するものであり、過去のOwner承認・FAIL/PASS・保証範囲を取り消したり、未達を完了へ変更するものではない。

## 9. main統合後に進めるテーマ一覧（2026-10-17）

### 9.1 先に今回の成果をmainへ固定する

OwnerはP4-B1を大きな作業転換と位置付け、その着手前に今回の成果をcommit・PR mergeしてmainを更新し、タグを付ける方針を示した。**今回の一覧整理までを現branchの文書へ含め、main統合・チェックポイント固定後にP4-B1用の新branchへ移る。** P4-B1の設計/実装を今回のPRへ混ぜない。

1. 現在の変更文書5件（AGENTS、本indexのREADME、本棚卸し、現品質/残課題、読取経路案）を点検し、commitする。
2. 現branch全体をmain向けPRへまとめる。差分には文書だけでなく、`652e504`の受入済みReference service1件・test/fixture2件も含むことを明記する。
3. 最新remote mainとの関係・PR差分・必要なCIを確認し、PRをmergeする。
4. merge後mainのHEADとCIを確認し、そのcommitへチェックポイントタグを付ける。候補名は`checkpoint/v0.1.0-phase4-s1-async-boundary-main-20261017`（実施日が変わる場合は日付を合わせる）。正式releaseやS1全体完了を表すタグにはしない。
5. 更新済みmainからP4-B1用branchを作り、既存との差分確認を始める。

これは実施順序であり、既にcommit/PR/merge/tagを実行した記録ではない。操作後に実際のcommit・PR・merge HEAD・tagを記録する。過去の検証結果はそのsource/epochを維持し、merge後CIや追加実行の結果と混同しない。

### 9.2 Phase4のテーマと着手条件

§4の詳細棚卸しを、次の作業選定に使うテーマ単位にまとめ直したもの。新しいタスクの追加ではない。**次に進むのはP4-B1。以降の表順は計画IDによる一覧であり、実施順の確定ではない。** 各テーマは複数の設計・実装・検証を含むため、一つの開始判断やPRで全体を抱え込まず、代表経路と成果の区切りを定める。

| テーマ / 当初ID | 主に整えるもの・確認すること | 現時点の扱い |
| --- | --- | --- |
| **Session SPA＋MVC Reference / P4-B1** | 最小React参照と既存Thymeleaf MVCの併用、Session API、CSRF、認証/権限/競合とbrowser確認（DoD4-8/4-9） | **次の対象**。既存MVC/REST/Securityとの差分から範囲を絞る。Customer BFFとは別 |
| Customer REST・BFF接続 / P4-B2 | 案件API/DTO/error、認証profile、IdP、logout、結合Evidenceとgap審査 | Customer主導・実案件入力待ち。B1の代替達成にしない |
| MyBatis・accounting / P4-C0/C1 | adoption判断、分離モデル・規約、会計Reference、楽観lock・共通error（4-6） | **trigger待ち／DEFERRED維持**。採用前に依存・規約を追加しない |
| 外部API耐障害性 / P4-C2 | timeout/retry/同時実行・流量制御、受理不明、transaction/資源境界（4-7） | 独立候補。代表操作と副作用の意味を先に定め、Resilience4j採否を判断 |
| non-Web Batch / P4-C3 | Reference job、永続metadata、単一実行・二重起動拒否、停止後restart/rerun、Audit（4-10） | 既存Batch Repositoryの現状・互換性と運用Ownerを確認。非同期復旧の続編にしない |
| File・Object Storage / P4-C4 | Port/Adapter、保存とDB確定の順、権限・容量・失敗/cleanup | 独立候補。代表use caseとstorage範囲を選ぶ。local fileとObject Storageを区別 |
| 同期処理の観測 / P4-D1残り | OpenTelemetry、実trace/exporter/sinkとlog/metricの照合、context非漏えい | 同期境界を対象として切り出す。今回区切る非同期相関の未達を抱き合わせない |
| Virtual Threads / P4-D2 | 既定無効・opt-inガイド、Java21/25互換、負荷/contextとCI（4-11） | 独立候補。比較条件・検証枠を限定し、CI変更は個別判断 |
| Container・ECS / P4-D3 | 固定artifact/imageの起動・health・診断・停止再起動、secret/network/資源 | local containerと実ECSを段階分け。ECSはplatform入力・権限・費用待ち |
| SAML・認証境界 / P4-04・P4-EDGE | 認証終端、credentialの信頼境界、broker/OIDC/直接SAML等の採否と必要Adapter | 実案件/platform入力待ち。SAMLの当初成果物としての扱いは明示的に判断 |
| 正式受渡し・release / P4-E1 | 対象artifact、version/checksum、配布先/support、独立Consumer受取・診断 | 実チーム受入・release/platform判断が必要。今回のcheckpoint merge/tagとは別 |
| 条件付き拡張 / P4-OPT | Authorization Server、Oracleの必要性と採用範囲 | 明示trigger待ち、必須DoD外。自動で着手しない |

非同期系（P4-A1/A2・D1非同期）の残課題は§7.2〜7.3に保持し、今回の区切りと再検討条件は§8に従う。上表を順に進めるたびに非同期系へ戻る工程は置かない。Phase4全体の最終判定では、未達と採否判断を改めて対応付ける。

### 9.3 横断して残る採用支援

Phase4の機能テーマと別に、既存の採用支援5区分を維持する。機能テーマの直列工程にはせず、必要な入力・時期に合わせて扱う。

| 既存区分 | 内容 | 現時点の扱い |
| --- | --- | --- |
| AD-01 | 実チームのbuild/run/operation/diagnosisと受入Evidence | 実チーム入力・受入待ち |
| AD-02 | Linux/WSLでの開発・検証手順 | 実チームの試行結果を受けて反映 |
| AD-03 | 日常開発用Maven stage、IDE/CLI、更新・切替・診断 | 方式の採用・Tooling実装・試用は別判断 |
| AD-04 | Customer依存宣言の誤選択防止・検査 | Owner了解済みの検証保留を維持 |
| AD-05 | 開発者のartifact準備・不足/権限エラー時の診断 | 既存の限定LOCAL PASSと恒常方式の採用を区別して整理 |
