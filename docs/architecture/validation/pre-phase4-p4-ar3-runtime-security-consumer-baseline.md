# Pre-Phase 4 P4-AR3 Runtime / Security Consumer baseline

## 1. Status and boundary

| Item | Result |
|---|---|
| Status | `COMPLETE — READY FOR P4-AR4` |
| Source commit | `78d529227437e067b6ae0a7aaef43d3b495f35f0` |
| Blocker record commit | `b11034c` |
| Runtime Tooling remediation commit | `5fcf7c4` |
| Execution branch | `docs/p4-ar1-environment-preflight` |
| Initial worktree | clean |
| Ownership | Tooling / Architecture Evidence |
| Production change | 0 |
| Start | 2026-09-17 22:42:38 +09:00 |
| Remediation verified | 2026-09-18 |
| Cleanup confirmed | 2026-09-18 |

P4-AR3は、正式Framework release unitを空の隔離Maven repositoryへstageし、Root Reactor外のRuntime / Security
Customer-like Consumerがpackage済みartifactだけを使用してPostgreSQL integrationを成立させることを確認する。
Root Reactorへの偶発依存またはReference混入を検出した場合は、その後の検証を成功扱いせず停止する。

## 2. Execution results

| Order | Verification | Result | Evidence / finding |
|---|---|---|---|
| 1 | Source identity / clean worktree | PASS | commit、branch、clean worktreeを開始時に確認 |
| 2 | Java 21 / 25、Docker、container / port preflight | PASS | Java環境変数設定済み、Docker client / server 29.5.3-rd、残留container・対象port 0 |
| 3 | Runtime CP1〜CP7 cumulative regression | PASS | Architecture、runtime defaults、PostgreSQL health UP / DOWN / restore、Domain Event、BOM境界が成功 |
| 4 | Runtime CP8 Customer-like Consumer | PASS | Root外4-module Consumerのbuild / test、25 application tests、package済みJAR、PostgreSQL 17.11、同一task競合、process kill後retryが成功 |
| 5 | Runtime CP8 isolated release-unit boundary | FAIL（初回）→ PASS（補正後） | 初回はRoot 16 projectsをstageし、非配布`koiki-reference-app`を隔離repositoryへ混入したため停止。補正後は現行正式15 projects / 12 JARとの完全一致とReference不在を確認 |
| 6 | Runtime CP10 aggregate | STOPPED（初回）→ PASS（補正後） | CP1〜CP8、CP10 packaged Consumerとfailure / cleanup pathが成功。CP10内CP9で残存していたReference stageも補正し、CP9 smoke単独再検証でPASS |
| 7 | Current formal package static verification | PASS | Phase 2履歴manifestを変更せず、明示的なcurrent modeで15 projects / 12 JAR、Root / BOM / staged coordinates完全一致、Reference / Tooling / Customer migration / source template非混入を確認 |
| 8 | Security Customer-like Consumer / PostgreSQL integration | PASS | Root外Consumerを隔離repositoryからbuild。Java 21 / 25、PostgreSQL 17、Framework migration 3件 / 11 tables、Customer history分離、Web認証境界、migration no-op、Session initializer拒否、secret非露出が成功 |
| 9 | Resource cleanup | PASS | container 0、対象Java process 0、P2-C2 temporary directory 0、対象port解放を独立確認 |

## 3. Blocking finding

| ID | Classification | Finding | Impact |
|---|---|---|---|
| AR3-F1 | F1 blocking / Tooling defect — REMEDIATED | Runtime CP1〜CP6 / CP8 / CP10とCP9 performance smokeのRoot stageがReferenceを除外していなかった。CP8 / CP10のrelease inventory assertionも余剰artifactを拒否していなかった | Runtime Consumerが正式release unitだけから解決されたというclaimを成立させるため、全stage境界と完全一致検査を補正 |
| AR3-F2 | F2 Developer-experience gap — REMEDIATED | Phase 2 package static検証は履歴14 projects / 11 JARだけを受け付け、Phase 3後の現行15 / 12境界を独立検証する入口がなかった | 履歴manifestと既定動作を維持したまま、`-CurrentFormalReleaseUnit`でWeb MVCを加えた現行境界を検査可能にした |

`p2-c2-formal-release-unit.txt`の14 projects / 11 JARはPhase 2時点の履歴baselineである。Phase 3で
`koiki-starter-web-mvc`が正式成果物へ追加されたため、現行正式release unitはReferenceを除く15 projects / 12 JAR、
Root全体はReferenceを含む16 projects / 13 JARである。今回の原因は正式境界の不在ではなく、Phase 1b Runtime
aggregateが現行release境界へ追随せず、履歴manifestも現行値として解釈していたことである。

## 4. Remediation result

Runtime CP8 / CP10を起点として、同じstage処理を持つ累積検証も原因単位で補正した。

1. Runtime CP1〜CP6 / CP8 / CP10とCP9 performance smokeのRoot stageに`!koiki-reference-app`を適用した。
2. CP8 / CP10はRoot POMからReferenceを除く現行正式release unitを導出し、15 projects / 12 JARの固定count、
   staged `org.koikifw` coordinates、packaging、Reference不在を完全一致で検査する。
3. 補正後CP10でCP1〜CP8、CP10 functional / failure / cleanup pathがPASSした。
4. CP10実行中に検出したCP9の同種欠陥を補正し、`-Smoke -SkipRegression`単独再検証でPASSした。
5. PowerShell parser、stage command静的点検、`git diff --check`がPASSした。

CP10再検証では、CP9補正前にCP1〜CP8 / CP10本体が完走している。CP9補正後はCP9 smokeを単独再実行し、
同一の長時間経路を重複実行していない。この組合せをAR3-F1の再検証Evidenceとし、P4-AR3全体の完了判定には
後段のformal package / Security Consumer結果を別途要求する。

## 5. Formal package and Security Consumer result

formal package検証は次のcurrent modeを使用した。

```powershell
pwsh -NoProfile -File build-support/security-foundation-verification/verify-p2-c2-package-static.ps1 `
  -CurrentFormalReleaseUnit
```

Phase 2履歴manifestの14 / 11契約と既定実行は変更せず、Phase 3で正式追加された`koiki-starter-web-mvc`を加えた
15 projects / 12 JARを検査した。続いて`verify-p2-c2-consumer.ps1`を実行し、同じReference除外project selectionから
Root外Security ConsumerをpackageしてPostgreSQL integrationを完了した。

## 6. Conclusion

production artifact、Framework Public API、migration、dependency、workflowまたはPhase 4実装は変更しない。
Runtime / Security Customer-like Consumerは、現行正式release unitを隔離repositoryから利用し、機能・失敗・Security・
cleanup経路を成立させた。blocking findingは0であり、P4-AR3を`COMPLETE — READY FOR P4-AR4`と判定する。
