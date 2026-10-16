# S1-07：FAILED監視の最小定義と人への引継ぎ（2026-10-16）

状態：**観測値の定義・引継ぎ方針を運用時に適用することはOWNER APPROVED（§6）**。全停止型の最小確認・HOLD境界はOwnerが`66783e7`へcommitし、clean sourceを確認した。今回はS1-07の初回として、FAILEDの検知材料と人への引継ぎだけを整理する。metric/alertの追加実装、監視運用の開始、実DB照会・通知送信は行わない。

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
