# KOIKI-JavaWeb-FW Phase 2 Security Foundation 実行計画

**状態:** `GATE P2-1 / P2-F1〜F4 COMPLETE / GATE F COMPLETE / GATE P2-2 APPROVED — P2-A1 READY`
**作成日:** 2026年8月31日
**開始branch:** `feature/phase2-security-foundation`
**開始基準main:** `b2e2123605e4d971c3ed5ccc729f668d91189d83`

## 1. Purpose and boundary

Phase 2 DoD 2-1〜2-10を、production実装前のKOIKI-PYFW Security Fitting workstreamと、
Authentication / Authorization、Local Identity / Session / Audit、PostgreSQL Migration / closeoutの3 implementation milestoneへ
分けて進める。Gate Fは完了したが、Gate P2-2承認前はproduction code、Public API、production migration、workflow、
remote environment、secret、snapshot、pushを変更しない。

旧DoD 2-11（Oracle互換SQL規約）と2-12（Oracle nightly）は2026年8月31日のArchitecture Owner判断でPhase 2から
除外した。Oracleは採用確度の低い将来optional patternとしてtraceabilityだけを保持し、明示Customer要件と優先度に基づく
optional `P4-ORACLE` Gateが承認されるまで、実装、依存、Image、MigrationまたはCI対象にしない。

Preflight Evidenceは`../architecture/validation/phase2-start-preflight.md`を正本とする。
KOIKI-PYFW fitting Evidenceは`../architecture/validation/phase2-koiki-pyfw-security-fitting.md`を正本とする。
React SPA / SSO profile Evidenceは`../architecture/validation/phase2-spa-sso-security-fitting.md`を正本とする。
P2-F2 identity / API / SPA / SSO semantics Evidenceは
`../architecture/validation/phase2-security-semantics-fitting.md`を正本とする。
P2-F3 Spring replacement test design Evidenceは
`../architecture/validation/phase2-security-test-design.md`を正本とする。
P2-F4 token lifecycle phase decision Evidenceは
`../architecture/validation/phase2-token-lifecycle-phase-decision.md`を正本とする。
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
9. PostgreSQL／Aurora PostgreSQLをproduction baselineとし、Oracleはoptional `P4-ORACLE` Gate前に具体化しない。
10. Spring Security 7 MFAはPhase 2で有効化せず、factor lifecycleのacceptance承認時に再判断する。
11. KOIKI-PYFWとのparityはendpoint / class / tableの移植ではなく、利用者向け能力とsecurity invariantのfittingとする。
12. email addressはlogin identifierとして扱い、Framework内部のimmutable user ID、audit actor、external identity linkから分離する。

### 2.2 Gate choices and approval status

| Choice | Recommended / accepted decision | Alternative / impact | Status |
|---|---|---|---|
| OIDC test provider | required CIはcredential不要のlocal ephemeral issuer。Amazon Cognito User Poolは標準OIDC Providerの任意hosted acceptance候補 | hosted acceptanceは実環境に近いがsecret、可用性、redirect URI管理が必要 | **APPROVED GATE F F-3** |
| Security audit failure | login成功、reset token発行、管理解除はfail closed。logout / disable / invalidationは処理継続 + alert | 全best-effortは監査欠損、全fail-closedは防御的失効を妨げる | **APPROVED P2-F2 O-4** |
| Session store failure | 全Session失効を伴うmutationは永続失効不能ならrollback + safe failure / alert。logoutはlocal context / Cookieを消去するが永続削除失敗を成功扱いしない | 全best-effortはstale Sessionを残し、logout自体のfail-closedはlocal防御操作を妨げる | **APPROVED GATE F F-4** |
| Session table | `koiki_session` / `koiki_session_attributes`をFramework Flywayで管理し、Spring Session schema自動初期化を無効化。列型、index、save mode、PostgreSQL DDLはP2-B3 / C1で実測 | Spring既定名を使う場合はFramework管理例外表へ記録 | **APPROVED OWNER 2026-08-31** |
| Single execution | acquired / contended / failed等のvendor-neutral外部挙動を契約とし、PostgreSQL internal adapterを実証する。Consumer codeを昇格せずPublic APIは型単位で再reviewし、将来DB adapterはoptional Gateで判断 | 現時点でDB別adapter比較を行うと、未採用DBの抽象化を先行固定する | **APPROVED OWNER 2026-08-31** |
| Oracle phase allocation | 採用確度の低い将来optional pattern。Phase 2ではedition / version、Image、JDBC Driver、Flyway vendor module、Migration、SQL互換規約、Testcontainers、CIを選定しない。明示Customer要件と優先度が成立した場合にoptional `P4-ORACLE` Gateで再判断 | 先行対応は低確度要件の依存・CI・保守コストを中核Security実装へ持ち込む | **APPROVED OWNER 2026-08-31 — SUPERSEDES EARLIER IMAGE DECISION** |
| Token lifecycle phase | Phase 2はOAuth2 Client + Resource Serverまで。external issuerを第一標準とし、KOIKI-hosted issuerはPhase 4 optional `P4-AS`候補 | Phase 2へ前倒し、またはPhase 4必須化する場合はDoD、Authorization Server、key / client運用、工数を追加review | **APPROVED P2-F4 T-1〜T-6** |
| Login identifier | emailを入力ID、別のimmutable IDを内部参照に使用 | emailを主キー化すると変更・監査・IdP link・PII管理が不安定になる | **APPROVED P2-F2** |
| Email canonicalization | Phase 2はASCII emailをcase-insensitiveに扱い、前後空白除去 + `Locale.ROOT` lowercaseをlookup keyとする。original valueは表示用 | 国際化emailを初期対応する場合はUnicode / IDNA、case folding、採用DBの照合規則の追加設計・実測が必要 | **APPROVED P2-F2 O-1** |
| Unknown external identity | default deny。明示provisioning / linkだけ許可 | JIT作成はCustomer policy、初期Permission、重複・rollback・audit設計が必要 | **APPROVED P2-F2 O-2** |
| Permission change / Session | 対象userの全KOIKI sessionを失効 | requestごとDB照会は負荷、authorization versionは追加contract / migrationが必要 | **APPROVED P2-F2 O-3** |
| OIDC logout | local logoutを必須、RP-Initiated Logoutはprovider対応時のopt-in | IdP logout必須化はprovider差、availability、global logout影響を増やす | **APPROVED P2-F2 O-5** |
| Current-user API | Frameworkはprincipal contractだけを候補とし、endpointはCustomer / Referenceが所有 | Framework `/me`固定はDTO、PII、versioningをPublic API化する | **APPROVED P2-F2 O-6** |

#### 2.2.1 Remaining-choice Owner approval record

2026年8月31日、Architecture OwnerはSession table、Single execution、Oracleに関するphase allocationの3件を承認した。
これにより§2.2は`0 OPEN / 13 APPROVED`とする。同日に先行して承認されたOracle Free image案は、後続のOwner判断により
supersedeされた履歴として保持する。save modeとPostgreSQL内部排他方式は、承認済みのstop conditionに従ってP2-B3で
実測・記録するimplementation decisionである。Oracleの具体的patch / digestはPhase 2の未決事項ではなく、選定対象外である。

この時点の3件承認は§2.1の12判断と本計画全体を最終承認したことを意味しなかった。後続の最終承認は§12に記録する。
その承認まではproduction code、Public API、production migrationまたはworkflowを変更しない境界を維持した。

### 2.3 Accepted SPA architecture baseline

ADR-006〜008により、same-origin Session、Customer-owned Next.js BFF、direct Tokenの3 profileを許容する判断は
ACCEPTEDである。業務・PII用途ではSession / BFFを優先し、direct Tokenは明示risk acceptanceを要する。
OIDC test provider、logout semantics、profile別fixture / CI境界はGate Fまでに承認済みである。CORS propertyの具体契約は
P2-A1 / A3のApplicationContext / negative testで確定し、Next.js BFF参照実装はPhase 4 deferredとして扱う。

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
| P2-F2 | identity / API / SPA / SSO semantics | **COMPLETE**。email / immutable ID、external subject、trust source、Permission、profile別CORS / logout / error / audit semanticsをOwner承認 |
| P2-F3 | Spring replacement test design | **COMPLETE**。test topology / Fixture ownership / A1〜B3 reuse / Spring component / threat・negative path / Boot-managed dependency候補 / CI境界 / Gate F handoffを文書化。code / dependency / workflow変更は0 |
| P2-F4 | token lifecycle phase decision | **COMPLETE**。T-1〜T-6をOwner承認。Phase 2をOAuth2 Client / Resource Serverまでとし、external issuerを第一標準、KOIKI-hosted issuerをPhase 4 optional `P4-AS`として追跡 |
| Gate F | fitting acceptance | **COMPLETE**。F-1〜F-5としてmapping、threat、negative path、dependency / CI境界、deferred backlogをOwner承認。P2-F4へ接続 |

Workstream FはPython repositoryを変更せず、Python endpoint互換APIやJWT Cookie方式をJava成果物へ固定しない。
P2-F3ではfixture codeを作らず、実証はGate P2-2後のP2-A1〜A3へ一元化する。
P2-F1は`phase2-koiki-pyfw-security-fitting.md`で完了した。P2-F2は
`phase2-security-semantics-fitting.md`の本文とO-1〜O-6をOwner承認して完了した。P2-F3は
`phase2-security-test-design.md`のcompletion evidenceを満たして完了した。Gate Fは同文書F-1〜F-5のOwner承認により完了し、
P2-F4は`phase2-token-lifecycle-phase-decision.md`のT-1〜T-6と本文全体をOwner承認して完了した。これはPhase 2で
Authorization Serverを実装しない判断であり、Phase 4 optional `P4-AS`の実装開始承認ではない。

### Gate P2-2 — minimum implementation approval

Gate F、P2-F4および§2.2の全choicesは完了した。2026年8月31日、Architecture Ownerは§12の6観点から
本計画を最終承認し、Gate P2-2を通過した。implementation milestoneはP2-A1から開始し、後続CPを先行しない。

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
| P2-B3 | Spring Session JDBC、2 instance、logout、cleanup / single execution | DoD 2-5、2-8。`koiki_session*`、Web cleanup競合なし、片instance停止後も継続。vendor-neutral結果とPostgreSQL internal adapterを実証 |
| P2-B4 | Reference `identity` | DoD 2-10。Tier 1、Framework contract経由、管理操作のMethod Security / audit |
| Gate B | aggregate / PR | DoD 2-5〜2-8、2-10、packaged journey、Public API inventory / japicmp方針 |

P2-B1でPublic API候補を型単位reviewする。P2-B2以降を先行して契約を既成事実化しない。P2-B3ではPhase 1b Consumerの
lock codeをcopyせず、EvidenceからFramework production contractを再実装する。

### Milestone C — PostgreSQL Migration / packaging / closeout

| CP | Scope | Exit criteria |
|---:|---|---|
| P2-C1 | PostgreSQL Migration、第三者table一覧、upgrade / clean fixture | Framework Flyway正本、所有権完全一致、Spring initializer無効、clean install / supported upgradeを実証 |
| P2-C2 | package / Consumer / OpenRewrite試作 | 配布単位、Root Reactor外Consumer、Public API互換性、Migration Support境界を実証 |
| P2-C3 | Developer Journey / DoD closeout | 全DoD trace、Skill / ADR、release unit、remote evidence、deferred一覧 |
| Gate C | final PR / main | required checks、PostgreSQL integration evidence、Owner approval、merge後main CI |

Oracle対応はMilestone Cの完了条件ではない。optional `P4-ORACLE`が将来承認された場合は、新しいDoD、見積、対象環境、
依存、MigrationおよびCI境界をそのwork package内で定義する。

## 5. Dependency inclusion / exclusion

### Include candidates

- `spring-boot-starter-security`
- `spring-boot-starter-security-oauth2-client`
- `spring-boot-starter-security-oauth2-resource-server`
- `spring-boot-starter-session-jdbc`
- test scopeの`spring-boot-starter-security-test`

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
- 現行Phase 2のOracle JDBC Driver、Flyway Oracle module、Oracle container / Testcontainers、Oracle Migration、
  Oracle SQL互換規約およびOracle CI / nightly

## 6. PostgreSQL Migration strategy

1. Framework migrationsは既存`koiki-starter-data`の`db/migration/koiki`と`koiki_flyway_history`を使用する。
2. Spring Sessionを含む第三者schema initializerは無効化する。
3. production SQLを追加する同じCPでPostgreSQL migration testとtable ownership inventoryを追加する。
4. ID、timestamp、Boolean、version、index、文字列長をPostgreSQLで実測する。
5. DB固有実装をFramework Public APIへ露出せず、未採用DB向けの共通DDLまたはvendor分岐を先行生成しない。
6. 将来DB対応は明示Gateで対象DB、要件、依存、Migration、検証環境およびsupport claimを再設計する。

## 7. Verification commands proposal

実際のscript名と`-pl`は各CPのmodule追加時に確定する。検証層は次を維持する。

```powershell
.\mvnw.cmd --batch-mode --no-transfer-progress -pl <focused-modules> -am verify
.\mvnw.cmd --batch-mode --no-transfer-progress verify
pwsh -NoProfile -File build-support/api-compatibility/verify-public-api-compatibility.ps1
pwsh -NoProfile -File build-support/null-safety/verify-null-safety.ps1
```

Milestone A / Bでは専用aggregateから隔離Maven repositoryへ成果物をstageし、Root Reactor外Consumerをbuildする。
Milestone BはPostgreSQL containerとpackage済み2 processを起動する。Milestone CもPostgreSQLを正本としてMigrationと
package / ConsumerのEvidenceを完成させる。
CP9 performance公式baselineとPhase 1b snapshotは再採取しない。

## 8. CI and remote-operation boundary

- focused validationと通常root verifyはlocalで実行する。
- workflow追加、required check変更、environment / secret作成、push、PR、merge、snapshot publishは個別Owner承認後だけ行う。
- Milestone A / Bのintegration jobは3回連続成功とcleanup確認後にrequired化をreviewする。
- Oracle jobは現行Phaseのworkflowへ追加しない。
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
| ADR-010 / ADR-044 Oracle optional scope update | Gate P2-2前。本計画とGrand Designへ反映 |
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

各Gateで残rangeを再校正し、Security、2 instance、PostgreSQL Migrationの実測時間を得るまで納期commitmentへ変換しない。

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
- optional `P4-ORACLE` GateなしにOracle依存、Image、Migration、AdapterまたはCIを追加する。
- EvidenceなしにOracle互換性、移行容易性または正式supportを表現する。
- Phase 3以降、SPA、Authorization Server、Level 2、cloud固有実装をP2-F4 / Gate P2-2判断なしに先行する。

## 12. Gate P2-2 final Owner approval record

2026年8月31日、Architecture Ownerは§2.1の12判断、§2.2の`0 OPEN / 13 APPROVED` choices、
§2.3のACCEPTED baseline、Workstream Fのfitting結果を、次の6観点から最終承認した。

1. Phase 2の実装範囲と明示的除外
2. Milestone A〜CとDoD 2-1〜2-10の実行順
3. Framework / Reference / CustomerのOwnershipと成果物境界
4. Spring Security / Spring Session標準を優先する構成方針
5. Audit transaction、Session障害、cleanup / single executionの挙動
6. 最初のproduction差分をP2-A1へ限定すること

これによりGate P2-2を`APPROVED`とし、P2-A1の開始条件が成立した。最初のproduction差分はP2-A1に限定し、
`koiki-starter-security`の最小artifact / dependency fixture、default deny、CSRF / header defaults、
negative tests、Public API inventoryまでとする。local identity、production migration、Reference、workflowは同じ差分へ含めない。

この承認はP2-A1の具体的なPublic API、property名、FilterChain matcher、または後続CPのimplementation decisionを
先行確定するものではない。それらは各CPのfixtureとEvidenceに基づいてreviewする。
