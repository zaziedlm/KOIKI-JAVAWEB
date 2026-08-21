# Agent Guidance

Phase 0 Walking Skeletonで検証済みの最小KOIKI Agent Skillsを、正式工程でも継続利用します。

- `skills/koiki-project-overview/`: Phase、Ownership、対象Module、正本、作業フローを特定する。
- `skills/koiki-business-feature-work/`: 業務ModuleのTier、責務配置、永続化、連携、View/API境界を判断する。

ArchUnitやNullAwayで機械検査できる規則をSkillsへ重複記述せず、人間・AIが判断すべき事項を
中心に記述します。

`docs/agent/skills/`を内容の正本とします。ルート`AGENTS.md`と`CLAUDE.md`、
`.agents/skills/`、`.claude/skills/`は正本への導線だけを持ちます。

OpenSpec changeがRepositoryに存在する場合、OpenSpecが変更固有の要求、設計、タスクを管理し、
KOIKI SkillsがRepository横断の設計判断を支援します。OpenSpecはPhase 1aの必須toolingではありません。
