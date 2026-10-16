# S1 R5：残す記録と削除できない条件（2026-10-16）

状態：**現段階の保持/削除境界と適用制限はOWNER APPROVED（§6）**。S1-08の初回として、現在の人手HOLD方針と既存記録の保持関係を整理する。基点はcleanな`feature/phase4-s1-r4-consume-recovery-connection`、HEAD `0294ee7`。保持日数・削除方式の採用、パージ実装/実行・DB操作・追加テストは含まない。

## 1. 目的と現行source

未解決案件の照合材料と重複抑止に必要な記録を、単に古いという理由で消さない。永久保存を一律に採用するのでもなく、削除候補と保護対象を分け、未確定なら削除しない境界を決める。

[既存S-11](phase4-s1-safety-and-recovery-conditions-20261005.md)と[E18/E19](phase4-s1-dod-demonstration-plan-20261005.md)は、保持期限超過COMPLETEDのみの削除、未処理/FAILED/復旧証跡の保全、単一実行と競合時の停止を要求する。現在の[非同期設定](../../koiki-reference-app/src/main/resources/application-s1-reference-async.properties)はcompletion-mode=UPDATE。今回確認したReference main sourceにはnotification専用パージ処理を見つけていない。DB管理者・外部job・provider側の削除/保持経路まで調査済みという意味ではない。

## 2. 残す記録と役割

| 記録 | 保持する理由 | 今回の削除境界案 |
| --- | --- | --- |
| publication | 対象event/listener、現在状態/attempt、発行/完了時点を照合する | 非COMPLETED、状態矛盾、未解決HOLD/未閉鎖permit/復旧中の参照があるものは除外。COMPLETEDでも自動的に削除可とはしない |
| Reference受理観測記録 | 通知keyとevent/宛先/payload/受理IDの対応を追う | publication削除に連動削除しない。未解決・照合/重複確認に必要なもの、保持条件未確定のものを消さない |
| provider受理証拠・idempotency key契約 | 実受理、同keyの抑止・内容一致・有効期間を確認する | provider側の管理責任・実保持条件がOPEN。Referenceから削除/期限更新を指示する設計にしない。期限切れを再送許可に変えない |
| permit・append-only消費 | 発行時target、操作者、operation、消費時刻と一回消費を追う。未閉鎖permitは同環境/publicationの追加発行を制約する | 未閉鎖permitとその消費を消さない。期限切れ/消費済みでも保護。閉鎖済み記録の保持/削除も今回のpublicationパージと別判断 |
| Business/Security/結果Audit | 権限判断・操作・結果記録とoperation/permitを追う | 独自の保持責任を持つ。publicationとまとめて削除せず、未解決/照合/記録回復に必要なものを残す。正本Auditを運用記録の要約だけで置き換えない |
| 運用の一件記録・HOLD証拠・snapshot | 観測時点の対象、受理、禁止事項、担当/次確認と出所をたどる | 未解決、引継ぎ未了、必要証拠の到達性/保持未確認なら削除しない。端末tmpを消せばよいとはしない。不要な原文payload等を無制限に複製する方針でもない |

記録の対応は[既存記録点検§7〜8](phase4-s1-r4-human-hold-handoff-guide-20261016.md)を利用する。主なschema根拠は[V4](../../koiki-reference-app/src/main/resources/db/migration/kkref-notification/V4__create_notification_recovery_records.sql)、[V5](../../koiki-reference-app/src/main/resources/db/migration/kkref-notification-async/V5__create_notification_async_publications.sql)、[V6](../../koiki-reference-app/src/main/resources/db/migration/kkref-notification-async/V6__create_notification_receipt_observations.sql)。

## 3. publicationを削除候補にできる最低条件

将来の限定パージを検討する場合も、次のすべてを確認したものだけを候補とする。候補になっても、本書だけで削除操作を許可しない。

1. **完了が整合している**：status=COMPLETEDとcompletion_dateが整合し、対象/時刻に矛盾がない。FAILED/未処理/PROCESSING/RESUBMITTED、不明な状態や不整合は除外する。
2. **承認された保持期限を超えている**：publication削除候補の年齢は完了時刻を起点とする案。publication発行時刻・最終再公開時刻を完了後の保持起点に置き換えない。保持期間と時計の扱いが未確定なら削除しない。
3. **未解決の参照がない**：未解決HOLD、未閉鎖permit、結果Audit記録回復/調査、別publicationにまたがる同論理通知の未解決照合、復旧中の利用対象ではない。関係が不明なら除外する。
4. **削除後も必要な照合ができる**：必要記録の保存先/参照権限/保持と、publication削除後に元対象をたどれる材料を確認する。実際の参照関係と保管保証がOPENなら除外する。
5. **単一実行と復旧との競合を制御できる**：削除時に状態/参照を再確認し、復旧/調査が同時に始まる窓をどう防ぐかを別設計で確定する。事前SELECTや復旧側lockがあるだけで成立としない。

現在は保持期間・参照の収集元・競合制御等が未確定なので、実行可能な削除対象一覧は作らない。既存APIがCOMPLETEDを期間で削除できる場合でも、上記の参照除外/競合制御が自動で備わると推定しない。広い一括削除や手書きUPDATEで候補を作る方針は採らない。

## 4. 寿命と依存関係の注意点

- **COMPLETEDでも未閉鎖permitがあれば除外する。** 現行closeは現在targetを要求し、対象を先に削除すると後からのcloseが拒否され得る。[未閉鎖維持方針](phase4-s1-r4-human-hold-handoff-guide-20261016.md#9-未閉鎖permitの保証範囲と別publicationの扱い2026-10-16)を、容量確保のためにclose/削除して回避しない。
- **受理記録・keyが残ることだけで再送安全とはしない。** 既存dispatchは通常時に受理記録を事前参照してPOSTを省く機構ではない。provider側の保持/契約が必要であり、期限が不明/切れたら照合不能としてHOLDする。保存期限の延長だけで新しい再投入許可は生じない。
- **DBの参照制約だけでは全保護対象を判定できない。** V4消費にはpermitへのFKがあるが、publication・受理記録・Audit・運用記録がすべてFKで結ばれているわけではない。一件の運用記録にある未解決HOLDを機械的に除外する仕組みも未接続で、取得不能なら削除を進めない。
- **保存用複写と削除許可は別。** backup/別保管先があるだけでは元記録を削除してよいとはしない。必要な内容・取得者/時点・参照/復元可能性の確認を別に行う。本整理では複写/移送/復元試験をしていない。

## 5. 今回の結論とOPEN（§6でOwner承認）

**未解決HOLD・未閉鎖permit・復旧/調査中の参照に必要な記録を残し、保持/参照/競合条件が不明なら削除しない。publicationは保持期限超過COMPLETEDを出発点に、保護対象を除外できる時だけ削除候補とする。** 記録ごとに保持責任を分け、publicationと一括で消さない。新しい保管台帳やパージ基盤は現段階で追加しない。

保持日数/完了後の起点の正式採用、provider key/証拠の実保持、削除権限、既存外部削除job、HOLD参照の収集方法、単一実行/復旧競合、backup/復元、容量逼迫時の連絡先はOPEN。容量不足や保管不能は運用責任者へ引き継ぎ、例外削除を自動許可しない。無期限保存の実装・容量保証を決定したものでもない。

E18/E19、DoD4-5、R5全体は未達を維持する。次に実装を必要とする具体的事実が出た場合に、対象/権限/件数/競合モデル/実行上限を別開始判断で定める。C3 Batchや実運用パージの開始、DB/provider操作、追加実証、成果commit/remoteの許可にはしない。

## 6. 保持/削除境界のOwner承認（2026-10-16）

Ownerは本書を確認し、現時点で整理された内容を認識したうえで承認した。これを**現段階の保持/削除境界と適用制限のOWNER APPROVED**として記録する。

未解決HOLD・未閉鎖permit・復旧/調査に必要な記録を残し、保持/参照/競合条件が不明なら削除しない。publicationは保持期限超過COMPLETEDを出発点に保護対象を除外できる時だけ削除候補とし、受理記録・provider key・permit/消費・Audit・運用証拠を連動して一括削除しない。

保持日数/起点の正式採用・削除権限・参照収集/競合制御・provider保持・backup/復元等はOPENを維持する。本承認を削除候補の現物確定、パージ実装/実行許可、全削除経路の保護成立、永久保存/容量保証、E18/E19・DoD4-5・R5全体完了とは扱わない。追加実装/検証・DB/provider操作・成果commit・remoteの開始承認は含まない。
