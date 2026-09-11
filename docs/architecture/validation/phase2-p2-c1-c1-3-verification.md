# Phase 2 P2-C1 C1-3 PostgreSQL verification

## 1. Status

- 検証日: 2026年9月11日
- 作業パッケージ: `P2-C1 / C1-3`
- 状態: `COMPLETE / LOCAL VERIFIED — C1-4 READY`
- branch: `feature/phase2-p2-c1-postgresql-migration`
- supported upgrade baseline: `40d16f9dbf26a7ba88ac13b2e3728075e0eff2a7`
- Ownership: Tooling Evidence（production migration追加なし）

C1-2で確定したpackage済みFramework migration 3件をPostgreSQL 17へ適用し、clean install 4 profile、
Phase 1b supported upgrade、再起動no-opおよび失敗時契約を実証した。C1-3では正式Starter、Referenceまたは
Customer migrationを複製せず、非配布fixtureとHarnessだけを追加した。

## 2. Verification Harness

`build-support/security-foundation-verification/verify-p2-c1-postgresql.ps1`は、正式Framework release unitを一時的な
隔離Maven repositoryへstageし、実PostgreSQL containerと次のprofileを検証する。

| Profile | Framework history entries | Framework tables | Reference-owned migration |
|---|---:|---:|---:|
| Audit only | 1 | 1 | 0 |
| Identity | 2 | 9 | 0 |
| Session JDBC | 3 | 11 | 0 |
| package済みReference | 3 | 11 | 0 |

`Framework history entries`は`koiki_flyway_history`内の成功したSQL migration履歴件数であり、
history table数ではない。Framework history tableは全profileで1 table、Customer historyは
別の`flyway_schema_history` 1 tableとして分離される。

実PostgreSQLから取得したFramework application tableの物理inventory集計は次のとおりである。
PK、FK、UNIQUEおよびCHECKをconstraint件数へ、PK / UNIQUEに伴うindexと明示indexをindex件数へ含める。

| Profile | Columns | Constraints | Indexes |
|---|---:|---:|---:|
| Audit only | 14 | 1 | 1 |
| Identity | 59 | 36 | 16 |
| Session JDBC | 69 | 39 | 21 |
| package済みReference | 69 | 39 | 21 |

各profileでversion順、checksum、成功history、table ownership、列 / nullability、constraint / index総数、JPA `validate`、
再起動後のhistory不変を確認した。Session closureでは`spring.session.jdbc.initialize-schema=never`、
`koiki_session`および`SPRING_SESSION`非生成を確認した。Referenceはpackage済みJARを2回起動し、Reference自身にSQLがなく、
Session JDBC Starterを通じて同じ3 migrationだけが適用されることを確認した。

## 3. Supported upgrade

承認済みPhase 1b baselineのCustomer migration resourceをそのまま適用してCustomer history、tableおよびseed rowを作成した後、
現行Session dependency closureを起動した。次を確認した。

1. Customer historyのversion、description、checksumおよび成功状態が変わらない。
2. Customer tableとseed rowが保持される。
3. Framework専用historyへ`2026090300`、`2026090301`、`2026090701`だけが追加される。
4. 2回目の起動がno-opとなる。
5. baseline以後にPhase 1b Customer migration resourceの変更がない。

Phase 1b相当状態の構築では、Framework / Customer双方について現行値の
`baselineOnMigrate=true`、`baselineVersion=0`を適用して挙動を実測した。空のFramework historyには
SQL migration entryがなく、既存Framework history tableによって非emptyとなったschemaへCustomer migrationを適用すると、
Customer historyだけにversion `0`の`<< Flyway Baseline >>` entryが作成された。その後Customer SQL migration
version `1`、`3`、`4`が適用され、現行Session dependency closureの2回の起動後もbaseline entryを含むCustomer history全体が
不変であることを確認した。これにより、既存非empty schemaをbaselineとして扱う範囲を承認済みPhase 1b境界から拡大していない。

## 4. Failure contracts

非配布fixtureで次を観測した。

- Framework migration失敗時はCustomer migrationへ進まない。
- Customer migration失敗後も成功済みFramework historyを保持する。
- Customer migrationのchecksum不一致はstartup failureとなる。
- `spring.session.jdbc.initialize-schema=always`への上書きはstartup failureとなり、既定の`SPRING_SESSION` tableを生成しない。

初回のHarness試行ではPostgreSQL公式imageの初期化再起動中にrole作成が競合したため、readiness後の短い再試行を追加した。
また失敗fixtureのpropertyをdefault propertyで渡していたためcommand-line propertyへ修正した。いずれもHarness固有の問題で、
production migrationまたはStarterの契約不良ではない。修正後のaggregateは全項目成功した。

## 5. Result

次を実行した。

```powershell
pwsh -NoProfile -File build-support/security-foundation-verification/verify-p2-c1-postgresql.ps1
pwsh -NoProfile -File build-support/security-foundation-verification/verify-p2-b1-audit-transaction.ps1
```

| Layer | Result |
|---|---|
| formal Framework release unit | 14 projects、`BUILD SUCCESS` |
| package済みReference | `BUILD SUCCESS`、Reference migration 0件 |
| clean install | 4 / 4 profiles success |
| restart no-op | 4 / 4 profiles success |
| Phase 1b supported upgrade | success |
| failure contracts | 4 / 4 success |
| C1-3 Harness final result | `succeeded (4 clean profiles / Phase 1b upgrade / failure contracts)` |
| P2-B1 Audit回帰 | T0–T4 31 / 31、累積fixture 72 / 72 tests success |
| PowerShell / fixture POM / diff | parser success / XML success / `git diff --check` success |
| residual container | 0 |

一時PostgreSQL container、Reference process、隔離Maven repository、runtime credentialおよび非配布targetはHarness終了時に
cleanupする。workflow、remote state、ruleset、required checkまたはsnapshot publishは変更していない。

## 6. C1-4 handoff

C1-4ではC1-2 static inventoryとC1-3 PostgreSQL aggregateを前提に、focused、Root Reactor、Null Safety、Public API、
Gate B回帰、repository状態、残留resourceおよびsensitive outputをcloseout観点で確認する。C1-3のfixtureを正式artifact、
`koiki-testing`、ReferenceまたはCustomer ownershipへ移動しない。
