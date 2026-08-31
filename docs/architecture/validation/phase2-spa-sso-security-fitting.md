# Phase 2 React SPA / SSO security fitting

## 1. Status and decision context

- **Fitting date:** 2026年8月31日
- **Phase:** Phase 2 Security Foundation design / fixture
- **Owner direction:** 純SPAだけへ固定せず、Next.js BFFを現実解として含むGrand Designのセキュリティ解釈の進展
- **Production implementation:** 未開始

KOIKI-JavaWebがAPI serverとなり、React SPAが利用する構成を、認証情報の置き場所とOAuth clientの所在で
明示的に分ける。KOIKI本体へReactまたはNext.jsを同梱せず、Spring標準のSession / OAuth2 Client /
Resource Server契約が各構成を支える。

## 2. Updated standards baseline

2026年時点のIETF Browser-Based Applications BCPは、browser applicationに次の3 patternを定義する。

1. Backend for Frontend（BFF）
2. token-mediating backend
3. browser-based OAuth client

BFFはtokenをbrowserへ露出せず、業務application、機微情報、個人情報を扱うapplicationへ強く推奨される。
browser-only OAuth clientはattack surfaceが大きく、これらのapplicationには推奨されない。same-domainの
frontend + APIではOAuth tokenをbrowserとbackendの間に導入せず、server-side Cookie Sessionを使える。

公式根拠:

- [RFC 10017: OAuth 2.0 for Browser-Based Applications](https://www.rfc-editor.org/rfc/rfc10017.html)
- [RFC 9700: Best Current Practice for OAuth 2.0 Security](https://www.rfc-editor.org/rfc/rfc9700.html)
- [OpenID Connect Core 1.0](https://openid.net/specs/openid-connect-core-1_0.html)
- [Spring Security OAuth 2.0 Resource Server JWT](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html)
- [Spring Security CORS integration](https://docs.spring.io/spring-security/reference/servlet/integrations/cors.html)

## 3. Supported deployment profiles

| Profile | Browser authentication material | OAuth client | KOIKI-JavaWeb role | Position |
|---|---|---|---|---|
| S: same-origin Session SPA | Secure HttpOnly session Cookie | KOIKI-JavaWeb OAuth2 Client | application backend + API | **KOIKI default**。業務・PII向け第一候補 |
| B: Next.js BFF | BFFのSecure HttpOnly session Cookie | Next.js BFF confidential client | Bearer JWT Resource Server | **推奨選択肢**。frontendとAPI分離時の高感度用途 |
| T: direct Token SPA | Access Token。browser public clientが保持 | React SPA public client | Bearer JWT Resource Server | **明示opt-in**。risk assessment / acceptance必須 |
| M: token-mediating backend | session Cookie + browserへ渡すAccess Token | Customer backend confidential client | Bearer JWT Resource Server | 初期KOIKI標準にしない。個別評価 |

KOIKIはProfile B用のNext.js runtimeやproxy frameworkを配布しない。CustomerがBFFを所有し、KOIKIは
Resource Server契約、JWT validation、Permission mapping、Problem Detailsを提供する。Profile SではKOIKI-JavaWeb
自身がOIDC loginとsessionを処理するため、別BFFを必須にしない。

## 4. Request topology

```text
Profile S
Browser / React -- HttpOnly Session Cookie + CSRF --> KOIKI-JavaWeb
KOIKI-JavaWeb -- OIDC Authorization Code --> External IdP

Profile B
Browser / React -- HttpOnly Session Cookie + CSRF --> Next.js BFF
Next.js BFF -- Authorization: Bearer Access Token --> KOIKI-JavaWeb API
Next.js BFF -- OIDC Authorization Code + confidential client --> External IdP

Profile T
Browser / React -- Authorization Code + PKCE (S256) --> External IdP
Browser / React -- Authorization: Bearer Access Token --> KOIKI-JavaWeb API
```

Profile間で1つのrequest pathへCookieとBearerのfallbackを設定しない。path / host / SecurityFilterChainを
明示分離し、unmatched requestはdenyする。

## 5. Profile T — frontend-managed SSO contract

### 5.1 React / IdP responsibility

- Authorization Code flow + PKCE `S256`を使用し、Implicit flowとpassword grantを使用しない。
- SPAはpublic clientであり、client secretをbundle、source map、environment fileへ含めない。
- redirect URIは事前登録した完全一致だけを許可する。
- transaction固有のPKCE verifier / challenge、OIDC `nonce`および必要な`state`を検証する。
- ID TokenはSPAがIdPでのauthentication結果を確認するためのtokenであり、KOIKI API呼出には使用しない。
- APIへ送るのはKOIKI API audienceを持つAccess Tokenだけとする。

### 5.2 KOIKI-JavaWeb Resource Server responsibility

- `Authorization: Bearer`だけからAccess Tokenを取得し、query、form、Cookieからfallbackしない。
- Spring Security `JwtDecoder`でsignature、許可algorithm、`iss`、`aud`、`exp`、`nbf`を検証する。
- scope / authorityをPermissionへmappingし、UI route guardを認可境界にしない。
- 外部identityは`issuer + subject`でimmutable Framework userへlinkし、email claim単独で同一人物と判断しない。
- ID Token、誤audience、unknown issuer、expired / not-yet-valid、scope不足を拒否する。
- token、claim全文、email、subjectを通常log、Problem Details、metric tagへ出さない。

### 5.3 CORS / CSRF boundary

- Customerが完全一致のSPA origin、method、request header、exposed headerをallowlistする。
- wildcard originとcredential許可を併用しない。Bearer専用APIではCookie credentialを送受信しない。
- CORS preflightをSecurity認証より前に処理し、許可originの`OPTIONS`だけを通す。
- Bearer専用pathはambient Cookieへ依存しないためCSRF対象外にできる。Cookieを同じpathで受理する場合はこの例外を適用しない。
- CORSは認証・認可ではない。非browser clientからの直接requestもJWT / Permissionで拒否する。

### 5.4 Token storage / refresh / logout

- browser-only token方式ではpersistent storageをKOIKI推奨にしない。memory / Workerを含む方式もXSS時のriskを消さない。
- refresh tokenをbrowserへ発行するかはAuthorization Serverのrisk decisionとする。発行する場合、rotationまたは
  sender constraint、maximum lifetime / inactivity expiry等のRFC 9700 / 10017要件を満たす。
- refresh tokenをKOIKI Resource Server APIへ送らない。発行、refresh、revoke、reuse detectionはIdP / Authorization Server責務とする。
- SPA内token破棄、KOIKI session logout、BFF session logout、IdP logout / global logoutを別の操作として契約化する。

## 6. Profile B — Next.js BFF contract

- Next.js BFFはCustomer-owned confidential OAuth clientとし、client credential、Access Token、Refresh Tokenをserver側で保持する。
- browserへOAuth tokenを返さず、Secure / HttpOnly / SameSite CookieとCSRF防御でBFF sessionを表現する。
- BFFはKOIKI API向けaudience /最小scopeのAccess Tokenだけを付与してserver-to-serverで呼び出す。
- KOIKI-JavaWebはBFFを信頼境界としてbypassせず、各requestのJWTとPermissionを通常どおり検証する。
- BFFがmultiple resource server tokenをcacheする場合、scope / audienceのsuperset tokenを無条件再利用しない。
- BFFのsession fixation、CSRF、redirect allowlist、token refresh、logout、secret rotation、observabilityはCustomer acceptanceに含める。

## 7. Ownership

| Concern | Owner |
|---|---|
| Session / Bearer chainの安全な既定と分離可能なcontract | Framework |
| JWT validation、authority mapping extension、safe error / audit hook | Framework |
| React route、OIDC client library、PKCE / nonce / state処理 | Customer / Phase 4 Reference |
| Next.js BFF runtime、session、token store、proxy、client secret | Customer / Phase 4 Reference candidate |
| issuer、client registration、redirect URI、audience、claim mapping | Customer |
| local User / external identity link / Permission | Framework contract + Customer mapping |

## 8. Phase allocation

| CP / Phase | Deliverable |
|---|---|
| P2-F2 | ACCEPTEDのProfile S / B / Tを前提に、identity / CORS / logout semanticsとrisk acceptance手順を確定 |
| P2-F3 | Profile S / B / Tのtest topology、threat / negative-path matrix、Spring component mappingを文書化。code変更は0 |
| P2-A3 | Resource Server、CORS、issuer / audience / time / scope、ID Token拒否のnegative test |
| P2-B3 | Profile SのSpring Session JDBC、CSRF、2 instance、logout |
| Phase 3 | REST API / OpenAPIとSPA向けerror / authorization contractの文書化 |
| Phase 4 | React reference。Profile Sを必須実証し、Profile Bを最小参照候補、Profile Tはrisk acceptance付きで検証 |

## 9. Required acceptance tests

1. Profile Sのunsafe requestはCSRFなしで拒否され、Bearer chainへfallbackしない。
2. Profile TのAccess Tokenは正しい`iss` / `aud` / time / scopeでだけ成功する。
3. ID TokenをBearerとして送ると拒否される。
4. CookieだけをBearer専用pathへ送っても認証されない。
5. 許可外origin、method、headerのpreflight / actual requestが拒否される。
6. BFF相当clientのBearer requestにも通常のMethod Security / Permissionが適用される。
7. token / authorization code / PKCE verifier / nonce / subject / emailがlog、error、artifactへ露出しない。
8. logout種別ごとにsession / token / IdP状態の残存範囲が期待どおりである。

## 10. Grand Design evolution

旧記述の「BFF層も設けない」は、KOIKI本体がBFF runtimeを必須配布しない判断として維持するが、BFF architectureの
禁止とは解釈しない。2026年のBCPとOwner directionを受け、次へ発展させる。

- same-originではserver-side Sessionを第一標準とする。
- frontend / API分離かつ業務・PII用途ではNext.js BFFを推奨選択肢とする。
- direct Token SPAはsupportするが第一標準にせず、明示risk acceptanceを要する。
- 3 profileは認証transportだけが異なり、同じApplication Use Case、Permission、auditを使用する。
