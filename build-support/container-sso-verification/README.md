# D3方式2：S1専用DB・Java基盤／S2実IdP→API

**Windows手動環境（2026-11-10）：** [操作順・保持条件](../../docs/architecture/validation/phase4-d3-option2-windows-manual-20261110.md)を参照。`d3-sso-s5-20261110-manual01`を稼働保持中。通常の`verify-s5.py`は障害注入とcleanupを行うため手動環境へ実行しない。新規S5 workの手動起動だけは`PYTHONUTF8=1`で`start-manual.py --work-dir <fresh-work>`を使う。image build後にpreflight、migration／seed各初回、4サービスのready確認を行い、環境と鍵を保持する。既存workへの再実行は拒否する。停止・cleanup・再seedはOwner指示に従う。

**最新結果：Windows win06のS5-V01〜06限定技術PASS（2026-11-10）。** [Windows記録§13.1](../../docs/architecture/validation/phase4-d3-option2-windows-reproduction-20261109.md#131-win06windows-s5限定技術pass2026-11-10)を参照。冒頭のS1記述は段階導入時の履歴で、後続のS2〜S5節を併せて読む。

Tooling-ownedの限定検証。S1ではPostgreSQLと既存Reference backendだけを起動する。
Keycloak／Next.jsサービス・OIDC／BFF画面・認証処理はまだ実装しない。
[段階別計画](../../docs/development/phase4-d3-option2-staged-task-plan-20261109.md)に従い、S1結果後に停止する。

Windows修正後のLinux回帰確認は[Cloud再検証引継ぎ](../../docs/development/phase4-d3-option2-cloud-harness-reverification-handoff-20261110.md)を入口とする。旧Cloud PASSと修正後harnessの結果を分けて記録する。

## 固定版と境界

`versions.json`がpackage／4 imageのtag＋index digest／amd64 digestとBrowserの識別情報を持つ。
`frontend/package-lock.json`は後段用の依存lockであり、BFFの実装・Next build成立を意味しない。
候補Next16.2.9のaudit結果を受け、採用を16.4.5へ変更した。S3開始時はsecurity情報を再確認する。
Node24.19.0／npm11.9.0、JDK21、Maven Wrapper3.9.16を使用する。
imageのmetadata取得とpackage auditを、image全OS packageの脆弱性scanとは区別する。

既存Reference main／POM・Framework／migration・Root Reactor／CI・方式1資材は変更しない。
backendの`d3-container`だけ有効にし、Bearerは無効。SPA／async JDBCはJAR検査で拒否する。
host公開portはS1では0。probeはbackend内のbash TCPで確認し、管理endpointを追加しない。

## build・新規work

Repository rootで実行する。現在のproxy／CAを維持し、Maven settings／toolchains・cacheの指定は環境に合わせる。
CloudではJava truststoreとNodeのCAをそのセッションから供給する。Dockerfileは依存取得をせずJARを抽出する。
Windowsでは`.ps1`、Linuxでは`bash build-reference.sh`を利用できる。

```sh
python3 build-support/container-sso-verification/build-reference.py \
  --output .tmp/d3-sso-s1-build-UNIQUE \
  --settings /absolute/path/settings.xml --toolchains /absolute/path/toolchains.xml

python3 build-support/container-sso-verification/prepare.py \
  --jar koiki-reference-app/target/koiki-reference-app-0.1.0-SNAPSHOT.jar \
  --work-dir .tmp/d3-sso-s1-YYYYMMDD-UNIQUE --project d3-sso-s1-YYYYMMDD-UNIQUE

docker compose --env-file .tmp/d3-sso-s1-YYYYMMDD-UNIQUE/compose.env \
  -f .tmp/d3-sso-s1-YYYYMMDD-UNIQUE/recipe/compose.yaml build backend
python3 build-support/container-sso-verification/verify.py \
  --work-dir .tmp/d3-sso-s1-YYYYMMDD-UNIQUE
```

上記project suffixは小文字英数字。workはrepoの`.tmp`直下、新規専用名に限る。
prepareがrecipe snapshot・全Tooling source hash・JAR／lock hash・所有名台帳・secretを記録する。
生成secret値はenv／CLI引数へ展開せず、secret fileをmountする。hostのwork／secret directoryは0700、
fileは異なるcontainer UIDが読める0444。local Composeのbind mount制約であり本番secret配布ではない。
既存work／projectを使う再開runnerではなく、preflightが既存資源を拒否してからcleanupをarmする。

直接回帰は既存`ReferenceContainerSecurityConfigurationTest`2件だけ。新規Tooling guardは`test_s1.py`5件。
S1の5群はtest件数ではない。S1-V02には他DB接続拒否6組と自DB内のrollback付きDDL／DML3組を含む。
build／取得60分、動的40分（実行30分・cleanup予約10分）。backend1536MiB／1CPU、DB1024MiB／1CPU、
PG最大32接続、backend pool最大4／最小1。buildとruntimeは並走しない。
失敗時は同workに結果・redacted logを保持してcleanupし、自動再試行しない。

## 3 DBとBFF DDL

| DB／同名role | S1の内容 |
| --- | --- |
| koiki_app | 既存KOIKI3・Reference4 migration、検証利用者4名・権限・master seed |
| koiki_bff | `bff_schema_history`／`bff_auth_transaction`／`bff_session` |
| koiki_keycloak | DB／roleのみ。製品tableなし。S2でKeycloak自身が初期化 |

全roleはDB owner／migration／runtime兼用、NOSUPERUSER／NOCREATEDB／NOCREATEROLE／NOREPLICATION／NOBYPASSRLS。
3 DBともPUBLICの権限を取り消し、自DBへのCONNECTを明示付与する。他2 DBへの接続を実credentialで拒否確認する。
PostgreSQL管理secretはDB初期化だけへ渡し、backendにはapp secretとHMACだけを渡す。
seedは空DB条件をtransaction内で確認し、再起動では適用しない。利用者UUIDはRFC形式のversion4／variant適合。

BFF DDLは`bff-migration.sql`、history作成・適用は`migrate-bff.py`。
transaction内でadvisory lockとlock待ち5秒／statement30秒を設定し、checksum不一致をDDL前に拒否する。
同じchecksumの再適用ではhistoryの適用時刻も保持する。DDL排他は実装済みで、S1は並行migration競合試験を含めない。
auth transactionはclient／state hashとbrowser binding hash、暗号化nonce／PKCE用payload、期限・消費時刻。
Sessionはclient／Session ID hash、issuer／subject、CSRF hash、idle／absolute期限、version、暗号化tokenとkey ID／IV／tag。
平文token／nonce／PKCEの専用列はなく、AES-256-GCMとIV12byte／tag16byteの制約を持つ。
client複合PK・期限indexを設け、IdP sid・業務データ複製・DB間FKは追加しない。
実際の暗号化・一回消費・Session期限・refreshの動作はS3以降の実装／検証であり、DDLだけで成立扱いしない。

## cleanupとexport

verifyは成功・失敗を問わず今回projectのログをsecret値でredactし、台帳と一致するcontainer／volume／networkだけを削除する。
残留0を確認した後に生成secretを削除し、imageとredacted Evidenceを保持する。
cleanupだけ必要な場合は同workの`execution.json`と所有台帳を照合する`cleanup.py --work-dir ...`を使用する。
全体prune・他project／旧方式1のvolume削除は行わない。

```sh
python3 build-support/container-sso-verification/export-evidence.py \
  --work-dir .tmp/d3-sso-s1-YYYYMMDD-UNIQUE --output-dir .tmp/d3-sso-exports
```

exportはmanifest／execution／results／cleanup／runtime-redacted.logだけをallowlistで抽出し、
JWT／Authorization／Set-Cookie／秘密鍵の残留を拒否する。生成secretの削除前にはexportしない。
JAR・env・realm・DB dump・HAR・trace・storageState・node_modulesをarchiveへ含めない。
archive内の各file checksumとarchive自体のSHA-256を出力する。
実行時にredacted logの内容を点検し、export済みとWindows取得・checksum照合済みを区別する。
Cloudのdownload経路が未確認ならOPENのまま記録し、workspace内に存在するだけで持帰り済みにしない。

## 実案件への示唆

DB owner兼用は検証用の簡略化。案件ではmigration主体とruntime roleの分離を決める。
BFFの鍵管理・暗号化tokenの保持期間・DDL運用はCustomer-ownedとし、本fixtureを正式配布物へ自動昇格させない。
このCloudの実効CPUは4。S1の2CPUは上限内だが、後段の最大4.5CPU構成は開始判断で見直す。

## S2：実KeycloakとJava API

S2はS1の固定版／DDL／seedを再利用し、新しいprojectへKeycloakを追加する。
`compose-s2.yaml`はbase Composeへのoverride。常設はDB／backend／Keycloakの3サービスで、
一時Node helperを`helper` profileで起動する。BFF／Client B／SSOはまだ作らない。
backend／DB／managementはhost非公開、Keycloak18080とhelper13000だけloopbackに公開する。
runtimeは3 CPU／4 GiB、helperは最大0.5 CPU／512 MiB・同時1個。host Chromiumはworker1／同時context1。

```sh
python3 build-support/container-sso-verification/build-reference.py --stage S2 \
  --output .tmp/d3-sso-s2-build-UNIQUE --settings /absolute/path/settings.xml
python3 build-support/container-sso-verification/prepare.py --stage S2 \
  --jar koiki-reference-app/target/koiki-reference-app-0.1.0-SNAPSHOT.jar \
  --work-dir .tmp/d3-sso-s2-YYYYMMDD-UNIQUE --project d3-sso-s2-YYYYMMDD-UNIQUE
docker compose --env-file .tmp/d3-sso-s2-YYYYMMDD-UNIQUE/compose.env \
  -f .tmp/d3-sso-s2-YYYYMMDD-UNIQUE/recipe/compose.yaml \
  -f .tmp/d3-sso-s2-YYYYMMDD-UNIQUE/recipe/compose-s2.yaml build backend
python3 build-support/container-sso-verification/verify-s2.py \
  --work-dir .tmp/d3-sso-s2-YYYYMMDD-UNIQUE
```

直接回帰は計画の4 class／23件を選択し、build側で件数・failure／error／skipを検査する。
helper用のopenid-clientと純JS依存は専用lockで取得した`frontend/node_modules`からclosureをsnapshotへコピーし、
各file hashを記録する。container内でnpm取得はしない。S1用`prepare.py`の既定stageは維持する。
Linux／Windowsのhost依存取得は`npm ci --ignore-scripts`、Browserは同版Playwrightの`install chromium`で準備する。
Browser binaryは`PLAYWRIGHT_BROWSERS_PATH`等でそのOSのものを指定し、他OSからコピーしない。

`realm-template.json`はsecretなし。prepareが新規client secret／login passwordを生成し、
生成realmをwork内だけへ保存する。管理属性`koiki_user_id`はadminのみ編集可能で、実DBのprofile設定も照合する。
`ADMIN_EDIT`は採用版の有効なunmanaged attribute policy。独立負例Clientは誤audience用で、S4のClient Bとは異なる。
4利用者は全員IdPでは有効、KOIKI側の無効1名・権限なし1名をJavaで拒否する。

helperの`verify-oidc.ts`と共通`frontend/lib/oidc.ts`は実Code＋PKCE S256／state／nonceを使用する。
HTTP例外は正確なlocal issuer `http://idp.localhost:18080/realms/koiki-d3`だけに限定する。
local HTTPではTLSでIdPを認証できないため、library標準の`enableNonRepudiationChecks`を明示し、
ID TokenのJWS署名をJWKSで検証する。issuer／audience／nonce／時刻・PKCE検証もlibraryへ委ねる。
Access Token署名とissuer／API audience／token_use／現在利用者・権限の検証は既存Javaが行う。
claimのdecodeは許可fieldの観測だけで、独自の署名検証は実装しない。

tokenはhelperメモリ内に保持し、Browser／HTTP結果／logへ返さない。固定の3業務操作と6拒否条件だけを扱い、
汎用proxy・正式BFFとして使わない。業務POSTの実行guardを先に立て、結果不明時に同じPOSTを再送しない。
`browser-s2.mjs`は6条件を独立contextで順次loginし、Cookieの属性と送信名だけを記録する。
Cookie値・token・code付きURL・HAR／trace／storageStateは保存しない。
helper終了前に保持token／未完認証recordを破棄し、exit0を確認する。cleanupは生成realmも削除する。

S2結果は[Cloud実証記録](../../docs/architecture/validation/phase4-d3-option2-s2-results-20261109.md)を参照する。
初回設定不備・観測SQL不備、途中の機能PASSと最終の署名検証・Cookie観測を含むPASSは別workで保持する。
exportは共通allowlistに値を含まない`browser.json`だけを追加する。Windows持帰り未確認の扱いはS1と同じ。
S2のbuild／取得枠20分、動的45分（cleanup10分予約）。段階終了で停止し、S3はOwner判断を待つ。

## S3：Client AのBFF縦断

Next.js App Router／Node runtimeのstandalone imageを追加した。S3ではRP `a`だけ許可する。
S2の同じJava JAR／imageを再利用し、Java API／Frameworkは変更しない。
`frontend/lib`がserver側の設定・暗号化・store・CSRF・固定API先を持ち、Client Componentには
本人表示用subject・CSRF・安全な申請DTOだけを渡す。Access／Refresh／ID Tokenは渡さない。

```sh
# frontendで指定Node/npmを使用し、security情報を確認してから実行する
cd build-support/container-sso-verification/frontend
npm audit
node --experimental-strip-types --test test/*.test.mjs
NODE_OPTIONS=--max-old-space-size=2048 NEXT_TELEMETRY_DISABLED=1 node node_modules/next/dist/bin/next build
```

Repository rootへ戻り、`prepare.py --stage S3 --jar ... --work-dir .tmp/d3-sso-s3-YYYYMMDD-UNIQUE --project d3-sso-s3-YYYYMMDD-UNIQUE`で新規workを作る。
prepareはhost buildの`.next/standalone`／staticを専用contextへコピーし、全fileのhashからfrontend識別を作る。
base／`compose-s2.yaml`／`compose-s3.yaml`をこの順に指定して`build frontend`を実行する。
backend imageは対応する元JAR hash labelを照合して専用projectの`:local`へtagするか、同じDockerfileで生成する。
実行は`python3 build-support/container-sso-verification/verify-s3.py --work-dir ...`。
生成されたcontextだけをDockerへ渡し、buildへrealm／secret／host cacheを含めない。

認証開始・logoutはJSON POST＋固定Origin＋CSRF、callbackだけGET＋一回消費stateで保護する。
認証前のHttpOnly CookieとDB transactionをhashで結び、nonce／PKCE情報をAES-256-GCMで保護する。
transactionを消費してcommitしてからtoken交換し、結果不明の交換は再送しない。
Sessionは新規256-bit ID、DBはhashだけを保存。tokenはclient／Session hashをAADに使って暗号化する。
Cookieは`D3_A_SESSION`、Path `/a`、HttpOnly／SameSite=Lax、Domainなし、local HTTPだけSecure=false。
idle15分／absolute60分、Access Token期限は`expires_in−5秒`の保守的な時刻で拒否し、S3ではrefreshしない。
有効Session取得時にidle／last_seen／versionを更新し、absoluteは延長しない。
期限切れauth transaction／Sessionの削除はlogin開始時に各最大100件の有限batchとする。

Browser→BFFはJSON・64KiB上限とOrigin／Session結付けCSRFを検査する。上流は固定3操作だけで、
BrowserのCookie／Authorization／Forwardedや上流Set-Cookieを中継しない。SSR／RSC／APIはno-store。
`startup.mjs`は暗号鍵欠落／不正時にNext起動前に終了する。非root／read-only、必要書込はtmpfsだけ。

CPU実効4のため、helper probeはfrontend未起動時に実行し、終了・削除してから常設4サービスを起動する。
通常runtimeは最大5GiB／4CPU、probe時はfrontendなしの4.5GiB／3.5CPUで、5サービスを同時起動しない。
Browserはhost側1 worker／1 context。最大負荷やCPU余裕を保証する試験ではない。
鍵欠落負例は依存サービス起動前に`compose-key-missing.yaml`でBFFだけを実行し、exit1と終了を確認する。

S3 runnerはBrowserでログイン・作成・提出・reload・logoutを行い、別Session1件のidle期限だけを
test所有のDB更新で過去へ移して拒否・削除を確認する。実アプリの時計や故障注入endpointは追加しない。
callback URL／Cookie値はBrowserプロセス内だけに保持し、証拠はstatus／属性／hashのみ。
Browser失敗時の出力も固定文言にし、provider URL／入力／資格情報を出力しない。

[S3結果](../../docs/architecture/validation/phase4-d3-option2-s3-results-20261109.md)を参照。
S3のbuild／依存枠40分、動的50分（cleanup10分予約）。S4／refreshは別段階とする。

## S4：独立2 Client SSO／logout

S3固定成果からOwnerのS4開始判断後に使用する。`compose-s4.yaml`でRP `a,b`とClient B専用secretを有効にする。
`prepare.py --stage S4`以外では生成realmからBを除外する。A／BでSession・認証途中・logout Cookie名とPath、
DB client_id／検索、暗号化AADとCSRF用途を分ける。同じOrigin／Next process内の検証であり、Cookie Pathを強い隔離境界とはしない。

frontendの同じ単体test／Next buildを実行後、Repository rootから新規workを準備する。

```sh
python3 build-support/container-sso-verification/prepare.py --stage S4 \
  --jar koiki-reference-app/target/koiki-reference-app-0.1.0-SNAPSHOT.jar \
  --work-dir .tmp/d3-sso-s4-YYYYMMDD-UNIQUE --project d3-sso-s4-YYYYMMDD-UNIQUE
```

S3と同様に元JAR hash labelを照合してbackendをproject専用`:local`へtagし、
base→`compose-s2.yaml`→`compose-s3.yaml`→`compose-s4.yaml`のsnapshotと専用`compose.env`で`build frontend`を実行する。
続いて以下を実行する。失敗後はworkを再利用しない。

```sh
python3 build-support/container-sso-verification/verify-s4.py --work-dir .tmp/d3-sso-s4-YYYYMMDD-UNIQUE
python3 build-support/container-sso-verification/export-evidence.py \
  --work-dir .tmp/d3-sso-s4-YYYYMMDD-UNIQUE --output-dir .tmp/d3-sso-exports
```

runnerは常設4サービスと非並走のhelper probe、DB seed／migration、実ChromiumのSSO集合、DB client結付け、
cleanupを担当する。Browserはhost1 worker／同時context最大2、build25分・動的35分（cleanup10分予約）。
Browserとrunnerがhashだけのfileで同期し、A／Bの独立Sessionをlogout前にDB照合する。業務作成／更新は行わない。

Aを対話login→Bが匿名であることを確認→`prompt=none`でBを認証→新規contextの同要求は`login_required`、
Aの途中state／bindingをBへ渡して拒否、Cookie値・CSRFのclient混同拒否を検証する。
A local logout後はB／IdPの存続とAの再SSOを確認する。RP-Initiated Logoutはclient_id・固定復帰URI・一回消費stateを使い、
ID TokenをBrowserへ渡さずKeycloakの確認画面を操作する。state不正・client／用途混同・復帰再利用、任意戻り先入力とIdPの未登録URIを拒否する。
logout後の新規SSO失敗と、Bの既存Session／発行済みJWTの利用可否を個別に観測し、即時失効を約束しない。

`.localhost`はChromiumとhost Nodeの名前解決が異なる場合がある。IdPへの未登録logout URI負例もChromiumの画面遷移を使う。
固定issuerを書き換えず、token／Cookie値・code付きURL・trace／HAR／storageStateを保存しない。
負例の一部はBrowserContextのHTTP要求（localhost宛）で行い、実画面操作と区別する。

[S4結果](../../docs/architecture/validation/phase4-d3-option2-s4-results-20261109.md)を参照。
段階終了時に停止し、S5／commit／remoteは個別のOwner判断に従う。

## S5：refresh・2利用者・障害・終了

S4固定成果とOwnerのS5開始判断を入力にする。依存版／lock・Java APIは継承し、frontendの同じ単体test／Next buildを実行する。
JDKの`javap`もPATHに必要。runnerは実行JAR内のoauth2-jose7.1.1を専用workへ取り出し、実際のTimestampValidator既定skew60秒を照合する。

```sh
python3 build-support/container-sso-verification/prepare.py --stage S5 \
  --jar koiki-reference-app/target/koiki-reference-app-0.1.0-SNAPSHOT.jar \
  --work-dir .tmp/d3-sso-s5-YYYYMMDD-UNIQUE --project d3-sso-s5-YYYYMMDD-UNIQUE
```

S4と同じ元JAR hashを照合してbackendへproject専用tagを付け、base→S2→S3→S4→`compose-s5.yaml`のsnapshotと
専用`compose.env`で`build frontend`を行う。その後に以下を実行する。毎回新規DB／secret／realmを用いる。

```sh
python3 build-support/container-sso-verification/verify-s5.py --work-dir .tmp/d3-sso-s5-YYYYMMDD-UNIQUE
python3 build-support/container-sso-verification/export-evidence.py \
  --work-dir .tmp/d3-sso-s5-YYYYMMDD-UNIQUE --output-dir .tmp/d3-sso-exports
```

S5の生成realmだけtoken寿命60秒と有限のevent記録を有効にする。SSO／Client Session期限・rotation true／MaxReuse0は継承する。
一時helperが独立2 IdP Sessionで実tokenを取得し、専用rotation後の旧token拒否、更新しないJWTのJava直接GETを実施する。
iat／exp／実効skew／UTC時計を根拠にexp＋60＋5秒を待つ。helper終了・token破棄・container削除後にfrontendを起動する。

`BFF_REFRESH=true`はS5だけに設定する。`migrate-refresh.py`は既存V1 checksumを確認してV2を適用し、
新しい`refresh_pending`列を追加する。初期化時のV2同一checksum再適用を確認し、SIGTERM再起動時にはmigration／seedを再実行しない。
client／Session hashのadvisory lockと短いrow lock transactionを使い、期限再読込→pending永続化→交換1回→token／versionの同時更新を行う。
不明応答を再送せず再認証にし、process／DB lossでpendingが残っても古いtokenを再送しない。logoutはrow削除で競合し、refresh完了のUPDATEはSessionを再作成しない。

実Browserは2利用者と最大1追加context、worker1／同時context最大3。並行refresh用のBFF deadlineだけを有限SQLで過去へ移す。
KeycloakのREFRESH_TOKEN event差分1、HTTP200×2、暗号化payload／version更新を照合する。
別SessionではIdPを短時間pauseし、pending確認→logout→unpauseで実refresh成功後もSessionが復活しないことを確認する。
交換結果不明は単体stub、再起動時pending拒否は別Sessionへtest-owned SQLでmarkerを設定するfixtureであり、実障害と区別して記録する。

IdP／backend／DBを今回所有containerに限ってstop／startし、ready／live、既存JWTとrefresh、業務POST失敗1回とDB／Audit不変を観測する。
DB停止回復後の古いpool接続を利用しないよう、S5のKeycloakだけ`QUARKUS_DATASOURCE_JDBC_VALIDATE_ON_BORROW=true`を明示する。
Node／Java／KeycloakへSIGTERMを送り、時間・exit／強制終了有無と同volume／同鍵での保持・復号を照合する。
処理中の業務POST／長時間requestのdrainを保証する試験にはしない。

最後に同じ最終artifactでAの作成→詳細→提出、A→B独立SSOをsmoke確認する。
SSO logout後のB既存JWT利用と、次のB refresh拒否／新規silent SSOのlogin_requiredも個別に観測する。
Cookie値・token・code付きURL・trace／HAR／storageStateを保存しない。exportは従来allowlistに公開clock／statusだけの`helper.json`を加える。

build30分・動的60分（cleanup10分予約）、常設5GiB／4CPU・PG32接続、一時helperはfrontendと非並走とする。
[S5結果](../../docs/architecture/validation/phase4-d3-option2-s5-results-20261109.md)に段階別source／artifact・KS coverage・初回失敗・残るOPENを記録する。
D3／Phase4全体の受入、commit／remote、Windows持帰り確認は個別に判断する。

### opt-in停止診断（通常S5とは別）

`prepare.py --stage S5 --stop-diagnostics ...`と`verify-s5.py --stop-diagnostics --work-dir ...`を
明示指定したworkだけでHTTP close／socket所有・要求完了状態を観測する。生成された診断overrideを
loaderが追加し、image／Next本体をpatchせずread-only preloaderを使う。
停止10秒後のAPIRequestContext解放対照を含み、停止後の保持・最終業務／SSO集合は実行しない。
正常終了しても`DIAGNOSTIC_COMPLETE`であり、S5全体PASSではない。40秒後の強制終了はFAILとする。
preloader専用testは`node --test build-support/container-sso-verification/test-stop-observer.mjs`。
実測と残る境界は[Windows記録§9](../../docs/architecture/validation/phase4-d3-option2-windows-reproduction-20261109.md#9-owner指示後の接続所有元停止待機箇所の限定調査)を参照。

通常S5の静穏時再起動は、Browser processを終了しCookieをNode harnessメモリへ保持してから行う。
新しいBrowserContextへCookieを戻し、値・属性・同一Session IDと、HTTP要求前のDB snapshot不変を照合する。
Cookie／storageStateをfileへ保存しない。`cookie-replay.mjs`は固定IdP／realmの`KC_AUTH_SESSION_HASH`が
注入時点で期限切れの場合だけ復元・期待集合から除外し、復活しないことを検査する。
有効な同Cookie・その他Cookieの完全一致、必須BFF Session3件の存在／同一ID／属性／有効性は維持する。
境界testは`node --test build-support/container-sso-verification/test-cookie-replay.mjs`（5件）。
Windows win06で復元とS5-V01〜06全件PASS、frontend停止0.412秒／143を確認した。
過去win03〜05のCookie比較FAILは履歴として保持する。詳細・未実施範囲はWindows記録§13.1を参照。
