# Phase 2 Security Foundation start preflight / inventory

## 1. Status

- **Inventory date:** 2026年8月31日
- **Architecture Owner:** Shuichi Kataoka
- **Branch / HEAD:** `feature/phase2-security-foundation` / `8234c31fc3c63997aae2ab19636609b661574627`
- **Baseline main / merge base:** `b2e2123605e4d971c3ed5ccc729f668d91189d83`
- **Gate:** `P2-1 INVENTORY COMPLETE / PYFW FITTING INVENTORY COMPLETE / P2-2 OWNER REVIEW REQUIRED`
- **Production implementation:** 未開始
- **OpenSpec:** Repositoryに存在しない

本記録はGate P2-1のread-only inventoryである。production code、Public API、production migration、
workflow、remote environment、secret、snapshotおよびSkill正本は変更していない。

KOIKI-PYFW `dev/v0.8` / `cdd96d8`の認証・token・browser protection実装を追加inventoryし、
`phase2-koiki-pyfw-security-fitting.md`へsource-to-target mappingとfitting Gateを記録した。
これはPython実装の直接移植またはAuthorization ServerのPhase 2実装承認を意味しない。

React SPA / SSOについては`phase2-spa-sso-security-fitting.md`を正本とし、same-origin Session、
Next.js BFF、direct Token SPAを別profileとしてPhase 2のSecurity境界へ取り込む。

## 2. Source and environment identity

| 項目 | 結果 |
|---|---|
| Branch | `feature/phase2-security-foundation` |
| Worktree | inventory開始時clean |
| Java | Eclipse Temurin 21.0.12.1 |
| Maven | Repository Wrapper 3.9.16 |
| Docker CLI | 29.5.3-rd |
| Docker daemon | **停止中**。named pipeへ接続できない |
| Root `verify` | PASS、10 reactor projects、70 tests、8.364秒 |
| Consumer dependency tree | PASS。Security、Session、OAuth2、Oracleは0件 |

Dockerを必要とするPostgreSQL、2 instance、Oracleの実測は未実施である。daemon起動後も、Gate P2-2承認前は
production実装を行わず、承認されたfixtureの検証時にだけ使用する。

## 3. Repository / release unit inventory

### 3.1 Confirmed

root aggregatorを除く配布単位は次の9成果物である。

1. `koiki-dependencies-bom`
2. `koiki-parent`
3. `koiki-architecture-contract`
4. `koiki-archunit-rules`
5. `koiki-starter-api`
6. `koiki-starter-data`
7. `koiki-starter-data-jpa`
8. `koiki-starter-observability`
9. `koiki-testing`

4 runtime StarterはPublic Java typeを持たず、合計12 configuration propertiesだけを公開契約とする。
Framework production SQLは0件である。`build-support/runtime-foundation-consumer`はTooling-owned、非配布、
正式Referenceではない。

### 3.2 Gap found

Phase 1bの単一実行EvidenceはConsumer内の`WorkItemExecutionLock`とPostgreSQL advisory lock実装である。
Framework成果物のPublic API、auto-configurationまたはproduction jobとしては配布されていない。
Phase 2の期限切れsession cleanupからConsumer codeを参照したり、直接昇格したりしてはならない。

### 3.3 Recommended release-unit evolution

| Candidate | Timing | Ownership / responsibility |
|---|---|---|
| `koiki-starter-security` | Milestone Aで必要性をfixture実証後 | Spring Security標準依存、profile別FilterChainの安全な既定、Method Security、header / CSRF既定。業務policyを含めない |
| Security / identity contract artifact | Milestone B CP開始時に名称と型を再review | local identity、audit、外部IdP link、single executionのうちSpring標準だけでは表せない安定契約。`-api` / `-impl`を先行生成しない |
| `koiki-reference-app` / `identity` | Milestone B | Reference-owned Tier 1 JPA application / module。Framework tableとmigrationを所有しない |
| Phase 2 verification fixture | P2-A1から | Tooling-owned、非配布。細粒度negative path、PostgreSQL、2 instance、OIDC / JWT test issuer、Oracle smoke |

契約artifactを1つにまとめるかidentity / audit / executionへ分けるかは、最初の公開型inventoryを提示してから決める。
空moduleは作らない。

## 4. Dependency baseline

### 4.1 Confirmed official baseline

2026年8月31日時点のSpring公式stableはSpring Boot 4.1.1で、Repository BOMと一致する。Boot 4.1.1の
dependency managementは次を管理するため、KOIKI独自version overrideは不要である。

| Dependency family | Boot-managed version |
|---|---:|
| Spring Security | 7.1.1 |
| Spring Session | 4.1.1 |
| Testcontainers | 2.0.5 |
| Flyway Oracle module | 12.4.0 |
| Oracle JDBC `ojdbc11` / `ojdbc17` | 23.26.3.0.0 |

候補のBoot 4 starterは`spring-boot-starter-security`、
`spring-boot-starter-security-oauth2-client`、
`spring-boot-starter-security-oauth2-resource-server`、
`spring-boot-starter-session-jdbc`である。Authorization Server、SAML、Redis Session、WebFluxは除外する。
Oracle smokeはtest scopeの`testcontainers-oracle-free`、`ojdbc11`、`flyway-database-oracle`を候補とする。

Spring Security / Spring SessionはApache License 2.0、TestcontainersはMITである。Oracle JDBCとDB imageは
採用前に配布・CI利用条件をOwner reviewし、license結果を第三者一覧へ記録する。

公式確認先:

- <https://docs.spring.io/spring-boot/appendix/dependency-versions/>
- <https://docs.spring.io/spring-security/reference/whats-new.html>
- <https://docs.spring.io/spring-session/reference/api.html>
- <https://java.testcontainers.org/modules/databases/oraclefree/>

### 4.2 MFA recommendation

Spring Security 7.1は標準MFA機構を持つが、factor登録、recovery、factor policy、管理画面、監査および運用手順は
Phase 2 DoDに定義されていない。Phase 2標準としては**有効化しない**。独自MFAは実装せず、Spring標準依存を
阻害しないことだけを保つ。factor lifecycleとacceptanceがOwner承認された時点を再判断条件とする。

### 4.3 KOIKI-PYFW fitting baseline

Phase 2はSpring Securityを先に組み込むだけでなく、KOIKI-PYFWが提供済みの利用者向け能力を棚卸しし、
Spring標準、KOIKI Framework、Reference、Customer、deferredへ割り当てるfittingをproduction実装前に行う。
詳細Evidenceは`phase2-koiki-pyfw-security-fitting.md`を正本とする。

- browser認証はPython版のJWT Cookieを移植せず、HTTP Session + Spring Session JDBCへ置換する。
- password hash、CSRF、JWT validation、OIDCはSpring Security標準を優先する。
- Amazon Cognito User PoolはKOIKI-JavaWebから直接接続する場合、Cognito専用APIではなく標準OIDC Providerとして扱う。
- ALB＋Cognito等のEdge Authenticationはcloud固有Adapterへ分離し、Phase 2では共通trust contractとnegative pathを設計する。
- login attempt、lock、reset、identity、security auditはSpring event / hookとKOIKI固有契約の境界をfixtureで検証する。
- access / refresh token発行、rotation、reuse検知、revokeはPhase 2でcontract gapまで分析し、実装phaseをOwnerが決定する。
- email addressはlogin identifier候補であり、変更不能なFramework user IDとは分離する。

## 5. Authentication / authorization profiles

### 5.1 Recommended chain boundary

| Order | Matcher / state | Authentication | CSRF / session |
|---:|---|---|---|
| 1 | 明示されたBearer API path | OAuth 2.0 Resource Server JWT | stateless。Bearer専用pathだけCSRF対象外 |
| 2 | 明示されたbrowser path | local loginとOIDC login | HTTP Session、CSRF有効、session fixation保護 |
| 3 | 明示的に有効化されたEdge Authentication path | cloud Adapterが検証済みのPre-Authentication | raw proxy headerを直接信用しない。Session / CSRFはprofile契約で明示 |
| 4 | unmatched | deny all | 認証方式を推測しない |

1つのchainへlocal、OIDC、Bearer、Edge Authenticationを無計画に混在させない。FrameworkはCustomerの公開URL、業務resource ownership、
業務状態policyを決めない。applicationはmatcherと認可ruleを明示し、未設定・重複・空白がある構成は起動時に拒否する。

### 5.2 Authorization contract

- URL securityとMethod Securityを併用し、双方の未認証・権限不足をnegative testする。
- RoleはPermissionの集合、実際の細粒度判定はPermission authorityを用いる。
- resource ownershipと業務状態はCustomer / ReferenceのApplication Use Case側policyとし、Frameworkへ入れない。
- UI非表示、route guard、JavaScriptを認可境界にしない。
- JWTは署名、`iss`、`aud`、`exp`、`nbf`、scopeを検証し、固定Clockとtest keyで境界値を再現する。
- Authorization Server、token発行、refresh、revocationは現行Phase 2 DoDには含めない。ただしKOIKI-PYFW parityの
  fitting / phase decisionをP2-F4で完了し、将来実装を阻害するidentity、audit、session失効境界を確定する。

### 5.3 React SPA / SSO profiles

| Profile | Authentication | Phase 2 responsibility |
|---|---|---|
| same-origin Session SPA | KOIKI-JavaWebのOIDC Client + Spring Session | 第一標準。CSRF、Cookie、2 instance、logout |
| Next.js BFF | BFF session。BFFからKOIKI APIへBearer Access Token | Customer-owned BFFを許容・推奨。KOIKIはJWT / Permissionを通常検証 |
| direct Token SPA | React public clientのAuthorization Code + PKCE | 明示opt-in。KOIKIはResource Server / CORS / negative pathを提供 |

RFC 10017を受け、BFFはKOIKI本体へ必須同梱しないが禁止しない。業務・機微情報・PII用途ではsame-origin Session
またはBFFを優先し、browser-only token方式はrisk assessment / acceptanceを要する。direct Token SPAからAPIへ送るのは
Access Tokenだけとし、ID Token、Cookie fallback、query tokenを拒否する。詳細は
`phase2-spa-sso-security-fitting.md`に記録する。

### 5.4 Cognito / Edge Authentication boundary

- KOIKI-JavaWebがAmazon Cognito User Poolへ直接接続する構成はapplication-direct OIDCであり、Spring Security
  OAuth 2.0 Loginの標準Provider設定として扱う。Cognito固有Public APIまたは専用Framework moduleを作らない。
- Cognito発行Access Tokenを受ける構成はBearer Resource Serverであり、issuer、audience / resource、time、scopeを検証する。
- ALB＋CognitoまたはALB＋外部OIDCはEdge Authenticationである。Phase 2では署名済みclaim、期待するALB ARN等の
  edge識別子、到達経路、external subject link、authority変換、audit、偽装header拒否をAdapter contractとして定義する。
- AWS固有production Adapterと実ALB acceptanceはPhase 4候補とする。Cognito Identity PoolによるAWS一時credentialは
  Web loginと分離し、Phase 2対象外とする。

公式確認先:

- <https://docs.aws.amazon.com/cognito/latest/developerguide/federation-endpoints.html>
- <https://docs.aws.amazon.com/elasticloadbalancing/latest/application/listener-authenticate-users.html>

## 6. Identity / table / migration inventory

### 6.1 Candidate logical tables

| Table candidate | Stored data / boundary |
|---|---|
| `koiki_user` | Framework user identifier、login identifier、status、lock state、version。社員番号・所属・雇用状態を含めない |
| `koiki_role` / `koiki_permission` | 標準Role / Permission definition |
| `koiki_user_role` / `koiki_role_permission` | Framework-owned joins |
| `koiki_password_credential` | password hashと更新時刻。平文・可逆暗号を保存しない |
| `koiki_password_reset` | one-time tokenのhash、expiry、used state。raw tokenを保存しない |
| `koiki_login_attempt` | account / source keyの失敗集計、lock判定。IP保持期間と匿名化は要決定 |
| `koiki_external_identity_link` | issuer + subjectとFramework user ID。provider固有claimを保存しない |
| `koiki_audit_event` | §15.4標準項目の安全な値。password / token / secret / PII全文を保存しない |
| `koiki_session` / `koiki_session_attributes` | Spring Session JDBC。実際のtable-name設定と両DB DDLで確定 |

全tableはFramework Flyway location `classpath:db/migration/koiki`と`koiki_flyway_history`を使う。
Referenceはmigrationを持たず、公開contractから操作する。Customer tableから外部キーを張らない。
ID型、timestamp精度、文字列長、index、retention、PII分類はDDL作成前のCPで確定する。

### 6.2 Oracle rule

最初はPostgreSQL / Oracle共通DDLを使用し、実測失敗前にvendor分岐しない。小文字・引用符なし、`VARCHAR`、
Boolean型不使用、予約語回避、方言関数回避をreview checklistへ追加する。Oracle smokeは設計適合であり正式supportではない。

## 7. Session / single execution

### 7.1 Recommended defaults

- Spring Session JDBCを使用し、Boot / Spring Sessionのschema自動初期化を無効にしてFramework Flywayだけを正本にする。
- `FlushMode.ON_SAVE`を開始候補とし、save modeは変更属性のみを第一候補に、DB write countを実測して確定する。
- Secure、HttpOnly、SameSite、session fixation、logout失効をintegration testする。
- sessionへJPA Entity、credential、token、非portable objectを保存しない。
- 2つの独立application processを同一PostgreSQLへ接続し、片方停止後に同じcookieで継続できることを外部観測する。
- Web instance内の既定cleanupを無効化し、non-web maintenance processからのみcleanupする。

### 7.2 Stop condition

cleanupに必要なFramework-owned single-execution contractが存在しないため、Consumerのadvisory-lock codeを流用しない。
PostgreSQLとOracleの双方で成立する排他方式、またはDB別adapterをfixtureで比較し、契約とfailure semanticsをOwner reviewする。

## 8. Audit transaction

Public API候補は業務監査とセキュリティ監査を呼出時点で区別する。単一のbooleanやenumだけで伝播方式を
呼出側へ委ねない。

| Path | Transaction | Required evidence |
|---|---|---|
| Business audit | callerの業務transactionと同一。監査失敗時は業務も失敗 | 業務例外でbusiness rowとaudit rowが双方0件 |
| Security audit | `REQUIRES_NEW` | 外側transaction rollback後もsecurity audit rowが1件 |
| Side effect | Phase 2では実装しない | Level 2までdeferred |

Role / Permission管理と管理者Password変更はbusiness audit、本人reset要求、login成功 / 失敗、lock、logout、認可拒否は
security auditとする。security audit書込み失敗時のfail-closed範囲は未決定であり、成功Login / reset token発行を
継続させない案を第一候補としてCPで検証する。

## 9. External environments

| Environment | Recommended required-CI fixture | External dependency / decision |
|---|---|---|
| OIDC | credential不要のephemeral local issuer。Session ClientとSPA public client / PKCEを分離 | hosted IdPは任意acceptance。test provider方式をOwner決定 |
| Amazon Cognito User Pool | required CIではlocal issuerで標準OIDC契約を再現 | application-direct OIDCの任意hosted acceptance。secret、redirect URI、availabilityを別承認 |
| ALB＋Cognito | Phase 2では署名・edge識別子・偽装headerのcontract / negative fixture | production Adapterと実ALB acceptanceはPhase 4候補 |
| JWT | test内RSA key / JWKS、固定issuer / audience / Clock | private keyをartifact / logへ出さない |
| 2 instance | PostgreSQL 17 Testcontainers + packaged app 2 process | Docker daemonが必要 |
| Oracle | `testcontainers-oracle-free:2.0.5` + digest固定したOracle Free image | edition / version / image / license / runner容量をOwner決定 |

OIDC、JWT、OracleにRepository secretを必須としない構成を優先する。外部providerを使う場合はredirect URI、client、
credential rotation、artifact retentionを別承認する。

## 10. Test / CI / evidence

1. focused unit / ApplicationContext / MockMvc negative tests
2. PostgreSQL Testcontainers transaction、migration、identity、audit tests
3. packaged 2 instance session journey
4. root / Public API / NullAway / ArchUnit / Java runtime aggregate
5. milestone PR CI、merge後main CI
6. Oracle nightlyは通常required checkから分離し、安定性確認後もrelease blockerとして扱う

通常PRへOracleを入れない。nightly failureを黙ってrerun成功へ置換せず、product defect、image / runner outage、flakyを分類する。
log / Problem Details / test report / artifactにpassword、token、secret、private key、PII全文がない検査を各milestoneへ入れる。

## 11. Governance inventory

ADR-006〜009、020、029、036、041、042、044は現時点の候補と矛盾しない。ADR-006〜008は
`phase2-spa-sso-security-fitting.md`をEvidenceとして、Session / BFF / direct Token profileへ解釈を進展させた。
次は新規または改訂ADR候補である。

- Security artifact / Public API / profile boundary
- local identityとauditの公開contractおよびtransaction failure semantics
- Spring Session table / cleanup / single execution方式
- MFA非採用と再判断条件
- Oracle Free固定環境とnightly governance

Security作業用SkillはMilestone Aで新設し、機械規則を複製せずprofile選択、secret、ownership、検証順を記述する。
Reference `identity`は既存business-feature Skillを使用する。

## 12. Gate P2-1 conclusion

Phase 2は開始可能だが、production実装開始にはGate P2-2 Owner承認が必要である。特に次を未決定のまま進めない。

1. Security / identity / audit / single-execution artifact境界と最初のPublic API
2. required CIのlocal OIDC issuer方式、Cognito hosted acceptanceの任意性、ALB＋CognitoをPhase 4 Adapter候補とする境界
3. Security audit失敗時のfail-closed範囲
4. session table名、save mode、cleanup排他方式
5. Oracle Free edition / version / image digest / license / nightly runner
6. MFAをPhase 2で有効化しない判断
7. KOIKI-PYFW fitting matrix、email login identifierとimmutable user IDの分離
8. token発行 / refresh / rotation / reuse検知 / revokeの実装phase。Phase 2へ前倒しする場合のDoD / ADR変更
9. ACCEPTEDのSPA 3 profileを前提としたOIDC test provider、CORS property、logout semantics、profile別fixture、
   Phase 4 Next.js BFF参照実装およびALB＋Cognito production Adapterの採否

提案する実行順と承認対象は
`docs/development/KOIKI-JavaWeb-FW_Phase2実行計画_v0.1.md`に記録する。
