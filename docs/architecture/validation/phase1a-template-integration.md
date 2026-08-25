# Phase 1a Template Integration — B5実装・検証計画

**準備日:** 2026年8月25日<br>
**対象branch:** `feature/phase1a-template-integration`<br>
**状態:** COMPLETE / GATE 1〜4 ACCEPTED<br>
**Ownership:** Tooling<br>
**対象:** B5 Tier 1 / Tier 2 Feature Template統合検証<br>
**開始baseline:** `main` / `9c9f519`（B4完了後の状態整合PR #12 merge）

## 1. 目的と完了条件

Phase 1a実行計画のB5とDoD 1a-1・1a-2・1a-4に基づき、B1のTier 1 / Tier 2
Feature Templateが生成直後からB3のArchUnit rules、B4のNullAwayおよびSpring Modulith
Level 0のquality gateを同時に適用できることを実証する。

B5は次をすべて満たしたときだけ`COMPLETE`とする。

1. Tier 1 `catalog`とTier 2 `approval`を未変更Templateから再生成できる。
2. 生成後の各production packageがそのpackage自身で`@NullMarked`を宣言し、ParentのNullAwayでcompile成功する。
3. `KoikiArchitectureRules.businessModuleRules` を生成後の両moduleへ適用し、正常系が成功する。
4. Spring Modulith Level 0が両moduleを発見し、`verify()`が成功する。
5. Tier 1 / Tier 2それぞれのArchUnit違反がKOIKI rule IDと修正方針を伴って失敗する。
6. Tier 1 / Tier 2それぞれのnull違反がNullAway固有diagnosticを伴って失敗する。
7. 負例の後にTemplateから再生成し、同じ正常系が再度成功する。
8. Spring Modulithをtest scopeに限定し、生成moduleのruntime dependencyへ混入させない。
9. Root ReactorとWindows / Ubuntu CIが成功する。

## 2. 作業境界

| 対象 | B5での扱い |
|---|---|
| Feature Template / generator | B1の生成契約を維持し、現存production subpackageの`package-info.java`だけを追加する |
| Generated fixture | Tooling所有の`verification/generated/`だけを負例注入・再生成復元の対象とする |
| ArchUnit | B3承認済みの1 public class / 2 public methodとrule message contractを変更しない |
| NullAway | B4承認済みのParent設定と`OnlyNullMarked=true`を変更しない |
| Spring Modulith | 2.1.0 Level 0をtest scopeに限定し、runtime依存を追加しない |
| 配布 | B5 fixtureはRoot Reactor、正式release unit、snapshot repositoryへ含めない |
| Deferred | C1 / C2の外部Consumer、C3のjapicmp、C4のruntime matrix、後続Phaseの成果物 |

B5ではFramework Public API、Maven coordinates、ADR、業務module、migration、runtime設定を
追加または変更しない。

## 3. 現在の接続点

| Gate | 現状 | B5で追加する証拠 |
|---|---|---|
| Template生成 | B1で両Tierの生成・unit test成功 | 同じ生成物に3 quality gateを同時適用 |
| Level 0 | `architecture-tests`から両moduleを発見して成功 | ArchUnitと同じtest moduleから再実証 |
| ArchUnit | B3の正式Rules artifactとcompliant / negative fixtureが成功 | 実際のTemplate生成物の正常・Tier別負例 |
| NullAway | B4の隔離fixtureでpositive→negative→restoreが成功。module rootの`@NullMarked`はsubpackageへ伝播しない | 現存の各production subpackageへの宣言、実際のTemplate生成物の正常・Tier別負例・復元 |

## 4. 検証Reactorの変更案

```text
KOIKI Dependencies BOM
KOIKI Architecture Contract
KOIKI ArchUnit Rules
Feature Template Verification parent
├── generated/catalog       Tier 1 SIMPLE
├── generated/approval      Tier 2 RICH
└── architecture-tests      ArchUnit + Spring Modulith Level 0
```

- 検証Reactorに正式`koiki-archunit-rules`をmoduleとして追加する。
- Tier 1の`application.usecase`・`adapter.outbound.persistence`、Tier 2の`application.usecase`・
  `domain.model`・`domain.repository`に、`@NullMarked`を宣言する`package-info.java`を生成する。
- 未使用の将来subpackageやtest packageの`package-info.java`は先行生成しない。
- `architecture-tests`はRules artifactをtest dependencyとし、生成後の`catalog`と`approval`を
  `businessModuleRules("org.koikifw.templateverification")`で検査する。
- Level 0とArchUnitは別testに分け、失敗原因を識別できるようにする。
- Rules、ArchUnit、Spring ModulithおよびJUnitは検証testに限定し、生成moduleのproduction dependencyへ
  追加しない。

## 5. 負例と復元経路

`verify-feature-templates.ps1`は追跡対象Templateを変更せず、毎回再生成する
`verification/generated/`に限定して意図的違反を注入する。

| 段階 | 対象 | 違反 | 必須diagnostic |
|---|---|---|---|
| Positive | Tier 1 + Tier 2 | なし | Reactor、ArchUnit、Level 0、NullAwayがすべて成功 |
| ArchUnit negative 1 | Tier 1 | 生成済みmodule rootの`@KoikiModule`宣言を隔離fixture上だけで除去 | `KOIKI-ARCH-007`と`008`、`catalog` |
| ArchUnit negative 2 | Tier 2 | 同上 | `KOIKI-ARCH-007`と`008`、`approval` |
| NullAway negative 1 | Tier 1 | non-null getterから`null`を返す | `[NullAway]`、`CatalogItem.java` |
| NullAway negative 2 | Tier 2 | non-null getterから`null`を返す | `[NullAway]`、`ApprovalRequest.java` |
| Restore | Tier 1 + Tier 2 | Templateから再生成 | Positiveと同じ検証が再度成功 |

負例buildは非0終了だけでなく、対象Tierと各tool固有diagnosticの両方を検査する。
scriptは`finally`で生成fixtureを正常状態へ戻し、最後の正常build成功を復元証拠とする。

## 6. Owner Review Gate

| Gate | Review対象 | 承認条件 | 状態 |
|---:|---|---|---|
| 1 | 所有権、subpackageのNull Safety、Reactor接続、負例注入、復元、Deferred | 現存production packageだけを`@NullMarked`にし、負例でtracked Templateを書き換えず、B1〜B4の承認済み契約を維持する | ACCEPTED（2026年8月25日、Shuichi Kataoka） |
| 2 | 正常系実装 | 両TierでArchUnit、Level 0、NullAwayが同時成功 | ACCEPTED（2026年8月25日、Shuichi Kataoka） |
| 3 | Tier別負例と復元 | 4負例が期待した識別可能なdiagnosticで失敗し、最後に正常復元 | ACCEPTED（2026年8月25日、Shuichi Kataoka） |
| 4 | Regression、CI、Validation、Deferred | Root ReactorとWindows / Ubuntu CIが成功し、Milestone Bを閉じられる | ACCEPTED（2026年8月25日、Shuichi Kataoka） |

Gate 1でPublic API、Template生成契約、production dependencyまたはPhase scopeの変更が必要と
判明した場合は実装へ進まず、Owner Reviewへ戻す。

2026年8月25日のOwner ReviewでGate 1の整理内容を承認した。以降の実装は、
本Gateの範囲とstop conditionを変更せずに進める。

### 6.1 Gate 2初回実行とstop condition

Gate 1承認後、現存production subpackageへ`@NullMarked`を宣言する`package-info.java`を
生成し、正式Rules artifactを検証Reactorへ接続した。最初のpositive buildでLevel 0は成功したが、
ArchUnit Rule 16が`approval.domain.repository.package-info`をRepository contractとして評価し、
Spring Data Commons `Repository`を継承していないと判定した。

```text
[KOIKI-ARCH-016]
Class <org.koikifw.templateverification.approval.domain.repository.package-info>
does not extend org.springframework.data.repository.Repository
```

`package-info` bytecodeはpackage annotationを保持するmetadataであり、Domain Repository contractではない。
このため、Rule 16が同classを検査対象から除外することはADR-024の意味を弱めない。
一方、B3承認済みrule実装の修正になるため、B5のstop conditionに従い以降の負例実行を
停止した。scriptの`finally`によるTemplate再生成は成功し、生成fixtureは正常sourceへ戻っている。

再開案は、Rule 16だけで`package-info`をRepository contract判定から除外し、
`@NullMarked`付きpackage metadataと有効なCommons Repositoryが共存するfocused regression testを
`koiki-archunit-rules`へ追加する方式とする。Public API、rule ID、message contractおよび
Plain / `JpaRepository`違反の判定は変更しない。

Ownerは2026年8月25日にこの限定修正案を承認した。Rule 16の判定からsimple nameが
`package-info`のclassだけを除外し、`@NullMarked`付きpackage metadataを含むcompliant fixtureの
focused regression testを追加した。既存のPlain Repository / `JpaRepository`違反testを含む
ArchUnit Rules全65件が成功し、Rule 16の適用範囲がRepository contractに維持されることを確認した。

### 6.2 ローカル実装・検証結果

2026年8月25日にJDK 21 / Maven Wrapperで次を実行した。

| 検証 | 結果 |
|---|---|
| `mvnw.cmd --offline --batch-mode --no-transfer-progress -pl koiki-archunit-rules -am clean verify` | SUCCESS。Architecture Contract 4件、ArchUnit Rules 65件成功 |
| `pwsh -NoProfile -File build-support/feature-templates/verify-feature-templates.ps1` | SUCCESS。全7 production packageの`@NullMarked`宣言、正常系、Tier 1 / Tier 2 ArchUnit負例、Tier 1 / Tier 2 NullAway負例、復元、runtime dependency検査が完走 |
| Positive / Restore | 両Tierのcompile・unit test、Spring Modulith Level 0、正式ArchUnit rulesが成功。`architecture-tests`は2件成功 |
| ArchUnit negative | Tier 1は`catalog`、Tier 2は`approval`について`KOIKI-ARCH-007` / `008`を検出 |
| NullAway negative | Tier 1は`CatalogItem.java`、Tier 2は`ApprovalRequest.java`について`[NullAway]`を検出 |
| Runtime dependency | 生成moduleのruntime dependency treeにSpring Modulithが含まれないことを確認 |
| Root Reactor `clean verify` | SUCCESS。Architecture Contract 4件、ArchUnit Rules 65件成功 |
| B4 `verify-null-safety.ps1` | SUCCESS。positive→negative→restoreが完走 |
| PR #13 CI run #32825065374 | SUCCESS。`Verify (windows-2025)`と`Verify (ubuntu-24.04)`がともに成功 |

負例では意図したNullAway診断に加えて、getterからfield参照を除いた結果としてError Proneの
`UnusedVariable`も報告される。ただしscriptは終了コードだけに依存せず、対象source名と
`[NullAway]`を必須条件として検査するため、Null Safety違反の検出証拠は分離できている。

Ownerは2026年8月25日にローカル実装・検証結果を確認してGate 2 / 3を承認し、PR #13の
Windows / Ubuntu CI成功を確認してGate 4を承認した。これによりGate 1〜4と完了条件1〜9を
すべて満たしたため、B5およびMilestone Bを`COMPLETE`とする。

## 7. 見積もり再校正

B1〜B4の実施結果から設定したB5の開始rangeは、直接`4〜7標準人日`、
AI支援Owner稼働`2〜5日`であった。これは経過日数または納期ではない。

ローカル統合検証では、positive、Tier別ArchUnit負例2回、Tier別NullAway負例2回、restoreの
合計6回の`clean verify`とruntime dependency tree検査が必要になった。CI run #32825065374では、
Ubuntu jobが約4分26秒、Windows jobが約6分48秒で成功し、20分timeout内に収まった。

実装上の不確実性だったRule 16誤検出、Tier別diagnostic分離およびCI所要時間は解消した。
B5の実装・Owner Reviewは完了し、残るPR記録更新後のCI再確認とmainへのmergeは統合作業として扱う。

## 8. Stop condition

- 生成直後の正常TemplateがB3承認済みruleに違反する。
- 負例が別のcompile errorだけで失敗し、tool固有diagnosticを示さない。
- 負例注入または復元が`verification/generated/`の外側を変更する。
- ArchUnitまたはSpring Modulithが生成moduleのproduction / runtime dependencyへ必要になる。
- Public API、Maven coordinates、ADRまたは後続Phaseの設計判断が必要になる。
