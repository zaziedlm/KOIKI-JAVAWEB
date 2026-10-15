# S1 R2 JAR対照後の実動作7件：限定開始判断案（2026-10-13）

状態：**LIMITED START OWNER APPROVED / EXECUTED**。後段7件は7／7 PASS、R2限定結果はOwner受入済み。受入は[結果文書§5](../architecture/validation/phase4-s1-r2-selected-seven-case-results-and-acceptance-input-20261013.md#5-r2限定結果のowner受入承認2026-10-13)へ追跡する。commit／remoteは未判断。

## 1 現在の判断入力

[局所再throwの停止・訂正・再実行記録](../architecture/validation/phase4-s1-r2-local-rethrow-correction-and-root-stop-20261013.md#10-9のowner承認と限定再実行結果2026-10-13)により、固定sourceのMaster対象2件と通常rootはPASSした。通常rootは303実行＋専用92skip、failure／error0。[同記録§11](../architecture/validation/phase4-s1-r2-local-rethrow-correction-and-root-stop-20261013.md#11-owner指示による通常async-jar静的対照2026-10-13)では同じ固定sourceからasync JARを1回作成し、通常／asyncのアプリ内容と配布runtime依存の対照がPASSした。実動作7件は未実施で、R2完了・Owner受入は未判断である。

branchは`feature/phase4-s1-reference-failure-isolation-r2`、原固定commitは`cfe9d9efb4cb2bbcdc273df578c75225b539bc08`。今回の局所再throwを含む実入力は未commitの1201 file snapshot `tmp/r2-local-rethrow-correction-20261013/source/`と`source-inputs.json`で固定した。commit IDだけを現在のcode sourceとしない。

| 入力 | 固定場所・SHA-256 |
|---|---|
| 通常root由来JAR | `tmp/r2-local-rethrow-master-recheck-20261013/normal-root-fixed.jar`／`15C490E3439FFA43DE4E686F04F7A730698DE9B09150641A160990BA35695296` |
| async JAR | `tmp/r2-local-rethrow-jar-pair-20261013/async-reference-fixed.jar`／`948F0C79F5D8354826A25A47015C0AD9671843B84911899B390E69DF21C15FA6` |

JAR対照はアプリ実動作、R2-04の403と副作用なし、provider stubの直接回帰、browser Critical Journeyを代替しない。現在の段階は、これら7件だけを順序付きで検証する条件付き開始判断に適する。

## 2 目的・実行順序と回数

各行は**1 Maven invocation／1 case**。直列実行し、前行のfresh XML・log・今回所有資源のcleanupを照合してから次へ進む。前行が失敗すれば後行は実行しない。

| 順 | case | 入力／必須確認 |
|---|---|---|
| 1 | `PackagedReferenceAsyncFailureIsolationTest#normalArtifactCapabilityDenialOriginComparison` | 固定通常JAR、async=false。`koiki.reference.async.r2.comparison.expected-status=403`を明示し、実method securityの拒否、SUBMITTED／version2、成功承認Audit0、async schemaなし、stub到着／受理0を確認 |
| 2 | `PackagedReferenceAsyncFailureIsolationTest#missingApprovalCapabilityRejectsWithoutNotification`（R2-04） | 固定async JAR、async=true。実method securityの403、状態／version保持、成功承認Audit・publication・stub到着／受理0を確認 |
| 3 | `NotificationProviderContractTest#reusesTheOriginalReceiptForIdenticalRequests`（D01） | 独立stubの同一key／payloadに対するreceipt再利用契約 |
| 4 | `NotificationProviderContractTest#rejectsDifferentPayloadWithoutChangingTheReceipt`（D02） | 同一key・異なるpayloadの409と元receipt不変 |
| 5 | `PackagedReferenceAsyncNotificationTest#asyncArtifactDisabledPreservesApprovalWithoutPublications`（C02） | 固定async JARでも通常起動では非同期無効、承認・既存Auditとpublication不作成を確認 |
| 6 | `PackagedReferenceAsyncNotificationTest#correlatesTwoIndependentApprovalNotificationsToCompletedPublications`（C04） | 固定async JARで独立した正常承認2件と通知・publication完了の対応関係を確認 |
| 7 | `PackagedReferenceCriticalJourneyTest#crossesBearerAndSessionBoundariesAndReconcilesExternalEvidence`（C01） | 固定通常JARで既存browser／API／Security／LOGIN Audit／DB／log契約を再確認 |

通常比較とR2-04は局所再throw後の追加確認。D01／D02／C02／C04／C01は今回source・固定artifactで初回1回とする。既に成立したR2の他7件、R1成果、通常root、専用92件を一律再実行しない。今回7件と旧epochの結果はsource／artifact／実行epochを明示して対応づけ、R2全8件を同一epochでPASSしたかのように扱わない。

## 3 入力固定・preflightと証拠

新しい実行epochを`tmp/`内に設け、1201 fileのhash、両JARのhash、branch・保護対象JavaのPID／creation、既存稼働containerを照合する。JARを新epochへcopyしてhash再確認し、ToolingのJAR入力propertyはcopy先の絶対pathに固定する。C01はToolingから参照するsnapshotの`koiki-reference-app/target/koiki-reference-app-0.1.0-SNAPSHOT.jar`が保存済み通常JARと同じhashであることを事前確認する。

最初の2件は`koiki.reference.async.r2.verification.enabled=true`、D01／D02／C02／C04は`koiki.reference.async.verification.enabled=true`を明示し、意図せぬskipを許容しない。通常比較だけは期待status403のpropertyを加える。C01には既存の`koiki.reference.verification.resource-limits.enabled=true`を指定する。必要な通常／async JAR pathは両方とも今回runの固定copyを使う。

JDK21／Maven Wrapper3.9.16、Docker Server／PostgreSQL17-alpine・Ryuk0.14.0のimage、空きmemory8 GiB・disk10 GiB、Playwright1.62.0とChromium／headless revision1234のcache実体、専用canonical JVM tempを開始前に読む。追加download・Docker／Rancher再起動・他session資源操作は含めない。Docker named pipeやCIM／Maven cacheが通常sandboxで拒否された場合はAGENTS.mdの承認済み権限付き手順で同じ診断・実行を行い、拒否とengine障害を混同しない。

各caseのXMLは同じclass内の後続invocationで上書きされ得る。**各case直後に**fresh XML、Maven log、子process log、DB／stub観測、JAR／source hash、今回所有PID／containerの終了結果を別名で保全する。XMLのcase名・tests1／failure0／error0／skip0を確認し、実行開始前のXMLを成果へ数えない。暗黙の再試行は0回。

## 4 有限予算・資源と停止条件

今回7件のために**新規30分、うちcleanup5分予約**を提案する。preflight5分、能力拒否2件8分、直接回帰4件7分、C01 5分、cleanup5分。旧R2・局所訂正・JAR対照の残時間を流用しない。全phaseで今回所有JVM4以内、PG1、Maven本体／test fork／Reference子JVM各heap768 MiB以内。R2 package／provider集合はRyuk1・DB client8以内、C01はRyuk2・client14以内。PGはmemory1 GiB・CPU1・max_connections16を維持する。R2側pool4／min idle0・executor1／queue16と、C01の既存resource limit selectorを維持する。

各集合の開始前と終了後に今回所有資源の0件化を照合する。起動120秒、失敗／completion30秒、子process終了30秒＋所有を限定したforce確認5秒、raw100 MiB／run2 GiB／子log10 MiBを超えない。有限sampleを連続上限保証に読み替えない。

期待403が500等に戻る、状態／Audit／publication／stub観測の不一致、JAR／source hash不一致、通常構成の保護失敗、Browser／DB／Docker環境の不成立、資源／時間／cleanup超過では停止して結果を保全する。期待値、権限、Security chain、production code、test assertion、POM／dependency、schema、Docker設定を変更して成功へ寄せない。追加再実行・root反復・JAR再作成は含めない。

## 5 Owner判断欄

判断対象は§2の7 invocationを上記順序と条件で実行すること、§3の固定source／両JARと今回runへcopyした入力、§4の新規30分・資源・停止／cleanup条件である。成立結果は`docs/architecture/validation/`へ記録してから、R2の限定結果と残課題を別途Owner受入判断に提示する。

**未判断／未実施：** 後段7件の開始、R2結果受入、追加修正・再試行、commit／push／PR、R3〜R5・Phase 4全体。

## 6 Owner承認と実行結果の追跡（2026-10-13）

Ownerは本案を確認して「Owner判断を確認し、承認します。次へ進めます」と明示した。§2の7件・順序、§3の固定入力・Evidence、§4の新規30分・停止／cleanup条件を採用し、追加再試行0回で実行した。7件は7／7 PASS、各fresh XMLはtests1・failure／error／skip0。詳細と旧epoch結果との対応は[選択7件の検証結果](../architecture/validation/phase4-s1-r2-selected-seven-case-results-and-acceptance-input-20261013.md)を正とする。

上記§5の「未判断／未実施」は提案時点の記録である。後段7件実行後、R2限定結果は[結果文書§5](../architecture/validation/phase4-s1-r2-selected-seven-case-results-and-acceptance-input-20261013.md#5-r2限定結果のowner受入承認2026-10-13)でOwner受入済み。追加修正・再試行、commit／push／PR、R3〜R5・Phase 4全体は未判断／未実施。
