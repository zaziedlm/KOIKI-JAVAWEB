# S1 R1：実装作成・早期8件・通常root統合確認（2026-10-12）

**状態：R1 BOUNDED SELECTED VERIFICATION COMPLETE / OWNER ACCEPTED（2026-10-13）。通常root300実行＋専用92skip、Tooling新規6件、既存Critical Journey1件成立。**

現行結果・残作業は§13、機能面の説明は§14、Owner受入承認は§15を入口とする。旧停止・訂正・予算は各時点の履歴であり、追加実行の許可へ流用しない。承認済み最小検証集合と限定結果のOwner受入は成立済み。次は成果固定に向けたGit差分・対象の点検であり、追加検証・commit／remoteは自動開始しない。

[R1開始票§8](../../development/phase4-s1-r1-minimal-start-owner-review-20261012.md#8-owner承認反映と文書source固定の対象2026-10-12)のOwner承認とpreflight成立後、`feature/phase4-s1-reference-async-r1` / `975af1d806dd99d7b65c9b745db5adfc1e8c4ce0`を基点に作成した。code・test・POM・metadata・SQLは24 file、上限28以内。今回の結果文書1件は別に扱う。local commit／remoteは未実施。

## 1 作成内容と保護

- Reference限定events APIとMaven profile内のJDBC starter。BOM／Parent／Framework／Root Reactor／CI不変更。
- expense-owned値record・発行Port／Spring Adapter／条件付き登録。approveの状態変更・flush・承認Audit後、同じtransactionの終了前だけ発行hookを呼ぶ。通常構成はno-op。既存use case signatureと旧constructor経路を保持。
- notificationの標準module listener→Application→provider Port／数値loopback HTTP Adapter。固定key／canonical payload、redirect非追従、有限timeout／応答サイズ、限定executorを作成。追加Audit主体・worker／復旧・consume／closeを導入しない。
- Boot 4.1.1 JARで確認した`org.springframework.boot.EnvironmentPostProcessor`のSPIを用い、通常registry除外と選択時の設定／Flyway locationを合成。V5は採用2.1.1のv2 DDLをschema-qualified化し、通常properties・V1〜V4を維持。
- 新規4 test class／9 invocation候補とTooling helper2件を作成。既存Critical Journeyには追加schema不在assertionだけを補足し、他assertionを維持。

早期実行前に、変更24 file以外の元source1166 fileのhash一致を確認した。実装入力manifestは1190 fileを固定している。既存Security／permit関連・migration・通常propertiesは不変更。この結果文書は実行後の記録であり、実行入力source manifestへ混入させない。

現行RESTには承認endpointがないため、Toolingの正常経路は既存Bearerで申請・提出し、既存Session login／CSRF付きMVC承認を利用する。新しい受付APIやtest routeは追加していない。payload整合・provider受理台帳はtest-ownedで、正式JARへ含めない。

## 2 gate停止と限定訂正

最初のearly gateはJava baseline件数不一致で停止し、Maven／compile／testは起動していなかった。元2件のPID／creation identityは保持され、追加PID27844も同じCode.exe親に属するIDEメタデータサービスと限定読取で分類した。command line全文は保存・表示していない。

Ownerが会話で委任した「同種の問題改修については、確認承認は経ずに継続を許可」の範囲を、この監視ハーネスの基準訂正へ適用した。元2件を除外せず追加IDE Javaを保護するbaseline追記だけを行い、所有JVM4・DB1・他上限／ケース数を維持。process停止・code原因修正・test追加再実行は0。未開始の初回compile・早期8件へ進んだ。

旧readiness・停止summaryのCleanup=falseを上書きせず、`stages/early-gate-stopped/`に保持。追加protected identityは`baseline-correction.json`、限定読取は`early-gate-observation.json`／`extra-java-classification.json`に別保存した。以降のbaseline差分を無条件採用する規則ではない。

## 3 成立結果

| 集合 | 結果 |
|---|---|
| Referenceと必要Reactor moduleのcompile／test-compile | Maven Exit0／BUILD SUCCESS。新Reference main・Reference testのNullAway／Error Prone等を含む初回compile成立 |
| 値event K01／K02 | 2 PASS |
| endpoint D03 | 1 PASS、非loopback拒否・redirect非追従 |
| ReferenceArchitectureTest | 既存2 PASS、notificationだけLEVEL_2、Framework identity検査を維持 |
| ExpenseApplicationServiceMethodSecurityTest | 既存3 PASS、constructor配線変更のみ、assertion・件数維持 |
| 合計 | 8 invocation、failure／error／skip0、fresh XML4 fileとcase名／hash照合済み |
| cleanup | 所有Job終了・元2件と追加IDE Javaのidentity保持・containerなしを照合し成立 |

初回Mavenを含むearly実行64秒。通常root・Tooling package検証は0回で、S1／B2専用92件は再検証していない。Toolingの新規6件は作成済みだが、Tooling test-compile／実行はまだ未成立。V5のSQL実行・選択起動・publication保存・provider受理／completionの実動作もまだ未成立。

## 4 次段階と証拠

次は変更全体の通常root1回とfresh XML集合／専用92skipの照合。その成立後に通常／async artifactを別固定し、Tooling D01〜02／C02〜05と既存C01を明示実行する。R2〜R5・全DoD・正式受渡し・実provider配信は未達を維持する。早期8件を最終rootの独立caseへ二重加算しない。

端末証拠：`tmp/r1-975af1d-20261012-first/`の`implementation-inputs.json`、`stages/early/summary.json`、`stages/early/reconciled-results.json`、`execution-budget.json`。Gitでは端末内raw／runner／artifactは移送されない。結果文書作成前の累計102秒・残4098秒、cleanup300秒予約。結果照合・source／差分の最終保全は別途台帳へ計上する。

## 5 通常root初回1回の停止と次の判断

Ownerの「通常root 1回の統合確認へ進めます」に従い、同じbranch／HEAD・1190 fileの隔離sourceで、offlineの通常`clean verify`を1回実行した。async Maven profileは選択していない。開始は2026-10-12 22:16:05 JST、97秒で`Owned PostgreSQL host limits mismatch.`により監視停止した。Maven終了成功・全集合成立として扱わない。追加再実行・async package・Tooling検証は実行していない。

原因はrunner準備の不整合である。root引数の`koiki.reference.verification.resource-limits.enabled=false`は、S1専用64件のskipだけでなく、既存`ReferencePostgreSqlTestConfiguration`のDB制限も無効化する。実装はfalse時に制限modifier／`max_connections=16`指定のないPostgreSQLContainerを返す。監視は最初の`ExpenseApiPostgreSqlIntegrationTest`のDB起動時、HostConfig memory 1 GiB／CPU1の一致検査で停止した。失敗時の個別memory／CPU実値は保存されていないため数値を推定して記載しない。アプリの業務assertion失敗を示す結果ではないが、rootのPASSにもならない。

| 確認 | 結果 |
|---|---|
| 停止までのfresh XML | 16 file／117 invocation、failure・error・skip0。部分結果のみ。全392件や専用92skipの成立へ加算しない |
| cleanup | 所有Jobと所有PG／Ryukのcleanup、protected Java baseline保持がrunnerの終了検査で成立（Cleanup=true） |
| 最終通常root／通常JAR固定 | 未成立／未固定。過去R0やearlyの結果で代用しない |
| 予算 | root終了時の累計201秒・残3999秒、cleanup300秒予約。後続の読取照合・保全分は別計上 |

証拠は`stages/root/summary.json`、`maven.log`、`owned-container-identities.json`、`container-resources.json`、`partial-xml-observation.json`に保持。停止summaryやearly成立結果を上書きしない。

次の限定訂正案は、通常rootのDB資源制限を専用test実行条件から分離すること。候補は既存test-owned `ReferencePostgreSqlTestConfiguration`に通常root用の独立selectorを設け、DB memory／CPU／max_connectionsだけを指定する最小変更と、tmp runnerの当該selector供給である。通常pool・業務assertion・test件数・S1／B2 skip条件は維持する。単に既存flagをtrueへ変えると専用64件が実行されるため採用しない。変更先は開始票の既存test不変更条件の例外となるため、この案は未実装・未承認とする。

追加rootは1回が必要となるが、開始票の初回1回を既に消費しており、残時間だけでは再開しない。訂正対象・例外・追加1回のOwner判断後に、source再固定・cleanup／環境差分の読取確認を経て実行する。追加pure網羅、専用92件の再検証、Framework／通常構成変更へ拡張しない。

## 6 §5のOwner承認と有限訂正

Ownerは会話で「5 通常root初回1回の停止と次の判断」を確認・承認し、「これで進めます」と指示した。既存test不変更条件の例外を`ReferencePostgreSqlTestConfiguration.java`のDB fixture資源制限分離だけに限定し、通常root追加1回／600秒を許可する。他の資源・残総予算・cleanup300秒予約・停止条件を維持する。

独立selector `koiki.reference.normal-root.db-limits.enabled`は既定false、true／false以外を拒否する。専用flagがfalseかつ独立selectorがtrueのときだけ、既存通常DBにmemory1 GiB／CPU1／max_connections16を指定する。既存専用flagとpool検査、専用test条件、通常pool、業務assertion・件数は維持する。code差分は同test-ownedファイルの10行追加のみで、R1作成対象は計25 file（上限28以内）。

tmp runnerへ明示的な`-RootRerun1`を追加し、初回STOPPED／Cleanup=trueと承認input hashをgateとして独立`stages/root-rerun1/`へ出力する。初回`stages/root/`・旧input manifest・runnerは保存し、新manifestは同1 file以外の1190 file一致を確認して固定した。再開前の読取観測でprotected Java3件のidentity一致、containerなし、memory／disk floor成立を確認した。記録は`root-correction-1/approved-inputs.json`／`restart-observation.json`。追加実行の結果は別に記録する。

## 7 承認済み追加1回の停止結果とtmp容量監視の限定訂正案

2026-10-12 22:31:51 JSTに追加1回を実行したが、10秒で停止した。`koiki-archunit-rules`のMaven cleanによるtarget削除と、tmp runnerの`Get-ChildItem -Recurse -File`による容量走査が重なり、`target/test-classes/org/koikifw/archunit/fixture/metadata`のDirectoryNotFoundで監視が例外終了した。DB起動前であり、§6のDB制限の実効確認と通常root全集合照合は未成立。試行は合計2回、以後のroot実行は0回。Cleanup=true、1190 fileのsource hash一致を確認した。旧earlyのPASSは元source epochの証拠として維持し、訂正後sourceのcompile成功へ読み替えない。

失敗summary／Maven log／cleanup観測は`stages/root-rerun1/`に独立保全した。初回停止は`stages/root/`に保持する。root-rerun1のcleanで旧targetを削除しているため、初回部分XMLのpath参照は現存を保証しない。保存済みの初回partial-xml-observationのhash・件数は歴史記録だけとし、次実行には流用しない。通常JARは固定しておらず、async package・Tooling・専用92件の再検証は実行していない。

同種harness訂正の委任範囲で、tmp runnerの容量走査だけを準備訂正した。走査のerrorを収集し、**DirectoryNotFoundExceptionかつ隔離source配下のtargetパス**だけをcleanと競合しうる消失として扱う。それ以外のerror（権限拒否、source／証拠領域の読取失敗、分類不能等）は引き続き停止する。容量・log・memory・DB・JVM上限は維持する。PowerShell構文解析のみ成立し、訂正runnerの実行／追加fixture／testは行っていない。

**次のOwner判断案（未承認）：このtmp訂正を固定し、通常root追加1回／600秒だけを許可する。** 開始前にsource hash・保護baseline・containerなしを再確認し、fresh XML392＝300実行＋S1 64／B2 28skipを照合する。既存test／mainの追加変更・test件数追加・追加pure網羅は0、再停止時はその結果を保全し、root自動反復しない。後続package／Toolingはroot成立後の次段階とする。

root-rerun1終了時の累計215秒・残3985秒、cleanup300秒予約。容量監視訂正と読取保全は別に台帳へ計上する。§5の追加1回は消費済みであり、残予算を新しい実行許可とみなさない。

## 8 §7のOwner承認・追加1回の実行記録

Ownerは会話で「7 承認済み追加1回の停止結果とtmp容量監視の限定訂正案」を確認・承認した。§7のtmp容量監視訂正と通常root追加1回／600秒を採用し、他上限・停止条件・cleanup300秒予約を維持する。source追加変更は0。1190 fileについてworkspace／隔離source双方のhash一致、runner構文、protected Java3件、containerなし・memory／disk floorを確認した。

runnerの`-RootRerun2`は前回STOPPED／Cleanup=trueと今回承認input hashをgateとし、単一使用の`stages/root-rerun2/`へ出力する。旧2回の記録・旧inputは維持する。今回固定は`root-correction-2/approved-inputs.json`／`restart-observation.json`。結果は以下へ追記する。

### 8.1 通常root成立結果

2026-10-12 22:44:26 JST開始、runner計上240秒。offline・async Maven profile未選択の`clean verify`がExit0／BUILD SUCCESSとなった。訂正後Reference test-compileを含む通常Reactorの実行結果であり、旧earlyやR0結果の流用ではない。

| 集合・確認 | 結果 |
|---|---|
| fresh XML | 62 file／392 invocation、failure0／error0 |
| 実行 | 300件。新規K01・K02・D03を含む |
| 計画skip | S1 7 class／64件＋B2 3 class／28件＝92件。予定外skip0、専用再検証なし |
| DB制限 | 独立selector供給でhost memory1 GiB／CPU1、準備後max_connections16の監視を通過。通常pool・既存assertion不変更 |
| 有限資源sample | PG最大1／Ryuk最大2／client最大12、各上限1／2／14以内。連続観測の保証ではない |
| cleanup／source | 所有Job・container cleanup成立、protected Java baseline保持。workspace source1190 fileのhash一致 |

`stages/root-rerun2/reconciled-results.json`にfresh XMLのhash・case名とskip分類を保存し、XML実体62 fileを同stageの`fresh-xml/`へコピーして保全した。実行summaryの`FreshXmlReconciled=false`は実行終了時点の値として改変せず、照合後の別記録と区別する。初回・追加1回目のSTOPPEDは引き続き未成立の履歴とする。

通常artifactは`normal-reference-fixed.jar`へ独立固定した。SHA-256：`BF660BEF4B76FDE1F72B58A6AD958E3DDB49D4C277D661AD78FF1B862FAF488D`。保存集合／source／資源sampleの索引は`stages/root-rerun2/fixed-result-inputs.json`。通常root成功はasync起動・V5実行・publication／providerの実動作PASSではない。

### 8.2 次工程と残予算

次は固定通常JARの内容／依存／migrationを照合し、async profileでtestを重複実行せずpackage1回、別JARとして固定・照合する。その後、Tooling D01〜02／C02〜05の6件と既存Critical Journey C01の1件を承認済み集合で明示検証する。今回これらは未開始。次runnerのroot gateは初回停止の`stages/root/`ではなく、成立した`stages/root-rerun2/`の固定hash・照合済み結果を入力とし、旧停止summaryの上書きやroot反復で代用しない。

root終了時の累計459秒・残3741秒。照合・XML／source保全を含む最終台帳は累計461秒・残3739秒（62分19秒）、cleanup300秒予約。R1全体のOwner受入・local commit・remote・R2〜R5・全DoD・正式受渡しは別判断のまま維持する。

## 9 通常artifact照合・async package初回の未成立と有限訂正案

Ownerの「通常／async artifactの照合・async packageへ進めましょう」に従い、通常root成立の`root-rerun2`をpackage gateへ接続した。停止した旧rootを改変していない。

固定通常JARの静的照合は成立。nested JAR117件、Modulithはevents-api 2.1.1だけでJDBC starterなし。main必須class、sourceの22 resource（通常設定・V1〜V5・選択設定・SPI metadataを含む）のbytes一致、fixture／test class・test依存不在を確認した。SPI metadataはrepackage後のJAR root `META-INF/spring.factories`に保存されており、この実配置を照合した。V5がJAR内に存在することと、通常起動で選択・実行されることは別である。

準備時runtime classpath132件との差は、payloadを持たないBoot starter16件のrepackage省略とjarmode-tools 4.1.1の追加。省略JARは各実体のentryを確認し、manifest／license／notice以外のruntime payloadを持たないことを検査した。その他の外部nested依存のhash一致を確認した。記録は`normal-artifact-inspection.json`。

async profile packageをoffline・`-DskipTests`で1回実行した（2026-10-12 22:59:18 JST開始、runner15秒、Maven12.125秒）。Exit0／BUILD SUCCESS、test実行0、cleanup成立。しかし保存されたasync候補は通常JARと同じSHA-256 `BF660BEF4B76FDE1F72B58A6AD958E3DDB49D4C277D661AD78FF1B862FAF488D`で、JDBC starterがなく、**ASYNC ARTIFACT NOT ESTABLISHED**。準備時async依存集合との照合が不一致となった。通常JARとsource1190 fileは維持されている。

logでは`jar:jar`がarchiveを新規作成せず、既存repackage済みJARが残っている。依存profile変更だけではarchiveを再作成しなかったことが原因と考えられる。使用中maven-jar-plugin 3.5.0の実plugin descriptorで、`maven.jar.forceCreation`の既定falseと、後処理済みarchiveを残さず再作成する設定であることを確認した。再作成後の結果による確認は未実施であり、Maven成功をasync artifact成立へ読み替えない。

今回のsummary／logは`stages/async-package/`、固定候補hashは`artifact-inputs.json`に履歴として保持する。別の`artifact-qualification.json`で未成立を明示した。Tooling／C01へ未成立候補を渡さないため、tmp runnerの後続gateにはasync静的照合PASSと通常／async hash差を追加した。package追加実行・Tooling・root再実行は0。

**次のOwner判断案（未承認）：test実行0・root再実行0・source追加変更0のまま、`-Dmaven.jar.forceCreation=true`を追加したasync packageを追加1回。** 通常JARを保持し、旧候補／input hashを保存したうえで別stage／別候補へ出力する。main／resources・通常と共通のnested依存のhash、profileで追加された実nested依存、JDBC starter／events-jdbc、既存migration bytes、fixture不在を照合する。準備probeと実appのdependency scope mediationが異なる場合は、その差を明示し、許可なしに依存やPOMを変更して帳尻を合わせない。追加取得・新規test・監視網羅へ拡張しない。

通常／async artifact段階の上限600秒を維持し、今回準備6秒＋package15秒＋照合保全2秒＝23秒計上、残577秒以内で追加1回と照合を完了する。総台帳は累計484秒・残3716秒（61分56秒）、cleanup300秒予約。再停止／再不一致では証拠を保全して停止し、残時間による自動反復はしない。

## 10 §9承認後の追加package・artifact成立結果

Ownerは§9を確認し「追加実行へ進めます」と指示した。採用案に従い`-Dmaven.jar.forceCreation=true`を加えたasync profile packageを追加1回、offline・test実行なしで実施した。同じMaven起動内で`dependency:build-classpath`をruntime scopeに限定して出力し、実Reference POMの解決集合を照合した。追加packageは2026-10-12 23:08:50 JST開始、runner40秒、Maven36.900秒、Exit0／BUILD SUCCESS、cleanup成立。root再実行・source追加変更・test実行は0。

旧候補と旧inputを保全し、新候補を`async-rerun1-reference-fixed.jar`、記録を`stages/async-package-rerun1/`へ分離した。通常固定JARのhashは変わらず、実行後のReference targetも固定通常JARへ戻した。静的照合成立後にだけ後続入力`artifact-inputs.json`を新候補へ接続した。

| artifact | SHA-256／結果 |
|---|---|
| 通常 | `BF660BEF4B76FDE1F72B58A6AD958E3DDB49D4C277D661AD78FF1B862FAF488D`、nested117件 |
| async | `45B6B63ADE3BA56C5FB5C9A5B172DFA475717DDC9A5DAACE11971F24FBD9BF5C`、nested127件、events-jdbc 2.1.1等のprofile runtime成立 |
| main／設定／migration | 両JARのmain・resource全file content一致、22 resourceはsource bytesとも一致。V1〜V4不変更、V5同梱 |
| 共通依存 | 外部nested JARはhash一致。強制再作成された内部KOIKI JAR9件はarchive hashが異なるが、全entryの名前とfile content hashが一致。container metadataの差を同一archive bytesと偽らず記録 |
| 配布境界 | fixture／test class、KOIKI ArchUnit Rules、JUnit／Testcontainers／koiki-testing等のtest専用依存なし |
| source／cleanup | workspace1190 file hash一致、protected baseline／所有Job・container cleanup成立 |

実runtime classpath145件とnested127件の差は、Boot repackageが省略するstarter18件／annotation processor1件と追加jarmode-tools1件で説明できた。Modulith JDBC／core starterはmetadataだけのJARで、nested本体へ入らない。選択成立はstarterのclasspath選択と、events-jdbc／events-core等の実runtime JARを合わせて確認した。APT JARのmanifestに`Spring-Boot-Jar-Type: annotation-processor`があり、Boot Maven plugin 4.1.1の実`JarTypeFilter`がこのtypeを除外することを確認した。

静的監視の初期判定はArchUnitを一律test依存として扱っていたが、実Modulith core 2.1.1 POMのcompile依存であることを確認し訂正した。core POM宣言1.4.2に対し既存BOM管理の解決版は1.5.0。async実runtime classpathとnested hashが一致し、KOIKI Rules／JUnitなどのtest専用artifactとは区別している。依存・POMの追加変更や検証目的の依存除外は行っていない。辞書形式とJSON読取後object形式の比較も同一形式に正規化して照合した。これらはtmpの静的照合訂正で、追加build／testは伴わない。

根拠は`normal-artifact-inspection.json`／`async-artifact-inspection.json`、実runtime classpath、`stages/async-package-rerun1/qualified-result.json`、`artifact-correction-1/approved-inputs.json`に保存した。初回async候補は未成立の歴史証拠のままとする。両artifactの静的成立はasync起動・V5 SQL実行・provider受理／publication completionの実動作成立ではない。

次は承認済みTooling D01〜02／C02〜05の6件と既存Critical Journey C01の1件。Toolingのtest-compileと実行はまだ未成立であり、今回開始していない。artifact段階の累計69秒／残531秒、総台帳は累計530秒・残3670秒（61分10秒）、cleanup300秒予約。R1結果受入・commit／remote・R2〜R5等の境界を維持する。

## 11 Tooling初回の観測停止・tmp訂正と有限再開案

Ownerは§10を確認し「Tooling新規6件と既存Critical Journey 1件へ進めます」と指示した。branch／HEAD・source1190 file・両固定artifact hashを確認し、Tooling新規2 class／6件を明示選択して初回実行した（2026-10-12 23:42:24 JST開始）。Tooling test-compileは成立したが、13秒でtmp DB監視により停止。Critical Journeyは開始していない。

停止表示は`PostgreSQL max_connections not 16.`だが、実記録はmax-connections SQLのExit137・stdout／stderr空で、値16との不一致を得ていない。`database-observation-1.json`は直前のTCP readiness Exit2、その後の`database-observation-2.json`がSQL失敗である。provider契約testは各methodのtry-with-resources終了時にDBを閉じるため、短命DBの終了と観測の競合が考えられる。ただし停止時に同DBの再inspect／稼働一覧を保存していないので、正常終了との競合・test内部例外による終了等のどれかは確定できない。表示上の設定不一致を実証済み原因へ昇格しない。

fresh XMLは0 file、成立caseは0として保全し、実際のassertion成否は未確認。S1／B2専用・root・packageの追加実行は0。Cleanup=true、protected baseline維持、workspace source1190 fileと通常／async JAR hash一致を確認した。Tooling compile成立と6件の実動作PASSは区別する。log・summary・有限DB失敗記録・`post-stop-preservation.json`は`stages/tooling/`に保全した。

同種harness訂正の委任範囲で、tmp `Containers.ps1`のobserver role準備／max_connections SQL失敗の扱いだけを訂正準備した。既存client-count queryと同じく、失敗記録を保全後に稼働container一覧を再読取し、**今回所有の同一IDが既に稼働一覧から消えた場合だけ**終了済みの観測として次へ進む。DBが稼働している場合、一覧読取不能、SQL成功だが値が16以外の場合は停止を維持する。137だけで正常終了と判断せず、未観測値をPASSにも変えない。role／grant／上限・main／test／assertion・件数は変更しない。構文解析のみ確認済み、訂正runnerは未実行。旧runnerは`tooling-observer-correction/Containers.before.ps1`に保存した。

**Owner判断案（未承認）：このtmp訂正を固定し、Tooling新規6件を追加1回だけ再開する。** 両artifact・source／baseline／containerなしを開始前に確認し、fresh XML6件・failure／error／skip0とcleanupが成立した場合だけ、未開始の既存Critical Journey 1件を初回1回実行する。Critical Journeyには固定通常JARを使用し、新規6件を再加算しない。Tooling＋Critical Journey合計1200秒の上限を維持し、初回13秒を差し引いた残1187秒以内。今回のDBやsource修正、追加test、root反復、依存取得へ拡張しない。再停止時はその結果を保全して止める。

総台帳は累計545秒・残3655秒（60分55秒）、cleanup300秒予約。通常root／artifact成立結果とR1実動作未成立を区別し、最終Owner受入・commit／remoteは未実施。

## 12 §11承認後のprovider2件成立・Ryuk上限停止と残4件の再開案

Ownerは§11のtmp訂正・Tooling追加1回と成立時のCritical Journey初回1回を承認した。訂正入力を固定してToolingを追加1回実行した（2026-10-12 23:59:52 JST開始、日付をまたぎ17秒計上）。provider契約D01／D02はfresh XMLで2件、failure／error／skip0となった。package class開始時にPG0／Ryuk2／client0を観測し、新規ToolingのRyuk上限1を超えて停止した。package4件の結果XMLはなく、Critical Journeyは未開始。

選択2 classに対して`reuseForks=false`のためclassごとに別Surefire JVMが使われ、先行provider JVMのRyukと次package JVMのRyukが終了／起動境界で重なったと考えられる。監視に保存された旧Ryukは所有session対、新RyukはPG未起動のpending session対であり、cleanupで新Ryukの所有確定ができなかった可能性がある。cleanup failureの個別理由はsummaryに保存されていないため、これを確定原因とはしない。

停止summaryのCleanup=falseは保全した。2026-10-13 00:01:20 JSTの独立読取ではprotected Java3件すべてidentity一致、稼働container0。process／container停止操作やbaseline採用は行っていない。後続観測と停止時cleanup成立は区別し、歴史summaryを変更しない。

provider2件のXML実体・case名・hashは`stages/tooling-rerun1/provider-two-fresh.xml`／`provider-two-fixed-result.json`に保全した。同じsource1190 fileと両artifact hashの一致を確認した。この2件は独立stub契約の成立であり、expense発行／publication／async listenerの実動作PASSではない。Tooling全体6件の成立へ読み替えない。

**Owner判断案（未承認）：成立D01／D02を保持し、残る`PackagedReferenceAsyncNotificationTest`のC02〜C05だけ4件を追加1回実行する。** 事前に同source／固定両artifact、保護baseline・containerなしを確認する。1 classだけを選択するため、provider classを先行させた別fork／Ryukとの重なりを避ける。test source／assertion／件数・資源上限・fork設定を変えず、新規ToolingのRyuk最大1を維持する。4件のfresh XMLとcleanup成立後にだけ、保持D2と新C4をそれぞれのepoch付きで照合して計6件とし、未開始の既存Critical Journey C01を固定通常JARで初回1回実行する。provider再実行・root／package再作成・追加pure網羅は0。再停止では保全して止める。

Tooling＋Critical Journey累積1200秒のうち初回13秒＋追加17秒＝30秒を消費、残1170秒以内。総台帳は累計564秒・残3636秒（60分36秒）、cleanup300秒予約。最終R1受入・commit／remoteは未実施。

## 13 §12承認後の残4件・Critical Journey成立とR1受入判断資料（2026-10-13）

Ownerは§12を確認し承認した。同source／両固定artifactとprovider2件の保存XMLを固定し、`PackagedReferenceAsyncNotificationTest`だけを追加1回実行した。2026-10-13 00:05:18 JST開始、runner50秒、Maven46.498秒、BUILD SUCCESS。fresh XML4件・failure／error／skip0、cleanup成立。provider2件は再実行していない。

4件のXML実体を保全し、D2とC4を別epochとして計6件へ照合した後、固定通常JARで既存Critical Journey C01を初回1回実行した。00:06:50 JST開始、runner25秒、Maven22.292秒、BUILD SUCCESS。fresh XML1件・failure／error／skip0、cleanup成立。既存Bearer／Session／業務／Security／Audit／DB／log assertionと追加async schema不在の確認を維持した。

| 承認目的 | 成立結果／証拠epoch |
|---|---|
| K01／K02：値event・stable key／payload整合 | 通常root内の新規2件、§8。early8と二重加算しない |
| D03：endpoint制限／redirect非追従 | 通常root内の新規1件、§8 |
| D01／D02：provider同key同payload受理再利用／異payload競合拒否 | 2件、`tooling-rerun1`の保存XML。whole-stage停止と後続cleanup観測を保持 |
| C02：async artifact無効時の既存承認保護 | 1件、`tooling-remaining-four`。承認・Audit成立、publication schema／stub送信なし |
| C03：通常artifactでasync有効のstarter不足拒否 | 1件、同epoch。追加／通常migration前の早期拒否 |
| C04：V4無効の正常async経路 | 1件、同epoch。異なる申請者2件のAudit／event／publication／key／provider受理／COMPLETED照合、V4非自動選択 |
| C05：明示V4＋V5併用 | 1件、同epoch。fresh DBのmigration併用・正常通知1件。consume／close操作なし |
| C01：既存Critical Journey保護 | 既存1件、`critical-journey`。固定通常JAR使用 |
| 全変更の通常統合と専用分離 | §8の392＝300実行＋S1 64／B2 28skip、予定外skip0 |
| 配布artifact境界 | §10の別固定通常／async JAR、main／resource整合、正当なruntime依存とtest専用依存を区別 |

新規9件はroot3＋Tooling6として目的へ対応し、既存C01は別1件。Rules107や早期8の重複を追加成果へ加算しない。通常rootと後続Tooling／C01はそれぞれのsource／artifact epochで成立した限定結果である。

### 13.1 最終保全と運用境界

workspace／隔離source1190 fileのhash一致、固定通常／async JAR hash不変更を最終確認した。残4件の有限sampleはPG最大1／Ryuk最大1／client最大3（上限1／1／8）、C01は1／1／4（上限1／2／14）。monitorのsample最大値をpool設定や連続上限の完全保証とはしない。最終2 stageの所有Job・container cleanup、protected Java baseline保持は成立。過去`tooling-rerun1`のCleanup=falseは改変せず、後続読取でbaseline一致／containerなしを確認した履歴を併記する。

保存XML実体は各成立stageへコピー済み。case名／hash／artifact入力は`tooling-six-fixed-result.json`、`stages/tooling-remaining-four/reconciled-results.json`、`stages/critical-journey/reconciled-results.json`、全体索引`r1-selected-verification-final.json`に保存した。各summaryのFreshXmlReconciled=falseは実行終了時の値として保持し、後続照合結果は別ファイルとする。Gitはtmp／raw／JAR／端末内browser状態を移送しない。

検証入力のbranchは`feature/phase4-s1-reference-async-r1`、基点HEADは`975af1d806dd99d7b65c9b745db5adfc1e8c4ce0`。実装は未commitのmanifest固定差分で、HEADだけで今回検証sourceを表現しない。code／test／設定／POM／V5の25 fileと結果文書を成果候補として保持する。

Tooling＋C01の実行累計は13＋17＋50＋25＝105秒（1200秒上限内）。最終照合・保全3秒を含め総台帳は累計642秒・残3558秒（59分18秒）、cleanup300秒予約。残時間を追加網羅の許可へ流用しない。

### 13.2 次の判断・残作業

承認済み最小集合について必要な追加実行はない。本節の**R1正常非同期経路・通常構成保護の限定結果**は§15でOwner受入済み。次はGit差分と対象を点検し、成果固定local commitを別承認／Owner操作で行う。push／PR／main mergeはそれぞれ別判断とする。

実provider配信・停止／再送／復旧・worker委譲・consume／close・R2〜R5・全DoD・正式受渡し・Phase 4全体の開始／受入には拡張しない。今回成立したのはローカルstubを含むR1選択正常経路であり、実運用保証や網羅完了ではない。

## 14 非同期イベントの機能面からの説明（会話内容の記録、2026-10-13）

Ownerの「この声明内容も、出来るだけそのままの内容で、記録追加してください」に従い、会話で提示した説明を以下に保持する。末尾の「判断待ち」は説明時点の状態であり、その後の受入承認は§15に記録する。

ここまでで確認できたのは、**既存の経費承認を入口に、承認イベントを保存し、別処理で通知先の受理まで進め、その処理完了を記録する正常経路**です。

流れは次のとおりです。

```text
既存の経費承認・承認Audit
  → 承認イベントを発行
  → DBにイベント処理記録を保存
  → 非同期listenerが通知処理
  → ローカルprovider stubが受理
  → イベント処理記録をCOMPLETEDへ更新
```

| 機能面 | 今回確認できた内容 |
|---|---|
| 承認とイベントの接続 | 既存の承認処理から、申請ID・申請者ID・承認version・金額などの値を持つイベントを発行する経路が成立しました。 |
| モジュール間の非同期連携 | expenseからnotificationへ、標準のmodule listenerを通じて処理を渡し、通知先の受理とpublicationの完了まで確認しました。 |
| 通知の取り違え防止 | 異なる申請者の2件について、承認Audit・イベント・通知key・provider受理記録・COMPLETEDがそれぞれ対応することを確認しました。 |
| 重複受理の扱い | provider stubでは、同じkey・同じ内容なら元の受理結果を返し、同じkeyで内容が違えば拒否します。この契約を独立に検証しました。 |
| 設定・migrationの選択 | async無効時は既存承認を維持し、追加schemaや通知送信を開始しません。必要なruntime依存なしで有効化すると早期拒否します。V5単独と、明示したV4＋V5の併用も成立しました。 |
| 既存Referenceの保護 | 通常rootと既存Critical Journeyで、従来の業務・認証・認可・Auditなどの確認を維持しました。 |

引き継いだのは、**R0で整えたmodule単位のLevel選択規約、既存の承認・Security・Audit、そしてS1／B2専用検証を通常buildから分離する扱い**です。その上に、今回の正常非同期経路を接続できました。

一方、今回の **COMPLETEDは「ローカルstubの受理後、listener処理が正常終了した」こと**を示します。実際のメール等が相手へ届いた保証ではありません。また、停止・障害時の再送、再起動後の復旧、処理途中の競合、実providerとの照合は今後の範囲です。通知全体の「必ず一度だけ届く」という保証も、今回の結果からは主張しません。

根拠は結果文書§13です。R1の限定検証は成立し、結果のOwner受入は現在判断待ちです。

## 15 R1限定結果のOwner受入承認（2026-10-13）

Ownerは会話で、§14の説明内容の記録追加を指示したうえで、**「結果文書§13 内容を確認承認といたします」**と明示した。

これにより、§13のR1正常非同期経路・通常構成保護に関する承認済み最小検証結果を **COMPLETE / OWNER ACCEPTED** とする。通常root300実行＋専用92skip、通常／async artifact静的照合、新規9件（root3＋Tooling6）、既存Critical Journey1件と、source／artifact epoch・cleanupの区別を含む保全結果を受け入れた。過去のSTOPPED／Cleanup=false記録は改変しない。

受入は§13・§14で説明した限定保証範囲に従う。実provider配信、停止／再送／復旧、worker委譲、consume／close、R2〜R5、全DoD、正式受渡し、Phase 4全体の開始／受入へ拡張しない。

この判断は成果固定local commitやremote操作の実行承認を兼ねない。次の残作業はGit差分・成果固定対象の点検と、別承認／Owner操作によるlocal commit。今回の更新は機能説明・受入記録と現行状態の整合に限定し、code／test変更・追加検証・commit／push／PR／mergeは行っていない。
