# Phase 4 S1 B2初回 issue／read：検証記録（2026-10-08）

状態：`BOUNDED ISSUE/READ VERIFICATION COMPLETE / OWNER ACCEPTED`。承認済み限定集合は新規54＋回帰301＝355件PASS。§30承認後の保全artifact再install・E2E1件初実行・最終auditが成立し、整合済みcacheを保持した。保存証拠と再検証のartifact epochは区別する。完了記録は§17・§18、結果受入承認は§19、総評は§20。local commit／remote、実運用・Phase 4全体開始は別判断。

## 1. 承認・source固定とpreflight

[開始票§13](../../development/phase4-s1-stage-b2-issue-read-limited-start-review-20261008.md#13-owner承認条件付き限定開始2026-10-08)に従う。Ownerが承認記録2ファイルの追加local commitを許可し、`03210795a8ffd146d0bcc10c9d1da4d62a69308c`（`docs: record approved S1 B2 conditional limited start`）へAGENTSと開始票を固定した。branchは`feature/phase4-s1-reference-foundation`、開始時の作業ツリーはclean。remote操作なし。

preflight raw：`tmp/b2-preflight-0321079-20261008/`。JDK21.0.12.1／Wrapper Maven3.9.16、Docker Server29.5.3、PG17／Ryuk／Chromium cacheを確認。観測空きmemory18,295,136,256 bytes、disk51,096,387,584 bytes、container0、Javaは既存IDE PID2660／4684だけ。必要artifact／compile／classpathをofflineで取得し、新downloadなし。

Tooling compileは`jdbc,s1-contract,s1-web`、runtime test classpathは`jdbc`単独129 JAR・Framework Starter0。Reference compileとtest classpath180 JARも成立。各classpath entryの存在とSHA-256をmanifestへ保存した。B1検証時source hash13件は現sourceと一致。raw inventoryは648ファイル／7,848,520 bytesの確認から開始した。

foundation preflightはPASS。ただしsource凍結・writer遮断の実成立をTooling18件で確認するまで、Reference肯定testへ進まない条件を保持した。

## 2. 作成・compile済み差分

Tooling補助2・SQL1・test2と検証script1を作成し、補助／testはoffline compile SUCCESS。Reference main5件とConfiguration Import1件も作成・offline compile SUCCESS。既存B1 source／SQL／assertion、既存Service／Port・Security／migration／POM／依存は変更していない。

Referenceのtest helper2・新規test4・README導線は未作成。現在のmainはcompile確認だけで、実登録・保存・認可・Audit接続のPASSではない。main／fixtureの作成は未commit差分であり、上記clean source基点と区別する。

## 3. Tooling18件のclass別結果とcleanup

raw：`tmp/b2-verification-0321079-20261008/Tooling-first/`。新しいXML、source hash、Maven exit、期待件数と各cleanupを対応付けた。

| class | 結果 | cleanup込み実経過 |
|---|---|---|
| B2SourceFreezeTest | 9 PASS、failure／error／skip0、exit0、FreshXml／SourceStable／Cleanup=true | 50.2737087秒 |
| B2FrozenSourceProtocolTest | 9 PASS、failure／error／skip0、exit0、FreshXml／SourceStable／Cleanup=true | 35.2215325秒 |

通常子の実起動→終了→凍結はF01／F07、その他の有限DB／protocol枝はtest-owned fixtureとして確認した。18件の完全性・本番停止／drain・provider未受理／再送保証を認定しない。

50 sampleのmemory最小16,786,386,944 bytes、disk最小51,068,973,056 bytes、開始前baselineを除くJava同時最大3、PG container同時最大1。heap768 MiB・DB memory1 GiB／CPU1／max_connections16等は既存B1ResourceLimitsのassertionを利用。各class後のcontainer／当該Java残留0を確認し、停止後の権限付きread-only再確認でもcontainer0・既存IDE Java2件だけだった。

compile／classpath処理の記録合計44.9645816秒、Tooling group85.515835秒、集計可能な小計130.4804166秒（約2.17分）。preflight／診断の未計測区間とOwner待ちを実測済み値へ混ぜず、開始票の暫定管理・各枠を維持し、次の実行前に再評価する。

## 4. script総件数集計の不備・停止

class別`result.json`／`results.json`とsanitized XMLは9件ずつだが、scriptの最終`summary.json`は`Status=PASS`／`Cases=null`だった。元rawは上書きしない。このsummaryを18件gate成立の正本にしない。

原因は、`$results`が`OrderedDictionary`の配列なのに、`Measure-Object -Property Cases -Sum`で集計したこと。同じ型の9＋9をread-onlyのPowerShell診断で再現するとSum=nullになった。一方、保存済みclass結果をConvertFrom-Jsonして整数として明示加算すると18となる。件数・assertion／業務sourceの失敗ではなく、scriptの集計・gate metadataの不備である。

Reference stageはCases=18のgateを要求するため、このmetadataのまま進めない。Reference肯定testと回帰301件は開始せず、原因修正・追加実行は停止した。Reference main compileは不備を検出する直前の処理で成功したが、接続検証とは扱わない。

## 5. 最小訂正review入力

[開始票§14](../../development/phase4-s1-stage-b2-issue-read-limited-start-review-20261008.md#14-script総件数集計の最小訂正案2026-10-08)へ、scriptの整数加算・期待集合検査と、元rawを保持した訂正gate生成の案を提出する。class別18件の再実行は今回の案に含めず、saved XML／exit／cleanup／source hashを再照合する。採用にはOwner判断が必要。

D11／D12の後続対象、網羅性懸念、provider UNKNOWN、同OS利用者／DB管理者・分散停止／fencing・backup／DRの保証限界を保持する。B2 mainの限定AdapterをFramework成果物へ昇格させず、結果受入・実運用・DoD／remoteへ拡張しない。

## 6. Owner承認済み最小訂正・保存証拠照合

[開始票§15](../../development/phase4-s1-stage-b2-issue-read-limited-start-review-20261008.md#15-14のowner承認と訂正gate成立2026-10-08)に承認を記録した。scriptの集計・gate読込だけを訂正し、`Tooling-first/gate-summary-corrected.json`を別生成。元summaryのSHA-256は`495FE35B9470D86A026E6A5D129FF9297F157BEC63293ED581F8A48F94ECFDCF`で生成前後同一。元summary／class rawを上書きしなかった。

保存XMLのclass名・testcase数・failure／error／skipと、class／group JSONの件数・exit・FreshXml／Cleanup／SourceStable・上限状態を照合し、18件を整数加算した。fresh／cleanupの再判定は実行時結果の照合であり、今回その実行を再現したものではない。Tooling source5件のhash不変、scriptの訂正前後hash、各入力path／hashを訂正gateへ記録した。Reference gate読込でもsource／入力hashを再確認し、nullの元summaryで肯定fallbackしない。

有限集計確認は正常9＋9と、欠落・重複・未知class・null・件数不一致・failure・error・skip・exit・fresh false・cleanup false・source false・上限失敗の13拒否枝＝14確認PASS。さらに`aggregation-ordered-input-check.json`で実行時と同じOrderedDictionary配列を18件へ集計できることを確認した。JUnit／Docker再実行なし。B2の残337件は未実行。

## 7. Reference構成登録8件と残予算

再開時の管理値は記録済み2.17467361分＋未計測確認処理5分、残82.82532639分。未計測分は実測認定ではなく保守的な管理見込み。Reference30分・回帰30分・残compile／package等8分・最終cleanup5分を確保する下方配分とした。B1の管理150分と累積240分の上限は維持する。

Reference補助2件（`B2FrozenSourceProcess`／`B2ReadConnectionHarness`）と構成登録classを作成した。Tooling型のJava参照を使わず、coordinator ready protocolとENV秘密を介する。補助の実子JVM／DB接続はまだ検証していない。

offline compile SUCCESS（10.946秒）：`tmp/b2-preflight-0321079-20261008/reference-b2-helpers-registration-compile.log`。構成登録C01〜C08は8件・failure／error／skip0・exit0・FreshXml／Cleanup／SourceStable=true、資源上限超なし、cleanup込み6.7946706秒。raw：`tmp/b2-verification-0321079-20261008/Reference-registration-first/`。DB／containerなし、Mavenとforkのみの構成検証。実保存・source freeze接続のPASSではない。

## 8. 残28件のcompile失敗・停止と訂正review

接続／issue12件、境界8件、transaction8件のclass3件を作成した。新補助の負例準備・transaction observerを含む。offline test-compileで、`B2ProtectedIssueReadTest`のI10／I11と`B2IssueTransactionTest`のX08にある`h=null`代入がNullAwayのnon-null field契約に拒否された。compile FAILURE／exit1、10.314秒。ログ：`tmp/b2-preflight-0321079-20261008/reference-b2-all-test-compile.log`。既存testの既知warning2件もログに表示されたが、既存sourceは変更していない。

当該28件はJUnit未実行。C01〜C08は先の成立済みsourceでのPASSを保持し、新補助の更新後compile成立とは区別する。現在は新規54件中26件がPASS、残28件＋回帰301件＝329件未実行。今回の追加処理28.0546706秒を前の小計へ加え、記録済み小計158.5350872秒（約2.64分）。未計測管理5分込み残約82.36分で、時間超過ではなくcompile不成立による停止である。

新main／Service／既存test／POM／Security／migration・Tooling18件のsourceを追加変更せず、compile失敗を保全して停止した。test2ファイルのnull代入を終了状態booleanへ置き換える最小訂正を[開始票§16](../../development/phase4-s1-stage-b2-issue-read-limited-start-review-20261008.md#16-reference新規testのnull代入に関する最小訂正案2026-10-08)へ提出する。訂正・再compile／未実行集合への再開はOwner判断待ち。README導線・package確認は未完了、commit／remoteなし。

## 9. §16訂正後の限定compileと既存class差分

[開始票§17](../../development/phase4-s1-stage-b2-issue-read-limited-start-review-20261008.md#17-16最小訂正のowner承認2026-10-08)にOwner承認を記録し、新規test2ファイルのnull代入3か所を終了状態booleanへ置換した。期待件数・assertion・未閉鎖permit保存確認→cleanupの順序は維持した。

offline test-compile1回：`reference-b2-null-corrected-compile.log`／`.json`（`tmp/b2-preflight-0321079-20261008/`）。exit1、Maven10.176秒／command timer11.6791891秒。新規null代入3か所の診断は消えたが、既存`ExpenseApiControllerTest.java:75`の`ExpenseRequestDetail` constructorへのnull引数がNullAwayに拒否された。既知warning2件も表示された。28件・回帰は未実行、追加compileなし。

source／bytecodeの読み取り比較は同raw内`null-corrected-bytecode-review/`へ保全した。既存record sourceと既存testはGit HEADから変更なし。sourceの`decisionReason`は`@Nullable`で、既存testはnullであることを期待している。現classのfield／accessorにはNullableがあるが、constructor引数にはない。受入済みJARではconstructorのparam_index=8にもNullableがある。

| 対象 | SHA-256／観測 |
|---|---|
| 現ExpenseRequestDetail.class | `B2F662D0201BD5A2EB5E9920513556693F6D3A18E68A1087C417144158E34470`、constructor引数注釈0 |
| B1受入JAR内の同class | `E8EE0A581AB9DE38796A0CBEEA977BD4A5E599D0818478AC79214987F26A2225`、constructor param_index=8のNullableあり |
| B1受入Reference JAR | `604412D21CB4C17F36E110EB9ED3D3FB71E586B7D4C8E2BF03E43F1CDA78CD6E`、受入時と同一 |

class生成元はUNKNOWN。IDE／compilerの原因を断定せず、既存test／main／POM／compiler設定を変更しなかった。現classと受入JAR比較・javap出力を保全し、[開始票§18](../../development/phase4-s1-stage-b2-issue-read-limited-start-review-20261008.md#18-既存classの注釈差分に対する隔離compile確認案2026-10-08)へ、同一sourceをtmpの隔離出力で1回compileする有限確認案を提出する。コピー／追加compile／runtime切替は未実施。

記録済み小計は170.2142763秒（約2.84分）、未計測管理5分込みB2残約82.16分。新規26件PASS／未実行28件＋回帰301件の状態を維持。今回Docker／coordinator／通常子は起動せず、Mavenとjavapは終了済み。commit／remoteなし。

## 10. §18承認済み隔離compile1回の結果

Ownerは既存source・設定を変更しない隔離compile1回を承認した（[開始票§19](../../development/phase4-s1-stage-b2-issue-read-limited-start-review-20261008.md#19-18隔離compile確認のowner承認2026-10-08)）。rawとコピー：`tmp/b2-isolated-compile-0321079-20261008/`。ReferenceのPOM・srcとParent POM、計205ファイルをコピーし、元／コピーのSHA-256を全件実行前後に照合した。IDE・既存target・既存source／設定は操作していない。

同じWrapper・JDK21・ParentのError Prone／NullAway条件、offline／MAVEN_OPTS=-Xmx768mでコピーPOMへ`-DskipTests test-compile`を1回実行。main131 sourceとtest52 sourceのcompile SUCCESS／exit0。Maven16.884秒、cleanup込みcommand timer18.6160021秒。追加の低い120秒監視枠内で終了。新規28件や既存testを除外せずcompileしたが、JUnitは実行していない。既知warningは保持し、compiler設定を緩めなかった。

生成recordのconstructorには`METHOD_FORMAL_PARAMETER, param_index=8`のNullableがあり、class SHA-256は`E8EE0A581AB9DE38796A0CBEEA977BD4A5E599D0818478AC79214987F26A2225`で、B1受入JAR内classと同一。元targetの当該class hash `B2F662...`と元Reference JAR hash `604412...`は前後不変。現targetの生成元はUNKNOWNのままで、IDE原因や全classの同一性を認定しない。生成artifact258件をhash台帳へ保存した。

resource sampleのmemory最小17,222,746,112 bytes、disk最小51,206,127,616 bytes、既存baselineを除くjava.exe最大1、終了後残留0。forked javacも使用したが、java.exeのsample countをjavac.exeを含む全JDK process countとは扱わない。Maven command exitとcleanupを確認した。Docker／coordinatorなし、秘密ENV追加なし、MAVEN_OPTSはfinallyで復元。

raw inventoryは明示的な各file Length加算でB2 526 files／2,344,664 bytes、B1＋B2 1,160 files／10,081,152 bytes。初期`summary.json`の配列Lengthをbytesとした暫定値は使わず、元を保持した`summary-inventory-corrected.json`を採用した。compile／source／注釈の判定は変わらない。

記録済み小計188.8302784秒（約3.15分）、未計測管理5分込みB2残約81.85分。承認はこの有限確認までなので、runtime／package・既存targetへのコピー戻し・追加compileは未実施。隔離出力を使う未実行28件＋回帰301件の実行導線候補を[開始票§20](../../development/phase4-s1-stage-b2-issue-read-limited-start-review-20261008.md#20-隔離reference出力を使う残集合の実行導線案2026-10-08)へ記録した。新規26件PASS／残329件未実行、結果受入・commit／remoteなし。

## 11. §20承認済み実行導線とI12初回失敗の保全

Ownerの実行導線採用・残集合再開承認を[開始票§21](../../development/phase4-s1-stage-b2-issue-read-limited-start-review-20261008.md#21-20実行導線採用残集合再開のowner承認2026-10-08)へ記録した。新規scriptのReference stageを隔離POM／reportsとI12／B8／X8に向け、Tooling gateの歴史script hashと新承認script hashを別台帳で対応付けた。保存C8 result／XML・構成sourceも照合し、acceptance補助2件はC8で使用しなかった後続作成差分として区別した。コピー205件と生成artifact258件は一致した。

実行導線raw：`tmp/b2-runtime-route-0321079-20261008/`（旧script・前後hash・approval）。I12 raw：`tmp/b2-verification-0321079-20261008/Reference-isolated-first/B2ProtectedIssueReadTest/`。12件・failure1／error2／skip0、exit1、FreshXml／SourceStable／Cleanup=true、cleanup込み164.7967734秒。上限超なし。class後に停止し、B8／X8・回帰301件・packageは開始しなかった。

| case | 保存XMLの結果とsource照合 |
|---|---|
| I01 realSaveAuditAndScopedRead | permit保存1件／発行id readのtarget一致のassertion後、Audit検索件数0対期待1でfailure。新規testがaction名PERMIT_ISSUEDをevent_type列へ指定していた。既存Serviceはevent_type=NOTIFICATION_RECOVERY_CHANGE、action=PERMIT_ISSUEDを記録する |
| I10 providerUnknownRejects | 負例fixture構築時の`Protocol column invalid`でerror。UNKNOWN拒否のassertionには未到達 |
| I11 providerAcceptedRejects | 同じ負例fixture構築error。ACCEPTED拒否のassertionには未到達 |
| I02〜I09、I12 | 9件PASS。未接続／認可／期限・不一致・接続不可の有限枝を示す部分結果であり、I12 class gateのPASSへ昇格させない |

負例fixtureのmetadata識別子検査は`[a-z_]+`で、現SQLの`jar_sha256`等、数字付き列名を拒否する。失敗rawは列名そのものを出していないが、SQL定義の順ではjar_sha256が最初の該当列。型・role／grant・protocol定義を変える必要はなく、new helper検査の前提誤りである。

保存sourceとの限定照合で未実行B06／B08も点検した。reason `"bad reason space"`は既存Serviceのblank／長さ／control検査に該当しないため、これをrollback／失敗入力にする前提は誤り。これら2件はまだ実行しておらず、失敗実証済みとは扱わない。新しい業務制約を追加せず、既存のblank拒否入力へ合わせる案とした。

resource sampleはmemory最小16,299,532,288 bytes、disk最小50,910,248,960 bytes、新java最大4、PG最大1。class後残留0／source・artifact不変を確認した。current source保護／scopeの実接続は観測したが、Audit照合とUNKNOWN／ACCEPTED枝は完了していない。sanitized XML／log・source hash・resource／resultを保持し、修正・再実行なし。

記録済み353.6270518秒（約5.89分）、未計測管理5分込みB2残約79.11分。集合gate成立はTooling18＋C8＝26件のまま。I12の9件PASSは部分結果として別記し、B8／X8・回帰301件は未実行。Java3ファイル＋scriptの固定隔離path更新、compile1回とI12のclass再検証1回を[開始票§22](../../development/phase4-s1-stage-b2-issue-read-limited-start-review-20261008.md#22-新規testのaudit照合負例fixture入力の限定訂正案2026-10-08)へ提出した。Owner判断待ち、commit／remoteなし。

## 12. §22訂正・compile／I12限定再検証と新規54件成立

Owner承認を[開始票§23](../../development/phase4-s1-stage-b2-issue-read-limited-start-review-20261008.md#23-22限定訂正compilei12再検証のowner承認2026-10-08)へ反映した。新規Java3件を訂正し、scriptの固定隔離root／POM literal2か所をr2へ変更した。旧script・§21 approvalは`tmp/b2-runtime-route-r2-0321079-20261008/`へ同hashで保全し、現在gateが参照するapproval台帳はrevision23へ更新した。元Tooling訂正gate・旧隔離出力・失敗class rawは保持した。

`tmp/b2-isolated-compile-r2-0321079-20261008/`へ205ファイルをコピー。旧版からの元Java差分は承認3件だけ、旧コピー自体は不変。offline test-compile1回がSUCCESS／exit0、Maven16.761秒・command timer18.7598059秒、source／copy不変・cleanup true・上限超なし。main131／test52を同じcompiler条件でcompileした。生成artifact258件の旧版からのhash差分も当該test／helperのclass3件だけ。constructor Nullable成立、元targetのrecord classと元JARは不変。

raw：`tmp/b2-verification-0321079-20261008/Reference-isolated-r2/`。I12は追加のclass attempt1回、B8／X8は初実行。各classともfailure／error／skip0・exit0・FreshXml／SourceStable／Cleanup=true、上限超なし。

| class | 件数 | cleanup込み秒 |
|---|---|---|
| B2ProtectedIssueReadTest | 12 PASS | 166.1209297 |
| B2IssueBoundaryTest | 8 PASS | 105.7019355 |
| B2IssueTransactionTest | 8 PASS | 105.9125196 |

I01は発行id／targetのreadとBusiness Auditのtype／action／actor／resource／resultを完全照合した。I10／I11は負例protocolのUNKNOWN／ACCEPTED拒否まで到達し、permit0を確認。B05／B06は実transactionのcommit／rollback completionでscope保持、X01はAudit INSERT REVOKEによるrollbackとfinally GRANT復元、X02は同publication競合で1件だけの保存、X04／X05はconsume／closeの未接続拒否を確認した。X07は実commit後のtest-owned response lossと結果照会／再試行なしの有限枝で、全commit障害を再現したとはしない。

Tooling18＋保存C8＋r2の28＝新規54件の集合gateが成立。I12初回12件は失敗attemptとして別保管し、今回の12件を新しいcase追加／coverage拡張へ数えない。9件の初回部分PASSと二重加算しない。追加反復なし。

記録済み小計750.1883496秒（約12.50分、r2 group377.8014919秒を含む）、未計測管理5分込みB2残約72.50分。回帰30分・残package／raw6分・最終cleanup5分を確保し、301件を開始した。実行script／集合台帳は`tmp/b2-runtime-route-r2-0321079-20261008/Run-Regression301.ps1`・`regression-preflight.json`。元Tooling58件、隔離Reference関連143件・baseline99件、隔離JARを指定する既存E2E1件に固定する。関連集合へpool4／idle1を供給せず、baseline99件にだけB1受入時の供給を維持する。

## 13. 回帰94件PASSとlog処理停止

raw：`tmp/b2-verification-0321079-20261008/Regression-r2-first/`。B1 T10／P8／E10／R8／S8／I8＝52件、PublicationRecoveryTest4＋選択ProcessCrashRecoveryIT2＝6件、Reference関連のTarget10／Stop8／Provider8／Store10＝36件、計12 class／94件がPASS。全classで期待件数・failure／error／skip0・exit0・fresh XML／source不変・cleanup成立、上限超なし。

続くOperationalRecoveryEvidenceDbTestはMaven stdout1,531,095 bytes／stderr0で、回帰scriptの入力1 MiB制限がlog処理前に停止した。保存XMLは12件・failure／error／skip0、Maven logはBUILD SUCCESS／19.311秒を記録。既存Audit権限拒否などの期待例外を含む正常logを収容する見積りが不足していた。Agent scriptの前提誤りで、既存test／mainを修正する根拠にはしない。

停止位置はprocess終了・cleanup待機・resources保存後、exit／XML／sourceをclass resultへ保存する前。exitの変数を永続化していなかったため、class gateをPASSへ補完しない。Sanitized XML観測を`OperationalRecoveryEvidenceDbTest/junit-sanitized-observation.xml`へ別保全し、元log／XMLを消していない。後続95件・baseline99件・package／E2Eは未実行。

停止後の権限付きread-only確認でsource／artifact843 hash差分0、container0、Javaは既存IDE PID4684／2660だけだった（`log-stop-observation.json`）。Maven child／当該DBの残留はない。未知process／IDEを停止せず、追加compile／test／cleanup操作なし。

回帰経過317.4429951秒を30分枠へ算入。記録済みB2小計1067.6313447秒（約17.79分）、未計測管理5分込み残約67.21分。continue枠24.71分、残package／raw5分と最終cleanup5分を保持する。有限stream log処理・exit先行保存と、当該class12件の1回再検証＋未実行195件の案を[開始票§24](../../development/phase4-s1-stage-b2-issue-read-limited-start-review-20261008.md#24-回帰scriptの有限log処理exit先行保存の限定訂正案2026-10-08)へ提出した。Owner判断待ち、source訂正・追加実行・commit／remoteなし。

## 14. §24承認済み訂正成立・回帰280件PASSと既存Security resource差分停止

Owner承認は[開始票§25](../../development/phase4-s1-stage-b2-issue-read-limited-start-review-20261008.md#25-24有限log処理exit先行保存のowner承認2026-10-08)。旧script／rawを保持し、端末内`tmp/b2-runtime-route-r2-0321079-20261008/Run-Regression301-Continue.ps1`と`continuation-approval.json`を追加した。変更はexit先行保存・各入力16 MiB監視・streamによる各1 MiB以内のredacted要約、保存94件の照合／再利用、回帰残1482.5570049秒の継承だけ。既存main／test／設定／POM／compiler条件は変更していない。

保存94件のclass result／sanitized XML・source／artifact・cleanupを照合し、再実行しなかった。新rawは`tmp/b2-verification-0321079-20261008/Regression-r2-continue/`。OperationalRecoveryEvidenceDbTest12件のclass再検証1回はPASS／failure・error・skip0／exit0／fresh XML／source不変／cleanup成立、32.2435964秒。exitはWaitForExit直後21.8334395秒の時点で`process-exit.json`へ保存した。元stdout1,531,096 bytesを保持し、redacted要約34,131 bytesを別保存した。§24の原因修正・再検証は成立し、追加反復なし。

続く関連95件とbaseline先行24 class／79件もPASS。保存分を含めてB1 52＋選択PL2 6＋Reference関連143＋baseline79＝**回帰280件のclass gate成立**。新規54件は再実行していない。合計334件の成立集合と、失敗class内の部分結果を区別する。

最後の`ReferenceBusinessUrlSecurityTest`は20件／1 failure／error0／skip0／exit1、fresh XML／source不変／cleanup成立、上限超なし、10.6522013秒。失敗methodは`servesHtmxIntegrationResourcesWithoutAuthentication`。HTTP200／JavaScript content typeと先行HTMX文字列検査の後、`responseUrl.endsWith("/login")`の文字列が応答になく失敗した。残19件の部分PASSを20件class gateへ昇格しない。package／E2Eは開始せず、追加実行・修正なし。全exit／command／時刻・result／sanitized XML・有限logを保存した。

### 14.1 source／既存artifactの有限read-only比較

`security-resource-stop-observation.json`と`web-mvc-payload-comparison.json`へ保存した。continue source／生成artifact・旧保存証拠1032 hash entryは差分0、Reference依存classpath180 JARもpreflightから差分0。これは開始後の未知変更がない確認であり、B1受入artifactとの一致証明ではない。

| 比較対象 | SHA-256／観測 |
|---|---|
| 解決されたローカル`koiki-starter-web-mvc:0.1.0-SNAPSHOT` JAR | `BDB24A1BCE9832FF9D0980A0F027C875EF9B2B2FDC5F221468A9293C70392B2A` |
| 元Reference受入JAR内の同座標nested JAR | `3C5C1A74DB64321069C190CA1233B2CBC8FB069E5A7B4960FF81860563F483DE` |
| ローカルcacheの`koiki-htmx.js` | `B7432253150408067CE1C8AD0A9FA12711FA97148ECABCD6E40A29E71B82D383`、login branchなし。失敗XMLの応答内容にこの全文が一致 |
| 受入nested JARのJSと現Repository source | ともに`C81A3764D4B98F755E049F64DA0E5C5C72124E2A1558129B3B9E561CD8331653`、login branchあり |

両JARの非directory entry各10件を有限streamで比較し、内容差分は4件：`META-INF/resources/koiki-web/koiki-htmx.js`、同`koiki.css`、`META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`、`templates/koiki/fragments.html`。単独JS差分と扱わない。現JS source／既存testを変更してPASSさせる根拠ではなく、既存依存artifactと受入artifactの不一致が実行に現れた。cacheの生成元・更新履歴はUNKNOWN。preflightのcache存在／hash固定だけでは受入artifactとの一致確認が不足していた。

### 14.2 停止後cleanup・上限と次の判断

read-only再確認でcontainer0、Javaは既存IDE PID2660／4684だけ。当該Maven／DB残留なし。未知process・IDEを停止せず、cache差替・Framework build／install・元target置換も行っていない。端末内`Final-Audit.ps1`は完了時の確認用に準備したが、301件gate未成立のため未実行。JAR非混入・新main包含の最終判定は未完了。

continue563.8875631秒、旧回帰317.4429951秒と合計881.3305582秒（約14.69分）、回帰残918.6694418秒（約15.31分）。B2記録済み1631.5189078秒、未計測管理5分込み残約57.81分。停止後点検／Owner待ちを実測済み検証時間へ混ぜず、残回帰を30分へリセットしない。raw inventoryはtmpのB2 25,786,954 bytes／B1＋B2 33,523,442 bytes、別途Tooling targetの有限sanitized coordinator log合計143,766 bytes。後続観測JSON・文書を含む小増分があっても上限を拡張しない。

新規54件・回帰280件の証拠と失敗attemptを保持し、依存artifact整合・影響する保存結果の再利用可否・再検証集合／残予算を[開始票§26](../../development/phase4-s1-stage-b2-issue-read-limited-start-review-20261008.md#26-既存web-mvc-artifact差分による停止と次review入力2026-10-08)へ整理した。原因修正・class再検証は別判断。Framework／Security sourceや既存assertionを変更せず、B2完了・結果受入・commit／remote・実運用・DoDを認定しない。

## 15. artifact整合・再検証範囲のread-only review

Ownerのreview続行指示を受け、既存artifact・POM・source／test・cache済みinstall pluginの記述を読み取った。build／install・cache書換え・追加testは未実施。rawは`Regression-r2-continue/artifact-review-observation.json`。

### 15.1 4 resource差分の精査

受入nested JARのresource4件はすべて現Repository sourceとbyte hash一致。cacheと受入のCSS／imports／HTML fragmentはCRLF→LF正規化で全文一致し、実内容差分はJSに限定できた。importsはいずれも`org.koikifw.starter.webmvc.internal.KoikiWebMvcAutoConfiguration`。前§14の4 byte差分記録は正しく、3件の改行差を今回さらに区別した。全class3件・manifest・埋込みPOM／pom.propertiesはcacheと受入で同hash、各非directory entryは10件。cached POM／現module POMも`EDFFBE294C96FFCC625747AC29BF99D937A2BFF6797775299B5A55AA8B0A4812`で一致する。当該Framework module／ParentのGit差分なし。

Reference依存180件・Tooling runtime129件を再照合し、preflightからhash差分0。Tooling runtimeにWeb MVC Starter0件。Foundation Db Harnessは明示`--spring.main.web-application-type=none`、B2 C8はFoundation明示構成のApplicationContextRunnerを使用する。他方、既存NotificationFoundationRegistrationTest7件とMigrationTest5件は通常Servlet起動を含む。baseline99件はWeb／Securityを含む集合として全件を再確認する案とした。

### 15.2 候補と未実施境界

[開始票§27](../../development/phase4-s1-stage-b2-issue-read-limited-start-review-20261008.md#27-artifact整合方法保存結果の扱い限定再検証review案2026-10-08)へ、Framework module／Parentのsource8 fileコピーと標準隔離package1回、受入nested JARとの全10 entry一致gate、旧cache全5 file保全、単一既存座標の標準install-file1回、成功時cache保持／失敗時既知差分の限定復元を提案した。既存source／POM／通常設定・元targetの修正、nested JARの直接cacheコピー、Root Reactor／他module install・remoteは含めない。

保存新規54件＋回帰189件を非影響範囲の旧artifact証拠として再利用し、通常起動12件＋baseline99件＋E2E1＝112件を新artifactで確認する候補。189＋112＝回帰301、新規54を加え355件。各artifact epochを分け、全355件を同一classpathで再実行したと説明しない。entry／他179依存／Tooling129／source／生成artifactの不変条件が崩れたら再利用せず停止する。

直近111件373.358798秒＋過去B1 E2E30.35秒＝約6.73分は見積り入力に留める。112件を最大12分、整合build／install・Reference package／raw合計5分、最終cleanup5分を下方予約する案。回帰既消費約14.69分と合わせ最大約26.69分、30分へリセットしない。B2残約57.81分の管理値・未計測5分・全資源／raw／再実行停止条件を維持する。予算増ではなく具体artifact整合／限定再検証のOwner判断待ち。現在は新規54／回帰280成立、Security20失敗・package／E2E未完了のまま。

## 16. §27承認済みartifact整合・111件PASSとE2E command組立停止

Ownerの採用承認を[開始票§28](../../development/phase4-s1-stage-b2-issue-read-limited-start-review-20261008.md#28-27-artifact整合112件限定確認のowner承認2026-10-08)へ記録した。端末内`Run-Artifact-Aligned.ps1`・`Aligned-Bounded-Functions.ps1`・`Cache-Restore.ps1`とapproval hash台帳を作成。既存Repository code／POM／source copyは変更していない。compilerの資源上限としてCLIの`-Dmaven.compiler.maxmem=768m`を明示し、Maven・test JVM heap768 MiBとjava.exe／javac.exe両方のprocess countを監視した。ParentのError Prone／NullAway等の検査は維持した。

### 16.1 整合・保存結果照合と実行結果

`tmp/b2-artifact-alignment-0321079-20261008/`へFramework module＋Parentの8 fileをコピー／hash固定。隔離package1回はPASS／exit0／source不変／cleanup成立、5.7786208秒。生成JAR hashは`34754252439BB8B5C446122A0F173B15B139731DF80A50D77C2F96B750F66FC1`、7,472 bytes。受入nested JARとはZIP全体hashが異なるが、非directory entry集合10件と全内容hashは一致した。`payload-comparison.json`に保存し、受入JARを直接cacheへコピーしていない。

旧cache5 fileをpath／hash付きで保全し、標準install-file1回がPASS／exit0／cleanup成立、2.8901813秒。installed JARと生成JAR同hash、POM不変、当該metadataの変更一覧を保存した。他Reference179 JAR・Tooling129 JARは不変。旧classpath manifestを保持し、`reference-classpath-after.json`を別作成した。保存新規54件・回帰189件の各result／XML・source条件を照合し、再実行しなかった。

raw：`tmp/b2-verification-0321079-20261008/Regression-artifact-aligned/`。Security20件・registration7件・migration5件を先行し、baseline残79件まで計27 class／111件が全PASS。failure／error／skip0、exit0、fresh XML／source不変／cleanup成立、上限超なし。旧Security failureは原本を保持し、追加のclass attempt1回だけで全20件成立。回帰は保存189＋今回111＝300件、新規54を加え354件が成立。旧artifactの91件を新artifact結果へ置き換え、二重加算していない。

隔離Reference package1回もPASS、5.5161596秒。JAR hash `5F78A9C4C88ED1E6B05222B8F607F7F0637D13210FB566E9AFD5B8555DE0876D`、64,525,393 bytes。nested Web MVC Starterは上記生成JARのhashと一致した。

### 16.2 E2E起動前のscript式不備とcache復元

E2E commandの`$flags -replace <pattern>, <文字列>+$e2eDirectory+<文字列>`で、PowerShellがreplacementの連結を単一の右operandとして扱わず「後続の要素2つのみ、4つではありません」と停止した。Agent runnerの式の不備であり、E2E／Reference testの失敗ではない。E2E process／raw directoryは作成されておらず、E2E attempt0。式のreplacementを括弧で包んだ有限read-only評価は文字列生成が成立し、`e2e-command-readonly-check.json`へ保存した。Maven／E2Eは実行していない。原因修正は別reviewとし、旧runnerを上書きしなかった。

catchの承認済み限定復元は成立。cacheのpost-install全5 fileが既知hashのままであることを照合し、before backupを復元した。`cache-restore.json`にRESTORED／5 fileを記録。停止後確認でもbefore hashとの差分0、cache JARは元`BDB24A...`に戻った。整合済み生成JAR・Reference JARと全PASS証拠は保持している。cache整合を現在も保持しているとは説明しない。

### 16.3 停止後の有限package／source／cleanup確認と残予算

`e2e-command-stop-observation.json`へ保存した。復元対象cacheを除くsource／artifact／保存証拠1160 entryのhash差分0、cache復元5 fileの差分0。Reference JARの新main5件はすべて包含、Tooling型／B2 acceptance helper／test SQL等の禁止entry0。Tooling JARもB1／B2 helper／SQL／test entry0、元hash`E72019...`不変。元Reference受入JARは`604412...`で不変。package確認はE2E前に成立し、最終class literal／全cleanup auditはE2E未実行のため完了判定に使わない。

container0、Java／javacは既存IDE PID2660／4684だけ。当該JVM／DB残留なし。112件枠のうち111件のgroup388.5566294秒を消費、既回帰881.3305582秒と合計1269.8871876秒。回帰30分残530.1128124秒（約8.84分）、§27の下方12分枠残331.4433706秒（約5.52分）を区別する。整合／package command計14.1888298秒、prep等を含むgroup外は17.9277332秒。今回実行全406.4843626秒、B2記録済み2038.0032704秒・未計測管理5分込み残約51.03分。read-only点検／Owner待ちを検証実測へ混ぜず、管理仮定と既枠を維持する。

停止時tmp inventoryはB2 93,968,547 bytes、B1＋B2 101,705,035 bytes。新JARと旧cache backupを含み、raw256 MiB／全1 GiB以内。既存有限coordinator logは別途143,766 bytesで、加算しても上限内。追加観測JSON／文書の小増分は次preflightで再照合する。

[開始票§29](../../development/phase4-s1-stage-b2-issue-read-limited-start-review-20261008.md#29-e2e-command式の限定訂正保全artifact再installと残1件確認案2026-10-08)へ、括弧だけのcommand訂正・保全生成JARの標準再install1回・既存保全Reference JARを使うE2E1件初実行の案を提出する。compile／package／111件反復は追加しない。B2完了／受入・commit／remoteは未実施。

## 17. §30承認済み再install・E2E初実行と最終audit（2026-10-08）

[開始票§30](../../development/phase4-s1-stage-b2-issue-read-limited-start-review-20261008.md#30-29-command訂正cache再installe2e初実行のowner承認2026-10-08)に基づき、新しい端末内`Run-E2E-Finish.ps1`でreplacement連結の括弧を訂正した。旧runner／失敗rawを保全。compile／package／成立済み354件のJUnit再実行を追加していない。各保存result／sanitized XMLの件数・exit・fresh XML・source・cleanupを再照合し、新規54、非影響回帰189、整合済み27 class／111件を確認した。

### 17.1 artifact・実行と集合

rawは`tmp/b2-verification-0321079-20261008/E2E-finish/`、cache台帳／backupは`tmp/b2-e2e-finish-0321079-20261008/`。開始時cache全5 fileが旧before manifestと一致し、保全Framework生成JAR／Reference JARは§16のhashのまま。受入との全10 entry内容一致条件とsource／他依存不変条件を保持した。

保全生成JARの標準install-file追加1回はPASS、exit0、3.2681504秒。installされたWeb MVC JARは`34754252439BB8B5C446122A0F173B15B139731DF80A50D77C2F96B750F66FC1`。cache全5 fileのbefore／after・backupを別保存し、全条件成立のためcacheを保持した。今回の再installは§28のinstallと区別し、Framework／Reference再packageはしていない。

`PackagedReferenceCriticalJourneyTest`初実行1回は1 PASS、failure／error／skip0、exit0、fresh XML／SourceStable／Cleanup=true、上限超なし。command、開始終了時刻、exitをlog処理より先に保存した。class実経過31.3136298秒、cleanup／source照合込みE2E group32.1142059秒。実行commandの`user.dir`は隔離出力配下のE2E位置を指し、保全Reference JAR `5F78A9C4C88ED1E6B05222B8F607F7F0637D13210FB566E9AFD5B8555DE0876D`を起動する。既存E2EのBearer／Session／browser／Audit／DB突合・application／issuer port解放assertionを変更していない。

| 集合 | 件数・保存証拠の扱い |
|---|---|
| B2新規 | Tooling18＋Reference登録8＋I12／B8／X8の28＝54 PASS、保存証拠を照合 |
| 非影響回帰 | Tooling58＋Reference関連131＝189 PASS、旧artifact下の保存証拠と非影響条件を照合 |
| 整合済み回帰 | baseline99＋registration7＋migration5＝111 PASS、同hashのartifact下の保存証拠を照合 |
| packaged E2E | 1 PASS、今回初実行 |
| 合計 | 新規54＋回帰301＝355 PASS |

全355件を今回同一classpathで再実行したとは説明しない。旧artifactの91件は整合済み111件側の結果で置き換え、失敗attempt・旧部分PASSを加算しない。集合の完全性・正式採用を認定しない。

### 17.2 最終audit・上限とcleanup

`tmp/b2-verification-0321079-20261008/Final-audit-finish/summary.json`はPASS。source／生成artifact／保存証拠1286 hash entryに差分なし、Reference原本／コピー205 file一致。Reference JARに意図したmain5型を包含し、Reference／Tooling JARのfixture・helper・test SQL禁止entry0。新main5型の有限class literal確認でUUID／fixture path marker0。ENV key名は設定入力として意図的に含むが、実行時の秘密値はpackage入力にしない。

元Reference受入JAR `604412...`・元ExpenseRequestDetail.class `B2F662...`・Tooling JAR `E72019...`は不変。元targetのNullable差分の生成元UNKNOWNは解消したとしない。container0、当該java／javac0、既存IDE PID2660／4684のみ。ready protocol一時file0、親processの検証ENV残留0。E2E内で当該application／issuerの終了とport解放が成立した。

全採用run sampleのmemory最小16,144,211,968 bytes、disk最小50,725,650,432 bytes、baseline除外JDK最大4、PG最大1。B2 raw95,165,631 bytes、B1＋B2 raw102,902,119 bytes、別保全coordinator log143,766 bytesを加算しても256 MiB／1 GiB以内。上記inventoryはaudit時点で、今回小容量の記録JSON／文書増分を含む将来の再実行前には再確認する。

今回全実行37.4037733秒、group外5.2895674秒でE2E3分／再install・raw2分以内。最終audit1.0117081秒でcleanup予約5分以内。回帰累積1302.0013935秒（約21.70分）、30分残約8.30分。§27の12分下方枠は111件＋E2Eで420.6708353秒、残約4.99分。B2既知実経過2076.4187518秒（約34.61分）、未計測管理5分を維持した90分残は約50.39分。B1の保守的管理150分と合わせ全240分枠を広げていない。Owner待ち／未計測作業のブレと作業量管理の仮定を保持し、厳密な累積実時間の確定値とはしない。

## 18. B2限定検証の完了記録・受入境界（2026-10-08）

承認済み初回issue／read集合54＋301件とpackage／artifact整合・cleanupの確認は完遂した。状態は`BOUNDED ISSUE/READ VERIFICATION COMPLETE / OWNER REVIEW PENDING`。Ownerの結果受入は未記録。追加検証、local commit／remoteは行っていない。

成立範囲はTooling test所有の有限凍結sourceとReference-owned保護scopeでの初回issue transaction・SELECT読取接続・登録／認可／Audit。通常設定は無効を維持し、肯定consume／close証拠接続・provider UNKNOWNの肯定化は含まない。permit発行を送信完了・再送許可とみなさない。

D11の歴史対象閉鎖／別publicationにまたがる抑止、D12の未保護競合観測と後続の網羅性懸念を保持する。実運用停止／drain・分散fencing・管理者遮断・provider通信／delivery・backup／DR、Reference Level 2／DoD、Framework API／Rules／正式配布、Phase 4全体開始はこの結果の保証・承認に含まれない。

保存証拠の再見はread-onlyで可能。再実行は新しいraw、source／環境／artifact／cache hash、有限集合・予算・cleanupのpreflightと新たな限定実行判断を要する。端末内runner・既存tmp出力への依存をfresh cloneで即再現できる構成とは説明しない。

## 19. B2限定検証結果のOwner受入承認（2026-10-08）

Ownerは§18を確認し、「結果受け入れを承認します」と明示した。承認済みB2初回issue／readの限定検証結果54＋301＝355件、artifact整合・package内容・cleanupの完了と、§18の保証範囲／未達を含め受け入れた。現在の状態は`BOUNDED ISSUE/READ VERIFICATION COMPLETE / OWNER ACCEPTED`。

§18の`OWNER REVIEW PENDING`は受入前の履歴として保持する。今回の承認は結果受入であり、追加検証・local commit／remote、肯定consume／close接続・実運用・Reference Level 2／DoD・Phase 4全体の開始承認へ広げない。

## 20. B2検証全般の総評（2026-10-08）

技術目的の達成は良好。B1の読取接続に対し、B2ではtest-owned凍結sourceをReference-owned保護scopeへ接続し、初回issue transactionのcommit／rollbackまで保護を保持する経路を実証した。permit保存・id／target読取・認可・Business Audit突合に加え、UNKNOWN／ACCEPTED拒否、Audit保存失敗時rollback、同publicationの競合で1件だけ保存、未接続consume／close拒否を確認した。肯定経路と拒否／失敗経路を有限集合で確認した点に価値がある。

既存機能保護も成立。回帰301件とpackage済みE2Eにより選択Tooling・Reference関連・baseline・Bearer／Session／browserの影響を確認し、既存assertion／Security／migration／POMを弱めなかった。Reference mainとTooling fixtureを分離し、JARへのfixture非混入・元target／受入JAR不変・資源上限／cleanupを最終確認した。これは有限集合での回帰成立であり、全障害・全利用環境を保証しない。

検証遂行の効率には改善余地がある。新規testのnull代入、Audit type／action照合、metadata列名／負例入力の前提、集計／log処理・exit保存、E2E command式にAgent側の不備があり、停止・review・訂正を要した。これらは検証実装の品質上の課題として扱う。業務sourceや既存assertionの変更で吸収せず、失敗原本を保全し、承認済み訂正・限定再検証で成立させた。

環境再現性について重要な課題を発見した。現targetのconstructor Nullableと受入classの不一致、および同一SNAPSHOT座標のcacheと受入nested JARのresource不一致があった。clean Git／artifact存在／開始前後hash固定だけでは受入状態との同一性が不足する。隔離同一source compileと全entry内容比較・標準installで今回の検証経路を整合させたが、元target／旧cacheの生成履歴UNKNOWNは解消していない。

後続検証の準備では、採用artifact manifestと実解決classpath・package内依存の照合、compile出力の隔離、実行前のtest期待値／command文字列確認、exit先行保存・有限log処理を入口の条件として整える価値がある。今回の総評は改善候補の提示であり、新しいscript機能・CI・配布・追加実行を採用／実施した記録ではない。

総合判断は、B2初回issue／readの承認範囲は完遂し、Owner受入済み。後続reviewへの証拠は揃った。運用保証・肯定consume／close・D11歴史対象／cross-publication抑止・D12後続競合／網羅性・provider deliveryは残課題として別に判断する。保存証拠の再見は可能だが、端末内tmp／classpath依存のためfresh cloneで即時に同じ実行が成立する保証はなく、再実行は改めてpreflightと有限実行条件を確認する。

## 21. 受入成果の固定・引継ぎ準備（2026-10-08）

Ownerの成果固定・引継ぎ整理の指示を受け、[次作業引継ぎ](../../development/phase4-s1-stage-b2-fixed-results-next-session-handoff-20261008.md)と[固定manifest](phase4-s1-stage-b2-fixed-evidence-manifest-20261008.json)を作成した。受入source18 file・artifact4件・保持cache5 fileと、最終runのsource／artifact／保存証拠1286 entryをread-only再照合し、差分0。manifestには証拠index266件の所在／bytes／SHA-256と最終auditの必要項目を保持する。raw本文／credential／JAR本体は埋め込まない。

code／test／SQL／script18 fileと、受入・引継ぎ文書等6 fileの2コミット案を具体化した。現HEADは`0321079`のまま、local commit操作前のOwner確認待ち。Maven／Docker／JUnit再実行・cache書換え・原本削除・外部保存はしていない。現端末のfile保全とGit source固定、別媒体backupは別状態として記録する。

## 22. 2コミットによるlocal固定のOwner承認（2026-10-08）

Ownerは「この2コミットのローカル操作を承認します。コミット進めてください」と明示した。第1コミットは`d70b883a1edface5f524272e564f715afb1bab25`、Reference／Tooling実装・検証18 fileだけを固定した。第2コミットは本Evidence・開始票・引継ぎ・manifest・AGENTS・READMEの6 fileを対象とする。本引継ぎを含む文書commitを次のsource基点とし、実際のcommit IDとclean statusを操作終了時に確認する。受入結果・保存raw／artifactのhashに差分なし。remote操作・追加検証なし。
