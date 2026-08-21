# Walking Skeleton — Flyway Two-tier Validation

**Status:** Completed

## Result

| ID | 検証 | 期待結果 | 実結果 | 判断 |
|---|---|---|---|---|
| WS-F01 | Spring Boot自動構成との共存 | Customer用の標準Flywayを維持できる | PASS | `FlywayMigrationStrategy`内でKOIKI用Flywayを先行実行できた |
| WS-F02 | 実行順序 | KOIKI V1の後にCustomer V1/V5が動く | PASS | Customer V1内の先行確認を通過し、KOIKI=1、Customer=5となった |
| WS-F03 | 履歴の分離 | 所有者ごとに履歴テーブルが作られる | PASS | `koiki_flyway_history`と`flyway_schema_history`へ分離された |
| WS-F04 | 後発KOIKI migration | Customer V5後にKOIKI V2を追加できる | PASS | 同じDBでKOIKI=2、Customer=5となった |
| WS-F05 | location分離 | 所有者間でversionが衝突しない | PASS WITH CHANGE | Customer locationを親Directoryから専用Directoryへ変更した |
| WS-F06 | 同一schemaの初期化 | 後続ownerが非空schemaでも開始できる | PASS WITH CHANGE | baseline-on-migrate=true、baseline-version=0が必要だった |

## Tested Configuration

| Item | Value |
|---|---|
| Spring Boot | 4.1.0 |
| Flyway | 12.4.0 |
| PostgreSQL | 17.10 (`postgres:17-alpine`) |
| Java | Temurin 21.0.12 |
| Docker Engine | 29.5.3 |

## Final Structure

| Owner | Location | History table | Order |
|---|---|---|---:|
| KOIKI Framework | `classpath:db/migration/koiki` | `koiki_flyway_history` | 1 |
| Customer Application | `classpath:db/migration/customer` | `flyway_schema_history` | 2 |

Spring Bootが自動構成するFlywayはCustomer用とする。KOIKI用Flywayは
`FlywayMigrationStrategy`から同じDataSourceを使って先行実行し、その後に
Spring Boot管理のCustomer Flywayを実行する。

## Findings

### Locationは親子関係にしない

当初設計のCustomer location `classpath:db/migration`では、Flywayが配下の
`db/migration/koiki`も再帰走査した。その結果、KOIKI V1とCustomer V1を
同一ownerの重複versionとして検出し、起動に失敗した。

したがって、履歴テーブルだけでなくlocationも所有者別の兄弟Directoryとして分離する。

### 後続ownerにはbaseline version 0が必要

KOIKI migration後の`public` schemaは非空であるため、履歴テーブルをまだ持たない
Customer Flywayは既定設定では安全停止した。

Customer側へ次を設定すると、version 0でbaselineした後にCustomer V1とV5が適用された。

```properties
spring.flyway.baseline-on-migrate=true
spring.flyway.baseline-version=0
```

baseline versionを既定の1にするとCustomer V1が適用対象外になるため、0を明示する。

### 後発migrationの独立性

初回起動後の履歴:

```text
KOIKI:   V1
Customer: baseline 0, V1, V5
```

同じDBへKOIKI V2を含む成果物を起動した後の履歴:

```text
KOIKI:   V1, V2
Customer: baseline 0, V1, V5
```

`koiki_runtime_setting`が作成され、Customer側はV5のままであることを確認した。

## Phase 1への持ち込み判断

- V2の「所有者別に独立してversionを進められるか」はPASSとする。
- locationsと履歴テーブルは双方とも所有者別に分離する。
- 同一schemaの後続ownerにはbaseline version 0を設定する。
- `FlywayMigrationStrategy`を用いる構成はKOIKI Starterが提供する。具体的なStarterへの所属はPhase 1bで決定する。
- 本PoCのapplicationとmigration SQLは正式製品コードへ昇格させない。
- Reference Applicationを含む3階層への一般化はPhase 1bで実装検証する。
