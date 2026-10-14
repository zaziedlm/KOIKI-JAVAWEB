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
| Work 4a additions | 2026-10-02 OWNER APPROVED — 文書承認（§1.2） |
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

### 1.2 Work 4a document update — 2026-10-02

§8.1〜§8.3、§9.1、§12の追加項目は、[作業順序案](pre-phase4-framework-independent-work-review-20260930.md)の作業4aとして反映した。Architecture Ownerは2026-10-02にALBまわりの記載内容を確認し、問題なしとして文書承認した（**OWNER APPROVED**）。2026-09-18の承認とは別の追加差分の承認記録である。
[見直し草案§8.5](KOIKI-JavaWeb-FW_Phase4実施計画_見直し草案_v0.1.md#85-新しい入口の作業で行うこと)の1・2を対象とし、認証終端・IdP方式、P4-04 / P4-EDGEの採否は顧客確認とOwner判断を待つ。
2026-10-02にAWS / Springの一次資料とReference sourceを照合した。ALB / Cognito / BFFの実環境検証は行っていない。

今回の承認は設計観点・比較・判断記録様式の文書化を対象とする。実案件の方式選定、実環境検証、cloud Adapter実装、P4-04 / P4-EDGEの採用、Phase 4開始またはremote操作の承認を含まない。

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

### 8.1 Choose the authentication termination and KOIKI credential

BFF→KOIKIの検証方式は、IdPがOIDCかSAMLかだけでは決まらない。**Browserの認証をどこが終端し、KOIKIへ何を渡すか**を先に記録する。

- KOIKIがOIDC ClientとSessionを持つならM / Sの責務を確認する。
- BFFがOAuth ClientとなりAccess Tokenを取得するなら§6のBを確認する。
- ALBがBrowserを認証するなら、直接KOIKIへ接続する§8と、BFFを経由する§8.2を区別する。

### 8.2 ALB in front of a BFF — Profile E + B

```text
Browser (ALB authentication Cookie; BFF Cookie if separately used)
  -> ALB (OAuth Client / authentication termination)
    -> Customer-owned Next.js BFF (ALB-provided information)
      -> explicitly selected API credential
        -> KOIKI (credential validation -> Identity / Permission / Use Case)
```

E＋Bは配置・責務の比較用表記で、新しいFramework profileではない。この経路ではALBがOAuth Clientとなるため、§6の「BFFがcodeを交換しTokenをrefreshする」構成を自動的に重ねない。BFFが別のSession・OAuth flowを持つかはCustomerが明示する。

| BFF→KOIKIへ渡す候補 | KOIKI側の確認 | 現行契約との関係 |
|---|---|---|
| `x-amzn-oidc-accesstoken`のIdP Access TokenをBearerとして送信 | JWTならIdP署名・issuer・time・API対象・scopeとIdentity対応を検証。ALB転送だけを信用しない | 標準JWT Resource Serverの候補。ALBが取得したTokenにKOIKI APIの権限があるかを確認する |
| ALB署名の`x-amzn-oidc-data` | ES256署名、期待する`signer`（ALB ARN）、headerの`exp`、issuer / client等を確認。鍵は地域別endpointから`kid`で取得 | IdP JWKSによる通常のBearer検証とは別契約。P4-EDGEのcloud Adapter候補であり現行未実装 |
| BFF自身の認証情報＋利用者ID | BFFの認証と利用者の委任・認可を区別して設計 | BFFを利用者認証の特権的な代替として信頼する変更になる。現行の推奨経路へ含めずArchitecture reviewへ戻す |

ALBが署名するのは`x-amzn-oidc-data`であり、Access Token / identity headerはunsignedである。IdPのJWT署名検証とALB headerの検証を混同しない。ALBはID Tokenをtargetへ転送しない（[AWS ALB authentication](https://docs.aws.amazon.com/elasticloadbalancing/latest/application/listener-authenticate-users.html)）。

opaque Access TokenはJWT decoderで検証できない。Spring標準にはintrospectionの方式があるが、本書ではKOIKIの検証済み経路に追加しない。IdPの対応、責任と検証範囲を別reviewする（[Spring Security — Opaque Token](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/opaque-token.html)）。

BFFへのALB迂回アクセスと、KOIKIへの未検証header送信を拒否できるnetwork / route境界を決める。Browser用ALB Cookieを送らない通常のBFF→KOIKI通信は、KOIKI前段にもALB認証ruleを置くだけでは認証されないと考えられる（配置からの推論・実環境未検証）。API経路でのredirect / 401の扱いも別に設計する。

### 8.3 Session, refresh and logout across boundaries

ALB Session、BFF Session（採用時）、Cognito / 上流IdP Session、KOIKI Session（M / S併用時）、Access Tokenの有効期限を別々に記録する。

| 境界 | 決めること |
|---|---|
| ALB | refresh可能な条件、Session timeout、未認証requestの応答、logout Cookie処理と戻り先 |
| BFF | 独自Sessionの有無、CSRF、Token保持・破棄、refresh責任者、失敗時の再認証・API応答 |
| Cognito / 上流IdP | logout対象のSession、上流IdPとのlogout連携、Token revokeの実施主体 |
| KOIKI | Session失効とBearer残存時間の区別、Identity無効化・現在Permissionとの突合、Audit |

ALBのrefreshはIdPがRefresh Tokenを提供する場合に行われる。Cookie expiryと認証Session timeoutは別である。logoutでは認証Cookieを失効させ、対応するIdP logoutへ遷移する。`deny`でも認証情報の欠落と期限切れで応答が異なり得るため、Ajax / APIの振る舞いを確認する（[AWS ALB authentication](https://docs.aws.amazon.com/elasticloadbalancing/latest/application/listener-authenticate-users.html)）。

Cognitoのlogoutが上流OIDC / social IdPのSessionまで終了するとは扱わない。SAML SLOも設定と対応の確認を要する（[Cognito logout endpoint](https://docs.aws.amazon.com/cognito/latest/developerguide/logout-endpoint.html)）。
Token revoke後も、署名と期限だけを確認するJWT検証では既発行Tokenを受理し得る。KOIKI APIへの即時失効を保証せず、許容する残存時間と必要な追加対応を案件の判断記録へ残す（[Cognito token revocation](https://docs.aws.amazon.com/cognito/latest/developerguide/token-revocation.html)）。

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

### 9.1 Cognito Access Token and Reference validation are different contracts

[ReferenceApiSecurityConfiguration](../../koiki-reference-app/src/main/java/org/koikifw/reference/identity/configuration/ReferenceApiSecurityConfiguration.java)はissuer / timeに加え`aud`と`token_use=access`を検査し、[ReferenceJwtAuthenticationConverter](../../koiki-reference-app/src/main/java/org/koikifw/reference/identity/configuration/ReferenceJwtAuthenticationConverter.java)は独自claim `koiki_user_id`からIdentityを確認する。これらはReference所有の実装であり、Cognito接続設定としてそのままコピーしない。

| 項目 | Cognito側で確認すること | Customer / KOIKI境界で設計すること |
|---|---|---|
| `aud` / `client_id` / scope | 標準の`aud`はresource bindingを要求した場合に入る。`client_id`は認証したapp clientを示す | API対象の検証とclient制限は別。`client_id`だけをAPI audienceの代用にせず、resource binding・custom scopeと実際の発行Tokenを確認する |
| `token_use` / signature / issuer / time | Access Token種別と発行元・鍵・期限 | ID Tokenを代用しない。Referenceを通す目的で検証条件を単に削除しない |
| `sub` / 独自claim | Cognito発行subjectと、追加claimの有無 | 検証済み`issuer + sub`とFramework userの対応、無効user、現在Permissionを設計。上流SAML識別子や`koiki_user_id`の存在を仮定しない |

claim仕様の根拠: [Cognito — Understanding the access token](https://docs.aws.amazon.com/cognito/latest/developerguide/amazon-cognito-user-pools-using-the-access-token.html)。
Access Tokenのcustomizeはpre token generationのevent versionとfeature plan等に条件がある。Essentials / Plusや一部legacy Liteの適用条件を確認し、独自claim追加を前提に案件構成を固定しない（[Cognito — Pre token generation](https://docs.aws.amazon.com/cognito/latest/developerguide/user-pool-lambda-pre-token-generation.html)）。

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
Authentication termination: ALB / BFF / KOIKI (per route)
IdP / Authorization Server:
OAuth Client owner:
Browser credential:
KOIKI credential:
If ALB precedes BFF, BFF -> KOIKI credential and validator:
IdP JWT / opaque token / ALB-signed claims distinction:
ALB signer / issuer / client / key source (if used):
Session / Token store owner:
Refresh owner and BFF Session presence:
Issuer / audience / scope:
API resource binding / allowed client_id / token_use:
External subject -> Framework user link:
CSRF / CORS boundary:
Login / logout / timeout / revoke:
Session boundaries and accepted Access Token residual lifetime:
ALB direct-bypass prevention (if E):
Business Permission / Audit owner:
Failure and cleanup verification:
Deferred decisions:
Decision owner / review status / evidence:
```

選択によってFramework Public API、Starter、cloud Adapterまたは新deployableが必要になる場合は、実装を先行せず
Architecture Ownerのblocking reviewへ戻す。

顧客入力の未取得は`PENDING`と記録し、比較用の候補を採用済みとして埋めない。P4-04 / P4-EDGEの採否と受入条件はPL1-Q6の回答後にOwnerが判断する。

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
| ALB + BFF / Token forwarding / cross-boundary logout | 作業4aの文書比較のみ。実環境の認証・失効・negative testは未実施 |

## 14. Related documents

- [KOIKI Grand Design — SPA profile](../architecture/grand-design/KOIKI-JavaWeb-FW_グランドデザイン_v0.2.md#135-spa-プロファイル)
- [Phase 2 Security Developer Journey](phase2-developer-journey.md)
- [Token lifecycle phase decision](../architecture/validation/phase2-token-lifecycle-phase-decision.md)
- [Reference engineer-facing journey](../reference/README.md)
- [Pre-Phase 4 Adoption Readiness plan](KOIKI-JavaWeb-FW_Pre-Phase4_Adoption_Readiness計画_v0.1.md)
- [Phase 4見直し草案 — 認証終端の設計観点](KOIKI-JavaWeb-FW_Phase4実施計画_見直し草案_v0.1.md#8-新しい入口へ引き継ぐ設計観点-認証の終端とbffkoiki間の検証)
- [P4-PL1 REST利用境界差分台帳 — PL1-Q6](KOIKI-JavaWeb-FW_Phase4_PL1_REST利用境界差分台帳_v0.1.md#3-p4-ar6へ渡す確認事項)
