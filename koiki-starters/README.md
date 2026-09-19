# koiki-starters

KOIKI Spring Boot StarterのCanonical ownership locationです。

Runtime Starterは所定Phaseの検証でOwnershipとPublic APIを確定してから追加し、空Maven Moduleを
先行作成しません。現行formal release unit候補には、Phase 1bのAPI / Data / Data JPA / Observability、
Phase 2のSecurity / Audit / Identity / Session JDBC、Phase 3のWeb MVCという9 Starterがあります。
すべてを一括導入する前提ではなく、Applicationの構成とOwnershipに応じて選択します。

## 現行Starter selection guide

| Application need | Starter | Boundary |
|---|---|---|
| Servlet REST、Validation、Problem Details、JSON、API versioning | `koiki-starter-api` | 業務DTO、route、例外mappingおよび認可はApplicationが所有 |
| KOIKI / Customer Flyway履歴の分離 | `koiki-starter-data` | 現行検証済みbaselineとしてFlyway PostgreSQL moduleを推移提供。DB固有moduleの選択、DB driver、Customer migrationはApplicationが所有 |
| JPAとOSIV無効の安全な既定 | `koiki-starter-data-jpa` | 業務Entity / Repositoryを含めず、persistence-neutralなData Starterと分離 |
| structured log、request correlation、health | `koiki-starter-observability` | exporter、cloud backend、業務logおよびreadiness構成はApplicationが所有 |
| Servlet default deny、OIDC Client、Bearer Resource Server | `koiki-starter-security` | Customerがroute matcher、認証profile、Role / Permissionを所有 |
| Business / Security Audit | `koiki-starter-audit` | DBを正本とし、transaction分類を混在させない |
| local Identity、Role / Permission、認証試行 / lock | `koiki-starter-identity` | Customer属性、reset delivery、provisioningは含まない |
| server-side Session、全Session失効、期限切れcleanup | `koiki-starter-session-jdbc` | Sessionを利用するApplicationだけが導入 |
| Servlet MVC、Thymeleaf、Bean Validation、共通Web resource | `koiki-starter-web-mvc` | Security、Identity、Session、業務route、Form、View DTOを自動構成しない |

`koiki-starter-session-jdbc`はIdentity / Audit / Security / DataのKOIKI依存を推移的に含みます。Bearer-only APIへSessionを
追加しません。Applicationは用途に応じてKOIKI Starterを選び、API / Web MVC StarterからSpring Web MVC、Data Starterから
現行検証済みのFlyway PostgreSQL moduleを推移的に利用します。DB固有moduleの選択責任、PostgreSQL JDBC driverおよび
Customer migrationはApplicationが所有します。現行Consumerでは、このOwnershipをPOM上で明示するため同じPostgreSQL moduleを
直接宣言しています。別DBへの変更は、依存を追加するだけで回避せずblocking reviewで扱います。

全体の選び方は[アプリ開発チーム向け引継ぎガイド](../docs/development/application-team-handoff-guide.md)、
Security profileの詳細は[Phase 2 Developer Journey](../docs/development/phase2-developer-journey.md)、
UI / SSO構成の比較は
[UI / Authentication Profile Selection Guide](../docs/development/frontend-authentication-profile-guide.md)を参照する。
