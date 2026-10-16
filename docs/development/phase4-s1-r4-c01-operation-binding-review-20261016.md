# S1 R4 C01：対象・操作・証拠の識別に関する技術参照（2026-10-16）

状態：**参照資料。全項目の実装採用ではない。** 現行方針は[R4目的・境界レビュー](phase4-s1-r4-failed-resolution-purpose-and-boundary-review-20261016.md)、今回の作成/検証条件は[承認済み接続実証開始票](phase4-s1-r4-consume-recovery-connection-limited-start-review-20261016.md)を参照する。

## 1. この資料の位置づけ

初期レビューで、対象・操作・主体・証拠の対応をC01として洗い出した。Ownerは、その全項目を運用管理することを求めず、最低限の区分と再投入抑止に絞る方針を示した。その後、追加受付table案も保留とし、既存permit/consumeと単発復旧の接続実証を条件付き承認した。

以下は必要な比較項目を選ぶための技術参照であり、C02以降の全件詳細化、新table、全世代の台帳、運用者による多数のID/hashの手入力を要求しない。今回の新規3 file・七ケースを増やす根拠にも使わない。

## 2. 現行sourceで区別すべき識別

読取codeは`af9fa01`。OwnershipはReference `notification`。

| 識別 | 現行の意味と注意点 |
| --- | --- |
| [RecoveryTarget](../../koiki-reference-app/src/main/java/org/koikifw/reference/notification/application/query/RecoveryTarget.java) | environment/publication/event/listener/expectedAttemptの5項目。今回のselectionと一致を確認する。statusやprovider受理をこのtupleだけから推定しない |
| permit/operation/worker | permitは許可、operationは今回の操作、workerは実行世代の識別。worker文字列やoperation IDだけで本人認証・実行済み・配送成功と認定しない |
| [ExpenseApprovedEvent](../../koiki-reference-app/src/main/java/org/koikifw/reference/expense/domain/event/ExpenseApprovedEvent.java)の通知key | 申請ID＋承認version＋受信者種別。publication/operationとは別。keyやcanonical payloadにeventIdは含まれず、同じpayload hashだけで同じevent/publicationとは認定しない |
| payload hash・宛先・受理ID | 通知内容・受信者・provider結果の照合材料。key一致だけで受理済みとはしない。秘密や宛先実値を汎用logに追加しない |
| [ConsumptionProof/ClosureProof](../../koiki-reference-app/src/main/java/org/koikifw/reference/notification/application/port/outbound/RecoveryEvidencePort.java) | 既存の対象/操作/確認者/参照を結ぶ値。参照文字列の存在だけで証拠の耐久性・真正性・使用中の有効性を保証しない |
| 発行時targetと復旧後結果 | 元snapshotは結果に合わせて書き換えない。復旧後のattempt変化と歴史closeの不足は、今回の呼出し実証とは分けて扱う |

## 3. 今回の接続で必要な対応

1. consumeに渡すtargetと、recoverのselectionの5項目を照合する。明らかな不一致は消費前に拒否する。
2. 同じpermit/operation/workerでconsumeし、**この呼出しの正常復帰後だけ**recoverへ進む。既存消費記録の照会は再実行許可ではない。
3. 結果Auditをoperationへ関連付け、既存消費→permit→対象をたどれるようにする。実行/結果不明は人の確認へ戻し、別IDで通常のやり直しをしない。

対象比較・認可はシステム側で行い、人は対象・前回結果・処置内容を確認する。人の判断誤りが残ることは限界として示し、すべてを防ぐ追加台帳へ広げない。

## 4. 後続で必要になった場合だけ扱うもの

証拠の一意保存/ACL/保持・復元、正式認証とworker委譲、全停止/再配置抑止・失効の継続、provider key寿命、別publication横断抑止、歴史閉鎖と自動再開は今回未解決の論点として残す。今回のtest principal・有限proof・模擬leaseを本番保証へ昇格しない。

これらをすべて先に実装する判断ではない。接続実証で不足が判明した場合に、事実・影響と必要最小限の追加候補を提示し、範囲/予算への影響をその時点で判断する。
