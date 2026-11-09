# D3方式2 S1：専用DB・Java起動基盤

Tooling-ownedの限定検証。S1ではPostgreSQLと既存Reference backendだけを起動する。
Keycloak／Next.jsサービス・OIDC／BFF画面・認証処理はまだ実装しない。
[段階別計画](../../docs/development/phase4-d3-option2-staged-task-plan-20261109.md)に従い、S1結果後に停止する。

## 固定版と境界

`versions.json`がpackage／4 imageのtag＋index digest／amd64 digestとBrowserの識別情報を持つ。
`frontend/package-lock.json`は後段用の依存lockであり、BFFの実装・Next build成立を意味しない。
候補Next16.2.9のaudit結果を受け、採用を16.4.5へ変更した。S3開始時はsecurity情報を再確認する。
Node24.19.0／npm11.9.0、JDK21、Maven Wrapper3.9.16を使用する。
imageのmetadata取得とpackage auditを、image全OS packageの脆弱性scanとは区別する。

既存Reference main／POM・Framework／migration・Root Reactor／CI・方式1資材は変更しない。
backendの`d3-container`だけ有効にし、Bearerは無効。SPA／async JDBCはJAR検査で拒否する。
host公開portはS1では0。probeはbackend内のbash TCPで確認し、管理endpointを追加しない。

## build・新規work

Repository rootで実行する。現在のproxy／CAを維持し、Maven settings／toolchains・cacheの指定は環境に合わせる。
CloudではJava truststoreとNodeのCAをそのセッションから供給する。Dockerfileは依存取得をせずJARを抽出する。
Windowsでは`.ps1`、Linuxでは`bash build-reference.sh`を利用できる。

```sh
python3 build-support/container-sso-verification/build-reference.py \
  --output .tmp/d3-sso-s1-build-UNIQUE \
  --settings /absolute/path/settings.xml --toolchains /absolute/path/toolchains.xml

python3 build-support/container-sso-verification/prepare.py \
  --jar koiki-reference-app/target/koiki-reference-app-0.1.0-SNAPSHOT.jar \
  --work-dir .tmp/d3-sso-s1-YYYYMMDD-UNIQUE --project d3-sso-s1-YYYYMMDD-UNIQUE

docker compose --env-file .tmp/d3-sso-s1-YYYYMMDD-UNIQUE/compose.env \
  -f .tmp/d3-sso-s1-YYYYMMDD-UNIQUE/recipe/compose.yaml build backend
python3 build-support/container-sso-verification/verify.py \
  --work-dir .tmp/d3-sso-s1-YYYYMMDD-UNIQUE
```

上記project suffixは小文字英数字。workはrepoの`.tmp`直下、新規専用名に限る。
prepareがrecipe snapshot・全Tooling source hash・JAR／lock hash・所有名台帳・secretを記録する。
生成secret値はenv／CLI引数へ展開せず、secret fileをmountする。hostのwork／secret directoryは0700、
fileは異なるcontainer UIDが読める0444。local Composeのbind mount制約であり本番secret配布ではない。
既存work／projectを使う再開runnerではなく、preflightが既存資源を拒否してからcleanupをarmする。

直接回帰は既存`ReferenceContainerSecurityConfigurationTest`2件だけ。新規Tooling guardは`test_s1.py`5件。
S1の5群はtest件数ではない。S1-V02には他DB接続拒否6組と自DB内のrollback付きDDL／DML3組を含む。
build／取得60分、動的40分（実行30分・cleanup予約10分）。backend1536MiB／1CPU、DB1024MiB／1CPU、
PG最大32接続、backend pool最大4／最小1。buildとruntimeは並走しない。
失敗時は同workに結果・redacted logを保持してcleanupし、自動再試行しない。

## 3 DBとBFF DDL

| DB／同名role | S1の内容 |
| --- | --- |
| koiki_app | 既存KOIKI3・Reference4 migration、検証利用者4名・権限・master seed |
| koiki_bff | `bff_schema_history`／`bff_auth_transaction`／`bff_session` |
| koiki_keycloak | DB／roleのみ。製品tableなし。S2でKeycloak自身が初期化 |

全roleはDB owner／migration／runtime兼用、NOSUPERUSER／NOCREATEDB／NOCREATEROLE／NOREPLICATION／NOBYPASSRLS。
3 DBともPUBLICの権限を取り消し、自DBへのCONNECTを明示付与する。他2 DBへの接続を実credentialで拒否確認する。
PostgreSQL管理secretはDB初期化だけへ渡し、backendにはapp secretとHMACだけを渡す。
seedは空DB条件をtransaction内で確認し、再起動では適用しない。利用者UUIDはRFC形式のversion4／variant適合。

BFF DDLは`bff-migration.sql`、history作成・適用は`migrate-bff.py`。
transaction内でadvisory lockとlock待ち5秒／statement30秒を設定し、checksum不一致をDDL前に拒否する。
同じchecksumの再適用ではhistoryの適用時刻も保持する。DDL排他は実装済みで、S1は並行migration競合試験を含めない。
auth transactionはclient／state hashとbrowser binding hash、暗号化nonce／PKCE用payload、期限・消費時刻。
Sessionはclient／Session ID hash、issuer／subject、CSRF hash、idle／absolute期限、version、暗号化tokenとkey ID／IV／tag。
平文token／nonce／PKCEの専用列はなく、AES-256-GCMとIV12byte／tag16byteの制約を持つ。
client複合PK・期限indexを設け、IdP sid・業務データ複製・DB間FKは追加しない。
実際の暗号化・一回消費・Session期限・refreshの動作はS3以降の実装／検証であり、DDLだけで成立扱いしない。

## cleanupとexport

verifyは成功・失敗を問わず今回projectのログをsecret値でredactし、台帳と一致するcontainer／volume／networkだけを削除する。
残留0を確認した後に生成secretを削除し、imageとredacted Evidenceを保持する。
cleanupだけ必要な場合は同workの`execution.json`と所有台帳を照合する`cleanup.py --work-dir ...`を使用する。
全体prune・他project／旧方式1のvolume削除は行わない。

```sh
python3 build-support/container-sso-verification/export-evidence.py \
  --work-dir .tmp/d3-sso-s1-YYYYMMDD-UNIQUE --output-dir .tmp/d3-sso-exports
```

exportはmanifest／execution／results／cleanup／runtime-redacted.logだけをallowlistで抽出し、
JWT／Authorization／Set-Cookie／秘密鍵の残留を拒否する。生成secretの削除前にはexportしない。
JAR・env・realm・DB dump・HAR・trace・storageState・node_modulesをarchiveへ含めない。
archive内の各file checksumとarchive自体のSHA-256を出力する。
実行時にredacted logの内容を点検し、export済みとWindows取得・checksum照合済みを区別する。
Cloudのdownload経路が未確認ならOPENのまま記録し、workspace内に存在するだけで持帰り済みにしない。

## 実案件への示唆

DB owner兼用は検証用の簡略化。案件ではmigration主体とruntime roleの分離を決める。
BFFの鍵管理・暗号化tokenの保持期間・DDL運用はCustomer-ownedとし、本fixtureを正式配布物へ自動昇格させない。
このCloudの実効CPUは4。S1の2CPUは上限内だが、後段の最大4.5CPU構成は開始判断で見直す。
