# Phase 1b CP10 Developer Journey／DoD／Gate 2 local closeout

## 1. Status

- **Current state:** `CP10 LOCAL COMPLETE / GATE 2 LOCAL READY`
- **Review date:** 2026年8月30日
- **Architecture Owner:** Shuichi Kataoka
- **Remote state:** Milestone C PR、required checks、snapshot publication、main merge／main CIは未実施
- **Phase state:** Phase 1bは未完了。remote evidenceを得るまでGate 2およびPhase完了へ昇格しない

OwnerはGate 10-1 inventoryを確認後、2026年8月30日にGate 10-2案と予定する最小実装を承認した。
CP10ではFramework production code、Public Java API、production migration、業務moduleを追加していない。
Ownerは同日、5点の差分review反映と最終aggregate結果を確認し、CP10 local closeoutを承認した。

## 2. Source／environment identity

| 項目 | 結果 |
|---|---|
| Branch / HEAD | `feature/phase1b-operations-closeout` / `f14f587bed4aec7aba5402e598251fd9f0c02a60` |
| main merge base | `b3973e66134898765b95796c3622aaa68759b4fd` |
| Upstream差分 | behind 0 / ahead 5 |
| Git identity | `Shuichi Kataoka <shu01k9@gmail.com>` |
| Build runtime | Eclipse Temurin 21.0.12.1、Apache Maven 3.9.16 |
| Container runtime | Docker Engine 29.5.3、Linux daemon |
| Database | `postgres:17-alpine`、acceptanceでPostgreSQL 17.11 |
| Spring Boot baseline | 4.1.1。2026年8月30日の公式stable確認とBOM／dependency inventoryが一致 |

本記録は未commitのcloseout差分上で採取したlocal evidenceである。commit SHA、PRおよびremote run identityは
Gate 10-4で追記する。

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

## 7. DoD closeout

| DoD | Local判定 | Evidence |
|---|---|---|
| 全Phase共通1 最新Boot minor | PASS | Boot 4.1.1公式stable、BOM／dependency inventory |
| 全Phase共通2 ADR／Owner approval | LOCAL PASS | Gate 1 accepted、Gate 10-2実装承認、CP10差分review承認。Gate 2最終承認はremote後 |
| 全Phase共通3 CI quality gates | LOCAL VERIFIED | local aggregate成功、Milestone C job追加。PR／main CI未実施 |
| 全Phase共通4 Agent Skills | PASS | Project Overview／Business Feature workflowを適用。規約差分なし、Skill更新不要 |
| 全Phase共通5 table／Flyway ownership | PASS | Framework SQL 0、Customer migration／table 3、history分離 |
| 1b-1 Flyway二階層 | PASS | [CP4](phase1b-cp4-data-runtime.md)とpackaged runtime再確認 |
| 1b-2 統一error | PASS | [CP3](phase1b-cp3-problem-details.md)と400／422外部観測 |
| 1b-3 log／correlation | PASS | [CP5](phase1b-cp5-observability.md)とasync構造化log |
| 1b-4 PostgreSQL CI | LOCAL READY | [CP4](phase1b-cp4-data-runtime.md)、CP10 local PostgreSQL 17。Milestone C remote待ち |
| 1b-5 health | PASS | [CP6](phase1b-cp6-health-osiv.md)と3 health endpoint |
| 1b-6 OSIV | PASS | [CP6](phase1b-cp6-health-osiv.md)回帰 |
| 1b-7 単一実行 | PASS | [CP8](phase1b-cp8-single-execution.md)回帰と同一JAR maintenance journey |
| 1b-8 性能baseline | PASS | [CP9](phase1b-cp9-performance-baseline.md)公式baselineとCP10 smoke |
| Customer-like利用可能性 | LOCAL PASS | 空repository、package、HTTP／DB／log／health／maintenanceの横断journey |

## 8. Deferred／next gate

Security、Reference業務、SPA／非同期API、Oracle、Spring Modulith Level 2、cloud固有scheduler、Project Template、
正式releaseはPhase 1bへ混在させない。次はGate 10-4として、差分review後にMilestone C commit／push／PRを明示承認の下で行い、
required checks、手動snapshot publication、remote-artifact Consumer、merge後main CIを記録する。その証拠を得てから
Architecture OwnerがGate 2および`PHASE 1B COMPLETE`を判定する。
