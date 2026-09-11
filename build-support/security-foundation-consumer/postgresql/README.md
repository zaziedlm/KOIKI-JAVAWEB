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
