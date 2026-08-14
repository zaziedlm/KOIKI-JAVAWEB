---
name: koiki-project-overview
description: Orient work in KOIKI-JavaWeb-FW by identifying the current phase, ownership, module boundary, authoritative documents, and appropriate KOIKI workflow. Use before planning, reviewing, or changing repository architecture, Framework, Reference, Customer, or Walking Skeleton work.
---

# KOIKI Project Overview

KOIKIでの作業位置を最初に確定し、所有権やPhaseを越えた実装を防ぐ。

## 作業を位置づける

1. リポジトリルートの`AGENTS.md`を読む。
2. ユーザー要求が属するPhaseと、検証・正式実装のどちらかを確認する。
3. 変更の所有者をFramework、Reference、Customer、Walking Skeletonから1つ選ぶ。
4. 対象となる業務モジュールまたはMavenモジュールを特定する。
5. 適用するSkill、設計文書、検証手段を決めてから編集する。

判断に必要な情報が不足し、選択により成果物の所有者やPublic APIが変わる場合は、推測せず確認する。

## 正本を使い分ける

| 確認事項 | 正本 |
|---|---|
| 全体方針、Phase、Tier、データ、イベント、Web、運用 | `docs/architecture/grand-design/KOIKI-JavaWeb-FW_グランドデザイン_v0.2.md` |
| リポジトリとMavenモジュールの責務 | `docs/architecture/KOIKI-JavaWeb-FW_Repository_Architecture_v0.1.md` |
| 個別の設計判断 | `docs/architecture/adr/` |
| Phase 0の実行順序と残件 | `docs/development/KOIKI-JavaWeb-FW_WalkingSkeleton実装計画_v1.0.md` |
| 実装で得た証拠 | `docs/architecture/validation/` |
| change固有の要求と計画 | `openspec/` |
| 実効バージョンとビルド設定 | 対象の`pom.xml`、Maven Wrapper、CI設定 |

文書が競合する場合は、ルート指示、グランドデザイン、ADR、対象changeの順に適用範囲を確認し、競合を報告する。検証証拠が設計上の推測を否定する場合は、証拠を優先して設計更新を提案する。

## 所有権を分離する

- **Framework**: 業務語彙を含まない安定した共通契約だけを置く。Spring標準で代替できる機能を独自実装しない。
- **Reference**: Frameworkの利用方法を実証する。Framework内部として扱わない。
- **Customer**: 顧客固有の業務、画面、外部連携、migrationを所有する。
- **Walking Skeleton**: 設計の実装可能性を調べる使い捨てコードとする。設定、規約実装、検証記録だけを正式工程へ引き継ぐ。

共通化できそうという理由だけでFrameworkへ移さない。Framework昇格はグランドデザイン§9.2の全条件を満たし、ADRで承認された場合に限る。

## モジュラーモノリスを保つ

最初に業務機能でモジュールを分け、その内部を責務で分ける。技術レイヤーをアプリケーション全体へ横断配置しない。

```text
application
├── module-a
│   ├── adapter
│   ├── application
│   ├── domain
│   └── configuration
└── module-b
    ├── adapter
    ├── application
    ├── domain
    └── configuration
```

他モジュールの`application`、`domain.model`、Repository、Adapterを直接参照しない。公開するモジュール間契約は原則として`domain.event`の値だけのイベントとする。`shared-kernel`は最小限に保ち、肥大化したらモジュール境界を見直す。

## モジュール内部の依存方向を保つ

```text
Inbound Adapter -> Application Use Case -> Domain
                         |
                         v
                   Outbound Port <- Outbound Adapter
```

- Inbound Adapterには入力受付、DTO変換、応答整形だけを置く。
- Application Use Caseにはトランザクション境界と処理調整を置く。
- Tier 2 Domainには不変条件と状態遷移を置く。
- Outbound AdapterにはDB、外部API、File、Messagingなどの技術詳細を置く。

業務機能を実装・変更する場合は`koiki-business-feature-work`を続けて使う。

## 現在の確定範囲を守る

Phase 0のV1〜V7で、ビルド基盤、ArchUnit配布、Flyway二階層、Tier 2実用性、OSIV境界、同期イベントを検証済みとして扱う。一方、次は後続Phaseの正式判断として固定しない。

- Spring Modulith Named Interfaceと正式バージョン
- Flyway Starterの所属と三階層への一般化
- 非同期イベントのLevel 2運用
- MyBatisの詳細実装規約
- REST API、Security、SPAの具体的実装パターン

未確定事項が必要になった場合は、該当Phaseの設計・実装検証として扱い、既定規約を先行生成しない。

## 作業開始時の結論を示す

編集前に必要な範囲で次を短く示す。

```text
Phase / status:
Ownership:
Target module:
Applicable guidance:
Validation:
Deferred decisions:
```

機械検査できる規則をSkill本文へ複製しない。Maven、ArchUnit、NullAway、japicmp等を実行し、違反時はメッセージが示すADRと修正方針に従う。
