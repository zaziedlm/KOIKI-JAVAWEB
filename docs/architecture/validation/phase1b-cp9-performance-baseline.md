# Phase 1b CP9 性能baseline検証

## 1. 判定

| 項目 | 結果 |
|---|---|
| Phase / status | Phase 1b CP9 HARNESS IMPLEMENTED / PROCESS SMOKE COMPLETE / OFFICIAL BASELINE PENDING |
| Milestone | C Operations & Closeout |
| Start commit | `679b6f6a769e8f1208baaf7cc0b45d1f22668390`（CP8 LOCAL COMPLETE） |
| Implementation branch | `feature/phase1b-operations-closeout` |
| Ownership | Tooling |
| Target | 新規・非配布の`build-support/performance-baseline` |
| Framework artifact / Public API | 変更しない |
| Production implementation | Framework production差分なし。承認済みの最小harnessを実装済み |

CP9は性能保証値または案件SLAを決める作業ではない。Grand Design §21.6／§25.3とPhase 1b DoD 1b-8に対し、
同一環境でKOIKI有無の差分を反復計測し、fingerprint付きraw／aggregate resultをリリース間で追跡できる
Tooling-owned harnessを整備する。

## 2. 開始preflight

### 2.1 Repository identity

| 項目 | 確認結果 |
|---|---|
| branch | `feature/phase1b-operations-closeout` |
| HEAD | `679b6f6a769e8f1208baaf7cc0b45d1f22668390` |
| main merge base | `b3973e66134898765b95796c3622aaa68759b4fd` |
| remote | `origin/feature/phase1b-operations-closeout`より1 commit ahead。pushはCP9開始条件にしない |
| worktree | clean |
| OpenSpec | Repositoryに採用済みchangeなし |
| Existing harness | `build-support/performance-baseline`は未作成 |

### 2.2 Environment identity

| 項目 | 確認結果 |
|---|---|
| OS | Microsoft Windows 11 Pro `10.0.26200`、64-bit |
| Timezone | Tokyo Standard Time（UTC+09:00） |
| Host CPU | Intel Core i5-14400F、16 logical processors |
| Host memory | `34,191,138,816` bytes |
| Build / measurement JDK | Eclipse Temurin `21.0.12.1+1-LTS` |
| Maven Wrapper | Apache Maven `3.9.16` |
| PowerShell | `7.6.3` |
| Docker | Rancher Desktop、client `29.5.3-rd`、Engine `29.5.3` |
| Docker allocation | 6 CPU、`16,771,686,400` bytes |
| Container environment | Rancher Desktop WSL Distribution、x86_64、kernel `6.6.87.2-microsoft-standard-WSL2` |
| PostgreSQL image | `postgres:17-alpine` |
| PostgreSQL digest | `sha256:18cfe3ef5e6815560c98237d6216d1e5119702fb0f3894c8785dd58b8bbe5d73` |

本表は開始時点のpreflightであり、公式baseline resultは計測時に再取得したfingerprintを正本とする。Docker、JDK、
OS、CPU allocationまたはJVM引数が異なるrunの数値を直接比較しない。

## 3. 要求と停止境界

### 3.1 必須要求

- bare Spring Boot fixtureとKOIKI適用fixtureの同じapplication pathを同一host／同一run内で対にして測る。
- Phase 1bに実在するAPI、Problem Details／Validation、structured logging、JPA／PostgreSQL経路だけを対象にする。
- workload、warm-up、measurement回数、fork、順序、JDK、JVM引数、OS、CPU、memory、Docker、DB image digest、
  commitを記録する。
- request単位のraw resultとaggregate resultを分離し、version付きschemaで検証する。
- harness build、positive schema、negative schema、fingerprint欠落、再集計の決定性を機械検証する。
- 性能数値をrequired checkにせず、harnessとresult contractの再現性だけをCI候補にする。

### 3.2 CP9で行わないこと

- 案件SLA、性能保証値、PCをまたぐ優劣判定、required数値閾値
- Security、認証、認可、監査、外部API、Oracle、Spring Batch等の未実装経路の架空baseline
- production用load test service、APM backend、OpenTelemetry exporterまたはcloud固有monitoring
- JMHによるmethod単位microbenchmarkをHTTP／DB end-to-end結果の代替にすること
- performance fixture、result schema、runnerをFramework release unitまたはCustomer成果物へ含めること
- CP9だけを理由とするFramework Starter、Public Java API、production設定の追加

## 4. 比較方式

| 案 | 内容 | 利点 | 問題 | 判定 |
|---|---|---|---|---|
| A. Starter有効／無効 | 同一KOIKI artifactでpropertyだけを切り替える | sourceとartifactが同じ | 無効側にもKOIKI classpathが残り、artifact有無の差を測れない | 補助診断のみ |
| B. 既存Consumer対bare | CP8までのConsumerと新規bare appを比較する | 実Consumerをそのまま使える | module、DB、event、endpoint等の業務差が大きく、KOIKI overheadへ帰属できない | 不採用 |
| C. 対称fixture | 同一workload codeをbare／KOIKIの2 assemblyから起動する | application pathを揃え、KOIKI artifact差分を説明できる | Tooling moduleが複数必要 | **推奨** |
| D. JMH | JVM内microbenchmarkとして測る | method単位の精度が高い | server起動、Servlet、filter、JSON、DB、process差分を表さない | CP9主経路には不採用 |

推奨案Cでは、業務語彙を持たない同一のperformance fixture codeを両variantが利用する。bare variantは同じSpring Boot
4.1.1、Servlet MVC、Validation、JPA、Flyway、PostgreSQL driverを直接利用し、`org.koikifw` runtime artifactを
含めない。KOIKI variantは同じfixture codeへPhase 1bのKOIKI Starterを適用する。dependency treeを保存し、bare側に
KOIKI artifactがないこと、両variantのSpring／driver versionが一致することを検査する。

Starterを無効化したvariantは、数値baselineではなくKOIKI auto-configurationのback-off診断が必要になった場合だけ
追加できる。推奨案Cのbare baselineを置き換えない。

## 5. 計測harness構成

予定する非配布構成は次のとおりとする。名称は実装時にMaven制約へ合わせて微修正できるが、Ownershipと依存方向は
変更しない。

```text
build-support/performance-baseline/
├── pom.xml
├── fixture/                 # 両variantが共有するController／Use Case／JPA fixture
├── bare-application/        # Spring Boot直接利用、KOIKI runtime依存なし
├── koiki-application/       # Phase 1b KOIKI Starterを通常artifactとして利用
├── runner/                  # JDK HttpClientによる外部process workload runner
├── schema/                  # fingerprint／raw／aggregateの3 JSON Schema
├── verify-performance-baseline.ps1
└── results/                 # 承認済みのtimestamp付きbaseline result
```

- Root Reactorへ含めず、空の隔離Maven repositoryへstageした正式KOIKI artifactだけを利用する。
- `fixture`はFramework規約やKOIKI internal型を参照しない。両variantで同一binaryを利用し、source copyを作らない。
- `runner`はJDK 21標準`HttpClient`を使い、外部benchmark executableのinstallation有無へ依存しない。
- processの起動、交互実行、fingerprint取得、PostgreSQL container、schema検査、cleanupは1本のPowerShell 7 scriptにまとめる。
- result schema検査にはPowerShell標準`Test-Json -SchemaFile`を使い、追加runtime libraryをFrameworkへ導入しない。
- negative schema inputはscript内で一時生成し、fixture fileや補助toolを増やさない。

## 6. Workload

### 6.1 Phase 1b workload set

| ID | application path | 主な観測対象 | 成功条件 |
|---|---|---|---|
| `http-success` | JSONを返し1件のkey-value logを出す副作用なしHTTP経路 | Servlet、JSON、correlation／structured logの差 | HTTP 200、response一致、期待log件数 |
| `validation-rejection` | 同一の不正payloadを`@Valid`で拒否する | ValidationとKOIKI Problem Details整形 | HTTP 400、variant別の期待error contract |
| `db-write` | Controller→transactional Use Case→JPA→PostgreSQL | data／transaction／JPA／DBを含むCustomer-like経路 | HTTP 201、forkごとのDB件数一致 |

`validation-rejection`は両variantのresponse表現が同一機能ではないため、KOIKIの追加error contractを含む経路として
別系列で扱う。`http-success`の差と混ぜて「純粋overhead」へ集約しない。`db-write`はDB変動を含むため、APIだけの
workloadと別に報告する。

### 6.2 Startup series

application process生成から専用readiness endpointがHTTP 200を返すまでを`startupMillis`として別系列で測る。
Servlet request latencyと混在させない。起動失敗、timeout、期待port以外のlistener、migration失敗はrun failureとする。

## 7. 計測protocol

### 7.1 既定条件

| 項目 | 推奨既定 |
|---|---|
| JVM | 同じTemurin 21、同じjava executable |
| JVM arguments | `-Xms256m -Xmx256m`。追加引数は全てfingerprintへ記録 |
| fork | variantごと3 fresh JVM |
| startup fork | variantごと3 fresh JVM。通常forkと別系列だが回数は揃える |
| warm-up | workloadごと200 request、raw sampleへ含めない |
| measurement | workloadごと1,000 request |
| concurrency | 1。Phase 1bではnoiseを抑えたpaired latency baselineを優先 |
| request timeout | 5秒。timeoutは除外せずfailure sampleとして保存 |
| application order | forkごとにbare→KOIKI／KOIKI→bareを交互にする |
| database | 同一digestの専用PostgreSQL container、variant別database、fork前reset |
| network | loopbackのみ。proxyを使わない |
| background process | harnessで制御できないためfingerprintと実行時注記へ残す |

計測回数はlocal所要時間とraw sizeの実測後にOwner承認範囲内で調整できる。調整した値はschema内のconfigへ必ず保存し、
異なるprotocolのrunを同一系列として直接比較しない。

### 7.2 paired execution

1. cleanな計測対象commitを記録する。
2. Framework release unitを空の隔離Maven repositoryへstageする。
3. bare／KOIKI fixtureを同一JDKでpackageし、dependency境界を検査する。
4. 専用PostgreSQLを起動し、image digestとserver versionを記録する。
5. variant順序をforkごとに交互化し、fresh JVMでstartupとworkloadを実行する。
6. warm-up後のrequestだけをraw resultへ保存し、失敗sampleを削除しない。
7. raw resultだけからaggregateを再生成し、既存aggregateと一致することを確認する。
8. 3 result schema、最小negative input、DB件数、process exit、container／一時repository cleanupを検査する。

公式baselineはclean commitからだけ取得する。このため、CP9は少なくとも次の2 commit pointを許容する。

1. harness／schema／negative fixtureを実装し、全構造検証を通したcommit
2. そのclean commitから採取したfingerprint／raw／aggregate／validation Evidenceのcommit

## 8. Result contract

### 8.1 `fingerprint.json`

必須項目は`schemaVersion`、`runId`、`startedAt`、`gitCommit`、`gitDirty`、host identity、`java`、
`jvmArguments`、`maven`、`docker`、PostgreSQL image／digest／server version、`timezone`、`harnessVersion`、
protocolおよびbare／KOIKI artifact SHA-256とする。host identityはOS、version、architecture、CPU、logical processor数、
host memoryを1 objectにまとめる。
公式baselineでは`gitDirty=false`を必須にする。

### 8.2 `raw-results.json`

request sampleごとに`runId`、`variant`、`workload`、`fork`、`sequence`、`durationNanos`、
`httpStatus`、`responseBytes`、`success`を持つ。timeout／connection failureも`success=false`、安定した`errorCode`で残す。
startup sampleは`workload=startup`、`durationNanos`、process exit／readiness結果を同じraw contractの別sample typeで表す。

### 8.3 `aggregate.json`

`variant`／`workload`ごとにsample count、failure count、p50、p95、measurement wall time、requests per secondをrawから
決定的に算出する。bareとKOIKIのp50／p95 absolute deltaとpercentageは同一fingerprint、同一protocol、同一workloadの
場合だけ生成する。percentageはbare値が0の場合に生成しない。参考値を読みやすく保つため、CP9では追加percentile、
CPU profile、heap内訳または独自scoreを生成しない。

schemaは初回`1`とし、required fieldの削除、単位変更または意味変更ではversionを上げる。任意項目追加だけで過去resultを
読める場合はcompatible extensionとして扱う。

## 9. Gate 9-2実装検証

### 9.1 細粒度検証

- bare dependency treeに`org.koikifw` runtime artifactが存在しない。
- KOIKI variantは隔離repositoryの正式artifactを利用し、Framework internal packageを参照しない。
- 両variantが同じfixture binary、Spring Boot、JDK、PostgreSQL driverを使う。
- runnerのwarm-up除外、fork／sequence、timeout保存、p50／p95、delta、0除算をunit testする。
- rawからaggregateを2回生成し、内容が一致する。
- 3つのpositive resultがschemaに適合し、必須fingerprint欠落とraw型不正の最小2 negativeが失敗する。
- 数値の大小、percentage、p50／p95をbuild failure条件にしない。

### 9.2 実process acceptance

- bare／KOIKIのpackage済みJARを別processとして交互に起動する。
- 全workloadのstatus、response、log件数、DB件数を計測前後に検査する。
- request workloadのraw sample数が`variant × 3 workload × fork × measurement`と一致し、startup sampleは別に
  `variant × startup fork`と一致する。
- fingerprint、raw、aggregateをschema検査し、rawからの再集計が一致する。
- child JVM、PostgreSQL container、DB補助session、隔離Maven repositoryを`finally`でcleanupする。

### 9.3 回帰

CP8 aggregateを含むCP1〜CP8回帰、Root release unit、Public API／runtime dependency／migration inventoryを維持する。
CP9のperformance数値はCI requiredにせず、Milestone Cではharness build、短縮smoke measurement、schema positive／negative、
fingerprint completenessおよびcleanupをCI候補とする。

## 10. Ownership／artifact inventory

| 対象 | CP9設計 |
|---|---|
| Framework release unit / artifact | 変更なし |
| Framework Public Java API | 追加なし |
| Framework runtime dependency / production config | 追加なし |
| Framework migration／table | 追加なし |
| Tooling | `build-support/performance-baseline`を新規追加 |
| Consumer | 既存Consumer production codeは変更しない。比較fixtureはperformance harness内に隔離 |
| CI workflow | CP9単独では変更しない。CP10のMilestone C接続で構造検証だけを候補化 |
| Versioned evidence | fingerprint、raw、aggregate、validation記録。dependency treeはrun内の一時検査 |

## 11. Owner Review

| Review項目 | 推奨案 | 状態 |
|---|---|---|
| Ownership | Tooling-owned、非配布、Root Reactor外 | ACCEPTED |
| 比較方式 | 同一fixture binaryを使うbare／KOIKIの対称2 assembly | ACCEPTED |
| workload | logを含むHTTP success、validation rejection、JPA／PostgreSQL write、startup | ACCEPTED |
| protocol | fresh JVM、3 fork、warm-up 200、measurement 1,000、concurrency 1、交互順序 | ACCEPTED |
| fingerprint | host／JVM／Docker／DB digest／commit／artifact hashを必須化 | ACCEPTED |
| result | version付きfingerprint／raw／aggregateの3 schemaだけ | ACCEPTED |
| quality gate | harnessとschema再現性だけ。性能数値はrequiredにしない | ACCEPTED |
| simplicity | 1 verification script、最小negative 2件、p50／p95だけを比較表示 | ACCEPTED |
| commit point | harness commit後、clean commitから公式baselineを採取 | ACCEPTED |
| Framework差分 | artifact、Public API、production設定、migrationを変更しない | ACCEPTED |

**Decision:** ACCEPTED  
**Decision owner:** Shuichi Kataoka  
**Review date:** 2026年8月29日

PC環境ベースの参考指標として、シンプルで見通し良く比較できることを優先する。本承認に基づき最小harnessの実装へ進む。
比較対象、workload、result contractまたはFramework境界を広げる必要が生じた場合は、実装を複雑化する前に本節を更新する。

## 12. 実装状況

2026年8月29日に、承認済み構成どおり`build-support/performance-baseline`へshared fixture、bare／KOIKIの
2 assembly、JDK `HttpClient` runner、3 JSON Schemaおよび1 verification scriptを実装した。fixtureはController、
transactional Use Case、JPA adapterを分離し、KOIKI runtime型へ依存しない同一binaryを両assemblyで利用する。

隔離Maven repositoryへ正式release unitをstageした上でharness 5 moduleをbuildし、全module成功、runner unit test
5件成功、PowerShell parser成功、`git diff --check`成功を確認した。検証中にSpring Boot 4.1のpersistence artifact分割、
PostgreSQL公式imageの初期化用一時server、およびhost環境変数`DEBUG=release`のSpring property誤認を検出し、明示的な
Entity／Repository scan、PostgreSQLの本server待機、application processの`--debug=false`固定へ反映した。

実行全般をやり直し、先にCP1〜CP8 aggregate回帰を完走した。Root release unit 10 module、Architecture Contract 4件、
ArchUnit 66件、CP6 Consumer 25件、CP8 workitem 7件／workreview 5件／application 25件に加え、CP8のwinner／contender、
異なるkey、crash／retryの実process検証がすべて成功した。

続いて、API versioningを両variantで同じSpring MVC標準設定と`/performance/1/*`経路へ揃え、CP9 Smokeを再実行した。
隔離repositoryへのrelease unit stage、harness 5 module build、runner unit test 3件、dependency境界、専用PostgreSQL 17、
bare／KOIKIの交互起動、startupと3 workload、status／response／log／DB件数、決定的再集計、3 positive schemaおよび
2 negative schemaが成功した。全般レビュー後、clean判定を結果生成前のpreflightへ移し、status／response contract、
variant／workload／fork別sample件数、failure 0件およびversion付き`Location`を機械検査へ追加して再実行した。最終結果は
run ID `8f4f4b45b7174516827d81ff7d463abc`として、各request workloadで
variantごとにwarm-up 3件を除外した10 sample、startup 1 sample、failure 0件を確認した。専用child process、container、
補助sessionおよび隔離repositoryは終了時にcleanupした。

Smoke値はprotocol動作確認用であり、性能baselineまたは性能判定には使用しない。Gate 9-2のlocal実process acceptanceは
満たしたが、CP9の完了判定はharness commit後のclean worktreeから公式baselineを採取するまで保留する。

```powershell
pwsh -NoProfile -File build-support/performance-baseline/verify-performance-baseline.ps1 -Smoke -SkipRegression
pwsh -NoProfile -File build-support/performance-baseline/verify-performance-baseline.ps1
```
