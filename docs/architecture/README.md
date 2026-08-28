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

実装で得た証拠の正本は`validation/`に置きます。2026年8月28日時点で、Phase 1aはMilestone A〜C、
C1〜C5およびGate 1〜4を完了し、Architecture Ownerが最終CIを含むcloseoutを承認しています。
Phase 1b Runtime FoundationはCP0開始baselineとGate 1 Owner Review、CP1〜CP3を完了し、
Milestone Aのlocal検証完了／PR CI確認前です。

### Phase 1b Runtime Foundation

| Commit Point | 状態 | 検証記録 |
|---|---|---|
| CP0 Start Baseline | COMPLETE / Gate 1 ACCEPTED | `validation/phase1b-cp0-start-baseline.md` |
| CP1 Modulith 2.1.1 Regression | COMPLETE / 2.1.1 ADOPTED | `validation/phase1b-cp1-modulith-2.1.1-regression.md` |
| CP1 Runtime Artifact / Consumer | COMPLETE | `validation/phase1b-cp1-runtime-artifact-consumer.md` |
| CP2 Runtime Core | COMPLETE | `validation/phase1b-cp2-runtime-core.md` |
| CP3 Problem Details | COMPLETE / PR CI PENDING | `validation/phase1b-cp3-problem-details.md` |

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
| C2 | COMPLETE / Gate 1〜4 ACCEPTED | `validation/phase1a-external-consumer.md` |
| C3 | COMPLETE / Gate 1〜4 ACCEPTED | `validation/phase1a-public-api-compatibility.md` |
| C4 | COMPLETE / Gate 1〜4 ACCEPTED | `validation/phase1a-java-runtime-matrix.md` |
| C5 | COMPLETE / Gate 1〜4 ACCEPTED | `validation/phase1a-closeout.md` |

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
