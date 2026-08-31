# Phase 2 P2-A1 start handoff

## 1. Purpose and status

本書は、2026年8月31日までのPhase 2 Security Foundation設計・承認作業を、別PCの新規AI対話sessionへ
引き継ぎ、P2-A1の正式実装を安全に開始するためのhandoffである。

- **Handoff date:** 2026年8月31日
- **Architecture Owner:** Shuichi Kataoka
- **Branch:** `feature/phase2-security-foundation`
- **Handoff base HEAD:** `44f6107349bdf8ba52a2e696f8626ce8b5ce0ccc`
- **Base commit subject:** `docs: Phase 2 Security Foundationの実装開始を承認`
- **Phase status:** `GATE P2-2 APPROVED — P2-A1 READY`
- **Production implementation:** 未開始
- **Worktree before this handoff file:** clean
- **Upstream tracking branch:** 未設定

`44f6107`には、P2-F1〜F4、Gate F、Gate P2-2の承認、Oracle scope再配置、Grand Design、ADR、
実行計画およびvalidation Evidenceの同期が含まれる。本handoff追加後は新しい未コミット差分が生じるため、
別PCへ切り替える前に本書をcommitし、branchをremoteへpushする。

## 2. Required reading order on the next PC

新規sessionでは次を順に読む。

1. `AGENTS.md`
2. `docs/agent/skills/koiki-project-overview/SKILL.md`
3. 本書
4. `KOIKI-JavaWeb-FW_Phase2実行計画_v0.1.md`
5. `../architecture/validation/phase2-security-test-design.md`
6. `../architecture/validation/phase2-security-semantics-fitting.md`
7. `../architecture/validation/phase2-koiki-pyfw-security-fitting.md`
8. `../architecture/validation/phase2-token-lifecycle-phase-decision.md`
9. `../architecture/grand-design/KOIKI-JavaWeb-FW_グランドデザイン_v0.2.md`のSecurity、Session、Phase 2、ADR節
10. `../architecture/adr/README.md`

`phase2-security-foundation-start-handoff-20260830.md`は開始時点のhistorical baselineである。現行計画と異なる場合は、
Grand Design、ADR、Phase 2実行計画、validation Evidence、本書を優先する。

## 3. Completed decisions and evidence

次は完了・承認済みであり、P2-A1開始時に再度OPENへ戻さない。

- P2-F1〜F4とGate Fは`COMPLETE`
- Gate P2-2は6観点のOwner reviewを経て`APPROVED`
- Phase 2はOAuth2 Client / Resource Serverまで。KOIKI-hosted Authorization Serverはoptional `P4-AS`
- same-origin Session、Customer-owned BFF、direct Tokenを別profileとして扱う
- required OIDC testはcredential不要のlocal ephemeral issuer。hosted Cognitoはoptional acceptance
- PostgreSQL／Aurora PostgreSQLをproduction baselineとする
- Oracleは採用確度の低い将来optional `P4-ORACLE`。Phase 2でImage、Driver、Migration、CIを選定しない
- Spring Session JDBC、Framework Flyway、non-web cleanup / single executionを正本とする
- email login identifierとimmutable Framework user IDを分離する
- Python版endpoint、自前JWT、JWT Cookie、migrationを直接移植しない
- production code、dependency、Migration、workflowはまだ追加していない

## 4. P2-A1 work position

```text
Phase / status: Phase 2 / Gate P2-2 APPROVED / P2-A1 READY
Ownership: Framework（正式Security Starter）+ Tooling（非配布verification fixture）
Target module: 新規koiki-starter-security候補とP2-A1専用非配布fixture
Applicable guidance: koiki-project-overview、Grand Design、Phase 2 plan、security test design
Validation: T0 dependency / context + T1 request / deny、focused verify、root verify、Public API inventory
Deferred decisions: local identity、OIDC / JWT、Audit、Session JDBC、Reference identity、production Migration
```

P2-A1は機能検証だけのspikeではない。業務applicationが利用する正式な`koiki-starter-security`の最小基盤を、
非配布Harnessで実証しながら同じCPで実装する。ただし、Harness、test user、test route、test keyは正式成果物へ昇格しない。

## 5. P2-A1 task checklist

### A1-0 — Next-PC preflight

- remoteから`feature/phase2-security-foundation`を取得する
- HEADが本handoffを追加したcommitと一致し、`git status`がcleanであることを確認する
- Repository Maven WrapperとJava 21を使用する
- root `verify`を変更前baselineとして実行する
- Maven local repositoryだけに存在する未公開snapshotを前提にしない

### A1-1 — Artifact and fixture boundary

- 既存Starter、Parent、BOM、Auto Configuration、verification scriptの配置規則をinventoryする
- 正式成果物`koiki-starter-security`と非配布Security verification fixtureの責務を分離する
- root reactor、release unit、Consumerへの影響を提示してから新規moduleを追加する
- 空の将来module、`-api` / `-impl`、Reference applicationを生成しない

### A1-2 — Boot-managed dependency baseline

- `spring-boot-starter-security`を必要最小の正式dependency候補として追加する
- test側はBoot-managed `spring-boot-starter-test`と`spring-boot-starter-security-test`を使用する
- 個別version、独立BOM、Authorization Server、SAML、Redis、WebFlux、独自JWT libraryを追加しない
- focused `dependency:tree`でversion authorityと想定外transitive dependencyがないことをEvidence化する

### A1-3 — Minimal production security foundation

- Spring Security標準を構成する最小Auto Configurationを実装する
- unmatched requestをdefault denyとする
- CSRFとSecurity Headerを既定有効とし、無効化は明示設定に限定する
- Customer route、Role / Permission、login UI、identity persistenceを含めない
- Spring Security内部classをKOIKI Public APIとして露出しない

### A1-4 — T0 / T1 verification Harness

- Security有効／無効、必要property欠落、空白、matcher重複、Customer overrideのApplicationContext matrixを作る
- anonymous access、401 / 403、CSRF、Header、unmatched deny、credential non-fallbackを外部観測する
- test専用user、route、key、failure switchはTooling test sourceへ隔離する
- response、log、test report、artifactにpassword、token、secret、private key、PIIがないことを確認する
- fixture utilityを`koiki-testing`またはFramework Public APIへ先行昇格しない

### A1-5 — Contract and evidence review

- property名、Auto Configuration条件、FilterChain matcher候補をfixture結果からreviewする
- Public API inventoryを作成し、公開Java型0候補を第一案として成立性を確認する
- Security profile / artifact ADRとMFA非有効化記録の更新要否を判断する
- P2-A1 Evidenceを`docs/architecture/validation/`へ記録する
- 実装が設計前提を否定した場合は、実装を正当化せず計画・ADRの再reviewを行う

### A1-6 — Verification and commit point

- focused module + fixture `verify`
- root `verify`
- Public API compatibility、Null Safety、ArchUnitの既存検査
- dependency treeとsecret non-exposure確認
- P2-A1以外のproduction scopeが混入していないことを差分reviewする
- P2-A1単位でcommitし、Milestone Aの同一branch / 1 PR上限を維持する

## 6. Decisions intentionally left for P2-A1 evidence

次はGate P2-2で先行確定していない。名前や型を推測で固定しない。

- Auto Configuration class、packageおよび条件の具体名
- configuration propertyの具体名と最小公開範囲
- applicationが明示するroute / matcher contract
- Customer `SecurityFilterChain` overrideとの合成方法
- 401 / 403 / redirectのprofile別具体表現
- P2-A1で必要な公開Java型の有無
- 非配布fixtureの最終directory / module構成

## 7. Explicit P2-A1 exclusions

- email + password loginの完成（P2-A2）
- User / Role / Permission永続化（P2-B2）
- OIDC Login、Bearer JWT、Cognito-compatible fixture（P2-A3）
- Spring Session JDBC、2 instance、logout / cleanup（P2-B3）
- Audit persistence / transaction（P2-B1）
- Reference `identity`（P2-B4）
- production Flyway Migration（P2-B2 / C1）
- workflow、required check、remote Environment、secretの変更
- Authorization Server、token発行、Oracle、SAML、Redis、AWS固有production Adapter

## 8. Initial verification commands

実際のfocused `-pl`とSecurity verification script名はA1-1で確定する。開始時は次を使用する。

```powershell
.\mvnw.cmd --batch-mode --no-transfer-progress verify
pwsh -NoProfile -File build-support/api-compatibility/verify-public-api-compatibility.ps1
pwsh -NoProfile -File build-support/null-safety/verify-null-safety.ps1
```

P2-A1のT0 / T1はJava 21、credential不要、Docker不要で成立させる。Docker / PostgreSQL TestcontainersはP2-B1以降で使用する。
Gate Aではpackage済み同一artifactのJava 21 / 25 runtime互換を確認するが、P2-A1開始時にruntime別rebuildを作らない。

## 9. Two-PC operating rules

- 作業開始時に`git status --short --branch`とHEADを記録する
- 作業終了時はfocused test、root verify、Evidence更新、commit、pushまでを一単位にする
- 未コミット差分、ローカルstash、IDE設定、Maven cache、起動済みcontainerをPC間handoffの正本にしない
- test key、password、token、client secretをrepository、shell history、log、artifactへ残さない
- JDK distribution差よりJava major versionとRepository Wrapperを正本とする
- OS / Docker差で失敗した場合は、回避設定を個人環境だけに入れずEvidenceへ環境差を記録する
- branchにupstreamがないため、本handoff commit後にremote branch作成とtracking設定を確認する

## 10. Stop conditions

次の場合は実装を拡張せず作業を停止し、Evidenceと選択肢をOwnerへ提示する。

- Spring標準だけでdefault deny、CSRF、Header、Customer overrideを安全に構成できない
- starterの公開型またはpropertyがA2 / A3 / B系統の未実装契約を先行固定する
- test専用route、identity、key、failure switchをproduction resourceへ置く必要が生じる
- `koiki-testing`へのSecurity依存追加がA1だけでは正当化できない
- Authorization Server、SAML、Redis、WebFlux、OracleまたはAWS固有dependencyがtransitiveに入る
- PC固有cache、secret、Docker状態がないと検証できない
- A2以降のscopeを同じcommitへ含める必要が生じる

## 11. Suggested prompt for the next session

> `docs/development/phase2-p2-a1-start-handoff-20260831.md`を読み、記載された正本を確認してください。
> branch / HEAD / worktree / Java / Maven Wrapperをpreflightし、P2-A1のA1-0〜A1-1から開始してください。
> P2-A2以降を先行せず、正式`koiki-starter-security`と非配布verification fixtureのOwnership、module配置、
> dependency差分、検証入口を提示してから実装へ進んでください。

## 12. Handoff completion action on this PC

本書作成時点で`44f6107`はlocal branchのHEADであり、branchにupstream trackingは設定されていない。
本書をcommitし、remoteへpushしてから別PCへ切り替える。push後はremote上のbranchとcommit IDを確認する。

Suggested commit message:

```text
docs: add P2-A1 cross-PC start handoff
```
