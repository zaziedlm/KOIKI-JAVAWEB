# S1 B-1読取接続：source固定・preflight記録（2026-10-08）

**最新結果：canonical property供給へ訂正後、選択IT2件PASS（§15）。** 子起動時pool2／idle0／接続待機10秒、4子のVM flags・pool log、container／DB設定・接続sampleとcleanupを確認。以前の既存4件PASSと合わせて選択回帰6件のPASSを保持する。新規52件・Reference関連／既存回帰の今回B-1全実行・Owner結果受入は未成立。失敗履歴は保全した。

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
