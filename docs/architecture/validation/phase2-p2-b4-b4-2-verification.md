# Phase 2 P2-B4 B4-2 Reference skeleton / protected read verification

## 1. Status and scope

- **Verification date:** 2026年9月9日
- **Work package:** `P2-B4 / B4-2`
- **Status:** `COMPLETE / ARCHITECTURE OWNER APPROVED — B4-3 NOT STARTED`
- **Ownership:** Reference（executable application / `identity` MVC / Application Use Case）
- **Baseline:** B4-C1〜C8 Architecture Owner承認済み

本記録は、正式なReference executable skeleton、Tier 1 `identity` module、単一user参照、Reference-owned DTO、
URL認可およびApplication Use CaseのMethod Securityを実装・検証したB4-2の証拠である。package済みprocessと実PostgreSQLを
使用したprotected readは本記録で確認済みとする。Role付与 / 剥奪とCSRFを伴うmutationはB4-3へ、そのmutationを含む正式な
AC-P2-01 / 02のnetwork越しjourneyはB4-4へ残し、本記録では成立をclaimしない。

## 2. Implemented artifacts

### 2.1 Reference executable and distribution boundary

- `org.koikifw:koiki-reference-app:0.1.0-SNAPSHOT`をtop-level Maven moduleとして追加した。
- Root Reactorの通常検証にはReferenceを含め、15 projectとした。
- ReferenceはBOMへ追加せず、Framework formal release unitは従来の14 projectを維持した。
- B3の3つの隔離staging Harnessは`-pl !koiki-reference-app`でformal release unitだけを選択するよう更新した。
- Spring Boot Maven Pluginでexecutable JARを生成し、Reference classと2つのThymeleaf templateを格納した。

Referenceの直接production dependencyは次に限定した。

| Dependency | Purpose |
|---|---|
| `koiki-architecture-contract` | `@KoikiModule` metadata |
| `koiki-starter-session-jdbc` | 承認済みSession / Identity / Security / Audit / Data構成 |
| `spring-boot-starter-webmvc` | server-rendered MVC |
| `spring-boot-starter-thymeleaf` | HTML view |
| Flyway PostgreSQL / PostgreSQL driver | 後続の実PostgreSQL実行時構成 |
| `jspecify` | Null Safety annotation |

当初候補の`koiki-starter-api`は、実装試験でREST向けpath-segment API versioningを`/identity/users`にも適用し、
`identity`を不正なversionとして400にすることを観測した。このためB4-C1のserver-rendered MVC境界に従い、Spring Boot標準
`spring-boot-starter-webmvc`へ補正した。`koiki-starter-api`はPhase 3 REST APIまで延期する。

### 2.2 Tier 1 identity module

`org.koikifw.reference.identity`を次のmetadataで宣言した。

```text
name = identity
tier = SIMPLE
persistence = JPA
persistenceModel = SHARED
```

実装は必要な3責務だけに限定した。

```text
identity
├── adapter.inbound.web    IdentityManagementController
├── application            IdentityUserManagement / IdentityUserView
└── configuration          ReferenceSecurityConfiguration
```

Reference-owned Domain Model、Repository、JPA Entity、migrationおよび将来moduleの空packageは追加していない。

### 2.3 Protected single-user read

- `GET /identity/users`はuser ID入力formだけを表示し、user一覧を提供しない。
- `GET /identity/users?userId=...`はcanonical detail URLへredirectする。
- `GET /identity/users/{userId}`は`IdentityQuery#findById`だけを呼び、単一user detailを表示する。
- malformed user IDは400、存在しないuserは404とし、parser / persistence詳細をresponseへ渡さない。
- Framework `IdentityUser`はMVC Modelへ渡さず、user ID、email、status、sort済みRole / Permission、versionだけを持つ
  immutable `IdentityUserView`へApplication Use Case内でcopyする。

Identity Public API、Identity tableおよびconfiguration propertyは変更していない。

### 2.4 URL and Method Security

- `/identity/**`はURL boundaryで`IDENTITY:ADMIN`を要求する。
- `/login`はSpring Security form login entry pointとしてpermitし、logoutはB3のSession customizerを適用する。
- ログイン成功時は`/identity/users`へ固定redirectし、browser補助requestがRequest Cacheへ入っても管理外pathへ遷移しない。
- `IdentityUserManagement`はclass-level `@PreAuthorize("hasAuthority('IDENTITY:ADMIN')")`で保護する。
- 権限不足またはanonymousの直接Use Case呼出しでは、Framework `IdentityQuery`を呼ぶ前に拒否する。
- Framework Security Starterのfallback default-denyと既存Method Securityを利用し、Referenceで独自認可抽象や
  `@EnableMethodSecurity` production構成を重複追加していない。

## 3. Verification results

### 3.1 Reference tests — 16 / 16

| Test | Count | Evidence |
|---|---:|---|
| `IdentityUserManagementTest` | 2 | immutable / sorted DTO変換、not found保持 |
| `IdentityUserManagementMethodSecurityTest` | 3 | admin許可、権限不足 / anonymousをquery前に拒否 |
| `IdentityManagementControllerTest` | 5 | form、canonical redirect、detail、malformed 400、not found 404 |
| `IdentityUrlSecurityTest` | 4 | 未認証login redirect、権限不足403、admin 200、login後の固定安全遷移 |
| `ReferenceArchitectureTest` | 2 | Tier 1 business module rule、Framework ownership rule |

```text
Tests: 16
Failures: 0
Errors: 0
Skipped: 0
```

### 3.2 Root and formal release boundary

```text
Root clean verify: Reactor 15 / 15 SUCCESS
Root Reactor Surefire tests: 86 / 86 SUCCESS
Architecture Contract: 4 / 4 SUCCESS
ArchUnit rule library: 66 / 66 SUCCESS
Reference tests: 16 / 16 SUCCESS
Error Prone / NullAway compilation: SUCCESS

Formal release project selection validate: Reactor 14 / 14 SUCCESS
Reference included in formal release selection: 0
```

### 3.3 Artifact and inventory inspection

| Boundary | Observed result |
|---|---:|
| Reference executable JAR | 1 |
| Reference production classes | 10（package-infoを含む） |
| Reference templates | 2 |
| Reference controller mappings | 3（B4-2 GETだけ） |
| Reference migration / SQL | 0 |
| Reference import of Framework `internal` package | 0 |
| Identity Public Java types | 10（変更なし） |
| `IdentityQuery` methods | 1（変更なし） |
| BOM entry for Reference | 0 |
| `koiki-starter-api` / WebFlux / Redis / SAML / AWS SDK / Oracle in executable JAR | 0 |
| committed credential / key value | 0（HMAC keyは環境変数placeholderだけ） |

Executable JAR内のKOIKI dependencyはArchitecture Contract、Session JDBC、Identity、Security、Data JPA、Audit、Dataの
承認済み連鎖だけである。Reference-owned migrationをJARへ含めず、Framework migrationはowner Starterのnested JAR内に維持する。

### 3.4 Manual browser confirmation

2026年9月9日、Architecture Ownerがpackage済みReference JAR、loopback HTTPおよび一時PostgreSQLを使用し、
Chromeのシークレットウィンドウから次を手動確認した。

1. Spring Security form loginが成功し、`/identity/users`へ遷移する。
2. 単一user ID入力formが表示され、入力値を送信できる。
3. canonical detail URLへredirectする。
4. detail viewに対象userのID、status、version、RoleおよびPermissionが表示される。

最初の試行ではChrome DevToolsの補助requestがRequest Cacheへ入り、認証成功後にdefault-deny対象の
`/.well-known/appspecific/com.chrome.devtools.json`へ遷移した。認証やSession保存の失敗ではなかったが、Reference UIの
安定した成功遷移として不適切なため、form login成功時の遷移先を`/identity/users`へ固定した。回帰testを追加し、
実HTTPと再度のmanual browser操作の双方で期待結果を確認した。test credential、emailおよびCookie値は本記録へ保存しない。

### 3.5 Post-journey database inspection

manual browser確認後、コンテナ削除前のPostgreSQLをread-only SQLで点検した。

| Inspection | Observed result |
|---|---|
| Framework migration | Identity / Sessionの2件が成功 |
| Tooling migration | Audit fixtureとbaselineが成功 |
| Identity user | admin / targetの2件。ともに`ACTIVE`、version `0` |
| Role / Permission | adminは`IDENTITY_ADMIN` / `IDENTITY:ADMIN`、targetは`EXPENSE_READER` / `EXPENSE:READ` |
| Credential | 1件。Spring delegating bcrypt形式、raw password一致0件 |
| Login attempt | 0件 |
| Security Audit | `IDENTITY_LOGIN / LOGIN / SUCCESS`が6件。失敗0件、actor / subject不一致0件、email形式値0件 |
| Session | 7件。認証済みadmin 6件、login前anonymous 1件、想定外principal 0件、期限切れ0件 |
| Session attributes | Security Context 6件、Saved Request 1件、CSRF token 2件。raw password byte一致0件 |
| Orphan records | user-role、role-permission、credential、session attributeの全て0件 |

複数の認証済みSessionとSecurity Auditは、自動HTTP確認、初回browser試行および修正後browser再確認で同じ一時adminへ
複数回loginした結果と一致する。anonymous Session 1件はlogin前のCSRF / Saved Request用であり、認証principalを持たない。

## 4. Explicitly deferred work

| Work package | Remaining work |
|---|---|
| B4-3 | Role assign / revoke、POST / CSRF、stale version、safe mutation failure mapping、Audit / Session semantics |
| B4-4 | Tooling-owned bootstrap、実PostgreSQL、package済みprocessへのnetwork越しAC-P2-01 / 02 |
| B4-5 | P2-B1〜B4 closeout、inventory / sensitive scan / cleanup、Gate B handoff |

B4-2では`IdentityAdministration`、Reference mutation form / route、test user / credential、failure switch、bootstrap route、
Reference migrationまたはCI workflowを追加していない。

## 5. Architecture Owner review points

1. ReferenceをRoot Reactorへ参加させつつBOM / Framework formal release unitから分離し、B3 stagingを14 projectへ固定した境界が妥当か。
2. `identity`をTier 1 SIMPLEとしてController / Application / Configurationだけで構成し、Reference persistenceを作らない判断が妥当か。
3. `IdentityQuery#findById`だけを利用し、Framework `IdentityUser`をimmutable Reference DTOへcopyするread boundaryが妥当か。
4. URLとApplication Use Caseの双方で`IDENTITY:ADMIN`を強制し、拒否時にFramework queryを呼ばない構成が妥当か。
5. `koiki-starter-api`をserver-rendered MVCから外した実装Evidenceと、B4-2のclaimをprotected readまでに限定した範囲が妥当か。

### 5.1 Architecture Owner decision

2026年9月9日、Architecture Ownerは上記5点を確認し、すべて承認した。
B4-2を`COMPLETE / ARCHITECTURE OWNER APPROVED`とし、次の実装sliceをB4-3 Role membership mutationとする。
