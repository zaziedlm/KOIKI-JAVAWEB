# Phase 2 P2-C3 C3-2 Engineer-facing Developer Journey

## 1. Status and boundary

- 実施日: 2026年9月12日
- 作業パッケージ: `P2-C3 / C3-2`
- 開始baseline: `5bfb2272315a146005c106fb385a43380d09c97f`
- branch: `feature/phase2-p2-c1-postgresql-migration`
- Architecture Owner承認日: 2026年9月12日
- status: `COMPLETE / ARCHITECTURE OWNER APPROVED`
- Ownership: Documentation / Tooling Evidence
- production artifact / Framework Public API / production migration / workflow change: 0

C3-2は、C3-1で承認されたEngineer-facing acceptanceを、Repositoryの入口からStarter選択、業務module設計、
public seam、secure defaults、diagnostics、verification、Reference / Tooling境界まで辿れる文書経路へ反映する。
機構の最終再実行は、C3-2差分をcommitしたclean HEADに対するC3-3が所有する。

## 2. Implemented documentation path

| Surface | C3-2 result |
|---|---|
| Repository entry | root READMEをPhase 2現在状態へ更新し、Developer Journeyへ接続 |
| central journey | `docs/development/phase2-developer-journey.md`を新設 |
| dependency selection | Parent / BOM、Security / Audit / Identity / Session StarterとApplication dependencyを分離 |
| profile selection | local Session、OIDC Session、Bearer APIの用途とCustomer-owned設定を比較 |
| business structure | Ownership → module → Public boundary → Tier → responsibility placementの順序をSkillへ接続 |
| secure defaults | default deny、CSRF / Header、local auth明示enable、Session initializer禁止、JPA validateを明記 |
| diagnostics | 401 / 403、HMAC、Identity administration、Session、Flyway / JPA、OIDC、Bearerの確認順を追加 |
| verification | Customerの日常loopとFramework acceptance Harnessを分離 |
| Reference | Tier 1 identityの正規利用例と、Project Templateではない境界を明記 |
| Consumer | package / runtime Evidenceと、Customerへcopyしないfixture境界を明記 |
| Architecture index | Phase 2のGate / CP状態と未完了remote境界を追加 |
| Harness index | C2-6 / C2-7 / P2-C3入口を追加 |

## 3. Human-operable journey

中央Journeyは、内部script名から読み始めなくても次の順で判断できる構成とした。

1. 現在のartifact availabilityとremote未公開を確認する。
2. Customer Ownership、業務module、Tier、責務を決める。
3. Parent / BOMと必要なStarterだけを選ぶ。
4. local Session / OIDC Session / Bearer APIのprofileを選ぶ。
5. Framework Public contractだけからUse Caseを構成する。
6. secure defaultsとsecret供給境界を維持する。
7. 代表的な失敗を診断し、Framework欠陥を文書workaroundで隠さない。
8. Application向け検証loopを実行する。
9. 必要な場合だけFramework保守者向けHarnessで配布境界を再現する。
10. 未提供範囲と次のGateを確認する。

P2-C3全体のHarnessは長時間・Repository固有であるため、Customer CIへそのまま要求しない。Customerは利用Starter、業務risk、
構成およびdeployment形態に応じてfocused / real PostgreSQL integrationを選ぶ。

## 4. Friction found and treatment

| Finding | Classification | Treatment |
|---|---|---|
| root READMEがPhase 1b現在形のまま | entry / discoverability | Phase 2進行状態、release境界、Developer Journeyを追加 |
| Architecture indexにPhase 2がない | documentation index | Gate F / A / B、C1 / C2 / C3を追加 |
| Starter説明が個別で選択順が見えない | dependency selection | central matrixとStarter indexを追加 |
| profileごとのCustomer責務が分散 | profile selection | local / OIDC / Bearer comparisonへ集約 |
| Consumer / Referenceをcopy元と誤認し得る | ownership / limits | Tooling Evidence / formal Reference / non-templateを明記 |
| Customerの日常testとFramework aggregateが混在 | verification usability | 2つの検証loopを分離 |
| C2-7先頭statusがOwner承認前のまま | evidence consistency | `COMPLETE / ARCHITECTURE OWNER APPROVED`へ修正 |

確認範囲では、Public API、設定契約、diagnosticまたはruntime behaviorのproduct defectは検出していない。
そのためproduction corrective sliceは起票せず、C3-2を文書変更だけに限定した。

## 5. Explicit non-claims

- central JourneyはProject Templateまたは完成済みCustomer Applicationではない。
- fixture user、route、credential、migrationおよびfailure switchを正式成果物へ昇格していない。
- OIDC / BearerのCustomer設定やprovisioning policyをFramework既定へ固定していない。
- remote snapshot、Repository外一般取得、final PR / mainおよびPhase 2完了を主張しない。
- Phase 3以降、Phase 5、Authorization Server、SAML、Redis、WebFlux、Oracleまたはcloud固有Adapterを先行していない。

## 6. C3-3 handoff

C3-2のOwner承認後に本差分をcommitし、そのclean HEADをC3-3 verification identityとする。C3-3ではC3-1 §6のとおり、
Gate A、P2-C1、P2-C2、Gate B 3ラウンド、Root Reactor、Null Safety、sensitive-output、最終inventoryおよびcleanupを実行する。
中央Journeyのlocal link、command、前提、期待観測点も同じcommitで再確認する。

## 7. Architecture Owner review points

1. root READMEからPhase 2 Developer Journey、Architecture index、Starter / Consumer / Referenceへ到達できる入口でよいか。
2. Parent / BOM、Starter、Security profile、Application dependencyおよびCustomer Ownershipの選択説明は業務アプリ開始に十分か。
3. secure defaultsとdiagnosticsが、回避策でproduct defectを隠さないEngineer-facing境界になっているか。
4. Applicationの日常verificationとFramework保守者向けaggregateを分離し、Customer CIへ過大なHarnessを要求しないか。
5. Referenceを正規利用例、ConsumerをTooling Evidenceとし、いずれもProject Templateではない境界を承認するか。
6. C3-2にproduction artifact / Public API / migration / workflow変更がないことを確認し、文書commit後にC3-3へ進めてよいか。

推奨結論は上記6点を承認し、C3-2を`COMPLETE / ARCHITECTURE OWNER APPROVED`としてcommitした後、clean HEADで
C3-3 local aggregate / human-operable journey verificationへ進むことである。remote操作は本承認に含めない。

2026年9月12日、Architecture Ownerは上記承認文案を確認し、6判断を提案どおり承認した。Repositoryからの入口、
Parent / BOM、Starter、Security profile、Application dependency、Customer Ownership、Framework Public contractの利用境界、
secure defaults、diagnostics、Application verificationとFramework aggregateの分離、およびReference / Consumerの
非Template境界を受け入れる。C3-2を`COMPLETE / ARCHITECTURE OWNER APPROVED`とし、本差分をcommitしたclean HEADで
C3-3 local aggregate / human-operable journey verificationへ進める。

production artifact、Framework Public API、production migrationおよびworkflowの変更は本承認に含めない。protected environment作成、
push、PR、main反映、workflow dispatch、remote snapshot publishおよびrequired check / ruleset変更も承認対象外とし、
引き続き`NOT APPROVED / NO-GO`とする。
