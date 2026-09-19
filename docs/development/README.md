# Development

このindexは、現在の開発者入口と、完了済みPhaseの履歴文書を分けて案内します。個別handoff文書は
作成時点の状態を保存する履歴であり、現在状態は実行計画、Architecture Validation indexおよびGit履歴を優先します。

## 現在の入口

- [アプリ開発チーム向け引継ぎガイド](application-team-handoff-guide.md): Phase履歴を知らない開発者が、提供物／非提供物、構成・Starter選択、Reference実行、最初のCustomer-owned module、copy禁止、拡張分類、診断およびFramework側への戻し条件を一本道で確認する第一入口
- [UI / Authentication Profile Selection Guide](frontend-authentication-profile-guide.md): MVC単一JAR、same-origin React、Next.js BFF、direct Token SPAおよびALB edge認証のUI / Session / Token / SSO責任選択
- [Phase 2 Developer Journey](phase2-developer-journey.md): Security Starter、認証profile、Ownership、診断および検証入口の詳細
- [P4-AR6 実チーム受入worksheet](p4-ar6-actual-team-reception-worksheet.md): 実行前承認、環境、journey、Repository topology、Customer-owned module、責任分担、findingおよびcleanupを非機密情報だけで記録する様式
- [Pre-Phase 4 Adoption Readiness計画](KOIKI-JavaWeb-FW_Pre-Phase4_Adoption_Readiness計画_v0.1.md): Framework、Consumer、Reference、開発環境およびDeveloper Handoffを実案件連携前に確認するtransition Gateの正本

共有source baselineのtag名と現在状態は[Repository Top README](../../README.md)を正本とします。source milestoneは
正式Maven release、実チーム受入完了、P4-AR7、Gate P4-ARまたはPhase 4開始を意味しません。

## 完了済みPhaseの計画

- [Phase 3実行計画](KOIKI-JavaWeb-FW_Phase3実行計画_v0.1.md): `COMPLETE / ACCEPTED`となったReference Vertical Sliceとpost-merge remote closeout
- [Phase 2実行計画](KOIKI-JavaWeb-FW_Phase2実行計画_v0.1.md): `COMPLETE / ACCEPTED`となったSecurity FoundationのGate、Milestone、DoD、Gate C remote closeout
- [Phase 1b実行計画](KOIKI-JavaWeb-FW_Phase1b実行計画_v0.1.md): Runtime Foundationのartifact Ownership、Customer-like Consumer、Milestone、DoDおよびGate 2 closeout
- [Phase 1a実行計画](KOIKI-JavaWeb-FW_Phase1a実行計画_v0.1.md): Build Foundationの判断Gate、Work Package、DoDおよびcloseout
- [Phase 0 Walking Skeleton実装計画](KOIKI-JavaWeb-FW_WalkingSkeleton実装計画_v1.0.md): Walking Skeletonの完了済み正式履歴計画
- [Walking Skeleton引継ぎ台帳](KOIKI-JavaWeb-FW_Phase1a_WalkingSkeleton_Transition_Inventory_v0.1.md): Phase 1aへ引き継いだ成果物の分類と直接昇格を防ぐ境界

## 履歴handoffとEvidence

`phase*-handoff-*.md`、`phase*-start-handoff-*.md`および同種の文書は、別PC・新規AIセッション・
commit point間で作業を再開するための時点記録です。現在の実装状態へ逐次書き換えず、当時の判断と停止条件を保持します。
実装で得たEvidenceと現在のWork Package状態は[Architecture / Validation index](../architecture/README.md)を参照してください。

Walking Skeletonの再実行方法と固定Commit上の証拠は、Repository rootの履歴と
`../architecture/validation/`を参照してください。
