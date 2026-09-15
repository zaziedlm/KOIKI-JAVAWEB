# Phase 3 Gate B — Web / query acceptance inventory

**Status:** INVENTORY / DISPOSITION OWNER APPROVED — GATE B INCOMPLETE / BLOCKING ITEMS OPEN  
**Inventory date:** 2026年9月15日  
**Branch:** `feature/phase3-reference-vertical-slice`  
**Inventory HEAD:** `272a2ab7c792a6b6fcf84c867b3697843fe907b5`  
**Architecture Owner:** Shuichi Kataoka  
**Production change:** 0  
**Next action:** GB-BLK-01 browser runner安定性のfocused原因分析

## 1. Review objective and boundary

本記録は、P3-B1〜P3-B4を個別成果の寄せ集めではなく、Reference ApplicationのWeb / queryを構成する
一つのMilestoneとして横断評価するGate B棚卸し案である。次の3区分でEvidenceを分類する。

1. **直接検証済み** — 対応する自動test、実browser、DB観測またはartifact inventoryがある。
2. **横断Evidenceで支持** — 複数CPの実装境界と検証結果を組み合わせると主張が成立する。
3. **未確認 / blocking / deferred** — Gate Bの達成主張へ含めず、再確認条件または後続CPを明示する。

本inventoryではproduction code、Public API、Maven module、migration、dependency、workflowまたはremote設定を
変更しない。Gate Bが`COMPLETE / ACCEPTED`となる前にP3-C0 REST contract reviewへ進まない。

P3-B1は`COMPLETE`、P3-B2〜P3-B4は`COMPLETE / OWNER APPROVED`であり、各CPの承認を取り消したり
過去Evidenceを無効化したりしない。一方、別PCでGate B開始前に得た新しいbrowser再現結果は、Gate Bの
自動browser安定性を現在形で判断する追加Evidenceとして扱う。

## 2. Milestone B coherence

### 2.1 One Web / query flow across P3-B1〜B4

Milestone Bの責務は次の一本の流れとして接続している。

```text
FrameworkPrincipal / Permission / Session / CSRF
  -> Reference MVC Controller / Form
    -> shared Application Use Case or application-owned Query
      -> JPA projection or scoped JdbcClient materialization
        -> Thymeleaf full page
          -> selected HTMX fragment interaction
            -> optimistic conflict / display-only TTL cache
              -> HTTP / browser / log / Audit / DB correlation
```

| CP | 固定した責務 | 後続CPとの接続 | 横断評価 |
|---|---|---|---|
| P3-B1 | master JPA射影、expense JdbcClient read model、actor別scope | B2が同じApplication DTO / query recordを画面へmaterialize | Entity露出や取得後filterを追加していない |
| P3-B2 | 正式Web MVC Starter、full-page Thymeleaf、Form / View境界、Security route | B3が同じController / Use Caseへfragment表現だけを追加 | 通常HTMLを主軸として維持 |
| P3-B3 | master検索・paging・登録ValidationだけのHTMX、CSRF、history、loading、focus、partial error | B4の競合操作は通常HTMLのまま分離 | 選択適用と11契約を業務処理の分岐へしていない |
| P3-B4 | Reference専用409、先行transactionだけの確定、option一覧だけの30秒cache | Gate Bでbrowser / Audit / DBと一体評価 | cacheを認可・更新・current-value判断へ流用していない |

P3-B1〜B4を通じて、Reference向けの画面都合がFramework Public APIへ昇格した形跡はない。正式Framework成果物は
P3-B0で承認した`koiki-starter-web-mvc`のdependency aggregation、resourceおよび内部supportに限定され、
master / expenseのController、Form、View DTO、Template、cacheおよび競合画面はReference Ownershipを維持する。
browser runnerとdemo seedはRoot Reactor外の非配布Toolingである。

### 2.2 Query and command separation

| Need | Implemented boundary | Gate B assessment |
|---|---|---|
| master一覧・検索・paging | JPA constructor expressionから`application.dto`を直接返す | DIRECTLY VERIFIED |
| applicant一覧 / 詳細 | SQL内で`applicant_user_id = actor`を強制 | DIRECTLY VERIFIED |
| approver一覧 / 詳細 | actorと部門の完全一致scope、`DRAFT`除外をSQL内で強制 | DIRECTLY VERIFIED |
| accounting一覧 / 詳細 | `APPROVED` / `SETTLED`だけをSQL内で強制 | DIRECTLY VERIFIED |
| 表示値JOIN | expense adapterがdepartment表示値とapplicant emailを最終recordへ直接materialize | DIRECTLY VERIFIED |
| current-value master確認 | ADR-049の狭い同期read-only contractをexpense Port / Adapter経由で利用 | DIRECTLY VERIFIED / SEPARATE EXCEPTION |
| Form用経費科目option | 表示専用JdbcClient queryだけを30秒cache | DIRECTLY VERIFIED |
| command認可・不変条件 | Application / Domainと非cache current-value確認 | DIRECTLY VERIFIED |

表示専用read-only JOINとcurrent-value master確認は、目的と許可条件が異なる二つの例外として分離されている。
表示queryまたはcacheを更新、認可判断、業務不変条件、Domain復元へ流用したEvidenceはない。

### 2.3 MVC, HTMX and security separation

| Concern | Current fact | Assessment |
|---|---|---|
| Controller | request、validation、full / fragment、status / header / view整形に限定 | READY |
| Form / View | inbound FormとApplication DTO / query recordを使用 | READY |
| Domain / Entity | Controller、MVC Model、Templateへの露出なし | READY |
| Full HTML | 通常GET / POST / PRGで対象画面が成立 | READY |
| HTMX | master検索・paging・登録Validationだけに限定 | READY WITH L4 BLOCKER |
| CSRF | Spring Security token / header名をmetaから取得しHTMX requestへ注入 | READY |
| Partial error | 403 / 404 / 500のstatusとsanitized HTML fragmentを維持 | READY |
| Conflict | 通常POST、hidden version、409専用画面、最新detail link、自動retryなし | READY |
| Security | default deny、Permission、resource ownership、部門scope、Sessionを再利用 | READY |

## 3. Gate B axis assessment

| Gate axis | Evidence summary | Current status |
|---|---|---|
| Ownership | Framework / Reference / Toolingの成果物とdependencyをB0〜B4で分離 | READY |
| Query | applicant / approver / accounting scope、paging、count、最終record materializationを実DB確認 | READY |
| MVC | full-page主軸、Form / View DTO / Controller / Use Case境界、OSIV無効 | READY |
| HTMX | 11契約のMockMvc / source / 過去browser Evidenceあり。ただし現PCでL4 journeyが同一点で2回連続timeout | **BLOCKED** |
| Conflict | 409、最新detail、先行だけcommit、後発reason / Auditなし | READY |
| Cache | option一覧だけ、30秒write TTL、master query非cache、command fail-safe | READY |
| Security | default deny、CSRF、Permission、scope、Session、情報非露出を維持 | READY |
| Accessibility | label / live region / status / alert / focus supportあり。keyboard-onlyとscreen readerの直接Evidence不足 | **BLOCKED FOR CHECKPOINT** |
| Hybrid verification | L1〜L6の個別Evidenceあり。L4安定性とaccessibility分類を閉じた後に最終集約が必要 | NOT READY |
| Clean aggregate | accepted P3-B4 HEADのRoot 16 / 16は成功済み。inventory後のclean HEAD最終実行は未実施 | PENDING FINAL RUN |
| Deferred | REST、distributed cache、Framework昇格、SPA、Level 2、remote変更は未着手 | READY |

現時点のGate B判定は`INCOMPLETE`である。これはP3-B1〜B4の個別完了を否定するものではなく、Gate B固有の
横断受入条件である安定した自動browser journeyとaccessibility checkpointが未完了であることを示す。

## 4. DoD / acceptance trace

| Requirement | Milestone B Evidence | Gate B treatment |
|---|---|---|
| DoD 3-4 EntityをTemplateへ渡す違反のCI検出 | Architecture test、MVC Model / Template inventory、Public API 0型 | SATISFIED |
| DoD 3-5 HTMX検索・paging・部分更新とCSRF自動注入 | B3 MockMvc / source / browser journey | FUNCTION SATISFIED / L4 STABILITY OPEN |
| DoD 3-6 2 Session後発競合画面 | B4 PostgreSQL / MockMvc / 2 BrowserContext / DB / Audit | SATISFIED |
| DoD 3-7 master JPA射影 / expense JdbcClient | B1の実DB scope / materializationとB2画面利用 | SATISFIED |
| DoD 3-8 申請・承認・却下のBusiness Audit | Milestone A、B2 HTTP、B4先行承認 / 後発rollback | SUPPORTED BY COMBINED EVIDENCE |
| DoD 3-9 cache TTL後再読込 | B4 focused testと30秒Owner目視 | SATISFIED |
| AC-P3-08 同一versionの2 Session競合 | B4と2026年9月15日の全3回で競合test成功 | SATISFIED |
| AC-P3-09 actor scope内だけのquery | B1 PostgreSQLでscope外row非返却 | SATISFIED |

DoD 3-10 / AC-P3-10のRESTおよびDoD 3-11のCI E2EはMilestone Cの責務であり、Gate B達成主張へ含めない。

## 5. Hybrid verification coverage

| Layer | Current Evidence | Coverage / gap |
|---|---|---|
| L1 Domain / Application | 状態、不変条件、Permission、version、Audit、cache fail-safe | COVERED |
| L2 Web / Repository / PostgreSQL | MockMvc、JPA射影、JdbcClient scope、optimistic lock、TTL | COVERED |
| L3 packaged HTTP journey | B2のlogin、Form、Validation、draft / submit、log / Audit / DB | COVERED |
| L4 automated real browser | B3 HTMXとB4競合。B4競合は現在も安定、B3 HTMXは現PCで連続timeout | **BLOCKING GAP** |
| L5 human visual / manual | full HTML、Validation、history、focus、409、TTL前後、stale拒否 | ACCESSIBILITY DETAILS OPEN |
| L6 log / Audit / DB | B2〜B4でstate / version / reason / Audit / sanitized logを突合 | COVERED |

同一scenarioを全層へ重複させていない。query scopeと業務規則はL1 / L2、packageされた外部挙動はL3、
HTMX / 2 Sessionの利用者操作はL4 / L5、backendの事実はL6がそれぞれ主に担う。

## 6. Accessibility inventory

### 6.1 Directly verified or source-confirmed

| Area | Evidence | Classification |
|---|---|---|
| Document language / landmarks | `lang="ja"`、`main`、主要menu / pagingの`nav`と`aria-label` | SOURCE CONFIRMED |
| Form labels | master検索・登録、expense入力、競合操作にnative `label` / controlを使用 | SOURCE CONFIRMED |
| Result announcements | query結果に`aria-live="polite"` / `aria-atomic="true"` | SOURCE + MockMvc |
| Loading | indicatorに`role="status"`、request中にtargetへ`aria-busy=true` | SOURCE + MockMvc + browser assertion |
| Validation | master登録field errorを`aria-describedby`で関連付け、partial再描画 | SOURCE + MockMvc + browser |
| Partial error | `role="alert"`、`tabindex="-1"`、`data-koiki-focus` | SOURCE + MockMvc |
| Focus after swap | integration scriptが`data-koiki-focus`へfocusしcustom eventを発行 | SOURCE + Owner目視 / browser event |
| Conflict recovery | alert文言と最新detailへのnative link | SOURCE + MockMvc + browser |

loading表示は応答が速くOwnerが視覚的には確認できなかったが、`aria-busy`の開始・終了を既存browser assertion、
MockMvcおよびsourceで確認済みである。この点を未検証とはしない一方、screen readerによる実announcementを
確認済みとは表現しない。

### 6.2 Not directly verified

| Item | Current evidence boundary | Required Gate B treatment |
|---|---|---|
| Keyboard-only journey | native control / link中心だが、Tab / Enter / Shift+Tabを使った明示checkpoint記録なし | master検索・paging・Validation・競合回復の代表経路を確認 |
| Screen reader | semantic markupとlive / status / alertはあるが、読み上げ順・announcementの直接記録なし | Windows Narrator等による代表的smokeを確認、またはOwnerが例外と再判断条件を明記 |
| Field error association全体 | master登録とexpense部門には`aria-describedby`があるが、expense全fieldのerror関連付けを網羅確認していない | source / rendered DOMを限定確認し、必要ならReferenceだけを限定修正 |
| Focusのbrowser assertion | Owner目視とcustom event assertionはあるが、全swap / errorで`document.activeElement`を直接assertしていない | focused browser診断時に代表focusを確認 |

色、responsive layout、zoom倍率および全画面・全Roleの網羅的screen reader試験はGate Bの現行Evidenceにない。
これらをGate Bの達成主張へ暗黙に含めず、Phase 3の代表critical journeyを越える範囲はOwner reviewでdeferredを明示する。

## 7. Browser runner stability observation

### 7.1 New-PC reproduction result

2026年9月15日、accepted handoff HEAD `272a2ab`、Temurin JDK 21.0.12、Maven 3.9.16、
Playwright Java 1.62.0、Chromium 151.0.7922.34（v1234）、PostgreSQL 17 container、package済みReference JARで
headed browser suiteを実行した。各runは新しい使い捨てDBとランダムcredentialを使用し、終了後にApplication、
container、credentialおよび一時logを破棄した。

| Run | HTMX journey | 2 BrowserContext conflict | DB / log observation |
|---:|---|---|---|
| 1 | PASS | PASS | 2 tests、failure / error / skip 0、17.107秒。競合DBは`APPROVED`、version 2、reasonなし |
| 2 | **ERROR** | PASS（2.184秒） | HTMX 2回目検索response待機で30秒timeout。failure 0 / error 1 |
| 3 | **ERROR** | PASS（2.042秒） | 同じline 80で30秒timeout。Application ERROR severity 0、競合DBは`APPROVED`、version 2、reasonなし |

失敗箇所は`ReferenceHtmxJourneyTest.exercisesSelectedMasterHtmxInteractionsInARealBrowser()`の、最初の
`#department-query` outerHTML swap後に検索inputへ`P3B3_NO_MATCH_B`をfillし、次のHTMX GET responseを待つ箇所である。
初回検索、loginおよび独立2 BrowserContext競合は同じ環境で成立している。

### 7.2 Assessment

同じ箇所の2回連続timeoutを単発の外部揺らぎとして無視しない。Architecture Ownerは2026年9月15日、
この再現結果をGate Bで確認・原因整理が必要なbrowser runner安定性事項として扱う方向を確認した。

現時点ではproduct側HTMX処理の不成立か、outerHTML swap直後にrunnerが次操作へ進む同期不足かを確定していない。
Application ERRORがなく、初回検索と競合journeyが成立し、過去Owner目視と最初の新PC runが成功しているため、
runnerのswap / settle同期が第一の調査候補である。ただし、原因確認前にtestだけの問題と断定しない。

次の対応ではtimeout延長、無条件retryまたはassertion削除で閉じない。request / HTMX lifecycle / DOM replacementを
secretなしで観測し、次の操作開始条件を特定する。runner同期の問題であれば、outerHTML swap後の安定したlifecycleを
待ち、置換後DOMからlocatorを再取得する等の限定修正を検討する。product側で2回目interactionが欠落する場合は、
Reference / Starterの責務に沿って限定修正する。

### 7.3 Tooling-only setup observation

新PCの明示的Playwright setupではChromiumをinstallした後、最初のbrowser test起動時にPlaywrightがFirefox / WebKitも
ユーザーcacheへ取得し、Surefire dumpstreamへdownload progressとJDK CDS warningを記録した。runnerがlaunchしたengineは
承認どおりChromiumだけであり、Root Reactor、Framework artifact、Reference JARまたはCustomer dependencyへbrowserを
追加していない。production blockerではないが、P3-C2のruntime / cleanup / CI候補評価で、明示setupと自動download抑止の
再現性を確認する非blocking Tooling事項とする。

## 8. Open items and disposition

### 8.1 Blocking before Gate B close

| ID | Item | Close condition |
|---|---|---|
| GB-BLK-01 | HTMX browser journeyが同一箇所で2回連続timeout | 原因をproduct / runner / environmentへ切り分け、限定修正後にfocused headed runを安定して成功させる |
| GB-BLK-02 | keyboard-onlyとscreen readerの代表checkpointが未記録 | §6.2の代表操作を実施し、結果・未確認範囲・deferredを記録する |
| GB-BLK-03 | expense Formのfield error関連付けを網羅確認していない | rendered DOM / sourceを確認し、必要ならReference限定修正とfocused testを行う |
| GB-CLOSE-01 | inventory後clean HEADのRoot Reactor未実行 | blocking解消後のclean HEADで`clean verify`を1回実行し、16 / 16、test、DB、cleanupを記録する |

### 8.2 Nonblocking observations

| Item | Treatment |
|---|---|
| applicantをemail表示する業務上の不自然さ | P3-B2承認どおりIdentityへ氏名を追加せず、employee profile等のOwnership reviewへ送る |
| Identity AuthenticationProvider起動WARN | P3-B4承認どおり既知の構成WARNとして保持する |
| loadingのOwner視覚確認 | automated browser / MockMvc / sourceの複数Evidenceを保持し、screen reader announcementとは分ける |
| Playwright初回のFirefox / WebKit追加download | 非配布Toolingのsetup再現性としてP3-C2で評価する |

## 9. Architecture Owner decision

| ID | Decision proposal | Recommendation | Status |
|---|---|---|---|
| GB-D1 | P3-B1〜B4はOwnership、query、MVC、HTMX、conflict、cache、Securityの一つのWeb / query flowとして整合する | ACCEPT WITH OPEN BLOCKERS | APPROVED（2026年9月15日） |
| GB-D2 | 2026年9月15日の連続timeoutをGate B blocking stability itemとし、無条件retry / timeout延長ではなくfocused原因分析を要求する | REQUIRE | APPROVED（2026年9月15日） |
| GB-D3 | accessibilityの成立済みEvidenceと未確認範囲を§6どおり分離し、代表keyboard / screen reader checkpointをclose条件とする | REQUIRE LIMITED CHECKPOINT | APPROVED（2026年9月15日） |
| GB-D4 | GB-BLK-01〜03の解消後、clean HEADのRoot ReactorをGate B最終close条件として1回実行する | REQUIRE FINAL RUN | APPROVED（2026年9月15日） |
| GB-D5 | applicant表示、全画面網羅accessibility、Playwright setup、REST、distributed cache、Framework昇格、SPA、Level 2、remote変更を明示した先へdeferする | ACCEPT DEFERRED BOUNDARY | APPROVED（2026年9月15日） |

**Decision:** `APPROVED — GATE B INVENTORY / OPEN ITEM DISPOSITION APPROVED; GATE B INCOMPLETE`  
**Approved scope:** §8のblocking / nonblocking分類、GB-D1〜D5、および§10のclose sequence  
**Evidence:** 本inventory、P3-B0〜P3-B4 Evidence、2026年9月15日の新PC browser再現結果  
**Rationale:** P3-B1〜P3-B4のWeb / query flowとしての整合を認める一方、未解決blockerを免除せず、
Gate Bの最終close条件を維持する。  
**Decided by:** Shuichi Kataoka, Architecture Owner  
**Decision date:** 2026年9月15日  
**Revisit trigger:** blockerの分類またはclose条件の変更、Ownership / scopeの変更、あるいは限定修正が
Reference / Tooling境界を越える場合

この承認はGate Bの最終Decisionではない。accepted HEAD、最終Root resultおよびArchitecture Owner close recordは、
blocking itemの解消前に記入しない。

## 10. Proposed close sequence

1. **DONE（2026年9月15日）** Architecture Ownerが§8のdispositionとGB-D1〜D5を承認した。
2. GB-BLK-01についてHTMX request / lifecycle / DOM replacementを限定診断し、責務を確定する。
3. 承認された場合だけ限定修正を行い、focused MockMvc / browser testで回帰する。
4. GB-BLK-02 / 03の代表accessibility checkpointと必要な限定修正を完了する。
5. clean HEADでRoot Reactor `clean verify`を1回実行し、package、PostgreSQL、test、cleanupを記録する。
6. Gate B Architecture Owner final review後にだけ`COMPLETE / ACCEPTED`、実行計画、validation indexを更新する。
7. Gate B承認後にだけP3-C0最小REST API contract reviewへ進む。

## 11. Deferred boundary

- P3-C0以降のREST endpoint、DTO、Permission、status、Problem Details、optimistic lock契約。
- P3-C2のcritical journey E2E、CI候補、runtime / flakiness / cleanup測定。
- workflow、required check、remote push / PR / merge、ruleset、snapshot publish。
- distributed cache、Redis、event-driven eviction、cache Starter / Public API、独自property。
- SPA、Spring Modulith Level 2 / 3、MyBatis accounting、Oracle、AWS固有Adapter。
- Reference Form、Template、競合画面、cache、browser fixtureのFramework昇格。
- Phase 3代表critical journeyを越える全画面・全Role・全支援技術のaccessibility certification。

## 12. Evidence sources

- `docs/development/KOIKI-JavaWeb-FW_Phase3実行計画_v0.1.md`
- `docs/development/phase3-gate-b-start-handoff-20260914.md`
- `docs/architecture/validation/phase3-p3-b0-mvc-htmx-contract-review.md`
- `docs/architecture/validation/phase3-p3-b1-read-model.md`
- `docs/architecture/validation/phase3-p3-b2-mvc-thymeleaf.md`
- `docs/architecture/validation/phase3-p3-b3-htmx.md`
- `docs/architecture/validation/phase3-p3-b4-lock-cache-contract-review.md`
- `docs/architecture/validation/phase3-p3-b4-lock-cache.md`
- `docs/reference/KOIKI-JavaWeb-FW_Reference_Application_Specification_v0.1.md`
- `docs/architecture/grand-design/KOIKI-JavaWeb-FW_グランドデザイン_v0.2.md`
- `koiki-starters/koiki-starter-web-mvc/src/main/resources/META-INF/resources/koiki-web/koiki-htmx.js`
- `koiki-reference-app/src/main/resources/templates/master/departments.html`
- `koiki-reference-app/src/main/resources/templates/master/expense-categories.html`
- `koiki-reference-app/src/main/resources/templates/expense/form.html`
- `koiki-reference-app/src/main/resources/templates/expense/conflict.html`
- `koiki-reference-app/src/test/java/org/koikifw/reference/web/ReferenceBusinessUrlSecurityTest.java`
- `build-support/reference-browser-verification/src/test/java/org/koikifw/buildsupport/referencebrowser/ReferenceHtmxJourneyTest.java`
