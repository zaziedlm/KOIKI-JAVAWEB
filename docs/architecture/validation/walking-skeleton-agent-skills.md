# Walking Skeleton — Agent Skills Validation

**Status:** Completed

## Result

| 検証 | 実結果 | 判断 |
|---|---|---|
| 最小Skill | `koiki-project-overview`と`koiki-business-feature-work`の2件を正本として作成した | PASS |
| Codexからの発見 | 新規セッションのSkill一覧に2件が掲載され、スラッシュコマンドから呼び出して正本を参照できた | PASS |
| Claude Codeからの発見 | `.claude/skills/`のアダプターをスラッシュコマンドから呼び出して正本を参照できた | PASS |
| Skill構造 | Skill Creator付属の`quick_validate.py`で正本2件とアダプター4件がすべてvalidとなった | PASS |
| OpenSpecとの責務分離 | OpenSpecは変更固有の要求・設計・タスク、KOIKI SkillはRepository横断の判断手順として分離できた | PASS |
| OpenSpec成果物 | `openspec validate --all --strict`でmain spec 4件が成功した | PASS |

## Placement and Ownership

`docs/agent/skills/`をKOIKI固有Skillの正本とする。Codex用の`.agents/skills/`とClaude Code用の
`.claude/skills/`には、正本を読むことだけを指示するアダプターを置く。これにより、複数エージェントへ
同じ設計規則を複製して将来不整合になることを避ける。

ルート`AGENTS.md`はPhase 0のRepository共通指示とSkillの適用条件を示す。`CLAUDE.md`は
Claude Code固有の薄い入口とし、共通指示と正本Skillへ誘導する。

## Scope of the Minimal Skills

### `koiki-project-overview`

- Phaseと検証・正式実装の区別
- Framework / Reference / Customer / Walking SkeletonのOwnership
- 業務Moduleを第一の分割軸とするモジュラーモノリス
- 正本となる設計、ADR、検証記録、OpenSpec、ビルド設定の選択
- Phase 0で検証済みの範囲と後続Phaseへ残す判断

### `koiki-business-feature-work`

- 所有Moduleと公開境界
- Tier 1 SIMPLE / Tier 2 RICHの選択
- Inbound Adapter、Application Use Case、Domain、Outbound Adapterの責務配置
- JPA / MyBatis、共有・分離永続化モデル、read modelの判断
- 同期・非同期Domain EventとModule間連携
- MVC / REST境界、Framework昇格条件、テスト方針

ArchUnit、Maven、NullAway等で機械検査できる規則そのものはSkillへ複製しない。

## Scenario Review

| 代表ケース | Skillから導く判断 |
|---|---|
| 状態遷移のない単純なマスタ更新 | Tier 1を開始点とし、Application Use Caseが処理を調整する |
| 複数状態と不変条件を持つ申請 | Tier 2を選び、状態遷移と不変条件をDomain Modelへ置く |
| 他Moduleの確認成功が処理成立条件 | 値のみのDomain Eventを同期`@EventListener`で受け、直接Bean呼出を避ける |
| 承認後のメール送信 | 外部副作用なので非同期候補とし、Level 2未決定なら実装を先行しない |
| 業務実装中に見つけた共通処理 | その場でFrameworkへ移さず、複数案件での実績、契約安定性、Spring標準、ADRを確認する |
| React SPA向けREST API | ControllerとDomain/JPA Entityの境界は適用し、Problem Details、Security、SPA詳細は後続Phaseで決める |

これらはPhase 0で確定した原則と保留事項を区別でき、未検証の詳細規約を先行生成しない。

## OpenSpec Responsibility Check

OpenSpecのproposal、spec、design、tasksは個別changeの目的、要求、設計、実装順序を定める。
KOIKI Skillsは、それらを読む順序と、Ownership、Tier、責務配置など複数changeに共通する判断方法を定める。
SkillはOpenSpec成果物を代替または上書きしないため、責務の競合は認められなかった。

## Validation Commands

```powershell
# 正本2件とエージェント別アダプター4件に対して実行
uv run --with pyyaml python <skill-creator>/scripts/quick_validate.py <skill-directory>

fnm env --shell powershell | Out-String | Invoke-Expression
fnm use 24
openspec validate --all --strict
```

`quick_validate.py`用のPyYAMLは`uv`の一時環境でのみ使用し、Repositoryの依存関係へ追加していない。

## Conclusion

Phase 0の最小2 Skillは、KOIKIの全設計を写経せず、AIが作業開始時と業務機能実装時に必要とする
判断手順を提供できる。CodexとClaude Codeの双方から同じ正本を参照でき、OpenSpecとも共存できる。
5種のフル版、REST / Security / SPAの具体規約、非同期Level 2の実装規約は後続Phaseへ残す。
