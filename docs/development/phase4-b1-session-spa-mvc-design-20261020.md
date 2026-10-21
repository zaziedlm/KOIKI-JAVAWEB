# P4-B1 Session SPA＋MVC：既存との差分と設計記録

作成日：2026-10-20

状態：**B1-07のCloud実装・統合検証完了（§12）**。最終通常root／同梱構成は各422件実行PASS＋99 skip、同じ同梱JARで新旧browser journey計2件PASS。実演で判明したMVC日付表示・明細保存を局所訂正した。ローカルPCの手動確認とB1-08のDoD 4-8／4-9 Owner受入は未実施。R-00〜03・B1-05〜06の結果は各時点の履歴として保持する。

Ownership：設計記録はFramework側。機能はReference `expense`／`identity`、Framework変更はR-01、検証harnessはToolingが所有する。

2026-10-21のB1-04合流点検は§9、その後のOwner開始指示に基づくB1-05結果は§10。B1-06の実装・局所結果は§11。B1-07の資源具体化・途中停止／是正・最終結果は§12。次はローカル手動確認の実施判断とB1-08の受入整理。

## 1. 目的と今回の到達点

[統合作業計画](phase4-review-integrated-task-plan-20261020.md)のI-01／B1-01を進め、B1-02〜03とR-00〜03へ渡す材料を本書にまとめる。以後も同じ記録を更新し、同じ内容の開始票を増やさない。

**既存の申請Use Caseを再利用し、自分の申請一覧・詳細、draft作成、submitを最小React UIから利用する案を採る。** 現行Bearer RESTをCookie認証へ開放するのではなく、別のSession API経路を設ける。部門・費目の選択肢も既存のscope付きreadをJSONへ変換する。承認・会計・通知復旧のSPA化は今回の範囲に加えない。

根拠は[Grand Design §13.5〜13.6・§27.8](../architecture/grand-design/KOIKI-JavaWeb-FW_グランドデザイン_v0.2.md)、[見直し草案R4の承認](KOIKI-JavaWeb-FW_Phase4実施計画_見直し草案_v0.1.md)、[他package分解§2.1](phase4-other-packages-breakdown-draft-20261005.md)、[レビューF1〜F5](../architecture/validation/main-framework-design-implementation-review-20261020.md)。2026-09-26のSession SPAでDoDを維持する判断は引き継ぐ。

| DoD | 成立条件 | 設計開始時の不足（現在の結果は§12） |
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

**B1-04での更新（2026-10-21）：** 上表はR-00点検時のsnapshotである。その後、CloudではR-02で実DB・実JAR・実ChromiumのCritical Journeyまで成立し、R-03では変更sourceの実DB・HTTP選択回帰まで成立した。R-02のJARはR-03適用済みartifactではない。Windowsでの別途点検も含む対応は§9.1にまとめる。frontend install・lock生成・B1同梱buildは引き続き未実施。

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

B1-06でprofileを実装し、B1-07で以下の順序の合流回帰と新旧実JAR journeyを実施した。具体的な資源引数・是正後の再検証・最終結果は§12と結果文書へ追跡する。B1-06のclean package・局所結果は§11へ区別して記録する。

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

上表のうちB1-05のSession認証／CSRF／業務APIと対応testは§10の範囲を作成済み。SPA配信入口・home link・frontend／profile／plugin・browser journeyは未作成。DTOは§5.5の10型を実装し、profile／pluginは§6.1〜2を後続への設計選択とする。差分と検証数は§10、概算・実行条件は§9.4を参照する。

## 7. R-00〜03の変更対象・最小回帰案

R-00は下記1 fileの変更・局所検証済み。R-01は§7.5のAdvice修正・限定回帰を実施。R-02は§7.6のCookie設定反映・限定回帰を実施。R-03は§7.7の認可順序是正・限定回帰を実施。共有回帰は合流時にまとめ、通常rootを修正ごとに反復しない。

| ID | 変更対象候補 | 最小の検証観点・既存回帰 |
| --- | --- | --- |
| R-00／F5：限定変更・検証完了 | `koiki-reference-app/src/test/java/org/koikifw/reference/ReferenceArchitectureTest.java`の両import | `DO_NOT_INCLUDE_TESTS`でproduction対象を明示。clean実行で既存2件＋規約負例5件PASS。両importのtest由来class0を確認。結果は§7.4 |
| R-01／F1：限定変更・検証完了 | `koiki-starters/koiki-starter-api/src/main/java/org/koikifw/starter/api/internal/KoikiProblemDetailsExceptionHandler.java`。Reference局所再throwは維持 | `build-support/runtime-foundation-verification`の`ApiProblemDetailsTest`等＋Security filterを通す新規合成testを追加。認証済み403・未認証401／login遷移・独自404・不測500。`ReferenceBusinessUrlSecurityTest`のMVC／HTMX拒否を保護 |
| R-02／F2：限定変更・検証完了 | `ReferenceSecurityConfiguration.java`のCookieSerializerへServerPropertiesを反映。合成test追加、Critical JourneyのHTTP設定・logout検証を更新。Framework Session構成は無変更 | Boot SessionAutoConfigurationを含め、Secure明示true／false、requestSecure両条件、SameSite上書き、logoutのname／path／消去をSet-Cookieで検証。`SessionJdbcAutoConfigurationContextTest`と実JARの起動・logoutでcustomizer生成を確認 |
| R-03／F3：限定変更・検証完了 | `ExpenseApplicationService.java`の共有`executeMutation`で、対象認可をversion照合より先へ移動。mutation以降は維持 | `ExpensePostgreSqlIntegrationTest`・`ExpenseApplicationServiceMethodSecurityTest`・`ExpenseAuditRollbackPostgreSqlIntegrationTest`、`ExpenseApiPostgreSqlIntegrationTest`。owner／approverのscope外・不存在・一致／不一致／負version、scope内競合を比較。拒否時に更新・成功Business Audit・eventなし |

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

browser回帰は§6.4のprofileでpackageした同じB1 JARを対象とし、同節の新旧2 class選択を使う。集合の間に通常JARへ再packageしない。追加classの所在・役割は§7.3、正確なmethod集合・件数は実装差分確定後に記録する。通常rootとB1 profileのReference集合は別のXML・artifact epochで記録する。上記は設計時の候補であり、実行済みcommandと現在の結果は§12へ区別して記録する。

| 項目 | 現在の到達点 | 実装／実行へ渡す案 |
| --- | --- | --- |
| 実行資源 | Cloud R-02で実DB・実JAR・browser、R-03で実DB回帰まで実行済み。WindowsでもR-03の47件を確認。各環境・sourceを§9.1で区別 | 新規DB／browser集合の資源案は§9.4。通常rootの全体資源は既存test構成を含め実行前に確定 |
| 取得・準備 | R-01／02で現sourceの内部artifactをlocal install。R-02では宣言済みOAuth2 test依存のcache不足を補充した。常に取得不要という意味ではない | frontend lockは実装時に作成。内部artifactと外部取得を区別し、proxy／CA・checksum検証を維持 |
| 証拠 | R-00〜03のCloud記録とWindowsの選択結果をsource・環境別に保持 | source SHA＋未commit差分、JAR hash・profile、lock hash、command、実行／skip、DB／Audit／sanitized logをrun単位で対応付ける |
| 停止 | sandboxのDocker／browser拒否は同じ診断を権限付きで成功。製品修正なし | 機能実演での漏えい・回帰・上限超・artifact不一致・cleanup不成立では当該集合を止める。root自動反復をしない |
| cleanup | 一時container・live子processは終了。終了済みbrowser zombie2件を点検記録へ明記 | 別runや親processを停止しない。次runでもowned resourceを区別して終了確認 |

I-01からR-00へ渡す局所検証の環境準備は成立した。B1新規実装の資源・test集合は別途確定する。過去S1の接続上限・残時間・実行権限をB1の予算へ転用しない。

### 7.3 具体仕様に対応する追加検証

新規classは次の5件を基準案とする。既存のR-00〜03／API／MVC回帰と役割を分け、同じ観点の巨大なcase一覧を別紙に増やさない。正確なtest method数は実装差分確定後に記録する。

B1-05で下記の認証／CSRF・DTO／error・実DBの3 classを作成し、22／40／10件を確認した（§10）。残る配信構成testとbrowser journeyはB1-06／07で作成する。

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

R-00〜03の局所検証後に、通常root1回、B1 profileのReference reactor1回、新旧browser journeyの選択集合へ進む案とする。R-02／03の後続検証は§7.6〜7の範囲で成立したが、このB1機能集合は未実行。B1-04で整理した資源／時間とfrontend準備は§9を参照する。

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

### 7.7 R-03の作業範囲と実行結果（2026-10-21）

Ownerの着手指示に基づき、同じ作業branchで`executeMutation`の対象認可とversion照合の2行を入れ替えた。処理は対象取得→owner／承認部門scope認可→version照合→mutation→flush→成功Business Audit→eventとなる。対象外・不存在は指定versionによらずNOT_FOUNDに揃え、認可済み対象の競合は従来どおりCONCURRENT_MODIFICATIONとする。

新規`ExpenseMutationAuthorizationOrderTest`でeditDraft／submit／beginReedit／approve／reject／returnForReworkの6操作を比較し、一致・不一致・負versionと不存在、mutation・flush・Audit・eventなしを確認した。実DBではmethod securityを通すowner／approver拒否・認可済みのversion拒否、状態／version・Audit・承認eventの不変性を追加検証。HTTPは他人の申請と不存在の正versionを404へ揃え、負versionは既存DTO検証の400を維持した。

既存の成功ライフサイクル・JPA楽観ロック・Audit失敗rollback・権限不足・API応答・event契約・Architectureも直接回帰へ含めた。settleに新たなowner／部門scope制約は設けず、既存のEXPENSE:SETTLE契約を維持する。Framework・POM・migration・DTO制約・同期veto・event payload・非同期送信は変更していない。

[限定結果](../architecture/validation/phase4-r03-authorization-order-results-20261021.md)へ変更前の再現・source・最終XML・資源とcleanupを記録した。今回commit・push・PR作成は行っていない。次はB1-04で設計とR-00〜03結果を照合し、実装対象・検証集合・概算・資源／時間を確定する。通常root全件・B1機能・DoD全体は未受入。

## 8. 次の更新と合流点

| 作業 | 今回の到達点 | 次の具体的な出口 |
| --- | --- | --- |
| I-01 | 保存済みCloud構成を有効化し、clean compile・規約7件・Docker／Chromiumを確認。前回の不足記述を訂正 | その後のsource／環境変更がある場合に必要差分だけ再確認。全再構築を前提にしない |
| B1-01 | path付き差分表と代表操作案を作成 | 代表操作の範囲を後続設計へ引継ぎ。未実装を実装済みにしない |
| B1-02 | §5に標準spa()・Cookie属性・login／logout接続・失敗応答、10 DTO・数値表現・codeを具体化 | R-01／02／03の設計・結果と照合。未実装のfilter順序・Cookie生成・JSON受入を合成testへ渡す |
| B1-03 | §6に依存version、Reference限定profile、Node起動・生成先・copy順、JAR配信・入力／asset hashを具体化 | 実装時にlockを生成し組合せbuildを確認。検証class案は§7.3。実行準備は未成立 |
| R-00〜03 | R-00はproduction対象明示、R-01は例外境界、R-02はCookie／logout、R-03は認可順序の限定変更・回帰完了（§7.4〜7） | 次はB1-04の合流点検。B1機能実装は未着手 |
| B1-04 | §9に合流点検・実装対象・検証割当て・概算・実行条件を整理済み | B1-05へ渡す。method数・通常root全体資源など実装時確定事項は§9.4に保持し、未確定のまま該当実行を開始しない |
| B1-05 | §10のReference backendを実装し、局所149件PASS。commit・remote操作なし | B1-06のUI／同梱へ渡す。backend局所PASSを実browser・DoD成立へ拡張しない |

次の作業は**ローカル手動確認の実施判断とB1-08の受入整理**。B1-04は§9、B1-05は§10、B1-06は§11、B1-07のCloud結果は§12。F6／F7／H1、非同期CI、S1全体の未達はB1の一律の停止条件にしない。

本書§5〜6のbackendは§10、frontend・build・配信入口は§11の範囲を実装済み。実JAR＋DBのbrowser統合実演はB1-07で確認する。Cloud点検とR-00〜03の限定変更・回帰は保持する。DoD 4-8／4-9は未達のまま維持する。

## 9. B1-04：設計と基盤是正結果の合流点検（2026-10-21）

### 9.1 source・証拠の位置づけと整合判定

Ownerの「B1-04の合流点検へ進める」指示に基づく文書作業。branchは`docs/phase4-review-integrated-plan-20261020`、開始HEADは`e9ff13a32219a2f66bbc7ac469840b160b319c65`、開始時working treeはclean。§5〜6、現行sourceと§7.4〜7の結果を照合した。今回の新規build・test・外部取得・Docker／browser起動はない。

| 入力 | 確認済みの範囲 | B1へ接続する判断・未検証の接続 |
| --- | --- | --- |
| R-00 `40eefa4` | production限定import、Rules・負例の維持 | 新規mainは同じArchitecture検査へ入れる。新規testのpackageとpathを一致させ、負例をproductionへ置かない |
| R-01 `1e0f149` | Security／HTTP例外の標準処理、安全な500、Security任意classpath | 新Session ControllerのAdviceはexpense業務例外に限定。認証／認可例外を汎用500に吸収しない。filter内のlogout失敗はMVC Adviceでは処理できず、§5.4の専用境界を実装する |
| R-02 `e9e760b` | Boot設定を既存CookieSerializerへ反映、既存MVC logout、Cloud実JAR＋DB＋browser | Session CookieSerializerを共用する。SPA CSRF repository・login時削除・両logoutとの接続は新規。R-02だけでSPA logout成立とはしない |
| R-03 `e9ff13a` | owner／承認部門scope認可→version、正常・拒否・rollback・HTTPの47件 | createDraft／submitをそのまま再利用し、Session APIへ独自の対象認可・transactionを複製しない。HTTP String versionの形式検査はApplication呼出前、業務認可はUse Case内 |
| SPA経路訂正 `a4e839f` | 文書でmatcherと404の期待値を整合 | browser chainへ`/spa`・`/spa/**`を追加し、assetsのpermitAllを先に指定。未知GETは未認証login遷移／認証済み404。専用Session APIはJSON 401／404で分離 |

上記と§5〜6の基本方針に矛盾はなく、今回追加のFramework修正は選定しない。既存Bearer DTO／MVC Formは再利用せず、Applicationのread／command契約だけを共有する。

CloudのR-02限定57件とJAR hashは[R-02結果](../architecture/validation/phase4-r02-cookie-logout-results-20261021.md)、R-03の47件とsource hashは[R-03結果](../architecture/validation/phase4-r03-authorization-order-results-20261021.md)に従う。R-03ではJARを再生成していない。

Windowsでは当該Ownerセッションの点検で、R-01後20件、R-02後clean選択51件、R-03後clean compile＋DB込み47件を別々に確認した。R-02のWindows点検は実JAR E2Eの再実行ではない。R-03の端末内集計は`target/r03-review-20261021/summary.json`、DB logは同directoryにあり、Gitで移送されない。これは今回再実行した結果ではなく、直前の点検結果の整理である。Windowsのclean test後にJARを再packageしていないため、次のbrowser実演には新しいB1同梱JARが必要。

### 9.2 実装対象と再利用境界

§5のroute／CSRF／DTO／error仕様と§6のbuild方式を実装入力にする。追加するのは次のReference／Tooling差分に限定する。

| 単位 | 対象path・作業 | 再利用・出口 |
| --- | --- | --- |
| B1-05 Session認証境界 | `identity/configuration/ReferenceSessionApiSecurityConfiguration.java`、`ReferenceSessionLogoutBoundary.java`、`ReferenceSessionApiProblemWriter.java`を追加。既存`ReferenceSecurityConfiguration.java`へSPA matcher・login成功時／MVC logout時のCSRF Cookie削除を接続 | order -10、Session NEVER、Bearer入口なし、no-store、実Cookie＋header照合。既存CookieSerializer・Framework logout customizer・Identityを利用 |
| B1-05 CSRF取得 | `identity/adapter/inbound/web/ReferenceSessionCsrfController.java`を追加 | 認証後204。tokenをJSONやlogに返さず、標準spa()によるmaterializeを確認 |
| B1-05 業務API | `expense/adapter/inbound/sessionapi/`へController、§5.5の10 DTO、mapper、業務Advice、厳密String deserializer／validationを追加 | `ExpenseReadService.findOwnRequests`／`findOwnRequest`／`findAvailableDepartments`／`findAvailableExpenseCategories`、`ExpenseApplicationService.createDraft`／`submit`を利用。Application・Domain・Repositoryは変更不要 |
| B1-06 配信入口 | `expense/configuration/ReferenceSpaWebConfiguration.java`追加、既存`HomeController.java`・`templates/home.html`更新 | resource存在時だけshellとlink。未知pathをindexへforwardしない。認証はidentity構成が担当 |
| B1-06 UI／build | `koiki-reference-app/frontend/`の§6.2入力、Reference `pom.xml`の`b1-session-spa` profile、`.gitignore` | Reactの一覧／詳細／draft作成／submit。UIの送信中・結果不明・拒否・競合・失効表示、再表示時の再取得。同じJARにbundleする |
| B1-05〜07 test／Tooling | §7.3の新規Reference test4 class＋Root外browser test1 class。既存Critical JourneyにB1 JARの選択・共用が必要なら最小変更 | package／class／pathを一致。既存fixture・resource上限を再利用し、production故障switchを追加しない |

`identity/configuration`等の略記は`koiki-reference-app/src/main/java/org/koikifw/reference/`配下を表す。新規Reference testは対応する`src/test/java/`の同package、browser testは`build-support/reference-e2e-verification/src/test/java/org/koikifw/buildsupport/referencee2e/`に置く。

新規Java runtime依存・migration・Framework Public API／property・Root Reactor・workflow変更は選定しない。npm依存とReference限定Maven pluginは§6.1の値を入力とし、導入時に存在・engine・peer・取得条件を再確認してlockを生成する。今回install済み・互換性実証済みにはしない。案件BFF・SSO・承認／会計／通知復旧のSPA化は追加しない。

### 9.3 検証集合と合流順序

| 実施段階 | 局所集合・観測 | 結果を広げない境界 |
| --- | --- | --- |
| B1-05 認証／CSRF | `ReferenceSessionApiSecurityTest`。Session／Bearer分離、双方credential、未認証GET 401・unsafeのCSRF先行403、権限拒否、実Cookie＋header成功と欠落／不一致、login更新、両logout、例外時503・local cleanup、二重filter登録なし | mock CSRF成功だけでCookie照合を代替しない。旧Cookie＋一致headerの任意再注入まで失効する保証は設けない |
| B1-05 DTO／error | `SessionExpenseApiControllerTest`。§5.5の10 DTO・paging・数値境界・strict JSON、400／404／409／422／503／500・安全な本文・Location | 既存Bearer long契約やglobal ObjectMapperを変更しない |
| B1-05 DB／Audit | `SessionExpenseApiPostgreSqlIntegrationTest`。SessionとMVC双方で同じ申請を参照しcreate／submitを突合。scope外／不存在・古いversion・無効master、更新・成功Audit・eventの非発生、永続Session失効 | 既存Applicationの正常／例外経路を利用。専用非同期profileは有効化しない |
| B1-05 既存直接回帰 | `ReferenceBusinessUrlSecurityTest`、`ReferenceSessionCookieConfigurationTest`、`ApiExceptionBoundarySecurityTest`、`ExpenseApiControllerTest`、`ExpenseApiPostgreSqlIntegrationTest`、`ExpenseReadModelPostgreSqlIntegrationTest`、`ExpenseMutationAuthorizationOrderTest`、`ExpenseApplicationServiceMethodSecurityTest`、`ReferenceArchitectureTest`、`RequiredNegativeArchitectureRulesTest` | 通常rootに含まれる集合を合流時に確認しただけなら、局所PASSとして二重計上しない。Application差分が不要な限りR-03全47件の反復を着手条件にしない |
| B1-06 配信／build | `ReferenceSpaWebConfigurationTest`。index有無・link・認証・asset・未知GET・no-store。§7.3の使い捨て入力によるbuild負例、型検査、通常JAR非同梱・B1 JAR内容検査 | 新規testは通常rootでnpm成果物を要求しない。Nodeはbuild時だけ使用 |
| B1-07 合流 | §6.4の順に、通常root `clean verify`→B1 profileのReference reactor `clean verify`→同じ同梱JARで新旧browser journey | 各buildのXML・hashを退避してから次のcleanへ進む。通常rootのPASSとB1 profileのPASSを区別 |
| B1-07 browser | 新規`PackagedReferenceSessionSpaJourneyTest`＋既存`PackagedReferenceCriticalJourneyTest`。実Cookie／CSRF、失効・logout・再login、MVC相互参照、再読込・戻る・表示破棄、DB／Audit／logとcleanup | JARのsource・profile・frontend入力／lock／asset hashを両testの起動前に照合。途中で再packageしない |

局所DB集合は1 classずつ直列実行し、終了／cleanup確認後に次へ進む。通常root／B1 profileで含まれる既存回帰を確認する際は、新XMLのclassと実行／skipを照合する。Root外Toolingが内部artifactを必要とする場合だけ現sourceからlocal installしてhashを記録し、固定済みB1 JARを上書きしない。

### 9.4 概算・実行条件・残る具体化

概算はbackend 4〜7時間、frontend／build 3〜5時間、統合検証・記録2〜4時間、受入整理1〜2時間の**計10〜18時間**を初期見積りとする。実施日・納期の約束や、今回許可された連続実行時間ではない。B1-05終了時に実差分・残作業から更新する。

| 項目 | B1へ渡す条件 |
| --- | --- |
| source／環境 | 実装開始時のbranch・HEAD・差分と実行環境を記録。Cloudは保存activationを確認、Windowsは既存JDK21・Wrapper・Dockerの実値を確認。別端末のcache／image／browser／raw記録を存在すると推定しない |
| 外部取得 | npm lock生成と初回npm ci、未cacheのplugin／宣言済み依存を対象別に識別する。proxy／CA／checksumを維持し、取得不能時にversionを無断で変更しない。Node／npm・依存lock・取得物を実行前記録へ残す |
| 新規・局所DB | DB同時1、PostgreSQL17・memory1 GiB／CPU1／max_connections16、pool最大4／idle1、test JVM768 MiB、Maven768 MiB、fork1・並列無効。既存fixtureの実効値assertを使用する |
| browser | application子JVM1・768 MiB／pool4・idle1、PostgreSQL1（上記制限）、Chromium1 context、issuer1、dynamic loopback port。新旧journeyは直列で各所有資源を終了。制限付き既存fixtureを新規journeyへ接続し、実効値を保存 |
| 検証時間案 | 局所集合60分、合流build＋browser60分、cleanup10分を別に予約する。失敗や上限到達時は当該集合を止め、原因・必要な訂正・残予算を整理する。自動root反復や旧S1予算の再利用はしない |
| 回数 | 通常root・B1 profile各1回、新旧browser各1回を初回合流の集合とする。修正が必要な場合は影響する局所集合を選定し、合流全体を無条件に繰り返さない |
| 停止／cleanup | 秘密・token露出、既存回帰、artifact不一致、資源／時間超過、権限不足、cleanup不成立では停止。権限要求は実行環境の承認手順へ従う。DB／role／PID／port／一時fileをrunに紐付け、他runを操作しない。終了直後に残存する場合は最大30秒の読取確認で状態を確定し、残存したまま後続へ進まない |
| 記録 | source SHA＋patch・file hash、環境、command、XML件数／hash、通常／B1 profile、JAR・lock・asset hash、DB／Audit／sanitized log、cleanupを保持。clean前に保存し、異なるepochの結果を付け替えない |

**B1-04時点のOPEN（B1-07の実数・供給方法・実行結果は§12で具体化済み）：** (1) §7.3の5新規classを実装する際のmethod名・parameterized invocation数、(2) 通常root／B1 profileの既存test contextを含むJVM・DB・接続総量と制限の供給方法、(3) 実行環境でのfrontend取得・lockとbrowserの利用可否。新規・局所DBの制限を通常root全件へ一律適用して既存test条件を変えてはならない。該当集合の実行前に本節へ実数・commandを追記する。これらを完了済みと偽って先行せず、一方で既存環境の全再構築や旧R-00〜03全再実行を新しい前提にしない。

B1-04時点の指示は設計との整合と実装計画の整理までとした。その後のOwnerによるB1-05開始指示と結果は§10へ記録した。必要な判断は既存計画に沿ってこの記録へ追記し、同内容の開始票や新しい包括Gateを増設しない。

### 9.5 B1-08へ渡す受入条件

DoD 4-8は同梱SPAのCookie Session認証・CSRF double-submit・失効／logout後の拒否を、DoD 4-9は同一JARのMVC／SPA併存・経路別CSRFと既存Bearer保護を実演して評価する。Evidenceと未達・利用条件を整理した後にOwnerが受入を判断する。局所PASSやB1-07の終了だけで受入済みとしない。B1完了後のpackage順は未確定で、B1-08で入力・必要性・検証可能性から次の一つを選ぶ。S1全体の未達・Customer BFF・MyBatis採用判断は別の状態として保持する。

## 10. B1-05：Reference backendの実装・局所結果（2026-10-21）

Ownerは、B1-04文書4件を未commitで保持したままB1-05へ進むことを確認し、「B1-05の実装へ進めましょう」と指示した。基点HEADは`e9ff13a`、branchは変更せず、文書commit・clean sourceの再固定を追加条件にしなかった。

§9.2のbackendをReference main新規21＋既存1 file、新規test3 fileとして作成した。標準spa()、Session API chain、CSRF取得、両logout・loginとの接続、専用10 DTO・mapper・業務Adviceを実装。既存Application／Domain／Repository・Framework・POM／依存・migration・workflowは変更していない。SPA matcher・assets認可は接続したが、shell・UI・home link・同梱profileはB1-06に残る。

[実装・検証記録](../architecture/validation/phase4-b1-05-session-api-backend-results-20261021.md)にsource識別・各class・途中失敗・訂正・資源・cleanupを整理した。新規認証22＋DTO40＋実DB10＝72件、既存77件の**計149件PASS、failure／error／skip 0**。同じmain sourceに対する局所結果であり、通常root全件・実JAR・browser・DoD全体の受入ではない。SameSiteはServlet Cookie属性で確認し、live Set-CookieとbrowserはB1-07で照合する。

test method／invocation数のOPENはB1-05分を解消した。B1-06／07の詳細と通常root全体資源・frontend取得条件は、該当作成／実行前に具体化する。次はB1-06。今回commit・push・PR作成は行っていない。


## 11. B1-06：frontend・build・配信入口（2026-10-21）

OwnerはB1-05 Cloud再検証の文書1 fileだけをcommitし、B1-06へ進むよう指示した。`00ce98a`へ文書だけを固定後、同じ`docs/phase4-review-integrated-plan-20261020`上で実装した。B1-06成果のcommit・push・PR作成は行っていない。

`frontend/`へ固定versionのReact／TypeScript・Vite、npm lockfileVersion3、型検査・asset／入力hash検査を作成。Node24.19.0／npm11.9.0と§6.1の依存を実在・engine／peer条件から再確認した。Referenceの専用`b1-session-spa` profileでnpm ci→型検査→bundle→検査→resource copy→repackageを実行する。通常profileはNode禁止条件でもpackageでき、SPAを含まない。

UIは一覧／paging、1明細のdraft作成、詳細／submit、既存MVCへのリンクとlogout。金額・version・件数は文字列を維持し、金額表示にBigIntを使用する。401・logout・ページ離脱で保持値を破棄し、GETをabort・世代番号で照合する。POST二重受付を抑止し、409は最新詳細を取得して人の再操作へ渡す。通信断・500／503では結果不明として入力を破棄し、自動再送しない。400／422は入力を保持し、validation項目を表示する。

`ReferenceSpaWebConfiguration`は同梱indexがある場合だけGET `/spa`・`/spa/`をforwardし、indexにno-storeを付ける。Homeは同じresource存在でlinkを表示。未知pathや不足assetをshellへforwardしない。B1-05のSecurity matcher・CSRF・Application Use Caseは変更していない。

[限定結果](../architecture/validation/phase4-b1-06-frontend-build-results-20261021.md)は新規配信2＋UI fixture5＋build負例6＋既存直接回帰52の計65件PASS。通常／専用profileを各clean package（skipTests）し、通常JARの非同梱、専用JARのindex／asset／静的配信外build記録、入力15 fileとasset hash一致を確認した。UI fixtureは実Chromiumとloopbackの代替APIを使い、実JAR＋実DBの接続成立を代替しない。

B1-07では通常root全件・B1 profileの全件verify、新規`PackagedReferenceSessionSpaJourneyTest`の実装と既存Critical Journeyを同じB1 JARで直列実行する。通常rootの全contextに対する資源制限、browser集合・時間・停止／cleanupを実行前に具体化する。今回の局所結果をDoD受入へ拡張しない。


## 12. B1-07：合流回帰・実JAR browser実演（2026-10-21）

OwnerのB1-07開始指示に基づき、branch `docs/phase4-review-integrated-plan-20261020`、B1-06成果commit `2b0ed5b`・clean treeを入力とする。Cloudはmemory上限8 GiB／CPU2、JDK21.0.12.1、Node24.19.0／npm11.9.0、保存済みChromium／PostgreSQL17 imageを使用する。remote・commit・PR操作は含めない。

### 12.1 実行前の資源・順序

- 通常root／B1 profile：Maven heap768 MiB、compiler fork1（既存設定）、test fork1／heap768 MiB／reuseForks=false、JUnit並列無効、reactor並列なし。通常DB専用selector `koiki.reference.normal-root.db-limits.enabled=true`でDB1 GiB／CPU1／max_connections16。既存pool既定最大10を変更せず、DB接続14以内をsampleする。専用verification／B2 flagは与えず、専用testの通常skipを維持する。DB同時1、class切替でRyuk最大2を見込む。JVMはMaven＋compilerまたはtestの最大3（compilerとtestの並列実行なし）。資源sampleと各集合後の残存確認を保存する。
- browser：Maven／test／application各heap768 MiB、DB1 GiB／CPU1／max_connections16、application pool4／idle1、管理接続を含め8以内。Chromium1 context・issuer1・application子JVM1。新旧classは別Maven invocationで直列、各終了後最大30秒のcleanup確認を行う。
- 合流build＋browserの実行時間60分、cleanup10分を予約。通常root1回→XML・通常JAR退避→B1 profile Reference reactor1回→XML・同梱JAR退避→同梱JAR hashを固定し新旧browser各1回。失敗は原因と影響集合を整理し、全体を自動反復しない。
- Root外Toolingに新規 `PackagedReferenceSessionSpaJourneyTest` を実装する。同梱JARのSHA-256・profile・frontend全入力／lock／asset hashを起動前に照合し、既存Critical Journeyも同じ照合を明示選択時だけ追加する。製品側の故障注入入口は追加しない。
- ローカルPCでの実ブラウザー手動確認はCloud自動実演と別に手順・確認表を用意する。push／他端末pullは必要時にOwnerと実施順を合わせ、未実施をPASSとしない。B1-08のDoD受入は別判断。

build共通引数：`-o -B -ntp -DargLine=-Xmx768m -DforkCount=1 -DreuseForks=false -Djunit.jupiter.execution.parallel.enabled=false -Dkoiki.reference.normal-root.db-limits.enabled=true`。通常rootは`clean verify`、同梱は`-pl koiki-reference-app -am -Pb1-session-spa clean verify`。各実行の時刻・command・sample・XMLを `/workspace/cloud-setup/logs/phase4-b1-07-20261021/` へ保存する。件数とbrowser method集合は作成・実行結果に追記する。


### 12.2 作成するbrowser集合

新規`crossesSessionSpaMvcBoundariesAndDiscardsExpiredViews` 1 method／1 invocation。artifact照合→匿名401／shell認証→実login／Cookie属性→SPAの大整数文字列での作成・text escape→詳細reload→CSRF欠落／誤値／MVC用token拒否・DTO400・業務422→MVC参照／別tab編集→SPA409と最新再取得→明示submit・連打1 POST→MVC／DB／Audit突合→戻る／進む・未知経路→実GET応答をbrowser側で保留してlogout／遅延応答除外→Cookie削除／旧Session拒否→別利用者scope拒否→DB expiryによる401／表示破棄→権限の異なる利用者で再login→未保存入力の履歴復元抑止→logout・log照合・cleanupを一本で確認する。内部checkpointはtest件数へ加算しない。DB fixture準備・expiry更新とbrowserの応答保留はTooling内だけに置く。

既存Critical Journey `crossesBearerAndSessionBoundariesAndReconcilesExternalEvidence` は1 invocation。Bearer作成／提出→MVC承認／HTMX検索→logout→Bearer再参照とDB／Audit／logを保持。B1明示選択時の起動前artifact照合と、既存helperのpackage内再利用だけを変更する。新規SPA classは明示selectorなしではskipする。

通常root初回は2026-10-21 09:45〜09:52 UTC、6分41秒でSUCCESS。fresh XMLは518件（419実行・99 skip）、failure／error0。通常JAR SHA-256 `7e5488e26cf89b95e88437b1bd20b948d85843d6dde2f9e78d6b91c4c5d14114`、SPA／build記録なし。XML・JARを`normal/`へ退避し、DB／Ryuk0を確認後に同梱profileへ進んだ。Root外Toolingのcompile準備だけを通常rootと並行した区間があり、その区間はMaven2＋compilerまたはtestの最大4 JVM、DBは通常rootの1だけ。cgroup memoryとDocker／接続sampleを保存した。


### 12.3 実演で判明したMVC日付形式の局所是正

初回新規journeyはstorage全体が空というfixtureの過剰な前提で停止した。実JAR内HTMX2.0.10は起動時に`sessionStorage`へ`htmx-current-path-for-history`を保存する。SPAがstorageを増やさず、業務データ／Cookieを保存しないことを検査する条件へ訂正した。

次の実演でMVC編集後のredirect待機が停止。入力validityを追加観測したところ、`usageDate`の`type=date`でvalueMissing=trueだった。Reference FormのLocalDate表示がlocale依存になり、HTML dateのISO値にならないため、空欄になって更新を送信できない。B1のSPA→MVC編集→SPA409照合に直接影響するため、`ExpenseDraftForm.usageDate`へSpring標準`@DateTimeFormat(iso=ISO.DATE)`を指定する最小訂正と、`ReferenceBusinessUrlSecurityTest`のen-US／ja-JP表示・POST binding回帰2 invocationを対象に追加する。

製品sourceが変わるため、初回の通常／同梱build成功は修正前の結果として保持する。追加テストで修正前再現→Form局所訂正後、通常rootと同梱profileを各1回だけ再検証し、新しい同梱JARへhashを固定して新旧journeyを実行する。これは判明した不具合に対する最終source確認であり、無変更のroot反復ではない。合流実行は初回開始09:45 UTCからの60分内、cleanup10分を別予約する条件を維持する。Framework／業務層／Security／migration／依存／frontendは変更しない。


MVC日付の修正前focused回帰はen-US／ja-JPの2件ともFAILを再現した。ISO値を出力・bindする要件の確認後にannotationを追加し、表示とPOSTの両方向を同じ2 invocationで検査する。新規browserのMVC編集は利用日をtest側で埋め直さず、初期表示値がそのまま有効であることを要求する。手動確認では、tab再表示の最新GETで競合が解消済みになる正常経路と、別windowを同時表示して409を起こす経路を分ける。


### 12.4 MVC既存明細の保存是正と最終sourceの再検証

日付訂正後の新規journeyでMVC編集POSTが409になった。`ExpensePostgreSqlIntegrationTest.editsExistingLineWithoutChangingItsIdentityAndAdvancesVersion` 1件を追加し、HTTPを外した実DB操作でも同じ明細IDを使うeditが`CONFLICT`になることを再現した。aggregateが既存IDの新しい明細instanceへ内容を置き換え、そのままflushする経路が原因である。`editDraft`の編集後に既存Repositoryの`save(request)`を通してから外側のflushへ進める最小訂正を行った。明細ID／件数・更新値・version+1・Audit追加なしの局所回帰はPASS。Domain model／Repository契約／認可・version照合の順序は変更しない。

製品変更はFormの日付指定とApplicationの編集時saveだけ。日付のみ訂正した通常／同梱構成421実行＋99 skipの成功は別epochで保持し、両訂正を含む最終sourceで通常root／同梱profileを各1回再確認する。既存回帰の不足が実演で判明した追加是正に伴い、合流build＋browserの見積りを開始09:45 UTCから**75分**へ更新し、cleanup10分の別予約とDB／JVM／接続・並列条件は維持する。旧S1予算の流用や無変更でのroot反復ではない。最終browserはこの最終同梱JARに固定し、日付のみのJARと結果を混ぜない。


### 12.5 最終結果とローカル手動確認への引継ぎ

[検証記録](../architecture/validation/phase4-b1-07-integrated-browser-results-20261021.md)に各artifact epoch、失敗再現・局所訂正、最終source7 file・command・XML・hash・資源・cleanupを記録した。両訂正を含む最終通常root／B1 profileは**各422件実行PASS＋99 skip**。Contract4＋Rules107＋Reference311で、skipをPASSへ数えない。MVC日付2 invocationと明細保存1件を既存classへ追加した。新旧実JAR browserは**各1件、計2件PASS／skip0**。内部checkpointや修正前の実行を重複計上しない。

最終同梱JAR SHA-256は`32e1c955c8458f052266c9ee0a0da59cdfa9e2d595e84dc0a46d26f8bf34e963`。新旧とも同じJAR・profile・frontend15入力／lock・3 assetを照合し、間で再packageせず終了後もhash不変。MVC編集→SPA409→最新取得／明示submit、実Cookie／CSRF、scope秘匿404・権限不足403、logout／旧Session拒否・遅延GET、JDBC失効→401、再login・history入力破棄、DB／Audit／検査済みlogを突合した。fixtureのstorage前提・scope応答・expiry更新の訂正も履歴に残した。

観測ではDB同時1／Ryuk最大2、memory最大2743 MiB、DB client最大13／最終browser最大4。Docker残存0、所有application8 PID停止と新旧test内のport／temp log／browser cleanupを確認した。Cloud実Chromiumのdesktop／mobile画面を目視したが、人の実操作やWindows buildは未実施。

[ローカルPC確認計画](phase4-b1-07-local-browser-check-plan-20261021.md)へ、成果固定・push後の同branch pull、Node／npm・同梱build、既存使い捨てdemo fixture、手動操作と記録／cleanupをまとめた。実施時期はOwnerと合わせる。B1-08のDoD受入・次package選定は未実施。今回の成果commit・push・PR作成は行っていない。
