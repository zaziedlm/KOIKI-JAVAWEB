# Phase 3 P3-B4 optimistic conflict / cache contract review

## 1. Status and boundary

| Item | Result |
|---|---|
| Date | 2026年9月14日 |
| Architecture Owner | Shuichi Kataoka |
| Phase / CP | Phase 3 Reference Vertical Slice / P3-B4 |
| Status | CONTRACT COMPLETE / OWNER APPROVED / IMPLEMENTATION READY |
| Branch / review start HEAD | `feature/phase3-reference-vertical-slice` / `98417ee3259dc3f44c4bc760b4140571a7cd24f3` |
| Primary ownership | Reference expense、Reference-owned cache configuration、非配布Browser Tooling |
| Production implementation | 未開始 |

P3-B4は、AC-P3-08の2 Session楽観lock競合画面と、Phase 3 DoD 3-9の経費科目コード値cacheを
実装する前のblocking reviewである。本reviewではproduction code、Public API、Maven module、migration、
dependency、workflow、remote設定およびbrowser fixtureを変更しない。

## 2. Authoritative inputs

- `AGENTS.md`
- `docs/architecture/grand-design/KOIKI-JavaWeb-FW_グランドデザイン_v0.2.md` §12.5、§13.2、§16.4、§26、Phase 3 DoD 3-6 / 3-9
- `docs/development/KOIKI-JavaWeb-FW_Phase3実行計画_v0.1.md`
- `docs/reference/KOIKI-JavaWeb-FW_Reference_Application_Specification_v0.1.md` §9、§10、§13、AC-P3-08
- `docs/architecture/validation/phase3-p3-b0-mvc-htmx-contract-review.md`
- `docs/architecture/validation/phase3-p3-b1-read-model.md`
- `docs/architecture/validation/phase3-p3-b2-mvc-thymeleaf.md`
- `docs/architecture/validation/phase3-p3-b3-htmx.md`
- `docs/agent/skills/koiki-project-overview/SKILL.md`
- `docs/agent/skills/koiki-business-feature-work/SKILL.md`

第三者機能はSpring Boot Cache、Spring Framework CacheおよびCaffeineの公式一次情報で確認した。参照先は§11に示す。

## 3. Current implementation inventory

### 3.1 Optimistic conflict

- `ExpenseRequest`はJPA兼用Tier 2 modelであり、`@Version`を保持する。
- expenseの編集・提出・承認・却下・差戻し・再編集開始・精算Formは`expectedVersion`を往復する。
- `ExpenseApplicationService`は取得済みversionとForm versionを比較し、JPA flush時の
  `OptimisticLockingFailureException`も`ExpenseFailure.CONCURRENT_MODIFICATION`へ変換する。
- `ExpenseWebExceptionHandler`は競合をHTTP 409とsafe messageへ変換するが、現状は汎用`expense/error`画面であり、
  最新状態へ戻る専用導線と2 Browser Session Evidenceはない。
- PostgreSQL testはversion増分とstale version拒否を確認済みだが、AC-P3-08の実画面journeyは未実施である。

### 3.2 Cache

- 現行RepositoryにはSpring Cache有効化、Caffeine dependency、cache property、`@Cacheable`がない。
- expense Formの経費科目optionは、P3-B1で承認した表示専用read-only JOINとして
  `ExpenseRequestQuery.findAvailableExpenseCategories()`がactiveなID / code / nameをJdbcClientで返す。
- command時の有効性は別経路の`MasterAvailabilityPort`から非cacheのcurrent-value queryを呼ぶ。
- 個別認可判断、Role / Permission、ユーザー所属、承認scopeおよびexpense transactionはcacheしていない。

## 4. Optimistic conflict contract

### 4.1 Representative journey

AC-P3-08の代表操作は、同じ承認者アカウントがCookieを共有しない二つのBrowserContextで、別ユーザー所有の
同じ`SUBMITTED`申請詳細を同一versionで開くjourneyとする。

1. Context A / Bが同じ承認詳細と同じ`expectedVersion=N`を取得する。
2. Context Aが承認し、`APPROVED`、`version=N+1`、`APPROVE_EXPENSE / SUCCESS`を成立させる。
3. Context Bがstaleなversion Nで却下を試みる。
4. 後発操作はHTTP 409の専用競合画面となり、state、reason、versionおよびBusiness Auditを変更しない。
5. 利用者は競合画面の明示linkから同じscopeの詳細を再取得し、最新の`APPROVED`を確認できる。

同一actorの二つの独立Sessionを使うことで、競合制御そのものと「別actor固有の認可差」を混在させない。
fixtureでは対象申請のapplicantを認証actorと分け、自己承認拒否が競合実証を遮らないようにする。

### 4.2 UI / error boundary

- `CONCURRENT_MODIFICATION`だけをReference-owned `expense/conflict`へ変換し、HTTP 409を維持する。
- 競合画面は「他の操作で更新された」「上書きしていない」「最新状態を確認する」を説明する。
- POSTの自動再送、暗黙retry、stale Formの再適用または成功Flash Messageを行わない。
- 元の操作scopeに対応する安全な詳細linkを返す。例外class、SQL、stack trace、内部actor、Session IDは表示しない。
- 他の400 / 404 / 409業務競合 / 503は既存sanitized error境界を維持する。
- P3-B4の競合画面をFramework共通Template、Java Public APIまたはStarter契約へ昇格させない。

### 4.3 Transaction / Audit

- hidden `expectedVersion`、Applicationのversion比較、JPA `@Version` / flushの三層を維持する。
- 先行transactionだけがstate、versionおよび成功Business Auditをcommitする。
- 後発競合ではDomain mutation、event、Business Auditおよび他rowへの副作用を0とする。
- stale versionの順次操作に加え、実PostgreSQLでJPA `@Version`による競合検出経路もfocused testする。

## 5. Cache contract

### 5.1 Target and exclusions

cache対象は、経費申請Formが表示するactiveな経費科目optionの不変record一覧だけとする。

| Data | Cache |
|---|---|
| active expense category ID / code / name option list | YES |
| 部門option、master管理一覧・検索結果 | NO |
| expense一覧・詳細・状態・version・金額 | NO |
| current-value master availability | NO |
| ユーザー所属、承認scope、個別認可判断 | NO |
| Role / Permission、Session、Audit | NO |

source tableと更新責務はmaster所有のまま、cacheはexpenseが所有する表示専用read modelの内部最適化とする。
P3-B1のread-only JOIN例外を更新、認可、不変条件またはcurrent-value判断へ拡張しない。

### 5.2 Placement and identity

- `JdbcExpenseRequestQuery.findAvailableExpenseCategories()`のOutbound Adapter実装に`@Cacheable`を限定する。
- cache名は`koiki:reference:expense-category-options`、固定keyは`active`とする。
- valueは`ExpenseSelectionOption`の変更不能な要素と`List.copyOf`による変更不能な一覧とする。
- 同一instance / 同一keyの同時初期loadは`sync=true`で集約するが、instance間lockとは扱わない。
- 空一覧はcacheする。loader例外はcacheせず、safeな既存error境界へ伝播する。

### 5.3 Provider, dependency and configuration

- Spring Cache抽象とCaffeineを使用する。Redis、JCache、独自cache SPIは追加しない。
- dependencyは非配布`koiki-reference-app`だけへ`spring-boot-starter-cache`と
  `com.github.ben-manes.caffeine:caffeine`を追加し、versionは承認済みSpring Boot BOMへ委ねる。
- `expense.configuration`で`@EnableCaching`を有効化し、Boot auto-configurationの`CaffeineCacheManager`を使う。
- Framework Starter、BOM、Root Reactor module、Public APIおよびmigrationを変更しない。
- 新しいKOIKI独自propertyを作らず、Spring標準の`spring.cache.type`、`spring.cache.cache-names`、
  `spring.cache.caffeine.spec`をReference applicationで設定する。

### 5.4 TTL and staleness

Referenceの既定値は`maximumSize=1,expireAfterWrite=30s`とする。`expireAfterAccess`は頻繁な参照で
staleness上限が延長されるため採用しない。期限到来時のbackground refresh、refresh-aheadおよびschedulerを追加せず、
TTL後の最初の参照が同期的にDBから再loadする。

- 各instanceは独立したCaffeine cacheを持ち、他instanceのmaster更新を即時には認識しない。
- 各entryの許容stalenessは、そのinstanceでloadしてから最大30秒とする。
- create / rename / deactivateで`@CacheEvict`またはevent駆動無効化を行わない。TTLだけを鮮度境界とする。
- test / explicit local verificationでは`SPRING_CACHE_CAFFEINE_SPEC`により短いTTLへ上書きできるが、
  production codeへtest用property、clock、routeまたはfailure switchを追加しない。

cacheに残った廃止済み経費科目がFormへ一時表示されることは許容する。ただし、submit時の
`MasterAvailabilityPort`は非cacheであり、廃止済み選択を`MASTER_UNAVAILABLE`として拒否し、expense rowを作らない。
このfail-safe境界をP3-B4の必須testとする。

## 6. Implementation sequence and commit point

1. P3-B4 contract reviewをOwner承認し、文書commit pointを閉じる。
2. 専用競合画面と409 mappingを実装し、MockMvc / PostgreSQL競合testを追加する。
3. Reference-only dependency、cache設定、option-list cacheとTTL focused testを追加する。
4. disposable fixtureを自己承認にならない最小構成へ調整し、Playwrightを2 BrowserContextへ拡張する。
5. cache stale / TTL reload、stale category command拒否、2 Session競合を実アプリで検証する。
6. Root Reactor `clean verify`、package inventory、log / Audit / DB突合、Owner目視を完了する。
7. P3-B4 close review後に実装一式をcommitし、Gate Bへ進む。

## 7. Verification plan

| Layer | Check |
|---|---|
| Domain / Application | version一致、stale version拒否、先行だけstate遷移、後発副作用0 |
| PostgreSQL | JPA `@Version`競合、DB state / version、Business Audit 1件、後発Audit 0 |
| Cache focused | 初回load、TTL前hit、TTL後reload、空一覧、loader failure非cache、変更不能value |
| Cache safety | 廃止科目のstale表示を許容しつつ、command current-value queryが更新を拒否 |
| MVC | 409専用画面、safe message、latest detail link、他error mapping回帰 |
| Security / Session | 2 BrowserContextのCookie非共有、同じPermission、CSRF、default deny維持 |
| Browser | A承認成功、B stale却下409、latest state再取得、cache TTL前後のoption表示 |
| Log / Audit / DB | parameter masking、例外detail非露出、先行commit、後発rollback、query再load時点 |
| Root / package | Root `clean verify`、Reference  executable JAR、Framework配布unit不変 |

人系checkpointでは、Ownerが二つのChrome context相当で先行成功と後発競合画面を目視し、
必要に応じてheaded PlaywrightでcacheのTTL前後を確認する。cache hitは画面だけで断定せず、DB変更、SQL log、
表示値およびTTL後の再loadを組み合わせる。通常HTMLを主軸とし、競合操作をHTMX化しない。

## 8. Gate / acceptance

P3-B4 closeには次をすべて必要とする。

1. AC-P3-08の2 Session browser journeyが先行成功 / 後発409を再現する。
2. 後発競合がstate、version、reason、event、Business Auditを変更しない。
3. 経費科目optionだけがcacheされ、TTL前はstale、TTL後の最初の参照で再loadされる。
4. staleな廃止科目をcommandが非cache current-value queryで拒否する。
5. 認可、Session、transaction data、master管理queryがcacheされていない。
6. Root、実PostgreSQL、package済みJAR、browser、log / Audit / DB EvidenceがPASSする。
7. Architecture Ownerが競合UX、30秒staleness、dependency / property境界を承認する。

## 9. Deferred scope

- P3-C0以降のREST 409 / ETag / If-Match / Problem Details。
- Gate BのP3-B1〜B4横断acceptanceとremote Gate。
- Framework共通競合画面、cache Starter、cache Public APIまたは独自property。
- Redis、分散cache、event-driven invalidation、refresh-ahead、warm-up、preload。
- Role / Permission cache、個別認可結果cache、Session cache、expense transaction cache。
- Spring Modulith Level 2 / 3、外部brokerおよびcache event externalization。
- cache metrics / alertの正式SLO、Customer固有TTL、複数region整合性。
- P3-C3 MyBatis楽観lock規約およびPhase 4 accounting競合。

## 10. Architecture Owner decisions

| ID | Decision | Recommendation | Status |
|---|---|---|---|
| P3-B4-D1 | 同一approverの独立2 BrowserContextと別applicantの`SUBMITTED`申請をAC-P3-08代表journeyとする | APPROVE | APPROVED（2026年9月14日） |
| P3-B4-D2 | 後発をHTTP 409のReference専用競合画面へ送り、最新scope detailへのlinkを示し、自動retryしない | APPROVE | APPROVED（2026年9月14日） |
| P3-B4-D3 | hidden version、Application比較、JPA `@Version`を維持し、先行だけstate / Auditをcommitする | APPROVE | APPROVED（2026年9月14日） |
| P3-B4-D4 | cache対象をactive経費科目optionのID / code / name一覧だけに限定する | APPROVE | APPROVED（2026年9月14日） |
| P3-B4-D5 | cacheをexpense表示専用JdbcClient Adapter内部へ置き、名前空間、固定key、変更不能value、`sync=true`を使う | APPROVE | APPROVED（2026年9月14日） |
| P3-B4-D6 | ReferenceだけにBoot Cache / Caffeine dependencyを追加し、Framework / BOM / Public APIを変更しない | APPROVE | APPROVED（2026年9月14日） |
| P3-B4-D7 | `maximumSize=1,expireAfterWrite=30s`、TTL後の次回同期load、event evictなしを採用する | APPROVE | APPROVED（2026年9月14日） |
| P3-B4-D8 | stale表示を許容する一方、commandのmaster current-value確認は非cacheでfail-safeに拒否する | APPROVE | APPROVED（2026年9月14日） |
| P3-B4-D9 | PostgreSQL、2 BrowserContext、headed Owner目視、log / Audit / DB突合を組み合わせる | APPROVE | APPROVED（2026年9月14日） |
| P3-B4-D10 | REST、distributed cache、Framework昇格、MyBatis競合およびremote変更をdeferする | APPROVE | APPROVED（2026年9月14日） |

**Decision:** APPROVED — P3-B4 CONTRACT COMPLETE / IMPLEMENTATION READY  
**Decided by:** Shuichi Kataoka, Architecture Owner  
**Decision date:** 2026年9月14日

Architecture OwnerはP3-B4-D1〜D10を一体として承認した。2 BrowserContextの楽観lock競合、
Reference専用409画面、先行transactionだけのstate / Audit確定、active経費科目option一覧だけのcache、
Reference限定のSpring Cache / Caffeine dependency、30秒のwrite TTL、非cache current-value確認による
fail-safe、実PostgreSQL・browser・log / Audit / DBの複合検証およびdeferred scopeを実装契約とする。

この承認によりP3-B4のblocking contract reviewを閉じ、文書commit後に専用競合画面の実装から開始できる。
P3-B4の実装・検証・close reviewは未完了であり、Gate B、P3-C0以降、workflow、required check、
remote push / PR / mergeまたはsnapshot publishを先行しない。

## 11. Third-party primary sources

- Spring Boot Caching: `https://docs.spring.io/spring-boot/reference/io/caching.html`
- Spring Framework `@Cacheable`: `https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/cache/annotation/Cacheable.html`
- Caffeine Specification: `https://github.com/ben-manes/caffeine/wiki/Specification`
- Caffeine Eviction: `https://github.com/ben-manes/caffeine/wiki/Eviction`
