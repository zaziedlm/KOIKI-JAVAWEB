# KOIKI Architecture Rules（ArchUnit）開発者向け説明

**状態:** OWNER APPROVED / 2026-09-30（Architecture Owner review完了）。2026-09-30時点の`koiki-archunit-rules`実装と照合済みです。実チームによる読み合わせは未実施です。
**対象:** KOIKIを使ってCustomer Applicationを開発する人、およびそのcodeをreviewする人。

## この文書の使い方

KOIKIのArchitecture Rulesは、業務moduleの分け方と依存の向きをbuildのたびに機械的に検査します。
`clean verify`が`KOIKI-ARCH-nnn`で失敗したら、まず§4でそのIDを探してください。
初めて読む場合は§1〜§3を読み、自分のpackage構成がどのように判定されるかを先に把握することを勧めます。

| 知りたいこと | 読む箇所 |
|---|---|
| Architecture Rulesは何を守っているのか | §1 |
| Customer projectへtestを組み込む方法 | §2 |
| 違反messageの読み方 | §3 |
| 各ルールの内容と直し方 | §4 |
| ルールが検出しないもの | §5 |
| どうしても直せないとき | §6 |

package構成と実装の手順そのものは[アプリケーション開発者ガイド](application-developer-guide.md)で説明しています。

## 1. ルールが守っているもの

Architecture Rulesは、次の3つの設計方針について、実装済みの条件に該当する違反をtestの失敗として検出します。

1. **業務moduleの独立性。** `orders`と`billing`のような業務moduleは、公開したevent等の決められた入口以外で互いの内部に触れません。触れてしまうと、一方の内部変更がもう一方を壊します。
2. **module内部の責務と依存の向き。** 入力を受ける層（Inbound Adapter）、処理を調整する層（Application）、業務規則を持つ層（Domain）、DBや外部APIに触れる層（Outbound Adapter）を分け、依存を一方向に保ちます。
3. **FrameworkとCustomerの境界。** Customer codeはKOIKIの公開APIとStarterを利用します。Rule 013が機械的に検出するのはFrameworkの`internal` packageへの直接依存であり、Public API以外の利用をすべて判定するものではありません。

ルールはpackage名、module宣言、annotation、method signature、class間の静的な依存などを、各Ruleに定義された範囲で確認します。業務的に正しいか、権限やtransactionが正しいかは検査しません（§5）。

## 2. Customer projectへ組み込む

### 2.1 依存とtest class

Customer POMへ`koiki-archunit-rules`を`test` scopeで追加します（[開発環境構築手順§5.1](application-team-development-environment-guide.md#51-pom)）。
そのうえで、次の2つの検査を持つtest classをCustomer projectに置きます。**両方が必要です。**

```java
package jp.example.customer;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Test;
import org.koikifw.archunit.KoikiArchitectureRules;

class CustomerArchitectureTest {

    // 業務moduleを直下に持つpackage。Customerのbase packageに置き換えます。
    private static final String BUSINESS_BASE = "jp.example.customer";

    @Test
    void followsBusinessModuleRules() {
        var classes = new ClassFileImporter().importPackages(BUSINESS_BASE);

        KoikiArchitectureRules.businessModuleRules(BUSINESS_BASE).check(classes);
    }

    @Test
    void usesOnlyFrameworkPublicApi() {
        var classes = new ClassFileImporter().importPackages("org.koikifw", BUSINESS_BASE);

        KoikiArchitectureRules.frameworkOwnershipRules("org.koikifw", BUSINESS_BASE).check(classes);
    }
}
```

| 検査 | 実行されるルール | 省略するとどうなるか |
|---|---|---|
| `businessModuleRules(業務base)` | 業務module向けの23件（§4.1〜§4.7） | module境界、層の向き、Tier構造が検査されません |
| `frameworkOwnershipRules("org.koikifw", Customer base)` | Framework境界の2件（§4.8） | `org.koikifw..internal`への依存（Rule 013）が検出されません |

R2 handoff verificationは、Architecture Rulesのtestが実行されたことも合否条件にしています（[開発環境構築手順§5.2](application-team-development-environment-guide.md#52-bootstrap時に必ず入れるもの)）。

### 2.2 業務baseに渡すpackageの決め方

`businessModuleRules`には、個別のmoduleではなく**業務moduleを直下に並べた親package**を渡します。
ルールは、渡したpackageの**直下にあるpackageをすべて業務moduleとみなします**。

```text
jp.example.customer                    <- BUSINESS_BASEに渡すpackage
├── CustomerApplication.java           <- base直下のclassはmoduleに属さず、module規則の対象外
├── orders                             <- 業務module「orders」
│   └── package-info.java              <- @KoikiModuleが必要
└── billing                            <- 業務module「billing」
    └── package-info.java              <- @KoikiModuleが必要
```

このため、業務base直下に`config`や`common`のような技術的なpackageを作ると、それも業務moduleとして扱われ、
`@KoikiModule`がないためRule 007 / 008で失敗します。業務moduleでないpackageを置きたい場合は、
業務moduleだけを並べる専用の親package（例: `jp.example.customer.business`）を作り、それを`BUSINESS_BASE`にします。

渡したpackageの配下にimport済みclassが1つもない場合、検査は成功ではなく失敗になります。
package名の綴り違いで「何も検査せずに緑になる」ことはありません。

## 3. 違反messageを読む

違反すると、ArchUnitの失敗messageに次の情報が含まれます（前後の書式はArchUnitのversionにより多少異なります）。

```text
... because [KOIKI-ARCH-001] [ADR-022]
影響: Inboundが技術実装へ結合し、依存方向と差替え境界が崩れる
修正: Application Use CaseとPortを介する' was violated (1 times):
違反内容: Method <jp.example.customer.orders.adapter.inbound.api.OrderApiController.create(...)>
  calls method <jp.example.customer.orders.adapter.outbound.persistence.OrderJpaRepository.save(...)>
  in (OrderApiController.java:42)
```

| 部分 | 読み方 |
|---|---|
| `[KOIKI-ARCH-001]` | ルールID。§4の表で探します |
| `[ADR-022]` | 根拠となる設計判断記録（`docs/architecture/adr/`）。「なぜ禁止か」を詳しく知りたいときに読みます |
| `影響:` | 放置すると何が起きるか |
| `修正:` | 直す方向 |
| `違反内容:` | 違反したclass、method、依存先。末尾の`(ファイル名:行)`が修正箇所の目安です |

1つの原因から複数のルールが同時に失敗することがあります。例えば`package-info.java`が無いmoduleは
Rule 007と008の両方で失敗し、MVC handlerがDomain Modelを返すとRule 017と020の両方で失敗します。
最初に出たIDだけでなく、同じclassに出たIDをまとめて見ると原因を特定しやすくなります。

## 4. ルール一覧

表の「対象」は、ルールが適用される条件を示します。

- **全Tier**: SIMPLE / RICHのどちらのmoduleにも適用されます。
- **RICH**: `tier = ModuleTier.RICH`を宣言したmoduleだけに適用されます。
- **SIMPLE**: `tier = ModuleTier.SIMPLE`を宣言したmoduleだけに適用されます。

「package名」の欄に出てくる`adapter.inbound`等は、module root（例: `jp.example.customer.orders`）からの相対名です。
`adapter.inbound`と書いてあれば、`orders.adapter.inbound`とその配下（`orders.adapter.inbound.api`等）が該当します。

### 4.1 moduleの宣言

| ID | 対象 | 違反になる例 | 直し方 | 根拠 |
|---|---|---|---|---|
| 007 | 全Tier | module rootに`package-info.java`がない。`@KoikiModule`の`name`がpackage名と違う（`orders` packageに`name = "order"`） | module rootの`package-info.java`に、package名と同じ`name`と`tier`を持つ`@KoikiModule`を付けます | ADR-022 |
| 008 | 全Tier | `persistence = MYBATIS`を宣言している | 現在は`persistence = JPA`、`persistenceModel = SHARED`を宣言します。`SEPARATED`は現行の`PersistenceModel`に存在せず、指定するとArchUnitより前にコンパイルで失敗します。MyBatis等が必要になった場合はFramework側へ相談します | ADR-022, 023, 039 |

### 4.2 module内部の依存の向き

| ID | 対象 | 違反になる例 | 直し方 | 根拠 |
|---|---|---|---|---|
| 001 | 全Tier | Controller（`adapter.inbound`）がJPA RepositoryやEntity、外部API client（`adapter.outbound`）を直接使う | ControllerはApplicationのUse Caseを呼び、DB等へのaccessはUse Caseの先で行います | ADR-022 |
| 002 | 全Tier | Use Case（`application`）がRequest DTOやForm（`adapter.inbound`）を引数に取る | Use Caseは自分の入力型（`application`内のrecord等）を受け取り、ControllerがDTOから変換します | ADR-022 |
| 006 | 全Tier | `@Controller` / `@RestController`、または`adapter.inbound`内の`*Controller`がRepositoryを参照する | Repositoryの操作をUse Caseへ移します。Spring DataのRepositoryはすべて対象です | ADR-022 |

### 4.3 module間の連携

| ID | 対象 | 違反になる例 | 直し方 | 根拠 |
|---|---|---|---|---|
| 003 | 全Tier | `billing`のclassが`orders`内の、event以外のclassを参照する | 下の「許容される2つの入口」のどちらかに変えるか、処理を所有moduleへ戻します | ADR-041, 049 |
| 009 | 全Tier | `billing`が`orders.application`のUse Caseや`orders.domain.model`を呼ぶ | 他moduleのUse Case / Domain Modelを直接呼ばず、値だけを持つeventで連携します | ADR-025 |
| 021 | 全Tier（参照先がRICH） | 他moduleが、RICH moduleの`domain.model`のclassを参照する | 参照を所有module内へ戻し、module間はeventで連携します | ADR-023, 025 |
| 004 | 全Tier | `orders`→`billing`と`billing`→`orders`の依存が両方ある（間接的な循環も含む） | 依存を一方向にします。**許容されたevent参照も循環の計算に含まれる**ため、互いのeventを参照し合う設計も違反になります | ADR-004, 025 |

**許容される2つの入口（Rule 003の例外）**

1. **他moduleの`domain.event`のclass。** 値だけを持つevent recordを参照するのは許容されます（Rule 010の許容条件）。
2. **狭いread-only contract。** 提供側moduleの`contract` packageにある、名前が`Query`で終わる**interface**を、
   利用側moduleの`adapter.outbound`配下のclassから参照する場合だけ許容されます（ADR-049）。
   利用側のUse Caseは自module内のPortを使い、そのPortの実装（`adapter.outbound`）がcontractを呼びます。
   Referenceの有効master確認だけで承認された例外なので、新しく使う場合は適用範囲をFramework側に確認します。

### 4.4 Tierと構造

| ID | 対象 | 違反になる例 | 直し方 | 根拠 |
|---|---|---|---|---|
| 014 | SIMPLE | SIMPLE moduleに`domain.model`、`domain.service`、`domain.repository`、`domain.gateway`のclassを作る | SIMPLEでは業務判断をUse Caseに置き、JPA EntityとRepositoryは`adapter.outbound`側に置きます。状態遷移や不変条件が増えたならmodule全体をRICHへ変更します | ADR-022 |
| 015 | RICH | `domain`配下のclassが`adapter`、`org.springframework.web`、`EntityManager`を使う | 技術処理をAdapterへ移します。JPAのannotation（`@Entity`等）とSpring Dataの`Repository`は許容されます | ADR-022, 023 |
| 016 | RICH | `domain.repository`のinterfaceが`JpaRepository`を継承する、またはSpring Dataの`Repository`を継承しないclassである | `org.springframework.data.repository.Repository<T, ID>`を継承したinterfaceにし、必要なmethodだけを宣言します | ADR-024 |
| 022 | RICH | `domain.model`のclassに、引数1つのpublicな`set*` methodがある | `submit()`、`approve(reason)`のような業務上意味のあるmethodで状態を変えます | ADR-023 |
| 024 | RICH | `domain.gateway`のinterfaceの実装classを`adapter.outbound.external`以外に置く | 実装classを`adapter.outbound.external`へ移します | ADR-022 |

### 4.5 画面・APIの境界（RICH module）

「handler」は`@RequestMapping`、`@GetMapping`、`@PostMapping`等が付いたmethodです。MVCとRESTの両方が対象です。

| ID | 対象 | 違反になる例 | 直し方 | 根拠 |
|---|---|---|---|---|
| 017 | RICH | `adapter.inbound`のmethodが、引数または戻り値に`domain.model`の型を使う | Request / Response DTO、Form、read modelへ変換します | ADR-023 |
| 018 | RICH | handlerの引数が`domain.model`の型（画面入力をEntityへ直接bindする） | Form / DTOで受け取り、Use Caseに渡して変換します | ADR-023 |
| 019 | RICH | handler内で、Domain Modelを返す呼出しと`model.addAttribute(...)`等を同じ行で組み合わせる | Use Caseのtransaction内でDTO / read modelへ変換し、それをModelへ渡します | ADR-023, 028 |
| 020 | RICH | handlerの戻り値が`domain.model`の型 | view名、DTO、read modelを返します | ADR-023 |

Rule 019は代表的な書き方だけを見る近似検査です（§5）。

### 4.6 eventとlistener

| ID | 対象 | 違反になる例 | 直し方 | 根拠 |
|---|---|---|---|---|
| 011 | 全Tier | `domain.event`のclassが`record`でない、またはfieldに`domain.model`の型を含む | 識別子や金額などの値だけを持つ`record`にします | ADR-025 |
| 038 | 全Tier | `@EventListener`のmethodを`adapter.inbound.event`以外に置く | listener classを`adapter.inbound.event`へ移します | ADR-025 |
| 039 | 全Tier | `adapter.inbound.event`のlistenerが`domain.model`や`domain.repository`を直接使う | listenerは自moduleのUse Caseを呼ぶだけにします | ADR-025 |
| 028 | 全Tier | `@TransactionalEventListener`または`@ApplicationModuleListener`を使う | 現在は同期の`@EventListener`を使います。非同期・commit後処理が必要な場合は自分で導入せず、Framework側へ相談します | ADR-005 |

### 4.7 技術選択

| ID | 対象 | 違反になる例 | 直し方 | 根拠 |
|---|---|---|---|---|
| 012 | 全Tier | `RestTemplate`を使う | Spring標準のHTTP Service Interface方式を使います | ADR-033 |

### 4.8 Framework境界（`frameworkOwnershipRules`）

| ID | 対象 | 違反になる例 | 直し方 | 根拠 |
|---|---|---|---|---|
| 013 | Customer code | `org.koikifw`配下の、package名に`internal`を含むclassを使う | Starter、公開API、Spring標準の機能で実現します。できない場合はFramework側へ相談します | ADR-041 |
| 005 | Framework code | FrameworkのclassがCustomerのclassに依存する | Customer開発で通常は発生しません。発生した場合はFramework側へ連絡します | ADR-014 |

### 4.9 IDの欠番について

現在の違反ルールは上記の25件です。欠番の意味は次のとおりです。

- **010、023:** 違反を出すルールではなく許容条件です。010は§4.3の「他moduleのevent参照」、023はmodule自身の`application.query`が所有するread modelの参照です。
- **025〜027、029〜037:** 現在は存在しません。MyBatis規約（035〜037）や`SEPARATED`向けの規則は、MyBatis採用が決まるまで追加されません。

## 5. ルールが検出しないもの

ルールが成功しても、次は保証されません。code reviewとtestで確認してください。

| 検出しないもの | 理由と対処 |
|---|---|
| 所定の名前でないpackageに置いたclass | package名で役割を判定するRuleは、その役割向けの違反を見落とします。ControllerやRequest DTOを`orders.controller`に置くと、`adapter.inbound`を条件とするRule 001 / 002 / 017 / 018 / 020の対象外になります（Rule 002では、Use CaseがそのDTOに依存しても検出されません）。一方、Rule 006はController annotation、Rule 019はMVC handlerを条件にするため、対象に残る経路があります。[アプリケーション開発者ガイド§2](application-developer-guide.md#2-package構成を決める)のpackage名を使ってください |
| Domain Model / Entityのviewへの間接的な受け渡し | Rule 019は同じ行の直接の受け渡しだけを見ます。helper method、field、別の行に分けた場合は検出されません（[検証記録§10.3](../architecture/validation/phase1a-archunit-rules.md#103-rule-19検証と保証限界)）。HTTP testで応答にEntityが出ないことを確認します |
| SIMPLE moduleでのEntityの露出 | Rule 017〜020はRICHだけが対象です。SIMPLEでEntityを`adapter.outbound`に置くと、Controllerからの直接依存はRule 001で検出されますが、Use Case経由の値の受け渡しまで保証しません。DTOへの変換とHTTP testで確認します |
| 業務規則、権限、Audit、transaction、DBの正しさ | 静的な依存の検査では分かりません。unit test、実DB test、HTTP testで確認します |
| `pom.xml`へのdependency追加 | 追加したlibraryの利用がルールに触れない限り検出しません（例えば`RestTemplate`以外のHTTP clientやMyBatisのlibrary追加自体）。dependencyの追加はreviewで確認します |

Level 2（非同期event）向けのRule 028 / 029候補はTooling上で試験中であり、現行ルールは変わっていません（[PL2検証Evidence](../architecture/validation/phase4-pl2-level2-verification.md)）。

## 6. ルールを外したくなったら

test対象から特定のclassを除外する、ルールを呼ばない、`@Disabled`にする、といった方法で緑にしないでください。
直し方が分からない、または設計上どうしても成り立たない場合は、次をまとめてFramework側へ相談します。

- 失敗したルールIDと`違反内容:`の全文
- 実現したいこと（業務上の目的）と、検討した代替案
- 使用しているKOIKIのversion / commit

相談の結果、ルールやFrameworkの変更が必要と判断された場合は、Framework側のreviewを経て対応します。
