# Phase 2 P2-C2 C2-5 publish review start handoff

## 1. Handoff status

- 作成日: 2026年9月11日
- Architecture Owner: Shuichi Kataoka
- current branch: `feature/phase2-p2-c1-postgresql-migration`
- implementation baseline commit: `6793956d6ee9d8a0b30c5ac9927e4442a14a4bef`
- baseline subject: `test(packaging): establish P2-C2 public API baseline candidate`
- Phase status: `P2-C2 C2-4 COMPLETE / ARCHITECTURE OWNER APPROVED — C2-5 PUBLISH REVIEW READY`
- Ownership: Framework Packaging / Tooling Evidence
- production artifact / Framework Public API change in C2-4: 0
- immediate next action: C2-5のcandidate座標、signature、SHA-256、公開対象およびremote publish実施可否を
  Architecture Owner reviewする

本handoffは、別PC・別AIセッションでP2-C2を安全に再開するための作業正本である。C2-4までのlocal実装・検証と
Architecture Owner承認は完了している。Git branchのremote pushは引継ぎ目的のsource共有であり、C2-5のMaven snapshot
publishとは別操作である。C2-5 remote artifact publishは本handoff作成時点で未承認・未実施である。

## 2. Required reading order

1. `AGENTS.md`
2. `docs/agent/skills/koiki-project-overview/SKILL.md`
3. 本書
4. `../architecture/validation/phase2-p2-c2-contract-review.md`の§6〜§10
5. `../architecture/validation/phase2-p2-c2-c2-4-verification.md`
6. `KOIKI-JavaWeb-FW_Phase2実行計画_v0.1.md`のP2-C2履歴、§7〜§9、§11
7. `../architecture/validation/phase2-p2-c2-c2-2-verification.md`
8. `../architecture/validation/phase2-p2-c2-c2-3-verification.md`
9. `../../build-support/security-foundation-verification/README.md`
10. `../../build-support/api-compatibility/README.md`
11. `../architecture/adr/README.md`のADR-029、ADR-041
12. `../architecture/grand-design/KOIKI-JavaWeb-FW_グランドデザイン_v0.2.md`の§8.5〜§8.6

P2-C2は業務機能を扱わないため、`koiki-business-feature-work`は通常不要である。Customer / Reference業務codeまたは
Framework Public APIの変更が必要になった場合は、C2-5のpublish review境界を越えるため作業を停止して再reviewする。

## 3. Transfer baseline

```text
Implementation HEAD: 6793956d6ee9d8a0b30c5ac9927e4442a14a4bef
Subject: test(packaging): establish P2-C2 public API baseline candidate
Current branch: feature/phase2-p2-c1-postgresql-migration
Worktree immediately after implementation commit: clean
Local tracking observation before handoff creation: origin tracking refに対して0 behind / 8 ahead
P2-C2 C2-4: COMPLETE / ARCHITECTURE OWNER APPROVED
```

本handoffだけを次の文書commitとする。別PCではremoteを`fetch`した後、branch先端に本handoff commitとimplementation
baseline `6793956`が含まれることを確認する。上記ahead / behind値は作成時点のlocal tracking refに基づくため、remoteの
最新状態として固定せず、push前および再開時に再取得する。

## 4. P2-C2 progress

| Slice | State | Confirmed result |
|---|---|---|
| C2-1 | Owner approved | C2-C1〜C7のpackage / Consumer / API / publish / OpenRewrite / closeout境界を確定 |
| C2-2 | Owner approved | formal release unit 14 projects / 11 JAR、隔離repository stage成功 |
| C2-3 | Owner approved | Root外Consumer、Java 21 build、Java 21 / 25、PostgreSQL 17実行成功 |
| C2-4 | Owner approved | 11 JAR / 24 Public型のbaseline candidateとpositive / negative fixture成功 |
| C2-5 | Review ready | remote artifact publishは未承認・未実施 |
| C2-6 | Not started | OpenRewrite非配布feasibility prototype |
| C2-7 | Not started | P2-C2 aggregate closeout |

## 5. C2-4 accepted baseline candidate

formal release unitのPublic型24件は次の内訳である。

| Position | Artifact | Public types |
|---|---|---:|
| Phase 1a公開済み | `koiki-architecture-contract` | 4 |
| Phase 1a公開済み | `koiki-archunit-rules` | 1 |
| Phase 2 candidate | `koiki-starter-audit` | 6 |
| Phase 2 candidate | `koiki-starter-identity` | 10 |
| Phase 2 candidate | `koiki-starter-session-jdbc` | 3 |
| Public型なし | Starter API / Data / Data JPA / Observability / Security、`koiki-testing` | 0 |

baseline candidateは型、継承、constructor、field、method、generic、例外、enum、annotation metadata / defaultおよび
JSpecify nullnessを正規化する。`.internal.` packageはPublic APIから除外する。既存inventoryとの照合は型集合の継続一致を
示すが、過去versionとのsignature互換性を証明しない。Phase 2 artifactは比較可能なpublished baselineをまだ持たないため、
現時点で`binary compatible`または`source compatible`と主張しない。

検証コマンドと最終結果:

```powershell
pwsh -NoProfile -File build-support/security-foundation-verification/verify-p2-c2-public-api.ps1
```

```text
Phase 2 P2-C2 C2-4 Public API verification succeeded (11 JARs / 24 public types / full signatures / nullness and negative fixtures).
```

## 6. C2-5 required decision sequence

C2-5はlocal成果物の通常commitではなくremote artifact stateを変更するため、次の順序を崩さない。

1. implementation baseline `6793956`を含むclean source HEADを確認する。
2. formal candidate artifactごとのMaven座標、正規化signature、local SHA-256および公開対象を作成する。
3. repository URL、認証方法、timestamped snapshotの取得方法、秘密情報を残さない実行・Evidence方式をreviewする。
4. 上記candidate一覧とremote publish実施可否をArchitecture Ownerへ提示する。
5. 個別Owner承認後だけ、承認済み同一commitをremote Maven repositoryへ一度だけpublishする。
6. remoteから公開済みbaselineを取得し、timestamp、SHA-256一致および`japicmp`経路を検証する。
7. 初回同一source比較の成功を取得経路の成立確認として記録し、過去versionとの互換性実績とは表現しない。

baseline JAR、repository token、settings実値および一時reportをGitへcommitしない。既存Phase 1aのpublished baseline、script、
workflowおよびrequired checkを変更しない。remote repositoryの実体や権限が確認できない場合は、推測で公開先を作らず
Owner reviewへ差し戻す。

## 7. Resume checklist on another PC

```powershell
git fetch origin
git switch feature/phase2-p2-c1-postgresql-migration
git pull --ff-only
git status --short
git log -5 --oneline --decorate
git merge-base --is-ancestor 6793956d6ee9d8a0b30c5ac9927e4442a14a4bef HEAD
java -version
.\mvnw.cmd --version
```

期待値:

- branchが`feature/phase2-p2-c1-postgresql-migration`
- `6793956`と本handoff commitがbranch先端履歴に含まれる
- worktreeがclean
- Phase 2実行計画が`C2-5 PUBLISH REVIEW READY`
- C2-4 Evidenceが`COMPLETE / ARCHITECTURE OWNER APPROVED`
- remote Maven artifact publishはまだ行われていない

C2-5のpublish実行に進む場合だけ、公開先へのread権限、write権限、credential供給方式および取得側のMaven設定を
秘密情報を表示せず確認する。Java / Maven / Gitのversion差がある場合は、実装変更前に差分を記録する。

## 8. Stop conditions

- Architecture Ownerの個別承認前にremote Maven snapshotをpublishする。
- C2-4 candidateと異なるsource HEADまたは再build後に未照合のJARをpublishする。
- token、password、private key、settings実値、repository responseの秘密情報をsource、log、Evidenceへ残す。
- Phase 1a published baseline、既存compatibility script、workflow、required checkまたはrulesetを変更する。
- 初回の同一source `japicmp`成功を、過去versionとのbinary / source compatibility実績として表現する。
- `koiki-reference-app`、Consumer、fixture、OpenRewrite prototypeをformal release unitへ含める。
- C2-5のためにproduction artifact、Framework Public API、migrationまたは新しいMaven moduleを追加する。
- dirty worktree、source commit不一致、hash不一致または公開対象未確定のままpublishする。

## 9. Immediate next action

再開後はpublish commandを先に実行せず、C2-5 candidate artifactの座標、signature、local SHA-256、公開対象、remote取得方法と
秘密情報を残さないEvidence方式を整理し、Architecture Owner reviewを行う。個別承認が得られた場合だけremote Maven
snapshotを一度公開する。承認を得られない、または公開先を安全に確定できない場合は、C2-5を未実施のまま保持する。
