# P2-C2 PostgreSQL Consumer fixture

P2-C2 C2-3専用のCustomer-like非配布Consumer。既存Gate A Consumerのbaselineを変更せず、その配下で
独立POMとしてParent / BOM / `koiki-starter-session-jdbc`を隔離Maven repositoryから解決する。

- Root Reactor、BOM、formal release unitおよびsnapshot publishには含めない。
- Framework Public APIだけを使い、Identity作成 / 照会、Business Audit、Session cleanupを実PostgreSQLで確認する。
- package済みWeb processでpublic / authenticated / unmatched routeとSecurity Headerを外部確認する。
- `db/migration/customer`と合成identityは検証fixtureだけが所有し、Project Templateや正式Customer成果物ではない。
- Java 21でbuild / test / packageし、同一JARをJava 21 / 25で実行する。

正本の実行入口は次である。

```powershell
pwsh -NoProfile -File build-support/security-foundation-verification/verify-p2-c2-consumer.ps1
```

## What an application engineer should learn

- KOIKI Parentは`relativePath`なしで解決し、Starter versionをConsumer側で重複指定しない。
- Session profileの入口は`koiki-starter-session-jdbc`で、WebMVC、PostgreSQL driver、Flyway PostgreSQL moduleは
  Applicationが明示する。
- Customerは`SecurityFilterChain`、route、policy、credential供給および`db/migration/customer`を所有する。
- Identity / Audit / Sessionは`org.koikifw`の公開契約だけから利用し、`internal`、Entity、Repositoryを参照しない。
- Framework migrationはStarter JARから適用し、Customer側へcopyしない。
- built-in userや固定passwordは提供されない。Harnessの合成identity / credentialは実行時だけ生成される。

成功時は、隔離repositoryからのbuild、Java 21 / 25の同一JAR、PostgreSQL clean install / restart no-op、Framework 3 migration /
11 table、Customer history分離、public / authenticated / unmatched route、Security Header、Session initializer拒否およびcleanupが
確認される。

起動失敗時は、最初にDataSource / PostgreSQL、Flyway PostgreSQL module、Customer migration、Identityのlocal-authentication / HMAC、
Session JDBC固定値を確認する。秘密値をcommand、READMEまたはlogへ直接記録しない。一般的な診断順は
[Phase 2 Developer Journey](../../../docs/development/phase2-developer-journey.md#6-diagnose-before-adding-a-workaround)を参照する。

本fixtureのclass、route、migration、合成userまたはpropertyをCustomer成果物へcopyしない。
