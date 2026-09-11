# Phase 2 P2-C2 C2-3 Root-external Consumer verification

## 1. Status and boundary

- 検証日: 2026年9月11日
- 作業パッケージ: `P2-C2 / C2-3`
- baseline commit: `4bea730cb8f72f9f1759cbbd74a33b5f4a87e038`
- branch: `feature/phase2-p2-c1-postgresql-migration`
- 状態: `COMPLETE / ARCHITECTURE OWNER APPROVED — C2-4 READY`
- Ownership: Tooling-owned Customer-like Consumer / Evidence
- production artifact / Framework Public API change: 0

C2-3では、既存Gate A Consumer `build-support/security-foundation-consumer`を新設対象として上書きせず、同directory配下に
PostgreSQL用の独立POM `postgresql`を追加した。Gate A ConsumerのPOM、source、testおよびartifact座標は変更していない。
新ConsumerもRoot Reactor、BOM、formal release unit、Reference、Project Template、`koiki-testing`およびsnapshot publishに含めない。

## 2. Consumer composition

| Subject | C2-3 result |
|---|---|
| Parent解決 | `relativePath`なしで隔離repositoryの`koiki-parent`を解決 |
| Framework入口 | `koiki-starter-session-jdbc` 1件を直接利用 |
| Framework closure | Security / Identity / Audit / DataをBOM管理versionで解決 |
| Application dependency | WebMVC、PostgreSQL driver、Flyway PostgreSQL moduleをConsumerが所有 |
| Public API利用 | Identity作成 / 照会、Business Audit、Session cleanup |
| Customer migration | Consumer fixtureの`db/migration/customer/V1`だけ |
| runtime identity | 実行ごとにUUIDから合成し、固定値・出力・正式artifact昇格なし |

Consumer use caseはTier 1の単一transactionとしてIdentity作成、`IdentityQuery`による再照会、
`BusinessAuditRecorder`によるConsumer event記録を行う。起動assemblyはfixture principalを一時的なSecurityContextへ設定し、
use case終了後に`SessionCleanup`をPublic APIから呼び出す。別のWeb probeではConsumer-owned Controller / Security chainと
実行時生成したin-memory identityを使用する。Reference source、Framework internal package、固定credentialおよび
failure switchは追加していない。Controller、route、security configurationおよびidentityはすべて非配布fixtureに限定する。

## 3. Isolated package result

HarnessはGUID付き空Maven repositoryへC2-2 manifestどおりformal Framework release unit 14 projectsをstageし、
ConsumerをRootとは別のMaven invocationで`clean package`した。

| Check | Result |
|---|---|
| formal stage | 14 / 14 projects `BUILD SUCCESS` |
| Consumer compile / test / package | `BUILD SUCCESS` |
| Consumer unit test | 1 / 1 success |
| executable JAR | 1件、Java 21 class baseline |
| required runtime closure | Session / Identity / Audit / Data / PostgreSQL present |
| forbidden runtime closure | Reference 0 / `koiki-testing` 0 |
| KOIKI internal package reference | 0 |
| Root Reactor membership | 0 |

## 4. Java 21 / 25 and PostgreSQL result

PostgreSQL 17の同一clean databaseに対し、Java 21でbuildした同一JARをJava 21、続いてJava 25で起動した。
両起動は`P2-C2-C2-3-CONSUMER-SUCCEEDED`を返し、2回の間でJAR SHA-256が変化しないことを確認した。

| Observation | Result |
|---|---|
| Java 21 packaged runtime | success |
| Java 25 same-JAR runtime | success |
| Framework history | 3 success rows |
| Framework physical inventory | 11 application tables |
| Customer history | 2 success rows（baseline version 0: 1、SQL version 1: 1） |
| Customer physical inventory | `c2_consumer_marker`: 1 table |
| second startup migration | history version / checksum / installed rank完全一致、no-op |
| Public Identity result | synthetic users: 2 |
| Public Audit result | Business Audit rows: 4（各起動2） |
| Public Session result | cleanup `COMPLETED`を各起動で確認 |
| Customer public route | 200＋`X-Content-Type-Options: nosniff` |
| Customer private route | unauthenticated 401 / runtime fixture identity 200 |
| Consumer matcher外の未一致route | `/framework-fallback-probe`: Framework fallback 401 |

Customer historyが2行になるのは、Framework migrationが先に11 tableを作成した非空schemaへCustomer Flywayが
`baseline-on-migrate=true`で開始するためである。1行目はversion 0の`BASELINE`、2行目がConsumer V1の`SQL`であり、
Framework history 3行は別table `koiki_flyway_history`に保持される。この実測値をhistory分離の正本とする。

## 5. Negative guard and non-exposure

同じJARへ`spring.session.jdbc.initialize-schema=always`を外部環境から強制し、起動が非0で失敗して成功markerを返さないことを
確認した。失敗後も`SPRING_SESSION%` tableは0件であり、Framework migration / Customer migrationの所有権をSpring initializerが
迂回しない。

Consumer POM、README、source、Customer SQL、package済みJAR、Surefire report、Maven出力およびruntime出力を走査し、
private key、credential assignment、email形式PIIおよび実行時database passwordの残留がないことを確認した。
Harness終了後は所有Consumer Web child process、PostgreSQL container、GUID付き隔離repository / log directoryおよび
Consumer `target`を削除した。

## 6. Verification command and final result

次を実行した。

```powershell
pwsh -NoProfile -File build-support/security-foundation-verification/verify-p2-c2-consumer.ps1
```

最終結果:

```text
Phase 2 P2-C2 C2-3 Consumer verification succeeded (isolated package / Java 21 and 25 / PostgreSQL / migration no-op and failure guard).
```

## 7. Architecture Owner review points

1. 既存Gate A Consumerを変更せず、その配下の独立`postgresql` ConsumerをC2-3専用fixtureとする境界でよいか。
2. Root外Consumerが隔離repositoryからParent / BOM / Session入口とFramework closureを解決し、Reference / `koiki-testing` /
   internal packageを利用しない結果を配布可能性のEvidenceとしてよいか。
3. Java 21 buildの同一JARをJava 21 / 25とPostgreSQL 17で実行し、Public Identity / Audit / Session APIを実DBで利用でき、
   同JARのWeb processでpublic / authenticated / unmatched routeとSecurity Headerを外部観測した結果をC2-3 Evidenceとしてよいか。
4. Customer historyを実測どおりbaseline version 0の1行＋V1 SQLの1行、Framework historyを別tableの3行として固定し、
   2回目のversion / checksum / installed rank不変をno-op判定としてよいか。
5. initializer強制有効化の起動失敗と`SPRING_SESSION`非生成、sensitive-output 0、Consumer Web child process / container /
   temporary repository / Consumer target cleanupをC2-3のnegative / cleanup条件としてよいか。

推奨結論は上記5点を承認し、C2-3を`COMPLETE / ARCHITECTURE OWNER APPROVED`としてC2-4 Public API inventory /
compatibility fixtureへ進むことである。

2026年9月11日、Architecture Ownerは上記5点を確認した。review時の指摘により、3番はConsumer matcher外の
`/framework-fallback-probe`へ修正してFramework fallbackを再検証し、5番はConsumer Web child processをcleanup対象へ明記した。
反映後の再検証成功をもって5点すべてを承認し、C2-3を`COMPLETE / ARCHITECTURE OWNER APPROVED`、次をC2-4とする。
