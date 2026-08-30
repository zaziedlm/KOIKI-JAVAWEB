# Phase 1b CP10 Developer Journey／DoD／Gate 2 final closeout

## 1. Status

- **Current state:** `GATE 2 ACCEPTED / MILESTONE C COMPLETE / PHASE 1B COMPLETE`
- **Completion date:** 2026年8月30日
- **Architecture Owner:** Shuichi Kataoka
- **Remote state:** PR #26、merge後main CI、9成果物snapshot publication、fresh remote Consumer検証を完了
- **Phase state:** Phase 1b Runtime Foundationは完了。後続scopeはPhase 2以降で個別に開始判定する

OwnerはGate 10-1 inventoryを確認後、2026年8月30日にGate 10-2案と予定する最小実装を承認した。
CP10ではFramework production code、Public Java API、production migration、業務moduleを追加していない。
Ownerは同日、5点の差分review反映と最終aggregate結果を確認し、CP10 local closeoutを承認した。
さらにPR／main CI、ruleset、snapshot publicationおよびremote Consumer Evidenceを確認し、Gate 2、Milestone C、
Phase 1bの最終完了を承認した。

## 2. Source／environment identity

| 項目 | 結果 |
|---|---|
| Branch / implementation closeout commit | `feature/phase1b-operations-closeout` / `792172576a964592a9c55c0f829433387d30bf84` |
| main merge base | `b3973e66134898765b95796c3622aaa68759b4fd` |
| Final PR head | `d43d8e4c9b2c1d5dc6cfe4af8cdfb6003ed4c74e` |
| Merge commit / main | `40d16f9dbf26a7ba88ac13b2e3728075e0eff2a7` / local・origin一致 |
| Final Evidence branch base | `docs/phase1b-final-closeout` / `40d16f9dbf26a7ba88ac13b2e3728075e0eff2a7` |
| Git identity | `Shuichi Kataoka <shu01k9@gmail.com>` |
| Build runtime | Eclipse Temurin 21.0.12.1、Apache Maven 3.9.16 |
| Container runtime | Docker Engine 29.5.3、Linux daemon |
| Database | `postgres:17-alpine`、acceptanceでPostgreSQL 17.11 |
| Spring Boot baseline | 4.1.1。2026年8月30日の公式stable確認とBOM／dependency inventoryが一致 |

local evidenceは未commit差分上で採取し、implementation closeout commit `7921725`として固定・pushした。
PRおよびremote run identityは§6.3へ記録する。

## 3. Gate 10-1 inventory

### 3.1 Release unit／snapshot unit

cleanな隔離Maven repositoryへ次の10 projectsをstageした。

1. `koiki-javaweb-fw-reactor`（root aggregator、配布しない）
2. `koiki-dependencies-bom`
3. `koiki-parent`
4. `koiki-architecture-contract`
5. `koiki-archunit-rules`
6. `koiki-starter-api`
7. `koiki-starter-data`
8. `koiki-starter-data-jpa`
9. `koiki-starter-observability`
10. `koiki-testing`

内部snapshotの配布単位はroot aggregatorを除く9成果物とする。file repositoryへの`clean deploy`と、別の空repositoryから
全9座標をtransitive resolveするdry-runは成功した。実GitHub Packagesへのpublishは承認済みmain commitだけを対象とする
手動workflowへ分離し、CP10 local closeoutでは実行していない。

### 3.2 Public API／configuration properties

| Artifact | Public Java types | Public configuration properties |
|---|---:|---:|
| `koiki-architecture-contract` | 4 | 0 |
| `koiki-archunit-rules` | 1 | 0 |
| `koiki-starter-api` | 0 | 6 |
| `koiki-starter-data` | 0 | 2 |
| `koiki-starter-data-jpa` | 0 | 1 |
| `koiki-starter-observability` | 0 | 3 |
| `koiki-testing` | 0 | 0 |

Architecture Contractの4型、ArchUnit Rulesの1型および既存method baselineを維持した。runtime Starterは
Public Java typeを持たず、合計12 configuration propertiesだけを外部契約とする。Consumer runtime treeには
Architecture Contractと4 runtime Starterが存在し、ArchUnit Rules、Testing、Spring Batch、Spring Cloud、
Kubernetes、MyBatis、Spring Modulithは存在しない。

### 3.3 Migration／table ownership

Framework正式成果物のproduction SQLは0件である。Customer-like Consumerだけが次を所有する。

| Migration | Table |
|---|---|
| `V1__create_work_item.sql` | `kkbiz_work_item` |
| `V3__create_work_review.sql` | `kkbiz_work_review` |
| `V4__create_work_item_maintenance.sql` | `kkbiz_work_item_maintenance` |

実起動後はKOIKI history、Customer history、Customer table 3件の計5 tablesを確認した。Customer historyは
baseline markerを除く3 migrationsが成功している。

## 4. Minimum implementation

- `verify-cp10-closeout.ps1`をToolingへ追加し、既存CP8 aggregate、10-project staging、Public API／migration inventory、
  packaged Consumerのweb／maintenance journey、CP9 smokeおよびcleanupを一括化した。
- 通常CIへ`Milestone C Closeout` jobを追加した。
- 既存の手動snapshot workflowをPhase 1bの単一publish入口へ更新した。preflight後に9成果物だけをpublishし、別jobの空repositoryから
  全座標をresolveして独立Consumerをbuildする。
- root、Consumer、verification、workflow READMEを現状へ更新した。
- ADRは既存判断を変更しないため追加不要、KOIKI固有Skillsも新規規約を導入しないため更新不要と判断した。

## 5. Packaged Developer Journey evidence

`runtime-foundation-consumer-application-0.1.0-SNAPSHOT.jar`をCustomer-likeな配布JARとして専用PostgreSQL上で実行した。

1. readinessが`UP`になった後、version付き`POST /api/1/work-items`が201を返し、Controller→Use Case→同期event→
   Domain→Repository→2 moduleのDB rowを観測した。
2. Validationは400／`KOIKI-VALIDATION-001`かつ拒否値非露出、Domain invariant違反は422／`WORKREVIEW-001`かつ
   両module rollbackとなった。
3. async endpointは202を返し、HTTPと`work item async processed`構造化logで同じrequest IDを観測した。
4. general／liveness／readiness healthは`UP`で、JDBC URL、password、例外、stack traceを露出しなかった。
5. 同じJARをmaintenance modeのnon-web processとして起動し、exit 0、listen portなし、DB副作用1回、成功lifecycle logを確認した。
6. child JVM、専用PostgreSQL container、隔離repositoryおよび一時directoryを終了時にcleanupした。

## 6. Verification results

| 検証 | 結果 |
|---|---|
| `verify-cp10-closeout.ps1` | PASS、`00:08:35.1652219` |
| CP1〜CP8 aggregate回帰 | PASS。Contract 4件、ArchUnit 66件、Consumer application 25件、実process排他／crash recoveryを含む |
| CP8 process evidence | winner／contender排他、contender exit 10、crashed holder後retry exit 0、PostgreSQL 17.11 |
| CP9 `-Smoke -SkipRegression` | PASS、run ID `412c90fcbe5a4b72b8252fe4609c4874` |
| 9-artifact deploy／resolve dry-run | PASS、9成果物、root aggregatorなし、`00:01:36.8214177` |
| Feature Template | PASS、positive、Tier別期待failure、restore、runtime dependency boundary |
| NullAway | PASS、positive、期待failure、restore |
| Public API fixtures | PASS、compatible、return type破壊／未承認追加の期待failure |
| Java runtime guards | PASS、Java 25 build拒否、hash／runtime mismatch拒否、Java 21／25 restore |
| Worktree preservation／cleanup | PASS。aggregate前後のvisible status一致、一時container／repository削除 |

### 6.1 Rework record

| # | 検出 | 修正 |
|---:|---|---|
| 1 | Consumer dependency treeがleaf moduleだけを評価しtransitive artifactを欠落判定 | `-pl application -am`でreactor全体を解決 |
| 2 | PowerShell 7.5がActuator vendor JSONを`byte[]`で返しreadinessを解釈不能 | HTTP helperでUTF-8文字列へ正規化 |
| 3 | Customer migration数へFlyway baseline markerを含め4件と誤判定 | `BASELINE`を除く実migration 3件を評価 |
| 4 | dry-runがread-onlyな既定`.m2`へinstallし失敗 | OS tempの専用build repositoryと空resolve repositoryへ分離 |

いずれもTooling／受入条件の手戻りであり、Framework production実装の修正は発生していない。

### 6.2 Diff review adjustments

| # | 指摘 | 対応／判断 |
|---:|---|---|
| 1 | remote journeyの重複 | preflightで実process、publish後は9座標のfresh resolve＋build／testに分担。配布条件変更時はremote起動も再必須化 |
| 2 | publish入口の重複 | `publish-snapshot.yml`へ統合。Phase 1a実績は文書とGit履歴で保存 |
| 3 | statusの先行確定 | Owner承認までは`OWNER REVIEW IN PROGRESS` |
| 4 | tableの追加漏れ | `public` BASE TABLEを期待5表と完全一致で判定 |
| 5 | cleanup結果の未判定 | `docker rm --force`失敗をaggregate失敗にする |

指摘1は検証削減ではなく、preflightとpost-publishの責務分離である。将来Application側の配布条件が変化した時点を
見直し契機として明記し、現段階では同一journeyの二重実行をGate条件にしない。

### 6.3 Milestone C PR evidence

| 項目 | Evidence |
|---|---|
| Pull Request | [#26](https://github.com/zaziedlm/KOIKI-JAVAWEB/pull/26)、MERGED |
| Base / verified implementation head | `main` (`b3973e6`) / `792172576a964592a9c55c0f829433387d30bf84` |
| CI run | `33298207252`。Verify、Public API Compatibility、Milestone B Integration、Milestone C CloseoutがSUCCESS |
| Runtime run | `33298207273`。Build Runtime FixtureとJava Runtime CompatibilityがSUCCESS |
| Final PR recheck | CI `33310095529`、Runtime `33310095523`。更新後HEAD `d43d8e4`で全job SUCCESS |
| Required checks | Verify、Public API Compatibility、Java Runtime Compatibility、Milestone B Integration、Milestone C Closeoutの5件がSUCCESS |
| Ruleset | `main-merge-protection` (`21140116`)、active、strict、bypass 0。2026年8月30日20:49 JSTにMilestone Cをrequiredへ追加 |
| Merge | 2026年8月30日21:03 JST、merge commit `40d16f9dbf26a7ba88ac13b2e3728075e0eff2a7` |

初回runとdocs-only Evidence追記後の最終runを分離して記録し、最終PR headに対するrequired checks成功後にmergeした。

### 6.4 Main／snapshot remote evidence

| 項目 | Evidence |
|---|---|
| Main CI | [run 33310474870](https://github.com/zaziedlm/KOIKI-JAVAWEB/actions/runs/33310474870)、merge commit `40d16f9`、4 jobs SUCCESS |
| Main runtime | [run 33310474844](https://github.com/zaziedlm/KOIKI-JAVAWEB/actions/runs/33310474844)、Java 21 build／Java 21・25 runtime SUCCESS |
| Environment | `phase1b-internal-snapshot` (`20873699719`)。required reviewer `zaziedlm`、main限定、admin bypass禁止 |
| Snapshot workflow | [run 33311794583](https://github.com/zaziedlm/KOIKI-JAVAWEB/actions/runs/33311794583)、approved main SHA `40d16f9dbf26a7ba88ac13b2e3728075e0eff2a7`、SUCCESS |
| Publication | root aggregatorを除く9 artifactsを同一session／`deployAtEnd=true`でpublishし、GitHub Packages上の9 packagesを確認 |
| Remote Consumer | 空のfresh Maven repositoryから全9座標をresolveし、Root Reactor外Consumerの`clean verify`がSUCCESS |

snapshot workflowではauthorize、CP10 preflight（8分9秒）、environment承認、9-artifact publish、remote Consumerの
全jobが成功した。credential実値、通常local cacheまたはFramework source pathへ依存していない。

## 7. DoD closeout

| DoD | Local判定 | Evidence |
|---|---|---|
| 全Phase共通1 最新Boot minor | PASS | Boot 4.1.1公式stable、BOM／dependency inventory |
| 全Phase共通2 ADR／Owner approval | PASS | Gate 1、Gate 10-2、CP10差分review、Gate 2最終判定をOwner承認 |
| 全Phase共通3 CI quality gates | PASS | local aggregate、PR required checks 5件、merge後main CI、snapshot preflightがSUCCESS |
| 全Phase共通4 Agent Skills | PASS | Project Overview／Business Feature workflowを適用。規約差分なし、Skill更新不要 |
| 全Phase共通5 table／Flyway ownership | PASS | Framework SQL 0、Customer migration／table 3、history分離 |
| 1b-1 Flyway二階層 | PASS | [CP4](phase1b-cp4-data-runtime.md)とpackaged runtime再確認 |
| 1b-2 統一error | PASS | [CP3](phase1b-cp3-problem-details.md)と400／422外部観測 |
| 1b-3 log／correlation | PASS | [CP5](phase1b-cp5-observability.md)とasync構造化log |
| 1b-4 PostgreSQL CI | PASS | [CP4](phase1b-cp4-data-runtime.md)、PR／main／snapshot preflightのMilestone C Closeout成功 |
| 1b-5 health | PASS | [CP6](phase1b-cp6-health-osiv.md)と3 health endpoint |
| 1b-6 OSIV | PASS | [CP6](phase1b-cp6-health-osiv.md)回帰 |
| 1b-7 単一実行 | PASS | [CP8](phase1b-cp8-single-execution.md)回帰と同一JAR maintenance journey |
| 1b-8 性能baseline | PASS | [CP9](phase1b-cp9-performance-baseline.md)公式baselineとCP10 smoke |
| Customer-like利用可能性 | PASS | package済み横断journey、9-artifact snapshot、fresh remote resolve、独立Consumer build／test成功 |

## 8. Deferred／Phase boundary

Security、Reference業務、SPA／非同期API、Oracle、Spring Modulith Level 2、cloud固有scheduler、Project Template、
正式releaseはPhase 1bへ混在させず、所定の後続Phaseへ残す。Customer-like ConsumerはTooling Evidenceとして維持し、
正式ReferenceまたはProject Templateへ昇格しない。Phase 2以降は別の開始計画とOwner Gateを経て着手する。
