# KOIKI-JavaWeb-FW Agent Guidance — Phase 0

このRepositoryでは `KOIKI-JavaWeb-FW グランドデザイン v0.2` を上位設計とする。

Phase 0 Walking Skeletonでは次を優先する。

1. Spring標準機能を優先する。
2. Framework / Reference / Customer のOwnershipを混在させない。
3. Walking Skeletonコードを正式Frameworkへ昇格させない。
4. 未使用の将来Module / Packageを先行生成しない。
5. 実装で確認できる事項は、文書上の推測より実装検証を優先する。
6. 検証結果は `docs/architecture/validation/` に記録する。
7. Repository内の作業を位置づけるときは、`docs/agent/skills/koiki-project-overview/SKILL.md`を読む。
8. 業務機能を設計・実装・レビューするときは、加えて`docs/agent/skills/koiki-business-feature-work/SKILL.md`を読む。

`docs/agent/skills/`をKOIKI固有Skillの正本とする。`.agents/skills/`と`.claude/skills/`は、
各エージェントから正本を発見するための薄い導線とし、設計規則を複製しない。

OpenSpecは変更固有の要求、設計、タスクを管理する。KOIKI固有Skillは横断的な判断手順を提供し、
OpenSpec成果物を上書きしない。OpenSpecはWalking Skeleton中の試行対象であり、正式配置は固定しない。
