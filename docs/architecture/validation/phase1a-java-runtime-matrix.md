# Phase 1a Java Runtime Matrix — C4実装・検証計画

**調査日:** 2026年8月27日<br>
**対象branch:** `feature/phase1a-java-runtime-matrix`<br>
**状態:** C4 GATE 1〜2 ACCEPTED / GATE 3 NEXT<br>
**Ownership:** Tooling（runtime compatibility fixture、検証script、CI）<br>
**対象:** Java 21 build artifact、Java 21 / 25 runtime、DoD 1a-6<br>
**開始baseline:** `fc178adfa8e107d5c28d1c3be9e1b1653a5d0554`（PR #18 merge、C3 COMPLETE）<br>

## 1. 目的と完了条件

Phase 1a実行計画のG6、Milestone CおよびDoD 1a-6に基づき、Java 21で一度だけ生成した
Tooling-owned CLI JARが、再compileや再packageなしにJava 21 / 25の両runtimeで起動することを証明する。

C4は次をすべて満たしたときだけ`COMPLETE`とする。

1. Maven WrapperとJDK 21だけで検証用JARを一度生成する。
2. 検査対象classのmajor versionがJava 21の`65`である。
3. build jobが記録したJARのSHA-256と、Java 21 / 25実行前後のSHA-256が一致する。
4. 両runtimeで固定marker、実runtime vendor / versionおよび終了コード`0`を確認する。
5. runtime jobがMaven、`javac`、compile、packageまたはruntime別artifact生成を行わない。
6. fixtureがRoot Reactor、正式4成果物、Public API、snapshot公開およびConsumerへ含まれない。
7. local WindowsとGitHub Actions fresh Ubuntu runnerで同じ検証契約が成功する。

## 2. 作業位置と境界

| 項目 | 内容 |
|---|---|
| Phase / status | Phase 1a / Milestone A・B COMPLETE / C1〜C3 COMPLETE / C4 Gate 1〜2 ACCEPTED / Gate 3 NEXT |
| Ownership | Tooling |
| target | `build-support/runtime-compatibility-fixture/`、独立workflow、Validation記録 |
| 正式module | Root ReactorはBOM、Parent、Architecture Contract、ArchUnit Rulesの4moduleを維持 |
| OpenSpec | Repositoryに採用済みchangeがないため必須前提にしない |
| deferred | Runtime Foundation、Spring Boot実行基盤、DB、Container、Reference、Security、性能、Java 25固有最適化 |

fixtureはJava標準機能だけを用いる最小CLIとする。Spring Boot、Spring Framework、業務語彙、DB、Web、
Virtual Threads、Scoped Values、CDS / AOT、Containerまたはproduction設定を追加しない。

## 3. read-only調査結果

### 3.1 正本と現行build contract

| 確認対象 | 実測・確定事項 |
|---|---|
| Grand Design §6.2 | target bytecodeはJava 21、Java 25は推奨runtimeかつ互換確認対象 |
| Grand Design §21.5 | Java 25系統はJava 21成果物を再compileせず実行し、失敗をrelease blockerとする |
| Phase 1a G6 | `build-support/runtime-compatibility-fixture/`の非配布CLI JAR、major `65`、同一hash、両runtime起動を承認済み |
| Root Reactor | 正式4moduleだけで、実行可能JARは含まれない |
| `koiki-parent` | `<release>21</release>`、build JDKを`[21,22)`へ制限し、Java 25 buildを明示的に拒否 |
| current CI | `ubuntu-24.04` / Temurin 21。runtime matrixは未実装 |
| current ruleset | ID `21140116`、strict。required checksは`Verify (ubuntu-24.04)`と`Public API Compatibility` |

### 3.2 local JDK

2026年8月27日のWindows環境で次を確認した。

| 用途 | runtime |
|---|---|
| build / Java 21 runtime | Eclipse Temurin `21.0.12.1+1-LTS` |
| Java 25 runtime | Eclipse Temurin `25.0.4.1+1-LTS` |
| environment | `JAVA_HOME` / `JAVA21_HOME`は21、`JAVA25_HOME`は25を指す |

Gate 2以降のlocal検証に必要なJDKは揃っている。JDKの配置pathは開発環境固有であり、tracked fileへ
固定しない。

### 3.3 Walking Skeleton資産の扱い

既存`build-support/scripts/verify-class-version.ps1`と`run-with-java25.ps1`は、それぞれ
`walking-skeleton/ws-smoke-lib`のclassと`ws-smoke-app`のSpring Boot JARへ直接依存する。
Phase 0ではTemurin 21 / 25による成功証拠があるが、code、一時座標、Spring Boot runtime、設定および
`Dockerfile.ws`はC4へ昇格させない。検証観点だけを新しいTooling fixtureへ再実装する。

### 3.4 GitHub Actions一次情報

- `actions/setup-java`はTemurinと`java-version: '25'`を正式にサポートする。
- GitHub Actionsのworkflow artifactは、同一workflow内のjob間でbuild成果物を受け渡す用途である。
- `upload-artifact` v4以降のartifact archiveはupload後にimmutableである。
- required checkへpath filter付きworkflowを指定すると、skip時にcheckがPendingのままになる場合がある。
- `workflow_dispatch`はworkflow fileがdefault branchに存在するときだけeventを受け取る。

参照:

- [actions/setup-java](https://github.com/actions/setup-java)
- [Workflow artifacts](https://docs.github.com/en/actions/concepts/workflows-and-actions/workflow-artifacts)
- [Store and share data with workflow artifacts](https://docs.github.com/actions/configuring-and-managing-workflows/persisting-workflow-data-using-artifacts)
- [Workflow syntax for GitHub Actions](https://docs.github.com/en/actions/reference/workflows-and-actions/workflow-syntax)

## 4. 実装計画

### 4.1 予定構成

```text
build-support/runtime-compatibility-fixture/
├── README.md
├── pom.xml
├── fixture/
│   ├── pom.xml
│   └── src/main/java/org/koikifw/buildsupport/internal/runtime/
│       └── RuntimeCompatibilityProbe.java
├── build-runtime-fixture.ps1
└── verify-runtime-fixture.ps1
```

Root Reactorの`<modules>`は変更しない。親POMのimport BOMをRepository内で解決するため、Tooling-owned
aggregatorは既存verification fixtureと同様にBOMとCLI fixtureだけを検証reactorへ含める。
CLI artifactの座標は`org.koikifw.validation`配下の検証専用座標とし、正式release unitへ含めない。

### 4.2 fixture contract

1. main classは`org.koikifw.buildsupport.internal.runtime.RuntimeCompatibilityProbe`とする。
2. 引数で期待Java majorを受け取り、`Runtime.version().feature()`との一致を検証する。
3. 成功時だけ固定marker `KOIKI_RUNTIME_COMPATIBILITY_SUCCESS`と、actual major、
   `java.vendor`、`java.version`を出力する。
4. 期待major不一致または引数不正では非0終了とし、成功markerを出力しない。
5. JAR manifestへmain classを固定し、`java -jar`だけで実行できるようにする。

### 4.3 build contract

`build-runtime-fixture.ps1`は次だけを担当する。

1. 実行JDKが21であることを確認する。
2. Repository Maven WrapperでTooling aggregatorを`clean package`する。
3. JAR内の対象class headerを直接読み、major version `65`を確認する。
4. JAR名、SHA-256、class entry、major version、build Java、commitをmanifestへ記録する。
5. JARとmanifest以外をruntime受け渡し対象にしない。

Java 25で同scriptを実行した場合は、script guardまたはParent Enforcerによりbuild前段で失敗させる。

### 4.4 runtime contract

`verify-runtime-fixture.ps1`はJARとbuild manifestを入力とし、Mavenやcompilerを呼び出さない。

1. 現在の`java`が期待major 21または25であることを確認する。
2. 実行前にJARのSHA-256を計算し、build manifestと照合する。
3. `java -jar <same-jar> <expected-major>`を一度実行する。
4. 固定marker、actual major、vendor / version、終了コード`0`を確認する。
5. 実行後もSHA-256を再計算し、build時および実行前と一致することを確認する。

### 4.5 CI contract

`.github/workflows/runtime-compatibility.yml`を`ci.yml`やsnapshot公開workflowと分離する。

| job | JDK / 処理 | 禁止事項 |
|---|---|---|
| Build Runtime Fixture | Temurin 21、Wrapper build、major / SHA検査、JAR＋manifest upload | Java 25 build、package公開 |
| Java Runtime Compatibility | build jobのimmutable artifactをdownloadし、Java 21、次にJava 25で同一JARを検証 | Maven、`javac`、compile、package、runtime別JAR |

workflowは`contents: read`だけを使用し、secret、PAT、Packages権限、認証済みGit credential、artifact cacheを
runtime jobへ与えない。外部Actionは実装時に公式releaseとcommitを照合し、full commit SHAで固定する。
artifactは検証用の短期保持とし、配布物またはrelease evidenceそのものにはしない。

PR fresh runner、`main` push、nightly scheduleおよび`workflow_dispatch`を対象とする。required check候補の
`Java Runtime Compatibility`にはpath filterを設定しない。Gate 4でfresh runner成功とOwner承認を得た後、
ruleset ID `21140116`へ追加し、既存2 checksとstrict policyを維持する。

## 5. Gate構成

| Gate | 確認対象 | 完了条件 | 状態 |
|---|---|---|---|
| 1 | read-only調査、Ownership、fixture、hash / bytecode / runtime、CI設計 | G6と実装が一対一対応し、後続Phase成果物を含まない | ACCEPTED（2026年8月27日、Shuichi Kataoka） |
| 2 | Tooling fixture、JDK 21 build、major 65、local Java 21 / 25 positive path | 一度生成したJARのhashが両runtime実行前後で一致し、markerとexit 0を確認 | ACCEPTED（2026年8月27日、Shuichi Kataoka） |
| 3 | negative guardsと非配布境界 | Java 25 build、hash改変、期待runtime不一致が各契約位置で失敗し、Root Reactorと正式成果物が不変 | PENDING |
| 4 | independent CI、fresh runner、required check、Evidence、C4 closeout | job間artifact受け渡しで成功し、runtime jobにbuild処理がなく、Owner Review後にC4 COMPLETE | PENDING |

## 6. Stop条件

次に該当した場合は実装を停止し、Gate 1またはG6へ戻す。

- Root Reactor、正式4artifact、Public API inventory、C1 snapshotまたはC2 Consumerへfixtureを含める必要がある。
- Java 25でMaven、`javac`、compile、packageまたは別JAR生成が必要になる。
- Spring Boot、Framework runtime、DB、Web、Container、Reference業務またはJava 25固有APIが必要になる。
- Java 21 / 25で別source、別dependency、別JARまたは別hashを許容する必要がある。
- CIに`contents: read`を超える権限、secret、PAT、Packages accessまたはartifact公開が必要になる。
- Walking Skeletonのcode、POM、一時座標、設定またはDockerfileを再利用する必要がある。
- required check追加時に既存checks、strict policy、PR保護またはbypassなしを維持できない。

Stop条件を、Java 25再compile、release引上げ、rule無効化、hash検査省略またはscope拡大で迂回しない。

## 7. Gate 1 Owner Review項目

Architecture Ownerは次の5項目を確認する。

1. C4をTooling所有・Root Reactor外・非配布fixtureとし、Walking Skeleton資産を直接昇格させない。
2. Maven Wrapper / JDK 21で一度だけCLI JARを生成し、対象class major `65`とbuild SHA-256を固定する。
3. Java 21 / 25で同一JARを実行し、実行前後hash、固定marker、runtime vendor / version、exit `0`を確認する。
4. Java 25 build、hash改変、期待runtime不一致のnegative guardsを設け、runtime jobからbuild処理を排除する。
5. 独立CIを最小権限・full SHA固定で構成し、Gate 4成功とOwner承認後にrequired checkへ追加する。

本計画は承認済みG6を具体化するもので、新規Public API、配布方式またはArchitecture Decisionを追加しない。
上記境界を維持できる限りADR追加・改訂は不要とする。

## 8. Gate 1 Owner Review結果

Architecture Ownerは2026年8月27日に§7の5項目と本実装計画を確認し、Gate 1を承認した。
承認範囲はTooling-owned非配布fixture、JDK 21での一回build、major `65`、同一JARのJava 21 / 25実行、
negative guards、独立CIおよびGate 4承認後のrequired check追加である。

Gate 2ではこの承認範囲内でfixtureとlocal positive pathを実装する。Public API、正式module、配布artifact、
runtime dependencyまたは後続Phase成果物が必要になった場合は実装を停止し、Gate 1へ戻す。

## 9. Gate 2 実装・local Evidence

### 9.1 実装資材

Gate 1承認範囲内で次のTooling-owned資材を実装した。

| 対象 | 役割 |
|---|---|
| `runtime-compatibility-fixture/pom.xml` | Parent build contractを継承し、BOMとfixtureだけを含む検証reactor |
| `fixture/pom.xml` | Java 21 CLI JARと`Main-Class` manifestを生成する非配布POM |
| `RuntimeCompatibilityProbe.java` | 期待runtime majorを照合し、固定markerと実runtime情報を出力するJava標準CLI |
| `build-runtime-fixture.ps1` | JDK 21 build、class header、SHA-256、build manifestを生成 |
| `verify-runtime-fixture.ps1` | Mavenを呼ばず、同一JARのruntime major、marker、exit、実行前後hashを照合 |
| `README.md` | Windows local再現手順と非配布境界 |

fixture packageは`org.koikifw.buildsupport.internal.runtime`、Maven groupIdは
`org.koikifw.validation`である。Spring、KOIKI正式artifactまたは外部dependencyを追加していない。

### 9.2 JDK 21 build Evidence

2026年8月27日にWindows localで次を実行した。

```powershell
pwsh -NoProfile -ExecutionPolicy Bypass `
  -File build-support/runtime-compatibility-fixture/build-runtime-fixture.ps1
```

| 項目 | 実測値 |
|---|---|
| Build JDK | Eclipse Adoptium / Temurin `21.0.12.1` |
| Maven | Repository Wrapper `3.9.16` |
| Maven result | 3 project SUCCESS、fixtureは`release 21`で1 sourceをcompile |
| JAR | `runtime-compatibility-fixture-0.1.0-SNAPSHOT.jar` |
| class entry | `org/koikifw/buildsupport/internal/runtime/RuntimeCompatibilityProbe.class` |
| class major | `65` |
| SHA-256 | `6B0A8CAE12B6B8206F43A3EE95F6C29D5C0041D99C0BD56C52C2CC9DF6BC9275` |
| source state | HEAD `f51b6ca28795aea33c943a69bb81d35c76fda533`＋Gate 2 working tree、manifest `workingTreeDirty: true` |

JARにはCLI class、JAR manifestおよびMaven metadataだけが入り、Walking Skeleton、Spring Boot、
正式KOIKI class、設定またはdependencyを含まない。JARとJSON manifestはignored
`build-support/runtime-compatibility-fixture/target/runtime-artifact/`へ生成した。

### 9.3 Java 21 / 25 runtime Evidence

build完了後にMaven、`javac`、`jar`またはpackage処理を実行せず、次を順に実行した。

```powershell
pwsh -NoProfile -ExecutionPolicy Bypass `
  -File build-support/runtime-compatibility-fixture/verify-runtime-fixture.ps1 `
  -ExpectedJavaFeature 21

pwsh -NoProfile -ExecutionPolicy Bypass `
  -File build-support/runtime-compatibility-fixture/verify-runtime-fixture.ps1 `
  -ExpectedJavaFeature 25
```

| Runtime | vendor / version | marker | SHA-256 before / after | exit |
|---|---|---|---|---|
| Java 21 | Eclipse Adoptium `21.0.12.1` | `KOIKI_RUNTIME_COMPATIBILITY_SUCCESS expected=21 actual=21` | MATCH / MATCH | `0` |
| Java 25 | Eclipse Adoptium `25.0.4.1` | `KOIKI_RUNTIME_COMPATIBILITY_SUCCESS expected=25 actual=25` | MATCH / MATCH | `0` |

両runtimeの実行前後hashは§9.2のSHA-256と一致した。Java 25用のcompile、package、source、dependency、
JARまたはmanifestは生成していない。

### 9.4 Regression・非配布境界

通常のRoot Reactorで`mvnw.cmd --batch-mode --no-transfer-progress clean verify`を再実行し、正式4moduleと
Root aggregatorの全projectが成功した。Architecture Contract 4 tests、ArchUnit Rules 65 testsの
計69 testsはfailure / error / skippedなしである。

Root POMの`<modules>`は正式4moduleのまま一致し、Gate 2のtracked差分は`build-support/`と本Validation記録に
限定される。fixtureの`target/`は既存`.gitignore`により追跡対象外で、C1 snapshot、Public API inventory、
japicmp、Feature TemplateおよびRepository外Consumerを変更していない。

### 9.5 Gate 2 Owner Review対象

1. fixtureがTooling所有・Root Reactor外・非配布で、Java標準機能だけを使用している。
2. JDK 21 / Wrapper buildが成功し、対象class major `65`とbuild SHA-256をmanifestへ固定している。
3. Java 21 / 25で同一JARの実行前後hash、固定marker、actual major、vendor / version、exit `0`が一致する。
4. runtime scriptがMaven、compilerまたはpackage処理を持たず、Java 25向け別artifactを生成していない。
5. 通常Root buildと69 testsが成功し、正式4module、Public APIおよび配布境界が不変である。

Gate 2 Owner承認前はGate 3 negative guardsを実装せず、Gate 2を`ACCEPTED`としない。

## 10. Gate 2 Owner Review結果

Architecture Ownerは2026年8月27日に§9.5の5項目とGate 2の内容・結果を確認し、Gate 2を承認した。
承認対象はTooling-owned非配布fixture、JDK 21 build、class major `65`、build manifest、同一SHA-256の
Java 21 / 25 local実行、固定marker、exit `0`、Root Reactor 69 testsおよび正式4module不変である。

Gate 3では承認済みpositive pathを変更せず、Java 25 build、hash改変、期待runtime major不一致の
3 negative guardsと非配布境界を検証する。CI、required checkおよびC4 closeoutはGate 4まで実装しない。
