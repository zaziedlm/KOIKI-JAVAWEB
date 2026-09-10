# Phase 2 Gate B GB-3 ローカル最終検証

## 1. ステータスと境界

- 検証日: 2026年9月10日
- 承認日: 2026年9月10日
- 作業パッケージ: Phase 2 / Gate B / GB-3
- 検証対象コミット: `e04852984ce2746e3ef4edd43769f730f40a4c04`
- ステータス: `COMPLETE / ARCHITECTURE OWNER APPROVED — GB-4 READY`
- 所有者: Tooling / CI Policy / Architecture Evidence
- Framework / Reference product実装変更: なし
- remote push / PR、remote CI実行、required check変更: 未実施

本検証は、GB-2で承認した`Local Identity Session Audit Integration`候補と同じaggregate正本を、
commit済みのcleanな同一HEADでローカル実行するGB-3境界である。Linux fresh runnerでの3回連続成功はGB-4へ残す。

## 2. 事前条件

検証開始前に次を確認した。

- `HEAD`は検証対象コミットと一致した。
- worktree entry数は0だった。
- Rancher DesktopのDocker Server `29.5.3`へ接続できた。
- 次のinspection-only commandが成功し、所有container、Java process、一時directoryおよびfixture targetの残留は0だった。

```powershell
pwsh -NoProfile -File build-support/security-foundation-verification/verify-p2-b4-closeout.ps1 `
  -InspectOnly -ExpectedHead e04852984ce2746e3ef4edd43769f730f40a4c04
```

## 3. 実行コマンド

途中工程を個別再実行せず、次の単一aggregateを実行した。

```powershell
pwsh -NoProfile -File build-support/security-foundation-verification/verify-p2-b4-closeout.ps1 `
  -ExpectedHead e04852984ce2746e3ef4edd43769f730f40a4c04
```

aggregate終了後、§2と同じinspection-only commandを独立して再実行した。

## 4. 3ラウンド連続結果

| ラウンド | B1 Audit | B2 Identity | B3 core | B3 two-process | B3 cleanup | B4 Reference | 合計 |
|---|---:|---:|---:|---:|---:|---:|---:|
| 1 | 00:01:46.2650126 | 00:01:47.8641317 | 00:01:46.7853397 | 00:01:41.2876442 | 00:01:58.2078242 | 00:01:38.1016259 | 00:10:38.5115783 |
| 2 | 00:01:44.9496449 | 00:01:46.3711210 | 00:01:43.6730235 | 00:01:48.8490530 | 00:02:09.5336916 | 00:01:40.7587675 | 00:10:54.1353015 |
| 3 | 00:01:50.0579119 | 00:01:45.4501439 | 00:01:48.1008857 | 00:01:46.1227850 | 00:02:17.0888596 | 00:01:40.2139405 | 00:11:07.0345266 |

6工程3ラウンドの合計は`00:32:39.6814064`だった。各ラウンドで次が成功した。

- B1 expected-suite contract 31 / 31
- B2 expected-suite contract 56 / 56
- B3 cumulative T0〜T5 72 / 72
- B3 two-process continuity、Identity mutation、logout failure / recovery
- B3 expired-only cleanup、exit 0 / 10、競合、crash recovery
- B4 AC-P2-01 / 02、DoD 2-10、rollback、artifact boundary、sensitive-output、cleanup
- Framework正式release unit 14 / 14 project
- ラウンド間のHEAD / clean worktree / residual-resource検査

## 5. 最終回帰と完全性

3ラウンド後のRoot Reactorは15 / 15 projectで成功した。18個のSurefire reportを独立集計した結果は次のとおりである。

| test | failure | error | skipped |
|---:|---:|---:|---:|
| 104 | 0 | 0 | 0 |

Null Safetyはpositive、expected negativeおよびrestoreの全経路で成功した。aggregate内の最終inspectionと、
終了後に独立実行したinspection-onlyの双方で次を確認した。

- `HEAD`は検証対象コミットのまま不変
- clean worktree
- P2-B所有container prefix 4件の残留0
- 所有Java process marker 3件の残留0
- 一時directory pattern 6件の残留0
- 非配布fixture target 3件の残留0

最終aggregateはexit code 0で、次の成功messageを返した。

```text
Phase 2 P2-B4 closeout succeeded: three consecutive P2-B1-B4 aggregates, Root Reactor, Null Safety, inventory, sensitive-output and cleanup checks. Gate B review may begin after Architecture Owner approval.
```

## 6. GB-4へ残す事項

- Architecture Ownerによる本GB-3結果の承認
- remote push / PRの個別承認
- 同一final HEADを使ったLinux fresh runner上の候補job 3回連続成功
- remote実行時間、権限、cleanupおよび既存check回帰の記録
- required check化の別Architecture Owner判断
- ruleset変更、mergeおよびmerge後main CI

## 7. Architecture Owner review points

1. CI候補を含むcleanな同一HEADで、B1〜B4の6工程を3ラウンド連続完了したことをGB-3 local successとしてよいか。
2. 各ラウンドの14 / 14 formal staging、31 / 31、56 / 56、72 / 72および3つのprocess journeyが、local aggregateの再現性を十分に示すか。
3. Root Reactor 15 / 15、104 tests、Null Safety正負 / restoreが、最終regressionとして十分か。
4. aggregate内と終了後独立inspectionの双方で、同一HEAD、clean worktree、container / process / directory / fixture残留0を確認した境界がGB-C5を満たすか。
5. GB-3ではlocal結果だけを承認対象とし、remote push / PR、Linux上の3連続successおよびrequired化判断をGB-4以降へ残す境界が妥当か。

推奨結論は上記1〜5を承認し、GB-3を`COMPLETE / ARCHITECTURE OWNER APPROVED`として記録した後、
remote操作の個別承認を得てGB-4へ進むことである。

## 8. Architecture Owner承認

2026年9月10日、Architecture Ownerは上記1〜5とレビュー支援内容を確認し、すべて承認した。これによりGB-3を
`COMPLETE / ARCHITECTURE OWNER APPROVED`とする。次はGB-4 remote PR Evidenceとrequired化reviewである。
remote push / PR、remote CI実行およびrequired check変更は本承認に含めず、定めた順序で個別判断する。
