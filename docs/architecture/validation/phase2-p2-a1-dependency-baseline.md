# Phase 2 P2-A1 dependency baseline

## 1. Status and scope

- **Validation date:** 2026年9月1日
- **Work package:** `P2-A1 / A1-2`
- **Status:** `A1-2 COMPLETE`
- **Branch:** `feature/phase2-security-foundation`
- **Baseline HEAD:** `ef708f251bcc3bcdd7829fd232b1f8a114bb6c04`
- **Java / Maven:** Eclipse Temurin 21.0.12 LTS / Maven Wrapper 3.3.4 / Maven 3.9.16

本記録は、正式`koiki-starter-security`と非配布Security fixtureについて、Spring Boot管理下の
最小dependency baselineを確定したEvidenceである。Securityの挙動、Auto Configuration、property、
matcher、401 / 403表現およびCustomer override契約はA1-3以降へ持ち越す。

## 2. Implemented dependency boundary

### 2.1 Formal Framework artifact

`org.koikifw:koiki-starter-security:0.1.0-SNAPSHOT`を
`koiki-starters/koiki-starter-security`へ追加し、root reactorとKOIKI BOMへ登録した。
production dependencyはversion指定なしの次の1件だけである。

```text
org.springframework.boot:spring-boot-starter-security
```

現時点ではdependency-only Starterであり、production Java class、Auto Configuration resource、
configuration propertyは追加していない。Public API inventoryは公開Java型0、公開property 0を記録する。

### 2.2 Non-distributed Tooling fixture

`build-support/security-foundation-verification`をroot reactor外の単一Maven projectとして追加した。
次をすべてtest scopeかつversion指定なしで使用する。

```text
org.koikifw:koiki-starter-security
org.springframework.boot:spring-boot-starter-test
org.springframework.boot:spring-boot-starter-security-test
```

fixtureの1 testは、production側の`SecurityFilterChain`とtest側の
`SecurityMockMvcRequestPostProcessors`が解決できることを確認する。fixture artifactは正式release unitへ
installされず、`koiki-testing`、既存Runtime Foundation Consumer、migrationおよびworkflowも変更しない。

## 3. Version authority evidence

StarterとfixtureのPOMにSpring Boot / Spring Securityの個別versionまたは独立BOMはない。
Spring Boot 4.1.1 BOMによるfocused `dependency:tree`の解決結果は次のとおりである。

| Dependency | Resolved version | Scope / role |
|---|---:|---|
| `spring-boot-starter-security` | 4.1.1 | production compile |
| `spring-security-config` | 7.1.1 | transitive compile |
| `spring-security-core` | 7.1.1 | transitive compile / test |
| `spring-security-crypto` | 7.1.1 | transitive compile / test |
| `spring-security-web` | 7.1.1 | transitive compile |
| `spring-boot-starter-test` | 4.1.1 | fixture test |
| `spring-boot-starter-security-test` | 4.1.1 | fixture test |
| `spring-security-test` | 7.1.1 | transitive fixture test |

専用検証scriptはproduction / fixture双方のdependency treeを隔離repository上で取得し、
Authorization Server、SAML、Redis Session、WebFluxおよびReactorが混入していないことを検査した。
独自JWT library、Oracle固有dependency、AWS固有dependencyもPOM差分には存在しない。

## 4. Release unit and regression alignment

- root reactorは10 projectsから11 projectsへ増加した。
- deploy可能な正式release unitは9 artifactsから10 artifactsへ増加した。
- Phase 1b CP10のhistorical invariantは、当時の必須成果物がすべて存在することを検査するsubset方式へ変更した。
- 現行11-project構成とSecurity artifactの存在はP2-A1専用scriptがexactに検査する。
- JDK commandがPATHにない環境でも既存検証が動くよう、CP7の`jar`、CP8 / CP9 / CP10の`java`を
  `JAVA_HOME/bin`から解決するようにした。検証内容やacceptance criteriaは変更していない。

## 5. Verification results

| Verification | Result |
|---|---|
| P2-A1 aggregate | `verify-p2-a1-security-foundation.ps1`: success |
| Isolated release stage | 11 / 11 projects success、Security POM / JARを確認 |
| Non-distributed fixture | 1 test、failure / error / skip 0 |
| Dependency boundary | required production / test dependencyあり、deferred dependency 0 |
| Distribution boundary | fixture artifactのrelease repository install 0 |
| Root verify | 11 / 11 projects `BUILD SUCCESS` |
| Existing root tests | Architecture Contract 4件 + ArchUnit Rules 66件、計70件成功 |
| CP8 regression | 実プロセス競合、異なるtask key、crash recovery成功 |
| CP10 Developer Journey | Web / maintenance同一JAR、PostgreSQL 17.11、HTTP / DB / event / async / health成功 |
| CP9 smoke | `-Smoke -SkipRegression`成功、result schema / minimal negative検証成功 |

CP10 aggregateの初回再実行では、CP1〜CP8およびCP10 Developer Journeyまで成功した後、末尾のCP9が
PATH上の`java`を直接参照して失敗した。CP9を`JAVA_HOME/bin/java`解決へ修正し、CP10が渡すものと同じ
`-Smoke -SkipRegression`引数で単体再実行して成功した。失敗はSecurity dependencyやruntime regressionではなく、
Java 21が`JAVA_HOME`にだけ設定されたPCで既存scriptがPATHを仮定していたことによる。

Security StarterのA1-2実行時点のempty JAR warningは、dependency baselineだけを実装し、A1-3の
Auto Configurationを先行していないための想定内結果である。

## 6. A1-2 conclusion

Spring Bootを唯一のversion authorityとする最小Security dependency baselineが、正式Framework artifactと
非配布Tooling fixtureのOwnershipを混在させず成立した。A1-2の差分にA1-3以降のSecurity挙動、identity、
session、audit、migrationまたはReference機能は含まれないため、次はA1-3の最小production security foundationへ進める。
