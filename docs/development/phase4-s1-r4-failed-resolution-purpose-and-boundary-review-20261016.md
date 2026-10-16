# S1 R4：FAILED処置と復旧操作接続の目的・境界レビュー（2026-10-16）

状態：**設計レビュー整理済み / 接続実証はOwner条件付き承認**。実行範囲・上限・承認は[接続実証の限定開始票](phase4-s1-r4-consume-recovery-connection-limited-start-review-20261016.md)を正本とする。文書source固定・fresh preflight待ちで、実装・検証は未開始。

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
| 既存機構による接続実証 | [開始票§7](phase4-s1-r4-consume-recovery-connection-limited-start-review-20261016.md#7-owner承認記録と網羅性の扱い2026-10-16)で条件付き承認。新規3 file・7件/直接回帰4件/root1回・60分。結果受入は別判断 |

現在は承認記録を含む10文書の内容整理・固定対象点検中。local commitは別承認またはOwner操作、その後に新実装branch・clean source・fresh preflightへ進む。code/test作成・検証・remote操作は未開始。

C01の識別項目は[技術参照](phase4-s1-r4-c01-operation-binding-review-20261016.md)として残す。今回の開始条件は承認済み開始票に一本化し、旧案の詳細項目や「次は〜」を実行条件として読み替えない。
