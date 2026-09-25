# External Reference Boundary Validation

## 1. Status and positioning

| Item | Value |
|---|---|
| Status | `COMPLETE / PASS — EXTERNAL REFERENCE BOUNDARY PROVEN` |
| Recorded on | 2026-09-25 |
| Framework source baseline | `f7ad1410f42a2f838eada66e0ba5eb82fd7455f4`、tracked change 0 |
| Ownership | External project / staging / verificationはTooling、検証対象production codeはReference |
| Customer production change | 0 |
| Framework production change | 0 |
| Remote operation | 0。push、PR、publish、workflow dispatchを行わない |

本検証は、現行Reference ApplicationをCustomerに近い独立project境界へ一時的にmaterializeし、
Framework source tree、Root Reactorまたは通常のMaven local repositoryへ偶発依存せずにbuild / runできるかを
確認する補足Adoption検証である。ReferenceをCustomer Application、Project Templateまたは継続開発するforkへ
変更しない。P4-ARまたはPhase 3を再オープンせず、Phase 4開始または正式Framework releaseを意味しない。

## 2. Initializr start baseline

### 2.1 Generated project

VS Code multi-root workspaceへ追加されたGit未管理の`EXT-REFERENCE`について、Reference sourceのmaterialize、
KOIKI dependencyの追加およびPOM変換より前の状態を採取した。

| Setting | Captured value |
|---|---|
| Build | Maven |
| Language | Java |
| Spring Boot | 4.1.1 |
| Packaging | Jar |
| Java | 21 |
| Configuration | Properties |
| Group | `jp.co.himacs` |
| Artifact | `ext-reference` |
| Project version | `0.0.1-SNAPSHOT` |
| Initial dependencies | `spring-boot-starter`、`spring-boot-starter-test` |
| Maven Wrapper | Maven 3.9.16、wrapper script distribution 3.3.4 |
| Git repository | なし。projectとworkspace管理directoryの双方でGit rootを検出しない |
| Reparse point | 0 |
| Generated files | 10 |

この時点では`spring-boot-starter-parent:4.1.1`を親とし、`application.properties`には
`spring.application.name=ext-reference`だけが存在する。KOIKI artifact、Reference source、Reference migration、
test fixtureおよびFramework source pathは含まれない。baseline取得時にbuildやdependency downloadは実行していない。

### 2.2 Machine-readable evidence

full baseline manifestはsource Repository外のEvidence storeへ保存し、Repositoryへは非機密summaryとhashだけを残す。

| Evidence | Value |
|---|---|
| Kind | `externalReferenceInitializrBaseline` |
| Schema version | 1 |
| Manifest file | `initializr-baseline-20260925.json` |
| Manifest SHA-256 | `D34F9068EFC036061CE366CA5DD3D3641FF11087D72EFE99AFCA1391F830AD63` |
| Initial POM SHA-256 | `4B37896E0C5C4B0AA299A7360A0859439BBE062A5772FA22639EB0067386553D` |
| Wrapper properties SHA-256 | `488E1B3F2E641779D4636ABF9390845F901E64607261BC3C0B0BFE4FE96E6706` |

manifestは10ファイルのrelative path、sizeおよびSHA-256を保持する。絶対source path、credential、Customer業務情報、
artifact本体またはdependency tree全文は保存しない。sidecarはmanifest自身のSHA-256を保持し、自己参照を避ける。

2026-09-25のPOM変換直前にToolingの`BaselinePreflight`を実行し、manifestとsidecar、Framework commit、
`target/`を除く10ファイルのsize / SHA-256がすべて一致することを再確認した。manifest SHA-256は
`D34F9068EFC036061CE366CA5DD3D3641FF11087D72EFE99AFCA1391F830AD63`から変化していない。

## 3. POM transformation specification

### 3.1 Decision

外部ReferenceはKOIKI Parentを通常のMaven coordinateとして継承し、filesystem上の親探索を空の
`relativePath`で無効化する。InitializrのCustomer相当coordinateは維持し、`org.koikifw`を外部projectの
groupIdに使用しない。依存と実行可能JAR構成は現行`koiki-reference-app`と一致させる。

変換後POMは次を目標形とする。

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">

  <modelVersion>4.0.0</modelVersion>

  <parent>
    <groupId>org.koikifw</groupId>
    <artifactId>koiki-parent</artifactId>
    <version>0.1.0-SNAPSHOT</version>
    <relativePath/>
  </parent>

  <groupId>jp.co.himacs</groupId>
  <artifactId>ext-reference</artifactId>
  <version>0.0.1-SNAPSHOT</version>
  <packaging>jar</packaging>

  <name>External Reference Verification</name>

  <dependencies>
    <dependency>
      <groupId>org.koikifw</groupId>
      <artifactId>koiki-architecture-contract</artifactId>
    </dependency>
    <dependency>
      <groupId>org.koikifw</groupId>
      <artifactId>koiki-starter-web-mvc</artifactId>
    </dependency>
    <dependency>
      <groupId>org.koikifw</groupId>
      <artifactId>koiki-starter-api</artifactId>
    </dependency>
    <dependency>
      <groupId>org.koikifw</groupId>
      <artifactId>koiki-starter-session-jdbc</artifactId>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-cache</artifactId>
    </dependency>
    <dependency>
      <groupId>com.github.ben-manes.caffeine</groupId>
      <artifactId>caffeine</artifactId>
    </dependency>
    <dependency>
      <groupId>org.flywaydb</groupId>
      <artifactId>flyway-database-postgresql</artifactId>
      <scope>runtime</scope>
    </dependency>
    <dependency>
      <groupId>org.postgresql</groupId>
      <artifactId>postgresql</artifactId>
      <scope>runtime</scope>
    </dependency>
    <dependency>
      <groupId>org.jspecify</groupId>
      <artifactId>jspecify</artifactId>
    </dependency>

    <dependency>
      <groupId>org.koikifw</groupId>
      <artifactId>koiki-archunit-rules</artifactId>
      <scope>test</scope>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-test</artifactId>
      <scope>test</scope>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-webmvc-test</artifactId>
      <scope>test</scope>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-security-test</artifactId>
      <scope>test</scope>
    </dependency>
    <dependency>
      <groupId>org.koikifw</groupId>
      <artifactId>koiki-testing</artifactId>
      <scope>test</scope>
    </dependency>
  </dependencies>

  <build>
    <plugins>
      <plugin>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-maven-plugin</artifactId>
        <executions>
          <execution>
            <goals>
              <goal>repackage</goal>
            </goals>
          </execution>
        </executions>
      </plugin>
    </plugins>
  </build>

</project>
```

### 3.2 Transformation rules

| Area | Rule |
|---|---|
| Parent | Spring Boot ParentをKOIKI Parent `0.1.0-SNAPSHOT`へ置換し、`<relativePath/>`を空で明示する |
| Coordinates | `jp.co.himacs:ext-reference:0.0.1-SNAPSHOT`を維持する |
| Java baseline | Initializrの`java.version`は削除し、KOIKI ParentのJava 21 build契約を単一正本とする |
| Dependencies | 現行Reference POMのruntime / test依存をversionなしで宣言し、KOIKI BOMへversion管理を委譲する |
| Packaging | `jar`を明示する |
| Boot plugin | KOIKI Parentはplugin versionだけを管理するため、`repackage` executionを外部POMで明示する |
| Metadata | 空の`url`、`licenses`、`developers`、`scm`は除去する |
| Repository | `<repositories>`、filesystem repository URLおよびstage pathをPOMへ記述しない |
| Prohibited path | Framework sourceへの値付き`relativePath`、`systemPath`または絶対pathを使用しない |
| External artifact | `package`までとし、外部Reference自身をisolated repositoryへ`install`しない |

### 3.3 Transformation result

POMだけを上記仕様へ変換し、次を静的に確認した。Maven build、dependency download、Framework artifactの
stageおよび通常の`.m2/repository`へのinstallは実行していない。

| Check | Result |
|---|---|
| Parent / source lookup | `org.koikifw:koiki-parent:0.1.0-SNAPSHOT` / empty `relativePath` |
| External coordinates | `jp.co.himacs:ext-reference:0.0.1-SNAPSHOT` / `jar` |
| Dependencies | 現行Reference POMと同じ14件、scope差分0、個別version 0 |
| Repository / path injection | repository 0、plugin repository 0、`systemPath` 0 |
| Executable JAR | Spring Boot `repackage` goal 1件 |
| Transformed POM SHA-256 | `E4E0F1965FC88F9827C10DC94ADCC7D0C3D4FBBF1E43D33C821733BFB9BE7B91` |
| Baseline registered-file difference | `pom.xml`だけ。他9ファイルはbaseline hashと一致 |

Baseline対象外の`target/`に存在したIDEまたはローカル実行由来のclass / resource 3件は、Initializr baselineや
POM変換成果物には含めず、source materialize前に除去した。

### 3.4 Source materialization result

Initializr生成のsample main / testを除去し、現行`koiki-reference-app/src`の148ファイルを同じrelative pathで
materializeした。production source、resource、template、migrationおよびpropertiesは元Referenceと一致する。
package名`org.koikifw.reference`はReference同一性のため維持し、Customer base packageの候補とは扱わない。

現行Reference testには`org.koikifw.starter.api.internal.KoikiApiAutoConfiguration`を参照するwhite-box testが1件ある。
外部側ではこのMVC sliceを通常のSpring Boot auto-configurationとPostgreSQL Testcontainersを使う統合contextへ変更し、
業務test本体を維持したままinternal importを除去した。元Referenceとのpath差分は0、内容差分はこのtest 1件だけであり、
外部Reference全Java sourceのKOIKI internal参照は0件である。

## 4. Isolated repository Tooling

`build-support/adoption-readiness-verification/invoke-external-reference-boundary-verification.ps1`を追加した。
`BaselinePreflight`はMavenを起動せずInitializr baselineを再照合し、`Full`は変換済みPOM、承認済み14依存、
production sourceのKOIKI internal参照0件を検査してから、承認済みP4-AR6 R2 Toolingへisolated stage / build /
artifact不変 / cleanup検証を委譲する。PowerShell parser検査とPOM変換前の`BaselinePreflight`はPASSした。

実行環境がJDKのCDS警告をversionより先に出力することが判明したため、R2 ToolingのJava / Maven version取得を
該当version行の明示抽出へ補正した。stage / manifest / ownership marker / cleanup契約は変更していない。

stageの親directoryとしてRepository外に`C:\KOIKI-JAVAWEB-BIZ-APP\local-artifact-stages`を確保し、空であることを
確認した。runごとに未作成の子directoryを渡し、Tooling所有markerを照合したcleanupに限定する。POMへstage pathや
filesystem repository URLは記述しない。全runでstage rootはcleanupされ、親directoryは空へ戻った。

## 5. Full build result

### 5.1 Execution history

| Run | Result | Evidence / finding |
|---|---|---|
| Initial Full | FAIL | Docker daemon停止によりTestcontainers context 26件がERROR。Framework stageは成功、stage cleanupはPASS。manifest SHA-256 `7F2457CEB6219F8EA8880BAA191AF2EF0FB1B4F6C106C071F75C3A07B468045B` |
| `r2` | PASS / superseded | Rancher Desktop起動後に99 test PASS。CDS警告をJava / Maven version欄へ記録するEvidence品質上の不備を検出。manifest SHA-256 `17686C6B083A769AE6A04333D114837A0171D565D2A74F65FBBE18189AB651DA` |
| `r3` | PASS / authoritative | version取得補正後に同じFull検証を再実行。manifestとsidecarを最終Evidenceとする |

失敗runを削除または成功へ読み替えず、環境前提の診断Evidenceとして保持する。`r2`もTooling補正前の成功履歴として
保持するが、最終判定には`r3`を使用する。

### 5.2 Authoritative evidence

| Evidence | Value |
|---|---|
| Manifest file | `external-reference-full-20260925-r3.json` |
| Manifest SHA-256 | `6FE89529D96B6F6BCF441594DB44DE7C8A6F12C8A1AD136A44477833A9CDA72B` |
| Sidecar | manifest hashと一致 |
| Framework source | `f7ad1410f42a2f838eada66e0ba5eb82fd7455f4` / dirty `false` |
| Java / Maven | OpenJDK `21.0.12` / Maven Wrapper `3.9.16` |
| Formal inventory / forbidden content | PASS / PASS |
| Customer-like build | PASS |
| Test | 99 run / 0 failure / 0 error / 0 skipped、Surefire report 25件 |
| KOIKI dependency / payload | resolved artifact 11件 / build前後のpayload不変 `true` |
| Public / internal boundary | internal package reference 0 |
| Architecture Rules | compiledかつ成功したtest 1件 |
| Executable JAR | `ext-reference-0.0.1-SNAPSHOT.jar`、64,451,287 bytes、SHA-256 `E960766BF0D76ACA3B8EBEEF7FBC7A5697022D7AF2CB0DD9F1E0C015946906D8` |
| Stage cleanup | PASS / stage root removed / stage親directoryは空 |
| Testcontainers cleanup | 残存container 0 |

Framework入力には固定commitから作成した差分0の一時local cloneを使用し、最終確認後にcloneを削除した。
通常の`.m2/repository`へKOIKI artifactをinstallせず、外部Reference自身もisolated repositoryへinstallしていない。

## 6. Conclusion

現行Referenceは、Root Reactor外の独立projectから、KOIKI Parentと公開artifactのMaven coordinateだけを使って
compile / test / packageできることを実証した。Framework production code、Public API、正式release unit、migration、
通常のMaven local repositoryおよびworkflowは変更していない。本検証はExternal Reference Boundaryの役目を完了するが、
Customer Application、Project Template、正式release、P4-AR完了またはPhase 4開始の承認を意味しない。
