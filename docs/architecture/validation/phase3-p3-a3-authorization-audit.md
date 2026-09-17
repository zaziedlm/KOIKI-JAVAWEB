# Phase 3 P3-A3 authorization and audit validation

## 1. Status and boundary

| Item | Result |
|---|---|
| Date | 2026年9月13日 |
| Phase / CP | Phase 3 Reference Vertical Slice / P3-A3 |
| Status | COMPLETE / P3-A4 READY |
| Branch / start HEAD | `feature/phase3-reference-vertical-slice` / `639d72c` |
| Ownership | Reference `expense`、Framework Security / Identity / Audit contract再利用 |
| Maven module | `koiki-reference-app`（追加moduleなし） |

P3-A3は、P3-A0で承認されたPermission、resource所有権、部門ID完全一致scope、V3と
Business AuditをP3-A2のUse Caseへ統合し、AC-P3-02〜05をbackendで検証するproduction CPである。
Department event、部門廃止、read model、MVC / HTMXおよびRESTは実装しない。

## 2. Implemented authorization boundary

| Operation | Permission | Additional Application guard | Domain guard |
|---|---|---|---|
| Draft作成・編集・提出 | `EXPENSE:APPLY` | 認証主体を申請者IDとして使用。既存申請は本人所有のみ | `DRAFT`、内容不変条件 |
| 再編集開始 | `EXPENSE:APPLY` | 本人所有のみ | `RETURNED` |
| 承認・却下・差戻し | `EXPENSE:APPROVE` | V3の承認者user IDと申請部門IDの完全一致scope | `SUBMITTED`、自己判断禁止、理由 |
| 精算 | `EXPENSE:SETTLE` | 認証主体必須 | `APPROVED` |

Actor IDはFormやDTO等の入力として受け取らず、Phase 2の`FrameworkPrincipal`からApplication Use Caseが
取得する。他人の申請またはscope外申請は、対象の存在有無を区別しない`NOT_FOUND`へ分類する。
identity管理者またはmaster管理者Permissionからexpense権限を暗黙に付与しない。

ReferenceのRole / Permission候補codeはP3-A0の
`EXPENSE_APPLICANT` / `EXPENSE:APPLY`、`EXPENSE_APPROVER` / `EXPENSE:APPROVE`、
`EXPENSE_ACCOUNTING` / `EXPENSE:SETTLE`を維持する。production user、固定credential、Roleまたは
Permission seedは追加せず、後続の操作fixtureはFramework `IdentityAdministration`経由で準備する。

## 3. Scope and migration

`db/migration/kkref/V3__create_expense_approver_scope.sql`で、expense所有の
`kkref_expense_approver_scope`を作成した。主keyは承認者user IDと部門IDの組であり、scope判定は
完全一致だけである。Framework Identity、masterまたはexpense requestへのFKを追加していない。

Applicationは`ApproverScopePort`だけを参照し、outbound persistence Adapterが`JdbcClient`でV3を
照会する。Framework Identityへ部門列やscope列を追加せず、他module tableも直接照会しない。

## 4. Business Audit and atomicity

成功したworkflow操作を`EXPENSE_WORKFLOW`として、認証主体と`EXPENSE_REQUEST` IDを付けて
Framework `BusinessAuditRecorder`へ記録する。

- `SUBMIT_EXPENSE`
- `APPROVE_EXPENSE`
- `REJECT_EXPENSE`
- `RETURN_EXPENSE`
- `BEGIN_REEDIT_EXPENSE`
- `SETTLE_EXPENSE`

作成・編集はUC-EXP-01として成功Audit対象に含めない。Permission、所有権、scope、状態または
不変条件で拒否された操作もBusiness Auditへ記録しない。Audit recorderが失敗した場合は、先に
flushされた申請状態を含む業務transaction全体をrollbackする。

## 5. Verification results

| Check | Result | Evidence |
|---|---|---|
| Compile / Error Prone / NullAway | PASS | 51 production、16 test sourceをcompile |
| Permission method security | PASS | 3 tests、APPLY / APPROVE / SETTLEの不足をUse Case実行前に拒否 |
| PostgreSQL / Flyway | PASS | PostgreSQL 17 Testcontainers、Reference V1〜V3適用 |
| Ownership | PASS | 他者の提出を`NOT_FOUND`で拒否し、状態・version・Audit不変 |
| Exact department scope | PASS | Permission保持者でもV3割当なしの承認を`NOT_FOUND`で拒否 |
| Successful workflow Audit | PASS | 提出、承認、却下、差戻し、再編集開始、精算のactionをDBで突合 |
| Audit failure rollback | PASS | recorder失敗後も実DBが`DRAFT`と元versionを維持 |
| AC-P3-02 / 03 | PASS | 有効masterと正しい入力で提出成功、不正入力は永続化しない |
| AC-P3-04 | PASS | scope内の他者申請を3種類の判断状態へ遷移し、同一transactionでAudit記録 |
| AC-P3-05 | PASS | Permission不足、他人所有、scope外、自己判断、状態違反で状態・Audit不変 |
| Reference regression | PASS | 55 tests、failure 0、error 0、skip 0 |
| Architecture rules | PASS | `ReferenceArchitectureTest` 2 tests |
| Browser operation | NOT APPLICABLE | expenseのController / Viewがまだなく、P3-B2以降で実施 |

検証は`mvnw package -pl koiki-reference-app -DskipTests`と、Rancher Desktop上のPostgreSQLを使う
`mvnw test -pl koiki-reference-app`で実施した。実DB logではV3 migration、scope query、version付き
状態更新とBusiness Audit INSERTを確認した。固定credential、Cookie、tokenまたはPIIは保存していない。

## 6. Deferred boundary

- 部門廃止、`DepartmentDeactivating`、未処理4状態と終端2状態の同期判定はP3-A4。
- 承認queueを含むscope付きread modelはP3-B1。
- expense URL認可、MVC / Thymeleaf / HTMX、実Form Login / Session browser journeyはP3-B2以降。
- 最小REST APIの認証profileと契約はP3-C0 review後。
- production初期user、固定credential、直接Identity table INSERTは追加しない。
- Framework Public API、Maven module、dependency、workflow、remote設定は変更しない。

## 7. Exit decision

P3-A3のexit criteriaであるAC-P3-02〜05、scope外非露出、申請状態とBusiness Auditの原子性を
実装・検証した。P3-A3を`COMPLETE`とし、次に開始できるproduction CPをP3-A4とする。
P3-A4以降の同期Domain Event実装は、このcommit pointより先行しない。

