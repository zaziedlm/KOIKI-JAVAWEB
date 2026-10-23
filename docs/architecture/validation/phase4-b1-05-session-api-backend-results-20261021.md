# B1-05：Reference Session API backendの実装・局所検証（2026-10-21）

状態：**実装・局所検証完了／149 tests PASS、Owner受入は未実施**。frontend・同梱build・実browser・DoD 4-8／4-9の成立を示すものではない。

## 1. 指示・source・所有範囲

Ownerの「ここではcommitせず、B1-05の実装へ進める」指示に基づき、Windowsで実施した。branchは`docs/phase4-review-integrated-plan-20261020`、基点HEADは`e9ff13a32219a2f66bbc7ac469840b160b319c65`。開始時のB1-04文書4件の未commit差分を保持した。新たなclean source commitを開始条件に加えていない。

実装入力は[B1設計記録§5・§9](../../development/phase4-b1-session-spa-mvc-design-20261020.md)。Reference mainの新規21 file＋既存1 file、新規test3 fileの計25 file。Framework source・Application／Domain／Repository・POM／依存・migration・workflowは変更していない。commit・push・PR作成も行っていない。

| 所在 | 実装 |
| --- | --- |
| `reference/identity/configuration/ReferenceSessionApiSecurityConfiguration.java` | `/spa-api/**`専用chain、order -10、Session NEVER、標準spa()とReference所有Cookie repository、JSON 401／403、no-store、POST専用logout |
| 同directoryの`ReferenceSessionApiProblemWriter.java`／`ReferenceSessionLogoutBoundary.java` | filter段階の安全なProblem Details、CSRF後・logout直前の例外境界。両Cookieのlocal cleanupと503、commit済み応答の失敗伝播。Servlet Filter Beanにはしない |
| 既存`ReferenceSecurityConfiguration.java` | `/spa`・`/spa/**`とassetの認証境界、login成功時のSPA CSRF Cookie消去、MVC logoutへのCookie消去と例外境界の接続。既存成功先・HTMX拒否を維持 |
| `reference/identity/adapter/inbound/web/ReferenceSessionCsrfController.java` | 認証後GETで204。CSRF tokenをJSONに返さない |
| `reference/expense/adapter/inbound/sessionapi/` | options／自分の一覧・詳細／draft作成／submit、専用10 DTO・mapper・業務Advice、厳密String／canonical UUIDの入力処理。金額・version・totalElementsは十進文字列 |

上記`reference/`は`koiki-reference-app/src/main/java/org/koikifw/reference/`を表す。既存`ExpenseReadService`・`ExpenseApplicationService`を呼び、認可・transaction・master確認・Auditを複製していない。Bearer DTO・MVC Form、Framework Identityに項目を追加していない。

SPA用CookieはJS可読・Path `/`・Domain未指定、Secure／SameSiteをSession設定から適用する。CSRF CookieとSessionへの暗号的結合、古いCookie＋一致headerの任意再注入までの失効は保証しない。logoutの永続store失敗ではlocal stateを除去して503とし、持ち出されたSessionまで失効済みとはしない。

## 2. 実行結果

保存済みJDK21・Wrapper Maven3.9.16、offlineでcompile・選択testを実行。既存Error Prone／NullAwayを維持した。通常root全件・package・browserは実行していない。

| class | tests | 結果 |
| --- | ---: | --- |
| 新規`ReferenceSessionApiSecurityTest` | 22 | PASS：実Security chain、Session／Bearer併存、CSRF Cookie／header、login更新、両logout、失効例外・commit済み失敗、認証境界 |
| 新規`SessionExpenseApiControllerTest` | 40 | PASS：DTO・strict JSON・数値境界・UUID・paging・Location・情報非露出・業務error |
| 新規`SessionExpenseApiPostgreSqlIntegrationTest` | 10 | PASS：実JDBC Session・MVC相互参照、create／submit、scope・競合・無効master、拒否時の状態／Audit／承認event不変、永続logout・期限切れ |
| `ReferenceSessionCookieConfigurationTest` | 8 | PASS |
| `ReferenceBusinessUrlSecurityTest` | 23 | PASS：MVC／HTMXの直接回帰 |
| `ApiExceptionBoundarySecurityTest` | 13 | PASS：R-01の例外境界 |
| `ExpenseApiControllerTest` | 6 | PASS |
| `ExpenseMutationAuthorizationOrderTest` | 6 | PASS |
| `ExpenseApplicationServiceMethodSecurityTest` | 3 | PASS |
| `ReferenceArchitectureTest` | 2 | PASS |
| `RequiredNegativeArchitectureRulesTest` | 5 | PASS |
| `ExpenseApiPostgreSqlIntegrationTest` | 6 | PASS：既存Bearer／Cookie-only拒否・競合・DB／Audit |
| `ExpenseReadModelPostgreSqlIntegrationTest` | 5 | PASS：既存scope・射影・read契約 |
| **合計** | **149** | **新規72＋既存77、failure／error／skip 0** |

DBなしの初回合流は新規認証17件を含む123件をclean compileから確認し、その後同じmain sourceで認証集合を22件へ補強して確認した。最終XMLは13 class／149 invocationで照合し、再実行やparameterized method内の比較を重複加算していない。

DB3 classは1 classずつ直列実行。`-DforkCount=1 -DreuseForks=false -DargLine=-Xmx768m -Djunit.jupiter.execution.parallel.enabled=false -Dkoiki.reference.verification.resource-limits.enabled=true -Dspring.datasource.hikari.maximum-pool-size=4 -Dspring.datasource.hikari.minimum-idle=1`を指定し、Maven heapも768 MiB。DB同時1、PostgreSQL17・memory1 GiB／CPU1／max_connections16とpool4／idle1をfixtureのassert・logで確認した。各class後に最大30秒の読取確認を挟み、当該container終了後に次へ進んだ。既存の別用途PostgreSQL15 container 2件は操作していない。

## 3. 実行入口・途中経過

Reference reactorの入口は次のとおり。`failIfNoSpecifiedTests=false`は他moduleに指定testがないために使用し、指定classのXML・実行数を別に確認した。

```powershell
# DBなしの新規・直接回帰10 class（上表のDB3 classを除く）
.\mvnw.cmd --batch-mode --no-transfer-progress -o -pl koiki-reference-app -am `
  '-Dtest=ReferenceSessionApiSecurityTest,SessionExpenseApiControllerTest,ReferenceSessionCookieConfigurationTest,ReferenceBusinessUrlSecurityTest,ApiExceptionBoundarySecurityTest,ExpenseApiControllerTest,ExpenseMutationAuthorizationOrderTest,ExpenseApplicationServiceMethodSecurityTest,ReferenceArchitectureTest,RequiredNegativeArchitectureRulesTest' `
  '-Dsurefire.failIfNoSpecifiedTests=false' clean test

# DBは上記資源引数を加え、上表の3 classを1 classずつ指定する
# 後続の局所確認ではfully qualified class名を指定して旧packageの残存出力と区別した。
```

- 初回compileで、Application例外constructorのpackage境界・ProblemDetailのstatus型を検出。Application契約を公開せず、Session専用のread-not-found markerとHttpStatusCodeを使う形へ訂正した。JDBC Session fixtureも内部型に依存せず、公開Session／SessionRepository契約を使うgeneric helperへ訂正した。
- DTO40件は初回PASS。認証／CSRFは初回17件中2 failure。SameSiteはServlet Cookie拡張属性に設定されている一方、MockHttpServletResponseのSet-Cookie文字列へ出ないことを確認し、Cookieの実属性をassertした。live Servlet／browserでのSet-Cookie照合はB1-07に残る。login fixtureには外部照会しないtest用CompromisedPasswordCheckerを置いた。productionの判定は変更していない。
- 既存MVC testの旧package compiled classにより23 error。ソースを変えず、失敗を保全してclean compile後に正しいpackageの23件PASSを確認した。
- 実DB初回10件中1 failure。期限切れfixtureが`expiry_time`だけを変更していたため、Session.isExpiredの再構成に使う`last_access_time`も期限切れへ訂正。最終10件PASS。productionの期限判定は変更していない。
- 認証集合の補強ではBearerとSession両chainを有効化。Bearer側のCookie-only確認に`user()`を使うとrequest contextへ直接認証を入れてしまうため、実際のHttpSession内SecurityContextに訂正した。最終22件PASS。別の集合を際限なく追加したものではなく、§7.3で計画したcredential分離・Cookie属性・commit済み失敗の境界を具体化した。

## 4. 証拠・残る作業

端末内証拠は`target/b1-05-20261021/`。`b104-before.patch`は開始時文書差分、`summary.json`は基点HEAD・25 code／test fileのSHA-256・13 XMLの件数／hash、各logはcompile・途中失敗・clean合流・最終認証・DB3 classを識別する。既存変更のpatchだけで新規fileを識別できたとはせず、新規fileもhash対象に含めた。このdirectoryはGit対象外であり、Cloudや別端末へ自動移送されない。

今回packageしておらず、clean testにより旧target JARは削除済み。R-02のCloud JARや過去browser PASSをB1-05のartifact証拠へ流用しない。新規login testはtest UserDetailsService、DB testの認証はtest所有の永続Sessionであり、実IdPや実browser loginの再実証ではない。

次は**B1-06：最小React UI・Reference限定build／同梱・SPA入口とhome link**。B1-07で通常root→B1 profile build→同じJARの新旧browser journeyを行い、B1-08でDoDの成立範囲とOwner受入を整理する。B1-05だけでSession SPA全体・DoD・Phase4全体を完了にしない。

## 5. Codex Cloudでの再検証（2026-10-21）

Ownerの指示で同branchを`--ff-only`でpullし、B1-05成果commit `89a534534dee94b26cdfad66ee10330b21c039a8`をこの環境で再検証した。開始時working treeはclean。Windowsの未追跡証拠を流用せず、新しいlog・XMLを生成した。

保存済みactivationのJDK `21.0.12.1`・Wrapper Maven `3.9.16`、Docker `28.4.0`・保存済みPostgreSQL17 imageを使用。全実行offline、新規取得・環境再構築なし。§3と同じDBなし10 classをLinuxの`./mvnw`で`clean test`し、13 projects SUCCESS・128件PASS。171 main sourceとtestを既存Error Prone／NullAway設定でcompileした。

続いて§2と同じDB3 classを1 classずつ直列実行し、新規Session API10件・既存Bearer API6件・read model5件がPASS。§2の資源引数、Maven／test JVM768 MiB、DB同時1・memory1 GiB／CPU1／max_connections16・pool4／idle1を適用し、実効値をfixtureのassert・logで照合した。各class後に最大30秒の読取確認を挟み、稼働containerなしを確認して次へ進んだ。Mockito attach・Docker／loopbackに必要な権限付き実行を使用した。

最終XMLは§2と同じ**13 class／149 tests、新規72＋既存77、failure／error／skip 0**。class名・件数・生成時刻を照合し、古いXMLや再実行を重複加算していない。検証終了時点でtracked 1,293 fileのSHA-256が実行前と一致し、working treeもcleanだった。終了後に文書の本節だけを追記した。production／test／POM等は変更していない。

Cloud内証拠：`/workspace/cloud-setup/logs/phase4-b1-05-cloud-revalidation-20261021/`。`source-before.json`にHEADと全tracked file hash、`clean-non-db.log`とDB各classのlog、`reports/`に最終13 XML、`selected-test-results.json`に件数・XML hash・source不変の照合を保存した。

通常root全件・package・実browserは今回も未実施。clean testで旧JARは消去されており、B1同梱JARやlive Set-Cookieの検証を成立済みとはしない。B1-06／07／08とOwner受入の境界は§4のまま維持する。今回commit・push・PR作成は行っていない。
