# KOIKI-JavaWeb-FW Phase 1b Runtime Foundation 実行計画

**版:** v0.1<br>
**作成日:** 2026年8月28日<br>
**文書状態:** ACCEPTED — EXECUTION IN PROGRESS<br>
**実行状態:** CP0 COMPLETE / Gate 1 ACCEPTED / CP1〜CP9 LOCAL COMPLETE / Milestone A・B COMPLETE / Milestone C IN PROGRESS<br>
**Architecture Owner:** Shuichi Kataoka<br>
**最終更新日:** 2026年8月29日（CP9公式baseline採取完了、CP10開始待ち）<br>
**対象Phase:** Phase 1b Runtime Foundation<br>
**実行方式:** local検証を主経路とする最大3 milestone branch / Pull Request<br>
**開始基準main:** `c87e7a5561dff24afea7452f63cce165c666df82`<br>
**CP0 branch / source:** `feature/phase1b-runtime-core` / `9483c796675b765b0c1f342fa974cb6732db1712`

## 1. 目的

グランドデザイン v0.2 §27.5のPhase 1b成果物、DoD 1b-1〜1b-8および§27.2の全Phase共通DoDを、
独立してreview・検証できるcommit pointへ分解する。併せて、runtime artifactのOwnership、依存方向、
Public API、release unit、CI、性能計測および後続Phaseとの停止境界をGate 1で確定する。

Phase 1b成果物はFramework内部のauto-configuration testだけで完了とせず、実際の顧客アプリと同じ
依存方向・module構造・設定・起動方法を持つ、Tooling-ownedなCustomer-like Runtime Consumerから
利用できることをCP1〜CP10の横断的な受入軸とする。

本文書はWalking Skeleton codeの昇格計画ではない。Phase 0のFlyway二階層、OSIV境界、Tier 2および
同期eventの証拠はテスト観点として参照するが、Java、SQL、一時Maven座標を正式成果物へcopyしない。

## 2. 作業位置

| 項目 | 内容 |
|---|---|
| Phase / status | Phase 1b Runtime Foundation / CP0 COMPLETE、Gate 1 ACCEPTED、CP1〜CP9 LOCAL COMPLETE、Milestone A・B COMPLETE、Milestone C IN PROGRESS |
| Ownership | Framework主体。BOM、CI、非配布fixture、性能harnessはTooling |
| Current branch / baseline | `feature/phase1b-operations-closeout` / main merge commit `b3973e66134898765b95796c3622aaa68759b4fd` |
| 対象module | Gate 1で候補を承認し、各CPの細粒度fixtureまたはCustomer-like Consumerが必要としたleaf moduleだけを追加する |
| 適用指針 | Root `AGENTS.md`、Project Overview Skill、Grand Design、Repository Architecture、ADR Register、Phase 1a closeout |
| 検証 | Java 21、Maven Wrapper、Docker / Testcontainers PostgreSQL、ArchUnit、Spring Modulith Level 0、NullAway、japicmp、required checks |
| 保留 | Security、Reference業務、REST受入、Level 2、非同期Domain Event、Oracle、cloud固有実装、Project Template、正式release |

## 3. 開始時点とCP0証拠

### 3.1 Repository identity

| 項目 | 結果 |
|---|---|
| `origin/main` | `c87e7a5561dff24afea7452f63cce165c666df82` |
| branch | `feature/phase1b-runtime-core` |
| CP0開始HEAD | `9483c796675b765b0c1f342fa974cb6732db1712` |
| merge base | `c87e7a5561dff24afea7452f63cce165c666df82` |
| remote差分 | branchは`origin/feature/phase1b-runtime-core`と一致、`origin/main`から1 commit |
| worktree | baseline実行前後ともclean |
| OpenSpec | active changeおよび`openspec/` directoryなし |

### 3.2 Environment identity

| 項目 | 結果 |
|---|---|
| OS | Windows 11相当 `10.0.26100`、x64 |
| Timezone | `Tokyo Standard Time`、UTC+09:00 |
| Host logical CPU | 24 |
| Build JDK | Eclipse Temurin 21.0.12、Java runtime build 21.0.12.1+1-LTS |
| Compatibility JDK | Eclipse Temurin 25.0.4.1+1-LTS |
| Maven Wrapper | 3.3.4 / Apache Maven 3.9.16 |
| Container runtime | Rancher Desktop、Docker client 29.1.4-rd / Engine 29.1.3 |
| Container OS | Linux amd64、WSL2 kernel 6.18.33.2 |
| Docker resources | 24 CPU / 15.5 GiB |
| PostgreSQL preflight | cached `postgres:17-alpine`を一時起動し、`pg_isready`成功後に削除 |

数値性能baselineには、本表に加えてJVM引数、Docker image digest、workload、warm-up、反復、
raw resultおよび計測対象commitを記録する。別PCの値と直接比較しない。

### 3.3 変更前baseline

| 検証 | 結果 |
|---|---|
| Root `clean verify` | SUCCESS、15.981秒、Architecture Contract 4件、ArchUnit Rules 66件、failure / error / skip 0 |
| Feature Template | SUCCESS、positive、Tier別ArchUnit負例、Tier別NullAway負例、restore、runtime dependency tree |
| NullAway | SUCCESS、positive、期待failure、restore |
| PostgreSQL start | SUCCESS、`postgres:17-alpine`、2回目の`pg_isready`でaccepting connections |
| `git diff --check` / restore | SUCCESS、tracked差分なし |

Temurin環境ではCDS archiveのversion差warningと、それに伴うSurefireのnative stream warningが出るが、
test resultとprocess exitは成功した。Phase 1bの機能failureとは扱わず、JDK更新またはCIで再現した場合に
build foundationの環境課題として切り分ける。

証拠の詳細は`../architecture/validation/phase1b-cp0-start-baseline.md`を正本とする。

### 3.4 CP1完了

Spring Modulith 2.1.1のLevel 0 / runtime非依存回帰後、`koiki-starter-api`をrelease unitへ追加した。
細粒度fixtureと独立Customer-like Runtime Consumerを空の隔離Maven repository経由でbuildし、
Tier 1 `workitem` moduleのunit / architecture / startup test、runtime依存境界およびexecutable JARの
HTTP起動を確認した。CP1ではKOIKI独自Public Java APIを追加していない。

証拠の詳細は`../architecture/validation/phase1b-cp1-modulith-2.1.1-regression.md`および
`../architecture/validation/phase1b-cp1-runtime-artifact-consumer.md`を正本とする。

### 3.5 CP2完了

`koiki-starter-api`へSpring Boot標準のJackson 3／path API Versioning既定値と、Spring Framework標準の
Resilience annotation有効化を追加した。KOIKI独自Public Java APIは追加せず、実装型を`internal`へ閉じた。
細粒度fixtureでpositive、全体／Resilience単独back-off、Customer override、strict Jackson、retry回数と
対象外例外を確認した。独立Consumerでは`POST /api/1/work-items`からController→Use Caseへ到達し、
非対応version 400とversion欠落404を実serverの`RestTestClient`で確認した。

証拠の詳細は`../architecture/validation/phase1b-cp2-runtime-core.md`を正本とする。

### 3.6 CP3完了

Spring Framework標準の`ProblemDetail`、`ErrorResponse`、`ResponseEntityExceptionHandler`を使い、
Validation、異常JSON、直接発生した`JacksonException`、未処理例外およびSpring MVC例外をRFC 9457へ
統一した。KOIKI独自Public Java APIは追加せず、安定した`code`と拒否値を含まないValidation
`violations`をStarter内部から提供する。

細粒度`MockMvc` fixtureでpositive、KOIKI／Problem Details単独無効、Application-owned handlerへの
back-off、Jackson／未処理例外の情報非露出を確認した。独立Consumerでは実serverのworkitem endpointへ
Validation、異常JSON、version／path errorを送り、test-only処理例外を含む統一error contractを確認した。
Milestone A隔離scriptをCIへ接続し、Draft PR #24のcommit
`cdfdebe783d2bb6808c10916235e2ff6b8ddf436`に対する全4 checkの成功を確認した。

証拠の詳細は`../architecture/validation/phase1b-cp3-problem-details.md`を正本とする。

### 3.7 CP4 local検証完了

`koiki-starter-data`をpersistence-neutralなFramework leafとして追加し、KOIKI→CustomerのFlyway実行順、
owner別location／history、Customer baseline 0を提供した。Starterへproduction migration SQLやPublic Java
APIは追加していない。`koiki-testing`はSpring Boot `@ServiceConnection`とTestcontainersの標準利用に必要な
test dependency bundleとし、独自Java abstractionを設けていない。

Customer-like Consumerをin-memory repositoryからSpring Data JPA／PostgreSQL 17へ移行し、実HTTPから
Use Case→Repository→DBまでの永続化を確認した。Application Use Caseを`@Transactional`境界とし、Boot既定の
class-based proxy contractに合わせてUse Case class／methodをnon-finalとした。実DB save後のtest-only例外で
rollbackを確認し、Feature Templateも同じ起動failureを再発させない形へ更新した。

隔離scriptでlocation混在、Customer先行、後発KOIKI versionをpositive／negative／restoreとして再現し、
Starter細粒度19試験、Consumer 15試験、artifact／Public API／dependency／ownership境界を全て確認した。
remote CIはMilestone BのCP5〜CP7を完了したPRで接続する。

証拠の詳細は`../architecture/validation/phase1b-cp4-data-runtime.md`を正本とする。

### 3.8 CP5 local検証完了

`koiki-starter-observability`をFramework leafとして追加し、Spring Boot組込みLogstash JSONの低優先度既定、
検証済み`X-Request-ID`のMDC設定、Micrometer Context Propagationを使うSpring標準`TaskDecorator`を
提供した。伝播対象は`requestId`だけとし、Customer側MDCやTaskDecoratorを上書きしない。

Customer-like Consumerが`@EnableAsync`と業務Use Caseを所有し、実HTTP→Controller→`@Async` Use Caseの
構造化logで`timestamp`、service／environment、相関ID、業務key-valueおよびCustomer decoratorの共存を
確認した。pool size 1で同じthreadを再利用し、headerなしの次requestへ前の相関IDが漏洩しない負例も確認した。

隔離scriptでrelease unit 9 projects、Starter細粒度27試験、Consumer 17試験、artifact／Public API／
dependency境界およびCP4のPostgreSQL回帰を全て確認した。remote CIはMilestone BのCP7完了後に接続する。

証拠の詳細は`../architecture/validation/phase1b-cp5-observability.md`を正本とする。

### 3.9 CP6 local検証完了

Spring Boot標準ActuatorによるDB healthとprobe分類、`koiki-starter-data-jpa`による上書き可能な
OSIV false既定を追加した。Customer-like ConsumerでDB UP／DOWN／restore、readinessへのDB明示追加、
情報非露出、test-only Entity露出負例とApplication override riskを実証した。

証拠の詳細は`../architecture/validation/phase1b-cp6-health-osiv.md`を正本とする。

### 3.10 CP7 local検証完了

MyBatis Spring Boot Starter 4.1.0をBOMのdependency managementだけへ追加した。Starter、Mapper、
MyBatis業務module、`PersistenceModel.SEPARATED`は追加せず、Consumer runtime treeにも混入していない。

Customer-like ConsumerへTier 2 `workreview`を追加し、Tier 1 `workitem`が保存後に値だけのimmutable
`WorkItemCreated` recordをSpring標準`ApplicationEventPublisher`で同期発行する。受信listenerは
`adapter.inbound.event`からApplication Use Caseへ委譲し、送信moduleのUse Case、Domain Model、Repositoryを
直接参照しない。正常時は両moduleを同一transactionで保存し、受信側invariant違反では安全な422
Problem Detailsを返して両方をrollbackする。Tier 2 Entityは識別子同一性と`@Version`を持ち、proxy相当
subtypeに加えて実Hibernate未初期化proxyとの同一性、nullable versionによる新規`persist`、競合状態遷移での
楽観的lockを実DBで確認した。

Spring Modulithはtest scopeのLevel 0を維持した。runtime retentionを持つ`@NamedInterface`をproductionへ
付与せず、公式`ApplicationModuleDetectionStrategy`と`NamedInterfaces.builder`をtest scopeだけで使って
`domain.event`をNamed Interfaceとして検証した。隔離scriptは全回帰、成果物境界、runtime非依存、
versionless MyBatis probeの4.1.0解決、Testcontainers cleanupを確認した。

証拠の詳細は`../architecture/validation/phase1b-cp7-domain-event-mybatis.md`を正本とする。

### 3.11 Milestone B remote CI・merge完了

Draft PR #25の初回CIでは、独立`Milestone B Integration`とPublic API Compatibilityは成功したが、
通常`Verify`に残っていた歴史的なCP3 aggregateが、CP4で承認済みの`spring-data-jpa`を拒否した。
CP3の当時の禁止contractを弱めず、通常CIの重複呼出を外してCP2／CP3回帰をCP7 aggregateへ引き継いだ。

是正commit `2d3705581866009008297b7f6a5b2abe80178e58`に対するCI run `33239813239`で、
`Verify (ubuntu-24.04)`、`Milestone B Integration`、`Public API Compatibility`が全て成功した。
Java Runtime Compatibility run `33239813233`もJava 21 fixture buildとJava runtime検証が成功した。
Evidence commit `6f99f63b18fc40c43d1709f60abc4ebce3c0456e`に対するCI run `33240299498`と
Java Runtime Compatibility run `33240299494`も全checkが成功した。`Milestone B Integration`は
初回3分04秒、是正後2分55秒、Evidence反映後2分56秒の3回連続SUCCESSで、PostgreSQL Testcontainers、
DB DOWN／restore、全Consumer test、cleanupをremote runnerで確認した。Architecture Ownerはrequired check化を
`ACCEPTED`とし、main rulesetの4番目のrequired contextへ追加した。この時点のpre-merge判定では、
PR #25はDraftかつmerge state `CLEAN`であり、Milestone Bはmerge pendingであった。

remote Evidenceの詳細とrun URLは`../architecture/validation/phase1b-cp7-domain-event-mybatis.md`を正本とする。

[PR #25](https://github.com/zaziedlm/KOIKI-JAVAWEB/pull/25)はfinal head
`84703a892b84e4980d30473131ca388a7e6aa453`からmerge commit
`b3973e66134898765b95796c3622aaa68759b4fd`として、2026年8月29日16:39 JSTにmainへmergeされた。
main pushのCI run `33241356803`では`Verify (ubuntu-24.04)`、`Milestone B Integration`、
`Public API Compatibility`、Java Runtime Compatibility run `33241356811`ではfixture buildとJava 21／25 runtime検証が
すべて成功した。main ruleset `21140116`もactive／strictかつrequired checks 4件で一致している。

PR merge、main CI、rulesetおよびlocal mainのidentityを確認したため、Milestone Bを`COMPLETE / ACCEPTED`としてcloseする。
Milestone Cの`feature/phase1b-operations-closeout`はmerge commit `b3973e6`から分岐済みであり、
この時点ではCP8を`START READY / NOT STARTED`として開始し、その後Gate 8-1／8-2を経てGate 8-3を完了した。

### 3.12 CP8 local検証完了

同一Consumer executable JARを明示的なmaintenance modeだけnon-web processとして起動し、Spring Boot標準の
`ApplicationRunner`／exit code経路と、PostgreSQL session advisory lockを専用JDBC connectionで保持する
Customer-like単一実行contractを実装した。Framework artifact、Public Java API、production migration、
新規business moduleは追加していない。

細粒度testと実OS process acceptanceにより、同一task keyのwinner exit `0`／contender exit `10`と
DB副作用1回、異なるtask keyの独立性、process kill後のsession lock解放／retry、invalid input exit `64`、
non-web起動および`executionId`によるstructured log相関をPostgreSQL 17.11で確認した。CP1〜CP7回帰、
release unit 10 projects、Architecture Contract 4件、ArchUnit Rules 66件も成功した。

証拠の詳細は`../architecture/validation/phase1b-cp8-single-execution.md`を正本とする。

### 3.13 CP9開始preflight／計測設計

CP8完了commit `679b6f6a769e8f1208baaf7cc0b45d1f22668390`を開始基準とし、Tooling-ownedかつ非配布の
`build-support/performance-baseline`について計測contractを設計した。同一fixture binaryをbare Spring Boot／
KOIKI適用の2 assemblyから実行し、Phase 1bに存在するHTTP success／structured log、Validation／Problem Details、
JPA／PostgreSQL writeおよびstartupだけを同一環境でpaired measurementする案をOwnerが承認した。

fingerprint、raw、aggregateの3結果をversion付きschemaで分離し、cleanなharness commitから公式baselineを採取する。
性能数値、PC間比較または案件SLAはquality gateにせず、harness、3 result schema、最小negative、再集計とcleanupだけを
機械検証する。参考値をシンプルに比較できるようp50／p95へ限定し、Framework artifact、Public API、production設定、
migrationは変更しない。

設計の詳細は`../architecture/validation/phase1b-cp9-performance-baseline.md`を正本とする。

### 3.14 CP9 harness実装

Tooling-ownedの`build-support/performance-baseline`へshared fixture、bare／KOIKI assembly、外部process runner、
fingerprint／raw／aggregate schemaと一括verification scriptを実装した。Framework release unit、Public API、production設定、
migrationおよび既存Consumer production codeは変更していない。実行全般をやり直し、CP1〜CP8 aggregate回帰に続いて、
隔離repositoryでharness 5 module、runner unit test 5件、bare／KOIKI実process、startupと3 workload、DB／log件数、再集計、
3 positive／2 negative schemaを含むCP9 Smokeが成功した。全般レビューでclean preflight、response／sample acceptanceおよび
version付き`Location`を補強し、再実行にも成功した（run ID `8f4f4b45b7174516827d81ff7d463abc`、計測sampleのfailure 0件）。
Gate 9-2のlocal実process acceptanceを満たし、harness commit後のclean worktreeから公式baselineを採取できる状態とした。

### 3.15 CP9公式baseline採取完了

harness commit `dff9d8c0a1eb1b5e399e5dbf435534d4dec912b5`のclean worktreeから正式protocolを実行し、run ID
`4b236a7e99b74ca0bc1a542aa668d30a`を`build-support/performance-baseline/results/20260829-232318`へ記録した。
fingerprintは`gitDirty=false`、3 fork、startup 3 fork、warm-up 200、measurement 1,000、concurrency 1を示す。
raw resultは18,006 sample、failure 0件で、8 variant／workload系列と4 paired comparisonを生成した。

公式実行はCP1〜CP8 aggregate回帰、隔離release unit／harness build、dependency境界、status／response／DB／log、
sample件数、決定的再集計、3 positive／2 negative schemaおよびcleanupをすべて通過した。性能数値は同一PC内の参考値であり、
required閾値や案件SLAにはしない。以上によりDoD 1b-8とCP9を`LOCAL COMPLETE / OFFICIAL BASELINE RECORDED`とし、
Milestone CはCP10 Developer Journey／DoD／Gate 2 closeoutへ進む。

## 4. Scope

### 4.1 In scope

- Core Configuration
- Problem DetailsとValidation。`JacksonException`を含む統一例外形式
- Jackson 3標準設定。`JsonMapperBuilderCustomizer`を用い、module自動検出を無効化する
- Spring Framework標準Resilience。`@EnableResilientMethods`自動有効化、retry規約、timeout既定値
- Spring標準API Versioning設定
- 構造化log、相関ID、Micrometer Context Propagationを適用する`TaskDecorator`
- Actuator healthとDB状態
- PostgreSQL、KOIKI / CustomerのFlyway二階層、Testcontainers
- JPA OSIV無効化とEntity露出の検出
- Web process外から起動するcloud非依存の単一実行契約と、複数processでの二重起動防止
- MyBatis Spring Boot Starter 4.1.0のBOM管理だけ
- Domain Event規約とNamed Interface採否の検証
- Framework overheadの性能baseline harness、環境fingerprint、raw result schema
- `MockMvc`と`RestTestClient`のPhase 1b規約化
- 新設artifactのrelease unit、Public API inventory、japicmp、snapshot、CIへの統合
- 独立Maven buildのCustomer-like Runtime Consumerを通したStarter利用、業務module組立、起動・運用経路

### 4.2 Non-scope

- Spring Security、SecurityContext用`ThreadLocalAccessor`、認証・認可、Session、監査実装
- 正式Reference業務、Reference migration、MVC / HTMX画面、REST APIの業務受入
- Spring Modulith Level 1 / 2、`@ApplicationModuleListener`、非同期Domain Event、Event Publication
- MyBatis Starter、Mapper配置、`PersistenceModel.SEPARATED`、MyBatis業務module、DoD 31〜34
- Oracle、Flyway vendor分岐、Oracle Testcontainers
- ECS、Kubernetes、特定cloud schedulerのReference実装
- React SPA、外部I/O、Spring Batch、正式container / deployment reference
- Project Template、OpenRewrite、SBOM、正式releaseとsupport開始
- 将来用途だけを理由とする空module、空package、仮Starter、仮Public API

Customer-like Runtime Consumerは正式Reference Application、Customer成果物またはProject Templateではない。
業務語彙、fixture table、Controller、Use Case、Domain、Repositoryおよび設定をFramework artifactへ同梱せず、
Phase 1b検証専用のTooling成果物として保持する。

相関IDの伝播はPhase 1bで実装する。§20.1が伝播対象に挙げるSecurityContextはPhase 2のSecurity成果物であり、
Phase 1bでは後からAccessorを追加できる構造だけを維持する。このPhase解釈はGate 1のOwner Review対象とする。

## 5. 実行原則

1. Spring Boot / Spring Framework / Micrometer / Testcontainers等の標準機構を優先する。
2. Walking SkeletonのJava、SQL、POM、packageおよび一時座標をcopyしない。
3. leaf moduleは、同じcommitでproduction code、test、依存境界を成立させる場合だけ追加する。
4. Framework、Tooling、Reference、Customerのfixtureをpackageやartifact内で混在させない。
5. 細粒度fixtureとCustomer-like Consumerの二層で検証し、内部testだけの成功を利用可能性の証拠にしない。
6. Customer-like ConsumerはRoot Reactorへ含めず、Frameworkの`internal` package、reactor classpathまたは
   通常の開発用local Maven cacheへ暗黙依存させない。
7. 意図的failureはTooling所有のfixtureまたはtest sourceへ隔離し、production sourceへ置かない。
8. override可能な設定は、既定値、上書き方法、無効化時のriskおよびnegative testを持つ。
9. Public API候補は、型名を実装する前に利用者、安定性、Spring標準代替、inventory、japicmpをreviewする。
10. local検証を主経路とし、remote CIはmilestone PRへまとめる。
11. 性能値はrequired checkにせず、harnessとresult schemaの再現性をCIで検査する。
12. 実装証拠が設計前提を否定した場合は実装を止め、ADRまたはGrand Designへ戻る。

## 6. Gate 1 — Phase 1b開始承認

Gate 1は本文書の実行順序と判断候補を承認するGateである。承認前にPOM、module、production code、
Public API、migrationまたはworkflowを追加しない。個別Public APIの型シグネチャは、該当CPの実証結果を
基に別途reviewし、Gate 1承認だけで先行固定しない。

### 6.1 Baseline候補

| 対象 | 現行 | Gate 1候補 | 判定方法 |
|---|---:|---:|---|
| Spring Boot | 4.1.1 | 4.1.1維持 | 2026年8月28日の公式stableと一致。全Phase共通DoDを満たす |
| Spring Modulith | 2.1.0 | 2.1.1採用 | CP1前半でLevel 0、runtime dependency tree、Feature Templateを回帰し、すべてSUCCESS |
| MyBatis Spring Boot | 未管理 | 4.1.0 BOM候補 | dependency managementだけを追加し、runtime treeへ出現しないことを検査 |
| Flyway | Boot管理 | 12.4.0 | Boot BOM値を使用し、KOIKI独自versionを持たない |
| Jackson BOM | Boot管理 | 3.1.5 | Boot BOM値を使用する |
| Testcontainers | Boot管理 | 2.0.5 | Boot BOM値を使用する |
| PostgreSQL Driver | Boot管理 | 42.7.13 | Boot BOM値を使用する |

Modulith patchとMyBatis BOMは別commit pointで検証し、runtime成果物の追加と混在させない。

### 6.2 ArtifactとOwnership候補

次はGate 1で承認する論理境界であり、空moduleを一括生成する許可ではない。

| 候補 | Ownership | 最初に必要となるCP | 含める責務 | 含めない責務 |
|---|---|---:|---|---|
| `koiki-starters/koiki-starter-api` | Framework | CP1 / CP2 | core configuration、Jackson、Resilience、API Versioning、Problem Details、Validation | Security、業務Controller、data、observability |
| `koiki-starters/koiki-starter-observability` | Framework | CP5 / CP6 | structured logging既定、request correlation、TaskDecorator、Actuator基本health contract | SecurityContext、cloud backend、OpenTelemetry exporter固定、Customer固有HealthIndicator |
| `koiki-starters/koiki-starter-data-jpa` | Framework | CP6 | OSIV無効化、JPA profileの既定 | MyBatis、業務Entity、Customer migration |
| persistence-neutral Flyway leaf | Framework | CP4 | KOIKI migration実行順とCustomer Flyway共存の自動構成 | Reference migration、業務SQL、vendor分岐 |
| `koiki-testing` | Toolingとして配布 | CP4 | PostgreSQL Testcontainers支援、runtime integration test support | 本番auto configuration、業務fixture |
| `build-support/runtime-foundation-verification` | Tooling、非配布 | CP1 | auto-configurationの細粒度positive / negative / restore、dependency tree | 顧客利用可能性の単独証拠、release unit、Public API |
| `build-support/runtime-foundation-consumer` | Tooling、非配布・独立build | CP1〜CP10 | Customer-like実行アプリ、業務module、利用設定、外部観測可能な受入経路 | Framework内部実装、正式Reference、Project Template、配布artifact |
| `build-support/performance-baseline` | Tooling、非配布 | CP9 | workload runner、fingerprint、raw result / schema検査 | 性能保証値、PC間の直接比較 |

Flyway leafのartifact名と、単一実行契約を将来の`koiki-starter-batch`から分離する配置は未決定である。
`koiki-starter-data-jpa`へFlywayを混在させるとMyBatis / JdbcClient利用者が使用できず、
`koiki-starter-api`へ混在させるとWeb APIとdata ownershipが崩れる。このためCP4前に次を比較し、
Gate 1 decision recordで一案を承認する。

1. persistence-neutralな`koiki-starter-data` leafをPhase 1b成果物として追加する。
2. Framework libraryとauto-configuration leafを分離する。ただし未実証の`-api` / `-impl`分割を先行しない。
3. Spring Boot標準だけでKOIKI先行migrationを表現できるか再検証し、独自leafを不要にする。

推奨は1を第一候補とし、CP4の最小fixtureが必要性を実証した同じcommitでだけ追加する。

### 6.2.1 Customer-like Runtime Consumer

#### 位置づけ

Customer-like Runtime Consumerは、KOIKIをMaven成果物として利用する業務Webアプリの組立経路を
Phase 1bから継続的に実演する受入fixtureである。Framework内部testのための補助applicationではなく、
利用者側から観測できるStarter、設定、HTTP、DB、log、healthおよびmaintenance processを検証する。

```text
runtime-foundation-consumer
├── application assembly / configuration
├── Tier 1 business-like module
│   ├── adapter.inbound
│   ├── application
│   └── adapter.outbound
├── Tier 2 business-like module
│   ├── adapter.inbound
│   ├── application
│   ├── domain
│   └── adapter.outbound
├── Customer-owned migration fixture
└── architecture / integration / acceptance tests
```

Tier 1 / Tier 2の開始形はPhase 1aで正式化したFeature Templateから生成し、必要な責務だけを追加する。
生成済みsourceをFrameworkへcopyせず、Consumer自身が業務moduleを所有する。正式Referenceの`master`、
`expense`、`identity`等の仕様・語彙・受入条件は使用しない。

#### 業務アプリ構築上の判断

| 観点 | Phase 1b Consumerでの扱い |
|---|---|
| Ownership / module | Tooling-owned Consumerがbusiness-like moduleを所有。Frameworkは業務語彙を知らない |
| Tier | 単純調整をTier 1、状態・不変条件を持つ経路をTier 2として別moduleで実演する |
| Responsibility | Controllerは受付、Use Caseはtransactionと調整、Domainは不変条件、AdapterはDBを所有する |
| Persistence / model | PostgreSQL + JPAを既定とし、Tier 2は共有モデル方式。MyBatisは実装しない |
| Read model | CPで必要になる最小DTO / read modelだけをConsumer側に置く |
| Module collaboration | CP7で値だけの同期Domain Eventを実演し、直接Bean呼出と非同期eventを使用しない |
| View / API boundary | Entity / DomainをHTTPへ露出せず、DTOとProblem Detailsで境界を確認する |
| Verification | 独立build、artifact解決、起動、HTTP、DB、log、health、jobを外部観測する |

#### Artifact解決と独立性

1. ConsumerはRoot Reactorの`<module>`へ追加しない。
2. local CP検証は空の一時Maven repositoryへPhase 1b release unitをstageし、Consumerを別Maven invocationでbuildする。
3. Consumer POMはKOIKI BOM / Parent / Starterを通常のMaven coordinatesで参照し、source directoryやreactor classpathを参照しない。
4. milestoneではtimestamped snapshotまたはCI artifact repository経由の解決を実証し、通常local cacheだけの成功で完了しない。
5. Consumerから`org.koikifw.*.internal..`への参照、Framework test fixtureの同梱、Customer設定のStarter内蔵をnegative testで拒否する。

#### 利用者向けEvidence

各CPで、依存追加、必須設定、任意override、起動command、外部観測結果、failure時のdiagnosticを記録する。
CP10では、次を一連のDeveloper Journeyとして再実行できる状態にする。

```text
KOIKI BOM / Parent / Starterを選ぶ
  -> business moduleを配置する
    -> Customer migrationを追加する
      -> applicationを起動する
        -> HTTP requestを受ける
          -> Use Case / Domain / Repositoryを通る
            -> Problem Details / log / healthを確認する
              -> maintenance processをWeb外から起動する
```

#### 後続Phaseへの接続

| Phase | Customer-like Consumerから先に追加される姿 |
|---|---|
| Phase 1b | HTTP、error、DB、migration、log、health、単一実行を持つ業務アプリの器 |
| Phase 2 | 認証・認可、Session、監査、identity、Oracle smoke |
| Phase 3 | 正式Referenceの`master` / `expense`、MVC / HTMX、REST、業務受入 |
| Phase 4 | SPA、notification、accounting、MyBatis、外部I/O、Batch、Level 2 |
| Phase 5 | Project Template、deployment reference、正式release、support |

ConsumerはPhase 2以降の代替ではない。Phase 1b終了時に正式Referenceへ改名、移動または昇格せず、
後続Phaseが同じ利用経路を正式成果物で実証できるかを比較するTooling evidenceとして維持する。

### 6.3 Release unitとPublic API候補

1. Phase 1bで配布するleaf artifactは既存の`0.1.0-SNAPSHOT` release unitへ加える。
2. C1 timestamped snapshotはPhase 1a immutable baselineとして変更しない。
3. 既存Architecture Contract / ArchUnit RulesのPublic APIはC1 baselineとのjapicmpを継続する。
4. 新規artifactには比較元がないため、最初にinventoryを作り、Gate 2承認後のtimestamped snapshotを
   そのartifactの初回japicmp baseline候補とする。
5. auto-configuration実装、condition、internal property bindingは原則`internal`とし、利用者が直接実装する
   SPIまたは設定契約だけをPublic API候補とする。
6. milestone中のsnapshot公開は、Repository外ConsumerまたはCIで配布形態の実証が必要になった時点で
   Owner Reviewする。CP0だけを理由に公開しない。
7. Customer-like Consumerのlocal検証は空の隔離repositoryへstageした成果物を使用し、milestoneでは
   remoteで取得可能なsnapshot経路を少なくとも1回実証する。

### 6.4 Flyway scope候補

- Phase 1b DoDはKOIKI Framework / Customerの二階層とする。Referenceを含む三階層へ一般化しない。
- CustomerはSpring Boot標準Flywayを所有し、Customer専用locationとhistory tableを使用する。
- KOIKI側は同じDataSource上でCustomerより先に実行し、専用locationと`koiki_flyway_history`を使用する。
- 正式なFramework tableがまだ不要な場合、検証SQLはTooling fixtureのtest resourcesへ置き、架空の
  production migrationを作らない。
- version衝突、owner location混在、順序逆転、non-empty schema、後発KOIKI versionをnegative / restoreで検証する。
- `baseline-on-migrate`等のWalking Skeleton設定を無条件に引き継がず、現在のFlywayで再実証する。

### 6.5 Named Interface / Domain Event候補

- Phase 1bではSpring Modulith Level 0を回帰し、Level 1 / 2へ進めない。
- Named Interfaceは、`domain.event`だけを公開する境界をtest scopeで検証でき、顧客production runtimeへ
  Modulith依存を追加せず、KOIKI独自Public APIを増やさない場合だけ採用候補とする。
- annotationのcompile/runtime依存または業務module実装が必要なら採用をPhase 3へ延期し、ArchUnitを主経路とする。
- Domain Event規約は値だけのimmutable `record`、`domain.model`非参照、同期既定、外部I/O禁止を維持する。
- 非同期listener、publication、retry、purgeはPhase 4へ残す。

### 6.6 `MockMvc` / `RestTestClient`規約候補

- `MockMvc`: Controller advice、validation、serialization等のWeb sliceと、server processを必要としない内部MVC検証。
- `RestTestClient`: 実serverを起動するHTTP contract、Problem Details、Actuatorおよび将来のREST受入。
- `WebTestClient`: Spring MVCおよびlive serverのテストにも利用できるが、`WebClient` / Reactorベースの
  non-blocking test APIである。KOIKIはServlet / Spring MVCのblocking stackを標準とするため採用せず、
  標準HTTP test clientには`RestTestClient`を使用する。WebFluxまたはstreaming経路を採用する個別案件での
  選択を禁止するものではない。
- Phase 1bではFramework-ownedな細粒度fixtureで規約そのものを実証し、Reference業務REST APIは作らない。
- 同じHTTP contractをCustomer-like Consumerから実行し、ControllerからRepositoryを直接呼ばず、
  Domain / Entityをresponseへ露出しないことを確認する。

### 6.7 単一実行契約候補

Gate 1ではJava型を固定せず、次の外部観測可能なcontractを承認対象とする。

1. 定期処理はWeb instance内の無条件な`@Scheduled`で起動しない。
2. dedicated non-web processを外部schedulerから起動できる。
3. 同じtask keyを持つ2以上のprocessが競合しても、業務処理へ進むのは1 processだけである。
4. 異常終了後に永久lockとならず、安全に再実行できる。
5. 開始、獲得、競合skip、成功、失敗を相関可能な結果とexit statusで記録する。
6. ECS / Kubernetes固有APIをFramework contractへ含めない。

PostgreSQL advisory lock、lease付きlock table、外部schedulerの単一起動保証を候補比較する。
外部scheduler保証だけには依存せず、複数process / DB競合testで二重起動防止を実証する。
Java Public APIが必要と判明した場合はCP8の実装前に型シグネチャを別途Owner Reviewする。

### 6.8 性能harness候補

- bare Spring Boot fixtureとCustomer-like ConsumerへKOIKI runtimeを適用した場合の差分を同一環境で計測する。
- Phase 1bで存在する主要経路だけを測り、Security、監査等の未実装経路へ架空値を入れない。
- workload、warm-up、反復、fork、JDK、OS、CPU、memory、Docker、DB image digest、commitを記録する。
- raw resultと集計resultを分離し、schema validationと再実行可能性をCIで検査する。
- PC固有の閾値や平均値をrequired checkにしない。退行判定値は複数環境の証拠が得られてから判断する。

### 6.9 CI候補

- A / B / Cの各PRでRoot `clean verify`、Feature Template、NullAway、Public API compatibilityを維持する。
- PostgreSQL TestcontainersはMilestone Bで通常CIへ追加し、3回連続成功による安定性と時間の確認後、
  `Milestone B Integration`をmain rulesetのrequired checkとして`ACCEPTED`にした。
- Java 21 build / Java 25 runtimeの既存matrixを維持し、runtime artifact追加時は同一JAR対象を拡張する。
- performance数値はrequiredにせず、harness build、result schema、fingerprint欠落のnegative testを通常CIへ入れる。
- remote CIはmilestone単位とし、各CPの通常経路は対象moduleの`-pl ... -am verify`で検証する。
- 各milestoneでConsumerの独立buildと起動smokeを実行し、Milestone B以降はHTTP / PostgreSQL acceptanceを含める。
- Root Reactor成功とConsumer成功を別結果として表示し、どちらか一方だけの成功をmilestone完了にしない。

### 6.10 Gate 1 decision record

| # | 判断 | 推奨案 | 状態 |
|---:|---|---|---|
| 1 | 8 DoDと全Phase共通DoDのtrace | §8のmatrixを採用 | ACCEPTED |
| 2 | version baseline | Boot 4.1.1維持、Modulith 2.1.1採用、MyBatis 4.1.0 BOMのみ | ACCEPTED |
| 3 | runtime artifact / Starter ownership | §6.2のleaf単位。空aggregatorを作らない | ACCEPTED |
| 4 | Flyway ownership / scope | KOIKI / Customer二階層、persistence-neutral leafを第一候補 | ACCEPTED |
| 5 | release unit / Public API / japicmp / snapshot | §6.3の既存baseline維持と新規artifact初回baseline方式 | ACCEPTED |
| 6 | Named Interface | test scope / runtime依存なしの場合だけ採用、満たせなければPhase 3へ延期 | ACCEPTED |
| 7 | 単一実行 | §6.7のcloud非依存contract、複数process / DB競合必須 | ACCEPTED |
| 8 | 性能harness | 同一環境差分、fingerprint、raw result、数値非required | ACCEPTED |
| 9 | branch / CI | 最大3 PR、local中心、Testcontainersは安定性確認後にrequired判断 | ACCEPTED |
| 10 | deferred scope | Security、Reference、Level 2、Oracle、cloud固有、正式releaseを除外 | ACCEPTED |
| 11 | Phase補足 | SecurityContext伝播はPhase 2、MockMvc / RestTestClient規約はPhase 1b | ACCEPTED |
| 12 | Customer-like Consumer | CP1〜CP10で同じ独立Consumerを育て、Starter利用からHTTP / DB / 運用まで実演 | ACCEPTED |

**Gate 1 Decision:** ACCEPTED<br>
**Decided by:** Shuichi Kataoka<br>
**Decision date:** 2026年8月28日<br>
**ADR action:** 実装証拠が既存ADRを変更する場合だけ追加または改訂する。

## 7. Milestoneとcommit point

| Milestone | Branch | CP | 内容 | Gate / commit条件 |
|---|---|---:|---|---|
| A Runtime Core | `feature/phase1b-runtime-core` | CP0 | 本計画、DoD trace、baseline、Ownership、Gate 1 | 本文書review。production差分なし |
| | | CP1 | Modulith 2.1.1回帰、最初のruntime leaf、細粒度fixture、独立Consumer骨格 | ConsumerがBOM / Parent / Starterをartifact経由で解決し起動 |
| | | CP2 | Core Configuration、Jackson 3、Resilience、API Versioning | Consumerのversion付きHTTP→Controller→Use Case経路、override / retry負例 |
| | | CP3 | Problem Details、Validation、`JacksonException` | Consumerの入力不正・業務相当例外・JSON異常が統一errorになる |
| B Data & Runtime Integration | `feature/phase1b-data-runtime-integration` | CP4 | PostgreSQL Testcontainers、Flyway二階層 | ConsumerのUse Case→Repository→DB、KOIKI / Customer migration共存 |
| | | CP5 | 構造化log、相関ID、`@Async`伝播 | ConsumerのHTTP→`@Async`で相関ID維持、thread再利用漏えい負例 |
| | | CP6 | Actuator DB health、OSIV無効化 | ConsumerのDB up / down health、Entity / Domain露出負例 |
| | | CP7 | Named Interface / Domain Event判断、MyBatis BOMのみ | ConsumerのTier 1 / 2間同期event、直接Bean参照なし、Level 0回帰 |
| C Operations & Closeout | `feature/phase1b-operations-closeout` | CP8 | 単一実行contractと複数process排他 | Consumer同一成果物をWeb外起動、2 process競合、crash / retry |
| | | CP9 | 性能baseline harness | Customer-like経路のKOIKI有無差分、fingerprint、raw result、schema負例 |
| | | CP10 | Developer Journey、DoD、ADR、Skills、release unit、最終CI、Gate 2 | remote artifactからConsumerをbuild・起動し、全経路とrequired checksを再実行 |

各Milestoneは最新mainから分岐する。CPはreview可能な論理境界であり、必ず1 CP = 1 commitとはしない。

### 7.1 Customer-like Application acceptance

| CP | 利用者操作 | Consumerで観測する結果 |
|---:|---|---|
| 1 | BOM / Parent / StarterをMaven依存として追加する | 独立buildとSpring Boot起動、KOIKI internal参照なし |
| 2 | version付きHTTP requestを送る | Controller→Use Case、Jackson / Resilience / versioning適用 |
| 3 | 不正入力・異常JSON・処理例外を送る | 一貫したProblem Details、内部情報非露出 |
| 4 | Customer migrationと永続化処理を追加する | KOIKI / Customer migration独立、Use Case→Repository→PostgreSQL |
| 5 | HTTP起点でasync処理を呼ぶ | request / async logに同じ相関ID、thread reuse後の漏えいなし |
| 6 | health確認とEntity露出負例を実行する | DB状態反映、OSIV false、Web境界違反を検出 |
| 7 | module間の成立条件を同期eventで連携する | 値eventだけを公開し、他moduleのUse Case / Domain / Repositoryを直接参照しない |
| 8 | maintenance taskを専用processから同時起動する | Web外起動、複数processでも1回だけ実行、異常終了後に再実行可能 |
| 9 | 同じapplication pathを反復計測する | 環境fingerprint付きraw / aggregate result |
| 10 | 空環境で依存取得から全経路を再実行する | Developer Journeyが文書どおり再現し、後続Phase追加点が明確 |

## 8. DoD Traceability

| DoD / 成果物 | 主CP | 必須Evidence |
|---|---:|---|
| 全Phase共通1: 最新Boot minor | CP0 / CP10 | 公式stable、BOM、effective POM、dependency tree |
| 全Phase共通2: ADR / Owner approval | CP0〜CP10 | decision record、既存ADRとの整合、Gate 1 / 2 |
| 全Phase共通3: CI quality gates | 各Milestone / CP10 | local結果、PR required checks、main最終CI |
| 全Phase共通4: Agent Skills | CP10 | 規約差分、Skill更新要否、Skill利用検証 |
| 全Phase共通5: table / Flyway ownership | CP4 / CP10 | owner別location / history、production table一覧 |
| Core Configuration | CP1 / CP2 | condition、override、absence時挙動、runtime dependency tree |
| Jackson 3 | CP2 / CP3 | `JsonMapperBuilderCustomizer`、module自動検出無効、serialization負例 |
| Resilience | CP2 | `@EnableResilientMethods`、retry回数、timeout、fail-silent負例 |
| API Versioning | CP2 | Spring標準path versioning、未指定 / 非対応version負例 |
| Problem Details / Validation | CP3 | 未処理例外、`JacksonException`、validation、情報漏えい負例 |
| 1b-1 Flyway二階層 | CP4 | KOIKI / Customer独立version、履歴、順序、衝突負例 |
| 1b-2 統一error | CP3 | error schema、`JacksonException`、positive / negative / restore |
| 1b-3 log / correlation | CP5 | JSON項目、`@Async`伝播、thread再利用漏えい負例 |
| 1b-4 PostgreSQL CI | CP4 / Milestone B | Testcontainers起動、image固定、CI時間と安定性 |
| 1b-5 health | CP6 | endpoint、DB up / down、情報露出範囲 |
| 1b-6 OSIV | CP6 | default false、Entity露出failure、override risk |
| 1b-7 単一実行 | CP8 | dedicated process、2 process競合、二重実行なし、crash回復 |
| 1b-8 性能baseline | CP9 | harness、fingerprint、raw result、集計、再実行 |
| MyBatis BOM管理 | CP7 | BOM 4.1.0、runtime dependencyなし、Starter / Mapperなし |
| Domain Event / Named Interface | CP7 | 規約matrix、Level 0、Named Interface採否と依存根拠 |
| MockMvc / RestTestClient | CP3 / CP6 | fixture別の使い分け、WebTestClient非依存 |
| Customer-like利用可能性 | CP1〜CP10 | 独立artifact解決、business module、HTTP→Use Case→Domain→DB、log / health / job |

## 9. 検証と計測記録

各CPのvalidation記録に次を残す。

| 項目 | 内容 |
|---|---|
| Source | branch、commit候補、dirty状態、main merge base |
| Scope | artifact、production / fixture ownership、Public API差分 |
| Implementation | 実装開始・終了、Owner確認時間 |
| Local verification | command、wall time、test数、result、期待failure |
| CI wait | pushからrequired checks完了まで |
| Rework | 回数、原因、修正時間 |
| Deferred | 実装しなかった後続判断 |

通常は対象moduleの`-pl ... -am verify`とpositive / negative / restoreを使う。Milestone PR候補ではRoot、
Feature Template、NullAway、Public API compatibility、Java runtime compatibilityの該当範囲をまとめて実行する。

## 10. 見積もりと再校正

Feasibilityの開始rangeは直接69〜114標準人日、contingency込み86〜143標準人日、
AI支援Owner稼働43〜93日である。Phase 1aは全WPを一貫した同一基準で計測していないため、
過去の稼働日を推測して係数を短縮しない。

Phase 1aで得た定性的実績は、localのpositive / negative / restoreを主経路とし、remote CIをmilestoneへ
まとめる方針へ反映済みである。Phase 1bではCP0から§9の同一項目を計測し、A / B / Cの終了時に、
残range、CI時間、container安定性および手戻り原因を再校正する。

## 11. Stop conditions

- Gate 1がPENDINGのままPOM、module、production code、Public API、migrationまたはworkflowを追加する。
- 空の`koiki-framework`、`koiki-starters`、`koiki-testing`を一括生成する。
- Spring標準の設定フックがあるのにKOIKI wrapperを追加する。
- Starter ownershipまたは最初の利用fixtureを決めずにleaf moduleを追加する。
- 細粒度fixtureだけを追加し、同じCPでCustomer-like Consumerから利用できる経路を示さない。
- ConsumerをRoot Reactorへ追加する、reactor classpath / Framework internal型へ依存させる、または通常local cacheだけで成功させる。
- Consumerの業務語彙、Controller、Use Case、Domain、RepositoryまたはCustomer migrationをFramework artifactへ移す。
- Consumerを正式Reference、Customer成果物またはProject Templateとして扱う。
- Public APIを追加するがinventory、japicmp、snapshot方針がない。
- Flyway二階層をReference込み三階層またはvendor分岐へ拡張する。
- MyBatis BOM管理を超えてStarter、Mapper、`SEPARATED`または業務moduleを実装する。
- Named InterfaceのためにLevel 1 / 2または未承認runtime依存が必要になる。
- SecurityContext、Spring Security、Reference業務、REST受入または非同期Domain Eventが必要になる。
- 単一実行contractが特定cloud APIへ依存する、または単一process内lockだけで実証する。
- 性能値にfingerprint、warm-up、反復、raw resultがなく比較不能である。
- Testcontainers / CIが不安定でDoD 1b-4の再現条件を満たさない。
- 実装証拠がGrand DesignまたはADRの前提を否定する。

## 12. Gate 2 — Phase 1b完了承認

次をまとめて1回判定する。

- DoD 1b-1〜1b-8と全Phase共通DoD
- Phase 1b成果物一覧とdeferred scope
- ADR追加・改訂要否とOwner Review
- Agent Skills追加・更新要否
- release unit、Public API inventory、japicmp baseline、timestamped snapshot
- Customer-like Consumerの依存取得、build、起動、HTTP / DB / log / health / job Developer Journey
- local最終検証、最終PR required checks、merge後main CI

最終PR required checks成功時点で`ACCEPTED — MAIN CI PENDING`まで判定し、merge後main CI成功で
`COMPLETE / ACCEPTED`とする。不一致または失敗時だけ再openする。

## 13. Owner Review観点

- §8が§27.2と§27.5の成果物・DoDを削らずtraceしているか。
- artifact候補がFramework / Tooling / Customer / Referenceを混在させていないか。
- Customer-like Consumerが業務アプリ構築の姿を示しつつ、正式ReferenceやProject Templateを先行実装していないか。
- 各CPが細粒度fixtureとConsumer acceptanceの二層を持ち、内部testだけで完了しないか。
- Consumerが独立Maven artifact利用、package by feature、Controller→Use Case→Domain→Repository境界を実証できるか。
- persistence-neutral Flyway leafの推奨と追加タイミングは妥当か。
- SecurityContext伝播をPhase 2へ残すPhase解釈は妥当か。
- Public APIをGate 1だけで固定せず、実証後に型単位reviewする境界は妥当か。
- Named Interface、単一実行、性能harnessの採否条件が後続Phaseを先行実装しないか。
- 最大3 PR、local中心、commit point単位の計測は妥当か。
- Stop conditionsがscope拡大時に実装を停止できるか。

## 14. 参照

- `phase1b-runtime-foundation-start-handoff-20260827.md`
- `../architecture/grand-design/KOIKI-JavaWeb-FW_グランドデザイン_v0.2.md` §7.1、§20.1、§21.1、§21.5〜21.6、§22、§27.2、§27.5
- `../architecture/KOIKI-JavaWeb-FW_Repository_Architecture_v0.1.md`
- `../architecture/KOIKI-JavaWeb-FW_Phase_Estimate_Feasibility_v0.1.md` §4
- `../architecture/adr/README.md`
- `../architecture/validation/phase1a-closeout.md`
- `../architecture/validation/phase1b-prep-mybatis-metadata-guard.md`
- `../architecture/validation/phase1b-cp0-start-baseline.md`
- `../architecture/validation/phase1b-cp1-modulith-2.1.1-regression.md`
- `../architecture/validation/phase1b-cp1-runtime-artifact-consumer.md`
- `../architecture/validation/phase1b-cp2-runtime-core.md`
- `../architecture/validation/phase1b-cp8-single-execution.md`
- `../architecture/validation/phase1b-cp9-performance-baseline.md`
