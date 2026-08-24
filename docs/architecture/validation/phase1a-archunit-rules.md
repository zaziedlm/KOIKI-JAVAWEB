# Phase 1a ArchUnit Rules — B3実装・検証計画

**準備日:** 2026年8月24日<br>
**対象branch:** `feature/phase1a-archunit-rules`<br>
**状態:** IMPLEMENTATION PLAN DRAFT / IMPLEMENTATION NOT STARTED<br>
**Ownership:** Tooling<br>
**対象artifact:** `org.koikifw:koiki-archunit-rules:0.1.0-SNAPSHOT`<br>
**開始baseline:** `main` / `b460b52`（B2 PR #9 merge）

## 1. 目的と完了条件

B2でOwner承認したPublic API、25 failure rule＋2許容predicate、message contract、dependency、fixtureおよび
Rule 19制約を、正式Maven artifactとして再実装する。Walking Skeletonのsourceをコピーせず、正式packageへ
実装し直し、Repository内のMaven buildとCIでpositive / negative evidenceを取得する。

B3は次をすべて満たしたときだけCOMPLETEとする。

- `KoikiArchitectureRules`の1 public class / 2 public static methodだけを提供する。
- 25 failure ruleを個別の`KOIKI-ARCH-nnn`とfocused testへ一対一に対応させる。
- Rule 10 / 23を許容predicateとして実装し、独立した常時成功ruleを作らない。
- compliant fixtureの6 positive testと必須5負例が承認済み契約どおり動作する。
- Rule 19の代表違反、DTO変換正常経路、保証外経路を実証する。
- production dependency boundary、module単体／Root Reactor、CIの証拠を本書へ記録する。

## 2. 正本と作業境界

| 対象 | 正本／扱い |
|---|---|
| Public API、入力、matrix、message、fixture、stop条件 | `phase1a-archunit-api-design.md`（B2 COMPLETE） |
| 39規則の上位定義 | グランドデザイン§21.3 |
| Phase 1a WPとDoD | Phase 1a実行計画§6.8、§7.2、§8 |
| Architecture Contract | `koiki-architecture-contract`の実装とB1 / B2承認内容 |
| Walking Skeleton | 成立性の証拠として参照するが、source、test、Maven座標をコピーしない |
| B3 evidence | 実装後に本書へcommand、結果、test、dependency、CIを追記する |

B3ではRuntime、Spring Starter、Reference業務、Named Interface、Level 1 / 2、MyBatis詳細、Tier 2分離方式、
Repository外Consumer、snapshot公開またはjapicmp baselineを実装しない。C1 / C2 / C3、B4 / B5および後続Phaseの
成果を先行しない。

## 3. 実装順序

Public facadeを一部ruleだけで公開する途中状態を避けるため、package-private実装とfocused testを先に作り、
25 failure rule＋2許容predicateが揃った後に2つのPublic APIへ接続する。各段階はRoot Reactorでbuild可能な
状態を保つが、branch完了前のartifactを正式配布またはConsumerへ公開しない。

### Stage 1 — Maven moduleと内部基盤

- BOMへArchUnit 1.5.0とRules artifactのdependency managementを追加する。
- Root Reactorへ`koiki-archunit-rules`を追加し、`koiki-parent`を継承するPOMを作る。
- production直接依存3件、test fixture依存8件をB2どおり宣言する。
- `org.koikifw.archunit`を`@NullMarked`にする。
- package名検証、module metadata読取、rule message組立てをpackage-privateで実装する。
- null、Java package名、重複／包含、defensive copyのcontract testを先行作成する。
- 空moduleだけのcommitにせず、内部基盤とそのtestを同じ段階に含める。

### Stage 2 — 全Tier共通・Ownership・Event rule

- Rule 1〜13のfailure ruleとRule 10の許容predicateを実装する。
- `frameworkOwnershipRules`を構成するRule 5 / 13を独立して検証する。
- Rule 28、38、39を実装する。
- `@TransactionalEventListener`の直接／meta-annotation経路をRule 28で検出する。
- `@ApplicationModuleListener`のRule 28違反とRule 38配置selectorをfocused testで分離確認する。
- 対象failure ruleごとに単独rule ID、違反箇所、message contractを確認する。

### Stage 3 — Tier・MVC・Rule 19

- SIMPLEのRule 14を実装する。
- RICHのRule 15〜22、24とRule 23の許容predicateを実装する。
- JPA annotation／Spring Data Commonsの許容と、`JpaRepository`／`EntityManager`の拒否を分ける。
- MVC handler判定とRule 17〜20の重複failureを結合IDなしで実装する。
- Rule 19のsource / sink検出を実装し、代表違反とDTO変換正常経路を同時に成立させる。
- Rule 24はgateway interfaceと具象実装packageを区別する。

### Stage 4 — Public APIとfixture統合

- 全private ruleを2つの合成Public APIへ接続する。
- public constructor、個別rule method、public内部型が存在しないことを検証する。
- root discovery guardと任意責務のempty selectionを区別する。
- B2 §10のbusiness / ownership fixtureと6 positive testを実装する。
- B2 §11の必須5負例を相互に隔離して実装する。
- 25 failure reportすべてのID、根拠、影響、修正方針、違反箇所を検証する。

### Stage 5 — Build・CI・Validation

- Rules module単体をMaven Wrapperで`verify`する。
- Root Reactor全体をMaven Wrapperで`verify`する。
- `dependency:tree`で直接／推移依存とscopeを記録する。
- JARとclass inventoryからPublic API候補とpackage-private境界を確認する。
- test件数、rule traceability、raw failure report、command、JDK、commitを本書へ記録する。
- remote CIを確認し、Owner ReviewでGate 5とB3最終判定を行う。

## 4. Owner Review Gate

| Gate | Review対象 | 承認条件 | 初期状態 |
|---:|---|---|---|
| 1 | Maven module、BOM、dependency、内部基盤 | 空moduleでなく、scopeと入力基盤がB2契約どおり | REVIEW PENDING |
| 2 | Rule 1〜13、28、38〜39 | 共通・Ownership・Event ruleのfocused testとmessageが対応 | REVIEW PENDING |
| 3 | Rule 14〜24、Rule 19 | Tier / MVC、2許容predicate、Rule 19の正常・違反経路が成立 | REVIEW PENDING |
| 4 | Public API、compliant fixture、必須5負例 | 1 class / 2 method、6 positive、5独立negative、25 messageが成立 | REVIEW PENDING |
| 5 | Maven、dependency、CI、Validation、Deferred | Repository内証拠が揃い、B4 / B5 / C1以降へ境界を引き継げる | REVIEW PENDING |

Gateごとに実装差分と検証結果を区切ってOwner Reviewする。承認前にPublic API追加、scope拡張または
後続Phaseの実装が必要になった場合は、そのGateを進めずB2のstop / return conditionに従う。

## 5. Commit候補

| Commit | 内容 | message案 |
|---:|---|---|
| 1 | Maven module、BOM、Root Reactor、package-private内部基盤とcontract test | `build: add Phase 1a ArchUnit rules module` |
| 2 | 全Tier共通、Ownership、Event ruleとfocused test | `feat: implement common ArchUnit architecture rules` |
| 3 | Tier、MVC、Rule 19とfocused test | `feat: implement Tier and MVC ArchUnit rules` |
| 4 | Public facade、input contract、compliant fixture | `feat: expose composite ArchUnit rule API` |
| 5 | 必須5負例、25 message contract、rule traceability | `test: verify composite ArchUnit rule contracts` |
| 6 | dependency / CI evidence、Validation、B3 closeout | `docs: complete B3 ArchUnit rules validation` |

実装のまとまりが上表と異なる場合も、partial Public API、no-op rule、赤い通常buildまたは未検証ruleを
commit境界に残さない。

## 6. Verification matrix

| 区分 | 最低限の証拠 |
|---|---|
| Public API | 1 public class、2 public static method、constructor非公開、Javadoc |
| Input | null、blank、不正package、重複、包含、defensive copy、root未検出 |
| Failure rules | 25 ruleそれぞれのfocused negative testと単独ID |
| Allowances | Rule 10 / 23のfocused positive test |
| Compliant | B2で定義した6 positive test |
| Required negatives | DoD 1a-2の5独立fixture |
| Rule 19 | 代表違反、DTO変換正常、保証外の記録 |
| Message | 25件のID、根拠、影響、修正方針、具体的違反箇所 |
| Dependency | production直接3件、test fixture 8件、禁止scope混入なし |
| Build | module `verify`、Root Reactor `verify`、CI |

## 7. 本日の準備結果

| 項目 | 結果 |
|---|---|
| main baseline | `b460b52`、`origin/main`と同期、開始時worktree clean |
| B2 | PR #9 merge済み、B2 COMPLETE |
| B3 branch | `feature/phase1a-archunit-rules`をmainから作成 |
| B3 task | Stage 1〜5、Owner Review Gate 1〜5、Commit候補1〜6へ分解 |
| Implementation | 未着手。POM、Java source、fixtureは変更していない |

次回はGate 1の実装前確認から開始し、Stage 1のMaven / dependency差分を作成する。
