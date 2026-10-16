# S1 R4：consume→単発復旧の接続実証（2026-10-16）

状態：**COMPLETE / OWNER ACCEPTED（接続実証の限定結果）**。受入記録は§5。開始根拠は[承認済み限定開始票§7](../../development/phase4-s1-r4-consume-recovery-connection-limited-start-review-20261016.md#7-owner承認記録と網羅性の扱い2026-10-16)。追加受付table案は保留を維持する。

## 1. Source・環境・実行境界

Ownerが承認文書10件をcommitした`c749e44b25690dff52eded5604808fc26f92ad1a`から、cleanな`feature/phase4-s1-r4-consume-recovery-connection`を作成した。元mainのcodeは`af9fa01c56df372559db99e23f495323114f8f93`と同じ。今回のcode/testは未commit差分であり、下記のSHA-256 manifestで検証時sourceを区別する。

fresh preflight開始は2026-10-16 19:19:27 JST、成立・作成phase開始は19:22:47。JDK21.0.12、Wrapper Maven3.9.16、Docker Server29.5.3、既存postgres:17-alpine/Ryuk:0.14.0を確認した。開始前containerなし、空きmemory約19 GiB、disk約43 GiB。既存Java2・Node3 processを保護対象として記録し、操作しない。既存公開DB harnessとcached Modulith APIを読取確認し、新規資材取得はしていない。

新規3 fileだけを作成した。既存code・harness・SQL・POM・依存・migration・Security・CIは変更しない。

- `PermittedSinglePublicationRecoveryService`：未登録のReference Application接続。transaction外で5項目を照合し、既存consumeの正常復帰後に既存recoverを一回呼ぶ。結果Business Auditは別の10秒上限transaction。
- `PermittedSinglePublicationRecoveryTest`：Q01〜Q07の7 method。専用flagなしでは全7件skip。専用実行時はDB起動前にresource flagとheap768 MiBを検査する。
- `PermittedRecoveryConnectionTestSupport`：既存DB harness公開入口と、復旧配下のlease/provider/Modulith代替を組み合わせるtest fixture。

## 2. 検証結果

各集合初回一回、追加再実行0。Maven引数は直接渡し、offline、fork1/reuseなし、JUnit並列無効、Maven/test heap768 MiB、javac初期128/最大768 MiBとした。TEMP/TMPは専用実体directoryへ設定し、終了時に環境変数を復元した。

| 集合 | 開始〜終了 JST | 結果 |
| --- | --- | --- |
| 新規Q01〜Q07 | 19:34:30〜19:35:13 | 7実行、7 PASS、failure/error/skip 0。compileを含む約42秒 |
| 直接回帰4 method | 19:40:47〜19:41:03 | 4実行、4 PASS、failure/error/skip 0。約16秒 |
| 通常root offline clean verify | 19:41:16〜19:45:14 | 16 project成功。404 invocation＝305実行PASS＋99 skip、failure/error 0。約3分58秒 |

新規7件のXMLを保全しcontainer/JVM終了を確認してから直接回帰へ進み、同様に4件のXMLとcleanupを確認してからrootへ進んだ。環境の権限付き実行承認待ちを含む実時刻を保持し、待機時間を旧予算へ付け替えない。

| ID | 成立した観測 |
| --- | --- |
| Q01 | 復旧lease入口でtransaction不在、独立DB接続から消費/consume Audit各1・結果Audit0を確認。入口/模擬再公開各1、完了結果Audit1。再要求で増えない |
| Q02 | publication不一致をconsume前に拒否。消費/consume Audit/結果Audit/復旧入口/模擬再公開すべて0 |
| Q03 | CONSUMERのAudit INSERT一時REVOKEでconsume失敗。消費/consume Auditともrollback、復旧入口0。finallyでGRANT復元・権限確認 |
| Q04 | 二threadの独立要求で、一方は正常復帰、他方はIllegalStateException。消費/consume Audit/結果Audit/入口/模擬再公開各1。threadごとの認証解除とexecutor終了を確認。例外原因が既消費であることまでは識別していない |
| Q05 | 実consume commit後にtest差替えが例外を返すと、消費/consume Audit各1・入口0。旧contextを閉じ、新contextのreadでoperation/消費時刻を確認して再要求しても入口0 |
| Q06 | provider照会代替UNKNOWNにより実recoverがHOLD。入口1・模擬再公開0、保留結果Audit1。再要求で増えない |
| Q07 | 模擬完了待ちの復帰直前にAudit INSERTをREVOKE。消費/consume Auditは各1保持、結果Audit0、入口/模擬再公開各1。再要求で増えず、finallyで権限復元・確認 |

結果Auditは`NOTIFICATION_RECOVERY_CHANGE`、actionは`RECOVERY_COMPLETED`/`RECOVERY_HELD`/`RECOVERY_FAILED`、resourceは`NOTIFICATION_RECOVERY_OPERATION`とoperation UUID。実証した結果分類は完了・HOLDであり、一般的なRuntimeException経路の結果Auditは今回の実行ケースに含めない。操作者はconsumeと同じ認証主体、既存Identityの現在権限検査は既存consumeに委ねた。

直接回帰は`NotificationFoundationTransactionTest`の`commitsConsumptionWithBusinessAudit`、`rollsBackConsumptionWhenAuditFails`、`rejectsConsumptionWithoutCurrentCapability`、`rejectsOutsideScopeWithoutLeakingTarget`だけを選択した。

## 3. 資源・cleanup・端末内証拠

専用DBは既存harnessのhost memory1 GiB/CPU1/max_connections16と、lock/statement/transaction timeout10秒を検査した。各pool最大2/min idle0、contextは同時最大3、独立管理/観測は逐次接続としてclient8以内の構成とした。二要求thread以外のtest並列化はない。既存contextを閉じてから再contextを開いた。専用実行中の全時点の実測peakは取得しておらず、設定上限とtestの有限接続構成を根拠とする。

通常rootでは専用二flag・async profileを付けず、normal-root DB上限flagだけを指定した。新規7件は全skip、既存専用92 skipとの計99 skipをXMLで確認した。通常rootの途中snapshotでは今回所有Java2、PG1、Ryuk2を観測した。

19:45:52にXMLを集計し、19:46:06の最終cleanup確認でcontainer0・今回Java/javac残存0、保護process5件のPID/開始時刻維持を確認した。Q03/Q07の権限復元、各testのcontext/pool終了と二thread終了はtest内で検査済み。raw約6.82 MiB、root module target合計約67.1 MiBで上限内。preflightから最終cleanupまで約26分39秒、各実行・集計・cleanupは承認phase枠内に収まった。検証後のsource hashは実行前と一致し、追加再実行はない。

端末内証拠：`tmp/r4-connection-c749e44-20261016/`（ignored、Git移送対象外）。`preflight.json`、`phases.json`、`source-sha256.json`、`run-phase.ps1`、各集合の`*-args.json`/`*-start.json`/`*-exit.json`/log、`new7-xml/`、`regression4-xml/`、資源snapshotを保全した。XMLは後続root clean前に複写してepochを分けた。今回packageしたJARを使う実行やasync JAR対照は行わない。

通常rootの`root-xml/`/`root-summary.json`/`root-totals.json`、`regression-cleanup.json`/`final-cleanup.json`/`work-size.json`も保全した。検証source SHA-256は以下のとおり。

| 新規file | SHA-256 |
| --- | --- |
| PermittedSinglePublicationRecoveryService.java | `B6988C8C67E27600B6568C0AA8B8467F947B5F592AAC3C31C714C717D699EC06` |
| PermittedSinglePublicationRecoveryTest.java | `8924315D93972E0489252F73C10BB169EFCD5B5B1B28EECDEB2F5C58C541486D` |
| PermittedRecoveryConnectionTestSupport.java | `02376E28D969B2308948076532E5E1B5583340682DB4F70CF7B202844824FE92` |

## 4. 限定結果の意味と残課題

実物は接続処理、既存consume/Identity/Audit/JPA/PostgreSQL、既存recover。lease/provider/Modulith再公開はtest代替であり、模擬完了待ちの正常復帰は実publication完了・実配信の証拠ではない。Q05はcommit後の正常復帰喪失の模擬であり、OS kill・実commit通信断の再現ではない。

同じpermitの再要求を止める接続順と、消費済み・未実行/結果記録未了を自動再実行しない境界を確認した。Bean/runner・正式認証入口には接続しておらず、現行runnerのpermitなし直接起動は残る。FAILED整合、全instance停止真正性/fencing、歴史close、別publicationの重複抑止、証拠の本番保持・実providerは残課題である。

七ケースは初回の有限集合。transaction外要求・五項目照合の残り四項目・一般異常の結果Audit・結果Audit二次失敗などの枝を追加実行していない。必要性と影響をOwnerに提示して都度判断する対象であり、自動でケースを増やさない。

限定結果は§5でOwner受入済み。成果commit・remote操作は別判断。R4完了、DoD4-2/4-3等、正式運用、Phase4全体の完了認定へ拡張しない。

## 5. 限定結果のOwner受入（2026-10-16）

Ownerは本記録のレビューを受け、Q04の記述をテストで直接確認した事実に整えることを前提として、本内容を承認した。§2のQ04を「一方正常復帰・他方IllegalStateException、消費/復旧各一回」に訂正し、条件を反映した。これは観測内容の表現訂正であり、code/test・期待値・既存XML・実行結果の変更や追加実行はない。

受入対象は、今回の限定fixtureでのconsume正常復帰後の一回の接続順、同じpermitの再要求・HOLD・結果Audit失敗からの非再実行、および通常構成保護。新規7件/直接回帰4件PASS、通常root305実行PASS＋99 skip、cleanupの限定結果を`COMPLETE / OWNER ACCEPTED`とする。

§4の適用限界と残課題は維持する。実配信・FAILED完遂・正式認証/運用入口・R4/DoD/Phase4全体の受入ではない。追加受付tableの保留、七ケースを網羅確定としない方針も維持する。次は成果固定対象の差分点検。local成果commitは別承認またはOwner操作、remote・追加実行・後続開始は別判断とする。
