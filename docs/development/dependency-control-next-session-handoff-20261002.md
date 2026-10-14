# 依存関係制御：次回作業引継ぎ（2026-10-02）

**目的:** 別端末で作業を再開し、承認済みの論点整理とここまでの経緯を元に、案を深掘りして実効性のあるものにする。
**Phase / Ownership:** Pre-Phase 4採用支援 / Framework側の分析・設計文書。CustomerのPOM・業務code・CI運用はCustomer所有。
**再開branch:** `docs/daily-development-workflow`（remote: `origin`）。本引継ぎを含むcommitを再開地点とする。
**承認状態:** 作業1〜3の文書は承認済み。依存関係制御の具体的な方式採用・実装、Phase 4開始は未承認。

## 1. ここまでの作業経緯

| 日付 | 作業・判断 | 到達点 |
|---|---|---|
| 2026-09-30 | Framework側の作業順序案と開発者ガイド2冊をOwner review | ガイド2冊は承認済み。PR #41でmain反映、PR #42でREADME整合 |
| 2026-10-01 | 作業1の状況更新 | 社内Repository同期完了をOwnerが報告。FAQはアプリケーション開発者ガイドに置き、増えたら独立文書に分ける方針を採用。commit `7653d16` |
| 2026-10-01 | 作業2：日常開発の反復方式の比較・推奨案 | commit別の開発者専用stage（C案）を推奨候補とする文書を承認。方式採用・Tooling実装とチーム試用は別判断。commit `27017ca` |
| 2026-10-01 | 作業3：依存関係の制御の論点整理を作成 | Parent / BOMとArchitecture Rulesの保証範囲、review・追加検査の候補を整理 |
| 2026-10-02 | Owner reviewでアプリチームの懸念を具体化 | Framework由来のモジュール／BOMの不用意なPOM記載を防ぐ制約と、アプリ型に対応するFramework定義からType宣言だけを記載する方式を比較対象へ追加 |
| 2026-10-02 | 作業3の文書全般をOwner承認 | 論点整理の文書化を完了。別端末での深掘りに引き継ぐため、文書・承認記録・本引継ぎのcommitとpushをOwnerが指示 |

作業1〜2の経緯は[作業順序案§8・§9](pre-phase4-framework-independent-work-review-20260930.md)、作業3の承認は[論点整理§7](dependency-control-issues-20261001.md#7-owner-review承認記録2026-10-02)を参照する。
本branchの差分のmain反映・社内同期は別途行う。既に報告されたガイドの社内同期と、本branch全体の同期完了を混同しない。

## 2. 次回に引き継ぐ問題認識

単に「第三者libraryの追加をreviewで管理する」だけでは、アプリチームが懸念するFramework構成の誤選択への対策として十分か判断できない。
中心となる問いは、**CustomerがPOMへ個別にモジュール／BOMを記載する責任を持ちながら、Framework側で誤選択を防ぎ、アプリ型に応じた構成の宣言を簡単にできるか**である。

比較対象は[論点整理§4.1](dependency-control-issues-20261001.md#41-アプリ型に応じたframework構成の選択と誤記載防止追加検討案)の3案とする。

1. 個別記載への制約・検査。
2. Frameworkが定義するアプリ型と、Customer側のType宣言による構成選択。
3. 型による構成選択と、宣言からの逸脱検査の組合せ。

「Type」は概念上のアプリ型の宣言であり、Mavenの`<type>`要素と同一とは決めていない。型名、座標、記法、実装方法は未定義。
Parent / BOMは追加依存を全面的に禁止しない。BOMの管理対象は利用許可リストではない。Architecture RulesのPASSも技術採用の承認にはならない。

## 3. 次回の検討順序と出口

次回は分析・設計から開始し、方式名や新しい成果物を先に固定しない。

1. **懸念をPOM例へ具体化する。** 現行BOM / Parent / Starterの構成を確認し、BOM import、module / Starterのdependency追加のどこで誤選択が起きるかを整理する。実チームの具体例が未取得なら、仮例と明記し、必要な確認事項を残す。
2. **アプリ型と構成条件の候補を整理する。** 型の分類基準、必須・任意・禁止・併用条件、Customer固有依存の拡張余地を表にする。実案件の型を推測して確定しない。
3. **3案をMaven上の実現方法へ落とす。** BOM / Parent / Starter / 検査の役割とCustomer側のPOM記載例を比較する。Mavenの継承・import・version上書き等の仕様を一次資料で確認し、宣言だけで実現できる範囲と検査が必要な範囲を分ける。
4. **実効性と運用負担を評価する。** 個別依存の追加、別BOMのimport、version上書き、exclusion、設定変更、検査skip等に対して、何を検出できるか、検査実行をどう担保するか、例外を誰が扱うかを比較する。
5. **推奨案と検証計画を提示する。** 採用支援に留める範囲、Framework正式成果物へ影響する範囲、Tooling試作の対象、検証ケース、Owner、承認点をまとめる。配置先・Phase 4 packageとの対応は作業4bの材料にする。

次回の出口は、Customer POM例、構成条件、検出範囲・限界、例外運用、保守責任を説明できる比較・推奨案と、必要な試作の承認提案である。
型の宣言を簡単にする効果と、不適切な構成を防ぐ強制力は別々に評価する。机上確認と実測Evidenceを区別する。

## 4. 別端末での再開手順

対象Repositoryの作業ツリーを確認する。既存変更がある場合は上書きせず、取り込み方法を確認する。
既存cloneで以下を実行する際、fetchは別端末の最新branch取得、switchは当該branchへの切替、pullはfast-forwardのみの更新を目的とする。

```powershell
git status --short --branch
git fetch origin
git switch docs/daily-development-workflow
git pull --ff-only origin docs/daily-development-workflow
git log -3 --oneline
git status --short --branch
```

local branchがまだ存在せずremote branchが取得済みの場合は、switchを次のcommandに置き換える。

```powershell
git switch --track origin/docs/daily-development-workflow
```

取得したHEADに本引継ぎと文書承認記録が含まれることを確認する。端末固有の絶対pathを引き継がず、Repository内の相対pathで資料を開く。

読む順序は`AGENTS.md`、`docs/agent/skills/koiki-project-overview/SKILL.md`、本書、[論点整理](dependency-control-issues-20261001.md)、[作業順序案](pre-phase4-framework-independent-work-review-20260930.md)とする。
現行実装の入口は`koiki-parent/pom.xml`、`koiki-dependencies-bom/pom.xml`、`koiki-archunit-rules/src/main/java/org/koikifw/archunit/`、[Architecture Rules説明](architecture-rules-developer-guide.md)。

## 5. 承認境界と未実施事項

- 作業3の文書承認を、具体的な制御方式・型・POM記法の採用承認へ読み替えない。次回の分析・設計は進めるが、Tooling試作・実行や正式成果物の変更は対象と承認範囲を提示して判断する。
- Parent / BOM / Starter / Maven module / Public API / Architecture Rules / CIは未変更。新しいfixture、Maven実行、IDE設定変更、cache操作は未実施。
- Customer側のCI、具体的な誤記載例、型別構成条件、運用合意は未確認。
- Project Templateが必要になる案はPhase 5境界を別途整理する。MyBatis・Level 2、Security契約の変更、Phase 4開始、Gate・DoD・正式配布の承認境界を維持する。
- 今回指示されたremote操作は当該branchのpush。PR作成・merge・ruleset変更・workflow dispatch・snapshot publishは含まない。

次回開始時の指示例：

> この引継ぎを読み、依存関係制御の論点整理§4.1の3案を深掘りしてください。まず現行POMと具体的な誤選択ケースを整理し、アプリ型の宣言方法と制約の実効性を比較する設計案へ進めてください。方式を先に固定せず、試作・正式実装が必要な範囲は承認提案として分けてください。
