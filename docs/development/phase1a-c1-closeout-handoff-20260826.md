# Phase 1a C1 完了・CI Linux化 作業引継ぎ — 2026-08-26

## 1. この文書の位置づけ

この文書は、Phase 1a C1「内部snapshot公開」の完了と、それに続くCI高速化対応（Windows runner一時停止）を
区切り、新規AIセッションで後続作業（C2以降、または他のOwner指示）を安全に開始するための運用上の
引継ぎメモである。C1の実装証拠の正本は`docs/architecture/validation/phase1a-internal-snapshot.md`のまま
変わらない。

判断が競合する場合は、次の順に正本を確認する。

1. Repository rootの`AGENTS.md`
2. `docs/agent/skills/koiki-project-overview/SKILL.md`
3. `docs/architecture/validation/phase1a-internal-snapshot.md`（C1 Gate 1〜4のEvidence正本）
4. `docs/development/KOIKI-JavaWeb-FW_Phase1a実行計画_v0.1.md`
5. `docs/development/phase1a-c1-handoff-20260825.md`（C1開始時点の引継ぎ、本書はその後継）
6. 実効構成である`.github/workflows/ci.yml`、`.github/workflows/publish-snapshot.yml`、Root / release unitのPOM

C1は業務機能を変更しないため、開始時の適用Skillは`koiki-project-overview`である。業務成果物を変更する
必要が生じた場合だけ、`koiki-business-feature-work`の適用要否を確認する。

## 2. 引継ぎ時点のGit・remote状態

| 項目 | 状態 |
|---|---|
| `main`最新commit | `10c6ca21dd60561991249a7affc6adc1696d85d4`（PR #16 merge commit） |
| main CI | run `32957094672`、`conclusion: success`（`Verify (ubuntu-24.04)`のみ） |
| worktree | clean、`main`は`origin/main`と同期済み |
| C1作業branch | `feature/phase1a-internal-snapshot`（merge済み、ローカル・remoteとも残置） |
| CI Linux化branch | `feature/ci-linux-only`（merge済み、ローカル・remoteとも残置） |
| 未マージbranch | 上記2つ以外に削除・整理は未実施。他のremote branchは本作業と無関係の既存branch |

両branchとも、次のセッションで不要と判断されれば削除してよい（Owner未指示のため保留中）。

## 3. 今回セッションで完了した作業

### 3.1 Phase 1a C1「内部snapshot公開」— COMPLETE

Gate 1〜4すべて承認済み。Evidence正本は
[docs/architecture/validation/phase1a-internal-snapshot.md](../architecture/validation/phase1a-internal-snapshot.md)。

| Gate | 内容 | 結果 |
|---|---|---|
| Gate 1 | 実装方針・publish workflow設計 | ACCEPTED |
| Gate 1A | 追加確認事項 | ACCEPTED |
| Gate 2 | Maven coordinates・release unit確定 | ACCEPTED |
| Gate 3 | environment・CI・package状態確認 | ACCEPTED（PR #14 merge commit `9573b1c`） |
| Gate 4 | 実publish・Evidence記録 | ACCEPTED（workflow run `32951187676`） |

- 公開snapshot: `org.koikifw:{koiki-dependencies-bom,koiki-parent,koiki-architecture-contract,koiki-archunit-rules}:0.1.0-20260826.091429-1`
- 公開先: `https://maven.pkg.github.com/zaziedlm/KOIKI-JAVAWEB`（GitHub Packages Apache Maven registry）
- 4 packageの存在、POM/JARのSHA-256 checksum、credential非露出をEvidenceとして記録済み。
- PR #14（Gate 3対応）・PR #15（Gate 4 Evidence記録）ともmerge済み。

### 3.2 CI高速化: Windows runner一時停止

開発初期段階でのCI速度改善のため、Owner指示により`windows-2025`をCI/publish workflowの両方から
一時的に除外した。本番runtimeはクラウドLinuxであるため、Windows検証は現時点で必須ではないという判断。

- 対象ファイル: [.github/workflows/ci.yml](../../.github/workflows/ci.yml)、
  [.github/workflows/publish-snapshot.yml](../../.github/workflows/publish-snapshot.yml)
- 変更内容: `matrix.os`を`[windows-2025, ubuntu-24.04]`から`[ubuntu-24.04]`のみへ変更。
  再有効化条件を示すinline comment（`# windows-2025 temporarily disabled: ...`）を付与。
  Windows専用step（`if: runner.os == 'Windows'`）は削除せず残置（matrixがWindowsを生成しないため無害）。
- 作業branch: `feature/ci-linux-only`（commit `1310db3`）
- PR #16としてmerge済み（merge commit `10c6ca2`）。

### 3.3 GitHub Ruleset起因のmerge blockと対処（重要な発見）

PR #16は、CI自体はSUCCESSしていたにもかかわらずmergeできない状態になった。原因は、classic branch
protection API（`gh api repos/{owner}/{repo}/branches/main/protection`）では検出できない
**GitHub Rulesets**機能によるもの。

- Repositoryは`main-merge-protection`という名前のruleset（id `21140116`）で`main`を保護している。
- このrulesetの`required_status_checks`が、当初`Verify (windows-2025)`と`Verify (ubuntu-24.04)`の
  両方を必須としていた。CI workflowからWindows runnerを除外した結果、`Verify (windows-2025)`という
  checkが永久に生成されなくなり、mergeが恒久的にblockされる状態になった。
- Owner側でGitHub UI上からrulesetを編集し、必須status checkを`Verify (ubuntu-24.04)`のみへ変更して解消。
- 修正後、`gh api repos/zaziedlm/KOIKI-JAVAWEB/rulesets/21140116`で必須checkが
  `ubuntu-24.04`のみになったことを確認し、PR #16は`mergeable: MERGEABLE` / `mergeStateStatus: CLEAN`
  となりmerge成功。

**教訓（次回セッション以降も要注意）**: このRepositoryではbranch保護に**classic API ではなく
Rulesets**を使用している。`branches/{branch}/protection`は404「Branch not protected」を返すが、
これは「保護なし」を意味しない。merge blockやrequired status checkを調査する際は必ず
`gh api repos/{owner}/{repo}/rulesets`と各rulesetの詳細（`gh api repos/{owner}/{repo}/rulesets/{id}`）を
確認すること。CI workflowのjob名（`matrix.os`の値等）を変更・削除する場合は、対応するrulesetの
`required_status_checks`も同時に見直す必要がある。

## 4. 次回セッションへの引継ぎ事項

### 4.1 未対応・保留中の任意作業（Owner未指示）

1. **Branch整理**: `feature/phase1a-internal-snapshot`と`feature/ci-linux-only`はmerge済みで
   ローカル・remoteに残置されている。削除は未指示のため保留。次回Owner判断で削除可。
2. **Rulesetsに関するドキュメント化**: 今回発見したRulesets vs classic API の挙動差異は、
   Repository固有の運用知識として`docs/architecture/`や`AGENTS.md`配下へ記録する余地があるが、
   本セッションでは未実施（本handoffのみに記録）。

### 4.2 後続Phase 1a作業（Milestone C継続）

C1完了により、次はC2以降が候補となる。開始する場合は
[docs/development/phase1a-c1-handoff-20260825.md](phase1a-c1-handoff-20260825.md) の
「C1の完了条件候補」「C1で行わないこと」節にあるC2〜C5の役割分担を踏まえること。

- C2: Repository外Consumer実装、認証運用の実演、意図的ArchUnit違反
- C3: Public API inventory確定、japicmp baseline、破壊検出
- C4: Java 21 build artifactを使ったJava 21 / 25 runtime検証
- C5: Phase 1a CloseoutとWalking Skeleton残置物の最終処置

ただし、次回セッション開始時にOwnerから別の作業指示がある場合はそちらを優先する。本書は
「どこまで完了しているか」の引継ぎであり、次の作業を強制するものではない。

## 5. 次回セッションの開始手順

```powershell
git switch main
git pull --ff-only
git status --short --branch
git log -5 --oneline --decorate
```

期待状態:

- branchが`main`
- 最新commitが`10c6ca2`（PR #16 merge commit）
- worktreeがclean
- `origin/main`と同期済み

## 6. 新規セッションへの開始依頼文（例）

> KOIKI-JavaWeb-FWのPhase 1a C1「内部snapshot公開」はGate 1〜4すべてACCEPTEDでCOMPLETEしています。
> 続けてCI高速化のためWindows runnerをCI/publish workflowの両方から一時停止し、GitHub Ruleset起因の
> merge blockも解消済みです（詳細は`docs/development/phase1a-c1-closeout-handoff-20260826.md`）。
> 現在`main`は`10c6ca2`です。次はC2以降の計画、branch整理、または別の作業指示に応じて進めてください。
> 作業前に`AGENTS.md`と`docs/agent/skills/koiki-project-overview/SKILL.md`を確認してください。

## 7. 引継ぎ時点の検証

- PR #14, #15, #16はいずれもmerge済みで、main CIは全てSUCCESS（最終run `32957094672`）。
- GitHub Packagesに4 packageの`0.1.0-20260826.091429-1`snapshotが公開済み（Gate 4 Evidence記録済み）。
- CI/publish workflowは`ubuntu-24.04`のみで動作する状態に変更済み、Ruleset側の必須checkも整合。
- 本handoff作成時点で、Repository構成・workflow・Validation文書に対する追加変更は行っていない。
