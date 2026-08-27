# Runtime Compatibility Fixture

Phase 1a C4 / DoD 1a-6のためのTooling-owned非配布fixtureです。Java 21で一度だけ生成したCLI JARが、
同一SHA-256のままJava 21 / 25で起動することを検証します。

このdirectoryはRoot Reactor、正式4artifact、GitHub Packages、Public API inventory、japicmp、
Feature TemplateおよびRepository外Consumerに含めません。Walking Skeletonのcode、POM、座標または
Spring Boot runtimeも再利用しません。

## Local positive path

`JAVA_HOME`と`JAVA21_HOME`をJDK 21、`JAVA25_HOME`をJDK 25へ設定して実行します。

```powershell
pwsh -NoProfile -File build-support/runtime-compatibility-fixture/build-runtime-fixture.ps1
pwsh -NoProfile -File build-support/runtime-compatibility-fixture/verify-runtime-fixture.ps1 `
  -ExpectedJavaFeature 21
pwsh -NoProfile -File build-support/runtime-compatibility-fixture/verify-runtime-fixture.ps1 `
  -ExpectedJavaFeature 25
```

build scriptはRepository Maven WrapperとJDK 21で検証reactorを`clean package`し、次を
`target/runtime-artifact/`へ生成します。

- `runtime-compatibility-fixture-0.1.0-SNAPSHOT.jar`
- `runtime-compatibility-manifest.json`

manifestにはsource commit、working tree状態、JAR SHA-256、対象class entry、class major version、build JDK情報を記録します。
runtime scriptはMaven、`javac`、compileまたはpackageを実行せず、manifestと同じJARに対して
`java -jar`だけを実行します。JAR hashは実行前後に照合します。

## Local negative guards

positive path成功後、次のscriptでJava 25 build、copy JARのhash改変、runtime major不一致が
期待どおり失敗することを検証します。

```powershell
pwsh -NoProfile -File `
  build-support/runtime-compatibility-fixture/verify-runtime-negative-guards.ps1
```

hash改変はOS temp内のcopyだけに行い、finallyで検証済みtemp pathを削除します。元JARのSHA-256を
各guard後に確認し、最後にJava 21 / 25 positive pathを再実行します。tracked source、build output原本、
Root Reactorまたは正式artifactは変更しません。

## CI

`.github/workflows/runtime-compatibility.yml`はJDK 21 build jobでJARとmanifestを生成・検証し、短期workflow
artifactとしてruntime jobへ渡します。runtime jobはdownloadした同一JARをJava 21 / 25で実行し、Maven、
compilerまたはpackage処理を行いません。remote Evidenceとrequired check反映はGate 4 Owner Reviewで扱います。
