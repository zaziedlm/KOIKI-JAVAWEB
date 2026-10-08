# S1 B-1読取接続：source固定・preflight記録（2026-10-08）

**結果：preflightは未成立／実効制限差分はOWNER APPROVED（§6）、作成・実効確認待ち。** Git・環境・cacheとoffline compileを確認したが、既存選択testのDB資源上限を現行sourceのまま担保できない。新B-1 code／test／SQLは作成せず、DB／子JVM・browser smoke・回帰testも実行していない。

## 1. 承認・clean source固定

- [開始票§11](../../development/phase4-s1-stage-b1-read-connection-limited-start-review-20261008.md#11-owner承認条件付き限定開始2026-10-08)とAGENTSの個別承認を確認した。
- branch：`feature/phase4-s1-reference-foundation`。固定HEAD：`df3b653651e29dde9c288fb02f5b1c0a44cf1dab`、`docs: S1 B-1読取接続の方式と条件付き限定開始承認を記録`。開始時clean、upstream比ahead11。
- `08f4740`からの変更は承認・具体化文書5件のみ。Tooling／Reference main・既存test／migration・POM差分なし。Agentはadd／commit／pushしていない。
- OwnershipはTooling、今回の新規集合は承認済み6 class／52件候補。preflightの成功を新規集合・回帰・B-2のPASSにしない。

## 2. 環境・cacheの確認

| 項目 | 確認結果 |
|---|---|
| Java／Wrapper | Temurin21.0.12.1、Maven3.9.16、Windows11 |
| 資源 | 初回権限付き診断でmemory17,284,728 KiB（約16.48 GiB）。終了08:57:04 JSTでmemory17,033,428 KiB（約16.24 GiB）、disk46,374,703,104 bytes（約43.19 GiB）。開始条件8／10 GiB以上。連続monitorの最小値ではない |
| Docker | Server29.5.3到達。初回・終了とも稼働container0 |
| image | 既存`postgres:17-alpine` ID `18cfe3ef5e68`、`testcontainers/ryuk:0.14.0` ID `7c1a8a9a47c7`確認。pull／installなし |
| browser cache | 既存Playwright classpathの全JAR存在（欠落0）、既存Chromium／headless shell cache directory確認。起動は未実行でbrowser実行PASSではない |
| process | 終了診断ではJava20104／19024のみ、いずれも2026-10-07 12:02起動の既存process。今回のMaven終了後で、新DB・子JVMを起動していない。既存process／停止containerは操作していない |

通常権限ではCIMアクセス拒否とDocker named pipe接続拒否を確認。AGENTS・今回の限定承認と実行環境の権限付き承認手順に従い、同じread-only診断を再実行して成功した。Docker障害と判定せず、OS権限変更・承認拒否の迂回は行っていない。

## 3. offline build・classpath確認

| 対象／command条件 | 結果と意味 |
|---|---|
| Tooling `-o -B -ntp -Pjdbc -DskipTests test-compile` | FAILURE、4.539秒。既存S1 test sourceのIdentity／Audit／Security等のtest依存が不足。初回失敗logを保全 |
| Tooling `-o -B -ntp -Pjdbc,s1-contract,s1-web -DskipTests test-compile` | SUCCESS、8.200秒。既存POMのtest-only support profileを有効にし、publication storeはjdbcを維持。新依存追加・download・Web test起動なし。既存warningは修正しない |
| Reference `-o -B -ntp -pl koiki-reference-app -am -DskipTests test-compile` | SUCCESS、10.673秒。新B-1 sourceなし、test未実行 |
| E2E `-o -B -ntp -f build-support/reference-e2e-verification/pom.xml validate` | SUCCESS、0.905秒。browser／E2E未実行 |

各Maven実行区間だけ`MAVEN_OPTS=-Xmx768m`を設定し、finallyで元の値へ復元した。profile補足は既存test sourceのcompileに必要な条件の確認であり、PL2全suite・JPA store・S1 Web検証を実行する許可へ拡張しない。

途中、PowerShellでcommaを含むprofile引数をquoteしていないcommandが構文解析エラーとなり、Mavenは起動しなかった。`'-Pjdbc,s1-contract,s1-web'`とquoteして実行した。compile失敗の無条件反復ではなく、依存条件を確認して修正した再確認である。

## 4. 実効制限の阻害点

[PublicationRecoveryTest](../../../build-support/phase4-level2-verification/src/test/java/org/koikifw/buildsupport/phase4/PublicationRecoveryTest.java)は既定の`new PostgreSQLContainer("postgres:17-alpine")`をそのまま起動する。[ProcessCrashRecoveryIT](../../../build-support/phase4-level2-verification/src/test/java/org/koikifw/buildsupport/phase4/ProcessCrashRecoveryIT.java)の選択2 methodが利用する`crashAndRecover`も同じ構成である。

両testにはcontainer memory1 GiB／CPU1・PostgreSQL max_connections16・lock／statement timeout10秒のcreate／command設定がない。子JVMのlaunchにはheap指定がなく、pool上限等の実効確認もない。環境変数で子heap／poolを渡す案は検討できるが、それだけではTestcontainersのcontainer／DB制限を満たさない。

Referenceの既存Harnessはこれらを明示しているが、別moduleの既存PL2 testへ自動適用されない。稼働中containerへ事後変更する方法や監視だけで開始時の制限を代替せず、未制限のtestを実行してPASSにしない。

したがって、開始票の「既存test変更なし」と「選択PL2回帰の実効制限を満たす」が現行sourceでは両立しないことが判明した。Agentが開始票を具体化した際の不足であり、既存PL2の過去結果の取消しや新規52件の失敗ではない。

## 5. 訂正案・未達・次の判断

[開始票§12の訂正案](../../development/phase4-s1-stage-b1-read-connection-limited-start-review-20261008.md#12-preflightで判明したprofile実効制限の差分案2026-10-08owner確認待ち)に、既存test-only support profileの補足と、既存PL2 test2ファイル＋test-only資源helper1件の追加変更候補を記録した。明示選択時だけ資源・起動／接続条件を設定し、test methodの業務assertion・既存main／POM／migrationを変えない案。

新規6 class／52件、選択PL2回帰6件、Reference関連143件・既存99件＋E2E、資源・時間・raw上限は維持する。差分変更が必要なため本票の停止条件に従い、Owner確認前に既存testや新fixtureを作成・変更しない。

今回rawは非配布`build-support/phase4-level2-verification/target/s1-b1-read-20261008/preflight/`にcompile／validate log4件とsanitized環境JSON1件を保管、確認時合計85,478 bytes。今後の全run1 GiB予算へ含める。Gitではrawを配布しない。

Ownerが差分を採用した場合は承認文書をcommitし、clean sourceを再固定する。成立済みの環境・compile確認は保持し、source／資源／cacheの変化と実効制限の実装・検証方法を確認する。source不変・追加懸念なしのcompileを無条件に反復しない。preflight完了・新規検証PASS・Owner結果受入・実運用受入はまだ成立していない。

## 6. 訂正承認と再開条件（2026-10-08）

Ownerは[開始票§13](../../development/phase4-s1-stage-b1-read-connection-limited-start-review-20261008.md#13-12訂正のowner承認2026-10-08)で§12のprofile補足・既存test2件の最小資源設定差分・helper1件追加を承認した。§5のOwner review待ちは解消した。検証集合・上限・未達は維持する。

次は訂正・承認文書3件のcommitとclean source再固定、source／環境差分確認。その後に承認済み3ファイルの資源設定差分を作成・限定検証し、container／DB／子JVM／pool・接続予算・timeout・cleanupの実効値を確認する。実効制限成立前に新規52件の作成・検証を開始しない。

今回の承認反映ではcode／test／SQL・環境は操作していない。preflight全体の成立は引き続き未認定。git add／commit／pushも行っていない。
