# Phase 2 P2-A3 T3 OIDC and Bearer profile verification

## 1. Status and scope

- **Validation date:** 2026年9月2日
- **Work package:** `P2-A3 / A3-2〜A3-4`
- **Status:** `COMPLETE / COMMIT READY`
- **Branch:** `feature/phase2-security-foundation`
- **Baseline HEAD:** `af49446`
- **Java / Maven:** Java 21.0.12.1 / Maven 3.9.16

本記録は、P2-A2までのdefault deny、local Session、URL / Method Security、CSRF / Security HeaderおよびPublic API境界を
維持し、Spring標準OIDC Login、Bearer JWT、SPA / BFF / API profile境界を非配布T3 fixtureで実証したEvidenceである。

## 2. Implemented boundary

### 2.1 Framework production scope

- Boot-managed `spring-boot-starter-security-oauth2-client`と
  `spring-boot-starter-security-oauth2-resource-server`をSecurity Starterへ追加した。
- OIDC / JWTの独自parser、Filter、token発行、profile property、Public Java型またはSecurity error codeは追加していない。
- P2-A1 fallback chainとP2-A2 Method Security構成は変更していない。

### 2.2 Tooling fixture scope

- test実行時にRSA鍵とloopback JDK HTTP issuer / token / JWKS endpointを生成し、終了時に破棄した。
- authorization-code、state、nonce、ID Token署名検証をSpring OAuth2 Loginへ委譲した。
- Resource Serverの`JwtDecoder`とvalidatorで署名、issuer、audience、`exp`、`nbf`、`token_use`を検証した。
- exact allowlist scopeだけをPermission authorityへ変換した。
- browser、API、callbackおよびunmatched pathをCustomer-owned chainで分離した。
- test key、client、token、routeおよびidentityは正式artifactや`koiki-testing`へ昇格していない。

## 3. T3 observable results

| Requirement / path | Observable result |
|---|---|
| OIDC authorization-code | authorization redirectからstate / nonceを受け、token endpointを1回だけ交換してSession成立 |
| OIDC state mismatch | token交換前に拒否し、generic `/login?error`へredirect |
| valid Access Token | runtime生成RS256 JWTで保護処理へ到達 |
| invalid JWT | wrong signature、issuer、audience、expired、future `nbf`を401で拒否 |
| scope mapping | exact `fixture.read`だけ許可し、unknown / case違い / malformedを403で拒否 |
| credential separation | Cookie、query token、raw edge header、ID TokenをBearer API認証に使用しない |
| profile separation | OIDC SessionをAPIへ流用せず、raw Bearerをbrowser認証へ流用しない |
| unmatched path | OIDC Session成立後もP2-A1 fallbackが403で拒否 |
| CORS | exact origin / GET / Authorization headerだけ許可し、unknown origin / method / headerを拒否 |
| non-exposure | rejected token、client secret、private keyをresponse、report、formal artifactへ露出しない |

## 4. Verification results

| Verification | Result |
|---|---|
| P2-A3 aggregate | isolated release 11 / 11、dependency / artifact / Public API / sensitive-content検査success |
| cumulative Security fixture | T0 / T1 / T2 / T3 23 tests、failure / error / skip 0 |
| T3 fixture | Bearer 5 + OIDC 2 = 7 tests、failure / error / skip 0 |
| dependency boundary | Boot 4.1.1 / Spring Security 7.1.1。Authorization Server、SAML、Redis、WebFlux、Reactorなし |
| Public API inventory | P2-A1 inventoryと完全一致 |
| Diff hygiene | `git diff --check` success |

再現commandは次である。

```powershell
pwsh -NoProfile -File build-support/security-foundation-verification/verify-p2-a3-oidc-bearer.ps1
```

## 5. Public API and ownership review

| Contract category | P2-A3 result |
|---|---:|
| Public Java types | 0 |
| Public configuration properties | 0 |
| Public Security error codes | 0 |
| Customer customization bean names | 1（変更なし） |

OIDC registration、issuer、client ID / secret、redirect URI、API audience、scope mapping、matcherおよびCORSは
Customer / deployment構成が所有する。FrameworkはSpring / Boot標準seamを提供し、fixture credentialやCognito固有型を公開しない。

## 6. DoD and threat trace

| Trace | P2-A3 Evidence |
|---|---|
| DoD 2-3 | OIDC authorization-code、state / nonce、ID Token署名、issuer、subjectによるSession成立 |
| DoD 2-4 | Bearer JWTの署名、issuer、audience、時刻、scope検証と401 / 403分離 |
| N-02 | Access TokenとID Token、Cookie、query token、raw edge headerの用途混同を拒否 |
| N-03 | browser Session、Bearer API、unmatched fallbackを明示matcherとchain順序で分離 |
| N-04 | CORS origin / method / headerをexact allowlist化し、negative pathを拒否 |
| N-05 | allowlist済みscopeだけをPermissionへ変換し、case / malformed / unknownを拒否 |

## 7. Excluded scope confirmation

- production Authorization Server、Access / Refresh Token lifecycle、private signing key
- Cognito / ALB / AWS固有production Adapter、raw edge header verifier
- identity link persistence、account state、audit persistence、Session全失効
- direct-token SPAの既定化、production BFF / Reference implementation
- test issuer、key、client、token、identityまたはrouteの正式artifact / `koiki-testing`昇格

## 8. Conclusion

P2-A3はSpring標準componentとBoot-managed dependencyだけでOIDC / Bearer coexistenceを成立させ、P2-A1 / A2の
fallback、Method SecurityおよびPublic API境界を維持した。P2-A3差分としてcommit可能であり、次はGate A総合判定へ進む。
