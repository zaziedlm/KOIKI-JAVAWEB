# Greenfield Bootstrap Smoke Validation

## 1. Status and positioning

| Item | Value |
|---|---|
| Status | `COMPLETE / PASS — REFERENCE-INDEPENDENT GREENFIELD BOOTSTRAP PROVEN` |
| Recorded on | 2026-09-25 |
| Framework source baseline | `bd7c87f15bb78b4b883cdb8c28e4016ad11b25e2`、dirty `false` |
| External project | Git未管理のSpring Initializr生成project |
| Ownership | 外部fixtureと実行EvidenceはTooling、追加source / migrationはCustomer-like fixture |
| Framework production change | 0 |
| Remote operation | 0。push、PR、publish、workflow dispatchを行わない |

本検証は、現行Reference Applicationのsourceを利用せず、新しいInitializr生成物からKOIKI Parent、必要なStarter、
Architecture RulesおよびCustomer所有migrationだけで独立Applicationを構成できるかを確認する補足Adoption検証である。
検証fixtureを正式なCustomer ApplicationまたはProject Templateへ昇格させず、Phase 5境界を維持する。

## 2. Initializr baseline

| Setting | Captured value |
|---|---|
| Build / language | Maven / Java |
| Spring Boot / Java | 4.1.1 / 21 |
| Packaging / configuration | Jar / Properties |
| Coordinates | `jp.co.himacs:greenfield-bootstrap:0.0.1-SNAPSHOT` |
| Initial dependencies | `spring-boot-starter`、`spring-boot-starter-test` |
| Maven Wrapper | Maven 3.9.16 |
| Git repository / reparse point | なし / 0 |
| Generated files | 10 |

POM変換前の10ファイルについてrelative path、sizeおよびSHA-256をRepository外Evidence storeへ記録した。

| Evidence | Value |
|---|---|
| Manifest | `greenfield-bootstrap-initializr-baseline-20260925.json` |
| Manifest SHA-256 | `983C41043D25E0E754A1117CC513DE3D89C253BA30F5E22C2AFA984DC3A13617` |
| Sidecar | manifest hashと一致 |

## 3. Minimal composition

InitializrのCustomer coordinateと`application.properties`形式を維持し、親だけを
`org.koikifw:koiki-parent:0.1.0-SNAPSHOT`へ変更した。親探索は空の`<relativePath/>`で無効化し、
filesystem repository、`systemPath`、Framework sourceへの相対pathは追加していない。

| Purpose | Dependency |
|---|---|
| Module metadata | `koiki-architecture-contract` |
| REST / validation | `koiki-starter-api` |
| KOIKI / Customer Flyway | `koiki-starter-data` |
| JPA / OSIV boundary | `koiki-starter-data-jpa` |
| Runtime DB driver | `org.postgresql:postgresql` |
| Architecture verification | `koiki-archunit-rules`（test） |
| Unit test | `spring-boot-starter-test`（test） |

Customer-like sourceは`jp.co.himacs.greenfield`配下だけに置いた。検証専用`readiness` moduleを
`SIMPLE / JPA / SHARED`として宣言し、Customer所有の`customer_bootstrap_probe`を
`classpath:db/migration/customer/V1__create_bootstrap_probe.sql`で作成する。read-only endpointは
`GET /api/v1/bootstrap/readiness`であり、KOIKI API Starterのpath-segment versioning契約に従う。

Reference由来のpackage / import `org.koikifw.reference`は0件、`koiki-reference-app`依存は0件、
KOIKI internal package参照は0件である。Securityは今回のbuild / startup / DB接続smokeの対象外であり、
この公開fixture endpointをproductionのSecurity構成例として扱わない。

## 4. Isolated clean verify

既存の`invoke-p4-ar6-r2-handoff.ps1`を変更せず再利用し、通常の`.m2/repository`ではないrun専用repositoryへ
固定commitのformal release unitをstageした。外部projectはそのcoordinateだけから`clean verify`し、終了時に
Tooling所有stageを削除した。

| Check | Result |
|---|---|
| Formal inventory / forbidden content | PASS / PASS |
| Customer-like `clean verify` | PASS、3 tests / failure 0 / error 0 / skipped 0 |
| Architecture Rules | 2 test methodを含むtest class 1件がcompile / execute / PASS |
| KOIKI dependency / payload | 5 artifacts / build前後不変 `true` |
| Internal package reference | 0 |
| Executable JAR | 53,812,270 bytes、SHA-256 `43C84B9C247A02717D6066CA5CAE6EB2519A36D4F90424C04E1DA200D7105733` |
| Stage cleanup | PASS、stage root removed |

| Evidence | Value |
|---|---|
| Manifest | `greenfield-bootstrap-full-final-20260925.json` |
| Manifest SHA-256 | `1336081E6C3EA9C6D2EF717377423F968EB67578760E9D46E9AD5ED41C15AE70` |
| Sidecar | manifest hashと一致 |

## 5. Packaged JAR / PostgreSQL result

`postgres:17`の使い捨てcontainerへpackage済みJARを接続し、port 18081で起動した。起動logではHikari接続、
Framework Flyway history `koiki_flyway_history`の作成、Customer Flyway baseline 0、Customer V1適用、
Hibernate schema validationおよびApplication起動完了を確認した。

| Observation | Result |
|---|---|
| HTTP | `GET /api/v1/bootstrap/readiness` → `status=READY`、`databaseMarker=GREENFIELD` |
| Customer Flyway | `0|<< Flyway Baseline >>|true`、`1|create bootstrap probe|true` |
| Customer row | `1|GREENFIELD` |
| Framework Flyway history | `koiki_flyway_history`が存在 |
| Runtime stdout | SHA-256 `99CCDA9B008668DE26C6D62C0402E8F2CE2FBEC0A30660931912524186E623AC` |
| Runtime stderr | 0 bytes |
| Cleanup | Application停止、container残存0、18081 / 55433 listener 0 |

初回runtime probeでは`/bootstrap/readiness`を使用し、API Starterが`readiness`をversion segmentとして解釈して
400 `Invalid API version`を返した。これはFramework障害ではなくfixtureの公開契約不適合である。
Reference sourceを移植せず、公開されているversioning契約へmappingを修正してから再度isolated `clean verify`と
runtime検証を行い、最終結果をPASSとした。この経緯により、Starter導入だけでなくAPI契約への適合もbootstrap時に必要だと確認した。

## 6. Conclusion

新規Initializr生成物は、Reference source、Root Reactor、Framework source pathおよび通常のlocal Maven repositoryへ
依存せず、KOIKI Parent / Starter / Architecture Rulesの公開coordinateからbuild、test、package、起動、
PostgreSQL接続およびCustomer migrationを実行できた。したがって外部Referenceが偶然持っていた構成への依存ではなく、
greenfield Customer-like projectからもFramework接続方式が成立する。

一方、本検証は正式release repository、認証認可を含むproduction baseline、実チーム受入、Project Template、
Gate P4-ARまたはPhase 4開始を証明しない。外部fixtureとEvidenceはこの検証目的に限定する。
