# Phase 1a CI Build Foundation — A4実効検証

**検証日:** 2026年8月21日<br>
**対象branch:** `feature/phase1a-build-foundation`<br>
**状態:** ACCEPTED<br>
**A4 status:** COMPLETE<br>
**Architecture Owner:** Shuichi Kataoka<br>
**承認日:** 2026年8月21日<br>
**Ownership:** Tooling<br>
**対象:** A4 `.github/workflows/ci.yml`

## 1. 現時点の結論

G4-CIで承認した最小CI骨格を実装した。

- pull requestと`main`へのpushを起点とする。
- `windows-2025` / Temurin 21と`ubuntu-24.04` / Temurin 21を同格の必須候補とする。
- 各OSのMaven Wrapperから`clean verify`を実行する。
- workflow全体を`contents: read`だけに制限し、secretおよびpackage書込み権限を使用しない。
- checkout後の認証済みGit操作は不要なため、credentialをlocal Git configへ保持しない。
- 外部Actionをfull commit SHAで固定する。
- 同一workflow / Git refの旧実行を`concurrency`で取り消す。

ローカルとPR #6のGitHub Actionsで正式Reactorの`clean verify`が成功し、マージしないPR #7の
意図的負例でWindows / Ubuntu両jobが失敗することを確認した。さらに`main`の保護規則で両jobを
required checkとし、最新pushでマージ条件を満たすことを確認した。Architecture Owner Reviewにより、
本書を`ACCEPTED`、A4を`COMPLETE`とする。

## 2. Workflow契約

| 観点 | 実装 | 現在の判定 |
|---|---|---|
| trigger | `pull_request`、`main`への`push` | STATIC PASS |
| Windows | `windows-2025`、Temurin 21、`mvnw.cmd clean verify` | PASS |
| Linux | `ubuntu-24.04`、Temurin 21、`./mvnw clean verify` | PASS |
| permissions | `contents: read`だけ | STATIC PASS |
| secret | 参照なし | STATIC PASS |
| package write | 付与なし | STATIC PASS |
| checkout credential | `persist-credentials: false` | STATIC PASS |
| external actions | full commit SHA固定 | STATIC PASS |
| cache | `setup-java`のMaven cache。高速化用途だけ | STATIC PASS |
| concurrency | workflow / Git ref単位で旧実行を取消 | STATIC PASS |

## 3. 外部Action baseline

2026年8月21日に各公式GitHub Repositoryのrelease tagを照合した。

| Action | tag | 固定commit |
|---|---|---|
| `actions/checkout` | `v6.1.0` | `d23441a48e516b6c34aea4fa41551a30e30af803` |
| `actions/setup-java` | `v5.7.0` | `b6effb05e454b25005698d916606bdc6ffcbf961` |

workflowではtagを説明用commentとして残し、実際の参照にはfull commit SHAを使用する。

## 4. Spring Modulith Level 0経路

`ci.yml`は正式Reactor全体の`clean verify`を実行するため、B1 / B5でFeature Templateへ追加する
Spring Modulith Level 0 testも同じPR gateへ自動的に含まれる。A4では未使用のFeature Templateや
検証専用Java moduleを先行生成しない。

Spring Modulith 2.1.0と`spring-modulith-starter-test`のtest scope限定はA2 / G1で実効検証済みである。
Level 0 testの実行証拠はB1 / B5で追加し、runtime依存への混入がないことを継続確認する。

## 5. ローカル検証

| Command / 条件 | 結果 |
|---|---|
| `.\mvnw.cmd --batch-mode --no-transfer-progress clean verify` | 正式4-project Reactor、BUILD SUCCESS |
| Contract test | 4 test、failure 0、error 0、skipped 0 |
| `uses:`参照のSHA形式検査 | 2件中2件が40桁full commit SHA |
| workflow禁止設定の静的検査 | secret参照、`packages: write`、`pull_request_target`なし |
| `git diff --check` | 問題なし |

ローカル検証はWindows / JDK 21で実施した。GitHub hosted runner固有のimage、Action実行、cacheおよび
Linux実行はローカル結果で代替せず、次節のGitHub実行で確認した。

## 6. GitHub正常系検証

| 項目 | 結果 |
|---|---|
| Pull request | [PR #6](https://github.com/zaziedlm/KOIKI-JAVAWEB/pull/6) |
| 対象commit | `a7b07d31cbacdd087ef74bb1fe7e69c77205ac61` |
| Workflow run | [CI run 32471707932](https://github.com/zaziedlm/KOIKI-JAVAWEB/actions/runs/32471707932)、success |
| Ubuntu job | `Verify (ubuntu-24.04)`、job `96739675841`、success、Required |
| Windows job | `Verify (windows-2025)`、job `96739676064`、success、Required |

各jobでは対象OSのMaven Wrapper stepが成功し、他OS用stepは条件どおりskippedとなった。

## 7. GitHub負例検証

| 項目 | 結果 |
|---|---|
| Pull request | [PR #7](https://github.com/zaziedlm/KOIKI-JAVAWEB/pull/7)、closed、未merge |
| 対象commit | `85db8e827254d6acee060a8a5dedd5291d7c3ba8` |
| 変更 | `KoikiModuleContractTest`へ識別可能な意図的failureを1件だけ追加 |
| Workflow run | [CI run 32469817969](https://github.com/zaziedlm/KOIKI-JAVAWEB/actions/runs/32469817969)、failure（期待結果） |
| Ubuntu job | `Verify (ubuntu-24.04)`、job `96734072364`、failure（期待結果） |
| Windows job | `Verify (windows-2025)`、job `96734072610`、failure（期待結果） |
| failure marker | `A4 intentional CI negative verification` |
| 一時branch | `test/a4-ci-negative`をremote / localから削除済み |

ローカルでも同じ一時commitに対する`clean verify`が終了コード`1`となり、5 test中、意図的に追加した
1 testだけがfailureとなった。PR #7は正式branchへmergeせずcloseし、一時branchも削除したため、
Frameworkのproduction source、Public APIおよび正式test suiteへ負例を混入させていない。

## 8. Required checksとOwner Review

| 項目 | 判定 |
|---|---|
| 保護対象 | `main` |
| Required check 1 | `Verify (windows-2025)` |
| Required check 2 | `Verify (ubuntu-24.04)` |
| 動作確認 | PR #6の最新pushで両checkが一旦pendingとなり、成功後にマージ条件を満たした |
| Negative check | PR #7で両checkがfailureとなり、PRをmergeせずcloseした |
| Decision | ACCEPTED |
| A4 status | COMPLETE |
| Decided by | Shuichi Kataoka |
| Date | 2026年8月21日 |

Architecture Ownerは、正常系、負例、required check、read-only permission、credential非保持、
外部Actionのfull commit SHA固定および意図的failureの非混入を確認した。A4に残件はなく、後続WPが
同じPR quality gateへtestを追加できるCI基盤として承認する。

## 9. Deferred

- Java 21で生成した同一artifactのJava 21 / 25 runtime検証はC4で扱う。
- snapshot公開、`packages: write`、Consumer認証およびchecksum記録はC1で扱う。
- ArchUnit、NullAway、japicmpの正式なpositive / negative gateはB3、B4、C3で扱う。
- `pull_request_target`、workflowからのrelease、artifact署名およびProduction Deploymentは導入しない。
