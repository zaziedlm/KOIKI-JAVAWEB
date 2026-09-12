# KOIKI-JavaWeb-FW — Phase 2 Security Foundation

KOIKI-JavaWeb-FWは、Spring Bootを基盤として、企業向けJava Web Applicationを一貫した
Architecture、build contract、品質規約のもとで構築するためのFramework projectです。

Phase 0 Walking SkeletonとArchitecture Baselineは2026年8月19日に完了しました。
Phase 1a Build Foundationでは、検証済みの知見を正式なMaven成果物、Public API、CIおよび
外部Consumer検証へ再構成し、2026年8月27日に完了しました。
Phase 1b Runtime Foundationでは、Spring標準を優先したAPI、Data、JPA、Observabilityの
runtime Starterと、Customer-like Consumerによる運用経路を実装・検証し、2026年8月30日に完了しました。
Phase 2 Security Foundationでは、default deny、local / OIDC / Bearer認証、Identity、Audit、Spring Session JDBC、
PostgreSQL migrationおよびReference `identity`を実装・検証しました。Milestone A / B、P2-C1 / C2は
Architecture Owner承認済みで、現在はP2-C3 Developer Journey / DoD closeoutを進めています。

業務アプリケーション開発の開始点は
[Phase 2 Security Foundation Developer Journey](docs/development/phase2-developer-journey.md)です。

## 現在の状態

- Phase 0 Architecture Baseline: COMPLETE / ACCEPTED
- Phase 1a Build Foundation: COMPLETE / ACCEPTED
- Phase 1b Milestone A / B / C: COMPLETE / ACCEPTED
- Phase 1b Runtime Foundation: COMPLETE / GATE 2 ACCEPTED
- Phase 2 Milestone A / B: COMPLETE / ACCEPTED
- Phase 2 P2-C1 / P2-C2: COMPLETE / ARCHITECTURE OWNER APPROVED
- Phase 2 P2-C3: C3-1 / C3-2 COMPLETE / ARCHITECTURE OWNER APPROVED
- 正式groupId / Java base package: `org.koikifw`
- Build JDK / target bytecode: Java 21
- Runtime compatibility target: Java 21 / Java 25

Root ReactorはReferenceを含む15 projectsです。formal Framework release unitは、Aggregator、BOM、Parent、
Architecture Contract、ArchUnit Rules、Starter 8件、Testing supportの14 projectsで、うち11件がJARです。
ReferenceはRootの回帰には参加しますが、BOM、formal release unitおよびpublish unitには含めません。
Customer-like Consumer、検証fixture、性能harness、OpenRewrite prototypeはTooling-ownedであり、配布しません。

Phase 0 Walking Skeletonのsourceと一時Maven座標は正式本線から除去し、固定branch、Git履歴および
Validation文書を検証証拠として保持します。C1では正式4成果物の内部snapshot公開を完了し、
C2ではそのsnapshotだけを利用するRepository外Consumerのlocal / remote検証を完了しました。
C3ではPublic API互換性、C4ではJava 21 / 25 runtime互換性を正式required checkとして実証し、
C5ではBaseline、Repository hygiene、DoD traceabilityおよびmain最終CIをcloseoutしました。
Phase 1bでは9成果物の内部snapshotを公開し、fresh remote repositoryから全座標を解決して
独立Customer-like Consumerをbuild／testする配布経路まで実証しました。

## 正本

- [グランドデザイン v0.2](docs/architecture/grand-design/KOIKI-JavaWeb-FW_グランドデザイン_v0.2.md)
- [Repository Architecture](docs/architecture/KOIKI-JavaWeb-FW_Repository_Architecture_v0.1.md)
- [ADR Register](docs/architecture/adr/README.md)
- [Phase 0 DoD Closeout](docs/architecture/KOIKI-JavaWeb-FW_Phase0_DoD_Closeout_v0.1.md)
- [Baseline Compatibility](docs/architecture/KOIKI-JavaWeb-FW_Baseline_Compatibility_v0.1.md)
- [Glossary](docs/standards/KOIKI-JavaWeb-FW_Glossary_v0.1.md)
- [Phase 1a実行計画](docs/development/KOIKI-JavaWeb-FW_Phase1a実行計画_v0.1.md)
- [Phase 1b実行計画](docs/development/KOIKI-JavaWeb-FW_Phase1b実行計画_v0.1.md)
- [Phase 2実行計画](docs/development/KOIKI-JavaWeb-FW_Phase2実行計画_v0.1.md)
- [Phase 2 Developer Journey](docs/development/phase2-developer-journey.md)
- [Phase 1a Walking Skeleton引継ぎ台帳](docs/development/KOIKI-JavaWeb-FW_Phase1a_WalkingSkeleton_Transition_Inventory_v0.1.md)
- [Architecture / Validation index](docs/architecture/README.md)

## Ownership

- Framework: 複数案件へ提供する、業務語彙を含まない正式KOIKI成果物
- Reference: Frameworkの正規利用方法と受入条件を実証するApplication
- Customer: 別Repositoryで管理する顧客固有業務・画面・外部連携・migration
- Walking Skeleton: Phase 0の実装可能性を示す固定Commit上の検証証拠

Walking SkeletonのJava、Template、SQL、一時artifactを正式FrameworkまたはReferenceへ
直接移植しません。設定、失敗条件、test観点を参照し、正式Ownershipで再設計・再実装します。

## 現在の正式範囲

- Root Reactor / Parent / BOM / Maven Wrapper
- Architecture ContractとArchUnit rules
- Spring Modulith Level 0（2.1.0、test scope、runtime依存なし）
- Tier 1 / Tier 2 Feature Template
- JSpecify / NullAway
- Public API inventoryとjapicmp
- Java 21 build、Java 21 / 25 runtime matrix
- CIとsnapshot artifactを用いたRepository外Consumer検証
- API、Data、Data JPA、Observabilityのruntime Starter
- Problem Details／Validation、Jackson 3、API Versioning、Spring標準Resilience
- PostgreSQL／Flyway二階層、structured log／correlation、health、OSIV false
- Tooling-owned Consumerによる同期Domain Eventとnon-web maintenance process
- default deny、CSRF／Security Headerの安全な既定
- local Session、OIDC Client、Bearer Resource Serverの分離profile
- Business／Security Auditのtransaction境界
- Framework Identity、Role／Permission、認証試行／lock
- Spring Session JDBC、2 process共有、全Session失効、non-web cleanup
- Framework Flyway migration 3件／11 tableとReference `identity`

Phase 2 remote snapshot、Project Template、正式Upgrade / Migration Guide、正式OpenRewrite recipe、Authorization Server、
SAML、Redis、WebFlux、Spring Modulith Level 2、非同期Domain Event、Oracle、SPA production実装、cloud固有実装および
正式releaseは所定のGateまたは後続Phaseで扱います。未使用の将来ModuleやStarterは先行生成しません。

## Agent Guidance

Repository共通の作業指針は[AGENTS.md](AGENTS.md)、KOIKI固有Skillの正本は
[docs/agent/skills/](docs/agent/skills/README.md)を参照してください。
