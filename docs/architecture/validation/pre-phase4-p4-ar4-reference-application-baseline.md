# Pre-Phase 4 P4-AR4 Reference Application baseline

## 1. Status and boundary

| Item | Result |
|---|---|
| Status | `BLOCKED — REFERENCE TEST DATE BOUNDARY REMEDIATION REQUIRED` |
| Source commit | `cea83a2f5f9913a17c6b5460f5de855a5d506c82` |
| Execution branch | `docs/p4-ar1-environment-preflight` |
| Initial worktree | clean |
| Ownership | Reference test / Tooling / Architecture Evidence |
| Production change | 0 |
| Start | 2026-09-18 |
| Cleanup confirmed | 2026-09-18 |

P4-AR4は、package済みReference Application JAR、PostgreSQL 17、MVC / REST、実Chromium、DB / Audit /
sanitized logをfocused testとcritical journeyで再検証する。失敗を検出した場合は後段を成功扱いせず、原因、
Ownership、影響範囲およびcleanupを記録して停止する。

## 2. Initial execution result

| Order | Verification | Result | Evidence / finding |
|---|---|---|---|
| 1 | Source identity / clean worktree | PASS | commit、branch、clean worktreeを開始時に確認 |
| 2 | Docker / port / browser preflight | PASS | Docker Engine 29.5.3、残留container 0、18080 / 55432未使用、Playwright Chromium導入済み |
| 3 | Reference clean verify / package | FAIL / STOP CONDITION | 13-project dependency closure中、Reference 99 testsの1件がFAIL。package phaseへ到達せず |
| 4 | Bearer API focused | NOT RUN | Reference test failure後は後段を成功扱いしない |
| 5 | Session browser focused 3 tests | NOT RUN | Reference test failure後は後段を成功扱いしない |
| 6 | Critical journey E2E | NOT RUN | Reference test failure後は後段を成功扱いしない |
| 7 | Resource cleanup | PASS | container 0、対象Java process 0、Reference関連temporary directory 0、18080 / 55432解放 |

実行commandは次である。

```powershell
.\mvnw.cmd --batch-mode --no-transfer-progress -pl koiki-reference-app -am clean verify
```

前段のArchitecture Rules 67 testsはPASSした。Reference本体は99 tests中98 PASS、1 FAIL、error / skip 0で停止した。
失敗時の長大なSpring / MockMvc diagnostic、SQL、HTMLまたは一時CSRF値は本Evidenceへ転記しない。

## 3. Blocking finding

| ID | Classification | Finding | Impact |
|---|---|---|---|
| AR4-F1 | F2 Developer-experience gap / Reference test fixture | `ReferenceBusinessUrlSecurityTest.associatesEveryExpenseFieldErrorWithItsControl`が未来日を`LocalDate.now(UTC).plusDays(1)`で生成する。JST早朝はUTC日付が前日のため、生成値がapplication local dateと同日になり、`@PastOrPresent`違反と`usage-date-error`が発生しない | 時刻帯によりclean verifyが非決定的に失敗し、package済みJARと後段journeyのbaselineを確定できない |

同じvalidationを実browserで確認する`ReferenceHtmxJourneyTest`は、P3-C4でUTC-12〜UTC+14を考慮して
`UTC current date + 2 days`へ補正済みである。今回のfailureはその補正がReference acceptance testへ反映されず残った
同種fixture不具合であり、`ExpenseDraftForm`の`@PastOrPresent`、Domainのbusiness date規則、Public API、migration、
dependencyまたはruntime設定の不具合を示さない。

## 4. Remediation and resume condition

1. Reference acceptance testの未来日fixtureを、既承認browser fixtureと同じ`UTC current date + 2 days`へ補正する。
2. failing testをfocused再実行し、`usage-date-error`を含む全field accessibility assertionを確認する。
3. Reference clean verify / packageを再実行する。
4. PASS後だけBearer API focused、Session browser focused 3 tests、critical E2Eへ進む。
5. 最終的にDB / Audit / sanitized log、secret非露出およびresource cleanupを確認する。

修正はReference test sourceだけに限定し、production source、Framework Public API、migration、dependency、property、
profile、route、workflowまたはPhase 4実装を変更しない。AR4-F1解消と全後続検証PASSまでは、P4-AR4を完了または
P4-AR5 readyと判定しない。
