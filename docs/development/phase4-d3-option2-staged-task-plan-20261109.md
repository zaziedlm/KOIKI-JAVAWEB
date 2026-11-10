# D3方式2：段階別実装・検証タスク計画（2026-11-09）

**Cloud引継ぎ方針：** Ownerは本計画へのCloud／Windows分担・Linux build・開始前確認・証拠持帰りの追記後、本作業branchへのcommit・pushを指示した。Cloudでは本branchを選択し、§11に従ってS1から順に進める。今回の端末では文書成果を固定・引継ぎし、実装やcontainer起動は行わない。以下の文書作成時の未commit／remote別判断表示は履歴とし、今回の文書commit・branch pushにはこの後続指示を適用する。PR／merge／tag／Releaseは含まない。

## 1. 今回の判断と計画の使い方

Ownerは[具体化案](phase4-d3-option2-implementation-conditions-20261109.md)とDB整理を了解し、一括実装ではなく5段階で進める方針を了解した。今回は「段階別タスク計画を具体化する」文書作業の指示に基づく。**各段階の実装・環境起動は未着手であり、今回の了解を5段階すべての実行許可にはしない。**

branchは`docs/d3-option2-implementation-conditions-20261109`を継続。HEADは`cf77a3c58c9dd4a7460e965ec9abe224f668c083`で、先行する具体化文書の未commit差分を保持する。mainの同期照合は具体化案作成時の記録であり、今回remoteを再照合していない。

Phase 4 D3のTooling-owned計画。実装場所は`build-support/container-sso-verification/`（以下、資材ルート）。Reference既存APIを利用し、初回はReference main／POM、Framework、Root Reactor／CIの変更0件を維持する。業務は申請作成・本人詳細・提出の3操作。方式1の手動project／DB／volume／secretは操作しない。

各段階は「対象・条件確認→実装→その段階の有限検証→結果提示→Ownerによる次段階の判断」の順とする。結果受入、local commit、push／PR／mergeはそれぞれ別の判断。段階ごとの新branch作成・commitを自動必須にはせず、開始時に実際のsource SHAと未commit差分hashを固定する。

| 段階 | 到達する状態 | 主に対応するKS群 |
| --- | --- | --- |
| S1 DB・起動基盤 | 専用3 DB・権限分離と既存Java migration／BFF新規DDLが再現できる | KS01、KS02のDB部分 |
| S2 Keycloak→Java API | 実IdPのtokenで既存APIの正常・拒否が成立 | KS02、KS03のAPI部分、KS05 |
| S3 BFF最小縦断 | Client AでBrowser→BFF→Java→DB／Auditが成立 | KS03、KS06のA境界、KS07のlocal logout |
| S4 独立2 ClientのSSO | A／B独立SessionとSSO・logout保証限界が確認できる | KS04、KS06のclient間分離、KS07のSSO logout |
| S5 更新・障害・終了 | refresh競合・障害・再起動・最終cleanupを評価できる | KS06の残り、KS07のrefresh／期限、KS08、KS09 |

KS10（証拠・cleanup）は全段階共通。KSは確認群でありtest件数ではない。細分したcheckpointに`S1-V01`等のIDを付け、結果を個別に記録する。

## 2. DB対応の共通前提

PostgreSQLコンテナ1つに3つの独立DBを新設する。既存方式1のDB実体は共用しない。「既存を利用」は既存DDL／処理の再利用を意味する。

| DB | 既存／新規の区別 | 今回の対応 |
| --- | --- | --- |
| `koiki_app` | 既存KOIKI／Reference migrationで新しい空DBに作成 | 既存テーブルの列追加・独自テーブル追加は予定0件。検証利用者・部門・経費区分・権限などの行をseed |
| `koiki_bff` | BFF用に今回新規設計 | `bff_schema_history`、`bff_auth_transaction`、`bff_session`の3テーブル案。名称・列・制約はS1で確定 |
| `koiki_keycloak` | Keycloak製品側テーブルを新規作成 | S1で空DB／roleを準備し、S2でKeycloak自身に初期化させる。KOIKI側の独自DDLを適用しない |

`koiki_app`の既存Identity（user／role／permissionと関連）、master（部門・経費区分・所属）、expense（申請・明細）、Auditを利用する。既存のJava用`koiki_session`／`koiki_session_attributes`はBFF用に流用しない。`koiki_external_identity_link`も既存だが今回のBearer経路では参照せず、Keycloakの管理属性→tokenの`koiki_user_id`→既存KOIKI利用者の対応を使う。既存local認証用テーブルや承認者scopeを削除する変更も行わない。

| BFF新規テーブル案 | S1でDDLへ落とす内容 | 使用開始 |
| --- | --- | --- |
| `bff_schema_history` | 適用version・checksum・適用時刻。再適用時のchecksum不一致拒否とmigration排他 | S1 |
| `bff_auth_transaction` | client識別、browserとの結付け、state照合情報、保護したnonce／PKCE情報、期限、一回消費状態 | S3 |
| `bff_session` | client識別、`session_id_hash`（BFF Session IDのhash）、issuer／subject、CSRF照合情報、idle／absolute期限、version、暗号化tokenと暗号化metadata、token期限 | S3、refresh本体はS5 |

Client A／Bは共通テーブル内で識別し、DBをclientごとに増設しない。DBをまたぐFKや業務情報・権限のBFF複製は作らない。BFFの暗号鍵はDB外で供給し、token／認証途中情報は暗号化、Session IDはhash保存とする。正確な型・NULL条件・一意制約・索引はS1の成果としてreview可能にする。

`session_id_hash`とOIDCの`sid` claim（IdP Session識別子）を混同しない。初回はbackchannel logoutを実装せず、IdPの`sid`専用列も追加しない。将来採用する場合はissuer／clientとの結付け・保存要件を別途設計する。

初回は各専用DB内でDB owner／migration role／runtime roleを兼用する簡略構成とする。`koiki_app`はJava起動時の既存Flywayを同roleで実行する。BFF／Keycloakも自DB内のDDLを許可するが、いずれもNOSUPERUSER／NOCREATEDB／NOCREATEROLEとし、PostgreSQL初期化管理credentialを渡さない。「管理credentialなし」はDB owner権限まで禁止する意味ではない。実案件でのDDL実行主体とruntime権限の分離は終了成果へ論点として残す。

## 3. S1：DB・起動基盤

**開始入力：** 共通の開始手順は§1／§8〜9による。S1では採用版調査／依存・image取得と専用環境操作の範囲を確認する。

| 順序／ID | タスク | 主な対象ファイル案（資材ルート配下） |
| --- | --- | --- |
| S1-T01 | Java／Node／PG／Keycloak・npm package候補を調べ、全採用版・digest／platformを固定。Browser自動化はPlaywright Nodeを採用候補とし、package版と対応Chromium revision／実Browser版・OSも固定。取得可否・engines／peer依存・security情報を記録 | `versions.json`、`frontend/package.json`／`package-lock.json`、`README.md` |
| S1-T02 | project／絶対workdir・所有物台帳、secret生成・redaction、preflight・cleanupを先に作る | `prepare.py`、`preflight.py`、`cleanup.py`、`evidence-schema.json`、`.dockerignore` |
| S1-T03 | 3 DB／専用role、PUBLIC CONNECT制限、BFF3テーブルのDDLとmigration手順を作る | `init-db.sh`、`bff-migration.sql`、`migrate-bff.py` |
| S1-T04 | 既存JavaをSPAなし・d3-container明示で生成し、専用DBへmigration・有限seedを適用。Linux／Windows入口は共通引数・版検査・終了判定を使う | `build-reference.py`、`build-reference.sh`、`build-reference.ps1`、`Dockerfile.backend`、`reference-seed.sql`、`compose.yaml` |
| S1-T05 | S1の観測／結果生成と自動cleanup、秘密を除く証拠exportを接続 | `verify.py`、`export-evidence.py`、`README.md` |

S1ではDB＋backendだけ起動する。Keycloak／BFFサービスは後段で追加し、4サービス同時起動をS1の前提にしない。JavaのBearer有効化はS2からとし、S1は既存probeとDB初期化の成立を確認する。Next.js package／lockを固定しても画面・認証処理はまだ実装しない。

| 検証ID | 有限確認・終了条件 |
| --- | --- |
| S1-V01 | source／JAR／lock／base digestと実imageを識別。公開DB portなし、今回所有資源だけを参照 |
| S1-V02 | 3 DB／roleが存在し、自DB内のmigration／runtime操作は成功、他2 DBへの接続は拒否。DB owner兼用・非superuser等のrole属性と管理credential非供給を確認 |
| S1-V03 | 既存KOIKI→Reference migration・table・seed対応が成立。BFF3 table／history作成、Keycloak DBに製品tableがまだないことを区別 |
| S1-V04 | BFF migration再適用で重複なし、checksum不一致は拒否。Java再起動後にseed再投入なし・history／既存行保持 |
| S1-V05 | Java GET live／readyと情報非露出、今回projectのcleanup・redacted証拠保存が成立 |

直接回帰は`ReferenceContainerSecurityConfigurationTest`を選択。DB／migration固有testは隔離DB内で実行する。**終了成果：** DB構成表・BFF DDL／索引・固定版一覧、S1各結果とcleanup、差分一覧。実案件への示唆はDB owner兼用と権限分離の検討点を1〜3行で記録する。

## 4. S2：Keycloak→Java API

**開始入力：** S1の固定artifactとDB／seed再現手順。realm／claimと既存Bearer契約の接続を先に確かめる。

| 順序／ID | タスク | 対象ファイル案 |
| --- | --- | --- |
| S2-T01 | Keycloak追加、canonical hostname／内部alias・port、製品DB初期化、health有効化・内部management port9000のready観測を設定。hostへmanagement portを公開しない | `compose.yaml`、`realm-template.json`、`prepare.py` |
| S2-T02 | Client A、audience／token_use／管理属性／scope、4検証利用者と負例Clientを定義 | `realm-template.json`、`reference-seed.sql`、`README.md` |
| S2-T03 | Javaへ既存Bearer設定を外部供給。同一Docker network内の一時Node helperでCode＋PKCEの実Browser認証・token取得・内部API観測 | `verify-oidc.ts`、`frontend/lib/oidc.ts`、`verify.py`、`compose.yaml` |

token取得はpassword grant／client credentialsで利用者loginを代用しない。Windows host Nodeの`idp.localhost`解決に依存しないよう、helperはKeycloakと同一networkの一時containerで動かす。固定callback `http://localhost:13000/a/auth/callback`へhost loopback `127.0.0.1:13000`からport転送し、helperはcontainer内でlistenする。state／nonce／PKCEをlibraryで検証し、tokenはhelper内に保持して生値を出力しない。helperはTooling runnerであり常設の5番目のサービスにはしないが、実行中の追加containerとして台帳・資源・cleanupへ計上する。S3ではhelperを終了・削除し、同callbackをNext.jsへ引き継ぐ。`frontend/lib/oidc.ts`の共通処理を再利用する。

Keycloakは`KC_HEALTH_ENABLED=true`を設定し、採用版のbuild／起動方式で実効有効化を確認する。image内curlを前提にせず、一時Node helperのHTTP probeから内部`http://keycloak:9000/health/ready`を観測する。ready成功に加えrealm discoveryも確認し、単なるport開放を起動完了としない。Compose内healthcheckが必要なら採用imageのbash／TCP機能を確認して有限checkを実装する。S3以降も同network上の一時probe runnerを使い、観測実行中の資源を計上する。

### 4.1 段階別token／Session設定案

realm templateの段階別生成設定として次を固定し、生成設定hashを証拠へ残す。異なる段階で新規realm／DBを作る際に適用し、既存realmへのimportで上書きされたと仮定しない。

| 設定 | S2〜S4 | S5 | 確認する意味 |
| --- | --- | --- | --- |
| Access Token Lifespan | 300秒 | 60秒 | refresh未実装の縦断に5分の操作時間を確保。S5は期限検証を短縮 |
| BFF Session idle／absolute | 15分／60分（S3から） | 同左 | BFF独自の利用期限。IdP Sessionと同一視しない |
| SSO Session Idle／Max | 30分／120分 | 同左 | IdP全体のSSO期限 |
| Client Session Idle／Max | 20分／90分 | 同左 | RPごとの期限。SSO側上限より短く、BFFより長い候補 |
| Revoke Refresh Token／Max Reuse | true／0 | 同左 | rotationを明示有効化。`revokeRefreshToken`／`refreshTokenMaxReuse`の採用版実効値を照合 |
| Remember Me／offline_access | 無効／要求しない | 同左 | 別の長期Session・offline tokenを初回へ混ぜない |

BFFへのAPIアクセスだけではIdP idle期限が更新されるとは限らず、IdPの認証／refresh要求とBFFのidle更新を分けて記録する。Keycloakのidle判定には猶予があり得るため、表の値どおりの秒で失効すると断定しない。S5-V01の「古いtoken再使用なし」はBFFの送信回数・保存更新の確認と、専用の別Sessionでrotation後の旧Refresh Tokenが拒否されるIdP確認を分離する。後者で主journeyのSessionを失効させない。

| 検証ID | 有限確認・終了条件 |
| --- | --- |
| S2-V01 | 採用Browserの実認証経路とhelper・Javaのdiscovery／JWKSでcanonical issuer対応を確認。内部health ready、製品table・realm・mapper・段階別Session設定・実token claimが整合 |
| S2-V02 | 利用者tokenで作成201→本人詳細200→提出204、DB／明細／利用者・成功Auditが対応 |
| S2-V03 | 誤audience401、ID Token401、scope不足403、KOIKI無効利用者401、権限なし403、他人詳細404。拒否で業務値・成功Audit増加なし |
| S2-V04 | helper終了・token非露出・今回projectのcleanupが成立 |

採用Chromiumの種類・revision／実versionとKeycloak Cookieの実属性・受入／送信挙動を記録する。`*.localhost`のsecure context扱いだけでHTTPのSecure Cookie受入を保証しない。HTTP経路が成立しなければ当該段階を停止し、local HTTPS化等の必要差分を提示する。Cookie保護・issuer検証を弱めて回避しない。

直接回帰は`ReferenceApiJwtDecoderTest`、`ReferenceJwtAuthenticationConverterTest`、`ExpenseApiControllerTest`、`ApiExceptionBoundarySecurityTest`を選択。**終了成果：** 実claim対応表、正常／拒否結果、API／DB／Audit照合。実案件への示唆としてHTTP／Cookieの制限・Session値とclaim対応の判断点を記録する。

## 5. S3：BFFの最小縦断（Client A）

**開始入力：** Client Aのclaim契約・同じJava API。S1で固定したNext.js等のsecurity情報を再確認し、更新が必要ならlock／imageの変更理由と再検証範囲を記録する。

| 順序／ID | タスク | 対象ファイル案 |
| --- | --- | --- |
| S3-T01 | BFF store・暗号化・認証途中record一回消費・Session発行／期限・CSRF／Origin検査を作る | `frontend/lib/store.ts`、`crypto.ts`、`csrf.ts`、`config.ts`、`frontend/test/` |
| S3-T02 | Aの認証開始／callback／local logout、固定3操作proxy、画面・SSRを作る | `frontend/app/[rp]/`、`frontend/lib/api-client.ts`、`oidc.ts` |
| S3-T03 | standalone build／非root・必要書込／health、4サービス起動と実Browser journeyを接続 | `Dockerfile.frontend`、`frontend/next.config.ts`、`frontend/app/health/`、`compose.yaml`、`browser/` |

RPはこの段階ではAだけ許可する。S3〜S4は§4.1の300秒tokenを使う。refresh本体はS5へ分け、期限到達後は再認証必須として更新を自動再送しない。token期限検査・Session失効・秘密保護は初回から実装する。

| 検証ID | 有限確認・終了条件 |
| --- | --- |
| S3-V01 | BFFからcanonical discovery／JWKS到達とissuer照合、Browser login→A Session発行→本人SSR表示。tokenはserver側のみ、Cookie属性・Session更新・期限検査が成立 |
| S3-V02 | Browser作成・本人詳細・提出とDB／Auditが対応。業務POST再送なし |
| S3-V03 | CSRF欠落・誤値・異Origin拒否、state不一致／callback再利用拒否、固定proxy先・入力上限。local logout後のSession／Cookie削除 |
| S3-V04 | 認証済みSSR／RSC／APIのno-store、HTML／RSC／bundle／logへtokenなし。BFF暗号鍵欠落の1構成は起動拒否 |
| S3-V05 | 単体境界test・今回projectのcleanupと証拠保存が成立 |

**終了成果：** Aの実Browser結果、BFF3 tableの使用状況、DB／Audit照合、境界test結果。実案件への示唆としてlocal HTTPのSecure=false、単一鍵供給、token／BFF Session寿命の選択を記録する。Java無変更ならS2の全回帰は繰り返さず、S3-V02を接続smokeとする。

## 6. S4：独立2 ClientのSSO

**開始入力：** Aの成立断面。共通処理を使ってBを追加し、Cookie／Session／認証途中recordのclient結付けを検証する。

| 順序／ID | タスク | 対象ファイル案 |
| --- | --- | --- |
| S4-T01 | Client Bと固定callback／post-logout URI、RP allowlist、独立Cookie・Sessionを追加 | `realm-template.json`、`frontend/lib/config.ts`、`frontend/app/[rp]/` |
| S4-T02 | prompt=noneのSSO対照、RP-Initiated Logoutと復帰state検査を追加 | `frontend/lib/oidc.ts`、`frontend/lib/store.ts`、`browser/`、`frontend/test/` |

| 検証ID | 有限確認・終了条件 |
| --- | --- |
| S4-V01 | 同一contextでA認証後、B Sessionなし→資格情報再入力なし→B独立Session。A Sessionを読むだけでB認証済みにしない |
| S4-V02 | 新規contextのprompt=noneはlogin_required等で失敗しB Sessionなし。Aの認証途中情報をBへ渡すと拒否 |
| S4-V03 | A local logout後のB／IdP存否、SSO logout後の新規SSO失敗を個別観測。B既存Session・発行済みJWTが即失効する保証にはしない |
| S4-V04 | A／BのCookie・DB client結付け、logout復帰state／固定戻り先の拒否test。今回projectのcleanup |

**終了成果：** 独立2 Client journeyと対照結果、local／SSO logoutの作用範囲表。実案件への示唆として同一origin／同一processの2 RPであること、Cookie Pathは強い隔離境界でないこと、backchannel logoutなしを明記する。別origin／別アプリのCookie分離・logout成立は未実証とし、企業IdP／別運用案件間SSOを認定しない。

## 7. S5：refresh・利用者分離・障害・終了

**開始入力：** 正常縦断とSSOの成立断面。S5用60秒tokenと§4.1のrotation／Session設定を新規realmへ適用する。

| 順序／ID | タスク | 対象ファイル案 |
| --- | --- | --- |
| S5-T01 | row lock／期限再読込・token更新transaction・結果不明時の再認証・logout競合対策 | `frontend/lib/store.ts`、`oidc.ts`、`frontend/test/` |
| S5-T02 | 2利用者・並行2要求、IdP／backend／DB停止回復、SIGTERM／再起動の有限runnerを作る | `browser/`、`verify.py`、`cleanup.py`、`README.md` |
| S5-T03 | 全段階証拠のsource／artifact対応とKS群coverage、残る未実証・結果判定をまとめる | 結果文書、`evidence-schema.json` |

| 検証ID | 有限確認・終了条件 |
| --- | --- |
| S5-V01 | 同一Session並行2要求でrefresh交換1回、BFFが旧Refresh Tokenを再送しない。専用別SessionでIdPのrotation後旧token拒否を確認。交換結果不明は再送せず再認証、logout競合でSession復活なし |
| S5-V02 | 2利用者のSSR／RSC／API／cache／Session分離と他人参照拒否。refreshせず保持したJWTをhelperからJavaへ直接送り、exp＋実効skew＋余裕後401（約125秒を予約）。BFF期限拒否とは分離 |
| S5-V03 | IdP停止でlogin／refresh失敗、既存JWT経路は別記録。backend停止時503または上限内504。業務POST再送なし |
| S5-V04 | DB停止時ready失敗・live分離、各依存回復後ready、restart増加・更新重複なし |
| S5-V05 | Node／Java／KeycloakのSIGTERM時間・強制終了有無、同volume／同鍵再起動後の業務／Audit／BFF Session／realm保持、再seed・migration重複なし |
| S5-V06 | 最終artifactでAの作成→詳細→提出とA→B SSOの短いsmoke、秘密非露出・最終cleanup・KS群coverage照合 |

Java既存実装は`JwtValidators.createDefaultWithIssuer`を使用し、公式資料の既定skewは60秒。S5のtoken寿命60秒なら発行から約120秒が境界となるため、5秒の余裕を含め約125秒をS5検証枠内に予約する。実tokenのiat／expとhost／containerのUTC時計を基準に`exp＋実効skew＋5秒`を判定時刻とし、単純な固定sleepだけにしない。採用Spring Security版の実効値を開始時に再照合し、Javaの既定を検証短縮目的で変更しない。単体時計注入の結果と実IdP／Java実時間の結果は分離する。

結果不明のtoken交換は単体test内の有限stub、実IdPでのrefresh・期限・SSOは実接続結果として分離する。**終了成果：** KS01〜10の確認済み／未実施／FAIL／HOLDをsource・artifact別にまとめ、Owner結果判断へ提示。実案件への示唆はrotation／token・Session期限、結果不明の扱い、同一volume／鍵再起動と本番の鍵更新との差を記録する。D3／Phase4全体や本番運用の完了とはしない。

## 8. 段階間の再現・実行枠・停止

各段階でproject名を`d3-sso-s<段階>-<実行日>-<一意suffix>`とし、新規専用DBで前段のmigration／seed／realmを再現する。段階終了時は今回所有container／network／volume・生成secretをcleanupし、image／redacted Evidence／manifestを保持する案。S5の同volume再起動は当該段階内だけvolume／鍵を保持する。段階をまたぐ稼働保持を自動前提にしない。

次段階では前段全testを反復しない。新projectの初期化・到達性／ready・artifact識別は毎回preflight、変更箇所の直接回帰と表中の検証を選択する。前段証拠のsourceと最終artifactが異なる場合は継承根拠と影響を記録し、S5-V06のsmokeだけで未再検証領域を全保証しない。

以下は**review用の実行上限案**で、開発所要時間の見積り・実行許可ではない。具体化案の一括runtime90分案を、段階別検証時はこの案で置き換える。累積した旧残予算は使わない。

| 段階 | build／依存取得枠 | 動的検証枠（cleanup予約を含む） |
| --- | --- | --- |
| S1 | 60分：版／lock／取得・Java／image | 40分：preflight10・DB／migration20・cleanup10 |
| S2 | 20分：realm／helper・必要image | 45分：preflight10・IdP／API25・cleanup10 |
| S3 | 40分：BFF／image | 50分：preflight10・A縦断／境界30・cleanup10 |
| S4 | 25分：Client B／BFF差分 | 35分：preflight5・SSO／logout20・cleanup10 |
| S5 | 30分：refresh／runner差分 | 60分：preflight10・競合／障害／再起動35・最終smoke5・cleanup10 |

各集合初回1回、失敗時は診断保全・cleanupし、訂正対象と再実行集合を提示する。再試行を成功で上書きしない。単体／直接回帰の正確な展開件数・コマンド・時間上限は各段階の開始manifestで固定し、表の検証ID数をtest件数へ置き換えない。

資源は具体化案§7のruntime最大5 GiB／4 CPU、PG接続32・pool上限、Browser worker1／context最大3、build非並走を上限候補として継承。S1／S2は起動サービス分だけ計上する。保持中サービスとOS余裕を差し引き不足なら開始しない。通信timeout・停止猶予も同案を継承し、実際の能力は開始前に確認する。

一時helper／probe containerはNode imageを共用し、各最大512 MiB／0.5 CPU、同時1個の追加観測資源として計上する。S3以降の最大はruntime5 GiB／4 CPU＋probe0.5 GiB／0.5 CPUとなる。Browser本体はhost側の別枠で、Playwright管理の固定ChromiumをS2から使用する。各段階開始は前段結果確認と当該段階の開始指示を条件とし、各節の終了成果を提示してから次へ進む。

停止は、既存資源参照・issuer不一致・秘密露出・誤利用者更新・結果不明・OOM／上限超過・cleanup不能。全体pruneや既存DBの操作で解消しない。残留ID・最後に成功したtask／検証ID・次の確認事項を記録し、cleanup不明をPASSにしない。

## 9. 記録形式・次の作業

各段階の開始manifestには、対象task／file、source SHA／dirty hash、採用版・image／JAR／lock hash、実行コマンド・件数／skip、環境・資源／時間、所有物台帳、Owner指示の範囲を記録する。結果には各検証IDの期待／実測、初回失敗と訂正、実行外、cleanup、残るOPEN、次段階判断を残す。生token・password・Cookie値を証拠へ格納しない。

各段階の結果に「実案件への示唆」欄を設け、意図的な簡略化と採用案件で決める値・責任を1〜3行で残す。検証合否と実案件への推奨を混同しない。

結果文書は段階ごとに`docs/architecture/validation/phase4-d3-option2-s<N>-results-<実行日>.md`を作り、S5で総合結果への索引を作る。予定ファイルは未作成のため現時点でリンクにしない。

| 項目 | 現在の状態 |
| --- | --- |
| 具体化案・DB整理 | Owner了解済み。実装前の正確な版／file・実行集合の確定は残る |
| 5段階で進める方針 | Owner了解済み。本書の具体的タスク・終了条件・実行枠は今回提示する案 |
| S1〜S5 | 全段階未着手。後続Owner方針によりCloudへ引継ぎ、S1から段階別に進める（§11） |
| commit／remote | 後続Ownerが今回の文書一式のcommit・本branch pushを指示。実際のSHA／push結果は操作後の引継ぎ報告で確定 |

**次の作業はS1の開始条件・対象／上限の確認と、OwnerのS1実装開始判断。** S1指示後はその範囲の通常作業を進め、同一範囲の許可を反復要求しない。S1が成立して結果を確認できるまではS2を実装しない。

既存E1 V09限定技術PASS／V10未完了、S1非同期系の既知未達・人手HOLD、C2 H07、D1結果受入未記録、正式support／全DoD／Phase4全体の状態を維持する。本書のS1〜S5は方式2の作業段階IDであり、過去の非同期package S1とは別である。

## 10. Claude Code点検の反映（2026-11-09）

Owner提示の`C:\Users\kataoka\.claude\plans\git-d3-2-2026-11-09-claude-rustling-island.md`を現行source／公式資料と照合した。指摘1〜15を以下のとおり採用し、HTTP Cookieの説明は実測条件へ補正した。コード・依存・稼働環境は変更していない。

| 指摘番号 | 対応・判断 |
| --- | --- |
| 1・2 | skew既定60秒の根拠と実効版再照合、約125秒の観測枠。S2〜S4はtoken300秒、S5は60秒 |
| 3 | §4.1にSSO／Client／BFF Sessionとrotationの明示設定、BFF挙動とIdP拒否の別検証を追加 |
| 4 | helperを同一networkの一時containerへ変更。host NodeのDNS依存を除き、追加資源を計上 |
| 5 | Playwright Node／Chromium版を固定対象化。HTTP Cookie成立はBrowser実測とし、secure contextだけから保証しない |
| 6・7 | 内部management readyのprobe実行主体、health有効化、DB owner兼用と非superuserの境界を明記 |
| 8・9 | 具体化案の結果文書名／project名を段階別計画へ整合 |
| 10・11 | `session_id_hash`へ統一、IdP sid列は初回なし。BFF discovery／JWKS到達をS3-V01に明記 |
| 12・13 | 同一originの限界、各段階の実案件への示唆欄を追加 |
| 14・15 | 共通手続きを§1／§8〜9へ寄せ、各節の重複を削減。S3でsecurity情報再確認 |

根拠：[Spring Security 7.1.1 JWT validation](https://github.com/spring-projects/spring-security/blob/7.1.1/docs/modules/ROOT/pages/servlet/oauth2/resource-server/jwt.adoc)、[Keycloak 26.8.1 Session timeout](https://github.com/keycloak/keycloak/blob/26.8.1/docs/documentation/server_admin/topics/sessions/timeouts.adoc)、[refresh rotation](https://github.com/keycloak/keycloak/blob/26.8.1/docs/documentation/server_admin/topics/sso-protocols/con-oidc-auth-flows.adoc)、[Keycloak health](https://www.keycloak.org/observability/health)。照合した資料版を採用版確定としない。runtime・Cookie・実効設定の成立確認は各段階の検証に残す。

## 11. Cloud実装・自動検証／Windows再現・手動確認

### 11.1 分担とsourceの引継ぎ

| 実行場所 | 担当範囲 | 記録・限界 |
| --- | --- | --- |
| Codex Cloud | S1〜S5のTooling実装、Linux build、単体・直接回帰、専用Compose、Cloud内Chromiumによる自動認証／業務／SSO、障害・cleanup | Cloud環境の実測として段階別結果を記録。Windowsの手動操作成立へ読み替えない |
| Windows端末 | Cloud成果を取得し、新規専用projectでWindows build／起動・選択回帰・自動smokeを再現。別途指示した手動環境でOwnerが操作 | Cloud結果を上書きせずWindows再現結果・Owner操作報告／DB／Audit点検を別記録。保持中の方式1環境は操作しない |
| Owner | 各段階の結果確認・次段階判断、Windows手動確認と結果受入 | Cloudの自動Browser結果をOwner打鍵と記録しない |

引継ぎbranchは`docs/d3-option2-implementation-conditions-20261109`。今回の計画commit・push後に報告する実際のSHAをCloudの開始入力とし、Cloud側でbranch／HEAD／clean状態と計画§10〜11の存在を確認する。Cloudの旧main／旧branchで実装しない。以後も各段階でsource SHA・採用差分・artifact hashを対応付け、remote反映はその時点のOwner指示に従う。

5段階の順序は維持する。新規Cloudタスクで再開する場合、前タスクのDB／container／`.tmp`／image cacheが利用できるとは仮定せず、Gitのsourceとmanifestから必要物を再生成する。計画commitに自身のSHAを埋め込む必要はなく、開始時に取得したSHAを実行manifestへ記録する。

### 11.2 Cloud開始前の確認

新しいCloud環境のruntime guidanceを最初に読み、以下を開始manifestへ記録する。[方式1Cloud結果](../architecture/validation/phase4-d3-codex-cloud-verification-20261107.md)は参考情報であり、同じ能力・設定が今回もあることの保証ではない。

| 確認対象 | 必要な確認・条件不成立時の扱い |
| --- | --- |
| source／指示 | 指定branchと引継ぎSHA、AGENTS、今回段階の対象file・検証集合・資源／時間・所有物。後段を一括開始しない |
| Docker | daemon接続・Compose／Buildx版、Linux platform、network／volume／loopback portの利用可否。接続拒否をEngine停止と即断せず環境の承認手順に従う |
| 資源 | 実際のCPU／memory／disk・cgroup制限、runtime最大5.5 GiB／4.5 CPUにBrowser／runner／OS分を加えた余裕。buildとは非並走。不足なら縮小起動でPASSにせず実行条件を見直す |
| build tool | Java／javac／MavenをJDK21へ統一、Node／npm・Python・shell・Wrapper権限、専用Maven repository・settings／toolchainsの供給方式 |
| 取得先・CA | Maven、npm、Java／Node配布先、image registry（Keycloakを含む）、Chromium／OS依存配布先とredirect先の到達。proxy／CAを環境の供給方法で設定。TLS検証やsecurity guardを無効化しない |
| Browser／origin | Playwright管理ChromiumをCloud内で起動し、VM loopback13000／18080への接続・idp.localhost解決・実Cookie挙動を確認。helper／BFF／JavaはDocker内部の同名issuerへ接続 |
| secrets／DB | 今回用の使い捨てcredential・暗号鍵・3 DB／roleを新規生成。Windowsのsecret／DB dump／社内CA一式をコピーしない |
| 出力経路 | Git結果文書と、秘密を除く証拠archiveの取得方法・保存先・checksum照合方法を確認。ignored領域にあることだけで持帰り済みとしない |

Docker不可ならcontainer検証は未実施、Browser不可ならBrowser検証は未実施とする。実装・静的確認の成果と動的未実施を分け、必要な環境整備を提示する。許可済み範囲の通常準備は進めるが、外部基盤・公開URL・TLS proxy新設で条件を自動変更しない。

Cloud内の自動Browserなら既存localhost callbackを使う。OwnerのWindows BrowserからCloud loopbackへ直接接続する運用は前提にしない。Cloud preview／port転送を追加する場合はorigin・issuer・redirect URI・Cookie・公開範囲の差分を設計してから扱う。初回はCloud自動検証までとし、手動打鍵はWindows再現後に行う。

### 11.3 Linux buildとWindows入口

S1で作る`build-reference.py`を共通build入口とし、Linuxの`build-reference.sh`とWindowsの`build-reference.ps1`は引数・環境の薄いadapterとする。OSに合わせ`mvnw`／`mvnw.cmd`を選び、Java21／javac・Node／npm版検査、実行cwd、失敗exit、test選択・heap・JAR hash記録を共通化する。方式1の既存scriptは変更しない。

LinuxのReference buildはRepository rootで次と同等の条件を使う。**以下はS1の予定コマンドで、今回実行していない。**

```sh
./mvnw -B -ntp -pl koiki-reference-app -am \
  -P d3-container clean package \
  '-Dtest=ReferenceContainerSecurityConfigurationTest' \
  -Dsurefire.failIfNoSpecifiedTests=false '-DargLine=-Xmx768m'
```

S2の直接回帰は§4の4 classへ選択を置き換える。段階ごとの正確なコマンド・展開件数・skipをmanifestへ固定する。Maven settings／toolchains／repositoryやproxy CAの引数はCloud実環境で確定し、過去の`/workspace/...`のパスをコピーしない。Wrapper実行権限が不足する場合も、その環境の通常手順で解決する。

Next.jsは専用lockに対する`npm ci`・段階に必要なtest／buildとする。LinuxのBrowser本体とOS共有libraryはCloud側で準備し、Windowsのnode_modules／browser cache／classpathをコピーしない。改行・実行権限・path・classpath区切り・secret mount権限を両OSで確認する。JARのbyte一致を環境間の必須条件とせず、各環境のsource→JAR→image対応と機能結果を照合する。

### 11.4 証拠を持ち帰る方法

| 種別 | 保存・引継ぎ方法 |
| --- | --- |
| Gitへ残すもの | Tooling source／lock／DDL／secretなしtemplate、段階別結果文書、必要最小限のredacted manifest・KS対応表。各段階のcommit／pushは個別の指示範囲を確認 |
| Gitへ入れないもの | JAR／image／node_modules／DB volume、password／token／Cookie／暗号鍵、生成realm・compose.env、未点検のraw log／Browser trace |
| 別artifactとして持帰るもの | allowlist方式で抽出・秘密点検済みの環境情報、HTTP status／DB集計／Audit対応、終了時間・cleanup台帳、必要なredacted log／画像。archive manifestとSHA-256を添付 |

`export-evidence.py`は`.tmp`を丸ごと圧縮せず、承認したファイル種別・相対pathだけを抽出する。tokenやCookieが入り得るHAR／Playwright trace／storageState、secret入りrealm、DB dumpを既定除外し、値のredactionと出力内容の点検後にarchiveを作る。元rawとexport済み証拠の区別を記録する。

Cloudが提供するdownload／artifact取得経路を開始時に確認し、段階終了前にarchiveを取得可能な場所へ出し、ファイル名・サイズ・SHA-256・内容一覧を報告する。保存場所は実環境で確定し、固定のdownload API／pathがあると仮定しない。Windowsで取得した際にSHA-256を照合して初めて「持帰り確認済み」と記録する。未取得は「export済み／持帰り未確認」、転送経路がない場合はOPENとして残す。

Windows再現時は同じ成果commitのsourceから新規build・専用DB・secretを作る。Cloud DBや鍵を移植せず、必要な検証データはseedから再生成する。手動環境は自動検証projectとは別名で起動し、URL・利用者案内・保持／停止条件を記録する。手動結果はOwner報告とlog／DB／Audit読取点検を分け、CloudのFAIL／未実施や既知制限を消さない。

### 11.5 Cloudでの再開入口

1. 指定branchを選び、引継ぎ報告の計画commit SHAと実checkout・clean状態を照合する。
2. AGENTSと本書を読み、§11.2の環境能力とS1のfile／検証manifestを確認する。新規work／projectを用意する。
3. Cloudの開始指示に従いS1-T01〜05だけを実装・検証する。環境能力や対象外差分が必要なら理由と必要範囲を提示する。
4. S1結果・cleanup・証拠export／持帰り状態を提示し、S2の判断を待つ。以後同じ方法で段階ごとに進める。

今回の文書commit／pushはCloud引継ぎのための操作で、Cloudタスクの起動・S1の実行結果を意味しない。Cloud成果をWindowsで確認するまで、Windows再現・Owner手動確認は未実施として保持する。
