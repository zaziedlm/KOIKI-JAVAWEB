# Phase 1a B4 作業引継ぎ — 2026-08-25

## 1. この文書の位置づけ

この文書は、作業を一旦区切り、新規AIセッションでPhase 1a B4を安全に開始するための運用上の引継ぎメモである。
設計判断やB4の検証結果の正本ではない。B4固有の計画と実装証拠は、次回セッションで
`docs/architecture/validation/phase1a-null-safety.md`を新規作成して管理する。

判断が競合する場合は、次の順に正本を確認する。

1. Repository rootの`AGENTS.md`
2. `docs/agent/skills/koiki-project-overview/SKILL.md`
3. `docs/development/KOIKI-JavaWeb-FW_Phase1a実行計画_v0.1.md`
4. `docs/architecture/grand-design/KOIKI-JavaWeb-FW_グランドデザイン_v0.2.md`
5. `docs/architecture/adr/README.md`のADR-034 Null Safety
6. `docs/development/KOIKI-JavaWeb-FW_Phase1a_WalkingSkeleton_Transition_Inventory_v0.1.md`
7. 実効構成である`koiki-parent/pom.xml`、Root `pom.xml`、Maven Wrapper、CI

業務機能を変更しないため、B4開始時の適用Skillは`koiki-project-overview`である。B4の検討中に業務module、
Controller、Use Case、Domain Model等を変更する必要が生じた場合だけ、`koiki-business-feature-work`も使用する。

## 2. 引継ぎ時点のGit・remote状態

| 項目 | 状態 |
|---|---|
| B3 PR | [PR #10](https://github.com/zaziedlm/KOIKI-JAVAWEB/pull/10)をmerge commit方式で`main`へMerge済み |
| `main` | `86b003365a6ffd511683167a673639163d73a56b` |
| `main` CI | [run #32762223565](https://github.com/zaziedlm/KOIKI-JAVAWEB/actions/runs/32762223565)、Windows / UbuntuともSUCCESS |
| B4 branch | `feature/phase1a-null-safety` |
| B4 branch baseline | `main` / `86b003365a6ffd511683167a673639163d73a56b` |
| B4実装 | 未着手 |

この引継ぎ文と索引更新はB4 branchの最初のcommitとして保存する。次回はcommit IDだけでなく、branch名、upstream、
最新履歴およびworktreeがcleanであることを確認する。

## 3. Phase / Ownership / 対象

| 項目 | 内容 |
|---|---|
| Phase / status | Phase 1a Milestone B / B3 COMPLETE / B4 START READY |
| Ownership | Tooling |
| 主対象 | `koiki-parent`のcompiler policyとB4専用NullAway検証経路 |
| 関連対象 | JSpecify `@NullMarked`方針、Root Reactor、Maven Wrapper、CI、Validation |
| 検証 | 正常build → 意図的NullAway違反の期待失敗 → 正常状態への復元build |
| 後続 | B5でTier 1 / Tier 2 Feature TemplateへArchUnit、Level 0、NullAwayを統合 |

B4はDoD 1a-4のうち、A2で構成済みのParent設定を正式なpositive / negative / restore証拠へ落とし込むWPである。
Framework、Reference、Customer、Walking Skeletonの業務成果物を追加する作業ではない。

## 4. B4開始baseline

`koiki-parent/pom.xml`には、A2で次の設定が既に存在する。

- Java 21、forked `javac`
- Maven Compiler Plugin 3.15.0
- Error Prone 2.50.0
- NullAway 0.13.8
- JSpecify 1.0.0はBOM管理
- `-Xep:NullAway:ERROR`
- `-XepOpt:NullAway:OnlyNullMarked=true`
- `-XepOpt:NullAway:JSpecifyMode=true`

通常のRoot Reactor `clean verify`、PR CIおよびB3実装はこのParent設定を継承して成功している。ただしA2 / A3の
Validationに記録されたとおり、Java production sourceを対象とするB4固有の正常→違反→復元の独立検証は未実施である。

グランドデザインでは、JSpecify＋NullAwayをPhase 1a開始時から適用し、NullAway違反をbuild failureにする。
モジュールrootの`package-info.java`へ`@NullMarked`と`@KoikiModule`を併記する方式はPhase 0で成立確認済みである。

## 5. B4の完了条件

Phase 1a実行計画とDoD 1a-4に基づき、少なくとも次を満たす。

1. Parentの実効compiler設定、annotation processor version、JSpecify dependency境界を記録する。
2. `@NullMarked`配下の正常なJava production sourceがMaven Wrapperでcompile / verifyに成功する。
3. 同じ検証対象へ意図的なnull違反を与えると、NullAwayの特定diagnosticを伴って非0終了する。
4. 違反を除去または正常fixtureへ戻すと、同じ検証経路が再び成功する。
5. 意図的違反を通常production source、通常Root Reactor、配布artifactまたは最終commitへ残さない。
6. WindowsローカルとPR CIで再現可能な正式経路を用意し、結果をValidationへ記録する。
7. B5、C1以降または後続Phaseの成果物を先行実装しない。

負例の具体的な配置方式、script、Maven module構成、Gate分割およびfailure messageの固定範囲は未決定である。
次回セッションでは既存構成とWalking Skeletonの証拠を調査したうえで案を提示し、Owner Review後に実装する。

## 6. 重要な境界

- `walking-skeleton/negative-tests/nullaway/`のcodeや一時座標を正式成果物へ直接コピーしない。
- 負例は専用fixtureまたは専用検証手順へ隔離し、通常buildを恒常的に失敗させるswitchをproduction sourceへ入れない。
- `koiki-archunit-rules`のtest fixtureにある意図的`return null`は、ArchUnitがimportするbytecodeを作るためのfixtureであり、
  B4の正式なNullAway negative evidenceとして代用しない。
- `@SuppressWarnings("NullAway")`は既存の意図的contract testに限定されている。B4負例を抑制して成功させない。
- Parent設定を変更する場合は、現在成功しているArchitecture Contract、ArchUnit RulesおよびFeature Template検証への
  影響をRoot Reactorで確認する。
- NullAwayが機械検査する規則をSkill本文へ複製しない。
- JSR 305 annotationを追加しない。JSpecifyを使用する。

## 7. B4で行わないこと

- B5のFeature Template最終統合およびArchUnit / Level 0の統合負例
- C1 / C2のsnapshot公開とRepository外Consumer
- C3のjapicmp / Public API baseline
- C4のJava 21 / 25同一artifact runtime検証
- Runtime、Security、Reference業務、REST、SPA、Flyway、MyBatis
- Spring Modulith Level 1 / 2、Named Interface、非同期event
- 未使用の将来module、package、Starter、Public APIの生成

## 8. 次回セッションの開始手順

```powershell
git fetch origin
git switch feature/phase1a-null-safety
git pull --ff-only
git status --short --branch
git log -5 --oneline
java -version
.\mvnw.cmd -version
```

期待状態:

- branchが`feature/phase1a-null-safety`
- remote trackingが設定済み
- baselineに`86b0033`が存在する
- この引継ぎ文のcommitが履歴に存在する
- worktreeがclean
- Java 21とMaven Wrapper 3.9.16を実行可能

## 9. 次回最初に行うこと

B4実装前に、次を調査・具体化してOwnerへ提示する。

1. 現在のParent compiler設定と、全正式Java moduleの`@NullMarked`適用状況
2. Walking SkeletonのNullAway検証から引き継ぐ知見と、直接流用しない資材
3. 通常sourceへ違反を残さないnegative fixture / verification harness候補
4. positive、negative、restoreそれぞれの実行commandと期待exit code / diagnostic
5. Root ReactorとWindows / Ubuntu CIへの影響
6. B4のOwner Review Gate、commit境界、Validation記録案
7. stop / return condition

特に、負例を一時的に実sourceへ書き込んで復元する方式と、Reactor外fixtureを独立buildする方式の安全性・再現性を
比較する。作業ツリーの復元に依存する方式を採る場合は、失敗・中断時にも正常sourceを確実に戻せる手順を先に設計する。

## 10. 新規セッションへの開始依頼文

次の文章を、新しいセッションの最初の依頼として使用できる。

> KOIKI-JavaWeb-FWのPhase 1a B4 Null Safety正式化を開始します。Repository rootの`AGENTS.md`、
> `docs/agent/skills/koiki-project-overview/SKILL.md`、
> `docs/development/phase1a-b4-handoff-20260825.md`、Phase 1a実行計画、ADR-034、
> `koiki-parent/pom.xml`および既存NullAway関連Validationを確認してください。作業branchは
> `feature/phase1a-null-safety`、baselineはB3をmergeした`main`の`86b0033`です。B4実装は未着手です。
> 最初にGit・Java・Maven環境と既存Parent設定を検証し、正常→NullAway違反失敗→復元成功を通常sourceへ
> 違反を残さず再現する方式、Owner Review Gate、検証command、stop conditionを具体化して提示してください。
> 承認前にB4実装やB5以降へ進めないでください。

## 11. 引継ぎ時点の検証

- B3 PR #10のmerge commit `86b0033`に対するWindows / Ubuntu CIは成功済み。
- B4 branchはその`main`から作成した。
- この時点ではB4のPOM、Java source、fixture、script、CI workflowを変更していない。
- 引継ぎ文書は作業再開用の情報だけであり、B4の設計承認または実装完了を意味しない。
