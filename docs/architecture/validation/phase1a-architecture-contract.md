# Phase 1a Architecture Contract — A3実効検証

**検証日:** 2026年8月21日<br>
**対象branch:** `feature/phase1a-build-foundation`<br>
**状態:** ACCEPTED<br>
**A3 status:** COMPLETE<br>
**Architecture Owner:** Shuichi Kataoka<br>
**承認日:** 2026年8月21日<br>
**Ownership:** Framework<br>
**対象:** A3 `org.koikifw:koiki-architecture-contract:0.1.0-SNAPSHOT`

## 1. 結論

G3で承認した最小Architecture Contractを正式artifactとして再実装し、7つのA3検証条件が
成立することを確認した。

- Public APIは`org.koikifw.architecture`の4型だけである。
- `KoikiModule`はpackageを対象とするruntime annotationであり、全4属性を必須とする。
- enum定数は承認済みの`SIMPLE`、`RICH`、`JPA`、`MYBATIS`、`SHARED`だけである。
- Customer相当packageで`@NullMarked`と`@KoikiModule`を併記し、4型を利用できる。
- production依存はJSpecify 1.0.0だけであり、Spring、Modulith、ArchUnitおよび永続化実装を含まない。
- 正式ReactorからContractだけを対象にした`verify`が隔離Maven repositoryでも成功し、4 testが成功した。
- JARのclass majorは65であり、後続japicmp向けPublic API inventoryを取得できた。

以上によりA3の技術条件を満たす。Architecture Owner Reviewにより、本書を`ACCEPTED`、
A3を`COMPLETE`とする。

## 2. Public API契約

| 型 | 承認・実装内容 |
|---|---|
| `KoikiModule` | `@Documented`、`@Retention(RUNTIME)`、`@Target(PACKAGE)`、`@Inherited`なし |
| `KoikiModule.name` | `String`、defaultなし |
| `KoikiModule.tier` | `ModuleTier`、defaultなし |
| `KoikiModule.persistence` | `PersistenceTechnology`、defaultなし |
| `KoikiModule.persistenceModel` | `PersistenceModel`、defaultなし |
| `ModuleTier` | `SIMPLE`、`RICH` |
| `PersistenceTechnology` | `JPA`、`MYBATIS` |
| `PersistenceModel` | `SHARED` |

`MYBATIS`は宣言語彙だけを提供する。MyBatis実装、Starter、Feature Templateまたは詳細規約が
利用可能になったことを意味しない。

## 3. Maven・依存境界

| 観点 | 実効結果 | 判定 |
|---|---|---|
| Maven coordinates | `org.koikifw:koiki-architecture-contract:0.1.0-SNAPSHOT` | PASS |
| Parent | `org.koikifw:koiki-parent:0.1.0-SNAPSHOT` | PASS |
| BOM管理 | Contract versionをKOIKI BOMへ登録 | PASS |
| production dependency | `org.jspecify:jspecify:1.0.0:compile`だけ | PASS |
| test dependency | `org.junit.jupiter:junit-jupiter:6.0.3:test` | PASS |
| test plugin | Maven Surefire Plugin 3.5.6 | PASS |
| prohibited dependency | Spring、Modulith、ArchUnit、JPA、MyBatis、Lombokなし | PASS |

Surefire 3.5.6はtest実行に必要なParent build policyとして追加した。Contractのproduction依存および
Public APIには影響しない。Architecture Owner Reviewにより、このA3追加baselineを承認した。

参照:

- [Apache Maven Surefire 3.5.6](https://maven.apache.org/surefire-archives/surefire-3.5.6/download.cgi)
- [Maven Surefire Plugin Usage](https://maven.apache.org/surefire/maven-surefire-plugin/usage.html)

## 4. Test結果

| Test | 確認内容 | 結果 |
|---|---|---|
| `KoikiModuleContractTest` | target、retention、documented、非inherited | PASS |
| `KoikiModuleContractTest` | 4属性、型、defaultなし | PASS |
| `KoikiModuleContractTest` | 承認済みenum定数だけ | PASS |
| `CustomerModuleContractTest` | 外部package、`@NullMarked`併記、annotation値 | PASS |

合計4 test、failure 0、error 0、skipped 0。

## 5. Public API inventory候補

JDK 21の`javap -public`から次を取得した。enumの標準生成memberである`values()`と
`valueOf(String)`も、将来のjapicmp baselineでは公開面として扱う。

```text
org.koikifw.architecture.KoikiModule
  String name()
  ModuleTier tier()
  PersistenceTechnology persistence()
  PersistenceModel persistenceModel()

org.koikifw.architecture.ModuleTier
  SIMPLE
  RICH
  ModuleTier[] values()
  ModuleTier valueOf(String)

org.koikifw.architecture.PersistenceTechnology
  JPA
  MYBATIS
  PersistenceTechnology[] values()
  PersistenceTechnology valueOf(String)

org.koikifw.architecture.PersistenceModel
  SHARED
  PersistenceModel[] values()
  PersistenceModel valueOf(String)
```

JAR内の`org/koikifw/architecture/`には、上記4型と`package-info.class`だけが存在する。

## 6. 実行検証

| Command / 条件 | 結果 |
|---|---|
| `.\mvnw.cmd -pl koiki-architecture-contract -am verify` | 隔離Maven repositoryでもexit 0、4 test成功 |
| `.\mvnw.cmd verify` | 正式4-project Reactor、exit 0 |
| Contract `dependency:tree -Dscope=runtime` | JSpecify 1.0.0だけ |
| Contract dependency tree | JUnit Jupiter 6.0.3はtest scope |
| `jar tf` | 4 Public APIと`package-info.class`だけ |
| `javap -public` | §5のinventoryを取得 |
| `javap -verbose KoikiModule` | major version 65 |

`-f koiki-architecture-contract/pom.xml verify`による直接実行は、同一release unitの
`koiki-parent`と`koiki-dependencies-bom`がローカルまたはartifact repositoryから解決できることを
前提とする。事前install済み環境ではexit 0を確認したが、空のMaven repositoryでは未公開BOMを
解決できないため失敗する。Repository内のA3正式検証経路は上記`-pl ... -am verify`とし、
snapshot公開後の独立Consumerによる解決はC1 / C2で検証する。

## 7. Scopeと残件

- `name`とmodule root package名の一致検査はB2 / B3のArchUnit ruleで扱う。
- japicmp baselineへの正式登録と破壊変更の負例はC3で扱う。
- NullAwayの正常・違反・復元の独立検証はB4で扱う。
- `SEPARATED`、MyBatis詳細規約、Named Interface、Runtime StarterおよびFlywayは追加していない。
- 本書とA3差分はArchitecture Owner Review済みである。後続WPは本承認へ含めず、各WPで独立して検証する。
