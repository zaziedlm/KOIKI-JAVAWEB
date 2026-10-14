# S1最小方式：初回局所検証Evidence（2026-10-05）

**状態:** preflight / L1〜4 LOCAL PASS。L1 11 tests、L2 9 tests、L3 8 tests、L4 12 tests、各failures / errors / skipped 0。局所結果から正式採用・Reference認証・DoD / Gateを認定しない。
**承認:** [方式票B1〜6](../../development/phase4-s1-method-ballot-and-local-verification-contract-20261005.md)。Ownerの個別継続指示でpreflight / L1、L2、L3、L4を順次実施した。
**baseline:** `docs/daily-development-workflow` / `be4bce1`＋L1〜4専用test / support / resourcesと文書差分。POM・既存Source・V1 / V2・正式migrationは変更していない。
**run-id:** 初回`s1-l1-20261005-01`、形式修正後`s1-l1-20261005-02`、最終`s1-l1-20261005-03`。raw証拠は`build-support/phase4-level2-verification/target/s1-minimum-evidence/`の各run-id配下へ保存した。target配下は非追跡であり、本文に結果・checksum・条件を残す。

## 1. preflight

- Java: Temurin 21.0.12.1。Wrapper: Maven 3.9.16。
- 初回Docker診断はEngine未起動でpipe不存在。Ownerが起動した後、sandboxで接続拒否。権限付きの同じ診断でServer 29.5.3 / linux amd64応答を確認した。メモリ診断のアクセス拒否も権限付きで再確認した。権限迂回・OS設定変更は行っていない。
- 端末メモリ: total 33,389,784 KiB、available 15,363,636 KiB（約14.7 GiB）。空きdiskは初回52,289,167,360 bytes（約48.7 GiB）。開始条件4 GiB / 10 GiBを満たす。
- `help:effective-pom`、`dependency:tree`は`-o -Pjdbc`で成功。既存artifact / pluginを利用し、新artifact install / publishは行わない。
- PostgreSQL image: `postgres:17-alpine@sha256:18cfe3ef5e6815560c98237d6216d1e5119702fb0f3894c8785dd58b8bbe5d73`。testはこのdigestを指定する。
- 実効依存: Boot 4.1.1、Modulith 2.1.1、Testcontainers 2.0.5、PostgreSQL JDBC 42.7.13、JUnit 6.0.3。Surefire 3.5.6、Compiler 3.15.0。実効POMにargLine / javaagent指定がなく、`-Xmx768m`を適用した。
- fork heap 768 MiB、DB memory 1 GiB / CPU 1・max_connections 16をfixtureで設定し実効値をassertした。poolなし、逐次testで同時connection最大2、DB1＋管理Ryuk 0.14.0。PostgreSQL実versionは17.11。Docker Engine側の資源はCPU6 / memory 16,771,686,400 bytes。ピークmemory / CPU使用量は未採取で、容量・性能PASSではない。
- 最終実行後に対象DB container `89070ea37c0a5dc28ce604840abde58855b39dbcf06961fc704928c1874d0642`の`docker ps -a --filter id=...`は0件、Ryukの同様の一覧も0件。別containerを削除していない。端末available memory 19,134,012 KiB、空きdisk 51,817,922,560 bytes。

## 2. L1必須枝と期待件数（実行前固定）

JUnit methodは以下の11件。全件実行・failures / errors / skippedが0であることと、XML上のmethod名を照合する。loop内の権限負例も各SQLSTATE assertionで判定し、11件はSQL操作数ではない。

| ID / method suffix（prefix `l1_`） | 契約 |
|---|---|
| 01_workerLocksAndCommitsConsumptionAndAuditBeforeSend | Web発行、worker row lock・消費INSERT、観測connectionからcommit前0 / 後1、送信probe順序 |
| 02_workerCannotIssueOrChangeImmutablePermitFields | worker発行拒否、publication / event / 環境 / listener / actor / 理由 / 期限変更拒否、前後snapshot一致 |
| 03_consumptionCannotBeUpdatedDeletedOrTruncated | 消費UPDATE / DELETE / TRUNCATEを拒否、snapshot保全 |
| 04_duplicateConsumptionFailsEvenAfterVersionReset | 許可ID再INSERT 23505、version巻戻しでも1件・元operation維持 |
| 05_explicitRollbackRemovesBothRecordsAndDoesNotSend | 消費・Audit相当記録ともrollbackで0件、送信0 |
| 06_auditWriteFailureAbortsConsumptionTransaction | Audit相当NOT NULL失敗23502、transaction abort 25P02、rollback後双方0・送信0 |
| 07_runtimeRolesHaveNoOwnerInheritanceOrDdlPrivileges | 実login role、superuser / role作成 / DB作成 / 継承 / bypass不在、owner member不在、schema CREATE / SET ROLE / DDL拒否・sequenceなし |
| 08_unresolvedTargetRejectsAnotherPermitUntilHumanConfirmation | 未解決対象への新許可23505、確認Webの確認終了後だけ新許可可 |
| 09_webCannotWriteConsumptionOrTamperWithIssuedTarget | Webは消費書込不可、発行後のactor / publication変更不可 |
| 10_workerCannotConfirmOrDeletePermit | worker確認終了・DELETE / TRUNCATE不可、snapshot保全 |
| 11_readOnlyRoleCannotLockOrWriteAndVersionPermissionIsColumnLimited | read-only row lock拒否、書込拒否、worker UPDATEはversion列に限定 |

role / DDLは[専用SQL](../../../build-support/phase4-level2-verification/src/test/resources/s1-minimum/permit-storage.sql)を明示適用し、[L1 test](../../../build-support/phase4-level2-verification/src/test/java/org/koikifw/buildsupport/phase4/S1PermitStorageTest.java)はSpring contextを起動しない。`s1.audit_probe`はtransaction観測専用で、許可管理の3つ目の正式recordではない。

## 3. 実行command・結果

実効POMのSurefire / argLineを確認し、方式票のL1 commandへoffline指定を加えて権限付き実行した。終了コードとXMLで判定し、必須11 method名の完全一致・各件数・skipなしを別scriptで照合した。

```powershell
.\mvnw.cmd -o -f build-support/phase4-level2-verification/pom.xml -Pjdbc "-Dtest=S1PermitStorageTest" "-DforkCount=1" "-DargLine=-Xmx768m" "-Djunit.jupiter.execution.parallel.enabled=false" "-Dsurefire.failIfNoSpecifiedTests=true" test
```

| run | 結果 / 理由 | 時間 |
|---|---|---|
| 01 | DB起動前のimage名互換性チェックでERROR。`postgres:17-alpine@sha256:...`をTestcontainersが互換名と認識しなかった。実DB枝は未実施 | Maven 9.767秒 |
| 02 | 同じdigestを`postgres@sha256:...`へ修正。11 tests / failures 0 / errors 0 / skipped 0、exit 0。compilerのUnnecessaryAsync警告あり | test 4.501秒、Maven 11.925秒 |
| 03 | 逐次probeの不要なAtomicIntegerを通常intへ修正して再確認。11 tests / failures 0 / errors 0 / skipped 0、exit 0、compiler警告なし | test 4.475秒、Maven 11.762秒 |

原因未変更の反復は行わず、各回のXML / consoleとsource checksumを上書き前に保存した。初回の失敗は最終PASSに含めて隠さず記録する。preflight / target出力を除く変更は新test・専用SQLと文書のみ。既存test sourceもmodule全体としてcompileされたが、実行したのはL1の11件のみで、他test / Failsafe ITの回帰PASSは主張しない。

| 最終証拠 | SHA-256 |
|---|---|
| S1PermitStorageTest.java | `a4bb25b7a339efe3d525285fce38d038787f9652969a8ee2a22a2a58eeb7d34f` |
| permit-storage.sql | `6506c0146dfd6f996372412370d4fcae02ba0d9d78adeda1db82e8775e28b85e` |
| TEST-org.koikifw.buildsupport.phase4.S1PermitStorageTest.xml（run03） | `9900b245f872b4aef260f8429ce9ec934c2f65edebe650e40be4c305317d9276` |
| l1-console.txt（run03） | `2da5b8c02c16e8ecad484dec4ad3a149b0659ea68fd5829a4c7704964fe947a6` |

SQLSTATE 42501（権限拒否）、23505（一意制約）、23502（Audit相当NOT NULL）、25P02（transaction abort）を期待どおり確認した。commit前に観測connectionから消費 / Audit相当とも0、commit後とも1、rollback後とも0。改変拒否時は許可 / 消費snapshot不変、一意消費はversion変更・巻戻し後も維持した。

**後続:** L2の同許可・同対象・確認終了との競合は§5で個別に実施した。L1の直列確認だけから並行制御の成立を推定していない。

## 4. 保証の限界

実DBで権限制限・一意消費・原子的記録を確認する。本人認証・権限照会・Framework BusinessAuditRecorder・JPA mapping・実executor / trace・通常worker排他・外部provider・実OS crashはこのtestに含めない。送信probeは明示的なcommit後順序の観測で、実アプリが同じ順序を守る保証ではない。Web credentialを持つ主体の確認判断の正当性は認証済みApplicationと人の責務。L2の消費／確認競合・L3相関・L4起動も別結果である。

## 5. L2競合検証

**状態:** LOCAL PASS / 9 tests、failures / errors / skipped 0。L1 source / DDLは変更せず、同じ専用SQLを独立したDBへ明示適用した。run-idは`s1-l2-20261005-01`。Toolingの[競合test](../../../build-support/phase4-level2-verification/src/test/java/org/koikifw/buildsupport/phase4/S1PermitConcurrencyTest.java)に局所の許可消費／確認手順を実装し、正式Referenceへの昇格は行わない。

開始前の再診断はDocker Server 29.5.3、available memory 18,693,368 KiB、空きdisk 51,853,004,800 bytes。L1と同じimage digest・DB memory 1 GiB / CPU1 / max_connections16、fork heap768 MiBを用いる。fork内2worker＋観測connection1、poolなし。別JVMなし。barrier / latch / lock / future待ち10秒を上限とする。

| ID / method prefix | 期待実行数 | 必須条件 |
|---|---|---|
| l2_01_samePermitConcurrentConsumptionSendsOnlyOnce | 1 | 2workerをbarrierで開始し、SENTとALREADY_CONSUMED各1、消費／Audit相当各1・送信probe1 |
| l2_02_twoPermitIdsForSameTargetCannotBothBeIssued | 1 | 同一対象の別許可IDをbarrierで発行し、ISSUEDと23505拒否各1、許可1・送信0 |
| l2_03_consumptionFirstMakesConcurrentClosureHold | 2 | CONFIRM / CANCEL各1。消費が先にrow lock、確認側のDBロック待ちを実観測後commit。確認はHELD、未解決を維持、次許可拒否 |
| l2_04_closureFirstMakesConcurrentConsumptionRefuse | 2 | CONFIRM / CANCEL各1。確認が先にrow lock、消費側のDBロック待ちを実観測後commit。消費はCLOSED、消費0・送信0 |
| l2_05_runnerExitAfterCommitNeverReusesConsumedPermit | 1 | commit後・送信前でrunner終了を模擬。消費1・送信0、再開ALREADY_CONSUMED、取消HELD |
| l2_06_unknownCommitRefusesRestartRegardlessOfActualDbOutcome | 2 | 実際のcommit / rollback各1。結果不明の認識を保持して、再開UNKNOWN・取消HELD・次許可拒否・送信0 |

合計9 invocation。必須branchのskipは未達とする。後発がDBで待機していることを`pg_blocking_pids`で確認してから先発を解放し、sleepだけで競合成立を推測しない。実行後XMLのprefix別件数と結果を照合する。

このfixtureは「消費済み／結果不明なら通常の確認終了・取消で解放しない」という保守的な操作経路を検証する。人によるprovider結果突合後の最終解決手順・DBだけでUNKNOWNを推定する機能は含めない。runner終了・commit不明は制御分岐の模擬であり、実OS crash・応答喪失・永続的な不明状態の引渡しの証拠ではない。

### 5.1 実行結果

```powershell
.\mvnw.cmd -o -f build-support/phase4-level2-verification/pom.xml -Pjdbc "-Dtest=S1PermitConcurrencyTest" "-DforkCount=1" "-DargLine=-Xmx768m" "-Djunit.jupiter.execution.parallel.enabled=false" "-Dsurefire.failIfNoSpecifiedTests=true" test
```

2026-10-05 18:27:55 JSTにexit 0 / BUILD SUCCESS。Maven全体12.508秒、test9件4.903秒。初回で9件成功、compiler警告なし、再試行なし。XMLの6 method prefix別の実行数1 / 1 / 2 / 2 / 1 / 2を開始前契約と突合した。L1のsource・専用SQL・保存済みXML / consoleのchecksum不変を確認し、不要なL1再実行は行っていない。

- 同許可: `SENT` / `ALREADY_CONSUMED`各1。消費1 / Audit相当1 / 送信probe1。
- 同対象・別許可ID: `ISSUED` / `DUPLICATE`各1、後者は23505。未解決許可1 / 送信0。
- 消費先行: CONFIRM / CANCELの各caseでDBロック待ちを観測。後発は`HELD`、消費1 / 確認未終了 / Audit相当1、次許可23505。
- 確認／取消先行: 各caseでDBロック待ちを観測。後発消費は`CLOSED`、消費0 / 確認終了 / Audit相当1 / 送信0。
- commit後runner終了模擬: 消費1 / 送信0を維持し、再開は`ALREADY_CONSUMED`、取消は`HELD`。
- commit結果不明: 実commit済み・rollback済みの両caseで再開`UNKNOWN`、取消`HELD`、次許可拒否、送信0。DBの消費有無だけで再開可としない。

ロック待ち観測は合計4件。PostgreSQL17.11、max_connections16、DBmemory / CPU設定のassertも成功。対象container `32e88d61b79e9248a82f2d80de8cff302430cb3eb53a503cebbfef402dad171f`の終了後一覧は0件、管理Ryukの一覧も0件。他containerの削除は行っていない。

| L2証拠 | SHA-256 |
|---|---|
| S1PermitConcurrencyTest.java | `c1c0160c90af20f286d4d81cd7f650f711383a2784401a179da7267e6fa5489d` |
| TEST-org.koikifw.buildsupport.phase4.S1PermitConcurrencyTest.xml | `73d73ecc79c971b3d59f2cd8d1449a16b09cb9b3171be4a7a72d1c47f50d6a71` |
| l2-console.txt | `bcd1e221ec59fd54b45289060665ebd8f2ac4f10e037d2fffe5e633cdff3d6dc` |

raw XML / console / prefix件数照合・hashes / cleanup証拠は`target/s1-minimum-evidence/s1-l2-20261005-01/`に保存。次はL3の実Modulith executor相関と厳密対象選別。L2 LOCAL PASSから実provider・実OS crash・実commit応答喪失・正式JPA / 認証 / Audit・DoD / Gateを認定しない。

## 6. L3実executor相関・厳密対象選別

L3 testと専用`s1fixture`を作成・実行しLOCAL PASS。開始前に6 method prefix / 合計8 invocationを契約として固定した。L1 / L2 source・専用SQLは変更していない。run-idは`s1-l3-20261005-01`。

| prefix | 件数 | 必須条件 |
|---|---|---|
| l3_01 | 1 | publication UUID・FAILED・attempt・型／event IDによるpredicate選別。同eventの別listener、別eventはFAILED維持。投入threadと実workerのoperation一致 |
| l3_02 | 1 | contextなし再送は実listenerで拒否、attempt増加／FAILED、送信0 |
| l3_03 | 3 | contextのevent / listener / publicationが不一致なら送信0、FAILED |
| l3_04 | 1 | 送信probe後の例外を経て別operationを同workerで処理。前後context非残留 |
| l3_05 | 1 | options batch1の抽出がfilterより先で対象を選べない負例。対象attempt不変・FAILED・送信0 |
| l3_06 | 1 | 同event別publication COMPLETEDとattempt不一致だけでは対象完了としない。正しいsnapshotの選別で対象だけ実行 |

再診断はDocker29.5.3、available memory18,373,784 KiB・空きdisk51,875,033,088 bytes。DB1 / fork1、既存digest・memory1 GiB / CPU1 / max_connections16、heap768 MiBを維持。明示Bean構成、Flyway無効、Modulith標準schema初期化。単一ThreadPoolTaskExecutor・queue20 / pool4、送信probeのみ。ThreadLocalの不変operationを投入時capture、workerでinstall / finally restoreし、実traceと区別する。新library・既存Source / POM / migration変更はない。

listener呼出にはPublic APIからpublication IDが渡らないため、predicate選別・listener ID・event IDとcontextに拘束したDB行を突合する。汎用的に「listener引数だけでpublication識別ができる」とはしない。真正な許可・現在権限・provider冪等性はL3の保証外。

### 6.1 実行結果

```powershell
.\mvnw.cmd -o -f build-support/phase4-level2-verification/pom.xml -Pjdbc "-Dtest=S1ResubmissionContextTest" "-DforkCount=1" "-DargLine=-Xmx768m" "-Djunit.jupiter.execution.parallel.enabled=false" "-Dsurefire.failIfNoSpecifiedTests=true" test
```

2026-10-05 18:54:17 JSTにexit0 / BUILD SUCCESS。初回8 tests、failures / errors / skipped 0、compiler警告なし。Maven15.727秒、test7.911秒。XML prefix別件数1 / 1 / 3 / 1 / 1 / 1を照合し、L1 / L2 source・専用SQL・保存証拠のchecksum不変を確認した。

- predicate版ではtarget UUIDのpublicationだけCOMPLETED。同event別listenerと別eventのpublicationはFAILEDを維持、送信probe1。
- main threadのcaptureから`s1-real-worker-1`の実listenerへoperation / publication / event / listenerが一致した。
- context欠落とevent / listener / publication不一致は実listenerで例外となり、対象attemptは増加してFAILED、送信0。
- 送信probe後の例外から別event / operationへ切替え、同じworker threadを再使用して正しい別operationを観測した。全before-install / restoredのoperationはnull、callerもnull。provider冪等性や実traceの証拠ではない。
- options版batch1は先に抽出した対象外FAILEDをfilterへ渡し、target未選定。filter評価1、target attempt不変・FAILED・送信0。APIのvoid復帰を受付・完了成功としない。
- 同event別publicationがCOMPLETEDでも、attempt不一致のtargetはFAILEDのまま。正しいsnapshotで選別後にだけtargetがCOMPLETED。

対象DB container `79b172999639859ebbd07d8389e6bc3cd52de8e9f23194497a6ec8eb1f73a6cb`と管理Ryukの終了後一覧は0件。context / executor / Hikari / JPAのcloseをログで確認した。JPA auto構成の起動は正式notificationのJPAモデル検証を意味しない。DBには意図的なFAILEDが残ったまま、当該使い捨てcontainerだけをcleanupした。

| L3証拠 | SHA-256 |
|---|---|
| S1ResubmissionContextTest.java | `3b84f3d6410242e76a2ce3a2356ef803506584ce5680690160ad37a844a57e0c` |
| S1ResubmissionFixture.java | `ea78de7da258e685d615cba5f366c492c17faa26a00860e88a1cf858cdd3c4be` |
| TEST-org.koikifw.buildsupport.phase4.S1ResubmissionContextTest.xml | `31d75d85ffe5cd7d3363277b3efd245b832a897340c3bf3a7f9106ecfea8310e` |
| l3-console.txt | `fc33076db1e74c8880a71552c365da15e6a68f154f832b1b0332e627bce87dcd` |

raw証拠は`target/s1-minimum-evidence/s1-l3-20261005-01/`。次はL4目的別mode組立・起動時副作用。L3のtest専用operation decoratorを既存Framework decorator・Public APIへ昇格させていない。

## 7. L4目的別mode組立・起動時副作用

L4専用test / TestConfigurationを作成・実行しLOCAL PASS。開始前に6 method prefix / 12 invocationを固定した。run-idは`s1-l4-20261005-01`。Spring Bootの実起動・ApplicationRunner実呼出し、Bean一覧・in-memory snapshotを観測した。Web server・SecurityFilterChain・CSRF / Session・DB・既存Referenceのscanは起動していない。Bean名の模擬と実認証／実registryの保証を混同しない。

sourceは[L4 test](../../../build-support/phase4-level2-verification/src/test/java/org/koikifw/buildsupport/phase4/S1ModeAssemblyTest.java)と[mode専用fixture](../../../build-support/phase4-level2-verification/src/test/java/org/koikifw/buildsupport/phase4/s1fixture/S1ModeFixture.java)。

| prefix | 件数 | 必須条件 |
|---|---|---|
| l4_01 | 1 | 通常modeはlistener / senderあり、閲覧／復旧runnerなし。起動時の副作用不変。明示dispatchのpositive controlだけprobeが変化し、closeで追加変化なし |
| l4_02 | 1 | 確認modeはinspectionだけ。sender / 通知listener / ApplicationRunnerなし、起動・read・dispatch event受信・closeでsnapshot不変 |
| l4_03 | 1 | 復旧modeはsender / 対象ID付きrunnerあり、通常listenerなし。Bootによるrunner実呼出し1、起動・通常dispatch event・closeで送信0／snapshot不変 |
| l4_04 | 3 | mode欠落・unknown・大文字誤modeは起動拒否、runner0・副作用不変 |
| l4_05 | 3 | permit / publication / operation ID欠落は各起動拒否、runner0・副作用不変 |
| l4_06 | 3 | permit / publication / operation ID不正は各起動拒否、runner0・副作用不変 |

fork1 / heap768 MiBのみ、DB container・追加library・別JVMなし。modeとIDを必須値としてBean生成前に検証し、送信Beanは検証済みOptionsへ依存させる。復旧runnerは起動時の対象準備だけを行い、許可消費と送信は別gateとするtest専用候補。正式単発processの全実行契約・既存non-Web起動成立を実証したとはしない。

### 7.1 実行結果

```powershell
.\mvnw.cmd -o -f build-support/phase4-level2-verification/pom.xml -Pjdbc "-Dtest=S1ModeAssemblyTest" "-DforkCount=1" "-DargLine=-Xmx768m" "-Djunit.jupiter.execution.parallel.enabled=false" "-Dsurefire.failIfNoSpecifiedTests=true" test
```

2026-10-05 19:04:50 JSTにexit0 / BUILD SUCCESS。初回12 tests、failures / errors / skipped 0、compiler警告なし。Maven8.544秒、test1.156秒。XML prefix別件数1 / 1 / 1 / 3 / 3 / 3を照合。ログの起動失敗9件は想定した入力拒否であり、assertThrowsと原因messageで照合している。L1〜3のsource / 専用SQL / 保存済み証拠のchecksumは不変。

| mode | 実際のfixture Bean | 起動・操作の観測 |
|---|---|---|
| normal | options / sender / normalListener | 起動時snapshot `(sends=0, registryWrites=0, attempts=1)`。明示dispatchだけがpositive controlとして`(1,1,2)`へ変更、closeで追加変更なし |
| check | options / inspection | Sender / 通知listener / ApplicationRunnerのBeanが0。起動・read・dispatch event・closeでsnapshot不変、runnerStarts0 |
| recovery | options / sender / recoveryRunner | 通常listener / inspectionなし。Bootがrunnerを実際に1回呼び出すが、送信0・snapshot不変。通常dispatch event・closeでも変更なし |
| invalid input | mode欠落／unknown／大文字誤指定、3種IDの欠落・不正 | 起動拒否、runnerStarts0、snapshot不変 |

registryWrites / attemptsはin-memory probeの値で、実Modulith DBの更新抑止を測定したものではない。目的別Bean集合と送信経路の有無を検証し、既存Referenceの条件・Security・実registry・scheduler無効化は後続の正式接続reviewで確認する。各成功contextはtry-with-resourcesでclose、失敗contextはBootの失敗処理でcloseされる。今回はDocker containerを起動していない。

| L4証拠 | SHA-256 |
|---|---|
| S1ModeAssemblyTest.java | `912001b2a2db97cb725ef0c125bb9a4a77001e0b1329c9c90b47199f9b1595ef` |
| S1ModeFixture.java | `588dd121a620ed271b9ed4d748c0f610d8e297bae169ae51fbf4839457a45ce7` |
| TEST-org.koikifw.buildsupport.phase4.S1ModeAssemblyTest.xml | `42df925dcc5081a002c467ecbc8fa593188f80d7c5a03d989f6699091891d121` |
| l4-console.txt | `178fc2763980f8c4507b19489920c4724872adb4abe53e03cf02a5161cb1f091` |

raw証拠は`target/s1-minimum-evidence/s1-l4-20261005-01/`。test / supportは非配布Tooling専用で、正式Referenceへ自動昇格しない。

## 8. 初回局所結果の出口

| 段階 | 最終結果 | 件数 |
|---|---|---|
| L1 DB保護・単発消費 | LOCAL PASS | 11 |
| L2競合・結果不明 | LOCAL PASS | 9 |
| L3実executor相関・対象選別 | LOCAL PASS | 8 |
| L4mode組立・起動時副作用 | LOCAL PASS | 12 |

各classを順次実行した最終結果の合計40 invocation、failures / errors / skipped各0。単一のcombined run・既存全testの回帰PASSではない。L1初回の形式ERROR・修正後の結果は§3に保持する。

初回局所契約は揃った。次はB1 / B4 / B5へ結果を戻し、B2のTier・B3の認可／Audit残条件と、正式Reference接続・追加ST-BまたはST-C〜Eの開始範囲をreviewする。4 classのPASSだけで方式正式採用・Tier確定・Reference実装開始・実trace／OS crash／独立provider・DoD / Gateを認定しない。人の総合判断と不明時の保留を維持する。
