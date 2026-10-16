# S1-07：FAILED監視の最小定義と人への引継ぎ（2026-10-16）

状態：**観測値の定義・引継ぎ方針の運用時適用はOWNER APPROVED（§6）。Audit失敗・上限・timeout整理の方針・制限もOWNER APPROVED（§8）**。全停止型の最小確認・HOLD境界はOwnerが`66783e7`へcommitした。監視定義/承認は`1ca4382`へ固定済み。metric/alertの追加実装、監視運用の開始、実DB照会・通知送信は行わない。

## 1. 対象と目的

FAILEDや未解決HOLDを見つけて既存の一件記録へ引き継ぎ、放置や不用意な再投入を避ける。検知を復旧実行の許可にしない。既存の[人手HOLD](phase4-s1-r4-human-hold-handoff-guide-20261016.md)・[全停止境界](phase4-s1-full-stop-operational-boundary-review-20261016.md)を維持する。

[E15](phase4-s1-dod-demonstration-plan-20261005.md)が求めるalert受信から認可再送・終端照合までの実証を、本書の方針整理で代替しない。E16のAudit障害・E17の上限/代行/連絡不達の実動作全体は残課題とする。

## 2. 既存記録で定義できる観測値

[V5](../../koiki-reference-app/src/main/resources/db/migration/kkref-notification-async/V5__create_notification_async_publications.sql)のpublicationにはstatus、publication_date、completion_date、completion_attempts、last_resubmission_dateがある。FAILED遷移時刻専用の列やstatus変更履歴はない。新columnを加える前提を置かず、次の意味を分ける。

| 値 | 最小定義案 | 誤って意味付けしないこと |
| --- | --- | --- |
| 現在のFAILED件数 | 指定した環境/DB・対象listener範囲で、観測時点の`status=FAILED`のpublication行数。範囲と観測時刻を併記 | 未解決案件数、未配信数、全通知の失敗数ではない。FAILEDかつcompletion_dateあり等の矛盾は別途確認し、正常扱いで除外しない |
| publicationの年齢 | 観測時点とpublication_dateとの差。時刻不整合は調査対象 | FAILEDになってからの時間ではない。last_resubmission_dateもFAILED開始時刻ではない |
| 初回検知からの未解決時間 | 既存運用記録の初回検知時点から、当該一件を未解決として追跡している時間 | FAILED状態が連続した実時間ではない。初回検知前の失敗時刻を補間しない。status変化や担当交代だけで起点をリセットしない |
| completion_attempts | 現在行に保存されたModulithのattempt値を、その名称/対象とともに記録 | HTTP通知POST数・provider受理数・業務上の許容再試行上限ではない。値だけで再投入を許可しない |

初回検知時点には、既存運用記録の作成時刻や観測の明示値を使う案とし、新table・metric履歴基盤を作らない。実際の記録先と履歴/参照性はOPEN。正確なFAILED状態の継続時間が必須になった場合にだけ、取得方法を別reviewする。

今回の読取範囲ではReference notification内のFAILED専用metric/alertの実装・sinkへの接続成立を確認していない。libraryや別監視系が提供する値も含め、実運用で何を採用するかは未取得であり、自動検知済みとは扱わない。

## 3. 検知から人への最小引継ぎ案

1. **検知内容を残す**：環境・対象範囲・観測時刻・FAILED件数と、対象一件のpublication/event/listener/status/attemptを既存記録へ結ぶ。通知key原文/宛先/payloadは広く転載せず、必要な証拠への権限制御された参照を残す。
2. **既存案件と照合する**：同じ環境/publicationの記録があれば追記する。別publicationが同じ論理通知に見える場合は関連付けて人が確認し、新しい再送案件として自動受付しない。statusがFAILEDでなくなっても、未解決HOLDを自動終了しない。
3. **担当が引き受ける**：主担当/代行と次の確認内容・時点を記録する。自動通知がある場合も送信成功だけで引受け済みとせず、実際の確認/引受けを既存運用記録へ残す。担当不在・連絡不能は未解決のまま運用責任者へ判断を依頼する。

これらは既存記録と運用手順で行う方針案。自動受付、重複alert制御、paging、担当割当システムを新設する案ではない。現時点で実際の連絡・引受けやalert配備を実施したわけではない。

## 4. 観測不能・放置を避ける境界

- 取得失敗、DB/監視接続不能、範囲不明、古い値しかない場合は「0件」「正常」とせず観測不能を記録し、監視/運用責任者へ確認を依頼する。判明している未解決HOLDを消さない。
- provider受理不明、Reference受理記録なし、timeout、attempt値増加は、それ自体で再送可否や副作用終了を決める材料にしない。追加の復旧要求を出さず既存証拠の照合へ戻す。
- 確認期限超過・通知不達・担当未引受け時の連絡順と責任者を運用側で定める。それが未設定の段階では放置防止が成立したと認定しない。

確認間隔、FAILED件数/年齢/未解決時間の閾値、通知先、主担当/代行/責任者、受領確認期限、監視失敗の検知先と許容時間、sink/費用はOPEN。数値や実名を本書で仮定しない。手動確認を選ぶ場合も、担当と確認間隔が決まるまでは監視運用成立としない。

## 5. 今回の結論（§6でOwner承認）

**既存publicationと一件の運用記録を使い、FAILED件数・publication年齢・初回検知からの未解決時間を区別して扱う。取得不能を正常扱いせず、担当の引受けと次確認へつなぐ。** 新table・FAILED時刻column・監視/通知基盤は現段階では追加しない。

閾値・担当・通知先等を実運用で定めることは残課題。E15〜17の実動作、FAILED滞留起点の正式metric契約、認可再送/終端照合、DoD4-4は未達を維持する。具体的な不足が判明した場合だけ必要な実装/実証を別判断し、本書を実行開始票や運用配備済みの認定にしない。

## 6. 観測値・運用時適用方針のOwner承認（2026-10-16）

Ownerは本書を確認し、観測値の意味を理解したうえで、運用時に適用するものとして承認した。これを**本書の観測値の定義・引継ぎ方針と適用制限のOWNER APPROVED**として記録する。

FAILED件数、publication年齢、初回検知からの未解決時間を区別し、publication年齢をFAILED滞留時間と扱わない。取得不能を0件/正常に読み替えず、担当の引受けと次確認へつなぐ。新table・FAILED時刻column・監視/通知基盤は現段階では追加しない。

閾値・担当/代行・通知先・確認期限・sink等はOPENのまま。実際の運用準備で定めて適用する方針であり、監視配備・通知/引受け実施、E15〜17やDoD4-4の成立認定ではない。追加実装/検証・実運用操作・成果commit・remote操作の開始承認は含まない。

## 7. Audit失敗・試行上限・timeoutの最小処置（2026-10-16）

状態：**SOURCE/既存限定結果の対応付けと方針・制限はOWNER APPROVED（§8）**。OwnerはS1-07の残りの整理へ進むことを指示した。直前の残タスク棚卸しの未commit差分を保持し、本節は文書整理だけを追加する。実装・追加テスト・故障注入・運用操作は行わない。

### 7.1 既存機構との対応

| 場面 | 確定すること・残る不明 | 再実行せず行うこと |
| --- | --- | --- |
| consume transactionのAudit保存失敗 | rollbackが確認できた場合、消費とconsume Auditは保存されず、当該接続からrecoverは呼ばれない。元の通知がproviderで受理済みかは別問題 | この接続を自動retryしない。DB/Audit障害と元通知の受理状況を分けて確認し、HOLDへ記録 |
| consumeの正常復帰を確認できない | 消費がcommitしている可能性を残す。正常復帰しない接続はrecoverを呼ばないが、DB読取で消費を見つけても実行の許可にはならない | permit/operation/消費/Auditを照合。新permit・新operation・process再起動で処置を再開しない |
| recover正常復帰後の結果Audit失敗 | 復旧処理が正常復帰したことと結果記録の失敗を区別する。限定fixtureの正常復帰は実配信完了の証拠ではない | provider受理・Reference受理記録・対象publicationを照合し、operationと記録未了を既存運用記録へ残す。Auditを残すために復旧をやり直さない |
| recover異常と結果Auditの双方が失敗 | 送信/受理/完了のどこまで進んだかは例外だけでは分からない。現行接続は主例外にAudit例外を付加して返し、retryしない | 元の異常と結果記録未了を別々に記録し、証拠参照・担当/次確認を残す。結果Auditがないことを未実行としない |
| 認可拒否と拒否Security Auditの失敗 | 現行permit Serviceは拒否を維持し、固定logを残して例外を返す。拒否Auditが失敗しても許可へ変わらない | 操作を進めず、認可拒否とAudit障害を担当へ引き継ぐ。Audit不調を理由に権限/証拠検査を省略しない |

source根拠：[permit Service](../../koiki-reference-app/src/main/java/org/koikifw/reference/notification/application/RecoveryPermitService.java)、[接続Service](../../koiki-reference-app/src/main/java/org/koikifw/reference/notification/application/PermittedSinglePublicationRecoveryService.java)。上表の処置は人手引継ぎ方針であり、全経路の実運用成立や全例外の実証を示すものではない。

結果Auditの記録回復が必要になった場合も、元の記録を削除/改変したり、根拠なくSUCCESSを追加したりしない。既存運用記録への追記は正本Auditの補完完了を意味しない。正式な補完が必要ならactor・operation・事実/理由・実行権限を限定して別判断する。今回、Audit補完機能を新設しない。

### 7.2 一回消費と運用上の試行上限は別

既存permitは一回消費、受入済み接続はその正常復帰後にrecoverを一回呼ぶ。同じpermitの再提示からrecoverへ進まないが、これは環境/論理通知を横断する累積試行上限ではない。`expected_attempt`は対象snapshotの一致条件、`completion_attempts`はModulithの保存値であり、運用が許容する通知送信回数を定義するものではない。

現段階は新しい上限counterやretry制御を足さず、不明な結果やHOLDを新permit/別publicationでやり直さない。将来、承認済みの復旧操作に上限を設ける際は、数えるもの（操作要求/復旧呼出し/通知POST等）と範囲・時点・上限値・判断者を運用側と決める。上限値/到達状態が不明、又は到達が判明した場合は新たな復旧要求を開始せず、理由と現時点の根拠を引き継ぐ。上限到達を解決/未受理の証拠にはしない。

### 7.3 timeoutは待機終了であり、副作用終了ではない

[既存recover](../../koiki-reference-app/src/main/java/org/koikifw/reference/notification/application/SinglePublicationRecoveryService.java)は再公開後のlease完了待ちへ30秒を渡す。[DB lease](../../koiki-reference-app/src/main/java/org/koikifw/reference/notification/adapter/outbound/operational/JdbcIsolatedPublicationAdapter.java)はdeadline内で観測し、期限到達なら例外とする。この30秒は復旧全工程の厳密な上限や業務SLAではなく、HTTP/DB/transactionのtimeoutとも別である。

完了待ちの例外・lease close/lock解放が、非同期listenerの終了・provider未受理を証明するわけではない。HTTP応答待ちのtimeoutも受理の有無を確定しない。追加要求を出さず、既存処理の終了・provider受理・Reference記録・publicationを照合し、人手HOLDを維持する。timeout延長や新permitでの再要求を自動処置にしない。

運用の確認期限を超えた場合は、処理のtimeoutと混同せず、未引受け/未解決案件として代行・責任者へ引き継ぐ。通知送信成功だけで引受け済みとはせず、通知不達や担当不在は§3〜4の既存記録へ残す。具体的な期限・連絡順・代行者はOPEN。

### 7.4 既存限定結果と未実証の区別

[接続実証の受入結果](../architecture/validation/phase4-s1-r4-consume-recovery-connection-20261016.md)のQ03は実consume Audit失敗/rollbackと復旧入口0、Q05は実consume commit後の正常復帰喪失の模擬と非再開、Q07は模擬recover正常復帰後の実結果Audit失敗と非再実行を確認している。Q06はprovider UNKNOWNによるHOLDであり、実30秒timeoutを再現したtestではない。

実listener/providerの遅延とtimeout、運用累積試行上限、復旧異常＋結果Audit二次失敗、拒否Security Audit失敗、正本Audit補完、通知不達/代行/担当引受けを含むE16/E17の統合実証は今回実行していない。既存PASSをこれらへ広げない。

### 7.5 今回の結論（§8でOwner承認）

**Audit失敗・上限不明/到達・timeoutから自動で再投入せず、消費/副作用/結果記録の確定範囲を分け、人の照合と既存記録への引継ぎへ戻す。** 結果Auditの回復を復旧再実行と結び付けない。新しいretry・counter・Audit補完機構は追加しない。

これは既存機構への対応付けと運用方針案であり、E16/E17/DoD4-4完了ではない。運用上限・許容時間・代行/連絡・正式Audit補完等が必要になった時に、その不足を限定して別判断する。

## 8. Audit失敗・上限・timeout整理のOwner承認（2026-10-16）

Ownerは§7を確認し、整理内容を承認した。これを**既存機構への対応付け、非再投入・人手照合方針と適用制限のOWNER APPROVED**として記録する。

Audit失敗・上限不明/到達・timeoutから自動再投入せず、消費/副作用/結果記録の確定範囲を分けて人の照合と既存記録への引継ぎへ戻す。結果Auditの記録回復のために復旧を繰り返さない。一permit一回を運用累積上限とせず、timeoutを処理取消し/未受理確定としない。

既存Q03/Q05/Q07等の限定結果と未実証範囲を区別する。E16/E17/DoD4-4、正式Audit補完・運用上限・許容時間・代行/連絡のOPENは維持する。新しいretry/counter/Audit補完機構、追加実証、実運用操作、成果commit・remoteの開始承認や運用配備済みの認定は含まない。
