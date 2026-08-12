# KOIKI-JavaWeb-FW — Phase 0 Walking Skeleton Base

この構成は、`KOIKI-JavaWeb-FW グランドデザイン v0.2` と
`Walking Skeleton 実装計画 v1.0` に基づき、Phase 0 の最初の検証
「リポジトリ・ビルドの土台」を開始するための **捨てる前提の検証用ベース** です。

## この構成で確認するもの

- Root Reactor / Parent / BOM の責務分離
- Maven Multi-module build
- Java 21 ターゲットバイトコード
- Maven Enforcer による Build JDK 21 の強制
- Maven Toolchains による JDK 21 選択
- JSpecify `@NullMarked` + NullAway
- Java 21 build artifact の Java 25 runtime 起動
- Spring Boot の最小 executable JAR
- Spring Boot tools jarmode を使ったコンテナレイヤ抽出の型

この時点では、Flyway / ArchUnit 本格ルール / Tier 1 / Tier 2 /
Spring Modulith 本格検証は含めません。

## 重要

`walking-skeleton/` 以下は正式な `koiki-reference-app` ではありません。
検証終了後はコードを引き継がず、得られた設定値・知見だけを Phase 1a に反映します。

## 一時 Maven 座標

Walking Skeleton の名前を正式仕様化しないため、一時 namespace を使用します。

```text
dev.koiki.walkingskeleton
```

正式な `groupId` / Java base package は Walking Skeleton 後に確定します。

## 前提

- JDK 21
- Java 25（runtime compatibility test 用）
- Maven 3.9.16
- Docker / OCI-compatible container engine（コンテナ検証を行う場合）

## Maven Wrapper

この ZIP には Maven Wrapper を **公式Pluginから生成するためのbootstrap script** と
期待設定templateを含めています。

最初にローカル Maven で次を実行してください。

PowerShell:

```powershell
.\build-support\scripts\bootstrap-maven-wrapper.ps1
```

sh:

```sh
./build-support/scripts/bootstrap-maven-wrapper.sh
```

以降は、

```powershell
.\mvnw.cmd clean verify
```

または、

```sh
./mvnw clean verify
```

を使用します。

## 最初の実行

### 1. Build環境確認

```powershell
.\build-support\scripts\check-build-environment.ps1
```

Maven自体もJava 21で実行してください。
`koiki-parent` のEnforcerがJava 21以外を拒否します。

### 2. JDK toolchain確認

```powershell
mvn org.apache.maven.plugins:maven-toolchains-plugin:3.3.0:display-discovered-jdk-toolchains
```

必要に応じて `build-support/maven/toolchains.xml.example` を
`%USERPROFILE%\.m2\toolchains.xml` または `~/.m2/toolchains.xml`
の参考にしてください。

### 3. Build

```powershell
mvn clean verify
```

Wrapper生成後:

```powershell
.\mvnw.cmd clean verify
```

### 4. Java 21 class version確認

```powershell
.\build-support\scripts\verify-class-version.ps1
```

Java 21 の class major version `65` を確認します。

### 5. Java 25 runtime確認

Java 21でpackage後:

```powershell
$env:JAVA25_HOME = "C:\path\to\jdk-25"
.\build-support\scripts\run-with-java25.ps1
```

## NullAway negative test

正常系コードは最初からbuildが通る状態を意図しています。
意図的違反の例は `walking-skeleton/negative-tests/nullaway/README.md` にあります。

1. `mvn clean verify` → PASS
2. READMEの違反コードを一時的に適用
3. `mvn clean verify` → FAIL
4. 元に戻す
5. `mvn clean verify` → PASS

## Container

最初にJARを作成します。

```powershell
mvn clean package
docker build -f walking-skeleton/ws-smoke-app/Dockerfile.ws -t koiki-ws-smoke .
docker run --rm koiki-ws-smoke
```

## Walking Skeleton開始時のVersion Snapshot

| 項目 | Version |
|---|---:|
| Spring Boot | 4.1.0 |
| Maven | 3.9.16 |
| Maven Compiler Plugin | 3.15.0 |
| Maven Enforcer Plugin | 3.6.3 |
| Maven Toolchains Plugin | 3.3.0 |
| Error Prone | 2.50.0 |
| NullAway | 0.13.8 |
| JSpecify | 1.0.0 |

これらは **Walking Skeletonの検証用固定値** であり、KOIKIの恒久固定値ではありません。
