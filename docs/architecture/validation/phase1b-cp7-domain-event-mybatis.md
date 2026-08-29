# Phase 1b CP7 Domain Event／MyBatis BOM検証

## 1. 判定

| 項目 | 結果 |
|---|---|
| Phase / status | Phase 1b CP7 LOCAL COMPLETE |
| Milestone | B Data & Runtime Integration（local complete、PR CI pending） |
| Start commit | `9d6ac2c`（CP6 local complete） |
| Branch | `feature/phase1b-data-runtime-integration` |
| Framework ownership | MyBatis Spring Boot Starter 4.1.0のBOM管理だけ |
| Tooling ownership | Tier 1／2 Consumer、test-scope Named Interface strategy、隔離検証script |
| Public Java API | 追加・削除なし。既存`PersistenceTechnology.MYBATIS`を維持 |

## 2. 公式仕様との整合

Spring Modulith公式の[Application Modules Fundamentals](https://docs.spring.io/spring-modulith/reference/fundamentals.html)は、
module内のnested packageを他moduleへ公開する場合にNamed Interfaceを使い、
`ApplicationModuleDetectionStrategy.detectNamedInterfaces`でprogrammaticに検出できるとしている。
同公式APIの[`@NamedInterface`](https://docs.spring.io/spring-modulith/docs/current/api/org/springframework/modulith/NamedInterface.html)は
runtime retentionである。このためproduction annotationを付けるとmain側へModulith依存が必要になる。

CP7ではArchUnitをprimary contractに維持し、`spring-modulith-starter-test`のtest scopeだけで
`ApplicationModuleDetectionStrategy`と`NamedInterfaces.builder(...).recursive().matching("domain.event")`を使う。
`workitem.domain.event`だけが明示Named Interfaceとなり、`workreview`からのevent参照を
`ApplicationModules.verify()`で検証する。production artifactとruntime treeへModulithを追加しない。

MyBatisは公式releaseとMaven CentralでSpring Boot Starter 4.1.0を確認し、KOIKI BOMの
dependency managementだけへ追加した。versionを持たない独立probe POMが4.1.0へ解決されることを検証し、
production POM、runtime treeにはMyBatisを追加していない。

## 3. Domain Event実装

| 規約 | 実装・証拠 |
|---|---|
| event contract | `WorkItemCreated(UUID workItemId, String label)` immutable record。値だけでDomain Model非参照 |
| sender | Tier 1 `CreateWorkItemUseCase`がJPA save後、同じ`@Transactional`内で同期publish |
| receiver | Tier 2 `adapter.inbound.event.WorkItemCreatedListener`が`RecordWorkReviewUseCase`へ委譲 |
| 依存方向 | `workreview`が参照する他module typeは`workitem.domain.event`だけ |
| positive | HTTP作成後、`kkbiz_work_item`とPENDINGの`kkbiz_work_review`を確認 |
| negative / restore | reviewの100文字上限へ101文字を渡し、安全なHTTP 422 Problem Details後に両tableが0件であることを確認 |
| identity | DB識別子ベースの`equals` / `hashCode`。`getClass()`を使わず、実Hibernate未初期化proxyとの双方向同一性を確認 |
| persistence | Tier 2 JPA共有モデル。nullable wrapper `@Version`で新規時は`persist`、Customer migrationはversion非null、競合時は楽観的lock |

受信側の業務invariant例外をproductionのtest switchなしで発生させ、Spring標準の同期eventが呼出元
transactionへ例外を戻すことを実DBで確認した。`@TransactionalEventListener`、
`@ApplicationModuleListener`、非同期event、Event Publicationは使用していない。

最終差分reviewでは、受信側の業務拒否を内部障害500として公開していた点、Tier 2 Entityの
識別子同一性が未実装だった点、状態遷移へ楽観的lockがなかった点を検出した。業務制約は
`workreview`に保ち、Domain生成時の業務例外だけをApplication例外へ変換し、同moduleのinbound MVC adviceで
`WORKREVIEW-001`を持つ422 Problem Detailsへ変換した。`type`、`title`、`status`、`detail`、`instance`、`code`と
内部情報非露出を実HTTPで確認した。さらに識別子同一性はgetter経由とし、`EntityManager.getReference()`の
未初期化Hibernate proxyと双方向で確認した。`@Version`はnullable wrapperにしてSpring Dataの新規判定を
`persist`へ揃え、2つのPersistence Contextによる競合更新では先行判断だけが保存され、後続の古い更新が
`OptimisticLockException`で拒否されることを確認した。KOIKI Framework共通例外や業務語彙は追加していない。

## 4. MyBatis停止境界

- `koiki-dependencies-bom`へ`org.mybatis.spring.boot:mybatis-spring-boot-starter:4.1.0`を管理対象として追加した。
- Starter依存、Mapper、MyBatis configuration、業務module、migrationは追加していない。
- `PersistenceModel.SEPARATED`を追加せず、既存KOIKI-ARCH-008のMyBatis拒否を維持した。
- 既存public enum値`PersistenceTechnology.MYBATIS`は互換性のため削除していない。
- Consumer runtime dependency treeに`org.mybatis`、`org.mybatis.spring.boot`は存在しない。

## 5. 検証結果

Command:

```powershell
pwsh -NoProfile -File build-support/runtime-foundation-verification/verify-cp7-domain-event-mybatis.ps1
```

Final result:

```text
CP7 domain event, named-interface, MyBatis BOM, regression, and boundary checks succeeded.
```

| 経路 | 結果 |
|---|---|
| KOIKI release unit | 空の隔離Maven repositoryへ10 projectsをstage、全SUCCESS |
| Root contract | Architecture Contract 4件、ArchUnit Rules 66件 SUCCESS |
| 細粒度fixture | CP2〜CP6を含む30 tests SUCCESS |
| Consumer unit | workitem 2 tests、workreview Domain／Application 5 tests SUCCESS |
| Consumer application | 22 tests SUCCESS、PostgreSQL 17.11 |
| Spring Modulith Level 0 | `workitem`／`workreview`を検出、`domain.event` Named Interface、`modules.verify()` SUCCESS |
| KOIKI ArchUnit | 両moduleへ全business rulesを適用、直接参照・cycle・listener配置・event不変性 SUCCESS |
| transaction | event成功時に両table保存、receiver invariant失敗時はHTTP 422かつ両table rollback |
| entity lifecycle | 新規reviewはversion 0でinsertし、実未初期化proxyとの識別子同一性を維持 |
| optimistic lock | 競合するreview状態遷移の後続更新を拒否し、先行statusとversion 1を維持 |
| MyBatis BOM | versionless isolated probeがStarter 4.1.0へ解決 |
| runtime境界 | MyBatis／Modulith runtime／WebFlux／Reactor／Security／OpenTelemetryなし |
| artifact境界 | test strategyとModulith classのproduction feature JAR混入なし |
| cleanup | CP6のDB pause復旧と新規Testcontainers container残存ゼロ、CP6／CP7隔離repository削除 |

初回の全検証では、CP7 script末尾の`Select-String -Pattern`改行位置によりPowerShell引数解析が失敗した。
検査式を是正して全工程を先頭から再実行し、最終成功を確認した。機能test、MyBatis dependency解決、
Maven buildは初回も成功しており、是正対象は隔離scriptの境界検査だけであった。

## 6. 結論と次境界

CP7のlocal条件であるTier 1／2同期Domain Event、値eventだけの公開、直接Bean／Domain参照なし、
同一transaction rollback、Level 0、test-scope Named Interface、MyBatis 4.1.0 BOM管理のみ、runtime非混入を満たした。
Framework runtime artifactとPublic Java APIを増やしておらず、既存ADRの変更は不要である。

Milestone BのCP4〜CP7 local実装はcommit `6811960`までで完了した。通常PRでCP7 aggregate scriptを
自動実行する独立`Milestone B Integration` jobをCIへ接続し、同じscriptのlocal再実行も成功した。
pushとremote CIは未実施であり、PRで実行時間とTestcontainersの安定性を確認してからrequired check化を判断する。
