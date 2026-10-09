# Phase 4 S1 通常buildと検証専用testの分離 追加Ownerレビュー案

作成日：2026-10-09。状態：`COMPLETE / OWNER ACCEPTED — BOUNDED NORMAL BUILD AND DEDICATED VALIDATION`（§9〜§12）。

通常root buildは、B2の対象28件を既定無効にするだけでは成立しなかった。既存S1の7クラス・64件も検証専用のresource-limits flagと有限環境を前提にするため、通常buildから分離し、S1／B2の専用実行で確認する案を提示する。実行条件の変更と明示実行手順を一体で整え、skipを検証PASSに数えない。

既存Frameworkと検証済みReferenceへの影響を最小化する方針を維持する。Reference／Tooling main、共通helperのassertion、Security、migration、POM／依存、Root Reactor、Framework API／Rules、CIは変更しない。B1／B2の旧受入結果は保持する。

入力：[B2通常build対策の承認済みレビュー](phase4-s1-b2-normal-build-remediation-owner-review-20261009.md)、[実行Evidence§12〜13](../architecture/validation/phase4-s1-b2-normal-build-remediation-20261009.md#12-ryuk訂正後のroot結果と通常実行前提の未達)、[S1目的対応表](phase4-s1-purpose-dod-stage-mapping-20261009.md)、[成果再利用方針§6](phase4-s1-b1-b2-purpose-retrospective-20261008.md#6-方針見直し後のb1b2成果の位置づけ2026-10-09)。確認基点は`feature/phase4-s1-reference-foundation` / `8b03b9f`＋既承認7ファイルの未commit改修差分である。

## 1 通常buildと専用実行の契約

| 実行経路 | 実行する範囲 | 条件と判定 |
|---|---|---|
| 通常root `clean verify` | 固定sourceの既定回帰。S1専用64件とB2 process-backed28件を除く | resource-limits／B2有効化flagを渡さない。台帳上349件のうち257実行・92無効が候補。登録8件は既定対象を維持。Tooling supplier・旧tmp・classpathを要求しない |
| S1専用実行 | 追加対象7クラス・64件 | 既存`koiki.reference.verification.resource-limits.enabled=true`、heap768 MiB、既存各harnessのDB／pool条件でclass逐次実行。64件のfailure／error／skip0・exit0が必要 |
| B2専用実行 | 登録8＋I12＋B8＋X8＝36件 | B2対象28件はB2有効化flag、共通resource-limits flag、明示classpathをすべて供給。登録8件はB2有効化flag不要。36件のfailure／error／skip0・exit0が必要 |

専用集合は64＋36＝100件。登録8件はrootでも実行するため重複観測である。root257＋専用100を357種類のcaseとは数えず、重複登録8を除いた349種類の対応を台帳で照合する。rootに表示された92 skipはPASSへ加算しない。これらはsource上のcase候補数であり、fresh XMLとclass／method照合で確定する。

今回のB2再実行は実行条件と再現手順の成立確認である。R1〜R3ではpermit・認可・Audit等に影響する変更に応じて関連caseを選び、R4ではReference publicationへの接続に合わせて契約とcaseを引き継ぐ。旧28件やTooling凍結fixtureを永久に固定する採用判断ではない。置換する場合は旧case→代替case→証拠→残課題の対応を残し、代替の成立前に削除しない。

## 2 追加7クラスと条件変更

path共通prefixは`koiki-reference-app/src/test/java/org/koikifw/reference/notification/`。既存assertion・method・case ID・件数を維持し、importとclass単位annotationだけを追加する。

| 追加対象file | 件数 | 役割 |
|---|---|---|
| `ManagedRecoveryConfigurationDbTest.java` | 6 | scope／TTL管理設定とDB接続 |
| `ManagedRecoveryConfigurationRegistrationTest.java` | 6 | 管理設定の登録・通常無効等 |
| `NotificationFoundationMigrationTest.java` | 5 | migrationと実Reference起動 |
| `NotificationFoundationPersistenceTest.java` | 12 | permit／消費の保存・不変条件 |
| `NotificationFoundationRegistrationTest.java` | 7 | 通常起動・条件付き登録 |
| `NotificationFoundationTransactionTest.java` | 16 | transaction、認可・Audit、rollback等 |
| `OperationalRecoveryEvidenceDbTest.java` | 12 | 対象・停止・provider観測と運用証拠 |
| 合計 | 64 | S1の復旧操作統制基盤の回帰 |

追加する条件は全7クラスで次を使用する。共通baseへのannotation継承を前提にせず、各実行classへ明示する。

```java
@EnabledIfSystemProperty(
    named = "koiki.reference.verification.resource-limits.enabled", matches = "true")
```

このpropertyは既存helperが既にtrueを要求しているもので、実アプリの有効化propertyではない。条件が未指定の通常buildでは`@BeforeAll`のDB準備を実行しない。trueを明示した場合は、heap・DB・poolの前提不足を既存assertionで失敗させる。失敗をskipに変えたりassertionを除去したりしない。

S1専用testを通常buildから分離しても、その他の通知Domain／設定単体testとB2登録8件は既定対象に残る。64件の存在・case対応・実行結果は専用台帳で確認し、通常buildの成功だけでS1基盤を検証済みとしない。

## 3 差分の上限とOwnership

既承認7ファイル（B2 test3・bridge・準備script・README・Evidence）を保持し、追加は§2の既存test7ファイルだけとする。合計は新規2＋既存12＝14ファイル。今回のレビュー案・承認記録は計画文書の別枠である。Reference内のtest・fixture／準備手順はTooling所有、計画・EvidenceはFramework側の文書管理とする。

READMEとEvidenceには、S1／B2の両実行経路、必要flag、case数、停止／cleanup、旧受入と新結果の区別を追記する。新しいJava型・Maven profile・dependencyは追加しない。既存`NotificationDbTest`／`NotificationFoundationDbHarness`／B1 helper／SQLを変更しない。既存履歴runnerを新しい実行手順から起動しない。

B2明示commandに共通resource-limits flagを追加する訂正は、文書と今回の有限実行commandに限定する。以前の承認済みレビューに記載したB2 commandでは同flagが欠落していたため、そのまま起動しない。通常rootへ同flagやHikari数値を全体供給する方式は採らない。

## 4 固定sourceと依存準備

初回の文書commit・clean source固定`8b03b9f`と成立済みpreflightを基点とする継続訂正である。現在は既承認7ファイルの改修差分が未commitであり、working treeがcleanとは扱わない。今回の方式は、承認後に基点＋既承認7件＋追加7件のhash manifestを固定し、新しい隔離sourceへ反映して検証する案とする。各fileの元／隔離コピー一致と許可path以外の差分0を確認する。local commit／remoteの別承認境界は維持する。

Ownerが新たなclean commitを必須と判断する場合は、未検証差分を無断commitせず、固定方法を別判断する。本案のmanifest方式を初回のclean source固定と混同しない。別端末への再現には、後続の成果commit・source／artifact台帳と準備手順を使用する。

既存run `tmp/b2-normal-build-8b03b9f-20261009-first/`の失敗raw・XML・partial artifact hashを保全し、追加の新raw／source出力を用いる。旧B2のtmp・target・JARはコピーしない。専用m2は今回準備済みrepositoryを継続使用する案とし、同一性・外部依存hashを再照合する。provider3.5.6、JUnit launcher6.0.3、agent1.18.11の不足は解消済み。追加の依存取得・version変更は自動で行わない。

JDK／Wrapper、available memory8 GiB、disk10 GiB、Docker Server、PG17／Ryuk、baseline PID／container、既存run容量を再確認する。旧PIDの一覧を現在の終了対象へ流用せず、起動時刻も確認する。source・環境差分や上限が不成立なら実行前に停止する。

## 5 準備と実行の順序

1. 本案の個別承認後、追加7クラスの条件とREADME／Evidenceを変更し、限定source manifestを固定する。新sourceに旧Tooling target・旧tmp・B2有効化flagがないことを確認する。
2. 新rawで通常root `clean verify`を1回。257実行・92無効の台帳、登録8件、B2 process／ready／supplier未起動、全体exit0を確認する。今回の分離結果が成立しなければ次へ進まない。
3. 同じsourceを標準root `-DskipTests install`1回で専用m2へ供給する。これまで未実施の準備を行うもので、元cacheや旧artifactへの書換え・手動JARコピーは行わない。
4. S1専用7クラスを§2の順で各1回、class後の当該JVM／DB／Ryuk・port・一時fileの終了を確認して次へ進む。各classのXMLを次command前に外部rawへ保全する。
5. 既承認の新準備scriptを1回。Windowsの新source配下に新規preparation directoryを作り、Tooling jdbc package、`jdbc,s1-contract,s1-web` test-compile、jdbc単独test-scope classpath生成各1回。compile support profileをcoordinator runtimeの依存解決へ流用しない。
6. B2登録8→I12→B8→X8を各1回。classpathと共通resource-limits／B2有効化flagを供給し、各class後cleanupを確認する。
7. 既承認の前提負例3回（classpath未指定／不存在／空file）を各1回。B2有効化・共通resource-limitsはtrueにし、意図した前提エラー・子／DB未起動・skip0を確認する。期待失敗を100 PASSに加算しない。
8. Reference／Tooling packageのfixture非混入、意図したReference main・依存の包含、source／artifact対応、最終cleanupを確認してEvidenceへ集約する。

通常rootは失敗後の続行を抑えるため、`-Dsurefire.skipAfterFailureCount=1`をCLIだけで追加する。Surefire 3.5.6の[公式停止条件](https://maven.apache.org/surefire-archives/surefire-3.5.6/maven-surefire-plugin/examples/skip-after-failure.html)はJUnit Platform6.0以上を対象に含み、今回の解決済み6.0.3と照合する。並列実行と再試行は無効とする。これは進行中のtestや外部副作用の即時取消しを保証しないため、fresh XML／logで失敗を確認したら有限runnerも後続を停止し、所有resourceのcleanupへ進む。

```powershell
# 承認後の新しい検証source rootから。$runM2と$classpathFileは絶対pathを検査する。
# 各commandのMAVEN_OPTS=-Xmx768mを一時供給し、finallyで復元する。
.\mvnw.cmd -o -B -ntp "-Dmaven.repo.local=$runM2" '-DargLine=-Xmx768m' '-DforkCount=1' '-DreuseForks=false' '-Djunit.jupiter.execution.parallel.enabled=false' '-Dsurefire.skipAfterFailureCount=1' clean verify

# S1専用の例。他6クラスも各々別command、終了・cleanup後に次を開始する。
.\mvnw.cmd -o -B -ntp "-Dmaven.repo.local=$runM2" -f koiki-reference-app/pom.xml '-Dtest=ManagedRecoveryConfigurationDbTest' '-Dkoiki.reference.verification.resource-limits.enabled=true' '-DargLine=-Xmx768m' '-DforkCount=1' '-DreuseForks=false' '-Djunit.jupiter.execution.parallel.enabled=false' '-Dsurefire.skipAfterFailureCount=1' surefire:test

# B2明示の例。B8／X8も各々別command。
.\mvnw.cmd -o -B -ntp "-Dmaven.repo.local=$runM2" -f koiki-reference-app/pom.xml '-Dtest=B2ProtectedIssueReadTest' '-Dkoiki.b2.reference-acceptance.enabled=true' '-Dkoiki.reference.verification.resource-limits.enabled=true' "-Dkoiki.b2.tooling.classpath-file=$classpathFile" '-DargLine=-Xmx768m' '-DforkCount=1' '-DreuseForks=false' '-Djunit.jupiter.execution.parallel.enabled=false' '-Dsurefire.skipAfterFailureCount=1' surefire:test
```

Migration／Registrationの実起動pool4／idle1と、DB harnessのpool2／idle0は各sourceが設定する値を維持し、CLIでHikari数値を共通上書きしない。共通resource-limits flagだけを供給する。通常rootのB2／S1無効化はforkへ実際に届いたpropertyとXMLを照合する。

## 6 資源とRyukの管理案

| 経路 | JVM／DB等の上限案 | Ryukと終了確認 |
|---|---|---|
| 通常root・標準install | 当該JVM4、PG2、接続24。Maven／test fork heap768 MiB | 通常rootは承認済み訂正のRyuk最大2を維持。class別forkの重なりを観測し、超過で停止 |
| S1専用64件 | 当該JVM2、PG1、接続16。heap768 MiB、各既存DB memory1 GiB／CPU1・max_connections16等は変更しない | class逐次、Ryuk1。class終了後に当該Ryukも消滅してから次へ。cleanup待ち最大30秒／classを実行枠へ計上 |
| B2専用28件 | 常駐JVM4、jcmdが重なる最大10秒だけ総JVM5、PG1、接続8。各常駐heap768 MiB | **今回の追加判断案：Ryuk最大3**。親test JVM・coordinator・前coordinatorの終了待ちの重なりを有限観測。所有session／起動時刻を記録し、未知所有・3超過・終了後30秒超の残留で停止 |
| B2登録8件・前提負例 | 当該JVM2、live supplier／PG未起動 | 前提負例ではRyukも新規起動しない。登録testの既存方式は維持 |

B2のRyuk3は本物の稼働結果ではなく、process構成からの上限提案である。coordinatorは自身のTestcontainers clientでDBを作り、親も`attachedDatabase()`でDocker clientへ接続する。これらと前coordinator終了時のreaper重なりを無視したRyuk1前提では、通常rootと同じ齟齬を繰り返す可能性がある。3以内の成立を保証せず、最初の明示classの実測を確認し、超過なら停止する。Ryuk／Testcontainersを無効化しない。DB・JVM・業務assertionを緩める許可ではない。

B2の各methodは既存closeで当該子・DBを終了させ、class後に親を含む全当該resource消滅を確認する。Ryuk3の提案はB2だけに限定し、通常rootへ流用しない。所有者が識別できないcontainerを許容数の範囲という理由で放置・削除しない。

## 7 時間と回数とEvidenceの上限

追加64件を検証するため、既存90分の総枠内で未実施段階の配分を見直す案とする。標準install／Tooling準備20分を15分へ、明示36件15分をS1／B2合計100件20分へ変更する。総枠は増やさず、以前の承認がこの配分変更を含むとは扱わない。

| 枠 | 提案 |
|---|---|
| preflight・依存確認 | 既存15分枠。既知取得・環境／差分確認の消費を再照合。追加取得なし |
| 通常root | 累積30分枠のうち既知約423.53秒消費、残約22分56秒。追加1回だけ |
| 標準install・Tooling準備 | 未実施15分枠へ訂正。root install1回、準備script1回／3 goal。script内部20分の単独上限より、このstageの残枠を外部runnerで先に適用する |
| S1／B2専用実行 | 未実施20分。S1 64件を8分、B2登録8＋process28を12分。class後cleanup待ちも各枠内。各class1回、自動再実行0 |
| 前提負例・package | 未実施5分。3負例各1回、有限package照合 |
| 最終cleanup | 5分を予約。他枠へ流用しない |
| 作業量／容量 | 既存作業量6時間進捗確認／8時間停止、sanitized raw256 MiB、子log1 MiB、classpath1 MiB／512 entry、全run disk増分10 GiB以内を維持。追加source／保全copyも累計へ加算 |

Owner待ちと文書整理の時間は検証command実測と分け、未計測の環境管理を保守的に記録する。90分の残枠を再計算できなければ実行前に停止し、予算を自動リセットしない。新sourceを追加しても初回失敗・旧出力は保管し、容量管理のため無断削除しない。

検証を成立とするには、通常root exit0と予定無効92件、S1専用64件とB2専用36件の全件実行、前提負例の意図した拒否、source／artifact／package整合、cleanupを揃える。通常rootだけの成功、部分case PASS、失敗後のskipだけで完了にしない。Linuxは通常buildの条件をsource点検する範囲で、実測／CIはremote個別判断。B2明示はWindows限定を維持する。

## 8 Owner判断事項

1. 追加7クラス・64件のclass条件を採用し、既承認7件と合わせた14ファイルの差分・case対応を許可するか。
2. 基点8b03b9f＋限定改修hash manifestのsource固定方式を採用し、環境差分確認後に追加root1回と未実施の専用100件等へ進めるか。
3. S1／B2共通resource-limits flag供給、CLI fail-fast、class後cleanup、B2だけRyuk最大3という有限上限を採用するか。
4. 総90分を維持した準備15分／専用20分の配分訂正と、§5〜§7の回数・集合・容量・停止条件を採用するか。

code・実行開始は本案への個別判断後とする。本案作成時点で追加7クラスの変更、準備script実行、Maven／Docker再実行、commit／remoteは行っていない。肯定consume／close、Reference非同期実装、Framework変更、正式受渡し・DoD・Phase 4全体へ拡張しない。

## 9 Owner承認記録

2026-10-09、Ownerは本案を確認し、「確認、承認いたします」と明示した。§1〜§8の追加7クラス、計14ファイル、基点＋限定hash manifestのsource固定、通常rootと専用100件等の有限実行、B2だけRyuk最大3、総90分内の配分訂正・cleanup／停止条件を承認済みとする。同じ範囲の開始承認は再要求しない。

source・環境・残予算の差分確認後に実装・検証へ進む。結果受入・改修commit・remote／Linux CI実測は別判断を維持する。

## 10 承認後の実行結果と停止

追加7クラスはimportとclass条件だけを変更し、14ファイルの限定hash manifestで新sourceを固定した。Windows通常rootは349件中257実行PASS／92予定skip、failure・error0、BUILD SUCCESS。標準installも成功した。

専用S1は先頭2クラス12件PASS／skip0。次のNotificationFoundationMigrationTest起動中に当該JVM3を観測し、§6の上限2を超えたため停止した。終了後当該JVM／container残留なし。第三JVMの識別情報を現runnerが保存しておらず、原因は未特定。未実行の集合へ続行しない。詳しい時刻・資源・証拠・未達は[Evidence§15〜16](../architecture/validation/phase4-s1-b2-normal-build-remediation-20261009.md#15-通常root成功と予定92件の分離確認)、再開の判断案は[有限JVM観測レビュー](phase4-s1-dedicated-jvm-observation-owner-review-20261009.md)を参照する。本票の承認済み上限を自動訂正しない。

## 11 後続の個別訂正・再開による最終成立

後続のOwner承認で有限観測、IDE側jcmdと当該所有数の分離、接続監視DBの分離、必要な最小再実行を行った。元失敗・source／artifact epochと上限を保持し、Windows通常root257 PASS／予定無効92、専用S1 64＋B2 36＝100 PASS／skip0、前提負例3件、package整合・cleanupが成立した。登録8重複を除く349種類のcase対応を照合した。[最終Evidence§23](../architecture/validation/phase4-s1-b2-normal-build-remediation-20261009.md#23-3承認後のb2成立負例package最終結果)に保証範囲・欠測／UNKNOWN・予算・hash・証拠を記録した。結果受入・改修commit・remote・Linux実測・後続非同期実装は別判断とする。

## 12 結果のOwner受入

2026-10-09、OwnerはEvidence§23を確認し結果を明示承認した。通常buildと専用testの分離について、記載したWindows限定結果・case対応・負例・artifact／cleanupと限界を`COMPLETE / OWNER ACCEPTED`とする。[受入記録§24](../architecture/validation/phase4-s1-b2-normal-build-remediation-20261009.md#24-最終結果のowner受入承認)を正本とし、改修commit・remote・Linux／CI実測・後続実装は別判断を維持する。
