# Phase 2 P2-B2 start handoff

## 1. Purpose and status

本書は、P2-B1で承認・実証したAudit contractをbaselineとして維持し、P2-B2のLocal Identity、Password、
Account Lock、認証試行制御、Password Resetの将来境界とIdentity migrationを安全に開始するためのhandoffである。

- **Handoff date:** 2026年9月3日
- **Architecture Owner:** Shuichi Kataoka
- **Branch:** `feature/phase2-security-local-identity-session-audit`
- **Start commit:** `2942db7`（P2-B1 Audit contract / transaction）
- **Phase status:** `P2-B1 COMPLETE / ACCEPTED — P2-B2 COMPLETE / ARCHITECTURE OWNER APPROVED — P2-B3 READY`
- **Ownership:** Framework（Identity contract / persistence / migration）+ Tooling（非配布T4 PostgreSQL fixture）
- **Primary target:** Local Identity、Password / Lock、Role / Permission、external identity link、login attempt
- **Deferred:** Local Password Reset詳細実装は将来要件成立時の別CP、Spring Session JDBCはP2-B3、Reference `identity`はP2-B4、Audit / Session migration総合はP2-C1

## 2. Required reading order

1. `AGENTS.md`
2. `docs/agent/skills/koiki-project-overview/SKILL.md`
3. 本書
4. `KOIKI-JavaWeb-FW_Phase2実行計画_v0.1.md`の§2、§3、§4 Milestone B、§6〜§9
5. `../architecture/validation/phase2-p2-b1-t4-verification.md`
6. `../architecture/adr/README.md`のADR-046 / ADR-047
7. `../architecture/validation/phase2-security-semantics-fitting.md`の§4〜§13
8. `../architecture/validation/phase2-security-test-design.md`のT4、N-01 / N-02 / N-07〜N-09、R-01 / R-05
9. `../architecture/validation/phase2-koiki-pyfw-security-fitting.md`の§4〜§7
10. `../architecture/grand-design/KOIKI-JavaWeb-FW_グランドデザイン_v0.2.md`の§14〜§16、DoD 2-1 / 2-2 / 2-6

## 3. Accepted baseline

- `koiki-starter-security`はdefault deny、local Form Login / HTTP Session seam、Method Security、OIDC Login、Bearer JWTを提供する。
- `koiki-starter-audit`はBusiness `MANDATORY`、Security `REQUIRES_NEW`を実PostgreSQLで実証済みである。
- Audit Public APIは6型、Security Public APIは0型である。
- T0〜T4は31 tests、root Reactorは12 / 12、Architecture Contract 4 tests、ArchUnit Rules 66 testsが成功した。
- Framework user IDとlogin emailは分離し、ASCII emailはtrim + `Locale.ROOT` lowercaseでcanonicalizeする。
- unknown external identityはdefault denyとし、issuer + subjectの明示linkだけを許可する。email auto-link / JIT作成は既定にしない。
- raw password、raw reset token、raw unknown email、external claim全文をDB、log、Audit、response、reportへ残さない。

## 4. P2-B2 scope and exit criteria

P2-B2は次をFramework contractと実PostgreSQLの状態遷移として実証する。

1. immutable Framework user IDとcanonical emailを分離し、canonical emailを一意にする。
2. User、Role、Permission、credential、external identity linkの所有権をFramework tableへ限定する。
3. Spring Security `PasswordEncoder`を使用し、raw passwordと独自hash実装を保存・公開しない。
4. unknown、bad password、disabled、local-lockedを同じ外部認証失敗へ一般化する。
5. login成功・失敗とlockをSecurity auditへ記録し、同時失敗でも閾値を迂回できないようにする。
6. optimistic lockまたは同等のDB競合制御を実PostgreSQLで確認する。
7. 初期SSO適用ではlocal Password Resetを実装せず、IdPのcredential recoveryとKOIKIのOwnershipを混在させない。
8. Role / Permission更新、管理者によるaccount / password操作、external link変更をBusiness auditと同一transactionにする。
9. Identity production migrationについてclean database、重複拒否、FK / unique / index / versionを実測する。
10. Public API、configuration property、error code、table inventoryを型・項目単位でreviewする。

DoD 2-6はP2-B2の中心である。認証失敗とlockのSecurity auditは外側transactionから独立させる。DoD 2-1 / 2-2は、
P2-A2のtest userを実永続Identityへ置換して再確認する。Session全失効と2 instanceの継続性はP2-B3で累積検証する。

## 5. Required contract review before production code

次の判断はB2-1の型・table単位reviewでArchitecture Ownerが承認した。B2-2以降は承認内容をbaselineとし、Evidenceが
否定した場合だけ該当decisionの再reviewへ戻る。

| ID | Decision | Primary question |
|---|---|---|
| B2-C1 | Artifact placement | data dependencyをOIDC / Bearer専用利用者へ強制しないため、Security Starter拡張と単一Identity Starterのどちらにするか |
| B2-C2 | Public Identity API | immutable user ID、principal、検索、管理操作、Session失効のうち、P2-B2で実利用される最小型は何か |
| B2-C3 | User / authority model | User status、local credential lock、account disable、Role / Permissionをどの境界で分けるか |
| B2-C4 | Password contract | `PasswordEncoder`、hash upgrade、password policy、raw passwordの寿命とPublic API露出をどう制限するか |
| B2-C5 | Login attempt key | known account、unknown identifier、送信元をraw email / raw IPなしでどう集計するか |
| B2-C6 | Lock semantics | threshold、window、lock duration、自動解除、管理解除、同時更新とaudit failureをどう扱うか |
| B2-C7 | Reset semantics | 初期SSO前提でlocal resetを実装せず、将来実装時の安全条件とactivation criteriaをどこまで保持するか |
| B2-C8 | External link | issuer + subject一意性、link / unlink transaction、email auto-link禁止をどのPortで表現するか |
| B2-C9 | Migration boundary | P2-B2のIdentity DDLとP2-C1のAudit / Session / upgrade総合をどう分離するか |
| B2-C10 | Session handoff | disable / password変更 / Permission変更後の全Session失効をP2-B3へどう接続し、B2で成功を過大claimしないか |

### 5.1 Approved direction

- local Identityを任意機能として保つため、単一`koiki-starter-identity`候補を第一案とする。Security StarterへJPA / Auditを
  直接追加してOIDC / Bearer専用構成までDB必須にしない。空の`-api` / `-impl`分割は行わない。
- Identity StarterはSecurity、Data JPA、Auditの承認済み契約を利用し、Spring / JPA内部型をPublic APIへ露出しない。
- known userの試行はimmutable user IDで集計する。unknown identifier / source keyはraw email / raw IP保存を避ける方式を
  B2-C5で決め、privacy、key管理、retentionが決まらないままDDLを固定しない。
- Password hashはSpring Securityのdelegating encoderを第一案とし、独自暗号・独自hash formatを作らない。
- 初期適用projectはSSO認証を前提とするため、local reset token、mail delivery、reset table / APIはP2-B2で作らない。
  将来local self-service reset要件が成立したCPで安全条件を再reviewする。
- Identity migrationはP2-B2で所有する。Audit table、Spring Session table、全migrationのsupported upgrade / clean総合はP2-C1へ残す。

2026年9月3日、Architecture Ownerは`phase2-p2-b2-contract-review.md`を一通りreviewし、B2-C1〜C10を
推奨案どおり承認した。本sectionはB2-2以降の実装baselineである。

## 6. Verification topology

既存`build-support/security-foundation-verification`をT4として拡張し、PostgreSQL Testcontainersで次を観測する。

1. canonical emailの一意性、immutable ID、status / credential / Role / PermissionのFK所有権。
2. 永続UserによるForm Login成功、bad password / unknown / disabled / local-lockのgeneric failure。
3. 並行login失敗が閾値を迂回せず、lockとSecurity auditが整合する。
4. Audit failure時にlogin成功、管理解除がfail closedになる。将来reset token発行にも同じO-4を適用する。
5. disable等の防御操作はAudit失敗後も完了し、alert可能な失敗を残す。
6. reset専用Public API、property、table、endpointまたはdelivery adapterがP2-B2 production artifactへ混入しない。
7. issuer + subject linkの一意性、unknown link deny、email-only auto-link拒否。
8. Business audit失敗時にRole / Permission / link等の管理変更もrollbackする。
9. row、Application log、Audit、response、Surefire report、JARをsecret / PII patternで走査する。
10. T0〜T4回帰、root verify、Null Safety、Public API inventory、migration clean検証、container cleanupを成功させる。

test user、raw credential、failure switch、clock、source key、並行実行helperはTooling-ownedとし、正式artifact、
`koiki-testing`、ReferenceまたはFramework Public APIへ昇格させない。

## 7. Working sequence and commit points

### B2-0 — Start preflight

- branch、HEAD、clean worktree、Java 21、Maven Wrapperを確認する。
- P2-B1 aggregateとroot verifyをbaselineとして維持する。

### B2-1 — Contract / table review

- B2-C1〜C10を比較し、artifact、Public API、properties、tables、failure semanticsを承認した。
- 数値既定、unknown attempt key、IP privacyおよびreset defer境界を`phase2-p2-b2-contract-review.md`で確定した。
- **Status:** `COMPLETE / ARCHITECTURE OWNER APPROVED`（2026年9月3日）
- **Commit point:** reviewとOwner承認だけを独立commitにできる。

### B2-2 — Identity core / migration

- 承認済み最小artifact、User / Role / Permission / credential / external link modelとIdentity migrationを追加する。
- canonical unique、FK、version、raw secret非保存を実PostgreSQLで確認する。
- **Status:** `COMPLETE / ARCHITECTURE OWNER APPROVED`（2026年9月7日）
- **Commit point:** Identity core / migration。

### B2-3 — Authentication / attempt / lock

- Spring AuthenticationProvider / UserDetailsService seamへ接続する。
- generic failure、並行閾値、lock、自動解除、Security audit failureを検証する。
- local password認証は明示opt-inとし、SOURCE保護は`APPLICATION`既定、承認済み公開境界へ責務移動する
  `EXTERNAL`を選択可能とする。`EXTERNAL`でもACCOUNT保護は維持する。
- **Status:** `COMPLETE / ARCHITECTURE OWNER APPROVED`（2026年9月7日）
- **Commit point:** authentication protection。

### B2-4 — Identity administration

- Role / Permission / link / account / password管理とAudit semanticsを検証する。reset専用成果物の非混入も確認する。
- external issuerは検証済み値との完全一致を維持して独自正規化せず、unknown / 表記不一致をdenyする。管理unlinkは
  最後の認証手段でも許可し、Business audit rollbackと対象userのSession全失効を確認する。self-service unlinkは含めない。
- Session全失効の未実装部分をP2-B3 handoffへ明示する。
- **Status:** `COMPLETE / ARCHITECTURE OWNER APPROVED`（2026年9月7日）
- **Commit point:** Identity administration。

### B2-5 — Regression / evidence

- T0〜T4 aggregate、root、Null Safety、API / table inventory、migration、secret / PII、cleanupを確認する。
- P2-B2 Evidenceを記録し、Owner acceptance後にP2-B3へ進む。
- **Status:** `COMPLETE / ARCHITECTURE OWNER APPROVED`（2026年9月7日、Shuichi Kataoka）。
- `../architecture/validation/phase2-p2-b2-b2-5-verification.md`にT0〜T4 56 / 56、root 13 / 13、Architecture Contract 4 / 4、
  ArchUnit 66 / 66、NullAway正負、secret / PII scanおよびcontainer / temporary resource cleanupを記録した。
- Owner reviewで指摘された失敗時report境界を補強し、fixture HMACの動的property供給、表示前Maven出力scan、
  失敗後のJAR / log / report再scanを追加した。Docker接続失敗の負例と、その後の56 / 56正常回帰を確認済みである。
- **Commit point:** P2-B2 closeout。必要ならB2-2〜B2-5を1つに統合できるが、未承認contractと実装を同時commitしない。

## 8. Downstream task map

| Next work | Main outcome | Must remain out of earlier task |
|---|---|---|
| **P2-B2** | Identity / Password / Lock / attempts、Reset defer境界、Identity migration、実DB T4 | reset token / delivery、Session JDBC、Reference UI |
| **P2-B3** | Spring Session JDBC、2 process、logout / 全Session失効、cleanup / single execution、T5 / T6 | Reference業務、cloud adapter |
| **P2-B4** | Reference `identity` Tier 1、Framework Public API経由の管理操作、Method Security / Audit | Framework Entity / Repository / migration所有 |
| **Gate B** | P2-B1〜B4 aggregate、packaged journey、Public API互換、Milestone B CI候補 | required化は3回連続成功とcleanup確認後にOwner review |
| **P2-C1** | Audit / Sessionを含むFramework migration、第三者table一覧、clean / supported upgrade総合 | Oracle、Customer migration混在 |
| **P2-C2** | package / independent Consumer / Public API compatibility / OpenRewrite試作 | Reference codeのFramework昇格 |
| **P2-C3** | Developer Journey、DoD 2-1〜2-10 trace、Skill / ADR / deferred closeout | 新機能追加 |
| **Gate C** | final PR、required checks、Owner approval、merge後main CI | Phase 3先行 |

## 9. Explicit exclusions

- Spring Session JDBC、`koiki_session*`、2 process、cleanup production実装（P2-B3）
- local Password Reset Public API、token / mail delivery、`koiki_password_reset`、reset endpoint（将来要件成立時の別CP）
- Reference `identity` Controller / Use Case / view（P2-B4）
- Audit / Session production migrationと全体upgrade総合（P2-C1）
- self registration、Customer社員 / 組織属性、業務resource ownership、Customer claim mapping
- Authorization Server、access / refresh token発行、SAML、Redis、WebFlux、Oracle
- AWS Cognito / ALB固有Adapter、CloudWatch SDK / appender、外部通知
- raw IP / raw unknown emailを、retention / privacy判断なしに保存すること
- Python class、endpoint、migration、progressive delayの直接移植

## 10. Stop conditions

- Identity artifact、Public API、table、property、数値既定またはfailure semanticsがOwner reviewされていない。
- unknown identifier / source attempt keyの方式がraw email / raw IP、秘密鍵管理またはretentionを曖昧にする。
- password照合、hash処理をSpring標準より独自実装する必要がある。
- concurrent failureで閾値迂回またはlost updateが実PostgreSQL上で残る。
- generic responseでもCookie、body、redirect、timing上の意図的な分岐、log、metricからaccount状態が識別できる。
- Audit failure時にO-4と異なる成功 / rollbackになる。
- P2-B2完了のためにSpring Session production実装やReference UIを混在させる必要がある。
- fixture credential、test identity、failure switchまたはdeferしたreset成果物が正式artifactへ入る。

## 11. Immediate next action

2026年9月7日、B2-4の`IdentityAdministration` production実装とPostgreSQL fixtureを追加した。user / Role / Permission、
local credential、external link管理、optimistic version、Business / Security Audit failure matrixおよび同期Session invalidatorの
呼出し・失敗時rollbackを確認した。Owner review後、必須4依存の欠落／充足によるAuto Configuration境界と、password、
User Role、Role Permission、external unlinkのSession失効失敗時rollbackを直接検証した。T0〜T4 aggregate 56 / 56、
root Reactor 13 / 13、Architecture Contract 4 / 4、
ArchUnit 66 / 66は成功済みである。Spring Session row削除と旧Cookie拒否はP2-B3の実証対象であり、B2-4では成功claimしない。
Architecture Ownerは補強結果を確認してB2-4を最終承認した。2026年9月7日、B2-5 regression / evidenceを実行し、
T0〜T4 56 / 56、root回帰、NullAway正負、inventory、secret / PIIおよびcleanupを確認した。
Architecture OwnerはB2-5の5項目とTooling補強結果を最終承認し、P2-B2をcloseした。次はP2-B3の
Spring Session JDBC contract / implementation / evidence整理へ進む。
