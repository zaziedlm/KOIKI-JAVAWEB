# Phase 4 S1 B2 接続数監視DBの訂正 Ownerレビュー案

作成日：2026-10-09。状態：`SECTION 2 AND 3 OWNER APPROVED / COMPLETE / OWNER ACCEPTED`（§5〜§9）。

通常rootと専用S1 64件・Tooling準備は成立した。B2登録8件もPASSしたが、process-backed最初のclassでcoordinatorの凍結検査が`source connection remains`となった。tmp runnerの資源観測が同じ凍結対象DBへ接続しており、検証対象へ影響し得ることを確認した。接続監視先だけを別DBへ訂正し、凍結・認可・接続上限のassertionを維持する案とする。

入力：[Evidence§21](../architecture/validation/phase4-s1-b2-normal-build-remediation-20261009.md#21-b2登録8件passと凍結前提エラーによる停止)、[所有数訂正と再開承認](phase4-s1-verification-process-ownership-correction-owner-review-20261009.md#7-3の検証再開承認)。sourceは8b03b9f＋承認14ファイルhash、専用m2／今回生成classpath・artifact epochを維持する。

## 1 事実と原因の確度

B2ProtectedIssueReadTestはI11_providerAcceptedRejectsの新しいcoordinator準備でerror1、他11件はfail-fast skip。最初のcoordinatorはreadyまで成立した。次coordinatorの保持logでB2FrozenSourceProtocol.freezeの`source connection remains`を確認した。Reference側ではconstructorのclose例外が先行し`Coordinator cleanup failed; finite log retained`となっていた。

freezeは`pg_stat_activity`のcurrent_databaseに自分以外の接続がないことを検査する。fixtureの既定DBとrunnerの`docker exec ... psql -U test -d test`が同じで、runnerは短い観測接続を反復していた。凍結検査への観測干渉は可能だが、当該瞬間の接続PID／application_nameを保存していないため、残留接続がrunnerだったと断定しない。既存coordinator／protocol／writer遮断／assertionを緩めない。

## 2 tmp runnerだけの訂正案

接続数監視のpsql接続先を同じ使い捨てclusterの既存`postgres` DBへ変更する。PGAPPNAMEを固定`koiki-b2-budget-observer`としてdocker execへ有限供給し、凍結対象のtest DBへ観測接続を作らない。SQLはcluster内client backend総数のままとし、観測接続自身も含めて既存接続上限8と照合する。観測不能は別記し、接続0の証拠にしない。

新しいDB・role／grant・migration・依存・profileを作らない。Reference／Tooling source、B1／B2 helper／protocol・assertion、schema・通常構成は変更しない。IDE所有数訂正、常駐JVM4／所有jcmd最大5・10秒、PG1／接続8／B2 Ryuk最大3、cleanup30秒と未知所有の停止条件を維持する。観測先変更を資源上限の引上げにしない。

## 3 有限再実行案と残予算

runner parser・限定差分・source／classpath／prepared artifact hash、baseline／IDE・container0・残予算確認後、B2ProtectedIssueReadTest12件だけ追加1回。今回I11はfixture準備errorで業務assertion結果未成立、他11件skipのため、class全体の12件を新rawで検証する。成功・cleanup成立後だけ未実行Boundary8／Transaction8へ各1回進む。root／install／S1 64件／Tooling準備／登録8件は繰り返さない。自動再実行0。

B2 12分枠は登録command5.97＋cleanup0.29、今回I12 command31.27＋cleanup11.60＝約49.12秒とstage管理分を引き継ぐ。開始前に未計測管理を保守的に加え、訂正準備30秒を計上して残予算を再計算する。予算を再計算できない、再失敗、上限超、cleanup失敗は停止し、追加原因修正・反復へ進まない。総90分と最終cleanup5分予約は維持する。

28件成立後だけ、既承認のclasspath未指定／不存在／空fileの負例3件、package／source／artifact整合・最終cleanupへ進む。負例は期待拒否として専用100 PASSに加算しない。Reference JARの意図したmain5型包含・fixture非混入等を記録する。結果受入・commit・remote・非同期Reference実装は別判断。

## 4 Owner判断事項

観測先DBだけを変える§2のtmp runner訂正と、既存残予算内の§3追加I12一回・未実施B8／X8等への条件付き続行を採用するか。本案作成時点で訂正・再実行は行っていない。今回の元log／XML／prepared artifactは保持する。

## 5 §2のOwner承認

2026-10-09、Ownerは本案を確認し、「2 tmp runnerだけの訂正案 を承認します。この対処で進めましょう」と明示した。今回明示された承認対象は§2のtmp runner訂正とする。§3の追加再実行・条件付き続行はこの判断に自動で含めず、点検結果と残予算を次の実行判断へ渡す。

## 6 訂正と点検の結果

tmp内`Run-Jvm-Observation.ps1`のpsql接続先をtestからpostgresへ変更し、docker execのprocess環境へPGAPPNAME=koiki-b2-budget-observerを供給した。SQLはclient backendのcluster総数のまま、観測接続自身も含む。sampleへObserverDatabase／ObserverApplication／ConnectionCountIncludesObserverを追加した。既存資源値、未知所有／観測不能の扱い、IDE側jcmd条件、cleanup条件は維持する。

PowerShell parser errors0。接続先・application名、旧test DB監視commandがないことを点検し、承認14 source hashと準備済みartifact hash全件の不変を照合した。結果とrunner SHA-256は`tmp/b2-normal-build-8b03b9f-20261009-first/evidence/b2-observer-database-correction-static-checks.json`。この点検は実接続・凍結検査の成立や専用testのPASSではない。Maven／Dockerは未実行。

§2の訂正は完了。通常root257実行PASS／92予定無効、S1専用64 PASS、B2登録8 PASSは保持し、B2 process28件・負例・package最終照合は未完了のまま。§3再開時は元rawを保持し、残予算・固定source／classpath／prepared artifact・IDE／baselineとcontainer0を再照合する。再予算設定・実装修正・commit／remoteは行わない。

## 7 §3のOwner承認

2026-10-09、Ownerは§3「有限再実行案と残予算」を明示承認し、「実行へ進めましょう」と指示した。訂正runnerでI12追加1回、成立後だけ未実施B8／X8・負例3件・package整合・最終cleanupへ進む。source／artifact・環境・累積残予算を確認し、既存回数・上限・停止条件を維持する。PASS済みroot／install／S1／準備／登録8件を繰り返さず、結果受入・commit・remote・非同期実装へ拡張しない。

## 8 承認後の実行完了

I12追加1回と未実施B8／X8は全28 PASS／skip0、class後cleanup成立。B2再開410.34秒で残610秒枠内。既登録8件を合わせB2専用36件、S1専用64件と合わせ100 PASS。前提負例3件は子／DB／Ryuk未起動のまま期待拒否、package・source／artifact整合・最終cleanupも成立した。root257 PASS／予定無効92件との対応は登録重複8を除く349種類で一致した。

旧エラー原因の確定、全時刻の資源・session完全監視、IDE側jcmdの対象／影響は保証しない。限界・時刻・上限・予算・hash・証拠所在を[Evidence最終結果§23](../architecture/validation/phase4-s1-b2-normal-build-remediation-20261009.md#23-3承認後のb2成立負例package最終結果)へ集約した。source不変、当該Java／container残留なし、rawと旧artifact保持。結果受入・commit／remote・後続非同期実装は別判断のまま。

## 9 最終結果の受入

2026-10-09、OwnerはEvidence§23を確認し結果を明示承認した。本票の有限訂正・検証結果は`COMPLETE / OWNER ACCEPTED`。[受入記録§24](../architecture/validation/phase4-s1-b2-normal-build-remediation-20261009.md#24-最終結果のowner受入承認)にsource／artifact epoch・承認範囲を記録した。commit／remote・再検証・後続非同期実装等は別判断のまま維持する。
