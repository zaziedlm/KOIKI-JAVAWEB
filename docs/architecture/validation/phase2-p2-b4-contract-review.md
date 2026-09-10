# Phase 2 P2-B4 Reference identity contract review

## 1. Status and scope

- **Review date:** 2026年9月9日
- **Work package:** `P2-B4 / B4-1`
- **Status:** `COMPLETE / ARCHITECTURE OWNER APPROVED — B4-2 READY`
- **Approved by:** Shuichi Kataoka、2026年9月9日
- **Ownership:** Reference（`identity` Controller / Application Use Case / view）
- **Production change at review:** 0。Owner承認前にReference artifact、Public API、route / view、migrationまたはCIを追加しない

本書は、Phase 2 DoD 2-10とReference受入条件AC-P2-01 / AC-P2-02を満たす最小の`identity`管理journeyを、
artifact、module、route、Permission、Use Case、DTO、Framework Public APIおよび検証境界の単位で比較する。

P2-B2で承認したIdentity Public API 10型とAudit / rollback semantics、P2-B3で承認したSpring Session JDBC、
全Session失効およびlogoutをbaselineとする。ReferenceはFramework所有のIdentity table、JPA Entity、Repository、
internal packageまたはmigrationを所有・参照しない。

## 2. Review principles and current decision position

```text
Ownership / module: Reference / identity
Tier and triggers: Tier 1 SIMPLE。Tier 2昇格triggerなし
Responsibility placement: MVC Controller -> Reference Application Use Case -> Framework public contract
Persistence / model: Framework-owned JPA。Reference-owned persistence、Domain Model、Repositoryなし
Read model: 既存IdentityQuery#findByIdをReference-owned immutable view DTOへ変換
Module collaboration: P2-B4ではidentity単独。他Reference moduleとの連携なし
View / API boundary: server-rendered MVC。HTMX / REST / SPAなし
Verification: Method Security、MockMvc、実PostgreSQL、package済みprocess、Audit / Session / inventory
Deferred decisions: 一覧・検索・Role/Permission read API、完全な管理UI、Gate B CI、P2-C1 migration upgrade
```

判断原則は次のとおりとする。

1. ReferenceはKOIKI Frameworkの正規Consumerであり、Framework内部やtest fixtureを近道として使わない。
2. 最初のjourneyで実際に必要なroute、DTOおよびUse Caseだけを作り、将来moduleの空packageを生成しない。
3. URL認可とApplication Use CaseのMethod Securityを併用し、画面表示制御だけで認可しない。
4. Identity mutationのtransaction、Audit、Session失効およびfailure categoryは既存`IdentityAdministration`へ委譲する。
5. Reference applicationのUI方式はserver-rendered MVCに限定し、Phase 3 / 4のHTMX、REST、SPA判断を先行しない。
6. production codeへbootstrap route、固定user / credential、failure switchまたはfixture migrationを追加しない。

Spring Security 7の標準Method Securityは`@EnableMethodSecurity`と`@PreAuthorize`を使用する。KOIKI Security Starterは
すでにMethod Securityを有効化しているため、Reference側で同じ有効化構成を重複させない。

- Spring Security Method Security:
  <https://docs.spring.io/spring-security/reference/servlet/authorization/method-security.html>
- Spring Security MVC integration:
  <https://docs.spring.io/spring-security/reference/servlet/integrations/mvc.html>

## 3. B4-C1 — Reference artifact / module placement

### 3.1 Comparison

| Candidate | Shape | Evaluation |
|---|---|---|
| **A — top-level single executable application** | `koiki-reference-app/`をRoot Reactorの1 Maven moduleとして追加し、業務moduleはJava packageで分割する | **Recommended.** Repository ArchitectureとADR-043の単一モジュラーモノリスを直接表し、root regressionとpackage smokeの対象にできる |
| B — business moduleごとのMaven artifact | `identity`、将来`master`、`expense`を別artifactにする | Reject。業務module境界をdeployable / Maven境界へ過剰に拡大し、単一application方針から逸脱する |
| C — `build-support`配下のfixture | B3同様の非配布fixtureとして管理画面を置く | Reject。DoD 2-10の正式Reference成果物にならず、fixtureからの昇格禁止にも抵触する |
| D — Framework Starter内へReference UIを追加 | Identity StarterにController / templateを置く | Reject。FrameworkとReferenceのOwnershipを混在させ、全Consumerへ画面・routeを持ち込む |

### 3.2 Recommended Maven and distribution boundary

- Maven artifactは`org.koikifw:koiki-reference-app:0.1.0-SNAPSHOT`の単一executable JARとする。
- `koiki-reference-app`はRoot Reactorへ追加し、通常のroot `verify`でcompile / test / Architecture Ruleを検査する。
- Framework formal release unitは承認済み14 projectのままとし、Reference executableをBOMのdependency management、
  Framework Public API互換対象またはFramework release stagingへ含めない。
- B4のpackage検証ではFramework formal release unitを隔離repositoryへ明示的にstageした後、Reference executableを
  Consumer / deployableとしてpackageする。Reference JARをFramework artifactと同じ配布分類に数えない。
- 既存B3 HarnessがRoot全体をformal release unitとしてstageしている箇所は、Reference追加後も14 projectだけを選択するよう
  B4実装sliceで更新し、B1〜B3のartifact count / fixture非混入証拠を維持する。

開始時の直接dependency候補は次に限定する。実効dependency treeはB4-2で固定する。

| Dependency | Scope / reason |
|---|---|
| `koiki-starter-session-jdbc` | browser Session、Identity、Audit、Dataの承認済みFramework構成 |
| `spring-boot-starter-webmvc` | server-rendered MVC。REST API versioning / Problem Detailsを有効化しない |
| `spring-boot-starter-thymeleaf` | server-rendered MVC view。HTMX / SPAなし |
| `flyway-database-postgresql`、`postgresql` | PostgreSQL runtime baseline |
| `org.jspecify:jspecify` | Null Safety annotation |
| `koiki-archunit-rules`、Spring Boot test / Security test | test scopeだけ |

ReferenceからFrameworkの個別Starterへ直接依存する必要が実装時に判明した場合は、transitive dependencyへ偶然依存せず、
利用するPublic contractのowner artifactを明示する。ただし重複Starterや未使用dependencyを先行追加しない。

B4-2実装検証では、`koiki-starter-api`をReference MVCへ導入するとREST向けの必須path-segment API versioningが
`/identity/users`にも適用され、`identity`をversionとして拒否することを確認した。このStarterはPhase 3のREST APIで採用を
再検討し、Phase 2のserver-rendered MVCはSpring Boot標準`spring-boot-starter-webmvc`へ直接依存する。これはREST / MVC境界を
明確化する実装Evidenceによる候補dependencyの補正であり、B4-C1のartifact / distribution判断は変更しない。

## 4. B4-C2 — Reference `identity` module structure

### 4.1 Comparison

| Candidate | Shape | Evaluation |
|---|---|---|
| **A — feature-first Tier 1 package** | `org.koikifw.reference.identity`の中に`adapter.inbound.web`、`application`、`configuration`を必要分だけ置く | **Recommended.** 業務moduleを第一分割軸にし、ControllerとUse Caseを分離できる |
| B — application全体の`controller/service/dto` | 技術layerをReference全体へ横断配置する | Reject。将来module追加時にOwnershipが混在する |
| C — Framework Identity packageを拡張 | `org.koikifw.identity`またはその`internal`へReference型を置く | Reject。Framework namespace / Ownershipへ侵入する |
| D — Tier 2構造を先行生成 | `domain.model`、`domain.service`、Repository / Gatewayを追加する | Reject。状態遷移・複数Entity不変条件・再利用業務ruleがなく、Tier 2 triggerを満たさない |

### 4.2 Recommended package and responsibility inventory

```text
org.koikifw.reference
├── ReferenceApplication                         # application assembly / main
└── identity                                     # @KoikiModule SIMPLE / JPA / SHARED
    ├── adapter.inbound.web
    │   ├── IdentityManagementController         # request、validation、response / redirect
    │   ├── IdentityUserLookupForm               # web input only
    │   └── IdentityRoleChangeForm                # roleCode、expectedVersion
    ├── application
    │   ├── IdentityUserManagement               # protected Reference Use Case
    │   └── IdentityUserView                      # immutable view DTO
    └── configuration
        └── ReferenceSecurityConfiguration        # Reference URL chain / form login assembly
```

名称はB4-2実装inventoryの候補であり、Owner承認後にこの範囲で固定する。次は作らない。

- `domain.model`、`domain.service`、`domain.repository`
- Reference-owned JPA Entity、Spring Data Repository、outbound persistence adapter
- 将来の`master`、`expense`、`notification`、`accounting` package
- 汎用Controller基底class、汎用CRUD service、Framework向けDTO mapper

`identity` module rootは`@KoikiModule(name = "identity", tier = SIMPLE, persistence = JPA,
persistenceModel = SHARED)`で宣言し、`@NullMarked`を付ける。JPA / SHARED metadataはGrand DesignのReference module分類を表すが、
ReferenceがFramework Entityまたはtableを所有する意味にはしない。

## 5. B4-C3 — Initial management journey

### 5.1 Comparison

| Candidate | Evidence value | Required new read API | Evaluation |
|---|---:|---:|---|
| **A — user Role membership** | user参照、Role付与 / 剥奪、Audit、Session失効、後続認可を1 journeyで確認できる | なし | **Recommended.** AC-P2-01 / 02とDoD 2-10へ最短で接続する |
| B — Role Permission管理 | Role versionとPermission候補のread契約が必要 | あり | 後続候補。最初のsliceでPublic API追加が必要なため選ばない |
| C — user一覧 / 作成 | pagination / searchまたは一覧read契約が必要 | あり | 後続候補。管理UI全体を先行設計することになる |
| D — password / external link管理 | credential / issuer inputと追加のsecret・privacy境界を持つ | なし | B2/B3のFramework証拠で成立済み。Reference最初のjourneyとしてはriskが高い |

### 5.2 Recommended journey and routes

最初のjourneyは、事前に存在するtarget userとRoleに対するRole membership管理とする。

1. identity管理者がuser IDを入力する。
2. `IdentityQuery.findById`でuser detailを取得し、Reference DTOとして表示する。
3. 管理者がrole codeと表示済み`expectedVersion`を送信し、`IdentityAdministration.assignRole`を呼ぶ。
4. 最新user detailへredirectし、Role / effective Permission / versionの変化を表示する。
5. 同じ画面から`IdentityAdministration.revokeRole`を呼び、membershipを剥奪できる。

| Method / path | Responsibility |
|---|---|
| `GET /identity/users` | user ID入力form。全user一覧を意味しない |
| `GET /identity/users/{userId}` | 単一user detailを表示 |
| `POST /identity/users/{userId}/roles` | `roleCode`を付与 |
| `POST /identity/users/{userId}/roles/{roleCode}/revoke` | 指定Roleを剥奪 |

全routeはserver-rendered HTMLとし、POSTはSpring標準CSRFを必須にする。Role / Permission masterの作成・削除、一覧・検索、
password、email、disable、external link、unlockは最初のjourneyへ含めない。

acceptance fixtureは既存Roleとtarget / admin / control userを実行ごとに用意してよいが、そのbootstrap mechanismを
Reference production route、template、migrationまたはJARへ含めない。

## 6. B4-C4 — Read contract

### 6.1 Decision

既存`IdentityQuery#findById(FrameworkUserId)`だけで推奨journeyは成立する。P2-B4開始時点ではIdentity Public APIを追加しない。

| Inventory | Before B4 | Recommended at B4 start |
|---|---:|---:|
| Identity Public Java types | 10 | 10（変更なし） |
| `IdentityQuery` methods | 1 | 1（変更なし） |
| Identity configuration properties | 12 | 12（変更なし） |
| Identity tables | 8 | 8（変更なし） |

`IdentityUser`をMVC Modelへ直接入れず、Application Use Case内で`IdentityUserView`へcopyする。DTOはuser ID、表示用email、status、
sort済みRole code、sort済みPermission code、versionだけを持つ。encoded credential、login attempt、external issuer / subject、
JPA Entity、lazy proxyを持たない。

user一覧 / email検索、Role / Permission候補一覧、Role version取得が実際に必要になるsliceでは、Framework RepositoryやSQLへ迂回せず、
型・pagination・順序・PII accessをadditive Public API reviewとして先に提示する。

## 7. B4-C5 — Current actor / Method Security

### 7.1 Comparison

| Candidate | Evaluation |
|---|---|
| URL authorizationだけ | Reject。Controller以外からUse Caseを呼ぶ経路とdirect method invocationを保護できない |
| ControllerでPermissionを手動比較 | Reject。認可がInboundへ閉じ、表示 / route変更で迂回しやすい |
| 独自authorization service / annotation | Reject。1 PermissionのためにSpring標準と重複するPublic abstractionを作る |
| **URL chain + Use Case `@PreAuthorize`** | **Recommended.** request入口を狭め、Application boundaryでも同じPermissionを強制する |

### 7.2 Recommended enforcement

- Reference `SecurityFilterChain`は`/identity/**`へ`IDENTITY:ADMIN` authorityを要求し、form login / logoutとCSRFをSpring標準で構成する。
- `IdentityUserManagement`のpublic Use Case methodはclassまたはmethod levelの
  `@PreAuthorize("hasAuthority('IDENTITY:ADMIN')")`で保護する。
- KOIKI Security Starterが有効化済みのMethod Securityを使用し、Referenceで`@EnableMethodSecurity`を重複させない。
- Controller request parameterからactor ID、RoleまたはPermissionを受け取らない。認証済みSpring `Authentication`のprincipal / authorityだけを正本とする。
- Framework mutationは既存`FrameworkPrincipal.userId()`をSecurityContextから取得し、そのimmutable IDをAudit actorに使用する。
  Referenceはemail、表示名またはrequest parameterからAudit actorを再構成しない。
- Use Case beanはproxy可能なpublic methodとして外部から呼び、同一class内self-invocationでMethod Securityを迂回しない。

検証では、URLを隠すだけでなく、権限不足userによるdirect POSTとApplication Use Caseの直接呼出しの双方を拒否し、
`IdentityAdministration`が呼ばれずDB / Audit / Sessionが変化しないことを確認する。

## 8. B4-C6 — Audit / Session semantics

Reference Application Use Caseは1回のFramework queryまたはmutationだけを調整し、Identity transactionを包む独自の
`@Transactional`境界、Audit recorder呼出し、Session repository操作または補償処理を追加しない。

| Operation | Reference action | Framework-owned result |
|---|---|---|
| detail read | `IdentityQuery.findById` | immutable read model。Audit / Session変更なし |
| Role assign / revoke | `IdentityAdministration.assignRole/revokeRole` | optimistic version、Business Audit、target全Session失効を同じ承認済みsemanticsで実行 |
| Business Audit failure | safe errorへ変換 | Role mutation rollback |
| Session invalidation failure | safe dependency errorへ変換 | Role mutation / Business Audit rollback。先行失効済みSessionは復元しない |
| stale version | conflict response | mutation / Auditなし |

正常系のpackage済みjourneyでは次を外部観測する。

1. Audit actorが管理者のimmutable Framework user IDであり、emailをpayloadへ複製しない。
2. Role membershipとuser versionが更新され、`ASSIGN_ROLE` / `REVOKE_ROLE` Business Auditが存在する。
3. targetの旧Sessionだけが失効し、admin / control Sessionは継続する。
4. targetが再loginすると新しいPermission snapshotが認可へ反映される。

failure系ではFrameworkの承認済みfailure injectionをproductionへ持ち込まず、Tooling-owned PostgreSQL権限またはtest beanだけで
Audit / Session failureを作る。Referenceは`IdentityOperationException`をcatchして成功redirectに変換しない。

## 9. B4-C7 — View / error boundary

### 9.1 View boundary

- MVC ModelへはReference-owned immutable `IdentityUserView`と入力 / feedbackだけを渡す。
- emailは`IDENTITY:ADMIN`で保護されたuser detailに限り表示できるが、URL、redirect parameter、log、Audit payload、test reportへ出さない。
- role / permission codeは管理者向けdetailだけに表示し、encoded password、credential有無、lock counter、issuer / subject、
  Session ID / CookieをModelへ含めない。
- templateはCSRF tokenをSpring MVC / Thymeleaf統合へ委譲し、JavaScript local storageや独自tokenを使用しない。
- controllerからFramework Entity / Repository / internal typeを参照せず、`IdentityUser`もtransaction外のModelへ渡さない。

### 9.2 Safe MVC failure mapping

| Failure | HTTP / MVC result | Data boundary |
|---|---|---|
| unauthenticated | Spring Security login entry point | request target以外のIdentity情報なし |
| access denied | `403 Forbidden` | 対象userの存在、Role、emailを返さない |
| malformed user ID / invalid form | `400 Bad Request`または同formのvalidation error | raw exception / stack traceなし |
| `NOT_FOUND` | `404 Not Found` | user enumerationを広げる詳細なし |
| `CONFLICT` / `CONCURRENT_MODIFICATION` | `409 Conflict`、再読込を促す固定message | 現在versionやDB詳細をerrorへ含めない |
| `INVALID_INPUT` | `400 Bad Request`、固定message | Framework validation詳細をそのまま露出しない |
| `DEPENDENCY_FAILURE` | `503 Service Unavailable`、再試行可能な固定message | JDBC URL、SQL、table、class、causeなし |

HTML error responseはReference-ownedの最小共通handlerで一貫させる。Phase 3のREST Problem DetailsやFramework共通例外階層を
先行実装しない。PRGを使う正常POSTでも、失敗を成功flash / redirectとして偽装しない。

## 10. B4-C8 — Packaging / Gate B handoff

### 10.1 Verification layers

| Layer | Evidence |
|---|---|
| B4-T0 | Maven / dependency / artifact / package / route / Permission / Public API / migration inventory |
| B4-T1 | Tier 1 Use Case、DTO変換、safe failure mapping、Method Security direct invocation |
| B4-T2 | MockMvcで未認証、権限不足、CSRF、not found、stale version、正常Role変更 |
| B4-T3 | 実PostgreSQLでRole row、version、Business Audit、rollback、Session対象範囲 |
| B4-T4 | package済みReference processへnetwork越しHTTPでAC-P2-01 / 02とDoD 2-10を確認 |
| B4 closeout | P2-B1〜B3回帰、root、Architecture Contract、ArchUnit、NullAway、sensitive scan、resource cleanup |

package済み検証は、実行時生成のadmin / target / control identityとcredentialをTooling-owned bootstrapで用意する。
Reference JARへfixture controller、failure switch、固定credential、test migrationを含めない。HTTP status、redirect、CSRF、DB row、
Audit row、Session継続 / 失効を組み合わせ、画面textやlogだけで成功判定しない。

### 10.2 Distribution and inventory assertions

| Boundary | Required assertion |
|---|---|
| Root Reactor | Referenceを含む15 projectが成功する |
| Framework formal release unit | 14 projectのまま。Reference artifact非混入 |
| BOM | `koiki-reference-app`を追加しない |
| Identity Public API | 10型 / query method 1件を維持 |
| Reference migration / table | 0件 |
| Reference Framework-internal imports | 0件 |
| Reference routes | §5.2の4 routeだけ |
| Reference management Permission | `IDENTITY:ADMIN` 1件 |
| Deferred dependency | HTMX、React、WebFlux、SAML、Redis、AWS SDK、Oracleなし |
| Sensitive output | credential、Cookie、Authorization、encoded password、issuer / subject、email形式PIIなし |
| Cleanup | child process、PostgreSQL container、temporary directory残存0 |

Gate BではP2-B1〜B4 aggregate、package済みjourneyおよびCI候補を別途reviewする。B4実装途中でworkflow、required check、
remote environmentまたはsecretを変更しない。

## 11. Proposed implementation slices after approval

### B4-2 — Application skeleton and protected read

- `koiki-reference-app`と`identity` Tier 1 packageを追加する。
- user ID lookup / detail、Reference DTO、URL / Method Security、未認証・権限不足・not foundを検証する。
- Identity Public API、Reference migration / Repositoryを追加しない。

### B4-3 — Role membership mutation

- Role assign / revokeをReference Use Caseから`IdentityAdministration`へ接続する。
- CSRF、direct request拒否、stale version、Audit / Session normal / failure semanticsを検証する。

### B4-4 — Packaged journey

- package済みReference processと実PostgreSQLでAC-P2-01 / 02、DoD 2-10を外部観測する。
- artifact / dependency / Public API / migration / sensitive output / resource cleanupを検査する。

### B4-5 — Closeout and Gate B handoff

- P2-B1〜B4、root、Null Safety、inventoryを同一HEADで集約する。
- Architecture Owner承認後だけGate B aggregate / CI候補reviewへ進む。

## 12. Explicitly deferred

- Identity user一覧、email検索、pagination
- Role / Permission一覧・検索・作成・削除・Role Permission管理画面
- password、email、disable / enable、unlock、external linkのReference管理画面
- local password self-service reset、mail / SMS、provisioning、Customer属性
- Reference-owned Identity table / migration / Repository
- `master`、`expense`、`notification`、`accounting` module
- HTMX、REST API、Problem Details、React SPA
- Gate B workflow / required化、P2-C1 migration clean / supported upgrade
- Redis、Oracle、SAML、AWS固有Adapter

## 13. Decisions requested

| ID | Recommended decision | Rejected / deferred | Status |
|---|---|---|---|
| B4-C1 | top-level単一executable、Root Reactor参加、Framework formal release / BOMから分離 | Maven module分割、fixture / Starter配置 | **APPROVED** |
| B4-C2 | `org.koikifw.reference.identity`のTier 1 feature-first構造 | technical-layer root、Tier 2先行生成 | **APPROVED** |
| B4-C3 | 単一user参照 + Role membership付与 / 剥奪 | 全管理UI、password / external link | **APPROVED** |
| B4-C4 | 既存`findById`だけを使用しPublic API追加0 | 一覧 / Role / Permission read API | **APPROVED** |
| B4-C5 | URL chain + Application Use Case `@PreAuthorize` | URLだけ、Controller手動判定、独自認可 | **APPROVED** |
| B4-C6 | FrameworkのAudit / Session / transaction semanticsへ委譲 | Referenceでの重複記録・補償・成功偽装 | **APPROVED** |
| B4-C7 | Reference DTO、admin限定PII表示、safe HTML error mapping | Entity / `IdentityUser`直接露出、REST共通化先行 | **APPROVED** |
| B4-C8 | B4-T0〜T4 + closeout、Gate B CIは後続 | workflow / required化の先行 | **APPROVED** |

推奨判断はB4-C1〜C8を上記どおり承認し、B4-2をReference application skeleton、protected lookup / detailおよび
Method Securityの最小sliceとして開始することである。Owner承認前にproduction codeまたはPublic APIを追加しない。

2026年9月9日、Architecture OwnerはB4-C1〜C8を推奨案どおり承認した。これによりB4-1を`COMPLETE`とし、
B4-2ではReference application skeleton、単一user lookup / detail、URL / Method Securityおよび代表拒否経路に限定して
実装を開始する。Role membership mutationはB4-3、package済み実PostgreSQL journeyはB4-4より前へ進めない。
