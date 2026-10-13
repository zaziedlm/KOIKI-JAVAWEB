# R2局所再throw訂正：早期成立と通常root停止（2026-10-13）

状態：§10のMaster対象2件・通常root、§11の通常／async JAR静的対照、§12の後段package7件がPASS。通常rootは303実行＋専用92skip、failure／error 0。[R2限定結果のOwner受入](phase4-s1-r2-selected-seven-case-results-and-acceptance-input-20261013.md#5-r2限定結果のowner受入承認2026-10-13)は成立。§4／§7のroot FAILは別epochの停止履歴として保持する。

**後続TEMP訂正（2026-10-13、§7）：** Ownerがcanonical専用JVM temp・失敗2件追加確認・成立時のroot追加1回を指示。path確認と失敗2件は成立し、追加rootで旧managed設定2件も成立した。一方、別のmaster DB fixtureでDocker応答読取のMalformedChunkCodingExceptionにより2 errorとなり再停止。旧FAILと新しい停止を別epochで保全し、後段7件は未実施。

## 1 承認・固定source・作成内容

Ownerは[限定訂正開始票](../../development/phase4-s1-r2-local-rethrow-limited-correction-start-review-20261013.md)について「この限定範囲での作成・検証開始を承認します」と明示した。新規45分（cleanup5分予約）と同票のscope・集合・source固定・停止条件を採用した。

branchは`feature/phase4-s1-reference-failure-isolation-r2`、原固定commitは`cfe9d9e`。今回の未commit採用差分と承認文書を含む1201 fileを`tmp/r2-local-rethrow-correction-20261013/source/`へsnapshotし、workspaceとcopyのSHA-256を全件照合した。実行後もsnapshotのhash不変更を確認した。commit IDだけで今回の修正sourceを表現しない。

新しい訂正差分は次の3 fileだけ。

- Reference `ExpenseWebExceptionHandler.java`：AccessDeniedException専用handlerで同じinstanceを再throw。優先順位annotation・独自resolver・Security設定を追加していない。
- Reference `ReferenceBusinessUrlSecurityTest.java`：API global adviceのauto-configurationを明示し、通常403／HTMX403／safe500の統合3件を追加。
- Tooling `AsyncReferenceFixture.java`：通常比較のexpected-statusを明示できる入力を追加。後段は403必須、旧500診断と区別する。

Framework production／API依存／Rules／POM／migration・拒否Auditは変更していない。AGENTSと開始票に個別承認を反映した。旧Tooling4 file、FAIL／比較XML・log・固定JARは保持し、今回source／実行epochと区別した。

## 2 preflight

Docker Engine29.1.3応答、稼働container0、空きmemory12.09 GiB／C空き498.11 GiB。保護対象Java30304（09:57:06.289039 JST）と25248（09:57:20.917818 JST）を記録した。

Maven本体／compiler／test forkのheap768 MiBを明示した。Playwright1.62.0のdriver JAR内browsers.jsonはChromium／headless-shell revision1234（151.0.7922.34）を指定し、新PCcacheの1234実体と一致した。browser起動は未実施。

## 3 早期3件

isolated snapshotからoffline、必要reactorを`-am`、forkCount1／reuseForks=trueで選択3件を初回1回実行。13:11:55 JST終了、Maven51.108秒、BUILD SUCCESS。fresh XML3件・failure／error／skip0。

| case | 結果 |
|---|---|
| returnsForbiddenForMethodDenialWithGlobalApiAdvicePresent | PASS。認証済みmethod拒否を通常403へ委譲 |
| preservesHtmxDenialResponseForMethodDenialWithGlobalAdvicePresent | PASS。HTMX403と既存safe partialを維持 |
| preservesSafeUnexpectedFailureFromGlobalApiAdvice | PASS。非Security RuntimeExceptionはglobal handlerのKOIKI-INTERNAL-001 safe500、内部detail非露出 |

`early-three-fixed.xml`をroot前に保全。mock serviceが投げた拒否のresolver／filter連携の成立であり、実method security・DB結果を含む後段package caseの代用にしない。

## 4 通常rootの結果と停止

同じsnapshotでoffline packageを初回1回、forkCount1／reuseForks=false、通常root用DB selectorだけをtrue、heap768 MiB・forceCreationを指定。Maven約200秒でBUILD FAILURE。

全62 fresh XMLを`normal-root-fixed-reports/`へcopyし、`correction-stopped-result.json`にcase名／hashを対応付けた。合計**395件＝303実行＋専用92skip、1 failure／1 error**。実行成功301件を含むが、root全体はPASSにしない。早期3件とrootの再実行分を独立成果へ二重加算しない。

失敗は次の既存fixtureで、局所handlerを変更したfileではない。

| case | 結果 |
|---|---|
| B2FrozenSourceRegistrationTest.C07_managedScopeTtlRemainIndependent | failure。managed設定snapshotのBean生成がManaged recovery configuration invalidで不成立 |
| ManagedRecoveryConfigurationLoaderTest.L01LoadsImmutableSnapshot | error。同じmanaged設定の正常読込が不成立 |

loaderは例外の内部path／causeを隠す既存契約なので、generic messageだけで原因を確定しない。root前の早期3件は成立しているが、通常rootの統合成立・package artifact・後段の403改善は未実証。

停止条件に従い、async package・通常／async artifact固定・後段7件・root再実行は行っていない。snapshot内のReference packageはtest段階で停止したので、新しい適格通常JARが作成済みとは扱わない。

## 5 読取診断：新PCのTEMP短名pathが原因候補

失敗XMLの実行propertyは`java.io.tmpdir=C:\Users\S-KATA~1\AppData\Local\Temp\`。新PCのTEMP／TMPも同じ8.3短名pathだった。既存test helperは@TempDirのpathにassignment.jsonを書き、そのpathをcanonical化せずloaderへ渡している。

production loaderは、絶対path・normalize一致・`path.toRealPath().equals(path)`・非間接pathを正常入力の前提として検査する。8.3短名pathがreal pathと一致しない場合、正常JSONでもこの検査で拒否される。二つの失敗が同じhelperを通ることと整合する。

**現時点はsourceと実行propertyに基づく原因候補。** 実際の失敗pathのtoRealPath値や拒否点を追加probeで採取していないので、TEMP短名だけを確定原因にはしない。notification保存・認可のproduction検査を緩める修正は行わない。

## 6 資源・cleanupと次の最小訂正候補

有限sampleはPG1／Ryuk1、Java4（既存2＋今回2）、空きmemory11.15 GiB。通常rootのRyuk2／client14のphase上限とR2専用のRyuk1／client8を区別する。DB clientの全期間上限は実証しておらず、sampleを連続保証へ読み替えない。

終了後の権限付きread-onlyでcontainer0、Javaは30304／25248だけ・creation不変更を確認した。今回資源cleanup成立。他sessionの資源は操作していない。

次の案は、今回run配下のcanonicalな専用JVM temp directoryを明示し、既存loaderのpath検査を保ったまま失敗2件を対象限定で確認すること。訂正の第一候補はJVM実行入力であり、production／fixtureを先に変更しない。

追加判断の候補は、(1)短名／canonical pathのread-only確認、(2)canonical java.io.tmpdirを指定した失敗2件の追加実行各1回、(3)成立時だけ同入力で通常rootの追加1回、(4)成立後に既承認の未実施後段へ進む順序。実際のtemp root・有限予算・追加回数と停止条件をOwnerへ提示してから実行する。旧予算残だけで再試行しない。

今回sourceのsnapshot・root FAIL・早期PASSを上書きせず、新たな訂正は別epochとする。既存fixture以外のpath間接参照・schema／Securityの緩和、APIのSecurity依存追加、Framework一般修正は対象外。

**追加再実行・上記環境入力訂正は未承認／未実施。** 修正source・文書は未commit。結果受入・commit／remote・R3〜R5は別判断。

## 7 Owner指示によるcanonical TEMP訂正と追加rootの別停止

Ownerは「canonicalな専用JVM tempを使い、失敗2件→成立時のみ通常root追加1回を確認する案へ進めます」と指示した。これに基づき`tmp/r2-local-rethrow-canonical-temp-20261013/`を新epochとして追加し、旧source snapshotを不変更で利用した。今回の追加はJVM実行入力のみ。production／fixture・path検査・期待値を変更していない。

訂正台帳の残下限1985秒（cleanup300秒予約）の範囲を上限として採用し、同じ許可を超える再試行をしていない。新epochのUTC wall-timeと残下限は`canonical-temp-root-stopped.json`へ記録する。文書・Owner待ちを含む保守的算定であり、残時間を追加実行許可にしない。

### 7.1 pathの直接確認と失敗2件の成立

Java21・heap768 MiBのread-only Path probeで次を確認した。

| 入力 | toRealPath | equals |
|---|---|---|
| `C:\Users\S-KATA~1\AppData\Local\Temp` | `C:\Users\s-kataoka\AppData\Local\Temp` | false |
| 今回run配下の`jvm-temp`絶対path | 同じpath | true |

この専用pathをMaven本体とtest forkのjava.io.tmpdirに指定。失敗L01／C07だけを各追加1回実行し、fresh XML各1件・failure／error／skip0、BUILD SUCCESS。Maven12.841秒、13:27:19 JST終了。既存loaderのcanonical検査を維持したまま両正常入力が成立した。

XMLは`ManagedRecoveryConfigurationLoaderTest-targeted-fixed.xml`と`B2FrozenSourceRegistrationTest-targeted-fixed.xml`、propertyのjava.io.tmpdirを照合した。Java path不一致と、変更した実行入力による2件の成立から、旧managed設定失敗には短名TEMPによる非canonical入力が関与したと判断する。独立した全因果枝の網羅検証は行っていない。

### 7.2 追加通常rootとDocker読取エラー

同じcanonical TEMP、同じsnapshot、heap768 MiB／fork1／reuseForks=false／通常root用DB制限で、承認された追加root1回を実行した。Maven約216秒、BUILD FAILURE。62 fresh XMLを`root-additional-fixed-reports/`へ保全し、395件＝303実行＋専用92skip、failure0／error2を照合した。旧L01／C07はこのrootでも成立した。

2 errorはMasterPostgreSqlIntegrationTestの次の2件。

- `ownsSeparateHistoryAndHasNoReferenceToFrameworkForeignKey`：ApplicationContext作成時にTestcontainersのDocker検出が失敗。
- `appliesV1AndKeepsMasterMutationAvailabilityAndAuditConsistent`：同じcontextのfailure thresholdにより再読込されずerror。独立したDocker検出失敗2回ではない。

13:31:28.496 JST、PID32936のNpipeSocketClientProviderStrategyで`MalformedChunkCodingException: Bad chunk header`が発生し、valid Docker environmentなしと判定した。context起動前のDocker応答読取で、局所再throwやmanaged設定の検証失敗とは別である。OS権限拒否やDocker Engine停止と断定しない。

後続MasterReadModelPostgreSqlIntegrationTestの別fork PID25752は13:31:34.581に同じnpipeでDocker environmentを検出し、成功した。終了後の権限付き最小診断でもDocker Server29.1.3応答・container0を確認した。これだけで断続的なchunk読取エラーの原因・再発防止を確定しない。

### 7.3 cleanupと残作業

root終了直後の読取ではJava4960／9708が一時的に残っていた。その後のPID・creation／command照合時には両方終了していた。force終了は行っていない。最終`final-cleanup-and-docker.json`でcontainer0、Javaは保護対象30304／25248のみ・creation不変更を照合した。即時に全cleanup済みだったとは記録せず、終了直後と最終観測を区別する。

追加rootも全体PASSにしていないので、適格通常／async artifact固定・後段7件へ進まず停止した。旧artifactや旧正常結果を新artifact入力へ差し替えていない。commit／pushは未実施。

次に必要な最小判断は、同じDocker Java client／npipeのread-only info診断、成立時のmaster対象2件と追加rootの有限再確認を行うかである。rootの反復、Docker再起動・他container操作、依存更新・schema／Security緩和を自動採用しない。**さらに追加の診断test／root再実行は未承認・未実施**。今回許可された2件＋root1回は完了した。

## 8 Rancher DesktopログとDocker接続の読取診断（2026-10-13）

Ownerの「Rancher DesktopのログとDocker接続状態を読み取り確認」指示に従い、追加test・container操作・エンジン再起動なしで診断した。通常sandboxでの`docker version`／`docker info`は`.docker/config.json`と`npipe:////./pipe/docker_engine`へのアクセスを拒否されたため、AGENTS.mdの承認済み手順どおり権限付きで同じread-only診断を実行した。この拒否自体をエンジン障害とは判定しない。

14:00 JST頃の権限付き診断で`docker version`はServer `29.1.3`／API `1.52`、`docker info`はLinux・overlayfs・container総数9／稼働0を返した。`docker ps`も稼働中0件。総数9は停止済みcontainerを含むDockerの現在値であり、今回の操作による作成・削除はない。

`%LOCALAPPDATA%\rancher-desktop\logs\docker.log`と`rancher-desktop-guestagent.log`を13:31 JST前後で読んだ。daemonログでは13:31:22に既存shimの終了処理、13:31:33に別の終了処理、13:31:35に次のshim接続とcontainer起動が記録されている。失敗時刻13:31:28.496のdaemon停止・再起動・`Bad chunk header`自体の行は見つからなかった。guestagentログには13:31:09以降、IPv6 HostIP `::`のIPv4未取得と、ポート転送で`proxy already running`が複数記録される。これらは観測された別の警告・エラーであり、Java Docker clientのMalformedChunkCodingExceptionを発生させた原因とは特定できない。

したがって現時点ではエンジン再起動を必要とする停止状態の証拠はない。同一Java client／npipeでの断続的な応答読取問題は未解明であり、CLIの現在応答だけで後段の安定性を保証しない。追加master対象test・通常root再実行は、回数・予算・停止条件を定める次のOwner判断まで実施しない。R2のpackage後段7件と受入も未実施のまま維持する。

## 9 再実行検証の限定判断案（提案時点。§10で承認・実施）

§7のrootエラー2件は`MasterPostgreSqlIntegrationTest`の2テストで、最初のDocker検出失敗と同じApplicationContextのfailure thresholdによる後続errorである。§8ではDocker CLIの現在応答を確認したが、失敗したJava Docker clientの接続安定性は未確認。エンジン再起動を先行する根拠はなく、同じclientを使う対象testで確認するのが次の最小案である。

1. 新しい実行epochを作り、§7で使ったisolated source snapshot・`source-inputs.json`のhashを再照合する。JDK／Wrapper、Docker Server・image、空きmemory／disk、保護対象Java・既存containerを読み取り確認する。新epoch専用のcanonical JVM tempを作成し、Java `toRealPath().equals(path)`を確認する。既存の停止XML・JAR・logは上書きしない。
2. `MasterPostgreSqlIntegrationTest`クラスだけを**1 Maven invocation／2テスト**で実行する。これは失敗箇所のJava Docker client／npipeとDB fixtureを通す診断兼対象回帰であり、2件それぞれのfresh XML、失敗／error数、Docker検出結果、今回所有container／Javaのcleanupを確認する。
3. 上記2件がPASSし、資源・cleanup条件も成立した場合に限り、同じsourceとcanonical JVM temp設定で**通常rootを追加1回**実行する。既存の通常root用DB制限、fork1／reuseForks=false、Maven本体・compiler・test fork heap各768 MiB、専用92skipの分離を維持する。全fresh XMLと実行／skip／failure／errorを保全し、途中の対象2件PASSだけでroot PASSとは扱わない。

この追加判断の案は**新規30分、うちcleanup5分予約**。preflight5分、対象2件5分、条件付きroot10分、Evidence5分、cleanup5分を上限配分とする。今回所有JVM4以内・PG1、対象test／通常rootのRyuk2・DB client14以内を維持し、無関係な停止container9件には触れない。起動120秒、失敗／完了30秒、子process終了30秒と限定force確認5秒の既存上限を延長しない。

source／temp不一致、Docker接続・port到達の失敗、対象2件のいずれかの失敗、rootの任意のfailure／error、資源／時間／cleanup条件不成立で直ちに停止する。エラーが偶発的に見えても同一集合の自動再試行は0回。Docker・Rancher Desktopの再起動、設定／依存／test期待値変更、他container操作は含めない。root成立後の通常／async packageと後段7件は、この追加実行の成果と残予算を照合して別途判断する。今回の案は追加実行のOwner判断入力であり、§7の許可回数や残時間から実行を開始しない。

## 10 §9のOwner承認と限定再実行結果（2026-10-13）

Ownerは「9 再実行検証の限定判断案を確認し、これを承認します」と明示した。新epochは`tmp/r2-local-rethrow-master-recheck-20261013/`。§9のMaster class1 invocationと、成立時だけの通常root1 invocationを実施した。Docker／Rancher Desktopの再起動、source・test・依存・設定の変更、追加の反復は行っていない。

### 10.1 preflightと対象2件

旧isolated snapshotの`source-inputs.json`にある1201 fileを全件SHA-256再照合し、不一致0。JDK21.0.12／Wrapper指定Maven3.9.16、空きmemory12.84 GiB・C空き496.93 GiB、PostgreSQL17-alpine・Ryuk0.14.0のimage実体を確認した。保護対象Java30304／25248のcreationは旧記録と一致した。新epoch専用JVM tempの`toRealPath().equals(path)`はtrue。通常sandboxで継承TEMPの`toRealPath`を読むprobeが拒否されたため、同一の読取probeを権限付きで再実行し、継承短名はfalse、新専用pathはtrueと確認した。これは対象testの追加試行ではない。

固定sourceから`MasterPostgreSqlIntegrationTest`クラスをoffline・1 invocation・fork1・heap768 MiBで実行。2件ともPASS、failure／error／skip0、Maven22.235秒。fresh XMLを`master-class-fixed.xml`へ保存し、SHA-256は`CA0DB9A57EB9BF6FD8DF8E5C6F4F72747E12EDB6DD435D4750412936116FD842`。XMLの`java.io.tmpdir`は新専用pathに一致した。この2件は前回のDocker検出失敗とcontext failure thresholdが起きた同じclassである。対象終了後は稼働container0、Javaは保護対象2件のみ。

### 10.2 条件付き通常root

対象2件の成立とcleanupを確認後、同じsource・canonical tempで通常rootを追加1回実行。通常root用DB制限、fork1／reuseForks=false、Maven本体・compiler・test fork heap各768 MiB、専用92skipを維持した。Maven3分18秒でBUILD SUCCESS。**62 fresh XML、395件＝303実行＋専用92skip、failure0／error0**。前回rootでerrorとなったMaster class2件、旧TEMP失敗L01／C07を含むmanaged設定2件、局所再throwのMVC統合3件を含む。rootの全XMLは`root-fixed-reports/`へcopyし、`root-result.json`へpath・件数・SHA-256を固定した。root XMLに記録されたJVM temp52件は全て今回の専用pathで一致した。今回logには`MalformedChunkCodingException`／`Bad chunk header`は見つからなかった。

通常rootの出力JARは`normal-root-fixed.jar`として別保存し、SHA-256は`15C490E3439FFA43DE4E686F04F7A730698DE9B09150641A160990BA35695296`。これは今回root成果の保存であり、通常／async artifact対の内容・依存分離や後段package caseの成立を意味しない。source1201 fileの事後hash不一致は0。

### 10.3 cleanup・判断境界

root終了後の権限付き読取で稼働container0、Javaは保護対象30304／25248のみでcreation不変更。`epoch-result.json`に開始UTC、結果、経過上限472秒／新規30分、cleanupと未実施後段を記録した。今回の2 invocationはいずれも1回のみで、開始前の空き資源・指定したJVM上限・終了時cleanup・時間条件が成立した。実行中のRyuk／DB client／JVM数の連続上限は採取しておらず、終了時の観測をその保証に読み替えない。

今回の限定再実行は**PASS**。以前のDocker応答読取エラーが再発しなかった事実は確認したが、その根本原因や将来の安定性を確定しない。R2失敗分離の全体受入、通常／async artifact対の照合、通常能力不足403、R2-04、直接回帰4件、Critical Journey C01は未完了。§9どおり、後段package7件の範囲・予算・入力artifactをこの結果に基づいて別途判断する。commit／push・R3〜R5は行っていない。

## 11 Owner指示による通常／async JAR静的対照（2026-10-13）

Ownerは「同じ固定sourceからasync JARを1回作成し、保存済み通常JARとのアプリ内容・runtime依存の差を確認へ進めます」と指示した。新epoch`tmp/r2-local-rethrow-jar-pair-20261013/`を作成し、build1回・test0回・root0回、新規15分うちcleanup3分予約、差異不明時は停止・自動再package0回とした。旧isolated snapshotのsource1201 fileのhash不一致0、保存済み通常JARのSHA-256一致、空きmemory12.39 GiB／C空き496.6 GiB、保護対象Java2件のcreation一致を開始前に確認した。Docker操作は不要だった。

同じsnapshotのReferenceと必要reactorを`-am`でoffline packageした。async profile `s1-reference-async`、`maven.test.skip=true`、`maven.jar.forceCreation=true`、Maven本体／compiler heap768 MiB上限。**1 invocation、Maven19.085秒、BUILD SUCCESS**。全testはskipされ、今回の実行結果へ数えない。新JARを`async-reference-fixed.jar`へ保存し、保存済み通常JARは不変更のまま保持した。

| 対象 | SHA-256／照合結果 |
|---|---|
| 通常root由来JAR | `15C490E3439FFA43DE4E686F04F7A730698DE9B09150641A160990BA35695296`、nested runtime JAR117件 |
| 今回async JAR | `948F0C79F5D8354826A25A47015C0AD9671843B84911899B390E69DF21C15FA6`、nested runtime JAR127件 |
| アプリ内容 | `BOOT-INF/classes/`の181 fileはpath・内容SHA-256すべて一致。source main resources22件は両JARと一致。`META-INF/spring.factories`はBoot JARのroot配置で照合 |
| runtime差 | 通常から削除0、async追加10件。`spring-modulith-events-jdbc-2.1.1.jar`は通常になくasyncにあり、追加10件はModulith関連の解決集合と一致 |
| 共通依存 | 外部nested JARはarchive SHA-256一致。再作成した内部KOIKI JAR9件はarchive hashが異なるが、各内部entryのpath・内容hashは一致 |
| 配布境界 | KOIKI ArchUnit Rules、koiki-testing、JUnit、Testcontainers、Playwright等のtest専用nested依存なし。追加の`archunit-1.5.0.jar`はModulith側のruntime解決分として区別 |

静的照合の初回スクリプトは`spring.factories`を`BOOT-INF/classes/`下に探したためsource resource1件を不一致と表示した。実JARには両方とも`META-INF/`直下にあり、配置を正しく指定して同じJARを再読取するとsource22件すべて一致した。build／testの再実行やJAR変更はない。入力・出力・全差分は`epoch-inputs.json`、`jar-pair-comparison.json`、`epoch-result.json`、package logに保存した。

比較後はsnapshotの作業用Reference targetを保存済み通常JARへ戻し、通常／async両固定JARとtargetのhashを確認した。source1201 fileの事後hash不一致0、今回所有Javaは終了し、保護対象30304／25248のみ維持。今回の**JAR静的対照はPASS**。async起動・V5実行・R2-04の403／副作用なし・直接回帰4件・Critical Journey C01の実動作成立は示していない。後段package7件は別の範囲・予算・停止判断を経て実施する。commit／pushは行っていない。

## 12 後段7件の限定実動作検証（2026-10-13）

Ownerが[後段7件の限定開始判断案](../../development/phase4-s1-r2-post-jar-pair-seven-case-owner-review-20261013.md)を承認し、§11と別のepochで通常JAR能力拒否比較、R2-04、provider直接回帰D01／D02、C02／C04、既存Critical Journey C01を順に各1回実行した。**7／7 PASS**、各fresh XMLはtests1・failure／error／skip0。固定source1201 fileと通常／async JARのhash一致を開始前・各case前・終了後に確認し、最終cleanupは稼働container0、保護対象Java2件のみ。R2-04直後のcontainer1件は後続読取で自然消滅を確認した。結果、case別XML hash、予算と観測限界は[専用結果文書](phase4-s1-r2-selected-seven-case-results-and-acceptance-input-20261013.md)に固定した。

§11時点の「後段未実施」は同節の静的対照epoch終了時点の記録であり、今回の7件成立により後続の実動作確認が追加された。過去のroot失敗と初回R2-04の500失敗は別epochの事実として保持する。本節作成時点でOwner受入は未判断だったが、後に[結果文書§5](phase4-s1-r2-selected-seven-case-results-and-acceptance-input-20261013.md#5-r2限定結果のowner受入承認2026-10-13)で承認された。成果commit／push、R3〜R5は未実施・未判断。
