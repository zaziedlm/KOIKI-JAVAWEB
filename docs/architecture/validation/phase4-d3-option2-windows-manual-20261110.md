# D3方式2：Windows Owner手動ブラウザ確認（2026-11-10）

## 1. 指示・位置づけ

Ownerの「方式2の手動環境起動・Owner打鍵を進めます」に従い、このbranchの`8517ca5`を入力として専用環境を準備する。Tooling-owned。方式1の[過去の手動確認](phase4-d3-home-container-reverification-20261107.md#7-後続owner指示による手動確認環境の準備)と同じく、専用project準備→Owner操作→log／DB／Audit読取点検を分ける。手動操作と結果全体受入は別に記録する。commit／push・PR／mergeは今回実施しない。

方式2の画面は作成・本人詳細・提出の3操作とA／BのSSO／logoutを提供する。方式1のSPA／MVC・差戻し・再編集・承認の一連操作は本画面の対象外。

## 2. 専用環境

- project／work：`d3-sso-s5-20261110-manual01`／`.tmp/d3-sso-s5-20261110-manual01/`
- Client A：`http://localhost:13000/a`
- Client B：`http://localhost:13000/b`
- IdP：`http://idp.localhost:18080/realms/koiki-d3`
- 利用者：`applicant-a`、`applicant-b`。共通の新規生成passwordはworkの`secrets/login_password`をローカルで参照。値をGit・共有文書へ転記しない。
- 常設4サービス：frontend／backend／Keycloak／DB。backend／DBはhost非公開、frontend／IdPはloopbackのみ。
- 自動検証用DB／鍵は移植せず新規生成。DB seedは初回1回だけ。打鍵後も指示なしに停止・cleanup・再seedしない。再開時は既存DBを初期化しない。

方式1は`127.0.0.1:18081`を使ったが、本方式は登録済みcallback／Originに合わせ、打鍵URLを必ず`localhost:13000`へ統一する。

## 3. 打鍵順と期待結果

1. 新しいブラウザprofileでAを開く。「ログイン」から`applicant-a`でIdP認証する。「ログイン済み」と作成formが表示される。
2. 例として金額1,300円、内容「方式2手動A」、目的「手動確認」を入力し「下書きを保存」。`DRAFT`と1,300円を確認し、URLを控える。「この申請を提出」で`SUBMITTED`、reload後も同じ内容が表示される。
3. 同じprofileの別tabでBを開く。最初は匿名。「SSOでログイン」で資格情報再入力なしにログインできる。Bで別申請（例2,300円、内容「方式2手動B」）を保存・提出する。AとBは独立Sessionだが同じ利用者。
4. Aの「ログアウト」でAが匿名になる。Bをreloadしログイン継続を確認。Aの「SSOでログイン」で復帰できる。
5. Aの「SSOログアウト」でIdP確認画面を操作。新しいAの「SSOでログイン」は再認証が必要になる。Bの既存Session／JWTは即時失効を保証しないため、Bの画面だけでSSO logout失敗とは判定しない。
6. 別のブラウザprofileで`applicant-b`としてAへログインし、例3,300円の申請を作成・提出する。最初のprofileの申請URLを別profileで開いても他人の詳細が表示されないことを確認する。

同一profileのA／BはIdP SSOを共有する。2利用者の分離には別profileを用い、tabだけで切り替えない。操作エラーや提出結果不明時は連打せず、URL・表示・操作順を報告してDB点検する。Cookie／token／code付きcallback URLは報告しない。

## 4. 準備と記録

このPCでは方式2の生成物がなく新規build。固定Node24.19.0／npm11.9.0、Java21.0.13を使用。Java直接回帰23件・Node39件・共有guard5件は全成功、skip0。`npm ci --ignore-scripts`でaudit 0、Next build・backend／frontend image build成功。

JAR SHA-256：`7c3c54dcc21aa03d522b68d9a475e06e7c7d4af0c3591e71cc9f35b95e378d72`。Java build記録は`.tmp/d3-sso-manual-build-20261110-elevated/`。手動起動用に`start-manual.py`を追加したため実行sourceはHEAD＋同Tooling未commit差分。prepare manifestのsource hashで追跡する。

初回sandbox Mavenはnetwork permission denied、npmはcache書込等で未完了。承認手順で権限付き実行後に上記成功。初回起動入口はcp932でUTF-8 manifestを読めず、container作成前に停止。`PYTHONUTF8=1`で明示する。固定版や判定条件は変更していない。

起動実測はworkの`manual-ready.json`／`manual-state.json`／`manual-services.json`で確認する。現時点のOwner打鍵・操作後DB／Audit点検・結果受入は未実施。

## 5. 起動実測

2026-11-10 10:57 JSTにREADY。readiness／A／BのHTTP200、常設4サービス稼働、seed適用1回、BFF migration1／2を確認。操作待ちのため自動cleanupしない。frontend context SHA-256は`88cb059449822adc82f4e8fb09563b11b8e54ed147e6f931c139e9570d313af4`。

image build cacheとは別に固定Node imageをDockerへpullしてpreflightの識別条件を満たした。起動前に匿名page確認のCookie追跡を追加し、manifestの当該source hashだけを補正。旧／新hash・理由は`manual-preflight-amendment.json`へ保存した。通常S5障害注入・JWT期限待ち・停止回復集合は再実行していない。起動時のHTTP確認は匿名画面の到達性であり、Owner login・打鍵成立はまだ確認していない。

## 6. Owner初回loginエラー報告後の読取点検（11:11 JST）

OwnerはAの対話login後に認証エラーJSONが出たが、再loginするとログイン済みの作成画面になったと報告し、同画面を提示した。業務作成・提出前の途中点検として、専用projectだけのlog／認証DBを読取確認した。環境再起動・Session変更・DB更新は行っていない。

- Keycloakの`koiki-d3-a` LOGIN成功は11:06:42／11:06:59／11:07:37の3件。LOGIN_ERRORなどの失敗eventは保存されていない。token交換eventは今回の設定の保存対象に含まれないため、LOGIN成功だけでtoken交換成立とはしない。
- BFFの初回認証transactionは11:03:43作成・11:08:43期限、未消費。後のtransactionは11:07:21作成・11:07:37消費済み。最初のtransactionを前2件のIdP eventに厳密に対応させる相関IDは今回取得していない。
- AのBFF Sessionは1件、idle／absolute有効・refresh_pending=false。BのSessionは0件。frontend／backend／Keycloak／DBは全healthy。
- frontendは起動logのみでcallbackの失敗理由を記録していない。backendの起動時WARN、DBのKeycloak初期schema存在確認時ERRORはlogin発生より前で、今回の原因と結び付く証拠はない。

実装では匿名画面のpreflightで発行するflow Cookieが300秒、DB transactionはlogin開始から5分。login開始時には既存flow Cookieの期限を延長しない。このため、画面を開いてから待機し、その後IdP入力中にCookieだけが期限切れになると、DB transactionの期限内でもcallbackで`invalid_auth_transaction`となり得る。Cookie欠落・別tabでのbinding置換／state不一致等も未消費を生む。**Cookie期限のずれは実装上の候補であり、当該試行の原因確定ではない。** ブラウザの実際のerror名・Cookie期限を記録していないため初回原因は未確定。生Cookie／token／callback URLは採取していない。

証拠はworkの`login-inspection-20261110-111106/summary.json`と各serviceのsanitized log。Ownerの現在のA login成立と初回の問題を分離し、手動全体PASSや「再loginできたので問題解消」とは扱わない。現在の環境を保持し、以後の業務／SSO確認と初回問題の原因確認を追跡する。

## 7. Owner打鍵報告後の最終読取点検（2026-11-10 12:18 JST）

Ownerは検証範囲を理解してブラウザ操作とAccess Token約60秒の期限後挙動を確認し、画面操作上の問題なしと報告した。12:18:55 JSTから専用projectの4サービスlog、申請／Audit、BFF／IdP DBを読取点検した。各DBの観測は`REPEATABLE READ READ ONLY`、statement上限10秒。DB間同時snapshotではない。HTTP業務要求・DB更新・Session失効操作・停止／再起動・cleanupは行っていない。

| 対象 | 実測 |
| --- | --- |
| 申請 | 5件、全件`SUBMITTED`／version2、全件applicant-a所有 |
| 金額 | 作成順1,200／1,205／1,200／1,202／1,200円。各明細1件、全申請で明細合計と申請額が一致 |
| Business Audit | `SUBMIT_EXPENSE / SUCCESS`5件。全申請に各1件、actorと申請者一致。保存された失敗Business Auditなし |
| 認証・更新 | A／B LOGIN、A／B REFRESH_TOKEN成功を確認 |
| SSO logout後 | A LOGOUT 11:22:52→B REFRESH_TOKEN_ERROR 11:22:55、11:49:07→11:49:30、11:55:56→12:03:19。各`invalid_token / Session not active` |
| 最終BFF Session | A／Bとも0件、refresh_pending残留なし |
| 認証途中record | 期限切れ5件（A消費済3、B消費済1／未消費1）。login開始時の有限purge対象であり、常駐purgeはない。今回削除しない |
| BFF migration | 1／2のchecksum記録あり |
| 稼働 | frontend／backend／Keycloak／DBすべてhealthy、restart各0、OOM各false |

frontend／backendに操作時の新しいERROR／例外記録なし。DBのERROR2件は10:56〜10:57のKeycloak初期化時schema存在確認で、操作中の新規ERRORなし。Keycloakのrefresh拒否3件はSSO logout後の期待される観測。11:44:17にAの`LOGIN_ERROR / user_not_found`1件があり、11:44:33にはLOGIN成功。入力内容は取得せず、これはSSO logout後の拒否や初回callback問題とは別の認証失敗として記録する。

Ownerは初回JSONに`_auth_transaction`の記述があったと記憶しており、`invalid_auth_transaction`と整合するが正確な値は未採取。Keycloakは10:56台healthy、初回LOGINは11:06台のため起動直後という根拠は薄い。§6のCookie期限／binding等の候補は未確定・未修正の残件として保持する。

IdP eventには保存期限3,600秒があり、今回の一覧は§6より古いeventが失われ得る。§6の証拠を保持し、初回記録が一覧にないことを障害解消とはしない。今回のDBからは全申請がapplicant-a所有と確認できるが、A／Bのどちらの画面で作成したかやapplicant-bでの他人詳細拒否までは識別できない。後者の手動実施を独立に証明したとはしない。DB時刻・event順だけでAccess Tokenを正確に60秒待ったことは証明せず、Owner観測と期限後のrefresh拒否を分ける。

**結論：Ownerが確認した画面操作と、申請／明細／提出Audit・logout後refresh拒否の限定照合は成立。** 初回認証エラー原因と未確認の利用者分離を残し、全ケースPASS・D3／Phase4全体受入へ拡張しない。環境・DB・鍵は引き続き保持し、commit／pushは未実施。

証拠：`.tmp/d3-sso-s5-20261110-manual01/final-inspection-20261110-121855/summary.json`と4サービスsanitized log。読取補助scriptは`.tmp/inspect-option2-final.py`。Cookie／token／認証payload／Session ID値は採取対象外、log内の関連識別子も保存時に除去した。`manual-state.json`へOwner報告・点検済み・残件を反映し、`manual-ready.json`は起動時履歴として保持する。

## 8. 別プロファイルClient B追加操作後の点検（12:29 JST）

Ownerは「Client Bを別プロファイルで申請保存」と報告。12:29:40 JSTから同じ読取手順で点検した。前回5件から2件追加、合計7件。追加は12:25:52保存／12:25:54提出の1,205円（`1150ced1-ce89-46b9-9c6d-b9de2c49ab38`）、12:26:06保存／12:26:07提出の1,200円（`c83796cb-ccb9-40c4-a8c5-555947cfcc6d`）。両件SUBMITTED／version2、各明細1件・金額一致、各提出SUCCESS Audit1件・actor整合。

**両件の申請者はapplicant-a（sso-a@example.test）であり、applicant-b所有の申請は0件。** Client BというRP名とapplicant-bという利用者名を区別し、別profileで実際に入力した利用者をOwnerへ確認中。今回の結果だけで利用者誤結付けと断定せず、applicant-b確認済みにもしない。

12:25:00 A LOGIN→12:25:11 B LOGIN、A／B refresh成功、12:26:34 B LOGOUT→12:28:25 A REFRESH_TOKEN_ERROR（invalid_token／Session not active）を確認。以前と逆方向のlogout後refresh拒否も観測した。最終BFF Session0、4サービスhealthy・restart0／OOMなし、frontend／backend／DBに操作中の新規ERRORなし。初回認証問題の残件と環境保持を継続する。

証拠はworkの`final-inspection-20261110-122940/summary.json`とsanitized log。前回証拠は保持し、今回もDB更新・Session変更・再起動・cleanupなし。

## 9. Chrome／Edge・別利用者の再試行と点検（12:45 JST）

Ownerは前回のprofile設定誤りの可能性を報告し、ChromeシークレットとEdge InPrivateで再試行した。applicant-bの申請保存・提出、ブラウザごとに独立した認証が必要なこととlogout可能なことを確認したと報告。12:45:37 JSTから専用projectを§7と同じ読取手順で点検し、最新IdP eventと利用者名の対応だけを追加照合した。環境・Session・DBの変更はしていない。

| 利用者 | 今回追加の申請 | 保存／提出時刻（JST） | 整合 |
| --- | --- | --- | --- |
| applicant-a | 1,208円、`22d7ff50-022f-4b93-88ed-9c6fe3ca6c57` | 12:34:57／12:34:59 | SUBMITTED／version2、明細1件・合計一致、提出SUCCESS Audit1件・actor一致 |
| applicant-b | 1,209円、`a5fc77c7-7536-4ef9-9596-ebf52a4c00bf` | 12:36:19／12:36:21 | SUBMITTED／version2、明細1件・合計一致、提出SUCCESS Audit1件・actor一致 |

前回7件から合計9件へ増加。applicant-a所有8件、applicant-b所有1件。全9件が提出済み、金額と明細合計一致・各提出成功Audit1件。applicant-bのDB利用者は`d3200000-0000-4000-8000-000000000002`／`sso-b@example.test`であり、提出Auditのactorも同ID。§8の「applicant-b所有0件」はその点検時点の履歴として保持する。

IdPのevent.user_idとuser_entityを読取JOINした結果、以下の利用者／RP対応が成立した。

1. 12:34:39 `koiki-d3-a / applicant-a` LOGIN。
2. 12:36:07 `koiki-d3-b / applicant-b` LOGIN。
3. 12:36:31 A／applicant-a REFRESH_TOKEN成功、12:36:39 同LOGOUT。
4. **A logout後の12:40:46にもB／applicant-b REFRESH_TOKEN成功**、12:41:02 同LOGOUT。

これはOwner報告の別ブラウザ認証分離と整合する。IdP eventだけではprompt指定やブラウザ種類を識別しないため、Chrome／Edgeの操作方法はOwner報告、利用者対応とlogout後の更新継続はDB観測として区別する。

最終BFF SessionはA／Bとも0件。常設4サービスはhealthy、restart0／OOMなし。前回点検以降に新たなアプリ例外・Keycloak警告・DB ERRORなし。期限切れ認証途中recordは今回も削除しない。

**別利用者applicant-bの保存・提出／Audit照合と、別ブラウザの認証・logout分離に関するOwner報告との限定整合を確認した。** 手動で相互の申請URLを開いて他人詳細が拒否される操作は今回報告されていないため、その負例を手動PASSとはしない。初回認証transactionエラーの原因未確定も継続。結果全体受入・D3／Phase4全体認定・commit／pushへは拡張しない。

証拠：workの`final-inspection-20261110-124537/summary.json`、4サービスsanitized log、`user-event-correspondence.json`。読取補助は`.tmp/inspect-option2-final.py`／`.tmp/inspect-option2-user-events.py`。`manual-state.json`の点検先と残件を更新し、環境・volume・鍵・realmは引き続き稼働保持する。
