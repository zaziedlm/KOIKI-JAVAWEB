# S1 R4：処置記録の受付・照会 限定開始票案（2026-10-16）

状態：**HOLD / 追加受付table案はOwner判断で保留（2026-10-16）**。

受付機能と追加tableの採用優位が未確認のため、Ownerは追加受付table案を保留し、一件の復旧の「確認→許可→実行→結果確認」を既存機構へ対応付けるよう指示した。[現行の検討入口](phase4-s1-r4-failed-resolution-purpose-and-boundary-review-20261016.md)を参照する。

**以下の変更12 file・新規15件/回帰24件・90分・限定副作用の案は保留前の履歴であり、実装/検証の開始根拠として使用しない。** table/SQL/code/testは未作成、検証未実行。保留は既存permitやR3受入を取り消す意味ではない。

## 1. 目的・範囲と現行source

[R4レビューの受付・照会案とU01〜08](phase4-s1-r4-failed-resolution-purpose-and-boundary-review-20261016.md)を、変更対象・有限検証・停止条件が分かる開始単位へ具体化する。Owner指示は本票の作成までであり、以下の実装や検証を開始する指示ではない。

- 計画branch：`docs/phase4-continuation-task-inventory-20261016`、読取HEAD：`af9fa01c56df372559db99e23f495323114f8f93`。
- 作成するもの：Reference `notification`所有の**処置記録の一件受付と二経路照会**。既存Tier2/JPA・Identity・scope・Business/Security Auditを利用する。
- 操作はApplication直接呼出しの限定fixtureで確認。正式Web/CLI/non-Web認証入口の新設、consume/closeの肯定接続、runner・listener・provider呼出し、FAILED状態遷移、結果更新API、自動再処置は含めない。
- 一環境/publicationにつき一件。三つの区分は保持するが、今回の本番側書込みは**処置中の受付行作成だけ**。要確認/処置済の入力はtest fixtureで作り、状態遷移・歴史closeを実装/実証済みとしない。

## 2. 採用候補の最小方式

1. Reference DBに処置tableを一つ追加。operation ID主key、environment/publicationの無条件unique、発行時target・permit・操作者・処置内容・三つの区分/理由・時刻・結果参照を保持する。既存permit/consumptionの列・制約は変更しない。
2. 発行済みpermitを入力とする。現在EXECUTE能力/scope・発行者本人・target/permit・期限を確認し、**消費済み/閉鎖済みpermitによる新規受付は拒否**。受付とBusiness Auditを一つの短い所有transactionでcommit。consumeは呼ばない。
3. 重複はDB制約で止め、競合transaction終了後に既存行を別読取transactionで照会する。READ能力/scopeがなければ内容を返さない。同じoperationの対象/permit/処置変更は拒否、ID変更による同一対象の新規受付も拒否する。
4. operation ID＋環境/対象、または環境/publicationの二経路で、scopeをSQLへ適用した最終recordを返す。Entityは外へ出さない。DB照会不能を未登録へ変換しない。
5. 受付の新規commit確定と既存記録の返却を戻り値で区別する。ただし本票では戻り値から復旧を起動する呼出し元を作らない。受付行をqueueとして走査しない。
6. 既存foundationの有効化条件を使用し通常は無効。独立した実行スイッチは増やさない。権限codeは既存`NOTIFICATION:PERMIT:EXECUTE`/`READ`を利用し、独自のFramework権限体系は追加しない。既存Serviceのprivateな認可処理は呼べないため、新Service内で同じ既存Portと契約を適用する最小処理を作る。共通認可基盤への再設計・既存Serviceの変更は行わない。

### Migration・Entity登録の限定条件

現行`NotificationFoundationConfiguration`は`RecoveryPermit`のpackageをEntityScanし、Referenceは`ddl-auto=validate`。新Entityだけを独立flagで無効にする構成は今回は採らない。同じfoundation有効時に新tableを必要とする変更としてreviewする。

追加SQLは`db/migration/kkref-notification/V7__create_notification_recovery_intake.sql`候補。既存V1〜V6は編集せず、通常の`spring.flyway.locations=classpath:db/migration/kkref`も変えない。**foundationを有効にする検証fixtureでだけ追加locationを適用**し、V7とEntityの不一致は起動時拒否する。

今回の使い捨てDBは既存foundation用のV1〜V4にV7を追加する構成。V5/V6のasync schemaを同じDBへ後から追加する順序は本票で検証しない。V7適用後に下位versionのV5/V6を追加する一般的な更新経路を成立済みとせず、out-of-orderやvalidate条件を緩めない。実DB更新・asyncとの同一DB切替は別のmigration review対象に残す。

## 3. 変更pathと上限

次の名前は本票の採用候補。すべて`koiki-reference-app/`配下で、code/test/SQLは**最大12 file＝新規11＋既存1**。新しいpackageは作らず既存packageへ配置する。実装上追加fileや既存production変更が必要なら作成前にSTOPして差分を提示する。

| 区分 | path（共通prefixは`src/`） | 役割 |
| --- | --- | --- |
| main新規1 | `main/java/org/koikifw/reference/notification/domain/model/RecoveryIntake.java` | JPA共有Entity・三つの区分。今回は受付後の公開更新操作を追加しない |
| main新規2 | `main/java/org/koikifw/reference/notification/domain/repository/RecoveryIntakeRepository.java` | 受付insert・scope付き照会の狭いRepository |
| main新規3 | `main/java/org/koikifw/reference/notification/adapter/outbound/persistence/JpaRecoveryIntakeAdapter.java` | JPA保存・DB制約競合・scope付き読取 |
| main新規4 | `main/java/org/koikifw/reference/notification/application/RecoveryIntakeService.java` | 認可・対象/permit照合・受付/Audit transaction・重複後照会 |
| main新規5〜6 | `main/java/org/koikifw/reference/notification/application/query/RecoveryIntakeRequest.java`、`RecoveryIntakeView.java` | 不変な要求/最終record。受付結果区分は必要なら同fileのnested型に閉じる |
| main新規7 | `main/java/org/koikifw/reference/notification/configuration/RecoveryIntakeConfiguration.java` | 既存foundation条件による明示Bean登録。通常無効 |
| main新規8 | `main/resources/db/migration/kkref-notification/V7__create_notification_recovery_intake.sql` | Reference追加table・制約のみ。既存tableのUPDATE/DELETE/ALTERなし |
| test新規1〜2 | `test/java/org/koikifw/reference/notification/RecoveryIntakeRegistrationTest.java`、`RecoveryIntakePersistenceTest.java` | 登録3 invocation＋DB12 invocation |
| test新規3 | `test/java/org/koikifw/referenceacceptance/notification/RecoveryIntakeTestSupport.java` | 既存DB harnessを組み合わせる有限fixture・故障注入・再接続。runnerは作らない |
| test既存1 | `test/resources/notification/s1-isolated-grants.sql` | 新tableに必要な既存test roleのgrantだけ追加。既存tableのgrant・assertionを変更しない |

既存`NotificationFoundationDbHarness.java`と他test sourceは読取再利用のみ。新fixtureから既存公開メソッドで準備できない場合、共通harnessを無断改修せずSTOPする。production/testの故障switch、POM・依存、Root Reactor、Framework API/Rules/Security/CI、既存V1〜V6、通常properties、`RecoveryPermitService`・復旧Serviceは変更しない。

文書は**最大10 file**を候補とする。本票、R4レビュー、棚卸し、C01参照資料、開発index、端末検証記録、今回新規Evidence1件、および承認後に必要となるAGENTS・project-overview/business-feature Skillの現行入口に限定する。過去の受入/FAIL記録を上書きしない。上記12 fileとは別枠で、合計最大22 file。文書source固定は§7の別判断に従う。

## 4. 有限の検証集合と順序

新規は**2 test class・15 invocation**。parameterizationで増やさず、1行1 methodを原則とする。各method内の有限fixture/要求数は以下の観点に閉じる。

| 順・集合 | method候補・件数 | 内容 |
| --- | --- | --- |
| 1 / 登録 | `RecoveryIntakeRegistrationTest` 3件 | property未指定、falseで新構成なし。不正値は拒否。trueの実登録/Entity/SQL整合はP01で確認 |
| 2 / 新規DB | `RecoveryIntakePersistenceTest` 12件 | 下表P01〜P12、各初回1回 |
| 3 / 直接回帰 | `NotificationFoundationRegistrationTest`全8件、`NotificationFoundationTransactionTest`全16件 | 既存の通常無効・未接続拒否・認可/保存/Audit・consume/close契約を保護。新経路からconsume/closeを接続する検証ではない |
| 4 / 通常root | 通常構成offline `clean verify` 最大1回 | 専用flag/async profileなし。新DB12件は新専用flagで無効、既存専用92件も無効。新登録3件を含め実行308＋予定skip104＝412 invocationを見込み、**430 invocation以内**。件数の不一致は理由照合してSTOPし、testを勝手に増減しない |

| 新DB method候補 | 対応・期待する確認 |
| --- | --- |
| P01 `acceptsOneAndReadsByOperationAndTarget` | U01。V7・実Bean/JPA・一行/受付Audit一件・二つの照会。発行済みpermitをfixtureで用意 |
| P02 `repeatsSameRequestAsLookup` | U02。同一要求の再提示で行/Audit増分0、新規受付結果にしない |
| P03 `rejectsChangedRequestForSameOperation` | U02。同IDの別対象/別処置を有限の二要求で拒否 |
| P04 `blocksNewOperationForEachStoredCategory` | U02。同じ対象を別IDで要求。三つの区分をfixtureで順に作り、各々一件のまま。状態遷移APIの証拠にはしない |
| P05 `acceptsOnlyOneOfTwoConcurrentRequests` | U03。独立二transaction、行/Audit一件。DB unique競合後の読取 |
| P06 `rejectsWithoutExecuteCapability` | U04。新規受付行なし |
| P07 `deniesDuplicateDetailsWithoutReadCapability` | U04。EXECUTEあり/READなしで重複時の内容非開示 |
| P08 `hidesOutsideScopeOnBothLookups` | U05。二経路ともscope外の内容/存在を非開示 |
| P09 `returnsMissingWithoutStartingAnything` | U05。scope内記録なしと、consume/対象publication不変 |
| P10 `rollsBackIntakeWhenAuditInsertFails` | U06。専用roleのAudit INSERTをtest内で一時REVOKEしfinally復元。独立接続で受付行/受付Auditなし |
| P11 `looksUpCommittedRequestAfterLostResponseAndReconnect` | U07。commit後の応答だけ破棄、新context/接続で同じ行を照会。再要求は新規受付にしない |
| P12 `holdsWhenLookupIsUnavailableThenReadsAfterRecovery` | U08。新tableへのtest roleのSELECTを一時REVOKEしfinally復元。照会不能を未登録へ変えず、新規受付も消費も行わない |

新DB classは`koiki.reference.intake.verification.enabled=true`**かつ**既存resource-limits flagの明示を要求する専用testとする。新flagはtestの選択にのみ使い、production propertyを増やさない。未指定は12 skipとして通常rootから分離し、DB起動前に制限成立を確認する。

登録3件→新DB12件→直接回帰の各class→rootの順に**各集合一回**。登録3件はrootでも通常回帰として実行されるが、失敗後の追加再実行ではない。直接回帰24件は既存専用92件の選択部分であり、全92件を実行しない。browser・async JAR pair・provider契約・R3 packageケースの追加実行は含めない。

MavenはWrapper・offlineで実行し、各classは明示選択する。Maven/test fork heap768 MiB、fork1/reuseなし、JUnit並列無効。forked javacにも`-Dmaven.compiler.meminitial=128m`/`-Dmaven.compiler.maxmem=768m`を直接供給する。資源設定は**完全な`-D...`引数を直接指定**し、`MAVEN_ARGS`依存にしない。通常rootは既存normal-root DB制限だけを使い、専用有効化flagを残さない。具体配列をrun別JSONへ保存する。

明示選択はroot Wrapperの`-pl koiki-reference-app -am -Dtest=<class> -Dsurefire.failIfNoSpecifiedTests=false test`候補とし、専用3 classにだけ上記二つの専用flagを加える。通常rootはprofile/専用flagなしの`clean verify`。MAVEN_OPTS・TEMP/TMPは実行区間だけ設定してfinallyで復元し、今回専用の実体pathを使う。選択外moduleのtestを誤実行しないことと、対象classのfresh XML・期待件数を各回照合する。

## 5. 時間・資源・実行回数の上限案

過去の検証枠を使わない**新規90分**。条件付き開始が承認され、source固定後のpreflight開始から計上する。phase間の自動繰越・延長なし。実装を含む上限であり、余剰時間を追加caseへ使わない。

| phase | 時間上限 | 資源・実行上限 |
| --- | --- | --- |
| preflight | 10分 | source/環境/資材/資源の読取照合。新規取得・test実行なし |
| 作成・compile/早期登録3件 | 45分 | 対象12 file以内。offline test-compile相当＋登録3件の明示実行1回。実装前に既存harness再利用経路を読取確認 |
| DB/直接回帰/通常root・結果保存 | 30分 | 専用3 class各1回、通常root1回。新DBと直接回帰合計15分、root10分、集計/照合5分を区切りとする |
| cleanup予約 | 5分 | 既に所有が記録された今回資源だけを終了・照合。途中STOPでも確保 |

- 開始前：空きmemory8 GiB以上・disk10 GiB以上、稼働する今回検証資源0。既存Java/process/containerは保護対象としてIDと開始時刻を記録。
- 同時JVMはMaven/test fork/必要なcompilerを含め最大4、各heap768 MiB。DBは使い捨てPG1、memory1 GiB/CPU1/max_connections16。専用classはRyuk1/client8以内、通常rootはRyuk2/client14以内。通常rootの既存pool設定は変更しない。
- 新規DB testのruntime contextは同時2つまで、各pool最大2/min idle0。既存直接回帰はPERMIT/CONSUMER/READERの既存3 context・各pool最大2/min idle0を維持し、準備/観測接続を含むclient8を超えない。U03の二transactionは新規testの範囲。使い捨てcontainer1件をclass内で共有してよいが、class終了後は停止・残存確認して次へ進む。
- 専用DB起動60秒、context起動120秒、DB lock/statement/transaction timeout各10秒、同時要求の各Future終了待ち30秒。取消要求だけで終了とせず、未終了ならSTOP。通常rootの既存test timeoutは改変しない。
- raw合計100 MiB、作業領域2 GiB以内。credential/payload/宛先実値は保存しない。既存image/依存だけを使い、不足は取得せずSTOP。
- 追加再実行0回。compile/test/packageの予期しない失敗は保全してSTOP。期待拒否・DB制約競合・故障注入はtestが所定結果をassertした場合だけPASS。余剰枠での原因修正・root反復なし。

実時間・資源sample・各終了値をrun別に保存する。sampleだけで全時点の最大値を証明したとは書かず、有限設定による制限と観測した値を区別する。

## 6. 限定副作用・停止とcleanup

Ownerが限定環境管理責任を兼務し、本票内の次の操作をAgentへ委任することを**開始判断の対象**とする：今回所有の使い捨てPG/Testcontainers、isolated DB/role、既存＋V7 migration、fixture対象/Identity/発行済みpermit準備、有限な権限制御、Application context/二要求thread、証拠保存とcleanup。稼働環境・他project DB/roleへ操作しない。OS/実行環境の権限付き承認は別途維持し、拒否を迂回しない。

P10/P12のREVOKEは専用DBの既知role・既知table/権限だけに限定する。必ずfinallyでGRANTを復元し、回復確認後にcontext/接続を閉じる。復元失敗はPASSにせずSTOPし、所有containerの終了まで確認する。既存のtest-only grant fileへの追加は、新tableの必要最小権限だけとする。

cleanupはSecurityContextの解除、test thread/Future終端、context/pool/観測接続のclose、今回containerのstop、Ryukの終了待ち、今回JVM残存確認を順に行う。testが失敗してもfinallyで行う。他projectの停止済みcontainerや保護processは削除/停止しない。強制停止が必要な場合は記録した今回所有IDに限り、通常終了30秒→有限の強制停止5秒を候補とし、失敗は記録してOwnerへ戻す。

STOP条件：source/branch/変更path/件数の不一致、通常構成への予期しない登録・migration/依存影響、既存harness公開入口だけでは成立しない、資材不足、上限超過、予期しないcompile/test失敗、結果/権限復元/所有資源cleanupの観測不能。停止時は実行済み/未実行・残存・終了値・次の判断対象を明示し、追加探索・修正・再実行へ広げない。

## 7. Source固定・preflightと開始判断

1. Ownerが本票§1〜6の方式・path/件数・新規15件/直接回帰24件/root上限・90分・限定副作用/cleanupをreviewする。これはまだ承認されていない。
2. 採用された場合だけ、本票の判断記録と必要なAGENTS/Skillの現行入口へ受付/照会限定の例外を文書反映する。**文書commitは別承認またはOwner操作**とし、文書source固定前にcode/test/SQL作成を始めない。
3. 固定後に新作業branch `feature/phase4-s1-r4-intake-read`候補を作り、clean HEAD・文書承認source・既存fixture/Wrapper・Java21・Docker Server/image・offline依存・資源/保護IDをfresh確認。既存の端末PASSや旧予算はpreflightの代替にしない。
4. preflight成立時のみ§3〜6を実施する。判明した前提不一致は作成前に止める。権限付き実行が要求される場合は、その環境承認も取得する。

結果は新しい`docs/architecture/validation/phase4-s1-r4-intake-read-20261016.md`候補へ、source/差分fingerprint・case別結果・rootの実行/予定skip・資源/cleanup・未達を記録する。run raw/XMLは新しいtmp配下に保持しhashを残す。tmpや実DB状態はGit移送されない。

受入対象候補は**受付・照会と通常構成保護だけ**。三つの運用区分の全遷移・consume/close接続・実復旧・provider送信・E10a完遂・R4全体・DoD全体・正式受渡し・Phase4全体へ広げない。結果受入、成果commit、remoteは別判断。

**Owner判断欄：未判断。** 本票作成指示だけでは実装・検証を始めない。
