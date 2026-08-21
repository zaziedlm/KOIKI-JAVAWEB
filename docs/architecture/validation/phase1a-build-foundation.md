# Phase 1a Build Foundation — A2 / G1実効検証

**検証日:** 2026年8月21日<br>
**対象branch:** `feature/phase1a-build-foundation`<br>
**状態:** ACCEPTED<br>
**A2 status:** COMPLETE<br>
**G1 status:** ACCEPTED<br>
**Architecture Owner:** Shuichi Kataoka<br>
**承認日:** 2026年8月21日<br>
**Ownership:** Tooling<br>
**対象:** A2 Root Reactor / Parent / BOM / Maven Wrapper、G1 Build Baseline

## 1. 結論

Phase 1a開始baselineを正式なRoot Reactor、Parent、BOMおよびMaven Wrapperへ再構成し、
G1で要求した実効依存関係とbuild contractが成立することを確認した。

- Maven座標は`org.koikifw:*:0.1.0-SNAPSHOT`へ統一され、一時座標と`ws-*`を正式Reactorから除外した。
- Rootは集約、BOMはdependency version、Parentはbuild / plugin policyだけを所有する。
- Spring Bootは4.1.1、Spring Frameworkは7.0.9、Spring Modulithは2.1.0へ実効的に統一された。
- `spring-modulith-starter-test`とそのModulith依存はtest scopeだけに存在し、runtime treeには現れない。
- Maven Wrapper 3.9.16とJDK 21で正式Reactorの`clean verify`が成功した。
- JDK 25によるbuildはEnforcerで意図どおり拒否された。Java 25 runtime互換性はC4で別途検証する。

以上によりA2の完了条件とG1の実効検証条件を満たす。Architecture Owner Reviewにより、
A2を`COMPLETE`、G1を`ACCEPTED`とする。

## 2. 正式構成

| 対象 | 責務 | 実効内容 |
|---|---|---|
| Root Reactor | 正式moduleの集約だけ | 現時点ではBOM、Parentを集約 |
| Dependencies BOM | dependency version管理 | Boot 4.1.1、Modulith 2.1.0、JSpecify 1.0.0 |
| Parent | build / plugin policy | Java 21、Enforcer、Toolchains、Compiler、Error Prone / NullAway |
| Maven Wrapper | build tool固定 | Wrapper 3.3.4 `bin`型、Maven 3.9.16 |
| G1 baseline consumer | 実効POMとscopeの検証 | 非配布、Root Reactor外、異なるConsumer versionを使用 |

ParentからimportするKOIKI BOM versionは`0.1.0-SNAPSHOT`へ明示固定した。これにより、
`9.9.9-SNAPSHOT`の検証ConsumerがParentを継承してもConsumer自身のversionへ誤置換されない。
正式release時は同一version release unitとしてParentとBOMを同時更新する。

## 3. 公式情報との照合

2026年8月21日に次の公式一次情報を確認した。

- Spring Boot 4.1.1はJava 17以上、Java 26まで、およびSpring Framework 7.0.9以上を要件とする。
- Spring Modulith 2.1.0を採用し、Phase 1aでは`spring-modulith-starter-test`だけをtest scopeで使用する。
- Maven 3系列の開始baselineを3.9.16、Wrapper Pluginを3.3.4とする。
- Maven Compiler 3.15.0、Enforcer 3.6.3、Toolchains 3.3.0を維持する。

参照:

- [Spring Boot System Requirements](https://docs.spring.io/spring-boot/system-requirements.html)
- [Spring Modulith Reference](https://docs.spring.io/spring-modulith/reference/index.html)
- [Spring Modulith Appendix](https://docs.spring.io/spring-modulith/reference/appendix.html)
- [Apache Maven Release History](https://maven.apache.org/docs/history.html)
- [Apache Maven Plugins](https://maven.apache.org/plugins/index.html)
- [Apache Maven Wrapper Plugin](https://maven.apache.org/tools/wrapper/maven-wrapper-plugin/wrapper-mojo.html)

## 4. 実効依存関係

検証Consumerは正式Parent `0.1.0-SNAPSHOT`を`relativePath`なしで継承し、Consumer自身には
`9.9.9-SNAPSHOT`を設定した。compile dependencyとして`spring-boot`、test dependencyとして
`spring-modulith-starter-test`を宣言した。

| 観点 | 実効結果 | 判定 |
|---|---|---|
| Spring Boot | 4.1.1 | PASS |
| Spring Framework | 7.0.9へ統一 | PASS |
| Spring Modulith | 2.1.0 | PASS |
| Modulith scope | すべてtest | PASS |
| runtime tree | Boot 4.1.1 / Framework 7.0.9。Modulithなし | PASS |
| Java release | Parent実効POMで21 | PASS |
| build plugin | Enforcer 3.6.3、Toolchains 3.3.0、Compiler 3.15.0 | PASS |

Modulith 2.1.0の推移依存が要求するBoot 4.1.0およびFramework 7.0.8は、KOIKI BOMで先に
importするBoot 4.1.1のdependency managementにより、それぞれ4.1.1と7.0.9へ統一された。

## 5. 実行検証

| Command / 条件 | 結果 |
|---|---|
| `.\mvnw.cmd -version` | Maven 3.9.16、Temurin 21.0.12 |
| `.\mvnw.cmd -U clean verify` | 正式3-project ReactorでBUILD SUCCESS |
| Parent `help:effective-pom` | release 21と3 build pluginを確認 |
| Consumer `help:effective-pom` | 異なるConsumer versionから正式Parent / BOMを解決 |
| Consumer `dependency:tree -Dverbose` | Boot / Framework / Modulithの実効versionとtest scopeを確認 |
| Consumer `dependency:tree -Dscope=runtime` | Modulith artifactなし |
| JDK 25で`.\mvnw.cmd validate` | exit 1、`KOIKI build must run with JDK 21` |

Consumer確認前の`install`はA2内のローカル実効POM検証に限る。C2のRepository外Consumer証明では
使用せず、G4で承認した内部snapshot repositoryだけから解決する。

## 6. Windows Maven Wrapper判断

Wrapper 3.3.4の`only-script`型は、Windows PowerShellで通常ディレクトリの`Target`が`null`の
場合に添字参照で停止する事象を実機で再現した。生成scriptへKOIKI独自patchを残さず、公式に
提供される`bin`型へ変更して`maven-wrapper.jar`をRepositoryへ含めた。再生成後、同じWindows
環境で`mvnw.cmd -version`と`clean verify`が成功した。

参照:

- [Apache Maven Wrapper Issue #395](https://github.com/apache/maven-wrapper/issues/395)
- [Apache Maven Wrapper — Usage with or without Binary JAR](https://maven.apache.org/tools/wrapper/index.html)

## 7. Scopeと残件

- A2ではNullAway / Error ProneのParent設定を保持したが、Java production sourceに対する正常・
  違反・復元の実動検証はB4で行う。
- Architecture Contractと正式Java moduleはA3以降で追加する。未使用の空moduleは生成しない。
- Java 21で生成した同一artifactのJava 21 / 25 runtime起動、hashおよびclass major 65はC4で検証する。
- CI上の再現はA4、内部snapshot経由の外部ConsumerはC1 / C2で検証する。
- 本書とA2差分はArchitecture Owner Review済みである。後続WPは本承認へ含めず、各WPで独立して検証する。
