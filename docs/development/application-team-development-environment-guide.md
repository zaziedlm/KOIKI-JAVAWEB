# KOIKI-JavaWeb-FW アプリ開発チーム向け 開発環境構築手順 v0.1

## 1. 本書の目的、対象読者、現在の位置付け

本書は、業務アプリ開発チームがKOIKI-JavaWeb-FWを利用する自チームのCustomer Applicationを開発し始めるまでの、
端末準備、Repository配置、KOIKIへの接続、初回build、ローカル実行を一本の手順にまとめます。

| 項目 | 内容 |
|---|---|
| 対象読者 | KOIKIを採用候補とする業務アプリ開発チーム |
| 前提として読む文書 | [アプリ開発チーム向け引継ぎガイド](application-team-handoff-guide.md)の§3〜§6 |
| 採用するRepository構成 | P4-AR6のR2（Framework / Customerの別Git Repositoryを同じ作業directoryへ並置し、isolated Maven repositoryで接続） |
| 手順の根拠 | [External Reference Boundary Validation](../architecture/validation/adoption-external-reference-boundary-validation.md)、[Greenfield Bootstrap Smoke Validation](../architecture/validation/adoption-greenfield-bootstrap-smoke.md)、[P4-AR1 Environment Preflight](../architecture/validation/pre-phase4-p4-ar1-environment-preflight.md) |
| 本書に含まれないもの | Project Template、正式release repository、案件固有の業務設計、production deployment、IDE / editorの選定や設定の統一強制、Dev Container環境の構築方法 |

本書は、2026-09-25に実施した外部Reference / Greenfield bootstrap検証で実際に通した構成を、アプリ開発者の作業順へ
並べ直したものです。R2は正式artifact repositoryが整備されるまでの**移行期の推奨候補**であり、実チームでの再現性は
[P4-AR6 実チーム受入worksheet](p4-ar6-actual-team-reception-worksheet.md)で確認します。本書を読んだだけで
正式release、support条件またはPhase 4開始が承認されたとは扱いません。

### 1.1 本書の読み方

本書は3つの層で構成しています。

| 層 | 該当箇所 | 内容 |
|---|---|---|
| 共通手順 | §2〜§7 | IDE / editorに依存しない手順です。buildとローカル起動はCLIで行い、合否もCLIの結果で判断します |
| VS Codeを採用する場合 | §8 | 検証で使用したVS Codeでの設定例です。他のIDE、editor、CLI中心の開発を採用する場合は読み飛ばせます |
| Dev Container / Linuxでの想定 | 各手順末尾の付記、§9 | Linux環境で開発する場合に事前に想定される差分です |

共通手順とVS Codeの手順は、**Windows 11端末で実際に検証した手順**です。アプリチームがDev Container等のLinux環境で
開発する場合に備え、各手順の末尾に次の形式の付記を置いています。

> **Dev Container / Linuxでの想定**（未検証）
>
> そのステップをLinux環境で行うときに、事前に想定される差分と注意点。

付記は、KOIKI側の検証環境、Tooling実装、Repository設定から予見できる内容をまとめたものです。KOIKI側では
Linux環境でこれらの手順を実行していません。どのIDE / editorを使うか、Dev Container環境をどう構成するか
（base image、JDKの導入方法、DBの用意の仕方、Dockerの利用方法等）はアプリチームが決めるものとし、本書では定めません。

## 2. 全体像

### 2.1 何をどこに置くか

R2では、Framework checkoutとCustomer Repositoryを同じ作業directoryに並べますが、**同じ場所に並べることと、
同じGit Repositoryや同じMaven reactorへ入れることは別**です。次の配置を使います。

```text
C:\koiki-dev\                             # 作業用の親directory。Git Repositoryにはしない
├── KOIKI-JAVAWEB\                        # Framework Git Repository（読み取り・診断用のclean checkout）
├── customer-app\                         # 自チームのCustomer Git Repository
├── local-artifact-stages\                # isolated Maven repositoryの親。実行ごとに作成・削除
└── evidence\                             # build検証のmanifestとSHA-256 sidecar
```

| 置き場所 | Owner | 使い方 | してはいけないこと |
|---|---|---|---|
| `KOIKI-JAVAWEB\` | Framework | Public API、Starter、Reference、検証Toolingを読む。R2 build scriptの実行元 | Customer codeを置く、未承認の差分を残す、Reference sourceをコピー元にする |
| `customer-app\` | Customer | 業務module、設定、migration、testを所有する | Framework sourceへの`relativePath` / `systemPath`、Framework moduleを`<modules>`へ追加 |
| `local-artifact-stages\` | Tooling | build実行ごとに空の子directoryを作り、終了時にToolingが削除する | 通常の`~/.m2/repository`を指定する、Git管理する |
| `evidence\` | 実行者 | 実行結果のmanifestを保管する | credentialや業務データを書き込む |

IDEのworkspace定義（VS Codeの`.code-workspace`等）を使う場合は、各開発者のlocalファイルとして親directoryに置き、
どちらのRepositoryにもcommitしません（§8.2）。

### 2.2 IDE / editorに関係なく守ること

| 観点 | ルール | 理由 |
|---|---|---|
| Git | commit先のRepositoryを確認してからcommitします | Framework / Customerの履歴を混在させないため |
| Framework checkout | 編集ファイルやIDE生成ファイルを残さず、`git status`がcleanな状態を保ちます | R2 scriptはdirtyなFramework checkoutを拒否します |
| Maven | Customer POMからFrameworkを`relativePath`、`systemPath`、`<modules>`で参照しません | Framework source treeへの偶発依存を防ぐため |
| Framework sourceの利用 | 学習・診断・差分確認に使います | ReferenceやFramework internalをCustomerへコピーしないため |
| build / testの合否 | §6のR2 buildの結果で判断します | IDE上の表示は、KOIKI artifactの解決状況により実際のbuildと食い違う場合があります（§8.3） |

Framework checkoutでは`.classpath`、`.project`、`.settings/`、`.vscode/`、`.idea/`、`*.iml`は`.gitignore`対象です。一方、
Eclipse系のJava toolingがannotation processing用に生成する`.factorypath`は現在ignore対象ではありません。Framework checkoutに
`.factorypath`が現れた場合は`git status`がdirtyになり、R2 buildが止まります。その場合は内容を確認し、
Frameworkへの差分として扱わずに削除するか、ignore追加をFramework Ownerへ依頼します。

> **Dev Container / Linuxでの想定**（未検証）
>
> - 上記4つのdirectoryがすべてcontainerから見える必要があります。親directoryごとcontainerへmountする構成が単純です。
> - Dev Container定義（`.devcontainer/`等）はCustomerの開発環境設定なので、Customer Repository側で管理します。
>   KOIKIは公式のDev Container定義やimageを提供しません。
> - Framework checkoutへ`.devcontainer/`等を置きません。`.devcontainer/`はFrameworkの`.gitignore`対象ではないため、
>   置くとR2 buildが止まります。
> - 親directoryはWindows filesystemではなく、Linux filesystem上（WSL2のdistribution内等）に置くことを推奨します（理由は§4.1の付記）。

### 2.3 Framework artifactの受け渡し

現時点のKOIKI `0.1.0-SNAPSHOT`は正式なmanaged Maven repositoryから配布されていません。Customer projectだけで
`mvnw clean verify`を実行してもKOIKI Parent / Starterは解決できず、それが正常な状態です。
KOIKI artifactは次の流れで受け渡します。

```text
Framework clean checkout（固定commit）
  → invoke-p4-ar6-r2-handoff.ps1 が空のisolated Maven repositoryへformal release unitをstage
    → 同じrepositoryからCustomer POMをclean verify（Architecture Rules実行を含む）
      → manifest + SHA-256 sidecarをevidenceへ出力
        → stageを削除
```

詳細な契約は[P4-AR6 Actual-project Handoff Contract §3](../architecture/validation/pre-phase4-p4-ar6-actual-project-handoff-contract.md)と
[Adoption Readiness Verification README](../../build-support/adoption-readiness-verification/README.md)を正本とします。

> **Dev Container / Linuxでの想定**（未検証）
>
> この受け渡しの仕組みはOSに依存しません。Linux環境でもR2 script（PowerShell）を使います。bash版はありません（§6.1の付記）。

## 3. 端末の前提を揃える

### 3.1 必要なソフトウェア

次の表の「検証済みversion」は、P4-AR1と2026-09-25の検証で実際に使用した値です。これより新しいpatch versionでも
動作する見込みですが、問題が出たときはまずこの表との差分を記録します。

| ソフトウェア | 要件 | 検証済みversion | 備考 |
|---|---|---|---|
| OS | Windows 11 x64 | Windows 11 / amd64 | 本書のcommandはPowerShell 7前提です |
| JDK（build） | Java 21 | Temurin 21.0.12.1 | `JAVA_HOME`と`JAVA21_HOME`に設定します |
| JDK（runtime互換確認） | Java 25 | Temurin 25.0.4.1 | 任意。Framework側のruntime互換確認で使用します |
| Maven | Wrapperを使用 | Wrapper 3.3.4 / Maven 3.9.16 | 個別installは不要です |
| PowerShell | 7系（`pwsh`） | 7.6.6 | R2 scriptはWindows PowerShell 5.1を前提にしません |
| Git | 任意の現行版 | 2.55.0.windows.3 | |
| Docker互換runtime | PostgreSQL containerを起動できること | Rancher Desktop 29.5.3 | Docker Desktop等でも可 |
| IDE / editor | 任意 | VS Code 1.138.0 / x64 | VS Codeを採用する場合は§8 |

> **Dev Container / Linuxでの想定**（未検証）
>
> - container内に次が必要です: JDK 21（`javac`を含むJDK。JREだけでは不可）、PowerShell 7（`pwsh`）、Git、
>   `wget`または`curl`（Maven Wrapperがwrapper jarのdownloadに使用します）。
> - Framework CIはGitHub-hosted `ubuntu-24.04`でbuildし、`pwsh`の検証scriptも実行しています。production runtimeもLinuxを
>   想定しているため、Linux環境での開発はbuild / 実行環境との差を小さくする方向です。
> - PostgreSQLやTestcontainersのためにcontainer内からDockerを使うかどうかは、Dev Containerの構成次第です（§7の付記）。

### 3.2 環境変数

| 変数 | 値 | 用途 |
|---|---|---|
| `JAVA_HOME` | JDK 21のhome | Maven Wrapper、R2 script |
| `JAVA21_HOME` | JDK 21のhome | Framework検証script |
| `JAVA25_HOME` | JDK 25のhome（任意） | Framework runtime互換検証 |

設定後、新しいPowerShell 7 terminalで確認します。Framework checkout後であれば、同梱scriptでまとめて確認できます。

```powershell
java -version
pwsh --version
git --version
docker version

# Framework clone後
pwsh -NoProfile -File C:\koiki-dev\KOIKI-JAVAWEB\build-support\scripts\check-build-environment.ps1
```

> **Dev Container / Linuxでの想定**（未検証）
>
> - `check-build-environment.ps1`は`mvnw.cmd`を固定で呼び出すため、**Linuxでは使えません**。代わりに個別に確認します。
>
>   ```bash
>   java -version && javac -version
>   echo "$JAVA_HOME" "$JAVA21_HOME"
>   pwsh --version
>   (cd <Framework checkout> && ./mvnw -version)
>   ```
>
> - `JAVA_HOME`はR2 scriptが直接参照します。container内の環境変数として設定されていることを確認します。

### 3.3 社内SSLインスペクションProxy配下の場合

Netskope等のSSLインスペクションProxy配下では、Maven Central等へのHTTPS通信で`PKIX path building failed`が発生します。
Windowsの証明書ストアには社内Root CAが登録済みでも、JDKの`cacerts`には反映されていないためです。
端末ごとに一度、Maven buildに使うJDK 21（必要なら25も）へ社内Root CAをインポートします。

```powershell
pwsh -NoProfile -File C:\koiki-dev\KOIKI-JAVAWEB\build-support\scripts\import-corporate-root-ca.ps1 -JdkHome "<JDK 21のhome>"
```

IDEもJavaで動くtool（Language Server等）を使う場合、そのtoolが使うJDK / JREにも同じ対応が必要です。
VS Codeでの対象は§8.1にまとめています。具体的なcommandと設定例は
[build-support README「社内SSLインスペクションProxy環境でのMavenビルドエラー対応」](../../build-support/README.md#社内sslインスペクションproxy環境でのmavenビルドエラー対応)
と[import-corporate-root-ca.ps1](../../build-support/scripts/import-corporate-root-ca.ps1)を正本とします。

> **Dev Container / Linuxでの想定**（未検証）
>
> - `import-corporate-root-ca.ps1`はWindowsの証明書ストアと`keytool.exe`を前提にしているため、**Linuxでは使えません**。
>   社内Root CAをWindowsからPEM形式（`.crt`）でexportし、container側へ取り込む方法をアプリチームで用意します。
> - 取り込み先は**OSの証明書ストアとJDKの`cacerts`の両方**です。Maven Wrapperは初回にwrapper jarを`wget` / `curl`で
>   downloadし、このときOSの証明書ストアを使います。Maven本体とdependencyのdownloadではJDKの`cacerts`を使います。
> - container imageのpullはDocker runtime側、container内で動かすIDE server（VS Code Server等）のdownloadはIDE側の
>   Proxy設定の対象です。container内の証明書設定とは別に確認します。

## 4. RepositoryとCustomer projectを配置する

### 4.1 作業用directoryとFramework checkout

Windowsではclone、Maven local repository、Java packageの組合せでpathが長くなるため、**短いpath**を使います。

```powershell
New-Item -ItemType Directory -Force -Path C:\koiki-dev, C:\koiki-dev\local-artifact-stages, C:\koiki-dev\evidence | Out-Null
git clone <Framework Repository URL> C:\koiki-dev\KOIKI-JAVAWEB
git -C C:\koiki-dev\KOIKI-JAVAWEB checkout <Framework Ownerから指定されたcommitまたはtag>
git -C C:\koiki-dev\KOIKI-JAVAWEB rev-parse HEAD
```

使用するFramework commitは、Framework Ownerと合意したものに固定します。最新の`main`へ自動追随しません。
記録した40桁commitは§6のbuildで使います。

> **Dev Container / Linuxでの想定**（未検証）
>
> - **Framework / Customer RepositoryはLinux filesystem上でcloneします。** Windows側（`C:\`、WSLからは`/mnt/c/`）で
>   cloneしたcheckoutをcontainerへmountすると、次の問題が予見されます。
>
>   | 予見される問題 | 理由 |
>   |---|---|
>   | Frameworkの`./mvnw`が`/usr/bin/env: 'sh\r'`等で起動しない | Frameworkの`.gitattributes`では`mvnw`が`text=auto`のため、Windowsでcheckoutすると改行がCRLFになります |
>   | R2 buildが`Framework worktree is not clean`で止まる | Windows filesystemをLinuxから見ると、実行権限や改行の見え方が変わり、`git status`に差分が出る場合があります |
>   | Maven buildやIDEのproject importが遅い | Windows filesystemとcontainer間のfile I/Oが遅いためです |
>
> - 同じcheckoutをWindows側とDev Containerの両方で使い回しません。
> - Windowsのpath長の問題はLinuxでは発生しません。
> - bind mountの所有者の違いにより、`git`が`detected dubious ownership`を表示する場合があります。所有者を確認したうえで
>   `git config --global --add safe.directory <path>`で対象Repositoryを登録します。

### 4.2 Customer projectの生成

[Spring Initializr](https://start.spring.io/)（Web版、またはIDEのInitializr連携）で次を選び、
`C:\koiki-dev\customer-app`として生成します。

| 設定 | 値 |
|---|---|
| Project / Language | Maven / Java |
| Spring Boot | KOIKI Parentと同じ系列（検証時は4.1.1） |
| Java | 21 |
| Packaging / Configuration | Jar / Properties |
| Group / Artifact | 自チームのcoordinate |
| Dependencies | 追加しない（KOIKI Starterは§5でPOMへ追加します） |

生成後、Customer Repositoryとして初期化します。

```powershell
git -C C:\koiki-dev\customer-app init
git -C C:\koiki-dev\customer-app add .
git -C C:\koiki-dev\customer-app commit -m "chore: initial Spring Initializr project"
```

Framework Repositoryの配下、またはFramework RepositoryのMaven reactorの`<modules>`へCustomer projectを置きません。

> **Dev Container / Linuxでの想定**（未検証）
>
> Initializrが生成する`.gitattributes`は`mvnw`をLF、`*.cmd`をCRLFに固定するため、Customer Repositoryの改行は
> WindowsとLinuxのどちらでcloneしても問題になりにくい構成です。この`.gitattributes`は削除しません。

## 5. Customer POMとsourceをKOIKIへ接続する

### 5.1 POM

Initializrが生成した`spring-boot-starter-parent`をKOIKI Parentへ置き換え、必要なStarterだけを追加します。
次はGreenfield bootstrap検証で使用したREST + JPA構成の例です。

```xml
<parent>
  <groupId>org.koikifw</groupId>
  <artifactId>koiki-parent</artifactId>
  <version>0.1.0-SNAPSHOT</version>
  <relativePath/>                               <!-- 空にして親探索を無効化する -->
</parent>

<dependencies>
  <dependency>
    <groupId>org.koikifw</groupId>
    <artifactId>koiki-architecture-contract</artifactId>
  </dependency>
  <dependency>
    <groupId>org.koikifw</groupId>
    <artifactId>koiki-starter-api</artifactId>
  </dependency>
  <dependency>
    <groupId>org.koikifw</groupId>
    <artifactId>koiki-starter-data</artifactId>
  </dependency>
  <dependency>
    <groupId>org.koikifw</groupId>
    <artifactId>koiki-starter-data-jpa</artifactId>
  </dependency>
  <dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
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
</dependencies>
```

MVC、Session、Securityが必要な場合のStarter選択は[引継ぎガイド§5〜§6](application-team-handoff-guide.md)と
[Phase 2 Developer Journey](phase2-developer-journey.md)で決めます。version指定はKOIKI Parent / BOMに任せ、
個別dependencyのversionを上書きしません。

### 5.2 bootstrap時に必ず入れるもの

| 項目 | 内容 | 入れないとどうなるか |
|---|---|---|
| 業務moduleの宣言 | module rootの`package-info.java`へ`@KoikiModule(name, tier, persistence, persistenceModel)`を付けます | Architecture Rulesでmodule境界を検査できません |
| Architecture Rules test | `KoikiArchitectureRules.businessModuleRules(<base package>)`と`frameworkOwnershipRules("org.koikifw", <base package>)`を実行するtestを置きます | R2 buildは、`clean verify`が成功してもRules実行証拠がなければFAILにします |
| Customer migration | `src/main/resources/db/migration/customer/`へ置きます | Framework migrationとOwnershipが混ざります |
| API versioning | `koiki-starter-api`使用時は`@RequestMapping("/api/v{version:[1-9][0-9]*}/...")`とし、handlerへ`version = "1"`を宣言します | `/xxx/yyy`形式では先頭segmentがversionと解釈され、400 `Invalid API version`になります |
| DB接続設定 | datasource値を環境変数から受け取り、`spring.jpa.open-in-view=false`、`ddl-auto=validate`を維持します | 回避設定で起動しても、本番と異なる前提で開発することになります |

module内部のpackage配置（adapter / application / domain）と依存方向は
[`koiki-business-feature-work` Skill](../agent/skills/koiki-business-feature-work/SKILL.md)と引継ぎガイド§8を正本とします。
動作する最小例が必要な場合は、Tooling所有の`GREENFIELD-BOOTSTRAP` exampleを参照できます
（入手方法はFramework Ownerへ確認してください）。ただしProject Templateではないため、`readiness` moduleや
endpointをそのまま業務コードとしてコピーしません。

> **Dev Container / Linuxでの想定**（未検証）
>
> - POMとsourceにOS固有の差分はありません。
> - DB接続値を環境変数から受け取る設計にしておくと、Windows直接実行とcontainer内実行で接続先が変わっても
>   `application.properties`を書き換えずに済みます（§7の付記）。
> - Linuxではfile名の大文字・小文字が区別されます。migrationファイル名やresource pathの綴りの揺れは、
>   Windowsでは通ってもLinuxで失敗する場合があります。

## 6. 初回buildを実行する

### 6.1 R2 handoff verificationでbuildする

Customer projectのbuildは、Framework checkoutにあるR2 scriptからCLIで実行します。IDEの種類には依存しません。
PowerShell 7で次を実行します。

```powershell
$ErrorActionPreference = 'Stop'

$frameworkRoot   = (Resolve-Path 'C:\koiki-dev\KOIKI-JAVAWEB').Path
$applicationRoot = (Resolve-Path 'C:\koiki-dev\customer-app').Path
$stageParent     = 'C:\koiki-dev\local-artifact-stages'
$evidenceRoot    = 'C:\koiki-dev\evidence'

$frameworkCommit = (& git -C $frameworkRoot rev-parse HEAD).Trim()
if ((& git -C $frameworkRoot status --porcelain=v1 --untracked-files=all | Out-String).Trim()) {
    throw 'KOIKI Framework worktree must be clean before staging.'
}

$runId          = Get-Date -Format 'yyyyMMdd-HHmmss'
$stageRoot      = Join-Path $stageParent "run-$runId"
$manifestOutput = Join-Path $evidenceRoot "customer-app-$runId.json"
$tool = Join-Path $frameworkRoot 'build-support\adoption-readiness-verification\invoke-p4-ar6-r2-handoff.ps1'

& pwsh -NoProfile -File $tool `
    -ExpectedFrameworkCommit $frameworkCommit `
    -ExpectedFrameworkVersion '0.1.0-SNAPSHOT' `
    -FrameworkRepository $frameworkRoot `
    -StageRoot $stageRoot `
    -ManifestOutput $manifestOutput `
    -CustomerPom (Join-Path $applicationRoot 'pom.xml') `
    -CustomerSourceIdentity "customer-app-$runId"

if ($LASTEXITCODE -ne 0) { throw "R2 handoff verification failed with exit code $LASTEXITCODE." }
```

`StageRoot`は実行ごとに未作成の新しいpathを使います。`CustomerSourceIdentity`はmanifestへ記録されるため、
顧客名などの機密情報を含まない識別子にします。

> **Dev Container / Linuxでの想定**（未検証）
>
> - **PowerShell 7（`pwsh`）は必要です。** R2 scriptはPowerShell scriptであり、bash版はありません。Tooling所有のscriptを
>   Customer Repositoryへコピーしたり、bashへ書き換えたりせず、必要な場合はFramework側へ依頼します。
> - R2 scriptは`$IsWindows`で`mvnw` / `mvnw.cmd`、`java` / `java.exe`、pathの比較方法を切り替える実装です。
>   Linuxでの実行Evidenceはまだありません。上記scriptは`pwsh`上で先頭4行のpathをLinux pathへ変えれば、そのまま使える想定です。
> - Windowsとの違いとして、次の点に注意します。
>   - **pathの大文字・小文字が区別されます。** 実際のdirectory名と同じ綴りで指定します。
>   - **stage / manifest / Repositoryのpathは、root（`/`）までのすべての親directoryでsymbolic linkが拒否されます。**
>     distributionやmount方法によっては`/home`等がsymbolic linkの場合があります。`readlink -f <path>`の結果が元のpathと
>     同じか確認します。
>   - **`JAVA_HOME`はJDK 21を指し、`javac`を含む必要があります。** JDK 21以外、またはJREだけの場合は停止します。
>   - **`StageRoot`はcontainer内の通常の`~/.m2/repository`の外に置きます。** userのhome directoryそのものも拒否されます。
> - Linuxで初めて実行した結果（PASS / FAIL、manifest）は、Linux環境の初回Evidenceとして
>   [P4-AR6 worksheet](p4-ar6-actual-team-reception-worksheet.md)へ記録し、FAILや手順の不足はfindingとしてFramework側へ戻します。

### 6.2 成功を確認する

- consoleに`P4-AR6 R2 handoff verification succeeded.`が表示されます。
- manifestの`result`が`PASS`、`verification.customerBuild.status`が`PASS`です。
- `verification.customerBuild.internalPackageReferences`が`0`、`architectureRuleTestCount`が1以上です。
- `$stageRoot`が削除されています。
- `customer-app\target\`に実行可能JARが生成されています。

stageは検証終了時に削除されるため、source変更後に再buildするときも新しい`runId`でこの手順を再実行します。

## 7. ローカルで起動する

使い捨てPostgreSQL 17 containerへ接続して起動します。DB名、user、password、portはローカル専用の例です。

```powershell
docker run --rm -d --name customer-app-postgres `
  -e POSTGRES_DB=customer -e POSTGRES_USER=customer -e POSTGRES_PASSWORD=customer `
  -p 127.0.0.1:55433:5432 postgres:17

docker exec customer-app-postgres pg_isready -U customer -d customer

$env:SPRING_DATASOURCE_URL      = 'jdbc:postgresql://127.0.0.1:55433/customer'
$env:SPRING_DATASOURCE_USERNAME = 'customer'
$env:SPRING_DATASOURCE_PASSWORD = 'customer'
java -jar .\target\<artifact>-<version>.jar --server.port=18081
```

起動logで、KOIKI用`koiki_flyway_history`とCustomer用`flyway_schema_history`が分かれて作成され、
Customer migrationが適用されたことを確認します。終了時は`Ctrl+C`でApplicationを止め、`docker stop customer-app-postgres`を実行し、
環境変数を削除します。port 18081 / 55433にlistenerが残っていないことを確認します。

Reference Applicationで全体の動作を先に見たい場合は
[Reference Local Run Guide](../reference/KOIKI-JavaWeb-FW_Reference_Application_Local_Run_Guide_v0.1.md)を使います。

> **Dev Container / Linuxでの想定**（未検証）
>
> - **DBの接続先は、PostgreSQLをどう用意するかで変わります。** container内の`127.0.0.1`は、Windows hostの`127.0.0.1`とは
>   別です。例えば、Dev Containerと並べて起動したDB serviceならservice名、host側で起動したcontainerなら
>   `host.docker.internal`等を指定することになります。Applicationは`SPRING_DATASOURCE_*`等の環境変数で接続先を切り替えます。
> - hostのbrowserからApplicationを確認する場合は、18081等のportをhostへ転送する設定が必要です。
> - bashでは環境変数を`SPRING_DATASOURCE_URL=... java -jar target/<artifact>-<version>.jar`のように指定します。
>   listenerの確認は`Get-NetTCPConnection`ではなく`ss -ltn`等を使います。
> - [Reference Local Run Guide](../reference/KOIKI-JavaWeb-FW_Reference_Application_Local_Run_Guide_v0.1.md)のcommandは
>   Windows向け（`mvnw.cmd`、`Get-NetTCPConnection`等）です。`./mvnw`等へ読み替えます。
> - `koiki-testing`はTestcontainersを利用します。Customer testでTestcontainersを使う場合は、container内からDockerを
>   操作できる構成と、testから起動したDB containerへの接続経路が必要です。

## 8. VS Codeを採用する場合

本節は、IDEとしてVS Codeを採用する場合の設定例です。検証ではVS Codeを使用しました。他のIDEやeditorを採用する場合、
またはCLI中心で開発する場合は本節を読み飛ばし、§2.2のルールを各自の環境で守ります。

### 8.1 拡張機能とJDK設定

Java / Maven / Spring開発には次の拡張機能を使用します。検証環境では、JDKを同梱する
`pleiades.java-extension-pack-jdk`経由で導入した構成で確認しています。

| 拡張機能ID | 用途 | 必要度 |
|---|---|---|
| `redhat.java` | Java Language Server（補完、compile error表示、import） | 必須 |
| `vscjava.vscode-maven` | Maven project認識、lifecycle実行 | 必須 |
| `redhat.vscode-xml` | `pom.xml`のXSD検証 | 推奨 |
| `vmware.vscode-spring-boot` | Spring Boot Language Server（properties補完等） | 推奨 |
| `vscjava.vscode-spring-boot-dashboard` | Application起動・停止 | 任意 |
| `vscjava.vscode-spring-initializr` | VS CodeからのSpring Initializr生成 | 任意（Web版Initializrでも可） |
| `vscjava.vscode-java-debug` / `vscjava.vscode-java-test` | debug実行、JUnit実行 | 推奨 |

`vscjava.vscode-java-pack`（Extension Pack for Java）を導入すると、`redhat.java`、Maven、debug、test拡張機能が
まとめて入ります。Pleiades版を使う場合も内容は同等です。

**IDEが使うJDKとMaven buildのJDKは同一とは限りません。** 拡張機能は独自の同梱JREでLanguage Serverを起動します。
Applicationのcompile対象JDKを明示するため、ユーザー`settings.json`（端末固有のpathを含むため共有しない）で
次のように設定します。

```jsonc
{
  "java.configuration.runtimes": [
    { "name": "JavaSE-21", "path": "C:\\Program Files\\Eclipse Adoptium\\jdk-21.0.12.1-hotspot", "default": true }
  ]
}
```

pathは各端末のJDK 21 homeへ置き換えます。

社内SSLインスペクションProxy配下では、§3.3のMaven用JDKに加えて次も対応します。

1. `java.configuration.runtimes`へ列挙したJDKへ社内Root CAをインポートします。
2. `redhat.java`拡張機能が同梱するJREにもインポートします。これが未対応だとSpring Initializr拡張機能が失敗します。
3. `redhat.vscode-xml`（lemminx）向けに、ユーザー`settings.json`へ`xml.server.binary.args`を設定します。
4. VS Codeを**完全に終了してから**再起動します。ウィンドウの再読み込みだけでは反映されない場合があります。

具体的なpathと設定例は[build-support README](../../build-support/README.md#社内sslインスペクションproxy環境でのmavenビルドエラー対応)を参照します。

> **Dev Container / Linuxでの想定**（未検証）
>
> - 拡張機能はcontainer側へinstallされます（Windows側のinstallとは別です）。
> - `java.configuration.runtimes`のpathは、container内のJDK 21 home（Linux path）を指定します。
> - 社内Proxy配下では、Language Serverを証明書取り込み済みのcontainer JDKで起動させると、取り込み先を1か所にまとめられます
>   （例: `java.jdt.ls.java.home`、`xml.java.home`とcontainer JDKの指定、`xml.server.preferBinary: false`）。
>   同梱JREを使い続ける場合は、そのJREの`cacerts`も取り込み対象になります。

### 8.2 multi-root workspaceを設定する

VS Codeでは一つのwindowで複数のfolderを同時に開けます（multi-root workspace）。Customer projectとFramework checkoutを
並べて開くと、Framework sourceを参照しながらCustomer codeを書けます。

**GUIで作成する手順**

1. `ファイル` > `フォルダーを開く`から`C:\koiki-dev\customer-app`を開きます。
2. `ファイル` > `フォルダーをワークスペースに追加`で`C:\koiki-dev\KOIKI-JAVAWEB`を追加します。
3. 必要なら同様に`C:\koiki-dev\evidence`を追加します。
4. `ファイル` > `名前を付けてワークスペースを保存`で`C:\koiki-dev\koiki-dev.code-workspace`として保存します。
5. 以後は`ファイル` > `ファイルでワークスペースを開く`、または`.code-workspace`をダブルクリックして開きます。

最初に開いたfolderが先頭になり、新しいterminalの既定cwdになります。日常作業の中心であるCustomer projectを先頭に置きます。

**workspaceファイルの例**

保存した`.code-workspace`を次のように整えます。`path`はworkspaceファイルからの相対pathです。

```jsonc
{
  "folders": [
    { "name": "CUSTOMER-APP", "path": "customer-app" },
    { "name": "KOIKI-FRAMEWORK (read-only)", "path": "KOIKI-JAVAWEB" },
    { "name": "EVIDENCE", "path": "evidence" }
  ],
  "settings": {
    // pom.xml変更時にJava Language Serverへ再importを確認させる（検証環境と同じ設定）
    "java.configuration.updateBuildConfiguration": "interactive"
  }
}
```

- `name`はExplorer上の表示名です。Framework側を読み取り用と明示しておくと、誤編集を防げます。
- Frameworkを`C:\koiki-dev`の外に置いている場合は、`"path": "../<相対path>/KOIKI-JAVAWEB"`のように指定できます。
  2026-09-25の検証環境もFramework checkoutをworkspace directoryの外に置いた構成で確認しています。
- workspaceファイルはどちらのGit Repositoryにも属さない各開発者のlocalファイルです。
  チームで配置を揃えたい場合は、Customer Repositoryの`docs/`へ例として記載し、必須設定にはしません。

**multi-root workspaceでの注意**

- Source Control viewでは各Repositoryが別々に表示されます。commit先Repositoryを確認してからcommitします。
- 新規terminalを開くときにfolderを選び、どのRepositoryで実行しているか確認します。
- Framework reactorは多数のmoduleを持つため、Java Language Serverのimportに時間がかかります。端末が重い場合は
  workspaceからFramework folderを外し、読むときだけ別windowで開いても構いません。R2 buildはworkspace構成に依存しません。
- VS CodeのJava拡張機能は、既定ではEclipse系のproject metadataをproject rootではなくVS Codeのworkspace storageに保存します。
  `java.import.generatesMetadataFilesAtProjectRoot`を`true`にするとproject rootへ生成され、§2.2の`.factorypath`が
  Framework checkoutに現れる場合があります。Framework checkoutを開くworkspaceでは、この設定を有効にしません。

> **Dev Container / Linuxでの想定**（未検証）
>
> - `path`を相対pathで書いておけば、同じ`.code-workspace`をcontainer内でも使えます。絶対path（`C:\...`）は使いません。
> - containerがどのdirectoryを開くか、`.code-workspace`をどのように開くかはDev Containerの構成に依存します。
>   multi-root表示にしない場合でも、R2 buildはworkspace構成に依存しません。
> - Java Language ServerのimportはcontainerのCPU / memoryの影響を受けます。参考として、P4-AR1の検証環境では
>   Docker runtimeに6 CPU / 約16 GBを割り当てていました。

### 8.3 IDE上のKOIKI依存解決と日常操作

VS CodeのJava Language Serverは、Customer POMのKOIKI coordinateを通常の`~/.m2/repository`から解決しようとします。
R2 buildはstageを実行後に削除するため、**R2 buildの成功だけではIDE上のKOIKI依存は解決されません**。
また、Framework開発に参加したことのある端末では、`~/.m2/repository/org/koikifw/`に過去の`0.1.0-SNAPSHOT`が
残っている場合があり、IDEはその古いartifactで補完やerror表示を行います。この制約はMavenの`~/.m2`を参照する
他のIDEでも同様に起こると考えられます。

このため現時点では次のように扱います。

| 観点 | 扱い |
|---|---|
| build / testの合否 | §6のR2 buildの結果を正とします。IDE上の表示だけで合否を判断しません |
| `pom.xml`のKOIKI依存が未解決と表示される | 正式artifact repository提供前の既知の制約です。R2 buildが成功していれば設定の誤りとは限りません |
| `~/.m2`の古いKOIKI SNAPSHOT | IDE表示がFramework checkoutと食い違う場合、古いartifactを疑います。削除する場合は自端末の所有物であることを確認します |
| KOIKI artifactを`~/.m2`へinstallしてIDEを通す | R2の手順には含まれません。導入する場合はFramework Ownerと合意し、使用commitを記録します |
| 日常開発用の永続stage | 現在のR2 cleanup契約の対象外です。必要になった場合は、所有権と更新・破棄方法を別Toolingとして設計します |

IDEでの依存解決をどう提供するかは未決定事項です。実チームで困った点は
[P4-AR6 worksheet](p4-ar6-actual-team-reception-worksheet.md)のfindingとして記録し、Framework側へ戻します。

よく使うVS Code操作は次のとおりです。

| 目的 | 操作 |
|---|---|
| `pom.xml`変更をIDEへ反映 | 変更保存時の確認で`Update`を選ぶか、`Java: Reload Projects`を実行 |
| IDE表示がおかしいとき | `Java: Clean Java Language Server Workspace`を実行し、VS Codeを再起動 |
| Framework sourceを読む | `Ctrl+P`でFramework folder内のファイルを開く。Customerへコピーしない |
| 単体testをIDEで実行 | Testing viewまたはtest classの`Run Test`。最終確認は§6のR2 build |

> **Dev Container / Linuxでの想定**（未検証）
>
> - container内の`~/.m2`は通常、空から始まるため、古いKOIKI SNAPSHOTによる食い違いは起きにくくなります。
>   ただし、`~/.m2`をvolume等で永続化している場合は、Windowsと同じく古いartifactが残る可能性があります。
> - IDE上でKOIKI依存が未解決になる制約そのものは、Linux環境でも同じです。

## 9. Dev Container / Linuxで事前に想定される事項の一覧

各手順の付記を、アプリチームがDev Container環境を設計するときの確認観点としてまとめます。
いずれもKOIKI側では未検証です。L10はVS Codeを採用する場合だけ該当します。

| # | 観点 | 想定される事項 | 付記の場所 |
|---|---|---|---|
| L1 | sourceの置き場所 | Framework / Customer RepositoryはLinux filesystem上でcloneします。Windows側checkoutのmountは、`mvnw`の改行、`git status`のdirty、性能の問題を起こす可能性があります | §4.1 |
| L2 | mount範囲 | Framework checkout、Customer Repository、stage、evidenceのすべてがcontainerから見える必要があります | §2.2 |
| L3 | 必要なtool | JDK 21（`javac`を含む）、PowerShell 7、Git、`wget` / `curl` | §3.1 |
| L4 | PowerShell | R2 scriptの実行に`pwsh`が必要です。bash版はありません | §6.1 |
| L5 | Windows専用script | `check-build-environment.ps1`と`import-corporate-root-ca.ps1`はLinuxで使えません | §3.2、§3.3 |
| L6 | 社内Proxy証明書 | OSの証明書ストアとJDKの`cacerts`の両方へ取り込みます | §3.3 |
| L7 | R2 scriptのpath検査 | 大文字・小文字の区別、rootまでのsymbolic link拒否、`~/.m2`とhome directoryの拒否 | §6.1 |
| L8 | Framework checkoutのclean維持 | Framework側へ`.devcontainer/`等を置くとR2 buildが止まります | §2.2 |
| L9 | DB接続先、port、Testcontainers | container内の`127.0.0.1`はhostとは別です。接続先は環境変数で切り替え、port転送を設定します。Testcontainersにはcontainer内からDockerを使える構成が必要です | §7 |
| L10 | VS Code拡張機能とLanguage Server | 拡張機能はcontainer側へinstallされます。Language Serverが使うJDK / JREも証明書の対象です | §8.1 |
| L11 | IDEの依存解決 | 古いSNAPSHOTの問題は起きにくくなりますが、KOIKI依存が未解決になる制約は同じです | §8.3 |
| L12 | Evidence | Linuxでの初回R2 build結果をP4-AR6 worksheetへ記録し、findingをFramework側へ戻します | §6.1 |

## 10. トラブルシューティング

| 症状 | 最初に確認すること | 参照先 |
|---|---|---|
| `PKIX path building failed` | どのprocess（Maven / IDEのLanguage Server等）が使うJDKか。そのJDKの`cacerts`へ社内Root CAをインポートしたか | §3.3、§8.1 |
| `Non-resolvable parent POM` / `Could not find artifact org.koikifw:...` | Customer projectだけで`mvnw`を実行していないか。§6のR2 buildを使ったか | §2.3、§6 |
| `Framework worktree is not clean` | `git -C <Framework> status --short`。`.factorypath`等のIDE生成物がないか | §2.2 |
| StageRootまたはmanifestが既に存在する | 新しい`runId`で再実行したか。既存Evidenceを上書きしない | §6.1 |
| R2 buildがArchitecture Rules未実行でFAIL | `KoikiArchitectureRules`を使うtestがあり、Surefireで実行されているか | §5.2 |
| HTTP 400 `Invalid API version` | URLが`/api/v1/...`形式か。handlerに`version = "1"`があるか | §5.2 |
| `Failed to determine a suitable driver class` | datasource環境変数、PostgreSQL driverのruntime dependency | §7 |
| `Connection refused` | container起動、`pg_isready`、port 55433のlistener | §7 |
| IDEの補完・error表示とbuild結果が食い違う | `~/.m2`の古いKOIKI SNAPSHOT、IDEのcache | §8.3 |
| （Linux）`/usr/bin/env: 'sh\r'`等で`mvnw`が起動しない | Windows filesystem上のcheckoutをmountしていないか | §4.1 |
| （Linux）Linux環境でだけ`Framework worktree is not clean`になる | checkoutの場所、`git status`の差分が改行・実行権限だけではないか、Framework側へ`.devcontainer/`を置いていないか | §4.1、§2.2 |
| （Linux）`A link or reparse point is not allowed` | stage / manifest / Repositoryのpathの経路にsymbolic linkがないか | §6.1 |
| （Linux）`P4-AR6 R2 staging requires JDK 21` / `A complete JDK was not found` | `JAVA_HOME`がJDK 21を指し、`bin/javac`があるか | §6.1 |
| （Linux）DBへ`Connection refused` | 接続先に`127.0.0.1`を指定していないか。DBをどこで起動しているか | §7 |

Flyway、Hibernate validation、Architecture Rules、Securityを無効化して症状を回避しません。
原因の切り分けとOwnerの判断は[引継ぎガイド§11](application-team-handoff-guide.md)に従います。

## 11. セットアップ完了チェックリスト

**共通**

- [ ] JDK 21、PowerShell 7、Git、Docker互換runtimeが利用でき、`JAVA_HOME` / `JAVA21_HOME`を設定した
- [ ] 社内Proxy配下の場合、Maven buildに使うJDKの証明書対応を行った
- [ ] Framework checkoutを合意済みcommitに固定し、40桁commitを記録した
- [ ] Customer projectを別Git Repositoryとして作成した
- [ ] Customer POMがKOIKI Parent（空の`relativePath`）と必要なStarterだけを参照している
- [ ] `@KoikiModule`、Architecture Rules test、`db/migration/customer`を用意した
- [ ] R2 buildが`PASS`し、stageが削除され、manifestとsidecarが`evidence`に残った
- [ ] 実行可能JARをPostgreSQL 17へ接続して起動し、cleanupまで確認した

**VS Codeを採用する場合**

- [ ] 必須・推奨の拡張機能を導入し、`java.configuration.runtimes`でJDK 21を指定した
- [ ] 社内Proxy配下の場合、`redhat.java`同梱JREとXML Language Serverの証明書対応を行った
- [ ] `.code-workspace`でCustomer / Framework（必要ならevidence）を開き、どちらのRepositoryにもcommitしていない

**Dev Container / Linux環境で開発する場合**

上記の各項目をLinux環境で満たしたうえで、§9のL1〜L12をDev Container設計時の確認観点として使い、
初回R2 buildの結果をP4-AR6 worksheetへ記録します。

## 12. 関連文書

- [アプリ開発チーム向け引継ぎガイド](application-team-handoff-guide.md): 提供物、構成選択、最初のCustomer module、診断の入口
- [P4-AR6 実チーム受入worksheet](p4-ar6-actual-team-reception-worksheet.md): 本手順の再現結果とfindingの記録様式
- [P4-AR6 Actual-project Handoff Contract](../architecture/validation/pre-phase4-p4-ar6-actual-project-handoff-contract.md): R1〜R5のRepository topologyとR2の安全条件
- [Adoption Readiness Verification README](../../build-support/adoption-readiness-verification/README.md): R2 scriptの入力境界と安全契約
- [External Reference Boundary Validation](../architecture/validation/adoption-external-reference-boundary-validation.md): multi-root workspace上の外部projectでReferenceをbuildした検証記録
- [Greenfield Bootstrap Smoke Validation](../architecture/validation/adoption-greenfield-bootstrap-smoke.md): 新規Initializr projectからbuild / 起動 / DB接続まで確認した検証記録
- [build-support README](../../build-support/README.md): 社内Proxy証明書対応の詳細
