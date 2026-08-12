# Flyway Two-tier Walking Skeleton

KOIKI FrameworkとCustomer Applicationのmigrationを、同じPostgreSQL schema上で
独立したlocationと履歴テーブルにより管理できるかを検証する捨てる前提のPoCです。

## 構成

| Owner | Location | History table |
|---|---|---|
| KOIKI | `classpath:db/migration/koiki` | `koiki_flyway_history` |
| Customer | `classpath:db/migration/customer` | `flyway_schema_history` |

Spring Bootが自動構成するFlywayをCustomer用として維持し、
`FlywayMigrationStrategy`でKOIKI用Flywayを先に実行します。

Customer側はKOIKI migration後の非空schemaへ履歴テーブルを作るため、
baseline-on-migrateを有効化し、baseline versionを0にします。

## 検証シナリオ

1. KOIKI V1を実行する。
2. Customer V1、V5を実行する。Customer V1はKOIKI V1の先行完了も検査する。
3. 同じDBに対し、`later-koiki-release` profileでKOIKI V2を追加する。
4. KOIKIがV2、CustomerがV5のままであることを検査する。

PowerShellから次を実行すると、専用PostgreSQLコンテナの作成から削除まで行います。

```powershell
.\walking-skeleton\ws-flyway-two-tier\verify-flyway-two-tier.ps1
```

PoCコードはPhase 1bの製品コードへコピーしません。実証した構成と注意点だけを
正式なRuntime Foundation設計へ反映します。
