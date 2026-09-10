# Phase 2 P2-B4 B4-5 クローズアウト検証記録

## 1. ステータスと境界

- 検証日: 2026-09-09
- 作業パッケージ: P2-B4 / B4-5
- 検証対象コミット: `1ec1faea8dfc129db210eb1b21d1058bbf2589fc`
- ステータス: `COMPLETE / ARCHITECTURE OWNER APPROVED`
- 所有者: Tooling / Architecture Evidence
- B4-5でのプロダクト実装変更: なし
- 検証対象コミットに含まれるクローズアウトハーネス補正:
  - 残留一時リソース検査へ`koiki-audit-transaction-*`と`koiki-identity-core-*`を追加した。
- 承認済みベースライン:
  - B4-C1〜B4-C8は承認済みである。
  - B4-2、B4-3、B4-4のArchitecture Owner review pointsは承認済みである。

B4-5はP2-B4の実装検証境界をクローズする。Gate Bのポリシーレビュー、CI required checkへの昇格、および後続のmigrationまたは業務機能の作業は、このクローズアウトの対象外とする。

## 2. 検証コマンドと実行順序

次のコマンドを1回実行し、失敗工程だけを個別に再試行することなく、集約検証を3ラウンド連続で完了した。

```powershell
pwsh -NoProfile -File build-support/security-foundation-verification/verify-p2-b4-closeout.ps1
```

各ラウンドでは、同じ6工程を次の順序で実行した。

1. P2-B1 Audit検証
2. P2-B2 Identity検証
3. P2-B3 core検証
4. P2-B3 two-process検証
5. P2-B3 cleanup-process検証
6. P2-B4 package済みReference journey検証

続いてスクリプトは、Root Reactor、Null Safetyのpositive / negative検査、inventory検査、sensitive-output検査、および最終cleanup検査を実行した。

## 3. 3連続実行結果

| ラウンド | B1 Audit | B2 Identity | B3 core | B3 two-process | B3 cleanup | B4 Reference | 合計 |
|---|---:|---:|---:|---:|---:|---:|---:|
| 1 | 00:01:52.9955134 | 00:01:54.9632854 | 00:01:53.5732165 | 00:01:37.9988126 | 00:02:04.1314865 | 00:01:31.6558720 | 00:10:55.3181864 |
| 2 | 00:01:50.8206209 | 00:02:12.7114524 | 00:01:59.7412806 | 00:01:36.5773344 | 00:02:22.1781300 | 00:01:35.1992608 | 00:11:37.2280791 |
| 3 | 00:01:54.5694064 | 00:01:48.4406586 | 00:01:50.9321635 | 00:01:49.9492832 | 00:02:17.2123793 | 00:01:42.6545559 | 00:11:23.7584469 |

最終結果:

```text
Phase 2 P2-B4 closeout succeeded: three consecutive P2-B1-B4 aggregates, Root Reactor, Null Safety, inventory, sensitive-output and cleanup checks. Gate B review may begin after Architecture Owner approval.
```

## 4. 検証できた事項

### 4.1 P2-B1 Audit

- Frameworkの正式staging対象は14/14 projectのまま維持された。
- B1のexpected-suite contractは31/31を維持し、累積fixtureでは72/72 testを完了した。
- Auditのartifact、Public API、dependency、inventory、sensitive-outputの各検査に合格した。
- 分離したFramework release repositoryにReferenceと検証fixtureが含まれていないことを確認した。
- 所有対象の一時リソースとfixture targetがcleanupされたことを確認した。

### 4.2 P2-B2 Identity

- Frameworkの正式staging対象は14/14 projectのまま維持された。
- B2のexpected-suite contractは56/56を維持し、累積fixtureでは72/72 testを完了した。
- IdentityのPublic API、dependency、Data dependency、inventory、sensitive-outputの各検査に合格した。
- 分離したFramework release repositoryにReferenceと検証fixtureが含まれていないことを確認した。
- 所有対象の一時リソースとfixture targetがcleanupされたことを確認した。

### 4.3 P2-B3 Session

- coreの累積T0〜T5 suiteは72/72 testを完了した。
- Sessionのcontract、dependency、inventory、sensitive-outputの各検査に合格した。
- two-process journeyで次を確認した。
  - process A/B間の継続性
  - 通常5件および`DELETE`失敗5件のIdentity mutation
  - logout失敗時の安全性と回復
  - proxy cookieの挙動
  - control sessionの継続性
  - process A停止後のprocess Bの継続性
- cleanup-process journeyで次を確認した。
  - expired-only cleanup
  - exit code 0および10
  - cleanup競合
  - crash recovery

### 4.4 P2-B4 Reference journey

- package済みReference applicationを通じて、AC-P2-01、AC-P2-02、およびGrand Design DoD 2-10に合格した。
- 2つの対象sessionの無効化、管理者sessionの継続性、およびcontrol sessionの継続性に合格した。
- Business Audit失敗時とSession失敗時の双方で、transactional rollbackが維持されることを確認した。
- artifact境界、sensitive-output、およびcleanupの各検査に合格した。

## 5. Root ReactorとNull Safety

- Root Reactorは15/15 projectを完了した。
- Root Surefireの結果を18個のreport fileから独立集計した。
  - test: 104
  - failure: 0
  - error: 0
  - skip: 0
- Null Safetyのpositive検証に合格した。
- negative fixtureは、要求どおり次の診断を出して失敗した。

```text
[NullAway] returning @Nullable expression from method with @NonNull return type
```

- negative fixture実行後の復元に合格した。

14 projectのFramework正式staging単位と15 projectのRoot Reactorは目的が異なる。`koiki-reference-app`はRoot Reactorへ参加する一方、Framework BOMおよび正式release単位の対象外に維持される。

## 6. 実行後の独立した完全性・cleanup確認

closeoutコマンドがexit code 0を返した後、独立したread-only検査で次を確認した。

- `HEAD`は`1ec1faea8dfc129db210eb1b21d1058bbf2589fc`のまま変更されていない。
- worktree entry数は0である。
- 所有対象container数は0である。
- 次の一時リソースpatternは、いずれも残留directory数が0である。
  - `koiki-audit-transaction-*`
  - `koiki-identity-core-*`
  - `koiki-session-core-*`
  - `koiki-session-two-process-*`
  - `koiki-session-cleanup-*`
  - `koiki-reference-b44-*`
- 次のfixture targetは存在しない。
  - `build-support/security-foundation-verification/target`
  - `build-support/security-foundation-verification/session-two-process-fixture/target`
  - `build-support/security-foundation-verification/session-cleanup-process-fixture/target`

## 7. ハーネス補正履歴

この確定実行に先立つ2回の補足確認により、closeout harnessを改善した。

1. 最初の集約検証で、旧来のB1 project数前提と、sensitive-output patternのfalse positiveが判明した。プロダクトの挙動を変更せず、これらのharness問題を補正した。
2. commit前に個別実行して中断したB1検証の一時directoryが残留し、集約残留検査がB3/B4 patternを対象にする一方、B1/B2 patternを対象としていないことが判明した。不足していたpatternを検証対象commitで追加した。

この文書に記録したcloseoutは、補正済みcommitに対する完全な3ラウンド再実行である。プロダクトtestの失敗を抑止または再分類した事実はない。

## 8. P2-B4より後へ明示的に先送りする事項

- Gate B集約policyと最終review
- CI workflowとrequired checkへの昇格
- Public API互換性baselineと`japicmp` policy
- P2-C1 migration集約
- 業務application / 申請承認workflow機能

これらの事項をP2-B4 closeout結果から完了済みと解釈してはならない。

## 9. Architecture Owner review points

1. 同一のcleanなcommitで全6検証工程を3ラウンド連続完了したことを根拠に、P2-B4をcloseしてよいか。
2. B1/B2の個別contract、B3の累積検証、およびpackage済みB4 Reference journeyが、P2-B4境界に十分なregression coverageを提供しているか。
3. 14 projectのFramework正式release単位、個別にpackageするReference application、および15 projectのRoot Reactorが、承認済みのownership境界を引き続き維持しているか。
4. sensitive-output検査、6つの一時リソースpattern検査、fixture target検査、所有対象containerのcleanup、およびclean worktree検証が、closeoutに十分な衛生状態を保証しているか。
5. CI workflowまたはrequired-check policyを先行変更することなく、P2-B4を`COMPLETE / ARCHITECTURE OWNER APPROVED`としてGate B reviewへ引き渡してよいか。

2026年9月9日、Architecture Ownerは上記5点をすべて承認した。

この承認により、P2-B4を`COMPLETE / ARCHITECTURE OWNER APPROVED`とする。次の作業はGate B reviewとし、CI workflow、required-check policy、Public API互換性baselineおよび`japicmp` policyは、Gate Bでの判断前に変更しない。
