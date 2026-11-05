# 方式1：SPA同梱Java JAR＋PostgreSQLのContainer検証

Java 21 runtimeで実行するappとPostgreSQLの2サービス。Next.js／Keycloakは起動しない。
実装所有はReference／Tooling、正式Framework配布unit・汎用Project Templateではない。

## 資材と再利用境界

| 資材 | 用途 |
| --- | --- |
| `Dockerfile`／`.dockerignore` | 明示した`app.jar`だけを格納するruntime recipe。案件側のJARにも適用できる構造 |
| `probe.sh` | 同じportのreadinessをHTTPで検査。base imageにbashが必要 |
| `compose.yaml`／`init-db.sh` | 専用DB・非superuser app role、secret file、非root／read-only app、loopback公開、資源制限 |
| `prepare.py` | 明示JARを新しいcontextへコピーし、hash記録・secret生成。既存workを上書きしない |
| `build-reference.ps1` | このリポのReferenceをbuildするWindows補助。案件自身のbuild commandへ置換する部分 |
| `seed.sql`／`ContainerSpaJourney.java`／`verify.py` | Reference専用fixture・有限browser／DB／再起動検証。案件業務へそのまま転用する資材ではない |
| `manual-seed.sql`／`start-manual.py` | 新しい専用projectをbuild・起動し、申請者／承認者を登録して手動browser確認用に稼働を保持 |

runtime DockerfileはRoot Reactorや相対親POMを読まない。案件では固定Framework版を依存として使い、案件repo自身で実行可能JARを生成する。そのJARを`prepare.py --jar`へ渡す。
Composeのprofile・DB名／role・HMAC等はReference向け設定例なので、案件の設定・health契約に置換する。DockerfileだけではSPA同梱やhealth／認証設定は追加されない。

## 前提

- Linux／amd64 Docker・Compose、JDK 21、Python 3。Reference frontendの正確なNode／npm版は`koiki-reference-app/frontend/package.json`を参照する。
- Windows補助は`node.exe`と`npm.cmd`がある専用toolchain directoryを指定。端末全体のNode切替は不要。
- runtime image／DB imageはdigest固定。更新時は新digestでbuild・検証して記録し、追跡可能にする。
- app 1.5 GiB／CPU1／heap 768 MiB、PG 1 GiB／CPU1／最大16接続。browser／build JVMは別計上しbuildとruntimeを並走させない。
- 初回はloopback HTTPの検証専用。Secure Cookie／TLS／信頼proxy・本番運用の成立は別検証。root／async専用suite全件はこのrunnerで実行しない。

## buildと準備

Repository rootから実行する。専用workはGit ignoredの`.tmp`等を使い、JAR／secret／raw logをGitへ入れない。

```powershell
pwsh -NoProfile -File build-support/container-verification/integrated/build-reference.ps1 `
  -NodeDirectory '<指定版Nodeとnpmがあるdirectory>' -RunDirectTests

python build-support/container-verification/integrated/prepare.py `
  --jar koiki-reference-app/target/koiki-reference-app-0.1.0-SNAPSHOT.jar `
  --work-dir .tmp/d3-my-run --project d3-my-run --port 18081 --require-reference-spa

docker compose --env-file .tmp/d3-my-run/compose.env `
  -f build-support/container-verification/integrated/compose.yaml config --quiet
docker compose --env-file .tmp/d3-my-run/compose.env `
  -f build-support/container-verification/integrated/compose.yaml build app
```

`prepare.py`はReference指定時に埋込SPA build recordとasset hash、async JDBC非同梱を検査する。JAR名だけを版識別にしない。
compose.envにはパス／image名等だけを記録し、passwordやHMACはsecret fileへ格納する。Compose secretはlocal file mountで、外部secret managerによる暗号化・配布とは異なる。

## 起動・検証・cleanup

Java Playwrightの依存は既存Toolingからclasspathだけを取得する。既存E2E全件を実行する操作ではない。

```powershell
./mvnw.cmd -B -ntp -f build-support/reference-e2e-verification/pom.xml `
  dependency:build-classpath '-Dmdep.includeScope=test' '-Dmdep.outputFile=target/d3-classpath.txt'
$taskJava = (Get-Command java).Source
# Chromiumが未導入の場合のみ、上のclasspathと com.microsoft.playwright.CLI install chromium で用意する。
python build-support/container-verification/integrated/verify.py `
  --work-dir .tmp/d3-my-run `
  --classpath build-support/reference-e2e-verification/target/d3-classpath.txt --java $taskJava
```

runnerは今回の専用projectを起動し、probe・拒否・非root／read-only／JAR一致・専用DB role／migration、fixture seed、実browserのlogin→SPA作成→CSRF欠落拒否→提出→reload→logout、DB／Audit照合、app再起動、DB停止／回復を確認する。最後に成功／失敗を問わず当該projectのcontainer／volume／networkをcleanupし、残留を照合する。

`artifact.json`、`results.json`、`browser.txt`、`spa.png`、`runtime-sanitized.log`をworkに保存する。初回失敗は同workへ保持し、訂正後は新しいwork／projectを使う。runnerは同じseedを既存DBへ重複投入する再開runnerではない。
既存の結果／失敗記録があるwork、または同じCompose project labelのcontainer／volume／networkが既にある場合は、起動・cleanupへ入る前に拒否する。

secret fileはignored workに保持される。調査・再現の必要がなくなった時点で、確認済みの当該work内のsecret fileだけを削除する。imageはlocalに保持し、push／registry公開・既存image削除を行わない。

手動起動は同じComposeへ`up -d --wait --wait-timeout 180`、停止は`down --volumes --timeout 40`。volume削除は今回専用DBの最終cleanupにだけ使い、状態保持を調べる再起動には使わない。

## 現在の限界

正常shutdown／同DB再起動の確認と、処理中HTTP要求のgraceful drain実証は分ける。secret mount全体の欠落による起動拒否は確認済み。個別secretの欠落／不正値、HTTPS／Secure Cookie、別案件Consumerの独立build／起動、ECSは別途記録する。方式2のBFF／SSO結果へ拡張しない。

## ローカルブラウザで手動確認する場合

自動検証runnerは最後にDBまでcleanupする。手動確認には新しいwork／projectを準備し、次の専用runnerで起動を保持する。

```powershell
python build-support/container-verification/integrated/prepare.py `
  --jar koiki-reference-app/target/koiki-reference-app-0.1.0-SNAPSHOT.jar `
  --work-dir .tmp/d3-manual-20261105 --project d3-manual-20261105 --port 18081 --require-reference-spa
python build-support/container-verification/integrated/start-manual.py --work-dir .tmp/d3-manual-20261105
```

同work／projectが既にある場合は、この初期化commandを繰り返さない。別の新しい名前を指定するか、後述の既存環境の再開commandを使う。

- login：`http://127.0.0.1:18081/login`、SPA：`http://127.0.0.1:18081/spa/`
- 申請者：`d3-applicant@example.test`。SPAの新しい申請→下書き保存→提出を確認できる。
- 承認者：`d3-approver@example.test`。MVCホーム／`/expenses/approvals`で同部門の申請を承認できる。master管理権限もある。Identity管理者は登録しない。
- 承認者はlogin後のホーム本文にある「承認対象の経費申請」から進む。共通headerの「経費申請」は申請者用`/expenses`を指すため、承認専用ユーザーの入口には使わない。
- 両者共通の生成passwordは`.tmp/d3-manual-20261105/secrets/login-password`。値はGit・共有文書へ転記しない。
- ユーザー切替はlogoutしてから行うか、別browser profile／private windowを使う。ブラウザのoriginは`127.0.0.1`へ統一する。

初期版seedの部門／科目UUIDはMVCのversion／variant条件に不適合だったため、現行seedは適合するIDへ訂正済み。旧手動DBは退避・不変照合付きの`repair-manual-master-ids.sql`で一回訂正した。このSQLは旧D3 fixture専用で、新しいDBへの自動初期化や一般案件のmigrationとして使わない。訂正前から開いているフォームは再読込する。

runnerはHTTPで両利用者のloginと選択routeへの到達を確認するが、手動browser操作の完了やOwner受入として数えない。成功時はcontainer・DB・secretを保持し、`manual-ready.json`へ起動状態・URL・passwordのファイル参照を記録する。失敗時はその専用projectをcleanupする。

確認後の停止はDBを保持する次の操作。作業はRepository rootから行う。

```powershell
docker compose --env-file .tmp/d3-manual-20261105/compose.env `
  -f build-support/container-verification/integrated/compose.yaml stop
# 同じデータで再開する場合（seedは再投入しない）
docker compose --env-file .tmp/d3-manual-20261105/compose.env `
  -f build-support/container-verification/integrated/compose.yaml up -d --wait --wait-timeout 180
```

この手動確認データが不要と判断できた後だけ、同じ指定のComposeで`down --volumes --timeout 40`を実行する。手動起動中は、以前の自動検証projectのcleanup結果と混同しない。
