# Reference local demo data

`seed-reference-demo.ps1`は、package済み`koiki-reference-app`を開発チームが短時間で確認するための
非配布Toolingである。production seed、Reference migration、Project Templateまたはbrowser harnessではない。

## Boundary

- Local Run Guideに従って起動した`--rm` PostgreSQL 17 containerだけを対象にする。
- `-ConfirmDisposable`を必須とし、migration未適用またはIdentity / Reference dataが存在するDBを拒否する。
- login passwordは実行ごとにランダム生成し、Repositoryへ固定値を保存しない。
- Framework / Reference production code、Public API、migration、Maven moduleまたはApplication JARへ含めない。
- 登録はToolingによる直接SQL fixtureであり、正式なIdentity provisioning contractの実証には使用しない。
- container停止による全data破棄を前提とし、共有・継続利用DBには実行しない。

## Usage

Applicationを起動し、Framework / Reference migrationだけでなく`Started ReferenceApplication`まで確認する。
Applicationを起動したPowerShellは開いたまま、別のPowerShellをRepository rootで開いて次を一度だけ実行する。
seed用PowerShellにApplicationの環境変数は不要である。

```powershell
.\build-support\reference-local-demo\seed-reference-demo.ps1 -ConfirmDisposable
```

成功時はlogin emailと、その実行だけで有効なランダムpasswordをterminalへ表示する。demo userには
`EXPENSE:APPLY`、`EXPENSE:APPROVE`、`EXPENSE:SETTLE`、`IDENTITY:ADMIN`、`MASTER:ADMIN`を付与し、
部門、経費科目、所属、承認scopeおよびDRAFT / SUBMITTED / APPROVEDの申請を1件ずつ登録する。
Identity user lookupにはUser ID `b2000000-0000-4000-8000-000000000001`を使用する。一度表示されたpasswordを失った
場合はdataを上書きせず、Applicationと`--rm` containerを停止してLocal Run Guideの§4から作り直す。
seed内の固定UUIDは、ReferenceのWeb Formと同じversion 4 / RFC variant形式を使用する。

既定以外のcontainer名、database名またはdatabase userを使う場合だけ明示指定する。

```powershell
.\build-support\reference-local-demo\seed-reference-demo.ps1 `
  -ContainerName koiki-reference-postgres `
  -Database koiki_reference `
  -DatabaseUser koiki `
  -ConfirmDisposable
```
