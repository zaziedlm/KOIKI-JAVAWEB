# Development

このindexは、現在の開発者入口と、完了済みPhaseの履歴文書を分けて案内します。個別handoff文書は
作成時点の状態を保存する履歴であり、現在状態は実行計画、Architecture Validation indexおよびGit履歴を優先します。

## 現在の入口

- [アプリ開発チーム向け引継ぎガイド](application-team-handoff-guide.md): Phase履歴を知らない開発者が、提供物／非提供物、構成・Starter選択、Reference実行、最初のCustomer-owned module、copy禁止、拡張分類、診断およびFramework側への戻し条件を一本道で確認する第一入口
- [開発環境構築手順](application-team-development-environment-guide.md): 端末前提、社内Proxy証明書、Framework / Customer別Repositoryの配置、KOIKI Parent接続、R2 isolated stage build、ローカル起動までの共通手順。VS Codeを採用する場合の設定例と、Dev Container / Linux環境で想定される事項の付記を含む
- [アプリケーション開発者ガイド（草案）](application-developer-guide.md): 初回build後に業務機能を1つ完成させるための、package構成、`@KoikiModule`、migration、Use Case / Controller / eventの置き方、test、詰まったときの相談方法
- [Architecture Rules説明（草案）](architecture-rules-developer-guide.md): Customer projectへのArchitecture Rules testの組み込み方、違反messageの読み方、`KOIKI-ARCH-nnn`全25件の違反例と直し方、検出しない範囲
- [P4-AR6 業務アプリチーム向け説明・対話資料](p4-ar6-application-team-briefing-20260928.md): Frameworkの現実装、frontendとSecurityの案件責任、VS Code / Maven構成案、外部project試行の証拠を9月28日の対話順に整理
- [UI / Authentication Profile Selection Guide](frontend-authentication-profile-guide.md): MVC単一JAR、same-origin React、Next.js BFF、direct Token SPAおよびALB edge認証のUI / Session / Token / SSO責任選択
- [Phase 2 Developer Journey](phase2-developer-journey.md): Security Starter、認証profile、Ownership、診断および検証入口の詳細
- [P4-AR6 実チーム受入worksheet](p4-ar6-actual-team-reception-worksheet.md): 実行前承認、環境、journey、Repository topology、Customer-owned module、責任分担、findingおよびcleanupを非機密情報だけで記録する様式
- [P4-PL1 REST利用境界差分台帳](KOIKI-JavaWeb-FW_Phase4_PL1_REST利用境界差分台帳_v0.1.md): Framework契約とReference例の区別、未取得の実案件入力、P4-AR6打ち合わせ用の確認票
- [Pre-Phase 4 Adoption Readiness計画](KOIKI-JavaWeb-FW_Pre-Phase4_Adoption_Readiness計画_v0.1.md): Framework、Consumer、Reference、開発環境およびDeveloper Handoffを実案件連携前に確認するtransition Gateの正本

共有source baselineのtag名と現在状態は[Repository Top README](../../README.md)を正本とします。source milestoneは
正式Maven release、実チーム受入完了、P4-AR7、Gate P4-ARまたはPhase 4開始を意味しません。

## Phase 4計画作業（途中記録）

2026年9月30日時点のFramework側の現在地と、実案件チームの回答待ちと並行して進める作業の順序案は
[Pre-Phase 4 Framework側の現在地と作業順序案](pre-phase4-framework-independent-work-review-20260930.md)（2026-09-30 OWNER APPROVED）に
まとめています。下記「次の着手順（2026年9月29日時点）」の表との対応は同文書§5にあります。

2026年9月26〜29日にPhase 4の計画見直しと非配布検証を行った途中記録です。Architecture Ownerが承認したのは
P4-PL1 / PL2の調査・設計までで、Gate P4-F、Phase 4 production開始、DoD変更、正式配布は未承認です。
Phase 4の入口は、[説明会の実施結果](p4-ar6-application-team-briefing-20260928.md#8-実施結果2026年9月28日)を受けて再整理する予定です。

- [Phase 4実施計画 見直し草案](KOIKI-JavaWeb-FW_Phase4実施計画_見直し草案_v0.1.md): 当初成果物・DoD 4-1〜4-12の棚卸し、work package案、Owner判断R1〜R7の記録
- [P4-F限定開始Gate提案](KOIKI-JavaWeb-FW_Phase4_P4-F限定開始Gate提案_v0.1.md): P4-AR6を待たずにFramework範囲へ限定して開始するGateの提案（DRAFT）
- [PL2 P4-F判定資料](KOIKI-JavaWeb-FW_Phase4_PL2_P4-F判定資料_v0.1.md): F-1〜F-5、Level 2採用規模S0 / S1 / S2の比較、暫定基準S0、次のPL2作業
- [PL2 F-5全件台帳・Gate改訂差分案](phase4-pl2-f5-integration-and-gate-delta-draft.md): P4-01〜11の採否・DoD・待ち条件、CP-F0の判断材料
- [Level 1 / Level 2 業務向け説明資料](phase4-pl2-level1-level2-business-guide.md): 業務要件からLevel 2の要否を判断するための説明（DRAFT）
- [event publication復旧runbook案](phase4-pl2-publication-recovery-runbook-draft.md): 非配布検証に基づく復旧手順案（productionの手順ではない）
- [PL2 V4開始引継ぎ](phase4-pl2-v4-start-handoff-20260927.md): 2026年9月27日時点の作業再開記録

### 次の着手順（2026年9月29日時点）

> **2026-09-30 移行済み:** この表の作業と順序は、Owner承認済みの
> [Pre-Phase 4 Framework側の現在地と作業順序案](pre-phase4-framework-independent-work-review-20260930.md)へ移行しました。
> 現在の作業順、待ち事項、Phase 4完遂までの見通しは同文書を正本とします。各行の移行先は同文書§5の対応表にあります。
> 以下の表は2026年9月29日時点の記録として残します。

Phase 4の次の作業は、実案件の開始（2026年11月予定）に合わせる**採用支援**と、Grand Design §27.8に基づく
**Phase 4本体**の2本に分けて進めます。次の表を着手順の入口とし、詳細は各リンク先を正本とします。
順序と各作業の採否は、1の入口再整理でOwnerが確定します。

| 順 | トラック | 作業 | 対応するID・正本 | 期限・判断者 |
|---|---|---|---|---|
| 1 | Phase 4入口 | 入口の再整理。2本のトラック構成とこの表を確定し、認証の終端に関する設計観点を認証profileガイドへ反映する | [見直し草案§8.5](KOIKI-JavaWeb-FW_Phase4実施計画_見直し草案_v0.1.md#85-新しい入口の作業で行うこと)、P4-PL3 | Owner |
| 2 | 採用支援 | ArchUnitの構造ルールを日本語で文書化する | [説明会アクション](p4-ar6-application-team-briefing-20260928.md#83-アクション)。既存の作業パッケージに属さない（IDは1で決める） | 11月開始前 |
| 3 | 採用支援 | 顧客へ認証の終端とIdPの方式を確認し、認証の詳細設計を相談する | [PL1-Q6](KOIKI-JavaWeb-FW_Phase4_PL1_REST利用境界差分台帳_v0.1.md#3-p4-ar6へ渡す確認事項)、P4-04、P4-EDGE | 11月開始前 |
| 4 | 採用支援 | Linux（WSL）上でR2 buildを実証する | [開発環境構築手順](application-team-development-environment-guide.md)のLinux付記、P4-AR6 | 11月開始前 |
| 5 | 採用支援 | 依存関係の制御（`pom.xml`への追加によるFramework制約の回避）の方針を決める | [説明会の未確定事項](p4-ar6-application-team-briefing-20260928.md#82-未確定事項とリスク)。既存の作業パッケージに属さない（IDは1で決める） | 継続検討 |
| 6 | Phase 4本体 | CP-F0：Level 2の要否とDoD 4-1〜4-5・4-12の扱いを判断する（暫定基準はS0 = Level 1維持） | P4-01、[判定資料§1.1](KOIKI-JavaWeb-FW_Phase4_PL2_P4-F判定資料_v0.1.md#11-level-2の採用規模を先に選ぶ)、[F-5§3.1](phase4-pl2-f5-integration-and-gate-delta-draft.md#31-cp-f0では業務上の必要性を先に判定する) | Owner |
| 7 | Phase 4本体 | CP-F0の結果に応じて、F-4とP4-Fの採否へ進むか、S0として残りのpackageを再計画する | [判定資料§3〜5](KOIKI-JavaWeb-FW_Phase4_PL2_P4-F判定資料_v0.1.md#5-次のpl2作業) | Owner |

P4-AR6の実チーム受入（build / run再現）、AR-D10、Gate P4-ARは未完了のままです。この表の作業は、
これらの完了やPhase 4 production開始を意味しません。

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
