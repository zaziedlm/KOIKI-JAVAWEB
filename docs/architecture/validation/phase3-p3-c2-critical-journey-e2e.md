# Phase 3 P3-C2 critical journey E2E Evidence

## 1. Result

- 実施日: 2026年9月16日
- 開始baseline: `ea248f653391cdf5ec7ef19c82d3b0fc9882d280`
- branch: `feature/phase3-reference-vertical-slice`
- Status: `COMPLETE / OWNER APPROVED / CI CANDIDATE ACCEPTED`
- Ownership: 非配布Tooling
- production変更: 0

`build-support/reference-e2e-verification`に、package済み`koiki-reference-app` JARを対象とする
1本のcritical journeyを追加した。同じapplication processとPostgreSQL上で、Bearer REST、Session MVC、
HTMX、DB、Business / Security Auditおよびprocess logを一連で突合した。

## 2. Implemented boundary

| Item | Result |
|---|---|
| Entry point | `mvnw.cmd -f build-support/reference-e2e-verification/pom.xml test` |
| Application | `koiki-reference-app-0.1.0-SNAPSHOT.jar`を外部processとして起動。classes directoryへのfallbackなし |
| Database | Testcontainers PostgreSQL 17、runごとに新規作成・破棄 |
| API authentication | runごとのRSA key、loopback OIDC discovery / JWKS、実署名Bearer |
| Browser authentication | local Session login。APIへCookie fallbackせず、MVCへBearerを流用しない |
| Browser | Playwright Java / Chromium 1.62.0、実BrowserContext |
| Fixture | 使い捨てDB内のsynthetic user / master / scope。password、token、Cookie、keyは実行中だけ保持 |
| Distribution | Root Reactor、Framework artifact、Reference JAR、`koiki-testing`、Project Templateへ非包含 |

Framework / Reference production source、Public API、property、migration、正式Maven module、BOM、Root Reactor、
workflowおよびrequired checkは変更していない。追加dependencyは独立Tooling POMのtest scopeに限定した。

## 3. Critical journey result

1. applicant Bearerでdraftを作成し、HTTP 201と生成IDを確認した。
2. Bearer detailで`DRAFT`、expected versionを確認した。
3. Bearer commandでsubmitし、HTTP 204を確認した。
4. approverがChromiumでSession loginした。
5. master部門検索を`HX-Request: true`で実行し、HTTP 200、fragment root、検索値とfocus維持を確認した。
6. 同じ申請をbrowserから承認し、redirectと完了表示を確認した。
7. applicant Bearer detailで最終`APPROVED`を確認した。
8. DBでstate `APPROVED`、version `3`、申請額、明細件数、明細合計を突合した。
9. Business Auditの`SUBMIT_EXPENSE` / `APPROVE_EXPENSE`と各actor、Security Auditのlogin成功、
   approver principalの永続Session 1件を突合した。
10. application停止後の全process logを検査し、token、password、Cookie、HMAC key、fixture email、SQL、
    `ERROR`、`Caused by`の非出力を確認した。startup failure時もprocess log本文をtest reportへ転記しない。

起動確認の`GET /login`も匿名Sessionを生成するため、Sessionの総件数ではなくapprover principalに紐づく
Session件数を検査する。この差異はfixture assertionの精度問題であり、production defectではない。

## 4. Stability and cleanup

最終コードの同一entrypointを3回連続実行した。

| Run | Result | Wall time | Cleanup |
|---:|---|---:|---|
| 1 | PASS | 19.058 s | PASS |
| 2 | PASS | 18.601 s | PASS |
| 3 | PASS | 18.577 s | PASS |

- 最小18.577秒、最大19.058秒、spread 0.481秒（約2.59%）であり、異常増加を認めない。
- sleep固定待機ではなく、application readiness、HTMX responseおよびBrowser locatorを条件待機した。
- 各run内でBrowserContext / Chromium、issuer、application、PostgreSQL、dynamic port、temp logをcleanupした。
- 各run後の外部確認でもPostgreSQL 17 container 0、対象application process 0を確認した。
- assertion failureを意図せず検出した実装途中runでも、finally経路によりcontainerとapplication processが
  残らないことを確認した。startup failureも同じfinally境界で処理する。

## 5. Regression

| Verification | Result |
|---|---|
| Root Reactor `clean verify` | PASS。16 / 16 projects、170 tests、Reference 99 tests、failure / error / skip 0 |
| P3-C1 package API focused Tooling | PASS。1 test、11.88 s、failure / error / skip 0 |
| P3-B3 / B4 browser focused Tooling | PASS。3 tests、6.430 s、failure / error / skip 0 |
| P3-C2 package E2E focused Tooling | PASS。1 test、failure / error / skip 0 |

最初のRoot Reactor実行はsandboxからDocker named pipeへ接続できずTestcontainersが開始不能だった。
同一commandをDockerへ接続できる環境で再実行して成功したため、コードまたは回帰不具合とは判定しない。
standalone ToolingのSLF4J NOP warningはTooling側logging provider非配置を示すもので、application logの
WARN / ERRORではない。

## 6. CI candidate decision

P3-C2 entrypointは、package済みJAR、Docker / Testcontainers、Chromium cacheとJDK 21が利用できるCI runnerから
同じcommandで実行できる。1 journey約19秒、3回の変動約2.59%、外部resource残存0のため、
Phase 3のcritical journey CI候補として妥当と判断する。

ただし、Browser / E2E harnessは非配布ToolingでありCustomer CIへ無条件で必須化しない。workflow追加、
required check化、browser download、remote実行およびrunner要件の確定はRemote Gateへ送る。

DoD 3-11の正本文言は「E2EスモークテストがCIで通る」である。P3-C2が完了させるのは、同じentrypointを
CI runnerから呼べる状態、localでの安定性・実行時間・cleanupおよびCI候補としての受入までとする。
実workflow上のPASSを伴うDoD 3-11の最終充足は本Evidenceでは宣言せず、Remote Gateで実行を承認した後の
CI結果をGate C final acceptanceへ入力して確定する。

| Scope | Status at P3-C2 |
|---|---|
| critical journey実装とlocal aggregate | COMPLETE |
| CI候補の安定性・runtime・cleanup評価 | OWNER APPROVED |
| workflow追加・remote実行 | DEFERRED TO REMOTE GATE |
| DoD 3-11「CIで通る」の最終充足 | PENDING CI EVIDENCE |

## 7. Owner close review points

Architecture Ownerには次の3点をblocking判断として依頼する。

1. API BearerとMVC Sessionを同一process / DBで連結した代表happy pathが、DoD 3-11をCIで実行可能な
   候補として十分に実証されているか。実CI通過による最終充足をRemote Gate / Gate Cへ継続する境界を受け入れるか。
2. 1 journey約19秒、3回連続PASS、cleanup残存0をCI候補判断として受け入れるか。
3. production変更0、Root外・test scope限定、workflow未変更のOwnership / deferred境界を受け入れるか。

Architecture Ownerは上記3点を2026年9月16日に一体として確認し、P3-C2のlocal implementationと
CI候補判断を承認した。

**Decision:** APPROVED — P3-C2 COMPLETE / OWNER APPROVED / CI CANDIDATE ACCEPTED
**Accepted scope:** package済みJARのBearer API / Session Browser横断journey、HTMX、DB / Audit / log突合、
3回連続安定性、cleanupおよびTooling / deferred境界
**DoD continuation:** DoD 3-11の実CI PASSは未完了。Remote Gateで承認したworkflowの結果をGate Cへ入力する
**Decided by:** Shuichi Kataoka, Architecture Owner
**Decision date:** 2026年9月16日
**Next CP:** P3-C3 MyBatis規約fixture / Rule 35〜37
**Subsequent decision:** P3-C3は同日、Architecture Owner判断により
`DEFERRED — MyBatis adoption trigger required`となった。現在のNext CPはP3-C4である。
判断Evidenceは`phase3-p3-c3-mybatis-deferral.md`を参照する。

本承認はP3-C2のcommit pointとP3-C3開始だけを許可する。workflow、required check、remote操作、
snapshot publishまたはDoD 3-11最終充足を承認するものではない。
