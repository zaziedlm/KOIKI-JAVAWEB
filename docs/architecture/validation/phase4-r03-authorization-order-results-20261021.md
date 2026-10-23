# R-03：対象認可・version照合順序の是正結果

実行日：2026-10-21（JST）

状態：**限定変更・局所検証完了／47 tests PASS**。通常root全件・Session SPA機能・DoD全体の受入を兼ねない。

## 1. sourceと変更

OwnerのR-03着手指示に基づき、`docs/phase4-review-integrated-plan-20261020`で対応した。base HEADは`e9e760bb33a235d7827b9dc40ac05e3a81d323be`。開始時working treeはclean。今回commit・push・PR作成は行っていない。

production変更は[ExpenseApplicationService.java](../../../koiki-reference-app/src/main/java/org/koikifw/reference/expense/application/ExpenseApplicationService.java)の共通`executeMutation`内の2行入替えだけ。対象取得後に`authorization.accept(request)`を実行し、その後に`requireVersion`を呼ぶ。mutation→flush→成功Business Audit→eventの順序は維持した。

実行時の同source SHA-256：`5476c9e17012ea1e8dcf912e67f14522acff427afdb2d366b2ee48cb04e6e2ff`。

対象外の申請と不存在は、一致・不一致・負versionによらずApplicationでNOT_FOUNDに揃える。認可済み対象の不一致はCONCURRENT_MODIFICATION、負versionはINVALID_INPUTを維持。HTTPではDTOの`@PositiveOrZero`が先に適用されるため負versionは400を維持する。settleへ新たなowner／部門scope認可は追加せず、既存EXPENSE:SETTLE権限を維持した。

Framework・POM・依存・migration・DTO・Security matcher・同期veto・event payload・非同期送信は無変更。SPA機能は未実装。

## 2. 追加した検証

| test file | 内容 |
| --- | --- |
| 新規[ExpenseMutationAuthorizationOrderTest.java](../../../koiki-reference-app/src/test/java/org/koikifw/reference/expense/application/ExpenseMutationAuthorizationOrderTest.java) | Application直接呼出でeditDraft／submit／beginReedit／approve／reject／returnForReworkの6操作を比較。各操作で3種類のversionと対象外／不存在を確認し、集約状態・version・金額・明細数の不変、flush・master参照・Audit・event呼出なしをassert |
| 拡充[ExpensePostgreSqlIntegrationTest.java](../../../koiki-reference-app/src/test/java/org/koikifw/reference/expense/ExpensePostgreSqlIntegrationTest.java) | method security／transactionを通す実DB検証。owner／承認部門scope外×一致／不一致／負versionの6件（各件で不存在も照合）、認可済み×不一致／負versionの4件を追加。状態・version・明細数、成功Audit件数、承認event発行なしをassert |
| 拡充[ExpenseApiPostgreSqlIntegrationTest.java](../../../koiki-reference-app/src/test/java/org/koikifw/reference/expense/adapter/inbound/api/ExpenseApiPostgreSqlIntegrationTest.java) | 実HTTP処理相当のMockMvc＋Bearer＋実DB。3種類のversionそれぞれで他人の申請と不存在を比較。正versionは404・共通code／detail、負versionは既存DTO検証の400・validation code／detailを確認。DB状態と成功Auditなしも照合 |

新規unit testはmethod securityを代替しない。権限拒否は既存method-security test、対象scope・transaction・HTTP応答は実DB testで別に確認した。各parameterized invocation内の比較を独立test件数へ水増ししていない。

## 3. 実行結果

保存済みactivation・JDK21・Maven3.9.16・offlineで実行。既存Error Prone／NullAwayを維持して変更main／testをcompileし、選択classの新XMLを照合した。

| class | tests | 結果 |
| --- | ---: | --- |
| `ExpenseMutationAuthorizationOrderTest` | 6 | PASS |
| `ExpenseApplicationServiceMethodSecurityTest` | 3 | PASS |
| `ExpenseApprovedEventContractTest` | 2 | PASS |
| `ExpenseApiControllerTest` | 6 | PASS |
| `ReferenceArchitectureTest` | 2 | PASS |
| `RequiredNegativeArchitectureRulesTest` | 5 | PASS |
| 拡充`ExpensePostgreSqlIntegrationTest` | 16 | PASS：既存成功ライフサイクル・settle・JPA楽観ロック等6件＋追加10件 |
| `ExpenseAuditRollbackPostgreSqlIntegrationTest` | 1 | PASS：成功Auditを保存できない場合の業務状態rollback |
| 拡充`ExpenseApiPostgreSqlIntegrationTest` | 6 | PASS：既存3件＋追加3件。認可済みの409・正常submitも維持 |
| **合計** | **47** | **failure／error／skip 0** |

変更前の新規unit testは6件すべてfailure。対象外の不一致versionがNOT_FOUNDではなくCONCURRENT_MODIFICATIONになることを再現した。修正後は全件PASS。一致・不一致・負version／不存在と副作用を最終sourceで確認した。変更前の失敗を最終47件へ加算していない。

DBなし6 classは`./mvnw --batch-mode --no-transfer-progress -o -pl koiki-reference-app -am -Dtest=ExpenseMutationAuthorizationOrderTest,ExpenseApplicationServiceMethodSecurityTest,ExpenseApprovedEventContractTest,ExpenseApiControllerTest,ReferenceArchitectureTest,RequiredNegativeArchitectureRulesTest -Dsurefire.failIfNoSpecifiedTests=false test`で実行。

DB3 classは同じreactor入口で1 classずつ直列実行。`-DforkCount=1 -DreuseForks=false -DargLine=-Xmx768m -Djunit.jupiter.execution.parallel.enabled=false -Dkoiki.reference.verification.resource-limits.enabled=true -Dspring.datasource.hikari.maximum-pool-size=4 -Dspring.datasource.hikari.minimum-idle=1`を指定し、Maven heapも768 MiB。保存済みPostgreSQL17 imageを使用し、DB同時1、memory1 GiB／CPU1／max_connections16・pool4／idle1を実効値で照合した。旧S1の実行予算を流用したものではない。

最初のDB16件はPASSしたが、終了直後のcontainer残存を検知して後続を一度停止。再点検で消去を確認後、残る2 classへ進んだ。以後は最大30秒のcleanup確認を挟み、各class終了後の稼働containerなしを確認した。他runの資源を停止せず、強制cleanupは行っていない。Mockito attach・Docker／loopbackに必要な権限付き実行を使用した。

raw記録：`/workspace/cloud-setup/logs/phase4-r03-authorization-order-20261021/`。変更前・選択集合・DB各classのlog、最終9 classのXMLと`selected-test-results.json`、`source.patch`・新規test source・`source.sha256`・`source.json`を保存。未commitのためbase SHA＋差分＋file hashで識別する。今回JAR再package・browser・通常root全件は実施せず、R-02のJAR結果をR-03の証拠へ付け替えない。

## 4. 引継ぎ

[B1設計記録§7.7](../../development/phase4-b1-session-spa-mvc-design-20261020.md#77-r-03の作業範囲と実行結果2026-10-21)へ反映した。次は**B1-04：B1設計とR-00〜03結果の合流点検**。残る実装対象・検証集合・概算・資源／時間を確定し、共有回帰をまとめる計画を維持する。今回の47件をB1機能・DoD全体の受入へ拡張しない。
