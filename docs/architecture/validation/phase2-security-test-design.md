# Phase 2 Security replacement test design

## 1. Status and bounded scope

- **Design date:** 2026年8月31日
- **Work package:** `P2-F3`
- **Status:** `P2-F3 / GATE F COMPLETE — OWNER APPROVED`
- **This slice:** dependency / CI boundary and Gate F handoff
- **Deferred after Gate F:** P2-F4 token lifecycle decision、Gate P2-2 implementation approval
- **Subsequent status:** P2-F4は2026年8月31日にT-1〜T-6をOwner承認して`COMPLETE`。同日、6観点の最終Owner承認によりGate P2-2を通過し、P2-A1開始可能
- **Code / dependency / migration change:** 0

本記録は、P2-F2で承認したsecurity semanticsを、Gate P2-2後に再利用可能な検証Harnessへ変換するための
test topology、Spring component mappingおよびthreat / negative-path matrixを定義する。production実装へ展開せず、
コードを作り始める前に必要な検証境界を固定する。

## 2. Test ownership

| Asset | Owner | Distribution | Rule |
|---|---|---|---|
| Security acceptance Harness | Tooling | 非配布 | P2-A1から同じHarnessを拡張し、CPごとに作り直さない |
| Framework unit / integration tests | Framework | production artifactへ非同梱 | 正式実装と同じmoduleで回帰実行 |
| local OIDC issuer / JWT keys | Tooling | test scopeのみ | credential不要、固定test identity、秘密値をartifactへ残さない |
| PostgreSQL / 2-process orchestration | Tooling | 非配布 | package済み成果物を検証し、内部mockだけで完了しない |
| Customer claim / route samples | Fixture | 非配布 | Framework defaultやPublic APIへ昇格しない |
| Reference `identity` tests | Reference | Reference成果物 | Framework public contract経由。Framework内部を参照しない |

安定したFixtureも自動的にFramework production APIへ昇格させない。ただし、Acceptance Harnessとtest utilityは
Repository内に保持し、後続CPと各Gateの回帰検証へ継続利用する。

## 3. Test layers

| Layer | Purpose | Runtime / dependency | First consumer |
|---:|---|---|---|
| T0 | dependency tree、Auto Configuration条件、default deny、起動失敗 | ApplicationContext、DBなし | P2-A1 |
| T1 | FilterChain、CSRF / Header、401 / 403、matcher非fallback | Servlet test client、stub identity | P2-A1 |
| T2 | local login、Role / Permission、Method Security、generic failure | Servlet test client、Framework contract fake | P2-A2 |
| T3 | OIDC Login、Bearer JWT、Cognito-compatible metadata、Edge拒否 | ephemeral local issuer / JWKS、test keys | P2-A3 |
| T4 | identity、audit、lock、migration、transaction rollback | PostgreSQL Testcontainers | P2-B1 / B2 |
| T5 | Spring Session JDBC、logout、権限変更後失効 | PostgreSQL Testcontainers | P2-B3 |
| T6 | 2 instance、片系停止、cleanup単一実行 | package済みapp 2 process + PostgreSQL | P2-B3 |

任意hosted Cognitoおよび実ALBはrequired PR testへ混ぜない。CognitoはT3で標準OIDC契約を再現し、
hosted acceptanceは別承認、実ALB AdapterはPhase 4候補とする。Oracleは採用確度の低い将来optional patternであり、
optional `P4-ORACLE` Gate前はfixture、dependencyまたはCI対象にしない。

### 3.1 Spring component mapping policy

- Spring componentはFramework内部配線として使用し、Filter、Provider、SecurityContext等をKOIKI Public APIへ露出しない。
- Customerへ公開する候補は、principal、Permission、identity link、audit等のSpring非依存な最小契約に限定する。
- component classの存在確認だけをtestせず、request、Authentication、Session、audit等の外部観測結果で検証する。
- Boot Auto Configurationを置換する場合は、置換理由とback-off条件をT0で検証する。
- Spring標準componentで成立する限り、KOIKI独自Filter、JWT parser、CSRF、Password hashを作らない。

### 3.2 Cross-cutting web security mapping

| Concern | Spring component / configuration seam | KOIKI responsibility | Test layer / observable evidence |
|---|---|---|---|
| request chain selection | `SecurityFilterChain`、`HttpSecurity`、`RequestMatcher` | profile別chain構成、重複 / 空白matcher拒否、unmatched deny | T0 / T1。起動成否、対象pathのstatus、非fallback |
| URL authorization | `authorizeHttpRequests`、`AuthorizationManager`、`AuthorizationFilter` | safe default、Customer rule接続、Permission semantics | T1 / T2。anonymous 401、権限不足403、許可時だけ到達 |
| exception translation | `ExceptionTranslationFilter`、`AuthenticationEntryPoint`、`AccessDeniedHandler` | browser redirectとAPI Problem Detailsの分離、内部reason非露出 | T1〜T3。redirect、401 / 403、`WWW-Authenticate`、safe body |
| CSRF | `CsrfFilter`、`CsrfTokenRepository`、logout support | Cookie path必須、Bearer専用pathだけ除外、無効化は明示 | T1 / T5。missing / invalid / valid token、POST logout |
| security headers | `HeaderWriterFilter`と`headers` DSL | secure defaults、Customer overrideの制約 | T1。response header positive / explicit override |
| CORS | Spring MVC CORS + `CorsFilter`のSecurity前処理 | exact allowlist property、credential / wildcard拒否 | T1 / T3。preflightとactual requestのallow / deny |
| SecurityContext | `SecurityContextHolder`、`SecurityContextRepository` | Framework user principalへの変換、request後cleanup | T1〜T6。principal、authority、thread / request間非漏洩 |
| Method Security | `@EnableMethodSecurity`、method `AuthorizationManager` | Permission annotation / expression方針、Use Case境界 | T2〜T4。Web迂回direct invocationも拒否 |

`HttpSecurity`、Filter順序、`AuthorizationManager`実装をCustomer向け安定APIとしない。Framework starterは安全な既定と
限定されたproperty / callback候補だけを提供し、具体的な公開型はP2-A1のPublic API inventoryで判断する。

### 3.3 Local login mapping

| Flow step | First Spring candidate | KOIKI seam | Test evidence |
|---|---|---|---|
| form credential受付 | Form Login / `UsernamePasswordAuthenticationFilter` | parameter名、login page / success URLはapplication設定。password Controllerを作らない | T2。POST login、CSRF、generic failure |
| authentication orchestration | `AuthenticationManager` / `ProviderManager` | Provider順序とfallback禁止 | T2。対応外token、disabled、locked、bad password |
| user / password verification | `DaoAuthenticationProvider`、`UserDetailsService`、`PasswordEncoder` | Framework identity contractへのinternal adapter、email canonicalization、account state | T2 / T4。O-1、local-only lock、raw password非露出 |
| success / failure | Spring success / failure handler、`AuthenticationEventPublisher` | redirect / error semantics、login attempt、Security audit接続 | T2 / T4。外部generic responseと内部reason差、audit row |
| session fixation | Spring Session Management / `SessionAuthenticationStrategy` | secure defaultを維持 | T2 / T5。login前後session ID差、principal維持 |

`DaoAuthenticationProvider`でO-1〜O-4を満たせるかを最初に検証する。KOIKI独自`AuthenticationProvider`は、
複数instanceのlock / audit transaction等を標準hookとinternal adapterで表せないEvidenceが得られた場合だけ再検討する。

### 3.4 OIDC Login mapping

| Flow step | Spring component / seam | KOIKI seam | Test evidence |
|---|---|---|---|
| provider registration | `ClientRegistrationRepository` | issuer、client、redirect URIはCustomer設定。Cognito専用APIなし | T0 / T3。missing / unknown registration、metadata差 |
| authorization request | OAuth 2.0 Login authorization request resolver / repository | allowed redirectとprofile境界 | T3。state / nonce、許可外redirect、Session保存 |
| callback authentication | OAuth 2.0 Login authentication filter / provider | local chainとの共存、generic failure | T3。success、state / nonce不正、provider error |
| OIDC principal取得 | `OidcUserService`等のOAuth2 User Service seam | issuer + subject link、unknown link deny、email auto-link禁止 | T3 / T4。O-2、Cognito-compatible claims、PII非露出 |
| authority mapping | `GrantedAuthoritiesMapper`またはUser Service変換 | external claim allowlistからKOIKI Permissionへ変換 | T3。unknown groupで権限なし、Method Security適用 |
| logout連携 | Spring logout + provider対応のOIDC logout success handler | local logout必須、RP-Initiated Logout opt-in | T3 / T5。O-5、local session失効を先に確認 |

具体Filter classやcallback pathをKOIKI Public APIへ固定しない。Cognito適合は標準discovery / issuer / subject契約で確認し、
hosted Cognitoをrequired CIへ入れない。

### 3.5 Bearer Resource Server mapping

| Flow step | Spring component / seam | KOIKI seam | Test evidence |
|---|---|---|---|
| token extraction | `BearerTokenAuthenticationFilter`、Bearer token resolver | Authorization headerだけを許容。Cookie / query / form fallback禁止 | T3。missing、duplicate、query、Cookie、ID Token拒否 |
| JWT validation | `JwtDecoder`、`OAuth2TokenValidator` | allowed issuer / audience / algorithm / Clock設定 | T3。signature、`iss`、`aud`、`exp`、`nbf`境界 |
| authentication | `JwtAuthenticationProvider` | provider / chain分離、invalid時のsafe 401 | T3。SecurityContext成立 / cleanup、`WWW-Authenticate` |
| authority conversion | `JwtAuthenticationConverter` | scope / claim allowlistからPermissionへ変換 | T3。unknown / insufficient scopeで403 |
| multi-issuer | `AuthenticationManagerResolver`候補 | 明示allowlistされたissuerだけ。必要性未実証なら導入しない | T0 / T3。unknown issuer fail closed |

Authorization Server、`JwtEncoder`、Access / Refresh Token generatorはResource Server mappingへ含めず、P2-F4で扱う。

### 3.6 Edge Authentication mapping

| Boundary | Spring candidate | Phase 2 responsibility | Evidence |
|---|---|---|---|
| verified principal受入 | Spring Pre-Authentication supportのProvider / token | trust source + subjectからFramework userへlinkするcontract | T3ではvalidated fixture principalだけ成功 |
| AWS header validation | cloud Adapter（Spring外を含む） | production実装はPhase 4候補 | T1 / T3でraw `x-amzn-oidc-*`が認証されない |
| authorization | 通常のURL / Method `AuthorizationManager` | Edge経路もPermissionをbypassしない | T3。権限不足403、direct invocation拒否 |

Phase 2でAWS固有Filterを実装しない。Spring Pre-Authentication componentは将来Adapterの接続候補であり、
T3では「raw headerでは認証されない」「検証済みprincipalも通常認可を通る」というcontractだけを検証する。

### 3.7 Audit / Session mapping

| Concern | Spring / platform component | KOIKI seam | Test layer / evidence |
|---|---|---|---|
| authentication events | `AuthenticationEventPublisher`、Spring application event | Security audit reason mapping、login attempt更新 | T2 / T4。success / failure / lockとtransaction |
| logout event | Spring logout handlers、`LogoutSuccessEvent` | 防御的失効を先に完了し、audit failureはalert | T5。O-4 / O-5、CSRF、Cookie削除 |
| JDBC session | `SessionRepositoryFilter`、`JdbcIndexedSessionRepository` | Framework Flyway、initializer無効化、save / flush mode判断 | T5 / T6。DB row、再起動 / 片系停止 |
| user session lookup | `FindByIndexNameSessionRepository`、Spring Session Security integration | Framework user IDで対象session失効 | T5。O-3、disable / reset / unlink |
| concurrent session support | Spring Security Session Registry + Spring Session bridge候補 | 同時login拡張点。Phase 2既定制限は未決 | T5 / T6。採用時だけ複数instance実測 |
| expiry cleanup | `JdbcIndexedSessionRepository` cleanup seam + Phase 1b single execution | Web process内cleanupを無効化しnon-web単一実行 | T6。二重実行なし、期限切れ削除 |

Spring SessionのfilterがSpring Securityより前にHttpSessionを置換すること、principal indexで複数sessionを検索できることを
T5 / T6の外部状態で確認する。Filter order自体をKOIKI Public APIにしない。

### 3.8 Component binding decisions

| Decision | Result |
|---|---|
| custom password Providerを既定化するか | **No**。まず`DaoAuthenticationProvider` + internal identity adapterを検証 |
| custom JWT parser / Filterを作るか | **No**。Resource Server標準componentを使用 |
| Cognito専用Clientを作るか | **No**。標準OIDC Providerとして扱う |
| ALB header FilterをPhase 2で作るか | **No**。raw header拒否contractのみ。production AdapterはPhase 4候補 |
| Spring classをKOIKI Public APIへ公開するか | **No**。実装詳細として閉じる |
| testが内部componentを直接assertするか | 起動条件以外は**No**。外部観測結果を主証拠とする |

## 4. Reuse path from P2-A1 to P2-B3

```text
P2-A1  T0 + T1: 共通起動・request・deny Harness
   ↓ retain and extend
P2-A2  + T2: local identity / login / Permission scenarios
   ↓ retain and extend
P2-A3  + T3: OIDC / JWT / SPA-BFF-Edge boundary
   ↓ same scenarios against real persistence
P2-B1  + T4: audit transaction and failure semantics
P2-B2  + T4: identity / lock / reset / migration
   ↓ same authenticated paths against shared session
P2-B3  + T5 + T6: JDBC Session / logout / 2 instance / cleanup
```

各CPは既存Harnessへscenarioと実行層を追加する。以前のpositive / negative testを削除して置き換えず、
より実環境に近い層でも同じsecurity invariantが成立することを確認する。

## 5. Conceptual Fixture capabilities

次はP2-A1で必要になる能力名であり、Java class、package、moduleまたはPublic API名ではない。

| Capability | Reused by |
|---|---|
| application context matrix | A1〜A3。profile有効 / 無効、重複matcher、missing property、Customer override |
| protected request client | A1〜B3。anonymous、Session、Bearer、CSRF、direct request |
| fixed test identities | A2〜B3。active、disabled、local-locked、SSO-only、Role / Permission差 |
| ephemeral issuer / JWKS | A3。OIDC ClientとResource Serverをclient / audience別に検証 |
| safe evidence capture | A1〜B3。response、log、metric、artifactへのsecret / PII非露出 |
| PostgreSQL reset / inspection | B1〜B3。migration、audit row、lock、session、rollback状態 |
| packaged process controller | B3。port分離、2 instance、片系停止、cleanup確認 |

重複が実証される前に共通test libraryや新Maven moduleを作らない。最初はP2-A1の非配布Fixture内に置き、
A2 / A3で本当に共有される能力だけをinternal Testing Support候補としてreviewする。

## 6. P2-F2 acceptance allocation

| P2-F2 invariant | Primary layer |
|---|---:|
| email collisionとgeneric login failure | T2、T4 |
| issuer + subject link、email auto-link禁止 | T3、T4 |
| Cognitoを標準OIDC Providerとして扱う | T3 |
| raw edge header / wrong signer拒否 | T1、T3 |
| unknown group / scopeでPermission非付与 | T2、T3 |
| URL / Method双方の401 / 403 | T1、T2 |
| logout / disable / reset / Permission変更のsession失効 | T4〜T6 |
| secret / PII / internal reason非露出 | T1〜T6 |
| business / security audit rollback差 | T4 |
| Session / Bearer / Edge非fallback、unmatched deny | T1、T3 |

## 7. Threat / negative-path matrix

### 7.1 Boundary and evidence rule

本matrixは、外部の未認証攻撃者、権限不足の認証済みuser、改変されたbrowser / API client、Customer設定誤り、
IdP / DB / networkの部分障害を対象とする。保護対象はcredential、token、session、identity link、Permission、
Security / Business auditおよびそれらの非露出性である。

- 各行はpositive、negative、failure-pathのすべてを持ち、Owning CPで外部観測可能なEvidenceを残す。
- `deny`は認証・権限・永続状態を成立させないことを指す。単に例外が発生したことを成功条件にしない。
- generic responseでも内部監査reasonは区別する。ただしpassword、token、raw unknown email等を記録しない。
- timingの完全一致は契約化しないが、user存在有無を意図的に露出する分岐、response差、metric tag差を許可しない。
- Phase 2でproduction実装しないcloud Adapter等はcontract fixtureまでをrequired Evidenceとし、実環境適合を偽装しない。

### 7.2 Acceptance matrix

| ID / threat | Positive path | Negative / attack path | Failure injection | Required observable evidence | Layer / owning CP |
|---|---|---|---|---|---|
| N-01 identity confusion / account enumeration | uniqueなASCII emailをtrim + `Locale.ROOT` lowercaseで同一userへ解決し、正しいpasswordでSessionを確立 | canonical collision、unknown user、disabled、local-locked、bad passwordを同じ外部失敗へ一般化。provider alias変換やraw email記録なし | login成功時のSecurity audit書込みを失敗させ、O-4どおりSession未確立 | response status / redirect / body / Cookieにuser状態差なし。内部reason、canonical unique constraint、audit rollbackを確認 | T2 / T4、A2 / B1〜B2、O-1 / O-4 |
| N-02 external account takeover / unintended link | allowlisted issuer + subjectと明示linkされたactive userだけを認証 | email一致だけ、unknown issuer / subject、disabled user、同一subjectの競合linkをdeny。email auto-link / JIT作成なし | link transactionまたはBusiness auditを失敗させ、linkとauditを共にrollback | Framework user IDへ一意に解決し、拒否時にuser / link rowが増えない。外部responseはsafe failure | T3 / T4、A3 / B1〜B2、O-2 / O-4 |
| N-03 token substitution / provider coupling | Cognito-compatibleな標準discovery、issuer、client、state / nonceでOIDC Login成立 | wrong issuer / client / redirect、state / nonce不正、ID TokenをBearer APIへ提示、Cognito固有claimだけによる認証をdeny | metadata / JWKS unavailable、unknown `kid`、key rotation境界でfail closed。secretをEvidenceへ残さない | Cognito専用Framework APIなしで成立し、不正時はSession / SecurityContext未確立。safe errorと監査categoryを確認 | T0 / T3、A3 |
| N-04 forged edge identity / trust-boundary bypass | trust検証済みfixture principalだけが通常のidentity linkと認可を通過 | raw `x-amzn-oidc-*`、wrong signer / edge ID、許可外network path相当fixture、email-only identityをdeny | verifier unavailable / key取得失敗相当をdenyし、header fallbackしない | raw headerでprincipalが成立せず、検証済みprincipalもPermission不足なら403。AWS production適合とは記録しない | T1 / T3、A1 / A3。cloud実環境はPhase 4 |
| N-05 privilege escalation by claim / scope | allowlist済みgroup / scopeだけをKOIKI Permissionへ変換 | unknown、malformed、case違い、過剰scope、別profileのauthorityを無視し、deny by default | 必須mapping設定が空、重複または曖昧なら起動失敗。実行時のunknown値にはPermissionを付与しない | AuthenticationのauthorityとURL / Method双方の結果を確認。unknown値をlog / metricの高cardinality tagにしない | T0 / T2 / T3、A1〜A3 |
| N-06 authorization bypass | 必要Permissionを持つuserだけがURL経由とUse Case direct invocationの双方で成功 | anonymousは401、認証済み権限不足は403、UI / Controller迂回のdirect invocationもdeny | chain / matcher欠落、重複、順序誤りを起動失敗またはunmatched denyへ倒す | protected処理と永続変更が拒否時に未実行。responseへ必要Permissionやresource ownershipを露出しない | T0〜T2 / T4、A1〜A2 / B1 |
| N-07 stale-session authorization | logout、disable、password reset、Permission変更後に対象userの全KOIKI Sessionを失効 | 旧Cookie、別instance上のSession、固定化前Sessionで再利用不可。対象外userのSessionは維持 | Security audit失敗時もlogout / disable / invalidationを完了してalert。Session store障害時は成功を偽装せずR-01へ接続 | Session row / Cookie / SecurityContext、2 instanceでの再request、対象範囲、alertを確認 | T4〜T6、B1〜B3、O-3〜O-5 |
| N-08 secret / PII disclosure | browser / APIの成功・失敗Evidenceが最小principal IDと低cardinality categoryだけを含む | password、token、code、PKCE verifier、nonce、client secret、claim全文、raw unknown emailを投入して非露出を確認 | provider / DB例外、debug stack、test失敗、artifact生成時にもredactionを維持 | response、Cookie、header、application / audit log、metric、test report、build artifactを走査 | T1〜T6、A1〜B3、O-4 |
| N-09 audit tampering / inconsistent transaction | Security auditは分離transaction、Business auditは対象変更と同一transactionで保存 | actor偽装、外部subject / emailをactor IDに使用、authorization deny reasonの外部露出を禁止 | Security audit失敗ではlogin成功等をfail closed、logout等は処理継続 + alert。Business audit失敗では対象変更をrollback | 実PostgreSQLでtransaction境界、row有無、対象状態、外部response、alertを同時確認 | T4、B1〜B2、O-4 |
| N-10 cross-profile credential fallback | Session、Bearer、検証済みEdge principalが各profileの明示pathだけで成立 | CookieをBearer pathへ、Bearerをbrowser loginへ、raw Edge headerを任意pathへ提示してdeny。unmatched pathもdeny | profile matcher重複 / 空白、必要property欠落、issuer ambiguityをT0で検出 | 想定外credentialから別Providerへfallbackせず、SecurityContext未確立。401 / 403 / redirectはprofile契約どおり | T0 / T1 / T3、A1 / A3 |

### 7.3 Residual risks and Gate treatment

| ID | Residual / unresolved point | Gate treatment |
|---|---|---|
| R-01 | Session store障害中のdisable、Permission変更、password resetとSession失効のatomicity / compensation | Gate F F-4で承認済み。永続失効不能時のmutation rollback / safe failureを適用し、失効未完了を成功扱いしない |
| R-02 | ALB署名、edge ARN / network trustおよびkey rotationのAWS実環境適合 | Phase 4 cloud Adapter候補。Phase 2ではcontract fixtureを越えた適合をclaimしない |
| R-03 | hosted Cognito availability、tenant設定差、RP-Initiated Logout差 | optional hosted acceptance。required PR testはlocal standard OIDC contractで再現 |
| R-04 | Access / Refresh Token発行、rotation、reuse、revoke | P2-F4でphaseとdependencyを決定。P2-F3 Resource Server testへ混入させない |
| R-05 | IP / User-Agent等のretention、masking、access control | Customer / Production Baseline判断。Phase 2の既定監査項目へ無断追加しない |

N-01〜N-10のいずれかでpositive、negative、failure-pathのEvidenceが欠ける場合、対応するCPは完了としない。
R-01は後続実装の挙動を変えるため、単なるtest詳細ではなくGate F decisionとして提示し、F-4で承認された。

## 8. Stop conditions for implementation handoff

- T0 / T1より前にproduction `koiki-starter-security`の契約を固定する。
- test専用issuer、key、userまたはrouteをproduction resourceへ入れる。
- Python endpoint互換を理由にlogin / token Public APIを作る。
- hosted IdP credentialをrequired CIの前提にする。
- Fixture codeをReference ApplicationまたはCustomer sampleとして扱う。
- 内部mockの成功だけでPostgreSQL、package済みprocess、2 instanceのEvidenceを代替する。
- stableに見えるという理由だけでtest utilityをFramework Public APIへ昇格する。

## 9. Dependency candidates and BOM boundary

### 9.1 Binding rules

- version authorityは既存`koiki-dependencies-bom`がimportするSpring Boot 4.1.1 BOMだけとする。Spring Security / Sessionの
  個別version、独立BOMまたはtest fixture固有version propertyを追加しない。
- Boot 4の現行名である`spring-boot-starter-security-oauth2-*`を使い、deprecatedな
  `spring-boot-starter-oauth2-*`を新規採用しない。
- production候補はOwning CPで必要なものだけ追加し、test候補は非配布verification fixtureで`test` scopeとする。
- Security test依存を既存の配布artifact `koiki-testing`へP2-A1から一括追加しない。A1〜B3で再利用が実証され、
  Customer向けTesting Supportとして公開する価値が確認された場合だけ別途Public API / dependency reviewを行う。
- Boot / Spring Security標準test supportで成立する限り、WireMock、独自JWT library、Authorization Server、
  hosted IdP SDKを追加しない。

### 9.2 Candidate allocation

| Purpose | Preferred candidate | Placement / scope | First CP | Decision |
|---|---|---|---:|---|
| Servlet Security runtime under test | `spring-boot-starter-security` | `koiki-starter-security` production候補 | P2-A1 | Gate P2-2後、A1で追加 |
| common Boot test | `spring-boot-starter-test` | Security verification fixture / `test` | P2-A1 | 採用候補 |
| Security / MockMvc / Method test | `spring-boot-starter-security-test` | Security verification fixture / `test` | P2-A1 | 採用候補。`spring-security-test`直接指定を重複させない |
| OIDC Login runtime under test | `spring-boot-starter-security-oauth2-client` | Security starterまたはprofile内部構成候補 | P2-A3 | A3で必要性とtransitive treeを確認 |
| OIDC Login test support | `spring-boot-starter-security-oauth2-client-test` | Security verification fixture / `test` | P2-A3 | 採用候補 |
| Bearer JWT runtime under test | `spring-boot-starter-security-oauth2-resource-server` | Security starterまたはprofile内部構成候補 | P2-A3 | A3で追加 |
| Bearer JWT test support | `spring-boot-starter-security-oauth2-resource-server-test` | Security verification fixture / `test` | P2-A3 | 採用候補 |
| PostgreSQL integration | 既存`koiki-testing`の`spring-boot-testcontainers`、`testcontainers-junit-jupiter`、`testcontainers-postgresql` | 非配布verificationから既存Testing Supportを利用 | P2-B1 | 再利用。Security依存は追加しない |
| JDBC Session runtime under test | `spring-boot-starter-session-jdbc` | Security / Session production構成候補 | P2-B3 | B3で追加 |
| JDBC Session test support | `spring-boot-starter-session-jdbc-test` | Security verification fixture / `test` | P2-B3 | 採用候補 |
| local issuer / JWKS | JDK HTTP fixture + test-only key、上記OAuth2 / JOSE classpath | Tooling test sourceのみ | P2-A3 | 追加OSSなしから開始 |

`spring-boot-starter-security-oauth2-authorization-server`および同test StarterはP2-F4のdecision対象であり、P2-F3 / A3の
ephemeral issuerを理由に追加しない。test-only signingはAuthorization Server production capabilityを意味しない。

### 9.3 BOM verification evidence

2026年8月31日に次を確認した。

1. `koiki-dependencies-bom`はSpring Boot 4.1.1 BOMをimportし、上記Boot Starter、
   `spring-security-test` 7.1.1、Spring Session 4.1.1、Testcontainers 2.0.5を管理する。
2. `koiki-testing`のoffline `dependency:tree`はversion指定なしでBoot 4.1.1とTestcontainers 2.0.5を解決した。
3. 同moduleのEffective POMで、Security / OAuth2 Client / Resource Server / Session JDBCのproduction / test Starterと
   `spring-security-test`がBoot BOM管理下にあることを確認した。

実装CPでは追加直後にfocused `dependency:tree`を保存し、version override、Authorization Server、SAML、Redis、
想定外reactive stackおよび重複test libraryが入っていないことをT0 Evidenceとする。

## 10. Execution and CI boundary

| Lane | Layers / scope | Trigger | Environment / authority | Gate treatment |
|---|---|---|---|---|
| focused local | T0〜T3、変更module + verification fixture | 各commit前 | Java 21、credential / Docker不要、Repository Wrapper | Owning CP必須。最短のdiagnostic経路 |
| PostgreSQL local integration | T4〜T6の該当scenario | B1〜B3変更時 | Java 21、local Docker、PostgreSQL Testcontainers | Owning CP必須。container cleanup確認 |
| root Verify | reactor unit / integration、T0〜T3回帰 | 全PR / `main` | 既存`Verify (ubuntu-24.04)`、`contents: read`、secretなし | 既存required pathへ累積。ただし長時間process testを混在させない |
| Security Foundation Integration候補 | T4〜T6、migration、audit、JDBC Session、package済み2 process | Security実装を含むPR / `main` | Linux、Java 21、PostgreSQL、`contents: read`、secretなし | 3回連続成功、時間、cleanupをOwner review後にrequired化 |
| Java runtime compatibility | package済み同一artifactのJava 21 / 25実行 | Gate A以降のPR / nightly | 既存workflow原則を再利用し、runtime別rebuildなし | production Security artifact追加後に対象拡張をreview |
| hosted Cognito acceptance | discovery、login、claim、optional RP-Initiated Logout | `workflow_dispatch`または承認済み検証 | Environment approval、最小secret、固定redirect URI、artifact非保存 | optional。required PR / releaseの代替にしない |
| Oracle compatibility | 現行Phaseでは実施しない | optional `P4-ORACLE`承認後に再設計 | 現時点では環境・依存未選定 | Phase 2の完了条件・CIへ入れない |
| ALB / AWS edge acceptance | signer、edge ID、network trust | Phase 4 | cloud Adapter環境 | Phase 2の完了条件にしない |

workflow、required check、Environmentまたはsecretは本記録では変更しない。実装時は既存CIへ無条件にjobを増やさず、
まずlocal aggregate scriptでpositive / negative / restoreとcleanupを安定化し、その同じ入口を承認後のworkflowから呼ぶ。
失敗時artifactはstatus、scenario ID、sanitized logだけに限定し、token、Cookie、key、PII、provider payloadをuploadしない。

## 11. Gate F handoff

### 11.1 P2-F3 completion evidence

| Exit item | Evidence | Status |
|---|---|---|
| test topology / fixture ownership | §2〜§6、T0〜T6、A1〜B3 reuse path | COMPLETE |
| Spring component mapping | §3.1〜§3.8 | COMPLETE |
| threat / negative / failure paths | §7、N-01〜N-10 | COMPLETE |
| stop conditions / residual risks | §7.3、§8、R-01〜R-05 | COMPLETE |
| dependency / BOM boundary | §9、offline tree + Effective POM | COMPLETE |
| local / CI / hosted execution boundary | §10 | COMPLETE |
| production code / dependency / workflow change | 0 | COMPLETE |

以上によりP2-F3の設計作業を`COMPLETE`とする。P2-F3完了だけではGate承認を意味せず、Gate F承認は§11.3に記録する。

### 11.2 Owner decisions requested at Gate F

1. §9のBoot-managed dependency候補、非配布fixture配置、追加OSSなしから開始する方針を承認する。
2. §10のrequired PR、PostgreSQL integration、optional hosted Cognito、Phase 4 Edgeの実行境界を承認する。
3. required OIDC Evidenceはcredential不要のlocal ephemeral issuerとし、hosted Cognitoは任意acceptanceとする。
4. R-01は次を推奨する。
   - disable、password reset、Permission削減等の全Session失効を伴う状態変更は、永続的失効を完了できなければ
     mutationをrollbackしてsafe failure + alertとする。
   - logoutはSession store障害時もlocal SecurityContextとclient Cookieを消去するが、永続Session削除を成功扱いせず、
     safe failure + alertとして再試行可能にする。
   - audit storeだけの障害にはO-4を適用し、Session store障害と混同しない。
5. R-02〜R-05を明示deferred backlogとして保持し、Gate Fで「Phase 2実装済み」と扱わない。

Gate F Owner承認後も、P2-F4 token lifecycle decisionと§2.2の残るGate choicesを完了しなければGate P2-2を通過しない。

### 11.3 Gate F Owner approval record

2026年8月31日、Architecture Ownerは§11.2の5項目を確認し、推奨案どおり承認した。

| ID | Approved decision | Status |
|---|---|---|
| F-1 | Boot-managed dependency候補、非配布fixture配置、追加OSSなしから開始する | APPROVED |
| F-2 | required PR、PostgreSQL integration、optional hosted Cognito、Phase 4 Edgeの実行境界 | APPROVED |
| F-3 | required OIDC Evidenceはlocal ephemeral issuer、hosted Cognitoは任意acceptance | APPROVED |
| F-4 | R-01のmutation rollback / safe failure、logout時のlocal消去 + 永続失敗通知、audit障害との分離 | APPROVED |
| F-5 | R-02〜R-05を明示deferred backlogとして保持する | APPROVED |

このGate F承認時点ではP2-F4、Gate P2-2、production実装は未承認であった。P2-F4の後続承認は§1に記録する。

## 12. Official references

- [Spring Security Servlet Architecture](https://docs.spring.io/spring-security/reference/servlet/architecture.html)
- [Spring Security Username / Password Authentication](https://docs.spring.io/spring-security/reference/servlet/authentication/passwords/index.html)
- [Spring Security OAuth 2.0 Login](https://docs.spring.io/spring-security/reference/servlet/oauth2/login/index.html)
- [Spring Security OAuth 2.0 Resource Server JWT](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html)
- [Spring Security Pre-Authentication](https://docs.spring.io/spring-security/reference/servlet/authentication/preauth.html)
- [Spring Security Testing](https://docs.spring.io/spring-security/reference/servlet/test/)
- [Spring Boot Build Systems / Starters](https://docs.spring.io/spring-boot/reference/using/build-systems.html)
- [Spring Boot Managed Dependency Coordinates](https://docs.spring.io/spring-boot/appendix/dependency-versions/coordinates.html)
- [Spring Session JDBC API](https://docs.spring.io/spring-session/reference/api/java/org/springframework/session/jdbc/package-summary.html)
- [Spring Session / Spring Security integration](https://docs.spring.io/spring-session/reference/guides/java-security.html)
