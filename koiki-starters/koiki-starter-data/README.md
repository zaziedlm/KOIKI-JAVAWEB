# KOIKI Data Starter

Persistence technologyに依存せず、同一DataSource上でKOIKI FrameworkとCustomer Applicationの
Flyway履歴を分離し、KOIKI migrationを先に実行する。

- KOIKI location: `classpath:db/migration/koiki`
- KOIKI history: `koiki_flyway_history`
- Customer location: `classpath:db/migration/customer`
- Customer history: `flyway_schema_history`

Starter自身は業務SQLや架空のFramework tableを同梱しません。Persistence technologyに対しては中立ですが、
現行の検証済みDB baselineはPostgreSQLであり、`flyway-database-postgresql`を依存として提供します。
DB固有moduleの選択責任、PostgreSQL JDBC driverおよびCustomer migrationはCustomerが所有します。現行Consumerは
このOwnershipを明示するため、推移依存と同じPostgreSQL moduleを自身のPOMにも直接宣言しています。別DBへの対応は、
database-specific moduleを追加して回避せず、Starterの推移依存を含む対応範囲と依存契約をblocking reviewで判断します。
