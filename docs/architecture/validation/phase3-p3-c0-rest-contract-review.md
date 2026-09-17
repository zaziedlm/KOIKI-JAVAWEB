# Phase 3 P3-C0 minimal REST API contract review

## 1. Status and boundary

| Item | Result |
|---|---|
| Date | 2026年9月15日 |
| Architecture Owner | Shuichi Kataoka |
| Phase / CP | Phase 3 Reference Vertical Slice / P3-C0 |
| Status | CONTRACT COMPLETE / ARCHITECTURE OWNER APPROVED / P3-C1 READY |
| Branch / review start HEAD | `feature/phase3-reference-vertical-slice` / `2daf4dfcd397b21c947d14b6d3ec89c1b5a0c230` |
| Primary ownership | Reference expense REST inbound adapter、Reference identity / security assembly |
| Production implementation | 未開始（本reviewの変更は文書だけ） |

P3-C0は、Gate B `COMPLETE / ACCEPTED`を入力として、DoD 3-10 / AC-P3-10の最小REST APIを
実装する前のblocking contract reviewである。endpoint、DTO、Permission、HTTP status、Problem Details、
楽観lockおよびBearer認証境界を確定する。本reviewではproduction code、Public API、Maven module、migration、
dependency、workflow、remote設定およびtest fixtureを変更しない。

## 2. Authoritative inputs

- `AGENTS.md`
- `docs/architecture/grand-design/KOIKI-JavaWeb-FW_グランドデザイン_v0.2.md` §12、§14、§16.4、Phase 3 DoD 3-10
- `docs/development/KOIKI-JavaWeb-FW_Phase3実行計画_v0.1.md`
- `docs/reference/KOIKI-JavaWeb-FW_Reference_Application_Specification_v0.1.md` AC-P3-02 / 03 / 10
- `docs/architecture/validation/phase2-p2-a3-contract-inventory.md`
- `docs/architecture/validation/phase2-p2-a3-t3-verification.md`
- `docs/architecture/validation/phase2-spa-sso-security-fitting.md`
- `docs/architecture/validation/phase2-token-lifecycle-phase-decision.md`
- `docs/architecture/validation/phase3-p3-a2-expense-vertical-slice.md`
- `docs/architecture/validation/phase3-p3-a3-authorization-audit.md`
- `docs/architecture/validation/phase3-p3-b1-read-model.md`
- `docs/architecture/validation/phase3-p3-b4-lock-cache-contract-review.md`
- `docs/architecture/validation/phase3-gate-b-web-query-acceptance.md`
- `docs/agent/skills/koiki-project-overview/SKILL.md`
- `docs/agent/skills/koiki-business-feature-work/SKILL.md`

第三者機能はSpring Framework API Versioning、Spring Security Resource ServerおよびRFC 9457の
公式一次情報で確認した。参照先は§14に示す。

## 3. Current implementation inventory

### 3.1 Reusable application boundary

- `ExpenseApplicationService.createDraft(...)`と`submit(...)`が、MVCで成立済みの作成・提出Use Caseである。
- `ExpenseReadService.findOwnRequest(...)`が、SQL内でactor scopeを先に強制する申請者本人向け詳細queryである。
- 上記はすべて`EXPENSE:APPLY`をMethod Securityで要求し、認証済み`FrameworkPrincipal`のimmutable
  `FrameworkUserId`をactorとする。
- create / submitはDomain invariant、current-value master確認、JPA transaction、同期Domain Eventおよび
  Business Auditを既に一体として扱う。REST Controllerへ同じ規則を複製しない。
- `ExpenseFailure`は`INVALID_INPUT`、`MASTER_UNAVAILABLE`、`NOT_FOUND`、`INVALID_TRANSITION`、
  `SELF_DECISION`、`CONCURRENT_MODIFICATION`、`CONFLICT`、`DEPENDENCY_FAILURE`を持つ。

### 3.2 Existing API and security foundation

- `koiki-starter-api`はJackson 3、path-segment API versioning、RFC 9457 `ProblemDetail`、request validation、
  invalid JSONおよびunexpected failureのsanitized fallbackを提供する。
- Frameworkのversion設定はpath segment index 1、required、supported version `1`である。
- Spring標準`SemanticApiVersionParser`は`v1`のような先頭の非整数文字を読み飛ばすため、handler version `1`と
  外部表記`v1`を独自parserなしで対応付けられる。
- Phase 2はstateless Bearer JWT、signature / issuer / audience / time、exact scope mapping、401 / 403、
  Session / Cookie非fallbackを非配布fixtureで実証済みである。
- 現在のReference Security chainはbrowser pathだけを明示し、`/api/**`はFramework default denyへ落ちる。
- Phase 2にJWT principalを`FrameworkPrincipal`へ変換する配布実装はない。issuer、audience、claim / scope mapping、
  path matcherおよびCORSはCustomer / application assembly責務として承認済みである。

## 4. Endpoint and version contract

正規の外部endpointは次の3本だけとする。

| Method and path | Use Case | Success |
|---|---|---|
| `GET /api/v1/expense-requests/{expenseRequestId}` | 認証actor本人の詳細取得 | `200 OK` + detail response |
| `POST /api/v1/expense-requests` | draft作成 | `201 Created` + ID response + `Location` |
| `POST /api/v1/expense-requests/{expenseRequestId}/submit` | expected versionで提出 | `204 No Content` |

- class mappingは`/api/v{version:[1-9][0-9]*}/expense-requests`、各handler mappingは`version = "1"`とし、
  Spring標準のpath-segment versioningを使用する。`/api/1`、leading zeroまたは任意prefixを正規endpointや
  互換保証へ含めない。
- version segmentは必須とし、versionなし、未知versionおよびpath外endpointを暗黙fallbackしない。
- `POST create`の`Location`は`/api/v1/expense-requests/{createdId}`とする。
- create / submitは一般的なHTTP retryに対するidempotent APIとはしない。Phase 3の人系journeyには
  Idempotency-Key要件がなく、新しいstorage、key Public APIまたは重複抑止を先行しない。
- edit、approve、reject、return、reopen、settle、list / search、masterおよびidentity REST endpointは追加しない。

## 5. DTO contract

すべてのDTOを`expense.adapter.inbound.api`が所有する別recordとし、MVC Form、Application query record、
Domain Model、JPA EntityおよびFramework型をJSON契約として直接公開しない。JSON propertyはlower camel case、
dateはISO `yyyy-MM-dd`、instantはUTC offsetを含むISO-8601、amountは小数なしのJSON integerとする。

### 5.1 Create request / response

```json
{
  "departmentId": "00000000-0000-4000-8000-000000000001",
  "claimedAmount": 1200,
  "lines": [
    {
      "expenseCategoryId": "00000000-0000-4000-8000-000000000002",
      "usageDate": "2026-09-15",
      "description": "train fare",
      "purpose": "customer visit",
      "amount": 1200
    }
  ]
}
```

```json
{
  "expenseRequestId": "00000000-0000-4000-8000-000000000003"
}
```

- `departmentId` / `expenseCategoryId`はUUID、`claimedAmount` / `amount`はpositive 64-bit integer、
  `lines`はnull不可・空不可、要素null不可とする。
- `usageDate`は当日以前、`description`はnonblank / 200文字以下、`purpose`はnonblank / 500文字以下とする。
- expense line IDはREST adapterがserver-sideで生成し、create requestから受け取らない。
- claimed totalとline totalの一致、active master、actorのactive department assignment等は既存Application / Domain
  が最終判断する。ControllerのValidationだけを業務不変条件にしない。

### 5.2 Detail response

```json
{
  "expenseRequestId": "00000000-0000-4000-8000-000000000003",
  "department": {
    "departmentId": "00000000-0000-4000-8000-000000000001",
    "code": "SALES",
    "name": "Sales"
  },
  "claimedAmount": 1200,
  "status": "DRAFT",
  "decisionReason": null,
  "version": 0,
  "updatedAt": "2026-09-15T01:23:45Z",
  "lines": [
    {
      "expenseLineId": "00000000-0000-4000-8000-000000000004",
      "expenseCategoryId": "00000000-0000-4000-8000-000000000002",
      "expenseCategoryCode": "TRAVEL",
      "expenseCategoryName": "Travel",
      "usageDate": "2026-09-15",
      "description": "train fare",
      "purpose": "customer visit",
      "amount": 1200
    }
  ]
}
```

- detailは`ExpenseReadService.findOwnRequest(...)`のscope済み結果からREST responseへcopyする。
- applicant user ID / emailは本人向け最小responseに不要なため公開しない。
- `status`は既存workflow名、`version`はnonnegative 64-bit integerとする。
- `decisionReason`は値がない場合もJSON `null`としてpropertyを保持する。

### 5.3 Submit request / response

```json
{
  "expectedVersion": 0
}
```

- `expectedVersion`は必須かつnonnegative 64-bit integerとする。
- 成功responseはbodyなしの`204 No Content`とし、最新representationの暗黙生成や再queryを行わない。
- clientは必要ならGET detailを明示的に再取得する。

## 6. Optimistic lock contract

- Phase 3ではETag / `If-Match`ではなくrequest bodyの`expectedVersion`を採用する。これはグランドデザイン§12.5が
  許容する方式であり、MVC hidden versionと同じApplication Use Caseへそのまま接続できる。
- REST adapterはversion比較、reload、merge、自動retryまたはlast-write-winsを実装しない。
- stale versionは`409 Conflict`とし、state、version、Domain Event、Business Auditおよび他rowを変更しない。
- `412 Precondition Failed`、`428 Precondition Required`、weak / strong ETagおよびconditional GETはPhase 3に追加しない。

## 7. Permission and Phase 3 representative Bearer identity contract

§4〜§6および§8のREST業務契約は、React / Next.jsの採否、Session / Bearerの認証transportまたは
Authorization Server製品から分離する。P3-C1ではPhase 2 Resource Server baselineの再利用を確認するため
Bearer profileを代表経路として限定実証するが、これをPhase 4以降のproduction frontend / SSO topologyの
決定とは扱わない。`/api/v1/expense-requests/**`というREST path自体へ認証transportを恒久固定せず、Phase 4では
必須のsame-origin Profile Sから同じendpointをCookie Session + CSRFで利用できることを実証する。

### 7.1 Security chain

- P3-C1のReference / Tooling代表profileに限り、`/api/**`だけに一致する最優先のstateless
  `SecurityFilterChain`を置き、OAuth 2.0 Resource Server Bearer JWTを使用する。browser Session chainおよび
  Framework fallback denyとmatcherを重複させない。このpath mappingをPhase 4以降の恒久契約にしない。
- このP3-C1代表profileではtokenを`Authorization: Bearer`だけから取得する。Session Cookie、query、form、ID Tokenおよびraw edge headerを
  API認証へfallbackしない。
- Bearer専用chainだけCSRF対象外とする。browser chainのCSRFを弱めない。
- signature、許可algorithm、exact issuer、audience、`exp`、`nbf`およびAccess Token種別markerを
  Spring標準`JwtDecoder` / validatorで検証し、ID Tokenを同じ署名だけで受理しない。
- CORSはP3-C1のserver-to-server / test clientには不要なため有効化しない。Phase 4でSPA originが確定するまで
  wildcardまたは仮originを追加しない。
- P3-C1のSecurity構成とtoken issuer / keyはReferenceまたはToolingの限定Evidenceとし、production SSO接続、
  production key、client registration、token発行またはrefresh管理の成立を主張しない。
- Phase 4 Referenceではsame-origin Profile Sを必須実証し、同じREST endpointをstateful Session chainへ割り当て、
  unsafe requestにCSRFを強制する。Profile B / Tを実証または採用する構成では同endpointをBearer chainへ割り当てる。
- 各deployment profile内で同じrequest pathにSession CookieとBearerを同時fallbackさせない。profile、hostまたは
  明示的な排他Security構成でtrust boundaryを分離する。これはprofile間で同じREST pathを再利用することを妨げない。

### 7.2 Actor and authority mapping

- URL boundaryは認証を必須とし、create / get / submitの最終認可は既存Applicationの
  `hasAuthority('EXPENSE:APPLY')`を維持する。
- P3-C1のReference限定候補では、検証済みAccess Tokenの専用`koiki_user_id` claimをcanonical UUIDとして読み、
  email、display nameまたは未検証`sub`をFramework user IDへ変換しない。このclaim名と発行方式を外部IdP / Authorization
  Server共通のproduction契約またはFramework Public APIとして固定しない。
- `IdentityQuery.findById(...)`で既存Framework userをrequestごとに照合し、unknown / malformed / `DISABLED`を
  authentication failureとして拒否する。userやexternal linkをJIT作成しない。
- exact token scope `expense.apply`とIdentityの有効Permission `EXPENSE:APPLY`の積集合だけを
  `GrantedAuthority`および`FrameworkPrincipal.permissions()`へ設定する。token scope単独でもDB Role単独でも
  権限を昇格させない。
- principalは`FrameworkUserId`、`AuthenticationSource.BEARER`、上記Permissionだけを保持するReference内部recordとする。
  JWT、claim map、email、issuer / subjectをApplication、Audit、responseまたはlogへ渡さない。
- issuer / audience、専用claim名、scope mappingおよびtoken発行はapplication / Customer assembly契約であり、
  Framework Public APIまたはKOIKI独自token発行機能へ昇格させない。
- production SSOのactor mappingは、外部ProviderがKOIKI API向け専用user ID claimを安全に発行できるか、または
  verified `issuer + subject`を既存external identity linkへ照合するseamが必要かをPhase 4のblocking reviewで決める。
  後者にFramework Public API変更が必要なら、P3-C1へ混入せず別のArchitecture reviewへ戻す。

認証失敗は`401`、認証済みだがscope / Permission不足は`403`とする。存在しないIDと他actor所有IDはGET / submitとも
同じ`404`へ畳み、resource存在を列挙させない。

## 8. Problem Details and status contract

error media typeは`application/problem+json`、基礎shapeはRFC 9457の`type`、`title`、`status`、`detail`、
`instance`と、KOIKI extension `code`とする。`type`はP3-C1では`about:blank`を維持する。

| Condition | Status | Stable code |
|---|---:|---|
| Jakarta Validation failure | 400 | `KOIKI-VALIDATION-001` |
| malformed / unreadable JSON | 400 | `KOIKI-JSON-001` |
| malformed UUID / request parameter conversion | 400 | `KOIKI-HTTP-400` |
| unsupported API version on the versioned route | 400 | `KOIKI-HTTP-400` |
| invalid aggregate input | 422 | `KOIKI-REF-EXPENSE-001` |
| inactive / unavailable master or assignment | 422 | `KOIKI-REF-EXPENSE-002` |
| unknown or out-of-scope expense | 404 | `KOIKI-REF-EXPENSE-003` |
| invalid state transition | 409 | `KOIKI-REF-EXPENSE-004` |
| stale optimistic version | 409 | `KOIKI-REF-EXPENSE-005` |
| other known persistence conflict | 409 | `KOIKI-REF-EXPENSE-006` |
| downstream / required dependency unavailable | 503 | `KOIKI-REF-EXPENSE-007` |
| missing / invalid Bearer authentication | 401 | `KOIKI-REF-AUTH-001` |
| authenticated but insufficient authority | 403 | `KOIKI-REF-AUTH-002` |
| unexpected server failure | 500 | `KOIKI-INTERNAL-001` |

- syntax / representation failureの400と、JSONとして有効だが業務制約を満たさない422を分離する。
- state transition、stale versionおよび一意性等の現在stateとの衝突は409とする。業務拒否を403へ混在させない。
- application-owned `@RestControllerAdvice`は3 endpointから到達可能な既知`ExpenseFailure`だけを上表へ変換し、
  Framework fallbackより高い優先度にする。将来のdecision endpoint用failure codeをP3-C0で予約しない。
- API chainのAuthenticationEntryPoint / AccessDeniedHandlerも同じsafe Problem Details shapeを返す。
- validationの`violations`は既存Framework契約どおりfield / safe messageだけとし、rejected valueを含めない。
- exception class、message、SQL、stack trace、JWT、claim、Authorization header、email、internal actor、DB key、
  submitted description / purposeをProblem Detailsまたは通常logへ含めない。

## 9. Assembly and ownership

| Concern | Owner / placement |
|---|---|
| REST Controller / request / response / mapper / known failure advice | `expense.adapter.inbound.api` |
| create / submit rule、transaction、event、audit | existing `expense.application` / `expense.domain` |
| detail scope / materialization | existing `ExpenseReadService` / outbound JdbcClient adapter |
| P3-C1代表profileのAPI SecurityFilterChain、JWT-to-principal adapter | Reference identity / security assembly |
| versioning、Jackson 3、generic Problem Details | existing `koiki-starter-api` |
| P3-C1限定issuer / audience / claim / scope構成 | Reference identity / security assemblyだけ |
| production issuer、audience、Access Token、claim / scope issuance | 未決定。§9.1でPhase 4へ引き継ぐ |
| API journey fixture / key / token | Tooling test source only |

P3-C1では`koiki-reference-app`だけへ`koiki-starter-api`を直接追加し、versionはReactor / BOMに委ねる。
Framework Starter、BOM、Root module、Public API、migrationおよび既存MVC DTOを変更しない。Reference RESTを
Framework共通Controller、Project Template、`koiki-testing`または正式security fixtureへ昇格させない。

### 9.1 React / Next.js and token lifecycle continuity inventory

React / Next.jsから本REST APIを利用する可能性は保持する。Phase 4 ReferenceでProfile Sを必須実証する
accepted baselineは変更しない。一方、SSO連携先、deployment topology、production採用profileおよびtoken lifecycleは
現時点で不透明なため、その具体化をP3-C0で固定せず、次を継続確認事項として記録する。

| ID | 継続確認事項 | 現時点で維持する境界 | Decision timing |
|---|---|---|---|
| P3-C0-O1 | frontend profile | Phase 4 Referenceではsame-origin Profile Sを必須実証する。Profile Bはfrontend / API分離時の候補、Profile Tはrisk acceptance付きopt-inとし、production採用profileはここで固定しない | Phase 4 frontend / security blocking review |
| P3-C0-O2 | origin / routing | React、BFF、KOIKI APIのhost / path / TLS終端と、Session / Bearer matcherの非重複 | Phase 4 deployment contract |
| P3-C0-O3 | OAuth client owner | KOIKI backend、Next.js BFF confidential client、React public clientのいずれか | Phase 4 profile decision |
| P3-C0-O4 | issuer / SSO provider | external OIDC Provider / Authorization Serverを第一候補とするが、製品、tenant、federationを未決定とする | Customer / deployment fitting |
| P3-C0-O5 | API token contract | KOIKI API向けaudience、最小scope、Access Token種別、TTL、algorithm、JWKS / key rotation | Provider selection後、接続実装前 |
| P3-C0-O6 | actor mapping | 専用user ID claimとverified `issuer + subject` linkのどちらを使うか。email auto-link / JITは不採用 | Phase 4 identity blocking review |
| P3-C0-O7 | token location | BFF採用時はAccess / Refresh Tokenをserver-sideに保持しbrowserへ露出しない。direct Tokenはrisk acceptance必須 | Phase 4 profile decision |
| P3-C0-O8 | refresh / revoke | 発行可否、rotation / sender constraint、reuse、maximum lifetime、revoke、既発行JWTの残存window | Authorization Server / client acceptance |
| P3-C0-O9 | CSRF / CORS / logout | Session CookieのCSRF、Bearerのexact CORS、BFF / KOIKI / IdP logoutの残存状態を別々に定義 | Phase 4 security acceptance |
| P3-C0-O10 | hosted issuer | external Providerで要件を満たせない明示use caseがある場合だけ、optional `P4-AS0`でbuild-vs-buyから再承認する | Phase 4 optional Gate |

profileが変わっても、同じREST DTO、status / Problem Details、Application Use Case、Permission、own scope、
optimistic lock、Business Auditを使用する。React route guard、BFF認証状態またはtoken claimだけを最終業務認可にしない。

P3-C1ではO1〜O10のPhase 4固有の選択・具体値を解決済みと扱わず、Bearer代表profileの限定Evidence、未決定事項および
Phase 4への引継ぎをclose記録へ残す。Phase 4でReact / Next.js production codeを開始する前にO1〜O9を再評価する。
外部Providerが認証だけを提供し、KOIKI API向けAccess Token、audience、scopeまたはlifecycleを提供できない場合も、
直ちにKOIKI-hosted issuerを採用せず、external Authorization Server / brokerを含めてO4〜O10で比較する。

expense RESTへ`/token`、`/refresh`、`/revoke`、login password APIまたは独自JWT発行を追加しない。
Next.js BFFが独自KOIKI JWTを署名する構成も、Authorization Server責務の無承認混入となるため採用しない。

### 9.2 Document ownership and future handoff

重複する認証設計文書をP3-C0で新設せず、次の正本分担を維持する。

| Concern | Current authoritative record | Future handoff |
|---|---|---|
| SPA / BFF / Token profileとtrust boundary | `phase2-spa-sso-security-fitting.md` | accepted baselineは履歴として維持し、Phase 4でProfile Sを必須実証する。production採用profileはblocking contract reviewで具体化する |
| Access / Refresh Token lifecycleとKOIKI-hosted Authorization Serverの採否条件 | `phase2-token-lifecycle-phase-decision.md` | external provider fittingで不足が判明した場合だけoptional `P4-AS0`を開始する |
| Phase 3 REST契約との接点、P3-C1限定Bearer Evidence、未決定事項 | 本P3-C0契約文書 §7、§9.1 | P3-C1 close記録にEvidenceと未決定事項を残す |
| production provider、client、deployment、actor mapping | 現時点では正本なし | React / Next.js production code着手前のPhase 4 blocking contract reviewで新規に確定する |

したがって、accepted済みのPhase 2 Evidenceを将来構成の仮定で書き換えず、現時点で推測的なproduction認証文書も
作成しない。Phase 3実行計画にはP3-C0 / P3-C1の完了事実と次Gateへの導線だけをOwner承認後に反映し、
O1〜O10の詳細な選択結果はPhase 4の現行契約文書を正本とする。

## 10. Implementation sequence and commit point

1. P3-C0 decision D1〜D13をOwner承認し、文書commit pointを閉じる。
2. Reference appへAPI Starterを追加し、3 endpointと独立REST DTO / mapperを実装する。
3. known `ExpenseFailure` adviceとAPI security Problem Details handlerを実装する。
4. P3-C1代表profileの`/api/**` Bearer chainとReference JWT principal adapterを実装し、browser / fallback chainとの
   非重複を確認する。production SSO / token lifecycleは実装しない。
5. Controller slice、Security、Application同値、PostgreSQLおよびpackage済みHTTP journeyを追加する。
6. Root Reactor `clean verify`、log / Audit / DB突合、artifact / Public API inventoryを完了する。
7. P3-C1 close review後に実装一式をcommitし、P3-C2へ進む。

## 11. Verification plan and acceptance

| Layer | Required check |
|---|---|
| Version / routing | `/api/v1`成功、versionなし・未知version・`/api/1`非契約path、3 endpoint以外なし |
| DTO / Jackson | valid JSON、invalid JSON、unknown property、null、boundary、date、64-bit amount、server-generated line ID |
| Validation / domain | syntax 400、business 422、total不一致、inactive master、future date、empty lines |
| Authorization | P3-C1限定issuerのvalid Bearer、signature / issuer / audience / time、ID Token、scope、disabled / unknown user、401 / 403 |
| Profile separation | P3-C1代表profileではCookieだけでAPI不成立、Bearerでbrowser path不成立、API CSRF exemptionがbrowserへ波及しない |
| Ownership scope | own detail 200、unknown / other actor 404同値、PII非露出 |
| Application parity | MVC / RESTが同じcreate / submit Use Case、invariant、state、version、event、Audit結果を使用 |
| Optimistic lock | current version成功、stale version 409、後発のstate / version / event / Audit副作用0 |
| Problem Details | status / content type / code / instance、safe detail、rejected value / internal detail非露出 |
| PostgreSQL | row / line / state / version、scope、Business Audit、failure rollback |
| Package / root | executable JAR HTTP、Root Reactor、Framework public-api / distribution unit不変 |

P3-C1 closeには上表がPASSし、次を満たす必要がある。

1. 3 endpointだけが`/api/v1`で動作し、MVCと同じApplication Use Caseを使う。
2. Bearer JWTとbrowser Sessionが混在せず、`EXPENSE:APPLY`、immutable actor、own scopeを維持する。
3. create 201、detail 200、submit 204および400 / 401 / 403 / 404 / 409 / 422 / 503の意味が契約どおりである。
4. expectedVersion競合が409となり、後発操作にDB / event / Audit副作用がない。
5. Framework / Reference / Tooling Ownership、Jackson 3、Problem Detailsおよび情報非露出を維持する。
6. Architecture OwnerがHTTP Evidence、MVC同値性、Security、log / Audit / DB結果を承認する。
7. P3-C1のBearer Evidenceをproduction SSO / frontend profile決定と誤認せず、§9.1の継続事項をPhase 4へ引き継ぐ。

## 12. Deferred scope

- edit、approve、reject、return、reopen、settle、list / searchおよびbulk REST API。
- ETag / `If-Match`、conditional GET、Idempotency-Key、pagination、sorting、filtering。
- OpenAPI document / UI公開、SDK generation、API deprecation / sunsetの実例。
- Phase 4必須のsame-origin Profile S実証、Next.js BFF / direct Token SPA候補の評価、production採用profile、
  origin / route、CORS allowlist、CSRFおよびfrontend token lifecycle。
- external OIDC Provider / Authorization Serverの選択と接続、client registration、production audience / scope / claim、
  token issue / refresh / revoke、production key / secretおよびprovider固有Adapter。
- optional `P4-AS0`未承認のKOIKI-hosted Authorization Server、token table、token endpointおよびsigning key。
- Framework JWT-to-`FrameworkPrincipal`共通adapter、external link lookup Public API、API security error code Public API。
- Customer endpoint、Project Template、MyBatis accounting、Oracle / AWS、Spring Modulith Level 2 / 3。
- workflow、required check、remote push / PR / merge、ruleset変更およびsnapshot publish。

## 13. Architecture Owner decisions

| ID | Decision | Recommendation | Status |
|---|---|---|---|
| P3-C0-D1 | 外部契約をGET detail、POST create、POST submitの3本だけに限定する | APPROVE | APPROVED（2026年9月15日） |
| P3-C0-D2 | `/api/v{version:[1-9][0-9]*}` + handler version `1`で正規pathを`/api/v1`とし、独自parserと`/api/1`互換保証を追加しない | APPROVE | APPROVED（2026年9月15日） |
| P3-C0-D3 | REST request / responseを別recordとし、MVC / Application / Domain / JPA型をJSONへ直接公開しない | APPROVE | APPROVED（2026年9月15日） |
| P3-C0-D4 | createはline IDをserver生成し201 + ID + Location、detailはapplicant PIIを除いて200、submitは204とする | APPROVE | APPROVED（2026年9月15日） |
| P3-C0-D5 | expectedVersionをsubmit bodyで必須とし、staleを副作用なし409にする。ETag / If-Matchはdeferする | APPROVE | APPROVED（2026年9月15日） |
| P3-C0-D6 | P3-C1では`/api/**`をstateless Bearer専用とする代表profileを限定実証する。このmappingはPhase 3 Evidenceであり恒久的なREST認証契約ではない。Phase 4必須のsame-origin Profile Sでは同じREST endpointをCookie Session認証 + CSRFで利用できる構成を実証する。各deployment profile内でCookieとBearerをfallbackさせず、排他的なSecurity構成とする | APPROVE | APPROVED（2026年9月15日） |
| P3-C0-D7 | `koiki_user_id`はP3-C1 Reference限定候補として既存Identityへ照合し、scopeとDB Permissionの積集合を使う。production claim / `issuer + subject` mappingはPhase 4で再決定する | APPROVE | APPROVED（2026年9月15日） |
| P3-C0-D8 | 全3 endpointを`EXPENSE:APPLY`、own detail / submit scope、unknown / other actor同一404とする | APPROVE | APPROVED（2026年9月15日） |
| P3-C0-D9 | syntax 400、auth 401 / 403、not found 404、state / lock 409、business input 422、dependency 503を採用する | APPROVE | APPROVED（2026年9月15日） |
| P3-C0-D10 | RFC 9457 + stable code、safe detail、Reference既知failure advice、Framework generic fallbackを採用する | APPROVE | APPROVED（2026年9月15日） |
| P3-C0-D11 | API StarterだけをReferenceへ追加し、Framework / BOM / Public API / migrationを変更しない | APPROVE | APPROVED（2026年9月15日） |
| P3-C0-D12 | MVC同値、Bearer negative、PostgreSQL、package HTTP、log / Audit / DB、Root aggregateをP3-C1 acceptanceとする | APPROVE | APPROVED（2026年9月15日） |
| P3-C0-D13 | Phase 4 ReferenceでProfile Sを必須実証するbaselineを維持しつつ、§9.1のproduction profile、React / Next.js構成、SSO、Access / Refresh Tokenの未決定事項を追跡し、Phase 4 blocking reviewまたはoptional `P4-AS0`で明示判断する | APPROVE | APPROVED（2026年9月15日） |

**Decision:** APPROVED — P3-C0 CONTRACT COMPLETE / P3-C1 READY

**Decided by:** Shuichi Kataoka, Architecture Owner

**Decision date:** 2026年9月15日

P3-C0-D1〜D13を一体として承認し、P3-C0を`COMPLETE / OWNER APPROVED`とする。次に許可されるのはP3-C1の
Reference / Tooling限定実装と検証だけであり、P3-C2以降、workflow、required checkまたはremote操作を先行しない。

## 14. Third-party primary sources

- Spring Framework API Versioning: `https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-config/api-version.html`
- Spring Framework `SemanticApiVersionParser`: `https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/web/accept/SemanticApiVersionParser.html`
- Spring Security JWT Resource Server: `https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html`
- RFC 9457 Problem Details for HTTP APIs: `https://www.rfc-editor.org/rfc/rfc9457.html`
- RFC 9700 OAuth 2.0 Security Best Current Practice: `https://www.rfc-editor.org/rfc/rfc9700.html`
- RFC 10017 OAuth 2.0 for Browser-Based Applications: `https://www.rfc-editor.org/rfc/rfc10017.html`
- OpenID Connect Core 1.0: `https://openid.net/specs/openid-connect-core-1_0.html`
