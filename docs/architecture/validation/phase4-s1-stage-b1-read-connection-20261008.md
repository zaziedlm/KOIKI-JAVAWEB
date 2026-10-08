# S1 B-1読取接続：source固定・preflight記録（2026-10-08）

**最新状態：B1限定検証の実装結果・完了記録は `COMPLETE / OWNER APPROVED`（§24、2026-10-08）。** 新規52件・選択PL2回帰6件・Reference関連143件・既存99件＋package済みE2E1件、最終clean package／fixture非混入／source不変／cleanupを確認。先行失敗・D11／D12等の限界を保持。B-2／実運用／remoteの開始は別判断。

**先行資源helper結果：canonical property供給へ訂正後、選択IT2件PASS（§15）。** 子起動時pool2／idle0／接続待機10秒、4子のVM flags・pool log、container／DB設定・接続sampleとcleanupを確認。以前の既存4件PASSと合わせて選択回帰6件のPASSを保持する。今回の最終sourceで再実行した回帰とは区別し、失敗履歴も保全した。

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

## 7. 訂正後source・資源設定作成・限定検証（2026-10-08）

Ownerのcommit `fad5ded7b7db7c4cc0a860ada2862252a97e18d1`をclean sourceとして固定。`df3b653`以降の差分は訂正・承認・preflightの文書3件のみ。作成前の権限付きread-only診断でmemory17,186,460 KiB（約16.39 GiB）、disk46,572,163,072 bytes（約43.37 GiB）、Docker29.5.3、稼働container0とPG17 image不変を確認した。

承認済み`B1ResourceLimits.java`をtest scopeに追加し、既存2 testの資源設定導線を変更した。`koiki.b1.resource-limits.enabled=true`でだけ設定・assertionを有効にする。未指定時は既存container／launchを返し、B-1資源値を押し付けない。Publication側は実Hikari poolを@BeforeEachで確認。IT側の選択methodが共有する`crashAndRecover`のcontainerと通常子launchへ適用する。子同時1・有界起動・実VM flags／Hikari設定・失敗時終了確認をhelperに置いたが、子の経路はまだ未実行。

offline packageはSUCCESS、9.786秒。新helperのJAR entryなし。業務assertEquals／assertTrueの既存行差分0、@Testの件数は維持。Tooling／Reference main・POM／既存migration差分0。実行前後の3ファイルhash差分0。

`jdbc,s1-contract,s1-web`、B-1選択true、Maven／fork heap768 MiB、fork1／reuseForks=false、JUnit並列無効・offlineでPublicationRecoveryTest4件を実行した。各@BeforeEachで以下を確認した。

| 実効確認 | 結果 |
|---|---|
| container | memory1,073,741,824 bytes／nanoCPUs1,000,000,000 |
| DB SHOW | max_connections16、lock／statement／transaction timeout各10秒 |
| fork | maxMemory805,306,368 bytes |
| 実Hikari | maximumPoolSize2／minimumIdle0／connectionTimeout10000 ms |
| DB client connection sample | 各確認時3、予算8以内。全runの連続最大値の実証ではない |

DB IDは`2e3488ad844c946e1073b66338aba1e26def647bb5cd014e11b1676c6c17581c`。資源assertionは成功したが、test結果は4件／failure0／error4／skip0、class8.066秒・Maven18.353秒でFAILURE。全件が`probe_approval`不存在で失敗した。子JVM heap／pool、選択IT2件のPASSとcleanup、未選択時のruntime回帰は未検証で、資源制限全体の成立にしない。

## 8. profile失敗原因・停止・次の訂正案

compileに必要なtest-only support profileは、Maven test実行時にも依存としてclasspathへ入った。logではKOIKI Data StarterのFlyway先行実行・Framework migration3件の適用後、Application migrationが見つからず、既存probe tableが作られていない。compile依存の補足だけで既存PL2のruntimeを保持できるという前提に不足があった。既存assertionを弱めたりmigrationを先行改変してPASSにしない。

[開始票§14](../../development/phase4-s1-stage-b1-read-connection-limited-start-review-20261008.md#14-実効確認でのruntime-profile訂正案2026-10-08owner確認待ち)に、compile時だけsupport profileを使い、実行時はjdbc単独へ戻す案を記録した。既存test compilation結果を用いた直接Surefire／Failsafe goalで選択testを実行し、既存POM／main／migrationの追加変更を避ける候補。これは未実行で、runtime正常化を確認済みとはしない。承認済み上限・検証集合・未達は維持する。

失敗runのlog・system-properties／system-out／system-errを除いたXML、source-before／after hash、終了状態を非配布`target/s1-b1-read-20261008/resource-limits/`へ保管。preflightを含むraw11件、確認時合計224,239 bytes。09:52:06 JSTの診断で稼働container0、Javaは前日からの20104／19024のみ。今回DB／Maven／forkは終了し、子JVMは起動していない。memory17,074,632 KiB・disk46,379,380,736 bytesを確認した。

§8の回帰失敗停止条件に従い、同条件rerun・IT2件・新規52件へ進んでいない。現在差分はcode3件と本Evidence・開始票の計5件。git add／commit／pushは行っていない。

## 9. runtime profile訂正の承認と再開条件（2026-10-08）

Ownerは[開始票§15](../../development/phase4-s1-stage-b1-read-connection-limited-start-review-20261008.md#15-14訂正のowner承認2026-10-08)で§14の訂正案を承認した。compile専用のsupport profileとjdbc単独のruntimeを分け、直接Surefire／Failsafe goalによる既存4件・選択IT2件の限定再検証と、jdbcのmain依存によるTooling JARのpackage・runtime依存確認を採用する。

§13の承認文書commit・clean source固定は`fad5ded`で実施済み。今回は承認を記録し、既存code3件・source／環境差分を確認して再開する。失敗runを保持して既存4件の正常化と実効制限を確認し、成立時に選択IT2件へ進む。制限成立前に新規52件を作成・検証しない。実効制限全体の成立と結果受入は未認定で、git add／commit／pushは行っていない。

## 10. jdbc単独での限定再検証結果（2026-10-08）

HEAD `fad5ded7b7db7c4cc0a860ada2862252a97e18d1`不変。code3件のhashは§7後の記録と実行前後で一致した。再開前はDocker29.5.3・稼働container0、available memory18,911,048 KiBを確認。承認記録以外のsource変更・新依存取得をせず、既存compile済みtest-classesを再利用した。

共通条件はoffline、MAVEN_OPTS／fork heap768 MiB、fork1／reuseForks=false、JUnit並列無効、`koiki.b1.resource-limits.enabled=true`。以下の限定commandを実行した（module POMを`-f`指定）。

| 集合 | command条件 | 結果 |
|---|---|---|
| Tooling JAR | `-Pjdbc -Dmaven.test.skip=true package` | SUCCESS、2.058秒。test source再compileなし |
| 既存4件 | `-Pjdbc -Dtest=PublicationRecoveryTest surefire:test` | 4／failure0／error0／skip0、class8.824秒、Maven11.421秒 |
| 選択IT2件 | `-Pjdbc -Dit.test=ProcessCrashRecoveryIT#incompletePublicationIsDeliveredAfterProcessRestart+acceptedSendBeforeCrashIsNotDuplicatedAfterRestart failsafe:integration-test failsafe:verify` | 2／failure2／error0／skip0、class31.078秒、Maven33.710秒 |

JARのentry検査でKOIKI support library・B1 helper／fixtureの混入なし。JAR hashをrun rawへ保存した。Publicationのprobe schema欠落は解消し、4件の業務assertionは成功した。logのlistener例外は既存testが明示するprovider受理後の失敗注入であり、JUnitのERRORではない。実Hikari2／idle0／connectionTimeout10000 ms、container memory1 GiB／CPU1、DB max_connections16・各timeout10秒、fork heap805,306,368 bytesを再確認した。DB client sampleは各3で予算8以内だが、連続最大値の実証ではない。

選択ITは2件とも最初の子launch中に`B1ResourceLimits.start`の起動確認assertionで失敗した。子heap／pool確認と既存の送信／再起動assertionに到達していない。起動失敗の詳細原因は未確定。既存finallyが子logを削除するため、今回の子logは残っていない。helperの失敗経路で子強制終了・10秒以内の終了確認を行い、containerもtry-with-resourcesで終了した。停止条件に従い、同条件rerun・新規52件には進んでいない。

失敗・成功の親log、秘密を含むproperties／system-out／system-errを除いたXML、source hash・JAR hash・終了状態を`target/s1-b1-read-20261008/runtime-jdbc/`へ保存し、§7の失敗rawは別directoryに保全した。10:46:13 JSTの診断で稼働container0、Javaは検証開始前09:54起動の2660／4684だけ。今回の子・fork／Mavenは残存なし。memory18,592,052 KiB、disk47,884,967,936 bytesで下限を維持した。rawは終了状態追加前で18件／336,732 bytes、1 GiB未満。

現在差分はcode3件・承認／検証文書3件の計6件。codeの追加変更なし、main／POM／migration・Reference変更なし。git diff --check成功。git add／commit／pushは行っていない。[開始票§16](../../development/phase4-s1-stage-b1-read-connection-limited-start-review-20261008.md#16-子jvm起動失敗の診断保全再開案2026-10-08owner確認待ち)へ、helperでcleanup前のsanitized診断を有限量保全し、選択IT2件だけを再検証する最小案を提出した。採用前のcode変更・再実行はしない。

## 11. 診断保全の承認・限定再検証（2026-10-08）

Ownerは[開始票§17](../../development/phase4-s1-stage-b1-read-connection-limited-start-review-20261008.md#17-16診断保全のowner承認2026-10-08)で§16の最小訂正を承認し、訂正経緯の点検を依頼した。今回はhelperの失敗診断だけを変更した。起動判定・60秒待機・子heap／pool／DB値・既存業務assertionは変更していない。

失敗時は子の生存／自然終了コードを記録し、当該子の終了確認後、既存finallyが元logを削除する前に末尾最大65,536 bytesを読み取る。実際に渡したURL・username・passwordを除去し、接続情報の行を除去する。出力文字数も65,536以内に制限し、非配布run directoryへPIDごとのlog・待機経過・終了状態を保存する。UTF-8出力のbyte長は文字数と一致する保証ではなく、有限量の診断保全である。診断保存例外は元failureへsuppressedとして追加し、元failureを成功に置換しない。

開始10:54:36 JST、HEAD `fad5ded`不変。既存2 test・packaged JARのhash不変、稼働container0、memory18,639,520 KiB／disk47,839,879,168 bytesを確認した。変更したhelperを含むtest sourceのoffline `-Pjdbc,s1-contract,s1-web -DskipTests test-compile`はSUCCESS、8.154秒。runtimeは前回と同じjdbc単独の直接Failsafe goal・選択2 method・資源制限true・Maven／fork heap768 MiB・fork1／JUnit並列無効。今回の変更は失敗保全であり、正常起動条件の変更ではない。初回の失敗rawは維持し、同じ起動条件の診断付き再実行1回として扱う。

ITは2件／failure2／error0／skip0、class31.120秒・Maven33.726秒でFAILURE。子PID2388／12392は起動確認時に生存false、自然終了コード1、cleanup後も終了1。sanitized子logは各65,666／65,667 bytesとして保存できた。両logで、Flywayのmigration connection取得中にHikariPool-1が`total=2, active=2, idle=0`となり、10,012／10,013 msで接続取得がtimeoutしたことを確認した。`entityManagerFactory`のFlyway依存初期化が失敗し、SpringApplicationが終了した。これは60秒の起動待機を使い切った失敗ではない。

**確認済みの直接原因は起動時のpool接続取得timeout。** 接続2本を保持している内訳、既存4件では同じpool2でmigrationが成功した理由、起動工程と運用工程を分けた接続予算の成立は未確定。timeout延長・pool増加・Flyway無効化を根拠なく採用しない。子VM.flags／正常時pool検証と再起動の業務assertionに未到達であり、子資源制限PASSにはしない。依存の一部照合では既存4件classpathとJAR双方にBoot4.1.1／Hikari7.0.2／Flyway12.4.0を確認したが、classpath・設定全体の同値証明ではない。

10:56:35 JSTの権限付きread-only診断で稼働container0、今回の子／fork／Mavenなし、既存Java2660／4684だけ。一時`phase4-crash-*` directory0、memory18,562,300 KiB／disk47,803,453,440 bytesを確認した。今回の子2件・DB2件は逐次起動・終了し、元logは既存cleanupで削除、sanitized診断は保全した。rawは`target/s1-b1-read-20261008/diagnostic-preservation/`にcompile／親log・sanitized XML・子log2件・開始／終了状態・source hashを保存。全run27件／484,974 bytesで1 GiB以内。code差分3件・文書3件の計6件を維持、git diff --check成功、main／POM／migration・Reference変更なし。git add／commit／pushは行っていない。

回帰失敗停止条件に従い追加rerun・原因修正・新規52件を開始していない。既存4件PASSは診断の失敗経路だけの変更で影響しないため保持し、無条件に再実行しなかった。

## 12. 小訂正の経緯に沿った点検（Owner依頼、2026-10-08）

**判断：変更の目的・所有権・検証対象は一貫しているが、検証の起動条件を一括して確認する準備が不足し、対応が後追いになっている。** 「場当たり的でない」と無条件に評価しない。今回の失敗も新規B-1業務機能の問題ではなく、その前段の検証実行環境の未成立である。点検のみとし、承認済み診断保全以外の修正はしていない。

| 順序・訂正 | 根拠と対応 | 現在の判断 |
|---|---|---|
| §12／§13：compile依存補足 | jdbcだけでは既存test sourceがcompileできず、既存support profileで成功 | 既存POM内の依存再利用は妥当。ただしcompile成功だけではruntime同値を示さないという検討が不足した |
| §12／§13：資源導線3件 | 既存testがcontainer／DB／子heap／poolの上限を指定していなかった | 明示選択のhelperへ集約し、main等を保護した点は一貫。既存test変更なしで上限成立という開始票の前提は事前source確認で検出できた |
| §14／§15：compileとruntimeの分離 | support profile実行でmigrationが変わり4件ERROR、jdbc単独で4件PASS | POM／migrationや業務assertionを変更せず、実行classpathを戻して原因を除いた。compile／test実行／JAR起動を別条件として最初に整理できていなかった |
| §16／§17：子診断保全 | IT2件の起動失敗、既存finallyが元logを消すため原因不明 | 起動条件を緩めず失敗証拠を残す対応は妥当。ただし診断保存とcleanupをセットで設計すべきだった。今回、Flyway接続timeoutまで原因を絞れた |

累積差分を確認し、既存2 test methodの業務assertion・@Test件数の改変、test除外による件数削減、main／POM／依存宣言／migrationの変更、Reference／Frameworkへの範囲拡張は認めなかった。helper未選択時は従来container／launchを返すsource構造を保持しているが、未選択の全runtime回帰は未実施である。失敗を保全・承認後に再開し、PASSと未達を分けている点も維持した。

残る点検事項は次の通り。

- **起動工程の接続予算：** pool2／総計8の設定値だけではFlywayを含むJAR起動を成立させていない。追加修正前にcompile・既存test起動・JARの初回migration・再起動・運用時の接続利用を同じ表で照合し、既存4件との違いと接続保持の内訳を特定する必要がある。
- **timeoutの適用範囲：** helperはDBのtransaction_timeoutとSpring default transaction timeoutを10秒に設定している。開始票の採取・読取transaction10秒候補より広く、listener等にも影響し得る。pool問題が解消しても、pause／再起動の観測を資源設定が変えていないか確認が必要。今回は変更していない。
- **観測の限界：** 総接続8以内は時点sampleで、全runの連続最大値ではない。起動判定はlog文字列に依存し、正常起動後のVM.flags・pool値は未実証。これらを「実効制限全体成立」にまとめない。

次の判断材料は起動・運用の資源利用とclasspath／設定差分の一括整理であり、すぐpoolやtimeoutを少しずつ増やす案ではない。必要な原因修正が判明した時点で、累積差分・既存条件への影響・残る未達を提示してOwner判断に戻す。検証成立後の新規52件・結果受入・実運用保証への境界は維持する。

## 13. Owner点検確認後の接続内訳調査（2026-10-08）

Ownerは§12を確認し、「整理を納得し、作業を続けます」と述べた。[開始票§18](../../development/phase4-s1-stage-b1-read-connection-limited-start-review-20261008.md#18-点検記録のowner確認と原因調査の続行2026-10-08)へ記録した。点検整理の確認と原因調査続行であり、preflight成立・実効制限PASS・実装結果受入ではない。

### source／classpathと起動工程の照合

HEAD `fad5ded`と既存2 test・Tooling JARのhashは不変。JARのBOOT-INF/lib78件のうち、jarmode-toolsを除く77件は既存4件のclasspathに同名JARが存在し、内容のSHA-256が一致した。BOOT-INF/classesのmain class／resourceもtarget/classesと一致した。JAR entryでKOIKI support依存・B1 test helperなしは前回確認を維持する。既存4件のclasspathにはtest依存等が追加されているため、全classpath・autoconfiguration・property供給経路まで同値とはしていない。

cached Flyway12.4.0のbytecodeを`javap`で読み、`JdbcConnectionFactory`が初期接続を保持し、`Database`がそれをmain接続として利用すること、`Database.getMigrationConnection`がsingle connection設定でなければ別接続を取得すること、`DbMigrate`がconstructorでmigration接続を要求することを確認した。PostgreSQLのsingle connection分岐はtransactional lock設定に依存する。Boot4.1.1のmigration DataSource選択には、専用Flyway接続設定がある場合のSimpleDriverDataSourceと、設定がない場合の通常DataSource再利用の分岐がある。これらは分岐の存在の確認であり、今回の全実行分岐・保持者を特定した証拠とはしない。

| 工程 | 接続／設定・確認できたこと | 未確定・判断 |
|---|---|---|
| compile | support profileで既存sourceをcompile。DB接続なし | runtime／package条件とは別 |
| 既存4件の起動・migration | jdbc単独、dynamic propertyでHikari2／idle0・接続待機10秒。既存V1／V2適用成功 | 成功時の接続取得stack・最大同時本数は採取していない |
| JARの初回migration | envでpool2／idle0・待機10秒を要求。初期接続取得stackとmigration接続取得timeoutを確認 | 子の実maximumPoolSizeは起動後assertionへ未到達。`active=2`だけを実max値2の証明にしない。残る保持接続1本が未特定 |
| JAR再起動 | 初回子起動失敗のため未到達 | migration確認・replay・子同時1の正常経路PASSなし |
| 運用／収集／読取 | 候補予算は子2＋writer2＋reader2＋管理2＝8 | 新B-1未作成。起動工程も含めてこの内訳で成立したとはしていない |

### 取得stackを追加した限定診断

helperの明示選択時にHikari leakDetectionThreshold2000 msとFlyway DEBUGを診断として追加した。pool／timeout／DB／heap／migration／既存業務assertionを変更せず、失敗logを先頭16 KiB＋末尾48 KiBで読む形へ変更した。中間を省略し、redaction後の出力文字数の上限も維持する。2秒保持の警告は取得元の観測であって恒久的な接続漏れの断定ではない。

11:25の再開前に稼働container0・資源下限成立・既存2 test／JAR不変を確認した。offline support profile test-compileはSUCCESS、8.349秒。jdbc単独の直接Failsafe goal、選択IT2 method、Maven／fork heap768 MiB・fork1／JUnit並列無効は前回と同じ。診断条件を変えた1回の実行で、2件／failure2／error0／skip0、class31.089秒・Maven33.737秒でFAILUREとなった。

子PID15768／4460は自然終了コード1、cleanup後も終了1。両logでHikariの2秒保持stackが`JdbcConnectionFactory.<init>`→`FlywayExecutor.init`→`Flyway.migrate`→Boot initializerを示した。失敗処理では同じ接続が`returned to the pool (unleaked)`と記録された。timeoutのstackは引き続き`Database.getMigrationConnection`→`DbMigrate.<init>`である。**初期接続1本の取得・返却は確認できたが、active2の残る1本の保持元は未確定。Flywayの必要本数を3と断定せず、恒久的なleakとも扱わない。** Flyway DEBUG／HikariConfigの実値logが十分に出たことも実証していないため、指定したlogging設定だけで観測成功としない。

11:31:31 JSTの診断でcontainer0、今回の子／fork／Mavenなし、既存Java2660／4684だけ、一時phase4-crash directory0。memory18,548,348 KiB／disk47,807,832,064 bytesで下限を維持した。rawはconnection-ownership directoryにcompile／親log・sanitized XML・子log2件・開始／終了状態・source hash・cached bytecodeを保存し、前回rawも保持。全run42件／1,034,980 bytes、1 GiB以内。業務method／既存main／POM／migrationの追加変更なし。git add／commit／pushなし。

回帰失敗のため追加実行・原因修正を停止した。次は[開始票§19](../../development/phase4-s1-stage-b1-read-connection-limited-start-review-20261008.md#19-保持接続の残る1本を特定する診断案2026-10-08owner確認待ち)の、package logger指定・起動中のDB状態1回採取・既存取得stackを同じ時系列で保全する診断追加案をOwner判断へ戻す。pool増加・timeout延長・Flyway無効化／lock方式変更は提案・実装していない。新規52件へ進まず、既存4件PASSを保持する。

## 14. §19採用承認後の1回診断・判断に使える結果（2026-10-08）

Ownerは[開始票§20](../../development/phase4-s1-stage-b1-read-connection-limited-start-review-20261008.md#20-19採用承認と判断に必要な情報への集中2026-10-08)で§19採用を承認し、原因追及そのものを目標にせず、得られる情報へ集中するよう指示した。helperの明示選択時だけpackage loggerをcommand propertyで指定し、起動待機中1回のread-only管理SELECTを追加した。管理接続1本・connect／socket／query timeout10秒・try-with-resources終了、SQL分類とstate／waitだけの有限量記録を実装。SQL本文／bind値／credentialは保存していない。9行で打ち切って予算8超を拒否し、追加poolは作らない。

前回のHEAD `fad5ded`・既存2 test／Tooling JAR不変を確認。起動前container0・メモリ／ディスク下限成立。offline support profile test-compileはSUCCESS、8.309秒。jdbc単独、既存IT2 method、fork1／reuseForks=false、JUnit並列無効、Maven／fork heap768 MiB・B-1選択trueで1回実行した。結果は2件／failure2／error0／skip0、class31.590秒、Maven34.226秒でFAILURE。初期子起動失敗を確認し、追加実行を停止した。

| 取得できた情報 | 両子での結果 | 実装判断への意味 |
|---|---|---|
| Hikari起動時config log | maximumPoolSize10／minimumIdle10／connectionTimeout30000 ms、leakDetectionThreshold0 | helperのenv要求2／0／10000・診断2000と一致していない。起動初期からの設定成立を認定できない |
| 接続取得timeout | 約10001／10005 ms、total2／active2／idle0 | 初期logとは別時点。後段の待機10秒だけを起動時からのpool制限証明にしない |
| 起動約4秒時のDB sample | 子2接続はともにidle・Client／ClientRead、SQL分類OTHER／TRANSACTION_END。管理観測1を含め計3 | この1回の観測では予算8以内。DB内のlock待機は観測されていないが、別時点の待機や全run最大値を否定しない |
| migration進行 | validate2件・schema history作成まで。業務migration適用完了・正常起動は未確認 | VM.flags・pool正常時assertion・既存再起動assertionへ未到達 |
| 取得stackと返却 | Flyway初期接続取得・失敗時返却を再観測 | 残る1本の完全特定は未達だが、今回の次判断に必須とはしない |

子PID9412／9916のsampleは各elapsed4054／4064 ms、管理接続は直後に閉鎖。両子は自然終了1・helperの終了確認成功。11:44:19 JSTの診断でcontainer0、今回の子／fork／Mavenなし、既存Java2660／4684だけ、phase4-crash一時directory0。memory18,425,500 KiB／disk47,907,110,912 bytesで下限成立。rawはstartup-observationへcompile／親log・sanitized XML・子log2件・SQL分類sample2件・source hash・開始／終了状態を保存し、前回rawを保持。全run54件／1,311,728 bytes、1 GiB以内。git diff --check成功、main／POM／migration・Reference変更なし、git add／commit／pushなし。

**次の判断：** 採用済みの数値を増量せず、子設定供給をcanonical propertyへ揃え、pool初期化時から2／0／10000を確認する。[開始票§21](../../development/phase4-s1-stage-b1-read-connection-limited-start-review-20261008.md#21-子pool設定供給の限定訂正案2026-10-08owner確認待ち)にhelper launchだけの限定案を用意した。既存4件のdynamic propertyと同じproperty名で供給し、URL等の秘密はenvに維持する。これで正常化することは未検証であり、根本原因の断定や起動時pool3必要の認定ではない。初期logと後段の値の不一致という得られた証拠に基づく案として、変更・限定検証のOwner判断に戻す。保持元の完全追跡・同条件診断反復・pool増加・timeout延長は行わない。

## 15. §21限定訂正・子資源制限の確認結果（2026-10-08）

Ownerは[開始票§22](../../development/phase4-s1-stage-b1-read-connection-limited-start-review-20261008.md#22-21限定訂正検証のowner承認2026-10-08)で§21の限定検証を承認した。helperの明示選択時launchで、Hikari数値env4件を除去し、maximum-pool-size2／minimum-idle0／connection-timeout10000／診断leak-detection-threshold2000をcanonical command propertyで渡す形に変更した。URL／username／passwordはenvを維持し、秘密をcommand lineへ出していない。数値・transaction timeout・Flyway／migration・既存test methodの業務assertion・件数は変更していない。

HEAD `fad5ded`・既存2 test／Tooling JARのhash不変、開始container0・既存Javaのみ・資源下限成立を確認した。offline support profile test-compileはSUCCESS、8.403秒。jdbc単独の直接Failsafe goal・選択IT2 method・Maven／fork heap768 MiB・fork1／reuseForks=false・JUnit並列無効・B-1選択trueで、新しい供給条件を1回実行した。

**結果は2件／failure0／error0／skip0、class24.619秒・Maven27.271秒でSUCCESS。** 両caseとも初回子を終了させてから次子を起動し、既存のPROCESSING／送信数観測、再起動後COMPLETED／送信数1のassertionをそのまま通過した。今回はSQL migrationの事前代行・Flyway無効化・pool増加・timeout延長による成功ではない。

| 確認対象 | 結果・証拠 |
|---|---|
| 初期pool・初回migration | 最初の子PID20432のlive first.logで、11:48:36にconnectionTimeout10000／maximumPoolSize2／minimumIdle0を確認。既存V1／V2適用成功、11:48:38正常起動。最初のlogで10／10／30000となる不一致はこの観測で解消 |
| 4子のpool／heap | PID20432→29532、20748→18152。各launchのHikari config log assertionが2／0／10000で成功、jcmd VM.flagsがMaxHeapSize805306368で成功。child同時1のhelper判定成功 |
| container／DB | DB2件を逐次利用。各子launchでHostConfig memory1073741824／nanoCPUs1000000000、SHOW max_connections16・lock／statement／transaction timeout各10秒を確認 |
| 接続予算 | 起動中1回の管理SELECTと起動後確認のsampleは4子とも各3。管理接続は各採取後に閉鎖し予算8以内。全実行の連続最大値ではない |
| 既存4件との関係 | §10のPublication4件PASSを保持。今回変更はその経路が使用しないlaunch設定だけで、既存2 test hashも不変。4＋今回2＝選択回帰6件。単一run／同一helper hashで全6件を再実行した結果とは区別 |

DB IDは`2901ee89802b3ea61caf332101a5f587e1331bbe131ad3e87a1b1209d0efa0f0`と`bbf028299aeb72892317f2301ff92153b638a5ed1f118f207a6a796de1db6996`。最初の子のlive log抜粋は、元logを既存finallyが削除する前にtoolで確認し、観測記録として非配布rawへ保存した。4子のhelper検証結果は親log／resource-values.txtに保全し、正常子の完全logは既存cleanupどおり削除した。全4子の「先頭のpool log」を個別に保存した証明ではなく、初回live確認と各launchのconfig assertionを区別する。

11:49:52 JSTの終了診断でcontainer0、今回の子／fork／Mavenなし、既存Java2660／4684だけ、phase4-crash directory0。memory18,277,336 KiB／disk47,752,577,024 bytesで下限成立。rawはcanonical-pool directoryにcompile／親log・sanitized XML・source hash・resource値・4子のSQL分類sample・開始／終了状態・初期pool観測記録を保存。観測記録追加前は全run69件／1,327,383 bytes、追加後も1 GiB以内。以前の失敗rawは維持した。

**今回の限定資源確認と選択IT2件は成立。** 当初のpool初期値不一致を供給方式の訂正で解消し、実装判断に必要な正常起動・再起動・上限・cleanupを確認できたため、保持接続の完全特定やライブラリ内部の原因追及は続けない。接続最大値の連続証明、広いtransaction timeout適用の本番保証、未選択の全Tooling runtime回帰を認定するものではない。既知D11／D12と限定検証の保証境界を維持する。

差分はcode3件＋文書3件＝6件、今回の追加code変更はhelper1件だけ。main／POM／migration・Reference変更なし、git diff --check成功、git add／commit／pushなし。新規52件を作成・実行しておらず、結果受入はOwnerの別判断。次は本差分・検証記録のレビュー材料として、この資源確認結果と残る未達を用いる。

## 16. 引継ぎ後のsource／環境差分・残preflight（2026-10-08）

Ownerの再開指示を受け、[次セッション引継ぎ](../../development/phase4-s1-stage-b1-read-connection-next-session-handoff-20261008.md)から確認を再開した。branchは`feature/phase4-s1-reference-foundation`、HEADは`23379b352bfb9ef261789290a3acf40a6805c7e6`で一致。確認開始時の追跡対象差分は0、未追跡は引継ぎ文書1件だけだった。承認済みsourceを変更せず、本節と非配布preflight記録を追加した。新規52件の作成・実行、既存回帰の再実行、clean package、commit／remoteは行っていない。

| 確認項目 | 現在の結果・限界 |
|---|---|
| source | `canonical-pool/source-after.json`の既存test2件・`B1ResourceLimits.java`と現在のSHA-256が全件一致。`23379b3`から追跡対象のcode／POM／migration差分なし |
| Docker／cache | 通常sandboxではnamed pipeとCIMを拒否。承認済みの権限付き読取診断でServer29.5.3、稼働container0、PG17 `18cfe3ef5e68`／Ryuk `7c1a8a9a47c7`の不変を確認。pull／installなし |
| 資源／既存process | 空きmemory18,238,928 KiB、disk47,297,396,736 bytesで下限成立。Java PID2660／4684は前回終了記録と起動時刻も一致。既存processを操作していない |
| JDK／Wrapper | Temurin21.0.12.1、Wrapperによる`-o -v`でMaven3.9.16を確認。build／testの無条件反復なし |
| browser classpath | 既存E2E Surefire XMLの`java.class.path`からJARだけを抽出し、全JAR存在を確認。存在しない`target/classes`と空entryは実起動classpathから除外（このmoduleはtest所有）。既存Playwright1.62.0／driver／driver-bundleを使用 |
| browser実起動 | 非配布`tmp/b1-preflight-20261008/BrowserPreflight.java`をJava source launcher・heap768 MiBで実行。`PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD=1`、headless起動timeout10秒・page timeout10秒、外部URLなしのlocal HTML titleを確認。`BROWSER_PREFLIGHT_PASS version=151.0.7922.34`、終了code0。環境変数はfinallyで復元 |
| browser cleanup | Playwright／Browserをtry-with-resourcesで終了。終了後の権限付きprocess診断では今回のJava／browser／driver残留なし、既存Chrome／Nodeと既存Javaのみ。container0、phase4-crash directory0。E2E journeyの実行PASSとは別 |
| Tooling package | 現JAR SHA-256は引継ぎの`9FFC2607B1C22A65E238A437BDE496EF75E3D9F879205D583888187F434399D5`と一致。BOOT-INF/lib78件、B1／b1fixture／s1-b1 entry0、support Starter（identity／audit／session／security）entry0。既存jdbc packageを再利用し、最終sourceのclean packageとは区別 |
| Reference package | 現JAR SHA-256は`33997D4275DF3DC71037364E87BD55D6672D56CE71EAFFAF7A6D7FFE3A8243DE`として今回観測。B-1最終package／回帰への対応付けは未実施であり、過去のReference／E2E PASSを今回のPASSにしない |

### raw保全と累積予算の残条件

確認時の既存B-1 rawは70ファイル／1,327,896 bytes。全件をGit除外済みの`tmp/b1-raw-preservation-23379b3-20261008/s1-b1-read-20261008/`へコピーし、元とコピーのSHA-256全件一致を確認、同directoryの親に`raw-manifest.json`を保存した。元rawは削除・上書きしていない。この保全先はMavenのmodule／reactor `target`外であり、今後のclean前にも最新分の保全とhash照合を行う。コピーを含めてもraw1 GiB上限内だが、累積は元データだけでなく保全コピーも含めて管理する。

既存rawのMaven `Total time`17記録は合計261.820秒。これはMaven外の診断・cleanup・作成作業・承認待ちを含む全実経過や標準作業量ではない。時刻記録は初回Maven終了08:20:39〜最新cleanup11:49:52にまたがるが、全区間を検証時間とも承認待ちとも推定しない。全実経過240分・標準作業量12時間進捗確認／28時間停止の累積台帳は今回参照したB-1 raw／引継ぎに存在せず、残予算を確定していない。Ownerへ既存集計記録の有無を確認中。

上記確認後、Ownerは「厳密でないので、想定できる範囲で大丈夫です」と回答した。厳密な未計測値の復元を開始必須条件とせず、概算と実測を分けて進める方針を採用する。§7の上限を増やす承認、累積のリセット、検証結果の受入には読み替えない。

概算管理の起点は、前セッションの08:20頃〜11:49:52の約210分（診断・承認待ち等も含めた保守的な時刻幅）とする。今回の読取・preflight整理は20分を仮置きし、累積約230分／240分、残り約10分と扱う。これは未計測区間を含む計画用見積りであり、実検証時間230分の認定ではない。標準作業量は初回preflightからの約3.5時間＋今回約0.5時間＝約4時間を暫定目安とし、今後の作業記録を加算する。12時間相当の進捗確認／28時間相当の停止は維持する。前提の見直しが必要なら、実測・Owner待ち等の区間と根拠を分けて再評価する。

**source／環境差分と残るpreflightの確認は成立。** 新規52件を作成・検証する技術条件は確認した。残る約10分の保守的予算で新規52件・各回帰・最終clean packageまで完了できるとは見積もらず、今回の再開ではpreflight記録とraw保全までとする。新規集合の開始時には残予算と予定するclass・cleanup所要時間を対応付け、上限内の有界実行か、累積見積りの根拠付き再評価を先に記録する。最終clean package・Reference関連143件／既存99件＋E2Eは後続工程として残す。D11／D12、I05の未保護窓、網羅性懸念とOwner結果受入の別判断を維持する。

## 17. 累積予算の概算方法の再評価（2026-10-08）

Ownerから、残予算の評価にも大幅なブレがあり、より保守的に判断できるとの指摘を受けた。§16の230分消費／10分残は、承認待ち・読取・作成時間を検証枠へ一括計上した仮置きであり、以後の開始可否を決める固定値として使わない。検証時間240分と標準作業量16〜28時間を区別する。上限の変更や累積リセットは行わない。

4つのraw start／end記録を再照合した。diagnostic-preservation118.167秒、connection-ownership372.175秒、startup-observation135.429秒、canonical-pool95.385秒、合計721.155秒（約12.02分）。この区間には各runのMaven・診断・cleanupを含むため、区間内のMaven時間は重複加算しない。4区間外のMaven記録は99.646秒で、両者の合計は820.801秒（約13.68分）。これは記録のある区間の集計であり、未記録のsetup／診断／cleanupを含む完全な累積値ではない。前回browser実起動はtool観測3.149秒で終了した。

計画上は、記録約14分に未計測区間とpreflight副作用の余裕約46分を置き、**過去消費枠60分／残枠180分を暫定管理値**とする。60分は実測値でも証明された上界でもなく、Ownerが認めた概算運用における保守的な予算配賦である。210分の時刻幅も参考履歴として保持し、未計測を0分とは扱わない。未記録の長い実行が判明すれば管理値を上方修正する。全残工程の完了を180分で保証するものではない。

次classはclass上限15分にsetup／cleanup予備5分を合わせた20分を先に確保し、実開始〜cleanup完了をmanifestへ記録して累積へ加算する。以後もclass逐次、残枠が次classとcleanupの予約分を下回れば開始しない。各集合の上限、新規計90分、全検証240分、raw／再実行／資源／失敗停止条件を維持する。承認待ち・文書／実装作業は検証時間と分けて記録し、標準作業量12時間の進捗確認／28時間の停止に適用する。§16の標準作業量約4時間も暫定の作業経過目安であり、標準時間を実測した値として扱わない。

今回の再評価は文書・rawの読取と本節の追記のみ。preflightの技術条件成立は保持し、新規52件・既存回帰は今回も未実行。次の開始を止める根拠として「残り10分」を使わず、上記の概算枠とclassごとの実測管理を用いる。

## 18. 承認済みB-1初稿作成・process entry差分の提示（2026-10-08）

Ownerは§17の再評価を確認し、「認識しましたので、次へ進めましょう」と指示した。HEAD `23379b3`と既存未commit文書を保持し、Tooling test専用の`B1ReadContract`／`B1SourceCollector`／`B1EvidenceLedger`／`B1JdbcReadClient`と`src/test/resources/s1-b1/read-source.sql`の初稿を作成した。process harnessと新規6 test classは未作成。初稿を採用済み完成実装や52件の検証結果として扱わない。

| 契約／case | 初稿で具体化した対応（未検証） |
|---|---|
| B-C01／T01〜T10 | `Target`の全tuple、`Manifest`と生成publicationの一度固定、元row fingerprint、snapshot revision・期限、`b1_read.target`の現行row照合。listener／run識別の強制は作成途中 |
| B-C03／B-C04／P01〜P08 | providerのevent／key／receipt ID照合、複数／矛盾拒否、行なしUNKNOWN、固定payload／recipientと観測・key期限。stub本体へのpayload検査追加なし |
| B-C02／S01〜S08 | `Stop`にrun／世代／source・終了／強制終了／制御／期間を分離。管理下実process台帳を作るharnessは未作成 |
| B-C05／E01〜E10 | 別schema、一意EvidenceKey、長さ付きcanonical本文、先行commit後の参照返却、同key異本文拒否、append-only失効、UPDATE／DELETE拒否trigger |
| B-C07／B-C08／R01〜R08 | SELECT専用view・role、read-only repeatable-read transaction、connect／socket／query timeout10秒、driver例外・credentialを出さない拒否。実role／timeout検証は未実行 |
| I01〜I08 | 通常子生成とfocused seedの区別、全tuple固定後のcollector／view／reader経路。実子連携、cleanup、JVM再起動は未実装・未実行 |

既存main／POM／dependency／migration／既存test／Reference／Frameworkの変更なし。offline `-Pjdbc,s1-contract,s1-web -DskipTests test-compile`はSUCCESS、8.909秒、36 test sourceのcompile成立。`MAVEN_OPTS=-Xmx768m`はfinallyで復元、test／SQL／DB／子JVMは起動していない。既存warning2件を変更していない。logは非配布`tmp/b1-preflight-20261008/draft-compile.log`に保持する。暫定過去枠60分に実compile約0.149分を加算し、文書／作成作業・Owner待ちは別記とする。実測compileだけで完成や回帰PASSを認定しない。

cached Modulith2.1.1のbytecodeと既存Tooling sourceを読取り、publication IDがevent発行後にModulith constructorの`UUID.randomUUID()`で生成されること、既存通常JARに独立証拠を読取るprocess entryがないことを確認した。E10／I07は同一JVMのclient再作成だけでは成立しない。追加entry前に差分を提示する開始票§4に従い、[開始票§23](../../development/phase4-s1-stage-b1-read-connection-limited-start-review-20261008.md#23-b-1作成中の固定順序test読取process-entryの具体化案2026-10-08owner確認待ち)へ、事前通知binding＋生成publicationの一度固定と、既存test helper内のSELECT専用読取main／逐次子起動案を提出した。追加entryは未作成・未実行。新規52件を実行せず、本差分のOwner判断を待つ。既存開始承認・preflight結果とD11／D12の未達は保持する。

## 19. §24採用・52 invocation作成・部分実行とS cleanup停止（2026-10-08）

Ownerの「2点の採用を承認いたします」を[開始票§24](../../development/phase4-s1-stage-b1-read-connection-limited-start-review-20261008.md#24-23の2点採用のowner承認2026-10-08)へ反映した。初回固定source `23379b3`とpreflightを保持し、作成中差分を記録して続行。5 helper＋6 class＋test SQL＋READMEを作成した。offline compileは対象10件作成後8.392秒、残helper／全6 class後8.880秒、harnessの例外保全整理後9.046秒、S／I枝補完後8.929秒でSUCCESS。compileの既存warning2件は未変更。後続に追加したclass選択conditionは未compile・未実行であり、現在の全source検証完了とは扱わない。

### 実装とcase ID対応

| class／method | invocation対応と主な境界 |
|---|---|
| `B1TargetReadTest.targetBranches` | @ValueSource順序の[1]〜[10]＝T01〜T10。厳密対象、一度固定、別publication／環境／event／listener／attempt／削除、revision・期間・供給不能 |
| `B1ProviderReadTest.providerBranches` | [1]〜[8]＝P01〜P08。receipt／key照合、行なしUNKNOWN、取得不能、複数／矛盾、immutable再照会、payload／宛先・期限・別event混同 |
| `B1EvidenceLedgerTest.evidenceBranches` | [1]〜[10]＝E01〜E10。一意key、本文・主体／run照合、先行保存失敗、ACL／DB側UPDATE・DELETE拒否、失効、E10の別読取JVMでの同参照・digest保持 |
| `B1ReaderPrivilegeTest.privilegeBranches` | [1]〜[8]＝R01〜R08。view SELECT・raw／書込／別schema拒否、誤credential／source、query timeout、失効照会不能時拒否、既存JAR非混入・sanitized拒否 |
| `B1StopObservationTest.stopBranches` | [1]〜[8]＝S01〜S08。管理下全子・生存／強制終了、世代・run／source／JAR・期間、再起動／制御喪失、停止file／advisory lockだけでは証拠なし |
| `B1ReadConnectionIT.connectionBranches` | [1]〜[8]＝I01〜I08（未実行）。通常子→元DB→collector→view→reader、受理／fixture未受理、採取中変更、I05未保護窓、保存失敗、I07読取JVM再起動、異常終了cleanup |

計52 invocationを作成した。件数はcoverage候補であり完全性を認定しない。複数負例を含むmethodの枝はsource内のassertionで識別する。供給元の観測分類を操作許可に変換しない。focused T／P／E／RではDB seed＋停止modelを使用し、実process停止と混同しない。通常子JVMからの生成・停止はS／Iの経路。Iのcollector準備はB1専用schemaを先に作り、通常子が既存Flyway V1／V2を適用してから元tableに依存するview／grantを作る構成で、既存migrationを改変しない。

### 部分実行結果（source／runを分離）

| run directory | 結果 | 終了・保証限界 |
|---|---|---|
| `target-first` | T10件PASS、failure／error／skip0、class5.559秒 | 先行helper／role／listener sourceで実行。後続helper補完後のT再検証は未実行。初回runに全未commit source hash台帳はなく、後続runの完全なhash対応とは区別する |
| `B1ProviderReadTest-first` | P8件PASS、failure／error／skip0、class5.503秒 | 即時確認で当該Ryuk1件。後続13:26:16の自然終了・container0・既存Javaだけを別JSONに保全。手動container操作なし |
| `B1EvidenceLedgerTest-first` | E10件PASS、failure／error／skip0、class6.817秒 | E10はPID16272／17932で同参照・digestを確認、失効後PID6096は拒否exit2。子は逐次終了。Ryuk即時／10秒後残留と最終13:32:35のcontainer0を分けて保全 |
| `B1ReaderPrivilegeTest-first` | R8件PASS、failure／error／skip0、class6.504秒 | manifestのcleanup true、source stable true。statement取消は実測、connect／socket10秒は設定確認。実接続timeoutの強制終了保証は未実証 |
| `B1StopObservationTest-first` | S8件、failure0／error7／skip0、S08だけPASS、class42.692秒 | S01〜S07で`B1ProcessHarness.close`の子log削除がWindows使用中エラー。一時directory7件残存。次classを停止。S01〜S07は他assertionだけのPASSにも読み替えない |

後続4 runの`source-before.json`でtest／helper／SQLのSHA-256を記録し、run中不変を確認。P／E／RとSの間にはS／Iの枝補完差分があり、単一sourceで52件がPASSした結果ではない。新規invocationの実行は44件、I8件は未実行。先行Tを含む部分PASSと現在の全sourceでの最終検証を区別する。

### 資源・cleanup・失敗保全

全実行はjdbc単独の直接Surefire goal、Maven／fork heap768 MiB、fork1／reuseForks=false、JUnit並列無効。B1ResourceLimitsがcontainer memory1 GiB／CPU1、DB max_connections16、DB lock／statement／transaction各10秒を確認。focused DBの終了前sampleは接続1、Sの起動sampleは子pool＋管理接続の予算内を確認。全経路の連続最大接続数やheap以外のmemoryを証明したものではない。

setup管理者（container user）・通常fixture writer `b1_fixture`・採取writer `b1_writer`・SELECT専用reader `b1_reader`を分離。focused raw seedのV1／V2もfixture roleで作成。通常子はfixture credentialをENVで受け、B1独立schemaのownerにならない。読取entryはcompile済みtest-classes＋cached PostgreSQL driverだけでE10を実行できた。子log／system-propertiesの配布は行わない。

S失敗後にcontainer0／当該Javaなしを確認し、確認済みworkspace内の当該directory7件のlogをcredential／接続設定の行を除去して各64 KiB以内（先頭16＋末尾48）で`sanitized-children/`へ保全。元logとmarker、空directoryを非再帰でcleanupした。保全log7件／458,752 bytes。13:43:14 JSTの再診断でcontainer0・既存Java2660／4684だけ。未解決のfile保持元は調査・断定せず、[開始票§25の最小cleanup訂正案](../../development/phase4-s1-stage-b1-read-connection-limited-start-review-20261008.md#25-s01s07-cleanup失敗限定訂正案2026-10-08owner確認待ち)を提出した。追加実行・cleanup原因修正は未実施。

Ryuk自然終了は子／fork終了より後になることを観測した。runtime runnerは当該containerの自然終了をcleanup予約枠内で有限に観測するだけで、子／JDBCの10秒を延長せず、手動停止を行わない。即時判定のfalseを消さず、follow-upで最終状態を追跡する。これをRyuk全条件の終了時間保証とはしない。

確認時のB1 rawは107件／1,869,498 bytes、既存保全コピーは1,339,521 bytes。合計約3.21 MBで1 GiB以内。失敗XML・sanitized親log・source hash・resource／cleanup manifestを保全し、同原因rerunはしていない。過去消費枠60分＋先行compile0.149分に今回compile／cleanup完了までの各run実測を加算し、保守的に累積約74分／残約166分を管理目安とする。待機を含む時刻幅と純実行時間を同値にせず、作業量は暫定約4時間に今回作成・整理の概算約0.5〜1時間を加算する。12時間進捗確認／28時間停止、新規90分／全240分の上限を維持する。

最終clean package・現在sourceでのT再検証、選択PL2回帰6件・Reference143件／既存99件＋E2Eは未実行。既存3ファイルhash・Tooling JAR hash不変と`git diff --check`を確認。現在のclass選択conditionは未compile。Owner結果受入・B-2・実運用・網羅性認定・remoteは未成立。commit／pushなし。

## 20. §25承認後のcleanup限定訂正とS再検証（2026-10-08）

開始票§26へOwnerの§25最小訂正承認を記録した。変更はtest-only `B1ProcessHarness`の終了・file cleanupだけ。全管理子の終了待機10秒・非生存確認とstdin／stdout／stderr close、directory列挙streamのclose後の既知file削除、最大10秒の有界再観測を追加した。失敗時は終了状態・connection／credential行除去済みlogを保全し、logは入力1 MiB超なら内容省略、出力各64 KiB以内。先行例外とcleanup例外はsuppressedで保持する。削除失敗を成功に読み替えず、assertion・件数・子動作・資源数値は変更していない。

offline `jdbc,s1-contract,s1-web` test-compile SUCCESS（8.960秒）。既存3ファイルと通常Tooling JARのhashは従前値と一致。runtime jdbc・property falseで6 classを限定選択し、6 class conditionがすべてskip、active test0（Maven5.038秒）。@BeforeAllに到達しない条件を確認した。通常設定・POM・依存は変更していない。

`B1StopObservationTest-cleanup-correction/`へ前回失敗とは別に保管し、同じS01〜S08の8 invocationを訂正条件で1回実行。8 PASS／failure0／error0／skip0、class42.770秒、Maven45.332秒、開始14:00:33〜cleanup確認14:01:31 JSTの57.937秒。source stable true・cleanup true、container0・当該Java0・一時directory0。開始memory18,084,044 KiB／disk47,492,239,360 bytes、Docker29.5.3。既存Java2660／4684は変更なし。削除不能の診断保全経路は今回未発動であり、すべてのWindows保持条件への恒久保証にはしない。

S成立を受け、I01〜I08へ進む。現時点の累積管理目安は約76分／残約164分で、次classの15分＋cleanup5分を予約できる。新規90分・全240分と失敗停止条件は維持する。現在sourceでのT／P／E／R検証、回帰・最終package・Owner結果受入は未成立。


I01〜I08は8 PASS／failure0／error0／skip0、class43.080秒・Maven45.776秒。`B1ReadConnectionIT-first/`の開始14:02:01〜cleanup14:03:00 JSTは58.130秒、source stable／cleanupともtrue。通常子の生成publicationを厳密復元・一度固定し、採取中変更・保存拒否・SELECT readerを実接続で確認。I07の読取子は逐次終了し、同参照／digest保持と失効拒否を確認。I05は最後の読取後変更が保護されない既知窓の観測であり、操作肯定を追加していない。I08のDBは枝の途中では保持し、class終了時に削除。container0・当該Java0・一時directory0を確認した。

### 同一最終sourceの52件・選択PL2回帰・Tooling package

訂正後のT10／P8／E10／R8をそれぞれ1回実行し、S8／I8と合わせた新規52 invocationがfailure／error／skip0でPASS。6 runの`source-before.json`本文一致、各runのsource stable／cleanup trueを照合した。T／P／E／Rのclass秒は5.225／4.960／6.120／5.771、6 runの開始〜cleanup合計197.896秒。`final-52-summary.json`に対応を保管。前回S7 errorを消していない。

最終選択PL2回帰は既存4件＋明示選択IT2件だけ。4件はclass8.241秒／Maven10.855秒／cleanup込み23.778秒、2件はclass23.530秒／Maven26.098秒／cleanup込み38.298秒、全failure／error／skip0。起動子のheap768 MiB／pool2／idle0／接続待機10秒とDB制限を実効確認、container0・当該Java0・source stable true。6件のclass／method selectionをsource・XMLと対応付けた。

clean前のraw保全を実行中log終了前に試みたコピーはhash照合不成立と記録し、元rawを保持。終了・cleanup後の`tmp/b1-before-clean-verified-20261008/`はraw188ファイル／3,747,613 bytesについて元とコピーの全SHA-256一致。Reference／Toolingの直前XMLもproperties／captured output除去後に別保存した。失敗コピーも予算に含め、成立済みコピーの代替にはしない。

Tooling clean packageの初回はPowerShellの未引用dotted propertyが分割され、Mavenのgoal解析で停止（0.292秒、clean／test／package未実行）。初回logを保持し、同じ承認済み引数を引用した1回で`jdbc -Dmaven.test.skip=true clean package` SUCCESS（5.325秒）。testを再実行せず、B1専用fixture／SQLとsupport StarterのJAR混入0を照合した。新JAR hashとBOOT-INF/lib数は`tmp/b1-preflight-20261008/tooling-final-package.json`に保存。test実行時の旧JARと最終生成JARをhashで分離する。

新規・選択回帰・packageまでの管理目安は累積約79分／残約161分。Reference関連143件60分、既存99件＋E2E60分、cleanup予備を予約できる。ここからの既存Source不変とclass逐次cleanupを別台帳で確認する。結果受入・B-2／実運用／remoteは別判断。

## 21. Reference関連集合の条件不一致で停止（2026-10-08）

Reference関連143件の先行4 class／36件はPASS、各cleanup成立。Target10／Stop8／Provider8／Store10の開始〜cleanup秒は13.83／13.46／13.62／13.52。5番目`OperationalRecoveryEvidenceDbTest`は12件実行、failure11／error1／skip0、Maven27.156秒・cleanup込み39.13秒。失敗は`connect:44`からの既存Harness `open:148`でpool期待2／実値4。D09はSQLState53300（接続枠不足）とHibernate metadata取得失敗を記録。source・commandの静的照合により、Agentが既存99件向けpool4／idle1 command overrideを関連集合にも付けた誤りを確認した。保持接続の完全な因果特定は行わず、Harnessや業務assertionを修正しない。

後続95件・既存99件＋package済みE2E・Reference最終clean packageを停止。再実行・追加原因修正はしていない。失敗のsanitized XML／log／manifestは`tmp/b1-reference-verification-20261008/related/OperationalRecoveryEvidenceDbTest/`、有限診断6,405 bytesとDB枠不足の非秘密行は同run rootの`failure-diagnostics.txt`／`failure-observations.txt`に保管。captured output／propertiesを配布用XMLへ含めず、原XMLを残す。新規52件・選択PL2回帰6件のPASSは保持し、今回全回帰PASSとはしない。

source-hashes-before／afterでReference／E2E全source差分0。関連集合19 sampleのmemory最小17,589,616,640 bytes・disk最小47,274,180,608 bytes、DB同時最大1。終了時当該DB不存在を確認し、14:13:18 JSTの権限付きread-only診断でcontainer0／今回Javaなし、既存Java2660／4684と前日等からのChromeのみを記録。既存processを操作していない。

raw／コピー／candidate等を保守的に含めた`tmp/b1*`は確認時482ファイル／7,443,033 bytes、1 GiB以内。前回失敗・不成立コピー・成立済み保全を消していない。暫定累積約83分／残約157分として次集合予約時に再確認する。標準作業量は前回4時間目安＋作成・整理等の概算約1〜2時間、12時間進捗確認／28時間停止を維持し、Owner待ちと検証時間は分ける。

[開始票§27](../../development/phase4-s1-stage-b1-read-connection-limited-start-review-20261008.md#27-reference関連集合の実行条件訂正案2026-10-08owner確認待ち)に、関連集合のglobal pool overrideだけを除去して前回PASSの既存Harness条件へ戻す未実行candidateを提出した。失敗12件の訂正条件1回、成立時だけ残95件・既存99件＋E2Eへ進む案であり、今回採用したとは扱わない。結果受入・B-2／実運用／網羅性認定・remoteは未成立。

## 22. §27承認後の実行条件訂正・限定再検証（2026-10-08）

開始票§28へOwnerの対応案・限定再検証承認を記録した。HEAD `23379b352bfb9ef261789290a3acf40a6805c7e6`不変、Reference／E2E source hash差分0、B1最終52件の保存済みsource台帳と現sourceの差分0を確認。14:19:28 JST preflightはDocker29.5.3・container0、memory18,029,656 KiB／disk47,219,507,200 bytes、既存Java2660／4684だけ。暫定累積83分／残157分を引継ぎ、関連集合先行分120秒を保守的に予約済みとし、同集合60分・次class＋cleanup予約・全240分を管理する。Owner待ちを検証時間へ一括計上せず、過去枠をリセットしない。

変更は関連集合のcommandからglobal pool4／idle1 override2件を除いた実行条件だけ。Reference／Tooling code、POM、main、test、migration、assertion・件数の追加変更なし。失敗原XMLは訂正runで置換される前に、properties／captured output／connection情報を除去した`diagnostic-result.xml`とSHA-256台帳として前回失敗runへ保全した。前回12件失敗を消さず、訂正runと分離する。

失敗した同じ`OperationalRecoveryEvidenceDbTest`12件を訂正条件で1回検証し、12 PASS／failure0／error0／skip0、cleanup込み44.11秒。既存`NotificationFoundationDbHarness.open`内のpool最大2／idle0／接続待機10000 ms検査が通過した。DB／Ryukの不存在を確認し、残95件へ進む。rawは`tmp/b1-reference-verification-20261008/related-corrected/`へclass別に保管する。既存99件／E2Eのpool4／idle1は当該集合専用として維持し、関連集合へ供給しない。

関連集合の訂正後runは12 class／107件（訂正12＋未実行95）が全PASS、failure／error／skip0、class別開始〜cleanup合計356.20秒。先行4 class／36件のPASSと合わせて関連143件を成立とし、誤ったglobal overrideが付いた先行model testと訂正後DB Harnessのcommand条件をrun別に保持する。`related-143-summary.json`に36＋107とsource stable／cleanupを記録。関連143件の累積60分には先行run・失敗分を含み、訂正runだけへリセットしていない。

既存25 class／99件＋E2E開始前にsource差分0・container0・既存Javaだけを確認。Reference `target`外の`reference-reports-before-regression/`に当時の全XMLをproperties／captured output除去後に保全し、全関連runも`tmp/`へ保管した。既存回帰は当該集合専用pool4／idle1・heap768 MiB・fork1／逐次・実効資源検査を維持する。全240分の暫定83分へ訂正runの実測を加算し、旧source台帳fileの経過時刻（Owner待ちを含む）を予算消費の代用にしない。既存99件とclean package／E2Eは進行中で、完了結果を先行認定しない。

## 23. 限定検証の完了・Owner結果レビュー入力（2026-10-08）

**本節提出時：検証集合は完了、状態は `VERIFICATION COMPLETE / OWNER REVIEW PENDING`。後続のOwner受入は§24に記録。** §27の実行条件訂正・限定再検証は§28のOwner承認に従って実施し、追加code／assertion／件数の変更なし。実装結果の受入・B-2／実運用・正式受渡し・DoD／Phase 4全体・remoteの承認へ読み替えない。

| 集合 | 最終結果・対応 |
|---|---|
| B1新規6 class／52 invocation | 同一最終sourceで52 PASS、failure／error／skip0（§20）。旧S7 errorを保持。今回の§27訂正では再実行せず、保存済みsource hashと現sourceの一致を最終再確認 |
| 選択PL2回帰 | 既存4＋明示選択IT2＝6 PASS、failure／error／skip0（§20）。今回追加再実行なし。通常Tooling main／既存2 test／resource helperの追加変更なし |
| Reference関連集合 | 先行4 class／36 PASS＋訂正後12 class／107 PASS＝143 PASS（§22）。関連Harnessはpool2／idle0、既存99件向けoverrideを混在させない。旧12件失敗を別保管 |
| 既存Reference回帰 | 25 class／99 PASS、failure／error／skip0。Architecture2件は99件内で重複計数なし。期待class／件数を前回baselineから維持 |
| package済みReference E2E | 1 PASS、failure／error／skip0。最終clean packageした同じJARを実行。API／実Chromium／DB state・version・line total／Business・Security Audit／sanitized logの既存critical journeyを通過 |

既存99件＋E2Eのrunは`tmp/b1-reference-verification-20261008/regression/`へ保管し、Maven exit・新しいXML・期待件数・class別cleanupを照合した。全開始〜完了629.50秒（約10.49分、package／cleanup含む）、採用60分以内。Referenceのoffline `-pl koiki-reference-app -am -DskipTests clean package`はSUCCESS、49.849秒。E2Eはclass15.720秒／Maven18.415秒／cleanup込み30.35秒。child PID16476、heap805,306,368 bytes、pool最大4／minimum idle1、DB memory1 GiB／CPU1／max_connections16の実効値を検査した。browser／issuer／child／port／一時logの既存cleanup検査も通過した。

### source・artifact・非混入

branchは`feature/phase4-s1-reference-foundation`、基点HEAD `23379b352bfb9ef261789290a3acf40a6805c7e6`不変。Reference／E2E sourceのbefore／resume／before-regression／final hash差分0。B1の52件に対応する保存済みhash台帳と現sourceも差分0、成立済みraw保全188ファイルのコピーhashも最終一致。検証結果は未commitのB1 source／SQLとこの基点の対応として記録し、clean source commit後のremote検証とは扱わない。

| 最終package | SHA-256 | 非混入 |
|---|---|---|
| Tooling、jdbc・test compile／実行skipでclean package | `E72019D3CA6A019D22548CDDCF88EA848D181F15E6A71E31E7C6069FC9C16F3A` | B1／b1fixture／test SQL entry0、test-only support Starter entry0、BOOT-INF dependency JAR78。旧test実行時JAR hashと分けて保持 |
| Reference、E2Eが使用したclean package | `604412D21CB4C17F36E110EB9ED3D3FB71E586B7D4C8E2BF03E43F1CDA78CD6E` | B1／b1fixture／test SQL／Reference acceptance fixture・関連test class entry0、BOOT-INF dependency JAR116 |

`final-packages.json`、`source-hashes-final.json`、`b1-source-hashes-final.json`、`final-verification-summary.json`に対応を保存。Source／POM／main／既存test／migration／Root Reactor／CIへの追加変更なし。JAR hashの前後差を消さず、最終生成物のhashで識別する。通常無効・未接続拒否を維持し、B1 fixtureを正式artifactへ配布しない。

### 資源・raw・最終cleanup

関連訂正runは69 sample、memory最小17,213,272,064 bytes／disk最小46,998,626,304 bytes／DB同時最大1。既存99件＋E2Eは119 sample、memory最小17,224,224,768 bytes／disk最小46,962,057,216 bytes／DB同時最大1。各開始条件8／10 GiB以上を維持。heap／CPU／各timeoutは開始票の範囲であり、全OS memory・全外部I/Oの強制停止保証にはしない。

14:43:17 JSTの権限付きread-only最終確認はcontainer0、child16476不存在、Javaは既存2660／4684だけ。今回の回帰開始以後に起動したbrowser／headless shell／Node driverの残留0。既存Chrome／IDE processを操作していない。全当該resourceのcleanupは各test／run検査とこの最終診断を対応付け、台帳外processの不在や分散drainの保証へ拡張しない。

raw・コピー・診断・candidate等を保守的に含めた`tmp/b1*`は確認時634ファイル／7,736,488 bytes、1 GiB以内。過去のS cleanup失敗・Reference12件失敗・不成立コピー・成立済みコピーを保管し、成功runで上書きしていない。予算は前回の暫定83分＋今回関連run356.20秒＋既存／package／E2E629.50秒から、管理目安として累積約100分／残約140分。全240分、各集合枠を維持した。これは未計測区間を含む推定で、累積実測上限の認定ではない。標準作業量は概算約6〜7時間、12時間進捗確認／28時間停止に未到達。Owner待ちは別枠で保持する。

### 受入前に保持する限界・次の判断

52件は採用候補caseの実行結果であり網羅性の認定ではない。D11の歴史対象閉鎖・別publicationにまたがる論理通知の抑止と、D12／I05の最後の照合後変更の未保護を維持する。provider行なしは既定UNKNOWN、停止観測・FAILED・lockだけで再送／許可を肯定しない。R06のstatement取消とconnect／socket timeout設定は確認したが、実接続timeoutの強制終了・全I/O停止は未実証。ACL／digestのPASSをDB管理者・同OS利用者への真正性保証、TLS／本番credential・本番provider・分散fencing／backup／災害復旧へ拡張しない。

次はOwnerが実装結果・Evidence・残存リスクを受入判断する工程。B-2のReference Adapter／肯定Port登録・issue／consume／close、外部送信／復旧runner／Reference Level 2・Framework API／Rules／依存・CI／remoteは別review・個別開始判断を必要とする。Agentのgit add／commit／pushなし。local commitはOwner操作または操作前確認を維持し、未commit差分を無断reset／削除しない。

## 24. B1完了記録のOwner承認（2026-10-08）

Ownerは§23の完了記録を確認し「確認、承認いたします」と明示した。B1の承認済み範囲・方法における実装結果、検証集合の完遂、source／artifact対応、非混入、cleanup、失敗保全と記録を受入済みとし、状態を `COMPLETE / OWNER APPROVED` とする。§23のレビュー待ちは提出時の履歴として保持し、今回の受入で解消した。

この承認はTooling-owned B1限定検証の結果受入に限定する。D11／D12、網羅性懸念、実接続timeout強制終了の未実証、provider UNKNOWN、ローカルACL／digest／停止観測等の保証限界を維持する。B-2のReference Adapter／肯定Port登録・issue／consume／close、実外部送信／復旧runner／Reference Level 2、Framework API／Rules／依存、実運用／正式受渡し／DoD／Phase 4全体・remoteへの開始承認とは扱わない。

今回の反映は承認・現在状態の文書更新だけで、追加検証・code変更・git add／commit／pushは実施していない。sourceは未commit、実行script／rawは端末内tmp保管の状態を保持する。local commitはOwner操作または事前確認、別端末・長期再現のsource／手順／証拠の固定は別作業として残す。次の実装開始には対応する個別review・判断を適用する。
