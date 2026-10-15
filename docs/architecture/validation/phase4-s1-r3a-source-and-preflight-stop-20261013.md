# Phase 4 S1 R3-A：文書source固定・preflight停止記録（2026-10-13）

状態：**DOCUMENT SOURCE FIXED / R3-A BRANCH CREATED / 訂正後の限定再preflight成立 / R3-A実装未開始**。本書は[承認済み開始票§9](../../development/phase4-s1-r3a-full-stop-contract-and-minimal-start-20261013.md#9-残る実行条件と文書固定の判断入力2026-10-13)に対する実行結果と停止・訂正履歴を記録する。R3-Aのcode／test／SQL／POM作成、package、Docker container起動、選択10件・通常rootは未開始。今回の失敗をR3-A実証結果へ加算しない。

## 1 文書固定とbranch

R2成果commit `66d9b8fb686405148269aa6eefed4819b8c53370`のbranch上で、承認済みの次の2文書だけをstageした。`git diff --cached --name-status`は追加2件、`git diff --cached --check`は指摘0件。local commit `146cd9aa9fbac4d0eeaaa6d610fae4e7e708aa2b`（`docs: fix R3-A full-stop recovery start conditions`）を作成し、R2成果を祖先に持つこととclean working treeを確認した。

1. `docs/development/phase4-s1-r3-purpose-recovery-model-and-minimal-verification-owner-review-20261013.md`
2. `docs/development/phase4-s1-r3a-full-stop-contract-and-minimal-start-20261013.md`

そのcommitから`feature/phase4-s1-reference-full-stop-recovery-r3a`を作成し、HEADが`146cd9aa9fbac4d0eeaaa6d610fae4e7e708aa2b`でcleanであることを確認した。remote push／PR／mergeは行っていない。本Evidenceは停止後の新規未commit差分であり、固定source commitの内容ではない。

## 2 preflightで成立した読み取り

| 項目 | 観測 |
|---|---|
| Java／Wrapper | Temurin JDK 21.0.12、Maven Wrapper 3.9.16。sandboxのWrapperはcache接続拒否で失敗し、同じ`-version`を権限付きで再実行して成功 |
| Docker | sandboxではnamed pipe接続拒否。権限付き`docker version`でClient 29.1.4-rd／Server 29.1.3を確認。割当24 CPU／16646848512 bytes。稼働container0 |
| image | `postgres:17-alpine` sha256:`18cfe3ef5e6815560c98237d6216d1e5119702fb0f3894c8785dd58b8bbe5d73`、`testcontainers/ryuk:0.14.0` sha256:`7c1a8a9a47c780ed0f983770a662f80deb115d95cce3e2daa3d12115b8cd28f0`がlocalに存在 |
| 空き資源 | OS空き物理メモリ13768304 KiB（約13.1 GiB）、C空き533028704256 bytes（約496 GiB）。開始条件8 GiB／10 GiBを満たす一時点の読み取り |
| 保護process | Java PID 30304（09:57:06 JST）／25248（09:57:20 JST）、親44788のVS Code Java extension JREを確認。16:27台の追加Javaは観測間で入れ替わる短命processだった。いずれも停止・変更していない |
| Maven基本設定 | 権限付き・offlineのReference reactor `validate`とTooling `validate`はいずれもBUILD SUCCESS。外部依存取得やtest実行は行っていない |

## 3 停止点：Reference offline test-compile

`2026-10-13 16:28:49 JST`以降のpreflightで、既存sourceの依存とtest compileを確認するため、`MAVEN_OPTS=-Xmx768m`、offlineの`.\mvnw.cmd -o -pl koiki-reference-app -am -DskipTests test-compile`を1回実行した。sandboxではWrapper cache接続拒否となり、同じ操作を権限付きで再実行。13-module reactorのうちReferenceの`maven-compiler-plugin:testCompile`で**BUILD FAILURE**。PowerShell block側の終了コード表示にかかわらず、Maven本文のBUILD FAILUREと5件のcompiler診断を失敗判定とした。testは実行していない。

| 既存test source | 診断 |
|---|---|
| `ExpenseApiControllerTest.java:75` | NullAway：`@NonNull`要求箇所へ`null`を渡す |
| `NotificationFoundationDbHarness.java:131` | Error Prone `UnnecessaryMethodReference`：`ports::current`が不要 |
| `NotificationProviderEndpointContractTest.java:25,26` | Error Prone `AddressSelection`：hostnameを単一IPへ変換する`InetSocketAddress`構築 |
| `OperationalRecoveryStopEvidenceTest.java:15` | Error Prone `NonAtomicVolatileUpdate`：volatile値の非原子的更新 |

4 source fileのSHA-256はR2の隔離snapshot `tmp/r2-local-rethrow-correction-20261013/source/`にある同名fileと一致した。R2通常rootの保存結果は303実行＋専用92 skip／failure0／error0だが、今回の固定branchでの新しいtest compile成功を意味しない。なぜR2の実行では表面化しなかったかは現時点で未確定であり、incremental build等を断定しない。今回の5件を無効化するcompiler optionやtest skipでpreflight成立へ読み替えない。

開始票§9の「preflight不成立ならSTOP」に従い、Toolingのoffline test-compile、R3-A対象実装、A01 gate／隔離DB、JAR作成、選択test／rootへ進んでいない。停止後の読み取りではGit HEADに未commit code差分0、今回所有JVM／container0、Docker稼働container0。保護Java PID30304／25248は残存し操作していない。

## 4 訂正判断の入力

観測された失敗位置は**既存test source 4 file／5診断**に限定される。ただし`ExpenseRequestDetail`のrecord componentには既に`@Nullable String decisionReason`が付いており、test側の`null`を単純な誤入力と断定できない。停止後の読取では、現行`target/classes`のrecord field／getterに`Nullable` type annotationがある一方、compact constructor由来の引数には同annotationが見えない。NullAwayの診断と整合するが、未修正での新規compileで原因を確証したわけではない。必要な修正がproduction recordの明示constructor／annotationへ及ぶ場合は、R3-A開始票のReference差分外として新たに判断する。残る3種のError Prone診断も、testの意味を維持した最小訂正を候補とし、警告抑止・検査無効化を先行しない。

訂正範囲がOwner判断で採用された場合のみ、source差分・再固定方法と、Reference offline test-compileの新しい1回、Toolingの未実施preflight、後段の予算／停止条件を明示して再開する。R2のPASSや残予算をR3-Aの新しいPASS・再試行許可へ転用しない。

本書は停止と訂正候補のEvidenceであり、訂正実装・追加検証・commit／remoteの承認記録ではない。

## 5 §4承認後の最小訂正と追加判断点（2026-10-13）

Ownerは§4「訂正判断の入力」を確認し、承認して進めると判断した。これに基づき、既存test sourceのうち次の3 fileだけを未commitで訂正した。R3-Aの復旧実装・設定・SQL・POMは変更していない。

| file | 最小訂正と保持する意味 |
|---|---|
| `NotificationFoundationDbHarness.java` | `FinitePorts`自身が`RecoveryTargetPort`を実装するため、不要な`ports::current`を同interfaceへのcastに置換。登録するbeanの対象と動作を維持 |
| `NotificationProviderEndpointContractTest.java` | `InetAddress.getByAddress(127.0.0.1)`からloopback IPv4を作り、2つの`HttpServer`に渡す。hostname選択を避け、検証対象のIPv4 endpointを維持 |
| `OperationalRecoveryStopEvidenceTest.java` | volatileのread-modify-writeをやめ、固定済み`receipt.controlGeneration() + 1`を代入。停止証拠と異なる世代を作るassertionを維持 |

`ExpenseApiControllerTest.java:75`の`null`は、申請詳細の`decisionReason`が未設定のときJSONへ`null`として出ることを確認する入力である。productionの`ExpenseRequestDetail.java`ではrecord componentに`@Nullable`があるが、現行`target/classes`のconstructor引数には対応するtype annotationが見えず、NullAwayの診断と整合する。test側の入力・期待値変更やNullAway抑止では境界を保てない。

**追加のOwner判断候補：** `ExpenseRequestDetail.java`のcompact constructorを明示canonical constructorへ置き換え、`decisionReason`引数へ`@Nullable`を明示する。既存の全`Objects.requireNonNull`、`lines`の`List.copyOf`、recordの値・API・JSON期待値は維持する。これはR3-A開始票で認めた`notification`内のReference差分を越えるため、承認なしに編集しない。必要な直接確認は、Reference offline test-compileの新規1回と`ExpenseApiControllerTest#returnsApplicantSafeScopedDetailForCanonicalVersion`の選択実行候補。選択実行をR3-Aの10 invocationや通常rootへ算入せず、再preflightの上限・資源とともに別判断する。

現時点ではReference test-compileの再実行、Tooling preflight、R3-A作成・検証を行っていない。3 test fileの未commit差分と本Evidenceを固定前の訂正候補として保持する。

## 6 追加訂正と再preflightの限定判断案

§5のproduction record注釈訂正を対象に加える場合だけ、`ExpenseRequestDetail.java`1 fileの明示canonical constructor化を許可候補とする。新しいFramework／Reference機能、JSON契約、migration、依存は追加しない。変更後にdiffで全field代入・null check・`List.copyOf`が現行と等価であることを先に確認する。

訂正epochは**新規20分、うちcleanup3分予約**の提案とし、次を順に各初回1回だけ実行する。①既存3 test file＋production1 fileの差分確認、②`MAVEN_OPTS=-Xmx768m`／offlineのReference reactor `test-compile`、③成立時だけ`ExpenseApiControllerTest#returnsApplicantSafeScopedDetailForCanonicalVersion`1件、④成立時だけTooling offline `test-compile`。新規再実行0回。今回所有JVM最大2、各heap768 MiB、Docker container／DB／Ryukの起動0、raw log20 MiB／今回作業500 MiBを上限候補とする。予期せぬcontainer起動、依存取得要求、追加compiler診断、test失敗、時間・資源超過では停止する。旧R3-Aの90分予算残として扱わず、R3-A実装・選択10件・rootはこの判断に含めない。

この限定訂正が成立した後、訂正source・Evidenceをどうcommitして新しいclean sourceを固定するか、R3-A実装／検証予算をどう再設定するかをOwnerへ戻す。現時点の`146cd9a`を訂正済みsourceとして扱わず、未commit差分のままR3-A実装へ進まない。

## 7 §5–6承認後の訂正epochと再停止（2026-10-13）

Ownerは§5–6のproduction record注釈訂正と限定再preflight案を確認し、承認した。16:47:36 JSTから新しい訂正epochとして`ExpenseRequestDetail.java`のcompact constructorを明示canonical constructorへ置き換え、`decisionReason`の引数へ`@Nullable`を明示した。12 fieldへの代入、既存の必須値`Objects.requireNonNull`、`lines`の`List.copyOf`をdiffで照合し、`git diff --check`は指摘0件。§5の3 test fileと合わせてcode差分は4 file、Evidence1 fileは未commitである。

16:48:31 JST、承認済みのReference offline `test-compile`初回1回は**BUILD SUCCESS**。ただしMaven logのmain `compile`は`Nothing to compile`で、変更後のclass fileはsource編集直後（16:47:48 JST）に作成されている。class fileを読み取るとconstructorの第9引数に`@Nullable`の`METHOD_FORMAL_PARAMETER` annotationが存在する。この結果はtest source54 fileの再compile成立と現在のclass metadataを示すが、独立したclean Maven main compileの成立証拠とはしない。

次の選択1件を実行するMaven呼出しは、PowerShellで`-Dsurefire.failIfNoSpecifiedTests=false`が意図した1引数として渡らず、Mavenの`Unknown lifecycle phase ".failIfNoSpecifiedTests=false"`で16:49:32 JSTに**BUILD FAILURE**となった。Parentで停止し、Reference・Surefireは実行されていない。対象testのfresh XMLは存在しない。この失敗はtest assertionのFAILではなく起動コマンドの不成立である。それでも§6の追加再実行0回に従い、その場で引用符を直して再実行しない。Tooling offline `test-compile`も未実施。

停止時の読み取りでは今回所有JVM0、Docker稼働container0、保護Java PID30304／25248だけが残り、操作していない。production・testの4 fileと本Evidenceは未commit。R3-Aの復旧入口、A01 gate、10件／root、packageには進んでいない。

### 7.1 次の限定判断案

Mavenへ渡す`-D`引数を全てPowerShellで単一引用符付きにしたコマンドを事前に固定し、**対象test1件の初回成立確認と、その成立時だけTooling offline `test-compile`1回**を別の訂正epochとして認める候補とする。新規10分（cleanup2分予約）、今回所有JVM最大2・各heap768 MiB、Docker container／DB／Ryuk0、raw log20 MiB／今回作業500 MiB、各invocation1回・再実行0回。対象testは既存のWebMvc sliceであり、予期せぬcontainer起動、追加test実行、依存取得、compile／test失敗、時間・資源超過で停止する。引用符を付ける以外のcompiler optionやtest期待値は変更しない。

この候補は失敗した16:49の呼出しをPASSに読み替えない。成立しても独立clean Maven main compileは未証明であり、後続のsource固定／package時に別途確認する。訂正sourceのcommit、R3-A実装・選択10件・root・remoteは含めない。

## 8 §7.1承認後の対象1件・Tooling再preflight結果（2026-10-13）

Ownerは§7.1の新規10分枠を承認した。16:52:48 JSTの開始時点でbranchは`feature/phase4-s1-reference-full-stop-recovery-r3a`、未commit差分は訂正済みsource4 fileと本Evidence1 fileであり、対象testのXMLは存在しなかった。承認された順序・回数のまま次を実行した。

| 順序 | 実行と結果 |
|---|---|
| 1 | `MAVEN_OPTS=-Xmx768m`、offline、`-pl koiki-reference-app -am`、PowerShellの引数配列で`-Dtest=ExpenseApiControllerTest#returnsApplicantSafeScopedDetailForCanonicalVersion`、`-Dsurefire.failIfNoSpecifiedTests=false`、`-DargLine=-Xmx768m`を各1引数として渡し、`test`を1回実行。16:54:01 JSTに13-module reactor **BUILD SUCCESS**。ReferenceのSurefireは1件実行、failure0／error0／skip0 |
| 2 | 1が成立したため、`MAVEN_OPTS=-Xmx768m`、offline、`-f build-support/reference-e2e-verification/pom.xml -DskipTests test-compile`を1回実行。16:54:18 JSTに**BUILD SUCCESS**。Tooling test source7 fileを再compile。test実行はしていない |

fresh XML `koiki-reference-app/target/surefire-reports/TEST-org.koikifw.reference.expense.adapter.inbound.api.ExpenseApiControllerTest.xml`の更新時刻は16:54:01.316 JSTで、開始時点より新しい。XMLのtestcaseは`returnsApplicantSafeScopedDetailForCanonicalVersion`だけで、tests1／failures0／errors0／skipped0。Surefireにはforked JVMのnative streamに関するdumpstream警告が出たが、今回の選択test結果とreactor終了コードはいずれも成功である。Tooling compileには既存の`AddressSelection`警告2件が残ったが、診断はwarningでBUILD SUCCESSだった。

終了後の読み取りでは今回所有JVM0、Docker稼働container0。保護対象のVS Code Java PID30304／25248のみが残り、操作していない。Git差分は開始時と同じ訂正source4 file＋本Evidence1 fileで、R3-A実装・SQL・POM・package・選択10件・通常rootは未開始。実行は新規10分枠内で終え、再試行はしていない。

**次の判断点：** 今回成立したのは訂正sourceに対するReference test-compile、対象WebMvc test1件、Tooling test-compileの範囲である。独立したclean Maven main compileは未証明。訂正sourceと本Evidenceの差分を確認し、commitによって新しいclean sourceを固定するか、R3-A実装・検証の予算をどう再設定するかは別途Owner判断とする。16:49の誤引数による停止記録は履歴として保持する。
