# koiki-starters

KOIKI Spring Boot StarterのCanonical ownership locationです。

Runtime Starterは所定Phaseの検証でOwnershipとPublic APIを確定してから追加し、空Maven Moduleを
先行作成しません。Phase 2では、P2-A1で`koiki-starter-security`、P2-B1で`koiki-starter-audit`を
Architecture Ownerのcontract承認後に正式Starterとして追加しています。P2-B2では
`koiki-starter-identity`、P2-B3ではSession利用applicationだけが導入するoptionalな
`koiki-starter-session-jdbc`を同じ手順で追加しています。

## Phase 2 selection guide

| Application need | Starter | Boundary |
|---|---|---|
| Servlet default deny、OIDC Client、Bearer Resource Server | `koiki-starter-security` | Customerがroute matcher、認証profile、Role / Permissionを所有 |
| Business / Security Audit | `koiki-starter-audit` | DBを正本とし、transaction分類を混在させない |
| local Identity、Role / Permission、認証試行 / lock | `koiki-starter-identity` | Customer属性、reset delivery、provisioningは含まない |
| server-side Session、全Session失効、期限切れcleanup | `koiki-starter-session-jdbc` | Sessionを利用するApplicationだけが導入 |

`koiki-starter-session-jdbc`はIdentity / Audit / Security / DataのKOIKI依存を推移的に含む。Bearer-only APIへSessionを
追加しない。WebMVC、PostgreSQL driver、Flyway PostgreSQL module等のApplication dependencyはCustomerが用途に応じて選ぶ。

導入順、profile選択、Ownership、診断と検証は
[Phase 2 Developer Journey](../docs/development/phase2-developer-journey.md)を参照する。
