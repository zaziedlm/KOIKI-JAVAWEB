# Phase 4 S1 R0-B：文書source固定local commitレビュー（2026-10-09）

**状態：OWNER COMMIT APPROVED（2026-10-09）、実行前記録。** 規約レビュー案RL-01〜06と条件付きR0-B開始は承認済み。[承認記録§9.2](phase4-s1-r0-rule28-29-level-selection-owner-review-20261009.md)に従い、実装前の文書source固定対象を示す。Ownerは「local commit 1回で固定してよい」と明示し、下記14 fileの現在branchへの1回固定を承認した。commit実行後のSHA／clean確認は後続R0-B runへ記録し、本票の追加commitをしない。

**対象branch／基点：** `feature/phase4-s1-reference-foundation` / `fe93b5da76a85fd6a4c41409c725c35661fac007`。commitは現在branchへ1回だけ、下記14 fileの文書に限定する。push／PR／merge／publishは含まない。

## 1 対象14 file

| path | 内容 |
|---|---|
| `AGENTS.md` | 文書source固定・preflight条件付き規約限定開始、上限・停止条件、Reference／remote除外 |
| `docs/agent/skills/koiki-project-overview/SKILL.md` | 正本の承認導線・開始条件 |
| `docs/agent/skills/koiki-business-feature-work/SKILL.md` | Level選択契約とruntime／R1開始の分離 |
| `docs/architecture/adr/ADR-050-module-event-level-selection.md` | 採用済み設計／実装未了、互換性・保証限界 |
| `docs/architecture/adr/README.md` | ADR-050登録 |
| `docs/architecture/grand-design/KOIKI-JavaWeb-FW_グランドデザイン_v0.2.md` | §17.5の採用仕様、静的Levelとruntime対象の責務分離 |
| `docs/development/KOIKI-JavaWeb-FW_Phase4_P4-F限定開始Gate提案_v0.1.md` | 規約blocking review部分の条件付き承認 |
| `docs/development/KOIKI-JavaWeb-FW_Phase4_PL2_P4-F判定資料_v0.1.md` | CP-F1に規約部分承認・未完了境界 |
| `docs/development/architecture-rules-developer-guide.md` | 採用仕様例・現行artifact未実装・検出限界 |
| `koiki-archunit-rules/README.md` | 利用可能性と採用仕様の区別 |
| `docs/development/phase4-s1-purpose-dod-stage-mapping-20261009.md` | R0→R1順序・承認・source固定待ち |
| `docs/development/phase4-s1-r0-reference-async-start-review-20261009.md` | Reference違反例外撤回、規約受入後のR1再確定 |
| `docs/development/phase4-s1-r0-rule28-29-level-selection-owner-review-20261009.md` | 採用API／Rule、予算・対象・条件付き承認記録 |
| `docs/development/phase4-s1-r0-rules-document-source-fixed-commit-review-20261009.md` | 本commit対象と実行後確認 |

最初のR0資料2件と目的対応表の先行文書差分を含めて固定する。code／test／POM／SQL／依存／runtime／CI差分は含まない。規約票の文書上限16 file内。別差分を検出した場合は含めず対象を再確認する。

## 2 操作案と確認

commit message案：`docs: approve S1 R0 event-level rules contract`。

承認後、branch／HEAD・対象集合・差分を再確認し、列挙14 pathだけをstageして1回commitする。実行環境が`.git`書込み承認を要求する場合はその手順に従う。commit対象14件・親HEAD・新HEAD・worktree cleanを確認し、source identityを後続R0-B Evidenceへ記録する。source固定のためにこの票自身へ新commit SHAを追記して追加commitを発生させない。

その後は承認済みpreflightだけを開始し、成立時に規約code／test作成へ進む。preflight不成立・不足依存・上限／scope不一致は停止する。文書commitで結果受入・Reference R1やremote開始を認定しない。

## 3 作成時の確認

文書の相互リンク・承認範囲・差分を確認する。今回の作業は文書整合のみで、実装・Maven／Docker検証、git add／commit／remoteは未実施。承認後の実測は新しいR0-B runとして管理し、旧PL2／B2のartifact epochや受入結果を上書きしない。
