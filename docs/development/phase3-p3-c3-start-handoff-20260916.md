# Phase 3 P3-C3開始検討・延期判断引継ぎ

> **Owner decision（2026年9月16日）:** P3-C3は
> `DEFERRED — MyBatis adoption trigger required`とする。本書のfixture / rule案は実装承認ではなく、
> 将来のadoption Gateで再reviewする検討記録として保存する。判断Evidenceは
> `../architecture/validation/phase3-p3-c3-mybatis-deferral.md`を正本とする。

## 1. Handoff status

- 作成日: 2026年9月16日
- Architecture Owner: Shuichi Kataoka
- branch: `feature/phase3-reference-vertical-slice`
- approved baseline commit: `9f2247f9be3c4d6a71b0355d7b6d0de0364b05eb`
  （P3-C2 critical journey E2E close承認）
- Phase status: `P3-C0〜C2 COMPLETE / OWNER APPROVED — P3-C3 DEFERRED — P3-C4 READY`
- Primary ownership: Architecture documentation（将来再開時の候補はFramework contract / rules + Tooling）
- Reference production change: 0
- deferred Tooling proposal: `build-support/mybatis-convention-verification`（未作成）
- blocking condition: MyBatis adoption trigger成立後のOwner contract review
- immediate next action: P3-C4 Journey / ADR / Skill / DoD traceへ進む

本handoffは、MyBatis accountingまたは正式MyBatis Starterを実装する導線ではない。開始準備では、
分離Persistence Modelを採るCustomer-likeな最小fixtureによるconverter、`@MybatisTest`、楽観lock、
`reconstitute`およびread model境界の実証案を検討した。Owner延期判断により実装せず、fixture、schema、SQL、
Public APIおよびdependencyを追加しない。検討案は採用トリガー成立時の再review入力としてだけ使用する。

## 2. Required reading order

1. `AGENTS.md`
2. `docs/agent/skills/koiki-project-overview/SKILL.md`
3. `docs/agent/skills/koiki-business-feature-work/SKILL.md`
4. 本書
5. `KOIKI-JavaWeb-FW_Phase3実行計画_v0.1.md`のdecision 15、Milestone C、§13〜17
6. `../architecture/grand-design/KOIKI-JavaWeb-FW_グランドデザイン_v0.2.md`の§11.7、§12.5、§21.3
7. `../architecture/adr/README.md`のADR-022、ADR-023、ADR-039
8. `../architecture/validation/phase1a-archunit-rules.md`
9. `../architecture/validation/phase1b-cp7-domain-event-mybatis.md`
10. `../../koiki-architecture-contract/src/main/java/org/koikifw/architecture/PersistenceModel.java`
11. `../../koiki-archunit-rules/src/main/java/org/koikifw/archunit/BusinessModuleRuleSet.java`

## 3. Work positioning

```text
Phase / status: Phase 3 / P3-C0〜C2 COMPLETE / OWNER APPROVED / P3-C3 DEFERRED / P3-C4 READY
Ownership: Architecture documentation（将来候補: Framework contract / rules + Tooling fixture）
Target: 現在は文書だけ。将来候補はkoiki-architecture-contract、koiki-archunit-rules、非配布Tooling
Verification: 現在は整合確認。再開時はArchUnit fixture + @MybatisTest + PostgreSQL Testcontainers
Deferred: Reference accounting、正式MyBatis Starter、Project Template、Phase 4、workflow / remote
```

P3-C3開始時点では`PersistenceTechnology.MYBATIS`とMyBatis starter `4.1.0`のBOM管理だけが存在する。
`PersistenceModel`は`SHARED`だけであり、Rule 8は`MYBATIS`を意図的に拒否している。Rule 25〜27と
Rule 30〜37は未実装である。このため、Rule 35〜37の追加だけで`MYBATIS`を有効化すると、分離modelと
MyBatis固有の前提規則を機械検査しない状態が生じる。実装着手前に§6のscopeを確定する。

## 4. Deferred fixture proposal

Root Reactor外の非配布Toolingとして、既存Reference業務語彙と衝突しない最小Tier 2 moduleを置く。
fixtureは次の構造と責務を持つ。

```text
<fixture-module>/
├── application/query       # Query PortとApplication所有read model
├── domain/model            # JPA非依存aggregate、id、version、reconstitute
├── domain/repository       # Repository Port
└── adapter/outbound/persistence/
    ├── entity              # MyBatis永続化record / POJO
    ├── converter           # 手書きDomain ⇔ entity変換
    ├── mapper              # MyBatis Mapper
    ├── query               # read modelを直接materializeするQuery Port実装
    └── *RepositoryAdapter  # converter利用と更新件数による楽観lock判定
```

fixtureは次を実証する。

1. `persistence = MYBATIS`と`persistenceModel = SEPARATED`をmodule単位で宣言する。
2. Mapperはpersistence entityまたはscalarだけを受け渡し、`domain.model`をsignatureへ出さない。
3. converterだけが`public static reconstitute(...)`を呼び、復元時に業務不変条件を再検証しない。
4. Repository Adapterは`WHERE id = ? AND version = ?`の更新件数0を
   Spring標準`OptimisticLockingFailureException`へ変換する。
5. Query AdapterはconverterやDomain Modelを経由せず、Application所有read modelを直接返す。
6. `@MybatisTest`とPostgreSQL Testcontainersでcreate / load / update、stale version conflict、
   converter復元およびread model mappingを検証する。
7. fixture専用schema / SQLはtest resourceに閉じ、Flyway migrationやReference DBへ追加しない。

## 5. Deferred rule verification proposal

Rule 35〜37はpositive fixtureと、1規約につき最低1つのnegative fixtureで検証する。

| Rule | 機械検査する境界 | 代表negative fixture |
|---|---|---|
| 35 | `adapter.outbound.persistence.mapper`配下のmethod parameter / return typeに`domain.model`を含めない | MapperがDomain Modelを直接返す |
| 36 | `adapter.outbound.persistence.jpa`配下のSpring Data Repository method parameter / return typeに`domain.model`を含めない | 分離JPA用dummy repositoryがDomain Modelを直接受ける |
| 37 | `adapter.outbound.persistence.query`から`converter`または`domain.model`へ依存しない | Query Adapterがconverterを介してread modelを作る |

Rule 37の「SQL実行手段から直接生成」は、静的検査ではQuery Adapterからconverter / Domainへの依存禁止として
表現し、実際の直接mappingは`@MybatisTest`で確認する。新しいannotation、marker interfaceまたはFramework DTOは
導入しない。

## 6. Reviewed alternatives retained for a future adoption Gate

以下は開始準備時の提案であり、2026年9月16日のOwner判断では実装承認していない。
`SEPARATED`、Rule 25〜27 / 30〜37、fixtureおよびdependencyはすべてdeferredとする。

### D1. `PersistenceModel.SEPARATED`の追加

**開始準備時の推奨（未採用）: 承認する。** `MYBATIS`を正規のmodule宣言として検証するには、
`koiki-architecture-contract`のpublic enumへ`SEPARATED`を追加する必要がある。これは加算変更でも
Java Public API変更であるため、inventory、Javadoc、Rule 8、compatibility testを同じ変更単位に含める。

### D2. ArchUnit rule scope

**開始準備時の推奨（未採用）: Rule 25〜27、30〜37を一体で実装対象とする。** 実行計画のP3-C3表記はRule 35〜37だが、
現在は分離modelのRule 25〜27とMyBatisのRule 30〜34も未実装である。`SEPARATED` / `MYBATIS`を許可しながら
前提規則を未検査にするより、§21.3で定義済みの関連規則を同じcontract reviewで閉じる。
これは新規設計ではなく、既存グランドデザインのdeferred ruleを有効化するscope補正として記録する。

OwnerがP3-C3をRule 35〜37だけへ厳密に限定する場合は、Rule 8の`MYBATIS`拒否を維持し、fixtureは
正式module metadataを有効化しない検証専用構成とする。その場合、`SEPARATED`のPublic API追加と
MyBatis moduleの正式許可は後続CPへdeferし、P3-C3 Evidenceに制約を明記する。

### D3. Rule 37の検査可能な解釈

**開始準備時の推奨（未採用）: Query Adapter → converter / domain.model依存禁止 + integration testによる直接mappingとする。**
bytecodeだけでSQL mapper内部の実行経路を完全に証明しようとせず、静的境界とruntime evidenceを組み合わせる。

### D4. MyBatis test dependency

**開始準備時の推奨（未採用）: 非配布Toolingのtest scopeだけに`mybatis-spring-boot-starter-test`を置く。** 実効versionは既存の
MyBatis Spring Boot `4.1.0` baselineと整合させる。正式BOM項目またはStarter依存への追加は行わず、
実装時にMaven effective dependencyと`@MybatisTest`の成立を確認する。

### D5. Runtime / persistence choices

**開始準備時の推奨（未採用）: PostgreSQL 17、手書きconverter、Spring標準`OptimisticLockingFailureException`を使用する。**
H2、MapStruct、独自optimistic-lock例外、cache、transactional eventまたは追加ORMを導入しない。

### D6. Distribution boundary

**開始準備時の推奨（未採用）: fixtureは非配布・Root Reactor外を維持する。** Public API対象はD1のenumだけとし、fixtureのDomain、
Mapper、converter、Repository Adapter、schema、SQLおよびtest helperをFrameworkへ公開しない。

## 7. Proposed implementation sequence only after a future adoption approval

1. D1〜D6のOwner判断をP3-C3 validation文書へ記録する。
2. 承認scopeに従ってarchitecture contractとPublic API inventoryを更新する。
3. Rule 8を`MYBATIS + SEPARATED`だけ許可するよう更新し、metadata組合せのpositive / negative testを追加する。
4. 承認されたdeferred ruleを`koiki-archunit-rules`へ実装し、rule単位のpositive / negative testを追加する。
5. `build-support/mybatis-convention-verification`へ最小fixtureと`@MybatisTest`を実装する。
6. focused test、Root Reactor `clean verify`、Public API compatibilityおよびTooling integration testを実行する。
7. Evidence、依存tree、Public API差分、Rule検出結果、楽観lock結果をvalidation文書へ記録する。
8. Owner close review後にcommit pointを閉じ、P3-C4より先へ進まない。

## 8. Required verification when reopened

- Public API差分が承認された`PersistenceModel.SEPARATED`だけである。
- Rule 8がJPA / SHARED、JPA / SEPARATED、MYBATIS / SEPARATEDの承認matrixを正しく扱い、
  MYBATIS / SHAREDを拒否する。
- 承認scopeの各ArchUnit ruleにpositive / negative testがあり、違反messageがrule番号、ADR、危険、修正方針を示す。
- Mapper / JPA RepositoryのsignatureにDomain Modelが流出しない。
- read modelがconverter / Domainを経由せず、SQL結果から直接materializeされる。
- converterだけが`reconstitute`を使用し、業務生成と永続化復元を分離する。
- 正常更新とstale version更新0件の双方を実PostgreSQLで検証する。
- Tooling fixtureが正式artifact、Root Reactor、Reference Application、migrationまたはProject Templateへ混入しない。
- Root Reactor、Public API compatibilityおよび既存Reference回帰がPASSする。

## 9. Stop conditions

- D1〜D6の承認前にPublic API、dependencyまたはArchUnit ruleを変更する。
- Rule 35〜37だけを追加して、`MYBATIS`を無条件に許可する。
- `koiki-reference-app`のexpenseをMyBatisへ置換する、またはaccounting moduleを先行実装する。
- fixtureのschema / SQLを正式migrationへ追加する。
- converterにreflection-based mappingを導入する、またはread modelをconverter経由で生成する。
- optimistic lockの更新件数0を正常終了として扱う、または独自Framework例外を先行追加する。
- fixtureを正式Starter、`koiki-testing`、Project Templateまたは配布artifactへ含める。
- Ownerの個別承認なしにworkflow、required check、push / PR / merge、rulesetまたはsnapshot publishを変更する。
- adoption triggerとOwner blocking reviewなしにP3-C3実装を再開する。
- Phase 4 `accounting`開始と同時に、暗黙にMyBatisを有効化する。

## 10. Start readiness

2026年9月16日に次を確認した。

- HEAD `9f2247f9be3c4d6a71b0355d7b6d0de0364b05eb`
- P3-C2は`COMPLETE / OWNER APPROVED`、DoD 3-11実CI PASSだけRemote Gate / Gate Cへ継続
- `PersistenceTechnology.MYBATIS`は存在し、`PersistenceModel.SEPARATED`は未提供
- Rule 8は`MYBATIS`を拒否し、Rule 25〜27 / 30〜37は未実装
- MyBatis starter `4.1.0`はBOM管理済みだが、`@MybatisTest` fixtureは未作成
- worktreeは本handoff作成前にclean、branchはremoteより3 commit ahead

2026年9月16日、§6の案を直ちに実装せず、P3-C3を
`DEFERRED — MyBatis adoption trigger required`とするOwner判断を記録した。次はP3-C4へ進む。
将来§6を再reviewするまでは、production code、Public API、dependency、migration、fixture、Rule 8、
workflowおよびremoteを変更しない。
