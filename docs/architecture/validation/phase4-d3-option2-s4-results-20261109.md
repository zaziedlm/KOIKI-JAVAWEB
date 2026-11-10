# D3方式2 S4：独立2 Client SSOのCloud限定結果（2026-11-09）

## 1. 判定・範囲

**S4限定技術PASS。** 新規project `d3-sso-s4-20261109-03`でS4-V01〜04とcleanup・exportが成立した。

Ownerの「S4へ進めましょう」により、S3固定 `f48a0cce6496ba1138b8289c1ae8d84e33dc0ddd`から[段階別計画§6](../../development/phase4-d3-option2-staged-task-plan-20261109.md#6-s4独立2-clientのsso)のS4-T01〜02をTooling内で実装した。Framework／Reference本体・既存業務DDL・BFF migration・依存版／lock・CIは変更していない。

## 2. 構成・契約

同じNext.js process／Origin内にRP `/a`・`/b`を配置する。Client IDは既存`koiki-d3-a`と新規`koiki-d3-b`。各clientのsecret、callback、post-logout URIを分ける。BはS4 Composeの`BFF_RPS=a,b`でのみ有効にし、S1〜S3の生成realmには追加しない。

| 結付け | A | B |
| --- | --- | --- |
| Session Cookie | `D3_A_SESSION`／Path `/a` | `D3_B_SESSION`／Path `/b` |
| 認証途中／logout Cookie | `D3_A_FLOW`／`D3_A_LOGOUT` | `D3_B_FLOW`／`D3_B_LOGOUT` |
| DB client_id | `a` | `b` |
| callback | `http://localhost:13000/a/auth/callback` | `http://localhost:13000/b/auth/callback` |
| logout復帰先 | `http://localhost:13000/a/auth/logout-return` | `http://localhost:13000/b/auth/logout-return` |

CookieはHttpOnly・SameSite=Lax・Domainなし・local HTTPのSecure=false。DBはopaque値のhashをclientと組み合わせて検索し、AES-GCMのAADとCSRFにもclientを結び付ける。state／binding／有効期限／用途の検査後に一回消費する。既存3 tableを利用し、logout stateも暗号化payloadの用途を分けてauth transactionに格納する。

RP-Initiated Logoutは標準libraryのend-session URLを使用し、`client_id`・固定`post_logout_redirect_uri`・stateを渡す。ID TokenをBrowserへ出さないため`id_token_hint`を使わず、Keycloakの確認画面を操作する。開始はJSON POST＋Origin／Session CSRF、戻りはCookie binding／state／client／用途の一回消費で保護する。任意戻り先入力は拒否し、stateから外部URLを復元しない。

## 3. 環境・実行条件

S3と同じLinux amd64／cgroup実効4 CPU・16 GiB、Engine28.4／Compose2.40.3、Node24.19.0／npm11.9.0、Next16.4.5、openid-client6.8.9、Playwright1.62.0／Chromium151.0.7922.34。依存追加はなく、S3でaudit全severity0だったlockを継承する。今回auditの再実行はしていない。

S2のJava JAR SHA-256 `43db9951ce99ccd53d3181e9f8ea9f03f66317ebc15b704265fa129da3493986`とbackend image `sha256:0a559688e4aaa5ac70a7406cb96493ca29dacfe9ab01c101de6058ff8451e26f`を照合して再利用した。Java再build／S2直接回帰の反復はしていない。

build25分・動的35分（cleanup10分予約）、常設4 containerの合計上限5 GiB／4 CPU、PG接続32。helperはfrontend未起動時だけ使い、stop／rm後にfrontendを起動する。Browserはhost1 worker／同時context最大2。buildとruntimeは非並走。各試行は新規project／DB／secretで分け、再実行はOwnerのFramework非改変の修正後再実行許可に基づく。

## 4. 検証結果

| ID | 実測 |
| --- | --- |
| S4-V01 | PASS。A対話login後、BはSessionなし・SSR匿名・API401。資格情報再入力0回のprompt=noneでB認証成功。同じ検証済みIdP subjectを持つ、異なるA／B opaque Cookieと暗号化DB行を同時に照合 |
| S4-V02 | PASS。新規contextのB prompt=noneは400／login_required、B Sessionなし。Aの実在する未完stateと同じbindingをB Cookieへ複製しても400。元A transactionは未消費のまま |
| S4-V03 | PASS。下表のlogout範囲を個別に照合。SSO logoutはKeycloak確認画面を操作し、正しい復帰後に新規SSOが失敗 |
| S4-V04 | PASS。Cookie属性・DB client結付け、混同／state／復帰先拒否、今回所有物cleanup・secret／realm削除 |

| 操作後 | A BFF Session | B BFF Session／JWT | IdP新規SSO |
| --- | --- | --- | --- |
| A local logout | Cookie削除・API401 | Cookie同一、固定GET404まで到達 | A prompt=none成功、新しいA Sessionを発行 |
| A SSO logout | Cookie／DB行削除 | 同じB Cookie／DB hashが残る、固定GET404まで到達 | A prompt=noneが400／login_required、A Sessionなし |

固定GETは存在しないUUIDの本人詳細で、Javaが認証後に対象不存在404を返す経路。業務申請／BUSINESS Auditは各0件で、副作用はない。404を対象申請の読取成功とは記録しない。BのJWTが期限まで常に使える保証や、他RPの即時失効保証にはしない。

| 境界 | HTTP |
| --- | --- |
| B認証前／A local logout後のA API | 各401 |
| A Session値をB Cookieへ複製 | 401（B Cookieを戻すと404） |
| AのCSRFをB logoutへ渡す | 403 |
| A認証途中state／bindingをB callbackへ渡す | 400 |
| SSO logoutの任意returnTo入力／CSRF欠落 | 400／403 |
| logout復帰state不一致／client不一致／auth用途への流用／復帰再利用 | 各400 |
| IdPへ未登録のpost_logout_redirect_uri | Chromiumで400、外部へ遷移しない |

正常login／SSO・IdP logout確認は実Chromium、正確なheaderやCookie混同・復帰再利用の一部は同じBrowserContextのHTTP要求で実行した。別Originの画面からの攻撃実証には読み替えない。HTMLにJWT／opaque Session値がないことも選択画面で確認した。S3のRSC／bundle全件・業務3操作・鍵欠落集合は反復していない。

最終BFF SessionはBの1件だけ。auth transactionはAが5件中4消費、Bが2件中2消費で、未消費1件はcross-client拒否に使ったAの途中情報。AES-GCM／IV12byte・tag16byteとclient別hashを照合し、全DBは最後に専用volumeごと削除した。

| 最終artifact | 値 |
| --- | --- |
| 開始branch／source | `docs/d3-option2-implementation-conditions-20261109`／`f48a0cc`、開始時clean＋今回Tooling差分のfile hash |
| frontend context SHA-256 | `eb7fc8002641b0b231b3c8bab2dd3871c51cc769b3469013420a48f6d59a7868` |
| frontend image | `sha256:4be601ce2be66143525d8f1215375a34c579f9b03008ea95efb04fa5844cd53b` |
| runtime／cleanup所要 | 113.747秒、常設4 container／restart0／OOMなし、計5GiB／4CPU上限 |

各試行のmanifestがsource SHA／全Tooling file hash／JAR／lock／image版／recipe／helper／realm／frontend context hash、資源上限・命令範囲・集合とコマンドを保持する。READMEの手順・本結果・AGENTS入口は実行後の記録として追記し、実行時snapshotと区別する。

## 5. 失敗・訂正の保持

単体初回は27 PASS／2 FAIL。logout URL用のsynthetic Configurationにlocal HTTP許可がなく、libraryがHTTPS必須として拒否した。fixtureにも明示的なHTTP許可を設定し、29件全PASS（skip0）。実アプリのHTTP許可範囲はcanonical local issuerだけを維持した。共有guard5件、Next build／TypeScript検査も成功。

動的集合は3回で、01／02はFAIL、03で全PASS。01はSSO logoutの負例まで進んだところでharnessが停止し、秘密を出さない初版の固定診断だけでは位置が不足した。02ではstaticな段階名・assertion名・HTTP statusだけを保存するよう補強し、未登録URI負例へのHTTP requestで停止したと特定した。host NodeのDNS lookupは`EAI_AGAIN`、公開discoveryへのPlaywright APIRequest読取probeもDNS失敗だった。同じcanonical issuerへChromiumのloginは到達していた。

未登録IdP URIの負例も`page.goto`へ訂正し、03を新規projectで同じS4-V01〜04集合として再実行した。issuer／host DNS／アプリ実装は変更していない。修正はharnessの診断・アクセス手段だけで、01生成の同じfrontend image／contextをhash照合して02／03でも再利用した。Next build1回・frontend image build1回。各試行のsource hashと旧browser script、FAIL／cleanup／exportを個別workに保持した。01／02の成功部分を段階全体PASSへ合算していない。

| project末尾 | 結果／秒 | export bytes／SHA-256 |
| --- | --- | --- |
| `01` | FAIL／120.603、cleanup PASS | 18,378／`a0fe5ec71a38687734d07c346a21d7377cbe28d9897f5ce5f769624402d81d58` |
| `02` | FAIL／115.017、cleanup PASS | 19,326／`77f886cb25d58191f315f653d89184a7bd90dbca056d2af980947117616691ac` |
| `03` | PASS／113.747、cleanup PASS | 20,492／`a0146e1679a59a3be6a4fcd3955b90c99033aaf67c2e996d92c6fa8609381f82` |

最終runtime logのbackendはINFO45／WARN2／ERROR0、BFFは起動・Readyのみ。Keycloakの`LOGOUT_ERROR`は予定した未登録URI負例のWARN `invalid_redirect_uri`。DBのERROR2件は初期製品probeの`migration_model`／`databasechangeloglock`不存在で、その後のmigration／realm／health／SSOは成立した。secret値／JWTのlog露出は検出されず、Session属性重複キー例外もない。

## 6. cleanup・証拠export

全3 projectの常設container・一時helper・専用network／DB volumeはcleanup済みで残留0。各試行の生成secret10 fileとrealmも削除済み。以前の匿名volume2件は保持し、全体pruneや既存DB操作は行っていない。JAR／image／依存・build cache／redacted Evidenceは保持する。

最終archiveは`.tmp/d3-sso-exports/d3-sso-s4-20261109-03-evidence.zip`。manifest／execution／results／cleanup／runtime-redacted.log／browser.json＋archive manifestの7 entries。JAR／image／DB dump／secret／生成realm／env／HAR／trace／storageStateは含めない。各entry checksumとresult schema、cleanup・source差分を照合した。

補助Evidenceは`.tmp/d3-option2-s4-build-20261109/`の単体初回／訂正後log、guard・Next build log、DNS診断、各workのimage build／browser／verify logと旧browser script。01のarchiveはbrowser.json未生成のため6 entriesで、失敗位置の詳細不足もそのまま保持した。Windows取得／checksum照合・Cloud download経路の成立は未確認のOPENを維持する。


## 7. 保証限界・次の判断

同じOrigin／process内の2 RPでの限定実証であり、Cookie Pathは強いセキュリティ隔離境界ではない。別Origin／別processの実企業SSO・本番TLS・secret manager・鍵rotationは未実証。local logoutは選択RPだけを削除する。SSO logoutでも他RPの既存BFF Session・発行済みJWTの即失効を約束せず、backchannel logoutを実装していない。refresh／並行更新・障害／終了drainはS5へ残す。

結果Owner受入・commit／push／PR／merge、Windows持帰り／手動確認、D3／Phase4全体の認定は未実施。S5へ自動進行しない。

## 8. S4コミットポイントのOwner判断

Ownerは「S4 PASS をコミットします」と指示した。今回のS4 Tooling実装・本結果記録・AGENTSの現行入口をlocal commitとして固定する。実際のSHAは操作後の報告とGit履歴で確認する。上記のcommit未実施表示はこの後続指示前の履歴であり、S5開始・push／PR／merge、D3／Phase4全体の受入を含めない。限定技術PASSの範囲・失敗履歴・Evidence・Windows持帰り未確認を保持する。
