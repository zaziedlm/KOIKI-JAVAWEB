# Phase 2 P2-A2 start handoff

## 1. Purpose and status

本書は、2026年9月1日に完了したP2-A1 Security Foundationを、翌日の新規AI対話sessionへ引き継ぎ、
同じ作業branchでP2-A2を安全に開始するためのhandoffである。

- **Handoff date:** 2026年9月1日
- **Next work date:** 2026年9月2日
- **Architecture Owner:** Shuichi Kataoka
- **Branch:** `feature/phase2-security-foundation`
- **P2-A1 implementation HEAD:** `f1c9fe9` (`feat(security): establish P2-A1 security foundation`)
- **Phase status:** `P2-A1 COMPLETE — P2-A2 READY`
- **Worktree before this handoff file:** clean
- **Upstream:** `origin/feature/phase2-security-foundation`

本書はP2-A1実装commitの次に単独commitし、同じremote branchへpushする。翌日はremote上の本handoff commitを
取得して開始する。

## 2. Required reading order

1. `AGENTS.md`
2. `docs/agent/skills/koiki-project-overview/SKILL.md`
3. 本書
4. `KOIKI-JavaWeb-FW_Phase2実行計画_v0.1.md`
5. `../architecture/validation/phase2-p2-a1-closeout.md`
6. `../architecture/validation/phase2-p2-a1-contract-review.md`
7. `../architecture/validation/phase2-security-test-design.md`のT2、Local login、N-01／N-05／N-06
8. `../architecture/validation/phase2-security-semantics-fitting.md`のAuthentication／Authorization semantics
9. `../architecture/adr/README.md`のADR-046
10. `../architecture/grand-design/KOIKI-JavaWeb-FW_グランドデザイン_v0.2.md`のSecurityとPhase 2 DoD

`phase2-p2-a1-start-handoff-20260831.md`はP2-A1開始時のhistorical baselineである。P2-A1の実測結果は
closeout、contract review、ADR-046および本書を優先する。

## 3. P2-A1 completion baseline

P2-A1では次を正式化した。

- 配布成果物`koiki-starter-security`と非配布`build-support/security-foundation-verification`を分離した
- Boot-managed Spring Security dependencyとAuto Configurationを追加した
- unmatched requestのdefault deny、CSRF、Security Headerの安全な既定を実装した
- application-owned `SecurityFilterChain`による明示overrideとback-offを実証した
- T0 ApplicationContext／T1 request boundary Harnessを構築した
- P2-A1時点の公開Java型、configuration property、KOIKI error codeはいずれも0件とした
- ADR-046を`ACCEPTED`とし、starter contractと所有境界を記録した

P2-A1の完了Evidenceは次のとおりである。

- focused Parent + Security Starter verify: 2/2 module success
- Security aggregate: isolated root 11/11、fixture 10 tests、dependency／JAR／API／secret scan success
- root verify: 11/11 module、Architecture Contract 4 tests + ArchUnit 66 tests success
- NullAway positive／negative／restore success
- Public API fixture Gate 3 success
- remote Public API Gate 2 success。Architecture Contract／ArchUnit Rulesのhash、inventory、japicmpが一致

詳細と正確なhashは`../architecture/validation/phase2-p2-a1-closeout.md`を正本とする。

## 4. P2-A2 position and exit criteria

```text
Phase / status: Phase 2 / Milestone A / P2-A2 READY
Ownership: Framework（Security構成）+ Tooling（非配布T2 fixture）
Production target: koiki-starter-security
Verification target: existing security-foundation-verification Harnessを拡張
Primary layer: T2 local login / Role / Permission / Method Security
Exit criteria: DoD 2-1、2-2、2-9とUI回避direct request拒否
```

P2-A2は、email + passwordによるlocal Form LoginとHTTP Session、URL Security、Method Security、Roleから展開した
Permission authorityを、Spring Security標準を優先して実証する。P2-A1のT0／T1を維持し、T2を同じ非配布Harnessへ追加する。

DoDの観測結果は次である。

- 2-1: 未認証requestはURL経由とMethod Securityの双方で拒否される
- 2-2: 認証済みでもPermission不足なら403となり、Controller／UIを迂回したUse Case direct invocationも拒否される
- 2-9: P2-A1で成立したCSRF／Security Headerの既定をlogin追加後も維持する
- 正しいemail／passwordと必要Permissionの組み合わせだけが保護処理へ到達する

## 5. P2-A2 working sequence

### A2-0 — Start preflight

- remoteから同branchを取得し、HEADが本handoff commit、worktreeがcleanであることを確認する
- Java 21とRepository Maven Wrapperを確認する
- root `verify`とP2-A1 Security aggregateを変更前baselineとして実行する
- P2-A1のPublic API 0件、property 0件、error code 0件を無断で変更しない

### A2-1 — T2 contract inventory

- `DaoAuthenticationProvider`、`UserDetailsService`、`PasswordEncoder`、Form Login、Session fixation protection、
  `@EnableMethodSecurity`を第一候補として成立性を確認する
- email canonicalizationはASCII、trim、`Locale.ROOT` lowercaseという承認済みO-1を使用する
- RoleはPermissionの集合、実際の認可判定はPermission authorityというsemanticsを維持する
- production契約とfixture fakeの境界、必要なPublic API候補を実装前に提示する

### A2-2 — Local login and session

- `POST /login`はSpring Security filterに処理させ、独自password Controllerやtoken発行APIを作らない
- T2 fixture-owned fake identityで、成功、unknown email、disabled、locked、bad passwordを検証する
- 失敗は外部から区別できないgeneric failureとし、password、raw unknown email、内部reasonを露出しない
- login成功時のSession確立とsession fixation protectionを外部観測する

### A2-3 — URL / Method authorization

- anonymous 401またはbrowser profileの明示redirect、認証済みPermission不足403、許可時だけ到達を確認する
- URL SecurityとMethod Securityを併用し、必要Permissionを持つprincipalだけを許可する
- Controller／UIを迂回したdirect method invocationでも保護処理が実行されないことを確認する
- unknown／malformed／case違いのauthorityからPermissionを付与しない

### A2-4 — Regression, evidence, and closeout

- T0／T1／T2 aggregate、focused verify、root verify、Null Safety、Public API compatibilityを実行する
- response、log、test report、JARをsecret／password／PII観点で走査する
- P2-A2 EvidenceとPublic API inventory差分を`docs/architecture/validation/`へ記録する
- P2-A2だけをcommitし、P2-A3以降を混在させない

## 6. Explicit P2-A2 exclusions

- User／Role／Permission／credentialのproduction永続化、migration、lock／reset／attempt制御（P2-B2）
- audit persistenceとtransaction rollback（P2-B1）
- Spring Session JDBC、複数instance、権限変更後の全Session失効、cleanup（P2-B3）
- OIDC Login、Bearer JWT、Cognito-compatible issuer、SPA／BFF／Edge（P2-A3）
- Reference `identity`、Customer業務route／resource ownership policy（P2-B4以降）
- Framework-owned `/me` endpoint、独自login REST API、Access／Refresh Token発行
- Authorization Server、SAML、Redis、WebFlux、Oracle、AWS固有production Adapter
- 新しい共通test module。共有性が実証されるまでは既存の非配布Harnessを拡張する

## 7. Initial commands for the next session

別PCの場合は、remote branchを取得してから開始する。

```powershell
git fetch origin
git switch feature/phase2-security-foundation
git pull --ff-only
git status --short --branch
git log -3 --oneline --decorate
.\mvnw.cmd --version
.\mvnw.cmd --batch-mode --no-transfer-progress verify
pwsh -NoProfile -File build-support/security-foundation-verification/verify-p2-a1-security-foundation.ps1
```

P2-A2の開始と日常検証にGitHub Packages PATは不要である。P2-A1 remote Public API Gate用に作成したPATはrepository、
設定ファイル、shell historyへ保存せず、不要ならGitHub側で失効する。Gate実行時に再び必要になった場合だけ、最小権限の
credentialをprocess環境へ一時注入する。

## 8. Stop conditions

次の場合は実装範囲を拡張せず、Evidenceと選択肢をOwnerへ提示する。

- Spring標準componentではT2 semanticsを満たせず、独自Authentication Filter／Provider／password処理が必要になる
- P2-A2のためにproduction identity persistence、migration、auditまたはJDBC Sessionが必要になる
- Public API 0件のbaselineを変更する必要があるが、型単位のcontract reviewが未実施である
- fixture user、password、route、failure switchをproduction resourceへ置く必要がある
- generic login failureと内部reason非露出を両立できない
- URL SecurityまたはMethod Securityの片方だけでdirect request／invocation bypassが残る
- P2-A3以降のdependencyまたはcredential transportを同じ差分へ含める必要がある

## 9. Suggested prompt for the next session

> `docs/development/phase2-p2-a2-start-handoff-20260901.md`を読み、記載された正本を確認してください。
> branch／HEAD／worktree／Java 21／Maven Wrapperをpreflightし、root verifyとP2-A1 Security aggregateを実行してください。
> その後P2-A2のA2-1 contract inventoryから開始し、既存の非配布HarnessをT2へ拡張してください。
> P2-B2の永続化やP2-A3のOIDC／JWTを先行せず、Spring標準componentでlocal login、URL／Method Security、
> Role／Permission、direct invocation拒否を実証してください。

## 10. Handoff completion action

本書だけをcommitし、`feature/phase2-security-foundation`を`origin`へpushする。push後にlocal HEADとtracking branchが
同期していることを確認して、本PCでの作業を終了する。

Suggested commit message:

```text
docs: add P2-A2 start handoff
```
