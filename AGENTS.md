# KOIKI-JavaWeb-FW Agent Guidance — Phase 1a

このRepositoryでは、`KOIKI-JavaWeb-FW グランドデザイン v0.2`の
`ACCEPTED（Phase 0 Architecture Baseline）`を上位設計とする。

Phase 1a Build Foundationでは次を優先する。

1. Spring標準機能を優先する。
2. Framework / Reference / Customer / Walking SkeletonのOwnershipを混在させない。
3. Walking Skeletonのcode、Template、migration SQL、一時Maven座標を正式成果物へ直接昇格させない。
4. Parent、BOM、Architecture Contract、ArchUnit、Spring Modulith Level 0、Null Safety、Public API互換性、Java runtime matrix、CIを正式構成として再実装する。
5. Runtime、Security、Reference業務、SPA・非同期、Production Baselineの成果物を先行実装しない。
6. 未使用の将来Module / Package / Starter / Public APIを先行生成しない。
7. 実装で確認できる事項は文書上の推測より実装検証を優先し、結果を`docs/architecture/validation/`へ記録する。
8. Repository内の作業を位置づけるときは、`docs/agent/skills/koiki-project-overview/SKILL.md`を読む。
9. 業務機能を設計・実装・レビューするときは、加えて`docs/agent/skills/koiki-business-feature-work/SKILL.md`を読む。

`docs/agent/skills/`をKOIKI固有Skillの正本とする。`.agents/skills/`と`.claude/skills/`は、
各エージェントから正本を発見するための薄い導線とし、設計規則を複製しない。

OpenSpecは、Repositoryに採用済みのchangeが存在する場合に限り、変更固有の要求、設計、タスクの
正本として参照できる。Phase 1aの必須tooling、Maven build、CIまたはConsumerの前提にはしない。
