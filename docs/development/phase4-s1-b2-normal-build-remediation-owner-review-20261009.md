# Phase 4 S1 B2通常build対策 Ownerレビュー案

作成日：2026-10-09。状態：`OWNER APPROVED / SOURCE FIXATION AND PREFLIGHT PENDING`（§9）。

B2初回issue／readの対象3テストが通常Surefire対象となり、Root Reactorが準備しないTooling target、元端末の固定tmp classpath、WindowsのJava実行pathを要求する。通常buildからこの限定検証を分離し、明示実行時には新しい準備出力を指定するA案を提案する。既存Frameworkと検証済みReferenceへの影響を最小化するため、Reference main・Framework・POM・依存・CIを変更せず、B2のtest実行条件・process bridge・専用準備手順に限定する。

入力は[通常build対策引継ぎ](phase4-s1-b2-normal-build-remediation-session-handoff-20261009.md)、[S1目的対応表](phase4-s1-purpose-dod-stage-mapping-20261009.md)、[軌道修正案](phase4-s1-async-reference-direction-correction-draft-20261008.md)である。確認sourceは`feature/phase4-s1-reference-foundation` / `6939799a48ed0c4e876bf92e74c22a7dc7453918`。確認基点からのtracked差分はなく、引継ぎ・目的対応表は未追跡文書である。

本案は後続作業のreview入力であり、実装・Maven／Docker検証・local commit・remoteの開始承認ではない。B1／B2の受入済み結果と旧manifestは保持する。

## 1 採用を提案する方式

| 項目 | A案の具体方式 |
|---|---|
| 対象3クラス | `B2ProtectedIssueReadTest`（12件）、`B2IssueBoundaryTest`（8件）、`B2IssueTransactionTest`（8件） |
| 既定の実行条件 | class単位の`@EnabledIfSystemProperty(named="koiki.b2.reference-acceptance.enabled", matches="true")`を追加。property未指定では対象28件を無効にし、`@BeforeEach`のprocess／DB準備を起動しない |
| 明示実行 | propertyを`true`にし、対象classを1つずつ指定。前提不足はskipにせず、子起動前の固定分類のエラーとして失敗する |
| classpath入力 | test用property `koiki.b2.tooling.classpath-file`で絶対pathを指定。固定tmpへのfallbackを削除。ファイルの存在・非空・有限量・各entryの存在を子起動前に検査する |
| Tooling source指定 | repository探索と既存Tooling module配置は維持。coordinatorのclass、B1 helper、SQL resource、jdbc package JARの存在を子起動前に確認。外部の任意command／main classは入力にしない |
| Java起動path | bridgeは`java.home/bin`内でOSに対応する`java`／`java.exe`を選び存在を検査。ただし今回のB2明示実行はWindowsだけを許可し、他OSでは子起動前に失敗させる |
| 保護・業務assertion | 件数、case ID、凍結protocol、writer遮断、60秒受付窓、認可・Audit・rollback等は変更しない |
| 登録8件 | `B2FrozenSourceRegistrationTest`はlive supplier不要のため、実行条件を追加せず通常build対象を維持 |

JUnitのclass単位条件はpropertyとの正規表現一致で有効化できる。[JUnit公式API](https://docs.junit.org/5.9.0/api/org.junit.jupiter.api/org/junit/jupiter/api/condition/EnabledIfSystemProperty.html)。CLIのuser propertyをforkへ渡すSurefireの仕様は[公式test goal](https://maven.apache.org/surefire/maven-surefire-plugin/test-mojo)に従う。実際の解決済みJUnitとSurefire 3.5.6で、無効化・有効化・property伝播を検証する。

### OS対応の範囲

bridgeの先には既存`B1ProcessHarness.java`の`java.exe`、`B1ResourceLimits.java`の`jcmd.exe`依存がある。bridgeだけを修正しても、LinuxでB2の全明示実行が成立するとは言えない。今回はB1変更へ広げず、Linuxの通常buildでは28件を既定無効にし、B2の明示実行はWindowsに限定する。

LinuxでのB2明示実行を必要とする場合はB1 helperを含む別reviewへ進む。Linux通常buildの実行結果もWindowsのPASSから推定しない。現行Linux CIの実測は、remote個別承認後の確認事項として残す。

## 2 変更pathと変更量

以下のpathはRepository rootからの相対path。所有者はtest／検証手順についてTooling、計画・Evidence文書についてFramework側の文書管理である。Reference module内のtest配置を移動せず、Reference mainからTooling Java型への依存を追加しない。

| 区分 | path | 許可を求める差分 |
|---|---|---|
| 既存test 1 | `koiki-reference-app/src/test/java/org/koikifw/reference/notification/B2ProtectedIssueReadTest.java` | importとclass単位実行条件だけ |
| 既存test 2 | 同directoryの`B2IssueBoundaryTest.java` | 同上 |
| 既存test 3 | 同directoryの`B2IssueTransactionTest.java` | 同上 |
| 既存helper 1 | `koiki-reference-app/src/test/java/org/koikifw/referenceacceptance/notification/B2FrozenSourceProcess.java` | classpath明示入力・有限前提検査・OS／実行path確認。前提検査をdirectory作成と子起動より前に配置。起動失敗時の作成済み一時file整理を必要最小限で追加 |
| 新規script 1 | `build-support/phase4-level2-verification/scripts/prepare-b2-reference-acceptance.ps1` | Windowsの有限準備だけ。新規run path必須、compile／package／classpath生成、source／artifact台帳、失敗時停止。DB・子JVM・受入testは起動しない |
| 既存README 1 | `build-support/phase4-level2-verification/README.md` | 通常buildと明示実行の区別、準備・実行・cleanup、OS範囲、新旧Evidenceの区別を追記 |
| 新規Evidence 1 | `docs/architecture/validation/phase4-s1-b2-normal-build-remediation-20261009.md` | 変更後source、準備、検証集合・exit／XML・資源・cleanup、旧結果との対応と限界を記録 |

実装・Evidenceの上限は**新規2＋既存5＝7件**（script・Evidence、test3・helper・README）とする。本レビュー案と承認記録の文書更新は別枠。新test class・新helper型・POM変更は含めない。必要差分がこの範囲を超える場合は停止して追加判断へ渡す。

変更しない対象はReference／Tooling main、既存B1 source・test・SQL、migration、Security、通常properties、Parent／BOM／Root Reactor／POM／依存、Framework API／Rules、CIである。既存`scripts/verify-s1-b2.ps1`は受入履歴scriptとして保持し、新しい準備scriptから起動しない。

## 3 source固定と検証環境の準備

実行の開始条件として、Ownerによる本案の範囲・上限の承認と承認文書のcommit、clean source固定を必要とする。未追跡の引継ぎ・目的対応表を無断削除しない。local commitはOwner操作または操作前確認に従う。元端末のraw・JAR・target・cacheを検証用sourceへコピーしない。

検証用sourceは承認済みcommitから`tmp/b2-normal-build-<新規run ID>/source/`へGit archiveで展開する案とする。新規pathであることとworkspace内に解決されることを確認し、元source／targetを保護する。必要なGit管理fileが全部含まれることを確認し、欠落があれば実行を止める。rawと台帳は同runの`evidence/`、classpathは`inputs/`に置き、`target`外で保全する。

検証用Maven local repositoryは同runの`m2/`とする。既存cacheは不足artifactの候補を読むだけとし、元cacheの書換え・旧受入JARの手動コピーを行わない。有限の事前依存解決で必要なplugin・依存・Wrapperを揃え、取得結果のversion／hashを記録する。ネットワーク・cache権限は実行環境の承認手順に従う。取得不能なら停止し、POMやversionを変えて吸収しない。

JDK 21、Wrapper Maven、Docker Server、PG17／Ryuk、memory／disk、当該process・container baselineを確認する。既存PIDを終了対象に流用しない。通常root buildに必要な各moduleの実行資源もpreflightで照合し、下記上限に適合できない場合は実行前にOwnerへ戻す。既存testのpool・assertionを変更して上限へ合わせない。

## 4 明示実行までの手順

以下は承認後のcommand構成案であり、今回実行しない。Windowsでは新sourceのrootを作業directoryとし、`$runRoot`はworkspace内の新規run絶対path、`$runM2`はその`m2/`、`$cpFile`はその`inputs/tooling-jdbc-test-classpath.txt`とする。各Maven processへ`MAVEN_OPTS=-Xmx768m`を一時供給し、`finally`で復元する。

1. Tooling未準備・旧tmp未コピーの状態でroot `clean verify`を1回実行する。対象28件の既定無効、登録8件とその他既存testの結果を保存する。
2. 同じsourceをroot `-DskipTests install`で1回準備し、専用m2へFramework／Reference artifactを供給する。sourceからの標準buildだけを使い、旧cacheとの同一性を仮定しない。
3. 新準備scriptでToolingのjdbc main JARをpackageし、`jdbc,s1-contract,s1-web`でtest-compileする。classpathは`jdbc`単独の**test scope**から生成する。
4. B2明示実行を1 classずつ、登録8→I12→B8→X8の順に実行する。Toolingの無変更をsource hashで照合し、Tooling18件はこの限定変更のために再実行しない。
5. fresh XML／exit、package、source・artifact対応、cleanupを確認し、新Evidenceへまとめる。

Tooling coordinatorはtest classであり、B1 helperからJUnit・Testcontainersも利用するため、mainのruntime scopeだけでは起動classpathを満たさない。`jdbc`単独でtest scopeを解決し、`s1-contract,s1-web`のruntime混入を避ける。Maven Dependency Pluginの`test`は全scopeを含む。[公式build-classpath goal](https://maven.apache.org/plugins/maven-dependency-plugin/build-classpath-mojo.html)。この端末には3.7.0のcache directoryがあるが、JAR／依存の利用可能性はpreflightで確認する。

```powershell
# 承認後、検証用sourceのrootから。変数は新規runの絶対pathを事前検査する。
.\mvnw.cmd -o -B -ntp "-Dmaven.repo.local=$runM2" '-DargLine=-Xmx768m' '-DforkCount=1' '-DreuseForks=false' '-Djunit.jupiter.execution.parallel.enabled=false' clean verify
.\mvnw.cmd -o -B -ntp "-Dmaven.repo.local=$runM2" '-DskipTests' install

.\mvnw.cmd -o -B -ntp "-Dmaven.repo.local=$runM2" -f build-support/phase4-level2-verification/pom.xml -Pjdbc '-DskipTests' '-Dmaven.test.skip=true' clean package
.\mvnw.cmd -o -B -ntp "-Dmaven.repo.local=$runM2" -f build-support/phase4-level2-verification/pom.xml '-Pjdbc,s1-contract,s1-web' '-DskipTests' test-compile
.\mvnw.cmd -o -B -ntp "-Dmaven.repo.local=$runM2" -f build-support/phase4-level2-verification/pom.xml -Pjdbc org.apache.maven.plugins:maven-dependency-plugin:3.7.0:build-classpath '-DincludeScope=test' "-Dmdep.outputFile=$cpFile"

# 登録8件は有効化property不要。先に別commandで実行する。
.\mvnw.cmd -o -B -ntp "-Dmaven.repo.local=$runM2" -f koiki-reference-app/pom.xml '-Dtest=B2FrozenSourceRegistrationTest' '-DargLine=-Xmx768m' '-DforkCount=1' '-DreuseForks=false' surefire:test

# I12の例。B8、X8も各classを別commandにし、同時実行しない。
.\mvnw.cmd -o -B -ntp "-Dmaven.repo.local=$runM2" -f koiki-reference-app/pom.xml '-Dtest=B2ProtectedIssueReadTest' '-Dkoiki.b2.reference-acceptance.enabled=true' "-Dkoiki.b2.tooling.classpath-file=$cpFile" '-DargLine=-Xmx768m' '-DforkCount=1' '-DreuseForks=false' '-Djunit.jupiter.execution.parallel.enabled=false' surefire:test
```

root `clean verify`では対象propertyを渡さず、Tooling target・固定tmpがないことを実行前後に記録する。Surefireの無効case数だけでは子未起動を保証しないため、B2 process／ready／directoryが作られないことも確認する。root既存回帰の総件数は承認済みsourceのcase台帳をpreflightで固定し、既存skip条件と対象28件の無効化を区別する。

専用classpathのentry一覧とhash、Tooling JARとtest classes／resourceのhashを採取する。生成後の`clean`はtest-classesを消すため、Toolingを再cleanせず明示実行する。実解決classpathとpackage内依存の対応を確認し、不一致・元targetの生成履歴UNKNOWNを修正済みとは扱わない。

## 5 検証集合と判定

| 集合 | 回数と判定 |
|---|---|
| 通常root build | Windowsのclean検証用sourceで`clean verify`1回。全選択既存回帰の成立、対象28件の無効化、B2外部fixture未起動。登録8件は既定対象のまま |
| 明示B2 | 登録8＋I12＋B8＋X8＝36件を各class1回。登録8はroot側の結果と別runで記録し、合算して新case44件とは扱わない。明示28件はfailure／error／skip0・exit0が必要 |
| 前提不足の負例 | `B2ProtectedIssueReadTest#I01_realSaveAuditAndScopedRead`を選び、有効化trueでclasspath未指定／不存在／空fileの3 commandを各1回。意図した固定分類の失敗・子／DB未起動・一時file残留なしを確認。期待失敗は36 PASSへ加算しない |
| package | root生成Reference JAR・Tooling jdbc JARについてhelper／fixture／test SQL非混入。意図した既存Reference mainの包含と、依存／構成非変更を照合 |
| Linux条件 | 対象28件のclass条件、Linux CIからpropertyを渡していないこと、通常buildからTooling前提を除いたことをsourceで点検。Linux実行PASSは未認定、CI実測は別remote承認 |

Tooling18件・B1全件・旧B2回帰301件の再演、package済みbrowser E2Eの追加実行は含めない。Reference main・依存・通常構成が変わらないため今回の出口は通常root buildとB2実行条件の成立とする。rootで失敗した既存testの原因修正へ自動拡張しない。

## 6 資源と時間と再実行の上限案

以下は本作業専用の新しい上限案であり、旧B2残予算を流用しない。Owner承認後に適用する。JVM数は既存IDE等のbaselineを除き、Maven・fork・coordinator・通常子・jcmd等を含む。

| 項目 | 上限案 |
|---|---|
| 開始時の余力 | available memory 8 GiB以上、disk 10 GiB以上。専用m2・source・targetの見積りがdisk枠を超える場合は取得／実行前に停止 |
| 通常root build・artifact準備 | 当該JVM最大4、PostgreSQL container最大2、DB接続総数最大24。既存test設定は維持し、preflightで適合確認。Ryukは別に1まで |
| 明示B2 | 常駐する当該JVM最大4（Maven／fork／coordinator／通常子）。既存jcmd診断が重なる最大10秒だけ総JVM5を許容する案。PostgreSQL container1、DB接続8。常駐4 JVMのheap各768 MiB。既存B1の実効Hikari・PG制限とjcmd確認を維持 |
| 並列実行 | Maven commandとclassは逐次。JUnit parallelを無効化。rootの既存設定に並列があれば上限成立を実行前に確認 |
| 新規準備script | 1回だけ。Maven goalは上記package・test-compile・classpath生成各1回。暗黙のtest／DB起動禁止 |
| 検証・環境管理時間 | 合計90分。preflight・有限依存準備15分、root verify30分、標準installとTooling準備20分、明示36件15分、前提負例・package照合5分、cleanup予約5分。段階枠と総枠の両方を維持 |
| 作業量 | 実装・文書・診断のAgent作業6時間で進捗確認、8時間で停止し残課題を提示。Owner待ちは分けて記録 |
| 証拠容量 | sanitized raw 256 MiB、個別子log 1 MiB、classpath file 1 MiB／entry512以下。専用source・m2・targetを含む全run disk増分10 GiB以内 |
| 再実行 | 自動再実行0。承認済みcommandが失敗・上限超の場合は保全・cleanupして停止。原因修正・追加runは別判断 |

root用資源枠は既存Frameworkの多module回帰を実行するための本案の提案値であり、旧B2の4 JVM／PG1／接続8の拡大承認が済んだとは扱わない。明示B2の常駐4 JVM／PG1／接続8は維持するが、jcmdを数えた瞬間の5 JVMは今回の明示判断事項とする。過去のsample最大4という記録だけから、jcmdを含む全瞬間の上限成立を推定しない。root集合・各pool／container寿命をpreflightで確認できなければ実行を先行しない。

## 7 cleanupと停止条件

B2 testと既存helperの通常終了・finally cleanupを維持する。各class後に当該coordinator・通常子・jcmd・DB／Ryuk・接続・port・ready／partial file・一時設定を確認してから次へ進む。検証用targetに残る有限sanitized logは証拠として識別し、readyや接続情報とは分ける。

前提負例は子起動前に判定し、作成した空classpath等の負例file以外に副作用がないことを確認する。親の`MAVEN_OPTS`・検証property／ENVの残留を確認する。元source・旧raw・target・m2・既存開発DBに対するcleanupを行わない。検証用source・m2・rawは証拠固定まで保管し、今回recursive削除しない。

失敗・不一致・上限超・cleanup失敗・source変化・秘密露出・未知接続／processを確認したら次の実行を止める。当該所有process／containerだけを識別し、通常終了を優先する。強制終了が必要な場合は所有PIDと開始時刻／run IDを確認し、記録して残留を再確認する。未知の接続・既存IDE／containerを無断停止しない。

exit・開始終了時刻をlog整形より先に保存し、command／fresh XML／source・artifact hash／資源観測／cleanup結果を新Evidenceへ残す。秘密・URL・credentialを除去した有限logだけを提示する。失敗rawを成功runで上書きせず、予算内だからと診断反復しない。Docker named pipe／m2権限エラーはAGENTSと実行環境の権限付き承認手順に従い、迂回しない。

## 8 Ownerに判断を求める範囲

1. A案のclass条件・明示classpath・子起動前前提検査を採用し、§2の7件だけの作成／変更を許可するか。
2. Linux通常buildへの対策とWindows専用B2明示実行を今回の範囲とし、B1を含むLinux明示実行対応を別reviewに分けるか。
3. 承認文書commit・clean source固定後のpreflightと、成立時の§3〜§7の隔離source／専用m2準備、root1回・明示36件・期待失敗3回・package確認を、上限付きで開始するか。限定fixtureの子JVM／DB・writer遮断等は旧B2と同じ有限副作用として含むが、今回の個別許可を必要とする。
4. 結果受入、local commit操作、remote／Linux CI実測は後続の個別判断とするか。

肯定consume／close、Reference非同期runtime・sender／listener／runner、publication／migration、Framework変更、広いTooling整理、B2方式の廃止、DoD／正式受渡し／Phase 4全体へ拡張しない。通常build対策の結果を受け、[S1目的対応表](phase4-s1-purpose-dod-stage-mapping-20261009.md)の作業順に従ってR0開始判断資料へ進む。

## 9 Owner承認記録

2026-10-09、Ownerは本レビュー案を確認し、次のとおり明示した。

> Phase 4 S1 B2通常build対策 Ownerレビュー案 を確認し、承認します。改修へ進めましょう

§1〜§8の方式・7件の変更範囲・Windows明示実行・検証集合・資源／時間／作業量／再実行上限・cleanup／停止条件を承認済みとする。承認文書commit・clean source固定後のpreflightと、成立時の限定改修・検証へ進む。同じ範囲の開始承認を再要求しない。

本承認はlocal commitの操作前確認、結果受入、remote／Linux CI実測の個別判断を兼ねない。最初に本書・通常build対策引継ぎ・S1目的対応表の3文書を固定する操作についてOwner確認を得て、clean sourceを確認する。preflight不成立ではcode作成・検証を先行しない。
