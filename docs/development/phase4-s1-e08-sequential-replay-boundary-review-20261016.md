# S1 E08：同一論理通知の逐次再要求・既存保証の整理（2026-10-16）

状態：**SOURCE整理に基づく方針・制限はOWNER APPROVED / 運用へ提供する方向を承認（§6）**。S1-05の最初の一件として逐次再要求だけを整理する。直前の未閉鎖permit/別publication方針はOwnerが`493af09`へcommitし、clean sourceを確認した。追加テスト・実装・DB/provider操作は行わない。

## 1. 今回の目的と境界

同じ論理通知に対する二回目の要求を、既存key・provider契約・Reference受理記録がどう扱うかを分ける。目的は新しい再送機構を作ることではなく、既存保証と人手HOLDに残す不足を判別すること。

同key並行要求と通常listener/復旧processの並走は別の残枝。Aの実装、FAILED自動完了、permit迂回、新keyでのやり直しは今回へ含めない。既存D01のstub直接逐次契約と、Reference全経路の逐次再要求を同じPASSとして扱わない。

## 2. Sourceで確認した識別と処理順

- [ExpenseApprovedEvent](../../koiki-reference-app/src/main/java/org/koikifw/reference/expense/domain/event/ExpenseApprovedEvent.java)のkeyはexpenseRequestId・approvalVersion・通知先種別applicantから作り、event ID/publication ID/operation IDを含めない。payload hashは宛先user/金額等を含むcanonical payloadから作り、event IDを含めない。同じkeyでも内容が一致するとは限らない。承認versionが変わる場合は別keyとなり、同一通知と決めつけない。
- [通常dispatch](../../koiki-reference-app/src/main/java/org/koikifw/reference/notification/application/NotificationDispatchService.java)は`deliver`後に受理記録を保存する。Reference受理記録の事前読取でPOSTを省く実装ではない。recovery時には別途照会し、一致なら記録だけ、有効期間内NOT_FOUNDなら送信へ進む。
- [stub受信処理](../../build-support/reference-e2e-verification/src/test/java/org/koikifw/buildsupport/referencee2e/LocalNotificationProviderStub.java)はkeyの一意保存を使い、同key/hash/宛先で既存受理IDを返す。受信時の既存受理照合ではevent IDを比較しない。一方、受理照会はevent IDも比較する。key契約がある場合の内容/宛先/期限検査もある。このtest実装を実providerの契約とみなさない。
- [Reference受理記録Adapter](../../koiki-reference-app/src/main/java/org/koikifw/reference/notification/adapter/outbound/persistence/JdbcNotificationReceiptLogAdapter.java)はkeyでINSERT競合を抑えた後、event ID・宛先・payload hash・受理IDの全一致を要求する。不一致なら既存行を書き換えず例外となる。失敗がproviderへの送信後に発生し得る点を区別する。

## 3. 逐次再要求の最小区分

以下は静的な処理対応であり、新しい実動作結果や自動再要求の許可ではない。

| 二回目の要求 | 既存機構から読み取れること | 現時点の扱い |
| --- | --- | --- |
| 同key・同event・同payload/宛先 | stubは既存受理IDを返し、Reference既存行も一致する構成。ただし通常dispatchなら二回目の通知POST到着自体は防がない | provider受理数、通知POST到着数、Reference記録、publication完了を別に観測する必要がある。Reference逐次再要求の全経路PASSは未取得 |
| 同key・同payload/宛先、別event（別publication等） | stubの受信では同じ受理IDを返し得るが、既存Reference記録とのevent不一致や、既存受理に対する照会不一致となり得る | 一律に正常完了すると保証しない。論理通知とevent/publicationの対応を人が照合し、未解決ならHOLD。event比較を外して通さない |
| 同key、payload/宛先が異なる | keyは同じでも同じ内容とはいえず、stubやReference側は不一致を拒否する構成 | 既存受理/記録を上書きせずHOLD。内容を変えた再送や新keyでの回避をしない |
| 別key（別expense/承認version等） | 既存keyによる重複抑止の対象外。正当な別通知である可能性もある | 同じ通知と推測して一律抑止しない。誤った再生成/重複が疑われる一件だけを関連付け、人が確認する |

key保持期限切れや照会不能は、上記の一致を推定する材料ではない。人手HOLDの既承認方針に従い、通知送信・再公開・新permitによるやり直しを行わない。

## 4. 今回の結論（§6でOwner承認）

**既存の安定keyと不一致拒否を維持し、逐次再要求を許可する新機構は追加しない。** 同keyでprovider受理が一件に留まること、追加通知POSTがないこと、Reference記録とpublicationが整合することは別の保証である。特に別event/publicationの同論理通知は、既存記録のevent一致条件と衝突し得るため、人手HOLDの関連記録として残す。

これは新たな不具合を実行で再現した結果ではなく、現在sourceから判明した適用限界。自動修正・比較条件緩和・別eventの自動吸収は行わない。実案件でevent再生成を正規の再要求とする必要が出た時だけ、その要件と保証したい出口を別reviewする。

## 5. 残る実証と次の判断

Reference全経路の同一event逐次再要求、同key並行、通常listener/復旧並走は未実証のまま。今回の整理だけでE08/DoD4-3をPASSへ変更しない。[R3-Bの限定受入](../architecture/validation/phase4-s1-r3b-source-preflight-and-blocking-review-20261014.md#23-r3-b限定結果のowner受入2026-10-14)と既存FAIL/HOLDの履歴を維持する。

必要な時に最初の実証候補とするのは、同一event/同payloadをReference経路へ逐次二回渡した際の、対象別POST到着・provider受理ID/件数・Reference行・publicationの対応。ただし入口の作り方、変更path・件数・資源/時間枠は未確定であり、本書を実行開始票としない。HOLD方針を優先する現段階では追加実証の開始を自動的な次作業にしない。

## 6. 逐次再要求方針・運用提供方向のOwner承認（2026-10-16）

Ownerは、既存key・不一致拒否を維持し、新しい再要求機構を追加せず、対応不明なものを人手HOLDへ残す方針を理解し、運用へ提供する方向で承認した。これを**本書の方針と適用制限のOWNER APPROVED**として記録する。

運用へ伝える内容は、provider受理一件・追加通知POSTなし・Reference/publication正常完了を別の保証として扱うこと、同keyでも別event/内容不一致等を正常完了へ読み替えないこと、対応不明なら既存運用記録へ対象と未解決箇所・再投入禁止・担当/次確認を残すこととする。[人手HOLD引継ぎ案](phase4-s1-r4-human-hold-handoff-guide-20261016.md)と合わせて提供する資料の方針であり、運用者へ送付・引渡し・配備を実施したという記録ではない。

運用側の記録先・担当・保管/参照条件等は未確定のまま。E08の逐次全経路/並行/並走の実証、DoD4-3、別publication横断の技術的抑止は未達を維持する。新しい実装・追加検証・実運用操作・成果commit・remote操作や、他者への送信の承認を兼ねない。
