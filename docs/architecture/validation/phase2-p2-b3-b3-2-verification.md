# Phase 2 P2-B3 B3-2 Session core / migration verification

## 1. Status and scope

- **Verification date:** 2026年9月8日
- **Work package:** `P2-B3 / B3-2`
- **Status:** `COMPLETE / ARCHITECTURE OWNER APPROVED`
- **Approved by:** Shuichi Kataoka、2026年9月8日
- **Ownership:** Framework（Session JDBC Starter / migration）+ Tooling（T0 / T5 fixture）
- **Baseline:** B3-C1〜C10 Architecture Owner承認済み

本記録は、optional Session JDBC Starter、Framework所有の2 table migration、Spring Session設定境界および
Session属性の永続化を実PostgreSQLで検証したB3-2の証拠である。全Session失効 / logout、2 process継続、
maintenance cleanup / single executionは、それぞれB3-3、B3-4、B3-5へ残し、本記録では成立をclaimしない。

## 2. Implemented artifacts

### 2.1 Artifact and dependency boundary

- 新規optional artifact `koiki-starter-session-jdbc`がSpring Session JDBCとSession migrationを所有する。
- 直接依存は`koiki-starter-identity`、`koiki-starter-data`、`spring-boot-starter-session-jdbc`、`jspecify`である。
- Root ReactorとBOMへ正式artifactとして追加した。非配布verification fixtureはrelease repositoryへinstallしない。
- Redis Session、SAML、WebFlux、AWS SDKを追加していない。
- `org.koikifw.session`直下のPublic Java型は0件で、Spring repository型をKOIKI APIとして再公開していない。

### 2.2 Framework migration

`V2026090701__create_koiki_session.sql`は次の2 tableだけを作成する。

1. `koiki_session`
2. `koiki_session_attributes`

列構成はSpring Session 4.1.1のPostgreSQL schemaへ適合させ、Framework接頭辞、constraint名およびindex名を
`koiki_`へ変更した。`session_id` unique、`expiry_time`、`principal_name`のindexを持ち、属性rowはSession削除時に
外部キーの`ON DELETE CASCADE`で削除される。Spring標準`SPRING_SESSION*` tableを併設しない。

### 2.3 Spring configuration boundary

Starterは低優先度の既定値として次を供給する。

| Property | Value / role |
|---|---|
| `spring.session.jdbc.initialize-schema` | `never`。DDL正本をFramework Flywayだけにする |
| `spring.session.jdbc.table-name` | `koiki_session` |
| `spring.session.jdbc.cleanup-cron` | `-`。Web processのcleanupを停止する |
| `spring.session.jdbc.flush-mode` | `on-save` |
| `spring.session.jdbc.save-mode` | `on-set-attribute` |
| `server.servlet.session.cookie.http-only` | `true` |
| `server.servlet.session.cookie.secure` | `true`。loopback fixture等は明示override可能 |
| `server.servlet.session.cookie.same-site` | `lax` |
| `spring.session.timeout` | Spring標準の案件設定点としてinventoryへ記録 |

Customer設定を低優先度既定値で上書きしない。一方、initializer、table名、Web cleanup、HttpOnlyを承認値以外へ
変更した場合はstartup guardでFail Fastする。Secureはproduction既定を`true`としつつ、HTTP loopback fixtureを許すため
固定guardには含めない。

ここでいうproductionのHTTPS前提は、containerへの直接接続がHTTPSであることではなく、browserからALB等の
外部入口までがHTTPSであることを指す。外部入口でTLSを終端し、入口からcontainerまでHTTPで転送する構成でも、
browserがCookieをHTTPSでだけ送信するよう`Secure=true`を維持する。ALB等が付与する`X-Forwarded-Proto`を
Springへ反映する`server.forward-headers-strategy`はApplication / deployment assemblyの設定とし、本Starterの
固定propertyには加えない。転送headerは信頼されたproxy経由に限定し、containerへの直接到達を許可しないことを
deployment条件とする。

### 2.4 Flush / save and serialization boundary

`ON_SAVE` / `ON_SET_ATTRIBUTE`を候補ではなくB3-2の採用値として実測確定した。

- `createSession()`後、`save()`前はDB rowを作成しない。
- 保存済みSessionを読み取って再保存しても、未変更の属性rowを書き換えない。
- 属性を変更して保存した場合だけ属性rowを更新する。

Spring SecurityがSessionへ保存する実principalを永続化できるよう、immutable `FrameworkUserId`とinternal
`KoikiIdentityUserDetails`をSerializableにした。password fieldは`transient`とし、deserialization時も空値へ戻すため、
credentialをSession属性へ保存しない。Customer object、JPA Entity、tokenを許可したという意味ではなく、追加属性の
portable / secret-free境界は導入案件側の設計・検証事項として残る。

## 3. Verification results

### 3.1 Auto Configuration context — 3 / 3

`SessionJdbcAutoConfigurationContextTest`で次を確認した。

1. 未指定時に承認済み8既定値が設定される。
2. Customer指定値はEnvironment既定値に上書きされない。
3. 承認値では起動し、initializer、table名、cleanup、HttpOnlyの各不正値では安全な一般化messageで起動を拒否する。

### 3.2 PostgreSQL Session core / migration — 3 / 3

`SessionJdbcCoreMigrationFixtureTest`で次を確認した。

1. Identity / Auditとの累積migration上でSession 2 tableと4 indexが作成され、`SPRING_SESSION*`は存在せず、
   Flyway再実行のpending migrationが0件である。
2. 実repositoryに対し`ON_SAVE` / `ON_SET_ATTRIBUTE`のDB write境界をrow数とPostgreSQL `xmin`で観測した。
3. 実local認証principalをSessionへ保存・復元し、`principal_name`がemailでなくimmutable Framework user ID、
   復元principalのuser IDが一致し、passwordが空であることを観測した。さらにSession属性`bytea`内を検索し、
   入力passwordのUTF-8 byte列が0件であることを確認した。

### 3.3 Cumulative regression and artifact inspection

```text
Formal release staging: Reactor 14 / 14 SUCCESS
Security / Audit / Identity / Session fixture: 15 suites, 62 tests
Failures: 0, Errors: 0, Skipped: 0
Session context: 3 / 3
Session PostgreSQL core / migration: 3 / 3
Session inventory / dependency / sensitive-content inspection: SUCCESS
Temporary isolated repository cleanup: SUCCESS
```

`verify-p2-b3-session-core.ps1`は正式release unitを隔離Maven repositoryへstageし、次を一括検査する。

- T0〜T5 62 / 62と既存Security / Audit / Identity回帰
- Session inventory、Public Java型0件、production migration 2 table、Auto Configuration import
- 必須dependencyとRedis / SAML / WebFlux / AWS SDK非混入
- fixture非配布
- 正式Session JAR、Maven log、全Surefire reportのsecret / PII pattern scan
- GUID付き一時repositoryの`finally` cleanup

Root回帰はworkspace内の隔離Maven repositoryを使用して再実行し、次を確認した。

```text
Root clean verify: Reactor 14 / 14 SUCCESS
Architecture Contract: 4 / 4
ArchUnit: 66 / 66
Error Prone / NullAway compilation: SUCCESS
```

通常のユーザーMaven repositoryを使う初回root実行は、sandboxから`.m2`へのアクセス拒否でSession Starterの依存解決前に
停止した。製品・テスト失敗ではなく、同一commandをworkspace内の隔離repositoryへ切り替えると成功した。

## 4. Explicitly deferred work

| Work package | Remaining evidence |
|---|---|
| B3-3 | `UserSessionInvalidator` JDBC adapter、principal index全Session失効、旧Cookie拒否、logout / store障害 |
| B3-4 | package済み同一JARの2 process Session継続、片系停止、別processからの失効 |
| B3-5 | non-web cleanup、PostgreSQL internal排他、acquired / contended / failed、crash recovery |
| B3-6 | T0〜T6 closeout、3回連続安定性、最終inventory / cleanup evidence |

B3-2では将来利用されるだけのinvalidator adapterやcleanup Public APIを先行生成していない。
B3-4またはB3-6では、production相当のproxy転送条件で`X-Forwarded-Proto=https`を認識し、外部応答のSession Cookieに
`Secure`が付くことを確認する。B3-4の直接HTTP loopbackで`Secure=false`を使う場合はtest scopeだけに限定する。

## 5. Architecture Owner review points

1. optional StarterがSession JDBCと2 table migrationを所有し、Identity / SecurityへSession dependencyを逆流させない境界が妥当か。
2. initializer `never`、`koiki_session`、Web cleanup無効、HttpOnlyをFail Fast固定し、Secureは外部TLS終端構成でも既定true、test scopeでは明示override可能とする判断が妥当か。
3. `ON_SAVE` / `ON_SET_ATTRIBUTE`を実DB write観測後の採用値として確定してよいか。
4. Framework principalの永続化に必要な2型だけをSerializableとし、passwordをtransientかつ復元時空値にする境界が妥当か。
5. B3-2のclaimをschema / repository / principal serializationまでに限定し、HTTP失効、2 process、cleanupを後続CPへ残しているか。

推奨判断は、上記5点を承認してB3-2を`COMPLETE / ARCHITECTURE OWNER APPROVED`とし、B3-3
Invalidation / logoutへ進むことである。

2026年9月8日、Architecture Ownerは上記5点をreviewした。外部TLS終端時もbrowser向けCookieの`Secure=true`を
維持し、forwarded header処理をApplication / deployment assemblyの責務とする補足、およびB3-4 / B3-6での
外部観測条件を確認した。5点を承認し、B3-2を`COMPLETE / ARCHITECTURE OWNER APPROVED`とする。
次はB3-3 Invalidation / logoutへ進む。
