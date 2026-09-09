# Phase 2 Gate B GB-2 CI候補実装検証

## 1. ステータスと境界

- 実装日: 2026年9月10日
- 承認日: 2026年9月10日
- work package: Phase 2 / Gate B / GB-2
- baseline commit: `9c73891`（Gate B契約GB-C1〜C7承認）
- status: `COMPLETE / ARCHITECTURE OWNER APPROVED — GB-3 READY`
- 所有者: Tooling / CI Policy / Architecture Evidence
- Framework / Reference product実装変更: なし
- remote実行、push、PR、required check変更: 未実施

本sliceは、承認済みGB-C1〜C7に従ってMilestone BのCI候補を実装する。clean HEADでのfull aggregateはGB-3、
fresh Linux runnerの3回連続成功はGB-4へ残す。

## 2. 実装内容

### 2.1 独立CI job

`.github/workflows/ci.yml`へ`Local Identity Session Audit Integration` jobを追加した。

| 項目 | 値 |
|---|---|
| runner | `ubuntu-24.04` |
| Java | Temurin 21 |
| job permissions | `contents: read` |
| job timeout | 60分 |
| aggregate step timeout | 52分 |
| final inspection timeout | 5分 |
| Maven cache | なし |
| secret / Packages / environment権限 | 追加なし |

既存Phase 1b `Milestone B Integration`、通常`Verify`および`Security Foundation Integration`へ処理を混在させない。

### 2.2 aggregate正本

CI jobは既存`verify-p2-b4-closeout.ps1`を直接実行する。Gate B専用の重複aggregateは作成していない。
この入口はB1 Audit、B2 Identity、B3 core / two-process / cleanup、B4 package済みReference journeyを3ラウンド連続実行し、
最後にRoot ReactorとNull Safetyを実行する。

### 2.3 最終inspection

`verify-p2-b4-closeout.ps1`へ次を追加した。

- `-InspectOnly`: aggregateを再実行せず、repositoryと所有resourceだけを検査する。
- `-ExpectedHead`: CI checkout対象SHAと実際の`HEAD`を比較する。
- 通常実行の途中失敗後にも最終inspectionを試行する。
- 一次失敗と最終inspection失敗が同時に発生した場合、双方のmessageを保持する。
- repository検査が失敗してもresource検査を継続し、カテゴリ別失敗を集約する。

CIではaggregate stepより短いtimeoutを設定し、その後の`if: always()` stepから`-InspectOnly`を呼ぶ。

### 2.4 所有resource検査

最終inspectionは次を確認する。

- expected HEADとclean worktree
- P2-B所有container prefix 4件
- 外部起動Java processの一時path marker 3件
- Audit / Identity / Session / Reference一時directory pattern 6件
- 非配布fixture target 3件

Java process検査は対象をJava executableへ限定し、WindowsではCIM、CIのLinuxでは`/proc`を使用する。検出時はPIDとmarkerだけを
報告し、command line本体を出力しない。repository内JARから起動するB4 Reference processには、Harnessが生成した非機密の
`koiki.verification.process-marker` JVM引数を付与し、product codeから利用しない。対応外OSまたはprocess情報へのアクセス拒否時は、
process検査を黙って省略せず失敗する。

## 3. 静的・negative path検証結果

| 検証 | 結果 |
|---|---|
| PowerShell AST parse | success、syntax error 0 |
| GitHub Actions YAML parse | SnakeYAML 2.6でsuccess、追加job keyを確認 |
| 一次失敗保持 | 不正`ExpectedHead`でprimary failureを保持 |
| 最終inspection併記 | primary failureとfinal integrity failureの双方を確認 |
| resourceカテゴリ継続 | Docker inspection failureを4 prefix分集約し、最初の失敗だけで終了しないことを確認 |
| Windows Java process検査 | marker付き実processを検出し、診断にはPIDだけを記録してcommand lineを露出しないことを確認 |
| `git diff --check` | success |
| workflow以外のproduct変更 | 0 |

YAML検証用の一時Java sourceは検証後に削除し、成果物へ含めていない。local implementation worktreeは意図的にdirtyであり、
Docker inspectionも実行時にnonzeroとなったため、`-InspectOnly`のpositive resultおよびfull closeout成功は本sliceの証拠として主張しない。

## 4. GB-3へ残す検証

1. 本実装をcommitしてclean HEADを確定する。
2. Docker / Rancher Desktopの利用可能性と事前残留0を確認する。
3. `verify-p2-b4-closeout.ps1`を通常実行し、3ラウンドとRoot / Null Safetyを完了する。
4. 同じHEADを指定した`-InspectOnly`がpositiveで完了することを確認する。
5. workflow候補を含むHEAD / worktree不変、process / container / temporary resource残留0を記録する。

## 5. 明示的に未実施の事項

- remote push / PR
- GitHub Actions上での候補job実行
- remote 3回連続success
- main ruleset / required check変更
- PR merge / merge後main CI
- P2-C1 migration集約

## 6. Architecture Owner review points

1. 既存B4-5 closeoutをlocal / CI共通正本として直接再利用し、Gate B専用aggregateを作らない実装がGB-C1を満たすか。
2. 独立job名、Ubuntu / Java 21、`contents: read`、60分job / 52分aggregate / 5分inspectionの境界がGB-C2 / C3を満たすか。
3. `if: always()`と`-InspectOnly -ExpectedHead`により、aggregate失敗後もrepository / resource検査を独立観測する構成が妥当か。
4. 一次失敗を保持しつつカテゴリ別cleanup failureを集約し、process command lineを露出しない実装がGB-C5を満たすか。
5. static / negative pathまでをGB-2の証拠とし、clean HEADでのpositive full aggregateをGB-3、remote / required化をGB-4以降へ残す境界が妥当か。

推奨結論は上記1〜5を承認し、GB-2を`COMPLETE / ARCHITECTURE OWNER APPROVED`としてcommitした後、GB-3 local final verificationへ進むことである。

## 7. Architecture Owner承認

2026年9月10日、Architecture Ownerは上記1〜5の整理内容を確認し、すべて承認した。レビュー中に補正した
Windows CIM / Linux `/proc`によるJava process検査、B4 Reference processへの非機密marker付与およびPIDだけを
報告する非露出境界も承認対象に含む。これによりGB-2を`COMPLETE / ARCHITECTURE OWNER APPROVED`とし、次をGB-3
local final verificationとする。remote push / PR、remote CI実行およびrequired check変更は本承認に含めない。
