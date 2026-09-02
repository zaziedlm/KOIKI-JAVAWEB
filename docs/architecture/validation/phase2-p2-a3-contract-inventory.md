# Phase 2 P2-A3 profile and contract inventory

## 1. Status and scope

- **Validation date:** 2026年9月2日
- **Work package:** `P2-A3 / A3-1`
- **Status:** `IMPLEMENTATION CANDIDATE`
- **Ownership:** Framework Security profile構成 + Tooling-owned非配布T3 fixture
- **Production target:** `koiki-starter-security`
- **Verification target:** `build-support/security-foundation-verification`
- **Baseline HEAD:** `af49446`

本記録は、OIDC Login、Bearer JWT、SPA / BFF / Edge境界をP2-A3で実証する前に、profile matcher、
Spring標準component、dependency、configurationおよびPublic APIの境界を確定するinventoryである。

## 2. Profile boundary

| Profile | Authentication | Matcher / chain owner | Required result |
|---|---|---|---|
| local / OIDC browser | Form LoginまたはOAuth 2.0 LoginからHTTP Session | Customerがbrowser pathとcallback pathを明示する | 未認証redirect、state / nonce、local loginとの共存、generic failure |
| Bearer API | OAuth 2.0 Resource Server JWT | CustomerがAPI pathを明示し、browser chainより独立させる | signature、issuer、audience、time、scope / Permission、401 / 403 |
| BFF ingress | 通常のBearer JWT | Bearer API chain | BFFをtrusted bypassにせず、requestごとに同じJWT validationとPermission判定 |
| Edge Authentication | 検証済みPre-Authenticationの将来接続contract | cloud AdapterはPhase 4候補 | raw AWS headerを認証せず、validated fixture principalも通常認可を通す |
| unmatched | P2-A1 fallback deny | Framework | どのprofileにも一致しないrequestをdeny |

同じpathでSession Cookie、Bearer、ID Tokenまたはraw edge headerをfallback認証しない。matcherの重複、空白、
順序はCustomer chain構成時の責務とし、P2-A3 fixtureで明示chainとunmatched denyを外部観測する。

## 3. Configuration decision

P2-A3では`koiki.security.*` profile propertyを追加しない。次を正本とする。

- OIDC client registration、issuer、client ID、client secret、redirect URIはBoot標準`spring.security.oauth2.client.*`、
  またはCustomer-owned Spring標準beanで構成する。
- Resource Serverのissuer、JWK Set、audience、JWS algorithmはBoot標準
  `spring.security.oauth2.resourceserver.jwt.*`またはCustomer-owned`JwtDecoder`で構成する。
- browser / API matcherとCORS allowlistはCustomer-owned `SecurityFilterChain` / `CorsConfigurationSource`で明示する。
- credential付きCORSとwildcard originを併用せず、Bearer専用pathだけをCSRF対象外にできる。
- direct Token SPAはrisk acceptanceなしにKOIKI defaultとして有効化しない。

Framework独自propertyを追加すると、Customer route、issuer topology、SPA risk decisionを固定するため、実証済みの安定契約に
ならない。P2-A3ではSpring / Boot標準configuration seamと安全なprofile分離をFramework利用契約とする。

## 4. Spring component inventory

| Concern | Selected candidate | Boundary |
|---|---|---|
| OIDC registration / discovery | `ClientRegistrationRepository` / issuer discovery | Cognito専用APIを作らず、標準metadataで構成 |
| authorization request / callback | OAuth 2.0 Login filter chain | state / nonceとcallback failureをSpring標準へ委譲 |
| OIDC principal | `OidcUserService`等の標準seam | T3 fixed issuer + subject link。email一致だけではlinkしない |
| JWT extraction | `BearerTokenAuthenticationFilter` | Authorization headerだけ。Cookie / query / form fallback禁止 |
| JWT validation | `JwtDecoder` / `OAuth2TokenValidator` | signature、issuer、audience、`exp`、`nbf`を検証 |
| authority mapping | `GrantedAuthoritiesMapper` / `JwtAuthenticationConverter` | allowlist済みclaim / scopeだけをPermissionへ変換 |
| CORS | Spring MVC CORS + Security CORS integration | exact origin / method / header allowlist |
| Edge contract | Spring Pre-Authentication候補 | T3ではverified fixture principalとraw header拒否だけ。AWS適合をclaimしない |

独自JWT parser、OIDC Filter、token generator、AWS header verifierまたはAuthorization Serverを作らない。

## 5. Dependency decision

| Purpose | Artifact | Placement |
|---|---|---|
| OIDC Login runtime | `spring-boot-starter-security-oauth2-client` | `koiki-starter-security` production |
| Bearer JWT runtime | `spring-boot-starter-security-oauth2-resource-server` | `koiki-starter-security` production |
| OIDC test support | `spring-boot-starter-security-oauth2-client-test` | verification fixture `test` scope |
| Resource Server test support | `spring-boot-starter-security-oauth2-resource-server-test` | verification fixture `test` scope |
| local issuer / JWKS | JDK HTTP fixture + OAuth2 / JOSE transitive classpath | Tooling test source only |

versionはSpring Boot 4.1.1 BOMだけが管理する。deprecated starter、独立Security BOM、WireMock、hosted IdP SDK、
Authorization Server starter / test starterを追加しない。

## 6. Public API inventory decision

| Contract category | P2-A2 baseline | P2-A3 candidate |
|---|---:|---:|
| Public Java types | 0 | 0 |
| Public configuration properties | 0 | 0 |
| Public Security error codes | 0 | 0 |
| Customer customization bean names | 1 | 1（変更なし） |

Spring Filter、SecurityContext、`Jwt`、`OidcUser`、decoder、converterまたはfixture identityをKOIKI Public APIへ露出しない。
principal / identity linkの公開型が必要な場合はP2-B1 / B2の型単位reviewへ延期し、A3で既成事実化しない。

## 7. T3 acceptance allocation

- OIDC: standard discovery、authorization redirect、state / nonce、callback、issuer + subject link、generic failure。
- Bearer: valid Access Token、signature / issuer / audience / `exp` / `nbf`、unknown `kid`、scope allowlist。
- Credential separation: ID Token、Cookie、query / form token、raw edge headerからBearer API認証を成立させない。
- Authorization: URL / Method双方でPermission不足403、許可時だけ到達。
- CORS: exact allowlist positive、unknown origin、credential + wildcard、許可外method / header negative。
- Non-exposure: response、log、report、artifactへprivate key、token、client secret、claim全文またはPIIを残さない。

## 8. Stop-condition decision

required CIはJDK HTTPによるephemeral local issuer / JWKSを使用し、matcherはCustomer-owned explicit chainとしてfixtureで
固定できるため、P2-A3開始時のprovider / matcher未確定stop conditionには該当しない。

次が必要になった場合は実装を拡張せず停止する。

- production Authorization Server、token発行 / refresh / revokeまたはprivate signing key
- AWS固有production Adapterまたは実ALB環境
- KOIKI独自profile property / Public APIなしでは安全なchain分離が成立しない
- local issuerを正式artifactまたはproduction capabilityへ配置する必要がある

## 9. Deferred decisions

- identity link persistence、account state、principal Public API: P2-B1 / B2
- Session logout / invalidation、権限変更後の全Session失効: P2-B3
- AWS署名、edge ARN / network trust、key rotation実環境適合: Phase 4 cloud Adapter候補
- Authorization Server、Access / Refresh Token lifecycle: optional `P4-AS`
- SPA / BFF production reference implementation: Phase 4

このinventoryにより、P2-A3はSpring標準component、Boot-managed dependencyおよび非配布T3 fixtureだけで開始できる。
