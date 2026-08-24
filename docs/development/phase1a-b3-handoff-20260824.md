# Phase 1a B3 作業引継ぎ — 2026-08-24

## 1. この文書の位置づけ

この文書は、別PC・新しいAIセッションでPhase 1a B3を安全に再開するための
運用上の引継ぎメモである。設計判断の正本ではない。

判断が競合する場合は、次の順に正本を確認する。

1. Repository rootの`AGENTS.md`
2. `docs/agent/skills/koiki-project-overview/SKILL.md`
3. `docs/agent/skills/koiki-business-feature-work/SKILL.md`
4. `docs/architecture/validation/phase1a-archunit-api-design.md`（B2設計）
5. `docs/architecture/validation/phase1a-archunit-rules.md`（B3実装・レビュー計画）
6. `docs/development/KOIKI-JavaWeb-FW_Phase1a実行計画_v0.1.md`

## 2. 引継ぎ時点のGit状態

- 作業ブランチ: `feature/phase1a-archunit-rules`
- 引継ぎ文作成前の先頭Commit: `48dc16c docs: prepare B3 ArchUnit rules implementation`
- B2を取り込んだ`main`のMerge Commit: `b460b52`
- B2 PR: `#9`（CI確認後、`main`へMerge済み）
- `48dc16c`時点ではB3の準備文書のみ作成済み
- Maven module、POM、Java source、test fixtureのB3実装は未着手
- 引継ぎ文作成開始時のworktreeはclean
- 引継ぎ文作成開始時、作業ブランチにupstreamは設定されていなかった

この文書をCommitすると先頭Commitは変わる。再開時はCommit IDだけでなく、
ブランチ名と最新履歴も確認すること。

## 3. 現PCで終了前に行うこと

1. この引継ぎ文をCommitする。
2. 作業ブランチをremoteへ初回Pushし、upstreamを設定する。
3. remote上のブランチと最新Commitを確認する。
4. worktreeがcleanであることを確認する。

想定コマンド:

```powershell
git add docs/development/README.md docs/development/phase1a-b3-handoff-20260824.md
git commit -m "docs: add B3 session handoff"
git push -u origin feature/phase1a-archunit-rules
git status --short --branch
git log -5 --oneline
```

## 4. 別PCでの取得・確認

作業ブランチを現PCからPushした後、別PCで次を実行する。

```powershell
git fetch origin
git switch --track origin/feature/phase1a-archunit-rules
git status --short --branch
git log -5 --oneline
java -version
.\mvnw.cmd -version
```

同名のlocal branchがすでに存在する場合は、状態を確認したうえで次を用いる。

```powershell
git switch feature/phase1a-archunit-rules
git pull --ff-only
```

期待状態:

- branchが`feature/phase1a-archunit-rules`
- remote trackingが設定済み
- この引継ぎ文のCommitが履歴に存在
- worktreeがclean
- Java/Maven Wrapperを実行可能

## 5. B3の位置づけと現在地

B3はPhase 1a Build Foundationにおける、Tooling ownershipの
`koiki-archunit-rules`実装である。B2で承認したArchitecture Rules API設計を、
実装・fixture・検証記録へ落とし込む。

現在地は次のとおり。

- B1: 完了、PR `#8`で`main`へMerge済み
- B2: Gate 1〜5承認、完了、PR `#9`で`main`へMerge済み
- B3: 実装計画作成と作業ブランチ準備まで完了
- B3実装: 未着手
- 次の区切り: B3 Owner Review Gate 1

## 6. 再開時の必読順序

実装前に、少なくとも次を全文確認する。

1. `AGENTS.md`
2. `docs/agent/skills/koiki-project-overview/SKILL.md`
3. `docs/agent/skills/koiki-business-feature-work/SKILL.md`
4. `docs/architecture/validation/phase1a-archunit-api-design.md`
5. `docs/architecture/validation/phase1a-archunit-rules.md`
6. `docs/development/KOIKI-JavaWeb-FW_Phase1a実行計画_v0.1.md`のB3関連箇所
7. 現在のroot POM、BOM、Architecture Contract、既存validation記録

Repository内の設計・実装の現状を確認してから、文書の推測ではなく実装証拠を
優先する。

## 7. 次に扱う範囲 — Gate 1

次回は、すぐに全ルールを実装せず、まずGate 1の変更範囲と受入条件を確認する。

Gate 1の対象:

- root reactorへの`koiki-archunit-rules` module追加
- BOMでのArchUnit `1.5.0`基準とrules module管理
- rules module POMのparent、dependency、test dependency
- `org.koikifw.archunit` packageの`@NullMarked`
- package-privateの内部基盤
  - `PackageName`
  - `ModuleMetadata`
  - `RuleMessage`
- B2で定めた入力契約のtest
- Maven reactor上でのcompile/test成立

再開時に最初に提示するもの:

1. 変更予定POMの具体的な差分
2. production/test dependencyの根拠
3. packageとvisibilityの配置案
4. 入力契約testのケース一覧
5. Gate 1の検証コマンドと承認判定

空moduleだけを先にCommitしない。内部基盤とそのtestを、検証可能なまとまりとして
扱う。公開facadeは、内部ルールが揃う前に公開しない。

## 8. B2から引き継ぐ主要契約

- Public APIは`org.koikifw.archunit.KoikiArchitectureRules`の1 classのみ
- Public methodは次の2つのみ
  - `businessModuleRules(String businessBasePackage): ArchRule`
  - `frameworkOwnershipRules(String frameworkBasePackage, String... consumerBasePackages): ArchRule`
- helperは同じroot packageのpackage-privateとする
- 39ルールは27適用、12保留
- 適用27は25 failure ruleとRule 10／23のallowance predicate
- failure messageは25件のcontractとして固定する
- Rule 19は近似判定であり、代表failure、DTO変換pass、限界の3点を検証する
- Rule 28は直接およびmetaの`@TransactionalEventListener`を検出する
- `@ApplicationModuleListener`はPhase 1a〜Level 1では禁止する
- ArchUnit baselineは`1.5.0`
- rules moduleのproduction dependencyは3件、fixture test dependencyは8件
- positive testは6件
- no-op ruleや、保留ルールを通過扱いで実装することは禁止

正確なrule matrix、message、fixture、受入条件はB2設計を参照し、この要約だけで
実装判断を行わない。

## 9. Phase 1aの境界

- Spring標準機能を優先する
- Framework / Reference / Customer / Walking Skeletonのownershipを混在させない
- Walking Skeletonのcode、template、migration SQL、一時Maven座標を直接昇格させない
- Runtime、Security、Reference業務、SPA・非同期、Production Baselineを先行実装しない
- Spring Modulith Level 2、MyBatis等の将来範囲をB3へ持ち込まない
- 未使用の将来module、package、starter、Public APIを生成しない
- 実装検証結果は`docs/architecture/validation/`へ記録する

## 10. B3 Owner Reviewの区切り

詳細はB3実装・レビュー計画を正本とする。概略は次のとおり。

1. Gate 1: Maven/BOM/dependency、内部基盤、入力契約
2. Gate 2: Rules 1〜13、28、38〜39
3. Gate 3: Rules 14〜24、allowance、Rule 19
4. Gate 4: Public API、positive/negative fixture、message contract
5. Gate 5: reactor検証、dependency確認、CI、validation記録、B3最終判定

各Gateで、レビュー対象・検証結果・未解決事項を提示し、Owner承認を得てから次へ
進む。B3計画にあるCommit候補を基本とし、レビュー可能な単位でCommitする。

## 11. 新しいAIセッションへの開始依頼文

次の文章を、新しいセッションの最初の依頼として使用できる。

> KOIKI-JavaWeb-FWのPhase 1a B3作業を再開します。まずRepository rootの
> `AGENTS.md`、`docs/agent/skills/koiki-project-overview/SKILL.md`、
> `docs/agent/skills/koiki-business-feature-work/SKILL.md`を全文読み、続いて
> `docs/development/phase1a-b3-handoff-20260824.md`、B2設計
> `docs/architecture/validation/phase1a-archunit-api-design.md`、B3計画
> `docs/architecture/validation/phase1a-archunit-rules.md`を確認してください。
> 作業ブランチは`feature/phase1a-archunit-rules`です。最初にGit状態、履歴、
> Java/Maven環境を確認し、B3が準備完了・実装未着手であることを検証してください。
> 次はOwner Review Gate 1です。実装前に、Gate 1の変更予定、入力契約test、
> dependency、検証方法、承認判定を具体化して提示してください。Ownerレビューの
> 区切りを守り、承認前にGate 2以降へ進めないでください。

## 12. 引継ぎ時点の検証

`48dc16c`では文書のみを追加しており、B3実装testはまだ存在しない。そのため、
引継ぎ準備時にMaven testは実施していない。次回Gate 1で、変更前baselineと
変更後の対象test・reactor testを区別して記録する。
