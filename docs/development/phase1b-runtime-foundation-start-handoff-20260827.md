# Phase 1b Runtime Foundation開始引継ぎ — 2026-08-27

## 1. 目的と状態

本文書は、2026年8月28日に別PCでPhase 1b Runtime Foundationを開始するための引継ぎである。
Phase 1a closeout後の補正、Phase 1b事前調査、正本との照合結果、開始Gate、commit point、停止条件、
初日の手順をまとめる。

| 項目 | 状態 |
|---|---|
| Phase 1a Build Foundation | `COMPLETE / ACCEPTED` |
| MyBatis metadata guard補正 | `COMPLETE / ACCEPTED`。PR #22 / #23でmain反映済み |
| Phase 1b Runtime Foundation | `NOT STARTED` |
| Phase 1b Gate 1 | `PENDING`。CP0の実行計画と開始判断をOwner Reviewする |
| 作成基準main | `c87e7a5561dff24afea7452f63cce165c666df82` |
| 開始branch | `feature/phase1b-runtime-core`を最新mainから作成する |
| OpenSpec | active changeなし。Phase 1bの必須toolingにはしない |

branch作成とCP0文書化は開始準備であり、Gate 1承認前にruntime production code、Maven module、
Starter、Public API、migration、CI checkを追加しない。

## 2. 正本と適用順序

1. Repository rootの`AGENTS.md`
2. `docs/agent/skills/koiki-project-overview/SKILL.md`
3. `docs/architecture/grand-design/KOIKI-JavaWeb-FW_グランドデザイン_v0.2.md`
4. `docs/architecture/KOIKI-JavaWeb-FW_Repository_Architecture_v0.1.md`
5. `docs/architecture/adr/README.md`
6. `docs/architecture/KOIKI-JavaWeb-FW_Phase_Estimate_Feasibility_v0.1.md`
7. `docs/architecture/validation/phase1a-closeout.md`
8. `docs/architecture/validation/phase1b-prep-mybatis-metadata-guard.md`
9. 本文書

Phase 1bの要求正本はグランドデザイン§27.5、分割とリスクの正本はFeasibility §4である。
本文書は要求を削減または変更せず、実行順序と停止条件を具体化する。

## 3. 作業位置

```text
Phase / status: Phase 1b Runtime Foundation / NOT STARTED
Ownership: Framework主体。BOM、CI、性能・統合fixtureはTooling
Target: CP0実行計画。Gate 1承認後に必要性が実証されたruntime moduleだけを追加
Applicable guidance: Grand Design、Repository Architecture、ADR-002 / 005 / 012 / 025 / 028 / 030〜032 / 039 / 041 / 042
Validation: Java 21、Maven Wrapper、Docker/Testcontainers、ArchUnit、NullAway、japicmp、required checks
Deferred decisions: Security、Reference業務、REST受入、Level 2、非同期event、Oracle、cloud固有実装、正式release
```

## 4. 本日までに完了した前提

### 4.1 Phase 1a

- 正式4成果物、Architecture Contract、ArchUnit、NullAway、Public API compatibility、Java 21 / 25
  runtime matrix、Feature Template、CI required checksを`COMPLETE / ACCEPTED`としてcloseした。
- 現行Root ReactorはBOM、Parent、Architecture Contract、ArchUnit Rulesの4 moduleとAggregatorだけである。
- Spring Modulith 2.1.0はLevel 0のtest scopeだけで使用し、runtime依存を追加していない。
- GitHub PackagesのC1 timestamped snapshotはPhase 1aのimmutable baselineとして維持する。

### 4.2 MyBatis metadata guard

- `MYBATIS + SHARED`をKOIKI-ARCH-008で拒否し、ADR-039と`SEPARATED`未提供理由を表示する。
- Public API inventoryとjapicmp baselineは変更していない。
- `PersistenceTechnology.MYBATIS`の維持／削除は、Phase 1bのrelease unit / baseline方針決定時に再判定する。
- Phase 1bでは`SEPARATED`、MyBatis Starter、Mapper実装規約、DoD 31〜34を先行実装しない。

## 5. Phase 1b要求

成果物はCore Configuration、Problem Details、Validation、構造化logとcontext伝播、Actuator、PostgreSQL、
Flyway、Testcontainers、Jackson 3、Resilience、API Versioning、OSIV無効化、単一実行基盤、MyBatis BOM管理、
Domain Event規約、性能baseline harnessである。

完了時に次の8 DoDを削らず判定する。

| DoD | 完了条件 |
|---|---|
| 1b-1 | FlywayがKOIKI階層とCustomer階層で独立適用され、versionが干渉しない |
| 1b-2 | 未処理例外が`JacksonException`を含め統一エラー形式で返る |
| 1b-3 | 構造化logに相関IDが載り、`@Async`境界を越えて伝播する |
| 1b-4 | Testcontainers PostgreSQL統合テストがCIで動作する |
| 1b-5 | Actuator healthが応答し、DB接続状態を反映する |
| 1b-6 | OSIVが無効で、View層へのEntity露出がテストで検出される |
| 1b-7 | 定期処理がWeb instance外の単一実行基盤から起動し、複数instanceでも二重起動しない |
| 1b-8 | Framework overheadが計測され、再現条件とbaselineが記録される |

全Phase共通DoDのSpring Boot baseline、ADR / Owner approval、CI quality gates、Agent Skills、
table / Flyway ownershipもGate 2で再判定する。

## 6. 事前調査の精査結果

### 6.1 採用できる方針

- 正式Gateを開始承認とPhase完了承認の2回に絞る。
- A / B / C途中はGateではなく、local検証済みcommit pointとmilestone PRで管理する。
- local検証を主経路とし、remote CIはmilestone単位にまとめる。
- 未使用の将来module、Starter、package、Public APIを先行生成しない。
- Security、Reference、Level 2、Oracle、cloud固有実装をPhase 1bへ混入させない。
- commit pointごとに実装時間、local検証時間、CI待ち時間、手戻り回数を記録する。

### 6.2 修正した方針

1. **main identityを更新する。** 事前調査時の`8a19cf1`ではなく、MyBatis補正closeout後の
   `c87e7a5561dff24afea7452f63cce165c666df82`を開始基準とする。明日は必ずremoteと再照合する。
2. **A / B / Cを1本の長期branchで進めない。** 各milestoneを最新mainから分岐する最大3 PRとする。
3. **CP1で空Starter骨格を一括生成しない。** 最初の実行可能fixtureが要求するmoduleだけを、同じcommitで
   production code、test、依存境界とともに追加する。
4. **Flyway三階層をPhase 1bの既定にしない。** DoD 1b-1はKOIKI / Customer二階層である。
   Grand Design §16.7.2のReferenceを含む三階層へ一般化するかはCP0の判断対象とする。
5. **SecurityContextを実装しない。** Phase 1bの1b-3は相関IDの伝播を実証する。Context Propagationは
   将来のAccessor追加を妨げない構造にするが、Security実装とSecurity testはPhase 2へ残す。
6. **性能数値へPC固有値を混ぜない。** workload、warm-up、反復、JDK、OS、CPU、memory、Docker、DB image、
   commitをfingerprintとして記録し、異なるPCの数値を同一baselineとして直接比較しない。
7. **Named Interfaceは採用候補であり、既定ではない。** Phase 1bでLevel 0を回帰し、採否とruntime依存の
   要否を証拠に基づいて判断する。Level 1 / 2へ到達したとは扱わない。
8. **MyBatis 4.1.0はBOM候補だけである。** Starter利用、`SEPARATED`、Mapper、Reference実装へ広げない。

### 6.3 version baseline

2026年8月27日の公式情報とcache済みSpring Boot BOMを照合した。

| 項目 | 開始候補 | 扱い |
|---|---:|---|
| Spring Boot | 4.1.1 | 現行維持。開始時に公式stableと再照合 |
| Spring Modulith | 2.1.1 | 現行2.1.0からのpatch候補。CP1でLevel 0回帰後に採否決定 |
| MyBatis Spring Boot | 4.1.0 | BOM管理候補。runtime dependencyへ追加しない |
| Flyway | 12.4.0 | Boot 4.1.1 BOM管理値 |
| Jackson BOM | 3.1.5 | Boot 4.1.1 BOM管理値 |
| Testcontainers | 2.0.5 | Boot 4.1.1 BOM管理値 |
| PostgreSQL Driver | 42.7.13 | Boot 4.1.1 BOM管理値 |

確認先:

- https://spring.io/projects/spring-boot/
- https://docs.spring.io/spring-modulith/reference/index.html
- https://github.com/mybatis/spring-boot-starter/releases

versionはCP0文書へ固定候補として記録し、BOM変更はGate 1承認後の独立commitで検証する。

## 7. Gate計画

### Gate 1 — Phase 1b開始承認

Gate 1はCP0実行計画を対象とする。次をOwnerが承認するまでCP1へ進まない。

1. 8 DoDと全Phase共通DoDをA / B / Cへ過不足なくtraceする。
2. Boot 4.1.1維持、Modulith 2.1.1 patch候補、MyBatis 4.1.0 BOM候補を確認する。
3. runtime artifact / Starterのownership、依存方向、最初の利用fixtureを決める。
4. Flyway自動構成の所有artifactと、二階層／三階層のPhase 1b scopeを決める。
5. 新規artifactをPhase 1b release unit、Public API inventory、japicmp、snapshotへどう組み込むか決める。
6. Named Interfaceの検証方法と、採用／不採用の判定条件を決める。
7. 単一実行基盤のcloud非依存contract、二重起動防止候補、複数process検証方法を決める。
8. 性能harnessのworkload、warm-up、反復、環境fingerprint、結果形式を決める。
9. local中心、最大3 milestone PR、Testcontainers CI、性能計測のCI扱いを決める。
10. Security、Reference、REST受入、Level 2、Oracle、cloud固有、正式releaseの除外を確認する。

Gate 1のOwner Review結果はPhase 1b実行計画へ記録する。本文書の作成だけでは承認済みと扱わない。

### Gate 2 — Phase 1b完了承認

次をまとめて1回判定する。

- DoD 1b-1〜1b-8
- 全Phase共通DoD
- ADRの追加／改訂要否とOwner Review
- Agent Skillsの追加／更新要否
- release unit、Public API、japicmp baseline、snapshot方針
- local最終検証と最終PR required checks
- 後続Phaseへのdeferred scope

Gate 2は最終PRのrequired checks成功を正式Evidenceとして`ACCEPTED — MAIN CI PENDING`まで判定する。
merge後main CIは運用確認とし、成功時にPhase 1bを`COMPLETE / ACCEPTED`としてcloseする。main CI結果だけを
追記する追加Evidence PRは作成せず、失敗または記録との不一致が生じた場合だけ再openする。

## 8. Milestone、branch、commit point

| Milestone | Branch | Commit point | 内容 |
|---|---|---:|---|
| A Runtime Core | `feature/phase1b-runtime-core` | CP0 | 実行計画、DoD trace、baseline、module / Starter ownership、Gate 1 |
| | | CP1 | Modulith patch回帰と、最初の実行可能fixtureが必要とする最小artifact／override contract |
| | | CP2 | Core Configuration、Jackson 3、Resilience、API Versioning |
| | | CP3 | Problem Details、Validation、`JacksonException` positive / negative / restore |
| B Data & Runtime Integration | `feature/phase1b-data-runtime-integration` | CP4 | PostgreSQL Testcontainers、Flyway ownership、version衝突・順序負例 |
| | | CP5 | 構造化log、相関ID、`@Async`伝播、thread再利用時の漏えい負例 |
| | | CP6 | Actuator DB health、OSIV無効化、Entity露出負例 |
| | | CP7 | Named Interface / Domain Event判断、MyBatis BOM管理のみ |
| C Operations & Closeout | `feature/phase1b-operations-closeout` | CP8 | cloud非依存の単一実行contract、複数process二重起動防止 |
| | | CP9 | 性能baseline harness、environment fingerprint、結果記録 |
| | | CP10 | DoD、ADR、Skills、release unit、Public API、最終CI、Gate 2 closeout |

CP番号は計画上の検証点であり、必ず1 CP = 1 commitとは限らない。独立してreview可能な論理差分を優先し、
未検証の大規模commitへまとめない。

## 9. Commit pointの検証と計測

各commit候補で次を記録する。

| 記録項目 | 内容 |
|---|---|
| Source | branch、commit候補、dirty状態 |
| Scope | 対象artifact、production / fixture ownership、Public API差分 |
| Implementation | 実装作業時間 |
| Local verification | command、所要時間、test数、結果 |
| CI wait | pushからrequired checks完了までの時間 |
| Rework | 手戻り回数、原因、修正時間 |
| Deferred | 今回実装しなかった後続判断 |

通常は対象moduleの`-pl ... -am verify`とpositive / negative / restoreを実行する。A / B / CのPR候補では
Root `clean verify`、Feature Template、NullAway、Public API compatibilityの該当local検証をまとめて行う。

Testcontainers PostgreSQLはBでCIへ追加し、安定性と実行時間を確認してからrequired check化を判断する。
性能値は環境noiseがあるためrequired checkにせず、harnessの再現性と結果schemaを通常CIで検査する。

## 10. Stop conditions

次に該当した場合は実装を止め、Gate 1または該当ADRへ戻る。

- Starter ownershipを決めずに新規moduleが必要になった。
- 概念上の`koiki-framework`、`koiki-starters`、`koiki-testing`を空moduleとして一括生成しようとしている。
- 新規artifactまたはPublic APIを追加するが、release unit / japicmp / snapshot方針が未決定である。
- Flyway二階層DoDを三階層へ読み替え、Reference migrationを正式成果物として追加しようとしている。
- MyBatis BOM管理を超えてStarter、Mapper、`SEPARATED`または業務moduleを実装しようとしている。
- Named Interface採用によりSpring Modulith runtime dependencyやLevel 1 / 2が必要になった。
- SecurityContext、Spring Security、Reference業務、REST受入または非同期Domain Eventが必要になった。
- 単一実行基盤がECS / Kubernetes固有実装をFramework contractへ漏らす。
- 二重起動防止が単一process内のlockだけで成立し、複数process / DB競合で実証できない。
- 性能値にenvironment fingerprint、warm-up、反復、raw resultがなく比較不能である。
- Docker / Testcontainers / CI runnerが不安定で、DoD 1b-4の再現条件を満たさない。
- 実装証拠がGrand DesignまたはADRの前提を否定した。

## 11. 別PCでの開始手順

### 11.1 Repository同期

```powershell
git switch main
git pull --ff-only
git log -5 --oneline --decorate
git status --short --branch
```

mainがremoteと一致し、worktreeがcleanであることを確認する。開始branchがremoteへpush済みなら取得し、
未作成なら最新mainから作る。

```powershell
git switch feature/phase1b-runtime-core
```

branchが存在しない場合だけ次を実行する。

```powershell
git switch -c feature/phase1b-runtime-core
```

### 11.2 別PCのenvironment再調査

本日PCのDocker割当6 CPU / 15.6 GiBは参考値であり、明日のPCへ引き継がない。次を再取得する。

```powershell
java -version
./mvnw.cmd -version
docker version
docker info
git status --short --branch
```

最低限、次をCP0へ記録する。

- OSとarchitecture
- JDK vendor / version
- Maven Wrapper version
- Docker Desktop / daemon versionとLinux container mode
- Docker割当CPU / memory
- PostgreSQL Testcontainers image pull / start可否
- timezone

性能baselineはこのenvironment identityを持たない結果を採用しない。

### 11.3 変更前baseline

```powershell
./mvnw.cmd --batch-mode --no-transfer-progress clean verify
pwsh -NoProfile -File build-support/feature-templates/verify-feature-templates.ps1
pwsh -NoProfile -File build-support/null-safety/verify-null-safety.ps1
```

認証を必要とするC1 baseline比較は通常PRの`Public API Compatibility`を正式経路とする。CP0でPublic APIを
変更しない限りlocal PATを再入力しない。

### 11.4 初日の作業順

1. 正本、本文書、Phase 1a closeout、MyBatis metadata guard closeoutを読む。
2. 別PCのenvironmentと変更前baselineを記録する。
3. `docs/development/KOIKI-JavaWeb-FW_Phase1b実行計画_v0.1.md`をCP0として作成する。
4. §7 Gate 1の10項目、DoD trace、stop conditions、time log形式を計画へ入れる。
5. module / Starter ownership、release unit、Public API、Flyway scope、Named Interface、単一実行、性能harnessを
   Owner Reviewへ提示する。
6. Gate 1が`ACCEPTED`になるまでCP1のPOM、module、production codeを追加しない。

## 12. 明日セッションへの開始依頼文

> KOIKI-JavaWeb-FWはPhase 1aとPhase 1b着手前MyBatis metadata guard補正を`COMPLETE / ACCEPTED`として
> close済みです。Phase 1b Runtime Foundationは`NOT STARTED`で、最初の作業はCP0実行計画とGate 1
> Owner Reviewです。`docs/development/phase1b-runtime-foundation-start-handoff-20260827.md`を正本への
> 導線として読み、別PCのJava / Maven / Docker / CPU / memoryを再調査し、変更前baselineを確認してください。
> `feature/phase1b-runtime-core`上でPhase 1b実行計画を作成し、Gate 1の10項目が承認されるまで空module、
> Starter、Public API、migration、production codeを追加しないでください。A / B / Cは最大3 PRとし、
> Security、Reference、Level 2、Oracle、cloud固有実装、正式releaseを混入させないでください。

## 13. 本引継ぎの検証範囲

- Grand Design §16.7.2、§19.2、§20.1、§21.5〜21.6、§22、§27.5を実ファイルで確認した。
- Feasibility §4のPhase 1b分割、規模、主要リスクを確認した。
- Root Reactor、BOM、Parent、CI、runtime compatibility workflowを確認した。
- 2026年8月27日時点でactive OpenSpec changeがないことを確認した。
- Spring Boot 4.1.1、Spring Modulith 2.1.1、MyBatis Spring Boot 4.1.0の公式公開情報を確認した。
- cache済みBoot 4.1.1 BOMでFlyway 12.4.0、Jackson BOM 3.1.5、Testcontainers 2.0.5、
  PostgreSQL Driver 42.7.13を確認した。
- 本日PCではJava 21.0.12.1、Maven 3.9.16、Docker daemon 29.5.3 Linux amd64を確認した。
  Docker resource値は明日の別PCへ引き継がず、再調査する。
- 本引継ぎではruntime code、module、dependency、Public API、migration、workflow、snapshotを変更していない。
