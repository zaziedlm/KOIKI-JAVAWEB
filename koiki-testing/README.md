# KOIKI Testing Support

Customer testからSpring Boot TestcontainersとPostgreSQL TestcontainersをBOM管理された同一依存で
利用するための、Tooling-ownedかつCustomer-facingなformal testing artifactです。配布単位上はFramework成果物に
含みますが、production runtime Starterではありません。独自Java abstractionを追加せず、Spring Boot標準の
`@ServiceConnection`とTestcontainers APIを直接利用します。production runtime、Reference fixture、test user、
credential、migrationまたはbrowser harnessは含めません。
