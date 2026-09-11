# Phase 2 P2-C1 C1-2 Audit migration / static inventory verification

## 1. Status

- 検証日: 2026年9月11日
- 作業パッケージ: `P2-C1 / C1-2`
- 状態: `COMPLETE / LOCAL VERIFIED — C1-3 READY`
- baseline: main merge commit `8873942b3c8b9c83f08b607f9fd03ebbad928324`
- branch: `feature/phase2-p2-c1-postgresql-migration`
- Ownership: Framework Audit Migration / Tooling Evidence
- production change: Audit production migration 1件

C1-1で承認されたC1-C1〜C7に従い、Audit Starterへproduction migrationを追加し、package済みAudit、Identity、
Session JDBC StarterのFramework migration 3件についてversion、artifact ownership、table inventoryおよびfixture非混入を
静的に検証した。実PostgreSQL clean install 4 profileとPhase 1b supported upgradeはC1-3へ維持する。

## 2. Production migration

`koiki-starter-audit`へ次を追加した。

```text
db/migration/koiki/V2026090300__create_koiki_audit.sql
```

作成するのは`koiki_audit_event` 1 tableだけであり、14列の型、長さ、nullableおよびprimary keyを
`AuditEventEntity`と一致させた。初期query要件のないindex、enum相当CHECK、外部キー、
`fixture_business_change`、`fixture_audit_failure`および`FORCE_AUDIT_FAILURE`は含めていない。

## 3. Static inventory Harness

`build-support/security-foundation-verification/verify-p2-c1-migration-static.ps1`を非配布Tooling正本として追加した。
Harnessはproduction SQLを複製せず、次を検査する。

1. production source上のFramework migrationが承認済み3 pathだけであること。
2. IdentityがAuditへ、Session JDBCがIdentityへ直接依存すること。
3. versionが一意で`2026090300`、`2026090301`、`2026090701`の単調順であること。
4. package済み各Starter JARがowner migrationだけを含むこと。
5. Audit 1、Identity 8、Session 2の合計11 tableが重複なくowner artifactへ属すること。
6. Audit DDLがEntity列契約と一致し、fixture固有DDL、未使用index、先行CHECKを含まないこと。
7. 非配布verification artifactとReference Applicationが隔離release repositoryへ入らないこと。

## 4. Verification result

次を実行した。

```powershell
pwsh -NoProfile -File build-support/security-foundation-verification/verify-p2-c1-migration-static.ps1
```

| Layer | Result |
|---|---|
| PowerShell parser | error 0 |
| `git diff --check` | success |
| focused Audit reactor | 3 modules、`BUILD SUCCESS`、39.404秒 |
| Session dependency closure staging | 7 modules、`BUILD SUCCESS`、17.846秒 |
| packaged migration inventory | 3 migrations / 3 unique versions |
| packaged table inventory | 11 unique Framework tables |
| Audit fixture / deferred DDL negative inventory | 0 |
| Tooling / Reference release repository leakage | 0 |
| Harness final result | `succeeded (3 migrations / 11 tables)` |

Maven起動時に、異なるHotSpot buildで作成されたCDS archiveのwarningが出たが、Java 21 toolchain選択、compile、package、
installおよびHarness assertionはすべて成功した。C1-2はDocker、PostgreSQL container、workflow、remote state、secret、
rulesetまたはsnapshot publishを変更していない。

## 5. C1-3 handoff

C1-3では、同一のpackage済みStarter resourceと実PostgreSQLを使い、Audit only、Identity、Session JDBC、Referenceの
clean install 4 profile、再起動no-op、列 / constraint / index / history、Spring Session initializer negative inventory、
Phase 1b supported upgradeおよび失敗時契約を実証する。C1-2の静的HarnessをSQL正本の前段検査として再利用する。
