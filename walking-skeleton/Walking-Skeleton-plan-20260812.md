> [!NOTE]
> この文書は2026年8月12日に使用したBuild Foundation第1ラウンドの履歴です。
> Flyway、ArchUnit、Tier 2、OpenSpec、Agent Skillsを含むWalking Skeleton全体は完了しています。
> 現行の実行計画は`docs/development/KOIKI-JavaWeb-FW_WalkingSkeleton実装計画_v1.0.md`、
> 完了判定は`docs/architecture/validation/walking-skeleton-phase0-completion.md`を参照してください。

Walking Skeleton実装計画では、最初に **Root Reactor／Parent／BOM、Java 21 Build Contract、NullAway、Java 25 Runtime互換性**を確認し、コンテナビルドの型まで先に試す方針です。
また、これはPhase 1a本実装ではなく、Phase 1aで必要になる Maven Multi-module、`<release>21</release>`、Toolchains、Enforcer、NullAway等が本当に成立するかを先行確認する作業です。

以下の順序で実行するのがよいです。

# 1. 今回の到達点

まず第1ラウンドでは、ここまでに限定します。

```text
WS-BUILD-01
  Root Reactor
      ↓
  BOM
      ↓
  Parent
      ↓
  最小Library Module
      ↓
  最小Spring Boot App
      ↓
  Java 21 Build
      ↓
  Java 25 Runtime
      ↓
  NullAway negative test
      ↓
  Container build
```

Flyway、ArchUnit本格実装、Tier 1/Tier 2は**まだ入れません**。

このラウンドの目的は「KOIKIのビルド骨格が成立するか」だけです。

---

# 2. Walking Skeletonブランチを作る

mainにはMaven Module群を正式配置しない方針なので、専用ブランチで作業します。

PowerShellなら、

```powershell
git switch main
git pull --ff-only
git switch -c walking-skeleton
```

ブランチ名は計画書どおり `walking-skeleton` で十分です。

このブランチは最終的に削除します。

---

# 3. 最初の実験用構造

正規構造を全部作らず、まず以下だけ作ります。

```text
koiki-javaweb-fw/
├── pom.xml
│
├── koiki-dependencies-bom/
│   └── pom.xml
│
├── koiki-parent/
│   └── pom.xml
│
└── walking-skeleton/
    ├── ws-smoke-lib/
    │   ├── pom.xml
    │   └── src/
    │
    └── ws-smoke-app/
        ├── pom.xml
        └── src/
```

ここで `ws-smoke-*` と明示しているのがポイントです。

これは、

```text
koiki-framework
koiki-starters
koiki-reference-app
```

の正式構造ではありません。

**Parent/BOMを利用するConsumerが本当にBuildできるか確認するためだけの捨てModule**です。

---

# 4. Root Reactorの候補を作る

Root `pom.xml` は、まず**Aggregator専用**として試します。

責務は、

```text
root pom.xml
  = Repository全体のReactor
  = module一覧

NOT
  = 顧客向けParent
  = dependency BOM
```

です。

概念的には、

```xml
<packaging>pom</packaging>

<modules>
    <module>koiki-dependencies-bom</module>
    <module>koiki-parent</module>
    <module>walking-skeleton/ws-smoke-lib</module>
    <module>walking-skeleton/ws-smoke-app</module>
</modules>
```

だけに近い状態から開始します。

ここではRoot POMへCompilerやDependency versionを大量に書きません。

**Root ReactorとParentを本当に分離できるか**を見るためです。

---

# 5. BOM候補を作る

次に、

```text
koiki-dependencies-bom
```

を作ります。

Walking Skeletonでまず試す構造は、

```text
KOIKI BOM
   ├─ Spring Boot dependencies BOM import
   ├─ JSpecify
   ├─ NullAway
   ├─ Error Prone
   └─ その他Walking Skeletonで必要なversion
```

です。

ここで重要なのは、

> Spring BootのDependency ManagementをKOIKI BOMから提供できるか

を見ることです。

Spring Bootの具体的なバージョン番号は、Walking Skeleton開始時のKOIKI⇔Spring Boot baselineに合わせて1つ固定します。

この段階では「将来ずっとこのversion」という意味にはしません。

---

# 6. Parent候補を作る

次に、

```text
koiki-parent
```

です。

最初に試す責務は、

```text
Java release = 21
Maven compiler
Toolchains
Enforcer
Test plugins
NullAway
```

です。

そして**候補A**として、

```text
koiki-parent
      ↓ import
koiki-dependencies-bom
```

を試します。

これがWalking Skeletonの重要な検証ポイントです。

つまり、

```text
Root Reactor
  ├── BOM
  ├── Parent → BOM
  ├── ws-smoke-lib → Parent
  └── ws-smoke-app → Parent
```

という構成を同一Reactorで問題なく扱えるか確認します。

ここは「きっと動く」と設計で決めず、**実際に `mvn verify` して決める**ところです。

もしこの形にMaven上の不都合が出たら、その時点でAlternativeを検討します。変な回避策を入れて無理に通さない方がよいです。

---

# 7. 一時的なgroupIdを使う

正式なPackage Namingはまだ確定させません。

Walking Skeletonでは例えば、

```text
dev.koiki.walkingskeleton
```

程度の明らかな一時Namespaceで十分です。

例えば、

```text
dev.koiki.walkingskeleton:koiki-parent
dev.koiki.walkingskeleton:koiki-dependencies-bom
dev.koiki.walkingskeleton:ws-smoke-lib
dev.koiki.walkingskeleton:ws-smoke-app
```

です。

これならWalking Skeletonで使った名前が、そのまま正式仕様になる事故も防げます。

---

# 8. Java 21 Toolchainを準備する

v0.2の正式契約は、

```text
Target bytecode : Java 21
Java 21 Runtime : 必須
Java 25 Runtime : 互換確認
```

です。

まずBuild JDKを21へ固定します。

Windowsでは例えば、

```text
%USERPROFILE%\.m2\toolchains.xml
```

に、

```xml
<?xml version="1.0" encoding="UTF-8"?>
<toolchains>
  <toolchain>
    <type>jdk</type>
    <provides>
      <version>21</version>
    </provides>
    <configuration>
      <jdkHome>C:\path\to\jdk-21</jdkHome>
    </configuration>
  </toolchain>
</toolchains>
```

を置きます。

実際のJDK pathは環境に合わせます。

そのうえでParentに、

```text
maven-compiler-plugin
    release = 21

maven-toolchains-plugin
    JDK = 21

maven-enforcer-plugin
    Build JDK = 21
```

を設定します。

ここはグランドデザインのBuild Contractそのものです。

---

# 9. 最初のPositive Test

Java 21を `JAVA_HOME` にした状態で、

```powershell
.\mvnw.cmd clean verify
```

を実行します。

期待値は、

```text
BUILD SUCCESS
```

です。

ここで、

```text
Root Reactor
Parent
BOM
ws-smoke-lib
ws-smoke-app
```

がすべてReactorに入り、Buildされれば最初の成功です。

記録します。

```text
WS-B01 PASS
Root Reactor / Parent / BOM 分離構成で
multi-module verify成功
```

---

# 10. Java 21 bytecodeを実物で確認する

単にPOMを見るだけではなく、生成classも確認するとよいです。

Java 21のclass file major versionは65なので、

```powershell
javap -verbose <生成されたclass>
```

で、

```text
major version: 65
```

になっていることを確認します。

これで、

> 設定上 `<release>21</release>` と書いてある

ではなく、

> 実際の成果物がJava 21 bytecode

まで確認できます。

---

# 11. EnforcerのNegative Test

次にわざとBuild JDKを変えます。

例えばJava 25を、

```powershell
$env:JAVA_HOME = $env:JAVA25_HOME
$env:PATH = "$env:JAVA_HOME\bin;" + $env:PATH
```

として、

```powershell
.\mvnw.cmd clean verify
```

します。

ここでは逆に、

```text
BUILD FAILURE
```

が正解です。

つまり、

> 開発者が間違ってJava 25でKOIKI本体をBuildしようとしても、Build JDK 21契約をEnforcerが破らせない

ことを確認します。

記録は、

```text
WS-B02 PASS

Java 25でMaven buildを実行
→ Enforcerにより意図どおりfailure
```

です。

---

# 12. Java 25は「Build」ではなく「Runtime」で確認する

その後、Java 21でBuildし直します。

```powershell
$env:JAVA_HOME = $env:JAVA21_HOME
.\mvnw.cmd clean package
```

生成した `ws-smoke-app` のSpring Boot JARを、**Java 25のjavaコマンドで直接起動**します。

概念的には、

```powershell
& "$env:JAVA25_HOME\bin\java.exe" `
  -jar walking-skeleton\ws-smoke-app\target\ws-smoke-app-*.jar
```

これが正常起動すれば、

```text
Java 21 build
      ↓
Java 25 runtime
```

の互換性確認ができます。

ここはBuild JDKテストと混ぜないことが重要です。

---

# 13. JSpecify / NullAwayを追加する

基本Buildが通った後でNull Safetyを追加します。

順序は、

```text
Multi-module PASS
      ↓
Java 21 Contract PASS
      ↓
NullAway導入
```

の方が切り分けやすいです。

`ws-smoke-lib` のpackageに、

```java
@NullMarked
package dev.koiki.walkingskeleton.smoke;
```

を設定します。

そのうえで、まず正常コードで、

```powershell
.\mvnw.cmd clean verify
```

をPASSさせます。

---

# 14. NullAway Negative Test

次に、わざとNull contract違反を作ります。

例えば概念的には、

```java
public String message() {
    return null;
}
```

のようなコードです。

`@NullMarked`配下なので、これを、

```powershell
.\mvnw.cmd clean verify
```

して、

```text
BUILD FAILURE
NullAway violation
```

になれば成功です。

その後違反コードを戻し、

```powershell
.\mvnw.cmd clean verify
```

が再びPASSするところまで確認します。

この

```text
PASS
→ 意図的違反でFAIL
→ 修正してPASS
```

までやるのが重要です。

Phase 1aの思想も「規約を書いた」ではなく「破るとBuildが落ちる」です。

---

# 15. Maven Wrapperもこの段階で試す

最初だけローカルMavenでWrapperを生成してよいですが、それ以降は、

Windows:

```powershell
.\mvnw.cmd verify
```

Linux/macOS:

```bash
./mvnw verify
```

へ統一します。

Walking Skeleton終了時には、

```text
Maven本体Version
Maven Wrapper Version
Build JDK Version
Target release
```

を検証結果に残しておくと、Phase 1aでそのまま採用判断できます。

---

# 16. コンテナビルドの型を試す

Walking Skeleton計画ではこのBuild Foundationと同時に、

> Multi-stage build + layer extract + JRE base + non-root

まで確認します。

この段階では正式Dockerfileにはしません。

概念的には、

```dockerfile
FROM <JDK-build-image> AS builder

WORKDIR /workspace

COPY . .

RUN ./mvnw clean package ...

RUN java -Djarmode=tools \
    -jar app.jar extract \
    --destination extracted


FROM <JRE-runtime-image>

WORKDIR /app

COPY --from=builder /workspace/extracted/ ./

USER <non-root-user>

ENTRYPOINT ["java", "-jar", "..."]
```

の型がSpring Boot成果物で成立するか確認するだけです。

確認項目は、

```text
docker build 成功
container起動成功
JRE imageで起動
non-rootで実行
layer extractが機能
```

です。

Base imageや正式なセキュリティ設定はここでは確定しません。

---

# 17. 検証ログを残す

コードは捨てますが、**結果は捨てません**。

例えば、

```text
docs/architecture/validation/
└── walking-skeleton-build-foundation.md
```

に、

| ID     | 検証                 | 結果          | 採用判断           |
| ------ | ------------------ | ----------- | -------------- |
| WS-B01 | Reactor/Parent/BOM | PASS        | 分離方式を継続        |
| WS-B02 | Enforcer           | PASS        | Build JDK 21強制 |
| WS-B03 | release 21         | PASS        | class major 65 |
| WS-B04 | Java 25 runtime    | PASS        | 互換対象として継続      |
| WS-B05 | NullAway           | PASS        | Parentから必須適用   |
| WS-B06 | Container          | PASS/CHANGE | Phase 1aへ設定持込  |

という形で残します。

ここで `PASS WITH CHANGE` が出るのは全く問題ありません。

Walking Skeletonの目的は、それを見つけることです。

---

# 18. このラウンドの終了条件

以下が全部確認できたら「リポジトリ・ビルドの土台」はいったん完了です。

```text
[ ] Root Reactor / Parent / BOM 分離でbuild成功
[ ] Java 21 Build JDK強制
[ ] <release>21</release> が実際にmajor 65
[ ] 想定外Build JDKでEnforcer failure
[ ] Java 21成果物がJava 25 Runtimeで起動
[ ] @NullMarked + NullAwayが正常コードでPASS
[ ] NullAway違反を入れるとBuild FAIL
[ ] 修正後再びPASS
[ ] Maven Wrapperから同じBuildが可能
[ ] Multi-stage / layer / JRE / non-root containerが成立
[ ] 結果をvalidation文書へ記録
```

ここまで終わったら、Walking Skeleton計画どおり、**次はFlywayの2階層構成**へ移ります。Flywayを早い段階に置いた理由も、「不成立だった場合の設計影響が大きいから」です。

なお、最初のMaven構造については、いきなり「これがKOIKIの正解」とせず、**Root → BOM → Parent → smoke-lib/app の候補構成を実際に通してから採否を判断する**のが今回のWalking Skeletonの肝になります。
