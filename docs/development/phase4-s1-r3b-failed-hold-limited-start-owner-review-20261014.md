# Phase 4 S1 R3-B：FAILED保留証拠の限定開始票案（2026-10-14）

状態：**R3-B LIMITED RESULTS / OWNER ACCEPTED**（[検証記録§23](../architecture/validation/phase4-s1-r3b-source-preflight-and-blocking-review-20261014.md#23-r3-b限定結果のowner受入2026-10-14)）。B03a自動完了FAILとE08／DoD未達は保持する。[比較票§7](phase4-s1-r3b-failed-publication-limited-correction-owner-review-20261014.md#7-owner採用判断2026-10-14)でOwnerがBの人手照合HOLDを先行採用したことを入力とする。本票はその方針を実証するための**新規実行枠**であり、[初回B03a FAIL](../architecture/validation/phase4-s1-r3b-source-preflight-and-blocking-review-20261014.md#11-e10事前点検とb03a初回fail停止2026-10-14)で停止した枠の残時間・追加再実行権ではない。第一段階の承認・結果は§6と検証記録§15、直接回帰結果は§10と検証記録§19、通常rootは§12と検証記録§21を参照する。

## 1. 目的と未達の扱い

provider独立台帳では受理済み、Reference受理観測logは未保存、publicationは`FAILED`という実際の応答喪失を、**再送・自動完了を行わずに**人へ引き継ぐためのHOLD証拠として固定する。元Reference processの停止、対象・別対象、受理ID、送信数、照合時点を別々に確認し、担当者が次に見るべきprovider台帳とReference publication／logを示す。これはToolingによる限定実証であり、正式なoperator受付・認可／Audit・保留台帳・再開操作を作らない。

B03a初回FAILの`FAILED`を`PUBLISHED`／`PROCESSING`へ偽装しない。既存のB03a自動完了テストは未達の診断ケースとしてsourceとFAIL Evidenceを残し、**本票では再実行しない**。新しいHOLDケースは別method・別XML／log／source epochにする。現行Reference実装では`FAILED`からのpublication完了は成立していないため、HOLDケースがPASSしても**E10aの完遂、DoD 4-2／4-3、R3-B／R3全体の`COMPLETE`は未達**と明記する。AのFAILED自動復旧の必要性はR4の運用統制とあわせて再判断する。

## 2. 対象sourceと静的開始条件

対象は`build-support/reference-e2e-verification/`の`PackagedReferenceAsyncUnknownRecoveryTest.java` **1 file以内**のTooling-owned追加methodと、必要なら同file内の小さな証拠保存helperだけ。既存`B03aAcceptedButLostResponseCompletesWithoutSecondPost`の期待値・FAIL履歴をPASSへ書き換えない。新methodは`FAILED`を事前判定し、**`authorize`・`recover`・復旧子起動へ進まない**。HOLD JSONはToolingが独立保存し、理由code `FAILED_RECONCILIATION_REQUIRED`、publication／event ID、通知keyのSHA-256、provider受理ID、元processのPID／開始時刻／停止確認、観測時刻、Reference log有無、publication status／attempt、到着・受理数の前後、次の照合先を含める。payload hashと通知key hashを混同しない。秘密・接続情報・payload本文は保存しない。照合値が取れない場合は成功扱いにせず`UNKNOWN`のままSTOPする。

Reference production／test、Port・migration、Framework API／Rules、Security、POM／依存、通常properties、Root Reactor／CIを変更しない。固定通常／async JARは[既存対照結果§7](../architecture/validation/phase4-s1-r3b-source-preflight-and-blocking-review-20261014.md#7-tooling-compile通常async-jar対照の限定結果2026-10-14)の同じものを用い、再packageしない。変更が必要なら本票で続行せず差分・artifact epochを改めて判断する。

開始前の静的点検では、別publicationを同じfresh DBに決定的に作りsnapshotを保持できること、受理IDをprovider独立observerから読めること、Reference log不存在と`FAILED`状態を独立接続で読めること、HOLD JSONをfixture cleanup後も保全できることを確認する。どれか不成立ならmethodの実動作へ進まない。`R3RecoveryFixture.expectHold`は復旧子を起動するため、本ケースのHOLD証拠の代用にしない。

## 3. 第一段階の最小検証と新規上限候補

1. branch `feature/phase4-s1-reference-unknown-recovery-r3b`／HEAD `fcc515c`、現行未commit差分、Reference source不変、固定通常JAR SHA-256 `462E9B663E0852E5FBF6F7E8E2C3FA4BAC9FAD31DDFFB78C76C18ADA2BA2C623`／async JAR `7A09B3F5B242477D31FDFFC4D8894CBD7A4C059C0D0BB4BAAB3A72F95F937B34`、保護process、Docker Server／所有container0、JDK21／Wrapper／offline依存をfresh preflightで照合する。両JARのapp classes・共通runtime一致は既存§7の結果を入力とし、新Tooling差分をJARの結果へ混ぜない。
2. §2のTooling差分を作り、Tooling `-o test-compile` **新規1回**で現行sourceをcompileする。Reference compile／packageは行わない。source差分とcompiled classのepochを記録する。
3. 成立時、固定async JAR・fresh隔離DBで**新HOLD methodだけを1 Maven invocation**実行する。受理済み対象と別publicationのsnapshot、provider台帳の対象受理ID、Reference log不存在、`FAILED`／attemptを停止前後で独立照合する。復旧子・追加POSTを起動せず、provider到着／受理総数および別publicationを不変とし、JSONの値を再読取してassertする。終了直後に同名XMLの上書き前保全と有限cleanupを確認する。

第一段階は**新規30分、cleanup5分予約**。Tooling所有JVM最大4・各heap768 MiB、PG1／Ryuk1／DB client8、開始前空きmemory8 GiB／disk10 GiB、raw log20 MiB／作業領域500 MiB、子起動120秒・通常終了30秒・所有子強制停止5秒を上限候補とする。case初回1回・**追加再実行0回**。Ownerが今回所有の隔離DB／専用schema・role、loopback stub、package済み子JVM、有限cleanupを委任する場合だけ実施する。実行環境が権限付き承認を求める場合はその手順に従う。失敗、前提不一致、資源超過、所有process／container cleanup未成立時はSTOPし、次段へ進まない。XML・redacted子log・HOLD JSON・所有資源記録・各SHA-256を`tmp`にepoch別保全し、Git移送されないことを記録する。

## 4. 第一段階後の判断対象（本票では実行しない）

HOLDケース成立後に、未実行のB03b（受理前切断）→B03c（照会不能）を**別fresh DB・各初回1回**で検討する。B03bは独立provider台帳で停止前到着／受理0と有効keyを確認してから同key1回の受理・publication完了を照合する。B03cは照会不能のまま送信増分0・理由付きHOLD・publication不変を照合する。どちらか不成立なら後行を止める。受理前切断を一般のHTTP例外だけから推定しない。

続く直接回帰候補4件は、`PackagedReferenceAsyncRecoveryTest`の`A01PublishedBeforeSendCompletesOneTargetAfterFullStop`／`A03AcceptedBeforeCompletionReusesOneProviderReceiptAfterFullStop`、`NotificationProviderContractTest`の`reusesTheOriginalReceiptForIdenticalRequests`（D01）、`PackagedReferenceAsyncNotificationTest`の`correlatesTwoIndependentApprovalNotificationsToCompletedPublications`（C04）。各1 invocation・fresh XMLとして、当時のR3-A PASSと新epochを混同しない。通常rootはそれらが成立した場合だけ**最大1回**、fresh preflightと新しい有限枠を別判断し、通常無効・専用skip・JAR内容／runtime依存を照合する。通常rootのJVM4／PG1／Ryuk2／DB client14候補は維持するが、旧150分の残りを流用しない。

B03b／c、直接回帰、通常rootの**具体的な時間・資源・順序承認は第一段階の結果後に別判断**とする。初回B03a FAILを新HOLD methodのPASSで上書きせず、専用クラス全件を自動選択しない。新method追加により当初のpackage新規7 method候補は8 methodとなるが、Reference契約は実際2 methodで、全体の新規method総数は10以内の見込み。**package7件という元の内訳への明示例外**は第一段階の承認で確認する。既存B03aの将来のtest扱いとmain統合時の未達表示も別判断に残す。

## 5. Ownerの第一段階開始判断

Ownerが判断する対象は§1〜3の**Tooling 1 file、静的点検、offline test-compile1回、新HOLD method1回、30分・資源・cleanup**と、§4のpackage method数内訳への例外に限る。Ownerがこの限定開始を明示するまでは、source作成・compile・Docker起動・新case実行を開始しない。B03b／c・直接回帰・通常root、AのFAILED自動復旧、R4実装、結果受入、local commit・remote操作は本票の第一段階開始には含めない。

## 6. 第一段階のOwner承認と実行境界（2026-10-14）

Ownerは本票を確認し「これを進めます」と指示した。§1〜3のTooling-only静的点検、1 file追加、offline test-compile1回、新HOLD method1回、30分・cleanup5分予約、§4のpackage method内訳への例外を**第一段階の限定開始**として扱う。§5の「開始未承認」は承認前の履歴である。B03b／c・直接回帰・通常rootと結果受入、local commit・remoteは含めない。実際のpreflight、source／artifact epoch、PASS／STOP、cleanupは[検証記録§15](../architecture/validation/phase4-s1-r3b-source-preflight-and-blocking-review-20261014.md#15-failed-hold第一段階の限定実行2026-10-14)に記す。

## 7. 第一段階後のB03b／c限定判断案（2026-10-14）

[検証記録§15](../architecture/validation/phase4-s1-r3b-source-preflight-and-blocking-review-20261014.md#15-failed-hold第一段階の限定実行2026-10-14)はHOLDケース1件PASS、所有資源cleanup成立であり、B03a自動完了FAILは残る。Ownerの「次へ進めます」を受け、ここでは**B03b→B03cの2件だけ**を次の新規実行枠候補にする。実行前の静的点検で、現行の両methodには[R3-B契約レビュー§9](phase4-s1-r3b-purpose-log-provider-unknown-owner-review-20261014.md#9-最小開始票へ渡す契約と対象候補)が各枝に求める**別publicationの不変照合がない**と判明した。この不足を残してE10枝の成立を主張しない。Toolingの`PackagedReferenceAsyncUnknownRecoveryTest.java` **1 file内だけ**に、各ケースで別承認publicationを作り、元process停止後のread-only DB tupleを復旧／HOLD後と比較するassertionを加える。B03bではgate後・元process停止前、B03cではtargetのDB gate成立後・元process停止前に別承認を作り、対象keyのprovider到着／受理と別keyの副作用を独立して数える。B03c専用の準備helperとkey別の独立SELECT helperを同file内に置き、共通`preSend`を使う既存B02a〜cのfixtureや既存B03a FAIL／新HOLD methodは変更しない。別publicationの故障位置を決定的に作れなければ、実動作前にSTOPする。Reference source・固定JAR、migration、POM／依存は不変更。

開始前にbranch／HEAD、`git diff --check`、固定通常／async JAR hash、JDK21／Wrapper・Docker Server／image／稼働container0、保護processと空きmemory8 GiB／disk10 GiBをfreshに照合する。Tooling限定訂正後にoffline `test-compile`を**新規1回**実行し、成立したcompiled classだけで下表へ進む。source／class／artifactのepochを別に記録する。compileまたは前提が不成立ならcaseを開始しない。

| 順序 | 選択method・fresh DB | 必須の独立照合 |
|---|---|---|
| 1 | `PackagedReferenceAsyncUnknownRecoveryTest#B03bDisconnectedBeforeArrivalResendsSameKeyOnce`、初回1 invocation | gateで対象keyのstub到着前に止め、元process停止・対象snapshot・対象keyの到着／受理0を確認。期限内の同key契約で単一復旧子を動かし、対象keyの到着／受理各1、Reference受理観測logのevent／宛先／hash／受理ID一致、publication完了、別publication不変を照合。接続断の例外だけで未受理と断定しない |
| 2 | `PackagedReferenceAsyncUnknownRecoveryTest#B03cUnavailableQueryHoldsWithoutPost`、B03b成立時のみ初回1 invocation | 対象keyの送信前停止・対象snapshot、stub照会不能、復旧子`UNKNOWN`理由付きHOLD JSON、対象keyのprovider到着／受理増分0、Reference log不存在・publicationと別publicationの不変を照合 |

**新規45分、cleanup5分予約**。各ケース所有JVM最大4・各heap768 MiB、PG1／Ryuk1／DB client8、raw log40 MiB／作業領域1 GiB、子起動120秒・復旧終端150秒・通常終了30秒・所有子強制停止5秒。今回所有の隔離DB／schema・role、loopback stub、故障gate、元Reference／単発復旧子と有限cleanupだけを対象とし、保護processに触れない。各ケース終了直後にXML・redacted子log・ownership・HOLD JSONをcase別`tmp`へ退避してhashを固定し、所有process／container0を確認する。B03bで停止位置・受理数・完了・cleanupが不成立ならB03cを開始しない。追加再実行0回、旧150分と第一段階30分の残りは流用しない。

**この§7は実行上限を具体化した判断案であり、まだ開始承認ではない。** 直接回帰4件・通常root、B03a自動完了、Reference訂正、結果受入、commit／remoteは含めない。B03b／c成立後もE10aとDoD未達を維持し、後続の必要性・上限を別判断する。

## 8. B03b／c限定進行のOwner判断と結果（2026-10-14）

Ownerは§4の第一段階後の対象を確認・承認し「次へ進めます」と指示した。§7に具体化した**Tooling test1 file・offline test-compile1回・B03b→B03c各初回1回・45分／cleanup5分予約**を、§4の次段を実施する上限として採用した。§7の「開始未承認」はその具体化時点の履歴である。実際のsource／artifact・preflight、各ケース結果、Evidence hash、cleanupは[検証記録§17](../architecture/validation/phase4-s1-r3b-source-preflight-and-blocking-review-20261014.md#17-b03bc限定結果と残る判断2026-10-14)に記録した。直接回帰4件・通常root、B03aの自動完了・R3-B結果受入、commit／remoteは今回の進行範囲に含めない。

## 9. 直接回帰4件だけの新規実行枠案（2026-10-14）

[検証記録§17〜18](../architecture/validation/phase4-s1-r3b-source-preflight-and-blocking-review-20261014.md#18-17後の残ケースと判断境界2026-10-14)のB03b／c限定PASSと残ケース整理を入力とする。**この枠は旧150分、HOLDの30分、B03b／cの45分の残りを流用しない。** 現R3-B source／固定JAR epochに対し、R3-Aから影響を受ける直接回帰だけを次の順で実施する。各行はfresh隔離DB・専用schema／role・loopback stubで**初回1 Maven invocation**、先行行の結果・証拠保全・cleanupが成立した場合だけ次行へ進む。

| 順序 | 選択method | 確認する境界 |
|---|---|---|
| 1 | `PackagedReferenceAsyncRecoveryTest#A01PublishedBeforeSendCompletesOneTargetAfterFullStop` | 送信前の全停止から単一対象だけを復旧し、provider受理・publication完了を確認 |
| 2 | `PackagedReferenceAsyncRecoveryTest#A03AcceptedBeforeCompletionReusesOneProviderReceiptAfterFullStop` | provider受理後・完了前の全停止から、同じ受理を用いて完了し追加POSTしないことを確認 |
| 3 | `NotificationProviderContractTest#reusesTheOriginalReceiptForIdenticalRequests`（D01） | stubに対する同一key・同一内容の逐次要求が同じ受理IDを返し、受理行を増やさないことを確認 |
| 4 | `PackagedReferenceAsyncNotificationTest#correlatesTwoIndependentApprovalNotificationsToCompletedPublications`（C04） | 別々の承認通知2件のevent・publication・provider受理の相関と完了を確認 |

開始前にbranch／HEAD `feature/phase4-s1-reference-unknown-recovery-r3b`／`fcc515c`、現行差分と`git diff --check`、関連Tooling source／compiled classのepoch、通常JAR SHA-256 `462E9B663E0852E5FBF6F7E8E2C3FA4BAC9FAD31DDFFB78C76C18ADA2BA2C623`／async JAR `7A09B3F5B242477D31FDFFC4D8894CBD7A4C059C0D0BB4BAAB3A72F95F937B34`、JDK21／Wrapper・offline依存、Docker Server／必要image／稼働container0、保護process、空きmemory8 GiB／disk10 GiBをfreshに点検する。A01／A03のopt-inは`koiki.reference.async.r3.verification.enabled=true`、D01／C04は`koiki.reference.async.verification.enabled=true`とし、**method単位選択・指定test不在時FAIL・fork1／reuseなし・heap768 MiB**を明示する。現行sourceと既存compiled classが一致しない場合は実行前にSTOPし、compile・source修正をこの枠へ暗黙追加しない。固定JARの再packageも行わない。

**新規50分、cleanup10分予約**、合計最大4 Maven invocation・各method初回1回・**追加再実行0回**とする。各case所有JVM最大4・各heap768 MiB、PG1／Ryuk1／DB client8、PG上限CPU1／memory1 GiB／max connections16、raw log合計40 MiB／作業領域1 GiB、子起動120秒・通常終了30秒・所有子強制停止5秒を上限候補とする。今回所有の使い捨てDB／schema・role、loopback stub、package済みReference／復旧子、有限cleanupだけを対象とし、保護processには触れない。実行環境が権限付き実行を求める場合は承認済み手順に従う。

各行の直後に同名XMLが次のcaseで上書きされる前に、XML・txt／dumpstream・秘密／接続情報を除いた子log・stub観測・ownershipをcase別`tmp`へ退避し、SHA-256とsource／artifact／run epochを記録する。所有JVMとcontainer0、DB接続・資源上限、保護process生存を照合する。前提不一致、test失敗／skip、証拠欠落、上限超過、cleanup不成立ではその時点でSTOPし、残りを実行しない。`tmp`証拠と固定JARはGit移送されない。

この4件がPASSしても、既知FAILの`B03aAcceptedButLostResponseCompletesWithoutSecondPost`、E08のReference逐次／stub並行／listener並走、DoD 4-2／4-3、R3-B／R3全体の`COMPLETE`は未達として維持する。専用クラス全件・通常root、Reference訂正、結果受入、commit／push／PR／mergeは本枠に含めない。通常rootは4件の結果を入力にfresh preflightと新しい有限枠を別判断する。**本§9は実行条件の具体化であり、Ownerが50分・4件の開始を確認するまではMaven／Docker実行を開始しない。**

## 10. 直接回帰4件のOwner開始承認と結果入口（2026-10-14）

Ownerは§9の**4 method、各fresh DB・初回1回、50分／cleanup10分予約、先行不成立時STOP**を確認して実行開始を承認した。§9末尾の開始未承認は承認前の履歴である。同じTooling compiled classと固定JAR対からA01→A03→D01→C04を順次実行し、各1件PASS・failure／error／skip 0、所有資源cleanup成立。source／artifact／run epoch、XML・logのhashと限定境界は[検証記録§19](../architecture/validation/phase4-s1-r3b-source-preflight-and-blocking-review-20261014.md#19-直接回帰4件の新規限定実行結果2026-10-14)に固定した。通常root・R3-B結果受入・commit／remoteは今回の開始承認に含まれない。

## 11. 通常rootの新規限定枠とR3-B受入判断の順序案（2026-10-14）

Ownerは§10を確認し、次の通常rootとR3-B結果受入へ進む意向を示した。[検証記録§19](../architecture/validation/phase4-s1-r3b-source-preflight-and-blocking-review-20261014.md#19-直接回帰4件の新規限定実行結果2026-10-14)の直接回帰4件PASSを入力に、**通常構成保護だけを検証する新規root枠**を設ける。元のR3-B開始票§4はE10の全枝成立後にrootを置いたが、B03a自動完了は§11でFAILし、OwnerはFAILEDの人手HOLDを先行採用した。今回のroot PASSをB03aの完遂やR3-B全体`COMPLETE`の代用にしない。root後の限定結果のOwner受入可否は、B03a／E08／DoD未達を明記して**別の判断**とする。

**新規30分**（実行直前preflight5分、通常root15分、XML／JAR／log保全と集計5分、cleanup5分予約）、通常構成のoffline root `clean package`を**最大1 Maven invocation・追加再実行0回**。`s1-reference-async` profile、Tooling専用test、過去選択caseの再実行を含めない。Maven／compiler／forkを各heap768 MiB、test fork1・reuseなし・JUnit並列なし、通常root専用`koiki.reference.normal-root.db-limits.enabled=true`を指定し、専用検証用`koiki.reference.verification.resource-limits.enabled`とasync有効化propertyは与えない。`maven.jar.forceCreation=true`で通常JARを再生成する。開始前に既存の選択XML／固定JARを別`tmp`へ保全済みであることを照合する。

今回所有JVM最大4、使い捨てPG最大1・memory1 GiB／CPU1／`max_connections=16`、Ryuk最大2・DB client14、開始直前の空きmemory8 GiB／disk10 GiB、raw log100 MiB／run領域2 GiB／子log10 MiB、子起動120秒・通常終了30秒・所有子force5秒を上限とする。全forkの`java.io.tmpdir`には今回専用のcanonical `tmp/r3b-root-preflight-20261014/jvm-temp`を渡し、fresh XML内のpropertyと照合する。Docker／Rancherの再起動、他session process操作、依存取得、source訂正・再compileのための別実行、async再packageは含めない。

実行直前にbranch／HEAD、未commit差分、`git diff --check`、Reference source／固定通常・async JAR hash、§19の4 XML hash、JDK21／Wrapper／offline plugin、Docker Server／必要image／稼働container0、保護process、空き資源、canonical JVM tempを再照合する。Maven終了後は**root reactor所属だけ**のfresh Surefire／Failsafe XMLを直ちに別保存し、path・case・実行／skip・failure／error・個別SHA-256のindexを作る。残存Walking Skeleton等の非reactor旧XMLは集計に混ぜない。通常JARも固定通常／async JAR対とは別のartifact epochで保存・hash固定し、固定通常JARとの`BOOT-INF/classes`および共通runtime依存のentry内容を比較する。外側JARのbyte hash一致は要求しない。通常無効・専用skipの分離、所有container／JVM終了、保護process生存を確認する。failure／error、想定外の通常入口露出、source／artifact差、資源・時間・cleanup不成立でSTOPし、追加修正や再実行を自動開始しない。実行中の連続最大値は有限sampleから推定しない。

開始前の静的点検は[検証記録§20](../architecture/validation/phase4-s1-r3b-source-preflight-and-blocking-review-20261014.md#20-通常rootの静的点検と次回判断入力2026-10-14)に置く。**本§11は新規枠の判断案であり、通常rootのMaven／Docker実行は、Ownerがこの30分・1 invocationとB03a未達を残す目的を確認した後に開始する。** root結果と選択caseをepoch別に点検してから、R3-Bを限定結果として受け入れるかを別判断する。local commit／push／PR／mergeも別判断を維持する。

## 12. 通常rootのOwner開始承認と結果入口（2026-10-14）

Ownerは§11の**新規30分・通常root offline `clean package`1回・再実行0回**を確認して実行開始を承認した。§11末尾の「開始未承認」は承認前の履歴である。B03a自動完了FAILを残し、今回のrootは通常構成保護だけを検証する。実際のpreflight、root結果、JAR対照、XML／log保全、cleanupは[検証記録§21](../architecture/validation/phase4-s1-r3b-source-preflight-and-blocking-review-20261014.md#21-通常rootの新規限定実行結果2026-10-14)に固定する。R3-B限定結果のOwner受入、local commit・remote／PR／mergeは、このroot実行承認には含めない。
