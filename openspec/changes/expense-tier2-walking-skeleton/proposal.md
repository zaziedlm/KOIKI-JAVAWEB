## Why

ArchUnit V1により、KOIKIの構造規約を配布し強制できることは確認できた。一方、小規模なTier 2モジュールを現実的な規模で実装できるか、JPAモデルのView層への露出防止と同期モジュールイベントのロールバックが設計どおり機能するかは未検証である。Phase 1の実装判断を確定する前に、必要最小限の`expense` / `masterdata`実験で実装根拠を得る。

## What Changes

- JPA共有モデルを採用するTier 2 RICHの`expense`と、検証に必要な最小限のTier 1 SIMPLEの`masterdata`を含む、使い捨てのWalking Skeletonアプリケーションを追加する。
- 経費申請と明細を持つ最小集約を実装し、明細合計と申請金額の一致、およびDraftからSubmitted、ApprovedまたはRejectedへの代表的な状態遷移を検証する。
- `masterdata`によるカテゴリ無効化時に値のみの同期ドメインイベントを発行し、`expense`が未処理経費からの参照を検査するモジュール間連携を実装する。
- 未処理経費から参照されている場合に、受信側の拒否がカテゴリ無効化トランザクションをロールバックすることを検証する。
- OSIVを無効化し、DTO/read modelを使用する正規のView経路と、意図的にEntityを露出させる失敗経路を、MVCレンダリングテストで検証する。
- 配布済みKOIKI ArchUnitルールとSpring Modulithのモジュール検証をアプリケーションへ適用し、Tier境界、モジュール依存、およびイベント公開境界を確認する。
- 本番コードとテストコードのクラス数を計測し、V5/V6の結果を`docs/architecture/validation/`へ記録する。

## Capabilities

### New Capabilities

- `expense-lifecycle`: 最小のTier 2経費集約、その永続化、業務不変条件、および代表的な申請状態遷移。
- `expense-view-boundary`: OSIV無効環境でのMVCレンダリングと、ドメインモデルをView境界の外へ露出させてはならないことの実行証拠。
- `module-event-collaboration`: カテゴリ無効化前の参照検査を行う`masterdata` / `expense`間の同期ドメインイベント連携と、トランザクションのロールバック伝播。
- `application-module-verification`: 宣言したTier境界、モジュール依存、およびイベント公開境界に対するArchUnitとSpring Modulithの検証。

### Modified Capabilities

なし。この試行リポジトリには、変更対象となる既存のOpenSpec基準仕様は存在しない。

## Impact

- `walking-skeleton/`配下に使い捨ての実験モジュールを追加する。このモジュールは`koiki-reference-app`ではなく、正式なFrameworkまたはReferenceコードへ直接昇格させない。
- この実験のビルドとテストに必要な範囲だけ、ルートMavenリアクターを拡張する。
- 検証上必要と確認されたSpring MVC、Thymeleaf、JPA、Spring ModulithおよびTestcontainersのテスト・実行時依存関係を追加する。
- 検証済みのKOIKI / Customer間のFlyway所有権判断を変更せず、このWalking Skeletonアプリケーション自身が所有するPostgreSQLマイグレーションを使用する。
- V5またはV6が成立しない場合に、グランドデザインを更新するための検証証拠を生成する。

## Non-goals

- 本番利用可能な完全な経費システム、正式なReference Application、認証、認可、監査Framework、またはフロントエンドアプリケーションの構築。
- 差戻し、精算などを含む経費ライフサイクル全体の実装。
- 未使用の将来モジュール、汎用Framework抽象、または正式なFlyway Starter実装の先行作成。
- Walking SkeletonのJavaクラスまたはSQLをPhase 1の製品コードへ直接持ち込むこと。
- 非同期イベント、Spring Modulith Level 2、MyBatis、またはTier 2分離永続化モデルの検証。
- REST API、API Versioning、JSON・Problem Details契約、認証・認可、およびReact SPA連携の検証。
