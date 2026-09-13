# Phase 3 P3-A2 expense vertical slice validation

## 1. Status and boundary

| Item | Result |
|---|---|
| Date | 2026年9月13日 |
| Phase / CP | Phase 3 Reference Vertical Slice / P3-A2 |
| Status | COMPLETE / P3-A3 READY |
| Branch / start HEAD | `feature/phase3-reference-vertical-slice` / `7f41175` |
| Ownership | Reference `expense` |
| Maven module | `koiki-reference-app`（追加moduleなし） |
| Tier / persistence | Tier 2 RICH / JPA共有モデル |

P3-A2は、承認済みの6状態、7遷移、INV-EXP-01〜04、ADR-049のmaster照会境界と
V2 migrationを実装・検証するproduction CPである。Permission、resource所有権、部門scope、
Business AuditはP3-A3、Department eventはP3-A4、read modelとWeb UIはMilestone Bへ留保した。

## 2. Implemented boundary

| Area | P3-A2 result |
|---|---|
| Aggregate | `ExpenseRequest`が申請と全明細を一貫性境界として所有 |
| Value / child | 日本円正整数の`Money`、利用日・科目・摘要・目的・金額を持つ`ExpenseLine` |
| Lifecycle | `DRAFT`、`SUBMITTED`、`APPROVED`、`REJECTED`、`RETURNED`、`SETTLED` |
| Transition | 作成、提出、承認、却下、差戻し、再編集開始、精算の7遷移 |
| Invariants | 明細合計一致、Draft限定編集、自己承認・却下・差戻し禁止、未定義遷移拒否 |
| Use Case | 作成・編集・提出・承認・却下・差戻し・再編集開始・精算を同一Application Serviceで調整 |
| Master collaboration | expense所有Portを介し、outbound Adapterだけがmaster所有`MasterAvailabilityQuery`へ接続 |
| Persistence | Domain ModelをJPA Entityとして兼用し、Spring Data Commons Repository contractを使用 |
| Migration | `db/migration/kkref/V2__create_expense.sql`、申請・明細の2表 |
| FK | `kkref_expense_line`から同一expense moduleの申請表だけ。master / Framework FKなし |

申請内容の編集は、候補明細をすべて検査して合計一致を確認してからAggregateへ反映する。
提出時も利用日、必須項目、金額一致を再検査し、master Portで部門・所属・科目の現在値を
再確認する。P3-A3で扱うactor所有権、Permission、承認部門scope、Business Auditは混入させていない。

## 3. ADR-049 architecture-rule alignment

従来のRule 3は他module依存を`domain.event`だけに限定しており、P3-A0-D1で承認された狭い同期
read-only query例外を表現できなかった。Rule 3へADR-049を加え、次をすべて満たす依存だけを許可した。

1. originが`adapter.outbound`にある。
2. targetが別moduleの`contract`にあるinterfaceである。
3. target名が`Query`で終わる。

Application、Domain、Repository、Inbound Adapterからの直接参照や、command contract、実装classへの
依存は許可しない。適合fixtureとReference実コードの`ReferenceArchitectureTest`で機械検証した。

## 4. Verification results

| Check | Result | Evidence |
|---|---|---|
| Compile / Error Prone / NullAway | PASS | 二段階buildのinstall / packageで49 production、14 test sourceをcompile |
| Domain unit | PASS | 4 tests、6状態・7遷移、INV-EXP-01〜04、入力再検査、失敗時非変更 |
| PostgreSQL / Flyway integration | PASS | PostgreSQL 17 Testcontainers、3 tests、V1 + V2適用 |
| Reference regression | PASS | 49 tests、failure 0、error 0、skip 0 |
| Architecture rules | PASS | Reference 2 tests、ArchUnit module 67 tests |
| Optimistic lock | PASS | 実DB versionを各更新で照合し、状態遷移ごとに増分を確認 |
| Refusal / rollback | PASS | 合計不一致、自己判断、無効masterを拒否し、申請状態・version・明細件数が不変 |
| Migration / FK | PASS | expense 2表、同一module FK 1件、master / Frameworkへのcross-module FK 0件 |
| Packaged JAR | PASS | executable JARにexpense classesとV2を収載し、test classは非収載 |
| Browser operation | NOT APPLICABLE | P3-A2にはController / View / endpointがなく、Gate A方針どおり必須化しない |

最終Reference検証は、P3-A1で記録したWindowsのReactor増分出力不整合を避ける二段階経路で行った。

1. `mvnw install -pl koiki-reference-app -am -DskipTests`
2. `mvnw test -pl koiki-reference-app`

別途`mvnw test -pl koiki-archunit-rules`と`mvnw package -pl koiki-reference-app -DskipTests`を
実行した。実DB logではV2適用、申請・明細SQL、version付き更新を確認した。固定credential、
Cookie、tokenまたはPIIはEvidenceへ保存していない。

## 5. Deferred boundary

- `EXPENSE:*` Permission、本人所有権、承認部門scope、Business Audit、V3はP3-A3。
- `DepartmentDeactivating`、未処理申請照会、同期listenerはP3-A4。
- `ExpenseApproved` / `ExpenseRejected`等の後続event利用は承認済みCP境界まで保留する。
- query read model、MVC / Thymeleaf / HTMX、browser操作はMilestone B。
- 最小REST APIはP3-C0 contract review後。
- Framework Public API、Maven module、dependency、Project Template、workflow、remote設定は変更しない。

## 6. Exit decision

P3-A2のexit criteriaである6状態、7遷移、INV-EXP-01〜04、成功・拒否・rollbackをすべて
実装・検証した。P3-A2を`COMPLETE`とし、次に開始できるproduction CPをP3-A3とする。
P3-A3のPermission / ownership / scope / Audit設計を再確認するまでは、その実装を開始しない。

