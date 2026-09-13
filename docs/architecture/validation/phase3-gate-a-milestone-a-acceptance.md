# Phase 3 Gate A — Milestone A acceptance review

**Status:** COMPLETE / ACCEPTED — GATE A PASSED
**Review date:** 2026年9月13日  
**Branch:** `feature/phase3-reference-vertical-slice`  
**Reviewed / verified HEAD:** `85275d90139cbe8fb916ba1386288a393d189482`
**Baseline main:** `c88b335efdd556613c9ef7f4c5267214fdb8254b`  
**Next CP:** `P3-B0` MVC / HTMX contract review

## 1. Review objective

本記録はP3-A1〜P3-A4を個別成果の寄せ集めとしてではなく、Reference Applicationの
Domain / transaction / module境界を構成する一つのMilestoneとして棚卸しする。

新しい機能検証を追加したり、既に成功した同一testを理由なく再実行したりせず、各CPで得たEvidenceを
次の3区分で評価する。

1. **直接検証済み** — 対応する自動test、DB観測またはartifact inventoryがある。
2. **横断Evidenceで支持** — 一つのtestだけでは完結しないが、複数CPの検証結果と実装境界が整合する。
3. **検証外 / 後続** — Gate Aの主張に含めず、後続CPまたはOwner判断へ明示的に残す。

このreview自体ではproduction code、Public API、Maven module、migration、workflow、dependencyまたは
remote設定を変更していない。実browserで操作できる業務画面はまだ存在しないため、計画§10.3に従い
browser確認はGate A条件とせず、P3-B2から開始する。

## 2. Milestone A coherence

### 2.1 One business flow across P3-A1〜A4

Milestone Aの責務は次の一本の流れとして接続している。

```text
FrameworkPrincipal / Permission
  -> expense Applicationの所有権・承認scope
  -> masterの現在値を読む狭いQuery contract
  -> ExpenseRequest Aggregateの状態・不変条件・version
  -> 同一transactionのBusiness Audit
  -> DepartmentDeactivating同期eventによるmaster廃止のveto / rollback
```

| CP | 固定した責務 | 後続CPとの関係 | 横断評価 |
|---|---|---|---|
| P3-A1 | `master`が部門、経費科目、現在所属とその利用可否を所有 | A2がPort / Adapter越しに現在値だけを照会し、A4では廃止commandの起点となる | `master`のEntity / Repositoryを`expense`へ公開していない |
| P3-A2 | `expense`がAggregate、6状態、7遷移、INV-EXP-01〜04、versionを所有 | A3は同じUse Caseへactor認可とAuditを加え、A4は同じpending状態定義を再利用する | A3 / A4でDomain規則を重複実装していない |
| P3-A3 | Permission、所有権、完全一致scope、自己承認拒否、Business Audit原子性 | A1の所属とA2の状態を、認証主体から得たimmutable user IDへ結び付ける | Roleだけ、UIだけ、入力actor IDだけで認可していない |
| P3-A4 | UUIDだけを持つ不変な同期eventと、`expense`所有Repositoryによる未処理判定 | A1の廃止transactionをA2の4 pending状態がvetoする | command整合性をquery例外やmodule間FKへ置き換えていない |

P3-A1〜A4へ集中した結果として、それぞれが独立した別方式へ分岐した形跡はない。A2のDomainを核に、
A1の現在値、A3のcaller境界、A4のmodule間command整合性が外側から接続されており、P3-A0で承認した
Tier 1 / Tier 2、Ownership、SecurityおよびModulith Level 1の判断を維持している。

### 2.2 Command / query collaboration

P3-A0-D1で認めた二種類のmodule間連携は、目的を混同せず実装されている。

| Need | Implemented boundary | Assessment |
|---|---|---|
| 作成・編集・提出時の有効部門、所属、科目という現在値 | `master`所有のread-only Query contractを、`expense.adapter.outbound`が自module Portへ適合 | ADR-049の狭い例外条件どおり。JPA Entity / Repository / Domain Modelは露出しない |
| 部門廃止commandと、未処理申請がある場合のtransaction拒否 | `DepartmentDeactivating(UUID)`と同期listener | Eventは識別子だけを持ち、listenerは`expense`のUse Case / Repositoryへ委譲する |

現在値QueryをEventで模倣せず、command整合性を同期Queryや直接table参照で代替していない。この使い分けが、
従来のmodule間Event原則へ加えたADR-049例外を必要最小限に保っている。

### 2.3 Ownership, database and transaction

| Concern | Owner / implementation | Assessment |
|---|---|---|
| 部門、経費科目、現在所属 | `master` / V1 | `expense`からmaster tableへの直接参照なし |
| 申請、明細 | `expense` / V2 | Tier 2 JPA共有モデルのAggregate内に状態と不変条件を保持 |
| 承認者scope | `expense` / V3 | Framework Identityやmasterへ業務policyを混入させない |
| Role / Permission / immutable user ID | Framework Identity contract | 部門または承認scopeをFramework tableへ追加しない |
| Business Audit | Framework `BusinessAuditRecorder` | Reference独自audit tableを作らず、成功操作と同一transactionで記録 |
| FK | 所有module内だけ | module間およびFramework tableへのFKなし |

部門廃止後も現在所属row自体は残り得るが、利用可否Queryはactiveな部門との組合せを要求する。未処理申請が
なければ廃止できるという承認済み仕様と矛盾せず、利用者の再所属を廃止の追加条件へしていない。

## 3. Evidence coverage

### 3.1 Directly verified

| Area | Evidence | Result |
|---|---|---|
| P3-A1 master | Tier 1管理Use Case、`MASTER:ADMIN`拒否、有効性Query、V1、Business Audit、PostgreSQL観測 | 42 tests、failure / error / skip 0 |
| P3-A2 expense | 6状態、7遷移、INV-EXP-01〜04、rollback、V2、master Port / Adapter | 49 tests、failure / error / skip 0 |
| ADR-049 rule | outbound adapterから他module `contract`の`*Query` interfaceだけを許可するpositive / negative fixture | architecture rules 67 tests成功 |
| P3-A3 authorization | `EXPENSE:APPLY / APPROVE / SETTLE`、所有権、scope外非露出、自己承認、Audit failure rollback、V3 | 55 tests、failure / error / skip 0 |
| P3-A4 Level 1 | 不変event、production listener 1件、pending 4状態の拒否とrollback、terminal 2状態 / 申請なしの成功 | 57 tests、failure / error / skip 0 |
| Current compilation / artifact | Error Prone / NullAwayを含むpackage、production / test class分離 | 55 production、17 test sourceをcompile。packaged JARにtest classなし |
| Migration | PostgreSQL 17 / FlywayでV1〜V3適用、`kkref` history、module内FK | 各CPのDB観測で確認 |
| Root Reactor clean aggregate | clean HEAD `85275d9`でMaven Wrapperから`clean verify` | 15 / 15 projects、128 tests、failure / error / skip 0 |

最終57件のReference回帰はA1〜A4を含む現在形を通している。ただし、その件数だけを根拠に後続のUI、REST、
実Sessionまたは同時実行まで検証済みとはしない。

### 3.2 Supported by combined evidence

| Claim | Combined evidence | Confidence boundary |
|---|---|---|
| A4のpending判定が正しい業務状態を参照する | A4は`ExpenseStatus.pendingStatuses()`を使用し4状態 / 2終端をDB matrixで確認。A2 / A3はDomain操作で各状態遷移を確認 | A4のmatrix fixture単独ではAggregate生成経路を証明しないが、A2 / A3との組合せで支持される |
| actorの偽装を受けない | A3の入力にactor IDを置かず`FrameworkPrincipal`から取得し、Phase 2のPrincipal / Permission contractを再利用 | 実Form LoginとSessionを経たbrowser主体はP3-B2で確認する |
| master成功操作のAudit原子性 | `@Transactional` Use Case内で共通`BusinessAuditRecorder`を呼び、A4のveto時にmaster状態とAuditが共にrollback。Phase 2でRecorder contractを検証 | `deactivateDepartment`へAudit failureを注入する専用testは重複追加していない |
| migration / Ownershipの分離 | V1〜V3のtable owner、module内FK、package依存rule、実DB schema観測が一致 | 将来のread model / cacheはP3-B1 / B4で別途確認する |
| Framework境界を拡大していない | Reference側の業務実装とtest-scope `koiki-testing`利用、ADR-049の限定rule変更をinventory | ADR-049はarchitecture toolingの意図的変更。Framework runtime / Public API / production dependency追加ではない |

### 3.3 Not verified at Gate A

次をGate Aの達成主張へ含めない。

| Item | Reason / destination | Gate A treatment |
|---|---|---|
| 実Form Login、Spring Session JDBC、複数Roleの人系journey | Application method securityは検証したが業務画面が未実装 | P3-B2以降。Gate A browser条件にはしない |
| MVC / Thymeleaf / HTMX、read model、query時scope | Milestone Bの責務 | AC-P3-08およびP3-B0〜B4へ保留 |
| REST endpoint / DTO / error contract | Milestone Cの責務 | AC-P3-09 / 10およびP3-C0〜C2へ保留 |
| 二つの実Sessionによる楽観lock競合画面 | Domain version / stale拒否までは検証済みだが、画面競合は未実装 | P3-B4へ保留 |
| expense作成・提出と部門廃止が同時進行するtransaction race | AC-P3-06 / 07は「先に未処理申請が存在する」逐次scenario。cross-module FK / distributed lockは採用していない | 現時点の保証外。production concurrency guaranteeが必要なら別contract reviewが必要 |
| master廃止時のAudit recorder故障注入 | 共通transaction pattern、Phase 2 Audit契約、expense側故障注入およびA4 veto rollbackで支持される | 同型testの追加をGate条件とはしない |
| 廃止部門に残る現在所属の管理画面上の扱い | 有効性Queryでは利用不可になるが、画面運用は未実装 | P3-B1 / B2で表示・操作契約を確認 |
| log / trace IDとbrowser / Audit / DBの一連の突合 | 操作面がなく、人系checkpointをまだ構成できない | P3-B2から段階導入し、Gate B / Cで必須化 |

## 4. Semantic consistency review

### 4.1 Department snapshot timing

上位Reference仕様は「初回提出時に申請部門snapshotを固定する」と表現する。一方、Milestone A実装は完成Draftの
作成時に現在所属部門IDを保存し、提出時に同じ所属が現在も有効か再検査する。Draftの部門だけを変更する操作は
提供せず、部門を変える場合は新しいDraftを作る単純化になっている。

これは、未完成autosaveを行わない完成Draftと、RETURNED後も同じsnapshotで再提出する承認済みflowに対して
一貫している。提出前に異動した場合は古い部門のまま提出せず拒否されるため、認可や廃止制約を弱めない。ただし、
「初回提出時に初めてDBへ固定する」という厳密な意味ではなく、「作成時に候補を記録し、初回提出で再検証して確定」
という実装である。Gate Aでこの解釈をPhase 3の単純化としてOwnerが確認した。

### 4.2 Layered authorization

Permission、resource所有権、部門scope、Domain状態は同じ層へ集約されていない。

- Framework / method boundary: Permissionと認証主体
- Application: 所有者本人、承認者scope、scope外非露出
- Domain: 状態遷移、自己承認、理由、金額・明細不変条件
- Persistence: optimistic version、所有module内constraint

この分担により、A3がA2のDomainを認可専用モデルへ置き換えたり、A4 listenerが認可や状態遷移を再実装したり
していない。

## 5. Scope and change inventory

| Boundary | Milestone A result |
|---|---|
| Production ownership | `koiki-reference-app`の`master` / `expense`だけ |
| Maven module | 新設・分割なし |
| Runtime / production dependency | 追加なし |
| Test dependency | `koiki-reference-app`から`koiki-testing`をtest scopeで利用 |
| Framework Public API type / signature inventory | 追加・削除・signature変更なし |
| Framework runtime behavior | 変更なし |
| Architecture tooling behavior | `BusinessModuleRuleSet`へADR-049の狭いQuery例外を加え、fixture / testで制約を固定 |
| Migration | Reference-owned `kkref` V1〜V3。seedなし、module間 / Framework FKなし |
| MVC / HTMX / REST | 未着手 |
| Workflow / remote | 変更・実行なし |
| Project Template / Customer | 変更なし |

## 6. Gate A assessment

| Gate axis | Assessment | Status |
|---|---|---|
| Domain | A2のAggregateと状態語彙をA3 / A4が再利用し、不変条件の重複・迂回なし | READY |
| Transaction | DB更新、Audit、同期event vetoのrollback境界が一貫 | READY |
| Module | master Query例外とcommand Event原則を用途別に維持し、直接参照 / cross-module FKなし | READY |
| Security / Identity | Phase 2 contractを再利用し、業務属性はReference Ownership | READY WITH DEFERRED BROWSER INTEGRATION |
| Migration / artifact | V1〜V3とpackage / JAR inventoryは整合 | READY |
| Clean aggregate | clean HEAD `85275d9`で正式Root Reactorの15 / 15 projectsと128 testsが成功 | PASS |
| Human checkpoint | 操作面がないため計画上N/A | DEFERRED TO P3-B2 |

**Decision:** `ACCEPTED — GATE A PASSED`。

P3-A1〜A4が局所最適へ分裂して一貫性を損なった証拠はなく、Domain / transaction / module acceptanceの
主要な設計主張は成立している。§7のOwner判断は承認済みであり、GA-D2のRoot Reactor clean aggregateも
成功した。Architecture Ownerの最終closeによりGate Aを`PASSED`、P3-A1〜A4を`COMPLETE / ACCEPTED`とする。

## 7. Architecture Owner decisions

| ID | Decision | Recommendation | Status |
|---|---|---|---|
| GA-D1 | P3-A1〜A4は一つのReference business flowとしてOwnership、Domain、transaction、module境界に矛盾がない | ACCEPT | APPROVED（2026年9月13日） |
| GA-D2 | P3-A1から残るRoot Reactor clean aggregateをGate Aの最終close条件にする | REQUIRE ONE FOCUSED RUN BEFORE PASS | APPROVED（2026年9月13日） |
| GA-D3 | Draft作成時に部門IDを記録し、初回提出で再検証して確定する挙動をPhase 3の単純化として扱う | CONFIRM | APPROVED（2026年9月13日） |
| GA-D4 | 作成・提出と部門廃止の厳密な同時transaction raceはAC-P3-06 / 07の保証外として記録し、Milestone Aを阻害しない | DEFER; REVISIT IF PRODUCTION GUARANTEE IS REQUIRED | APPROVED（2026年9月13日） |
| GA-D5 | 実browser、実Session、log / Audit / DBの人系突合をGate Aへ遡及させず、操作面が成立するP3-B2から開始する | CONFIRM | APPROVED（2026年9月13日） |
| GA-D6 | master Audit故障注入の重複testを追加せず、既存の共通契約と横断EvidenceでGate Aを判定する | CONFIRM | APPROVED（2026年9月13日） |

**GA-D1 decision record:** P3-A1〜A4は、P3-A0で承認したOwnership、Tier、Domain、transaction、
ADR-049およびSpring Modulith Level 1境界を維持しており、Milestone A内に設計上の矛盾は認めない。
この承認は、後続のMVC、HTMX、RESTまたは実Session検証の完了を意味しない。

**Decided by:** Shuichi Kataoka, Architecture Owner  
**Decision date:** 2026年9月13日

**GA-D2〜D6 decision record:** Root Reactor clean aggregateをGate A PASS前の最終close条件とし、clean HEADに
対して一度実行する。完成Draftの部門IDは作成時に記録し、初回提出時の現在所属・有効性の再検証によって
snapshotを確定する。申請処理と部門廃止の厳密な同時transaction競合は現行ACの保証外として延期し、
production相当の保証が必要になった時点、遅くともPhase 5で再検討する。実browser、実Sessionおよび
log / Audit / DBの人系突合はP3-B2から開始する。master専用の重複Audit故障注入testはGate A条件としない。

**Decided by:** Shuichi Kataoka, Architecture Owner  
**Decision date:** 2026年9月13日

GA-D1〜D6のOwner reviewは完了し、GA-D2の実行結果を§8で確認した。最終Owner closeは§9に記録する。

## 8. GA-D2 clean aggregate result

| Item | Result |
|---|---|
| Verification identity | `85275d90139cbe8fb916ba1386288a393d189482`、実行前tracked worktree clean |
| Command | `.\mvnw.cmd --batch-mode --no-transfer-progress clean verify` |
| Environment | Windows 11 amd64、Maven 3.9.16、Eclipse Adoptium Java 21.0.12.1 |
| Docker | Rancher Desktop Docker Engine 29.5.3、Testcontainers 2.0.5 |
| Reactor | 15 / 15 projects `SUCCESS` |
| Tests | 27 Surefire report files、128 tests、failure 0、error 0、skip 0 |
| Reference | 57 tests、failure 0、error 0、skip 0 |
| Database | PostgreSQL 17.11。Framework migration 3件、Reference V1〜V3を適用 |
| Duration | 1分14秒、終了コード0、`BUILD SUCCESS` |
| Cleanup | 実行後tracked worktree clean。Testcontainers由来の稼働中・残存containerなし |

P3-A1で観測したWindows単一Reactorの上流module `target/classes`欠落は再現せず、cleanからReferenceの
55 production sourceと17 test sourceをcompileし、Reference testまで同一Reactor内で完走した。
したがってGA-D2のclose条件は満たされた。

非blocking observationとして、Mockito inline mock makerの将来JDKに関するdynamic agent warningと、
ArchUnit test classpathのSLF4J NOP warningが出力された。いずれもtest failure、skip、production runtime変更、
またはGate AのDomain / transaction / module境界不成立を示すものではない。

## 9. Architecture Owner final close

Architecture Ownerは、GA-D1〜D6の承認結果およびclean HEAD
`85275d90139cbe8fb916ba1386288a393d189482`に対するRoot Reactor clean aggregateの成功を確認し、
Phase 3 Gate Aを`PASSED`、P3-A1〜P3-A4を`COMPLETE / ACCEPTED`とする。

Milestone Bの最初の作業はP3-B0 contract reviewとし、MVC / HTMXに関するproduction変更、Maven module、
Public API、Starter、外部library、dependencyまたはbrowser runnerをP3-B0の個別Owner承認より前に追加しない。

**Decision:** APPROVED — GATE A PASSED / MILESTONE A COMPLETE / ACCEPTED
**Decided by:** Shuichi Kataoka, Architecture Owner
**Decision date:** 2026年9月13日
**Next CP:** P3-B0 MVC / HTMX contract review

## 10. Evidence sources

- `docs/architecture/validation/phase3-p3-a0-contract-review.md`
- `docs/architecture/validation/phase3-p3-a1-master-vertical-slice.md`
- `docs/architecture/validation/phase3-p3-a2-expense-vertical-slice.md`
- `docs/architecture/validation/phase3-p3-a3-authorization-audit.md`
- `docs/architecture/validation/phase3-p3-a4-level1-synchronous-event.md`
- `docs/reference/KOIKI-JavaWeb-FW_Reference_Application_Specification_v0.1.md`
- `docs/development/KOIKI-JavaWeb-FW_Phase3実行計画_v0.1.md`
