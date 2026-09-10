# Phase 2 P2-B4 B4-3 Role membership mutation verification

## 1. Status and scope

- **Verification date:** 2026年9月9日
- **Work package:** `P2-B4 / B4-3`
- **Status:** `COMPLETE / ARCHITECTURE OWNER APPROVED — B4-4 NOT STARTED`
- **Approved by:** Shuichi Kataoka、2026年9月9日
- **Ownership:** Reference（`identity` MVC / Application Use Case）
- **Baseline:** B4-C1〜C8およびB4-2 Architecture Owner承認済み

本記録は、Reference `identity`のRole membership付与 / 剥奪を、既存Framework Public API
`IdentityAdministration`へ接続したB4-3の実装証拠である。POST / CSRF、URL / Method Security、optimistic version、
safe HTML failure mappingおよびFramework所有のAudit / rollback / Session失効semanticsを検証した。

package済みReference processと実PostgreSQLによる補助的なブラウザ確認も実施した。ただし、Tooling-owned bootstrapから
HTTP、DB、Auditおよび事前確立した複数Sessionを再現可能な一続きのtestとして外部観測する正式なAC-P2-01 / 02 journeyは
B4-4へ残し、本記録では成立をclaimしない。

## 2. Implemented boundary

### 2.1 Reference MVC mutation

| Method / path | Reference responsibility |
|---|---|
| `POST /identity/users/{userId}/roles` | `roleCode`と表示済み`expectedVersion`を受け、Roleを付与する |
| `POST /identity/users/{userId}/roles/{roleCode}/revoke` | 表示済みRoleと`expectedVersion`を受け、Roleを剥奪する |

- detail templateへRole assign formと、表示中Roleごとのrevoke formを追加した。
- form actionはThymeleaf / Spring MVCへ委譲し、Spring SecurityのCSRF hidden inputを自動生成する。
- 正常POSTは同一userのcanonical detail URLへredirectし、再queryで最新Role / Permission / versionを表示する。
- `IdentityRoleChangeForm`はweb inputだけを所有し、missing inputを固定400 responseとして拒否する。
- Role / Permission一覧、Role作成、password、email、disable、external link等は追加していない。

### 2.2 Application and Framework ownership

`IdentityUserManagement`へ次の2 methodだけを追加した。

```text
assignRole(userId, roleCode, expectedVersion)
  -> IdentityAdministration.assignRole(...)

revokeRole(userId, roleCode, expectedVersion)
  -> IdentityAdministration.revokeRole(...)
```

Reference Use CaseはFramework mutationを1回呼ぶだけであり、`@Transactional`、Audit recorder、Session repository、
補償処理またはFramework internal typeを追加していない。optimistic version、Business Audit、target全Session失効、
failure時rollbackは既存`IdentityAdministration`の所有権を維持する。

Identity Public Java type、method、configuration property、tableおよびmigrationは変更していない。

### 2.3 Runtime assembly

package済みprocessの初回起動で、Referenceが`CompromisedPasswordChecker`を構成していないため、Frameworkの
`IdentityAdministration` beanが成立せずstartup時に必須注入エラーとなることを観測した。B2で承認した
「Frameworkはno-op checkerを提供せず、管理機能を使うapplicationが依存を明示する」境界に従い、Reference
`configuration`へSpring Security標準`HaveIBeenPwnedRestApiPasswordChecker`を追加した。

- Reference独自checker、常時許可checker、test fixtureまたは新dependencyは追加していない。
- B4-3のRole操作およびlocal loginではcheckerへの外部照会を行わない。password管理操作は本sliceのUIに存在しない。
- `ReferenceSecurityConfigurationTest`で標準実装の選択を固定し、MVC sliceのlogin testではtest-owned mockへ置換して
  外部接続を発生させない。
- 修正後のpackage済みprocessは同じ実PostgreSQLへ接続して起動した。

### 2.4 Authorization and failure boundary

- `/identity/**`のURL boundaryで`IDENTITY:ADMIN`を要求する。
- `IdentityUserManagement`のclass-level `@PreAuthorize`によりApplication boundaryでも同じauthorityを要求する。
- POSTはSpring Security標準CSRFを必須とする。
- CSRF欠落、Permission不足またはanonymousのrequest / direct invocationでは、Reference Use Caseまたは
  `IdentityAdministration`を呼ぶ前に拒否する。
- Framework `IdentityOperationException`はReference共通HTML handlerで次の固定responseへ変換し、exception message、
  identifier、version、SQL、tableまたはcauseをModelへ渡さない。

| Framework failure | HTTP | Fixed user action |
|---|---:|---|
| `INVALID_INPUT` | 400 | request inputを確認する |
| `NOT_FOUND` | 404 | userまたはRoleが存在しない |
| `CONFLICT` / `CONCURRENT_MODIFICATION` | 409 | userを再読込して再試行する |
| `DEPENDENCY_FAILURE` | 503 | 後で再試行する |

失敗を成功redirectまたはflash messageへ変換せず、PRGは正常系だけに限定した。

## 3. Verification results

### 3.1 Reference tests — 34 / 34

| Test | Count | Evidence |
|---|---:|---|
| `IdentityUserManagementTest` | 3 | DTO readとFramework assign / revokeへの正確な委譲 |
| `IdentityUserManagementMethodSecurityTest` | 6 | admin read / mutation許可、権限不足 / anonymousをFramework呼出前に拒否 |
| `IdentityManagementControllerTest` | 9 | B4-2 GET、assign / revoke PRG、required input、stale version 409 |
| `IdentityManagementExceptionHandlerTest` | 5 | Framework failure全categoryのstatus / fixed message |
| `IdentityUrlSecurityTest` | 8 | URL認可、CSRF、direct POST拒否、CSRF / versionを含むHTML form |
| `ReferenceSecurityConfigurationTest` | 1 | Spring標準compromised-password checkerのReference選択 |
| `ReferenceArchitectureTest` | 2 | Tier 1 business module rule、Framework ownership rule |

```text
Tests: 34
Failures: 0
Errors: 0
Skipped: 0
```

### 3.2 Root Reactor

```text
Root clean verify: Reactor 15 / 15 SUCCESS
Root Reactor Surefire tests: 104 / 104 SUCCESS
Architecture Contract: 4 / 4 SUCCESS
ArchUnit rule library: 66 / 66 SUCCESS
Reference tests: 34 / 34 SUCCESS
Error Prone / NullAway compilation: SUCCESS
```

### 3.3 Framework mutation regression and release boundary

既存`verify-p2-b2-identity-core.ps1`を再実行し、Identity administrationの実PostgreSQL、Business Audit、rollback、
optimistic versionおよびSession invalidation semanticsが不変であることを確認した。

```text
P2-B2 Identity administration T0-T4: 56 / 56 SUCCESS
Formal Framework release staging: Reactor 14 / 14 SUCCESS
Reference artifact in isolated Framework repository: 0
```

初回再実行では、B2 HarnessがReference追加後もRoot全体15 projectをisolated repositoryへstageすることを観測した。
B4-C1のformal release境界に従い、B2 Harnessにも`-pl !koiki-reference-app`とReference非混入assertを追加し、
上記14 projectで再実行した。Framework production artifactまたはPublic APIは変更していない。

### 3.4 Supplemental packaged browser and DB observation

保持中の実PostgreSQLへpackage済みReference JARを接続し、localhostのsecure-cookieだけを手動確認用に解除して実施した。
secret、credential、CookieまたはCSRF tokenは本記録へ保存していない。

| Sequence | Browser observation | DB / Audit observation |
|---|---|---|
| unauthenticated GET | `/identity/users`から`/login`へ302 | mutationなし |
| target detail | Version 0、`EXPENSE_READER`、`EXPENSE:READ` | 初期Role membershipと一致 |
| revoke | Version 1、Role / Permissionなし | `REVOKE_ROLE` SUCCESS、actorはadmin immutable user ID |
| assign | Version 2、Role / Permission復元 | `ASSIGN_ROLE` SUCCESS、同じactor / target |
| first tab revoke | Version 3、Role / Permissionなし | 3件目の`REVOKE_ROLE` SUCCESS |
| stale second tab revoke | 固定messageのerror page、HTTP 409 | versionは3のまま、4件目のAuditなし |

最終DB点検ではtargetが`ACTIVE / version=3 / Roleなし / Permissionなし`、対象user Sessionが0、admin Sessionが残存していた。
409 requestがmutation / Auditを発生させず、adminの操作Sessionを失効させないことを外部観測した。一方、操作前にtarget Sessionを
確立していないため、この補助確認だけをtarget Session失効の証拠とはせず、target / admin / control Sessionを事前確立する正式確認は
B4-4へ残す。

### 3.5 Inventory

| Boundary | Observed result |
|---|---:|
| Reference unique route paths | 4（承認済み範囲） |
| Reference controller mappings | 5（GET 3、POST 2） |
| Reference production classes | 12（package-infoを含む） |
| Reference templates | 3 |
| Reference-owned migration / SQL / Entity / Repository | 0 |
| Reference `@Transactional` / Audit / Session repository operation | 0 |
| Reference import of Framework `internal` package | 0 |
| Identity Public API change | 0 |
| New dependency / BOM entry | 0 |
| Reference test route / credential / failure switch | 0 |

## 4. Explicitly deferred work

| Work package | Remaining work |
|---|---|
| B4-4 | Tooling-owned bootstrap、package済みprocess、実PostgreSQL、HTTP / DB / Audit / multi-Session AC-P2-01 / 02 journey |
| B4-5 | P2-B1〜B4 closeout、全staging Harness inventory、sensitive scan、resource cleanup、Gate B handoff |

## 5. Architecture Owner review points

1. Role assign / revokeを承認済み2 POSTとSpring標準CSRF formだけで構成した境界が妥当か。
2. Reference Use Caseを`IdentityAdministration`への単一委譲とし、transaction / Audit / Session semanticsをFrameworkへ維持した判断が妥当か。
3. URLとApplicationの二重`IDENTITY:ADMIN`およびCSRFにより、拒否時にFramework mutationを呼ばない構成が妥当か。
4. `IdentityOperationException`を固定HTML 400 / 404 / 409 / 503へ変換し、失敗を成功PRGへ偽装しない境界が妥当か。
5. ReferenceがSpring標準compromised-password checkerを明示し、補助的なpackage済みブラウザ確認と正式な再現可能
   AC-P2-01 / 02 journeyを分離してB4-4へ残す判断が妥当か。

2026年9月9日、Architecture Ownerは上記5点をすべて承認した。

Point 5の承認は、Referenceで`IdentityAdministration`を成立させるcompromised-password checker構成に限定する。
将来のpassword管理機能およびCustomer環境における外部照会方針はこの承認に含めず、必要となるPhase / work packageで
別途判断する。正式なAC-P2-01 / 02 multi-Session journeyはB4-4で検証する。
