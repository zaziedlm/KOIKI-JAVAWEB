# Phase 2 Gate B GB-5 required check化レビュー

## 1. Review境界

- 作成日: 2026年9月10日
- 承認日: 2026年9月10日
- 作業パッケージ: Phase 2 / Gate B / GB-5
- 対象check: `Local Identity Session Audit Integration`
- 対象ruleset: `main-merge-protection` / ID `21140116`
- 対象PR: [#29 Phase 2 Gate B: add security aggregate CI candidate](https://github.com/zaziedlm/KOIKI-JAVAWEB/pull/29)
- Evidence HEAD: `f30a34094dce21e5943513b4bbb32c9b84be4a75`
- ステータス: `COMPLETE — REQUIRED CHECK ACTIVE`
- 所有者: CI Policy / Architecture Evidence
- 本reviewでのremote変更: なし

本reviewは、GB-4でremote 3回連続成功した候補jobをmain rulesetのrequired status checkへ追加するかを判断する。
workflow、product code、secret、environment、権限またはtimeoutは変更しない。ruleset変更、Draft解除、mergeおよび
merge後main CIはArchitecture Owner承認後の実行境界とする。

## 2. 承認済みEvidence

- commit済みclean final HEADでB4-5 local closeoutが成功した。
- Linux fresh runnerの同一final HEADで候補jobが39分11秒、32分20秒、34分31秒の3回連続成功となった。
- 全3回でaggregateと独立した最終repository / owned-resource inspectionが成功した。
- final HEADの`Verify`、Public API、Java Runtime、Phase 1b Milestone B / C、Security Foundationがすべて成功した。
- jobは`ubuntu-24.04`、Temurin 21、`contents: read`だけを使用し、secret、Packages、environment権限およびMaven cacheを追加しない。
- job timeout 60分、aggregate 52分、最終inspection 5分で、最大実測は39分11秒だった。

詳細は`phase2-gate-b-gb-4-verification.md`を正本とする。

## 3. 現行main ruleset

2026年9月10日のread-only確認結果は次のとおりである。

| 項目 | 現行値 |
|---|---|
| enforcement | `active` |
| 対象 | default branch `main` |
| strict required status checks | `true` |
| create時の例外 | なし |
| bypass actor | なし |
| current user bypass | `never` |
| pull request必須 | 有効 |
| force push / deletion | 禁止 |

required status checksは次の6件で、すべてGitHub Actions integration ID `15368`へ固定されている。

1. `Verify (ubuntu-24.04)`
2. `Public API Compatibility`
3. `Java Runtime Compatibility`
4. `Milestone B Integration`
5. `Milestone C Closeout`
6. `Security Foundation Integration`

## 4. 選択肢

### A — 現在required checkへ追加する（推奨）

`Local Identity Session Audit Integration`を既存6件と同じGitHub Actions integration ID `15368`で追加する。
strict policy、bypassなし、既存checkおよびPR保護は変更しない。

このjobは既に全PRで起動するため、required化そのものはrunner使用量や実行回数を増やさない。変化は、Local Identity、
Session、Audit、package済みReference、PostgreSQL、複数processおよびcleanupの成功をmerge条件として強制する点である。
通常のPR feedbackは短いcheckから先に得られるが、merge可能になるまでの下限は候補jobの実測約32〜39分となる。

### B — Gate Cまでrequired化を保留する

CI実行量はAと同じだが、Gate Bの主要integrationが失敗していても他checkだけでmerge可能な期間が残る。
GB-C6で定めたremote success後のrequired化判断を再度先送りする明確な理由が必要となる。

### C — workflow自体を軽量化してからrequired化する

現行3ラウンドaggregateを分割・短縮すると、GB-C1で承認したlocal / CI共通正本とremote 3連続Evidenceを変更する。
新構成の再reviewと再検証が必要であり、Gate B closeout中の選択肢としては推奨しない。将来、実測履歴に基づく
CI最適化sliceとして扱う。

## 5. 推奨するruleset差分

required status checksの配列へ次の1件だけを追加する。

```json
{
  "context": "Local Identity Session Audit Integration",
  "integration_id": 15368
}
```

次は変更しない。

- `strict_required_status_checks_policy: true`
- 既存required check 6件
- pull request rule
- deletion / non-fast-forward rule
- bypass actorなし
- workflow名、job名、timeout、permissions

job名はruleset contractになるため、required化後のrenameは互換性変更として別reviewする。

## 6. 承認後の実行順序

1. 現行rulesetを再取得し、review時点から差分がないことを確認する。
2. 既存ruleset全体を保持したまま、対象context 1件だけを追加する。
3. rulesetを再取得し、strict、bypassなし、既存6件および新規1件を確認する。
4. PR #29で新規required contextが認識され、final HEADの成功checkへ結び付くことを確認する。
5. README、GB-5 EvidenceおよびPhase 2実行計画へ結果を記録する。
6. 記録をcommit / pushし、更新HEADのPR CIを確認する。
7. Draft解除、mergeおよびmerge後main CIは別Architecture Owner判断として残す。

ruleset更新に失敗または予期しない差分がある場合は追加操作を停止し、元のrulesetを推測で上書きしない。

## 7. Architecture Ownerへ求める判断

1. GB-4の同一final HEAD remote 3回連続成功と全回cleanup成功がrequired化の必要条件を満たすか。
2. 候補jobが既に全PRで起動するため、required化はCI実行量を増やさずmerge保護だけを強化するとの評価が妥当か。
3. 約32〜39分のmerge待ち時間を、PostgreSQL、複数processおよびpackage済みjourneyの保護として受け入れるか。
4. strict、bypassなし、既存6 checkを維持し、正確なcontext 1件だけを追加する差分を承認するか。
5. timeoutやaggregateをGate Bで軽量化せず、将来のCI最適化を実測履歴に基づく別sliceへ残すか。
6. ruleset更新後もDraft解除、PR mergeおよびmerge後main CIを別Owner判断へ残すか。

推奨結論は1〜6を承認し、選択肢Aにより`Local Identity Session Audit Integration`をrequired checkへ追加することである。

## 8. Architecture Owner承認

2026年9月10日、Architecture Ownerは上記1〜6とレビュー支援内容を確認し、すべて承認した。
選択肢Aにより、`Local Identity Session Audit Integration`を既存6件と同じGitHub Actions integration ID
`15368`でmain rulesetのrequired status checkへ追加する。

約32〜39分のmerge待ち時間は、Framework Repository自身についてPostgreSQL、複数processおよびpackage済みjourneyを
保護するGateとして受け入れる。一方、本required化はFramework Repository固有のCI Policyであり、Customer Applicationへ
同一の3ラウンドaggregate検証を必須化しない。Customer ApplicationのCIは、利用するStarter、業務リスク、構成および
デプロイ形態に応じて別途軽量化・段階化する。

本承認はrequired context 1件の追加だけを対象とする。workflow、timeout、strict policy、bypass、既存6 checkおよび
PR保護は変更しない。ruleset更新後のEvidence記録、Draft解除、PR mergeおよびmerge後main CIは、§6の順序に従う
後続の個別判断として残す。

## 9. Ruleset更新Evidence

2026年9月10日、承認後の実行順序に従い、ruleset ID `21140116`を更新した。更新直前に現行rulesetを
再取得し、review時点から差分がないことを確認したうえで、次のrequired status check 1件だけを追加した。

```json
{
  "context": "Local Identity Session Audit Integration",
  "integration_id": 15368
}
```

更新後の独立再取得結果は次のとおりである。

| 項目 | 更新後の値 |
|---|---|
| ruleset | `main-merge-protection` / ID `21140116` / `active` |
| 対象 | default branch |
| required status checks | 既存6件を保持し、対象1件を加えた7件 |
| strict policy | `true` |
| bypass actor | なし |
| current user bypass | `never` |
| rule type | deletion、non-fast-forward、pull request、required status checksを維持 |
| GitHub更新時刻 | `2026-09-10T18:29:25.597+09:00` |

PR #29では、final HEAD `f30a34094dce21e5943513b4bbb32c9b84be4a75`の
`Local Identity Session Audit Integration`が`COMPLETED / SUCCESS`として認識され、更新後もPRはDraft、
merge stateは`CLEAN`だった。workflow、timeout、permissions、既存checkおよび他のruleset ruleは変更していない。

これによりGB-5を`COMPLETE / ARCHITECTURE OWNER APPROVED`とする。Draft解除、PR mergeおよびmerge後main CIは
本実行に含めず、引き続き別Architecture Owner判断とする。
