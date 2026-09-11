# Phase 2 P2-C2 package / Consumer start handoff

## 1. Handoff status

- 作成日: 2026年9月11日
- Architecture Owner: Shuichi Kataoka
- current branch: `feature/phase2-p2-c1-postgresql-migration`
- baseline commit: `4d9230ff19105ddf4f6a5a2e213ef0cee349a90b`
- Phase status: `P2-C1 COMPLETE / ARCHITECTURE OWNER APPROVED — P2-C2 CONTRACT REVIEW PREPARED`
- Ownership: Framework Packaging / Tooling Evidence
- production change: 0
- immediate next action: `phase2-p2-c2-contract-review.md`のC2-C1〜C7をArchitecture Owner reviewする

本handoffは、休憩後にP2-C2を安全に開始するための準備正本である。実施したのはread-only inventory、contract review案、
handoffおよびPhase 2実行計画の状態整理だけであり、Java、POM、test、script、Public API、OpenRewrite recipe、CI、
remote package、rulesetまたはbranchを変更していない。

## 2. Required reading order

1. `AGENTS.md`
2. `docs/agent/skills/koiki-project-overview/SKILL.md`
3. 本書
4. `../architecture/validation/phase2-p2-c2-contract-review.md`
5. `KOIKI-JavaWeb-FW_Phase2実行計画_v0.1.md`のMilestone C、§7〜§9
6. `../architecture/validation/phase2-p2-c1-c1-4-closeout.md`
7. `../architecture/validation/phase2-gate-b-contract-review.md`のGB-C4
8. `../architecture/validation/phase2-gate-b-closeout.md`
9. `../../build-support/api-compatibility/README.md`
10. `../../build-support/runtime-foundation-consumer/README.md`
11. `../architecture/adr/README.md`のADR-029、ADR-041
12. `../architecture/grand-design/KOIKI-JavaWeb-FW_グランドデザイン_v0.2.md`の§8.5〜§8.6

P2-C2は業務機能を設計・実装しないため、`koiki-business-feature-work`は通常不要である。Consumerのsynthetic業務語彙を
FrameworkまたはReferenceへ昇格する必要が生じた場合は作業を停止し、Ownershipを再reviewする。

## 3. Transfer baseline

```text
HEAD: 4d9230ff19105ddf4f6a5a2e213ef0cee349a90b
Subject: docs(security): close P2-C1 after Architecture Owner approval
Current branch: feature/phase2-p2-c1-postgresql-migration
Worktree before preparation: clean
P2-C1: COMPLETE / ARCHITECTURE OWNER APPROVED
```

P2-C2作業branchを分ける場合は、準備文書をcommitしたclean HEADから作成する。branch作成、pushまたはPRは本準備では行わない。

## 4. Current reusable assets

| Asset | Reuse in P2-C2 | Must not be changed during preparation |
|---|---|---|
| formal Framework staging | 14-project隔離stage | release unitを増減しない |
| `runtime-foundation-consumer` | Root外Consumer構成の参照 | Phase 1b回帰正本をPhase 2用に改変しない |
| security verification Harness | artifact / API / sensitive / cleanup assertion | 重複正本を作らない |
| C1 PostgreSQL Harness | 3 migrations / 11 tables、history / upgrade | Consumer独自DDLへcopyしない |
| API compatibility Tooling | inventory generator、published 2-artifact比較、negative fixture | 既存baselineを差し替えない |
| Gate B closeout | Root / Null Safety / 3-round regression / cleanup | P2-C2専用に複製しない |

OpenRewriteのproduction module、recipeまたはfixtureは現時点で存在しない。Grand Design / ADRが承認しているのは
正式提供方針であり、正式artifactとCI検証の時期はPhase 5である。

## 5. Preparation conclusions

1. P2-C2で正式release unitを増やす必要はない。
2. Security ConsumerはPhase 1b Consumerと分離した非配布Root外fixtureが適切である。
3. Phase 2 APIには過去published baselineがないため、最初のsnapshotを過去互換性証拠として扱えない。
4. snapshot publishはremote state変更であり、local実装・検証とは別のOwner承認が必要である。
5. OpenRewriteはsynthetic old inputを使うfeasibilityに限定し、正式recipeへ昇格させない。
6. ConsumerはJava 21でbuildし、package済み同一JARをJava 21 / 25で実行する。
7. P2-C2 closeoutにはC1とGate Bの回帰、sensitive-outputおよびcleanupを含める。

## 6. Proposed slices

| Slice | Scope | Start condition |
|---|---|---|
| C2-1 | contract / inventory review | 現在開始可能 |
| C2-2 | formal package manifest / isolated staging | C2-C1〜C7 Owner承認 |
| C2-3 | Root外Security Consumer | C2-2 Evidence確認 |
| C2-4 | full Public API inventory / compatibility fixture | C2-3 Evidence確認 |
| C2-5 | publish review / optional execution | remote publishの個別Owner承認 |
| C2-6 | OpenRewrite feasibility | prototype境界のOwner承認 |
| C2-7 | closeout | C2-2〜C2-6の承認済み成果 |

## 7. Resume checklist

```powershell
git status --short
git branch --show-current
git log -3 --oneline --decorate
java -version
.\mvnw.cmd --version
docker version
```

期待値:

- preparation commit後のworktreeがclean
- baselineに`4d9230f`が含まれる
- P2-C1が`COMPLETE / ARCHITECTURE OWNER APPROVED`
- 最初の作業はC2-C1〜C7のreviewであり、POMやJava実装ではない

## 8. Stop conditions

- C2-C1〜C7承認前にPOM、Java、fixture、scriptまたはPublic APIを変更する。
- ReferenceまたはPhase 1b ConsumerをP2-C2 Consumerとして流用・改変する。
- Consumer、OpenRewrite prototypeまたはfixtureをRoot Reactor、BOM、formal release unit、`koiki-testing`へ追加する。
- 過去baselineのないPhase 2 artifactを`japicmp compatible`と表現する。
- 個別Owner承認前にsnapshot publish、workflow、push、PR、required checkまたはrulesetを変更する。
- synthetic OpenRewrite fixtureを正式migration recipeまたはCustomer対応保証として扱う。
- Spring Boot公式recipeをKOIKI側で再実装する。
- Oracle、SAML、Redis、WebFlux、AWS固有Adapter、Project TemplateまたはPhase 3業務機能を追加する。

## 9. Immediate next action

休憩後は`phase2-p2-c2-contract-review.md`のArchitecture Owner判断1〜7をレビューする。特に、11 JARのbaseline候補、
初回publishの意味、Phase 1b Consumerとの分離、およびOpenRewrite試作をPhase 5正式提供と混同しない境界を確認する。
承認後だけC2-2 formal package manifest / isolated stagingへ進む。
