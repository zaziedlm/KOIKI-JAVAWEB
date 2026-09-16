# Phase 3 P3-C1 minimal REST API

## 1. Status and boundary

| Item | Result |
|---|---|
| Date | 2026年9月16日 |
| Architecture Owner | Shuichi Kataoka |
| Phase / CP | Phase 3 Reference Vertical Slice / P3-C1 |
| Status | COMPLETE / OWNER APPROVED |
| Start HEAD | `0d5c59c7595219aacf28f3d49de0385e6bf6e560`（P3-C0 Owner approval後） |
| Ownership | Reference expense REST、Reference identity/security assembly、非配布API Tooling |

P3-C0で承認した契約を再設計せず、既存expense Application Use Caseとscoped read modelへ
最小REST adapterを追加した。Framework production source、Framework Public API、migration、BOM、Root Reactor module、
workflowおよびremote設定は変更していない。P3-C2、Phase 4およびproduction SSO / frontend topologyは先行しない。

## 2. Implemented REST contract

| Method / path | Success | Existing use case / read model |
|---|---:|---|
| `GET /api/v1/expense-requests/{id}` | 200 | applicant own-scopeの`ExpenseReadService.findOwnRequest` |
| `POST /api/v1/expense-requests` | 201、`Location`、生成ID | `ExpenseApplicationService.createDraft` |
| `POST /api/v1/expense-requests/{id}/submit` | 204 | `ExpenseApplicationService.submit` |

Controller、request / response recordおよびmapperは`expense.adapter.inbound.api`に置き、MVC Form / View DTO、
Domain ModelおよびJPA Entityを共有またはJSON露出しない。明細IDはserver側で生成し、detail responseから
applicant user ID / emailを除外した。Jackson 3と`koiki-starter-api`をReferenceだけで再利用する。

Spring MVC API version resolverは`/api/**`だけのpath segment 1へ限定した。これにより`version = "1"`の
handler契約を維持しながら、既存の`/identity/**`、`/master/**`、`/expenses/**`およびstatic resourceに
API version解釈を波及させない。

## 3. Problem Details and failure mapping

全REST errorはRFC 9457 `application/problem+json`として`type=about:blank`、`status`、安全な`detail`、
request pathの`instance`およびstable `code`を返す。

| Failure | HTTP | Code |
|---|---:|---|
| validation | 400 | `KOIKI-VALIDATION-001` |
| malformed JSON | 400 | `KOIKI-JSON-001` |
| invalid business input | 422 | `KOIKI-REF-EXPENSE-001` |
| unavailable master | 422 | `KOIKI-REF-EXPENSE-002` |
| not found / own-scope concealment | 404 | `KOIKI-REF-EXPENSE-003` |
| invalid transition | 409 | `KOIKI-REF-EXPENSE-004` |
| stale expected version | 409 | `KOIKI-REF-EXPENSE-005` |
| data conflict | 409 | `KOIKI-REF-EXPENSE-006` |
| dependency failure | 503 | `KOIKI-REF-EXPENSE-007` |
| missing / invalid Bearer | 401 | `KOIKI-REF-AUTH-001` |
| authenticated but insufficient authority | 403 | `KOIKI-REF-AUTH-002` |

exception class、stack trace、SQL、raw token、internal actorまたは他利用者の存在をresponseへ露出しない。

## 4. P3-C1 representative Bearer profile

`api-bearer` profileでだけReference専用の最優先`/api/**` SecurityFilterChainを有効化する。

- `Authorization: Bearer`だけを使用し、Session Cookie、query、form、raw edge headerへfallbackしない。
- API chainはstateless、request cacheなし、API chainだけCSRF対象外とする。browser Session chainのCSRFは維持する。
- CORSは有効化しない。
- signature、issuer、audience、timeおよび`token_use=access`を検証する。ID Tokenを拒否する。
- 検証済み`koiki_user_id`をcanonical UUIDとして読み、各requestでFramework `IdentityQuery`からACTIVE userを再取得する。
- exact token scope `expense.apply`と現在DB Permission `EXPENSE:APPLY`の積集合だけをauthorityにする。
- `FrameworkPrincipal`へimmutable user IDと上記permissionだけを載せ、JWTまたは業務属性を保持しない。

issuerとaudienceは環境変数で注入し、production issuer、key、login/token endpointまたはAuthorization Serverを
Repositoryへ追加していない。このprofileはPhase 2 Resource Server baselineのP3-C1限定Evidenceであり、Phase 4の
Session Cookie Profile Sまたはproduction SSO選択を確定しない。

## 5. Verification evidence

| Check | Result | Evidence |
|---|---|---|
| Controller / Jackson 3 | PASS | 3 endpoint、201 / 200 / 204、Location、server生成line ID、DTO非露出、validation、malformed JSON、unsupported / noncanonical version |
| Stable failures | PASS | 400 / 404 / 409 / 422 / 503と承認済みcode mapping |
| Actual JWT validation | PASS | test-only RSA署名とloopback OIDC/JWKSでvalid access tokenを受理し、signature / issuer / audience / expired / future / ID Tokenを拒否 |
| Identity mapping | PASS | exact scope、ACTIVE user、current DB Permissionの積集合。malformed / unknown / disabled userを認証失敗 |
| Profile separation | PASS | missing / invalid / unknown Bearerは401、scope不足は403、browser Session CookieだけではAPI不成立、valid Bearer POSTはCSRF tokenなしで成立 |
| PostgreSQL HTTP integration | PASS | MockMvcからreal Identity query、Use Case、JPA、scoped JdbcClient、Business Auditまで接続 |
| Scope / rollback | PASS | outsider detailは404、stale submitは409、inactive master createは422。state / row / Audit副作用なし |
| MVC / browser regression | PASS | API version resolverを`/api/**`へ限定し、既存Session / CSRF / MVC / HTMX suiteを維持 |
| MVC local manual | PASS | 通常profileのpackage済みJARでlogin、master HTMX検索、expense create / submitを実操作し、HTTP成功、`SUBMITTED:2`、申請額と明細合計、`SUBMIT_EXPENSE` AuditをDB突合。操作中ERROR / WARN、4xx / 5xxなし |
| Architecture | PASS | Reference business module rulesとFramework Identity public boundary |
| Root Reactor | PASS | `clean verify`、16 / 16 projects、170 tests、failure / error / skip 0、Reference 99 tests |
| Packaged JAR HTTP | PASS | 最終JARを別JVMで起動し、PostgreSQL 17、test-only OIDC/JWKS、実署名Bearerでcreate → detail → submit |
| HTTP / DB / Audit / log | PASS | 201 / 200 / 204、`SUBMITTED:2`、`SUBMIT_EXPENSE` 1件、response PIIなし、process logにBearer tokenなし |

実行commandは次のとおりである。

```powershell
.\mvnw.cmd clean verify
.\mvnw.cmd -f .\build-support\reference-api-verification\pom.xml test
```

package済みJAR journeyはRoot Reactor外の`build-support/reference-api-verification`だけが所有する。
fixture key、token、user、DBおよびprocess logはtest process内の一時物であり、配布artifactへ含めない。

非cleanの部分Reactor再実行では、Maven compilerのincremental判定後に`koiki-starter-api/target/classes`から
EnvironmentPostProcessor classが欠落し、Reference test contextが起動しない事象を再現した。source compile errorや
P3-C1 runtime failureではなく、正本のRoot `clean verify`は同じworking treeで連続して成功した。P3-C1では
Framework build設定を変更せず、部分Reactor確認でもclean buildを使用する。incremental build挙動の調査は
Build Foundationの別follow-up候補とする。

## 6. Artifact and Public API inventory

| Item | Result |
|---|---|
| Formal Framework modules | 変更なし |
| Framework Public API / `public-api.txt` | 変更なし |
| Production migration | 変更なし |
| Reference dependency | `koiki-reference-app`だけへ`koiki-starter-api`を追加。versionはReactor / BOMに委譲 |
| Reference JAR | expense REST adapterとReference限定Bearer assemblyを`BOOT-INF/classes`へ収録 |
| Test / Tooling classes in Reference JAR | 0。JWT issuer/JWKSとpackage journeyはtest scope / Root Reactor外 |
| New Maven module / workflow / remote | なし |

REST Controller等のJava型はReference実装詳細であり、正式Framework Public APIではない。DTO recordは
package-privateを維持し、Domain / Entityまたはfixture APIをCustomer契約へ昇格させない。

## 7. Deferred decisions

P3-C0 §9.1のO1〜O10を継続する。特にReact / Next.js採否、Session Cookie Profile S、BFF、production issuer、
`issuer + subject` mapping、Access / Refresh Token、CORS / origin / logoutおよびtoken storageはPhase 4で決定する。
P3-C1の`koiki_user_id`、issuer profileおよびtest fixtureをproduction契約とみなさない。

## 8. Owner review checkpoint

P3-C1実装・自動検証・package済みHTTP Evidenceは完了した。Architecture Owner reviewでは次を確認する。

1. P3-C0のendpoint / DTO / status / error / lock契約との一致。
2. MVCとRESTが同じUse Case、認可、state transitionおよびBusiness Auditを使用すること。
3. Bearer / Session非fallback、browser CSRF維持、exact scopeとcurrent DB Permissionの積集合。
4. Reference / Tooling限定で、Framework Public API、migration、production SSOまたはPhase 4を先行していないこと。
5. Root Reactor、package済みJAR、HTTP / DB / Audit / log Evidence。

**Decision:** APPROVED — P3-C1 COMPLETE / OWNER APPROVED

**Decided by:** Shuichi Kataoka, Architecture Owner

**Decision date:** 2026年9月16日

P3-C0契約との一致、MVC / RESTのApplication同値性、Bearer / Session非fallback、Reference / Tooling Ownership、
Root Reactor、package済みHTTPおよびMVC local manual Evidenceを一体として承認し、P3-C1を
`COMPLETE / OWNER APPROVED`とする。P3-C1の実装一式をcommit pointとして閉じた後、次に許可されるのは
P3-C2 critical journey E2Eの開始であり、Phase 4、Framework Public API、migration、workflowまたはremote変更を
先行しない。
