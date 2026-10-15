> ARCHIVED / HISTORICAL ONLY（2026-10-10）。現在の実行指示・進捗判定には使用しない。

# S1 R0-B：文書source固定・preflight停止記録（2026-10-09）

**状態：SAME SOURCE SNAPSHOT / RULES107 PASS / NORMAL ROOT STOPPED — CLIENT13 EXCEEDS APPROVED12。** tmp終了競合訂正後、実際の資源上限超過で停止。root全体・package・結果受入は未成立。

## 1 文書commitの実行結果

Ownerは「local commit 1回で固定してよい」と明示した。[commitレビュー](../../../development/phase4-s1-r0-rules-document-source-fixed-commit-review-20261009.md)の14 pathとworktree集合・branch／親HEADを照合し、対象だけをstageして実行した。

- branch：`feature/phase4-s1-reference-foundation`
- 親HEAD：`fe93b5da76a85fd6a4c41409c725c35661fac007`
- 新HEAD：`9c41aa4b3fc93cd0c6afb2c767171a771e42616b`
- message：`docs: approve S1 R0 event-level rules contract`
- 対象：文書14 file、551 insertion／2 deletion。commit直後worktree clean。
- code／POMの親commitとの差分0。remote操作0。

本票と訂正レビューはその後のpreflight記録として追加する未commit文書であり、上記commitへamend・追加commitしていない。

## 2 preflightで確認したこと

JDKはTemurin `21.0.12.1`、Maven `3.9.16`。ParentのJDK `[21,22)`／Maven `[3.9.16,3.10.0)`条件に適合。wrapper配布物は既存cacheに存在し、version確認だけを実施した。dependency取得・test／verify／packageは実行していない。

Rules testのannotation検索は既存67 method候補。予定新規40を加えた107候補はmodule120件の上限内だが、invocation確定・fresh XMLは未実施。既存B2専用m2にはRules JAR、Modulith API `2.1.1`、Surefire JUnit provider `3.5.6`がある。Playwright `1.62.0`は同m2にはなく、元の`.m2/repository`には存在する。browser cacheも存在する。依存closure／新専用classpathの完全成立は未確認で、限定取得はしていない。

最終read-only baselineではDocker Server `29.5.3`、container0。既存Java PID `24852`／`10280`（親PID `5292`）を保持し、今回所有Java／container0。空きmemory `17,716,867,072` bytes、disk `47,662,260,224` bytesで開始条件を満たす。これら既存Javaを今回の検証用と扱わず、停止しない。

Rules／Reference／Tooling／E2E sourceとPOMの保護hash台帳417 fileを保存した。旧tmpのsource／raw／cacheを変更せず、copy・生成・baseline Consumer compileは未実施。

## 3 停止した理由

[承認済み規約票§7.2](../../../development/phase4-s1-r0-rule28-29-level-selection-owner-review-20261009.md)は今回所有DB接続8を上限とする。一方、同日の受入済み通常rootの`root-separation-resources.json`ではclient connection最大11、8超のsample14件を確認した。sample例は2026-10-09 09:24:35.899 JST、PostgreSQL1／Ryuk1／JVM2／client connection11で、観測不可sampleではない。

これは旧rootの実測であり、今回rootが既に上限超過した結果ではない。今回rootは未実行。新sourceで接続8以内に収まる証拠がなく、既存test／pool変更も除外なので、資源条件の整合前にcode・testを先行しなかった。開始票作成時に既存回帰の接続実績照合が不足していた。

旧rootの最大はJVM3／PostgreSQL1／Ryuk2／接続11。Ryuk2は今回root限定上限に収まるが、接続11は8に収まらない。計測対象からobserverや管理接続を引いて数値を小さくしない。

## 4 予算・証拠・再開条件

初回preflight開始は2026-10-09 15:48:51 JST、停止集約まで実測227秒。source hash・最終確認等の管理予備13秒を保守的に加算して240秒（4分）を計上する。文書編集・Owner／実行環境承認待ちは除外。総60分枠の残56分、preflight10分枠の残6分、cleanup予約5分を維持する。未消費を追加実行許可にしない。

端末内raw：`tmp/r0-rules-9c41aa4-20261009-first/evidence/`。
`commit-preflight-result.json`、`environment-baseline.json`、`protected-source-manifest.json`にcommit identity、停止理由、予算・baselineを保存した。元のroot sampleは`tmp/b2-normal-build-8b03b9f-20261009-first/evidence/root-separation-resources.json`で保持する。Gitだけではこれら端末内rawを別端末へ移さない。

再開は[root接続上限限定訂正レビュー](../development/phase4-s1-r0-root-connection-limit-correction-owner-review-20261009.md)のOwner判断と、採用した訂正文書の別source固定後だけ。規約code／Public APIは未変更、test0、起動container0、追加再実行0。R0-C受入・R1／runtime開始・remoteは未成立のまま保持する。

## 5 接続上限訂正後の再開結果と時間集計停止（2026-10-09）

Ownerが接続上限訂正票を承認したため、指定の文書4件を追加local commit1回で固定した。新HEADは`671fd171a7c2ce70eff571995a5f20b3c598c1ae`、親は`9c41aa4b3fc93cd0c6afb2c767171a771e42616b`、messageは`docs: correct R0 root connection ceiling`。対象4件、84 insertion／1 deletion、commit直後clean。remoteは未実施。

既存B2専用m2の読み取りコピーと、元の`.m2`にあるPlaywright `1.62.0`・Gson `2.13.2`・error_prone_annotations `2.41.0`の補完で、E2E／Rules／PL2 jdbcのtest classpathをofflineで解決した。新規download0。最初の専用cacheにGson、次にそのtransitiveが漏れたためclasspath組立てが2回失敗し、それぞれのlogを保存した。原本cacheに存在する同じversionだけを補完して最終解決した。これはtest／verifyの再実行ではなくpreflight組立てで、validation testは依然0。

旧Rules JARのhash・公開descriptorを保存し、そのJARで最小Consumerと既存simple fixtureをJDK21でcompileした。Consumer bytecodeを保存し、新Rules JARへの接続確認はpackage検証まで実行しない。保護source417 fileは差分0。最終環境・classpath file存在確認は成立し、container0・当該owned Java0、既存Javaを保持した。

時間guardには誤りがあった。PowerShellのJSON変換でDateTime化されたUTC値を再度文字列としてParse／ToUniversalTimeした結果、9時間多い`32,970秒`を計上し停止した。元の`preflight-final.json`を上書きせず、raw JSONのtimestampをDateTimeOffsetで比較した`preflight-time-correction.json`を保存した。

実際の再開確認区間は17:53:00.714〜18:02:29.261 JST、568.547秒。権限付き実行のOwner待ちが含まれるが、開始・終了境界を分離計測していないので控除しない。保守的に570秒＋先行240秒＝810秒（13分30秒）を計上する。総60分の残46分30秒、cleanup予約5分を保持する。preflight10分枠での成立を証明できないためcode作成・test前で停止した。技術条件の成立と時間枠の成立を区別する。

同runの`resume-start.json`、各classpath log／result、`baseline-consumer.json`、`baseline-public-descriptors.txt`、`resume-environment.json`、時間訂正jsonへ記録した。Consumer／JARのhash、copyした依存の元cacheとrun所有cacheを区別する。今回の規約変更・validation test・container起動は0。追加commitはOwner承認分の1回だけで、新たな成果commitをしていない。

再開には訂正票§5のOwner判断が必要。新たな依存取得・既存test変更・validation集合の再実行を求めるものではない。

## 6 preflight時間枠の訂正承認

Ownerは訂正票§5を「この限定訂正で進めてよいです」と承認した。preflight15分、総60分・残2790秒・cleanup300秒予約、指定文書4件の追加local commit1回と差分確認／成立時の限定実装・初回検証に進む。commit identityと差分確認実測は続く実行記録へ保存する。

## 7 訂正source固定・差分preflightと初回compile結果

指定文書4件を承認どおり追加local commit1回で固定した。親`671fd171a7c2ce70eff571995a5f20b3c598c1ae`、新HEAD `e8c81fe46fcc1f3de19c66fb6c5eea180831b9b7`、message `docs: correct R0 preflight time accounting`、45 insertion／1 deletion、commit直後clean。remote0。

差分preflightは0.735秒、保守的に1秒計上。累計811秒で訂正後15分以内。保護source417件差分0、既存3 classpathの全entry存在、container0、既存Java2を保持、空きmemory17,667,141,632 bytes／disk46,808,604,672 bytesを確認した。依存探索・download・旧Consumer再compileを反復していない。`time-corrected-source-preflight.json`へ保存した。

その後、Rules内のmain4 file（新enum／helper、既存Facade／rule set）、test5 file（新4 class／40 method、既存contract）、fixture6 fileを作成・変更した。合計15 fileで24以内、fixture6で12以内。新API・Rule28選択・Rule29と互換性検証を作成したが、未検証の実装でありPASSではない。`case-ledger.json`は10／12／8／10の40 methodと実装fixtureを記録する。parameterized invocationの追加なし。

run所有監視scriptの構文検査では最初に改行したPowerShell論理演算子のparse errorを検出し、Maven起動前に訂正した。validation再実行には数えない。その後、初回Rules module `test`をofflineで実行したが、main compileにて以下2診断でexit1。test compile／Surefireへ到達しておらず、今回のtest実行0。

- `BusinessModuleRuleSet.java:533`：`JavaEnumConstant.getName()`は存在しない。既存classpathの`javap`でaccessorは`name()`と確認した。
- 同531：booleanとObjectへの`&&`型不一致。先の未解決methodを含むOptional lambdaの型診断で、根本原因の独立性は未確認。Optional chainを明示的な`JavaAnnotation<?>`／取得値／enum判定に分ける有限訂正を提示し、compileで確認する。

初回process計測4.561秒、5秒計上、累計816秒。JVM観測最大2／PostgreSQL0／Ryuk0／DB接続0、resource guard停止なし。元log・exit・PID／起動時刻・samplesを`rules-first.log`、`rules-first-result.json`、`rules-first-resources.json`に保全した。rawのcompiler日本語文字列はencodingにより一部文字化けしており、上記はfile位置・symbol名・実効APIから整理したもの。

cleanup確認0.769秒を1秒計上、累計817秒、残2783秒（46分23秒）、cleanup300秒予約を維持する。今回所有process残0、container0、既存Java PID24852／10280保持、Rules外の保護source差分0。raw142,121 bytes／run544,465,187 bytesで上限内。`rules-first-cleanup.json`と`execution-budget.json`へ保存した。時間はprocess内Stopwatchから計上し、承認待ち・code／文書作成を混入していない。

規約票§7.2の「test失敗」「追加再実行0回」「失敗後の再実行は別判断」に従って停止した。root／PL2候補／package／旧binary Consumer／既存E2Eは0回のまま。Rules codeのcommit、R0-C受入、R1／runtime開始は行っていない。次は訂正票§6の限定修正・Rules追加1回のOwner判断。既存上限や未実行集合の回数を自動で増やさない。

## 8 §6承認後のcompile成立・Rules107件結果（2026-10-09）

Ownerが訂正票§6を明示承認したため、`BusinessModuleRuleSet`のpropagation判定を採用案どおりに分離し、`JavaEnumConstant.name()`と型の照合へ訂正した。tmp runnerのconsole要約も`[pscustomobject]`変換へ訂正した。Rules sourceを`rules-approved-correction-source.json`でhash固定し、追加Rules1回だけをofflineで実行した。code／文書の新commit・remoteは0。

main／test compile成立、Surefire107件、74 PASS／2 failure／31 error／skip0。既存9 class／67件は全PASS、新規4 class／40件は7 PASS／2 failure／31 error。重複・未実行集合をPASSへ加算しない。2 failureの違反自体はdetailに存在するが、diagnostic IDをdetailから探すtestの誤りだった。31 errorはすべて診断helperへのRule29登録漏れで、rule生成時の例外。errorの先にある検出／assertion成立は未確認。

`RuleMessage.violation()`はdetailだけを返し、IDは`description()`に含む。既存contractとnegative testは個別ruleの実評価でIDを対応づけている。新規4 classの同種誤りを静的検索で点検した。詳細な有限訂正は[訂正票§7](../development/phase4-s1-r0-root-connection-limit-correction-owner-review-20261009.md)へまとめ、未適用・未承認とする。

`rules-approved-retry.log`／`rules-approved-retry-result.json`／`rules-approved-retry-resources.json`、fresh XML13 fileをコピーした`rules-approved-retry-xml/`とSHA付き`rules-approved-retry-xml-summary.json`へ結果を保全した。process実測36.531秒を37秒計上し累計854秒。CIM／Docker・source hash・XML保全の実測cleanup0.877秒と管理予備を合わせ5秒計上、累計859秒、残2741秒（45分41秒）、cleanup300秒予約を維持する。

owned process残0・container0、既存PID24852／10280保持。観測JVM最大2、PG／Ryuk／接続0。Rules修正source hash不変・Rules外の保護差分0。raw487,675 bytes／run544,810,757 bytesで上限内。`rules-approved-retry-cleanup.json`／`execution-budget.json`へ保存した。

§6の「さらなる失敗時停止」に従い停止済み。root・PL2候補・package・旧Consumer・E2Eはいずれも未実行。compile成立と既存67 PASSは確認済みだが、Rules全体・Rule29新API・Reference回帰の成立やR0-C受入を意味しない。次は訂正票§7のOwner判断。

## 9 §7承認後のsource訂正と不整合targetによる探索停止（2026-10-09）

Ownerは訂正票§7を「確認し、承認いたします」と明示承認した。RuleMessageの許可IDへ29だけを追加、新規4 test classのRule識別を個別ruleの実評価とpublic compositeへのdetail包含へ訂正した。既存message形式・67件・fixture・本体検出条件・新規40件の数を維持した。code対象は16 file、source hashは`rules-id-assertion-corrected-source.json`で保存。

追加Rules実行は19:03:01.421〜19:03:04.775 JST、3.352秒、exit1。main／testともcompile不要と判定され、JUnit探索で`NoClassDefFoundError: PackageName`。今回test実行0、fresh test XMLによるPASSなし。前回107件XMLは既に別dirで保全されているが今回結果ではない。

実行前19:02:51に更新されたtargetのFacade／新testに、`Unresolved compilation problems`を投げるbytecode、不正descriptor `()LPackageName;`を`javap -v`で確認した。同packageの本来のPackageName.classは存在する。今回Mavenがcompileせず別生成者のclassを再利用した事実と、書込みprocess未特定を区別する。IDE生成を疑うが確定しない。既存Java／IDE停止や依存補完は行っていない。

`rules-id-assertion-retry.log`／`result.json`／`resources.json`（実際のfilenameは同stage接頭辞）を保全。`rules-id-assertion-discovery-failure/`に不整合class2件・SHA／更新時刻、javap出力、今回dump／dumpstreamを保存した。source訂正の検出成立や正規Maven artifact成立は未確認で、codeの新たな誤りとも断定しない。

process4秒＋artifact保全／cleanup5秒を計上し累計868秒・残2732秒（45分32秒）、cleanup300秒予約を維持する。今回所有process残0／container0、既存Java24852／10280保持、訂正source hash不変・Rules外保護source差分0。`rules-id-assertion-cleanup.json`へ保存した。

§7のさらなる失敗停止に従い、root／PL2／package／Consumer／E2Eは未実行のまま停止。次の[訂正票§8](../development/phase4-s1-r0-root-connection-limit-correction-owner-review-20261009.md)は、同一sourceをrun所有snapshotへコピーして新compile epochを固定する検証環境訂正案。未適用・未承認で、追加Rules再実行やsnapshot作成を先行していない。

## 10 §8承認後の正規compile・Rules成立と通常root監視停止

§8をOwner承認後、`source-isolated`へsource1156 fileをコピーし全pathの元／snapshot hashを照合した。初回列挙ではGitが日本語path12件をquoteしたため当該copyが未完了だった。失敗前recordを`isolated-source-copy-incomplete.json`／`manifest-incomplete.json`で保全し、per-command `core.quotepath=false`で未copy12件だけを補完した。既存成功fileは再copyせず全1156件のhash一致を確認してからcompileへ進んだ。Git設定を変更していない。コピー・補完5秒計上。old target／tmp／cacheをコピーしていない。

snapshot内`clean test-compile`成立（11.599秒→12秒）、main10／test90 sourceを正規javac・JDK21設定でcompile。173 classに`Unresolved compilation problem`がないこと、Facade公開descriptorと`rootPackage()`のFQCN descriptor成立を静的確認した（1.404秒→2秒）。Surefire直接goalの追加1回で107件全PASS、既存67＋新40、failure／error／skip0（31.215秒→32秒）。Rules phase計46秒で15分以内。

`isolated-source-manifest.json`／`isolated-source-copy.json`、`isolated-rules-compile-result.json`／log、`isolated-rules-compile-artifacts.json`、descriptor2件、`isolated-rules-static-check.json`、`isolated-rules-tests-result.json`／log／resources、fresh XML13件の`isolated-rules-tests-xml/`とhash付きsummaryへ保存した。root開始前にsourceとcompile class hash不変を照合した。

同snapshotの通常root `clean verify`初回は19:12:22.760〜19:14:02頃、100.082秒→101秒計上。owned JVM28600のRyuk起動時にtmp runnerの所有guardが停止。root完了・Reference DB回帰・予定無効92の最終照合は未成立。部分fresh XML117件（failure／error／skip0）を別dirへ保存し、root全PASSとして扱わない。Rulesの107件はこの117件にも含まれ、二重加算しない。

tmp所有guardは全containerにsession labelを必須にしていた。実効Testcontainers2.0.5の`RyukResourceReaper.register`はRyuk自身にlabelを追加する登録を省略する。`ryuk-resource-reaper-javap.txt`／`ryuk-container-javap.txt`／`generic-container-javap.txt`へ保存した。実際のcontainer full IDはowned JVM起動logに記録されるが停止後には既に削除され、当該labelsは採取不能。拒否subconditionを保存していなかった限界を保持し、[訂正票§9](../development/phase4-s1-r0-root-connection-limit-correction-owner-review-20261009.md)へtmp限定修正を提案した。

root停止log／result／samples、`root-ownership-stop-diagnosis.json`、`root-stopped-partial-xml/`とsummary、`root-stop-cleanup.json`を保全した。観測JVM最大2、停止前PG／Ryuk／接続sampleは0だったが停止containerを記録前に拒否したため最大Ryuk0と断定しない。root資源成立は未了。

当該process残0／container0、既存Java24852／10280保持、元／snapshotの1156 source hash一致。cleanup・証拠保全5秒、実効API静的照合5秒を計上、累計1030秒／残2570秒（42分50秒）、cleanup300秒予約。`section8-results-budget.json`の参考`RulesPhaseSeconds=47`は実計上46秒より1秒多い予備値であり、総台帳1030秒を正本とする。PL2／package／Consumer／E2E未実行、tracked code・Reference変更追加0、commit／remote0。Rules限定PASSとR0-C受入／root成立を区別し、§9判断前に追加実行しない。

## 11 §9承認後の所有判定成立とcontainer終了競合停止

Ownerは訂正票§9を「承認します」と明示した。tmp runnerでRyukのname／raw timestamp／owned JVM起動ID、PGのsession label対応を照合し、5秒以内の有限対応待ち・拒否metadata保存を追加した。実containerなし6例は全PASS、`ownership-six-cases.json`へ保存した。snapshot1156 fileは全hash不変、元sourceは承認／Evidence更新の2文書だけを除いて不変。`section9-pre-run.json`へharness hashと文書差分の範囲を記録し2秒計上した。

追加通常rootは168.556秒→169秒計上で停止。tmp runnerの`docker ps`にあったRyuk ID `b614daf637b5`がinspect時には`no such object`となり、`Container identity observation failed`を出した。保存済みfull ID・image・name・`OWNED_LOG_ID`の台帳は一致し、今回ownedである。停止原因は列挙／inspect間の消失を一律失敗とするtmp監視であり、test／resource失敗を検出したものではない。

部分fresh XML27 file／168件、failure／error／skip0を`root-ownership-corrected-partial-xml/`とSHA付きsummaryで保存した。Rules107はこの部分集合にも含む。root全体・予定無効92の照合・Reference全回帰・packageは未成立とする。観測最大JVM2／PG1／Ryuk2／DB client11。owned container履歴の累計Ryuk4／PG4を同時最大として加算しない。

元log／exit／owned PID・起動時刻／container full ID・image・name／samplesは`isolated-root-approved-retry-*.json`とlogに保全。root停止後のCIM／Docker・snapshot source照合でowned process残0／container0、既存Java24852／10280保持、元code／snapshot hash不変。`root-ownership-corrected-cleanup.json`に記録した。cleanup2.343秒と証拠予備を合わせ5秒計上、累計1206秒・残2394秒（39分54秒）、cleanup300秒予約を維持する。

§9のさらなる失敗停止に従い、未実行PL2／package／Consumer／E2Eへは進んでいない。次の[訂正票§10](../development/phase4-s1-r0-root-connection-limit-correction-owner-review-20261009.md)はtmpだけの正常終了競合訂正・4例確認と通常root追加1回案。未適用・未承認、Rules単独の追加実行は不要とする。

## 12 §10承認後の有限4例成立・通常root接続上限超過

Ownerは訂正票§10を指定して「案を承認します」と明示した。tmp runnerだけにfull container ID列挙、既にownedとして確認したIDの正常消失／non-runningの有限再確認・終了記録を追加し、未知ID／存続中の観測失敗は拒否を維持した。実containerなし4例が全PASS、`retirement-four-cases.json`へ記録した。snapshotと元codeのhash確認、harness hashを`section10-pre-run.json`へ保存、2秒計上。tracked code／既存test／pool追加変更0。

追加rootは124.372秒→125秒計上で`TIME_OR_RESOURCE_LIMIT`停止。最終sampleの当該PG client13が上限12を実超過した。時刻19:32:08.844 JST、JVM2／PG1／Ryuk1／client13、直前はPG not-ready。observerを含む従来queryを維持しており、13を保持元の内訳不明のaggregate実測として保存する。library／runnerの実行errorやtest assertion failureとは区別する。

部分fresh XML128件、failure／error／skip0を`root-connection13-partial-xml/`とSHA付きsummaryへ保存。Rules107が含まれるがroot全体・予定無効92の最終照合は未了。PL2／package／Consumer／E2Eは未実行。観測最大JVM2／PG1／Ryuk1／client13、memory最小17,080,401,920 bytes／disk最小50,571,608,064 bytes。raw5,265,012 bytes／run565,196,442 bytesで上限内。今回sampleまで終了競合は発生せず、retired event fileが存在しないことを終了イベント0と区別して保持する（PowerShellの空配列出力でfile未作成）。

`isolated-root-retirement-retry-result.json`／log／resourcesに失敗を保全。cleanup2.383秒＋証拠予備で5秒計上、累計1338秒、残2262秒（37分42秒）・cleanup300秒予約。owned process残0／container0、既存Java保持、snapshotと元code不変を`root-connection13-cleanup.json`へ記録した。

§10のさらなる失敗停止に従い追加実行しない。[訂正票§11](../development/phase4-s1-r0-root-connection-limit-correction-owner-review-20261009.md)へroot接続14・他条件維持・追加root1回案を提示した。未承認、root成立／R0-C受入・code成果commit／R1／remoteは先行しない。

§11のOwner承認を受け、通常root上限14・他条件維持・追加1回をAGENTS／規約票と整合した。source snapshotのcode／POMを変更せず、承認文書4件の更新を別に記録して実行へ進む。

## 13 §11承認後の所有済みRyuk再判定停止・子JVM台帳の不足

source hash照合2秒後、root追加1回を`-ConnectionLimit 14 -RyukLimit 2`で実行した。165.116秒→166秒計上で`Container ownership rejected: PENDING_SESSION_PAIR`停止。拒否Ryuk full ID `af546844fbd2263316660d41fbdded4c3730a6ea7012eaad1a783a9f7f0aa400`は同runの所有台帳で既に`OWNED_SESSION_PAIR`成立済み。対応PGは19:41:48.608 JSTにnon-runningを記録したが、runnerがlive pairを再要求して所有済みRyukを未確定へ戻し、5秒後に停止した。container identityの維持不足を確認した。

同Ryuk起動logのPID30512がprocess所有台帳に存在しない。短命parentをCIM一覧pollで捕捉できなかった可能性があるが、実parentの履歴は未採取なので仮説とする。実行済みforkの台帳欠落は確定し、JVM sample最大2を全JVM捕捉・資源成立の証明にはしない。接続上限14超過による停止ではなく、監視の所有判定／捕捉不足として保全する。

fresh部分XML25 file／154件、failure／error／skip0を`root-limit14-partial-xml/`とSHA付き`root-limit14-cleanup.json`へ保存。Rules107はその部分集合にも含まれ二重加算しない。root完了・予定無効92照合・Reference全回帰・R0-C受入は未成立。PL2候補／専用package／旧Consumer／既存E2Eへは進んでいない。

記録sample最大JVM2／PG1／Ryuk2／client11、memory最小16,747,028,480 bytes／disk最小50,581,037,056 bytes。raw8,486,406 bytes／run569,209,752 bytes。停止sample前の未記録区間・PID欠落の限界を保持する。`isolated-root-limit14-retry-result.json`／log／resources／ownership-rejection／retired-containersへ保存。今回所有process残0／Docker running container0、既存Java24852／10280保持、snapshot1156 file／元code hash不変を確認し、保全／cleanup5秒計上。累計1511秒・残2089秒（34分49秒）、cleanup300秒予約。

§11の観測不能停止条件に従い追加実行を止めた。[訂正票§12](../development/phase4-s1-r0-root-connection-limit-correction-owner-review-20261009.md)へ所有済みidentity維持と短命launch捕捉を合わせたtmp限定訂正・有限harness確認・root追加1回案を整理した。未承認・未適用。code／test／pool／POM追加変更0、commit／remote0、Rules単独107 PASSと通常root未成立を区別する。

## 14 §12承認後のtmp訂正・起動event harness権限エラー

Ownerは訂正票§12を指定して「承認します」と明示した。tmp runnerへ所有済みcontainer identity維持とprocess start event履歴・CIM creation照合を追加し、parserとdiff checkは成立。tracked Rule／Reference／test／POM追加変更なし。空文字の必須引数bindingで起動前に終了した呼出し後、非空未使用引数で未実行harnessを開始した。

権限付き実行のharnessは0.220秒で`アクセスは拒否されました`として停止。probe出力未作成、短命shell／Javaは未起動、rootも未開始。個別8例JSONは成功後保存のため未作成であり、harness全体のPASSへ昇格しない。messageのみ保存したcatchでは失敗call行・HRESULTを失っているため、厳密な権限拒否点は未確定。Windows起動event登録を含む区間の環境権限エラーとして保全する。

`launch-probe-result.json`へ停止理由／時間／subscription残0を保存。`section12-stop-cleanup.json`でDocker running0／追加Java0・既存Java24852／10280保持、snapshot1156 file／元code hash不変を確認。harness SHA256は`DC7626A2AF2830A2420347BA40C645A3F71978FCA968792648D3F86B8E0D4525`。harness1秒＋保全／cleanup5秒計上、累計1517秒・残2083秒（34分43秒）、cleanup300秒予約。

§12のharness不成立停止条件に従い、通常root追加1回・PL2／package／旧Consumer／E2Eを開始していない。Rules単独107 PASS、通常root未成立を維持。OS設定変更・別event経路による迂回・再試行・commit／remote0。[訂正票§13](../development/phase4-s1-r0-root-connection-limit-correction-owner-review-20261009.md)にtoken有効性と同event登録の限定診断1回だけを整理し、未承認・未実行とした。

## 15 §13承認後の起動event登録拒否点の確定

Ownerは訂正票§13を指定して「承認します」と明示した。`require_escalated`による限定診断1回は0.092秒で完了。管理者tokenはfalse、`Register-CimIndicationEvent`／`Win32_ProcessStartTrace`登録はPermissionDenied。失敗行は`Diagnose-LaunchPermission.ps1`14、原exceptionは`Microsoft.Management.Infrastructure.CimException`。WMI error IDは`HRESULT 0x80041003`、例外objectのCLR HRESULTは`0x80131500`。同じ値として扱わない。

`section13-event-permission-diagnosis.json`へ保存。subscription残0、Java／shell・root未起動、container操作なし。登録権限拒否は確認済み、管理者token不成立だけが唯一の原因であるとの断定や、管理者実行時の成功保証はしない。OS／WMI権限変更・別event provider・追加登録を実施していない。

診断1秒＋source確認1秒で累計1519秒、残2081秒（34分41秒）、cleanup300秒予約。snapshot1156 fileと元code hash一致を`section13-source-check.json`へ保存。Rules単独107 PASS、通常root／後続集合未成立・未実行の境界を維持し、次はOwnerによる実行環境と再開条件の判断。§13は診断だけの承認で、harness／rootを再開していない。tracked code追加変更・commit／remote0。

Ownerは訂正票§13.1の診断結果を確認・承認し、次への具体化を指示した。診断結果だけを`COMPLETE / OWNER ACCEPTED`とし、管理者起動やharness／root開始の承認と区別する。Microsoft公式の当該event登録の管理者実行要件を訂正票§14へ明記し、同ユーザーのOwner管理者PowerShellで既存runner・子Mavenを実行する限定環境と再開案を整理した。監視だけを管理者実行する案ではなく、子検証へのtoken継承を明示した。§14は未承認・未実行。OS変更・管理者起動・追加検証0、予算・source境界を維持する。

## 16 §14 Owner承認・管理者terminal用手順の準備

Ownerは訂正票§14を承認した。同ユーザーのOwner管理者PowerShellで有限harness1回、成立時の未起動root1回、root成立後の未実行PL2／package／Consumer／E2E再開を承認済み。Agentによる自動昇格やOS権限変更を含まない。

tmpに`Start-Section14-Owner.ps1`を準備し、管理者token／SID／branch・HEAD／source hash／artifact hash／baseline／資源／予算のgateと、harness／root・fresh XML／cleanup保存までを連続実行する手順とした。root証拠確認後の後続集合への継続は既承認内で行う。runnerは8例をprobe前に保存、原failure metadata保存、新epoch・二重実行guard・subscription残記録を追加した。旧失敗log／source／code／test／POMを変更していない。

1543 artifactのhash付き`section14-expected-environment.json`を準備し3秒計上。累計1522秒・残2078秒（34分38秒）、cleanup300秒予約。parser／diff check成立。harness／root未実行、Owner管理者terminal起動待ち。具体的なcommandと担当境界は訂正票§14.5へ記録した。

## 17 Owner側§14のharness停止・二重実行guard

Owner提示のguard errorについて保存結果を確認した。先行実行は同ユーザー／管理者token／source・artifact／Docker baseline成立、8例全PASS、既存JDK java-version実行後、harness0.366秒で`Owned PID reused; creation identity mismatch`（launch creation照合、runner行76）停止。root result／XML未作成。今回表示は既存summary検出であり、guard・旧resultを削除／上書きしていない。

harness recordのsubscription残1はcleanup未成立として保持。child終了後のOwner summaryはowned process0／container0・既存Java保持・source hash不変だが、harness残1を後段の値で0へ置換しない。source／Rulesのfailureとは区別する。

tmpは.NET root seedとCIM creationの全tick一致を要求し、WMI microsecond表現差を扱っていない。失敗時の両実ticks・PIDは未保存なので、今回の端数差や実PID再利用を断定しない。また登録返値を解除条件にし、実SourceIdentifierの登録を独立確認していない。これらの有限訂正と実値保存・SourceIdentifier解除を訂正票§15へ整理した。未承認・未適用、11例／短命launch確認・未起動rootの再開を先行していない。

保存台帳累計1528秒・残2072秒（34分32秒）、cleanup300秒予約。Rules単独107 PASS維持、root／後続集合未起動。今回の調査は既存fileとscript・公式仕様の照合だけで、追加登録・Java／root／container操作・commit／remote0。

## 18 §15 Owner承認後のtmp identity・解除訂正と手順準備

Ownerは訂正票§15を承認した。tmp runnerの.NET seed／CIM creation初回照合をmicrosecond精度へ揃え、CIM確認後は厳密一致を維持する。元ticks・正規化値・差・供給元・判定を保存する。SourceIdentifierで実subscriberを確認して当該SubscriptionIdだけを解除し、元failure・cleanup failureとbefore／after countを別保存する。有限11例は実probe前保存、解除残0を必須とした。

`Start-Section15-Owner.ps1`と新manifest1544件を準備。旧§14の7 fileはSHA付き保全、旧1543 artifactのうち承認済みrunner更新以外のhash差分0。新Owner手順だけ1件追加、新result epochと二重実行guardを維持した。manifest照合・作成2秒計上、累計1530秒・残2070秒（34分30秒）、cleanup300秒予約。Parser／diff check成立。Agentからevent登録・Java／root／container起動0、tracked code／test／POM追加変更0、commit／remote0。具体的なOwner管理者terminal commandは訂正票§15.3へ記録し、Owner実行待ち。

## 19 §15 Owner実launch照合停止：event生成時刻と既知root上書き

Ownerの終了連絡と続くconsole outputを保存recordと照合した。gate成立、有限11例全PASS、java-version実行後、harness10.367秒で`Finite launch probe failed`停止。解除before1→after0・failureなし、Owner cleanupのowned process0／container0・既存Java保持・source hash不変は成立。root result／XML未作成、rootと後続は未起動。

RootIdentityはPID12240／parent24856／Owned=false／creation nullで、起動root seedが同PIDのevent派生recordに上書きされている。root eventの保護条件にevent生成時刻とprocess開始の2秒以内を要求し、その条件外でowner parentを持つrootを所有外へ戻す構造を確認した。TIME_CREATEDは公式仕様上event生成時刻であり、process creationとの同一性・2秒以内の契約はない。保存event UTC12:24:53.7114558／probe出力JST21:24:51。元seed値は上書きで失われたため正確な差・遅延内訳・PID再利用は未確定とする。

累計1546秒・残2054秒（34分14秒）、cleanup300秒予約。§15の監視不成立停止条件で追加実行なし。訂正票§16にroot anchor独立保持・event時刻とCIM creation分離・failure時の元identity／履歴保存、有限14例／短命launch確認1回と未起動root再開の案を整理した。未承認・未適用。旧result・guard・snapshot・Rules単独107 PASSを保持し、tracked code／test／POM追加変更・commit／remote0。

## 20 §16 Owner承認後のroot anchor・event時刻分離と手順準備

Ownerは§16を確認・承認した。tmp runnerでroot anchorを独立保持し、root eventの2秒差条件・既知root上書きを除去。event生成／受信時刻はraw・UTCとともに保存、生存子の正式creationはCIM／name／parent／launch期間で独立照合する。元.NET／canonical CIM・派生identity・受信履歴・終了値を成否どちらでも保存する。有限14例と実launch1回、SourceIdentifier解除・残0、root未起動1回の条件付き再開の承認範囲を維持。

`Start-Section16-Owner.ps1`、新manifest1545件を準備。旧§14／15の15 fileを保全、許可したrunner更新以外の旧artifact hash差分0。parser／diff check成立。manifest準備2秒＋最終script identity確認1秒で累計1549秒、残2051秒（34分11秒）、cleanup300秒予約。Agentからevent登録／Java／root／container起動・tracked code／test／POM追加変更・commit／remote0。Owner管理者terminalでの具体的commandは訂正票§16.3へ記録した。

## 21 §16 Owner実行：有限harness成立・通常rootの所有外PID履歴衝突

Owner管理者terminalのgate成立、有限14例は全一致、java-version probe終了0／2.622秒、subscription before1→after0・failureなしを保存recordで確認。root anchorの元.NET ticksとcanonical CIMは初回正規化で一致（差-8 ticks）。通常rootは7.433秒で`Ambiguous duplicate launch PID=26068`停止、fresh XML0、root／予定skip92照合未成立。PL2／package／旧Consumer／既存E2E未起動。

受信154 event中、PID26068は先行所有外`git.exe`／parent10272（生成UTC12:50:16.6124209）と後続所有対象`cmd.exe`／parent20768（UTC12:50:22.6333987）の2 event。後続親は今回Maven Java。CIM親子pollでは同PIDのcreation `639271470206878650`を正式Owned台帳へ登録済みだが、event経路は先行Latest recordの所有区分を確認せずContainsKeyだけで停止する。異なるname／parentの開始eventとtable衝突は確定。先行CIM creation／終了値と停止時の後続CIM詳細は未保存で、完全な世代identity／終了順・event重複配信を断定しない。

Owner cleanupはowned process0／running container0、既存Java24852／10280保持、source hash不変。root subscription残0。途中sample最大JVM2／PG0／Ryuk0／client0は全期間資源成立の証明ではない。Agentの保存照合でsnapshot1156 file／元code hash・1545 artifact hash不変、今回fresh root XML0を再確認。旧XMLを今回結果へ混入しない。

`section16-stop-preservation.json`へ§16旧recordのSHA・照合結果を保全し2秒計上。累計1569秒、残2031秒（33分51秒）、cleanup300秒予約。Rules単独107 PASS維持、通常root未成立。さらなる不成立停止条件に従い再実行・tmp訂正・追加launch／container操作なし。訂正票§17へ、先行所有外recordとCIM確認済み所有instanceの厳密照合・比較値保存、有限22例／短命launch1回と通常root追加1回の案を整理した。未承認・未適用、tracked code／test／POM追加変更・commit／remote0。

## 22 §17 Owner承認後のPID履歴統合・実行手順準備

Ownerは訂正票§17を確認・承認した。tmp runnerに、先行所有外recordと別signatureの後続eventを、既にCIM親子pollで確認した子・親のcanonical creation／name／parentと厳密照合する処理を追加。全条件成立時だけ後続instanceへ移行し、先行所有外履歴とその子の非昇格を維持する。既所有重複、CIM未確認／不一致、同signatureの世代曖昧は停止する。成否どちらでも対象比較値を保存する。

有限22例／短命launch1回・成立時の通常root追加1回用に`Start-Section17-Owner.ps1`を準備した。旧§14〜16の30 recordをSHA付き保全、許可済みrunner更新以外の旧1545 inputはhash不変、新Owner手順追加でmanifest1546件。parser／diff check成立。準備確認2秒計上、累計1571秒、残2029秒（33分49秒）、cleanup300秒予約。Agentから有限例／event登録／Java／root／container起動なし。tracked code／test／POM追加変更・commit／remote0。

Owner管理者terminalでの1行commandは訂正票§17.3に記録した。旧guard・snapshot・上限を維持し、Owner実行待ち。通常rootの成立・後続集合・結果受入へ先行昇格しない。

## 23 §17 Owner実行：有限22例成立・衝突子CIM未捕捉の指定どおりの停止

gate成立、有限22例全一致、java-version probe終了0／2.942秒、subscription残0。通常rootは10.661秒でPID28576の衝突照合に失敗した。先行は所有外git.exe／parent10272（生成UTC13:00:38.6430941）、後続はcmd.exe／parent19260（UTC13:00:43.6660868、受信13:00:45.6293820）。比較recordのChildCim=[]／KnownChildTicks=null、親Java19260はcanonical creation `639271476353969990`と一致。§17の子CIM未捕捉拒否条件に従った停止で、§16のCIM登録済みLatest衝突とは異なる。実終了・完全なPID世代identity・event重複を断定しない。

cleanupはowned process0／container0・既存Java24852／10280保持・source不変、root subscription残0。途中sample5件の最大JVM2／PG0／Ryuk0／client0は全期間資源保証ではない。fresh部分XML2 file／4件（failure／error／skip0）を`section17-root-partial-xml/`へ保存しSHAを記録。root全体・Rulesの今回root内結果・予定skip92照合は未成立、旧Rules単独107 PASSと区別する。PL2／package／旧Consumer／E2E未起動。

snapshot1156 file／元code hash、manifest1546 artifactと旧記録30 fileのhash不変を再確認。`section17-stop-preservation.json`に記録・部分XML・照合を保全し2秒計上、累計1594秒、残2006秒（33分26秒）、cleanup300秒予約。追加tmp訂正・launch／root／container操作、tracked code／test／POM変更・commit／remote0。

訂正票§18に短命shellのevent系譜と生存CIM正式identityの分離、cmd.exeの限定TraceOnly条件・比較記録・受信batch全件保全、有限28例／短命launch1回・成立時の通常root追加1回の案を整理した。子CIM未捕捉例の期待を変える監視契約訂正であり、§17の承認を流用しない。未承認・未適用。

## 24 §18 Owner承認後のTraceOnly系譜・batch保全訂正と手順準備

Ownerは訂正票§18を明示承認した。tmp runnerへ、先行所有外recordと衝突し生存CIMも既知creationもないcmd.exeの限定TraceOnly経路を追加。新event順／異なるsignature・確認済み生存parentの厳密identityを要求し、generation／親generation・creation未確認／終了UNKNOWNを保存する。正式Owned台帳への登録を行わず、子の生存CIMを独立照合する。既知creation／親generation不一致・Java関連の衝突・生存CIM不一致の拒否を維持する。

受信batch全件を分類前に保存し、未処理状態も記録する。旧22例の子CIM未捕捉期待だけを承認どおり改め、承認済み6例を追加した有限28例を実Owner手順に組み込んだ。Agentから有限例／event登録／Java／root／container起動なし。過去の未保存eventや未確認creationを復元したとは記録しない。

`Start-Section18-Owner.ps1`とmanifest1547件を準備。許可済みrunner更新以外の旧1546 inputはhash不変、旧§14〜17のrecord／部分XML49 fileをSHA付き保全。parser／diff check成立。準備確認2秒計上、累計1596秒、残2004秒（33分24秒）、cleanup300秒予約。tracked code／test／POM追加変更・commit／remote0。Owner管理者terminalでの1行commandは訂正票§18.3へ記録した。root全体は未成立のまま、実行結果待ち。

## 25 §18 Owner実行：有限28例成立・一般生存identity照合の停止

gate成立、有限28例全一致、java-version probe終了0／2.916秒・subscription残0。通常rootは8.822秒で`Live launch name/parent mismatch: PID=4784`停止。PID4784のcmd.exe／parent20960 eventは分類済み（生成UTC13:13:20.5302249、受信13:13:20.9338246）、event recordのcreationはnull／CimConfirmed=false／TraceOnly=false。一方で正式ticks表にcanonical creation `639271483986047520`がある。子conhost4248／javac30288 eventも分類済み。受信190 eventすべてClassifiedで、今回の停止は衝突移行例外ではなく一般の生存CIM照合経路。

監視はPIDで旧Latest recordを引き、世代分類前にname／parent不一致でthrowする。CIM登録時の完全なname／parentと、停止時のcurrent実値が保存されていないため、実PID再利用／current identity／旧instance終了を断定できない。移行比較fileも今回未作成で、一般照合の診断保存不足と空集合の保存契約を次のreviewへ残す。欠落値を復元したとは記録しない。

cleanupはowned process0／container0・既存Java24852／10280保持・source不変、root subscription残0。途中sample4件の最大JVM2／PG0／Ryuk0／client0は全期間資源保証ではない。fresh部分XML2 file／4件（failure／error／skip0）を`section18-root-partial-xml/`へSHA付き保存。root全体／Rulesの今回root内結果／予定skip92照合は未成立、旧Rules単独107 PASSと混算しない。PL2／package／旧Consumer／E2E未起動。

snapshot1156 file／元code hash、manifest1547 artifact／旧記録49 fileのhash不変を再確認し、`section18-stop-preservation.json`へ保存。保全2秒計上、累計1617秒、残1983秒（33分03秒）、cleanup300秒予約。tmp追加訂正・launch／root／container操作、tracked code／test／POM変更・commit／remote0。

訂正票§19へcanonical観測の完全なinstance record保存、世代分類後の生存照合、新parent generationの非継承、一般照合failureの実値／空JSON保全をまとめた有限34例1回の案を整理した。未承認・未適用。本案はsynthetic dataの純粋fixtureだけで、実event登録／短命launch／root追加は0回。個別messageごとのroot反復ではなく、契約確認後に実再開を別判断とする。

## 26 §19 Owner承認後のpure fixture：33一致・例間canonical台帳共有による1不一致

Ownerは訂正票§19を明示承認した。tmp runnerへ完全なcanonical instance record・世代分類後の生存照合・別instance非昇格・一般照合の比較値／空JSON保存を追加。pure専用入口と実外部操作を拒否する通常PowerShell用手順を作成した。parser／diff check成立、旧1547 inputは許可済みrunner更新以外hash不変、新手順追加でmanifest1548件、旧記録／部分XML67 file保全。準備2秒計上。

承認済みpure34例を1回実行。比較0.381秒、33一致・1不一致で停止。追加6例は全一致だが全体PASSではない。不一致は旧`Unowned transition batch-failure`。先行例のcanonical PID401／cmd.exe／parent400が例間共有台帳に残り、当該例のOwned=true／git.exe／parent999のsynthetic先行recordと矛盾した。`canonical-event-binding`診断でCandidateCount0となり、batch分類前の生存世代点検で拒否されたため、意図したClassifying／Pending期待に到達しなかった。Agentのfixture台帳分離不足であり、実監視の拒否条件を緩める根拠ではない。旧否定例の意図外拒否の可能性を保持する。

`section19-pure-summary.json`は外部操作attempt=[]／root未起動を保存。実CIM query／event登録／fixture process・Java／Maven／container起動0、actual probe出力なし。純粋なsynthetic確認であり、追加6例の一致を実root／資源／完全網羅の成立としない。

`section19-stop-preservation.json`へ34例33一致／1不一致・旧record SHA・snapshot1156 file／元code hash・1548 artifact／旧記録hash不変を保全。準備2秒＋gate／fixture／保存5秒＋保全2秒計上で累計1626秒、残1974秒（32分54秒）、cleanup300秒予約。停止後の追加訂正・fixture再実行なし、通常root未成立・旧Rules単独107 PASS維持。tracked code／test／POM変更・commit／remote0。

訂正票§20へsynthetic各例のcanonical台帳分離・pure failure Callラベルの最小訂正、同じ34例のpure追加1回だけを整理した。未承認・未適用。実event登録／短命launch／root追加は0回を維持する。

## 27 §20 Owner承認後のpure34例全成立（2026-10-10）

Ownerは§20を確認・承認した。tmpのsynthetic scenario開始時にcanonical台帳を新規化する1箇所と、pure failure Callラベルだけを訂正。実監視の台帳・判定条件・34例の期待値は不変。旧1548 inputはrunner更新以外hash不変、新pure手順追加でmanifest1549件、旧記録／部分XML77 fileを保全、parser／diff check成立。準備2秒計上。

承認済みpure追加1回は0.513秒で**34例全一致・0不一致、pure fixture PASS**。batch-failureは先頭Classifying／後続Pendingを含む期待に一致。owned-duplicate／batch-failureの比較記録はPriorUnowned=false／ChildCimUnique=true／Accepted=falseで、意図した既系譜重複拒否へ到達。前回のcanonical-event-binding早期拒否0、一般照合の実値・空JSON `[]`の保存も確認した。

前後gateはsource／snapshot1156 file・1549 artifact・旧記録hash不変、Failure=null。外部操作attempt0、実CIM query／event登録／fixture process・Java／Maven／container起動／root実行0、actual probe出力なし。summary／34例／identity・transition比較／canonical記録を`section20-result-preservation.json`でSHA付き保全した。

準備2秒＋gate／fixture／保存5秒＋保全1秒計上、累計1634秒、残1966秒（32分46秒）、cleanup300秒予約。日付変更による予算resetなし。tracked code／test／POM変更・commit／remote0。

実OSのevent遅延／短命process／PID世代、全期間資源上限・通常root成立は未確認。過去PID4784のcurrent実値を復元したとはしない。旧Rules単独107 PASS、通常root／予定skip92照合未成立、PL2／package／旧Consumer／E2E未実行、R0-C結果受入別判断を維持する。訂正票§21に実環境gate・有限28例／短命launch1回・全成立時の通常root追加1回と後続未実行集合の再開案を整理した。未承認・未準備・未実行。

## 28 §21 Owner承認・実環境再開手順の準備（2026-10-10）

Ownerは訂正票§21を確認・承認した。新epoch `Start-Section21-Owner.ps1`を、実環境gate→有限28例・短命launch1回→全成立時の通常root追加1回→fresh XML／予定skip92照合／cleanup保全までの手順として準備した。runnerはpure34例成立時から変更0。旧1549 artifact／旧記録hash不変、新Owner手順追加でmanifest1550件、旧§14〜20のrecord／部分XML89 fileをSHA付き保全。pure証拠・同runner epochと保護対象Java identityを明示照合する。parser／diff check成立。

準備確認2秒計上、累計1636秒、残1964秒（32分44秒）、cleanup300秒予約。Agentから実CIM query／event登録／Java／root／container操作なし。tracked code／test／POM変更・commit／remote0。Owner管理者terminalの1行commandは訂正票§21.1へ記録した。実環境gate・harness・通常rootは実行結果待ち。root成立時だけ既承認の未実行後続集合へ進む。

## 29 §21 Owner実行：生存CIM子が正式登録される前の移行停止（2026-10-10）

gate成立、有限28例全一致、java-version probe成立1.773秒・subscription残0。通常rootは22.670秒、PID5456のChildKnownCreationだけがfalseで停止。子CIMはcmd.exe／parent13924／creation `639271671436269350`、親Java13924は既知creation `639271671231316200`と一致。移行比較10条件中9条件true、KnownChildTicks=null。子CIM未捕捉だった§17、一般name／parent不一致だった§18とは異なる。

現行loopのevent分類・衝突判定は、同じcurrent CIMを使う親子poll登録より先。子未登録のまま分類で停止し、後段登録へ到達しない。旧pure例は衝突子を事前登録していたため、この経路の確認が不足していた。PID再利用／先行instance終了を断定せず、ChildKnownCreationを無条件に免除しない。

停止event Classifying・後続conhost27016／java30984 Pendingを保存。他3移行成立を監視全体PASSとしない。Owner cleanupはowned process0／container0／subscription0、保護Java24852／10280のcreation保持、source不変。11 resource sampleの最大JVM2／PG0／Ryuk0／client0は全期間保証ではない。

fresh部分XML3 file／23件（contract4＋BusinessModuleRuleSetTest19、failure／error／skip0）をSHA付き保全。通常root／予定skip92照合は未成立、旧Rules単独107 PASSは別epoch。PL2／package／旧Consumer／E2E未起動。snapshot／元source1156 file（文書4件除外）、artifact1550件・旧記録89 fileのhash不変を確認し、`section21-stop-preservation.json`へ保存した。

保全3秒を計上し、累計1671秒、残1929秒（32分09秒）、cleanup300秒予約。停止後のrunner訂正・追加実行・code／test／POM変更・commit／remote0。訂正票§21.2へ結果、§22へ検証付きCIM子登録と旧34＋新規6＝pure40例1回の案を整理した。未承認・未適用、実CIM／event／launch／root追加0回。実環境再開と結果受入は別判断。

## 30 §22 Owner承認後：検証付きCIM子登録・pure40例全成立（2026-10-10）

Ownerは§22を確認・承認した。tmp移行判定に、既存9条件・親canonical identity・event時刻・既存子canonicalとの整合を登録前に検証し、全成立時だけ未登録子を完全identityで正式登録する処理を追加。登録前後値と根拠を保存し、既知値上書き・旧履歴昇格を拒否。旧TraceOnly条件・全体loop・旧34例の期待値を維持した。

新pure手順とmanifest1551 artifact・旧証拠324 fileをSHA付き準備しparser成立。承認済みpure40例1回は0.454秒、40一致・0不一致でPASS。旧34例は§20保存結果と名前／Actual／Expected一致、新肯定1例は検証付き登録・移行・旧履歴保持／旧子非昇格成立、負例5件は拒否と子正式台帳の非追加／非上書き・新canonical子非追加成立。比較6件中登録1件・非登録5件。

前後source／snapshot1156 file（文書4件除外）・artifact／旧証拠hash gate成立、Failure=null。外部操作attempt0、実CIM／event登録／fixture process・Java／Maven／Docker／root0回、実probe出力なし。`section22-result-preservation.json`へ証拠SHAと旧34例不変・登録件数を保全した。

準備2秒＋gate／fixture／保存5秒＋保全1秒で累計1679秒、残1921秒（32分01秒）、cleanup300秒予約。tracked code／test／POM追加変更・commit／remote0。§22範囲は完了、通常root／予定skip92は未成立、旧Rules単独107 PASSは別epoch、後続集合未実行・結果受入別判断を維持する。訂正票§23へ同runnerの実環境gate・有限28例／短命launch1回・成立時のroot追加1回と残時間内後続案を整理した。未承認・実環境操作未実行。

## 31 §23 Owner承認・実環境再開手順の準備（2026-10-10）

Ownerは訂正票§23を確認・承認した。新epoch `Start-Section23-Owner.ps1`を準備し、pure40例と現runner hash一致を固定。runner変更0、旧1551 artifact／旧証拠hash不変、新Owner手順追加でmanifest1552件、旧証拠336 fileをSHA付き保全。source／snapshot1156 file（文書4件除外）不変・parser成立。

Owner同ユーザー管理者PowerShell 7で実gate→有限28例・短命launch1回→全成立時の通常root追加1回→fresh XML／予定skip92確認→cleanup／保護Java保持を確認する。後続未実行集合はroot成立・Agent証拠確認後の既承認継続で、残予算・cleanup予約を優先する。commandと停止時再実行禁止は訂正票§23.1に記録。

準備確認3秒計上、累計1682秒・残1918秒（31分58秒）、cleanup300秒予約。Agentの実CIM／event／launch／Maven／root／container操作0、tracked code／test／POM追加変更・commit／remote0。実環境実行結果待ち、通常root成立・R0-C受入へ昇格しない。

## 32 §23 Owner実行：canonical確認済み親子のevent後着・current CIMなしで停止（2026-10-10）

gate成立、有限28例全一致、java-version probe終了0／1.695秒・subscription残0。通常rootは9.174秒、conhost.exe PID17264／parent24292の衝突移行で停止。ChildCim／ParentCimとも空だが、正式台帳と完全なcanonical親子記録は前pollで保存済み。子creation `639271682668393670`、親cmd.exe creation `639271682668336200`、観測sourceは双方`parent-chain-cim-poll`。親eventはcanonical結合済み、後着子eventでcurrent CIM必須判定が停止した。子未登録だった§21とは異なり、§22の生存CIM登録訂正の実成立は今回確認できていない。終了やPID再利用を断定しない。

停止event Classifying・後続java28580 Pendingを保存。cleanupはowned process0／container0／subscription0、保護Java24852／10280のcreation保持・source不変。4 sampleの最大JVM2／PG0／Ryuk0／client0は全期間保証ではない。fresh部分XML2 file／contract4件（failure／error／skip0）をSHA付き保存。旧Rules107 PASS・pure40 PASSは別epoch、通常root／予定skip92未成立・後続未実行。

source／snapshot1156 file（文書4件除外）・artifact1552件・旧証拠336 fileのhash不変を確認し、`section23-stop-preservation.json`へ保全。累計1700秒＋保全3秒＝1703秒、残1897秒（31分37秒）、cleanup300秒予約。停止後のtmp訂正・追加実行・tracked code／test／POM変更・commit／remote0。

訂正票§23.2へ結果、§24へ保存済みcanonical親子の後着event照合・観測順3形態＋負例7件を含む旧40＋新10＝pure50例1回の案を整理。未承認・未適用、実CIM／event／launch／root追加0回。実環境再開・成果受入は別判断。

## 33 §24 Owner承認後：pure fixtureの整数キーJSON比較で停止（2026-10-10）

Ownerは§24を確認・承認した。tmp cached移行へcanonical親子・正式creation・親event結合・一意root chain・時刻・current実値の検査を追加。旧40例の期待値不変、新10例を追加し、pure専用手順・manifest1553 artifact／旧記録361 fileをSHA付き準備。旧1552 inputは許可済みrunner更新以外hash不変、parser成立、準備2秒計上。

承認済みpure50例1回はgate成立、0.528秒で`NonStringKeyInDictionary`停止。新10例の最初の監視呼出し前、runner line504の整数PIDキーHashtableをJSON化する比較処理が失敗した。Agentのfixture不備で、cached移行の監視成否は未確認。50例JSON未保存のため今回の旧40／新10のPASS数も認定しない。旧§22 pure40 PASSは旧epochとして保持し、変更後runnerへ流用しない。

外部操作attempt0・実CIM／event／launch／Java／Maven／Docker／root0回。失敗記録・canonical／identity／transition記録・旧artifact／証拠hash不変を`section24-stop-preservation.json`へ保全。準備2秒＋gate／fixture／保存5秒＋保全2秒＝累計1712秒、残1888秒（31分28秒）、cleanup300秒予約。追加修正・実行・tracked code／test／POM変更・commit／remote0。

訂正票§24.1へ結果、§25へfixtureの台帳比較2箇所だけを直接キー／値比較へ訂正し、同じpure50例を追加1回確認する案を整理した。未承認・未適用、実環境操作／root追加0回。通常root・予定skip92・後続集合未成立、成果受入別判断を維持する。

## 34 §25 Owner承認後：fixture比較訂正・pure50例全成立（2026-10-10）

Ownerは§25を確認・承認した。新10例内の台帳比較2箇所を整数キー／値コピーと件数・キー存在・値一致の直接比較へ訂正。実監視・cached移行・root証明・50例の期待値は不変。新pure手順とmanifest1554 artifact／旧記録370 fileをSHA付き準備、旧1553 inputは許可済みrunner更新以外hash不変・parser成立。

承認済みpure追加1回は0.559秒、50一致・0不一致でPASS。旧40例は§22保存結果と名前／Actual／Expected一致。新10例の比較記録はcached移行成立3件・拒否7件で、肯定3形態の結合・旧履歴保持／旧子非昇格・台帳不変・終了UNKNOWN、負例7件の拒否・非昇格・台帳非上書きを確認。前後source／snapshot1156 file（文書4件除外）・artifact／旧証拠hash gate成立、Failure=null・外部操作attempt0。

実CIM／event／process・Java／Maven／Docker／root0回、実probe出力なし。`section25-result-preservation.json`へ証拠SHA・旧40例不変・成立／拒否件数を保全。準備2秒＋gate／fixture／保存5秒＋保全2秒＝累計1721秒、残1879秒（31分19秒）、cleanup300秒予約。tracked code／test／POM追加変更・commit／remote0。

§25範囲は完了。実root／資源保証／予定skip92は未成立、旧Rules107 PASSは別証拠、後続未実行・成果受入別判断を維持。訂正票§26へ同runnerの実環境gate・有限28例／短命launch1回・成立時のroot追加1回と残時間内後続案を整理した。未承認・未準備・実環境操作未実行。

## 35 §26 Owner承認・同種改修継続許可・実環境手順準備（2026-10-10）

Ownerは§26を確認・承認し、同種問題改修を個別確認承認なしで継続し、最終結果だけ承認すると明示した。適用範囲は本R0の同種tmp監視／pure fixture訂正・同じ検証集合の再開で、既存総予算・資源／作業量／容量・cleanup／source固定・除外対象を維持する。失敗epochは停止・保全してから有限訂正・確認と新epochへ進み、旧guardを消さない。新OS権限／provider・依存取得・Repository実装拡張・commit／remote／R1は含めない。承認sourceと適用条件は訂正票§26に記録した。

Start-Section26-Owner.ps1を新epochで準備。runnerはpure50例PASS時から変更0、旧1554 artifact／旧証拠hash不変、manifest1555件・旧証拠382 fileをSHA付き固定。source／snapshot1156 file（文書4件除外）不変・parser成立。pure50例と同runnerを実gate入力とし、Owner同ユーザー管理者PowerShell 7でgate→有限28例／短命launch→成立時root1回→fresh XML／skip92／cleanup・保護Java保持を確認する。

準備3秒計上、累計1724秒、残1876秒（31分16秒）、cleanup300秒予約。Agentの実CIM／event／launch／Maven／root／container操作0、tracked code／test／POM追加変更・commit／remote0。commandは訂正票§26.1に記録。実環境結果待ち、root／R0-C受入へ昇格しない。

## 36 §26実停止・同種継続許可によるPID世代訂正とpure58例成立（2026-10-10）

§26のrootは32.026秒でPID4580衝突停止。旧canonical creation639271694690404940／parent31396を正式PID台帳が保持し、新current CIM creation639271694805093440／parent21696がForeignObservedCimとなった。停止eventは新current recordと同signature、旧event時刻なし、停止時子／親current CIMなし。旧・新instanceの比較記録を保全し、終了や単純event重複と断定しない。cleanup残0・保護Java保持。fresh部分56件（failure／error／skip0）・source1156／artifact1555／旧証拠382 hash不変をsection26-stop-preservation.jsonへ保存、保全3秒で累計1769秒。

§26の同種継続許可に基づき、tmpで完全なCIM親子・root証明付き新instance登録と旧cache／event履歴保持、観測済みForeignObservedCimから後着eventを結合する条件を訂正。旧50例不変・新8例のpure58例を確認。section27は57一致・親矛盾fixture1不一致で停止し保全。実処理は子を非所有分類・台帳非登録としており、fixtureの例外必須期待を訂正した。実監視条件はこの再確認で変更せず、新section28で58一致・0不一致、0.709秒PASS。旧50例は§25保存結果と名前／Actual／Expected一致。外部操作attempt0・実CIM／event／launch／Java／Maven／Docker／root0、前後hash gate成立・Failure=null。

section28-result-preservation.jsonへ証拠SHA・旧50例不変を保全。section27準備／実行／保全9秒、section28準備／実行／保全9秒、累計1787秒。失敗epoch・guardは保持、tracked code／test／POM追加変更・commit／remote0。

## 37 同種継続許可内のsection29実環境再開手順（2026-10-10）

Start-Section29-Owner.ps1を準備。runnerはpure58例epochから変更0、manifest1558 artifact／旧証拠435 file固定・parser成立。Owner同ユーザー管理者PowerShell 7で実gate・有限28例／短命launch1回・全成立時root1回・fresh XML／skip92／cleanup／保護Java保持を確認する。root成立後の未実行集合は既承認範囲・残予算内で進む。新規承認は要求せず、最終成果受入はOwner判断を維持。

準備2秒で累計1789秒、残1811秒（30分11秒）、cleanup300秒予約。Agent実環境操作0。command・有限上限は訂正票§29へ記録。実root／予定skip92・成果受入は未成立、Owner実行結果待ち。

## 38 §29実停止・補助conhostの系譜保持訂正とpure66例成立（2026-10-10）

§29 rootは9.011秒、conhost PID31408／parent13444で停止。current／canonical親子とも未捕捉・正式値null。親cmdのeventは既知Java23848→root29060系譜に分類済み。別PID30812でcached移行成立記録はあるが全体PASSではない。cleanup残0・保護Java保持、fresh部分4件（failure／error／skip0）・4 sample最大JVM2、source1156／artifact1558／旧証拠435 hash不変をsection29-stop-preservation.jsonへ保全。3秒計上で累計1811秒。

§26同種継続許可でtmpに補助conhost限定TraceOnly経路を追加。新event／異signature・子CIM／正式値なし・raw時刻整合・親generationの固定rootへの連鎖・親currentがあれば完全一致を要求。正式台帳非登録・creation未確認／終了UNKNOWN、Javaへの非適用・独立CIM照合・後着CIM不一致拒否を維持する。

旧58例不変・新8例のpure66例1回は0.781秒、66一致・0不一致でPASS。旧58例は§28保存結果と名前／Actual／Expected一致、前後hash gate成立・Failure=null・外部attempt0、実CIM／event／launch／Java／Maven／Docker／root0回。manifest1559 artifact／旧証拠460 file・parser成立。section30-result-preservation.jsonへ証拠SHAと旧58例不変を保存、準備2秒／実行等6秒／保全1秒で累計1820秒。

## 39 同種継続許可内のsection31実環境手順準備（2026-10-10）

pure66例と同runner・旧証拠を固定しStart-Section31-Owner.ps1を準備。manifest1560 artifact／旧証拠472 file・parser成立。Owner管理者terminalで実gate→有限28例／短命launch1回→全成立時root1回→fresh XML／skip92／cleanup／保護Java保持を確認。root成立後の後続集合は既承認範囲・残予算内で進む。

準備2秒計上で累計1822秒、残1778秒（29分38秒）、cleanup300秒予約。Agent実環境操作0、追加tracked code／test／POM変更・commit／remote0。command／上限は訂正票§31。実root／skip92・後続未成立、Owner実行結果待ち・最終受入別判断。

## 40 §31停止・親成立後の再照合・固定root参照訂正（2026-10-10）

§31 rootは20.351秒、PID22948の新conhost正式登録不足で停止。旧javac instanceから新CIMを観測した初回は親26392未登録で拒否、その後親が登録されてもForeignObservedCim子を再照合しなかった。停止時は生存親子整合・親正式identity成立。cleanup残0／保護Java保持、fresh部分4件・10 sample最大JVM2、source1156／artifact1560／旧証拠472 hash不変を保全3秒で累計1856秒。root／skip92・後続未成立。

§26継続許可内でtmpのForeignObservedCim再照合と主pollの親成立後再照合を追加。完全なcurrent親子・新creation・root証明条件と旧cache／履歴保全を維持し、pure70例（旧66＋新4）は70一致・0不一致、0.787秒PASS。実導線点検でevent chainのroot参照を固定anchorへ統一し、実構成相当のroot Latest placeholder例を追加。pure71例は71一致・0不一致、0.837秒PASS。旧70・旧66の名前／Actual／Expected不変を確認。

新manifest1561／1562・旧証拠497／508 file・parser／前後hash gate成立、外部attempt0・実CIM／event／launch／Java／Maven／Docker／root0。section33-result-preservation.jsonへ証拠SHA・旧70不変を保全し累計1874秒。追加tracked code／test／POM変更・commit／remote0。

## 41 同種継続許可内のsection34実環境手順準備（2026-10-10）

pure71と同runnerを固定しStart-Section34-Owner.ps1を準備、manifest1563 artifact／旧証拠520 file・parser成立。Owner管理者terminalで実gate→有限28例／短命launch→全成立時root1回→fresh XML／skip92／cleanup／保護Java保持。root成立後だけ既承認後続を残予算内で進める。準備2秒で累計1876秒、残1724秒（28分44秒）、cleanup300秒予約。Agent実環境操作0・最終受入別判断。commandと上限は訂正票§34に記録。

## 42 §34 Java未確認拒否・event後fresh CIM観測訂正（2026-10-10）

§34 rootは9.007秒、Java PID28356／親cmd16848のCIM未捕捉で停止。event chainは固定root22772まで成立したが、補助conhost限定条件でJavaを拒否。未確認Javaを正式台帳へ登録したとはしない。cleanup残0・保護Java保持、fresh部分contract4件・source1156／artifact1563／旧証拠520 hash不変を保全3秒で累計1898秒。

§26継続許可でtmp観測順をevent回収→Pending保存→fresh CIM取得へ変更しmain／probe／最終drainで共通化、分類での二重追加を防止。main待機1000ms→100ms。実sample周期や短命Java捕捉は保証せず、実観測・資源上限・Java未確認拒否は維持する。停止Javaがsnapshot間隙に生まれたか終了したかは未確定。

旧71＋新3のpure74例は74一致・0不一致、0.846秒PASS。旧71の名前／Actual／Expectedは§33結果と一致。event後CIM順、CIM失敗時batch保持、event失敗時CIM非実行を確認。manifest1564 artifact／旧証拠545 file・parser／前後hash gate成立・Failure=null・外部attempt0、実環境操作0。証拠SHAと旧71不変をsection35-result-preservation.jsonへ保存、準備2秒＋実行等6秒＋保全1秒で累計1907秒。

## 43 同種継続許可内のsection36実環境手順準備（2026-10-10）

Start-Section36-Owner.ps1をpure74と同runnerで準備、manifest1565 artifact／旧証拠557 file・parser成立。Owner管理者terminalでgate→有限28例／短命launch→全成立時root1回→fresh XML／skip92／cleanup／保護Java保持を確認。root成立後だけ既承認後続を残予算内で進める。準備2秒で累計1909秒、残1691秒（28分11秒）、cleanup300秒予約。Agent実環境操作0、追加tracked code／test／POM変更・commit／remote0。上限・Java未確認拒否・最終Owner受入を維持、commandは訂正票§36。

## 44 R0最小検証集合への集中・監視拡張停止（2026-10-10）

Ownerの最新指示で、Rules107／PL2 4／package・旧Consumer／Reference E2E 1／最終root297＋skip92へ集中し、監視pure74の網羅拡張を停止。S1／B2専用92件は通常build分離確認だけ。過去監視への投資・case成功数を本体の進捗にしない。§26の同種監視改修継続許可を根拠に拡張・root反復を続けない。

§36最後の停止は9.087秒、PID2860期待cmd／parent12776（creation未確認）とcurrent conhost／parent13168の不一致。Owner cleanup残0／保護Java保持／source不変。fresh部分XML2 file／contract4件（failure／error／skip0）とartifact1565／旧証拠557 hash不変をsection36-stop-preservation.jsonへSHA付き保全、2秒計上で累計1930秒・残1670秒（27分50秒）、cleanup300秒予約。今回改修・test・実環境操作0。

訂正票の冒頭導線を最新§37へ更新し、固定集合・成果・4段階の区切り・最小実行条件・残予算・完了判定・網羅拡張の契機を整理した。順は段階0安全な実行方法選定→PL2 4→package／旧Consumer→Reference E2E 1→最終root。root未成立でも独立した成立証拠を保持し、全体PASSへ読み替えない。既知失敗runnerの再使用や監視を無効化する実行はしない。

Rules107の成立済みsource／XMLは保持、監視74は補助証拠として凍結。PL2／package／旧Consumer／E2E／root全体は未成立。段階0は実行方法未選定、次の最小準備として残す。新fixture／Owner runner／test実行／root反復・code／POM変更・commit／remote0。最終受入・R1以降は別判断を維持する。
