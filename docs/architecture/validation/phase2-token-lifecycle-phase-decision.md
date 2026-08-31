# Phase 2 token lifecycle phase decision

## 1. Status and scope

- **Decision date:** 2026年8月31日
- **Work package:** `P2-F4`
- **Status:** `P2-F4 COMPLETE — OWNER APPROVED`
- **Owner:** Architecture Owner
- **Code / dependency / migration / workflow change:** 0

本記録は、KOIKI-JavaWebがOAuth 2.0 Resource Serverとして外部Access Tokenを検証する責務と、
Authorization ServerとしてAccess / Refresh Tokenを発行・更新・失効する責務を分離し、後者の実装phaseを決める。
KOIKI-PYFWの利用者向け能力は継承候補とするが、Python endpoint、自前JWT、JWT Cookieまたはtableを互換移植しない。

入力Evidenceは次とする。

- `phase2-koiki-pyfw-security-fitting.md`のcapability / gap inventory
- `phase2-security-semantics-fitting.md`のSession / Bearer / logout境界
- `phase2-spa-sso-security-fitting.md`のSession / BFF / direct Token profile
- `phase2-security-test-design.md`のGate F承認済みtest / threat境界
- Grand Design §13.5、§14.2、§14.6、§27.6〜§27.9

## 2. Executive recommendation

1. **Phase 2はtoken consumerに限定する。** P2-A3でOAuth 2.0 Resource Serverとして外部発行Access Tokenを検証し、
   Authorization Server、token endpoint、refresh、rotation、reuse detection、revocationをproduction実装しない。
2. **外部Authorization Serverを第一標準とする。** Amazon Cognito User Pool等の標準OIDC / OAuth Providerがissuer、
   client、grant、key、token lifecycleを所有し、KOIKI-JavaWebはissuer / audience / time / scopeを検証する。
3. **KOIKI-hosted Authorization ServerはPhase 4の独立optional work package候補とする。** 外部Providerを利用できない、
   KOIKI local identityから標準OAuth tokenを発行する等の明示use caseがある場合だけ、Grand Design / ADR / DoD / 見積を
   追加承認して開始する。現行Phase 4の必須DoDへ暗黙に追加しない。
4. 採用時はSpring Security Authorization Serverを第一候補とするが、`koiki-starter-security`へ自動同梱せず、
   Resource Server / business applicationから論理的・security責任上分離する。別artifact / process / deployable等の
   物理topologyはP4-AS0で決定する。
5. KOIKI-hostedでrefresh tokenを発行する場合は、Spring Securityが提供するrotation、token invalidation、revocation、
   JDBC persistenceを基準とする。KOIKI-PYFW固有のtoken-family全失効、独自hash-at-rest、device token listを必須化しない。

この判断により、Phase 2 DoD 2-4を拡張せずにCognito / external IdP / BFF / direct Tokenを受けられる一方、
KOIKI-PYFWが持つ発行管理能力を消去せず、明示された後続work packageへtraceableに保持できる。

Grand Design §13.5 / §14.6の「Token方式利用時」のrotation / reuse検知は、選択したAuthorization Serverとclientを含む
deployment acceptanceと解釈する。KOIKI Resource Serverがrefresh tokenを処理する意味ではない。P2-F4承認後にGrand Designへ
この責務解釈、Spring標準優先および`P4-AS`のoptional boundaryを補足し、external / hostedの両経路へ割り当てる。

## 3. Responsibility separation

| Concern | OAuth Client / BFF | Authorization Server | KOIKI Resource Server | KOIKI Session / Identity |
|---|---|---|---|---|
| user authentication request | Authorization Code + PKCEを開始 | user / clientを認証しcodeを発行 | 責務なし | application-direct login時はOIDC Clientまたはlocal Form Login |
| Access Token issuance | token endpointから受領 | issuer、audience、scope、expiry、signatureを決定して発行 | **発行しない** | local Session loginからJWTを発行しない |
| Access Token validation | tokenをopaque secretとして扱う | metadata / JWKSを公開 | signature、`iss`、`aud`、`exp`、`nbf`、scopeをrequestごとに検証 | Framework user / Permissionへlink |
| Refresh Token | BFF等のconfidential clientがserver側で保持。public clientはrisk decision | 発行可否、client binding、rotation、reuse、expiryを所有 | **受け取らない** | HTTP Sessionとは別物 |
| revocation / grant | revoke endpointを呼び得る | 標準endpointとprovider policyに従いtoken / authorizationを失効 | JWT local validation時の反映方式だけを契約 | KOIKI Session失効をissuer token失効と同一視しない |
| key lifecycle | private keyを持たない | generation、KMS / HSM、rotation、overlap、retirement | public JWKSを取得・cacheし、unknown `kid`をdeny | 責務なし |
| client registration / secret | Customer clientを所有 | registration、redirect URI、grant、secret lifecycleを所有 | client secretを受け取らない | Customer / runtime environment責務 |

OAuth2 Clientがproviderからtokenを取得・refreshすることはAuthorization Server実装ではない。ただし、そのtokenをbrowserへ
露出するか、server-sideに保持するかはProfile S / B / Tの承認済み境界に従う。

## 4. Phase allocation

| Phase / work package | Scope | Required result | Explicit non-scope |
|---|---|---|---|
| Phase 2 P2-A3 | external JWT Resource Server、OIDC Client共存 | issuer / audience / time / scope、ID Token / Cookie fallback拒否、Cognito-compatible local Evidence | token発行、refresh、revocation endpoint、Authorization Server table |
| Phase 2 P2-B2 / B3 | local credential、HTTP Session、logout / disable / reset | KOIKI Session失効、identity / audit semantics | issuer-owned tokenの即時失効を偽装しない |
| Phase 3 | Reference REST APIがPhase 2 Resource Server contractを利用 | API / Permission / error contractの利用実証 | Authorization Server追加なし |
| Phase 4 current baseline | SPA最小参照、MVC / SPA併用、Enterprise Integration | 必須DoDはProfile SのCookie Session / CSRF実証。B / Tは承認済み境界を維持 | KOIKI-hosted issuer、BFF / direct Token production実装を必須DoDにしない |
| **Phase 4 optional `P4-AS`候補** | KOIKI-hosted Authorization Serverとtoken lifecycle | 下記P4-AS0〜AS4を別Gateで完了 | 現行Phase 4へ無承認で混入しない |
| Phase 5 | 採用済み構成のSecurity Review、SBOM、upgrade / operations | external / hostedいずれの構成もproduction review | Phase 5で初めてissuerを設計しない |

`P4-AS`を採用する場合の最小順序は次とする。

| CP | Work | Exit criteria |
|---:|---|---|
| P4-AS0 | use case / build-vs-buy / threat / protocol ADR | external Providerでは不足する理由、grant、client種別、issuer topology、運用Ownerを承認 |
| P4-AS1 | Spring feasibility / persistence gap | Boot-managed tree、標準endpoint、JDBC / secret-at-rest / 採用persistence基盤のgap、Public API 0候補を実証 |
| P4-AS2 | issuer / key / client / grant baseline | asymmetric key、JWKS rotation、exact redirect、PKCE、client authentication、scope / audience最小化 |
| P4-AS3 | refresh lifecycle | Spring標準rotation / invalidation / revocation / persistenceの実挙動、public client発行可否、cleanup / audit |
| P4-AS4 | BFF / SPA / service integration | confidential BFF、承認済みpublic client、Resource Server、logout / disable / incident journey |

P4-AS0を通過しない場合、P4-AS1〜AS4は作らずexternal Authorization Server標準を維持する。

## 5. External provider and KOIKI-hosted issuer selection

| Criterion | External Provider — default | KOIKI-hosted — optional |
|---|---|---|
| primary use | Cognito / enterprise IdP / cloud security boundaryが利用可能 | external Providerを使えない、local identityをOAuth issuer化する明示要件 |
| security operations | providerがpatch、key、availability、abuse protectionを所有 | KOIKI運用者が24/7 issuer、key、client、incident、backupを所有 |
| KOIKI implementation | OAuth2 Client / Resource Server fitting | Authorization Server production capabilityを追加 |
| customization | provider claim / grant制約内 | 高いが、標準適合・攻撃面・運用負担も増える |
| default decision | **採用** | **自動採用しない** |

顧客ごとに外部Providerが異なっても、Frameworkはstandard discovery、issuer、audience、scope、subject linkを契約とする。
Cognito専用token APIまたはprovider固有claimをKOIKI Public APIへ固定しない。

Profile Tのpublic clientへrefresh tokenを発行する場合、Customer acceptanceはRFC 9700に従うsender constraintまたはrotation、
reuse detectionおよびlifetimeをprovider契約 / 実証で確認する。満たせない場合はrefresh tokenを発行せず、Authorization Code +
PKCEによる再認証へ戻す。BFFのconfidential clientでもclient binding、server-side保管、scope / audience最小化を必須とする。

## 6. Future hosted Spring-standard baseline

| Area | Minimum accepted behavior | Spring candidate / fitting gap |
|---|---|---|
| grant | Authorization Code + PKCEをbrowser系の基本とし、Implicit / Resource Owner Password Credentialsを禁止 | Spring標準grant。client credentials等はuse case単位でopt-in |
| Access Token | asymmetric署名、明示`iss` / `aud` / scope、短いTTL、`kid`、key overlap | JWT generator / JWK support。RFC 9068適合範囲をfixtureで確認 |
| Refresh Token issue | client / grant / scope / resourceにboundし、発行自体をrisk decisionとする | Refresh Token grant。public clientへ無条件発行しない |
| rotation | client policyに応じてnew refresh tokenを発行し、旧tokenを再利用しない | `reuseRefreshTokens(false)`と標準authorization persistenceの実挙動を実DB / concurrencyで確認。独自family graphを作らない |
| reuse / replay | Spring / provider標準のinvalidated token処理と監査可能範囲を記録 | public clientでRFC 9700のreplay detectionを満たせない場合はrefresh tokenを発行せず再認証へ戻す |
| lifetime | Spring `TokenSettings`のAccess / Refresh Token TTLを明示し、実効残存windowを運用文書へ記録 | KOIKI-PYFW固有のabsolute / inactivity family lifetimeを必須化しない |
| storage | Spring標準JDBC persistenceを利用し、DB encryption at rest、TLS、最小権限、backup / log非露出で保護 | application-level hash保存は必須化しない。顧客policyで必要ならexternal Providerまたは別拡張を選ぶ |
| revocation | RFC 7009 compatibleなSpring標準revocation endpointを利用し、invalid tokenから情報を漏らさない | 独自token list、device-all、subject-all、family cascadeを標準機能として追加しない |
| immediate Access Token invalidation | JWTはexpiryまで有効になり得ることを明示 | short TTLを基本とし、即時性が必要ならopaque introspection、denylist、authorization version等を別途選択 |
| logout | browser Session、BFF Session、authorization / token revoke、IdP global logoutを別操作として表現 | OIDC logout / revocation endpoint。全logoutの同一化は禁止 |
| audit / metrics | issue、refresh、reuse、revoke、key / client変更を記録し、token / PIIは非露出 | Spring event + KOIKI Security / Business audit。low-cardinality metric |
| cleanup | expired authorizationをnon-web single executionで削除 | Spring標準persistenceに合わせPhase 1b single executionを再利用 |

KOIKI-PYFWのtoken-family全失効、hash-at-rest、device token管理はsource capabilityとして保持するが、Java版のminimum
acceptanceへは昇格させない。P4-AS1 / AS3はSpring標準が実際に提供するrotation / invalidation / revocation / persistenceを
negative / concurrency testで確認し、その保証範囲と残存riskを文書化する。標準範囲を超える独自実装は別のOwner判断とする。

## 7. Revocation truth table

| Event | Phase 2 KOIKI Session | External issuer token | Future KOIKI-hosted token |
|---|---|---|---|
| local logout | current local Sessionを失効 | 変化なし | client / grant policyにより別途revoke。暗黙連動なし |
| OIDC logout | local Sessionを必ず失効。provider logoutはopt-in | provider policy | issuer session / grantを別々に定義 |
| BFF logout | KOIKI Sessionの存在を仮定しない | BFFがserver-side token storeを削除し、必要ならrevoke | 同左 |
| direct SPA logout | browser内Access Tokenを破棄 | expiryまたはprovider revoke policy | Spring標準revokeを利用。既発行JWTは選択した即時失効方式に従う |
| password change / reset | 対象userの全KOIKI Sessionを失効 | issuer-owned tokenは変化なし | Spring標準で可能なauthorization / token失効範囲をP4-AS3で確定 |
| account disable | 対象userの全KOIKI Sessionを失効 | external Provider / Resource Server連携policy次第 | new issue / refreshをdenyし、標準失効を実行。既発行JWTの残存windowを明示 |
| Permission reduction | 対象userの全KOIKI Sessionを失効 | claim入りJWTはexpiryまで古い可能性 | short TTLまたは即時失効方式。過大な鮮度保証をしない |
| refresh token reuse | 対象外 | provider責務 | Spring標準の拒否 / invalidation結果に従う。RFC 9700を満たせないpublic clientにはrefresh tokenを発行しない |

Resource ServerがJWTをlocal validationする構成で、refresh tokenだけをrevokeしても既発行Access Tokenが即時無効になるとは限らない。
API / 運用文書はこの残存windowと標準revocationの対象を明示し、Access Token即時失効や全device失効を過大に保証しない。

## 8. Public API, artifact and data boundary

- Phase 2では`/login` API、`/token`、`/refresh`、`/revoke`、token listをKOIKI endpointとして追加しない。
- OAuth標準endpointは将来Authorization Server deployableが所有し、Customer business `/api/v1`と混在させない。
- Springの`OAuth2Authorization`、`RegisteredClient`、token generator / contextをKOIKI Customer Public APIへ露出しない。
- `koiki-starter-security`はClient / Resource Serverの安全な既定を担い、Authorization Serverをclasspath条件だけで起動しない。
- P2-A3のephemeral issuer、key、token encoder、clientはTooling test sourceだけに置き、production capabilityへ昇格させない。
- 将来のauthorization / token tableはFrameworkまたはdedicated issuer所有とし、Customer / Reference tableにしない。
- client登録、scope / consent、key管理、device / grant一覧UIはuse caseとprivacy reviewなしにPublic API化しない。

## 9. Dependency and operational consequence

Phase 2のinclude / exclusionは変更しない。`spring-boot-starter-security-oauth2-authorization-server`および同test Starterは
Spring Boot 4.1.1 BOM管理下の将来候補だが、P2-F4承認だけではPOMへ追加しない。

P4-AS採用時は少なくとも次を追加見積・承認する。

- deployable / module ownership、issuer URL、HA、backup / restore、DR
- key generation、KMS / HSM、rotation ceremony、overlap、emergency revocation
- client onboarding / secret rotation、redirect URI、consent、scope governance
- Spring標準authorization / token persistence、DB encryption / access control、採用DBでの適合、retention、cleanup
- abuse / brute force / replay、refresh concurrency、incident response、security audit
- standards conformance、BFF / public client / service client、package済みmulti-process CI
- SBOM、vulnerability response、upgrade compatibility、operations guide

これらは現行Phase 4の見積とDoDに含まれていないため、P4-AS0承認時にGrand Designと実行計画を再見積する。

## 10. Acceptance and stop conditions

P2-F4のphase decisionは次で完了とする。

1. Phase 2がResource Server / OAuth2 Clientまでであることを承認する。
2. external Authorization Serverを第一標準とすることを承認する。
3. KOIKI-hosted issuerをPhase 4 optional `P4-AS`へ割り当て、無承認では開始しないことを承認する。
4. §6〜§8を将来hosted implementationのminimum acceptanceとして保持する。
5. plan / Grand Design上のtraceabilityと、Gate P2-2がAuthorization Serverなしで通過可能なことを確認する。

次の場合は作業を停止してP4-AS0 / Architecture Ownerへ戻す。

- local login APIの応答として独自JWTを発行しようとする。
- Resource Serverへrefresh token、client secret、private signing keyまたはrevoke責務を持たせる。
- Spring標準のrotation / revocationが提供する以上のfamily / device全失効を、Evidenceなしに保証する。
- 標準token persistenceを無保護なDB、backup、query logへ置く、またはprivate key / secret / tokenをlogやartifactへ出す。
- JWTの即時revokeを、introspection / denylist / version / short TTL等の方式決定なしに保証する。
- P2-A3 test issuerをproduction Authorization Serverへ昇格する。
- P4-ASを現行Phase 4 DoD / 見積へ無承認で追加する。

## 11. Owner decisions requested

| ID | Recommended decision | Alternative / impact | Status |
|---|---|---|---|
| T-1 | Phase 2 production scopeはOAuth2 Client + JWT Resource Serverまで | Authorization Server前倒しはDoD、threat、運用、工数を拡大 | **APPROVED — OWNER 2026-08-31** |
| T-2 | external Authorization Serverを第一標準とする | KOIKI-hostedをdefaultにすると全Customerへissuer運用を課す | **APPROVED — OWNER 2026-08-31** |
| T-3 | KOIKI-hosted issuerはPhase 4 optional `P4-AS`。P4-AS0でbuild-vs-buyを再承認し、論理的・security責任上分離する。別artifact / process / deployable等の物理topologyはP4-AS0で決定する | Phase 4必須化にはGrand Design / DoD / 見積変更が必要 | **APPROVED — OWNER 2026-08-31** |
| T-4 | hosted採用時は§6のSpring標準rotation / invalidation / revocation / JDBC persistenceを採用し、Python固有family全失効 / hash保存を必須化しない | 追加保証が必要なCustomerはexternal Providerまたは別拡張を承認 | **APPROVED — OWNER DIRECTION 2026-08-31** |
| T-5 | Phase 2ではtoken endpoint / table / Java Public APIを作らない | Python互換API移植はSpring / OAuth標準とPhase境界を崩す | **APPROVED — OWNER 2026-08-31** |
| T-6 | JWT即時失効を既定保証せず、short TTLを基本として追加方式はP4-AS0 / AS3で選ぶ | 常時DB denylist / introspectionは可用性・性能・運用を増やす | **APPROVED — OWNER 2026-08-31** |

### 11.1 Owner approval record

2026年8月31日、Architecture OwnerはT-1〜T-6と本文全体を承認した。先行して示されたT-4のOwner方針に加え、
Phase 2ではKOIKI自身によるJWT Access / Refresh Token発行APIを実装せず、外部Authorization Serverのtokenを受ける
OAuth2 Client / Resource Serverまでとする境界を確定した。将来KOIKI-hosted issuerが必要となる可能性は保持するが、
Phase 4 optional `P4-AS`のP4-AS0でuse case、build-vs-buy、運用責任および物理topologyを再承認する。

この承認によりP2-F4を`COMPLETE`とする。承認はP4-AS implementation、Authorization Server依存、token table、
production endpointまたは物理的な別deployableの開始を意味せず、Phase 2からの除外と後続traceabilityを確定するものである。

## 12. Official references

- [OAuth 2.0 Security Best Current Practice — RFC 9700](https://www.rfc-editor.org/rfc/rfc9700.html)
- [OAuth 2.0 Token Revocation — RFC 7009](https://www.rfc-editor.org/rfc/rfc7009.html)
- [OAuth 2.0 Token Introspection — RFC 7662](https://www.rfc-editor.org/rfc/rfc7662.html)
- [JWT Profile for OAuth 2.0 Access Tokens — RFC 9068](https://www.rfc-editor.org/rfc/rfc9068.html)
- [Spring Security Authorization Server overview](https://docs.spring.io/spring-security/reference/servlet/oauth2/authorization-server/index.html)
- [Spring Security Authorization Server core model / components](https://docs.spring.io/spring-security/reference/servlet/oauth2/authorization-server/core-model-components.html)
- [Spring Security Authorization Server configuration model](https://docs.spring.io/spring-security/reference/servlet/oauth2/authorization-server/configuration-model.html)
