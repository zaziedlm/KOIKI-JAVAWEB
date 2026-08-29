# KOIKI Data Starter

Persistence technologyに依存せず、同一DataSource上でKOIKI FrameworkとCustomer Applicationの
Flyway履歴を分離し、KOIKI migrationを先に実行する。

- KOIKI location: `classpath:db/migration/koiki`
- KOIKI history: `koiki_flyway_history`
- Customer location: `classpath:db/migration/customer`
- Customer history: `flyway_schema_history`

Starter自身は業務SQLや架空のFramework tableを同梱しない。CustomerはDB driverとFlywayの
database-specific moduleを選択し、Customer migrationを自身のartifactで所有する。
