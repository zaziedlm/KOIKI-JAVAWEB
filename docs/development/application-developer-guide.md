# KOIKI アプリケーション開発者ガイド

**状態:** OWNER APPROVED / 2026-09-30（Architecture Owner review完了）。2026-09-30時点のFramework実装と照合済みです。実チームによる試用と、案件固有の設計（認証方式、API仕様等）の反映は未実施です。
**対象:** [開発環境構築手順](application-team-development-environment-guide.md)でCustomer projectの初回buildを通し、これから業務機能を実装する開発者。

## この文書の位置付け

KOIKIを使った開発で読む文書は、次の順につながっています。本書は3番目です。

| 順 | 文書 | 答える問い |
|---|---|---|
| 1 | [引継ぎガイド](application-team-handoff-guide.md) | KOIKIは何を提供し、何を提供しないか。どのStarterを選ぶか |
| 2 | [開発環境構築手順](application-team-development-environment-guide.md) | 端末をどう準備し、Customer projectを最初にbuild・起動するか |
| 3 | **本書** | 業務機能を1つ、どこに何を置いて実装し、どう検証するか |
| 4 | [Architecture Rules説明](architecture-rules-developer-guide.md) | buildが`KOIKI-ARCH-nnn`で失敗したとき、何が問題でどう直すか |

本書は、1つの業務機能を「設計メモ → package作成 → 実装 → 検証 → 完了確認」の順に完成させる流れを説明します。
例には架空の受注業務（`orders`）を使います。class名や属性は説明用であり、業務の標準形ではありません。

本書で扱わないもの:

- 案件固有の認証方式、IdP、BFFとのAPI仕様。これらは実案件の設計で決めます。
- Project Template（Application全体の雛形）。現時点では提供していません。
- MyBatis、非同期event、Spring Modulith Level 2など、Frameworkがまだ採用していない方式。

## 1. KOIKIアプリケーションの形

### 1.1 業務で分け、その中を責務で分ける

KOIKIのApplicationは、1つのSpring Boot Applicationの中を**業務能力ごとのmodule**に分けます（モジュラーモノリス）。
`controller`、`service`、`repository`のような技術層をApplication全体に横断して作るのではなく、
先に`orders`、`billing`のような業務moduleを作り、その**中**を責務で分けます。

```text
jp.example.customer
├── orders          <- 業務module
│   ├── adapter     <- 外界との出入口（HTTP、event受信、DB、外部API）
│   ├── application <- Use Case（transaction、権限、処理順序）
│   └── domain      <- 業務規則（Tier 2のみ本格的に使う）
└── billing         <- 別の業務module
```

### 1.2 依存の向き

module内部の依存は、次の向きに設計します。Architecture Rulesは、所定のpackage配置と静的な依存に対して定義済みの違反を検出します。すべての逆向き依存を検出するわけではないため、検査範囲は[Architecture Rules説明§5](architecture-rules-developer-guide.md#5-ルールが検出しないもの)も確認してください。

```text
Inbound Adapter ──> Application（Use Case） ──> Domain
                         │
                         v
                    Outbound Port <── Outbound Adapter
```

| 層 | 置くもの | 置かないもの |
|---|---|---|
| Inbound Adapter | HTTP / 画面 / eventの受付、入力の形式検証、DTO変換、応答の組立て | 業務規則、Repositoryの操作 |
| Application | transaction境界、権限確認、処理順序、Domainの呼出し、eventの発行 | HTTPの詳細、SQL、画面描画 |
| Domain（Tier 2） | 不変条件、状態遷移、値オブジェクト | Controller、DTO、`EntityManager` |
| Outbound Adapter | DB、外部API、file等の技術詳細 | Use Caseの判断 |

### 1.3 SIMPLEとRICH（Tier）

moduleごとに、業務規則の複雑さに応じてTierを1つ選びます。1つのmodule内でTierを混ぜません。

| Tier | 選ぶ目安 | 業務規則の置き場 | JPA EntityとRepositoryの置き場 |
|---|---|---|---|
| SIMPLE（Tier 1） | 単純なCRUD、マスタ、設定管理 | Application（Use Case） | `adapter.outbound.persistence` |
| RICH（Tier 2） | 3つ以上の状態と遷移規則がある、複数Entityにまたがる不変条件がある、同じ業務規則を複数のUse Caseが使う | `domain.model` | Entityは`domain.model`（Domain Modelと兼用）、Repositoryは`domain.repository` |

迷ったらSIMPLEで始め、上記の条件が現れた時点でmodule全体をRICHへ変えます。判断の詳細は[引継ぎガイド§8](application-team-handoff-guide.md#8-最初のcustomer-owned業務moduleを設計する)を参照してください。

## 2. package構成を決める

Architecture Rulesは**packageの名前**で各classの役割を判定します。下表の名前から外れた場所に置いたclassは、
依存の向きの検査から漏れます。したがって、このpackage構成はReferenceの真似ではなく、KOIKIとの約束事です。

### 2.1 packageの名前と役割

名前はmodule root（例: `jp.example.customer.orders`）からの相対名です。「ルールが判定」列が「はい」の名前は、
Architecture Rulesが役割の判定に使うため、この綴りで作ります。「慣例」はReferenceが使っている推奨名です。

| package | 置くもの | Tier | ルールが判定 |
|---|---|---|---|
| （module root） | `package-info.java`（`@KoikiModule`） | 両方 | はい |
| `adapter.inbound.api` | REST Controller、Request / Response DTO | 両方 | `adapter.inbound`として、はい |
| `adapter.inbound.web` | MVC Controller、Form | 両方 | `adapter.inbound`として、はい |
| `adapter.inbound.event` | 他moduleのeventを受けるlistener | 両方 | はい（listenerはここに限る） |
| `adapter.outbound.persistence` | Tier 1のJPA EntityとSpring Data Repository、JdbcClientによるquery実装 | 両方 | `adapter.outbound`として、はい |
| `adapter.outbound.external` | 外部API等のGateway実装 | RICH | はい（Gateway実装はここに限る） |
| `application` | Use Case class、Use Caseの入出力型、業務例外 | 両方 | はい |
| `application.query` | 画面・API向けread modelとQuery interface | 主にRICH（SIMPLEでは先に作らず、Use Caseの戻り値型で足ります） | はい |
| `application.port.outbound` | Use Caseが使う外向きinterface（Port） | 両方 | `application`として、はい |
| `domain.model` | Domain Model（JPA Entity兼用）、値オブジェクト | RICH | はい |
| `domain.repository` | Domain所有のRepository interface | RICH | はい |
| `domain.service` | 1つのModelに属さない業務判断 | RICH | はい |
| `domain.gateway` | 外部I/Oのinterface | RICH | はい |
| `domain.event` | 他moduleへ公開するevent record | 両方 | はい（他moduleから参照できる入口） |
| `configuration` | Bean定義、module固有の設定 | 両方 | 慣例 |

使わないpackageは作りません。将来のために空のPort、Gateway、Domain Serviceを用意する必要はありません。

### 2.2 SIMPLE moduleの例

```text
jp.example.customer.orders
├── package-info.java                      @KoikiModule(tier = SIMPLE, ...)
├── adapter
│   ├── inbound
│   │   └── api
│   │       ├── OrderApiController.java    Use Caseを呼ぶだけ
│   │       ├── CreateOrderRequest.java    record
│   │       └── OrderResponse.java         record
│   └── outbound
│       └── persistence
│           ├── OrderEntity.java           @Entity
│           └── OrderRepository.java       Spring Data Repository
└── application
    ├── OrderRegistration.java             @Service、@Transactional。業務判断もここ
    └── OrderSummary.java                  Use Caseの戻り値（record）
```

SIMPLEでは、Use Caseが`adapter.outbound.persistence`のRepositoryとEntityを直接使います。
Rule 001はControllerから`adapter.outbound`への**直接依存**を検出しますが、Use Caseを経由したEntityのHTTP応答への露出まで保証しません。Use Case内で応答用の値へ変換し、ControllerはDTOを返します。HTTP testでも応答を確認してください。

### 2.3 RICH moduleの例

```text
jp.example.customer.orders
├── package-info.java                      @KoikiModule(tier = RICH, ...)
├── adapter
│   ├── inbound
│   │   ├── api
│   │   │   ├── OrderApiController.java
│   │   │   └── ...Request / Response.java
│   │   └── event
│   │       └── PaymentCompletedListener.java   他moduleのeventを受けてUse Caseを呼ぶ
│   └── outbound
│       └── persistence
│           └── JdbcOrderListQuery.java    一覧表示用query実装（必要な場合だけ）
├── application
│   ├── OrderApplicationService.java       transaction、権限、処理順序
│   └── query
│       ├── OrderListQuery.java            query interface
│       └── OrderListItem.java             read model（record）
└── domain
    ├── model
    │   ├── Order.java                     @Entity兼用。submit()等の業務methodで状態を変える
    │   └── OrderStatus.java
    ├── repository
    │   └── OrderRepository.java           extends Repository<Order, UUID>
    └── event
        └── OrderSubmitted.java            値だけのrecord
```

### 2.4 `package-info.java`

module rootには、module名、Tier、永続化方式を宣言します。宣言がないとbuildが失敗します（Rule 007 / 008）。

```java
/** 受注業務module。 */
@NullMarked
@KoikiModule(
        name = "orders",
        tier = ModuleTier.SIMPLE,
        persistence = PersistenceTechnology.JPA,
        persistenceModel = PersistenceModel.SHARED)
package jp.example.customer.orders;

import org.jspecify.annotations.NullMarked;
import org.koikifw.architecture.KoikiModule;
import org.koikifw.architecture.ModuleTier;
import org.koikifw.architecture.PersistenceModel;
import org.koikifw.architecture.PersistenceTechnology;
```

- `name`はpackage名の最後の部分と同じにします。
- `persistence`と`persistenceModel`は、現在は`JPA`と`SHARED`だけが使えます。
- `@NullMarked`は、null安全の検査（NullAway）をこのpackageで有効にします。KOIKI ParentはNullAwayを
  `@NullMarked`が付いたpackageだけに適用します。package annotationはsubpackageへ継承されないため、
  検査したいsubpackageにもそれぞれ`@NullMarked`だけを書いた`package-info.java`を置きます。
  nullになり得る値には`@Nullable`（`org.jspecify.annotations`）を付けます。

## 3. 1つの機能を実装する

### 3.1 設計メモを書く

実装の前に、Customer Repositoryへ短い設計メモを残します。問いの一覧は[引継ぎガイド§8](application-team-handoff-guide.md#8-最初のcustomer-owned業務moduleを設計する)にあります。
最低限、次の3点が決まっていれば着手できます。

- **どのmoduleが所有するか。** その機能の状態と規則を持つ業務moduleはどれか。
- **最初に通す操作。** 成功する操作1つと、拒否される操作1つ。それぞれ実行後にDBとAuditで何を確認するか。
- **Tier。** SIMPLEかRICHか、その理由。

認証方式やAPIのpath / error形式など、案件でまだ決まっていないことは推測で埋めず、担当者と確認先を書いておきます。

### 3.2 moduleとtableを作る

1. §2.4の`package-info.java`を置き、module rootを作ります。
2. Customerのmigrationを`src/main/resources/db/migration/customer/`へ置きます（例: `V1__create_orders.sql`）。
   - KOIKIのData Starterは、この場所をCustomer migrationの既定とし、履歴を`flyway_schema_history`に記録します。
     Framework自身のmigrationは別の場所・別の履歴tableで管理されるため、Customer側で混ぜません。
   - 一度適用したmigration fileは書き換えず、変更は新しいversionのfileで行います。
   - Framework所有tableと名前が衝突しないよう、接頭辞などの命名規則をCustomer側で決めます。
   - JPAにschemaを自動生成させません（`ddl-auto=validate`を維持）。
3. JPA Entityを作ります。
   - SIMPLE: `adapter.outbound.persistence`に`@Entity`とSpring Data Repositoryを置きます。
   - RICH: `domain.model`のclassに`@Entity`等のJPA annotationを付けて兼用します。publicなsetterは作らず、
     `submit()`や`approve(reason)`のような業務methodで状態を変えます（Rule 022）。
     Repositoryは`domain.repository`に、`org.springframework.data.repository.Repository<T, ID>`を継承したinterfaceとして置き、
     必要なmethodだけを宣言します（`JpaRepository`は継承しません。Rule 016）。
   - 同時更新を検出する必要があるEntityには`@Version`を付けます。

### 3.3 Use Caseを作る

`application`にUse Case classを作ります。

- `@Service`と`@Transactional`を付けます。Springがproxyを作れるよう、classと対象methodを`final`にしません。
- 権限確認はUse Caseで行います（例: `@PreAuthorize("hasAuthority('ORDER:WRITE')")`）。Permission名はCustomerが決めます。
- 業務Auditを記録する場合は、Audit Starterの`BusinessAuditRecorder`をUse Caseから呼びます。何をAuditに残すかはCustomerが決めます。
- 引数と戻り値は`application`内の型（recordなど）にします。ControllerのRequest DTO（`adapter.inbound`）を受け取りません（Rule 002）。
- SIMPLEでは業務判断をここに書きます。RICHでは判断をDomain Modelのmethodへ委ね、Use Caseは呼出し順序とtransactionに徹します。

### 3.4 入口（Controller）を作る

REST APIの場合は`adapter.inbound.api`、MVC画面の場合は`adapter.inbound.web`に置きます。

- ControllerはRequest DTO / Formを受け取り、入力の形式を検証し、Use Caseを呼び、Response DTO / view名を返すだけにします。
- Repositoryを直接呼びません（Rule 006）。Entity / Domain Modelを引数・戻り値・画面Modelへ渡しません。Rule 017〜020はRICH moduleの検査であり、SIMPLE moduleでもDTOへの変換とHTTP testで露出を確認します。
- `koiki-starter-api`を使うREST APIでは、pathを`@RequestMapping("/api/v{version:[1-9][0-9]*}/...")`とし、
  handlerに`version = "1"`を宣言します（[開発環境構築手順§5.2](application-team-development-environment-guide.md#52-bootstrap時に必ず入れるもの)）。
- 業務例外からHTTP応答への変換は、Controllerごとに書かず、module単位の`@RestControllerAdvice` / `@ControllerAdvice`にまとめます。
- BFFから呼ばれるAPIのpath、error code、認証情報の受け渡しは、実案件のAPI設計に従います。

### 3.5 他のmoduleと連携する（必要な場合だけ）

他moduleのUse Case、Repository、Entityは直接呼べません（Rule 003 / 009 / 021）。連携はeventで行います。

1. 送る側: `domain.event`に値だけを持つ`record`を作り（Rule 011）、Use Caseから`ApplicationEventPublisher`で発行します。
2. 受ける側: `adapter.inbound.event`にlistenerを置き（Rule 038）、同期の`@EventListener`で受けて、自moduleのUse Caseを呼びます（Rule 039）。
3. 受け手が送り手のDBを読みに行かなくて済むよう、必要な識別子と値をeventに含めます。
4. 2つのmoduleが互いのeventを参照し合うと循環になります（Rule 004）。どちらからどちらへ流れるかを先に決めます。

`@TransactionalEventListener`や非同期処理は使いません（Rule 028）。commit後の処理や非同期配信が必要になった場合は、Framework側へ相談します。

### 3.6 外部APIやfileを扱う（必要な場合だけ）

- 技術処理はOutbound Adapterに置きます。RICHでは`domain.gateway`にinterfaceを、`adapter.outbound.external`に実装を置きます（Rule 024）。
- HTTP clientは`RestTemplate`を使わず（Rule 012）、Spring標準のHTTP Service Interface方式を使います。
- timeout、再試行、重複実行、失敗時のAuditの扱いを設計メモに書きます。

## 4. 検証する

### 4.1 何をtestで確かめるか

| 確かめること | test | 主な確認点 |
|---|---|---|
| 業務規則 | Domain Model / Use Caseのunit test | 成功時の状態、拒否時に状態が変わらないこと |
| DBとmigration | 実DB（PostgreSQL）を使うintegration test。Testcontainersを使えます | migrationが適用できる、Entityのmappingが正しい、rollback後にdataが残らない、`@Version`で競合を検出する |
| HTTP / 画面 | MockMvc等によるtest | 入力不正、権限不足（401 / 403）、成功、競合（409）の応答。応答にEntityの項目が出ていないこと |
| Security / Audit | 上記testの中で確認 | 選択した認証profileで既定の拒否（default deny）が保たれている、Auditに主体・操作・結果が残る |
| 構造 | Architecture Rules test | [Architecture Rules説明§2](architecture-rules-developer-guide.md#2-customer-projectへ組み込む)のtestが成功する |

Architecture Rulesの成功は、構造が約束どおりであることを示すだけです。業務規則や権限の正しさは上の他のtestで確かめます。

### 4.2 日々のbuildとIDE

現時点の手順と、その限界は次のとおりです。

- **buildとtestの合否は、R2 handoff verification（[開発環境構築手順§6](application-team-development-environment-guide.md#6-初回buildを実行する)）の結果で判断します。** R2は実行のたびに新しいstageを作り、終了時に削除します。
- R2の成功だけでは、IDE上のKOIKI依存は解決されません。IDEの補完やerror表示が実際のbuildと食い違うことがあります（[同§8.3](application-team-development-environment-guide.md#83-ide上のkoiki依存解決と日常操作)）。
- KOIKI artifactを各自の`~/.m2`へinstallする方法や、日常開発用に残しておくstageは、現在の手順に含まれません。

**IDEでの依存解決と、編集からtestまでを素早く繰り返す方法は、Framework側で方式を整理中です。**
決まるまでに困ったこと（IDEで起きた症状、R2の所要時間など）は、§5.2の形式でFramework側へ伝えてください。方式の検討に使います。

## 5. 詰まったとき

### 5.1 症状から確認先を選ぶ

| 症状 | 最初に見るもの | 参照 |
|---|---|---|
| `KOIKI-ARCH-nnn`でbuildが失敗する | ルールIDと`違反内容:` | [Architecture Rules説明§3〜§4](architecture-rules-developer-guide.md#3-違反messageを読む) |
| `[NullAway]`でcompileが失敗する | 指摘された行の値がnullになり得るか | §2.4。nullになり得るなら`@Nullable`を付けて扱いを書き、そうでなければ値を保証します |
| dependencyが解決しない / buildが始まらない | JDK 21、Wrapper、KOIKIのversion、Framework checkoutがcleanか | [開発環境構築手順§10](application-team-development-environment-guide.md#10-トラブルシューティング) |
| 起動しない / DB・migrationのerror | DB接続設定、Flywayの履歴、JPA validation | [引継ぎガイド§11](application-team-handoff-guide.md#11-まず診断しその後で対応するownerを決める) |
| 401 / 403 | 選択した認証profile、Permission、CSRF | [引継ぎガイド§11](application-team-handoff-guide.md#11-まず診断しその後で対応するownerを決める) |
| 409 | `@Version`、画面の再読込導線 | 同上 |

どの症状でも、Securityの既定、migration、JPA validation、Architecture Rulesを無効にして先へ進むことはしません。

### 5.2 Framework側へ相談する

Customerの実装で直せない、またはKOIKIの公開範囲では実現できないと判断したら、次の形でFramework側へ相談してください。
顧客名や業務data、secretは含めません。

```text
件名: （例）RICH moduleのlistenerからRule 039で失敗する

1. 実現したいこと（業務上の目的）:
2. 使用しているKOIKIのversion / Framework commit:
3. 何をしたか（最小の再現手順、関係するpackage構成）:
4. 期待した結果:
5. 実際の結果（ルールID、error message、logの該当部分）:
6. 自分で試した回避策と、その結果:
```

相談の内容は、公開済みの使い方の説明、Customer側で設計すること、Frameworkの不足（gap）のいずれかに分類して回答します。
Frameworkの変更が必要な場合は、Framework側のreviewを経て対応します。

## 6. してはいけないこと

次は、一時的に動かすためであっても行いません。理由と代わりの方法は[引継ぎガイド§9〜§10](application-team-handoff-guide.md#9-referenceやtoolingからコピーせず自案件で設計するもの)にあります。

- ReferenceやToolingの業務class、migration、demo user、test keyをCustomer projectへコピーする。
- `org.koikifw`配下の`internal` package、FrameworkのEntity / Repositoryを使う。
- `@Primary`やbean定義の上書きでFrameworkのbeanを差し替える。
- 個別dependencyのversionを上書きする。KOIKI Parent / BOMが管理するversionを使います。
- Architecture Rulesのtestを外す、対象classを除外する。

## 7. 機能完了の確認

- [ ] 設計メモに、所有module、Tierとその理由、公開する入口を書いた。
- [ ] module rootに`@KoikiModule`があり、package名が§2.1に沿っている。
- [ ] 成功と拒否の操作について、DB、HTTP応答、Auditをtestで確認した。
- [ ] Architecture Rules testの2つの検査が成功している。
- [ ] Framework / Referenceのcode、migration、fixtureをコピーしていない。
- [ ] 未決定の認証・API・運用事項に、担当と確認先を書いた。

## 8. FAQ

問い合わせへの回答は本節へ蓄積します。問い合わせが増えたら独立文書へ分け、本節から参照します（2026-10-01 Architecture Owner判断）。
§5.2に沿って受けた問い合わせのうち、回答で繰り返し現れた論点を追加します。現在、FAQ項目は未登録です。
