# Walking Skeleton — ArchUnit Rules and Distribution Validation

**Status:** V7 Completed / V1 Round 1 Completed

## Result

| ID | 検証 | 実結果 | 判断 |
|---|---|---|---|
| WS-A01 | 配布可能なrule artifact | `koiki-archunit-rules`をJARとしてbuild・installできた | PASS |
| WS-A02 | 外部Consumer利用（V7） | Root Reactor外の独立POMからtest依存として利用できた | PASS |
| WS-A03 | 外部での違反検出 | `internal`参照により外部Consumerの`mvn test`が失敗した | PASS |
| WS-A04 | エラーメッセージ | 外部のfailureにADR-041と`domain.event`への修正案が含まれた | PASS |
| WS-A05 | 代表規則の自己テスト | negative 5規則、compliant fixture、annotation併記の7テストが成功した | PASS |
| WS-A06 | V3 annotation併記 | 同じ`package-info.java`の`@NullMarked`と`@KoikiModule`を読めた | PASS |
| WS-A07 | 主要26規則すべて | 第1ラウンドでは未完了 | CONTINUE |

## Representative Rules

| 対象 | 関連する規則 | 検証内容 |
|---|---|---|
| module宣言 | 7、8 | module rootの`@KoikiModule`欠落を検出 |
| domain model露出 | 17〜20 | inboundメソッドの引数・戻り値を検出 |
| internal参照 | 3、13 | 他moduleの`internal`への依存を検出 |
| 直接module連携 | 9、10 | 他moduleの`application` / `domain.model`への依存を検出 |
| event listener | 28 | `@TransactionalEventListener`を検出 |

各ruleの`because()`には、規約、ADRまたは設計節、違反の影響、修正方法を記述した。

## Distribution Finding

`koiki-archunit-rules`だけを`-am install`しても、ParentがimportするBOMは
dependency graph上のmoduleではないためinstallされなかった。外部Consumerはartifact
descriptorのBOMを解決できず、Architecture Test以前に失敗した。

次の4成果物を同じversionのリリース単位として公開する必要がある。

1. `koiki-dependencies-bom`
2. `koiki-parent`
3. architecture contract artifact
4. `koiki-archunit-rules`

Walking Skeletonでは4成果物をローカルRepositoryへinstallした後、外部Consumerを
別Maven buildとして起動し、期待したArchUnit failureを確認した。

## Architecture Contract Finding

`@KoikiModule`、`ModuleTier`、`PersistenceTechnology`、`PersistenceModel`は
Customer production codeが参照する。test用の`koiki-archunit-rules`へ配置せず、
依存を持たない極小architecture contract artifactへ分離する構成が成立した。

Walking Skeletonのcontractはグランドデザインの宣言例に合わせ、次を持つ。

- `name`
- `tier = SIMPLE / RICH`
- `persistence = JPA / MYBATIS / JDBC`
- `persistenceModel = SHARED / SEPARATED`

## V1 Remaining Work

V1全体を完了とするには、計画にある主要26規則（共通1〜13、Tier 1の14、
Tier 2兼用15〜24、event listener 38〜39）をすべて実装し、positive / negative
fixtureと誤検出評価を完了する必要がある。

第1ラウンドでは配布方式を先に確定するため、代表5規則だけを実装した。
実装不能な規則やReview Checklistへ格下げすべき規則は現時点では見つかっていない。
