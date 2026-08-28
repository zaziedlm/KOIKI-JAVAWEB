# Phase 1b CP5 Observability検証

## 1. 目的と現在判定

`koiki-starter-observability`とCustomer-like Runtime Consumerを使い、構造化JSON log、HTTP相関ID、
`@Async`境界のlogging context伝播、および非同期thread再利用時のcontext cleanupを検証する。

| 項目 | 結果 |
|---|---|
| Phase / status | Phase 1b CP5 LOCAL COMPLETE |
| Milestone | B Data & Runtime Integration |
| Start commit | `4e989dd`（CP4 commit） |
| Branch | `feature/phase1b-data-runtime-integration` |
| Framework ownership | structured logging既定、request correlation、`requestId`のTaskDecorator伝播 |
| Customer ownership | `@EnableAsync`、業務Use Case、業務log語彙、追加TaskDecorator |
| Tooling ownership | 細粒度fixture、Customer-like Consumer、thread再利用受入試験 |
| Public Java API | 追加しない |

## 2. 設計判断

### 2.1 構造化logと相関ID

- Spring Boot組込みLogstash JSONをconsoleの低優先度既定とし、Applicationによるoverrideを妨げない。
- 共通項目は`timestamp`、`level`、`service`、`environment`とし、SLF4J MDCおよびfluent key-valueを
  JSON memberへ含める。
- Servlet requestの`X-Request-ID`は`[A-Za-z0-9._:-]{1,128}`だけを受け入れる。不在または不正値なら
  UUIDを生成し、response headerとMDC `requestId`へ設定する。
- filter終了時は従前の`requestId`を復元し、存在しなければ削除する。request間でMDCを残さない。
- exporter、cloud backend、SecurityContext、業務固有のlog abstractionは追加しない。

### 2.2 `@Async`とTaskDecoratorの境界

- FrameworkはMicrometer Context PropagationとSpringの`ContextPropagatingTaskDecorator`を使う。
  global MDC全体ではなく`requestId`だけを登録し、Customer側decoratorが追加したMDCを上書きしない。
- Applicationが`@EnableAsync`と`@Async` Use Caseを所有する。Starterは非同期処理を無条件に有効化しない。
- `@Async`対象のUse Caseをnon-finalとする。CP4の`@Transactional`と同様に、Boot既定のclass-based proxyが
  methodをinterceptできる形であり、proxyを回避するための独自interfaceは追加しない。
- Spring Bootが複数の`TaskDecorator` beanを合成する経路を使い、Customer decoratorとの共存を
  実Consumerで確認する。
- `koiki.observability.enabled`、`correlation.enabled`、`context-propagation.enabled`で全体または
  部分的に無効化できる。

## 3. 検証結果

Command:

```powershell
pwsh -NoProfile -File build-support/runtime-foundation-verification/verify-cp5-observability.ps1
```

Final result:

```text
CP5 isolated artifact, structured logging, correlation, async propagation,
thread cleanup, and dependency checks succeeded.
```

| 経路 | 結果 |
|---|---|
| KOIKI release unit | 空の隔離Maven repositoryへ9 projectsをstage、全SUCCESS、約1分 |
| Root contract | Architecture Contract 4件、ArchUnit Rules 66件 SUCCESS |
| Observability細粒度fixture | 既存CP2〜CP4を含む27 tests SUCCESS。CP5固有8件は伝播／cleanup、Customer decorator共存、安全／不正header、全体／部分無効、既定／override |
| Starter JAR | 実装classは`internal`のみ。auto-configuration、environment post-processor、metadataを格納 |
| Public API | Public Java type 0、設定property 3件 |
| Consumer unit | Tier 1 Use Case 2 tests SUCCESS |
| Consumer application | 15 tests SUCCESS、PostgreSQL 17.11、約34秒 |
| 構造化log | `timestamp`、`level`、`service=runtime-foundation-consumer`、`environment=acceptance`、業務key-valueを実JSONで確認 |
| 相関ID | 安全なincoming IDをresponse／同期MDC／非同期logで維持。不在時はUUIDを生成 |
| `@Async`伝播 | 実HTTP→Controller→`@Async` Use Caseで同じ`requestId`を確認 |
| decorator共存 | Customer-owned decoratorの`customerDecorator=applied`とFrameworkの`requestId`が同じ非同期logに共存 |
| thread漏洩負例 | pool size 1で同じ`task-1`を再利用し、2回目は1回目のIDでなく新規UUIDとなることを確認 |
| dependency boundary | Observability Starter／Micrometer Context Propagation／SLF4Jあり。WebFlux／Reactor／Security／MyBatis／Modulith／OpenTelemetry runtimeなし |
| CP4回帰 | Flyway異常／復旧、二階層migration、transaction rollback、HTTP→Repository→PostgreSQLを含む全試験SUCCESS |
| cleanup | Testcontainersと隔離Maven repositoryを終了・削除 |

初回Consumer検証では`spring.factories`のEnvironmentPostProcessor key誤りを検出して修正した。次の検証で、
構造化logの時刻名が`@timestamp`のままであることと、MDC全体のsnapshotがCustomer decoratorの値を
合成順により上書きし得ることを検出した。rename propertyをmap key用のbracket記法へ直し、
`Slf4jThreadLocalAccessor("requestId")`による選択伝播へ変更した。修正後に焦点試験を再実行し、最後に
上記隔離scriptを先頭から実行して成功を確認した。

CDS archive差、Surefire native streamおよびMockito dynamic agentの既知warningはtest failureではない。
全testはfailure、error、skip 0で終了した。

## 4. CI境界と結論

CP5のlocal完了条件であるJSON項目、incoming／generated相関ID、`@Async`伝播、Customer拡張との共存、
thread再利用漏洩負例、無効化／override、artifact／Public API／dependency境界を満たした。
KOIKI独自Public Java APIやlogging abstraction、exporterは追加していない。既存ADRの前提変更はなく、
ADR追加・改訂は不要である。

remote CIは実行計画どおりCP7までを含むMilestone BのPRで接続する。次の実装CPはCP6
Actuator health／DB readinessである。
