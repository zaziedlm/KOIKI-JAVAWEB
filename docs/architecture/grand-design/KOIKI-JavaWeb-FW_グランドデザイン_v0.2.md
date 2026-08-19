# KOIKI-JavaWeb-FW グランドデザイン v0.2

**文書版:** v0.2（構想確定・基本設計準備版）
**改訂日:** 2026年7月27日（v0.2初期改訂）／2026年8月17日（Phase 0成果物反映）
**文書状態:** ACCEPTED（Phase 0 Architecture Baseline）
**承認日:** 2026年8月19日
**Architecture Owner:** Shuichi Kataoka
**対象プロジェクト:** KOIKI-JavaWeb-FW
**参照元:** KOIKI-PYFW dev/v0.8
**前版:** v0.1（2026年7月22日）

---

## 目次

- [0. 文書の目的](#0-文書の目的)
  - [v0.1 からの主な変更](#v01-からの主な変更)
- [1. エグゼクティブサマリー](#1-エグゼクティブサマリー)
- [2. 背景と問題認識](#2-背景と問題認識)
- [3. KOIKI-PYFW から継承する原則と刷新する範囲](#3-koiki-pyfw-から継承する原則と刷新する範囲)
  - [3.1 フレームワークとアプリケーションの所有権分離](#31-フレームワークとアプリケーションの所有権分離)
  - [3.2 一方向の依存関係](#32-一方向の依存関係)
  - [3.3 横断機能を個別業務へ埋め込まない](#33-横断機能を個別業務へ埋め込まない)
  - [3.4 UI に依存しないバックエンド認可](#34-ui-に依存しないバックエンド認可)
  - [3.5 運用をアーキテクチャに含める](#35-運用をアーキテクチャに含める)
  - [3.6 実行可能な Reference Application](#36-実行可能な-reference-application)
  - [3.7 刷新する範囲](#37-刷新する範囲)
- [4. ビジョン、対象、非対象](#4-ビジョン対象非対象)
  - [4.1 ビジョン](#41-ビジョン)
  - [4.2 主対象](#42-主対象)
  - [4.3 初期の主対象外](#43-初期の主対象外)
- [5. 設計原則](#5-設計原則)
- [6. 技術標準](#6-技術標準)
  - [6.1 基本スタック](#61-基本スタック)
  - [6.2 Java バージョン方針](#62-java-バージョン方針)
  - [6.3 JDK ディストリビューション](#63-jdk-ディストリビューション)
  - [6.4 Spring Modulith の位置づけと採用レベル](#64-spring-modulith-の位置づけと採用レベル)
- [7. 提供形態とリポジトリ戦略](#7-提供形態とリポジトリ戦略)
  - [7.1 提供物](#71-提供物)
  - [7.2 ビルド設定の規約](#72-ビルド設定の規約)
  - [7.3 顧客案件との関係](#73-顧客案件との関係)
- [8. リリース管理とサポート方針](#8-リリース管理とサポート方針)
  - [8.1 バージョン体系と Spring Boot 対応](#81-バージョン体系と-spring-boot-対応)
  - [8.2 リリースサイクル](#82-リリースサイクル)
  - [8.3 サポートウィンドウ](#83-サポートウィンドウ)
  - [8.4 顧客への更新義務](#84-顧客への更新義務)
  - [8.5 Semantic Versioning と Deprecation](#85-semantic-versioning-と-deprecation)
  - [8.6 移行支援](#86-移行支援)
  - [8.7 第三者ライブラリの採用と追従](#87-第三者ライブラリの採用と追従)
  - [8.8 セキュリティ修正と脆弱性管理](#88-セキュリティ修正と脆弱性管理)
- [9. ガバナンスと所有権](#9-ガバナンスと所有権)
  - [9.1 昇格の不可逆性](#91-昇格の不可逆性)
  - [9.2 Framework への昇格チェックリスト](#92-framework-への昇格チェックリスト)
  - [9.3 Framework へ入れないもの](#93-framework-へ入れないもの)
  - [9.4 アーキテクチャオーナー](#94-アーキテクチャオーナー)
  - [9.5 四半期アーキテクチャレビュー](#95-四半期アーキテクチャレビュー)
  - [9.6 Public API 境界](#96-public-api-境界)
- [10. アーキテクチャ全体構造](#10-アーキテクチャ全体構造)
  - [10.1 モジュラーモノリス](#101-モジュラーモノリス)
  - [10.2 モジュールの公開範囲](#102-モジュールの公開範囲)
  - [10.3 依存の方向](#103-依存の方向)
  - [10.4 shared-kernel](#104-shared-kernel)
- [11. 業務モジュールの内部構造](#11-業務モジュールの内部構造)
  - [11.1 構造 Tier の考え方](#111-構造-tier-の考え方)
  - [11.2 Tier 1（Simple）の構造](#112-tier-1simpleの構造)
  - [11.3 Tier 2（Rich）の構造](#113-tier-2richの構造)
  - [11.4 責務](#114-責務)
  - [11.5 Tier の昇格](#115-tier-の昇格)
  - [11.6 Tier 2 のモデル方針 — 兼用を既定とする](#116-tier-2-のモデル方針--兼用を既定とする)
  - [11.7 モデル分離のオプトイン](#117-モデル分離のオプトイン)
  - [11.8 Domain Event の定義規約](#118-domain-event-の定義規約)
  - [11.9 実用的 DDD](#119-実用的-ddd)
- [12. Web API 設計](#12-web-api-設計)
  - [12.1 標準契約](#121-標準契約)
  - [12.2 API バージョニング](#122-api-バージョニング)
  - [12.3 Controller の責務](#123-controller-の責務)
  - [12.4 エラー処理と統一エラー形式](#124-エラー処理と統一エラー形式)
  - [12.5 競合制御](#125-競合制御)
- [13. UI プロファイル](#13-ui-プロファイル)
  - [13.1 位置づけと API との共存](#131-位置づけと-api-との共存)
  - [13.2 Thymeleaf プロファイル](#132-thymeleaf-プロファイル)
  - [13.3 ビューモデル変換の必須化](#133-ビューモデル変換の必須化)
  - [13.4 HTMX](#134-htmx)
  - [13.5 SPA プロファイル](#135-spa-プロファイル)
  - [13.6 プロファイル併用](#136-プロファイル併用)
- [14. セキュリティアーキテクチャ](#14-セキュリティアーキテクチャ)
  - [14.1 基本原則](#141-基本原則)
  - [14.2 認証方式](#142-認証方式)
  - [14.3 セッションストア](#143-セッションストア)
  - [14.4 認可](#144-認可)
  - [14.5 ローカル Identity](#145-ローカル-identity)
  - [14.6 アカウント保護と認証試行制御](#146-アカウント保護と認証試行制御)
  - [14.7 ブラウザ保護](#147-ブラウザ保護)
  - [14.8 セキュリティ試験](#148-セキュリティ試験)
- [15. 監査アーキテクチャ](#15-監査アーキテクチャ)
  - [15.1 通常ログとの分離](#151-通常ログとの分離)
  - [15.2 監査の3分類とトランザクション方針](#152-監査の3分類とトランザクション方針)
  - [15.3 監査対象と分類の割当](#153-監査対象と分類の割当)
  - [15.4 標準項目](#154-標準項目)
- [16. データアーキテクチャ](#16-データアーキテクチャ)
  - [16.1 対応方針](#161-対応方針)
  - [16.2 永続化技術の選択](#162-永続化技術の選択)
  - [16.3 read model](#163-read-model)
  - [16.4 キャッシュ](#164-キャッシュ)
  - [16.5 共通化する機能](#165-共通化する機能)
  - [16.6 避ける抽象化](#166-避ける抽象化)
  - [16.7 所有権と Migration 管理](#167-所有権と-migration-管理)
  - [16.8 Oracle 適合](#168-oracle-適合)
- [17. トランザクションとモジュール間連携](#17-トランザクションとモジュール間連携)
  - [17.1 境界](#171-境界)
  - [17.2 原則](#172-原則)
  - [17.3 モジュール間連携](#173-モジュール間連携)
  - [17.4 永続化技術の混在に関する規約](#174-永続化技術の混在に関する規約)
  - [17.5 Spring Modulith Level 1 期間の暫定規約](#175-spring-modulith-level-1-期間の暫定規約)
- [18. 外部連携](#18-外部連携)
  - [18.1 対象](#181-対象)
  - [18.2 クライアント技術の標準](#182-クライアント技術の標準)
  - [18.3 共通ポリシー](#183-共通ポリシー)
  - [18.4 配置](#184-配置)
- [19. バッチ、ファイル、スケジュール](#19-バッチファイルスケジュール)
  - [19.1 バッチ](#191-バッチ)
  - [19.2 スケジュールと単一実行基盤](#192-スケジュールと単一実行基盤)
  - [19.3 ファイル](#193-ファイル)
- [20. Observability と運用](#20-observability-と運用)
  - [20.1 構造化ログとコンテキスト伝播](#201-構造化ログとコンテキスト伝播)
  - [20.2 メトリクス](#202-メトリクス)
  - [20.3 トレース](#203-トレース)
  - [20.4 ヘルスチェック](#204-ヘルスチェック)
  - [20.5 Graceful Shutdown](#205-graceful-shutdown)
- [21. テスト戦略](#21-テスト戦略)
  - [21.1 Test Pyramid と契約試験](#211-test-pyramid-と契約試験)
  - [21.2 Testcontainers](#212-testcontainers)
  - [21.3 ArchUnit / Spring Modulith](#213-archunit--spring-modulith)
  - [21.4 Quality Gate](#214-quality-gate)
  - [21.5 CI テストマトリクス](#215-ci-テストマトリクス)
  - [21.6 性能ベースライン計測](#216-性能ベースライン計測)
- [22. Auto Configuration と拡張点](#22-auto-configuration-と拡張点)
  - [22.1 自動構成してよいもの](#221-自動構成してよいもの)
  - [22.2 明示設定すべきもの](#222-明示設定すべきもの)
  - [22.3 拡張契約](#223-拡張契約)
- [23. 実行・デプロイアーキテクチャ](#23-実行デプロイアーキテクチャ)
  - [23.1 第一参照環境](#231-第一参照環境)
  - [23.2 コンテナ標準](#232-コンテナ標準)
  - [23.3 Virtual Threads](#233-virtual-threads)
  - [23.4 Kubernetes-ready](#234-kubernetes-ready)
  - [23.5 WAR](#235-war)
- [24. 開発者体験と AI 駆動開発](#24-開発者体験と-ai-駆動開発)
  - [24.1 開発支援](#241-開発支援)
  - [24.2 Agent Guidance](#242-agent-guidance)
- [25. 非機能品質目標](#25-非機能品質目標)
  - [25.1 セキュリティ](#251-セキュリティ)
  - [25.2 可用性](#252-可用性)
  - [25.3 性能](#253-性能)
  - [25.4 保守性](#254-保守性)
  - [25.5 監査性](#255-監査性)
- [26. Reference Application](#26-reference-application)
  - [26.1 位置づけと役割](#261-位置づけと役割)
  - [26.2 業務題材](#262-業務題材)
  - [26.3 モジュール構成と Phase 別追加計画](#263-モジュール構成と-phase-別追加計画)
  - [26.4 設計判断と実証箇所の対応](#264-設計判断と実証箇所の対応)
  - [26.5 Reference Application で実証しない決定](#265-reference-application-で実証しない決定)
- [27. 段階的ロードマップと完了条件](#27-段階的ロードマップと完了条件)
  - [27.1 完了条件（DoD）の考え方](#271-完了条件dodの考え方)
  - [27.2 全 Phase 共通の完了条件](#272-全-phase-共通の完了条件)
  - [27.3 Phase 0: Architecture Baseline](#273-phase-0-architecture-baseline)
  - [27.4 Phase 1a: Build Foundation（内部マイルストーン）](#274-phase-1a-build-foundation内部マイルストーン)
  - [27.5 Phase 1b: Runtime Foundation（v0.1）](#275-phase-1b-runtime-foundationv01)
  - [27.6 Phase 2: Security Foundation（v0.2）](#276-phase-2-security-foundationv02)
  - [27.7 Phase 3: Reference Vertical Slice（v0.3）](#277-phase-3-reference-vertical-slicev03)
  - [27.8 Phase 4: Enterprise Integration（v0.4）](#278-phase-4-enterprise-integrationv04)
  - [27.9 Phase 5: Production Baseline（v1.0）](#279-phase-5-production-baselinev10)
  - [27.10 DoD の運用](#2710-dod-の運用)
- [28. 主要リスクと対策](#28-主要リスクと対策)
  - [28.1 アーキテクチャ](#281-アーキテクチャ)
  - [28.2 データアクセス](#282-データアクセス)
  - [28.3 UI](#283-ui)
  - [28.4 イベントと非同期](#284-イベントと非同期)
  - [28.5 Java と実行環境](#285-java-と実行環境)
  - [28.6 リリースとサポート](#286-リリースとサポート)
  - [28.7 ガバナンスと規約強制](#287-ガバナンスと規約強制)
  - [28.8 計画と実行](#288-計画と実行)
- [29. 今後作成する詳細設計文書](#29-今後作成する詳細設計文書)
- [30. ADR 一覧](#30-adr-一覧)
  - [基盤](#基盤)
  - [アーキテクチャ](#アーキテクチャ)
  - [UI](#ui)
  - [セキュリティと監査](#セキュリティと監査)
  - [データ](#データ)
  - [Web API と外部連携](#web-api-と外部連携)
  - [ガバナンスとリリース](#ガバナンスとリリース)
  - [Reference Application](#reference-application)
- [付録A. KOIKI-JavaWeb-FW の定義](#付録a-koiki-javaweb-fw-の定義)
- [付録B. 参照資料](#付録b-参照資料)
  - [KOIKI-PYFW dev/v0.8](#koiki-pyfw-devv08)
  - [公式技術資料](#公式技術資料)

---

## 0. 文書の目的

本書は、KOIKI-PYFW で培った設計思想、責務分離、セキュリティ、運用性、開発支援の考え方を継承し、企業向け Java Web システムの共通基盤となる「KOIKI-JavaWeb-FW」の全体像を定義するグランドデザイン文書である。

本書では、個別クラスや API の詳細設計に先立ち、次の事項を正本として定める。

- プロジェクトの目的、対象、非対象
- KOIKI として継承する設計原則と、刷新する範囲
- 技術スタックと採用理由
- フレームワーク、リファレンスアプリケーション、顧客アプリケーションの境界
- **リリース管理、サポートウィンドウ、顧客への更新義務**
- **ガバナンス体制と、Framework への昇格判定**
- モジュラーモノリスおよび業務モジュールの内部構造
- セキュリティ、監査、データアクセス、運用、テストの基本方針
- フレームワークが提供する機能群と拡張点
- **Reference Application が実証する設計判断**
- 初期リリースから本番品質へ至る開発ロードマップと、**Phase 別の完了条件**
- 今後作成する基本設計書、ADR、実装規約の起点

本書は「Spring を覆い隠す独自フレームワーク」を定義するものではない。Spring Boot および Spring Framework の標準機能を、企業システムで安全かつ一貫して利用するための統合基盤、規約、Starter、参照実装、検証手段を定義する。

### v0.1 からの主な変更

| 領域 | 変更内容 |
|---|---|
| Java ベースライン | Java 25 → **Java 21**（ターゲットバイトコード） |
| Spring Boot | バージョン固定 → **追従方針型** |
| 業務モジュールの内部構造 | 一律の DDD 構造 → **構造 Tier 制**（Tier 1／Tier 2） |
| UI | Thymeleaf を第一標準 → **Thymeleaf＋HTMX と SPA を対等プロファイル** |
| Spring Modulith | 補助利用 → **採用レベルの段階導入**（Level 0〜3） |
| 新設 | **リリース管理とサポート方針**（§8）、**ガバナンスと所有権**（§9）、**Reference Application**（§26） |
| 統合 | ロードマップと**Phase 別完了条件**（§27） |

---

## 1. エグゼクティブサマリー

KOIKI-JavaWeb-FW は、Spring Boot を実行基盤とし、Spring MVC、Spring Security、Jakarta Validation、Spring Data、Actuator 等の標準技術を組み合わせた、**API 指向のエンタープライズ Java バックエンド基盤**とする。

中核アーキテクチャは、業務機能を単位としたモジュラーモノリスとする。**業務モジュールの内部構造は一律ではなく、構造 Tier により段階化する。**単純なマスタ保守は Tier 1（Application Use Case が業務ルールを持つ軽量構造）、不変条件や状態遷移を持つ領域は Tier 2（Domain 層を持つ構造）とし、昇格の判定条件を明示する。

UI は API 基盤の上に載る**公式プロファイル**として提供する。Spring MVC ＋ Thymeleaf ＋ HTMX による**サーバーサイド Web プロファイル**と、React 等の SPA に対する **SPA プロファイル**を対等に位置づけ、いずれも必須依存としない。両者は Application Use Case、Domain、認可、Repository を共有し、Inbound Adapter のみが異なる。

提供形態は、Maven BOM、Spring Boot Starter、共通ライブラリ、実行可能な Reference Application、Project Template、Testing Support、**ArchUnit ルールセット**、**移行支援レシピ**、Agent Guidance から構成する。顧客案件は KOIKI 本体をコピーして改造するのではなく、別リポジトリからバージョン付き依存として利用する。

**KOIKIは年1回のメジャーリリースを行い、最新と直前の2ラインを管理対象とする。**各ラインのOSSサポートは、対応するSpring Bootの実際のOSSサポート終了日までとし、日付をリリース時に明示する。12か月の移行期間が必要な場合は商用延長サポートを選択肢とし、顧客への更新義務と移行支援を契約可能な形で定める。

ブラウザ向け認証は HTTP Session を第一標準とし、セッションは Spring Session JDBC により既存 PostgreSQL へ格納する。外部 API およびサービス間連携には OAuth 2.0 Bearer JWT を用いる。OIDC を企業 SSO の優先方式とし、SAML、Cognito、ALB 認証、HENNGE 等を Adapter として追加できる構造を持つ。

標準データベースは PostgreSQL／Aurora PostgreSQL とし、Oracle を設計適合対象とする。データアクセスは **JPA、MyBatis、JdbcClient の3系統**を公式化し、**モジュール単位で選択**する。既存 SQL 資産の移行を正式なユースケースとして扱う。

言語は **Java 21** をターゲットバイトコードとし、Java 25 を推奨実行環境かつ互換確認対象とする。第一の参照実行環境は AWS ECS Fargate とするが、アプリケーション構造は Kubernetes-ready とし、EKS 向け運用資材は後続段階で提供する。デプロイ方式は Executable JAR およびコンテナを標準とする。

---

## 2. 背景と問題認識

企業向け Web システムでは、Spring Boot を採用するだけで自動的にエンタープライズ品質が得られるわけではない。案件ごとに個別判断へ委ねると、次のようなばらつきが生じる。

- Controller へ業務ロジックや SQL が混在する
- 認証方式ごとにユーザー、権限、監査の実装が分裂する
- 例外形式、エラーコード、ログ項目がシステムごとに異なる
- CSRF、Cookie、CORS、CSP 等の設定品質が担当者に依存する
- DB マイグレーションや初期データ投入の責任が不明確になる
- Web コンテナ内の定期処理が多重実行される
- テストが Unit Test だけに偏り、セキュリティの失敗系が検証されない
- 顧客固有要件が共通基盤へ混入し、フレームワークが肥大化する
- テンプレートを複製した案件が独自進化し、セキュリティ修正を横展開できない
- **フレームワークの更新が止まり、案件がセキュリティ修正を受けられなくなる**
- AI コーディングエージェントが変更対象や責務境界を判断できない

KOIKI-JavaWeb-FW は、これらを個別の便利クラスではなく、**アーキテクチャ、所有権、ガバナンス、既定値、テスト、運用、サポート方針、ドキュメントを含む一体的な仕組み**として解決する。

---

## 3. KOIKI-PYFW から継承する原則と刷新する範囲

### 3.1 フレームワークとアプリケーションの所有権分離

KOIKI-PYFW dev/v0.8 では、再利用可能な framework layer、reference application layer、downstream/customer-specific 領域を明確に分離している。Java 版でも、この所有権分離を最重要原則として継承する。

| 区分 | Java 版の位置づけ | 主な責務 | KOIKI-PYFW からの変更 |
|---|---|---|---|
| KOIKI Framework | 再利用可能な Maven モジュール／Starter | 認証基盤、例外、監査、ログ、トランザクション、Web 共通契約 | 変更なし（`components/libkoiki/` に対応） |
| KOIKI Reference Application | 実行可能な標準利用例 | 認証、CRUD、MVC、API、外部 IdP、監査等の組み合わせ例 | 変更なし（`components/koiki_ref_app/` に対応） |
| Customer Application | 顧客案件の**別リポジトリ** | 業務ルール、画面、外部連携、顧客固有テーブル | **変更あり。**KOIKI-PYFW は同一リポジトリ内 `apps/` に予約領域を持つが、Java 版は物理的に別リポジトリとする |

再利用性が曖昧な機能は、最初から Framework へ入れない。Reference Application または Customer Application で実装実績を作り、**§9.2 の昇格チェックリストを満たした段階で** Framework へ昇格させる。

### 3.2 一方向の依存関係

基本的な処理の流れは次のとおりとする。

```text
Inbound Adapter（REST / MVC / Event / Batch / Message）
  -> Application Use Case
    -> Domain
    -> Outbound Port
      -> Outbound Adapter（DB / External API / File / Messaging）
```

下位の業務ルールが Web、DB、Spring MVC 等の上位技術へ逆依存しないことを原則とする。

### 3.3 横断機能を個別業務へ埋め込まない

次の機能は個別の業務機能内へ重複実装せず、共通基盤が所有する。

- 認証、認可
- セキュリティヘッダー、CSRF、CORS
- 監査イベント
- 設定管理、秘密情報
- 例外、エラーコード
- トランザクション
- 構造化ログ、相関ID
- メトリクス、トレース、ヘルスチェック
- **認証試行制御**
- テスト支援

> **汎用的なレート制御はインフラ層（WAF、ALB 等）の責務とし、KOIKI はアプリケーション内に実装しない。**認証試行制御（ブルートフォース対策）のみ、DB への試行回数記録により §14 で提供する（§16 参照）。

### 3.4 UI に依存しないバックエンド認可

画面の非表示、ルートガード、JavaScript 制御は利便性向上の手段であり、認可境界ではない。URL、Method Security、リソース所有権、業務状態の検証をバックエンドで強制する。

### 3.5 運用をアーキテクチャに含める

Web リクエスト処理だけでなく、マイグレーション、初期データ投入、定期クリーンアップ、バッチ、監査保持、秘密情報、障害解析を設計対象とする。

### 3.6 実行可能な Reference Application

Reference Application はデモではなく、設計契約の正しい利用例、統合テスト対象、リリース時のスモークテスト、AI エージェントの参照実装として維持する。

**ただし Reference Application がすべての設計判断を実証するわけではない。**実証する範囲と実証しない範囲を §26 に明示し、実証されない決定は規約文書と Agent Skills が担う。

### 3.7 刷新する範囲

**継承するのは原則であり、構造ではない。**

KOIKI-PYFW dev/v0.8 は `API → Service → Repository → Model` の技術レイヤー分割を採用しており、業務モジュール境界と Domain 層は存在しない。Java 版がこれらを導入する理由は次の3点である。

1. **Inbound の多様性** — Java 版の対象スコープ（§4.2）は REST、Spring MVC、Event、Batch、Message の系統を含む。技術レイヤー分割では、1つの業務機能の変更が全レイヤーへ分散する。
2. **境界の機械的強制が可能** — ArchUnit および Spring Modulith により、モジュール境界を規約ではなく検査として実効化できる。KOIKI-PYFW が規約とレビューに依存していたのは実行環境上の制約であり、境界が不要と判断した結果ではない。
3. **永続化技術のモジュール単位選択** — JPA、MyBatis、JdbcClient の使い分け（§16.2）および既存 SQL 資産の移行は、業務機能単位の境界を前提とする。

刷新に伴う学習コストは実在する。本書はこれを2段構えで抑制する。

- **Tier 1** — KOIKI-PYFW の `API → Service → Repository → Model` とほぼ同型の構造とし、移行コストを実質ゼロに保つ
- **Tier 2** — 新たに学習すべき概念を「**業務ルールを Service からモデル自身へ移す**」の一点に絞る

KOIKI-PYFW と Java 版の概念対応、および**単純に対応させてはならない概念**は、用語集（§29）に対応表として定める。

---

## 4. ビジョン、対象、非対象

### 4.1 ビジョン

> Spring 標準機能を尊重しながら、企業向けシステムに必要な堅牢性、セキュリティ、監査性、運用性、保守性を初期状態から備え、開発者が業務機能へ集中できる Java Web 基盤を提供する。

### 4.2 主対象

- 社内業務システム
- 管理システム
- B2B ポータル
- 認証・認可を伴う REST API
- Spring MVC ＋ Thymeleaf ＋ HTMX によるサーバーサイド Web
- React 等の SPA に対するバックエンド API
- バッチ、ファイル、外部 API 連携を含む業務システム
- 数十人から数千人規模の利用者を想定するシステム
- AWS 上のコンテナ実行を中心とするシステム
- 既存 Oracle／SQL 資産から Java へ移行するシステム

### 4.3 初期の主対象外

- 超大規模コンシューマー SNS
- 超低遅延取引システム
- イベントソーシングを必須とするシステム
- WebFlux／Reactive を前提とするシステム
- **マルチテナンシー（単一テナントを前提とする）**
- マイクロサービス群全体を統制するサービスメッシュ基盤
- Kafka、Redis、Kubernetes 等を必須とする基盤
- 汎用ワークフローエンジン
- 独自 ORM、独自 DI、独自認証プロトコル

これらを将来接続できる余地は残すが、標準構成へ強制しない。

> **WebFlux／Reactive を対象外とする根拠** — Java 21 以降の仮想スレッドにより、ブロッキング I/O を前提とした構造でも高い並行性を確保できる。リアクティブプログラミングの導入コスト（学習、デバッグ、ライブラリ制約）に見合う利益が、本書の対象規模では得られない。
>
> **マルチテナンシーを対象外とする根拠** — 現時点で要件として確定しておらず、Repository、Flyway、認可、セッション、キャッシュへ広く波及する。必要が確定してから設計する。**将来対応のための先回りの実装は行わない**（識別子カラムの先行追加、テナント対応 Repository、テナント境界を持つ認可のいずれも実装しない）。

---

## 5. 設計原則

1. **Spring 標準優先** — Spring が提供する契約を独自抽象化で覆い隠さない。
2. **Secure by Default** — 安全な Cookie、CSRF、認可、監査、秘密値保護を既定とする。
3. **Fail Closed / Fail Fast** — 不正な設定や許可外の入力は安全側へ拒否し、起動時に検出する。
4. **Explicit Ownership** — Framework、Reference、Customer の所有者を明示する。
5. **Modular Monolith First** — 分散を前提にせず、モジュール境界を保った単一配置から始める。
6. **Package by Feature** — 技術レイヤーだけで全体を横断分割せず、業務機能を第一単位とする。
7. **Use Case Transaction Boundary** — トランザクション境界を Application Use Case に置く。
8. **No Hidden Magic** — Auto Configuration は予測可能で、無効化・上書き可能にする。
9. **Production Parity** — 開発・CI でも本番に近い DB やコンテナ動作を検証する。
10. **Observable by Default** — ログ、メトリクス、トレース、監査を後付けにしない。
11. **Test the Failure Paths** — 認証、認可、競合、タイムアウト、リトライ、失効を検証する。
12. **Documentation as Architecture** — ADR、Skills、規約、サンプルをコードと同時に保守する。

---

## 6. 技術標準

### 6.1 基本スタック

| 領域 | 標準 | 方針 |
|---|---|---|
| Java | **Java 21 LTS** | **正式標準。ターゲットバイトコードは 21。**Java 25 は推奨実行環境かつ互換確認対象。年次見直しの対象 |
| Framework | **Spring Boot 4.x** | **特定マイナーへ恒久固定しない。**各 KOIKI メジャーがリリース時点で OSS サポート中の最新マイナーへ固定する（§8.1） |
| Web | Spring MVC | Servlet ベースを標準 |
| Security | Spring Security | 認証・認可プロトコルを独自実装しない |
| **UI（サーバーサイド）** | **Thymeleaf ＋ HTMX** | 公式 UI プロファイル。必須依存にはしない |
| **UI（SPA）** | **契約提供のみ**（React 参照実装） | 公式 UI プロファイル。フレームワーク本体へ同梱しない |
| Build | Maven | BOM、Parent POM、Starter 配布を標準化 |
| Architecture | Modular Monolith | **Spring Modulith を採用レベル Level 0〜3 で段階導入する**（§6.4） |
| Validation | Jakarta Validation | DTO 検証と業務ルールを分離 |
| **Null Safety** | **JSpecify ＋ NullAway** | Framework は必須検査。JSR 305 アノテーションは使用しない |
| **JSON** | **Jackson 3** | `JsonMapperBuilderCustomizer` による設定。モジュールの自動検出は無効化する |
| Migration | Flyway | Runtime 自動 DDL を本番で使用しない。**所有者別に独立管理する**（§16.7） |
| Testing | **JUnit 5 / Testcontainers / ArchUnit** | 実 DB、構造規約、統合試験を標準化。JUnit 4 は使用しない |
| Operations | Actuator / Micrometer / OpenTelemetry | ヘルス、メトリクス、トレースを統合 |
| Deployment | Executable JAR / Container | WAR は原則標準対象外 |

**本表に記載するバージョンは、本書作成時点のものである。**KOIKI が実際に使用するバージョンは、**KOIKI ⇔ Spring Boot ⇔ Java ベースライン対応表**（§8.1）を正とする。本書はバージョン番号ではなく、追従方針を規定する。

### 6.2 Java バージョン方針

KOIKI のターゲットバイトコードを **Java 21** とする。KOIKI 成果物は Java 21 ランタイム上で動作し、Java 25 ランタイム上でも動作することを CI で継続的に検証する。

Spring Boot 4.x のベースラインは Java 17 であり、より新しい Java を要求する技術的必然性はない。**実行環境の下限を引き上げることは、適用可能な案件の範囲を狭める。**§4.2 の対象案件には既存環境の制約を持つものが含まれるため、下限を Java 21 に置く。

Java 25は推奨実行環境とする。特に仮想スレッドを利用する場合、Java 24以降で`synchronized`に起因するpinningのほぼすべてが解消されているため、Java 25上での実行を推奨する。native code等に起因する残存ケースは別途検証する。

バージョンは「常に最新へ自動追従」せず、KOIKI のリリース単位で固定する。ベースラインの妥当性は §8 に基づき KOIKI メジャー更新ごとに評価する。**Java 29 LTS（2027年9月予定）は次期以降の引き上げ候補である。**

#### 本方針に伴う技術的制約

- **Scoped Values（Java 25 で正式化）は利用できない。**コンテキスト伝播には **Micrometer Context Propagation** を標準機構として用いる（§20.1）
- **仮想スレッドは既定で無効とする。**Java 21 では `synchronized` ブロック内のブロッキング操作でキャリアスレッドが pin されるため、有効化は opt-in とし、Java 25 以上を推奨条件とする（§23）

### 6.3 JDK ディストリビューション

KOIKI は Oracle JDK へ固定しない。Eclipse Temurin、Amazon Corretto 等を含む OpenJDK ディストリビューションを利用可能とし、案件ではサポート契約、クラウド環境、ライセンス方針に基づいて選定する。

### 6.4 Spring Modulith の位置づけと採用レベル

Spring Modulith は、業務モジュール構造の検証、モジュール単位の統合テスト、観測、ドキュメント生成に加え、**コミット後の耐久的なイベント配信基盤（Event Publication Registry）**として利用する。

KOIKI は Spring Modulith の全機能へ無条件に依存するのではなく、**採用機能を明示的に列挙し、その範囲においては中核依存とする。**採用機能の拡大はリリース段階に対応させる。

| Level | 追加する依存 | 得られる機能 | 適用 Phase |
|---|---|---|---|
| **Level 0** | `spring-modulith-starter-test`（test scope） | モジュール境界検証、PlantUML／Application Module Canvas 生成。**実行時依存なし** | Phase 1a・1b・2 |
| **Level 1** | 追加なし（Spring 標準） | `ApplicationEventPublisher` ＋ 同期 `@EventListener` によるモジュール間連携 | Phase 3 |
| **Level 2** | `spring-modulith-starter-jpa` または `-jdbc` | Event Publication Registry、`@ApplicationModuleListener`、トランザクショナル Outbox 相当の耐久配信 | Phase 4 |
| **Level 3** | `spring-modulith-events-*`（broker 別） | 外部ブローカーへの externalization | v1.0 以降・要件が生じた場合 |

**Level を上げる判断基準**

- Level 0 → 1: モジュール間の連携が実際に発生した時点
- Level 1 → 2: **コミット後に実行すべき副作用（外部 API 呼出、メール送信、ファイル出力）が発生した時点**
- Level 2 → 3: 外部システムとのメッセージング連携が要件として確定した時点（§4.3 により初期は対象外）

**バージョン制約** — Spring Modulith 2.x は Spring Boot 4 をベースラインとして要求する。1.x 系は Spring Boot 3.5 対応であり、§6.1 と併用できない。**バージョンによる段階導入は不可能であり、機能単位で段階化する。**

**採用しない機能** — Spring Modulith の Observability モジュールは §20 の Micrometer／OpenTelemetry 標準と重複しうるため、Phase 4 で必要性を評価する。

---

## 7. 提供形態とリポジトリ戦略

### 7.1 提供物

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
├── ops
└── build-support
```

主な成果物は次のとおりである。

| 成果物 | 内容 |
|---|---|
| `koiki-dependencies-bom` | 依存バージョン統制。**MyBatis の `mybatis-spring-boot-starter` を含む** |
| `koiki-parent` | Maven プラグイン、Java、品質ゲートの統一 |
| `koiki-starter-api` | API、例外、Validation、Web 共通設定 |
| `koiki-starter-security` | Security 共通設定 |
| `koiki-starter-web-mvc` | Spring MVC、Thymeleaf、**HTMX 統合** |
| `koiki-starter-data-jpa` | JPA プロファイル |
| `koiki-starter-data-jdbc` | JdbcClient プロファイル |
| `koiki-starter-observability` | Actuator、Metric、Trace、Log |
| `koiki-starter-batch` | Spring Batch 統合 |
| `koiki-testing` | テスト Fixture、Security Test、Testcontainers 支援 |
| **`koiki-archunit-rules`** | **ArchUnit ルールセット。顧客プロジェクトから依存として利用する** |
| **`koiki-migration-recipes`** | **KOIKI メジャー更新時の OpenRewrite レシピ**（§8.6） |
| `koiki-reference-app` | 正規利用例（§26） |
| `koiki-project-template` | 顧客アプリ開始用テンプレート。**Thymeleaf＋HTMX 版と API＋SPA 版の2種類** |

**SPA プロファイル向けの専用 Starter は設けない。**`koiki-starter-api` と `koiki-starter-security` で必要な機能はすべて満たされるため、SPA プロファイルの実体は契約文書と参照実装である。

**MyBatis 向けの専用 Starter も設けない。**BOM によるバージョン管理と規約・ArchUnit ルールを提供し、顧客プロジェクトが `mybatis-spring-boot-starter` を直接依存に追加する（§16.2）。

### 7.2 ビルド設定の規約

#### コンパイラ設定

- `maven-compiler-plugin` に `<release>21</release>` を指定する
- **`-source` / `-target` は使用しない。**これらはブートクラスパスを検証しないため、Java 22 以降の API を誤って参照してもコンパイルが通り、実行時に `NoSuchMethodError` 等を招く
- `<release>` を用いることで、javac が Java 21 の API シグネチャに照らして検証する

#### ビルド JDK の固定

- Maven Toolchains または CI のセットアップにより、ビルド JDK を Java 21 に固定する
- Maven Enforcer Plugin により、想定外の JDK でのビルドを失敗させる
- 将来ビルド JDK を引き上げる場合も、`<release>21</release>` を維持する限り成果物の互換性は保たれる

#### 顧客プロジェクトへの適用

`koiki-parent` に上記設定を含め、顧客アプリケーションが継承できるようにする。顧客が Java 25 で開発する場合も、`<release>21</release>` を維持すれば KOIKI 成果物との整合が保たれる。ただし顧客アプリケーション自身が Java 25 の機能を使う判断は顧客側の裁量とし、**その場合、当該アプリケーションは Java 25 ランタイムが必須となる。**

### 7.3 顧客案件との関係

顧客案件は KOIKI 本体と別リポジトリで開発し、社内 Maven Repository 等からバージョン付き依存として利用する。

```text
Customer Application
  -> KOIKI Starter / Framework
    -> Spring Boot / Spring Framework
```

案件ごとに KOIKI 本体をコピーして独自改修する運用は採用しない。Framework の拡張が必要な場合は、公開された拡張点、案件側 Adapter、または KOIKI 本体への正式な変更提案（§9.2）を用いる。

**顧客プロジェクトは `koiki-archunit-rules` をテスト依存として組み込む。**これを行わない場合、本書が定める規約の大半は顧客リポジトリにおいて機械的に強制されない。

---

## 8. リリース管理とサポート方針

> **本章は顧客への約束事項を定める。**Spring Bootに一律のOSS LTSラインはなく、商用延長サポートは別契約であるため、KOIKIが自らサポートウィンドウを明示する必要がある。

### 8.1 バージョン体系と Spring Boot 対応

- **KOIKI メジャーバージョンは、Spring Boot マイナー1本に1対1で対応する。**
- **KOIKI メジャー内で Spring Boot のマイナーを跨がない。**Spring Boot マイナーの更新は、必ず KOIKI のメジャー更新として扱う。KOIKI のマイナー・パッチ更新では、同一 Spring Boot マイナーのパッチ追従のみ行う。
- **KOIKI ⇔ Spring Boot ⇔ Java ベースライン対応表**を公開し、リリースごとに更新する。この表を実際に使用するバージョンの正本とする。
- BOM と Starter の組み合わせをリリース単位で整合させる。

**開発期間中の運用** — 各 Phase の完了条件に「OSS サポート中の最新 Spring Boot マイナーへの追従完了」を含める。Phase 途中でのバージョン変更は行わない。

### 8.2 リリースサイクル

- **KOIKI メジャーを年1回、毎年12月にリリースする。**
- リリース時期は Spring Boot の11月マイナーへ追従する形とする。
- **最新ラインと直前ラインの2ラインを管理対象とする。**ただし、OSSでのセキュリティパッチと重大バグ修正の提供は、対応するSpring BootのOSSサポート期間内に限る。

**この時期設定を選ぶ理由** — Spring Bootの11月マイナーに追従し、年次更新のリズムを一定にするためである。Spring Boot minorのOSSサポートは最低13か月であり、年次リリース間で重複する期間は限定的である。そのため、OSSのみで12か月の旧ライン移行期間を保証せず、リリース時に確定した実日付を公開する。

| KOIKI | 対応 Spring Boot | リリース | OSSサポート終了 | 移行期間 |
|---|---|---|---|---|
| N | 11月マイナー | 同年12月 | Spring公式日付に合わせてリリース時に明示 | 実際のOSSサポート残存期間 |
| N+1 | 翌年11月マイナー | 翌年12月 | Spring公式日付に合わせてリリース時に明示 | 12か月必要な場合は商用延長サポートを利用 |

### 8.3 サポートウィンドウ

| 種別 | 内容 |
|---|---|
| **OSS サポートウィンドウ** | 対応する Spring Boot マイナーの OSS サポート終了日まで。KOIKI はこの期間、セキュリティパッチと重大バグ修正を提供する |
| **サポート終了後** | Spring Boot 側が OSS パッチを出さないため、**KOIKI もセキュリティパッチを提供できない。**この制約を顧客へ明示する |
| **延長サポート（オプション）** | 顧客が商用サポート契約（Tanzu Spring 等）を保有する場合、KOIKI は当該ラインのビルド検証を継続する。工数は別途とする |

**「LTS」という語をKOIKIでは使用しない。**OSSと商用延長サポートで条件が異なるため、単一の「LTS」表記ではなく、**リリース時点でサポート終了日を日付で明示する。**

なお、商用サポートのリリースは Maven Central へ公開されず、契約者向けリポジトリからのみ取得する。当該構成での BOM と Starter の解決可否は Phase 5 で検証する。

### 8.4 顧客への更新義務

- **標準保守契約に「年1回のフレームワーク更新」を含めることを標準とする。**
- 更新を行わない場合、KOIKI がセキュリティパッチを提供できなくなることを契約時に明示する。
- 更新できない事情（規制対象システム、依存ライブラリ未対応、他プロジェクトとの並行）がある案件に対しては、**塩漬けを容認するのではなく、商用延長サポートという正規の選択肢を提示する。**

**更新コストの負担者**は案件ごとの商流判断とし、KOIKI 側は次の2案を標準提示とする。

1. 標準保守契約に年次更新を含め、費用を年額に織り込む
2. 更新作業を都度見積もりとし、KOIKI が提供する OpenRewrite レシピ（§8.6）により工数を圧縮する

### 8.5 Semantic Versioning と Deprecation

- Semantic Versioning を基本とする。
- **KOIKI の Public API を削除する場合、1メジャー前に非推奨化する。**したがって最低12か月の猶予が保証される。
- 非推奨化にあたっては、代替手段と OpenRewrite レシピ（提供可能な場合）を同時に示す。
- Spring Boot 側の変更に起因する破壊的変更については、この猶予を保証できない場合がある。その旨をリリースノートに明記する。
- **API の非推奨は RFC 9745 準拠の標準形式で表明する**（詳細は §12.1）。独自ヘッダを設計しない。
- **japicmp により Public API の破壊的変更を機械的に検出する**（品質ゲートの詳細は §21.4）。規約の記載だけでは、非推奨化を経ない削除を防げない。

### 8.6 移行支援

- **KOIKI メジャー更新ごとに OpenRewrite レシピを同梱する**（`koiki-migration-recipes`）。
- Spring Boot 自体の変更は Spring 公式の OpenRewrite レシピに委ねる。**KOIKI は自身の API 変更（Starter のプロパティ名、アノテーション、契約の変更）のみを担当する。**
- **レシピ自体を CI で検証する。**Reference Application および Project Template の旧メジャー版に対してレシピを適用し、テストが通ることをリリース条件とする。
- レシピが適用できない変更については移行手順書で補う。**「レシピですべてが移行できる」とは謳わない。**

### 8.7 第三者ライブラリの採用と追従

#### 採用基準

Spring 公式ポートフォリオ外のライブラリを KOIKI の公式プロファイルの依存へ含める場合、次の基準を満たすことを確認し、ADR に記録する。

1. **メンテナンスの活発性** — 直近12か月にリリースがあり、課題への応答がある
2. **Spring Boot 追従実績** — Spring Boot のメジャー／マイナー更新に対し、KOIKI のリリースサイクルに間に合う速度で追従した実績がある
3. **ライセンス** — Apache 2.0 等、企業利用に支障のないライセンスである
4. **依存範囲の限定** — 利用する機能を明示列挙し、当該ライブラリの契約がアプリケーション全体へ広がらない
5. **代替手段の存在** — 当該ライブラリが利用不能になった場合の自前実装コストを見積もり、ADR に記載する
6. **SBOM と脆弱性検査** — リリース成果物の SBOM へ記載し、CI の脆弱性検査対象へ含める

#### 追従が滞った場合の判断

**原則：プロファイル1つのために KOIKI 本体のリリースを止めない。**

| 経過期間 | 対応 |
|---|---|
| Spring Boot 新版リリースから3か月 | 対応版が出ない場合、代替実装への切り替え検討を開始する |
| 6か月 | 代替実装へ切り替えるか、**当該プロファイルのみ旧ラインへ据え置く**（本体は新ラインでリリースする） |
| 恒常的に追従しない | 採用基準を再評価し、当該ライブラリの採用を取り消す |

判断はアーキテクチャオーナー（§9.4）が行う。

### 8.8 セキュリティ修正と脆弱性管理

- セキュリティ修正の影響範囲と更新手順を明記する。
- SBOM をリリース成果物として生成する。
- 依存ライブラリの脆弱性検査を CI へ組み込む。

---

## 9. ガバナンスと所有権

> **本章は「誰が、何を根拠に決めるか」を定める。**所有権分離（§3.1）を原則の表明に留めず、運用可能な形にすることを目的とする。

### 9.1 昇格の不可逆性

§8.5 の Deprecation ポリシーにより、**Framework へ昇格した時点で最低12か月の後方互換義務が発生する。**さらに §8.6 により、破壊的変更時は OpenRewrite レシピの提供義務を伴う。

**昇格は事実上不可逆である。**軽量な降格パスは存在しない。削除には非推奨化を経た1メジャー分の猶予を要する。したがって昇格の判定は慎重に行う。

### 9.2 Framework への昇格チェックリスト

本チェックリストは、Reference、CustomerまたはWalking Skeletonで実装された候補を、再利用可能なFramework契約へ昇格する場合に適用する。グランドデザインでPhase 1からのFramework基盤として明示的に定義済みの項目は、「2案件の実績」を待つ昇格候補には含めない。ただし、それらもFrameworkの品質ゲート、Public API review、Ownership規約の対象とする。

**昇格候補は次の全項目を満たすことを条件とする。**

1. **実利用実績** — 2つ以上の独立した案件で、実質的に同一の契約として使われている（コードのコピーではなく、同じ振る舞いを期待している）
2. **契約の安定性** — 直近6か月または2案件分の開発期間において、インターフェースの破壊的変更がない
3. **代替不可能性** — Spring 標準または既存ライブラリで代替できない（§5 原則1）
4. **横断性** — 特定業務ドメインに依存せず、業務語彙が API に現れない
5. **品質** — Framework の品質ゲート（§21.4）を満たすテストがある
6. **文書** — 使用方法、設定項目、拡張点、制約が文書化されている
7. **義務の受諾** — Deprecation ポリシー（§8.5）と OpenRewrite レシピ提供義務（§8.6）を受け入れる判断がなされている

**昇格の決定は ADR として記録する。**判断根拠を残し、後任者が追跡できるようにする。

### 9.3 Framework へ入れないもの

**実務上はこちらの一覧の方が重要である。**

- 1案件でしか使われていないもの
- 特定業務ドメインの知識を含むもの
- Spring 標準で代替できるもの
- 頻繁に変わる可能性が高いもの
- 顧客固有の外部システム連携

**この一覧を Agent Skills に含める**（§24.2）。AI コーディングエージェントは「共通化できそうなもの」を Framework へ寄せる傾向があるため、明示的な打ち消しが必要である。

### 9.4 アーキテクチャオーナー

**アーキテクチャオーナー1名を任命する。**本RepositoryではPrimary Maintainerがこの役割を担う。
複数の意思決定可能なMaintainerが参加した時点で代理者1名を任命する。一人projectの間は
実在しない代理者を形式的に置かず、Owner不在時の最終判断を停止し、設計文書、ADR、検証記録、
Git履歴で継続性を確保する。詳細は`../governance/KOIKI-JavaWeb-FW_Architecture_Governance_v0.1.md`に定める。

#### 責務

| 責務 | 参照 |
|---|---|
| Framework への昇格・非昇格の最終判断 | §9.2 |
| ADR の承認 | §30 |
| 業務モジュールの Tier 妥当性の四半期見直し | §11.5 |
| Spring Modulith 採用 Level の移行判断 | §6.4 |
| 第三者ライブラリの採用審査、および追従が滞った場合の判断 | §8.7 |
| Spring Boot 追従および Java ベースラインの見直し | §6.1、§6.2、§8.1 |
| サポートラインの維持・終了判断 | §8.3 |
| 保留中の将来構想（マルチテナンシー等）の再評価 | §4.3 |
| Public API 変更の承認 | §9.6 |
| Phase 完了の判定 | §27 |

### 9.5 四半期アーキテクチャレビュー

開発活動がある各四半期に、Architecture Ownerが次のアジェンダでreviewを実施する。
一人projectでは自己reviewを認めるが、結果を`docs/architecture/reviews/`へ記録する。

1. 昇格候補の審議（§9.2 のチェックリストによる）
2. 業務モジュールの Tier 妥当性の見直し（§11.5）
3. Spring Modulith 採用 Level の見直し（§6.4）
4. 第三者ライブラリの追従状況確認（§8.7）
5. Spring Boot および Java ベースラインの見直し（§6.1、§6.2）
6. Public API 変更の確認（japicmp レポートのレビュー）
7. Agent Skills の妥当性確認（§24.2）
8. 保留中の将来構想の再評価（§4.3）
9. 未完了DoD、例外、risk、次四半期の再判断対象

### 9.6 Public API 境界

#### 強制手段

| 手段 | 適用範囲 |
|---|---|
| **`internal` パッケージ規約 ＋ ArchUnit** | 全体。`org.koikifw.<module>.internal.**` を非公開とし、Framework 外からの参照を禁止する |
| **Maven モジュール分割**（`-api` / `-impl`） | 拡張点・SPI となる重要な契約に限定して併用する。対象は Phase 1a で確定する |
| **japicmp** | Public API の破壊的変更を CI で検出する（§8.5） |

**JPMS（`module-info.java`）は採用しない。**コンパイル時・実行時に境界を強制でき最も強力だが、Spring Boot の executable JAR は classpath 実行を前提としており、リフレクションを多用する Spring との相性も悪い。導入コストと運用リスクが、得られる強制力に見合わない。

#### 規約

- 非公開パッケージのクラスは、Javadoc に公開 API でない旨を明記する
- japicmp の対象は Framework の Public API のみとし、`internal` パッケージは除外する
- **意図的な破壊的変更は、非推奨化を経ていることをアーキテクチャオーナーが確認したうえで**例外設定する
- KOIKI メジャー更新時に japicmp のベースラインを更新する

#### 顧客側での規約強制

**ArchUnit ルールは KOIKI のリポジトリでしか実行されない。**顧客アプリケーションのリポジトリで `internal` パッケージへの参照や、その他の構造規約を検出するには、ルールを顧客側のビルドへ配布する必要がある。

**`koiki-archunit-rules` をアーティファクトとして提供し、Project Template のテストへ組み込む。**これを行わない場合、本書が定める機械的担保の大半は顧客リポジトリにおいて機能しない。

この点は Public API 境界に限らず、**本書が定める全 ArchUnit ルール**（Tier 検査、Domain Model の露出防止、永続化技術の混在検出等）に共通して当てはまる。

## 10. アーキテクチャ全体構造

> 本章はモジュール**間**の境界を扱う。モジュール**内**の構造は §11 に定める。

### 10.1 モジュラーモノリス

KOIKI-JavaWeb-FW はモジュラーモノリスを基本とする。単一アプリケーションとしてデプロイ可能でありながら、業務モジュール間の依存を制御し、将来的な分離可能性を保つ。

```text
Application
├── customer
├── order
├── approval
├── identity
└── shared-kernel（最小限）
```

分割の第一軸は**業務機能**である（§5 原則6）。技術レイヤーによる全体横断分割は行わない。

### 10.2 モジュールの公開範囲

業務モジュールは、公開パッケージと非公開パッケージを区別する。他モジュールから参照できるのは公開パッケージに限定する。

| 参照可否 | パッケージ |
|---|---|
| **参照してよい** | `domain.event`（モジュールが外部へ公開する業務イベントの契約） |
| **参照してはならない** | `domain.model`、`domain.service`、`domain.repository`、`domain.gateway`、`application`、`adapter` |

**モジュール間の連携は Domain Event を経由する。**他モジュールの Bean を直接呼び出さない（§17.3）。

Spring Modulith の Named Interface を用いてモジュール公開範囲を記述するかは Phase 1b で判断する。当面は ArchUnit による検査を主とする（§21.3）。

### 10.3 依存の方向

§3.2 に定めた一方向の依存関係を、モジュール境界においても維持する。

```text
Inbound Adapter（REST / MVC / Event / Batch / Message）
  -> Application Use Case
    -> Domain
    -> Outbound Port
      -> Outbound Adapter（DB / External API / File / Messaging）
```

加えて、次を原則とする。

- **KOIKI Framework は Reference Application および Customer Application へ依存しない**
- **モジュール間に循環依存を作らない**
- Controller から Repository を直接呼び出さない

これらは ArchUnit で検査する（§21.3）。

### 10.4 shared-kernel

複数モジュールが共有する型（識別子の型、共通の Value Object、共通の列挙）を置く。**最小限に留める。**

shared-kernel が肥大化する場合、モジュール分割の位置が誤っている可能性を疑う。四半期アーキテクチャレビュー（§9.5）の確認対象とする。

---

## 11. 業務モジュールの内部構造

### 11.1 構造 Tier の考え方

**すべての業務モジュールに同一の内部構造を強制しない。**モジュールは次の2つの構造 Tier のいずれかを宣言する。

| Tier | 名称 | 適用対象 | Domain 層 | 業務ルールの置き場 |
|---|---|---|---|---|
| Tier 1 | Simple | マスタ保守、参照系、単純 CRUD、設定管理 | なし | Application Use Case |
| Tier 2 | Rich | 不変条件、状態遷移、業務ルールを持つ領域 | あり | Domain Model / Domain Service |

Tier は業務モジュール単位で宣言する。**1モジュール内で Tier を混在させない。**

#### 宣言方法

モジュールルートの `package-info.java` にアノテーションを付与する。

```java
@NullMarked
@KoikiModule(
    name = "expense",
    tier = ModuleTier.RICH,
    persistence = PersistenceTechnology.JPA,
    persistenceModel = PersistenceModel.SHARED
)
package com.example.application.expense;
```

**Tier 宣言そのものを ArchUnit の検査対象とし、宣言の欠落を検出する。**ArchUnit は宣言された Tier および永続化技術に応じた規則セットを適用する（§21.3）。

#### この方式を採る理由

一律にフル構造を強制すると、単純な CRUD に対して Domain Model・Port・永続化モデル・DTO と実質同一データの型が4〜5個生じる。一方、Domain 層を持たない構造のみとすると、不変条件や状態遷移を持つ領域で業務ルールが Application 層へ堆積する。

**Tier 制により、刷新のコストを複雑な領域に限定できる。**同時に、昇格トリガ（§11.5）により「必要な領域で Domain Model を厚くする」という方針が、判断の先送りによって形骸化することを防ぐ。

### 11.2 Tier 1（Simple）の構造

```text
com.example.application.<feature>
├── domain
│   └── event                     # 外部へ公開する業務イベント（発行する場合のみ）
├── application
│   ├── usecase
│   ├── command
│   ├── query
│   └── dto
├── adapter
│   ├── inbound
│   │   ├── api
│   │   ├── mvc
│   │   ├── event                 # アプリケーションイベントのリスナー
│   │   ├── batch
│   │   └── message               # 外部メッセージング
│   └── outbound
│       ├── persistence
│       ├── external
│       ├── file
│       └── messaging
└── configuration
```

- **Domain 層を持たない。**業務ルールとユースケース調整の双方を Application Use Case が担う
- **Outbound Port を設けない。**Application Use Case は `adapter.outbound.persistence` の Spring Data Repository を直接利用してよい。Spring Data の Repository インターフェース自体が抽象の役割を果たすため、その上へさらに Port を重ねない
- 永続化モデルをデータモデルとして扱う（振る舞いを持たせない）
- **実際に使用しないサブパッケージは作成しない**

この構造は KOIKI-PYFW の `API → Service → Repository → Model` とほぼ同型であり、移行時の学習コストが最小になる。

> **`domain.event` は Tier に依存しない。**モジュールが外部へ公開する業務イベントの契約を置く場所であり、Domain 層の有無とは独立した概念である。Tier 1 と Tier 2 の違いは `domain.model`、`domain.service`、`domain.repository`、`domain.gateway` の有無に現れる（§11.8）。

### 11.3 Tier 2（Rich）の構造

Tier 1 との差分は `domain` パッケージの内容のみである。

```text
com.example.application.<feature>
├── domain
│   ├── model                     # 業務モデル。既定では JPA Entity を兼用する
│   ├── service                   # Domain Service
│   ├── event                     # Domain Event
│   ├── repository                # 永続化 Port
│   └── gateway                   # 外部システム連携 Port
├── application
│   ├── usecase
│   ├── command
│   ├── query
│   └── dto
├── adapter
│   ├── inbound
│   │   ├── api
│   │   ├── mvc
│   │   ├── event
│   │   ├── batch
│   │   └── message
│   └── outbound
│       ├── persistence
│       ├── external
│       ├── file
│       └── messaging
└── configuration
```

#### Repository の扱い

`domain.repository` に Repository インターフェースを置き、**Spring Data に実装を生成させる。**Adapter クラスを記述しない。

```java
package com.example.application.order.domain.repository;

public interface OrderRepository extends Repository<Order, OrderId> {
    Optional<Order> findById(OrderId id);
    Order save(Order order);
}
```

- `domain` が Spring Data Commons（`Repository`、`Pageable` 等）へ薄く依存することを許容する。依存範囲は Spring Data Commons に限定し、`domain` から `EntityManager` や Spring Web を参照しない
- 依存を最小化するため、`JpaRepository` ではなくマーカーの `Repository<T, ID>` を継承し、**必要なメソッドのみを宣言する**
- **手書きの Outbound Adapter は、DB 以外の Outbound（外部 API、ファイル、メッセージング）にのみ用いる。**実装差し替えの現実味があるのはこれらであり、DB ではない
- ただし永続化技術として MyBatis を採用する場合は例外とする（§11.7）
- 複雑queryの契約とread modelは`application.query`に置き、`adapter.outbound.persistence`にmaterialize実装を置く（§16.3）

#### 外部システム連携の Port

`domain.gateway` に Port を定義し、`adapter.outbound.external` に実装を置く。実装差し替えの現実味がある場合に用いる。

Tier 1 では `gateway` を設けず、Application Use Case が `adapter.outbound.external` を直接利用してよい。

### 11.4 責務

| 層 | 責務 | 原則として置かないもの | Tier |
|---|---|---|---|
| Domain | 業務モデル（不変条件と状態遷移）、Value Object、Domain Service、Domain Event、Port | Controller、Spring Web、HTTP DTO、`EntityManager` の直接操作、SQL | Tier 2 のみ（`domain.event` を除く） |
| Application | Use Case、トランザクション境界、権限呼出、処理調整、（Tier 1 では業務ルール） | HTTP 詳細、SQL、画面描画 | 共通 |
| Inbound Adapter | REST、MVC、Event、Batch、Message の入力受付と応答整形、DTO 変換 | 業務ルール、永続化処理、**業務モデルの外部露出** | 共通 |
| Outbound Adapter | 外部 API、File、Messaging、複雑queryとread modelのmaterialize実装 | Use Caseの判断、Application所有のQuery契約 | 共通 |
| Configuration | Bean 構成、Adapter 選択 | 業務ルール | 共通 |

### 11.5 Tier の昇格

Tier 1 のモジュールが次のいずれかに該当した場合、Tier 2 へ昇格させる。

1. 業務状態が3状態以上あり、状態間の遷移規則が存在する
2. 複数エンティティにまたがる不変条件が存在する
3. 同一の業務ルールが2つ以上の Use Case から必要になった
4. Use Case 内の条件分岐が、技術都合ではなく業務ルールに由来して増加している

#### 運用

- 昇格トリガの該当有無を Review Checklist（§24.1）の必須確認項目とする
- **四半期アーキテクチャレビューでモジュール一覧の Tier 妥当性を見直す**（§9.5）
- Tier 2 から Tier 1 への降格は原則として行わない

#### 昇格の実作業

既定のモデル方針（§11.6 兼用方式）のため、昇格は「永続化モデルを `adapter.outbound.persistence` から `domain.model` へ移動し、振る舞いを与え、Repository インターフェースを `domain.repository` へ移す」という**段階的な移動**で完了する。モデルの二重化やデータ移行を伴わない。

**昇格が軽量であることは、昇格トリガの実効性を支える。**昇格が事実上の作り直しになる方式では「面倒だから Tier 1 のまま」という力学が働く。

### 11.6 Tier 2 のモデル方針 — 兼用を既定とする

**既定は兼用とする。**JPA Entity を業務モデルとして扱い、不変条件と状態遷移を Entity 自身が持つ。業務モデルと永続化モデルを別クラスに分離しない。

#### 兼用を既定とする理由

- JPA の dirty checking、`@Version` による楽観的ロック、遅延ロード、DB 採番をそのまま利用でき、これらを代替する契約設計（識別子のドメイン側採番、version の層間往復、明示保存手順）が不要になる
- **Domain Unit Test は兼用でも成立する。**JPA アノテーションはメタデータであり、`new` してビジネスルールを検証する単体テストに `EntityManager` は不要である
- Spring / JPA の標準的な記述に沿うため、開発者および AI コーディングエージェントの前提知識と乖離しない
- KOIKI-PYFW からの概念的距離が「業務ルールを Service からモデル自身へ移す」の一点に収まる

#### 受け入れるコスト

- 引数なしコンストラクタが必要（`protected` で可）、フィールドに `final` を使えない、コレクションは JPA 管理可能な型に限定される
- Value Object は `@Embeddable` とし、Domain 側に JPA 依存が入る
- DB スキーマの形がモデルの形に影響する

#### 兼用時の規約

1. **setter を公開しない。**状態変更は業務上の意味を持つメソッドを通す（`order.approve(approver)` 等）
2. **引数なしコンストラクタは `protected` とし、生成は業務上有効な状態を保証するコンストラクタまたはファクトリメソッドに限定する**
3. **コレクションは変更不可ビューで公開し、要素の追加・削除は業務モデルのメソッドを通す**
4. **業務モデルを Inbound / Outbound の入出力型として使用しない。**REST・MVC の応答、外部連携の送受信は必ず DTO へ変換する。兼用方式では分離方式が自動的に与えていた「モデルの外部流出防止」が働かないため、**この規約は ArchUnit で機械検査する**（§13.3、§21.3）
5. **参照専用の一覧・検索・帳票・集計はread modelを用いてよい。**Query契約と戻り値のread modelは`application.query`が所有し、`adapter.outbound.persistence`がJPA射影、JdbcClientまたはMyBatisで実装する。ApplicationからAdapterを直接参照せず、業務モデルも経由させない
6. **Value Object は `@Embeddable` とし、setter を公開しない**
7. **`equals` / `hashCode` は識別子ベースで実装し、遅延ロードプロキシを考慮する。**型比較に `getClass()` を用いない

> **Open Session in View との関係** — KOIKI は `spring.jpa.open-in-view=false` を既定とする（§22.1）。OSIV はビュー描画への永続化コンテキスト延長であり、**トランザクション内の dirty checking には影響しない。**兼用方式の利点は OSIV 無効化と両立する。

### 11.7 モデル分離のオプトイン

次のいずれかに該当するモジュールに限り、業務モデルと永続化モデルを分離してよい。

1. 永続化スキーマを変更できず、かつ業務モデルと構造が乖離している（既存 Oracle 資産の移行など。§4.2 の対象に含まれる）
2. 同一の業務モデルを複数の永続化先へ保存する必要がある
3. スキーマ設計上の制約により、モデル側で守るべき不変条件を表現できない
4. **永続化技術として MyBatis を採用する場合**（dirty checking が存在しないため、兼用方式が成立しない）

#### 分離方式の構造

**永続化技術により、SQL 実行手段のパッケージのみが異なる。それ以外は共通である。**

```text
# JPA 分離方式                            # MyBatis 分離方式
domain/                                   domain/
├── model        # JPA 非依存の POJO      ├── model        # JPA 非依存の POJO
├── service                               ├── service
├── event                                 ├── event
├── repository   # Port                   ├── repository   # Port
└── gateway                               └── gateway

adapter/outbound/persistence/             adapter/outbound/persistence/
├── entity       # JPA Entity             ├── entity       # 永続化レコード（POJO）
├── converter    # Domain ⇔ entity        ├── converter    # Domain ⇔ entity
├── jpa          # Spring Data Repository ├── mapper       # MyBatis Mapper
├── query         # Query Port実装       ├── query        # Query Port実装
└── *RepositoryAdapter                    └── *RepositoryAdapter
```

#### 各パッケージの責務

| パッケージ | 責務 |
|---|---|
| `entity` | 永続化専用モデル。DB のテーブル構造に対応する。JPA 方式では JPA Entity、MyBatis 方式では単純な POJO |
| `converter` | 業務モデルと `entity` の相互変換。**手書きとし、リフレクションベースの自動マッピングを用いない**（§16.6） |
| `jpa` / `mapper` | SQL 実行手段。**メソッドシグネチャに `domain.model` の型が現れてはならない**（ArchUnit で検査） |
| `query` | `application.query`が所有するQuery Portの実装。Application所有のread modelを直接materializeし、**`converter`を経由しない** |
| `*RepositoryAdapter` | `domain.repository` の Port 実装。SQL 実行手段と `converter` を組み合わせる。**楽観ロックの更新件数チェックはここで行う**（§12.5） |

> **`mapper` という語は MyBatis の SQL 定義インターフェースを指す。**変換層は `converter` と呼び、両者を混同しない。

#### 実装例（MyBatis 分離方式）

```java
class OrderRepositoryAdapter implements OrderRepository {   // domain.repository の Port

    private final OrderSqlMapper sqlMapper;      // MyBatis Mapper
    private final OrderConverter converter;

    @Override
    public Optional<Order> findById(OrderId id) {
        return Optional.ofNullable(sqlMapper.selectById(id.value()))
                       .map(converter::toDomain);
    }

    @Override
    public void save(Order order) {
        OrderEntity entity = converter.toEntity(order);
        int updated = sqlMapper.update(entity);              // WHERE version = ?
        if (updated == 0) {
            throw new OptimisticLockingFailureException("Order", order.id());
        }
    }
}
```

#### 復元に関する規約（分離方式・兼用方式に共通）

**(1) 復元用ファクトリメソッドを設ける**

`converter` が永続化モデルから業務モデルを生成する手段が必要となる。業務的な生成とは別に、**復元用の静的ファクトリメソッド**を設ける。

```java
public class Order {
    public static Order place(CustomerId customerId, List<OrderLine> lines) { ... }   // 業務的生成
    public static Order reconstitute(OrderId id, OrderStatus status, long version, ...) { ... }  // 復元
}
```

- 復元用メソッドの名称は **`reconstitute`** を標準とする
- `converter` は別パッケージにあるため可視性は `public` とする。**業務モデルの API に永続化という技術的関心が現れることを許容する**
- **`converter` 以外から `reconstitute` を呼ばない**

**(2) 復元時に不変条件を再検証しない**

DB に既存の不正データが存在する場合に読み込めなくなると、システムが停止する。**不変条件は状態変更時に守り、復元時には検証しない。**

これは兼用方式とも一貫する。兼用方式では JPA がリフレクションによりオブジェクトを復元するため、同様に不変条件の検証は行われない。

**(3) read modelは復元経路を持たない**

参照専用の結果型は業務モデルではないため、不変条件も復元用ファクトリも持たない。SQL 実行手段が直接マッピングする。

#### 分離採用時の追加規約

- 識別子はドメイン側で採番する（DB 採番に依存できないため。§16.5）
- 楽観的ロックの version を業務モデルにも保持する（§12.5）
- dirty checking を利用せず、明示保存の手順を Adapter が担う
- 集約は1回の Use Case で完全に materialize できる粒度に保つ

#### 運用

- 分離の採用はモジュール単位とし、`@KoikiModule` に `persistenceModel = SEPARATED` を宣言する
- **`persistence = MYBATIS` を宣言した場合、`persistenceModel = SEPARATED` が必須となる**（ArchUnit で検査）
- **JPA 分離方式は Reference Application で実証しない**（§26.4）。分離トリガ1に該当するケースは MyBatis との親和性が高く、JPA 分離は稀と判断したためである。採用する案件が生じた場合、Reference への追加を検討する

### 11.8 Domain Event の定義規約

> 本節はイベントの**定義**を扱う。同期／非同期の選択と配信方式は §17.3 に定める。

1. **不変の `record` として定義する。**setter を持たず、生成後に状態が変わらないこと
2. **ペイロードに業務モデル（JPA Entity）を含めない。**識別子と値のみを持つ
   - 兼用方式では業務モデルが JPA Entity であるため、非同期リスナーへ渡すと detached entity となり遅延ロードで例外が発生する。同期リスナーでも、受け手が発行側の永続化状態に依存することは避ける
3. **`domain.event` はモジュールの公開パッケージとする。**他モジュールが購読するため、「`domain.model` の型が他モジュールから参照されない」という規則に対する明示的な例外である
4. **命名は `<集約名><過去分詞>` とする。**例: `OrderConfirmed`、`ApprovalCompleted`、`DepartmentDeactivating`
5. **Spring Modulith Level 2 到達後は、イベント型の後方互換を維持する。**Event Publication Registry はイベントを JSON として永続化し再送するため、クラス名・パッケージ名・既存フィールドの変更は再送を破壊する。フィールドの追加は可、削除・改名は不可とする。互換性を壊す変更が必要な場合は新しいイベント型を定義する

### 11.9 実用的 DDD

Aggregate、Domain Event、Factory 等のパターンを全モジュールへ強制しない。適用範囲は Tier で制御する（§11.1〜§11.5）。

Tier 2 の内部においても、Domain Event や Factory は必要が生じた時点で導入する。**Tier 2 の宣言は、Domain 層と Repository インターフェースを持つことを意味するのであって、DDD の全パターンを実装することを意味しない。**

---

## 12. Web API 設計

### 12.1 標準契約

- 明示的な API バージョニング（§12.2）
- 入力 DTO と出力 DTO の分離
- Jakarta Validation による構文・形式検証
- Domain／Application による業務検証
- RFC Problem Details を基礎とする統一エラー形式（§12.4）
- 安定した KOIKI エラーコード
- 許可項目を限定したソート、検索、ページング
- リソース更新時の楽観的ロック（§12.5）
- 必要な操作に対する Idempotency-Key
- request ID、trace ID の発行・伝播
- OpenAPI 定義
- Production における API ドキュメント公開制御

### 12.2 API バージョニング

**Spring Framework 7 の標準機構を採用し、独自実装を行わない。**戦略は**パスセグメント方式**とする。

```java
@Configuration
public class KoikiApiVersionConfig implements WebMvcConfigurer {
    @Override
    public void configureApiVersioning(ApiVersionConfigurer configurer) {
        configurer.usePathSegment(1)          // /api/v1/... の "v1"
                  .addSupportedVersions("1");
    }
}
```

```java
@GetMapping(path = "/accounts/{id}", version = "1")
```

#### 規約

- **メジャーバージョンのみをパスに含める**（`v1`、`v2`）。マイナーバージョニングは行わない
- 後方互換な変更ではバージョンを上げない。破壊的変更のみメジャーを上げる（§8.5 と整合）
- **Thymeleaf 経路（MVC）にはバージョニングを適用しない。**UI は内部契約であり、外部公開 API とは扱いが異なる
- 外部 API を呼び出す際は `ApiVersionInserter` を用いる（`RestClient`、HTTP Service Client、`RestTestClient` で共通）

#### API の非推奨

**Spring Framework 7 の API バージョニングは RFC 9745 準拠の非推奨処理を備える。**KOIKI の API 非推奨化は、独自ヘッダではなくこの標準形式で表明する。§8.5 の Deprecation ポリシーと統合して運用する。

### 12.3 Controller の責務

Controller は次に限定する。

- リクエスト受信
- 入力検証
- 認証主体・パラメータの Use Case への引き渡し
- ステータスコード、ヘッダー、レスポンス DTO の整形

**Controller から Repository を直接呼び出さない。**非自明な業務処理を Controller へ置かない。

**業務モデル（`domain.model` の型）を引数・戻り値・`Model` 属性に用いない**（§11.6 規約4、§13.3）。ArchUnit で検査する。

### 12.4 エラー処理と統一エラー形式

- RFC Problem Details を基礎とする統一エラー形式を用いる
- 安定した KOIKI エラーコードを付与する
- 未処理例外は統一エラー形式へ変換する

#### Jackson 3 に関する注意

**Jackson 3 は検査例外（`JsonProcessingException`）ではなく、非検査例外の `JacksonException` を投げる。**

`catch (IOException)` では捕捉されないため、**`JacksonException` を統一エラー形式へマッピングする処理を明示的に追加する。**これはコンパイルが通り、例外経路を通らないテストでは検出されない。

#### Spring 例外の変換

MyBatis-Spring は MyBatis の例外を Spring の `DataAccessException` へ変換する。したがって永続化技術によらず、`DataAccessException` を基点とした統一的な変換が成立する。

### 12.5 競合制御

更新系では、未存在と更新競合を区別し、競合は **409 Conflict** 等の統一応答へ変換する。

#### API 契約（経路によらず統一）

- ETag／If-Match、または更新リクエストへ version を含める
- **JPA 経路と MyBatis 経路で、API から見た契約は同一とする**

#### 実装（経路別）

| 経路 | 実装 |
|---|---|
| **JPA 兼用方式** | `@Version` により自動的に制御される。競合時は `OptimisticLockingFailureException` |
| **JPA 分離方式** | 業務モデルにも version を保持し、Inbound → Use Case → Domain → Adapter → JPA Entity の経路で往復させる |
| **MyBatis 方式** | `UPDATE ... SET version = version + 1 WHERE id = ? AND version = ?` の**更新件数チェック**。0 件の場合、`OptimisticLockingFailureException` を送出する |

`OptimisticLockingFailureException` を共通の送出型とすることで、**§12.4 のマッピングと API 応答が経路によらず統一される。**

#### MyBatis 経路のリスク

JPA 経路では楽観ロックが自動的に効くのに対し、**MyBatis 経路では実装漏れが起こりうる。**更新件数チェックの記述を忘れてもコードは正常に動作するため、ArchUnit では検出が困難である。

**Review Checklist（§24.1）の必須確認項目とし、競合検出テストを必須とする**（§21.1）。

#### UI 経路

Thymeleaf 経路では、競合時に専用の競合画面へ遷移する（§13.2）。

---

## 13. UI プロファイル

### 13.1 位置づけと API との共存

KOIKI の中核は API 基盤であり、UI はその上に載るプロファイルである。**Thymeleaf ＋ HTMX プロファイルと SPA プロファイルを対等の公式プロファイルとし、いずれかを「第一標準」とはしていない。**

いずれのプロファイルも必須依存ではなく、API-only システムが不要な依存を持ち込まないよう独立して提供する。

```text
REST Controller（API / SPA向け）  --+
MVC Controller（Thymeleaf向け）   --+--> Application Use Case --> Domain
```

**共通化の対象は Application Use Case、Domain、認可、Repository、変換規約である。**REST Controller と MVC Controller を無理に共通化しない。

#### 提供時期による優先順位

| プロファイル | 参照実装の提供 |
|---|---|
| Thymeleaf ＋ HTMX | Phase 3 |
| SPA | 契約の文書化は Phase 3、最小参照実装は Phase 4 |

**「標準」という呼称で序列をつけず、Phase 配置で優先順位を表明する。**

#### 投資額の非対称性について

SPA プロファイルが提供する内容（Cookie Session、Bearer Token、CSRF、CORS、OpenAPI、エラー形式）は、§12 および §14 で既に提供が決まっているものである。**SPA 支援は API 基盤の副産物としてほぼ追加コストなく提供できる。**

一方、Thymeleaf プロファイルの標準機能（§13.2）は独立した新規開発である。この非対称性を踏まえ、**Thymeleaf コンポーネント群を先に整備する**という Phase 配置としている。

### 13.2 Thymeleaf プロファイル

`koiki-starter-web-mvc` として提供する。HTMX を同梱する（§13.4）。

#### 標準機能

- 共通レイアウト
- ヘッダー、メニュー、パンくず
- ログインユーザー表示
- CSRF hidden field
- Validation エラー表示
- Flash Message
- PRG パターン
- 二重送信防止
- ページング
- 一覧、詳細、登録、更新
- 楽観的ロック競合画面
- 403、404、500 画面
- 権限に応じた表示制御
- CSP nonce 等のセキュリティ支援
- アクセシビリティ規約

### 13.3 ビューモデル変換の必須化

MVC Controller は Inbound Adapter であるため、§11.6 規約4 が適用される。**Thymeleaf テンプレートへ `domain.model` の型を直接渡してはならない。**必ずビューモデル（DTO）へ変換する。

Spring MVC ＋ JPA では Entity をそのまま `Model` に載せる書き方が最も自然であり、教科書、チュートリアル、AI コーディングエージェントの出力はいずれもこの形になる。**規約の記載だけでは守られないため、機械的な検出手段を併せて定める。**

#### 13.3.1 具体的な失敗モード

**(1) OSIV 有効時に Entity を渡した場合 — 静かな性能劣化**

Thymeleaf は SpEL によりプロパティを自由に辿るため、`${order.customer.name}` のような記述1つごとに遅延ロードが発火する。100行の一覧で200回の追加クエリが**描画時に**発行されうる。これらはトランザクション外（1クエリ1オートコミット）で実行される。

さらに OSIV 有効時は EntityManager がリクエスト全体にわたって開かれるため、DB コネクションが長時間占有され、負荷時にコネクションプールが枯渇する。§20.2 の N+1 検出、§25.3 の性能目標に直結する。

**(2) OSIV 無効時に Entity を渡した場合 — エラー処理の破綻**

テンプレート描画の途中で `LazyInitializationException` が発生した時点で、**レスポンスの一部は既にクライアントへ送信済みである。**Spring のエラーハンドリングは 500 ページへ切り替えられず、利用者には途中で切れた HTML が表示される。§12.4 の統一エラー形式も §13.2 の 500 画面も機能しない。

REST では Jackson の例外時にレスポンスがまだバッファ内にあることが多く、統一エラー形式を返せる。**Thymeleaf の方が失敗の質が悪い。**

**(3) フォームバインディング — マスアサインメント脆弱性**

```java
// 禁止
@PostMapping("/orders/{id}")
String update(@ModelAttribute Order order) { ... }   // Order は業務モデル
```

`@ModelAttribute` はフォームに存在しない項目でも、パラメータ名が一致すればバインドする。**兼用方式ではバインド対象が業務モデルそのものであるため、業務不変条件を迂回した状態書き換えが可能になる。**§14 のセキュリティ方針に抵触する。

**(4) テンプレートからの業務メソッド呼出（兼用方式に固有）**

Thymeleaf の SpEL は public メソッドを呼び出せる。貧血な Entity であればテンプレートができるのは読み取りのみだが、**兼用方式の業務モデルは業務メソッドを持つため、テンプレートから状態遷移を起動できてしまう**（`${order.approve(user)}` は構文上通る）。OSIV 有効時はその変更が dirty checking でコミットされる。

§11.6 規約1（setter を公開しない）はこれを防がない。**分離方式には存在しない、兼用方式に固有のリスクである。**

**(5) 楽観的ロック version の往復**

Entity を直接バインドすると version も含めて上書きされ、利用者由来の値と永続化由来の値を区別できなくなる。§12.5 の競合制御が成立しない。

#### 13.3.2 推奨パターン

**参照系** — 業務モデルを経由せず read model を用いる（§11.6 規約5）

```java
@GetMapping("/orders")
String list(@ModelAttribute OrderSearchForm form, Model model) {
    Page<OrderSummaryView> page = orderQueryService.search(form.toCriteria());
    model.addAttribute("orders", page);   // read model。materialize 済みの値のみ
    return "order/list";
}
```

read model は完全に materialize された DTO であるため、テンプレートがプロパティを辿ってもクエリは発生しない。**Thymeleaf プロファイルにおいて read model 経路が重要になる理由である。**

**更新系** — Form → Command → Use Case → 業務モデル

```java
@PostMapping("/orders/{id}/approve")
String approve(@PathVariable OrderId id,
               @Valid @ModelAttribute ApproveOrderForm form,   // 業務モデルではない
               BindingResult binding, ...) {
    if (binding.hasErrors()) { return "order/approve"; }
    approveOrderUseCase.handle(
        new ApproveOrderCommand(id, form.version(), form.comment(), currentUser));
    return "redirect:/orders/" + id;
}
```

```java
@Transactional
public void handle(ApproveOrderCommand cmd) {
    Order order = orderRepository.findById(cmd.orderId()).orElseThrow(...);
    order.approve(cmd.approver(), cmd.comment());   // 業務メソッド経由でのみ状態変更
}
```

Form には**利用者が入力してよい項目と version のみ**を置く。これによりマスアサインメントと業務不変条件の迂回が構造的に不可能になる。

#### 13.3.3 検査手段

規約の記載だけでは守られないため、3層で担保する。

| 手段 | 検出できるもの |
|---|---|
| **`spring.jpa.open-in-view=false`**（§22.1） | Entity をテンプレートへ渡した場合、テストで必ず失敗する。**最も強力** |
| **ArchUnit**（§21.3） | フォームバインドおよび `Model` 経由の露出 |
| **Web Slice Test でテンプレート描画まで実行**（§21.1） | 描画時の遅延ロード、テンプレート内メソッド呼出 |

ArchUnit は Java コードを検査できてもテンプレートは見えない。**OSIV 無効化と、描画まで含む統合テストの組み合わせが実質的な防御線となる。**

### 13.4 HTMX

**HTMX を Thymeleaf プロファイルの構成要素として同梱する。**後続の拡張プロファイルとしない。

#### 同梱とする理由

HTMX を伴わない Thymeleaf では、検索、絞り込み、ページング、インライン編集がすべて全画面リロードとなる。その形の Reference Application では案件側が採用を見送り、**Thymeleaf コンポーネント群への投資が回収されない。Thymeleaf プロファイルの実用性は HTMX の有無に依存する。**

#### 標準化する契約

1. 全画面描画と部分描画（フラグメント）の契約
2. CSRF トークンの自動付与
3. バリデーションエラーの部分描画
4. リダイレクト（`HX-Redirect` / `HX-Location`）
5. 履歴管理（`HX-Push-Url`）
6. Out-of-Band swap の使用方針
7. ローディング表示
8. 検索入力のデバウンス
9. 動的挿入コンテンツの再初期化
10. 403 / 404 / 500 の部分描画時の扱い
11. **JavaScript 有効を前提とする**（下記）

#### プログレッシブエンハンスメントを採らない

**JavaScript 無効時の全画面フォーム送信フォールバックは提供しない。**

フォールバックを必須とすると、各操作について「HTMX による部分描画」と「全画面描画」の2経路を実装・テストすることになり、Thymeleaf プロファイルの開発量がほぼ倍増する。対象とする企業システムにおいて JavaScript 有効を前提とする制約は妥当と判断する。

ただし §13.2 の「アクセシビリティ規約」は引き続き適用対象とする。スクリーンリーダー対応、キーボード操作、フォーカス管理は JavaScript 有効下で満たす。**プログレッシブエンハンスメントを採らないことと、アクセシビリティを満たさないことは別問題である。**

#### 依存ライブラリ

HTMX と Spring Security の統合には `wimdeblauwe/htmx-spring-boot` を用いる。Spring Security の CSRF 保護は Thymeleaf の `th:action` によるフォーム送信には自動で効くが、**HTMX が発行するリクエストには効かない。**当該ライブラリは HTMX 要素の `hx-headers` へ CSRF トークンを自動注入し、`@HxRequest` 等のアノテーション、引数リゾルバ、Thymeleaf ダイアレクトを提供する。

**これは Spring 公式ポートフォリオ外のコミュニティライブラリである。**§8.7 の第三者ライブラリ採用基準を適用する。

**依存範囲の限定** — 利用する機能を「HTMX リクエスト判定アノテーション、CSRF ヘッダー自動注入、リダイレクト／OOB 用ビュー、Thymeleaf ダイアレクト」に限定し、`koiki-starter-web-mvc` 内に閉じる。

**代替手段** — 当該ライブラリが Spring Boot の新版へ追従しない場合、CSRF ヘッダー注入は `htmx:configRequest` イベントを捕捉する JavaScript で代替可能である。アノテーションと引数リゾルバは自前実装が可能であり、**KOIKI 側で1〜2人週規模の代替実装で置換できる範囲に依存を留める。**

### 13.5 SPA プロファイル

React 等の SPA を KOIKI 本体へ同梱しない。**BFF 層も設けない。**SPA は静的配信し、バックエンド API を直接利用する。

> KOIKI-PYFW が Next.js BFF を撤去して純 SPA へ移行した経験に基づく判断である。

KOIKI が提供するもの：

- Cookie Session による認証契約（`same-origin` 前提）
- CSRF double-submit 契約
- CORS 許可リスト設定
- セキュリティヘッダー、CSP
- OpenAPI 定義
- 統一エラー形式（RFC Problem Details）
- SSO / SAML の SPA コールバック契約
- リフレッシュトークン rotation・再利用検知・許可外リダイレクト URI の fail-closed（Token 方式利用時）
- 最小参照実装（Phase 4）

**専用 Starter は設けない。**`koiki-starter-api` と `koiki-starter-security` で必要な機能はすべて満たされるため、**SPA プロファイルの実体は契約文書と参照実装である。**

これらの契約は KOIKI-PYFW dev/v0.8 が構築した実績のある仕様を移植する。言語非依存の仕様であり、そのまま転用できる。

### 13.6 プロファイル併用

1つのアプリケーションが Thymeleaf 画面と SPA 向け API を同時に提供することを許容する。管理画面は Thymeleaf、利用者向けは SPA、といった構成は現実的である。

併用時の注意点：

- CSRF の扱いが異なる（Thymeleaf は hidden field、SPA は double-submit）ため、Spring Security の設定を経路ごとに分離する
- セッションは共有される。認証状態の一貫性を Phase 4 で検証する

## 14. セキュリティアーキテクチャ

### 14.1 基本原則

- Spring Security 標準機能を利用する
- 認証と認可を分離する
- UI の表示制御へ認可を依存させない
- 認証プロトコルと業務ユーザーを分離する
- 許可リスト方式を優先する
- セキュリティ設定不備は起動時に検出する
- 失敗イベントを監査対象とする
- 秘密値、Token、Password をログへ出さない

### 14.2 認証方式

| 利用形態 | 第一標準 | 補足 |
|---|---|---|
| Spring MVC / Thymeleaf | HTTP Session | CSRF、Session Fixation、Cookie 保護 |
| same-origin SPA | Secure HttpOnly Cookie Session | **Token をブラウザ Storage へ保存しない** |
| 外部 API | OAuth 2.0 Bearer JWT | issuer、audience、scope を検証 |
| サービス間 | OAuth 2.0 Client Credentials 等 | 案件要件に応じる |
| 企業 SSO | OIDC 優先 | SAML は拡張モジュール |
| **多要素認証** | **Spring Security 7 の MFA** | 標準機能として利用可能。KOIKI 標準に含めるかは Phase 2 で判断する |

### 14.3 セッションストア

**Spring Session JDBC を標準とし、既存の PostgreSQL をセッションストアとして利用する。**

#### 選定理由

§4.3 により Redis 等を必須とする基盤は対象外であり、一方で §25.2 はセッションの外部化を要求し、§23 は ECS Fargate の複数タスク構成を前提とする。この3条件を同時に満たす構成が必要である。

| 方式 | 新規インフラ | 評価 |
|---|---|---|
| ALB スティッキーセッション | 不要 | タスク入替・デプロイでセッションが失われ、負荷も偏る |
| **Spring Session JDBC** | **不要**（既存 PostgreSQL） | **採用** |
| Spring Session Redis | 必要 | §4.3 と矛盾する |

決め手は、**Spring Session の抽象により JDBC → Redis の差し替えが設定変更レベルで済む**ことである。これにより「Redis を必須としない」と「将来 Redis も選べる」を両立できる。

ALB スティッキーセッションは DB 負荷軽減のために併用してよいが、**セッションの永続性をスティッキーに依存しない。**

#### 付随して必要になるもの

- セッションテーブルの Flyway 定義（Framework が所有する。§16.7）
- **期限切れセッションの清掃ジョブ。**§19.2 の単一実行制約に従う
- 保存モード・フラッシュモードのチューニング（最終アクセス時刻の更新でリクエストごとに DB 書き込みが発生するため）

### 14.4 認可

- URL 単位の認可
- Method Security
- Role および Permission
- リソース所有権
- 業務状態に基づく操作可否
- 管理者操作の追加監査

`ROLE_ADMIN` 等の粗いロールだけで全機能を制御せず、**Permission と業務ポリシーを組み合わせる。**

**画面の非表示、ルートガード、JavaScript 制御は認可境界ではない**（§3.4）。画面上の制御を回避して直接リクエストを送っても、認可が強制されることを統合試験で検証する（§14.8）。

### 14.5 ローカル Identity

KOIKI は User、Role、Permission の標準モデルを任意モジュールとして提供する。外部 IdP 利用を強制せず、外部 IdP と業務ユーザーをリンクできる Port を持つ。

これらのテーブルは **Framework が所有する**（§16.7）。Reference Application は管理画面を提供するが、テーブルの所有権は Framework にある。

### 14.6 アカウント保護と認証試行制御

- 強固な Password Hash
- Password Policy
- **認証試行制御**（下記）
- Account Lock
- Password Reset Token
- Session 失効
- 同時 Login 制御の拡張点
- Refresh Token rotation／reuse 検知（Token 方式利用時）
- MFA（§14.2）

#### 認証試行制御

**汎用的なレート制御はインフラ層（WAF、ALB 等）の責務とし、KOIKI はアプリケーション内に実装しない**（§3.3）。ただし**認証系エンドポイントのブルートフォース対策のみ、本節で提供する。**

**この例外を設ける理由** — ECS Fargate の複数タスク構成では、インメモリのレート制限はインスタンス単位となり、実効的な上限がタスク数倍になる。厳密な分散制限には共有ストア（Redis 相当）が必要となり §4.3 と矛盾する。一方、**認証試行制御は低頻度かつ厳密性を要し、DB への試行回数記録で正確に実現できる。**

#### 仕様

- ログイン失敗回数の記録（アカウント単位、および送信元 IP 単位）
- 閾値超過時のアカウントロック
- ロック解除方式（時間経過による自動解除／管理者操作）
- **失敗および解除の記録は「セキュリティ監査」として扱う**（§15.2。`REQUIRES_NEW` の独立トランザクション）

KOIKI-PYFW dev/v0.8 に実装済みの仕様を移植する。

### 14.7 ブラウザ保護

- CSRF
- Secure / HttpOnly / SameSite Cookie
- Session Fixation 対策
- CSP
- HSTS
- X-Content-Type-Options
- Referrer-Policy
- Permissions-Policy
- Frame Ancestors 制御
- Open Redirect 防止
- CORS 許可リスト

**CSRF の扱いは UI プロファイルにより異なる**（Thymeleaf は hidden field、SPA は double-submit、HTMX はヘッダー自動注入）。併用時は Spring Security の設定を経路ごとに分離する（§13.6）。

### 14.8 セキュリティ試験

Unit Test だけで完了とせず、次を統合試験する。

- 未認証
- 権限不足
- 所有者違反
- **画面制御を回避した直接リクエスト**
- CSRF
- Token 改ざん
- 期限切れ
- Replay
- Session 失効
- 許可外 Redirect
- **認証試行制御とアカウントロック**
- 監査記録（§15.2 の分類ごとのロールバック挙動を含む）

---

## 15. 監査アーキテクチャ

### 15.1 通常ログとの分離

通常ログは障害解析と運用監視を目的とし、監査ログは「誰が、いつ、何に、何を行い、結果がどうだったか」を追跡する。保存期間、アクセス権、改ざん防止、検索性を別に設計する。

### 15.2 監査の3分類とトランザクション方針

**監査を Domain Event の非同期配信機構に乗せない。**監査イベントを次の3種別に分類し、それぞれ異なる機構で処理する。

| 種別 | 判定基準 | 実行機構 | トランザクション |
|---|---|---|---|
| **業務監査** | 業務トランザクションが存在し、そのロールバック時に監査も無効化されるべきもの | Framework が提供する Audit API を Application Use Case から直接呼び出す、または同期 `@EventListener` 経由 | **業務トランザクションと同一** |
| **セキュリティ監査** | 業務トランザクションが存在しない、または業務トランザクションのロールバックに巻き込まれてはならないもの | Framework が提供する Audit API を `REQUIRES_NEW` で呼び出す | **独立したトランザクション** |
| **副作用・連携** | 監査ではなく、監査を契機とする通知・外部連携 | `@ApplicationModuleListener`（Spring Modulith Level 2 以降） | コミット後・別トランザクション |

#### 業務監査を非同期にしない理由

- 監査書き込みがコミット後の別トランザクションになると、業務変更と監査の間に遅延窓が生じ、「業務は成功したが監査は FAILED として滞留中」という状態が運用上発生する。§15.1 が求める追跡可能性に対して弱い
- 監査は §3.3 において共通基盤が所有すると定めた横断機能である。業務モジュールのイベント購読者として実装するより、**Framework 所有の Audit API を直接呼ぶ方が所有権が明確になる**
- Event Publication Registry を経由すると、監査1件ごとに publication レコードの書き込みが加わり、コストが二重になる

#### 原則

**業務監査の書き込み失敗は、業務処理の失敗として扱う。**監査を残せない業務操作は成立させない。

### 15.3 監査対象と分類の割当

| 監査対象 | 分類 |
|---|---|
| Login 成功・失敗、Logout | セキュリティ監査 |
| Account Lock、認証試行の閾値超過 | セキュリティ監査 |
| Password 変更・Reset | 管理画面操作時は業務監査／本人によるリセット要求時はセキュリティ監査。**操作経路ごとに Phase 2 で確定する** |
| Role、Permission 変更 | 業務監査（管理業務トランザクション内で発生するため） |
| 管理者操作 | 業務監査 |
| 重要データの作成、更新、削除 | 業務監査 |
| 承認、却下、状態遷移 | 業務監査 |
| 機密情報への参照 | 業務監査 |
| File 出力、Download | 業務監査 |
| 外部連携実行 | 業務監査（実行記録）。実行そのものは副作用側 |
| 設定変更 | 業務監査 |
| 認可拒否 | セキュリティ監査 |

### 15.4 標準項目

- eventType
- actorId / actorType
- subjectId
- resourceType / resourceId
- action
- result
- reasonCode
- occurredAt
- requestId / traceId
- sessionId
- clientIp
- userAgent
- before / after の安全な要約

**Password、Token、秘密値、個人情報の全文を記録しない。**

---

## 16. データアーキテクチャ

### 16.1 対応方針

| 領域 | 方針 |
|---|---|
| 第一標準 DB | PostgreSQL / Aurora PostgreSQL |
| 設計適合 DB | Oracle（§16.8） |
| **更新系（既定）** | **Spring Data JPA** |
| **更新系（SQL 指向）** | **MyBatis**（モジュール単位で選択。§16.2） |
| **参照系（複雑クエリ・集計・帳票）** | **Spring JdbcClient**、または MyBatis モジュールでは MyBatis（§16.3） |
| Migration | **Flyway（所有者別に独立管理。§16.7.2）** |
| **Session Store** | **PostgreSQL（Spring Session JDBC。§14.3）** |
| **Cache** | **Caffeine（Spring Cache 抽象。§16.4）** |
| Test DB | Testcontainers による実 DB |

**H2 を本番 DB の代替として統合テストの中心には使用しない。**

> v0.1 は「CRUD／Aggregate = JPA」「SQL 指向 = JdbcClient」という技術軸で整理していたが、v0.2 では**更新系／参照系の軸へ組み替えた。**JdbcClient は参照専用と位置づけ（§16.3）、SQL 指向の更新系は MyBatis が担う（§16.2）。

### 16.2 永続化技術の選択

| 技術 | 用途 | 選択単位 |
|---|---|---|
| **JPA（Spring Data）** | 更新系の既定 | モジュール |
| **MyBatis** | SQL 指向を要する更新系、既存 SQL 資産の移行 | モジュール |
| **JdbcClient** | 参照専用（read model） | いずれのモジュールでも併用可 |

技術選択をプロジェクト全体で一方へ強制せず、**モジュール単位で明示する**（`@KoikiModule` の `persistence` 属性）。

#### モジュール内の混在禁止

**モジュール内で、更新系の永続化技術を1つに限定する**（JPA または MyBatis）。JdbcClient は参照専用として、いずれのモジュールでも併用してよい。

#### MyBatis の提供範囲

KOIKI は **BOM によるバージョン管理と規約・ArchUnit ルールを提供する**（Level B）。`koiki-starter-data-mybatis` は設けず、Reference Application にも Phase 4 まで含めない。

顧客プロジェクトは KOIKI BOM のバージョン管理下で `mybatis-spring-boot-starter` を直接依存に追加する。需要が実証された段階で、公式プロファイル化への昇格を §9.2 の基準に従い検討する。

MyBatis-Spring は MyBatis を Spring トランザクションへ参加させ、例外を Spring の `DataAccessException` へ変換するため、§12.4 の統一エラーハンドリングがそのまま適用できる。

#### 未フラッシュデータの読み取り禁止

**JPA は1次キャッシュを持ち、フラッシュを遅延させる。**したがって同一トランザクション内で JPA により書き込んだ後、JdbcClient または MyBatis で同じデータを読むと、**古い値が返る。**Spring の JPA 統合は JDBC 系のクエリ実行前に自動フラッシュを行わない。

> **同一トランザクション内で、JPA による書き込みの後に JdbcClient または MyBatis で同じデータを読まない。**
>
> 参照専用クエリは Query 系 Use Case（read-only トランザクション）で実行することを原則とする。Command 系 Use Case の内部で複雑クエリが必要になる場合は、書き込み前に実行するか、明示的にフラッシュする。

**この制約は MyBatis 固有ではない。**JdbcClient による read model（§16.3）を採用した時点で発生する。

### 16.3 read model

参照専用の結果型をread modelと呼び、Query契約とともに`application.query`に配置する。永続化の実装は`adapter.outbound.persistence`に置き、ApplicationがAdapterを参照する逆向き依存を作らない。

#### 生成方式の使い分け

| 条件 | 方式 |
|---|---|
| 1つの集約から導出できる単純な一覧・詳細 | **JPAのclass-based射影**（Java `record`） |
| 複数集約にまたがる検索、集計、帳票、既存 SQL 資産の移行 | **JdbcClient** |
| MyBatis を採用するモジュール | **MyBatis Mapper**（モジュール内の一貫性を優先し、JPA 射影を混在させない） |

#### 規約

- read modelは**Java `record`**として定義し、JPAのinterface-based射影は用いない
- Query Portは`application.query`が所有し、Outbound Adapterがその契約を実装してApplication所有のread modelをmaterializeする
- **Tier 1ではread modelという専用概念を設けない。**Tier 1にはDomain層が存在しないため、`application.dto`で足りる。read modelを所有する`application.query`はTier 2でのみ使用する
- 分離方式において、read model は `converter` を経由しない（既に最終形であるため）

#### Oracle 適合への影響

JdbcClient および MyBatis で SQL を記述する箇所が増えるほど、§16.8 の Oracle 適合リスクが上がる。**SQL 記述規約をコードレビュー項目として Phase 2 から適用する。**

### 16.4 キャッシュ

**Spring Cache抽象＋Caffeineを標準とする。**Redisは標準採用しない。Spring Cacheにより呼出側の契約は維持できるが、Redis等の分散cacheへの変更を設定だけの無条件な差し替えとは扱わない。シリアライズ、TTL、key互換性、障害時動作を別途検証する。

複数タスク構成ではインメモリキャッシュの内容がインスタンス間で食い違う。**したがって方針の本体はキャッシュ対象を限定する規約である。**

#### 規約

1. **キャッシュ対象は「更新頻度が低く、インスタンス間の一時的な不整合が許容できるデータ」に限定する** — マスタ、コード値、ロールと権限のマッピング定義など
2. **業務トランザクションデータをキャッシュしない**
3. **個別の認可判断結果をキャッシュしない。**「このユーザがこの操作を実行できるか」という判断結果はキャッシュしない。ロールと権限のマッピング定義も、権限剥奪の反映遅延をTTLまで許容できる場合に限りキャッシュしてよい。即時失効が必要なシステムでは認可関係のキャッシュを禁止する
4. **TTL を必須とする。**無期限キャッシュを禁止する
5. **キャッシュキーに名前空間プレフィックスを付ける**（例: `koiki:<領域>:<キー>`）

#### 無効化戦略に関する制約

**イベント駆動によるキャッシュ無効化は採用しない。**

Spring Modulith の Event Publication Registry および `@ApplicationModuleListener` は、**採用レベル Level 0〜2 では同一インスタンス内でのみイベントを配信する。**複数タスク構成において、あるインスタンスで発生したイベントが他インスタンスのキャッシュを無効化することはない。

**したがってキャッシュの鮮度は TTL のみで担保する。**この制約が、キャッシュ対象の限定（規約1）と TTL 必須（規約4）の根拠である。

> Level 3（externalization）で外部ブローカーを用いればインスタンス間配信は可能だが、§4.3 により初期は対象外である。

### 16.5 共通化する機能

- DataSource と Connection Pool
- Transaction Manager
- 例外変換
- Pagination
- Optimistic Lock
- 監査列
- SQL Log の安全な制御
- Migration
- DB Health Check
- Testcontainers
- **識別子採番の標準**（下記）

#### 識別子採番

兼用方式（§11.6）では DB 採番（sequence / identity）を利用できる。**分離方式では業務モデルが永続化前に完全な状態で成立する必要があるため、ドメイン側で採番する。**

PostgreSQL と Oracle の双方で成立する採番方式を標準として定める。方式の選定にあたっては、インデックスの局所性と既存 SQL 資産移行時の既存採番との共存を評価する。

### 16.6 避ける抽象化

- 巨大な BaseEntity
- あらゆる条件を受け取る汎用 Repository
- Reflection による自動 CRUD
- 業務 SQL を隠す独自 Query 言語
- Spring／JPA の契約を見えなくする独自 ORM
- **Domain 境界におけるリフレクションベースの自動マッピング**（`converter` は手書きとする。§11.7）

### 16.7 所有権と Migration 管理

#### 16.7.1 テーブル命名規約

KOIKI-PYFW の接頭辞規約を移植する。

| 接頭辞 | 所有者 |
|---|---|
| `koiki_` | KOIKI Framework |
| `kkref_` | Reference Application |
| `kkbiz_` | Customer Application |

**第三者が名前を決めるテーブル**（Spring Session、Spring Modulith の Event Publication、Flyway 履歴等）は、カスタマイズ可能なものは `koiki_` へ揃え、**不可能なものは「Framework 管理の例外テーブル一覧」として文書化する。**

目的は「DB を見たときに誰のテーブルか分かること」であり、一覧があれば達成できる。

**既存スキーマを変更できない移行案件のテーブルは、接頭辞規約の対象外とする。**

#### 16.7.2 Flyway の所有権分割

| 所有者 | locations | 履歴テーブル | 実行順序 |
|---|---|---|---|
| KOIKI Framework | `classpath:db/migration/koiki` | `koiki_flyway_history` | 1 |
| Reference Application | `classpath:db/migration/kkref` | `kkref_flyway_history` | 2 |
| Customer Application | `classpath:db/migration/customer` | `flyway_schema_history` | 3 |

**この方式を選ぶ理由** — 単一の履歴テーブルに混在させると、**KOIKI が後からマイグレーションを追加したときにバージョン順序が破綻する**（顧客の `V5` の後に KOIKI の `V2` を追加できない）。バージョン帯域の分割も同じ問題を抱える。履歴テーブルを分けることで、各所有者が独立してバージョンを進められる。

locationsも所有者ごとに相互に親子関係を持たない専用Directoryとする。Flywayはlocation配下を再帰走査するため、Customerを`classpath:db/migration`にすると`db/migration/koiki`まで取り込み、所有者間で同じversionを使用した際に重複migrationとなることをWalking Skeletonで実測した。

同一schemaで先行ownerがテーブルを作成すると、履歴テーブルをまだ持たない後続ownerはFlywayの非空schema検査で停止する。このため後続ownerは`baselineOnMigrate=true`、`baselineVersion=0`として履歴を初期化する。baseline versionを0とすることで、後続owner自身の`V1`以降を省略せず適用する。

Spring Boot は Flyway Bean を1つ自動構成するため、複数構成には追加の Bean 定義と実行順序の制御を要する。**KOIKI Starter がこの構成を提供する。**

#### 16.7.3 Framework 管理テーブル

| 区分 | テーブル |
|---|---|
| 認証・認可 | ユーザー、ロール、権限 |
| 監査 | 監査イベント |
| セッション | Spring Session のセッションテーブル |
| 認証試行制御 | ログイン試行記録 |
| イベント配信 | Event Publication（Level 2 以降） |

第三者が名前を決めるテーブルについては、カスタマイズ可否と最終的な名称を一覧として維持する。

#### 16.7.4 所有権を維持するための規約

- **顧客テーブルから KOIKI テーブルへの外部キー制約を作成しない。**参照は識別子の値で行う。外部キーを張ると KOIKI 側のスキーマ変更が顧客テーブルに制約され、年次更新（§8.4）が困難になる
- **KOIKI が提供するマイグレーションは、既存データを破壊しない形とする。**カラム追加は可、削除は Deprecation 期間の経過後とする（§8.5 と整合）
- Reference Application のテーブルを Customer Application が利用しない

### 16.8 Oracle 適合

初期リリースでは PostgreSQL を正式対象とするが、ID 採番、日時、Boolean、Paging、Schema、LOB、Lock、Flyway 等で **Oracle 移行を阻害しない設計**とする。

#### 検証戦略

**Phase 2からOracle TestcontainersによるnightlyスモークをCIへ追加する**（§21.5）。開始時にOracle edition／version、container image、JDBC Driverを明示的に固定する。

§5 の原則9「Production Parity」に対し、Oracle の CI を Phase 4 まで持たない構成では人的担保に依存することになる。**JdbcClient による read model（§16.3）と MyBatis の採用（§16.2）により SQL 記述量が増えたため、リスクは上昇している。**

Phase 2 時点では Framework 所有のテーブルのみが存在するため範囲は限定的だが、**最も価値が高い「Flyway の DDL 方言差の検出」はこの段階で行える。**

Phase 2のOracle Freeコンテナによる検証は、DDL、基本CRUD、ページング、楽観ロックに対する設計適合スモークであり、本番Oracleの正式サポートを意味しない。

#### マイグレーション方針

**Flyway の vendor 分岐を先行導入せず、PostgreSQL と Oracle の双方で通る共通 DDL でマイグレーションを記述する。**共通 DDL で通らないと判明した時点で、vendor 分岐の導入を判断する（判断はアーキテクチャオーナー）。

vendor 分岐を先行導入すると、§16.7.2 の所有者別2階層と組み合わさり **2階層 × 2ベンダー = 4系統**となる。設計を先行して複雑化させる代償が大きい。**共通 DDL で書けるならそれが最善であり、書けないと判明してから分岐する方が合理的である。**

#### SQL 記述規約

DDL と DML の双方をカバーする Oracle 互換 SQL 記述規約を定め、**Phase 2 からコードレビュー項目として適用する。**

- Boolean 型を使用しない（Oracle の対応状況が版により異なる）
- 識別子は小文字・引用符なしで記述する
- `VARCHAR` を用いる（`VARCHAR2` は Oracle 固有）
- 予約語を識別子に用いない
- シーケンスと IDENTITY の扱いを統一する
- 方言依存の関数・構文を用いない

**MyBatis は `databaseIdProvider` により、同一 Mapper 内で DB 種別ごとに SQL を切り替えられる。**JdbcClient にはない利点であり、Oracle 適合において有利に働く。

#### スキーマ分離を採らない理由

Oracle ではスキーマ＝ユーザであるため、スキーマによる所有権分離は権限管理を複雑にする。**接頭辞方式（§16.7.1）が単純であり、Oracle 適合にも適する。**

Oracle正式対応時（Phase 4）には、対象とする本番Oracleのedition／versionとJDBC Driverをbaselineに固定し、専用Integration TestとMigration差分を提供する。

---

## 17. トランザクションとモジュール間連携

### 17.1 境界

トランザクションは Application Use Case に置く。Controller や Repository の都合だけで分割しない。

### 17.2 原則

- Query は read-only を明示する
- **外部 API 呼出、メール送信、ファイル出力を業務トランザクションへ含めない**
- **コミット後の耐久的な非同期連携には、Spring Modulith Event Publication Registry を標準機構として用いる（Level 2 以降）。独自の Outbox テーブルおよびポーリング機構を実装しない**
- Retry 可能な処理と非 Retry 処理を区別する（§18.3）
- Optimistic Lock と業務競合を区別する（§12.5）
- 分散トランザクションを初期標準としない

### 17.3 モジュール間連携

**モジュール間の直接 Bean 呼出を禁止する。**他モジュールの `application` および `domain.model` の型を直接参照しない。連携は必ず Domain Event を経由する（§21.3 で検査）。

#### 同期を既定とする

| 方式 | 適用条件 | 実装 |
|---|---|---|
| **同期**（既定） | 受け手の処理完了が、送り手の業務的成立条件である場合 | 同期 `@EventListener` |
| **非同期**（明示選択） | 受け手の処理が、送り手の副作用・派生処理である場合 | `@ApplicationModuleListener`（Level 2 以降） |

#### 判断の実務ルール

**リスナー内で外部 I/O（外部 API 呼出、メール送信、ファイル出力、オブジェクトストレージ操作）を行う場合は、必ず非同期とする。**これらを同期リスナーに置くことは §17.2 の原則違反であり、大半のケースはこのルールで正しく振り分けられる。

#### 同期を既定とする理由

**誤りの深刻度が非対称である。**

- **誤って非同期にした場合** — 業務的成立条件が結果整合となり、データ不整合を生む。しかも FAILED として滞留するため発見が遅れる。**正しさの破壊である**
- **誤って同期にした場合** — トランザクションが長くなり、結合が強くなる。**重く遅くなるが、データは正しいままである**

**安全側は同期である**（§5 原則3 Fail Closed / Fail Fast）。

加えて、対象規模（§4.2）において結果整合の運用コスト（冪等性設計、FAILED 監視、再送手順、テストの複雑化）が得られる利点を上回ること、および Spring Modulith Level 1 の期間中は非同期を利用できないため同期を既定とすれば Level 移行時に規約が変化しないことも根拠となる。

#### 同期イベントも疎結合である

**「同期＝密結合」ではない。**イベント経由であれば、同期であっても送り手は受け手を知らない。非同期にして初めて得られるのは「トランザクション分離」と「障害分離」のみである。

| | 直接 Bean 呼出 | イベント経由（同期） | イベント経由（非同期） |
|---|---|---|---|
| 送り手が受け手を知るか | **知っている** | 知らない | 知らない |
| トランザクション | 同一 | 同一 | 別（コミット後） |
| 受け手の失敗が送り手へ波及 | する | する | しない |

#### モジュール境界の形骸化を防ぐガードレール

1. モジュール間の直接 Bean 呼出を ArchUnit で禁止する
2. **同期モジュール間リスナーの件数をモジュール単位で把握し、増加した場合はモジュール境界の妥当性を見直す。**同期依存の多いモジュール対は、分割位置が誤っている可能性が高い（§9.5 の確認対象）
3. 外部 I/O のルールを Review Checklist（§24.1）の必須確認項目とする

#### リスナーの配置

モジュール間イベントのリスナーは **`adapter/inbound/event`** に置く。同期・非同期の双方をここに置き、ArchUnit はアノテーションで判別する。

**`adapter/inbound/message`（外部メッセージング）と同居させない。**トランザクション境界、失敗時の扱い、テスト方法がいずれも異なるためである。

**リスナーは薄く保つ。**Application Use Case を呼び出すのみとし、業務ロジックを持たない。

### 17.4 永続化技術の混在に関する規約

異なる永続化技術のモジュール間で同期イベント連携を行う場合、**受け手が送り手側の未コミットデータを DB から読む設計にしない**（§16.2 の未フラッシュ問題）。必要なデータは Domain Event のペイロードに含める。

これは §11.8 規約2（イベントは識別子と値のみを持つ不変 `record`）と整合する。**識別子のみでは不足する場合、値を含める。**

### 17.5 Spring Modulith Level 1 期間の暫定規約

Level 2 到達までの期間、次を規約とする。

- **`@TransactionalEventListener` の直接使用を禁止する。**同期 `@EventListener` のみを許可する

**理由** — Level 2 で Event Publication Registry を有効化した時点で、既存の `@TransactionalEventListener` がすべて永続化対象となり、意図しない DB 書き込みと性能変化が発生する。Spring Modulith は `@TransactionalEventListener` と `@ApplicationModuleListener` の**両方**を追跡するため、一部だけを除外する段階移行はできない。

コミット後の副作用が必要になった時点で、Level 2 への移行を判断する（§6.4）。

---

## 18. 外部連携

### 18.1 対象

- REST
- SOAP
- File / SFTP
- Mail
- Object Storage
- Messaging
- 外部 Identity Provider
- 外部 Master

### 18.2 クライアント技術の標準

| 用途 | 標準 |
|---|---|
| 同期 HTTP 呼出 | **`RestClient`** |
| 宣言的クライアント | **HTTP Service Interface ＋ `@ImportHttpServices`** |
| JMS | `JmsClient` |
| **使用しない** | `RestTemplate`（新規コード）、`WebClient`（§4.3 でリアクティブを対象外とするため）、`HttpServiceProxyFactory`（`@ImportHttpServices` に置き換え済み） |

`RestTemplate` の新規使用禁止は ArchUnit で検査する。

```java
@HttpExchange
public interface InventoryClient {
    @GetExchange("/items/{id}")
    @Retryable(includes = { ... }, maxRetries = 2, delay = 100, multiplier = 2, maxDelay = 1000)
    @ConcurrencyLimit(10)
    InventoryItem findItem(@PathVariable String id);
}

@Configuration(proxyBeanMethods = false)
@ImportHttpServices(group = "inventory", types = InventoryClient.class)
class InventoryHttpClientConfiguration {
}
```

`maxRetries` は初回実行を含まない再試行回数である。上記例の最大試行回数は、初回1回と再試2回の合計3回となる。

外部 API がバージョニングされている場合は `ApiVersionInserter` を用いる（§12.2）。

### 18.3 共通ポリシー

| 項目 | 実現手段 |
|---|---|
| Connection／Read Timeout | `RestClient` / HTTP クライアントの設定。**KOIKI が既定値を提供する** |
| Retry | **Spring コアの `@Retryable`**（`includes`、`maxRetries`、`delay`、`jitter`、`multiplier`、`maxDelay`） |
| Bulkhead | **Spring コアの `@ConcurrencyLimit`** |
| Fallback | コアに専用機構がないため、例外ハンドリングで実装する |
| **Circuit Breaker** | **コアに含まれない。Phase 4 で Resilience4j の採用を評価する**（§8.7 の第三者ライブラリ採用基準を適用） |
| Error Translation | §12.4 |
| 構造化 Log、Metric／Trace | §20 |
| 秘密値 Masking | §14.1 |
| 疎通 Health の扱い | §20 |
| Fake／Stub による Integration Test | §21 |

#### `@EnableResilientMethods` の自動有効化

`@EnableResilientMethods` を付けない場合、**`@Retryable` は静かに無視される。**Fail Silent は §5 原則3 に反するため、**KOIKI Starter で自動有効化する**（§22.1）。

#### リトライ規約

§17.2 の「Retry 可能な処理と非 Retry 処理を区別する」を具体化する。

- **冪等でない操作をリトライしてはならない**
- `@Retryable` の `includes` で対象例外型を限定する。**無指定（全例外リトライ）を禁止する**
- 業務例外をリトライ対象に含めない。リトライ対象は一時的障害（接続エラー、タイムアウト、5xx）に限定する

### 18.4 配置

- 実装は `adapter.outbound.external` に配置する
- **Tier 2 では `domain.gateway` に Port、`adapter.outbound.external` に実装を置く**（実装差し替えの現実味がある場合）
- Tier 1 では `gateway` を設けず、Application Use Case が直接利用してよい
- **外部 API 呼出を業務トランザクションへ含めない**（§17.2）

接続先固有 DTO、業務判断、認証属性マッピングは顧客アプリケーションの Adapter へ置く。

---

## 19. バッチ、ファイル、スケジュール

### 19.1 バッチ

Spring Batch を公式統合対象とし、次を標準化する。

- Job／Step 命名
- Parameter
- Restart
- Skip／Retry
- 排他制御
- 実行履歴
- 監査
- Metric
- 大量データの Chunk 処理
- Web アプリケーションとの責務分離

### 19.2 スケジュールと単一実行基盤

**定期処理を Web インスタンス内で無条件に起動しない。**ECS Scheduled Task、Kubernetes CronJob、外部 Job Scheduler 等の**単一実行基盤**から起動する。

#### 単一実行基盤に依存する処理

| 処理 | 由来 | 必要となる Phase |
|---|---|---|
| **期限切れセッションの清掃** | §14.3 | **Phase 2** |
| 完了済み Event Publication のパージ | §6.4 Level 2 | Phase 4 |
| 業務バッチ | §19.1 | Phase 4 |

**単一実行基盤は Phase 1b の成果物とする。**セッション清掃ジョブ（Phase 2）が依存するため、それより先に整備する必要がある。

#### Event Publication のパージ

Level 2 到達後、完了した publication は既定でテーブルに保持され続ける。**定期パージを行わないとテーブルが際限なく肥大化し、新規レコードの作成・完了処理も遅くなる。**パージジョブは Phase 4 の必須成果物とする。

### 19.3 ファイル

- CSV／Excel の安全な読み書き
- Size 上限
- MIME と拡張子の検証
- File Name Sanitization
- 一時 File の Life Cycle
- Virus Scan 連携点
- Object Storage 連携
- Download 権限と監査

帳票製品は初期に固定せず、Adapter として統合する。

**ファイル出力は外部 I/O であるため、業務トランザクションへ含めず、モジュール間連携では非同期側に置く**（§17.3）。

## 20. Observability と運用

### 20.1 構造化ログとコンテキスト伝播

#### 標準項目

timestamp、level、service、environment、requestId、traceId、userId、operation、elapsed、result、errorCode を標準項目として定義する。

**秘密値、Token、Password、個人情報の不用意な出力を禁止する。**

#### コンテキスト伝播

**Micrometer Context Propagation（`io.micrometer:context-propagation`）を標準機構とする。**

§6.2 のとおり、Java 21 をターゲットとするため Scoped Values（Java 25 で正式化）を利用できない。Scoped Values は仮想スレッド環境における `ThreadLocal` の後継として設計されており、相関ID・SecurityContext の伝播に本来最も適するが、本書のベースラインでは選択できない。

Micrometer Context Propagation は `ThreadLocalAccessor` によるプラガブルな仕組みであり、Spring 自身もこの方式でコンテキスト伝播を扱う。**伝播対象を後から追加することが容易である**点も、この選択の利点である。

#### 実装方式

**Micrometer Context Propagation を適用する `TaskDecorator` を Bean として提供する。**

Spring Boot 4 は複数の `TaskDecorator` Bean を宣言した場合、`@Order` に従って自動的に連鎖させる。したがって顧客が独自の `TaskDecorator` を追加しても共存できる。

**伝播対象:** 相関ID（MDC）、SecurityContext

#### 非同期処理における注意

Spring Modulith Level 2 で `@ApplicationModuleListener` を使用すると、リスナーは別スレッド・別トランザクションで実行される。**この TaskDecorator が機能しないと、非同期処理のログを相関ID で追跡できなくなる。**Level 2 到達時（Phase 4）に、非同期リスナーでのコンテキスト伝播を検証項目に含める。

### 20.2 メトリクス

- HTTP 件数、応答時間、Status
- DB Pool
- External API 応答時間、失敗
- Authentication 成功・失敗
- Authorization 拒否
- Optimistic Lock 競合
- Batch 成功・失敗、処理件数
- JVM、GC、Memory、Thread

#### Event Publication に関するメトリクス（Level 2 以降）

- **Event Publication の状態別件数**（PUBLISHED / PROCESSING / COMPLETED / FAILED）
- **FAILED publication の滞留時間**
- Event Publication テーブルの行数

**FAILED publication の件数と滞留時間は、業務影響のある監視対象として扱う。**非同期連携の失敗が滞留したまま気付かれない状態を防ぐ。

### 20.3 トレース

OpenTelemetry を標準拡張点とし、HTTP、DB、外部 API、Messaging へ Trace Context を伝播する。

### 20.4 ヘルスチェック

- Liveness
- Readiness
- DB
- 必要に応じた外部依存

**すべての外部依存障害を Readiness 失敗へ直結させない。**アプリケーションが提供可能な機能と再起動効果を考慮して分類する。

### 20.5 Graceful Shutdown

SIGTERM 受信時に新規受付を停止し、処理中 Request を完了させ、Connection と Telemetry を Flush して終了する。

---

## 21. テスト戦略

### 21.1 Test Pyramid と契約試験

| テスト種別 | 対象と要件 |
|---|---|
| **Domain Unit Test** | Tier 2 の業務モデルの不変条件と状態遷移。**`EntityManager` を必要としない。**兼用方式であっても、`new` してビジネスルールを検証する単体テストは JPA を要さない |
| **Application Use Case Test** | Use Case の調整ロジック、トランザクション境界、権限呼出 |
| **Web Slice Test** | Controller と描画。**Thymeleaf 経路では、テンプレート描画の完了までを検証対象に含める。**ステータスコードとモデル属性の検証で終えると、描画時の遅延ロードとテンプレート内メソッド呼出を検出できない。`spring.jpa.open-in-view=false` の状態で実行する |
| **Repository Integration Test** | Testcontainers による実 DB。MyBatis モジュールでは `@MybatisTest` を用いる |
| **Security Integration Test** | §14.8 の項目 |
| **Module Integration Test** | Spring Modulith によるモジュール単位の検証 |
| **End-to-End Smoke Test** | Reference Application の主要フロー |
| **Architecture Test** | ArchUnit（§21.3） |
| **性能ベースライン計測** | §21.6 |

#### 失敗パスの必須検証

§5 原則11 に基づき、次を必ず検証する。

- 認証、認可、競合、タイムアウト、リトライ、失効
- **監査の分類ごとのロールバック挙動**（業務監査は業務 Tx と同時に消える／セキュリティ監査は残る。§15.2）
- **MyBatis 経路の楽観ロック競合検出。**JPA 経路と異なり自動制御が働かないため、**MyBatis モジュールでは必須項目とする**（§12.5）

#### テストフレームワーク

| 項目 | 標準 |
|---|---|
| テストフレームワーク | **JUnit 5**（Spring Boot 4 は JUnit 6 もサポート）。**JUnit 4 は使用しない** |
| REST API テスト | **`RestTestClient`**（Spring Framework 7 で新設された非リアクティブクライアント） |
| 使用しない | `WebTestClient`（リアクティブ前提のため） |

`MockMvc` と `RestTestClient` の使い分けは Phase 1b で規約化する。

### 21.2 Testcontainers

PostgreSQL 等の実 DB、必要な外部 Middleware を Testcontainers で起動し、本番との差異を縮小する（§5 原則9）。

**Oracle についても Phase 2 から Testcontainers による nightly スモークを実施する**（§21.5、§16.8）。

### 21.3 ArchUnit / Spring Modulith

#### エラーメッセージの規約

**ArchUnit の `because()` に、次の3要素を記述する。**

```text
〈規約の内容〉（ADR-XXX / §Y.Z）。〈違反した場合に何が起きるか〉。〈どう修正するか〉。
```

**記述例:**

```java
.because("業務モデルを Inbound Adapter の入出力型に使わない（ADR-023 / §11.6 規約4）。"
       + "兼用方式では遅延ロードがビュー描画時に発火し、"
       + "レスポンスの一部送信後に失敗するためエラーページも表示できなくなる。"
       + "Form オブジェクトまたは read model へ変換すること。")
```

**エラーメッセージ自体をドキュメントとして機能させる。**違反した開発者および AI コーディングエージェントが、規約の意図と修正方法へエラーメッセージから直接辿れることを要件とする。

**これは §24.2（Agent Skills に判断のみを書く）の前提である。**規約の詳細を Skill から外すには、違反時にどこかで意図を伝える必要がある。

#### 規則一覧

##### 全 Tier 共通

| # | 規則 |
|---|---|
| 1 | `adapter.inbound` は `adapter.outbound` を参照しない |
| 2 | `application` は `adapter.inbound` を参照しない |
| 3 | 他モジュールの非公開パッケージを参照しない |
| 4 | モジュール間に循環依存がない |
| 5 | Framework が Reference / Customer へ依存しない |
| 6 | Controller が Repository を直接呼ばない |
| 7 | すべての業務モジュールが `@KoikiModule` で Tier を宣言している |
| 8 | すべての業務モジュールが `persistence` を宣言している |
| 9 | 他モジュールの `application` および `domain.model` の型を直接参照しない |
| 10 | `domain.event` は他モジュールから参照してよい（**明示例外**） |
| 11 | Domain Event 型は `record` であり、`domain.model` の型をフィールドに持たない |
| 12 | `RestTemplate` を新規コードで使用しない |
| 13 | Framework 外から `org.koikifw.<module>.internal.**` を参照しない |

##### Tier 1 固有

| # | 規則 |
|---|---|
| 14 | Tier 1 は `domain.model`、`domain.service`、`domain.repository`、`domain.gateway` を持たない（**`domain.event` は Tier に依らず許可**） |

##### Tier 2 固有（兼用方式・既定）

| # | 規則 |
|---|---|
| 15 | `domain` は `adapter`、Spring Web / MVC、`EntityManager` を参照しない（`jakarta.persistence` のアノテーションおよび Spring Data Commons への依存は許容） |
| 16 | `domain.repository` は `Repository<T, ID>` を継承する（`JpaRepository` の継承を禁止） |
| 17 | `adapter.inbound` のメソッドが `domain.model` の型を戻り値・引数に用いない |
| 18 | MVC ハンドラメソッドの**引数**に `domain.model` の型が現れない |
| 19 | `Model` への `addAttribute` および `ModelAndView` へ渡す値に `domain.model` が含まれない |
| 20 | MVC ハンドラメソッドの**戻り値**に `domain.model` が現れない |
| 21 | `domain.model` の型が他モジュールから参照されない |
| 22 | `domain.model` に public setter が存在しない |
| 23 | `application.query`は同packageが所有するread modelを参照してよい（**明示例外**） |
| 24 | `domain.gateway` の実装は `adapter.outbound.external` にのみ存在する |

##### Tier 2（分離オプトイン時に追加）

| # | 規則 |
|---|---|
| 25 | `domain` は `jakarta.persistence` を参照しない |
| 26 | `adapter.outbound.persistence.entity` が `domain` および `application` から参照されない |
| 27 | `domain.repository` の実装は `adapter.outbound.persistence` にのみ存在する |
| 35 | `mapper`（MyBatis Mapper）のメソッドシグネチャに `domain.model` の型が現れない |
| 36 | `jpa`（Spring Data Repository）のメソッドシグネチャに `domain.model` の型が現れない |
| 37 | read modelは`converter`を経由せずSQL実行手段から直接生成される |

##### Spring Modulith 採用レベル別

| # | 規則 | 適用期間 |
|---|---|---|
| 28 | `@TransactionalEventListener` を宣言したメソッドが存在しない | **Level 1 期間中** |
| 29 | 同期 `@EventListener` から `adapter.outbound.external` / `file` / `messaging` への依存が存在しない | Level 2 以降 |

##### MyBatis 採用時

| # | 規則 |
|---|---|
| 30 | `persistence = MYBATIS` かつ `persistenceModel = SHARED` の組み合わせが存在しない |
| 31 | `persistence = MYBATIS` のモジュールに `@Entity` を付与したクラスが存在しない |
| 32 | `persistence = JPA` のモジュールに `@Mapper` を付与したインターフェースが存在しない |
| 33 | MyBatis Mapper は `adapter.outbound.persistence` にのみ存在する |
| 34 | Tier 2 において `domain` から MyBatis Mapper が参照されない |

##### イベントリスナー

| # | 規則 |
|---|---|
| 38 | `@EventListener` および `@ApplicationModuleListener` を宣言したメソッドは、`adapter.inbound.event` パッケージにのみ存在する |
| 39 | `adapter.inbound.event` は `domain.model` および `domain.repository` を直接参照しない（Application Use Case を経由する） |

#### 配布

**上記39件は `koiki-archunit-rules` として配布し、Project Template のテストへ組み込む。**

これを行わない場合、**本書が定める機械的担保は KOIKI リポジトリ内でのみ機能し、顧客アプリケーションでは一切強制されない**（§9.6）。

### 21.4 Quality Gate

- Compile
- Unit／Integration Test
- Formatting／Static Analysis
- **NullAway による null 安全性検査**（違反でビルド失敗）
- **japicmp による Public API 互換性検査**（破壊的変更でビルド失敗。§8.5）
- **ArchUnit**（§21.3）
- Dependency Vulnerability Scan
- Secret Scan
- SBOM 生成
- Container Scan
- Migration Validation
- Reference Application Smoke Test

#### NullAway の導入時期

**Phase 1a の立ち上げ時点から適用する。**既存コードへ後から適用すると大量のビルド失敗が発生するため、**後から導入しない。**

### 21.5 CI テストマトリクス

| 系統 | DB | Java | 実行タイミング | 導入 Phase |
|---|---|---|---|---|
| **主系統** | PostgreSQL | 21 | 全 PR・全マージ | Phase 1a |
| **互換系統1** | PostgreSQL | **25 ランタイム** | nightly、リリース前 | Phase 1a |
| **互換系統2** | **Oracle** | 21 | nightly | Phase 2 |
| **互換系統3** | PostgreSQL | 21（**Virtual Threads 有効**） | nightly | Phase 4 |

**互換系統1では再コンパイルを行わない。**主系統で生成した成果物を Java 25 ランタイムで実行する。これにより、実際の顧客環境（Java 21 でビルドされた KOIKI を Java 25 で動かす）と同じ条件を検証する。

**互換系統の失敗はリリースブロッカーとして扱う。**

### 21.6 性能ベースライン計測

§25.3 は Framework のオーバーヘッドを計測することを要求する。**計測の仕組みをテスト戦略の一部として位置づける。**

- Framework の主要経路（認証、認可、監査、例外処理、ログ出力）のオーバーヘッドを計測する
- 計測結果を記録し、**リリース間の推移を追える形にする**
- 計測ハーネスを Phase 1b で整備し、以降の Phase で継続的に計測する

**ベースラインの仕組みを持たない場合、オーバーヘッドの増加は本番稼働まで検出されない。**

---

## 22. Auto Configuration と拡張点

### 22.1 自動構成してよいもの

- 共通 Exception Handler
- request ID Filter
- Logging Context
- **JSON 標準設定（`JsonMapperBuilderCustomizer`）**
- Security Header
- Actuator 基本設定
- Audit Event Publisher
- 設定値 Validation
- 共通 Metric
- **`@EnableResilientMethods` の有効化**
- **Micrometer Context Propagation の `TaskDecorator` 登録**
- **JPA の Open Session in View 無効化（`spring.jpa.open-in-view=false`）**

#### JSON 標準設定

Spring Boot 4 は形式別マッパー（JSON 用 `JsonMapper`、XML 用 `XmlMapper`）を自動構成するため、**`ObjectMapper` Bean を定義しても自動構成された Bean を置き換えられない。**設定フックは `JsonMapperBuilderCustomizer` を用いる。

**`spring.jackson.find-and-add-modules=false` とし、KOIKI が明示的にモジュールを登録する。**Spring Boot 4 はクラスパス上の全モジュールを検出して自動登録するが、これは §22.3 の方針と衝突する。**依存ライブラリが Jackson モジュールを推移的に持ち込むと、JSON 出力が黙って変わる。**

#### `@EnableResilientMethods` の自動有効化

`@EnableResilientMethods` を付けない場合、**`@Retryable` は静かに無視される。**Fail Silent は §5 原則3 に反するため自動有効化する。

#### Open Session in View の無効化

`spring.jpa.open-in-view` は Spring Boot の既定で有効であり、起動時に警告が出力される。KOIKI は明示的に無効化する。

**理由:**

1. OSIV 有効時は EntityManager がリクエスト全体にわたり開かれ、DB コネクションの長時間占有によりプールが枯渇する
2. 遅延ロードがビュー描画時にトランザクション外で発火し、N+1 が静かに発生する
3. **最も重要な点として、OSIV 無効化は §11.6 規約4（業務モデルをビュー層へ渡さない）をビュー層で強制する唯一の実効的な手段である。**有効のままでは違反コードが「動いてしまう」ため検出できない

§5 原則2（Secure by Default）および原則3（Fail Fast）の適用例である。

案件が上書きすることは可能とするが、**既定を有効へ戻した場合に §11.6 規約4 の違反が検出されなくなる**点を文書化する。

### 22.2 明示設定すべきもの

- URL／Method ごとの認可
- IdP 固有設定
- User 属性 Mapping
- 業務 Role／Permission
- CORS Origin
- 外部接続先
- DB Schema／方言
- 監査保持期間
- **`spring.threads.virtual.enabled`**（既定 false。有効化は案件の明示的判断とする。§23.3）

### 22.3 拡張契約

`@ConditionalOnMissingBean` 等を用いて上書き可能にするが、**Default Bean が何を行うかを文書化する。**

#### 自動構成の判断基準

**自動構成してよいのは、開発者が明示的にアノテーションや設定を記述して初めて効果が現れるものである。依存を追加しただけで挙動が変わるものは自動構成しない。**

この基準により、§22.1 の判断が説明できる。

| 対象 | 判断 | 根拠 |
|---|---|---|
| `@EnableResilientMethods` | **自動有効化する** | 開発者が `@Retryable` を書いて初めて効果が現れる。書かなければ何も起こらない |
| Jackson モジュールの自動検出 | **自動構成しない** | 依存を追加しただけで JSON 出力が変わる |

**単に依存を追加しただけで、予期しない Endpoint や Schedule が起動しないようにする。**

---

## 23. 実行・デプロイアーキテクチャ

### 23.1 第一参照環境

```text
Client
  -> ALB
    -> ECS Fargate
      -> Spring Boot Container
        -> Aurora PostgreSQL / PostgreSQL
        -> CloudWatch / OpenTelemetry Backend
```

セッションは Spring Session JDBC により PostgreSQL へ格納するため、**複数タスク構成でセッションが共有される**（§14.3）。

定期処理は Web インスタンスではなく、ECS Scheduled Task 等の**単一実行基盤**から起動する（§19.2）。

### 23.2 コンテナ標準

JVM をコンテナで運用する際、既定値のままでは性能を発揮できず、また障害の原因が特定しにくい。**本節では設計上の考慮点を明示する。**具体的な設定値は案件ごとのメモリ・CPU 割当により変わるため、KOIKI は既定値ではなく**設計指針と確認項目**を提供する（§23.2.7）。

#### 23.2.1 イメージ構成

- **Multi-stage Build**
- **レイヤ分割** — Executable JAR をそのまま COPY すると Docker のレイヤキャッシュが効かず、アプリケーションの1行変更でも依存ライブラリ全体が再送される。`-Djarmode=tools ... extract` により dependencies / spring-boot-loader / snapshot-dependencies / application へ展開し、変更頻度の低い層を下位に置く
- **展開レイアウトで起動する** — ネストアーカイブからの実行にはオーバーヘッドがあり、展開レイアウトの方が起動が速い。CDS / AOT キャッシュの前提でもある（§23.2.5）
- **JRE ベースイメージを用いる**（実行に JDK は不要）
- 必要最小限の Base Image
- **非 root User**
- **Read-only Root Filesystem への適合** — 書き込みが必要な一時領域（`java.io.tmpdir`、ヒープダンプ出力先）を明示的にマウントする

#### 23.2.2 メモリ設計

**コンテナのメモリ制限はヒープだけを対象としない。**次のすべてが同一の cgroup 制限に対して計上される。

```text
コンテナメモリ = ヒープ + Metaspace + Code Cache + スレッドスタック
               + Direct Buffer + GC のオーバーヘッド + JVM 内部構造
```

**規約**

1. **`-Xmx` ではなく `-XX:MaxRAMPercentage` を用いる。**コンテナのメモリ割当を変更した際に追従できる
2. **必ず明示的に設定する。**コンテナ対応 JVM の既定は保守的であり、512MB を超えるコンテナでもメモリ制限の 25% 程度しかヒープに使わない
3. **ヒープ以外の領域に 25〜30% 程度の余裕を残す。**Direct Buffer を多用する場合はさらに広く取る
4. **`-XX:MaxMetaspaceSize` に上限を設ける。**Spring アプリケーションはクラス数が多く、Metaspace が動的に伸びる
5. **小容量コンテナでは割合指定が機能しにくい。**1GB 以下の割当では、ヒープが小さくなりすぎて GC の挙動が想定と異なる場合がある。この規模では絶対値指定を検討する
6. **コンテナのメモリ制限を最大ヒープサイズと等しく設定しない**

**OOMKilled の検知**

**コンテナがメモリ制限を超えた場合、カーネルがプロセスを強制終了する。これは JVM のエラーではないため、アプリケーションログには何も残らない。**終了コードのみが手掛かりとなる。

- `-XX:+HeapDumpOnOutOfMemoryError` と `-XX:+ExitOnOutOfMemoryError` を設定する
- ヒープダンプの出力先を永続化する（Read-only Root Filesystem との整合に注意）
- **JVM の OutOfMemoryError と、カーネルによる OOMKilled を区別して監視する**
- 内訳の把握には Native Memory Tracking を用いる
- **JDK またはカーネルの更新後は、cgroup 検出が正しく機能しているかを確認する。**検出に失敗するとホストの全メモリを基準にヒープが確保され、確実に OOMKilled となる

#### 23.2.3 CPU 設計とスロットリング

**コンテナの CPU 制限は「速度の上限」ではなく「一定時間内に使用できる CPU 時間のクォータ」である。**クォータを使い切ると、次の期間までコンテナ全体が停止する。

**この停止はアプリケーションスレッド、JIT コンパイラスレッド、GC スレッドを同時に凍結させる。**その結果、GC の停止時間が伸び、応答時間のテールが悪化する。

**規約**

1. **`-XX:ActiveProcessorCount` でプロセッサ数を明示する。**JVM の自動検出は、割当が vCPU の整数倍でない場合や cgroup の設定によっては実際の割当と一致しない。ECS Fargate においても明示が推奨される
2. **GC スレッド数と JIT コンパイラスレッド数を CPU 割当に合わせる。**プロセッサ数を過大に検出するとスレッドが過剰生成され、限られたクォータを奪い合う
3. **1 vCPU 未満の割当を避ける。**JIT と GC がクォータを消費するため、業務処理に回る時間が著しく減る
4. **少数の大きいインスタンスを優先する。**総 CPU 量が同じ場合、少数の大きいタスクの方が多数の小さいタスクよりスロットリングが少なく、スループットとテールレイテンシの双方で有利になる傾向がある
5. **CPU スロットリングをメトリクスとして監視する**（§20.2）

#### 23.2.4 GC の選択

**ヒープサイズと CPU 割当により適切な GC が変わる。既定値に委ねない。**

| 条件 | 選択 |
|---|---|
| 一般的な業務アプリケーション | **G1GC** |
| 小容量ヒープ（概ね 512MB 未満） | Serial GC または Parallel GC。G1 のリージョン管理のオーバーヘッドが相対的に大きくなるため |
| バッチ・ETL 等のスループット重視 | Parallel GC |
| 大容量ヒープかつ CPU が潤沢 | ZGC を検討 |

**GC ログを常時出力する。**本番障害の解析において、GC ログの有無が原因特定の速度を決める。

#### 23.2.5 起動時間の最適化

スケールアウトやローリングデプロイの頻度が高い場合、起動時間が可用性に直結する。

| 手段 | 適用条件 | 効果 |
|---|---|---|
| **展開レイアウト**（§23.2.1） | 常時 | ネストアーカイブのオーバーヘッド解消 |
| **CDS（Class Data Sharing）** | **Java 21 で利用可能** | 起動時間およびメモリ使用量の削減 |
| **AOT キャッシュ** | Java 24 以降。Spring の AOT キャッシュ連携は Java 25 ＋ Spring Boot 4 が前提 | CDS を上回る削減 |

**KOIKI における方針**

- **ターゲットバイトコードが Java 21 であるため、CDS を基本線とする**（§6.2）
- **AOT キャッシュは推奨実行環境（Java 25）を利用する案件における任意最適化とする。**Java 21 ランタイムの案件では利用できない
- CDS / AOT のいずれも、**イメージビルド時に訓練実行を行い、生成物をイメージへ含める。**AOT キャッシュは専用の最上位レイヤへ置き、下位レイヤのキャッシュを活かす
- **CDS および AOT キャッシュはプロセスメモリのダンプではないため、秘密値が成果物へ漏出するリスクがない。**この点で、プロセス状態を保存する方式とは性質が異なる

#### 23.2.6 運用上の設定

- **Actuator Health**（§20.4）
- **Graceful Shutdown** — SIGTERM 受信時の挙動（§20.5）
- **標準出力 Log** — ファイル出力せず、コンテナランタイムへ委ねる
- **Secret 外部注入** — イメージへ焼き込まない
- **タイムゾーンとロケールを明示する** — ベースイメージの既定に依存させない
- **`JAVA_TOOL_OPTIONS` を JVM オプションの受け渡し手段とする** — 起動方法によらず JVM が読み取る標準的な環境変数である

#### 23.2.7 KOIKI の提供範囲

**具体的な設定値は、案件ごとのメモリ・CPU 割当とワークロード特性により変わる。**KOIKI は既定値を強制せず、次を提供する。

| 提供物 | 内容 |
|---|---|
| **設計指針** | 本節の規約 |
| **確認項目** | メモリ内訳の算定、CPU 割当とスロットリング、GC 選択、起動時間、OOMKilled 検知の各確認項目 |
| **参照 Dockerfile** | Reference Application のコンテナ定義（Phase 4） |
| **DB Pool と Task 数の設計指針** | コネクションプールのサイジングは CPU 割当およびタスク数と連動する。仮想スレッドを有効化する場合は特に重要となる（§23.3） |

**性能ベースライン計測（§21.6）は、これらの設定を含めた状態で行う。**JVM 設定を変更した際の影響が推移として追える状態を維持する。

### 23.3 Virtual Threads

**既定で無効とする**（`spring.threads.virtual.enabled=false`）。有効化は opt-in とする。

#### 根拠

§6.2のとおり最低実行環境をJava 21と定めた。**Java 21では`synchronized`ブロック内のブロッキング操作でキャリアスレッドがpinされ、負荷時にスレッド枯渇やデッドロックを招きうる。**Java 24でJEP 491が`synchronized`に起因するpinningのほぼすべてを解消したが、native codeやclass initialization等に起因する残存ケースがある。また、最低実行環境がJava 21である以上、既定有効化は安全ではない。

#### 有効化時のチェックリスト

1. **ランタイムが推奨実行環境（Java 25）以上であること**
2. コンテキスト伝播が機能すること（Micrometer Context Propagation の `TaskDecorator`。§20.1）
3. **DB コネクションプールのサイジング見直し** — 仮想スレッドは無制限に増えるため、プールがボトルネックとなり待機が積み上がる
4. **`@ConcurrencyLimit` による流量制限の設定** — 従来のスレッドプールが暗黙に果たしていた流量制限が失われるため
5. native code、class initialization、JDBCドライバ等のブロッキングを含む依存ライブラリの棚卸しと負荷計測

Phase 4 で検証し、Phase 5 で正式ガイドを提供する。CI にも有効時の検証系統を設ける（§21.5）。

### 23.4 Kubernetes-ready

初期から Stateless、External Configuration、Health Probe、Graceful Shutdown、Horizontal Scale、Web／Batch 分離を満たす。Helm、HPA、NetworkPolicy、PDB 等の EKS 運用資材は後続リリースで提供する。

### 23.5 WAR

Executable JAR および Container を標準とし、外部 Tomcat への WAR 配置は原則標準サポート外とする。

顧客制約で必要となる場合は互換プロファイルとして個別評価する。**その際、Spring Boot 4 は Jakarta EE 11 / Servlet 6.1 互換コンテナを要求する**点に留意する。Servlet 5 系コンテナでは動作しない。

---

## 24. 開発者体験と AI 駆動開発

### 24.1 開発支援

- Project Template（**Thymeleaf＋HTMX 版／API＋SPA 版の2種類**）
- **Feature Template（Tier 1 用／Tier 2 用の2種類）**
- CRUD Sample
- Security Sample
- MVC／Thymeleaf Sample
- External API Sample
- Batch Sample
- Migration Guide
- Review Checklist
- ADR Template

**空のディレクトリを大量生成せず、必要になった責務を追加する。**実際に使用しないサブパッケージは作成しない。

#### Review Checklist の必須確認項目

本書が定める規約のうち、**機械検査できないもの**を Review Checklist の必須項目とする。

| 確認項目 | 参照 |
|---|---|
| Tier 昇格トリガの該当有無 | §11.5 |
| 同期／非同期の判断（リスナー内の外部 I/O の有無） | §17.3 |
| **MyBatis 経路の楽観ロック実装**（更新件数チェックの記述） | §12.5 |
| **未フラッシュデータの読み取り**（同一 Tx 内で JPA 書き込み後の JdbcClient / MyBatis 読み取り） | §16.2 |
| Oracle 互換 SQL 記述規約への適合 | §16.8 |
| Framework へ入れるか否かの判断 | §9.2、§9.3 |
| キャッシュ対象の妥当性 | §16.4 |
| 監査の分類 | §15.2 |
| `converter` 以外からの `reconstitute` 呼出がないこと | §11.7 |

### 24.2 Agent Guidance

KOIKI は AI コーディングエージェント向けの Skill を提供する。

#### 設計方針

**機械検査で担保される規約を Skill に記述しない。Skill には機械検査できない判断のみを書き、詳細は ADR 番号への参照とする。**

| **書かない**（機械検査で担保される） | **書く**（機械検査できない判断） |
|---|---|
| ArchUnit 39件の規則内容 | **Tier の選択**（§11.5 の4トリガ） |
| NullAway が検出する null 安全性違反 | **永続化技術の選択**（JPA / MyBatis。§16.2） |
| japicmp が検出する破壊的変更 | **分離オプトインの判断**（§11.7 の4トリガ） |
| OSIV 無効化により失敗する Entity のビュー渡し | **同期／非同期の判断**（§17.3） |
| コンパイルエラーとなるもの | **read model の方式**（§16.3） |
| | **監査の分類**（§15.2） |
| | **キャッシュ対象の判断**（§16.4） |
| | **Framework へ入れるか否か**（§9.2、§9.3） |

**この方針は §21.3 の `because()` 規約とセットで成立する。**規約の詳細を Skill から外すには、違反時に ArchUnit のエラーメッセージが意図を伝える必要がある。

#### Skill 構成

```text
docs/agent/skills/
├── koiki-project-overview
├── koiki-business-feature-work
├── koiki-framework-work
├── koiki-testing-work
└── koiki-troubleshooting
```

| Skill | 内容 |
|---|---|
| **`koiki-project-overview`** | 全体構造、Framework / Reference / Customer の所有権、モジュール構成とディレクトリ規約、現在の Spring Modulith 採用 Level、**どの Skill をいつ使うか** |
| **`koiki-business-feature-work`** | 業務モジュールの新規作成・変更。上表の判断すべて。禁止事項の要点（業務モデルをビューへ渡さない、`@TransactionalEventListener` を使わない） |
| **`koiki-framework-work`** | Framework への変更。昇格チェックリスト、**「Framework に入れないもの」リスト**、Public API 境界、Deprecation ポリシー |
| **`koiki-testing-work`** | テストピラミッドと各層の責務、Testcontainers、スライステスト、**失敗パスのテスト**、楽観ロック競合テストの必須化 |
| **`koiki-troubleshooting`** | **ArchUnit / NullAway / japicmp の違反時の原因と修正**、`LazyInitializationException`（OSIV 無効化に起因）、`JacksonException`（Jackson 3 の非検査例外化）、Flyway の適用順序エラー |

#### Skill の更新規律

- **新しい ADR を追加しても、判断基準が増えない限り Skill を更新しない**
- 判断基準が増えた場合のみ、該当 Skill に判断フローを追加する
- 四半期アーキテクチャレビューで Skill の妥当性を確認する（§9.5）

**この委譲構造により、ADR が増えても Skill は肥大化しない。**

AGENTS.md、CLAUDE.md、Copilot Instructions 等は共通 Skills への薄い導線とする。

---

## 25. 非機能品質目標

### 25.1 セキュリティ

OWASP ASVS 等を参照し、認証、セッション、アクセス制御、入力、暗号、ログ、通信の基準を品質ゲートへ落とす。具体的適合レベルは基本設計で定義する。

### 25.2 可用性

- **複数インスタンスで安全に実行できる**
- **Web プロセス内 Singleton 処理へ依存しない**（定期処理は単一実行基盤から起動する。§19.2）
- **セッションを外部化する**（Spring Session JDBC。§14.3）
- Graceful Shutdown に対応する
- Health Check を用途別に分離する

### 25.3 性能

- API、DB、外部連携の計測を既定化する
- **N+1、Slow Query、Pool 枯渇を検出可能にする**
- 性能目標値は案件ごとに定義する
- **Framework のオーバーヘッドを計測し、リリース間の推移を追う**（§21.6）

#### 性能に影響する設計判断

本書の決定のうち、性能に直接影響するものを記録する。

| 決定 | 影響 |
|---|---|
| OSIV 無効化（§22.1） | 描画時の N+1 とコネクション長時間占有を防ぐ |
| read model の採用（§16.3） | 参照系で業務モデルを materialize しない |
| Spring Session JDBC（§14.3） | リクエストごとの DB 書き込みが発生する。保存モードのチューニングを要する |
| Event Publication Registry（Level 2） | イベント1件×リスナー数の INSERT が業務トランザクションに乗る |
| キャッシュ（§16.4） | TTL のみで鮮度を担保するため、更新頻度の低いデータに限定する |

### 25.4 保守性

- **モジュール境界を自動検査する**（§21.3）
- **Public API と内部実装を区別する**（§9.6）
- **Deprecation と Migration Guide を提供する**（§8.5、§8.6）
- **設計文書と Reference Application を同期する**（§26）

### 25.5 監査性

- Security Event と Business Event を追跡可能とする（§15.2）
- Actor、Resource、Result、Request を相関可能とする
- 保存期間と閲覧権限を明示する

## 26. Reference Application

### 26.1 位置づけと役割

Reference Application はデモではなく、次の役割を担う（§3.6）。

- 設計契約の正しい利用例
- 統合テスト対象
- リリース時のスモークテスト
- AI エージェントの参照実装

**ただし Reference Application がすべての設計判断を実証するわけではない。**実証する範囲を §26.4、実証しない範囲を §26.5 に明示する。実証されない決定は規約文書と Agent Skills が担う。

### 26.2 業務題材

**業務題材を「経費申請・承認」とする。**

#### 選定理由

| 理由 | 内容 |
|---|---|
| **認可の実例として最適** | 申請者／承認者／経理という役割分離、自己承認の禁止、部門による可視範囲の制限。**§3.4「UI に依存しないバックエンド認可」を、画面制御では代替できない形で実証できる** |
| **監査の3分類が対比できる** | 業務監査（申請・承認・却下＝業務トランザクションと同一）とセキュリティ監査（ログイン失敗＝独立トランザクション）を、同一アプリケーション内で示せる |
| **楽観ロックの競合が自然に起きる** | 同じ申請を複数の承認者が開く状況は現実的であり、§12.5 の競合制御と §13.2 の競合画面を実演できる |
| **Tier 1／Tier 2 の対比が自然** | マスタ（部門・経費科目）と業務トランザクション（申請）が明確に分かれる |
| **業種非依存** | どの案件にもマッピングでき、特定業界の知識を要求しない |
| **ドメインが重すぎない** | 在庫・価格計算のような複雑さがなく、**Reference が業務ロジックの説明に埋没しない** |

### 26.3 モジュール構成と Phase 別追加計画

**単一アプリケーションとし、Phase の進行に合わせて業務モジュールを追加する。**Reference Application 自身がモジュラーモノリスを体現する。

```text
koiki-reference-app
├── identity        (Tier 1, JPA)              … Phase 2
├── master          (Tier 1, JPA)              … Phase 3
├── expense         (Tier 2, JPA 兼用)          … Phase 3
├── notification    (Tier 1, JPA)              … Phase 4
└── accounting      (Tier 2, MyBatis, 分離)     … Phase 4
```

**モジュールごとに Tier・永続化技術・UI が異なる構成は、実際の案件でも起こりうる姿である。**「全部入り」の不自然さを避けつつ、各 Phase で導入する設計判断をそのモジュールが実証する。

#### Phase 2 — `identity`（Tier 1, JPA）

| 項目 | 内容 |
|---|---|
| 責務 | ユーザー、ロール、権限の管理画面 |
| テーブル | **Framework 所有**（`koiki_` 接頭辞）。Reference は画面のみを持つ |
| 実証する内容 | **Framework 所有のテーブルを Reference の画面が操作する構図**（§3.1 の所有権分離）／セキュリティ監査（`REQUIRES_NEW`）／認証試行制御／セッションストア |

#### Phase 3 — `master`（Tier 1, JPA, Thymeleaf＋HTMX）

| 項目 | 内容 |
|---|---|
| 責務 | 部門マスタ、経費科目マスタ |
| テーブル | `kkref_department`、`kkref_expense_category` |
| 画面 | 一覧（検索・ページング・部分更新は HTMX）、登録、更新、廃止 |
| 実証する内容 | Tier 1 の構造／**read model は JPA の射影**（単一集約）／HTMX 契約／楽観ロック／業務監査／キャッシュ（コード値） |

#### Phase 3 — `expense`（Tier 2, JPA 兼用, Thymeleaf＋HTMX）

| 項目 | 内容 |
|---|---|
| 責務 | 経費申請・承認 |
| テーブル | `kkref_expense_request`、`kkref_expense_line` |
| 状態遷移 | `DRAFT` → `SUBMITTED` → `APPROVED` → `SETTLED`。`SUBMITTED` → `REJECTED`、`SUBMITTED` → `RETURNED` → `DRAFT`を分岐として許可する |
| 不変条件 | 明細合計と申請額の一致を新規作成・Draft編集・提出時に保証する（**複数エンティティにまたがる不変条件**）／編集可能なのは`DRAFT`のみ／自己承認の禁止 |
| 業務モデル | `ExpenseRequest`（JPA Entity 兼用）、`ExpenseLine`、`Money`（Value Object、`@Embeddable`）、`ExpenseStatus` |
| 業務メソッド | `submit()`、`approve(approver)`、`reject(approver, reason)`、差戻し、再編集開始、`settle()`。差戻しと再編集開始は別の状態遷移として実装する |
| 実証する内容 | **Tier 2 昇格トリガの該当例**／兼用方式／`domain.repository` を Spring Data が実装／**read model は JdbcClient**（承認待ち一覧に申請者名・部門名を含むため複数集約にまたがる）／楽観ロック競合画面／業務監査 |

**Reference Application内で JPA の射影と JdbcClient の両方が実演される。**§16.3 の使い分け基準を対比として示せる。

#### Phase 3 — `expense` の最小 REST API

MVC と同じ Application Use Case と業務認可を呼び出す最小 REST API を追加し、
`/api/v1` のパスセグメント方式による API Versioning と Jackson 3 の契約を実証する。
React SPA とブラウザ向け認証・CSRFの併用構成はPhase 4で追加する。

#### Phase 3 — `master` ⇔ `expense` の同期イベント連携

> **部門マスタで部門を廃止する際、当該部門に未処理の経費申請が残っていれば廃止を拒否する。**

未処理申請は非終端状態の`DRAFT`、`SUBMITTED`、`RETURNED`、`APPROVED`とする。
`REJECTED`と`SETTLED`は部門廃止を妨げない。

```text
master.domain.event.DepartmentDeactivating   （公開パッケージ）
        ↓ 同期 @EventListener（同一トランザクション）
expense.adapter.inbound.event   未処理申請を検査 → 存在すれば例外
        ↓
部門廃止のトランザクションごとロールバック
```

**§17.3 の判定基準「受け手の処理完了が、送り手の業務的成立条件である場合は同期」に完全に合致する。**

加えて次を同時に実証する。

- `master` は `expense` を知らない（依存方向の維持）
- モジュール間の直接 Bean 呼出を行わない
- `domain.event` が他モジュールから参照可能な明示例外である
- Domain Event が識別子と値のみを持つ不変 `record` である
- リスナーが `adapter/inbound/event` に配置され、Application Use Case を呼ぶのみである

#### Phase 4 — `notification`（Tier 1, JPA）

| 項目 | 内容 |
|---|---|
| 責務 | 承認結果のメール通知 |
| テーブル | `kkref_notification_log` |
| 連携 | `ExpenseApproved` / `ExpenseRejected` を **`@ApplicationModuleListener`** で非同期受信 |
| 実証する内容 | **非同期の判断ルール**（リスナー内で外部 I/O を行うため非同期）／Spring Modulith **Level 2**／at-least-once に対応する**冪等性の実装例**／FAILED publication の監視と再送／パージジョブ／非同期リスナーでのコンテキスト伝播 |

#### Phase 4 — `accounting`（Tier 2, MyBatis, 分離）

| 項目 | 内容 |
|---|---|
| 責務 | 精算済み申請から仕訳データを生成し、会計システムへ連携する |
| 連携 | `ExpenseSettled`を非同期受信し、申請ごとに仕訳を冪等に生成する |
| テーブル | **既存スキーマを模擬**し、接頭辞なしのテーブル名とする。§16.7.1 の「既存スキーマは接頭辞規約の対象外」という現実を示す |
| 実証する内容 | **モデル分離オプトイン**（トリガ1: 永続化スキーマを変更できない）／**MyBatis 分離方式の構造**（`entity` / `converter` / `mapper`）／**楽観ロックの手動実装**／**`databaseIdProvider` による PostgreSQL・Oracle 切り替え**／`domain.gateway` とHTTP Service Clientによる外部連携／`@Retryable` / `@ConcurrencyLimit` |

#### Phase 4 — `expense` の React SPAとMVC / API併用構成

Phase 3のREST APIとPhase 2の認証基盤を利用してSPA最小参照実装（React）を追加する。
**Thymeleaf と SPA の併用**、ブラウザ経路ごとの認証・CSRF設定、およびKOIKI-PYFWの
SPA認証契約の移植を実証する。

#### Phase 4 — Spring Batch

- 未処理申請のリマインド（`notification` へイベント発行）
- 月次締め（通常操作と同じ精算Application Use Caseを使った`SETTLED`への一括遷移）

### 26.4 設計判断と実証箇所の対応

**各 ADR がどこで実証されるかを一覧化する。**Reference Application が §3.6 の役割を果たしていることを確認するための対応表である。

| ADR | 実証箇所 | Phase |
|---|---|---|
| ADR-005 Modulith 採用レベル | Level 0（Phase 1a・1b）／Level 1（`master`⇔`expense` 同期）／Level 2（`notification`） | 1a・3・4 |
| ADR-006・026 UI プロファイル | `master`・`expense`（Thymeleaf＋HTMX）／`expense` REST ＋ SPA | 3・4 |
| ADR-020 セッションストア | 全体 | 2 |
| ADR-022 Tier 制 | `master`（Tier 1）／`expense`（Tier 2） | 3 |
| ADR-023 兼用方式 | `expense` | 3 |
| ADR-023 分離オプトイン | `accounting`（MyBatis 分離） | 4 |
| ADR-024 Repository 方針 | `expense`（Spring Data 実装）／`accounting`（手書き Adapter） | 3・4 |
| ADR-025 同期イベント | **`master` ⇔ `expense`** | **3** |
| ADR-025 非同期イベント | `notification` | 4 |
| ADR-025 監査3分類 | `identity`（セキュリティ監査）／`expense`（業務監査） | 2・3 |
| ADR-027 HTMX | `master` 一覧（検索・ページング・部分更新） | 3 |
| ADR-028 OSIV 無効化 | 全体 | 1b |
| ADR-030 Jackson 3 | `expense` REST API | 3 |
| ADR-031 API Versioning | `expense` REST API | 3 |
| ADR-032 Resilience | `accounting` 外部連携 | 4 |
| ADR-033 HTTP Client | `accounting`（HTTP Service Interface ＋ `@ImportHttpServices`） | 4 |
| ADR-034 JSpecify | 全体 | 1a |
| ADR-036 認証試行制御 | `identity` | 2 |
| ADR-037 キャッシュ | `master`（コード値） | 3 |
| ADR-038 read model | `master`（JPA 射影）／`expense`（JdbcClient） | 3 |
| ADR-039 MyBatis | `accounting` | 4 |
| ADR-041 Public API 境界 | Framework 側 | 1a |
| ADR-042 テーブル所有権・Flyway | 全モジュール（`koiki_` / `kkref_` / 既存スキーマ模擬） | 1b〜4 |
| ADR-044 Oracle 検証 | Framework テーブル（Phase 2）／`accounting`（Phase 4） | 2・4 |

**ADR を追加した際は、本表に実証箇所を記載する。**記載できない場合は §26.5 へ記録する。

### 26.5 Reference Application で実証しない決定

| 決定 | 理由 | 代替手段 |
|---|---|---|
| **ADR-023 の JPA 分離方式** | `accounting` を MyBatis 分離としたため実例がない。分離トリガ1（既存スキーマを変更できない）に該当するケースは MyBatis との親和性が高く、JPA 分離は稀と判断した | §11.7 の構造図と規約で示す。採用する案件が生じた場合、Reference への追加を検討する |
| **Tier 1 ＋ MyBatis** | Reference では Tier 1 をすべて JPA とした | 規約文書で構造を示す |
| **Tier 1 → Tier 2 の昇格** | 実際に昇格させる例を作ると Reference の構成が不自然になる | 昇格トリガの判定例を Agent Skills と Review Checklist に記載する |
| **ADR-035 Virtual Threads** | 既定で無効のため、通常構成では動作しない | Phase 4 に VT 有効時の CI 検証系統を設ける（§21.5） |
| **ADR-017 サポート方針、ADR-040 昇格ポリシー** | プロセスであり、コードでは実証できない | 四半期アーキテクチャレビューの運用で担保する（§9.5） |
| **ADR-019 マルチテナンシー** | 対象外のため実証不要 | — |

---

## 27. 段階的ロードマップと完了条件

### 27.1 完了条件（DoD）の考え方

**成果物リストを DoD としない。**成果物は各 Phase に列挙されており、DoD がそれを繰り返しても判定の役に立たない。

| 悪い例 | 良い例 |
|---|---|
| Maven Multi-module を作成した | **Feature Template から新規モジュールを生成し、`mvn verify` を通過させられる** |
| ArchUnit を導入した | **意図的に規約違反を作ると CI が失敗する** |
| Audit Event を実装した | **業務トランザクションをロールバックすると業務監査も消え、認証失敗の監査は残る** |

**「できる」で書けない項目は、DoD ではなく成果物である。**

### 27.2 全 Phase 共通の完了条件

Phase 0 を除く全 Phase に適用する。

1. **OSS サポート中の最新 Spring Boot マイナーへ追従済みである**（§8.1）
2. **当該 Phase で行った設計判断が ADR として記録され、アーキテクチャオーナーが承認している**
3. **CI の品質ゲートがすべて通過している**（§21.4）
4. **当該 Phase で新設した規約が Agent Skills に反映され、Skill を用いた実装で規約違反が発生しない**
5. **当該 Phase で追加したテーブルが所有権規約に従い、Flyway の適切な階層に配置されている**（§16.7）

### 27.3 Phase 0: Architecture Baseline

**目的:** 設計判断を確定し、**実装可能性を検証し**、計画の土台を作る。

**成果物**

- 本グランドデザイン v0.2
- [用語集](../../standards/KOIKI-JavaWeb-FW_Glossary_v0.1.md)（KOIKI-PYFW ⇔ Java概念対応表、`converter` / `mapper` / `entity` / `reconstitute` / read model）
- **Phase 0で有効な全ADR**（「確定」「Phase 0で検証」の区分付き）
- Module Dependency 図／Security 基本方針／Initial Scope・Non-scope／Repository 構成
- **Walking Skeleton**（捨てる前提。設定は Phase 1a へ持ち込む）
- Reference Application の業務仕様（経費申請の状態遷移図、不変条件、権限マトリクス）
- **全 Phase の DoD と規模見積もり**
- Architecture Ownerの任命、一人project中の代理・継続性方針、四半期reviewの標準agenda
- KOIKI ⇔ Spring Boot ⇔ Java ベースライン対応表の公開場所と更新手順

**完了条件**

| # | 内容 |
|---|---|
| 0-1 | Phase 0で有効な全ADRが記述され、Architecture Ownerが承認している。各ADRに「確定」「Phase 0で検証」の区分が付いている |
| 0-2 | 用語集が完成している |
| 0-3 | **Walking Skeleton が完了し、検証項目すべてに回答が出ている** |
| 0-4 | **Walking Skeleton の結果を受けた規約の調整が完了している。実装不能と判明した規約が残っていない** |
| 0-5 | **全 Phase の DoD と規模見積もりが存在し、Phase 1a〜5 の実現可能性が判断されている** |
| 0-6 | Reference Application の業務仕様が確定している |
| 0-7 | Architecture Ownerが任命され、一人project中の代理・継続性方針と四半期reviewの標準agendaが文書化されている |
| 0-8 | KOIKI ⇔ Spring Boot ⇔ Java ベースライン対応表の公開場所と更新手順が定まっている |

**0-3 と 0-4 が本 Phase の核心である。**設計判断の確定だけでは Phase 0 を完了としない。**規約が実装可能であることを実測で確認する。**

#### Walking Skeleton

**目的は「動くものを作ること」ではなく、「規約が実装可能か、開発体験が実務に耐えるか」の検証である。**

| 原則 | 内容 |
|---|---|
| 捨てる前提で作る | 作り込むと、検証で問題が判明しても捨てにくくなる。業務ロジックは最小限に留める |
| コードは捨て、設定は残す | pom 設定、CI 定義、ArchUnit ルール実装、検証記録を Phase 1a へ持ち込む |
| 期間の上限を設ける | 超える場合は検証項目を削る。延長しない |
| 失敗も成果である | 「この規約は実装できない」という結論は、Phase 1 で判明するより桁違いに安価である |

**検証項目**

| # | 問い | 不合格時に見直す決定 |
|---|---|---|
| V1 | ArchUnit で意図した規約が記述できるか。誤検出はどの程度か | 記述できないルールを Review Checklist へ格下げ。§21.3 の39件を見直す。**§24.2（Skill に判断のみを書く）の前提が崩れるため、Skill 設計も再検討する** |
| V2 | Flyway の所有者別2階層が Spring Boot 自動構成と共存するか | **§16.7.2 を見直す。**顧客の年次更新（§8.4）に直結するため、代替案なしに先へ進まない |
| V3 | `package-info.java` への `@NullMarked` と `@KoikiModule` の併記に問題がないか | アノテーションの配置場所を分離する |
| V4 | Tier 2 のクラス数と記述量が実務に耐えるか | Tier 2 の構造を簡素化する（`domain.service` の任意化等） |
| V5 | OSIV 無効化により Entity 露出が確実に検出されるか | ArchUnit のみに依存する形へ後退し、§22.1 の根拠を書き換える |
| V6 | 同期イベント連携が素直にテストできるか | **§17.3 の「同期を既定とする」判断を再評価する** |
| V7 | `koiki-archunit-rules` を外部ビルド単位から利用できるか | 配布方式を見直す。**§9.6 の単一障害点が現実化するため、代替手段を確定させるまで Phase 1a を完了としない** |

**V2 と V7 は早期に着手する。**不合格時の影響が最も大きく、代替案の検討に時間を要する。

### 27.4 Phase 1a: Build Foundation（内部マイルストーン）

**目的:** 以降のすべての実装が乗る品質基盤。**外部リリースを伴わない。**

**成果物**

Maven Multi-module 構成／Parent・BOM／コンパイラ設定（`<release>21</release>`、Toolchains、Enforcer）／CI テストマトリクス／ArchUnit 基盤と Tier 別規則セット／**`koiki-archunit-rules` アーティファクト**／NullAway／japicmp とベースライン／Feature Template（Tier 1 用・Tier 2 用）／`@KoikiModule` アノテーション／Spring Modulith Level 0／CI Quality Gate

**完了条件**

| # | 内容 |
|---|---|
| 1a-1 | Feature Template から Tier 1 モジュールと Tier 2 モジュールを生成し、`mvn verify` を通過させられる |
| 1a-2 | 意図的に規約違反を作ると CI が失敗する（Tier 宣言の欠落／`domain.model` の Controller 露出／`internal` の外部参照／`@TransactionalEventListener` の使用／モジュール間の直接 Bean 呼出）。**かつ、エラーメッセージから ADR 番号・影響・修正方法が読み取れる** |
| 1a-3 | **`koiki-archunit-rules` を外部プロジェクトから依存として利用でき、そこでも規約違反が検出される** |
| 1a-4 | NullAway による null 安全性検査が動作し、違反でビルドが失敗する |
| 1a-5 | japicmp が動作し、Public API の破壊的変更でビルドが失敗する |
| 1a-6 | 成果物が Java 21 でビルドされ、Java 21 と Java 25 の両ランタイムで起動する |

**1a-2 と 1a-3 が最重要である。**規約が「書かれている」だけでなく「破ると落ちる」状態、**かつ顧客側でも落ちる**状態を確認する。

### 27.5 Phase 1b: Runtime Foundation（v0.1）

**成果物**

Core Configuration／例外・Problem Details（`JacksonException` を含む）／Validation／構造化ログ・コンテキスト伝播（TaskDecorator）／Actuator／PostgreSQL・**Flyway 2階層**／Testcontainers／Jackson 3 標準設定／Resilience（`@EnableResilientMethods` 自動有効化、リトライ規約、タイムアウト既定値）／API Versioning 設定／OSIV 無効化／**単一実行基盤**／MyBatis の BOM 管理／Domain Event の規約策定／**性能ベースライン計測ハーネス**

**完了条件**

| # | 内容 |
|---|---|
| 1b-1 | Flyway が KOIKI 階層と顧客階層で独立して適用され、双方のバージョンが干渉しない |
| 1b-2 | 未処理例外が統一エラー形式で返る。`JacksonException` を含む |
| 1b-3 | 構造化ログに相関ID が載り、`@Async` 境界を越えて伝播する |
| 1b-4 | Testcontainers による PostgreSQL 統合テストが CI で動作する |
| 1b-5 | Actuator のヘルスチェックが応答し、DB 接続状態を反映する |
| 1b-6 | JPA の Open Session in View が無効であり、ビュー層への Entity 露出がテストで検出される |
| 1b-7 | **定期処理が Web インスタンスではなく単一実行基盤から起動し、複数インスタンス構成でも二重起動しない** |
| 1b-8 | Framework のオーバーヘッドが計測され、ベースラインが記録されている |

### 27.6 Phase 2: Security Foundation（v0.2）

**成果物**

Spring Security 標準構成／Local User・Role・Permission／**HTTP Session（Spring Session JDBC）**／Password・Lock・Reset／**認証試行制御**／CSRF・Cookie・Security Header／**Audit Event の3分類実装**／OIDC Login／OAuth 2.0 Resource Server／Security Integration Test／**`identity` モジュール**／**Oracle 互換 SQL 記述規約**／第三者管理テーブルの例外一覧／OpenRewrite レシピの試作開始

**前提** — 2-8（セッション清掃ジョブ）は Phase 1b の単一実行基盤に依存する。

**完了条件**

| # | 内容 |
|---|---|
| 2-1 | 未認証で保護リソースへアクセスすると拒否される（URL ベースと Method Security の双方） |
| 2-2 | **画面上の制御を回避して直接リクエストを送っても、ロール・権限に基づく認可が強制される** |
| 2-3 | OIDC でログインでき、ローカル認証と併存する |
| 2-4 | Bearer JWT による API 認証が動作する |
| 2-5 | **2インスタンス構成でセッションが共有され、一方を停止してもセッションが維持される** |
| 2-6 | **ログイン失敗が閾値でアカウントロックを発生させ、その記録が業務トランザクションのロールバックに巻き込まれない** |
| 2-7 | **業務トランザクションをロールバックすると、業務監査も同時に消える** |
| 2-8 | 期限切れセッションの清掃ジョブが、単一実行基盤から起動する |
| 2-9 | CSRF とセキュリティヘッダーが既定で有効であり、無効化には明示的な設定を要する |
| 2-10 | `identity` モジュールから Framework 所有テーブルを操作できる |
| 2-11 | Oracle 互換 SQL 記述規約がレビュー項目として適用されている |
| 2-12 | **Oracle に対する nightly スモークが CI で動作し、Flyway マイグレーションの適用、基本 CRUD、ページング、楽観ロックが確認される** |

**2-6 と 2-7 の対比が本 Phase の核心である。**監査の3分類（§15.2）が設計どおり機能することを、ロールバック挙動として実演する。

### 27.7 Phase 3: Reference Vertical Slice（v0.3）

**成果物**

`master`（Tier 1）／`expense`（Tier 2）／**両者間の同期イベント連携**／Spring Modulith **Level 1**／Thymeleaf ＋ HTMX／**HTMX 契約11項目の標準化**／REST API（API Versioning、Jackson 3）／read model（JPA 射影／JdbcClient）／楽観ロック競合画面／業務監査／キャッシュ規約と適用例／SPA 契約の文書化／E2E スモークテスト／Agent Skills

**Phase 3 の末尾で実施** — MyBatis 実装規約の整備（楽観ロック、`converter`、`@MybatisTest`、`reconstitute` 誤用防止、ArchUnit 規則35〜37）。Phase 4 の `accounting` モジュールの前提となる

**完了条件**

| # | 内容 |
|---|---|
| 3-1 | `master`（Tier 1）と `expense`（Tier 2）が動作し、**それぞれの Tier を選択した理由が文書化されている** |
| 3-2 | **部門廃止時に未処理の経費申請が存在すると、廃止が拒否され部門マスタの更新がロールバックされる** |
| 3-3 | **`master` が `expense` を知らない状態で 3-2 が成立している** |
| 3-4 | **Thymeleaf テンプレートへ Entity を渡すコードを書くと、CI で検出される** |
| 3-5 | HTMX による一覧の検索・ページング・部分更新が動作し、**CSRF トークンが自動注入される** |
| 3-6 | **同一の経費申請を2つのセッションで更新すると、後発が競合画面へ遷移する** |
| 3-7 | **read model が `master` で JPA 射影、`expense` で JdbcClient として実装され、両方式の使い分け基準が実例として示されている** |
| 3-8 | 経費申請の申請・承認・却下が業務監査として記録される |
| 3-9 | 経費科目マスタがキャッシュされ、TTL 経過後に再読み込みされる |
| 3-10 | REST API が `/api/v1` のパスセグメント方式でバージョニングされている |
| 3-11 | E2E スモークテストが CI で通る |

**3-2 と 3-3 の組み合わせが本 Phase の核心である。**モジュール間連携が、依存方向を保ったまま業務要件として機能することを実演する。

### 27.8 Phase 4: Enterprise Integration（v0.4）

**成果物**

`notification`（非同期、Level 2）／`accounting`（MyBatis 分離）／Phase 3の`expense` REST APIを利用する **SPA 最小参照実装**とMVC / SPA併用構成／**Oracle Integration Baseline**／SAML Extension／External API Resilience／Spring Batch／File・Object Storage／OpenTelemetry／**Container・ECS Reference**／**Virtual Threads 有効化ガイドと CI 検証系統**

**完了条件**

| # | 内容 |
|---|---|
| 4-1 | **承認時にメール通知が非同期で送られ、送信に失敗しても承認はロールバックしない** |
| 4-2 | **アプリケーションを強制終了しても、未処理イベントが再起動後に配信される** |
| 4-3 | **同一イベントが2回配信されても、通知が二重送信されない** |
| 4-4 | FAILED publication の件数と滞留時間がメトリクスに現れ、再送手順が実行できる |
| 4-5 | 完了済み publication のパージジョブが単一実行基盤から起動する |
| 4-6 | **`accounting` が PostgreSQL と Oracle の双方で動作する** |
| 4-7 | **Oracle に対する統合テストが CI で通る** |
| 4-8 | **MyBatis 分離方式で楽観ロック競合が検出され、JPA 経路と同一のエラー応答が返る** |
| 4-9 | 外部 API の障害時にリトライと流量制限が働き、タイムアウトが既定値で機能する |
| 4-10 | **SPA から Cookie セッション認証で API を利用でき、CSRF double-submit が機能する** |
| 4-11 | **Thymeleaf 経路と SPA 経路が同一アプリケーション内で併存し、CSRF 設定が経路ごとに分離されている** |
| 4-12 | Spring Batch のジョブが単一実行基盤から起動し、二重起動しない |
| 4-13 | **Virtual Threads を有効にした CI 検証系統が通る** |
| 4-14 | 非同期リスナーにおいて相関ID が伝播し、ログが追跡可能である |

**4-2 と 4-3 が本 Phase の核心である。**耐久配信と冪等性は、非同期を採用する以上、動作を確認しないまま本番へ出せない。

### 27.9 Phase 5: Production Baseline（v1.0）

**成果物**

Starter 安定化／Reference Application 完成／**Project Template 2種類**／Security・Performance Review／SBOM・Vulnerability 運用／Upgrade・Migration Guide／EKS Reference Deployment／Agent Skills 整備／**対応表の公開とサポートウィンドウの日付明示**／**`koiki-migration-recipes` の正式提供**／商用延長サポート利用時の構成検証／標準保守契約テンプレートへの年次更新条項の反映

**完了条件**

| # | 内容 |
|---|---|
| 5-1 | **Project Template から新規プロジェクトを生成し、動作する画面と API が短時間で得られる**（2種類） |
| 5-2 | **Project Template で生成したプロジェクトにおいて、規約違反が CI で検出される** |
| 5-3 | **KOIKI ⇔ Spring Boot ⇔ Java ベースライン対応表が公開され、各バージョンのサポート終了日が日付で明示されている** |
| 5-4 | **前バージョンのプロジェクトに OpenRewrite レシピを適用して移行でき、移行後にテストが通る** |
| 5-5 | SBOM がリリース成果物として生成され、脆弱性検査が CI で動作する |
| 5-6 | **Framework のオーバーヘッドが計測され、Phase 1b からの推移が記録されている** |
| 5-7 | **Agent Skills を用いて新規業務機能を実装した際、規約違反が発生しない** |
| 5-8 | Security Review が完了し、指摘事項が解消または受容判断されている |
| 5-9 | Upgrade / Migration Guide が整備されている（OSIV 無効化、Jackson 3 の挙動変化を含む） |
| 5-10 | 商用延長サポート利用時の BOM・リポジトリ構成が検証されている |
| 5-11 | ECS Fargate の参照デプロイが動作し、EKS 参照デプロイも提供されている |

**5-2 と 5-4 が本 Phase の核心である。**顧客側で規約が機能すること、そして顧客が次バージョンへ移行できることが、v1.0 の実質的な要件である。

### 27.10 DoD の運用

- 各 Phase の終了時に**実演会**を開催する
- **アーキテクチャオーナーが判定する**（§9.4）
- **実演できない項目は未達とする。**「実装済みだが動かしていない」を完了としない
- **DoD を満たさない Phase は完了としない。**次 Phase へ進まない
- 外部要因により達成できない項目は、**アーキテクチャオーナーが明示的に例外承認し、ADR または課題として記録する**
- DoD を緩和する場合、承認と理由の記録を要する
- **DoD は規模見積もりの単位とする。**各 DoD 項目を「実演できる状態にするための工数」として積み上げる

---

## 28. 主要リスクと対策

### 28.1 アーキテクチャ

| リスク | 内容 | 対策 |
|---|---|---|
| 独自 FW 肥大化 | Spring 標準を再実装する | Spring 標準優先（§5 原則1）、昇格チェックリスト（§9.2） |
| DDD 過剰 | 単純 CRUD まで複雑化する | **Tier 制と昇格トリガ**（§11.1〜§11.5） |
| Tier 1 の肥大化 | 昇格すべきモジュールが昇格しない | 昇格トリガを Review Checklist の必須項目とし、四半期レビューで見直す |
| モジュール境界の形骸化 | 同期既定により実質的な密結合が進む | 直接 Bean 呼出の禁止、同期リスナー件数の監視（§17.3） |
| Reference と Framework の混同 | サンプル業務が共通化される | 所有権ポリシーと昇格手順（§9） |
| 案件 Fork | 更新不能なコピーが増える | 別リポジトリ＋Maven 依存、正式拡張点（§7.3） |
| Reference の「全部入り」化 | 実証したい決定が増えるたびに現実から乖離する | モジュール単位で技術を分ける。**実証しない決定を許容する**（§26.5） |

### 28.2 データアクセス

| リスク | 内容 | 対策 |
|---|---|---|
| DB 抽象化の失敗 | PostgreSQL と Oracle の差分を隠す | 方言差分の明示、**Oracle nightly（Phase 2 から）**、SQL 記述規約（§16.8） |
| **兼用モデルのビュー層流出** | Spring MVC ＋ JPA では Entity を `Model` に載せる書き方が最も自然であり、規約だけでは守られない | OSIV 無効化、ArchUnit、描画まで含む Web Slice Test の3層（§13.3.3） |
| **フォームバインドによるマスアサインメント** | `@ModelAttribute` で業務モデルへ直接バインドすると不変条件を迂回できる | Form オブジェクトの必須化と ArchUnit（§13.3） |
| **テンプレートからの業務メソッド呼出** | 兼用方式固有。SpEL から状態遷移を起動できる | ビューモデル変換の徹底。Review Checklist と Agent Skills |
| **MyBatis 経路の楽観ロック実装漏れ** | 更新件数チェックを忘れてもコードは動作する | Review Checklist の必須項目化、競合検出テストの必須化（§12.5） |
| **未フラッシュデータの読み取り** | JPA 書き込み後に JdbcClient / MyBatis で読むと古い値が返る | §16.2 の規約、Review Checklist |
| モジュール内の技術混在 | JPA と MyBatis が同一モジュールに混在する | ArchUnit（§21.3） |
| 共通 DDL 制約によるスキーマ設計の歪み | 両 DB の共通部分に制約される | **通らないと判明した時点で vendor 分岐へ移行する**（§16.8） |
| 復元時の不変条件スキップ | DB 上の不正データが業務処理に流れる | 不変条件は状態変更時に守る。データ品質は DB 制約と移行時の検証で担保する |
| `reconstitute` の誤用 | 業務的生成に復元用ファクトリが使われる | 命名規約、`converter` 以外からの呼出禁止（§11.7） |
| キャッシュのインスタンス間不整合 | 更新が他インスタンスへ伝わらない | 対象の限定と TTL 必須。**イベント駆動の無効化は採用しない**（§16.4） |
| 認可判断結果のキャッシュ | 権限を剥奪しても旧権限で操作できる | 個別の認可判断結果をキャッシュしない（§16.4 規約3） |
| セッションテーブルの DB 負荷 | リクエストごとの書き込みがボトルネックとなる | 保存モードのチューニング、Phase 4 での負荷計測 |

### 28.3 UI

| リスク | 内容 | 対策 |
|---|---|---|
| Thymeleaf 成果物の不採用 | UI コンポーネント群を作っても案件で使われない | **HTMX 同梱により実用性を確保**（§13.4）。Phase 3 の Reference で検証 |
| 第三者ライブラリの追従遅延 | `htmx-spring-boot` が Spring Boot 新版へ追従しない | 依存範囲の限定、代替実装コストの事前見積（§13.4） |
| Thymeleaf の型安全性欠如 | テンプレートの誤りが実行時まで検出されない | Reference による網羅、スライステスト、Review Checklist |
| UI プロファイル間の設定衝突 | Thymeleaf と SPA 併用時に CSRF・セッション設定が競合 | Phase 4 で併用構成を検証（§13.6） |

### 28.4 イベントと非同期

| リスク | 内容 | 対策 |
|---|---|---|
| Level 2 移行時の挙動変化 | Registry 有効化により既存の `@TransactionalEventListener` がすべて永続化対象となる | Level 1 期間中の使用禁止規約と ArchUnit（§17.5） |
| 結果整合の誤用 | 業務的成立条件を非同期にしてデータ不整合を生む | 同期を既定とし、外部 I/O の有無による判断ルール（§17.3） |
| Event Publication テーブルの肥大化 | 完了済みレコードの蓄積で性能劣化 | パージジョブを Phase 4 の必須成果物とする（§19.2） |
| イベントのスキーマ破壊 | クラス名・フィールド変更で再送が失敗 | 後方互換ポリシー（§11.8 規約5） |

### 28.5 Java と実行環境

| リスク | 内容 | 対策 |
|---|---|---|
| Java 21 での仮想スレッド pinning | `synchronized` を含むライブラリでスレッド枯渇が発生する | 既定で無効。有効化条件として Java 25 以上を推奨（§23.3） |
| Scoped Values 不使用による伝播の複雑化 | `ThreadLocal` ベースの伝播はスレッド境界で失われやすい | Micrometer Context Propagation を標準機構とする（§20.1） |
| **Jackson 3 の静かな挙動変化** | 日時形式（timestamp → ISO）と例外型（検査 → 非検査）の変更は、コンパイルエラーにならない | KOIKI が既定に依存せず明示設定。`JacksonException` を §12.4 でマッピング |
| モジュール明示登録の漏れ | `find-and-add-modules=false` により必要なモジュールが登録されない | Reference で必要モジュールを網羅。統合テストで検証 |
| リトライ対象の誤設定 | 冪等でない操作をリトライし二重処理が発生する | `includes` による例外型限定を必須化（§18.3） |
| Circuit Breaker 不在による障害波及 | 外部システム障害時にリトライが積み重なる | Timeout ＋ `@ConcurrencyLimit` で一次抑制。Phase 4 で Resilience4j を評価 |
| **コンテナ設定の不備** | OOMKilled、CPU スロットリングが原因不明の性能劣化を招く | §23.2 の設計指針と確認項目。OOMKilled の監視（§23.2.2） |

### 28.6 リリースとサポート

| リスク | 内容 | 対策 |
|---|---|---|
| **v1.0 が EOL 済み Spring Boot でリリースされる** | 特定バージョンへ固定したまま開発が長期化する | Phase 完了ごとの追従（§8.1）。各 Phase の完了条件へ組み込む |
| 顧客が年次更新に応じない | 更新されない案件がセキュリティパッチを受けられない | 契約時の明示、商用延長サポートの提示（§8.4） |
| Spring Boot 次期メジャーへの移行 | KOIKI メジャー2つ分に相当する作業量となりうる | 移行計画を独立した Phase として計上する |
| 商用リポジトリ利用時の構成差異 | 商用リリースは Maven Central へ公開されない | Phase 5 で検証（§8.3） |
| OpenRewrite レシピの品質不足 | レシピが不完全で移行がかえって困難になる | CI でのレシピ適用テストをリリース条件とする（§8.6） |
| 第三者ライブラリの追従遅延によるリリース停止 | プロファイル1つのために本体リリースが止まる | プロファイル単位で切り離し、旧ラインへ据え置く（§8.7） |

### 28.7 ガバナンスと規約強制

| リスク | 内容 | 対策 |
|---|---|---|
| **顧客側で ArchUnit ルールが実行されない** | Framework 側の規約が顧客リポジトリで一切強制されない。**本書が定める全 ArchUnit ルールに共通する** | `koiki-archunit-rules` の配布と Project Template への組み込み（§9.6、DoD 1a-3） |
| アーキテクチャオーナー不在による判断停滞 | 昇格、追従、Level 移行の判断が滞る | 代理者の指定（§9.4） |
| 昇格の乱発による Framework の肥大化 | 後方互換義務が積み上がる | チェックリストの全項目充足。昇格を ADR として記録（§9.2） |
| japicmp の誤検出 | 内部的な変更が破壊的変更として検出される | `internal` の除外設定。例外設定はアーキテクチャオーナーの承認を要する |
| NullAway の初期ビルド失敗多発 | 既存コードへ後から適用すると大量の違反が出る | **Phase 1a の立ち上げ時点から適用し、後から導入しない**（§21.4） |
| Skill と ADR の乖離 | ADR を改訂しても Skill が更新されない | 四半期レビューで妥当性を確認（§9.5） |
| `because()` の記述漏れ | エラーメッセージから意図が読み取れず、Skill の軽量化が成立しない | **DoD 1a-2 の完了条件に含める**（§21.3） |

### 28.8 計画と実行

| リスク | 内容 | 対策 |
|---|---|---|
| テストコスト増 | 統合試験が遅い | テストの分層、コンテナ再利用、CI 分割 |
| 運用基盤の先行肥大 | EKS 等で本体が遅れる | ECS 優先、Kubernetes-ready 止まりから開始（§23.4） |
| Phase 3 の肥大化 | HTMX 契約と Tier 1／2 実例の追加で検証が遅延する | SPA を Phase 4 へ送る。超過時は Tier 2 実例を最小構成へ |
| 追従作業による Phase 進行の圧迫 | 各 Phase の完了時に追従工数が発生する | 小刻みな追従による変更量の分散。全 Phase の見積もりへ追従工数を計上 |
| Flyway 複数構成の複雑性 | Spring Boot 自動構成と衝突し起動順序が不定になる | **Walking Skeleton の V2 で検証**（§27.3） |
| Oracle nightly の実行時間 | コンテナ起動が遅く nightly が長時間化する | 検証範囲の限定（§16.8）。イメージ選定と実行時間を Phase 2 で評価 |
| 顧客による Virtual Threads の誤有効化 | Java 21 ランタイムで有効化し pinning が発生する | 既定無効、有効化ガイドとチェックリスト（§23.3） |
| 顧客による KOIKI テーブルへの外部キー作成 | KOIKI のスキーマ変更が制約される | §16.7.4 の規約。Migration Guide とレビュー項目に反映 |

---

## 29. 今後作成する詳細設計文書

1. **KOIKI-JavaWeb-FW 用語集**（KOIKI-PYFW ⇔ Java 概念対応表を含む）
2. 正規リポジトリ／Maven モジュール構成
3. Package／Dependency 規約
4. Web API 標準
5. Error／Exception 標準
6. Security Architecture 詳細
7. Identity／RBAC Schema
8. Session／JWT／OIDC／SAML 契約
9. Audit Event 標準
10. Data Access 選択ガイド
11. **PostgreSQL／Oracle Migration 標準**（共通 DDL 記述規約を含む）
12. Transaction／Concurrency 標準
13. **UI プロファイル標準**（Thymeleaf／HTMX／SPA）
14. Batch／File 標準
15. Observability 標準
16. Test Strategy／Quality Gate
17. **Container／ECS 運用標準**（JVM 設定の設計指針と確認項目を含む）
18. **Release／Versioning／Support Policy**
19. Agent Guidance／Skills 設計
20. **Reference Application 仕様**（経費申請・承認）
21. **ArchUnit ルール仕様**
22. **Upgrade／Migration Guide 体系**
23. **運用手順書・ランブック体系**

---

## 30. ADR 一覧

**ADRは本書の設計判断を個別に記録する。**区分、検証証拠、承認状態は
`../adr/README.md`のADR registerを正本とする。

> **採番に関する注記** — ADR-018 と ADR-021 は欠番である。検討過程で仮採番したが、内容がそれぞれ ADR-035（Virtual Threads）および ADR-006 改訂・ADR-026（UI 戦略）へ集約されたためである。

### 基盤

| ADR | テーマ | 決定 |
|---|---|---|
| ADR-001 | Java Baseline | **Java 21 をターゲットバイトコードとする。**Java 25 は推奨実行環境かつ互換確認対象。年次見直しの対象 |
| ADR-002 | Spring Boot Version | **特定バージョンへ恒久固定せず、追従方針を規定する** |
| ADR-003 | Build | Maven、Parent ＋ BOM |
| ADR-013 | Distribution | Starter ＋ Reference ＋ Template |
| ADR-014 | Repository | Framework と顧客案件を分離 |
| ADR-015 | Deployment | Executable JAR／Container |
| ADR-016 | Reference Runtime | AWS ECS Fargate |
| ADR-034 | Null Safety | JSpecify 採用。Framework は NullAway 必須検査 |
| ADR-035 | Virtual Threads | 既定で無効。opt-in ＋ Java 25 以上を推奨条件とする |

### アーキテクチャ

| ADR | テーマ | 決定 |
|---|---|---|
| ADR-004 | Architecture | Modular Monolith ＋ Package by Feature（**モジュール境界のみを対象とする**） |
| ADR-005 | Module Tooling | **Spring Modulith を採用レベル Level 0〜3 で段階導入。**Level 2 以降はイベント基盤として中核依存 |
| ADR-022 | モジュール内部構造 | **構造 Tier 制**（Tier 1／Tier 2、昇格トリガを明示）。`domain.gateway` を含む |
| ADR-023 | Tier 2 のモデル方針 | **兼用を既定とし、分離をトリガ付きオプトインとする** |
| ADR-024 | Tier 2 の Repository 方針 | `domain.repository` にインターフェースを置き Spring Data が実装。MyBatis 採用時は例外 |
| ADR-025 | Domain Event | **同期を既定、非同期は明示選択。監査はイベント機構に乗せない** |

### UI

| ADR | テーマ | 決定 |
|---|---|---|
| ADR-006 | UI プロファイル方針 | **API 指向を正本とし、Thymeleaf＋HTMX と SPA を対等の公式プロファイルとする** |
| ADR-026 | UI プロファイルの提供順序 | Phase 3 で Thymeleaf＋HTMX、Phase 4 で SPA 参照実装 |
| ADR-027 | HTMX の同梱と第三者ライブラリ | HTMX を Thymeleaf プロファイルへ同梱。`htmx-spring-boot` を採用 |

### セキュリティと監査

| ADR | テーマ | 決定 |
|---|---|---|
| ADR-007 | Browser Auth | HTTP Session を第一標準 |
| ADR-008 | API Auth | OAuth 2.0 Bearer JWT |
| ADR-009 | Enterprise SSO | OIDC 優先、SAML 拡張 |
| ADR-020 | セッションストア | **Spring Session JDBC**（既存 PostgreSQL を利用、Redis 差し替え可） |
| ADR-036 | レート制御 | インフラ層へ委ね、**認証試行制御のみアプリケーション内に持つ** |

### データ

| ADR | テーマ | 決定 |
|---|---|---|
| ADR-010 | Database | PostgreSQL 標準、Oracle 適合 |
| ADR-011 | Persistence | **JPA／MyBatis／JdbcClient の3系統。**選択はモジュール単位 |
| ADR-012 | Migration | Flyway |
| ADR-019 | マルチテナンシー | **単一テナントを前提とする**（v1.0 のスコープ外） |
| ADR-028 | Open Session in View | **無効化する** |
| ADR-037 | キャッシュ | Spring Cache ＋ Caffeine。対象限定と TTL 必須 |
| ADR-038 | read model | Query契約と`record`は`application.query`が所有。単一集約はJPAのclass-based射影、複雑queryはJdbcClient |
| ADR-039 | MyBatis | 規約 ＋ BOM 管理（Level B）。モジュール単位で選択 |
| ADR-042 | テーブル所有権と Flyway | 接頭辞規約 ＋ 所有者別の独立管理 |
| ADR-044 | Oracle 検証戦略 | **Phase 2 から nightly スモーク。**vendor 分岐は先行導入しない |

### Web API と外部連携

| ADR | テーマ | 決定 |
|---|---|---|
| ADR-030 | JSON Processing | Jackson 3 全面採用。モジュールの自動検出を無効化 |
| ADR-031 | API Versioning | Spring 標準機構、パスセグメント方式。RFC 9745 非推奨処理を採用 |
| ADR-032 | Resilience | コアの `@Retryable`／`@ConcurrencyLimit` を標準。Circuit Breaker は Phase 4 で評価 |
| ADR-033 | HTTP Client | `RestClient` ＋ HTTP Service Interface（`@ImportHttpServices`で登録） |

### ガバナンスとリリース

| ADR | テーマ | 決定 |
|---|---|---|
| ADR-017 | Support & Upgrade Policy | **年次リリース、最新／直前の2ライン管理、Spring公式に合わせたOSSサポート終了日の明示。商用延長サポートを顧客オプションとする** |
| ADR-029 | Migration Support | **KOIKI 独自の OpenRewrite レシピを提供する** |
| ADR-040 | 昇格ポリシーの運用化 | Reference／Customer／Walking Skeleton発の昇格候補にチェックリスト7項目を適用し、Architecture Ownerによる判定とADR記録を行う |
| ADR-041 | Public API 境界 | `internal` パッケージ ＋ ArchUnit を基本、重要契約は Maven 分割。JPMS は採用しない |
| ADR-045 | Agent Skills の設計方針 | **判断のみを Skill に書く。**5本構成。ArchUnit の `because()` にADR番号・影響・修正方法を記述 |

### Reference Application

| ADR | テーマ | 決定 |
|---|---|---|
| ADR-043 | Reference Application | **業務題材は経費申請・承認。**単一アプリケーション ＋ Phase ごとのモジュール追加 |

---

## 付録A. KOIKI-JavaWeb-FW の定義

KOIKI-JavaWeb-FW とは、単なる Spring Boot テンプレート、共通部品集、コードジェネレーターではない。

次の要素を**一体として提供する**エンタープライズアプリケーション基盤である。

- Architecture Principles
- Maven BOM／Starter／Libraries
- Secure Defaults
- **ArchUnit ルールセット**（顧客プロジェクトへ配布可能な形で）
- Reference Application
- Project Template
- Test／Quality Gate
- Operations／Observability
- Documentation／ADR
- Agent Guidance
- **Release／Support Policy**
- **Migration Support（OpenRewrite レシピ）**
- **Governance（アーキテクチャオーナー、昇格ポリシー、四半期レビュー）**

**v0.1 からの追加は、いずれも「規約を実効化する仕組み」と「長期に使い続けるための約束」である。**規約を文書に書くだけでは守られず、優れた設計も更新されなければ使えなくなる、という認識に基づく。

---

## 付録B. 参照資料

### KOIKI-PYFW dev/v0.8

- `docs/design_kkfw_0.8.0.md`
- `docs/agent/architecture.md`
- `docs/agent/auth-security.md`
- `docs/agent/skills/koiki-project-overview/SKILL.md`

Repository: `https://github.com/zaziedlm/koiki-pyfw/tree/dev/v0.8`

### 公式技術資料

> **参照日と版を明示する。**本書の技術判断は、記載時点の情報に基づく。§9.5 の四半期レビューで更新の要否を確認する。

| 資料 | URL | 参照日 |
|---|---|---|
| Spring Boot Support Policy | `https://spring.io/projects/spring-boot#support` | 2026年7月 |
| Spring Boot System Requirements | `https://docs.spring.io/spring-boot/system-requirements.html` | 2026年7月 |
| Spring Boot Class Data Sharing / AOT Cache | `https://docs.spring.io/spring-boot/reference/packaging/aot-cache.html` | 2026年7月 |
| Spring Modulith Reference Documentation | `https://docs.spring.io/spring-modulith/reference/` | 2026年7月 |
| Spring Framework API Versioning | Spring Framework 7.0 Reference | 2026年7月 |
| Oracle Java SE Support Roadmap | `https://www.oracle.com/java/technologies/java-se-support-roadmap.html` | 2026年7月 |
| JSpecify | `https://jspecify.dev/` | 2026年7月 |
| MyBatis Spring Boot Starter | `https://mybatis.org/spring-boot-starter/` | 2026年7月 |
| htmx-spring-boot | `https://github.com/wimdeblauwe/htmx-spring-boot` | 2026年7月 |
| JVM Container Best Practices (AWS) | `https://aws.amazon.com/blogs/containers/jvm-memory-cpu-and-classpath-best-practices-for-java-containers-on-aws/` | 2026年7月 |
