# Phase 2 Gate B GB-4 remote PR検証記録

## 1. 実行境界

- 実行日: 2026年9月10日
- 作業パッケージ: Phase 2 / Gate B / GB-4
- 初回remote対象コミット: `79f344e5cbafbbce4b9d654ad1f0556bc3abb624`
- Draft PR: [#29 Phase 2 Gate B: add security aggregate CI candidate](https://github.com/zaziedlm/KOIKI-JAVAWEB/pull/29)
- 初回CI run: [34421786829](https://github.com/zaziedlm/KOIKI-JAVAWEB/actions/runs/34421786829)
- ステータス: `IN PROGRESS — REMOTE CORRECTION REQUIRED`
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

補正はCP10のSQL検査対象を、同検証器が承認済み成果物として列挙するPhase 1bの10 projectへ限定する。
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

## 5. final HEAD確定前に残す事項

- 本補正と記録をcommitする。
- commit済みclean HEADでB4-5 local closeoutを1回成功させる。
- 同じHEADをDraft PRへ1回pushする。
- PR synchronizeで起動した通常CIの全check回帰と候補job成功を確認する。
- 同じworkflow runの候補jobだけを2回rerunし、候補jobのremote 3回連続成功を完成させる。
- 各成功回のrun / attempt、commit SHA、実行時間、最終inspectionおよびcleanupを本記録へ追記する。
- remote evidenceをArchitecture Ownerがreviewした後、required check化を別途判断する。

