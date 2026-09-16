# Phase 3 P3-C4 Journey / ADR / Skill / DoD traceability closeout

## 1. Status

| Item | Result |
|---|---|
| Date | 2026年9月16日 |
| Owner approval date | 2026年9月17日 |
| Branch | `feature/phase3-reference-vertical-slice` |
| Phase 3 start baseline | `c88b335efdd556613c9ef7f4c5267214fdb8254b` |
| P3-C4 start HEAD | `4f6b2ccc4b186c46d5dfcba1f83d0e5fb951da65` |
| Current status | `COMPLETE / OWNER APPROVED` |
| Ownership | Architecture documentation / closeout verification |
| Production change in C4-1〜C4-4 | 0 |
| Remote mutation | 0 |
| Next | Remote Gate（個別Owner承認前は未開始） |

P3-C4は、Phase 3で追加した実装と承認Evidenceを横断して、DoD、Reference AC、Journey、ADR、Skill、
Public API、migration、dependency、artifactおよびdeferred inventoryを一致させるcloseout CPである。

本節までに実施したC4-1はread-onlyのGit baseline差分分類である。Maven、Docker、PostgreSQL、Browser、
package済みJARまたはfocused Toolingはまだ実行しておらず、P3-C4 closeを宣言しない。

## 2. Comparison method

次の範囲をGitのcommit済み差分として比較した。

```text
c88b335efdd556613c9ef7f4c5267214fdb8254b..4f6b2ccc4b186c46d5dfcba1f83d0e5fb951da65
```

- `git diff --name-status`で追加・変更・削除を分類した。
- `git diff --shortstat`とtop-level / source-set単位の件数を突合した。
- POM、Public API inventory、migration、workflowは個別に差分を確認した。
- commit履歴をP3-CP0〜P3-C4 start handoffのCP / Gate記録と照合した。
- 内容判断が必要なFramework rule、Security設定、application propertyは実diffを確認した。

生成物、untracked file、作業中の差分をbaseline比較へ混入させないため、C4-1はP3-C4準備commit後の
clean HEADから開始した。

## 3. Aggregate result

| Measure | Result |
|---|---:|
| Commits after Phase 3 baseline | 28 |
| Changed files | 194 |
| Added | 172 |
| Modified | 22 |
| Deleted | 0 |
| Insertions | 14,833 |
| Deletions | 79 |

主要file typeは次のとおりである。

| Type | Count |
|---|---:|
| Production Java | 96 |
| Test Java | 26 |
| Maven POM | 7 |
| Markdown | 41 |
| SQL migration | 3 |
| HTML / Thymeleaf | 14 |
| Properties | 2 |
| PowerShell | 2 |
| JavaScript | 1 |
| CSS | 1 |
| Auto-configuration imports metadata | 1 |

削除またはrenameはなく、既存Phase 2契約を除去する差分は認めない。

## 4. Top-level classification

| Root / area | Files | Ownership | Classification | Primary CP / Gate |
|---|---:|---|---|---|
| `AGENTS.md` | 1 | Repository guidance | Phase状態とstop conditionの同期 | P3-CP0〜C4 |
| Root `pom.xml` | 1 | Build Foundation | Web MVC StarterをRoot Reactorへ追加 | P3-B0 / B2 |
| `koiki-dependencies-bom` | 1 | Framework | Web MVC Starter座標とHTMX 2.0.10を管理 | P3-B0 / B2 |
| `koiki-archunit-rules` | 4 | Framework | ADR-049の狭いmodule contract例外とpositive / negative fixture | P3-A0 / A1 |
| `koiki-starters/koiki-starter-web-mvc` | 8 | Framework | 正式Starter、内部auto-configuration、resource contract | P3-B0 / B2 / B3 |
| `koiki-reference-app` | 132 | Reference | master / expense、MVC、REST、Security統合、migration、test | P3-A1〜C1 |
| `build-support` | 13 | Tooling | local demo、browser、API、critical E2E、index | P3-B2〜C2 |
| `docs` | 34 | Architecture / Reference / Development | contract、Evidence、Journey、Skill、handoff | 全CP / Gate |
| **Total** | **194** | — | — | — |

想定外のtop-level directory、Customer artifact、Project Template、workflow、cloud固有Adapterまたは
Phase 4 moduleの変更は存在しない。

## 5. Framework classification

### 5.1 Build / artifact

| Change | Result | Disposition |
|---|---|---|
| Root Reactor | `koiki-starter-web-mvc`を1 module追加 | P3-B0承認、P3-B2実装 |
| Formal release inventory | 15 projects / 12 JAR | P3-B0承認値と一致する見込み。C4 inventoryで再検証 |
| Root Reactor inventory | 16 projects / 13 JAR | P3-B2 Evidenceと一致 |
| Formal publish unit | Root aggregatorを除く14座標 | P3-B0承認値。Remote publishは未実施 |
| BOM | Web MVC Starter座標、HTMX `2.0.10`を追加 | P3-B0承認済み |
| Java Public API inventory | Phase 3 start baselineとの差分0 | Web MVC StarterはJava Public API 0型 |

`build-support/api-compatibility/public-api.txt`はbaselineと同一であり、`PersistenceModel.SEPARATED`を含む
Public API追加はない。新Starterは正式artifactだが、Java classをconsumer APIとして公開せず、internal
auto-configurationとclasspath resource contractだけを提供する。

### 5.2 Web MVC Starter

追加8 filesは次の責務へ限定される。

- Maven dependency aggregation。
- `internal` packageのauto-configuration。
- Spring Boot auto-configuration imports metadata。
- 共通Thymeleaf fragment。
- `/koiki-web/**`配下のCSSとHTMX integration JavaScript。
- artifact README。

Reference業務Controller、Form、View DTO、Template、PermissionまたはmigrationはStarterへ混入していない。

### 5.3 Architecture rule

`BusinessModuleRuleSet`はRule 3へADR-049の狭いread-only contract例外を追加した。許可条件は次のすべてである。

- 呼出元がconsumer moduleの`adapter.outbound`である。
- 呼出先がprovider moduleの`contract`である。
- contractがinterfaceであり、名称が`Query`で終わる。

test fixture 2 filesと`BusinessModuleRuleSetTest`変更により、許可経路と境界違反を検査する。
Framework Identity、Security、Data、AuditまたはSessionのPublic APIは変更していない。

## 6. Reference classification

`koiki-reference-app`の132 filesは次のsource setへ分類できる。

| Source set | Files | Classification |
|---|---:|---|
| `src/main/java` | 93 | master、expense、identity Security fitting、home |
| `src/main/resources` | 18 | application config 2、migration 3、Template 13 |
| `src/test/java` | 20 | Domain / Application / Persistence / MVC / REST / Security integration |
| module POM | 1 | approved Starter / cache / test dependency selection |

### 6.1 Business module ownership

| Module / area | Main Java files | Responsibility | Primary Evidence |
|---|---:|---|---|
| `master` | 29 | Tier 1管理Use Case、JPA、current-value contract、MVC | P3-A1、B1〜B3 |
| `expense` | 56 | Tier 2 Domain、JPA共有model、scope、event、read model、MVC、REST | P3-A2〜A4、B1〜B4、C1 |
| `identity.configuration` | 7 added + 1 modified | MVC Session chain維持、P3-C1限定Bearer chain / Problem response | P3-B2、C0 / C1 |
| home | 1 | server-side UI navigation | P3-B2 |

master / expenseは単一`koiki-reference-app`内の業務packageであり、別Maven artifactへ分割していない。
expenseはJPA共有モデルを維持し、MyBatis code、Mapperまたは分離Persistence Modelを含まない。

### 6.2 Resources and schema

| Resource | Count | Classification |
|---|---:|---|
| `application.properties` | 1 modified | Reference Flyway、cache / TTL、MVC API versioning基礎設定 |
| `application-api-bearer.properties` | 1 added | P3-C1限定Bearer代表profile |
| Reference migration | 3 added | V1 master、V2 expense、V3 approver scope |
| Reference table | 6 | master 3、expense 3。module内FKのみ |
| Thymeleaf Template | 13 changed | home、identity fitting、master、expense |

Framework migration差分は0であり、Reference migrationは`classpath:db/migration/kkref`と
`kkref_flyway_history`に分離される。production seedは追加していない。

### 6.3 Dependency classification

Reference POMは次の承認済み変更を持つ。

- Spring MVC / Thymeleaf直接依存を正式`koiki-starter-web-mvc`へ置換。
- REST / Problem Detailsのため`koiki-starter-api`を追加。
- Reference限定cacheのためSpring Cache / Caffeineを追加。
- test scopeへ`koiki-testing`を追加。

MyBatis、Redis、WebFlux、Spring Modulith runtime、SPA、SAML、OracleまたはAWS固有dependencyは追加していない。

## 7. Tooling classification

`build-support`差分13 filesはすべて非配布Toolingである。

| Tooling | Files | Purpose | Boundary |
|---|---:|---|---|
| Root Tooling index | 1 modified | 各verification入口を案内 | code / artifactなし |
| `reference-local-demo` | 2 added | 使い捨てlocal seedと手動確認支援 | production seedではない |
| `reference-browser-verification` | 4 added | HTMX / CSRF / history / conflictの実Chromium確認 | Root Reactor外、test scope |
| `reference-api-verification` | 3 added | package済みJARのBearer API journey | Root Reactor外、test scope |
| `reference-e2e-verification` | 3 added | Browser / API / DB / Audit / log critical journey | Root Reactor外、test scope |

Playwright、test issuer、key、token、user、password、failure / seed fixtureはFramework artifact、Reference JAR、
`koiki-testing`、Project TemplateまたはCustomer dependencyへ含めていない。

## 8. Documentation / governance classification

docs差分34 filesは次のように分類する。

| Area | Files | Classification |
|---|---:|---|
| Agent Skills | 2 modified | Phase状態、Ownership、業務feature workflowの同期 |
| Architecture | 23 | ADR / Grand Design fitting、Phase estimate、index、Phase 3 Evidence |
| Development | 6 | Phase 3実行計画とGate / CP start handoff |
| Reference | 3 | 業務仕様、Reference index、Local Run Guide |

Phase 3 validation EvidenceはP3-CP0、P3-A0〜A4、Gate A、P3-B0〜B4、Gate B、P3-C0〜C3を
連続して記録する。P3-C3は未完了扱いではなく、Architecture Owner判断による
`DEFERRED — MyBatis adoption trigger required`として記録済みである。

## 9. Commit / CP classification

baseline後28 commitsは、次の承認系列から外れていない。

| Sequence | Commit purpose | Classification |
|---|---|---|
| P3-CP0 / A0 | Phase開始、Docker baseline補正、module / migration contract | Documentation / blocking review |
| P3-A1〜A4 | master、expense、authorization / Audit、同期event | Reference production + test |
| Gate A | Milestone A acceptance | Documentation / Owner acceptance |
| P3-B0 | MVC / HTMX contract | Documentation / blocking review |
| P3-B1〜B4 | read model、MVC、Tooling、HTMX、lock / cache | Framework + Reference + Tooling |
| Gate B | inventory、browser stabilization、accessibility remediation、acceptance | Production / test / documentation |
| P3-C0 | REST contract | Documentation / blocking review |
| P3-C1 | REST API、Bearer profile、API Tooling | Reference + Tooling |
| P3-C2 | critical journey E2E | Tooling + Evidence |
| P3-C3 | MyBatis adoption-trigger deferral | Documentation only |
| P3-C4 start | traceability closeout handoff | Documentation only |

P3-C3またはP3-C4準備commitにproduction、Public API、dependency、migrationまたはworkflow変更はない。

## 10. Boundary checks

| Check | C4-1 result |
|---|---|
| Unexpected top-level path | 0 |
| Deleted / renamed baseline contract | 0 |
| Java Public API inventory diff | 0 |
| Framework migration diff | 0 |
| Reference migration | V1〜V3、3 files / 6 table |
| Workflow diff | 0 |
| MyBatis `SEPARATED` / fixture / test dependency | 0 |
| Root ReactorへのTooling混入 | 認めない |
| Phase 4 production code / module | 0 |
| Remote mutation | 0 |

## 11. Findings and follow-up

### 11.1 Approved / consistent

- 194 filesはすべて承認済みCP、Gate、P3-C3延期またはP3-C4開始準備へ分類できる。
- Framework / Reference / Tooling / DocumentationのOwnershipを跨いだ未説明の昇格は認めない。
- 正式Framework追加はWeb MVC Starter 1 artifactであり、P3-B0 / B2の承認範囲と一致する。
- Public API、Framework migrationおよびworkflowは承認済みbaselineを維持する。

### 11.2 Explicitly deferred / pending

- P3-C3 MyBatis規約fixture、`PersistenceModel.SEPARATED`、Rule 25〜27 / 30〜37。
- DoD 3-11の実CI PASS、workflow / required check / remote operation。
- Phase 4、optional Gateおよび実行計画§13のdeferred scope。

### 11.3 Documentation gap for C4-2 / Journey work

Reference Local Run Guideの対象表示はP3-B2であり、Phase 3完成形の通常Session MVC、P3-C1限定Bearer profile、
browser / API / critical E2E Toolingへの入口がReference engineer-facing Journeyとしてまだ集約されていない。
production defectではなく、P3-C4のJourney / Skill closeoutで最小差分補正するdocumentation gapと分類する。

## 12. C4-1 conclusion

Phase 3開始baselineからP3-C4開始HEADまでの194 filesを、Framework、Reference、Tooling、Documentation、
承認CP / Gateへ分類できた。未説明のproduction、Public API、migration、dependency、workflowまたはPhase 4差分は
検出していない。

C4-1を`COMPLETE`とし、次はC4-2 DoD 3-1〜3-11、AC-P3-01〜10、ADRおよびSkillのtrace matrixを作成する。
この結論はMaven / runtime verificationまたはP3-C4 close承認を代替しない。

## 13. C4-2 trace method and status vocabulary

C4-2はGrand DesignのDoD、Reference SpecificationのAC、承認済みADRおよびKOIKI固有Skillを、
Phase 3の実装・自動test・DB / Audit / log観測・Owner判断へ追跡した。ここでは過去CPの承認済み結果を
再利用し、Maven、Docker、Browserまたはpackage済みJARを再実行していない。

| Status | Meaning |
|---|---|
| `SATISFIED / OWNER ACCEPTED` | 実装と自動testに加え、必要なDB / Audit / log / browser観測が承認済みEvidenceに存在する |
| `CONSISTENT` | ADR / Skillの指示と実装・Evidence・deferred境界が一致する |
| `PENDING CI EVIDENCE` | local候補は受入済みだが、Owner承認済みworkflowによる実CI PASSがまだ存在しない |
| `DEFERRED BY DECISION` | 欠落ではなく、理由・安全境界・再開条件をOwnerが明示して延期した |

文書リンクだけを充足根拠とせず、代表test class / method、観測対象および最終Owner判断を各行に含める。

## 14. DoD 3-1〜3-11 trace

| DoD | Implementation / representative verification | Observed evidence / Owner decision | C4-2 status |
|---|---|---|---|
| 3-1 | `master` Tier 1 SIMPLE / JPAと`expense` Tier 2 RICH / JPA共有model。`MasterPostgreSqlIntegrationTest`、`ExpenseRequestTest.followsAllSevenAcceptedTransitions`、`ExpensePostgreSqlIntegrationTest.persistsAcceptedLifecycleWithSharedJpaModelAndOptimisticVersion` | P3-A1 / A2でPostgreSQL、state / version、Audit、rollbackを観測。Tier選択は実行計画、Specification、Skillへ記録しGate Aで承認 | `SATISFIED / OWNER ACCEPTED` |
| 3-2 | 同期`DepartmentDeactivating`とexpense listener。`DepartmentDeactivationPostgreSqlIntegrationTest.rollsBackDepartmentDeactivationForAllFourPendingExpenseStates` | 4未処理状態で部門active維持、Business Audit 0件、transaction rollbackをP3-A4 / Gate Aで確認 | `SATISFIED / OWNER ACCEPTED` |
| 3-3 | masterはmaster-owned event / query contractだけを公開しexpenseを直接参照しない。`ReferenceArchitectureTest`と`BusinessModuleRuleSetTest` | ADR-049の依存方向、production直接参照0、同期rollbackをP3-A4 / Gate Aで承認 | `SATISFIED / OWNER ACCEPTED` |
| 3-4 | MVC Model / TemplateへEntityを渡す経路をArchitecture testで拒否。`ReferenceArchitectureTest`をRoot Reactor verifyへ包含 | Gate BでMVC Model型、Template inventory、Web MVC Starter Java Public API 0型を突合 | `SATISFIED / OWNER ACCEPTED` |
| 3-5 | 選択的HTMX 11契約、通常HTML fallback、CSRF header自動注入。P3-B3 MockMvcと`ReferenceHtmxJourneyTest` | headed Chromiumでlogin、検索、履歴、validation、CSRF、部分errorを確認し、DB / Audit / logと突合 | `SATISFIED / OWNER ACCEPTED` |
| 3-6 | JPA `@Version`とReference専用409競合画面。`ExpensePostgreSqlIntegrationTest.jpaVersionRejectsStalePersistenceContextWithoutStateOrAuditSideEffects`およびbrowser 2 context test | 先行だけ更新、後発409、state / version / Audit非更新をP3-B4 / Gate Bで確認 | `SATISFIED / OWNER ACCEPTED` |
| 3-7 | masterはJPA projection、expenseはscope先行のread-only `JdbcClient`。`ExpenseReadModelPostgreSqlIntegrationTest.enforcesApplicantApproverAndAccountingScopeBeforeMaterialization` | SQL時点のscope、取得後filter 0、Application所有recordへのmaterializeをP3-B1 / Gate Bで承認 | `SATISFIED / OWNER ACCEPTED` |
| 3-8 | submit / approve / rejectをBusiness transaction内で監査。`ExpensePostgreSqlIntegrationTest.recordsApproveRejectReturnAndReeditSuccessesWithinTheBusinessTransaction` | 成功時stateとAuditを同時記録し、拒否 / rollback時Audit 0件をP3-A3、C1、C2でDB突合 | `SATISFIED / OWNER ACCEPTED` |
| 3-9 | active経費科目だけをReference local Caffeine cacheへ30秒保持し、commandはcurrent-value確認を継続 | TTL前後のbrowser / DB観測、inactive master command拒否、認可cache 0をP3-B4 / Gate Bで承認 | `SATISFIED / OWNER ACCEPTED` |
| 3-10 | `/api/v1/expense-requests`のcreate / read / submitと独立REST DTO / Problem Details。`ExpenseApiControllerTest`、`ExpenseApiPostgreSqlIntegrationTest` | 実署名Bearer、HTTP 201 / 200 / 204、DB / Audit / log、MVC parityをP3-C1で承認 | `SATISFIED / OWNER ACCEPTED` |
| 3-11 | Root Reactor外の`PackagedReferenceCriticalJourneyTest.crossesBearerAndSessionBoundariesAndReconcilesExternalEvidence` | package済みJAR、PostgreSQL、Bearer API、Session Chromium、HTMX、DB / Audit / sanitized logを最終コード3回連続PASS。P3-C2をCI候補として承認したが実workflow PASSは未実施 | `PENDING CI EVIDENCE` |

3-2と3-3の核心は同じP3-A4実行で成立しており、業務rollbackとmodule依存方向を別々の推測で補っていない。
3-11以外に未充足DoDは認めない。3-11はRemote Gateでworkflow内容とremote mutationを個別承認し、Gate Cへ
実CI PASS Evidenceを入力するまで完了へ変更しない。

## 15. AC-P3-01〜10 trace

| AC | Representative implementation / test | DB / Audit / UI / API evidence | Owner evidence | C4-2 status |
|---|---|---|---|---|
| AC-P3-01 | master管理Use Case、JPA、method security、`MasterAdministrationTest` / `MasterPostgreSqlIntegrationTest` | 有効master保存、拒否rollback、管理Business Audit。B2 / B3でMVC検索・管理操作 | P3-A1、P3-B2 / B3 | `SATISFIED / OWNER ACCEPTED` |
| AC-P3-02 | expense Aggregate 7遷移、create / edit / submit Use Case、`ExpenseRequestTest` / `ExpensePostgreSqlIntegrationTest` | `DRAFT`作成から`SUBMITTED`、line / total / version / AuditをPostgreSQLで突合 | P3-A2 / A3、P3-C1 / C2 | `SATISFIED / OWNER ACCEPTED` |
| AC-P3-03 | Aggregate不変条件とApplication validation。APIではrepresentation validationも実施 | 金額不一致、日付・金額・master無効、stale versionを拒否し、Aggregate / Audit非更新 | P3-A2 / A3、P3-C1 | `SATISFIED / OWNER ACCEPTED` |
| AC-P3-04 | approve / reject / return遷移とexact department scope。`recordsApproveRejectReturnAndReeditSuccessesWithinTheBusinessTransaction` | 許可状態への遷移と同一transactionのBusiness AuditをDB突合 | P3-A3、P3-B4 / C2 | `SATISFIED / OWNER ACCEPTED` |
| AC-P3-05 | method security、ownership / scope / state check。`enforcesPermissionOwnershipAndExactDepartmentScopeWithoutAuditLeakage` | 自己承認、scope外、不正状態を拒否し、state / Audit変更0 | P3-A3、P3-C1 | `SATISFIED / OWNER ACCEPTED` |
| AC-P3-06 | `DepartmentDeactivationPostgreSqlIntegrationTest.rollsBackDepartmentDeactivationForAllFourPendingExpenseStates` | `DRAFT` / `SUBMITTED` / `APPROVED` / `RETURNED`参照時に部門廃止とAuditをrollback | P3-A4 / Gate A | `SATISFIED / OWNER ACCEPTED` |
| AC-P3-07 | `DepartmentDeactivationPostgreSqlIntegrationTest.deactivatesDepartmentWithNoExpenseOrOnlyTerminalExpenseStates` | 申請なし、`REJECTED` / `SETTLED`のみで部門廃止とAudit 1件 | P3-A4 / Gate A | `SATISFIED / OWNER ACCEPTED` |
| AC-P3-08 | JPA optimistic lock、409 mapping、2 BrowserContext journey | 先行更新成立、後発競合画面、後発によるstate / version / Audit副作用0 | P3-B4 / Gate B | `SATISFIED / OWNER ACCEPTED` |
| AC-P3-09 | scopeをSQL内で強制するexpense read model。`enforcesApplicantApproverAndAccountingScopeBeforeMaterialization` | applicant / approver / accountingのscope内だけをlist / detailへmaterialize、取得後filter 0 | P3-B1 / Gate B、P3-C1 | `SATISFIED / OWNER ACCEPTED` |
| AC-P3-10 | MVC / REST Controllerが同じApplication input portを使用し、Form / View DTO / REST DTOは分離 | 同一actorのcreate / read / submitでPermission、不変条件、state / version / Audit結果をPostgreSQLと外部HTTPで突合 | P3-C1 / C2 | `SATISFIED / OWNER ACCEPTED` |

Gate Aで明示したとおり、申請作成・提出と部門廃止を別transactionから完全同時実行した場合のstrict race保証は
AC-P3-06 / 07の受入範囲外である。これはAC未充足ではなく、production要件化時にlock / isolationを再設計する
既知の境界である。

## 16. ADR consistency trace

| ADR | Phase 3 fitting / implementation | Evidence / disposition | C4-2 status |
|---|---|---|---|
| ADR-004 / 022 | 単一Reference modular monolith内のmaster Tier 1 / expense Tier 2とArchitecture rule | P3-A1〜A4、`ReferenceArchitectureTest`、Gate A | `CONSISTENT` |
| ADR-023 / 024 / 028 | expenseのJPA共有Domain / Entity、domain repository + Spring Data、OSIV無効、外部Entity露出0 | P3-A2、B1 / B2、Gate B | `CONSISTENT` |
| ADR-025 / 049 | 同期Domain Eventでcommand rollback、master-owned narrow current-value query、Reference V1〜V3、module内FKのみ | P3-A0 / A4、Gate A | `CONSISTENT` |
| ADR-006 / 007 / 008 | 通常server-side UIはSession、P3-C1限定APIはstateless Bearer。MVCとRESTのSecurity chainを分離 | P3-B2、C0 / C1。Phase 4 Profile S判断はdeferred | `CONSISTENT` |
| ADR-027 | Thymeleaf HTML主軸、HTMX 2.0.10を効果がある11契約へ選択適用、第三者Spring integration非採用 | P3-B0 / B3、Gate B | `CONSISTENT` |
| ADR-037 | Reference表示専用local cache、30秒TTL、認可・command判断をcacheしない | P3-B4、Gate B | `CONSISTENT` |
| ADR-038 | 複数owner表示queryだけをscope先行read-only JOINとし、Application recordを直接materialize | P3-B1、Gate B、AC-P3-09 | `CONSISTENT` |
| ADR-039 | Level B方針を維持しつつ、具体需要までMyBatis詳細規約と実装を延期。Rule 8拒否を維持 | P3-C3 deferral。`SEPARATED`、Rule 25〜27 / 30〜37、fixture / dependency追加0 | `DEFERRED BY DECISION` |
| ADR-041 / 043 | Reference code / fixtureをFramework Public APIへ自動昇格せず、単一`koiki-reference-app`で題材を完成 | C4-1 Java Public API差分0、Reference classification | `CONSISTENT` |
| ADR-042 | Framework migrationとReference V1〜V3 / history / table ownershipを分離 | P3-A0 / A1 / A2、C4-1 Framework migration差分0 | `CONSISTENT` |
| ADR-046 / 047 / 048 | Phase 2 default deny、Identity、Business / Security Audit、Session JDBC契約を再利用し弱めない | P3-A3、B2〜B4、C1 / C2 | `CONSISTENT` |

ADR-039だけが実装延期であるが、ADR自体を否定していない。採用トリガー、blocking review、安全境界が
`phase3-p3-c3-mybatis-deferral.md`に残るため、P3-C4およびGate Cをblockする不整合ではない。

## 17. KOIKI Skill consistency trace

| Skill instruction | Phase 3 realization | C4-2 status |
|---|---|---|
| Project Overview: Framework / Reference / Customer / Tooling / Walking SkeletonのOwnershipを分離 | Web MVC StarterだけをFrameworkへ追加。業務codeはReference、browser / API / E2E fixtureはRoot Reactor外Tooling、Customer変更0 | `CONSISTENT` |
| Project Overview: modular monolithと承認済みCP / Gateを順守 | master / expenseを単一Reference artifactのpackageとして分離し、A / B / Cの順でOwner Evidenceを確定 | `CONSISTENT` |
| Business Feature Work: master Tier 1 SIMPLE、expense Tier 2 RICH / JPA共有model | A1 / A2の実装、Domain 7遷移、JPA optimistic version、PostgreSQL Evidence | `CONSISTENT` |
| Business Feature Work: inbound / application / domain / outbound責務とDomain / Entity非露出 | MVC Form / View DTO、REST DTOを分離し、共通Application input portだけを再利用。Architecture / Controller testで固定 | `CONSISTENT` |
| Business Feature Work: module連携は同期event、ADR-049の狭いread-only queryだけを例外化 | DepartmentDeactivating + expense listener、master `*Query` contractへのadapter.outbound限定依存 | `CONSISTENT` |
| Business Feature Work: queryはmaster JPA projection、expense JdbcClient、scopeをSQLで先に強制 | P3-B1の2方式とAC-P3-09、取得後filter 0 | `CONSISTENT` |
| Business Feature Work: JPA既定、MyBatisは明示Gate後だけ | P3-C3をadoption trigger待ちにし、Rule 8拒否とJPA baselineを維持 | `DEFERRED BY DECISION / CONSISTENT` |
| 両Skill: 実装検証を文書上の推測より優先し、Public API / Framework昇格を抑制 | PostgreSQL、browser、package済みJAR、DB / Audit / log Evidenceを各CPで取得。Java Public API差分0 | `CONSISTENT` |

Skillの設計指示とPhase 3実装に矛盾は認めない。ただしProject Overviewの現在地表示はC4-2完了へ同期し、
Engineer-facing Journeyの入口不足はC4-3以降でReference文書を最小補正する。

## 18. C4-2 gaps, deferred items and non-gaps

| Classification | Item | Required follow-up |
|---|---|---|
| Pending | DoD 3-11の実workflow PASS | Remote Gateでworkflow / required check / remote mutationを個別承認し、Gate Cへ実CI Evidenceを入力 |
| Documentation gap | Phase 3完成形のSession MVC、Bearer API、focused Tooling、critical E2EへのReference入口 | C4-3以降のEngineer-facing Journey補正。production / profile変更は行わない |
| Deferred by decision | P3-C3 MyBatis詳細規約 / fixture / rule / Public API | adoption trigger成立後にblocking reviewを再開。それまではRule 8拒否を維持 |
| Deferred by phase | SPA、Level 2 / async event、accounting、Project Template、migration toolingほかPhase 4 / 5成果物 | 当該Phase / optional Gateまで先行しない |
| Nonblocking observation | 申請者email表示のIdentity / business profile Ownership、限定範囲外のaccessibility certification | 既存Evidenceの再開条件を維持し、Gate Cの必須AC / DoDへ読み替えない |
| Explicit non-gap | AC-P3-06 / 07のstrict concurrent race保証 | production要件化時にlock / isolationを再設計。現在の受入契約は変更しない |

## 19. C4-2 conclusion

DoD 3-1〜3-10とAC-P3-01〜10は、具体的な実装、自動test、DB / Audit / log / browser観測および
Architecture Owner承認へ双方向に追跡できる。主要ADRとProject Overview / Business Feature Skillも、
Phase 3実装およびP3-C3延期境界と一致する。

唯一の未充足DoDは、計画どおりRemote Gate / Gate Cへ継続するDoD 3-11の実CI PASSである。local critical E2Eの
3回連続PASSはCI候補の受入Evidenceであり、実workflow PASSへ読み替えない。C4-2を`COMPLETE`とし、次はC4-3として
Public API、artifact / publish unit、dependency、migration / table、property / profile、route、Tooling混入、
deferred inventoryおよびEngineer-facing Journeyを確定する。P3-C4 close、Remote GateまたはGate Cはまだ宣言しない。

## 20. C4-3 inventory method

C4-3ではPhase 3開始baselineからC4-2完了HEADまでを対象に、Root POM、BOM、全Phase 3変更POM、
Public API inventory、production properties、Security configuration、Controller mapping、Flyway SQLおよび
3つのReference verification POM / READMEを実体から照合した。生成済み`target`、IDE表示または文書上の予定値は
inventoryへ含めていない。

## 21. Public API, artifact and publish inventory

| Inventory | C4-3 result | Boundary |
|---|---:|---|
| Formal Framework release projects | 15 | Root aggregator、BOM、Parent、Architecture Contract、ArchUnit Rules、9 Starter、`koiki-testing` |
| Formal Framework JAR | 12 | Architecture Contract / ArchUnit Rules、9 Starter、`koiki-testing` |
| Formal publish unit | 14 coordinates | Root aggregatorを除く2 POM + 12 JAR |
| Root Reactor projects | 16 | Formal 15 projects + `koiki-reference-app` |
| Root Reactor JAR | 13 | Formal 12 JAR + Reference executable JAR |
| Phase 3 formal artifact addition | 1 | `koiki-starter-web-mvc` |
| Java Public API inventory diff | 0 | baselineと`build-support/api-compatibility/public-api.txt`が同一 |
| Web MVC Starter Java Public API | 0 types | internal auto-configurationとresource contractだけを提供 |
| Phase 3 Tooling in Root Reactor / publish unit | 0 | local demo、browser、API、E2EはRoot外・非配布 |

`public-api.txt`のSHA-256は`9B2B26920B8B69B61C656F6A22438218822FA5CB41A707942D2CF5F806542FEA`であり、
Phase 3 baselineとの差分は0である。Reference ApplicationはRoot Reactorでbuildするが正式Framework publish unitではない。

## 22. Dependency inventory

| Owner / module | Phase 3 dependency change | Disposition |
|---|---|---|
| BOM | `koiki-starter-web-mvc`座標、`org.webjars.npm:htmx.org:2.0.10`を管理 | P3-B0 / B2承認済み |
| Web MVC Starter | Spring Boot Web MVC、Thymeleaf、Validation、HTMX WebJar、JSpecify | Spring標準中心の正式Starter。testはBoot Starter Testだけ |
| Reference | 直接Web MVC / ThymeleafをWeb MVC Starterへ置換し、`koiki-starter-api`、Spring Cache、Caffeineを追加 | REST、表示専用30秒local cache、MVC resource contractの承認範囲 |
| Reference test | `koiki-testing`をtest scopeへ追加 | 正式test artifactの承認済み利用 |
| Browser Tooling | Playwright / JUnitをtest scopeだけで利用 | Root外・非配布 |
| API Tooling | JUnit、Testcontainers PostgreSQL、PostgreSQL Driver、OAuth2 JOSEをtest scopeだけで利用 | Root外・非配布 |
| E2E Tooling | Playwright、JUnit、Testcontainers PostgreSQL、PostgreSQL Driver、OAuth2 JOSEをtest scopeだけで利用 | Root外・非配布 |

MyBatisはbaseline BOMでversion管理されるだけで、Reference、Framework StarterまたはPhase 3 Toolingのdependencyには
追加されていない。Spring Modulith runtime、Redis、WebFlux、SPA、SAML、Oracle、AWS固有libraryおよび
`htmx-spring-boot`の追加も0である。

## 23. Migration and table inventory

| Owner | Location / history | Phase 3 result |
|---|---|---|
| Framework | `classpath:db/migration/koiki` / `koiki_flyway_history` | baselineからmigration差分0 |
| Reference | `classpath:db/migration/kkref` / `kkref_flyway_history` | V1〜V3、3 files、6 tables |

Reference tableは`kkref_department`、`kkref_expense_category`、`kkref_user_department_assignment`、
`kkref_expense_request`、`kkref_expense_line`、`kkref_expense_approver_scope`の6つである。DB-level FKは
master内のassignment→departmentとexpense内のline→requestだけであり、master / expense間または
Reference / Framework間のFKはない。identity user ID、department ID、expense category IDを跨ぐ参照整合は
承認済みApplication / event / current-value contractで扱い、他owner tableの更新Ownershipを移動していない。
production seedとTooling fixture SQLのmigration混入は0である。

## 24. Property and profile inventory

| Configuration | Keys / behavior | Boundary |
|---|---|---|
| Base Reference | API path-segment versioning 2 keys、Caffeine 3 keys、Reference Flyway 4 keys | `application.properties`へ9 keys追加。既存OSIV / Identity設定を維持 |
| `api-bearer` profile | activation、enabled、issuer、audienceの4 keys | P3-C1 Reference限定。issuerは環境変数必須、audienceだけ既定値あり |
| Web MVC Starter | KOIKI独自property 0 | resource / internal auto-configuration contractだけ |
| Framework property / profile | Phase 3追加0 | Phase 2のdefault deny、Session、Identity、Audit契約を維持 |

通常profileではSession MVC chainを使用し、`/api/**`はBearer chainが未生成のためdefault deny側へ残る。
`api-bearer`を有効にすると最優先のstateless `/api/**` chainが追加され、Session MVC chain自体は無効化されない。
したがって同一processで技術的には共存するが、P3-C1の受入はBearer API focused Toolingと通常profile MVC手動確認を
分離しており、両経路同時利用を新しい正式profile契約として固定していない。

## 25. Route inventory

Phase 3でReference productionへ追加したController method mappingは28である。

| Route group | Methods | Representative path / behavior |
|---|---:|---|
| Home | 1 | `GET /` |
| master MVC | 8 | department / expense categoryのlist、create、rename、deactivate |
| expense MVC | 16 | applicant / approver / accountingのlist / detail、create / edit、7状態操作 |
| expense REST | 3 | `POST /api/v1/expense-requests`、`GET /api/v1/expense-requests/{id}`、`POST .../{id}/submit` |

Phase 2 identity管理5 mappingとform login / logoutは既存経路を再利用する。Static resourceは
`/koiki-web/**`とHTMX WebJar、Template共通部品は`templates/koiki/fragments.html`であり、業務routeではない。
test route、issuer route、failure switch、seed endpointまたはactuator公開追加は0である。APIはversion 1だけを受理し、
noncanonical / unsupported versionをController testで拒否する。

## 26. Tooling containment inventory

| Tooling | Root module | Dependency scope | Production artifact inclusion | Cleanup / secret boundary |
|---|---|---|---|---|
| `reference-local-demo` | No | PowerShell only | No | disposable DBだけ。random passwordをterminal外へ保存しない |
| `reference-browser-verification` | No | Playwright / JUnit test | No | 外部起動済みApplicationを使用しBrowserContextをclose |
| `reference-api-verification` | No | 全dependency test | No | issuer / key / token / user / DBをprocess内生成してcleanup |
| `reference-e2e-verification` | No | 全dependency test | No | browser、issuer、JAR process、DB、port、temp logを成功・失敗ともcleanup |

3つのMaven Toolingは`org.koikifw.buildsupport` groupであり、正式`org.koikifw` publish unitへ含まれない。
Reference JARを外部processとして検証するだけで、fixture class / resourceをReference classpathへ注入しない。
Root Reactor、BOM、`koiki-testing`、Project Template、workflowおよびCustomer dependencyへの混入は0である。

## 27. Final deferred / pending inventory before local verification

| Item | Classification / owner | Preserved boundary | Reopen / completion trigger |
|---|---|---|---|
| DoD 3-11 actual CI PASS | Pending / Remote Gate・Gate C | workflow / required check / remote mutation 0 | Ownerがworkflowを個別承認し、実CI PASS Evidenceを取得 |
| P3-C3 MyBatis detail rules | Deferred by decision / Architecture | Rule 8拒否、`SEPARATED` / Rule 25〜27 / 30〜37 / fixture / dependency 0 | SQL指向更新、変更不能schema / SQL移行、JPAで満たせない計測要件、またはPhase 4 accounting判断 |
| React / Next.js、SSO、Access / Refresh Token | Deferred by phase / Phase 4 | P3-C1 Bearer fixtureを正式認証基盤へ昇格しない | Phase 4 Profile S blocking review |
| accounting拡張、notification、Level 2、async event | Deferred by phase / Phase 4 | 現在の同期Level 1とReference scopeを維持 | Phase 4開始判断と個別設計 |
| Project Template、正式Upgrade / Migration Guide / OpenRewrite recipe | Deferred by phase / Phase 5 | Reference / ToolingをCustomer成果物へ自動昇格しない | 対応Phase / Gate |
| Authorization Server、SAML、Redis、WebFlux、Oracle、AWS Adapter | Optional / future owner | dependency、profile、migration、image追加0 | 明示Customer要件と対応Gate |
| 申請者email表示の属性Ownership | Nonblocking observation / future review | Framework Identityへ業務属性を追加しない | 独立したIdentity / business profile設計判断 |
| 全画面・全Role・全支援技術accessibility certification | Nonblocking observation / future review | Gate Bの限定keyboard / Narrator Evidenceを過大表現しない | certification scopeと担当を明示した別計画 |
| AC-P3-06 / 07 strict concurrent race | Explicitly outside accepted AC / future production design | 現在の同期event / transaction Evidenceを維持 | production保証要件化時のlock / isolation review |

deferred itemは欠落を隠すために削除せず、同時にPhase 3必須DoD / ACへ追加してGate Cを不要にblockしない。

## 28. Engineer-facing Journey closeout

`docs/reference/README.md`をPhase 3 Referenceの入口として、業務仕様、通常Session MVC手動journey、
browser focused、Bearer API focused、critical E2EおよびPhase 2 Security developer journeyを目的別に接続した。
Local Run Guideの対象をP3-B2からPhase 3完成形の通常Session MVCへ更新し、`api-bearer`を有効化しない境界を明記した。
`build-support/README.md`には分散していたlocal demoとAPI Toolingの入口を追加した。

各Tooling READMEをcommand、fixture、secret、cleanupの正本として維持したため、新しい
`phase3-developer-journey.md`は作成しない。profile、route、fixture、credentialまたはproduction設定の変更もない。

## 29. C4-3 conclusion

Public API、formal artifact / publish unit、Root Reactor、dependency、migration / table、property / profile、routeおよび
Tooling containmentは、P3-B0〜C3の承認済み境界と一致する。未承認のFramework / Reference production差分、
Phase 4成果物、Tooling昇格またはdeferred itemの消失は認めない。Engineer-facing Journeyのdocumentation gapも
既存文書の責務を重複させず補正した。

C4-3を`COMPLETE`とし、次はC4-4でRoot Reactor clean verify、Public API compatibility、package済みReference JAR、
API focused Toolingおよびcritical E2E Toolingを実行し、cleanupと非露出を確認する。実CI PASS、workflow、remote操作、
Gate CおよびPhase 4は引き続き開始しない。

## 30. C4-4 execution baseline

2026年9月16日、C4-3 commit `b60f403`のclean worktreeからC4-4を開始した。Java 21.0.12.1、
Maven Wrapper 3.9.16およびDocker Engineを使用し、production source、dependency、migration、property、profile、
routeまたはworkflowを変更していない。

## 31. Aggregate and packaged verification result

| Verification | Result | Evidence |
|---|---|---|
| Root Reactor `clean verify` | PASS | 16 / 16 projects、170 tests、failure / error / skip 0、Reference 99 tests、1分24秒 |
| Public API timestamped baseline identity | PASS | Architecture Contract / ArchUnit Rulesの`0.1.0-20260826.091429-1`を取得し、固定SHA-256が両方MATCH |
| Public API inventory | PASS | 5 public types、4 annotation elements、2 Rules methodsが`public-api.txt`とMATCH |
| japicmp 0.26.1 | PASS | Architecture Contract / ArchUnit Rulesとも`access=public`、`modifications=NONE`、exit 0 |
| Public API fixture | PASS | package-private変更を許容し、public return type破壊と未承認public追加を期待failureとして検出 |
| package済みBearer API focused | PASS | 1 test、failure / error / skip 0、12.11秒 |
| package済みcritical E2E | PASS | 1 test、failure / error / skip 0、15.81秒 |

Public API本比較には`read:packages`だけを持つPAT classicをsecure promptから供給した。scriptがscopeを検査し、
baseline JAR、隔離Maven repository、比較reportおよびtoken参照を`finally`で破棄した。PAT値をEvidence、Repository、
command lineまたは共有出力へ保存していない。

API focusedは実署名Bearerでcreate / read / submit、PostgreSQL state / version / line totalとBusiness Audit、
認証・scope・stale version・inactive masterの拒否およびtoken非出力を検証した。critical E2Eは同じpackage済みJARで
Bearer API、Session Chromium、HTMX検索 / 承認、DB / Business / Security Auditおよびprocess logを一連で突合した。

## 32. Root test count correction

C4-4のclean実行とSurefire XML再集計では、Root Reactorのtest内訳は次のとおりである。

| Module | Tests |
|---|---:|
| `koiki-architecture-contract` | 4 |
| `koiki-archunit-rules` | 67 |
| `koiki-reference-app` | 99 |
| **Total** | **170** |

P3-C1 / C2 Evidenceと実行計画に記録していた217 testsは、同じ実装から再現できない集計誤りであった。
P3-C2実装commitからC4-3 commitまでRoot test sourceの変更は0であり、test削除またはcoverage縮小による差ではない。
本C4-4でC1 / C2 Evidenceと実行計画の該当値を170へ訂正する。各testのPASS、Reference 99 tests、
focused Tooling結果およびOwnerの機能受入判断は変更しない。

## 33. Cleanup and non-disclosure result

| Check | Result |
|---|---|
| Reference JAR / issuer / journey process | 残存0 |
| PostgreSQL 17 Testcontainers | 残存0 |
| Public API temporary directory / report / isolated repository | 残存0 |
| E2E dynamic port | test内で再bind可能を確認 |
| Bearer token / password / Cookie / private key / source HMAC key | process log非出力assertion PASS |
| SQL statement / stack trace | critical E2E process log非出力assertion PASS |
| Worktree before Evidence update | clean |
| Remote mutation / workflow / required check / publish | 0 |

Docker上にC4-4以前から存在する別用途のPostgreSQL 15 container 2件は、C4-4の所有物ではないため停止・変更していない。

## 34. C4-4 conclusion and close review input

Root aggregate、timestamped Public API baseline、inventory / japicmp、package済みBearer APIおよびcritical E2Eは
すべてPASSした。cleanup、secret、SQLおよびstack traceの非露出境界も満たし、production不足または
P3-C4から所有CPへ戻すblocking findingは認めない。Root test総数の誤記だけをEvidence上で明示訂正した。

C4-1〜C4-4を`COMPLETE`とし、P3-C4をArchitecture Owner close reviewへ送る。Owner reviewでは次を判断する。

1. DoD 3-1〜3-10、AC-P3-01〜10、ADR / Skill traceを受け入れる。
2. inventory、deferred分類およびEngineer-facing Journeyを受け入れる。
3. C4-4の実検証と170 testsへの集計訂正を受け入れる。
4. DoD 3-11だけを`PENDING CI EVIDENCE`としてRemote Gate / Gate Cへ継続する。
5. P3-C4を`COMPLETE / OWNER APPROVED`として閉じてよいか判断する。

本節作成時点ではOwner承認前であり、P3-C4 close、Remote Gate、Gate C、workflow変更、remote操作または
Phase 4を開始しない境界としていた。2026年9月17日の補足検証とOwner判断は次節に記録する。

## 35. Browser focused supplemental verification and Owner close decision

2026年9月17日JST 00時台のbrowser focused再実行で、3 tests中2件がPASSし、validation accessibility testだけが
`#usage-date-error`待機でtimeoutした。test fixtureが`UTC current date + 1 day`を入力していたため、この時間帯には
JST当日となり、未来日validationが成立しないことを原因と確認した。これはReference productionの業務日付判定ではなく、
非配布browser Toolingのタイムゾーン境界不具合である。

`ReferenceHtmxJourneyTest`の入力を`UTC current date + 2 days`へ補正した。UTC-12〜UTC+14の範囲で
application local dateより確実に未来日となり、production code、Public API、dependency、migration、property、
profile、routeまたはworkflowは変更していない。

同じ通常Session profileのpackage済みReference JAR、使い捨てPostgreSQL 17および実Chromiumで再実行した。

| Supplemental check | Result |
|---|---|
| Browser focused | 3 tests、failure / error / skip 0、6.575秒 |
| Master HTMX / history / CSRF | PASS |
| Expense validation accessibility | PASS |
| Two independent browser contexts / optimistic lock | PASS |
| Conflict target DB state | `APPROVED` / version 2 |
| Cleanup | 専用container残存0、18080 / 55432 port解放 |
| Worktree verification | `git diff --check` PASS |

Architecture Ownerはclose reviewの5判断点を次のとおり承認した。

1. DoD 3-1〜3-10、AC-P3-01〜10、ADR / Skill traceを受け入れる。
2. inventory、deferred分類およびEngineer-facing Journeyを受け入れる。
3. C4-4の実検証と170 testsへの集計訂正を受け入れる。
4. DoD 3-11だけを`PENDING CI EVIDENCE`としてRemote Gate / Gate Cへ継続する。
5. P3-C4を`COMPLETE / OWNER APPROVED`として閉じる。

**Decision:** APPROVED — P3-C4 COMPLETE / OWNER APPROVED

**Decided by:** Shuichi Kataoka, Architecture Owner

**Decision date:** 2026年9月17日

**Preserved boundary:** production、Public API、migration、dependency、workflowおよびremote変更0

**Next decision point:** Remote Gate。workflow、required check、push / PR / merge、snapshot publishまたは
Gate C開始には個別Owner承認を必要とする。

P3-C4完了はPhase 3 final acceptanceまたはGate C通過を意味しない。Phase 4を開始せず、DoD 3-11の
実CI PASSとRemote Gateの個別判断を先に扱う。
