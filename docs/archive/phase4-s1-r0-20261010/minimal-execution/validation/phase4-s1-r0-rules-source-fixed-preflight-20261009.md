# S1 R0-B：現行検証結果（2026-10-10）

**Rules107 PASS／通常root未成立。R0全体の受入前。** 目的に必要な結果だけを載せる。監視pure74例・反復rootの途中成功・失敗診断を本体結果へ加算しない。

| 必要な確認 | 現在の結果 | 完了に必要な証拠 |
|---|---|---|
| Rules 新規40＋既存67 | 107 PASS、failure／error／skip0 | 固定sourceと保存済みXML13ファイルの整合維持 |
| PL2既存規約4件 | 4 PASS／failure・error・skip0、cleanup成立 | 保存済みfresh XML4件と入力Rules JARの整合維持 |
| package・旧Consumer互換性 | PACKAGE_LEGACY_PASS、旧bytecode不変／descriptor・非混入確認／cleanup成立 | Rules／ReferenceのJAR hash・依存／classpath、旧bytecode実行結果 |
| Reference E2E 1件 | 1 PASS／failure・error・skip0、固定JAR不変／cleanup成立 | 新packageによる既存journey1件、既存assertion・cleanup |
| 通常root297件＋skip92 | 未成立 | fresh全体XML、297実行＋指定92 skip、資源／cleanup・source整合 |
| S1／B2専用92件 | 専用再検証対象外 | 通常rootで分離を照合 |

Rules107の正規compile・XML結果は `tmp/r0-rules-9c41aa4-20261009-first/evidence/isolated-rules-tests-xml/` と `isolated-rules-tests-result.json`、sourceは `isolated-source-manifest.json` に保存済み。現在のtargetや別rootの途中XMLをその成立artifactと混同しない。source／入力変更時だけ影響を再評価する。

実行順は安全な最小実行方法の選定→PL2→package・旧Consumer→Reference E2E→通常root。Owner読取preflightは成立。PL2既存4件は成立・固定済み。段階2のReference packageは容量走査停止で未成立。失敗時はその段階で停止・保全し、監視拡張・root反復を自動で行わない。

累計1987秒、残1613秒（26分53秒）、うちcleanup300秒予約。最終停止後の所有process／container／subscription残0・保護Java保持・source不変を確認済み。現在のOS状態を再取得したという意味ではない。今回、検証・process／container操作・code変更・commit・remoteは実施していない。

[現行計画](../../../../development/phase4-s1-r0-root-connection-limit-correction-owner-review-20261009.md)に集合・区切り・資源条件を集約した。過去の承認、監視74例、途中結果と診断は[アーカイブ](../../README.md)だけで参照する。R1／非同期runtime・DoD／Gate・Phase4全体の開始判断を兼ねない。

最小準備の静的照合：snapshot1156／元source hash一致（承認文書差分除外）、保存Rules XML13件・107 PASSを確認。classpath31／129／46 entryの参照先と旧Consumer／JDK／Maven入力を確認し、`minimal-preparation-20261010/static-readiness.json`／`readiness-inputs.json` へ保存。2秒計上済み。`Start-Readiness-Owner.ps1` は読取専用・構文確認済み、Owner環境実行前。新規case・監視pure・build／test・container起動0。

現在の区切り：Owner読取preflightはREADINESS_ONLY_PASS。PL2初回は前提Rules packageのcompile中、6秒でResource ceiling/floorにより停止。PL2 test未起動、Rules cache JAR不変、source不変、Owner cleanup成立。保存sample3件は全て閾値内だが停止時点のmemory／disk／job process数をthrow前に保存しておらず、停止項目はUNKNOWN。原因を断定せず `minimal-preparation-20261010/pl2-stop-preservation.json` にhash付き保全し1秒計上。再実行・新case・上限変更0。次に必要な訂正は停止値保存だけであり、網羅拡張へ戻らない。

Ownerは停止値保存だけの訂正を含む再実行へ進むことを指示した。再実行1回は別script／別出力、旧証拠・helper・上限・4件集合は維持。174秒以内で結果を区切る。準備後のscriptと文書2件を新入力hash台帳へ固定し、未実行の時点ではPL2 PASSへ更新しない。

再実行準備1秒を計上済み、現在累計1944秒・残1656秒（27分36秒）、cleanup300秒予約。新scriptは構文確認済み、Owner端末での再実行は未実施。

現在の停止理由はOWNED_PROCESS_LIMIT（全processカウンタ6）。memory／diskは下限内、Owner cleanup成立。JVM4を全process4へ誤適用したrunner設計を確認し、独自全process制限を外してjob所属の生存Java実行imageだけを数える限定draftを別名準備した。実際のJVM数は記録されておらずJVM4超過は未立証。draftのC# compileだけ成立、再実行は0。`pl2-rerun1-stop-preservation.json` へhash付き保全済み。現在累計1951秒・残1649秒（27分29秒）、cleanup300秒予約。Rules107成立維持、PL24件／package／旧Consumer／E2E／root未成立。

OwnerはJVM数の判定訂正を採用して再実行へ進むことを指示した。新scriptはJob所有を維持し、全process参考値とjob所属の生存Java画像からのJVM数を分けて記録・JVM4を判定する。別出力・169秒以内・既存4件だけで実行を区切る。現在は準備段階、PL2結果は未実行のまま。

再実行2準備1秒を計上し、現在累計1952秒・残1648秒（27分28秒）、cleanup300秒予約。script構文と旧証拠hashを確認済み、Owner実行前。

最新状態：再実行2はJava baseline gateで1秒停止。build／test未起動、Cleanup=falseはbaseline一致未確認を意味し、今回残留processの証明ではない。停止時CIM一覧がなく差分UNKNOWN。準備source・入力hash不変を保全し1秒計上。現在累計1954秒・残1646秒（27分26秒）、cleanup300秒予約。管理者端末で期待baselineと現CIM一覧を読取保存する手順だけを準備。baseline自動更新・process停止・再実行は行っていない。

最新Owner CIM読取は期待baseline2件と名前／PID／creationまで一致、未知PID0。baseline更新なし。前回不一致は未確定のままで、確認値を判定前に保存する限定scriptを別名準備した。読取1秒＋保存・準備1秒で現在累計1956秒・残1644秒（27分24秒）、cleanup300秒予約。build／test未実行、規約107以外の成立表は変更しない。

最新結果：PL2既存4件はPL2_FOUR_PASS、cleanup成立、最大観測JVM2、全process参考値6。Rules JAR入力・XML4件・log等を `pl2-four-fixed-evidence.json` にhash固定し、PL2入力JARは別保存した。Rules107成立を維持、専用92件・Reference E2E・root未実行／未成立の区別を維持する。次は通常package・旧binary Consumer互換性だけの300秒手順を準備済み。現在累計1987秒・残1613秒（26分53秒）、cleanup300秒予約。

最新の区切り：package初回は32秒、Reference clean中に容量走査がtarget/test-classes消失で停止、Owner cleanup成立。新Reference artifact・旧Consumer互換性は未成立。source1156とPL2固定証拠hashを確認・初回失敗を別manifestへ保全済み。target内DirectoryNotFoundだけ全容量走査を追加1回する限定訂正を別scriptへ準備した。package残268秒／package＋E2E残448秒、累計2021秒・残1579秒、cleanup300秒予約。Owner再実行前、Agentによるbuild／test起動なし。Rules107・PL24件の成立を維持する。

初回Reference reactorはRules JARも再packageしており、新hash49925B073772E5AB9A04A2F6769272BAA01D9B30A7965C1D7465E681DD6303B3。PL2入力DED17…とは別epochとして保存し、全26 classの名前・byte一致を確認した。PL2固定JAR／XMLは不変。訂正準備中の入力hash照合はこの想定済み再packageを検出して停止したため、元hashを上書きせず新入力台帳にPriorEpochSha256と新hashを明示する。build再実行はしていない。

最新結果：Maven Reference reactor13 module BUILD SUCCESS／Exit0／所有Job空。直後のbaseline照合で短命jcmd.exe追加1件を検出して停止、cleanupでは既知2件へ戻った。生成artifactのmain／nested KOIKI hashとRules26 classのPL2入力一致を静的確認・固定済み。段階全体PASSではなく旧Consumer互換性／descriptorは未実行。成功buildを再実行せず未実施分だけの再開scriptを準備、baseline期待値を維持。累計2063秒・残1537秒、cleanup300秒予約、package残228秒／package＋E2E残408秒。Agent build／test起動なし。

最新の区切り：旧Consumer用fixtureのcompileはrunner引数分割（`@`とpath）によりjavac Exit2、3秒で停止、cleanup成立。source1156／固定artifact・PL2証拠は不変。結合式への括弧だけを訂正した別scriptを準備、reactor再buildなし。旧Consumer／descriptorは未実行のまま。package残225秒／package＋E2E残405秒、累計2067秒・残1533秒、cleanup300秒予約。新case・専用92件・E2E／root起動なし。

最新結果：package・旧Consumer段階はPACKAGE_LEGACY_PASS、7秒／cleanup成立。JAR hash・bytecode不変・descriptor・非混入と保存証拠をpackage-legacy-fixed-evidence.jsonへ固定済み。次の既存Reference E2E1 journeyだけを180秒／PG1・Ryuk1・client8・JVM4で準備、Owner実行前。Rules107／PL24件を維持、通常rootは未成立。累計2075秒・残1525秒、cleanup300秒予約。Agentによるbuild／test／container起動なし。

最新結果：Reference E2E既存1 journeyはREFERENCE_E2E_ONE_PASS、31秒・cleanup成立。固定JAR／fresh XML・資源log／sample／baselineをhash保存、成立済みRules／Reference JARはtarget外へ別保存した。残る実行集合は通常root297＋予定skip92だけ。root600秒／JVM4・PG1・Ryuk2・client14の手順を準備、Owner実行前。通常root未成立・R0最終受入前を維持。累計2110秒・残1490秒、cleanup300秒予約。新case／専用92件／pure拡張・Agent build／container起動なし。

最新の区切り：通常rootは208秒でDB接続数観測失敗、cleanup未成立。保存sample最大JVM3／PG1／Ryuk2／client11、失敗時SQL Exit／stderr・cleanup例外未保存で原因の細部UNKNOWN。途中XML187件（実行187／skip0）は別保全し、完全root PASSへ加算しない。source／成立済み独立証拠と別保存JARは不変。現在残留の読取だけを準備、build再実行なし。累計2319秒・残1281秒、cleanup300秒予約。

最新状態：Owner読取で現在cleanup成立（保護Java2件一致・running container0）、歴史STOPPED／Cleanup=falseは変更なし。根本原因は未確定のまま、TCP起動確認・SQL失敗の有限診断保存・既存Job観測Java由来log ID照合・cleanup診断保存だけを別helper／scriptへ準備した。新case／網羅拡張なし、root残392秒・追加1回の実行前。累計2322秒・残1278秒、cleanup300秒予約。成立済み独立4集合と保存JAR／sourceは不変、通常root未成立。

新root scriptでは旧資源sampleのContainers=0固定欄を除去し、container数は実数を保存するcontainer-resources.jsonで照合する。歴史sampleは上書きしない。
