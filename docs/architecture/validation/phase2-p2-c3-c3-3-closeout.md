# Phase 2 P2-C3 C3-3 local aggregate closeout

## 1. Status and boundary

- 実施日: 2026年9月12日
- 作業パッケージ: `P2-C3 / C3-3`
- verification identity: `28b0220bc6bfbee1b7b9c01721717e9d5017ec7e`
- branch: `feature/phase2-p2-c1-postgresql-migration`
- status: `COMPLETE / LOCAL VERIFIED / ARCHITECTURE OWNER APPROVED`
- Ownership: Architecture Evidence / Tooling
- production artifact / Framework Public API / production migration / workflow change: 0
- remote operation: 0 (`NOT APPROVED / NO-GO`)

C3-2をcommitしたclean HEADに対し、C3-1 §6のlocal aggregateとC3-2のEngineer-facing journeyを再実行した。
Gate A、P2-C2、P2-C1、Gate B 3ラウンド、Root Reactor、Null Safety、inventory、sensitive-outputおよびcleanupは
すべて成功した。C3-3はlocal Evidenceの確定までを対象とし、Phase 2完了またはremote受入れを主張しない。

## 2. Preconditions and verification identity

| Item | Observation |
|---|---|
| Git HEAD | `28b0220bc6bfbee1b7b9c01721717e9d5017ec7e` |
| worktree | clean |
| Java | build / target 21、runtime 21 / 25 |
| Docker | Docker Engine 29.5.3、TestcontainersからPostgreSQL 17を使用 |
| Maven repository | 各Consumer / aggregateが空の一時repositoryを使用 |
| start inspection | repository / process / container / fixture target残留なし |

前回中断時の実行セッションは終了済みで最終exitを回収できなかったため、その途中結果をC3-3の最終証拠には採用しなかった。
host accessによる`-InspectOnly`が成功したclean startから、Gate B closeout全体を再実行した。

## 3. Commands and results

次の既存scriptを正本として順に実行した。新しい全部入りfixtureまたはaggregate wrapperは追加していない。

```powershell
pwsh -NoProfile -File build-support/security-foundation-verification/verify-gate-a-security-foundation.ps1
pwsh -NoProfile -File build-support/security-foundation-verification/verify-p2-c2-package-static.ps1
pwsh -NoProfile -File build-support/security-foundation-verification/verify-p2-c2-consumer.ps1
pwsh -NoProfile -File build-support/security-foundation-verification/verify-p2-c2-public-api.ps1
pwsh -NoProfile -File build-support/security-foundation-verification/verify-p2-c2-publish-dry-run.ps1
pwsh -NoProfile -File build-support/openrewrite-feasibility/verify-p2-c2-openrewrite.ps1
pwsh -NoProfile -File build-support/security-foundation-verification/verify-p2-c1-migration-static.ps1
pwsh -NoProfile -File build-support/security-foundation-verification/verify-p2-c1-postgresql.ps1
pwsh -NoProfile -File build-support/security-foundation-verification/verify-p2-b4-closeout.ps1 `
  -ExpectedHead 28b0220bc6bfbee1b7b9c01721717e9d5017ec7e
./mvnw.cmd --batch-mode --no-transfer-progress `
  -f build-support/null-safety/verification/pom.xml clean
pwsh -NoProfile -File build-support/security-foundation-verification/verify-p2-b4-closeout.ps1 `
  -InspectOnly -ExpectedHead 28b0220bc6bfbee1b7b9c01721717e9d5017ec7e
```

| Scope | Result | Principal observation | Approx. elapsed |
|---|---|---|---:|
| Gate A | SUCCESS | 72 / 72、isolated Consumer、Java 21 / 25同一JAR、Public API正負fixture、Root、secret scan、cleanup | 約5分 |
| P2-C2 package | SUCCESS | formal 14 projects / 11 JAR、Reference / Tooling除外 | 1分00秒 |
| P2-C2 Consumer | SUCCESS | isolated package、Java 21 / 25、PostgreSQL、migration no-op / failure guard | 約2分 |
| P2-C2 Public API | SUCCESS | 11 JAR / 24型 / full signature / nullness、互換・破壊・追加fixture | 約1分30秒 |
| P2-C2 publish dry run | SUCCESS | 13座標 / 24 payload SHA-256 / aggregate signature / 11 same-source japicmp | 約3分 |
| P2-C2 OpenRewrite | SUCCESS | before / after、idempotence、transformed test、manual action、非配布 | 約1分30秒 |
| P2-C1 static | SUCCESS | migration 3 / Framework table 11 | 約50秒 |
| P2-C1 PostgreSQL | SUCCESS | 4 clean profiles、Phase 1b upgrade、failure contract | 約2分 |
| Gate B closeout | SUCCESS | 6工程×3周、Root Reactor、Null Safety、inventory、secret scan、cleanup | 約35分30秒 |
| final Null Safety clean | SUCCESS | fixture target除去 | 0.4秒 |
| final InspectOnly | SUCCESS | repository / residual-resource inspection | 2.2秒 |

Gate Bの各周ではAudit 31 / 31、Identity 56 / 56、Session core 72 / 72を再現した。Web two-processではA / B間継続、
正常・DELETE失敗Identity mutation、logout safe failure / recovery、proxy-aware Cookie、片系停止後継続を確認した。
non-web cleanupではexpired-only、exit 0 / 10、競合、crash recoveryを確認し、package済みReferenceではAC-P2-01 / 02と
DoD 2-10、対象Session失効、control continuity、Business Audit / Session failure rollbackを確認した。

## 4. Inventory and DoD ledger

| Contract | Final value | Result |
|---|---:|---|
| formal Framework release unit | 14 projects | MATCH |
| Root Reactor | 15 projects / 104 tests | PASS |
| publish candidate | 13 coordinates | MATCH |
| distributed JAR | 11 | MATCH |
| aggregate Public API | 24 types | MATCH |
| Framework migration | 3 SQL | MATCH |
| Framework business-security table | 11 | MATCH |

| DoD | Final local observation |
|---:|---|
| 2-1 / 2-2 | Gate AでURL / Use Case authorizationとUI / Method Security迂回拒否を再現 |
| 2-3 / 2-4 | Gate Aでlocal / OIDC / Bearer profile、JWT検証と401 / 403を回帰 |
| 2-5 | Gate Bでpackage済み二process Session共有と片系停止後継続を3周再現 |
| 2-6 | Gate Bでlock thresholdと独立Security Auditを3周再現 |
| 2-7 | Gate Bで業務transactionとBusiness Auditの同時rollbackを3周再現 |
| 2-8 | Gate Bでcleanup single execution、競合、crash recoveryを3周再現 |
| 2-9 | Gate AでCSRF / Security Header既定と明示overrideを回帰 |
| 2-10 | Gate B package済みReferenceでFramework公開契約からのIdentity管理を3周再現 |

旧DoD 2-11 / 2-12は除外済みのまま維持し、OracleをPhase 2へ戻していない。

## 5. Engineer-facing and human-operable acceptance

C3-2の中央Developer Journey、root / Architecture / Development / Starter / Consumer / verification / Referenceの入口を
同じHEADで確認した。入口、Parent / BOM / Starter、local / OIDC / Bearer profile、Customer Ownership、公開契約、secure defaults、
diagnostics、Application向け日常loop、Framework保守者向けaggregate、Reference / Consumer非Template境界を順に辿れる。

代表的な人手実行経路としてGate A、P2-C2 PostgreSQL Consumer、Gate B Reference journeyを実行し、前提、成功marker、
負例の期待失敗、cleanupを観測した。Consumerは通常local Maven cacheやFramework source pathに依存せず、隔離repositoryから
package済み同一JARをJava 21 / 25で実行できた。利用者がFramework internal型、fixture APIまたは秘密値へ依存する必要はなかった。

## 6. Friction and triage

| Finding | Classification | Treatment / outcome |
|---|---|---|
| SQL placeholder `encoded_password=?`を旧secret regexが値代入と誤認 | Tooling defect | C3-3 identity commitで`?`を除外。実secret正例とplaceholder負例を確認し、全aggregateのsecret scan成功 |
| C1実行後の非配布verification `target`をGate B preflightが検出 | execution-order / cleanup friction | fail-fastを確認。生成物だけを除去後、clean startのGate B全体を再実行し、最終cleanup成功 |
| sandboxからDocker named pipe / process command lineを検査できない | execution environment | sandbox結果を証拠に採用せず、host accessで`-InspectOnly`とDocker利用検証を実行 |
| Gate B 3周が約35分で大量のDEBUG出力を生成 | usability / duration | 所要時間を改善材料として記録。現段階ではSLAやCustomer CI必須条件にしない |
| 中断済みsessionの最終exitを後から回収できない | traceability | 中間結果を不採用とし、clean startから完全再実行 |

Public API、設定契約、既定値、diagnostic messageまたはruntime behaviorのproduct defectは検出していない。
production corrective sliceは不要である。

## 7. Cleanup and non-claims

- 最終HEADはverification identityと一致し、Evidence作成前のworktreeはcleanだった。
- C3-3所有のprocess、running container、一時repository、directory、fixture targetは残っていない。
- sensitive-output検査は全aggregateで成功した。
- production artifact、Public API、migration、ADR、Skill、workflow、required check、ruleset、environment、secretを変更していない。
- protected environment作成、push、PR、main反映、workflow dispatch、remote snapshot publishは実施していない。
- Gate A / Gate Bの過去remote成功を、このC3-3 source commitのremote成功とは扱わない。
- `PHASE 2 COMPLETE`または`ACCEPTED`を主張しない。

## 8. Architecture Owner review points

1. verification identity `28b0220...`のclean HEADで、C3-1 §6のlocal aggregateが成立したと認めるか。
2. Gate A、P2-C1、P2-C2、Gate B 6工程3周、Root Reactor、Null Safety、sensitive-output、inventory、cleanupの結果を受け入れるか。
3. Engineer-facing acceptanceとhuman-operable journeyが、公開契約、隔離package、Java 21 / 25、PostgreSQLおよび診断・cleanupまで成立したと認めるか。
4. §6のfrictionをTooling / execution usabilityとして受け入れ、production corrective sliceを不要とするか。
5. formal 14 / Root 15 / publish 13 / JAR 11 / Public API 24 / migration 3 / table 11を最終local inventoryとするか。
6. C3-3を`COMPLETE / LOCAL VERIFIED`、P2-C3を`COMPLETE / LOCAL VERIFIED / GATE C READY`としてlocal closeoutするか。
7. Gate C remote Evidence前はPhase 2を完了扱いせず、push、PR、main、ruleset、environment、dispatchおよびpublishを
   引き続き`NOT APPROVED / NO-GO`とするか。
8. snapshot publishを、final main CI成功後に一度実施し、remote取得・payload SHA-256・aggregate signatureおよび
   same-source `japicmp`の成功を確認するPhase 2最終受入れ条件とするか。

推奨結論は上記8点を承認し、P2-C3を`COMPLETE / LOCAL VERIFIED / GATE C READY`としてcloseすることである。
次はGate C-1のremote plan reviewであり、本承認文案にremote操作の実行権限は含めない。

2026年9月12日、Architecture Ownerは1〜7を提案どおり承認した。verification identityに対するlocal aggregate、
Engineer-facing acceptance、human-operable journey、最終inventory、sensitive-outputおよびcleanupの結果を受け入れ、
§6のfindingはTooling / execution usabilityとして扱い、production corrective sliceを不要と判断した。C3-3を
`COMPLETE / LOCAL VERIFIED / ARCHITECTURE OWNER APPROVED`、P2-C3を
`COMPLETE / LOCAL VERIFIED / GATE C READY`としてlocal closeoutする。

8は、snapshot publishをfinal main CI成功後に一度実施するPhase 2最終受入れ条件として確定した。Gate C-1では
final source commit、PR / mainの順序、protected environmentおよび一回限りのdispatchを実行前に個別reviewする。
publish後は同一workflow run / source commit、座標別resolved snapshot value、24 payload SHA-256、aggregate signatureおよび
11 same-source `japicmp`をremote Evidenceとして確認し、Gate C-3でPhase 2の最終判定を行う。本承認はpush、PR、main反映、
ruleset / environment変更、workflow dispatchまたはremote snapshot publishの実行権限を含まず、引き続き
`NOT APPROVED / NO-GO`とする。
