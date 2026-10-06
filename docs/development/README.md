# Development

このindexは、現在の開発者入口と、完了済みPhaseの履歴文書を分けて案内します。個別handoff文書は
作成時点の状態を保存する履歴であり、現在状態は実行計画、Architecture Validation indexおよびGit履歴を優先します。

## 現在の入口

- [Frameworkローカル開発のartifact準備・整備検討案](framework-local-artifact-readiness-options-20261006.md): 共通／用途別資材、取得・local install・offline確認、cache更新／復旧の検討案。選択Toolingの不足解消とoffline再開は最小実証済み。恒常方式の実装・採用は未完了
- [アプリ開発チーム向け引継ぎガイド](application-team-handoff-guide.md): Phase履歴を知らない開発者が、提供物／非提供物、構成・Starter選択、Reference実行、最初のCustomer-owned module、copy禁止、拡張分類、診断およびFramework側への戻し条件を一本道で確認する第一入口
- [開発環境構築手順](application-team-development-environment-guide.md): 端末前提、社内Proxy証明書、Framework / Customer別Repositoryの配置、KOIKI Parent接続、R2 isolated stage build、ローカル起動までの共通手順。VS Codeを採用する場合の設定例と、Dev Container / Linux環境で想定される事項の付記を含む
- [アプリケーション開発者ガイド](application-developer-guide.md): 初回build後に業務機能を1つ完成させるための、package構成、`@KoikiModule`、migration、Use Case / Controller / eventの置き方、test、詰まったときの相談方法とFAQ。2026-09-30 Owner review完了
- [Architecture Rules説明](architecture-rules-developer-guide.md): Customer projectへのArchitecture Rules testの組み込み方、違反messageの読み方、`KOIKI-ARCH-nnn`全25件の違反例と直し方、検出しない範囲。2026-09-30 Owner review完了
- [P4-AR6 業務アプリチーム向け説明・対話資料](p4-ar6-application-team-briefing-20260928.md): Frameworkの現実装、frontendとSecurityの案件責任、VS Code / Maven構成案、外部project試行の証拠を9月28日の対話順に整理
- [UI / Authentication Profile Selection Guide](frontend-authentication-profile-guide.md): MVC単一JAR、same-origin React、Next.js BFF、direct Token SPAおよびALB edge認証のUI / Session / Token / SSO責任選択
- [Phase 2 Developer Journey](phase2-developer-journey.md): Security Starter、認証profile、Ownership、診断および検証入口の詳細
- [P4-AR6 実チーム受入worksheet](p4-ar6-actual-team-reception-worksheet.md): 実行前承認、環境、journey、Repository topology、Customer-owned module、責任分担、findingおよびcleanupを非機密情報だけで記録する様式
- [P4-PL1 REST利用境界差分台帳](KOIKI-JavaWeb-FW_Phase4_PL1_REST利用境界差分台帳_v0.1.md): Framework契約とReference例の区別、未取得の実案件入力、P4-AR6打ち合わせ用の確認票
- [Pre-Phase 4 Adoption Readiness計画](KOIKI-JavaWeb-FW_Pre-Phase4_Adoption_Readiness計画_v0.1.md): Framework、Consumer、Reference、開発環境およびDeveloper Handoffを実案件連携前に確認するtransition Gateの正本

共有source baselineのtag名と現在状態は[Repository Top README](../../README.md)を正本とします。source milestoneは
正式Maven release、実チーム受入完了、P4-AR7、Gate P4-ARまたはPhase 4開始を意味しません。

## Phase 4計画作業（途中記録）

**引継ぎの入口（2026-10-06）:** [追加局所検証A・引継ぎ](phase4-s1-additional-a-next-session-handoff-20261005.md)、[狭いJPA共有モデル候補の継続検討](phase4-s1-jpa-candidate-continuation-after-a-20261006.md)。A／B受入とCの開始承認・実行結果を反映済み。

- [追加局所検証B・専用開始票](phase4-s1-additional-b-start-review-20261006.md): 候補文書確認後のB進行指示を受け、test限定依存・Spring transaction・認可／actor／失効・schema／grant・31件案と回帰／上限を具体化。作成／実行の具体条件review用

**追加検証の再開入口（2026-10-06）:** A18件・B31件・[C38件＋同profile回帰89件はすべてCOMPLETE / OWNER APPROVED](../architecture/validation/phase4-s1-additional-c-web-mode-20261006.md#6-owner受入承認2026-10-06)。次はA／B／Cを入力として狭いJPA共有モデル候補の責務・正式接続条件を整理する。正式Tier／Reference／DoD／Gateは未採用。

**接続条件の具体化案（2026-10-05）:** [S1責務・認可／失効・Audit・mode／DB権限案](phase4-s1-responsibility-authorization-audit-mode-draft-20261005.md)。承認済み方針をもとに文書案を作成。観察と確認終了、通知読取とSession／Auditの必要更新、権限失効窓・不明保全を分けて整理。具体案はreview待ち、追加検証・正式実装は未開始。

**S1の現在の再開入口（2026-10-05）:** [初回局所検証後の後続タスク](phase4-s1-follow-up-tasks-after-local-verification-20261005.md)。commit `5252124`にpreflight / 初回L1〜4のLOCAL PASS（11 / 9 / 8 / 12 tests）と方式票への結果反映を記録済み。Tier・認可・Audit・正式Reference接続の4項目の候補継続・残条件具体化はOwner承認済み（[方式票§8.3](phase4-s1-method-ballot-and-local-verification-contract-20261005.md#83-owner方針レビュー承認記録2026-10-05)）。次は接続条件の具体化、その後に追加検証契約へ進みます。正式実装・Tier確定・DoD / Gateは後続判断です。初回の経緯は[開始引継ぎ](phase4-s1-local-verification-start-handoff-20261005.md)を参照してください。以下の一覧は検討順の履歴を含みます。

2026-10-02の今後の作業は[作業順序案§11](pre-phase4-framework-independent-work-review-20260930.md#11-作業3の区切りと今後の作業一覧2026-10-02)に一覧化しています。作業4aは[認証profileガイド](frontend-authentication-profile-guide.md)への文書反映 COMPLETE / OWNER APPROVED（2026-10-02、[承認範囲](pre-phase4-framework-independent-work-review-20260930.md#12-作業4aの文書反映とreview範囲2026-10-02)）。当時の次の計画整理は作業4b・5・7でした。現在の局所検証開始範囲は上記引継ぎを参照してください。依存関係制御の検証は保留し、日常開発方式の実装は別判断待ちです。

- [作業2：日常開発の反復方式の比較・推奨案](daily-development-workflow-options-20261001.md): IDEとCLIの依存解決、日常用stageの所有権・更新・破棄、R2との使い分けを整理（2026-10-01 文書承認済み。方式採用・Tooling実装は別承認）
- [作業4b：Phase 4入口・Framework先行範囲の整理案](phase4-entry-and-forward-scope-options-20261002.md): 全packageの必要入力・Owner候補・DoD、現行Gate維持と先行範囲再定義の比較、作業5・7へ渡す材料（DRAFT。分類・先行範囲・開始は未承認）
- [S1計画方針・案件協働とnon-Web境界の決定記録](phase4-s1-completion-direction-decision-20261002.md): S1検証完遂を目指す進め方の候補化はDECIDED / OWNER APPROVED（2026-10-02）。対話の経緯、承認範囲、S2別判断と並走協働、non-Web推奨 / Web同居非推奨、Batch点検材料と作業7への入力。具体的実行・Gate / 開始は別判断
- [S1方針承認後の次回作業引継ぎ](phase4-s1-next-session-handoff-20261002.md): 上記決定記録の内容はOwner承認済み。10月2日時点の履歴。現在の再開は上記の10月5日引継ぎを参照
- [作業7：A1 / A2 / D1とS1限定範囲の照合](phase4-s1-a1-a2-d1-scope-mapping-20261005.md): S1の対象・成果物Owner、現行Reference / Rules / Observabilityとの差分、DoDとschema / 依存 / APIのreview論点（DRAFT。安全方式・担当 / 環境 / 上限・実行開始は未確定）
- [作業7：S1安全性・復旧条件の具体化](phase4-s1-safety-and-recovery-conditions-20261005.md): 停止確認、通常listener / 複数復旧者の競合、lock喪失窓、通知識別・provider受理不明、認可 / Audit、観測・パージの条件と必要Evidence（DRAFT / blocking review入力。方式採用・実装開始は未承認）
- [作業7：S1 DoD実演計画・人による確認判断](phase4-s1-dod-demonstration-plan-20261005.md): 実運用担当者の総合判断を前提とする21 case、DoD / S-01〜11対応、証拠とPASS / FAIL / BLOCKED条件、保留・調査・エスカレーションの記録（DRAFT。実演・DoD判定は未実施）
- [作業7：S1担当・検証環境・作業量／上限・実施順案](phase4-s1-execution-resources-and-sequence-draft-20261005.md): 責務別担当案、ローカル隔離環境、残作業10単位の低確度概算・上限案、段階出口とOPEN入力（元WBSの履歴。Owner一人＋Codexの役割合意済み。初回局所上限・開始は方式票を参照）
- [作業7：S1以外のpackage分解・比較材料](phase4-other-packages-breakdown-draft-20261005.md): B1 / C2 / C3 / C4 / D2 / D3 / D1同期、trigger・顧客入力待ち全件の対象・review・実演・低確度概算と共有費 / 未算定範囲（DRAFT。先行対象・Gate・実装は未承認）
- [作業4b・5・7：先行範囲／CP-F0統合判断材料](phase4-forward-scope-cpf0-integrated-review-draft-20261005.md): S1中心の初回限定案、独立候補・先行順序、Gate経路比較、共通費 / 未取得とOR論点を統合（DRAFT。採用・上限・開始経路・OR完了は未承認）
- [初回S1対象・Gate経路・残判断の具体化案](phase4-initial-scope-gate-and-open-decisions-draft-20261005.md): 初回S1限定・既存Level 2限定P4-F経路の推奨案、前置 / 後続reviewの整合、残判断J1〜8と次の詳細設計材料（DRAFT。対象・経路採用・Gateは未承認）
- [S1 W01 / W02：保存・配置・規約と復旧安全性のsource照合](phase4-s1-w01-w02-source-design-review-draft-20261005.md): Data Starterとkkref履歴・最小依存 / Rule互換、停止 / key・provider確定順、対象publicationの当該試行と誤終端の追加論点（DRAFT。read-only調査、方式・実装は未承認）
- [S1方式候補・DoD解釈・運用／Audit・観測条件案](phase4-s1-method-dod-operations-observation-draft-20261005.md): 限定環境停止方式、復旧時間の測定、人の判断、Audit分類 / actor・操作台帳・記録失敗、別process trace・alert / 保持の条件（DRAFT。方式・数値・実装は未承認）
- [S1非HTTP復旧主体・専用process・実観測構成review案](phase4-s1-nonhttp-process-observation-review-draft-20261005.md): Servlet条件 / 認証source protectionの制約、認証済み要求から非Web復旧する候補、単発process・tracing / exporter・隔離sinkと残判断（DRAFT。方式採用・依存・実装は未承認）
- [S1実装前review票・環境preflight案・再見積差分](phase4-s1-preimplementation-review-preflight-estimate-draft-20261005.md): IdentityQueryによる現在user / permission再確認の接続、RV-1〜7、PF-1〜6、要求 / nonWeb / 観測のWBS差分（DRAFT。票未判定・preflight未実施・新総額未算定）
- [S1方式候補・復旧要求の保存構造・依存構成・検証環境上限案](phase4-s1-request-storage-dependencies-environment-draft-20261005.md): 要求 / 試行 / 実行権 / 判断履歴、JPA・JDBC publication・OTLP候補、環境数値仮値と作業量差分（DRAFT。方式・数値未採用、実測 / 実装未実施）
- [S1実証の最小構成と将来の運用管理機能の分離](phase4-s1-minimum-demonstration-scope-review-20261005.md): 分離方針・最小構成後のTier判断はOwner了解。必須安全能力と将来管理機能を再整理し、前段のTier 2 / 台帳一式・追加見積を採用前提から外す（具体方式・実装は未決定）
- [S1最小方式：non-Web許可引渡し・対象試行識別案](phase4-s1-minimum-permission-and-attempt-review-20261005.md): 1件の許可recordを優先比較、Modulith 2.1.1公開APIの署名確認、publication / operation / 通知keyの識別と不明時保全（DRAFT。DB保護・実相関・起動条件とTierは未判定）
- [S1最小方式：許可DB保護・executor相関・結果確認起動条件](phase4-s1-permission-db-executor-startup-review-20261005.md): role / 列権限と巻戻し防止の区別、requestId限定decoratorへの追加相関、送信機能を持たない確認Webとmigration分離（DRAFT。保護方式・実executor・mode実動は未判定）
- [S1最小方式：保護方式・相関経路・起動境界の成立性確認](phase4-s1-minimum-boundary-feasibility-review-20261005.md): 許可＋append-only消費記録を優先比較、標準再送overload / executor経路のbytecode照合、Reference Web Security・scan境界の不足（DRAFT。実動PASS・方式 / Tier採用は未判定）
- [S1最小候補：Tier・保存／権限・検証手順の整理](phase4-s1-minimum-tier-storage-verification-review-20261005.md): 残る不変条件から狭いRICHを優先review、2種類recordとrow lockの修正権限、V0〜7とE01〜21対応（DRAFT。Tier / schema / 開始未採用、全段階未実施）
- [S1最小候補：採用候補・残条件・変更一覧と個別開始範囲](phase4-s1-minimum-change-and-start-scope-draft-20261005.md): K1〜6・CH-01〜08・ST-A〜Eへ統合。Tooling局所検証と正式実装 / Gateを分けた判定材料（正式方式は未採用。初回局所作成・実行は承認済み、L1〜4 LOCAL PASS）
- [S1方式採用票・初回局所検証契約案](phase4-s1-method-ballot-and-local-verification-contract-20261005.md): B1〜6、予定4 testの枝 / 合格条件、順次実行commandと局所資源 / 上限。局所候補と初回fixture作成・実行はOWNER APPROVED（2026-10-05）。Tier正式確定・Web認証・実trace / OS crash等は後続。初回L1〜4作成・11 / 9 / 8 / 12 tests成功。正式方式／Tier・Reference接続は後続review
- [S1初回局所検証Evidence](../architecture/validation/phase4-s1-minimum-local-verification-20261005.md): preflight、L1 DB保護・単発消費・原子的記録、L2競合・結果不明、L3実executor相関・対象選別、L4mode組立・起動時副作用のLOCAL PASS。11 / 9 / 8 / 12 tests、各failures / errors / skipped 0。正式認証 / Audit・実trace・DoD / Gateは未検証
- [S1局所結果の方式票反映・正式接続の残条件review](phase4-s1-method-ballot-and-local-verification-contract-20261005.md#7-局所結果の方式票への反映2026-10-05): B1〜6へ結果を反映し、Tier・認可・Audit・Reference起動境界の4論点と次の具体化順を整理。結果不明の再起動引継ぎ・実JPA／Recorder・実Reference接続は未検証。方針候補はOwner判定待ち
- [作業5：CP-F0判断材料・S0時の再計画案](phase4-cpf0-and-s0-replan-options-20261002.md): Framework実証と案件導入を分けたS0 / S1 / S2比較、S1成立条件、S0の未達DoD保持、accounting / Batch依存と再計画（DRAFT。S1候補化は上記承認済み。具体的採用・DoD変更・開始は未承認）
- [作業3：依存関係の制御の論点整理](dependency-control-issues-20261001.md): Parent / BOMとArchitecture Rulesの検出範囲、誤選択防止とアプリ型による構成選択の候補、作業4bへ渡す判断事項（2026-10-02 文書承認済み。方式採用・実装は未決定）
- [依存関係制御：構成宣言・検査の比較と検証方式案](dependency-control-design-options-20261002.md): A案を有力候補として保持。案の共有・認識合わせを進め、検証作業は保留する方針をOwnerが了解（2026-10-02）。最初のPOM構成・Starter選択を具体化する前段で再開。方式案はDRAFT、検証実行は未承認
- [依存関係制御：次回作業引継ぎ](dependency-control-next-session-handoff-20261002.md): 作業1〜3の経緯、別端末での再開手順、実効性のある構成選択・制約案へ深掘りする検討順序と承認境界

2026年9月30日時点のFramework側の現在地と、実案件チームの回答待ちと並行して進める作業の順序案は
[Pre-Phase 4 Framework側の現在地と作業順序案](pre-phase4-framework-independent-work-review-20260930.md)（2026-09-30 OWNER APPROVED）に
まとめています。下記「次の着手順（2026年9月29日時点）」の表との対応は同文書§5にあります。

以下は2026年9月26〜29日にPhase 4の計画見直しと非配布検証を行った途中記録です。当時の承認は
P4-PL1 / PL2の調査・設計までです。2026-10-02のS1候補化判断は上記決定記録を参照してください。Gate P4-F、Phase 4 production開始、DoD変更、正式配布は未承認です。
Phase 4の入口は、[説明会の実施結果](p4-ar6-application-team-briefing-20260928.md#8-実施結果2026年9月28日)を受けて再整理する予定です。

- [Phase 4実施計画 見直し草案](KOIKI-JavaWeb-FW_Phase4実施計画_見直し草案_v0.1.md): 当初成果物・DoD 4-1〜4-12の棚卸し、work package案、Owner判断R1〜R7の記録
- [P4-F限定開始Gate提案](KOIKI-JavaWeb-FW_Phase4_P4-F限定開始Gate提案_v0.1.md): P4-AR6を待たずにFramework範囲へ限定して開始するGateの提案（DRAFT）
- [PL2 P4-F判定資料](KOIKI-JavaWeb-FW_Phase4_PL2_P4-F判定資料_v0.1.md): F-1〜F-5、Level 2採用規模S0 / S1 / S2の比較、当時の暫定基準S0、次のPL2作業。後続のS1候補化判断は上記決定記録に分けて保存
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
