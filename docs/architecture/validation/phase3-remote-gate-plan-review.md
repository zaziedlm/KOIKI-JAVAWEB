# Phase 3 Remote Gate plan review

## 1. Status and boundary

| Item | Result |
|---|---|
| Review date | 2026年9月17日 |
| Planning identity | `ee84312beda89a9430a4e2f4451a827d4c0aa863`（P3-C4 Owner承認commit） |
| Branch | `feature/phase3-reference-vertical-slice` |
| Status | `REMOTE CI EVIDENCE RECORDED / DoD 3-11 CI PASS / RG-6 OWNER DECISION READY` |
| Ownership | Repository governance / CI Tooling / Architecture Evidence |
| DoD continuation | DoD 3-11 `CI PASS — GATE C ACCEPTANCE PENDING` |
| Remote mutation in planning review | 0 |

P3-C4は`COMPLETE / OWNER APPROVED`である。Remote Gateは、P3-C2で受け入れたcritical journey CI候補を
GitHub-hosted runnerで実行し、DoD 3-11の実CI PASSをGate Cへ入力するためのworkflow、push、PR、ruleset、
mergeおよび失敗時境界を個別判断するblocking reviewである。

本reviewではlocal GitとGitHub API / CLIからremote状態をread-onlyで確認した。workflow source、ruleset、
remote branch、PR、workflow run、main、packageまたはenvironmentは変更していない。

## 2. Read-only local and remote inventory

| Subject | Current fact | Remote Gate treatment |
|---|---|---|
| Repository | `zaziedlm/KOIKI-JAVAWEB`、PUBLIC、default branch `main` | 維持 |
| Local worktree | clean | plan作成開始時の基準 |
| Local HEAD | `ee84312beda89a9430a4e2f4451a827d4c0aa863` | planning identity。workflow実装後のfinal identityではない |
| Remote `main` | `c88b335efdd556613c9ef7f4c5267214fdb8254b` | local branchのancestor |
| Phase 3 branch relation | `main`より33 commits / 195 files先 | 1本のPhase 3 final PRへまとめる |
| Tracking branch | remote HEAD `0d5c59c7595219aacf28f3d49de0385e6bf6e560`より10 commits / 51 files先 | forceなしのfast-forward push候補 |
| Existing PR | 対象branchのopen / closed PRなし | `main`向けdraft PRを新規作成する候補 |
| Registered workflows | `CI`、`Java Runtime Compatibility`、Phase 1b / Phase 2 snapshot publish | publish workflowは本Gateの対象外 |
| Phase 3 CI job | なし | `CI`へ独立jobを追加する候補 |
| Actions default permission | `read`、workflowからのPR approval不可 | 維持 |
| Main ruleset | `main-merge-protection`、active、strict、bypassなし | 緩和しない |
| PR rule | PR必須、approval count 0、merge / squash / rebase許可 | draft PRとGate判断で不用意なmergeを防ぐ |
| Required checks | 7 contexts | 初回remote PASS後にPhase 3 E2E追加を別判断 |

main rulesetの既存required contextsは次の7件である。

1. `Verify (ubuntu-24.04)`
2. `Public API Compatibility`
3. `Java Runtime Compatibility`
4. `Milestone B Integration`
5. `Milestone C Closeout`
6. `Security Foundation Integration`
7. `Local Identity Session Audit Integration`

対象branchへのpushだけでは`CI`を起動しない。`CI`はpull requestと`main` pushで起動するため、DoD 3-11の
remote Evidence取得にはPR作成が必要である。

## 3. Accepted local input

Remote Gateは次のlocal Evidenceを再審査せず入力として利用する。

| Evidence | Accepted result |
|---|---|
| P3-C2 critical journey | package済みJAR、Bearer API、Session Chromium、HTMX、PostgreSQL、Business / Security Audit、sanitized logを3回連続PASS |
| P3-C2 runtime | 1 journey約19秒、3回の変動約2.59% |
| C4-4 rerun | package済みcritical E2E 1 test PASS、15.81秒 |
| Cleanup | application / issuer process、PostgreSQL 17 container、dynamic port、temp log残存0 |
| Non-disclosure | token、password、Cookie、private key、source HMAC key、SQL、stack trace非出力assertion PASS |
| P3-C4 | DoD 3-1〜3-10とAC-P3-01〜10を受入済み。DoD 3-11だけを継続 |

P3-C2後にcritical E2E source、Reference production source、dependency、migration、propertyまたはprofileの
変更はない。P3-C4後のbrowser fixture補正は別Toolingの未来日入力だけであり、critical E2E契約を変更しない。

## 4. Approved local CI workflow contract

新しいworkflow fileを増やさず、既存`.github/workflows/ci.yml`へ次の独立jobを追加する。

| Contract | Approved local contract |
|---|---|
| Job ID | `phase3-critical-journey-e2e` |
| Check name | `Phase 3 Critical Journey E2E` |
| Trigger | 既存`CI`のpull requestと`main` push |
| Runner | `ubuntu-24.04` |
| Java | Temurin 21 |
| Timeout | 20分 |
| Permission | `contents: read`だけ |
| Credential | 追加secret / PAT / Packages権限なし |
| Cache | `actions/setup-java`のMaven cacheだけ。browser binaryをEvidenceとしてcacheしない |
| Test scope | Root Reactor外の`build-support/reference-e2e-verification`だけ |
| Artifact upload | なし。token、Cookie、key、process log、screenshot、traceまたはvideoをuploadしない |

jobのstep順は次とする。

1. full commit SHA固定の`actions/checkout`を`persist-credentials: false`で実行する。
2. full commit SHA固定の`actions/setup-java`でTemurin 21を設定する。
3. Tooling POMのtest classpathからPlaywright 1.62.0 CLIを起動し、`install --with-deps chromium`を実行する。
4. `./mvnw --batch-mode --no-transfer-progress -pl koiki-reference-app -am -DskipTests clean package`で
   package済みReference JARを生成する。
5. `./mvnw --batch-mode --no-transfer-progress -f build-support/reference-e2e-verification/pom.xml test`で
   accepted entrypointを1回実行する。
6. `if: always()`の最終stepで、本jobが所有するTestcontainers container、package済みReference JAR process、
   Playwright / Chromium processおよび一時process logの残存0を独立確認する。

TestcontainersはGitHub-hosted runnerのDocker Engineを使用する。fixture credential、RSA key、Bearer token、Cookie、
source HMAC keyおよびdynamic portはtest process内で生成し、GitHub secret、environmentまたはcommand lineへ渡さない。

## 5. Required-check disposition

`Phase 3 Critical Journey E2E`はPhase 3のBrowser / API / DB / Audit / log横断契約を担うため、Gate C後も
Framework Repositoryの回帰checkとしてrequired化することを推奨する。ただし存在前のcontextをrulesetへ先行登録しない。

1. draft PRのfresh runnerで新jobを1回成功させる。
2. 実行時間、cleanup、secret非露出およびflakiness兆候をOwner reviewする。
3. 別の明示承認後、既存7 contextsを維持したまま8件目として追加する。
4. rulesetのactive、strict、bypassなしと8 contextsをAPIでread-backする。
5. Evidence commit後のfinal PR HEADで8 / 8 required checksを再度成功させる。

local 3回連続PASSがあるため、remoteで意図的な複数rerunを成功条件にしない。初回失敗をrerunで隠さず、§8に従う。

## 6. Proposed remote execution sequence

| Order | Action | Acceptance before next action |
|---:|---|---|
| 1 | 本planをreviewしcommit | local worktree clean |
| 2 | §4のCI jobとworkflow READMEをlocal実装・検証しcommit | workflow diff、権限、command、cleanup stepをOwner確認 |
| 3 | current branchをoriginへfast-forward push | local / remote branch HEAD一致、force push 0 |
| 4 | `main`向けdraft PRを作成 | base / head、195 filesのplanning baseline以降を含むfinal inventory、PR本文を確認 |
| 5 | 既存CIとPhase 3 E2Eのfresh runner結果を取得 | 新job success、cleanup success、実行時間とlogを確認 |
| 6 | remote EvidenceをRepositoryへ記録しbranchへpush | source identityとrun URL / ID / attemptを固定 |
| 7 | Owner review後に新contextをmain rulesetへ追加 | active / strict / bypassなし、既存7件維持をread-back |
| 8 | final PR HEADのrequired checksを確認 | 8 / 8 success、branchが最新mainを包含。有効な同一HEAD結果を無目的にrerunしない |
| 9 | Gate C review | DoD 3-11、実browser Evidence、全DoD / AC、PR diff、merge可否を判断 |
| 10 | Gate Cの個別承認後だけmerge commit方式でmerge | PR final HEADとmerge commitを記録 |
| 11 | merge後main CIを確認 | Phase 3 E2Eを含む対象workflow success |
| 12 | Phase 3 final closeout | `COMPLETE / ACCEPTED`を判断 |

Phase 3の33 commitにCP / Gate履歴があるため、merge方式は履歴を保持するmerge commitを推奨する。squash、rebase、
force push、direct main pushまたはruleset bypassを使用しない。

## 7. PR contract

- baseは`main`、headは`feature/phase3-reference-vertical-slice`とする。
- 初回はdraft PRとし、Gate C merge承認前にready / mergeしない。
- PR本文にはPhase 3 scope、P3-C3延期、P3-C4承認、DoD 3-11のremote目的、local verification、
  Public API / migration / Tooling境界およびtest planを記載する。
- `P3-C3 DEFERRED`を未実装欠陥や暗黙採用として表現しない。
- PR作成による自動workflow起動はPR承認範囲に含めるが、workflow rerun、ruleset変更、mergeは含めない。
- GitHub-hosted runner log、Surefire reportまたはPR本文へsecret、token、Cookie、keyまたはfixture passwordを残さない。

### 7.1 Proposed draft PR metadata

推奨titleは次とする。

```text
feat(reference): complete Phase 3 reference vertical slice
```

PR本文は少なくとも次を含める。

```text
## Purpose

Complete the Phase 3 Reference Vertical Slice and collect the Remote Gate evidence required for Gate C.

## Included

- master Tier 1 and expense Tier 2 Reference modules
- MVC / Thymeleaf / selective HTMX and optimistic-lock conflict handling
- minimal Bearer REST API using the same Application use cases
- non-distributed browser, API, and critical E2E verification tooling
- Journey, ADR, Skill, DoD, and acceptance evidence

## Boundaries

- P3-C3 MyBatis rules remain deferred until an adoption trigger
- no Phase 4 implementation or Customer artifact
- no snapshot publish or package mutation
- DoD 3-11 remains pending until the Phase 3 CI job passes

## Local verification

- Root Reactor: 16 / 16 projects, 170 tests
- Reference: 99 tests
- Public API baseline / inventory / japicmp: PASS
- packaged API and critical E2E: PASS
- browser focused: 3 / 3 PASS
- cleanup and sensitive-output assertions: PASS

## Remote Gate

This PR starts as draft. Merge, ruleset mutation, workflow rerun, and Gate C acceptance require separate Owner decisions.
```

## 8. Failure and retry boundary

- browser install、package、E2Eまたはcleanupのどれかが失敗した場合、DoD 3-11をPASS扱いにしない。
- 初回失敗後のworkflow rerunは自動で行わず、network / runner transientとsource defectをlogから分類する。
- sourceを変更する場合はlocal再検証、commit、push前の差分確認をやり直す。
- rerun、追加push、ruleset再変更または別PRは、原因と対象SHAを提示して個別Owner承認を得る。
- cleanup failure時はtest本体がPASSでもGate Evidenceとして受け入れない。
- ruleset required化後にjob名を変更しない。変更が必要なら旧contextとの移行計画を再reviewする。
- failed runのlog artifact、browser traceまたはprocess logを無条件uploadしない。必要時はsecret / PIIを点検して別承認する。

## 9. Explicit non-changes

- production source、Framework Public API、migration、dependency、property、profileまたはrouteを変更しない。
- snapshot publish、Packages write、protected environmentまたはdeploymentを追加しない。
- MyBatis、Phase 4、Project TemplateまたはCustomer CIへ範囲を広げない。
- 既存required checksを削除・一時解除せず、strict policyまたはbypass禁止を弱めない。
- Gate C前にPhase 3を`COMPLETE / ACCEPTED`と表現しない。

## 10. Architecture Owner review points

| ID | Decision requested | Owner decision |
|---|---|---|
| RG-1 | §2のremote inventoryとcurrent branch継続利用を受け入れるか | APPROVED |
| RG-2 | §4の独立CI jobを既存`ci.yml`へ追加してよいか | APPROVED WITH CLEANUP CONDITION |
| RG-3 | CI jobと同時に`.github/workflows/README.md`へ権限・実行・cleanup境界を記録してよいか | APPROVED |
| RG-4 | workflow実装commit後、既存branchをforceなしでpushしてよいか | APPROVED / EXECUTED — forceなしでpush済み |
| RG-5 | push後、`main`向けdraft PRを作成し自動CIを起動してよいか | APPROVED / EXECUTED — draft PR #35を作成済み |
| RG-6 | fresh runner PASS後、新contextを8件目のrequired checkへ追加してよいか | READY FOR OWNER DECISION — §11のrun Evidence確認待ち |
| RG-7 | §8の失敗・rerun境界を採用するか | APPROVED |
| RG-8 | Gate Cまでdraft、merge commit、bypass / direct push禁止を採用するか | APPROVED |

RG-2の承認条件として、local workflowの最終cleanupは、本jobが所有するTestcontainers container、
package済みReference JAR process、Playwright / Chromium processおよび一時process logの残存0を独立確認する。

**Planning-time decision:** RG-1〜RG-3、RG-7、RG-8 APPROVED
**Decided by:** Shuichi Kataoka, Architecture Owner
**Decision date:** 2026年9月17日
**Authorized next action:** §4のworkflowとworkflow READMEのlocal実装・検証・commit

計画承認時点ではRG-4〜RG-6を、それぞれworkflow実装差分、remote branch identity、fresh runner Evidenceが
揃ってから判断するものとした。この計画承認自体はpush、PR、ruleset変更、workflow rerun、mergeまたは
Gate C開始を許可しなかった。RG-4 / RG-5の後続個別承認と実行結果は上表および§11へ記録する。

## 11. Remote CI Evidence and RG-6 readiness

2026年9月17日、draft PR #35の最終修正HEADに対するfresh runner結果をread-onlyで確認した。

| Evidence | Result |
|---|---|
| PR | [#35](https://github.com/zaziedlm/KOIKI-JAVAWEB/pull/35)、base `main`、head `feature/phase3-reference-vertical-slice`、draft維持 |
| Source identity | `9dd1ee250cc1e1f92f25245a3afa3a5e2d8d8785` |
| CI run | [35130435989](https://github.com/zaziedlm/KOIKI-JAVAWEB/actions/runs/35130435989)、attempt 1、7 / 7 jobs SUCCESS |
| Runtime run | [35130436062](https://github.com/zaziedlm/KOIKI-JAVAWEB/actions/runs/35130436062)、attempt 1、2 / 2 jobs SUCCESS |
| PR check rollup | 同一HEADで9 / 9 checks SUCCESS |
| Critical E2E | `Phase 3 Critical Journey E2E` SUCCESS、2分27秒 |
| Critical E2E steps | BOM stage、Chromium install、Reference package、critical journey、cleanupの全step SUCCESS |
| Cleanup | `Inspect and clean owned E2E resources` SUCCESS、test本体成功後の独立stepとして11秒で完了 |
| Cumulative verification | `Local Identity Session Audit Integration`を含む既存checkもSUCCESS |

初回のremote実行系列ではsource defectを検出し、manual rerunで結果を上書きせず、原因ごとに修正commitを追加した。

| Run | HEAD | Result / treatment |
|---|---|---|
| [35122117830](https://github.com/zaziedlm/KOIKI-JAVAWEB/actions/runs/35122117830) | `753d5cf` | CANCELLED。後続source commitのpushにより新runへ移行 |
| [35123220247](https://github.com/zaziedlm/KOIKI-JAVAWEB/actions/runs/35123220247) | `3d8bae8` | CANCELLED。後続source commitのpushにより新runへ移行 |
| [35124442067](https://github.com/zaziedlm/KOIKI-JAVAWEB/actions/runs/35124442067) | `66fa0ad` | FAILURE。Phase 2累積検証のReference migration境界を修正 |
| [35127925961](https://github.com/zaziedlm/KOIKI-JAVAWEB/actions/runs/35127925961) | `4e07592` | FAILURE。Phase 2累積検証のReference source境界を修正 |
| [35130435989](https://github.com/zaziedlm/KOIKI-JAVAWEB/actions/runs/35130435989) | `9dd1ee2` | SUCCESS。最終HEADのattempt 1 |

この結果は§8の失敗・retry境界に適合する。DoD 3-11の要求である実CI上のcritical journey PASSは充足した。
これはGate Cのfinal acceptance、ruleset変更、ready化またはmerge承認を意味しない。

### RG-6 recommended decision

`Phase 3 Critical Journey E2E`は独立jobとしてfresh runnerで成功し、cleanupも独立stepで成功した。
同一HEADで既存7 required contextsを含む全checkが成功しているため、既存7件を維持したまま8件目のrequired checkへ
追加する条件は満たしている。

推奨承認文言は次のとおりとする。

> §11のfresh runner Evidenceを確認し、RG-6を承認する。`main-merge-protection`のactive、strict、
> bypassなしおよび既存7 required checksを維持したまま、`Phase 3 Critical Journey E2E`を8件目の
> required checkへ追加してよい。変更後はrulesetをread-backし、final PR HEADで8 / 8 required checksを確認する。

RG-6がOwner承認されるまではrulesetを変更しない。workflow rerun、PR ready化、mergeおよびGate C acceptanceも
引き続き別判断とする。
