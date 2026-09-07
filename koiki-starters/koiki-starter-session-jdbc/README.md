# KOIKI Session JDBC Starter

Phase 2 P2-B3のFramework-owned Spring Session JDBC構成を、Sessionを利用するapplication向けに提供する。

- `koiki_session` / `koiki_session_attributes`をFramework Flyway migrationで所有する
- Spring Session JDBCのschema initializerを無効化し、Framework migrationだけをDDLの正本とする
- Spring標準のJDBC repository、`ON_SAVE` / `ON_SET_ATTRIBUTE`および`bytea` serializationを使用する
- table名とWeb process内cleanup無効化を固定し、設定逸脱時はstartup failureとする
- Secure / HttpOnly / SameSite Cookieを安全な既定とする
- Sessionを利用しないBearer専用applicationは本Starterを導入しない

P2-B3 B3-2ではSession coreとmigrationを提供する。全Session失効 / logoutはB3-3、2 process継続はB3-4、
期限切れcleanup / single executionはB3-5で追加・検証する。

Public contractと設定境界の正本は
`docs/architecture/validation/phase2-p2-b3-contract-review.md`のB3-C1〜C10とする。
