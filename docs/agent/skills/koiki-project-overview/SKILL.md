---
name: koiki-project-overview
description: Orient work in KOIKI-JavaWeb-FW by identifying the current phase, ownership, module boundary, authoritative documents, and appropriate KOIKI workflow. Use before planning, reviewing, or changing repository architecture, Framework, Reference, Customer, or Walking Skeleton work.
---

# KOIKI Project Overview

KOIKIでの作業位置を最初に確定し、所有権やPhaseを越えた実装を防ぐ。

## 作業を位置づける

1. リポジトリルートの`AGENTS.md`を読む。
2. ユーザー要求が属するPhaseと、検証・正式実装のどちらかを確認する。
3. 変更の所有者をFramework、Reference、Customer、Tooling、Walking Skeletonから1つ選ぶ。
4. 対象となる業務モジュールまたはMavenモジュールを特定する。
5. 適用するSkill、設計文書、検証手段を決めてから編集する。

判断に必要な情報が不足し、選択により成果物の所有者やPublic APIが変わる場合は、推測せず確認する。

## 正本を使い分ける

| 確認事項 | 正本 |
|---|---|
| 全体方針、Phase、Tier、データ、イベント、Web、運用 | `docs/architecture/grand-design/KOIKI-JavaWeb-FW_グランドデザイン_v0.2.md` |
| リポジトリとMavenモジュールの責務 | `docs/architecture/KOIKI-JavaWeb-FW_Repository_Architecture_v0.1.md` |
| 個別の設計判断 | `docs/architecture/adr/` |
| Phase 0の完了済み実行履歴 | `docs/development/KOIKI-JavaWeb-FW_WalkingSkeleton実装計画_v1.0.md` |
| Phase 1aへの引継ぎ境界 | `docs/development/KOIKI-JavaWeb-FW_Phase1a_WalkingSkeleton_Transition_Inventory_v0.1.md` |
| Phase 1a Build Foundation | `docs/development/KOIKI-JavaWeb-FW_Phase1a実行計画_v0.1.md` |
| Phase 1b Runtime Foundation | `docs/development/KOIKI-JavaWeb-FW_Phase1b実行計画_v0.1.md` |
| Phase 2 Security Foundation | `docs/development/KOIKI-JavaWeb-FW_Phase2実行計画_v0.1.md` |
| Securityを伴う業務アプリ開発の入口 | `docs/development/phase2-developer-journey.md` |
| KOIKI / Spring Boot / Java baselineとsupport状態 | `docs/architecture/KOIKI-JavaWeb-FW_Baseline_Compatibility_v0.1.md` |
| 実装で得た証拠 | `docs/architecture/validation/` |
| change固有の要求と計画 | Repositoryに存在する場合の`openspec/` |
| 実効バージョンとビルド設定 | 対象の`pom.xml`、Maven Wrapper、CI設定 |

文書が競合する場合は、ルート指示、グランドデザイン、ADR、存在する場合の対象changeの順に適用範囲を確認し、競合を報告する。検証証拠が設計上の推測を否定する場合は、証拠を優先して設計更新を提案する。

## 所有権を分離する

- **Framework**: 業務語彙を含まない安定した共通契約だけを置く。Spring標準で代替できる機能を独自実装しない。
- **Reference**: Frameworkの利用方法を実証する。Framework内部として扱わない。
- **Customer**: 顧客固有の業務、画面、外部連携、migrationを所有する。
- **Tooling**: Consumer、verification fixture、検証script、性能harness、移行試作等を置く。正式release unitやCustomer配布物へ含めない。
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

他モジュールの`application`、`domain.model`、Repository、Adapterを直接参照しない。公開するモジュール間契約は原則として`domain.event`の値だけのイベントとする。Phase 3 Referenceのcurrent-value master確認だけはADR-049の狭い同期read-only contractを明示例外とし、consumer moduleのPort / Adapterを介して使う。`shared-kernel`は最小限に保ち、肥大化したらモジュール境界を見直す。

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

Phase 0、Phase 1a Build Foundation、Phase 1b Runtime FoundationおよびPhase 2 Security Foundationは
`COMPLETE / ACCEPTED`である。次を承認済みbaselineとして扱う。

- Phase 1a: Root Reactor、Parent / BOM、Architecture Contract、ArchUnit Rules、Tier 1 / 2 Feature Template、
  JSpecify / NullAway、Public API / japicmp、Java 21 build・Java 21 / 25 runtimeおよび内部snapshot配布
- Phase 1b: API、Data、Data JPA、Observabilityのruntime Starter、PostgreSQL / Flyway二階層、OSIV無効、
  同期Domain Event、non-web maintenance processおよびCustomer-like Consumer
- Phase 2: default deny、CSRF / Security Header既定、local Session、OIDC Client、Bearer Resource Server、
  Business / Security Audit、Identity、Spring Session JDBC、Framework migrationおよびReference `identity`
- Spring Modulith 2.1.1のLevel 0はtest scopeだけで使用し、runtime依存を追加しない。
- formal Framework release unitは14 projects / 11 JAR、Phase 2 publish unitはRoot aggregatorを除く13座標である。
  Reference、Customer-like Consumer、fixture、性能harnessおよびOpenRewrite prototypeは配布しない。
- Phase 2内部snapshotはaccepted manifestで固定済みだが、正式release、一般公開repositoryまたは
  Customer向けsupport付き配布ではない。

Securityを伴う業務アプリの依存選択、profile、Ownership、診断および検証入口は
`docs/development/phase2-developer-journey.md`を使う。一方、次は後続Phaseまたはoptional Gateの正式判断として固定しない。

- Phase 3 P3-B1以降のread model、MVC / HTMX実装、最小REST APIおよびbrowser / E2E実装
- Project Template、正式Upgrade / Migration Guideおよび正式OpenRewrite recipe
- Spring Modulith Level 2、非同期Domain Eventおよびruntime依存
- Authorization Server、SAML、Redis、WebFlux、SPA production実装
- MyBatisの詳細実装規約、Oracle、AWS固有Adapterおよびcloud固有実装

Phase 3 Reference Vertical Sliceは
`docs/development/KOIKI-JavaWeb-FW_Phase3実行計画_v0.1.md`のGate P3-1が承認済みであり、
P3-CP0とP3-A0を完了し、P3-A1〜P3-A4およびGate Aを`COMPLETE / ACCEPTED`、P3-B0を
`COMPLETE / OWNER APPROVED`とした。P3-A0で承認された有効master確認の狭い同期read-only contract、
command整合の同期Event、承認者部門scopeのReference Ownership、V1〜V3 migration、module内FKのみの
方針を維持する。P3-B0で正式`koiki-starter-web-mvc`、初期Java Public API 0型、Spring標準＋KOIKI内部
HTMX fallback、Thymeleaf HTML主軸の選択適用、Phase 2 Security再利用および非配布browser Tooling境界を
承認した。次はP3-B1 read modelであり、P3-B2のMaven module / Starter / dependency変更またはP3-B3の
browser runnerを先行しない。以降も同計画のCP、blocking reviewおよびGateを順守する。

個別のPublic API、module、Starter、migration、dependency、workflowまたは既定規約は、対応するblocking reviewと
Evidenceより前に先行生成しない。remote push / PR / merge、ruleset変更、workflow dispatchおよびsnapshot publishは
Remote Gateの個別承認を必要とする。

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
