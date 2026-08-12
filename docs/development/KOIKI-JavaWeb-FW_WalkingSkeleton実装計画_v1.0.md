# KOIKI-JavaWeb-FW Walking Skeleton 実装計画（実行版）

**版:** v1.0（最終整理版。マスタープラン／v0.1／v0.2を本書に統合し、以後は本書を起点とする）
**作成日:** 2026年7月27日
**位置づけ:** グランドデザイン v0.2 §27.3 の実行計画。一人プロジェクトのため、承認ゲートや複数文書間の版管理は行わず、本書1本で完結させる

---

## 0. スタンス

- グランドデザインとRepository Architectureが出した技術的な結論（後述）はそのまま採用する
- 「Proposed／Accepted」「M0」「RAxx」といった手続き上の仕組みは廃止する。**決めるのも実行するのも自分なので、判定はそのまま実装の合否で行う**
- 迷ったら「まず作って試す」を優先する。Walking Skeletonの目的自体がそれである

---

## 1. 何を作るか

| モジュール | Tier | 内容 |
|---|---|---|
| `masterdata` | Tier 1（Simple） | 経費科目マスタ相当の単純CRUD |
| `expense` | Tier 2（Rich） | 経費申請の状態遷移1本（下書き→申請→承認/却下） |

両モジュール間でDomain Eventを1本飛ばす（`expense`承認時に`masterdata`の利用回数カウンタを更新、等）。**これは正式な`koiki-reference-app`ではない。**業務題材が同じ（経費申請）なのは検証結果を後で活かすためで、コードは引き継がない。

---

## 2. どこで作るか

- 作業は1本のブランチ（例: `walking-skeleton`）で行い、**終わったら消す。**mainには正規のトップレベル構造だけを置く
- 正規のトップレベル構造（最終形）は次のとおり。**Walking Skeleton中にこれを全部作る必要はない。**必要になったものだけ作る

```text
koiki-javaweb-fw/
├── koiki-parent
├── koiki-dependencies-bom
├── koiki-framework
├── koiki-starters
├── koiki-testing
├── koiki-archunit-rules
├── koiki-migration-recipes
├── koiki-reference-app
├── koiki-project-template
├── docs
│   └── agent/skills/
├── ops
└── build-support
```

- `docs/`配下（Skills、ADR等）だけは最初からmainに置いてよい。それ以外（Maven Module群）はWalking Skeleton検証後、Phase 1aで正式に作る

---

## 3. 何を確認するか（統合チェックリスト）

グランドデザインとRepository Architectureで洗い出した検証項目を、テーマ別にまとめる。

### リポジトリ・ビルドの土台

- [ ] Root Reactor／Parent／BOMの責務を分離した状態でMulti-module buildが通る
- [ ] `<release>21>`＋Toolchains＋Enforcerで、想定外JDKでのビルドを実際に失敗させられる
- [ ] Java 21でビルドした成果物がJava 25ランタイムでも起動する
- [ ] `@NullMarked`とNullAwayを導入し、違反でビルドが失敗する

### ArchUnit・構造規約

- [ ] Tier1/Tier2の主要規則（全Tier共通1-13、Tier1固有14、Tier2兼用15-24、イベントリスナー38-39の計26件）が実際に書け、意図的違反を検出できる。誤検出がないか
- [x] `koiki-archunit-rules`を独立Maven artifactとしてinstallし、**外側の別プロジェクトから**test依存として使って違反を検出できる
- [x] `internal`パッケージへの外部参照が検出される
- [x] `package-info.java`への`@NullMarked`と`@KoikiModule`の併記に問題がない

### Flyway

- [ ] KOIKI側・顧客側で履歴テーブルを分けた2階層構成が独立して進む（顧客側`V5`の後にKOIKI側`V2`を追加してもバージョン順序が壊れない）
- [ ] この構成をどのStarter（または内部モジュール）に持たせるかを、実装しながら決める

### Tier2実装の実務感

- [ ] `expense`の実装量が現実的か（クラス数を実測して記録する）
- [ ] OSIV無効化状態で、Entityをビューへ渡す違反コードがArchUnitで検出される
- [ ] `expense`→`masterdata`の同期イベント連携が、Testcontainers統合テストで素直に書ける（ロールバック伝播を含む）
- [ ] Spring Modulithのモジュールテスト機能（`ApplicationModules.of`等）がtest scopeで使える

### コンテナビルドの型（本番設定はPhase 4。ここでは型だけ）

- [ ] Multi-stage build＋レイヤ分割（`-Djarmode=tools ... extract`）＋JREベースイメージ＋非rootユーザーが機能する

### AIエージェント向けSkills

- [ ] `docs/agent/skills/koiki-project-overview`・`koiki-business-feature-work`を最小版で作成する
- [ ] `AGENTS.md`等の導線ファイルを置き、実際にAIコーディングエージェントから参照できる

### OpenSpec試行

- [ ] `expense`の実装をOpenSpecのワークフロー（提案→設計→タスク分解→実装→検証→アーカイブ）で進めてみて、実務に合うか判断する
- [ ] OpenSpecが生成するAI向け指示と、`docs/agent/skills/`が衝突しないか確認する

---

## 4. 進め方（順序）

1. **リポジトリ・ビルドの土台**（§3の1つ目のグループ）とコンテナビルドの型を先に作る
2. **Flyway**の2階層構成に着手する（一番厄介で、代替案の検討に時間がかかりやすいため早めに）
3. **ArchUnit・構造規約**を実装する
4. `expense`の実装を**OpenSpecワークフローに乗せて**進めながら、Tier2実務感・OSIV・イベント連携をまとめて検証する
5. Skillsの最小版を書く（V1・ArchUnitの結果を踏まえてからの方が書きやすい）

---

## 5. 終わったらどうするか

- ブランチごと捨てる。**コードそのものは残さない**
- 次の設定・学びだけを正規リポジトリへ持っていく
  - `koiki-parent`の実際のビルド設定値
  - `koiki-archunit-rules`のルール実装
  - Flyway複数構成の実装（所属先が決まった状態のもの）
  - Dockerfileの型
  - Skillsの最小版の文面
- グランドデザイン側を直したほうがよい箇所があれば、メモとして残す（例: Flyway Starterの所属先、ArchUnitで記述不能と判明した規則のReview Checklistへの格下げ）。都度ADRを起票するかは実装しながら判断してよい

---

## 6. 作らないもの

- `koiki-reference-app`の正式実装（`expense`/`masterdata`は別物）
- 認証・認可・監査本体
- コンテナの本番設定値・正式な参照Dockerfile
- OpenSpecの正式なディレクトリ配置（試行のみ）
- Skill 5種のフル版（最小2種のみ）
- Project Template、Migration Recipes
