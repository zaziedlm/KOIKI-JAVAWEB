# Phase 2 P2-B2 B2-2 Identity core / migration verification

## 1. Status and scope

- **Verification date:** 2026年9月7日
- **Work package:** `P2-B2 / B2-2`
- **Status:** `IMPLEMENTED / VERIFIED / ARCHITECTURE OWNER APPROVED`
- **Approved by:** Shuichi Kataoka、2026年9月7日
- **Ownership:** Framework（Identity contract / persistence / migration）+ Tooling（非配布T4 fixture）
- **Baseline:** B2-C1〜C10 Architecture Owner承認済み

本記録は、承認済みの単一`koiki-starter-identity`、Public API 10型、Identity内部JPA model、
`V2026090301__create_koiki_identity.sql`およびPostgreSQL実測結果を記録する。P2-B2全体の完了記録ではない。
Authentication / attempt / lock動作はB2-3、Identity administration / Audit semanticsはB2-4に残す。

## 2. Implemented boundary

### 2.1 Formal artifact

- root ReactorとKOIKI BOMへ`org.koikifw:koiki-starter-identity:0.1.0-SNAPSHOT`を登録した。
- Identity StarterはSecurity、Data JPA、Audit、JSpecifyへ依存し、cloud SDK、mailer、Spring Session、
  Reference / Customer codeへ依存しない。
- `koiki-starter-data`へFlywayのPostgreSQL database moduleを追加した。Flyway 12でPostgreSQL 17 migrationを
  実行するために必要な、既存PostgreSQL baselineの補完である。
- Identity migrationを実行するapplicationは、Identity Starterに加えて`koiki-starter-data`を導入する。

### 2.2 Public contract

承認済みの次の10型だけを`org.koikifw.identity`へ追加した。

1. `FrameworkUserId`
2. `FrameworkPrincipal`
3. `AuthenticationSource`
4. `UserStatus`
5. `IdentityUser`
6. `IdentityQuery`
7. `IdentityAdministration`
8. `UserSessionInvalidator`
9. `IdentityOperationException`
10. `IdentityFailure`

JPA Entity / Repository、canonical email、encoded password、lock reason、Spring Security型はPublic APIへ露出しない。
Identity administrationのproduction beanとSession invalidatorのno-op beanはまだ提供しない。

### 2.3 Persistence and migration

Identity内部JPA modelと、次の8つのFramework tableを追加した。

1. `koiki_user`
2. `koiki_role`
3. `koiki_permission`
4. `koiki_user_role`
5. `koiki_role_permission`
6. `koiki_password_credential`
7. `koiki_login_attempt`
8. `koiki_external_identity_link`

emailはinternal valueでtrim、ASCII確認、`Locale.ROOT` lowercaseを一度だけ行い、Entityの生成・変更メソッドが
case-preserving valueとcanonical valueを同時設定する。個別setterは設けない。DBも両列の対応とtrim済みvalueをCHECKで保護する。

canonical email、Role / Permission code、external issuer + subject、user + issuer、ACCOUNT / SOURCE attempt keyを
DB制約でも保護する。FKは`RESTRICT`、管理対象aggregateは`version`、timestampは`timestamp(6) with time zone`とした。
raw password、salt別列、direct user Permission、password reset、Spring Session tableは作成しない。

#### 2.3.1 SOURCE protection deployment note

2026年9月7日のOwner reviewで、BFF / SSR構成におけるSOURCE scopeの扱いを次のとおり整理した。

- `koiki_login_attempt`のACCOUNT / SOURCEは独立した保護軸であり、SOURCE rowを作成しない構成でも現行の
  CHECK制約とpartial unique indexは成立する。このため、本migrationへ有効 / 無効flag列を追加しない。
- BFFまたはServer Componentsの採用だけを理由にSOURCEを自動的に無効化しない。ローカルログイン要求を
  受け付ける境界と、そこで実際のclient sourceを信頼して識別できるかによって判断する。
- Next.js BFF + external IdPでは、KOIKI APIは通常Bearer Resource Serverであり、BFF / IdP側の認証失敗を
  `koiki_login_attempt`へ記録する責務を持たない。BFFがKOIKIのlocal loginを中継する個別構成では、全利用者が
  同じBFF sourceに集約され、applicationのSOURCE blockが一括拒否を招かないかを評価する。
- application内のSOURCE protectionを外部化する場合、代替制御は「実際のclient sourceを識別でき、login要求を
  最初に受ける公開境界」に置く。BFFより内側で送信元が集約されたWAF / load balancerを代替とみなさず、未検証の
  forwarded headerをIdentity Starterが直接信用しない。
- 外部化は防御の単純な無効化ではなく責務移動として扱い、将来propertyを設ける場合は既定有効のbooleanより
  `APPLICATION` / `EXTERNAL`のように責務を表すmodeを候補とする。`APPLICATION`で必要なHMAC設定が不足する場合は
  startup failureを維持する。

SOURCE mode、Port / Auto Configurationの構造、外部制御のCustomer acceptance項目は、認証試行ロジックを実装する
後続CPでB2-C5 / C6を更新して承認する。B2-2ではPublic API、property、no-op beanを先行追加しない。

#### 2.3.2 External identity link review note

2026年9月7日のOwner reviewで、`koiki_external_identity_link`のissuer、unlinkおよびAuditを次のとおり整理した。

- OIDC issuerはcase-sensitiveなidentifierとして検証済み`iss`を完全一致のまま保存する。末尾slashの有無を含む異なる
  文字列表現をalias化せず、canonical issuer列、lowercase、末尾slash除去またはURL decodeを導入しない。
- `(issuer, subject)`と`(user_id, issuer)`のunique制約は完全一致した値に適用する。許可issuerとの照合、unknownまたは
  表記不一致のdeny、および検証済みadapter以外からのlink拒否はB2-4のapplication実装とtestで保証する。
- 管理者unlinkは侵害linkの切断を妨げないため最後の認証手段でも許可し、認証手段を持たない`ACTIVE` userをDB制約で
  禁止しない。self-service unlinkはP2-B2の対象外とし、将来要件成立時に別の認証手段と直近の再認証を再reviewする。
- external link / unlinkはBusiness auditと同じtransactionで処理し、Audit失敗またはunlink時のSession全失効失敗では
  mutationをrollbackする。実装とSession連携の証拠はB2-4 / P2-B3で取得する。

以上からB2-2のDDL、EntityおよびPublic APIは変更しない。グランドデザイン§15.3へのexternal link / unlinkの明示追加は、
P2-B2 acceptance時の文書同期で行う。

## 3. Verification results

### 3.1 Root and compile baseline

目的は、Identity Starterの追加後もRepository全体がbuild可能で、既存のmodule / ownership規約、JDK 21、
Error ProneおよびNullAwayのbaselineを破っていないことを確認することである。

```text
./mvnw --batch-mode --no-transfer-progress clean verify
Reactor: 13 / 13 SUCCESS
Architecture Contract: 4 tests SUCCESS
ArchUnit Rules: 66 tests SUCCESS
```

確認内容と結果は次のとおりである。

- Reactor 13 moduleがすべてcompile / test / packageに成功し、module追加、BOMおよび依存解決に失敗がない。
- Architecture Contract 4 testsは、`@KoikiModule`のruntime contract、必須属性、承認済みenum値、および
  Customer相当の外部packageからの利用境界を確認した。
- ArchUnit Rules 66 testsは、正常fixtureを許可し、module間依存、Framework internal参照、ControllerからRepositoryへの
  直接依存、Domain modelのWeb境界露出およびpublic setter等の違反fixtureを検出できることを回帰確認した。
- Identity Starter単体でもJDK 21、Error Prone、NullAwayを含むcompile / packageが成功した。

この層はRepositoryと静的品質の回帰を示す。IdentityのDB制約や実PostgreSQL上の挙動は3.2で確認する。

### 3.2 PostgreSQL T4 fixture

目的は、PostgreSQL 17.11 Testcontainersへproduction migrationを実際に適用し、Public contractとDB制約を
実行時に確認するとともに、P2-A1〜A3およびP2-B1のSecurity / Audit fixtureを累積回帰することである。

```text
./mvnw -f build-support/security-foundation-verification/pom.xml clean verify
Tests run: 39, Failures: 0, Errors: 0, Skipped: 0

IdentityCoreMigrationFixtureTest: 5 / 5 SUCCESS
IdentityPublicContractTest: 3 / 3 SUCCESS
```

39 testsの内訳は次のとおりである。

| Test suite | Tests | Main verification |
|---|---:|---|
| `SecurityDependencyBaselineTest` | 1 | Security production dependency baseline |
| `SecurityAutoConfigurationContextTest` | 5 | Security Auto Configurationの条件と安全なfallback |
| `SecurityRequestBoundaryTest` | 4 | default deny、CSRFおよびrequest境界 |
| `LocalSessionAuthorizationTest` | 6 | local Session認証・認可 |
| `BearerProfileBoundaryTest` | 5 | Bearer JWT profile境界 |
| `OidcProfileCoexistenceTest` | 2 | OIDC profile coexistence |
| `AuditTransactionFixtureTest` | 8 | Business / Security Audit transaction semantics |
| `IdentityPublicContractTest` | 3 | Identity Public APIの値・immutable性・safe failure |
| `IdentityCoreMigrationFixtureTest` | 5 | Identity migration、DB制約、query |
| **Total** | **39** | **T0〜T4 cumulative regression** |

`Failures: 0`はassertion不一致なし、`Errors: 0`はSpring起動、Flyway、SQLまたはTestcontainersの実行時errorなし、
`Skipped: 0`は未実行testなしを表す。今回追加したIdentity固有8 testsだけでなく、既存Security / Audit 31 testsも
全件再実行した。

`IdentityPublicContractTest` 3 testsでは次を確認した。

1. `FrameworkUserId`はopaque UUID valueとしてparse、等価比較および文字列表現が成立し、不正値を拒否する。
2. `IdentityUser`のRole / Permission集合はimmutableで、呼出側から変更できない。
3. `IdentityOperationException`は承認済みfailure categoryと固定messageだけを公開し、email、passwordまたはSQLを漏らさない。

`IdentityCoreMigrationFixtureTest` 5 testsでは、PostgreSQL 17.11上で次を観測した。

1. clean databaseへ承認済み8つのIdentity tableを作成し、password reset / Session tableを作らず、timestamp精度を
   microsecondとする。同じFlyway historyでの再実行は0 migrationとなる。
2. canonical email重複を拒否し、credentialから参照されるimmutable user IDの変更をFKで拒否する。
3. emailとcanonical emailの不一致、および未trim emailをDB CHECKで拒否する。
4. Role / PermissionのFK、`(issuer, subject)`と`(user_id, issuer)`のunique制約を確認し、`IdentityQuery`が
   User→Role→Permissionをimmutable Public read modelへ変換する。version初期値は0となる。
5. credential schemaは`encoded_password`だけを保存し、raw password / salt列を持たない。ACCOUNT / SOURCE列の
   不正混在をCHECKで拒否し、各partial unique indexが同じkeyの二重rowを拒否する。

migration JARが8 tableだけを所有することや後続成果物が混入しないことは、3.3でpackaged artifactを直接検査する。

### 3.3 Aggregate script

目的は、workspace上のsourceだけでなく、隔離Maven repositoryへstageした正式JARとruntime dependencyを検査し、
B2-2の配布境界を再現可能な1 commandで保証することである。

```text
build-support/security-foundation-verification/verify-p2-b2-identity-core.ps1
Phase 2 P2-B2 Identity core verification succeeded (T0-T4 39/39).
```

scriptは次を順に実行する。

1. 一時的な隔離Maven repositoryへKOIKI正式release unitをstageする。
2. 生成されたIdentity JARについて、Public API inventory 10型、公開package内class、migration 1本、CREATE TABLE 8件、
   Auto Configuration imports 1件を承認済みinventoryと完全一致で検査する。
3. JARへpassword resetまたはSpring Session成果物が混入していないことを検査する。
4. 3.2の累積39 testsを再実行し、9 suiteそれぞれについて期待件数、failures 0、errors 0、skipped 0を
   Surefire XMLから再検査する。3.3の39件は3.2とは別の追加39件ではない。
5. Identity runtime dependencyにSecurity、Data JPA、AuditおよびJSpecifyが存在し、Spring Session、SAML、Mail、
   WebFluxまたはAWS SDKが混入していないことを確認する。
6. Data StarterにFlyway PostgreSQL database moduleが存在することを確認する。
7. 非配布T4 fixtureが正式release repositoryへinstallされていないことを確認する。
8. 検証終了時に一時領域を安全なtemporary pathであることを再確認して削除する。

したがって3.3の成功は、Identityのsourceが動くだけでなく、実際の配布JAR、依存関係、Public APIおよびmigration境界が
承認済みB2-2 contractどおりであることを示す。

## 4. Findings resolved during verification

1. `spring-boot-starter-flyway`だけではFlyway 12がPostgreSQL 17を認識しなかったため、
   `flyway-database-postgresql`をData Starterへ追加した。
2. `@Transactional`対象のIdentity query内部実装が`final`でCGLIB proxyを生成できなかったため、
   internal classをproxy可能にした。Public APIは変更していない。
3. Owner reviewでemailとcanonical emailの対応がapplicationだけでは保証されていない点を検出したため、
   internal email value、Entityの同時設定、DB CHECKの三層で不変条件を追加した。`PENDING`状態とDBのemail構文CHECKは追加していない。

いずれも実PostgreSQL／実ApplicationContextで検出し、修正後の累積fixtureで回帰確認した。

## 5. Deferred boundary and next action

本Evidenceが成立を主張しない範囲は次のとおりである。

- persistent UserによるForm Login、generic authentication failure
- account / source attemptのatomic update、lock / unlock、Security audit
- password policy、delegating encoding、compromised-password check、hash upgrade
- Identity administration mutation、Business / Security audit failure matrix
- 実Session全失効、2 process、Spring Session JDBC
- local password reset API / token / table / delivery

2026年9月7日、Architecture Ownerは本Evidenceをreviewし、B2-2 Identity core / migrationを承認した。
B2-2をcommit pointとして確定し、次はB2-3 Authentication / attempt / lockへ進む。
