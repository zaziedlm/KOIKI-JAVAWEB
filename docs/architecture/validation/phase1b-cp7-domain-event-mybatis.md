# Phase 1b CP7 Domain Event／MyBatis BOM検証

## 1. 判定

| 項目 | 結果 |
|---|---|
| Phase / status | Phase 1b CP7 COMPLETE（PR #25 MERGED、main CI SUCCESS） |
| Milestone | B Data & Runtime Integration `COMPLETE / ACCEPTED` |
| Start commit | `9d6ac2c`（CP6 local complete） |
| CP7 commit | `6811960` |
| PR CI head | `6f99f63b18fc40c43d1709f60abc4ebce3c0456e` |
| Final PR head | `84703a892b84e4980d30473131ca388a7e6aa453` |
| Merge commit | `b3973e66134898765b95796c3622aaa68759b4fd` |
| Milestone B required check | `ACCEPTED`（main rulesetへ追加済み） |
| Implementation branch | `feature/phase1b-data-runtime-integration` |
| Closeout record branch | `feature/phase1b-operations-closeout` |
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

### 5.1 Milestone B PR CI

[Draft PR #25](https://github.com/zaziedlm/KOIKI-JAVAWEB/pull/25)で、通常CIとJava runtime compatibilityを
実行した。初回head `50f50b0cab5b8d86f5124c8016faa04f2b5a9aaf`の
[CI run 33239250846](https://github.com/zaziedlm/KOIKI-JAVAWEB/actions/runs/33239250846)では、
`Milestone B Integration`が3分04秒、`Public API Compatibility`が1分17秒で成功した。
`Verify (ubuntu-24.04)`もRoot、Feature Template、NullAway、CP3内のbuildとConsumer 22 testsまでは成功したが、
歴史的なCP3 aggregateがCP4で承認済みの`spring-data-jpa`を後続依存として拒否し、最後の境界検査だけ失敗した。

CP3 scriptの当時の禁止contractは変更せず、現在の通常CIから歴史的aggregateの重複呼出を外した。
CP2／CP3回帰は、承認済みData依存を含む現在のConsumerを対象にするCP7 aggregateへ引き継いだ。
是正commitは`2d3705581866009008297b7f6a5b2abe80178e58`である。

是正後の[CI run 33239813239](https://github.com/zaziedlm/KOIKI-JAVAWEB/actions/runs/33239813239)と
[Java Runtime Compatibility run 33239813233](https://github.com/zaziedlm/KOIKI-JAVAWEB/actions/runs/33239813233)で、
次を確認した。

| Check | 結果 | 所要時間 |
|---|---|---:|
| Verify (ubuntu-24.04) | SUCCESS | 4分55秒 |
| Milestone B Integration | SUCCESS | 2分55秒 |
| Public API Compatibility | SUCCESS | 1分24秒 |
| Build Runtime Fixture (Java 21) | SUCCESS | 33秒 |
| Java Runtime Compatibility | SUCCESS | 30秒 |

Evidence commit `6f99f63b18fc40c43d1709f60abc4ebce3c0456e`に対する
[CI run 33240299498](https://github.com/zaziedlm/KOIKI-JAVAWEB/actions/runs/33240299498)でも、
`Verify (ubuntu-24.04)`、`Milestone B Integration`、`Public API Compatibility`が成功した。
[Java Runtime Compatibility run 33240299494](https://github.com/zaziedlm/KOIKI-JAVAWEB/actions/runs/33240299494)も成功し、
`Milestone B Integration`は2分56秒で完了した。

`Milestone B Integration`は3回連続で成功し、各回でPostgreSQL Testcontainersの起動、DB DOWN／restore、
全Consumer testおよびcontainer cleanupを完了した。Architecture Ownerはrequired check化を`ACCEPTED`とし、
main rulesetの4番目のrequired contextへ追加した。この時点のpre-merge判定では、PR #25はDraft、
merge state `CLEAN`、Milestone Bはmerge pendingであった。

### 5.2 Milestone B main final Evidence

[PR #25](https://github.com/zaziedlm/KOIKI-JAVAWEB/pull/25)はfinal head
`84703a892b84e4980d30473131ca388a7e6aa453`からmerge commit
`b3973e66134898765b95796c3622aaa68759b4fd`として、2026年8月29日16:39 JSTにmainへmergeされた。
同一merge commitを対象とするmain pushの最終結果は次のとおりである。

| Workflow / job | Evidence | 結果 |
|---|---|---|
| `CI` / `Verify (ubuntu-24.04)` | [run 33241356803 / job 99071141411](https://github.com/zaziedlm/KOIKI-JAVAWEB/actions/runs/33241356803/job/99071141411) | SUCCESS、3分59秒 |
| `CI` / `Milestone B Integration` | [run 33241356803 / job 99071141317](https://github.com/zaziedlm/KOIKI-JAVAWEB/actions/runs/33241356803/job/99071141317) | SUCCESS、2分58秒 |
| `CI` / `Public API Compatibility` | [run 33241356803 / job 99071141409](https://github.com/zaziedlm/KOIKI-JAVAWEB/actions/runs/33241356803/job/99071141409) | SUCCESS、1分26秒 |
| `Java Runtime Compatibility` / `Build Runtime Fixture (Java 21)` | [run 33241356811 / job 99071141413](https://github.com/zaziedlm/KOIKI-JAVAWEB/actions/runs/33241356811/job/99071141413) | SUCCESS、46秒 |
| `Java Runtime Compatibility` / `Java Runtime Compatibility` | [run 33241356811 / job 99071221369](https://github.com/zaziedlm/KOIKI-JAVAWEB/actions/runs/33241356811/job/99071221369) | SUCCESS、26秒 |

main ruleset `21140116`はactiveかつstrictであり、`Verify (ubuntu-24.04)`、`Public API Compatibility`、
`Java Runtime Compatibility`、`Milestone B Integration`の4 contextをrequiredとして維持している。
PR merge、main CI、rulesetおよびlocal mainのidentityが一致したため、CP7とMilestone Bを
`COMPLETE / ACCEPTED`としてcloseする。CP8は`START READY / NOT STARTED`である。

| Owner Review項目 | 結果 |
|---|---|
| Decision | Milestone B `COMPLETE / ACCEPTED` |
| Decided by | Shuichi Kataoka |
| Date | 2026年8月29日 |
| Scope | CP4〜CP7、PR #25 merge、main CI、required check、Milestone C開始境界 |
| Evidence | merge commit `b3973e6`、runs `33241356803` / `33241356811`、4 required contexts SUCCESS |
| Next boundary | CP8 `START READY / NOT STARTED`。Public APIが必要な場合は実装前に別途Owner Review |

## 6. 結論と次境界

CP7のlocal条件であるTier 1／2同期Domain Event、値eventだけの公開、直接Bean／Domain参照なし、
同一transaction rollback、Level 0、test-scope Named Interface、MyBatis 4.1.0 BOM管理のみ、runtime非混入を満たした。
Framework runtime artifactとPublic Java APIを増やしておらず、既存ADRの変更は不要である。

Milestone BのCP4〜CP7実装はcommit `6811960`までで完了した。PR #25のfinal head `84703a8`と
main merge commit `b3973e6`で、CP7 aggregateを実行する`Milestone B Integration`を含む全checkが成功し、
Testcontainersのremote実行とcleanupを確認した。required check化も`ACCEPTED`としてmain rulesetへ反映済みである。
CP7とMilestone Bを`COMPLETE / ACCEPTED`と判定する。次は最新mainから分岐した
`feature/phase1b-operations-closeout`でCP8開始準備へ進む。
