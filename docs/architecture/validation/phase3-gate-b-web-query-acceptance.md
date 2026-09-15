# Phase 3 Gate B — Web / query acceptance inventory

**Status:** INVENTORY / DISPOSITION OWNER APPROVED — GATE B INCOMPLETE / BLOCKING ITEMS OPEN  
**Inventory date:** 2026年9月15日  
**Branch:** `feature/phase3-reference-vertical-slice`  
**Inventory HEAD:** `272a2ab7c792a6b6fcf84c867b3697843fe907b5`  
**Architecture Owner:** Shuichi Kataoka  
**Production change:** Framework内部HTMX focus resource / Reference accessibility templateの限定修正
**Next action:** Gate B accessibility remediationを単一commitとし、commit後clean HEADでGB-CLOSE-01を実行

## 1. Review objective and boundary

本記録は、P3-B1〜P3-B4を個別成果の寄せ集めではなく、Reference ApplicationのWeb / queryを構成する
一つのMilestoneとして横断評価するGate B棚卸し案である。次の3区分でEvidenceを分類する。

1. **直接検証済み** — 対応する自動test、実browser、DB観測またはartifact inventoryがある。
2. **横断Evidenceで支持** — 複数CPの実装境界と検証結果を組み合わせると主張が成立する。
3. **未確認 / blocking / deferred** — Gate Bの達成主張へ含めず、再確認条件または後続CPを明示する。

本inventory開始時点ではproduction codeを変更しなかった。その後、blocking checkpointで検出した不足に対し、
Architecture Ownerの個別承認を得てFramework内部HTMX resourceとReference accessibility templateだけを限定修正した。
Public API、Maven module、migration、dependency、workflowまたはremote設定は変更しない。Gate Bが
`COMPLETE / ACCEPTED`となる前にP3-C0 REST contract reviewへ進まない。

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
| L4 automated real browser | B3 HTMXとB4競合。GB-BLK-01はclose済み。keyboard checkpointでouterHTML error focus gapを検出 | **ACCESSIBILITY BLOCKER OPEN** |
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
| Focus after swap | integration scriptが接続済みtarget内の`data-koiki-focus`へfocusしcustom eventを発行 | SOURCE + browser active-element assertion |
| Conflict recovery | alert文言と最新detailへのnative link | SOURCE + MockMvc + browser |

loading表示は応答が速くOwnerが視覚的には確認できなかったが、`aria-busy`の開始・終了を既存browser assertion、
MockMvcおよびsourceで確認済みである。この点を未検証とはしない一方、screen readerによる実announcementを
確認済みとは表現しない。

### 6.2 Not directly verified

| Item | Current evidence boundary | Required Gate B treatment |
|---|---|---|
| Keyboard-only journey | master検索・paging・Validation・競合回復をTab / Enter / Shift+Tabで確認済み | 代表経路は完了。全画面網羅はdeferred boundaryを維持 |
| Screen reader | Windows Narratorでmaster Validation後のlabel / 関連errorを直接確認し、検索後の画面内容も読み上げ可能。件数固有announcementは非blocking観察 | Gate Bの限定checkpointを完了。全支援技術・固有announcement保証はdeferred boundaryを維持 |
| Field error association全体 | expense全7可視fieldをsource / MockMvc / rendered DOMで確認済み | GB-D8で承認し、GB-BLK-03をclose済み |
| Focusのbrowser assertion | master検索focus保持とValidation error focusを`document.activeElement`で直接確認済み | 代表経路は完了。全swap網羅へ拡張しない |

色、responsive layout、zoom倍率および全画面・全Roleの網羅的screen reader試験はGate Bの現行Evidenceにない。
これらをGate Bの達成主張へ暗黙に含めず、Phase 3の代表critical journeyを越える範囲はOwner reviewでdeferredを明示する。

### 6.3 Gate B representative checkpoint result

2026年9月15日、HEAD `153e262`をbaselineに代表checkpointを実施した。検出したouterHTML error-focus gapは、
Architecture Ownerの個別承認後にFramework内部HTMX resourceへ限定修正した。

| Checkpoint | Result | Evidence boundary |
|---|---|---|
| master検索 | PASS | Tabで検索inputへ到達し、keyboard入力後のHTMX GET、swap、URL更新を確認 |
| master paging | PASS | 使い捨てDBへcheckpoint専用20件を一時投入し、native「次へ」linkへTabで到達してEnter遷移。正式seedは未変更 |
| master Validation | PASS | Tab / Shift+Tab / EnterでPOSTとerror描画が成立し、新fragmentの最初のerror controlへのfocusを直接assert |
| conflict recovery | PASS | 409画面のnative回復linkへTabで到達し、Enterで最新detailへ遷移 |
| expense error association | PASS | 7可視fieldすべてのnative label、`aria-describedby`、対応する非空error要素をrendered DOMで確認 |
| Screen reader | PASS / LIMITED CHECKPOINT | Windows NarratorでValidation errorを直接確認し、master検索後の画面内容も読み上げ可能。件数固有announcementは確認できず、非blocking観察へ分離 |

keyboard強化後のheaded browser suiteは、focus marker追加前の検索、Validation操作、expense error関連付け、409回復を
3 tests、failure / error / skip 0、7.789秒で成功した。Framework修正後は通常30秒timeoutのheaded suiteを再実行し、
同じ3 testsがfailure / error / skip 0、7.627秒で成功した。この再検証にはmaster検索のfocus保持とValidation後の
`#department-create #code`へのactive-element assertionを含む。paging checkpointも一時testで成功し、testと追加dataは破棄した。
一覧1ページに20件を表示すると行ごとの名称更新・廃止controlが先行し、「次へ」までのTab回数が多くなるが、link自体は
到達・操作可能である。これはGate B blockerではない操作性観察とし、全画面UX改善へscopeを拡張しない。

GB-BLK-03では、expense Formの部門以外の6 fieldにerror関連付けがないことを検出した。Reference templateへ各error IDと
`aria-describedby`を追加し、focused MockMvc 1 testとheaded rendered-DOM browser testを成功させた。Domain、Use Case、
Form binding、migration、FrameworkまたはPublic APIは変更していないため、GB-BLK-03の技術的close条件を満たす。

### 6.4 HTMX outerHTML error-focus finding

master Validationの新fragmentで最初のerror controlへ条件付き`data-koiki-focus`を描画し、MockMvcではmarkerを確認した。
一方、headed / headless browserの`document.activeElement`待機は通常30秒および診断用5秒でtimeoutした。secretを含まない
lifecycle観測結果は次のとおりである。

| Phase | event target | `isConnected` | target内marker | active element |
|---|---|---:|---|---|
| query `afterSwap` / `afterSettle` | 旧`department-query` | false | 旧`search` | `search` |
| Validation `afterSwap` / `afterSettle` | 旧`department-create` | false | なし | body |

HTMX outerHTML swapでは`event.detail.target`が切断済み旧DOMを指す。現行KOIKI integration scriptはこの旧target内で
`data-koiki-focus`を検索するため、新fragmentのmarkerを発見できない。検索inputのfocusはHTMX自身の同一ID保持で成立しており、
integration scriptが置換後DOMをfocusできる証拠ではなかった。原因をFramework内部HTMX focus処理のlive target解決不足へ
分類する。

Architecture Ownerは責務越境としての個別確認後、Framework内部限定修正への着手を承認した。`koiki-htmx.js`内部に、
切断済みtargetと同じIDの接続済み要素をdocumentから解決し、見つからなければ従来targetへ戻す処理を追加した。
focus探索と`koiki:htmx:afterSwap`のdetailはこのlive targetを使用する。custom event名、Public Java API、Reference MVC、
timeoutまたはHTMX採用範囲は変更していない。

修正済みresourceの配信契約、master Validation markerおよびexpense error関連付けをfocused MockMvc 3 testsで確認し、
failure / error / skip 0で成功した。続いて使い捨てPostgreSQL、package済みReference JAR、Chromium headedで通常timeoutの
3 browser testsを実行し、failure / error / skip 0、7.627秒で成功した。Validation後のactive elementは新しい
`#department-create #code`であり、旧target参照によるfocus gapは解消した。使い捨てApplication、DB、credentialは破棄済みである。

同日、Architecture OwnerはChromeのnative constraint validationとKOIKIのserver-side Validationを分けて確認した。
checkpoint時だけBrowser consoleで対象formの`noValidate`を有効にし、HTMX partial再描画後、Windows Narratorが
「部門コード」と関連する正規表現errorを読み上げることを直接確認した。この操作は使い捨てBrowser DOMだけへの変更であり、
Reference template、ApplicationまたはFramework artifactを追加変更していない。これによりValidation announcementの
代表screen-reader checkpointをPASSとする。master検索結果更新の`aria-live` announcementは別checkpointとして継続確認した。

続くmaster検索checkpointでは、Architecture OwnerはNarratorが画面上の内容を読み上げる挙動を確認したが、
「部門 0件」等の検索結果更新に固有のannouncementは確認できなかった。これをscreen reader全体の不成立とはせず、
同時に`aria-live` announcementのPASSとも判定しない。source上は`aria-live="polite"` / `aria-atomic="true"`を持つ
`#department-results`が、親`#department-query`の`outerHTML` swapとともに除去・再生成される。既存live regionの
内容更新ではなくlive region自体の再挿入になることが影響した可能性があるが、現時点では実測に基づく原因候補であり、
Reference viewのfocused診断前に修正方針へ固定しない。

### 6.5 Master search announcement requirement trace and disposition

検索結果件数の固有announcementがGate Bの承認済み要件かを、上位設計から実装Evidenceまで逆向きに確認した。

| Source | Required accessibility boundary | Result-count announcement |
|---|---|---|
| グランドデザイン§13.2 / §13.4 | accessibility規約、screen reader、keyboard、focus管理 | 固有文言または検索件数announcementの指定なし |
| Phase 3実行計画§6.2 / Gate B | JavaScript有効下のaccessibility、自動browser、Owner実演 | 固有announcementのexit criteriaなし |
| P3-B0-D5 / HTMX 11契約 | semantic HTML、keyboard、focus、error関連付け、loading通知 | loadingは明示。検索はdebounce / query整合であり、件数announcementは未指定 |
| P3-B1〜B4 Evidence | query scope、MVC、HTMX、conflict、cacheの個別受入 | 固有announcementをPublic / Reference契約として固定していない |
| Gate B §6.2初期inventory | 未確認範囲を安全側に抽出 | 上位要件の転記ではなく、Gate内で追加した確認候補 |

W3Cの[WCAG 2.2 Understanding SC 4.1.3](https://www.w3.org/WAI/WCAG22/Understanding/status-messages.html)では、
検索結果一覧そのものはstatus updateではない一方、非フォーカス更新される「18 results returned」「No results returned」等の
短い表示はstatus messageになり得ると説明する。現行caption「部門 N 件」は`aria-live="polite"` / `aria-atomic="true"`を持つ
領域内にあり、[WAI-ARIA 1.2](https://www.w3.org/TR/wai-aria-1.2/#aria-live)が定める更新予告のprogrammatic propertyは存在する。
ただしKOIKI Phase 3はWCAG適合level、特定screen readerでの発話文言、または件数固有announcementを受入契約としていない。

HTMX公式の[`hx-swap`](https://htmx.org/attributes/hx-swap/)どおり、`outerHTML`はtarget要素全体を置換する。
今回のNarrator観測はlive region再挿入時の相互運用性を示すが、keyboard操作、検索結果への到達、画面内容の読み上げ、
Validation error関連付け / focus / 読み上げは成立している。固有announcementのためにswap境界を変更すると、承認済み
検索・paging・history・focus契約へ新たな変更を入れることになる。

以上から、件数固有announcementをGate B blockerとして新設せず、screen reader / browser組合せの非blocking相互運用性観察へ
再分類する。Architecture OwnerはGB-D8でこのdispositionを承認し、限定keyboard / screen-reader checkpointを満たした
GB-BLK-02をcloseした。
将来WCAG適合levelまたはstatus message契約を定める場合は、安定live region、`role="status"`、swap境界および複数支援技術での
実測を一体で再評価する。現時点ではReference templateまたはFramework resourceを追加修正しない。

この人系確認前のコミット前worktreeでは、Root Reactor `clean verify`が16 / 16 SUCCESS、Reference 86 testsを含めて
failure / error / skip 0、1分12秒で成功した。そのpackage済みJARに対するChromium headed browser suiteも3 tests、
failure / error / skip 0、7.454秒で成功した。Evidence追記後かつ手動確認環境を維持中であるため、このrunを
GB-CLOSE-01のコミット後clean HEAD / cleanup済み最終runへは代用しない。

OwnerのNarrator / browser checkpoint完了後、保持していたReference Application processを停止し、`--rm`指定の
`koiki-reference-postgres`を停止・自動削除した。Application process不在、container不在およびhost側18080 / 55432の
解放を直接確認し、ランダムcredentialと使い捨て業務dataを破棄した。操作用に表示したcredential windowはApplication / DBを
所有しないためcleanup対象processへ含めず、不要になった時点で利用者が閉じる。

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

同日、承認済みinventory HEAD `3463d1c`で、testへ一時的なsecretを含まないlifecycle counterだけを追加し、
同じheaded focused journeyを再現した。第2検索のinput event時点とtimeout後の観測値は次のとおりである。

| Observation | Value |
|---|---|
| `koiki:htmx:afterSwap` count | 1 |
| timeout後の`htmx:afterSettle` count | 1 |
| 第2input event時点の`htmx:afterSettle` count | 0 |
| 第2input value | `P3B3_NO_MATCH_B` |
| timeout後URL | `?search=P3B3_NO_MATCH_A` |

outerHTML swap後の新しいinputへ値は設定されたが、その要素をHTMXがsettleしてtriggerを結線する前にinput eventが発生したため、
第2HTMX GET自体が発火していなかった。response predicateの取りこぼし、Applicationの応答停止、Reference MVCまたは
Starter integrationの機能不成立ではない。原因を**Browser Toolingのlifecycle同期不足**へ分類する。

診断実験として、第1swap後にnative `htmx:afterSettle`を明示的に待ってから第2inputを操作したところ、同一Application / DBに
対してbrowserを毎回作り直したheaded focused journeyが3 / 3 PASSした。固定sleep、timeout延長、無条件retry、assertion削除、
product code変更またはDB更新は行っていない。観測用test変更は実験後にすべて戻した。

Architecture Ownerは2026年9月15日、この切り分けとTooling限定修正を確認した。非配布
`ReferenceHtmxJourneyTest`へ第1swapのnative `htmx:afterSettle` counterと待機を追加し、置換後DOMの初期化完了後に
第2検索へ進むよう修正した。KOIKI integration eventの意味、Reference template / MVC、Framework artifact、Public API、
assertionまたはtimeout既定値は変更していない。

恒久修正後、通常の30秒timeoutを維持したheaded focused journeyを、同一Application / DBに対してbrowser / JVMを毎回
作り直して3回連続実行し、3 / 3 PASSした。無条件retryはなく、使い捨てApplication、DB、credentialおよび一時logは
実行後に破棄した。以上によりGB-BLK-01のclose条件を満たした。
Architecture Ownerは同日、限定修正、再検証結果およびGB-BLK-01のcloseを確認し、承認した。

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
| GB-BLK-01 | **CLOSED / OWNER APPROVED** — Browser ToolingのouterHTML swap / settle同期不足 | native `htmx:afterSettle`同期をTooling限定で適用し、通常timeoutのfocused headed runが3 / 3 PASS（2026年9月15日） |
| GB-BLK-02 | **CLOSED / OWNER APPROVED** — keyboard、focus、Narrator Validationと検索内容読み上げが成立。件数固有announcementは承認済み要件でなく、nonblockingへ再分類 | §6.5のrequirement traceとGB-D8をOwnerが承認（2026年9月15日） |
| GB-BLK-03 | **CLOSED / OWNER APPROVED** — expense全7可視fieldを関連付け済み | focused MockMvc 1 testとheaded rendered-DOM browser testがPASSし、Ownerがcloseを承認（2026年9月15日） |
| GB-CLOSE-01 | inventory後clean HEADのRoot Reactor未実行 | blocking解消後のclean HEADで`clean verify`を1回実行し、16 / 16、test、DB、cleanupを記録する |

### 8.2 Nonblocking observations

| Item | Treatment |
|---|---|
| applicantをemail表示する業務上の不自然さ | P3-B2承認どおりIdentityへ氏名を追加せず、employee profile等のOwnership reviewへ送る |
| Identity AuthenticationProvider起動WARN | P3-B4承認どおり既知の構成WARNとして保持する |
| loadingのOwner視覚確認 | automated browser / MockMvc / sourceの複数Evidenceを保持し、screen reader announcementとは分ける |
| master検索件数のNarrator固有announcement | `aria-live` semanticsは存在するが、outerHTML再挿入時に固有発話を確認できず。Gate B要件へ追加せず、将来のWCAG level / status message契約時に再評価する |
| Playwright初回のFirefox / WebKit追加download | 非配布Toolingのsetup再現性としてP3-C2で評価する |
| master一覧のpaging linkまでのTab回数 | native linkは到達・Enter操作可能。行単位操作が先行するUXは全画面改善へ拡張せず後続評価へ送る |

## 9. Architecture Owner decision

| ID | Decision proposal | Recommendation | Status |
|---|---|---|---|
| GB-D1 | P3-B1〜B4はOwnership、query、MVC、HTMX、conflict、cache、Securityの一つのWeb / query flowとして整合する | ACCEPT WITH OPEN BLOCKERS | APPROVED（2026年9月15日） |
| GB-D2 | 2026年9月15日の連続timeoutをGate B blocking stability itemとし、無条件retry / timeout延長ではなくfocused原因分析を要求する | REQUIRE | APPROVED（2026年9月15日） |
| GB-D3 | accessibilityの成立済みEvidenceと未確認範囲を§6どおり分離し、代表keyboard / screen reader checkpointをclose条件とする | REQUIRE LIMITED CHECKPOINT | APPROVED（2026年9月15日） |
| GB-D4 | GB-BLK-01〜03の解消後、clean HEADのRoot ReactorをGate B最終close条件として1回実行する | REQUIRE FINAL RUN | APPROVED（2026年9月15日） |
| GB-D5 | applicant表示、全画面網羅accessibility、Playwright setup、REST、distributed cache、Framework昇格、SPA、Level 2、remote変更を明示した先へdeferする | ACCEPT DEFERRED BOUNDARY | APPROVED（2026年9月15日） |
| GB-D6 | GB-BLK-01のTooling限定`htmx:afterSettle`同期、通常timeoutのheaded focused 3 / 3 PASSおよびblocker closeを受け入れる | ACCEPT BLOCKER CLOSE | APPROVED（2026年9月15日） |
| GB-D7 | outerHTMLの切断済みtargetを接続済み同一ID要素へ解決するFramework内部限定修正へ進み、公開契約を変えずfocused再検証する | AUTHORIZE LIMITED FRAMEWORK FIX | APPROVED（2026年9月15日） |
| GB-D8 | 件数固有announcementは承認済みGate B要件でないため追加修正せず、Narrator相互運用性をnonblockingへ再分類してGB-BLK-02をcloseする | ACCEPT BLOCKER CLOSE | APPROVED（2026年9月15日） |

**Decision:** `APPROVED — GATE B INVENTORY / OPEN ITEM DISPOSITION APPROVED; GATE B INCOMPLETE`  
**Approved scope:** §8のblocking / nonblocking分類、GB-D1〜D5、および§10のclose sequence  
**Evidence:** 本inventory、P3-B0〜P3-B4 Evidence、2026年9月15日の新PC browser再現結果  
**Rationale:** P3-B1〜P3-B4のWeb / query flowとしての整合を認める一方、未解決blockerを免除せず、
Gate Bの最終close条件を維持する。  
**Decided by:** Shuichi Kataoka, Architecture Owner  
**Decision date:** 2026年9月15日  
**Revisit trigger:** blockerの分類またはclose条件の変更、Ownership / scopeの変更、あるいは限定修正が
Reference / Tooling境界を越える場合

**GB-D6 Decision:** `APPROVED — GB-BLK-01 CLOSED`

**Approved scope:** 非配布Browser Toolingの限定修正、通常timeoutのheaded focused再検証結果、GB-BLK-01 close

**Decided by:** Shuichi Kataoka, Architecture Owner

**Decision date:** 2026年9月15日

**GB-D7 Decision:** `APPROVED — LIMITED FRAMEWORK FIX AUTHORIZED`

**Approved scope:** `koiki-htmx.js`内部のlive target解決、Referenceの条件付きfocus marker、MockMvc / headed browser再検証

**Decided by:** Shuichi Kataoka, Architecture Owner

**Decision date:** 2026年9月15日

**GB-D8 Decision:** `APPROVED — GB-BLK-02 / GB-BLK-03 CLOSED`

**Approved scope:** §6.5のrequirement trace、件数固有announcementのnonblocking再分類、限定keyboard / Narrator checkpoint、expense全7可視fieldのerror関連付け、およびGB-BLK-02 / GB-BLK-03 close

**Decided by:** Shuichi Kataoka, Architecture Owner

**Decision date:** 2026年9月15日

この承認はGate Bの最終Decisionではない。accepted HEAD、最終Root resultおよびArchitecture Owner close recordは、
blocking itemの解消前に記入しない。

## 10. Proposed close sequence

1. **DONE（2026年9月15日）** Architecture Ownerが§8のdispositionとGB-D1〜D5を承認した。
2. **DONE（2026年9月15日）** GB-BLK-01を限定診断し、Browser Toolingのswap / settle同期不足と確定した。
3. **DONE / OWNER APPROVED（2026年9月15日）** Tooling限定修正を適用し、通常timeoutのfocused headed browser testが3 / 3 PASSした。
4. **DONE / OWNER APPROVED（2026年9月15日）** Framework内部focus修正、headed再検証、Narrator限定checkpointおよびrequirement traceが完了し、GB-BLK-02 / GB-BLK-03をcloseした。
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
