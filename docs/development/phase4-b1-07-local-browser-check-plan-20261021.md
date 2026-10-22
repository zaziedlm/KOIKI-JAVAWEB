# B1-07：ローカルPCでの実ブラウザー手動確認計画

状態：**自宅PCの選定ブラウザ検証結果はCOMPLETE / OWNER ACCEPTED（2026-10-22）、cleanup完了**。[承認記録§5](../architecture/validation/phase4-b1-08-local-acceptance-input-20261022.md#5-選定ブラウザ検証結果のowner受入2026-10-22)へ追跡する。DoD全体受入・B1-08全体完了・後続package選定は別判断。認証中の一覧／詳細間の戻る・進む単独操作は未確認として保持する。Cloudの自動Chromium検証と区別する。
作業branchは`docs/phase4-review-integrated-plan-20261020`、PRを作らない。Cloud成果のcommit／pushとPC側pullは必要時にOwner指示に基づいて行い、現在の作業指示でpush済みとはしない。

## 1. 引継ぎ・build

1. Cloud結果と未解決事項を点検し、成果commit／push後に、PC側の未commit変更を確認する。cleanな同branchで`git pull --ff-only`し、Cloudの引継ぎcommitと一致することを記録する。別の変更がある場合は上書きしない。
2. JDK21、Wrapper、Docker、**Node24.19.0／npm11.9.0**を確認する。B1-06のWindows用npm起動分岐はCloud上で実行確認できないため、このbuildを独立した確認項目とする。
3. `mvnw.cmd -pl koiki-reference-app -am -Pb1-session-spa -DskipTests clean package`で同梱JARを生成し、exit code・commit・JAR SHA-256を記録する。これはPC側package確認であり、Cloud全件verifyの反復や代用ではない。Node／lock不一致を自動修正しない。

## 2. 手動起動

既存の[ローカル手動起動ガイド](../reference/KOIKI-JavaWeb-FW_Reference_Application_Local_Run_Guide_v0.1.md)§4〜9を基本にする。§5の通常packageだけは上記B1 profile buildに置き換える。通常Session構成で起動し、`api-bearer`は不要。HTTP loopbackの同じ起動用PowerShellへ、§6の環境設定に加えて以下を明示する。

```powershell
$env:SERVER_SERVLET_SESSION_COOKIE_SECURE = 'false'
$env:SPRING_SESSION_JDBC_INITIALIZE_SCHEMA = 'never'
$env:KOIKI_IDENTITY_LOCAL_AUTHENTICATION_ENABLED = 'true'
java -jar .\koiki-reference-app\target\koiki-reference-app-0.1.0-SNAPSHOT.jar
```

Secure=falseはこのローカルHTTPだけの設定。起動完了後、別PowerShellで既存の
[使い捨てdemo seed](../../build-support/reference-local-demo/README.md)を一度実行する。
表示された今回限りのpasswordでloginし、ホームのSPAリンクまたは`http://127.0.0.1:18080/spa/`へ進む。
既存DBを流用しない。password／Cookie／tokenをスクリーンショット、HAR、共有logへ保存しない。

## 3. 人が操作する確認表

| 項目 | 操作・期待 | 結果 |
| --- | --- | --- |
| Windows build | Node／npm固定versionで同梱build成功、入力・lockに意図しない変更なし | 2026-10-22 自宅PC PASS。Node24.19.0／npm11.9.0、13 projects SUCCESS、入力・asset hash照合済み |
| 導線・画面 | login→ホーム→SPA、desktop／狭幅で文字・表・入力・keyboard focusを確認 | login・SPA利用、狭幅操作・Tabキー移動時の選択位置をOwner確認済み |
| 下書き・提出 | SPAで1明細作成、金額・利用日・文字表示、MVC編集の利用日初期値が空欄にならないこと、詳細→提出→一覧を確認 | SPA下書き作成・MVC編集時の利用日初期表示・更新保存、SPA提出→一覧で提出済みをOwner確認済み。MVCのSUBMITTED表示も確認 |
| MVC相互参照 | 同じ申請をMVCで参照。SPAと別windowのMVCを同時表示し、MVCで下書き編集→古いSPAから提出で409→最新表示→明示再操作。tab切替では再表示時の自動再取得で既に最新になる場合も正常 | MVC参照・編集反映、および古いSPAからの提出時の競合案内→最新内容→明示再提出成功をOwner確認済み。今回HTTP409の直接観測は未依頼 |
| 経路 | 詳細URL直接表示、reload、戻る／進むで現在の状態を取得。未知pathは404 | 詳細URLの別tab直接表示・reloadと`/spa/unknown`の404をOwner確認済み。認証中の戻る／進むは未確認 |
| 入力破棄 | 新規フォーム入力中にMVC移動→戻る、別tab切替→再表示で未保存入力を再表示しない | MVC移動→戻る、別tab切替→戻る、双方で未保存入力非復元をOwner確認済み |
| logout・他tab | SPA logout後に表示／入力が消え、別tabも再表示時に未認証となる。戻る／reloadで申請を復元しない | logout後の戻る／reloadで内容非復元、別tab切替時の内容破棄と再login要求をOwner確認済み |
| 再login | 再login後に権限のある現在データだけを取得。前Sessionの未保存入力は復元しない | 保存済み申請の参照・未保存入力非復元をOwner確認済み |
| Cookie／CSRF | DevToolsでSESSIONはHttpOnly／Lax／Path=/、XSRFはJS可読。POSTにX-XSRF-TOKEN。値は記録しない | Owner確認済み。SESSION属性、XSRF-TOKENのHttpOnlyなし、SPA下書き保存POSTのヘッダー存在を確認。値は非記録 |
| cleanup | Application停止→所有する--rm DB停止。port解放・container消去、追加環境変数を削除 | 2026-10-22 13:14:35 JST PASS。所有app・seed window停止、専用DB自動削除、18080／55432待受なし。環境変数は起動子process限定。既存DB2件は維持 |

実行時にOS／browser version、commit、JAR hash、時刻、結果と再現操作を記録する。Cloud自動検証済みの項目でも、人による操作結果はこの表の実測で更新する。B1-08のOwner受入へ渡す資料とし、未実施のまま受入済みとしない。

2026-10-22のOwner提示browserは自宅PCのGoogle Chrome155.0.8059.85（64ビット）。提示画面では更新中50%であり、更新後再起動のversionは未確認。
