# Runtime Foundation Verification

Phase 1bのFramework-owned runtime規約を細粒度に検証する、Tooling-ownedの非配布fixtureである。
Root Reactorには含めず、隔離Maven repositoryへstageしたKOIKI artifactだけを通常座標で参照する。

CP1では`koiki-starter-api`によってServlet Spring MVCとJakarta Validationが利用可能になり、
Spring Boot application contextがrandom portで起動することを検証する。業務機能、正式Reference、
Customer設定またはFramework内部実装をfixtureへ混在させない。
