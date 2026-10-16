# S1 R5：既存IDによる追跡と観測回復の境界（2026-10-16）

状態：**追跡/観測回復の方針と制限は、運用設計時の反映を前提にOWNER APPROVED（§6）**。S1-09の初回として、追える範囲と観測不能からの人手照合を整理する。基点はcleanな継続branch、HEAD `505b947`。新しいtrace/保管基盤・実装・故障注入・追加テスト・実sink/DB/provider操作は行わない。

## 1. 目的と適用限界

一件の通知について、publication・受理・permit/operation・Audit・HOLD記録を既存IDで照合できる範囲を明らかにする。観測不能ならHOLDを維持し、回復後は新しい観測時点の材料で再照合する。観測回復を復旧再実行の許可にしない。

[E20/E21](phase4-s1-dod-demonstration-plan-20261005.md)の実trace関連・無関係requestへの非漏えい・sink障害からの回復/解決を、本書の文書整理でPASSにしない。保持は[承認済みR5境界](phase4-s1-r5-retention-purge-boundary-review-20261016.md)に従い、未解決案件の必要材料を消さない。

## 2. 既存IDでたどる道筋

| 起点 | 既存の照合先 | 限界/確認事項 |
| --- | --- | --- |
| 環境＋publication ID | publicationのlistener、serialized eventとevent ID、status/attempt/時刻 | 環境とDBの対応を一緒に確認する。eventとpublicationを常に1対1と決めない。現在行だけでは過去時点の証拠にならない |
| eventと通知key | [ExpenseApprovedEvent](../../koiki-reference-app/src/main/java/org/koikifw/reference/expense/domain/event/ExpenseApprovedEvent.java)のkey生成規則、宛先/payload hash | event IDとkeyは別識別。同keyの別eventが存在し得るため、IDのどれか一つが一致するだけで全同一としない |
| 通知key/受理ID | [Reference受理観測](../../koiki-reference-app/src/main/java/org/koikifw/reference/notification/adapter/outbound/persistence/JdbcNotificationReceiptLogAdapter.java)のevent/宛先/payload hash/観測時点とproviderの受理証拠 | Reference行がなければprovider未受理とはしない。provider側の保持/照会契約はOPEN。通知key原文や宛先を共有logへ集めない |
| permit ID | [既存read](../../koiki-reference-app/src/main/java/org/koikifw/reference/notification/application/RecoveryPermitService.java)の環境/発行時target、actor、消費operation/worker世代/時刻 | 現在権限/scopeを維持して照会。permitがないHOLDもある。消費は送信/完了の証明ではない |
| operation ID | 消費→permit→発行時target、[結果Audit](../../koiki-reference-app/src/main/java/org/koikifw/reference/notification/application/PermittedSinglePublicationRecoveryService.java)のoperation resource | 結果Auditが欠落する場合がある。Audit不存在を未実行としない。全対応がFKや自動JOINで保証されているわけではない |
| process/worker世代と観測時点 | 元process停止材料、fixture/run/source/artifact、HOLD記録 | worker文字列やPID単独を本人認証・全instance停止の証明にしない。別端末tmpはGitでは移送されない |
| requestId/traceId | 取得されている場合のAudit/logの補助参照 | AuditStoreはMDCの値を保存するが、非同期/別processへの親子関係、span採取/export/sink到達が成立する証拠ではない。欠落を推測で埋めない |

schema/保存の根拠は[V4](../../koiki-reference-app/src/main/resources/db/migration/kkref-notification/V4__create_notification_recovery_records.sql)、[V5](../../koiki-reference-app/src/main/resources/db/migration/kkref-notification-async/V5__create_notification_async_publications.sql)、[V6](../../koiki-reference-app/src/main/resources/db/migration/kkref-notification-async/V6__create_notification_receipt_observations.sql)、[AuditStore](../../koiki-starters/koiki-starter-audit/src/main/java/org/koikifw/audit/internal/AuditStore.java)。必要な担当者が権限制御された元記録へ到達し、既存の一件記録へ出所/時点と参照を残す方針とする。新しい共通相関table/受付IDは作らない。

## 3. 既存HOLD logのhashを取り違えない

[SinglePublicationRecoveryService](../../koiki-reference-app/src/main/java/org/koikifw/reference/notification/application/SinglePublicationRecoveryService.java)は[RecoveryHoldException](../../koiki-reference-app/src/main/java/org/koikifw/reference/notification/application/RecoveryHoldException.java)の`keyHash`引数へ`selection.payloadSha256()`を渡す。このlog項目名だけを見て通知keyのSHA-256と判断してはいけない。payload hashは本文整合用であり、通知key自体のhashと用途が異なる。

[R3-Bの既存記録](../architecture/validation/phase4-s1-r3b-source-preflight-and-blocking-review-20261014.md)はこの限界を保持し、B03aの人手HOLDやB03cの独立証拠に`notificationKeySha256`を別途保存している。照合時はどの値を何から計算したかを確認し、通知key hashが必要ならその独立証拠等を参照する。既存logを遡って書き換えたり、同じ64桁だから同一用途として照合したりしない。

本節は既知の意味上の制限をsourceへ再対応したもの。今回コード訂正や過去Evidenceの差替えはしない。独立証拠も取得できなければ、相関を推測せずHOLDの不足材料として残す。

## 4. 観測不能から回復した時の最小手順案

1. **不能を記録して止める**：見えない対象/情報源と時点、最後に確認できた事実、担当/次確認を既存運用記録へ残す。DB/sink/providerの取得不能を0件/未受理/成功としない。追加の復旧要求は出さない。
2. **回復後に新しい材料を取得する**：許可された読取手段で同じ環境/対象の現在値・受理証拠・消費/結果Auditを確認し、新しい観測日時/取得元を添える。実際の権限・手段・担当は運用側で定める。sinkに過去データが戻ったことと、現在の状態が確認できたことを分ける。
3. **前後を照合して引き継ぐ**：前のsnapshot/記録を消さず、変化・欠落・不一致・未完了箇所を追記する。矛盾や対応不明ならHOLDと照会先を更新する。材料が揃っても、本書を根拠に再送/close/Aの処置を始めない。

「観測回復」は情報を再取得できること。「案件解決」は必要な副作用・記録・publication等の整合が確認できることであり、同じ状態ではない。現段階で未実装のFAILED完遂等に当たる場合は、観測が回復しても未解決のまま判断窓口へ引き継ぐ。担当への正式な引継ぎも、単に資料を作成しただけで実施済みとはしない。

## 5. 今回の結論と残課題（§6でOwner承認）

**既存IDと権限制御された記録への参照で照合し、不足を推測で結ばない。観測不能時はHOLD、回復後は新しい材料で前後を照合し、自動再投入へ進まない。** 新しい相関台帳、trace基盤、MDC継承処理は現段階では追加しない。

実traceの生成/採取/export/sink、非同期・別process関連、無関係requestへのcontext非漏えい、sink障害/復元、実運用の取得者/権限/保管先/保持と引継ぎ先は未確定又は未実証。実traceの不足を業務ID照合で充足したとはしない。

E20/E21・DoD4-12、R5全体は未達を維持する。実装/実証が必要になった時に、保証対象と必要差分・実行上限を別判断する。本書を実DB/sink/provider操作、証拠移送、追加実証、成果commit/remoteや正式運用開始の承認にはしない。

## 6. 追跡/観測回復・運用設計反映のOwner承認（2026-10-16）

Ownerは本書を確認し、運用設計時に反映を意識しておく前提で承認した。これを**本書の追跡/観測回復方針と適用制限のOWNER APPROVED**として記録する。

既存IDと権限制御された記録への参照を使い、不足を推測で結ばない。観測不能時はHOLD、回復後は新しい材料で前後を照合し、自動再投入へ進まない。観測回復と案件解決を分け、MDC/traceId欄を実trace成立の証拠とせず、既存HOLD logのhashの意味を取り違えないことを運用設計へ反映する。

取得者/権限/保管先/保持/引継ぎ先等はOPENを維持し、実際の運用設計への反映完了・配備/引渡し済みとは認定しない。実trace・sink回復・context非漏えい等の実証、E20/E21・DoD4-12・R5全体の未達は変更しない。新しい相関/trace基盤、追加実装/検証・実操作・成果commit・remoteの開始承認は含まない。
