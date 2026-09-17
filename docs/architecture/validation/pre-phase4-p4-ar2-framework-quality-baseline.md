# Pre-Phase 4 P4-AR2 Framework build / quality baseline

## 1. Status and boundary

| Item | Result |
|---|---|
| Status | `PREPARED — NOT STARTED` |
| Execution baseline | P4-AR1完了時に記録する |
| Execution branch | P4-AR1完了後に記録する |
| Ownership | Tooling / Architecture Evidence |
| Production change | 0 |

本書はP4-AR2の実行結果を記録するためのEvidence枠である。P4-AR1のremote baseline確定前は、手順準備だけを行い、
P4-AR2の検証開始またはPASS判定を行わない。

検証対象はFramework build / quality baselineに限定する。検証中にtracked source変更、Public API変更、配布、
workflow変更またはPhase 4 production実装を行わない。

## 2. Execution identity

| Check | Observed result |
|---|---|
| Source commit | 実行時に記録 |
| Branch | 実行時に記録 |
| Initial worktree | 実行時に記録 |
| OS / architecture | 実行時に記録 |
| Java build | 実行時に記録 |
| Java runtime 21 / 25 | 実行時に記録 |
| Maven Wrapper | 実行時に記録 |
| Start / end | 実行時に記録 |

## 3. Verification results

| Order | Verification | Result | Evidence / finding |
|---|---|---|---|
| 1 | Source identity / clean worktree | NOT RUN | — |
| 2 | Root Reactor `clean verify` | NOT RUN | — |
| 3 | Tier 1 / Tier 2 Feature Template | NOT RUN | — |
| 4 | NullAway positive / negative / restore | NOT RUN | — |
| 5 | Public API compatibility fixtures | NOT RUN | — |
| 6 | Timestamped Public API baseline comparison | BLOCKED — credential not supplied | `read:packages`だけを持つPAT classicが必要 |
| 7 | Runtime fixture Java 21 build | NOT RUN | — |
| 8 | Same-artifact Java 21 runtime | NOT RUN | — |
| 9 | Same-artifact Java 25 runtime | NOT RUN | — |
| 10 | Runtime negative guards | NOT RUN | — |
| 11 | Final source / resource hygiene | NOT RUN | — |

個別検査の詳細commandとstop conditionは
[`pre-phase4-p4-ar1-environment-preflight.md`](pre-phase4-p4-ar1-environment-preflight.md) §8を正本とする。

## 4. Credential handling

Public API baseline比較前に、`read:packages`だけを持つPAT classicをsecure promptまたは対象process限定の
`KOIKI_PACKAGES_TOKEN`で供給する。token値、Maven settings実値、baseline JARおよび比較reportをRepository、
Evidenceまたはconsole commandへ記録しない。credential未供給、scope不適合またはbaseline取得不能はFAIL / BLOCKEDであり、
PASSに読み替えない。

## 5. Findings

| ID | Classification | Finding | Treatment |
|---|---|---|---|
| AR2-F1 | Execution precondition | Public API baseline用credential未供給 | 当該比較直前までにOwnerがsecureに供給 |

## 6. Completion criteria

P4-AR2は、全11項目の結果、検出事項の分類、tracked source変更0、およびTooling所有resourceのcleanupを確認してから
完了判定する。一部だけのPASS、remote CIの代用、または検査不能の成功扱いは行わない。
