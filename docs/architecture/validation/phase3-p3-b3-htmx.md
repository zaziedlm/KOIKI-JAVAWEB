# Phase 3 P3-B3 HTMX

## 1. Status and boundary

| Item | Result |
|---|---|
| Date | 2026年9月14日 |
| Architecture Owner | Shuichi Kataoka |
| Phase / CP | Phase 3 Reference Vertical Slice / P3-B3 |
| Status | COMPLETE / OWNER APPROVED |
| Start HEAD | `a1229f0`（Reference local demo Tooling commit） |
| Ownership | Framework Web MVC resource contract、Reference master HTMX、非配布Browser Tooling |
| Target | `koiki-starter-web-mvc`、`koiki-reference-app`、`build-support/reference-browser-verification` |

P3-B3はThymeleaf full-page HTMLを主軸に維持し、効果を説明できるmaster検索、paging、登録Validationへだけ
HTMXを適用する。P3-B4の2 Session競合、cache / TTL、Gate B、REST、workflowおよびremote操作を先行しない。

## 2. Implemented boundary

| Area | Result |
|---|---|
| Asset | Starter同梱済みHTMX 2.0.10 WebJarをlocal配信し、CDNを使用しない |
| KOIKI integration | CSRF header、loading状態、partial error swap、focus、swap後custom eventを小さな内部JavaScriptで提供 |
| Reference selection | 部門・経費科目の検索、paging、登録Validationだけへ`hx-*`を適用 |
| Full-page path | 既存GET / POST / redirectを維持し、URLを直接開いた場合はThymeleaf full pageを返す |
| Fragment path | `HX-Request: true`の場合だけ検索form＋結果のquery sectionまたは登録sectionを返す |
| Security | 通常・HTMX未認証ともPhase 2のlogin redirectを維持し、HTMX XHRの最終login URLを検知してfull-page遷移。CSRF拒否はsanitized 403 fragment |
| Error | master業務404等と予期しない500をstatus付きsanitized fragmentとして返す |
| Browser Tooling | Playwright Java 1.62.0 / ChromiumをRoot外・非配布のtest scopeへ限定 |

Domain Model、JPA Entity、migration、業務Use Case、Public Java API、Root Reactor module、workflowおよびremote設定は
変更しない。HTMX requestもP3-B2と同じApplication Use Caseを利用する。

## 3. HTMX 11-contract trace

| # | Contract | Implementation / verification | Current result |
|---:|---|---|---|
| 1 | full / fragment | 同一GETでheader有無によりfull page / query fragmentを分岐 | MockMvc / browser PASS |
| 2 | CSRF automatic injection | Thymeleaf metaからSpring Security token / header名を取得し全HTMX requestへ付与 | MockMvc / browser PASS |
| 3 | Validation partial | create sectionだけを再描画しUse Caseを呼ばない | MockMvc / browser PASS |
| 4 | redirect | successful createは204 + `HX-Redirect`、通常POSTは従来redirect | MockMvc PASS |
| 5 | history | 検索・pagingに`hx-push-url=true`を限定適用。query fragmentで検索値を再materializeし、back後のURL・input・結果を一致させる | MockMvc / browser back PASS |
| 6 | OOB policy | `hx-swap-oob`は採用せず、対象DOM外を変更しない | source inventory PASS |
| 7 | loading | indicatorと`aria-busy`をrequest開始・終了で更新 | source / MockMvc / browser PASS |
| 8 | search debounce | input changeを350 ms debounceしsubmitも許可 | MockMvc / browser PASS |
| 9 | dynamic reinitialization | swap後にfocus処理と`koiki:htmx:afterSwap` eventを発行 | JavaScript resource / browser PASS |
| 10 | partial 403 / 404 / 500 | status保持、HTML fragment、例外detail / ID非露出 | MockMvc PASS |
| 11 | JavaScript required | HTMX enhancementはJS必須。通常HTML routeは維持するがJS無効時のHTMX同等fallbackは保証しない | documented |

OOBは有用な採用箇所がないため、無理に実装していない。現時点のDOM更新は明示した一つのtargetへ限定する。

## 4. Automated verification

| Check | Result | Evidence |
|---|---|---|
| Reference MVC / Security | PASS | Root Reactor内の`ReferenceBusinessUrlSecurityTest`、18 tests |
| Form Login reuse | PASS | 通常・HTMX requestとも302 `/login`を維持し、内部JavaScriptがXHRのlogin到達をfull-page遷移へ変換 |
| HTMX resources | PASS | JS / WebJar 200、integration event marker |
| Search / paging fragment | PASS | query sectionだけを200で返しfull `<html>`と登録sectionなし。検索値・結果・focusを同時に再materialize |
| Validation / redirect / CSRF | PASS | partial Validation、204 redirect、tokenなし403 |
| Partial 404 / 500 | PASS | status、sanitized message、例外class / detail / UUID非露出 |
| Browser Tooling compile | PASS | `mvnw -f build-support/reference-browser-verification/pom.xml -DskipTests test` |
| Playwright / Chromium setup | PASS | Playwright Java 1.62.0、Chromium 151.0.7922.34（v1234） |
| Root Reactor clean verify | PASS | Docker接続可能な境界で16 projects成功、Reference 80 tests、failure / error / skip 0 |
| Package inventory | PASS | Starter JARにintegration JS / CSS / fragment、Reference実行可能JARにStarter / HTMX 2.0.10を収録 |
| Playwright headed journey | PASS | Owner端末で実行、1 test、failure / error / skip 0、6.283 s。login、検索fragment、debounce、loading、history back、Validation、CSRF注入・拒否を実ブラウザassertionで確認 |
| Application log reconciliation | PASS | 認証成功、HTMX検索A / B各200、Validation 200、CSRF拒否を実アプリlogで確認。request parameterはmasked |
| DB / Audit reconciliation | PASS | 部門1、科目1、申請3を維持。`MASTER_ADMINISTRATION` Business Audit 0、Security認証成功Auditあり |

非clean Reactor `-pl koiki-reference-app -am test`はP3-B2で記録済みのupstream Starter class output解決制約により
Context起動前に停止した。変更したBOM / Web MVC Starterをlocal installしたfocused実行でP3-B3 testを先に評価したが、
最終受入の正本は通常Root Reactor `clean verify`とpackage済みJARに対するbrowser journeyとする。最初のRoot実行は
sandboxからDocker named pipeへ接続できずPostgreSQL統合test 13件がContext初期化前に停止した。同じsourceを
Docker接続可能な境界で再実行し、全testとpackageまで成功したため、production defectまたはskipとして扱わない。

実browserの最初の履歴確認では、検索結果だけをswapする構成により、back後にURLは直前の検索条件へ戻る一方、
target外の検索inputが空へ戻る不整合を検出した。テスト期待を弱めず、部門・経費科目とも検索formと結果を一つの
query fragmentとして返し、server-side検索値をinputへ再materializeする構成へ限定修正した。登録sectionはquery
fragment外に維持して検索操作による入力消失を避けた。修正後のRoot `clean verify`とheaded Playwright journeyは成功した。

## 5. Browser and human checkpoint plan

1. [DONE] 起動中P3-B2 applicationを停止した。
2. [DONE] Root Reactor `clean verify`を成功させ、P3-B3のpackage済みJARを作成した。
3. [DONE] Local Run Guideに従い使い捨てPostgreSQLとP3-B3 JARを起動し、demo dataを一度だけ投入した。
4. [DONE] 非配布Playwright journeyでlogin、検索fragment、debounce、loading、history back、Validation、CSRF注入・拒否を確認した。
5. [DONE] `-Headed`で同じjourneyをOwner端末から実行し、自動assertionは成功した。Owner目視では検索・focus、history back / forwardを問題なしと確認した。loading表示は応答が速く目視未確認だが、headed browser assertion、MockMvcおよびsource確認はPASSしている。
6. [DONE] sanitized application log、Security Audit、DB非更新を突合し、credential、token、Cookie、emailをEvidenceへ記録していない。
7. [DONE] Owner review後にapplicationをgraceful shutdownし、使い捨てDB containerを破棄した。

P3-B3のinteractionは検索とValidationが中心であり、Business Auditを新規発生させる業務更新をbrowser journeyへ
含めない。DB突合では検索・Validation・CSRF拒否によってmaster件数・内容が変化していないことを確認する。

Ownerの追加目視中、HTMX対象外の経費申請作成でlocal demo seedの固定UUIDがWeb Formのversion / variant制約を
満たさない不整合を検出した。これはP3-B3 production contractの不具合ではなくlocal disposable Tooling fixtureの
不整合として分離し、seed内の固定UUIDと参照をversion 4 / RFC variant形式へ統一した。使い捨てDBの再作成、
migration、seed、再login、通常HTML FormによるDRAFT登録、DB参照・金額突合まで成功し、単純なDRAFT作成で
Business Auditを発生させない承認済み仕様も維持した。

## 6. Deferred and owner review points

- P3-B4: 2 BrowserContextによる楽観lock競合、cache対象 / TTL blocking decisionと実装。
- Gate B: P3-B1〜B4横断の自動browser、Owner実演、accessibility、log / Audit / DB突合。
- P3-C0以降: 最小REST API contractと実装。
- Remote Gate: workflow、required check、push / PR / merge、snapshot publish。
- OOB swap: 効果が説明できる具体箇所が生じるまで不採用を維持する。

## 7. Architecture Owner approval record

Architecture Ownerは2026年9月14日、11契約の実装・Evidence、HTMXの選択範囲、通常HTML経路の維持、
Security境界、Playwright Tooling分離、人系checkpoint結果、実browserで回収したhistory不整合および
deferred境界を一体として承認した。

Thymeleaf full-page HTMLを主軸とし、部門・経費科目管理の検索、pagingおよび新規登録だけをHTMXの採用範囲とする。
Owner目視では検索・focusおよびhistory back / forwardを問題なしと判断した。loadingは応答が速く目視未確認だが、
headed browser assertion、MockMvcおよびsource確認の複数Evidenceにより受入可能とする。

Owner追加目視で検出したlocal demo seedのUUID不整合はproduction contractから分離して修正・再検証済みである。
Browser Toolingを非配布成果物に留め、P3-B4、Gate B、RESTおよびOOB swapのdeferred境界を維持する。
以上によりP3-B3を`COMPLETE / OWNER APPROVED`とし、次CPをP3-B4とする。
