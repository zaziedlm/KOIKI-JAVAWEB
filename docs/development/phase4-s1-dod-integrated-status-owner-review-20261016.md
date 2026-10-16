# S1-10：非同期基本機能の提供・現品質・残課題（2026-10-16）

状態：**非同期系の今回作業を区切るための現品質・人手補完・残課題の整理**。整理開始時の基点はcleanな継続branch、HEAD `bbcf3ded5b836c9c43c9ac969e45639d6b262ffa`。追加実行や過去tmpの現物再照合ではなく、Repository内の正本・限定結果・Owner承認を集約する。S1全体の完了を現在の作業目標や一律の提供前提にはしない。新しいDoD採用/改訂・実装/検証開始・remote操作は行わない。

## 現行判断：今回の非同期系作業の区切り（2026-10-17）

Ownerは、現時点の実装に区切りをつけ、復旧で解決しきれない部分は人の照合・担当付きHOLDによる補完を優先し、当初Phase4の他の残タスクへ移行すると指示した。本書の提供準備・最小差分の具体化は継続必須とせず、既存成果・保証範囲・残課題の参照材料として保持する。追加GETや復旧機構、組込み手順の作成を移行の前提にしない。

限定受入済みの成果、未達のDoD、実担当・保管先等のOPENはそのまま残す。人手補完の方針を、実運用体制の成立やS1全体の完了へ読み替えない。再開は具体的な利用上の不足・不具合・運用要件が判明した時に対象を限定して判断する。次の作業入口は[残タスク棚卸し§8](phase4-continuation-task-inventory-20261016.md#8-非同期系の区切りと当初phase4残タスクへの移行2026-10-17)。本書は成果と残課題の参照資料とし、次の非同期実装工程を指定しない。

## 0. 基本機能の利用と品質の考え方

非同期event連携の基本機能を実案件の開発に沿って利用し、必要な品質・運用を補う。S1全体の完遂を一律の提供前提にはしない。メール配信は処理ライフサイクルの異なる処理をつなぐ一例であり、メール固有仕様を基礎機能の前提にしない。

希望提供時期は2026年10月末。未達をPASSとせず、開発向けの利用判断と本番運用の判断を分ける。今回の非同期系作業は現時点で区切り、追加整備は具体的な必要が判明した場合に判断する。

## 1. 判定の読み方

- **限定実証受入**：特定source/artifact/runで成立しOwnerが受け入れた範囲。個別結果は維持する。
- **方針承認**：現在の保証範囲・停止/HOLD・運用設計へ渡す判断の承認。実装・運用配備・統合実証の完了とは別。
- **未達/OPEN**：要求を満たす実証がない、既知FAILが残る、又は実運用情報が未取得。方針承認で免除しない。

新規7件や通常rootのPASSを加算してDoD全体PASSにしない。初回FAIL・訂正後PASS・模擬HOLDと、通常rootでの専用skipも分ける。case一覧は見直し可能であり、21ケースすべての組合せを機械的に増やす前提でもない。

## 2. 提供範囲を判断するためのDoD・現品質の対応

原文は[Grand Design §27.8](../architecture/grand-design/KOIKI-JavaWeb-FW_グランドデザイン_v0.2.md)。下表は内容の要約であり、原文を変更しない。[E01〜21実演計画](phase4-s1-dod-demonstration-plan-20261005.md)の追加条件/残枝は別に対応付ける。

| DoD / 原文の要求 | 受入済みの材料・承認済み方針 | 不足と判定案 |
| --- | --- | --- |
| 4-1 承認時の非同期メール通知、送信失敗でも承認非rollback | R1のローカルstub正常通知、R2の失敗分離・同期拒否/rollback保護を限定受入 | メール/実providerとしての達成範囲と最終受入が未確定。**限定成立材料あり、DoD全体PASSは未認定** |
| 4-2 強制終了後、未処理イベントが再起動後に配信 | R3-A/BでPUBLISHED/PROCESSING等の選択停止復旧・完了、受理照合、HOLD。全停止確認/不明時HOLD方針を承認 | B03a/E10aのFAILED完遂、実運用の停止維持/再開・許容時間等が残る。**未達**。隔離手動復旧の結果を一般的な再起動復旧保証にしない |
| 4-3 同一イベントの二回配信で通知が二重送信されない | 安定key・stub同内容受理再利用/異内容拒否、選択復旧時の受理一意性。E08逐次/並行/並走の限界と人手HOLD方針を承認 | Reference逐次全経路、同key内部競合、通常listener/復旧並走、key寿命と別publication等の保証が未実証。**未達**。追加POST0・受理一件・正常完了を同一視しない |
| 4-4 FAILED件数/滞留時間がmetricに現れ、再送手順が実行可能 | permit/Identity/Audit基盤とconsume→recover一回→結果Auditの限定実証。FAILED観測値・引継ぎ・Audit失敗/上限/timeoutの方針を承認 | 正式入口/肯定proof、採用metric/滞留起点、監視・alert・担当引受け・認可処置/終端結果の統合実演が未完。**未達**。publication年齢や初回検知後の未解決時間をFAILED状態の継続時間へ読み替えない |
| 4-5 完了済みpublicationのパージjobが単一実行基盤から起動 | 未解決HOLD/未閉鎖permit等の保全、COMPLETED削除候補の除外条件を方針承認 | 保持値、参照/競合制御、単一実行job、削除/復元の実証が未完。**未達**。保持方針承認をjob実装としない |
| 4-12 非同期listenerへ相関IDが伝播し、log追跡可能 | R1/R3の相関材料、既存IDによる照合経路、観測回復後の新材料による照合方針を承認 | HTTP→非同期→別processの統合追跡と非漏えい、実運用sinkの対応が未完。**DoD全体PASSは未認定**。実trace/export/sink回復はE20/E21側の追加実演条件として別に未実証を残す |

DoD 4-12の原文そのものを「実trace必須」と書き換えない。一方、採用済み実演計画側の実trace条件を文書整理だけで充足・不要とも扱わない。適用範囲を変える必要が生じた場合は、要求/実演条件のどちらを変更するのかを明示してOwner reviewへ戻す。

## 3. 実演caseと主な限定証拠の索引

以下はE分類との対応索引であり、全case一括PASS表ではない。method正式名・hash・時刻・停止位置とassertionはリンク先の正本に残す。

| E分類 | 主な既存材料/不足 | 結果正本 |
| --- | --- | --- |
| E01 | R1の承認→非同期stub受理→publication完了、条件登録・通常保護を限定受入 | [R1 §13〜15](../architecture/validation/phase4-s1-r1-implementation-and-early-verification-20261012.md) |
| E02/E03 | R2失敗分離と拒否/rollback保護。R2-04初回FAILと局所訂正後のPASSを区別 | [R2 §4〜5](../architecture/validation/phase4-s1-r2-selected-seven-case-results-and-acceptance-input-20261013.md) |
| E04/E05/E06 | R3-AのA01/A02/A03の選択停止復旧。A01旧XML欠落と新epoch結果、A04a初回FAILも履歴保持 | [R3-A §22〜23](../architecture/validation/phase4-s1-r3a-implementation-and-a01-stop-20261013.md) |
| E07/E09 | R3-BのB01、B02a〜cと契約2件の限定PASS | [R3-B §22〜23](../architecture/validation/phase4-s1-r3b-source-preflight-and-blocking-review-20261014.md) |
| E08 | D01はstub逐次契約、C04は独立通知の材料。Reference逐次/並行/並走の代用ではない | [E08方針 §6/8](phase4-s1-e08-sequential-replay-boundary-review-20261016.md) |
| E10 | B03b/cとB03a人手HOLD別methodは限定PASS。元B03a自動完了FAILは維持 | [R3-B §22〜23](../architecture/validation/phase4-s1-r3b-source-preflight-and-blocking-review-20261014.md) |
| E11〜E14 | A04a〜d等の選択拒否材料。A04dはTooling接続の既占有lock、復旧process同士の競合ではない。全停止/再起動/喪失後の窓の包括保証は未達 | [R3-A §22〜23](../architecture/validation/phase4-s1-r3a-implementation-and-a01-stop-20261013.md)、[全停止境界 §6](phase4-s1-full-stop-operational-boundary-review-20261016.md) |
| E15〜E17 | Q01〜Q07と直接回帰4件はconsume接続の限定証拠。Q04の例外原因は識別せず、Q05は実通信断/OS killではなく、Q06は実timeoutではない。metric/上限/代行/正本Audit補完等の統合実証は未達 | [接続実証 §2/5](../architecture/validation/phase4-s1-r4-consume-recovery-connection-20261016.md)、[監視/異常方針 §6/8](phase4-s1-failed-monitoring-handoff-boundary-review-20261016.md) |
| E18/E19 | 保持/削除境界の方針のみ。job/二起動/復旧競合/復元の実証なし | [R5保持 §6](phase4-s1-r5-retention-purge-boundary-review-20261016.md) |
| E20/E21 | 既存ID照合と観測不能/回復の方針。実trace/実sink回復・解決/引継ぎの統合実証は未完 | [R5追跡 §6](phase4-s1-r5-correlation-observation-recovery-review-20261016.md) |

R1/R2/R3-A/R3-B/接続実証は別source/artifact/run epochの限定結果。通常rootの実行数も300→303→305等と異なる。接続実証時のrootは305実行PASS＋専用99 skipであり、99 skipを実証PASSへ加えない。過去hashは各正本を参照し、今回のHEADを過去の検証sourceと読み替えない。Gitにないtmp/XML/JARの現物存在・再照合は今回確認していない。

## 4. 方針承認後にも残る責任・リスク

- **実処置の不足**：正式operator入口・現在権限/proof供給、FAILED完遂、歴史close、別publication横断抑止、停止維持/fencing等。今回の人手方針は危険な再投入を避ける出口であり、技術的な配送保証を代替しない。
- **運用入力**：担当/代行/連絡、記録先/権限/保持、監視閾値/期限/sink、停止/再開、provider保持/受理証拠、許容時間。未取得のまま運用成立や正式引渡し済みとしない。
- **未実証の機構**：R5のjob/競合/復元、実trace/sink回復、統合された認可/alert/処置/終端確認。新table・新状態・汎用自動復旧を必要と決めたわけではないが、必要な保証を文書だけで満たしたとも言わない。

既存受付table案は保留、FAILED対応Aは設計候補に留め、既存key/不一致拒否と全停止型・不明時HOLDを維持する。未閉鎖permitを保持し、記録やAuditの欠落/期限切れを新しい操作許可にしない。これらの既決方針を本統合で再オープンしない。

## 5. 現時点で区切る成果と人手補完

- event発行、commit後の非同期listener、publication保存・完了、失敗分離について、R1/R2等の限定受入結果を維持する。
- 処理正常・拒否・失敗・結果不明を区別する材料は§8、既存記録の読取順・照合・HOLD引継ぎは[読取経路案§9](phase4-s1-publication-read-path-minimal-design-review-20261017.md#9-担当者向け一件の読取照合holdへの戻し方)を参照する。
- 復旧の不明点は人が照合し、担当付きHOLDで管理する。確認不足・記録欠落・期限切れから再投入を許可しない。
- Reference実装例とFrameworkの正式契約・成果物を区別する。限定結果を実provider配信や一般的な自動復旧保証へ広げない。

## 6. 残課題の扱い

§2〜4に示すDoD未達と運用入力は保留事項として保持する。追加GET、読取権限/scope拡張、FAILED処置A、復旧機構、組込み資料の追加整備は今回進めない。他のPhase4タスクへの移行条件にも置かない。

実案件の利用上の不足・不具合・運用要件が具体化した場合に、必要範囲だけを再開対象として判断する。正式release・実チーム受入は既存の別判断を維持する。

## 7. 当初Phase4タスク内での対応付け（2026-10-17）

非同期系の成果・残課題は[Phase4計画草案のP4-01と§4.1](KOIKI-JavaWeb-FW_Phase4実施計画_見直し草案_v0.1.md)、[S1目的/段階対応](phase4-s1-purpose-dod-stage-mapping-20261009.md)に対応する。新しい独立packageや共通非同期基盤計画を立てない。既存草案の旧「未成立」等は後続の受入記録と区別する。

| 成果・残課題 | 当初Phase4/S1での位置 | 守る境界 |
| --- | --- | --- |
| event保存・commit後の非同期実行・完了状態 | P4-01、P4-A1/A2、S1-R1/R2、DoD4-1/4-2 | 既存Reference経路とSpring標準機構を基礎にする。独自event busや新しい汎用moduleを追加しない |
| 正常/拒否/rollback/非同期失敗の把握 | P4-A1/A2、S1-R2/R4、DoD4-1/4-4 | 既存Identity/認可/Auditとpublicationを使い、業務上の不正判定をFrameworkへ移さない |
| 処理状態・関連記録・失敗対象の参照 | P4-D1の失敗観測とP4-01、S1-R4/R5、DoD4-4/4-12 | 人手による照合を採り、追加の照会機能は実装しない。新しい監視製品や汎用管理consoleの導入を前提にしない |
| 停止/結果不明からの照合・非再投入 | P4-A1/A2、S1-R3/R4 | 既決の全停止型・人手HOLD・未閉鎖維持を使い、一般的な自動復旧へ広げない |
| 保持/追跡と提供方法の継続整備 | P4-A1/D1、S1-R5。正式配布は既存S2/P4-E1の判断 | 保持/追跡の残課題を記録し、開発向け提供資料の準備を正式release/共通API昇格と混同しない |

P4-C2の汎用外部API耐障害性、P4-C3のBatch、メール専用Adapter/大量宛先配信等を、今回の非同期基礎の作業へ自動的に加えない。関連が生じた場合も当初の担当/判断位置へ戻す。

## 8. 正常・不正/異常の判断：現行経路の具体化（2026-10-17）

以下はReferenceにおける既存実装/限定結果の読み方。新しいDB状態enumではなく、開発者/運用者の判断に必要な区別である。結果正本は§3、現在sourceのtest入口は[正常経路](../../build-support/reference-e2e-verification/src/test/java/org/koikifw/buildsupport/referencee2e/PackagedReferenceAsyncNotificationTest.java)と[失敗分離](../../build-support/reference-e2e-verification/src/test/java/org/koikifw/buildsupport/referencee2e/PackagedReferenceAsyncFailureIsolationTest.java)を参照する。今回は再実行していない。

| 確認したいこと | 既存の判断材料 | 読み違えてはいけない点 |
| --- | --- | --- |
| 正常に処理されたか | 業務承認/Audit、対象publicationのCOMPLETED、Reference受理記録とstub受理の対応。R1正常経路・独立2通知の限定証拠 | Web成功だけでは非同期完了ではない。COMPLETEDは当該listenerの正常終了であり、実案件の外部処理の最終完了条件は別に定める |
| 実行前に拒否されたか | R2の権限不足/部門scope/古いversion等の拒否、業務状態/Audit、publication/送信なし | publicationがないことだけで拒否原因を断定しない。入力/業務上の正当性・認可判断は既存Application/Securityの責務 |
| 保存失敗で業務確定が取り消されたか | R2のpublication保存/承認Audit保存失敗時のrollback・副作用なしの限定証拠 | 非同期実行失敗とは別。transaction不成立を「受付成功・後段FAILED」として扱わない |
| 業務確定後に非同期処理が失敗したか | R2のprovider失敗時に業務承認/Auditを保持する限定証拠、対象publicationの失敗と関連記録 | 業務確定の失敗へ戻さない。FAILEDだけでは外部未受理/未実行とは分からず、原因/受理証拠の照合が必要 |
| 未処理/処理中か、結果がまだ確定しないか | 現在のpublication status/attempt/時刻、process/受理/結果Audit。R3以降とQケースの限定証拠/適用限界 | 経過時間やtimeoutを処理終了/未受理としない。不明なら照合・HOLDとし、自動再投入しない |

### 8.1 現在あるものと、利用者向けに足りないもの

現行Referenceには[permit照会](../../koiki-reference-app/src/main/java/org/koikifw/reference/notification/application/query/RecoveryPermitView.java)があるが、対象はpermit/消費であり、通常の非同期publication全体の状態・失敗理由を照会する入口ではない。通常経路の材料はDB、受理記録、log/AuditとTooling観測に分かれる。fixtureが直接SELECTできることを、開発者/運用者向けの照会機能が整ったことと同一視しない。

状態・関連記録の確認は[読取経路案§9](phase4-s1-publication-read-path-minimal-design-review-20261017.md#9-担当者向け一件の読取照合holdへの戻し方)の人手手順で補完する。担当者が許可された経路で対象一件を読み、保存値・受理観測・permit・Audit/logを照合する。確認不能はHOLDへ戻し、保存されていない失敗理由を推測しない。

一件GET/専用読取権限は設計候補として検討したが、既存scopeが最大30分・起動時固定という制約もあり、今回は実装しない。対象担当・接続権限・記録先等の実運用準備はOPENとして残す。
