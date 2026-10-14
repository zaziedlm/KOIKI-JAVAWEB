# Phase 4 S1 R3-B：FAILED publication単一復旧の限定訂正判断案（2026-10-14）

状態：**OPTION B OWNER ADOPTED / OPTION A DEFERRED / R3-B LIMITED RESULTS OWNER ACCEPTED**（[結果§23](../architecture/validation/phase4-s1-r3b-source-preflight-and-blocking-review-20261014.md#23-r3-b限定結果のowner受入2026-10-14)）。OwnerはB03a初回FAIL後、応答喪失後の`FAILED`を扱う方向の**設計**を選択し、その後、人による照合を含む現段階に適した保留案との比較を求めた。本票は[停止記録§11](../architecture/validation/phase4-s1-r3b-source-preflight-and-blocking-review-20261014.md#11-e10事前点検とb03a初回fail停止2026-10-14)を入力に、差分と検証上限を判断できる形にしたもので、初回FAILをPASSへ読み替えない。§1〜4のA案と§7の開始・受入前の記述は当時の履歴であり、現行の開始根拠・受入状態ではない。

## 1. 問題と必要な契約

B03aではloopback provider独立台帳に受理1がある一方、応答喪失によりReference受理観測logは未保存、Modulith publicationは`FAILED`となった。R3-Aの`IsolatedRecoverySelection`は`PUBLISHED`／`PROCESSING`だけを許可するため、復旧子はprovider照会前に停止した。この実際の故障位置を保持し、全停止・単一対象・advisory lock・snapshot一致・対象限定・無追加POSTというR3-A/R3-B境界で`FAILED`からの復旧を証明する。選択statusを偽装したり、publicationを直接DB UPDATEしたりしない。

採用済みSpring Modulith **2.1.1**のローカルJARを静的に確認した。`PersistentApplicationEventMulticaster`は`IncompleteEventPublications`と`FailedEventPublications`を実装する。現行の`resubmitIncompletePublications(Predicate)`は`processIncompletePublications`へ進む。一方、`FailedEventPublications.resubmit(ResubmissionOptions)`は`processFailedPublications`へ進み、`FAILED`候補の取得後にoptionsのfilterを適用する。`processFailedPublications`は先にbatch size／in-flight上限で候補取得件数を絞るため、**ID filter単独では任意のFAILED行から対象を必ず選び出せない**。この順序は実行可能性の静的入力であり、対象限定の実動作PASSではない。

## 2. 最小訂正候補

1. Referenceの選択契約で`FAILED`を明示的に許す。ただし期待status・attempts・event／key／hash／listener／publication IDの厳密一致、元process停止、認可済み単一対象、session lockは維持する。
2. `FAILED`枝は採用済みの`FailedEventPublications.resubmit(ResubmissionOptions)`を使い、ID filter・batch size 1・max in-flight 1・min age 0を明示する。既存`PUBLISHED`／`PROCESSING`枝は現行の`IncompleteEventPublications`経路を維持する。通常起動ではrunnerを登録しない。
3. `FAILED`枝のlease取得時に、対象以外の`FAILED`および既存`RESUBMITTED`が同じ隔離DBにないことをread-onlyで確認する。対象外候補があれば**provider照会・送信前に拒否**する。これにより候補取得の先行limitで対象を取り逃す条件を今回は排除する。広い複数FAILED対象の選択運用は未対応とする。
4. provider照会の`ACCEPTED_MATCH`時は既存受理IDをReference観測logへ独立commitし、POSTを増やさずModulithの完了遷移を待つ。照会不能・不一致・期限条件未成立は従来どおりHOLDし、UNKNOWNからの盲目的送信を許さない。`FAILED`再実行時のattempt増分、`RESUBMITTED`中間状態、listener完了・lock保持を実動作で確認する。

対象候補はReference `IsolatedRecoverySelection.java`の追加差分、既存`SinglePublicationRecoveryService.java`／`IsolatedPublicationRecoveryConfiguration.java`／`JdbcIsolatedPublicationAdapter.java`の限定訂正、必要最小のReference／Tooling testとEvidenceである。Framework API／Rules・migration・POM／依存・Security・通常properties・Root Reactor／CIは変更しない。既存R3-Bの28 code/test/migration file・文書4 file上限を超える場合は、先に停止して判断する。

## 3. 検証の新規枠候補

初回B03a FAILとE09以前のPASSは各epochで保全する。訂正sourceのpreflight後に、Reference・Tooling offline test-compile各1回、通常／async JAR package各1回とapp classes／runtime依存対照を行う。対象外FAILED／RESUBMITTEDが存在する時の送信前拒否を新規選択1件で確認する。成立時、**新source／artifact epochのB03aを1回**、続けてB03b／cを各fresh DB・初回1回とし、provider独立台帳・Reference観測log・publication・別対象・HOLD／cleanupを独立照合する。いずれか不成立ならそこでSTOPする。直接回帰A01／A03／D01／C04と通常root最大1回は、E10成立後にsource／artifact差分と必要性を再点検してから順に実施する。

これは開始票の「追加再実行0回」と新規最大10 methodに対する**明示例外候補**であり、初回FAILの単純再試行ではない。新しい検証枠は**最大70分、cleanup10分予約**を候補とする。ToolingはJVM4／PG1／Ryuk1／DB client8、通常rootはJVM4／PG1／Ryuk2／DB client14、各JVM heap768 MiBを維持する。空きmemory8 GiB／disk10 GiB、raw log100 MiB／作業領域2 GiB、子起動120秒・通常終了30秒・所有子強制停止5秒、保護processと所有container0をfresh preflightで確認する。tmp XML／log／JARはGit移送対象外としてepoch別hashを保全する。

## 4. Owner判断点（FAILED自動復旧案）

`FAILED`を本票の**単一FAILED対象・他FAILED／RESUBMITTED不在**という限定条件で扱うか。さらに、上記Reference訂正・追加の拒否1件／B03a新epoch1件・順序／資源／70分を新規開始枠として承認するか。Owner判断までは設計候補に留め、code修正・再package・B03a再実行・B03b／c・直接回帰・通常root・commit／remoteを開始しない。

## 5. 人による照合を前提にした保留案との比較（2026-10-14）

[R3目的・A/B分割](phase4-s1-r3-purpose-recovery-model-and-minimal-verification-owner-review-20261013.md#4-最小実証を二段階で定義する案)は全停止型の人による判断を採用し、UNKNOWN時のHOLDを認める。一方、[S1目的・DoD対応表](phase4-s1-purpose-dod-stage-mapping-20261009.md)と[DoD実演計画E10](phase4-s1-dod-demonstration-plan-20261005.md)は、**provider受理を照合できる枝ではpublication完了まで**を要求する。B03aの独立provider台帳には受理1件があり、単なる判別不能枝ではない。したがって保留案を安全上の限定出口として採っても、E10aの完遂・DoD 4-2／4-3をPASSにすることはできない。

| 観点 | A：FAILEDを単一対象で再実行 | B：FAILEDをHOLDして人へ引き継ぐ |
|---|---|---|
| 行うこと | `FailedEventPublications`と対象選別guardをReference復旧子へ接続し、provider照会一致なら追加POSTなしで完了を待つ | Toolingの独立observerが`FAILED`、provider受理ID、Reference観測log不存在、停止世代を照合し、**復旧子を起動せず**理由・対象・次の確認先をHOLD Evidenceへ残す。人が照合するまで再送も完了操作もしない |
| 安全性 | 成立時は受理1件とpublication完了を照合できる。選別・状態遷移・lock／時間切れの新しい失敗点を検証する必要がある | 誤再送を避けられる。既存の`Invalid isolated recovery selection`を単なる例外で終わらせず、担当者が扱える保留理由へ変える必要がある |
| 実装・検証負担 | Referenceの複数file、JAR pair再固定、FAILED専用API／対象外FAILED拒否、B03a新epoch＋直接回帰・通常root。§2〜3の候補上限 | 原則Tooling-ownedの事前判定／証拠保存と選択検証。Reference JAR・migrationは変更せず、固定JARを維持。B03aの既存FAILとは別のHOLDケース、B03b／cを含む後段は新しい個別実行枠が要る |
| 到達点 | B03aの照合可能な受理後応答喪失を完了まで実証できれば、E10aの成立候補になる | B03aは`FAILED / HELD / PROVIDER ACCEPTED / PUBLICATION INCOMPLETE`のまま。E10a完遂とDoD 4-2／4-3は**未達**。B03b／cが成立してもE10全体PASSにはしない |
| 後段への扱い | 限定全停止復旧としてR4のoperator認可・Auditへ接続する入力 | 人の判断・正式操作はR4以降の対象として別設計。実provider照会契約、再開手段、保持／purgeは未達と明記する |

保留案の最小実証は、既存B03a初回FAILを消さず、別source epochの**Tooling所有HOLDケース**で①元process停止、②`FAILED`とprovider受理IDを独立照合、③Reference観測log未保存、④対象／別対象と到着・受理数不変、⑤理由code・次の照合先を独立Evidenceに記録すること。確認不能なら`UNKNOWN`のままHOLDする。Reference状態を`PROCESSING`に偽装したり、例外を握りつぶしてpublicationを完了扱いにしたり、Toolingから直接DB UPDATEしたりしない。限定async構成は`spring.modulith.events.republish-outstanding-events-on-restart=false`だが、通常／実環境への運用保証に転記しない。

## 6. 現段階の推奨と残る判断

**現段階ではBの保留案を先に採り、AのFAILED自動復旧は必要性をR4の運用統制とあわせて再判断することを推奨する。** R3-AではPUBLISHED／PROCESSINGの単一対象復旧、R3-BではE07完了とE09安全HOLDが既に限定成立している。ここでFAILED用の別API・候補選別・中間状態の処理までReferenceへ追加すると、全停止型の人の判断を活かす最小実証から作業が大きくなる。一方、保留案でもB03aの完遂未達は残り、R3-B／R3全体を`COMPLETE`へ上げたり、main統合時にE10aまで解決済みと表現したりしない。

OwnerがBを採る場合は、§2〜4のA案を**未採用の設計候補**とし、Tooling-onlyのHOLD証拠、B03b／c、直接回帰・通常rootの対象と順序、追加実行枠・cleanupを改めて限定開始票にする。OwnerがE10a完遂を今回のR3出口に必要とする場合はAを選び、§2〜4の差分・70分候補を精査する。比較の時点ではcode／test修正、再実行、結果受入、commit／remoteを開始しない。

## 7. Owner採用判断（2026-10-14）

Ownerは§5〜6の比較を確認し、**B：`FAILED`を独立証拠付きHOLDとして人の照合に引き継ぐ案を先に採用**した。Aの`FAILED`自動復旧は今回実装せず、必要性をR4のoperator認可・Audit・停止／再開統制とあわせて再判断する。これは設計方針の承認であり、§2〜4のReference訂正・JAR再作成・B03a再実行を許可しない。

B03aの初回FAIL、provider受理1・Reference観測logなし・publication `FAILED`という実値は保持する。Bを選んでもE10a完遂とDoD 4-2／4-3は未達であり、R3-B／R3全体を`COMPLETE`や`OWNER ACCEPTED`へ変更しない。Tooling-owned HOLD証拠と残りの選択検証は[別の限定開始票](phase4-s1-r3b-failed-hold-limited-start-owner-review-20261014.md)で対象・上限・停止条件を具体化し、その開始判断を経る。今回の採用は、検証実行・結果受入・local commit・remote操作の承認を兼ねない。
