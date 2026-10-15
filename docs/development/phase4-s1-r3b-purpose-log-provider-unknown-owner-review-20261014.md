# Phase 4 S1 R3-B：通知log・provider不明結果の目的と開始前判断（2026-10-14）

状態：**CONTRACT REVIEW DRAFT / R3-B IMPLEMENTATION NOT STARTED**。Ownerは本レビュー案に沿って進める方針を確認した。§5以降の具体候補は追加点検による提案であり、個々の契約採用・実装開始を先取りしない。本書は[R3目的・A/B分割](phase4-s1-r3-purpose-recovery-model-and-minimal-verification-owner-review-20261013.md#4-最小実証を二段階で定義する案)と、[R3-A限定結果のOwner受入](../architecture/validation/phase4-s1-r3a-implementation-and-a01-stop-20261013.md#23-r3-a限定結果のowner受入承認2026-10-14)を入力とする。R3-A成果commit `c8d406b713fed97269ca5843ecf31b79824593ad`はOwner個別承認後に`origin/feature/phase4-s1-reference-full-stop-recovery-r3a`へpush済みで、remote参照hash一致を確認した。本書の作成はR3-Bのcode・migration・test作成、Maven／Docker実行、commit・push・PR・mergeを許可しない。

## 1. R3-Bの目的と既存境界

R3-Bは、R3-Aの全停止・単一対象復旧モデルを前提に、**provider副作用とReferenceの保存結果が食い違う／分からない場合の安全な判定**を具体化する。対象はE07の通知log保存後・publication完了前、E09のkey欠落／異内容／期限切れ、E10の受理後応答喪失／受理前切断／照会不能、E08の未実証な並行・別通知識別枝である。すべてを一括実装・PASSとするのではなく、枝ごとの契約・実証可否を先に固定する。

現行`ExpenseApprovalListener`→`NotificationDispatchService`はprovider `deliver`を呼ぶだけで、Reference-owned通知logの保存点はない。`HttpNotificationProviderAdapter`はnumeric loopbackの`/notifications`だけを受け、key・event ID・payload hashを送信し、HTTP 200／201とUUID応答を確認する。IOExceptionや応答異常は例外となるため、例外だけから**provider未受理**とは判断できない。Toolingの`LocalNotificationProviderStub`は到着記録と独立transactionの受理台帳を持つが、現行のReference outbound契約には照会・保持期間の意味がない。

R3-Aで受入済みなのは、別epochの選択10件と通常root303実行＋92 skipの限定結果である。A04dは既占有lock拒否、A04aは拒否した復旧子の独立probe送信0だけ。全instanceの実運用停止、通常listenerとの一般競合、実provider受理照会、通知log、UNKNOWNからの復旧は未証明として保持する。R3-Bでも同期のexpense承認と同transactionのAudit／publication保存、commit後の非同期通知、通常構成での無効を維持する。

## 2. 開始前に閉じる契約

| 判断点 | 決める内容 | 未確定時の安全側 |
|---|---|---|
| L1 通知logの所有と意味 | Reference `notification`所有とし、event／publication／key／payload hash／宛先・provider受理識別子／到達状態のどれを保存するか。logを送信要求・provider受理・照合済みのどの証拠と呼ぶかを分ける | log行の存在だけで受理済み／未送信を断定しない |
| L2 保存順序と失敗 | provider受理前後のlog transaction境界、受理後log書込失敗、log保存後publication完了前の停止位置。必要ならReference-owned migrationの一意制約と既存V5との関係を設計する | 受理後の記録失敗はUNKNOWN／HOLD。無条件の再公開をしない |
| P1 provider照会 | key・payload hash・宛先に対する照会結果の種類（受理・未受理・不明・期限外）、独立台帳の保証、応答遅延／認証の扱い。loopback stubでの意味だけを固定する | 照会不能・矛盾はUNKNOWN／HOLD |
| P2 key保持と再要求 | 同key同内容の重複受理防止期間と、再要求可能な時点・期限。Reference側の対象年齢・停止時間と矛盾しない条件を定める | key欠落・異内容・期限切れで送信しない。運用保持・purge全体はR5へ残す |
| H1 HOLDの記録 | 対象publication、元process世代、観測時刻、判定理由、次の照合先・担当を、個人情報や秘密を増やさずどこへ残すか。R4の正式operator認可／Auditとは区別する | HOLDを単なるtest assertionに閉じず、確認不能な理由をEvidenceに残す |
| C1 E08残枝 | R3-Aの独立2通知C04と、同一論理通知の並行要求・異なる論理通知の識別を分ける。全停止型のままReferenceで実証できる範囲と、通常listener並走を要する枝を区別する | R3-A／stubの既存重複拒否を広い並行保証へ読み替えない |

E07のために通知logを導入する場合、現行の送信Portが`void deliver`である点も設計判断に含む。provider受理結果・不明結果をApplicationへどう返すか、HTTP例外に付随する受理不明をどう表現するかを決める前に実装形を固定しない。Framework Public APIやCustomer通知へ昇格させない。

## 3. 最小検証の候補と停止条件

| 枝 | 検証候補 | 成立時に言えること／STOP |
|---|---|---|
| B01 / E07 | loopback stubで受理を独立確定し、Reference通知log保存後・publication完了前に元processを停止。全停止・対象再照合から同じkey／payloadで終端を確認 | provider受理総数1、logとpublicationの整合、別対象不変更。logだけから結果を推定した場合はSTOP |
| B02 / E09 | key欠落、同key異内容、key期限外を独立fixtureで作る | 各枝で復旧側送信0・HOLD理由を照合。危険な再要求や受理0の推定があればSTOP |
| B03 / E10 | 受理後応答喪失、受理前切断、照会不能を別fixtureで作り、provider台帳とReference観測を分離する | 照合可能な枝だけ同key安全再要求または完了、受理総数1。確認不能は送信0でHOLD。例外だけで未受理としたらSTOP |
| B04 / E08残枝 | 同一通知の再要求・並行要求と、別通知のkey／payload／宛先識別を分けて方法をreview | R3-A C04の独立2通知を再利用PASSとして数えない。全停止モデルで安全に実証できない並行枝は未達として残す |
| B05 / 保護回帰 | 通常構成の通知・復旧無効、同期承認／Audit／publication、既存R1〜R3-A契約への直接影響を点検 | 変更sourceの影響に応じて選択回帰と通常rootの要否を開始票で確定。過去のPASSを新sourceのPASSへ流用しない |

これはcase候補であり、invocation数・故障位置・期待状態、production／Tooling変更file、JAR対照、時間・JVM／DB／Ryuk／client・raw上限、再実行回数、cleanupを承認済み実行値としない。L1〜C1を閉じた後に、各枝の必要性と実証可能性から最小集合を再選定する。Reference-owned log／schema／provider契約を変えるなら、別source／artifact epochを固定し、通常構成保護と必要な直接回帰を新たに確認する。

## 4. Owner判断の入口と統合への接続

最初の判断は、①L1〜L2の通知log意味・保存境界、②P1〜P2のloopback照会・同key保持契約、③H1のHOLD記録と担当、④C1のE08残枝をR3-Bでどこまで扱うか、を決めること。その後、対象path・migration／依存要否・具体case・有限予算・preflight・STOP／cleanupを書いた**別の最小開始票**を作り、Owner判断を経てR3-Bを実装・検証する。今は作成・検証を始めない。

R3-Bの結果をR3断面の統合判断に入力する際は、R3-AとBの受入範囲、UNKNOWN／HOLDの未達、実provider適用条件、通常構成保護、Git／artifact／証拠の各epochを分けて示す。[main統合点検案](phase4-s1-main-integration-first-pr-scope-review-20261014.md)の最初の文書PR候補はR3-Bを待たずに審査できる。PR作成・mergeは各々のOwner個別判断とする。

## 5. 現行sourceの静的差分点検（2026-10-14）

`ExpenseApprovedEvent`は申請ID・承認version・受信者種別から安定keyを作り、canonical payloadとSHA-256を作る。一方、現行の`NotificationProviderPort.deliver`は`void`で、受理ID・確認不能・key期限の戻り値はない。`HttpNotificationProviderAdapter`はnumeric loopbackのPOSTのみで、HTTP 200／201とUUID文字列以外を例外へまとめ、受理照会APIはない。したがって、IOException・timeout・応答異常を**未受理確定**へ変換して再要求する契約は現時点で存在しない。

`LocalNotificationProviderStub`の受理台帳は`notification_key`をPRIMARY KEYとし、同keyのpayload hashが違えば409を返し、同key同hashには元の受理IDを返す。到着記録と受理台帳は別に保存される。ただし現在の比較対象はkey・payload hashが中心で、宛先の独立した一致列、照会用HTTP endpoint、key有効期限、保持後の結果意味はない。HTTP serverのexecutorもsingle-threadであり、この実装のままで並行要求の衝突を実証したとは言えない。これらはR3-Bの候補差分であり、現行受入済みR3-Aの欠陥と断定しない。

## 6. L1〜C1の具体契約候補（Owner判断入力）

| 判断 | 第一候補 | 採用前に残る確認 |
|---|---|---|
| L1／L2 | Reference `notification`に、受理ID・key・event／publication相関・payload hash・宛先識別・保存時刻を持つ**受理観測log**を置く。providerから確定した受理応答を得た後に、publication完了とは独立して耐久保存する。`REQUIRES_NEW`等の具体transaction方式はSpring Modulithの完了記録順序を確認して決める | logはReferenceが観測・保存した事実で、providerの独立台帳そのものではない。log保存失敗時は受理不明／HOLDを維持する。logの一意性と追加migrationの必要性を別review |
| P1 | loopback stubに**読取専用照会**を設け、同keyの受理IDと、payload hash・宛先識別との一致を返す。結果は`ACCEPTED_MATCH`、`CONFLICT`、`NOT_FOUND_WITHIN_VALID_WINDOW`、`EXPIRED`、`UNKNOWN`を区別する | `NOT_FOUND`を安全な未受理確定と呼べる遅延・保持保証、照会の認証／独立性を固定する。実providerへの転用は禁止 |
| P2 | stub-ownedの検証用時刻・有効期限を明示し、**同key・同内容・有効期間内**だけ元受理IDの再利用を認める。期限外や期限証拠がない場合は送信せずHOLD | 実運用の保持／purge値はR5で別判断。R3-Bでは具体TTLの数字を先取りせず、caseごとの時刻・期限証拠を固定する |
| H1 | 復旧判定から`COMPLETED`／`HOLD`を理由code・対象ID・key hash・観測時刻とともに構造化して返し、Tooling-owned独立Evidenceへ保存する。復旧子logだけに依存しない | 正式operator Audit・認可・運用台帳はR4の未達。Reference側に恒久HOLD記録が必要かは、運用導線・migration負担を比較してOwnerが決める |
| C1 | E08を、別通知の独立受理、同通知の逐次再要求、provider stubへの並行同key要求、Reference通常listenerと復旧processの並走に分割する。R3-B初回は全停止型と矛盾しない前3者の採否を判断する | 現行single-thread stubで並行成立を主張しない。最後の通常listener並走は全停止型の外で、fencing設計なしにR3-B PASSへ加えない |

候補の順序は、①照会結果と期限の意味を固定、②受理logが示す事実と保存transactionを決める、③HOLD記録、④E08各枝の採否、⑤その決定に合わせてReference／Tooling差分と最小caseを確定、である。Reference側の送信Portを変更するなら、受理ID付き成功・明示拒否・受理不明を区別し、旧`void`例外から安全性を推測しない。照会の`NOT_FOUND`も無条件の再送許可ではなく、定めた有効期間・応答完結性・全停止証拠を満たした場合だけ次の判断材料とする。

静的な変更候補は、Reference `notification`の`NotificationProviderPort`／`NotificationDispatchService`／`HttpNotificationProviderAdapter`、`SinglePublicationRecoveryService`／隔離起動Configuration、通知log保存用の新規Port／AdapterとReference-owned追加migration、Toolingの`LocalNotificationProviderStub`／R3 fixture／選択testである。現在のV5は`db/migration/kkref-notification-async/`にあり、追加migrationを採るなら通常構成のFlyway locationを変えず、async opt-in側だけで適用する候補とする。これらは**影響調査の候補path**で、全ファイル変更の承認やV6採用の決定ではない。Framework、既存V4／V5の書換え、通常properties、Security、CIは初回候補に含めない。

## 7. 次のOwner判断点

§6の第一候補の採否、特に**受理観測logをReferenceに追加するか／どの事実を記録するか**、provider照会の`NOT_FOUND`をどこまで信用するか、HOLDをTooling Evidenceだけに留めるか、E08の並行枝をR3-B初回に含めるかを先に決める。その判断を入力に、source・artifact・case数・migration／Port変更・回帰・有限資源・停止／cleanupを載せた最小開始票を作る。本書だけではR3-B実装・検証を開始しない。

## 8. 第一候補の設計基線採用（2026-10-14）

Ownerは§6の第一候補について「第一候補を設計基線にする で進めます」と判断した。これにより、Referenceの受理観測log、loopback照会とkey期限、HOLDの独立Evidence、E08の枝分けを**最小開始票を具体化する基線**とする。個々のschema／transaction／provider応答やcase実行値の採用、R3-B実装・検証開始、文書commit／branch／remote操作を承認した意味ではない。以下の§9はこの基線からの具体化案であり、未確定条件を隠さず次の判断へ渡す。

## 9. 最小開始票へ渡す契約と対象候補

### 9.1 受理観測logと保存境界

受理観測logは、**Referenceがproviderから受理ID付き成功応答を観測し、その事実を耐久保存できたこと**だけを表す。provider独立台帳の真値やpublication完了を代替しない。現行listenerは`ExpenseApprovedEvent`だけを受けてpublication IDを直接受け取らないため、書込時点でpublication IDを無理に生成・推測しない。まずevent ID、通知key、payload hash、論理宛先、受理ID、観測時刻を最小候補とし、対象publicationとの対応は復旧時の既存選別snapshot（publication ID・listener ID・event ID）で照合する。複数publicationが同じeventに関係する条件とlogの一意性はmigration設計前に確認する。

E07の停止位置を成立させるには、provider成功応答後のlogが**publication完了記録より先に独立commit**される必要がある。現行`@ApplicationModuleListener`とSpring Modulithのpublication完了順をsource／依存実装で確認し、同一transaction内に埋もれて元process停止でlogもrollbackする方式は採らない。具体的なtransaction実装・失敗時伝播・既存listenerの再試行への影響は開始票で固定する。受理済みでも応答喪失ならlogは作成できない場合があるので、log欠落を未受理証明にしない。

### 9.2 provider照会・key期限と`NOT_FOUND`

照会結果の`ACCEPTED_MATCH`は同じkey・payload hash・論理宛先に対する独立受理IDを確認した状態、`CONFLICT`は同keyで内容不一致、`EXPIRED`は冪等保証期間外、`UNKNOWN`は通信失敗・矛盾・照会不能とする。`NOT_FOUND_WITHIN_VALID_WINDOW`は**照会時点の台帳に受理行がない観測**に過ぎず、並行または遅延中の要求が後で受理されない証明ではない。これ単独を未送信・再送許可にしない。

同key再要求の安全性は、provider側が**同key同内容を有効期間内に論理受理1件へ収束させる契約**と、R3-Aの全停止・対象再照合・期限証拠の組合せから判定する。`NOT_FOUND`を得た枝でも、この条件がそろわなければHOLDとする。受理済み応答喪失時は照会した受理IDと内容が一致すれば、追加POSTを避けてpublicationを安全に完了させる方法を第一候補とし、Modulith listener経由での完了可否を実装前に確認する。照会が不能ならHOLDを維持する。stubのreceiptを単純削除して期限切れを模擬すると重複受理の危険を隠すため、期限後も過去受理の観測証拠を別保持し、照会の`EXPIRED`と重複抑止の失効を区別する。

### 9.3 HOLDとE08の境界

R3-Bの限定実演では、HOLD理由code・対象ID・key hash・観測時刻・確認先を復旧子の構造化結果からToolingの独立Evidenceへ保存する。これは実験での第三者照合用であり、実運用のoperator記録・Audit／permitの代替ではない。復旧子が結果を返せず異常終了した場合も、Tooling側がprovider・publicationの確定／不明を独立観測し、安易に送信可能へ変えない。Reference本番の恒久HOLD記録がR3の受入条件として必要かは、R4境界と合わせて開始票前にOwnerへ示す。

E08は、R3-A C04の別通知成立を保持し、同一通知の逐次再要求とstub側の同key並行要求を**別の候補**にする。現行stubはsingle-threadなので並行枝には明示的な競合位置と独立受理台帳の確認が必要。Reference通常listenerと復旧processの並走は全停止型に反し、fencingなしにR3-BのPASSへ含めない。

### 9.4 最初の限定検証集合と固定前提

最小候補はE07のlog保存後停止1件、E09のkey欠落・異内容・期限外3件、E10の受理後応答喪失・受理前切断・照会不能3件の**新規7枝**。各枝は異なる故障位置・provider台帳・Reference log・publication／対象外行・HOLD理由を照合する。E08の逐次／stub並行枝は競合契約の成立可能性を先に確認し、7枝へ黙って加算しない。変更するPort／stub／migrationに直接影響する既存caseと、通常構成の保護・root1回の要否を別に選ぶ。過去R3-Aの10件・通常rootを新sourceのPASSへ再計上しない。

開始票では、実装対象をReference `notification`とTooling `reference-e2e-verification`に限定し、既存V4／V5・通常properties・Framework／Customer・Security／CI不変更を候補とする。追加migrationのpath、Port戻り値と照会API、選択caseのmethod名／正確なinvocation、通常／async artifact、隔離DB・stub・子JVMの所有、時間・heap／JVM／PG／Ryuk／client・raw上限、再実行0回・cleanup予約、BLOCKED条件を数値で固定する。**この節は開始票の入力であり、予算や実行回数の承認ではない。** 文書source固定とclean source・preflight成立前にcode／test／SQL作成や実行へ進まない。
