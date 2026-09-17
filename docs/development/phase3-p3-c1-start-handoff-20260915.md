# Phase 3 P3-C1開始引継ぎ

## 1. Handoff status

- 作成日: 2026年9月15日
- 再開予定: 次回作業時
- Architecture Owner: Shuichi Kataoka
- branch: `feature/phase3-reference-vertical-slice`
- approved baseline commit: `02074e83469fe722c338aa0b1e85540faac9451c`
  （P3-C0 REST contract review承認）
- Phase status: `P3-C0 COMPLETE / OWNER APPROVED — P3-C1 READY`
- Primary ownership: Reference `expense` REST inbound adapter、Reference identity / security assembly
- Tooling ownership: API journey用issuer / key / token fixture
- immediate next action: P3-C1実装対象と再利用対象のread-only inventory

本handoffはP3-C1を別セッションで安全に開始するための導線である。P3-C0で承認したREST契約を
再設計せず、Reference / Tooling限定で最小REST APIを実装・検証する。P3-C1 close前にP3-C2以降、
Phase 4、Framework Public API、migration、workflow、required checkまたはremote設定へ進まない。

## 2. Required reading order

1. `AGENTS.md`
2. `docs/agent/skills/koiki-project-overview/SKILL.md`
3. `docs/agent/skills/koiki-business-feature-work/SKILL.md`
4. 本書
5. `KOIKI-JavaWeb-FW_Phase3実行計画_v0.1.md`のMilestone C、§26
6. `../architecture/validation/phase3-p3-c0-rest-contract-review.md`
7. `../architecture/validation/phase2-spa-sso-security-fitting.md`
8. `../architecture/validation/phase2-token-lifecycle-phase-decision.md`
9. `../reference/KOIKI-JavaWeb-FW_Reference_Application_Specification_v0.1.md`のAC-P3-10

## 3. Approved transfer baseline

1. 外部REST契約は次の3 endpointだけとする。
   - `GET /api/v1/expense-requests/{expenseRequestId}`
   - `POST /api/v1/expense-requests`
   - `POST /api/v1/expense-requests/{expenseRequestId}/submit`
2. REST Controller / DTO / mapperは`expense.adapter.inbound.api`が所有し、MVC Form、Application query record、
   Domain ModelおよびJPA EntityをJSONへ直接公開しない。
3. MVCとRESTは既存のcreate / submit Use Case、本人scope済みdetail query、`EXPENSE:APPLY`、Domain invariant、
   optimistic lock、Domain EventおよびBusiness Auditを共有する。Controllerへ業務規則を複製しない。
4. createは201 + ID + `Location`、detailは200、submitは204とする。submitはbodyの`expectedVersion`を必須とし、
   stale versionを副作用なし409にする。
5. syntax 400、auth 401 / 403、scope済みnot found 404、state / lock 409、business input 422、
   dependency 503と、RFC 9457 `ProblemDetail` + stable codeを使用する。
6. P3-C1に限り`/api/**`をstateless Bearer専用とする代表profileを実証する。これは恒久的なREST認証transportではない。
   Phase 4必須のsame-origin Profile Sでは同じendpointをCookie Session + CSRFで利用する。
7. P3-C1の各deployment profile内でCookieとBearerをfallbackさせず、browser Session chainとAPI chainを分離する。
8. `koiki_user_id`はP3-C1 Reference限定候補であり、production claim契約またはFramework Public APIにしない。
   exact token scope `expense.apply`と有効なDB Permission `EXPENSE:APPLY`の積集合だけをauthorityにする。
9. React / Next.js、production SSO、Access / Refresh Token lifecycle、actor mappingおよびoptional `P4-AS0`は
   P3-C0 §9.1の継続事項であり、P3-C1で解決済みにしない。
10. Framework Starter、BOM、Root module、Public API、migration、既存MVC DTOおよびproduction token issuerを変更しない。

## 4. Next-session start checklist

```powershell
git status --short --branch
git branch --show-current
git log -5 --oneline --decorate
git rev-parse HEAD
java -version
.\mvnw.cmd --version
docker version
```

期待値:

- branchが`feature/phase3-reference-vertical-slice`で、handoff commitがremote tracking branchと一致する。
- worktreeがcleanで、履歴にapproved baseline `02074e8`を含む。
- Java 21とRepository Maven Wrapperを使用できる。
- PostgreSQL integration testを開始する前にDocker利用可能性を確認する。

## 5. P3-C1 proposed sequence

1. 既存`ExpenseApplicationService`、`ExpenseReadService`、MVC adapter、Reference Security構成、
   `koiki-starter-api`およびPhase 2 Resource Server Evidenceをread-onlyで棚卸しする。
2. `koiki-reference-app`へAPI Starterを追加し、3 endpoint、独立REST DTO / mapperを実装する。
3. 到達可能な既知`ExpenseFailure`だけを変換するREST adviceと、API用safe 401 / 403 Problem Detailsを実装する。
4. P3-C1限定のBearer chain、JWT-to-`FrameworkPrincipal` adapterおよびTooling fixtureを実装する。
5. Controller / Jackson / versioning / Securityのfocused testを先に通す。
6. PostgreSQLでMVC同値、scope、lock競合、rollback、Business Auditを検証する。
7. package済みJARのHTTP journey、log / Audit / DB突合、artifact / Public API inventoryを完了する。
8. Root Reactor `clean verify`とP3-C1 close reviewを完了し、Owner承認後に実装一式をcommitする。

## 6. Required verification at close

- `/api/v1`の3 endpointだけが契約どおり動作し、versionなし、未知version、`/api/1`をfallbackしない。
- valid / malformed JSON、Validation、業務拒否、optimistic lockと全採用HTTP statusを検証する。
- JWT signature / issuer / audience / time / Access Token種別 / scope、ID Token拒否、unknown / disabled user、
  401 / 403を検証する。
- P3-C1ではCookieだけでAPI認証せず、Bearerでbrowser pathへfallbackせず、browser CSRFを弱めない。
- own detailだけ200、unknown IDと他actor所有IDを同じ404にする。
- MVC / RESTで同じUse Case、state、version、event、Business Audit結果になる。
- stale submitのDB / event / Audit副作用が0である。
- Problem Details、通常log、artifactへJWT、claim、Authorization header、PII、SQLまたはstack traceを露出しない。
- Reference / Tooling fixtureをFramework distribution unitまたはPublic APIへ昇格させない。

## 7. Stop conditions

- P3-C0のendpoint、DTO、status、Problem Details、lockまたは認証profile境界を実装都合で変更する。
- REST ControllerからRepositoryを直接呼ぶ、またはMVC Controller / Form / View DTOを共有する。
- `koiki_user_id`、issuer、scope、fixture keyまたはtokenをproduction / Framework契約へ昇格する。
- Session CookieとBearerを同じrequest pathでfallbackさせる、またはAPI CSRF exemptionをbrowser chainへ波及させる。
- expense RESTへlogin、`/token`、`/refresh`、`/revoke`または独自JWT発行を追加する。
- test key、token、password、Cookie、Session IDまたはCSRF tokenを文書、log、artifactへ保存する。
- P3-C1 close前にP3-C2、Phase 4、Framework Public API、migrationまたはremote設定を変更する。
- Ownerの個別承認なしにremote push / PR / merge、workflow dispatch、ruleset変更またはsnapshot publishを行う。

## 8. Immediate next action

次回はproduction code編集から即開始せず、§5手順1のread-only inventoryで既存Use Caseのsignature、DTO変換点、
SecurityFilterChainのmatcher / order、API Starterの自動構成および再利用可能なPhase 2 test patternを特定する。
棚卸し結果がP3-C0契約と矛盾しないことを確認してから、同手順2のReference REST実装へ進む。
