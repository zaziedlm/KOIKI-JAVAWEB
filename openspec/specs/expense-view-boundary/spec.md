# expense-view-boundary Specification

## Purpose

OSIVを無効化したMVCアプリケーションにおいて、正規のView経路が専用のview/read modelだけを受け取り、JPAドメインモデルの誤った露出が構造検証と実際のレンダリングの両方で検出されることを確認する。

## Requirements

### Requirement: OSIVを無効化する

Walking Skeletonアプリケーションは、Persistence ContextをMVCのViewレンダリングまで延長するOSIVを無効にして動作しなければならない（`SHALL`）。

#### Scenario: 通常構成でアプリケーションを起動する

- **WHEN** Walking Skeletonアプリケーションを通常のプロファイルで起動する
- **THEN** リクエスト単位の追加設定を必要とせずOSIVが無効になっている

### Requirement: 正規の経費詳細Viewは専用のViewデータを使用する

正規の経費詳細画面は専用の不変なview/read modelからレンダリングし、JPAドメインモデルをMVCモデルへ格納してはならない（`SHALL`）。

#### Scenario: 既存経費の詳細を表示する

- **WHEN** 利用者が既存の経費申請の詳細画面を要求する
- **THEN** 経費申請の識別子、説明、申請金額、状態、および明細を含むViewデータから応答を正常にレンダリングする
- **AND** MVCモデルはJPAドメインモデルを含まない

### Requirement: JPAドメインモデルの露出を構造検証で検出する

KOIKIのアーキテクチャ検証は、MVCハンドラまたはMVCモデルを介してJPAドメインモデルをView層へ露出する構造を違反として検出しなければならない（`SHALL`）。

#### Scenario: 意図的なEntity露出fixtureを検査する

- **WHEN** KOIKI ArchUnitルールが、JPA EntityをMVC境界へ直接渡す意図的な違反fixtureを検査する
- **THEN** View境界ルールが違反を報告する

### Requirement: Lazy状態の誤った露出をレンダリング時に検出する

OSIVが無効な状態で、トランザクション終了後に未初期化のLazy関連へアクセスするView経路は、`LazyInitializationException`を原因としてレンダリングに失敗しなければならない（`SHALL`）。

#### Scenario: 検証専用経路で未初期化の明細へアクセスする

- **WHEN** 検証専用MVC経路が、未初期化の明細を持つ経費Entityをトランザクション終了後にテンプレートへ渡す
- **THEN** テンプレートによる明細アクセス時にレンダリングが失敗する
- **AND** 例外の原因連鎖に`LazyInitializationException`が含まれる
