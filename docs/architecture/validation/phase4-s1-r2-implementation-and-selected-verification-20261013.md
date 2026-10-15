# S1 R2 Tooling作成・選択検証と停止記録（2026-10-13）

状態：Tooling4 file作成・compile／package成立。新規8件中7 PASS／1 FAIL、直接回帰4件は未実施。R2未完了／Owner受入未判断、追加再実行・production修正は未開始。

## 1 承認・入力と作成範囲

[R2開始票§8](../../development/phase4-s1-r2-purpose-and-minimal-verification-owner-review-20261013.md#8-owner承認記録と文書固定対象2026-10-13)、[preflight訂正承認・成立§5〜7](phase4-s1-r2-source-and-preflight-20261013.md#5-訂正承認とoffline確認の成立)、Ownerの「了解です。このまま進めましょう」に従い作成・初回検証した。

branchは`feature/phase4-s1-reference-failure-isolation-r2`、固定commitは`cfe9d9efb4cb2bbcdc273df578c75225b539bc08`。Toolingは未commit差分であり、commit IDだけで検証sourceを表現しない。次の4 fileだけを作成・変更した。

| file（すべてreference-e2e-verificationのtest package内） | 作成内容 |
|---|---|
| PackagedReferenceAsyncFailureIsolationTest.java（新規） | 明示R2 selector、新規8 invocation。正常対照を別に選択して先行実行 |
| R2DatabaseFailureInjection.java（新規） | 今回DBの対象別BEFORE INSERT trigger、P0001、rollbackと独立したsequenceによる到達証拠、撤去 |
| AsyncReferenceFixture.java（既存） | submitted／approve分離、主体別login・正当CSRF、scope／version／同期veto操作、対象DB観測、有限待機・resource sample・所有記録・独立cleanup |
| LocalNotificationProviderStub.java（既存） | 独立した到着要求台帳とstatus、受理前503 mode、観測失敗を隠さない処理。既定受理契約は維持 |

Framework／Reference production、POM／dependency／migration／Security／CIは変更していない。承認・Audit・publicationを同transactionに保存し、commit後に非同期通知する境界を維持する。

runは`tmp/r2-cfe9d9e-20261013-preflight/`。`implementation-inputs.json`は初期compile入力、`implementation-selected-tests.json`はCSRFの既存meta形式対応を含む実行sourceの4 file hash。未commitのpreflight EvidenceをTooling差分へ混ぜない。

## 2 compile・artifact

Toolingの初回test-compileはBUILD SUCCESS、Maven4.732秒。testは実行していない。既存同様のError Prone AddressSelection warningあり。以後のtest lifecycleはCSRF meta対応差分を必要compileした。

通常／async packageは固定sourceのReferenceと必要reactorを`-am`で各初回1回、offline・`maven.test.skip=true`・`maven.jar.forceCreation=true`で作成した。通常19.004秒、async19.065秒、両方BUILD SUCCESS。全testを省略し、通常root300実行＋専用92skipを今回の実行結果へ転記しない。

| 固定JAR | SHA-256 |
|---|---|
| normal-reference-fixed.jar | `547559E92C0D6B500AD1AB9364722E29B345BC60B5BF92845813825CF1F81A09` |
| async-reference-fixed.jar | `B12F1A9AD7CB4471B9C08D5AAAE9D3F53F86B1EED2C85DBBFF446EE37FB4EDAD` |

181 main／resource fileのbytes一致、通常にevents-jdbcなし・asyncに2.1.1あり、KOIKI Rules／Tooling／Playwrightの配布JAR混入なしを照合。Modulith由来のruntime ArchUnitとKOIKI test Rulesを混同しない。旧PC R1 JARとは別epochで、旧hash不一致を旧受入失敗としない。

検証時はMaven heap768 MiB、forkCount1／reuseForks=true、fork heap768 MiB、Reference heap768 MiBを明示した。compile／packageのcompiler fork上限も768 MiB。一方、先行compile／packageのMaven本体へはこの明示MAVEN_OPTSを設定していなかったため、その最大heap上限の実証は未取得。package成果の成立と全phaseの資源条件証明を区別し、検証全体が完全に資源条件を実証したとは報告しない。

## 3 初回case結果

正常対照を初回1回実行し、2026-10-13 11:24:14 JST終了、Maven17.387秒、fresh XML1件・failure／error／skip0で成立。cleanup後、負例7件を別Maven集合の初回1回で実行し、11:27:18 JST終了、Maven約95秒、fresh XML7件・failure1／error0／skip0。正常対照は再実行していない。

| case | 結果と実証範囲 |
|---|---|
| R2-08 正常対照 | PASS。承認・成功Audit・event／key／payload・独立stub受理1・publication COMPLETED |
| R2-01 provider失敗 | PASS。受理前503の要求到着1／受理0、対象publication FAILED／completion_dateなし、承認APPROVED／version3・成功承認Audit1を保持。再送なし |
| R2-02 publication保存失敗 | PASS。対象trigger到達1、承認503、SUBMITTED／version2保持、成功承認Audit0・publication0・到着／受理0、5秒有限観測と終了後照合 |
| R2-03 承認Audit保存失敗 | PASS。対象trigger到達1、承認503、状態・version保持、成功承認Audit0・publication0・到着／受理0。Security Audit全不在は要求していない |
| R2-05 scope外 | PASS。404拒否、状態・version保持、成功承認Audit／publication／到着／受理0 |
| R2-06 古いversion | PASS。409拒否、状態・version保持、成功承認Audit／publication／到着／受理0 |
| R2-07 同期veto | PASS。master部門無効化409、active／version保持、成功無効化Audit0、expense保持、publication／到着／受理0 |
| R2-04 承認能力不足 | **FAIL。期待403に対して500。** HTTP assertionで停止したため、その後の状態・Audit・publication・到着／受理のassertionは未到達。副作用なしをPASSとしない |

保存XMLは`normal-control-fixed.xml`と`negative-seven-fixed.xml`。新規8件は7 PASS／1 FAIL。直接回帰D01／D02／C02／C04の4件は未実施で、既存成立結果を今回PASSへ転記しない。R0／R1の旧結果は保持する。

Surefireはnative streamに関するcorrupted-channel warningを出した。case名・fresh XML実体を照合し、warning自体を消さず、失敗の原因と混同しない。dumpstreamはTooling targetにありGitでは移送されない。

## 4 失敗原因の読取診断と停止

能力不足の子logは`r1-child-12001314864996355004.log`。11:27:17.553 JSTに`KoikiProblemDetailsExceptionHandler`がUnexpected request processing failureを記録し、例外は`AuthorizationDeniedException: Access Denied`であった。

固定sourceの`koiki-starters/koiki-starter-api/.../KoikiProblemDetailsExceptionHandler.java`はLOWEST_PRECEDENCEのglobal adviceで、`@ExceptionHandler(Exception.class)`が例外を500 ProblemDetailへ変換する。Reference Expense MVC adviceは業務例外とResponseStatusExceptionを処理するが、AccessDeniedの専用処理はない。**method securityで拒否した例外がglobal fallbackへ捕捉され、500となる経路**をlogとsourceで照合した。非同期listener失敗やCSRF不正を原因に読み替えない。

既存Reference／Frameworkの例外処理境界に関わるproduction修正の可能性があり、Tooling-only承認を超える。開始票の停止条件に従い、期待値403を500へ変更せず、直接回帰4件・新たなcompile／package／test再実行・production修正を停止した。正常／残6件を再実行して失敗を隠さない。

## 5 資源・cleanupと限界

正常対照の有限DB sampleはclient3。負例集合のsampleはPG1／Ryuk1、host Java5のうち既存IDE2・今回所有3、空きmemory11.46 GiB、DB client3。PG memory1 GiB／CPU1とclient8以下をfixture内で照合した。有限sampleであり、全期間の連続上限保証ではない。

負例集合終了後の権限付きread-only診断で稼働container0、Javaは保護対象PID30304／25248だけ、creation identity不変を確認した。各caseのfixture closeによる子process終了・DB停止もXML内で成立している。今回所有の終了・protected baseline保持を確認し、他sessionのprocess／containerへ操作していない。

run直下42 file・約135.1 MBは固定JARを含む。子log8 fileの最大210946 bytesで、各10 MiB上限内。JARを含むrun2 GiBと、raw100 MiBを区別する。rawの最終合計・source／artifact／XML hashを停止結果索引に保存する。旧runを上書きしていない。

停止結果索引`r2-selected-verification-stopped.json`で、XML8件の7 PASS／1 FAIL、実行source4 file hash不変更、両固定JAR hash不変更、承認Tooling test以外の追跡source差分0、raw909929 bytesを照合した。`r2-budget-stopped.json`は前台帳158秒に、preflight記録終了から今回停止までのwall-timeを加えた保守的な累積上限を保存する。除外可能な文書時間・Owner待ちも含むため実稼働時間そのものではない。残時間は追加再実行・production修正の許可へ流用しない。

## 6 Owner判断用の最小対応案

最初に、能力不足の403契約を戻す**Reference MVC限定修正で扱うか、Framework global exception／Security境界の別reviewへ戻すか**を判断する。Framework一般契約の修正を今回の4 fileへ紛れ込ませない。

Reference限定案の候補pathは`koiki-reference-app/src/main/java/org/koikifw/reference/expense/adapter/inbound/web/ExpenseWebExceptionHandler.java`。SpringのAccessDeniedを既存MVC403へ接続する方式を具体化し、Security Audit・default deny・CSRFを保護する。方式採用と修正開始は未承認。Security拒否を独自業務例外に変換したり、新しいpermissionを追加してケースを通す方法は採らない。

採用する場合の最小検証候補は、MVCの拒否・Security Auditを確認する対象限定test、失敗R2-04の追加1回、production変更に必要な通常root1回、package正常保護／既存Critical Journeyと未実施直接回帰4件から影響に応じた集合。具体path・case・package再固定・新予算／停止／cleanupを訂正開始票としてOwnerへ提示してから実行する。成立済みR2-01〜03／05〜08は保存結果を維持し、再実行を一律追加しない。新artifact結果は新epochで区別する。

今回の作業は未commit。成果commit／remote、R2結果受入、上記修正・追加再実行、R3〜R5は別判断を維持する。
