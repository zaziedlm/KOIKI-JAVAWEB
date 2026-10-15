# Phase 4 S1 専用検証 第三JVMの有限観測 Ownerレビュー案

作成日：2026-10-09。状態：`OWNER APPROVED / ONE-CLASS OBSERVATION COMPLETE / THIRD JVM NOT OBSERVED`（§4〜§5）。

通常rootは成功したが、S1専用NotificationFoundationMigrationTestの起動中にJVM3となり、承認上限2で停止した。第三JVMの識別情報が保存されていないため、恒常的に上限を増やす判断材料は不足している。まず停止した1クラスだけを有限観測する案とする。

入力：[分離案の承認と結果](phase4-s1-normal-build-dedicated-verification-separation-owner-review-20261009.md#10-承認後の実行結果と停止)、[Evidence§16](../architecture/validation/phase4-s1-b2-normal-build-remediation-20261009.md#16-s1専用12件passとmigrationのjvm上限停止)。実行sourceは基点8b03b9f＋承認14ファイルのhash manifest。code／assertion／resource設定を追加変更しない。第三JVMがMockito／Byte Buddy自己attachかどうかは未確認である。

## 1 限定観測の具体案

1. 既存rawとsourceを保持し、hash・baseline JVM・container0・残予算を再照合する。新しいcommand名／rawを使用する。
2. tmp内の検証runnerだけで、Java系processのPID、parent PID、開始時刻、実行file名、起動classの識別区分を保存する。command line全文・credential・接続情報は保存／表示しない。baseline2件を除外し、親子関係も照合する。
3. NotificationFoundationMigrationTestだけを1回、最大120秒（cleanup30秒を含む）で明示実行する。共通resource-limits=true、Maven／test fork heap768 MiB、並列・自動再実行なし。今回の観測に限り当該JVM最大3、PG1／Ryuk1／接続16を提案する。これは診断用上限であり、専用S1の恒常上限2の訂正や、第三JVMのheap／用途の保証ではない。
4. 第三JVMの所有関係が不明、最大3超過、他の資源超過、test失敗、時間超過、cleanup失敗では停止する。第三JVMの存続時間と用途を記録し、5件の完了・skip・failureをXMLで照合する。
5. 結果にかかわらず、この1クラスの後は停止する。残りS1／Tooling準備／B2を自動実行せず、第三JVMの観測と適切な上限・必要設定をOwnerへ提示する。

新しい依存取得・agent JVM設定供給・javaagent追加・helper修正・pool変更・Ryuk無効化は含めない。第三JVMの最大heapが確認できなければUNKNOWNとして記録し、通常Maven／forkの768 MiBと同じと断定しない。

## 2 予算と未完了事項

S1専用8分枠で既知command＋cleanup消費約79.54秒、残約400秒。診断120秒はこの残枠から充当し、総90分を増やさない。過去root423.53秒＋今回232.54秒、標準install15.09秒、preflight保守的600秒を引き継ぐ。観測準備・cleanup管理の未計測分も実行前に保守的計上し、予算を再計算できなければ開始しない。最終cleanup5分を流用しない。

通常root257 PASS／92無効と、専用S112 PASSは確認済み。Migration5件は未完了、残りS1 47件、Tooling準備、B2専用36件、負例3件、package最終照合は未実施。これらを完了扱いにしない。結果受入・commit・remote・Linux実測・Reference非同期実装・DoD／Phase 4全体の開始は本案に含めない。

## 3 Owner判断事項

第三JVMを識別するためのrunner記録追加と、診断用JVM最大3／120秒・Migrationクラス1回だけの限定観測を採用するか。本案作成時点ではその記録追加・上限変更・再実行を行っていない。

## 4 Owner承認記録

2026-10-09、Ownerは本案を確認し、「承認します。限定観測を進めます」と明示した。§1〜§3のrunner記録追加、Migrationクラス1回だけ、診断用JVM最大3、cleanupを含む120秒、既存残予算からの充当を承認済みとする。source・環境・予算差分確認後に実施し、結果にかかわらずこの1クラスで停止する。残り集合・設定変更・commit・remoteの開始承認には拡張しない。

## 5 限定観測の結果

2026-10-09 09:35:57〜09:36:19 JST、Migrationクラス1回の明示実行はexit0、5 PASS／failure0／error0／skip0。command21.52秒、cleanup11.56秒、合計約33.09秒で120秒以内。終了後baseline JVM2件のみ、container0、14 source hash不変を再確認した。第三JVMは今回のsampleでは観測されなかった。

最大JVM2／PG1／Ryuk1／接続4。所有親子関係を確認したprocessはPID24432（親5916、起動00:35:57.600625 UTC、起動class区分UNKNOWN）と、Surefire fork PID9212（親4420、起動00:36:00.768479 UTC）。両processのcommandに明示Xmx768mを確認した。PID24432は今回起動したcommandの子孫だが、保存した識別区分でmain classを確定できない。command line全文・credential・接続情報は記録しない。

250 msのwaitとCIM／Docker／DB観測を組み合わせた有限sampleであり、短命processを漏れなく捕捉する保証はない。前回の第三JVMの用途・heap・存続時間はUNKNOWNのまま。今回5件が成立したことは確認したが、自己attach説の立証や、他クラスでJVM2が常に成立する証明にはしない。専用S1の恒常上限2は維持する。

rawは`tmp/b2-normal-build-8b03b9f-20261009-first/evidence/migration-jvm-observation*`、開始／終了確認は`jvm-observation-environment.json`／`jvm-observation-final-cleanup.json`、source照合は`jvm-observation-source-after.json`。観測runnerは同runの`Run-Jvm-Observation.ps1`。最初の呼出しはPowerShell引数解析でMaven起動前に終了し、直接script呼出しへ訂正した。実際のMaven実行は1回だけで、test再試行はない。

観測準備・環境確認の未計測分をS1枠に追加120秒として保守的計上。S1累積は79.54＋120＋33.09＝約232.63秒、8分枠の残約247.37秒。root・install・preflight等の従前消費と最終cleanup予約を引き継ぎ、枠をリセットしない。承認どおりこの1クラスで停止した。

## 6 残り集合の再開判断案（未承認）

今後の再開を判断する場合は、今回sourceと新しい観測runnerを用い、S1恒常JVM2／PG1／Ryuk1／接続16を維持して、未実行4クラス47件だけを各1回とする。今回PASSしたMigration5件と先頭12件、通常root、標準installは繰り返さない。新しいraw、各class後cleanup30秒以内、資源超過／第三JVM／未知所有／test失敗で停止する。第三JVMが再度現れた場合は、保存したPID／parent／起動区分を判断材料とし、診断用上限3を流用しない。

残4クラスはNotificationFoundationPersistenceTest12、NotificationFoundationRegistrationTest7、NotificationFoundationTransactionTest16、OperationalRecoveryEvidenceDbTest12。観測準備・class cleanupを含めS1残約247秒以内に収まる条件を開始前に再計算する。成立後だけ、未実施のTooling準備3 goal（install分を引いた準備15分枠）、B2登録8＋process28（12分枠、B2専用Ryuk最大3）、前提負例3件・package照合（5分枠）、最終cleanupへ進む案とする。回数・flag・資源・source／artifact照合・停止条件は既承認分離票のまま維持し、追加取得・新実装修正は含めない。

これは次のOwner判断用の具体案であり、本限定観測の承認だけで実行しない。専用S1は17件PASS／残47件、B2専用36件等は未実行。結果受入・commit・remote・非同期実装の境界は維持する。

## 7 残集合の再開承認

2026-10-09、Ownerは観測結果と§6の再開案を確認し、「これを進めましょう」と明示した。§6の残S1 47件、成立後のTooling準備・B2専用36件・前提負例・package照合・最終cleanupを、引き継いだ予算・回数・資源上限・停止条件で再開することを承認済みとする。S1恒常JVM2を維持し、今回PASSした集合は繰り返さない。source・環境・残予算成立後に実行する。結果受入・commit・remote・非同期実装への拡張は含めない。

## 8 再開後の監視停止と所有数訂正の判断

最初のPersistenceクラスは完了XML前に停止した。第三processはjcmd.exeで、親PID5292は検証前から稼働するCode.exeと確認した。全体数3に対し、保存sampleの検証所有Java数は2。runnerがIDE側診断processも当該上限へ数えていた。§6の第三process／未知所有で停止する条件を守り、追加commandは実施しない。恒常上限は変更せず、[process所有数訂正案](phase4-s1-verification-process-ownership-correction-owner-review-20261009.md)を次の判断入力とする。[Evidence§18](../architecture/validation/phase4-s1-b2-normal-build-remediation-20261009.md#18-残s1再開とide側jcmdによる監視停止)にprocess・親・予算・cleanupを記録した。
