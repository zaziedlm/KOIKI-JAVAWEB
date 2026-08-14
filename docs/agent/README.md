# Agent Guidance

Phase 0 Walking Skeletonで検証する最小Agent Skillsを管理します。

- `skills/koiki-project-overview/`: Phase、Ownership、対象Module、正本、作業フローを特定する。
- `skills/koiki-business-feature-work/`: 業務ModuleのTier、責務配置、永続化、連携、View/API境界を判断する。

ArchUnitやNullAwayで機械検査できる規則をSkillsへ重複記述するのではなく、
人間・AIが判断すべき事項を中心に記述します。

`docs/agent/skills/`を内容の正本とします。ルート`AGENTS.md`と`CLAUDE.md`、
`.agents/skills/`、`.claude/skills/`は正本への導線だけを持ちます。

OpenSpecが変更固有の要求、設計、タスクを管理し、KOIKI SkillsがRepository横断の
設計判断を支援します。両者が競合した場合は、適用範囲を確認して報告します。
