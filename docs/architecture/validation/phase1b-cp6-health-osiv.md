# Phase 1b CP6 Health／OSIV検証

## 1. 目的と現在判定

`koiki-starter-observability`、`koiki-starter-data-jpa`およびCustomer-like Runtime Consumerを使い、
Spring Boot標準ActuatorによるDB health、liveness／readiness分類、JPA OSIVの低優先度無効化と
Entity Web境界違反の実response生成時検出を検証する。

| 項目 | 結果 |
|---|---|
| Phase / status | Phase 1b CP6 LOCAL COMPLETE |
| Milestone | B Data & Runtime Integration |
| Start commit | `16fb8f8`（CP6開始引継ぎ） |
| Branch | `feature/phase1b-data-runtime-integration` |
| Framework ownership | Actuator基本health contract、JPA OSIVの上書き可能な低優先度既定 |
| Customer ownership | DBをreadinessへ含める判断、OSIV明示override、業務Entity／DTO |
| Tooling ownership | health DB異常／復旧、test-only Entity露出負例、隔離script |
| Public Java API | 追加しない |

## 2. 設計判断

### 2.1 Actuatorのartifact配置

Actuatorは既存`koiki-starter-observability`へ追加した。候補3案の判断は次のとおりである。

1. **既存observabilityへ追加（採用）**: グランドデザインのOperations責務と一致し、log／healthを同じ
   observability leafで提供できる。
2. 新しいoperations leaf: CP6の基本healthだけでは独立artifactを必要とする責務差が実証されず、
   未使用の将来moduleを先行生成することになるため不採用。
3. ConsumerのBoot Actuator直接依存: health自体は応答するが、Framework-ownedな公開範囲とprobe既定を
   提供できないため不採用。

`spring-boot-starter-actuator`とSpring Boot標準`DataSourceHealthIndicator`を使用し、KOIKI独自
`HealthIndicator`、health用Java API、外部監視backendは追加していない。

低優先度既定は次である。

```properties
management.endpoints.web.exposure.include=health
management.endpoint.health.show-components=always
management.endpoint.health.show-details=never
management.endpoint.health.probes.enabled=true
```

Applicationは通常のSpring Boot propertyで全項目を上書きできる。

### 2.2 health公開contractとprobe分類

- `/actuator/health`はDBを含む総合healthとする。
- livenessへDBを含めない。DB障害時のinstance再起動は障害を解消せず、連鎖再起動を招くためである。
- readinessへ外部依存をFrameworkから自動追加しない。
- Customer-like ConsumerはDBなしでは主要業務経路を提供できないため、Application-owned設定として
  `readinessState,db`を明示する。
- component名とstatusは公開し、detailは公開しない。JDBC URL、username、password、SQL例外、
  connection message、stack traceをresponseへ含めない。
- `UP`はHTTP 200、`DOWN`はSpring Boot標準mappingのHTTP 503を使用する。

### 2.3 JPA profile Starter

`koiki-starter-data`はJPA／MyBatis／JdbcClientから利用できるpersistence-neutral leafであり、JPA固有設定を
混在させない。`koiki-starter-data-jpa`をFramework-owned JPA profileとして追加し、Consumerの直接
`spring-boot-starter-data-jpa`依存を置き換えた。

同Starterは`spring-boot-starter-data-jpa`を集約し、`EnvironmentPostProcessor`で
`spring.jpa.open-in-view=false`をproperty source末尾へ追加する。`koiki.data.jpa.enabled=false`で
KOIKI既定を無効化でき、Applicationの`spring.jpa.open-in-view=true`等が常に優先される。

Public Java typeは0、KOIKI設定propertyは1件で、業務Entity、Repository、migration SQLを含まない。

### 2.4 DB異常／復旧の隔離方式

停止したTestcontainers containerを同じtest内で再起動する方式は、resource reaperが停止containerを
cleanup対象にするため復旧経路として不安定であることを実測した。専用Consumer contextのPostgreSQLを
Docker pause／unpauseし、test-only JDBC socket timeoutを1秒へ設定する方式を採用した。

この方式はcontainer ID、公開port、DataSource設定を変えずに接続不能と復旧を実証でき、他test contextの
PostgreSQLを停止しない。`finally`で必ずunpauseし、JDBC接続が戻るまで最大30秒pollした後にhealthの
再度`UP`を確認する。

### 2.5 Entity露出負例

- test sourceだけにLazy `@OneToMany`を持つEntity、transactional Use Case、Controllerを配置した。
- fixtureはApplicationのcomponent scan外である`org.koikifw.cp6fixture`へ隔離し、test configurationから
  明示的に組み立てる。
- version付きtest endpointの実serverへ`RestTestClient`で接続し、HTTP response serialization完了まで実行する。
- OSIV falseではdetached EntityのLazy collection serializationがHTTP 500となり、違反を検出した。
- `spring.jpa.open-in-view=true`をApplicationから明示overrideすると、同じ違反endpointがLazy queryを発行して
  HTTP 200になることを対比し、違反検出が低下するriskを実証した。
- 正常なproduction `WorkItemController`は変更せず、DTO responseとHTTP→Use Case→Repository→DB経路を維持した。
- test-only classとendpointがSpring Boot executable JARへ混入しないことをarchive検査した。

最初のfixture配置ではtest-only ControllerをApplication base package配下へ置いたため、通常component scanと
明示importで二重登録された。また`/test-only/cp6/...`はAPI Versioningが`cp6`をversion候補として解釈した。
fixtureをscan外へ移し、既存contractと同じ`/api/{version}/...`へ変更して責務と経路を明確化した。

### 2.6 差分レビューの是正

CP6差分レビューで次の2点を検出し、最終確定前に是正した。

1. Phase 1b実行計画のartifact責務表に、`koiki-starter-observability`の対象外として
   Actuator healthが残っていた。CP6で承認・実証した配置に合わせ、Actuator基本health contractを
   同StarterのFramework責務へ更新した。Customer固有`HealthIndicator`は対象外に維持する。
2. 隔離scriptはPostgreSQLのunpauseと隔離Maven repository削除を実行していたが、Testcontainersの
   残存を成功条件として検査していなかった。Consumer build前の`org.testcontainers=true` label付き
   container IDをbaselineとし、build後に新規containerが最大30秒以内にゼロとなることを検査する。

## 3. 検証結果

Command:

```powershell
pwsh -NoProfile -File build-support/runtime-foundation-verification/verify-cp6-health-osiv.ps1
```

Final result:

```text
CP6 isolated artifact, health UP/DOWN/restore, OSIV boundary,
regression, and dependency checks succeeded.
```

| 経路 | 結果 |
|---|---|
| KOIKI release unit | 空の隔離Maven repositoryへ10 projectsをstage、全SUCCESS、47.812秒 |
| Root contract | Architecture Contract 4件、ArchUnit Rules 66件 SUCCESS |
| 細粒度fixture | 既存CP2〜CP5を含む30 tests SUCCESS、13.993秒。JPA既定／override／無効化、health公開既定／override／無効化 |
| Framework artifact | Data JPA／Observability実装classは`internal`のみ。metadataあり、production SQLなし |
| Public API | Data JPA Public Java type 0／設定property 1件。ObservabilityはPublic Java type 0／設定property 3件を維持 |
| Consumer unit | Tier 1 Use Case 2 tests SUCCESS |
| Consumer application | 19 tests SUCCESS、PostgreSQL 17.11、40.003秒 |
| DB available | 総合health 200／DB `UP`、readiness 200／DB `UP`、liveness 200 |
| DB unavailable | PostgreSQL pause中、総合health 503／DB `DOWN`、readiness 503／DB `DOWN`、liveness 200 |
| DB restore | unpause後にJDBC接続と総合health／readinessが200／`UP`へ復帰 |
| 情報非露出 | responseにJDBC URL、credential、`jdbc:`、SQL例外、connection message、stack traceなし |
| OSIV既定 | 実Environmentで`spring.jpa.open-in-view=false` |
| Entity露出負例 | OSIV falseはresponse生成時500、Application override trueは同じ経路が200 |
| 正常業務経路 | DTOを返す既存HTTP→Use Case→Repository→PostgreSQLおよびtransaction rollback SUCCESS |
| CP4／CP5回帰 | Flyway二階層／異常復旧、structured log、request ID、async correlation、thread cleanup SUCCESS |
| production artifact | test-only Entity、Controller、test classの混入なし |
| dependency boundary | Data JPA Starter、Actuator、JPAあり。WebFlux／Reactor／Security／MyBatis／Modulith runtime／OpenTelemetryなし |
| cleanup | pauseしたPostgreSQLを`finally`で復旧、新規Testcontainers container残存ゼロをscriptで検査、隔離repositoryを削除 |

レビュー是正後の再検証で、隔離Consumer build全体はworkitem 2 testsとapplication 19 testsを含め46.919秒、
dependency tree検査は5.973秒であり、script全体は約2分で完了した。全testはfailure、error、skip 0で
終了し、新規Testcontainers container残存ゼロの追加検査も成功した。

## 4. CI境界と結論

CP6のlocal完了条件であるActuator endpoint、DB UP／DOWN／restore、liveness／readiness分類、情報非露出、
OSIV falseのFramework既定、Application override、response生成までのEntity露出負例、正常DTO経路、
artifact／Public API／dependency／cleanup境界を満たした。

Spring Boot標準機能でDoDを満たしたため、KOIKI独自health Java APIや新しいoperations leafは不要である。
Phase 0 ADR-028の前提を変更せず正式構成で再実証したため、ADR追加・改訂は不要である。

remote CIは実行計画どおりCP6単独では接続せず、CP7までを含むMilestone BのPRで接続する。次の実装CPは
CP7 Named Interface／Domain Event判断およびMyBatis BOM管理である。
