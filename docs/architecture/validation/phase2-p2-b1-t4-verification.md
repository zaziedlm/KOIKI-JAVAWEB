# Phase 2 P2-B1 T4 Audit transaction verification

## 1. Status and scope

- **Validation date:** 2026年9月3日
- **Work package:** `P2-B1 / B1-2〜B1-3`
- **Status:** `COMPLETE / ARCHITECTURE OWNER ACCEPTED / COMMIT READY`
- **Accepted by:** Shuichi Kataoka、2026年9月3日
- **Branch:** `feature/phase2-security-local-identity-session-audit`
- **Baseline HEAD:** `2bb654d`
- **Java / Maven:** Java 21.0.12.1 / Maven 3.9.16
- **Database:** PostgreSQL 17.11、Testcontainers 2.0.5

本記録は、B1-C1〜C7で承認されたAudit contractとtransaction semanticsを、正式`koiki-starter-audit`と
非配布T4 fixtureへ実装し、実PostgreSQLのtransaction結果として検証したEvidenceである。

## 2. Implemented boundary

### 2.1 Framework production scope

- 単一`koiki-starter-audit`へPublic API 6型、internal Auto Configuration、JPA Entity / store、transaction executorを追加した。
- `BusinessAuditRecorder`は別beanの`MANDATORY` transactionへ委譲し、既存業務transactionへ参加する。
- `SecurityAuditRecorder`は別beanの`REQUIRES_NEW` transactionへ委譲し、呼出元transactionから独立してcommitする。
- `EntityManager.persist + flush`により、保存失敗をtransaction完了前に顕在化させる。
- event IDと発生時刻はinternal実装が生成し、request ID / trace IDはMDCから取得する。
- Audit DB rowを正本とし、Application log、ファイル、CloudWatch等の外部backendへ依存しない。
- production Migration、Identity永続化、Session、通知、outbox / CDCは追加していない。

### 2.2 Tooling fixture scope

- 既存`security-foundation-verification`をT4へ拡張し、`koiki-testing`とPostgreSQL Testcontainersをtest scopeで利用した。
- `koiki_audit_event`相当schema、synthetic business table、保存失敗用constraintはtest resourceだけが所有する。
- fixture actor、failure path、業務変更およびPostgreSQL containerを正式artifactや`koiki-testing`へ昇格していない。

## 3. T4 observable results

| Scenario | PostgreSQLで観測した結果 |
|---|---|
| Business commit | 業務rowとBusiness audit rowが共に残る |
| Business rollback | 業務rowとBusiness audit rowが共に残らない |
| Business audit保存失敗 | callerが例外をcatchしても外側transactionはrollback-onlyとなり、両rowが残らない |
| Security audit + 外側rollback | Security audit rowだけが独立commitされ、外側の業務rowは残らない |
| Security audit保存失敗 | 固定messageかつ内部causeなしの`AuditRecordingException`として安全に識別できる |
| fail-closed / continue + alert | 後続処理を止める経路と、防御操作を完了してalert状態を残す経路を区別できる |
| Business recorderのtransaction外呼出 | 保存せず`AuditRecordingException`となる |
| correlation | caller payloadへ追加せず、MDCのrequest ID / trace IDがDB rowへ保存される |
| actor / value非露出 | raw email actorを拒否し、`toString()`はactor / event内容をredactする |

## 4. Verification results

| Verification | Result |
|---|---|
| P2-B1 aggregate | isolated release 12 / 12、dependency / artifact / API / sensitive-content検査success |
| cumulative fixture | T0〜T4 31 tests、failure / error / skip 0 |
| T4 fixture | Audit transaction 8 tests、failure / error / skip 0 |
| Root `verify` | Reactor 12 / 12、Architecture Contract 4、ArchUnit Rules 66、全件success |
| Public API inventory | 承認済み6型と完全一致、property 0、Audit error code 0 |
| Dependency boundary | Data JPA / JSpecify / SLF4J。Security / KOIKI Observability / Modulith / cloud SDK / Redis / WebFlux / SAMLなし |
| Distribution boundary | fixtureはRoot Reactorと隔離release repositoryへ未収載 |
| Sensitive-content scan | formal Audit JAR、fixture JAR、Surefire reportで該当なし |
| Cleanup | isolated Maven repositoryを削除し、`postgres:17-alpine`実行container 0件 |
| Diff hygiene | `git diff --check` success |

再現commandは次である。Dockerが利用可能であることを前提とする。

```powershell
pwsh -NoProfile -File build-support/security-foundation-verification/verify-p2-b1-audit-transaction.ps1
```

## 5. Public API and ownership review

| Contract category | P2-B1 result |
|---|---:|
| Public Java types | 6 |
| Public configuration properties | 0 |
| Public Audit error codes | 0 |
| Public Spring / JPA / transaction types | 0 |
| Public persistence Entity / Repository | 0 |

Public APIは`AuditActor`、`AuditEvent`、`AuditResult`、`AuditRecordingException`、
`BusinessAuditRecorder`、`SecurityAuditRecorder`に限定した。transaction annotation、JPA Entity、store、executor、
audit分類値はinternal packageに留めた。

Frameworkは共通Audit contract、標準項目、transaction semanticsと内部保存を所有する。Application / Customerは
業務event type、action、resourceおよび安全なreason codeを所有し、DeploymentはDB運用、retention、access controlと
外部監視先を所有する。

## 6. DoD and threat trace

| Trace | P2-B1 Evidence |
|---|---|
| DoD 2-6 preparation | Security auditの独立transactionを実証。account lock / attempt自体はP2-B2 |
| DoD 2-7 | fixture-owned業務変更とBusiness auditの同時commit / rollbackを実証 |
| O-4 | fail-closedと防御操作継続 + alertのcaller選択を識別可能にした |
| N-09 | Business / Security transaction差、actor境界、保存失敗、外側rollback後のrow有無を実DBで確認 |
| R-05 boundary | raw email actor、secret / PII patternのartifact / report非露出を確認。retention等はProduction Baselineへ継続 |

## 7. Excluded scope confirmation

- production Flyway Migrationと第三者table一覧（P2-C1）
- User / credential / Role / Permission / lock / reset / attempt永続化（P2-B2）
- Spring Session JDBC、複数instance、全Session失効（P2-B3）
- Reference業務、Audit検索API、管理画面
- Application log API、CloudWatch SDK / appender、外部監査基盤、完全配送、outbox / CDC
- 複数DataSource / transaction manager、XA、別Audit DB

## 8. Conclusion and next decision

実PostgreSQL Evidenceは、承認済みB1-C1〜C7を否定せず、`MANDATORY` / `REQUIRES_NEW`のrollback差、
保存失敗semantics、Public API 6型およびDB正本とApplication logの分離を支持した。P2-B1差分はcommit可能である。

2026年9月3日、Architecture Ownerは本EvidenceとPublic APIを確認し、P2-B1をacceptした。Audit contract / transaction判断は
ADR-047として確定した。次はP2-B2 Identity / lock / resetの開始整理へ進み、P2-B2の実装は本commitへ混在させない。
