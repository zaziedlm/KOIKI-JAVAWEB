# Architecture

- `grand-design/KOIKI-JavaWeb-FW_グランドデザイン_v0.2.md`: 全体方針とPhase別DoDの正本
- `KOIKI-JavaWeb-FW_Repository_Architecture_v0.1.md`: RepositoryとOwnershipの正本
- `KOIKI-JavaWeb-FW_Phase0_DoD_Closeout_v0.1.md`: Phase 0全体DoDの完了判定と証拠台帳
- `KOIKI-JavaWeb-FW_Phase_Estimate_Feasibility_v0.1.md`: Phase 1a〜5のDoD別規模、依存、risk、実現可能性評価
- `KOIKI-JavaWeb-FW_Baseline_Compatibility_v0.1.md`: KOIKI／Spring Boot／Javaの対応表と更新・support管理の正本
- `governance/KOIKI-JavaWeb-FW_Architecture_Governance_v0.1.md`: Owner、代理・継続性、Architecture Review、Phase判定
- `validation/`: Walking Skeleton等の実装検証証拠
- `validation/phase1a-build-foundation.md`: Phase 1a A2 / G1の正式build baseline実効検証
- `validation/phase1a-architecture-contract.md`: Phase 1a A3の最小Architecture Public API実効検証
- `validation/phase1a-ci-build-foundation.md`: Phase 1a A4のWindows / Ubuntu CI Build Foundation実効検証
- `validation/phase1a-feature-template.md`: Phase 1a B1のTier 1 / Tier 2 Feature Template実効検証
- `validation/phase1a-archunit-api-design.md`: Phase 1a B2 COMPLETEのArchUnit Public API・rule matrix設計とB3引継ぎ
- `validation/phase1a-archunit-rules.md`: Phase 1a B3 COMPLETEのArchUnit Rules実装・検証記録
- `validation/phase1a-null-safety.md`: Phase 1a B4 COMPLETEのJSpecify / NullAway正常・負例・復元検証
- `validation/phase1a-template-integration.md`: Phase 1a B5のFeature Template quality gate統合・ローカル検証記録
- `validation/phase1a-internal-snapshot.md`: Phase 1a C1の内部snapshot公開計画・Owner Review・実装検証記録

Phase 0では次を中心に管理し、承認済み成果をPhase 1aの判断基準として維持します。

- `grand-design/`: 上位設計
- `adr/`: Architecture Decision Record
- `diagrams/`: Module / dependency等の図
- `validation/`: Walking Skeleton等の実装検証結果

Walking Skeletonのsource code自体を `validation/` へ保存する用途ではありません。
