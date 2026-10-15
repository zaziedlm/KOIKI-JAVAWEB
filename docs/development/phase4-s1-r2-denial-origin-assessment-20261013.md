# R2能力不足500：非同期導入と既存例外処理境界の切り分け（2026-10-13）

状態：READ-ONLY ASSESSMENT / OWNER REVIEW INPUT。追加test・package・production修正・依存変更は未実施。

**後続比較結果（2026-10-13、§6）：** Ownerが固定通常JARでの比較1件を指示し実施した。normal／async=falseでもHTTP500、method Security拒否→同じglobal fallback経路が成立。少なくとも今回sourceでは非同期有効化は発生の必要条件ではない。以下の通常比較未実測・未承認は調査時点の履歴。production修正・依存追加・追加回帰は未開始。

## 1 現時点の結論

今回の500は、同期承認HTTP要求のmethod security拒否を、APIのglobal exception handlerが500へ変換した結果である。[失敗記録§4](../architecture/validation/phase4-s1-r2-implementation-and-selected-verification-20261013.md#4-失敗原因の読取診断と停止)が直接の実行証拠。

**非同期listenerやprovider処理の失敗を、承認HTTPが500として返した事象ではない。** sourceと履歴比較は、従来からある例外処理境界の問題がR2で発見された可能性を強く示唆する。一方、通常JARで同じ能力不足HTTPを実測していないため、「非同期無効でも再現する」「R1前にも実際に同じ500だった」は未実証として残す。

APIにSecurity依存を追加する判断も、Reference局所修正を採用する判断も、この時点では固定しない。発見した工程・発生した構成・原因となる処理を区別する。

## 2 比較sourceと確認済み事実

比較点はR1作成前の`975af1d`と、現在固定点`cfe9d9e`。後者にはR1成果`610c589`を含む。現在のTooling4 fileは未commit差分であり、比較対象productionを変更していない。

| 調査点 | 確認済みの事実 | 意味と限界 |
|---|---|---|
| API Starterのhandler・auto-configuration | R1前後のsource差分なし。handlerの履歴はPhase 1b CP3の`cdfdebe`。汎用Exceptionを500へ変換し、Securityには依存していない | 500への変換処理はR1で追加したものではない。過去の実起動でのhandler登録・同じ拒否HTTPの結果までは証明しない |
| Security Starter | R1前後のsource差分なし。EnableMethodSecurity・default denyの既存構成 | R1で能力拒否契約を変更した証拠はない |
| expense MVC controller／advice、browser Security chain | R1前後のsource差分なし。Security拒否の専用MVC handlerなし | 同じ応答境界が通常構成にも存在するsource上の根拠 |
| ExpenseApplicationService.approve | PreAuthorizeのEXPENSE:APPROVE条件は不変更。追加hookは状態更新・flush・承認Auditの後 | 能力不足の拒否はevent発行hookより手前で起きる。今回logはmethod proxy→MVC controller→global handlerを示す |
| async selector | falseではevent Portがno-op、registry auto-configurationを除外。trueではpublisher／listener／executor／JDBC store／追加DDLを選択 | selectorにAPI adviceやSecurity応答契約を切り替える処理は見つからない。ただしruntime classpath・proxy構成は異なり、読取だけで全影響不存在とは断定しない |
| 固定normal／async JAR | R2のartifact照合で181 main／resourceのbytes一致。runtime library集合は異なる | normalも同じ承認・Security・MVC・global handler codeを含む。異なるruntime libraryによる影響は実測比較が必要 |

今回のlogではAuthorizationDeniedExceptionが同期Servlet request threadで発生した。非同期通知threadのprovider例外は別case R2-01であり、そちらは承認を保持するPASS結果だった。二つの例外を同じ原因として扱わない。

## 3 既存testで成立していること・覆っていないこと

`ExpenseApplicationServiceMethodSecurityTest`は能力不足からAccessDeniedExceptionを投げることを検査する。MVC resolver・HTTP変換・global advice・DB保存を通した検査ではない。

`ReferenceBusinessUrlSecurityTest`の既存403には、master URLでの権限拒否やCSRF拒否が含まれる。これらはMVCのApplication methodから投げられた例外をglobal adviceが処理する経路と異なる。既存MVC sliceのApplication Beanはmockであり、実method securityを通すR2-04の代替証拠ではない。

通常Critical Journeyの成立は、選択した正常・拒否経路の保護証拠であって、今回の「認証済み主体、正当CSRF、EXPENSE:APPROVE不足、承認POST」の全条件を覆った証拠ではない。既存root／E2E PASSと今回FAILは直ちに矛盾しない。通常経費承認そのものが非同期へ置き換わったという意味でもない。

## 4 実測比較が必要なら最小候補は1件

| 入力 | 状態 |
|---|---|
| async JAR＋async=trueの能力不足HTTP | 保存済みR2-04。500とAuthorizationDeniedException・global fallback logを保持。再実行不要 |
| normal JAR＋async=falseの同じ能力不足HTTP | **比較候補の追加1件。未承認・未実施**。既存固定normal JARを利用し、再packageは初回比較へ含めない |

比較操作は同じhelper・同じ認証方式・正当CSRF・同じ業務状態／version・能力不足条件、fresh隔離DB／独立stubを使う。正常承認を再検証する操作へ置換しない。HTTP status／safe body／実例外型とhandler経路、expense状態・version／成功承認Audit、async schema不在、stub到着・受理0、cleanupを採取する。通常にはpublication tableがないため、async用のpublication SQLをそのまま実行しない。

HTTP403を期待してtestをFAILさせる再現反復ではなく、今回の比較目的に沿って500／403と根拠を採取する診断として別IDにする。500を業務正常契約や受入PASSへ変更する意味ではない。比較fixtureの新規method／観測差分と検証上限は、実行前に有限一覧としてOwner判断へ提示する。通常構成で1件だけ確認する承認を、旧R2の12件・直接回帰4件・通常rootの追加実行へ拡張しない。

| normal比較の結果 | 次の解釈・必要判断 |
|---|---|
| 同じ500・同じglobal fallback | 少なくとも今回sourceではasync有効化を必要条件としない。既存共通境界を修正reviewの主対象にする。R1前のruntime再現を完了扱いにはしない |
| 403となる | async runtimeの登録・proxy／resolver順などの差を絞って調査する。APIのSecurity依存追加を先に決めない |
| 別の拒否／起動／観測失敗 | 比較不成立として停止。CSRF拒否・認証失敗を能力不足の比較結果にしない |

実測を採用する場合でも、旧固定JAR／失敗XML／logとsource hashを保持して新しい診断epochを作る。旧run残下限921秒は実行許可ではなく、新しい予算・停止／cleanup条件が必要。過去codeをcheckoutして既存未commit差分を破棄することはしない。

## 5 修正方式の判断順序

1. 本書のsource確認と未実測の区別をOwnerと確認する。
2. 通常構成の最小比較1件が判断に必要か決め、必要なら個別の診断条件を承認する。
3. 原因境界を確認後、API・Security・Applicationそれぞれの依存／責務を踏まえて[修正方式比較](phase4-s1-r2-access-denied-correction-options-20261013.md)を更新する。
4. 方式・修正path・Consumer影響・必要回帰・新実行条件を個別判断してから修正する。

APIのgeneric handlerへ型判定を追加する案は、例外処理の責務として検討できるが、Security非依存という現行契約を変更するならFramework reviewが必要。局所再throw案も一般Framework問題を解決したとは言えない。どちらも原因切り分けの代わりにはしない。

**現時点は原因整理まで。修正方式の採用、通常比較1件、production変更・追加回帰・commit／remoteは未承認。** R2の7 PASS／1 FAIL・直接回帰4件未実施を維持する。

## 6 Owner指示による通常構成比較1件の結果

Ownerは「既存の固定通常JARで同じ能力不足要求を行う比較1件を進めいたいです」と指示した。これに基づき、比較専用の新epochを作り、正常JARの再package・production修正・Framework依存追加を行わず初回1件だけ実行した。診断の有限上限は10分（cleanup3分予約）、既存fixtureの240秒test上限・起動120秒・正常終了30秒＋限定force後5秒・PG1／client8・各Java heap768 MiBを維持した。旧R2残予算を流用していない。

run：`tmp/r2-normal-denial-comparison-20261013/`。通常JARは旧epochから同bytesのcopyを新runへ固定し、hashは`547559E92C0D6B500AD1AB9364722E29B345BC60B5BF92845813825CF1F81A09`で不変更。基点commitは`cfe9d9e`、既存Tooling差分に比較用methodと観測helperだけを追加した。差分対象は既存の`PackagedReferenceAsyncFailureIsolationTest.java`と`AsyncReferenceFixture.java`の2 file、production不変更。比較実行source4 fileのhashは`comparison-inputs.json`に保全し、旧8件の実行manifestを上書きしない。

実行したcaseは`normalArtifactCapabilityDenialOriginComparison`。normal artifact／async=false／V4=false、fresh隔離DB、実Session login、EXPENSE:APPROVEを持たない別申請者、正当CSRF、SUBMITTED／version2の対象に承認POSTを送った。比較の診断はHTTP500または403を区別して採取するもので、R2-04の期待403を変更したものではない。

2026-10-13 12:55:38 JST終了、Maven22.430秒、BUILD SUCCESS。fresh XMLは1件・failure／error／skip0、追加再実行0、他test実行0。これは**診断採取の成立**であり、500応答の業務契約受入やR2完了ではない。corrupted-channel warningを保持し、case名とXML実体を別照合した。

| 観測項目 | 通常構成の比較結果 |
|---|---|
| HTTP | **500**、safe ProblemDetailにKOIKI-INTERNAL-001。例外型・実装package名はbody非露出 |
| 実例外・変換先 | `AuthorizationDeniedException: Access Denied`、`KoikiProblemDetailsExceptionHandler`のUnexpected request processing failure |
| 例外発生thread | 同期Servlet request thread。expense承認MVC routeからの拒否 |
| expense | SUBMITTED／version2のまま |
| 成功承認Audit | 0 |
| async schema | 不在。通常構成の不存在tableにpublication SQLを実行していない |
| stub | 到着0／受理0を5秒有限pollと子process終了後に照合 |
| cleanup | 子process／DB終了、終了後docker psは0。既存Java30304／25248のcreation identityを保持 |

子logは`r1-child-741853837699306417.log`。12:55:32.967 JSTにglobal fallbackが拒否例外を記録し、ExpenseWebController.approve経路を確認した。旧async=trueのlog・FAIL XMLと区別して保存する。

`normal-denial-comparison-fixed.xml` SHA-256は`200969286EB554298BC1B56F430E95A648E8061B33E099010CA72C912EAFBC31`。`normal-denial-observation.json`、`cleanup-observation.json`、`comparison-fixed-result.json`でsource／JAR不変更・case名・件数・到達点・cleanupを照合した。tmpのraw／固定JARはGitでは移送されない。

### 6.1 今回確定できたことと限界

同じ固定source由来のnormal／async両構成で、同じ能力不足要求が同じglobal fallbackによって500となる。したがって、**今回の問題の発生に非同期有効化は必要ない**。今回の修正検討では、同期method Securityと既存global例外処理の境界を主対象にする。

R1前のartifactを実行した比較ではないため、過去の全versionでも500だったとは言わない。非同期導入の全影響不存在、他Controller・他Consumerの同種問題解決、拒否Audit自動保存も実証していない。

修正方式・APIへのSecurity依存追加・Reference局所修正・追加回帰は引き続き未採用／未開始。R2は7 PASS／1 FAIL・直接回帰4件未実施を維持し、比較1件をR2新規8件へ混ぜない。次はこの切り分け結果を入力にOwnershipと依存境界をreviewする。
