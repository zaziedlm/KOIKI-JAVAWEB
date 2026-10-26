# Java 25 Virtual Threads verification (non-distributed Tooling)

対象・判定・資源上限は[承認済みD2計画](../../docs/development/phase4-d2-01-java25-virtual-threads-plan-20261026.md) §8〜12。
正式Root Reactor、publish unit、Framework Public APIには含めない。

## Inputs and preparation

JDK21で通常rootを一回build／verifyし、Reference JARを固定する。
`JAVA21_HOME`、`JAVA25_HOME`を明示し、Mavenとharnessの実行にはJDK21を使用する。
Docker／Testcontainersは実行環境の権限手順を使う。

独立reactorから既存KOIKI BOMを解決するため、ローカルMaven repositoryにBOMが未登録の場合は
既存`koiki-dependencies-bom/pom.xml`の`install`を準備操作として実施する。正式成果物のremote publishではない。
不足install pluginの取得が必要なら、許可されたMaven Centralから取得する。
HMAC fixtureは既存Identityの`Base64.getDecoder()`に合わせた標準Base64・32 byte鍵を用いる。
password等で使うURL-safeな乱数文字列をHMAC設定へ流用しない。

通常ReferenceにはObservability Starterを含まない。観測起動は同一Reference JARへ実Starterとconsumerとして解決した
runtime推移依存を追加する。既存生成物だけを使い、通常rootを再実行する必要はない。
独立harnessの`observation-runtime` profileは依存解決専用。検証用Maven起動ではこのprofileを有効にしない。

```bash
# 未登録の場合だけ、既存Parent／BOMと生成済みStarterをローカルMaven repositoryへ登録する。
./mvnw -o -B -ntp -N -f koiki-parent/pom.xml install
./mvnw -o -B -ntp -f koiki-starters/koiki-starter-observability/pom.xml \
  org.apache.maven.plugins:maven-install-plugin:3.1.4:install-file \
  -Dfile=/absolute/path/to/koiki-starter-observability-0.1.0-SNAPSHOT.jar \
  -DpomFile=/absolute/path/to/koiki-starters/koiki-starter-observability/pom.xml
./mvnw -o -B -ntp -f build-support/virtual-threads-verification/harness/pom.xml \
  -Pobservation-runtime org.apache.maven.plugins:maven-dependency-plugin:3.8.1:build-classpath \
  -DincludeScope=runtime -Dmdep.outputFile=/absolute/path/to/consumer-runtime-classpath.txt
```

`build-classpath`の結果とReferenceの`BOOT-INF/lib`を照合する。同artifactで版が違う場合は停止し、
同じ版はpayload SHA-256も一致したものだけ既存JARを再利用する。残りをmanifestの`observationRuntime`へ
coordinate（group:artifact:version）・絶対path・SHA-256付きで固定する。Starter自体は基点sourceのroot生成JARを使う。
任意directory scan、相関だけ動かすための宣言依存の手動除外はしない。
現在の固定基点ではconsumer runtime 33 JARのうち19を再利用、14を追加する。scope optionalのServlet APIは推移導入しない。

```bash
./mvnw -o -B -ntp -DargLine=-Xmx768m -DforkCount=1 -DreuseForks=false \
  -Djunit.jupiter.execution.parallel.enabled=false \
  -Dkoiki.reference.normal-root.db-limits.enabled=true clean verify
./mvnw -o -B -ntp -f build-support/virtual-threads-verification/pom.xml -DskipTests package
```

生成後、リポジトリ外のJSON manifestへ次を記録する。pathは絶対path、checksumは実際のSHA-256。
observer JARは`observer/target/virtual-threads-observer-0.1.0-SNAPSHOT.jar`。

```json
{
  "baseCommit": "1284ba08a9e0cb44c67487dc02148e378699ceb4",
  "referenceJar": "/absolute/path/to/koiki-reference-app-0.1.0-SNAPSHOT.jar",
  "referenceSha256": "<64 lowercase hex characters>",
  "observerJar": "/absolute/path/to/virtual-threads-observer-0.1.0-SNAPSHOT.jar",
  "observerSha256": "<64 lowercase hex characters>",
  "observationRuntime": [
    {"coordinate": "org.koikifw:koiki-starter-observability:0.1.0-SNAPSHOT",
     "jar": "/absolute/path/to/koiki-starter-observability-0.1.0-SNAPSHOT.jar",
     "sha256": "<64 lowercase hex characters>"}
  ]
}
```

上記arrayは書式例であり、実際には照合済みの追加14 JARすべてを記録する。
再利用した19 JARのcoordinate・embedded entry・checksumも別の`observationEmbedded` arrayへ保存する。
外付け全JARのhashを試行前後に照合する。空のruntime listや実Starterなしの構成は起動前に拒否する。

baseCommitはこの計画の固定基点。別sourceへ再利用するときは適用計画と検証対象を更新する。
さらに、実行直前のGit差分、承認文書とTooling sourceのhash一覧、Java配布物checksum、image digestを保存する。
Maven JVM heapも768 MiB以下に制限する。証拠保存先には十分な空き領域が必要。

## Selected execution (once)

`koiki.vt.evidence`は存在する空directoryを指定する。構成別directoryが既にあれば失敗し、以前の試行を上書きしない。

```bash
./mvnw -o -B -ntp \
  -f build-support/virtual-threads-verification/harness/pom.xml \
  -DskipTests=false -Dtest=PackagedReferenceVirtualThreadsTest \
  -Dkoiki.vt.pool-diagnostic.enabled=false \
  -Dkoiki.vt.verification.enabled=true \
  -Dkoiki.vt.manifest=/absolute/path/to/artifact-manifest.json \
  -Dkoiki.vt.evidence=/absolute/path/to/empty-evidence-directory \
  surefire:test
```

`A-native → A-observed → B-native → B-observed → C-native → C-observed`の6 invocation。
AはJava21／VT無効、BはJava25／無効、CはJava25／有効。
N01×3、V01〜12×3、L01〜02×2の43論理checkpoint。失敗した構成以降はBLOCKED／SKIPとし、成功へ集計しない。
失敗を消す自動再実行、上限拡大は行わず、結果と計画差分を記録する。
Toolingの観測訂正後、保存済みの正常起動結果を再利用して未完了分だけを実行する場合は
`-Dkoiki.vt.start-at=A-observed`等を明示する。source・artifact・構成の同一性を確認し、別の保存先を使う。

## Observation and limits

通常起動は`java -jar`。観測起動は同一JARを`PropertiesLauncher`と外付けobserverで起動する。
observerは相関filterの外側と既存MVC handlerを読み取り観測する。認証、MDC、既存Bean、HTTP応答を変更しない。
観測側の`loader.path`はobserverとmanifestのruntime listだけを用いる。
`management.endpoints.web.exposure.exclude=*`を明示し、認証付き`/actuator`／`/actuator/health`が403または404であることをV01内で確認する。
通常起動では相関ヘッダーを要求せず、観測起動でだけ応答・MDC・Auditの実相関を検証する。
Hikari MXBeanのread-only観測は500 msごと。platform thread数と処理中のrequest VT数を区別する。
出力queue 256、30000 record／32 MiB上限。欠落・overflowは観測不成立。

アプリheap768 MiB、pool4／minimumIdle1／connectionTimeout3000 ms。
Hikari値とkeep-aliveは子processの正規`--spring.datasource.hikari.*`／`--spring.main.keep-alive=true`引数で渡す。
`SPRING_DATASOURCE_HIKARI_CONNECTION_TIMEOUT`等の分割された環境変数は使用しない。
この構成ではbinderが`connection`をネストしたBeanと解釈し、`getConnection()`を呼んで未返却の借用を作ることを診断で確認した。
継承Hikari環境変数を拒否し、起動ログで最初から4／1／3000になっていることも照合する。
PostgreSQL17 container一つ（1 GiB／CPU1、max_connections16）、Ryuk最大一つ、DB client上限8。
Maven／harness／アプリの最大3 JVM。browser・Node・外部providerなし。
V10／11だけ専用DB内のexpense tableを有限lockし、finallyでrollbackする。
HTTP timeout10秒、同時要求8以下。L01／02は各30秒warmup（500件上限）、60秒計測（2000件上限）。
上限で早期終了した場合も実時間を記録し、最大throughputや統計的優位は主張しない。

証拠は各構成directoryのownership、checkpoint、application log、観測TSV、pool試験、負荷JSON・raw latency、cleanup。
TSVは`CONFIG`（Java・VT・heap・pool）、`H`（handler入口）、`C`（handler完了）、`F`（外側context復元）、
`M`（時刻・active・idle・total・pending・処理中request数・platform thread数・heap・CPU ns）を区別する。
`F`は応答ID・context復元・statusに加えて、安全性検査済みの受信IDを末尾へ記録する。
Securityで先に拒否され応答IDがない要求も、受信markerで識別する。受信IDの記録を実MDC伝播の証拠へ転用しない。
Fのstatusは初回REQUESTのfilter復帰時点の値であり、その後のERROR dispatchの最終statusではない。
障害応答の分類はHTTP clientの最終statusを使う。ERROR dispatch全体のcontext相関をこのrecordだけで認定しない。
観測対象のrequestIdと固定fixture actor UUID以外の秘密値・業務bodyは出力しない。
B／CのJFRは同じ設定、32 MiB上限。Start／End、Pinned20 ms、SubmitFailed、CPU、GCを記録する。

V11は少なくとも一件のHTTP5xxとpool timeoutの因果を必須にする。Session読取り中の失敗から既存のERROR dispatchが
403を返す場合は、marker付き外側record・handler未到達・非露出を照合して`preMvc403`へ別集計し、5xxへ数えない。
全要求の有限完了、pool解放と上限、業務状態不変は同じ。全障害経路で5xxが統一される保証は含めない。

## Bounded startup diagnosis

接続保持の原因切り分けに限り、`-Dkoiki.vt.pool-diagnostic.enabled=true`を使うとJava21の観測起動一回だけになる。
Hikari生成前に非秘密の診断propertiesで4／1／3000とleak検知2000 msを設定し、readiness後10秒のmetricsと
read-onlyのDB接続状態・query hashを保存する。seed、login、業務操作、故障注入、負荷は実行しない。
診断のJUnit成功は資料採取とcleanupの成立であり、43 checkpointのPASSへ計上しない。通常比較では必ずfalseを明示する。

通常起動の代表業務と観測起動の詳細証拠を分ける。本番の流量制御、S1独自executor、全native依存、将来Java版、
案件SLOの成立をこの結果から認定しない。実CIはD2-03、DoD4-11は実CI結果で判断する。

## D2-03 CI bundle and source guards

`verification.py prepare`はconsumer runtimeを上記classpath fileから読み、版・payload衝突を拒否して
Reference／observer／追加runtimeを一つのdirectoryへコピーする。manifest schema1はsource commit・tree・dirty状態、
Java21／class major65、Tooling source hash、各JAR hash、資源owner UUIDを記録する。

```bash
python3 build-support/virtual-threads-verification/verification.py prepare \
  --repository /absolute/path/to/checkout --output /absolute/path/to/new-bundle \
  --classpath /absolute/path/to/consumer-runtime-classpath.txt \
  --maven-repository /absolute/path/to/maven/repository \
  --java21-home "$JAVA21_HOME" --expected-commit <source-commit>
python3 build-support/virtual-threads-verification/verification.py verify \
  --manifest /absolute/path/to/new-bundle/artifact-manifest.json --expected-commit <source-commit>
```

新manifestでの実行は前述のSurefire goalへ`-Dkoiki.vt.expected-commit=<source-commit>`も指定する。
CIはGitHub SHA一致とclean sourceを必須とし、dirty許可・旧D2-02 manifest・診断／途中再開を成功判定に使わない。
ローカルの未commit差分を点検する場合だけprepare／verify／summarizeの`--allow-dirty`を明示できるが、
manifestはdirty=trueのまま記録し、`GITHUB_ACTIONS=true`ではこの例外を拒否する。

`verification.py summarize`は6 invocationが全て成功・SKIP0、43 checkpoint・cleanup記録とVT実測を必須にする。
JFRの環境変数／System property／ProcessStart記録は無効化し、summaryでも0を確認する。
CI artifactには固定JAR、manifest、集計・JUnit・観測・負荷・cleanupの証拠を保存し、生app log・生JFRはuploadしない。
各実行の固定pathを使うため、保存artifactを別環境で再実行する際はcheckoutとmanifest pathの対応を改めて指定する。

`verification.py cleanup`はmanifest owner labelのDB、記録した起動時刻・固定JAR引数が一致するPID、
当該Testcontainers sessionのRyukだけを扱う。他のprocess／containerを削除しない。
予期しないapp／DB残存をcleanupした場合も、検証結果を成功へ変えずCIを失敗にする。

[VT適用ガイド](../../docs/development/virtual-threads-adoption-guide.md)と
[D2-03 CI判定記録](../../docs/development/phase4-d2-03-ci-and-adoption-guide-20261026.md)を参照する。
