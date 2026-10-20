# P4-B1 Session SPA＋MVC：既存との差分と設計記録

作成日：2026-10-20

状態：**I-01／B1-01の調査済み、B1-02〜03の具体仕様案を整理済み。R-00のtest1 file変更と直接回帰7件が完了／PASS**。R-01はAdvice修正と限定回帰を実施（§7.5）。R-02はCookie設定反映と限定回帰を実施（§7.6）。R-03・B1機能の実装／実演・DoD受入は未実施。§5〜6は実装に渡す設計上の選択であり、Owner受入・互換性実証を記録するものではない。

Ownership：設計記録はFramework側。機能はReference `expense`／`identity`、Framework変更はR-01、検証harnessはToolingが所有する。

## 1. 目的と今回の到達点

[統合作業計画](phase4-review-integrated-task-plan-20261020.md)のI-01／B1-01を進め、B1-02〜03とR-00〜03へ渡す材料を本書にまとめる。以後も同じ記録を更新し、同じ内容の開始票を増やさない。

**既存の申請Use Caseを再利用し、自分の申請一覧・詳細、draft作成、submitを最小React UIから利用する案を採る。** 現行Bearer RESTをCookie認証へ開放するのではなく、別のSession API経路を設ける。部門・費目の選択肢も既存のscope付きreadをJSONへ変換する。承認・会計・通知復旧のSPA化は今回の範囲に加えない。

根拠は[Grand Design §13.5〜13.6・§27.8](../architecture/grand-design/KOIKI-JavaWeb-FW_グランドデザイン_v0.2.md)、[見直し草案R4の承認](KOIKI-JavaWeb-FW_Phase4実施計画_見直し草案_v0.1.md)、[他package分解§2.1](phase4-other-packages-breakdown-draft-20261005.md)、[レビューF1〜F5](../architecture/validation/main-framework-design-implementation-review-20261020.md)。2026-09-26のSession SPAでDoDを維持する判断は引き継ぐ。

| DoD | 今回設計する成立条件 | 現在の不足 |
| --- | --- | --- |
| 4-8：SPAからCookie Session認証でAPIを利用し、CSRF double-submitが機能する | login後の参照・更新、CSRF Cookieとheaderの照合、欠落／不正拒否、失効・logout後の拒否をpackage済みReferenceで観測 | Session API、SPA、Cookie方式のCSRF接続とbrowser実演がない |
| 4-9：同一アプリ内でThymeleafとSPAが併存し、CSRF設定を経路ごとに分離する | MVC hidden field／HTMX headerを維持し、SPA側だけCookie方式を使用。同じ申請のDB・Audit結果を照合 | 現行はMVCとBearer RESTの併存。これだけではSession SPAのDoDを満たさない |

本書のpath・画面・方式は調査を踏まえた基準案であり、新規実装や各案のOwner受入を記録するものではない。F1／F2／F3の是正とF5の検証対象整理をB1実装前に合流する順序を維持する。

## 2. I-01：sourceと検証入口

### 2.1 確認したsource

- 作業branch：`docs/phase4-review-integrated-plan-20261020`。
- 開始時HEAD：`047b45780ae1352970822f419c9cff0b6ad77dfa`。親は `521572d195a5a854c57ef58b2b61c59ce5c5a0b8`。開始時working treeはclean。
- 指定branchをoriginからfetchし、同HEADへ切替済み。親との差分はレビュー記録・probe2件・README・残タスク棚卸し・統合作業計画の6ファイルだけ。production／test／POMは親から変更されていない。
- 今回の読取対象は同HEADのsource。既存レビューの再現結果は入力として参照し、今回の再実行結果にはしない。remote main・tag・CIの最新状態は未照合。
- 適用指示はルート `AGENTS.md`、[project overview](../agent/skills/koiki-project-overview/SKILL.md)、[business feature work](../agent/skills/koiki-business-feature-work/SKILL.md)。Repositoryに `openspec/` はない。副Agentは使用していない。

同日の継続具体化もHEADは`047b457`。本書と導線3件の未commit差分を保持して追記した。既存JAR内のSpring Security `7.1.1`／Session `4.1.1`を読取り、同versionの公式source、npm registryのversion・engine情報、Maven Centralのplugin POMを照合した。取得した外部sourceは`/tmp/koiki-b1-design-sources/`に置き、Repositoryへ複製していない。compiled JARの生成元や製品動作の検証はしていない。

### 2.2 Codex Cloudの実行環境：有効化後の再点検による訂正

前回は有効化前のPATHと通常cacheだけを見ており、保存済み構成を見落としていた。環境そのものが未整備という解釈を訂正する。後続の[Cloud環境点検記録](../architecture/validation/phase4-r00-cloud-environment-preflight-20261020.md)で実行確認した。

| 項目 | 2026-10-20の有効化後の結果 | 次の実行での扱い |
| --- | --- | --- |
| Java／Maven | 保存JDKのjava／javac `21.0.12.1`、Wrapper Maven `3.9.16`。offline validate・13 projectのclean test-compile成功 | 各shellで`source /workspace/cloud-setup/activate.sh`。JDK再install不要 |
| frontend | Node `24.19.0`、npm `11.9.0`。B1 package／lockはまだ作成していない | 選定値は§6.1。実装時にlock生成・組合せbuildを確認 |
| Tooling | PowerShell `7.5.4`起動。Docker Engine `28.4.0`・保存imageでversion確認成功。Playwright Java `1.62.0`で実Chromium起動・ページ照合・終了成功 | Docker／browserはsandbox制約があるため必要な権限付き実行を使う。再導入やdaemon再起動は不要 |
| Maven cache | `/workspace/.cache/maven/repository`を使用。保存settings／toolchainsが有効。cache BOMはsourceとbyte一致 | 今回のcompile・規約回帰7件は新規取得なしのofflineで成立。別Toolingは選択範囲で照合 |
| 出力・過去証拠 | 保存された2026-10-03のroot170件・E2E1件PASSを確認。今回はfresh outputで規約負例5件＋Reference Architecture2件PASS | 過去結果と現sourceの選択結果を分ける。clean test-compileで旧Reference JARは消去済み。実JAR検証前には新packageが必要 |

保存構成のactivateはshell内の既存ツール・proxy・cacheを有効化する操作であり、platformの環境設定を変更していない。環境点検時はソース・依存の変更、再install、root全件／実DB／E2Eの再実行なし。その後のR-00のtest変更・限定結果は§7.4に記録した。

### 2.3 検証入口と通常／専用の区別

| 入口 | 用途・今回確認した構成 | B1での扱い |
| --- | --- | --- |
| [Root POM](../../pom.xml)、[Parent](../../koiki-parent/pom.xml)、[Reference POM](../../koiki-reference-app/pom.xml) | Java 21 compile、Boot 4.1.1、通常root reactor内にReference | 局所検証後、合流時に通常`clean verify`を1回予定。現在の実行件数は未測定 |
| [Reference test](../../koiki-reference-app/src/test/java/org/koikifw/reference/) | MVC／API／method security／PostgreSQL／architecture | §7の影響別集合を選定。F5整理後のclean出力で比較 |
| [runtime-foundation-verification](../../build-support/runtime-foundation-verification/pom.xml) | Root外のAPI Starter／Problem Details test | R-01のFramework合成test・既存Problem Details回帰の候補。依存artifact準備は別確認 |
| [security-foundation-verification](../../build-support/security-foundation-verification/pom.xml) | Root外のSession構成／logout等のtest | R-02のBoot構成とlogout Bean生成確認の候補 |
| [reference-e2e-verification](../../build-support/reference-e2e-verification/README.md) | Root外。Playwright Java `1.62.0`、隔離PostgreSQL 17、package済みJARの子process起動 | 既存Critical Journeyを直接回帰に選び、B1用browser実演を追加する候補 |
| Referenceの`s1-reference-async` profile、専用testのenable property | 通常buildから分離した非同期runtime／有限検証 | 今回有効化しない。変更で直接影響する場合に該当回帰だけ選び直す。旧S1予算・runnerの再利用はしない |

通常／専用は実行時のprofile・property・新しいXMLの実行／skipで区別する。ReviewProbeは問題挙動を期待する限定再現用であり、修正後の受入testには使わない。

## 3. B1-01：既存コード・testとの対応表

以下の「不足」は読取時点のB1との差分である。既存sourceがあることと、今回動作確認済みであることを分ける。

| 要素 | 存在する実装・test | 再利用するもの | 変更／不足 |
| --- | --- | --- | --- |
| login／MVC Session | [ReferenceSecurityConfiguration](../../koiki-reference-app/src/main/java/org/koikifw/reference/identity/configuration/ReferenceSecurityConfiguration.java)、[ReferenceBusinessUrlSecurityTest](../../koiki-reference-app/src/test/java/org/koikifw/reference/web/ReferenceBusinessUrlSecurityTest.java) | `/login`のlocal form login、既存Identity、MVC／HTMXのCSRFと拒否処理 | SPA入口の追加。APIにはHTML loginを返さない。OIDC SSOの新設は含めない |
| 永続Session／logout | [Session JDBC構成](../../koiki-starters/koiki-starter-session-jdbc/src/main/java/org/koikifw/session/internal/KoikiSessionJdbcAutoConfiguration.java)、[logout handler](../../koiki-starters/koiki-starter-session-jdbc/src/main/java/org/koikifw/session/internal/KoikiSessionLogoutHandler.java) | 同じSession、失効、Cookie消去、Framework logout customizer | R-02でCookie設定反映とcustomizer生成順を確認。SPAのCSRF Cookie処置はReferenceで接続 |
| 既存Bearer REST | [ReferenceApiSecurityConfiguration](../../koiki-reference-app/src/main/java/org/koikifw/reference/identity/configuration/ReferenceApiSecurityConfiguration.java)、[ExpenseApiController](../../koiki-reference-app/src/main/java/org/koikifw/reference/expense/adapter/inbound/api/ExpenseApiController.java) | DTOの公開項目・validation・HTTP意味を設計入力に利用 | `/api/**`は最優先・stateless・CSRF無効のBearer専用。Sessionだけでは利用できない。別Session APIが必要 |
| 自分の申請一覧・詳細 | [ExpenseReadService](../../koiki-reference-app/src/main/java/org/koikifw/reference/expense/application/ExpenseReadService.java)、[JdbcExpenseRequestQuery](../../koiki-reference-app/src/main/java/org/koikifw/reference/expense/adapter/outbound/persistence/JdbcExpenseRequestQuery.java)、[read model DB test](../../koiki-reference-app/src/test/java/org/koikifw/reference/expense/ExpenseReadModelPostgreSqlIntegrationTest.java) | `findOwnRequests`／`findOwnRequest`、`EXPENSE:APPLY`、SQL内の申請者scope | 一覧のJSON経路がない。Application recordには申請者email等もあるため直接serializeせずSPA DTOへ絞る |
| 部門・費目の選択 | 同ReadServiceの`findAvailableDepartments`／`findAvailableExpenseCategories` | 所属部門の有効候補、費目候補、command側での現在値再確認 | JSON取得経路がない。UUIDの固定入力やmaster管理APIの開放で代用しない |
| draft作成・submit | [ExpenseApplicationService](../../koiki-reference-app/src/main/java/org/koikifw/reference/expense/application/ExpenseApplicationService.java)、[業務DB test](../../koiki-reference-app/src/test/java/org/koikifw/reference/expense/ExpensePostgreSqlIntegrationTest.java)、[Audit rollback test](../../koiki-reference-app/src/test/java/org/koikifw/reference/expense/ExpenseAuditRollbackPostgreSqlIntegrationTest.java) | `createDraft`／`submit`、Tier 2 model、transaction、`SUBMIT_EXPENSE` Audit | transport用Controller／DTO／mapperを追加。R-03でversionより先に対象認可する |
| MVCとの併用 | [ExpenseWebController](../../koiki-reference-app/src/main/java/org/koikifw/reference/expense/adapter/inbound/web/ExpenseWebController.java)、[templates](../../koiki-reference-app/src/main/resources/templates/expense/) | 一覧・新規作成・詳細・提出・競合画面。SPAと同じUse Case | MVC Form／View DTOをJSONへ流用しない。同じ申請をMVCで再取得して照合 |
| API error | [ExpenseApiExceptionHandler](../../koiki-reference-app/src/main/java/org/koikifw/reference/expense/adapter/inbound/api/ExpenseApiExceptionHandler.java)、[API test](../../koiki-reference-app/src/test/java/org/koikifw/reference/expense/adapter/inbound/api/ExpenseApiControllerTest.java) | 400／404／409／422／503、Problem Detailsの既存意味 | Adviceの対象は既存API Controller限定。新Controllerへの適用が必要。F1の汎用500問題はFramework側で是正 |
| API version | [ExpenseApiVersionConfiguration](../../koiki-reference-app/src/main/java/org/koikifw/reference/expense/configuration/ExpenseApiVersionConfiguration.java)、[application.properties](../../koiki-reference-app/src/main/resources/application.properties) | `/api/`限定resolver、`required=false`のReference回避策 | Session APIの別prefixでversionをどう表すかを§5で分離。Framework既定値変更は今回の必須差分にしない |
| browser統合実演 | [PackagedReferenceCriticalJourneyTest](../../build-support/reference-e2e-verification/src/test/java/org/koikifw/buildsupport/referencee2e/PackagedReferenceCriticalJourneyTest.java) | JAR起動、隔離DB、実Chromium、DB／Audit／log突合、cleanupの方式 | 現行はBearerで作成・提出→MVCで承認。SPA double-submitの証拠にはならない。新規B1実演が必要 |
| architecture | [ReferenceArchitectureTest](../../koiki-reference-app/src/test/java/org/koikifw/reference/ReferenceArchitectureTest.java)、[規約負例](../../koiki-archunit-rules/src/test/java/org/koikifw/archunit/RequiredNegativeArchitectureRulesTest.java) | module境界・Framework Identity境界・負例検出 | F5：両importでtest除外が未指定。clean compile後のproduction対象検査をR-00で整理 |
| frontend／配布 | [Reference POM](../../koiki-reference-app/pom.xml)に既存Web／API／Session依存、Boot repackage | 単一Reference JAR、既存Framework Starter | React source・依存lock・build・asset同梱・SPA入口は新規。専用SPA Starterは作らない |

既存APIの`CreateExpenseRequest`／`SubmitExpenseRequest`等はpackage-privateで`adapter.inbound.api`に属する。別packageのSession Controllerからそのまま参照できない。MVC Formも共有せず、Session専用DTO／mapperを同じ`expense` module内に置く案とする。Application／Domain／Repositoryの複製や公開範囲拡大は不要。

## 4. 代表操作と業務上の出口

### 4.1 最小の画面・操作案

| 画面・操作 | 呼ぶ既存Use Case | 成功時の観測 | 拒否・回復 |
| --- | --- | --- | --- |
| 自分の申請一覧→詳細 | `findOwnRequests(0,20)`等／`findOwnRequest` | 自分の申請だけを表示。詳細のstatus／versionを取得 | 権限不足403、別user／不存在404。ページsizeは既存上限100以内 |
| 新規draft作成 | 候補read→`createDraft` | 1明細の最小formで作成し、返されたIDを詳細で表示。DBのDRAFTと明細・金額を確認 | 形式400、業務制約／無効master422。候補は表示用途でありcommandの再確認を省略しない |
| draft提出 | `submit(id, expectedVersion)` | 詳細で取得したversionを送信。SUBMITTEDへの遷移と`SUBMIT_EXPENSE`成功Auditを同じDBで照合 | 同一userの古いversion409、scope外404、権限403。自動再提出せず最新状態を再取得 |
| MVCとの突合 | 同じIDの`/expenses/{id}` | SPAから提出した同一申請の状態・金額・versionをMVCで確認。別fixtureではMVC作成→SPA参照も確認 | 経路によるHTTP表現差と業務結果差を区別 |
| logout・失効 | 既存Session基盤＋SPA接続 | logout後のSession API拒否。取得済み表示・入力を破棄しloginへの導線を示す | POSTを再認証後に自動再送しない |

draft作成自体には現行serviceのBusiness Audit記録がなく、submitで記録される。作成にもAuditがあると仮定せず、DB作成とsubmit Auditを分けて観測する。選定操作は承認通知eventを発行しない。

二重クリック中は送信を抑止する。create応答不達は「未作成」と断定せず一覧照合へ戻し、自動retryしない。これは冪等作成APIの新設や重複防止保証ではなく、最小UIの動作案である。

### 4.2 所有・責務と今回の対象外

expenseは既存Tier 2を維持する。Controllerは入力／DTO変換／応答、Applicationは認可・transaction・Domain操作、既存Outbound AdapterはSQL／JPAを所有する。Domain Model・Entity・Application read recordをそのままJSONへ露出しない。新しいmodule間呼出・table・migrationは想定しない。

承認・却下・差戻し・会計操作のSPA化、draft編集UI、Customer BFF、OIDC SSO追加、実provider・復旧・S1全件再開、MyBatis、公開API既定値・CIの変更は本代表操作に含めない。既存approver操作はR-03の共有mutation順序変更の直接回帰には含める。

## 5. B1-02への設計案：経路・CSRF・error

### 5.1 経路とchainの基準案

新規pathは以下を候補とする。既存Bearerの`/api/**`に新Session経路を置かず、同じpathでcredentialをfallbackさせない。

| 経路 | 認証・matcher／order案 | CSRF・未認証／拒否応答 |
| --- | --- | --- |
| 既存`/api/**` | 現行Bearer chain、`HIGHEST_PRECEDENCE`、有効化property維持、stateless | 現行のCSRF無効を維持。Cookie単独401、Bearer権限不足403。Bearer無効時はfallback deny |
| 新`/spa-api/**` | Referenceの新chain、order -10。`SessionCreationPolicy.NEVER`で既存Sessionを読取り、request cache無効、form／Basic／Bearer認証入口なし | Cookie方式CSRFを有効。GETの未認証はJSON 401、権限不足403。Bearer単独は認証に使わない |
| 新`/spa`、`/spa/**` | 既存browser chainのsecurity matcherへ追加。下記assetsを除いてauthenticated | 未認証GETは既存`/login`へ。認証後、shellは定義済み経路だけで配信し、未知pathはMVCの404へ渡す。SPA APIの401をlogin HTMLへ変換しない |
| 新`/spa/assets/**` | 同じbrowser chain内で、上記authenticated指定より先にpermitAllを指定 | 非機密build assetだけを配信し、存在しないassetは404。業務値やtokenを埋め込まない |
| 既存MVC／HTMX／`/login`／`/logout` | 現行browser chain、order 0 | HttpSession側のCSRF、hidden field／HTMX headerを維持。form login、既存HTML拒否／遷移を保護 |
| その他 | FrameworkのLOWEST_PRECEDENCE fallback | 現行denyAllを維持。未定義の新APIもControllerへ到達・更新しない |

`/spa-api/**`はSession credential専用とし、Authorization headerを解析するResource Server設定を追加しない。SessionとBearerが両方ある場合もSession主体だけで認可し、Bearer-onlyのprincipalを持ち込まない。

### 5.2 JSON操作のpath案

`GET /spa-api/csrf`はauthenticated、expenseの定義済み操作はURL側とUse Case側の双方で`EXPENSE:APPLY`を要求する。prefix内のその他のrequestはauthenticatedとしてmapping不成立404へ渡す。prefix外は§5.1の経路ごとのchainへ渡し、いずれにも該当しない経路は既存fallback denyを維持する。logoutは§5.4のfilter処理で完結する。

| method／path候補 | 内容 | 応答案 |
| --- | --- | --- |
| `GET /spa-api/csrf` | 認証後のCSRF token materialize／Cookie発行 | 204、no-store。tokenをlogへ出さない |
| `GET /spa-api/v1/expense-options` | 所属部門・有効費目の選択肢 | 200、ID／code／nameのみ |
| `GET /spa-api/v1/expense-requests?page=0&size=20` | 自分の申請一覧 | 200、申請ID・部門・金額・status・version・更新時刻・paging。emailを除く |
| `GET /spa-api/v1/expense-requests/{id}` | 自分の詳細 | 200。既存APIの申請者識別情報を含めない項目を基準にする |
| `POST /spa-api/v1/expense-requests` | draft作成 | 201＋同じSession API prefixのLocationと生成ID |
| `POST /spa-api/v1/expense-requests/{id}/submit` | expectedVersion付き提出 | 204、最新詳細をGET |
| `POST /spa-api/logout` | Framework customizerを使うSession logout | CSRF必須、成功204。Session CookieとSPA CSRF Cookieを消去 |

Session APIは`v1`を明示した固定mapping案とし、既存の`/api/`限定Spring API version resolverを広げない。version annotationは使わず、`v2`・欠落・非canonical pathはmapping不成立とする。これがF4のReference回避策と併存することをroute testで確認する。Frameworkのversion既定値・Public API inventory変更は不要という基準案である。

### 5.3 Cookie／CSRFと認証ライフサイクル

#### 設定するrepositoryとCookie

Spring Security `7.1.1`の標準`CsrfConfigurer.spa()`を使う。これはCookie repositoryと、headerのraw token／request attributeのXOR表現を扱う内部handlerを設定する。ReferenceはCookie属性を指定したrepositoryをBeanとして所有し、**`csrf.spa().csrfTokenRepository(referenceSpaCsrfTokenRepository)`の順**で設定する。逆順だと`spa()`が指定repositoryを置き換える。独自のSPA token request handlerは作らない。

| 項目 | Session Cookie | SPA CSRF Cookie |
| --- | --- | --- |
| name／header | `SESSION`。JSから読まない | `XSRF-TOKEN`／`X-XSRF-TOKEN` |
| HttpOnly | trueを維持 | false。Cookieのraw値をheaderへコピーするため |
| Path／Domain | B1はroot context、Path `/`、Domain未指定 | 同じくPath `/`、Domain未指定。発行・削除で一致させる |
| Secure／SameSite | 既存`server.servlet.session.cookie.*`設定をR-02で実Cookieへ反映 | `setCookieCustomizer`で同じsecure／same-site設定値を反映。既定true／Lax。CSRF用にSessionのHttpOnly・nameをコピーしない |
| 寿命 | Spring Sessionの既存期限・失効に従う | browser session Cookie（Max-Age指定なし）。削除は空値＋Max-Age 0 |

R-02でReferenceの独自CookieSerializer Beanを維持し、Boot `ServerProperties`のCookie属性を反映した。Boot管理へ戻す案ではBeanの登録・条件評価をBoot側へ移す変更も必要になるため、今回は既存logout customizerが参照するReference Beanを維持する最小修正を採用した。BootのSession構成を含む合成testと実JAR起動でlogout Beanの成立を確認し、Frameworkの生成順は変更していない。結果は§7.6。

HTTPのloopback fixtureでは`server.servlet.session.cookie.secure=false`を明示し、公開HTTPS用の既定は変えない。R-02で既存Critical JourneyのHTTP子processへ同上書きを追加した。requestSecure=false／secure明示trueでもSecureが付くことは合成testのSet-Cookieで確認済み。context-path変更・Cookie Domain共有はこのB1構成に加えない。

#### tokenの取得・送信・更新

| 時点 | server処理 | SPA処理 |
| --- | --- | --- |
| login | MVCのHttpSession CSRFとform loginを維持。成功handlerでSPA repositoryの`saveToken(null,...)`を呼び、既存の成功先`/`へ委譲 | homeから`/spa/`へ全画面遷移。SPA内にpassword／JWT受付を作らない |
| SPA起動・再表示 | 認証済み`GET /spa-api/csrf`は204＋`Cache-Control: no-store`。標準`spa()`が必要なCookieをmaterialize。Controllerはtoken値を返さない | Cookieを毎回読み直す。初回GET成功後に一覧等を取得。tokenをReact stateへ長期保持しない |
| create／submit／SPA logout | Cookie＋headerを標準CSRF filterで照合。MVC `_csrf`やCSRF query parameterへfallbackしないAPI呼出を使う | `credentials: same-origin`、unsafe requestに`X-XSRF-TOKEN`。header値はCookie値から取得し、独自mask解除をしない |
| token欠落・不一致 | 403。CSRF filterが先に新Cookieを発行する場合があっても、当該POSTは拒否 | CSRF再取得を案内する。元POSTを自動再送せず、最新業務状態を照合して人が操作し直す |
| logout／次のlogin | 両logout経路とlogin成功でSPA Cookieを消去。次の認証後GETで再発行 | 保持表示・入力を破棄し、再login後は全データを再取得 |

`spa()`はCSRF filter内でtokenを実体化するため、未認証401のrequestにもCSRF Cookieが付く場合がある。Cookie発行は認証成立を意味しない。MVC chainの標準`CsrfAuthenticationStrategy`が消去するのはMVC側repositoryであり、別のSPA Cookieを自動で消すとは扱わない。

SPA clientの送信契約はCookie＋headerだが、標準handler自体にはheaderがない場合にXOR表現のparameterを解決する経路もある。serverがあらゆるparameter方式を禁止する独自契約は追加しない。「header欠落拒否」の検証は代替の有効tokenを送っていない条件とし、MVC tokenをSPA更新の証拠に代用しない。

Cookie repositoryはtokenのserver保存・Sessionへの暗号的な結合を行わない。通常browserでlogin／logout後にCookieが削除・再発行され、古いheaderと現在のCookieの不一致が拒否される範囲を検証する。任意の古いCookie＋一致headerを再注入した対まで失効する保証や、signed double-submitは本設計に含めない。

### 5.4 logoutの接続と失敗時の応答

SPAは**POST `/spa-api/logout`だけ**をlogout matcherとする。GETでlogoutしない。`koikiSessionLogoutCustomizer.customize(http)`を同chainへ適用し、その後に専用URLと成功handler（204・bodyなし）を指定する。MVCのPOST `/logout`は既存の成功遷移を維持する。通常の`CsrfLogoutHandler`に加え、MVC logoutでもSPA repositoryを消去する接続を置く。

Springの`LogoutFilter`はAuthorizationFilterより前で処理を完結する。正しいCSRFで既にSessionがないlogoutは204の冪等応答とし、一般のSession APIの401とは区別する。CSRF欠落／不正なら、匿名であっても先行filterで403にしてSession／業務状態を変更しない。

Framework logout handlerは永続Sessionの失効失敗でもlocal context・credential・Session Cookieを消去してから例外を投げる。`LogoutFilter`内のこの例外はMVC Adviceでは扱えないため、Referenceの`ReferenceSessionLogoutBoundary`を**CsrfFilterより後、LogoutFilter直前**に置く。実装仕様は次のとおり。

- 対象は当該chainの正確なPOST logout pathだけ。その他のrequestは素通しする。Servlet Filterとして二重登録せず、SecurityFilterChain内のprivate instanceとして構成する。
- 対象logout処理の`finally`でlocal SecurityContextを除去し、未commitならSPA CSRF CookieとSession Cookieも除去する。Sessionの再読取・失効再試行はせず、CSRFは同じrepository、Sessionは注入済みCookieSerializerで空値を書き込む。handler途中例外でも後続handlerの実行に削除を依存させない。正常時はhandlerでも削除してsuccess handlerのcommit前に発行する。失敗は一旦保持し、local cleanup後に503を出す。
- 永続失効等のRuntimeExceptionは成功へ読み替えない。未commitならSPAは503の安全なProblem Details、MVCは503の非機密な短いエラー応答とし、成功遷移を返さない。responseがcommit済みなら成功へ書換えず接続失敗として記録する。生のexception値は応答に含めない。
- Cookie消去・logout失敗のどちらでも、SPAは保持データを破棄する。503／通信断は「local破棄済み・server失効未確認」と表示し、失効確認を人へ戻す。自動logout再試行や認証状態の復元はしない。

永続失効失敗時に、すでに持ち出されたSession Cookieまで無効化済みとは主張しない。この限定接続はReference所有で、Frameworkのlogout契約・Public APIを変えない。

### 5.5 JSON DTOの具体仕様

Session APIは`application/json`を入出力とし、errorは`application/problem+json`。Domain／Application recordを直接serializeしない。専用DTOは`expense.adapter.inbound.sessionapi`に閉じ、MVC Form・Bearer DTO・Framework Identityへ属性を足さない。

| JSON型／項目 | 形式・境界 |
| --- | --- |
| UUID | canonical UUID文字列。入力の未指定・null・不正形式は400。申請ID・明細ID・applicantはserverが決定する |
| 金額`claimedAmount`／`amount` | 円整数の十進**文字列**。`[1-9][0-9]{0,18}`かつLong.MAX_VALUE以下。空白・符号・先頭0・小数・指数・数値tokenは400 |
| `version`／`expectedVersion` | 十進**文字列**。`0`または`[1-9][0-9]{0,18}`、Long.MAX_VALUE以下。JS Numberへ変換せず、取得した値を送る |
| `totalElements` | 非負longの十進文字列。page／sizeだけJSON整数。pageは0以上のint、sizeは1〜100、既定0／20 |
| `usageDate`／`updatedAt` | 日付は`YYYY-MM-DD`、更新時刻はUTC Instant文字列。日付の未来拒否は既存API／業務のUTC基準に合わせる |
| description／purpose | 非blank、最大200／500文字。既存のvalidation・Domainで再検証。renderはReactのtextとして扱う |
| status／decisionReason | statusは既存6状態の文字列。decisionReasonはnullable、権限scope内の詳細だけで返す |

入力の金額・versionにはDTO局所の厳密なString deserializerを付け、JSON number→Stringの暗黙coercionを拒否する。`@NotNull`・形式／範囲のvalidationを行ってからmapperでlongへ変換し、非数値例外を500へ落とさない。global ObjectMapper設定・既存Bearer APIのlong契約は変えない。未知fieldは既存strict JSON方針に従い400とする。

| DTO名 | 完全なJSON項目／内容 |
| --- | --- |
| `SessionExpenseOptionResponse` | `{id, code, name}`。部門・費目で同じ表示形 |
| `SessionExpenseOptionsResponse` | `{departments: Option[], expenseCategories: Option[]}`。現在所属の有効部門と費目だけ |
| `SessionExpenseListItemResponse` | `{expenseRequestId, department: Option, claimedAmount, status, version, updatedAt}`。applicant ID／emailなし |
| `SessionExpensePageResponse` | `{content: ListItem[], totalElements, page, size}`。Application paging結果をcopy |
| `SessionExpenseLineResponse` | `{expenseLineId, expenseCategory: Option, usageDate, description, purpose, amount}` |
| `SessionExpenseDetailResponse` | `{expenseRequestId, department: Option, claimedAmount, status, decisionReason, version, updatedAt, lines: Line[]}` |
| `CreateSessionExpenseLineRequest` | `{expenseCategoryId, usageDate, description, purpose, amount}`。line IDは受付後に生成 |
| `CreateSessionExpenseRequest` | `{departmentId, claimedAmount, lines: CreateLine[]}`。linesは非empty／null要素なし。UIは初回1明細、APIは既存Use Caseの複数明細を維持 |
| `CreateSessionExpenseResponse` | `{expenseRequestId}`。201のLocationは`/spa-api/v1/expense-requests/{id}` |
| `SubmitSessionExpenseRequest` | `{expectedVersion}`。successは204、bodyなし |

新規DTOは上記10型。mapper・validation／deserializer・Adviceは同packageに置く。合計一致・加算overflow・master現在値は既存Use Case／Moneyが最終判断し、表現が正しく業務制約に反する金額は422とする。UIのinput値は文字列のまま保持し、比較・合計が必要なときだけBigIntを使い、送信前に十進文字列へ戻す。

例：submit bodyは`{"expectedVersion":"9007199254740993"}`。Number安全域を超える値もそのままApplicationへ渡し、既存対象の実versionと比較する。`{"expectedVersion":9007199254740993}`、`{"expectedVersion":"01"}`、Long.MAX_VALUE超過は受付400とする。

### 5.6 error・画面回復の具体仕様

共通error項目は`type: about:blank`、HTTP相当の`title`／`status`、固定の安全な`detail`、queryを含めないpathの`instance`、`code`。validation時の`violations`はfield／messageだけとし、拒否値・token・内部exceptionを含めない。全Session API応答にno-storeを設定する。

| 条件 | status／code案 | SPAの処理 |
| --- | --- | --- |
| 未認証のGET、正しいCSRFで未認証の業務POST | 401／`KOIKI-REF-SESSION-001` | 表示・入力・選択中IDを破棄し、loginへの全画面遷移を案内 |
| CSRF拒否 | 403／`KOIKI-REF-CSRF-001` | 操作結果を再照合。token再取得後も元POSTを自動再送しない |
| 認証済み権限不足 | 403／`KOIKI-REF-SESSION-002` | 当該表示を消去して利用不可を表示 |
| 存在しないID／scope外 | 404／既存`KOIKI-REF-EXPENSE-003` | 同じ本文・codeで秘匿し、一覧へ戻る |
| DTO形式・validation／不正JSON | 400／既存`KOIKI-VALIDATION-001`、`KOIKI-JSON-001`等 | 項目errorを表示。Use Caseを呼ばない |
| 業務制約／master無効 | 422／既存`KOIKI-REF-EXPENSE-001`／`002` | 入力・候補の見直しを案内 |
| 状態遷移／version／データ競合 | 409／既存`KOIKI-REF-EXPENSE-004`〜`006` | 最新詳細を再取得し、人が再操作 |
| 依存失敗／logout失敗 | 503／既存expense `007`、logoutは`KOIKI-REF-SESSION-003` | 失敗を成功とせず表示。logoutでは保持値を破棄 |
| 予期せぬ例外 | 500／既存`KOIKI-INTERNAL-001` | 安全なエラー表示。更新結果不明なら自動retryしない |

CSRF検査は認証・業務処理より先行し、未認証unsafe requestもtoken欠落／不正なら403。logoutの冪等204は§5.4の例外である。API未定義pathは未認証401／認証済み404、unsupported methodは認証・CSRF成立後405を基準にする。MVCの既存400／409・HTML／HTMX拒否表現は維持し、Session APIの表現へ統一しない。

クライアントのfetchは`credentials: same-origin`・`cache: no-store`・`redirect: error`とし、login HTMLをJSONとして処理しない。logout開始時に新規操作を止め、進行中GETをabortし、request世代番号で遅延応答が画面を再充填するのを防ぐ。ページ離脱／再表示・hiddenからvisibleへの復帰でも保持値を捨てて再取得する。他タブの認証変更を非表示期間中に即時検知する保証は設けず、再表示時と次のAPI拒否時を回復点とする。

### 5.7 照合した標準仕様

- [Spring Security 7.1.1 CsrfConfigurer](https://github.com/spring-projects/spring-security/blob/7.1.1/config/src/main/java/org/springframework/security/config/annotation/web/configurers/CsrfConfigurer.java)：`spa()`はCookie repository／内部handlerを設定。CSRF logout handlerはchain内repositoryに対して登録される。
- [CookieCsrfTokenRepository](https://github.com/spring-projects/spring-security/blob/7.1.1/web/src/main/java/org/springframework/security/web/csrf/CookieCsrfTokenRepository.java)、[CsrfAuthenticationStrategy](https://github.com/spring-projects/spring-security/blob/7.1.1/web/src/main/java/org/springframework/security/web/csrf/CsrfAuthenticationStrategy.java)：Cookie属性・削除、認証時に渡されたrepositoryだけを更新する契約。
- [LogoutFilter](https://github.com/spring-projects/spring-security/blob/7.1.1/web/src/main/java/org/springframework/security/web/authentication/logout/LogoutFilter.java)：handler正常終了後にのみsuccess handlerを呼ぶ。MVC Adviceでfilter例外を補う設計にはしない。
- [Boot 4.1.1 SessionAutoConfiguration](https://github.com/spring-projects/spring-boot/blob/v4.1.1/module/spring-boot-session/src/main/java/org/springframework/boot/session/autoconfigure/SessionAutoConfiguration.java)：ServerPropertiesのCookie属性適用を確認。今回のReference Beanが自動適用を受けるという意味ではない。

## 6. B1-03：frontendとpackageの具体仕様案

| 項目 | 選定した方式 | 具体仕様・確認事項 |
| --- | --- | --- |
| source所有 | `koiki-reference-app/frontend/`。React＋TypeScript、一覧・作成・詳細の3状態、fetchを使用 | 初回はVite標準TSX変換。追加router・UI library・React Fast Refresh pluginなし |
| 配置 | 同じReference JARの`META-INF/resources/spa/`へindex・assetを同梱 | Nodeはbuild時だけ使用。production runtimeは既存JAR |
| build順 | Reference限定profile `b1-session-spa`。Maven clean→generate-resources内のNode処理→resource copy→repackage | 前段の外部npm buildを手で維持する方式から具体化。生成先はclean後の`target/b1-frontend/` |
| 既存build保護 | profile未指定の通常rootはNodeを呼ばない | 両構成をclean buildで区別。SPA同梱JARに限ってB1 DoDを評価 |
| UI／routing | `/spa/#/requests`、`#/new`、`#/requests/{id}` | shellは`/spa`・`/spa/`からだけ明示forward。全URLをindexへforwardしない |
| 開発server | 初回の受入構成には含めず、同梱JARで実演 | dev proxy・CORS追加の整備は今回の成立条件にしない |
| browser | 既存Playwright Java `1.62.0`を再利用 | Node版Playwrightは導入しない。新規B1 journeyと既存Critical Journeyを選択して実行 |

今回の代表操作では新規Java runtime dependency・migration・Framework SPA Starterを想定しない。以下のfrontend依存・Reference限定pluginをB1-04へ渡す。Profile Sの一般的なOIDC構成と、今回local loginで実証するSession／CSRFの範囲を区別する。

### 6.1 build依存の選定値

| 用途 | 選定version | 配置・確認した範囲 |
| --- | --- | --- |
| Node／npm | `24.19.0`／`11.9.0` | この環境のversionと一致。packageのengines／packageManagerとbuild scriptで照合し、不一致は実行前に停止 |
| runtime npm依存 | `react: 19.3.1`、`react-dom: 19.3.1` | package.jsonのdependencies。bundleに含む。react-domのpeer React条件をregistryで照合 |
| build npm依存 | `typescript: 7.0.2`、`vite: 8.3.4` | devDependencies。npm registryの実在・engineを照合。ViteのNode条件をNode24.19.0が満たす |
| 型定義 | `@types/react: 19.3.0`、`@types/react-dom: 19.3.0` | devDependencies。対応するpeer条件を照合。JS設定fileを使うので初回は@types/node不要 |
| MavenからNode起動 | `org.codehaus.mojo:exec-maven-plugin:3.6.3` | 新profile内だけ、generate-resourcesへbind。Maven Central POMの実在確認 |
| asset copy | `org.apache.maven.plugins:maven-resources-plugin:3.3.1` | 新profile内の明示copy-resources execution。Maven Central POMの実在確認 |

これらは2026-10-20の設計選定値。package install・組合せbuild・脆弱性検査の成立を意味しない。package.jsonは上記を範囲指定なしで固定し、実装時に同じnpmで生成したlockfileVersion 3の`package-lock.json`をcommit対象とする。以後は`npm ci`で取得し、buildで`npm install`や`latest`へ解決し直さない。lock生成・依存変更の具体差分はB1-04で扱う。

[Vite 8.3.4の標準仕様](https://github.com/vitejs/vite/blob/v8.3.4/docs/guide/features.md)はTSX変換を提供するが型検査は行わないため、`tsc --noEmit`を別に必須化する。初回はReact plugin・RSC・SSRを導入せず、`jsx: react-jsx`／React automatic runtimeを設定する。

### 6.2 build入力・生成物・Mavenの実行順

frontend入力は`package.json`、`package-lock.json`、`index.html`、`tsconfig.json`、`vite.config.mjs`、`scripts/package.mjs`、`scripts/verify-build.mjs`、`src/main.tsx`、`src/App.tsx`、`src/api.ts`、`src/types.ts`、`src/styles.css`を基準とする。package.jsonのbuild scriptは型検査→`vite build`→生成物検査を直列化する。`.gitignore`へ`koiki-reference-app/frontend/node_modules/`だけ追加し、出力は既存ignore対象の`target/`へ閉じる。

`vite.config.mjs`ではbase `/spa/`、出力`../target/b1-frontend`、`emptyOutDir: true`、`build.target: es2022`、source map無効、manifest有効、JS／CSSを`assets/`下へ設定する。rootの`index.html`に業務情報・tokenを埋め込まず、外部module scriptとCSSを参照する。tsconfigはstrict・noEmit・jsx react-jsx・module ESNext／Bundler resolution・ES2022／DOMを基準とする。

| 順序・phase | 実行・停止条件 |
| --- | --- |
| `clean` | Referenceの`target`を消去。以前の同梱assetを通常JARへ残さない。正式候補の作成commandではcleanを省略しない |
| `generate-resources` | profileのexec goalで`node frontend/scripts/package.mjs`を呼ぶ。scriptは自身の位置からfrontend rootを解決し、Node／npm versionとlock存在を検査する |
| 同script内 | 残存する当該`target/b1-frontend`だけを削除→`npm ci --no-audit --no-fund`→`npm run build`。子commandは終了code非0で直ちに停止。実行中にversionやlockを自動修正しない |
| build検査 | indexが参照するJS／CSSがすべて存在し、baseが`/spa/`、source mapなし、外部CDN・dev server URLなしを検査。frontend入力とlockのSHA-256、asset一覧・hashを記録 |
| `process-resources` | `target/b1-frontend/index.html`と`assets/**`だけを`${project.build.outputDirectory}/META-INF/resources/spa/`へcopy。`.vite/manifest.json`やsource・node_modulesは静的配信へ含めない |
| 同phase | build記録を`META-INF/koiki-reference-spa-build.json`へcopy。静的配信場所へ置かない。profile名・frontend入力hash・lock hash・Node／npm version・asset hash一覧を持つ |
| `package` | 既存Boot repackage。生成物欠落やNode処理失敗を無視するskip／fallbackを設けない。B1用JARは通常と同名なので、hashとbuild記録で構成を識別する |
| package後 | 新規B1 Toolingは起動前に`JarFile`でindex／asset／build記録を検査し、現在の入力hashと突合。profile不一致・古い生成物・不足assetならbrowserを起動しない |

Node scriptからnpmを呼ぶ際、Linuxはshellなしで固定引数を渡す。Windowsは固定のnpm commandだけを`cmd.exe /d /s /c`へ渡し、path・外部入力をcommand文字列へ連結しない。OS差はscript内へ閉じ、Mavenでnpm.cmd／npmを個別に切り替えない。Repositoryの秘密値・環境全体をbuild記録へ保存しない。

### 6.3 JAR配信と画面の入口

`ReferenceSpaWebConfiguration`は同梱indexの存在を`@ConditionalOnResource`で確認してから、GET `/spa`・`/spa/`を`/spa/index.html`へforwardする。homeは同じresource存在をmodelへ渡してSPAリンクを出す。profile未指定のclean通常JARではshell mappingとリンクを出さない。API source／Security chainのcompileをMaven profileで分岐させず、通常testからも境界を検査できるようにする。

browser chainのsecurity matcherは`/spa`と`/spa/**`を対象に含める。認可は`/spa/assets/**`のpermitAllを先に指定し、それ以外の`/spa`・`/spa/**`はauthenticatedとする。indexはno-store、hash付きassetは非機密のbuild成果物として扱う。indexへのforwardはGET `/spa`・`/spa/`だけに限定する。

GET `/spa/unknown`は未認証ならlogin遷移、認証済みならMVCのmapping・resource不成立による404とする。存在しない`/spa/assets/**`は認証の有無によらず404とする。`/spa-api/v2/**`は専用Session API chainで未認証GETをJSON 401、認証済みGETを404とし、shellへforwardしない。初回はroot contextの配置だけを対象とする。

Reactはhashから一覧・新規・詳細を選択し、API URLは`/spa-api/...`に固定する。browserの戻る／進むとshell再読込は新しいGETで状態を照合し、POSTを再現しない。JSでSession Cookieを読む処理、OAuth token保管、Service Worker、永続client cacheを追加しない。

### 6.4 実装後に使用するcommand案

以下はまだ存在しないprofile／testを含む**実装後の予定手順**であり、今回実行したものではない。

```bash
# 通常構成：Nodeを要求しない合流回帰
./mvnw --batch-mode --no-transfer-progress clean verify

# B1同梱構成：このprofile内でnpm ci・型検査・bundle・copyを実施
./mvnw --batch-mode --no-transfer-progress -pl koiki-reference-app -am -Pb1-session-spa clean verify

# 上で作成した同じB1同梱JARを選択し、package境界と両UIを確認
./mvnw --batch-mode --no-transfer-progress -f build-support/reference-e2e-verification/pom.xml -Dtest=PackagedReferenceSessionSpaJourneyTest,PackagedReferenceCriticalJourneyTest test
```

通常rootとB1 profileの各集合は1回ずつを計画し、R-00〜03ごとに繰り返さない。Windowsは同じ引数を`mvnw.cmd`へ渡す。Root外Toolingの依存・browser準備はI-01で別確認し、このcommandの存在を環境準備成立に読み替えない。

### 6.5 新規・変更pathの候補

| 所在候補 | 内容・所有者 |
| --- | --- |
| `koiki-reference-app/src/main/java/org/koikifw/reference/identity/configuration/ReferenceSessionApiSecurityConfiguration.java` | Session API chain・JSON拒否・CSRF・logout。Reference identity構成 |
| 同directoryの`ReferenceSessionLogoutBoundary.java`、`ReferenceSessionApiProblemWriter.java` | logout filter例外境界とSession用Problem Details。Bearer固有文言のwriterを流用しない |
| `koiki-reference-app/src/main/java/org/koikifw/reference/identity/adapter/inbound/web/ReferenceSessionCsrfController.java` | 認証後の204 endpoint。tokenの値をbody／logへ出さない |
| 同directoryの既存`ReferenceSecurityConfiguration.java` | R-02、SPA入口／asset許可、必要なlogin・logout接続。既存MVC chainを保護 |
| `koiki-reference-app/src/main/java/org/koikifw/reference/expense/adapter/inbound/sessionapi/` | `ExpenseSessionApiController`、専用DTO／mapper／Advice。Reference expense。部門・費目readも自module Use Caseに委譲 |
| `koiki-reference-app/src/main/java/org/koikifw/reference/expense/configuration/ReferenceSpaWebConfiguration.java`、既存`koiki-reference-app/src/main/java/org/koikifw/reference/HomeController.java`／`koiki-reference-app/src/main/resources/templates/home.html` | 同梱resourceを条件とするSPA入口とリンク。既存version設定の拡張は不要 |
| `koiki-reference-app/frontend/`、`koiki-reference-app/pom.xml`、`.gitignore` | §6.1〜2の依存・入力file、Reference限定profile、node_modules除外 |
| `koiki-reference-app/src/test/java/org/koikifw/reference/`配下 | 新規Session chain／Controller／DB test。packageと配置を一致させ、通常testとTooling fixtureを区別 |
| `build-support/reference-e2e-verification/src/test/java/org/koikifw/buildsupport/referencee2e/` | 新規B1 browser journey。Tooling所有。既存Critical Journeyは直接回帰として維持 |

class名は実装対象の候補で、ファイル作成は未実施。DTOは§5.5の10型、profile／pluginは§6.1〜2を設計選択とする。実際の差分数・残作業概算と実行予算は、基盤是正の結果を入力してB1-04で更新する。

## 7. R-00〜03の変更対象・最小回帰案

R-00は下記1 fileの変更・局所検証済み。R-01は§7.5のAdvice修正・限定回帰を実施。R-02は§7.6のCookie設定反映・限定回帰を実施。R-03は変更予定の所在と検証観点で、まだ実装していない。共有回帰は合流時にまとめ、通常rootを修正ごとに反復しない。

| ID | 変更対象候補 | 最小の検証観点・既存回帰 |
| --- | --- | --- |
| R-00／F5：限定変更・検証完了 | `koiki-reference-app/src/test/java/org/koikifw/reference/ReferenceArchitectureTest.java`の両import | `DO_NOT_INCLUDE_TESTS`でproduction対象を明示。clean実行で既存2件＋規約負例5件PASS。両importのtest由来class0を確認。結果は§7.4 |
| R-01／F1：限定変更・検証完了 | `koiki-starters/koiki-starter-api/src/main/java/org/koikifw/starter/api/internal/KoikiProblemDetailsExceptionHandler.java`。Reference局所再throwは維持 | `build-support/runtime-foundation-verification`の`ApiProblemDetailsTest`等＋Security filterを通す新規合成testを追加。認証済み403・未認証401／login遷移・独自404・不測500。`ReferenceBusinessUrlSecurityTest`のMVC／HTMX拒否を保護 |
| R-02／F2：限定変更・検証完了 | `ReferenceSecurityConfiguration.java`のCookieSerializerへServerPropertiesを反映。合成test追加、Critical JourneyのHTTP設定・logout検証を更新。Framework Session構成は無変更 | Boot SessionAutoConfigurationを含め、Secure明示true／false、requestSecure両条件、SameSite上書き、logoutのname／path／消去をSet-Cookieで検証。`SessionJdbcAutoConfigurationContextTest`と実JARの起動・logoutでcustomizer生成を確認 |
| R-03／F3 | `ExpenseApplicationService.java`の共有`executeMutation`。認可→versionの順序へ限定 | `ExpensePostgreSqlIntegrationTest`・`ExpenseApplicationServiceMethodSecurityTest`・`ExpenseAuditRollbackPostgreSqlIntegrationTest`、`ExpenseApiPostgreSqlIntegrationTest`。owner／approverのscope外・不存在・一致／不一致／負version、scope内競合を比較。拒否時に更新・成功Business Audit・eventなし |

R-03の負versionはApplication直接呼出とHTTP DTO検証を区別する。HTTP側の`@PositiveOrZero`で先に400になることを、対象の存在漏えいと混同しない。R-03は共有mutationを変えるため、SPAに出さないapprove／reject／returnForRework／beginReedit／settleの直接影響も確認する。同期veto・event内容・非同期送信の仕様は変更しない。

**R-00の変更前baseline：** [Cloud点検記録§4](../architecture/validation/phase4-r00-cloud-environment-preflight-20261020.md)ではimport指定を変えずにReference Architecture2件・規約負例5件がPASS。fresh importは243 classes中test65、production限定178、business違反は双方0だった。過去の15違反を現在の失敗と扱わず、検査責務の明示として§7.4の変更を実施した。

### 7.1 B1実装時の検証表案

| 集合 | 主な観測 | 入口候補 |
| --- | --- | --- |
| B1-T1 route／認証 | MVC／SPA shellの未認証GETはlogin遷移。GET `/spa/unknown`は未認証login遷移／認証済み404、存在しないassetは認証有無によらず404。Session API未定義pathのGETはJSON 401／認証済み404としlogin遷移・shell配信をしない。Bearer Cookie-only拒否、Session API Bearer-only拒否、両credential時のSession主体、fallback deny維持 | 新規Reference Security合成test＋既存API／MVC URL test |
| B1-T2 CSRF | Cookie＋header成功、header欠落・不一致・Cookie欠落拒否、login／logout前後のtoken更新、MVC hidden／HTMX既存動作 | 新規filter合成test＋実browser。security testの`csrf()`だけで代替しない |
| B1-T3 DTO／error | 400／404／409／422／503／500、scope外の応答同一性、数値往復、不要なemail・内部例外の非露出 | 新規Session Controller test、R-01／03の結果と照合 |
| B1-T4 DB／Audit | SPA create→read→submit→MVC read、MVC create→SPA read、別user拒否・古いversion拒否時に副作用なし | 新規Session API PostgreSQL test、既存read model／API DB test |
| B1-T5 package browser | 同一JARのSPAとMVC、実Cookie送信とCSRF header、logout・失効・再login時の表示破棄、直接アクセス・再読込、二重操作抑止・結果不明表示 | 新規B1 browser journey＋既存`PackagedReferenceCriticalJourneyTest` |
| B1-T6 合流回帰 | architecture、通常root、既存Bearer／MVC、JAR asset・依存、所有資源の終了 | R-00結果＋局所結果→通常root1回→選択browser実演。実行／skipを新XMLで記録 |

### 7.2 実行案・資源・証拠の具体化

実行command候補は、局所のReference testが `./mvnw -pl koiki-reference-app -am -Dtest=<選択class> -Dsurefire.failIfNoSpecifiedTests=false test`、Framework局所が対象Tooling POMへの `-f ... -Dtest=<選択class> test`、合流時がrootの `./mvnw clean verify` である。DB testのclean compile・依存準備・test選択を先に整え、空の選択をPASSとせず新XMLで実行classを照合する。

browser回帰は§6.4のprofileでpackageした同じB1 JARを対象とし、同節の新旧2 class選択を使う。集合の間に通常JARへ再packageしない。追加classの所在・役割は§7.3、正確なmethod集合・件数は実装差分確定後に記録する。通常rootとB1 profileのReference集合は別のXML・artifact epochで記録する。上記は実行済みcommandでも、現環境で動作確認済み手順でもない。

| 項目 | 現在の到達点 | 実装／実行へ渡す案 |
| --- | --- | --- |
| 実行資源 | 後続点検でclean compile・規約7件・Chromium smoke・networkなしcontainerのversion表示まで実行。DB・applicationは未起動 | B1機能実演はphaseを直列化し、DB同時1・application子process1・browser1を基準候補にする。数値予算は別に具体化 |
| 取得・準備 | 保存済み構成・cacheだけで局所検証成立。installなし | frontend lockは実装時に作成。Docker実行時は環境のproxy／CA手順を適用 |
| 証拠 | fresh XML・log・source無変更のhash照合をCloud点検記録へ保存 | source SHA＋未commit差分、JAR hash・profile、lock hash、command、実行／skip、DB／Audit／sanitized logをrun単位で対応付ける |
| 停止 | sandboxのDocker／browser拒否は同じ診断を権限付きで成功。製品修正なし | 機能実演での漏えい・回帰・上限超・artifact不一致・cleanup不成立では当該集合を止める。root自動反復をしない |
| cleanup | 一時container・live子processは終了。終了済みbrowser zombie2件を点検記録へ明記 | 別runや親processを停止しない。次runでもowned resourceを区別して終了確認 |

I-01からR-00へ渡す局所検証の環境準備は成立した。B1新規実装の資源・test集合は別途確定する。過去S1の接続上限・残時間・実行権限をB1の予算へ転用しない。

### 7.3 具体仕様に対応する追加検証

新規classは次の5件を基準案とする。既存のR-00〜03／API／MVC回帰と役割を分け、同じ観点の巨大なcase一覧を別紙に増やさない。正確なtest method数は実装差分確定後に記録する。

| 新規class・所在 | 観測する境界 |
| --- | --- |
| `ReferenceSessionApiSecurityTest`：Reference `identity/configuration`のtest | 本物のfilter chainとBoot Session構成を使う。Session APIとBearerのcredential分離、無権限、401／403の順序、Cookie＋headerの実照合、MVC hidden token維持、GET logout非成立を確認 |
| 同classのlogout／login集合 | 古いCSRF Cookieを持ってform login→削除→GET再発行→更新。MVC／SPA logout両側のCookie削除と旧Session拒否。失効例外をhandler代替で注入し503・local cleanup・成功handler非実行を確認。filterのServlet二重登録なし、customizer Bean成立も観測 |
| `SessionExpenseApiControllerTest`：Reference `expense/adapter/inbound/sessionapi`のtest | 10 DTOのJSON項目、生成IDとLocation、paging、strict String入力。`9007199254740993`・Long.MAX_VALUE境界、超過・負値・先頭0・JSON number・null・未知field、不要なemail／Domain非露出、既存codeとの対応を確認 |
| `SessionExpenseApiPostgreSqlIntegrationTest`：同test package | 同じ申請でSession create→read→submit→MVC readをDB／Auditと突合。scope外・不存在・version競合・無効master拒否で更新・成功Audit・eventが増えない。DBのSession失効・API拒否も確認 |
| `ReferenceSpaWebConfigurationTest`：Reference `expense/configuration`のtest | test専用resource／classloaderで同梱indexの有無を切替。mapping・home link、index認証、asset permit、unknown path非forward・no-storeを確認。通常testがnpm build成果物に依存しない構成にする |
| `PackagedReferenceSessionSpaJourneyTest`：既存Root外E2E Tooling | 同梱build記録・JAR asset hashを起動前照合。本物のCookie／header、create／submit／MVC突合、直接アクセス／再読込、logout／失効、戻る・再表示後のデータ破棄を実Chromiumで確認。入力・DB・Audit・sanitized logを対応付けてcleanup |

logoutのhandler例外注入はReference test内に閉じ、productionに失敗switchを置かない。Cookieはname／HttpOnly／Secure／SameSite／Path／Domain／削除Max-Ageを実ヘッダーで観測し、property値だけで受入しない。

buildの負例は、使い捨てworkspaceまたは検査用一時directory内でlock欠落／不一致・Node version不一致・型error・不足asset・古い入力hashを選択して失敗を確認する。通常JARにSPA assetが残らないことと、B1 JARにsource map・node_modules・frontend source・公開build記録が混入しないこともpackage境界で確認する。正式sourceのlockを書き換えて負例を残さない。

R-00〜03の局所検証後に、通常root1回、B1 profileのReference reactor1回、新旧browser journeyの選択集合へ進む案とする。今回のCloud点検は既存compile・規約7件と環境smokeに限定し、このB1機能集合は未実行。B1-04で必要な資源／時間とfrontend準備を具体化する。

### 7.4 R-00の作業範囲と実行結果

**Ownerの着手指示に基づき、Referenceのproduction architecture検査からtest出力を明示的に除外する1 fileの変更を実施済み。** 変更前も7件はPASSしており、失敗を直す作業ではない。mainとtestを同じpackage名でimportすると、fixture追加や古いtest出力によってproduction検査の対象が変わるため、検査対象をsource setで定めた。

| 項目 | 今回実施した範囲 |
| --- | --- |
| 対象file | `koiki-reference-app/src/test/java/org/koikifw/reference/ReferenceArchitectureTest.java`だけ |
| 変更 | 既存2メソッドそれぞれの`ClassFileImporter`へ`withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)`を追加し、必要なimportを1件追加 |
| 維持する契約 | business module Rules、notificationの`LEVEL_2`選択、Framework Identity境界、対象package、既存2 assertionを維持。class名の個別除外・Rules緩和は行わない |
| fixtureの責務 | 意図的な違反を検出する既存`koiki-archunit-rules`のfixture／`RequiredNegativeArchitectureRulesTest`が担当。Referenceの業務・Security testはそれぞれのtestとして残る。65 classをproduction検査から除くことで業務testを無効化しない |
| 追加しないもの | 新規test class、fixture移動、package再配置、production変更、Rules／依存／POM／Security／CI変更。R-01の例外修正も混ぜない |
| 完了条件 | 変更後のclean compile、Reference Architecture2件＋規約負例5件PASS、failure／error／skip 0。fresh importの選択集合にtest-classes由来がないことを確認し、sourceと新XMLを結果へ対応付ける |
| countの扱い | production178 classesは変更前の観測値。恒久的な固定件数のassertionを追加せず、今回の対象と除外結果を証拠に記録 |
| 検証の範囲 | 下記の局所集合を1回。Docker／DB／browserは使用しない。通常root全件は基盤是正の合流時にまとめる |

実行commandは次のとおり。環境を再構築せず保存済みactivationとcacheを使い、同じ一回のclean testでcompileと7件を確認した。

```bash
source /workspace/cloud-setup/activate.sh
./mvnw --batch-mode --no-transfer-progress -o -pl koiki-reference-app -am -Dtest=RequiredNegativeArchitectureRulesTest,ReferenceArchitectureTest -Dsurefire.failIfNoSpecifiedTests=false clean test
```

**[限定結果](../architecture/validation/phase4-r00-production-import-results-20261020.md)：13 projects SUCCESS、clean compile＋7件PASS、failure／error／skip 0。** 業務規約は178 classes、Identity境界は218 classesで、ともにtest-classes由来0を確認した。選択2 classの新XMLとsource patch／hashを保存し、実行対象が空でないことを照合した。class件数の固定assertionや新規testは追加していない。commit・remote操作は行っていない。

R-00の限定結果を整理した次は、**R-01：汎用AdviceとSecurity／HTTP例外の境界是正**へ進む。対象はAPI Starterの`KoikiProblemDetailsExceptionHandler`と必要な合成test。認証済み403、未認証の経路別401／login遷移、独自404、不測500、既存MVC／HTMX応答を出口にする。R-02 Cookie、R-03認可順序はその次に問題単位で進め、三件とB1設計をB1-04で合流する。

### 7.5 R-01の作業範囲と実行結果

Ownerの「R-01へ進める」指示に基づき、`docs/phase4-review-integrated-plan-20261020`上でAPI StarterのAdvice1 fileと検証2 fileを変更した。**作業は同branch上で完結し、PR作成・pushは行わない。** commitも行っていない。

汎用AdviceはSecurity例外（causeを含む）を再throwし、各Security filterの401／403／login遷移へ渡す。`@ResponseStatus`／`ResponseStatusException`は標準HTTP処理へ渡し、独自`ErrorResponse`のstatus・header・bodyを保持する。Security型は任意classpathとして解決し、API Starterへ依存を追加しない。予期せぬ例外の安全な500は維持する。Referenceの局所再throwは今回除去しない。

[限定結果](../architecture/validation/phase4-r01-exception-boundary-results-20261020.md)へ変更前の失敗、修正後のSecurity合成・既存MVC／HTMX／API・Architecture回帰とSecurity依存なしのProblem Details結果を記録した。通常root全件・B1機能・全体DoDの受入へ拡張しない。次はR-02のCookie設定反映とlogout customizer生成順の確認。

### 7.6 R-02の作業範囲と実行結果（2026-10-21）

Ownerの着手指示に基づき、同じ作業branchでReferenceのCookieSerializerを修正し、BootのCookie設定を発行・削除に共通で反映した。既定のSecure=true・HttpOnly=true・SameSite=Laxは維持する。POM・依存version・Framework Session構成・既存Security matcherは変更していない。

追加した`ReferenceSessionCookieConfigurationTest`は、Boot Session／JDBC構成とFramework logoutを組み合わせて、Secureの明示true／false×requestSecure両条件、既定属性、SameSite上書き／省略、Cookie名・Path・寿命の反映、logout削除を実ヘッダーで確認する。customizerとhandlerが成立することもassertした。mock repositoryによる構成testと、実DB・実JARの検証を区別する。

Critical JourneyはHTTP fixtureのSecure=falseを明示し、実Chromiumのlogin Cookie属性、CSRF付きlogoutのSet-Cookie、browser側削除、旧Cookieでの再アクセス拒否、DB上のprincipal Session消去を追加した。Bearer／MVCの既存業務操作・Audit照合・sanitized log・cleanupも維持する。

[限定結果](../architecture/validation/phase4-r02-cookie-logout-results-20261021.md)へsource・artifact・選択test・途中経過を記録した。通常root全件・B1機能・DoD全体の受入へ拡張しない。今回のcommit・push・PR作成は行っていない。次はR-03の認可とversion照合順序の是正。

## 8. 次の更新と合流点

| 作業 | 今回の到達点 | 次の具体的な出口 |
| --- | --- | --- |
| I-01 | 保存済みCloud構成を有効化し、clean compile・規約7件・Docker／Chromiumを確認。前回の不足記述を訂正 | その後のsource／環境変更がある場合に必要差分だけ再確認。全再構築を前提にしない |
| B1-01 | path付き差分表と代表操作案を作成 | 代表操作の範囲を後続設計へ引継ぎ。未実装を実装済みにしない |
| B1-02 | §5に標準spa()・Cookie属性・login／logout接続・失敗応答、10 DTO・数値表現・codeを具体化 | R-01／02／03の設計・結果と照合。未実装のfilter順序・Cookie生成・JSON受入を合成testへ渡す |
| B1-03 | §6に依存version、Reference限定profile、Node起動・生成先・copy順、JAR配信・入力／asset hashを具体化 | 実装時にlockを生成し組合せbuildを確認。検証class案は§7.3。実行準備は未成立 |
| R-00〜03 | R-00はproduction対象明示・clean回帰7件PASS。R-01は例外境界、R-02はCookie／logoutの限定変更・回帰完了（§7.5〜6） | 次はR-03の認可順序。R-03・B1実装は未着手 |
| B1-04 | 未実施 | B1-02〜03の仕様とR-00〜03の結果を合流し、対象path・test集合・概算・資源／時間／停止／cleanupを本書で確定 |

次の実装単位は**R-03：認可をversion照合より先に行う是正**。R-00〜02は§7.4〜6の限定変更・検証を完了した。R-03の結果を加え、B1-04で設計と合流する。F6／F7／H1、非同期CI、S1全体の未達はB1調査・設計の停止条件にしない。

本書§5〜6のB1機能仕様はまだ実装・機能実演していない。Cloud点検とR-00〜02の限定変更・回帰を実施済み。DoD 4-8／4-9は未達のまま維持する。
