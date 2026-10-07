# S1初回Reference基盤：端末preflight（2026-10-07）

**最新結果：§12のRepository／JPA Adapter・Spring管理transaction・現在認可／実Audit接続について、新規6 class／55件、変更後の既存25 class／99件とpackage済みReference E2E1件はすべてPASS。初回限定基盤の差分レビュー・local検証結果は2026-10-07にOwner承認済み（§13）。** §8は変更前baseline、§10・11は前段の結果。先行各節の未実施／未commit／停止記載は各確認時点の履歴として保持する。§10の変更はOwner操作で`a316a0d`へcommit済み、§11・12の変更25ファイルは未commit。正式運用scope／TTL・停止／provider証拠の接続は未成立。

## 1. 結論・source・承認範囲

**判定：検証専用設定の作成へ進むための端末・source・offline準備preflightは成立。** 設定作成後の実効pool／子JVM／Testcontainers制限、既存baseline、新規55件、通常無効構成・migration互換、正式受入は未実施である。本結果はそれらのPASSを意味しない。

- branch：`feature/phase4-s1-reference-foundation`を継続。今回のReference基盤用branchであり、新しいbranchは作成しなかった。
- 実行source：`f7e5427c83a3acdc8244e49d39444e097ef6d666`（引継ぎ文書追加）。承認文書commit `371f0245af0f1c4b58f93fbd6287961a502d9b12`を含む。
- 開始時／本Evidence追加前：`git status --short`は出力なし、`git diff --check`は成功。承認commitとの差分は引継ぎと関連導線の文書3件のみ。
- 端末：Windows 11 Pro／amd64、PowerShell 7.6.6。別端末再開として現物確認した。前端末のcache成立を転記していない。
- 根拠：[引継ぎ](../../development/phase4-s1-reference-foundation-next-session-handoff-20261006.md)、[初回実行資料§8〜9](../../development/phase4-s1-reference-foundation-execution-review-20261006.md#8-source固定preflight条件付き正式開始承認2026-10-06)、AGENTS.mdとproject-overview／business-feature-work正本Skill。
- Ownership：Reference保存基盤／非配布検証Tooling。今回のtracked変更は本Evidenceのみ。code／SQL／POM／Framework／CI／remote変更、download／installは行っていない。

## 2. 端末・資材の現物確認

| 面 | 結果・根拠 | 限界／次の確認 |
|---|---|---|
| JDK | Temurin 21.0.12.1+1。JAVA_HOMEとPATHのjavaはVS Code Java extension配下の同じJDK21 | build用JDK21。Java25互換検証を意味しない |
| Wrapper／Maven | tracked Wrapper 3.3.4、既存wrapper JARとMaven配布cacheを確認。`mvnw.cmd -o -version`は3.9.16で成功 | Wrapper再生成・取得なし |
| toolchain | user toolchains.xmlはなし。両validateで現JDK21が要求を満たし、外部toolchain不要と表示 | 未設定を不足と誤認しない |
| Maven解決先 | user settings.xmlなし、global settingsのlocalRepository指定なし、MAVEN_USER_HOMEと`.mvn/maven.config`の追加指定なし。既存`C:\Users\kataoka\.m2\repository`を使用 | 別checkout由来の同一SNAPSHOTを無条件に採用しない。Reactor対象は現sourceでbuild |
| BOM | sourceとcacheの`koiki-dependencies-bom:0.1.0-SNAPSHOT` POM SHA-256が一致（§5） | BOM-only追加install不要。全installed JARのsource一致を認定しない |
| JVM環境 | MAVEN_OPTS／JAVA_TOOL_OPTIONS／JDK_JAVA_OPTIONSは未設定。Maven実行区間だけ-Xmx768mを設定しfinallyで元へ復元 | test fork／子Referenceの制限は設定作成後に確認 |
| trust store | JDK cacertsの存在・hashのみ確認（§5） | offline確認なのでproxy／TLS疎通・credentialは検証していない |
| host資源 | 空きmemory18.4 GiB、Cドライブ空き48.66 GiB。採用済み8 GiB／10 GiB条件を満たす | 実検証前と各終了点で再測定。変動値を永続保証にしない |
| Docker | 権限付き`docker version`でClient／Server29.5.3、context default、Rancher Desktop WSL、linux/amd64。Engine memory16,771,682,304 bytes／CPU6 | sandboxのnamed pipe拒否はEngine障害ではなかった |
| image | 既存postgres:17-alpine（ID18cfe3ef5e68）とtestcontainers/ryuk:0.14.0（ID7c1a8a9a47c7）を確認 | pullなし。Testcontainersの実行・Ryuk cleanupはbaseline時に確認 |
| 既存資源 | postgres:15の既存container `f4dde4ab972f`／`80f8cead3230`を確認。5432使用中、5433／8080／8081のlistenは照会時なし | 既存containerは停止・変更しない。testはdynamic portで当該runのDB1個だけを管理 |
| browser | cached Playwright1.62.0のbrowsers.jsonはChromium／headless-shell revision1234。両Windows executableあり。実headless起動で151.0.7922.34、in-memory HTMLのtitle確認成功 | install・外部ページ操作なし。アプリE2Eではない |
| browser終了 | try-with-resourcesでcontext／browser／driverをclose。照会時、10:34以降開始のchrome-headless-shell／node残存0 | 全browser／nodeを一括停止していない |

## 3. offline確認・権限付き再実行

次のMaven操作はすべて`-o -B -ntp`。testの起動はしなかった。

| 操作 | 結果 |
|---|---|
| `-pl koiki-reference-app -am validate` | 13-project Reactor成功、Enforcer／JDK toolchain成立 |
| `-f build-support/reference-e2e-verification/pom.xml validate` | 成功 |
| `-pl koiki-reference-app -am -DskipTests test` | compile／testCompile／Surefire pluginの経路成功。testは全skip |
| `-f build-support/reference-e2e-verification/pom.xml -DskipTests test` | 成功、test全skip。incremental compileでありclean compile実証ではない |
| E2E POMの`org.apache.maven.plugins:maven-dependency-plugin:3.7.0:build-classpath` | cached pluginで成功。出力はE2E targetの`s1-preflight-20261007/playwright-classpath.txt` |
| `-pl koiki-reference-app -am -DskipTests package` | 13-project成功、33.302秒。Reference104 source／26 test sourceを実再compileし、ErrorProne／NullAwayとrepackage成立 |

packageはnon-cleanの資材確認であり、正式baseline JARとしない。追加install／publishなし。Surefire JUnit provider3.5.6の既存JARも確認したが、provider実起動・当該run XMLはbaselineで確認する。

### 権限エラーと解決

| 操作 | 通常sandboxの結果 | 承認手順後の結果 |
|---|---|---|
| Docker read-only診断 | `permission denied ... npipe:////./pipe/docker_engine` | 実行環境の権限付き承認後、同じ診断でServer・image・既存containerを確認 |
| OS資源照会 | Get-CimInstanceのアクセス拒否 | 権限付き承認後、memory／disk／listen portを確認 |
| Chromium smoke | 実行fileあり、起動時`spawn EPERM` | 権限付き承認後、同じsmoke成功・終了。PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD=1を区間限定し復元 |

環境権限変更や別cacheへの迂回は行っていない。DB preflightもS1の個別承認と実行環境の権限付き承認に基づく。権限手順は既存AGENTS.mdを使用し、新しい回避手順は追加しない。

preflight診断作成中に、PowerShellで未quoteの`-Dmdep.outputFile`が分割された操作はMaven失敗となった。引数全体をquoteして修正後成功。target内の一時BrowserPreflight.javaもexecutablePathのString→Path変換不足を修正してから実行した。これらを正常run／機能test件数へ加算しない。同じ失敗条件の無条件rerunはしていない。

### 隔離DBの作成権限・資源・cleanup

既存imageのみ（`--pull never`）、network none、memory1 GiB、CPU1、max_connections16で使い捨てDB1個を起動した。passwordは実行中に生成し、process環境経由で渡し、元の環境値をfinallyで復元。秘密値をcommand／Evidenceへ固定していない。

- 当該container ID：`22cccfa94049c4c00e2fc983bf512aaaeeefa7d9c8851f02abb9c65dabb10919`。
- pg_isready成立後、inspectでmemory=`1073741824`、nanoCpus=`1000000000`、network=`none`。
- DBの`SHOW max_connections`は`16`。
- finallyで検証中に作成した完全IDのみ削除し、当該IDの残存数0を確認。操作は正常終了。

これはDocker資源設定・DB起動・削除のpreflight。Reference接続・grant／Audit／migration・Testcontainers bridge／Ryukの成立を代替しない。

## 4. 固定したtest集合と次の実行条件

sourceの既存25 classを固定した。通常@Test94件＋parameterized5 invocation＝期待99件。E2EはPackagedReferenceCriticalJourneyTestの1件。実数・skip・XMLはbaselineで照合する。

| class | 期待invocation |
|---|---:|
| ReferenceArchitectureTest | 2 |
| ExpenseAuditRollbackPostgreSqlIntegrationTest | 1 |
| ExpensePostgreSqlIntegrationTest | 6 |
| ExpenseReadModelPostgreSqlIntegrationTest | 5 |
| ExpenseApiControllerTest | 6 |
| ExpenseApiPostgreSqlIntegrationTest | 3 |
| ExpenseApplicationServiceMethodSecurityTest | 3 |
| ExpenseReadServiceMethodSecurityTest | 1 |
| ExpenseRequestTest | 4 |
| IdentityManagementControllerTest | 9 |
| IdentityManagementExceptionHandlerTest | 5 |
| IdentityUrlSecurityTest | 8 |
| IdentityUserManagementMethodSecurityTest | 6 |
| IdentityUserManagementTest | 3 |
| ReferenceApiJwtDecoderTest | 1 |
| ReferenceJwtAuthenticationConverterTest | 3 |
| ReferenceSecurityConfigurationTest | 1 |
| DepartmentDeactivationPostgreSqlIntegrationTest | 2 |
| MasterPostgreSqlIntegrationTest | 2 |
| MasterReadModelPostgreSqlIntegrationTest | 1 |
| JpaMasterAvailabilityQueryTest | 1 |
| MasterAdministrationMethodSecurityTest | 1 |
| MasterAdministrationTest | 4 |
| MasterCatalogQueryMethodSecurityTest | 1 |
| ReferenceBusinessUrlSecurityTest | 20 |

新規は実行資料§2のP12／C3／T16／D12／R7／M5＝6 class／55件。まだ作成していない。基盤追加前のbaselineと変更後は、既存25 class＋E2Eの同じ集合・条件を使う。

各classの採用済み呼出しは以下。$referenceTestClassは上表の1要素とし、任意縮小しない。

```powershell
.\mvnw.cmd -o -pl koiki-reference-app -am "-Dtest=$referenceTestClass" "-Dsurefire.failIfNoSpecifiedTests=false" "-DforkCount=1" "-DreuseForks=false" "-DargLine=-Xmx768m" "-Djunit.jupiter.execution.parallel.enabled=false" "-Dkoiki.reference.verification.resource-limits.enabled=true" "-Dspring.datasource.hikari.maximum-pool-size=4" "-Dspring.datasource.hikari.minimum-idle=1" test
```

制限専用設定はまだ存在しない。現sourceのReference DB fixtureは資源制限なし、E2E子Referenceはheap指定なし。この引数を今実行して実効制限済みと扱わない。次の順に進む。

1. 承認済みtest fixture／E2E／READMEのみに明示選択式設定を追加する。production properties／Parent／BOM／POM／CIは変更しない。
2. 未設定／falseの従来動作、有効時DB・pool・子JVMの実効値、当該run識別情報と終了確認を検証する。不成立なら停止する。
3. Owner操作または明示指示に基づき検証用変更sourceを固定し、既存baselineを取得する。clean package前に必要な既存証拠を保全する。
4. baselineのXML時刻・期待件数・exit code、JAR hash・sanitized log・資源・cleanupを記録して初回基盤へ進む。

run証拠はReference／E2E各targetのrun別directoryへ保存し、classごとの終了・DB停止確認後だけ次へ進む。baseline／変更後各60分、raw合計1 GiB、同原因・条件のrerun最大1回を適用する。新規DB class10分／SQL・lock・transaction各10秒、DB同時1、Maven／test／子Reference heap各768 MiB、既存pool4／idle1、新規用途別pool各2／idle0＋管理2（合計8以内）は設定作成後に実証する。

秘密は実行時生成・process環境／memoryに限定し、command line・XML・log／rawへ出力しない。Security記録失敗を安全扱いしない。無関係container／processの一括停止を行わない。

## 5. hash・記録保管と未達

| 対象 | SHA-256 |
|---|---|
| Root POM | AD482D632A04F52ED28981A7B3766EDF09B91B46F016EB1BF9F550A8DAE88F7B |
| Reference POM | 62B1948B1ADFA76AD6A6FA0646C8911B3C43FBBA2D5A174404F92E63CB247E05 |
| E2E POM | B54CD9AC333F1B1F6254EE8DDC9E88DA359BBF0057CE1506021F49C05B27E85E |
| source／cache一致BOM POM | 6CB0E6738694056E0A026340A438A31F6A69EE2178DA867C5F21E1933FD5E031 |
| preflight non-clean Reference JAR | E656E4595A4AA7740FF99F9FF41D943DC7884E21E690BAF702B20D92CD003A52 |
| global Maven settings | 2F552325AEDD9A9C9268DF520BE22CFE3AA86260B76A0F830D2796D2F0E22BD4 |
| JDK cacerts | 98D34A90FCA2688EF5674862D2A484E09E5CA25D2F07C5D654AA28F1581F3419 |

一時browser診断とclasspathは`build-support/reference-e2e-verification/target/s1-preflight-20261007/`に保存（確認時2 file／5,651 bytes）。正式test／配布物へ含めない。本Evidenceを継続記録とし、古いSurefire XMLを今回のtest実績へ転記しない。

実行時刻の確認点：offline validate 10:32:49、classpath成功10:33:56、Reference package終了10:35:35（すべて2026-10-07 JST）。実作業・環境承認待ちを含む経過時間から24〜40標準時間の消費量は推定していない。標準作業量の記録は設定作成／baseline／登録・migrationの区切りで更新する。

**未達：** 検証専用設定の実効確認、既存99件＋E2E baseline、新規55件、登録／通常無効・新旧DB互換、実grant／Query／Recorder／transaction、Owner受入。Phase 4全体開始、通知／復旧／Level2接続、Framework API／Rules／依存、正式運用scope／TTL・provider／実trace、remoteは対象外を維持する。

## 6. 検証専用設定の作成・実効確認（後続作業、2026-10-07）

§1〜5はpreflight時点の記録。後続指示によりReference DB fixture／独立E2E／READMEへ明示選択式設定を作成した。production／POM／Framework／CIは変更していない。

- Reference／E2E両方で未設定／falseは無効、trueのみ有効、不正値拒否をtarget内の一時診断で実確認。Reference未設定／falseのDB commandは元のPostgreSQLContainerと一致。pool引数不足はDB起動前に拒否。
- Referenceは有効時にtest JVM heap768 MiBとpool指定4／1を先に要求し、実container inspect・DB SHOW・HikariDataSourceで実効値を照合する。
- MasterPostgreSqlIntegrationTestの2件が成功（設定確認用、正式baselineへの転記なし）。memory1073741824／nanoCpus1000000000／max_connections16／test heap805306368、実pool4／idle1を確認。DB `0b31271be658b7dd376caf83bc63a8272f77ee5b480cace1280c2f9570c7be3f`の削除を確認。
- E2Eはchild起動引数-Xmx768mとVM診断、子環境pool4／idle1、Hikari packageの検証限定DEBUGから実効値を照合する。既存のBearer／Session／DB／Audit／秘密非出力assertionを維持。
- E2E初回設定確認はheap成立／pool診断欠落で失敗。class loggerの環境変数がcaseを保持できない指定だったため、package logger指定へ修正。失敗XMLはsystem-properties／out／errを除き`target/s1-settings-check/failed-pool-log/result.xml`へ保存した。assertion削除・上限緩和・同条件の無条件rerunはしていない。
- 修正後E2E1件成功、15.131秒。DB `535c952fdd9290bccf56edbc7f3aa56c350535b1db484f2c8a386cd30bf42d59`はmemory1 GiB／CPU1／max_connections16、child PID23304はheap768 MiB、実pool4／idle1。DB削除とchild残存なしを外側でも確認した。初回失敗DB `2cea46de54ad9a3498caa633117935e172c1cf3ecb3f571d8fb9983a0fc9b35b`も削除済み。
- E2E既存の秘密非出力検査を通った子logからheap／pool診断行のみ`target/s1-resource-limits/sanitized-process.log`へ保管し、元logは削除。秘密原文をrawへ保存しない。
- 11:30:19 JSTの確認で空きmemory18.62 GiB／disk48.14 GiB、既存postgres:15の2 containerだけが残存。

設定確認用の2件＋E2E1件を既存baseline成功へ加算しない。次は検証用変更をlocal commitで固定し、25 class／99 invocationとE2E1件を同じ条件で取得する。通知の業務code／DDL追加・新規55件は未開始。baseline結果は次節に追記する。

## 7. baseline初回停止とclean source再固定

設定確認済み変更を`d6b014e26653e711d6d7d046bb8b00720f12adac`へlocal commitし、clean worktreeからbaselineを開始した。最初のReferenceArchitectureTestは2件中1件が失敗し、後続を停止した。

違反15件は旧package `org.koikifw.reference.web.ReferenceBusinessUrlSecurityTest`が業務moduleとしてimportされた内容。現sourceは`org.koikifw.referenceacceptance.web`であり、target/test-classesには移動前後の両classが残存していた。non-cleanの既存出力が原因で、Rules／test assertion／production sourceを変更せず、必要証拠を保全してclean buildへ切り替えた。失敗XML・manifest・資源記録はE2E targetの`s1-baseline-20261007/`へ保持している。

clean buildでは追加fixtureの`command.getHostConfig()`へのNullable参照をNullAwayが拒否した。IDE既存compile出力による先の実行だけではMaven再compile適合を保証できていなかった。Reference／E2EともObjects.requireNonNullで欠落を明示的に拒否する修正を追加した。cleanで旧出力を除去した後のMaven packageは9.634秒で成功し、Reference26 test sourceを実再compileしている。NullAway除外・Rules緩和・失敗assertionの削除はしていない。

初回停止の結果を上書きせず、修正をlocal commitで再固定してから、別directory `s1-baseline-20261007-clean/`へ25 class＋E2Eを最初から取得する。初回ArchUnit失敗のtest2件を成功baselineへ加算しない。原因・条件を変えた再取得であり、同条件の無条件反復はしない。

## 8. 既存25 class＋E2E baseline結果（2026-10-07）

**PASS：既存25 class／99 invocation＋PackagedReferenceCriticalJourneyTest 1件。失敗0／error0／skip0。** §4の全class・期待件数と一致。新規通知testは追加していない。

| 項目 | 結果 |
|---|---|
| 実行source | `884c2b83b521e4a29b412ccbd03046627c2ed5fc`、開始・続行前clean worktree |
| 方法 | class別Maven／fork1／reuseForks=false／並列無効／heap768 MiB、DB停止後に次class |
| 実効DB | 各DB class・E2EのS1_RESOURCEでmemory1073741824／nanoCpus1000000000／max_connections16を確認 |
| 実効pool | 各Reference DB contextとE2E childで最大4／idle1を確認 |
| E2E child | PID7964、heap805306368。process終了・port解放・issuer／browser終了・temp log削除の既存assertion成功 |
| clean JAR SHA-256 | `054028B8511F72C8E5E85E61A47B22F7FD23CF5ABBDD9FC64DFE602463FFA592` |
| 比較区間 | 2026-10-07 11:42:40〜11:54:42 JST、722.08秒（約12分）。開始時刻は最初のresource sampleより前の保守的な記録時刻 |
| 時間上限 | 続行時も元の開始時刻を保持。調査・環境承認待ち・clean packageを含む区間で60分以内 |
| 観測資源 | 最小available memory17.03 GiB／disk47.89 GiB、DB同時数最大1。5秒間隔を基本に観測（Docker stats所要時間を加算） |
| 記録量 | preflight・設定確認・途中停止を含むE2E targetのs1-*合計1,512,798 bytes（集計時）。1 GiB以内 |
| 残存 | classごとの当該DB削除・Testcontainers補助container終了、E2E child終了を確認。最終当該DB `3b7a160a31e2b74e645f070d213a812e3dd78f02008a202ff72753ac8ce19894`も残存なし |

### cleanup待機による一時停止と続行

clean sourceでArchitecture2件とExpenseAuditRollbackPostgreSqlIntegrationTest1件が成功した後、実行用scriptの10秒待機ではRyukの終了を確認できず停止した。業務DBは削除済みであり、11:44:20 JSTのread-only再確認では補助containerも自動終了していた。test失敗や残存無視で続行したものではない。

補助containerの待機は最大60秒へ補正し、残存0確認は維持。phase全体の60分上限は元の開始時刻から計測した。同じsource・同じheap／DB／pool／test集合で成功済み2 classのXMLとcleanup確認を保持し、残り23 classとE2Eを続行した。既存testのrerunは行わず、確認済みの全25 classを一つのmanifestへ照合した。Audit classのMaven時間は24.211秒、後続の終了確認時刻を別記し、未測定の外側経過秒数はnullとした。

### 保存先・終了時確認

- 統合manifest：`build-support/reference-e2e-verification/target/s1-baseline-20261007-clean-cleanup/baseline-manifest.json`。26結果行／合計100件、class別期待件数・exit code・cleanupを照合済み。
- 同directoryのclass別`result.xml`／`manifest.json`／`sanitized-maven.log`と`sanitized-process.log`を保存。XMLのsystem-properties／out／errと失敗payloadを除去し、環境・秘密をrawへ転記しない。
- 初回停止は`s1-baseline-20261007/`、clean後の途中停止は`s1-baseline-20261007-clean/`へ保持。成功結果で失敗記録を上書きしていない。
- resource-samples.jsonlは途中停止側と続行側の両方を保存。最終確認11:57:05 JSTの空きmemory18.27 GiB／disk47.95 GiB、E2E child残存なし。
- 開始時にrunningだった既存postgres:15の2 containerは最終確認でexited（削除なし）となっていた。inspectの終了時刻は`f4dde4ab972f`が11:45:02 JST、`80f8cead3230`が11:45:08 JST。本作業のcommandに両IDの停止・削除はなく、停止主体・理由は未確認。自動再起動は行わない。資源観測のhost環境変化として記録する。

**今回の出口は完了：検証専用設定の作成・実効制限の確認・既存baseline取得。** 次は承認済み順序に従う通常無効構成／登録・Reference-owned migration互換の最小確認から初回保存・認可・Audit基盤へ進む。新規6 class／55件、通常無効の新旧DB保護、初回基盤のOwner受入は未実施。Phase 4全体開始／非同期通知・復旧の接続／DoD／正式受渡し・remoteの判定へ拡張しない。

## 9. 既存container停止のOwner確認とcommit境界

§8の既存postgres:15の2 containerについて、Ownerは「検証を妨げないタイミングで停止いたしました」と確認した。停止主体・理由は確認済み。§8の未確認記載は当時の照会結果として保持し、baseline PASS／当該run cleanup成立の判定に変更はない。

以降のlocal commitはOwnerへの事前確認またはOwner自身の操作とする指示を受領した。remote pushは現時点で不要。§10の作成・検証ではgit add／commit／pushを行っていない。

## 10. 通常無効構成・登録・migration互換の最小確認

**PASS：M01〜M05の5件、R01〜R03の3件、既存ReferenceArchitectureTestの2件。失敗0／error0／skip0。** 初回基盤全体・登録7件全体・新規55件の受入ではない。

### 作成した最小範囲

- Reference-owned notificationのRICH／JPA／SHARED metadata、RecoveryPermit／RecoveryConsumptionのJPA field mapping。
- 専用NotificationFoundationConfiguration。未設定／falseで登録不在、trueだけでnotification modelのEntityScanを追加。不正値は診断して起動拒否。既存EntityScan、root Repository scan、Security、通常propertiesは変更していない。
- 別location `db/migration/kkref-notification/V4__create_notification_recovery_records.sql`。採用済み2 table・必須値／期限／閉鎖一式制約・未閉鎖target partial unique・operation unique・自module内FKを追加。V1〜V3、Framework migration、POM／依存／Rules／CIは変更していない。
- この段階のmodelはmappingのみで、操作を受け付けるfactory／更新method／Application／Repository／Adapterは未作成。Domain規則や用途別grant・実Query／Recorder接続は次段階。operational entrypointを設けず、testの有限UUID／TTL等を運用値へ昇格していない。

### 検証結果

| ID／class | 今回確認した内容 | 結果 |
|---|---|---|
| M01 | freshのFramework二階層→Reference V1〜V4、両table・partial unique／FK、既存IdentityUserEntity／DepartmentEntity／ExpenseRequestと追加2 Entityの登録、Hibernate schema validate | PASS |
| M02 | V1〜V3から管理側の両location適用。既存department rowとV1〜V3／baseline履歴の全列が不変 | PASS |
| M03 | test限定の失敗SQLでV4 transactionをrollback。途中tableが残らず、既存department rowと履歴が不変。既存SQL／履歴の削除・validation無効化なし | PASS |
| M04 | 通常Web起動・未設定、V4未適用。追加Entity／専用Configuration Bean不在、既存3代表Entity保持、通常Flyway validation成功、追加table不在 | PASS |
| M05 | 管理適用後に通常Web起動・false・既存locationのみ。validation成功、追加Entity／Bean不在、V4履歴・両table・permit／consumption test記録を保持 | PASS |
| R01〜R03 | 未設定／false時の登録不在と既存代表Entity保持、不正値による起動拒否 | 3／3 PASS |
| ReferenceArchitectureTest | businessModuleRules＋frameworkOwnershipRules。Rules除外・Framework内部import追加なし | 2／2 PASS |

通常locationでのV4 validationは**現行のFlyway既定`*:future`**で成立した。ignore pattern、validate-on-migrate、既存locationを変更していない。保証対象は現行V1〜V3＋別location V4であり、通常locationに後続versionを追加する際や旧JAR互換は別途再検証する。

M01の有効時登録確認は現段階のmappingに限定する。必要なService／Adapter／Repository全体の登録・未接続拒否（R04〜R07）や認可・Audit・grantの成立を認定しない。既存25 class＋E2Eの変更後全回帰は初回基盤が揃った段階でbaselineと同じ条件で行う。

### source・環境・記録・cleanup

- branch `feature/phase4-s1-reference-foundation`、基点HEAD `1311fbfd8c471d0488427274e60fc81d7b91bb7c`＋未commitの新規7 file。本Evidence追記を加えた作業差分は8 file。
- offline clean test-compileは108 production source／28 test sourceを再compileし45.642秒で成功。前段incremental compileも成功したが、clean実証と区別する。
- 初回実行のM5／R3は成功したが、Architectureで旧package `org.koikifw.reference.web.ReferenceBusinessUrlSecurityTest`の残存classが再検出され停止。旧class更新時刻13:11:23、現packageのclass更新時刻13:13:26 JSTを確認。生成主体は未確定。source／Rules変更による回避はせず、失敗記録を保全しclean build後に別runで再確認した。
- 再確認は既存row・追加記録保持のassertionを補強した変更後の条件。最終M5／R3／Architecture2を採用し、前runの結果を加算しない。
- DB同時1、memory1 GiB／CPU1／max_connections16、test heap768 MiB、既存startup pool4／idle1。管理接続は逐次1本とFlywayの管理処理のみで、用途別runtime grantの証拠ではない。新規DBのlock／statement／transaction timeout各10秒もDB SHOWで確認。
- 最終classの外側実時間はM5＝40.97秒、R3＝36.22秒、Architecture2＝15.60秒。DB class各10分以内。5秒間隔の観測でavailable memory最小15.20 GiB／disk44.18 GiB、DB最大1、資源違反なし。標準作業量の消費をこれらの実時間から算定しない。
- 新規DB ID `5697e56497fabb27a0d0647b71e8b39285c0d8531efbc806d167c2468ab07c8d`／`4363c36d862e3938818b6328b5e5ba1c478e2b6c329f7ea65a51815853d638cf`の削除と補助container残存0を各class後に確認。Web context／poolもtry-with-resourcesで終了。失敗SQLの一時file／directoryは当該pathだけを削除。
- 証拠は`build-support/reference-e2e-verification/target/s1-initial-check-20261007-clean/`のresults.json、source-hashes.json（新規7 file）、class別sanitized XML／log／manifest、resources.jsonl。初回停止は`s1-initial-check-20261007/`に保持。XMLの環境・captured output・失敗payloadを除去し、秘密原文をrawへ保存しない。Maven heap環境値はfinallyで復元。

**次の作業：** この最小mappingを採用済みのDomain生成・期限／対象／閉鎖規則へ完成させ、限定Repository／Adapter・Spring管理transaction・Public IdentityQuery／Recorder・用途別grantへ接続する。全55件・変更後25 class＋E2E・Owner受入は残る。commitは事前確認またはOwner操作を待つ。

## 11. Domain生成・期限・対象照合・閉鎖規則（2026-10-07）

**判定：採用済みP01〜P12／C01〜C03のDomain15件と既存Architecture2件はPASS。** sourceはOwner操作でcommit済みの`a316a0d00b356895fd4ec87fe0976927bb2c45f8`＋本節の未commit差分。開始時のbranchは`feature/phase4-s1-reference-foundation`、作業ツリーcleanを確認。今回の変更はReference notificationのRICH／JPA SHARED Domain2型、Domain test2 class、本Evidenceの計5ファイル。Maven依存・Framework／Rules・Security・登録構成・V4は変更していない。git add／commit／pushは実行していない。

### 11.1 実装と保証範囲

- `RecoveryPermit.issue`で必須permit／actor／target／reason／時刻、非空文字列、column長、attempt非負を検証。発行時刻・期限をmicrosecond精度へ切り捨て、調整後の`expiresAt <= issuedAt`を拒否する。Clockと採用TTL policyからの時刻生成は後続Application責務であり、正式TTL値を追加していない。
- `requireConsumable`でenvironment／publication／event／listener／expected attemptの全一致、未閉鎖、`now < expiresAt`を確認。判定時刻は丸めず、期限一致／経過を拒否する。判定自体は消費記録・送信・認可を実行せず、期限切れで自動閉鎖しない。
- `close`はclosedAt／confirmedBy／resultRefをすべて検証してから一式を更新。不足・非空／長さ違反で部分更新せず、再閉鎖による証拠書換えを拒否する。期限経過後の人の解決記録は可能。主体の真正性・停止／突合証拠の信頼性は後続Application責務であり、Domainの一式整合を配信成功と同一視しない。reason／resultは現段階では必須／非空／長さを検証し、信頼できるcode／opaque参照の供給は後続で確認する。
- `RecoveryConsumption.record`でpermit／operation／worker世代／消費時刻を必須とし、worker非空／column長とmicrosecond精度を確認。公開操作は生成と値の取得のみで、更新・削除methodなし。append-onlyのSQL権限制御・一意性・FK・commitは後続の制限付き実DB検証で確認する。
- 不変列の`updatable=false`とpermitの`@DynamicUpdate`／`@Version`を維持。JPA保存／再読取後の期限境界は採用済みD03に残し、今回のメモリ上Domain testだけで実DB保証を認定しない。

### 11.2 method対応・結果

method名は[初回実行資料§2.1・2.2](../../development/phase4-s1-reference-foundation-execution-review-20261006.md#21-recoverypermittest12)の採用案と一致し、通常Test各1 invocationとして作成。項目別assertionをinvocation数へ数え替えていない。

| class／対応 | 実施 | 結果 | class外側時間 |
|---|---|---|---|
| RecoveryPermitTest／P01〜P12 | 期限直前／一致／直後、対象差異5、閉鎖済み、生成必須値、理由・期間、閉鎖一式／再閉鎖 | 12件、failure／error／skip 0 | 12.44秒 |
| RecoveryConsumptionTest／C01〜C03 | operation／worker／permit欠落拒否。各method内で正常生成・値保持・時刻／長さも確認 | 3件、failure／error／skip 0 | 12.12秒 |
| ReferenceArchitectureTest | businessModuleRules／frameworkOwnershipRulesの既存2入口 | 2件、failure／error／skip 0 | 16.51秒 |

clean `test-compile`はoffline／`-pl koiki-reference-app -am -DskipTests`で成功（50.663秒、Reference main108／test30 source、NullAwayを含む）。各classは別Maven呼出し、fork1／reuseForks=false、Maven・test heap768 MiB、JUnit並列無効で逐次実行した。

```powershell
# class名をRecoveryPermitTest／RecoveryConsumptionTest／ReferenceArchitectureTestへ順次指定
# 各呼出し区間だけMAVEN_OPTS=-Xmx768m、終了時に元値を復元
.\mvnw.cmd -o -B -ntp -pl koiki-reference-app -am "-Dtest=RecoveryPermitTest" "-Dsurefire.failIfNoSpecifiedTests=false" "-DforkCount=1" "-DreuseForks=false" "-DargLine=-Xmx768m" "-Djunit.jupiter.execution.parallel.enabled=false" test
```

### 11.3 実行環境・証拠・残作業

最初の通常sandbox実行は資源取得で「アクセスは拒否されました」となり、test開始前に終了。同じ検証scriptを環境の権限付き承認手順で再実行し15＋2件は成功したが、前回終了時の停止印が残り、継続資源monitorの記録が欠落していた。成功XMLを保全し、既存結果を上書きしない新規run directoryを必須とするscriptへ修正。資源記録の存在も必須確認として、別runで同じ15＋2件を再実行した。これは監視証拠不足の修正であり、test期待値・拒否枝・Rulesを変更した再実行ではない。

最終runは5秒間隔9 sample、使用可能memory最小16.89 GiB、disk最小44.03 GiBで8 GiB／10 GiB条件を満たした。対象classはDB／Docker／browser／serverを使用せず、各Maven／test JVM終了後に次のclassを開始、当該monitor jobもfinallyで終了。各classは10分以内。今回終了時の既存`target/s1-*` raw総量は2,105,611 bytesで1 GiB未満。

rawは`build-support/reference-e2e-verification/target/s1-domain-check-20261007-resourced/`。更新時刻を照合したclass別sanitized XML／Maven log／manifest、`results.json`、`resources.jsonl`、Domain4ファイルのSHA-256を記録した`source-hashes.json`を保全。初回結果は`target/s1-domain-check-20261007/`へ別保存し、検証専用runnerも同directoryに置いた。XMLのsystem properties／stdout／stderrはarchiveから除去し、環境値全体や秘密をEvidenceへ出力していない。

**残作業：** 限定Repository／Adapter・Spring管理transaction・Public IdentityQuery／Recorder・用途別grant、実DB精度／lock／競合／Audit rollback、R04〜R07を完成させる。ここまでの新規実施は段階別に23／55件（Domain15＋先行migration5／登録3）であり、全55件の同一完成実装での受入、変更後既存25 class＋E2E、Owner受入は未成立。正式TTL／scope供給元・真正な停止／provider証拠の運用接続と通知／復旧は後段境界を維持する。今回5ファイルは未commitで、commitは事前確認またはOwner操作に従う。

## 12. Repository／Adapter・transaction・認可／Auditの実接続（2026-10-07）

**判定：新規6 class／55 invocation、既存25 class／99件、package済みReference E2E1件は同じ最終sourceでPASS。** sourceは`a316a0d00b356895fd4ec87fe0976927bb2c45f8`＋§11・12の未commit差分。§11からDomain2型／test2 class／Evidenceの差分を継続し、今回の接続・test・隔離grant setupと合わせて計25ファイル。codeはReference notification、有限port／Clock・grantと実DBharnessはtest所有。POM／依存・Framework API／Rules／SQL・通常Security／properties・既存V1〜V3は変更していない。Agentによるgit add／commit／pushは実行していない。

### 12.1 保存・認可・Auditの実装

- Domain Repository2契約はSpring Data Commons `Repository`を継承し、`@NoRepositoryBean`でroot自動登録を抑止。必要なscope付き取得／row lock、INSERT／flush、version前進、消費参照だけを公開し、DELETE／TRUNCATE／消費UPDATEを追加しない。
- 明示登録のJPA AdapterがEntityManagerを使用。permit取得はpermit ID＋environment＋publicationをJPQL／SQLの条件に含め、全件取得後filterで代替しない。消費と閉鎖は同じpermit rowを最初に`PESSIMISTIC_WRITE`で取得し、消費は`PESSIMISTIC_FORCE_INCREMENT`でversionだけを更新する。JPA例外はSpring標準の例外変換を使い、Application外へは対象・DB競合の詳細を含まない拒否／HOLDとする。
- 採用済みINSERT列権限に合わせ、permit閉鎖3列とversionを`insertable=false`に調整。初期NULL／version0はDB既定とHibernateの初期versionで成立し、閉鎖は既存`@DynamicUpdate`とversion条件付きUPDATEで閉鎖列／versionだけを更新する。不変列の`updatable=false`を維持し、用途別権限を拡大して成立させていない。
- `RecoveryPermitService`はSpring管理`TransactionTemplate`（timeout10秒）で発行・消費・閉鎖・観察を調整。保存／flushと実Business Recorderは同じJpaTransactionManager・DB・commitへ参加。拒否の実Security Recorderは保存transactionを終了してから呼び、記録失敗でも拒否を維持する。callerの既存transactionを持ち込む操作は拒否し、caller-held lockを残したままSecurity別transactionへ進まない。
- 主体はSecurityContextの認証済みPublic `FrameworkPrincipal`から取得し、Public IdentityQueryへAdapter経由で現在照会。presented principalのpermission snapshotを信用せず、ACTIVE・現在のISSUE／READ／EXECUTE／CLOSE能力とReference-owned scopeを、保存／permit取得より先に確認する。消費の初回境界は現在主体が保存済み発行者と一致する場合に限定し、worker認証／委譲受付は追加しない。確認済み主体だけをUSER、不存在・照会不能等の未確認主体をANONYMOUSとして拒否Auditへ対応付ける。
- Business code／action／actor／resource、Security拒否／確認不能分類は採用判断表D5に対応。発行=`PERMIT_ISSUED`、消費=`PERMIT_CONSUMED`、閉鎖=`PERMIT_CLOSED`は消費確定／人の解決記録の意味に限定し、配信成功を意味しない。scope外／未確認対象のSecurity記録にはresourceを付けない。技術競合・期限切れ・証拠不足を成功Auditや一律Security侵害へ変換せず、自動再送・消費削除・自動閉鎖を行わない。
- scope／TTL policy／現在target／運用証拠の必要portだけを作成し、production既定は確認不能／empty。肯定側はtest所有の有限scope、可変UTC Clock、有限TTL／snapshot／証拠recordに限定した。証拠はpermit／5項目target／operation・worker世代または確認主体・result参照と照合し、callerの任意booleanで確認済みにしない。正式TTL値／上限、scope割当、真正な停止／provider突合Adapterは未接続のまま必要操作を拒否する。
- 構成有効時だけRepository／Adapter／Service／Entityを登録。通常未設定／falseでは不在。観察はApplication-owned最終recordへtransaction内でmaterializeし、Entityを外へ返さない。Web／CLI・sender／listener／runner／publication schema／Modulith runtimeは追加していない。

### 12.2 隔離grant・実DBharness

新規testのharnessは`org.koikifw.referenceacceptance.notification`へ配置し、正式Framework／Reference操作受付へ含めない。隔離setupは実Framework二階層FlywayとReference V1〜V4を管理接続で適用。その後のruntime testではFlyway実行をtest設定で停止し、Hibernate schema validationは維持した。通常ReferenceのFlyway設定／validationは変更せず、Servletの通常起動とM01〜M05を別に再実証している。

test resource `notification/s1-isolated-grants.sql`を管理setupだけで実行。NOLOGIN／NOINHERIT notification ownerとNOINHERIT permit／consumer／readerを作成し、passwordは実行中に生成。owner membershipなし、superuser／createdb／createroleなしをDB照会で確認。grantは採用表の2 table／列と、Public IdentityQueryの現行5 read table SELECT、Public RecorderのAudit INSERTだけ。Audit SELECT・password／login-attempt／Session参照・schema CREATE・permit削除／TRUNCATE・消費変更は禁止を維持。ALL TABLES／default privilegesを使用せず、正式運用credential／role配備へ昇格していない。

各用途は同じDBへの別DataSource／JPA context、pool2／minimum idle0／取得待ち10秒。最大3 poolの6接続＋管理最大2で8以内。直接の権限拒否確認はruntime接続1本ずつを閉じて進め、同時競合worker2と同時管理接続の予算を越えない。管理接続はDDL／test seed・許可の一時REVOKE／証拠突合／test cleanupだけに用い、業務成功をowner接続で代替しない。実IdentityQuery／Business・Security Recorder／JPA／transactionはmockに置換していない。

### 12.3 method対応と最終結果

T01〜T16／D01〜D12／R04〜R07を[初回実行資料§2](../../development/phase4-s1-reference-foundation-execution-review-20261006.md#2-新規6-class55-invocationのmethod対応案)と同じmethod名／通常Test各1 invocationで作成。R01〜R03、M01〜M05、P01〜P12／C01〜C03も同じ最終実装で再実行した。

| class | 件数 | failure／error／skip | class外側時間 |
|---|---|---|---|
| RecoveryPermitTest | 12 | 0／0／0 | 14.00秒 |
| RecoveryConsumptionTest | 3 | 0／0／0 | 13.45秒 |
| NotificationFoundationTransactionTest | 16 | 0／0／0 | 38.60秒 |
| NotificationFoundationPersistenceTest | 12 | 0／0／0 | 49.28秒 |
| NotificationFoundationRegistrationTest | 7 | 0／0／0 | 43.69秒 |
| NotificationFoundationMigrationTest | 5 | 0／0／0 | 45.09秒 |
| ReferenceArchitectureTest（既存） | 2 | 0／0／0 | 17.85秒 |

実Audit INSERT権限を一時REVOKEして発行／消費／閉鎖それぞれの保存・Audit rollbackを確認。現在能力失効、DISABLED／不存在、実Identity SELECT失敗、scope外／取得不能、transactionなしBusiness拒否、Security INSERT失敗でも拒否継続を実証した。

実DBでは禁止操作がSQLSTATE42501で失敗し、消費record不変を確認。未閉鎖targetは期限切れ・消費済みUNKNOWNでも次許可を拒否し、operation一意違反もrollback。消費競合／閉鎖競合は成功1件、消費対閉鎖は共通row lockで直列化、stale entity mergeはversion保護で既存証拠を保持。保存・再読取後のmicrosecond期限直前は消費可、一致／直後は拒否。制限付きSpring transactionで消費INSERT／version更新をflush後にtest-only `pg_sleep(11)`を実行し、実10秒SQL／transaction上限で失敗・transaction終了・消費とversionのrollbackを確認した。lock_timeout10秒は実DB SHOWで確認し、すべてのJVM／port処理が10秒で強制終了するという保証には広げない。

R04〜R07は未接続scope／TTL／target／証拠と証拠差異の拒否、有効時の必要登録・既存Entity維持、notification sender／runner／registry／scheduler／listener Bean・HTTP handler不在を確認。M01〜M05は既存location／履歴validationを維持して再成功。

### 12.4 実行記録・資源・cleanup

最初のtest実行はcompile段階で停止。追加testのpublic可変array警告はimmutable Listへ修正。既存ExpenseRequestDetailのnullable引数を拒否するincremental出力不一致はsourceの`@Nullable`を確認し、既存source／NullAway設定を変更せずclean buildで解消した。clean test-compileは49.225秒、main121／test34 sourceで成功。その後T16、D12／R7／Architecture2の実接続smokeは成功し、例外変換を整えた最終sourceで上表の全55件＋Architecture2を再確認した。

最終runは`build-support/reference-e2e-verification/target/s1-foundation-suite-20261007/`。class別sanitized XML／Maven log／manifest、`results.json`、`resources.jsonl`、notification source／test／V4／grant27ファイルのSHA-256を保全。各class別Maven／fork1／reuseForks=false、Maven・test heap768 MiB、JUnit並列無効で逐次実行。5秒間隔43 sampleで使用可能memory最小15.40 GiB、disk最小43.74 GiB、DB同時最大1。各DB inspectとSHOWでmemory1 GiB／CPU1／max_connections16／各timeout10秒を確認。各classは10分以内で、DBとRyuk等の補助container消滅を確認してから次へ進み、monitorもfinallyで終了した。

| class | 当該DB ID（削除確認済み） |
|---|---|
| Transaction | `231754b0dd8228b2421021cd64a670e3cfb1ac9bfec643785527bd14073ec1e0` |
| Persistence | `643fdf62a2fe2275cc67939a8da7ef85eed3c5b1b3be3cb036fba567a3f4611e` |
| Registration | `c893619a622810ceb295ab13540bab26909dee2da9161ab397061a06e2b490a2` |
| Migration | `adc485d570e5b8c0ae2881c9434f96b08978cb16c131bd6ec1021004d18c334e` |

初回compile停止のrunは`target/s1-connection-smoke-20261007/`、修正後T16は`target/s1-connection-smoke-20261007-2/`、D12／R7／Architecture2は`target/s1-connection-persistence-20261007/`へ保全。環境の権限付き承認手順で同じ限定検証を実行し、無関係container／processを停止していない。初回smoke runnerの最終表示に前段のcase名が残っていたため、判定は表示文言ではなく当該classの新しいXML／manifestで照合した。最終suite runnerの表示は6 class／55件へ修正済み。

### 12.5 既存回帰と残条件

既存25 class／99件とpackage済みReference E2E1件は、§8のbaselineと同じ集合・条件で`target/s1-regression-20261007/`へ実行しすべてPASS。新規55件の証拠はReference target外へ保全し、E2E直前のclean packageで消失させない。変更後の新規sourceは未commitとしてbase HEAD＋SHA-256で識別し、commit操作を前提に検証を止めない。

変更後回帰の開始16:12:23〜終了16:23:07 JST、644.04秒（約10分44秒）で60分条件以内。新しいXML26件（Reference25＋E2E1）を件数／exit code／failure・error・skip／cleanupで照合し、すべて成功。baselineのclass名＋invocation数との差分0、notification source27ファイルの実行前後SHA-256差分0、HEADは`a316a0d`のまま。既存Architecture2件は既存99件にも含まれ、新規55件へ重複加算していない。

回帰monitorは121 sample、使用可能memory最小15.47 GiB、disk最小43.45 GiB、DB同時最大1。E2E子ReferenceはPID27052、heap768 MiB／pool4／minimum idle1、DBは`b4eed22505ea015e444839500df84e20604d0fcb73836ccccd78bd5af9a3f802`（memory1 GiB／CPU1／max_connections16）。各classのDB・補助container、E2Eのbrowser／issuer／子Reference／port／temp logのcleanupが成功し、外側で子PIDとDBの残存なしを確認した。

E2E直前のoffline clean packageは51.321秒で成功。JAR SHA-256は`DAEE94CAF5A9552CF07F8CEB37B9D578993663A4C2FD72F3C0B6B6CE55CBF014`。JARにはnotification code／V4が含まれ、有限port・Clockのtest harnessと隔離grant SQLが含まれないことをentry一覧で確認。E2Eの秘密非出力検査を通った子logからheap／pool診断だけを保管した。結果は`regression-manifest.json`、class別sanitized XML／log／manifest、`sanitized-package.log`、`sanitized-process.log`、`resource-samples.jsonl`へ保存。全既存`target/s1-*`のraw総量は終了時3,919,416 bytesで1 GiB以内。

本段階は採用済み初回基盤の実装・local検証結果であり、Ownerの初回受入は未成立。Phase 4全体開始・DoD 4-1〜4-5／4-12のPASS、正式運用scope／TTL・時計差、本人認証／worker委譲の受付、停止／provider証拠・通知／復旧／Modulith Level2接続、配布／remoteは後段境界を維持する。標準作業量は実経過時間から推定しておらず、Owner review・環境承認待ちを別枠とする管理条件を維持。今回差分25ファイルは未commitで、commitは事前確認またはOwner操作に従う。

## 13. 初回限定基盤の差分レビュー・local検証結果のOwner承認（2026-10-07）

Ownerは実装ポイントの説明、差分ソースおよび本検証記録を確認し、同日のチャットで「説明も参考に、差分と[検証記録](docs/architecture/validation/phase4-s1-reference-foundation-20261007.md)を確認しました。問題なし検証を承認します」と明示した。

**判定：初回限定Reference保存・認可・Audit基盤の差分レビュー・local検証結果は OWNER APPROVED。** 対象は§11・12のDomain、Repository／JPA Adapter、Spring管理transaction、現在認可／実Audit、条件付き登録と未接続時の拒否、隔離実DB検証および変更後回帰（新規55件・既存99件・package済みE2E1件）の結果。対象sourceは§12で識別した`a316a0d`＋未commit差分と検証時SHA-256であり、§12.5末尾のOwner受入未成立は承認前時点の履歴とする。

正式運用scope／TTL・時計差、本人認証／worker委譲の受付、停止／provider証拠の運用接続、通知／復旧／Modulith Level2、Phase 4全体開始・DoD・配布／remoteは本承認の対象に拡張しない。ローカル手動実行は後日とする。後段は必要reviewと個別開始判断に従う。

本承認はcommit／pushの操作指示ではない。Agentは承認記録のみ更新し、git add／commit／pushを行っていない。commitはOwnerへの事前確認またはOwner自身の操作、remote pushは現時点で不要という境界を維持する。
