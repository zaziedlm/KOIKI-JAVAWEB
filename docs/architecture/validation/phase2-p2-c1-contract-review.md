# Phase 2 P2-C1 PostgreSQL Migration contract review

## 1. Status and scope

- 作成日: 2026年9月10日
- 作業パッケージ: `P2-C1 / C1-1`
- 状態: `PROPOSED — ARCHITECTURE OWNER REVIEW REQUIRED`
- baseline: main merge commit `8873942b3c8b9c83f08b607f9fd03ebbad928324`
- branch: `feature/phase2-p2-c1-postgresql-migration`
- Ownership: Framework Migration / Tooling Evidence
- 本reviewでのproduction change: なし

本reviewは、Gate Bで個別実装・検証したAudit、Identity、Sessionの永続化を、Framework Flyway正本、
table ownership、第三者schema、clean installおよびsupported upgradeの単位で総合するための契約を定める。
P2-B2 / P2-B3のproduction migrationを未検証へ戻さず、P2-B1で留保したAudit production migrationと
cross-Starterの順序・upgrade Evidenceを追加する。

## 2. Accepted baseline

1. Framework migration locationは`classpath:db/migration/koiki`、historyは`koiki_flyway_history`である。
2. Customer migration locationは`classpath:db/migration/customer`、historyは`flyway_schema_history`である。
3. Framework migrationをCustomer migrationより先に実行し、所有者別historyを混在させない。
4. Identity migration `V2026090301__create_koiki_identity.sql`は8 tableを所有し、clean PostgreSQLで検証済みである。
5. Session migration `V2026090701__create_koiki_session.sql`は2 tableを所有し、Spring Session公式PostgreSQL schemaを
   baselineに、Framework名へ適合している。
6. `spring.session.jdbc.initialize-schema=never`、table `koiki_session`、cleanup cron `-`はstartup guardで固定済みである。
7. AuditのJPA persistenceとtransaction semanticsは実PostgreSQL fixtureで検証済みだが、production migrationはP2-C1へ留保した。
8. Oracle、未採用DB向けDDL、vendor分岐、Customer tableまたはtest fixture SQLをFramework migrationへ昇格させない。

## 3. Current inventory

### 3.1 Production migration

| Version | Owner artifact | Tables | State |
|---|---|---|---|
| `2026090301` | `koiki-starter-identity` | Identity 8 table | production / verified |
| `2026090701` | `koiki-starter-session-jdbc` | Session 2 table | production / verified |
| 未割当 | `koiki-starter-audit` | `koiki_audit_event` | production migration pending |

Identity 8 tableは`koiki_user`、`koiki_role`、`koiki_permission`、`koiki_user_role`、
`koiki_role_permission`、`koiki_password_credential`、`koiki_login_attempt`、
`koiki_external_identity_link`である。Session 2 tableは`koiki_session`、
`koiki_session_attributes`である。P2-C1完了時のFramework business / security tableは合計11 tableとなる。

### 3.2 Ownership axes

| Object | DDL / migration owner | Runtime data manager | Classification |
|---|---|---|---|
| `koiki_audit_event` | KOIKI Framework / Audit Starter | KOIKI Audit persistence | Framework table |
| Identity 8 table | KOIKI Framework / Identity Starter | KOIKI Identity persistence | Framework table |
| `koiki_session*` 2 table | KOIKI Framework / Session JDBC Starter | Spring Session JDBC | Framework-owned schema using third-party runtime |
| `koiki_flyway_history` | KOIKI configuration | Flyway | Framework migration control table |
| `flyway_schema_history` | Customer configuration | Flyway | Customer migration control table |
| Customer migration tables | Customer Application | Customer persistence | Customer table |

第三者table一覧では、単一の「owner」表現に畳まず、DDL責任とruntime管理主体を分ける。
Spring Session既定の`SPRING_SESSION` / `SPRING_SESSION_ATTRIBUTES`が生成されないこともnegative inventoryで確認する。

## 4. C1-C1 — Scope and artifact placement

### Proposed decision

- 新しいproduction Maven moduleは追加しない。
- Audit migrationは実装ownerである`koiki-starter-audit`へ置く。
- Identity / Session migrationは既存owner artifactから移動・複製しない。
- `koiki-starter-data`は実行順、location、historyの共通機構だけを所有し、全DDLの集積先にしない。
- table inventoryとupgrade fixtureは`build-support/security-foundation-verification`が所有する。

中央artifactへ全SQLを移す案は、optional Starterを導入していないApplicationへ未使用tableを強制するため採用しない。

## 5. C1-C2 — Audit migration and version order

Starter dependencyは次の単調な順序である。

```text
koiki-starter-audit
  <- koiki-starter-identity
    <- koiki-starter-session-jdbc
```

すべてのFramework migrationが同じlocation / historyを共有するため、migration versionもこの依存順を保つ必要がある。
Auditだけを導入したApplicationが後からIdentity、Sessionを追加しても、未適用の低いversionが現れない構成にする。

| Candidate | Evaluation |
|---|---|
| **A — AuditをIdentityより前の未使用versionへ配置** | **推奨。** 既存Identity / Session fileを変更せず、Starter追加順とversion順を一致させる |
| B — Auditを最新versionへ配置し`outOfOrder=true` | 不採用。通常運用での履歴順を崩し、意図しない古いmigration適用を許す |
| C — Identity / Sessionを改名して再採番 | 不採用。mainへ入ったmigration identityと既存Evidenceを変更する |
| D — Starterごとにhistory tableを分割 | 不採用。承認済み二階層契約をP2-C1だけで三階層以上へ変更する |

候補Aの具体versionは`V2026090300__create_koiki_audit.sql`とする。Audit production実装が追加された
2026年9月3日と整合し、同日のIdentity `...0301`より前に位置する。`00` sequenceを正式規約として許容するかを
Architecture Owner判断対象とし、承認前にSQLを追加しない。

## 6. C1-C3 — Audit physical schema

Audit production DDLはB1 fixtureの業務table / failure constraintを持ち込まず、`AuditEventEntity`と一致する
`koiki_audit_event` 1 tableだけを作成する。

| Concern | Proposed decision |
|---|---|
| ID | `event_id uuid` primary key |
| type / result | `audit_type varchar(16)`、`actor_type varchar(16)`、`result varchar(16)` |
| event / action | `event_type varchar(128)`、`action varchar(128)` |
| optional identifiers | actor / subject / resource ID、reason、request / traceをEntity長に一致させnullable |
| timestamp | `occurred_at timestamp(6) with time zone not null` |
| index | 初期query要件がないため先行追加しない。実利用queryと同じCPでreviewする |
| fixture exclusion | `fixture_business_change`と`fixture_audit_failure` constraintをproductionへ含めない |

enum相当値のCHECK制約は、Public APIで将来additive値を追加するとDB migrationが必須になるため初期DDLへ固定しない。

## 7. C1-C4 — Clean install matrix

同一のpackage済みFramework artifactsと実PostgreSQLで次を確認する。

| Profile | Expected Framework migrations / tables |
|---|---|
| Audit only | Audit 1、history 1 |
| Identity | Audit 1 + Identity 8、history 1 |
| Session JDBC | Audit 1 + Identity 8 + Session 2、history 1 |
| Reference | Session JDBCと同じ11 table。Reference-owned table / migrationは0 |

各profileでmigration件数、version順、checksum success、再起動no-op、列型、nullable、PK / FK / UNIQUE / CHECK、
明示indexおよびhistory分離を検査する。

## 8. C1-C5 — Supported upgrade boundary

### Proposed supported baseline

P2-C1で正式に保証するupgrade起点は、Phase 1bの承認済みmerge commit
`40d16f9dbf26a7ba88ac13b2e3728075e0eff2a7`が示す二階層Flyway / Customer schema状態とする。
Phase 2 B1〜B4の途中commitは正式releaseではなく、production support baselineにはしない。

upgrade fixtureは次を確認する。

1. Phase 1b相当のCustomer history / table / seed rowを作る。
2. Framework historyとCustomer historyを混在させない。
3. 現行Session JDBC profileを適用し、Audit→Identity→Sessionの順で3 migrationを成功させる。
4. 既存Customer table、history、rowおよびchecksumが不変であることを確認する。
5. 二度目の起動がno-opとなり、Framework / Customer双方のhistoryがsuccessだけであることを確認する。

P2-C1では次を保証しない。

- B1〜B4途中commit間のupgrade
- downgrade
- checksum変更済みmigrationからの回復
- 複数versionのApplication processを同時稼働するrolling upgrade
- Oracleまたは未採用DB
- Customer migrationの内容・version設計

## 9. C1-C6 — Initializer and failure contract

- Spring Session initializerは`never`を維持し、既定`SPRING_SESSION*` table生成を拒否する。
- JPA schema generationはmigrationの代替にせず、検証Applicationではschema validationを使用する。
- migration validation、checksum、version conflictまたはownership混在はstartup failureとする。
- Framework migration failure後にCustomer migrationを実行しない。
- Customer migration failureをFramework成功と混同せず、それぞれのhistoryから原因を確認できるようにする。
- `baselineOnMigrate`の現行値はPhase 1b upgrade fixtureで実測し、既存非empty schemaを黙って正当化する範囲を拡大しない。

## 10. C1-C7 — Tooling and Evidence placement

`build-support/security-foundation-verification`へP2-C1専用fixture / scriptを追加する候補とする。
fixtureは非配布とし、production SQLをcopyして別正本を作らず、package済みStarter JARのmigration resourceを実行する。

検証層は次の順とする。

1. SQL / JAR inventoryとversion uniqueness
2. focused Maven tests
3. clean install profile matrix
4. Phase 1b supported upgrade fixture
5. Root Reactor / Null Safety / Public API回帰
6. P2-B4 / Gate B aggregateの必要範囲の回帰
7. residual container / process / directory cleanup

P2-C1単独でworkflow、required check、secret、environmentまたはsnapshot publishを変更しない。Gate Cでremote接続を別途判断する。

## 11. Proposed work breakdown

### C1-1 — Contract / inventory review

- 本書C1-C1〜C7をArchitecture Ownerが判断する。
- 承認前にproduction SQL、test fixture、scriptまたはCIを変更しない。

### C1-2 — Audit migration / static inventory

- 承認versionでAudit production migrationを追加する。
- 3 migrationの一意性、順序、artifact ownership、fixture非混入を検査する。

### C1-3 — PostgreSQL clean / upgrade verification

- clean install 4 profileとPhase 1b supported upgradeを実PostgreSQLで検証する。
- table / column / constraint / index / history / initializer negative inventoryを記録する。

### C1-4 — Closeout

- focused、Root Reactor、Null Safety、Public API、Gate B回帰とcleanupを実行する。
- P2-C1 EvidenceとOwner承認を記録し、P2-C2 package / Consumer contract reviewへ引き渡す。

## 12. Architecture Ownerへ求める判断

1. 新moduleを追加せず、各owner Starterにmigrationを置き、Data Starterを共通実行機構に限定するか。
2. Audit migrationをIdentityより前の`V2026090300`候補とし、共有history上で依存順を保つか。
3. Audit production schemaを`koiki_audit_event` 1 tableに限定し、fixture table / failure constraintや未使用indexを含めないか。
4. Audit / Identity / Session / Referenceのclean install matrixと二階層history検査を採用するか。
5. supported upgrade起点をPhase 1b承認済みDB状態とし、Phase 2途中commit、downgrade、rolling upgradeを対象外とするか。
6. Spring Session initializer禁止、JPA schema generation非依存、migration失敗時startup failureを維持するか。
7. package済みStarter resourceを使う非配布Tooling fixtureを正本とし、CI変更をGate Cへ残すか。

推奨結論はC1-C1〜C7を上記案で承認し、C1-2 Audit migration / static inventoryへ進むことである。
