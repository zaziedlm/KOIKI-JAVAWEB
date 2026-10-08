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
