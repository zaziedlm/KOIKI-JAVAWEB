# Phase 4 S1: R3統合後mainの通常・非同期限定検証（2026-10-15）

状態：**通常root PASS、非同期選択8件 PASS。R3-B既知FAILと未達範囲は維持する。**

## Source・artifact・実行条件

- source：`main` / `origin/main`のmerge commit `450b6009a85024543d5e406e9a8ac9fa7aa3c297`。開始前と終了後のtracked差分なし。
- JDK 21、Maven Wrapper 3.9.16、Docker Server 29.1.3。開始前の稼働containerは0、空きmemory 11,027,804 KiB、C空き523,615,338,496 bytes。Maven／fork heap 768 MiB、fork 1、JUnit並列なし、専用JVM temp、offline実行を用いた。
- 通常構成のroot `clean package`を1回実施し、生成JARを`tmp/main-450b600-async-verification-20261015/main-normal-reference-fixed.jar`に固定。SHA-256 `8930711577D51E5D37D45A9747EA461AF95507D66B634B51C45020F8816944DD`。
- 同じsourceから`s1-reference-async` profileのJARをtest skipのoffline `clean package` 1回で作成し、`main-async-reference-fixed.jar`に固定。SHA-256 `2D91A38E0FC982BAAA6D14F13E03D9CBCA93469845396A09858E9F46DBBFCC45`。通常JARにModulith events-jdbc runtimeはなく、async JARにはある。Tooling offline `test-compile`は1回成功。

## 実行結果

| 対象 | 結果 |
| --- | --- |
| 通常root reactor | **BUILD SUCCESS**。fresh Surefire XML 62 file、397件＝305実行＋専用92 skip、failure 0／error 0。通常構成保護の回帰として扱う |
| async無効JARでの承認、独立承認2件の非同期通知完了 | 選択2件 PASS |
| provider受理前失敗と承認・Auditの保持 | 選択1件 PASS |
| R3-A：A01送信前停止・単発復旧、A03受理済みreceiptの再利用 | 選択2件 PASS。A01のgateで`PUBLISHED`／attempt 1、provider到着・受理0、advisory lock待機を観測 |
| R3-B：B01耐久受理観測から無追加POSTで復旧、B03aの別method人手HOLD、B03b同key安全再送 | 選択3件 PASS。HOLD JSONを保存 |

非同期選択は4クラス8メソッドを固定通常／async JARと隔離Testcontainers DB・ローカルprovider stubで1 Maven invocation実行した。Surefire XMLは**8件、failure 0／error 0／skip 0**。R3-A／Bの出力したartifact SHA-256は開始前の固定async JARと一致した。実行後のDocker稼働containerは0、検証用Java子processは残存せず、検証終了時のtracked差分はなかった。Surefire dumpstreamの警告はJDK CDS archiveのversion差であり、テスト成否は上記XMLとBUILD SUCCESSで確認した。

証拠は`tmp/main-450b600-async-verification-20261015/`の`normal-root.log`、`root-reports/`、`async-package.log`、`tooling-test-compile.log`、`selected-eight.log`、選択4クラスの`TEST-*.xml`、子process log、HOLD JSON、固定JARに保全した。**tmpの現物はGitでは移送されない。**

## 判定境界

この確認はmain統合断面の通常構成と代表的な非同期連携の実動作に限る。R3-Bの元B03a「受理後応答喪失から自動完了」methodは既知FAILとして選択せず、別methodの人手HOLD PASSで置き換えたとは判定しない。E08の残枝、DoD 4-2／4-3、実provider配信、広い並行運用保証、R4／R5、Phase 4全体受入も未検証・未達のままである。
