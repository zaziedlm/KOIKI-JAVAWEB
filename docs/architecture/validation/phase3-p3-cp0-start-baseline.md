# Phase 3 P3-CP0 start baseline

## 1. Status

| Item | Result |
|---|---|
| Date | 2026年9月13日 |
| Architecture Owner | Shuichi Kataoka |
| Phase / CP | Phase 3 Reference Vertical Slice / P3-CP0 |
| Status | COMPLETE / P3-A0 READY |
| Branch | `feature/phase3-reference-vertical-slice` |
| HEAD / start main | `c88b335efdd556613c9ef7f4c5267214fdb8254b` |
| `main` / `origin/main` | start mainと一致 |
| Phase 2 published artifact baseline | `af7b4f71d885fe4991e5fcf85fcf8888aec6a539` |
| Production implementation | 未開始 |
| OpenSpec | Repositoryに存在しない |

## 2. Authorization and purpose

Architecture Ownerは2026年9月13日、
`docs/development/KOIKI-JavaWeb-FW_Phase3実行計画_v0.1.md` §18でGate P3-1を承認した。
P3-CP0は、その承認をRepositoryのAgent導線と開始Evidenceへ反映するための文書CPである。

この承認はP3-CP0から実行計画の順に進むことを許可するが、後続のblocking review、Milestone Gate、
Public API、dependency、migration、workflowまたはremote操作を一括承認するものではない。

## 3. Work positioning

```text
Phase / status: Phase 3 / P3-CP0 COMPLETE / P3-A0 READY
Ownership: Repository guidance and Architecture Evidence
Target Maven module: none in P3-CP0; koiki-reference-app from later CPs
Business ownership: Reference master / expense
Tier: master Tier 1 SIMPLE / expense Tier 2 RICH
Validation: document, Git identity and scope checks
Deferred decisions: P3-A0 / B0 / C0 / C3 and Remote Gate
```

## 4. Confirmed baseline

- Phase 0、Phase 1a、Phase 1b、Phase 2は`COMPLETE / ACCEPTED`である。
- Phase 3開始baselineは`c88b335efdd556613c9ef7f4c5267214fdb8254b`であり、branch、HEAD、
  `main`、`origin/main`が一致する。
- Reference Applicationは単一Maven moduleで、master、expense、Reference migration、HTMX、
  最小REST APIおよび`koiki-starter-web-mvc`は開始baselineに存在しない。
- Phase 2のSecurity、Identity、Audit、Session契約を再利用し、P3-CP0では変更しない。
- Spring Modulith 2.1.1はLevel 0 test scopeのbaselineを維持し、runtime dependencyを追加しない。
- root reactorは14 module、正式Framework release unitは14 projects / 11 JAR、Phase 2 publish unitは
  Root aggregatorを除く13座標という承認済みinventoryを維持する。

## 5. P3-CP0 deliverables

| Deliverable | Result |
|---|---|
| Phase 3 execution plan and Gate P3-1 record | APPROVED / recorded |
| Root `AGENTS.md` | Phase 3の薄い導線へ更新 |
| Project Overview Skill | 現在Phase / Gate状態だけを最小同期。業務設計規則の更新はP3-C4へ延期 |
| Start Evidence | 本書を作成 |
| Production code / Public API | 変更0 |
| Maven module / dependency | 変更0 |
| Production migration | 変更0 |
| Workflow / remote setting | 変更0 |
| Remote push / PR / merge / publish | 実施0 |

## 6. Blocking reviews preserved

| Review | Must decide before |
|---|---|
| P3-A0: active master contract、部門scope Ownership、migration / FK | P3-A1 / A2 / A3のproduction変更 |
| P3-B0: MVC / HTMX artifact、dependency、asset、browser runner | P3-B1〜B3の関連変更 |
| P3-B4: cache対象、TTL、staleness | cache実装 |
| P3-C0: 最小REST API契約 | REST production code |
| P3-C3: MyBatis fixture Public API review | Public API変更 |
| Remote Gate | push / PR / merge、workflow / ruleset、dispatch、publish |

## 7. Verification result

| Check | Result |
|---|---|
| Branch / HEAD | `feature/phase3-reference-vertical-slice` / `c88b335efdd556613c9ef7f4c5267214fdb8254b` |
| `main` / `origin/main` | HEADと一致 |
| Java | Eclipse Temurin 21.0.12.1 |
| Maven Wrapper | Apache Maven 3.9.16 |
| Docker | Client 29.5.3-rd。daemon停止中 |
| OpenSpec | absent |
| Approval marker | execution planにstaleな`PENDING`なし |
| Changed scope | execution plan、`AGENTS.md`、Project Overview Skillの状態記述、本Evidenceだけ |
| Maven / browser / DB test | 文書CPのため未実施。P3-A0もcontract reviewに限定する |

Docker daemon停止は、production実装もDB検証も行わないP3-CP0の完了を妨げない。
必要な実DB検証を開始するCPで環境を再確認する。

## 8. Exit decision

P3-CP0の成果物はGate P3-1の承認内容と一致し、production変更0のexit criteriaを満たす。
P3-CP0を`COMPLETE`、P3-A0を`READY / NOT STARTED`とする。

次の作業はP3-A0のread-only contract reviewに限定する。Architecture OwnerがP3-A0の契約、
Ownershipおよびmigration / FK方針を承認するまで、P3-A1以降のproduction実装を開始しない。
