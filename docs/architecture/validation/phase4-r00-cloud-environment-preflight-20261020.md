# P4-B1／R-00：Codex Cloud実行環境の再点検

実行日：2026-10-20（JST）

状態：**R-00のclean compile・選択した直接回帰を実行できることを確認済み。ソース変更なし。** Framework／Referenceの是正実装・R-00の最終受入・全testの再受入ではない。

## 1. 対象と結論

Ownerは、以前整備・検証済みのCodex Cloud環境を点検し、R-00のclean compileと直接回帰の実行準備を進めるよう指示した。ソースコードの変更はまだ行わない。

- branch：`docs/phase4-review-integrated-plan-20261020`。
- source HEAD：`047b45780ae1352970822f419c9cff0b6ad77dfa`。開始時から[B1設計記録](../../development/phase4-b1-session-spa-mvc-design-20261020.md)と導線3件の文書差分を保持。
- Ownership：Tooling／実行環境の点検と文書訂正。既存production・test・POM・依存・migration・workflowを変更していない。
- **保存済み構成を有効化すればJDK・Maven・PowerShell・依存cache・Chromiumが使用できる。現在のHEADでoffline clean test-compileと既存7 testが成功した。環境再構築は不要。**

## 2. 前回の環境記述の訂正と過去結果

前回B1調査は有効化前のPATHと通常`~/.m2`／browser cacheだけを見ており、`/workspace/cloud-setup/activate.sh`と代替cacheを見落としていた。「未準備」を環境の不足と解釈した点を訂正する。今回新しくJDK等をinstallしたのではない。

保存済みの`/workspace/cloud-setup/report.md`、`results.json`、`start.md`を確認した。2026-10-03のsource `217cd0cae8a088cf890fee2093ad86b31fb67f36`では次が成立している。

- root clean verify：16 projects、170 tests、failure／error／skip 0。
- PostgreSQL integration：7 suites／20 tests（上記170件に含む）。
- Feature Template／Null Safety：期待失敗とrestoreを含むscript exit 0。
- Reference clean package・起動・cleanup、実ChromiumのCritical Journey：1 test、failure／error／skip 0。

この過去結果を否定・再実行対象に戻さない。一方、現在のHEADの全test成功とは区別する。過去の170件やWindowsの305件等を今回の実行件数へ転記しない。

## 3. 使用した環境

各shellで最初に次を実行する。起動時PATHだけから導入有無を判定しない。

```bash
cd /workspace/KOIKI-JAVAWEB
source /workspace/cloud-setup/activate.sh
```

| 対象 | 今回確認した結果 |
| --- | --- |
| JDK | `/workspace/cloud-tools/jdk-21/usr/lib/jvm/java-21-openjdk-amd64`。java／javac `21.0.12.1`。一時directoryでrelease 21のcompile→実行も成功 |
| Maven | Wrapper Maven `3.9.16`。13 projectのEnforcer／Toolchains validate成功。JDK 21選択をlogで照合 |
| 設定・cache | `MAVEN_USER_HOME=/workspace/.cache/maven`、repositoryはその`repository/`。保存済みsettings／toolchains／proxyをactivationで適用。TLS・checksum検証を緩めていない |
| BOM | cacheの`koiki-dependencies-bom:0.1.0-SNAPSHOT` POMは作業sourceとbyte単位で一致。Boot `4.1.1`／Modulith `2.1.1`を照合 |
| PowerShell | 保存済み`7.5.4`が起動しversion応答。既存scriptは今回再実行していない |
| Docker | platform管理のlocal socketでEngine `28.4.0`、vfs、3 CPU、memory約9.7 GiB、稼働container 0。daemon再起動・設定変更なし |
| 保存image | `postgres:17-alpine`、`testcontainers/ryuk:0.14.0`。pullし直していない |
| container実行 | 保存PostgreSQL imageでversion表示だけを実行し`17.11`。networkなし・memory512 MiB／CPU1・`--rm`。DB初期化・公開portなし |
| Playwright | Java `1.62.0`の既存classpath 46項目すべて存在。browser cacheは`/workspace/.cache/ms-playwright`。`PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD=1`を維持 |
| Chromium | revision1234／`151.0.7922.34`。native library不足0。実headless起動→メモリー内HTMLのtitle照合→close成功。外部通信・application起動なし |
| disk／memory | 点検時workspace空き約30 GiB、available memory約9.1 GiB。B1実装全体の資源上限を決めたものではない |

通常sandboxではDocker socket接続が`Operation not permitted`、Chromium起動が`shutdown: Operation not permitted`で失敗した。同じ診断／smokeを環境の権限付き実行へ移して成功。Engine・browserの欠損とは判断せず、承認された実行手順を使った。proxy経路・daemon・browser flagsを変更して回避していない。

## 4. 現sourceのclean compileと直接回帰

### 実行command

保存済みactivation後、以下を直列に実行した。Mavenはすべてoffline。新規取得・install・snapshot publishは行っていない。

```bash
./mvnw --batch-mode --no-transfer-progress -o -pl koiki-reference-app -am validate
./mvnw --batch-mode --no-transfer-progress -o -pl koiki-reference-app -am clean test-compile
./mvnw --batch-mode --no-transfer-progress -o -pl koiki-reference-app -am -Dtest=RequiredNegativeArchitectureRulesTest,ReferenceArchitectureTest -Dsurefire.failIfNoSpecifiedTests=false test
```

| 集合 | 結果・適用範囲 |
| --- | --- |
| offline validate | 13 projects SUCCESS、exit 0 |
| clean test-compile | 同13 projects SUCCESS、exit 0、約76秒。Referenceはmain150 source／test56 sourceをcompile。既存Error Prone／NullAway設定を維持。既存ReferenceEquality warning 2件は保持し修正していない |
| `RequiredNegativeArchitectureRulesTest` | 5 tests PASS、failure／error／skip 0。意図的なproduction規約違反が検出されることをassertするtest |
| `ReferenceArchitectureTest` | 2 tests PASS、failure／error／skip 0。現在のimport指定を変えずに実行 |
| 選択test reactor | 13 projects SUCCESS、exit 0、約25秒。実行7件。`failIfNoSpecifiedTests=false`だけで成功認定せず、指定2 classの新XMLと実行数を照合 |

新XMLは`koiki-archunit-rules/target/surefire-reports/TEST-org.koikifw.archunit.RequiredNegativeArchitectureRulesTest.xml`と`koiki-reference-app/target/surefire-reports/TEST-org.koikifw.reference.ReferenceArchitectureTest.xml`。各hash・件数はraw記録の`selected-test-results.json`へ保存した。

### F5／R-00に関するfresh観測

今回生成したReferenceのSurefire classpathを使い、Repository外の一時Java probeから既存の公開Rulesを評価した。source・test・assertionは変更していない。

| import集合 | class数 | business rule違反 |
| --- | ---: | ---: |
| 現行と同じ`importPackages("org.koikifw.reference")` | 243（うちtest-classes 65） | 0 |
| `DO_NOT_INCLUDE_TESTS`を付けた観測用import | 178 | 0 |

既存[レビューF5](main-framework-design-implementation-review-20261020.md)の241／67／174 classes・15違反は、既存compiled classを含む別epochの限定観測である。**今回のfresh compileでは15違反は再現せず、現行Architecture testもPASS。** 差の完全な原因特定や、過去レビューの実行条件再現は今回行っていない。

現行importにtest除外指定がなく、testが65 class含まれる事実は残る。「現在testが失敗するから修正必須」とは扱わず、R-00では検査対象をproductionへ明示する必要性・fixture検査との役割をこの結果に基づいて整理する。R-00のcode変更・結果受入は今回行っていない。

## 5. ソース保全・cleanup・次の実行

- 文書以外のtracked file 921件について実行前後のSHA-256を比較し、変更0。production・既存test・POM・workflowにGit差分なし。文書は既存の未commit差分を保持して更新した。
- 一時compile／probeのdirectoryは終了時に削除。一時containerは自動削除され、最終`docker ps -a`は空。liveなbrowser／Java子processは残していない。
- Chromium終了後にPID 1配下の終了済み`chrome-headless` zombieが2件残った。実行中processではないが、process表の残留0とは記録しない。親processのrestartや無関係なprocess停止は行っていない。
- clean test-compileは`target`を作り直すため、以前のReference実行JARは削除済み。今回はpackageしていない。次に実JARを使う時は同sourceから新しくpackageし、hashを固定する。
- root全件、実DB integration、Critical Journey、専用async、Feature Template／Null Safety全体は今回再実行していない。上記7件のPASSをそれらの結果へ広げない。

**次のR-00の局所作業を実行できる準備は成立した。** 現在のfresh classpathと直接回帰7件を基準に、production importの明示・fixtureの検査責務を整理する。ソース変更を始める前に対象差分を具体化し、環境の再installや全root反復を開始条件にしない。

raw記録は `/workspace/cloud-setup/logs/phase4-r00-preflight-20261020/`：`maven-offline-validate.log`、`clean-test-compile.log`、`architecture-baseline.log`、`selected-test-results.json`、`fresh-import-observation.log`、`browser-smoke.log`（sandbox失敗）、`browser-smoke-privileged.log`（成功）、`source-before.json`。このmachine上の記録であり、remote保管や正式受入を意味しない。
