# D3方式2 S5：refresh・利用者分離・障害・終了のCloud限定結果（2026-11-09）

## 1. 判定・範囲

**S5限定技術PASS。** 修正後の新規project `d3-sso-s5-20261109-02`でS5-V01〜06がすべて成立し、cleanup・secret／realm削除・証拠exportも完了した。初回FAILは別Evidenceで保持する。

Ownerの「S5へ進めましょう」により、S4固定`f3658311d9a9d811a203ca8ab0dd092a37e27c5b`から[段階別計画§7](../../development/phase4-d3-option2-staged-task-plan-20261109.md#7-s5refresh利用者分離障害終了)のS5-T01〜03をTooling内で実装した。Framework／Reference main／POM・業務DDL・CI・依存版／lockは変更していない。S5専用realmはAccess Token60秒、rotation有効／MaxReuse0。旧段階の300秒設定は維持する。

## 2. refreshと保存契約

`BFF_REFRESH=true`はS5 Composeでのみ有効にする。従来のBFF V1は変更せず、Tooling-owned V2で`bff_session.refresh_pending boolean NOT NULL DEFAULT false`だけを追加する。V1／V2 checksumをhistoryに保持し、V2の同一checksum再適用がDDLを重複させないことも初期化時に確認する。再起動時はseed／migrationを実行しない。

同じclient／Session hashのPostgreSQL advisory lockで要求を直列化し、row lock後に期限とtokenを再読込する。exchange前にpending=trueをcommitし、成功後は新token一式・AES-GCM IV／tag・token期限・pending=falseを条件付きUPDATEのtransactionで保存する。二つ目の要求は更新済みの期限を読み、不要な二度目の交換を行わない。

row lockだけをnetwork交換中保持すると、交換後のprocess／DB lossによるrollbackで旧tokenが利用可能な状態へ戻る。pendingを先に永続化し、advisory lockをnetwork交換まで保持することで、その状態を再認証必須として区別する。交換は1回だけ、例外／結果不明は当該Sessionを削除する。削除できないDB障害でもpendingが残り、回復後に削除して再送を拒否する。既に到達したidle／absolute期限はrefreshで復活させず、absoluteは延長しない。通常のアクセスではabsolute以内でidleを更新する。

local logoutはOrigin／Sessionに結び付くCSRF検査後にrowを削除し、refresh用advisory lockを待たない。refresh保存は既存rowの条件付きUPDATEだけなので、logout後にINSERTでSessionを復活させない。ID Token／Access Token／Refresh Tokenはserver内だけで扱い、Browserへ渡さない。IdPとの交換はopenid-client標準のrefresh grant／署名検証を用いる。DB poolのidle connection errorと、交換中のconnection lossを処理して秘密をlogへ出さない。

## 3. source・artifact・環境

同じLinux amd64／cgroup実効4 CPU・16GiB、Docker28.4／Compose2.40.3、Node24.19.0／npm11.9.0、Next16.4.5、openid-client6.8.9、Playwright1.62.0／Chromium151.0.7922.34を使用した。依存取得・追加はなく、S3のaudit全severity0だった同じlockを継承する。今回auditの再実行はしていない。

Java JAR SHA-256 `43db9951ce99ccd53d3181e9f8ea9f03f66317ebc15b704265fa129da3493986`、backend image `sha256:0a559688e4aaa5ac70a7406cb96493ca29dacfe9ab01c101de6058ff8451e26f`を再照合して再利用した。Javaの再build／S2直接回帰の反復はしていない。Spring Security oauth2-jose7.1.1のJARが実行JAR内と同hashであることを確認し、`javap`でJwtValidatorsのTimestampValidator採用と既定skew60秒を再照合した。

build30分・動的60分（cleanup10分予約）。常設4 container計5GiB／4CPU、PG接続32、Browser host1 worker／同時context最大3。JWT／rotation用の一時helperはBFF未起動時の3.5CPU／4.5GiB構成で実行し、終了・削除してからBFFを起動した。5サービスの同時稼働は行わない。buildとruntimeも非並走。

| 最終artifact／実行 | 値 |
| --- | --- |
| 開始branch／source | `docs/d3-option2-implementation-conditions-20261109`／`f365831`、開始時clean＋Tooling差分の全file hash |
| frontend context SHA-256 | `e9268bf62634df4d56a39ce5847149955b9c2424a1cf5f7a1e7f0b184187afae` |
| frontend image | `sha256:9165698b434f3c876705900fff7090b7edf2ac888406c3c21ff32771a7b3f712` |
| 経過時間 | 353.412秒、cleanupまで含む |
| source保存 | 各workの`source-snapshot/`に62 fileをmanifest hashと照合して保持。README／結果／AGENTSは実行後の記録として区別 |

## 4. 実行結果

| 集合／ID | 実測 |
| --- | --- |
| 単体／静的 | 既存29＋新規refresh境界10＝39件PASS・skip0、共有所有物guard5件PASS。Python／Node構文、Next build／TypeScript検査成功 |
| S5-V01 | PASS。並行API200×2、Keycloak REFRESH_TOKEN event差分1、旧token再送0、暗号化payload変更・pending=false・version差分4（marker1＋token更新1＋2要求のSession更新2）。専用IdP Sessionの旧token再使用拒否、実refresh成功とlogout競合でもSession row0。結果不明は単体stub、永続pendingの再起動後拒否は別fixture |
| S5-V02 | PASS。2利用者のSSR／RSC／API／Cookie／cache分離、本人200・他人404、RSCの各subjectとno-store。helperの未更新JWTはJava直接GETで期限前404→exp＋60＋5秒以後401、BFF refresh0 |
| S5-V03 | PASS。IdP停止でlogin503・refresh401／Session削除、別の有効JWT経路は404まで到達。backend停止中の業務POST1回は503、DB／Audit不変。上流timeout5秒設定・業務POST自動再送なし、各依存回復後ready |
| S5-V04 | PASS。DB停止でready503／live200／API503、DB回復後readyと本人API200・新規IdP login成立。自動restart0／OOM0、業務値・Audit重複なし |
| S5-V05 | PASS。下表のSIGTERM停止後、同じvolume／鍵で業務・Audit・BFF Session暗号文／hash・schema history・realm IDが完全一致。再読込／復号と2利用者SSRも成功。再seed／BFF migration再適用なし |
| S5-V06 | PASS。同じ最終artifactでBrowser作成201→本人詳細200→提出204→reload SUBMITTED、A→B独立SSO。HTML／RSC／client bundle7件にJWT・選択HTML／RSCにopaque ID露出なし。最終DB／Audit・KS対応・cleanup成立 |

| SIGTERM対象 | 停止秒数 | exit | 強制kill／OOM |
| --- | --- | --- | --- |
| frontend | 0.122 | 143 | なし／なし |
| backend | 0.446 | 143 | なし／なし |
| keycloak | 1.159 | 143 | なし／なし |

Node／Java imageはStopSignal=SIGTERM、KeycloakはDocker既定SIGTERM。停止時はBrowserをrendezvousで待機させ、業務POST実行中の停止は行っていない。exit143はSIGTERMによる終了として扱い、SIGKILL相当のexit137は不成立条件とした。手動stop／startと、自動restart counter0を区別する。

保持用申請は`DRAFT:1200:version1`、明細1件／BUSINESS Audit0。通常Session2件と、pending fixture1件の計3行について、停止前後のclient／Session hash／暗号文digest／pendingを照合した。schema historyはV1／V2の2件、realm IDも一致し、鍵fileのhashも同じ。復帰後、pending fixtureだけ401／row削除・IdP交換0で拒否し、通常2 Sessionは同じ鍵で復号できた。製品／Springの通常の起動時migration確認は行われるが、BFFのseed・DDLを再投入していない。

最終smokeは別の2300円申請を作成・提出し、DBは申請2件・明細2件・提出SUCCESS Audit1件、提出申請のownerは`d3200000-0000-4000-8000-000000000001`。backend停止中の999円作成要求は再送していない。

SSO logout後のBは、既存JWTで本人申請を200で読めた。その後BFF deadlineを過去へ移すと、Bの実refreshは401・Keycloak REFRESH_TOKEN_ERROR差分1・B Session削除。新しいB prompt=noneは400／login_requiredで、IdP Sessionがないことを再確認した。S4の「既存B Session／JWTが即失効する保証はない」という境界と、次のrefreshによる再認証必須への移行を分けて記録する。

最終logはbackend INFO121／WARN9／ERROR0、Keycloak ERROR0、BFFは各起動・Readyのみ。WARNは既存provider／cache設定とDB停止に伴う接続失効・health失敗等。DB ERROR2件は初期製品probeのmigration_model／databasechangeloglock不存在で、その後のmigration・全journeyが成功した。生成secret値／JWTのlog露出、Session属性の重複キー例外は検出されていない。

## 5. 意図的なfixtureと実接続の区別

並行refreshを同じタイミングで発生させるため、test所有の対象Session1件だけのBFF Access Token deadlineを過去へ移す。実IdPへは有効なRefresh Tokenを送り、並行2要求のHTTP結果・Keycloakの永続REFRESH_TOKEN event差分・暗号化payload変更・version差分を照合する。tokenを単体stubで発行した結果にはしない。

logout競合は独立した別Browser Sessionを使う。Keycloakを短時間pauseし、pendingがcommitされた後にlogoutでSessionを削除、unpauseして実refresh成功が戻っても保存rowが復活しないことを観測する。主journeyの業務Sessionは競合用Sessionと分ける。

交換結果不明は単体内の有限stubで「IdP消費後に応答喪失」「DB commit結果不明」を与え、旧credentialの交換呼出しが1回だけ・再認証へ移ることを確認する。実networkで応答喪失を起こした保証とは分ける。再起動のpending拒否も専用別Sessionのpending=trueをtest-owned SQLで設定したfixtureであり、実process crashを発生させた結果とはしない。

JWT期限のhelperはBFFのSession／refreshを通らず、実認証で取得したAccess Tokenをメモリ保持する。実iat／exp・host／Java container／DBのUTC時計を照合し、exp＋確認済みskew60秒＋余裕5秒以後に同じJWTでJavaへ直接要求する。専用別IdP Sessionでのrotation後旧token拒否と、主journeyの認証を混ぜない。

## 6. 過去段階との対応とKS coverage

全10群を段階ごとの証拠へ対応付ける。群数はtest件数ではない。過去段階の限定成立を継承する領域と、今回の最終artifactで再実証した領域を区別し、旧負例全集合を反復したとは扱わない。

| 固定段階 | source／artifactの対応 | S5への継承根拠・境界 |
| --- | --- | --- |
| [S1](phase4-d3-option2-s1-results-20261109.md) | `3e3ec1b`。元JAR `12cec4df…`／image `60d15067…` | 3DB／role・権限・V1 DDL／seed・所有物guard。S1とS2以降は別JAR hash。S1 artifactを最終artifactと同一として全再検証したとはしない |
| [S2](phase4-d3-option2-s2-results-20261109.md) | `445681c`。JAR `43db9951…`／backend `0a559688…`をS3〜S5で再利用 | 実claim・ID Token JWS・Java正常3操作／拒否6条件・直接回帰23件。Java／mapper／利用者profile・scopeの契約は維持。S5は寿命60秒・event記録だけ段階生成で変更 |
| [S3](phase4-d3-option2-s3-results-20261109.md) | `f48a0cc`。当時frontend `ec1f72ff…` | CSRF／Origin／state／body／固定API・Session暗号化・no-store・鍵欠落負例。S5ではSession取得・refresh／logout競合を変更し、旧frontendとのbinary同一性は主張しない |
| [S4](phase4-d3-option2-s4-results-20261109.md) | `f365831`。当時frontend `4be601ce…` | 独立2 Client、cold prompt=none、client Cookie／transaction混同、logout復帰state／固定URI。該当境界の関数・client設定を継承し、S5の最終SSO／refresh chainで影響をsmoke確認 |
| S5 | `f365831`＋今回file hash、最終frontend `9165698b…` | 新しいrefresh branch・V2／競合・2利用者・実JWT期限・停止回復／再起動・最終smoke。S5のfile／context／imageはmanifest・execution・resultsで対応付け |

| KS群 | 確認済みの証拠 | 今回未反復・保証外 |
| --- | --- | --- |
| KS01 | S1-V01〜05、S3-V04、S5 preflight／V2／resource・image照合 | 最終imageで3DB権限拒否全集合／鍵欠落負例を反復していない |
| KS02 | S2-V01、S5の新規DB／realm／claim時刻／ready | 実企業IdP、別Originは対象外 |
| KS03 | S3-V02、S5-V06の最終Browser／DB／Audit | 業務全機能・負荷は対象外 |
| KS04 | S4-V01〜02、S5最終SSOとlogout後login_required | 別アプリ運用間SSOは対象外 |
| KS05 | S2-V03、S5の他人404／実JWT期限401 | 誤audience／ID Token／scope／無効／権限なしの旧全集合を最終artifactで再実行していない |
| KS06 | S3-V03〜04、S4-V02〜04、単体39件、S5-V02／V06 | 別Originの攻撃画面操作、全将来画面の秘密非露出保証は対象外 |
| KS07 | S4-V03、S5-V01〜02、SSO logout後のB JWT／refresh／silent SSO対照 | 結果不明はstub、pending復帰はSQL fixture。実network応答喪失／process crash全般ではない。backchannelなし |
| KS08 | S5-V03〜04、DB回復後の新規login | 任意障害・長期安定／最大pool競合の保証は対象外 |
| KS09 | S5-V05の静穏時SIGTERM・同volume／同鍵保持 | 処理中POST drain、鍵更新／volume喪失時は対象外 |
| KS10 | 各段階のcleanup／export、S5-V06の残留0 | Windows受領／checksum照合・Cloud download経路は未確認 |

### 初回失敗・訂正と実行回数

初回project `d3-sso-s5-20261109-01`は323.375秒でFAIL。実JWT期限・rotation、並行refresh／logout競合、IdP／backend／DB停止の応答とready回復までは観測できたが、DB回復後の新規IdP loginが500になり、restart集合へ進む前に停止した。KeycloakのFreeMarker login画面生成中のSQLに、`FATAL: terminating connection due to administrator command`とclosed connectionが残っていた。ready UPだけで新規loginの成立まで保証できなかった断面として保持する。

S5用Keycloakだけ`QUARKUS_DATASOURCE_JDBC_VALIDATE_ON_BORROW=true`を追加した。採用image内のQuarkus Agroal3.40.1で、既定falseのvalidateOnBorrow設定がDataSourcesからAgroal supplierへ渡るbytecodeを確認し、container設定のtrueと対応付けた。修正後は同じDB停止集合と、回復後の本人API200・新しい独立IdP loginを再実証して通過した。Framework／ReferenceやKeycloak本体を改変していない。

併せて点検で見つけたtoken保存時のversion更新を同transactionへ移し、並行時の期待差分を3から4へ訂正した。初回の主BFFでも古いtoken再送／Session復活は観測されなかったが、最終版はtoken／versionの原子更新条件まで満たす。runnerのJava依存参照もCloud固定cache pathから実行JAR内のlibrary抽出へ改めた。SSO logout後のB JWT利用→refresh拒否→新規silent失敗も明示観測に加えた。harnessの構文訂正は再実行前に解消している。

単体は39件×2回とも全PASS、共有guardは5件1回PASS。Next build3回はすべて成功（初版→DB接続error処理補強→token/version訂正）、frontend image buildは01／02それぞれ1回。動的集合は01 FAIL／02 PASSの2回。OwnerのFramework非改変の修正後再実行許可に従い、新規project／DB／secret・各source snapshotで分離し、初回を成功で上書きしていない。

## 7. cleanup・export

両projectの常設container／一時helper・専用network／DB volumeはcleanup済み、残留0。各試行の生成secret10 fileとrealmも削除した。以前の匿名volume2件は保持し、全体prune・既存DBの操作は行っていない。JAR／image／依存・build cache／source snapshot／redacted Evidenceは保持する。

| project | 結果 | archive bytes／SHA-256 |
| --- | --- | --- |
| `d3-sso-s5-20261109-01` | FAIL、cleanup PASS | 30,103／`734ff76692fb87bb19b7c8c075b9755c30d1edb1a29f2bd8a30d60f32174fa53` |
| `d3-sso-s5-20261109-02` | PASS、cleanup PASS | 29,577／`9e38be2845c55f5b706273fada15bf23e260a24da78d876d18bc2217ef07e7cb` |

archiveは`.tmp/d3-sso-exports/<project>-evidence.zip`。manifest／execution／results／cleanup／runtime-redacted.log／browser.json／helper.json＋archive manifestの8 entries。公開status／hash／属性／時刻だけを記録し、JAR／image／DB dump／secret／生成realm／env／trace／HAR／storageStateは含めない。両試行でJSON schema・entry checksum・source snapshot hash・所有物残留0を照合した。

補助Evidenceは`.tmp/d3-option2-s5-build-20261109/`の39件×2・guard・Next build3回・JWT bytecode・Keycloak pool設定／bytecode／最終review、各workのimage build／browser／helper logとsource snapshot。Windows取得／checksum照合・Cloud download経路の成立は未確認のOPENを維持する。

## 8. 保証限界・次の判断

同じOrigin／Next process内のlocal HTTP構成における有限実証である。別Origin／別案件の企業SSO、backchannel logout、本番TLS／secret manager／鍵rotation、Windows再現／手動確認は未実証。SIGTERMは今回の静穏な状態での停止・保持を測定し、処理中の業務POSTや長時間requestをdrainする保証にはしない。負荷／長期安定・最大pool競合・多instance BFFの運用保証には拡張しない。

実案件では、rotation・token／Session期限・交換結果不明時の再認証と永続marker、鍵保持／更新の手順を案件の要件として決める。同一volume／同鍵の再起動成立を、本番鍵更新・障害時の一般保証と同一視しない。

Owner結果判断・commit／push／PR／merge、D3／Phase4全体や本番運用の完了は今回行わない。

## 9. S5コミットポイントとremote反映のOwner判断

Ownerは「S5 PASS をコミットします。Windows再現のために、リモートpushして、Windows端末でこれをpullしたところで再現へ進めるでよいでしょうか」と指示した。今回のS5 Tooling実装・結果記録・AGENTS入口と本引継ぎをlocal commitへ固定し、作業branch `docs/d3-option2-implementation-conditions-20261109`へ通常のpushで送る。S1〜S4の未送信local固定commitも同branchの祖先履歴として反映する。実SHAとpush後のlocal／remote一致は操作後の報告とGit履歴で確認する。

以前のcommit／remote未実施表示はこの後続指示前の履歴として保持する。Windowsの実行結果・手動打鍵・証拠受領、PR／merge、D3／Phase4全体の受入を今回のcommit／pushだけで成立扱いにしない。

## 10. Windows再現への引継ぎ

1. Windowsの既存変更を保持したうえでoriginをfetchし、上記branchへ切替・`git pull --ff-only`する。`git rev-parse HEAD`をCloudのpush完了報告のSHAと照合し、`git status --short`で開始時cleanを確認する。未反映変更や分岐があれば上書きせず差分を確認する。
2. AGENTS、本書、[段階別計画§11](../../development/phase4-d3-option2-staged-task-plan-20261109.md#11-cloud実装自動検証windows再現手動確認)、[実行README](../../../build-support/container-sso-verification/README.md)を読む。Docker Linux container／Compose／Buildx、amd64、CPU／memory／disk、port13000／18080、Java21のJDK／javap、Python、Node24.19.0／npm11.9.0、CA／proxyをWindowsで確認する。Cloudの絶対pathや環境変数をコピーしない。
3. Windows側で専用lockから`npm ci --ignore-scripts`、同版Playwrightの`install chromium`を行う。39件のNode境界・共有guard5件・Next buildを実行する。Referenceは`build-reference.ps1 --stage S2 --output <新規出力先>`を入口に、選択直接回帰23件を含めて同sourceから新規生成する。Maven settings／toolchains／CAはWindowsの設定を使い、JAR／imageのhashをその環境のmanifestへ記録する。CloudとのJAR byte一致を必須にはしない。
4. `prepare.py --stage S5`とREADMEのCompose手順でWindows用の新規work／project／DB／secret／realm・imageを用意し、`verify-s5.py`のS5-V01〜06をWindowsの自動再現として実行する。失敗履歴・source／artifact・cleanup・exportをWindows結果として別記録し、Cloud結果を上書きしない。保持中の方式1project／DBを操作しない。各OSでのsecret mount・改行・path／standalone出力等の成立も実測する。
5. Cloudの`.tmp`・証拠ZIP・image・JAR・node_modules・Browser cacheはGit pullでは取得されない。Cloud証拠を比較用に持ち帰る場合は、§7のallowlist ZIPを別途取得し、PowerShellの`Get-FileHash -Algorithm SHA256 <取得ZIP>`で§7のSHA-256と照合して初めて持帰り確認済みと記録する。再現そのものはGit sourceと新規生成物を入力にできる。
6. 自動再現後のOwner手動打鍵は別の判断で進める。手動環境は自動cleanup用projectと別名にし、起動時にURL／利用者案内／保持・停止条件を記録する。手動操作報告とDB／Audit点検を区別する。今回のCloud環境ではWindows実行や手動環境起動を先行しない。

Windows開始時の依頼例：

> Cloud報告のS5固定commitを確認し、AGENTS.mdとS5結果§10に従ってWindows開始前確認、同sourceの新規build、専用projectでS5の自動再現を進めてください。Cloud結果を上書きせず、Windows結果・cleanup・export状態を別に記録してください。既存方式1環境は操作せず、手動環境の起動・保持やremote操作へは自動進行しないでください。
