# Phase 4 S1 R3-B：source固定・blocking静的点検・preflight（2026-10-14）

状態：**R3-B LIMITED RESULTS / OWNER ACCEPTED（§23）**。B03a自動完了FAIL・E08／DoD未達を保持する。以下の§1〜22はsource固定から通常root・受入判断入力までのepoch別履歴であり、各節の「未実施」「受入待ち」はその時点の記録として読む。現行の受入境界は§23に従う。

## 1. 文書source・branch

R3-A受入済み成果`c8d406b713fed97269ca5843ecf31b79824593ad`を祖先とし、統合点検案、R3-B契約レビュー、R3-B開始票の**3文書だけ**を`fcc515cd266b0297af11db4cb6e8a783c5ce23fa`（`docs: fix R3-B contract and conditional start scope`）でlocal commitした。stage対象は3文書、`git diff --cached --check`指摘0件。commit後のworking treeはclean。そのcommitから`feature/phase4-s1-reference-unknown-recovery-r3b`を作成し、HEADは`fcc515c`。R3-Aのremote branchは`c8d406b`のままで、今回pushしていない。source差分`c8d406b..fcc515c`も上記3文書のみで、production／test／POM／migrationはまだ不変更。

## 2. blocking静的点検

採用済みSpring Modulith **2.1.1**のlocal `spring-modulith-events-api`／`spring-modulith-events-core` JARを`javap`で読み取った。`ApplicationModuleListener`には`@Async`、`@Transactional(REQUIRES_NEW)`、`@TransactionalEventListener`があり、`CompletionRegisteringMethodInterceptor`はlistener `MethodInvocation.proceed()`の正常返却後に状態遷移を登録し、失敗時はpublicationを未完了に残す経路を持つ。Referenceのlistenerはeventを受け、R3-A単発復旧は対象publication IDだけをpredicateへ渡す。R3-A JDBC Adapterは行のeventを復号し、event ID・key・payload hashを選択snapshotと照合する。

これにより、provider照会が受理一致を返す場合、listener本体でPOSTせず正常返却させる**候補経路は静的に存在**する。logはlistener transactionから独立commitさせ、publication完了より前に耐久保存する方式が必要。adviceとtransaction commitの厳密なDB順序、E07の停止位置、照会後無POSTでの実際のCOMPLETED、logの一意性は未証明。これらをpackage済み実動作で確認できなければBLOCKEDとし、直接DB UPDATEやtest期待値緩和で代用しない。

## 3. fresh環境preflight

| 項目 | 読取結果 | 判定 |
|---|---|---|
| Java／Wrapper | Temurin JDK 21.0.12、Maven Wrapper 3.9.16。Wrapper配布物はlocal cacheに存在し、権限付き`mvnw.cmd -v`で起動成功 | 条件成立 |
| Docker | Server 29.1.3応答。`docker ps -q`は0件。`postgres:17`・`testcontainers/ryuk:0.14.0` imageを確認 | 条件成立 |
| host／Docker資源 | host空きmemory 15,775,580 KiB、C空きdisk 534,897,651,712 bytes。Docker総memory 16,646,848,512 bytes、CPU24 | 開始floor 8 GiB／10 GiB以上 |
| 保護process | VS Code拡張由来のJava PID 23472／24668／25088を読取確認。今回所有JVMはpreflight時0 | 触らず保護 |
| Git | branch・HEAD `feature/phase4-s1-reference-unknown-recovery-r3b`／`fcc515c`、working tree clean。R3-A受入成果からの追加は文書3件のみ | 条件成立 |

通常sandboxでの`mvnw.cmd -v`はMaven配布物cacheへのアクセスが制限され、Wrapperがdownloadを試みて`Permission denied: connect`となった。Docker named pipeとWMIにもsandboxのアクセス拒否が出た。AGENTS.mdの承認済み手順どおり、同じ読み取り診断を実行環境の権限付き手順で再試行し、上表の成功値を得た。権限不足をMaven／Docker Engine障害とは判定しない。依存の新規取得、image pull、container起動、Maven compile／testは行っていない。**transitive依存のoffline解決は初回compile前には未証明**であり、そこで不足が判明した場合は取得を自動実施せずSTOPする。

## 4. 次の実行境界

ここから開始票のReference／Tooling・新migration候補の限定作成と、契約3件・package7件・直接回帰4件・通常root最大1回の順序へ進める。最初のcode作成前に、Source `fcc515c`／保護process／空き資源・Docker container0の差分がないことを再確認する。E07停止位置・log独立commit、期限内同key契約、対象だけの再公開、照会後無POSTが実動作で成立しない場合は本書へSTOPを記録し、追加修正・再実行をOwner判断に戻す。過去R3-A結果やtmp artifactを新sourceのPASSに数えない。

## 5. 限定source・初回compileと契約テスト起動停止（2026-10-14）

開始票のReference `notification`／Tooling `reference-e2e-verification`内に、受理IDと結果分類のprovider Port、Reference-owned受理観測log／V6新migration、loopback照会、単発復旧の照会前判定・HOLD、Tooling側のE07完了直前gateとE09／E10用stub契約・選択7件、既存A03の無追加POST期待値を作成した。正常経路の同期承認、Framework／Security／通常properties、既存V4／V5、POM／CIは変更していない。未commitのcode／test／migrationは20 file以内、文書は本Evidence 1 file。`git diff --check`は指摘0件。

Reference reactor `-o -pl koiki-reference-app -am -DskipTests clean test-compile`を`MAVEN_OPTS=-Xmx768m`で**初回1回**実行し、13 moduleすべて`BUILD SUCCESS`（14:47:36 JST）。Reference main 149 source、test 54 sourceをcompileした。既存`JdbcProtectedRecoveryTargetAdapter`のReferenceEquality warning 2件と、新規テストのloopbackアドレス指定に関するAddressSelection warning 2件が出た。後者のtest sourceだけを`InetAddress.getByAddress`へ置き換えたため、**現行test sourceの再compileは未実施**。Tooling test-compile、通常／async package、Docker／選択case、通常rootは未実施。

次の契約テスト選択2件を起動したが、PowerShellに渡した未引用の`-Dsurefire.failIfNoSpecifiedTests=false`が分割され、Mavenが`Unknown lifecycle phase ".failIfNoSpecifiedTests=false"`で**test実行前にBUILD FAILURE**（14:48:07 JST）。Surefire XMLは生成されず、実行テスト0件、Docker起動0。これは契約assertionのFAILではなくCLI引数構築の失敗だが、開始票の「各集合初回1回・追加再実行0回」を緩めない。契約テスト再起動・Tooling compile／package／E07以降・通常rootを**STOP**した。R3-Aの旧PASSを今回の結果に加算しない。

次の限定判断候補は、PowerShellで`'-Dsurefire.failIfNoSpecifiedTests=false'`と他の`-D`引数を明示引用した**契約選択2件の新規1 invocationだけ**を認めるかどうかである。Sourceと選択method、JVM／DB上限を変えず、実行前にbranch／差分・空き資源を確認する。そこで不成立なら再び停止し、packageや後段へ自動進行しない。後段の再開範囲もOwner判断で明示する。

## 6. 契約2件の個別再開承認と結果（2026-10-14）

Ownerは§5のCLI引数失敗を踏まえ、**同じ選択2件の新規1回だけ**を明示承認した。後段のTooling／package検証は別判断とされた。実行直前のbranch／HEADは`feature/phase4-s1-reference-unknown-recovery-r3b`／`fcc515cd266b0297af11db4cb6e8a783c5ce23fa`、`git diff --check`指摘0、host空きmemory 14,680,752 KiB、C空き534,972,424,192 bytes、Docker稼働container0だった。前回のMaven引数失敗と別epochであり、前回をPASSへ上書きしない。

PowerShellの`-D`引数を引用したoffline Reference reactorの選択test 1 invocationは、`NotificationProviderEndpointContractTest`の新規2 methodを**tests 2／failures 0／errors 0／skipped 0、BUILD SUCCESS**（14:50:12 JST）で終了した。現行test sourceの修正分もこの実行中に再compileされ、追加のAddressSelection warningは出ていない。Surefireの`Corrupted channel` warningは採用JDKのCDS warningがforkのnative streamへ出たことを示すdumpstream 1件で、XMLのtest失敗ではない。JVM／Dockerの実動作caseはこの実行に含まない。

証拠は`tmp/r3b-contract-20261014/`にXML、txt、dumpstreamをコピーして保全した。SHA-256はXML `51EA6D53DD0C86F7E40406537336B5E13CB10166E04BF20E63B0044CCABB5760`、txt `8578216D769E393BD36568359B390D342857BA998621B048F43372FCC6E34B15`、dumpstream `3EE4FA49973CB76F62593C1491384C30F4B90F0BE5F9DE4FD7937AC4FD78E2BF`。`tmp`はGit移送対象外である。

契約2件の成立はReference-owned観測logの独立commit、E07故障位置、HOLD、復旧完了や通常構成保護をまだ証明しない。**後段はHOLD**。次は開始票の範囲内で、Tooling offline test-compile、通常／async packageの各1回、artifact対照、E07を先頭とするpackage済み選択7件・既存4件・条件付き通常rootへ進めるか、その順序と上限をOwnerが判断する。実動作で停止条件が出たらその地点で止め、追加実行しない。

## 7. Tooling compile・通常／async JAR対照の限定結果（2026-10-14）

Ownerは§6の後段判断として、**Tooling offline test-compile 1回、同一sourceから通常／async JARのoffline package各1回、JAR内容・runtime依存対照まで**を承認した。E07以降と通常rootは別判断とした。Tooling初回`test-compile`はtest 15 sourceをcompileし`BUILD SUCCESS`（14:51:59 JST）。既存のAddressSelection warning 2件、新規`R3ReceiptCompletionGate`未使用field warning 1件があり、compiler error 0。未使用fieldは機能判定に影響せず、この時点で追加source訂正・compile再実行はしていない。

通常構成のoffline packageは`maven.jar.forceCreation=true`で13 moduleすべて`BUILD SUCCESS`（14:53:30 JST）。生成JARを`tmp/r3b-artifacts-20261014/reference-normal.jar`にコピーしSHA-256 `462E9B663E0852E5FBF6F7E8E2C3FA4BAC9FAD31DDFFB78C76C18ADA2BA2C623`で固定した。続く`s1-reference-async` profileのoffline packageも同条件で13 moduleすべて`BUILD SUCCESS`（14:54:21 JST）。`reference-async.jar`のSHA-256は`7A09B3F5B242477D31FDFFC4D8894CBD7A4C059C0D0BB4BAAB3A72F95F937B34`。両方ともtest実行をskipし、通常rootは実行していない。

JARをentry単位で照合すると`BOOT-INF/classes/`は通常／asyncとも**199 entry、path差0・内容hash差0**。nested runtime libraryは通常117／async127、通常のみ0、asyncのみModulith関連等10件。共通libの外側byte hash差9件は再packageしたKOIKI JARで、各nested JARのentryを再照合すると9件ともpath・内容hash差0。したがって同一app内容・同一共通runtime内容に、async profile固有10依存が追加された対照結果である。外側JARのhash一致は要件にしていない。V6 migrationはapp entryとして通常JARにも格納されるが、通常のFlyway location／async beanはopt-in設定でのみ有効となる設計であり、**通常起動の実動作保護は未検証**。

Tooling compileとJARの静的対照は、log独立commit、provider受理後の完了・HOLDや停止位置の実動作証拠ではない。B01の実行前静的レビューで、初回Tooling sourceは**別publicationの不変照合が欠落**していた。開始票のE07条件を満たすため、B01 testに同じ元process内の別対象を追加し、選択対象だけを復旧した後に別対象snapshotが不変であるassertionを加えた。Tooling gateの未使用fieldも除去した。Reference source／固定JARは不変更だが、**現行Tooling sourceは上記compile後の新epochであり未compile**。初回Tooling compile・固定JAR対をこのtest訂正のPASSへ流用しない。

次の最小判断候補は、このTooling-only訂正差分を点検し、offline test-compileを**新規1回**、成立時に固定async JARと新test-classesで**B01 E07だけを新規1 invocation**実行すること。受理観測log・provider受理・publication未完了の同時点、元process停止後の対象／別対象照合、照会後無追加POSTと完了を確認する。直前のbranch／source差分・artifact hash・資源／Docker・保護processを再確認し、JVM4／PG1／Ryuk1／DB client8、heap768 MiB、今回の開始票150分とcleanup10分予約を越えない。compile／B01不成立ならE09／E10・直接回帰・通常rootを開始せず、その観測地点でSTOPする。

## 8. B01 E07の個別承認と実動作結果（2026-10-14）

Ownerは§7のTooling-only訂正後offline test-compile**新規1回**と、成立時の固定async JARによる**B01新規1 invocationだけ**を承認した。上限は30分以内・cleanup5分予約、開始票のJVM4／PG1／Ryuk1／DB client8、各heap768 MiBを維持し、失敗時は後続を止める条件だった。実行直前はbranch／HEAD `feature/phase4-s1-reference-unknown-recovery-r3b`／`fcc515c`、`git diff --check`指摘0、固定通常JAR hash `462E9B...2C623`・async JAR hash `7A09B3...7B34`が§7と一致した。host空きmemory 14,692,628 KiB、C空きdisk 534,774,583,296 bytes、Docker稼働container0、保護Java PID23472／24668／25088を確認した。Toolingが指定する`postgres:17-alpine` imageもlocalに存在した。

訂正後Toolingのoffline `test-compile`は**15 test source／BUILD SUCCESS**（15:01:22 JST）。既存AddressSelection warning 2件、compiler error0。固定async JAR hashは起動時Tooling出力でも`7a09b3f5b242477d31fdffc4d8894cbd7a4c059c0d0bb4baab3a72f95f937b34`と一致した。B01だけのSurefire実行は**tests1／failures0／errors0／skipped0、BUILD SUCCESS**（15:02:23 JST、test17.456秒）。E07 gateのassertionは、provider受理1とReference受理観測log行が独立接続から可視で、対象completion更新がadvisory lock待機中・未完了という同時点を確認する。元Reference process停止後、単発復旧子は指定publicationを完了し、provider到着・受理の総数は各1のまま。別publicationのsnapshotは復旧前後で不変だった。復旧子logにも指定publicationの完了記録がある。これは**選択1ケースの証拠**であり、一般的な並行運用・実provider配信や他故障枝へ拡張しない。

Tooling内のDB client観測は3で上限8以下。終了後`docker ps -q`は0件、今回所有の元Reference PID10428と復旧子PID43276は存在せず、保護PID23472／24668／25088は継続した。Surefire forkの`Corrupted channel` warningはJDK CDS warningがnative streamに出たdumpstream 1件で、XML上のtest失敗ではない。`tmp/r3b-b01-20261014/`にXML・txt・dumpstream・redacted子logとownershipをコピーした。SHA-256はXML `8E3D93A0E9524D35517F73520517D4C2C7219E6CE84B5E03AF252E0DB10F6416`、元子log `0904DE936384E0D33AFA1BE1A6873E03DEFD3E609A54FB59C384A41AC9972BF8`、復旧子log `A500AF6F3D0EFDC695F8A4B9FB1F238959DB130A9D4DF05790627844C3D72E99`。`tmp`はGit移送対象外である。

承認範囲はB01までなので、E09／E10・直接回帰・通常rootは開始しない。次の判断前にB02a〜cのHOLD理由と、**B02cが「期限後の過去receiptを消さない」条件を実際に照合しているか**を静的点検する。現行B02cは未受理targetの期限切れとHOLDを確認する設計で、過去receipt保全そのもののassertionはまだない。必要ならTooling-only訂正・新source epoch／compile回数を別判断し、未検証のE09全体をPASS扱いしない。

## 9. E09の実行前静的点検と次回判断入力（2026-10-14）

追加実行せず、承認済み契約と現行B02a〜c／loopback stubを照合した。B02aは登録key削除、B02bは同keyのhash改変、B02cは未受理targetの期限切れを各fresh DBで作り、いずれも復旧送信増分0・HOLDを検査する構成である。ただし、B02a／bの復旧子理由codeは両方`CONFLICT`であり、Toolingの注入位置とcase名を見ないと欠落と異内容を識別できない。またstubの期限判定は現在`Instant.now()`を読むため、§2の「検証用固定時刻」は未実装である。B02cは過去受理行が存在しないままHOLDするため、期限後の過去receipt保全は未検証。**このまま3件を実行してもE09全契約の成立とは判定できない。**

第一候補はTooling-onlyで`LocalNotificationProviderStub`の検証時刻をfixture内に固定し、B02cに別keyのHTTP受理済みrowと期限切れ契約を用意して、対象keyの期限切れHOLD後も過去受理IDが独立台帳に残ることを照合する訂正である。対象はTooling stubとB02 testの2 file、新しいTooling source epoch／offline test-compile1回、成立時B02a〜c各fresh1回と有限cleanup。固定Reference通常／async JAR、Port／migration、B01結果のepochは変更しない。B02a／bの復旧子理由`CONFLICT`を用途上の共通codeとして採用するか、別codeを要求してReference Port／Adapter・JAR対まで訂正するかはOwner判断とする。後者は今回のTooling-only候補に含めず、差分・case・再packageを別に定める。どちらの場合もE09実行前に判断し、E10・直接回帰・通常rootへ自動進行しない。

## 10. E09第一候補の採用・限定実行結果（2026-10-14）

Ownerは§9のTooling-only第一候補を進め、B02a（key欠落）／B02b（異内容）については、注入条件とcase別証拠で区別したうえで復旧子の共通`CONFLICT`理由codeを採用した。Reference側に別codeを追加しない。対象差分は`LocalNotificationProviderStub.java`と`PackagedReferenceAsyncUnknownRecoveryTest.java`の2 fileである。stubはfixture生成時に検証時刻を固定し、testからの明示設定時だけその時刻を進める。B02cは別key・別eventのHTTP受理を先に成立させ、同じ期限を過ぎた固定時刻で対象keyをHOLDさせた後、独立observerから過去受理IDが同一のまま残ることをassertした。各sourceのSHA-256は順に`1C1F2A2434D1F2290553E965573DC9FB1075ACCE27CAB4E556BF378CF6D7C3CB`、`6DBE913FA25619CBA5E1419889A952B131AFE1EFCFE3A30EFB2668EE9BE08FBF`。B01の既存PASSをこの新Tooling epochの再実行として扱わない。

実行前は同じbranch／HEAD `feature/phase4-s1-reference-unknown-recovery-r3b`／`fcc515c`、`git diff --check`指摘0、固定通常JAR `462E9B...2C623`・async JAR `7A09B3...7B34`一致、Docker Server `29.1.3`・稼働container0・`postgres:17-alpine` imageあり、空きmemory 14,460,184 KiB／C空きdisk 534,746,570,752 bytes、保護Java PID23472／24668／25088生存を確認した。通常sandboxではDocker named pipeとMaven Wrapper配布物への接続が権限拒否になったため、承認済みの権限付き手順で最小診断とoffline compileを実施した。sandboxのWrapper起動失敗はMaven/test起動前であり、選択caseの再実行には数えない。Tooling `test-compile`は**BUILD SUCCESS**（15:10:47 JST）、compiler error0。既存のAddressSelection warning2件だけで、新規warningはない。Reference source／固定JARは変更していない。

同一Tooling sourceと固定async JARで、B02a、B02b、B02cを**別fresh DB・各1 Maven invocation**で順次実行した。いずれもtests1／failures0／errors0／skipped0、BUILD SUCCESS（順に15:11:21、15:12:30、15:14:01 JST）。B02aは契約key削除、B02bは同keyのpayload hash改変で、各`CONFLICT` HOLD・送信増分0・receipt増分0。B02cは期限外対象の`EXPIRED` HOLD・送信増分0であり、別keyの事前HTTP受理を含めprovider到着1・受理1がHOLD後も各1のまま、過去受理IDも同一だった。3件とも選択publication不変を照合した。これは**E09選択3枝の限定結果**であり、実providerの期限／purge保証や全般的な再送保証へ拡張しない。

各XML、txt、dumpstream、秘密・接続情報を含む行を除いた子log、ownership、HOLD JSONを`tmp/r3b-b02a-20261014/`、`tmp/r3b-b02b-20261014/`、`tmp/r3b-b02c-20261014/`へcase別に保全した。XML SHA-256は順に`AD4C3182C44359473892558933E2F408111B8BC93C85937027D6206804228F89`、`3BD79F5898789CB611271D5D337894F365A032B7F0B1B98BC89E8B139DDEED15`、`7EC0CD7F9C588B9BD8EA376EDBE8C020D35D1BC6560D28FADC115D27B922D61B`。HOLD JSON SHA-256は順に`550D537018B9BC1077B3160147119301AE08B622748C073B63E467C801EF02CC`、`61D99C3BE967044DB1A2AB2B228BE534BF2AFCAC483764EF28207D2EA6B3D02C`、`44DF82738B16DCF3DB801E93F5EED7807B54D5C7B116507C725F74E7C84775F5`。dumpstreamの`Corrupted channel`表示はJDK CDSのnative warningで、各XMLにtest failureはない。終了後Docker稼働container0、各回の元／復旧子PID不在、保護Java PID生存を確認した。観測DB clientは各3で上限8以下であり、連続最大値を推定しない。`tmp`はGit移送対象外である。

E10のB03a〜c、直接回帰4件、通常root、R3-B結果受入、local commit／remoteは**未実施**。今回の個別判断はE09第一候補までなので、次の集合は目的・順序・上限を別に確認する。

## 11. E10事前点検とB03a初回FAIL・停止（2026-10-14）

OwnerはE10の内容説明を確認して次段へ進むとした。実行前静的点検で、現行B03a／bにはprovider受理IDとReference受理観測logの独立一致assertionが不足し、B03bのTooling gateは5秒待機満了後にstubへ転送し得ることを確認した。Toolingの`PackagedReferenceAsyncUnknownRecoveryTest.java`へ、受理前のReference log不存在、受理後のevent／宛先／hash／受理ID一致と重複なしを独立observerで照合するassertionを追加した。`R3ProviderGate.java`は待機満了を転送せず失敗とし、正常転送時の論理宛先headerを保持する訂正を加えた。Reference source／固定JARは不変更。2 fileのSHA-256は順に`3A1C02E9757E4277372AC2D9519859D296C2D7C39C55421F546D0B5F4D1922D0`、`7B7CB7AE5586467B308E4D8134C32A5AD81353CE4A260CCD465316FD5030C352`。訂正後Tooling offline `test-compile`は**BUILD SUCCESS**（15:19:56 JST、既存AddressSelection warning2件、compiler error0）。

直前preflightはbranch／HEAD `feature/phase4-s1-reference-unknown-recovery-r3b`／`fcc515c`、`git diff --check`指摘0、固定通常／async JAR hashは§7と一致、Docker Server `29.1.3`・稼働container0・必要imageあり、空きmemory 14,292,940 KiB／C空きdisk 534,520,393,728 bytes、保護Java PID23472／24668／25088生存だった。B03aだけを固定async JAR・fresh DBで**初回1 invocation**実行したが、tests1／failures1／errors0／skipped0、BUILD FAILURE（15:20:23 JST）。元processではprovider到着・受理各1、publication `FAILED`、Reference観測log未保存までのassertionは通過した。その後、復旧子は起動時の`IsolatedRecoverySelection`生成で`Invalid isolated recovery selection`となり、provider照会やpublication再実行より前に失敗した。JUnitのFAIL地点は`R3RecoveryFixture.recover`の復旧子終了確認であり、B03a全体のPASSではない。

原因境界は静的にも一致する。R3-Aの`IsolatedRecoverySelection`はstatusを`PUBLISHED`／`PROCESSING`だけに限定し、今回の応答喪失後にModulithが記録した`FAILED`を受け付けない。`JdbcIsolatedPublicationAdapter`も選択snapshotのstatus一致を要求する。したがって**単にtest期待値や子引数を`PROCESSING`へ偽装できない**。B03aは「受理済みだが応答喪失後にFAILEDとなったpublication」の実際の復旧未達を示す。B03b／c・直接回帰・通常rootは停止条件に従い未実行で、今回の失敗を追加再実行で上書きしない。

初回FAIL証拠は`tmp/r3b-b03a-fail-20261014/`へXML・txt・dumpstream・秘密／接続情報を含む行を除いた元／復旧子logとownershipを別epochで保全した。XML SHA-256は`9D101BF25FB5ADD814D09E6CFB5AD8A55E8BC41CA3385A1CA91BB01BD825F933`、元子logは`24F9FB36E6081319C7C1A79951023991D83303E80035DB852A8CDDFE924D6579`、復旧子logは`D63192B8165391379DF4DCB627258D7B4CC2692C370E23A922D1D9509CDB1C9E`。終了後Docker稼働container0、元／復旧子PID不在、保護PID生存を確認した。`tmp`はGit移送対象外である。

次の判断候補は、**FAILEDを含む全停止・単一対象復旧の契約拡張を別の限定訂正として設計する**こと。status追加だけでは足りず、ModulithのFAILED publicationが選択再実行・完了できるか、試行回数・対象snapshot／lock・provider既受理照会・受理観測logの独立commitを静的／実動作で確認する必要がある。これはReference source・JAR再作成・固定通常／async対照と選択回帰を伴い、現行B03aの再実行0回には含まれない。代替としてA03型の「受理後、元processが応答を認識する前に停止」へ故障位置を狭めればR3-Aの`PROCESSING`対象で検証可能だが、今回観測した**応答喪失後FAILED**の復旧を証明しない。どちらを採るか、差分・新予算・case／root順序をOwner判断で定めるまでE10以降を開始しない。

## 12. FAILED復旧の設計方向と採用JAR静的確認（2026-10-14）

Ownerは§11の二方向から**FAILED復旧を設計する**方針を選択した。これは設計の選択であり、訂正実装・B03a新epoch再検証・後段実行の承認ではない。採用済みModulith 2.1.1のローカルJARを権限付き読取で調べると、現行`resubmitIncompletePublications(Predicate)`と別に`FailedEventPublications.resubmit(ResubmissionOptions)`がある。後者はFAILED候補をbatch／in-flight上限で取得した**後に**ID filterを適用するため、filterだけに単一対象保証を委ねない。対象以外のFAILED／RESUBMITTED不在を全停止下でread-only確認し、条件不成立ならprovider照会前に拒否する限定方式を候補とした。差分、選択case、JAR再固定、時間／資源と停止条件は[FAILED単一復旧の限定訂正判断案](../../development/phase4-s1-r3b-failed-publication-limited-correction-owner-review-20261014.md)にまとめた。実装・追加実行はOwner判断待ちとする。

## 13. FAILED自動復旧と人によるHOLDの比較（2026-10-14）

Ownerは、現段階で場合分けを積み重ねず、人による介入を含む最小復旧の観点から§12の自動復旧案と保留案の比較を求めた。[比較・判断入力§5〜6](../../development/phase4-s1-r3b-failed-publication-limited-correction-owner-review-20261014.md#5-人による照合を前提にした保留案との比較2026-10-14)に、実装所有・副作用・証拠・DoD到達点・後段への残し方を整理した。現段階の推奨は、`FAILED`をTooling独立証拠付きHOLDとして人の照合へ引き継ぎ、自動FAILED再実行はR4の運用統制とあわせて再判断すること。ただし今回のB03aはprovider受理が独立確認されているため、**HOLDだけでE10aの完了・DoD 4-2／4-3をPASSにしない**。比較は設計判断の入力であり、HOLD実装・選択case再実行や後段開始を承認済みにしない。

## 14. FAILEDの人手照合HOLDを先行するOwner判断（2026-10-14）

Ownerは[比較票§7](../../development/phase4-s1-r3b-failed-publication-limited-correction-owner-review-20261014.md#7-owner採用判断2026-10-14)のB案を採用し、Aの`FAILED`自動復旧はR4の運用統制と一緒に必要性を再判断するとした。§11の初回B03a FAILはそのまま残す。今回の判断でReference復旧入口を`FAILED`へ広げず、publication statusの偽装・直接DB UPDATE・盲目的POSTも行わない。

次の実行候補は[FAILED保留の限定開始票案](../../development/phase4-s1-r3b-failed-hold-limited-start-owner-review-20261014.md)へ分離する。採用判断時点ではHOLD用Tooling sourceの作成・compile、B03a別ケース、B03b／c、直接回帰・通常rootを**いずれも未実行**とする。E07・E09の過去PASSとB03a FAILはsource／artifact／run epochごとに維持し、E10a完遂・R3-B／R3全体受入は未達のまま扱う。

## 15. FAILED HOLD第一段階の限定実行（2026-10-14）

Ownerは[限定開始票§6](../../development/phase4-s1-r3b-failed-hold-limited-start-owner-review-20261014.md#6-第一段階のowner承認と実行境界2026-10-14)の第一段階を承認した。開始前はbranch／HEAD `feature/phase4-s1-reference-unknown-recovery-r3b`／`fcc515cd266b0297af11db4cb6e8a783c5ce23fa`、`git diff --check`指摘0、固定通常JAR SHA-256 `462E9B663E0852E5FBF6F7E8E2C3FA4BAC9FAD31DDFFB78C76C18ADA2BA2C623`／async JAR `7A09B3F5B242477D31FDFFC4D8894CBD7A4C059C0D0BB4BAAB3A72F95F937B34`一致。Docker Server `29.1.3`・稼働container0・必要な`postgres:17-alpine`／`testcontainers/ryuk:0.14.0` imageあり、JDK21.0.12／Wrapper Maven3.9.16、空きmemory 15,716,836 KiB／C空きdisk 534,530,994,176 bytes、保護Java PID23472／24668／25088生存を確認した。通常sandboxのDocker named pipe・Maven Wrapper cache・OS情報はアクセス拒否だったため、AGENTS.mdの承認済み権限付き手順で同じ読取を確認した。依存取得・image pullは行っていない。

静的点検では同じfresh DBに別の承認publicationを作成し、provider独立台帳からkey別の受理ID・event／宛先／payload hash、Reference独立observerからpublication snapshot／観測log不存在を読み取れることを確認した。`R3RecoveryFixture.expectHold`は復旧子を起動するため使わない。Tooling `PackagedReferenceAsyncUnknownRecoveryTest.java` **1 fileだけ**に別method `B03aFailedAfterAcceptedResponseLossIsHeldForHumanReconciliation`と同file内helperを追加した。既存B03a自動完了methodは書き換えていない。新source SHA-256は`862818BA44917130C3F7A4AE6A6CE7CC720525328B62AAE19D7BEA1A40C5738F`。Reference source／固定JAR・migrationは変更していない。

Tooling offline `test-compile`は**新規1回、15 test source／BUILD SUCCESS**（16:22:48 JST）。compiler error0、従来からのAddressSelection warning2件のみ。固定async JAR hashは選択ケース起動時も一致した。fresh DBで新HOLD methodだけを**1 Maven invocation**実行し、tests1／failures0／errors0／skipped0、BUILD SUCCESS（16:23:55 JST、test11.617秒）。対象keyの受理IDはprovider独立台帳のevent／宛先／payload hashと一致し、Reference受理観測logは存在せず、publicationは`FAILED`・attempt1だった。別publicationも同条件でsnapshotを固定し、元process PID19800停止後、両snapshotと対象受理ID・log不存在を再照合した。2 publicationを用いたため、provider**総到着2・総受理2**は停止前後で各2のまま。復旧子は起動せず、追加POST・状態更新は行っていない。別publicationも不変だった。Tooling所有HOLD JSONには`FAILED_RECONCILIATION_REQUIRED`、対象／受理ID、**通知key自体のSHA-256**、元process停止世代、`FAILED`／attempt、観測時刻、前後件数、次の照合先が保存された。payload hashをkey hashとして記録していない。

同名report上書き前に`tmp/r3b-failed-hold-20261014/`へXML、txt、dumpstream、redacted元子log、ownership、HOLD JSONを別保存した。SHA-256は順にXML `7C07D06BB24EA9F67D7E0516D315C0306708331553E2686CF94994BA66D55C4E`、txt `CCB665F73A46DA58B98EF82C4A6D2B63F1CBF242F6F51D6977DBDC3922E96445`、dumpstream `88233702017AB60D7769E683F08241CE6A52144A9109A3AADF2B1BF2543883AA`、元子log `D69A729212748464D4DD900DDBCE81C1E87288D2DC7E42219C39F7DF42ECCA3E`、ownership `CFECAE0C14E1CCDA2C0D8D087911AB1107C369F6B7820C35EB24734630C1F904`、HOLD JSON `2F640DFF9A2ACA59DD6FD4FF7D3C909B0A752F35B8588A9A1CE7893B6017AAEE`。dumpstreamの`Corrupted channel`はJDK CDS native warningで、XML上のtest failureは0。`tmp`はGit移送対象外。終了後Docker稼働container0、所有元子PID19800不在、保護Java PID3件生存、空きmemory 15,227,648 KiB／C空きdisk 534,514,741,248 bytesを確認した。ToolingのDB client出力sampleは3で上限8以下だが、全時間の連続最大値は推定しない。

**第一段階のHOLD証拠だけ限定PASS。** §11のB03a自動完了FAILを上書きせず、E10aのpublication完了、DoD 4-2／4-3、R3-B／R3全体受入は未達。B03b／c、直接回帰、通常rootはこの承認範囲外として未実施。次は§4候補の必要性・順序・新規有限枠を、今回のHOLD結果とsource／artifact epochを入力に別判断する。追加再実行、Reference訂正、commit／remoteを自動開始しない。

## 16. B03b／cの実行前静的点検と次回判断入力（2026-10-14）

Ownerは§15と開始票§6を確認し、次段へ進む意向を示した。追加実行なしでB03b／cの現行method、`R3ProviderGate`、R3-B契約レビュー§9を照合した。B03bはstub到着前gateと元process停止後の同key送信、B03cはDB送信前gateと照会不能HOLDという故障位置を持つ。B03bのgateはrelease期限後にstubへ転送しない訂正済みであり、B03cの`expectHold`は復旧子理由と送信増分0を検査する。一方、**両methodには別publicationの不変assertionがない**。契約レビューが各枝に要求した対象外行照合を、このままの選択実行だけで成立としない。

次の[限定判断案§7](../../development/phase4-s1-r3b-failed-hold-limited-start-owner-review-20261014.md#7-第一段階後のb03bc限定判断案2026-10-14)は、Tooling test1 fileで別承認publicationの停止後tuple照合を加え、offline test-compile1回、固定JARのB03b→B03c各fresh1回、45分・cleanup5分予約、JVM4／PG1／Ryuk1／DB client8とする。Reference source・固定JARは変えない。別publicationの位置を決定的に作れない、source／artifact／環境不一致、先行case不成立、cleanup未成立ならその場でSTOPする。**§7の具体化をOwnerが確認するまではsource訂正・compile・B03b／cを開始しない。** 直接回帰・通常root、R3-B受入、commit／remoteも未実施。

## 17. B03b／c限定結果と残る判断（2026-10-14）

Ownerは[開始票§4](../../development/phase4-s1-r3b-failed-hold-limited-start-owner-review-20261014.md#4-第一段階後の判断対象本票では実行しない)を確認・承認して次へ進むとし、§7の**Tooling 1 file、offline compile1回、B03b→B03c各fresh1回、45分・cleanup5分予約**の内側で実施した。B03b／cへ別承認publicationの停止後read-only tuple照合と、対象key別のstub到着／受理照合を追加した。B03cは既存B02a〜cの`preSend`を変えず専用準備helperを使い、復旧子の従来HOLD JSONに加え、正しい**通知keyのSHA-256**を持つTooling独立`UNKNOWN-HOLD.json`を保存した。既存`expectHold`のJSON内`keyHash`はpayload hashを記録する既存仕様のため、本段の通知key hash証拠として採用しない。訂正は`PackagedReferenceAsyncUnknownRecoveryTest.java` **1 fileだけ**、SHA-256 `78FCFCEF012BB1E1AA56B2BB2AFEE640013597BCF3B41AC3CE8BA2C9BA5ED914`。Reference source、通常／async JAR、migrationは変更していない。

fresh preflightはbranch／HEAD `feature/phase4-s1-reference-unknown-recovery-r3b`／`fcc515c`、`git diff --check`指摘0、固定通常JAR `462E9B663E0852E5FBF6F7E8E2C3FA4BAC9FAD31DDFFB78C76C18ADA2BA2C623`・async JAR `7A09B3F5B242477D31FDFFC4D8894CBD7A4C059C0D0BB4BAAB3A72F95F937B34`一致、Docker Server29.1.3・稼働container0・必要imageあり、保護Java PID23472／24668／25088生存、空きmemory 15,215,316 KiB／C空きdisk 534,513,917,952 bytesだった。Tooling offline `test-compile`は**新規1回、15 test source／BUILD SUCCESS**（16:41:40 JST）、compiler error0、既存AddressSelection warning2件のみ。依存取得・JAR再packageはしていない。

固定async JAR・別fresh DBでB03bを**初回1 invocation、tests1／failures0／errors0／skipped0、BUILD SUCCESS**（16:42:17 JST、test16.99秒）で実行した。対象keyはstub到着前gateで停止、元Reference停止後の単発復旧で同keyの到着／受理各1、Reference観測log一致とpublication完了を確認した。別publicationの停止後tupleは復旧後も不変。case証拠は`tmp/r3b-b03b-20261014/`へ同名report上書き前に保全した。SHA-256はXML `94473AE28A7E46078C0162DC806BA1B94DEA127AA381908F2698A6783C116FE4`、txt `C7A758E5B7E21D0048D4F2F39029D16D71BA85B2E32BDD5DD0F8B25C51CBE89C`、dumpstream `8EA06142E56D092583AA8B3B27EA1E8B6157A34536C78449BFA83603B3B5F746`、元子log `F7E3AB971B9F6B0E92862450C4B97201DDA8941435DE0B5868F94637E1C12F6D`、復旧子log `F48ABA5AD75074832E5F550B0F44A338D2161589F5F9409C315724E61BBACB21`。所有PID28092／39108不在・Docker稼働container0、保護PID3件生存を確認してからB03cへ進んだ。

B03cは同じcompiled Tooling／固定async JAR・新しいfresh DBで**初回1 invocation、tests1／failures0／errors0／skipped0、BUILD SUCCESS**（16:43:35 JST、test16.93秒）。targetのDB送信前gateを確認し、元process停止後の照会不能で復旧子は`UNKNOWN` HOLD、対象keyのstub到着／受理0、Reference観測log不存在、対象publicationと別publicationのtuple不変だった。補助`UNKNOWN-HOLD.json`は`reason=UNKNOWN`、通知key SHA-256、次の照合先を記録した。証拠は`tmp/r3b-b03c-20261014/`へ分離し、SHA-256はXML `C790AB4D48458C2D90B1C674BF5A054021E596CC83D238BA41A44A4F4354B10C`、txt `A0B3BA9AB93E559EBAFA9278C88228A32829DF2484787E0E60D34CDDEF04A1E0`、dumpstream `943375E76DD804FB9D5565B057258D4E6EAEF0AF1B1F13BD4FAF3149542DE9DC`、元子log `AB6A9FEC5A5E6B8EFD22855343A61E764FF82B2871477D625F6A31C4F3AAAE0C`、復旧子log `A6A1C7464EDC82E5503F85F15754352F9608190D9DAE428F8F4B416D03553F7B`、復旧子HOLD JSON `4597AFD83FA888B48A98B01DF5760DC6D61A690DD32ECE04DFB3589D31D8E7DF`、補助UNKNOWN JSON `7BBC8FB4E59D7A1F34B24CCB185C76E27E36D49A65201E85E14EC745D83EFDE0`。dumpstreamの`Corrupted channel`はJDK CDS native warningで、両XMLにtest failureはない。

B03c終了後Docker稼働container0、所有PID9188／27164不在、保護PID3件生存、空きmemory 15,084,484 KiB／C空きdisk 534,526,947,328 bytesを確認した。ToolingのDB client出力sampleは各3で上限8以下だが全時間の連続最大値を示さない。tmp内のXML・log・JSON・JARはGit移送されない。**B03b／cの選択2枝だけ限定PASS**。§11のB03a自動完了FAILと§15の人手HOLD PASSは別epochとして維持し、E10a完遂・DoD 4-2／4-3・R3-B全体受入を主張しない。直接回帰4件と通常rootは未実施で、必要性・順序・新規有限枠を次に判断する。追加実行・commit／remoteは開始しない。

## 18. §17後の残ケースと判断境界（2026-10-14）

Ownerは§17を確認し、残ケースの整理を求めた。今回の整理は既存XML・source・開始票の読取であり、Maven／Dockerの追加実行、結果受入、実装訂正やcommitを含まない。R3-Bの選択結果は**契約2件PASS、B01 1件PASS、B02a〜c 3件PASS、B03b／c 2件PASS、B03a自動完了1件FAIL、別methodのB03a人手HOLD1件PASS**。各PASS／FAILは§6・§8・§10・§11・§15・§17の異なるrun epochに属する。後から加えたTooling sourceで過去のPASSを再実行したことにはしない。Reference通常／async JARは§7の固定対で、直近Tooling source SHA-256は§17の`78FCFCEF...ED914`である。

| 区分 | 正確な残対象 | 現状と次の判断 |
|---|---|---|
| 直接回帰 | `PackagedReferenceAsyncRecoveryTest#A01PublishedBeforeSendCompletesOneTargetAfterFullStop`、`#A03AcceptedBeforeCompletionReusesOneProviderReceiptAfterFullStop`、`NotificationProviderContractTest#reusesTheOriginalReceiptForIdenticalRequests`（D01）、`PackagedReferenceAsyncNotificationTest#correlatesTwoIndependentApprovalNotificationsToCompletedPublications`（C04） | **現R3-B source／固定JAR epochでは4件とも未実行**。R3-Aの旧PASSは代替にしない。A01→A03→D01→C04を各fresh DB・各初回1 invocationとして検討し、各件のXML・子log／stub・cleanupを別保存。先行不成立で後行STOP。新規時間／資源枠と実行承認が必要 |
| 通常構成回帰 | 通常rootのoffline `clean package` 最大1 invocation、専用skipと通常無効、生成通常JARと固定通常JARのapp内容／共通runtime対照 | **未実行**。直接回帰4件成立と新しいpreflight後に、root専用の上限・cleanupを別判断。R3-Aのroot303実行＋92skipを今回のPASSへ転記しない |
| E10aの完遂 | 既存`B03aAcceptedButLostResponseCompletesWithoutSecondPost`は§11でFAIL。別methodの`B03aFailedAfterAcceptedResponseLossIsHeldForHumanReconciliation`は§15でHOLD PASS | **安全な人手HOLDだけ成立、publication完了は未達**。FAILED自動復旧A案は採用せず、R4のoperator認可・Audit・再開統制と一緒に必要性を再判断。B03aを無条件に再実行しない。専用クラス全件実行にはこの既知FAILが含まれるため、PR／CIの選択方法と未達表示を統合判断前に明示する |
| E08残枝 | Referenceでの同論理通知の逐次再要求、stubへの同key並行要求、通常listenerと復旧processの並走 | **今回の新規7枝には含まない**。直接回帰候補D01はstubの同key逐次要求、C04は別通知相関に限る。両件が成立してもReference全経路や並行枝の保証へ拡張せず、残枝は別の目的・故障位置・開始判断へ送る |
| 結果・統合 | R3-B限定結果のOwner受入、source／artifact・証拠保全、local commit・remote／最初のPR | **未判断・未実施**。4件／rootの結果とE10a・E08の残存を明示した限定受入の可否を先に判断する。R3-B／R3全体の`COMPLETE`、DoD 4-2／4-3、正式受渡し・main統合は自動で宣言しない |

次の最小実行候補は**直接回帰4件だけ**を先に固定し、現行Tooling sourceと固定JAR hash、各methodのproperty／fixture条件を静的点検してから新規有限枠・停止／cleanupをOwner判断へ出すこと。通常rootはその結果を見て別枠とする。`tmp`内の固定JAR・XML・log／JSONはGit移送されないため、証拠のhashと所在を保持する。

## 19. 直接回帰4件の新規限定実行結果（2026-10-14）

Ownerは[開始票§9〜10](../../development/phase4-s1-r3b-failed-hold-limited-start-owner-review-20261014.md#9-直接回帰4件だけの新規実行枠案2026-10-14)の**4件だけ・新規50分／cleanup10分予約**を確認して開始を承認した。実行はA01→A03→D01→C04、各fresh隔離DB・各初回1 Maven invocationであり、旧枠の残予算・旧R3-A PASSは使用していない。通常root、既知FAILのB03a、専用クラス全件は実行していない。

preflightはbranch `feature/phase4-s1-reference-unknown-recovery-r3b`／HEAD `fcc515cd266b0297af11db4cb6e8a783c5ce23fa`、`git diff --check`指摘0、固定通常JAR SHA-256 `462E9B663E0852E5FBF6F7E8E2C3FA4BAC9FAD31DDFFB78C76C18ADA2BA2C623`／async JAR `7A09B3F5B242477D31FDFFC4D8894CBD7A4C059C0D0BB4BAAB3A72F95F937B34`一致、直近UnknownRecovery test source `78FCFCEF012BB1E1AA56B2BB2AFEE640013597BCF3B41AC3CE8BA2C9BA5ED914`不変だった。選択3 test classは16:41:40 JSTのTooling offline compile済みclassを使用し、今回の再compile・JAR再packageはない。JDK21.0.12、Docker Server29.1.3、`postgres:17-alpine` imageあり、稼働container0、開始前空きmemory 15,056,576 KiB／disk 534,504,906,752 bytes、保護Java PID23472／24668／25088生存を確認した。sandboxからDocker named pipeへ接続拒否となったため、AGENTS.mdの承認済み権限付き手順で同じ読取を確認し、同手順で選択testを実行した。

| 順序・保存先 | XML結果・時刻 JST | XML SHA-256 | 限定して確認した境界 |
|---|---|---|---|
| A01 `tmp/r3b-direct-a01-20261014/` | 1／0／0／0、16:55:56、16.699秒 | `ED9CEF5ABB1F5B4D90E309F5BCD6367DFC453406AB75C0A3878D04D52EBFA633` | 固定async JARで送信前`PUBLISHED/1`、stub到着／受理0の停止位置から単一復旧・完了 |
| A03 `tmp/r3b-direct-a03-20261014/` | 1／0／0／0、16:57:08、16.679秒 | `BC2967607809597CAC693FC939814154BBAE3BB9CC8B96A1401F4CE71418998F` | provider受理後・完了前の停止から既受理を再使用し、追加POSTなく完了 |
| D01 `tmp/r3b-direct-d01-20261014/` | 1／0／0／0、17:03:00、3.613秒 | `4AF4A32B32B6F6CFAA82E6D6BEB9A42A2F4E7C8A3071877149D405CB43BA0948` | **stub契約だけ**の同一key／同一内容の逐次要求と同一受理ID |
| C04 `tmp/r3b-direct-c04-20261014/` | 1／0／0／0、17:03:42、11.231秒 | `461BDA9AA16EEC54761F1F96FA8AC1EFD6133F08B5120F3D4C143432E93F2A5D` | 独立承認通知2件の相関とpublication完了 |

各caseのmethod単位選択、opt-in property、fork1／heap768 MiB、指定test不在時FAILを明示したoffline Surefire直接goalで、**4 invocation・再実行0回**。A01／A03で同名reportが上書きされる前にXML・txt／dumpstream・元／復旧子logとownershipを別directoryへ退避し、C04の元子logも別保存した。元子log SHA-256はA01 `FB639DF734843ED908024F57E8F5FC40945EE1606BC89DAA2637352C81D37D1E`、A03 `54F95E01822065C42BC6193D653CD3D68F13BA3B2AB8182D907EC1E9654A593D`、C04 `FEDBD9878EC1B0D63670C17860E62C6926745D6FDBA7E90EE290963C510B19D8`。A01／A03の復旧子log SHA-256は順に`03CE70915CADD8EA1A995BFDB6F655C47B630ECD6A3E7D3055387EDD3AA2AD58`、`5F8AD7F34E81339EEDB82216F29E203C0AA95D5B39265C9370ED6413C361C989`。各directoryの`maven.log`・XML・txt・dumpstreamとownershipにもhashを付して保全した。dumpstreamの`Corrupted channel`はJDK CDSのnative warningで、各XMLのfailure／errorは0だった。

各case後にDocker稼働container0を確認し、A01の所有PID47508／27428、A03の28988／16116、C04の4372はいずれも不在、保護PID3件は生存した。Tooling表示のDB client sampleはA01／A03／C04で各3、上限8以下だが全時間の連続最大値を意味しない。終了時空きmemory 14,862,392 KiB／disk 534,510,829,568 bytesで、資源・時間上限内かつcleanup成立。D01はReference子JVMを起動しないstub契約testである。`tmp`のXML・log・固定JARはGit移送されない。

この結果は**現R3-B source／固定JAR epochの直接回帰4件限定PASS**。B03a自動完了FAIL、E08残枝、DoD 4-2／4-3、R3-B／R3全体の`COMPLETE`は未達のまま。次は通常rootの必要範囲・新規上限・fresh preflightを別判断し、結果受入・commit／remoteも先行しない。

## 20. 通常rootの静的点検と次回判断入力（2026-10-14）

Ownerは[開始票§10](../../development/phase4-s1-r3b-failed-hold-limited-start-owner-review-20261014.md#10-直接回帰4件のowner開始承認と結果入口2026-10-14)を確認し、通常root・R3-B結果受入の順へ進む意向を示した。**ここではroot Maven invocation、Docker起動、結果受入を行っていない。** §19の4 XML hashと固定通常／async JAR hashは再照合で一致。branch `feature/phase4-s1-reference-unknown-recovery-r3b`／HEAD `fcc515cd266b0297af11db4cb6e8a783c5ce23fa`、未commit差分23 file（code20／文書3）、`git diff --check`指摘0。通常profileのPOMはModulith JDBC starterを含まず、`NotificationAsyncConfiguration`と`IsolatedPublicationRecoveryConfiguration`はそれぞれasync／recoveryの明示propertyで条件登録する。通常構成で有効化propertyを与えない方針を静的に確認した。これは実際のrootでの不登録・専用skip検証の代用ではない。

Maven Wrapper3.9.16／Temurin JDK21.0.12は起動し、Docker Server29.1.3、`postgres:17`／`postgres:17-alpine`／`testcontainers/ryuk:0.14.0` imageあり、稼働container0。点検時空きmemory 15,123,632 KiB／C disk 534,721,609,728 bytes、保護Java PID23472／24668／25088生存。専用`tmp/r3b-root-preflight-20261014/jvm-temp`を作り、Java probeで設定pathと`toRealPath()`の一致`true`を確認した。通常sandboxのDocker named pipe接続拒否については承認済み権限付き読取手順でServer・資源を確認した。**これは静的・環境preflightの時点値であり、root起動直前に再照合する。**

初回R3-B開始票§4ではE10全枝成立後だけrootへ進む順序だったが、§11のB03a自動完了FAILと§15の別method人手HOLD PASSを履歴として残している。したがって[開始票§11](../../development/phase4-s1-r3b-failed-hold-limited-start-owner-review-20261014.md#11-通常rootの新規限定枠とr3-b受入判断の順序案2026-10-14)では、rootの目的を**通常構成保護の限定回帰**に絞り、新規30分・通常`clean package`1回・cleanup5分予約・再実行0回と具体化した。通常rootがPASSしてもB03a完遂・E08残枝・DoD 4-2／4-3・R3-B／R3全体`COMPLETE`を認定しない。rootの実行承認と、その後のepoch別結果点検・R3-B限定受入判断は区別する。

## 21. 通常rootの新規限定実行結果（2026-10-14）

Ownerは[開始票§11〜12](../../development/phase4-s1-r3b-failed-hold-limited-start-owner-review-20261014.md#11-通常rootの新規限定枠とr3-b受入判断の順序案2026-10-14)の新規30分・通常構成root1回を承認した。起動直前にbranch `feature/phase4-s1-reference-unknown-recovery-r3b`／HEAD `fcc515cd266b0297af11db4cb6e8a783c5ce23fa`、未commit code20 file fingerprint `9C7A8A76A0F77D8629D28FEA00FFF44C33A8A01D9CACFD87C57036C458338126`、`git diff --check`指摘0を確認した。固定通常／async JAR hashは§7、直接回帰4 XML hashは§19と一致し、各XMLは別`tmp`へ退避済み。専用JVM tempはJavaの`toRealPath()`と設定pathが一致`true`、JDK21.0.12／Maven Wrapper3.9.16、Docker Server29.1.3・必要imageあり・稼働container0、開始直前空きmemory 14,928,776 KiB／disk 534,722,002,944 bytes、保護Java PID23472／24668／25088生存を確認した。

通常構成のoffline root `clean package`を、async profile・専用検証propertyなしで**1 Maven invocation**実行した。通常root DB limits、Maven／test heap768 MiB、fork1・reuseなし・JUnit並列なし、JAR再生成、専用canonical JVM tempを指定した。実際の引数は`tmp/r3b-root-run-20261014/run-command.txt`に保全した。17:16:35〜17:20:03 JST、経過208.260秒で**Exit 0／BUILD SUCCESS**、reactor全module成功。追加再実行0回。Maven logは同dir `normal-root.log`、1,187,165 bytes、SHA-256 `DFF940766E6F6B41D7D2FB32015BEB38ACB1748C949EF57EDB3C22796FB1DB72`。

root reactor所属のfresh Surefire XML **62 file、397件＝305実行＋S1／B2専用92 skip、failure0／error0**。内訳はArchitecture Contract4、ArchUnit Rules107、Reference286（うち92 skip）。各XMLのmodule・path・case・件数・SHA-256を`tmp/r3b-root-run-20261014/root-result.json`へ固定し、SHA-256 `1368420BF518BD5E3239014B510497EDBCE7EB60833C24F0F202496BEC0AF8E8`。現物は同dir `reactor-reports/`へ保存し、root reactor外のWalking Skeleton等の旧XMLを集計へ含めない。JVM temp propertyがある52 XMLはすべて今回の専用canonical pathと一致した。前R3-A rootの303実行＋92skipを転記せず、今回の305実行という新epochの値を採用する。通常起動の条件評価logではasync／recovery有効化propertyが見つからず、それぞれの条件付きConfigurationは登録条件不成立だった。

生成通常JARは`tmp/r3b-root-run-20261014/reference-normal-root.jar`へ別保存し、64,616,426 bytes、SHA-256 `8CDF1D40B72ADD310689712B886182DAAE9E5EB259D5E08C074074E29E64C8A1`。固定通常JARとは**別build／artifact epoch**で外側byte hashは一致しない。`BOOT-INF/classes`199 entryは名称・内容hashが一致し、`BOOT-INF/lib`117件は名称集合が一致。うち108件のnested JAR byte hashが一致し、残るKOIKI-owned nested JAR9件は外側byte hashが異なるが、各nested JARを開いた全entryの名称・内容hashは一致した。比較結果は`jar-content-comparison.json` SHA-256 `707C705FB7F5678A9D7754CBC8766657245A24E4D7B72CA4AC96E55C01ABF475`と`nested-jar-comparison.json` SHA-256 `9BCF4D2F7910E93F182CF664BC7E97B1808E43135B53017A464FFE2C52710684`で保全した。通常JARには既存の`spring-modulith-events-api`だけがあり、async専用JDBC runtimeはない。外側JARやnested JARのarchive byte差の原因を推定して一致扱いにはせず、今回の判定は**entry内容の一致**に限る。

終了後Docker Server応答・稼働container0、Java processは保護PID3件のみ、空きmemory 14,838,596 KiB／disk 534,625,124,352 bytes。run保全領域69,019,208 bytesで2 GiB以下。branch／HEAD、code20 file fingerprint、固定JAR hashは不変、`git diff --check`成立。実行中のJVM／Ryuk／DB client最大値は連続計測していないため、終了時0や有限sampleから最大値を保証しない。tmpのXML・log・JAR・indexはGit移送されない。

**通常rootの限定結果はPASS**。これは通常構成保護の回帰であり、§11のB03a自動完了FAILを覆さない。選択case・rootのepoch別点検とR3-B限定結果のOwner受入判断は次節へ分ける。local commit、remote／PR／mergeは未実施。

## 22. R3-Bのepoch別結果点検と限定受入の判断入力（2026-10-14）

本節は保存済みsource・固定JAR・XML／log・§21の通常root結果の照合であり、追加test・packageは行わない。Reference固定通常／async JARは§7の同一source build、Toolingは§6→§10→§11→§15→§17の追加／訂正epoch、直接回帰は§19、通常rootは§21の**別build epoch**として扱う。後段PASSで初回FAILや旧sourceのPASSを上書きしない。

| 境界 | 今回判断に使える限定証拠 | 残す未達 |
|---|---|---|
| 契約・E07／E09 | Reference契約2件、B01、B02a〜cの各選択PASS。受理観測log・key／期限・理由付きHOLDを各故障位置で確認 | 実providerの期限・purge、包括的な再送保証には拡張しない |
| E10 | B03b／c各PASS、B03a人手HOLDの別method PASS。対象外publication不変・受理数／照会不能をcase別に確認 | **元B03a自動完了methodはFAIL**。provider受理後応答喪失でpublication `FAILED`となる枝の自動完了は未達。FAILED自動復旧A案はR4の運用統制と再判断 |
| 直接回帰・通常保護 | §19のA01／A03／D01／C04各1件PASS、§21のroot305実行＋専用92skip・failure／error0、通常JAR内容対照 | D01はstub逐次同key契約であり、E08のReference逐次／stub並行／listener並走を証明しない |
| 統合・DoD | 上記を別epochの**R3-B限定結果**としてOwner受入の候補にできる | E10a完遂、DoD 4-2／4-3、R3-B／R3全体`COMPLETE`、正式受渡し・main統合は主張しない。既知FAIL methodを含む専用クラス全件のCI／PR選択方法も別判断 |

**現状態：R3-B LIMITED RESULTS VERIFIED / OWNER ACCEPTANCE PENDING。** OwnerがB03a FAIL・E08残枝・rootの限定目的を含むこの範囲を確認してから、限定結果の受入可否を判断する。受入しても自動完了を満たしたことにはせず、成果固定のGit差分・tmp保全、local commit、remote／最初のPR・main mergeはそれぞれ別判断に残す。

## 23. R3-B限定結果のOwner受入（2026-10-14）

Architecture Ownerは§22のsource／artifact／run epoch別の結果、**B03a自動完了FAILを残した人手HOLD採用**、E08残枝、DoD 4-2／4-3未達、通常rootを通常構成保護に限定する境界を確認し、「§22の範囲を『R3-B限定結果／Owner受入』として承認します」と明示した。これを**R3-B LIMITED RESULTS / OWNER ACCEPTED**として記録する。受入対象は、契約2件、B01、B02a〜c、B03b／cと別methodのB03a人手HOLD、直接回帰4件の各epoch限定PASS、および通常root305実行＋専用92skip・failure／error0と通常JAR内容対照である。

§11のB03a自動完了FAILとpublication `FAILED`、§15の人手HOLD PASSを別証拠として保持し、元methodの期待をPASSへ書き換えない。FAILED自動復旧の必要性はR4のoperator認可・Audit・再開統制とともに再判断する。E08のReference逐次同論理通知／stub並行同key／通常listenerと復旧process並走、実provider配信、DoD 4-2／4-3、R3-B／R3全体`COMPLETE`、正式受渡し、Phase 4全体へ受入を拡張しない。専用クラス全件には既知FAILが含まれるため、PR／CIでの選択方法と未達表示は統合判断に残す。

このOwner受入は、**未commit差分の成果固定対象点検、local commit、remote push、PR／main mergeまたは追加検証の承認を兼ねない**。次はGit差分とtmp証拠の所在・hash・Git移送範囲を点検し、成果固定の判断材料を整える。`tmp`のXML・log・固定／生成JARはGitでは移送されない。§22末尾の`OWNER ACCEPTANCE PENDING`は本受入前の履歴であり、本節を現行判断とする。

## 24. Owner受入後の成果固定・統合前点検（2026-10-14）

追加Maven／Docker実行・source訂正・commit／remote操作を行わず、branch `feature/phase4-s1-reference-unknown-recovery-r3b`／HEAD `fcc515cd266b0297af11db4cb6e8a783c5ce23fa`のworking treeと保全証拠を読取点検した。未commitは**23 file＝Reference production／migration12、Reference test1、Tooling test7、文書3**。tracked変更14 file、新規9 fileで、POM／Framework API・Rules／Security／通常properties／Root Reactor／CIの差分はない。code20 fileのroot実行前後fingerprintは§21の`9C7A8A76A0F77D8629D28FEA00FFF44C33A8A01D9CACFD87C57036C458338126`で一致し、`git diff --check`指摘0。これは差分の範囲・固定候補の点検であり、23 fileのlocal commitを実行または承認した記録ではない。

| Git固定候補 | file数 | 境界 |
|---|---:|---|
| Reference production／migration | 12 | provider結果分類・受理観測logとV6、単一復旧選別／HOLD。通常無効の条件付き登録を維持 |
| Reference test | 1 | provider受理／照会の直接契約。通常rootでは追加分を含む305件実行＋専用92件skip |
| Tooling test | 7 | loopback stub、停止／復旧fixture、E07／E09／E10と直接回帰の故障位置・独立観測。既存B03a自動完了methodのFAILと別method人手HOLD PASSを両方保持 |
| 文書 | 3 | 本Evidence、FAILED自動復旧A／保留B比較票、保留Bの段階別限定開始票・Owner判断 |

Git ignore `tmp/`を確認した。固定通常／async JAR、契約2件・B01・B02a〜c・B03a初回FAIL／別HOLD・B03b／c・直接回帰4件のXML、HOLD JSON、通常root62 XML／index／生成JARはいずれも所定の`tmp/r3b-*`に存在した。主要XML・HOLD JSON・固定JAR・root indexのSHA-256は§6〜21の記載値と再照合一致。特に`tmp/r3b-b03a-fail-20261014/B03a.xml`は**1 failure**でhash `9D101BF25FB5ADD814D09E6CFB5AD8A55E8BC41CA3385A1CA91BB01BD825F933`、`tmp/r3b-failed-hold-20261014/B03a-HOLD.xml`は**1 PASS**でhash `7C07D06BB24EA9F67D7E0516D315C0306708331553E2686CF94994BA66D55C4E`。この端末内証拠は23 fileのGit commit／pushでは移送されない。別PCや長期履歴で必要ならhash付きで別途保全する。

次は①23 fileの差分内容と既知FAIL methodの説明を最終reviewし、成果固定対象とlocal commitをOwnerが別判断、②保存目的のremote pushを個別判断、③[main統合の最初のPR点検案](../../development/phase4-s1-main-integration-first-pr-scope-review-20261014.md)に従い、先頭文書9 fileの小さなPR候補からbase／head・CI選択を点検する順が妥当。R3-B branch全体をそのまま最初のmain向けPRにしない。R3-B専用classは`koiki.reference.async.r3b.verification.enabled=true`のopt-inで、**全method選択には既知FAILの元B03aを含む**。PR／CIでの選択・未達表示を決めるまで、限定受入を専用class全件PASSやDoD完了へ言い換えない。R4のFAILED自動復旧要否・operator認可／Audit／再開統制は別の目的・開始判断へ送る。
