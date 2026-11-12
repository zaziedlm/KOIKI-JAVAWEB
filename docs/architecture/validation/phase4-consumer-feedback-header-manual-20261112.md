# 利用側是正：ヘッダー目視確認用のWindows手動環境（2026-11-12）

状態：**Owner目視確認で問題なし（2026-11-12、§6）／手動環境の稼働保持を継続**。以下の準備時点の未実施表示は履歴として読む。

## 1. 起動範囲と固定artifact

Ownerの「ヘッダーで見た目の部分なので、手動ブラウザ実行の検証を実施したい。実行環境を整えてほしい」に基づき、今回の修正を含む独立した手動環境を準備した。

- source：branch `claude/eager-hawking-2pyla8`、commit `ac23657d1283955857915e1cd8fd651ff5e1b6c9`。
- [Windows自動検証](phase4-consumer-feedback-windows-verification-20261112.md#5-owner承認後のclean再実行後続検証)でpackaged journeyが成功した通常JARを、そのまま専用作業領域の`reference.jar`へコピー。再buildしていない。
- SHA-256：`c5b1cc3c34187660c50cc61fb9750ec0e24bb18b657ed326d0325d41f8157df9`。コピー後も一致。
- [Referenceローカル手動起動ガイド](../../reference/KOIKI-JavaWeb-FW_Reference_Application_Local_Run_Guide_v0.1.md)の通常Session MVCを使用。SPA profile・Bearer API・Next.js・Keycloakは今回起動していない。
- Java 21をhost側の非表示background processとして起動（PID `26008`、heap768 MiB、ActiveProcessorCount2、Hikari最大4／minimum idle1）。
- PostgreSQL 17 Alpineは今回専用container。既存方式1／方式2の保持資源の起動・停止・cleanup・再seedは行っていない。

## 2. 現在の環境

| 項目 | 値 |
| --- | --- |
| Login URL | `http://127.0.0.1:18082/login` |
| Home | `http://127.0.0.1:18082/` |
| Login利用者 | `p3-demo-user@example.test` |
| 起動作業領域 | `.tmp/header-manual-20261112/` |
| 状態／準備完了記録 | 同`manual-state.json`／`manual-ready.json` |
| Passwordのローカル参照 | 同`secrets/login-password` |
| Java実行JAR | 同`reference.jar` |
| Java log | 同`application.stdout.log`／`application.stderr.log` |
| DB container | `koiki-reference-header-20261112` |
| DB named volume | `koiki-reference-header-20261112-data` |
| DB接続先 | `127.0.0.1:55442/koiki_reference`、user `koiki` |
| 初回準備日時 | 2026-11-12 19:19 JST |

ApplicationとDB公開portはいずれもloopbackに限定。HTTPローカル確認としてSession CookieのSecureだけfalse、HttpOnly／SameSite=Laxは維持している。CSRF・認可・migration・schema validationは無効化していない。

DB passwordとsource HMAC keyは今回用の乱数。demo passwordも既存seed scriptが実行時に生成した。command lineや共有文書へ値を載せていない。今回の稼働保持・手動引継ぎでは、既存D3手動環境の運用に合わせ、ローカル参照用passwordをGit ignoredの`secrets/`へ保持した。このdirectoryはWindows ACLを現在利用者とSYSTEMに限定している。通常ローカルガイドのterminal表示だけの運用とは異なるため、秘密fileを共有・commitしない。

このJARコピーを使うため、後のMaven `clean`でRepositoryの`target`が消えても、今回の実行JARは変更されない。環境の停止・cleanup・再seedはOwnerの指示なしに行わない。

## 3. seedと準備確認

`build-support/reference-local-demo/seed-reference-demo.ps1`を新規DBへ`-ConfirmDisposable`で**1回**実行した。secretを含むseed出力はログへ保存していない。

- Identity user2件（ログイン可能なのは上記reviewer1件）、Permission5件。
- reviewerに`EXPENSE:APPLY`／`EXPENSE:APPROVE`／`EXPENSE:SETTLE`／`IDENTITY:ADMIN`／`MASTER:ADMIN`。
- 部門・経費科目各1件、reviewerの所属・承認scope。
- 経費申請3件：DRAFT 1,200円、別applicant所有のSUBMITTED 2,400円、APPROVED 3,600円。
- Framework migration成功3件、Reference migration成功4件。

HTTPの準備点検ではCSRF付きlogin→次の5画面→CSRF付きlogoutを実行し、各画面でHTTP200・Reference表題1個・4メニューlinkを確認した。

| 画面 | path |
| --- | --- |
| Home | `/` |
| 経費申請 | `/expenses` |
| 部門 | `/master/departments` |
| 経費科目 | `/master/expense-categories` |
| Identity | `/identity/users` |

これはHTTP HTMLの準備点検であり、実browserの見た目の確認ではない。`http-readiness.json`に安全な結果だけ保存している。起動後のDB読取ではuser2／Permission5／申請3／Session2、migration3／4を観測した。HTTP点検によるSession／LOGIN Auditがあるため、Owner操作前の完全未アクセス状態ではない。logout成功をSession総数0の証拠としては扱わない。

DBは`running／healthy`、restart0／OOMなし。Java PIDも生存確認済み。起動用一時scriptは、初回preflightでhelperとnative commandの名前衝突を訂正した。最初の試行は`manual-state.json`とDocker資源の作成前に中止し、訂正後の起動とseedが上記の1回である。Framework／Reference sourceの変更はしていない。

## 4. Ownerの手動確認手順

Repository rootのローカルPowerShellでpasswordを確認する。値はチャット・共有logへ貼り付けない。

```powershell
Get-Content .tmp/header-manual-20261112/secrets/login-password
```

1. `http://127.0.0.1:18082/login`をChromeまたはEdgeで開き、上記emailとpasswordでloginする。
2. Homeの「KOIKI Reference」、経費申請・部門・経費科目・Identityのメニューが重複なく表示され、文字・配置・余白が崩れていないことを確認する。
3. 各メニューを順に開き、ヘッダー表示が共通であること、link先が正しく遷移すること、CSS・日本語表示に問題がないことを確認する。
4. 経費では詳細・作成画面も開き、ヘッダーの表示を確認する。今回は見た目が目的なので、保存・提出・承認は必須ではない。
5. Identityの検索にはreviewer ID `b2000000-0000-4000-8000-000000000001`を使用できる。
6. 任意のウィンドウ幅でもメニュー・表題の重なりや欠けがないか確認し、最後にHomeへ戻ってlogoutする。

header markup自体を変更する作業ではなく、Reference側へ移設した結果の表示・遷移を確認する。目視結果の報告後、必要に応じてこの専用環境のlog／DB／Auditをread-onlyで別途点検する。Owner手動確認・結果受入・O-1是正・全画面／全権限の認定は準備完了と区別する。

## 5. 保持と後続

手動確認用としてJava／DB／named volume／secretを保持する。`start.ps1`は既存状態を検出すると再起動・再seedを拒否する。停止／再開は保存済みstateと対応するsecret／volumeを照合して別途行う。作業領域はGitで転送されない。

今回の記録とAGENTSの文書差分だけを追加し、commit／push／PR／mergeは行っていない。

## 6. Ownerの目視確認報告（2026-11-12）

Ownerから「確認しました。問題ありません。」との報告を受けた。今回用意したヘッダー手動確認環境でのOwner目視確認結果として、問題なしを記録する。

実際に操作した画面数、browser種類、ウィンドウ幅、保存・提出・承認の実施有無は個別に報告されていないため、全画面・全権限や全操作の確認済みへ拡張しない。手動操作後のlog／DB／Auditの追加点検は今回行っていない。前段の自動検証結果は[Windows記録§5](phase4-consumer-feedback-windows-verification-20261112.md#5-owner承認後のclean再実行後続検証)と分けて保持する。

Java／DB／volume／secretの稼働保持方針を継続し、停止・cleanup・再seedは行っていない。今回のヘッダー目視結果を、O-1是正・後続版公開・D3／Phase4全体の受入へは拡張しない。commit／push／PR／mergeは未実施。
