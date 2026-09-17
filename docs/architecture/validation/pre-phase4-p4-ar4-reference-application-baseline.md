# Pre-Phase 4 P4-AR4 Reference Application baseline

## 1. Status and boundary

| Item | Result |
|---|---|
| Status | `COMPLETE / READY FOR P4-AR5` |
| Baseline source commit | `cea83a2f5f9913a17c6b5460f5de855a5d506c82` |
| Blocker evidence commit | `07dbbcf` |
| Execution branch | `docs/p4-ar1-environment-preflight` |
| Initial worktree | clean |
| Ownership | Reference test / Reference developer guide / Tooling / Architecture Evidence |
| Production change | 0 |
| Start / completion | 2026-09-18 |
| Cleanup confirmed | 2026-09-18 |

P4-AR4は、package済みReference Application JAR、PostgreSQL 17、MVC / REST、実Chromium、DB / Audit /
sanitized logをfocused testとcritical journeyで再検証した。初回failureを成功扱いせずEvidenceへ記録してから、
Reference test fixtureとDeveloper Journeyを限定修正し、同じ順序で後段まで再開した。

## 2. Initial execution and stop condition

| Order | Verification | Initial result | Evidence / finding |
|---|---|---|---|
| 1 | Source identity / clean worktree | PASS | commit、branch、clean worktreeを開始時に確認 |
| 2 | Docker / port / browser preflight | PASS | Docker Engine 29.5.3、残留container 0、18080 / 55432未使用、Playwright Chromium導入済み |
| 3 | Reference clean verify / package | FAIL / STOP CONDITION | 13-project dependency closure中、Reference 99 testsの1件がFAIL。package phaseへ到達せず |
| 4 | Bearer API focused | NOT RUN | Reference test failure後は後段を成功扱いしなかった |
| 5 | Session browser focused 3 tests | NOT RUN | Reference test failure後は後段を成功扱いしなかった |
| 6 | Critical journey E2E | NOT RUN | Reference test failure後は後段を成功扱いしなかった |
| 7 | Resource cleanup | PASS | container、対象Java process、temporary directory、18080 / 55432の残留なし |

初回のArchitecture Rules 67 testsはPASSした。Reference本体は99 tests中98 PASS、1 FAIL、error / skip 0で
停止した。失敗時の長大なSpring / MockMvc diagnostic、SQL、HTMLまたは一時CSRF値は本Evidenceへ転記しない。

## 3. Findings and remediation

| ID | Classification | Finding | Remediation / result |
|---|---|---|---|
| AR4-F1 | F2 Developer-experience gap / Reference test fixture | `ReferenceBusinessUrlSecurityTest.associatesEveryExpenseFieldErrorWithItsControl`が未来日を`LocalDate.now(UTC).plusDays(1)`で生成し、JST早朝にはapplication local dateと同日になる | 既承認browser fixtureと同じ`UTC current date + 2 days`へ補正。focused 1 / 1とReference 99 / 99でPASS |
| AR4-F2 | F2 Developer-experience gap / Reference run configuration | hostの汎用環境変数`DEBUG=release`をSpring Bootがdebug有効化として解釈し、通常Session操作中にHibernate / JdbcTemplate SQLをprocess logへ出力した | Local Run Guideで`DEBUG=false`を明示し、cleanupと切り分けも追記。再実行でSQL / ERROR / exception cause / generated credential非出力を確認 |

AR4-F1は`ExpenseDraftForm`の`@PastOrPresent`、Domainのbusiness date規則、Public API、migration、dependencyまたは
runtime設定の不具合ではない。AR4-F2もFramework logger既定値の変更ではなく、既知のhost環境衝突をReferenceの
package JAR起動手順で遮断する修正である。Framework production source、Public API、migration、dependency、profile、
routeまたはworkflowは変更していない。

なお、最初のbrowser orchestrationではseed scriptのInformation stream捕捉漏れにより、一時生成credentialが
terminalへ出力された。該当DB containerは同じ実行の`finally`で直ちに破棄し、credentialは失効した。再実行では
Information streamを内部捕捉し、Repository、Evidenceおよびapplication process logへ値を保存していない。

## 4. Completed verification

| Order | Verification | Final result | Evidence |
|---|---|---|---|
| 1 | AR4-F1 focused test | PASS | `ReferenceBusinessUrlSecurityTest#associatesEveryExpenseFieldErrorWithItsControl` 1 / 1 |
| 2 | Reference clean verify / package | PASS | 13-project reactor成功、Architecture Rules 67 / 67、Reference 99 / 99、実行可能JAR生成 |
| 3 | Bearer API focused | PASS | package JAR、使い捨てPostgreSQL / JWKS、Bearer REST journey 1 / 1 |
| 4 | Session browser focused | PASS | 通常Session profile、実Chromium、master HTMX / CSRF、expense accessibility、独立BrowserContext競合の3 / 3 |
| 5 | Browser DB / Audit reconciliation | PASS | `expenses=3`、`approved=2`、`audit=5`、`sessions=4` |
| 6 | Browser process log | PASS | `DEBUG=false`でSQL、`ERROR`、exception causeおよび生成credentialを非検出 |
| 7 | Critical journey E2E | PASS | package JARを用いたBearer / Session / browser / DB / Audit / sanitized log横断journey 1 / 1 |
| 8 | Resource cleanup | PASS | container 0、対象Java process 0、18080 / 55432 listener 0、P4-AR4 temporary directory 0 |

主要commandは次である。

```powershell
.\mvnw.cmd --batch-mode --no-transfer-progress `
  -pl koiki-reference-app -am clean verify

.\mvnw.cmd --batch-mode --no-transfer-progress `
  -f .\build-support\reference-api-verification\pom.xml test

.\build-support\reference-browser-verification\verify-reference-htmx.ps1 `
  -LoginEmail "一時fixture email" -LoginPassword $ephemeralSecureString

.\mvnw.cmd --batch-mode --no-transfer-progress `
  -f .\build-support\reference-e2e-verification\pom.xml test
```

browser focusedではLocal Run Guideどおりpackage JARと使い捨てPostgreSQLを別processで起動し、seed passwordと
source HMAC keyを実行中のmemoryだけで扱った。各実行後はapplication、container、port、一時logおよび環境変数を
回収した。

## 5. Conclusion

Reference Applicationは、clean buildからpackage JARを生成し、Bearer RESTとSession MVCを別境界で実行できる。
focused browser journeyとcritical E2EはいずれもDB / Audit / sanitized logまで整合し、検証後のresource残留もない。
AR4-F1とAR4-F2はReference test / developer guide境界で解消され、Framework production contractを変更していない。

以上によりP4-AR4を`COMPLETE / READY FOR P4-AR5`とし、次はP4-AR5で受渡し候補とDeveloper Journeyを
consumer視点で棚卸しする。
