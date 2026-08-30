# Phase 1b CP8開始引継ぎ — 2026-08-29

## 1. この文書の位置づけ

この文書は、Milestone Bを`COMPLETE / ACCEPTED`としてcloseした時点で作業を区切り、新規AI対話セッションから
Phase 1b Milestone CのCP8を安全に開始するための運用引継ぎである。CP8の設計判断や実装証拠の正本ではない。
CP8で得た証拠は、次回`docs/architecture/validation/phase1b-cp8-single-execution.md`を作成して記録する。

判断が競合する場合は、次の順に正本と実効構成を確認する。

1. Repository rootの`AGENTS.md`
2. `docs/agent/skills/koiki-project-overview/SKILL.md`
3. `docs/agent/skills/koiki-business-feature-work/SKILL.md`
4. `docs/development/KOIKI-JavaWeb-FW_Phase1b実行計画_v0.1.md`
5. `docs/architecture/grand-design/KOIKI-JavaWeb-FW_グランドデザイン_v0.2.md` §19.2、§27.5
6. `docs/architecture/validation/phase1b-cp7-domain-event-mybatis.md`とCP4〜CP6のvalidation
7. `docs/architecture/adr/`
8. 実効構成である各`pom.xml`、source、test、Maven Wrapper、検証script

CP8はFrameworkの単一実行境界候補とTooling-ownedなCustomer-like Consumerのmaintenance taskを扱う。
開始時は`koiki-project-overview`を使用し、Consumerのmodule、Use Case、inbound commandまたはoutbound DB adapterを
設計・変更するときは`koiki-business-feature-work`も使用する。

## 2. 引継ぎ時点のGit・remote状態

| 項目 | 状態 |
|---|---|
| branch | `feature/phase1b-operations-closeout` |
| main / merge base | `b3973e66134898765b95796c3622aaa68759b4fd`（PR #25 merge commit） |
| Milestone B closeout commit | `828a6a7` `docs(runtime): close Milestone B after main CI` |
| worktree | closeout commit直後はclean |
| upstream | 引継ぎ作成時点では未設定。引継ぎcommit後に初回pushする |
| Milestone C PR / remote CI | 未作成／未接続。CP8単独では接続せず、CP10完了後にMilestone Cとして接続する |
| Java / Maven | Temurin 21.0.12.1、Maven Wrapper 3.9.16を確認済み |

PR #25のfinal headは`84703a892b84e4980d30473131ca388a7e6aa453`、merge commitは`b3973e6`である。
main CI runs `33241356803` / `33241356811`は全5 jobsがSUCCESSで、main ruleset `21140116`はactive／strict、
required contexts 4件を維持している。詳細はCP7 validation §5.2を正本とする。

この引継ぎ文と`docs/development/README.md`の索引更新をdocs-only commitとし、その後次でpushする。

```powershell
git push -u origin feature/phase1b-operations-closeout
```

## 3. Phase / Ownership / 対象

| 項目 | 内容 |
|---|---|
| Phase / status | Phase 1b Milestone C / Milestone B COMPLETE・ACCEPTED / CP8 START READY・NOT STARTED |
| Framework ownership候補 | cloud非依存の単一実行contractと、案件ごとに上書き可能な最小の実行境界。採用artifactは未決定 |
| Tooling ownership | 細粒度fixture、Customer-like Consumer、dedicated process、2 process競合、crash／retry、隔離script |
| Customer ownership | maintenance taskの業務意味、実処理、Customer table／migration、外部scheduler設定 |
| 主対象候補 | 既存Starter、最小の新規leaf、またはConsumer直接構成を比較してから決定 |
| 受入対象 | `build-support/runtime-foundation-consumer`の同一build／実行artifact |
| Public API | 原則追加しない。必要性が判明した場合は実装前に型シグネチャをOwner Reviewする |
| Deferred | Spring Batch、実業務batch、session cleanup、Event Publication purge、cloud scheduler adapter、Security |

単一実行contractを将来の`koiki-starter-batch`から分離する配置は未決定である。CP8開始だけを理由に
空module、独自annotation、汎用Job API、cloud固有adapterまたは将来用Starterを先行生成しない。

## 4. ここまでの確定baseline

### 4.1 Milestone A — Runtime Core

- `koiki-starter-api`をrelease unitへ追加し、Core Configuration、Jackson 3、Spring標準Resilience、
  API Versioning、Problem Details／ValidationをCustomer-like Consumerから検証済み。
- ConsumerはRoot Reactor外で独立buildし、Framework internal型を参照しない。
- Spring Modulithはtest scopeのLevel 0、runtime非依存を維持する。

### 4.2 Milestone B — Data & Runtime Integration

- `koiki-starter-data`でKOIKI／Customer二階層Flyway、PostgreSQL 17 Testcontainers、transactionを検証済み。
- `koiki-starter-observability`で構造化log、`X-Request-ID`、`@Async`相関ID伝播とthread再利用時の漏えい防止を検証済み。
- Spring Boot標準ActuatorによるDB UP／DOWN／restore、readiness／liveness分類を検証済み。
- `koiki-starter-data-jpa`で上書き可能なOSIV false既定と、Entity／DomainのWeb露出負例を検証済み。
- Tier 1／2 module間の値だけの同期Domain Event、rollback、楽観的lock、Modulith Level 0を検証済み。
- MyBatisはBOM 4.1.0管理だけで、Starter、Mapper、runtime dependencyを追加していない。
- `Milestone B Integration`はmain rulesetのrequired checkとして`ACCEPTED`済み。

### 4.3 現在の実効構成

- 正式release unitはRootの10 projectsで、Runtime Consumerと検証moduleはRoot Reactor外のToolingである。
- Framework runtime leafは`koiki-starter-api`、`koiki-starter-data`、`koiki-starter-data-jpa`、
  `koiki-starter-observability`である。
- Runtime Consumerは`workitem`、`workreview`、`application`の3 moduleである。
- 最新の隔離検証は
  `build-support/runtime-foundation-verification/verify-cp7-domain-event-mybatis.ps1`である。
- CP8はこのbaselineを壊さず、DoD 1b-7だけを独立して成立させる。

## 5. CP8の目的と完了条件

グランドデザイン §19.2、§27.5のDoD 1b-7とPhase 1b実行計画 §6.7に基づき、少なくとも次を満たす。

1. maintenance taskをWeb instance内の無条件な`@Scheduled`では起動しない。
2. 同じConsumerの成果物をdedicated non-web processとして外部から起動できる。
3. 同じtask keyを持つ2以上の実OS processを同一PostgreSQLへ競合させ、実処理へ進むprocessが1つだけになる。
4. 非獲得processが競合を明示して安全に終了し、業務副作用を起こさない。
5. lock保持processの異常終了後、永久lockを残さず別processが安全に再実行できる。
6. 開始、lock獲得、競合skip、成功、失敗を、相関可能な実行identity、構造化log、結果、exit statusで観測できる。
7. ECS、Kubernetes等のcloud APIや外部schedulerの単一起動保証だけへ依存しない。
8. Consumerの業務語彙、task実処理、Customer migrationをFramework artifactへ移さない。
9. Public API、production table、runtime dependencyおよびrelease unitの差分を記録する。
10. CP1〜CP7回帰、隔離Maven repository、PostgreSQL cleanupを含む一括検証を成功させる。

## 6. 実装前に確定する設計判断

### 6.1 Contractとartifact配置

次を比較し、Customer-like Consumerの最小fixtureが必要性を示した案だけを採用する。

1. Spring Boot／Spring Framework／JDBCの標準構成をConsumerが直接利用し、Framework artifactを増やさない。
2. 既存Framework leafへ、業務語彙を含まない条件付きの最小構成だけを追加する。
3. 単一実行専用leafを追加する。ただし最初のConsumer利用、Ownership、back-off、依存境界を同じ差分で実証する。

将来のSpring Batch責務、Job／Stepモデル、実行履歴、skip／retry規約をCP8 contractへ混在させない。
新規leafやPublic Java APIを第一案とせず、Spring標準とConsumer直接構成でDoDを満たせるかを先に確認する。

### 6.2 Dedicated processの起動contract

Spring Boot 4.1.1の公式仕様と実効依存を確認し、`WebApplicationType.NONE`、`ApplicationRunner`／
`CommandLineRunner`、application argument、profile、`SpringApplication.exit`等の候補を比較する。

- Web serverを起動しないことをprocess外から確認する。
- task keyと必要な入力を明示し、Web endpointをmaintenance起動口にしない。
- processの終了がtask結果と一致し、成功後にJVMが残留しない。
- launcherは受付、入力変換、exit整形に限定し、task判断やDB排他を直接実装しない。
- 実処理はApplication Use Case、排他取得はoutbound port／adapter候補として責務を分離する。

### 6.3 ConsumerのmoduleとTier

maintenance taskの主語と所有moduleを最初に決める。`application` module直下へ技術レイヤー横断の
runner、service、repositoryをまとめない。新しいfeature moduleが必要な場合も、最小taskが必要とした同じ差分でだけ追加する。

- 単純な1 task、状態遷移なし、処理調整中心ならTier 1を開始点とする。
- taskの状態、再実行規則または複数Entity不変条件が必要になった場合だけTier 2昇格を再評価する。
- 実証用taskはTooling-ownedなCustomer-like業務とし、実在するsession cleanup、publication purge、業務batchを先行実装しない。
- lock技術をDomain Modelへ入れず、Applicationの実行調整とoutbound adapterへ分離する。

### 6.4 排他方式

次の候補を、公式PostgreSQL仕様、接続pool lifecycle、crash recovery、migration ownership、観測可能性で比較する。

| 候補 | 必ず確認する点 |
|---|---|
| PostgreSQL advisory lock | session／transaction scope、poolへconnectionを返した場合のlock寿命、安定したkey生成と衝突、process kill時の解放 |
| lease付きlock table | owner token、期限、DB時刻、renewal、fencing、期限切れ回収、KOIKI／Customer migrationのowner |
| 外部scheduler保証 | 起動元として利用できるが、CP8の複数process／DB競合Evidenceを置き換えない |

単一process内のJava lock、singleton Bean、in-memory flagだけではDoDを満たさない。長時間task全体を不要な
単一DB transactionへ閉じ込める案や、connection poolへ戻したsessionがlockを保持し得る案も、実証なしに採用しない。

### 6.5 結果・exit status・log contract

実装前に少なくとも次をOwnerへ提示する。

- task key、execution ID、lock owner identityの生成・公開範囲
- acquired／contended／succeeded／failedの結果分類
- 正常成功、競合skip、入力不正、実処理失敗それぞれのexit status
- 既存構造化logとの統合方法と、HTTP `requestId`をmaintenance processへ誤用しない相関方式
- credential、JDBC URL、lock内部値、stack trace等を通常logへ露出しない境界
- crash時に記録が途中状態となる場合の診断方法

競合skipを成功扱いにするか区別可能な非0とするかは運用contractであり、実装都合で決めない。

### 6.6 Table、migration、Public API

- advisory lockだけで成立する場合はlock tableを先行追加しない。
- lease tableが必要ならFramework infrastructure tableかCustomer tableかを決め、CP4の二階層Flyway ownershipを守る。
- Consumerの実処理結果を確認するCustomer table／migrationをFramework側へ置かない。
- Java Public APIが必要なら、型、package、signature、利用者、代替不能性、japicmp baselineを実装前にOwner Reviewする。
- Public APIなしで成立する場合も、internal class、auto-configuration metadata、artifact inventoryを検証する。

## 7. 検証案

### 7.1 細粒度fixture

- auto-configurationまたはlauncher条件のpositive／negative／override／back-off
- Web applicationへ無条件にrunnerやschedulerが混入しないこと
- DB／JDBC classpath absence時の挙動
- task key／入力validation、結果分類、exit status
- lock取得、競合、解放、実処理失敗、再実行
- Public API、artifact、runtime dependency、production migration inventory

### 7.2 Customer-like Consumer acceptance

- packageした同一成果物をnon-web modeで起動し、Web portをlistenしないことを確認する。
- 同一PostgreSQL、同一task keyへ2 processを同期して競合させる。
- DB上の外部観測可能な副作用が1回だけであることを確認する。logだけを成功証拠にしない。
- 獲得processと非獲得processの結果、exit status、構造化logを別々に確認する。
- 異なるtask keyは不必要に相互排他しないことを確認する。
- lock保持中のprocessをtest harnessから終了し、後続processが取得・完了できることを確認する。
- test用待機や失敗制御をproduction endpoint／通常業務sourceへ追加しない。

threadsだけの並行test、mock DBまたは同一ApplicationContext内のBean競合だけでは、複数instance Evidenceとしない。

### 7.3 隔離一括検証

CP8用scriptは`build-support/runtime-foundation-verification/verify-cp8-single-execution.ps1`を候補とする。
空のMaven repositoryへ正式release unitをstageし、細粒度fixture、Consumer独立build、packageしたprocessの競合、
crash／retry、CP1〜CP7回帰、artifact／Public API／dependency／migration境界、child processとcontainer cleanupを
一括確認する。実測件数、時間、process ID、exit status、PostgreSQL versionはCP8 validationへ記録する。

CP8単独でGitHub Actionsを追加・変更しない。remote CIはCP9／CP10を含むMilestone C PRで接続し、性能の
数値閾値はrequired checkにしない。

## 8. 推奨する開始Gate

### Gate 8-1 — 公式仕様と実効構成の確認

- Spring Boot 4.1.1のnon-web起動、runner、exit code、conditional構成を公式資料と実fixtureで確認する。
- PostgreSQL advisory lockとlease方式を公式仕様に照らし、connection／transaction／crash時の寿命を整理する。
- 現在のConsumer artifact、DataSource、Flyway、observability、Testcontainersを再利用できる範囲を確認する。

### Gate 8-2 — Owner方針確認

次を1つの比較表として提示し、承認前にproduction code、module、migration、Public APIを追加しない。

1. Framework contractの有無とartifact owner
2. Consumer module、Tier、launcher／Use Case／outbound adapterの責務配置
3. 排他方式とcrash recovery
4. result／exit status／structured log contract
5. table／migration／Public API差分
6. 実process acceptanceと隔離scriptの完了条件

### Gate 8-3 — 実装・local検証

方針承認後に最小fixtureから実装し、Consumer process acceptance、crash／retry、CP1〜CP7回帰の順に進める。
結果は`docs/architecture/validation/phase1b-cp8-single-execution.md`へ記録し、最終差分review後にCP8を
`LOCAL COMPLETE`と判定する。

## 9. CP8で行わないこと

- Spring Batch Starter、Job／Step実装、業務batch、ETL
- Phase 2のsession cleanup、Spring Security、SecurityContext、監査
- Phase 4のEvent Publication purge、Modulith Level 2、非同期Domain Event
- ECS Scheduled Task、Kubernetes CronJob等のcloud固有adapter／manifest
- Web instance内の無条件な`@Scheduled`、HTTP経由のmaintenance起動
- MyBatis Starter、Mapper、`SEPARATED` model
- CP9の性能harness、性能保証値、required数値閾値
- CP10の正式snapshot、release、Phase 1b Gate 2 closeout
- 正式Reference業務、Project Template、Customer成果物への昇格

## 10. Stop conditions

- taskのowner、module、Tierを決めずにrunner、service、repositoryを横断配置する。
- Spring標準とConsumer直接構成を比較せず、新Starter、独自annotationまたはPublic APIを追加する。
- 単一process内lock、singleton Beanまたは外部scheduler保証だけで二重起動防止を完了扱いする。
- 実OS processを使わず、threads／mockだけで複数instance Evidenceとする。
- advisory lockのconnection lifecycle、leaseの期限／fencing、crash recoveryを実証しない。
- 競合processが業務副作用を起こす、または異常終了後に永久lockとなる。
- processの結果とexit statusが一致しない、または失敗をsuccessとして隠す。
- Customer task／migrationをFramework artifactへ置く、またはFramework tableをCustomer migrationへ置く。
- test用sleep、failure switch、process kill endpointを通常production経路へ露出する。
- child process、DB connection、containerをcleanupできず後続試験へ影響する。
- Spring Batch、Security、Level 2、cloud実装、性能harness等の後続scopeが必要になる。
- 実装証拠がGrand Design、ADRまたはMilestone B baselineを否定する。

該当時は実装を正当化せず、証拠、代替案、Ownership、Public API／migration影響をOwnerへ提示して判断を待つ。

## 11. 次回セッションの開始手順

```powershell
git fetch origin
git switch feature/phase1b-operations-closeout
git pull --ff-only
git status --short --branch
git log -5 --oneline
java -version
.\mvnw.cmd -version
docker version
```

期待状態:

- branchが`feature/phase1b-operations-closeout`
- upstreamが`origin/feature/phase1b-operations-closeout`
- 履歴にMilestone B closeout commit `828a6a7`とこの引継ぎcommitが存在する
- merge baseがmain commit `b3973e6`
- worktreeがclean
- Java 21とMaven Wrapper 3.9.16を実行可能
- CP8のproduction／test差分はまだ存在しない

Docker／PostgreSQLは設計比較後、実process acceptanceのpreflightで確認する。新規セッションで引継ぎを読むだけのために
CP7隔離scriptを再実行する必要はない。環境差、dependency cacheまたはcontainer不整合が疑われる場合だけ、
`verify-cp7-domain-event-mybatis.ps1`をbaselineとして再実行する。

## 12. 次回最初に行うこと

1. Git、Java、Maven、Dockerの実効状態とremote同期を確認する。
2. 正本、CP7 closeout、実効POM／Consumer／CP7 scriptを確認し、Milestone B baselineを固定する。
3. Spring Boot non-web起動とPostgreSQL lock lifecycleを公式資料で確認する。
4. contract／artifact配置、Consumer module／Tier、排他方式の候補をOwnershipとともに比較する。
5. acquired／contended／failedの結果、exit status、structured log contractを整理する。
6. 2 process競合、DB副作用1回、process kill後retryを含む検証方式を具体化する。
7. Public APIとmigrationの要否、隔離script、stop conditionをOwnerへ提示する。
8. 方針承認後にCP8実装へ進む。

## 13. 新規セッションへの開始依頼文

次の文章を、新しいセッションの最初の依頼として使用できる。

> KOIKI-JavaWeb-FWのPhase 1b CP8を開始します。Repository rootの`AGENTS.md`、
> `docs/agent/skills/koiki-project-overview/SKILL.md`、
> `docs/agent/skills/koiki-business-feature-work/SKILL.md`、
> `docs/development/phase1b-cp8-start-handoff-20260829.md`、Phase 1b実行計画、グランドデザイン
> §19.2／§27.5、CP7 validationを確認してください。作業branchは
> `feature/phase1b-operations-closeout`、baselineはmain merge commit `b3973e6`とMilestone B closeout
> commit `828a6a7`です。最初にGit／Java／Maven／Docker状態を確認し、Spring Bootのnon-web起動、
> Framework contractとartifact配置、Consumer module／Tier、PostgreSQL排他方式、result／exit status／log、
> migration／Public API、2 process競合とcrash／retryの検証方式を比較してください。Owner方針確認前に
> CP8 production code、module、migration、Public APIまたはworkflowを追加しないでください。

## 14. 引継ぎ時点の判定

- CP1〜CP7とMilestone A／Bは`COMPLETE`、Milestone Bは`ACCEPTED`済み。
- PR #25 merge、main CI、required check、local identityのcloseout Evidenceはcommit `828a6a7`に記録済み。
- Milestone C branchは最新main `b3973e6`から分岐し、CP8は`START READY / NOT STARTED`。
- Gate 1で外部観測可能な単一実行contractは`ACCEPTED`だが、Java型、artifact、排他方式、exit contract、
  module／Tier、table／migrationはCP8の実装前判断として未確定。
- この文書はCP8の実装許可、方式承認または完了判定ではない。
