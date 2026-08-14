# Skills

このディレクトリをKOIKI固有Skillの正本とします。

| Skill | 用途 |
|---|---|
| `koiki-project-overview` | 作業のPhase、Ownership、対象Module、参照すべき設計と検証を特定する |
| `koiki-business-feature-work` | 業務機能のTier、責務配置、永続化、Module連携、View/API境界を判断する |

## 管理方針

- `.agents/skills/`と`.claude/skills/`には、正本を読むための薄いアダプターだけを置く。
- ArchUnit、NullAway、Maven等が機械検査する規則はSkillへ複製しない。
- OpenSpecのproposal、spec、design、tasksを変更固有の正本とし、Skillで置き換えない。
- Walking Skeletonで未検証の規約を推測で固定しない。
- 変更で得た検証結果は`docs/architecture/validation/`へ記録する。

Phase 0ではこの2種だけを対象とし、フルセットへの拡張は後続Phaseで判断します。
