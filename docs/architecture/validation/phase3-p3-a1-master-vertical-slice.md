# Phase 3 P3-A1 master vertical slice validation

## 1. Status and boundary

| Item | Result |
|---|---|
| Date | 2026年9月13日 |
| Phase / CP | Phase 3 Reference Vertical Slice / P3-A1 |
| Status | COMPLETE / P3-A2 READY |
| Branch / start HEAD | `feature/phase3-reference-vertical-slice` / `48e1151` |
| Ownership | Reference `master` |
| Maven module | `koiki-reference-app` |
| Tier / persistence | Tier 1 SIMPLE / JPA |

P3-A1は、P3-A0で承認されたmaster Ownership、V1 migration、`MASTER:ADMIN`、
master-owned current-value query contractを実装し、AC-P3-01、拒否経路、Business Auditを
実PostgreSQLで確認するproduction CPである。expense、DepartmentDeactivating、MVC / HTMX、REST、
Framework Public API、Maven module、workflowまたはremote設定は変更しない。

## 2. Implemented boundary

| Area | P3-A1 result |
|---|---|
| Master model | 部門、経費科目、ユーザー所属部門をbehavior-light JPA Entityとして実装 |
| Administration | 登録、名称変更、経費科目廃止、所属・異動をTier 1 Application Use Caseで実装 |
| Authorization | 全管理操作を`MASTER:ADMIN`でmethod security強制 |
| Identity reuse | `FrameworkPrincipal`とimmutable `FrameworkUserId`だけを使用 |
| Audit reuse | 成功した管理操作を`MASTER_ADMINISTRATION` Business Auditとして同一transactionに記録 |
| Query contract | 部門、経費科目、ユーザー所属部門の有効性だけを返すread-only contractをmasterが所有 |
| Migration | `db/migration/kkref/V1__create_master.sql`、`kkref_flyway_history` |
| FK | `kkref_user_department_assignment`から同一moduleの`kkref_department`だけ |
| Seed | production seedなし |

Framework starterの`@EntityScan`と共存させるため、Reference masterのadapter内に
module-owned `MasterPersistenceConfiguration`を配置した。Framework側のscan範囲やPublic APIは
変更していない。

## 3. Verification results

| Check | Result | Evidence |
|---|---|---|
| Compile / Error Prone / NullAway | PASS | `mvnw -pl koiki-reference-app -am -DskipTests compile` |
| Focused unit / method security | PASS | 6 tests、failure mapping、Audit非記録、`MASTER:ADMIN`拒否 |
| PostgreSQL / Flyway integration | PASS | PostgreSQL 17 Testcontainers、2 tests |
| Reference regression | PASS | 42 tests、failure 0、error 0、skip 0 |
| Architecture rules | PASS | `ReferenceArchitectureTest` 2 tests |
| Packaged JAR | PASS | executable JARにReference V1とmaster classesを収載し、test dependencyは非収載 |
| Framework / Reference migration分離 | PASS | `koiki_flyway_history` 3 migrations、`kkref_flyway_history` baseline 0 + V1 |
| Repeat migration | PASS | Reference Flyway再実行は0 migration |
| Reference-to-Framework FK | PASS | PostgreSQL catalog queryで0件 |
| AC-P3-01 | PASS | 部門・経費科目を保存し、成功Auditを同一transactionで記録 |
| Refusal / rollback | PASS | 重複codeを`CONFLICT`へ安全に分類し、追加row / Auditとも0 |
| Availability query | PASS | 有効部門、経費科目、所属部門を実SQLで確認し、科目廃止後はfalse |
| Browser operation | NOT APPLICABLE | P3-A1には操作可能な画面がなく、Gate A方針どおり必須化しない |

実PostgreSQL testではHibernate SQL logも確認し、master 3表へのmutationと
`koiki_audit_event`への成功Audit、重複constraint違反時のrollbackをDB件数と突合した。
固定credential、Cookie、tokenまたはPIIはEvidenceへ保存していない。

## 4. Build-path observation

Windows上の単一`-am test` Reactorで、fork compile後に一部上流moduleの`target/classes`が
欠落し、`spring.factories`だけが残る増分出力不整合を観測した。production codeの回避変更は
加えず、次の再現可能な二段階経路で最終検証した。

1. `mvnw install -pl koiki-reference-app -am -DskipTests`
2. `mvnw test -pl koiki-reference-app`

この経路は各Framework artifactをJARとして確定した後にReferenceをConsumerとして試験し、
Reference全42 testを通過した。Reactor clean aggregateの再確認はGate Aの検証項目として残す。

## 5. Deferred boundary

- expense Domain / JPA共有モデル / Use Case / V2はP3-A2。
- Permission、resource所有権、承認scope / V3はP3-A3。
- 部門廃止と`DepartmentDeactivating`同期eventはP3-A4。
- read model、MVC / Thymeleaf / HTMX、browser操作はMilestone B。
- 最小REST APIはP3-C0 contract review後。
- Framework Public API、Maven module、Project Template、workflow、remote設定は変更しない。

## 6. Exit decision

P3-A1のexit criteriaであるAC-P3-01、拒否経路、Business Auditはすべて実装・検証済みである。
P3-A1を`COMPLETE`とし、次に開始できるproduction CPをP3-A2とする。P3-A3以降は先行しない。
