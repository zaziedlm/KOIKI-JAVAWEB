# Pre-Phase 4 P4-AR2 Framework build / quality baseline

## 1. Status and boundary

| Item | Result |
|---|---|
| Status | `COMPLETE — READY FOR P4-AR3` |
| Execution baseline | `21e6be4de3e485c376a05779c42114794ed4e504`（main source baseline） |
| Evidence commit | `f790d9cec2ead854b2bd23cd6230c781dec7b3de` |
| Execution branch | `docs/p4-ar1-environment-preflight` |
| Ownership | Tooling / Architecture Evidence |
| Production change | 0 |

本書はP4-AR2の実行結果を記録するためのEvidence枠である。P4-AR1のremote baseline確定前は、手順準備だけを行い、
P4-AR2の検証開始またはPASS判定を行わない。

検証対象はFramework build / quality baselineに限定する。検証中にtracked source変更、Public API変更、配布、
workflow変更またはPhase 4 production実装を行わない。

## 2. Execution identity

| Check | Observed result |
|---|---|
| Source commit | `f790d9cec2ead854b2bd23cd6230c781dec7b3de`。mainとの差分はEvidence文書のみ |
| Branch | `docs/p4-ar1-environment-preflight` |
| Initial worktree | clean |
| OS / architecture | Windows 11 / amd64 |
| Java build | Temurin 21.0.12.1 |
| Java runtime 21 / 25 | Temurin 21.0.12.1 / 25.0.4.1 |
| Maven Wrapper | Wrapper 3.3.4 / Maven 3.9.16 |
| Start / end | 2026-09-17 22:05:27 +09:00 / 2026-09-17 22:36:11 +09:00 |

## 3. Verification results

| Order | Verification | Result | Evidence / finding |
|---|---|---|---|
| 1 | Source identity / clean worktree | PASS | mainとの差分はEvidence文書だけ。開始時clean |
| 2 | Root Reactor `clean verify` | PASS | 16 / 16 projects SUCCESS。Reference Application 99 tests、failure / error 0 |
| 3 | Tier 1 / Tier 2 Feature Template | PASS | positive、Tier別Architecture negative、NullAway negative、restoreが成功 |
| 4 | NullAway positive / negative / restore | PASS | positive、nullable returnの期待failure、restoreが成功 |
| 5 | Public API compatibility fixtures | PASS | package-private互換、public return型変更と未承認public追加の期待failureを確認 |
| 6 | Timestamped Public API baseline comparison | PASS | PAT classic scopeは`read:packages`のみ。2 artifactのbaseline identity / inventoryがMATCHし、japicmp 0.26.1は変更なし・exit 0 |
| 7 | Runtime fixture Java 21 build | PASS | class major 65、SHA-256 `7ACCB37A2DDC3479D94C4A64A051C7644A5CC0DA275A028E1440E2CEA0983077` |
| 8 | Same-artifact Java 21 runtime | PASS | expected / actual 21、実行前後hash一致 |
| 9 | Same-artifact Java 25 runtime | PASS | expected / actual 25、Java 21 build artifactの実行前後hash一致 |
| 10 | Runtime negative guards | PASS | Java 25 build、hash改変、runtime major不一致をすべて期待failureとして検出し、positive path復元 |
| 11 | Final source / resource hygiene | PASS | container / 対象port / fixture temp残存なし。Tooling所有のignored生成物を削除し、tracked差分は本Evidenceとindex更新だけ |

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
| AR2-F1 | Execution precondition（resolved） | Public API baseline用credentialが開始時未供給 | Ownerがsecure promptで`read:packages`だけのPAT classicを供給し、token値を記録せず比較をPASS |
| AR2-F2 | Environment / Developer Journey | Root Reactor初回実行時、Docker daemon停止によりTestcontainersが環境を検出できず失敗 | Rancher Desktop起動後に同一commandを再実行し、16 / 16 projectsをPASS。事前条件としてDocker daemon稼働確認が必要 |
| AR2-F3 | Evidence execution context | Runtime manifestの`workingTreeDirty`は検証中に更新した本Evidence文書だけを検出 | production source差分ではないことを`git status`で確認。最終Evidenceではtracked source変更0を別途判定 |

## 6. Public API baseline identity

| Artifact | Baseline | SHA-256 | Result |
|---|---|---|---|
| Architecture Contract | `0.1.0-20260826.091429-1` | `947EE8CF0E109FE58D81E6008A56C06C8F4C035FF76BDF462F8F6BD9BB50DE45` | MATCH |
| ArchUnit Rules | `0.1.0-20260826.091429-1` | `A51E26E7386D19E53C18BD63BC4E4F95EC1EAE471F39D519D6AE0CBC7C2DF3F2` | MATCH |

Inventoryは5 public types、4 annotation elements、2 Rules methodsでMATCHした。japicmp 0.26.1は両artifactとも
`access=public`、`modifications=NONE`、`exit=0`である。

## 7. Completion judgment

P4-AR2は、全11項目の結果、検出事項の分類、tracked source変更0、およびTooling所有resourceのcleanupを確認してから
完了判定する。一部だけのPASS、remote CIの代用、または検査不能の成功扱いは行わない。

2026-09-17、全11項目のPASS、production source変更0、Tooling所有resourceのcleanup、およびfindingsの分類を確認した。
したがってP4-AR2を`COMPLETE — READY FOR P4-AR3`と判定する。AR2-F2はP4-AR3以降もDocker前提の明示確認として
引き継ぐが、Rancher Desktop起動後の再実行がPASSしているためP4-AR3開始のblockerとはしない。
