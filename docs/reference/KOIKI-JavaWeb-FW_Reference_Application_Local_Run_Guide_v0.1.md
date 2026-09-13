# KOIKI-JavaWeb-FW Reference Application ローカル手動起動ガイド

**版:** v0.1  
**対象:** Phase 3 P3-B2 MVC / Thymeleaf  
**Ownership:** Reference  

## 1. 目的

本書は、`koiki-reference-app`をpackage済みSpring Boot実行可能JARとして、ローカルPostgreSQLと
組み合わせて手動起動する最小手順を示す。アプリケーションとDBを将来コンテナ化した後も、JAR単体の
成立確認、起動障害の切り分け、および手動Checkpointの基礎経路として利用する。

本書はProject Template、production deployment手順、固定test userの提供、またはbrowser test harnessではない。

## 2. P3-B2で確認した構成

| 項目 | 確認した構成 |
|---|---|
| OS | Windows 11 |
| Java | Temurin JDK 21.0.12.1 |
| Maven | Repository同梱Maven Wrapper / Maven 3.9.16 |
| Container runtime | Rancher Desktop / Docker Engine |
| Database | PostgreSQL 17 container |
| Application | `koiki-reference-app-0.1.0-SNAPSHOT.jar` |
| 起動方式 | `java -jar` |
| 確認URL | `http://127.0.0.1:18080` |

`koiki-reference-app/pom.xml`のSpring Boot Maven Pluginが`repackage`を行うため、生成物は
classes directoryを直接参照する起動ではなく、依存libraryを内包した実行可能JARになる。

## 3. 前提

Repository rootをPowerShellで開き、次を確認する。

```powershell
java -version
.\mvnw.cmd -version
docker info
```

- Build JDKは21を使用する。
- Rancher Desktopを使用する場合は、アプリケーション本体だけでなくDocker Engineが起動済みであることを確認する。
- 本手順ではhost側`55432`をPostgreSQLへ、`18080`をApplicationへ使用する。

## 4. 使い捨てPostgreSQLを起動する

次のdatabaseとcredentialは、ローカルの使い捨て環境だけで使用する。

```powershell
docker run --rm --name koiki-reference-postgres `
  -e POSTGRES_DB=koiki_reference `
  -e POSTGRES_USER=koiki `
  -e POSTGRES_PASSWORD=local-only-password `
  -p 55432:5432 `
  -d postgres:17-alpine

docker ps --filter name=koiki-reference-postgres
Test-NetConnection 127.0.0.1 -Port 55432
```

`docker ps -a`に停止済みcontainerが見えても、Docker Engine自体が利用可能とは限らない。
`docker info`、containerの状態、およびhost側port到達性を分けて確認する。

## 5. 実行可能JARを生成する

Application停止中にRepository rootから実行する。

```powershell
.\mvnw.cmd -pl koiki-reference-app -am -DskipTests clean package
Get-Item .\koiki-reference-app\target\koiki-reference-app-0.1.0-SNAPSHOT.jar
```

`clean`を含め、過去のincremental compilation出力へ依存しない状態から生成する。P3-B2では約63 MBの
実行可能JARが生成されることを確認した。正式な受入では、この手動buildだけでなくRoot Reactorの
`clean verify`を別途実行する。

## 6. 実行時設定を供給する

secretやpasswordをcommand line引数へ直接載せず、現在のPowerShell processだけに環境変数として設定する。

```powershell
$env:SERVER_PORT = "18080"
$env:SPRING_DATASOURCE_URL = "jdbc:postgresql://127.0.0.1:55432/koiki_reference"
$env:SPRING_DATASOURCE_USERNAME = "koiki"
$env:SPRING_DATASOURCE_PASSWORD = "local-only-password"
$env:KOIKI_REFERENCE_SOURCE_HMAC_KEY_ID = "local-reference-1"
$env:KOIKI_REFERENCE_SOURCE_HMAC_KEY = "replace-with-a-local-secret-of-at-least-32-bytes"
```

`KOIKI_REFERENCE_SOURCE_HMAC_KEY`には32 byte以上のlocal secretを使用し、Repositoryへcommitしない。

## 7. Applicationを起動する

```powershell
java -jar .\koiki-reference-app\target\koiki-reference-app-0.1.0-SNAPSHOT.jar
```

初回起動時は、同じDataSourceに対して次の所有者別migrationが自動適用される。

- Framework: `classpath:db/migration/koiki` / `koiki_flyway_history`
- Reference: `classpath:db/migration/kkref` / `kkref_flyway_history`

起動完了後、`http://127.0.0.1:18080/login`へアクセスする。

## 8. 初期データの制約

clean DBには、user、credential、Role、Permission、部門、経費科目、所属または承認scopeを投入する
production seedが存在しない。このため、migrationとApplication起動は確認できるが、初期Identityを別途
provisioningしない限りログイン後の業務操作はできない。

これはPhase 2 / Phase 3で承認した次の境界による。

- 固定user、固定password、test keyまたはtest routeをproduction artifactへ含めない。
- Reference migrationへ検証用seedを追加しない。
- Identity作成やRole / Permission準備は、承認済みPublic contractを利用するprovisioning経路が所有する。

P3-B2の目視確認に使用したデータは、使い捨てDBだけへ準備した検証データであり、本書の正式な初期データとは
しない。再現可能なローカルprovisioningやbrowser runnerはTooling OwnershipとしてP3-B3以降の判断対象にする。

## 9. 停止する

Applicationを実行しているPowerShellで`Ctrl+C`を入力した後、PostgreSQLを停止する。

```powershell
docker stop koiki-reference-postgres
```

本手順は`--rm`かつvolumeなしのため、container停止時に検証データは破棄される。永続化volume、Application container、
DB初期データ構成、および複数processを含む開発環境は、後続の開発環境整備で別途設計する。

必要に応じて、PowerShell processへ設定した値を削除する。

```powershell
Remove-Item Env:SERVER_PORT
Remove-Item Env:SPRING_DATASOURCE_URL
Remove-Item Env:SPRING_DATASOURCE_USERNAME
Remove-Item Env:SPRING_DATASOURCE_PASSWORD
Remove-Item Env:KOIKI_REFERENCE_SOURCE_HMAC_KEY_ID
Remove-Item Env:KOIKI_REFERENCE_SOURCE_HMAC_KEY
```

## 10. 切り分け

### Docker Engineへ接続できない

```powershell
docker info
docker ps -a
```

Rancher Desktopの画面に停止済みcontainerが表示されることと、Docker Engine APIへ接続できることは別に確認する。

### PostgreSQLのportへ到達できない

```powershell
docker ps --filter name=koiki-reference-postgres
docker logs koiki-reference-postgres
Test-NetConnection 127.0.0.1 -Port 55432
Get-NetTCPConnection -LocalPort 55432 -ErrorAction SilentlyContinue
```

container内のPostgreSQL起動、hostへのport公開、および他processとのport競合を分けて確認する。

### Applicationが起動しない

次を順に確認する。

1. Java 21でJARを実行しているか。
2. datasource URL、user、passwordがPostgreSQL containerと一致するか。
3. `KOIKI_REFERENCE_SOURCE_HMAC_KEY_ID`と32 byte以上のsecretを設定したか。
4. Framework / Reference Flyway migration、JPA schema validationのどこで失敗したか。
5. `18080`を別processが使用していないか。

回避のためにCSRF、default deny、Identity source protection、migrationまたはJPA validationを無効化しない。

## 11. 将来の開発環境との関係

Application container、PostgreSQL、初期table / data、provisioning、および起動順序をcompose等で統合する場合も、
次のOwnershipを維持する。

- Framework migrationはFramework Starterが所有する。
- Reference migrationと業務dataはReferenceが所有する。
- local検証userとcredentialの生成はToolingが所有し、production artifactへ含めない。
- Customer固有の初期dataとprovisioningはCustomerが所有する。

統合環境は本手順を置き換える利便経路であり、package済みJARが単独で起動できる契約自体は維持する。
