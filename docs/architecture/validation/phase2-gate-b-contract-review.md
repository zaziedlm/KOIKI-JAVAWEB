# Phase 2 Gate B 契約レビュー

## 1. ステータスと対象

- 作成日: 2026年9月9日
- Architecture Owner承認日: 2026年9月10日
- Gate: Phase 2 / Milestone B / Gate B
- baseline commit: `4931f83`
- status: `CONTRACT REVIEW COMPLETE / ARCHITECTURE OWNER APPROVED`
- 所有者: Tooling / Architecture Evidence / CI Policy
- product実装変更: なし

本reviewは、P2-B1〜B4の承認済み実装・検証を変更せず、Gate Bのaggregate / PR境界、Public API方針、
CI候補およびrequired化条件を確定する。workflow、remote state、rulesetまたはsecretは本reviewでは変更しない。

## 2. Evidence baseline

| 対象 | 承認済み証拠 | Gate Bへの接続 |
|---|---|---|
| P2-B1 Audit | 実PostgreSQL、31-test contract、Business / Security Audit rollback対比 | DoD 2-6、2-7 |
| P2-B2 Identity | 56-test contract、認証・lock・管理mutation・optimistic lock | DoD 2-6、2-7 |
| P2-B3 Session | T0〜T5 72 tests、2 process、logout、cleanup / single execution | DoD 2-5、2-8 |
| P2-B4 Reference | package済みJAR、AC-P2-01 / 02、Method Security、Audit / Session failure rollback | DoD 2-10 |
| B4-5 closeout | 上記6工程の3連続成功、Root 15 / 15、104 tests、Null Safety、inventory、cleanup | Gate B local aggregate候補 |

## 3. DoD trace review

### 3.1 DoD 2-5

同一package済みJARを2 processで起動し、process Aで作成したSessionをprocess Bで継続でき、A停止後もBで継続することを
HTTP、DBおよびprocess状態から確認済みである。Reference journeyでもtargetだけの全Session失効とadmin / control継続を確認した。

### 3.2 DoD 2-6

unknown / bad credential、並行失敗、閾値到達、account lock、automatic unlock、およびSecurity Audit失敗時のfail-closedを
実PostgreSQLで確認済みである。Security Auditは独立transactionであり、業務transaction rollbackへ巻き込まれない。

### 3.3 DoD 2-7

Business Auditは対象のIdentity mutationと同一transactionで記録される。Audit INSERT失敗およびSession DELETE失敗では、
Identity変更とBusiness Auditがrollbackされることをpackage済みReference journeyから外部観測した。

### 3.4 DoD 2-8

non-web cleanup processで期限切れSessionだけを削除し、winner / contender、exit code 0 / 10、PostgreSQL advisory lock、
winnerのOS kill後のlock解放とretryを確認済みである。

### 3.5 DoD 2-10

Reference `identity` Tier 1 moduleがFramework Public `IdentityQuery` / `IdentityAdministration`だけを使用し、Reference-owned
Repository / migrationを持たずにFramework所有Identity / Audit / Session tableを操作することを確認済みである。

## 4. Gate B判断事項

### GB-C1 — local aggregateの正本

| 候補 | 評価 |
|---|---|
| **A — B4-5 closeoutをGate B local aggregateとして再利用** | **推奨。** B1〜B4、Root、Null Safety、inventory、sensitive-output、cleanupをすでに単一入口で累積する |
| B — Gate B専用aggregateを複製 | 非推奨。同じ検証順とcleanup規則が二重化し、将来差異を生む |
| C — 各HarnessをCI YAMLから個別実行 | 非推奨。順序、clean HEAD、round間cleanupの契約を失う |

推奨: `verify-p2-b4-closeout.ps1`をlocal / CI共通の正本とし、Gate B専用の重複scriptを作らない。

### GB-C2 — CI jobの責務と名称

| 候補 | 評価 |
|---|---|
| **A — 独立した`Local Identity Session Audit Integration` job** | **推奨。** 既存Phase 1b `Milestone B Integration`と責務を区別できる |
| B — 既存`Milestone B Integration`へ追加 | 非推奨。Phase 1b RuntimeとPhase 2 Securityの失敗原因・実行時間・required statusが混在する |
| C — 通常`Verify`へ追加 | 非推奨。PostgreSQL、複数process、長時間journeyを通常unit / architecture回帰と混在させる |

推奨jobは`ubuntu-24.04`、Temurin 21、PowerShell 7、Docker利用可能runner、`contents: read`だけを使用する。
package、environment、secret権限は追加しない。

### GB-C3 — remote実行回数とtimeout

localでは補正済み同一commit上で3連続aggregateが成功済みである。CI候補を含むfinal HEADではlocal aggregateを再確認する。
その後、同一final HEADのremote integration jobを3回連続成功させ、required化reviewの入口とする。Gate Aで承認した
remote 1回の例外は、外部DB、containerまたは常駐processを使用しないMilestone A固有の判断であり、Gate Bへ継承しない。

local実績は6工程だけで約11分×3ラウンドであり、その後にRoot ReactorとNull Safetyが続く。初期timeoutは余裕を含む
`60 minutes`を推奨し、remote実測後に短縮可否をOwner reviewする。timeout拡大だけで停止やcleanup不備を隠さない。

### GB-C4 — Public API inventoryと`japicmp`方針

既存`Public API Compatibility` jobは、Phase 1a C1でpublished baselineが存在するArchitecture Contract / ArchUnit Rulesを
固定timestampおよびSHA-256で比較している。この検査は変更せず維持する。

Phase 2のAudit 6型、Identity 10型、Session 3型は各承認済みHarnessの完全一致inventoryで保護する。一方、これらには比較可能な
過去のpublished artifactがないため、Gate Bで架空のold JARを作って`japicmp`成功を主張しない。Gate Bでは現inventoryを
次回published baseline候補として承認し、実際のbaseline公開と以後のbinary compatibility比較はP2-C2 / publish reviewへ接続する。

### GB-C5 — sensitive-outputとresource cleanup

CI候補は子Harnessのartifact / log / Surefire report走査を再利用し、credential、Cookie、Authorization、encoded password、
HMAC key、email形式PII等の非露出を検査する。各roundと最終処理で次を必須とする。

- P2-B所有container prefixの残留0
- Audit / Identity / Session / Referenceの一時directory 6 patternの残留0
- 非配布fixture target 3件の残留0
- child process残留0
- HEAD不変およびclean worktree

GitHub Actionsのpost-job成功表示だけをcleanup証拠にせず、aggregate自身の検査成功を必須にする。

GB-2では、子Harnessの途中失敗時にも最終残留検査を独立実行し、cleanup検査で本来の検証失敗を隠さないことを
受入条件とする。aggregate stepのtimeoutをjob timeoutより短く設定し、`if: always()`相当の最終検査へ実行余地を残す。
timeoutまたは強制cancelで検査を完了できない場合は、runner破棄だけを成功証拠にせず、未観測として明示する。

### GB-C6 — required check化の判断順序

次の順序を推奨する。

1. 本contract reviewをArchitecture Ownerが承認する。
2. workflow候補を実装し、local final aggregateを成功させる。
3. Architecture Ownerがpush / PRを個別承認する。
4. 同一final HEADのremote jobを3回連続成功させ、各回の実行時間、権限、cleanup、既存check回帰を記録する。
5. Architecture Ownerがrequired化を別途承認する。
6. main rulesetへcheckを追加し、PR mergeとmerge後main CIを確認する。

remote successはrequired化の必要条件だが、十分条件ではない。job名の変更はruleset status名へ影響するため、required化後は
互換性変更として扱う。

### GB-C7 — Gate BとMilestone Cの境界

Gate BではP2-B1〜B4のaggregate、package済みjourney、Public API方針、CI / PR / required checkを扱う。
Framework Flyway正本の全体集約、第三者table一覧、clean install / supported upgradeはP2-C1へ残す。
Gate Bのためにmigration ownershipまたはapplication semanticsを変更しない。

## 5. Architecture Ownerへ求める判断

| ID | 推奨判断 | status |
|---|---|---|
| GB-C1 | B4-5 closeout scriptをGate B local / CI aggregate正本として再利用する | `APPROVED` |
| GB-C2 | 独立`Local Identity Session Audit Integration` job、Ubuntu / Java 21、`contents: read`とする | `APPROVED` |
| GB-C3 | final local成功後、同一HEADのremote jobを3回連続成功させ、初期timeoutを60分とする | `APPROVED` |
| GB-C4 | published 2 artifactの既存比較を維持し、Phase 2 APIはinventoryをbaseline候補として承認する | `APPROVED` |
| GB-C5 | sensitive-outputと所有resource cleanupをaggregate自身の必須条件とする | `APPROVED` |
| GB-C6 | remote success後にrequired化を別Owner判断し、merge後main CIまでGate B Evidenceへ含める | `APPROVED` |
| GB-C7 | migration集約・upgradeをP2-C1へ残し、Gate Bでproduct semanticsを変更しない | `APPROVED` |

2026年9月10日、Architecture OwnerはGB-C1〜C7を推奨案どおり承認した。GB-C5については、途中失敗時の最終残留検査と
一次失敗を保持するerror handlingをGB-2の受入条件とする。これによりGB-1を`COMPLETE`とし、次の作業をGB-2
「Gate B CI候補実装」とする。workflowのremote実行、push、PRおよびrequired check化は、定めた個別Owner判断より前に実施しない。
