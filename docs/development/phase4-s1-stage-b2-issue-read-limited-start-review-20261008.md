# Phase 4 S1 B-2初回 issue／read：具体差分・条件付き限定開始票案（2026-10-08）

状態：`OWNER APPROVED — 条件付き限定開始（§13）`。方式・初回操作範囲は[方式review§9](phase4-s1-stage-b2-use-boundary-review-draft-20261008.md#9-方式初回操作範囲のowner承認2026-10-08)でOwner承認済み。承認文書commit・clean source固定とpreflight成立後に、本票内の作成・検証へ進む。

## 1. 開始対象と除外

初回は、Tooling所有の隔離供給元を不変にした期間内で、Reference-owned Adapterから対象を読み、既存Referenceの保存・実Identity／現在能力・管理scope／TTL・Business／Security Auditへissue／readだけを接続する。issueは再送可・実行許可・配送成功の判定ではない。

consume／close用RecoveryEvidencePortは既定emptyのまま。肯定consume／close、人の歴史対象閉鎖、別publication横断抑止、送信・復旧runner・worker認証／委譲・通常listenerのReference接続、Reference Level 2、Framework API／Rules／依存・POM／Root Reactor／CI・remote・正式受渡し／DoD・Phase 4全体は含めない。

branchは`feature/phase4-s1-reference-foundation`。本票作成時のHEADは`23379b352bfb9ef261789290a3acf40a6805c7e6`、B1受入sourceは未commitだった。後続の固定結果は§12を参照する。B2実開始基点は§9の開始承認・文書固定後に別途記録し、今回の文書コミットを実開始承認とは扱わない。

## 2. 供給元とprocess／DB構成

```text
Maven → Reference test fork
          → Tooling test-only coordinator JVM
               → 通常Tooling子JVM（準備中だけ、同時1）
               → PostgreSQL container 1個
                    source DB：既存Tooling publication／provider＋B1／B2 test schema
                    Reference DB：既存Framework／Reference migrationと隔離grant

通常Tooling子の終了・writer遮断・接続終了 → frozen供給契約を確定
Reference ProtectedRecoveryIssueService
  → RecoveryIssueProtectionPortで保護scopeを開始
  → RecoveryPermitService.issue（既存transaction、実Identity／保存／Audit）
      → 保護scope必須のJdbcProtectedRecoveryTargetAdapter
  → commit／rollback確定までscopeを保持
read → 既存RecoveryPermitService.read
```

別Maven artifactのJava型は共有しない。Reference test helperがcompile済みToolingのtest classpathを用いてcoordinatorを別JVMで起動する。Tooling側にReferenceのcompile依存は追加しない。Reference mainはTooling class／内部tableへ依存せず、供給元所有のビューをJDK JDBCで読む。

coordinatorは管理用processとして残るが、凍結後に通常子を起動・イベント発行・source更新する入口を閉じる。終了対象の「全当該process」はpublication／providerを変更できる通常子であり、読み取り専用のReference test forkと管理coordinatorまで停止済みとは表現しない。

## 3. 保護手順と供給protocol v1

### 3.1 fixture準備・凍結

1. 使い捨てPG17を起動し、source DBへ既存Tooling V1／V2とB1 SQL、B2専用SQLを適用する。既存SQLは変更しない。manifestのevent／通知key／非秘密payload・宛先識別を先に固定する。
2. B1既存process harnessで通常子を1個起動し、fixtureイベント1件を作る。初回肯定対象は静止した未受理fixtureに限る。stub受理のある有限負例も準備する。実providerへ通信しない。
3. 当該子を終了・終了待機・stream closeし、run／起動世代／JAR hash／source対応を記録する。強制終了とdrainを区別し、実providerの遅延要求不在は保証しない。
4. snapshotを採取する。source DBの`b1_fixture`／`b1_writer`とB2準備用writerをNOLOGINとし、通常writerのDML／CREATE／sequence権限とDB CONNECTを取り消す。source DBのPUBLIC CONNECTも取り消し、b2_readerだけへ明示grantする。ここで権限変更を先にcommitしてから次の接続確認へ進む。B1の`b1_fixture`はtable ownerなので、NOLOGINと全既存接続終了が不可欠。権限取消だけでowner／進行中処理を止めたとは扱わない。
5. source DBの既存接続はreaderと、凍結処理中の当該setup接続だけであることをcatalogから確認する。未知接続・writer接続・未終了transactionがあれば拒否する。当該子終了後も残留する接続を無断terminateしない。全writerのrole membership・superuser／作成権限・ログイン可否・table ownershipを検査する。
6. 通常起動入口の閉鎖と権限変更commit後、元row／snapshotを再照合する。変更があれば既存snapshotのまま凍結証明を作らず拒否する。次の当該setup transactionで凍結証明を一度だけ登録し、commit後setup接続を閉じる。管理接続をsource DBへ保持しない。凍結後にwriter権限／LOGINを戻さず、source DBは当該container破棄まで凍結を維持する。

通常子の再起動要求はcoordinatorが拒否し、直接起動してもNOLOGINのfixture credentialでsource DBへ接続できない。coordinatorの状態変更・起動入口は単一所有で直列化し、肯定区間に管理SQLを実行しない。DB管理者・同OS利用者による任意変更、Tooling外の本番process／遅延送信・分散基盤への防御は保証外である。

PostgreSQLの[role変更](https://www.postgresql.org/docs/17/sql-alterrole.html)と[所有者の権限](https://www.postgresql.org/docs/17/ddl-priv.html)を踏まえ、NOLOGIN・membership・接続終了と権限検査を組み合わせる。ビューは[CREATE VIEWの権限境界](https://www.postgresql.org/docs/17/sql-createview.html)に従い供給元が所有し、readerに基底table権限を渡さない。

### 3.2 ビューと照合

新規test SQL `s1-b2/frozen-source.sql`が以下をsupplier-owned契約として定義する。Referenceに元publication tableのqueryを持たせない。

| 対象 | 契約 |
|---|---|
| `b2_source.freeze` | runにつき一意のimmutable証明。protocol_version=1、run、environment、publication／event／event_type／listener／attempt、source_id、Tooling JAR SHA-256、採用revision／fingerprint、通知key・payload／宛先識別、通常子最終generation、writer遮断・終了確認、freeze時刻、admit_untilを保存 |
| `b2_read.frozen_target` | 上記とB1 supplier-owned viewの現値を同じSELECTで返す。現revision／tuple／fingerprint／binding／provider分類が一致し、凍結済みroleのLOGIN／権限に矛盾がないかの非秘密booleanを併記。writer／管理者のcredential・raw payloadは返さない |
| `b2_reader` | source DB CONNECT＋`b2_read` USAGE／view SELECTだけ。NOSUPERUSER／NOCREATEDB／NOCREATEROLE／NOINHERIT、membership0、raw table／DML／CREATE拒否 |

Referenceは`run + environment + publication`で0／1行だけ取得する。version、全tuple、expected source／JAR hash／revision、binding、generation、writer遮断、現値との一致を厳密比較する。event JSONの厳密復元は供給側で実施し、ReferenceにTooling event型を追加しない。返却値は最終RecoveryTargetと保護scope内の照合情報だけ。

providerがACCEPTED／UNKNOWN、矛盾、受理行混同、binding／source／世代差異、行欠落・重複、未凍結、未来時刻、admission期限一致、取得不能ならissueを拒否する。`FIXTURE_NOT_ACCEPTED`は全関連子終了後の限定fixture分類に限る。FAILED・停止file・advisory lockだけで適格にしない。

### 3.3 transaction全期間の扱い

Reference-owned `RecoveryIssueProtectionPort`はtransaction外で`open(target)`し、同threadの排他的scopeを保持する。scopeのcurrent確認を使ってtarget Adapterが肯定する。scope外で既存Service.issueを直接呼んでも、実Adapterはemptyを返す。scopeの共有・nested open・他thread使用・close後利用を拒否する。

`ProtectedRecoveryIssueService.issue`はこのscope内で既存Service.issueを呼ぶ。既存Serviceは自身のTransactionTemplateでcommit／rollbackを完了してから戻るため、外側scopeはその確定まで解放しない。例外は自動retryせず、commit不明として当該Reference DBのpermit／Audit照会・HOLDへ渡す。単に例外が返ったことから未保存と推定しない。

freeze証明の`admit_until`は新しい操作の受付期限（freeze後60秒）であり、進行中transactionの保護解除期限ではない。snapshotの期限・key保持10分はopen時とtarget再検査時に確認する。凍結は期限後もcontainer破棄まで維持されるため、時計進行・読取接続喪失でwriter権限を再開しない。scope close後もDBの凍結は解かない。

証拠の時刻をcommit時点で再検査した保証、全I/Oの強制停止保証、動的供給元のlease／fencingとしては扱わない。permitのTTL／能力／scope再検査は既存Serviceの契約を維持し、後続consumeでは使用時に別途確認する。凍結の持続がこの限定構成で実証できなければ、本方式の肯定issueは開始しない。

## 4. 作成・変更pathの上限

以下は本票で採用する候補名。今回sourceを作成しない。

| 区分 | path（Repository rootから）／責務 |
|---|---|
| Reference main新規1 | `koiki-reference-app/src/main/java/org/koikifw/reference/notification/application/port/outbound/RecoveryIssueProtectionPort.java`：open／AutoCloseable scope、同thread現在対象照合の契約。Framework Public APIへ昇格しない |
| Reference main新規2 | 同module `application/ProtectedRecoveryIssueService.java`：transaction外の保護scopeと既存issue呼出しの調整。readは既存Serviceへ委譲。新しいtransaction／権限の迂回なし |
| Reference main新規3 | 同module `adapter/outbound/operational/JdbcProtectedRecoveryTargetAdapter.java`：両Port実装、view SELECT・厳密照合・scope管理。raw table／Tooling型参照なし |
| Reference main新規4 | 同module `adapter/outbound/configuration/FrozenRecoverySourceSettings.java`：有限接続設定の起動固定・妥当性検査。secretをtoString／logに含めない |
| Reference main新規5 | 同module `configuration/FrozenRecoverySourceConfiguration.java`：明示mode時だけ同じAdapterを両Portへ登録。evidence肯定Beanなし |
| Reference main変更1 | `configuration/NotificationFoundationConfiguration.java`：上記configurationのImportだけ。Service／既存Port／Repository／Domain・Audit・Security契約は変更しない |
| Tooling test新規2 | `build-support/phase4-level2-verification/src/test/java/org/koikifw/buildsupport/phase4/b2fixture/B2FrozenSourceCoordinator.java`、`B2FrozenSourceProtocol.java`：prepare→freeze→ready→finish、一意証明・ビューの準備・有限観測。coordinatorにtest-only main entryを含む |
| Tooling SQL新規1 | `build-support/phase4-level2-verification/src/test/resources/s1-b2/frozen-source.sql`：§3の追加schema／role／viewとimmutable制約。Flyway登録なし |
| Tooling test新規2 | 同test基点 `B2SourceFreezeTest.java`、`B2FrozenSourceProtocolTest.java`：§6の18件 |
| Reference test補助新規2 | `koiki-reference-app/src/test/java/org/koikifw/referenceacceptance/notification/B2ReadConnectionHarness.java`、`B2FrozenSourceProcess.java`：source子の有界管理、同container内Reference DB、実Identity・scope設定・Audit、sanitized結果／cleanup |
| Reference test新規4 | `koiki-reference-app/src/test/java/org/koikifw/reference/notification/B2FrozenSourceRegistrationTest.java`、`B2ProtectedIssueReadTest.java`、`B2IssueBoundaryTest.java`、`B2IssueTransactionTest.java`：§6の36件 |
| 検証script新規1 | `build-support/phase4-level2-verification/scripts/verify-s1-b2.ps1`：compile／JAR／classpath固定、明示集合逐次・資源／時間監視、source hash・有限sanitized raw。Git／download／任意cleanupなし |
| Evidence新規1 | `docs/architecture/validation/phase4-s1-stage-b2-issue-read-20261008.md`：case／source／実効制限・未達・raw・cleanup・結果レビュー |
| 導線既存変更1 | `build-support/phase4-level2-verification/README.md`：B2限定入口・非配布・限界への導線 |

新規18件（main5、Tooling補助2＋SQL1＋test2、Reference補助2＋test4、script1、Evidence1）＋既存2件＝20件を上限候補とする。承認文書・AGENTS承認導線は別。補助追加が必要なら先に差分reviewし、無断で枠を増やさない。

既存B1 source／SQL／assertion、既存Reference acceptance harness／test、RecoveryPermitService／既存Port、migration／通常properties、POM／依存・Framework・CIは変更しない。既存harnessの方式は新補助から参考にし、既存有限肯定Portを実接続testの代用にしない。

coordinator起動時に親test helperが生成したrun別secretをENVで渡し、Tooling側がb2_readerを作る。readyは所有run directoryの最大64 KiBの有限manifestにprotocol／run／port／DB名／tuple／hash／revisionだけを返す。Reference helperは期待runと127.0.0.1・portを照合し、ENVのsecretと組み合わせる。readyにpassword・JDBC URLを保存しない。Reference DB準備のadmin secretもENV供給し、source凍結後のadmin SQLはReference DB内のfixture準備・X01だけに限定する。

Tooling coordinatorのworking directoryは当該Tooling moduleに固定する。test classpathはoffline compileしたclasses／test-classesと既存依存JARの明示manifestから構成し、任意path・未知JARを追加しない。Reference test helperは別process境界のmanifestとENVだけを使い、Tooling classをimportしない。

## 5. 新しいReference構成の明示設定

prefixは`koiki.reference.notification.frozen-source.`。modeは`disabled`（既定）／`tooling-jdbc-v1`だけ。foundation.enabled=trueかつ明示mode時に登録する。未知mode・設定欠落・不正は起動失敗、disabled時は従来のempty target／evidenceと通常無効を維持する。

| 設定 | 意味／制限 |
|---|---|
| `mode` | 前述2値。scope／TTLの既存managed-config.mode=fileは独立して指定する |
| `environment-id`、`run-id`、`source-id`、`expected-jar-sha256`、`expected-revision` | run固定の非秘密期待値。任意環境・全publicationへのfallbackなし |
| `B2_SOURCE_JDBC_URL`、`B2_SOURCE_READER_USERNAME`、`B2_SOURCE_READER_PASSWORD` | ENVだけで供給。hostは127.0.0.1、動的port、当該source DB限定。username=b2_reader。URLの任意propertyを許可せず、connectTimeout／socketTimeout=10秒をAdapter側で固定 |

Reference datasourceは別Reference DB・既存の隔離権限role。source用DataSourceをprimaryとして登録しない。Reader接続はscopeにつき最大1、Statement timeout10秒、read-only transaction。scope外に接続を保持せず、エラーは固定分類だけでcredential／URL／SQLcauseを出力しない。JDBC driverは既存runtime依存を使用し追加依存なし。

Reference main5件はReference JARに含まれる意図した限定Adapter差分であり、「B2 source全部がJAR非混入」とは説明しない。非混入検査の対象はTooling型／SQL・test harness・fixture値・secret。通常起動で無効であることを別に確認する。

## 6. 新規6 class／54 invocation

各IDをJUnitの1 method／1 invocationに対応させる。有限枝はcoverage候補で、完全性の認定ではない。

| class／件数 | 固定する枝 |
|---|---|
| Tooling B2SourceFreezeTest：F01〜F09（9） | F01準備→全通常子終了→凍結、F02生存子拒否、F03writer接続残留拒否、F04未知source接続拒否、F05NOLOGINで再接続拒否、F06readerのsource DML／CREATE拒否、F07凍結後通常子再起動要求拒否、F08fixture owner・membership／権限異常拒否、F09凍結失敗でreadyなし・cleanup |
| Tooling B2FrozenSourceProtocolTest：P01〜P09（9） | P01一意正常ビュー、P02version／run／source差異、P03tuple差異、P04revision／fingerprint差異、P05binding差異、P06ACCEPTED／UNKNOWN拒否、P07未来／期限一致拒否、P08証明UPDATE／DELETE拒否、P09reader raw table拒否・取得不能 |
| Reference B2FrozenSourceRegistrationTest：C01〜C08（8） | C01既定無効、C02明示mode両Port同一Adapter・evidence empty、C03未知mode、C04欠落設定、C05不正loopback／credential供給、C06foundation無効、C07managed scope／TTL独立・通常構成維持、C08fixture／secret非露出 |
| Reference B2ProtectedIssueReadTest：I01〜I12（12） | I01実issue保存＋Audit＋read、I02ISSUE能力なし、I03READ能力なし、I04Identity無効、I05scope外、I06scope供給不明、I07TTL不明／失効、I08別tuple、I09source／binding差異、I10UNKNOWN、I11ACCEPTED、I12取得不能。拒否時にpermit／消費／閉鎖記録が増えないこと |
| Reference B2IssueBoundaryTest：B01〜B08（8） | B01直接Service.issueはscopeなしで拒否、B02他thread／nested scope拒否、B03最終照合後の通常source更新拒否、B04最終照合後の通常子再起動拒否、B05commit完了までscope保持、B06rollback完了までscope保持、B07受付期限超で新issue拒否・進行中の凍結は継続、B08読取接続／呼出し失敗でもwriter再開なし |
| Reference B2IssueTransactionTest：X01〜X08（8） | X01Business Audit権限一時REVOKE時rollback・finally復元、X02同環境／publication競合、X03呼出し元transaction拒否、X04消費未接続拒否、X05閉鎖未接続拒否、X06否定時Security Audit・保存不変、X07commit後応答喪失のtest-only注入と照会HOLD・retryなし、X08異常終了／未閉鎖permit保持→全当該resource cleanup |

54＝9＋9＋8＋12＋8＋8。X07は応答喪失後の結果照会手順の実証で、DB commit自体の全障害パターンを再現したとは扱わない。X01は当該使い捨てReference DB内だけ、既存SQL／grant定義は変更せず復元確認を必須とする。

保護の負例は通常権限／入口に対する操作で確認する。肯定区間中にDB管理者からsourceを強制更新して「検出できたから保護PASS」としない。期限・競合を検査するtest-only hookは新補助／新class内に限り、mainに失敗switchを入れない。

## 7. 回帰・package・実行順

1. offline compileと既存Tooling jdbc JARのpackage／hash・classpath固定。Tooling test compileは`jdbc,s1-contract,s1-web`、runtimeは`jdbc`単独。B1と同じprofile訂正を保持する。
2. Tooling新規18件で凍結・writer遮断・起動拒否を確認。成立しなければReference肯定testへ進まない。
3. Reference構成8件・新規保存／境界／transaction28件を順に実行。同時container1、class逐次。既存test harness条件をCLIの全体pool overrideで上書きしない。
4. B1新規52件と選択PL2回帰6件。既存PublicationRecoveryTest4件とProcessCrashRecoveryITの採用済み2 methodだけ。B1の未保護観測I05はそのままPASSの意味を保持する。
5. Reference関連143件、既存25 class／99件＋clean-package済みReference E2E1件。Architecture2件は99件内。既存件数とbaselineのclass一覧を開始前にscriptへ固定し、全PL2 suiteへ拡張しない。
6. Tooling JARはB1／B2 helper・SQL・test0、Reference JARはTooling型・B2 acceptance helper・test SQL・secret0を確認。新Reference Adapter5件の包含と通常無効を記録する。

期待集合は新規54＋回帰301＝355 invocation。rawはrun別に新規作成し、古いXMLや過去PASSの流用なし。Source hash、JAR／classpath hash、class／case／command、resource sample、有限sanitized XML／log、cleanupを対応付ける。

## 8. 資源・時間・作業量・再実行上限

今回の数値は新しいB2上限候補。方式承認から自動で許可された数値ではない。

| 項目 | 候補上限／開始条件 |
|---|---|
| preflight | available memory8 GiB／disk10 GiB以上、JDK21／Wrapper／PG17・Ryuk・Chromium cache・offline artifact。新download／installなし |
| JVM | 各heap768 MiB、JUnit並列無効、fork1／reuseForks=false。新接続testはMaven＋Reference fork＋coordinator＋準備中通常子＝最大4 JVM、準備後3。Tooling単体testは最大3。coordinator＋通常子同時2を新規副作用として明示 |
| DB | 同時PG container1、memory1 GiB／CPU1／max_connections16、source／Reference DBは別。各lock／statement／transaction timeout10秒。実SHOWとcontainer設定を確認 |
| 接続 | 新testのReference pool最大2／idle0、source reader最大1、準備中通常子pool2／idle0、setup管理最大2、競合testの第2Reference pool最大2。phaseを分け同時総接続8以下。既存E2Eは採用済みpool4／idle1 |
| 有界待機 | pool／connect／socket／Statement10秒、子起動60秒、各子終了10秒、Future／latch10秒、coordinator ready待機90秒。timeout後は当該process終了を確認し、全I/O自動停止とは扱わない |
| fixture時間 | admission60秒、key保持10分。open／currentで時刻比較、期限一致拒否。進行中freezeは期限で解放しない。Clock注入と実UTCの結果を区別し、時計差1秒超拒否の有限枝をP07に含める |
| B2検証総枠 | 新規54件45分、回帰301件30分、compile／package／raw保全10分、最終cleanup5分、合計90分。class単独10分も適用し、先に到達する枠で停止。各class cleanupは各集合枠に含める |
| 累積の保守的再評価 | B1暫定約100分に未計測／見積り差50分を加え、管理上150分消費・90分残と置く。B2全90分を加えて240分。実測済み認定ではなく、実開始前の再評価で残90分未満なら全集合開始を停止し、予算差分をreviewする |
| 作業量 | B1約6〜7時間を不確かさ込み9標準時間として管理。B2作成／検証12〜18標準時間候補、B2開始後8時間で進捗確認、18時間で停止。累積最大27時間として既存28時間停止を越えない。Owner待ちは別枠 |
| raw／rerun | B1保全分を含む全run1 GiB以内、新規B2 raw256 MiB以内。失敗保全後に停止。同原因・同条件の再実行はOwner判断後最大1回、原因修正は別review。未計測反復で時間枠を広げない |
| cleanup | classごとにcoordinator・通常子・接続／pool・Executor・当該DB／Ryuk・port・一時ENV／秘密を確認。子終了／stream close後、既知run directory内の既知fileだけを有界削除。未知残留は停止 |

予算の50分・9標準時間は保守的な管理仮定であり、過去実時間の確定ではない。各集合前に消費＋当該集合枠＋残る必須集合枠＋cleanup余裕を計算する。新規54件で45分使い切った場合など、残必須集合の枠が確保できなければ次集合を開始しない。cleanup用時間を通常検証へ振り替えない。

## 9. source固定・preflight・停止と戻し方

**実開始前の条件：** 本票の具体main／test／SQL／構成・有限副作用・担当／操作委任・355件・上限をOwnerが個別承認する。承認をAGENTS正本と本票へ反映する。Owner操作または事前確認でB1受入成果と承認済み文書をcommitし、clean sourceを固定する。その後source／環境差分、artifact／classpath、接続先／role・通常経路隔離、資源・予算・cleanupのpreflightを実施する。

保護Port／outer service／構成の必要差分は本票の新規5 main＋既存Import1件に限定する。既存Service／Port・Security・migration等の変更が必要なら、条件未成立として追加作成・肯定testを停止し、根拠と最小差分を提出する。同じ採用方式を再要求せず、具体差分・上限の変更だけを判断する。

副Agentは使用しない。Docker named pipe／Maven書込み等の権限拒否は環境が提供する権限付き承認手順に従い、拒否を迂回しない。既存常設container／無関係processを停止しない。

**停止条件：** source差分不明、未知writer／接続／process、NOLOGIN／凍結／scope不成立、通常更新／再起動が通る、UNKNOWN肯定、secret出力、既存回帰失敗、追加path／schema／依存変更が必要、資源・時間・作業量／raw上限、cleanup不成立。

**戻し方：** 新しいissueを止め、確定不明をHOLDとしてpermit／Audit照会・有限sanitized結果を保全する。当該子・scope・pool・DBのみを終了し、source writerを再開しない。未閉鎖permit・消費・Auditの任意UPDATE／DELETE・未承認close・down migration／Git resetを行わない。最後の使い捨てcontainer破棄は承認されたtest cleanupで、本番の解決手順ではない。

## 10. Owner判断欄

本欄提出時は`未判断`。後続の§13で、§2〜§9の具体差分・操作委任・検証集合・予算・停止／cleanup条件をOwner承認済み。

Ownerが限定環境管理責任を兼務し、文書commit・clean source固定とpreflight成立後の本票内作成・Maven／隔離Docker・当該子JVM／fixture操作をAgentへ委任する条件付き開始案。実装・検証結果の受入は別判断。今回の方式採用承認だけでは本票の開始を実行しない。

今回の作業は承認記録と開発文書の作成のみ。code／SQL・Maven／Docker・process・Git add／commit／pushは未実行。

## 11. コミット対象の整理

コミット前のB1差分は新規test6＋補助5＋SQL1、README・Evidence・開始票の更新3＝15件。既存の次セッション引継ぎ文書1件は当時のsource／残作業を示す履歴として保存する案。合計16件をB1成果のコミット候補とした。履歴の「未作成・未実行」を現在状態へ書き換えず、現在状態はEvidence§24を優先する。

B2文書の別コミット候補は本票、方式reviewの承認記録、段階B契約の次工程導線。限定開始承認後はAGENTS導線と本票の承認記録も含める。commit message候補はB1 `test: complete approved S1 B1 read connection validation`、B2文書 `docs: define S1 B2 protected issue-read start conditions`。

tmp／targetのraw・scriptはこのコミットに含まれない。所在とhashの記録を保持する。B2検証scriptをRepositoryに置くことと、B1実行script／rawを長期・別端末再現向けに固定する作業は別であり、後者を完了済みとしない。

## 12. 2コミット操作のOwner承認とB1 source固定（2026-10-08）

Ownerは「この2コミットのローカル操作を承認します。対応お願いします」と明示した。§11のB1成果16件とB2文書3件の分割を採用し、Agentのlocal add／commitを許可した。remote操作・B2実装／検証開始の許可ではない。§10は開始判断待ちを維持する。

B1の16件はcommit `6af0d3eae279c38c0faa5541f2bbfd5bde357dcd`（`test: complete approved S1 B1 read connection validation`）で固定した。登録前に検証時のsource hash台帳13件との一致、登録差分16件とdiff checkを確認した。検証の再実行・code変更は行っていない。

B2文書3件は本票・方式review・段階B契約の導線を別コミットする。今回の操作ではB2開始承認のAGENTS導線を追加せず、開始票の未判断状態を保持する。コミット完了後にGit履歴と作業ツリーのcleanを確認する。raw／scriptの端末内保管は継続する。

## 13. Owner承認：条件付き限定開始（2026-10-08）

Ownerは「B2条件付き限定開始票案 を開始します」と明示した。本票§2〜§9の具体差分、新規18件＋既存2件、fixture準備・凍結・当該子JVM管理・隔離DB操作、新規54件＋回帰301件、資源・累積予算／作業量・raw／再実行上限・停止／cleanup条件を採用し、条件付き限定開始を承認した。

Ownerが限定環境管理責任を兼務し、承認文書commit・clean source固定後のpreflightと、成立時の本票内作成・Maven／隔離Docker・当該coordinator／通常子JVM・fixture操作をAgentへ委任する。事前のsourceはbranch `feature/phase4-s1-reference-foundation`、clean HEAD `66bee695719db3c82f2ec2002dbd1703625e6ef2`で確認した。開始承認記録を含む固定後commitは別途記録する。

肯定consume／close・実外部送信／復旧runner／worker委譲・Reference Level 2、Framework／POM／依存／CI／remote・実運用／正式受渡し／DoD／Phase 4全体への承認ではない。保証限界・網羅性懸念・D11／D12の後続対象を保持し、結果受入は別判断とする。同じ限定開始範囲の承認を再要求しない。

今回の反映は本票とAGENTSの承認記録だけ。local commitはOwner操作または操作前確認を維持する。直前の2コミット操作承認は完了した16件＋3件の操作に対応するため、今回の承認記録2ファイルの追加commitは個別操作確認を経る。文書commit・clean source固定前にcode／SQL作成・Maven／Docker・子JVM操作を先行しない。

## 14. script総件数集計の最小訂正案（2026-10-08）

Ownerが§13の承認記録2ファイルの追加commitを許可し、`03210795a8ffd146d0bcc10c9d1da4d62a69308c`へ固定、clean sourceからpreflightを実施した。foundation preflight・Tooling新規18件のclass別PASS／cleanupは[Evidence§1〜§4](../architecture/validation/phase4-s1-stage-b2-issue-read-20261008.md)に記録する。

scriptの`OrderedDictionary`配列をMeasure-Objectで総件数集計したため、class別9＋9・failure／error／skip0にもかかわらず、summaryのCasesがnullとなった。18件gateは未成立として停止した。Reference main5件＋Import1件のcompileは成功したが、Reference肯定test・36件・回帰301件は未実行。

**最小訂正候補：** 対象は承認済み新規script `build-support/phase4-level2-verification/scripts/verify-s1-b2.ps1`の集計・gate読込だけ。

1. `$results`の各Casesを整数として明示加算し、期待class数・各期待件数・fresh XML・failure／error／skip・exit・cleanup／source不変を満たす時だけPASS summaryを作る。数字18を固定して書き込む方式にはしない。
2. 現在の元`summary.json`とclass別rawは保持し、saved XML／JSON・source hashを再照合した`gate-summary-corrected.json`を別生成する。元summaryのpath／hash、訂正理由・期待class／件数・使用sourceを対応付ける。
3. Reference stageは訂正gateを優先し、訂正前のnull summaryへ肯定fallbackしない。訂正前後でTooling helper／test／SQLのhashが同一であることを確認する。scriptの承認済み集計差分だけは新hashとして明記する。
4. 限定確認は9＋9の整数集計、欠落／不一致／failure／cleanup false等でPASSにしない負例と、保存済み実XML／JSONとの照合に限る。JUnit／Dockerの18件再実行は含めない。

業務source／assertion・件数、JDBC／role／保護方式、資源・時間・raw／再実行上限は変更しない。成立時に残るReference helper／testを作成し、開始票の残予算再評価・実効制限確認後に既承認の36件・回帰301件へ進む。追加原因探索・別修正・結果受入・remoteへ拡張しない。

**Owner判断待ち。** §8の「失敗保全後に停止」「原因修正は別review」に従う訂正案であり、同じ限定開始承認を再要求するものではない。今回のscript修正・訂正gate生成・限定確認はまだ実行していない。

## 15. §14のOwner承認と訂正gate成立（2026-10-08）

Ownerは「script総件数集計の最小訂正案 を承認します。」と明示した。§14のscript集計・gate読込、保存済みXML／JSON／source hash照合と有限確認を採用し、Agentの実施を許可した。Tooling18件のJUnit／Docker再実行、業務source／assertion変更、検証上限拡張は含めない。

明示整数加算と期待集合・結果フラグ検査を反映し、元summaryを保持した`gate-summary-corrected.json`を別生成した。保存済みXMLとclass／group JSONの9＋9、failure／error／skip0、exit0、fresh／cleanup／source成立を再照合。Tooling helper／test／SQL5件は実行時hashと同一、scriptだけ承認済み訂正hashを記録した。正常集計と13拒否枝の14確認、および実行時と同じOrderedDictionary入力の18件集計がPASS。再実行なし。

訂正gateはTooling18件の成立だけを示す。残るReference36件・回帰301件とpackage／通常無効・cleanupは未達であり、結果受入ではない。成立後は§14のとおり残予算再評価を行い、既承認範囲へ進む。local commit／remoteは今回実施しない。

## 16. Reference新規testのnull代入に関する最小訂正案（2026-10-08）

§15成立後、記録済み小計130.4804166秒へ未計測確認処理5分を管理上見込み、B2残約82.8分とした。Reference新規検証30分・回帰30分・残compile／package等8分・最終cleanup5分、計73分を確保する下方配分で作成を再開した。Owner待ちは別枠。既承認の90分／各集合上限を広げていない。

Reference補助2件と構成登録classを作成しoffline compile SUCCESS（10.946秒）。構成登録C01〜C08は8件PASS（cleanup込み6.7946706秒）。Tooling source5件は訂正gateから不変。接続／issue28件のclass3件を追加し、compile時に新規testの`h=null`代入3か所がNullAwayに拒否された（10.314秒）。当該28件・回帰301件は実行していない。rawを保存し、§8に従い修正・再実行を停止した。

**具体的な最小訂正候補：** `B2ProtectedIssueReadTest.java`と`B2IssueTransactionTest.java`だけ。nullable契約・main／既存test／compiler設定は変更しない。

- I10／I11の既存fixture終了→負例fixture切替では、`h=null`を廃止し、test所有のbooleanで終了済み状態を記録する。新fixture構築成功後だけbooleanを未終了へ戻し、AfterEachは未終了のfixtureだけ終了する。切替途中の構築失敗は新補助のconstructor cleanupを維持する。
- X08はfixture終了後の`h=null`をbooleanの終了済み記録へ置き換え、AfterEachで二重終了しない。未閉鎖permit保存確認→使い捨てDB破棄の順序を維持する。

期待件数・assertion・source保護・認可／Audit・資源／cleanup／時間上限は維持する。限定確認はoffline test-compile1回と、成立時の未実行28件・既承認回帰301件への再開。構成登録8件とTooling18件を再実行しない。compileログには既存test2件の既知warningも表示されているが、そのsourceは変更対象に含めない。

**Owner判断待ち。** 訂正は未反映。追加失敗・別原因・既存source修正・上限超では再停止し、結果受入・remoteへ拡張しない。[Evidence§7〜§8](../architecture/validation/phase4-s1-stage-b2-issue-read-20261008.md#7-reference構成登録8件と残予算)に実結果と保全先を記録する。

## 17. §16最小訂正のOwner承認（2026-10-08）

Ownerは「16. Reference新規testのnull代入に関する最小訂正案 を承認します」と明示した。§16の新規test2ファイルに限る終了状態boolean化、offline test-compile1回、成立時の未実行28件・既承認回帰301件への再開を許可した。Tooling18件／構成登録8件の再実行、main／既存test／compiler設定の追加変更は含めない。

I10／I11は旧fixture終了後にclosed=true、新fixture構築成功後にfalseとし、X08は終了後にtrueを記録する。AfterEachは未終了のfixtureだけを終了する。null代入3か所を廃止し、assertion・件数・cleanup順序を維持した。失敗保全と上限・停止条件は継続し、結果受入・local commit／remoteへ拡張しない。

限定compile1回はFAILURE。新規testのnull代入3か所の診断は消えたが、既存`ExpenseApiControllerTest.java:75`が`ExpenseRequestDetail`のnull引数で拒否された。追加原因が判明したため、§16／§8の条件に従って28件・回帰301件を開始せず停止した。既存test／mainは訂正していない。詳細は[Evidence§9](../architecture/validation/phase4-s1-stage-b2-issue-read-20261008.md#9-16訂正後の限定compileと既存class差分)へ保全する。

## 18. 既存classの注釈差分に対する隔離compile確認案（2026-10-08）

読み取り照合で、既存record sourceは`decisionReason`に`@Nullable`を持ち、既存testのnull期待はHEADから不変と確認した。B1受入済みReference JARの当該constructorには`METHOD_FORMAL_PARAMETER, param_index=8`のNullable注釈があり、現`target/classes`のconstructorにはない。現classのfield／accessorにはNullableが残る。JARは受入時hashのまま。現classを生成したcompiler／processはUNKNOWNで、IDEが原因と確定しない。

**最小確認候補（業務source修正なし）：** 同一sourceからの生成差か、現在の出力混在による差かを切り分ける有限確認だけ。

1. 現class／javap結果／失敗compile／受入JAR比較を保全し、既存`target`を削除・上書きしない。IDE／無関係processを停止せず、IDE設定・POM・compiler optionも変更しない。
2. 未作成の`tmp/b2-isolated-compile-0321079-20261008/`へReferenceのpom／main・test source／resourceとParent POMだけをコピーする。元・コピーを全件SHA-256照合し、copy sourceへ訂正を加えない。Root Reactorへmoduleを追加せず、ConsumerへのJARコピー／配布は行わない。
3. コピーしたReference POMに対して、同じWrapper・JDK21・offline・ParentのError Prone／NullAway条件・heap768 MiBで`-DskipTests test-compile`を1回だけ実行する。新出力はtmp配下。Docker／子coordinator／JUnitを実行せず、既存testを除外しない。10分単独上限と残compile／raw枠の先着を適用する。
4. copy source／POMの実行前後hash、生成recordのconstructor注釈、52 test sourceのcompile結果を記録する。既存targetのhash変化は観測だけとし、未知writerを停止しない。FAIL／source差分／注釈欠落／上限超なら停止し、反復・source修正を先行しない。

今回の案は隔離compile1回の確認まで。成立しても、28件・回帰301件の実行がまだ必要であり、訂正classの受入・既存targetへのコピー戻し・隔離出力を使うruntime command／packageへの切替を自動実行しない。必要な実行導線差分は確認結果から別途具体化する。Tooling18件／構成登録8件の再実行・検証上限拡張は含めない。

**Owner判断待ち。** §8の「失敗保全後に停止。同原因・同条件の再実行はOwner判断後最大1回、原因修正は別review」に基づく、別原因・出力環境差分の限定確認案である。隔離コピー／compileは未実施。残予算は記録済み170.2142763秒＋未計測管理5分からB2約82.16分で、今回も時間超過による停止ではない。

## 19. §18隔離compile確認のOwner承認（2026-10-08）

Ownerは「既存source・設定を変更せず、隔離出力で1回compileする確認を承認します」と明示した。§18の同一source／POMコピー・全件hash照合・既存compiler条件でのoffline test-compile1回とconstructor注釈比較を採用した。既存targetの上書き／削除、IDE／無関係processの停止、source／POM／compiler設定変更は含めない。

今回の承認は隔離compileの有限確認まで。28件・回帰301件の実行導線変更／package、コピー戻し・結果受入・local commit／remoteは実施せず、結果をEvidenceへ記録して次の具体差分を整理する。FAIL／注釈欠落／source差分／上限超では停止し、反復しない。

隔離compile1回はSUCCESS。205ファイルのコピー／実行前後hash一致、main131 source／test52 sourceを同じcompiler条件でcompileした。constructor param_index=8のNullableを確認し、生成recordのclass hashはB1受入済みJAR内のclassと同一。元targetの当該classとJARは前後不変。JUnit／Dockerなし。詳細は[Evidence§10](../architecture/validation/phase4-s1-stage-b2-issue-read-20261008.md#10-18承認済み隔離compile1回の結果)を参照する。

## 20. 隔離Reference出力を使う残集合の実行導線案（2026-10-08）

§19で同一sourceの隔離compileが成立したため、既存source修正を追加せず、既承認の未実行28件＋回帰301件へ進むための実行導線だけを変更する候補を示す。現class生成元の特定を開始条件にせずUNKNOWNを保持する。元targetの置換／cleanやIDE停止は行わない。

**具体差分候補：** 承認済み新規script `build-support/phase4-level2-verification/scripts/verify-s1-b2.ps1`のReference実行先・集合／gate対応付けと、端末内rawの実行台帳。新しいmain／test／SQL／POM／依存／Repository fileを追加しない。

| 集合・処理 | 実行先・最小変更 |
|---|---|
| Reference未実行28件 | コピーPOM `tmp/b2-isolated-compile-0321079-20261008/koiki-reference-app/pom.xml`のSurefire直接goal。I12／B8／X8だけを逐次実行。reportsはコピーmoduleのtarget。成立済みC8は再実行せず、保存C8 XML／resultと構成sourceのhash一致を別gateとして照合する |
| Tooling B1 52件＋選択PL2 6件 | 元Tooling moduleのjdbc runtime・採用済みSurefire／選択Failsafe methodを維持。B2 Tooling18件は再実行しない。Tooling helper／SQL／JARを追加変更しない |
| Reference関連143件＋既存25 class99件 | 同じコピーPOMのSurefire直接goalへ実行先を置き換え、B1受入時のclass／件数を固定。全suiteへ拡張しない。既存resource flag・pool条件を維持し、全体pool overrideを加えない |
| Reference package | 新規の隔離targetから同じコピーPOMでoffline `-Dmaven.test.skip=true package`。元JARへコピーしない。main5件の包含とtest／Tooling型／SQL／secret非包含を確認する |
| package済みE2E1件 | 元E2E POM／既存test sourceを維持。forkのargLineを`-Xmx768m -Duser.dir=<隔離root>/build-support/reference-e2e-verification`とし、既存の`user.dir/../..`によるJAR解決を隔離JARへ向ける。既知の空working directoryだけを作成し、E2E source／assertionは変更しない。JAR hashと実起動commandを対応付ける |

Tooling訂正gateの旧script hashは歴史証拠として保持する。新scriptの変更前後hash・Owner承認§・具体diffを新しいruntime導線台帳へ保存し、script以外のTooling source hash不変を確認する。訂正gateを上書きせず、旧script hashを現在と同一だと扱わない。コピー205件と生成artifact258件のhashを各集合前後に照合し、未知差分では停止する。

資源／source保護方式・assertion・総355件・停止／cleanup条件・raw／再実行上限は従来どおり。新規26件のPASSは成立済みの証拠を対応付け、残329件だけを新しいrawで実行する。copy source変更が必要なら、コピーだけを修正して続行せず停止して差分reviewへ戻す。

管理残は約81.85分。残新規28件30分・回帰301件30分・残compile／package／raw処理7分・最終cleanup5分＝72分を予約できる。各処理前に再評価し、既承認90分／新規45分・回帰30分／compile等10分・累積240分を広げない。現在B2 raw約2.35 MB、B1＋B2約10.08 MBで、隔離packageを含めても256 MiB／1 GiBの実測上限を各処理前後に確認する。

**Owner判断待ち。** §18は隔離compile確認までのため、このruntime導線差分・隔離packageとE2EのJAR解決差分を自動採用しない。script／実行導線変更・package・残集合は未実施。Framework／CI／remote、結果受入・実運用・Phase 4全体へ拡張しない。

## 21. §20実行導線採用・残集合再開のOwner承認（2026-10-08）

Ownerは「この実行導線差分の採用と残集合への再開を承認いたします」と明示した。§20のscript／端末内台帳・隔離Reference runtime／package・E2EのJAR解決差分を採用し、成立済み26件を再実行せず残28件＋回帰301件を既承認の上限で実行することを許可した。

元target／JARの置換・clean、IDE停止、main／test／SQL／POM／compiler条件変更は追加しない。Tooling gate・C8の保存証拠、コピー205件・生成artifact258件とsource／環境・予算を各集合前後に照合する。失敗／未知差分／上限超／cleanup不成立では停止し、原因修正・再実行を先行しない。結果受入・commit／remoteは別判断。

実行導線を反映し、Tooling訂正gate／保存C8・構成source、コピー205件／生成artifact258件を照合してI12を開始した。I12は12件中9 PASS・1 failure・2 error、exit1で停止。fresh XML／source不変／cleanupは成立、資源上限超なし。B8／X8と回帰301件・packageは未実行。rawと原因のsource照合は[Evidence§11](../architecture/validation/phase4-s1-stage-b2-issue-read-20261008.md#11-20承認済み実行導線とi12初回失敗の保全)へ保全した。

## 22. 新規testのAudit照合・負例fixture／入力の限定訂正案（2026-10-08）

I12の失敗は新規test側のAudit検索列と、負例fixtureの識別子検査に起因する。追加実行はせず、保存XMLと現sourceを照合した。未実行B06／B08にも、既存Serviceでは許可される空白入りreasonを失敗入力とする前提誤りがあるため、同じreviewで明示する。

**具体訂正候補：** 新規Java3ファイルと承認済み新規script1件のみ。main／既存test／SQL／POM／依存／compiler条件・source保護方式を変更しない。

1. `B2ProtectedIssueReadTest.java`のI01：`WHERE event_type='PERMIT_ISSUED'`を廃止し、既存`RecoveryPermitService.audit()`および受入済みtransaction testと同じ契約で照合する。`WHERE action='PERMIT_ISSUED'`で取得した`audit_type,event_type,actor_type,actor_id,resource_type,resource_id,result`が、`BUSINESS|NOTIFICATION_RECOVERY_CHANGE|USER|<USER>|NOTIFICATION_RECOVERY_PERMIT|<発行id>|SUCCESS`の1行だけであることをassertする。保存／read／対象一致のassertionは維持し、単なるAudit行数確認へ弱めない。
2. `B2ReadConnectionHarness.java`：負例protocol準備のmetadata列名検査を`[a-z_]+`から`[a-z_][a-z0-9_]*`へ限定訂正する。現SQLには`jar_sha256`があり、数字を含む正当な識別子を現検査が拒否する。SQLに使える識別子制限・固定UNKNOWN／FIXTURE_ACCEPTED候補・Reference admission前の負例準備・reader grant・writer停止／cleanupは維持する。肯定区間の管理者更新は追加しない。
3. `B2IssueBoundaryTest.java`のB06／B08：失敗入力を`"bad reason space"`から空文字`""`へ変更する。既存Serviceは空白入りの非blank文字列を許可し、blankを拒否する。rollback completion／scope保持／writer再開拒否のassertionと2件は維持する。
4. scriptはReferenceの固定隔離root／POM literalだけを新run `tmp/b2-isolated-compile-r2-0321079-20261008/`へ向ける。旧隔離source／生成class／失敗rawは保持する。新runへ同じ205ファイルをコピーし、元／コピーhashと、旧版からのJava差分が上記3件だけであることを照合する。scriptの前後hash・§21からの承認済み差分chainと新runtime台帳を保存し、旧gate／旧approvalを保持する。

**限定確認候補：** 新コピーPOMでoffline test-compile1回。main131／test52・既存Error Prone／NullAway条件、constructor Nullableと元source／設定／target不変を確認する。成立時はI12を新rawで1回だけclass再検証し、PASSとcleanup成立時に未実行B8／X8と既承認回帰301件・隔離package／E2Eへ進む。I12の初回12件を消さず、再検証12件は追加attemptとして記録する。Tooling18件／C8は再実行しない。I12の追加反復は行わない。

実時間管理小計353.6270518秒、未計測管理5分込みB2残約79.11分。残新規30分・回帰30分・compile／package／raw7分・最終cleanup5分＝72分を予約できる。I12初回の約2.75分は新規集合の消費に算入し、新規45分・B2全90分・累積240分等を広げない。I12再検証・後続集合での失敗／別差分／上限超／cleanup不成立では再停止する。

**Owner判断待ち。** §8の「失敗保全後に停止」「原因修正は別review」に従う。上記source／script訂正・新コピー・compile／再検証は未実施。既存mainの新しい制約・Audit仕様を追加する承認ではなく、元の採用済み検証目的へ新規testを合わせる限定案。結果受入・commit／remote・実運用へ拡張しない。

## 23. §22限定訂正・compile／I12再検証のOwner承認（2026-10-08）

Ownerは「既存main・設定を変えない訂正と、compile1回・I12の限定再検証1回を承認いたします」と明示した。§22の新規Java3件・scriptの固定隔離path、新コピー／hash照合・compile1回・I12 class再検証1回を採用した。成立後の未実行B8／X8・既承認回帰301件／package／E2Eへの進行条件も§22に従う。

I01はAuditのaction・event_typeとactor／resource／resultの完全な1行照合へ訂正し、負例fixtureの識別子検査は数字付き正当列名を許容する安全なpatternへ訂正した。B06／B08の拒否入力はblankとした。旧隔離出力・失敗rawを保持し、旧script／§21 approvalをr2台帳へ同hashで保全する。現在のgate参照台帳は承認済みscript revisionとして更新し、元Tooling訂正gateは上書きしない。

main／既存test／SQL／POM／依存／compiler条件を変更せず、I12の追加反復・上限拡張は許可しない。FAIL／別差分／上限超／cleanup不成立では停止し、結果受入・commit／remoteへ拡張しない。

訂正compile1回とI12再検証1回はPASS、未実行B8／X8もPASS。新規54件が成立した。回帰はB1 52＋選択PL2 6＋Reference関連先行36＝94件がPASSしたが、次のOperationalRecoveryEvidenceDbTestで回帰用scriptの有限log入力1 MiBを超え、metadata生成前に停止した。XMLは12／failure0／error0／skip0・Maven logはBUILD SUCCESSだが、子Maven exitのmetadataを保存できておらず、このclass gateは未成立。詳細は[Evidence§13](../architecture/validation/phase4-s1-stage-b2-issue-read-20261008.md#13-回帰94件passとlog処理停止)へ記録する。

## 24. 回帰scriptの有限log処理・exit先行保存の限定訂正案（2026-10-08）

回帰用端末内script `tmp/b2-runtime-route-r2-0321079-20261008/Run-Regression301.ps1`でAgentが設定した1 MiBの入力制限が、既存の正常DB検証logを拒否した。stdoutは1,531,095 bytes、stderr0。開始票のB2 raw256 MiB／全1 GiBを超えたものではなく、test／main／role・poolの失敗を認定しない。metadata不成立として§8に従い停止した。

**最小訂正候補：** 元script／rawを保持し、新しい端末内continue script／rawを作る。Repository code／test／SQL／POM／依存・検証集合は追加変更しない。

1. 子MavenのWaitForExit直後にexit・command・開始／終了時刻・経過を先に保存する。XML／log処理が失敗してもexitを失わないようにする。
2. log入力は各16 MiBを有限上限とし、実行中のsize監視で超過時は当該process treeだけを終了して停止する。保存済み正常log1.53 MBを扱える上限で、B2 raw256 MiB／全1 GiBは拡張しない。入力はstreamで読み、connection／credential行をredactしたMaven結果／resource・warning／errorの有限summaryを別fileへ保存する。出力summaryは各1 MiB以内。元logを消さず、無制限全文メモリ読込や新しいdiagnostic反復を加えない。
3. 94件の保存class result／XML・source/artifact hash・cleanupを照合してcontinue集合へ対応付け、Mavenを再実行しない。旧runのscript／source／失敗raw／approvalを保持する。
4. OperationalRecoveryEvidenceDbTest12件はexit metadataがないため、BUILD SUCCESSからexit0を補完しない。新rawで同じclassを1回だけ再検証し、期待12・failure／error／skip0・fresh XML・exit0・source不変・cleanupでgateを判定する。compileやtest修正は行わない。
5. 当該12件成立時だけ、未実行のReference関連95件・baseline99件・E2E1件と隔離packageへ進む。continueは12＋95＋99＋1＝207件、旧94件と合わせて回帰301件。新規54件／94件の再実行・全suiteへの拡張はしない。12件の観測済み前attemptは別記し、coverageを増やしたと数えない。

全source／生成artifact843件は停止後のread-only照合で差分0、container0・Javaは既存IDE2660／4684だけ。記録済みB2時間1067.6313447秒、未計測管理5分込み残約67.21分。回帰30分のうち317.4429951秒を消費済みとし、continue枠は最大1482.5570049秒（約24.71分）。Owner待ち・文書点検は別管理とし、回帰枠を30分へリセットしない。残package／raw5分・最終cleanup5分も保持する。

**Owner判断待ち。** 原因修正・class再検証1回の案であり、追加script／実行はまだ行っていない。入力16 MiB／出力1 MiB・raw／資源／時間上限超、case／exit／source不一致・cleanup失敗では再停止する。追加反復・別修正・結果受入・commit／remoteへ拡張しない。

## 25. §24有限log処理・exit先行保存のOwner承認（2026-10-08）

Ownerは「24. 回帰scriptの有限log処理・exit先行保存の限定訂正案（2026-10-08）を、確認し承認いたします」と明示した。旧script／rawを保持し、端末内continue scriptだけに§24の訂正を適用する。成立済み94件は保存証拠を照合して再利用し、OperationalRecoveryEvidenceDbTest12件を1回だけ再検証する。成立時に未実行195件と隔離packageへ進む。

既存main／test／設定／POM／compiler条件は変更しない。回帰残1482.5570049秒、有限入力16 MiB／要約出力1 MiB、raw／資源／cleanup・停止条件を維持する。追加反復・別原因修正・結果受入・commit／remoteは含めない。

§24のlog処理訂正とDB12件再検証1回は成立。回帰280件までPASSしたが、最後の既存Security20件で1 failure／exit1となり停止した。package／E2E未実行。詳細は[Evidence§14](../architecture/validation/phase4-s1-stage-b2-issue-read-20261008.md#14-24承認済み訂正成立回帰280件passと既存security-resource差分停止)。

## 26. 既存Web MVC artifact差分による停止と次review入力（2026-10-08）

`ReferenceBusinessUrlSecurityTest.servesHtmxIntegrationResourcesWithoutAuthentication`で、応答JSに既存testが要求するlogin遷移branchがなく失敗した。現Repository JSとB1受入Reference JAR内JSは同hashでbranchあり。今回解決されたローカルWeb MVC Starter JARは別hashでbranchなし、失敗応答と同じ内容だった。classpath180 JARはpreflightから不変であり、途中のcache変更ではない。preflightで受入artifactとの一致確認が不足していた。

受入nested JARとlocal cacheの各10 entryを比較し、差分はJS／CSS／自動構成imports／HTML fragmentの4件。Framework source・既存testを修正してPASSさせる案、JSだけを差し込む案には狭めない。cacheの生成元・更新履歴はUNKNOWN。元cache／target／受入JAR・失敗rawを保持し、build／install・cache差替・再検証は実施していない。

**次reviewで具体化する事項：** 受入artifactと現source／既存座標を一致させる方法、その操作が必要とするlocal cache／artifact書込み範囲・保全／戻し条件、4 resource差分が影響する新規54／回帰280の保存結果の再利用可否と必要再検証集合。既存POM／依存座標やFramework実装変更へ自動拡張しない。供給artifactが変わる場合、旧hashのPASSを新hashのPASSと説明しない。少なくとも既存Security20件・隔離package／E2E1件が未成立で、再検証を21件だけと今ここで確定しない。

新規54件・回帰280件は成立済み、失敗20件内の19部分PASSはclass gateへ加算しない。回帰30分は旧317.4429951＋continue563.8875631＝881.3305582秒消費、残918.6694418秒（約15.31分）。B2残約57.81分の管理値は未計測5分を差し引いた仮定で、回帰残枠や再実行上限を広げるものではない。必要集合が残枠に収まらなければ、artifact整合と併せて予算・集合差分をOwner reviewへ出す。

停止後source／artifact1032 entryと依存180 JARは差分0、container0・当該Maven／DB残留0。READMEへ限定入口と再現前提を追加したが、完了判定は保留。§8の「既存回帰失敗」「原因修正は別review」に従い停止を維持する。次の修正／再実行・結果受入・commit／remoteは未承認。

## 27. artifact整合方法・保存結果の扱い・限定再検証review案（2026-10-08）

Ownerの「artifact整合方法と再検証範囲のreviewへ進めます」に基づくread-only review。build／install・cache書換え・test実行は行っていない。推奨案は**現Framework sourceを変えず隔離package1回→受入artifactとの全entry一致確認→既存1座標の標準local install1回→影響する112件の限定確認**。Framework実装や依存座標の変更ではなく、検証環境のartifact整合操作の例外を明示する。

### 27.1 差分の再見と影響判定の前提

`artifact-review-observation.json`（Evidence§15）に比較を保存した。byte差分4 resourceのうち、CSS／AutoConfiguration.imports／HTML fragmentはCRLFとLFを正規化すると全文一致する。importsのクラス名は両方とも`org.koikifw.starter.webmvc.internal.KoikiWebMvcAutoConfiguration`。JavaScriptだけは正規化後も不一致で、cacheの1670 bytesに対し現source／受入resourceは2196 bytes。4 resourceすべて現sourceと受入nested JARはbyte hash一致。両JARは非directory entry各10件、全class3件・POM／pom.properties／manifestは同内容。Repositoryの当該Framework module／ParentにはGit差分なし、module POMとcached POMも同hash。

JavaScriptの欠落を既存testの弱化やsource追加修正で解消しない。生成／cache更新履歴はUNKNOWNを維持する。今回の比較は差分の範囲を特定したもので、cache全体の正当性やFramework正式受入の新規認定ではない。

### 27.2 整合操作の具体範囲・保全と戻し条件

1. 新run `tmp/b2-artifact-alignment-0321079-20261008/`を使用し、Framework moduleのPOM／main src6件とParent POM、計8ファイルを同じ相対配置でコピーする。元／コピーhashを前後照合する。元Framework target、Reference元target／JAR、既存隔離Reference src／class258件を変更しない。
2. 元Wrapper・JDK21・同Parent compiler条件で、コピーmoduleへoffline `-Dmaven.test.skip=true package`を**1回**実行する。既存Framework module1件の標準compile／packageだけで、Root Reactor／他moduleのbuild・Framework test・install／新downloadは含めない。
3. 生成JARをB1受入Reference JAR内のWeb MVC Starterと比較する。非directory entry名集合10件と**全entry内容hash一致**が必要。ZIPのtimestamp等でJAR全体hashが異なる場合は両hashを別記する。追加entry・class／resource／POM／manifest内容の不一致では停止し、installせず、build反復／追加修正を行わない。直接nested JARをcacheへコピーしたり、JSだけを差し込んだりしない。
4. local install前に`C:\Users\kataoka\.m2\repository\org\koikifw\koiki-starter-web-mvc\`配下を全件path／size／hash付きで当該rawへ保全する。現在は5 file／10,065 bytes。対象directory内64 file／4 MiBを追加の保全上限とし、未知差分・他のMaven実行・baseline以外のJavaがあれば開始しない。Reference依存180 JAR／Tooling runtime129 JARを再照合する。
5. 検証済み生成JARと同じコピーPOMを、cache済み`maven-install-plugin:3.1.4:install-file`で**1回**標準installする。座標は既存`org.koikifw:koiki-starter-web-mvc:0.1.0-SNAPSHOT`、packaging=jar、generatePom=falseに固定する。書込みは当該artifactのJAR／POM／local metadataだけ。他座標のJAR／POM変更、settings／通常構成／依存座標変更、remote publishは含めない。実行環境の権限付き承認手順を使用する。
6. installed JARとコピーPOMのhash一致、当該cache配下の変更一覧、他のReference179 JAR・Tooling129 JAR不変を確認する。新しいclasspath180件のmanifestとsource／artifact台帳を別に作る。旧180件manifest／旧PASS・失敗rawは上書きしない。Reference test classはFramework class内容が不変で生成artifact258件も不変の場合に限り既存隔離出力を利用し、Reference再compileを加えない。
7. 全確認が成立した場合は整合済みcacheを保持し、環境変更として記録する。旧cache backupは削除しない。install失敗／不一致や後続検証FAILの場合は当該artifact配下の既知変更だけをbackupから復元する。復元前に絶対pathが上記directory内にあること、変更が当該installによる既知fileだけであることを確認する。第三者更新・未知file／復元失敗では強制上書き／広範囲deleteをせず停止してOwner判断へ戻る。復元は合意済みcleanupであり、再install／test反復の許可ではない。

候補command（未実行、`<alignment>`は上記新run、`<module>`はその`koiki-starters/koiki-starter-web-mvc`）：

```text
mvnw.cmd -o -B -ntp -f <module>/pom.xml -Dmaven.test.skip=true package
mvnw.cmd -o -B -ntp -f <module>/pom.xml org.apache.maven.plugins:maven-install-plugin:3.1.4:install-file -Dfile=<module>/target/koiki-starter-web-mvc-0.1.0-SNAPSHOT.jar -DpomFile=<module>/pom.xml -DgroupId=org.koikifw -DartifactId=koiki-starter-web-mvc -Dversion=0.1.0-SNAPSHOT -Dpackaging=jar -DgeneratePom=false
```

cache操作前に復元用script／manifestを有限範囲で準備し、未知writerの排除を保証したとは説明しない。実行前後のhash／process確認で不一致を検出したら停止する。

### 27.3 保存結果の再利用条件と再検証112件

| 集合 | 候補の扱い／理由 |
|---|---|
| 新規Tooling18件＋回帰Tooling58件 | runtime JDBC classpath129件にWeb MVC Starter0、Tooling JAR／source不変を再照合して保存PASSを再利用する。artifact変更の影響対象外 |
| 新規Reference36件（C8／I12／B8／X8） | C8はFoundation明示構成のApplicationContextRunner、残28件は既存Harnessが`--spring.main.web-application-type=none`で起動。MVC resourceを利用しない。Web MVC class全3件／importsの意味不変、他179 JAR・新規main／test／source／生成artifact不変を条件に保存PASSを非影響範囲の証拠として採用する。I12の追加反復はしない |
| 関連143件のうち131件 | 下記12件を除く既存契約／scope／TTL／許可／Audit等のpure test・明示構成・non-web Harness検証。resource利用なしと上記不変条件を対応付け、保存PASSを再利用する |
| NotificationFoundationRegistrationTest7件＋NotificationFoundationMigrationTest5件 | 通常Servlet起動を含むため、整合後artifactでclass各1回を再確認する。既存件数・assertion・pool／資源を維持する |
| baseline25 class／99件 | 既存Web／Securityを含む集合として全99件を整合後artifactで各class1回再確認する。失敗したReferenceBusinessUrlSecurityTest20件を最初にclass再検証1回し、成立時だけ残79件等へ進む。失敗methodだけの実行／assertion削除はしない |
| 隔離Reference package／E2E1件 | 同じ既存隔離POMでpackage1回。新Reference main5件包含・fixture非混入、nested Web MVC JARがinstalled JARと同hashを確認後、§20採用済みJAR導線で既存E2E1件を初実行する |

再検証／初実行は**7＋5＋99＋1＝112件**。回帰の保存再利用はTooling58＋関連131＝189件、189＋112＝301件。新規54件を加え355件の候補集合を維持する。旧回帰280件のうち91件（baseline先行79＋registration7＋migration5）は旧artifact epochの証拠として保持し、最終採用値は今回の結果へ置き換える。追加attemptでcoverage件数を増やさない。

**旧artifactでのPASSを新artifactで再実行したPASSと説明しない。** 再利用54＋189件は旧artifact／非影響条件／今回の差分比較を記録し、再検証112件と区別する。全355件が一つのclasspathで再実行されたという結論は出さない。新たなentry差分／Web resource利用／依存差分が判明したらこの再利用判定を中止し、集合reviewへ戻る。

### 27.4 予算・実行導線・停止とOwner判断

baseline99＋registration7＋migration5の直近class経過373.358798秒と、B1 E2E過去30.35秒の合計は約6.73分。再実行の確定時間ではないため、今回112件は**最大12分**を下方予約する。既消費881.3305582秒＋720秒＝1601.3305582秒で回帰30分以内。残918.6694418秒を30分へリセットせず、12分または総枠の先に達する方で停止する。class10分・class cleanupを含む条件は維持する。

Framework隔離package／install・Reference package／raw処理には残の**合計5分**を上限予約し、最終cleanup5分を別に保持する。B2残約57.81分に対して追加予約22分が可能。B2全90分・累積240分・作業量18標準時間／累積28時間、memory8 GiB／disk10 GiB、heap768 MiB／最大4 JVM・PG1、DB／接続／待機、入力16 MiB／要約1 MiB、B2 raw256 MiB／全1 GiBを広げない。過去時間の不確かさ・未計測管理5分を保持し、実開始前に再評価する。

今回の具体変更は端末内alignment／backup／新artifact・classpath／再検証台帳と限定runner、README／Evidence／本票の記録だけ。Repository Framework／Reference main・既存test／POM／依存／通常設定・既存source copyを変更しない。Repository新規scriptの機能変更も追加しない。旧runは保持し、新しいsource snapshotは当該1 artifact変更を承認済み環境差分として対応付ける。package後のReference JARとcached dependency hash・実起動commandを記録する。

**Owner判断待ち。** 採用には、上記のFramework単一module隔離build／標準cache1座標installと成功時保持・失敗時限定復元、保存54＋189件の非影響再利用、112件各class最大1回・package／E2Eと下方予約枠の承認が必要。§8の「原因修正は別review」に従い、現時点では文書reviewまで。内容不一致／未知cache変更／回帰失敗／上限超／cleanup不成立では再停止し、反復・結果受入・commit／remote・正式Framework配布／実運用へ拡張しない。

## 28. §27 artifact整合・112件限定確認のOwner承認（2026-10-08）

Ownerは「この限定案を承認します。これで進めましょう」と明示した。§27のFramework単一moduleの隔離package1回、全entry照合成立後の標準local install1回・旧cache保全と成功時保持／失敗時限定復元、保存54＋189件の非影響再利用、112件各class最大1回・隔離Reference package／E2Eを採用した。回帰最大12分、整合／package／raw合計5分・最終cleanup5分の下方予約と従来の上限／停止条件を維持する。

Repository source／POM／通常構成、元target／受入JARは変更しない。cache1座標の変更は承認済み環境差分として新旧hashを保存する。未知差分／不一致／失敗／上限超／cleanup不成立では停止し、追加修正・反復・結果受入・commit／remoteへ拡張しない。

隔離package／全entry一致・標準install1回・111件確認・Reference packageはPASSした。E2E command組立のPowerShell式で起動前に停止し、承認済み失敗時手順でcache全5 fileを復元した。新規54＋回帰300＝354件成立、E2E未実行。詳細は[Evidence§16](../architecture/validation/phase4-s1-stage-b2-issue-read-20261008.md#16-27承認済みartifact整合111件passとe2e-command組立停止)。

## 29. E2E command式の限定訂正・保全artifact再installと残1件確認案（2026-10-08）

E2E起動前に、端末内runnerの`-replace`右operandの文字列連結が括弧不足でPowerShellに4 operandとして解釈され、停止した。既存test／assertionの失敗ではなく、Agent scriptの組立不備。今回E2E attempt0で、追加JUnit実行や原因修正はしていない。read-onlyの同式確認ではreplacementを括弧で包めば文字列生成が成立した。

**最小訂正・残確認候補：**

1. 新しい端末内finish runnerで、E2E commandのreplacement連結だけを`($flags -replace '-DargLine=-Xmx768m ', ('"-DargLine=-Xmx768m -Duser.dir='+$e2eDirectory+'" '))`とする。元runner／失敗rawを保持する。既存E2E source／POM／flags・heap／user.dirでのJAR解決・assertionを変更しない。有限の文字列生成確認は起動せずに行い、実commandとhashを保存する。
2. 新規54＋回帰189の非影響再利用条件と、整合artifact下の111件のresult／XML・source／artifact／cleanupを保存証拠で照合する。54／189／111件は再実行しない。元source／POM／compiler条件・既存src copy／生成classは変更しない。
3. cacheは§28の失敗時処理で元5 fileへ復元済み。その全件hash一致と、保全生成Web MVC JAR `347542...`・コピーPOM・受入nested JARとの全10 entry一致を再照合する。新しいrun／cache before-after台帳と復元手順を用い、**同じ保全生成JARを標準install-fileで追加1回だけinstall**する。生成Framework sourceの再compile／package・Reference再packageはしない。今回のinstallは§27の1回とは別の追加1回であり、同じcache1座標・他179／Tooling129不変・成功時保持／失敗時既知差分の限定復元を適用する。
4. 保全Reference JAR `5F78A9...`の不変、新main5件包含／fixture非混入とnested Web MVC JAR＝installed `347542...`を確認し、**既存E2E1件を初実行1回だけ**する。E2E moduleにはWeb MVC Starter依存がなく、起動されるApplicationは保全JARのnested依存を使用することをPOM／test sourceで確認済み。fresh XML／exit0／source不変・実起動JAR／command・cleanupと最終package literal／残留auditを対応付ける。
5. 成立時は新規54＋回帰189＋111＋E2E1＝355件の限定集合として記録し、artifact epochを区別する。111件を再install後に再実行したとは説明しない。同じ生成JAR hashでの再install、他依存／source／class不変を確認条件とする。失敗時はcacheの既知変更を復元して停止し、E2Eの反復・追加修正は行わない。

**下方予約：** E2E1件はcleanup込み最大3分。§27の12分枠は388.5566294秒消費済みで残331.4433706秒あり、3分を予約できる。既回帰1269.8871876＋180＝1449.8871876秒で30分以内。再install／raw処理最大2分、最終cleanup5分を確保し、§27の整合／package／raw合計5分とB2全90分・累積240分を広げない。B2残約51.03分は未計測管理5分を差し引いた管理仮定。memory／disk／JVM／DB／接続／待機・有限log／raw・作業量／停止条件は従来どおり。

**Owner判断待ち。** §8の「原因修正は別review」に従う、command式の限定訂正・cache追加install1回とE2E初実行1回の具体案。上記訂正／install／E2Eは未実施。結果受入・local commit／remote・Framework正式配布／実運用へ拡張しない。

## 30. §29 command訂正・cache再install／E2E初実行のOwner承認（2026-10-08）

Ownerは「限定案を承認します。対応で進めてください」と明示した。§29の端末内command式の括弧訂正、保全生成JARの追加標準install1回、保全Reference JARでのE2E1件初実行1回・最終確認を採用した。成立済み54＋189＋111件は保存証拠を照合し、compile／package／JUnit再実行を加えない。

E2E3分、再install／raw2分、最終cleanup5分の下方予約と、§27の消費済み枠・従来上限を維持する。成功時は整合済みcacheを保持し、失敗時は既知差分だけを復元して停止する。未知差分／不一致／失敗／上限超／cleanup不成立では追加訂正・反復を先行せず、結果受入・commit／remoteへ拡張しない。

§30の実行は成立した。保全生成JARの追加install1回PASS、E2E初実行1件PASS、保存54＋189＋111件の照合成立により、限定集合は新規54＋回帰301＝355件PASS。最終auditでsource／artifact不変、fixture非混入、当該JVM／container・protocol一時file／検証ENV残留0を確認した。成功条件により整合済みcacheを保持。compile／package／成立済みJUnit反復は追加していない。

[完了記録](../architecture/validation/phase4-s1-stage-b2-issue-read-20261008.md#18-b2限定検証の完了記録受入境界2026-10-08)は`OWNER REVIEW PENDING`。結果受入・local commit／remote、後続検証・実運用・Phase 4全体への承認ではない。

## 31. B2限定検証結果のOwner受入承認（2026-10-08）

Ownerは[Evidence§18](../architecture/validation/phase4-s1-stage-b2-issue-read-20261008.md#18-b2限定検証の完了記録受入境界2026-10-08)を確認し、結果受入を明示承認した。限定集合54＋301＝355件とartifact／cleanupの完了を`OWNER ACCEPTED`とする。承認記録はEvidence§19、総評は§20に整理した。§30末尾のreview pendingは受入前の履歴。

結果受入は追加検証・local commit／remote、後続接続・実運用・DoD／Phase 4全体開始の承認を兼ねない。本承認反映では文書とREADMEの状態だけを更新し、検証を再実行していない。

## 32. B2成果2コミットのlocal操作承認（2026-10-08）

Ownerが具体化済みの2コミットを明示承認した。実装・検証18 fileを`d70b883a1edface5f524272e564f715afb1bab25`へ固定し、受入・引継ぎ文書等6 fileを続く文書commitへまとめる。source基点の取得方法と操作境界は[引継ぎ§8](phase4-s1-stage-b2-fixed-results-next-session-handoff-20261008.md#8-source基点の取得)。remote／追加実行・後続操作は本承認に含まない。
