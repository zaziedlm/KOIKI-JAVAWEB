# S1 R4：FAILED処置と復旧操作接続の目的・境界レビュー（2026-10-16）

状態：**接続実証 COMPLETE / OWNER ACCEPTED、Aの設計候補留保・人手HOLD維持はOWNER APPROVED（§10）**。実行範囲・上限・承認は[接続実証の限定開始票](phase4-s1-r4-consume-recovery-connection-limited-start-review-20261016.md)、限定結果は[実証記録](../architecture/validation/phase4-s1-r4-consume-recovery-connection-20261016.md)を参照する。

## 1. 現行方針と採用判断

一件の復旧を「確認→許可→実行→結果確認」として既存機構へ接続する。非同期リカバリーを故障の組合せごとの状態管理へ広げず、最低限の再投入抑止、人の確認・保留、結果の追跡を優先する。人為ミスの完全排除や全故障からの自動再開は初回の目標にしない。

- **追加受付table案はOwner判断で保留。** 既存permit/消費/Auditと重複する受付・状態管理を増やす優位性が確認できていない。[保留した開始票](phase4-s1-r4-intake-read-limited-start-owner-review-20261016.md)は判断履歴として残し、その変更対象・15件/24件・90分を実行根拠にしない。
- 運用上の「要確認／処置中／処置済」は説明上の区分として使う。新tableやDB enumとして永続管理する採用判断ではない。
- Ownerは、追加tableなしの接続実証（新規3 file・7ケース・直接回帰4件・通常root1回・60分）を条件付き承認した。七ケースは網羅確定ではなく、不足と影響を都度提示して追加・見直しを判断する。
- B03aの人手HOLD先行は受入済みだが、FAILED publication完了とDoD4-2/4-3は未達。今回の接続実証で解決済みとはしない。

作業branchは`docs/phase4-continuation-task-inventory-20261016`、読取codeは`af9fa01c56df372559db99e23f495323114f8f93`。OwnershipはReference `notification`、Tier2 RICH/JPAを維持する。Framework/Customerの契約へ昇格しない。

## 2. 一件の復旧を既存機構へ対応付ける

対象は元Referenceを停止し、単一publicationを人が確認して単発復旧する限定モデル。本番運用可能という認定ではない。

| 手順 | 人が行うこと | 再利用する機構 | 不足する接続 |
| --- | --- | --- | --- |
| 確認 | 対象、元process停止、provider受理状況を照合。不明なら担当付きHOLD | publication snapshot、R3停止証拠・独立observer、provider照会、受理log | 確認した対象/停止/provider材料を既存target/proofへ渡す。fixtureの`authorized_target`を本人認証・正式許可としない |
| 許可 | 発行者本人が理由・対象と現在権限を確認して一回の操作を許可 | permitのissue/read/consume、Identity/能力/scope/TTL、permit lock・一回消費、同transactionのAudit | 肯定可能なproof供給と、確認から実行までの停止維持。既定empty proofの成功fallbackは作らない |
| 実行 | 停止を維持し一回だけ実行。異常時は自動再投入しない | 単発復旧Service、snapshot照合、advisory lock、provider再照会、Modulith再公開/完了待ち | 新規consumeのcommit確定後だけ同じ対象へ進む呼出し。現行runnerはpermitと未接続 |
| 結果確認 | provider・受理log・publication・今回Auditを照合し、完了/未解決を記録 | permit readのoperation/消費時刻、受理log、publication、既存Audit | operationと結果Audit/参照を結ぶ。attempt更新後のcloseを無理に通さない |

根拠：[permit Service](../../koiki-reference-app/src/main/java/org/koikifw/reference/notification/application/RecoveryPermitService.java)、[単発復旧Service](../../koiki-reference-app/src/main/java/org/koikifw/reference/notification/application/SinglePublicationRecoveryService.java)、[通知処理](../../koiki-reference-app/src/main/java/org/koikifw/reference/notification/application/NotificationDispatchService.java)、[段階B契約](phase4-s1-stage-b-operational-integration-contract-review-draft-20261008.md)。

## 3. 一回だけ呼ぶ接続

既存Serviceの外側に呼出し順を調整するReference Application処理を一つ置く。今回承認された実証ではBean/runner登録をせず、testから明示生成する。

```text
transactionのない呼出し境界
  → targetとselectionを照合
  → 既存consume［消費＋Auditの所有transactionが終了］
      例外・既消費・結果不明なら停止
  → このconsumeの正常復帰時だけ既存recoverを一回
  → 結果Auditを別の短いtransactionで保存
  → 人がprovider・log・publication・Auditを照合
```

入力照合はenvironment/publication/event/listener/expectedAttemptの対応を確認し、復旧側が受け付けないstatus等はconsume前に拒否する。現行consumeは`TransactionTemplate.execute`終了後に復帰するため、この呼出しの正常復帰をcommit確定の判断に使う。外側を`@Transactional`で囲まない。

事後readで消費を見つけたことを、再実行の許可に置き換えない。既存recoverが内部でleaseを取得するため、外側から同じadvisory lockを別接続で先取りする構成も採らない。consume後にrecoverの入場が失敗する窓は残し、人の確認で扱う。

| 場面 | 処置 |
| --- | --- |
| 同じpermitの同時/再要求 | permit lockと一回消費を利用。既消費例外を握りつぶしてrecoverへ進まない |
| consume例外・commit結果不明 | recoverなし。read/Audit照会は調査用とし、消費確認後の自動再開を作らない |
| 消費確定後・recover前に停止 | 消費済み・未実行の可能性を残してHOLD。新processが記録を見て自動実行しない |
| recoverの拒否・HOLD・timeout・異常終了 | retryなし。未送信と断定せずprocessと副作用を確認する |
| recover完了後の結果Audit失敗 | 復旧を繰り返さず、結果記録の回復/調査へ。完了材料と記録未了を分ける |

未閉鎖permitは保持し、新permit/operationやcloseで通常のやり直し経路を空けない。この限定運用は実行exactly-onceや、管理者の手順逸脱の完全防止を保証しない。

### 結果を既存Auditへ結ぶ

結果Auditのresource参照をoperation IDとする候補。既存消費記録のoperationはuniqueなので、結果Audit→operation→消費→permit→発行時targetをたどれる。操作者と固定分類の完了/保留理由を残し、新column・payload本文は追加しない。

consume前の拒否Auditは既存契約を維持し、結果Auditを消費成功に見せない。結果Auditはrecover後の短い所有transactionで保存し、provider通信・完了待ちをtransactionに含めない。本番の結果参照の保存先/保持は今回の実証で成立済みとしない。

## 4. 最小実証と適用限界

詳細なmethod・回帰・資源・時間・停止条件は[承認済み開始票§2〜5](phase4-s1-r4-consume-recovery-connection-limited-start-review-20261016.md)へ集約する。

| ID | 確認する代表ケース |
| --- | --- |
| Q01 | 独立DB接続でconsume/Auditのcommitを確認してrecover一回。その後の同じ要求では増えない |
| Q02 | publicationが不一致ならconsume前に拒否し、消費/復旧なし |
| Q03 | consume Audit保存失敗でrollbackし、recoverなし |
| Q04 | 同じpermitへの同時二要求でも消費/復旧は一回 |
| Q05 | 実consume commit後、呼出側へ例外を返すとrecoverなし。再contextで照会・再要求しても実行しない |
| Q06 | recoverのHOLD後、catch内や再要求から呼び直さない |
| Q07 | recover後の結果Audit失敗から復旧をやり直さない |

実物は接続処理、既存permit/Identity/Audit/JPA/PostgreSQL、既存recover。recover配下のlease/provider/Modulith再公開は記録可能なtest用代替を使う。statusはPUBLISHED一つを代表とする。log順だけでなく独立接続のDB記録と入口/再公開要求回数を照合する。

模擬leaseの正常復帰は実publication完了の証拠ではない。Q05は呼出側の正常復帰喪失の模擬で、OS kill・実commit通信断・生存する遅延processの検証ではない。七ケースは有限の初回集合であり、網羅性や全人為ミスの防止は認定しない。

## 5. 今回残す課題

| 課題 | 今回の境界 |
| --- | --- |
| 正式認証・確認材料/proof・使用中保護 | test principalと有限proofで接続を確認する。正式non-Web入口、全instance停止真正性、fencing等は未接続/未実証 |
| 既存runnerの直接起動 | 今回は変更しない。将来の運用入口はconsumeを通る必要があるが、この実証だけで入口統合済みとしない |
| FAILED整合 | 現selectionはPUBLISHED/PROCESSINGのみ。FAILED完遂は別差分。受理済み整合を採るなら後段でもPOST禁止が必要で、照会がNOT_FOUNDに変わった際の再送へ自動切替しない |
| 歴史close | 現行closeは現在target一致を要求し、復旧でattemptが進むと拒否され得る。結果確認と閉鎖を分け、まず未閉鎖を保持する限定出口で区切る |
| 別publication・保持/追跡・正式運用 | E08残枝、D11/D12、R5、実provider、本番証拠の保持は後続。今回の一回消費・lockだけで解決済みとしない |

停止後は「要確認」、実行中は「処置中」、結果確認後は「処置済」と説明する。timeoutや要確認に戻ったことだけでは再投入できず、未解決HOLDを処置済にしない。この説明のための新しい永続状態は追加しない。

## 6. 判断記録と現在の次作業

| 判断 | 現在の扱い |
| --- | --- |
| 運用を場合の数へ発散させない | Owner方針。最低限の区分・不用意な再投入抑止・人の確認を優先。C01〜08の全詳細化や全組合せ検証を前提にしない |
| 追加受付table | Owner判断で保留。検討済みの方式/予算は[旧開始票](phase4-s1-r4-intake-read-limited-start-owner-review-20261016.md)に隔離して参照する |
| 既存機構による接続実証 | [開始票§7](phase4-s1-r4-consume-recovery-connection-limited-start-review-20261016.md#7-owner承認記録と網羅性の扱い2026-10-16)で条件付き承認。新規3 file・7件/直接回帰4件/root1回・60分。限定結果は実証記録§5でOwner受入済み |

承認文書はOwnerが`c749e44`へcommitし、新実装branch・clean source・fresh preflightを経て新規3 fileを作成した。新規7件/直接回帰4件PASS、通常root305実行PASS＋99 skip、cleanup成立。限定結果のOwner受入とQ04記述訂正を含む成果はOwnerが`652e504b8a4161b5b221e8d35bb14f863c5ef944`へcommitした。次は§7のFAILED処置の判断材料整理。remote・追加実行・後続実装は別判断とする。

C01の識別項目は[技術参照](phase4-s1-r4-c01-operation-binding-review-20261016.md)として残す。今回の開始条件は承認済み開始票に一本化し、旧案の詳細項目や「次は〜」を実行条件として読み替えない。

## 7. 次の一件：FAILEDを人の照合後にどう解決するか（判断材料案）

状態：**DESIGN REVIEW INPUT / 方式採用・実装開始未判断**。接続実証成果`652e504`のclean状態を確認してから文書整理へ進んだ。今回の作業はsource/既存証拠の読取と本節の整理のみ。旧FAILED自動復旧A案を再開せず、旧予算や実行枠も使わない。

### 対象と目的

B03aの「providerは受理済み、Reference受理観測logなし、publicationはFAILED」という一件を対象にする。受入済みの人手HOLDを起点に、**人が受理を照合した後の解決条件と、再投入を止め続ける条件**を決める。新たな受付table・永続状態・全障害の組合せを増やさない。

現行selectionはFAILEDを拒否し、既存recoverはIncompleteEventPublicationsだけを使う。consume→recoverの接続実証はこの不足を解消していない。また、既存recoverはprovider照会が有効期間内NOT_FOUNDなら再公開を許すため、これをそのままFAILED受理済み整合へ流用すると「追加送信なし」の境界にならない。

### 最小限の区分と停止条件

| 人の照合で得たこと | 扱いの候補 | 再投入・解決を止める条件 |
| --- | --- | --- |
| 同じ通知のprovider受理を一意に確認できた | **受理済み整合の候補**。元process停止・対象固定・既存許可の条件を満たした場合だけ、追加POSTなしでReference記録とpublication完了を整合させる方式を次に比較する | 後段の再照会がNOT_FOUND/UNKNOWN/不一致へ変わっても送信へ切り替えずHOLD。人の確認だけでpublication完了済みと扱わない |
| 受理の有無・対応が一意に決まらない | 担当付きHOLDを継続し、provider照会/独立台帳/Reference記録の確認先と理由を残す | FAILEDやlog不存在だけから未送信と推定しない。新permit・新operation・closeで再実行の入口を空けない |
| 照合時と実行時で対象・停止・許可条件が変わった | 操作を止め、人による再確認へ戻す | 古い確認材料や消費済み記録から自動再開しない。別publicationへ対象を置き換えない |

これは人の判断を支える説明上の区分であり、新しいDB状態を採用する表ではない。「受理済み整合の候補」は実行許可や完了を意味しない。

### 解決と呼べる出口の候補

同一通知のprovider受理証拠、Reference受理記録、対象publicationの完了、今回operation/結果Auditを照合でき、処置による追加POSTがないことを確認できた時だけ、当該一件の解決候補とする。歴史closeは別課題のまま、未閉鎖permitを保持して結果確認と区別する。記録だけ整った場合やpublication完了が未確認の場合は未解決HOLDとして残す。

### 次に判断する一点

**B03aの次の検討範囲を「provider受理済みの一件を、追加POSTなしで整合する方式」に絞るか。** 絞る場合に限り、既存機構で可能な操作、後段でも送信を禁止する境界、publication完了の確認方法と不足を比較する。人の照合から送信再開する方式、正式operator入口、FAILED全般、自動再開、歴史closeは同時に進めない。

本節では方式採用・変更path・実行上限を確定せず、code/test変更、DB操作、Maven/Docker検証、commit/remote操作は行っていない。

## 8. 受理済み一件・追加POSTなしの方式比較（2026-10-16）

Ownerは§7の範囲に絞って方式比較へ進むことを指示した。これは比較の実施指示であり、以下の方式採用や実装/検証の開始承認ではない。R3-Bで採った人手HOLDを現在の安全な出口として維持する。

### 8.1 比較の前提とsourceで確認した事実

- 人が同一通知の受理を一意に照合した一件だけを対象とする。対象publication/event/listener/attempt、通知key/payload/宛先と受理IDを対応付け、元process停止と対象固定を維持する。人の照合は既存consumeの認可/proofを省略する根拠にはしない。
- `NotificationDispatchService`のrecovery動作は、ACCEPTED_MATCH＋受理IDなら受理記録だけを保存するが、有効期間内NOT_FOUNDなら`provider.deliver`へ進む。開始時の照会一致だけでは後段のPOST禁止を保証できない。
- `JdbcNotificationReceiptLogAdapter`は受理記録を独立transactionで保存し、既存行とevent/宛先/payload/受理IDが違えば拒否する。記録保存とpublication完了は一つのtransactionではないため、記録だけ保存された中間状態を許容しHOLDで扱う必要がある。
- 採用済みModulith 2.1.1のcached JARを`javap`で読取確認した。`FailedEventPublications.resubmit(ResubmissionOptions)`と、coreの`EventPublicationRepository.markCompleted(UUID, Instant)`等は存在する。APIの存在は当該FAILED行の安全な選択・更新・完了を実証したものではない。候補取得の先行limitに関する制限は[既存比較§1〜2](phase4-s1-r3b-failed-publication-limited-correction-owner-review-20261014.md)を引き継ぐ。

### 8.2 三方式の比較

| 観点 | A：送信禁止を設けてFAILEDを限定再公開 | B：受理記録を整合し、listenerを通さず完了確定 | C：受理記録の整合まででHOLD継続 |
| --- | --- | --- | --- |
| 処理 | 当該一件をFAILED専用APIで再公開し、listenerは確認済み受理を照合・記録するだけにする。Modulithのlistener完了経路で終端を確認 | 確認済み受理をReferenceへ記録し、指定publicationを完了APIで直接終端へ進める | 必要な受理材料/記録を残し、publicationはFAILEDのまま担当付きHOLD |
| 追加POSTを止める方法 | 当該隔離処置で使うprovider接続に**送信不能の境界**を設ける。deliver到達時もHTTPを呼ばず拒否。再照会がNOT_FOUND/UNKNOWN/不一致ならHOLDし、確認済み受理IDのすり替えも拒否 | 再公開/listener/送信処理を呼ばない。完了確定経路にprovider送信能力を持たせない | 再公開・送信を行わない |
| 既存機構の利用 | 既存listener、受理記録Adapter、Modulithの完了監視を利用できる。ただしFAILED選択・送信禁止・受理ID固定・consume接続の不足がある | 既存受理記録Adapterは利用できるが、core SPIへの依存と手動完了の業務上の正当性が新たに必要 | 人手HOLDの既受入方針を維持できる。記録保存を実装するなら、その操作も別開始判断 |
| 主なリスク | FAILED候補選別、非同期threadにも禁止境界が届く構成、記録commit後の完了失敗。現在のrecovery=trueだけでは不足 | listener処理を省略して完了扱いにする責任、対象/状態変化との競合、completion modeによる更新/保存挙動。APIの存在だけでは妥当といえない | 再送を避けられるがpublication完了には到達せず、B03a/E10a完遂は未達のまま |
| 初回の位置づけ | **次の具体化の推奨候補**。既存listenerによる記録と標準完了経路を維持する | 今回は推奨しない。Aが成立しない事実が出た場合に、完了の意味・責任・SPI依存を先に再審査する | Aの条件不成立・失敗・観測不能時の出口。解決済みへ読み替えない |

どの方式もpublication tableの手書きUPDATE、FAILEDをPUBLISHEDへ偽装、既存FAILの期待値変更を採らない。BはAPI経由でも意味上は手動完了であり、単に短い実装だからという理由では選ばない。

### 8.3 Aを具体化する場合に先に閉じる不足

1. **送信禁止は当該処置全体の条件にする。** 外側の事前照会だけ、thread-localだけ、運用者の注意だけに依存しない。非同期listenerが実際に参照するprovider接続にも禁止を適用し、通常通知や既存の未受理復旧へ意図せず広げない。Bean構成・誤設定時の起動拒否・受理ID固定の方法は未確定。
2. **FAILEDを一件だけ選ぶ。** 現selection/lease/再公開API/完了監視のどこを変更するか確認する。旧A案の「他FAILED/RESUBMITTED不在」は候補制限であり、今回の採用条件として自動承継しない。成立しない対象を別publicationへ置換しない。
3. **既存consumeと接続する。** 受入済みの正常復帰後一回という順序を保つ。新しいFAILED操作を既存runnerのpermitなし入口へ足すだけでは、正式操作との接続不足が残る。今回は正式認証入口まで同時に作る前提を置かず、有限fixtureで扱う範囲を次の設計で明示する。

初回の判定材料は、受理一致で追加POST0かつ対象完了、後段照会が変化した場合の追加POST0/HOLD、受理記録だけcommitして完了未確認なら非再実行/HOLD、という代表的な出口に絞る。これは実行集合・件数の承認ではなく、変更範囲を定める前の必要観測候補である。

### 8.4 今回の比較結果と次の判断

**A「送信禁止を設け、確認済み受理をlistenerで記録してFAILEDを限定完了する」を具体化候補として推奨する。** 送信禁止・受理ID固定・単一対象・consume接続が小さな範囲で成立するかを先に設計し、膨らむ場合はCのHOLDへ戻す。Bへ自動で切り替えない。

次のOwner判断は、Aについて上記三つの不足をsourceへ対応付け、変更候補と最小実証案を作る設計へ進むか。現段階では方式の正式採用、code/test/設定変更、検証実行、実provider操作、旧B03a再実行、commit/remoteを承認済みとして扱わない。

## 9. Aの三点設計と、人の対応へ戻す境界（2026-10-16）

OwnerはAの三点を具体化する設計へ進むことを指示し、必要以上に複雑になる場合は人の対応に倒す方針を示した。本節はその設計入力であり、実装開始票ではない。

### 9.1 「追加POSTなし」の対象

現行`HttpNotificationProviderAdapter`は通知送信に`POST /notifications`、受理照会にbodyなしの`POST /notifications/receipt`を使う。以下では**追加の通知送信POSTを0にする**ことを目的とし、受理照会POSTは別件数で記録する。全HTTP POST禁止を意味するなら、この現行照会Adapterを使うAは成立しない。送信と照会を合算したPOST件数だけでは判定しない。

### 9.2 送信禁止：一件固定のprovider接続

Reference内の専用provider実装候補を一つ置き、確認済みのevent/通知key/payload/宛先/受理IDを生成時に固定する。可変modeや状態tableは追加しない。

- `inspect(event)`は固定対象との一致を検査し、既存HTTP Adapterの受理照会だけを呼ぶ。ACCEPTED_MATCHかつ固定した受理IDと一致する場合だけ結果を返す。それ以外・例外・対象違いはHOLDとし、NOT_FOUNDを既存dispatchへそのまま返さない。
- `deliver(event)`はHTTP Adapterへ委譲せず、必ず呼出し前に拒否する。誤って送信経路へ入っても通知POSTを発生させない第二の止めとする。
- 復旧Serviceと非同期listenerの双方へ**同じ固定provider接続**を明示注入する。thread-localは使わない。初回候補は専用test構成からの組立だけとし、通常Beanや既存runnerのproperty切替で運用可能にする変更を含めない。underlying HTTP clientのclose責任はtest構成が一つに持つ。
- 起動時に未処置publication全体を再公開する機能を無効にし、対象以外のeventがこの接続へ来たら拒否する。これは通常listenerとの並走や全instance停止を保証するものではなく、全停止・隔離fixtureを前提とする。

### 9.3 単一FAILED対象：選別不能なら止める

FAILEDをselectionで明示し、既存leaseの環境/ID/event/listener/status/attempt/key/hash一致・session lockを維持する。対象snapshotを取り直してから既存permitのtargetへ対応付ける。状態をPUBLISHEDへ偽装しない。

初回候補は、**対象以外のFAILEDと既存RESUBMITTEDがない隔離DB**に限定する。この制限を§8の比較候補から明示的な設計条件へ進めるが、本番の単一選択が解決したとはしない。lease内の読取確認と再公開直前の再確認、FAILED専用APIのID filter・batch1/max-in-flight1/min-age0を組み合わせる。条件不成立なら対象探しのloopや別対象への置換をせずHOLDする。

再公開後は同じpublicationのModulith完了を待ち、event/listenerと想定attempt増分を照合する。FAILED残存・再FAILED・想定外遷移・lock接続喪失・期限超過は未解決HOLD。旧FAILED状態と再公開後の状態遷移、attempt増分が実APIで成立することは未実証であり、実装後の実証条件とする。session lockが通常listenerや外部管理者を排除するとは扱わない。

### 9.4 consume接続：受入済みの順序を変えない

```text
人の一件照合 → fixture組立/設定点検 → permit発行・現在条件/proof確認
  → 既存PermittedSinglePublicationRecoveryService
      target/selection一致 → consume正常復帰
        → FAILED対応のSinglePublicationRecoveryServiceを一回
          → leaseで対象を再確認 → 固定providerで受理照会
          → FAILED専用再公開 → listenerも固定providerで受理照会/記録
          → 対象完了を観測 → 既存の結果Audit
```

`PermittedSinglePublicationRecoveryService`/`RecoveryPermitService`の認可・消費・結果Auditの契約は変えず、既存SinglePublicationRecoveryServiceのFAILED経路を接続先に加える候補とする。元process停止・対象/proofは初回test所有の有限材料に限定し、既定empty proofを肯定へ変えない。既存B2 target Adapterはprovider値`FIXTURE_NOT_ACCEPTED`を要求するため、受理済み対象への実運用供給として流用しない。

consume例外ならFAILED経路を呼ばない。consume後にleaseが拒否した場合や受理記録だけcommitした場合も、消費済みを根拠に再開せず人へ戻す。結果Audit失敗で復旧をやり直さない。現行runnerはpermit接続がないため本案の入口に使わない。

### 9.5 変更候補と複雑さの評価

| 候補 | 最小限の役割・境界 |
| --- | --- |
| 新Reference provider実装1件（名称/path未固定） | 一件の固定照合と送信不能。既存Portを利用し、新しいFramework API/Portは作らない |
| `IsolatedRecoverySelection` | FAILEDの明示受入。通常のPUBLISHED/PROCESSING経路を維持 |
| `SinglePublicationRecoveryService` | FAILED専用APIの供給と分岐。FAILED機能未供給時は拒否し、既存経路へfallbackしない。既存呼出しの互換条件を点検 |
| `JdbcIsolatedPublicationAdapter` | 他FAILED/RESUBMITTED不在の確認とFAILED完了監視。publication更新を手書きSQLへ移さない |
| test構成・fixture・Evidence（件数未確定） | 実permit/Identity/Auditと実Modulith listenerを同じ一件へ組み合わせ、専用providerを両経路へ注入。fixture組立の成立性/必要file数は未確定 |

production候補だけで既存3件＋新規1件、さらにtest構成・実DB/非同期実証が必要であり、前回の一回呼出し接続に小さなflagを足す作業ではない。一方、一般受付・新table・汎用状態機械・新しい認証/委譲機構を作る必要は、この限定fixtureには置かない。

**Owner承認方針（§10）：Aは上記の限定fixture設計候補までに留め、現段階の運用は人の照合・担当付きHOLDを維持する。** 正式運用へつなぐには認証入口・肯定proof供給・停止維持・起動構成が別に必要で、その整備をこの一件へ抱き合わせるのは大きい。現在のsource確認だけで「Aは小さく実証できる」とは確定しない。

次のいずれかが必要ならAの開始票作成を止め、人の対応へ戻す：本番proof/正式入口を同時に新設、複数FAILEDの選択管理、通常listenerとの並走対応、既存受入済みconsume契約の作り直し、直接完了API/手書きUPDATEへの切替、fixtureだけで収まらない新基盤。単に不足を別の新状態として増やさない。

人の対応とは、対象・受理証拠・未完了箇所・再投入禁止・担当・次の照会先を既存の運用記録へ残し、FAILEDと未閉鎖permitを維持すること。追加table、直接DB更新、運用者によるpermit迂回実行を認めるものではなく、E10a/DoD完遂は未達のままとする。

次の判断材料は、この限定fixtureに実装/実証費用をかける価値があるかという点。作成する場合も必要file・最小集合・新規資源/時間枠を別開始票で確定する。現段階で実装・Maven/Docker検証・追加再実行はしていない。

## 10. Aの設計候補留保・人手HOLD維持のOwner承認（2026-10-16）

Ownerは、現段階ではAを限定実証の設計候補に留め、正式運用については人の照合・担当付きHOLDを維持する判断を了解し、承認した。これを**現段階の対応方針のOWNER APPROVED**として記録する。

- Aの実装・実証・運用採用は開始しない。§9の変更候補は設計入力のまま保持し、開始票の作成や実行を自動で次作業にしない。
- 人の照合で対象・受理証拠・未完了箇所・再投入禁止・担当・次の照会先を既存運用記録へ残す方針を維持する。実際の運用体制・記録先の配備や運用操作が完了したという承認ではない。
- FAILEDと未閉鎖permitを維持し、HOLDを解決済みや配送完了に読み替えない。追加通知POST、直接DB更新、permit迂回、Bの直接完了への自動切替を行わない。
- 必要以上の複雑化を避ける方針を維持する。Aを再検討する場合は必要性・費用・不足を示し、新しい限定範囲と開始判断を経る。

既受入のconsume接続実証、追加受付tableの保留、E10a/DoD・FAILED完遂等の未達は変更しない。本承認はcode/test変更・追加検証・成果commit・remote・Phase4全体の開始承認を兼ねない。今回の反映は本レビューと棚卸しの文書のみ。
