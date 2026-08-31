# KOIKI-JavaWeb-FW Phase 2 Security Foundation 実行計画

**状態:** `GATE P2-1 / P2-F1 COMPLETE / P2-F2〜F4・GATE P2-2 OWNER REVIEW REQUIRED`
**作成日:** 2026年8月31日
**開始branch:** `feature/phase2-security-foundation`
**開始基準main:** `b2e2123605e4d971c3ed5ccc729f668d91189d83`

## 1. Purpose and boundary

Phase 2 DoD 2-1〜2-12を、production実装前のKOIKI-PYFW Security Fitting workstreamと、
Authentication / Authorization、Local Identity / Session / Audit、Oracle / Migrationの3 implementation milestoneへ
分けて進める。Gate F / Gate P2-2承認前はproduction code、Public API、production migration、workflow、
remote environment、secret、snapshot、pushを変更しない。

Preflight Evidenceは`../architecture/validation/phase2-start-preflight.md`を正本とする。
KOIKI-PYFW fitting Evidenceは`../architecture/validation/phase2-koiki-pyfw-security-fitting.md`を正本とする。
React SPA / SSO profile Evidenceは`../architecture/validation/phase2-spa-sso-security-fitting.md`を正本とする。
`phase2-security-foundation-start-handoff-20260830.md`は開始時点のhistorical baselineとして保持し、milestone、CP、
SPA / token scopeまたは承認状態が本計画と異なる場合は、Preflight Evidence、ADR、Grand Design更新、本計画を現行判断とする。

## 2. Gate P2-2 proposal

Architecture Ownerへ次を一括して承認依頼する。

### 2.1 Recommended decisions

1. Spring Boot 4.1.1を維持し、Security 7.1.1、Session 4.1.1、Testcontainers 2.0.5等はBoot BOM管理を使う。
2. 最初の新規成果物は`koiki-starter-security`とし、Milestone Aの最小fixtureと同じCPでだけ追加する。
3. local / application-direct OIDC browser chain、Bearer API chain、fallback deny chainを分離する。Edge Authenticationは
   検証済みPre-AuthenticationのcontractだけをPhase 2で定義し、cloud固有Adapterなしにraw headerを認証へ使用しない。
4. SAML、Redis、SPA固有token保管 / CSRF、MFA有効化を除外する。Authorization Server、token発行、
   refresh / rotation / reuse検知 / revokeはP2-F4でphaseを明示決定し、現行Phase 2 production scopeへ暗黙に含めない。
5. identity / audit / single executionのPublic APIはMilestone B開始時に型単位で再reviewし、空の`-api` / `-impl`を作らない。
6. Spring Session schema自動初期化とWeb process内cleanupを無効化し、Framework Flywayとnon-web single executionを正本にする。
7. business auditは同一transaction、security auditは`REQUIRES_NEW`とし、実DB rollback対比をGate条件にする。
8. Reference `identity`はTier 1 JPA、Reference-owned UI / Use Caseとし、Framework table / migrationを所有しない。
9. Oracle Freeはnightly設計適合smokeに限定し、共通DDL失敗を実測するまでvendor分岐しない。
10. Spring Security 7 MFAはPhase 2で有効化せず、factor lifecycleのacceptance承認時に再判断する。
11. KOIKI-PYFWとのparityはendpoint / class / tableの移植ではなく、利用者向け能力とsecurity invariantのfittingとする。
12. email addressはlogin identifierとして扱い、Framework内部のimmutable user ID、audit actor、external identity linkから分離する。

### 2.2 Owner choices still required

| Choice | Recommended | Alternative / impact |
|---|---|---|
| OIDC test provider | required CIはcredential不要のlocal ephemeral issuer。Amazon Cognito User Poolは標準OIDC Providerの任意hosted acceptance候補 | hosted acceptanceは実環境に近いがsecret、可用性、redirect URI管理が必要 |
| Security audit failure | login成功、reset token発行、管理解除はfail closed | best-effort継続は監査欠損を許すため例外承認が必要 |
| Session table | `koiki_session` / `koiki_session_attributes`候補 | Spring既定名を使う場合はFramework管理例外表へ記録 |
| Single execution | vendor-neutral contract + DB別internal adapterをfixture比較 | PostgreSQL専用Public APIはOracle適合と衝突 |
| Oracle image | `gvenzl/oracle-free`のversion + digest固定、Linux hosted runner | Oracle公式registryはlicense受諾 / pull認証がCI運用を増やす |
| Token lifecycle phase | Phase 2はfitting / contract gapまで、発行・refresh・revoke実装は後続phase | Phase 2へ前倒しする場合はDoD、Authorization Server、key / client運用、工数を追加review |
| Login identifier | emailを入力ID、別のimmutable IDを内部参照に使用 | emailを主キー化すると変更・監査・IdP link・PII管理が不安定になる |

### 2.3 Accepted SPA architecture baseline

ADR-006〜008により、same-origin Session、Customer-owned Next.js BFF、direct Tokenの3 profileを許容する判断は
ACCEPTEDである。業務・PII用途ではSession / BFFを優先し、direct Tokenは明示risk acceptanceを要する。
Gate P2-2で未決定なのはprofile自体の採否ではなく、OIDC test provider、CORS property、logout semantics、
profile別fixture、およびPhase 4でNext.js BFF参照実装を作るかである。

## 3. Ownership and module plan

| Area | Owner | Boundary |
|---|---|---|
| Security defaults / profile orchestration | Framework | Spring Security標準を構成。Customer route / business policyを含めない |
| User / Role / Permission / credential / lock / reset | Framework | `koiki_` table、公開contract、internal persistence |
| Audit API / persistence | Framework | transaction分類を契約化。通知・外部連携は含めない |
| Session / cleanup | Framework | Spring Session JDBC、Framework migration、single execution job |
| `identity` | Reference | Tier 1 JPAのController / Use Case / view。Framework schemaへ直接依存しない |
| Customer identity / claims / policy | Customer | 社員番号、所属、provisioning、IdP固有mapping、resource ownership |
| Security acceptance fixtures | Tooling | 非配布。正式Referenceへ昇格しない |

Reference `identity`の作業判断:

```text
Ownership / module: Reference / identity
Tier and triggers: Tier 1 SIMPLE。管理操作の調整で開始し、domain.modelを作らない
Responsibility placement: Controller -> Application Use Case -> Framework public contract
Persistence / model: Frameworkが所有。Reference Repository / migrationを作らない
Read model: 画面に必要な最小Application DTO
Module collaboration: Framework public contractのみ。Framework internal / JPA Entityを参照しない
View / API boundary: EntityをMVC Modelへ出さない。Phase 3のHTMX / RESTを先行しない
Verification: architecture、method security、direct request、PostgreSQL integration、audit rollback
Deferred decisions: Customer属性、claim mapping、provisioning、SPA、業務固有policy
```

## 4. Fitting workstream, milestones and commit points

### Pre-implementation Workstream F — KOIKI-PYFW Security Fitting

| CP | Scope | Exit criteria |
|---:|---|---|
| P2-F1 | Python capability / invariant inventory | source commit固定、採用 / Spring置換 / 非採用 / deferredが全項目で明示 |
| P2-F2 | identity / API / SPA / SSO semantics | email / immutable ID、external subject、trust source、Permission、profile別CORS / logout / error / audit semanticsをOwner review |
| P2-F3 | Spring replacement test design | local issuer、Cognito direct OIDC、Edge Authenticationを区別したtest topology、Spring component mapping、threat / negative-path matrix、dependency候補、stop conditionを文書化。code / dependency変更は0 |
| P2-F4 | token lifecycle phase decision | Resource ServerとAuthorization Serverを分離し、発行 / refresh / rotation / reuse / revokeのphaseとdependencyを決定 |
| Gate F | fitting acceptance | mapping、threat、negative path、deferred backlog承認。Gate P2-2判断へ接続 |

Workstream FはPython repositoryを変更せず、Python endpoint互換APIやJWT Cookie方式をJava成果物へ固定しない。
P2-F3ではfixture codeを作らず、実証はGate P2-2後のP2-A1〜A3へ一元化する。
P2-F1は`phase2-koiki-pyfw-security-fitting.md`で完了し、P2-F2〜F4はOwner review対象とする。

### Gate P2-2 — minimum implementation approval

Gate F、本計画、§2.2のOwner choicesを承認してからimplementation milestoneを開始する。

### Milestone A — Authentication / Authorization profiles

| CP | Scope | Exit criteria |
|---:|---|---|
| P2-A1 | dependency / artifact fixture、default deny、header / CSRF defaults | Boot-managed tree、条件別ApplicationContext、未認証URL拒否、明示override、Public API 0候補 |
| P2-A2 | email + password local session login、URL / Method Security、Role / Permission | DoD 2-1、2-2、2-9。UI回避direct requestも拒否 |
| P2-A3 | OIDC共存、Bearer JWT、SPA / BFF / Edge境界 | DoD 2-3、2-4。標準OIDC ProviderとしてのCognito適合候補、署名 / issuer / audience / time / scope、ID Token / Cookie / raw edge header fallback、CORS negative path |
| Gate A | aggregate / PR | root、Consumer、Public API、Java 21 / 25、secret non-exposure、required checks |

Milestone Aは1 PRを上限とする。OIDC providerまたはprofile matcherが確定しない場合、P2-A3を実装せずGateを停止する。

### Milestone B — Local Identity / Session / Audit

| CP | Scope | Exit criteria |
|---:|---|---|
| P2-B1 | Audit contractとtransaction fixture | 実PostgreSQLで2-6 / 2-7のrollback対比、failure semantics |
| P2-B2 | User / Role / Permission、Password / Lock / Reset、attempt制御、migration | email / immutable ID分離、raw secret非保存、enumeration防止、並行失敗閾値、optimistic lock、所有権完全一致 |
| P2-B3 | Spring Session JDBC、2 instance、logout、cleanup / single execution | DoD 2-5、2-8。Web cleanup競合なし、片instance停止後も継続 |
| P2-B4 | Reference `identity` | DoD 2-10。Tier 1、Framework contract経由、管理操作のMethod Security / audit |
| Gate B | aggregate / PR | DoD 2-5〜2-8、2-10、packaged journey、Public API inventory / japicmp方針 |

P2-B1でPublic API候補を型単位reviewする。P2-B2以降を先行して契約を既成事実化しない。P2-B3ではPhase 1b Consumerの
lock codeをcopyせず、EvidenceからFramework production contractを再実装する。

### Milestone C — Oracle / Migration / closeout

| CP | Scope | Exit criteria |
|---:|---|---|
| P2-C1 | Oracle SQL rule、第三者table一覧、Oracle fixture | DoD 2-11、共通DDL、review checklist、table完全一致 |
| P2-C2 | Oracle nightly、CRUD / paging / optimistic lock、OpenRewrite試作 | DoD 2-12、version / digest / runtime固定、通常CIから分離 |
| P2-C3 | Developer Journey / DoD closeout | 全DoD trace、Skill / ADR、release unit、remote evidence、deferred一覧 |
| Gate C | final PR / main | required checks、nightly evidence、Owner approval、merge後main CI |

Oracle imageまたはlicenseを確定できない場合、DoD 2-12を削らずP2-C2をholdし、代替fixture、risk、再開条件を記録する。

## 5. Dependency inclusion / exclusion

### Include candidates

- `spring-boot-starter-security`
- `spring-boot-starter-security-oauth2-client`
- `spring-boot-starter-security-oauth2-resource-server`
- `spring-boot-starter-session-jdbc`
- test scopeの`spring-boot-starter-security-test`
- test scopeの`testcontainers-oracle-free`、`ojdbc11`、`flyway-database-oracle`

すべてBoot BOM管理を使用し、採用CPで実効tree、license、Java 21 build、Java 25 runtimeを確認する。

### Explicit exclusions

- 現行Phase 2 production scopeのSpring Authorization Server、SAML、Redis Session
- Keycloak等のproduction runtime依存
- AWS ALB＋Cognito production Adapter、実ALB environment、Cognito Identity Pool / AWS一時credential連携
- 独自JWT parser、独自Password hash、独自CSRF framework
- WebFlux / Reactor security stack
- Python版の自前JWT encode / decode、JWT CookieをHTTP Sessionと呼ぶ方式、endpoint / migrationの直接移植
- 現行Phase 2 production scopeでのrefresh token、token発行 / revoke、React / Next.js production実装、
  persistent browser token storageのKOIKI標準化。
  P2-F4で後続phaseへtraceableに割り当て、Phase 2へ前倒しする場合はGate P2-2を再承認する
- Spring Modulith Level 2、async audit / notification

## 6. Migration / Oracle strategy

1. Framework migrationsは既存`koiki-starter-data`の`db/migration/koiki`と`koiki_flyway_history`を使用する。
2. Spring Sessionを含む第三者schema initializerは無効化する。
3. production SQLを追加する同じCPでPostgreSQL / Oracle migration testとtable ownership inventoryを追加する。
4. ID、timestamp、Boolean代替、version、index、文字列長を両DBで実測する。
5. vendor分岐は共通DDLの具体的失敗、影響、代替をOwnerへ提示してから導入する。
6. Oracle nightlyはLinux / Java 21、image version + digest、driver version、Flyway versionをEvidenceへ固定する。

## 7. Verification commands proposal

実際のscript名と`-pl`は各CPのmodule追加時に確定する。検証層は次を維持する。

```powershell
.\mvnw.cmd --batch-mode --no-transfer-progress -pl <focused-modules> -am verify
.\mvnw.cmd --batch-mode --no-transfer-progress verify
pwsh -NoProfile -File build-support/api-compatibility/verify-public-api-compatibility.ps1
pwsh -NoProfile -File build-support/null-safety/verify-null-safety.ps1
```

Milestone A / Bでは専用aggregateから隔離Maven repositoryへ成果物をstageし、Root Reactor外Consumerをbuildする。
Milestone BはPostgreSQL containerとpackage済み2 processを起動する。Milestone C nightlyだけOracleを起動する。
CP9 performance公式baselineとPhase 1b snapshotは再採取しない。

## 8. CI and remote-operation boundary

- focused validationと通常root verifyはlocalで実行する。
- workflow追加、required check変更、environment / secret作成、push、PR、merge、snapshot publishは個別Owner承認後だけ行う。
- Milestone A / Bのintegration jobは3回連続成功とcleanup確認後にrequired化をreviewする。
- Oracle nightlyはrequired PR checkへ入れず、release blockerとして別運用する。
- CI permissionsは原則`contents: read`。package / environment権限を通常CIへ追加しない。
- private key、client secret、password、token、PIIをsource、log、artifact、Problem Detailsへ残さない。

## 9. ADR / Skill / documentation targets

| Target | Timing |
|---|---|
| KOIKI-PYFW fitting / token phase decision | P2-F2〜F4。既存Grand Designの明確化で足りなければADR |
| SPA Session / BFF / direct Token profile | P2-F2。Grand Design §13.5 / §14.2、ADR-006〜008の進展をEvidenceへ接続 |
| Security profile / artifact ADR | P2-A1 |
| Identity / Audit / transaction ADR | P2-B1 |
| Session / cleanup / single execution ADR | P2-B3 |
| MFA decision record | P2-A1 |
| Oracle environment / nightly ADR or ADR-044 update | P2-C1 |
| Security Agent Skill | P2-A1。判断、secret境界、検証順だけを記述 |
| Business Feature Skill | P2-B4で新規規約が生じた場合だけ更新 |
| README / validation / third-party table list | 各CPとP2-C3 |

既存ADRの実装証拠だけで足りる場合は手続き目的のADRを増やさない。

## 10. Estimate and recalibration

Feasibilityの開始rangeは直接115〜189標準人日、contingency込み150〜246標準人日、AI支援Owner 90〜185日である。
Phase 1bは全CPの同一基準でOwner稼働を計測していないため、推測で係数を短縮しない。

Workstream FはFeasibilityの`Phase共通` 15〜25標準人日に含まれるthreat model、identity設計、共通Security成果物の
具体化であり、現時点では総rangeへ加算しない。P2-F1は文書inventoryとして完了したが同一基準の実績時間は未採取である。
P2-F2〜F4は設計・test matrix・phase allocationに限定し、fixture codeをP2-A1〜A3へ一元化することで二重計上を防ぐ。
P2-F3へcode spikeを再導入する、またはAuthorization ServerをPhase 2へ前倒しする場合は、Gate P2-2前にrangeを再見積もりする。

Phase 1bから次の実績だけを計画へ反映する。

- remote CIを各milestoneへまとめ、local positive / negative / restoreを主経路にする。
- PostgreSQL jobは3回連続成功後にrequired化を判断する。
- package済みConsumer、空repository、実processのEvidenceを内部testと分離する。
- rework、CI wait、container cleanup、Public API inventoryをCPごとに記録する。

各Gateで残rangeを再校正し、Security、2 instance、Oracleの実測時間を得るまで納期commitmentへ変換しない。

## 11. Stop conditions

- Gate P2-2承認前にproduction差分を作る。
- Gate F前にPython class / endpoint / tableをJava Public API / production migrationへ写す。
- Security経路を1 chainへ無計画に混在させる、またはunmatched pathを許可する。
- 同じpathでSession Cookie、BFF / SPA Bearer、ID Tokenをfallback認証する。
- direct Token SPAを業務・PII用途の無条件defaultにする、またはfrontend bundleへclient secretを置く。
- `x-amzn-oidc-*`等のraw edge headerを、署名、期待するedge識別子、到達経路の検証なしに認証へ使用する。
- Customer route / claim / business policyをFrameworkへ固定する。
- Consumerのsingle-execution codeをFrameworkへcopy / 昇格する。
- ReferenceがFramework Entity、Repository、internal package、migrationを所有・参照する。
- session schema initializerとFramework Flywayを併用する。
- Web instance cleanupとnon-web cleanupを競合させる。
- audit分類をtest名だけで済ませ、実DB rollbackを確認しない。
- raw password / reset token / JWT / private key / client secret / PII全文を保存・出力する。
- 共通DDL失敗のEvidenceなしにOracle vendor分岐を作る。
- Oracle smokeを本番Oracle正式supportと表現する。
- Phase 3以降、SPA、Authorization Server、Level 2、cloud固有実装をP2-F4 / Gate P2-2判断なしに先行する。

## 12. Owner review request

Gate P2-2では§2.1の12判断、§2.2の7 choices、§2.3のACCEPTED baseline、Workstream Fのfitting結果を確認する。
承認後の最初のproduction差分はP2-A1に限定し、
`koiki-starter-security`の最小artifact / dependency fixture、default deny、CSRF / header defaults、
negative tests、Public API inventoryまでとする。local identity、production migration、Reference、workflowは同じ差分へ含めない。
