# B1-08：ローカル確認・Owner受入への入力（2026-10-22）

状態：**自宅PCの選定手動確認・cleanup完了／Owner受入と後続package選定は判断待ち。** Cloud自動検証と自宅PCの手動結果を入力に、§4へDoDの受入案・残課題を整理した。認証中の一覧／詳細間の戻る・進むは単独未確認として保持する。

Owner指示に基づき、検証済みCodex Cloud環境での開発・自動検証と、自宅PC／会社PCでの手動確認を分ける。今回のPCはOwner申告により自宅PC。会社PCでの確認結果は未取得。別PCでの成功を今回のPCの結果として扱わない。

## 1. sourceとCloud入力

- branch：`docs/phase4-review-integrated-plan-20261020`
- 開始HEAD：`bb479138ee845194d9dcc12b4471ffac99fcdf26`。開始時は追跡branchと一致しclean。
- production／test／POM／frontend入力・lockは変更していない。commit・remote操作なし。
- [2026-10-22 Cloud再検証](phase4-b1-07-cloud-revalidation-20261022.md)：通常／同梱各422実行PASS＋99 skip、artifact照合PASS。新環境のbrowser journeyはChromium配信元のHTTP403で未実行。
- [2026-10-21 Cloud統合検証](phase4-b1-07-integrated-browser-results-20261021.md)：当時のJARで新旧journey2件PASS。今回のWindows JARや新Cloudのbrowser成功として流用しない。

## 2. 今回のWindows実測

| 項目 | 結果 |
| --- | --- |
| OS／browser | 自宅PC、Windows 11／PowerShell。Owner提示画面のGoogle Chrome155.0.8059.85（公式ビルド、64ビット）。画面は更新中50%であり、更新後再起動のversionは未確認 |
| Java／Maven | Temurin21.0.12.1、Wrapper Maven3.9.16 |
| Docker | Client29.5.3-rd／Engine29.5.3。通常sandboxのnamed pipe権限拒否後、承認済み権限付き読取で接続確認 |
| Node／npm | 元のPATHは22.20.0／10.9.3。Ownerの実行環境承認後、`target/b1-local-runtime/`だけへNode24.19.0とnpm11.9.0を準備。Node ZIPの公式SHASUMS256照合。OS全体のNode設定は切替なし |
| 同梱package | 固定Node／npmを子processのPATHへ供給して`mvnw.cmd -B -ntp -pl koiki-reference-app -am -Pb1-session-spa -DskipTests clean package`。13 projects SUCCESS、2026-10-22 11:42:25 JST終了。test実行なし |
| 初回停止 | Maven cacheの`exec-maven-plugin`書込でAccessDeniedException。初回logを保持し、同じcommandの権限付き再実行で成功。ソース修正なし |
| JAR SHA-256 | `284548e66ee7b93008b2aab6eef66c59761dc9332efbe207829461b178506df3` |
| 同梱照合 | private build記録のprofile・Node／npm、現frontend入力15件と同梱asset3件のSHA-256一致。source map／node_modules／Vite manifest／frontend sourceの非混入を確認 |
| アプリ起動 | 専用DBへ接続し、migration後`Started ReferenceApplication`を11:43:21 JSTに確認。loopback:18080、heap768MiB、pool4／idle1 |
| 専用DB | `koiki-reference-b1-local-20261022`。`postgres:17-alpine`、`--rm`、loopback:55432、memory1GiB／CPU1／max_connections16。既存の別用途DB2件は変更しない |
| seed | 起動後に既存Toolingを専用DBへ一度実行。user2件／expense3件を読取確認。passwordは対話PowerShellだけへ表示し記録しない |
| HTTP | login200、匿名SPA302。`MaximumRedirection=0`によるPowerShellの停止表示は302の観測であり、ログイン済み操作成功の証拠ではない |

raw・補助script・PIDは`target/b1-local-check-20261022/`。`package.log`が初回、`package-elevated.log`が成功回、`artifact-summary.json`がhash照合、`application.log`が起動記録。これらとJAR・runtimeはGitに含めず、別PCへのpullで移送されない。ログイン情報やCookie値を記録しない。

## 3. 人による確認と受入

[ローカル確認計画](../../development/phase4-b1-07-local-browser-check-plan-20261021.md)の確認表に従う。

| 項目 | 今回の状態 |
| --- | --- |
| Windows固定版同梱build | PASS（§2）。通常root全件の再実行ではない |
| login→ホーム→SPA、狭幅・keyboard focus | PASS（Owner申告）。狭幅でも操作可能、Tabキーでリンク・入力欄・ボタンへ移動した際の選択位置を確認 |
| SPA下書き作成→MVCで同一申請参照 | PASS（Owner申告）。指示した作成・MVC確認操作が実行できたとの回答 |
| 提出→一覧・提出後のMVC参照 | PASS（Owner申告）。SPA下書きを提出し一覧で提出済みを確認。SUBMITTED状態の同一申請のMVC画面も確認 |
| MVC編集時の日付初期値・更新保存→SPA反映 | PASS（Owner申告）。新たな下書きを提出せずMVCで編集し、利用日表示・内容変更・更新・SPAでの変更反映の全手順を確認 |
| 競合案内と最新取得・明示再操作 | PASS（Owner申告）。SPA下書き詳細を表示したまま別windowのMVCで更新し、SPA提出で競合案内と最新内容が表示され、再度明示提出して成功。今回HTTP409自体のDevTools観測は依頼していないため、UI確認とCloudのHTTP409観測を区別 |
| 詳細URLの別tab直接表示・reload | PASS（Owner申告）。同一申請が表示されることを確認。認証中の戻る／進むは別確認 |
| 未知path `/spa/unknown` | PASS（Owner申告）。SPAへ置き換わらず404を確認 |
| tab非表示・MVC遷移後の未保存入力破棄 | PASS（Owner申告）。新規フォームに未保存入力→別tab→元tab、およびMVCへ移動→ブラウザで戻る、の両方で入力非復元を確認 |
| logout後の戻る・reload・別tab反映 | PASS（Owner申告）。SPA詳細からlogout後、戻る・再読込で申請内容が復元されない。同一Chromeの別tabに切り替えると申請内容が消え再loginを求められることを確認 |
| 再login後の保存済み申請参照・未保存入力非復元 | PASS（Owner申告） |
| Cookie属性／CSRF header（値は非記録） | PASS（Owner申告）。SESSIONはHttpOnly・SameSite=Lax・Path=/、XSRF-TOKENはHttpOnlyなし。SPA新規下書き保存POSTのRequest HeadersにX-XSRF-TOKENがあることを確認。値は受領・保存していない |
| 所有アプリ・専用DB停止とport解放 | PASS。2026-10-22 13:14:35 JST最終確認。所有PID・JAR pathとcontainer IDを照合して停止、seed対話windowも終了。専用container自動削除、18080／55432の待受なし。既存DB2件は維持 |
| DoD4-8／4-9、B1-08 Owner受入 | 未判断 |
| 後続package選定 | §4.3のC2-01調査案を提示。Owner未選定、着手なし |

手動操作の結果・browser version・再現手順をOwnerから得て反映する。未実施の確認をPASSにせず、B1-08・Phase4全体の完了に読み替えない。

### Ownerの操作報告（2026-10-22）

自宅PCでログイン成功。指示したSPA新規下書き作成とMVC参照を実施し、SPAで作成した同じ申請がMVCでも確認できたとの報告を受領した。提出・編集保存・競合・logoutなどの結果はこの報告から補完しない。

続いてOwner提示画面で申請がSUBMITTEDとなっていることを確認。編集リンクはDRAFTにだけ表示される実装を説明した。Ownerは別の申請をSPAで下書き保存し、提出せずMVCで開く→編集→利用日初期表示確認→内容変更・更新→SPAで同一申請の変更反映、の5手順を順にすべて確認できたと報告した。競合409・入力破棄・logoutの成功までは含めない。

その後、Ownerは(1)SPA新規フォームへの未保存入力→別tab切替→戻った際の入力破棄、(2)SPA詳細からlogout→戻る・reloadで申請内容非復元、(3)再login後の保存済み申請参照と未保存入力非復元、をすべて問題なく確認したと報告。添付「Chromeについて」の画面に155.0.8059.85／64ビット、更新中50%の表示を確認した。ブラウザの更新や再起動はAgentから指示・実行していない。

さらに、Ownerは(1)詳細URLを別tabで直接表示し再読込して同一申請が表示されること、(2)`/spa/unknown`がSPAへ置き換わらず404となること、(3)狭幅でも操作できTabキー移動時の選択位置が分かること、の3項目についていずれも問題ないと報告した。

OwnerはDevToolsのApplication／CookiesでSESSIONのHttpOnly・SameSite=Lax・Path=/と、XSRF-TOKENのHttpOnlyなしを確認し、NetworkでSPA新規下書き保存POSTのX-XSRF-TOKENヘッダーの存在を確認したと報告。Cookie／ヘッダーの値やそのスクリーンショットは受領していない。この確認は正常POSTの属性・ヘッダー存在の目視確認であり、CSRF欠落・誤値の拒否を今回新たに実行した結果ではない。

Ownerは続く3項目、(1)SPA下書き提出→一覧でも提出済み、(2)SPA新規フォームへの未保存入力→MVCへ移動→ブラウザで戻る際の入力非復元、(3)同一Chromeの2tabで一方のlogout後もう一方へ切替時に申請内容が消え再loginを求められること、をいずれも問題なく確認したと報告した。

最後にOwnerは、SPA下書き詳細を表示→別windowのMVCで同一下書きを更新→SPAで再読込せず提出→「申請の状態が変わりました。最新の内容を確認してから操作してください。」とMVC更新内容の表示→明示的な再提出成功、の5手順すべてを問題なく確認したと報告した。

cleanupは通常sandboxのCIM読取拒否後、実行環境承認を得て所有者照合と停止を実施した。専用DB停止直後は残存確認で停止し、追加削除はせず後続読取で自動削除完了を確認。`cleanup-summary.json`へ最終状態を保存した。JVM／seed processとDBの終了を確認したもので、graceful shutdown保証の検証ではない。環境変数は起動用子PowerShellにだけ設定しており、親OSのNode切替・永続環境変数変更はない。手動確認用DBのデータ・ログインpasswordは再利用しない。

## 4. Owner受入への提案（未承認）

### 4.1 DoDのEvidence対応

| 対象 | 自動検証の入力 | 今回の自宅PC手動結果 | 受入案 |
| --- | --- | --- | --- |
| DoD4-8：Cookie Session／CSRF double-submit | B1-05の149件と新Cloud通常／同梱各422件。認証・CSRF欠落／誤値拒否・失効は当該test。旧Cloud B1-07は実browserでCookie／CSRF・失効を実演 | 実login、SESSION／XSRF属性、POSTヘッダー、logout／他tab・再login、表示／入力破棄 | 同梱Reference・同一origin Session SPAの選定構成に限った受入候補 |
| DoD4-9：MVC／SPA併存・経路別CSRF・既存Bearer保護 | 新Cloud通常／同梱各422件、旧Cloud同一JARの新旧journey2件。新旧Cloudと今回WindowsのJAR hashを別管理 | 同じWindows同梱JAR上でSPA作成→MVC参照・編集→SPA反映・競合回復、未知path404 | 同じReference JAR上の選定経路の受入候補。今回手動でBearer／CSRF負例を再実行したとはしない |

### 4.2 利用条件・残課題

- WindowsはNode24.19.0／npm11.9.0をbuild時だけ用い、`b1-session-spa` profileで同梱する。通常buildはSPA非同梱。再現手順は[ローカル確認計画](../../development/phase4-b1-07-local-browser-check-plan-20261021.md)と[frontend README](../../../koiki-reference-app/frontend/README.md)を入口とする。
- 手動確認は自宅PC／Chrome155.0.8059.85の今回JARに限定。会社PC、更新後Chrome、認証中の一覧／詳細間の戻る・進む単独操作は未確認。戻る操作はlogout後の非復元・MVCから戻る際の未保存入力破棄として確認済み。
- 新Cloudのbrowser journeyはChromium配信元HTTP403で未実行。配信元許可または指定版の承認済みcacheが必要。2026-10-21の成功を新環境の実行成功として扱わない。
- ローカルHTTPのSecure=false、専用使い捨てDB・Tooling seedは今回の手動環境条件。HTTPS配備や正式Identity provisioning、Customer BFF、実IdP、正式受渡しの成立を含まない。
- S1・F6／F7／H1等の別管理課題、Phase4全体・正式release・remoteの判断は維持。今回の選定手動確認・cleanupをOwner受入そのものに読み替えない。

### 4.3 後続package案

[統合計画§5.2](../../development/phase4-review-integrated-task-plan-20261020.md#52-各テーマの作業単位と着手条件)のP4-C2から、**C2-01：代表外部操作と失敗契約の調査・設計**を次候補として提案する。実案件／IdP／platform入力待ちのテーマより先に、Reference／Toolingで対象操作・副作用・timeout／retry上限・受理不明時の扱いを整理できるため。対象操作は未選定であり、実provider・通信やlibrary追加・実装は含めず、調査結果から別途範囲を具体化する。S1の再開を前提にしない。

Ownerには、(1)上記Evidenceと残課題を明示したB1の限定受入、(2)後続候補としてC2-01調査を選ぶか、を判断いただく。承認記録・commit・remoteは未実施。
