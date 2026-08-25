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
