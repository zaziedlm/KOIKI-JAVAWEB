# Phase 2 P2-C1 PostgreSQL Migration start handoff

## 1. Handoff status

- 作成日: 2026年9月10日
- 再開予定日: 2026年9月11日
- Architecture Owner: Shuichi Kataoka
- branch: `feature/phase2-p2-c1-postgresql-migration`
- baseline commit: `8873942b3c8b9c83f08b607f9fd03ebbad928324`（PR #30 / Gate B final closeout）
- Phase status: `GATE B COMPLETE / ARCHITECTURE OWNER APPROVED — P2-C1 CONTRACT REVIEW PREPARED`
- Ownership: Framework Migration / Tooling Evidence
- production change: 0
- immediate next action: `phase2-p2-c1-contract-review.md`のC1-C1〜C7をArchitecture Owner reviewする

本handoffは、2026年9月11日にP2-C1を安全に再開するための作業正本である。本日の作業はbranch作成、
read-only inventory、contract review案およびhandoff作成までとし、production SQL、Java、POM、test、script、CI、
rulesetまたはremote stateを変更していない。

## 2. Required reading order

1. `AGENTS.md`
2. `docs/agent/skills/koiki-project-overview/SKILL.md`
3. 本書
4. `../architecture/validation/phase2-p2-c1-contract-review.md`
5. `KOIKI-JavaWeb-FW_Phase2実行計画_v0.1.md`のMilestone C、§6〜§9、§11
6. `../architecture/validation/phase2-gate-b-closeout.md`
7. `../architecture/validation/phase2-p2-b1-contract-review.md`のMigration境界
8. `../architecture/validation/phase2-p2-b2-contract-review.md`のB2-C9
9. `../architecture/validation/phase2-p2-b3-contract-review.md`のB3-C3
10. `../architecture/adr/README.md`のADR-012、ADR-042、ADR-048
11. `../architecture/grand-design/KOIKI-JavaWeb-FW_グランドデザイン_v0.2.md`のMigration、table ownership、Phase 2 DoD

P2-C1は業務機能ではないため、`koiki-business-feature-work`は通常不要である。Customer / Reference業務codeへ変更が
必要になった場合は境界逸脱として停止し、その理由をreviewする。

## 3. Transfer baseline

```text
Local / origin main: 8873942b3c8b9c83f08b607f9fd03ebbad928324
Baseline subject: Merge pull request #30 from zaziedlm/docs/phase2-gate-b-closeout
Working branch: feature/phase2-p2-c1-postgresql-migration
Worktree before preparation: clean
Gate B: COMPLETE / ARCHITECTURE OWNER APPROVED
```

準備差分は本handoff、P2-C1 contract review案およびPhase 2実行計画の状態更新だけとする。

## 4. Current implementation inventory

### Framework Flyway mechanism

- location: `classpath:db/migration/koiki`
- history: `koiki_flyway_history`
- Customer location: `classpath:db/migration/customer`
- Customer history: `flyway_schema_history`
- order: Framework migration後にCustomer migration
- implementation: `KoikiDataFlywayAutoConfiguration`

### Production migrations

| Artifact | Migration | Tables | State |
|---|---|---:|---|
| `koiki-starter-audit` | 未追加 | 1予定 | P2-C1 |
| `koiki-starter-identity` | `V2026090301__create_koiki_identity.sql` | 8 | verified |
| `koiki-starter-session-jdbc` | `V2026090701__create_koiki_session.sql` | 2 | verified |

Auditは`AuditEventEntity`と非配布`audit-fixture-schema.sql`でtransaction semanticsを検証済みだが、fixtureの
`fixture_business_change`および強制失敗constraintをproductionへ移さない。

### Session initializer

- `spring.session.jdbc.initialize-schema=never`
- `spring.session.jdbc.table-name=koiki_session`
- `spring.session.jdbc.cleanup-cron=-`
- guardで異なる実効値をstartup failureにする

## 5. Key issue found during preparation

Audit、Identity、Sessionは同じFramework migration location / historyを共有する。Starter依存は
Audit→Identity→Sessionの順であるため、version順も一致させないと、後からStarterを追加したApplicationに
未適用の低version migrationが出現する。

contract review案は、既存Identity / Session migrationを変更せず、AuditをIdentityより前の未使用version
`V2026090300`へ置く案を推奨している。`outOfOrder=true`、既存migration改名、Starter別history分割は不採用候補である。
このversion判断は未承認であり、明日のArchitecture Owner review前にSQLを追加しない。

## 6. Proposed P2-C1 slices

| Slice | Scope | Commit point |
|---|---|---|
| C1-1 | contract / inventory review | Owner承認済み契約だけをcommit |
| C1-2 | Audit production migration / static inventory | SQL、version / ownership検査、focused test |
| C1-3 | PostgreSQL clean / supported upgrade | clean 4 profile、Phase 1b upgrade、table / history Evidence |
| C1-4 | closeout | Root / Null Safety / Public API / Gate B回帰、Owner承認 |

各sliceの具体構成はC1-C1〜C7承認で変更できる。準備案を実装済み契約として扱わない。

## 7. Tomorrow start checklist

```powershell
git status --short
git branch --show-current
git log -3 --oneline --decorate
docker version
java -version
.\mvnw.cmd --version
```

期待値:

- branchが`feature/phase2-p2-c1-postgresql-migration`
- baselineが`8873942`
- preparation commit後ならworktreeがclean
- Rancher Desktop / Docker Engineが必要な検証はC1-C3〜C7承認後にだけ開始

## 8. Stop conditions

- contract review承認前にAudit production SQL、既存migration、POM、fixtureまたはscriptを変更する。
- Identity / Session migrationを移動、複製、改名またはchecksum変更する。
- `outOfOrder`、別Framework history、Spring initializerまたはJPA schema generationを推測で有効化する。
- Phase 2途中commitを正式なsupported production baselineと表現する。
- Customer table / migration、Reference persistenceまたはtest failure switchをFrameworkへ含める。
- Oracle、未採用DB向けDDL、vendor分岐またはCIを追加する。
- Docker残留、dirty worktreeまたはbaseline不一致のまま検証を続ける。

## 9. Immediate next action

明日はproduction実装から開始せず、`phase2-p2-c1-contract-review.md`のArchitecture Owner判断1〜7をレビューする。
特にAudit migration version、Phase 1b supported upgrade起点、table ownershipの二軸分類を確認し、承認後に
C1-2 Audit migration / static inventoryへ進む。
