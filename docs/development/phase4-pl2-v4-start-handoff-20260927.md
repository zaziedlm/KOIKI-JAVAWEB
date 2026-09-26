# Phase 4 PL2 継続作業・V4開始引継ぎ

## 1. 引継ぎ時点

- 作成日: 2026年9月27日
- branch: `docs/phase4-execution-plan-review`
- 作成時HEAD: `00f5c29`（V5 store / 二階層migrationのTooling比較）
- Phase status: `P4-PL2 IN PROGRESS`。P4-AR6実チーム受入、AR-D10、Gate P4-ARは未完了。Gate P4-Fは提案段階で、Phase 4 production開始は未承認
- 次に着手する作業: **PL2-V4 — Rule 28 / 29の負例検証**
- Ownership: Root Reactor外の非配布Toolingによる検証とArchitecture文書。正式Framework Rules / Public APIの変更は対象外

本書は作業再開の入口である。設計とEvidenceの正本は§2の資料とし、ここに記したHEADや作業ツリー状態は
次回セッションの開始時に`git status` / `git log`で再確認する。
作成時点では、[PL2 P4-F判定資料](KOIKI-JavaWeb-FW_Phase4_PL2_P4-F判定資料_v0.1.md#5-次のpl2作業)と
[P4-F限定開始Gate提案](KOIKI-JavaWeb-FW_Phase4_P4-F限定開始Gate提案_v0.1.md#42-pl2の非配布検証案実施承認済み)に
本日の文書整理差分があり、本書も新規作成した。これらは作成時点で未コミットである。

## 2. 次回の参照順

1. `AGENTS.md`、`docs/agent/skills/koiki-project-overview/SKILL.md`を読み、GateとOwnershipを確認する。
2. [Phase 4見直し草案](KOIKI-JavaWeb-FW_Phase4実施計画_見直し草案_v0.1.md)のR1〜R7と§4、
   [P4-F提案§4.2](KOIKI-JavaWeb-FW_Phase4_P4-F限定開始Gate提案_v0.1.md#42-pl2の非配布検証案実施承認済み)の
   非配布検証承認範囲を確認する。
3. [PL2判定資料§5](KOIKI-JavaWeb-FW_Phase4_PL2_P4-F判定資料_v0.1.md#5-次のpl2作業)で現在の順序と待ち条件を確認する。
4. [PL2検証記録§3〜4](../architecture/validation/phase4-pl2-level2-verification.md#3-未実施と次の確認)で
   V1 / V2 / V5の実証済み事項、V4のsource照合・fixture案を確認する。
5. `koiki-archunit-rules`の現行`BusinessModuleRuleSet`、`KoikiArchitectureRules`と既存のpositive / negative test、
   [Tooling README](../../build-support/phase4-level2-verification/README.md)を確認する。

## 3. 固定済みEvidenceと残る判断

| 項目 | 現在地 | 次回以降の扱い |
|---|---|---|
| V1 / V2 | publicationの強制停止・復旧、複数JVM競合、guarded再送とlock喪失の限界をToolingで確認。`96796e9`で固定 | 停止確認の真正性、検知前の競合窓、外部送信fencing、運用者認可・AuditはV2 runbook案とA1 reviewへ残す |
| V5 | JDBC / JPA両profileでSurefire 7件・Failsafe 11件ずつPASS。二階層FlywayのKOIKI / Application所有候補、KOIKI独立upgrade、PostgreSQLでの失敗DDL rollbackを確認。`00f5c29`で固定 | JDBC＋UPDATEはA1 reviewの第一候補。正式schema所有者、DB方言、成功済みmigrationのrollback、性能・運用差は未決定 |
| V3 | FAILED件数とpublicationからの年齢gaugeのみ検証済み | FAILED遷移からの滞留、event / retry / job相関、trace / logと漏えい負例を検証する |
| V4 | Registry記録対象のannotation種別選択を両storeで確認。Rule 28 / 29のsource照合とfixture案を文書化 | ArchUnitのpositive / negative fixtureは未実行。正式RulesとPublic APIは未変更 |

V1 / V2 / V5のTooling PASSをDoD 4-1〜4-5 / 4-12の正式PASS、P4-F通過、A1方式承認と読み替えない。

## 4. 次回の最初の作業：PL2-V4

1. `git status --short --branch`と直近commitを確認し、未コミットの本日分があれば内容を保持して差分を確認する。
2. [検証記録§4](../architecture/validation/phase4-pl2-level2-verification.md#4-rule-28--29のsource照合と検証案未採用)にある
   Rule 28 / 29の期待値を、現行Rulesと既存testへ突き合わせる。
3. 非配布Toolingのfixtureで、Level 0 / 1の同期`@EventListener`許容とtransactional listener拒否、
   Level 2選択時の`@ApplicationModuleListener`配置許容、直接`@TransactionalEventListener`の扱いを検証する。
4. 同期listenerから外部Adapterへの直接依存と、Use Case / Portを介した間接I/O経路を負例にする。
   Rule 1との重複と、静的検査で捕捉できない範囲を結果として明示する。
5. positive / negativeの実行結果、検出できない経路のreview・動作testへの割当、正式Rule変更案を
   `docs/architecture/validation/phase4-pl2-level2-verification.md`へ記録する。

**V4の区切り:** fixtureと検証記録を一組で差分確認し、必要な検証を通してコミット可能にする。
`businessModuleRules(String)`の既存意味、正式`koiki-archunit-rules`、Framework Public API、
Reference production moduleはA1 blocking review前に変更しない。
非配布fixtureの置き方は、既存Toolingとの依存関係を確認して決める。

## 5. V4の後に進める順序

| 順 | 作業 | 次の区切り |
|---|---|---|
| 1 | **V3：D1の観測検証** | FAILED遷移時刻とpublication年齢の区別、初回event・再送・job間の相関、別requestへの漏えい負例をToolingで実証し、Evidenceと一組で固定する |
| 並行 | **V2残件：復旧runbook案** | 停止確認の発行元・有効期限・process識別、複数運用者、試行上限通知、認可・Audit、lock喪失と外部送信fencingを「確認方法・失敗時の停止・Owner」で整理する |
| 2 | **F-1〜F-4の判定資料** | baseline、A1 / A2 / D1の配置と選定理由、DoD実演手順、commit point / rollback、作業別工数・Owner / CI費用を揃える |
| 3 | **Phase 4全体のPL2台帳とF-5** | P4-01〜11・optionalとDoDの追跡、P4-F対象外の待ち条件、P4-AR計画と`AGENTS.md`の改訂提案を用意する |
| 4 | **Owner review** | F-1〜F-5を揃えてP4-Fの採否を判断する。Gate規定、A1 blocking review、production開始は別判断とする |

[PL1差分台帳](KOIKI-JavaWeb-FW_Phase4_PL1_REST利用境界差分台帳_v0.1.md#3-p4-ar6へ渡す確認事項)の
Q1〜Q5とP4-AR6実チーム受入は並行の入力待ちとする。実案件固有の確定情報はNext.js/BFFとKOIKI REST連携のみ。
外部IdP SSOは見込みであり、P4-03BはCustomer主導、当初DoD 4-8 / 4-9のReference Session SPAとは分ける。

## 6. 実行と承認の境界

- [P4-F提案§4.2](KOIKI-JavaWeb-FW_Phase4_P4-F限定開始Gate提案_v0.1.md#42-pl2の非配布検証案実施承認済み)で、
  Root Reactor外のTooling fixture、PostgreSQLを使う検証、Rule 28 / 29案の確認とEvidence記録は承認済み。
- Docker named pipeやMaven `~/.m2`へのsandbox権限エラーでは、`AGENTS.md`の承認済み権限付き実行手順を使う。
  実行環境側の承認が必要ならその手順に従い、拒否を迂回しない。
- 正式Framework / Reference実装、Public API、Starter、migration、配布、workflow / remote操作、
  Gate P4-F採用・production開始はこの検証承認に含まれない。
- V4でRuleの実装不能または規約矛盾が判明したら、推測で正式Ruleを変えず、検出限界と選択肢をA1 reviewへ渡す。

## 7. 次回セッションへの開始依頼文

> `docs/development/phase4-pl2-v4-start-handoff-20260927.md`を起点にPL2を再開してください。
> まずbranch / HEAD / 作業ツリーと承認境界を確認し、次タスクV4のRule 28 / 29負例検証を
> 非配布Toolingで進め、検証記録を更新してください。正式Rules / Public APIやproduction成果物は変更しません。
