# Phase 2 Security identity / API / SPA / SSO semantics fitting

## 1. Status and scope

- **Fitting date:** 2026年8月31日
- **Work package:** `P2-F2`
- **Status:** `P2-F2 COMPLETE / OWNER APPROVED`
- **Architecture Owner:** Shuichi Kataoka
- **Approval date:** 2026年8月31日
- **Approval scope:** 本文全体およびO-1〜O-6の推奨案
- **Production implementation:** 未開始
- **Public Java API / table / migration:** 未確定

本記録は、local login、application-direct OIDC、Bearer Resource Server、SPA / BFF、Edge Authenticationで
共通に使用するidentity、authority、login / logout、error、audit semanticsをOwner review可能な形へ固定する。
Spring Security標準で表現できる機構をKOIKI独自frameworkとして再実装せず、KOIKI固有契約は永続identity、
Permission、identity link、auditおよびCustomer mapping境界へ限定する。

P2-F2ではproduction code、dependency、Public Java type、endpoint、tableまたはmigrationを作らない。
実装可能性とnegative pathはP2-F3でtest designへ変換し、Gate P2-2後のP2-A1〜B3で実証する。

## 2. Accepted direction carried into P2-F2

次はOwner directionまたは既存ACCEPTED設計として扱い、方式選択へ戻さない。

1. Spring MVC / ThymeleafではSpring Security + HTTP Sessionを第一標準とする。
2. local loginはemail address + passwordを利用し、emailとimmutable Framework user IDを分離する。
3. same-origin Session、Customer-owned Next.js BFF、direct Token SPAを別profileとして許容する。
4. direct Token SPAは明示risk acceptanceを要し、KOIKI APIへID Tokenを送らない。
5. application-direct SSOはSpring Security OIDC Clientを使用し、Amazon Cognito User Poolを標準OIDC Provider候補とする。
6. ALB＋Cognito等はEdge Authenticationとし、cloud固有Adapterなしにraw proxy headerを信用しない。
7. Bearer pathはSpring Security Resource ServerとしてAccess Tokenをrequestごとに検証する。
8. KOIKI-PYFWのendpoint、自前JWT、JWT Cookieまたはtableを互換移植しない。

## 3. Canonical identity semantics

### 3.1 Framework user

`Framework user`は認証方式から独立したKOIKI内の主体である。

- immutableでopaqueな`Framework user ID`を持つ。
- email、OIDC `subject`、employee number、Cognito usernameをFramework user IDとして使用しない。
- session principal、audit actor / subject、Role / Permission relation、external identity linkはFramework user IDを参照する。
- IDのJava型、採番方式、物理列型はP2-B1 / B2のPublic API・Oracle fixture reviewまで固定しない。
- Customerの社員番号、所属、雇用状態、tenant固有属性をFramework userへ埋め込まない。

### 3.2 Account state

認証可否に関する最小状態を次のように分離する。

| Concern | Semantics |
|---|---|
| lifecycle status | `ACTIVE` / `DISABLED`を最小集合とする。招待、仮登録、退職等はCustomer workflowまたは後続拡張 |
| local credential lock | password認証失敗等による一時lock。lifecycle statusや外部authenticatorと混同しない |
| local credential | local loginを使うユーザーだけが持つ。SSO-only userへdummy passwordを要求しない |
| external identity | 0件以上の検証済みlink。email属性とは別に管理 |
| authorization state | Role / Permission割当。認証credentialと独立して変更可能 |

`DISABLED` userは全方式の新規認証を拒否する。local credential lockは新規password loginだけを拒否し、
検証済みOIDC / Edge loginを第三者のpassword試行で停止させない。管理者が全方式を停止する場合は`DISABLED`を使用する。
既存session、将来tokenおよび外部linkへの失効波及は§8で定義する。

### 3.3 Email login identifier

- Phase 2標準では別の`username`を追加せず、emailを唯一のlocal login identifierとする。
- 入力・表示用emailと、一意lookup用canonical valueを区別する。
- provider固有のdot除去、plus tag除去、alias推測は行わない。
- email変更後もFramework user ID、audit履歴、external identity linkは変化しない。
- emailはPIIであり、通常log、metric tag、Problem Details、URLへ出さない。

case folding、国際化emailの初期support範囲およびcanonicalization algorithmは承認済みdecision O-1とする。
一度production migrationへ固定した後に変えると一意性衝突を起こすため、P2-B2開始前に決定する。

## 4. External identity and trust source

### 4.1 Stable link key

外部identityは、検証済みの`trust source + external subject`でFramework userへlinkする。

- application-direct OIDCではtrust sourceを検証済み`issuer`、external subjectを`sub`とする。
- OIDC `issuer`と`sub`はcase-sensitiveな値として扱い、email claimで代用しない。
- Cognito User PoolもCognito固有user nameではなく、検証済みissuer + subjectを使用する。
- Edge Authenticationでは、cloud Adapterが署名、期待するedge識別子、到達経路を検証した後にだけ、
  設定済みtrust sourceとexternal subjectをKOIKIへ渡す。
- 1つのtrust source + external subjectを複数Framework userへlinkしない。

OIDC Coreは`sub`をissuer内でlocally uniqueかつnever reassignedな識別子として定義するため、emailよりlink keyに適する。

### 4.2 Provisioning and linking

- unknown external identityはdefault denyとする。
- email一致による自動link、Role付与またはaccount takeoverを許可しない。
- link作成は、事前provisioning、既存ユーザーによる再認証付きlink、または管理者操作のいずれかをCustomerが選ぶ。
- Just-In-Time user作成をFramework既定にせず、採用時はCustomer-owned policy、attribute validation、初期Permission、
  rollback、auditを明示する。
- link / unlinkは業務監査対象とし、unlink後のsession失効を必須とする。

## 5. Authentication ingress semantics

| Ingress | Spring Security responsibility | KOIKI semantics | Excluded behavior |
|---|---|---|---|
| local browser login | Form Login、AuthenticationManager / Provider、PasswordEncoder、Session fixation protection | email lookup、account / lock判定、Framework user principal、audit | password照合Controller、JWT発行login API |
| application-direct OIDC | OAuth 2.0 Login / OIDC Client、state / nonce、provider response validation | issuer + subject link、account state、Permission、session、audit | email auto-link、IdP claimの無条件authority化 |
| Bearer API | Resource Server、signature / issuer / audience / time validation | subject linkまたはclient identity、scope mapping、Permission、audit | ID Token、Cookie、query token fallback |
| Edge Authentication | cloud Adapter検証後のPre-Authentication | trust source + subject link、account state、Permission、audit | raw `x-amzn-oidc-*`等の直接信用 |

local Form Loginの`POST /login`はSpring Security filterが処理する認証入口であり、KOIKI独自REST Public APIとしない。
KOIKIはemail / passwordを受けてAccess / Refresh Tokenを返すendpointをPhase 2で提供しない。

current user情報が必要なController / APIはSpring `Authentication`へ直接依存するのではなく、Framework user ID、
authentication source、Permissionを表す最小read-only principal contract候補を利用する。ただし型とartifactはP2-A1 / B1で
inventoryを提示してからPublic API化を判断し、Framework-owned `/me` endpointを先行生成しない。

## 6. Authorization and authority mapping

1. RoleはPermissionの集合であり、実際の細粒度判定はPermission authorityを用いる。
2. URL SecurityとMethod Securityを併用し、UI非表示、SPA route guard、BFF経由を認可境界にしない。
3. resource ownershipと業務状態policyはCustomer / Reference Application Use Caseが所有する。
4. external group、role、scope、email domainをKOIKI Permissionとして無条件に受理しない。
5. Customer mappingは許可リスト方式とし、unknown claim / scopeは権限を付与しない。
6. BFF requestも通常のBearer validationとPermission判定を通す。

Session認証のauthorityは認証時snapshotとなり得る。Role / Permission変更後も古いauthorityが残らないよう、
Phase 2標準では対象ユーザーの既存sessionを失効させる案を推奨する。requestごとのDB再読込またはauthorization version方式は
性能・整合性fixtureが必要な代替案とし、承認済みdecision O-3を適用する。

Bearer Access Tokenのscope / authority鮮度はissuerのtoken lifetime、revocationおよびP2-F4のtoken lifecycle判断に依存する。
Resource Serverが発行責務を引き取らない。

## 7. External response and error semantics

### 7.1 Browser profile

- 未認証のbrowser requestは許可されたlogin入口へredirectする。
- local loginのunknown email、password不一致、disabled、lockedは外部表示を同一のlogin failureとする。
- OIDC / Edge失敗もtoken、claim、provider responseまたは内部reasonを画面・query stringへ露出しない。
- success redirect、return URL、OIDC redirect URIはallowlistまたはsame-origin完全一致で制限する。

### 7.2 API / Bearer profile

| Condition | HTTP semantics | Public detail |
|---|---|---|
| credentialなし / 無効 | `401 Unauthorized` | 一般化したauthentication required / failed。標準`WWW-Authenticate`を維持 |
| 認証済み・Permission不足 | `403 Forbidden` | access denied。必要Permission、Role構造、所有者情報を露出しない |
| CSRF失敗 | `403 Forbidden` | request rejected。token期待値やsession情報を露出しない |
| Security設定不成立 | startup failure | runtimeでpermit fallbackしない |
| 内部監査・identity障害 | `5xx`またはlogin拒否 | stack、SQL、provider payload、PIIを露出しない |

API errorはGrand Design §12.4のRFC Problem DetailsとKOIKI error codeを使用する。RFC 9457のSecurity Considerationsに従い、
公開`detail`をdebug情報として使用しない。具体的なerror code文字列はP2-A1のPublic API inventoryで固定するが、
認証失敗の外部categoryを内部監査reasonごとに細分化しない。

## 8. Logout and invalidation semantics

logoutを単一操作として曖昧にせず、次を分離する。

| Operation | Required effect | Not guaranteed |
|---|---|---|
| KOIKI local / Session logout | POST + CSRF、HTTP Session / SecurityContext / CSRF token失効、session Cookie削除、security audit | IdP global session、BFF session、既発行Bearer Tokenの失効 |
| application-direct OIDC logout | KOIKI local logoutを先に完了。provider対応時だけRP-Initiated Logoutを追加 | 全端末・全applicationのIdP global logout |
| BFF logout | Customer BFF session / token storeを失効 | KOIKI側にSessionが存在するという仮定 |
| direct Token SPA logout | browser内token破棄。必要ならissuer revoke / end-sessionへ接続 | KOIKI Resource Serverによるtoken発行・revoke |
| account disable | 全方式の新規認証拒否、全KOIKI session失効 | 外部IdP sessionまたはBearer Token即時失効 |
| local credential lock | 新規password login拒否。既存sessionと検証済みOIDC / Edge loginは維持 | account全体のdisable |
| password change / reset | local credentialを更新し、全KOIKI sessionを失効 | issuer-owned tokenの失効 |
| Role / Permission変更 | O-3で決める鮮度方式により旧authorityを無効化 | 既発行external Access Tokenのclaim変更 |
| external identity unlink | 対象link経由の新規login拒否、関連KOIKI session失効 | 外部IdP accountの無効化 |

Spring Security標準logout handlerを利用し、passwordやtokenを扱う独自logout Controllerを作らない。
監査障害があってもlogout / disable等の防御的失効を取り消さない。

## 9. CORS / CSRF semantics by profile

- Cookie Session pathはunsafe methodへCSRFを必須とする。Thymeleafはform token、HTMXはheader注入を使用する。
- Bearer専用pathはCookie authenticationを受けない場合だけCSRF対象外にできる。
- Customerがprofileごとにexact origin、method、request / exposed headerをallowlistする。
- credential付きCORSとwildcard originを併用しない。
- CORS成功を認証・認可成功とみなさず、非browser clientにも同じSecurity / Permissionを適用する。
- 同じpathでSession Cookie、Bearer、ID Token、raw edge headerをfallback認証しない。

### 9.1 Direct Token SPA risk acceptance

Profile Tを採用するCustomerは、少なくとも次をarchitecture / security reviewへ記録し、明示承認する。

1. same-origin SessionまたはBFFを採用できない理由と、扱うデータの機密性。
2. Access Tokenの保存場所、lifetime、scope / audience最小化、page reload時の再取得方式。
3. refresh tokenをbrowserへ発行する場合のrotation / sender constraint、reuse、maximum / inactivity lifetime。
4. CSP、third-party script、dependency / supply-chain、XSS検出とincident response。
5. logout、端末紛失、account disable時に残存するtokenと失効手段。
6. CORS allowlistと、client secretをfrontendへ置かないことの確認。

この記録がないProfile TをKOIKI defaultとして有効化しない。Frameworkはrisk acceptanceを自動判定せず、
Customer configurationと運用Evidenceを要求する契約だけを提供する。

## 10. Audit semantics

### 10.1 Event classification

| Event | Classification | Public response relation |
|---|---|---|
| local / OIDC / Edge login success・failure | Security audit / `REQUIRES_NEW` | failure reasonは一般化し、内部reasonだけ監査 |
| logout、session invalidation、account lock | Security audit / `REQUIRES_NEW` | logoutは監査失敗でも防御的失効を完了 |
| authorization denied | Security audit / `REQUIRES_NEW` | 必要Permissionやresource ownershipを外部へ露出しない |
| external identity link / unlink | Business audit / same transaction | audit不能ならlink変更をrollback |
| Role / Permission変更 | Business audit / same transaction | audit不能なら権限変更をrollback |
| administrator password / account操作 | Business audit / same transaction | audit不能なら管理操作をrollback |
| self-service reset request / completion | Security audit / `REQUIRES_NEW` | account存在有無を外部responseへ反映しない |

### 10.2 Actor and sensitive data

- 認証済みactorはFramework user IDで記録する。email、external subjectまたはdisplay nameをactor IDにしない。
- unknown loginではraw emailを監査・通常logへ保存せず、照合不能試行の集計key方式をP2-F3 / B2で検証する。
- token、password、authorization code、PKCE verifier、nonce、client secret、claim全文を記録しない。
- IP、User-Agent等はPII / privacy対象としてretention、masking、access controlを別途決定する。
- metric tagはevent category、result、authentication source等のlow-cardinality値に限定する。

Security audit書込み失敗時のfail-closed範囲は承認済みdecision O-4とする。login成功、reset token発行、
管理解除をfail closedとし、logout / disable / session invalidationは防御的操作を完了して運用alertを発生させる。

## 11. Ownership boundary

| Concern | Owner |
|---|---|
| SecurityFilterChainの安全な既定、Spring event接続、principal / Permission / auditの最小契約 | Framework |
| Framework user、local credential、Role / Permission、external identity linkの永続化 | Framework |
| email canonicalization標準、account / lockの共通semantics | Framework。変更可能なproperty化はしない |
| issuer、client registration、redirect URI、audience、CORS origin、claim / group mapping | Customer |
| Cognito User Pool / external IdP設定、client secret rotation | Customer / runtime environment |
| ALB署名・ARN・network trust検証 | cloud固有Adapter / Customer。Phase 4候補 |
| employee number、organization、employment status、resource ownership | Customer business module |
| identity管理画面 | Reference `identity`。Framework public contract経由 |

## 12. P2-F3 handoff / acceptance matrix

P2-F3は少なくとも次をtest topologyとnegative-path matrixへ変換する。

1. email canonical collision、unknown user、disabled、locked、password mismatchが同じ外部失敗になる。
2. issuer + subjectはlinkでき、email一致だけではlinkされない。
3. Cognito direct OIDCは標準Provider設定で扱え、Cognito固有Framework APIを要求しない。
4. raw edge header、誤署名、誤edge識別子、許可外到達経路ではPre-Authenticationが成立しない。
5. external group / scopeのunknown値がPermissionを付与しない。
6. URL / Method Securityの双方で401 / 403とdirect request拒否を確認する。
7. logout / disable / password reset / Permission変更のsession失効範囲を実証する。
8. browser / APIで秘密値、PII、内部failure reasonがresponse、log、metric、artifactへ出ない。
9. business / security auditのrollback差とaudit failure semanticsを実PostgreSQLで確認する。
10. Session、Bearer、Edge pathが相互fallbackせず、unmatched pathをdenyする。

## 13. Approved Owner decisions

2026年8月31日、Architecture Ownerは本文全体を確認し、O-1〜O-6を推奨案どおり承認した。

| ID | Choice | Approved decision | Rejected / deferred alternative |
|---|---|---|---|
| O-1 | email canonicalization | Phase 2はASCII emailをcase-insensitiveに扱い、前後空白除去 + `Locale.ROOT` lowercaseをlookup keyとする。original valueは表示用に保持。provider alias変換なし | 国際化emailを初期対応する場合、Unicode / IDNA、case folding、Oracle照合を追加設計・実測 |
| O-2 | unknown external identity | default deny。明示provisioning / linkだけ許可 | JIT作成はCustomer policy、初期Permission、重複・rollback・audit設計が必要 |
| O-3 | Permission変更後のSession | 対象userの全KOIKI sessionを失効 | requestごとDB照会は負荷、authorization versionは追加contract / migrationが必要 |
| O-4 | Security audit failure | login成功、reset token発行、管理解除はfail closed。logout / disable / invalidationは処理継続 + alert | 全best-effortは監査欠損、全fail-closedは防御的失効を妨げる |
| O-5 | OIDC logout | local logoutを必須とし、RP-Initiated Logoutはprovider対応時のopt-in | IdP logout必須化はCognitoを含むprovider差、availability、global logout影響を増やす |
| O-6 | public current-user API | Frameworkはprincipal contractだけを候補とし、endpointはCustomer / Referenceが所有 | Framework `/me`固定はDTO、PII、versioningをPublic API化する |

O-1〜O-6と本文全体の承認により、P2-F2を`COMPLETE`とする。型、artifact、table、error code文字列は
それぞれP2-A1 / B1 / B2でinventoryとfixtureを提示するまで承認対象に含めない。

## 14. Official references

- [Spring Security Authentication](https://docs.spring.io/spring-security/reference/servlet/authentication/index.html)
- [Spring Security OAuth 2.0 Login](https://docs.spring.io/spring-security/reference/servlet/oauth2/login/index.html)
- [Spring Security Logout](https://docs.spring.io/spring-security/reference/servlet/authentication/logout.html)
- [OpenID Connect Core 1.0](https://openid.net/specs/openid-connect-core-1_0.html)
- [RFC 9457: Problem Details for HTTP APIs](https://www.rfc-editor.org/rfc/rfc9457.html)
- [RFC 9700: Best Current Practice for OAuth 2.0 Security](https://www.rfc-editor.org/rfc/rfc9700.html)
- [RFC 10017: OAuth 2.0 for Browser-Based Applications](https://www.rfc-editor.org/rfc/rfc10017.html)
- [NIST SP 800-63B: Authenticator and Verifier Requirements](https://pages.nist.gov/800-63-4/sp800-63b.html)
- [AWS ALB user authentication](https://docs.aws.amazon.com/elasticloadbalancing/latest/application/listener-authenticate-users.html)
- [Amazon Cognito OIDC endpoints](https://docs.aws.amazon.com/cognito/latest/developerguide/federation-endpoints.html)
