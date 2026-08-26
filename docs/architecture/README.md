# Architecture

## 設計文書

- `grand-design/KOIKI-JavaWeb-FW_グランドデザイン_v0.2.md`: 全体方針とPhase別DoDの正本
- `KOIKI-JavaWeb-FW_Repository_Architecture_v0.1.md`: RepositoryとOwnershipの正本
- `KOIKI-JavaWeb-FW_Phase0_DoD_Closeout_v0.1.md`: Phase 0全体DoDの完了判定と証拠台帳
- `KOIKI-JavaWeb-FW_Phase_Estimate_Feasibility_v0.1.md`: Phase 1a〜5のDoD別規模、依存、risk、実現可能性評価
- `KOIKI-JavaWeb-FW_Baseline_Compatibility_v0.1.md`: KOIKI／Spring Boot／Javaの対応表と更新・support管理の正本
- `governance/KOIKI-JavaWeb-FW_Architecture_Governance_v0.1.md`: Owner、代理・継続性、Architecture Review、Phase判定
- `adr/`: Architecture Decision Record
- `diagrams/`: Module / dependency等の図

## Validation index

実装で得た証拠の正本は`validation/`に置きます。2026年8月26日時点で、Phase 1aはMilestone A・Bと
Milestone CのC1まで完了しています。C2以降の検証記録は、各WPの実装・Owner Review後に追加します。

### Phase 1a Build Foundation

| Work Package | 状態 | 検証記録 |
|---|---|---|
| A2 / G1 | COMPLETE / ACCEPTED | `validation/phase1a-build-foundation.md` |
| A3 | COMPLETE / ACCEPTED | `validation/phase1a-architecture-contract.md` |
| A4 | COMPLETE / ACCEPTED | `validation/phase1a-ci-build-foundation.md` |
| B1 | COMPLETE / ACCEPTED | `validation/phase1a-feature-template.md` |
| B2 | COMPLETE / ACCEPTED | `validation/phase1a-archunit-api-design.md` |
| B3 | COMPLETE / ACCEPTED | `validation/phase1a-archunit-rules.md` |
| B4 | COMPLETE / ACCEPTED | `validation/phase1a-null-safety.md` |
| B5 | COMPLETE / ACCEPTED | `validation/phase1a-template-integration.md` |
| C1 | COMPLETE / Gate 1〜4 ACCEPTED | `validation/phase1a-internal-snapshot.md` |

### Phase 0 Walking Skeleton

| 対象 | 状態 | 検証記録 |
|---|---|---|
| Build Foundation | Completed | `validation/walking-skeleton-build-foundation.md` |
| ArchUnit配布 | Completed | `validation/walking-skeleton-archunit-distribution.md` |
| Flyway二階層 | Completed | `validation/walking-skeleton-flyway-two-tier.md` |
| Tier 2 practicality | Completed | `validation/walking-skeleton-tier2-practicality.md` |
| Agent Skills | Completed | `validation/walking-skeleton-agent-skills.md` |
| Phase 0統合完了レビュー | Completed | `validation/walking-skeleton-phase0-completion.md` |

Walking Skeletonの検証記録はPhase 1aの判断根拠として維持しますが、そのsource code自体を
`validation/`へ保存したり、正式Framework成果物へ直接昇格させたりしません。
