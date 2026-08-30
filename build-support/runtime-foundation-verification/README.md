# Runtime Foundation Verification

Phase 1bのFramework-owned runtime規約を細粒度に検証する、Tooling-ownedの非配布fixtureである。
Root Reactorには含めず、隔離Maven repositoryへstageしたKOIKI artifactだけを通常座標で参照する。

CP1では`koiki-starter-api`によってServlet Spring MVCとJakarta Validationが利用可能になり、
Spring Boot application contextがrandom portで起動することを検証した。

CP2ではJackson 3とAPI Versioningの既定値、Spring Framework Resilienceの有効化、retry回数と
対象外例外、Starter全体の無効化、Customer propertyによるoverrideを検証する。業務機能、
正式Reference、Customer設定またはFramework内部実装をfixtureへ混在させない。

CP3では`MockMvc`を使い、Validation、異常JSON、直接発生した`JacksonException`、未処理例外と
Application-ownedなSpring `ErrorResponse`を検証する。KOIKI handlerの無効化、内部情報非露出および
RFC 9457 `application/problem+json` contractを含む。

CP4では`koiki-starter-data`の有効／無効、Application-owned strategyへのback-off、Customer migrationの
低優先度既定とoverrideを細粒度に検証する。実PostgreSQL上のmigration順序、異常系と復旧、業務経路は
Customer-like Runtime Consumerが担当する。

CP5では`koiki-starter-observability`のstructured logging既定、相関IDの受入／生成／cleanup、Micrometer
Context Propagationによる同一executor threadの伝播／漏えい防止、Customer `TaskDecorator`との共存、
全体／機能別無効化を検証する。

CP6ではActuator healthの公開範囲とprobe既定、`koiki-starter-data-jpa`のOSIV false／Application overrideを
細粒度に検証する。実PostgreSQLのUP／DOWN／restoreとresponse生成時のEntity露出負例はCustomer-like
Runtime Consumerが担当し、`verify-cp6-health-osiv.ps1`がartifact／依存境界を含めて一括検証する。

CP8では`verify-cp8-single-execution.ps1`がCP1〜CP7回帰、隔離Maven repositoryへの正式release unit stage、
Consumer独立build、artifact／依存／migration境界を検証する。package済みの同一JARを複数の実OS processで
起動し、同一task keyの競合、異なるkeyの独立性、process kill後のPostgreSQL session lock解放とretry、
DB副作用件数、non-web processおよびstructured lifecycle logを一括確認する。

```powershell
pwsh -NoProfile -File build-support/runtime-foundation-verification/verify-cp8-single-execution.ps1
```

CP10では`verify-cp10-closeout.ps1`がCP8回帰を再利用したうえで、別の空Maven repositoryへ正式10-project
release unitをstageし、Customer-like Consumerを独立packageする。package済みJARをWeb processとして起動し、
version付きHTTP、Problem Details、同期Eventを含むDB副作用、structured correlation log、health、Flyway履歴、
table ownershipを外部観測する。同じJARのnon-web maintenance成功後、CP9を`-Smoke -SkipRegression`で実行し、
性能数値ではなくharness／schema／cleanup contractだけを確認する。

```powershell
pwsh -NoProfile -File build-support/runtime-foundation-verification/verify-cp10-closeout.ps1
```

scriptは正式artifact、Public API inventory、production migration、runtime dependencyおよび実行前後のGit statusも
機械検査する。production source、通常local Maven cache、固定credentialまたはCP9公式baseline resultを変更しない。
