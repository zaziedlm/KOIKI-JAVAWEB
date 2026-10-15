# Phase 4 S1: R1＋R2 merge後のmain限定確認（2026-10-15）

状態：**限定3件 PASS。R3-A／R3-Bの受入、Phase 4全体開始、正式運用保証の判定ではない。**

## Source・artifact・実行範囲

- `main` / `origin/main`：`e31d8f69064f56c00197697ec8c039c5aee3e052`。開始時・終了時のtracked差分なし。
- このsourceから `s1-reference-async` profileでReference JARをoffline `clean package` 1回作成。固定JAR：`tmp/main-e31d8f6-async-check-20261015/main-async-reference-fixed.jar`、SHA-256 `29AB21B76AEDBC6001669B1E1E9B62A9F0514FD60605DF4C7273A8053141DCF2`。V5 migrationとModulith events-jdbc runtimeを確認。
- Tooling offline `test-compile` 1回成功。実動作はTestcontainersの隔離DB／ローカルprovider stubによる次の3メソッドだけ。通常root全体、実provider、R3復旧processは実行していない。

| 確認 | 結果 |
| --- | --- |
| async対応JARをasync無効で起動し、承認を実行 | PASS。承認成立、async schemaとprovider受理は0 |
| async有効で独立した承認2件を実行 | PASS。各承認・通知を対応づけ、provider受理2件とpublication完了を確認 |
| providerが受理前に失敗 | PASS。通知失敗後も承認・Auditのcommitを確認 |

## 起動停止・再実行の区別

最初の選択3件は、Toolingの`AsyncReferenceFixture`がログ置場の決定に必要とする`koiki.reference.async.verification.normal-jar`未指定で、全件がDB起動前に`Path.of(null)`で停止した。この実行は3 ERRORとして`selected-three.log`に保全し、製品挙動のFAILとは扱わない。対象3メソッドは通常JARを実行しないことを確認した上で、同じ固定async JARのパスをログ置場指定にも使用して入力を訂正し、新規1回実行した。訂正後のSurefire XMLは**3 tests / 0 failures / 0 errors / 0 skipped**、BUILD SUCCESS。

証拠は`tmp/main-e31d8f6-async-check-20261015/`の`package.log`、`tooling-test-compile.log`、`selected-three.log`、`selected-three-corrected.log`、`failure-isolation-fixed.xml`、`notification-fixed.xml`、子processの`r1-child-*.log`に保存した。これらのtmp証拠と固定JARはGitでは移送されない。Surefire dumpstream警告はJDK CDS archiveのversion差警告で、テスト結果は成功。終了後`docker ps`は空で、検証用Java子processは残存しなかった。

この限定確認により、R1＋R2がmainへ入った断面で通常構成保護・正常な非同期通知・provider失敗分離の代表経路を確認した。R3-A／R3-Bは第5段階統合の対象であり、この結果でその実装や未達条件を受入済みに変更しない。
