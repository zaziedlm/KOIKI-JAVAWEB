# Walking Skeleton — ArchUnit Rules and Distribution Validation

**Status:** V7 Completed / V1 Completed with Rule 19 Limitation

## Result

| ID | 検証 | 実結果 | 判断 |
|---|---|---|---|
| WS-A01 | 配布可能なrule artifact | `koiki-archunit-rules`をJARとしてbuild・installできた | PASS |
| WS-A02 | 外部Consumer利用（V7） | Root Reactor外の独立POMからtest依存として利用できた | PASS |
| WS-A03 | 外部での違反検出 | `internal`参照により外部Consumerの`mvn test`が失敗した | PASS |
| WS-A04 | エラーメッセージ | 外部のfailureにADR-041と`domain.event`への修正案が含まれた | PASS |
| WS-A05 | 代表規則の自己テスト | 代表5規則、主要26規則、compliant fixture、annotation併記の13テストが成功した | PASS |
| WS-A06 | V3 annotation併記 | 同じ`package-info.java`の`@NullMarked`と`@KoikiModule`を読めた | PASS |
| WS-A07 | 主要26規則すべて | 共通1〜13、Tier 1の14、Tier 2兼用15〜24、event 38〜39を記述・評価した | PASS WITH LIMITATION |

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

## V1 Major 26 Rules

| 規則 | 検証方法 | 結果 |
|---|---|---|
| 1〜2 | layer逆流のnegative fixture | 検出 |
| 3、13 | module間internal参照とFramework外参照を別fixtureで評価 | 検出 |
| 4 | 2 moduleの双方向フィールド依存 | cycleとして検出 |
| 5 | Framework型からCustomer型への依存 | 検出 |
| 6 | ControllerからRepositoryへの直接依存 | 検出 |
| 7〜8 | module rootの`@KoikiModule`欠落。persistenceはannotation必須属性 | 検出 |
| 9、21 | 他moduleのapplication/domain.model参照 | 検出 |
| 10 | 他moduleのdomain.event参照 | 許容（誤検出なし） |
| 11〜12 | 非record event、domain.model field、RestTemplate | 検出 |
| 14 | SIMPLE moduleのrich-domain package | 検出。domain.eventは許容 |
| 15〜18、20〜22、24 | RICH moduleの依存・型・配置違反 | 検出 |
| 19 | Use Case戻り値または同一method内で生成したdomain.modelをMVC Modelへ追加 | 検出（下記制限あり） |
| 23 | application.queryからreadmodelへの依存 | 許容（誤検出なし） |
| 38〜39 | event listenerの配置とdomain直結 | 検出 |

compliant fixtureへ全V1 rule setを適用し、違反がないことも確認した。

## Rule 19 Finding

`Model.addAttribute(String, Object)`と`ModelAndView`のbytecode descriptorでは、
渡した値の実際の型は`Object`となる。ArchUnitは一般的なdata-flow解析器ではないため、
任意のlocal variableの由来を完全には追跡できない。

Walking Skeletonでは、同一MVC handler内に次のいずれかがあり、かつModelへの書込みが
ある場合を検出する近似ruleが成立した。

- 戻り値が`domain.model`であるmethod call
- `domain.model` constructor call

これは典型的な「Use CaseからEntityを受け取り、そのままModelへ載せる」違反を検出する。
一方、`Object`へ型消去したhelper methodやfieldを経由する複雑なdata flowは検出保証外である。
規則19はReview Checklistへ全面格下げせず、ArchUnitの近似検査に加えて、OSIV無効化と
描画まで含むWeb testを防御線とする。これはグランドデザイン§13.3.3の三層方針と整合する。

## V1 Conclusion

主要26規則はArchUnit rule setとして記述可能だった。規則19だけは完全なdata-flow検査では
なく、典型違反を対象にした近似検査である。その他の規則について、今回のcompliant fixture
では誤検出は確認されなかった。Tier 2分離方式、MyBatis、Modulith level別規則は後続Phaseの
対象であり、本V1の26規則には含めない。
