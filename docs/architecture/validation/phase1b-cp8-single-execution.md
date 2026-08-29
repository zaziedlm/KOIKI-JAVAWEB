# Phase 1b CP8 単一実行contract検証

## 1. 判定

| 項目 | 結果 |
|---|---|
| Phase / status | Phase 1b CP8 LOCAL COMPLETE / Gate 8-1 COMPLETE / Gate 8-2 ACCEPTED / Gate 8-3 COMPLETE |
| Milestone | C Operations & Closeout |
| Start commit | `87d4fb7`（CP8開始引継ぎ） |
| Implementation branch | `feature/phase1b-operations-closeout` |
| Framework ownership候補 | cloud非依存な外部起動・結果contract。新規artifact／Java APIは追加しない案を推奨 |
| Tooling ownership | Customer-like maintenance task、実OS process競合、crash／retry、隔離script |
| Customer ownership | task key、task実処理、Customer table／migration、外部scheduler設定 |
| Public Java API | 追加なし（推奨案） |
| Production implementation | Owner承認scope内で完了。Framework成果物の変更なし |

Owner承認済みcontractをCustomer-like Consumerへ実装し、細粒度test、同一JARの実OS process競合、
process kill後のretryおよびCP1〜CP7回帰をlocalで完了した。DoD 1b-7を`LOCAL COMPLETE`と判定する。

## 2. 開始preflight

| 項目 | 確認結果 |
|---|---|
| branch | `feature/phase1b-operations-closeout` |
| baseline | main merge commit `b3973e66134898765b95796c3622aaa68759b4fd` |
| handoff | commit `87d4fb7`、remote branchへpush済み、upstream設定済み |
| worktree | Gate 8-1開始時点でclean |
| Java / Maven | Temurin 21.0.12.1 / Maven Wrapper 3.9.16 |
| Docker | CLI 29.5.3-rd / Engine 29.5.3。承認済み実行経路で接続を確認 |
| OpenSpec | Repositoryに採用済みchangeなし |
| Milestone B | CP7 validationにより`COMPLETE / ACCEPTED` |

Gate 8-3ではDocker Engine接続を再確認し、PostgreSQL 17.11の専用containerで実process acceptanceを完了した。

## 3. 公式仕様と実効構成

### 3.1 Spring Boot 4.1.1 non-web process

Spring Boot 4.1.1公式の
[SpringApplication](https://docs.spring.io/spring-boot/reference/features/spring-application.html)と
[non-web application guide](https://docs.spring.io/spring-boot/how-to/application.html#howto.application.non-web-application)は、
Web dependencyを持つ同一artifactでも`WebApplicationType.NONE`を明示してnon-web contextを起動できること、
起動後の単発処理に`ApplicationRunner`／`CommandLineRunner`を使用できることを示している。
同公式の
[embedded web server guide](https://docs.spring.io/spring-boot/how-to/webserver.html#howto.webserver.disable)では、
`spring.main.web-application-type=none`によりembedded web serverを無効化できる。

また、`ExitCodeGenerator`と`SpringApplication.exit(...)`の結果を`System.exit(...)`へ渡す標準経路がある。
このため、専用scheduler library、独自runner annotationまたは別実行artifactを追加せず、既存Consumerの同一
executable JARからdedicated processを構成できる。

### 3.2 PostgreSQL advisory lock

PostgreSQL公式の
[Advisory Locks](https://www.postgresql.org/docs/current/explicit-locking.html#ADVISORY-LOCKS)と
[Advisory Lock Functions](https://www.postgresql.org/docs/current/functions-admin.html#FUNCTIONS-ADVISORY-LOCKS)は、
application-definedな64-bit keyまたは2つの32-bit keyでlockを識別できること、
`pg_try_advisory_lock`が待機せず取得成否を返すこと、session-level lockが明示解放またはsession終了まで保持され、
異常切断を含むsession終了時に解放されることを定義している。

したがって、task実行中だけ専用JDBC connectionを保持し、同じconnectionで取得／解放する方式なら、connection poolへ
lock保持sessionを返さず、process crash後にもlease期限、renewal、fencingまたはlock table cleanupを必要としない。
JDBC connection wrapperをtask完了前にcloseしてpoolへ返す実装は採用しない。

### 3.3 Repositoryの再利用可能範囲

- ConsumerはSpring Boot 4.1.1の同一executable JARを既にpackageできる。
- `koiki-starter-data`経由でDataSource、Flyway二階層およびPostgreSQL driverを利用できる。
- `koiki-starter-observability`のSpring Boot標準structured logging既定をnon-web processでも再利用できる。
- HTTP `requestId`はServlet filter所有である。maintenance processの相関には別の`executionId`を使う。
- ConsumerはRoot Reactor外のTooling-owned独立buildであり、Customer-like taskとmigrationをFrameworkへ移さず実証できる。

## 4. Contract／artifact比較

| 案 | 配置 | 利点 | 主な負担・リスク | 判定 |
|---|---|---|---|---|
| A. Consumer直接構成 | 既存Consumerの業務moduleとapplication assembly | Spring Boot／JDBC標準だけで成立。Framework artifact、Public API、release unit差分なし | 顧客ごとに小さなadapter実装が必要。将来の複数案件で共通契約が判明した場合は再評価 | **推奨** |
| B. 既存Framework leafへ追加 | `koiki-starter-data`等のinternal auto-configuration | 条件付き構成を配布できる | task呼出契約をinternalだけでは利用できずPublic API化しやすい。data／operation責務も混在 | 不採用 |
| C. 単一実行専用leaf | 新規Starter／library | 将来の再利用点を明示できる | 最初の1 fixtureだけでartifactとPublic APIを固定し、将来Batch責務を先行しやすい | 不採用 |
| D. lease付きlock table | FrameworkまたはCustomer migration＋adapter | connectionを長時間保持しない | owner token、DB時刻、renewal、fencing、回収、監視が必要。CP8要件には過剰 | reserve |
| E. 外部scheduler保証のみ | ECS／Kubernetes等 | application実装が少ない | cloud依存で、複数process／DB競合Evidenceとcrash recoveryを満たさない | 不採用 |

推奨案Aは「Frameworkに再利用Java APIを追加しない」という判断であり、DoD 1b-7の外部contractを削るものではない。
専用processの入力、単一実行、結果、exit status、structured logをCustomer-like Consumerで固定し、Phase 2以降の
2つ目の実利用で安定した共通Java contractが必要と判明した場合だけ、別change／ADRでFramework昇格を再評価する。

## 5. 推奨設計

### 5.1 Ownership／module／Tier

| 観点 | 推奨 |
|---|---|
| Ownership / module | Tooling-owned Consumerの既存`workitem` moduleがCustomer-like maintenance taskを所有 |
| Tier | Tier 1 SIMPLEを維持。状態遷移、複数Entity invariant、共有業務ruleを追加しない |
| Inbound adapter | `workitem.adapter.inbound.command`の条件付き`ApplicationRunner`。入力受付、形式検証、結果からexit codeへの変換だけ |
| Application Use Case | 実行identity生成、lock取得結果による分岐、task処理順序、lifecycle logの調整 |
| Outbound port / adapter | module内の使用中portと、専用JDBC connectionを保持するPostgreSQL advisory-lock adapter |
| Persistence / model | taskの外部観測可能な副作用は既存JPA方針を維持。advisory lockだけをJDBCで扱い、lockをDomain Modelへ入れない |
| Application assembly | 既存mainがmaintenance modeだけ`WebApplicationType.NONE`と`SpringApplication.exit`を選ぶ。通常Web起動は変更しない |
| Module collaboration | なし。`workreview`や他moduleのUse Case／Domain／Repositoryを直接参照しない |
| View / API | HTTP endpoint、`@Scheduled`、production failure／kill endpointを追加しない |

新規business moduleは追加しない。既存`workitem`のmaintenance taskとして所有者を固定し、application module直下へ
runner、use case、repositoryを横断配置しない。

### 5.2 起動contract

同一の`runtime-foundation-consumer-application-0.1.0-SNAPSHOT.jar`を次の2 modeで使う。

- 通常起動: 現在どおりServlet Web applicationとして起動し、maintenance runnerは存在しない。
- maintenance起動: 明示的なmodeと既知のtask keyをapplication argumentで渡し、mainが
  `WebApplicationType.NONE`を強制してrunner完了後にJVMを終了する。

mode指定だけでnon-web化し、operatorが`server.port=-1`を指定する方式にはしない。`server.port=-1`はWeb contextを
残すため、CP8のdedicated non-web process contractには使用しない。

### 5.3 lock keyとconnection lifecycle

- CLIのtask keyはConsumerが定義する既知の値へ限定し、入力時に拒否できるようにする。
- DB lock識別子にはPostgreSQLの2つの32-bit key空間を使い、Consumer namespaceとtaskごとの固定IDを明示mappingする。
- 任意文字列のhashだけでlock keyを生成せず、hash collisionで異なるtaskが競合する余地を作らない。
- `DataSource.getConnection()`で取得した専用connection上で`pg_try_advisory_lock(int, int)`を実行する。
- acquiredの場合はtask完了まで同じconnectionを保持し、`finally`で同じconnectionからunlockしてからcloseする。
- contendedの場合は業務Use Caseへ進まず、DB副作用なしでconnectionをcloseする。
- process kill時はPostgreSQL session終了による自動解放を使い、永久lockを残さない。

### 5.4 result／exit status／log contract

| 結果 | exit status | 外部効果 | structured log |
|---|---:|---|---|
| `SUCCEEDED` | `0` | task副作用をcommit | started、acquired、succeeded |
| `CONTENDED` | `10` | task副作用なし | started、contended |
| `INVALID_ARGUMENT` | `64` | DB接続／task副作用なし | failed、`errorCode=INVALID_ARGUMENT` |
| `FAILED` | `1` | transaction rollback。lockは解放 | started、必要ならacquired、failed |

競合skipはschedulerから区別可能にするため`10`を推奨する。retry policyは外部schedulerのCustomer設定であり、
Framework contractへ含めない。入力不正の`64`はusage errorとして運用上の再試行対象外にできる。

各lifecycle logは少なくとも`operation`、`result`、`taskKey`、`executionId`、`lockOwner`を持ち、完了時は
`elapsed`、失敗時はcredentialやJDBC URLを含まない`errorCode`を加える。maintenance相関にHTTP `requestId`を使わず、
processごとに生成するUUID `executionId`を使う。CP8が追加する独自lifecycle logへcredential、JDBC URL、内部lock数値、
stack traceを出さない。Spring Boot、Flyway、JDBC driver等を含むprocess stdout全体の出力制御は別の運用設定であり、
CP8の非露出Evidenceには含めない。

## 6. Gate 8-3検証設計

### 6.1 細粒度fixture

- maintenance modeのpositive／absence／unknown task key／通常Web mode back-off
- `WebApplicationType.NONE`とrunnerの条件、結果からexit codeへのmapping
- DataSource／PostgreSQL classpath absence時にFramework側へBeanや依存を追加していないこと
- advisory lockのacquired／contended／明示解放／task失敗後解放
- Public API、artifact、runtime dependency、production migration inventory

### 6.2 実OS process acceptance

packageした同一JARと同一PostgreSQL 17へ、同じtask keyの2 processを起動する。test harnessがDB側で
taskの更新対象rowを一時的にlockし、先行processをadvisory lock取得後の業務更新で待機させる。これにより
production sourceへsleep／failure switch／kill endpointを追加せず、後続processの競合を決定的に再現する。

1. 先行processがadvisory lockを取得したことをPostgreSQLとstructured logで確認する。
2. 後続processが`CONTENDED`／exit `10`で終了し、業務副作用がないことをDBで確認する。
3. harness側row lockを解放して先行processを完了させ、副作用countが1であることを確認する。
4. 異なるtask keyの2 processが不必要に相互排他しないことを確認する。
5. 別試行で先行processをOSから強制終了し、PostgreSQL session lockが消えるまでbounded pollingする。
6. 後続processが同じtask keyを取得して成功し、永久lockがないことをDBで確認する。
7. JAR process、補助DB session、container、隔離Maven repositoryを`finally`でcleanupする。

process外からlistening portが存在しないことも確認し、logだけをnon-webまたは単一副作用の証拠にしない。

### 6.3 隔離aggregate

`build-support/runtime-foundation-verification/verify-cp8-single-execution.ps1`を追加し、空のMaven repositoryへの
正式release unit stage、細粒度fixture、Consumer独立build、同一JARのprocess競合、crash／retry、CP1〜CP7回帰、
boundary inventoryおよびcleanupを一括実行する。

## 7. 差分inventory（推奨案）

| 対象 | 予定差分 |
|---|---|
| Framework release unit | なし。10 projects維持 |
| Framework artifact | なし |
| Framework Public Java API | なし |
| Framework runtime dependency | なし |
| Framework production migration／table | なし |
| Consumer module | 新規moduleなし。既存`workitem`と`application`だけを変更 |
| Consumer migration／table | task副作用をDBで観測する最小Customer migrationを追加 |
| Tooling | CP8細粒度test、実process harness、隔離aggregateを追加 |
| CI workflow | CP8単独では変更なし |

## 8. Owner Review

| Review項目 | 推奨案 | 状態 |
|---|---|---|
| Framework contract / artifact | 外部観測contractだけを確定し、新規artifact／Java APIなし | ACCEPTED |
| Consumer module / Tier | 既存`workitem`、Tier 1 | ACCEPTED |
| launcher / responsibility | 同一JAR、maintenance modeだけnon-web、runnerはinbound command | ACCEPTED |
| 排他方式 | PostgreSQL session advisory lock＋task中の専用connection保持 | ACCEPTED |
| crash recovery | process session終了による自動解放＋bounded retry検証 | ACCEPTED |
| result / exit | success `0`、contended `10`、invalid `64`、failure `1` | ACCEPTED |
| correlation / log | `executionId`を使用し、`requestId`を流用しない | ACCEPTED |
| migration / Public API | Customer副作用tableだけ。Framework migration／Public APIなし | ACCEPTED |
| acceptance | 同一JARの2 process、DB副作用1回、OS kill後retry | ACCEPTED |

**Decision:** ACCEPTED  
**Decided by:** Shuichi Kataoka  
**Decision date:** 2026年8月29日

本承認に基づきGate 8-3のproduction／test実装へ進む。Public API、新規Framework artifact、lock tableまたは
新規business moduleが必要になった場合は本承認のscope外として停止し、再度Owner Reviewする。

## 9. Gate 8-3実装結果

### 9.1 実装

- 同一Consumer JARのmaintenance modeだけをnon-web化し、runner完了後にSpring標準exit code経路で終了する。
- `workitem` moduleにcommand adapter、単一実行Use Case、outbound lock port、PostgreSQL adapterを配置した。
- PostgreSQL adapterは専用connectionでsession advisory lockを取得し、task完了時に明示解放してcloseする。
- Customer migration `V4__create_work_item_maintenance.sql`だけを追加し、副作用回数と最終executionを観測可能にした。
- 独自lifecycle logは`executionId`で相関し、HTTP `requestId`、credential、JDBC URL、内部lock数値を出力しない。

### 9.2 Local evidence

| 検証 | 結果 |
|---|---|
| Framework formal release unit | SUCCESS、10 projects |
| Architecture Contract | SUCCESS、4 tests |
| ArchUnit Rules | SUCCESS、66 tests |
| Runtime細粒度fixture | SUCCESS、30 tests |
| Consumer `workitem` | SUCCESS、7 tests |
| Consumer `workreview` | SUCCESS、5 tests |
| Consumer application | SUCCESS、25 tests |
| Consumer executable JAR | SUCCESS、同一artifactで通常Web／maintenance non-webを確認 |
| invalid input | exit `64`、task副作用なし |
| 同一task key競合 | winner exit `0`、contender exit `10`、DB副作用count `1` |
| 異なるtask key | 独立実行成功、不必要な相互排他なし |
| commit前process kill／retry | session lock消失後、同一key retry exit `0`、DB副作用count `1` |
| Database | PostgreSQL `17.11` |
| 実process evidence | winner PID `33624`、contender PID `13940`／exit `10`、kill対象 PID `17924`、retry PID `10524`／exit `0` |
| Aggregate実測 | `00:05:32.2772422`、script exit `0` |
| Cleanup | child process、補助DB session、container、隔離Maven repositoryを削除 |

PIDは環境固有の一時値であり、contract判定にはexit status、PostgreSQL lock状態、DB副作用件数を用いる。

### 9.3 完了inventory

| 対象 | 結果 |
|---|---|
| Framework release unit / artifact | 差分なし、10 projects維持 |
| Framework Public Java API | 追加なし |
| Framework runtime dependency | 追加なし |
| Framework production migration／table | 追加なし |
| Consumer module | 新規moduleなし。既存`workitem`／`application`だけを変更 |
| Consumer dependency | Consumer `workitem`にSpring Boot API／conditional構成用依存を追加 |
| Consumer migration／table | Customer-owned `V4`／`kkbiz_work_item_maintenance`だけを追加 |
| CI workflow | CP8単独の変更なし。Milestone C closeoutで統合判断 |

**Gate 8-3判定:** COMPLETE  
**CP8判定:** LOCAL COMPLETE  
**次の状態:** CP9 START READY

## 10. Javaアプリケーション実行基盤としての参考所見

本節はCP8結果から得た将来判断向けの参考所見であり、グランドデザイン、Phase 1b実行計画、後続Phaseのscopeまたは
Framework成果物を変更する決定ではない。単一のCustomer-like Consumerだけを根拠に、新規Starter、Public Java API、
専用実行artifactへ昇格しない。

### 10.1 CP8で確認できた適合性

- 同一executable JARをServlet Web applicationとdedicated non-web processの双方で起動でき、maintenance modeでは
  embedded web serverのlistening portを持たずに処理と終了statusを外部観測できた。
- Parent／BOM、Architecture Contract、業務module境界、Spring DI、transaction、JPA、Flyway二階層、structured
  loggingおよびTestcontainersは、HTTP request処理を前提にせずnon-web processでも利用できた。
- Command Adapter → Application Use Case → Outbound Port → PostgreSQL Adapterの依存方向を維持でき、Web Controller、
  Servlet requestまたはHTTP `requestId`へ単一実行処理を結合せずに済んだ。
- Spring Boot標準の`WebApplicationType.NONE`、`ApplicationRunner`、`ExitCodeGenerator`とJDBC／PostgreSQL標準機能で
  成立し、KOIKI独自のrunner frameworkまたはscheduler abstractionを必要としなかった。
- 実OS processと実PostgreSQLにより、同一task keyの同時実行排他、異なるkeyの独立性、commit前process kill時の
  session lock自動解放とretryを確認した。

以上から、KOIKIの骨格はWeb frameworkとしての利用だけでなく、PostgreSQLを利用するDB中心の短時間な単発Java processを
Customer側で構成する土台としても自然に適合した、と評価できる。

### 10.2 CP8からは確定しない耐性と適用範囲

- CP8が証明したのは同時実行による二重起動防止であり、at-most-onceまたはexactly-onceではない。業務transactionの
  commit後、success log／process exit前にcrashした場合のretryでは、副作用が再実行され得る。task固有の冪等性、
  実行履歴またはSpring Batchの再開性は後続の利用要件として扱う。
- process kill試験はDB row lockで業務更新をcommit前に停止した状態を対象とする。SIGTERM等のgraceful termination、
  長時間処理、timeout、外部I/Oのcleanup、異常終了中の部分成功は未検証である。
- advisory lock adapterはPostgreSQL固有であり、Oracleを含むDB非依存の単一実行方式は確定していない。
- 同一JARは`koiki-starter-api`等のWeb classpathも保持する。CP8はWeb／non-web両用の適合性を示すが、軽量なCLI／batch
  専用artifactとしての起動時間、memory、classpath最小性は示さない。
- 検証scriptは独自lifecycle eventの相関項目とHTTP `requestId`非使用を検査するが、依存libraryを含むprocess stdout
  全体のcredential／JDBC URL非露出までは検査しない。
- 外部scheduler製品、cloud固有deployment、Spring Batch、複数種類の実業務taskはCP8のscope外である。

### 10.3 将来の再評価条件

複数の独立した実利用で共通contractが安定し、Spring標準だけでは重複を解消できず、業務語彙を含まないことが確認された
場合に限り、Javaアプリケーション実行基盤としてのFramework昇格を別change／ADRで再評価する。それまではCP8実装を
Tooling-owned Consumer evidenceとして維持し、現行グランドデザインおよび実装計画へ追加scopeを発生させない。
