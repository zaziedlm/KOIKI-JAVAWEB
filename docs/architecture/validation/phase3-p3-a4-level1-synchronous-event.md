# Phase 3 P3-A4 Level 1 synchronous event validation

## 1. Status and boundary

| Item | Result |
|---|---|
| Date | 2026年9月13日 |
| Phase / CP | Phase 3 Reference Vertical Slice / P3-A4 |
| Status | COMPLETE / GATE A READY |
| Branch / start HEAD | `feature/phase3-reference-vertical-slice` / `ed1359b` |
| Ownership | Reference `master` event / Reference `expense` invariant |
| Maven module | `koiki-reference-app`（追加moduleなし） |
| Spring Modulith boundary | Level 1、Spring標準の同期event、runtime Modulith依存なし |

P3-A4は、部門廃止のcommand整合を`DepartmentDeactivating`で連携し、expenseが所有する
未処理申請不変条件によってmasterの廃止transactionを同期rollbackできることを実証するproduction CPである。
read model、MVC / HTMX、REST、非同期eventおよびLevel 2は実装しない。

## 2. Implemented collaboration

| Responsibility | Placement |
|---|---|
| 部門廃止Use Case、transaction、event発行、成功Audit | `master.application.MasterAdministration` |
| 部門状態の更新 | master所有のTier 1 JPA Entity |
| 公開event | master所有の`domain.event.DepartmentDeactivating` record |
| event受付 | `expense.adapter.inbound.event.DepartmentDeactivatingListener` |
| 未処理判定の調整 | `expense.application.DepartmentDeactivationGuard` |
| 未処理存在照会 | expense所有のSpring Data Commons Repository |
| 未処理状態の定義 | expense Domainの`ExpenseStatus` |

eventは部門IDだけを持つ不変recordであり、JPA Entity、Repository、Application Use Caseまたは
Domain Modelを露出しない。listenerは同期`@EventListener`を1件だけ使用し、自module Application
guardへ委譲する。`@TransactionalEventListener`、`ApplicationModuleListener`、`@Async`および
追加runtime dependencyは使用しない。

expenseの業務vetoはexpense内部の例外に閉じ、masterはevent発行境界で安全な`CONFLICT`へ変換する。
予期しないRuntime例外は従来どおり`DEPENDENCY_FAILURE`となる。masterからexpenseへのclass参照、
expenseからmasterのApplication / persistenceへの参照、および他module所有tableへの直接照会はない。

## 3. Transaction and state decision

未処理状態は`DRAFT`、`SUBMITTED`、`RETURNED`、`APPROVED`の4状態である。同期listenerが対象部門の
存在を検出すると廃止を拒否し、部門の`active`、versionおよび`DEACTIVATE_DEPARTMENT` Auditを
変更しない。`REJECTED`、`SETTLED`の2終端状態だけが存在する場合、または申請が存在しない場合は、
部門を非activeへ更新し、versionを進め、同一transactionで成功Auditを記録する。

`DRAFT`を含むすべての申請はV2の`department_id`を判定snapshotとして使用する。listenerが
masterの未commit値を再照会する必要はなく、V1〜V3 migrationおよびmodule間FKなしのbaselineも不変である。

## 4. Verification results

| Check | Result | Evidence |
|---|---|---|
| Compile / Error Prone / NullAway | PASS | 55 production、17 test sourceをcompile |
| AC-P3-06 | PASS | 未処理4状態ごとに`CONFLICT`、部門active / version不変、廃止Audit 0 |
| AC-P3-07 | PASS | 申請なし、`REJECTED`、`SETTLED`で廃止成功、version更新、成功Audit 1 |
| PostgreSQL / Flyway | PASS | PostgreSQL 17 Testcontainers、Reference V1〜V3適用 |
| Synchronous listener | PASS | production `@EventListener` 1件、transactional / async listener 0件 |
| Direct module reference | PASS | Architecture Testでeventと承認済みread-only contract以外の直接参照0 |
| Architecture rules | PASS | `ReferenceArchitectureTest` 2 tests |
| Reference regression | PASS | 57 tests、failure 0、error 0、skip 0 |
| Browser operation | NOT APPLICABLE | P3-A4にはController / View操作面がなく、Gate Aでは自動testとDB / logを使用 |

検証は`mvnw package -pl koiki-reference-app -DskipTests`、Architecture Test単独、およびRancher Desktop上の
PostgreSQLを使う`mvnw test -pl koiki-reference-app`で実施した。初回全回帰ではArchUnitが
非record event-package型とtest起因のmodule cycleを検出したため、非公開vetoをexpense内部へ戻し、
cross-module test参照を除去した。修正後のArchitecture Testと全57 testはすべて成功した。

実DB logでは、部門取得、expense所有queryによる4状態の存在確認、許可時だけのversion付き部門UPDATEと
Business Audit INSERTを確認した。固定credential、Cookie、tokenまたはPIIはEvidenceへ保存していない。

## 5. Deferred boundary

- Gate AでMilestone AのDomain / transaction / module acceptanceをreviewする。
- master / expense read modelとscope付きqueryはP3-B0承認後のP3-B1。
- MVC / Thymeleaf / HTMXと実browser操作はP3-B0承認後のP3-B2以降。
- 最小REST APIはP3-C0 contract review後。
- 非同期event、通知、会計連携およびSpring Modulith Level 2はPhase 4。
- Framework Public API、Maven module、dependency、migration、workflow、remote設定は変更しない。

## 6. Exit decision

P3-A4のexit criteriaであるAC-P3-06 / 07、未処理4状態の同期rollback、終端2状態と申請なしの成功、
直接参照0および同期listener件数を実装・検証した。P3-A4を`COMPLETE`とし、次をGate A reviewとする。
Gate A承認前にMilestone BまたはP3-B0を開始しない。
