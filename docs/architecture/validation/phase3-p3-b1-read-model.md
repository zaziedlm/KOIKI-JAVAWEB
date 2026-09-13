# Phase 3 P3-B1 read model

## 1. Status and boundary

| Item | Result |
|---|---|
| Date | 2026年9月13日 |
| Architecture Owner | Shuichi Kataoka |
| Phase / CP | Phase 3 Reference Vertical Slice / P3-B1 |
| Status | COMPLETE / P3-B2 READY |
| Start HEAD | `5321298` |
| Ownership | Reference `master` / `expense`、Architecture Evidence |
| Target Maven module | `koiki-reference-app` |

P3-B1はTier 1 `master`のJPA class-based射影とTier 2 `expense`のJdbcClient read modelを実証する。
P3-B2のMVC / Thymeleaf / Starter / dependency、P3-B3のHTMX / browser runner、migration、Framework Public API、
workflowまたはremote設定を変更しない。

## 2. Architecture Owner decision

### P3-B1-D1 — display-only cross-owner read model

**Decision: APPROVED（2026年9月13日）**

Architecture Ownerは、expenseに限らず、表示専用read modelで現実的な最終結果の生成を
優先すべき局面があることを確認し、次の狭いread-only JOIN例外を承認した。

1. 表示専用であり、command、更新、認可判断、業務不変条件、current-value検証またはDomain Model復元に使用しない。
2. actor / resource scopeをSQL内で先に強制し、scope外rowを取得後にfilterしない。
3. consumerの`application.query`が契約と不変`record`を所有し、`adapter.outbound.persistence`が最終形を直接materializeする。
4. providerのApplication Use Case、Domain Model、Repository、AdapterまたはJPA Entityに依存しない。
5. table、migration、FK、更新責務のOwnershipを移動しない。
6. Framework Public API、別artifact、shared-kernelまたは汎用query frameworkへ自動昇格させない。
7. 各適用は必要性と境界を記録し、実DB testでscope外rowが返らないことを確認する。

P3-B1では`expense` read modelが`kkref_expense_request`と、表示値に必要な
`kkref_department`および`koiki_user`をJOINする。Identity v0.1は氏名属性を持たないため、
申請者の表示識別子はemailとする。氏名属性の追加やIdentity contract変更は本CPの範囲外である。

この判断はADR-049のcurrent-value master確認を緩めない。expense commandは引き続き
`MasterAvailabilityPort`経由のprovider contractを使い、表示SQLを判定へ流用しない。

## 3. Planned implementation

| Area | Contract |
|---|---|
| `master` | `application.dto` recordをJPA constructor expressionで射影し、Entityを外へ返さない |
| `expense` | `application.query` owning Query Port / page / item records、JdbcClient adapter |
| applicant scope | `applicant_user_id = actor` |
| approver scope | `kkref_expense_approver_scope`完全一致かつ`status <> 'DRAFT'` |
| accounting scope | `status in ('APPROVED', 'SETTLED')` |
| display JOIN | department code / name、applicant emailだけをread-onlyで取得 |
| ordering | `updated_at desc, expense_request_id` |

## 4. Verification plan

- masterのJPA class-based射影がApplication DTOを直接返し、検索・pagingと`MASTER:ADMIN`を守る。
- expenseの3 actor経路がそれぞれのPermissionを要求する。
- PostgreSQL上でin-scope / out-of-scopeを同時に用意し、scope外rowがread modelに現れない。
- JdbcClientがemail、department表示値、amount、status、versionを最終recordへ直接materializeする。
- ArchUnit / NullAway / module testでEntity / RepositoryのView境界への露出がない。
- P3-B1に操作面はないためbrowserは`NOT APPLICABLE`とし、P3-B2以降で同じread modelを画面から確認する。

## 5. Implemented boundary

| Area | Result |
|---|---|
| master DTO | `DepartmentSummary` / `ExpenseCategorySummary`と各pageを`application.dto`に配置 |
| master query | `MasterCatalogQuery`がread-only transactionと`MASTER:ADMIN`を強制 |
| master persistence | Spring Data JPA constructor expressionでApplication DTOを直接射影 |
| expense contract | `ExpenseRequestQuery`、`ExpenseRequestListItem`、`ExpenseRequestPage`を`application.query`が所有 |
| expense use case | `ExpenseReadService`が各Permissionと`FrameworkPrincipal`のactorを確定 |
| expense persistence | `JdbcExpenseRequestQuery`がscope条件、表示JOIN、paging、最終record materializationを実装 |
| unchanged | Maven module / dependency、migration、Framework Public API、MVC / HTMX、workflow、remote設定 |

`ExpenseRequestListItem`にはrequest / applicant / department ID、applicant email、department code / name、
claimed amount、status、version、updated timeだけを含めた。JPA Entity、Domain Model、Repository、
authorization判断またはmaster availability判断を含めていない。

## 6. Verification result

| Check | Result | Evidence |
|---|---|---|
| Compile / Error Prone / NullAway | PASS | `mvnw -pl koiki-reference-app -am -DskipTests compile` |
| Method security | PASS | master / expense追加2 tests、Permission欠如を拒否 |
| Architecture | PASS | `ReferenceArchitectureTest` 2 tests |
| master PostgreSQL projection | PASS | PostgreSQL 17.11、検索・paging・DTO fieldを確認 |
| expense PostgreSQL scope | PASS | applicant 3件、approver 2件、accounting 2件を準備し、scope外row非返却を確認 |
| Direct materialization | PASS | email、department code / name、amount、status、version、updated timeをJdbcClientから最終recordへ生成 |
| Reference regression | PASS | Reactor 11 projects、Reference 61 tests、failure / error / skip 0 |
| Browser operation | NOT APPLICABLE | P3-B1にHTTP / UI操作面なし。P3-B2以降で同じDTO / read modelを表示確認する |

TestcontainersはRancher DesktopのDocker daemonへ接続し、Framework 3 migrationとReference V1〜V3を
新規PostgreSQL containerへ適用した。SQL logで次を確認した。

- applicant queryは`request.applicant_user_id = :actorUserId`を持つ。
- approver queryはscope tableのactor / department完全一致と`status <> 'DRAFT'`を持つ。
- accounting queryは`status in ('APPROVED', 'SETTLED')`を持つ。
- 表示queryとcount queryは同じJOIN / scope条件を使用する。
- scope外dataの全件取得後filterは存在しない。

最初の実DB実行はsandboxからDocker named pipeへアクセスできずApplicationContext初期化前に停止した。
Docker接続可能な実行境界で同じtestを再実行し、コード修正後のfocused testと全Reference回帰をPASSとした。
この初回停止はproduction defectまたはtest skipとして扱わない。

## 7. Deferred boundary

- MVC Controller、Form、View DTO、Template、全画面描画はP3-B2。
- HTMX dependency / asset、11契約、部分描画、browser runnerはP3-B3。
- cache / TTLと2 browser Sessionの楽観lock競合画面はP3-B4。
- 最小REST APIはP3-C0 contract review後。
- applicant氏名属性、汎用cross-owner query framework、Framework Public API昇格は本CPで行わない。

## 8. Exit decision

P3-B1のexit criteriaであるmaster JPA射影、expense JdbcClient query、query時点scope強制、
Entity非露出およびDoD 3-7の実例は実装・検証済みである。P3-B1を`COMPLETE`とし、
次に開始できるproduction CPをP3-B2とする。P3-B3以降を先行しない。
