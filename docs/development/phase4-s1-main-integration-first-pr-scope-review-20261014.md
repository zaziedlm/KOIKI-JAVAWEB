# Phase 4 S1：main統合計画と最初のPR範囲の点検案（2026-10-14）

状態：**LOCAL REVIEW DRAFT / OWNER INTEGRATION DECISION PENDING**。Architecture Ownerは、R3-Bを待たずに統合計画と最初のPR範囲の点検を始める方針を採用した。本書はローカルGit履歴に基づく判断入力であり、push・PR作成・merge・CI変更・R3-B開始の承認ではない。

## 1. 点検基点と統合の意味

2026-10-14のローカル点検開始時、`main` / `origin/main`のローカル参照は`217cd0c`、現branch `feature/phase4-s1-reference-full-stop-recovery-r3a`は`c8d406b`、working treeはcleanだった。本書作成後は本書が未追跡差分である。`main`は現branchの祖先で、差分は52 commit。続くread-only `git ls-remote --heads`でremote `main`も`217cd0c`と一致し、当初R3-A branchのremote参照は存在しなかった。Ownerの個別承認後に成果コミット`c8d406b713fed97269ca5843ecf31b79824593ad`を新規remote branch `feature/phase4-s1-reference-full-stop-recovery-r3a`へ非forceでpushし、`git ls-remote`の参照hash一致を確認した。本書の未追跡差分はpushに含めていない。

R3断面までの累積sourceをmainへ統合すると、R1のReference-owned opt-in非同期通知実装、R2失敗分離、R3-Aの隔離・単発全停止復旧入口も取り込まれる。ただし、経費承認自体は同期処理で、承認とAudit／publication保存は同一transaction、通知はcommit後の非同期処理という境界を維持する。通常構成では非同期経路は無効。mainへの取り込みは、実provider配信、一般運用の復旧保証、R4の認可・Audit／permit接続、R5、DoD全体、正式受渡し、Phase 4全体の受入を意味しない。

R3-BはE07通知log保存境界、E09のkey異常、E10の応答喪失・照会不能、E08残枝について別票で目的・契約・最小検証を判断する。R3-B成果が受入済みになればR3統合断面の候補となるが、その完了を最初のPR範囲の点検条件にはしない。

## 2. 履歴上の自然な境界

| 基点からの出口候補 | 累積commit数 | 役割 | 現時点の扱い |
|---|---:|---|---|
| `dbee2bf` | 5 | Phase 4準備に先立つ開発ガイド・依存管理・認証profile等の文書 | 最初のPR候補。`main..dbee2bf`は9 file、876 insertions／11 deletions、`docs/development/`のみ |
| `be4bce1` | 9 | S1の目的・方式・Gate等の計画文書 | 次の文書PR候補。`dbee2bf..be4bce1`は4 commit、26 file、2,658 insertions／6 deletions |
| `f5e2672` | 13 | 追加局所検証A／B／CのToolingとEvidence | 独立したTooling検証PR候補。`be4bce1..f5e2672`の検証差分・通常buildへの影響を別審査 |
| `5f31d41` | 44 | S1 Reference基盤、B1／B2、通常build対策、R0規約と受入文書 | さらに受入済み段階で分割する。単一PRを前提にしない |
| `18cb8b6` | 47 | R1正常非同期経路・受入／引継ぎ | R1のPR候補境界 |
| `66d9b8f` | 49 | R2失敗分離・受入 | R2のPR候補境界 |
| `c8d406b` | 52 | R3-A全停止型の限定結果・受入 | R3-AのPR候補境界。R3-Bの扱いは成果と差分確定後に判断 |

この表はレビュー順序の候補であり、後続PRの確定分割ではない。特に`f5e2672..5f31d41`は31 commitに及び、Reference基盤、B1／B2、R0の受入境界と各差分を追加点検してから切る。前段PRのmergeを待って後段PRのbaseを更新する方法と、未mergeの前段branchをbaseにしたstacked PRの方法を比較する。mainへの最終merge順は依存順を守る。個々のPRで文書の「未開始」「未受入」が記録時点の履歴か現行状態かを明示する。

## 3. 最初のPR候補

第一候補は、**base `main`／head `dbee2bf`相当の専用integration branch**で、先頭5 commitの文書9 fileだけを扱うPRとする。remote `main`の参照一致をread-onlyで確認した。`gh pr list --state all`では既存PRは#42までで、本候補と同じheadのPRはなかった。現R3-A branchをそのままmain向けの最初のPRにすると52 commitが全て差分へ入るため、この候補のheadにはしない。既存の作業branchをrewind／force-pushせず、履歴上のcommitから別branchを作る方法を採る。

目的は、開発ガイド、日々の開発手順、依存管理判断、frontend認証profileとPre-Phase 4検討の記録をmainへ取り込むこと。Reference非同期実装・migration・Tooling検証は含めない。`git diff --check main..dbee2bf`は成立し、差分は文書9 fileのみ。候補headの9 fileにある相対Markdown linkのtargetを同headのGit treeで確認し、欠落0件だった。PR作成前に状態表記、secretや環境依存path、main最新状態との整合を再点検する。文書のみの候補なので新しいMaven／Docker検証は原則要求せず、PR上の既存required checksとreview結果を確認する。CI失敗や新しいmain差分があれば原因を分類して別判断する。

候補としては小さいが、それでも文書差分は約865 net linesある。文書9 fileを一度にreviewできるかをOwnerが確認し、難しければ先頭commitからさらに分割する。`main..dbee2bf`の5 commitは時系列と依存を保ったまま扱い、後続内容を先取りして混ぜない。

## 4. 次の判断順

1. R3-A成果branchのremote保全と参照hash一致はOwner個別承認後に完了。commit済み成果に対する追加差分はなく、統合計画・R3-B契約／開始票の文書だけが未追跡で残る。tmpのJAR／XML／logはGitでは移送されない。
2. [R3-B契約レビュー](phase4-s1-r3b-purpose-log-provider-unknown-owner-review-20261014.md)の第一候補は設計基線としてOwner採用済み。[R3-B最小開始票案](phase4-s1-r3b-contract-and-minimal-start-owner-review-20261014.md)のblocking条件・数値は引き続きOwner review対象。実装開始はその票の判断後とする。
3. 読取可能なmain最新参照、既存PR／ruleset／required checksを確認し、最初のPRのbase・head・9 fileを確定する。この段階ではPR作成やworkflow dispatchを行わない。
4. 最初のPR作成・mergeを各々のOwner個別判断で進める。後続は受入済み断面ごとに差分・CI・通常構成保護を確認し、R3-B完了時にR3断面の最終統合範囲を再判定する。

本書の点検ではremote `main`参照・既存PR一覧をread-only確認した。R3-A成果branchのpushは別のOwner個別承認により実行・参照hashを照合した。統合用branch作成、最初のPR、merge、追加検証は行っていない。
