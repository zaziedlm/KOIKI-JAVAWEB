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

secretやpasswordをcommand line引数へ直接載せず、Applicationを起動するPowerShell processだけに環境変数として
設定する。Identityのsource HMAC keyは、実行ごとに32 byteの乱数を生成し、Base64で設定する。

```powershell
$env:SERVER_PORT = "18080"
$env:SPRING_DATASOURCE_URL = "jdbc:postgresql://127.0.0.1:55432/koiki_reference"
$env:SPRING_DATASOURCE_USERNAME = "koiki"
$env:SPRING_DATASOURCE_PASSWORD = "local-only-password"
$env:KOIKI_REFERENCE_SOURCE_HMAC_KEY_ID = "local-reference-1"

$sourceHmacKeyBytes = [byte[]]::new(32)
$sourceHmacKeyGenerator = [System.Security.Cryptography.RandomNumberGenerator]::Create()
try {
  $sourceHmacKeyGenerator.GetBytes($sourceHmacKeyBytes)
} finally {
  $sourceHmacKeyGenerator.Dispose()
}
$env:KOIKI_REFERENCE_SOURCE_HMAC_KEY = [Convert]::ToBase64String($sourceHmacKeyBytes)
Remove-Variable sourceHmacKeyBytes, sourceHmacKeyGenerator
```

`KOIKI_REFERENCE_SOURCE_HMAC_KEY`はBase64文字列であり、Base64復号後に32 byte以上でなければならない。
生成値をterminalへ表示せず、Repository、command history、log、文書または共有channelへ保存しない。同じPowerShellで
Applicationを再起動する場合は設定値をそのまま再利用できる。

## 7. Applicationを起動する

```powershell
java -jar .\koiki-reference-app\target\koiki-reference-app-0.1.0-SNAPSHOT.jar
```

初回起動時は、同じDataSourceに対して次の所有者別migrationが自動適用される。

- Framework: `classpath:db/migration/koiki` / `koiki_flyway_history`
- Reference: `classpath:db/migration/kkref` / `kkref_flyway_history`

Framework / Reference migrationの成功だけでなく、最後に`Started ReferenceApplication`が表示されることを確認する。
ApplicationContextがその後の設定検証で停止した場合は起動完了ではなく、demo data投入へ進まない。起動完了後、
このPowerShellはApplication実行用として開いたままにし、`http://127.0.0.1:18080/login`へアクセスする。

## 8. 初期データの制約

clean DBには、user、credential、Role、Permission、部門、経費科目、所属または承認scopeを投入する
production seedが存在しない。このため、migrationとApplication起動は確認できるが、初期Identityを別途
provisioningしない限りログイン後の業務操作はできない。

これはPhase 2 / Phase 3で承認した次の境界による。

- 固定user、固定password、test keyまたはtest routeをproduction artifactへ含めない。
- Reference migrationへ検証用seedを追加しない。
- Identity作成やRole / Permission準備は、承認済みPublic contractを利用するprovisioning経路が所有する。

P3-B2の目視確認に使用したデータは、使い捨てDBだけへ準備した検証データであり、本書の正式な初期データとは
しない。再現用のlocal demo fixtureはTooling Ownershipとして次項へ分離し、正式provisioningやbrowser runnerの
代わりにはしない。

### 8.1 使い捨てdemo dataを準備する

開発チームが画面を短時間で確認する場合は、非配布Tooling
`build-support/reference-local-demo/seed-reference-demo.ps1`を明示実行できる。§7のApplicationが
`Started ReferenceApplication`まで到達した後、Applicationは起動したまま、別のPowerShellをRepository rootで開いて
次を一度だけ実行する。seed用PowerShellには§6の環境変数を設定しない。

```powershell
.\build-support\reference-local-demo\seed-reference-demo.ps1 -ConfirmDisposable
```

scriptは次をすべて確認してからdataを登録する。

- container名が`koiki-reference-`で始まり、PostgreSQL 17 containerが稼働している。
- Framework / Referenceの必須tableが存在する。
- Identity user、Reference master、所属、承認scopeおよびexpense dataが0件である。
- 実行者が`-ConfirmDisposable`を明示している。

成功すると、次のdemo dataと、実行ごとにランダム生成したlogin passwordをterminalへ表示する。

- `p3-demo-user@example.test`と`P3_DEMO_REVIEWER`
- Identity user ID `b2000000-0000-4000-8000-000000000001`
- `EXPENSE:APPLY`、`EXPENSE:APPROVE`、`EXPENSE:SETTLE`、`IDENTITY:ADMIN`、`MASTER:ADMIN`
- demo部門、経費科目、所属、承認scope
- DRAFT、SUBMITTED、APPROVEDの経費申請各1件

seed内の固定UUIDは、ReferenceのWeb Formが検証するversion 4 / RFC variant形式に合わせている。

passwordの固定値はRepositoryへ保存されず、そのterminalと使い捨てDBだけに存在する。terminal出力をlog、文書、
screenshotまたは共有channelへ保存しない。scriptはToolingによる直接SQL fixtureであり、正式なIdentity provisioning、
production seed、migration、browser harnessまたはCustomer向け初期dataとして扱わない。既存dataを検出した場合は
追記・更新・削除せず停止する。一度表示されたpasswordを失った場合は、scriptを再実行してcredentialを上書きせず、
§9でApplicationとcontainerを停止して§4から使い捨て環境を作り直す。利用後も§9どおりdataを破棄する。

### 8.2 Browserで確認する

成功時に表示されたpasswordを共有・保存せず、その場で次のloginに使用する。

| 確認対象 | 入力またはURL |
|---|---|
| Login | `http://127.0.0.1:18080/login` / `p3-demo-user@example.test` |
| Identity user lookup | User ID `b2000000-0000-4000-8000-000000000001` |
| Expense | `http://127.0.0.1:18080/expenses` |

Identity user detailではRoleと5 Permissionを確認する。ExpenseではDRAFT、SUBMITTED、APPROVEDの3件を確認する。
Browser開発者toolでは、login後に`SESSION` Cookieが発行されることを確認できる。Cookieはsession識別子だけを持ち、
認証状態はserver-sideの`koiki_session` / `koiki_session_attributes`へ保存される。本local HTTP経路では`Secure`属性を
付けず、`HttpOnly` / `SameSite=Lax`を確認する。productionのHTTPS境界へこのlocal例外を持ち込まない。

## 9. 停止する

Applicationを実行しているPowerShellで`Ctrl+C`を入力した後、PostgreSQLを停止する。

```powershell
docker stop koiki-reference-postgres
```

本手順は`--rm`かつ永続化volumeを明示指定しない。PostgreSQL imageが作る匿名volumeもcontainerの自動削除に伴って
削除され、停止時に検証データは破棄される。named volume、Application container、DB初期データ構成、および
複数processを含む開発環境は、後続の開発環境整備で別途設計する。

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
3. `KOIKI_REFERENCE_SOURCE_HMAC_KEY_ID`を設定したか。
4. `KOIKI_REFERENCE_SOURCE_HMAC_KEY`がBase64としてdecodeでき、decode後に32 byte以上になるか。
5. Framework / Reference Flyway migration、JPA schema validation、その後の設定検証のどこで失敗したか。
6. `18080`を別processが使用していないか。

回避のためにCSRF、default deny、Identity source protection、migrationまたはJPA validationを無効化しない。

### Demo seedが停止する

- `Required Framework and Reference migrations have not been applied.`: §7のApplication初回起動より先に実行している。
  migration成功だけでなく`Started ReferenceApplication`を確認してから再実行する。
- `Demo seed requires a clean disposable database`: IdentityまたはReference dataがすでに存在する。正常投入後の再実行も
  この条件で拒否される。passwordを保持していれば再投入せず利用し、失っていれば§9で破棄して§4から作り直す。

## 11. 将来の開発環境との関係

Application container、PostgreSQL、初期table / data、provisioning、および起動順序をcompose等で統合する場合も、
次のOwnershipを維持する。

- Framework migrationはFramework Starterが所有する。
- Reference migrationと業務dataはReferenceが所有する。
- local検証userとcredentialの生成はToolingが所有し、production artifactへ含めない。
- Customer固有の初期dataとprovisioningはCustomerが所有する。

統合環境は本手順を置き換える利便経路であり、package済みJARが単独で起動できる契約自体は維持する。
