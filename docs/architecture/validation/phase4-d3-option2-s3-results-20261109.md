# D3方式2 S3：Client A BFFのCloud限定結果（2026-11-09）

## 1. 判定・範囲

**S3限定技術PASS。** Ownerの「S3へ進めます」により、S2固定commit `445681ce49267b208ed30ea5efdc85215dd088a5`から、[段階別計画§5](../../development/phase4-d3-option2-staged-task-plan-20261109.md#5-s3bffの最小縦断client-a)のS3-T01〜03をTooling配下に実装した。新規project `d3-sso-s3-20261109-01`でS3-V01〜05が成立し、自動cleanup・生成secret／realm削除・証拠exportまで成功した。

Framework／Reference main／POM・migration・Root Reactor／CI・方式1資材は変更していない。JavaのS2成果を再利用し、BFF接続をBrowser→API→DB／Auditでsmoke確認した。S2の直接回帰23件やS1全集合を反復していない。S4・S5・Windows手動確認・結果Owner受入・commit／remoteは別判断のまま維持する。

## 2. Source・環境・artifact

| 項目 | 今回の値 |
| --- | --- |
| branch／開始source | `docs/d3-option2-implementation-conditions-20261109`／`445681c`、開始時clean。その上の今回Tooling差分hashをprepareで固定 |
| host／toolchain | S2と同じDebian13／Linux amd64、cgroup実効4 CPU／16 GiB。Engine28.4.0、Compose2.40.3、Node24.19.0／npm11.9.0、Next16.4.5、Playwright1.62.0／Chromium151.0.7922.34 revision1234 |
| package security | S3開始時の同じlockに対するnpm auditは全severity0。package／lock版は変更していない |
| Java JAR | S2のSHA-256 `43db9951ce99ccd53d3181e9f8ea9f03f66317ebc15b704265fa129da3493986`を再照合して再利用 |
| backend image | S2の`sha256:0a559688e4aaa5ac70a7406cb96493ca29dacfe9ab01c101de6058ff8451e26f`、project専用tagへ識別付け |
| frontend context hash | `78b0680bcca68e4712b94e07f0cbcfea027a91284883cdeba608be4c2640d42a`、host生成standalone／static等のfile inventoryに基づく |
| frontend image | `sha256:ec1f72ff8e97424de8b96e78f0163f479c470f2dcb318cdcfac05fa24269fdc8`、context hash labelと起動imageを対応付け |
| project／work | `d3-sso-s3-20261109-01`／`.tmp/d3-sso-s3-20261109-01/` |
| 証拠・環境設定 | `.tmp/d3-option2-s3-build-20261109/`、既存のCloud専用CA／cache設定を使用。TLS検証の無効化なし |

runtimeはbackend1.5GiB／1CPU、PG1GiB／1CPU、Keycloak1.5GiB／1CPU、BFF1GiB／1CPU。実効CPU4のため、0.5CPU／512MiBの一時probeをfrontend未起動の状態で実行し、終了・削除してからBFFを起動した。probeと常設4サービスの同時起動は0回で、5サービス計4.5CPU構成の成立とは数えない。BFF readiness自身も内部IdP／backend／DB／JWKSを観測する。Browserはhost側1 worker／同時context1であり、最大負荷やOSのCPU余裕の保証ではない。

buildとruntimeは非並走。build／取得枠40分・動的50分（cleanup10分予約）に対し、動的runnerは113.806秒でcleanupまで終了した。

## 3. 実装・Session契約

| S3対象 | 実装 |
| --- | --- |
| server store／暗号化 | `config.ts`／`crypto.ts`／`store.ts`。BFF DBだけを使用し、AES-256-GCM・IV12byte／tag16byte、client／用途／record hashのAAD、外部32byte鍵を使う |
| 認証途中 | HttpOnlyの事前Cookieに結び付けたstate hashと暗号化nonce／PKCEを保存。row lock下で一回消費をcommitしてからtoken交換し、交換結果不明を再送しない |
| Session | 256-bit opaque IDを新規発行しDBはhash保存。検証済みID Tokenのissuer／subject、CSRF hash、暗号化token・期限・versionを保持。Browserへtokenを渡さない |
| 期限 | idle15分／absolute60分、取得時idle／last_seen／versionを更新。absoluteは延長せず、Access Token期限を`expires_in−5秒`で保守的に拒否。refreshは未実装、期限到達は再認証 |
| Browser境界 | 認証開始・logout・業務更新はJSON POST＋固定Origin＋CSRF。body64KiB上限、callbackだけGET＋state／binding／nonce／PKCE／ID Token JWS検証 |
| API／画面 | 固定3操作だけをJavaへproxy。本人SSR表示とClient Component入力、SSR／RSC／APIのno-store。上流Set-CookieやBrowserの認証headerを転送しない |
| 起動 | Node standalone image、user10001、read-only、tmpfs限定書込み。暗号鍵欠落時はNext起動前にexit1 |

RP `a`だけを許可する。Client IDはS2で固定した`koiki-d3-a`を継承し、canonical issuer／callbackは変更していない。Cookieは`D3_A_SESSION`、Path `/a`、HttpOnly／SameSite=Lax／Domain指定なし、local HTTPのためSecure=false。初回Sessionを認証後Sessionへ昇格させず、新しいIDを発行する。

BFF3 tableのうちauth transactionは2件が一回消費済み／未消費0。初回Sessionはlogoutで削除、再認証で発行した別Sessionはidle期限のtest-owned更新後にAPI拒否・削除を確認し、最終Sessionは0件。nonce／PKCEの復号が実token交換で、tokenの復号が実API成功で成立した。平文token／Cookie値・鍵・code／verifierは証拠へ保存していない。

## 4. 検証結果

| 集合／ID | 結果 |
| --- | --- |
| 単体 | 既存OIDC境界3件、新規S3境界18件、共有所有物guard5件がPASS。暗号改竄／AAD・key・CSRF／Origin・stream上限・固定API先・期限境界を含む |
| S3-V01 | PASS。BFFからdiscovery／JWKS、実Browser login、JWS／nonce／PKCE、Session CookieとJS非公開、本人subjectのSSR、Session更新・期限検査 |
| S3-V02 | PASS。Browser作成201→本人詳細200→提出204→reload、SUBMITTED:1200、本人UUID・明細1件合計1200・提出成功Audit1件を照合。業務POSTの自動再送なし |
| S3-V03 | PASS。下表の14条件、state不一致／callback再利用、local logoutのCookie／Session削除、idle期限拒否と削除 |
| S3-V04 | PASS。SSR／RSC／APIのno-store、HTML／RSC・client bundle7件にJWT／opaque Session IDなし。暗号鍵欠落の別1構成はexit1でサーバー起動拒否 |
| S3-V05 | PASS。境界testの記録、今回所有資源cleanup・生成secret／realm削除、redacted証拠保存 |

| 条件 | 実HTTP |
| --- | --- |
| 許可外return parameter | 400 |
| login CSRF欠落 | 403 |
| state不一致／callback再利用 | 各400 |
| API CSRF欠落／誤値／異Origin | 各403 |
| JSONでないContent-Type | 415 |
| body64KiB超過 | 413 |
| 任意proxy先／未提供RP b | 各404 |
| logout CSRF欠落 | 403 |
| logout後API／idle期限切れAPI | 各401 |

正常操作は実Chromiumの画面入力・clickで行い、負例の一部は同じBrowserContextに結び付くHTTP requestで正確なOrigin／headerを供給した。異Origin拒否を別origin画面の実操作とは記録しない。callback再利用では元の事前Cookieを復元した要求も拒否し、DBで消費済み2件を確認した。

RSC実応答のContent-Typeは`text/x-component`。本人表示のsubmitted画面・1200円は`spa.png`でも点検した。画面・bundle／RSCに秘密がないことは今回の7 bundleと選択経路の観測であり、全将来画面の保証ではない。

Session更新のDB観測はversion1以上・last_seen>created、absolute−created=3600秒、AES-GCM／IV12／tag16。実idle拒否は二つ目のtest Sessionだけの時刻を過去へ移した有限fixtureによる。絶対期限／Access Token期限は注入clockの単体境界で確認し、実時間15分／60分／token期限待機の試験とは区別する。S5の実JWT失効試験を代替しない。

## 5. 訂正・実行回数・ログ

最初のNode test読込みでTypeScript parameter propertyがnative strip-only mode非対応と判明し、新規S3 test fileが読込停止した。既存OIDC3件は成功、新規18件は未実行だった。`BoundaryError`を通常のproperty定義へ訂正し、新規18件を1回実行して全PASS。初回logを保持した。Next buildは初版成功後、auth開始／logoutのbodyを空objectへ限定する変更を加えて再buildし、最終版もTypeScript検査を含め成功した。

frontend image build1回、鍵欠落の予定負例1回、S3 runtime／Browser集合1回。動的失敗・動的再実行はない。実行後のBrowser失敗log出力は、provider URL／資格情報の混入を避ける固定文言へ限定した。これは失敗時診断文言だけの変更で、実証済みの正の経路・assertionは変更していない。README／結果／入口も実行後に追記し、実行時snapshotのhashとは区別する。

backendログはINFO45／WARN2／ERROR0、WARNは既存認証provider／cache統計。BFFはNext16.4.5起動・ReadyだけでERRORなし。Keycloak ERRORなし。DBのERROR2件はS2と同じ初期製品probeのmigration table／changelog lock不存在で、その後のmigration／realm／ready／業務検証が成立した。Session属性重複キー例外・生成secret値／JWTのlog露出は検出されていない。

## 6. cleanup・export

常設4 container・専用network／DB volumeは自動cleanup済み。一時probeと鍵欠落one-offも残留なし。生成secret9 fileと生成realmを削除し、S1／S2／方式1のEvidence・旧匿名volumeは操作していない。image／JAR／依存・build cache／redacted Evidenceを保持する。

| export | 内容 |
| --- | --- |
| archive | `.tmp/d3-sso-exports/d3-sso-s3-20261109-01-evidence.zip` |
| size | 19,306 bytes |
| SHA-256 | `6f8dd0de095ea0da8941b1153b91dfe59115e80e9695bee6ea8fa74ee9a92ac0` |
| allowlist | manifest／execution／results／cleanup／runtime-redacted.log／browser.json＋archive manifest、7 entries |
| 持帰り | export済み。Windows取得・checksum照合は未確認、Cloud download経路の成立も未確認としてOPEN |

archiveにJAR／image／DB dump／secret／生成realm／env／HAR／traceは含めない。画面画像はworkで別保持しarchiveに含めていない。補助Evidenceは開始manifest、audit、初回／訂正後unit log、共有guard、2回のNext build log、image build log、runtime review／export情報。archive内のchecksumと結果JSON schema・cleanup残留を照合する。

## 7. 限界・次の判断

S3のClient A限定成立であり、Client B・独立2 ClientのSSO／logout保証、refresh／並行更新、IdP／backend／DB障害・終了drain、Windows再現／Owner手動操作は未実施。暗号鍵はprojectごとの単一外部鍵で、鍵rotation・外部secret manager・本番TLSは未実証。OIDCの再認証で同じIdP Sessionが利用されても、S4の2 RP SSOの成立へ読み替えない。

**実案件への示唆：** local HTTPのSecure=false、Session／token期限・キー管理を案件条件へ置き換える。Node／library既定の暗黙挙動に依存せず、body上限・固定Origin・client結付け・一回消費を明示する。Cookie Pathは強い隔離境界ではなく、未提供RPはserver側でも拒否する。

S3結果を報告して停止する。S4開始・結果Owner受入・commit／push／PR／merge、D3／Phase4全体の認定は今回行っていない。

## 8. S3コミットポイントのOwner判断

Ownerは「S3 PASSのここで、コミットしましょう」と指示した。今回のS3 Tooling実装・本結果記録・AGENTSの現行入口をlocal commitとして固定する。実際のSHAは操作後の報告とGit履歴で確認する。上記のcommit未実施表示はこの後続指示前の履歴であり、S4開始・push／PR／merge、D3／Phase4全体の受入を含めない。検証経緯・Evidence・Windows持帰り未確認を保持する。
