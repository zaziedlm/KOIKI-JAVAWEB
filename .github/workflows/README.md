# GitHub Actions

Phase 1aでは、CIとartifact公開を別の権限境界として扱います。

## `ci.yml`

- pull requestと`main`へのpushで実行します。
- `windows-2025` / Temurin 21と`ubuntu-24.04` / Temurin 21で、Maven Wrapperから`clean verify`を実行します。
- 両OSでNullAwayのpositive、意図的違反のexpected failure、restoreを隔離fixtureから検証します。
- workflow権限は`contents: read`だけであり、secretや`packages: write`を使用しません。
- checkout後に認証済みGit操作を行わないため、credentialをlocal Git configへ保持しません。
- 外部Actionはfull commit SHAで固定します。tag更新時も差分reviewを必要とします。
- Maven cacheは高速化だけに用い、配布artifactまたは検証証拠として扱いません。

Java 21で一度だけbuildした同一artifactのJava 21 / 25 runtime検証はC4、内部snapshot公開はC1の
独立workflowで扱います。`ci.yml`へpackage公開権限を追加しません。

## `publish-snapshot.yml`

- `workflow_dispatch`だけで起動し、preflight前にmain refとOwnerが承認した40文字のcommit SHAとの一致を検査します。
- Windows / Ubuntuのpreflightは`contents: read`だけで既存CI相当の検証を完走します。
- preflight成功後のpublish jobだけが`packages: write`を持ち、`phase1a-internal-snapshot` environmentを使います。
- Maven server ID `github`へRepository固有の`GITHUB_TOKEN`を接続し、PATをRepository secretへ保存しません。
- 配布先はworkflowの`altSnapshotDeploymentRepository`だけに指定し、BOM / ParentやCustomer POMへ継承させません。
- BOM、Parent、Architecture Contract、ArchUnit Rulesの4成果物だけを明示選択し、Root Reactorやfixtureを公開しません。
- `deployAtEnd=true`と単一実行のconcurrencyにより、build失敗時と同時実行時の部分公開riskを抑えます。
- 実公開はPOM / workflowのreview、local file repository dry run、main CIおよびOwner承認後にだけ実行します。
