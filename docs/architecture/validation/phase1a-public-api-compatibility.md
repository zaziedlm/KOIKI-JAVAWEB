# Phase 1a Public API Compatibility — C3実装・検証計画

**調査日:** 2026年8月26日<br>
**対象branch:** `feature/phase1a-public-api-compatibility`<br>
**状態:** C3 COMPLETE / GATE 1〜4 ACCEPTED<br>
**Ownership:** Framework（Public API契約）/ Tooling（inventory、japicmp、fixture、CI）<br>
**対象:** `koiki-architecture-contract`、`koiki-archunit-rules`、C1 baseline artifact<br>
**開始baseline:** `9642ba1`（PR #17、C2 COMPLETE）<br>

## 1. 目的と完了条件

Phase 1a実行計画のG3、G5、Milestone CおよびDoD 1a-5に基づき、承認済みPublic APIを
machine-readable inventoryとC1公開artifactに対するjapicmpで固定し、Public API破壊をCIで拒否する。

C3は次をすべて満たしたときだけ`COMPLETE`とする。

1. Architecture Contractの4 public型とArchUnit Rulesの1 public型 / 2 static methodをinventoryへ記録する。
2. C1のtimestamped snapshot JAR 2件をSHA-256付きimmutable baselineとして利用する。
3. 現在のJARとbaseline JARのbinary / source compatibilityおよび全Public API modificationをjapicmpで検査する。
4. public型・method・annotation element・enum constantの削除またはsignature変更を期待failureで実証する。
5. package-private実装変更はPublic API破壊として扱わず、正常系で許容する。
6. 未承認のpublic型・method追加もinventory差分として失敗し、Owner承認なしにAPIを拡張できない。
7. localとGitHub Actionsで同じ検証scriptを実行し、通常の認証不要`clean verify`は維持する。
8. baseline JAR、PAT、settings実値をRepositoryやartifactへ保存しない。
9. comparison report、正常系・負例、CI run、credential非露出および再現手順を本書へ記録する。
10. Owner Review Gate 1〜4をすべて`ACCEPTED`とする。

## 2. 作業位置と境界

| 項目 | 内容 |
|---|---|
| Phase / status | Phase 1a / Milestone A・B COMPLETE / C1〜C3 COMPLETE / C4 NEXT |
| Framework ownership | 承認済みPublic APIの型、member、annotation、enum contract |
| Tooling ownership | baseline取得、inventory、japicmp、positive / negative fixture、CI |
| 対象artifact | `org.koikifw:koiki-architecture-contract`、`org.koikifw:koiki-archunit-rules` |
| baseline | C1公開commit `9573b1cf38713d51707a14884230d5bd5e1d97fb` |
| OpenSpec | Repositoryにchangeが存在しないため必須前提にしない |

C3ではPublic API、Maven coordinates、C1 package、ArchUnit rule内容、message contract、Runtimeまたは
Java 21 / 25 matrixを変更しない。互換検査のためにPublic API変更が必要になった場合はC3内で迂回せず、
G3 / G5およびADR-041のOwner Reviewへ戻す。

## 3. read-only調査結果

### 3.1 Git・実装状態

| 項目 | 結果 |
|---|---|
| C2 integration | PR #17、merge commit `9642ba1` |
| C3 branch | `feature/phase1a-public-api-compatibility`。read-only調査開始時はclean |
| japicmp | POM、CI、build-support、Validationのいずれにも未実装 |
| C1との差分 | C1公開commit以降、両artifactのproduction source差分なし |
| current JAR | 両moduleとも`0.1.0-SNAPSHOT` JARをbuild可能 |
| Root Reactor | 正式4moduleだけ。C3 fixture moduleは追加しない |

### 3.2 Public API inventory

`javap -public`と承認済みG3 / G5設計を照合した結果は次のとおりである。

#### Architecture Contract

| Public type | Public contract |
|---|---|
| `org.koikifw.architecture.KoikiModule` | `name()`、`tier()`、`persistence()`、`persistenceModel()`、PACKAGE target、RUNTIME retention |
| `org.koikifw.architecture.ModuleTier` | `SIMPLE`、`RICH` |
| `org.koikifw.architecture.PersistenceTechnology` | `JPA`、`MYBATIS` |
| `org.koikifw.architecture.PersistenceModel` | `SHARED` |

#### ArchUnit Rules

| Public type | Public contract |
|---|---|
| `org.koikifw.archunit.KoikiArchitectureRules` | public constructorなし |
| `businessModuleRules(String)` | `public static ArchRule` |
| `frameworkOwnershipRules(String, String...)` | `public static ArchRule` |

`BusinessModuleRuleSet`、`FrameworkOwnershipRuleSet`、`ModuleMetadata`、`PackageName`、`RootPackageRule`、
`RuleMessage`とnested implementationはpackage-private / privateであり、Public APIではない。

### 3.3 Public dependency境界

- `KoikiArchitectureRules`の戻り値に`com.tngtech.archunit.lang.ArchRule`が現れるため、ArchUnit 1.5.0は
  Consumerから見えるPublic API dependencyである。
- Architecture Contractのclass fileにはJSpecify nullness annotationが含まれるため、JSpecify 1.0.0も
  comparison class pathへ含める。
- dependency不足を`ignoreMissingClasses`で隠さず、old / new comparison class pathを明示する。
- JUnit、Spring、Spring Data、Jakarta Persistence、Spring Modulithはtest scopeであり、Public API
  comparison class pathへ追加しない。

### 3.4 baseline identity

| Artifact | Timestamped snapshot | SHA-256 |
|---|---|---|
| Architecture Contract JAR | `0.1.0-20260826.091429-1` | `947EE8CF0E109FE58D81E6008A56C06C8F4C035FF76BDF462F8F6BD9BB50DE45` |
| ArchUnit Rules JAR | `0.1.0-20260826.091429-1` | `A51E26E7386D19E53C18BD63BC4E4F95EC1EAE471F39D519D6AE0CBC7C2DF3F2` |

baselineの正本は`phase1a-internal-snapshot.md`とGitHub Packages上のC1 payloadである。同じ
`0.1.0-SNAPSHOT`の最新metadataへ追従せず、timestamped filenameとSHA-256の両方を固定する。

### 3.5 japicmp公式仕様との照合

2026年8月26日にjapicmp公式Maven Plugin文書を確認した。

- stable candidateは`com.github.siom79.japicmp:japicmp-maven-plugin:0.26.1`とする。
- old / new versionはMaven dependencyまたはfileで明示できる。
- `accessModifier=public`、binary / source incompatible、all modifications、include / exclude、
  missing artifact / class、report形式を個別設定できる。
- pluginは`japicmp.diff`と`japicmp.xml`を生成できる。

参照:

- [japicmp Maven Plugin](https://siom79.github.io/japicmp/MavenPlugin.html)
- [japicmp cmp goal](https://siom79.github.io/japicmp/japicmp-maven-plugin/cmp-mojo.html)

## 4. Gate 1設計案

### 4.1 baseline取得

`build-support/api-compatibility/verify-public-api-compatibility.ps1`をTooling-owned scriptとして作成する。

1. GUID付きtemporary directoryと空Maven local repositoryを作成する。
2. localはPAT classic `read:packages`だけ、CIはRepository `GITHUB_TOKEN`の`packages: read`だけを用いる。
3. C1のbase SNAPSHOT directoryからtimestamped JAR 2件を認証付きHTTPSでtemporary directoryへ取得する。
4. SHA-256を再計算し、§3.4と一致しない場合はjapicmp実行前に停止する。
5. token、Authorization header、認証付きURL、settings実値をcommandまたはlogへ出さない。
6. `finally`でcredential環境変数とtemporary directoryを削除する。

baseline JARをGitへ追加せず、通常の`~/.m2/repository`にもinstallしない。C1 packageの削除・上書き・再公開は
C3の権限外とする。

### 4.2 build-support構成

```text
build-support/api-compatibility/
├── README.md
├── pom.xml
├── public-api.txt
├── verify-public-api-compatibility.ps1
└── fixture/
    ├── baseline/       # 最小Public API source
    ├── compatible/     # package-private変更
    └── breaking/       # public signature破壊
```

- Root Reactorへfixtureを追加しない。
- Tooling POMでjapicmp 0.26.1をversion固定し、両正式artifactをfile-to-fileで比較する。
- comparison class pathにはArchitecture Contract、ArchUnit 1.5.0、JSpecify 1.0.0を必要な側だけ明示する。
- current JARはJava 21と隔離Maven repositoryで正式Reactorからbuildする。
- `public-api.txt`はreview可能な正規化signature inventoryとし、生成結果との差分でもbuildを失敗させる。
- effective POM、baseline JAR、fixture JAR、japicmp reportは`target`またはtemporary directoryだけへ置く。

### 4.3 compatibility policy

| 項目 | 方針 |
|---|---|
| access | `public`。外部から到達できないpackage-private / private実装は対象外 |
| binary incompatibility | build failure |
| source incompatibility | build failure |
| compatible public addition | inventory差分およびall-modificationsでbuild failure。Owner Review後だけ許可 |
| annotation / enum変更 | modificationとしてfailure |
| missing class / artifact | failure。ignoreしない |
| includes / excludes | 原則なし。両artifact内の全public型を検査する |
| semantic version判定 | Phase 1aでは使用しない。`0.1.0-SNAPSHOT`のversion差では許否を決めない |
| report | diff / XMLを生成。secretを含まないことを確認する |

baseline更新または例外は、変更対象、Consumer影響、binary / source判定、migration要否、version判断、ADR-041との
整合をArchitecture Ownerが承認した場合だけ行う。除外追加や`ignoreMissing*`でgreen化しない。

### 4.4 positive / negative fixture

| 経路 | 期待結果 |
|---|---|
| 正式current vs C1 baseline | modificationなし、終了コード`0` |
| `compatible` fixture | package-private実装だけを変更し、終了コード`0` |
| `breaking` fixture | public methodまたは型を削除 / signature変更し、japicmpが非互換を報告して非`0` |
| inventory追加負例 | 未承認public型を追加し、inventory差分で非`0` |

負例はtemporary buildまたは`build-support`配下の非配布fixtureだけで実行する。正式production sourceを一時編集して
復元する方式、赤いbranch、C1 packageの改変またはWalking Skeleton JARは使用しない。

### 4.5 CI

- 通常の`Verify (ubuntu-24.04)`は認証不要`clean verify`のまま維持する。
- 独立job `Public API Compatibility`を追加し、`contents: read`と`packages: read`だけを付与する。
- checkoutは`persist-credentials: false`、Actionはfull commit SHA固定、初回EvidenceではMaven cacheを使わない。
- 追加PAT secret、package write / delete、publisher checkout、Consumer Repository checkoutを使用しない。
- 正常系と期待failure fixtureを同じscriptで実行し、負例を捕捉した通常jobはgreenにする。
- PR required checkへの追加はremote CI成功とOwner Review後に行う。

## 5. Gateと停止条件

| Gate | Review対象 | 承認条件 | 状態 |
|---:|---|---|---|
| 1 | read-only調査、inventory、baseline、policy、fixture、認証・CI設計 | Public APIを拡大せず、C1 artifactをimmutable baselineとして再現可能に比較できる | ACCEPTED（2026年8月27日、Shuichi Kataoka） |
| 2 | baseline取得、inventory、正式artifact positive comparison | timestamp / SHA一致、5 public型 / 2 method、japicmp終了コード`0` | ACCEPTED（2026年8月27日、Shuichi Kataoka） |
| 3 | breaking / internal / addition fixture | public破壊と未承認追加だけが失敗し、package-private変更は成功する | ACCEPTED（2026年8月27日、Shuichi Kataoka） |
| 4 | CI、report、secret safety、DoD traceability、C3 closeout | fresh runnerで成功し、required check・再現手順・Owner Reviewが揃う | ACCEPTED（2026年8月27日、Shuichi Kataoka） |

次に該当した場合は実装を停止し、Gate 1またはG3 / G5へ戻す。

- C1 timestamped JARまたはSHA-256が一致しない、取得できない、またはlatest SNAPSHOT追従が必要になる。
- 承認済み5型 / 2 method以外をPublic APIへ固定する必要がある。
- Public API破壊を除外、ignore、semantic version設定だけで許容する必要がある。
- normal `clean verify`へpackage credentialを必須化する必要がある。
- PATに`read:packages`を超えるscope、Actionsにpackage write / deleteまたは追加PAT secretが必要になる。
- C4 runtime fixture、C5 closeout、後続PhaseのStarter / API / packageが必要になる。

## 6. Gate 1 Owner Review項目

Architecture Ownerは次の5項目を確認する。

1. C1 timestamped JAR 2件とSHA-256をimmutable baselineとし、binaryをGitへ保存しない。
2. inventoryをArchitecture Contract 4型、Rules 1型 / 2 methodに限定し、全public modificationを承認対象にする。
3. japicmp 0.26.1、public access、binary / source / all-modifications failure、missing class非ignoreを採用する。
4. 正常系、package-private許容、public破壊、未承認追加の4経路をTooling fixtureで実証する。
5. 専用CI jobだけに`packages: read`を与え、通常build、ADR、Skill、migrationおよびC4以降を変更しない。

Gate 1は2026年8月27日にOwner承認された。Gate 2ではbaseline取得、inventoryおよび正式artifactの
正常系比較だけを実装し、負例fixture、CI job、required checkは後続Gate承認まで実装しない。

## 7. Gate 2実装・検証Evidence

### 7.1 実装範囲

2026年8月27日にGate 1承認範囲内で、次のTooling-owned資材を実装した。

| 資材 | 責務 |
|---|---|
| `build-support/api-compatibility/verify-public-api-compatibility.ps1` | PAT scope検査、baseline取得、SHA、隔離build、inventory、japicmp、cleanup |
| `build-support/api-compatibility/pom.xml` | japicmp 0.26.1と2 artifact別comparison class path |
| `build-support/api-compatibility/PublicApiInventory.java` | JARからpublic型、annotation metadata、enum定数、public memberを正規化 |
| `build-support/api-compatibility/public-api.txt` | Gate 1で承認したPublic API inventory |
| `build-support/api-compatibility/README.md` | local再現手順とcredential / artifact境界 |

これらはRoot Reactor、正式配布artifact、Framework Public APIまたは通常`clean verify`へ追加していない。

### 7.2 実行環境とcommand

| 項目 | 実測値 |
|---|---|
| OS | Windows 11 |
| JDK | Eclipse Adoptium 21.0.12.1 |
| Maven | Wrapper 3.9.16 |
| 実行日 | 2026年8月27日 |
| 認証 | PAT classic、OAuth scopeは`read:packages`だけ |
| local repository | GUID付きtemporary directory内の空Repository |

```powershell
pwsh -NoProfile -ExecutionPolicy Bypass `
  -File .\build-support\api-compatibility\verify-public-api-compatibility.ps1
```

通常の認証不要`./mvnw clean verify`も別途実行し、69 tests、failure / error / skipped各`0`で成功した。

### 7.3 C1 baseline identity

GitHub Packagesのbase version directoryからtimestamped JARを直接取得し、japicmp前にSHA-256を照合した。

| Artifact | Timestamped snapshot | SHA-256 | 結果 |
|---|---|---|---|
| Architecture Contract | `0.1.0-20260826.091429-1` | `947EE8CF0E109FE58D81E6008A56C06C8F4C035FF76BDF462F8F6BD9BB50DE45` | MATCH |
| ArchUnit Rules | `0.1.0-20260826.091429-1` | `A51E26E7386D19E53C18BD63BC4E4F95EC1EAE471F39D519D6AE0CBC7C2DF3F2` | MATCH |

可変なlatest SNAPSHOT metadata、通常local repositoryまたはC1 packageの変更は使用していない。

### 7.4 inventoryとjapicmp

| 検査 | 結果 |
|---|---|
| Public type inventory | 5型、MATCH |
| `KoikiModule` annotation elements | 4、MATCH |
| `KoikiArchitectureRules` public methods | 2、MATCH |
| Architecture Contract | `access=public`、modifications `NONE`、exit `0` |
| ArchUnit Rules | `access=public`、modifications `NONE`、exit `0` |

japicmpはbinary / source incompatibilityと全modificationでbuildを失敗させる設定、
`ignoreMissingClasses=false`、include / excludeなしで実行した。両比較はdiff / XML reportを生成してから
成功し、reportはtemporary directoryだけに保持した。

### 7.5 credential・cleanup

- PATはsecure promptで入力し、scope headerが`read:packages`だけであることを検証した。
- 共有logにtoken実値、Authorization header、Basic credentialまたは認証付きURLはない。
- baseline JAR、isolated repository、inventory実測値、japicmp reportはGUID付きtemporary directoryへ置いた。
- 初回dry runのtemporary directoryが`finally`後に存在しないことを独立確認した。修正版最終runも
  cleanup errorなしでPowerShell promptへ復帰した。
- Repositoryのstatusにbaseline JAR、report、settingsまたはcredential fileはない。

### 7.6 Gate 2 Owner Review結果

2026年8月27日にOwnerは次の4項目とGate 2の内容・結果を承認した。Gate 3では承認済みの
compatibility policyを変更せず、package-private許容、public破壊および未承認public追加のfixtureへ進む。

1. C1 timestamped JAR 2件のversionとSHA-256がC1 Evidenceに一致する。
2. inventoryが承認済み5 public型、4 annotation element、2 Rules methodに一致する。
3. 正式2 artifactのjapicmp比較がpublic modificationなし、終了コード`0`である。
4. 通常buildを認証不要のまま維持し、credentialおよびbaseline binaryをRepositoryへ残していない。

## 8. Gate 3 fixture実装・検証Evidence

### 8.1 実装境界

`build-support/api-compatibility/fixture/`へ、Root Reactor外・非配布のJava source fixtureを追加した。
`verify-public-api-fixtures.ps1`は認証を使用せず、各sourceをJava 21でtemporary JARへcompileし、
inventoryとjapicmp 0.26.1を実行する。

| Fixture | Public contract | Internal差分 | 期待 |
|---|---|---|---|
| `baseline` | `public static String value()` | package-private `int value()` | 比較元 |
| `compatible` | baselineと同一 | package-private `long changedValue()`へ変更 | inventory一致、japicmp成功 |
| `breaking` | `value()`の戻り値を`String`から`int`へ変更 | baselineと同一 | japicmp期待failure |
| `addition` | `public static String added()`を追加 | baselineと同一 | inventory / japicmp期待failure |

正式production source、C1 baseline、正式Public API inventory、Root POMまたは通常build lifecycleは変更していない。

### 8.2 commandと結果

```powershell
pwsh -NoProfile -ExecutionPolicy Bypass `
  -File .\build-support\api-compatibility\verify-public-api-fixtures.ps1
```

2026年8月27日のWindows / JDK 21 local実行結果は次のとおりである。

| 経路 | inventory | japicmp | script判定 |
|---|---|---|---|
| package-private implementation change | MATCH | modifications `NONE`、exit `0` | PASS |
| public return type change | 差分あり | `METHOD_RETURN_TYPE_CHANGED`、非`0` | expected failure PASS |
| unapproved public addition | MISMATCH、`PublicContract#added()`検出 | `METHOD_ADDED_TO_PUBLIC_CLASS`、非`0` | expected failure PASS |

負例のMaven invocationは個別には`BUILD FAILURE`となる。scriptは生成されたdiff / XML reportでも
詳細diagnosticを照合し、期待した理由による非`0`だけを捕捉して、全体を
`C3 Gate 3 Public API fixture verification: SUCCESS`で終了した。

### 8.3 回帰・cleanup

- Gate 2の正式5 public型 / 4 annotation element / 2 Rules method inventoryを再生成し、MATCHを確認した。
- 通常の認証不要`mvnw.cmd clean verify`は69 tests、failure / error / skipped各`0`で成功した。
- fixture JAR、class、隔離Maven repository、diff / XML reportはGUID付きtemporary directoryだけへ生成した。
- 成功runのtemporary directoryが`finally`後に存在しないことを確認した。
- tracked fixtureはJava sourceと期待inventoryだけであり、binary、credentialまたはreportを含まない。

### 8.4 Gate 3 Owner Review結果

2026年8月27日にOwnerは次の4項目とGate 3の内容・結果を承認した。

1. package-private実装変更はPublic API inventoryとjapicmpを変化させず、終了コード`0`となる。
2. public methodのreturn type変更は`METHOD_RETURN_TYPE_CHANGED`で拒否される。
3. 未承認public method追加はinventory差分と`METHOD_ADDED_TO_PUBLIC_CLASS`の両方で拒否される。
4. 負例は正式sourceを一時編集せず、非配布fixtureとtemporary outputだけで再現される。

Gate 4では承認済みfixtureを変更せず、CI workflow、`GITHUB_TOKEN`の`packages: read`経路、
secret safety、required checkおよびC3 closeoutを検証する。

## 9. Gate 4 CI実装・local Evidence

### 9.1 専用job

`.github/workflows/ci.yml`へ独立job `Public API Compatibility`を追加した。

| 境界 | 設定 |
|---|---|
| runner | `ubuntu-24.04` / Temurin 21 |
| permissions | `contents: read`、`packages: read`だけ |
| checkout | `persist-credentials: false` |
| authentication | `${{ secrets.GITHUB_TOKEN }}`をstep環境変数へ渡す |
| cache | 使用しない |
| Actions | checkout / setup-javaを既存のfull commit SHAへ固定 |
| checks | C1 baseline / SHA / inventory / 正式japicmp、Gate 3 fixture |

通常の`Verify (ubuntu-24.04)` jobはworkflow既定の`contents: read`だけを維持し、package credentialを
必要としない。追加PAT secret、package write / delete、publisherまたはConsumer checkoutは追加していない。

### 9.2 script認証境界

`verify-public-api-compatibility.ps1`へ`-GitHubActions`を追加した。

- local modeは従来どおりPAT classicのOAuth scopeが`read:packages`だけであることをGitHub APIで検査する。
- Actions modeは`GITHUB_ACTIONS=true`のrunnerだけで利用でき、token未設定またはlocal偽装を拒否する。
- Actions tokenにはPAT用`X-OAuth-Scopes`検査を誤適用せず、workflow jobの明示permissionsを権限境界とする。
- token、Basic credential、Authorization header、認証付きURLはcommand lineまたはsummaryへ出力しない。

localでplaceholder tokenと`GITHUB_ACTIONS=false`を用いたguard検査は、期待diagnostic
`The GitHubActions switch may only be used on a GitHub Actions runner.`で非`0`となり、PASSした。

### 9.3 local回帰結果

2026年8月27日に次を再実行した。

| 検査 | 結果 |
|---|---|
| PowerShell parse / `git diff --check` | PASS |
| job permissions | `contents: read`、`packages: read`だけ |
| secret reference | Repository `GITHUB_TOKEN` 1件だけ |
| Action pin | checkout / setup-javaとも40文字commit SHA |
| Public API job cache | なし |
| normal `mvnw.cmd clean verify` | 69 tests、failure / error / skipped各`0` |
| Gate 3 fixture script | SUCCESS、正常系1 / 期待failure 2 |

local環境にYAML validatorはないため、workflow構文とActions tokenによるpackage readはPRのGitHub Actions
parse / fresh runnerを最終証拠とする。

### 9.4 required check承認前調査

GitHub APIで2026年8月27日に確認したmain保護は、classic branch protectionではなくRepository rulesetである。

| 項目 | 現況 |
|---|---|
| Ruleset | `main-merge-protection`、ID `21140116`、active |
| 対象 | default branch |
| strict policy | 有効 |
| required check | `Verify (ubuntu-24.04)`、GitHub Actions integration ID `15368` |
| bypass | なし。current userもbypass不可 |

`Public API Compatibility`はまだrequired checkへ追加していない。Gate 4では、workflowをcommit / pushして
PR fresh runnerを成功させ、Ownerが結果を承認した後に限り、既存rulesetのrequired checkへ同じ
GitHub Actions integration IDで追加する。

### 9.5 PR fresh runner Evidence

| 項目 | 実測値 |
|---|---|
| PR | [#18](https://github.com/zaziedlm/KOIKI-JAVAWEB/pull/18) |
| source commit | `dffe96c7e1a0b961a50619e47e9234c22870938e` |
| workflow run | [33029288981](https://github.com/zaziedlm/KOIKI-JAVAWEB/actions/runs/33029288981)、SUCCESS |
| `Verify (ubuntu-24.04)` | job `98377808697`、SUCCESS、3分48秒 |
| `Public API Compatibility` | job `98377808884`、SUCCESS、1分42秒 |

Public API jobのfresh Ubuntu runnerは次をすべて確認した。

| 検査 | remote結果 |
|---|---|
| authentication | `GITHUB_TOKEN with workflow packages: read` |
| Architecture Contract baseline | timestamp `0.1.0-20260826.091429-1`、SHA-256 MATCH |
| ArchUnit Rules baseline | timestamp `0.1.0-20260826.091429-1`、SHA-256 MATCH |
| inventory | 5 public型 / 4 annotation element / 2 Rules method、MATCH |
| 正式japicmp | 両artifactともpublic modifications `NONE`、exit `0` |
| package-private fixture | inventory MATCH、modifications `NONE`、exit `0` |
| breaking fixture | `METHOD_RETURN_TYPE_CHANGED`、expected failure PASS |
| addition fixture | inventory MISMATCH、`METHOD_ADDED_TO_PUBLIC_CLASS`、expected failure PASS |

job log全体をtoken形式で走査し、PAT / `GITHUB_TOKEN`実値に該当するliteralを検出しなかった。
全stepはcheckout credentialを保持せず完了し、artifact upload、Maven cache、追加secretまたはpackage writeを
使用していない。

### 9.6 Gate 4 Owner Review結果

1. PR #18の通常VerifyとPublic API Compatibilityが同一commitで成功している。
2. fresh runnerがC1 baseline identity、正式inventory / japicmpおよびGate 3 fixtureを再現している。
3. CI認証がRepository `GITHUB_TOKEN`と`packages: read`だけで、credential実値を露出していない。
4. `Public API Compatibility`をruleset ID `21140116`のrequired checkへ追加し、既存
   `Verify (ubuntu-24.04)`とstrict policyを維持する。
5. required check設定後のPR状態とC3 DoD 1a-5を確認し、C3を`COMPLETE`とする。

Architecture Ownerは2026年8月27日に上記内容と結果を確認し、Gate 4を承認した。承認に基づき、
required check設定とC3 closeoutを実施した。PRのmergeはcloseout文書のcommit / push後に行う。

### 9.7 required check反映・C3 closeout Evidence

Gate 4承認後、ruleset ID `21140116`へ`Public API Compatibility`を追加し、GitHub APIで設定を
読み戻した。

| 項目 | 反映・確認結果 |
|---|---|
| enforcement | `active`を維持 |
| required checks | `Verify (ubuntu-24.04)`、`Public API Compatibility` |
| GitHub Actions integration ID | 両checkとも`15368` |
| strict policy | 有効を維持 |
| PR保護 | deletion禁止、non-fast-forward禁止、PR必須を維持 |
| bypass | なしを維持 |
| PR #18 | head `dffe96c7e1a0b961a50619e47e9234c22870938e`、両required check `SUCCESS`、`CLEAN` |

Public API inventory固定、正式artifactの互換比較、public破壊と未承認追加の期待failure、internal変更の
許容、fresh runner、secret safetyおよびrequired checkを一連の証拠として確認したため、DoD 1a-5を満たす。
C3は`COMPLETE`、次回WPはC4 Java runtime matrixとする。
