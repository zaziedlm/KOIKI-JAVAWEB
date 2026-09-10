# Phase 2 P2-B4 Reference identity start handoff

## 1. Handoff status

- **Handoff date:** 2026年9月8日
- **Architecture Owner:** Shuichi Kataoka
- **Branch:** `feature/phase2-security-local-identity-session-audit`
- **Approved baseline commit:** `9197d9b`（P2-B3 Session Foundation closeout / ADR-048）
- **Phase status:** `P2-B3 COMPLETE / ARCHITECTURE OWNER APPROVED — P2-B4 READY`
- **Ownership:** Reference（`identity` Controller / Application Use Case / view）
- **Primary target:** DoD 2-10、Framework Identity contractを利用する管理journey、Method Security、Audit
- **Deferred:** Gate B aggregate / CI required化はP2-B4完了後、全Framework migration upgradeはP2-C1、HTMX / REST / SPAは後続Phase

本handoffは、別PC・新規AI対話でP2-B4を開始するための作業正本である。handoff commitをremoteへpushした後は、
新しいPCで同branchをcheckoutし、`git status --short`が空であること、本書を含むremote HEADであることを確認してから作業する。

## 2. Required reading order

1. `AGENTS.md`
2. `docs/agent/skills/koiki-project-overview/SKILL.md`
3. `docs/agent/skills/koiki-business-feature-work/SKILL.md`
4. 本書
5. `KOIKI-JavaWeb-FW_Phase2実行計画_v0.1.md`の§3、Milestone B、§7〜§9
6. `../architecture/validation/phase2-p2-b2-contract-review.md`のB2-C2、B2-C8、B2-C10
7. `../architecture/validation/phase2-p2-b2-b2-4-verification.md`
8. `../architecture/validation/phase2-p2-b2-b2-5-verification.md`
9. `../architecture/validation/phase2-p2-b3-contract-review.md`
10. `../architecture/validation/phase2-p2-b3-b3-6-closeout.md`
11. `../architecture/adr/README.md`のADR-046〜ADR-048
12. `../architecture/grand-design/KOIKI-JavaWeb-FW_グランドデザイン_v0.2.md`の§3、§14、§15、§26、§27.5

## 3. Transfer baseline

handoff作成前の状態は次のとおりである。

```text
Local HEAD: 9197d9b docs(session): close out P2-B3 session foundation
Remote branch: origin/feature/phase2-security-local-identity-session-audit
Remote baseline before transfer: 9b5bee2 docs(session): add B3-4 two-process handoff
Local commits ahead before this handoff: 6
Worktree: clean
```

push後はremote branchが本handoff commitを含むことを`git ls-remote`または別PCでの`git fetch`後に確認する。
別PCではlocalに同名branchがある場合も、未push変更の有無を確認せずresetしない。

## 4. Accepted baseline — do not redesign

1. FrameworkのUser / Role / Permission / credential / external linkは`koiki-starter-identity`が所有し、Referenceは
   Framework table、JPA Entity、Repositoryまたはinternal packageを直接参照しない。
2. Reference `identity`はTier 1 SIMPLEで開始し、Application Use Caseが管理操作の調整を所有する。未使用の
   `domain.model`、`domain.service`、RepositoryまたはGatewayを先行生成しない。
3. Inbound Controllerは入力受付、形式検証、DTO変換、応答整形だけを担い、Framework public contractを直接操作する処理は
   Reference Application Use Caseへ委譲する。
4. backendのMethod Securityを認可境界とする。画面の非表示、URLの秘匿、JavaScript制御だけで管理操作を保護しない。
5. 認証済みactorは`FrameworkPrincipal`のimmutable `FrameworkUserId`とPermissionを使用し、emailや表示名を
   actor識別子または認可keyにしない。
6. Identity mutationは既存`IdentityAdministration`を使用し、FrameworkのAudit、Session失効、rollbackおよびsafe failureを
   Reference側で複製または回避しない。
7. 読取は既存`IdentityQuery`等のFramework public contractを通す。画面都合でFramework schemaへSQLを発行しない。
8. Reference-owned migrationまたはtableをP2-B4へ追加しない。Identity 8 tableはFramework Flyway所有を維持する。
9. Security acceptance fixtureのroute、test user、failure switch、credential、migrationを正式Referenceへcopyしない。
10. P2-B3のSession JDBC、2 process継続、全Session失効、logout、cleanup / single executionおよびADR-048をbaselineとして扱う。
11. local password reset、mail delivery、provisioning、Customer属性、IdP固有claim mappingを追加しない。
12. Gate B workflow、required check、remote environmentまたはsecretをP2-B4実装sliceの途中で変更しない。

## 5. Existing contracts and known B4 review boundary

### 5.1 Framework Public API baseline

`koiki-starter-identity`の承認済みPublic Java APIは10型である。

```text
AuthenticationSource
FrameworkPrincipal
FrameworkUserId
IdentityAdministration
IdentityFailure
IdentityOperationException
IdentityQuery
IdentityUser
UserSessionInvalidator
UserStatus
```

`IdentityAdministration`はuser、email、enable / disable、local password、unlock、Role、Permission、external linkの
管理操作を提供する。`IdentityQuery`は現在`findById(FrameworkUserId)`だけを提供する。

管理画面に一覧、検索、Role / Permission候補またはcurrent principal解決が実際に必要となる場合は、ReferenceからFramework
Entity / Repositoryへ迂回せず、最初の利用箇所と同じsliceでadditive Public APIを型・method単位にreviewする。
pagination、汎用filter、DTO mapper、REST専用modelを将来用途だけで追加しない。

### 5.2 Responsibility placement

```text
Reference identity Controller
  -> Reference identity Application Use Case
    -> Framework IdentityQuery / IdentityAdministration
      -> Framework-owned persistence / Audit / Session invalidation
```

- Controller: request、binding validation、response / MVC model整形
- Application Use Case: Method Security、処理順、Framework failureからReference結果への変換
- Framework contract: Identityの永続化、楽観lock、Audit、Session失効、secret処理
- View model: transaction外で不変なReference DTO。`IdentityUser`をそのままMVC Modelへ露出するかも事前reviewする

## 6. Required B4 contract review before implementation

P2-B4の最初はproduction codeではなく、次の比較・推奨案をArchitecture Evidenceとしてまとめる。

| ID | Decision | Primary question |
|---|---|---|
| B4-C1 | Reference artifact / module placement | `koiki-reference-app`をどのMaven単位で開始し、正式Framework release unitとどう分離するか |
| B4-C2 | Reference `identity` module structure | Tier 1のController / Application / DTO / Configurationをどのpackageへ置くか |
| B4-C3 | Initial management journey | DoD 2-10を証明する最小操作を何にし、全管理機能を一度に画面化しない境界をどう置くか |
| B4-C4 | Read contract | `findById`だけで最小journeyが成立するか。一覧等が必要ならどのFramework APIをadditive reviewするか |
| B4-C5 | Current actor / Method Security | `FrameworkPrincipal`とPermissionをController外のbackend境界へどう接続し、direct request拒否をどう検証するか |
| B4-C6 | Audit / Session semantics | Referenceが既存`IdentityAdministration`のAudit / invalidationを重複させず、成功・rollbackをどう外部観測するか |
| B4-C7 | View / error boundary | Entity、encoded credential、issuer / subject、internal errorを露出せず、競合 / not found / dependency failureをどう表現するか |
| B4-C8 | Packaging / Gate B handoff | package済みReference journey、Public API inventory、fixture非混入、Gate B aggregateをどう分けるか |

B4-C1〜C8をArchitecture Ownerが承認するまで、新規Reference artifact、Public API、production route / view、migrationまたはCIを追加しない。

## 7. Proposed work breakdown

### B4-1 — Contract and module review

- B4-C1〜C8の比較・推奨案を作成する。
- Maven artifact、package、route、Permission、Use Case、DTO、Public API差分をinventory化する。
- **Proposed evidence:** `../architecture/validation/phase2-p2-b4-contract-review.md`
- **Commit point:** contract / implementation handoff。Owner承認前にproduction codeを含めない。

### B4-2 — Reference application skeleton and one read journey

- 承認済み配置に最小の実行可能Reference Applicationと`identity` Tier 1 moduleを追加する。
- Framework `IdentityQuery`経由の単一user参照と、未認証・権限不足・not foundを確認する。
- Reference独自table、migration、Repositoryを追加しない。

### B4-3 — Management mutation and Method Security

- 承認した最小管理操作をReference Use Caseから`IdentityAdministration`へ接続する。
- UI制御を回避したdirect requestでもPermissionを強制する。
- 楽観lock、Audit failure、Session invalidation failureの既存Framework semanticsを維持する。

### B4-4 — Packaged journey and evidence

- package済みReference Applicationと実PostgreSQLでDoD 2-10を外部観測する。
- HTTP、DB row、Audit row、Session継続 / 失効、sanitized response / logを組み合わせる。
- Reference / Framework / fixture artifact、Public API、migration、dependencyのOwnershipを検査する。

### B4-5 — P2-B4 closeout and Gate B handoff

- B1〜B4の回帰、root、Null Safety、inventory、secret / PII、resource cleanupを確認する。
- P2-B4をOwner reviewへ提示し、承認後にGate B aggregate / packaged journey / CI候補reviewへ進む。

各sliceの分け方はB4-C1〜C8 reviewで変更できる。上記を未承認の実装契約として扱わない。

## 8. P2-B4 exit criteria

- 実行可能なReference Applicationの`identity` Tier 1 moduleがFramework public contractだけを使用する。
- Framework所有tableをReference-owned Controller / Use Caseから安全に操作でき、DoD 2-10を満たす。
- 管理操作がbackend Method Securityで保護され、未認証・Permission不足・direct requestを拒否する。
- 認証済みactorのimmutable user IDが既存Auditへ記録され、PIIやcredentialを複製しない。
- Identity mutationのAudit rollback / Security Audit semantics / Session失効がB2 / B3の承認済み挙動と一致する。
- ControllerがRepositoryを直接呼ばず、Framework internal、JPA Entity、Spring Data型を参照しない。
- MVC Model / response / log / reportへencoded password、Cookie、Authorization header、issuer / subject等を不用意に露出しない。
- Reference fixture、test route、failure switch、migrationがFramework artifactまたは`koiki-testing`へ混入しない。
- root Reactor、Architecture Contract、ArchUnit、NullAwayおよびP2-B1〜B3回帰が成功する。
- 結果をArchitecture Evidenceへ記録し、Architecture OwnerがP2-B4を承認する。

## 9. Explicitly deferred

- Gate BのP2-B1〜B4 aggregate、remote CI接続、required化
- P2-C1のAudit / Identity / Session migration順序、clean / supported upgrade
- Phase 3のReference `master` / `expense`、HTMX、REST API、業務監査journey
- Phase 4のSPA、通知、非同期event、File / Batch / 汎用Worker基盤
- Customer社員番号、所属、provisioning、IdP固有claim mapping、resource ownership
- local password self-service reset、token、mail / SMS delivery
- Redis、Oracle、SAML、AWS固有Adapter

## 10. Start commands on the new PC

```powershell
git clone https://github.com/zaziedlm/KOIKI-JAVAWEB.git
cd KOIKI-JAVAWEB
git fetch origin
git switch --track origin/feature/phase2-security-local-identity-session-audit
git status --short
git log -5 --oneline --decorate
```

すでにclone済みの場合は次を使用する。

```powershell
git fetch origin
git switch feature/phase2-security-local-identity-session-audit
git pull --ff-only
git status --short
git log -5 --oneline --decorate
```

`git status --short`が空でない場合は、既存変更の所有者と内容を確認できるまで編集、reset、cleanを行わない。
Docker Engine、Java、Maven Wrapperの確認は次で行う。

```powershell
docker version
java -version
.\mvnw.cmd --version
```

P2-B3の重い3連続closeoutを作業開始時に再実行する必要はない。環境差またはSession回帰を疑う場合は、目的に応じて
`verify-p2-b3-session-core.ps1`、`verify-p2-b3-session-two-process.ps1`、`verify-p2-b3-session-cleanup-process.ps1`を個別実行する。

## 11. Immediate next action

新しいPCではB4-1から開始する。既存Reference artifactの有無、Root Reactor / BOM / Architecture Contractの配布境界、
Identity Public API 10型と実際の管理journeyを照合し、B4-C1〜C8の比較・推奨案を
`phase2-p2-b4-contract-review.md`へ作成する。Architecture Owner承認前に新規Reference artifactやPublic APIを実装しない。
