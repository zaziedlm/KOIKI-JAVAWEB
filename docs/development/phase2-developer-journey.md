# Phase 2 Security Foundation Developer Journey

## 1. Audience and current availability

本書は、KOIKI-JavaWeb-FW上でSecurityを伴う業務アプリケーションを構成するエンジニアの入口である。
Phase 2の正式なProject TemplateやCustomer Applicationを提供するものではなく、承認済みFramework contract、
Referenceおよび非配布Consumerから、依存選択、Ownership、実装境界、診断と検証の順序を示す。

Phase 2の内部snapshotは、source commit `af7b4f71d885fe4991e5fcf85fcf8888aec6a539`、workflow run
`34701933485`でpublishとfresh remote acquisitionを検証済みである。これは認証を要する内部snapshotであり、正式releaseや
一般公開を意味しない。Repository外から利用するには承認済みGitHub Packages repositoryの設定と認証が必要である。
受入証拠は[Gate C remote Evidence](../architecture/validation/phase2-gate-c-c2-remote-evidence.md)、Repository内での成立確認は
§8のTooling-owned Harnessを参照する。

## 2. Start with ownership and business structure

Security設定から書き始める前に、次を決める。

```text
Customerが所有する業務
  -> business module
    -> Public boundary
      -> Tier 1 / Tier 2
        -> Controller / Use Case / Domain / Adapterの責務
          -> Security profileと必要なFramework contract
```

- Customerはroute、Role / Permission定義、業務policy、画面、外部IdP設定、credential供給、Customer migrationを所有する。
- Frameworkはdefault deny、Security / Identity / Audit / Sessionの共通契約とFramework migrationを所有する。
- ReferenceはFrameworkの使い方を実証するが、Customerのcopy元となるProject Templateではない。
- Consumerとverification fixtureは配布成果物ではなく、package / runtime境界を検証するToolingである。

業務module、Tier、Controller / Use Case / Domain / Repositoryの判断は
[KOIKI Business Feature Work](../agent/skills/koiki-business-feature-work/SKILL.md)に従う。

## 3. Select dependencies deliberately

KOIKI Parentを使用できるprojectは`org.koikifw:koiki-parent`をparentとし、Repository外projectでは`relativePath`を空にする。
既存の独自parentを維持する場合は`org.koikifw:koiki-dependencies-bom`を`dependencyManagement`でimportする。
両方を重複してversion管理の正本にしない。

### 3.1 KOIKI Starter selection

| Need | Select | Do not add automatically |
|---|---|---|
| Servlet Securityの安全なfallback、OIDC Client、Bearer Resource Server | `koiki-starter-security` | Session、Identity persistence |
| Business / Security Audit | `koiki-starter-audit` | 外部log backend、Customer監査table |
| local Identity、Role / Permission、外部identity link | `koiki-starter-identity` | reset token / mail、Customer属性 |
| server-side Session共有、全Session失効、期限切れcleanup | `koiki-starter-session-jdbc` | Bearer-only applicationへのSession |

`koiki-starter-session-jdbc`はIdentity、Audit、Security、Dataの必要なKOIKI依存を推移的に含む。Sessionを利用するApplicationでは、
Application所有dependencyとして少なくともWebMVC、PostgreSQL driver、Flyway PostgreSQL moduleを用途に応じて追加する。
Bearer-only APIはSession Starterを導入せず、Security StarterとApplicationが必要とするWeb / Data dependencyだけを選ぶ。

各Starterの正確な責務は[Starter index](../../koiki-starters/README.md)から確認する。未使用Starterや将来用moduleを追加しない。

Session / Identityを利用するApplicationの最小依存選択は次の形になる。これは完成POMやProject Templateではない。
`0.1.0-SNAPSHOT`はGate Cで承認された内部GitHub Packages repositoryから解決できるが、この例にはrepository設定や
credential設定を含めない。

```xml
<parent>
  <groupId>org.koikifw</groupId>
  <artifactId>koiki-parent</artifactId>
  <version>0.1.0-SNAPSHOT</version>
  <relativePath/>
</parent>

<dependencies>
  <dependency>
    <groupId>org.koikifw</groupId>
    <artifactId>koiki-starter-session-jdbc</artifactId>
  </dependency>
  <dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webmvc</artifactId>
  </dependency>
  <dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-database-postgresql</artifactId>
    <scope>runtime</scope>
  </dependency>
  <dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
  </dependency>
</dependencies>
```

### 3.2 Security profile selection

| Profile | Use when | Customer-owned configuration | Required boundary |
|---|---|---|---|
| local Session | email + passwordとserver-side Sessionを使用する | login UI、Customer chain、user provisioning、HMAC secret供給 | local authは明示enable、Session JDBCを使用 |
| OIDC Session | external OIDC Providerでbrowser loginする | client registration、callback matcher、issuer / client secret、provisioning / link policy | authorization-code、state / nonce、HTTP Session |
| Bearer API | 外部issuerのAccess TokenでAPIを保護する | API matcher、issuer、audience validator、scope mapping、CORS | ID Token拒否、401 / 403分離、Session fallbackなし |

同じpathでSession Cookie、Bearer Access Token、ID Tokenまたはraw edge headerをfallback認証しない。Authorization Server、
token発行 / refresh / revoke、SAML、Redis、WebFluxおよびcloud固有AdapterはPhase 2の導入対象ではない。

MVC単一JAR、same-origin React、Next.js BFF、direct Token SPAまたはALB edge認証のどれを選ぶかは、
[UI / Authentication Profile Selection Guide](frontend-authentication-profile-guide.md)でdeployable、OAuth Client、
Browser credentialおよびKOIKI API境界を先に決める。本節のSecurity profileをfrontend技術名だけから自動選択しない。

## 4. Implement through public seams

Customer codeは`org.koikifw.*.internal`、Framework JPA EntityまたはRepositoryを参照しない。

- Inbound Adapterはrequest受付、形式検証、DTO変換、response整形だけを行う。
- Application Use Caseはtransaction、権限確認、処理順序とFramework Public contract呼出を所有する。
- Tier 2の業務不変条件はDomain Modelへ置く。
- Customer persistence、IdP、外部API等はOutbound Adapterへ置く。
- Framework Identity管理には`IdentityQuery` / `IdentityAdministration`等の公開契約を使用する。
- Audit分類はBusinessとSecurityの意味で選び、transaction挙動を呼出側が推測で変更しない。
- Session cleanupはWeb process内schedulerではなく、公開`SessionCleanup`をnon-web単一実行経路から呼ぶ。

[Reference identity module](../reference/README.md)はController → Application Use Case → Framework Public contractの配置例である。
ただし、業務語彙、Role、画面、provisioningまたはmigrationをそのままCustomerへcopyしない。

## 5. Preserve secure defaults

- Customer `SecurityFilterChain`は対象を明示し、KOIKI fallbackより高い優先順位で構成する。
- どのCustomer chainにも一致しないrequestはfallback denyへ到達させる。
- CSRFとSpring Security標準Security Headerを無効化しない。変更には経路単位の明示設定とtestを必要とする。
- local authenticationは`koiki.identity.local-authentication.enabled=true`でだけ有効化する。
- source protectionを`APPLICATION`とする場合、HMAC key IDと32 byte以上のsecretをdeployment environmentから供給する。
- Session JDBCではFramework既定のinitializer `never`、table `koiki_session`、Web cleanup disabledを上書きしない。
- `spring.jpa.hibernate.ddl-auto=validate`を使い、Framework / Customer FlywayよりJPA schema generationを先行させない。
- built-in user、固定password、test key、failure switchは存在しない。初期identityはCustomerの承認済みprovisioning経路で用意する。

secret、password、JWT、private key、raw emailまたは外部subjectをsource、log、Problem Details、Audit payloadへ保存しない。

local authenticationを選ぶ場合の設定境界は次のように明示する。値そのものはcommitせず、deployment environmentから供給する。

```properties
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.open-in-view=false
koiki.identity.local-authentication.enabled=true
koiki.identity.login-attempt.source-protection=APPLICATION
koiki.identity.login-attempt.source-hmac-key-id=${KOIKI_SOURCE_HMAC_KEY_ID}
koiki.identity.login-attempt.source-hmac-key=${KOIKI_SOURCE_HMAC_KEY}
```

Session initializer、table名、Web cleanup cronはStarterの安全な既定に任せ、Application側のpropertiesへ重複記載しない。

## 6. Diagnose before adding a workaround

| Observation | First check | Expected action |
|---|---|---|
| 未一致routeが401 / 403 | Customer chainのmatcher / order | 意図するrouteだけをCustomer chainへ追加する。fallbackを広域permitしない |
| POST等が403 | authentication、Permission、CSRF token | CSRFを全体無効化せず、拒否原因を経路別に確認する |
| local auth起動失敗 | enable、source protection、HMAC key ID / secret | secretを環境から供給し、値をlogへ出さない |
| `IdentityAdministration`を注入できない | Audit、password checker、Session invalidator | 管理機能の必須協力beanを明示する。Query-only構成と混同しない |
| Session JDBC起動失敗 | initializer、table name、cleanup cron | KOIKI固定値への独自overrideを除去する |
| migration / JPA validate失敗 | PostgreSQL Flyway module、location、history、schema | Framework SQLをcopyせず、所有者別migrationと履歴を修正する |
| OIDC login失敗 | registration、issuer、redirect URI、state / nonce | Spring Security / Boot標準設定とCustomer matcherを確認する |
| Bearer 401 / 403 | token種別、signature / issuer / audience / time、scope | 401認証失敗と403権限不足を分け、ID TokenをAccess Token代用しない |

原因がPublic API、設定契約、diagnosticまたはruntime実装の欠陥にある場合、READMEの回避策で隠さずFramework側の
corrective workとしてArchitecture Ownerへ戻す。

## 7. Application development verification loop

通常の業務開発では、変更したmoduleのunit / architecture / application testから始め、次にpackage済みApplicationを観測する。

1. Controller迂回を含むURL / Method Securityの拒否をtestする。
2. Business transactionとBusiness Auditのcommit / rollbackを同時にtestする。
3. 認証失敗、lock、Security Auditの独立性を実PostgreSQLでtestする。
4. Session profileではlogout、権限変更後の全Session失効、2 instance継続をtestする。
5. cleanup taskをnon-web processで起動し、競合、終了code、crash後retryを確認する。
6. package済みJAR、DB row、HTTP responseとsanitized logを外部観測する。
7. child process、container、一時repository、fixture targetを終了時にcleanupする。

Customer CIへRepository固有の3ラウンドGate B aggregateをそのまま必須化しない。利用Starter、業務risk、構成、
デプロイ形態に合わせてfocused testと実DB integrationを選ぶ。

## 8. Reproduce the verified Framework journey

Framework保守者がPhase 2の配布境界を再現する入口は次である。PowerShell 7、Java 21 / 25、Maven Wrapper、
Linux Docker Engineを前提とし、開始前にclean worktreeと対象commitを確認する。

```powershell
git status --short --branch
java -version
.\mvnw.cmd -version
docker version

pwsh -NoProfile -File build-support/security-foundation-verification/verify-gate-a-security-foundation.ps1
pwsh -NoProfile -File build-support/security-foundation-verification/verify-p2-c2-consumer.ps1
pwsh -NoProfile -File build-support/security-foundation-verification/verify-p2-b4-reference-journey.ps1
```

- Gate Aはlocal / OIDC / Bearer profile、default deny、CSRF / HeaderとJava 21 / 25を確認する。
- C2 Consumerは隔離repositoryからPublic APIだけを使用し、PostgreSQL migration / Identity / Audit / Sessionを確認する。
- B4 ReferenceはRole付与 / 剥奪、Method Security、Audit / Session rollbackをpackage済みJARから確認する。

これらは日常のCustomer build commandではなく、Framework contractを再現するToolingである。P2-C3全体の最終command setは
[Security Foundation Verification](../../build-support/security-foundation-verification/README.md)を参照する。

## 9. Know what is not yet promised

- 正式release、一般公開repositoryおよびCustomer向けsupport付き配布
- Project Template、code generator、正式Upgrade / Migration Guide
- 実Customer applicationの完全移行と正式OpenRewrite recipe
- Authorization Server、refresh token lifecycle、SAML、Redis Session、WebFlux
- React / Next.js production実装、Spring Modulith Level 2、非同期Event
- Oracle、AWS固有Adapterおよびproduction deployment reference

これらは所定の後続Phaseまたはoptional Gateで別途判断する。P2-C3のcloseoutを理由に先行実装しない。
