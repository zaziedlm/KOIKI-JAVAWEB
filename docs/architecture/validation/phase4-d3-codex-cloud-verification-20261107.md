# D3方式1：Codex CloudでのContainer再検証（2026-11-07）

## 1. 結果と対象

**Codex CloudでJAR再生成、Container image build、直接回帰25件、既存7 checkpointがPASSした。** Ownerの本chatでの指示により、`docs/d3-01-container-contract-20261105`へ切り替え、最新commit `5e01aaccc27970f98ed5dc8299161f8981584de7`の改善を再検証した。開始時のworking treeはclean。実装資材・依存版・検証assertionは変更していない。本書だけを新規追加した。

[引継ぎ](../../development/phase4-d3-cross-environment-verification-handoff-20261106.md)と[実行手順](../../../build-support/container-verification/integrated/README.md)に従い、対象をReference／Toolingの方式1（SPA同梱Java app＋PostgreSQL）に限定した。Windowsの既存work・secret・DB volumeは使用・操作していない。自動runnerは新規projectで1回実行し、cleanupまで成立した。

## 2. Source・環境・artifact

| 項目 | 今回の値 |
| --- | --- |
| 最新commitの変更 | Boot標準toolsによる2 stage／layer別COPY、元JAR hashのimage label照合、healthcheck・thread・OOM・ログ設定と説明 |
| host／container platform | Debian 13、Linux kernel 6.18.57、x86_64／Linux amd64、cgroup v2、Docker storage driver vfs |
| Docker／Compose | Engine／client 28.4.0、Compose 2.40.3、Buildx 0.31.0 |
| build JDK | Temurin 21.0.12.1+1。java／javac／Mavenを同じJDKへ統一 |
| build tooling | Maven Wrapper 3.9.16、Node 24.19.0、npm 11.9.0、Python 3.12.14 |
| browser | Java Playwright 1.62.0、Chromium／Headless Shell 151.0.7922.34（revision 1234）。Linux用を新規取得、共有library欠落なし |
| runtime images | Dockerfile／ComposeのTemurin21・PostgreSQL17-alpineの固定digestを変更せず取得 |
| 元JAR | `koiki-reference-app/target/koiki-reference-app-0.1.0-SNAPSHOT.jar`、SHA-256 `3ac921fdf1b84acec8d328cd6d7b07fda6562d0aa216cf4ae523ad355fe6c074` |
| image ID | `sha256:dd8d1757f46176a4a6eb9d5db27e15e2faff514a6316380c45de4ca277571e32` |
| project／port | `d3-cloud-20261107-01`／loopback 18081 |
| work | `.tmp/d3-cloud-20261107-01/`、補助Evidence `.tmp/d3-cloud-20261107-evidence/` |

元JAR hashと起動containerが使用するimageの`org.koikifw.d3.source-jar-sha256` labelが一致した。Windowsで生成したJARとのbyte一致は要件にしていない。主要資材hash・実行環境は補助Evidenceの`environment.json`に保存した。

## 3. Cloudの準備と初回停止の扱い

環境のcloud runtime guidanceを確認し、local Docker socketへの接続、network policyがenforcedであることを確認した。既存proxyとDocker認証設定を維持し、TLS検証を無効化していない。runtime Dockerfileの抽出工程はネットワーク依存の取得を行わないため、DockerfileへのCA追加は不要だった。

| 準備・初回停止 | 対応と再開範囲 |
| --- | --- |
| 既定javaはJREでjavacなし | workspaceの既存Temurin JDK21を`JAVA_HOME`／`PATH`で指定。Mavenの既存settings／toolchainsを明示 |
| Java／Nodeのproxy CA | 現在のsystem Java truststoreを専用workへ複製し、現在の`CODEX_PROXY_CERT`を追加。Mavenのtruststoreと`NODE_EXTRA_CA_CERTS`を指定 |
| classpath生成の初回失敗 | 専用local Maven repositoryに`koiki-dependencies-bom`が未登録。同sourceのBOMを`-f koiki-dependencies-bom/pom.xml install`でローカル登録後、classpath生成が成功。全suiteは実行していない |
| JAR生成の初回失敗 | npm既定cache `/home/agent/.npm/_cacache`へ書けず、frontendの`npm ci`で停止。テスト実行前。`npm_config_cache`を専用work内へ指定してJAR生成を再実行し成功 |
| image buildの初回失敗 | Buildxがread-onlyの`/home/agent/.docker/buildx`を作れず、build開始前に停止。`BUILDX_CONFIG`だけを専用work内へ指定して成功。`DOCKER_CONFIG`・registry credentialsは変更なし |
| bind mount読取り | host既定umaskは0077。`prepare.py`だけumask0022で実行し、今回workの親directoryを0700で保護。secret値は表示していない。`init-db.sh`は内容・Git modeを変えず0644へ整え、DBユーザーからの読取りを確保。appの非root／read-only条件は維持 |
| browser | `PLAYWRIGHT_BROWSERS_PATH`を専用workに指定し、既存E2E ToolingのclasspathでChromiumを取得。Windows binary／classpathは使用なし |

JAR生成は合計2 invocation（準備不足による停止1＋成功1）、image buildも合計2 invocation（builder開始前停止1＋成功1）。直接回帰25件と自動7 checkpointの動的検証集合は各1回実行した。初回ログを上書きせず保持している。

## 4. 実行commandと検証結果

Repository rootで`JAVA_HOME`等を設定後、以下を実行した。Mavenには`-s /workspace/koiki-cloud/.m2/settings.xml -t /workspace/koiki-cloud/.m2/toolchains.xml`を指定した。環境設定は補助Evidenceの`toolchain.sh`を参照する。

```sh
./mvnw -B -ntp -pl koiki-reference-app -am \
  -P b1-session-spa,d3-container clean package \
  '-Dtest=ReferenceContainerSecurityConfigurationTest,ReferenceSessionApiSecurityTest,ReferenceSecurityConfigurationTest' \
  -Dsurefire.failIfNoSpecifiedTests=false '-DargLine=-Xmx768m'

python3 build-support/container-verification/integrated/prepare.py \
  --jar koiki-reference-app/target/koiki-reference-app-0.1.0-SNAPSHOT.jar \
  --work-dir .tmp/d3-cloud-20261107-01 --project d3-cloud-20261107-01 \
  --port 18081 --require-reference-spa

docker compose --env-file .tmp/d3-cloud-20261107-01/compose.env \
  -f build-support/container-verification/integrated/compose.yaml config --quiet
docker compose --env-file .tmp/d3-cloud-20261107-01/compose.env \
  -f build-support/container-verification/integrated/compose.yaml build app

./mvnw -B -ntp -f build-support/reference-e2e-verification/pom.xml \
  dependency:build-classpath '-Dmdep.includeScope=test' \
  '-Dmdep.outputFile=target/d3-classpath.txt'
python3 build-support/container-verification/integrated/verify.py \
  --work-dir .tmp/d3-cloud-20261107-01 \
  --classpath build-support/reference-e2e-verification/target/d3-classpath.txt \
  --java "$JAVA_HOME/bin/java"
```

| 検証 | 結果 |
| --- | --- |
| JAR生成／直接回帰 | PASS。3 class／25件、failure0・error0・skip0。成功build 54.105秒 |
| prepare／Compose／image build | PASS。SPA asset hash・async JDBC非同梱の既存検査、Compose config、Boot tools抽出・layer別COPY成功 |
| DC01 | PASS。非root10001／read-only／Java PID1、memory1536MiB・PID160、元JAR label、thread設定40、SPA配置、healthcheck start interval2秒・ログ上限・JVM optionsを照合 |
| DC02 | PASS。app／DB起動、migration、readiness成立 |
| DC03 | PASS。実Chromiumでlogin→SPA申請作成→CSRF欠落403→提出→reload→logout／API拒否。`D3_BROWSER_PASS`を保存 |
| DC04 | PASS。readiness／liveness、未公開Actuator／API拒否、非superuser専用DB role・migration成立 |
| DC07 | PASS。SIGTERM／app再起動後、SUBMITTED:1200とAudit・migration件数保持 |
| DC06 | PASS。DB停止時readiness503／liveness200、DB再開後ready、再提出なし・SUBMIT Audit1件保持 |
| DC08 | PASS。今回projectのcontainer／volume／networkをcleanup、label検索でも残留0を確認 |

起動時thread31／cgroup PID32で上限160以内。Compose初回`up --wait`は27.720秒。Startedログは初回12.818秒／再起動12.771秒で、image pull・build時間とは区別する。最大負荷時の余裕やWindowsとの速度差の原因を証明する値ではない。

## 5. ログとcleanup・保持範囲

appの構造化JSONをlevelで集計し、INFO83／WARN6／ERROR0。WARNは起動・再起動の既存認証provider／cache統計警告が計4件、意図的DB停止時のHikari接続切断・検査警告が2件。message内の文字列`ERROR`をERROR levelとして数えていない。既知のSession属性重複キー例外は検出されず、未改修のため解消とは判定しない。

DBはERROR0／PANIC0。FATAL2件はDB停止時の`terminating connection due to administrator command`で、DC06の停止操作と時刻が対応する。回復後のreadiness・Audit確認は成功した。runnerによるruntime logへのsecret値非出力検査も成功した。

今回projectのcontainer／volume／networkはすべて削除済み。image、生成JAR、browser cache、専用work内のsecret・調査Evidenceは保持する。work親directoryは0700で保護している。他projectやDocker全体へのpruneは実行していない。手動確認環境は起動していない。

証拠はworkの`artifact.json`／`results.json`／`browser.txt`／`spa.png`／`runtime-app.jar`／`runtime-sanitized.log`、補助Evidenceの`build.log`／`build-prepared.log`／`classpath.log`／`classpath-prepared.log`／`bom-install.log`／`docker-build.log`／`docker-build-prepared.log`／`browser-install.log`／`verify-01.log`／`direct-tests.json`／`runtime-review.json`／`environment.json`。binary・secret・raw logはGitへ追加しない。

## 6. 限界

今回のCloudでの限定技術PASSを、Ownerによる結果全体受入、正式support、D3／Phase4全体の認定へ拡張しない。DC05のsecret欠落負例、通常root全件、async専用suite、layer比較測定、手動打鍵、A4再現可能build、B5 CDS、B6 restart policy、Session対応、HTTPS／Secure Cookie、処理中drain、別案件Consumer、ECS、方式2SSO、image公開／deployは実行していない。

検証後、Owner指示で自宅Windows検証記録のcommit `6313ef5`を同branchへfast-forwardで取り込んだ。続くOwner指示により、本Cloud検証記録だけをlocal commitの対象とする。push／PR／mergeは行っていない。
