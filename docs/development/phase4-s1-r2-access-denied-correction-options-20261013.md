# R2承認能力不足：例外処理の修正範囲と必要回帰の判断案（2026-10-13）

状態：DRAFT / OWNER REVIEW INPUT。対応方法の比較・整理だけを行い、production修正・追加compile／package／testは未開始。

**後続の熟考方針（2026-10-13）：** Ownerとの対話で、APIのSecurity非依存を維持する意味と、問題が非同期導入由来か従来境界由来かを先に整理する方針を確認した。[原因切り分け資料](phase4-s1-r2-denial-origin-assessment-20261013.md)を現在の判断入口とする。以下のA案第一候補は当初の変更範囲最小化の提案であり、採用・優先順位を固定しない。API依存追加・Reference局所修正のどちらも未採用。

**比較後の入力（2026-10-13）：** [通常構成比較§6](phase4-s1-r2-denial-origin-assessment-20261013.md#6-owner指示による通常構成比較1件の結果)でnormal／async=falseでも同じSecurity拒否→global fallback→500を確認した。非同期有効化を必要条件としない例外処理境界の問題として方式reviewを継続する。修正・追加回帰は未開始。

**後続の第一案選択（2026-10-13）：** Ownerは「局所再throwを用途限定の修正候補 第一案として、進めます」と指示した。Aをexpense MVC限定の第一案として具体化し、[限定訂正開始判断案](phase4-s1-r2-local-rethrow-limited-correction-start-review-20261013.md)へ対象・集合・source固定・新予算／停止条件を整理した。Framework一般gapは未解決として保持する。第一案の選択と、production差分・追加検証の限定開始判断を区別する。

## 1 確認した問題

[停止結果§3〜6](../architecture/validation/phase4-s1-r2-implementation-and-selected-verification-20261013.md#4-失敗原因の読取診断と停止)のR2-04は、能力不足の認証済み主体からの承認要求で403期待に対して500となった。method securityのAuthorizationDeniedExceptionをFramework APIのglobal汎用adviceが500へ変換している。能力不足から承認を成立させた証拠ではないが、失敗HTTP assertion後の副作用なし確認は未実施であり、R2-04をPASSにしない。

目的は、**Securityの拒否を既存Security応答へ戻し、通常の業務・予期しない例外の扱いを維持すること**。非同期通知の仕組みを変更する作業ではない。

今回のsourceは`feature/phase4-s1-reference-failure-isolation-r2` / `cfe9d9e`＋未commit Tooling4 fileとEvidence2 file。7 PASS／1 FAIL・未実施直接回帰4件を保持し、既存停止記録を訂正成功で上書きしない。

## 2 選択肢

| 案 | 対応とOwnership | 利点・確認が必要なこと |
|---|---|---|
| **A：Reference expense MVCでSecurity例外を再throw（第一候補）** | ExpenseWebExceptionHandlerにAccessDeniedException専用handlerを追加し、同じ例外をそのままthrow。Reference所有、production1 file候補 | 既存Security chainへ返す。Framework依存／公開契約を変更せず、HTML／HTMXの既存拒否処理を利用できる。adviceの選択順と再throw後のfilter到達を実構成で確認する必要がある |
| B：Reference expense MVCで直接403 HTMLを返す | 同handlerでModelAndViewを返す | 表示は局所制御できるが、Security chainの既存拒否処理を迂回し、HTMX応答・将来のAudit接続を別に整合する必要がある。今回の第一候補にしない |
| C：Framework全体のSecurity／API exception境界を修正 | API adviceの適用範囲見直し、またはSecurity-owned bridge等。Framework所有の別review | 他のMVC／RESTにも共通対応できる一方、Consumerの既存Problem Details契約・非Security構成・依存選択へ影響する。R2の局所修正として自動採用しない |

AはReferenceの今回のexpense MVCに限る。Framework根本原因の一般修正を完了したとは報告しない。masterのgeneric handlerや他Controllerで同種問題がないことも、今回の8ケースだけでは主張しない。Frameworkの一般gapは別課題として残す。

## 3 A案の処理と未実証点

候補path：`koiki-reference-app/src/main/java/org/koikifw/reference/expense/adapter/inbound/web/ExpenseWebExceptionHandler.java`。

```java
@ExceptionHandler(AccessDeniedException.class)
void propagateSecurityDenial(AccessDeniedException exception) {
    throw exception;
}
```

これは方式説明用であり、production fileへは書いていない。AuthorizationDeniedExceptionだけに限定せず、Spring SecurityのAccessDeniedException契約で扱う。例外の型・cause・認証主体を変更せず、HTTP応答body・Auditをこのhandlerで新設しない。

Spring MVCはExceptionHandlerが元の例外を再throwし、残るresolverへ処理を委ねる方式を提供する。[公式MVC資料](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-exceptionhandler.html)と[公式ExceptionTranslationFilter契約](https://docs.spring.io/spring-security/reference/api/java/org/springframework/security/web/access/ExceptionTranslationFilter.html)を判断入力とした。資料の一般契約から、今回のadvice／resolver／filter構成での403成立を実証済みとはしない。

実構成で確認する点は、局所handlerがglobal Exception fallbackより先に選ばれること、再throw後に同じglobal handlerで再捕捉されないこと、認証済み拒否が403となること、HTMXの既存access denied handlerが動くこと、非SecurityのRuntimeExceptionは従来のsafe500となること。これらが成立しなければ停止し、C案を含む境界reviewへ戻す。最初から独自resolver・優先順位調整・Security設定変更を追加しない。

## 4 Security Auditの扱い

今回読取では、Framework Security auto-configurationはmethod securityとdefault denyを設定しているが、method拒否eventをSecurityAuditRecorderへ自動接続する実装は確認できなかった。Referenceのbrowser／API access denied handlerにも拒否Auditの保存呼出は見つからなかった。Phase 2のAudit fixtureにはAUTHORIZATION_DENIEDの手動記録があり、その成功を全method拒否の自動Auditへ読み替えない。

既存Critical Journeyが照合するSecurity AuditはIDENTITY_LOGIN／LOGINである。この実装の保護は維持する。一方、R2-04へ新たな拒否Audit保存を義務化する場合は、event／action・主体・transaction・失敗時挙動・二重記録防止の契約を別に判断する必要がある。A案で暗黙に追加しない。

前段資料の「Security Auditを保護する」は、既存成立経路を弱めない意味で扱う。存在を確認できない拒否Auditについて、新たなPASS条件や既存保証を作らない。

## 5 A案の最小変更・検証候補

production追加は上記1 file、root test追加は既存`koiki-reference-app/src/test/java/org/koikifw/reference/web/ReferenceBusinessUrlSecurityTest.java`の3 invocation候補。既存のMVC sliceへAPI global adviceのauto-configurationも明示的に含め、mock serviceから拒否／予期しない例外を発生させて**resolverとSecurity filterの統合**を検査する。mockだけでは実method security・DB結果を証明できないため、R2-04のpackage testも残す。

| 集合 | 必要性・回数案 |
|---|---|
| 新規root3件 | 認証済みmethod拒否の通常HTML403、HTMX403と既存body、予期しないRuntimeExceptionのsafe500。早期選択1回 |
| 通常root | production例外処理変更の最終統合1回。上記3件との重複を独立成果へ加算しない。CSRF／Header／default deny／method security／既存業務回帰、専用92skipの分離を維持 |
| R2-04 | 新artifactで追加1回。403と、その後の状態／成功承認Audit／publication／到着／受理0を最後まで確認 |
| 未実施直接回帰4件 | D01／D02／C02／C04を各初回1回。shared Toolingの受理契約・通常保護・正常対応関係を閉じる |
| 既存Critical Journey C01 | 新しい通常JARで初回1回。通常browser／API／Security／LOGIN Audit／DB／logの既存assertionを保護 |

新規機能caseはroot3候補、R2-04は失敗caseの追加実行、直接回帰4＋C01は既存case。R2成立7件の一律再実行・専用92件の全件再実行・R1再検証・R3以降は含めない。新規root件数は候補で、実XML／case名で集計し、未実装の件数をPASSへ加算しない。

ToolingのR2-04期待403は変更しない。再送・復旧・stubの成功偽装・capability追加による拒否回避を行わない。必要なpackage／artifact照合は新epochとして通常／asyncを別固定し、旧7件のartifactを上書きしない。通常root JARを通常入力に利用し、async packageはtest全省略・forceCreationで作成する。

## 6 開始判断前に詰める事項

本票は方式判断案であり、追加実行開始票ではない。OwnerとA／B／Cの範囲を確認してから、採用path・test setupの具体値・正本差分・source固定方式・新しい有限予算・集合順・各初回／追加回数・停止／cleanupを訂正開始票へ落とす。

旧停止台帳の残下限921秒を自動許可へ流用しない。通常rootの既存資源条件はRyuk2／DB client14、R2 package集合はRyuk1／client8であり、同じ上限を機械的に流用しない。どちらもJVM4・PG1、各所有JVM heap768 MiBをMaven本体も含め開始前に明示する。先行packageでMaven最大heapが未実証だった点を引き継ぎ、新しいepochはその不足を残さない。

現在の未commit ToolingとEvidenceを破棄・自動commitせず保全する。新しい実行sourceは、承認済み文書commit＋旧Tooling4 file hash＋承認された修正差分を区別して固定する方式を具体化する。commit／remoteは別判断。

**現時点の推奨：A案の詳細reviewへ進む。** production1 fileの局所再throwでSpring標準へ戻せるかを最小集合で確認する案。一般Framework修正・拒否Audit新設は別reviewを維持する。方式と必要回帰のOwner判断は未取得、作成・検証は未開始。
