# Phase 4 S1 R3-A：限定実装・停止履歴・Owner受入記録（2026-10-13〜14）

状態：**R3-A LIMITED RESULTS COMPLETE / OWNER ACCEPTED（2026-10-14、§23）**。選択10件は別epochで限定PASS、通常rootは303実行＋専用92 skip・failure／error0。A01旧XML欠落、A04a初回FAIL、A04d／A04aの狭い保証境界を履歴として維持する。入力sourceは`feature/phase4-s1-reference-full-stop-recovery-r3a`のclean commit `b064084a20cd7c1a82726241d8fb5970248d836c`。R3-Aの[全停止契約・開始票§9](../../development/phase4-s1-r3a-full-stop-contract-and-minimal-start-20261013.md#9-残る実行条件と文書固定の判断入力2026-10-13)と[訂正済みpreflight結果](phase4-s1-r3a-source-and-preflight-stop-20261013.md#8-71承認後の対象1件tooling再preflight結果2026-10-13)を起点とする。§3・§6の停止履歴を経て、A01結果を§8・§19、A04d結果を§13、A02・A03とA04a初回停止を§15、A04a訂正結果を§16、残る5件を§17、通常rootを§21、epoch別点検を§22に記録する。

## 1 作成範囲と静的判断

実装開始時の未commit候補はReference `notification`内の5 file、Tooling `build-support/reference-e2e-verification/`内の6 file、本Evidence1 fileだった。§8の承認済み非Web訂正でReference `identity`設定2 fileを追加し、**§8時点の未commit差分は計14 file**。§12で開始票を訂正した時点で15 file、§16でTooling probeを追加した現行差分は16 file。Framework、Customer、POM、既存migration、通常設定は変更していない。Reference側は明示opt-inの非Web `ApplicationRunner`、単一publicationのsnapshot再照合、PostgreSQL session advisory lock、Modulith 2.1.1 `IncompleteEventPublications.resubmitIncompletePublications(Predicate)`のID限定呼出しと完了待ちを実装候補とした。通常起動ではrunner構成を登録しない設計だが、通常構成のroot実動作保護は未検証。

Tooling側はfresh DB専用のA01 PUBLISHED→PROCESSING trigger／advisory lock gate、A02のstub転送前・A03の独立receipt確定後loopback gate、所有子processの起動・停止とDB／provider観測を作成した。`PackagedReferenceAsyncRecoveryTest`にA01〜A04dの7 methodを割り当てた。A04dの現行候補はTooling接続が同じadvisory lockを保持する方式で、**実際の先行専用復旧processとの競合をまだ実証しない**。後続実行・受入前に方式の適合を再reviewする。

localのModulith 2.1.1 bytecodeでは、predicateをpublication集合へ適用してから対象の`markResubmitted`へ進む。JDBC V2の更新SQLはIDと`STATUS != 'RESUBMITTED'`を使い、旧status／試行回数をWHERE条件に含めない。したがって今回のReference候補は専用lock、通常process全停止、実行直前のtuple再照合を前提とし、DB直接UPDATEによる完了処理は追加していない。A01の故障位置はこの静的読取だけでは成立しない。

## 2 compile・JAR対照

`MAVEN_OPTS=-Xmx768m`、offlineのReference reactor `clean test-compile`は13 moduleすべてBUILD SUCCESS。Reference main 146 source、test 54 sourceをcleanからcompileし、前回残った「独立clean Maven main compile未証明」はこの時点のsourceについて解消した。その後のReferenceコード追記・Tooling追加に対してTooling offline `test-compile`は12 test sourceでBUILD SUCCESS、Reference通常／asyncの`package`も各BUILD SUCCESS。実testはこの段階でskipした。

最初の通常／async packageでは、async profileが有効なのに2本が同一hash・同一runtime依存になった。`help:active-profiles`で`s1-reference-async`有効を確認。Maven JAR pluginが既存JARを再利用したと整合するため、失敗出力を`tmp/r3a-implementation-20261013/reference-async-attempt1.jar`に保全し、R2の対照時に採用した`maven.jar.forceCreation=true`でasync packageを1回作り直した。これはA01 testの再実行ではない。

| 固定JAR | SHA-256／内容 |
|---|---|
| 通常 `reference-normal.jar` | `6CCAEB7D9EF866C3DC5EAA8D6CE4D05198A7858312DE29E0834608B03F7D2B8A`、app entry189、nested lib117 |
| async `reference-async.jar` | `6555A6C89B55B334FA39BEC589381B0AF154A9128D4C9D9AA6B6B944CF08E3BF`、app entry189、nested lib127 |

`BOOT-INF/classes/`の189 entryはpath・内容SHA-256が一致し、通常だけのlibraryは0、asyncだけのlibraryはModulith関連10件。async側に`spring-modulith-events-jdbc-2.1.1.jar`が存在する。pair照合は静的なartifact成立であり、復旧動作のPASSを示さない。

## 3 A01初回1件の結果と停止

17:36 JST、固定async JAR `6555A6...`を入力に、`PackagedReferenceAsyncRecoveryTest#A01PublishedBeforeSendCompletesOneTargetAfterFullStop`を**初回1 invocation**だけ実行した。直前のDocker Serverは29.1.3、稼働container0、OS空きmemory約12.02 GiB、C空き約495.47 GiB、保護Java PID30304／25248のみを確認した。Toolingのfresh PostgreSQL、Reference子process、対象要求とA01 gateの作成までは到達した。

fresh XML `build-support/reference-e2e-verification/target/surefire-reports/TEST-org.koikifw.buildsupport.referencee2e.PackagedReferenceAsyncRecoveryTest.xml`は17:36:17.602 JST更新、SHA-256 `27C72A0A64DDA0AFEF367485B1615FB689C52C405DF73E55A468956706C1BED0`。**tests1／failures0／errors1／skipped0**。例外は`R3DatabaseGate.awaitBlockedBackend`の`A01 target update did not reach the DB gate`で、対象UPDATEのadvisory lock待ちを4秒以内に観測できなかった。A01のPUBLISHED送信前停止、DB rollback、provider到着0・受理0の組を固定できていない。Reference子logは起動完了を示すが、gate到達の証拠はない。trigger条件不一致、観測SQLの絞り過ぎ、実際の更新順序、観測時間の不足のどれかは今回の記録だけでは確定しない。

[開始票§5.1](../../development/phase4-s1-r3a-full-stop-contract-and-minimal-start-20261013.md#51-a01故障位置gateの静的実現性点検未実証)のBLOCKED条件に従い、**A01を再実行せず、A02／A03、A04a〜d、既存C02／D01／C04、通常rootを開始していない**。復旧専用processも未起動であり、Reference側の実動作は未検証。A01失敗を他方式の成功で置換しない。

## 4 cleanupと訂正判断入力

失敗時のtry-with-resources cleanup後、Docker稼働container0、今回所有JVM0、保護Java PID30304／25248のみを確認した。所有子logとA01 Maven log、XML、通常／async／初回失敗JARは`tmp/r3a-implementation-20261013/`およびTooling `target/surefire-reports/`に保持する。これらはGitで移送されない。logはそれぞれ約27 KiB以下、tmp内全9 fileは約189.73 MiB（raw log計約0.096 MiB）で、run全体2 GiB・raw100 MiBの上限内。実行中のJVM／DB client／Ryuk最大値は連続測定しておらず、終了時0を上限証明へ読み替えない。Git差分は未commit、remote操作なし。

次の判断には、A01 gateの**対象行に対する発火・待機を独立に観測できる方法**が必要。候補は、次回fresh DBでtrigger定義と対象status／serialized eventを停止前に読み取り、`pg_stat_activity`の広い候補集合と`pg_locks`を同時に保持し、対象更新backendを絞ること。今回のcontainerはcleanup済みなので、その情報を後から復元できない。A01の新規実行枠・停止条件・追加観測pathをOwnerが判断するまで、コードを訂正して再試行しない。あわせてA04dのTooling接続による代用が§5の「専用復旧processのlock競合」を満たすかを再reviewする。

## 5 A01診断訂正の次回限定判断案

今回の`pg_stat_activity`条件は`wait_event='advisory'`に加え、query文字列へ`event_publication`を含むことを要求している。これだけでは、**同じlock keyの待機者がいなかった**のか、**待機者はいたがquery文字列条件で落とした**のかを区別できない。次回候補はToolingのA01観測だけを訂正し、Reference production・provider・DB migration・A02以降のcase期待値を変えない。

1. 承認要求前にfresh DBの対象trigger定義、advisory lock holder backend PID、対象行の絞り込み条件を読み取り固定する。
2. 承認後は`pg_locks`でholderと**同じdatabase／classid／objid／objsubid**の`granted=false`待機者を結び、`pg_stat_activity`のPID・wait eventを別に照合する。query文字列一致だけをgate成立条件としない。対象publicationがPUBLISHED、stub到着0／受理0であることを同じ停止前時点で確認する。
3. 待機者が見えなければ対象status・試行回数、stub台帳、限定したDB活動の要約を保存してBLOCKEDとし、元processを故障位置として強制停止しない。待機者が一意でなければ同様にBLOCKED。証拠なしにA02へ進まない。

次回は**Tooling観測訂正＋Tooling offline test-compile＋A01新規1回だけ**を候補とする。新規20分、うちcleanup5分予約、case再実行0回、今回所有JVM最大4・各heap768 MiB、PG1／Ryuk1／DB client8、raw log20 MiB・作業2 GiB、直前空きmemory8 GiB／disk10 GiB以上、起動120秒・gate観測8秒・所有process強制終了5秒・後始末30秒＋5秒の上限を提案する。A01成立時もA02以降・rootはこの判断に含めず、新しい結果と資源をOwnerへ戻す。現在のJAR対はReference source不変更時だけhash固定入力として再使用し、Tooling test sourceの新compileと入力JAR hash一致を開始前に確認する。この限定判断は未承認であり、現時点で実行しない。

## 6 §5承認後のA01診断訂正・新規1回の結果（2026-10-13）

Ownerが§5を承認したため、この項の作業を新規20分枠で開始した。§5末尾の「未承認」は**承認前の履歴**として維持する。Tooling `R3DatabaseGate`だけを訂正し、trigger定義内の対象request IDとlock key、保持側backend PIDを承認要求前に確認するようにした。承認後は同じdatabase／classid／objid／objsubidのadvisory lockに対する`granted=false`のbackendを`pg_locks`で照合し、`pg_stat_activity`のwait eventを独立に確認する。query文字列一致は条件から除外した。対象status／試行回数、provider到着／受理を停止前に記録し、待機者なし・複数・不整合時は限定診断を出して停止する。Reference production、provider、migration、A02以降は変更していない。

直前のDocker Serverは29.1.3、稼働container 0、空きmemory約12.02 GiB、C空き約495 GiB、既存の保護Java PID25248／30304だけを確認した。固定通常／async JARのSHA-256は§2と一致。Tooling offline `test-compile`は12 test sourceを再compileして**BUILD SUCCESS**（17:45 JST）。続いて固定async JARでA01を**新規1 invocationのみ**実行した。`R3_A01_GATE`記録ではtrigger定義と対象IDが一致し、holder PID69に対するwaiter PID63が一意、wait eventは`Lock/advisory`。同時点の対象は`PUBLISHED`／試行回数1、provider到着0／受理0だった。所有元processの強制停止後に対象が`PUBLISHED`のまま、到着0であるassertionも通過した。したがって初回の「gate未到達」は観測条件の絞り過ぎと整合し、今回の**停止前位置・DB rollbackの限定証拠は成立**した。ただし、元processを止める前のquery本文やDB transactionの全履歴を記録したものではない。

後段の専用復旧processは起動時に失敗し、**A01全体はFAIL**。fresh Surefire XML（17:46:39 JST、SHA-256 `02A6E61FD0C3CACA13154E4AFA98797FAE53D1FD0D23C87C8B43C81AC1820BE5C`）はtests1／failures1／errors0／skipped0。子logの直接原因は、非Web起動（`spring.main.web-application-type=none`）にもかかわらず`ReferenceApiSecurityConfiguration.referenceApiSecurityFilterChain(HttpSecurity)`が構成され、`HttpSecurity` beanがなくApplicationContextの初期化が失敗したこと。復旧serviceの実行、publication完了、provider送信は証明されていない。Tooling `launchRecovery`は非Web指定を渡す一方、共通`configureEnvironment`が`SPRING_PROFILES_ACTIVE=api-bearer`を設定し、同profileのpropertyは`koiki.reference.api.bearer.enabled=true`である。次の訂正候補は**専用復旧processだけでWeb Security構成を無効化する条件**の確認であり、Reference側の通常Web Security契約変更や自動的な実装承認を意味しない。

Maven logは`tmp/r3a-implementation-20261013/a01-diagnostic-20261013.log`、子logは同dirの`r3-recovery-10609124764312556830.log`、XMLはTooling `target/surefire-reports/`に保存した。Maven log約5.4 KiB、子log約16 KiB、tmp内14 file合計約189.77 MiB。cleanup後、Docker Server応答29.1.3・稼働container0、今回所有JVM0、保護Java PID25248／30304のみを確認した。実行中の瞬間最大使用量は連続測定していない。**A01を再実行せず、A02以降・rootへ進まず停止**する。次回の訂正範囲、復旧processの安全な非Web起動条件と新規検証枠はOwner判断の入力とする。remote操作は行っていない。

## 7 専用復旧processの非Web起動条件：静的調査と次回判断案

§6の停止後に**read-onlyでsourceと子logを照合**した。Toolingの共通`AsyncReferenceFixture.configureEnvironment`は通常Web processと専用復旧processの双方へ`SPRING_PROFILES_ACTIVE=api-bearer`を設定する。このprofileは`application-api-bearer.properties`で`koiki.reference.api.bearer.enabled=true`にする。一方、専用復旧processの起動引数は`spring.main.web-application-type=none`を指定する。`ReferenceApiSecurityConfiguration`はBearer propertyだけで有効化され、`HttpSecurity`を要求するAPI用`SecurityFilterChain`を登録する。子logはそのbean生成で失敗した。さらに`ReferenceSecurityConfiguration`のブラウザ用`SecurityFilterChain`は現在無条件登録で、同じく`HttpSecurity`を要求する。**Bearer profileを外す／propertyだけfalseにする訂正では、後者が残るため非Web起動成立を保証できない**。子logが最初にAPI側で停止したので、ブラウザ側の次の失敗は静的推論であり、まだ実行結果ではない。

推奨する最小訂正候補はReferenceのidentity設定2箇所に限定する。`ReferenceApiSecurityConfiguration`にはServlet Web context条件をclass単位で追加し、API用chainだけでなくBearer用JWT beanも非Web processへ登録しない。`ReferenceSecurityConfiguration`では**`referenceIdentitySecurityFilterChain` bean methodだけ**に同条件を追加する。後者をclass全体で無効化すると、Web chain以外の`CompromisedPasswordChecker`や`CookieSerializer`も落ち、identity構成との依存を変えるため避ける。Toolingの専用復旧processは引き続き`web-application-type=none`、async有効、起動時自動再公開無効、loopback provider、対象DB一致を要求する。`api-bearer` profileが継承されても、Servlet contextでないため両Web chainが登録されないことを確認する。通常Web processでは既存のブラウザ／API chainが登録され、既存認可を維持する。

この案は[開始票§9](../../development/phase4-s1-r3a-full-stop-contract-and-minimal-start-20261013.md#9-残る実行条件と文書固定の判断入力2026-10-13)の「Reference差分は`notification`内」の境界を**identity設定2 fileへ拡張**する。現行承認から自動的に実装・再検証へ進める範囲ではない。Web型のServlet起動に変える案は同票の非Web条件を満たさない。`spring.autoconfigure.exclude`でアプリ独自の`@Configuration`を除外する案、lazy initializationでbean生成を遅らせる案も採用候補にしない。専用の最小Spring Boot入口を新設する案は起動対象と依存構成の変更が大きいため、上記2箇所で非Web起動できない場合の再設計候補とする。

Owner判断用の次回限定範囲案は次のとおり。

1. `identity/configuration/ReferenceApiSecurityConfiguration.java`のclass条件、`ReferenceSecurityConfiguration.java`のchain method条件だけを訂正する。Framework／Customer／POM／migration、通知service／provider／gate／A01期待値は変更しない。対象外の非Web bean不足が現れたらその場で停止し、追加の除外を積み重ねない。
2. 新しいclean compile後に通常／async JARを同じsourceから各1回作り、`BOOT-INF/classes`の変更が対象2 classの条件追加に整合し、runtime依存差分は従来のModulith10件だけであることを対照する。現行固定JAR hashはReference source変更後の実行入力に使わない。非Web contextで両chainがなく、Servlet通常／Bearer有効contextで各chainが存続することを対象testで確認する。
3. 上記成立時だけA01を新規1 invocation実行する。故障位置、停止・rollback、復旧process起動、publication完了、stub受理1、所有資源cleanupをfresh証拠で判定する。A01 FAIL／BLOCKEDなら再実行0回で止め、A02以降・既存3件・通常rootは実行しない。A01 PASSでも次段のOwner判断まで進めない。

新規実行枠の候補は45分（うちcleanup5分予約）、各case再実行0回、今回所有JVM最大4・各heap768 MiB、PG1／Ryuk1／DB client8、raw log20 MiB・作業2 GiB、開始前空きmemory8 GiB／disk10 GiB、起動120秒・所有元process強制停止5秒・通常cleanup30秒＋5秒とする。Web側対象testの選定・実行時間を開始前に固定し、時間／資源超過時はSTOPする。**この§7は判断入力であり、identity訂正と新規検証は未開始**。実provider、正式運用の復旧入口、remote操作を含めない。

## 8 §7承認後の非Web起動訂正・A01限定結果（2026-10-13）

Ownerが§7を確認・承認したため、新規45分枠で開始した。§7末尾の「未開始」は**承認前の履歴**として保持する。Web側対象testは開始前に、既存`PackagedReferenceCriticalJourneyTest#crossesBearerAndSessionBoundariesAndReconcilesExternalEvidence`の1件と固定した。固定通常JARでBearer APIとブラウザsessionを同一journeyで確認できる。非Web側はA01新規1件だけとした。§7のidentity設定2 fileにだけServlet Web起動条件を追加した。API設定はclass単位、ブラウザ設定は`SecurityFilterChain` bean method単位とし、他のidentity beanは維持した。Framework／Customer／POM／migration、通知service／provider／gate／case期待値は変更していない。

開始前はDocker Server 29.1.3、稼働container0、空きmemory約12.28 GiB、C空き約495.45 GiB、既存の保護Java PID25248／30304だけだった。`MAVEN_OPTS=-Xmx768m`でReference reactor offline `clean test-compile`を1回実行し、13 moduleすべて**BUILD SUCCESS**。Reference main146 source、test54 sourceをcleanからcompileした。通常／async packageはそれぞれofflineで1回、`maven.jar.forceCreation=true`を使い**BUILD SUCCESS**。新しい固定通常JARのSHA-256は`8EEFA4CE1FAEC49BF755E71B696FCFD9BCB2871628F19F5F0A6F0F68837A46F9`、新async JARは`20C6DDD19CA661460540C66FFAC861D955ABA5FAFE2605DB20FDE43521C52362`。`BOOT-INF/classes/`各189 entryは新通常／async間でpath・内容hashが一致し、旧固定JARから変わったapp entryは承認済みidentity設定2 classだけ。nested runtime libraryは通常117／async127、通常のみ0／asyncのみModulith関連10件だった。旧JARは実行入力へ使っていない。

通常JARをToolingが参照するpackage pathへhash一致で配置し、固定済みWeb側1件を実行した。fresh XMLは18:03:02 JST更新、SHA-256 `8519D82CDEDB9886FA774055FE50760352705E82DCEB9C70CFE5E0FB7554599A`、**tests1／failures0／errors0／skipped0**。BearerでのAPI操作とブラウザsession承認のjourneyが成立した。続いて新async JAR hashを照合し、A01を**新規1 invocationのみ**実行した。fresh XMLは18:04:12 JST更新、SHA-256 `D07F7E91C17DFFB0DF9CA7C96D3A4735DF1B833D4CDB65BF5A976312F42BDB9C`、**tests1／failures0／errors0／skipped0**。停止前は対象trigger一致、holder PID69／一意のwaiter PID63と`Lock/advisory`、対象`PUBLISHED`／試行回数1、provider到着0／受理0を再確認した。元processの停止後、対象`PUBLISHED`・provider到着0のassertionを通過し、専用復旧processは非Web設定と`api-bearer` profileのまま起動して`Isolated publication recovery completed`を出して正常終了した。A01の後段assertionは対象`COMPLETED`・試行回数増分・completion date、同key provider論理受理1、承認状態／成功Audit維持を確認した。非Webで両Web chainのbean数を直接列挙するtestは今回追加しておらず、実process起動・復旧成功が有効条件の実動作証拠である。

Maven logは`tmp/r3a-implementation-20261013/nonweb-*.log`、新しいJAR対も同dirの`reference-*-nonweb.jar`、復旧子logは`r3-recovery-1322060419523925086.log`、fresh XMLはTooling `target/surefire-reports/`に保存した。子logは18:04:11 JSTの起動完了と18:04:11 JSTの単発復旧完了を示す。raw log全15 file合計約0.25 MiB、tmp全25 file合計約318 MiBで提案上限内。終了後はDocker Server 29.1.3・稼働container0、今回所有JVM0、保護Java PID25248／30304のみ、空きmemory約12.29 GiB・C空き約495.31 GiB。実行中の瞬間最大JVM／DB client／Ryuk数は連続測定していない。開始からcleanupまで約6分で、45分枠内に終了した。

**A01限定結果はPASS、R3-A全体の受入は未判断**。A02／A03、A04a〜d、既存C02／D01／C04、通常rootは今回の§7承認範囲に含めず実行していない。A04dの「先行専用復旧processのlock競合」方式適合も未解決。次に進む場合は、A01結果とidentity条件差分を入力に、残りcase・通常rootの順序と上限を別判断で固定する。commit／remote操作は行っていない。

## 9 残りcase・通常rootとコミット位置の判断入力

現行の14 fileは、A01限定PASSを含む**実装候補**であり、A02／A03・A04a〜dと既存3件、通常rootの結果はまだない。したがって今はR3-A成果の受入・最終local commitの位置としない。A01だけを名目に全fileを成果確定すると、未実行のA04dを含むtestとReference差分が同時に確定したように見える。作業中断やPC移動のためのcheckpointが必要になった場合は、`A01 verified / R3-A incomplete`と明記して差分14 fileを別途点検し、Ownerのcheckpoint判断を得る。現時点ではcheckpointの必要は示されていない。

**先行する静的判断点はA04d**。現行methodはToolingのJDBC接続が`pg_advisory_lock`を保持してから復旧processを1つ起動する。これは`pg_try_advisory_lock`の拒否分岐を確かめる候補だが、[開始票§5](../../development/phase4-s1-r3a-full-stop-contract-and-minimal-start-20261013.md#5-最小開始候補の差分検証集合)が求める「先行専用復旧processとのlock競合」には一致しない。先行者と競合者を**別々の実際の専用復旧process**として識別し、先行者のlock保持中に競合者がprovider送信前に拒否されたことを独立照合できるTooling gateを、次の実行承認前に設計・静的点検する。現行provider gateやDB読取の待機には5秒のtimeoutがあり、単に先行者を待たせて競合者を後から起動する案を、時間的に決定的と仮定しない。実現不能ならA04dをPASSへ読み替えずSTOPし、方式と範囲をOwnerへ戻す。Framework・production故障switch・timeout緩和を先行追加しない。

A04dの方式・fixture差分・必要compileが別判断で成立した場合の**実行順序案**は、fresh preflightと固定JAR hash／source一致、Tooling compileを確認した後、A02→A03→A04a→A04b→A04c→A04d→既存C02→D01→C04を各1回、すべて成立した場合だけReference通常rootを最大1回とする。A02〜A04dは同名XML reportを上書きするため、終了ごとにcase名・時刻・SHA-256・log・DB／provider結果と所有資源cleanupを別保存する。A01は§8の同一source／固定JARでの結果を入力とし、今回の9件に重複加算・再実行しない。A04d訂正がReference productionやA01共通観測を変更する場合は、この流れを止め、JAR再固定・A01再確認の必要性を別判断する。

新しい有限枠の**候補**は60分（cleanup10分予約）。preflight・Tooling compile10分、残り9件25分、成立時の通常root15分、cleanup10分を上限とし、待機や失敗で超えた分を次段へ繰り越さない。各case／root再実行0回。今回所有JVM最大4・各heap768 MiB、PG1、選択case Ryuk1／DB client8、通常root Ryuk2／DB client14、PG memory1 GiB／CPU1／`max_connections=16`、開始前空きmemory8 GiB／disk10 GiB、raw log100 MiB／run2 GiB、起動120秒、復旧終端30秒、故障位置の所有process停止5秒、通常cleanup30秒＋限定force5秒を維持する。通常rootは既存のcanonical専用JVM tempと通常profileでfresh XML全件・実行／skip・failure／errorを照合し、選択caseのPASSで代替しない。FAIL／BLOCKED、provider UNKNOWN、別publication変更、lock／所有資源不明、資源超過時は後行caseとrootを開始しない。

残りcaseと通常rootが成立した後に、Git差分14 fileとEvidenceを再点検し、R3-A限定結果のOwner受入判断を得てからlocal commitを成果固定候補とする。差分の内容やfile数が変われば再点検する。`tmp/`のXML・log・固定JARはGitで移送されない履歴証拠として別保持し、remote push／PR／mergeは個別判断とする。**本§9は順序・上限とコミット位置の提案であり、A04dの方式確定、残りcase／root実行、R3-A受入またはcommitの承認ではない**。

## 10 A04d方式の先行静的点検（2026-10-13）

§9に従い、**code変更・Maven／Docker実行をせず**、現行A04dとReference／Tooling sourceを読んだ。現行A04dはToolingのJDBC接続が復旧用advisory lockを保持し、復旧processを1つだけ起動する。`JdbcIsolatedPublicationAdapter.acquire`は同じkeyに`pg_try_advisory_lock`を使うため拒否分岐の確認にはなるが、lock保持者が専用復旧processではない。開始票の「先行専用復旧processとのlock競合」のPASSには使用できない。

Tooling限定の訂正候補は、**実際の先行専用復旧processと競合専用復旧processの2つ**を使う。元通常processの停止・snapshot認可後、先行者だけを起動し、先行者専用のloopback中継へ通知を送る。中継は既存stubへ転送してreceiptを独立transactionで確定し、先行者へHTTP status／headersを送った後、受理IDの**本文だけを保留**する。先行者は`BodyHandlers.ofInputStream()`で得た本文を`readNBytes(257)`する実装なので、この位置で待てば復旧serviceのleaseが維持される候補になる。先行者の所有PID／開始時刻、専用中継への到達、stub受理1、同じadvisory keyの`pg_locks`上の単独holder backend、publication未完了を**競合者起動前**に固定する。その後、競合者を別の所有processとして起動し、先行者のholderが残る間に`pg_try_advisory_lock`で拒否されたことを子log／終了状態とDB lockで照合する。競合者のprovider要求増分0、対象tuple不変を確認してから本文を解放し、先行者が同じreceiptで1件だけ完了することを照合する。先行者の到着1を競合者の到着0と混同しない。

この候補には**未証明の前提**がある。現在の`R3ProviderGate.AFTER_RECEIPT`は5秒待ってからheadersを返すため、そのままでは競合者の起動時間を安定して確保できない。Java 21の[BodyHandlers.ofInputStreamの仕様](https://docs.oracle.com/en/java/javase/21/docs/api/java.net.http/java/net/http/HttpResponse.BodyHandlers.html#ofInputStream())では、headersを受けた時点で`HttpResponse`が返り、本文は後からstreamで読む。[HttpRequest timeoutの仕様](https://docs.oracle.com/en/java/javase/21/docs/api/java.net.http/java/net/http/HttpRequest.Builder.html#timeout(java.time.Duration))は応答受信までの5秒を定める。これらから「headers後の本文保留なら5秒の`send` timeoutを越えて先行者を保持できる」可能性を**推論**できるが、今回のJDK／中継実装での保証とはしない。先行者が本文待ちで5秒後に終了する、`sendResponseHeaders`がheadersを届かせない、または復旧serviceの30秒完了待ちより先に競合者を観測できないならBLOCKEDとする。次回実装前にToolingだけの小さなHTTP timing probeで、headers到着・本文保留・5秒超の待機・解放後の正常読取を確認する方法と有限時間を固定する。

さらに現行`AsyncReferenceFixture`は`recoveryProcess`／`recoveryLog`を**各1つだけ**保持し、2件目の起動で参照を上書きする。2 process案を作る前に、両者のPID・`startInstant`・入力JAR hash・個別logと終了状態を追跡し、例外時も両方をcleanup／秘匿値redactionできるTooling所有台帳へ訂正する必要がある。先行者が生存していても競合者だけで使い捨てDBを閉じない。今回所有JVM4以内、PG1、DB client8以内、子log10 MiBの上限を2 process時にも照合する。fixtureの所有・cleanupを先に解決できなければA04dを実行しない。

**判定：現行A04dは契約不適合、headers後本文gateは条件付きの訂正候補**。Tooling-onlyのtiming probe・2 process所有方式・A04dの期待値（先行者到着1／競合者追加0）の差分をreviewしてから、新しい有限枠で作成・検証する。Reference production、provider adapterの5秒timeout、Framework／migrationは変更候補に含めない。probeが不成立なら先行者をTooling lockで代用せず、A04dをBLOCKEDとして方式を再判断する。この静的点検だけではA04dの実証PASS、§9の残りcase／通常root実行開始、R3-A受入またはcommitを承認しない。

## 11 単一復旧process前提でのA04d再整理案（Owner提起、2026-10-13）

Ownerから「人間系の対応としては、単一の復旧processと整理できないか」と提起された。[R3目的票§4](../../development/phase4-s1-r3-purpose-recovery-model-and-minimal-verification-owner-review-20261013.md#4-最小実証を二段階で定義する案)は既にR3-Aを「対象1件・1起動世代・1復旧processを基本」としている。**R3-Aの隔離runでは、元通常processを全停止した後にToolingが専用復旧processを1つだけ所有・起動し、終端／HOLDとcleanupまで次の復旧processを起動しない**と整理できる。これは人間が正式環境で常に単一起動を守れること、operator認可・permit・Auditが成立したこと、または2 process競合を実証したことまでは意味しない。正式な運用担当／代行と開始・終了権限はR4等の別判断に残る。

この整理を採るなら、A04dの目的を**「advisory lockが既に占有された状態では、単発の復旧processをprovider送信前に拒否する」**へ限定する。Toolingの独立JDBC接続を既占有状態の注入元とし、起動する復旧processは1つだけにする。Toolingは同じlock key／holder backendと保持期間をDBから固定し、復旧子の非ゼロ終了だけでなく`Isolated recovery lock unavailable`の拒否理由、provider到着増分0／受理増分0、対象publicationのstatus／試行回数不変を確認する。現行`expectRejected`は非ゼロ終了とprovider countだけをassertしており、無関係の起動失敗でもPASSし得るため、A04dの限定理由照合をToolingへ追加する。lock解放と所有資源cleanupも照合する。**これはlock既占有時の防御分岐の証拠であり、復旧process同士の競合証拠ではない。**

この案では選択数を変えず、A01〜A04dの7件＋既存3件＝10件を維持する。A01は§8で成立済み、残り9件と条件付き通常rootの順序・上限は§9の候補を使える。A04dの限定訂正がToolingだけなら、固定通常／async JARはsource・SHA-256一致を確認して再使用する。§10の2 process／HTTP本文gate・timing probe・2 process所有台帳は**この単一process案には不要**となるが、§10は検討履歴として残す。実際の復旧process同士の競合・運用上の二重起動防止は、[段階対応表](../../development/phase4-s1-purpose-dod-stage-mapping-20261009.md)のE08並行枝を含む後段で、必要性と証拠を個別判断する。

ただし[承認済みR3-A開始票§5](../../development/phase4-s1-r3a-full-stop-contract-and-minimal-start-20261013.md#5-最小開始候補の差分検証集合)はA04dを**「専用復旧processのlock競合」**と明記している。単一process案を採用する場合、同票のA04dの名称・期待値を上記の「既占有lockによる単発process拒否」へ訂正し、2 process競合をR3-Aの受入主張から外すOwner判断が必要。T3のlock後再照合・競合時拒否は維持し、通常listenerの全停止やproviderの同key受理一意性をlockで代替しない。**本§11は判断案であり、開始票の改訂承認、A04d実装・実行、残りcase／root開始、R3-A受入またはcommitを意味しない。**

## 12 A04d単一process方式のOwner採用とTooling候補訂正（2026-10-13）

Architecture Ownerは§11の単一復旧process整理を採用し、A04dの目的を**「advisory lockが既に占有された状態では、単発の復旧processをprovider送信前に拒否する」**と定めた。[開始票§5・§10](../../development/phase4-s1-r3a-full-stop-contract-and-minimal-start-20261013.md#10-a04d単一復旧processへの訂正判断2026-10-13)へ反映した。§9〜10の2 process案は当時の検討履歴であり、現行のR3-A受入条件ではない。選択数10件、Reference production、固定通常／async JARの候補は変えない。

ToolingのA04d methodを`A04dOccupiedAdvisoryLockRejectsSingleRecoveryBeforeProviderSend`へ改名し、Tooling独立接続のholder backendと`pg_locks`保持、別接続による同一keyの取得拒否、単発復旧子の非ゼロ終了と`Isolated recovery lock unavailable`のlog、provider到着・受理0、対象snapshot不変、holder終了後の同一key解放をassertする候補へ訂正した。これらは**未実行のassertion**であり、A04dのPASSを示さない。§8のA01限定PASSを今回の訂正のPASSに読み替えない。

現行未commit差分は、§8時点の14 fileに開始票1 fileを加えた15 file。A04dのTooling sourceは既存の未追跡file内で訂正した。A04d・残りcase・通常rootの実行、R3-A受入、local commit、remote操作は実施していない。次はTooling test-compileと差分静的点検を行い、§9の実行順序・有限枠をこの単一process方式に合わせて別判断する。

## 13 A04d単一process限定検証結果（2026-10-13）

Ownerの「ここでtest-compileとA04dの実動作検証へ進めますか」を、§12で採用したA04d方式について**Tooling offline test-compile 1回とA04d選択test 1回だけ**を進める指示として扱った。A02／A03、A04a〜c、既存C02／D01／C04、通常rootは開始していない。追加再実行0回とし、A04dの失敗または前提不一致なら後段へ進まない範囲で実施した。

直前preflightでbranch `feature/phase4-s1-reference-full-stop-recovery-r3a`、固定source HEAD `b064084a20cd7c1a82726241d8fb5970248d836c`、未commit差分15 fileを確認した。固定通常JAR `reference-normal-nonweb.jar`のSHA-256は`8EEFA4CE1FAEC49BF755E71B696FCFD9BCB2871628F19F5F0A6F0F68837A46F9`、固定async JAR `reference-async-nonweb.jar`は`20C6DDD19CA661460540C66FFAC861D955ABA5FAFE2605DB20FDE43521C52362`で§8と一致した。Reference production sourceの更新時刻は両JAR作成前で、今回変更した実装はToolingのA04d testと文書だけ。JDK 21.0.12、Docker Server 29.1.3、開始前稼働container0、保護Java PID25248／30304、開始前空きmemory `13009864 KiB`（約12.4 GiB）、C空き約531.8 GBを確認した。sandboxからのDocker named pipe／CIM読取拒否はEngine障害とせず、`AGENTS.md`の承認済み権限付き診断手順で同じpreflightを確認した。

`MAVEN_OPTS=-Xmx768m`、offlineのTooling `-DskipTests test-compile`を1回実行し、12 test sourceを再compileして**BUILD SUCCESS**（18:44:58 JST）。既存の`AddressSelection` warning2件が残るがcompiler errorは0。次に同じTooling POMへ固定通常／async JARの絶対path、`koiki.reference.async.r3.verification.enabled=true`、`-DargLine=-Xmx768m`を渡し、`PackagedReferenceAsyncRecoveryTest#A04dOccupiedAdvisoryLockRejectsSingleRecoveryBeforeProviderSend`を1回実行した。Surefireは18:45:54 JSTに**tests1／failures0／errors0／skipped0、BUILD SUCCESS**、実行時間17.118秒。A04dのassertionでは、Toolingのholder backendとadvisory lock保持、別DB接続での同一key取得拒否、単発復旧子の非ゼロ終了と指定lock拒否理由、provider到着0／受理0、対象snapshot不変、holder閉鎖後の同一key再取得・解放を確認した。DB holder PIDの数値と対象tuple値そのものはXMLへ出力していないため、assertion成立の証拠として扱う。

fresh XMLは`build-support/reference-e2e-verification/target/surefire-reports/TEST-org.koikifw.buildsupport.referencee2e.PackagedReferenceAsyncRecoveryTest.xml`（SHA-256 `BA2914426B051319E0F996BD770BD09C41FE940CC6E42BCD9E87416E6DC3F9B0`）で、同じ内容を`tmp/r3a-a04d-20261013/TEST-PackagedReferenceAsyncRecoveryTest-A04d.xml`へ別保存した。compile logとMaven test logは同dir、復旧子logは`tmp/r3a-implementation-20261013/r3-recovery-9793507445139571844.log`（SHA-256 `6B92EADAFCE52AD8972291B4E613B26147A954D5323DE59F8049E2ABD084BF68`）。子logはPID36432、最大heap768 MiB、18:45:53 JSTの起動と`Isolated recovery lock unavailable`を記録する。Mavenのforked JVM native stream warningは残ったが、fresh XMLと終了コードは成功。今回のMaven log2件・子log2件は各約20 KiB以下。これらtmpとTooling targetの証拠はGitで移送されない。

終了後のDocker稼働container0、Java PIDは保護対象25248／30304だけで、今回所有JVM0。空きmemory `13275616 KiB`（約12.7 GiB）、C空き`531530956800` bytes。実行中のJVM／DB client／Ryukの連続最大値は測定しておらず、Toolingの途中sample `clients=3`と終了時0を上限全時間の証明へ読み替えない。A04dは**既占有lockによる単発process拒否の限定PASS**であり、復旧process同士の競合、運用時の単一起動保証、残り8件・通常root、R3-A全体の受入は未成立。次のcase・rootの順序と新しい有限枠、R3-A成果固定は別判断とする。commit／remote操作なし。

## 14 A04d成立後の残作業と次回限定判断案（2026-10-13）

§8のA01と§13のA04dは、同じReference production source・固定JAR対を入力とする**別epochの限定PASS**として保持する。A04dは§5の当初順序より先にOwner指示で実施したため、次回の選択集合へ重複算入・再実行しない。R3-A選択10件のうち成立済みは2件、残りは**A02・A03・A04a・A04b・A04c・既存C02・D01・C04の8件**である。A02／A03の故障位置・受理と復旧完了、A04a〜cの拒否と対象外不変、既存3件の通常非接続・stub契約・正常相関は未実証のまま残る。

実行前の静的点検で、現行`R3RecoveryFixture.expectRejected`はA04a〜cについて非ゼロ終了とprovider到着増分0だけを確認し、**想定した拒否理由を照合しない**ことが分かった。A04dと同じ偽陽性を避けるため、次の作業はTooling-onlyの限定訂正案を点検することとする。A04aは`Original Reference process is not confirmed stopped`、A04bは`Isolated recovery target snapshot changed`、A04cは`Isolated recovery target not authorized in fresh DB`をそれぞれ子logで確認し、provider受理増分0と対象snapshot不変も独立に確認する。期待文字列が現行Reference実装の拒否分岐と一致するかを訂正前に静的確認し、Tooling offline test-compileを1回だけ行う候補とする。この準備はDocker／case実行を含めず、新規15分（cleanup3分予約）、今回所有JVM最大2・各heap768 MiB、raw log20 MiB／作業500 MiB、再実行0回を上限案とする。Reference production、Framework、migration、固定JAR対は変更しない。compile不成立や理由の不一致なら残りcaseを開始しない。

上記訂正・compileが成立した後の実行順序案は、①fresh preflightでbranch／Reference production sourceと固定通常・async JAR hash／Tooling compiled class・Docker Server・保護process／空き資源を再照合、②A02→A03→A04a→A04b→A04c→既存C02→D01→C04を各1回、③**8件すべて成立した場合だけ**Reference通常profileのrootを最大1回、④fresh XML・子log・DB／stub・所有資源cleanupとGit差分を集約、である。各caseの同名Surefire XMLは上書きされるため、その都度case名・時刻・SHA-256を付けて別保存する。A01とA04dの既存XML・固定JARもhash照合する。A02／A03で5秒のprovider gateより先にHTTP timeoutやlistener終端へ進む、停止位置・元process終了・受理数・対象tupleが曖昧、A04a〜cが別理由で拒否される、provider UNKNOWN、別publication変更、資源／cleanup不成立なら後行caseとrootを止める。失敗を別caseのPASSや再実行で覆わない。

実行epochの新しい有限枠の**提案**は60分、うちpreflight10分、選択8件25分、成立時の通常root15分、cleanup10分を上限とする。前段のTooling訂正枠と混同しない。各case／rootは1回、追加再実行0回。今回所有JVM最大4・各heap768 MiB、PG1、選択case Ryuk1／DB client8、通常root Ryuk2／DB client14、PG memory1 GiB／CPU1／`max_connections=16`、開始前空きmemory8 GiB／disk10 GiB、raw log100 MiB／run2 GiB、子log10 MiB、起動120秒・復旧終端30秒・故障位置の所有process停止5秒・通常cleanup30秒＋限定force5秒を維持する。§9の旧60分枠や§13の残時間を流用せず、次回開始時から測る。Reference production変更なら固定JARの前提を、A01・A04d共通fixture変更なら既存PASSの前提を再点検し、必要な再固定・再検証を別判断する。

8件と条件付き通常rootの結果がそろってから、実際のfile数・内容・XMLと停止履歴を点検し、**R3-A限定結果のOwner受入**を求める。受入後にlocal commitの固定対象を確認し、remote pushは個別判断とする。その後は[R3目的票§4](../../development/phase4-s1-r3-purpose-recovery-model-and-minimal-verification-owner-review-20261013.md#4-最小実証を二段階で定義する案)のR3-B（E07／E09／E10、E08残枝）の目的・provider照会／保持契約・最小検証を別途整理する。R4のoperator認可／Audit・permit接続、R5の保持・追跡、R3全体・DoD全体、正式受渡し、Phase 4全体の開始をR3-A限定結果から認定しない。**本§14は次回の順序・上限・停止条件のOwner判断入力であり、Tooling訂正・compile、残り8件／通常root、受入、commit、pushの実行承認ではない。**

## 15 §14承認後の限定実行とA04a停止（2026-10-14）

Architecture Ownerは§14を確認し承認した。これをTooling-onlyのA04a〜c拒否理由訂正・offline test-compileと、成立時の残り8件・条件付き通常rootの有限実行判断として扱った。branchは`feature/phase4-s1-reference-full-stop-recovery-r3a`、HEADは`b064084a20cd7c1a82726241d8fb5970248d836c`のまま、未commit対象は15 file。Reference production／Framework／migration／POM／固定JARを変更していない。

Tooling準備枠では、Reference source上のA04a `Original Reference process is not confirmed stopped`、A04b `Isolated recovery target snapshot changed`、A04c `Isolated recovery target not authorized in fresh DB`を静的確認した。`PackagedReferenceAsyncRecoveryTest`の3 methodだけに、各子logの理由、provider受理0、対象snapshot不変のassertionを追加した。`R3RecoveryFixture`などA01／A04d共通fixtureは変更していない。Docker／caseは起動せず、`MAVEN_OPTS=-Xmx768m`、offlineのTooling `-DskipTests test-compile`を1回実行し、12 test source再compileで**BUILD SUCCESS**（11:12:42 JST）。既存`AddressSelection` warning2件、compiler error0。logは`tmp/r3a-post-a04d-20261014/tooling-test-compile.log`（SHA-256 `6781D5FDEF68670AC1E16A4A676BFB156C28E9B93197C08D8A4445AF35615DC8`）。

実行前preflightでは固定通常JAR SHA-256 `8EEFA4CE1FAEC49BF755E71B696FCFD9BCB2871628F19F5F0A6F0F68837A46F9`、固定async JAR `20C6DDD19CA661460540C66FFAC861D955ABA5FAFE2605DB20FDE43521C52362`、保全済みA04d XML `BA2914426B051319E0F996BD770BD09C41FE940CC6E42BCD9E87416E6DC3F9B0`を再照合した。Reference source更新時刻は固定JARより前。JDK21.0.12、Docker Server29.1.3、稼働container0、PostgreSQL image `postgres:17`あり、空きmemory `13258888 KiB`・C空き`535294316544` bytes。Java PID23472／24668／25088はVS Code Java拡張の保護processと確認した。検証開始後のcase／rootは各1回・追加再実行0回とした。

| 順 | 結果 | fresh XML・log |
|---|---|---|
| A02 | **PASS**。`PROCESSING`・stub到着／受理0で元processを停止し、同じ対象の完了と受理1をtest assertionで確認。11:25:55 JST、tests1／failures0／errors0／skipped0 | `tmp/r3a-post-a04d-20261014/A02.xml` SHA-256 `387887F05F8810D703BA9AAB5D3C839F7676CC13F007FF421DE6DA75160991B5`、同dir `A02.log`、元・復旧子logは`tmp/r3a-implementation-20261013/` |
| A03 | **PASS**。provider独立受理後・publication未完了で元processを停止し、同一receipt再利用と論理受理総数1をtest assertionで確認。11:28:11 JST、tests1／failures0／errors0／skipped0 | `tmp/r3a-post-a04d-20261014/A03.xml` SHA-256 `C8F8D7CE9105B49FAFAEA62C9B541FA075BDA5B6C700F560DC878E261655D27D`、同dir `A03.log`、元・復旧子logは上記dir |
| A04a | **FAIL / STOP**。復旧process起動中にstub総到着数が0→1となり、`R3RecoveryFixture.expectRejected`の「拒否された復旧processからのprovider要求0」を総到着差で代用するassertionが失敗。11:28:59 JST、tests1／failures1／errors0／skipped0。A04aをPASSへ読み替えない | `tmp/r3a-post-a04d-20261014/A04a-FAIL.xml` SHA-256 `B6BF4ECA42848739E6DD06C17F6CC6BB08D2FFE5CD783DA0D03129F3677238CD`、同dir `A04a.log`、元子log `r1-child-14242999462097523457.log` SHA-256 `AF03087788CDB75D99A2F40DD2EF4B6E4980AE6843881932C663FB0BF0289137`、復旧子log `r3-recovery-16647020337918392296.log` SHA-256 `4D07B3C5295719104EDB9E185AEF72C7DA2B06A4FBFBE14605985B4B4E6E7CE8` |

A04aの復旧子logには、11:28:58 JSTの`Original Reference process is not confirmed stopped`があり、拒否分岐に到達したことを示す。一方、元Reference子logには同時刻のprovider HTTP timeoutがある。`R3ProviderGate.BEFORE_ARRIVAL`は`released.await(5, SECONDS)`の結果を判定せず、5秒経過後に元listenerの要求をstubへ転送し得る。元子の起動・復旧子の起動時間と今回のstub総到着1は**元listenerからの転送と整合する**が、総到着値だけでは要求元を独立特定できない。このため「復旧process自身からの要求0」の証拠が欠け、test失敗をToolingの誤検知と断定してPASSへ置き換えない。追加したA04a拒否理由・受理0・snapshot不変のassertionは、先行する到着差assertionで止まったため未到達である。

§14の停止条件に従い、**A04b／A04c／既存C02・D01・C04と通常rootは開始していない**。A02・A03のfresh XMLは上書き前に別保存済み。終了後はDocker稼働container0、今回所有JVM0、Java PIDは保護対象23472／24668／25088のみ、空きmemory `13366028 KiB`・C空き`535279263744` bytes。実行中のJVM／DB client／Ryukの連続最大値は測定していない。raw log・XMLはGitに含まれず、別保存を維持する。R3-A選択10件のうちA01／A04d／A02／A03の4件はそれぞれのepochで限定PASS、A04aはFAIL、残り5件は未実行。R3-A受入、local commit、remote操作なし。

次の判断には、A04aで「元process生存中の送信」と「拒否された復旧processの送信」をどの独立観測で区別するか、または全停止型の停止未確認拒否をどの時点のprovider台帳で示すかを固定する必要がある。候補はTooling gateの5秒経過時の扱い・転送数の観測、元processからの要求と復旧processからの要求を識別できる台帳、または元processの送信が確定した後に復旧子を起動する順序の見直しである。後者は「元listener生存」の意味を狭め得るため、§5の目的との適合を先に確認する。**本§15は停止記録と訂正判断の入力であり、code訂正、A04a再実行、後行case／root再開の承認ではない。**

第一候補は、A04aの復旧子だけにTooling-ownedの**別loopback provider probe**を渡し、probeへの到着0と復旧子logの停止未確認拒否を組み合わせること。元processは従来のgate→stub経路に残すので、元listenerがstubへ送った到着を復旧子の送信と混同しない。probeは既存の`127.0.0.1`・`/notifications`契約を満たす範囲で到着数だけを記録し、送信が起きた場合はA04aをFAILとする。まずTooling-onlyの変更file・資源・有限再実行1回と、A02／A03／A04dの既存PASSへの影響を別判断する。元gateのtimeout変更やReference productionの拒否条件変更は第一候補に含めない。

## 16 §15第一候補のA04a独立probe訂正・限定結果（2026-10-14）

Ownerは§15の第一候補を確認し、進行を指示した。今回の対象はTooling testの`PackagedReferenceAsyncRecoveryTest`内A04a method訂正、新規`R3RecoveryProviderProbe`1 class、Tooling offline test-compile1回、**A04a新規1 invocation**とcleanupに限定した。A04b／A04c、既存C02／D01／C04、通常rootへは進まない。新しい有限枠は25分（cleanup5分予約）、今回所有JVM最大4・各heap768 MiB、PG1／Ryuk1／DB client8、raw log20 MiB・作業500 MiB、開始前空きmemory8 GiB／disk10 GiB、追加再実行0回とした。§14の停止済み実行枠を残予算として流用していない。

A04aで元Reference processには従来の`gate→stub`経路を維持し、拒否される復旧子だけに別`127.0.0.1`・`/notifications`のTooling probeを渡した。probeは到着数を記録し、予期せぬ要求には503を返す。A04aは元processが復旧子の起動時・拒否確認後とも生存、復旧子は非ゼロ終了、子logに停止未確認の理由、probe到着0をそれぞれ確認する。元listenerが送った可能性のある既存stub総到着・受理数や、その後変わり得るpublication snapshotを復旧子の送信判定に用いない。`R3RecoveryFixture`、`R3ProviderGate`、Reference production、固定JAR対、Framework／migration／POMは変更していない。したがって§8／§13／§15の別epochで成立したA01・A04d・A02・A03の共通fixture／production条件は維持されるが、この訂正sourceで4件を再実行したという意味ではない。

開始前にbranch／HEAD、固定通常JAR SHA-256 `8EEFA4CE1FAEC49BF755E71B696FCFD9BCB2871628F19F5F0A6F0F68837A46F9`、固定async JAR `20C6DDD19CA661460540C66FFAC861D955ABA5FAFE2605DB20FDE43521C52362`を照合した。Docker Server29.1.3応答、稼働container0、保護Java PID23472／24668／25088のみ、空きmemory `14703072 KiB`・C空き`535251419136` bytesを確認した。通常sandboxでのDocker named pipe／Maven wrapper接続拒否はRancher Desktop障害とみなさず、承認済みの権限付き実行で同じ診断・検証を行った。Tooling offline test-compileは13 test sourceをcompileして**BUILD SUCCESS**、既存`AddressSelection` warning2件、compiler error0。logは`tmp/r3a-a04a-probe-20261014/tooling-test-compile.log`、SHA-256 `F0E4D970401D8BFF1D4AA034B4B5AF0B58C13519140018A8C46595E2DDE96C17`。

A04aのfresh実行は2026-10-14 11:42 JSTに**tests1／failures0／errors0／skipped0、BUILD SUCCESS**。復旧子logには`Original Reference process is not confirmed stopped`があり、元process生存と復旧子の非ゼロ終了、専用probe到着0のassertionを通過した。fresh XMLは`tmp/r3a-a04a-probe-20261014/A04a-PASS.xml`（SHA-256 `5D56B1AFF08AB3FCC5B43977419410C7A7FAC5C9530E0FA9D8F974BE0EB78BA7`）、Maven logは同dirの`A04a.log`（SHA-256 `DDB68D39C84A173A653D314694CB74F111746037B4449BD008CC8B4776F6CA44`）、子logは`tmp/r3a-implementation-20261013/r3-recovery-4857452945937758933.log`（SHA-256 `2091B006C798D13AD4BE4116402DC07590306B826A923CC32C5718AFDE8E82E0`）。§15の旧FAIL XML／logは上書きせず履歴として保持する。今回のXML・logと固定JARはGit移送対象外。

cleanup後はDocker稼働container0、今回所有JVM0、Java PIDは保護対象23472／24668／25088のみ、空きmemory `14572728 KiB`・C空き`535240224768` bytes。実行中のJVM／DB client／Ryuk最大値は連続測定しておらず、Tooling出力の途中sample `clients=3`と終了時0を全時間の上限証明とはしない。今回のA04aは**停止未確認拒否・復旧子からproviderへの送信0の限定PASS**。§15初回FAILを消さず、原因をprovider観測の混同として区別する。未commit差分は計16 file、local commit／remoteなし。残るA04b／A04c・既存C02／D01／C04と通常root、R3-A限定結果の受入は別の順序・新しい有限枠・停止判断が必要であり、今回の成立だけで再開しない。

## 17 A04b・A04cと既存C02・D01・C04の限定実行（2026-10-14）

Ownerの「残るA04b・A04cと既存C02・D01・C04へ進めましょう」を、この**5 invocationだけ**の新しい限定実行判断とする。順序はA04b→A04c→C02→D01→C04、各初回1回・追加再実行0回。前件FAIL／BLOCKED、別理由の拒否、provider到着・受理／対象snapshot不変の不成立、所有process／Docker cleanup・上限不成立時は後行を開始しない。今回の上限は新規40分（preflight5分・選択5件25分・cleanup10分）、今回所有JVM最大4・各heap768 MiB、PG1／Ryuk1／DB client8、PG memory1 GiB／CPU1／`max_connections=16`、開始前空きmemory8 GiB／disk10 GiB、raw log100 MiB／run2 GiB／子log10 MiB、起動120秒・復旧終端30秒・所有process停止5秒・通常cleanup30秒＋限定force5秒とする。§14と§16の終了枠は流用しない。通常root、Reference production訂正、受入、commit、remoteは含めない。

実行前にbranch／HEAD、Reference production差分と固定通常／async JAR hash、Tooling compiled class、Docker Server／image／稼働container、保護processと空き資源を再照合する。A04bは古い試行回数snapshot、A04cは未許可の別publicationについて、復旧子の非ゼロ終了だけでなく指定拒否理由、provider到着・受理0、対象snapshot不変を確認する。C02は通常構成のpublication非接続、D01は同一要求のreceipt再利用、C04は独立した2承認の相関と完了を、それぞれ当該testのfresh XMLで確認する。同名Surefire XMLは各件後に別名で保全し、旧FAILを上書きしない。

preflightは11:45 JSTに成立した。branch／HEADは上記から不変、固定通常JAR SHA-256 `8EEFA4CE1FAEC49BF755E71B696FCFD9BCB2871628F19F5F0A6F0F68837A46F9`、固定async JAR `20C6DDD19CA661460540C66FFAC861D955ABA5FAFE2605DB20FDE43521C52362`、Tooling compiled classは§16の11:41:08 JST版。Reference production差分は既承認のidentity2 fileとR3-A候補notification5 fileから変わっていない。JDK21.0.12、Docker Server29.1.3、稼働container0、`postgres:17`／`postgres:17-alpine` imageあり、開始前空きmemory `15221520 KiB`・C空き`535276142592` bytes、Java PIDは保護対象23472／24668／25088のみ。A04d保全XMLのSHA-256 `BA2914426B051319E0F996BD770BD09C41FE940CC6E42BCD9E87416E6DC3F9B0`、§16のA04a保全XML `5D56B1AFF08AB3FCC5B43977419410C7A7FAC5C9530E0FA9D8F974BE0EB78BA7`も一致した。

5件は順序どおり各1回実行し、すべて**tests1／failures0／errors0／skipped0、BUILD SUCCESS**。各fresh XMLを、次の同名Surefire XMLで上書きされる前に`tmp/r3a-remaining-five-20261014/`へ保存した。

| case・終了時刻 JST | 限定確認 | 保全XML SHA-256 |
|---|---|---|
| A04b・11:46:39 | 古い試行回数snapshotを指定理由`Isolated recovery target snapshot changed`で拒否。provider到着・受理0と対象snapshot不変 | `A04b.xml` `BFEC4879848B82222C3574847C17C420B8B4B0BB7A4A6DA8B3D2391ECB45F269` |
| A04c・11:47:34 | 未許可の別publicationを指定理由`Isolated recovery target not authorized in fresh DB`で拒否。provider到着・受理0と許可対象／別対象snapshot不変 | `A04c.xml` `D502BAAEEE7A583D9CA934805FB6D910D55D6571816874BBE904FFD8F355CCE5` |
| C02・11:48:49 | async機能無効のpackage済みReferenceで承認成立、非同期publication schemaなし、stub受理0 | `C02.xml` `49566BC03BFD5C3DCB6CD21450FDE6EB25B3205DEC496DCEAD911B885FABFF6E` |
| D01・11:49:35 | 同一要求の2送信が同じreceiptを返し、論理受理1 | `D01.xml` `CF8CD3F9815AE22D4E72237D65CA1155B4E802F960F5ADCDE474458E2AD52551` |
| C04・11:50:11 | 独立した2件の承認通知の相関、各publication完了、stub受理2 | `C04.xml` `D021D4AD2510286ED07776C269367815BF81C74D115CF1E3AB44A6FEB521CEA9` |

A04b／A04cの復旧子logはそれぞれ`tmp/r3a-implementation-20261013/r3-recovery-6286352679693971996.log`（SHA-256 `E8D3C81AFD91E9DEDE2B955191FD447FAC7CB81C5A3E3C44655872C9A35F5C22`）、`r3-recovery-899935387748842897.log`（`E9A9A2066046E042AC8BD3832D0416F0079A0BAE196641E9B9C187A0F4D9D878`）。指定拒否理由を子logのcause行でも確認した。各Maven logも同dirへcase別保存し、5 XML＋5 Maven logは計103841 bytes。これらと子log・固定JARはGitで移送されない。

各case終了後にDocker稼働container0と所有JVM0を確認した。最終cleanup後もcontainer0、Java PIDは保護対象23472／24668／25088のみ、空きmemory `14951876 KiB`・C空き`535240249344` bytes。実行中のJVM／DB client／Ryuk最大値を連続測定しておらず、Tooling途中sample `clients=3`を全時間の上限証明に読み替えない。Git差分は`git diff --check`成立、未commit対象16 file、HEAD不変。local commit／remote操作なし。

これで選択10件は**A01・A04d・A02・A03・A04a訂正後・今回5件の別epochを通じた限定PASS**となった。§15のA04a初回FAILは消さない。R3-Aの通常rootは未実行であり、選択結果のOwner受入やR3-A全体の受入も行っていない。次の判断入力は、通常rootを新規有限枠で最大1回実施するか、その前にsource／artifact epoch・16 file差分・保全証拠を再点検するかである。§14の停止済み条件付きroot枠を残時間として再利用しない。

## 18 通常root前のsource／artifact epoch・差分・証拠再点検（2026-10-14）

Ownerは§17の結果を確認し、通常rootより先にsource／artifact epoch、16 file差分、保全証拠の再点検を指示した。本節は**読み取り点検のみ**で、追加compile・case／root実行、code訂正、commit、remote操作を含めない。branchは`feature/phase4-s1-reference-full-stop-recovery-r3a`、HEADは`b064084a20cd7c1a82726241d8fb5970248d836c`で不変。Gitの未commit対象は16 fileで、内訳はReference production7 file（notification新規5・identity既存2）、Tooling test7 file（既存`AsyncReferenceFixture`1・新規6）、文書2 file（開始票・本Evidence）。`git diff --check`は成立し、Framework／Customer／POM／migration／通常properties／CIの差分はない。7 production sourceの最終更新時刻は、固定通常JARの2026-10-13 18:01:13 JST・固定async JARの18:01:48 JSTより前である。これは時系列の整合であり、未commit sourceとJARの暗号学的な同一性証明ではない。

内容面でも、Referenceの新規5 fileは明示opt-in・非Web単発入口、対象snapshot再照合、advisory lock、指定publicationだけのModulith再公開に閉じる。identity既存2 fileはServlet起動条件の追加のみ。Toolingの7 fileは子process所有・隔離DB gate・loopback provider gate／probe・選択caseに閉じ、通常rootのtest集合や通常propertiesを変更していない。code14 fileの現行source fingerprintはSHA-256 `AA2AE5335BAE278AEA35CB8C9F50796EAA5C1A3E9FA42A76959728B50A78C487`。これはGit相対path昇順の各`*.java`について`path<TAB>uppercase SHA-256<LF>`をUTF-8連結して得た値で、次回preflightのsource drift検出用である。開始票のSHA-256は`FD099A37AA5F2A37687CBC8CA4D311D8C6E292D60BA6C8A402C6AAA3B41D5068`。本Evidenceは追記中のため同fingerprintに含めない。

固定通常JAR SHA-256は`8EEFA4CE1FAEC49BF755E71B696FCFD9BCB2871628F19F5F0A6F0F68837A46F9`、固定async JARは`20C6DDD19CA661460540C66FFAC861D955ABA5FAFE2605DB20FDE43521C52362`で§8以降の記録と一致した。両JARの`BOOT-INF/classes/`は各189 entryでpath・内容hashの差0、`BOOT-INF/lib/`は通常117・async127、通常のみ0・asyncのみ既記録のModulith関連10 libraryだった。現在の`koiki-reference-app/target/classes/`はJAR作成後の2026-10-14に再生成されたファイルを含み、固定JARの全entryとbyte一致しない。したがって**現行target/classesを固定JARのsource証拠として用いない**。通常rootを行う場合は、新たにclean sourceからcompileした別epochの結果として扱い、今回の固定JAR対や旧XMLへ遡及させない。

選択10件のうち**9件の保全XML**（A04d・A02・A03・A04a訂正後・A04b・A04c・C02・D01・C04）は、§13・§15〜17記載の各SHA-256と再一致し、それぞれtests1／failures0／errors0／skipped0・case名一致を確認した。§15のA04a初回FAIL XMLも記載hashと一致して現存する。一方、**A01の2026-10-13 18:04 JSTのSurefire XMLは保存先`target/surefire-reports/`で後続caseに上書きされ、Repository／tmp内で現物を発見できなかった**。§8に記録されたA01 XML hash `D07F7E91C17DFFB0DF9CA7C96D3A4735DF1B833D4CDB65BF5A976312F42BDB9C`を現時点で再照合することはできない。A01のMaven log `tmp/r3a-implementation-20261013/nonweb-a01-20261013.log`（SHA-256 `B0825D05A4F534D9245478B06DC3D7223BAAAAE02CC93E91325E9BD146D5C2A1`）には対象gate・送信前PUBLISHED／stub到着0・一意のlock waiter、tests1／failure0／error0／skipped0、BUILD SUCCESSが残る。復旧子log `r3-recovery-1322060419523925086.log`（SHA-256 `A9265197B41C1A9B1EFDA2F409C56980466E7BA128B13392333526DC0F757FB7`）も現存する。**A01の当時の限定PASS記録を消さず、XML再照合だけは未成立として区別する。**

§14は通常root前にA01とA04dの既存XML hash照合を求めていたが、A01の現物欠落によりその条件を文字どおりには満たせない。よって現時点の通常root開始判断は**HOLD**。次のOwner判断では、A01の当時のMaven log・子log・§8同時代記録をXML欠落の代替証拠として明示採用し、rootの新しい有限枠とfresh source／環境preflightを定めるか、A01のXMLを新たに得るための**別枠の限定再実行**を先に選ぶかを決める。前者でも欠落した旧XML hashを再現・検証できたとは書かない。後者では旧PASSを上書きせず、新しいsource／artifact／run epochとして扱う。どちらも本点検から自動的に実行しない。

## 19 A01新規1回の再実行判断と結果（2026-10-14）

Ownerは§18の欠落を確認し、同じ固定JAR・現行sourceによる**A01新規1回だけ**の実行を指示した。過去の予算は流用せず、新規25分（preflight5分、A01・証拠保存15分、cleanup5分）を上限とする。今回所有JVM最大4・各heap768 MiB、PG1／Ryuk1／DB client8、PG memory1 GiB／CPU1／`max_connections=16`、開始前空きmemory8 GiB／disk10 GiB、raw log20 MiB／作業2 GiB／子log10 MiB、gate観測8秒・子起動120秒・所有元process停止5秒・通常cleanup30秒＋限定force5秒、追加再実行0回。A01 FAIL／BLOCKED、source／artifact不一致、資源・cleanup不成立なら停止し、code調整・通常rootを開始しない。fresh XMLは後続に上書きされないよう即時にtmpへ別保存する。初回FAILと§8のPASSを上書きせず、本実行を別epochにする。

開始前preflightではbranch `feature/phase4-s1-reference-full-stop-recovery-r3a`・HEAD `b064084a20cd7c1a82726241d8fb5970248d836c`、未commit16 file、§18のcode14 file source fingerprint `AA2AE5335BAE278AEA35CB8C9F50796EAA5C1A3E9FA42A76959728B50A78C487`を再照合した。固定通常JAR SHA-256 `8EEFA4CE1FAEC49BF755E71B696FCFD9BCB2871628F19F5F0A6F0F68837A46F9`・async JAR `20C6DDD19CA661460540C66FFAC861D955ABA5FAFE2605DB20FDE43521C52362`も一致し、ToolingのA01 test／DB gate compiled classは2026-10-14 11:41:08 JST版。JDK21.0.12、Docker Server29.1.3、稼働container0、`postgres:17` imageあり、開始前空きmemory `15102484 KiB`・C空き`535242502144` bytes、Java PIDは保護対象23472／24668／25088のみ。preflight成立により、A01新規1回の起動条件を満たした。

A01は12:07:13 JST終了、**tests1／failures0／errors0／skipped0、BUILD SUCCESS**。Maven logには対象trigger定義一致、holder PID70／一意のwaiter PID64・`Lock/advisory`、送信前`PUBLISHED/1`・provider到着0／受理0を記録した。test assertionは元process停止後のPUBLISHED維持、復旧後の対象COMPLETED・試行回数増分・completion date、同key provider論理受理1、承認状態と成功Audit維持を通過した。復旧子logは12:07:12 JSTに`Isolated publication recovery completed`を記録する。fresh XMLは**即時に**`tmp/r3a-a01-evidence-refresh-20261014/A01-PASS.xml`へ別保存し、SHA-256 `E87B635B137137D3BF13F23F80BAE53DDB53C7D2955DC92CFE4A93506D3DB1EF`、case名`A01PublishedBeforeSendCompletesOneTargetAfterFullStop`・tests1／failure0／error0／skip0を再読確認した。同dirの`A01.log`はSHA-256 `C5281982DD611E8A0B402E27C247C98BD3CA83CE7B84B31C92384614C941F00B`、復旧子log `tmp/r3a-implementation-20261013/r3-recovery-14824138213597425389.log`は`591402183F11BA5F9539FD967BDE8C36528DBAF7976CEB7B341BFBE45C01D0AB`。新XML＋Maven logは計21328 bytesでraw上限内、固定JAR・log・XMLはGit移送対象外。

cleanup後はDocker稼働container0、今回所有JVM0、Java PIDは保護対象23472／24668／25088のみ、空きmemory `15082308 KiB`・C空き`535229575168` bytes。実行中のJVM／DB client／Ryuk最大値は連続測定しておらず、Tooling途中sample `clients=3`を全時間の上限証明に読み替えない。固定JAR hash、16 file差分、HEADは不変で、`git diff --check`成立。§8の旧A01 XMLは依然として欠落し、その旧hashを再照合したとは書かない。今回のXMLは**別epochの新しいA01限定PASS証拠**であり、§3・§6のFAILと§8のPASSを履歴として保持する。通常rootの実行とR3-A結果のOwner受入、local commit、remote操作は今回の承認に含めず未実施。次は新しいroot実行枠・fresh preflightと結果の扱いを別判断する。

## 20 通常rootの新規実行枠案とfresh preflight（2026-10-14）

Ownerは§19を確認し、次の判断点として**新しい通常root実行枠とfresh preflight**へ進むよう指示した。本節では実行条件を具体化し、読み取り中心のpreflightを実施した。通常rootのMaven invocation、追加case、成果受入、commit、remote操作はまだ行っていない。§14の停止済み条件付き枠や§19のA01枠の残時間・再実行回数を流用しない。

### 20.1 新規通常root実行枠の判断案

通常rootを実行すると判断する場合の上限案は**新規30分**（実行直前照合5分、通常root15分、XML・JAR・log保全と集計5分、cleanup予約5分）。通常構成のroot `clean package`を最大**1 Maven invocation**、offline・追加再実行0回とする。`clean`前に既存の選択XML／logがtmpへ別保存されていることを再確認する。対象はroot reactorの通常構成であり、`s1-reference-async` profile、Tooling専用test、選択10件の再実行を含めない。専用92件のskipは実行件数へ加えず、R2の303実行という値も今回の予定実績とみなさない。

今回所有JVM最大4・各heap768 MiB、使い捨てPostgreSQL最大1・memory1 GiB／CPU1／`max_connections=16`、Ryuk最大2・DB client最大14、開始直前の空きmemory8 GiB／C空き10 GiB、raw log100 MiB／run2 GiB／子log10 MiBを維持する。Maven本体・compiler・test forkはheap上限を与え、test fork1／`reuseForks=false`／JUnit並列なし、通常root用DB接続上限を使う。JVMの`java.io.tmpdir`は今回作成したcanonicalな`tmp/r3a-root-preflight-20261014/jvm-temp`を全forkへ供給し、XMLのJVM propertyでも照合する。先行rootと今回の出力が混ざらないよう、root開始前の既存`target` reportは**別epochとして識別**し、今回生成したfresh XMLを直ちに別保存する。実行前後にbranch／HEAD・code14 file fingerprint・16 file差分を照合し、rootが作る通常JARは固定通常／async JAR対とは別のhash・epochで保存する。

判定はMaven `BUILD SUCCESS`だけに依存しない。全fresh Surefire／Failsafe XMLのpath・case・件数・failure／error／skip・hashを集計し、failure／error0、通常構成保護、専用skipの分離、通常JARの存在とhash、今回所有container／JVMの終了・cleanupを確認する。起動120秒、終了待ち30秒、通常cleanup30秒＋今回所有processへの限定force5秒。source／artifact・環境条件のずれ、任意のfailure／error、資源・時間・cleanupの不成立、秘密を含むlogの発生では停止し、同一集合を自動再試行しない。Docker／Rancher再起動、他session process／container操作、依存取得、code／test／設定訂正は本案に含めない。実行中資源の有限sampleは連続最大値の証明と区別する。

### 20.2 fresh preflightの実測

branch `feature/phase4-s1-reference-full-stop-recovery-r3a`、HEAD `b064084a20cd7c1a82726241d8fb5970248d836c`。未commit16 fileの内訳は§18と同じで、`git diff --check`成立。code14 fileのsource fingerprintは`AA2AE5335BAE278AEA35CB8C9F50796EAA5C1A3E9FA42A76959728B50A78C487`で§18・§19と一致した。固定通常JAR `8EEFA4CE1FAEC49BF755E71B696FCFD9BCB2871628F19F5F0A6F0F68837A46F9`、固定async JAR `20C6DDD19CA661460540C66FFAC861D955ABA5FAFE2605DB20FDE43521C52362`、§19の新A01 XML `E87B635B137137D3BF13F23F80BAE53DDB53C7D2955DC92CFE4A93506D3DB1EF`、A04d XML `BA2914426B051319E0F996BD770BD09C41FE940CC6E42BCD9E87416E6DC3F9B0`を再照合した。§8の旧A01 XMLは依然として欠落し、その旧hashを検証できたとは扱わない。

JDK21.0.12／Maven Wrapper3.3.4・配布Maven3.9.16、Docker Server29.1.3が応答した。`postgres:17`、`postgres:17-alpine`、`testcontainers/ryuk:0.14.0` imageが存在し、稼働container0。空き物理memory `15544448 KiB`、C空き`535225479168` bytesで上記下限を満たす。Java PID23472／24668／25088はVS Code Java／Spring拡張の保護processで、今回所有JVMはない。通常sandboxのDocker named pipe・Wrapper接続、CIM・継承TEMP読取はアクセス拒否されたため、同じ読み取り診断をRepositoryの承認済み権限付き手順で実施した。これはDocker Engine障害やroot test失敗とは扱わない。

新専用JVM temp `C:\KOIKI\koiki-java\KOIKI-JAVAWEB\tmp\r3a-root-preflight-20261014\jvm-temp`を作成し、Javaの`toRealPath().equals(path)`は**true**。継承TEMPの短縮名pathはfalseであり、rootで継承値を使わない。専用temp作成とprobe以外のcompile・DB／Docker起動は行っていない。

**preflightは本時点の読み取り条件として成立**。ただし通常root実行直前に空き資源・Docker・所有process・source fingerprintを再照合する。§20.1の上限・停止条件で通常rootを実際に1回起動する判断と、その結果受入はOwnerの次の判断点として残す。preflight成立だけで通常rootやR3-A限定結果をPASS／受入とはしない。

## 21 §20.1承認後の通常root限定実行結果（2026-10-14）

Architecture Ownerは§20.1の新規通常root実行枠案を確認し、承認した。承認範囲は通常構成のoffline `clean package` **1 Maven invocation**、fresh XML・通常JARの保全とcleanupである。追加再実行、async profile／Tooling専用case、結果受入、commit、remote操作は含めない。

起動直前にbranch `feature/phase4-s1-reference-full-stop-recovery-r3a`、HEAD `b064084a20cd7c1a82726241d8fb5970248d836c`、未commit16 file・code14 file fingerprint `AA2AE5335BAE278AEA35CB8C9F50796EAA5C1A3E9FA42A76959728B50A78C487`、固定通常／async JARと§19 A01／§13 A04dのXML hashを再照合し、一致した。選択case XML／logのtmp別保存とoffline clean plugin cacheも確認した。Docker Server29.1.3、稼働container0、開始前空きmemory `15488864 KiB`・C空き`535247425536` bytes。Java PID23472／24668／25088はVS Codeの保護processのみで、今回所有JVMは0だった。§20のcanonical専用JVM tempをMaven／test forkへ渡し、通常root DB selector `koiki.reference.normal-root.db-limits.enabled=true`、heap768 MiB、fork1／`reuseForks=false`／JUnit並列なし・JAR再生成を指定した。専用検証用 `koiki.reference.verification.resource-limits.enabled`やasync profileは有効化していない。

13:13:14〜13:16:45 JST、経過210.908秒でMavenは**Exit 0／BUILD SUCCESS**。通常JARを`tmp/r3a-root-run-20261014/reference-normal-root.jar`へ別保存し、SHA-256 `C76487126C648D81F3B2522D1EDD31960AE85ADCF3D46337EEA605D5F89FACB8`、64600471 bytes。固定JAR対とは**別のsource build／artifact epoch**として扱い、固定通常／async JARのhashや選択10件の結果を上書きしない。Maven logは同dir `normal-root.log`、SHA-256 `8CC9AA37DE151AD9FA60C8461AC5537788A518CBA5DD937DA5BB62C90B78D361`、1194054 bytes。

root reactorに属するfresh Surefire XMLは**62 file、395件＝303実行＋S1／B2専用92 skip、failure0／error0**。全62 XMLのpath・case・件数・個別SHA-256を`tmp/r3a-root-run-20261014/root-result.json`へ固定し、XML現物は同dir `reactor-reports/`へ保存した。result JSONのSHA-256は`DE4065B67EF3ED4A504C9704154341D35F9716FE7200EE60FF38464F0BB5AE7F`。JVM temp propertyを持つ52 XMLはすべて§20の専用canonical pathと一致した。`clean`後のroot reactor以外に既存Walking Skeleton XML 6件が残っていたため、初回の広いファイル探索にはこれらが混入した。初回の68件集計は`discovery-including-nonreactor.json`として履歴保全し、**今回のroot判定には含めない**。最終indexと`reactor-reports/`はroot reactorの62件だけである。

終了後のDocker Server29.1.3応答・稼働container0、今回所有JVM0、Java PIDは保護対象23472／24668／25088のみ。空きmemory `15213272 KiB`、C空き`534912770048` bytes。run保全領域は約72.5 MBで2 GiB上限内。実行中のJVM／Ryuk／DB clientの連続最大値は採取しておらず、開始・終了時とXML／logの観測から最大値を保証したとは扱わない。postflightでもbranch／HEAD、未commit16 file、code14 file fingerprintは不変で、`git diff --check`成立。

この1回の**通常root限定結果はPASS**。選択10件の別epoch限定PASSと合わせたR3-A結果全体のOwner受入、正式受渡し・R4／R5、local commit、remote操作はまだ判断していない。次はsource／artifact／run epochを区別して限定結果と差分・保全証拠を点検し、Owner受入の判断入力を整理する。

## 22 選択10件・通常rootのepoch別点検とR3-A限定受入判断入力（2026-10-14）

Ownerは§21を確認し、**受入判断より先に**選択10件と今回の通常rootをepoch別に点検するよう指示した。本節は保存済み証拠・source・差分の読み取り照合であり、case／rootの追加実行、code訂正、R3-A結果のOwner受入、commit、remote操作は行っていない。

### 22.1 選択caseのrun epochと保存証拠

次の10件の**採用する保全XML現物**を再読し、各SHA-256が記録値と一致、各XMLの`testcase.name`が対象methodと一致、すべてtests1／failures0／errors0／skipped0であることを確認した。各caseは表の別epochでの1 invocationであり、10件を同一Maven run・同一Tooling compiled sourceで一括実行したとは扱わない。Reference production7 fileは各case間で変更されず、固定通常JAR `8EEFA4CE1FAEC49BF755E71B696FCFD9BCB2871628F19F5F0A6F0F68837A46F9`・固定async JAR `20C6DDD19CA661460540C66FFAC861D955ABA5FAFE2605DB20FDE43521C52362`を適用対象に応じて使用した。Toolingは拒否理由assertionとA04a独立probeの訂正でepochが分かれる。

| run epoch・順 | 採用case | 保存XML・意味の入口 |
|---|---|---|
| 2026-10-13 §13 | A04d | `tmp/r3a-a04d-20261013/TEST-PackagedReferenceAsyncRecoveryTest-A04d.xml`。Tooling DB接続によるadvisory lock既占有時の単発復旧process拒否。復旧process同士の競合ではない |
| 2026-10-14 §15 | A02、A03 | `tmp/r3a-post-a04d-20261014/A02.xml`、`A03.xml`。到着前停止と独立受理後停止の各限定PASS |
| 2026-10-14 §16 | A04a訂正後 | `tmp/r3a-a04a-probe-20261014/A04a-PASS.xml`。復旧子専用provider probe到着0と停止未確認拒否を確認 |
| 2026-10-14 §17 | A04b、A04c、C02、D01、C04 | `tmp/r3a-remaining-five-20261014/`の各case名XML。古いsnapshot／別publication拒否、通常非接続、同一receipt、独立2通知を確認 |
| 2026-10-14 §19 | A01新規再実行 | `tmp/r3a-a01-evidence-refresh-20261014/A01-PASS.xml`。送信前PUBLISHED・元process停止／rollback・単一対象完了・論理受理1を確認 |

§15の**A04a初回FAIL** XMLもSHA-256 `B6BF4ECA42848739E6DD06C17F6CC6BB08D2FFE5CD783DA0D03129F3677238CD`、tests1／failures1として再確認し、訂正後PASSで消さない。§3・§6のA01停止／FAILと§8の初回PASSも当時の記録として残る。§8初回A01 XML現物は欠落したままで、その旧hashを再照合したとはしない。今回採用するA01は§19の**新XML**であり、SHA-256 `E87B635B137137D3BF13F23F80BAE53DDB53C7D2955DC92CFE4A93506D3DB1EF`とMaven log／復旧子logの記録hashが現物と一致した。A04d復旧子log、A04a訂正Maven log、固定JAR対もそれぞれ記録hashと再一致した。各caseのDB／provider意味は§13・§15〜17・§19のtest assertionと当時のlogに依拠し、XMLのPASS数値だけから再構成しない。

### 22.2 通常rootの別artifact epoch

§21の通常rootは選択caseの固定JAR対を再実行したものではなく、同じ現行code sourceから通常構成で新規`clean package`した**別artifact／run epoch**である。保全済み`root-result.json`のSHA-256 `DE4065B67EF3ED4A504C9704154341D35F9716FE7200EE60FF38464F0BB5AE7F`、index62件と`reactor-reports/`現物62件の個別hash不一致0を再確認した。303実行＋専用92 skip、failure0／error0、JVM temp propertyを持つ52 XMLの専用path不一致0。初回探索に混じったroot reactor対象外のWalking Skeleton既存XML6件は履歴indexに分離しており、通常root件数へ加えない。

通常root JAR SHA-256 `C76487126C648D81F3B2522D1EDD31960AE85ADCF3D46337EEA605D5F89FACB8`は固定通常JARの外側hashと異なる。一方、通常root JAR・固定通常JAR・固定async JARの`BOOT-INF/classes/`は各189 entryでpath・内容hashの差0。通常rootと固定通常JARのruntime library名は117件で一致し、固定async側のみModulith関連10件が追加される。通常rootと固定通常JARではKOIKI製SNAPSHOT依存JAR9件の**JAR全体のバイトhash**が異なるが、各JAR内部のファイル名・各entryの内容hash差は0だった。再packageによるarchive表現の差と整合するが、外側JARがbyte同一とは主張しない。通常構成がasync依存を暗黙に含むという証拠はなく、両epochのclass／runtime内容比較は成立した。

### 22.3 受入判断の境界

branchは`feature/phase4-s1-reference-full-stop-recovery-r3a`、HEADは`b064084a20cd7c1a82726241d8fb5970248d836c`。未commit差分16 file＝Reference production7、Tooling test7、文書2で§18と同じ。code14 file fingerprint `AA2AE5335BAE278AEA35CB8C9F50796EAA5C1A3E9FA42A76959728B50A78C487`、開始票hash `FD099A37AA5F2A37687CBC8CA4D311D8C6E292D60BA6C8A402C6AAA3B41D5068`は不変、`git diff --check`成立。Framework／Customer／POM／依存／migration／通常properties／CIの変更はない。本Evidenceは追記中なのでfingerprintに含めない。tmp内のXML・log・JAR・indexはGitでは移送されず、端末内の履歴証拠として別保管が必要。

以上から、**隔離した全停止型の単発復旧・拒否枝と通常構成保護というR3-A限定範囲の結果**はOwner受入の判断入力として揃った。ただし実運用の全instance停止手順・正式operator認可／Audit／permit、実provider照会・保持、UNKNOWN時の安全判断、R3-BのE07／E09／E10・E08残枝、R4／R5、DoD全体、正式受渡し、Phase 4全体は含めない。A04dは既占有lock拒否だけ、A04aは独立probeでの復旧子送信0だけであり、より広い並行運用保証へ拡張しない。各runの実行中JVM／Ryuk／DB clientの連続最大値も未測定で、終了時cleanupを全時間の上限証明へ読み替えない。

**状態（§22点検時点）：R3-A LIMITED RESULTS VERIFIED / OWNER ACCEPTANCE PENDING。** Ownerが上記の限定範囲・履歴FAIL／欠落・未達を確認して受入を判断する。受入後も、16 fileの成果固定対象の最終点検とlocal commit、remote pushはそれぞれ別判断とする。

## 23 R3-A限定結果のOwner受入承認（2026-10-14）

Architecture Ownerは§22を確認し、**「A04dは既占有lock拒否だけ、A04aは独立probeでの復旧子送信0だけであり、より広い並行運用保証へ拡張しない」ことを認識したうえで、§22の限定範囲の結果を承認・受入**した。これを**R3-A LIMITED RESULTS COMPLETE / OWNER ACCEPTED**として記録する。選択10件の別epoch限定PASSと、通常rootの303実行＋専用92 skip・failure／error0が受入対象である。

§3・§6のA01停止／FAIL、§8初回A01 XML欠落、§15のA04a初回FAIL、Tooling観測訂正と§19の新A01 XMLを履歴として保持する。A04dの既占有lockはTooling DB接続による注入であり、復旧process同士の競合や実運用での二重起動防止を認定しない。A04aは拒否された復旧子に独立probeへの送信がなかったことの確認で、元processを含む広い並行運用保証ではない。実provider配信・正式operator認可／Audit／permit、UNKNOWN時の照合・保持、R3-B、R4／R5、DoD全体、正式受渡し、Phase 4全体の受入や開始へ拡張しない。

この受入は、未commit16 fileの成果固定対象の最終点検、local commit、remote pushを兼ねない。次は§22.3の差分とGit移送されないtmp証拠の所在を確認し、成果固定の対象・方法を別判断する。今回の受入記録に伴う追加Maven／Docker検証は行わない。

## 24 R3-A成果のlocal commit候補点検（2026-10-14）

Ownerから「ここでコミットポイントでしょうか」との確認を受け、**commit操作は行わず**、§23で受入済みの限定成果とGit差分を読み取り点検した。branchは`feature/phase4-s1-reference-full-stop-recovery-r3a`、HEADは`b064084a20cd7c1a82726241d8fb5970248d836c`。`git status --porcelain`の変更・未追跡は次の**16 fileだけ**で、`git diff --check`成立。

| 所有・件数 | local commit候補 |
|---|---|
| Reference production 7 | `koiki-reference-app/src/main/java/org/koikifw/reference/notification/`の新規5 file（`JdbcIsolatedPublicationAdapter.java`、`SinglePublicationRecoveryService.java`、`IsolatedPublicationPort.java`、`IsolatedRecoverySelection.java`、`IsolatedPublicationRecoveryConfiguration.java`）と、`identity/configuration/`の既存2 file（`ReferenceApiSecurityConfiguration.java`、`ReferenceSecurityConfiguration.java`） |
| Tooling test 7 | `build-support/reference-e2e-verification/src/test/java/org/koikifw/buildsupport/referencee2e/`の既存`AsyncReferenceFixture.java`と新規`PackagedReferenceAsyncRecoveryTest.java`、`R3DatabaseGate.java`、`R3ProviderGate.java`、`R3RecoveryFixture.java`、`R3RecoveryProviderProbe.java`、`R3RecoveryTarget.java` |
| 文書 2 | `docs/development/phase4-s1-r3a-full-stop-contract-and-minimal-start-20261013.md`のA04d単一process契約訂正と、本Evidenceの停止履歴・10件／root・Owner受入記録 |

code14 file fingerprintは§22の`AA2AE5335BAE278AEA35CB8C9F50796EAA5C1A3E9FA42A76959728B50A78C487`で不変、開始票のSHA-256も`FD099A37AA5F2A37687CBC8CA4D311D8C6E292D60BA6C8A402C6AAA3B41D5068`で不変。本Evidenceは追記対象なのでこれらの事前hashへ含めない。Framework／Customer、POM／依存、migration、通常properties、CI、`AGENTS.md`の差分はない。

`tmp/`はGit ignore対象であり、固定通常／async JAR、選択case XML・Maven／子log、通常root JAR・62 XML・indexは**このcommitでは移送されない**。§13〜22に所在とhashを残し、必要な場合は端末外へ別途保全する。受入済み限定結果の履歴と再現可能なsource差分を固定するには、この16 fileを**1 local commitの候補**とするのが妥当。local commitの実行はOwnerの別指示またはOwner自身の操作とし、remote push／PR／mergeはさらに別判断とする。追加検証は不要。
