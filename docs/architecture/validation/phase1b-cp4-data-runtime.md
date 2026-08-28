# Phase 1b CP4 Data Runtime Integration検証

## 1. 目的と現在判定

`koiki-starter-data`、`koiki-testing`およびCustomer-like Runtime Consumerを使い、PostgreSQL 17上で
KOIKI／Customer二階層Flywayと、HTTP→Application Use Case→Spring Data Repository→DBの実行経路を
検証する。

| 項目 | 結果 |
|---|---|
| Phase / status | Phase 1b CP4 LOCAL COMPLETE |
| Milestone | B Data & Runtime Integration |
| Start commit | `c6f2f86`（PR #24 merge後main） |
| Branch | `feature/phase1b-data-runtime-integration` |
| Framework ownership | persistence-neutral Flyway実行順とowner別既定 |
| Tooling ownership | `koiki-testing`、Customer-like Consumer、migration／failure fixture |
| Public Java API | 追加しない |

## 2. 設計判断

### 2.1 Flywayと業務moduleの境界

- `koiki-starter-data`は同じDataSourceでKOIKI Flywayを先行実行し、その後Spring Boot管理のCustomer
  Flywayを実行する。
- KOIKIは`classpath:db/migration/koiki`と`koiki_flyway_history`、Customerは
  `classpath:db/migration/customer`と`flyway_schema_history`を所有する。
- Starterへ架空のFramework tableや業務SQLを同梱しない。CP4のKOIKI probeはConsumerのtest resources、
  `kkbiz_work_item`はConsumerのmain resourcesが所有する。
- Customer-like `workitem`はTier 1 SIMPLEを維持し、ControllerはUse Caseだけを呼び、Spring Data
  RepositoryとJPA modelを外部へ露出しない。

### 2.2 `@Transactional`と`final` Use Case

Phase 1a Feature Templateはruntime統合を先行しないため、Use Caseを`final`で生成し、Spring Bean登録と
transaction境界を後続Phaseへ保留していた。CP4で`CreateWorkItemUseCase#create`へ`@Transactional`を
追加して実Applicationを起動したところ、Spring Boot既定のclass-based proxy（CGLIB）が`final` classを
subclass化できず、ApplicationContextが起動失敗した。

次をCP4のruntime統合判断とする。

1. Application Use Caseをtransaction境界とし、Spring標準の`@Transactional`を使用する。
2. class-based proxy対象のUse Case classとtransactional methodをnon-finalにする。これはproxy回避ではなく、
   Spring AOP contractを満たすための設計である。
3. proxy対策だけを理由にUse Case interfaceを追加しない。Boot既定のclass-based proxyを維持したままでは
   interface追加だけで解決せず、Tier 1へ不要な契約を増やすためである。
4. `TransactionTemplate`はfinalを維持できるが、標準経路をprogrammatic transactionへ置き換える理由がない。
   AspectJ weavingもPhase 1bの範囲を超えるため採用しない。
5. Phase 1a Tier 1／Tier 2 Feature TemplateのUse Caseもnon-finalへ更新し、実Application統合時に同じ
   起動failureを再発させない。
6. test-only Repository decoratorで実DB save後に例外を発生させ、Use Case transactionにより行が
   rollbackされることをPostgreSQLで実証する。本番sourceへfailure switchを追加しない。

この判断は業務interfaceやFramework Public APIを新設せず、グランドデザインのApplication Use Caseが
transaction境界を所有する方針と整合する。

## 3. 検証結果

Command:

```powershell
pwsh -NoProfile -File build-support/runtime-foundation-verification/verify-cp4-data-runtime.ps1
```

Final result:

```text
CP4 isolated artifact, Flyway failure/restore, transaction, Consumer DB,
and dependency checks succeeded.
```

| 経路 | 結果 |
|---|---|
| KOIKI release unit | 空の隔離Maven repositoryへ8 projectsをstage、全SUCCESS、約1分 |
| Root contract | Architecture Contract 4件、ArchUnit Rules 66件 SUCCESS |
| Data Starter細粒度fixture | 既存API検証を含む19 tests SUCCESS。Data固有は有効／無効、Flyway単独無効、Application-owned strategyへのback-off、既定値／override |
| Starter JAR | 実装classは`internal`のみ、必須metadataあり、production migration SQLなし |
| Public API | `koiki-starter-data`、`koiki-testing`ともPublic Java type 0。Data設定property 2件 |
| Consumer unit | Tier 1 Use Case 2 tests SUCCESS |
| Consumer application | 13 tests SUCCESS、PostgreSQL 17.11、約31秒 |
| 正常経路 | 実HTTP→Controller→Use Case→Spring Data Repository→`kkbiz_work_item`永続化 SUCCESS |
| owner分離 | KOIKI／Customer locationとhistory分離、同じV1共存、KOIKI→Customer、Customer baseline 0 SUCCESS |
| migration異常／復旧 | location混在のV1衝突を拒否、Customer先行を失敗検出後にschema復元・正順成功 |
| 後続migration | Customer v5適用後にKOIKI v2を独立適用、各historyのcurrent version 5／2 |
| transaction | test-only Repository decoratorがsave後に例外を発生、Use Case transactionで実DB行0へrollback |
| dependency boundary | MVC／JPA／Flyway／PostgreSQLあり、WebFlux／Reactor／Security／MyBatis／Modulith runtimeなし。`koiki-testing`はtest scope |
| cleanup | UUID付きtest schemaおよび隔離repositoryを削除 |

最初の隔離実行では、Data Starterを細粒度fixtureのclasspathへ追加したことでAPI専用
`SpringBootTest`までDataSourceを構成する責務混在を検出した。fixtureの`application.properties`で
DataSource／Flyway auto-configurationを除外し、Data Starterは`ApplicationContextRunner`へ隔離した。
修正後は既存CP2／CP3を含む19試験が成功し、上記scriptを先頭から再実行して最終成功を確認した。

CDS archive差、Surefire native stream、Mockito dynamic agentの既知warningはtest failureではない。
全testはfailure、error、skip 0で終了した。

## 4. CI境界と結論

CP4のlocal完了条件である二階層Flyway、PostgreSQL 17、業務DB経路、異常／復旧、transaction、artifact、
ownershipおよびdependency境界を満たした。KOIKI独自Public Java APIやproduction migration SQLは追加して
いない。既存ADRの前提変更はなく、ADR追加・改訂は不要である。

remote CIは実行計画どおりCP4単独では接続せず、CP5〜CP7を含むMilestone BのPRで
PostgreSQL Testcontainersを通常CIへ追加する。次の実装CPはCP5 構造化log／相関ID／`@Async`伝播である。
