# Walking Skeleton — Tier 2 Practicality Validation

**Status:** V4 / V5 / V6 Completed

## Result

| ID | 検証 | 実結果 | 判断 |
|---|---|---|---|
| V4 | Tier 2のクラス数と記述量 | `expense`は本番13 Javaソース単位（通常型12、`package-info.java` 1）で、手書きRepository Adapterや未使用Packageを必要としなかった | PASS |
| V5 | OSIV無効化によるEntity露出検出 | Rule 19が代表違反を検出し、実レンダリングも`LazyInitializationException`を原因として失敗した。一方、正規View経路は正常描画できた | PASS |
| V6 | 同期イベント連携のテスト容易性 | PostgreSQL統合テストで1回の同期処理、成功時commit、`Draft` / `Submitted`参照時rollbackを確認できた | PASS |
| WS-T01 | KOIKI Phase 0規約 | Walking Skeletonの本番classへ全Phase 0 ruleを適用し、違反なし | PASS WITH RULE FIX |
| WS-T02 | Spring Modulith module検証 | `ApplicationModules.of(...).verify()`が循環、不正依存、非公開型参照を報告しなかった | PASS |
| WS-T03 | Spring Modulith公開境界 | `masterdata::events`が`CategoryDeactivating`だけを公開し、ApplicationとPersistenceの型を公開しないことを確認した | PASS |
| WS-T04 | Java 21リアクター再現性 | Java 21.0.12で9 moduleのRoot Reactorが成功した | PASS |

## Validation Scope

対象は`walking-skeleton/ws-tier2-practicality`に隔離した使い捨て実験である。正式な
FrameworkまたはReference Applicationではなく、JavaクラスやSQLをPhase 1へ直接昇格させない。

- `expense`: Tier 2 RICH、JPA共有モデル
- `masterdata`: Tier 1 SIMPLE、JPA
- UI: Spring MVC + Thymeleaf、OSIV無効
- module連携: 値のみの同期Domain Event
- database: Testcontainers PostgreSQL 17
- 構造検証: KOIKI ArchUnit Phase 0 rule + Spring Modulith

REST API、React SPA、非同期イベント、Spring Modulith Level 2、MyBatis、Tier 2分離永続化、
認証・認可・監査は検証対象外である。

## Tested Configuration

| Item | Effective version / setting |
|---|---|
| Java | Temurin 21.0.12、`release=21` |
| Maven | 3.9.16 |
| Spring Boot | 4.1.0 |
| Spring Framework | 7.0.8 |
| Spring Data JPA | 4.1.0 |
| Hibernate ORM | 7.4.1.Final |
| Spring Modulith | 2.1.0 |
| Flyway | 12.4.0 |
| Testcontainers | 2.0.5 |
| PostgreSQL image | `postgres:17-alpine` |
| OSIV | `spring.jpa.open-in-view=false` |

Spring Boot BOMを先に、Spring Modulith BOMを後にimportする構成で、実効依存関係の
Spring Frameworkは7.0.8へ統一され、6.2系の混在はなかった。Spring Modulith 2.1.0の
正式採否はこの試行だけでは固定せず、Phase 1bで対象Spring Boot lineとの組合せを再評価する。

## Class Count

計測単位は`.java`ファイル1件を1 Javaソース単位とする。アーキテクチャ宣言も実装コストなので
`package-info.java`を本番数の内数に含め、通常型との区別が分かるよう別記した。nested recordは
独立ソース単位として重複加算しない。

| Ownership area | Production Java | うち`package-info` | Test Java | 主な責務 |
|---|---:|---:|---:|---|
| `expense` | 13 | 1 | 2 | 集約、Repository契約、Use Case、Query/View、MVC、event listener |
| `masterdata` | 6 | 2 | 1 | Category永続化、無効化Use Case、公開event契約 |
| application common | 2 | 1 | 3 | Boot root、統合検証、KOIKI/Modulith検証 |
| validation fixture | 0 | 0 | 4 | Rule 19負例、Lazy Loading失敗経路 |
| **Total** | **21** | **4** | **10** |  |

再計測は次の分類で行う。

```text
production: src/main/java/org/koikifw/walkingskeleton/tier2/**/*.java
test:       src/test/java/org/koikifw/walkingskeleton/**/*.java
expense:    /tier2/expense/
masterdata: /tier2/masterdata/
fixture:    /fixture/ または /tier2test/
common:     上記以外
```

検証専用またはFramework連携のために追加した主なソースは次のとおりである。

- `ApplicationModuleVerificationTest`: KOIKI Phase 0 ruleとSpring Modulithの検証
- `ExpenseViewBoundaryArchitectureTest`と`fixture/v5`の2型: Rule 19の負例
- `tier2test/mvc`の2型とtest template: OSIV無効時の実レンダリング失敗経路
- `Tier2PracticalityInfrastructureTest`: PostgreSQL、Flyway、JPA、MVC、同期transactionの統合証拠
- 各moduleの`package-info.java`: KOIKI Tier宣言とSpring Modulith公開境界

## V4 Finding — Tier 2 Practicality

`expense`は、状態遷移と不変条件を持つJPA共有集約、Spring Dataが実装するRepository契約、
Application Use Case、同期event listener、DTO/read model、およびMVC Controllerを13ソース単位で
表現できた。複雑query用Adapter、Domain Service、Gateway、分離永続化モデルは必要にならなかった。

View用の変換処理と同期event受信によりTier 1より型は増えるが、責務ごとの分離は明瞭であり、
今回の小規模なRICH moduleでは実務上許容できる。V4はPASSとし、Tier 2構造を簡素化する
必要は認めない。ただし、13という数値を全moduleの目標値や上限にはしない。

## V5 Finding — OSIV and View Boundary

V5は次の三層で検証した。

1. `spring.jpa.open-in-view=false`を通常設定として固定する。
2. KOIKI Rule 19が、Use Case由来のJPA EntityをMVC Modelへ格納する代表違反fixtureを検出する。
3. test専用Controllerが未初期化のLazy明細を持つdetached EntityをTemplateへ渡すと、原因連鎖に
   `LazyInitializationException`を含んでレンダリングが失敗する。

正規経路はtransaction内で集約と明細を`ExpenseDetailView`へ変換し、EntityをMVC Modelへ
格納せず正常にレンダリングできた。したがってV5はPASSとし、OSIV無効化とRule 19、
レンダリングテストを組み合わせる§13.3.3 / §22.1の方針を維持する。

Rule 19は完全なdata-flow解析ではないため、複雑なhelperや型消去を経由する露出はReviewと
実レンダリングテストで補完する。

## V6 Finding — Synchronous Event Collaboration

`masterdata`はCategoryを無効化する前に、category IDだけを持つ`CategoryDeactivating`を同期発行する。
`expense.adapter.inbound.event`の薄いlistenerは自身のApplication Use Caseへ委譲し、`Draft`または
`Submitted`から参照されていれば例外を送出する。

Testcontainers PostgreSQLで次を確認した。

- 未処理経費から参照されていなければeventは1回処理され、Categoryは無効状態でcommitされる。
- `Draft`から参照されていれば受信側の拒否が伝播し、Categoryは有効状態へrollbackされる。
- `Submitted`から参照されている場合も同じrollbackとなる。
- `@TransactionalEventListener`、非同期実行、失敗注入用スイッチは不要だった。

業務成立条件としてFail Closedにする連携は自然に記述・検証できたためV6はPASSとし、§17.3の
「同期を既定とする」判断を維持する。外部I/Oや派生処理へ同じ判断を一般化しない。

## Spring Modulith Public Boundary

`masterdata.domain.event`へ`@NamedInterface("events")`を付け、`expense`の許可依存を
`masterdata::events`へ限定した。Spring Modulithが構築したmodule modelに対して次を確認した。

- `ApplicationModules.of(Tier2PracticalityApplication.class).verify()`が成功する。
- `events` Named Interfaceは`CategoryDeactivating`を含む。
- `CategoryDeactivating`は`masterdata`の公開型として認識される。
- `DeactivateCategoryUseCase`と永続化型`Category`は公開型として認識されない。

KOIKIの`domain.event`規約とSpring Modulithの公開モデルはNamed Interfaceで整合できた。一方、
本番compile scopeへ`spring-modulith-api`が必要になるため、正式規約への採用はPhase 1bで判断する。

## Implementation Friction and Constraints

### Rule 11 package-info false positive

`domain.event/package-info.class`をArchUnitが通常classとしてimportし、Rule 11が「recordではない
Domain Event」と誤検出した。Rule 11からsimple nameが`package-info`の型だけを除外し、
注釈付きcompliant fixtureを加えた。非record eventとDomain Model fieldの検出は維持している。

この修正により、KOIKI規約のための`domain.event`配置とSpring Modulithの`@NamedInterface`を
同じpackageで安全に併用できる。

### Tool and runtime conditions

- Java 21、Maven 3.9.16、Docker互換runtimeが必要である。
- PostgreSQL統合テストはTestcontainersを利用するため、Docker daemonへ接続できる必要がある。
- 依存関係またはToolchainsの追加調整なしにRoot Reactorを実行できた。
- agent subprocessではfnm由来PATHが常に継承されないため、OpenSpec CLIは解決済みの絶対pathで
  実行した。VS Codeの通常Terminalでは`openspec`を直接実行できることを別途確認済みである。

## OpenSpec Evaluation

OpenSpecの`proposal.md`、4 delta specs、`design.md`、`tasks.md`を日本語の単一正本として使用し、
1.1から7.4まで実装と証拠を段階的に照合できた。設計対象外のREST API / React SPAをNon-goalsへ
明記でき、実装中に発見したRule 11の誤検出もchangeの目的を変えずに修正・回帰検証できた。

責務の関係は次のとおりで、現時点の競合はない。

| Guidance | Responsibility |
|---|---|
| ルート`AGENTS.md` | Repository全体のPhase 0統治、所有権、Walking Skeletonの扱い |
| `openspec/config.yaml` | OpenSpec成果物の日本語方針と、この試行changeの共通context |
| change artifacts | `expense-tier2-walking-skeleton`固有の要求、設計、実装タスク |
| `.agents/skills/openspec-*` | OpenSpec CLI操作workflow |
| `docs/agent/skills/` | KOIKI固有の横断的な設計判断と参照先 |

`docs/agent/skills/`には要求一覧や実装手順を複製せず、ADRに基づく判断と参照先だけを置く。
OpenSpecはchange単位の計画正本、`AGENTS.md`はRepository統治、Skillは反復判断の導線とすれば
衝突を避けられる。最小Skill 2件とCodex / Claude Codeの導線を作成し、実ファイル同士の照合と
双方のスラッシュコマンドからの呼び出しを確認した。詳細は`walking-skeleton-agent-skills.md`に記録する。

OpenSpecの提案から検証、4 capabilityのmain spec同期、およびarchiveまで一貫して実行できた。
2026-08-13にchangeを`openspec/changes/archive/2026-08-13-expense-tier2-walking-skeleton/`へ
移動し、同期後の全main specがvalidationを通過したため、このworkflowは実務に適用可能と判断する。

## Evidence Mapping

| OpenSpec tasks | Evidence |
|---|---|
| 1.1〜1.4 | module POM、application設定、Flyway migration、`startsApplicationContextWithOsivDisabled`、`appliesApplicationOwnedFlywayMigration` |
| 2.1〜2.6 | `ExpenseRequestTest`、`applicationUseCasePersistsLegalLifecycleTransitions`、JPA Lazy再取得テスト |
| 3.1〜3.4 | `DeactivateCategoryUseCaseTest`、module metadata / Named Interface検査 |
| 4.1〜4.5 | listener unit test、event 1回処理、`Draft` / `Submitted` rollback統合テスト |
| 5.1〜5.4 | 正規MVC描画、Rule 19負例、detached EntityのLazy描画失敗テスト |
| 6.1〜6.3 | `ApplicationModuleVerificationTest`のKOIKI / Modulith / 公開型検査 |
| 6.4 | Java 21.0.12によるRoot Reactor 9 moduleの`mvn test`成功 |
| 7.1 | 本文書のClass Count |
| 7.2 | 本文書のV4 / V5 / V6、公開境界、摩擦、制約、引継ぎ判断 |
| 7.3 | Walking Skeleton実装計画のチェックリスト更新と本文書のOpenSpec Evaluation |
| 7.4 | `openspec validate expense-tier2-walking-skeleton --strict`と全taskの証拠照合 |

## Phase 1 Handoff Decision

- Tier 2 RICH + JPA共有モデルを既定候補として維持する。
- OSIV無効化を維持し、DTO/read model、ArchUnit近似検査、実レンダリングテストを併用する。
- 業務成立条件となるmodule間eventでは同期を既定候補として維持する。
- `domain.event`のNamed Interface方式は有効な候補だが、Spring Modulith versionとともに
  Phase 1bで正式採用を判断する。
- Rule 11の`package-info`除外とその回帰fixtureはArchUnit規約実装の知見として引き継ぐ。
- Maven設定、依存関係の実効version、検証テストの構成、本文書の判断を引き継ぐ。
- `ws-tier2-practicality`のJavaクラス、Template、migrationは正式コードへ直接移植しない。
