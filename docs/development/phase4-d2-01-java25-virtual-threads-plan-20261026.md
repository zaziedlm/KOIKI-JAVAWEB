# D2-01：Java 25 Virtual Threads対応方針・比較検証計画

**状態：** D2-01の§8〜12をOwner確認済み／§14に沿った見直し・推進指示済み（2026-10-26）。D2-02は構成・Tooling訂正を経て43論理checkpointの限定検証成立、Owner結果確認済み・D2-03へ進行指示。[D2-03](phase4-d2-03-ci-and-adoption-guide-20261026.md)はCI系統・ガイド・正本整合を実装し、ローカル6構成／43項目成立、実CI待ち。[最終結果・制限§9〜11](../architecture/validation/phase4-d2-02-java25-virtual-threads-results.md#9-構成訂正tooling診断の経緯)。VT既定無効を維持し、実CI／DoD4-11・正式提供は未成立。§13〜14の停止・途中状態は履歴として保持する。

**開始指示：** 2026-10-26、Ownerは専用branchでのD2-01開始を指示。強い採用推奨を目的とせず、Java 25以降で本格利用する際に対応できることを確認したい、と意図を明示した。

**基点：** `main` / `1284ba08a9e0cb44c67487dc02148e378699ceb4`、タグ `checkpoint/v0.1.6-phase4-development-baseline`。

**作業branch：** `docs/d2-01-java25-vt-verification-plan-20261026`。

**Ownership：** Frameworkは適用条件、Referenceは既存業務操作、Toolingは観測・比較harnessを所有する。

## 1. 目的とJava対応方針案

案件がJava 25上でVTを選択したときに、既存の業務・認証・監査契約を維持して利用できる条件と制限を示す。高速化率や全案件での採用、既定有効化を成功条件にしない。OwnerのJava 25への期待は検証動機として扱い、依存libraryを含めた安定性の実証結果とは区別する。

| 構成 | D2での位置づけ |
|---|---|
| Java 21 target / build | 現行baselineを維持。同一のJava 21生成JARを比較に用いる |
| Java 21 runtime / VT無効 | 最低実行環境の回帰確認 |
| Java 25 runtime / VT無効 | VT比較の対照群。Java更新の影響とVTの影響を分ける |
| Java 25 runtime / VT有効 | 本格利用を検討する案件向けの検証対象。`spring.threads.virtual.enabled=true`を明示する |
| Java 21 runtime / VT有効 | 案件向け推奨対象に含めない。診断上の必要が具体化した場合だけ追加を判断 |
| Java 25より後のruntime | 今回のJava 25の成功から一括認定しない。採用版・Boot・driverの対応を確認し、同じ検証を適用する |

VTは既定無効を維持する。対応状況は開発baselineの検証範囲として示し、正式release・support認定はE1等の判断へ接続する。

## 2. 静的調査で確認したこと

| 入力 | 確認内容と扱い |
|---|---|
| `koiki-parent/pom.xml` | `maven.compiler.release=21`。Java 25で再buildして比較しない |
| グランドデザイン§21.5 / §23.3 | CI表はJava 21＋VT有効、有効化チェックリストはJava 25以上。主たるVT系統をJava 25へ合わせる訂正案を用意する。正本は今回未変更 |
| `.github/workflows/runtime-compatibility.yml`、`build-support/runtime-compatibility-fixture/README.md` | 同一Java 21生成CLI JARの21/25実行系統は既存。ReferenceのWeb処理やVT有効化を実証するものではない |
| `KoikiObservabilityAutoConfiguration` | `requestId`を対象とするContext Propagation用TaskDecoratorがある。Beanの存在だけで全executorへの適用やSecurityContext伝播を保証しない |
| `NotificationAsyncConfiguration` | S1通知は独自の`ThreadPoolTaskExecutor`（core/max 1、queue 16）を登録する。VT propertyだけで通知処理までVTになると扱わず、S1の再設計・再検証を今回へ持ち込まない |
| Reference通常設定 | `application.properties`にはVTの明示設定がない。比較起動時に有効／無効を明示し、実際の実行threadを観測する |

一次情報の照合結果・取得制限・対象版は§8へ記録した。JEP 491による改善と、全pinning・全依存の問題解消を同一視しない。

## 3. 最小比較案

主比較はJava 25上のVT無効／有効。同一source・JAR SHA-256・DB初期fixture・pool設定・JVM上限・負荷条件を用い、別processで順次起動する。Java 21無効は回帰対照とし、21対25の差をVTの効果としない。条件を変えた結果は別の試行として識別する。

| 集合 | 操作・観測 | 合否の根拠 |
|---|---|---|
| 起動・適用 | Java版、実効property、HTTP request処理内の`Thread.currentThread().isVirtual()`相当の証拠 | VT有効側で実際にVTが使われ、無効側ではplatform threadである。propertyの表示だけでPASSにしない |
| 正常・業務 | Session認証付きexpense参照と既存更新操作。結果・DB・Business Auditを照合 | 有効／無効で既存業務契約を維持する。fixtureを戻して比較し、二重更新による差を混ぜない |
| 認証・分離 | 未認証、CSRF不正、scope外拒否、異なる二userの並行要求 | 期待した拒否とuser／requestIdの分離。別要求へのcontext漏えいはFAIL |
| 待機・資源 | DB poolを固定した通常並行負荷とpool待機が生じる有限負荷 | 接続上限、待機・timeout、要求終了後の解放。無制限受付や処理滞留を推奨構成として残さない |
| 診断 | 応答時間分布、throughput、エラー、platform／virtual thread、DB active／pending、CPU／memory、利用可能なJFR観測 | 計測条件と欠測を記録。高速化しなくても契約成立と制限を説明できれば対応判断の入力とする |

Web request内のSecurityContext確認と、別executorへのcontext伝播は別の検証とする。同期Webを初回範囲とし、executorを跨ぐ経路は採用対象に含める必要がある場合だけ、executor種別・decorator接続・伝播対象を明示して追加する。D1のtrace／exporter全体を待たず、今回必要なrequestIdと資源観測を用意する。

C2の外部待機は初回範囲から外す。今回必要なblocking I/O・pool待機は既存expenseのJDBC経路と隔離DBの有限ロックで確認する（§10）。S1非同期、Batch、Container／ECS、全組合せは各packageへ残す。

## 4. 対象path・契約差分と実施段階

- **D2-01現在：** 本計画と統合計画の導線を更新する。production・POM・CI・公開APIの差分なし。静的照合のみ。
- **D2-02候補：** 非配布Toolingを`build-support/`配下に置き、固定Reference JARの起動・負荷・計測・cleanupを担当させる。既存`build-support/reference-e2e-verification/`の認証・fixtureを調査して必要部分を再利用する。内部threadの証拠を得る観測方法は、正式JARへtest routeを入れない方法を先に検討する。
- **D2-03候補：** 実CIのVT系統、有効化ガイド、提供機能ガイドの対応条件、グランドデザインの整合訂正。既存21/25互換系統をVT実証済みへ読み替えない。DoD 4-11はVT有効系統の実CI成功で判定する。

新規独自executor・Framework wrapper・流量制御APIの追加を初期解としない。既存Spring標準と接続先pool／既存制限で評価し、不足が実証された場合に公開契約差分と必要修正を別に具体化する。

## 5. 環境・資源・停止／cleanupの初期案

以下は初版時点の候補。現在の確定提案は§11とし、JVM数・負荷上限等は§11を適用する。

クラウドでの計画具体化は可能。直前の環境確認ではJava 21、Maven 3.9.16、4 CPU相当・16 GiBを確認済み。Java 25の追加準備とDockerの現在の疎通、必要依存・imageの利用可否は実行前に確認する。前回mainの通常root成功はD2の結果に数えない。

初回実行案はアプリJVM 1、負荷／観測JVM最大1、PostgreSQL 1（Testcontainers採用時はRyukを別計上）、アプリheap最大768 MiB、DB pool最大4、負荷同時要求1と8、未完了要求は最大8、各計測60秒・warmup30秒。負荷側は有限timeoutと終了期限を持ち、終了した要求だけ補充する。これは設計候補であり、既存fixtureとの整合とpool飽和時の挙動を確認して確定する。

D2-01の残作業はharness・一次情報照合と計画確定で1〜2時間、D2-02は実装・検証で4〜8時間の暫定見積。実行自体の時間枠案はpreflight 10分、build・必要回帰20分、比較20分、cleanup 5分の計55分。D2-03のCI変更・実CI時間は比較設計確定後に見積もる。過去のS1／C2予算を流用しない。

検証前にsource commit、JDKの配布元・patch版・取得物checksum、JAR checksum、fixture、設定、検証ケース／回帰集合を固定する。秘密値やSessionは証拠へ出力しない。起動失敗・認可差異・context漏えい・上限超過・期限超過・cleanup不成立で停止し、原因と影響を記録する。無条件の再実行や資源上限拡大は行わない。

終了時は今回所有の子process・container・fixtureだけを停止／削除し、残存process・container・DB接続を確認する。raw計測・log・集計・source／artifact対応はリポジトリ外へ保存し、結果文書から参照する。共有クラウドの計測は同条件内の相対比較であり、案件の本番SLOは配備環境で評価する。

## 6. D2-01の完了条件と次の着手点

1. Java 21 build／最低runtimeとJava 25 VT opt-inの関係、将来版の再確認方針を明文化する。
2. 一次情報と実効依存を照合し、正本§21.5／§23.3の訂正案を具体化する。
3. 実在する代表操作・harness・観測方法・対象file・新規検証／直接回帰集合を特定する。
4. 資源・計測条件・上限・時間・停止／cleanupとD2-02見積を確定する。
5. D2-02の開始判断に必要な具体的差分を提示できる状態にする。文書作成をVT対応認定や実行結果の受入と扱わない。

2026-10-26に一次情報・既存harness・JAR内の実ライブラリを照合し、§8〜12へ対象・方法・判定・資源・実装pathを具体化した。その後、Owner確認・D2-02開始と§14に沿う見直し・推進指示を経て、43論理checkpointの限定検証が成立した。次は結果・制限のOwner確認を入力にD2-03へ進む。D2-02の経緯は§13〜14と結果記録を参照し、具体化時点の未着手記述を再開入口にしない。commit／push／PRは未実施。

## 7. 関連入口

- [承認済み推進計画§9](phase4-review-integrated-task-plan-20261020.md#9-初期標準確立に向けた見直し推進計画owner-approved2026-10-23)
- [提供機能・適用条件・選択ガイド](framework-capabilities-and-selection-guide.md)
- [Baseline Compatibility](../architecture/KOIKI-JavaWeb-FW_Baseline_Compatibility_v0.1.md)
- [グランドデザイン](../architecture/grand-design/KOIKI-JavaWeb-FW_グランドデザイン_v0.2.md) §21.5、§23.3、DoD 4-11

## 8. 一次情報と実際の構成の照合（2026-10-26）

| 一次情報 | 確認事項・本計画への反映 |
|---|---|
| [Spring Boot v4.1.1：Virtual threads](https://raw.githubusercontent.com/spring-projects/spring-boot/v4.1.1/documentation/spring-boot-docs/src/docs/antora/modules/reference/pages/features/spring-application.adoc) | 採用版のtagで確認。技術的にはJava 21以上、Java 24以上を推奨し、propertyで有効化する。KOIKIのJava 25開始という利用方針とは両立する。daemon性と`spring.main.keep-alive`の案内も確認 |
| [Spring Boot v4.1.1：Task Execution](https://raw.githubusercontent.com/spring-projects/spring-boot/v4.1.1/documentation/spring-boot-docs/src/docs/antora/modules/reference/pages/features/task-execution-and-scheduling.adoc) | 独自Executorによるauto-configurationのback-offと`applicationTaskExecutor`の役割を確認。HTTP request threadと非同期executorを区別する |
| [v4.1.1：Tomcat構成](https://raw.githubusercontent.com/spring-projects/spring-boot/v4.1.1/module/spring-boot-tomcat/src/main/java/org/springframework/boot/tomcat/autoconfigure/TomcatWebServerConfiguration.java)、[VT customizer](https://raw.githubusercontent.com/spring-projects/spring-boot/v4.1.1/module/spring-boot-tomcat/src/main/java/org/springframework/boot/tomcat/autoconfigure/TomcatVirtualThreadsWebServerFactoryCustomizer.java) | VT条件でTomcat protocol handlerへVirtualThreadExecutorを設定する実装を確認。独自Web executorは追加しない |
| [v4.1.1：PropertiesLauncher](https://raw.githubusercontent.com/spring-projects/spring-boot/v4.1.1/loader/spring-boot-loader/src/main/java/org/springframework/boot/loader/launch/PropertiesLauncher.java) | `loader.path`で外付けJARを読み込み、元のBOOT-INF/classes・libも用いる。正式JARを改変しないTooling観測に利用する |
| [Oracle Java 25：Virtual Threads](https://docs.oracle.com/en/java/javase/25/core/virtual-threads.html) | VTは処理そのものを高速化するthreadではない。native／foreign functionのpinning、JFRのPinned／SubmitFailed、Start／Endの既定無効、VTを含む`Thread.dump_to_file`を確認。速度向上やpinning不存在を合格条件にしない |
| [pgJDBC：並行利用](https://jdbc.postgresql.org/documentation/thread/)、[42.7.13 release](https://jdbc.postgresql.org/changelogs/2026-07-06-42.7.13-release/) | 同一Connectionの無制御な並行共有を行わない。既存transaction／Hikari貸出境界を維持する。release情報だけでVT対応済みと認定しない |

最新版へ追従するSpring資料では4.1.2の表示があったため、採用版に関わる根拠はv4.1.1の公式tagへ固定した。JEP 444本文は検索要約のみ、JEP 491本文・OpenJDK issueは取得403で、本文の再確認済みとはしない。Java 25公式ガイドとBootの採用版実装を今回の検証設計の根拠とする。JEP本文に依拠した正本の根拠説明を改訂する場合は再確認を残す。この取得制限はローカル検証が失敗したことを意味しない。

保存済みReference JARのmanifestと`BOOT-INF/lib`を静的に確認した。Java-Version／Build-Jdk-Specは21、Boot 4.1.1、Spring Core 7.0.9、Tomcat 11.0.24、HikariCP 7.0.2、pgJDBC 42.7.13を含み、PropertiesLauncherも存在する。これは保存artifactの構成確認であり、新規build・Java 25での動作確認ではない。実行対象はD2-02でsourceとchecksumを改めて固定する。

### 8.1 正本訂正の具体案

D2-03でグランドデザイン§21.5の互換系統2を「PostgreSQL／Java 25 runtime（VT有効）／PR・main push・手動実行／Phase4」へ変更し、「Java 21主系統で生成した同一成果物を再コンパイルせず実行する」を互換系統2にも明記する。互換系統1のVT無効対照を維持する。実workflowの起動条件・実行時間・費用・required check扱いはD2-03で確定する。2026-10-26のOwner指示により、VT専用workflowはスケジュール実行を行わない。

§23.3は既定無効とJava 25以上の利用条件を維持し、将来のJava版は個別互換検証を要する旨を補う。TaskDecoratorの適用先、同期request内とexecutor間のcontextの違いを明記する。`@ConcurrencyLimit`等のチェックリスト項目は、負荷生成側の制限だけで充足したとしない。今回正本の本文は変更していない。

## 9. 観測方式と既存harnessの再利用範囲

### 9.1 同じReference JARの二つの起動方式

各構成A＝Java 21／VT無効、B＝Java 25／VT無効、C＝Java 25／VT有効について、通常起動と観測起動を一回ずつ、計6 processを順次実行する。

1. **通常起動：** `java -Xmx768m -jar <reference.jar>`。追加観測JARなし。実HTTPでlogin、Session APIのcreate／read／submit、MVC表示、DB／Auditを確認する（N01）。
2. **観測起動：** `java -Xmx768m -Dloader.main=org.koikifw.reference.ReferenceApplication -Dloader.path=<observer.jar> -cp <reference.jar> org.springframework.boot.loader.launch.PropertiesLauncher`。同じReference JARへToolingの観測JARを外付けする。V01〜12と、B／CだけL01〜02を実施する。

VT propertyと`spring.main.keep-alive=true`は両起動へ同じ方法で明示する。`api-bearer`等の追加profile、S1通知・復旧、C2外部接続は有効化しない。通常Session経路だけを使用し、loopback issuer／browserも不要とする。環境変数とloader設定の継承による混入を検出し、別作業のprofile／JAVA_TOOL_OPTIONS等が影響する場合はpreflightで止める。認証情報は子processの限定envへ供給し、コマンド・logへ出さない。

通常起動と観測起動の結果は区別する。観測JARが動作に及ぼす影響はゼロと仮定せず、通常起動のN01で代表業務の成立を対照確認する。詳細なthread／context・pool計測と負荷結果は観測起動の証拠であり、無観測の本番性能へそのまま転用しない。

### 9.2 外付けobserverの責務

- ToolingだけのAutoConfigurationを専用`AutoConfiguration.imports`で読み込む。HTTP route、認証filter、許可規則、executor、transaction・DataSourceを置き換えない。Boot／Spring／Security／Hikari依存はprovidedとし、observer JARへ同梱しない。
- MVC interceptorで既存`ExpenseSessionApiController`の同期handler入口・完了を観測する。requestId、固定fixtureのactor UUID、handler名、`isVirtual()`、thread ID、HTTP statusを記録する。credentials、Session／CSRF、メール、body・query、任意principalの文字列表現は記録しない。
- 外側の観測filterを既存相関filterより先に置き、処理前後のrequestIdとSecurityContextの復元／除去を照合する。内側のinterceptorで確かめた認証主体と混同しない。observer自身はMDC・SecurityContextを設定／復元しない。
- アプリの既存Hikari MXBeanを同一process内からread-onlyで500 msごとに観測する。active／idle／total／pending、実効maximum／minimum／connectionTimeoutとJVM CPU／heapを採る。接続を借りて計測したり、remote JMX endpointを開いたりしない。ThreadMXBeanのthread数はplatform threadの指標とし、VT総数と呼ばない。処理中のrequest VT数は入口／完了から別集計する。
- 出力は有限queue・専用writerでリポジトリ外に保存する。request処理を同期file書込みで待たせない。欠落・queue overflow・出力上限到達は観測不成立として停止する。

接続確認はD2-02の最初の一回に含める。observerだけがロードされること、既存Beanを置換しないこと、filter順、認証actor取得、MXBean取得、実測isVirtualのA/B/C差を確認する。不成立時に正式アプリへtest routeを追加して続行しない。

### 9.3 再利用する実在の入口

`build-support/reference-e2e-verification/`の`PackagedReferenceExternalCheckTest`から、browserなしのJava HttpClient／CookieManager、login form CSRF、子JVM起動・log非露出・cleanupの手順を参考にする。`PackagedReferenceCriticalJourneyTest.seed`と`PackagedReferenceSessionSpaJourneyTest`から、所有する隔離DBのuser／権限／department／category fixtureとSession API期待値を照合する。

既存helperは現在のtest JVMの`java.home`を使うため、そのままJava 25子起動には利用しない。D2専用harnessはJava21Home／Java25Homeを明示入力とし、子の実版を照合する。既存のprivate helperやbrowser testへのcompile依存を追加せず、必要な有限fixtureだけをD2 Tooling内へ記述する。既存E2E・C2・S1 test fileは変更しない。

## 10. 確定提案：検証ケースと判定

以下はD2-02で実装する論理checkpoint。N01×3、V01〜12×3、L01〜02×2の**計43 checkpoint**とし、JUnitは構成ごとの通常／観測をparameter化した6 invocationを想定する。HTTP要求数やparameter展開を43 test実行と混同しない。負例の内部要求と計測要求は別に数える。

fixtureはapply権限を持つ別user A／Bと、A所有のexpenseを用意する。実loginを使い、user／workerごとにCookieManagerを分離する。`/spa-api/csrf`で取得したXSRF-TOKENを`X-XSRF-TOKEN`へ供給する。起動ごとに新しいDBへ同じ初期状態を作り、試行間でSession・version・Auditを引き継がない。

| ID／構成 | 実操作 | 期待値・証拠 |
|---|---|---|
| N01／A・B・C | 通常`java -jar`でlogin→`POST /spa-api/v1/expense-requests`→詳細→submit→`GET /expenses/{id}` | login 302、create 201／DRAFT:1、詳細200、submit 204／SUBMITTED:2、MVC200。`EXPENSE_WORKFLOW`／`SUBMIT_EXPENSE` Auditが対象に一件。正式JARにobserver／test routeなし |
| V01／A・B・C | 観測起動、認証付き`GET /spa-api/v1/expense-options` | 実JDK／property／heap／poolとobserver登録を記録。handlerのisVirtualはA・B=false、C=true。観測fixtureのactorとMDCを照合 |
| V02／全構成 | create→list→detail | 201／200／200、本人の作成内容・DRAFT:1。DB・DTO一致、workflow Auditなし |
| V03／全構成 | 正常submit→MVC表示 | 204、SUBMITTED:2、workflow Audit一件、actor A。既存同期業務契約が維持される |
| V04／全構成 | Cookieなしで保護Session APIのGET | 401、業務handler未到達、業務変更なし |
| V05／全構成 | 別draftへのsubmitでCSRF欠落／不正 | どちらも403、draft/version／workflow Audit不変 |
| V06／全構成 | BがAのdraftをGET／submit | どちらも404。存在の秘匿と対象の無変更を確認 |
| V07／全構成 | AがdraftへexpectedVersion=0でsubmit | 409。DRAFT:1、workflow Auditなし |
| V08／全構成 | A／Bの独立Sessionで並行GET（各5回、最大2並行） | 各userの一覧／詳細scopeとactor／requestId一致。他userの状態・contextが混入しない |
| V09／全構成 | 明示した安全なX-Request-ID、未指定、不正値のGET | response headerとhandler MDC一致。不正値を採用せず新規IDを生成。全要求後の外側filterでMDC／SecurityContextが処理前の状態へ戻る |
| V10／全構成 | 隔離DBでexpense tableを一時lockし、既存詳細GETを8並行。pool pending観測後、timeout前にlock解除 | active≤4、total≤4、pending>0を実測し、全要求200で完了。解除を早め過ぎてpending未観測なら未実証。fixture・Audit不変 |
| V11／全構成 | 同じlockで8並行GET。pending観測から4秒保持、絶対上限6秒でrollback | 3秒のpool connectionTimeoutによる失敗を一件以上、原因をlog／pool観測で確認。該当HTTPは有限時間内の5xxと非露出を確認。session読み取り段階の失敗とcontroller段階の失敗を区別し、一律のProblem codeは仮定しない。単なるclient timeoutをpool timeout成功にしない |
| V12／全構成 | lock解除・要求終了後、本人GET成功→logout→旧CookieでGET | pending=0、active=0へ10秒以内に復帰、接続≤4、業務状態不変。GET200、logout204、旧Cookieは401。直後にresource／log非露出・cleanupを確認 |
| L01／B・C | 同じfixtureの認証付き詳細GET、同時要求上限1 | 30秒warmup（最大500要求）、60秒計測（最大2000要求）。終了した要求だけ補充。HTTP／body正当性、応答分布、throughput、CPU／memory、pool／thread・JFRを保存 |
| L02／B・C | L01と同じ操作、同時要求上限8 | 同じ時間・件数上限。合格条件は予期しない失敗・漏えい・資源逸脱なし、終了後の解放。速度向上率の閾値なし |

V10／11のlockは専用admin接続の`LOCK TABLE kkref_expense_request IN ACCESS EXCLUSIVE MODE`をtransaction内だけで保持し、必ずfinallyでrollback／closeする。GETだけを注入対象にし、trigger・migration・本番権限を変更しない。pool pendingとともに`pg_stat_activity`／`pg_locks`でアプリ接続の対象relation待機を確認し、別の原因の待機を成功へ数えない。V10はpendingの初回観測後直ちに解除し、lock全体2秒以内。V11はpending待機を含め6秒以内で解除する。成立しない場合に保持時間を延長しない。

比較設定はpool最大4／minimumIdle 1／connectionTimeout 3000 ms、アプリ接続のstatement_timeout 8秒・lock_timeout 6秒を全構成で共通にする。fixture adminのlock transactionには10秒のidle-in-transaction timeoutを設ける。SQL timeoutも使い捨て環境に限定する。V11で許容する故障由来の5xxを通常負荷L01／02の成功へ混ぜない。

計測窓は時間上限か要求上限の早い方で終了し、実時間・件数・打切り理由を記録する。warmupを統計から除き、p50／p95／p99、完了数／実時間、エラー種別を算出する。各条件一回の有限診断であり、統計的優位や最大throughputを主張しない。負荷generatorの最大8要求は本番の受付上限・`@ConcurrencyLimit`成立の証拠にならない。案件で本格利用する際の流量制御とSLO評価は採用条件として残す。

JFRはB／Cの観測processで同じ設定を用い、Pinned（threshold 20 ms）／SubmitFailed／CPU／GC等を記録する。Start／Endの有効化・event名の存在を採用JDKで確認する。Pinnedゼロはその窓・閾値で未観測という結果とし、pinning不存在としない。SubmitFailedや未説明の停止・資源異常はFAIL。native libraryやCPU中心処理を今回の代表操作で網羅したとしない。

## 11. 実装対象・資源・実行順の確定提案

### 11.1 D2-02の対象path

新規Toolingは`build-support/virtual-threads-verification/`へ置く。下表の相対pathを対象とし、正式Root Reactor／publish unitへ追加しない。

| path | 役割 |
|---|---|
| `pom.xml`、`observer/pom.xml`、`harness/pom.xml` | 独立Tooling reactor、provided依存のobserver、test-only harness。既存Parent／BOMに版を合わせ、Java21でbuildする |
| `observer/src/main/java/org/koikifw/buildsupport/vt/observer/VtObservationAutoConfiguration.java` | 観測Bean登録と対象設定の照合 |
| 同packageの`VtRequestObserver.java` | 外側filter・MVC interceptor・有限出力。認証・相関値は読取りだけ |
| 同packageの`VtResourceSampler.java` | Hikari／JVMの有限・read-only観測と停止 |
| `observer/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` | Toolingに限定した登録 |
| `harness/src/test/java/org/koikifw/buildsupport/vt/VtReferenceFixture.java` | JDK・JAR固定、子process、隔離DB・fixture・lockと所有資源cleanup |
| 同packageの`VtSessionClient.java` | 独立CookieManager、login／CSRF・既存操作、HTTP timeoutと要求上限 |
| 同packageの`PackagedReferenceVirtualThreadsTest.java` | 6 invocation、43 checkpoint、負荷／判定・証拠集計 |
| `README.md` | 入力・コマンド・資源・結果位置・再実行条件 |

Toolingは計11 file、新しい外部library座標の追加は予定しない。検証結果は`docs/architecture/validation/phase4-d2-02-java25-virtual-threads-results.md`一件へ記録し、本計画・統合計画の状態を更新する。production、既存test、既存POM、migration、CI、Public APIに変更なし。必要pathや依存・観測方式が変わる場合は根拠と差分を先に更新する。

### 11.2 実行資源と時間

- 最大同時JVMはMaven 1（heap 768 MiB以下）、harness 1（768 MiB）、アプリ1（768 MiB）の計3。短命なJFR集計CLIはアプリ停止後に実行し、この上限内に収める。browser／Node／issuer／外部stubは0。
- PostgreSQLは1 container、memory 1 GiB・CPU 1相当、max_connections 16。Testcontainers採用時はRyuk最大1を別計上。実image digestとRyuk制限をpreflightで記録する。
- DB client接続はアプリ4＋admin／lock／monitor各1＝最大7、予備込み上限8。各起動用の使い捨てDBを順次作り、既存processを停止してから次を開始する。fixture準備とlock／monitor接続の重複を数える。
- HTTP connect timeout 1秒、request timeout 10秒。最大8要求、待機queueなし。起動readiness期限60秒、通常停止30秒・終了しなければ今回所有PIDだけ強制終了し10秒確認。cleanup予約5分を侵食しない。
- 観測出力は起動ごと最大30000 record／32 MiB、JFRはprocessごと最大32 MiB。上限到達や欠測で停止し、黙って切り捨てない。既存pool DEBUGは設定確認に必要な範囲とし秘密を出さない。
- 検証枠55分：preflight 10分、build／root回帰20分、6起動／比較20分、cleanup 5分。初回一回ずつとし、失敗を消すための自動再実行をしない。JDK取得・新規Tooling作成の時間はこの実行枠に含めず、別に実績を記録する。D2-02作成・実行・結果整理は4〜8時間の暫定見積を維持する。

### 11.3 実行順と回帰集合

1. source固定後、環境・Java25配布物／checksum、Docker権限・image、空き資源、既存process／container、必要依存を確認する。Java25未導入は準備待ちであり、実行成功としない。
2. Java21で通常root `clean verify`を一回実施し、同時にReference JARを生成する。前回と同じbounded DB設定を使い、JAR SHA-256、manifest、fixture混入なしを固定する。
3. Java21でTooling reactorをbuild／test-compileし、構成Aの通常起動、観測起動で接続確認から進める。B、Cも同じ順序で実施し、harnessをJava25で再compileしない。正式JARとobserver JARのhashを試行前後で照合する。
4. B／CのL01／02は対応する観測process内で順次実施する。注入故障の回復確認後にfixtureを揃え、load前後の業務状態・Auditが不変であることを確認する。
5. process・container・接続・port・lock・一時秘密をcleanupし、証拠台帳と実績を集計する。

通常rootのコマンドは、クラウドの既存Java21／Maven環境を読み込んだうえで次を用いる。Maven JVMもheap最大768 MiBとし、Docker接続には実行環境の権限手順を適用する。不足依存をofflineの失敗と区別し、必要な取得先・範囲を確認してから準備する。

```bash
./mvnw -o -B -ntp -DargLine=-Xmx768m -DforkCount=1 -DreuseForks=false \
  -Djunit.jupiter.execution.parallel.enabled=false \
  -Dkoiki.reference.normal-root.db-limits.enabled=true clean verify
```

Tooling側は`-f build-support/virtual-threads-verification/pom.xml`を入口とし、Java21で`-DskipTests package`（test-compileを含む）を行った後、harness moduleのSurefire直接goalへ`-DskipTests=false`と対象classを明示して6 invocationを一回だけ選択実行する。準備buildで検証を先行実行しない。harnessは`JAVA21_HOME`／`JAVA25_HOME`と固定artifact manifestを明示入力として検証し、欠落時はSKIPで成功扱いせず停止する。readinessは通常profileで`/login`と未認証Session APIを確認し、既存helperのBearer endpoint待ちを流用しない。実装時の選択property・Surefire goalと完全な再現コマンドは新規READMEへ記録する。

直接回帰は通常root内の`SessionExpenseApiControllerTest`（保存結果40）、`SessionExpenseApiPostgreSqlIntegrationTest`（10）、`ReferenceSessionApiSecurityTest`（22）、`ExpenseAuditRollbackPostgreSqlIntegrationTest`（1）の73 invocationを重点照合する。別に重複再実行しない。件数は前回保存XMLの静的照合値で、今回の成功件数ではない。root全体は従来449 PASS／100 SKIPが比較基準だが、実結果と差異を記録する。S1専用全件、C2 packaged、実browserの再実行は今回の回帰集合に含めない。

## 12. 判断状態と残す条件

以下はD2-01具体化時点の状態。後続のOwner指示・実行・訂正は§13〜14と最終結果を適用する。

D2-01として、Java方針、同一artifactの比較、代表操作、43 checkpoint、外付け観測方式、対象11 Tooling file、回帰集合、資源・時間・停止／cleanupを具体化した。**検証範囲はレビュー可能な状態であり、D2-02の実装・動的検証はまだ開始していない。** 文書だけの変更について`git diff --check`とリンク・path・件数の静的照合を行う。

実行開始前に確認する事項は、今回の文書・source固定、Java25取得とDocker preflight、今回所有の隔離DB／有限lock・process／cleanup、§9の観測接続の成立。接続不成立や依存・公開契約変更が必要な場合は計画差分へ戻す。

結果判定では、VT有効化を観測できた同期Web経路、業務・認証・Auditの同等性、資源制限・失敗回復の成立をそれぞれ記録する。案件の受付制限、SLO、独自executor・非同期・native依存、将来Java版への適用は確認条件として残す。D2-03の実CIがない段階でDoD 4-11をPASSにしない。文書確認から、結果受入・commit／push・正本変更・CI変更が成立したとは扱わない。

## 13. D2-02開始指示と対象固定（2026-10-26）

Ownerは§8〜12を確認し「この範囲に基づくD2-02の実装・検証へ進めましょう」と指示した。今回のTooling実装、隔離DB・有限lock、子process・計測・cleanup、通常root一回と6構成別invocationの検証をこの範囲で開始する。結果受入・commit／push・正本／CI変更とは区別する。

Referenceの対象コードは基点commit `1284ba08a9e0cb44c67487dc02148e378699ceb4`から変更せず、承認時文書のコピーとSHA-256をリポジトリ外へ保存した。新規Toolingは動的検証前にsourceコピー・hash一覧とobserver JAR hashを保存し、比較JARとともにmanifestへ固定する。作業treeがclean／commit済みであると記録せず、Git commitと未commit Tooling／文書のhashを組み合わせて試行を識別する。

Java25はTemurin 25.0.5+8（公式release assetとchecksumの照合済み）を用いる。Docker28.4.0、既存PostgreSQL17／Ryuk0.14.0 imageを確認した。通常root初回は449 PASS／100 SKIP・失敗／エラー0、3分49秒。Tooling準備buildはKOIKI BOMのローカル未登録で停止したため、既存BOMをローカルMaven repositoryへ登録した。その際不足していたinstall pluginをMaven Centralから取得した。リモートへのartifact公開ではなく、検証準備であり、通常rootやVTケースの再実行ではない。ToolingのJava21 compile／test-compileは検証をskipして完了している。

**初回停止：** A-nativeがHMAC fixtureのURL-safe／標準Base64不一致で起動FAIL。後続5 invocationはSKIP、43 checkpointのPASSは0。子process・DB・container・portのcleanupを確認した。HMAC生成を標準Base64の32 byte鍵へ訂正し、Frameworkは変更せず、専用検証の自動再実行はしていない。初回証拠を残し、同じ範囲の追加一回・通常root再実行なしの案を結果記録§5へまとめた。

**追加一回の承認・結果：** Ownerは訂正後の同範囲で追加一回を承認。2026-10-26に専用検証だけを実行し、A-nativeはlogin・create・read・submit204まで進んだが、helperの応答相関ヘッダー必須取得でERROR、後続5件はSKIP。43 checkpointは未成立。cleanup済みで、更なる起動は行っていない。通常ReferenceにObservabilityが未組込みだったため、§14へ構成訂正案を記録する。

## 14. 追加一回の結果を受けた構成訂正案（2026-10-26）

**状態：** Ownerは本節に沿った見直し・推進を指示（2026-10-26）。§9の「既存相関filterがReferenceにある」という前提を訂正し、以下の具体化を経て同じ6 invocation／43 checkpointの専用検証一回へ進める。前回までの追加一回で実行済みの構成とは分ける。

静的調査ではObservabilityのsourceを確認したが、保存Reference JARにそのStarterが入っているかの照合が不足していた。通常ReferenceにはWeb MVC／API／Data／JPA／Security／Identity／Audit／Session等が入り、Observability Starterは含まれない。これをFrameworkの不具合や必須依存の欠落としない。案件が必要なStarterを選択する既存方針を維持する。

### 14.1 通常起動と観測起動の条件

| 起動 | 訂正する条件と証拠の意味 |
|---|---|
| N01の通常`java -jar` | 固定Reference JARを維持し、Observabilityの追加・相関ヘッダーの要求を行わない。業務・認証・DB／Auditの成立を確認する |
| V／Lの観測起動 | 同じReference JARへobserverだけでなく、基点sourceの実`koiki-starter-observability`と採用POMの通常runtime推移依存を外付けする。実相関filter・TaskDecoratorのある構成として識別する。単なるTooling製の代替相関filterやMDC設定で成功を作らない |
| 比較の適用範囲 | A／B／Cの観測classpathを同じJAR集合で固定する。B／CのVT比較をこの集合内で行い、通常Reference単体の相関・無観測性能へ一般化しない |

通常起動でhelperが送信requestIdを取得することは、相関の成功証拠ではない。観測起動では従来どおり実filterによる応答ID・handler MDC・Auditの突合、要求終了後の復元を必須にし、V03／V09を削減しない。

### 14.2 必要な実装・入力差分

1. rootで既にJava21生成済みのObservability Starter JARを利用する。Reference POM／正式JARは変更しない。推移依存は現行BOM／Starter POMからMavenで解決し、相関だけ動かすために宣言依存を手作業で間引かない。
2. ReferenceのBOOT-INF/libと同じcoordinate／版のlibraryは二重追加しない。版の衝突や解決不能はpreflightで停止する。実際に追加するJARのcoordinate・path・SHA-256をmanifestへ追加し、`loader.path`をその有限listだけから構成する。任意directoryの一括scanにしない。
3. `VtReferenceFixture`をこのmanifest読取り・hash照合・観測用classpath供給に対応させる。`observer`自身は現在のprovided依存と読取り専用責務を維持する。新しいFramework API／property／source変更は追加しない。
4. Starterの宣言依存にはActuator／metricsが含まれるため、観測環境だけ`management.endpoints.web.exposure.exclude=*`を明示して管理HTTP endpointを公開しない。DB／認証・認可の既存境界は変更せず、D1のexporter・sink・運用全体は対象にしない。
5. 現在の11 Tooling file内のharness／READMEを必要最小限に変更する案とする。runtime依存解決の具体的JAR一覧とartifact manifest、追加classpathの成立を作成・compile段階で提示したうえで、実行範囲を再確認する。

論理43 checkpoint・6 invocation、資源・時間・停止／cleanupは維持する。Ownerの本節に沿った見直し・推進指示を受け、具体化したclasspathで専用検証一回へ進む。通常rootは再実行しない。前回までのFAIL／ERROR・SKIPと証拠を保持し、今回のsource・manifest・出力先は別epochへ固定する。

### 14.3 具体化したclasspath・実装差分

`harness/pom.xml`へ依存解決専用の`observation-runtime` profileを追加した。既存Starter JAR／POMとParentをローカルMaven repositoryへ登録し、Maven Dependency Plugin 3.8.1の`build-classpath -DincludeScope=runtime`でconsumerのruntimeを解決した。正式publish・既存POM変更・root再buildではない。Starter自身のdirect optional依存を含む解決結果とは区別し、consumerへ推移導入されないServlet API等を余分に追加していない。

結果は全33 JAR。Reference内19 JARは版・payload hash一致を確認して再利用し、以下の14 JARを追加する。全path・SHA-256、再利用entryはリポジトリ外の`observability-revision/artifact-manifest.json`に固定した。版の衝突はない。

| group／version | 追加artifactId |
|---|---|
| `org.koikifw`／`0.1.0-SNAPSHOT` | `koiki-starter-observability`（基点sourceのJava21 root生成JAR） |
| `org.springframework.boot`／`4.1.1` | `spring-boot-starter-actuator`、`spring-boot-starter`、`spring-boot-starter-logging`、`spring-boot-starter-micrometer-metrics`、`spring-boot-micrometer-metrics`、`spring-boot-micrometer-observation`、`spring-boot-actuator-autoconfigure`、`spring-boot-actuator`、`spring-boot-health` |
| `io.micrometer`／`1.17.1` | `micrometer-jakarta9`、`micrometer-core` |
| `io.micrometer`／`1.2.1` | `context-propagation` |
| `org.hdrhistogram`／`2.2.2` | `HdrHistogram` |

`VtReferenceFixture`は追加runtimeの必須化、coordinate・path・hash・重複の検査、有限listからのloader.path供給、試行後のhash照合に対応した。観測起動だけ管理endpointのexposureを除外し、V01内で認証付き`/actuator`と`/actuator/health`が403／404であることを確認する。observer本体は変更せず、読取り専用の責務を維持する。検証用Maven／harness自身には依存解決profileを適用しない。

今回のsourceは基点commit＋未commit Tooling／文書の保存コピー・hashで識別する。Git commit／push／CI変更は含めない。新しい試行は`/workspace/koiki-cloud/logs/d2-02-20261026/observability-revision/`へ保存し、比較20分以内・cleanup5分予約、最大3 JVM／PG1／Ryuk1／DB client8の条件を維持する。

### 14.4 構成訂正試行のTooling照合訂正と未完了分の続行

最初の構成訂正試行は25.872秒。A-nativeのN01、A-observedのV01〜03が成功し、V04は401取得後の完了記録照合で停止した。実相関filterはSecurityより後に動作する現在の構成で、先に拒否される要求には応答相関IDが付かない。`@Order`はTaskDecorator Beanに付いており、相関filterの優先順を示すものではなかった。これはVT動作の差ではない。

V04の契約は401・handler未到達・業務変更なしであり、拒否応答の相関IDを必須にしない。読取り専用observerのF recordへ検証済み受信IDを追加し、拒否要求のmarker照合にだけ使う。MDC・応答・Auditを書き換えず、V03／V09の実相関要件は維持する。拒否時の相関範囲はD1への確認事項として記録する。

Ownerの本節に沿う見直し・推進指示の範囲で、このTooling観測訂正と未完了分の一回続行を行う。A-nativeはReference JAR・起動条件・N01判定が不変なので保存済み成功を再利用し、`koiki.vt.start-at=A-observed`で残る5 invocation／42 checkpointを対象とする。先のA-observed V01〜03は履歴とし、重複計上しない。source／observer JARの変更は別epochへ固定する。

比較枠を更新せず、消費25.872秒を含め20分内とするため、続行は19分30秒以内・cleanup5分予約とする。通常root・成功済みA-nativeを再実行しない。今回の経緯と失敗は保持し、既存アプリ・Security・Framework・公開契約・43項目の判定基準を変更しない。

### 14.5 接続保持の限定原因診断

未完了分の続行ではA-observedのV01〜09が成功。V10はロック待機・pool待機と8要求の200完了を確認したが、active=1が残り10秒内のactive=0に達しなかった。起動時点からactive=1が見られるため、故障注入の漏えいとは断定せず、判定を緩めず比較を停止する。続行Maven実時間24.052秒、構成訂正試行との合計49.924秒。

原因切り分けとして、同じ固定JAR／14外付けruntime／Java21の観測起動を一回だけ行い、Hikariのleak-detection-threshold=2000 msで借用元stackを採る。起動readiness後10秒だけ観測し、DB接続のpid／state／wait・query hash／transaction ageをread-onlyで保存する。SQL本文・秘密値は採取しない。fixture user seed、login、業務更新、DB lock、負荷、43項目の合格判定は実施しない。

これはOwnerの構成見直し・推進指示に対する限定原因診断であり、失敗した比較の再実行ではない。`koiki.vt.pool-diagnostic.enabled=true`でA-observed一回に固定し、起動・診断120秒以内とcleanup5分予約、既存のJVM・DB接続上限を維持する。通常root・N01を再実行せず、接続の意図的保持か不具合かを説明できるまでV10／V12をPASSにしない。

### 14.6 診断結果とHikari入力の正規化

最初の起動診断は21.326秒、ログでHikari初期設定がmaximum=10／minimumIdle=10／connectionTimeout=30000、leakDetectionThreshold=0と判明した。後から4／1／3000へ変化していた。leak検知自体が未設定だったため、Hikari生成前に上限4／1／3000と検知2000を非秘密の診断fileで読み込ませる限定診断を21.708秒で実施した。いずれも業務操作・故障注入・合否checkpointなし、cleanup済み。

後者のstackは`HikariDataSource.getConnection`→`JavaBeanBinder$BeanProperty.getValue`→`ConfigurationPropertiesBindingPostProcessor`を示した。Toolingが環境変数`SPRING_DATASOURCE_HIKARI_CONNECTION_TIMEOUT`等を渡すと、設定binderがconnectionのネストをたどって接続を借用し、未返却の1本が生じていた。VTや業務操作のリークと断定したものではない。

ToolingのHikari環境変数を廃止し、子processの正規引数`--spring.datasource.hikari.maximum-pool-size=4`、`--spring.datasource.hikari.minimum-idle=1`、`--spring.datasource.hikari.connection-timeout=3000`へ統一する。keep-aliveも正規引数で指定する。継承Hikari環境変数がある場合は起動前に止め、起動ログでも初期4／1／3000を照合する。Frameworkや既存アプリ・POMを変更せず、active=0の基準も緩めない。

起動条件を正規化したため、最終比較はA-nativeを含む6構成すべてを同条件で確認する。通常rootは再実行しない。前試行・診断合計92.958秒を保持し、今回の有限比較は1080秒以内・cleanup5分予約とし、20分の比較枠内に収める。診断fileは通常比較には使わず、診断専用flagもfalseを明示する。新しいsource／入力・証拠は`canonical-input/`へ分離する。

### 14.7 正規入力試行とpool timeoutの経路別判定

正規入力試行は29.707秒。A-native N01とA-observed V01〜10が成功し、起動時点からのactive=1は消失、ロック解放後のactive=0も確認した。V11は3秒のpool待機失敗に対してMVC内で500、Session読取り中の例外から既存ERROR dispatchの拒否境界で403が生じ、helperが全て200／5xxだけと仮定して停止した。

V11の最低一件以上のpool timeout・HTTP5xx・非露出という要件は維持する。追加の403は成功／5xxに数えず、受信marker付き外側recordとhandler未到達を確認した場合だけpre-MVCのfail-closedとして別集計する。全要求の有限終了、pool上限・解放、DB／Audit不変は維持する。応答を一律500へ変更したり、既存SecurityのERROR dispatch許可を追加したりしない。「DB逼迫時の全経路が5xxへ統一される」という保証は提供せず、D1／運用ガイドの確認事項に残す。

source変更はToolingの経路別分類だけで、正規化後のA-nativeとその起動条件・判定は不変。保存済みN01を再利用し、未完了5 invocation／42 checkpointを`canonical-completion/`へ分離して一回続行する。前試行・診断合計122.665秒を保持し、続行1050秒以内・cleanup5分予約で20分枠を更新しない。過去の失敗・部分成功を重複計上せず、成立範囲と限界を結果へ記録する。
