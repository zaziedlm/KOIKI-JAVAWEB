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
