# Phase 4 S1 B2通常build対策 実装と検証記録

状態：`COMPLETE / OWNER ACCEPTED — BOUNDED NORMAL BUILD AND DEDICATED VALIDATION`（最終結果§23、受入記録§24）。2026-10-09の[Owner承認§9](../../development/phase4-s1-b2-normal-build-remediation-owner-review-20261009.md#9-owner承認記録)、[追加承認§9](../../development/phase4-s1-normal-build-dedicated-verification-separation-owner-review-20261009.md#9-owner承認記録)と後続の有限訂正・再開承認に従う。旧B1／B2受入結果・manifestは保持し、本記録は変更後sourceに対する別結果とする。

## 1 source固定とpreflight

実行環境の承認手順で、レビュー案・通常build引継ぎ・S1目的対応表の3文書を`8b03b9f7f237d8987c637b6fc2f1a5e66e03ab4f`へlocal commitし、clean sourceを確認した。改修・結果のcommit、remoteは未実施。

隔離出力は`tmp/b2-normal-build-8b03b9f-20261009-first/`。sourceは同commitのGit archiveから作成。旧tmp・target・JARをコピーせず、Maven repositoryは同runの`m2/`へ分離した。台帳は`evidence/preflight-baseline.json`と`root-test-catalog.json`。

JDK Temurin 21.0.12.1、Wrapper Maven 3.9.16。preflight時available memory 19,794,911,232 bytes、disk 51,814,055,936 bytes。既存JVM PID24852／10280をbaselineとし、当該検証の終了対象にしない。Docker起動container0、Server応答あり、PG17／Ryukのimageあり。

sandboxでCIMのmemory／process読取とDocker named pipe接続が拒否された。実行環境の権限付き承認手順で同じ読取を行い、成立した。Docker Engine障害とは判定しなかった。

Root依存の`dependency:3.7.0:go-offline -DexcludeReactor=true`は専用m2へ1回実行しexit0、約50.09秒。Modulith jdbc 2.1.1とFailsafe 3.5.6の有限依存取得もexit0。元Maven cacheへの書込みなし。root台帳は通常method344＋parameterized5＝349 invocation候補、変更後の予定は321実行＋28無効。実結果はfresh XMLで確認する。

## 2 実装差分

承認済み7件の範囲で、対象3クラスのproperty条件、B2 process bridgeの明示classpath・有限前提検査・Windows実行制限・起動失敗時整理、Windows専用準備script、README、本Evidenceを作成／変更した。業務assertion・件数、Reference／Tooling main、既存B1、POM／依存・migration・Security・CIは変更していない。

隔離sourceには改修対象のfileだけを反映し、承認済みcommit＋有限差分としてsource／artifact hashを台帳へ保存する。改修sourceを旧受入hashへ差し替えない。

## 3 検証結果

準備scriptのPowerShell parser error0、`git diff --check`は成立。7件を隔離sourceへ反映し、`evidence/remediation-source.json`へ元とコピーのhashを保存した。これはsource点検であり、Java compile・明示B2のPASSではない。

### 通常root verify 1回の停止

2026-10-09 07:31:59〜07:32:06 JST、承認済みWindows root `clean verify`を1回実行。Maven exit1、資源／時間上限違反なし。実測group約6.66秒、Maven表示約5.27秒。`koiki-architecture-contract`のSurefire goalで停止し、Referenceへ到達していない。JUnit XML0、case PASS0。B2条件・Java helperのcompile成立は未確認である。

固定した原因は、専用m2の`org.apache.maven.surefire:surefire-junit-platform:3.5.6`が未取得で、offlineでは解決できないこと。root `go-offline`のexit0だけでは実行時選択providerまで揃わなかった。annotation processorのError Prone 2.50.0／NullAway 0.13.8は実行前に有限取得済みだが、provider準備が不足した。POM・業務source・test assertionの不具合と判定したものではない。

rawは`evidence/root-verify.log`、`root-verify-result.json`、`root-resource-samples.json`。原本を保持し、失敗結果を成功runで上書きしない。実行時のsample最大JVM2、PG0、Ryuk0、接続0。memory最小19,159,506,944 bytes、disk最小51,499,212,800 bytes。Tooling targetは不存在のままで、旧固定tmpもコピーしていない。

失敗後、権限付き読取で既存baselineのJVM2件だけ、container0を確認。当該JVM／DB／readyの残留なし。run全fileは確認時287,647,534 bytes（専用m2・source等を含み10 GiB以内）。親MAVEN_OPTSはrunnerのfinallyで復元。標準install・Tooling準備script実行・明示36件・前提負例3回・package確認は未実施。

承認済み自動再実行0・失敗時停止条件に従い、追加取得・再実行・code原因修正を停止した。改修sourceは未commit、結果受入は未判断。Framework・Reference非同期実装・remoteへ進んでいない。

## 4 限定続行のOwnerレビュー入力

source・POMを変えずに、同専用m2へSurefire実行時provider `org.apache.maven.surefire:surefire-junit-platform:3.5.6`とその必要依存を有限取得する案を提示する。JUnit launcher等のversionは専用m2の解決済みJUnitと照合し、異なるversionへの切替やPOM変更で吸収しない。取得は最大5分・1回、実行環境の権限付き承認手順に従い、元cacheを書き換えない。

取得・source同一性・残予算・環境差分確認が成立した場合だけ、root `clean verify`の追加1回を新しいlog／result／sample pathで許可する案とする。初回失敗rawと部分生成artifactの台帳を保全し、旧Evidenceを上書きしない。root再実行は既承認の30分枠残から充当し、検証・環境管理90分の総枠、4 JVM／PG2／接続24、全run10 GiB等を拡大しない。

root成立時のみ、既承認・未実施の標準install、Tooling準備、明示36件、前提負例3回、package／cleanup確認へ進む。追加失敗・依存不整合・上限超では再停止する。今回の続行案は未承認であり、追加取得・root再実行は行っていない。

## 5 限定続行のOwner承認

2026-10-09、Ownerは「不足providerの限定取得と、既存上限内でのroot検証追加1回を承認します」と明示した。§4の限定続行を承認済みとし、5分内の不足provider／必要依存取得、source・環境・残予算確認、root追加1回へ進む。初回失敗は履歴として保持する。既存上限・未実施の後続集合・停止条件、結果受入／local commit／remoteの別判断は維持する。

## 6 承認済み限定続行の結果と再停止

provider `surefire-junit-platform:3.5.6`と、解決済みengineと同じJUnit Platform launcher 6.0.3を専用m2へ有限取得し、いずれもexit0。取得は約6秒、5分枠内。`provider-preparation-approved.log`と同result JSONを保全した。実行対象sourceの元／隔離コピーのhashは初回台帳と一致。初回の部分生成artifactは`root-attempt1-partial-artifacts.json`で固定した。

2026-10-09 07:35:52〜07:36:48 JST、追加root `clean verify`1回はMaven exit1。実測group約56.76秒、Maven表示約55.20秒、時間／資源上限違反なし。Architecture Contract・ArchUnit Rules・API／Data／JPA／Observability／Audit／Security／Identity／Session JDBC Starterまで成立し、Web MVC Starterで停止した。fresh XML11 suite／71件、failure・error・skip0。ただしroot全体PASS、Referenceのcompile／B2条件成立ではない。

今回の固定原因は`net.bytebuddy:byte-buddy-agent:1.18.11`の専用m2未取得。最初の`go-offline`はagent 1.17.7を取得していたが、実際のWeb MVC buildは1.18.11を要求した。provider不足は解消済みで、別の依存準備不足として区別する。POM・dependency version・既存assertionを変更して吸収しない。

新しい原本は`evidence/root-retry1.log`、`root-retry1-result.json`、`root-retry1-resource-samples.json`。初回rawと別pathで保持。sample最大JVM2、PG0、接続0、Tooling target不存在。停止後の権限付き読取でbaselineのJVM24852／10280だけ、container0、当該JVM／DB残留なしを確認。親MAVEN_OPTSはfinallyで復元。

承認済み追加1回を消費し再停止した。標準install・Tooling準備・明示36件・前提負例3回・package確認は引き続き未実施。次に必要な修正は専用m2の依存準備であり、業務codeやFrameworkの改修ではない。失敗の再見・台帳照合以外の取得・実行は行っていない。

## 7 次の限定続行案

`net.bytebuddy:byte-buddy-agent:1.18.11`と必要依存を専用m2へ1回・最大5分で取得し、同じ改修sourceでroot offline検証をさらに1回だけ実行する案。既存90分の総枠とroot30分枠から初回約6.66秒＋追加約56.76秒を差し引き、root残約28分56秒を維持する。新log／result／sampleを使用し、失敗原本・部分生成artifactを保全する。追加の不一致・失敗・上限超では再停止する。

依存準備の`go-offline`成功を、実効classpathの準備完了とみなさない。次の実行でもReference／B2へ到達しなければ改修結果を未検証のまま保持する。rootが成立した場合だけ既承認の未実施後続集合へ進む。本案はOwner判断前に実行しない。

## 8 agent取得とroot追加1回のOwner承認

2026-10-09、Ownerは「不足agentの限定取得と、既存残時間内でのroot検証追加1回を承認します」と明示した。§7を承認済みとし、専用m2への不足agent／必要依存取得と、source・環境差分確認後のroot追加1回を実行する。新しいrawを使用し、既存上限・停止条件・未実施後続集合を維持する。

## 9 agent取得後のroot結果とRyuk上限停止

agent 1.18.11の有限取得はexit0、約3.68秒、5分枠内。専用m2だけを変更。旧71件のXML11 suiteは`evidence/root-retry1-xml/`へ保存し、partial artifact hash台帳も保持した。実行対象source hashは不変。

2026-10-09 08:50:10〜08:52:11 JST、root追加1回を実行。Web MVC・Testing Supportを通過し、Reference main131 sourceとtestのcompileが成立した。保存fresh XML17 suite／89件、failure・error・skip0。B2対象classの実行順にはまだ到達していないため、既定無効・明示有効・root全体PASSは未確認。

実測group約121.19秒で監視が停止し、process exit -1、`Ceiling=TIME_OR_RESOURCE`。該当sampleのJVM2、PG1、接続1は上限内だが、Ryuk2が承認上限1を超えた。直前sampleはPG0／Ryuk1で、次forkのReference context起動logに新しいRyuk作成が記録されている。class逐次・`reuseForks=false`でも前forkのRyuk消滅と次fork開始は同期しておらず、root用Ryuk1の計画前提が不足していた。

上限違反を正常caseのPASSにしない。runnerは当該cmd／Maven／forkのprocess treeを停止した。権限付きcleanup確認ではbaseline JVM24852／10280のみ、container0。当該JVM／PG／Ryuk残留なし。Tooling targetは不存在、追加取得・追加run・業務source修正は停止した。

rawは`evidence/root-retry2.log`、`root-retry2-result.json`、`root-retry2-resource-samples.json`、`root-retry2-suite-results.json`。前回のprovider／agent不足停止とは原因を分ける。依存不足2件は解消し、今回は環境監視の上限前提で停止した。

## 10 root用Ryuk上限の訂正と追加1回の案

通常root検証だけRyuk最大2を許可する案を提示する。class別forkの切替によるreaper重なりを観測し、当該runのRyukのみを数える。JVM最大4・PG最大2・DB接続24、heap・source／POM／test条件、raw／disk・cleanup・時間上限は維持する。B2明示classはclass後のcleanup待機を維持し、今回の変更を流用しない。

source・依存・環境・残予算を照合し、新しいrawでroot追加1回のみ実行する案。root累積約6.66＋56.76＋121.19＝184.61秒、30分枠の残約26分55秒から充当し、検証・環境管理90分の総枠を維持する。初回からの失敗rawと各XML／partial artifact台帳を保全する。再失敗・上限超・cleanup失敗では停止する。root成立後だけ既承認の未実施後続集合へ進む。

本訂正案は未承認であり、Ryuk上限変更・root追加実行は行っていない。結果受入・改修commit・remoteは別判断を維持する。

## 11 root用Ryuk訂正と追加1回のOwner承認

2026-10-09、Ownerは通常root検証だけRyuk最大2へ訂正し、他の上限を維持して残約26分55秒以内の追加1回を承認した。§10の訂正・有限続行を承認済みとする。B2明示検証のRyuk上限・class後cleanup、その他上限・停止条件・結果受入／commit／remoteの別判断を維持する。

## 12 Ryuk訂正後のroot結果と通常実行前提の未達

2026-10-09 08:56:03〜09:00:02 JST、承認済みroot追加1回を実行。実測group約238.92秒。fresh XML49 suite／233件のうち199 PASS、failure6、error0、skip28。B2登録8件はPASS、対象I12／B8／X8の28件はすべてskipとなり、既定無効を確認。Tooling target・旧固定tmpは不存在のまま。B2の明示実行はまだ行っていない。

6失敗は既存S1の`ManagedRecoveryConfigurationDbTest`／`ManagedRecoveryConfigurationRegistrationTest`／`NotificationFoundationMigrationTest`／`NotificationFoundationPersistenceTest`／`NotificationFoundationRegistrationTest`／`NotificationFoundationTransactionTest`の初期化で、共通の`koiki.reference.verification.resource-limits.enabled`が未指定のため、期待値trueに対しnullとなった。通常root commandはこのpropertyを渡していない。S1 helper／既存testは検証専用flag・heapを必須とするため、B2の3クラスだけを無効にしても通常root buildの成立には不足していた。

`OperationalRecoveryEvidenceDbTest`も同じ共通基盤を継承する。B2明示helperも同基盤を呼ぶため、明示commandには同property=trueが必要である。これはsource読取で確認した実行前提であり、既存assertionを緩める案ではない。CLI全体へpool設定を上書きして吸収しない。

同一root command内でSurefireが初期化失敗後も次classへ進み、短いfork切替でRyuk3になった。監視は最大2超過を確認して当該process treeを停止、exit -1。sample最大JVM3／PG1／接続11は各上限内。既存6失敗とRyuk上限違反を区別し、root全体PASSとはしない。失敗後の新しいcommandは開始していない。

cleanupの権限付き読取でbaseline JVM24852／10280だけ、container0。当該JVM／PG／Ryuk残留なし。rawは`evidence/root-retry3.log`・同result／resource samples／suite results JSON。失敗rawを保持し、標準install・Tooling準備・明示検証・packageへの進行を停止した。

## 13 追加reviewへ渡す具体的な差分

既承認7件の範囲では通常build対策が完結しないことを確認した。次はB2に限らず、既存S1の検証専用testを通常buildから分離する範囲のOwner reviewを行う案とする。単にRyuk上限をさらに上げてrootを反復しない。

候補は、同moduleのnotification test directoryにある前節6クラスと`OperationalRecoveryEvidenceDbTest`の計7クラスへ、既存resource-limits property=trueのclass単位条件を追加する方式。通常rootでは実行せず、従来の限定検証では同flag・実効heap／DB条件・assertionを維持して明示実行する。共通helperのassertion・resource値を緩めず、Reference main／Framework／POMを変更しない。既存test変更禁止の例外になるため、この差分の個別採用が必要である。

B2明示commandには同resource-limits flagを供給する訂正も必要。通常buildのskipはS1／B2 PASSと数えない。新しい既定集合・追加7クラスの明示回帰・B2未実施36件、残予算と停止時のSurefire挙動をreviewで確定する。ここまでのroot実行累積は約423.53秒、30分枠の残約22分56秒。既承認の残枠を追加差分へ自動流用せず、新しい検証集合の成立を判断する。

追加7クラスの条件変更・明示command訂正・次のroot実行は未承認であり、実装・実行は行っていない。B2の既定条件の成立確認を、通常root全体の成立・B2明示PASS・結果受入へ拡張しない。

## 14 通常build・専用test分離の追加承認とsource固定

2026-10-09、Ownerは[追加レビュー案](../../development/phase4-s1-normal-build-dedicated-verification-separation-owner-review-20261009.md)を確認し承認した（同票§9）。追加7クラスにはimport・class条件だけを追加し、既存assertion・件数・main・POMを維持した。READMEのB2共通resource-limits flag欠落を補正し、S1専用64件の実行手順を追加した。

基点`8b03b9f7f237d8987c637b6fc2f1a5e66e03ab4f`のarchiveから新しい`tmp/b2-normal-build-8b03b9f-20261009-first/source-separation/`を展開し、承認範囲14ファイルを上書き、元とcopyのSHA-256を照合した。未commit差分をclean commitと呼ばず、`evidence/separation-source-manifest.json`で固定する。後日追記する本Evidenceのworkspace版と、今回実行時のcopyを区別する。

前回49 suiteのXMLを`evidence/root-retry3-xml/`へ保全し、partial artifact hashも記録した。開始前の当該container0、baseline JVM24852／10280のみ、disk約50.4 GB、全run約314 MB。Docker Server29.5.3を権限付き読取で確認した。副Agent・remote・commitは実施しない。

予算は累積を引き継ぐ。過去root423.53秒、今回root上限1376秒。preflight15分中の既知取得・管理を含め消費600秒として保守的計上、未実施の準備900秒、専用1200秒、負例／package300秒、最終cleanup300秒を予約する。合計最大5100秒（85分）、90分内の余裕300秒を追加実行許可とはしない。Owner待ち・文書編集は検証実測から分離する。runnerはfresh出力・CLI fail-fast・fresh XML失敗検知・資源監視・終了後最大30秒cleanup確認を行う。接続観測不能sampleは0接続の証拠にせず別欄へ記録する。

## 15 通常root成功と予定92件の分離確認

2026-10-09 09:23:06〜09:26:58 JST、承認済み追加root1回の`clean verify`はexit0／BUILD SUCCESS。command232.54秒、cleanup0.54秒。fresh XMLの349 invocationは257実行PASS、failure0／error0、予定の92 skip。skipはS1追加7クラス64件とB2 process3クラス28件だけで、B2登録8件はPASS。対象classのXMLにB2／共通resource-limits有効化propertyがないことも照合した。Tooling targetと旧固定tmpを準備せずに成立したWindows通常rootの結果であり、Linux実測ではない。

監視sample最大JVM3／PG1／Ryuk2／接続11。通常rootの承認上限内。PG初期化中の観測不能は別記し、全時刻の接続上限成立を断定しない。終了後当該JVM／containerなし。raw・XML・suite／resource／resultは`evidence/root-separation*`。通常rootのPASSは専用92件のPASSではない。

標準`-DskipTests install`1回も09:27:32〜09:27:47 JST、exit0、command14.80秒、cleanup0.29秒。専用m2へ今回sourceのRoot artifactをinstallした。最大JVM2／PG0／Ryuk0。`evidence/install-separation*`を保存した。Tooling準備scriptは未実行。

## 16 S1専用12件PASSとMigrationのJVM上限停止

共通resource-limits=true、heap768 MiB、各class逐次、既存pool設定のまま専用7クラスを開始した。

| class | 結果 | command／cleanup | sample最大JVM／PG／Ryuk／接続 |
|---|---|---|---|
| ManagedRecoveryConfigurationDbTest | 6 PASS、skip0 | 16.13／12.63秒 | 2／1／1／2 |
| ManagedRecoveryConfigurationRegistrationTest | 6 PASS、skip0 | 15.62／11.46秒 | 2／1／1／2 |
| NotificationFoundationMigrationTest | 上限停止、完了XML0 | 12.00／11.70秒 | **3**／1／1／4 |

Migration commandは09:29:02〜09:29:14 JST。最後のsampleでJVM3を観測し、S1専用上限2を超えたため所有process treeを停止、exit -1／TIME_OR_RESOURCE。DB・Ryuk・接続は上限内。test assertion failureの記録はなく、完了していない5件をPASS／FAILに割り当てない。他の4クラス47件、B2専用36件、classpath負例3件は未実行。失敗・上限超の停止条件に従い、Tooling準備も開始していない。

確定している原因はJVM数上限超過である。現runnerは数だけを保存したため、第三JVMのPID／parent／起動classは未特定。logはReference起動・Hibernate初期化途中までで、agent自己attach完了の記録はない。既存Mockito／Byte Buddy自己attachに伴う一時JVMは候補だが、今回の第三JVMをそれと断定しない。上限2という計画前提と診断記録の不足を、業務実装の不具合とは区別する。上限の自動引上げ・再実行は行わない。

終了後、権限付き再確認でbaseline JVM24852／10280のみ、container0。各classのcleanupは30秒以内。14 source hashは実行時copyと一致、全run容量約457 MB。rawは`evidence/s1-<class>*`、集約は`separation-stopped-summary.json`、source照合は`separation-source-after-stop.json`。最終停止までのS1 command＋cleanup合計約79.54秒を消費し、8分枠の未消費分を自動再試行許可にはしない。

通常build対策のsource変更とWindows通常root成立は確認済みだが、専用100件の成立・前提負例・package最終照合・結果受入は未完了。再開判断用に[第三JVMの有限観測案](../../development/phase4-s1-dedicated-jvm-observation-owner-review-20261009.md)を作成した。未commit・remote未実施、非同期Reference実装へ進まない。

## 17 Owner承認後のMigrationクラス限定観測

Ownerは[有限観測案§4](../../development/phase4-s1-dedicated-jvm-observation-owner-review-20261009.md#4-owner承認記録)を承認した。tmp runnerにPID／parent／起動時刻／class区分／所有関係／明示heap引数の有限記録を追加し、command line全文やcredentialは保存しない。source14ファイルのhash、baseline JVM、container0、累積予算を再確認した。

2026-10-09 09:35:57〜09:36:19 JST、NotificationFoundationMigrationTestだけ1回。exit0、fresh XML5 PASS、failure／error／skip0。command21.52秒＋cleanup11.56秒＝約33.09秒。sample最大JVM2／PG1／Ryuk1／接続4で、診断用JVM最大3・cleanup込み120秒以内。観測中の第三JVMはなく、前回の用途／heap／存続時間はUNKNOWN。有限samplingは短命processの全捕捉を保証しない。恒常S1上限2の変更根拠にはしない。

所有process PID24432（起動class区分UNKNOWN）とSurefire fork PID9212を保存し、各々明示Xmx768mを確認した。親PID・起動時刻・sample区間は同票§5と`evidence/migration-jvm-observation-jvm-observations.json`へ記録した。最初のPowerShell引数解析エラーはMaven起動前であり、実Maven実行・testは1回だけ。runner待機を含むsample間隔の限界も記録した。

最終権限付き確認で当該JVM0／container0、source不変、全run約459 MB。source・環境・result／resources／XML／識別集約を同runのevidenceへ保全した。観測準備の未計測分を追加120秒としてS1枠へ保守的計上、累積約232.63秒／残約247.37秒。通常root257実行PASS／92予定無効、専用S1は計17 PASS、残47件とB2専用36件等は未実行。

承認範囲どおり、この1クラスで停止した。新たな恒常上限変更・他クラス・Tooling準備・B2・commit／remoteは行っていない。残集合の再開案は[同票§6](../../development/phase4-s1-dedicated-jvm-observation-owner-review-20261009.md#6-残り集合の再開判断案未承認)へ整理し、今回観測の承認を続行許可に拡張しない。

## 18 残S1再開とIDE側jcmdによる監視停止

Ownerは残集合の再開案を確認し「これを進めましょう」と明示し、[同票§7](../../development/phase4-s1-dedicated-jvm-observation-owner-review-20261009.md#7-残集合の再開承認)へ承認を記録した。14 source hash不変、当該JVM／container0、基点8b03b9fを再確認。S1残247.37秒から準備・管理の保守的30秒を充当し、217秒以内で未実行4クラスを逐次実行するrunnerを準備した。

2026-10-09 09:53:17〜09:53:26 JST、最初のNotificationFoundationPersistenceTestで監視停止。command9.32秒、cleanup11.61秒、exit -1／TIME_OR_RESOURCE、完了fresh XML0。業務caseのPASS／FAILを認定しない。次class・Tooling準備・B2は開始していない。

停止sampleの全体Java系process数3は、検証所有PID16108（起動class区分UNKNOWN、Xmx768m）、Surefire fork PID4972（Xmx768m）、**jcmd.exe PID7180／親PID5292**。jcmdの開始00:53:26.428671 UTC、観測00:53:26.609070 UTC、所有判定false。JVM数判定が先に上限超で停止したため、保存状態には未知所有も併記する。PG1／Ryuk1／接続1は各上限内だった。

親PID5292の権限付きread-only確認で、Name Code.exe、起動2026-10-08T21:59:05.4173020Z、親27084。検証前から稼働するIDEに属するjcmdを、現runnerが当該検証JVM数へ含めていた計数上の不足と確認した。保存sampleでは当該検証の所有Javaは2。jcmdの対象PID／command、前回Migrationの第三JVMとの同一性はUNKNOWN。共通harness／当該testのsourceにはjcmd起動がない。command全文や秘密は保存しない。

停止後baseline Java2件のみ／container0を再確認した。rawは`evidence/remaining-NotificationFoundationPersistenceTest*`、parent情報は`remaining-jcmd-parent.json`、開始／cleanupは`remaining-preflight.json`／`remaining-stop-cleanup.json`。旧rawを保持し、無断の上限変更・外部process終了・再実行をしない。

S1消費は232.63＋準備30＋今回20.93＝約283.56秒、残196.44秒。専用17件PASSと残47件、B2等未実施は不変。恒常JVM2を維持し、固定IDEの有限jcmdと当該所有数を分ける[訂正レビュー案](../../development/phase4-s1-verification-process-ownership-correction-owner-review-20261009.md)を作成した。

## 19 tmp runner所有数訂正だけの承認と完了

Ownerは訂正レビュー案の§2だけを明示承認した（[同票§5](../../development/phase4-s1-verification-process-ownership-correction-owner-review-20261009.md#5-2だけのowner承認)）。tmp内Run-Jvm-Observation.ps1で固定IDE・baseline開始時刻照合、全体／当該／IDE側jcmdの分離、IDE側1件／10秒制限、cleanupでの同条件確認を追加した。実装source・資源値・共通helperを変更しない。B2所有jcmdの既存有限例外も所有数に限定して維持する。

parser errors0、有限入力による所有判定・拒否／上限条件8件PASS、source14 hash不変。記録は`evidence/process-ownership-correction-static-checks.json`。この点検は実test結果ではなく、Maven／Dockerは未実行。§3の再実行は今回承認に含めず、S1 17件PASS／残47件、B2等未実行のまま。恒常JVM2を維持し、再開は同票§3の判断と実行前の環境・残予算照合に従う。

## 20 §3再開承認後のS1専用64件成立とTooling準備

Ownerは「では、検証へ進めましょう」と明示し、[訂正票§7](../../development/phase4-s1-verification-process-ownership-correction-owner-review-20261009.md#7-3の検証再開承認)へ§3再開承認を記録した。source14 hash不変、container0を確認し、S1残196.44秒から訂正準備の保守的20秒を充当した176秒枠で未完了47件を実行した。

2026-10-09 10:02:25〜10:04:39 JST、Persistence12／Registration7／Transaction16／Operational12は全47 PASS、failure／error／skip0、各exit0とclass後cleanupを確認。stage実測134.13秒、既定枠内。既PASSの先頭12件とMigration5件を合わせ、S1専用64件の対応が成立した。既PASS集合・root・installは再実行していない。

rawは`evidence/owned-<class>*`、集約`s1-owned-summary.json`、preflight`owned-preflight.json`。訂正後Jvm欄は当該所有数、JvmAll／IdeJcmdは環境全体と識別IDE processで、旧計数との意味差を維持する。未知所有やIDE側jcmdの対象／影響をPASSの業務保証にしない。

Tooling準備script1回／3 goalは10:04:52〜10:05:13 JST、exit0、約21.90秒。offline jdbc main clean package、jdbc,s1-contract,s1-web test-compile、jdbc test-scope classpath生成の各exit0・summary PASS・source不変・artifact hash台帳を保存した。runtime classpathにcompile support profilesを使用しない。出力は新sourceの`tmp/b2-preparation-separation/`、外側結果`evidence/preparation-separation-result.json`。既存標準install15.09秒を含め、準備15分枠内。B2専用結果・負例・package最終照合は別段階とする。

## 21 B2登録8件PASSと凍結前提エラーによる停止

2026-10-09 10:05:22〜10:05:28 JST、B2登録8件はexit0／全件PASS／skip0、cleanup0.29秒、PG／Ryuk未起動。続くB2ProtectedIssueReadTestは10:05:28〜10:06:00 JST、I11_providerAcceptedRejectsでerror1、他11件はfail-fast skip。完了PASS0、command31.27秒、cleanup11.60秒、runnerはfresh XML errorを検知しTEST_FAILUREで後続を停止、exit -1。XMLのerrorを資源上限停止と混同しない。

Reference側errorは`Coordinator cleanup failed; finite log retained`。初回coordinator logはB2_FROZEN_READYまで成立。I11で作り直したcoordinatorのlogにB2FrozenSourceProtocol.freezeの`source connection remains`を確認した。constructorのclose例外が元のready不成立を覆う経路もsourceで確認し、元logを保持した。assertionの緩和やbridge修正は行わない。

sample最大は常駐を含む所有Java系5（所有jcmdの既承認有限例外）、PG1／Ryuk3／接続3。IDE側jcmdも識別記録した。Ryuk session欄はnullのsampleがあり、識別未取得をUNKNOWNとして残す。resource／process／container時刻の記録を保持し、全時刻・全sessionの観測成立を保証しない。停止後、権限付き再確認でbaseline Java2件のみ、container0、source14 hash不変。全run容量約521 MB。

保持coordinator log2件（3268／3679 bytes）を`evidence/b2-owned-coordinator-retained/`へcopyしhash照合、台帳`b2-owned-coordinator-retained-manifest.json`に保存。XML／資源／process／resultは`evidence/b2-owned-B2ProtectedIssueReadTest*`、登録結果は同Registration名、最終cleanup`b2-owned-stop-cleanup.json`、専用結果台帳`dedicated-progress-after-b2-stop.json`。不成功rawを破棄しない。

source点検でfreezeは同じDBの他接続0を要求し、runnerも`psql -U test -d test`でそのDBへ観測接続を反復していたと確認した。観測干渉は有力な候補だが、残留接続PID等を保存していないため今回errorの単独原因と断定しない。別の既存postgres DBへ監視接続を移し、接続総数上限と凍結assertionを維持する[限定訂正・再実行レビュー案](../../development/phase4-s1-b2-observer-database-correction-owner-review-20261009.md)を作成した。訂正・追加実行は未実施。

現結果は通常root257実行PASS／92予定無効、S1専用64 PASS、B2専用登録8 PASS。B2 process28件の全件成立、負例3件・package最終照合・結果受入は未完了。B8／X8・負例へ進まず停止、改修commit／remote／非同期実装は未実施。

## 22 接続数監視DBのtmp runner訂正完了

Ownerは[監視DB訂正票§2](../../development/phase4-s1-b2-observer-database-correction-owner-review-20261009.md#2-tmp-runnerだけの訂正案)を明示承認した。同票§5に記録し、tmp Run-Jvm-Observation.ps1のpsql接続先を既存postgres DBへ変更、PGAPPNAME固定、sampleへ監視先と総数に観測接続を含むことを記録した。資源上限・source・凍結assertion・schema／依存を変更しない。

parser errors0、観測command／記録項目の点検と14 source／準備artifact hashの不変を確認。`evidence/b2-observer-database-correction-static-checks.json`へ保存した。Maven／Dockerは未実行で、凍結検査の実測成立は未確認。§3追加再実行を本承認へ自動拡張せず、B2 process28件・負例・package最終照合は未完了として次判断へ渡す。

## 23 §3承認後のB2成立・負例・package・最終結果

Ownerは[監視DB訂正票§7](../../development/phase4-s1-b2-observer-database-correction-owner-review-20261009.md#7-3のowner承認)で§3の有限再実行と条件付き続行を明示承認した。source14ファイル・classpath・準備artifact hash不変、container0・固定IDE／baselineを確認した。B2既知49.12秒に管理30秒／訂正準備30秒を保守的に計上し、新たな610秒枠でI12追加1回と未実施B8／X8を逐次実行した。

| B2 class | 実行JST | fresh XML | command／cleanup秒 |
|---|---|---|---|
| B2ProtectedIssueReadTest | 11:32:12〜11:35:00 | 12 PASS、failure／error／skip0 | 168.31／11.73 |
| B2IssueBoundaryTest | 11:35:12〜11:36:57 | 8 PASS、failure／error／skip0 | 105.00／11.54 |
| B2IssueTransactionTest | 11:37:09〜11:38:51 | 8 PASS、failure／error／skip0 | 102.06／11.55 |

各exit0・class後cleanup成立、B2再開stage410.34秒で610秒以内。既PASSの登録8件を合わせB2専用36 PASS、S1 64件と合わせ専用100 PASS／skip0。PASS済み集合・root・install・準備・登録8件を反復していない。旧凍結エラーは今回再現しなかったが、以前の残留接続のPID／原因はUNKNOWNとして保持する。readonly監視でも対象DBへ接続すると前提検査へ干渉し得るため、監視接続は同clusterのpostgres DBへ分離する。

sample最大は各classの所有Java系5（常駐4＋所有jcmdの有限例外）、PG1／Ryuk3／接続3。I12の環境全体Java系最大6は、識別済みIDE側jcmdを別計上した値で、当該上限の引上げではない。起動／停止時のPG観測不能とdocker ps／inspect間のcontainer消滅競合があり、全時刻・全接続・全sessionの完全監視を保証しない。未知・欠測は0接続の証拠にせず記録する。Ryuk session欄nullや前回第三JVMの用途、IDE側jcmdの対象／影響は引き続きUNKNOWN。テスト成立・有限観測と実運用保証を区別する。

11:39:14〜11:39:27 JST、classpath未指定／不存在／空fileの負例3 commandは各1回、選択I01のerror1／skip0・Maven exit1で、それぞれMISSING／UNAVAILABLE／EMPTYの固定前提エラーを確認。coordinator／通常子・DB／Ryuk未起動、各cleanup成立。期待拒否3件を100 PASSへ加算しない。

Reference／Tooling JARのfixture非混入、Reference main5型包含、Reference target/classes全fileとBOOT-INF/classes byte hash整合、nested KOIKI dependencyと専用m2のhash整合、固定source／準備artifact hash不変、ready／今回b2-managed割当file残留0を確認した。Reference JAR SHA-256は`8A95568DC3944B78DB6463F5C5F0FC3949D4759B989C3EE9175E944D639E95B2`、Tooling JARは`CE68D082BEFD24515B8923A8228FBA8158AC83A4E2E60C2304846F76F75D92BC`。classpath hashは`8EBEA70B8B8D37F874DEAA05C80B81B3CF37095C5867F63907AC4C301EE48970`。負例＋package照合約13.81秒で5分枠内。

最終権限付きcleanup確認は当該Java0／container0、baseline2件を保持。全run約527 MB、evidence raw約27.2 MBで既存容量枠内。保守的予算集約はpreflight600秒、root累積656.07秒、S1累積437.69秒、準備36.99秒、B2累積519.46秒、負例／package13.81秒、最終cleanup管理予約300秒、合計約2564.01秒（42分44秒）で総90分以内。Owner待ち／文書編集と実検証・環境管理を分離し、未消費を再実行許可にしない。

case IDの対応を集合で照合し、root257実行PASS＋専用100 PASS−登録重複8＝349種類が元のroot349 invocation候補と一致した。rootの予定無効92件をPASSへ加算せず、専用経路で全件確認した。Windows通常buildと今回承認済み専用再現は成立。Linux実測・remote CIは未実施。旧受入・失敗raw・artifact epochを維持する。

最終台帳は同runの`evidence/final-validation-summary.json`／`final-dedicated-case-mapping.json`／`final-resource-summary.json`／`package-owned-summary.json`／`final-budget-summary.json`／`final-cleanup.json`／`final-temporary-artifacts.json`。raw hash indexは`final-evidence-index.json`。今回B2 rawは`b2-observer-corrected-<class>*`、負例は`negative-owned-*`。sourceの正本は基点8b03b9f＋`separation-source-manifest.json`、端末内artifactは別epochとしてhash管理する。workspaceの本Evidence追記と実行時copyを区別する。

実行・整合・cleanupは完了、結果のOwner受入は未判断。改修source／新文書のlocal commit・remoteは未実施。肯定consume／close、送信／復旧・実運用／DoD／正式受渡し・Phase 4全体／Reference非同期実装の開始には拡張しない。

## 24 最終結果のOwner受入承認

2026-10-09、Ownerは§23「§3承認後のB2成立・負例・package・最終結果」を確認し、「この結果を承認します」と明示した。§23のWindows通常root257実行PASS／予定無効92、専用S1 64＋B2 36＝100 PASS／skip0、前提負例3件、package・source／artifact整合、有限予算・cleanupの結果と記載した限界／UNKNOWNを、`COMPLETE / OWNER ACCEPTED`として受け入れた。

受入sourceは`feature/phase4-s1-reference-foundation`／基点`8b03b9f7f237d8987c637b6fc2f1a5e66e03ab4f`＋承認14ファイルの限定hash manifest。結果・case対応・artifact epochと証拠所在は§23を維持する。端末内`final-validation-summary.json`は受入判断前の実行時snapshotとして変更せず、今回の判断は本節と別の`evidence/final-owner-acceptance.json`へ記録する。旧受入・失敗raw・hash indexを破棄・上書きしない。

今回の結果受入は改修commit・remote・再検証・Linux／CI実測や、後続の非同期Reference実装の開始承認を兼ねない。肯定consume／close、送信／復旧・実運用、DoD／正式受渡し・Phase 4全体の開始は別判断。既存Framework／Reference本体・Securityの承認済み境界を維持する。受入記録反映時点で改修source／新文書は未commit、remote未実施。
