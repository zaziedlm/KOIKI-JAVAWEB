# Phase 3 P3-A0 contract review

## 1. Status and boundary

| Item | Result |
|---|---|
| Date | 2026年9月13日 |
| Architecture Owner | Shuichi Kataoka |
| Phase / CP | Phase 3 Reference Vertical Slice / P3-A0 |
| Status | COMPLETE / ARCHITECTURE OWNER APPROVED / P3-A1 READY |
| Branch / start HEAD | `feature/phase3-reference-vertical-slice` / `b257548` |
| Ownership | Reference `master` / `expense`、Framework Identity reuse、Architecture Evidence |
| Target Maven module | 後続CPの`koiki-reference-app`。P3-A0では変更しない |
| Production implementation | 未開始 |

P3-A0は、P3-A1〜A4より前に業務入力、認証・認可、module連携、table Ownership、
migration / FKを確定する文書CPである。Java、Template、SQL、Maven dependency、Public API、
workflowまたはremote設定を変更しない。

## 2. Authoritative inputs

- `docs/development/KOIKI-JavaWeb-FW_Phase3実行計画_v0.1.md`
- `docs/reference/KOIKI-JavaWeb-FW_Reference_Application_Specification_v0.1.md`
- `docs/development/phase2-developer-journey.md`
- `docs/architecture/adr/README.md`のADR-004、007、012、020、022〜025、041〜043、046〜049
- `docs/agent/skills/koiki-project-overview/SKILL.md`
- `docs/agent/skills/koiki-business-feature-work/SKILL.md`

## 3. Approved Reference simplification baseline

Architecture Ownerは2026年9月13日、Referenceとして実装の核を外さない次の単純化を承認した。

### 3.1 Authentication and authorization

| Decision | Approved boundary |
|---|---|
| Browser authentication | Phase 2のlocal Form LoginとSpring Session JDBCを再利用する |
| Identity | `FrameworkPrincipal`、immutable user ID、PermissionをFramework Public API経由で利用する |
| Business roles | 申請者、承認者、経理、master管理者。identity管理者はPhase 2の境界を維持する |
| Authorization | Permission、resource所有権、部門scope、状態をApplication Use Caseで組み合わせる |
| Explicit deny | Role兼務時も自己承認を拒否する |
| Business attributes | 所属部門と承認scopeをReferenceが所有し、Framework Identityへ追加しない |
| Deferred profiles | OIDC / BearerはMVC申請journeyへ混在させない。最小RESTの認証はP3-C0で決定する |

### 3.2 Expense input and workflow

| Decision | Approved boundary |
|---|---|
| 申請額 | 利用者が明細とは独立して入力する |
| 金額 | 日本円、税込、正の整数とする。多通貨・為替・税計算を行わない |
| 不変条件 | 申請額と全明細合計が一致しなければ作成、編集、提出を拒否する |
| 明細 | 1件以上。利用日、経費科目、摘要、目的、正の金額を持つ |
| 利用日 | 操作時の業務日以前とし、提出時に再検査する |
| 経費科目 | 新規作成・編集・再提出では有効な科目だけを許可する。既存表示は廃止後も保持する |
| 文字入力 | 摘要、目的、却下理由、差戻し理由は前後空白を除去し、空文字を拒否する |
| Draft | 整合する完成形だけを保存し、未完成autosaveを行わない |
| Approval queue | 部門scope内の承認者が共有queueから処理し、楽観lockにより先行操作だけを成立させる |
| Decision reason | 却下・差戻し理由を申請者が確認できる形で保持し、成功操作を業務監査へ記録する |
| Non-goals | 証憑、多段・代理承認、取消、動的workflowをPhase 3へ追加しない |

## 4. Approved module collaboration decision

### P3-A0-D1 — command and query collaboration

**Decision: APPROVED（2026年9月13日）**

| Collaboration | Contract |
|---|---|
| 部門廃止 | `master`が`DepartmentDeactivating`を同期発行し、`expense` listenerが自module Use Caseへ委譲する |
| 有効master確認 | `master`所有の狭い同期read-only module contractで、部門、申請者所属部門、経費科目の利用可否だけを返す |
| 承認scope確認 | `expense`が自moduleのscope tableをqueryし、完全一致で判定する |

有効master確認はDomain Eventでは表現しにくい現在値queryである。Event複製tableを追加せず、次を条件に
同期read-only contractをmodule間Event原則の明示的例外とする。

- contractは`master`が所有し、`koiki-reference-app`内部だけに公開する。
- UUID等の識別子と利用可否だけを扱い、JPA Entity、Repository、Domain Model、Application Use Caseを公開しない。
- `expense.application`は自module Portを介し、`expense.adapter.outbound.master`がcontractへ接続する。
- `expense`からmaster所有tableを直接queryしない。
- Framework Public API、別Maven artifact、shared-kernelへ昇格させない。
- ADR-049としてADR Registerとグランドデザイン§30へ記録する。

この制約はcommandおよびcurrent-valueの有効性判断に適用する。後続P3-B1でArchitecture Ownerが承認した
表示専用read modelのread-only JOINはADR-038 P3-B1 fittingの別例外であり、この判断を更新・認可・
業務不変条件またはcurrent-value検証へ流用しない。

## 5. Approved Ownership and table decision

### P3-A0-D2 — Reference-owned business attributes

**Decision: APPROVED（2026年9月13日）**

| Table | Owner | Purpose |
|---|---|---|
| `kkref_department` | `master` | 部門code、名称、有効状態、version |
| `kkref_expense_category` | `master` | 経費科目code、名称、有効状態、version |
| `kkref_user_department_assignment` | `master` | immutable Framework user IDに対する現在の所属部門 |
| `kkref_expense_request` | `expense` | 申請者、部門snapshot、申請額、状態、理由、version |
| `kkref_expense_line` | `expense` | 申請内の利用日、経費科目ID、摘要、目的、金額 |
| `kkref_expense_approver_scope` | `expense` | 承認者user IDと承認対象部門IDの完全一致scope |

所属部門は組織masterとして`master`、承認scopeは経費承認policyとして`expense`が所有する。
Framework Identity tableへ部門列または業務scope列を追加しない。

Role / PermissionはPhase 2 Identity contractで管理し、Reference migrationからFramework tableへ
INSERTしない。開始候補codeは次に限定する。

| Role | Permission |
|---|---|
| `EXPENSE_APPLICANT` | `EXPENSE:APPLY` |
| `EXPENSE_APPROVER` | `EXPENSE:APPROVE` |
| `EXPENSE_ACCOUNTING` | `EXPENSE:SETTLE` |
| `MASTER_ADMIN` | `MASTER:ADMIN` |
| `IDENTITY_ADMIN` | 既存の`IDENTITY:ADMIN` |

本番固定credentialや自動作成userは追加しない。自動testは非配布fixtureから
`IdentityAdministration`等のPublic contractを使って必要なRole / Permissionを準備する。

## 6. Approved migration and FK decision

### P3-A0-D3 — Reference migration series

**Decision: APPROVED（2026年9月13日）**

| Item | Decision |
|---|---|
| Location | `classpath:db/migration/kkref` |
| History table | `kkref_flyway_history` |
| Schema | 現行PostgreSQL schemaを維持し、`kkref_` prefixで所有権を識別する |
| V1 | master tableと所属部門割当 |
| V2 | expense request / line |
| V3 | expense approver scope |
| Production seed | user、credential、Role、Permission、部門、経費科目、scopeを投入しない |

### P3-A0-D4 — FK and constraint boundary

**Decision: APPROVED（2026年9月13日）**

- 同一module内のFKだけを作る。
- `kkref_user_department_assignment.department_id`から`kkref_department`へのFKは許可する。
- `kkref_expense_line.expense_request_id`から`kkref_expense_request`へのFKは許可する。
- expenseからmaster table、ReferenceからFramework Identity tableへのFKは作らない。
- expenseが保持するdepartment ID、category ID、user IDはapplication contractで検査する。
- code、scope組合せはunique constraint、金額・version・状態はDBで表現可能な範囲をcheck constraintで補強する。
- 明細合計と申請額、状態遷移、自己承認はDomain / Applicationの業務規則とし、DB triggerを作らない。
- query時点のscope強制に必要なdepartment、applicant、status、request-line結合indexを各所有moduleが持つ。

## 7. P3-A1〜A4 trace

| CP | Input from P3-A0 | Exit impact |
|---|---|---|
| P3-A1 | master table、所属部門、read-only contract、V1、`MASTER:ADMIN` | 有効master管理、拒否経路、Business Auditを実装できる |
| P3-A2 | 入力規則、Money、完成Draft、V2、master availability Port | 6状態、7遷移、INV-EXP-01〜04を推測なしで実装できる |
| P3-A3 | Form Login / Session、Permission、resource所有権、approver scope、V3 | AC-P3-02〜05とAudit原子性を実装できる |
| P3-A4 | `DepartmentDeactivating`、部門snapshot、未処理4状態、module間FKなし | AC-P3-06 / 07と同期rollbackを実装できる |

P3-A1〜A4の責務は重複せず、P3-A0の承認判断によるTier、Ownership、SecurityまたはLevel 1の
変更はない。

## 8. Verification result

P3-A0では次を文書と静的inventoryで確認し、すべて`PASS`とした。

1. Reference仕様のActor、状態、不変条件、Use Case、AC-P3-01〜10とのtrace。
2. Phase 2 Identity / Audit / Session Public APIだけで構成でき、Framework tableを直接参照しないこと。
3. tableごとにownerが1つであり、module間およびReference-to-Framework FKが0件であること。
4. Eventと同期query contractの用途が混在していないこと。
5. P3-A1〜A4のproduction変更が先行していないこと。

Maven、PostgreSQL、browser testはproduction変更0の文書CPでは実施しない。P3-A1以降で、
focused test、Testcontainers、package済みJAR、log / Audit / DB突合へ段階的に移す。

| Check | Result |
|---|---|
| Reference仕様のActor、状態、不変条件、Use Case、AC-P3-01〜10とのtrace | PASS |
| Phase 2 Identity / Audit / Sessionとの再利用境界 | PASS |
| table Ownership、module間FK 0、Reference-to-Framework FK 0 | PASS |
| command eventとcurrent-value query contractの用途分離 | PASS |
| P3-A1〜A4 production変更の未着手 | PASS |
| ADR-049、実行計画、Reference仕様、Agent導線の整合 | PASS |

文書差分だけであるためMaven、PostgreSQL、browser testは`NOT RUN`とする。これは未検証の実装を
受け入れたことを意味せず、各production CPで対応する実行Evidenceを必須とする。

## 9. Deferred decisions

- Java package、interface、method、exceptionの最終名称はP3-A1の実装直前inventoryで固定する。
- column型、varchar長、numeric precision、index名等の物理詳細は、上記semantic contractを変えない範囲で各migration CPに固定する。
- MVC / HTMX artifact、dependency、asset、browser runnerはP3-B0で決定する。
- cache対象とTTLはP3-B4、最小REST契約と認証profileはP3-C0で決定する。
- workflow、required check、remote操作、snapshot publishはRemote Gateまで実施しない。

## 10. Architecture Owner decision record

| ID | Decision | Recommendation | Status |
|---|---|---|---|
| P3-A0-D1 | Eventと同期read-only master contractの使い分け | APPROVE | APPROVED（2026年9月13日） |
| P3-A0-D2 | table Ownership、所属部門と承認scopeの分離、Role / Permission code | APPROVE | APPROVED（2026年9月13日） |
| P3-A0-D3 | `kkref` migration location / historyとV1〜V3 | APPROVE | APPROVED（2026年9月13日） |
| P3-A0-D4 | module内FKのみ、module間 / Framework FKなし | APPROVE | APPROVED（2026年9月13日） |
| P3-A0-D5 | §3の承認済み単純化とP3-A1〜A4 traceに矛盾なし | CONFIRM | APPROVED（2026年9月13日） |

**Decision:** APPROVED — P3-A0 COMPLETE / P3-A1 READY
**Decided by:** Shuichi Kataoka, Architecture Owner
**Date:** 2026年9月13日

D1〜D5の全項目が承認され、P3-A0を`COMPLETE`とする。次に開始できるproduction CPはP3-A1だけであり、
P3-A2以降、Public API、dependency、workflowまたはremote操作を先行しない。
