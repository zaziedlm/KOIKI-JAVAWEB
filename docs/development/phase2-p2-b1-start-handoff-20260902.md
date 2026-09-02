# Phase 2 P2-B1 start handoff

## 1. Purpose and status

本書は、Phase 2 Milestone A / Gate Aの承認済み成果物をbaselineとして維持し、Milestone Bの最初のCPである
P2-B1 Audit contract / transaction fixtureを安全に開始するためのhandoffである。

- **Handoff date:** 2026年9月2日
- **Architecture Owner:** Shuichi Kataoka
- **Branch:** `feature/phase2-security-local-identity-session-audit`
- **Start commit:** `7f63bc1234aa7f79416e36dd8c15c1da0ab6987c`（PR #28 merge commit）
- **Phase status:** `Gate A COMPLETE / ACCEPTED — B1-1 APPROVED / B1-2 READY`
- **Ownership:** Framework（Audit contract / transaction semantics）+ Tooling（非配布PostgreSQL fixture）
- **Production target:** Owner承認済みの単一`koiki-starter-audit`。再開後のB1-2まで生成しない
- **Verification target:** T4 Audit transaction fixture、実PostgreSQL

## 2. Required reading order

1. `AGENTS.md`
2. `docs/agent/skills/koiki-project-overview/SKILL.md`
3. 本書
4. `KOIKI-JavaWeb-FW_Phase2実行計画_v0.1.md`の§2、§4 Milestone B、§7〜§8
5. `../architecture/validation/phase2-gate-a-security-foundation.md`
6. `../architecture/validation/phase2-security-test-design.md`のT4、N-01、N-06〜N-09、CI境界
7. `../architecture/validation/phase2-security-semantics-fitting.md`のAudit semantics / O-4
8. `../architecture/grand-design/KOIKI-JavaWeb-FW_グランドデザイン_v0.2.md`の§15、§17、§20、§23.2、DoD 2-6 / 2-7
9. `../architecture/adr/README.md`
10. `../architecture/validation/phase1b-cp5-observability.md`はconsole structured loggingの成立済みEvidenceとして参照する
11. `../architecture/validation/phase1b-cp8-single-execution.md`は再利用可能なEvidenceとして参照し、codeをcopyしない

## 3. Gate A accepted baseline

- PR #28をmergeし、merge commit `7f63bc1`に対するmain CI 5 jobsとJava runtime CI 2 jobsがすべて成功した。
- `Security Foundation Integration`はmain rulesetのrequired checkであり、strict policyとbypassなしを維持する。
- `koiki-starter-security`はdefault deny、CSRF / Security Header、local login / Session、URL / Method Security、
  OIDC Login、Bearer JWTのSpring標準seamを成立させた。
- Security StarterのPublic Java型、configuration property、KOIKI error codeはGate A時点で0件である。
- Authorization Server、SAML、Redis、WebFlux、Oracle、AWS固有Adapter、production identity / Session / Audit
  persistenceはGate Aへ混在させていない。

P2-B1開始時のlocal baselineは次のとおりである。

| Verification | Result |
|---|---|
| Maven / Java | Maven 3.9.16 / Java 21.0.12.1 |
| Root `verify` | Reactor 11 / 11 success、Architecture Contract 4 tests、ArchUnit Rules 66 tests success |
| Gate A aggregate | T0〜T3 23 tests、Consumer 1 test、Java 21 / 25同一JAR、Public API fixture success |
| Gate A final marker | `Phase 2 Gate A local Security aggregate succeeded.` |

## 4. P2-B1 scope and exit criteria

```text
Phase / status: Phase 2 / Milestone B / P2-B1 READY
Ownership: Framework Audit contract + internal persistence、Tooling PostgreSQL fixture
Primary layer: T4 Audit transaction boundary
Database: PostgreSQL Testcontainers
Exit criteria: DoD 2-6 / 2-7のrollback対比、Audit failure semantics、Public API型単位review
```

P2-B1の核心は、監査データの保存有無を実PostgreSQLのtransaction結果として観測することである。

- **Business audit:** Application Use Caseの業務transactionへ参加し、業務rollback時は対象変更と監査rowが共に消える。
- **Security audit:** `REQUIRES_NEW`の独立transactionで保存し、呼出元transactionのrollbackに巻き込まれない。
- **Side effect / integration:** Auditではない。Spring Modulith Level 2や非同期通知をP2-B1へ追加しない。
- Business audit書込み失敗時は業務処理もrollbackする。
- Security audit書込み失敗時は、login成功、reset token発行、管理解除をfail closedとする。
- logout、disable、invalidationは防御操作を継続し、監査失敗をalert可能な失敗として残す。具体的業務経路はB2 / B3で累積検証する。

DoD 2-6のaccount lock自体はP2-B2で実装する。P2-B1では、独立Security auditが外側transaction rollback後も残る
transaction contractを先に実証し、P2-B2のlock / attempt制御を先行しない。DoD 2-7はBusiness auditとfixture-ownedな
業務変更を同一transactionでrollbackし、両rowが残らないことを実証する。

## 5. Contract and artifact review boundary

現時点のproduction codeにはAudit API / persistence実装がなく、正式成果物一覧にもAudit専用artifactは存在しない。
したがって、次を実装前に型単位でreviewする。

| Review item | Question |
|---|---|
| Entry point | Business / Securityを別interfaceにするか、単一interfaceの別operationにするか |
| Audit value | 必須項目を1つのimmutable valueへまとめるか。nullable optional fieldを無制限に増やさないか |
| Actor | immutable Framework user ID、system、anonymousをどう表現し、email / external subjectを排除するか |
| Transaction | transaction annotationをPublic APIへ露出せず、internal implementationで意味を保証できるか |
| Failure | callerがfail-closed / continue + alertを明示的に扱える最小contractは何か |
| Artifact | 既存Starterへの混在、新規runtime artifact、別の構成をOwnershipと依存方向で比較できるか |
| Persistence | P2-B1の実証に必要な最小内部model / repositoryは何か。production migrationをC1より前に追加しないか |
| Logging boundary | AuditのDB保存とApplication log出力を別責務とし、log backendやファイル出力へtransaction semanticsを依存させていないか |

`koiki-architecture-contract`はbuild-timeのArchitecture metadataを所有するため、runtime Audit APIの置き場として扱わない。
`koiki-starter-security`へBusiness auditまで置く案も自明に採用しない。新規artifact案は、P2-B1で即利用される型と実装があり、
空の`-api` / `-impl`を作らない場合だけ候補にできる。

## 6. Audit data boundary

グランドデザイン§15.4の標準項目を候補集合とし、P2-B1で全項目を無条件Public API化しない。

- event type、actor、subject、resource、action、result、reason、occurred time
- request / trace / session correlation
- client IP、User-Agent
- before / afterの安全な要約

Password、token、secret、raw unknown email、claim全文、任意のPII全文を監査row、Application log、response、test report、
artifactへ記録しない。IP / User-Agentのretention、masking、access controlはR-05としてCustomer / Production Baselineへ残す。

### 6.1 Application log / Audit logの2分類

ログ種別は次の2つとし、Securityを第3のログ種別として追加しない。Securityは記録対象の性質であり、目的に応じて
Application log、Audit logまたは双方に現れる横断的観点とする。

| Log type | Purpose / destination | Security-related content |
|---|---|---|
| **Application log** | 障害解析と運用監視。既存Observability Starterの低優先度既定により構造化してconsoleへ出力し、収集先はDeploymentが構成する | 認証処理エラー、認可拒否、設定異常等を、検知と調査に必要な安全な項目だけで記録する |
| **Audit log** | 操作証跡。Framework Audit APIを通じてDBへ保存し、DB rowを正本とする | Login / Logout、Account Lock、認可拒否等をSecurity auditとして独立transactionで記録する。業務操作はBusiness auditとして同一transactionで記録する |

同じSecurity関連事象を双方へ記録する場合も、Application logをAudit rowへcopyせず、それぞれの目的に必要な最小項目を
同じrequest / trace correlationで関連付ける。Application logの出力または外部収集の成否からAudit保存成功を推定せず、
Auditの成立はDB transaction結果で判定する。Password、token、cookie、Authorization header、secret、claim全文等の
出力禁止情報は、Application logとAudit logの双方へ記録しない。

### 6.2 Ownershipとdelivery境界

- **Framework:** 既存のconsole structured logging既定、Audit contract、Auditの安全な標準項目とtransaction semanticsを所有する。
- **Application / Customer:** 業務固有の操作、resource、before / after要約およびApplication logへ記録する業務情報を所有する。
- **Deployment:** stdout / stderrの収集先、CloudWatch等のbackend、retention、access control、暗号化、alertを所有する。

コンテナ実行ではApplication logをconsoleへ出力し、コンテナランタイムとDeploymentのlog collectorへ委ねる。Frameworkは
ファイル出力を必須化せず、CloudWatch SDK / appender、AWS固有設定または外部log backendへの依存をAudit transactionへ
追加しない。非コンテナ環境等で必要となるファイル出力はApplication / Deploymentのopt-inとする。

Audit保存後の要約をApplication logへ投影することは可能だが、P2-B1の成立条件には含めない。DB正本との完全配送保証、
再試行、outbox / CDC、archiveまたは外部監査基盤へのexportは、明示要件と後続Gateなしに先行設計しない。
また、`continue + alert`はCloudWatch等への同期配送成功を意味しない。P2-B1ではAudit失敗をcallerが識別できることまでを
契約対象とし、Application log / metricへの変換、alert ruleおよび通知経路は各機能とDeploymentの後続設計とする。

## 7. Verification topology

最初の候補は、既存`build-support/security-foundation-verification`をT4へ拡張し、`koiki-testing`が提供する
PostgreSQL Testcontainers支援をtest scopeで利用する構成である。fixture、test route、failure switch、synthetic business rowは
Tooling-ownedとし、Root Reactor、正式release unit、Framework Public API、`koiki-testing`へ昇格させない。

最低限、次を別scenarioとして観測する。

1. Business change + Business auditのcommitで両rowが残る。
2. Business change + Business audit後に例外を発生させ、両rowが残らない。
3. Business auditの保存失敗でBusiness changeも残らない。
4. 外側transactionからSecurity auditを記録後に外側をrollbackし、Security audit rowだけが残る。
5. Security audit自体の保存失敗を、fail-closed経路とcontinue + alert経路で区別できる。
6. actor偽装、raw email、secret / PII混入を拒否し、Audit row、fixture実行時のApplication log、reportのすべてで非露出にできる。
7. container、connection、temporary repository、reportを成功 / 失敗の双方でcleanupできる。
8. Application log collectorまたは外部backendを使わずに、Audit transactionの成否をDB rowで検証できる。

## 8. Working sequence

### B1-0 — Start preflight

- branch、HEAD、worktree、Java 21、Maven Wrapperを確認する。
- root `verify`とGate A aggregateを変更前baselineとして成功させる。
- Gate A Public API 0件とrequired checksを維持する。

### B1-1 — Type-level contract review

- API型、method、field、failure表現、artifact配置の候補を比較する。
- Spring `TransactionTemplate` / `@Transactional` / propagation構成を比較し、proxy bypassやself-invocationを避ける。
- Public API inventory増分と互換義務を明示し、Owner review前にproduction型を追加しない。
- Identity / single executionの将来Public APIを同時に作らない。

### B1-2 — T4 transaction fixture

- 承認された最小contract / internal実装だけを追加する。
- PostgreSQL Testcontainersでpositive、rollback、failure injectionを実行する。
- H2やmock transactionだけをDoD Evidenceにしない。

### B1-3 — Regression and evidence

- focused B1検証、T0〜T4 aggregate、root verify、Null Safety、Public API fixtureを実行する。
- dependency、JAR、report、logをsecret / PII観点で走査する。
- P2-B1 Evidence、contract inventory、必要なADRを更新する。
- P2-B2以降を混在させず、P2-B1を独立commit pointとする。

## 9. Explicit exclusions

- User / Role / Permission / credential / lock / reset / attemptのproduction永続化とmigration（P2-B2）
- Spring Session JDBC、2 instance、Session失効、cleanup、single execution production実装（P2-B3）
- Reference `identity`（P2-B4）
- production PostgreSQL migrationと第三者table一覧（P2-C1）
- async audit / notification、Spring Modulith Level 2
- Authorization Server、SAML、Redis、WebFlux、Oracle、AWS固有Adapter
- Audit検索API、管理画面、retention / archive / external export
- CloudWatch SDK / appender、log group、collector、IAM、暗号化、alert等のDeployment設定
- Application logのファイル出力profile、Audit要約の完全配送、outbox / CDC

## 10. Stop conditions

次の場合は実装せず、選択肢と影響をArchitecture Ownerへ提示する。

- Audit APIの公開型、artifact配置またはtransaction semanticsが型単位reviewで決まっていない。
- P2-B1のためにP2-B2 / B3 / C1のproduction table、migrationまたは業務機能が必要になる。
- Business / Security auditのtransaction差をmockやH2でしか示せない。
- fixture固有route、actor、failure switch、tableを正式artifactへ置く必要がある。
- `REQUIRES_NEW`がself-invocation等で実効化されず、外側rollback後にSecurity auditが残らない。
- raw secret、email、external subject、claim全文が監査、log、responseまたはreportへ露出する。
- Audit transactionがApplication log、ファイルまたは外部log backendの可用性へ依存する。
- 新規artifactが空、未使用、または将来用途だけで必要になる。

## 11. Next decision point

B1-1の型単位contract review案を`../architecture/validation/phase2-p2-b1-contract-review.md`へ記録した。
単一Audit Starter、Business / Security別interface、immutable Audit value、安全なfailure exception、recorder façadeと
別beanの`MANDATORY` / `REQUIRES_NEW` transaction executor、およびDB正本とApplication logの分離を推奨し、
Public API増分を6型と見積もった。

2026年9月2日、Architecture Ownerは同reviewのB1-C1〜C7を推奨案どおり承認した。承認時点でproduction source、POM、
root Reactor、BOM、Public API inventoryまたはfixtureの変更は0である。再開後の次工程はB1-2 T4 transaction fixtureとし、
実PostgreSQL Evidenceが承認案を否定した場合は実装を固定せずcontract reviewへ戻る。
