# KOIKI-JavaWeb-FW UI / Authentication Profile Selection Guide v0.1

## 1. Purpose and status

本書は、KOIKI-JavaWebを採用する業務アプリ開発者が、UI配置と認証方式を同時に選ぶための入口である。
特に次の要求を両立するときの責務境界を整理する。

- KOIKI単体のpackage済みJARで、業務UI / UXまで提供したい。
- React SPAまたはNext.jsをfrontendとして選べるようにしたい。
- Cognito、企業OIDC / SAML IdPまたはALBによるSSOを利用したい。
- UI方式が違っても、同じApplication Use Case、Domain、DB、PermissionおよびAuditを利用したい。

| Item | Position |
|---|---|
| Status | `OWNER APPROVED — P4-AR5 developer handoff input` |
| Approval | Architecture Owner — 2026-09-18 |
| Framework baseline | Phase 2 SecurityとPhase 3 MVC / RESTは`COMPLETE / ACCEPTED` |
| Implemented Reference | Thymeleaf / HTMX Session MVC、Bearer REST、両経路のcritical journey |
| Not yet promised | React / Next.js production reference、ALB Adapter、KOIKI-hosted Authorization Server |
| Decision owner | 実案件のUI topology、IdP、OAuth Client、route、claim / scopeおよびdeploymentはCustomer Ownership |

本書はProject Template、production構成の承認、React / Next.js実装またはPhase 4開始承認ではない。P4-AR5 / AR6で
受渡し候補とFramework / Customer / joint責任を判断し、見直し後Phase 4計画へ入力する。

### 1.1 Owner approval scope

Architecture Ownerは、現時点で実案件から反映できる情報に限界があることを前提として、次の整理を承認した。

- UI / application shapeと認証profileの現時点の選択肢
- Framework、Customer / frontend、外部IdPおよびAWS edgeの責任境界
- 実装・検証済み範囲と、今後の実証または設計判断を要する範囲の区別
- P4-AR5 developer handoff整理および見直し後Phase 4計画への入力としての利用

この承認は、実案件に対する`M / S / B / T / E`の選定、production topology、IdP / OAuth Client、claim / scope、
React / Next.js / ALB固有実装、新規Public API / Starter / dependency / workflow、またはPhase 4開始を承認するものではない。
実案件の情報が具体化した時点で、本書のdecision recordを用いて案件固有判断を追記する。

## 2. Start with two application shapes

最初に、画面技術ではなくdeployable境界を選ぶ。

| Shape | Description | Typical choice |
|---|---|---|
| A — KOIKI integrated UI | 1つのSpring Boot JARがHTML、認証Session、Application Use Case、DB接続を所有する | Spring MVC + Thymeleaf + HTMX |
| B — Separated frontend | ReactまたはNext.jsをKOIKI APIと別のfrontend境界に置く | same-origin SPA、Next.js BFF、direct Token SPA |

どちらも業務処理の正本はKOIKI Application / Domainに置く。frontendへ業務認可、状態遷移またはDB更新責務を
移さない。MVCとRESTは異なるInbound Adapterであり、同じUse Caseを利用する。

```text
MVC Controller ───────────────┐
                              ├─> Application Use Case -> Domain -> Repository -> DB
REST Controller <─ SPA / BFF ─┘
```

認証SessionやTokenをMVC / REST間で共有して業務を接続するのではない。同じ業務IDとDBへ永続化した状態を介して、
複数の利用者とchannelが同じworkflowへ参加する。

## 3. Profile catalog

| Profile | Browser credential | OAuth Client / login owner | KOIKI boundary | Position |
|---|---|---|---|---|
| M — MVC integrated | KOIKI `SESSION` Cookie | KOIKI Spring OIDC Clientまたはlocal Form Login | Session MVC | 実装・package JAR検証済み |
| S — same-origin React | Secure HttpOnly KOIKI Session Cookie | KOIKI Spring OIDC Client | Session API + CSRF | SPAの第一標準。Phase 4実証前 |
| B — Next.js BFF | BFF Session Cookie | Customer-owned Next.js server | BFFからBearer JWT | frontend / API分離の業務・PII用途で推奨候補。production参照未実装 |
| T — direct Token SPA | Access Token | React public client | BrowserからBearer JWT | 明示opt-in。risk acceptance必須 |
| E — ALB edge auth | ALB authentication Session | ALB + Cognito / OIDC Provider | verified edge claims | AWS固有Adapter未実装 |

`S / B / T`はGrand DesignのSPA選択に対応する。`M / E`は本書内の比較用略号であり、新しいFramework profile、
property、StarterまたはPublic APIを定義しない。

Profileはroute単位で明示し、同じpathへSession、Bearer、ID Tokenまたはraw edge headerをfallbackさせない。

## 4. Profile M — single JAR MVC

```text
Browser
  -> KOIKI login / OIDC callback
    -> Spring Session JDBC
      -> MVC Controller
        -> Application Use Case / Domain / DB / Audit
```

### Suitable when

- 単一JARで画面、業務処理、認証Sessionを運用したい。
- server-side HTMLで必要なUI / UXを実現できる。
- frontend build / deploy / runtimeを分離する必要がない。
- CSRF、Cookie、logoutをSpring Security / Spring Sessionへ集約したい。

### External SSO

Application-direct OIDCでは、KOIKIがOAuth Client / OIDC Relying Partyとなる。Cognito User Poolが企業SAML IdPを
上流に持つ場合も、KOIKIからはCognitoのOIDC endpointへ接続できる。SAML AssertionをKOIKIが直接処理せず、
Cognitoが認証結果をOIDCへ標準化する。

login完了後のBrowserはKOIKI Session Cookieを使う。Cognito Access TokenをBrowserへ保存してMVCを呼ぶ構成ではない。

### Current evidence

- package済みReference JARのlocal Session MVC
- Spring Session JDBC
- CSRF / Security Header / default deny
- Thymeleaf / HTMX、validation、browser history、optimistic lock conflict
- MVC / DB / Business and Security Audit

## 5. Profile S — same-origin React with KOIKI Session

```text
Browser / React
  -> KOIKI OIDC login
    -> Secure HttpOnly KOIKI Session Cookie
      -> same-origin KOIKI API
        -> Application Use Case / Domain / DB / Audit
```

ReactへJWTを保持させず、KOIKI backendがOIDC ClientとSessionを所有する。BrowserはSession Cookieを自動送信し、
unsafe requestはCSRFで保護する。

### Required decisions

- React assetとAPIを同一originに置く方法
- SPA login開始、callback、未認証時の応答
- CSRF tokenのmaterializeとheader送信
- Session timeout、logout、権限変更時の全Session失効
- MVC routeとSPA API routeを併用する場合のSecurityFilterChain分離

これはGrand Design上の第一標準だが、React production referenceとCookie Session APIのPhase 4実証は未完了である。

## 6. Profile B — Next.js BFF

```text
Browser / React
  -> BFF Session Cookie
    -> Next.js BFF (confidential OAuth Client)
      -> server-side Access / Refresh Token storage
        -> Authorization: Bearer <Access Token>
          -> KOIKI Resource Server
            -> Application Use Case / Domain / DB / Audit
```

BrowserへOAuth Tokenを露出せず、Next.js serverが認証flow、BFF Session、Token保管およびrefreshを所有する。
KOIKI APIはBFFをtrusted bypassとして扱わず、Bearer JWTをrequestごとに検証する。

### BFF Ownership

- Authorization Code flow、client secret、redirect URI
- BFF Session Cookie、CSRF、Session fixation、timeout
- Access / Refresh Tokenのserver-side保管、rotation、revocation
- KOIKI APIのaudience / scopeを持つAccess Tokenの送信
- Browser向けresponse整形とfrontend固有aggregation

### KOIKI Ownership

- JWT signature、issuer、audience、time、`token_use`、scopeの検証
- 外部subjectとFramework userのlink
- 現在Permission、業務scope、Domain不変条件
- Business / Security Audit

frontend / APIを別deployableにし、業務情報または個人情報を扱う場合の有力候補である。ただし現時点のNext.js
production referenceは未実装であり、Customer-owned BFFとしてP4-AR6で責任分担を確定する。

## 7. Profile T — direct Token React SPA

```text
React SPA (public client)
  -> Authorization Code + PKCE
    -> external Authorization Server
      -> Access Token
        -> Authorization: Bearer <Access Token>
          -> KOIKI Resource Server
```

client secretをBrowserへ置かず、APIへはKOIKI API audienceのAccess Tokenだけを送る。ID TokenをAPI認証へ
利用せず、Implicit flowを採用しない。

次を承認できる場合だけ選択する。

- XSS時のToken流出riskとCSP / dependency governance
- Browser内Token保持方式
- Refresh Tokenを発行するか、再認証へ戻すか
- PKCE、exact redirect URI、CORS allowlist
- logout、revoke、Access Token残存時間
- public clientとして満たすべきrotation / replay対策

軽量さだけを理由にProfile S / Bから変更しない。

## 8. Profile E — ALB edge authentication

```text
Browser
  -> ALB authenticate-oidc / authenticate-cognito
    -> ALB authentication Session
      -> X-AMZN-OIDC-* headers
        -> cloud-specific verified pre-authentication Adapter
          -> KOIKI Application
```

ALB OIDCは通常の`Authorization: Bearer`とは別契約である。`x-amzn-oidc-data`の署名、期待するALB ARN、issuer、
client、expiry、Applicationへのdirect bypass防止を検証する必要がある。unsigned headerを単独で認証根拠にしない。

現行KOIKIはAWS固有Adapterを提供しない。ALB edge authを採用する場合は、Customer / cloud Ownershipとして
blocking review、threat、network topology、negative testおよびAudit mappingを追加する。

参考: [AWS — Authenticate users using an Application Load Balancer](https://docs.aws.amazon.com/elasticloadbalancing/latest/application/listener-authenticate-users.html)

## 9. Cognito and enterprise SAML placement

企業SAML IdPとCognito User Poolを組み合わせる場合、CognitoがSAML Assertionを受け、ApplicationへOIDC / OAuth
Tokenを発行する境界を第一候補とする。

```text
Enterprise SAML IdP
  -> Cognito User Pool
    ├─ Profile M / S: KOIKI OIDC Clientへauthorization code
    ├─ Profile B: Next.js BFFへauthorization code / token
    └─ Profile T: React public clientへauthorization code + PKCE
```

Bearer profileでは、Cognito側とKOIKI側で少なくとも次を一致させる。

- issuer / JWKS
- Access Token audienceまたはresource binding
- custom scope
- `token_use=access`
- Cognito subjectとFramework user IDのlink
- logout / revoke / account disable時の残存Access Token方針

参考: [AWS — User pool sign-in with third-party identity providers](https://docs.aws.amazon.com/cognito/latest/developerguide/cognito-user-pools-identity-federation.html)

## 10. Responsibility matrix

| Concern | IdP / Authorization Server | MVC / BFF / SPA OAuth Client | KOIKI API / Application |
|---|---|---|---|
| User authentication / MFA | Own | flow開始・callback | 認証UIを代替しない |
| Access Token issue | Own | 取得・必要ならrefresh | 発行しない |
| Browser Session | Own IdP Session | Profile M/SはKOIKI、BはBFF、Tはなし | 選択profileだけを処理 |
| Token storage | private key / grantをOwn | Bはserver-side、Tはrisk decision | Access / Refresh Tokenを永続保管しない |
| API authentication | metadata / JWKSを公開 | Access Tokenを送信 | requestごとにJWTを検証 |
| Business authorization | Ownしない | UI表示制御は補助だけ | Permission、scope、Domain ruleを必ず検証 |
| Business state | Ownしない | DB状態を正本にしない | Application / Domain / DBがOwn |
| Audit | IdP login / issuance | BFF Session / Token operation | API access、Identity link、業務操作 |
| Logout | IdP Session / revoke | Browser / BFF Session、Token削除 | KOIKI Session失効またはToken残存windowを区別 |

## 11. Selection guidance for the current stated needs

「KOIKI単体でもUI / UXを提供する」「React要望が強い」「外部IdP SSOを使う」という要求では、次の順に比較する。

1. **Profile Mを維持する。** KOIKI単体で完結する正式なserver-side UI optionとして残す。
2. Reactが同一site / originで成立するなら、**Profile Sを第一候補**として検証する。BrowserへOAuth Tokenを出さない。
3. frontendとAPIを別deployableにし、業務・PIIを扱うなら、**Profile Bを有力候補**とする。
4. **Profile Tはrisk acceptanceが成立した場合だけ**選ぶ。
5. ALB SSOがinfrastructure標準なら、Profile EをM / S / Bの代替と決めつけず、edgeからどのApplication Sessionまたは
   Token境界へ接続するかを別途決める。

Profile MとS / Bは排他的ではない。管理画面をMVC、利用者画面をReactとし、同じApplication Use Case / DBを使う構成も
可能である。ただしroute、SecurityFilterChain、CSRF / CORS、logoutおよび認証状態をprofileごとに分離する。

## 12. Required decision record before implementation

実案件は実装前に次を1枚のdecision recordへ記録する。

```text
UI topology:
Deployables / origins:
Selected profile(s): M / S / B / T / E
IdP / Authorization Server:
OAuth Client owner:
Browser credential:
KOIKI credential:
Session / Token store owner:
Issuer / audience / scope:
External subject -> Framework user link:
CSRF / CORS boundary:
Login / logout / timeout / revoke:
ALB direct-bypass prevention (if E):
Business Permission / Audit owner:
Failure and cleanup verification:
Deferred decisions:
```

選択によってFramework Public API、Starter、cloud Adapterまたは新deployableが必要になる場合は、実装を先行せず
Architecture Ownerのblocking reviewへ戻す。

## 13. Current verification boundary

| Capability | Current evidence |
|---|---|
| MVC single JAR / Session | Phase 3 Reference browser journey、P4-AR4 package JAR再検証 |
| Bearer Resource Server | Phase 2 profile verification、Phase 3 Reference API journey |
| MVC + Bearer in one Spring process | Phase 3 Critical Journey E2E |
| DB / Business Audit across channels | Phase 3 Critical Journey E2E |
| React same-origin Session | Grand Design contract。Phase 4実装・実証前 |
| Next.js BFF | architecture option。Customer-owned production reference未実装 |
| direct Token SPA | contract / risk boundary。production reference未実装 |
| Cognito / enterprise SAML production integration | external IdP option。実案件environmentで未検証 |
| ALB edge Adapter | cloud-specific後続成果物。未実装 |

## 14. Related documents

- [KOIKI Grand Design — SPA profile](../architecture/grand-design/KOIKI-JavaWeb-FW_グランドデザイン_v0.2.md#135-spa-プロファイル)
- [Phase 2 Security Developer Journey](phase2-developer-journey.md)
- [Token lifecycle phase decision](../architecture/validation/phase2-token-lifecycle-phase-decision.md)
- [Reference engineer-facing journey](../reference/README.md)
- [Pre-Phase 4 Adoption Readiness plan](KOIKI-JavaWeb-FW_Pre-Phase4_Adoption_Readiness計画_v0.1.md)
