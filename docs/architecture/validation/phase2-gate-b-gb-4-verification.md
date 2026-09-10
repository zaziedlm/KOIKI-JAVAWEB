# Phase 2 Gate B GB-4 remote PR検証記録

## 1. 実行境界

- 実行日: 2026年9月10日
- 作業パッケージ: Phase 2 / Gate B / GB-4
- 初回remote対象コミット: `79f344e5cbafbbce4b9d654ad1f0556bc3abb624`
- 補正後final HEAD: `f30a34094dce21e5943513b4bbb32c9b84be4a75`
- Draft PR: [#29 Phase 2 Gate B: add security aggregate CI candidate](https://github.com/zaziedlm/KOIKI-JAVAWEB/pull/29)
- 初回CI run: [34421786829](https://github.com/zaziedlm/KOIKI-JAVAWEB/actions/runs/34421786829)
- 補正後CI run: [34428043345](https://github.com/zaziedlm/KOIKI-JAVAWEB/actions/runs/34428043345)
- ステータス: `COMPLETE / ARCHITECTURE OWNER APPROVED — REQUIRED CHECK REVIEW READY`
- 所有者: Tooling / CI Policy / Architecture Evidence
- Framework / Reference product実装変更: なし
- required check / ruleset変更、merge: 未実施

本記録は、Architecture Ownerが個別承認したremote push / Draft PRと、Linux fresh runner上での
`Local Identity Session Audit Integration`候補jobを検証するGB-4境界である。初回runで検出した
検証器のplatform / phase境界を補正した後、補正済みの同一final HEADでremote 3回連続成功を確認する。

## 2. CI負荷を抑える実行方針

1. 補正は初回runで判明した検証器2件へ集約し、失敗済みcommitのjobは再実行しない。
2. 補正後は対象Harnessを局所検証し、commit済みclean HEADでB4-5 local closeoutを1回実行する。
3. final HEADを1回pushし、PR synchronizeで起動する通常CIをremote成功1回目とする。
4. 通常CIで既存check回帰を確認した後、候補jobだけを同じHEADで2回rerunする。
5. workflow全体を3回起動せず、候補jobの3回連続成功、各回の実行時間およびcleanupを記録する。
6. required check化はremote evidence完成後の別Architecture Owner判断とし、本GB-4実行ではrulesetを変更しない。

この方針は、同一final HEADの候補jobを3回連続成功させるGB-C3を維持しつつ、既存jobの重複実行を避ける。

## 3. 初回remote結果

### 3.1 成功したcheck

| Check | 結果 | 実行時間 |
|---|---|---:|
| `Verify (ubuntu-24.04)` | SUCCESS | 5分29秒 |
| `Security Foundation Integration` | SUCCESS | 4分21秒 |
| `Milestone B Integration` | SUCCESS | 3分20秒 |
| `Public API Compatibility` | SUCCESS | 1分22秒 |
| `Build Runtime Fixture (Java 21)` | SUCCESS | 52秒 |
| `Java Runtime Compatibility` | SUCCESS | 18秒 |

### 3.2 候補jobの失敗

`Local Identity Session Audit Integration`は4分54秒で失敗した。Session JDBC JAR内の公開classは
承認済み3型と一致していたが、検証器がJAR entryの列挙順を契約順として比較していたため、Linux上の順序差を
契約差分として誤検知した。aggregate失敗後も`if: always()`の最終inspectionは成功し、失敗経路でも
repository / owned resource cleanupを独立観測できた。

補正は実際の公開class名をsortしてから、sort済みの承認契約と比較する。型集合、型数およびmigration検査は変更しない。

### 3.3 既存Milestone Cの失敗

`Milestone C Closeout`は6分21秒で失敗した。Maven buildとtestは成功していたが、Phase 1b CP10検証器が
`koiki-starters`全体のproduction SQLを0件と仮定し、Phase 2で正式追加済みのIdentity / Session migration 2件を
Phase 1b契約違反として誤検知した。

補正はCP10のSQL検査対象を、同検証器が承認済み成果物として列挙するPhase 1bの10-project release unitのうち、
production sourceを所有し得る9 project rootへ限定する。残る1 projectはsourceを所有しないRoot Reactorであり、
repository rootを再帰走査して後続Phaseのprojectを混入させない。
Phase 2 migrationをCP10の許可リストへ取り込まず、Identity / Session migrationの検査と全体集約をそれぞれ
P2-B2 / B3の正本およびP2-C1に残す。

## 4. 補正後のlocal検証

| 検証 | 結果 |
|---|---|
| 変更した2つのPowerShell scriptのAST parse | SUCCESS |
| Phase 1b SQL検査対象のproduction SQL件数 | SUCCESS — 0件 |
| `verify-p2-b3-session-core.ps1` | SUCCESS — formal staging 14 / 14、cumulative fixture 72 / 72 |
| `verify-cp10-closeout.ps1` | SUCCESS — 10分14秒、CP8回帰、package済みDeveloper Journey、CP9 smoke、cleanup |
| `git diff --check` | SUCCESS |

最初のCP10 local実行はsandboxからDocker named pipeへ接続できず停止したため、検証結果には算入しない。
Dockerアクセス可能な通常実行で全工程を再実行し、成功を確認した。

commit済みclean final HEADでは、B4-5 local closeoutの6工程を3ラウンド実行した。各ラウンドのB1 31 / 31、
B2 56 / 56、B3 72 / 72、two-process、non-web cleanupおよびpackage済みReference journeyがすべて成功した。
続くRoot Reactor 15 / 15、104 tests、Null Safety正負 / restore、最終repository / residual-resource inspectionも成功した。

## 5. 補正後remote結果

### 5.1 既存check回帰

final HEADの通常CIでは、候補job以外も次のとおり成功した。Milestone Cのremote成功により、§3.3の
Phase 1b検証範囲補正もLinux fresh runner上で確認できた。

| Check | 結果 | 実行時間 |
|---|---|---:|
| `Verify (ubuntu-24.04)` | SUCCESS | 4分50秒 |
| `Security Foundation Integration` | SUCCESS | 3分59秒 |
| `Milestone B Integration` | SUCCESS | 3分16秒 |
| `Milestone C Closeout` | SUCCESS | 8分46秒 |
| `Public API Compatibility` | SUCCESS | 1分41秒 |
| `Build Runtime Fixture (Java 21)` | SUCCESS | 52秒 |
| `Java Runtime Compatibility` | SUCCESS | 18秒 |

### 5.2 候補jobの3回連続成功

通常CIでの第1回成功後、workflow全体ではなく`Local Identity Session Audit Integration`だけを2回rerunした。
全回が同一run、同一final HEADを使用し、aggregateと`if: always()`の最終inspectionの双方に成功した。

| 回 | run attempt / job | 結果 | 実行時間 | 最終inspection |
|---:|---|---|---:|---|
| 1 | `34428043345` attempt 1 / `102717361523` | SUCCESS | 39分11秒 | SUCCESS |
| 2 | `34428043345` attempt 2 / `102725454040` | SUCCESS | 32分20秒 | SUCCESS |
| 3 | `34428043345` attempt 3 / `102735748326` | SUCCESS | 34分31秒 | SUCCESS |

job timeout 60分、aggregate step timeout 52分、最終inspection 5分の範囲内で安定して完了した。
最大実測39分11秒に対してaggregate timeoutには約13分、job timeoutには約21分の余裕がある。
現時点では開発途上の診断余地を保持するためtimeoutを短縮しない。

## 6. GB-4承認後に残す事項

- 本記録とPhase 2実行計画をcommitする。
- required check化の可否を次の別Architecture Owner判断として記録する。
- required化を承認する場合だけmain rulesetを変更する。
- PR mergeとmerge後main CIはrequired化判断後の境界に残す。

## 7. Architecture Owner review points

1. 初回remoteで検出したJAR entry順序依存を、公開型の集合・数を変えずsort比較へ補正した判断が妥当か。
2. Phase 1b CP10のSQL検査を、10-project release unitのうちproduction sourceを所有し得る9 project rootへ限定し、Phase 2 migrationの全体集約をP2-C1へ残した境界が妥当か。
3. final HEADでlocal B4-5 closeout、既存remote check回帰およびMilestone C remote成功を確認した証拠が補正の受入に十分か。
4. 候補jobだけをrerunして同一final HEADで39分11秒、32分20秒、34分31秒の3回連続成功を得た方法がGB-C3を満たすか。
5. aggregateと最終inspectionが全3回成功し、60分 / 52分 / 5分のtimeoutを維持したうえで、required化を別判断へ残す結論が妥当か。

推奨結論は上記1〜5を承認し、GB-4 remote verificationを`COMPLETE`とした後、
`Local Identity Session Audit Integration`のrequired check化を別Architecture Owner判断へ進めることである。

## 8. Architecture Owner承認

2026年9月10日、Architecture Ownerは上記1〜5とレビュー支援内容を確認し、すべて承認した。
項目2は、CP10のSQL検査対象を「Phase 1bの10-project release unitのうちproduction sourceを所有し得る
9 project root」と正確化した。本補正は検査対象の説明精度を上げるもので、実装、検証結果または
P2-C1へ残すmigration集約境界を変更しない。

これによりGB-4を`COMPLETE / ARCHITECTURE OWNER APPROVED`とする。次は
`Local Identity Session Audit Integration`のrequired check化reviewである。required check / main ruleset変更、
PR mergeおよびmerge後main CIは本承認に含めず、引き続き個別判断とする。
