# Phase 2 P2-B2 Identity contract / table review

## 1. Status and scope

- **Review date:** 2026年9月3日
- **Work package:** `P2-B2 / B2-1`
- **Status:** `COMPLETE / ARCHITECTURE OWNER APPROVED — B2-2 READY`
- **Approved by:** Shuichi Kataoka、2026年9月3日
- **Ownership:** Framework（Identity contract / persistence / migration）+ Tooling（非配布T4 PostgreSQL fixture）
- **Production change at review:** 0。Owner承認前にmodule、Public API、migrationを追加しない

本書は、P2-B2のLocal Identity、Password、認証試行、local credential lock、Password Reset境界、Role / Permission、
external identity linkを、artifact、型、property、tableおよび失敗時挙動の単位で比較する。P2-B1でacceptしたAudit contract、
P2-F2でacceptしたidentity semanticsおよびP2-A2 / A3のSpring Security seamをbaselineとする。

Spring Session JDBCと実Session全失効はP2-B3、Reference `identity`画面 / ControllerはP2-B4、Audit / Session migrationと
全Framework migrationのsupported upgrade総合はP2-C1の責務であり、本reviewで先行実装しない。

## 2. Review principles

1. OIDC / Bearerだけを使うapplicationへJPA、Identity tableまたはAuditを強制しない。
2. Spring Securityの`DaoAuthenticationProvider`、`UserDetailsService`、`PasswordEncoder`を認証処理の標準seamとする。
3. JPA Entity、Repository、encoded password、canonical email、内部lock reasonをPublic APIへ露出しない。
4. email、external subject、送信元IPはimmutable Framework user IDと区別し、用途のない複製を作らない。
5. 初期SSO適用ではpassword reset tokenをKOIKIが発行しない。将来local resetを実装する場合もraw secretを永続化・出力しない。
6. high-contention counterは「JPAだけ」に固執せず、実PostgreSQLでlost updateがない方式を優先する。
7. Session失効未実装をno-opで隠さず、P2-B3との接続点と未成立範囲を明示する。

### 2.1 Primary specification basis

- Spring Security Password Storage:
  <https://docs.spring.io/spring-security/reference/features/authentication/password-storage.html>
- Spring Security `DaoAuthenticationProvider`:
  <https://docs.spring.io/spring-security/reference/servlet/authentication/passwords/dao-authentication-provider.html>
- Spring Data JPA Locking:
  <https://docs.spring.io/spring-data/jpa/reference/jpa/locking.html>
- NIST SP 800-63B password verifier requirements:
  <https://pages.nist.gov/800-63-4/sp800-63b.html#passwordver>

Spring公式仕様からdelegating hash、`UserDetailsService` / `PasswordEncoder` seam、credential消去、lock機構を採用する。
NIST SP 800-63Bのpassword-only最低15文字、64文字以上のmaximum許容、blocklist、composition rule / 定期変更非推奨を
P2-B2のbaseline policyへ反映する。外部checkerや特定cloud serviceを必須runtime dependencyにはしない。

## 3. B2-C1 — Artifact placement

| Candidate | Shape | Evaluation |
|---|---|---|
| **A — single Identity Starter** | `koiki-starters/koiki-starter-identity`にPublic API、internal persistence / auto-configuration、Identity migrationを置く | **Recommended.** Local Identity / external linkを選ぶapplicationだけが導入でき、P2-B2でAPIと実装を同時利用するため空artifactにならない |
| B — Security Starterへ追加 | `koiki-starter-security`へJPA、Audit、Identity tableを追加 | Reject。OIDC / Bearer専用applicationまでDB依存となり、既存Security Starterのdefault-deny / protocol責務と永続Identityを混在させる |
| C — `-api` / `-impl`分割 | Identity APIとStarter実装を別artifactにする | Defer。初期利用者と代替実装がなく、release / compatibility境界だけを増やす |
| D — Data Starterへ追加 | `koiki-starter-data-jpa`がIdentity Entity / Repositoryを所有 | Reject。Data基盤がSecurity domainを所有し、Customerが意図せずIdentity schemaを導入する |

**推奨:** Candidate A。初期dependencyは`koiki-starter-security`、`koiki-starter-data-jpa`、`koiki-starter-audit`、
JSpecifyとする。cloud SDK、mailer、Spring Session、Reference codeへ依存しない。Starterを導入してもlogin / reset HTTP endpointや
test userを自動生成せず、Customer / ReferenceがSecurity filter chainとtransportを所有する。

## 4. B2-C2 — Public Identity API

### 4.1 Comparison

| Candidate | Shape | Evaluation |
|---|---|---|
| **A — use-case API + opaque values** | ID / principal / read model、管理use case、Session失効SPIだけを公開。reset APIは実利用時に追加 | **Recommended.** ReferenceがFramework tableを直接操作せず、Spring / JPA型も未使用の将来APIも増やさない |
| B — Entity / Repository公開 | `UserEntity`やSpring Data RepositoryをCustomerへ公開 | Reject。schema、transaction、fetch、migration Ownershipが越境する |
| C — Spring Security型だけ公開 | `UserDetailsService`、`Authentication`をCustomerが直接利用 | Reject。認証seamとしては利用するが、業務Controllerのcurrent user / Identity管理契約として過剰にSpringへ結合する |
| D — P2-B4までPublic APIを作らない | B2はinternal serviceだけで実装 | Reject。B4直前にEntity中心の実装を無理に公開APIへ包むriskがあり、B2のcompatibility検証対象も失う |

### 4.2 Recommended conceptual signatures

次は型と責務の承認対象を示す疑似signatureであり、まだsourceではない。

```java
public final class FrameworkUserId {
    public static FrameworkUserId parse(String value);
    public UUID value();
}

public interface FrameworkPrincipal {
    FrameworkUserId userId();
    AuthenticationSource authenticationSource();
    Set<String> permissions();
}

public enum AuthenticationSource { LOCAL, OIDC, BEARER, EDGE }
public enum UserStatus { ACTIVE, DISABLED }

public record IdentityUser(
        FrameworkUserId userId,
        String email,
        UserStatus status,
        Set<String> roleCodes,
        Set<String> permissionCodes,
        long version) {}

public interface IdentityQuery {
    Optional<IdentityUser> findById(FrameworkUserId userId);
}

public interface IdentityAdministration {
    FrameworkUserId createUser(String email);
    void changeEmail(FrameworkUserId userId, String email, long expectedVersion);
    void enable(FrameworkUserId userId, long expectedVersion);
    void disable(FrameworkUserId userId, long expectedVersion);
    void setLocalPassword(FrameworkUserId userId, char[] rawPassword, long expectedVersion);
    void unlockLocalCredential(FrameworkUserId userId);
    void createRole(String roleCode);
    void deleteRole(String roleCode, long expectedRoleVersion);
    void registerPermission(String permissionCode);
    void deletePermission(String permissionCode);
    void assignRole(FrameworkUserId userId, String roleCode, long expectedVersion);
    void revokeRole(FrameworkUserId userId, String roleCode, long expectedVersion);
    void grantPermission(String roleCode, String permissionCode, long expectedRoleVersion);
    void revokePermission(String roleCode, String permissionCode, long expectedRoleVersion);
    void linkExternalIdentity(FrameworkUserId userId, String issuer, String subject, long expectedVersion);
    void unlinkExternalIdentity(FrameworkUserId userId, String issuer, long expectedVersion);
}

public interface UserSessionInvalidator {
    void invalidateAll(FrameworkUserId userId);
}
```

`IdentityOperationException`と`IdentityFailure`を追加し、Public failure categoryは`INVALID_INPUT`、`NOT_FOUND`、`CONFLICT`、
`CONCURRENT_MODIFICATION`、`DEPENDENCY_FAILURE`に限定する。message、causeまたはcategoryでemail、account状態、role構造、
SQL、raw tokenを返さない。loginはこのcategoryを外部responseへ直接写さずgeneric responseへ変換する。

初期Public API inventoryは次の10型とする。

| Group | Public types |
|---|---|
| Identity value / read | `FrameworkUserId`、`FrameworkPrincipal`、`AuthenticationSource`、`UserStatus`、`IdentityUser` |
| Use case | `IdentityQuery`、`IdentityAdministration` |
| Outbound SPI | `UserSessionInvalidator` |
| Safe failure | `IdentityOperationException`、`IdentityFailure` |

`FrameworkUserId`はIdentity implementationが生成するrandom UUID v4、PostgreSQL物理列は`uuid`とする。IDはopaqueに扱い、時系列、tenant、
emailまたはDB採番を意味に持たせない。P2-B1の`AuditActor.user(String)`は維持し、Identity integrationがIDの文字列表現を渡す。
additive overloadは実需要が確認できるまで追加しない。

一覧、検索、Role / Permission read model、current-principal resolverはP2-B4の最初の実利用に合わせてadditive reviewする。
P2-B2で未使用のpagination、DTO mapper、REST endpointを先行公開しない。

## 5. B2-C3 — User / authority model

| Concern | Recommended model | Rejected conflation |
|---|---|---|
| Framework user | immutable user ID、email、`ACTIVE` / `DISABLED`、version | employee、department、tenant、IdP claimをUserへ埋め込む |
| Local credential | Userに0または1件。encoded password、変更時刻、local lock期限 | SSO-only userへdummy passwordを持たせる |
| Account disable | 全認証方式を拒否し、全KOIKI Sessionを失効 | password lockでOIDC / Edgeまで停止する |
| Local lock | password loginだけを一時拒否。既存Sessionは維持 | User lifecycle statusを`LOCKED`へ変更する |
| Authorization | User→Role→Permission。backend判定はPermission code | UserへのPermission直接付与、外部groupの無条件authority化 |
| External identity | Userとは別tableのverified issuer + subject link | email一致auto-link、provider claim複製 |

RoleはPermission集合とし、`koiki_user_permission`は作らない。Role / Permissionのcodeは大文字英数字と`_` / `:`による
安定した識別子とし、表示名やCustomer業務属性を兼ねさせない。Sessionには認証時のPermission snapshotを保持し、変更時は
対象userの全Sessionを失効する。RoleのPermission変更時は、そのRoleが付与された全userを失効対象にする。
requestごとのDB再読込とauthorization-version照合はP2-B2では採用しない。

## 6. B2-C4 — Password contract

### 6.1 Encoding and upgrade

| Candidate | Evaluation |
|---|---|
| **Spring `DelegatingPasswordEncoder` + `{id}` format** | **Recommended.** 新規hash方式とlegacy照合をSpring標準で分離し、`upgradeEncoding`を利用できる |
| algorithm固定列 + KOIKI独自dispatcher | Reject。Springと重複するformat / migration責務を作る |
| reversible encryption / raw password | Reject。要件違反 |
| `NoOpPasswordEncoder` fallback | Reject。testを含め正式artifactへ入れない |

- `PasswordEncoder` beanがなければSpringのdelegating encoderを提供し、Customer beanがあればback offする。
- DBには`{id}encoded`だけを最大512文字で保存する。algorithm名、saltまたはparameterを別列へ分解しない。
- 正しいpassword照合後に`upgradeEncoding`がtrueなら同じraw passwordから再encodeし、credential versionを条件に更新する。
  競合またはupgrade保存失敗はlogin成功を偽装せずsafe failureとし、raw passwordを再試行queueへ渡さない。
- internal `UserDetails`は`CredentialsContainer`として照合後にcredentialを消去し、Public principalへencoded passwordを持たせない。
- Public APIでraw passwordを受ける箇所は`char[]`に限定し、implementationはdefensive copyを処理後にzero-fillする。
  caller側copyの消去責務もJavadocへ記載する。ただしJVM memoryからの完全消去は保証しない。

### 6.2 Baseline password policy

| Rule | Recommended default / constraint |
|---|---|
| Minimum length | 15 Unicode code points。Phase 2はpassword-only local loginのため15未満への緩和propertyを設けない |
| Maximum length | 128 Unicode code points。Customer設定は64以上、1024以下だけ許可 |
| Character handling | spaceとUnicodeを許容し、NFC normalization後の全体をhashする。login時も同じ処理 |
| Composition | upper / lower / digit / symbolの組合せ規則を設けない |
| Rotation | 定期変更を強制しない。侵害、本人変更、管理操作、将来reset時に変更する |
| Compromised password | 作成 / 管理変更時にSpring `CompromisedPasswordChecker`を必須とする。将来resetにも同じpolicyを適用 |

Frameworkは外部ネットワーク型checkerを既定導入しない。Customerはriskと可用性に応じてoffline blocklistまたはSpring提供checkerを
beanとして選ぶ。P2-B2 fixtureはネットワーク不要のdeterministic checkerをTooling内だけに置く。checkerなしでpassword設定機能を
有効にした構成はstartup failureとし、runtimeで検査をskipしない。

## 7. B2-C5 — Login attempt key and privacy

### 7.1 Comparison

| Candidate | Account enumeration / privacy | Evaluation |
|---|---|---|
| raw canonical email + raw IP | DBで追跡しやすいがPIIを重複保存 | Reject |
| unsalted SHA-256(email / IP) | dictionary attackで復元可能 | Reject |
| unknown emailもHMAC集計 | email自体は隠せるが、秘密鍵rotationと不要なidentifier追跡を増やす | Defer。現要件では不要 |
| **known user ID + source HMAC、unknown email非保存** | account lockは既知ID、unknown試行は送信元だけ集計 | **Recommended.** 最小PIIでenumeration防止とsource throttlingを両立 |
| applicationはaccountだけ、sourceは承認済み公開境界 | client sourceを識別できるBFF / ingressへ責務を移せる | 明示的な`EXTERNAL` modeとして採用。単なる無効化は不採用 |

### 7.2 Recommended key semantics

- known userのaccount attempt keyは`FrameworkUserId`とする。email変更後もcounter / lockを維持する。
- unknown emailはcanonical valueもhashも保存しない。Spring Security標準のtiming mitigationを維持し、外部responseを一般化する。
- source keyは`HMAC-SHA-256(normalized remote address, deployment secret)`の32 byte digestとする。raw IPを保存しない。
- 全instanceで同じ32 byte以上のsecretとkey IDを使う。key rotation後は旧bucketを再利用せず、保持期限で削除する。
- reverse proxy headerをIdentity Starterが直接信用しない。Servlet container / deploymentでtrusted proxy処理後のremote addressだけを使う。
- HMAC key、raw IP、raw unknown emailをAudit、Application log、metric tag、exception、test reportへ出さない。
- Application内のsource制御はWAF / load balancerのrate limitを置き換えない。
- local password認証は`koiki.identity.local-authentication.enabled=false`を既定とし、利用applicationだけが明示的に有効化する。
- source保護は`APPLICATION`を既定とする。`EXTERNAL`は、実client sourceを識別してlocal login要求を最初に受ける
  承認済み公開境界が同等制御を所有する場合だけ選択し、KOIKIはSOURCE rowを作成しない。ACCOUNT保護は継続する。
- BFF / SSRの採用だけでは`EXTERNAL`を選択しない。BFFがlocal loginを中継してsourceを集約する構成では、公開境界側の
  代替制御をdeployment acceptanceで確認する。

## 8. B2-C6 — Attempt and lock semantics

### 8.1 Recommended defaults

| Scope | Threshold / window | Block / lock | Success handling |
|---|---|---|---|
| Known account | 5 consecutive failures / 15 minutes | local credentialを30 minutes lock | account counterをclear。source counterはclearしない |
| Source | 100 failures / 15 minutes | sourceを15 minutes block | 単一userの成功ではclearしない |

locked accountへの追加試行は`locked_until`を延長せず、source counterとSecurity auditだけを更新する。これにより第三者が
永続lockを維持することを防ぐ。期限到達後は自動解除し、次の成功でaccount counterをclearする。管理解除はcounterと
`locked_until`をclearし、Security auditが成功した場合だけ成立する。

数値は次のpropertyでCustomer調整を許す。

```properties
koiki.identity.local-authentication.enabled=false
koiki.identity.password.maximum-length=128
koiki.identity.login-attempt.account-threshold=5
koiki.identity.login-attempt.account-window=15m
koiki.identity.login-attempt.account-lock-duration=30m
koiki.identity.login-attempt.source-protection=APPLICATION
koiki.identity.login-attempt.source-threshold=100
koiki.identity.login-attempt.source-window=15m
koiki.identity.login-attempt.source-block-duration=15m
koiki.identity.login-attempt.retention=24h
koiki.identity.login-attempt.source-hmac-key-id=<deployment key id>
koiki.identity.login-attempt.source-hmac-key=<base64 secret from secret manager>
```

| Property group | Accepted range |
|---|---|
| Password maximum | 64〜1024 code points |
| Account threshold | 3〜10 failures |
| Account / source window | 1 minute〜24 hours |
| Account lock / source block | 1 minute〜24 hours |
| Source threshold | 10〜10,000 failures |
| Attempt retention | 各window / block以上、30 days以下 |

0、負値、範囲外、retention不足または相互矛盾はstartup failureとする。
`APPLICATION`では`source-hmac-key`に既定値を置かず、短いkey、空keyまたはplaceholderをstartupで拒否する。
`EXTERNAL`ではHMAC keyを要求せず、source fingerprint用beanも生成しない。property valueはActuator、config dump、test reportへ
出さない。`minimum-length`、token byte数、hash方式は安全性を弱める設定になるためproperty化しない。

### 8.2 Concurrency and audit failure

| Candidate | Evaluation |
|---|---|
| read-modify-writeだけ | Reject。2 instanceでlost updateし閾値を迂回できる |
| JPA optimistic retryだけ | User / Role管理には採用するが、集中するattempt counterではretry exhaustionがfailure欠落になり得る |
| pessimistic row lock | 既存rowには有効だが初回insert競合の別処理が必要 |
| **PostgreSQL atomic upsert + conditional credential update** | **Recommended.** counterを1 SQLで増加させ、閾値到達者だけがlockを設定できる |

User / Role / Permission / credential / external linkには`version`を持たせ、管理操作のstale updateを拒否する。login attemptだけは
PostgreSQL `INSERT ... ON CONFLICT DO UPDATE ... RETURNING`をinternal adapterで使用する。これはPostgreSQL baselineに限定した
高競合counterの例外であり、Public APIへSQL / vendor型を露出しない。将来DB対応では同じ外部挙動を別adapterで実証する。

login failure時はattempt / lockの防御状態を先に永続化し、Security auditが失敗しても認証拒否と防御状態を取り消さずalertする。
login成功はSecurity audit成功前にSecurityContext / Sessionを成立させず、audit失敗時はgeneric authentication failureとする。

## 9. B2-C7 — Password reset semantics

### 9.1 Initial SSO project premise

初期適用projectはSSO認証を前提とする。SSO credentialのpassword reset、recovery token、本人確認、通知およびdelivery SLAは
IdPが所有し、KOIKIがIdP password resetを複製しない。したがってP2-B2では、KOIKI local password resetのtoken発行、
mail送信、reset endpoint、delivery adapter、reset tableをproduction実装しない。

| Candidate | Evaluation |
|---|---|
| local resetをB2で完全実装 | Reject for initial scope。初期SSO利用で未使用となり、mail / token lifecycle / delivery failureを先行固定する |
| SSOとlocal resetを同じflowへ統合 | Reject。IdPとKOIKIのcredential Ownershipが混在する |
| **設計上の安全条件だけ保持し、実装をdefer** | **Recommended.** 現在の要件に合わせ、未使用Public API / table / propertyを作らない |
| reset概念を設計から完全削除 | Reject。将来local login採用時の安全条件とPhase 2 traceabilityを失う |

### 9.2 P2-B2 implementation boundary

P2-B2で実装するpassword管理は、local credentialを採用する場合の初期設定 / 管理者変更、Spring `PasswordEncoder`、
policy検査、credential lockまでとする。管理者変更後の全Session失効は`UserSessionInvalidator`へ接続する。

P2-B2で追加しないものは次のとおりである。

- `PasswordResetService`、`PasswordResetDeliveryPort`その他reset専用Public API
- `koiki_password_reset` tableと`koiki.identity.password-reset.*` property
- raw reset token生成、mail / SMS送信、reset request / completion endpoint
- reset delivery retry、outbox、通知provider、template
- SSO / IdP reset画面へのredirectを行うFramework共通Controller

SSO applicationがpassword recovery導線を必要とする場合は、Customer / Reference UIが設定済みIdPの公式導線を提示する。
KOIKIはIdP token、temporary passwordまたはreset結果をDBへ同期しない。

### 9.3 Future activation criteria and invariant

将来、KOIKI local passwordのself-service reset要件が成立した場合は、別CPでPublic API、delivery Ownership、migration、
generic response、Audit failure、Session失効および運用を再reviewする。その際も次を最低条件とする。

1. `SecureRandom` 32 bytes以上のtokenを生成し、DBにはSHA-256等のone-way digestだけを保存する。
2. 短いexpiry、userごとの単一有効token、再発行時revoke、atomicなone-time consumeを保証する。
3. known / unknown / disabled / SSO-onlyでaccount存在有無をresponse、timing、log、Auditから露出しない。
4. raw tokenはdelivery境界にだけ一時的に渡し、DB、log、Audit、exception、response、reportへ保存しない。
5. credential変更と全KOIKI Session失効を同じ安全な結果として扱い、失効未完了を成功にしない。
6. mail deliveryをFramework内へ固定せず、Customer-owned adapterまたは将来の通知基盤との境界を承認する。
7. Security audit failure時はtoken発行 / reset完了をfail closedとする。

このdeferは「安全性を簡略化したreset」を許可する判断ではなく、不要なreset機能そのものを初期配布物へ入れない判断である。

## 10. B2-C8 — External identity link

| Candidate | Evaluation |
|---|---|
| email claim auto-link / JIT | Reject。account takeoverと意図しないRole付与を招く |
| provider別user table | Reject。OIDC / EdgeごとにschemaとAPIが分裂する |
| **verified issuer + subject link** | **Recommended.** 認証protocolとFramework userを安定keyで接続する |

- `issuer`と`subject`は検証済みadapterからだけ受け、case-sensitiveなopaque stringとして保存する。OIDCでは検証済みの
  `iss`を完全一致のまま使用し、lowercase、末尾slash除去、URL decode等の独自正規化を行わない。
- `(issuer, subject)`をglobal unique、`(user_id, issuer)`もuniqueとし、同一issuerの複数accountを1 userへ結び付けない。
- email、display name、group、claim全文をlink tableへ保存しない。
- link / unlinkは`IdentityAdministration`経由だけとし、Business auditと同じtransactionで成功またはrollbackする。
- unlinkは対象link経由の新規loginを即時拒否し、P2-B3の`UserSessionInvalidator`で対象userの全Sessionを失効する。
- 管理者によるunlinkは、侵害されたlinkを確実に切断できるよう、最後の認証手段であっても許可する。認証手段を持たない
  `ACTIVE` userは再link待ちまたはaccess遮断状態として許容し、DBで認証手段1件以上を強制しない。
- self-service unlinkはP2-B2で提供しない。将来要件が成立した場合は、別の有効な認証手段、直近の再認証、Auditおよび
  Session失効を別CPでreviewする。
- unknown issuer / subject、disabled user、unique競合はsafe failureとし、User / link rowを自動作成しない。

## 11. B2-C9 — Identity tables and migration boundary

### 11.1 Recommended physical inventory

| Table | Principal columns / constraints | Ownership note |
|---|---|---|
| `koiki_user` | `user_id uuid PK`、`email varchar(320)`、`canonical_email varchar(320) UNIQUE`、`status varchar(16)`、`version bigint`、timestamps | emailは正規のIdentity PII。Audit / logへ複製しない |
| `koiki_role` | `role_id uuid PK`、`role_code varchar(100) UNIQUE`、`version bigint`、timestamps | Framework authorization definition |
| `koiki_permission` | `permission_id uuid PK`、`permission_code varchar(100) UNIQUE`、`version bigint`、timestamps | backend authority definition |
| `koiki_user_role` | `(user_id, role_id) PK`、両FK | direct user-permission tableは作らない |
| `koiki_role_permission` | `(role_id, permission_id) PK`、両FK | RoleをPermission集合として固定 |
| `koiki_password_credential` | `user_id uuid PK/FK`、`encoded_password varchar(512)`、`locked_until timestamptz`、`version bigint`、timestamps | raw password、salt別列なし |
| `koiki_login_attempt` | `attempt_id uuid PK`、`scope`、`user_id nullable`、`source_key_id nullable`、`source_fingerprint bytea nullable`、window / count / blocked timestamps | CHECKでACCOUNT / SOURCE列の排他、partial unique indexで各keyを一意化 |
| `koiki_external_identity_link` | `link_id uuid PK`、`user_id FK`、`issuer varchar(2048)`、`subject varchar(255)`、`version bigint`、timestamps | `(issuer, subject)`と`(user_id, issuer)`をUNIQUE |

全timestampは`timestamp with time zone`、application ClockはUTC、精度はmicrosecondとする。全FKのdelete actionは原則`RESTRICT`とし、
attempt等のlifecycle rowだけ明示cleanupで削除する。User hard delete APIはPhase 2で提供せず、`DISABLED`を利用する。
P2-B2では期限切れattempt rowを認証判定から除外し、物理cleanupのsingle executionはP2-B3へ接続する。

### 11.2 Migration placement comparison

| Candidate | Evaluation |
|---|---|
| B2はfixture DDLだけ、全production DDLをC1 | Reject。Identity schemaとimplementationが別CPになり、P2-B2のclean installをclaimできない |
| **Identity DDLをB2、aggregate migrationをC1** | **Recommended.** owner moduleと同時にproduction schemaを固定し、C1でAudit / Sessionを加えsupported upgradeを総合確認できる |
| Audit / Session DDLもB2へ前倒し | Reject。P2-B3 / C1の未承認schemaを先行固定する |

Identity Starter内の`classpath:db/migration/koiki/V2026090301__create_koiki_identity.sql`を初回候補とする。
同じ`koiki_flyway_history`を使うFramework migrationはrepository全体で日付 + 2桁sequenceを一意管理し、release後のfile変更や
version再利用を禁止する。P2-B2では次を実PostgreSQLで確認する。

1. clean databaseへの適用と再起動時のno-op。
2. canonical email、role / permission code、external issuer + subjectの重複拒否。
3. FK、CHECK、partial unique index、version初期値、timestamp精度。
4. migration JARだけがFramework locationへ入り、Reference / Customer migrationを含まないこと。

P2-C1は`koiki_audit_event`、Spring Session table、全Framework migration順序、旧versionからのsupported upgrade、clean install、
第三者table inventoryを総合する。B2で未来のAudit / Session tableを空生成しない。

## 12. B2-C10 — Session handoff

### 12.1 Comparison

| Candidate | Evaluation |
|---|---|
| B2ではno-op invalidator | Reject。disable / password変更 / Permission変更を成功表示してstale Sessionを残す |
| Spring Session RepositoryへIdentityが直接依存 | Reject。P2-B3のartifact / tableを先行し、in-memory等の別Session構成も排除する |
| Domain eventを非同期publish | Reject。失効完了前にIdentity変更がcommitし得る |
| **同期`UserSessionInvalidator` SPI** | **Recommended.** B2で必須効果を契約化し、B3がSpring Session JDBC adapterを提供できる |

`disable`、password change、Role / Permission変更、external unlinkは、`UserSessionInvalidator.invalidateAll(userId)`が
成功しなければmutationをcommitしない。例外はsafeな`DEPENDENCY_FAILURE`へ変換し、失効未完了を成功扱いしない。

ただし、Audit storeだけが失敗したaccount disableではO-4に従いdisableとSession失効を完了してalertする。Session store障害では
R-01に従ってdisable自体をrollback / safe failureとする。この2つを同じbest-effort catchへまとめない。

P2-B2 fixtureはtransaction参加を観測できるTooling-owned invalidatorを提供し、次だけをclaimする。

- invalidatorが呼ばれる操作と対象user ID。
- invalidator failure時にIdentity mutationをcommitしないこと。
- no-op implementationがproduction auto-configurationに存在しないこと。

実Spring Session row削除、2 process、旧Cookie再利用拒否、cleanup / single executionはP2-B3で初めてclaimする。B3完成前は
`UserSessionInvalidator` beanがない構成で失効必須操作を有効化せず、startupまたはbean creation時に明示失敗させる。

## 13. Audit classification and failure matrix

既存文書の「administrator password / account操作はBusiness audit」とO-4の「disableは監査失敗でも継続、管理解除はfail closed」を
同時に満たすため、保護制御そのものの変更を次のように具体化する。

| Operation | Audit | Audit failure | Session failure |
|---|---|---|---|
| create user / email change | Business / same transaction | mutation rollback | Session失効なし。principalはuser ID / Permissionを正本とする |
| Role / Permission change | Business / same transaction | mutation rollback | mutation rollback |
| external link / unlink | Business / same transaction | mutation rollback | unlinkはmutation rollback |
| admin password設定 | Business / same transaction | mutation rollback | mutation rollback |
| login success | Security / `REQUIRES_NEW` | authentication fail closed | Session未作成 |
| login failure / account lock | Security / `REQUIRES_NEW` | rejectionと防御状態を維持 + alert | 該当なし |
| future local reset request / completion | Security / `REQUIRES_NEW` | issuance / credential変更をfail closed | B2では未実装。将来CPで再review |
| account disable | Security / `REQUIRES_NEW` | disable + invalidation継続 + alert | disable rollback / safe failure |
| management unlock | Security / `REQUIRES_NEW` | unlockしない | 該当なし |

この細分化では、account disable / unlockを一般の「administrator account操作」ではなくSecurity protection controlとして扱う。
Owner承認後に`phase2-security-semantics-fitting.md`の広い表現をこのmatrixへ整合させる。二重にBusiness / Security auditを記録する
既定にはせず、1操作の正本event分類を一つにする。

## 14. T4 evidence required before P2-B2 acceptance

1. Public API inventoryが承認済み型だけで、Spring / JPA / encoded passwordを露出しない。
2. clean migration、constraint、index、version、Flyway ownershipが§11と一致する。
3. canonical email collisionをDBでも拒否し、email変更後もuser ID、attempt、Role、Audit actorが変わらない。
4. persistent UserでForm Loginが成立し、unknown / bad password / disabled / lockedの外部response / Cookieが同一である。
5. 2 instance相当の並行失敗でaccount thresholdを迂回せず、lost updateなしに1回だけlockへ遷移する。
6. raw unknown email / IPを保存せず、`APPLICATION`では同じsource HMACが同じbucket、key変更後は別bucketとなる。
   `EXTERNAL`ではSOURCE rowを作らずACCOUNT保護を維持する。
7. password policy、delegating hash、legacy match後upgrade、credential消去を確認する。
8. reset専用Public API、property、table、endpoint、delivery adapterがproduction artifactへ入っていないことをinventoryで確認する。
9. external issuer + subject競合、issuer完全一致と独自正規化の禁止、email auto-link拒否、最後の認証手段の管理unlink、
   Business audit rollbackを確認する。
10. Audit / invalidator failureが§13どおりで、Session失効未実装を成功claimしない。
11. row、Application log、Audit、response、Surefire report、JARをsecret / PII patternで走査する。
12. T0〜T4 aggregate、root verify、Null Safety、ArchUnit、cleanupが成功する。

## 15. Decisions requested

| ID | Recommended choice | Material alternative / impact | Status |
|---|---|---|---|
| B2-C1 | optionalな単一`koiki-starter-identity` | Security Starter混在はOIDC / Bearer専用構成へDBを強制 | **APPROVED** |
| B2-C2 | 10型のuse-case API / value / SPI、Spring / JPA型非公開、reset専用型なし | Entity / Repository公開はOwnership越境。使用前のreset API固定も避ける | **APPROVED** |
| B2-C3 | User `ACTIVE/DISABLED`、local credential lock、Role→Permission、external linkを分離 | `LOCKED` User status、direct user Permission、email auto-linkは不採用 | **APPROVED** |
| B2-C4 | Delegating encoder、15〜128 code points、compositionなし、checker必須 | 独自hash、NoOp、定期変更、checker skipは不採用 | **APPROVED** |
| B2-C5 | known user ID + unknown email非保存。SOURCEは`APPLICATION`既定、承認済み公開境界へ移す`EXTERNAL`を明示選択可 | raw / plain hash PIIと暗黙disableは不採用。unknown email HMACは要件成立までdefer | **APPROVED** |
| B2-C6 | account 5 / 15m / lock 30m、source 100 / 15m / block 15m、atomic upsert | read-modify-write / optimistic retryだけではcounter欠落risk | **APPROVED** |
| B2-C7 | 初期SSO前提ではlocal resetを実装せず、安全条件と将来activation criteriaだけを保持 | B2でのtoken / mail / table先行実装、IdP reset複製は不採用 | **APPROVED** |
| B2-C8 | verified issuer + subject、2 unique制約、明示link / unlink | email auto-link / JITは不採用 | **APPROVED** |
| B2-C9 | B2でIdentity migration、C1でAudit / Session / aggregate upgrade | 全DDLのB2前倒し、Identity DDLのC1延期は不採用 | **APPROVED** |
| B2-C10 | 同期`UserSessionInvalidator`、no-opなし、実Session evidenceはB3 | direct Spring Session依存、非同期event、no-opは不採用 | **APPROVED** |

2026年9月3日、Architecture Ownerは本文を一通りreviewし、B2-C1〜C10を推奨案どおり承認した。これによりB2-1を完了し、
B2-2 Identity core / migrationへ進める。後続の実PostgreSQL Evidenceが承認案を否定した場合も、実装を既成事実化せず
本reviewへ戻り、該当decisionだけを再reviewする。

本承認はP2-B2実装完了、Spring Session JDBCまたは実Session全失効の成立を意味しない。B2-2〜B2-5のEvidenceとOwner acceptance後に
P2-B2をcloseし、P2-B3の実Session Evidence後にB2-C10のadapter実装を確定する。ADR候補化もP2-B2 acceptanceまで行わない。

2026年9月7日、Architecture OwnerはB2-3で具体化したlocal認証opt-in、SOURCE `APPLICATION / EXTERNAL`責務分離、
generic failure、attempt / lockおよびSecurity Audit failure semanticsをreviewし、B2-C4〜C6の設計判断として承認した。
同日、SOURCE遮断DB読取障害のgeneric failure補正後を含むT0〜T4 aggregate 48 / 48とroot Reactor 13 / 13を確認し、
B2-3の実装を最終承認した。
