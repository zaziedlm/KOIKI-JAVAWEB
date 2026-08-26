# Phase 1a External Consumer — C2実装・検証計画

**調査日:** 2026年8月26日<br>
**対象branch:** `feature/phase1a-external-consumer`<br>
**状態:** C2 COMPLETE / GATE 1〜4 ACCEPTED<br>
**Ownership:** Tooling（独立Consumerは検証fixtureであり、Framework / Reference / Customer成果物ではない）<br>
**対象:** C2 Repository外Consumer、認証運用、ArchUnit正常系・意図的違反<br>
**開始baseline:** `a41dcde`（C2-0ドキュメント同期完了）<br>
**C1 Evidence:** `phase1a-internal-snapshot.md`

## 1. 目的と完了条件

Phase 1a実行計画のG2、G4、G5、Milestone CおよびDoD 1a-3に基づき、C1で公開した正式4成果物を、
KOIKI本体とは別のGit Repositoryに置くConsumerから利用できることを実証する。

C2は次をすべて満たしたときだけ`COMPLETE`とする。

1. ConsumerをKOIKI本体と別のlocal Git RepositoryおよびGitHub Repositoryへ置き、Root Reactorへ追加しない。
2. Maven Wrapper 3.9.16とJDK 21を使い、`koiki-parent`、BOM、Architecture Contract、ArchUnit Rulesを
   GitHub Packagesの`0.1.0-SNAPSHOT`だけから解決する。
3. Parentの`relativePath`、Root Reactor、`-am install`、共有済みKOIKI local artifact、Maven workspace reader
   またはpublisher checkoutへフォールバックしない。
4. localではPAT classicの`read:packages`だけ、GitHub ActionsではConsumer Repositoryの`GITHUB_TOKEN`と
   `packages: read`だけを用いる。package書込み・削除権限を与えない。
5. credential実値、Authorization headerまたは認証付きURLをPOM、settings template、workflow、log、
   artifactおよびValidationへ保存しない。
6. `KoikiArchitectureRules.businessModuleRules(String)`と
   `frameworkOwnershipRules(String, String...)`の正常系を、Consumer側production classに対して実行する。
7. Consumer所有の隔離fixtureで`KOIKI-ARCH-001`を意図的に発生させ、rule ID、`ADR-022`、影響、修正方法、
   source / targetの違反箇所を、公開snapshotのraw failureから確認する。
8. 通常`mvn verify`とCIはgreenに保ち、意図的違反はJUnitで期待failureとして評価する。赤いbranch、
   production sourceのfailure switchまたはKOIKI本体のtest fixtureには依存しない。
9. 実際に解決したtimestamped snapshot、公開元commit、対象POM / JARのSHA-256、local command、CI run、
   credential非露出およびConsumer commitを本書へ記録する。
10. Owner Review Gate 1〜4をすべて`ACCEPTED`とする。

## 2. 作業位置と境界

| 項目 | 内容 |
|---|---|
| Phase / status | Phase 1a / Milestone C / C1・C2-0・C2 COMPLETE / C3 NEXT |
| Ownership | Consumer Repository、fixture、workflow、検証scriptはTooling |
| Framework成果物 | C1公開済み4成果物を変更せず利用する |
| 本Repositoryの成果物 | 本Validation、実行計画・indexの状態更新だけ |
| Consumerの性質 | 配布しない検証fixture。Reference ApplicationまたはCustomer実装ではない |
| OpenSpec | Repositoryにchangeが存在しないため必須前提にしない |

C2ではPublic API、Maven coordinates、Parent / BOM、Architecture Contract、ArchUnit Rulesおよび
publish workflowを変更しない。変更が必要になった場合はC2内で迂回せず、該当Gateを停止してC1、G2または
G5のOwner Reviewへ戻す。

## 3. 2026年8月26日のread-only調査結果

### 3.1 Git・build環境

| 項目 | 結果 |
|---|---|
| branch / HEAD | `feature/phase1a-external-consumer` / `a41dcde` |
| worktree | clean |
| JDK | Eclipse Temurin 21.0.12.1 |
| Maven Wrapper | Apache Maven 3.9.16 |
| Root Reactor | 正式4moduleだけ。Consumer moduleなし |
| OpenSpec | `openspec/`なし |

### 3.2 GitHub Repository・Packages

read-onlyのGitHub APIで次を確認した。

| 項目 | 結果 |
|---|---|
| 公開元 | `zaziedlm/KOIKI-JAVAWEB`、PUBLIC、default branch `main` |
| Consumer候補 | `KOIKI-JAVAWEB-PHASE1A-CONSUMER`というRepositoryは未作成 |
| Maven package | C1対象4件だけが`KOIKI-JAVAWEB`に関連付いて存在 |
| package visibility | 4件ともpublic |
| package version | 4件とも`0.1.0-SNAPSHOT`が1 version存在 |
| 匿名metadata取得 | credentialなしのHEADはHTTP `401` |

C1で確定した参照対象は、公開元commit
`9573b1cf38713d51707a14884230d5bd5e1d97fb`、timestamped snapshot
`0.1.0-20260826.091429-1`である。6 payloadのSHA-256はC1 Evidence §6.6を正本とし、C2では
Consumerが実際に取得したファイルを再計算して一致を確認する。

## 4. 現行公式仕様との照合

2026年8月26日にGitHub DocsとApache Maven公式文書を確認した。

1. GitHub Packagesはprivate / internal / public packageのpublish、install、deleteすべてにaccess tokenを
   要求する。今回の匿名HTTP `401`もこの仕様と一致する。
2. GitHub Packagesの認証に利用できるPersonal Access TokenはPAT classicである。download / installには
   `read:packages` scopeと対象Repositoryへのread permissionが必要である。
3. Actionsでは、別Repositoryに関連付くpackageも、Consumer Repositoryへpackage read accessを付与すれば
   `GITHUB_TOKEN`でinstallできる。workflow jobは`contents: read`と`packages: read`だけを明示する。
4. Apache Maven registryはrepository-scoped permissionだけをサポートし、packageは公開元Repositoryへ
   常に関連付き、そのpermissionとvisibilityを継承する。
5. GitHub PackagesのApache Maven registryは`SNAPSHOT` versionをサポートし、repository URLで
   snapshotsを有効化して取得する。
6. Mavenのrepository IDと`settings.xml`のserver IDを一致させる。username / passwordはPOMへ置かず、
   `settings.xml`で保持する。Maven settingsは`${env.X}`による環境変数展開をサポートする。

参照した公式文書:

- [GitHub Packages — Working with the Apache Maven registry](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-apache-maven-registry)
- [GitHub Packages — Permissions](https://docs.github.com/en/packages/learn-github-packages/about-permissions-for-github-packages)
- [GitHub Packages — Access control and Actions access](https://docs.github.com/en/packages/learn-github-packages/configuring-a-packages-access-control-and-visibility)
- [GitHub Actions — Use GITHUB_TOKEN for authentication](https://docs.github.com/en/actions/tutorials/authenticate-with-github_token)
- [Apache Maven Settings Reference](https://maven.apache.org/settings.html)

## 5. Gate 1承認済み設計

### 5.1 Consumer Repository

| 項目 | Gate 1承認内容 |
|---|---|
| GitHub Repository | `zaziedlm/KOIKI-JAVAWEB-PHASE1A-CONSUMER` |
| visibility | PUBLIC。fixture sourceとworkflowだけを置き、secretを含めない |
| local配置 | KOIKI本体の外側に独立directory / `.git`として配置する |
| Maven coordinate | `com.example.koiki:phase1a-external-consumer:0.0.0-SNAPSHOT`。非公開・非配布 |
| Java base package | `com.example.koiki.consumer` |
| Maven | Consumer自身の公式Wrapper 3.9.16、JDK 21 |
| KOIKI Parent | `org.koikifw:koiki-parent:0.1.0-SNAPSHOT`、`<relativePath/>`必須 |
| BOM | Parent経由のimportを実効POMで確認する |
| dependencies | Architecture Contractは通常scope、ArchUnit RulesとJUnitはtest scope |
| repository | `github` ID、C1 URL、snapshots enabled。credential値なし |

Repository名、Maven coordinateおよびJava packageは検証fixtureの識別子であり、正式Framework、Reference、
CustomerまたはProject Templateの命名を確定しない。

### 5.2 Consumer構成

```text
KOIKI-JAVAWEB-PHASE1A-CONSUMER/
├── .github/workflows/verify.yml
├── .mvn/wrapper/
├── src/
│   ├── main/java/com/example/koiki/consumer/
│   │   ├── business/                   # compliant Tier module
│   │   ├── framework/                  # ownership rule用の最小Framework相当fixture
│   │   └── customer/                   # ownership rule用の最小Consumer相当fixture
│   └── test/java/com/example/koiki/consumer/
│       ├── CompliantArchitectureTest.java
│       └── IntentionalViolationContractTest.java
├── settings.xml.example
├── pom.xml
├── mvnw
├── mvnw.cmd
└── README.md
```

`framework` / `customer` packageは`frameworkOwnershipRules`の入力境界を外部から実演するための値だけのfixtureで、
KOIKI FrameworkまたはCustomer業務コードではない。`business`も業務語彙を持たない最小classだけとする。

### 5.3 認証・権限設計

#### Local

- PAT classicは`read:packages`だけを持つものを利用する。
- `settings.xml.example`は`${env.KOIKI_PACKAGES_USER}`と`${env.KOIKI_PACKAGES_TOKEN}`を参照し、実値を持たない。
- 実値入りsettingsはtracked directory外の一時directoryに生成し、検証後に削除する。
- Mavenは空の一時local repositoryを`-Dmaven.repo.local=<temp>`で指定し、通常の`~/.m2/repository`を使わない。
- command / logへtokenを引数展開せず、debug / effective-settingsのcredential出力も行わない。

#### GitHub Actions

- Consumer Repository作成後、C1の4 packageそれぞれでConsumer RepositoryへActions read accessを付与する。
- workflowは`permissions: contents: read`を既定とし、verify jobだけに`packages: read`を追加する。
- `GITHUB_TOKEN`は`${{ secrets.GITHUB_TOKEN }}`から環境変数へ渡し、追加PAT secretを作らない。
- checkoutは`persist-credentials: false`、外部Actionはfull commit SHA固定、Maven cacheは初回Evidenceでは無効とする。
- pull request由来codeへwrite tokenを渡さず、package publish / delete APIを呼ばない。

### 5.4 検証fixture

1. `CompliantArchitectureTest`はproduction classだけをpackage指定でimportし、
   `businessModuleRules`と`frameworkOwnershipRules`の両方が成功することを確認する。
2. `IntentionalViolationContractTest`はtest sourceに隔離した同一moduleの
   `adapter.inbound` → `adapter.outbound`直接依存だけをimportする。
3. `businessModuleRules(...).check(...)`が投げる`AssertionError`を期待failureとして捕捉し、次を検査する。
   - `[KOIKI-ARCH-001] [ADR-022]`
   - `影響: Inboundが技術実装へ結合し、依存方向と差替え境界が崩れる`
   - `修正: Application Use CaseとPortを介する`
   - inbound source classとoutbound target class
4. fixtureには他のKOIKI rule違反を含めず、偶然別ruleで成功したと判定しない。
5. 通常`mvn verify`は、正常系成功と期待failure契約確認を含めて終了コード`0`とする。

## 6. 実装・Owner Review Gate

| Gate | Review対象 | 承認条件 | 状態 |
|---:|---|---|---|
| 1 | read-only調査、Repository / Maven / fixture / 認証設計、完了・停止条件 | C2を別Repository・Tooling fixtureへ限定し、PAT / `GITHUB_TOKEN`、独立解決、正常系・負例の証拠を推測なく実装できる | ACCEPTED（2026年8月26日、Shuichi Kataoka） |
| 2 | local独立Consumer、POM、Wrapper、settings template、PAT dry run | 空local repositoryからC1 snapshotだけを解決し、両Public API正常系と`KOIKI-ARCH-001`期待failureが成功する。credential非露出 | ACCEPTED — 2026年8月26日、Shuichi Kataoka |
| 3 | GitHub Repository、package Actions access、workflow、remote CI | `GITHUB_TOKEN`の`packages: read`だけでfresh runnerが同じ検証に成功し、PAT secret・cache・KOIKI checkoutを使わない | ACCEPTED — 2026年8月26日、Shuichi Kataoka |
| 4 | timestamp / checksum / dependency / CI / log Evidence、C2 closeout | C1 payloadとの一致、Consumer commit、再現手順、credential非露出が揃い、DoD 1a-3をC1 / C2の連続証拠で満たす | ACCEPTED — 2026年8月26日、Shuichi Kataoka |

Gate 1承認により、Gate 2のlocal独立Consumer、POM、Wrapper、settings templateおよびPAT dry runへ進める。
Gate 2承認前はGitHub Repositoryを作成・pushせず、package Actions accessも変更しない。Gate 3成功前に
C2を完了扱いしない。

## 7. Evidence計画

| 証拠 | 記録内容 |
|---|---|
| Consumer identity | Repository URL、visibility、commit SHA、tracked tree |
| Build environment | OS、JDK vendor / version、Maven Wrapper version |
| Independence | KOIKI checkoutなし、`relativePath`なし、空local repository、cacheなし、resolved repository URL |
| Artifact identity | base / timestamped version、6 payloadのSHA-256、C1 Evidenceとの比較 |
| Dependency | effective POM、dependency tree、Contract通常scope、Rules / JUnit test scope |
| Public API | 1 public class / 2 public static methodをConsumer testから実行 |
| Positive | compliant business / ownership ruleの成功、test件数、終了コード`0` |
| Negative | `KOIKI-ARCH-001`、ADR、影響、修正、source / target、他rule非混入 |
| Local auth | PAT classic `read:packages`、credential値非記録、隔離settings削除 |
| Actions auth | `GITHUB_TOKEN`、`packages: read`、package Actions access、run URL / ID |
| Secret safety | POM / template / workflow / log / artifactにcredential実値・認証付きURLなし |

## 8. Stop / Return condition

- C1の公開元commit、timestamped snapshotまたはSHA-256とConsumer取得物が一致しない。
- 4成果物以外のKOIKI artifact、Root Reactor、publisher checkout、`install`または共有local repositoryが必要になる。
- `koiki-parent`のremote解決にParent / BOM / Public API変更が必要になる。
- PAT classicの`read:packages`を超えるscope、package write / delete、追加PAT Actions secretが必要になる。
- Consumer RepositoryへActions read accessを付与できず、`GITHUB_TOKEN`で取得できない。
- credential、Authorization header、tokenを含むURLまたはsettings実値がtracked file、log、artifactへ出る。
- Consumer正常fixtureが失敗する、意図的fixtureが`KOIKI-ARCH-001`を検出しない、または別ruleだけで失敗する。
- ConsumerをFramework、Reference、Customer、Feature Templateまたは正式配布物へ昇格する必要が生じる。
- C3のjapicmp、C4のruntime fixture、C5のWalking Skeleton処置または後続Phase成果物が必要になる。

Stop条件に該当した場合は、credential scope拡大、local install、rule無効化または本RepositoryへのConsumer追加で
迂回せず、該当Gateを停止してArchitecture Ownerへ戻す。

## 9. 作業負荷の再校正

| 作業 | 想定range |
|---|---|
| Gate 1 Reviewと条件反映 | 0.5〜1時間 |
| 独立Consumer、Wrapper、POM、fixture実装 | 2〜4時間 |
| local PAT・空repository検証 | 1〜2時間 |
| GitHub Repository、Actions access、CI検証 | 1〜3時間 |
| checksum・dependency・log EvidenceとCloseout | 1〜2時間 |

合計rangeは5.5〜12時間とする。GitHub側障害、PAT再発行、Owner承認待ちおよび予期しないsnapshot差異の調査は
range外とし、経過日数または納期として扱わない。

## 10. Gate 1 Owner Review結果

| 項目 | 判定 |
|---|---|
| Decision | ACCEPTED |
| Decided by | Shuichi Kataoka |
| Date | 2026年8月26日 |
| Scope | §1〜§9のC2完了条件、調査結果、Consumer / Maven / fixture / 認証設計、Gate、Evidence、Stop条件 |
| Next | Gate 2 local独立Consumer実装とPAT dry run |

Architecture Ownerは次の5項目を確認し、Gate 1として承認した。

1. Consumer Repositoryを`zaziedlm/KOIKI-JAVAWEB-PHASE1A-CONSUMER`、PUBLICで新規作成する。
2. local PAT classic `read:packages`とcross-repository `GITHUB_TOKEN`の両経路をC2必須証拠とする。
3. 両Public APIの正常系と、`KOIKI-ARCH-001`の期待failureを最小fixtureとする。
4. Gate 2まではlocalの独立Repositoryだけ、Gate 2承認後にGitHub Repository作成・push・4 packageの
   Actions read access設定を行う。
5. 新規ADR、Skill変更およびmigrationは不要とし、C2 Evidenceだけを本Repositoryへ記録する。

Gate 1承認はGate 2のlocal作業だけを許可する。GitHub Repository作成、push、package Actions access変更、
secret登録またはworkflow実行はGate 2のEvidenceとOwner Review後まで行わない。

## 11. Gate 2実装状況

### 11.1 local独立Consumer

2026年8月26日に、KOIKI本体のSibling directoryへ次の独立local Git Repositoryを作成した。

```text
C:\Users\kataoka\Desktop\KOIKI-JAVA\KOIKI-JAVAWEB-PHASE1A-CONSUMER
```

Gate 2検証時点ではGitHub Repository、remote、commit、workflowおよびpackage Actions accessは未作成であった。local Repositoryには
次を実装した。

- Consumer自身のMaven Wrapper 3.3.4 / Maven 3.9.16
- remote Parentの`<relativePath/>`とC1 GitHub Packages URLを持つ単一Consumer POM
- Architecture Contract通常scope、ArchUnit Rules / JUnit test scope
- `settings.xml.example`の`${env.KOIKI_PACKAGES_USER}` / `${env.KOIKI_PACKAGES_TOKEN}`参照
- `businessModuleRules`と`frameworkOwnershipRules`のcompliant production fixture
- test sourceへ隔離した`adapter.inbound` → `adapter.outbound`の`KOIKI-ARCH-001`負例
- PAT scope、空local Maven repository、C1 timestamp / SHA-256、後片付けを検証する`verify-local.ps1`

### 11.2 実装静的検査

| 検査 | 結果 |
|---|---|
| local Git Repository | 独立`.git`、branch `main`、commit / remoteなし |
| 本Repository worktree | Consumer source非混入。Validation更新前はclean |
| POM / settings | XML parse成功 |
| `verify-local.ps1` | PowerShell parser error 0 |
| credential literal scan | PAT形式、Authorization実値、Basic credential、固定passwordなし |
| Parent fallback | `<relativePath/>`を明示 |
| Wrapper | Maven Wrapper 3.3.4、Maven 3.9.16 distribution |

### 11.3 PAT scope確認と現在の停止点

利用可能だった既存GitHub tokenのOAuth scope headerは
`gist, read:org, read:packages, repo, workflow`であり、Gate 1で承認した`read:packages`だけのPAT classicでは
なかった。このtokenはMaven downloadへ使用していない。

`verify-local.ps1`へ既存tokenを渡し、scope検査がMaven起動前に期待どおり拒否することを確認した。
credential値はcommand、出力、tracked fileまたはValidationへ記録していない。

### 11.4 初回PAT dry runとfixture修正

`read:packages`だけのPAT classicを用いた初回dry runでは、scope検査とremote snapshot解決に成功し、
Consumerのcompile / testCompileおよびPublic API正常系2テストが成功した。credential値は共有された出力へ露出していない。

意図的違反テストは`KOIKI-ARCH-001`を検出したが、Consumer testが違反明細数を固定値`1`と仮定していたため、
実際の`2`明細に対して失敗した。fixtureの1つのJava依存が戻り値型とコンストラクタ呼出しの2明細として報告されるためであり、
C1 artifactの解決・Public APIまたはArchitecture Contractの不具合ではない。

Consumer testは件数固定を廃止し、すべての違反明細が同じ`NegativeInbound`から`NegativeOutbound`への依存を
示すことを検証するよう修正した。

### 11.5 Gate 2再実行結果

2026年8月26日23時01分（JST）に、修正後の`verify-local.ps1`を再実行し、終了コード`0`で完走した。

| 検証 | 結果 |
|---|---|
| 認証 | PAT classic、OAuth scopeは`read:packages`だけ |
| 独立解決 | GUID付きの空temporary Maven repositoryからGitHub Packagesを解決 |
| Build runtime | Maven 3.9.16、JDK 21、`clean verify`成功 |
| Public API正常系 | `CompliantArchitectureTest` 2件成功（business module / framework ownership） |
| 意図的違反 | `IntentionalViolationContractTest` 1件成功（`KOIKI-ARCH-001` / ADR-022、source / target確認） |
| Test合計 | 3件、Failures 0、Errors 0、Skipped 0 |
| Timestamped snapshot | `0.1.0-20260826.091429-1`だけを確認 |
| Secret safety | secure promptを使用し、共有出力にPAT、Authorization header、認証付きURLなし |
| Cleanup | 実行後に`koiki-phase1a-consumer-*` temporary directory残存なし |

取得したC1 payloadは次のSHA-256とすべて一致した。

| Payload | SHA-256 | 結果 |
|---|---|---|
| Dependencies BOM POM | `63C7AB55E1BB2FE290E795A59212B6314F0347104DC9B536BD4EBDBE903183DF` | MATCH |
| Parent POM | `ADC149D5C693BDCCBA008FD5F6BE8D5DF3BE5F43DCEBACCB47A72892C4BDAE37` | MATCH |
| Architecture Contract POM | `7BE7635FE5E776FB0F5B5E4935DB0054D08F6D3CCE01EE7016275C685E1D926F` | MATCH |
| Architecture Contract JAR | `947EE8CF0E109FE58D81E6008A56C06C8F4C035FF76BDF462F8F6BD9BB50DE45` | MATCH |
| ArchUnit Rules POM | `7B24A824B9EBD55794B7A626AE0FBB52A0781FFD1ECEFC18A899B629F4FEDA45` | MATCH |
| ArchUnit Rules JAR | `A51E26E7386D19E53C18BD63BC4E4F95EC1EAE471F39D519D6AE0CBC7C2DF3F2` | MATCH |

Gate 2の実装・検証条件は満たした。

## 12. Gate 2 Owner Review結果

| 項目 | 結果 |
|---|---|
| Decision | ACCEPTED |
| Decided by | Shuichi Kataoka |
| Date | 2026年8月26日 |
| Scope | §11のlocal独立Consumer、PAT scope、remote snapshot解決、正常系・意図的違反、timestamp・SHA-256、credential非露出 |
| Next | Gate 3 GitHub Repository、package Actions access、workflow、remote CI |

Architecture OwnerはGate 2の内容と結果を承認した。C2は`IN PROGRESS`のままGate 3へ進み、Gate 3成功前に
C2を完了扱いしない。

## 13. Gate 3実装状況

### 13.1 Consumer workflow

Gate 3開始承認後、Consumerへ`.github/workflows/verify.yml`を実装し、初回local commitを作成した。

| 項目 | 内容 |
|---|---|
| Consumer commits | `598c599b06477bb31f2f7a3a7482b9f89d17742c`（初回）、`e3504648377c4e1f2cf3f39fd917abfc613095ff`（Linux修正） |
| Runner | `ubuntu-24.04`、Temurin 21 |
| Workflow permissions | workflow既定`contents: read`、verify jobだけ`contents: read` / `packages: read` |
| Authentication | `${{ secrets.GITHUB_TOKEN }}`を環境変数経由でMaven settingsへ渡す |
| Independence | Maven cacheなし、GUID付き空local repository、KOIKI checkoutなし |
| Checks | `clean verify`、3 tests、timestamped snapshot、6 payload SHA-256 |
| Action pin | `actions/checkout`と`actions/setup-java`をfull commit SHA固定 |
| Secret safety | PAT secretなし、`persist-credentials: false`、debug出力なし |

`verify-local.ps1`はlocal PAT経路のscope検査を維持し、`-GitHubActions`は`GITHUB_ACTIONS=true`のrunnerでだけ
利用可能とした。PowerShell AST、credential value scanおよび`git diff --check`は成功した。

### 13.2 GitHub認証とRepository作成

GitHub CLIの保存済み`zaziedlm`認証は失効しており、利用可能なサインイン済みbrowser接続もなかった。
OwnerがGitHub CLIを再認証した後、次のPUBLIC Repositoryを作成し、Consumer `main`をpushした。

```text
https://github.com/zaziedlm/KOIKI-JAVAWEB-PHASE1A-CONSUMER
```

Repository IDは`R_kgDOUFAFrw`、default branchは`main`である。追加Actions secretは登録せず、Repositoryの
default workflow permissionは`read`、pull request review承認権限は無効である。

### 13.3 初回remote CIとcross-platform修正

初回pushのrun `32979212106`は、package認証へ到達する前にConsumer scriptのcross-platform不備で失敗した。
Linux runnerでWindows用`mvnw.cmd`を選択し、temporary path検査もWindows path separator固定だった。

`verify-local.ps1`をOS中立化し、Linuxでは`mvnw`を選択して実行権限を付与し、path separatorと比較規則を
OSごとに切り替えた。PowerShell AST検査後、commit `e3504648377c4e1f2cf3f39fd917abfc613095ff`としてpushした。
C1 artifact、Architecture Contractまたはpackage認証の不具合ではない。

### 13.4 成功remote CI

修正後のpush runは次のとおり成功した。

| 項目 | 結果 |
|---|---|
| Workflow run | `32979522941` |
| URL | `https://github.com/zaziedlm/KOIKI-JAVAWEB-PHASE1A-CONSUMER/actions/runs/32979522941` |
| Commit | `e3504648377c4e1f2cf3f39fd917abfc613095ff` |
| Runner | GitHub-hosted Ubuntu 24.04.4、Temurin 21.0.12+8、fresh checkout |
| Effective token permissions | Contents read、Metadata read、Packages read |
| Authentication | Consumer Repositoryの`GITHUB_TOKEN`。追加PAT secretなし |
| Maven isolation | cacheなし、GUID付き空temporary local repository、KOIKI checkoutなし |
| Build | `clean verify`、終了コード`0`、BUILD SUCCESS |
| Tests | 3件、Failures 0、Errors 0、Skipped 0 |
| Timestamped snapshot | `0.1.0-20260826.091429-1` |
| Payload | C1の6 POM / JARすべてでSHA-256 MATCH |
| Credential safety | tokenはGitHubにより`***`へmaskされ、credential実値・認証付きURLの出力なし |

### 13.5 package accessに関する実装確認

Gate 1では4 packageそれぞれへのConsumer RepositoryのActions read access付与を想定した。一方、Apache Maven
registryはrepository-scoped permissionで、C1の4 packageはすべてPUBLICかつ`zaziedlm/KOIKI-JAVAWEB`へ
関連付いている。明示的なpackage access変更を行う前に、Consumerの`GITHUB_TOKEN`とjobの`packages: read`だけで
4 packageの取得、timestampおよびSHA-256検証が成功した。

したがって明示的なActions access変更は不要かつ実施していない。Gate 1の安全目的であるcross-repository
`GITHUB_TOKEN`読取りは、より小さい外部状態変更で実証された。この実装確認をGate 3 Owner Review対象とする。

Gate 3の技術条件は満たした。

## 14. Gate 3 Owner Review結果

| 項目 | 結果 |
|---|---|
| Decision | ACCEPTED |
| Decided by | Shuichi Kataoka |
| Date | 2026年8月26日 |
| Scope | §13のPUBLIC Consumer Repository、workflow、cross-platform修正、remote CI、token権限、package access実装確認 |
| Variance accepted | repository-scoped PUBLIC Maven packageは明示的Actions access変更なしで`GITHUB_TOKEN`読取りに成功 |
| Next | Gate 4 timestamp / checksum / dependency / CI / log EvidenceとC2 closeout |

Architecture OwnerはGate 3の内容と結果、およびGate 1時点のpackage access想定との差分を承認した。
C2は`IN PROGRESS`のままGate 4へ進み、Gate 4承認前にC2を完了扱いしない。

## 15. Gate 4 closeout検証

### 15.1 最終Consumer identity

| 項目 | 結果 |
|---|---|
| Repository | `https://github.com/zaziedlm/KOIKI-JAVAWEB-PHASE1A-CONSUMER`、PUBLIC |
| Default branch | `main` |
| Final candidate commit | `178f5e90c867bb59d91e8c7f05a53124dbeae729` |
| Local / remote | local `HEAD`と`origin/main`が同一commit |
| Tracked tree | 20 files。Consumer POM / Wrapper / source / tests / workflow / scriptsだけ |
| KOIKI本体 | Root ReactorへConsumerを追加せず、Framework artifact変更なし |

final candidate commitでは、Gate 4 Evidence計画で未確定だったeffective POMとdependency treeのscope検証を
`verify-local.ps1`へ追加した。生成物はGUID付きtemporary directoryだけへ置き、検証後に削除する。

### 15.2 Dependency Evidence

2026年8月26日23時28分（JST）開始のpush runで、effective POMとdependency treeをremote artifactから生成した。

| Dependency | Effective scope | Dependency tree |
|---|---:|---|
| `org.koikifw:koiki-architecture-contract:0.1.0-SNAPSHOT` | compile | direct compile entry確認 |
| `org.koikifw:koiki-archunit-rules:0.1.0-SNAPSHOT` | test | direct test entry確認 |
| `org.junit.jupiter:junit-jupiter` | test | effective test scope確認 |

Parentは`org.koikifw:koiki-parent:0.1.0-SNAPSHOT`を`<relativePath/>`でremote解決し、Dependencies BOM POMも
空local repositoryへ取得してC1 SHA-256と一致した。通常の`~/.m2/repository`、Root Reactor、workspace reader、
publisher checkoutまたは`install`は使用していない。

### 15.3 最終remote CIとlog監査

| 項目 | 結果 |
|---|---|
| Workflow run | `32980511364` |
| URL | `https://github.com/zaziedlm/KOIKI-JAVAWEB-PHASE1A-CONSUMER/actions/runs/32980511364` |
| Commit | `178f5e90c867bb59d91e8c7f05a53124dbeae729` |
| Result | SUCCESS、job `98215726801`、約1分3秒 |
| Runtime | Ubuntu 24.04、Temurin 21、Maven Wrapper 3.9.16 |
| Token permission | Contents read、Metadata read、Packages read |
| Secret inventory | Actions Repository secret 0件、追加PATなし |
| Tests | 3件、Failures 0、Errors 0、Skipped 0 |
| Artifact identity | `0.1.0-20260826.091429-1`、6 payload SHA-256 MATCH |
| Dependency | effective POM scope 3件とKOIKI direct dependency tree 2件をassert |
| Log scan | PAT形式、Bearer / Basic credential値、credential付きURLの検出0件 |

GitHub標準Actionがcheckout中に利用するtoken表示は`***`へmaskされ、`persist-credentials: false`によりcheckout直後に
削除された。Consumer scriptはtoken値、Maven Authorization header、settings実値またはeffective POMを出力・保存しない。

### 15.4 C2完了条件traceability

| §1 | 判定 | Evidence |
|---:|---|---|
| 1 | PASS | 別local Git Repositoryと別PUBLIC GitHub Repository、Root Reactor非追加 |
| 2 | PASS | Wrapper 3.9.16 / JDK 21、正式4成果物をGitHub Packagesから解決 |
| 3 | PASS | `<relativePath/>`、空local repository、KOIKI checkout / installなし |
| 4 | PASS | local PAT classic `read:packages`のみ、Actions `GITHUB_TOKEN` packages readのみ |
| 5 | PASS | tracked tree、local出力、成功run logにcredential実値なし |
| 6 | PASS | 両Public APIのcompliant production fixture 2 tests成功 |
| 7 | PASS | `KOIKI-ARCH-001`、ADR-022、影響、修正、source / targetを期待failureで確認 |
| 8 | PASS | 意図的違反をJUnitで捕捉し、通常`verify`とCI green |
| 9 | PASS | timestamp、公開元commit、6 SHA-256、commands、run、Consumer commitを記録 |
| 10 | PASS | Gate 1〜4 ACCEPTED |

DoD 1a-3は、C1の公開証拠とC2の独立Consumerによるlocal / remote取得・違反検出の連続証拠で満たした。

## 16. Gate 4 Owner Review・C2 closeout結果

| 項目 | 結果 |
|---|---|
| Gate 4 Decision | ACCEPTED |
| C2 Decision | COMPLETE |
| DoD | 1a-3 COMPLETE |
| Decided by | Shuichi Kataoka |
| Date | 2026年8月26日 |
| Scope | §15のConsumer identity、dependency、timestamp、checksum、CI、log、secret safety、traceability |
| Next | C3 Public API Compatibility / japicmp |

Architecture OwnerはGate 4の内容と結果を承認した。これにより§1の完了条件1〜10、Gate 1〜4および
DoD 1a-3をすべて満たし、C2を`COMPLETE`とする。

C2ではPublic API、Maven coordinates、C1 artifact、Root Reactor、ADR、Skillまたはmigrationを変更していない。
ConsumerはTooling-ownedの非配布fixtureとして独立Repositoryに維持し、Framework、ReferenceまたはCustomer成果物へ
昇格しない。Milestone Cの次回WPはC3とする。
