## Context

動機と変更範囲は`proposal.md`、検証する振る舞いは4つのdelta specを参照する。リポジトリではJava 21のMaven基盤、配布可能なArchUnitルール、および所有者を分離したFlyway二階層構成を検証済みだが、実行可能な業務アプリケーションはまだ存在しない。

この変更では、将来の顧客アプリケーションで想定する制約を実際に検証しながら、コードを`walking-skeleton/`配下の使い捨て実験として明確に隔離する。主要な根拠は、Tier 1 / Tier 2構造（ADR-022、グランドデザイン§11）、JPA共有モデル（ADR-023、§11.6）、Repository方針（ADR-024、§11.3）、同期イベント（ADR-025、§17.3）、OSIV無効化（ADR-028、§13.3.3および§22.1）、Phase 3の`master` / `expense`連携例（§26.3）、ならびにV5/V6（§27.1）である。

## Goals / Non-Goals

**Goals:**

- 最小のRICHモジュールについて、業務不変条件、状態遷移、永続化、View境界、およびモジュール間連携を一つの実行可能な検証へまとめる。
- 同期イベントが送り手の業務成立条件として機能し、受け手の業務拒否によって同一トランザクションがロールバックされることを確認する。
- KOIKIの公開規約とSpring Modulithが認識する公開境界の差異を実装で確認する。
- Framework所有の契約、Walking Skeletonアプリケーションのコード、およびアプリケーション所有のDBオブジェクトを区別する。
- Mavenテストと検証文書から、V5/V6およびTier 2の実装規模を再現可能に評価できるようにする。

**Non-Goals:**

- 完成度の高いサンプルアプリケーションまたは再利用可能な経費APIを設計すること。
- Walking Skeletonで選択したSpring Modulith公開方法をPhase 1bの正式規約として確定すること。
- 永続化、イベント発行、MVC変換をKOIKI Frameworkの汎用抽象へ昇格させること。
- セキュリティ、監査、非同期イベント、Spring Modulith Level 2、HTMX、または本番運用を検証すること。

## Decisions

### 1. 使い捨ての単一Spring Bootモジュールとする

`walking-skeleton/ws-tier2-practicality`を一つの実行可能なMavenモジュールとして作成し、ルートMavenリアクターにはこのモジュールだけを追加する。アプリケーションルート直下に`expense`と`masterdata`を兄弟パッケージとして配置し、各ルートの`package-info.java`で既存の`@KoikiModule`を宣言する。

`expense`はRICH / JPA / SHARED、`masterdata`はSIMPLE / JPAとして宣言する。実際に利用するパッケージだけを作成し、正式な`koiki-reference-app`や将来モジュールは先行生成しない。

別々のMavenモジュールにする案は、ADR-022が選択したパッケージ単位のモジュラーモノリスではなくビルド時分離を検証することになり、構成とクラス数も増やすため採用しない。

### 2. expenseは最小のJPA共有集約とする

`ExpenseRequest`を集約ルート兼JPA Entityとし、`ExpenseLine`、`Money`値オブジェクト、および`ExpenseStatus`を持たせる。明細は1件以上とし、各金額を正に保つ。`submit()`は明細合計と申請金額の一致を検証し、状態遷移メソッドは`Draft`から`Submitted`、`Submitted`から`Approved`または`Rejected`だけを許可する。状態を外部から変更できるpublic setterは設けない。

明細CollectionはLazy Loadingとする。これは通常経路ではトランザクション内で完全に読み出してViewデータへ変換し、検証専用経路ではOSIV無効化によるEntity露出の失敗を実証するためである。明細をEager Loadingにする案はV5の失敗条件を再現できないため採用しない。

差戻し、精算、自己承認禁止、楽観ロック競合画面、および複数集約にまたがる一覧read modelは、Phase 0の最小検証に必要ないため実装しない。

### 3. domain.repositoryはSpring Dataに実装させる

Repositoryインターフェースを`expense.domain.repository`へ配置し、Spring Data Commonsの`Repository<ExpenseRequest, ExpenseRequestId>`を継承して必要なメソッドだけを宣言する。Spring Data JPAがProxy実装を生成するため、手書きのRepository Adapterは作成しない。domainから`JpaRepository`、`EntityManager`またはSpring Webを参照しない。

これはグランドデザイン§11.3およびADR-024の共有JPAモデル方針に従う。`adapter.outbound.persistence`に手書き実装を置く方式は、MyBatisまたは分離永続化モデルの構造と混同するため採用しない。独自実装が必要な複雑クエリも今回のスコープには含めない。

Tier 1の`masterdata`はDomain層とOutbound Portを持たず、Application Use Caseが`adapter.outbound.persistence`に置くSpring Data Repositoryを直接利用する。カテゴリの永続化モデルは振る舞いを持たないデータモデルとし、無効化の業務判断はApplication Use Caseが担う（§11.2）。

### 4. カテゴリ無効化を同期イベントで事前検査する

`masterdata`のカテゴリ無効化Use Caseを一つの`@Transactional`処理とする。Use Caseは有効なカテゴリを取得し、`masterdata.domain.event.CategoryDeactivating`を`ApplicationEventPublisher`で同期発行する。イベントはカテゴリ識別子だけを持つ不変な`record`とし、JPA Entityを含めない（§11.8）。受信処理が成功した後でカテゴリを無効状態にして保存し、トランザクションをコミットする。

`expense.adapter.inbound.event`の薄い`@EventListener`がイベントを受信し、`expense.application`の参照検査Use Caseへ委譲する。参照検査は`expense`自身のRepositoryだけを使用して、対象カテゴリを参照する`Draft`または`Submitted`の経費申請が存在するか確認する。存在する場合は業務例外を送出するため、イベント発行元まで例外が伝播し、カテゴリ無効化トランザクション全体がロールバックされる。

この方向では`masterdata`は`expense`を知らず、直接Bean呼出も行わない。一方、受け手である`expense`は公開イベント契約だけを参照する。受け手の成功がカテゴリ無効化の成立条件なので、グランドデザイン§17.3の同期選択基準および§26.3の例に直接合致する。

「経費承認後にカテゴリ利用件数を更新する」案は派生処理であり、同期イベントの業務的必要性を実証できないため採用しない。障害注入用の失敗スイッチも作らず、未処理経費からの参照という実際の業務条件でロールバックを発生させる。`@TransactionalEventListener`、非同期実行、および永続Event Publication Registryは対象外とする。

### 5. domain.eventだけをSpring Modulithの公開境界として試行する

KOIKIでは`masterdata.domain.event`をモジュールの公開パッケージとし、それ以外の`domain`、`application`および`adapter`を非公開とする（§10.2、§11.8）。一方、Spring Modulithは既定ではモジュール直下をAPIとみなすため、`domain.event`を明示的に公開しなければ`expense`からの参照を不正と判定する可能性がある。

このWalking Skeletonでは、`masterdata.domain.event`の`package-info.java`へ`@NamedInterface("events")`を付与し、必要に応じて`expense`側の許可依存を`masterdata::events`へ限定する方式を試行する。アノテーション利用に必要な`spring-modulith-api`だけを本番コンパイル依存とし、`ApplicationModules.of(...).verify()`などの検証機能はテスト依存に置く。

イベントを`masterdata`のルートパッケージへ移す案はKOIKIの`domain.event`規約を崩し、Spring Modulith検証を無効化または緩和する案は公開境界を検証する目的を失うため採用しない。ただし、`@NamedInterface`の採用はPhase 1bで判断する事項であるため、この試行結果だけで正式規約へ固定しない。追加依存、検証結果、およびKOIKI規約との摩擦を検証文書へ記録する。

### 6. 正規View経路と検証専用の失敗経路を分離する

`spring.jpa.open-in-view=false`を通常構成で明示する。正規の経費詳細Use Caseはトランザクション内で集約と明細を取得し、必要な全項目を不変なexpense detail view recordへ変換する。MVC ControllerとThymeleaf TemplateはこのViewデータだけを扱い、JPA EntityをMVCモデルへ格納しない。

実行時防御の検証には、テスト用ControllerとTemplateからなる検証専用経路をテストソース側へ置く。この経路は未初期化の明細Collectionを持つEntityをトランザクション終了後にTemplateへ渡し、描画時に失敗することを確認する。テストは単なるHTTPエラーではなく、事前に関連が未初期化であることと、例外の原因連鎖に`LazyInitializationException`が含まれることを確認する。

これとは別に、JPA EntityをMVC境界へ直接渡す意図的なArchUnit違反fixtureをテストソースへ置き、構造検出も確認する。正規の本番コードへ違反経路やテスト用失敗スイッチを混入させない。

### 7. PostgreSQLでトランザクションと永続化を検証する

Repository、状態遷移、およびロールバックの統合テストにはTestcontainers PostgreSQLを使用する。このWalking Skeletonアプリケーションが所有するFlyway migrationに、経費申請、経費明細、およびカテゴリの最小テーブルだけを定義する。Framework所有migrationまたはKOIKI / Customerの二階層構成をこの実験へ再実装しない。

H2はPostgreSQLとの差異があり、トランザクション、DDLおよびLazy Loadingについてサポート対象DBと同じ証拠にならないため採用しない（§16.8、§21.5）。

### 8. KOIKIとSpring Modulithを独立して検証する

Walking Skeletonの本番クラスへ`KoikiArchitectureRules.phaseZeroRules(...)`を適用する。意図的な負例は本番パッケージと分離したtest fixtureとして検査する。これとは別にSpring Modulithのアプリケーションモデルを構築し、循環、不正なモジュール依存、および非公開型参照がないことを`verify()`で確認する。

両者の検出範囲が重なる場合も、一方へ統合したり、検証を通すためにKOIKIの公開範囲を広げたりしない。KOIKIルールをTier詳細の正本とし、Spring Modulithとの差異を実装証拠として記録する。

### 9. 検証結果と規模を記録する

Mavenテストを実行可能な受入手段とする。成功後、`expense`、`masterdata`、アプリケーション共通部分、および検証専用fixtureについて、本番Javaクラス数とテストJavaクラス数を計測する。

依存関係とバージョン、V5、V6、Spring Modulith公開境界、クラス数、実装上の摩擦、制約、およびPhase 1へ引き継ぐ判断を`docs/architecture/validation/walking-skeleton-tier2-practicality.md`へ記録する。

## Risks / Trade-offs

- **[最小実装でもLazy Collectionによりクラスとテストが増える]** → V5を実際の描画まで検証するために必要なコストとして所有領域別に計測し、Tier 2の実用性判断へ含める。
- **[Spring Modulithの公開モデルとKOIKIの`domain.event`規約が一致しない可能性がある]** → `@NamedInterface`を限定的に試行し、依存と検証結果を記録する。正式採用はPhase 1bまで保留する。
- **[同期イベントは受け手の障害を送り手へ波及させる]** → この検証では業務的なFail Closed要件として意図的に採用し、外部I/Oをlistenerへ置かない。
- **[Rule 19は完全なバイトコードデータフロー解析を行えない]** → 代表的な直接露出fixtureと、OSIV無効状態の実レンダリングテストを組み合わせる。
- **[検証専用経路が本番設計へ混入する可能性がある]** → Controller、Template、fixtureをテストソースへ隔離し、本番コードには正規View経路だけを置く。
- **[OpenSpec成果物とルートのAgent指示が重複する]** → OpenSpecはこのchangeの計画正本、ルート`AGENTS.md`はリポジトリ全体の統治指示として役割を分け、競合があれば検証文書へ記録する。

## Migration Plan

本番環境へのデプロイmigrationはない。使い捨てモジュールとアプリケーション所有テーブルを追加して対象テストとMavenリアクター全体を実行し、検証結果を記録する。Phase 0完了時にはWalking Skeletonモジュール、そのmigration、および試行changeを削除できる状態を保つ。

Phase 1へ持ち込めるのは、検証済みの設定、アーキテクチャルール、および文書化した判断だけとする。Walking SkeletonのJavaクラスとSQLを製品コードへ直接移植しない。
