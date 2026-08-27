# KOIKI-JavaWeb-FW — Phase 1a Build Foundation

KOIKI-JavaWeb-FWは、Spring Bootを基盤として、企業向けJava Web Applicationを一貫した
Architecture、build contract、品質規約のもとで構築するためのFramework projectです。

Phase 0 Walking SkeletonとArchitecture Baselineは2026年8月19日に完了しました。
現在はPhase 1a Build Foundationとして、検証済みの知見を正式なMaven成果物、Public API、
CIおよび外部Consumer検証へ再構成しています。

## 現在の状態

- Phase 0 Architecture Baseline: COMPLETE / ACCEPTED
- Phase 1a Build Foundation: IN PROGRESS
- Milestone A / B: COMPLETE
- Milestone C: C1・C2 COMPLETE / C3 Gate 1・2 ACCEPTED・Gate 3 NEXT
- 正式groupId / Java base package: `org.koikifw`
- Build JDK / target bytecode: Java 21
- Runtime compatibility target: Java 21 / Java 25

Root Reactorは、BOM、Parent、Architecture Contract、ArchUnit Rulesの正式4モジュールだけで構成します。
Phase 0の`walking-skeleton/ws-smoke-*`はRepository内に履歴資産として残っていますが、Root Reactor、
正式配布対象およびPhase 1aの実装基盤には含まれません。C1では正式4成果物の内部snapshot公開を完了し、
C2ではそのsnapshotだけを利用するRepository外Consumerのlocal / remote検証を完了しました。

## 正本

- [グランドデザイン v0.2](docs/architecture/grand-design/KOIKI-JavaWeb-FW_グランドデザイン_v0.2.md)
- [Repository Architecture](docs/architecture/KOIKI-JavaWeb-FW_Repository_Architecture_v0.1.md)
- [ADR Register](docs/architecture/adr/README.md)
- [Phase 0 DoD Closeout](docs/architecture/KOIKI-JavaWeb-FW_Phase0_DoD_Closeout_v0.1.md)
- [Baseline Compatibility](docs/architecture/KOIKI-JavaWeb-FW_Baseline_Compatibility_v0.1.md)
- [Glossary](docs/standards/KOIKI-JavaWeb-FW_Glossary_v0.1.md)
- [Phase 1a実行計画](docs/development/KOIKI-JavaWeb-FW_Phase1a実行計画_v0.1.md)
- [Phase 1a Walking Skeleton引継ぎ台帳](docs/development/KOIKI-JavaWeb-FW_Phase1a_WalkingSkeleton_Transition_Inventory_v0.1.md)
- [Architecture / Validation index](docs/architecture/README.md)

## Ownership

- Framework: 複数案件へ提供する、業務語彙を含まない正式KOIKI成果物
- Reference: Frameworkの正規利用方法と受入条件を実証するApplication
- Customer: 別Repositoryで管理する顧客固有業務・画面・外部連携・migration
- Walking Skeleton: Phase 0の実装可能性を示す固定Commit上の検証証拠

Walking SkeletonのJava、Template、SQL、一時artifactを正式FrameworkまたはReferenceへ
直接移植しません。設定、失敗条件、test観点を参照し、正式Ownershipで再設計・再実装します。

## Phase 1aの範囲

- Root Reactor / Parent / BOM / Maven Wrapper
- Architecture ContractとArchUnit rules
- Spring Modulith Level 0（2.1.0、test scope、runtime依存なし）
- Tier 1 / Tier 2 Feature Template
- JSpecify / NullAway
- Public API inventoryとjapicmp
- Java 21 build、Java 21 / 25 runtime matrix
- CIとsnapshot artifactを用いたRepository外Consumer検証

Runtime Foundation、Security、Reference業務実装、REST、SPA、非同期event、正式Deploymentは
所定の後続Phaseで扱います。未使用の将来ModuleやStarterは先行生成しません。

## Agent Guidance

Repository共通の作業指針は[AGENTS.md](AGENTS.md)、KOIKI固有Skillの正本は
[docs/agent/skills/](docs/agent/skills/README.md)を参照してください。
