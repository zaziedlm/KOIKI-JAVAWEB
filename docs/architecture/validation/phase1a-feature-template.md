# Phase 1a Feature Template — B1実効検証

**検証日:** 2026年8月24日<br>
**対象branch:** `feature/phase1a-feature-template`<br>
**状態:** IMPLEMENTED / LOCAL PASS<br>
**B1 status:** OWNER IMPLEMENTATION REVIEW<br>
**Architecture Owner:** Shuichi Kataoka<br>
**設計承認日:** 2026年8月24日<br>
**Ownership:** Tooling<br>
**対象:** B1 Tier 1 SIMPLE / Tier 2 RICH Feature Template

## 1. 現時点の結論

Phase 1aのTier構造検証に限定した2種類のFeature Templateを、Walking Skeletonのcodeをコピーせず
正式package、Architecture ContractおよびParent / BOMを使って再実装した。

- Tier 1はDomain Modelを生成せず、単純な業務判断をApplication Use Caseへ置く。
- Tier 2はJPA兼用Domain Modelへ3状態の遷移規則を置く。
- 未使用のController、DTO、Domain Service、Event、Gateway、Query Port、Configurationを生成しない。
- Templateと生成ツールはTooling所有とし、Root Reactorと正式release unitへ含めない。
- Customer相当の検証Reactorへ両Tierを生成し、Java 21で`clean verify`が成功した。
- Spring Modulith 2.1.0 Level 0をtest scopeだけで実行し、runtime dependencyへ混入しなかった。

GitHub ActionsのWindows / Ubuntu jobへ同じ検証commandを追加した。remote CI結果とOwnerによる
実装Reviewが完了するまでは、本書の状態を`IMPLEMENTED / LOCAL PASS`、B1を
`OWNER IMPLEMENTATION REVIEW`とする。

## 2. Owner Review済み設計判断

2026年8月24日のB1設計Reviewで、Architecture Ownerは次を承認した。

| 観点 | 承認内容 |
|---|---|
| Ownership | Feature Templateと生成・検証資材はTooling所有 |
| 生成物Ownership | Customer向け生成物はCustomer、Reference向け生成物はReferenceが所有。検証用生成物だけはTooling fixture |
| 配置 | `build-support/feature-templates/` |
| 配布境界 | Maven artifact化せず、Root ReactorおよびPhase 1a release unitへ含めない |
| Template境界 | 既存Applicationへ業務moduleを追加する。Application全体を生成するProject Templateではない |
| Tier 1 | JPA / SHARED。Domain Model、Port、Gatewayを生成しない |
| Tier 2 | JPA / SHARED。Domain Modelが不変条件と状態遷移を所有する |
| 生成方式 | JDK 21 source-file modeで動く単一生成ツールとplain template |
| Level 0 | Customer相当Applicationのtestから両moduleをまとめて検証する |

新しいFramework Public API、Maven coordinatesまたはADRを必要としないため、B1固有ADRは追加しない。

## 3. TemplateとProject Templateの境界

| Feature Template — B1 | Project Template — Phase 5 |
|---|---|
| 既存Applicationへ1つの業務Maven moduleを追加する | 顧客RepositoryとApplication全体を生成する |
| Tierと内部責務の最小構造を扱う | Web方式、Security、runtime設定、CI、運用構成を扱う |
| ApplicationのAggregator POMは利用者が明示的に更新する | Project全体の初期構成を所有する |
| migration、Controller、画面、APIを生成しない | Phase 5で承認した構成だけを生成する |

生成ツールは既存POM、業務code、migrationおよび設定を自動変更しない。これによりB1がPhase 5の
Project Templateへ拡張されることを防ぐ。

Feature Template、生成ツールおよびRepository内の検証fixtureはToolingが継続して所有する。
生成された業務moduleは生成先Applicationの成果物であり、Customer ApplicationではCustomer、
Reference ApplicationではReferenceが所有する。B1資材をPhase 5のProject Templateから内部利用するか、
再設計するか、または正式配布するかはPhase 5で判断し、B1から先行固定しない。

## 4. 生成構成

### 4.1 Tier 1 SIMPLE

```text
org.<owner>.<application>.<module>/
├── package-info.java
├── application/usecase/Create<Feature>UseCase.java
└── adapter/outbound/persistence/
    ├── <Feature>.java
    └── <Feature>Repository.java
```

- `package-info.java`は`@KoikiModule(SIMPLE, JPA, SHARED)`と`@NullMarked`を併記する。
- persistence modelは状態遷移を持たない。
- 空文字を拒否する最小業務判断をApplication Use Caseへ置く。
- Spring Data RepositoryをApplication Use Caseから直接利用し、追加Portを重ねない。

### 4.2 Tier 2 RICH

```text
org.<owner>.<application>.<module>/
├── package-info.java
├── application/usecase/Submit<Feature>UseCase.java
└── domain/
    ├── model/<Feature>.java
    └── repository/<Feature>Repository.java
```

- `package-info.java`は`@KoikiModule(RICH, JPA, SHARED)`と`@NullMarked`を併記する。
- JPA兼用Domain Modelが`DRAFT -> SUBMITTED -> APPROVED`の状態遷移を所有する。
- public setterを持たず、意味のある業務methodだけで状態を変更する。
- 識別子ベースの`equals` / `hashCode`を持ち、`getClass()`による型比較を使用しない。
- Domain所有Repositoryは`Repository<T, ID>`を継承し、`JpaRepository`を公開しない。
- Domain Service、Event、Gatewayは利用していないため生成しない。

### 4.3 Gate 3 Owner Review結果

| 項目 | 判断 |
|---|---|
| Decision | ACCEPTED WITH CONDITIONS SATISFIED |
| Decided by | Shuichi Kataoka |
| Date | 2026年8月24日 |
| Tier 1 | Domain層なし、Application業務判断、Spring Data Repository直接利用を承認 |
| Tier 2 | JPA兼用Domain Model、Domain所有Repository、状態遷移の配置を承認 |
| Entity identity | 識別子ベースかつ`getClass()`を使わない`equals` / `hashCode`とproxy相当testを追加 |
| サンプル業務 | `label`と3状態遷移は責務配置の実証用であり、生成先の業務要件で置換する |
| Runtime境界 | Spring Bean、transaction、runtime ConfigurationはB1で生成せず、適用Phaseの承認構成に従う |

Ownerは、Templateが業務語彙を標準化するものではなく、Tier別の責務配置を実証するコンパイル可能な
サンプルであることを確認した。Tier 2のJPA兼用Domain Modelへグランドデザイン§11.6規約7を反映し、
runtime成果物をPhase 1aへ先行実装しない境界を維持する。

## 5. 生成・利用契約

`GenerateFeature.java`はJDK 21のsource-file modeで動作し、次を入力とする。

- Tier
- module名とclass名
- base package
- Maven artifactId
- 既存Application parentのcoordinates、relative path
- 新規出力先

module名、package、class名およびMaven coordinatesを形式検査する。出力先が既に存在する場合は
失敗し、既存資材を上書きしない。生成後のAggregator POM登録は利用者が明示的に行う。

### 5.1 Gate 2 Owner Review結果

| 項目 | 判断 |
|---|---|
| Decision | ACCEPTED WITH DOCUMENTED LIMITATIONS |
| Decided by | Shuichi Kataoka |
| Date | 2026年8月24日 |
| 未知option | 現実装は拒否せず無視する。READMEの許可optionだけを使用する |
| 部分出力 | 中断またはI/O failureで残る可能性がある。利用者が対象を確認して指定出力先だけを除去する |
| 実装修正 | B1では行わない |
| 再検討時期 | 正式な顧客向け生成ツールとしての実運用を判断するPhase 5 |

Ownerは、既存出力を上書きしないこと、入力値と生成pathを制限していること、およびB1が非配布の
内部Toolingであることを確認した。未知optionの拒否と一時directory経由のall-or-nothing生成は
必要性を認識した既知制約として記録し、B1の直接実装修正を行わずPhase 5の運用設計へ保留する。

READMEには全option、PowerShell / Bash例、既知制約および部分出力が残った場合の再実行手順を記載する。

## 6. Repository内検証経路

```text
KOIKI Dependencies BOM
KOIKI Architecture Contract
Feature Template Verification parent
├── generated/catalog       Tier 1 SIMPLE
├── generated/approval      Tier 2 RICH
└── architecture-tests      Spring Modulith Level 0
```

正式snapshot公開前のB1検証であるため、検証ReactorはRepository内の正式BOMとContractをmoduleとして
含める。local Maven Repositoryへの`install`には依存しない。公開snapshotだけから解決する
Repository外Consumer検証はC1 / C2で行う。

実行command:

```powershell
pwsh -NoProfile -File build-support/feature-templates/verify-feature-templates.ps1
```

このcommandは既存の検証専用`generated/`だけを再生成し、次を順に実行する。

1. Tier 1 `catalog` module生成
2. Tier 2 `approval` module生成
3. 6-project Reactorの`clean verify`
4. `architecture-tests`のruntime dependency tree生成
5. Spring Modulith runtime依存が存在しないことの検査

## 7. Local test結果

| 対象 | 結果 |
|---|---|
| Architecture Contract | 4 tests、failure 0、error 0、skipped 0 |
| Tier 1 Application Use Case | 2 tests、failure 0、error 0、skipped 0 |
| Tier 2 Application Use Case | 2 tests、failure 0、error 0、skipped 0 |
| Tier 2 Domain Model | 3 tests、failure 0、error 0、skipped 0 |
| Spring Modulith Level 0 | 1 test、failure 0、error 0、skipped 0 |
| Reactor | 6 projects、BUILD SUCCESS |
| 正式Root Reactor回帰 | 4 projects、BUILD SUCCESS |
| Java build | Enforcer / ToolchainsともJDK 21でPASS |
| runtime dependency | `org.springframework.modulith`なし |

Spring Modulithの検証rootはtest classへ`@Modulithic`を付与している。Modulith APIをproduction
sourceから参照せず、`spring-modulith-starter-test`をtest scopeだけに保つ。

### 7.1 Gate 4 Owner Review結果

| 項目 | 判断 |
|---|---|
| Decision | ACCEPTED WITH CONDITIONS SATISFIED |
| Decided by | Shuichi Kataoka |
| Date | 2026年8月24日 |
| Maven fixture | 正式BOM / Architecture Contractを含む6-project ReactorとJDK 21 buildを承認 |
| Level 0 | `catalog`と`approval`の両module発見を明示assertしたうえで`verify()`する |
| Tier tests | Tier 1 Application、Tier 2 Application、Tier 2 Domainの成功・拒否経路を承認 |
| Runtime依存 | Spring Modulithをtest scopeに限定し、runtime dependency treeに存在しないことを承認 |

Owner Reviewで指摘したLevel 0のmodule発見証拠とTier 2 Application Use Caseの直接testを追加し、
同じ検証commandの成功を確認した。これによりGate 4の承認条件は充足し、Gate 4をクローズする。

## 8. 実装中に得た知見

### 8.1 別ReactorでのBOM解決

Parent POMへの相対参照だけでは、未公開のBOMが管理するArchitecture Contractのversionを別Reactorへ
供給できなかった。B1のRepository内検証Reactorへ正式BOMとContractを明示的に含めることで、
local installを使わず解決した。外部配布成立の証拠とは表現せず、C1 / C2へ保留する。

### 8.2 Spring Modulith検証root

`ApplicationModules.of(Class<?>)`へ渡すrootは`@Modulith`、`@Modulithic`または
`@SpringBootApplication`で明示する必要がある。B1ではruntime依存を追加しないため、
検証専用test classへ`@Modulithic`を付与した。

## 9. CI適用

既存の`Verify (windows-2025)`と`Verify (ubuntu-24.04)`へ、正式Reactorの`clean verify`後に
同じ`verify-feature-templates.ps1`を実行するstepを追加した。GitHub hosted runnerでの結果は、
branch push後に本書へ追記する。

## 10. Deferred

- B2: ArchUnit Public APIとrule matrixの設計具体化
- B3: `koiki-archunit-rules`、compliant / negative fixture、必須5違反
- B4: `@NullMarked`適用方針とNullAwayの正常・負例・復元検証
- B5: 両TemplateへのArchUnit、NullAway、Level 0の最終統合検証
- C1 / C2: snapshot公開とRepository外Consumerによる独立解決
- Phase 1b以降: Runtime、Flyway Starter、Web、Security
- 実Applicationへの統合時: Application Use CaseのSpring Bean登録、transaction境界、runtime Configuration
- Phase 3以降: Reference業務、MVC / REST、Spring Modulith Level 1
- Phase 4以降: MyBatis詳細Template、分離model、非同期、Level 2
- Phase 5: Project Template、B1資材の内部利用・再設計・正式配布の判断
- Phase 5: 生成CLIの未知option拒否、利用者向けerror、all-or-nothing生成、自動cleanup
