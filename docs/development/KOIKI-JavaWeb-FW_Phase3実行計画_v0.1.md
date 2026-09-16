# KOIKI-JavaWeb-FW Phase 3 Reference Vertical Slice 実行計画

**状態:** GATE P3-1 APPROVED / P3-CP0 COMPLETE / P3-A0 COMPLETE / P3-A1〜A4 COMPLETE / ACCEPTED / GATE A PASSED / P3-B0 COMPLETE / OWNER APPROVED / P3-B1 COMPLETE / P3-B2〜B4 COMPLETE / OWNER APPROVED / GATE B PASSED / MILESTONE B COMPLETE / ACCEPTED / P3-C0〜C2 COMPLETE / OWNER APPROVED / P3-C3 READY
**作成日:** 2026年9月13日
**開始作業branch:** feature/phase3-reference-vertical-slice
**開始基準main:** c88b335efdd556613c9ef7f4c5267214fdb8254b
**Phase 2 published artifact baseline:** af7b4f71d885fe4991e5fcf85fcf8888aec6a539
**Architecture Owner:** Shuichi Kataoka

## 1. Purpose and current boundary

Phase 3では、Phase 0、Phase 1a Build Foundation、Phase 1b Runtime Foundationおよび
Phase 2 Security FoundationのCOMPLETE / ACCEPTED baselineを維持し、Reference Applicationの
master（Tier 1）とexpense（Tier 2）を中心とする正式な業務Vertical Sliceを実証する。

主な実証対象は次のとおりである。

- Reference master（Tier 1 SIMPLE / JPA）
- Reference expense（Tier 2 RICH / JPA共有モデル）
- masterからexpenseへの同期Domain Eventとtransaction rollback
- Spring Modulith Level 1
- Spring MVC / Thymeleaf / HTMX
- JPA射影とJdbcClientによるread model
- 楽観lock、Business Audit、TTL cache
- MVCと同じApplication Use Caseを使う最小REST API
- API、自動browser操作、実browserでの目視・手動操作、log / Audit / DB突合を組み合わせた検証

Gate P3-1のArchitecture Owner承認前に、production code、Public API、Maven module、
production migration、workflow、dependency、remote設定または配布単位を変更しない。

AGENTS.mdは開始前にはPhase 2の履歴的baselineを維持し、本計画とGate P3-1の承認後、
P3-CP0でPhase 3向けの薄い導線へ更新した。P2-A1を再開しない。

## 2. Authoritative inputs

| 確認事項 | 正本 |
|---|---|
| 全体方針、Phase 3成果物、DoD 3-1〜3-11 | ../architecture/grand-design/KOIKI-JavaWeb-FW_グランドデザイン_v0.2.md |
| Repository / Maven / Ownership | ../architecture/KOIKI-JavaWeb-FW_Repository_Architecture_v0.1.md |
| 基準versionとsupport状態 | ../architecture/KOIKI-JavaWeb-FW_Baseline_Compatibility_v0.1.md |
| Phase 2 Securityの承認境界 | KOIKI-JavaWeb-FW_Phase2実行計画_v0.1.md |
| Securityを伴う業務applicationの入口 | phase2-developer-journey.md |
| Reference業務仕様、状態、不変条件、権限、AC-P3-01〜10 | ../reference/KOIKI-JavaWeb-FW_Reference_Application_Specification_v0.1.md |
| Phase 3規模と内部milestone | ../architecture/KOIKI-JavaWeb-FW_Phase_Estimate_Feasibility_v0.1.md |
| 個別の設計判断 | ../architecture/adr/ |
| 実装で得た証拠 | ../architecture/validation/ |

Repositoryに採用済みOpenSpec changeは現在存在しない。Phase 3でOpenSpecを使用する場合も、
Maven build、CI、Consumerまたは成果物の必須前提にしない。

## 3. P3-F0 pre-start inventory

### 3.1 Confirmed baseline

- main / origin/mainはc88b335efdd556613c9ef7f4c5267214fdb8254bで一致し、開始時のworktreeはcleanである。
- Phase 2の正式Framework release unitは14 projects / 11 JAR、publish unitはRoot aggregatorを除く13座標である。
- Phase 2 Public APIは24型、Framework migrationは3件 / 11 tableで固定済みである。
- Phase 2 internal snapshotは正式release、一般公開またはCustomer support付き配布ではない。
- Reference Applicationは単一Maven moduleであり、現在の業務moduleはidentityだけである。
- master、expense、Reference migration、HTMX、REST APIおよびkoiki-starter-web-mvcは未実装である。
- Spring Modulith 2.1.1はLevel 0 test scopeだけで使用中である。
- PersistenceModelの現行Public APIはSHAREDだけである。

### 3.2 Work positioning

    Phase / status: Phase 3 / P3-CP0 COMPLETE / P3-A0 COMPLETE / P3-A1〜A4 COMPLETE / ACCEPTED / GATE A PASSED / P3-B0 COMPLETE / OWNER APPROVED / P3-B1 COMPLETE / P3-B2〜B4 COMPLETE / OWNER APPROVED / GATE B PASSED / MILESTONE B COMPLETE / ACCEPTED / P3-C0〜C2 COMPLETE / OWNER APPROVED / P3-C3 READY
    Primary ownership: Reference
    Target Maven module: koiki-reference-app
    Business modules: master / expense
    Framework candidates: web-mvc integration, architecture rules, dependency management
    Tooling candidates: browser / package / PostgreSQL / E2E harness
    Deferred: Phase 4 / 5 and optional Gate scope

## 4. Gate P3-1 approved decisions and staged decisions

Gate P3-1はPhase 3の目的、境界、実行順序および次の判断を承認する。後続CPへ明示的に
配置した判断は「未決のまま実装してよい」という意味ではなく、当該CPのproduction変更前に
Owner reviewで確定する停止点として承認する。

| # | Decision | Gate P3-1 status | Blocking review |
|---:|---|---|---|
| 1 | c88b335efdd556613c9ef7f4c5267214fdb8254bをPhase 3開始baselineとする | APPROVED | baseline変更時はGate再審査 |
| 2 | DoD 3-1〜3-11とReference AC-P3-01〜10を必須受入範囲とする | APPROVED | scope変更時はGate再審査 |
| 3 | masterはTier 1 SIMPLE / JPA、expenseはTier 2 RICH / JPA共有モデルとする | APPROVED | Tier変更時はGate再審査 |
| 4 | masterとexpenseは単一koiki-reference-app内の業務packageとし、別artifactにしない | APPROVED | artifact分割時はGate再審査 |
| 5 | 有効な部門・経費科目をexpenseが確認するmodule間契約を定義する | APPROVED | P3-A0-D1 COMPLETE。master-ownedの狭い同期read-only contractとexpense Port / Adapterを使用する |
| 6 | 承認者の部門scope割当はReferenceが所有し、Framework Identityへ混入させない | APPROVED | P3-A0-D2 COMPLETE。`expense`所有tableと完全一致scopeを使用する |
| 7 | DepartmentDeactivatingは同期Eventとし、Level 1期間はtransactional / async eventを禁止する | APPROVED | Level変更時はGate再審査 |
| 8 | koiki-starter-web-mvcを正式Framework artifactにするか、Reference実証に留めるかを判断する | APPROVED | P3-B0-D1 COMPLETE。P3-B2で実責務と同時に正式artifactを新設する |
| 9 | HTMX連携library、依存範囲、asset配布方式とJavaScript無効時のfallbackを判断する | APPROVED | P3-B0-D3〜D5 COMPLETE。2.0.10 WebJarとSpring標準＋内部fallbackを選択し、効果が明確な操作だけに適用する |
| 10 | 最小REST APIのendpoint、DTO、Permission、status、error code、optimistic lock契約を確定する | APPROVED | P3-C0-D1〜D13 COMPLETE / OWNER APPROVED。P3-C1は承認済み契約内で実装する |
| 11 | Reference migration version、history table、table / index / constraint、module間FKを確定する | APPROVED | P3-A0-D3 / D4 COMPLETE。V1〜V3、module内FKのみ、module間 / Framework FKなし |
| 12 | masterのJPA class-based射影はTier 1のApplication DTOとして扱う | APPROVED | P3-B1でpackage / namingをEvidence化する |
| 13 | cache対象、TTL、複数instance間で許容するstalenessを確定する | STAGED DECISION APPROVED | P3-B4のcache実装より前に確定する |
| 14 | API / 自動testを回帰の主軸とし、操作面が成立するP3-B2以降で実browser、人系checkpoint、log / Audit / DB突合を併用する | APPROVED | Gate Aのbrowser確認は操作面がある場合のみ。runnerとCI required化はP3-B0 / C2 / Remote Gateで確定する |
| 15 | MyBatis fixtureに必要なPublic API変更は型単位で再reviewし、fixture都合で先行追加しない | APPROVED | P3-C3。承認前はPublic API変更0 |
| 16 | workflow、required check、snapshot publish、remote environment変更はlocal closeout後の別Gateで承認する | APPROVED | Remote Gate |

個別のPublic API、property名、migration SQL、UI component、外部libraryおよびremote設定は、
上表のblocking reviewと対応CPのEvidenceなしには確定・変更しない。

## 5. Ownership and module plan

| Area | Owner | Boundary |
|---|---|---|
| master | Reference | 部門、経費科目、Reference table / migration、MVC / HTMX |
| expense | Reference | 経費申請、状態遷移、不変条件、query、MVC / REST |
| Security defaults | Framework | Phase 2 default deny、CSRF / Header、Session / Bearer境界を再利用 |
| Identity | Framework / Reference | Framework Public contractとReference-owned UI。業務属性をFramework tableへ追加しない |
| Audit | Framework | Business Auditを業務transaction内で使用。Reference独自audit tableを作らない |
| Session | Framework | Spring Session JDBC、logout、失効、cleanup契約を再利用 |
| MVC / HTMX共通契約 | Framework | P3-B0承認済み。StarterはP3-B2で実責務と同時に追加する |
| Browser / E2E harness | Tooling | 非配布。Reference、Project Template、koiki-testingへ自動昇格しない |
| Customer route / policy / migration | Customer | 本PhaseではCustomer実装を作成しない |

### 5.1 master

    Ownership / module: Reference / master
    Tier: Tier 1 SIMPLE
    Persistence: JPA
    Business rules: Application Use Case
    Read path: JPA class-based projection -> application.dto
    Public boundary: domain.event only when required
    View: Reference-owned MVC / HTMX Form and View DTO

masterは専用domain.model、domain.repository、未使用Portを作成しない。
DepartmentDeactivatingは識別子と必要な値だけを持つ不変recordとする。

### 5.2 expense

    Ownership / module: Reference / expense
    Tier: Tier 2 RICH
    Persistence / model: JPA / SHARED
    Domain: ExpenseRequest, ExpenseLine, Money, ExpenseStatus
    Read model: application.query-owned record, JdbcClient materialization
    Public boundary: domain.event only
    View / API: separate MVC and REST adapters sharing Application Use Cases

6状態とTR-01〜TR-07を対象とする。不変条件と状態遷移はDomain Model、Permission、
resource所有権、部門scopeと処理順序はApplication Use Caseが所有する。

## 6. View and API boundaries

### 6.1 MVC / Thymeleaf

- MVC Controllerはrequest受付、形式検証、Use Case呼出、HTTP / Model整形に限定する。
- Domain Model / JPA EntityをController、Form、ModelまたはTemplateへ渡さない。
- 更新系はForm -> Command -> Use Case -> Domainとする。
- 参照系はtransaction内で完全にmaterializeしたDTO / read modelを使用する。
- MVC pathにAPI versioningを適用しない。
- Validation、Flash Message、PRG、403 / 404 / 500と競合画面を実際に描画して確認する。

### 6.2 HTMX

server-side UIの主軸はSpring MVC / Thymeleafが生成するHTMLとする。HTMXはその土台を置換せず、
全画面遷移を避ける効果、更新DOM、history / focus / error挙動、同一Use Caseの再利用およびbrowserでの
検証可能性を説明できる箇所だけに採用する。Phase 3ではmaster一覧の検索・paging・部分更新を必須採用箇所とし、
他の画面を契約実証のためだけにHTMX化しない。

HTMXを採用したinteractionはJavaScript有効を前提とし、次の11契約を実証する。11項目は共通契約の
coverageであり、全画面または各操作へすべてのHTMX機能を適用することを意味しない。

1. 全画面とfragmentの描画分離
2. CSRF header自動付与
3. Validation errorの部分描画
4. HX-Redirect / HX-Location
5. HX-Push-Url
6. Out-of-Band swap
7. loading表示
8. 検索入力のdebounce
9. 動的contentの再初期化
10. 403 / 404 / 500の部分request時の扱い
11. JavaScript有効前提とaccessibilityの両立

### 6.3 Minimum REST API proposal

RESTはexpense.adapter.inbound.apiが所有し、MVC Controllerとクラスを共通化しない。
Gate P3-1でP3-C0への検討入力とした最小提案は次の3 endpointとする。

| Method / path | Purpose |
|---|---|
| GET /api/v1/expense-requests/{id} | 認可済み詳細参照とversioned response |
| POST /api/v1/expense-requests | JSON / Validation / 不変条件 / Problem Details |
| POST /api/v1/expense-requests/{id}/submit | MVCと同じ提出Use Caseと楽観lock |

全Use CaseをRESTへ露出しない。承認・却下・差戻し・精算を追加する場合は、
最小境界の変更としてOwner reviewする。

## 7. Spring Modulith Level 1 boundary

- 追加runtime dependencyを必要としない。
- ApplicationEventPublisherと同期EventListenerを使用する。
- Listenerはadapter.inbound.eventに配置し、自moduleのApplication Use Caseへ委譲する。
- masterはexpenseのApplication、Domain、Repository、Adapterを参照しない。
- command整合は公開event契約を使う。current-valueの有効master確認だけは、master-ownedの狭い同期read-only module contractを明示例外として使う。
- expenseは自module Portとoutbound Adapterを介してこのcontractへ接続し、masterのApplication、Domain、Repository、Adapterまたは所有tableを直接参照しない。
- ただしP3-B1の表示専用read modelは、ADR-038 P3-B1 fittingに従い、scopeをSQL内で先に強制したread-only JOINからApplication所有の最終recordを直接materializeできる。この例外を有効性判定、更新、認可判断またはDomain復元に流用しない。
- TransactionalEventListener、ApplicationModuleListenerと非同期処理は導入しない。
- 未処理4状態で部門廃止を拒否し、終端2状態で許可する。
- 同期listener件数を記録し、境界の形骸化をArchitecture Review対象とする。

## 8. Phase 2 reuse boundary

| Capability | Phase 3 use | Do not do |
|---|---|---|
| Security | default denyとCSRF / Headerを維持し、MVC / API matcherを明示する | fallback permit、matcher gap |
| Identity | FrameworkPrincipal、immutable user ID、PermissionをPublic API経由で使う | Framework Entity / Repository参照、業務属性のFramework化 |
| Audit | Business Auditを業務transaction内で記録する | Security Auditへの誤分類、Reference独自audit table |
| Session JDBC | browser login、2 Session競合、logoutを再利用する | 新Session store、Redis、Web内cleanup |
| Migration | 3 migration / 11 tableを不変baselineとする | Framework SQLのReference migrationへのcopy |

ReferenceのRole / Permission code、部門scope、初期dataはReferenceが所有する。test user、
固定password、test key、failure switchまたはtest-only routeをproduction artifactへ含めない。

## 9. Milestones, commit points and Gates

### Pre-implementation P3-F0

| CP | Scope | Exit criteria |
|---:|---|---|
| P3-F0 | 開始前棚卸し、本計画、Gate判断案 | production変更0、baseline、Ownership、DoD、deferred、Owner判断点が明示される |
| Gate P3-1 | Phase 3 start approval | 本計画、§4の判断、実行順、remote境界をOwnerが承認 |
| P3-CP0 | 承認記録、AGENTS.md、start Evidence | Phase 3導線が承認内容と一致し、production変更0 |

### Milestone A — Business core / Level 1

| CP | Scope | Exit criteria |
|---:|---|---|
| P3-A0 | master / expense連携、部門scope Ownership、migration contract review | Ownerが契約、所有module / table、FK方針を承認し、production変更0 |
| P3-A1 | master contract、Tier 1、Reference migration、管理Use Case | AC-P3-01、拒否経路、Business Audit |
| P3-A2 | expense Domain / JPA共有モデル / Use Case | 6状態、7遷移、INV-EXP-01〜04、成功・拒否・rollback |
| P3-A3 | Permission、resource所有権、部門scope、Business Audit | AC-P3-02〜05、scope外非露出、状態とauditの原子性 |
| P3-A4 | DepartmentDeactivating、同期listener、Level 1 | AC-P3-06 / 07、未処理4状態のrollback、終端2状態の成功、直接参照0 |
| Gate A | Domain / transaction / module acceptance | 自動test、package / DB観測、log / Audit / DB突合。実browserは操作面がある場合のみ |

### Milestone B — Read model / MVC / HTMX

| CP | Scope | Exit criteria |
|---:|---|---|
| P3-B0 | MVC / HTMX artifact、dependency、asset、browser runner contract review | OwnerがFramework / Reference境界と採用方式を承認し、先行dependency変更0 |
| P3-B1 | master JPA射影、expense JdbcClient query | DoD 3-7、query時点でscopeを強制し、取得後filterを行わない |
| P3-B2 | MVC / Thymeleaf / Form / View DTO | Entity露出0、OSIV無効、全画面描画、Validation、error画面 |
| P3-B3 | HTMX 11契約とCSRF | 検索・paging・部分更新、CSRF注入、部分error、browser確認 |
| P3-B4 | 楽観lock競合画面、cache / TTL | AC-P3-08、2 browser Sessionの後発拒否、TTL後の再読込 |
| Gate B | Web / query acceptance | 自動browser journey、Owner実演、accessibility、log / Audit / DB突合 |

### Milestone C — REST / E2E / closeout

| CP | Scope | Exit criteria |
|---:|---|---|
| P3-C0 | 最小REST API contract review | Ownerがendpoint、DTO、Permission、status、error、lock契約を承認し、先行REST実装0 |
| P3-C1 | 最小REST API、Jackson 3、/api/v1、Problem Details | DoD 3-10、AC-P3-10、MVCと同じ認可・業務結果 |
| P3-C2 | critical journey E2E、package済みJAR、CI候補 | DoD 3-11のCI実行候補、browser / API / DB / log aggregateとcleanup。実CI通過はRemote Gate / Gate Cで確定 |
| P3-C3 | MyBatis規約fixture、Rule 35〜37 | 非配布Toolingでconverter、MybatisTest、楽観lock、reconstitute境界を実証 |
| P3-C4 | Journey / ADR / Skill / DoD trace | DoD、AC、Public API、migration、dependency、deferred inventory一致 |
| Gate C | Phase 3 final acceptance | clean HEAD、local aggregate、Remote Gate承認済みDoD 3-11 CI PASS、checks、実browser実演、Owner承認 |

### Remote Gate

workflow追加、required check変更、protected environment、push / PR / merge、snapshot publishは、
local Evidenceと実行時間・flakiness・cleanup確認後に個別承認する。
Gate P3-1はremote mutationを一括許可しない。
DoD 3-11の最終充足には、Remote Gateで承認したworkflowによるcritical journey E2EのPASS Evidenceを必要とする。
P3-C2のlocal CI候補受入だけで「CIで通る」を完了扱いにせず、この結果をGate C final acceptanceへ入力する。

## 10. Hybrid verification strategy

### 10.1 Principle

自動testとAPI呼び出しを主軸とし、実装が進んだ段階で無理のない範囲から実browserを使った
目視・手動操作と背後のlog / Audit / DB確認を追加する。人系確認を自動testの代替にせず、
表示品質、操作感、部分描画、focus、accessibility、途中失敗時の利用者体験を補完する。

    Browser / API operation
      -> HTTP response and rendered state
        -> Application / Security log
          -> Business Audit
            -> persisted business state

### 10.2 Verification layers

| Layer | Main method | Purpose |
|---|---|---|
| L1 | Domain Unit / Application test | 不変条件、状態遷移、認可、処理順序を高速回帰 |
| L2 | Repository / Web Slice / RestTestClient / Testcontainers | MVC、REST、Security、PostgreSQLを自動検証 |
| L3 | Agent-driven HTTP / API journey | package済みJARを外部操作し、HTTP / DB / logを突合 |
| L4 | Automated real-browser journey | login、Form、HTMX、履歴、CSRF、競合を実browserで回帰 |
| L5 | Human visual / manual browser checkpoint | 表示、操作感、focus、accessibility、error時の体験 |
| L6 | Log / Audit / DB correlation | UI結果とbackendの事実の一致を確認 |

### 10.3 Human checkpoints

| Point | Browser operation | Correlated evidence |
|---|---|---|
| Gate A（操作可能な画面がある場合のみ） | 部門廃止の成功・拒否、申請・承認の代表操作 | rollback、Business Audit、request / trace ID、DB state |
| P3-B2 | 一覧、詳細、登録、更新、Validation、error画面 | 描画完了、Entity露出なし、sanitized log |
| P3-B3 | HTMX fragment、loading、history、Validation、CSRF拒否 | request header、response fragment、Security log |
| Gate B | 2 Sessionで同じ申請を更新 | 先行だけcommit、後発競合画面、DB version |
| P3-C1 | MVCとRESTで同じUse Caseを実行 | 認可、状態遷移、Auditの同値性 |
| Gate C | critical journey全体 | browser、API、log、Audit、DBの一連のEvidence |

Gate Aまでに業務画面が存在しない場合、実browser確認はGate条件にせず、同じscenarioを
L1〜L3 / L6で受け入れる。実browserによる必須checkpointはP3-B2から開始する。

### 10.4 Evidence record

各実browser / 手動checkpointで次を記録する。

- source commit、JDK、browser / version、profile、DB version
- Given、操作手順、期待結果、実結果
- request ID / trace ID、sanitized log、Audit、業務tableの突合結果
- PASS / FAIL、残課題、再現手順、実施者、実施日時
- 必要に応じたscreenshot / video。自動testと手順を正本とし、画像は補助証拠とする

password、token、Cookie、private key、email、個人情報または秘密値をlog、screenshot、
video、artifact、Problem DetailsまたはAudit payloadへ残さない。

### 10.5 Automation promotion

実browserで安定して再現できたscenarioはL4の自動browser journeyへ順次移す。CIでは
critical journeyだけを実行し、全画面・全権限・全拒否組合せをE2Eへ集約しない。
網羅性はL1〜L3、利用者体験はL4〜L5、事実の突合はL6で担保する。

## 11. Migration and data ownership

1. Reference migrationは`classpath:db/migration/kkref`に配置し、`kkref_flyway_history`で管理する。
2. V1はmaster所有の`kkref_department`、`kkref_expense_category`、`kkref_user_department_assignment`を作る。
3. V2はexpense所有の`kkref_expense_request`と`kkref_expense_line`を作り、V3はexpense所有の`kkref_expense_approver_scope`を作る。
4. Frameworkのkoiki_ table / migrationを修正・copyしない。
5. HibernateのDDL自動生成をproduction schemaの正本にしない。
6. clean install、再起動no-op、Phase 2 baselineとの統合、checksum、失敗経路をPostgreSQLで検証する。
7. FKは同一module内だけとし、expenseからmaster、ReferenceからFramework IdentityへのFKを作らない。production seedを投入しない。

## 12. Dependency and artifact boundary

- Spring Boot 4.1.1 baselineとBoot BOM管理を維持する。
- Spring MVC / Thymeleafは現行Reference dependencyを出発点とする。
- P3-B0承認によりHTMX 2.0.10 WebJar、Spring標準＋KOIKI内部fallback、Thymeleaf HTML主軸の選択適用を固定した。
- Spring Cache / CaffeineはP3-B4で対象とTTLを実証する範囲だけ追加する。
- Spring Modulith runtime dependencyをLevel 1の理由だけで追加しない。
- master、expense、Reference migration、Templateをformal Framework release unitへ含めない。
- web-mvc StarterはP3-B2でartifact、dependency、Public API、release / publish unitを同時に検証する。
- browser / E2E harnessは非配布Toolingとし、Customer CIへ無条件で必須化しない。

## 13. Deferred scope

- React / Next.js SPA実装とMVC / SPA併用
- Spring Modulith Level 2、Event Publication Registry、非同期Domain Event
- notification、accounting、メール、会計API
- Spring Batch、remind、月次締めjob
- File / Object Storage / SFTP
- Authorization Server、token発行 / refresh / revoke、SAML、MFA有効化
- Redis、WebFlux、Oracle、AWS / ALB / ECS固有Adapter
- MyBatisによるaccounting実装、MyBatis専用Starter
- Project Template、code generator、正式Upgrade / Migration Guide、正式OpenRewrite recipe
- 正式release、一般公開repository、Customer support付き配布
- Reference業務語彙、Form、Template、Eventまたはcache方針のFramework昇格

Phase 3末尾のMyBatis作業は、後続Phaseのaccountingに先立つ規約と非配布fixtureの実証だけとする。

## 14. Verification commands policy

実際のscript名とMaven module選択は各CPのcontract reviewで確定する。検証層は次を維持する。

- focused Maven verify
- Root Reactor verify
- Public API compatibility
- Null Safety positive / negative / restore
- PostgreSQL migration / Repository / rollback aggregate
- package済みReference JARのHTTP / DB / log journey
- agent-driven REST / MVC regression
- automated real-browser critical journey
- human visual / manual checkpointとEvidence記録
- dependency / migration / sensitive-output / cleanup inventory

## 15. Commit discipline

- 各CPは単一責務のcommitまたはreview可能な最小commit群に分ける。
- 後続CPのproduction実装を先行しない。
- 各Gateはclean HEADで再検証し、commit SHAをEvidenceに記録する。
- 意図しないPublic API、dependency、migration、table、property、routeをinventoryで検出する。
- remote実行とsnapshot publishはlocal commitから分離し、個別Owner承認前に実行しない。

## 16. Estimate and recalibration

Phase 0のFeasibility見積は直接120〜194標準人日、contingency込み156〜252標準人日、
AI支援Owner稼働78〜164日である。これを納期commitmentとして扱わない。

Gate P3-1ではこのrangeを初期planning rangeとして承認する。P3-CP0 / A0でPhase 2実績と
master / expenseのmodule契約を反映し、P3-B0でHTMX libraryとbrowser runnerを固定した。P3-C2で
CI候補のlocal実行時間を反映し、Remote Gateで実CI時間を確定して再見積する。Gate A / B / Cごとに残range、flakinessと
手動checkpoint負担を再評価する。

## 17. Stop conditions

- 承認済みGate P3-1のscopeまたはblocking reviewを飛ばしてproduction code、Public API、module、migration、dependencyまたはworkflowを変更する。
- Walking SkeletonのJava、Template、SQL、Maven座標を正式Referenceへcopyする。
- masterとexpenseを別Maven artifactにする。
- 他moduleのApplication、Domain、Repository、Adapterまたは所有tableを直接利用する。
- 有効master検証または承認者部門scopeのOwnerが未決のまま実装を始める。
- Entity / Domain ModelをForm、MVC Model、REST DTOまたはTemplateへ露出する。
- UI表示制御、取得後filterまたはJavaScriptでbackend認可を代替する。
- Level 1期間にtransactional / async eventまたは外部I/O listenerを追加する。
- Phase 2 default deny、CSRF、Security Header、Session障害semanticsまたはAudit transactionを弱める。
- test user、固定credential、failure switch、test route、secretまたはPIIをproduction artifactへ含める。
- browser目視だけを根拠に自動回帰testを省略する。
- screenshot / log / videoにsecret、token、CookieまたはPIIを残す。
- Project Template、SPA、Level 2、MyBatis accounting、Oracle、AWS固有実装またはPhase 5成果物を先行する。
- Ownerの個別承認なしにremote push / PR / merge、ruleset変更、workflow dispatchまたはsnapshot publishを行う。

## 18. Gate P3-1 Owner review record

Architecture Ownerは次をreviewし、§1〜17の実行計画と段階的な停止点を一体として承認した。

1. 開始baseline、Phase 2 published artifact baselineとPhase 3 branch identity
2. Phase 3の必須scope、DoD / AC trace、deferred scope
3. Framework / Reference / Customer / ToolingのOwnership
4. master / expenseのTier、責務、永続化、read model、module連携
5. MVC / Thymeleaf / HTMX、REST、Security、Audit、Sessionの再利用境界
6. Milestone A〜C、CP、Gate、commit discipline、remote操作境界
7. Hybrid Verification、人系checkpoint、log / Audit / DB突合とsecret保護
8. §4の16判断点と、P3-A0 / B0 / C0 / C3 / Remote Gateへ配置したblocking review

**Decision:** APPROVED — GATE P3-1 PASSED
**Subsequent status:** P3-CP0 COMPLETE / P3-A0 COMPLETE / P3-A1〜A4 COMPLETE / ACCEPTED / GATE A PASSED / P3-B0 COMPLETE / OWNER APPROVED / P3-B1 COMPLETE / P3-B2〜B4 COMPLETE / OWNER APPROVED / GATE B PASSED / MILESTONE B COMPLETE / ACCEPTED / P3-C0〜C2 COMPLETE / OWNER APPROVED
**Approved scope:** §1〜17、§4の確定判断、staged decisionの停止点、P3-CP0からGate Cまでの順序、Hybrid Verification方針
**Evidence:** 上位設計とReference仕様、Phase 2 COMPLETE / ACCEPTED baseline、開始main c88b335efdd556613c9ef7f4c5267214fdb8254b、§3のread-only棚卸し、本計画のDoD / AC trace
**Decided by:** Shuichi Kataoka, Architecture Owner
**Date:** 2026年9月13日
**Revisit trigger:** scope、Ownership、Public API、dependency、migration、Level、browser / CI方式またはremote境界の変更

Gate P3-1はP3-CP0以降を本計画の順序で進めることを承認する。ただし、各blocking reviewを
越える実装、Gate未達での次Milestone開始、remote push / PR / merge、workflow / ruleset変更、
workflow dispatchまたはsnapshot publishを許可するものではない。P3-CP0とproduction変更0の
P3-A0 contract reviewからP3-A4 Level 1同期eventおよびGate A、P3-B0 MVC / HTMX contract reviewまで
完了した。次に開始できるproduction CPはP3-B1 read modelだけであり、P3-B2以降を先行しない。
P3-A1からP3-A4の実装・検証Evidenceは、それぞれ
`docs/architecture/validation/phase3-p3-a1-master-vertical-slice.md`と
`docs/architecture/validation/phase3-p3-a2-expense-vertical-slice.md`、
`docs/architecture/validation/phase3-p3-a3-authorization-audit.md`、
`docs/architecture/validation/phase3-p3-a4-level1-synchronous-event.md`に記録する。

## 19. Gate A Architecture Owner close record

Architecture Ownerは、P3-A1〜P3-A4の横断的なOwnership、Domain、transaction、module境界、GA-D1〜D6の
判断、およびclean HEAD `85275d90139cbe8fb916ba1386288a393d189482`に対するRoot Reactor
`clean verify`の成功を確認した。

**Decision:** APPROVED — GATE A PASSED / MILESTONE A COMPLETE / ACCEPTED
**Accepted scope:** P3-A1 master、P3-A2 expense、P3-A3 authorization / Audit、P3-A4 Level 1同期event
**Verification:** Windows 11、Maven 3.9.16、Java 21.0.12.1、15 / 15 projects、128 tests、failure / error / skip 0、PostgreSQL 17.11、Framework migration 3件、Reference V1〜V3
**Evidence:** `docs/architecture/validation/phase3-gate-a-milestone-a-acceptance.md`
**Decided by:** Shuichi Kataoka, Architecture Owner
**Date:** 2026年9月13日
**Next CP:** P3-B0 MVC / HTMX contract review

Gate Aの承認はP3-B0 contract reviewの開始だけを許可する。MVC / HTMXに関するproduction変更、Maven module、
Public API、Starter、外部library、dependencyまたはbrowser runnerは、P3-B0の個別Owner承認より前に追加しない。

## 20. P3-B0 Architecture Owner decision record

Architecture Ownerは`docs/architecture/validation/phase3-p3-b0-mvc-htmx-contract-review.md`の
P3-B0-D1〜D8を一体として承認した。

**Decision:** APPROVED — P3-B0 COMPLETE / P3-B1 READY
**Approved boundary:** 正式`koiki-starter-web-mvc`、初期Java Public API 0型、限定dependency、
Spring標準＋KOIKI内部HTMX fallback、Thymeleaf HTML主軸とHTMX選択適用、Phase 2 Security再利用、
非配布Playwright Tooling、Hybrid Verification
**Evidence:** `docs/architecture/validation/phase3-p3-b0-mvc-htmx-contract-review.md`
**Decided by:** Shuichi Kataoka, Architecture Owner
**Date:** 2026年9月13日
**Next CP:** P3-B1 read model

P3-B0の承認はP3-B1の開始だけを許可する。P3-B2のMaven module / Starter / dependency変更、P3-B3の
HTMX / browser runner実装、workflow、required checkまたはremote操作を先行しない。

## 21. P3-B1 completion record

Architecture OwnerはP3-B1開始時に、表示専用read modelについて、scopeをSQL内で先に強制し、
更新・認可判断・業務不変条件・current-value検証へ流用しないread-only JOINからApplication所有の
最終recordを直接materializeする狭い例外を承認した。判断と実装Evidenceは
`docs/architecture/validation/phase3-p3-b1-read-model.md`を正本とする。

master JPA class-based射影、expense JdbcClient query、申請者・承認者・経理scope、Permission、
PostgreSQL 17およびReference全回帰を検証し、P3-B1を`COMPLETE`とする。

**Next CP:** P3-B2 MVC / Thymeleaf / Form / View DTO

P3-B3のHTMX / browser runner、P3-B4のcache / lock画面、P3-C0以降、workflowまたはremote操作を先行しない。

## 22. P3-B2 completion record

Architecture Ownerは`docs/architecture/validation/phase3-p3-b2-mvc-thymeleaf.md`のP3-B2-D1〜D6を
2026年9月14日に一体として承認した。full-page ThymeleafをUIの主軸とし、HTMX interactionと
browser runnerをP3-B3へ分離する境界を維持する。

正式`koiki-starter-web-mvc`、初期Java Public API 0型、resource contract、Reference master / expense MVC、
Form / View境界、Phase 2 Security再利用、Root Reactor 189 tests、実browser、log / Audit / DB突合および
consumerからの公開挙動testを検証し、P3-B2を`COMPLETE / OWNER APPROVED`とする。申請者email表示の
不自然さは非blocking Evidenceとし、Identityまたは業務profileの属性Ownershipを別reviewへ送る。

**Next CP:** P3-B3 HTMX 11契約 / CSRF / browser runner

P3-B4、Gate B、P3-C0以降、workflow、required checkまたはremote操作を先行しない。

## 23. P3-B3 completion record

Architecture Ownerは`docs/architecture/validation/phase3-p3-b3-htmx.md`のHTMX 11契約、選択適用範囲、
Security境界、通常HTML経路、非配布Playwright Tooling、人系checkpointおよびdeferred境界を
2026年9月14日に一体として承認した。

Root Reactor、headed browser journey、Owner目視、application log、Security AuditおよびDB突合を完了した。
実browserで検出したhistory不整合をquery fragmentへ回収し、追加目視で検出したlocal demo seedのUUID不整合は
production contractから分離して修正・再検証した。以上によりP3-B3を`COMPLETE / OWNER APPROVED`とする。

**Next CP:** P3-B4 楽観lock競合画面、cache / TTL

Gate B、P3-C0以降、workflow、required checkまたはremote操作を先行しない。

## 24. P3-B4 completion record

Architecture Ownerは`docs/architecture/validation/phase3-p3-b4-lock-cache-contract-review.md`で承認した
P3-B4-D1〜D10と、`docs/architecture/validation/phase3-p3-b4-lock-cache.md`の実装・検証Evidenceを
2026年9月14日に一体として承認した。

Reference専用409競合画面、自動retryなしの最新detail誘導、active経費科目optionだけの30秒local cache、
master管理queryの非cache境界、command current-value確認によるfail-safe、Reference / Tooling限定、
deferred scopeおよび既知のIdentity AuthenticationProvider起動WARNを非blockingとする判断を承認する。

Root Reactor 16 / 16 projects、Reference 85 tests、headed Playwright 2 tests、PostgreSQLのstate / version / Audit、
TTL前後のOwner目視、stale category command拒否とDB非更新、application logのsecret / parameter非露出を確認した。
以上によりP3-B4を`COMPLETE / OWNER APPROVED`とする。

**Next Gate:** Gate B Web / query acceptance inventory
**Start handoff:** `docs/development/phase3-gate-b-start-handoff-20260914.md`

Gate BではP3-B1〜P3-B4の一貫性、accessibility、自動browser、Owner実演、log / Audit / DB Evidenceを
横断評価する。Gate B承認前にP3-C0 REST contract review、workflow、required checkまたはremote操作を先行しない。

## 25. Gate B Architecture Owner close record

Architecture Ownerは`docs/architecture/validation/phase3-gate-b-web-query-acceptance.md`の横断評価、
GB-D1〜D8、全blocking itemのclose、限定accessibility remediationおよび最終clean aggregateを
2026年9月15日に一体として承認した。

P3-B1〜P3-B4は、Framework / Reference / Tooling Ownership、query / command分離、full-page Thymeleaf主軸、
選択的HTMX、Phase 2 Security再利用、Reference専用競合画面および表示専用local cacheの承認済み境界を維持する。
件数固有のNarrator announcement、全画面・全Role・全支援技術のaccessibility certification、Playwright setup、
REST、distributed cache、Framework昇格、SPA、Level 2およびremote変更はEvidence記載の先へdeferする。

**Decision:** APPROVED — GATE B PASSED / MILESTONE B COMPLETE / ACCEPTED
**Accepted HEAD:** `0f998ba440881108e7ffec2b1690a7d28f19bf61`
**Verification:** Root Reactor 16 / 16 SUCCESS、Reference 86 tests、failure / error / skip 0、headed browser、Owner実演、keyboard / Narrator限定checkpoint、log / Audit / DB突合、Testcontainers cleanup
**Evidence:** `docs/architecture/validation/phase3-gate-b-web-query-acceptance.md`
**Decided by:** Shuichi Kataoka, Architecture Owner
**Date:** 2026年9月15日
**Next CP:** P3-C0最小REST API contract review

Gate Bの承認はP3-C0 contract reviewの開始だけを許可する。endpoint、DTO、Permission、status、error、
optimistic lock契約のOwner承認前にREST production codeを実装しない。workflow、required check、remote push / PR / merge、
ruleset変更またはsnapshot publishは引き続き個別Owner承認を必要とする。

## 26. P3-C0 Architecture Owner decision record

Architecture Ownerは`docs/architecture/validation/phase3-p3-c0-rest-contract-review.md`のP3-C0-D1〜D13を
2026年9月15日に一体として承認した。

**Decision:** APPROVED — P3-C0 CONTRACT COMPLETE / P3-C1 READY
**Approved boundary:** 3 endpoint、独立REST DTO、expectedVersion、HTTP status / Problem Details、
P3-C1限定Bearer代表profile、Phase 4必須Profile Sとの非衝突、Reference / Framework / Tooling Ownership、
React / Next.js・SSO・Access / Refresh Token継続事項
**Evidence:** `docs/architecture/validation/phase3-p3-c0-rest-contract-review.md`
**Decided by:** Shuichi Kataoka, Architecture Owner
**Date:** 2026年9月15日
**Next CP:** P3-C1 最小REST API / Jackson 3 / Problem Details
**Start handoff:** `docs/development/phase3-p3-c1-start-handoff-20260915.md`

P3-C0の承認はP3-C1のReference / Tooling限定実装と検証だけを許可する。P3-C2以降、Phase 4実装、
Framework Public API、migration、workflow、required check、remote push / PR / merge、ruleset変更または
snapshot publishを先行しない。

## 27. P3-C1 implementation and verification checkpoint

2026年9月16日、P3-C0の承認契約内で最小REST API、Jackson 3 Problem Details、P3-C1限定Bearer代表profile、
PostgreSQL integrationおよびpackage済みJARの外部HTTP journeyを実装・検証した。

**Status:** COMPLETE / OWNER APPROVED
**Verification:** Root Reactor 16 / 16 projects、217 tests、Reference 99 tests、failure / error / skip 0、
PostgreSQL 17、実署名Bearer、HTTP 201 / 200 / 204、state / version / Business Audit / log突合、
通常profileのMVC local manualでmaster HTMX検索、expense create / submit、DB / Audit突合、操作中ERROR / WARNなし
**Evidence:** `docs/architecture/validation/phase3-p3-c1-minimal-rest-api.md`
**Decision:** APPROVED — P3-C1 COMPLETE / OWNER APPROVED
**Decided by:** Shuichi Kataoka, Architecture Owner
**Decision date:** 2026年9月16日
**Next CP:** P3-C2 critical journey E2E
**Start handoff:** `docs/development/phase3-p3-c2-start-handoff-20260916.md`

P3-C1のReference / Tooling限定実装とEvidenceをcommit pointとして閉じた後、P3-C2を開始する。
Phase 4、Framework Public API、migration、workflow、required check、remote push / PR / merge、ruleset変更または
snapshot publishを先行しない。

## 28. P3-C2 implementation and verification checkpoint

2026年9月16日、Root Reactor外の非配布`build-support/reference-e2e-verification`に、package済みJAR、
PostgreSQL 17、Bearer API、Session Chromium、HTMX、DB / Audit / logを連結する代表critical journeyを実装した。

**Status:** COMPLETE / OWNER APPROVED / CI CANDIDATE ACCEPTED
**Verification:** 最終コード3回連続PASS（19.058 / 18.601 / 18.577秒、spread約2.59%）、
各run cleanup残存0、Root Reactor 16 / 16 projects・217 tests、P3-C1 API focused 1 test、
P3-B3 / B4 browser focused 3 tests、failure / error / skip 0
**Boundary:** production変更0、Root外・test scope限定、workflow / required check変更0
**Evidence:** `docs/architecture/validation/phase3-p3-c2-critical-journey-e2e.md`
**Decision:** APPROVED — P3-C2 COMPLETE / OWNER APPROVED / CI CANDIDATE ACCEPTED
**Decided by:** Shuichi Kataoka, Architecture Owner
**Decision date:** 2026年9月16日
**DoD continuation:** 実CI通過によるDoD 3-11最終充足はRemote Gate / Gate Cへ継続する
**Next CP:** P3-C3 MyBatis規約fixture / Rule 35〜37

P3-C2をcommit pointとして閉じた後、P3-C3を開始できる。workflow、required check、remote操作および
snapshot publishはRemote Gateまでdeferする。
