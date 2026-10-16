# S1 R4：consume→単発復旧の接続実証 限定開始票（2026-10-16）

状態：**OWNER APPROVED / 限定結果 COMPLETE / OWNER ACCEPTED（2026-10-16）**。承認記録は§7、source固定・実行記録は§8。§1〜7の未開始・次手順は開始判断時点の記録として保持する。

## 1. 今回の出口と除外

[現行R4レビュー](phase4-s1-r4-failed-resolution-purpose-and-boundary-review-20261016.md)に基づき、**実DBでのconsume commit後だけ既存recoverを一回呼び、再要求や結果不明から呼び直さない**接続を確認する。本票の方式・範囲は§7でOwner条件付き承認済み。文書source固定・clean source・fresh preflight成立後に限り、以下の作成/限定検証へ進む。

計画branch：`docs/phase4-continuation-task-inventory-20261016`。読取HEAD：`af9fa01c56df372559db99e23f495323114f8f93`。追加受付table案はOwner判断で保留のまま。旧受付案の12 file・15件/24件・90分は使わない。

OwnershipはReference `notification`のApplication接続と非配布test fixture。既存Tier2/JPA、permit/消費/Auditのtableとtransactionを使い、**新table・migration・POM/依存・Framework API/Rules/Security/CI変更なし**とする。

本票には、Bean/runnerへの登録、正式認証入口、実provider/実Modulithによる配信、FAILED整合、歴史close、自動再開、実process killを含めない。現行runnerのpermitなし直接起動は変更せず、今回の成果を正式運用入口の認可接続完了としない。

## 2. 変更path：code/test最大3 file、既存code変更なし

すべて`koiki-reference-app/`配下の新規fileとする。既存Service・Configuration・harness・SQLを変更しない。追加fileや既存code変更が必要なら、作成前に理由を示してSTOPする。

| path | 作るもの |
| --- | --- |
| `src/main/java/org/koikifw/reference/notification/application/PermittedSinglePublicationRecoveryService.java` | transaction外でtarget/selectionを照合→既存consume→正常復帰時のみ既存recover一回→別の短いtransactionで結果Business Audit。Spring stereotype/Bean登録なし、testから明示生成する。新しいPortや受付機能は作らない |
| `src/test/java/org/koikifw/reference/notification/PermittedSinglePublicationRecoveryTest.java` | Q01〜Q07を各1 method、計7 invocationで確認する専用test |
| `src/test/java/org/koikifw/referenceacceptance/notification/PermittedRecoveryConnectionTestSupport.java` | 既存DB harnessの公開入口を組み合わせ、模擬lease/provider/再公開、入口回数、同期、故障注入とfinally cleanupを実装 |

新main classは既存の`RecoveryPermitService`・`SinglePublicationRecoveryService`・`BusinessAuditRecorder`・transaction managerを受け取る単純な接続とする。結果Auditの操作者は、consumeが検査した同じ認証主体から得る。既存Auditのoperation参照を使い、秘密や業務payloadを追加しない。action/event名はReference内の固定分類に閉じ、Framework契約を増やさない。

標準listener/runnerへの登録がないため、通常起動や既存のasync/recovery構成からこの接続は呼ばれない。testでの明示生成だけを最初の実証入口にする。通常構成への影響確認は§3のrootで行い、本番導入済みとしない。

文書は**最大11 file**：本票、R4レビュー、棚卸し、C01参照資料、保留した旧開始票、開発index、端末検証記録、今回Evidence新規1件、および承認後のAGENTS/project-overview/business-feature Skillの現行入口。既存未commit文書を含む固定対象はcommit前に個別点検する。code/test3 fileと合わせて最大14 file。過去のFAIL/受入記録を上書きしない。

## 3. 実証集合と順序

### 新規7 invocation：各初回一回

| ID | method候補 | 限定して観測すること |
| --- | --- | --- |
| Q01 | `commitsConsumptionBeforeOneRecoveryAndRejectsReplay` | recoverのlease取得入口で独立接続から消費/consume Audit各1件を確認し、呼出元transaction不在を確認。入口/模擬再公開各1、結果Audit1。同じ要求の再提示で増えない |
| Q02 | `rejectsMismatchedTargetBeforeConsumption` | publicationだけ違える代表入力。消費/consume Audit/復旧入口すべて0 |
| Q03 | `doesNotRecoverWhenConsumptionAuditFails` | 専用DBのCONSUMER roleのAudit INSERTを一時REVOKE。consume rollback、復旧入口0。finally復元 |
| Q04 | `recoversOnlyOnceForTwoConcurrentConsumers` | 同じpermitへ独立二transactionを有限barrierで同期。消費/consume Audit/復旧入口/模擬再公開各1。各threadにtest principalを設定・解除 |
| Q05 | `doesNotRecoverAfterCommittedConsumptionReturnsFailure` | test専用Service差替えで、実consume正常終了後に例外。消費/consume Audit各1だが復旧入口0。新contextで照会後に同じ要求をしても入口0。OS kill/実commit通信断とは呼ばない |
| Q06 | `doesNotRetryRecoveryAfterHold` | 模擬復旧境界がHOLD例外。入口1、保留結果Audit。再要求でも入口1。timeout等との組合せは増やさない |
| Q07 | `doesNotRepeatRecoveryAfterResultAuditFails` | 模擬recover正常復帰時にCONSUMER roleのAudit INSERTを一時REVOKEし、結果Auditだけ失敗。消費/consume Audit保持、復旧入口/模擬再公開各1。再要求でも増えない。finally復元 |

実物は新しい接続処理、既存consume/Identity/Audit/JPAとPostgreSQL、既存recover。recover配下の`IsolatedPublicationPort`・provider・Modulith再公開はtest用代替で、leaseの正常復帰を実publication完了と主張しない。statusはPUBLISHED一つ、target/proof/test principalは既存fixtureの有限供給。新しいproduction失敗switchは置かない。

### 直接回帰4件と通常root

新規7件成立・fresh XML保全・今回資源cleanup確認後、`NotificationFoundationTransactionTest`の次の4 methodだけを新規一回の選択集合として実行する。

- `commitsConsumptionWithBusinessAudit`
- `rollsBackConsumptionWhenAuditFails`
- `rejectsConsumptionWithoutCurrentCapability`
- `rejectsOutsideScopeWithoutLeakingTarget`

その後、通常構成のoffline root `clean verify`を最大一回。新test7件は`koiki.reference.permit-recovery-connection.verification.enabled=true`のtest-only flagがないと無効にする。専用時は既存resource-limits flagも要求する。production propertyは追加しない。

通常rootは既存305実行＋既存専用92 skip＋新専用7 skip＝**404 invocation（実行305・予定skip99）見込み、上限420**。実件数と差が出た場合は根拠を照合してSTOPし、勝手なtest追加/削除で合わせない。通常rootのPASSに専用skipを加算しない。

順序は**新規7件→直接回帰4件→通常root1回**。各集合初回一回・追加再実行0。専用全92件・R3 package・browser・async JAR pair・provider契約の実行は含めない。source不変のための重複baselineやroot反復はしない。

## 4. 時間・資源・作業量の上限案

文書source固定後、fresh preflight開始から**新規60分**。旧案の残時間は使わず、余剰枠でcaseを追加しない。

| phase | 時間上限 | 内容 |
| --- | --- | --- |
| preflight | 10分 | source/承認・環境・資材・資源/保護対象の読取確認。新規取得なし |
| 作成 | 25分 | 対象3 fileの作成、差分・fixture準備/cleanupの静的点検。既存codeを編集しない |
| 検証・集計 | 20分 | compileを含む新規7件6分、直接回帰4件3分、通常root10分、集計1分を区切りとする。予期しないcompile/test失敗は停止 |
| cleanup予約 | 5分 | 途中停止でも今回資源の終了・権限復元・証拠保存を確保 |

- JVMはcompilerを含め同時最大4、Maven/test fork heap768 MiB、fork1/reuseなし、JUnit並列無効。Q04の二要求threadだけを並行させる。forked javacにも`-Dmaven.compiler.meminitial=128m`/`-Dmaven.compiler.maxmem=768m`を直接指定する。
- 専用はPG1（memory1 GiB/CPU1/max_connections16）、Ryuk1、DB client8以内。既存3 contextのpool各最大2/min idle0を維持し、独立観測/管理接続を同時2本以内とする。新規contextを開く前に旧contextを閉じる。
- 通常rootはPG1/Ryuk2/client14、既存normal-root DB制限を使い、既存test/pool設定は改変しない。
- 開始前空きmemory8 GiB/disk10 GiB、raw合計50 MiB/作業領域1 GiB以内。DB起動60秒、context起動120秒、DB lock/statement/transaction timeout10秒。Q04のbarrier/Future終了待ちは各30秒以内、未終了を取消し要求だけで成功扱いしない。
- Wrapper/offline・既存imageのみ。Maven引数は各`-D...`を直接渡し、MAVEN_ARGS依存にしない。MAVEN_OPTS/TEMP/TMPは実行区間だけ変更しfinally復元。tmpは今回専用の実体path。
- 不足依存/imageの取得、追加compile/test反復、失敗後の原因修正や対象外探索は含めない。実行前に修正が必要と判明した対象も、本票を超えるなら停止して判断へ戻す。

明示選択はroot Wrapperの`-pl koiki-reference-app -am -Dtest=<classまたはmethod集合> -Dsurefire.failIfNoSpecifiedTests=false test`を候補とし、専用時に二つのtest flagを直接指定。通常rootでは両flag/async profileを除いて`clean verify`。各実引数配列・開始終了・終了値・XML件数を保存する。package JARを固定して動かす検証はしない。

## 5. 限定副作用とcleanup・停止条件

§7の承認に基づき、Ownerが限定環境管理責任を兼務し、今回所有の使い捨てPG/Testcontainers、isolated DB/role・既存migration/Identity/permit/proof fixture、context/pool・二要求thread、Q03/Q07の既知role/tableへの有限REVOKE/GRANT、独立SELECT・証拠保存とcleanupを本票内でAgentへ委任する。実行環境の権限付き承認は別に維持し、拒否を迂回しない。

既存`NotificationFoundationDbHarness`等の公開入口だけを利用する。公開入口では成立しない場合、既存harness/SQL/Serviceを追加改修せずSTOP。Q03/Q07の権限操作は専用DBのCONSUMER role・Audit INSERTだけに限定し、finally復元と確認を必須にする。

各集合終了時は、SecurityContext解除→thread/Future終了→context/pool/観測接続close→今回container停止→今回Ryuk/JVM残存確認を行う。開始前の保護process/containerは停止しない。今回所有IDの通常終了30秒・必要時の強制停止5秒を候補とし、不成立は残存を報告して停止する。

STOP条件：source/承認/変更pathの不一致、既存code/依存/migration変更が必要、資材不足、予期しないcompile/test失敗、件数/順序/回数の不一致、時間/資源/raw超過、権限復元・同期・cleanup・必要観測の不成立。期待する拒否/模擬例外だけは所定assertionが成立した場合に限りPASS。STOP時は実行済み/未実行と証拠・残存を保持し、追加反復しない。

## 6. Source固定・結果・Owner判断

1. **追加3 file・新規7件/直接回帰4件/root1回・60分・資源/停止/cleanup・test用代替の限界**は§7でOwner承認済み。
2. 本票の判断記録とAGENTS/Skillの限定例外は反映済み。対象文書の点検とcommitによるsource固定へ進む。**local commitは別承認またはOwner操作**。現在の未commit文書を未確認のまま一括固定しない。
3. その後に新branch `feature/phase4-s1-r4-consume-recovery-connection`候補のclean sourceを確認し、新規preflight。JDK21/Wrapper、Docker Server/image、offline資材・空き資源、保護対象、既存harness利用・test flagによる通常分離を点検。前提成立後のみ対象作成と限定検証へ進む。

結果候補：`docs/architecture/validation/phase4-s1-r4-consume-recovery-connection-20261016.md`。source/差分fingerprint、実引数、case別XML・DB観測・入口回数、結果Auditの対応、rootの実行/予定skip、資源/cleanupと限界を記録する。新規tmpのraw/XML/hashはGitでは移送されない。credential/payload/宛先実値は保存しない。

受入候補は**限定fixtureでの一回の接続順・非再実行と通常構成保護のみ**。FAILED完遂・実配信・正式入口・全停止真正性/使用中保護・歴史close・R4/DoD全体・本番運用・Phase4全体へ拡張しない。結果受入、成果commit、remoteは別判断。

## 7. Owner承認記録と網羅性の扱い（2026-10-16）

Ownerは本票を確認して承認した。また、現時点で網羅不足が残る可能性を認識し、都度の追加・見直しを想定する方針を示した。これを**本票§1〜6の方式・限定開始の条件付き承認**として記録する。

承認範囲は新規code/test3 file、Q01〜Q07の7件・直接回帰4件・通常root1回、新規60分（cleanup5分予約）、phase別資源・限定副作用・停止/cleanup。Ownerが限定環境管理責任を兼務し、§5内の今回所有の隔離DB/role・fixture・二要求thread・有限権限操作とcleanupを委任する範囲を含む。OS/実行環境が要求する権限付き承認は維持する。

**七ケースは初回の限定集合であり、網羅性の確定ではない。** 実装・検証で不足枝や残リスクが判明したら、具体的な事実・影響・追加/見直し候補を示す。既存PASSを完全性の証拠へ広げず、実行範囲/件数・path・予算・停止条件を変える追加はその時点で別判断する。今回承認だけで無制限にcaseや再実行を追加しない。

実行順は§6を維持する。まず承認記録とAGENTS/Skillの限定入口を反映し、**対象文書のlocal commitを別承認またはOwner操作で固定**する。その後に新実装branchのclean sourceとfresh preflightを確認し、成立時にだけ3 fileの作成・限定検証へ進む。本承認はlocal commit・remote操作・結果受入を兼ねない。

追加受付tableの保留、新Bean/runner登録・FAILED対応・実配信・本番運用・R4/DoD/Phase4全体の除外は変更しない。承認は条件付き開始であり、現時点のcode/test作成・検証は未着手。

## 8. Source固定・限定実行記録（2026-10-16）

Ownerが承認文書10件を`c749e44b25690dff52eded5604808fc26f92ad1a`としてcommitした。そこから新実装branch `feature/phase4-s1-r4-consume-recovery-connection`をcleanな状態で作成した。19:19:27 JST開始のfresh preflightが成立し、19:22:47から対象3 fileを作成した。

新規7件、直接回帰4件、通常root clean verifyを各初回一回・追加再実行なしで順に実施した。新規7/7 PASS、回帰4/4 PASS、通常root305実行PASS＋99 skip（404 invocation）、failure/error 0。19:45:52にXML集計、19:46:06にcontainer/今回JVM残存なし・保護process維持を確認し、承認phase枠と60分以内で終了した。code/testは実行前後のhash一致、既存code変更なし。

結果・限界・資源と端末内証拠は[実証記録§5](../architecture/validation/phase4-s1-r4-consume-recovery-connection-20261016.md#5-限定結果のowner受入2026-10-16)を参照する。Q04の表現訂正を条件としたOwner受入を反映済みで、次は成果差分の点検。今回の実行済み予算を追加実行に流用しない。成果commit・remote・後続実装の承認は行っていない。
