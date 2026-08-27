# GitHub Actions

Phase 1aでは、CIとartifact公開を別の権限境界として扱います。

## `ci.yml`

- pull requestと`main`へのpushで実行します。
- `ubuntu-24.04` / Temurin 21で、Maven Wrapperから`clean verify`を実行します。production runtimeを
  Linux-onlyとしたため、Windows matrixは再採用判断まで無効です。
- NullAwayのpositive、意図的違反のexpected failure、restoreを隔離fixtureから検証します。
- 通常の`Verify` jobは`contents: read`だけで、secretやpackage権限を使用しません。
- 独立した`Public API Compatibility` jobだけが`contents: read`と`packages: read`を持ち、C1 baseline、
  inventory、japicmp正常系とGate 3 fixtureを同じTooling scriptで検証します。
- Public API jobはRepositoryの`GITHUB_TOKEN`だけを使用し、追加PAT secret、Maven cache、
  package write / deleteまたは認証済みGit credentialを使用しません。
- checkout後に認証済みGit操作を行わないため、credentialをlocal Git configへ保持しません。
- 外部Actionはfull commit SHAで固定します。tag更新時も差分reviewを必要とします。
- 通常の`Verify` jobのMaven cacheは高速化だけに用い、配布artifactまたは検証証拠として扱いません。

内部snapshot公開はC1の独立workflowで扱います。`ci.yml`へpackage公開権限を追加しません。

## `runtime-compatibility.yml`

- pull request、`main` push、nightly schedule（03:17 JST）および`workflow_dispatch`で実行します。
- `Build Runtime Fixture (Java 21)` jobはTemurin 21とRepository WrapperでCLI JARを一度だけ生成し、
  class major `65`、source commit、working tree、JAR SHA-256をmanifestへ固定します。
- build jobはGate 3 negative guardsとJava 21 / 25 positive restoreを再現した後、JARとmanifestだけを
  retention 1日のimmutable workflow artifactとしてuploadします。packageまたはrelease artifactではありません。
- `Java Runtime Compatibility` jobはworkflow artifactをdownloadし、manifest commit、major、SHA-256を
  再確認してから、同一JARをTemurin 21 / 25で実行します。
- runtime jobはMaven、`javac`、compile、package、cacheまたはruntime別artifact生成を行いません。
- workflow全体は`contents: read`だけを使用し、secret、PAT、Packages権限、artifact公開または
  認証済みGit credentialを使用しません。
- checkout、setup-java、upload-artifact、download-artifactは公式release commitのfull SHAで固定します。
- required check候補は`Java Runtime Compatibility`である。PR fresh runner成功とOwner承認後にだけ、
  既存rulesetへ追加します。

## `publish-snapshot.yml`

- `workflow_dispatch`だけで起動し、preflight前にmain refとOwnerが承認した40文字のcommit SHAとの一致を検査します。
- Windows / Ubuntuのpreflightは`contents: read`だけで既存CI相当の検証を完走します。
- preflight成功後のpublish jobだけが`packages: write`を持ち、`phase1a-internal-snapshot` environmentを使います。
- Maven server ID `github`へRepository固有の`GITHUB_TOKEN`を接続し、PATをRepository secretへ保存しません。
- 配布先はworkflowの`altSnapshotDeploymentRepository`だけに指定し、BOM / ParentやCustomer POMへ継承させません。
- BOM、Parent、Architecture Contract、ArchUnit Rulesの4成果物だけを明示選択し、Root Reactorやfixtureを公開しません。
- `deployAtEnd=true`と単一実行のconcurrencyにより、build失敗時と同時実行時の部分公開riskを抑えます。
- 実公開はPOM / workflowのreview、local file repository dry run、main CIおよびOwner承認後にだけ実行します。
