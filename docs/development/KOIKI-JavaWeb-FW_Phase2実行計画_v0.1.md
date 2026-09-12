# KOIKI-JavaWeb-FW Phase 2 Security Foundation 実行計画

**状態:** `PHASE 2 COMPLETE / ACCEPTED`
**作成日:** 2026年8月31日
**最終更新日:** 2026年9月13日
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
| Application / Audit log classification | ログ種別はApplication logとDBを正本とするAudit logの2つとし、Securityを第3のログ種別にしない。Security関連事象は目的に応じてApplication log、Audit logまたは双方へ安全な別表現で記録する | Security専用ログ種別を追加すると、運用監視と監査証跡の責務、保存先、failure semanticsが重複する | **APPROVED OWNER 2026-09-02** |
| Audit contract / transaction | 単一Audit Starter、Business / Security別recorder、immutable value、safe unchecked failure、recorder façade + internal `MANDATORY` / `REQUIRES_NEW` executor、JPA `persist + flush`、DB正本を採用する | Security Starter混在、単一classification引数、result返却、JDBC例外化、外部log backend依存は誤分類・監査欠損・scope拡大を招く | **APPROVED OWNER B1-C1〜C7 2026-09-02** |
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
2026年9月2日、Application / Audit logの2分類とSecurityを横断的性質として扱う境界を承認した。
同日、P2-B1のAudit contract / transactionに関するB1-C1〜C7を推奨案どおり承認した。
2026年9月3日、実PostgreSQLのT4 Evidenceを確認してP2-B1をacceptし、同判断をADR-047として確定した。
これにより§2.2は`0 OPEN / 15 APPROVED`とする。8月31日に先行して承認されたOracle Free image案は、後続のOwner判断により
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
| P2-B2 | User / Role / Permission、Password / Lock、attempt制御、Reset設計境界、migration | email / immutable ID分離、raw secret非保存、enumeration防止、並行失敗閾値、optimistic lock、所有権完全一致 |
| P2-B3 | Spring Session JDBC、2 instance、logout、cleanup / single execution | DoD 2-5、2-8。`koiki_session*`、Web cleanup競合なし、片instance停止後も継続。vendor-neutral結果とPostgreSQL internal adapterを実証 |
| P2-B4 | Reference `identity` | DoD 2-10。Tier 1、Framework contract経由、管理操作のMethod Security / audit |
| Gate B | aggregate / PR | DoD 2-5〜2-8、2-10、packaged journey、Public API inventory / japicmp方針 |

P2-B1でPublic API候補を型単位reviewする。P2-B2以降を先行して契約を既成事実化しない。P2-B3ではPhase 1b Consumerの
lock codeをcopyせず、EvidenceからFramework production contractを再実装する。

2026年9月3日、P2-B1をacceptしてADR-047を確定し、`phase2-p2-b2-start-handoff-20260903.md`でP2-B2の
contract / table review、実装・検証順、Gate BおよびMilestone Cまでのtask mapを開始した。
同日、`../architecture/validation/phase2-p2-b2-contract-review.md`でB2-C1〜C10の具体的比較・推奨案を作成し、
Architecture Ownerが全項目を推奨案どおり承認した。B2-1を完了し、B2-2 Identity core / migrationを開始可能とする。
初期適用projectはSSO認証を想定するため、B2-C7ではlocal resetの安全条件を設計に保持しつつ、reset専用Public API、
token / mail delivery、table、property、endpointのproduction実装を将来要件成立時の別CPへdeferする案とした。
2026年9月7日、`../architecture/validation/phase2-p2-b2-b2-2-verification.md`のIdentity core / migration、Public API 10型、
8 table、実PostgreSQL T4および配布境界をArchitecture Ownerがreviewし、B2-2を承認した。次はB2-3
Authentication / attempt / lockへ進む。
同日、B2-3 Authentication / attempt / lockとB2-4 Identity administrationをArchitecture Ownerが承認した。
`../architecture/validation/phase2-p2-b2-b2-5-verification.md`ではT0〜T4 56 / 56、root回帰、NullAway正負、
Public API / property / error / table inventory、secret / PIIおよびcleanupを再検証し、B2-5のOwner review対象とした。
同日、失敗時reportのSecret / PII境界をToolingで補強し、負例と56 / 56正常回帰を確認した。Architecture Ownerは
B2-5の5項目を最終承認し、P2-B2を`COMPLETE / ARCHITECTURE OWNER APPROVED`としてcloseした。次はP2-B3へ進む。
同日、`phase2-p2-b3-start-handoff-20260907.md`でSpring Session JDBC、全Session失効、2 process継続、
cleanup / single executionの実装・検証順とB3-C1〜C10の事前review境界を整理し、B3-1を開始した。
`phase2-p2-b3-contract-review.md`にartifact、Public API、schema、serialization、失敗時挙動、cleanup、
single execution、設定、T5 / T6およびCI境界の比較・推奨案を作成し、Architecture Owner review対象とした。
Architecture Ownerは同日B3-C1〜C10を推奨案どおり承認し、B3-1を`COMPLETE`としてB3-2 Session core / migrationを開始可能とした。
2026年9月8日、B3-2ではoptional `koiki-starter-session-jdbc`、Framework所有のSession 2 table migration、
initializer / table / Web cleanupのFail Fast境界を実装した。実PostgreSQLで`ON_SAVE` / `ON_SET_ATTRIBUTE`のwrite境界と
immutable principal ID / credential非永続化を確認し、T0〜T5 15 suite / 62 testsが成功した。
`../architecture/validation/phase2-p2-b3-b3-2-verification.md`をArchitecture Owner review対象とし、
全Session失効 / logout、2 process、maintenance cleanupはB3-3〜B3-5へ残す。
同日、Architecture OwnerはB3-2の5 review pointsと外部TLS終端 / Secure Cookieの補足を確認し、B3-2を
`COMPLETE / ARCHITECTURE OWNER APPROVED`とした。次はB3-3 Invalidation / logoutへ進む。
同日、B3-3では既存`UserSessionInvalidator`をSpring Session JDBCのimmutable principal indexへ接続し、対象userだけの
全Session同期失効とSpring Security標準chain上の永続local logoutを実装した。実PostgreSQLとMockMvcによるServlet filter chain上の
HTTP動作でSession row削除、Cookie失効、同一processでの旧Cookie拒否を確認し、注入したSession invalidate障害ではlocal context / credential / Cookieを
消去しつつ成功redirectを止めることを確認した。T0〜T5 15 suite / 66 testsが成功し、
`../architecture/validation/phase2-p2-b3-b3-3-verification.md`をArchitecture Owner review対象とした。
package済み2 processでの継続 / 旧Cookie拒否と実store障害のHTTP結果はB3-4、cleanup / single executionはB3-5へ残す。
同日、Architecture OwnerはB3-3の5 review pointsを確認し、MockMvcによるServlet filter chain上のHTTP動作と
package済みprocessへのnetwork越しHTTP試験の証拠境界を明確化したうえで、B3-3を
`COMPLETE / ARCHITECTURE OWNER APPROVED`とした。次はB3-4 Two-process continuityへ進む。
同日、B3-4ではpackage済み同一JARの2 process継続、5種のIdentity mutation、Session DELETE権限障害、
logout safe failure / recoveryおよびproxy下Cookie属性をHTTP / DB / process状態で確認した。
B3-5では公開`SessionCleanup` 3型、Spring標準cleanup、PostgreSQL advisory lock、non-web lifecycleを実装し、
期限切れだけの削除、winner / contender、OS kill後のlock解放とretryを確認した。
`001e619`のcleanな同一HEADでT0〜T6 aggregateを3回連続実行し、各回のT0〜T5 72 / 72、B3-4 / B3-5 T6、
inventory / sensitive-output scanおよびresource cleanupが成功した。root Reactor 14 / 14とNullAway正負 / restoreも成功し、
`../architecture/validation/phase2-p2-b3-b3-6-closeout.md`をP2-B3最終Architecture Owner review対象とする。
Milestone B integration jobの候補化条件は満たすが、workflow接続 / required化はP2-B4完了後のGate B reviewへ留保する。
同日、Architecture OwnerはB3-4 / B3-5の個別EvidenceとB3-6の5 review pointsを承認し、P2-B3を
`COMPLETE / ARCHITECTURE OWNER APPROVED`とした。Session Foundation以外の汎用non-web Worker / Batch基盤は
本承認に含めず、次のCPをP2-B4 Reference `identity`とする。判断と実装EvidenceはADR-048へ接続する。
2026年9月9日、B4-C1〜C8、B4-2、B4-3およびB4-4を順次承認し、`1ec1fae`のcleanな同一HEADで
B1 Audit、B2 Identity、B3 core / two-process / cleanupおよびB4 package済みReference journeyの6工程を
3回連続実行した。Root Reactor 15 / 15、104 tests、NullAway正負 / restore、inventory、sensitive-output、
所有resource cleanupも成功し、`../architecture/validation/phase2-p2-b4-b4-5-closeout.md`の5 review pointsを
Architecture Ownerが承認した。P2-B4を`COMPLETE / ARCHITECTURE OWNER APPROVED`とし、次をGate B reviewとする。
workflow追加、remote実行およびrequired check変更はGate Bの個別Owner承認前に行わない。
2026年9月10日、Architecture OwnerはGate B契約GB-C1〜C7を承認した。B4-5 closeoutをlocal / CI aggregateの正本とし、
独立`Local Identity Session Audit Integration` job、同一final HEADのremote 3回連続成功、Phase 2 Public API inventoryの
baseline候補化、途中失敗時を含むcleanup検査およびrequired化の別Owner判断を固定した。GB-1を完了し、次をGB-2
CI候補実装とする。push / PR / remote実行およびruleset変更は引き続き個別Owner承認まで行わない。
同日、Architecture OwnerはGB-2 CI候補実装の5 review pointsを承認した。既存B4-5 closeoutの正本利用、独立CI job、
失敗後の最終inspection、一次失敗を保持するcleanup failure集約およびGB-2〜GB-4の検証境界を確定し、GB-2を
`COMPLETE / ARCHITECTURE OWNER APPROVED`とした。次はGB-3 local final verificationとし、remote push / PR、
remote CI実行およびrequired check変更は引き続き個別Owner承認まで行わない。
同日、CI候補を含む`e048529`のcleanな同一HEADでGB-3 local final verificationを実行した。B1〜B4の6工程を
3ラウンド連続成功し、Root Reactor 15 / 15、104 tests、Null Safety正負 / restoreおよび終了後の独立cleanup検査も
成功した。Architecture OwnerはGB-3の5 review pointsを承認し、GB-3を`COMPLETE / ARCHITECTURE OWNER APPROVED`
とした。次はGB-4とし、remote push / PR、remote CI実行およびrequired check変更は個別Owner承認まで行わない。
同日、個別承認に基づきDraft PR #29でGB-4 remote verificationを実行した。初回Linux runで判明したJAR entry順序依存と
Phase 1b CP10 SQL検査範囲を補正し、final HEAD `f30a340`でlocal B4-5 closeout、既存remote check回帰および
`Local Identity Session Audit Integration`の39分11秒、32分20秒、34分31秒の3回連続成功を確認した。
全3回でaggregateと最終cleanup inspectionが成功し、Architecture Ownerは5 review pointsを承認した。GB-4を
`COMPLETE / ARCHITECTURE OWNER APPROVED`とし、次を候補jobのrequired check化reviewとする。main ruleset変更、
PR mergeおよびmerge後main CIは本承認に含めず、引き続き個別判断とする。
同日、Architecture OwnerはGB-5の6 review pointsと選択肢Aを承認した。現行rulesetの事前再取得後、既存6 check、
strict policy、bypassなしおよび他のruleを保持し、`Local Identity Session Audit Integration` 1件だけを
GitHub Actions integration ID `15368`でmain rulesetへ追加した。更新後の独立再取得とPR #29で、required check 7件、
対象checkの`COMPLETED / SUCCESS`、Draft維持およびmerge state `CLEAN`を確認し、GB-5を
`COMPLETE / ARCHITECTURE OWNER APPROVED`とした。本required化はFramework Repository固有とし、Customer業務アプリの
CIは利用Starter、業務リスク、構成およびデプロイ形態に応じて別途軽量化・段階化する。Draft解除、PR mergeおよび
merge後main CIは引き続き個別Owner判断へ残す。
同日、PR #29のrequired checks全件成功後、source final HEAD `c053fd8`をmerge commit方式でmainへmergeした。
merge commit `97d8ff2`に対するpush eventのCI run `34466016255`とJava Runtime Compatibility run
`34466016290`は全8 jobが成功した。main rulesetもactive、strict、bypassなし、required check 7件を維持している。
`../architecture/validation/phase2-gate-b-closeout.md`へ最終Evidenceを集約し、Gate B final Architecture Owner reviewを
開始可能とする。P2-C1のFramework Migration全体集約、第三者table一覧、clean install / supported upgradeは先行しない。
同日、Architecture OwnerはGate B final closeoutの5 review pointsを承認した。「Migration全体集約」を
「Framework Flyway正本の全体集約」と正確化し、P2-B2 / P2-B3の個別Framework migration検証済みという事実と、
P2-C1へ残す統合検証境界を明確化した。Gate Bを`COMPLETE / ARCHITECTURE OWNER APPROVED`とし、次はP2-C1の
契約レビューとする。
同日、main merge commit `8873942`から`feature/phase2-p2-c1-postgresql-migration`を作成し、P2-C1のread-only
inventory、contract review案および翌日再開用handoffを準備した。Audit migration version、Starter依存順、
table ownership、clean install matrix、Phase 1b supported upgrade起点およびTooling境界はArchitecture Owner review前の
提案であり、production SQL、Java、POM、test、scriptまたはCIは変更していない。

2026年9月11日、Architecture OwnerはP2-C1契約C1-C1〜C7をすべて推奨案どおり承認した。C1-2でAudit Starterへ
`V2026090300__create_koiki_audit.sql`を追加し、package済みAudit / Identity / Session JDBC Starter JARを直接検査する
非配布static inventory Harnessを追加した。focused Audit reactor 3 modulesとSession依存closure 7 modulesはbuild成功し、
Framework migration 3件の一意性、`0300`→`0301`→`0701`の依存順、owner artifact配置、合計11 table、Audit Entity列契約、
fixture固有DDL / 未使用index非混入を確認した。実PostgreSQL clean install 4 profileとPhase 1b supported upgradeはC1-3へ進める。

同日、C1-3の非配布PostgreSQL Harnessを追加し、package済みStarter resourceを使うAudit only、Identity、Session JDBC、
package済みReferenceのclean install 4 profileをPostgreSQL 17上で実証した。Framework migration 3件 / 11 tableの
version、checksum、owner history、列 / constraint / index、再起動no-op、Reference migration 0件、Spring Session initializer
無効を確認した。承認済みPhase 1b baseline `40d16f9`からのsupported upgradeではCustomer history / checksum / table / seed rowを
保持して現行Framework migrationを追加し、再起動no-opを確認した。Framework / Customer migration失敗、Customer checksum不一致、
initializer上書き拒否の失敗系も成功し、既存P2-B1 Audit contractを回帰確認した。C1-3を
`COMPLETE / LOCAL VERIFIED`とし、次はC1-4 aggregate回帰へ進める。

同日、commit `f7a3d906`のcleanな同一HEADでC1-4 closeoutを実行した。C1 static inventory 3 migration / 11 table、
PostgreSQL 17.11上のclean install 4 profile、Phase 1b supported upgradeおよびfailure contractsを再確認した。
Gate Bの6工程は3ラウンド連続成功し、B1 31 / 31、B2 56 / 56、B3 core 72 / 72、two-process、non-web cleanup、
package済みReference journeyを各roundで完了した。Root Reactor 15 projects / 104 tests、Null Safety正負 / restore、
Public API inventory、sensitive-outputおよび終了後の独立cleanup inspectionも成功した。C1-4を
`COMPLETE / LOCAL VERIFIED`とした。Architecture OwnerはC1-4の5 review pointsをすべて承認し、P2-C1を
`COMPLETE / ARCHITECTURE OWNER APPROVED`とした。次はP2-C2 package / Consumer contract reviewとし、
契約承認前にproduction artifact、Public API、Consumer fixture、OpenRewrite recipeまたはCIを追加しない。

同日、P2-C2のread-only inventoryを実施し、package / Root Reactor外Consumer / Public API baseline / OpenRewrite試作の
contract review案と再開handoffを準備した。formal Framework release unit 14 projects、11 JAR、Reference分離、
Phase 1b Consumer、既存2 artifact published baseline、Phase 2 Public API 19型およびADR-029のPhase 5正式提供境界を確認した。
準備段階のproduction changeは0であり、次はC2-C1〜C7のArchitecture Owner reviewとする。

同日、Architecture OwnerはP2-C2契約C2-C1〜C7を推奨案どおり承認した。正式release unit 14 projects / 11 JAR、
Root外Security Consumer、初回Public API baselineの過大claim禁止、snapshot publishの個別承認、OpenRewrite非配布試作、
C1 / Gate B回帰とcleanupのcloseout条件を確定した。C2-C5のremote publishは本承認に含めず、次はC2-2
formal package manifest / isolated stagingとする。

同日、C2-2でformal release unit manifestと非配布static Harnessを追加した。Root Reactorのformal 13 modulesとReference、
BOMの11 JARおよびpackage種別をmanifestと照合し、Referenceを除く14 projectsを空の隔離Maven repositoryへstageした。
14 / 14 `BUILD SUCCESS`、staged coordinates完全一致、Reference / Tooling / Customer migration / source template非混入、
一時repository残留0を確認した。production artifact / Public API変更は0であり、C2-2を`COMPLETE / LOCAL VERIFIED`として
Architecture Owner reviewへ提示した。Architecture OwnerはC2-2の4 review pointsをすべて承認し、次をC2-3
Root Reactor外Consumerとした。

同日、既存Gate A Consumerを変更せず、その配下にC2-3専用の独立PostgreSQL Consumerを追加した。formal release unit
14 projectsを空の隔離Maven repositoryへstageし、Consumerを別invocationでbuild / test / packageした。Public APIだけで
Identity作成 / 照会、Business Audit、Session cleanupを利用し、Java 21 buildの同一JARをJava 21 / 25とPostgreSQL 17で実行した。
Framework history 3行 / 11 table、Customer history 2行（baseline 1＋V1 SQL 1）/ marker 1 table、2回目no-op、
同JARのWeb processによるpublic / authenticated / unmatched routeとSecurity Header、initializer強制有効化の起動失敗と
`SPRING_SESSION`非生成、sensitive-outputおよびcleanupを確認した。C2-3を
`COMPLETE / LOCAL VERIFIED`としてArchitecture Owner reviewへ提示し、次候補をC2-4 Public API inventory / compatibility fixtureとする。

同日、Architecture OwnerはC2-3の5 review pointsをすべて承認した。review指摘に基づき、default denyはConsumer matcher外の
`/framework-fallback-probe`へ修正してFramework fallback 401を再確認し、Consumer Web child processをcleanup対象へ明記した。
C2-3を`COMPLETE / ARCHITECTURE OWNER APPROVED`とし、次はC2-4 Public API inventory / compatibility fixtureとする。

同日、C2-2 manifestの正式11 JARを隔離repositoryへstageし、package済みartifactからPublic API baseline candidateを
生成した。全11 artifact sectionとPublic Java型24件（Architecture 4、ArchUnit rules 1、Audit 6、Identity 10、
Session 3、残る6 JARは0件）を既存の承認済みtype inventoryと照合し、型種別、修飾子、継承、constructor、field、method、
generic型、例外、enum値、annotation metadata / default値およびJSpecify nullnessを正規化した。internal package追加を
inventory対象外とし、nullness-only変更、public戻り値破壊および未承認public追加をnegative fixtureで検出した。
既存Phase 1a published baseline script / required jobは変更せず、Phase 2 artifactに過去公開baselineとの互換性を主張しない。
remote publish、hash確定およびCI変更はC2-5の個別Owner判断へ残し、C2-4を`COMPLETE / LOCAL VERIFIED`として
Architecture Owner reviewへ提示する。

同日、Architecture OwnerはC2-4の5 review pointsをすべて承認した。review指摘に基づき、formal release unit全体24型を
Phase 1a公開済み5型とPhase 2 candidate 19型に分け、Public型0件の6 JARを正式artifact名で明記した。既存inventoryとの
照合は過去versionとのsignature互換性証明ではなく、型集合の継続一致確認であることも明確化した。C2-4を
`COMPLETE / ARCHITECTURE OWNER APPROVED`とし、次はC2-5 baseline publish / remote operationの実施可否reviewとする。

同日、C2-5 publish reviewを準備した。Root aggregatorを除くPOM 2 / JAR 11の13 coordinatesを候補とし、GitHubへ接続しない
一時file repositoryへ単一Maven sessionでdeployした。13 / 13 projectsが成功し、全coordinateが同一timestampとなり、
13 POM / 11 JARのlocal SHA-256とC2-4 signature SHA-256を採取した。既存Phase 1b workflowを変更せず、Phase 2専用manual
workflow、protected environment、publish sessionとfresh remote取得のhash一致および同一source`japicmp`をreview案とした。
再開環境のGitHub CLI認証が失効しており、package既存状態、environmentおよびActions accessを確認できないため、remote publishは
`NOT APPROVED / NO-GO`のまま保持し、Architecture Ownerへ6点のreviewを提示する。

同日、Architecture OwnerがGitHub CLIを再認証した後、read-only remote readiness確認を完了した。Repositoryはpublic、既存9 Maven
packageは`0.1.0-SNAPSHOT`を保持し、Phase 2のAudit / Security / Identity / Session JDBC 4 packageは未作成だった。
Phase 1a / 1b environmentはいずれもrequired reviewer、main限定およびadmin bypass無効を維持し、既存Phase 1b workflowは
`origin/main`との差分0だった。main run `34475233082`のPublic API Compatibility成功によりPhase 1a固定baseline取得経路も確認した。
remote readinessを`VERIFIED`とするが、専用workflow / environment、PR / main、最終source commitおよびpublishは未承認のため、
remote publishは引き続き`NO-GO`とする。

同日、Architecture OwnerはC2-5 review 6点を修正条件付きで承認した。Rootを除く13座標、Phase 2専用workflow分離および
remote publish `NO-GO`を提案どおり承認した。24型は現行release unitのaggregate signatureとし、Phase 2初回published baselineを
Audit 6型、Identity 10型、Session 3型、Security 0型としてPhase 1a固定baselineから分離した。remoteでは共通build numberを
要求せず、同一workflow run / source commit、座標ごとのresolved snapshot value、24 payload SHA-256、signatureおよび同一source
`japicmp`でrelease unitを固定する。修正後の専用workflow実装とlocal reviewを開始し、remote publishは未承認のまま保持する。

同日、Phase 2専用manual workflow、13座標manifest、publish-state Capture / Verify toolおよびlocal file repository dry-run toolを
実装した。local reviewでMaven metadataのclassifier欠落処理と検証用POMに対する`japicmp` skipを検出・修正した後、
13座標、24 payload SHA-256、24型aggregate signatureおよび11 JARのsame-source `japicmp`実比較がすべて成功した。
既存Phase 1b workflowは変更していない。protected environment、push、PR、main CI、manual dispatchおよびremote publishは
未実施とし、C2-5 workflow実装をArchitecture Owner reviewへ提示する。

同日、Architecture Ownerは承認条件の反映を確認し、13座標のpublish指定と検証manifestの乖離防止をworkflow実装承認の
修正条件とした。workflow / dry runのMaven project listを13座標manifestから生成する共通publish helperへ統一し、共有loaderで
formal 14-project release unitからRoot aggregatorだけを除いた集合との完全一致をpublish / Capture / Verify前に機械検査するよう
修正した。修正後のlocal dry runで13座標、24 payload SHA-256、aggregate signatureおよび11 JARのsame-source `japicmp`が
再度成功したため、C2-5 workflow実装を`APPROVED / LOCAL VERIFIED`とする。protected environment、push、PR、main反映、
workflow dispatchおよびremote publishは承認範囲外であり、引き続き`NO-GO`とする。

2026年9月12日、C2-6 OpenRewrite feasibilityを`build-support`配下のRoot Reactor外・非配布Toolingとして実装した。
syntheticなKOIKI所有型変更1件に限定し、recipe正例 / 非変更例、固定before / after、旧API定義非変更、2回目適用の冪等性、
変換前後のConsumer compile / test、手動残件report、formal release unit非混入および一時成果物cleanupを隔離Maven repositoryで
検証した。正式recipe artifact、実Customer / 過去KOIKI version、Spring Boot recipe再実装、required CIおよびremote操作は含めない。
C2-6を`COMPLETE / LOCAL VERIFIED`としてArchitecture Owner reviewへ提示し、承認後の次候補をC2-7 closeoutとする。
C2-5 remote snapshot publishは引き続き`NOT APPROVED / NO-GO`である。

同日、Architecture OwnerはC2-6の6判断を提案どおり承認した。OpenRewrite prototypeは初期フレームワーク運用、正式release unit、
Starter / BOM、Customer配布、runtime、required CIおよびsnapshot publishへ含めず、`build-support`配下の非配布Tooling Evidenceとして
のみ保持する。syntheticな型変更の成立を実Customer完全移行、意味的同値性、過去version互換性または正式recipe提供とは扱わない。
正式artifactとrelease CIへの昇格はPhase 5の個別Owner reviewへ残す。C2-6を`COMPLETE / ARCHITECTURE OWNER APPROVED`とし、
成果物のcommit後、clean HEADでC2-7 local closeoutへ進む。C2-5 remote publishとその他のremote変更は引き続き`NO-GO`である。

同日、commit `5704cdd5dbe407e33c8eba6b8367fc6bf98f544e`のcleanな同一HEADでC2-7 local closeoutを実行した。
C2-2 package、C2-3 Consumer、C2-4 Public API、C2-5 local publish dry run、C2-6 OpenRewriteのfocused 5工程、
C1 static / PostgreSQL回帰、Gate B 6工程の3ラウンド、Root 15 projects / 104 testsおよびNull Safety正負 / restoreが成功した。
formal 14 projects / 11 JAR、publish候補13座標、Public API 24型、Reference分離、Tooling非配布、sensitive-outputおよび
process / container / temporary repository / fixture target cleanupを最終確認した。C2-7を`COMPLETE / LOCAL VERIFIED`として
Architecture Owner reviewへ提示し、承認後の次候補をP2-C3 Developer Journey / DoD closeoutとする。
C2-5 remote snapshot publishとその他のremote変更は引き続き`NOT APPROVED / NO-GO`である。

同日、Architecture OwnerはC2-7の6判断を提案どおり承認した。C2-2〜C2-6のfocused検証、P2-C1回帰、Gate B 6工程の
3ラウンド、Root Reactor、Null Safety、最終inventory、sensitive-output検査およびcleanupをP2-C2 closeout Evidenceとして
受け入れ、P2-C2を`COMPLETE / ARCHITECTURE OWNER APPROVED`としてcloseした。次はP2-C3 Developer Journey / DoD closeoutの
contract / inventory reviewへ進む。C2-5 remote snapshot publish、protected environment作成、push、PR、main反映、
workflow dispatch、required check変更およびその他のremote操作は引き続き`NOT APPROVED / NO-GO`である。

同日、P2-C3 C3-1のread-only inventoryを実施し、DoD 2-1〜2-10、Developer Journey、Architecture / verification index、
ADR / Skill、release / Public API / migration、remote Evidenceおよびdeferred scopeのcloseout契約案を
`../architecture/validation/phase2-p2-c3-contract-review.md`へ記録した。P2-C3は新機能を追加せず、既存Consumer、Referenceおよび
verification scriptの合成で成立させる案とする。Architecture Ownerの指摘を受け、機構の成立と同格で、エンジニアによる
入口の発見、dependency / profile / Ownershipの選択、公開契約からの実装、secure default、失敗時診断、正負検証、cleanupおよび
留保範囲の理解をEngineer-facing acceptanceへ追加した。次はC3-1の10判断に対するArchitecture Owner reviewであり、承認前に
production code、Public API、migration、workflowまたはremote stateを変更しない。

同日、Architecture OwnerはC3-1契約案の§4.1 `Engineer-facing acceptance`を内容承認した。機構の成立と同格で、
entry / discoverability、dependency / profile selection、Ownership、公開契約、secure defaults、diagnostics、verification、
runtime / packagingおよびlimits / next actionを評価する。§4.2 / §4.3を含む10判断全体とC3-2開始は未承認のため、
C3-1を`CONTRACT REVIEW PENDING`のまま維持する。

同日、Architecture Ownerは§4.1の部分承認を前提としてC3-1の10判断を提案どおり承認した。P2-C3を新機能追加なしの
Developer Journey / DoD / governance closeoutとし、Engineer-facing acceptance、human-operable journey、friction triage、
DoD 2-1〜2-10 trace、文書対象およびclean-HEAD local aggregateを承認した。C3-1を
`COMPLETE / ARCHITECTURE OWNER APPROVED`とし、次はC3-2のEngineer-facing文書・Developer Journey接続へ進む。
production artifact、Public API、migration、workflowおよびremote操作は本承認に含めず、C2-5 remote publishも
`NOT APPROVED / NO-GO`を維持する。

同日、C3-2としてroot / Architecture / Development / Starter / Consumer / verification / ReferenceのREADMEを
`phase2-developer-journey.md`へ接続した。Parent / BOM、Starter、local Session / OIDC / Bearer profile、Customer Ownership、
public seam、secure defaults、diagnostics、Application verification、Framework aggregateおよびdeferred scopeを、
エンジニアが順に辿れる形へ整理した。Referenceを正規利用例、ConsumerをTooling EvidenceとしてProject Templateから分離し、
Customerの日常CIへRepository固有の全aggregateを要求しない。production artifact、Public API、migration、workflowの変更は0である。
`../architecture/validation/phase2-p2-c3-c3-2-developer-journey.md`をC3-2 Owner reviewへ提示し、承認・commit後のclean HEADで
C3-3 local aggregate / human-operable journey verificationへ進む。remote操作は引き続き`NOT APPROVED / NO-GO`である。

同日、Architecture OwnerはC3-2の6判断を提案どおり承認した。Repositoryからの入口、依存・profile・Ownership選択、
Framework Public contractの利用境界、secure defaults、diagnostics、Application verificationとFramework aggregateの分離、
Reference / Consumerの非Template境界を受け入れ、C3-2を`COMPLETE / ARCHITECTURE OWNER APPROVED`とした。次は本差分を
commitし、そのclean HEADでC3-3 local aggregate / human-operable journey verificationへ進む。production artifact、Public API、
migration、workflowおよびremote操作は承認に含めず、`NOT APPROVED / NO-GO`を維持する。

同日、C3-2承認差分をcommitしたclean HEAD `28b0220bc6bfbee1b7b9c01721717e9d5017ec7e`でC3-3 local aggregateを
実行した。Gate A、P2-C2 package / Consumer / Public API / local publish dry run / OpenRewrite、P2-C1 static / PostgreSQL、
Gate B 6工程3ラウンド、Root Reactor、Null Safety、sensitive-output、inventoryおよびcleanupはすべて成功した。
formal 14 / Root 15 / publish 13 / JAR 11 / Public API 24 / migration 3 / table 11の固定inventoryを確認し、
`../architecture/validation/phase2-p2-c3-c3-3-closeout.md`へ記録した。C3-3を`COMPLETE / LOCAL VERIFIED`として
Architecture Owner reviewへ提示する。承認候補はP2-C3 `COMPLETE / LOCAL VERIFIED / GATE C READY`であるが、Gate C remote
Evidence前にPhase 2を完了扱いしない。protected environment、push、PR、main、ruleset、dispatchおよびsnapshot publishは
引き続き`NOT APPROVED / NO-GO`である。

同日、Architecture OwnerはC3-3のreview points 1〜7を提案どおり承認し、C3-3を
`COMPLETE / LOCAL VERIFIED / ARCHITECTURE OWNER APPROVED`、P2-C3を
`COMPLETE / LOCAL VERIFIED / GATE C READY`としてlocal closeoutした。snapshot publishはfinal main CI成功後に一度実施し、
remote取得、24 payload SHA-256、aggregate signatureおよび11 same-source `japicmp`を確認するPhase 2最終受入れ条件とする。
次はGate C-1 remote plan reviewであり、protected environment、push、PR、main、ruleset、dispatchおよびsnapshot publishは
実行前の個別Owner承認まで`NOT APPROVED / NO-GO`を維持する。

同日、Gate C-1のread-only remote inventoryを実施した。`origin/main`はC3-3 Owner承認commitのancestor、対象branchの既存PRは0、
main rulesetはactive / strict / bypass 0かつrequired checks 7件、Phase 2 workflow / environmentは未反映、Phase 2新規4 packagesは
未公開であることを確認した。PR、main CI、protected environment、一回限りのsnapshot publish / verifyおよび失敗時境界を
`../architecture/validation/phase2-gate-c-c1-remote-plan-review.md`へ記録し、Architecture Owner reviewへ提示する。
pushその他のremote mutationはまだ実施せず、`NOT APPROVED / NO-GO`を維持する。

同日、Architecture OwnerはGate C-1の10 review pointsを提案どおり承認した。本承認記録のcommit後、承認済み順序に従う
branch push、final PR、required checks 7件、merge commit、merge後main CI、Phase 2 protected environment、final main SHAの
一回限りpublish / remote verifyへ進めてよい。ruleset緩和、bypass、追加secret、無断retry、package削除または別SHA publishは
承認せず、順序またはidentity変更時は再reviewする。Gate C-3最終Owner review前はPhase 2を完了扱いしない。

同日、Gate C-1承認commit `712595e`をpushし、final PR #31のrequired checks 7件成功後、merge commit `31724b6`としてmainへ
反映した。merge後CI / runtime全8 jobは成功し、`phase2-internal-snapshot` environmentをmain限定・required reviewer・
admin bypass禁止・secret / variable 0で作成した。final main SHAを指定したsnapshot run `34686248900`はauthorize、formal package、
aggregate Public APIまで成功したが、Ubuntuで一時pathの`AbsoluteUri`が空になるTooling defectによりlocal publish dry runで停止した。
publish / verify jobはskipされ、remote Maven packageは既存9件のままでmutation 0である。

`UriBuilder`でfile schemeを明示しfail-fastする最小修正を行い、localで13座標 / 24 payload / aggregate signature /
11 same-source `japicmp`のdry runを再確認した。修正、失敗Evidenceおよび新しいPR / main / workflow run順序を
`../architecture/validation/phase2-gate-c-c2-publish-preflight-correction.md`へ記録し、identity変更のOwner再reviewへ提示する。

同日、Architecture OwnerはC-2 corrective sliceの7 review pointsを提案どおり承認した。初回runのremote mutation 0、
cross-platform file URI Tooling defectへの最小修正、local再検証結果および新しいPR / main CI / new main SHA / new workflow runの
順序を受け入れた。本修正と承認記録のcommit後、corrective branch pushとfollow-up PR作成へ進める。failed runのrerun、
旧SHA dispatch、無断retry、package削除またはenvironment / ruleset / required checks / publish unitの変更は行わない。

同日、PR #32をmerge commit `e10f88c`としてmainへ反映し、merge後CI / runtime全8 jobと設定read-back成功後、同SHAで
snapshot run `34693368000`を新規dispatchした。authorize、preflightおよびprotected environment承認後の13座標publishは
成功したが、CaptureはGitHub Maven metadataに過去snapshotVersionが累積することを考慮せず、POMを1件と仮定したため失敗した。
manifest uploadとfresh remote Verifyはskipされ、本runをaccepted baselineとしない。公開物は削除せず、rerunもしない。

Architecture Ownerは同一workflow run / source commit内でpublish、Capture、fresh Verifyを完結する要件を厳格維持し、修正後に
改めてpublishする方針を選択した。今回だけのtimestamp固定ではなく、publish前後の座標 / extension別metadata差分を一意に選び、
pre-publish inventoryを先に保存する恒久修正を実装した。履歴累積の正負fixtureとfull local dry runで13座標、24 hash、signature、
11 same-source `japicmp`が成功した。`../architecture/validation/phase2-gate-c-c2-publish-capture-correction.md`を実装Owner reviewへ提示する。

同日、Architecture Ownerはpublish capture correctionの9 review pointsを提案どおり承認した。run `34693368000`を
historical unaccepted publishとして保持し、同一run要件を厳格維持した新しいpublishを正式な回復方法とする。
publish前後metadata差分、retention 7日のpre-publish inventory、POM / JAR一致、run / attempt固定およびfail-closedを
恒久契約として受け入れた。本修正と承認記録のcommit後、新しいPR / main CI / main SHA / workflow run順序へ進める。
failed runのrerun、package削除、自動retryまたは承認外の追加publishは行わない。

2026年9月13日、capture correction commit `4f58cfb`をPR #33のrequired checks成功後、merge commit `af7b4f7`として
mainへ反映した。同commitのCI / runtime全8 jobとenvironment / ruleset read-back成功後、40文字main SHA
`af7b4f71d885fe4991e5fcf85fcf8888aec6a539`を指定してsnapshot run `34701933485`を一度dispatchした。
protected environment承認後、同run / attempt 1内でpre-publish inventory保存、13座標publish、exact Capture、
hash-only manifest uploadおよびfresh remote Verifyがすべて成功した。

remote Verifyは13座標、24 payload SHA-256、aggregate signatureおよび11 same-source `japicmp`をSUCCESSとした。
成功runのmanifest identityと全payload hashは
`../architecture/validation/phase2-gate-c-c2-remote-evidence.md`へ永続記録した。Gate C-2 remote Evidenceを
`COMPLETE`とし、次はGate C-3 Architecture Owner最終reviewである。Owner承認前はGate CおよびPhase 2全体を
`COMPLETE / ACCEPTED`へ変更しない。

同日、Architecture OwnerはGate C-3の9 review pointsについて、source identityを40文字の完全SHAで記録し、
Phase 2最終状態を既存Phaseと同じ`COMPLETE / ACCEPTED`表記へ統一することを条件に、全体を承認した。
`../architecture/validation/phase2-gate-c-c2-remote-evidence.md`へ両条件と承認記録を反映し、run `34701933485`の
manifestをPhase 2初回published baseline identityとして確定した。Gate CおよびPhase 2 Security Foundationを
`COMPLETE / ACCEPTED`として最終closeoutする。

正式release、Customer配布、追加snapshot publish、OpenRewrite prototypeの正式運用・配布、Oracle対応および
その他のdeferred scopeは本承認に含めず、各後続Phase / Gateの個別Owner reviewへ残す。

### Milestone C — PostgreSQL Migration / packaging / closeout

| CP | Scope | Exit criteria |
|---:|---|---|
| P2-C1 | PostgreSQL Migration、第三者table一覧、upgrade / clean fixture | Framework Flyway正本、所有権完全一致、Spring initializer無効、clean install / supported upgradeを実証 |
| P2-C2 | package / Consumer / OpenRewrite試作 | 配布単位、Root Reactor外Consumer、Public API互換性、Migration Support境界を実証 |
| P2-C3 | Developer Journey / DoD closeout | 全DoD trace、Skill / ADR、release unit、remote evidence、deferred一覧 |
| Gate C | final PR / main / snapshot | required checks、PostgreSQL integration evidence、Owner approval、merge後main CI、snapshot remote Evidence |

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
- Milestone Aのintegration jobは、final HEADでremote 1回成功し、cleanupと実行時間を確認した後にrequired化をreviewする。
- Milestone BのPostgreSQL integration jobは、3回連続成功とcontainer / process cleanup確認後にrequired化をreviewする。
- Oracle jobは現行Phaseのworkflowへ追加しない。
- CI permissionsは原則`contents: read`。package / environment権限を通常CIへ追加しない。
- private key、client secret、password、token、PIIをsource、log、artifact、Problem Detailsへ残さない。

2026年9月2日、Architecture Ownerは、外部IdP、DB、containerまたは常駐processを使用しないMilestone Aについて、
local aggregate 3回連続成功済みであることを踏まえ、同一commitのremote rerunをrequired化条件としない判断を承認した。
Testcontainers、実DBおよび複数processの安定性を扱うMilestone Bの3回連続成功条件は維持する。

## 9. ADR / Skill / documentation targets

| Target | Timing |
|---|---|
| KOIKI-PYFW fitting / token phase decision | P2-F2〜F4。既存Grand Designの明確化で足りなければADR |
| SPA Session / BFF / direct Token profile | P2-F2。Grand Design §13.5 / §14.2、ADR-006〜008の進展をEvidenceへ接続 |
| Security profile / artifact ADR | P2-A1 |
| Identity / Audit / transaction ADR | P2-B1 |
| ADR-048 Session JDBC / cleanup / single execution境界 | P2-B3（2026年9月8日 ACCEPTED） |
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
