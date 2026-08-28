# Phase 1b CP6開始引継ぎ — 2026-08-28

## 1. この文書の位置づけ

この文書は、CP5 local完了で作業を区切り、新規AIセッションでPhase 1b CP6を安全に開始するための
運用上の引継ぎメモである。CP6の設計判断や検証結果の正本ではない。CP6で得た実装証拠は、次回
`docs/architecture/validation/phase1b-cp6-health-osiv.md`を作成して記録する。

判断が競合する場合は、次の順に正本を確認する。

1. Repository rootの`AGENTS.md`
2. `docs/agent/skills/koiki-project-overview/SKILL.md`
3. `docs/development/KOIKI-JavaWeb-FW_Phase1b実行計画_v0.1.md`
4. `docs/architecture/grand-design/KOIKI-JavaWeb-FW_グランドデザイン_v0.2.md`
5. `docs/architecture/adr/`
6. CP4／CP5の`docs/architecture/validation/`記録
7. 実効構成である各`pom.xml`、Maven Wrapper、source、test、検証script

CP6はFramework設定とCustomer-like ConsumerのWeb／JPA境界を扱う。開始時は
`koiki-project-overview`を使用し、ConsumerのController、Use Case、JPA modelまたはHTTP経路を変更する場合は
`koiki-business-feature-work`も使用する。

## 2. 引継ぎ時点のGit・remote状態

| 項目 | 状態 |
|---|---|
| branch | `feature/phase1b-data-runtime-integration` |
| main / merge base | `c6f2f86083c5350ee7a13e4ababcc237034d388d`（PR #24 merge後main） |
| CP4 commit | `4e989dd` `feat(data): add CP4 PostgreSQL and two-tier Flyway integration` |
| CP5 commit / CP6 baseline | `547e5b757cad911de6ee69e21061662ac64e9c73` `feat(observability): complete CP5 structured logging and async correlation` |
| worktree | CP5 commit直後はclean |
| upstream | 引継ぎ作成時点では未設定。引継ぎcommit後に初回pushする |
| PR / remote CI | Milestone Bは未作成／未接続。CP7 local完了後に接続する |

この引継ぎ文と`docs/development/README.md`の索引更新をdocs-only commitとし、その後次でpushする。

```powershell
git push -u origin feature/phase1b-data-runtime-integration
```

## 3. Phase / Ownership / 対象

| 項目 | 内容 |
|---|---|
| Phase / status | Phase 1b Milestone B / CP5 LOCAL COMPLETE / CP6 START READY |
| Framework ownership | Actuator基本設定候補、DB healthの安全な既定、JPA OSIV無効化の低優先度既定 |
| Tooling ownership | 細粒度fixture、Customer-like Consumer、PostgreSQL異常／復旧、Entity露出負例、隔離script |
| Customer ownership | 業務Entity、DTO／read model、Controller／Use Case、案件による明示override |
| 主対象候補 | `koiki-starter-observability`または別leaf、`koiki-starter-data-jpa`候補 |
| 受入対象 | `build-support/runtime-foundation-consumer` |
| Public API | 原則追加しない。必要性が実証された場合だけ型単位でOwner Reviewする |

候補module名や責務は実行計画のGate 1候補であり、CP6開始だけを理由に空moduleや独自Java APIを先行生成しない。

## 4. 完了済みbaseline

### 4.1 CP4 Data Runtime

- persistence-neutralな`koiki-starter-data`でKOIKI→Customer二階層Flywayを構成済み。
- Customer-like ConsumerはSpring Data JPA／PostgreSQL 17を使用し、HTTP→Use Case→Repository→DBを実証済み。
- OSIV無効化はCP6へ意図的に残している。
- CP4隔離scriptは`verify-cp4-data-runtime.ps1`、検証正本は
  `docs/architecture/validation/phase1b-cp4-data-runtime.md`である。

### 4.2 CP5 Observability

- `koiki-starter-observability`で構造化JSON log、`X-Request-ID`、`requestId`の`@Async`伝播を構成済み。
- CP5隔離scriptで空のMaven repositoryへ9 projectsをstageした。
- 細粒度fixture 27 tests、Consumer 17 tests、PostgreSQL 17.11、artifact／Public API／dependency境界が
  failure、error、skip 0で成功した。
- 検証正本は`docs/architecture/validation/phase1b-cp5-observability.md`である。

CP6はこのbaselineを壊さず、healthとJPA Web境界を追加する。

## 5. CP6の目的と完了条件

実行計画とグランドデザインのDoD 1b-5／1b-6に基づき、少なくとも次を満たす。

1. Actuator health endpointが実Consumerで応答する。
2. PostgreSQL接続可能時と接続不能時でDB healthの状態が変わる。
3. liveness、readiness、DBの分類と、HTTP status／公開detail範囲を記録する。
4. `spring.jpa.open-in-view=false`をFrameworkの上書き可能な低優先度既定として明示する。
5. Application overrideが可能であることと、`true`へ戻す場合のEntity露出検出低下riskを記録する。
6. OSIV falseの状態でJPA Entity／業務モデルをWeb境界へ漏らすtest-only負例が失敗を検出する。
7. 正常なControllerはDTO／read modelを返し、既存HTTP→DB経路が成功する。
8. Framework内部型、業務Entity、health専用fixtureをPublic APIやproduction artifactへ露出しない。
9. CP4／CP5を含む隔離一括検証、依存境界、cleanupを成功させる。

## 6. 実装前に確定する設計判断

### 6.1 Actuatorのartifact配置

次を比較し、Customer-like Consumerの最小fixtureが必要性を示した案だけを採用する。

1. 既存`koiki-starter-observability`へActuator基本設定を追加する。
2. 運用責務を分離した新しいFramework leafを追加する。
3. ConsumerがBoot Actuatorへ直接依存し、KOIKI固有既定が不要かを実証する。

healthのためだけにKOIKI独自Health Java APIや架空のHealthIndicatorを作らない。Spring Boot標準の
DataSource healthを第一候補とし、独自indicatorは標準機能でDoDを満たせない証拠がある場合だけ検討する。

### 6.2 health公開contract

次を起動前に固定せず、Spring Boot 4.1.1の公式仕様と実Consumer結果で決める。

- `/actuator/health`、liveness、readinessのどこへDB状態を含めるか
- DB `DOWN`時のHTTP status
- anonymous endpointに公開するdetail、component名、例外messageの範囲
- DB障害をreadiness失敗へ結び付けるか。livenessへは安易に結び付けない
- Customerが設定overrideする場合の安全境界

秘密値、JDBC URL、credential、SQL、stack traceをhealth responseへ露出しない。

### 6.3 JPA専用Starterの必要性

`koiki-starter-data`はJPA／MyBatis／JdbcClientから利用可能なpersistence-neutral leafであるため、JPA固有の
OSIV既定を混在させない。実行計画候補どおり`koiki-starter-data-jpa`を追加する案と、Boot設定だけで十分な案を
細粒度fixtureとConsumerで比較する。Starterを追加する場合もPublic Java API 0を第一候補とする。

### 6.4 Entity露出負例

- 不正なControllerやfailure switchをConsumerのproduction sourceへ追加しない。
- test-only fixtureでlazy associationを持つEntityをWeb境界へ渡し、OSIV falseで描画またはserializationまで
  実行して違反を検出する。
- HTTP statusやmodel属性だけで終了せず、実際のresponse生成まで検証する。
- OSIV trueなら不正経路が動き得ることをoverride riskとして対比するが、通常試験の既定はfalseを維持する。
- `LazyInitializationException`という実装例外名だけへ過度に固定せず、Web境界違反を確実に検出した証拠を残す。

## 7. 検証案

### 7.1 細粒度fixture

- health／OSIV既定値
- Application override
- 全体または機能単位の無効化／back-offが必要か
- Actuator／JPA classpath absence時の挙動
- Public API inventoryとStarter JAR内部化

### 7.2 Customer-like Consumer

- PostgreSQL available: health `UP`、正常HTTP→DTO→DB経路成功
- PostgreSQL unavailable: DB healthが`DOWN`へ変化し、秘密情報を露出しない
- PostgreSQL restore: 同じ検証単位または隔離再起動で正常状態へ復帰
- OSIV falseを実Environmentから確認
- test-only Entity露出負例をresponse生成まで実行
- `RestTestClient`でlive serverを確認し、reactive依存を追加しない
- CP4 migration／transactionとCP5 structured log／async correlationを回帰

### 7.3 隔離一括検証

CP6用scriptは`build-support/runtime-foundation-verification/verify-cp6-health-osiv.ps1`を候補とする。
空のMaven repositoryへのrelease unit stage、細粒度fixture、Consumer全試験、artifact、Public API、runtime
dependency、internal参照、Testcontainers cleanupを一括確認する。実測件数と時間はCP6 validationへ記録する。

## 8. CP6で行わないこと

- Spring Security、認証／認可、SecurityContext、監査
- Prometheus、OpenTelemetry exporter、cloud監視backendの固定
- Customer固有の外部依存HealthIndicator
- 正式Reference業務、MVC／HTMX画面、SPA
- MyBatis Starter、Mapper、`SEPARATED` model
- Spring Modulith Level 1／2、Named Interface、非同期Domain Event
- CP7のDomain Event判断／MyBatis BOM管理
- CP8以降の単一実行、性能harness、正式release
- production sourceに置くEntity露出endpointまたはDB停止switch

## 9. Stop conditions

- ActuatorまたはJPA責務のownerを決めずに新leafを追加する。
- Spring Boot標準機能で足りるのにKOIKI独自Health APIを追加する。
- DB down試験が他試験の共有containerを破壊し、restore／cleanupを保証できない。
- health responseへcredential、接続先、例外detail等が露出する。
- Entity露出負例をproduction sourceへ入れる、または正常Customer経路がEntityを返す。
- OSIV falseだけをproperty assertionし、response生成時の負例を実証しない。
- 細粒度fixtureだけで完了し、実Consumerのhealth／DB up-downを確認しない。
- WebFlux／Reactor、Security、MyBatis、Modulith runtime等の保留依存を持ち込む。
- 実装証拠がグランドデザインまたは既存ADRの前提を否定する。

該当時は実装を正当化せず、証拠、代替案、影響範囲をOwnerへ提示して判断を待つ。

## 10. 次回セッションの開始手順

```powershell
git fetch origin
git switch feature/phase1b-data-runtime-integration
git pull --ff-only
git status --short --branch
git log -5 --oneline
java -version
.\mvnw.cmd -version
```

期待状態:

- branchが`feature/phase1b-data-runtime-integration`
- upstreamが`origin/feature/phase1b-data-runtime-integration`
- 履歴にCP5 commit `547e5b7`とこの引継ぎcommitが存在する
- worktreeがclean
- Java 21とMaven Wrapper 3.9.16を実行可能
- CP6のproduction／test差分はまだ存在しない

Docker／PostgreSQLはCP6の設計確認後、実装開始時にpreflightする。引継ぎ確認だけを理由に全隔離scriptを
再実行する必要はないが、環境差または依存cache不整合がある場合はCP5 scriptをbaselineとして再実行する。

## 11. 次回最初に行うこと

1. Git、Java、Maven、Dockerの実効状態を確認する。
2. Spring Boot 4.1.1のActuator health／probe／DB indicatorとOSIV既定を公式資料・実効依存で確認する。
3. Actuatorのartifact配置3案と`koiki-starter-data-jpa`追加要否をOwnership／依存方向で比較する。
4. DB up／down／restoreを他試験から隔離するTestcontainers構成を決める。
5. production sourceを汚さないEntity露出負例を設計する。
6. 細粒度fixture、Consumer acceptance、隔離script、validation記録の具体的な完了条件をOwnerへ提示する。
7. 方針確認後にCP6実装へ進む。

## 12. 新規セッションへの開始依頼文

次の文章を、新しいセッションの最初の依頼として使用できる。

> KOIKI-JavaWeb-FWのPhase 1b CP6を開始します。Repository rootの`AGENTS.md`、
> `docs/agent/skills/koiki-project-overview/SKILL.md`、
> `docs/development/phase1b-cp6-start-handoff-20260828.md`、Phase 1b実行計画、グランドデザインの
> health／OSIV節、CP4／CP5 validationを確認してください。作業branchは
> `feature/phase1b-data-runtime-integration`、CP6 baselineはCP5 commit `547e5b7`です。
> 最初にGit／Java／Maven／Docker状態を確認し、Actuatorのartifact配置、DB healthの公開contract、
> `koiki-starter-data-jpa`追加要否、DB up／down／restoreおよびtest-only Entity露出負例の検証方式を
> Ownership境界とともに提示してください。承認前にCP6実装やCP7以降へ進めないでください。

## 13. 引継ぎ時点の判定

- CP5はcommit `547e5b7`でLOCAL COMPLETE。
- CP5隔離一括検証は成功済みで、remote CIはCP7後のMilestone B PRまでPENDING。
- CP6はSTART READYだが、Actuator配置、health公開contract、JPA Starterおよび負例方式は未決定。
- この文書はCP6の実装許可、設計承認または完了判定ではない。

