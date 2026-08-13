## 1. モジュールと依存関係の基盤

- [x] 1.1 `walking-skeleton/ws-tier2-practicality`をMavenリアクターへ追加し、Spring Boot MVC、Thymeleaf、JPA、Flyway、PostgreSQL、Testcontainers、ArchUnit、およびSpring Modulithの必要最小限の依存関係を設定する。
- [x] 1.2 アプリケーションルートと`expense`、`masterdata`のモジュールルートだけを作成し、KOIKI Tier・永続化メタデータを宣言してOSIVを無効化する。
- [x] 1.3 空のアプリケーションコンテキストがJava 21で起動することをテストする。
- [x] 1.4 経費申請、経費明細、およびカテゴリのアプリケーション所有Flyway migrationを追加し、Testcontainers PostgreSQLへの適用を検証する。

## 2. expense Tier 2集約と永続化

- [x] 2.1 `Money`、`ExpenseStatus`、`ExpenseLine`、`ExpenseRequest`からなるJPA共有集約を実装し、public setterを設けず、正の金額と1件以上の明細を保証する。
- [x] 2.2 `submit()`で明細合計と申請金額の一致を検証し、`Draft → Submitted → Approved / Rejected`の合法な状態遷移だけを実装する。
- [x] 2.3 `expense.domain.repository`にSpring Data Commonsの`Repository<T, ID>`を継承するRepository契約を定義し、手書きのJPA Repository Adapterを作成せずSpring Data生成実装で永続化する。
- [x] 2.4 作成、申請、承認、および却下を行うトランザクション境界付きApplication Use Caseを実装する。
- [x] 2.5 正常な不変条件と状態遷移、および金額、明細合計、状態に関する拒否ケースのDomainテストを追加する。
- [x] 2.6 集約とLazy明細Collectionの保存・再取得をTestcontainers PostgreSQLで検証する。

## 3. masterdata Tier 1と同期イベント契約

- [x] 3.1 `masterdata`に最小のカテゴリ永続化モデル、Spring Data Repository、およびカテゴリ無効化Application Use CaseをTier 1構造で実装する。
- [x] 3.2 `masterdata.domain.event`にカテゴリ識別子だけを持つ不変な`CategoryDeactivating`イベントを定義する。
- [x] 3.3 `masterdata.domain.event`を`events` Named Interfaceとして試行し、`expense`からの許可依存をその公開境界へ限定する。
- [x] 3.4 カテゴリ無効化Use Caseからイベントを同期発行し、受信処理の成功後だけカテゴリを無効状態で保存する。

## 4. expenseによるカテゴリ参照検査

- [x] 4.1 対象カテゴリを参照する`Draft`または`Submitted`状態の経費申請が存在するか検査するexpense Application Use CaseとRepository queryを実装する。
- [x] 4.2 `expense.adapter.inbound.event`に同期listenerを配置し、業務ロジックを持たせず参照検査Use Caseへ委譲する。
- [x] 4.3 未処理経費から参照されていないカテゴリの無効化がコミットされ、イベントが1回だけ処理されることを統合テストで確認する。
- [x] 4.4 `Draft`から参照されているカテゴリの無効化が拒否され、カテゴリが有効状態へロールバックされることを確認する。
- [x] 4.5 `Submitted`から参照されているカテゴリについても同じ拒否とロールバックを確認する。

## 5. MVCとOSIV境界

- [x] 5.1 不変な経費詳細view/read modelと、トランザクション内で集約をViewデータへ変換するQuery Use Caseを実装する。
- [x] 5.2 JPA EntityをMVCモデルへ格納しない正規ControllerとThymeleaf Templateを実装し、明細を含む詳細画面の正常レンダリングをテストする。
- [x] 5.3 MVCハンドラまたはMVCモデルへEntityを直接露出する意図的なArchUnit違反fixtureを追加し、KOIKIルール19による検出を確認する。
- [x] 5.4 テスト専用ControllerとTemplateで未初期化のLazy明細へトランザクション終了後にアクセスし、原因連鎖に`LazyInitializationException`を含むレンダリング失敗を確認する。

## 6. アーキテクチャとモジュール検証

- [x] 6.1 Walking Skeletonの本番クラスへ`KoikiArchitectureRules.phaseZeroRules(...)`を適用し、該当するV1ルールをすべて成功させる。
- [x] 6.2 Spring Modulithの`ApplicationModules.of(...).verify()`を実行し、循環、不正依存、および非公開型参照がないことを確認する。
- [x] 6.3 Spring Modulithが`masterdata::events`を公開境界として認識し、それ以外の`masterdata`内部型を公開APIとして扱わないことを検証する。
- [x] 6.4 Java 21で対象モジュールのテストとMavenリアクター全体を実行し、再現性に必要な依存関係またはツール調整を記録する。

## 7. 検証証拠とOpenSpec評価

- [x] 7.1 `expense`、`masterdata`、アプリケーション共通部分、および検証専用fixtureごとに、本番・テストJavaクラス数を計測する。
- [x] 7.2 `docs/architecture/validation/walking-skeleton-tier2-practicality.md`を作成し、V5/V6、クラス数、Spring Modulith公開境界、実装上の摩擦、制約、およびPhase 1への引継ぎ判断を記録する。
- [x] 7.3 実行証拠に基づいてWalking Skeleton実装チェックリストを更新し、OpenSpecの指示とルート`AGENTS.md`または将来の`docs/agent/skills/`との責務衝突がないか評価する。
- [x] 7.4 OpenSpec changeをstrict validationし、各完了タスクを対応するテストまたは検証文書の証拠と照合する。
