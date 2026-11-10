# D3方式2：実装・有限検証条件の具体化（2026-11-09）

**Cloud引継ぎ：** Owner指示により、[段階別計画§11](phase4-d3-option2-staged-task-plan-20261109.md#11-cloud実装自動検証windows再現手動確認)にCloud実装・自動検証／Windows再現・手動確認、Linux build、開始前確認、証拠持帰りを追記し、今回の文書一式を本branchへcommit・pushする。下記のcommit／remote未実施・別判断表示は操作前の記録であり、今回の文書成果固定にはこの指示を適用する。Cloud／Windowsの実装・検証結果はまだない。

**後続Owner判断：** 本具体化案とDB整理、5段階で進める方針はOwner了解済み。[段階別タスク計画](phase4-d3-option2-staged-task-plan-20261109.md)で対象ファイル・作業順・検証・終了条件を具体化した。以後の実装順は同計画S1〜S5を使い、下記A〜Dは各段階内の進め方として読む。段階別検証の時間枠は同計画§8の案へ分割し、一括90分枠を並行して適用しない。実装・動的実行は未着手。

**Claude Code点検の反映：** 段階別計画§10に指摘1〜15の対応を記録した。Browser／Chromium版、Session／rotation設定、内部health観測、一時container helper・追加資源、段階別token寿命は同計画を優先する。以下の初版候補値はこの詳細化で補う。

## 1. 状態・基点・所有

Owner指示「同期済みmainを基点に、新しい作業branchで方式2の実装・検証条件を具体化する」に基づく計画初版。**条件具体化の文書作業中。実装開始・動的検証の承認記録ではない。** [4コンテナ環境案](phase4-d3-keycloak-sso-environment-20261105.md)を構成の入力とし、本書で初回の対象・判定・残る開始条件を整理する。

| 項目 | 今回の位置づけ |
| --- | --- |
| 基点 | local main／origin/main／GitHub mainが`cf77a3c58c9dd4a7460e965ec9abe224f668c083`で一致、作業開始時clean。GitHubは`git ls-remote`で読取照合 |
| 作業branch | `docs/d3-option2-implementation-conditions-20261109` |
| Phase／status | Phase 4 D3-01の方式2詳細化→D3-02の限定実装・local検証へ渡す計画。方式1の証拠を方式2のPASSとして使わない |
| 主Owner | Tooling：4サービス検証環境・Next.js BFF fixture・realm／seed・runner。Reference：既存Java API契約。Customer：実案件BFF／画面／IdP連携の採用判断 |
| 今回の変更 | 本書と関連文書の入口。application code／依存／Compose／CIは変更しない |
| 保持対象 | 方式1の手動project／DB／volume／secret／Evidenceは停止・再起動・seed・cleanupしない。既存`.tmp`の資格情報・実行予算を流用しない |

Javaは既存Reference、Java 21、PostgreSQL、Next.js BFF、Keycloakによる4実行サービス。Framework共通API・Starter・migration・公開済みFramework ZIPを変更する根拠は現時点でない。実企業IdP・本番BFF・ECS・TLS終端は初回対象外。

## 2. 静的照合したJava契約と初回の業務範囲

以下は基点sourceの確認であり、Keycloak実tokenによる成立結果ではない。

| source | 再利用する契約 |
| --- | --- |
| [API chain](../../koiki-reference-app/src/main/java/org/koikifw/reference/identity/configuration/ReferenceApiSecurityConfiguration.java) | `koiki.reference.api.bearer.enabled=true`で`/api/**`をstateless Bearerにする。issuer・audience・`token_use=access`を検証。CSRF無効は当該Bearer chain内だけ |
| [claim converter](../../koiki-reference-app/src/main/java/org/koikifw/reference/identity/configuration/ReferenceJwtAuthenticationConverter.java) | `koiki_user_id`で現在のACTIVE利用者を取得。文字列scopeの`expense.apply`と現在DBの`EXPENSE:APPLY`の積で権限を決める |
| [REST controller](../../koiki-reference-app/src/main/java/org/koikifw/reference/expense/adapter/inbound/api/ExpenseApiController.java) | `POST /api/v1/expense-requests`、本人の`GET /api/v1/expense-requests/{id}`、`POST /api/v1/expense-requests/{id}/submit`。既存Use Case・認可・Auditを再利用 |
| [probe chain](../../koiki-reference-app/src/main/java/org/koikifw/reference/identity/configuration/ReferenceContainerSecurityConfiguration.java) | `d3-container`限定のGET live／ready。その他経路を一括公開しない |

初回画面は申請者2名の「login・本人詳細・作成・提出・logout」。作成は既存DTOのdepartmentId／claimedAmount／lines、提出はexpectedVersionを送る。入力DTO・業務検証はJavaを正本とし、BFFは利用者IDを入力から受け取って代理認証しない。承認・差戻し・master管理・一覧API追加はこの初回へ混ぜない。

通常MVC／Session APIをBearerへ置き換えない。Java buildは`d3-container`を明示選択し、方式2は`b1-session-spa`を不要とする案。既存の方式1用build scriptは同梱SPAを前提とするため変更せず、専用手順でReference JARを新規生成・hash記録する。

## 3. BFFの実装候補と再現性

| 項目 | 初回の提案・固定条件 |
| --- | --- |
| frontend | Next.js App Router／TypeScript、Node runtime、standalone output。static export／Edge runtimeは採用しない |
| OIDC | `openid-client` v6系を第1候補。Code＋PKCE S256、confidential Client、state／nonce照合・ID Token検証をlibraryへ委ねる。独自JWT署名検証を作らない |
| Session | `pg`＋Node標準cryptoによるTooling専用PostgreSQL store。ORM／Redis／追加Sessionサービスを初回要件にしない |
| toolchain | Node `24.19.0`／npm `11.9.0`を既存端末手順とそろえる候補。Next.js・React・openid-client・pg・TypeScript・Playwright Nodeと対応Chromium revision／実Browser版を固定対象とする。lock作成時に取得可否・engines・peer依存・security情報を確認し、S3開始時にsecurity情報を再確認 |
| build | 専用package.json／package-lock.json、`npm ci`。lock作成と承認後の再現buildを区別。Maven／Node buildは順次実行。global PATH・既存frontend lockを変更しない |
| image | Java／Node／PG／Keycloakのtag＋digest・platform、source SHA／dirty差分hash、JAR／lock／image IDをmanifestへ記録。Keycloak `26.8.1`は既存案の候補で、取得・適合確認前に採用確定としない |
| runtime | frontend／backendは非root・read-onlyを候補、必要書込を専用tmpfsへ限定。Next.jsの実際の書込先を確認。PG／Keycloakはそれぞれ必要な永続／一時領域を明示 |

版・digestの空欄を残したままruntime検証へ進まない。今回はpackage／lock生成・依存取得・image pullを行っていない。公式資料に存在する版を、取得・互換性確認済みの採用版と読み替えない。

### 3.1 Browser境界

- `/a`と`/b`を同一BFF内の独立RPとする。client IDは`koiki-next-a`／`koiki-next-b`、callbackは`http://localhost:13000/a/auth/callback`／`/b/auth/callback`を正確に列挙。post-logout URIも同じ固定origin内に列挙する。
- Cookieは`D3_A_SESSION`／`D3_B_SESSION`、それぞれPath `/a`／`/b`、HttpOnly、SameSite=Lax、Domainなし。local HTTPだけSecure=false、HTTPS成立は別検証。Cookie pathは秘密保持の強い境界とみなさず、server側でclientとSessionの対応を必ず検証する。
- Session IDは暗号学的乱数32 byte以上のopaque値、DBにはhashだけを保存。login成功時に新規発行、認証前Sessionを昇格させない。idle 15分／absolute 60分、認証途中stateは5分を初回候補とし、testは時計注入で待機を短縮する。
- 認証開始前のbrowserとの結付けを専用一時Cookie＋DB transactionで保持。callbackはstate／nonce／PKCE／client／redirect URIを検証し、認証途中recordは一回だけ消費する。URLのHost／Forwardedからcallbackを組み立てない。戻り先は固定相対pathのallowlist。
- browser→BFF更新はPOST＋Sessionに結付けたCSRF token、固定Origin検査、JSON Content-Type・body上限64 KiBで保護する。login開始・local logout・SSO logoutもPOST＋CSRF。OIDC callbackのGETは一回限りのstate検証で保護する。
- BFF→Javaは固定backend URL＋固定3操作だけ。任意URL proxyを設けない。BrowserのAuthorization／Cookie／ForwardedをJavaへ転送せず、server側Access Tokenだけを付与。JavaのSet-CookieをBrowserへ中継しない。
- 認証済みSSR・RSC payload・API応答はprivate／no-store、Java fetchもno-store、tokenをClient Component propsへ渡さない。HTML／RSC／bundle／console／logを検査対象にする。

### 3.2 Session／token保存・refresh・logout

`koiki_bff`だけにmigration履歴、認証途中record、Sessionを置く。Sessionはclient ID、`session_id_hash`（BFFのSession ID hash）、issuer／subject、CSRF hash、期限、version、暗号化token列を持つ案。OIDCの`sid`とは別で、初回はIdP sid専用列を設けない。Access／Refresh／logout用ID TokenはAES-256-GCMで暗号化し、実行ごとの外部鍵と毎回異なるIVを使う。鍵・生token・code／verifierを証拠へ出さない。認証途中のverifier／nonceも保護し、期限後削除を有限batchで行う。

refreshは同じSessionのDB row lockで直列化し、lock取得後に期限を再読込して不要な二重交換を避ける。HTTP timeoutを設け、成功時だけtoken／versionを同一transactionで更新。接続消失など交換結果不明は自動再送せず当該Sessionを再認証必須にする。別要求が古いRefresh Tokenを使わないこと、logout済みSessionをrefresh完了で復活させないことを検証する。業務POSTは401／timeoutから自動再送しない。

local logoutは対象RPのSession削除・Cookie削除。SSO logoutは別操作で、CSRF確認後に当該Sessionを削除し、固定post-logout URI／stateを使うRP-Initiated Logoutへ進む。他RPの既存Sessionを即失効させるbackchannel logoutは初回実装対象外とし、他RPは残る可能性・次のrefresh失敗・再認証結果を記録する。発行済みJWTも即失効としない。Access Token寿命は段階別計画に合わせS2〜S4が300秒、S5が60秒。Java validatorの既定skew60秒は採用版で再確認し、S5はexp＋skew＋5秒（発行から約125秒）を予約して直接APIの拒否を観測する。BFF期限検査・refresh結果で代用しない。SSO／Client／BFF Sessionとrotationの設定表は段階別計画§4.1を正本とする。

## 4. issuer・realm・DB／fixture契約

canonical issuerは`http://idp.localhost:18080/realms/koiki-d3`。Keycloakの完全hostname、内部listen port、network aliasを既存案どおり一致させる。hostの名前解決だけでなく、実Browser・BFF・Javaからdiscoveryと実際のtoken／JWKS URLへ到達し、discoveryのissuerとJWTのissが一致することを開始時に確認する。hosts自動変更・issuer検証無効化では回避しない。

公開portはloopback `13000`／`18080`だけ、DB／backend／managementはhost非公開。HTTP許可は正確なlocal issuerと明示local設定の組合せに限定。`openid-client`のHTTP例外を他issuerへ適用しない。Javaのdiscovery／JWKSがHTTPで成立するかも実確認する。

| realm要素 | templateへ具体化する内容・負例 |
| --- | --- |
| Client A／B | standard flowのみ、confidential、PKCE S256必須。direct access grant／implicit／service accountは無効。secret・redirect・state／Sessionを分離 |
| audience mapper | `oidc-audience-mapper`候補でAccess Tokenに`koiki-reference-api`。ID TokenへAPI audienceを追加しない |
| token_use mapper | `oidc-hardcoded-claim-mapper`候補、String `access`、Access Tokenのみ。実tokenを検証しJSONの設定名の推測だけでPASSにしない |
| 利用者mapper | `oidc-usermodel-attribute-mapper`候補、管理属性`koiki_user_id`をString claimへ。user profileの本人編集を禁止し、ID入力のなりすましを防ぐ |
| scope | `expense.apply` client scopeをA／Bへ割当て、要求scopeとAccess Tokenの文字列scopeに現れることを照合 |
| 検証利用者 | ACTIVE＋権限あり2名、無効1名、ACTIVE＋権限なし1名。Keycloak subjectとKOIKI UUIDの対応を台帳化。利用者間の本人データ分離を検証 |
| 拒否token | 同realm内の専用負例Client／scope設定で誤audience・scope不足を発行。ID Tokenも負例。token署名を壊すだけで別の検証に代えない |

PostgreSQLは3 DB／3 runtime role、各DBのPUBLIC CONNECTを取り消し必要roleへ限定、他DBへの接続拒否を実際に確認する。初回は各DBのowner／migration／runtime roleを兼用し、Javaの起動時Flywayも同roleで動かす。runtime roleは非superuser・DB／role作成不可で、PostgreSQL初期化管理credentialは渡さない。権限は自DB内に閉じ、実案件でのDDL／runtime分離は検討事項として残す。Keycloak自身のschema管理とKOIKI→Reference、BFF migrationを混在させない。

seedは新規project専用、空DB／対象UUID不在を条件としたtransaction。業務利用者・部門・category・権限を作り、2利用者分の有効UUIDをMVCのversion／variant条件とも整合させる。方式1のseed／既存volumeへ投入しない。再起動時にseedやrealm importを更新処理として再実行しない。realm更新は新規projectで検証し、既存realmを黙って置き換えない。

## 5. 対象ファイル案と段階

新規資材は`build-support/container-sso-verification/`へ閉じる。以下は配置案で、作成済みのファイルではない。

| 単位 | 予定ファイル・責務 |
| --- | --- |
| 起動・識別 | `README.md`、`compose.yaml`、`Dockerfile.backend`、`Dockerfile.frontend`、`.dockerignore`、`versions.json`（固定版／digest） |
| 初期化 | `realm-template.json`、`init-db.sh`、`bff-migration.sql`、`reference-seed.sql`。secretはplaceholder、生成後はignored領域だけ |
| runner | `prepare.py`、`build-reference.py`とLinux／Windowsの`.sh`／`.ps1`入口、`preflight.py`、`verify.py`、`cleanup.py`、`export-evidence.py`。workdir／project所有物台帳、timeout、redaction、証拠保存 |
| Next.js | `frontend/package.json`／lock、tsconfig／Next設定、`app/[rp]`の画面・auth／固定API routes、`app/health`、`lib`のconfig／OIDC／store／CSRF／API client |
| 検証 | `frontend/test`の境界test、`browser`の実IdP journey、結果JSON schema／summary。実装単位に応じてファイルを分け、開始前に正確なmanifestを確定 |
| 文書 | 段階別計画で初版の一括結果名を置き換え、`docs/architecture/validation/phase4-d3-option2-s<N>-results-<実行日>.md`を各段階に作成。S5で総合索引、本書に判断記録 |

Reference main／POMの変更は初回計画0件。専用configの外部供給でBearer＋既存probeを有効化する。これで成立しない場合は理由・最小差分・直接回帰を提示し、Framework／Reference一般修正へ拡張する前に判断する。

段階は、(A)本計画review・版とファイルmanifest固定、(B)Tooling実装・静的／単体確認、(C)新規projectでbuild・preflight・KS検証、(D)結果・cleanup照合・Owner結果判断。手動環境の起動保持はCの自動延長にせず別のOwner指示で扱う。

## 6. 初回の有限検証集合

既存KS01〜10の10群を維持し、次のcheckpointを対応付ける。群数をtest件数と呼ばず、runnerでは各checkpointの期待HTTP／DB／Audit／終了状態を別に記録する。

| 群 | 初回checkpointと合格条件 |
| --- | --- |
| KS01 | source／JAR／lock／image一致、4サービスと公開port、3 DB間の接続拒否・runtime非管理権限。秘密欠落負例はBFF暗号鍵欠落の1構成で起動拒否 |
| KS02 | Browser／BFF／Javaのcanonical到達、discovery／JWKSと実claim、初回migration／realm／seed完了。ready後だけjourney開始 |
| KS03 | Aへ実BrowserのCode＋PKCE login、申請作成201→本人詳細200→expectedVersion付き提出204。DB状態・明細合計・利用者／成功Auditを照合 |
| KS04 | 同一contextでB Sessionなし→`prompt=none`→資格情報再入力なし→B独立Session。新規contextの同操作は`login_required`等でSessionを作らない |
| KS05 | 誤audience401、ID Token401、scope不足403、KOIKI無効利用者401、現在権限なし403。他人の詳細404。拒否で業務値・成功Auditが増えない |
| KS06 | 2利用者のSSR／RSC／API／Cookie分離、CSRF欠落・誤値・異Originを403、state不一致／callback再利用拒否、Aの認証途中情報をBへ渡して拒否。HTML／bundle／redacted logへtokenなし |
| KS07 | 同一Session同時2要求のrefresh交換1回と後続整合、結果不明時再認証、refreshとlogout競合でSession復活なし。local logout後BとIdPの存否、SSO logout後の新規SSO失敗・残るB Session、JWT期限＋実効skew後401を区別 |
| KS08 | IdP停止時のlogin／refresh失敗と既存JWTの扱い、backend停止時のBFF 503または上限内504、DB停止時のready失敗・live分離。各回復後ready、更新再送・container restart増加なし |
| KS09 | Node／Java／KeycloakのSIGTERM終了時間・強制終了有無、DB同volumeで再起動後の業務／Audit／BFF Session・realm保持。鍵も保持し復号可否を確認。再seed・migration重複なし |
| KS10 | 今回所有物だけcleanup、残留・image／secret／Evidence保持方針の照合。判定不能やcleanup失敗はHOLD、全群成功へ丸めない |

BFF単体確認はCSRF／Origin、client分離、state一回消費、Session期限、refresh競合／結果不明、logout競合、固定proxy先・body上限・秘密redactionを対象にする。実際に発生しにくいtoken交換結果不明はtest内の有限stubで検証し、実IdP結果と区別する。

Java直接回帰は`ReferenceApiJwtDecoderTest`、`ReferenceJwtAuthenticationConverterTest`、`ExpenseApiControllerTest`、`ApiExceptionBoundarySecurityTest`、`ReferenceContainerSecurityConfigurationTest`を選択候補とする。Session／browser chainを触る必要が出た場合だけ`ReferenceSessionApiSecurityTest`／`ReferenceBusinessUrlSecurityTest`を追加判断。実行コマンド・展開された件数・skipを開始前manifestに固定する。通常root全件・方式1の7 checkpoint・S1専用suiteを一律再実行しない。

## 7. 実行資源・時間・停止／cleanup案

| 対象 | 初回上限案 |
| --- | --- |
| runtime | backend 1.5 GiB／1 CPU（heap768 MiB）、Keycloak 1.5 GiB／1 CPU、BFF 1 GiB／1 CPU、PG 1 GiB／1 CPU。runtime計5 GiB／4 CPU |
| DB接続 | PG max_connections=32候補。backend最大4、BFF最大4、Keycloak最大4、初期化／観測最大2＝14を見込み、最小pool・起動migrationも実値確認。旧24案を無条件適用しない |
| host側 | Browser worker1／context最大3、runner1。build時Maven JVM1・Node build1を順次、各heap最大2 GiB候補。runtime／browserとbuildを並走させない。保持中サービス分とOS余裕を差し引いて空き不足なら開始しない |
| build枠 | 新規60分（版／lock／取得15、Java20、frontend／image25）候補。依存取得失敗・上限到達時に停止し、runtime枠へ繰越さない |
| runtime枠 | 新規90分：preflight10、初期化15、正常／SSO20、負例／refresh20、障害／再起動15、cleanup10。既存案の70分は詳細化前の候補として残す |
| 通信・終了 | OIDC／Java外向要求上限5秒、DB lock待機5秒候補。Node／Java／Keycloak stop猶予各40秒、PG30秒、全体cleanup10分。強制killは正常drainと区別 |
| 試行 | 各build集合・直接回帰集合・KS群は初回1回。失敗の診断を保存し、必要な訂正／再実行の対象を示す。自動無限retry／業務再送を作らない |

上記はOwner review用の提案値で、実行許可・環境能力の測定値ではない。project名は段階別計画で初版の一括名を置き換え、`d3-sso-s<段階>-<実行日>-<一意suffix>`のproject／volume／network／ignored workdirを新設し、台帳に絶対path・Compose label・IDを記録する。一時helper／probeの追加資源（最大512 MiB／0.5 CPU、同時1個）は段階別計画§8に計上する。

停止条件は、版／issuer不一致、意図しない既存資源参照、秘密露出、誤利用者更新、OOM／上限超過、原因不明の更新結果、cleanup不能。秘密露出時は生値を再掲せず当該使い捨て資格情報を破棄・再生成判断へ戻す。DB不明状態で業務要求を再送しない。

cleanupは台帳と一致する当該projectのcontainer／network／volumeだけ。`docker system prune`や全体削除は使わない。secret削除前にcontainer停止・所有確認を行い、再起動checkpoint中はDB volume／鍵を保持する。最終時は生成secret削除、imageはlocal保持、redacted Evidence／manifestはignored領域へ保持する案。cleanup未完了なら残留IDと手動復帰点を記録する。

## 8. 実装開始前の確定事項と今回の到達点

| ID | 必要な入力／判定 | 現在 |
| --- | --- | --- |
| O1 | 初回3操作・独立2 Client・local HTTP・logout保証限界・KS集合のOwner判断 | 本具体化案・DB整理はOwner了解済み。段階別の実装開始は別判断 |
| O2 | 採用package全版／lock、4 image digest／platform、脆弱性・互換性確認 | 未取得・未固定。取得／lock作成を含むB段階で確定しC前に照合 |
| O3 | 対象file manifest・直接回帰コマンド／件数、資源／時間・実行環境操作範囲 | §5〜7の案。開始前に実値・正確な集合を確定 |
| O4 | Docker context／空き資源／port、実BrowserとcontainerのDNS／HTTP到達 | 未実行。read-only診断と新規環境のprobeを分ける |
| O5 | 計画source固定・実装開始指示、実行前のsource／secret／所有台帳確認 | 今回は文書差分のみ。commit／remote・実装／動的実行は未実施 |

具体化初版作成時の確認はmain SHA照合、既存Java／方式1資材の静的読取、公式資料照合、文書差分／リンク検査。KS PASS・D3-01完了・方式2実装済みとはしない。後続Owner指示で段階別計画まで具体化し、次は同計画のS1開始条件・対象／上限を確認して開始判断へ進む。結果Owner受入は実行結果を読める状態にしてから別に記録する。

E1 V09限定技術PASS／V10未完了、S1未達・人手HOLD、C2 H07、D1結果受入未記録、正式support・全DoD／Phase4全体の未完了は維持する。

## 9. 公式資料と照合範囲

2026-11-09にContext7／公式ページを参照。以下は設計根拠であり、採用版の実動証拠ではない。

- [Keycloak hostname](https://www.keycloak.org/server/hostname)：公開hostname／内部経路を分ける際のissuer契約。初回は共通hostname方式を採用候補とする。
- [Keycloak 26.8.1 audience](https://github.com/keycloak/keycloak/blob/26.8.1/docs/documentation/server_admin/topics/clients/oidc/con-audience.adoc)、[protocol mapper](https://github.com/keycloak/keycloak/blob/26.8.1/docs/guides/admin-api/protocol-mappers.adoc)：audience／管理属性mapping。templateと実tokenの適合は後段。
- [openid-client Code flow](https://github.com/panva/openid-client)、[HTTP例外](https://github.com/panva/openid-client/blob/main/docs/functions/allowInsecureRequests.md)：Code＋PKCE／state、local専用HTTP許可。main資料と採用v6 patchのAPI差は版固定時に再確認。
- [Next.js v16.2.9 self-hosting](https://github.com/vercel/next.js/blob/v16.2.9/docs/01-app/02-guides/self-hosting.mdx)、[environment variables](https://github.com/vercel/next.js/blob/v16.2.9/docs/01-app/02-guides/environment-variables.mdx)：server側設定と認証済み応答cache。16.2.9は照合した資料の版で、採用版確定ではない。
