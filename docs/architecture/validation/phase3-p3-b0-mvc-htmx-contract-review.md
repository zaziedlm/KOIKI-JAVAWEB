# Phase 3 P3-B0 MVC / HTMX contract review

## 1. Status and boundary

| Item | Result |
|---|---|
| Date | 2026年9月13日 |
| Architecture Owner | Shuichi Kataoka |
| Phase / CP | Phase 3 Reference Vertical Slice / P3-B0 |
| Status | COMPLETE / ARCHITECTURE OWNER APPROVED / P3-B1 READY |
| Branch / review start HEAD | `feature/phase3-reference-vertical-slice` / `5d1313d8a695a70ef03f7676ac68f03cde4aa3b7` |
| Primary ownership | Framework MVC / HTMX共通契約、Reference業務画面、Tooling browser harness |
| Production implementation | 未開始 |

P3-B0は、P3-B1〜B4より前にMVC / Thymeleaf / HTMXのartifact、dependency、asset、
Security統合およびbrowser runnerの境界を固定する文書CPである。本reviewではproduction code、
Public API、Maven module、migration、workflow、dependency、browser binaryまたはremote設定を変更しない。

Architecture Ownerは§11を承認した。次に開始できるproduction CPはP3-B1だけであり、P3-B2以降を先行しない。

## 2. Authoritative inputs

- `docs/architecture/grand-design/KOIKI-JavaWeb-FW_グランドデザイン_v0.2.md` §8.7、§13.1〜13.4、ADR-006 / 027
- `docs/architecture/KOIKI-JavaWeb-FW_Repository_Architecture_v0.1.md`
- `docs/architecture/KOIKI-JavaWeb-FW_Baseline_Compatibility_v0.1.md`
- `docs/development/KOIKI-JavaWeb-FW_Phase3実行計画_v0.1.md`
- `docs/reference/KOIKI-JavaWeb-FW_Reference_Application_Specification_v0.1.md`
- `docs/architecture/validation/phase3-gate-a-milestone-a-acceptance.md`
- `docs/agent/skills/koiki-project-overview/SKILL.md`
- `docs/agent/skills/koiki-business-feature-work/SKILL.md`

第三者libraryの適合性は2026年9月13日に各projectの公式文書、公式repositoryおよびMaven Centralで
再確認した。参照先は§12に記録する。

## 3. Current baseline inventory

| Boundary | Current fact | P3-B0 implication |
|---|---|---|
| Release / Reactor | formal release unit 14 projects / 11 JAR、Root Reactor 15 projects / 12 JAR（Referenceを含む）。`koiki-starter-web-mvc`は存在しない | Starter新設はformal release / publish inventory変更になる |
| Reference | 単一`koiki-reference-app`。Spring MVCとThymeleafへ直接依存 | 後続でStarter利用へ置換できるが、業務画面はReferenceが所有する |
| Existing browser UI | Identity管理controllerとThymeleaf templateだけ | master / expense画面、共通layout、HTMXは未実装 |
| Security | Phase 2 default deny、CSRF、headers、Form Login、Session JDBCを利用 | 新しい認証基盤やSession storeは不要 |
| Business authorization | P3-A3でPermission、所有権、部門scope、自己承認拒否をApplication Use Caseに実装済み | Controllerや表示制御を最終認可にしない |
| OSIV | Framework baselineで無効 | Entity / Domain ModelをTemplateへ渡さずread model / View DTOをmaterializeする |
| HTMX | Java library、JavaScript assetとも未導入 | version、配布経路、request / response契約を先に固定する |
| Browser automation | repository内にrunnerなし | Referenceやformal Framework成果物と分離したToolingが必要 |
| Workflow / remote | P3-B0の対象外 | CI required化、workflow変更、browser image固定はRemote Gateまで行わない |

## 4. Proposed ownership and artifact decision

### 4.1 UI baseline and HTMX adoption principle

Phase 3のserver-side UIの主軸は、Spring MVCがUse Caseを呼び出し、Thymeleafが完全にmaterializeされた
View DTOからHTMLを生成する構成である。HTMXはこの土台を置換するUI frameworkではなく、server-rendered
HTML fragmentを利用して操作上の効果が得られる箇所だけに適用するinteraction layerとする。

次をHTMX適用の必要条件とする。

1. 全画面遷移を避けることで、検索・paging・部分validation等の操作継続性が明確に改善する。
2. 更新対象DOM、URL / history、loading、focusおよびerror時の挙動を明示できる。
3. 通常MVCと別の業務処理、認可またはqueryを作らず、同じUse Case / read modelを再利用できる。
4. MockMvcと実browserの双方で、CSRF、status、fragmentおよびaccessibilityを安定して検証できる。

これらを満たさない画面や操作は通常のMVC / Thymeleafによるfull-page render、Form送信およびPRGを使う。
HTMX assetが同梱されること、または11契約をFrameworkとして標準化することを理由に、全link、全Form、
全CRUD操作へ`hx-*`属性を付けない。

### P3-B0-D1 — formal `koiki-starter-web-mvc`

**Recommendation: APPROVE**

`koiki-starter-web-mvc`をReference専用実証ではなく、グランドデザイン§13.2どおり正式Framework artifactとする。
ただし、将来機能の空moduleにはせず、最初の実装時点から次の再利用可能な責務を持たせる。

| Starter owns | Starter does not own |
|---|---|
| Spring MVC / Thymeleaf / Validationのdependency aggregation | master / expenseのController、route、文言、Form、View DTO |
| HTMX assetの固定・classpath配布 | 業務Template、業務navigation、業務Permission code |
| 共通layout、HTMX利用時のCSRF meta、外部JavaScriptによるCSRF header設定 | 認証方式、user store、Session store、業務認可 |
| full / fragment判定、redirect、partial errorの最小内部support | Reference Entity / Domain Model / Repository / query |
| Reference非依存のbase styleとaccessibility既定 | Customer固有theme、画面構成、migration |

新設後の想定inventoryはformal release unit 15 projects / 12 JAR、Root Reactor 16 projects / 13 JAR、
Root aggregatorを除くformal publish unit 14座標である。formal publish対象には
`org.koikifw:koiki-starter-web-mvc`を1座標追加し、BOMでversion管理する。Reference Applicationを
Starterへ混入させず、StarterからReferenceへのdependencyを禁止する。

このartifact / dependency変更はP3-B0承認後、画面実装を開始するP3-B2で行う。P3-B1では先行して
read modelを作るため、空Starterを先に生成しない。

### P3-B0-D2 — Java Public API and resource contract

**Recommendation: APPROVE**

最初のsliceではKOIKI独自のpublic annotation、Controller base class、Form base classまたはView DTOを
追加しない。Spring MVCの標準annotation、request header、Model、BindingResultおよびThymeleafを使う。

- Java Public API追加は0型を初期目標とする。
- auto-configuration実装は`org.koikifw.starter.webmvc.internal`へ閉じる。
- 配布するclasspath resource、fragment名、asset URLおよび設定propertyはJava型でなくても
  consumer-visible contractとしてinventory / compatibility testの対象にする。
- 実装Evidenceによりpublic helperが不可避になった場合は、型・signature・代替案を示して
  Architecture Ownerへ再reviewし、便宜的にPublic APIへ昇格させない。

## 5. Dependency and HTMX asset decision

### P3-B0-D3 — dependency set

**Recommendation: APPROVE**

`koiki-starter-web-mvc`のproduction dependencyを次に限定する。

| Dependency | Scope / management | Purpose |
|---|---|---|
| `spring-boot-starter-webmvc` | compile / Spring Boot BOM | Servlet MVC |
| `spring-boot-starter-thymeleaf` | compile / Spring Boot BOM | server-side HTML / fragments |
| `spring-boot-starter-validation` | compile / Spring Boot BOM | Form validation |
| `org.webjars.npm:htmx.org:2.0.10` | compile、KOIKI BOMでversion固定 | local HTMX asset |
| `org.jspecify:jspecify` | compile / KOIKI BOM | null-safety annotation |

`koiki-starter-security`、`koiki-starter-identity`および`koiki-starter-session-jdbc`は
web-mvc Starterから強制しない。Security profileは利用applicationが明示的に選び、Referenceは既存の
`koiki-starter-session-jdbc`を継続利用する。これによりweb-mvc Starter単体利用がOIDC、Bearer、
IdentityまたはJDBC Sessionを暗黙に有効化しない。

Referenceの直接`spring-boot-starter-webmvc` / `spring-boot-starter-thymeleaf` dependencyは、Starterが
同じ能力を提供するP3-B2で`koiki-starter-web-mvc`へ置換し、二重宣言を残さない。

### P3-B0-D4 — HTMX integration library

**Recommendation: DO NOT ADOPT `htmx-spring-boot`; USE THE APPROVED FALLBACK**

グランドデザインは`wimdeblauwe/htmx-spring-boot`を第一候補としながら、Spring Boot新版への追従が
不十分な場合は1〜2人週規模の小さな代替へ切り替える境界を定めている。現行調査では、同library 5.1.0の
compatibility表はSpring Boot 4.0.3を示し、Spring InitializrのHTMX対応範囲も4.1.0-M1未満である。
KOIKI baselineのSpring Boot 4.1.1を明示的に含む一次情報を確認できない。

したがって、互換性を推測して導入せず、次のSpring標準＋KOIKI内部実装へ限定する。

- `HX-Request`等の標準HTTP headerでfull / fragmentを判定する。
- redirectは標準3xxまたは`HX-Redirect` / `HX-Location` response headerを使う。
- CSRF token / header名はThymeleafでmetaへmaterializeし、外部scriptの`htmx:configRequest`で注入する。
- OOB、push URL、loading、debounce、再初期化はHTMX標準attribute / eventで表現する。
- 独自annotation、argument resolverまたは第三者Thymeleaf dialectを初期実装へ追加しない。

これはADR-027の「HTMX同梱」を維持し、同ADRに記録済みの不適合時fallbackを発動する判断である。
承認後にADR Registerとグランドデザイン§13.4の実採用記述を実装事実へ合わせて更新する。

### P3-B0-D5 — asset distribution, selective adoption and JavaScript boundary

**Recommendation: APPROVE**

- HTMXは安定系列`2.0.10`をMaven CentralのWebJarからclasspath配布し、CDNをproduction前提にしない。
- 2026年8月29日公開の4.0.0は直近majorであり、Phase 3 baselineには採用しない。
- checksum、license（0BSD）、SBOM / dependency inventoryを通常のMaven dependencyとして追跡する。
- TemplateがWebJar内部構造を直接散在参照しないよう、Starter所有の共通layout / resource contractから
  一箇所で読み込む。
- asset読込はlink / FormをHTMX化しない。`hx-*`属性と対応するserver-side fragmentを明示した操作だけを
  HTMX対象とし、globalなlink / submit interceptionを実装しない。
- Phase 3で必須採用する業務箇所は、効果が上位設計とDoD 3-5に明記されたmaster一覧の検索・paging・
  部分更新とする。他の画面は§4.1の条件を満たす場合だけ採用し、契約実証の数合わせでHTMX化しない。
- inline scriptを前提にせず、KOIKI所有の外部integration scriptを使用してCSP nonce方針と衝突させない。
- HTMXを採用したinteractionはJavaScript有効を前提とし、グランドデザイン§13.4どおり同じ操作の
  JavaScript無効用full-form fallbackを別経路として重複実装しない。通常のMVC / Thymeleaf画面まで
  不要にJavaScript依存へ変える趣旨ではない。
- JavaScript必須であっても、semantic HTML、keyboard操作、focus移動、error関連付け、loading通知等の
  accessibilityは免除しない。

## 6. MVC / Thymeleaf / HTMX execution boundary

### 6.1 Controller and DTO

| Boundary | Contract |
|---|---|
| Controller | HTTP parameter、Form binding、validation、full / fragment選択、status / header / view名だけを扱う |
| Application Use Case | Permission、所有権、scope、状態、不変条件、transaction、Auditの正本を維持する |
| Form DTO | inbound専用。trim、format、field validationを表すがDomain Entityではない |
| View DTO / read model | Template向けにmaterializeし、Entity / Domain Model / Repositoryを公開しない |
| Thymeleaf | 表示と単純な分岐だけ。query、認可判断、業務計算を行わない |
| Persistence | P3-B1のquery時点scopeとP3-A1〜A4のcommand modelを維持する |

MVCはApplication Use Caseを再利用し、画面向けの別業務実装を作らない。表示制御でbuttonを隠すことは
補助的UXに過ぎず、server-side認可を置換しない。最初に通常のThymeleaf full-page responseを成立させ、
§4.1の条件を満たす選択箇所だけにfragment responseを追加する。

### 6.2 Full / fragment and error semantics

- HTMXを採用した操作では、通常requestはlayoutを含むfull HTML、`HX-Request: true`は対象fragmentだけを返す。
- 同一Use Case結果をfull / fragmentで分岐させず、分岐はresponse表現だけに限定する。
- validation errorは4xxへ機械的変換せず、入力fragmentとfield / global errorを再描画する。
- not found / scope外は情報を露出しない既存Application failureを保ち、full / partial双方で同じHTTP意味を持つ。
- 認証期限切れ、access denied、CSRF拒否、404、500のfull / partial responseを明示的に検証する。
- unexpected exceptionのmessage、stack trace、credential、token、Cookieまたは個人情報を画面へ出さない。
- POST成功後はPRGまたはHTMX redirectを使い、二重submitをserver-sideの状態 / versionでも拒否できる形にする。

### 6.3 HTMX 11 contracts trace

11契約はHTMXを利用する際のFramework共通契約であり、全画面または各操作が11項目すべてを使うという
意味ではない。Phase 3では、選択した少数のinteractionで契約全体を実証し、不要なOOB、history操作、
動的再初期化等を業務画面へ持ち込まない。

| # | Contract | Planned proof |
|---:|---|---|
| 1 | full / fragment render | MockMvc + browser response DOM |
| 2 | CSRF automatic injection | DOM / request headerと正常・欠落時403 |
| 3 | validation partial | field / global error fragmentとfocus |
| 4 | redirect | `HX-Redirect` / `HX-Location`と最終URL |
| 5 | history | `HX-Push-Url`、back / forward後の表示 |
| 6 | OOB policy | 採用箇所限定と対象外DOM不変 |
| 7 | loading | `aria-busy` / indicatorの開始・終了 |
| 8 | search debounce | 過剰request抑制、query / paging整合 |
| 9 | dynamic reinitialization | swap後のevent / widget再設定 |
| 10 | partial 403 / 404 / 500 | status、fragment、秘密情報非露出 |
| 11 | JavaScript required | fallback非提供の文書化と有効環境でのaccessibility |

## 7. Security / Identity / Audit / Session reuse

### P3-B0-D6 — browser security composition

**Recommendation: APPROVE**

Referenceは既存のlocal Form Login、`FrameworkPrincipal`、Permission、CSRF、Security Header、
Spring Session JDBC、logoutおよびBusiness Auditを再利用する。

- `/master/**`はrequest境界でも`MASTER:ADMIN`を要求する。
- `/expenses/**`は認証を要求し、最終的な`EXPENSE:APPLY / APPROVE / SETTLE`、所有権、部門scope、
  自己承認および状態判定は既存Application Use Caseで実施する。
- static resourceのpermit範囲はStarter資産の読込に必要なpathだけを明示し、catch-all permitを作らない。
- Referenceの明示的`SecurityFilterChain`が業務routeを捕捉し、Phase 2のlowest-precedence default denyへ
  偶然落ちる構成にしない。対象外routeは引き続きdefault denyとする。
- HTMX CSRFはSpring Securityが発行したtokenだけを使い、固定tokenやCSRF無効化を採用しない。
- controller parameterからactor ID、Permissionまたはscopeを信用しない。
- Business Auditは既存Use Case transaction内に留め、画面描画やHTMX swap自体を業務監査にしない。
- browser test用user、credential、Role / Permission seedをproduction migrationやStarterへ追加しない。

## 8. Browser runner and human checkpoint

### P3-B0-D7 — automated runner ownership

**Recommendation: APPROVE PLAYWRIGHT JAVA AS NON-DISTRIBUTED TOOLING**

自動実browser journeyにはPlaywright Java `1.62.0`とChromiumを使用する。複数の独立した
`BrowserContext`を一つのscenarioで扱えるため、P3-B4の2 Session競合をCookie共有なしで再現できる。

| Item | Contract |
|---|---|
| Owner | `build-support`配下の非配布Tooling |
| Formal artifact | `koiki-starter-web-mvc`、`koiki-testing`、Reference JAR、Project Templateへ含めない |
| Dependency | Toolingのtest scopeだけ。KOIKI BOMやCustomer dependencyへ強制しない |
| Browser | Phase 3ではChromium 1 engineをcritical journeyの基準とする |
| Binary install | 明示的なlocal setupで行い、通常のRoot Reactor `clean verify`から暗黙downloadしない |
| Data | 非配布fixtureがFramework Public API / Reference入口から準備し、production seedを使わない |
| Cleanup | BrowserContext、process、container、test dataをrun後に終了・除去する |
| Evidence | source commit、runner / browser version、request / trace ID、sanitized log、Audit、DB、必要時trace / screenshot |
| CI | P3-C2でruntime / flakiness / cleanupを測定し、Remote Gate承認前はrequired checkにしない |

versionはTooling作成時に再確認し、同一Phase中の無目的な追随更新はしない。version変更はbrowser binaryとの
組合せを含むTooling reviewとする。

### P3-B0-D8 — automated and human verification combination

**Recommendation: APPROVE**

P3-B2から、次の順に無理なく積み上げる。

1. Domain / Application / query testで業務規則を高速回帰する。
2. MockMvc / PostgreSQLでForm、View DTO、Security、full HTML / fragmentを再現可能に検証する。
3. agent-driven HTTP操作でpackage済みapplication、log、Audit、DBを突合する。
4. Playwrightでlogin、CRUD、選択したHTMX interaction、CSRF、history、2 Session競合のcritical journeyを自動化する。
5. Architecture Ownerまたは実施者がheaded browserで表示、操作感、keyboard、focus、error、loadingを目視する。

人系checkpointは自動testを置換しない。画像やvideoは補助証拠とし、再現手順、期待結果、実結果および
backend evidenceを記録の正本にする。秘密値をlog、trace、screenshotまたはvideoへ残さない。

## 9. CP order, Gate and commit points

| CP | Allowed work after P3-B0 approval | Commit / verification point |
|---:|---|---|
| P3-B0 | 本contractと承認記録だけ | 文書commit。Maven / browser testはNOT RUN |
| P3-B1 | master JPA projection、expense JdbcClient query、query時点scope | focused test、PostgreSQL、Entity露出0。Starter変更なし |
| P3-B2 | `koiki-starter-web-mvc`新設、BOM / Reactor、Reference MVC / Thymeleaf / Form / View DTO、Security route | focused / Root Reactor、package inventory、最初のheaded browser / log突合 |
| P3-B3 | HTMX asset / integration、選択的採用、11契約、CSRF、Playwright Tooling初期journey | master検索・pagingを中心にMockMvc、実browser、Playwright、partial error / CSRF evidence |
| P3-B4 | 競合画面、承認済みcache / TTL契約 | 2 BrowserContext、先行commit / 後発拒否、DB version、TTL evidence |
| Gate B | B1〜B4のWeb / query横断acceptance | clean Root Reactor、自動browser journey、Owner実演、accessibility、log / Audit / DB突合 |

各CPを個別にcommit可能な状態で閉じ、次CPを混在させない。P3-B4のcache対象 / TTLは別のblocking decisionで
実装前に固定する。workflow、required check、push / PR / merge、snapshot publishまたはremote操作は
Gate Bのlocal acceptanceによっても自動許可されない。

## 10. Verification scope of this review

P3-B0では文書、repository inventoryおよび第三者library一次情報だけを検査した。

| Check | Result |
|---|---|
| Grand DesignのStarter / HTMX同梱 / 11契約 / JS必須とのtrace | PASS |
| Framework / Reference / Tooling / Customer ownership分離 | PASS |
| 現行Maven / dependency / Security / browser inventory | PASS |
| Spring Boot 4.1.1に対する`htmx-spring-boot`明示対応情報 | NOT CONFIRMED — fallback推奨 |
| HTMX 2.0.10 WebJar座標 / license | CONFIRMED |
| Playwright Java、複数BrowserContext、browser install / CI前提 | CONFIRMED |
| Milestone B production / module / dependency先行変更 | 0 |

文書CPであるためMaven、PostgreSQL、MockMvc、実browserおよびPlaywrightは`NOT RUN`とする。これは
実装の受入を意味せず、§9の各production CPで対応Evidenceを必須とする。

## 11. Architecture Owner decisions

| ID | Decision | Recommendation | Status |
|---|---|---|---|
| P3-B0-D1 | `koiki-starter-web-mvc`を正式Framework artifactとし、P3-B2で実責務を伴って新設する | APPROVE | APPROVED（2026年9月13日） |
| P3-B0-D2 | 初期Java Public API 0型、内部auto-configuration、resource contractをcompatibility管理する | APPROVE | APPROVED（2026年9月13日） |
| P3-B0-D3 | MVC / Thymeleaf / Validation / JSpecify / HTMX WebJarだけをStarter dependencyとし、Security profileを強制しない | APPROVE | APPROVED（2026年9月13日） |
| P3-B0-D4 | Boot 4.1.1明示対応を確認できない`htmx-spring-boot`を採用せず、Spring標準＋小さな内部fallbackを使う | APPROVE FALLBACK | APPROVED（2026年9月13日） |
| P3-B0-D5 | Thymeleaf HTMLを主軸とし、HTMX 2.0.10をlocal同梱するが効果が明確な操作だけに採用する。CDN / JS無効fallback / HTMX 4.0.0は採用しない | APPROVE | APPROVED（2026年9月13日） |
| P3-B0-D6 | Phase 2 Security / Identity / Audit / Sessionを再利用し、業務routeとHTMX CSRF境界を明示する | APPROVE | APPROVED（2026年9月13日） |
| P3-B0-D7 | Playwright Java 1.62.0 / Chromiumを非配布Toolingに限定し、CI required化をRemote Gateへ送る | APPROVE | APPROVED（2026年9月13日） |
| P3-B0-D8 | P3-B2から自動test、実browser目視・操作、log / Audit / DB突合を段階的に組み合わせる | APPROVE | APPROVED（2026年9月13日） |

**Decision:** APPROVED — P3-B0 COMPLETE / P3-B1 READY
**Decided by:** Shuichi Kataoka, Architecture Owner
**Decision date:** 2026年9月13日

Architecture OwnerはP3-B0-D1〜D8を一体として承認した。server-side UIはSpring MVC / Thymeleafによる
HTMLを主軸とし、HTMXはmaster一覧の検索・paging・部分更新を必須実証箇所として、効果と検証可能性を
説明できる操作だけに採用する。`htmx-spring-boot`はSpring Boot 4.1.1への明示対応を確認できないため
採用せず、ADR-027に定めたSpring標準＋KOIKI内部fallbackへ切り替える。Playwrightは非配布Toolingに限定する。

この承認によりP3-B0を`COMPLETE`、P3-B1を`READY`とするが、P3-B2以降の先行実装、remote変更、workflow、
required checkまたはsnapshot publishは許可しない。条件変更がある場合は、影響するD項目と
P3-B1〜B4 / Gate Bのtraceを修正して再reviewする。

## 12. Third-party primary sources

- htmx official documentation: `https://htmx.org/docs/`
- htmx official changelog / package metadata: `https://github.com/bigskysoftware/htmx/blob/master/CHANGELOG.md`、`https://github.com/bigskysoftware/htmx/blob/master/package.json`
- htmx official releases: `https://github.com/bigskysoftware/htmx/releases`
- htmx 2.0.10 Maven Central: `https://central.sonatype.com/artifact/org.webjars.npm/htmx.org/2.0.10`
- htmx-spring-boot official repository / compatibility table: `https://github.com/wimdeblauwe/htmx-spring-boot`
- Spring Initializr official dependency metadata: `https://github.com/spring-io/start.spring.io/blob/master/start-site/src/main/resources/application.yml`
- Playwright Java official documentation: `https://playwright.dev/java/docs/intro`、`https://playwright.dev/java/docs/api/class-browsercontext`、`https://playwright.dev/java/docs/browsers`、`https://playwright.dev/java/docs/ci`
- Playwright Java 1.62.0 Maven Central: `https://central.sonatype.com/artifact/com.microsoft.playwright/playwright/1.62.0`
