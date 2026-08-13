# application-module-verification Specification

## Purpose

使い捨てのWalking Skeletonアプリケーションが、レビューだけに依存せず、KOIKIのTier宣言、モジュール依存方向、およびSpring Modulithが認識する公開境界へ適合することを実行可能な検証で確認する。

## Requirements

### Requirement: 各業務モジュールがKOIKI Tierを宣言する

実験対象の各業務モジュールはルートパッケージでKOIKIモジュールメタデータを宣言し、`masterdata`をSIMPLE、`expense`をJPA共有永続化モデルのRICHとして定義しなければならない（`SHALL`）。

#### Scenario: KOIKI Phase 0ルールを適用する

- **WHEN** 配布済みKOIKI Phase 0ルールがWalking Skeletonアプリケーションを検査する
- **THEN** モジュール、レイヤ、Tier、イベントリスナー、およびモデル境界に関する適用可能なルールがすべて成功する

### Requirement: Spring Modulithがモジュール境界を検証する

検証スイートは、Walking Skeletonアプリケーションのモジュールモデルを構築し、循環依存、不正なモジュール依存、および公開されていない型への参照がないことを検証しなければならない（`SHALL`）。

#### Scenario: アプリケーションモジュールを検証する

- **WHEN** アプリケーションルートに対するSpring Modulithのモジュール検証を実行する
- **THEN** 循環依存、不正なモジュール依存、またはアクセス不能なモジュール参照が報告されない

### Requirement: モジュール間イベントの公開範囲を限定する

`masterdata`は、`expense`によるカテゴリ無効化前の参照検査に必要な不変のイベント契約だけを公開し、それ以外のapplicationおよびdomain内部型を`expense`から参照可能にしてはならない（`SHALL`）。

#### Scenario: 公開されたイベント契約を参照する

- **WHEN** `expense`の受信処理が`masterdata`のカテゴリ無効化イベントを参照する
- **THEN** Spring Modulithのモジュール検証はその参照を正当な公開境界として受理する

#### Scenario: masterdata内部型を公開しない

- **WHEN** Spring Modulithが`masterdata`の公開境界を評価する
- **THEN** イベント契約以外のapplicationおよびdomain内部型は`expense`からアクセス可能なAPIとして公開されない

### Requirement: 実装規模を計測する

検証結果は、`expense`、`masterdata`、アプリケーション共通部分、および検証専用fixtureについて、本番Javaクラス数とテストJavaクラス数を報告しなければならない（`SHALL`）。

#### Scenario: 実装規模を記録する

- **WHEN** Walking Skeletonの全テストが成功する
- **THEN** 検証文書に所有領域別の本番クラス数とテストクラス数を記録する
- **AND** テスト専用またはFramework連携のためだけに必要となったクラスを識別する
