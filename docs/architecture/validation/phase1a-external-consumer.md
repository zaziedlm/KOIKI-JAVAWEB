# Phase 1a External Consumer — C2実装・検証計画

**調査日:** 2026年8月26日<br>
**対象branch:** `feature/phase1a-external-consumer`<br>
**状態:** C2 GATE 1 ACCEPTED / GATE 2 NEXT<br>
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
| Phase / status | Phase 1a / Milestone C / C1・C2-0 COMPLETE / C2 Gate 1 ACCEPTED・Gate 2 NEXT |
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
| 2 | local独立Consumer、POM、Wrapper、settings template、PAT dry run | 空local repositoryからC1 snapshotだけを解決し、両Public API正常系と`KOIKI-ARCH-001`期待failureが成功する。credential非露出 | PENDING |
| 3 | GitHub Repository、package Actions access、workflow、remote CI | `GITHUB_TOKEN`の`packages: read`だけでfresh runnerが同じ検証に成功し、PAT secret・cache・KOIKI checkoutを使わない | PENDING |
| 4 | timestamp / checksum / dependency / CI / log Evidence、C2 closeout | C1 payloadとの一致、Consumer commit、再現手順、credential非露出が揃い、DoD 1a-3をC1 / C2の連続証拠で満たす | PENDING |

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
