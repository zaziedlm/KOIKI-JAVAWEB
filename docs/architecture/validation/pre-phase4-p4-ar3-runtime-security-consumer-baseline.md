# Pre-Phase 4 P4-AR3 Runtime / Security Consumer baseline

## 1. Status and boundary

| Item | Result |
|---|---|
| Status | `BLOCKED — RUNTIME HARNESS RELEASE BOUNDARY REMEDIATION REQUIRED` |
| Source commit | `78d529227437e067b6ae0a7aaef43d3b495f35f0` |
| Execution branch | `docs/p4-ar1-environment-preflight` |
| Initial worktree | clean |
| Ownership | Tooling / Architecture Evidence |
| Production change | 0 |
| Start | 2026-09-17 22:42:38 +09:00 |
| Cleanup confirmed | 2026-09-18 04:58:31 +09:00 |

P4-AR3は、正式Framework release unitを空の隔離Maven repositoryへstageし、Root Reactor外のRuntime / Security
Customer-like Consumerがpackage済みartifactだけを使用してPostgreSQL integrationを成立させることを確認する。
Root Reactorへの偶発依存またはReference混入を検出した場合は、その後の検証を成功扱いせず停止する。

## 2. Execution results

| Order | Verification | Result | Evidence / finding |
|---|---|---|---|
| 1 | Source identity / clean worktree | PASS | commit、branch、clean worktreeを開始時に確認 |
| 2 | Java 21 / 25、Docker、container / port preflight | PASS | Java環境変数設定済み、Docker client / server 29.5.3-rd、残留container・対象port 0 |
| 3 | Runtime CP1〜CP7 cumulative regression | PASS | Architecture、runtime defaults、PostgreSQL health UP / DOWN / restore、Domain Event、BOM境界が成功 |
| 4 | Runtime CP8 Customer-like Consumer | PASS — functional path only | Root外4-module Consumerのbuild / test、25 application tests、package済みJAR、PostgreSQL 17.11、同一task競合、process kill後retryが成功 |
| 5 | Runtime CP8 isolated release-unit boundary | FAIL / STOP CONDITION | Root `install`が16 projectsをstageし、非配布`koiki-reference-app`を隔離repositoryへ混入 |
| 6 | Runtime CP10 aggregate | STOPPED | CP8後のCP10独自stageでもRoot 16 projectsを選択したため、3 / 16 project実行時点で中断 |
| 7 | Current formal package static verification | NOT RUN | stop condition後は後段を成功扱いしない |
| 8 | Security Customer-like Consumer / PostgreSQL integration | NOT RUN | stop condition後は後段を成功扱いしない |
| 9 | Resource / worktree cleanup | PASS | container、対象process、対象port 0。残留CP10 temporary repositoryを安全確認後に削除。worktree clean |

## 3. Blocking finding

| ID | Classification | Finding | Impact |
|---|---|---|---|
| AR3-F1 | F1 blocking / Tooling defect | `verify-cp8-single-execution.ps1`と`verify-cp10-closeout.ps1`がRoot POMをproject除外なしで`install`する。CP10のrelease inventory assertionは必須artifactの不足だけを検査し、Referenceや後続Phase artifactの余剰を拒否しない | Runtime Consumerが正式release unitだけから解決されたというclaimが成立せず、Root Reactorへの偶発依存とReference混入を見逃す |

現行Security Toolingは、`verify-p2-c2-package-static.ps1`と`verify-p2-c2-consumer.ps1`で
`-pl !koiki-reference-app`を指定し、`p2-c2-formal-release-unit.txt`の14 projects / 11 JARとstaged coordinatesを
完全一致で検査している。したがって正式release unit自体の定義不在ではなく、Phase 1b Runtime aggregateが
現行release境界へ追随していない問題である。

## 4. Remediation and resume condition

P4-AR3を再開する前に、Runtime CP8 / CP10 Toolingを原因単位の独立変更として補正する。

1. Root全体ではなく、Referenceを除く現行正式release unitだけを隔離repositoryへstageする。
2. staged `org.koikifw` coordinatesを14 projects / 11 JARの正本と完全一致で検査し、Reference、Consumer、Tooling、
   migration fixture等の余剰artifactを拒否する。
3. CP8 / CP10のfunctional path、failure path、cleanupを再実行する。
4. 補正後のRuntime aggregateがPASSしてから、formal package staticとSecurity Consumerを順に実行する。

production artifact、Framework Public API、migration、dependency、workflowまたはPhase 4実装は変更しない。
AR3-F1解消と全後続検証PASSまでは、P4-AR3を完了またはP4-AR4 readyと判定しない。
