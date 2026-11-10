# D3方式2 S2：実Keycloak→Java APIのCloud限定結果（2026-11-09）

## 1. 判定・範囲

**S2限定技術PASS。** Ownerの「S2へ進めます。」により、S1固定commit `3e3ec1b024e05ca56db853d53c1cc8f681e9f686`から、[段階別計画§4](../../development/phase4-d3-option2-staged-task-plan-20261109.md#4-s2keycloakjava-api)のS2-T01〜03を実装した。最終project `d3-sso-s2-20261109-05`でS2-V01〜04が成立し、cleanup・証拠exportまで成功した。

Reference main／POM、Framework本体、既存migration、Root Reactor／CI、方式1資材の変更は0件。変更は`build-support/container-sso-verification/`のToolingと結果／入口文書だけ。先行Ownerが許可したFramework本体改変を伴わない修正・再実行を適用し、初回失敗・途中結果を上書きせず保存した。S3〜S5・結果全体のOwner受入・commit／remoteは自動進行していない。

## 2. 環境・artifact・実装

| 項目 | 確認値 |
| --- | --- |
| branch／source | `docs/d3-option2-implementation-conditions-20261109`、開始時cleanの`3e3ec1b`＋今回Tooling差分。各workで実行source／recipe／lock hashを固定 |
| host | Debian13、Linux amd64、cgroup実効4 CPU／memory16 GiB。Docker表示5 CPUとの違いはS1から維持 |
| tooling | Engine28.4.0、Compose2.40.3、Buildx0.31.0。JDK／javac21.0.12.1+1、Wrapper3.9.16、Node24.19.0／npm11.9.0、Python3.12.14 |
| IdP／helper／Browser | 固定Keycloak26.8.1、Node24.19.0、openid-client6.8.9、Playwright1.62.0／Chromium151.0.7922.34 revision1234 |
| JAR | `d3-container`、SPA／async JDBCなし。SHA-256 `43db9951ce99ccd53d3181e9f8ea9f03f66317ebc15b704265fa129da3493986` |
| backend image | `sha256:0a559688e4aaa5ac70a7406cb96493ca29dacfe9ab01c101de6058ff8451e26f`。JAR hash labelを照合し、各新規projectのtagで同一imageを再利用 |
| その他image | S1の`versions.json`のdigestを変更せずNode／Keycloakもpull。PG／Node／Keycloakの実image IDは最終結果JSONへ記録 |
| runtime | backend1.5 GiB／1 CPU、PG1 GiB／1 CPU、Keycloak1.5 GiB／1 CPU、一時helper512 MiB／0.5 CPU。計4.5 GiB／3.5 CPU。Browserはhost側worker1／同時context1 |
| port | DB／backend／managementはhost非公開。Keycloak18080／helper13000は127.0.0.1のみ。外部公開・previewは作っていない |

build／依存取得枠は20分、動的45分（cleanup10分予約）。Java build55.371秒、image取得はNode8.37秒／Keycloak15.58秒で並列取得。buildとruntimeは並走していない。5試行のruntime runnerはcleanup込み累積453.693秒。旧S1予算を流用していない。

新規は`compose-s2.yaml`／`realm-template.json`／`verify-oidc.ts`／共通`frontend/lib/oidc.ts`／`browser-s2.mjs`／`verify-s2.py`／OIDC境界test。既存prepare／preflight／cleanup／build／export／schemaをstage選択へ拡張し、S1既定を保持した。共有guardのfixtureは実行前source hash確認の追加に合わせた。最終実行時のTooling29 fileとhelper依存98 fileのhashはwork manifestに記録し、最終README／結果文書は実行後に追記した。

## 3. 実claim・Client・利用者の対応

canonical issuerはBrowser／helper／Javaで`http://idp.localhost:18080/realms/koiki-d3`に統一した。hosts変更はなく、Browserはlocalhost解決、containerはDocker aliasを使用した。内部managementのreadyとdiscovery／JWKS key2件をhelperから確認した。Javaの正常API成功が、既存issuer／audience／署名検証を含む実接続の証拠となる。

| claim／設定 | 実測・確認 |
| --- | --- |
| Access Token | 正常aud=`koiki-reference-api`、scope=`openid expense.apply`、token_use=`access`、管理属性由来の`koiki_user_id`。exp−iat=300秒を6認証すべてで照合 |
| 誤audience | 独立した負例Clientがaud=`unrelated-api`を発行。署名を壊したtokenで代用していない |
| scope不足 | 正常Clientで`openid`だけを要求。実tokenのscopeに`expense.apply`なし |
| ID Token | audはClient Aまたは負例Client自身でAPI audienceなし。issuer／audience／nonce／時刻とJWS署名をopenid-clientで検証、実JWKS cache使用も確認 |
| subject／利用者 | ID Tokenのsub=`d321…0001〜0004`と管理属性のKOIKI UUID=`d320…0001〜0004`を対応。Access Tokenの観測ではsubはなく、KOIKI UUID claimで既存Java利用者へ接続する。BFFのsubjectは検証済みID Tokenを基準にする論点として残す |
| KOIKI状態 | 1・2はACTIVE＋APPLY権限、3はDISABLED、4はACTIVE＋権限なし。4名ともIdPは有効なので、KOIKI状態による拒否をJavaで確認できる |
| 属性編集 | `koiki_user_id`のedit権限はadminのみ。生成templateだけでなく、Keycloak DBのuser-profile component設定を照合 |
| Client | confidential／standard flowのみ、PKCE S256必須、password grant／implicit／service accountは無効。固定callbackは`http://localhost:13000/a/auth/callback` |
| Session／rotation | realm DBのSSO idle1800秒／max7200秒、refresh revoke=true／max reuse0、access300秒を照合。両Clientのidle1200秒／max5400秒はclient_attributesの実値も照合 |

helperが記録するAccess Token claimは許可fieldのdecode観測で、独自署名検証ではない。Access Tokenの検証主体はJava。ID TokenのJWS検証はlibrary標準`enableNonRepudiationChecks`を明示して行う。HTTP例外は正確なlocal issuerだけに制限し、別issuer／Client／callbackを拒否する境界testを追加した。

Cookieは6認証とも、`AUTH_SESSION_ID`／`KEYCLOAK_IDENTITY`等がSecure=true、SameSite=NoneとしてChromiumに保存された。両者はHttpOnly=true、`KEYCLOAK_SESSION`／`KC_AUTH_SESSION_HASH`はfalse。IdPへの要求で`AUTH_SESSION_ID`／`KC_AUTH_SESSION_HASH`／`KC_RESTART`の送信名を確認した。値は保存していない。これは今回のChromium・local HTTPでの実測であり、一般HTTP／別Browser／本番TLSの保証へ拡張しない。Cookie保護設定を緩めて成立させていない。

## 4. 最終検証結果

| 集合／ID | 結果 |
| --- | --- |
| Java直接回帰 | PASS、4 class／23件、failure0／error0／skip0。Decoder1、Converter3、ExpenseApiController6、ApiExceptionBoundary13。build1回 |
| Tooling境界 | PASS、共有所有物／selector guard5件、OIDCの別issuer・別Client・別callback拒否3件 |
| S2-V01 | PASS。実ChromiumでCode＋PKCEの6認証、state／nonce／ID Token JWS、内部ready／canonical discovery／JWKS、製品DB／realm／mapper／設定・claim対応を確認 |
| S2-V02 | PASS。作成201→本人詳細200→提出204、DBのSUBMITTED:1200、明細1件合計1200、申請者UUID、SUBMIT_EXPENSE成功Audit1件を照合 |
| S2-V03 | PASS。下表の拒否6条件・計11要求で、申請・明細・Business SUCCESS Auditの行hashが前後一致 |
| S2-V04 | PASS。helperの保持token／未完認証0、helper exit0／OOMなし、全所有資源と生成secret／realmのcleanup、redacted証拠保存が成立 |

| 拒否条件 | GET本人詳細 | POST作成 |
| --- | --- | --- |
| 誤audience | 401 | 401 |
| ID Token | 401 | 401 |
| scope不足 | 403 | 403 |
| KOIKI無効利用者 | 401 | 401 |
| KOIKI権限なし | 403 | 403 |
| 別の有効利用者による他人参照 | 404 | 対象外 |

最終申請IDは`f71737ed-f8a4-4897-93dd-38f6b0cf7d03`。既存実装に従い、作成に新しいAuditを追加せず、提出の成功Auditを確認した。業務POSTはhelper側で開始guardを立て、同一projectで結果不明の再送をしない。Browserはtokenを受け取らず、helperが内部APIへAuthorizationを供給する。HAR／trace／storageState・Cookie値・生token・code付きURLは保存していない。

## 5. 初回失敗・途中結果・最終条件

| project suffix | 結果／秒 | 内容 |
| --- | --- | --- |
| 01 | FAIL／62.690 | Keycloakのuser-profile policyに無効なenum `DISABLED`を指定しimport停止。本人編集禁止を維持して有効値`ADMIN_EDIT`へ訂正 |
| 02 | FAIL／89.518 | IdP／Java起動後、観測SQLの`SSO_SESSION_IDLE_TIMEOUT`等が製品DB列名と不一致。採用版RealmEntityを照合し`SSO_IDLE_TIMEOUT`／`SSO_MAX_LIFESPAN`へ訂正 |
| 03 | 機能PASS／99.794 | 4 checkpoint・cleanup成功。Cookie送信名とClient期限のDB観測、ID Token明示JWS検証は後続で補強 |
| 04 | 観測補強PASS／100.734 | Cookie送信名・Client期限のDB値も成立。ID Token JWSはlibrary既定へ依存していたため最終条件とはしない |
| 05 | 最終PASS／100.957 | library仕様に従いJWS署名検証を明示有効化し、全6認証で実JWKS cache使用を確認。4 checkpoint・cleanup成功 |

各試行は新規secret／DB／realm／workを使い、JAR／imageを再buildしていない。失敗／途中成功のresultsとexportをそのまま保持し、後続成功で上書きしていない。retry対象はTooling設定・観測・OIDC境界の訂正だけで、Framework・Referenceの一般修正は行っていない。

## 6. ログ・cleanup・export

最終backendログはINFO45／WARN2／ERROR0。WARNは既存の認証provider／cache統計警告。KeycloakはERRORなし、deprecated feature／sticky-sessionのWARN3件。DBのERROR2件はKeycloak初期化時の`migration_model`／`databasechangeloglock`不存在の製品probeで、その後の製品migration・realm import・ready・API検証が成立した。Session属性重複キー例外、生成secret値・JWTのruntime log露出は検出されていない。

5試行すべて、container／network／DB volume残留0。生成secret8 fileと生成realmを各試行のcleanupで削除した。helperは最終exit0でプロセスを終了し、保持tokenをDB／fileへ保存していない。S1／方式1の保持資源・既存匿名volumeは操作していない。JAR／image／依存cache／点検済みEvidenceはlocal保持する。

| 最終export | 内容 |
| --- | --- |
| archive | `.tmp/d3-sso-exports/d3-sso-s2-20261109-05-evidence.zip` |
| size | 18,241 bytes |
| SHA-256 | `59796d777fa8e6acbfa4115b20eb081c939a47c57144ae14e3b7f31573daa07f` |
| allowlist | manifest／execution／results／cleanup／runtime-redacted.log／browser.json＋archive manifest、7 entries |
| 持帰り状態 | export済み。Windowsの取得・checksum照合は未確認。Cloud download経路の成立も未確認としてOPEN |

project01〜04も別archiveで保持した。生成realm、env、鍵、JAR、DB dump、node_modules、HAR／traceは含めない。各entryとarchiveのchecksum、結果JSON schema、資源残留・secret削除を点検する。補助Evidenceは`.tmp/d3-option2-s2-build-20261109/`の開始manifest、直接回帰内訳、image取得結果、各試行log／export情報、最終runtime点検。

## 7. 限界・次の判断

S2の選択正常／拒否経路の限定成立であり、BFF／Client B SSO・refresh競合・実時間失効・logout保証・障害再起動・Windows再現／手動操作は未実施。Client Session値は設定の照合で、実時間の満了試験ではない。S3開始時にpackage security情報を再確認する。S1で残したimage全OS packageの脆弱性scan未実施・PostgreSQL securityサイトの到達制約も解消扱いにしない。

**実案件への示唆：** managed UUID claimの編集主体と現在DB権限の積を案件の責務として定める。local HTTPの例外を他issuerへ流用せず、通信保護とID Token署名検証のlibrary既定を採用構成に合わせて確認する。IdP subjectとKOIKI UUIDを同一視せず、Cookie実測を一般環境の保証へ広げない。

S2結果を報告して停止する。S3開始・結果Owner受入・成果commit／push／PR／merge、D3／Phase4全体の認定は今回行っていない。

## 8. S2コミットポイントのOwner判断

Ownerは「S2 PASSをコミットします」と指示した。今回のS2 Tooling実装・本結果記録・AGENTSの現行入口をlocal commitとして固定する。実際のSHAは操作後の報告とGit履歴で確認する。上記のcommit未実施表示はこの後続指示前の履歴であり、S3開始・push／PR／merge、D3／Phase4全体の受入を含めない。初回失敗・途中結果・最終成功のEvidenceとWindows持帰り未確認を保持する。
