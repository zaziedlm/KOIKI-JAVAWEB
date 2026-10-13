# R2局所再throw：用途限定修正・追加検証の開始判断案（2026-10-13）

状態：第一案の選択はOWNER CONFIRMED。修正path・追加実行・新予算の限定開始はOWNER REVIEW INPUT。

**現行実行状態（2026-10-13）：** 限定開始は下記Owner承認で採用済み。訂正3 file作成、isolated source固定、早期3件はPASS。通常rootは既存managed設定fixtureの1 failure／1 errorで停止し、後段package7件は未実施。[停止・読取診断](../architecture/validation/phase4-s1-r2-local-rethrow-correction-and-root-stop-20261013.md)へ追跡する。追加再実行・環境入力訂正は別判断。

**限定開始承認（2026-10-13）：** Ownerは「この限定範囲での作成・検証開始を承認します」と明示した。§2〜5の3 file以内の訂正差分、早期3件／通常root1回／package7件、未commit採用差分のisolated snapshot・hash固定、新規45分・phase別資源・今回所有資源／停止／cleanupを条件付き採用する。正本と承認記録・source hashを固定し、環境preflight成立後に限定作成検証する。旧source／artifact／FAIL／比較を保持し、追加再実行・対象外修正・commit／remoteは別判断。

## 1 第一案の選択と適用範囲

Ownerは[通常構成比較§6](phase4-s1-r2-denial-origin-assessment-20261013.md#6-owner指示による通常構成比較1件の結果)を入力に「局所再throwを用途限定の修正候補 第一案として、進めます」と指示した。本票で第一案を具体化する。第一案の選択を、未提示のproduction修正範囲・追加実行回数・予算の一括承認へ読み替えない。

対象はReference-owned expense MVC。Spring SecurityのAccessDeniedExceptionを元のまま再throwして既存Security chainへ返す。承認を同期のまま維持し、APIへのSecurity依存追加・Framework一般修正・拒否Audit新設を行わない。master／identity／RESTへの適用拡大も含めない。

Framework global handlerがSecurity例外を捕捉する一般gapは残課題として保持する。今回sourceの通常／asyncで500が再現した結果を保全し、局所修正をFramework全体の解決へ昇格させない。

## 2 許容差分の案

今回の訂正差分は**3 file以内**。既存R2 Tooling4 fileと合わせて、全未commit code差分は最大6 fileとなる。元のTooling-only4 file制限からの明示的な追加判断であり、同じ承認内と扱わない。

| path | 訂正差分 |
|---|---|
| `koiki-reference-app/src/main/java/org/koikifw/reference/expense/adapter/inbound/web/ExpenseWebExceptionHandler.java` | AccessDeniedExceptionの専用handlerで同一instanceを再throw。既存の業務／ResponseStatus処理を維持 |
| `koiki-reference-app/src/test/java/org/koikifw/reference/web/ReferenceBusinessUrlSecurityTest.java` | API global adviceを含むMVC統合test3件。既存assertionを弱めない |
| `build-support/reference-e2e-verification/src/test/java/org/koikifw/buildsupport/referencee2e/AsyncReferenceFixture.java` | 既存通常比較の観測helperに明示expected-status入力を追加し、訂正後は500を診断成功として通さず403を必須化する案 |

局所handlerは例外型・cause・主体を変えず、403bodyやAuditを書かない。独自resolver・新SecurityFilterChain・依存／POM／migration／Framework API／Rules／CI変更を行わない。

既存Reference adviceとglobal adviceはともに既定／LOWEST_PRECEDENCE相当で、bean登録順を一般保証にしない。初回は現行順を維持して再throwだけを確認する。選択順・再throw伝播が成立しなければ停止し、優先順位annotation等の追加を別訂正判断へ戻す。勝手に高優先度化してtestを通さない。

## 3 必要集合と順序

| 順 | 集合・回数案 | 必須確認 |
|---|---|---|
| 1 | 新規MVC統合3件、早期1回 | 認証済み能力拒否の通常403、HTMX403＋既存safe partial、非Security RuntimeExceptionの従来safe500。APIのgeneric adviceを有効にし、欠落により成功するtestにしない |
| 2 | 最終通常root1回 | 上記3件を含め、既存method security／CSRF／Header／業務／Auditの統合。専用92skipの分離を維持。早期3件の重複を独立成果へ二重加算しない |
| 3 | 通常／async artifact各必要package・内容照合1回 | root成果を通常入力に使用。asyncだけtest全省略・forceCreationで再packageし、181 main／resource等の実内容一致とruntime依存分離を新epochで照合 |
| 4 | 通常構成の能力不足確認、追加1回 | 既存比較caseを新通常JAR／async=falseで実行し、expected-status=403を必須化。状態・成功Audit／到着／受理0、schema不在、終了後照合 |
| 5 | 失敗R2-04、追加1回 | 新async JAR／async=trueで期待403を維持し、前回未到達の状態／成功承認Audit／publication／到着／受理0まで確認 |
| 6 | 未実施直接回帰D01／D02／C02／C04、各初回1回 | stub key／payload契約、async無効保護、正常2件の対応関係 |
| 7 | 既存Critical Journey C01、今回訂正epochの初回1回 | 新通常JARのbrowser／API／Security／LOGIN Audit／DB／log契約 |

Maven集合は直列で、前集合の成功・XML・cleanup・所有資源終了を照合してから次へ進む。通常能力不足確認が500のままならR2-04や直接回帰へ進めない。

新規root3 invocation候補、package側は通常能力不足1＋R2-04 1＋直接回帰4＋C01 1＝**7 invocation**。通常比較とR2-04だけが追加実行の個別判断対象。既存成立R2の7件・R1・専用92件の一律再検証はしない。新sourceでの7件全体の結果とはせず、旧source／artifact結果と訂正epochの結果を対応表で集約する。

MVC sliceのservice mockが投げる拒否はresolver／filter連携の検査であり、実method security／DB保存不変を代替しない。後段の2つのpackage能力不足caseでその境界を実証する。新規拒否Auditをtest成功条件へ暗黙追加せず、C01の既存LOGIN Auditを保護する。

## 4 source固定と証拠保全の案

新たなcommitを自動実行しない。原固定点`cfe9d9e`と、既存Tooling4 fileの現行hash、今回追加2 Reference file・Tooling観測差分、承認文書のhashを別manifestで固定する案とする。実行sourceの未commit差分をcommit IDだけで表現しない。旧停止／比較のsource manifest・XML・JAR・logを保全し、isolated新runへsnapshotしてbuild／testする。

branchは既存R2 branchを維持する。source確定後に比較snapshotのhashを照合し、対象外のcode差分があれば停止する。過去snapshot／target／固定JARを上書きせず、新通常／async JARを別保存する。clean source要件の扱いは、未commit採用差分を含む上記固定方式を今回の限定開始判断で明示採用する必要がある。文書commitが必要とOwner判断した場合は、その操作を別承認する。

## 5 新しい有限実行条件の提案

旧R2残下限921秒・通常比較の上限残は再利用せず、訂正実行に**新規45分（cleanup5分予約）**を提案する。文書作成とOwner待ちは除外。source／環境確認5分、作成後compile・早期3件5分、通常root10分、async artifact作成／照合7分、package7件10分、Evidence3分、cleanup5分。各集合上記1回、これ以外の追加再実行0回。

全phaseの今回所有JVM4以内・PG1、Maven本体／fork／compiler／Reference各heap768 MiB以内を開始前に設定する。早期MVCはDB不要。通常root／C01は既存R1で承認された有限資源区分の候補としてRyuk2／DB client14、R2 package／provider集合はRyuk1／client8。区分は本票の開始判断で採用し、未承認の上限拡大として使用しない。

通常rootのDB制限は`koiki.reference.normal-root.db-limits.enabled=true`を使用し、専用skip flagを資源制限の代用にしない。R2 pool4／min idle0・executor1／queue16、PG memory1 GiB／CPU1／max_connections16を維持する。起動120秒・失敗／completion30秒・子process終了30秒＋限定force確認5秒、既存のより短い制限を延長しない。

空きmemory8 GiB／disk10 GiB、raw100 MiB／run2 GiB／子log10 MiB以内。JDK21／Wrapper・Docker Server／image・保護対象Java／containerを新しく確認する。C01のPlaywright1.62.0とbrowser実体／revisionを読取確認し、不足downloadは有限一覧提示後に別判断する。無関係なbrowserを起動・停止しない。

今回は所有を確認した隔離DB／stub／issuer／package子JVM／C01 browserと有限cleanupだけを操作対象として開始判断へ含める。protected baselineを記録し、集合終了時に今回資源が終了することを確認する。有限sampleを連続上限保証にしない。

## 6 停止・判断欄

advice選択不成立、再throw後の500・別拒否、CSRF／default deny／Header／既存Auditの弱化、対象外修正・依存変更、source不一致、資源／時間超過、観測／cleanup不成立では停止しOwnerへ差分を示す。期待値・Security権限を変更して成功へ寄せない。失敗集合をroot反復や全件再実行で隠さない。

**Owner判断済み：** 用途限定の局所再throwを第一案として具体化する方針。

**今回判断が必要：** §2〜5のproduction1 file＋root test1 file＋Tooling観測差分、早期3件／通常root1回／package7件、未commit採用差分のsource固定方式、新規45分・phase別資源・所有／停止／cleanupの条件付き開始。方式確認・preflight成立後に限定作成検証し、条件不成立なら停止する。

修正開始・追加検証は未実施。Framework共通修正・拒否Audit新設・結果受入・commit／remote・R3〜R5は別判断を維持する。
