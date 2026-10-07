# S1初回Reference基盤：端末preflight（2026-10-07）

**最新結果：§8の既存25 class／99件＋E2E1件baselineはPASS。検証専用設定の実効確認・baseline取得まで完了。通知の業務基盤実装・新規55件は未開始。** §1〜7の未実施／停止記載は各確認時点の履歴として保持する。

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
