# S1計画方針承認後の次回作業引継ぎ（2026-10-02）

> **現在の参照先（2026-10-05）:** 本書は10月2日時点の引継ぎ履歴。現在は作業7の方式具体化を経て初回ST-Bの作成・実行が承認済み。再開は最新引継ぎから行い、本書の未承認表示・次作業を現在へ転用しない。 [最新引継ぎ](phase4-s1-local-verification-start-handoff-20261005.md)／[承認済み方式票](phase4-s1-method-ballot-and-local-verification-contract-20261005.md)。

**目的:** S1検証完遂を目指す計画方向の承認と文書reviewを区切りに、次回は作業7の具体化から再開する。
**状態:** 引継ぎ記録。新しい実装開始・Gate承認ではない。
**作業位置:** `docs/daily-development-workflow`。作成時HEADは`dbee2bf`、今回の文書差分はcommit前。次回はOwnerが行うcommit後のHEADと作業treeを再確認する。

## 1. 再開時に維持する決定

- **S1検証完遂を目指す進め方を計画上の候補とすることはDECIDED / OWNER APPROVED。** S0 / S1 / S2を全て同じ未決状態へ戻さない。
- [S1計画方針・案件協働とnon-Web境界の決定記録](phase4-s1-completion-direction-decision-20261002.md)は、2026-10-02にOwnerが確認し内容承認した。承認範囲は同書§1・§8を参照する。
- 案件判断の時期に連動させず、Framework側で成立する範囲を進める。案件との協調・FAQ対応を継続する。
- S1の適用条件を土台として設計初期から案件と適合確認し、S2の共通拡張が必要な場合は差分・担当・Evidence・費用 / 納期影響を示して別判断する。
- Batch本体はnon-Web専用実行へ`koiki-batch-fw`で構築した処理を組み込む想定。Web内組込みは可能だが非推奨の例外候補。Batch整備へS1の中心目的を置き換えない。
- S0はS1成立が難しい場合の再計画材料として保持するが、自動切替しない。

## 2. 最初に読む入力

1. Repositoryの`AGENTS.md`と`docs/agent/skills/koiki-project-overview/SKILL.md`で現在のPhase・Ownership・承認境界を確認する。
2. [決定記録](phase4-s1-completion-direction-decision-20261002.md)：今回の承認・経緯・検証軸・残る判断。
3. [CP-F0判断材料](phase4-cpf0-and-s0-replan-options-20261002.md)：S1成立条件、S0時のDoD・accounting / Batch再計画。詳細案はDRAFT。
4. [入口・先行範囲案](phase4-entry-and-forward-scope-options-20261002.md)：全packageの必要入力、先行Gateの比較。先行範囲・Gate改訂は未承認。
5. [作業順序案§11・§15](pre-phase4-framework-independent-work-review-20260930.md#15-s1検証完遂を目指す候補化判断と対話記録2026-10-02)：作業状態と後続順序。
6. [PL2判定資料](KOIKI-JavaWeb-FW_Phase4_PL2_P4-F判定資料_v0.1.md)、[PL2 Evidence](../architecture/validation/phase4-pl2-level2-verification.md)、[復旧runbook案](phase4-pl2-publication-recovery-runbook-draft.md)：技術材料・残課題・既存見積。過去の暫定S0は時点記録として読む。

## 3. 次の中心作業：作業7の具体化

最初にS1のA1 / A2 / D1を今回の限定範囲へ照合し、作業単位ごとに次を揃える。既存概算を今回の予算やAI支援時の所要時間へ自動転用せず、未取得は未取得とする。

| 計画項目 | 具体化する入力・出口 |
|---|---|
| 対象と成果物 | Reference event・模擬provider、Framework / Reference / ToolingのOwnership、schema・依存・APIのreview論点 |
| 安全性・復旧 | 停止確認の真正性、通常listenerとの競合、lock喪失の窓、provider受理不明時の照合、冪等性、再送認可 / Audit |
| Evidence | DoD 4-1〜4-5・4-12の正常 / 負例、process停止・再送・観測・パージ、正式ReferenceとToolingの区別 |
| 実施可能性 | 実施 / review / 復旧担当、検証環境・cleanup、作業分解・概算と上限、停止点・残課題 |
| 開始経路 | CP-F0残項目、先行Gateの対象・条件、A1 / A2等のblocking reviewと個別承認点 |

次に独立packageを同じ様式で分解し、作業4bの先行対象・順序・Gate差分と合わせてOR材料を揃える。全件を一括開始しない。

C3 / C4は関連するnon-Web / Batch連携検証として整理する。外部ファイル受付・転送完了・重複判定、Batch起動・停止・再実行、業務確定・結果反映、処理主体・Audit・相関、transaction時間とDB接続保持を確認項目へ入れる。別processへの受渡しをアプリ内Domain Eventと同一視しない。

`koiki-batch-fw`は`C:\KOIKI\koiki-java\koiki-batch-fw`にclone済み（別端末では同じパスとは限らない）。点検時HEADは`c999298`。H2インメモリ、排他guard、rerun型ファイル処理、監査永続化等の限界は決定記録§6を参照する。再点検時は同Repositoryの`AGENTS.md`とHEADを確認し、点検材料を現在の実装へ照合する。正式依存・本番適用は未決定。

## 4. 未承認・未完了の境界

S1の具体的採用・実行条件、担当・上限・環境、CP-F0全項目、案2の正式採用、Gate P4-F設置 / 通過、blocking review、Phase 4 production開始は未承認。S1未完了、作業7未完了、OR全体未実施である。
DoD変更、MyBatis adoption、正式Rules / API / migration / dependency、Batch正式依存、CI変更、release・remote操作も今回の承認には含めない。既存R1〜R7、F-5、Gate規定・Agent guidanceは後続の正本改訂作業で整合する。

依存関係制御はA案を保持して検証作業保留。日常開発方式は方式採用・Tooling実装の別判断待ち。認証方式・実チーム受入・Linux / WSL等の未取得入力は[作業一覧](pre-phase4-framework-independent-work-review-20260930.md)で並行管理する。

## 5. 今回のcommit対象と次回の照合

今回の区切りは文書6ファイル：本書、決定記録、CP-F0判断材料、入口・先行範囲案、作業順序案、Development READMEである。決定記録は内容承認済み、関連比較・詳細案は承認範囲を越えて採用済みとしない。

作成時点で差分・ローカル文書リンクを確認した。production code変更、Batch Repository変更、新規Maven / Docker検証、remote操作は行っていない。commitはOwnerが実施するため、未確定のcommit SHAを記載しない。

次回はbranch・HEAD・`git status`と文書6ファイルの反映を確認し、既存の記録を再作成せず作業7へ進む。
