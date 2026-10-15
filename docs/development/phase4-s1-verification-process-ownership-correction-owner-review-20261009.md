# Phase 4 S1 検証process所有数の訂正 Ownerレビュー案

作成日：2026-10-09。状態：`SECTION 2 AND 3 OWNER APPROVED / S1 COMPLETE / B2 VALIDATION STOPPED`（§5〜§8）。

残S1の再開はPersistenceクラスの準備中に停止した。記録した第三processはjcmd.exeで、親は検証前から稼働するVS CodeのCode.exeだった。監視が検証外のIDE診断processまで当該JVM上限2へ数えていたため、恒常上限を変えず、所有数と全体数を分ける訂正を提案する。

入力：[再開承認](phase4-s1-dedicated-jvm-observation-owner-review-20261009.md#7-残集合の再開承認)、[Evidence§18](../architecture/validation/phase4-s1-b2-normal-build-remediation-20261009.md#18-残s1再開とide側jcmdによる監視停止)。原sourceは8b03b9f＋承認14ファイルhash。業務code・test assertion・POM・依存・heap・DB／Ryuk上限を変更しない。

## 1 確認した事実と限界

停止sampleはMaven系PID16108、Surefire fork PID4972、jcmd PID7180。前三者のうち検証commandの子孫はMaven系とSurefireだけで、両者の明示Xmx768mを確認。jcmdは親PID5292に属し、parentの現process名はCode.exe、開始2026-10-08T21:59:05.4173020Z（検証開始より前）だった。jcmdのcommand line全文は保存していないため、対象PID／実行した診断commandはUNKNOWN。前回Migrationの第三JVMもこのIDE processだったとは断定しない。

当該JVM上限2の成立と、環境全体に第三processが存在した事実を区別する。未知processを一律に除外しない。IDE設定変更・IDE／baseline終了・外部jcmdのkillは禁止し、観測と専用runの停止で対処する。

## 2 tmp runnerだけの訂正案

1. preflightで既存Code.exeのPID5292・開始時刻・file名とbaseline Java2件を固定する。PID／開始時刻不一致なら除外しない。
2. Java系processを所有関係付きで保存し、全体数・当該所有数・有限に識別したIDE側jcmd数を別欄へ記録する。command line全文／credentialは保存しない。
3. 除外は名前jcmd.exeかつ直接親が固定Code.exeに一致するものだけ。これを当該検証JVM数へ加算しないが、存在と時刻を記録する。常時1件以内・連続観測10秒以内とし、未知所有・それ以外の外部process・除外条件不成立・長時間残留なら停止する。IDE側jcmdの対象／影響はUNKNOWNを維持する。
4. 検証に属するMaven／forkはS1恒常JVM2、PG1／Ryuk1／接続16を維持する。B2の既承認常駐JVM4と所有jcmdの有限例外は別扱いで維持する。実装sourceと共通helperは変更しない。

## 3 有限再開案と予算

訂正runnerのparser／差分・source hash・環境・残予算確認後、完了XMLがなかったPersistenceクラス12件だけを追加1回、その成功・cleanup後に未実行Registration7／Transaction16／Operational12を各1回とする。既PASSの17件・root・installは繰り返さない。再停止時の自動再実行0。

S1枠の消費は前回232.63秒＋再開準備保守的30秒＋今回command9.32秒／cleanup11.61秒＝約283.56秒、残約196.44秒。訂正準備・管理の保守的20秒を充当し、47件のcommandとclass後cleanupを残約176秒内とする案。実行前に残予算が成立しなければ停止する。総90分と最終cleanup5分予約を維持する。

47件成立後だけ、既承認の未実施Tooling準備3 goal、B2専用36件、classpath負例3件・package照合、最終cleanupへ進む。各枠・回数・flag・artifact／raw上限・所有／未知状態・停止条件は既承認票を引き継ぎ、追加取得や再予算設定を行わない。専用caseのPASS／skipをXMLで判定する。

## 4 Owner判断事項

固定した既存IDEの有限jcmdを当該検証所有数から分けるtmp runner訂正と、上記の未完了47件等の有限再開を採用するか。実行失敗の原因を業務実装不具合と扱わず、process計数の訂正として判断する。本案の訂正・再実行は未実施。結果受入・commit・remote・非同期Reference実装は含めない。

## 5 §2だけのOwner承認

2026-10-09、Ownerは「2 tmp runnerだけの訂正案 を確認し承認いたします」と明示した。承認対象は§2のrunner訂正に限定する。§3の有限再実行をこの判断に含めない。恒常JVM2・既存資源上限・実装sourceを維持してtmp runnerを訂正し、再実行前に点検結果を記録する。

## 6 runner訂正と点検結果

対象は`tmp/b2-normal-build-8b03b9f-20261009-first/Run-Jvm-Observation.ps1`だけ。preflightでCode.exe PID5292／開始時刻とbaseline Java2件のPID／名前／開始時刻を照合し、不一致ならMaven起動前に拒否する。Java識別記録へFixedIdeJcmdを追加し、resource記録をJvmAll／JvmOwned／IdeJcmdに分けた。従来Jvm欄は訂正後の当該所有数を示すため、旧記録の全体数と同じ意味で比較しない。

固定したCode.exeを直接親とするjcmd.exeだけを当該数から除外し、常時1件・連続観測10秒以内を監視する。未知所有・除外条件不成立では停止する。B2所有jcmdの最大5／10秒例外は当該所有processだけに適用し、IDE側と混同しない。cleanup時も同じ識別・有限条件を確認し、IDE／baselineを終了しない。jcmd対象／影響はUNKNOWNとして記録する。環境変数はpreflight成立後のtry内で変更し、finallyで復元する。

PowerShell parser errors0。抽出した判定functionだけを有限入力で評価し、固定IDE一致、親不存在拒否、開始時刻不一致拒否、process名不一致拒否、親PID不一致拒否、2件停止、10秒超停止、上限内許容の8条件がPASS。これはrunnerの判定条件の点検であり、実Maven／Docker／専用testのPASSではない。結果とrunner SHA-256は`evidence/process-ownership-correction-static-checks.json`。実行source14ファイルのhashは不変。

§2訂正は完了、Maven再実行・container起動なし。S1専用17件PASS／残47件、B2等未実行は不変。§3の再開判断は未承認として保持する。予算はリセットせず、訂正準備の保守的20秒を引き継いだ残枠へ計上する案のまま、次の実行前に環境と残予算を再照合する。

## 7 §3の検証再開承認

2026-10-09、Ownerは§3の必要性と§2だけの承認境界を確認後、「では、検証へ進めましょう」と明示した。§3の残S1 47件、成立後の未実施Tooling準備・B2専用36件・前提負例・package照合・cleanupを、訂正済みrunnerと引き継いだ有限予算・回数・上限・停止条件で実行することを承認済みとする。PASS済み17件・root・installは繰り返さない。source・固定IDE／baseline・残予算成立後に開始する。結果受入・commit・remote・非同期Reference実装には拡張しない。

## 8 §3再開の結果

残47件は全PASS、S1専用64件が成立。Tooling準備3 goalとB2登録8件もPASSした。続くB2 process classはcoordinatorの凍結前提でerror1、他11件skipとなり、停止条件に従って終了した。IDE計数の訂正とは別の原因で、接続監視が凍結対象DBへ接続する干渉の可能性を確認した。次の[監視DB訂正レビュー案](phase4-s1-b2-observer-database-correction-owner-review-20261009.md)へ渡し、同じ実行を自動反復しない。[Evidence§20〜21](../architecture/validation/phase4-s1-b2-normal-build-remediation-20261009.md#20-3再開承認後のs1専用64件成立とtooling準備)に集合・資源・元log・cleanupを保存した。source不変、当該JVM／container残留なし、未commit・remote未実施。
