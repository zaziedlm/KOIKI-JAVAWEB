# Phase 1a ArchUnit API Design — B2設計案

**検討日:** 2026年8月24日<br>
**対象branch:** `feature/phase1a-archunit-api-design`<br>
**状態:** DESIGN ACCEPTED / GATE 1〜5 ACCEPTED<br>
**B2 status:** COMPLETE<br>
**Architecture Owner:** Shuichi Kataoka<br>
**Ownership:** Tooling<br>
**対象:** `org.koikifw:koiki-archunit-rules:0.1.0-SNAPSHOT`

## 1. 現時点の結論

G5で承認済みの2つの合成ruleだけをPublic APIとし、Phase 1aで適用する27規則と保留する12規則を
private実装へ対応付ける。Consumerが個別規則を選択して必須検査を回避できるAPIは提供しない。

B2はPublic API、入力契約、dependency、rule matrix、message contract、Rule 19の保証限界および
compliant fixture仕様を設計する。正式Maven module、rule実装、positive / negative fixtureの実行と
CI証拠はB3で作成する。B2完了のためのno-op ruleまたは一部ruleだけの暫定実装は行わない。

新しいFramework機能、Spring Starter、Runtime、Named Interface、Spring Modulith Level 1 / 2、
Tier 2分離方式またはMyBatis詳細規則は追加しない。

## 2. Gate 1 — B2 scopeとB3引継ぎ境界

| 項目 | 判断 |
|---|---|
| Decision | ACCEPTED |
| Decided by | Shuichi Kataoka |
| Date | 2026年8月24日 |
| B2 ownership | Tooling |
| B2成果 | API・入力・dependency・matrix・message・Rule 19・fixture仕様のOwner承認 |
| B3成果 | 正式artifact、private実装、compliant成功、必須5負例、CI証拠 |
| 禁止 | no-op rule、一部ruleだけの暫定Public API、Walking Skeleton codeのcopy |
| B2完了条件 | 本書のGate 2〜5をOwnerが承認し、B3が実装判断を推測せず開始できる |

Phase 1a実行計画§7.2の「compliant fixture成功」は、B2でfixtureと期待結果を確定し、B3で実行成功を
得る連続受入条件として扱う。B2単独ではfixture成功を主張せず、B3のpositive evidenceへ引き継ぐ。

## 3. 正本と再利用境界

| 対象 | 使い方 |
|---|---|
| グランドデザイン§21.3 | 39規則の内容とエラーメッセージ方針の正本 |
| Phase 1a実行計画§6.8 | Public API、27適用／12保留、必須5違反、Rule 19制約の正本 |
| `koiki-architecture-contract` | `@KoikiModule`とTier / persistence宣言の正式Public API |
| Walking Skeleton ArchUnit検証 | 規則の成立性、誤検出、Rule 19制約、fixture観点の証拠 |
| Walking Skeleton source | コピーまたはPublic API化しない。B3で正式packageへ再実装する |

RepositoryにB2固有のOpenSpec changeは存在しないため、上記文書と本設計記録を適用する。

## 4. Public API案

公開packageは`org.koikifw.archunit`だけとし、公開型は`KoikiArchitectureRules`だけとする。

```text
public final class org.koikifw.archunit.KoikiArchitectureRules

public static ArchRule businessModuleRules(String businessBasePackage)

public static ArchRule frameworkOwnershipRules(
        String frameworkBasePackage,
        String... consumerBasePackages)
```

### 4.1 API責務

| API | 含める規則 | 含めないもの |
|---|---|---|
| `businessModuleRules` | 業務module共通、Tier別、event規則 | Framework / Consumer ownership規則、保留12規則 |
| `frameworkOwnershipRules` | Framework逆依存禁止と`internal`参照禁止 | 業務Tier判定、Customer固有業務規則 |

`representativeRules`、`phaseZeroRules`、`layerAndTierRules`、規則ごとのpublic methodは公開しない。
戻り値を`ArchRule`へ固定し、ConsumerはJUnitまたは任意のtest runnerから`.check(importedClasses)`で
実行できる。JUnit固有annotationをPublic APIへ含めない。

実装時は両methodへJavadocを付与し、base packageの意味、import対象、戻り値、拒否する入力と例外を
Public API contractとして記載する。上記は設計上のsignatureであり、B2で実装bodyやno-op ruleは作らない。

### 4.2 入力契約

- base packageは完全修飾Java package名とし、`SourceVersion.isName(value, SourceVersion.RELEASE_21)`相当で
  検証する。wildcard、Java keyword、先頭・末尾`.`、空segment、前後空白を含む値を拒否する。
- 入力のtrim、wildcard付与などの自動補正は行わない。
- 引数またはvarargs要素の`null`は`NullPointerException`、空文字、blank、不正なJava package名、
  重複または包含関係は`IllegalArgumentException`で早期に拒否する。
- `businessBasePackage`は、個別moduleのrootではなく、直下のpackage segmentに業務moduleが並ぶ親packageを
  指定する。例えば`com.example.application.catalog`ではなく`com.example.application`を渡す。
- `frameworkBasePackage`はConsumer所有packageを含まないFramework所有subtreeを指定する。
- `frameworkOwnershipRules`は1件以上のconsumer base packageを必須とする。FrameworkとConsumerのroot、
  およびConsumer root同士は相互に異なり、どちら向きの包含関係も持たないものとする。
- Framework所有範囲が複数の独立したrootに分かれる場合は、Consumerを包含する共通祖先を指定せず、
  `frameworkOwnershipRules`をrootごとに呼び出して、返された`ArchRule`をConsumer側で合成する。
- varargsは防御的にコピーし、入力値または配列を外部状態として保持しない。呼出しごとに独立した
  合成`ArchRule`を返す。

例外messageはparameter名と拒否理由を含む。例えば
`businessBasePackage must be a valid Java package name`のように、呼出し側が修正対象を特定できる形式とする。
例外型と検証条件はPublic API contract testで固定し、message断片は実装上の診断契約として検証する。

Consumerは`businessModuleRules`へ業務moduleの親package全体を、`frameworkOwnershipRules`へFrameworkと
すべてのConsumer rootを含む`JavaClasses`を渡す。設定したrootにclassが存在しない場合は、import漏れや
base package誤指定を見逃さないためroot discovery guardを失敗させる。一方、Controller、event listener、
gatewayなど任意の責務に該当classが存在しないこと自体は違反ではなく、個別ruleではempty selectionを
許容する。この区別をB3のpositive / input contract testで検証する。

### 4.3 visibilityとNull Safety

- `org.koikifw.archunit.package-info.java`へ`@NullMarked`を付与する。
- Public API inventoryとjapicmp候補は1 class、2 static methodとし、public constructorを設けない。
- Facadeから利用する実装型は`org.koikifw.archunit`と同一packageへ置き、package-private top-level classを
  既定とする。Javaのpackage-privateはsubpackageへ伝播しないため、`.internal`へ分離しない。
- private nested classの方が責務を明確に閉じられる小規模処理では、Facadeまたは同一packageの
  package-private class内へ閉じ込めてよい。
- rule ID、message断片、condition、metadata readerをpublic enum / classへ昇格させない。

`@NullMarked`はsubpackageへ伝播しない。B3で新しいsubpackageが必要になった場合は、そのpackageにも
`package-info.java`と`@NullMarked`を置き、公開型を追加せずにNullAwayの検査対象とする。ただし、Gate 2の
既定構成は同一packageへのpackage-private配置であり、責務名だけを理由にsubpackageを先行作成しない。

## 5. Mavenとdependency設計

### 5.1 BOM / module構成

B3で`koiki-dependencies-bom`へ次を追加する。

| BOM項目 | 値／扱い |
|---|---|
| property | `<archunit.version>1.5.0</archunit.version>` |
| external dependency management | `com.tngtech.archunit:archunit:${archunit.version}` |
| KOIKI dependency management | `org.koikifw:koiki-archunit-rules:0.1.0-SNAPSHOT` |

`koiki-archunit-rules`は`koiki-parent:0.1.0-SNAPSHOT`を継承し、Root Reactorへ正式moduleとして追加する。
Rules自身のdependencyにversionを重複記載せず、ParentがimportするKOIKI BOMを正本とする。

| 区分 | dependency | scope / 理由 |
|---|---|---|
| Parent | `org.koikifw:koiki-parent` | Java 21、Enforcer、Toolchains、NullAwayを継承 |
| Production direct | `org.koikifw:koiki-architecture-contract` | `@KoikiModule`と宣言enumを読む |
| Production direct | `com.tngtech.archunit:archunit` | `ArchRule`、import model、condition APIを使用 |
| Production direct | `org.jspecify:jspecify` | 公開packageを`@NullMarked`にする。Contract経由へ暗黙依存しない |
| Test | `org.junit.jupiter:junit-jupiter` | rule自己test、contract test、fixture実行 |
| Test fixture | `org.springframework:spring-context` | `@Controller`、`@EventListener`等のfixture |
| Test fixture | `org.springframework:spring-web` | MVC mapping、`RestTemplate`等のfixture |
| Test fixture | `org.springframework:spring-webmvc` | `Model` / `ModelAndView`経路のfixture |
| Test fixture | `org.springframework:spring-tx` | `@TransactionalEventListener`禁止fixture |
| Test fixture | `org.springframework.data:spring-data-commons` | `Repository<T, ID>`許容fixture |
| Test fixture | `org.springframework.data:spring-data-jpa` | `JpaRepository`禁止fixture |
| Test fixture | `jakarta.persistence:jakarta.persistence-api` | JPA annotation許容fixture |
| Test fixture | `org.springframework.modulith:spring-modulith-events-api` | `@ApplicationModuleListener`のRule 28 / 38 negative fixture |

上記test fixture dependencyはすべてtest scopeとし、Spring Starter、Spring Modulith runtime / core、
JPA provider、MyBatis、DB driverまたはSpring Boot application runtimeを追加しない。fixture sourceが直接参照する
APIを直接dependencyとして宣言し、推移依存だけにcompileを委ねない。

B3の`dependency:tree`では、productionの直接依存がContract、ArchUnit core、JSpecifyだけであること、
Spring Framework、Spring Data、Spring Modulith、JPAおよびJUnitがcompile / runtime scopeへ混入しないことを
記録する。ArchUnitとContract自身の正当な推移依存は「production dependencyが3 artifactだけ」という表現で
誤って禁止せず、直接依存と解決後treeを分けて証拠化する。

### 5.2 ArchUnit versionと互換性境界

Spring Boot BOM 4.1.1はArchUnitを管理していない。2026年8月24日時点の公式最新releaseであり、
Java 27 class fileまでの対応を含む[ArchUnit 1.5.0](https://github.com/TNG/ArchUnit/releases/tag/v1.5.0)を
Phase 1a baselineとして採用する。

Public APIが`ArchRule`を戻すため、ArchUnitは内部実装dependencyではなく、Consumerから見える実効Public API
dependencyである。KOIKI BOMで1.5.0へ固定し、Consumerによるversion overrideは互換性未検証として扱う。
後続のArchUnit更新では、C3のPublic API inventory / japicmpに加えて、Consumer source / binary compatibilityと
rule実行を再検証する。

ConsumerはArchUnit coreの`ClassFileImporter`と`ArchRule.check(JavaClasses)`を通常のJUnit testから利用する。
KOIKI Public APIへJUnit固有annotationを含めないため、`archunit-junit5`、`archunit-junit6`またはArchUnit test
engineをRules artifactのproduction dependencyへ追加しない。

### 5.3 Consumer利用契約

KOIKI BOMをimportし、Rules artifactをtest scopeで利用する構成を標準とする。

```xml
<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>org.koikifw</groupId>
      <artifactId>koiki-dependencies-bom</artifactId>
      <version>${koiki.version}</version>
      <type>pom</type>
      <scope>import</scope>
    </dependency>
  </dependencies>
</dependencyManagement>

<dependencies>
  <dependency>
    <groupId>org.koikifw</groupId>
    <artifactId>koiki-archunit-rules</artifactId>
    <scope>test</scope>
  </dependency>
</dependencies>
```

BOMをimportできないConsumerはRulesとArchUnitを承認済みversionへ明示的に揃える必要があり、異なるArchUnit
versionとの組合せをサポート済みとみなさない。内部snapshot repositoryのURL、認証、公開単位とRepository外の
実解決はC1 / C2で検証する。

標準的なConsumer testは対象をpackage単位でimportし、2つの合成ruleを個別に実行する。

```java
JavaClasses businessClasses = new ClassFileImporter()
        .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
        .importPackages("com.example.application");

KoikiArchitectureRules.businessModuleRules("com.example.application")
        .check(businessClasses);

JavaClasses ownershipClasses = new ClassFileImporter()
        .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
        .importPackages("org.koikifw", "com.example");

KoikiArchitectureRules.frameworkOwnershipRules("org.koikifw", "com.example")
        .check(ownershipClasses);
```

`businessModuleRules`には個別moduleでなく、その直下にmoduleが並ぶ親packageを渡す。ownership検査には、
設定したFramework rootと全Consumer rootのclassを同じ`JavaClasses`へimportする。Consumerの通常利用例では
`importClasspath()`を既定にせず、第三者libraryやtest fixtureを意図せず検査対象へ含めない。B3のfixtureは
test source自体が検査対象なので、上記`DO_NOT_INCLUDE_TESTS`を使わずfixture packageを明示importする。

B2ではPOMまたは空moduleを先行生成せず、上記の正式構成をB3でrule実装、testおよびdependency evidenceと
同時に追加する。

## 6. private実装構成案

```text
org.koikifw.archunit
├── KoikiArchitectureRules.java        # 唯一のPublic API
├── package-info.java                  # @NullMarked
├── PackageName.java                   # 入力検査。package-private
├── ModuleMetadata.java                # @KoikiModule読取。package-private
├── BusinessModuleRuleSet.java         # 規則1〜24、28、38〜39の合成。package-private
├── FrameworkOwnershipRuleSet.java     # 規則5、13の合成。package-private
├── KoikiArchCondition.java            # 必要な場合だけ抽出。package-private
└── RuleMessage.java                   # 必要な場合だけ抽出。package-private
```

上記は責務境界を示す案であり、未使用classを先行作成する指示ではない。B3では実際に共有される処理だけを
抽出し、1つの巨大conditionへ27規則を集約しない。各failureが単一rule IDへ対応できる粒度を保つ。

### 6.1 Gate 2 decision

| 項目 | 判断 |
|---|---|
| Decision | ACCEPTED WITH DOCUMENTATION CONDITIONS — 条件反映済み |
| Decided by | Shuichi Kataoka |
| Date | 2026年8月24日 |
| Public API | `KoikiArchitectureRules`の2 static methodだけを公開する |
| package / visibility | Facadeとpackage-private実装型を同一packageへ置き、公開内部型を作らない |
| Null Safety | root packageを`@NullMarked`とし、将来subpackageを作る場合は個別に付与する |
| Input contract | Java 21 package名、root非重複、例外型、防御的copyを固定する |
| Empty selection | root未検出は失敗、任意責務の対象classなしは許容する |

Owner Reviewで整理した5点を本書へ反映したため、Gate 2をクローズする。B3ではJavadocとcontract testを
同時に実装し、package-private制約を緩めてFacadeからsubpackageへ接続する回避は行わない。

## 7. Phase 1a rule matrix詳細案

### 7.1 B3で適用する27項目

`<base>`は`businessBasePackage`、`<module>`はその直下の1 package segmentを表す。module判定は
文字列の部分一致ではなく`<base>.<module>`のpackage境界で行う。Tier別規則はmodule rootの
`@KoikiModule`を読み、SIMPLE / RICHを混在適用しない。

| Rule | 種別・適用条件 | B3で実装する違反判定／許容条件 | failure対象 |
|---:|---|---|---|
| 1 | 違反／全Tier | `<base>..<adapter.inbound..>`のclassから`<base>..<adapter.outbound..>`への直接依存を拒否する | source classとtarget dependency |
| 2 | 違反／全Tier | `<base>..<application..>`のclassから`<base>..<adapter.inbound..>`への直接依存を拒否する | source classとtarget dependency |
| 3 | 違反／全Tier | sourceとtargetのmodule segmentが異なり、targetが公開境界でない直接依存を拒否する。Phase 1aのmodule間公開境界はRule 10の`domain.event`だけとする | source／target moduleとdependency |
| 4 | 違反／全Tier | `<base>.(*)..`をmodule sliceとして、slice間のcycleを拒否する | cycleを構成するmoduleとdependency chain |
| 5 | 違反／Ownership | `frameworkBasePackage`配下から、いずれかの`consumerBasePackages`配下への直接依存を拒否する | Framework source classとConsumer target |
| 6 | 違反／全Tier | Spring MVC Controller annotationを持つclass、または`adapter.inbound`配下で`Controller`接尾辞を持つclassから、`domain.repository`のcontractまたはSpring Data Repository型への直接依存を拒否する | ControllerとRepository dependency |
| 7 | 違反／全Tier | classが存在する各module rootにruntime可視の`@KoikiModule`があり、`name`がmodule segmentと一致し、`tier`を取得できることを要求する | module root package |
| 8 | 違反／全Tier | Rule 7で発見した各moduleについて`persistence`と`persistenceModel`を取得でき、Architecture Contractで定義済みの値であることを要求する。方式間の組合せ制約は保留Rule 25〜27、30〜37で扱う | module root packageと宣言値 |
| 9 | 違反／全Tier | 他moduleの`application..`または`domain.model..`への直接依存を拒否する。Rule 10のevent例外を通常のBean参照へ拡張しない | source classとtarget dependency |
| 10 | 許容／全Tier | 他moduleの`domain.event..`にある値eventへの参照をRule 3／9の拒否predicateから除外する。event自身の形はRule 11で検査する | failureなし。許容dependencyをpositive fixtureで追跡 |
| 11 | 違反／全Tier | `domain.event..`のevent型が`record`でない場合、またはrecord component／fieldの型に`domain.model..`を含む場合に拒否する | event型またはcomponent／field |
| 12 | 違反／全Tier | `org.springframework.web.client.RestTemplate`へのconstructor call、method call、field、引数、戻り値等の直接依存を拒否する | source class／memberとRestTemplate dependency |
| 13 | 違反／Ownership | `frameworkBasePackage`外のimport済みclassから、その配下でpackage segmentが`internal`である型への直接依存を拒否する | 外部source classとFramework internal target |
| 14 | 違反／SIMPLE | SIMPLE moduleに`domain.model`、`domain.service`、`domain.repository`、`domain.gateway` packageのclassが存在する場合に拒否する。`domain.event`は許容する | 禁止package内のclass |
| 15 | 違反／RICH | RICH moduleの`domain..`から`adapter..`、Spring Web / MVCまたは`EntityManager`への直接依存を拒否する。JPA annotation型とSpring Data Commonsへの依存は明示的に許容し、JPA API全体を一律許容しない | domain source classと禁止dependency |
| 16 | 違反／RICH | `domain.repository..`のRepository contractがSpring Data Commonsの`Repository<T, ID>`を継承しない、または`JpaRepository`を直接・間接継承する場合に拒否する | Repository contract型 |
| 17 | 違反／RICH | `adapter.inbound..`に宣言されたmethodのraw引数型またはraw戻り値型が同じbusiness base配下の`domain.model..`である場合に拒否する | inbound methodと露出型 |
| 18 | 違反／RICH MVC | Spring MVC mapping annotationを持つhandler methodのraw引数型が`domain.model..`である場合に拒否する | handler methodと引数型 |
| 19 | 近似違反／RICH MVC | §9の限定条件で、同一handler内のdomain.model生成／取得とMVC Model sinkへの書込みから代表的な露出を検出する | handler methodと検出したsource／sink |
| 20 | 違反／RICH MVC | Spring MVC mapping annotationを持つhandler methodのraw戻り値型が`domain.model..`である場合に拒否する | handler methodと戻り値型 |
| 21 | 違反／RICH | `domain.model..`の型が別moduleのclassから直接参照される場合に拒否する。Rule 10をdomain.modelへ適用しない | source／target moduleとdependency |
| 22 | 違反／RICH | `domain.model..`に、publicかつ`set`で始まり引数を1つ取るmethodが存在する場合に拒否する。状態変更は業務上の意味を持つmethodへ閉じ込める | setter method |
| 23 | 許容／RICH | 同一moduleの`application.query..`が同じpackage subtreeで所有するread modelを参照することを許容する。他moduleへの公開例外にはせず、Rule 3／9／21を緩和しない | failureなし。許容dependencyをpositive fixtureで追跡 |
| 24 | 違反／RICH | classまたは継承interfaceが同一moduleの`domain.gateway..`にあるinterfaceを実装し、その具象classが`adapter.outbound.external..`外にある場合に拒否する | gateway実装classとinterface |
| 28 | 違反／Phase 1a〜Level 1 | `@TransactionalEventListener`を直接またはmeta-annotation経由で宣言したmethodを拒否する。`@ApplicationModuleListener`もPhase 1aでは拒否し、Level 2採用時にRule 29と合わせて継続・変更をOwner Reviewする | annotationを直接／間接宣言したmethod |
| 38 | 違反／全Tier | `@EventListener`または`@ApplicationModuleListener`を直接宣言したmethodの所有classが`adapter.inbound.event..`外にある場合に拒否する | listener methodと所有class |
| 39 | 違反／全Tier | `adapter.inbound.event..`のclassから`domain.model..`または`domain.repository..`への直接依存を拒否する。listenerは自moduleのApplication Use Caseへ委譲する | listener classとDomain dependency |

Spring MVC handlerはSpring MVCのmapping annotationを直接またはmeta-annotation経由で持つmethodとして
判定する。B3でArchUnit import modelがmeta-annotationを解決できない場合は、Spring標準mapping annotationの
完全修飾名を列挙して同じ範囲を検査し、検証結果へ方式を記録する。

規則10と23は誤検出防止の許容predicateであり、独立した`ArchRule`、常時成功ruleまたはfailureを生成しない。
これらを除く25規則は、該当する違反ごとに単独のrule IDを報告する。同一codeがRule 17と18、または
Rule 17と20の両方へ違反する場合も、`17/18`のようにIDを結合せず独立した2 failureとして報告する。

### 7.2 B2 / B3で実装しない12項目

| Rule | 保留理由 | 判断時期 |
|---:|---|---|
| 25〜27 | Tier 2分離方式を未実証 | 分離方式を採用する後続Phase |
| 29 | Level 2の同期listener / 外部I/O境界が未確定 | Spring Modulith Level 2設計時 |
| 30〜34 | MyBatis詳細規約を未実証 | Phase 3末尾〜Phase 4 |
| 35〜37 | 分離modelとMapper / JPA signature規約を未実証 | Phase 3末尾〜Phase 4 |

保留ruleのID、説明、理由はREADMEとValidationへ残すが、成功する空ruleや常時無効な実装は作らない。

## 8. Error message contract案

各違反は次の形式を持つ。

```text
[KOIKI-ARCH-<3桁rule ID>] [ADR-<3桁ID>] [ADR-<追加ID>または§<設計節>]...
違反内容: <class / method / package / dependency>
影響: <設計・保守上の影響>
修正: <具体的な修正方法>
```

複数の判断根拠を持つ場合は、例えば`[KOIKI-ARCH-004] [ADR-004] [ADR-025]`のように
角括弧を分けて列挙する。

- 1つのfailure messageへ複数rule IDを`17/18`のように結合しない。
- 同じcodeが複数規則へ違反する場合は、各ruleが独立したfailureを報告してよい。
- `because()`だけに情報を置かず、違反eventにも具体的なclass / method / dependencyを含める。
- rule ID、1件以上のADRまたは設計節、影響、修正方法の欠落をB3のmessage contract testで検出する。
- rule IDはグランドデザイン§21.3と一対一に対応し、別規則へ再利用しない。

`because()`には規約、判断根拠、影響、修正方針を置き、custom `ArchCondition`のviolation eventには
具体的なclass、method、packageまたはdependencyを置く。B3のtestは最終failure reportに両方が含まれることを
検証し、rule descriptionだけ、またはeventだけを個別に見て契約充足と判定しない。

### 8.1 規則別message catalog

| Rule ID | 判断根拠 | 影響 | 修正方針 |
|---|---|---|---|
| `KOIKI-ARCH-001` | ADR-022 | Inboundが技術実装へ結合し、依存方向と差替え境界が崩れる | Application Use CaseとPortを介する |
| `KOIKI-ARCH-002` | ADR-022 | Applicationが入力技術へ依存し、Use Caseを別入口から再利用できない | InboundからApplicationを呼ぶ方向へ戻す |
| `KOIKI-ARCH-003` | ADR-041 | 他moduleの非公開実装に結合し、内部変更が波及する | 公開された`domain.event`を利用するか所有moduleへ処理を戻す |
| `KOIKI-ARCH-004` | ADR-004、ADR-025 | module間の変更方向と初期化順序が循環する | event等で依存を一方向にする |
| `KOIKI-ARCH-005` | ADR-014 | Frameworkの独立配布とCustomer差替えができなくなる | Consumer依存を除去し、必要な契約をFramework側へ反転する |
| `KOIKI-ARCH-006` | ADR-022 | Controllerがtransactionと業務処理順序を迂回する | Application Use Case経由でRepositoryを利用する |
| `KOIKI-ARCH-007` | ADR-022 | Tier別規則を選択できずmodule ownershipも曖昧になる | module rootの`package-info.java`へ正しいname／tierの`@KoikiModule`を付与する |
| `KOIKI-ARCH-008` | ADR-022、ADR-023 | 永続化方式に対応する規則を選択できない | `persistence`と`persistenceModel`を明示する |
| `KOIKI-ARCH-009` | ADR-025 | 他moduleのUse Case／Domainへ直接結合し境界が形骸化する | 値だけを持つ`domain.event`で連携する |
| `KOIKI-ARCH-011` | ADR-025 | eventが可変またはDomain Modelを露出しmodule間を密結合にする | 値だけをcomponentに持つ`record`へ変更する |
| `KOIKI-ARCH-012` | ADR-033 | 新旧HTTP client方式が混在し横断方針が分裂する | 承認済みHTTP Service Interface方式へ置き換える |
| `KOIKI-ARCH-013` | ADR-041 | Framework内部実装への依存がPublic API互換性を迂回する | 公開APIだけを利用する |
| `KOIKI-ARCH-014` | ADR-022 | SIMPLE moduleへ未使用のRich Domain構造が入り複雑性が増す | 判断をApplicationへ置くか、昇格条件を満たす場合にmodule全体をRICHへ変更する |
| `KOIKI-ARCH-015` | ADR-022、ADR-023 | DomainがAdapter／Web／永続化操作へ結合し業務規則を独立維持できない | 技術処理をAdapterへ移し、許容されたannotation／Repository contractだけを残す |
| `KOIKI-ARCH-016` | ADR-024 | Domain RepositoryがJPA固有操作を公開し永続化境界が漏れる | Spring Data Commons `Repository<T, ID>`だけを継承する |
| `KOIKI-ARCH-017` | ADR-023 | Inbound APIへDomain Modelが露出し遅延loadと変更波及を招く | Form、DTOまたはread modelへ変換する |
| `KOIKI-ARCH-018` | ADR-023 | HTTP bindingがDomain Modelを直接変更し不変条件を迂回する | 入力Form／DTOを受け取りUse Caseへ変換する |
| `KOIKI-ARCH-019` | ADR-023、ADR-028 | view描画時の遅延loadやresponse送信後の失敗を招く | transaction内でDTO／read modelへ変換してからModelへ渡す |
| `KOIKI-ARCH-020` | ADR-023 | MVC戻り値としてDomain Modelが外部境界へ露出する | DTO、view名またはread modelを返す |
| `KOIKI-ARCH-021` | ADR-023、ADR-025 | 他moduleがDomain内部表現へ結合し独立変更できない | 所有module内へ参照を戻し、module間はeventで連携する |
| `KOIKI-ARCH-022` | ADR-023 | setterが不変条件を迂回し任意状態変更を許す | 意味のある状態遷移methodへ閉じ込める |
| `KOIKI-ARCH-024` | ADR-022 | 外部I/O実装がDomainへ混入し技術詳細に結合する | 実装を`adapter.outbound.external`へ移す |
| `KOIKI-ARCH-028` | ADR-005 | 未確定のtransaction phase／非同期運用をPhase 1aへ先行導入する | Level 0／1では`@ApplicationModuleListener`を含むtransactional listenerを使わず、同期`@EventListener`を所定packageで使用する |
| `KOIKI-ARCH-038` | ADR-025 | listener入口が散在しmodule連携の監査と変更が難しくなる | listenerを`adapter.inbound.event`へ移す |
| `KOIKI-ARCH-039` | ADR-025 | listenerがUse Caseとtransaction調整を迂回する | listenerからApplication Use Caseだけを呼び出す |

Rule 10と23はfailureを生成しないためmessage catalogを持たず、`KOIKI-ARCH-010`または
`KOIKI-ARCH-023`を成功messageとして出力しない。両IDは39規則matrixとの対応を示す識別子として保持し、
許容fixture名とtest結果から追跡する。日本語表現の改善は互換変更としないが、rule ID、判断根拠、影響、
修正方針または違反箇所を失う変更はOwner Review対象とする。

## 9. Rule 19設計制約

Rule 19は次のsourceとsinkが同一MVC handler内に存在する代表経路を検出する。

1. source: domain.modelを返すmethod call、またはdomain.model constructor call
2. sink: `Model.addAttribute`、`ModelAndView.addObject`、またはmodel値／mapを受け取る
   `ModelAndView` constructor call

helperで`Object`へ型消去した値、field経由、複数method間、reflectionによるdata flowは保証しない。
また、ArchUnit import modelだけでsourceからsinkへの引数data flowを証明できない実装では、同じhandler内で
domain.modelをDTOへ変換し、DTOだけをModelへ追加する正常経路を誤検出する可能性がある。検出漏れだけでなく、
このco-occurrence判定による誤検出可能性もREADME、error messageおよびValidationへ明記する。

B3ではRule 19専用に次の3経路を隔離して検証する。

1. domain.modelを生成またはUse Caseから取得し、そのまま対象sinkへ渡す代表違反が
   `KOIKI-ARCH-019`で失敗する。
2. domain.modelを取得後にDTOへ変換し、DTOだけを対象sinkへ渡す正常経路が成功する。
3. helper、field、複数methodまたはreflection経由は検出保証外であることをValidationへ記録する。

2の正常経路を通せない場合はRule 19を正式artifactへ組み込んだままB3を完了せず、判定方法または
Review Checklistとの役割分担をOwner Reviewへ戻す。ArchUnit単独でEntity露出を完全防止できると表現せず、
後続PhaseのOSIV無効化と実レンダリングWeb testを含む三層防御へ引き継ぐ。

### 9.1 Gate 3 decision

| 項目 | 判断 |
|---|---|
| Decision | ACCEPTED WITH DOCUMENTATION CONDITIONS — 条件反映済み |
| Decided by | Shuichi Kataoka |
| Date | 2026年8月24日 |
| 27適用 | 25 failure ruleとRule 10／23の2許容predicateとして実装条件を確定 |
| 12保留 | 方式別の判断時期までno-opを含め実装しない |
| Message | 規則別ID・根拠・影響・修正方針と具体的違反箇所を要求 |
| Rule 19 | source／sink、検出漏れ、誤検出可能性、B3の3経路を確定 |

Owner Reviewで整理した4点を本書へ反映したため、Gate 3をクローズする。B3はWalking Skeletonの
結合rule IDやRule 19実装をコピーせず、本節の判定条件とfixtureで正式実装を検証する。

## 10. compliant fixture仕様

fixtureは`koiki-archunit-rules/src/test/java`配下に置くTooling所有の検査用sourceとし、正式Framework、
ReferenceまたはCustomer業務moduleとして配布しない。B1生成物やWalking Skeleton sourceをコピーせず、
B3 rule検証に必要な最小classだけを作成する。

### 10.1 business fixture package tree

```text
org.koikifw.archunit.fixture.compliant.business
├── simple
│   ├── package-info.java                         # @NullMarked + @KoikiModule(name="simple", tier=SIMPLE, persistence=JPA, persistenceModel=SHARED)
│   ├── application/SimpleUseCase.java
│   ├── adapter/outbound/persistence/SimpleStore.java
│   └── domain/event/SimpleCompleted.java         # 値だけを持つrecord
└── rich
    ├── package-info.java                         # @NullMarked + @KoikiModule(name="rich", tier=RICH, persistence=JPA, persistenceModel=SHARED)
    ├── application/RichUseCase.java
    ├── application/query/RichQuery.java
    ├── application/query/RichSummary.java        # query package所有record
    ├── domain/model/RichAggregate.java           # JPA annotationは許容、public setterなし
    ├── domain/repository/RichRepository.java     # Repository<RichAggregate, Long>
    ├── domain/gateway/ExternalService.java
    ├── domain/event/RichCompleted.java            # 値だけを持つrecord
    ├── adapter/outbound/external/ExternalServiceAdapter.java
    ├── adapter/inbound/mvc/RichController.java   # domain取得後DTOへ変換しDTOだけをModelへ追加
    ├── adapter/inbound/mvc/RichView.java          # DTO / Form相当の値record
    └── adapter/inbound/event/RichListener.java   # Application Use Caseだけを参照
```

`SimpleUseCase`は他moduleの`rich.domain.event.RichCompleted`だけを参照し、Rule 10のmodule間公開例外を
実証する。`RichQuery`は同じ`application.query`が所有する`RichSummary`を参照し、Rule 23を実証する。
`RichAggregate`はJPA annotationだけを使用し、`EntityManager`へ依存しない。`RichRepository`は
Spring Data Commonsの`Repository<T, ID>`を継承し、`JpaRepository`を継承しない。

`simple`にはController、Domain Model、Repository、Gatewayおよびlistenerを作らない。module rootのclassは
存在するためroot discovery guardは成功し、存在しない任意責務を対象にしたRule 14〜24、38〜39の
empty selectionも成功することを確認する。未使用packageを空directoryとして先行作成しない。

Rule 38のcompliant positive確認では、同期`@EventListener`を宣言した必要最小限のlistener methodだけを
`adapter.inbound.event`へ置く。`@ApplicationModuleListener`は`@Async`、`@Transactional`および
`@TransactionalEventListener`を合成するmeta-annotationであるため、Phase 1aのcompliant fixtureへ含めない。
Rule 28がmeta-annotation経由でも拒否することとRule 38の配置判定は、B3の隔離negative / focused testで
個別に検証する。

### 10.2 ownership fixture package tree

```text
org.koikifw.archunit.fixture.compliant.ownership
├── framework
│   ├── api/FrameworkApi.java
│   └── sample/internal/FrameworkInternal.java
├── reference/ReferenceConsumer.java              # FrameworkApiだけを参照
└── customer/CustomerConsumer.java                # FrameworkApiだけを参照
```

Framework側は`reference`または`customer`へ依存せず、両Consumerは`FrameworkInternal`を参照しない。
`framework`、`reference`、`customer`は相互に重複・包含しないrootとして、2件のConsumer varargsと
各rootのclass discoveryを同時に検証する。`FrameworkInternal`は検査対象としてimportするが、正常fixtureから
参照しない。

### 10.3 positive test matrix

| test | import範囲／Public API | 期待結果・実証する契約 |
|---|---|---|
| `compliantBusinessRulesPass` | business fixture全体／`businessModuleRules("...compliant.business")` | SIMPLE / RICHを自動判定し、合成rule全体が成功 |
| `explicitAllowancesPass` | simpleとrich／同上 | Rule 10のmodule間event参照とRule 23のquery所有read modelが成功 |
| `optionalResponsibilitiesMayBeAbsent` | simple module全体／同上 | moduleは発見し、Controller、Rich Domain、Gateway、listenerなしを理由に失敗しない |
| `richJpaSharedBoundaryPasses` | rich module全体／同上 | JPA annotation、Spring Data Commons Repository、gateway配置が成功 |
| `rule19AllowsDtoConversion` | rich module全体／同上 | domain.model取得後にDTOへ変換し、DTOだけをModelへ渡す正常経路が成功 |
| `compliantOwnershipRulesPass` | ownership fixture全体／`frameworkOwnershipRules("...ownership.framework", "...ownership.reference", "...ownership.customer")` | Framework逆依存と2 Consumerからのinternal参照がなく成功 |

B3 testではfixture packageを明示してimportし、test class除外optionは指定しない。各testは対象の
`ArchRule.check(JavaClasses)`が例外なく完了することを期待結果とする。合成rule全体の成功だけでなく、
Rule 10、23、19およびempty selectionは上表のfocused testでも意図を固定する。

fixtureはbytecode構造だけを検査するため、Spring ApplicationContext、Spring Boot application、DB、JPA provider、
networkまたはcontainerを起動しない。B3で`mvn verify`成功、test件数、dependency treeおよび適用した2つの
Public APIをpositive evidenceとして記録する。

### 10.4 Gate 4 decision

| 項目 | 判断 |
|---|---|
| Decision | ACCEPTED WITH DOCUMENTATION CONDITIONS — 条件反映済み |
| Decided by | Shuichi Kataoka |
| Date | 2026年8月24日 |
| Maven | BOM管理、直接production dependency、限定test dependencyをartifact単位で確定 |
| ArchUnit | 1.5.0をbaselineとし、`ArchRule`露出による実効Public API互換性を管理 |
| Consumer | BOM + test scope、限定package import、2 Public APIの実行契約を確定 |
| Fixture | business / ownership tree、6 positive test、非runtime境界を確定 |

Owner Reviewで整理した4点を本書へ反映したため、Gate 4をクローズする。B3でdependency解決またはfixture実装が
本設計の前提を否定した場合は、依存をproductionへ昇格して回避せずValidationとOwner Reviewへ戻す。

## 11. B3へ引き継ぐ必須5負例

| 負例 | Rule | 独立性 |
|---|---:|---|
| Tier宣言欠落 | 7〜8 | 他のpackage / dependency違反を含めない |
| Controllerのdomain.model露出 | 17〜20 | 露出以外のRepository直結等を含めない |
| `internal`外部参照 | 3 / 13 | 業務module間とFramework境界をfixtureで区別する |
| `@TransactionalEventListener` | 28 | direct annotationを所定listener packageで使用し、Rule 38の配置違反を同時に発生させない。meta-annotation経路は別focused testで検証する |
| module間直接Bean参照 | 9〜10 | domain.event許容fixtureと対にする |

各負例は独立Maven testまたは隔離したtest fixtureで期待failureを検証し、production sourceへ失敗switchを
追加しない。testから期待違反を評価して通常CIは成功させ、raw failure reportにADR、影響、修正方法および
違反箇所が含まれることをB3のOwner Review対象とする。

## 12. B2 ValidationとB3引継ぎ

### 12.1 B2 design validation

| 確認対象 | B2での確認結果 | 判定 |
|---|---|---|
| 正本突合 | グランドデザイン§21.3、Phase 1a実行計画§6.8、ADR Register、Walking Skeleton ArchUnit Validationと照合 | PASS |
| 39 rule traceability | Phase 1a適用27＋保留12＝39 | PASS |
| 適用rule内訳 | 25 failure rule＋Rule 10 / 23の2許容predicate＝27 | PASS |
| Public API | 1 public class、2 public static method、public constructorなし | PASS |
| Dependency設計 | production直接依存3、test fixture依存8、Spring等のproduction昇格なし | PASS |
| Positive fixture設計 | business / ownership package tree、focused testを含む6 positive test | PASS |
| Rule 19 | source / sink、検出漏れ、誤検出可能性、3検証経路を明記 | PASS |
| Rule 28 / 38 | `@ApplicationModuleListener`をPhase 1aのRule 28違反とし、Rule 38の将来配置検査と分離 | PASS |
| 文書品質 | rule件数、message catalog 25件、結合IDなし、末尾空白なし、差分check成功 | PASS |
| B2実行境界 | POM、Java source、fixture、Maven / CI実行結果をB2で作成していない | PASS |

ArchUnit 1.5.0は2026年8月24日時点の公式release情報と照合した。`@ApplicationModuleListener`の意味は
[Spring Modulith 2.1.0 API](https://docs.spring.io/spring-modulith/docs/current/api/org/springframework/modulith/events/ApplicationModuleListener.html)で
`@Async`、`@Transactional`および`@TransactionalEventListener`の合成であることを確認した。

B2は設計Validationだけで完了し、ruleが実際に成功／失敗する証拠を主張しない。実装、Maven、CIおよび
dependency解決の実証は次項のB3 acceptance checklistで取得する。

### 12.2 B3 acceptance checklist

B3は次の順序と完了条件で実装する。

1. BOMへArchUnit 1.5.0とRules artifactを追加し、Root Reactorへ正式moduleを追加する。
2. 1 public class / 2 static method、Javadoc、入力検査、root discovery guardおよびpackage-private実装を作る。
3. 25 failure ruleとRule 10 / 23の2許容predicateを実装し、保留12 ruleのno-opを作らない。
4. 25 failure ruleそれぞれに、違反判定と単独`KOIKI-ARCH-nnn`を直接確認するfocused testを最低1件設ける。
5. Rule 10 / 23を独立したpositive testで確認し、成功messageまたは常時成功ruleを作らない。
6. null、blank、不正package名、重複、包含、varargs防御的copy、root未検出および任意責務emptyを
   input / behavior contract testで確認する。
7. §10の6 positive testでcompliant business / ownership fixtureの合成rule成功を確認する。
8. §11の必須5負例を、他の違反による偶然の失敗がない独立end-to-end fixtureで確認する。
9. 25 failure reportすべてでrule ID、判断根拠、影響、修正方針および具体的違反箇所を確認する。
10. Rule 19の代表違反、DTO変換正常経路、保証外経路の3つを確認する。
11. `@ApplicationModuleListener`がmeta-annotation経由のRule 28違反になることを確認する。Rule 38の
    `@ApplicationModuleListener`配置selectorはpackage-private ruleのfocused testで分離確認する。
12. `dependency:tree`で直接／推移依存とscopeを記録し、Spring等がproductionへ混入していないことを確認する。
13. Rules module単体とRoot ReactorのMaven Wrapper `verify`、既存CI成功を記録する。
14. test件数、rule traceability、raw failure report、dependency tree、command、JDK、commitおよびCI結果を
    B3 Validationへ記録する。

focused negative testは`EvaluationResult`または`ArchRule.check`の期待failureをtest codeから検証して、
通常の`mvn verify`をgreenに保つ。恒常的に失敗するMaven profile、production sourceのfailure switchまたは
失敗fixtureを通常Application sourceへ追加しない。

### 12.3 B3 stop / return conditions

次の場合は実装都合で設計を緩和せず、B2設計とOwner Reviewへ戻す。

- Rule 19がDTO変換正常経路を誤検出し、§9の3経路を同時に満たせない。
- ArchUnit import modelでmeta-annotationを解決できず、Rule 28 / 38の承認済み意味を保持できない。
- Spring、JPA、ModulithまたはJUnitをproduction scopeへ移さなければ実装できない。
- Public APIの追加、個別rule選択API、公開内部型または入力契約変更が必要になる。
- 25 failure ruleのいずれかをno-op、結合IDまたは未検証のままにする必要が生じる。
- 保留12 rule、Runtime、Level 2、MyBatis詳細またはTier 2分離方式の先行実装が必要になる。

## 13. B2 Owner Review計画

| Gate | Review対象 | 現在状態 |
|---:|---|---|
| 1 | B2 scope、Ownership、B3境界、no-op禁止 | ACCEPTED |
| 2 | Public API、入力契約、package、visibility | ACCEPTED |
| 3 | 27適用／12保留matrix、message contract、Rule 19 | ACCEPTED |
| 4 | Maven dependency、ArchUnit version、Consumer利用、fixture仕様 | ACCEPTED |
| 5 | Validation、Deferred、B3引継ぎ、B2最終判定 | ACCEPTED |

### 13.1 Gate 5 decision / B2 final decision

| 項目 | 判断 |
|---|---|
| Decision | ACCEPTED |
| Decided by | Shuichi Kataoka |
| Date | 2026年8月24日 |
| Validation | B2 design validationをPASSとし、実装証拠はB3へ分離 |
| Deferred | B3以降のWP、後続Phase、保留12 ruleの所有を維持 |
| B3 handoff | 14項目のacceptance checklistと6 stop / return conditionを確定 |
| B2 final status | COMPLETE |

Gate 1〜5がすべてOwner承認され、B3がPublic API、rule判定、message、dependencyおよびfixtureを推測せず
開始できるため、Phase 1a B2をCOMPLETEとしてクローズする。

## 14. Deferred

- B3: 正式Maven module、25 failure rule＋2許容predicate、contract test、compliant / 必須5 negative fixture、CI証拠
- B4: ParentへNullAwayの正常・負例・復元検証を正式統合
- B5: Feature TemplateへArchUnit / NullAway / Level 0を最終統合
- C1 / C2: snapshot公開とRepository外Consumer検証
- C3: 正式Public API inventory、ArchUnit dependency互換性、japicmp baseline / 破壊検出
- Phase 1b以降: Runtime、OSIV、Web test、Named Interface
- Level 2設計時: Rule 28 / 29と`@ApplicationModuleListener`の適用を再判定
- Phase 3 / 4: Level 1 / 2、MyBatis、Tier 2分離方式、保留12規則
