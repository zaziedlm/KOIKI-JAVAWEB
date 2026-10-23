# B1-07：Cloud環境での直近commit再検証（2026-10-22）

実行日：2026-10-22（JST）。以下のlog時刻はUTC。

## 対象

Ownerの「指定開発branchへpull切替し、直近commitから実行可能な検証を実行する」指示に基づく。
`origin`から対象branchをfetchし、cleanな作業ツリーを
`docs/phase4-review-integrated-plan-20261020`へ切り替えた。
local HEADとremote-tracking refはともに`58b3269c427804246df6f1f0a6717459900aa8cd`。
当該branchを追跡する設定も追加した。production／test source・依存version・lockfileは変更していない。

直近commitは`test(reference): complete B1-07 SPA integration and correct MVC edits`。
対象はReferenceのMVC日付ISO binding・同一明細IDの編集保存と、ToolingのSession SPA統合journey／artifact照合。
前回結果は[2026-10-21記録](phase4-b1-07-integrated-browser-results-20261021.md)を参照し、今回の測定と区別する。

## 環境と実行

保存済み`/workspace/koiki-cloud/env.sh`を使用。JDK21.0.12.1、Wrapper Maven3.9.16、Node24.19.0／npm11.9.0、Docker28.4.0。
既存PostgreSQL17-alpine／Ryuk0.14.0を使用し、daemonの再起動・設定変更はしていない。
cgroup上限はmemory16GiB／CPU4。Maven／test heap768MiB、fork1／reuseForks=false、JUnit／reactor並列なし。
通常DB制限selectorでDB memory1GiB／CPU1／max_connections16を適用する。

```bash
source /workspace/koiki-cloud/env.sh
export MAVEN_OPTS="$MAVEN_OPTS -Xmx768m"
./mvnw -o -B -ntp -DargLine=-Xmx768m -DforkCount=1 -DreuseForks=false \
  -Djunit.jupiter.execution.parallel.enabled=false \
  -Dkoiki.reference.normal-root.db-limits.enabled=true clean verify
# SPA構成は -pl koiki-reference-app -am -Pb1-session-spa を追加し、
# 未キャッシュ依存を取得するため -o を外して実行。
```

書込不可の通常homeに代わり、上記の保存済みworkspace内Maven設定を利用した。
Toolingの依存解決には現sourceのBOMをMavenのlocal repositoryへinstallした。
初回offline installはinstall-plugin未キャッシュで停止し、online installは成功。
SPAの初回offline buildはexec-maven-plugin3.6.3未キャッシュでfrontend生成前に停止したため、
許可済みMaven／npm配信元から取得するonline実行へ切り替えた。
proxy・CA・npm integrityと固定versionを維持し、取得のためのPOM変更は行っていない。
これらの準備停止はtest assertionの失敗と区別し、途中の実行件数を最終件数へ加算しない。

## 結果

| 集合 | 実行 | skip | failure／error | 結果 |
| --- | ---: | ---: | ---: | --- |
| 通常root clean verify | 422 | 99 | 0／0 | SUCCESS、5分7秒 |
| SPA同梱 clean verify | 422 | 99 | 0／0 | SUCCESS、5分12秒 |
| 最終SPA JARの既存artifact照合 | — | — | — | PASS。JUnit件数へ加算しない |
| E2E Tooling test-compile | — | — | — | SUCCESS、10.226秒。実行PASS件数へ加算しない |
| 新旧実JAR browser journey | 0 | — | — | Chromium取得拒否により未実行 |

両構成のXML内訳はそれぞれArchitecture Contract4＋Rules107＋Reference311実行、専用99 skip。
直近追加の`rendersAndBindsIsoExpenseEditDateRegardlessOfLocale`はen-US／ja-JPの2 invocationがPASS。
`editsExistingLineWithoutChangingItsIdentityAndAdvancesVersion`も実PostgreSQLでPASS。
通常JARにSPA asset／private build記録が存在しないことを確認した。
両build後の稼働containerはそれぞれ0件。

通常JAR SHA-256：`f262c04dcd094f41ca24f0d22028f50c4dd620caf75aa3627d736c1f9978bc6e`。

SPA JAR SHA-256：`9c2ba475818fd85c12c4754d0d8a04c1d5ba560b1b379bcdf4a35881f8a69273`。

SPA frontendの`tsc --noEmit`／Vite build／build記録検査も成功した。
最終JARへ既存`SessionSpaArtifact.verify`を直接呼び出し、JARの固定SHA-256、profile、
frontend全入力15 file（lock含む）、同梱asset3 fileのhashと過不足、source map等の非混入を照合してPASS。
呼出専用Java entrypointは端末内rawだけに置き、製品／test sourceを変更していない。
通常構成の最終buildは02:04:35 UTC、SPA構成は02:11:43 UTCに終了した。

## browser検証の制約

Tooling依存解決と全test sourceのcompileは成功したが、指定Playwright1.62.0のChromiumを導入する
`com.microsoft.playwright.CLI install chromium`は以下で停止した。

```text
Chrome for Testing 151.0.7922.34 (playwright chromium v1234)
https://cdn.playwright.dev/builds/cft/151.0.7922.34/linux64/chrome-linux64.zip
HTTP 403: Domain forbidden
```

環境の通信ポリシーで配信元が許可されていない。今回の新旧journeyは未実行とし、前回の2件PASSを今回の結果へ流用しない。
実行には同配信元の許可、または指定バージョンの承認済みbrowser cacheの準備が必要。
Windows build・人の手動操作・B1-08 Owner受入も今回の検証対象に含めない。

## 証拠

端末内raw：`/workspace/koiki-cloud/logs/revalidation-20261022/`。
`normal/`と`spa/`にbuild log、fresh Surefire XML、summary JSON、JARを保存する。
`spa-preparation/offline-build.log`、`bom-install*.log`、`browser-setup*.log`、
`tooling-compile.log`、`chromium-install.log`で準備結果と拒否理由を保持する。
`spa/artifact-verification.log`に既存artifact照合結果を保存した。
raw／JAR／cacheはGit成果物へ含めない。検証終了後のOwner指示により、本記録1 fileをcommit／push対象とする。PR作成は含めない。
