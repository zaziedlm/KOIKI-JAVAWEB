# Phase 4 S1 R3-A：全停止型の契約と最小開始条件（2026-10-13）

状態：**FULL-STOP MODEL / MINIMAL SCOPE OWNER ADOPTED、§5.1実証方式OWNER APPROVED、実行条件は具体化中**。[Owner判断](phase4-s1-r3-purpose-recovery-model-and-minimal-verification-owner-review-20261013.md#7-ownerによるr3-a全停止型の採用と契約具体化指示2026-10-13)はR3-Aの全停止型採用と契約具体化・最小開始を指示し、後続の[§8](#8-実証方式のowner承認記録2026-10-13)で故障位置の実証方式を承認した。本票は残る実行条件をreview可能にする。§5.1以外の候補数値・path・実行条件は個別に確認するまで承認済み実行値としない。実装・Maven／Docker検証、branch／commit／pushは未開始。

## 1 対象と保証境界

入力はR2の受入済み固定source `66d9b8fb686405148269aa6eefed4819b8c53370`、Reference R1のasync有効時publication保存・通知key、R2の失敗分離である。目的は、**隔離したpackage済みReferenceの1件の未完了publication**について、元processの全停止、provider stub受理状態、対象snapshotを照合し、同じevent／通知keyで復旧すること。R3-Aの成功枝はE04・E05・E06、拒否枝は生存中の元process・古いsnapshot・別publication、回帰は通常起動保護と既存通知契約に限定する。

この「全停止」はR3-Aの**今回所有する隔離実験環境**で、通知を処理し得るReference通常processの全数を把握し、元process終了から復旧結果の固定まで新しい通常processを起動しないことを意味する。実運用の全instance停止手順やWeb停止許容時間を承認した意味ではない。S1の正式なDoD 4-2・4-3判定には、運用担当／代行、停止範囲・影響、許容時間、真正な停止証拠と正式入口を別reviewする。

本票はReference-owned復旧処理とTooling-owned故障・観測を分ける。認可・Audit・permit consume／closeが未接続の隔離検証用入口を、通常起動や正式運用から到達可能にしない。実provider、無停止個別復旧、R3-BのE07／E09／E10・E08残枝、R4／R5、Framework API、CI、正式受渡し、Phase 4全体は対象外。

## 2 停止契約 S：元実行の終了と再開禁止

| ID | 開始前・停止時の必要条件 | 実証／不成立時 |
|---|---|---|
| S1 所有 | Toolingが起動したReference子processのPIDと`startInstant`、固定JAR hash、DB container ID、server port、対象event／publicationをrun固有の台帳へ結ぶ。開始時に既存Java／containerを別に記録 | 他sessionのprocess／containerへ停止操作しない。所有・世代が一致しなければHOLD |
| S2 全数 | 隔離環境に対象を処理できる通常Referenceが1 processだけであること、起動時自動再公開=false、monitor／別runner／別operatorが対象を再送しないことを設定と実体で確認 | 追加instance・隠れた再公開入口・再起動制御不明なら復旧を開始しない |
| S3 故障位置 | A01はPUBLISHEDでlistener送信前、A02はPROCESSINGでstub到着前、A03はstubの別transaction受理後かつpublication完了前を、DB・Tooling gate・stub台帳の独立観測で固定する | `completion_attempts`やstatusだけを送信前／停止位置の証拠としない。決定的なgateをToolingに置けなければそのcaseはBLOCKEDで停止 |
| S4 実停止 | A01〜A03の故障位置では、今回所有の当該Process objectのPID／`startInstant`を直前に再照合し、gate保持中に`destroyForcibly()`で直ちに停止する。5秒以内の終了・終了コード、`ProcessHandle.isAlive=false`、port応答終了を観測する。通常cleanupだけは正常終了を最大30秒待ち、未終了時に同じ所有processを限定forceして追加5秒確認する。今回所有の関連通常process0、未処理taskの他instance移譲なしを確認 | PID再利用・process生存・停止結果不明・再配置可能ならHOLD。確認file単独、lock取得、DBのFAILED状態を停止証明にしない |
| S5 維持／再開 | 元通常process終了後に初めて専用復旧processを起動し、対象の終端照合・復旧process終了・lock解放まで通常起動を禁止。異常終了・lock喪失・timeout時は新しいsnapshotとprovider照合まで追加再送しない | 許可外の通常起動・制御断・復旧worker残存は直ちにSTOP。対象のCOMPLETED・受理を確認しcleanupした後だけ隔離runを閉じる |

S1〜S5はToolingが自身の子processを観測する隔離実証契約である。正式環境の停止証拠発行元・改変耐性・有効期限・運用者の本人確認は別判断。PL2のstop-confirmation fileやB2の全停止結果を、今回のprocessの停止証明としてコピーしない。

## 3 provider契約 P：到着・受理・UNKNOWN

| ID | R3-Aに必要な契約 | 判定 |
|---|---|---|
| P1 識別 | `ExpenseApprovedEvent`のevent ID、申請ID、承認version、宛先種別から初回と復旧で同一の通知key・canonical payload・SHA-256を導出し、stub台帳で別々に観測する | retry／process IDから新keyを作らない。同key異内容は409拒否し、成功へ読み替えない |
| P2 独立記録 | loopback stubは要求到着と受理receiptを別transaction／台帳で記録し、受理後に元Referenceを停止してもreceiptを保持する。今回のfresh DB run内でkey・payloadに対し1つのreceiptを維持する | ReferenceのHTTP応答・notification log・publication statusだけをprovider受理証拠としない。到着0と受理0は観測時点の値であり、停止前の未送信証明と組み合わせる |
| P3 重複 | 同じkey／同じpayloadの再要求には元receiptを返し、新規の論理受理を作らない。A03では停止前の受理1と再要求後の受理総数1、同じreceiptを確認する | 2件目の受理、別key、payload変更、receipt不一致ならFAIL |
| P4 不明 | 受理照会不能、台帳矛盾、key対応不明、同key安全再要求の契約不成立はUNKNOWNとしてHOLDし、追加送信せず対象・理由・次の照合先を残す | DBがPROCESSING／FAILED、HTTP timeout、Reference process死亡だけで未受理としない。R3-Bの応答喪失・照会不能は今回PASSへ加算しない |

R3-Aのstub保証は今回のfresh DBが生きている間の同key契約だけ。keyの運用保持期間、実provider照会の遅延・認証・外部受理保証はここで固定しない。`HttpNotificationProviderAdapter`はloopback限定を維持し、実endpointやcredentialを追加しない。

## 4 対象選別契約 T：1 publicationだけを明示再公開

| ID | 選別・実行前照合 | 拒否条件 |
|---|---|---|
| T1 tuple | Reference-owned `kkref_notification_async.event_publication`から、環境、publication ID、event ID、listener ID、event type、status、`completion_attempts`、通知key／payload hashを対象1行に結ぶ。ID／試行回数は実観測値を使い固定値を仮定しない | 行0／複数、COMPLETED、event／listener／payload不一致、別環境・別対象は拒否 |
| T2 選別 | `IncompleteEventPublications`の対象限定predicateを候補とし、通常の一括再公開・起動時自動再公開・staleness monitorを利用しない。DBを手動UPDATEしてCOMPLETEDへ進めない | 採用APIが1行だけを選べず、別publicationを変更するならSTOPして方式を再review |
| T3 再照合 | 専用復旧processが単一実行lockを得た後に、T1のstatus／試行回数とS4停止、P1〜P4を再読取し、同じtupleだけをpredicateへ渡す。復旧終了までlock接続を保持し、その喪失は結果不明として止める | 生存中の通常listener、停止証拠なし、古いsnapshot、lock競合／喪失、provider不明時は送信しない |
| T4 結果 | targetだけがCOMPLETED、同じevent／keyのstub論理受理総数1、承認状態・成功Audit不変、別publicationのstatus／試行回数不変をfresh DBで突合する | predicate呼出し成功だけで配信完了としない。timeout／異常終了は再送0回でHOLD・証拠保全 |

既存V5 publication schemaの読取はReference内の狭いadapter候補として扱い、Modulith内部tableをFramework Public APIや他moduleの一般契約へ昇格しない。対象tupleに必要な情報が現行API／schemaから安全に得られない場合は、SQL・migration・依存を推測で追加せず、差分と回帰をOwnerへ戻す。専用lockは復旧process同士だけを排他し、S2／S4が通常listenerを止める条件を代替しない。

## 5 最小開始候補の差分・検証集合

実装責務の候補は、Reference `notification`内の明示opt-in単発復旧入口、対象読取／再照合と限定再公開、通常起動時の未登録・拒否である。Tooling `build-support/reference-e2e-verification/`内に、今回所有のprocess世代・故障位置gate・provider台帳観測・fresh隔離DB・cleanupを置く。productionへtest用の停止switch・任意の再送Web／CLI公開入口を置かない。R3-Aは既存V5とModulith runtimeを使用し、新しいmigration／POM／Framework依存を初回案に含めない。必要が判明したら作成前に停止して対象を見直す。

| 順 | 明示実行候補 | 最低限の確認 |
|---|---|---|
| 1〜3 | A01 PUBLISHED送信前、A02 PROCESSING到着前、A03受理直後の3件 | それぞれ実停止・同じ対象のCOMPLETED・論理受理1。A03は元receipt再利用。停止位置と時間を独立記録 |
| 4〜7 | A04a 元listener生存／停止未確認、A04b古いstatus／試行回数、A04c別publication、A04d専用復旧processのlock競合の4件 | 拒否された復旧processからのprovider要求0、対象外のstatus／試行回数不変。競合者はlistenerへ入らない。生存中の元listenerや先行復旧者自身の送信と区別 |
| 8〜10 | 既存C02 `PackagedReferenceAsyncNotificationTest#asyncArtifactDisabledPreservesApprovalWithoutPublications`、D01 `NotificationProviderContractTest#reusesTheOriginalReceiptForIdenticalRequests`、C04 `PackagedReferenceAsyncNotificationTest#correlatesTwoIndependentApprovalNotificationsToCompletedPublications`の3件 | 通常起動の非接続、stub契約、既存正常経路。R2／R1の過去XMLを今回のPASSに転記しない |
| 条件付き最終 | Reference通常rootを1回 | Reference production差分に対する既存保護。S1／B2専用skipを実行件数に加えず、fresh XML全件・failure／error0を確認 |

上記は**選択10 invocation＋通常root最大1回**の開始候補。case名・assertion、決定的故障位置gateの装着方法、Reference単発入口とmodulith predicateの実効動作をsourceで設計確認してから固定する。特にA01をHTTP応答直後の競争的process killで代用しない。A02／A03のstub gateは送信前／受理後を別観測し、既存stubの通常受理契約を維持する。Tooling-onlyの追加故障制御をpackage済みReferenceへ安全に装着できない場合は、productionに故障switchを足して通さずBLOCKEDで停止する。

### 5.1 A01故障位置gateの静的実現性点検（未実証）

同じ固定sourceのReference async設定は1-threadの`ThreadPoolTaskExecutor`を自前で作り、package済みJARは`JarLauncher`で起動する。PL2のPUBLISHED caseに用いたfixture executorを、Referenceの`java -jar`へ外部classpathだけで差し替えられるとは扱わない。provider stubだけで待たせると既に送信開始後であり、A01の「送信前」を証明できない。

採用候補は、**fresh隔離DBにToolingだけが設置する、対象publicationのPUBLISHED→PROCESSING更新を待たせるtrigger／DB lock gate**である。手元のModulith 2.1.1 runtimeの`CompletionRegisteringMethodInterceptor`では、listener methodの`proceed()`前に状態遷移を呼ぶことを静的に確認した。Toolingが承認要求前にgateを準備し、対象の更新がDB lock待ちに入ったことをDB session／lock観測で特定してから、対象がPUBLISHED、stub到着0・受理0、元process世代を固定して停止する。元processのDB transactionが終了し、gateを解除しても送信が始まらずPUBLISHEDに残ることを再観測してから、復旧caseへ進む。triggerは今回所有のfresh DBだけに置き、Reference V5 migrationや通常構成へ持ち込まない。

これは**静的な候補**であり、status更新とlistener本体の実行順、対象1行への限定、DB transaction rollback、lock待ち観測、停止時の接続解放をpackage済みReferenceで1件確認するまでA01成立としない。更新がgateへ到達しない、先にstubへ要求が届く、他publicationを止める、停止後にPUBLISHEDが残らない場合はA01をBLOCKEDとして後段を開始しない。gateの成立確認はA01の初回試行に含め、失敗を別方式で再試行してPASSへ置き換えない。

A02はToolingのloopback中継でstub転送前に要求を保持し、PROCESSINGとstub到着0を独立照合する候補。A03はstubが独立transactionでreceiptを確定した後に応答だけを保持し、PROCESSINGと同じreceiptを観測する候補。ReferenceのHTTP要求timeoutは5秒なので、gate到達・必要な独立観測からS4の直ちの強制停止までを5秒未満で完了する。経過時間と停止直前の状態を保存し、先にHTTP timeout／listener終端へ進んだ場合はcaseをBLOCKEDとする。両gateとも到着・受理の語義を分け、通常のstub受理処理と同key再要求契約を変えない。

## 6 提案する有限実行条件と順序

**数値はOwnerレビュー候補で、旧R2実行権限や残予算の流用ではない。** 新規90分、うちcleanup10分予約。source／環境preflight10分、Reference／Tooling作成・対象compile25分、通常／async package・内容照合10分、選択10件25分、条件付き通常root10分、cleanup10分の上限案。文書作成・Owner待ちは実行予算外とする。各case／rootは初回1回、追加再実行0回。途中FAIL／BLOCKEDを後行の成功で覆わない。

全phaseで今回所有JVM4以内、使い捨てPostgreSQL1、Maven本体／compiler／test fork／Reference／復旧子JVMのheap各768 MiB以内。選択caseはRyuk1・DB client8以内、通常rootはRyuk2・client14以内。PG memory1 GiB／CPU1／max_connections16、開始前空きmemory8 GiB・disk10 GiB、raw100 MiB／run2 GiB／子log10 MiBを候補上限とする。起動120秒、listener／復旧終端待ち30秒、故障位置での所有子process強制終了5秒、通常cleanupの正常終了待ち30秒＋限定force後5秒。有限sampleを連続上限の証明としない。Chrome不要のためbrowserは起動・取得しない。

順序は①本票を含む必要文書の固定とclean source、R3 branch／snapshotの同一性確認、②JDK21／Wrapper、依存・Docker Server／image・空き資源・保護対象process、隔離DB・stubのpreflight、③T1〜T3と故障gateのcompile／静的確認、④通常／async JARを別hashで固定しmain class／runtime依存対照、⑤選択10件を表順に各fresh XML・log・DB／provider／所有資源のcleanupを保全、⑥成立時だけ通常root1回、⑦source／artifact hash・結果・stop／cleanupをEvidenceへ記録、とする。preflightだけの成立を復旧実証PASSにしない。

対象停止・provider受理・選別tupleが曖昧、通常listener生存、別process再起動、lock喪失、復旧入口の通常露出、同key異内容、対象外publication変更、予算／資源超過、秘密露出、所有資源cleanup不成立で即時停止し、新しい再実行・修正範囲はOwnerへ戻す。Docker／Rancher再起動、依存取得、他session process／container操作、PR／merge／workflow dispatchは含めない。

## 7 開始判断と残る確認

Ownerが全停止型と最小開始を指示したことは[方針票§7](phase4-s1-r3-purpose-recovery-model-and-minimal-verification-owner-review-20261013.md#7-ownerによるr3-a全停止型の採用と契約具体化指示2026-10-13)に記録済み。本票のS／P／T契約、選択10件・root最大1回、90分と資源上限、Reference単発入口、source固定・preflight・停止条件を**具体的な限定作成／検証開始条件案**としてOwnerへ提示する。§5.1のTooling故障gateの実証方式は[§8](#8-実証方式のowner承認記録2026-10-13)で承認済みだが、A01 DB gateの成立は静的候補にとどまり、限定実動作での確認を最初の停止点とする。数値・残るpath・実行条件の最終採用、文書commit、R3 branch作成はまだ判断待ち。条件が固まるまでcode／test／SQL／POMの作成・検証へ進めない。

R3-A結果の受入は別判断。E07のReference通知log保存境界、E09／E10のkey期限切れ・応答喪失／照会不能、E08並行枝、実運用の停止証拠／Web影響と許容時間、R4の認可・Audit／permit、R5の保持・実traceを未達として保持する。

## 8 実証方式のOwner承認記録（2026-10-13）

Ownerは本票§5.1の具体化を確認し、「その実証方式を承認いたします」と判断した。採用する実証方式は、A01でfresh隔離DBのTooling専用trigger／DB lock gateにより対象publicationのPUBLISHED→PROCESSING遷移を送信前に待たせ、A02ではloopback中継のstub転送前、A03ではstubの独立receipt確定後・応答前にそれぞれ保持する方式である。Reference productionへの故障switch、通常構成へのtrigger、PL2 fixture executorの流用は含めない。

この承認は**方式の採用**であり、A01〜A03の実動作成立やR3-A結果の受入ではない。package済みReferenceで停止位置・DB rollback・stub到着／受理・対象限定を独立照合し、§5.1のBLOCKED条件に従う。選択10件・通常root・90分・資源上限、Reference単発復旧入口、文書commit、R3 branchとsource固定、preflight後の作成／検証開始は本判断だけでは確定しない。次に残る開始条件を固定してから実行へ進む。

## 9 残る実行条件と文書固定の判断入力（2026-10-13）

§5.1承認後の開始条件を、次の**一体の限定判断案**として整理する。これはOwner判断前の提示であり、§8の方式承認から自動成立しない。

| 判断項目 | 固定候補 |
|---|---|
| 文書source | [R3目的・方式・最小範囲票](phase4-s1-r3-purpose-recovery-model-and-minimal-verification-owner-review-20261013.md)と本票の2件だけをR2 branch上でlocal commitし、commit hashと差分2件を照合する。R2成果commit `66d9b8fb686405148269aa6eefed4819b8c53370`を祖先とする。未追跡・変更済みの別fileがあれば固定を止める |
| R3 source | 文書commitから新branch `feature/phase4-s1-reference-full-stop-recovery-r3a`を作り、clean source／HEADとbranchの一致を記録する。R2 branchのPR／merge、remote pushは含めない |
| Reference差分 | `koiki-reference-app`の`notification`内に限り、対象1件の読取・照合、単発再公開、専用process用の明示opt-in入口を作る。通常起動で入口は未登録、隔離runでは非Web起動・async有効・自動再公開無効・loopback provider・今回所有DBを必須とする。正式なoperator認可／Audit／permit入口として公開しない |
| Tooling差分 | `build-support/reference-e2e-verification/`内に所有子process／世代、fresh DBのA01 gate、loopbackのA02／A03 gate、停止・provider・DB照合とcleanupを置く。A01のtrigger／functionはfresh隔離DBだけに作成し、V5 migrationへ追加しない。Framework／Customer／POM／依存／既存migrationは初回対象外 |
| 選択検証 | 新規7 method（A01、A02、A03、A04a〜d）と§5の既存C02／D01／C04を各1回、直列で計10 invocation。A01で決定的停止位置が成立しなければBLOCKEDとしてA02以降へ進まない。Reference production差分がある場合だけ通常rootを最後に最大1回 |
| 有限条件 | §6の新規90分（cleanup10分予約）、今回所有JVM4・PG1、選択caseのRyuk1／DB client8、通常rootのRyuk2／DB client14、heap／memory／disk／log上限と各timeoutを採用する。再実行0回。故障caseの元processは§2 S4の直ちの強制停止を使い、通常cleanupの30秒待ちと混同しない |
| 開始・停止 | 文書commit→R3 branchのclean source→JDK／Wrapper・依存／Docker Server・image／空き資源／保護processのpreflight→対象実装・compile／JAR固定→A01から選択検証→成立時の通常root→Evidence・所有資源cleanup。preflight不成立、入口露出、対象外差分、時間・資源超過、故障位置不成立、provider UNKNOWN、lock喪失、cleanup不成立ならSTOPし、結果・差分をOwnerへ戻す |

新規method名は実装前に`PackagedReferenceAsyncRecoveryTest`内のA01〜A04dへ1対1で割り当て、選択実行リストとXMLを照合する。Reference単発入口とModulith 2.1.1のpredicateが対象1件に限定できるかは作成前のsource確認とA01の実動作で判定する。predicateが別publicationを動かす、または入口を非Web・通常無効にできない場合は方式変更として停止する。90分は停止を伴う上限であり、10件完遂の所要時間を保証する値ではない。

本判断案が採用されても、文書2件のlocal commitとR3 branch作成、source／環境preflightを順に証拠化する。preflightの成立前にcode／test／SQL／POMを作成しない。文書固定だけではR3-Aの実証PASSやR3全体・DoDの受入としない。
