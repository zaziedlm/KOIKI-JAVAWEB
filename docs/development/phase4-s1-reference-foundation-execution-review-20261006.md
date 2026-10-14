# S1 Reference基盤：残判断・初回実行資料（2026-10-06）

**状態:** 初回実行資料 OWNER APPROVED（§6）、Gate P4-F設置・初回限定APPROVE LIMITED START（§7）、source固定・preflight条件付き正式作成検証開始 OWNER APPROVED（2026-10-06、§8）。文書commit・clean source固定／preflightは未実施、code／POM／SQLは未作成、Maven／Docker実行なし。
**入力source:** `feature/phase4-s1-reference-foundation`／`f9ea06a`＋正本改訂・対話採用の未commit文書差分。
**入力:** [正式開始票](phase4-s1-reference-foundation-formal-start-review-20261006.md)、[対話採用・正本改訂記録§6・9](phase4-s1-reference-foundation-canonical-delta-and-start-conditions-20261006.md)。本書は新Gate・CPを設けない。

## 1. 残判断を三つへまとめる

| 判断 | 今回提示する具体内容 | 成立後の範囲 |
|---|---|---|
| 実行資料の採用（Owner採用済み、§6） | §2の55件対応、§3の設定名・接続配分・実行／cleanup、§4の対象と停止点 | 採用済み初回条件を満たす作成・検証手順を確定。実行開始は次の判断と分ける |
| Gate P4-Fの設置・初回限定判定 | §5のF-1〜F-5により、初回Reference保存・認可・Audit基盤だけをAPPROVE LIMITED START／REWORK／REJECTで判定 | 対象に限る正式開始経路の成立。CP-F1／2全体・S1全体・DoD完了は認定しない |
| 正式作成・検証開始 | 上記採用・判定、文書source固定後に、承認対象preflight→検証専用設定→baseline→基盤作成・検証を進める | 指定範囲のcode／test／Reference DDL作成と隔離検証。artifact取得／追加install・remote等は含めない |

責任者・受入判定者はArchitecture Owner、作成・操作支援はCodex。サブエージェントなし。Owner review・環境待ち・Gate整合の実時間は別に記録し、24〜40標準時間の作業量へ自動包含しない。開始承認の有効範囲はこの初回と採用済み上限まで。上限到達・scope変更・後段への接続では再判断する。

## 2. 新規6 class／55 invocationのmethod対応案

各行を1 invocationの通常Testとして作成する。名前は対応照合用の案。意味を変えない命名調整はEvidenceに対応を記録し、範囲・件数変更は差分を提示する。複数assertion／列ごとの確認は同じmethod内で実施し、invocation数と確認項目数を混同しない。

### 2.1 RecoveryPermitTest（12）

| ID | method案 | 確認 |
|---|---|---|
| P01 | permitsConsumptionImmediatelyBeforeExpiry | 期限直前は期限規則を満たす |
| P02 | rejectsConsumptionAtExpiry | 期限ちょうどで拒否 |
| P03 | rejectsConsumptionAfterExpiry | 期限経過で拒否 |
| P04 | rejectsDifferentEnvironment | environment差異を拒否 |
| P05 | rejectsDifferentPublication | publication差異を拒否 |
| P06 | rejectsDifferentEvent | event差異を拒否 |
| P07 | rejectsDifferentListener | listener差異を拒否 |
| P08 | rejectsDifferentAttempt | attempt差異を拒否 |
| P09 | rejectsConsumptionAfterClosure | 閉鎖済みの消費拒否 |
| P10 | rejectsMissingIdentityAndTargetValues | permit／actor／target必須値と非空条件。対象値ごとに検証 |
| P11 | rejectsInvalidReasonAndValidityInterval | reason必須、expires<=issued、精度切捨て後の不正期間を拒否 |
| P12 | requiresCompleteClosureEvidence | 閉鎖3項目の一式整合と再閉鎖による証拠書換え拒否 |

### 2.2 RecoveryConsumptionTest（3）

| ID | method案 | 確認 |
|---|---|---|
| C01 | rejectsMissingOperationId | operation必須 |
| C02 | rejectsMissingWorkerGeneration | worker世代必須・非空 |
| C03 | rejectsMissingPermitId | permit必須 |

### 2.3 NotificationFoundationTransactionTest（16）

Public IdentityQuery／Recorder、Spring管理transactionと実DBを使用する。Audit失敗はDB側のINSERT拒否等で実Recorderの失敗を作り、Recorderをmockに置換しない。Query障害も実Adapterの照会失敗を作り、失敗を安全扱いしない。

| ID | method案 | 確認 |
|---|---|---|
| T01 | commitsIssueWithBusinessAudit | 発行とAudit同commit、真正な主体・resource対応 |
| T02 | commitsConsumptionWithBusinessAudit | 消費とAudit同commit。配信成功を意味しない |
| T03 | commitsClosureWithBusinessAudit | 確認終了とAudit同commit |
| T04 | rollsBackIssueWhenAuditFails | Audit失敗で発行rollback |
| T05 | rollsBackConsumptionWhenAuditFails | Audit失敗で消費rollback |
| T06 | rollsBackClosureWhenAuditFails | Audit失敗で閉鎖rollback |
| T07 | rejectsIssueWithoutCurrentCapability | ISSUE不足、拒否Audit |
| T08 | rejectsReadWithoutCurrentCapability | READ不足、拒否Audit・対象情報非露出 |
| T09 | rejectsConsumptionWithoutCurrentCapability | EXECUTE不足、発行後の能力失効も確認 |
| T10 | rejectsClosureWithoutCurrentCapability | CLOSE不足、拒否Audit |
| T11 | rejectsDisabledIdentity | DISABLED主体拒否 |
| T12 | rejectsMissingIdentity | 不存在主体拒否・未確認をUSER扱いしない |
| T13 | rejectsWhenIdentityQueryFails | 実照会障害による拒否・AUTHORIZATION_UNAVAILABLE |
| T14 | rejectsOutsideScopeWithoutLeakingTarget | scope外は保存／取得前に拒否、resource非露出 |
| T15 | rejectsBusinessAuditOutsideTransaction | 実Business Recorderのtransactionなし拒否 |
| T16 | preservesDenialWhenSecurityAuditFails | 外側transaction終了後の実Security記録失敗でも拒否維持 |

### 2.4 NotificationFoundationPersistenceTest（12）

DB管理接続はsetup・権限操作・証拠突合だけ。業務操作は用途別の制限付き接続で検証し、owner接続の成功で代替しない。

| ID | method案 | 確認 |
|---|---|---|
| D01 | rejectsUpdatesToPermitTargetColumns | environment／publication／event／listener／attempt等の不変列UPDATE拒否 |
| D02 | rejectsUpdatesToPermitIssuerAndReason | permit ID／actor／reason書換え拒否 |
| D03 | rejectsUpdatesToPermitValidityColumns | issued／expires書換え拒否、再読取後のmicrosecond期限境界も確認 |
| D04 | rejectsConsumptionUpdate | consumption UPDATE拒否 |
| D05 | rejectsConsumptionDelete | consumption DELETE拒否 |
| D06 | rejectsConsumptionTruncate | consumption TRUNCATE拒否 |
| D07 | preventsNextPermitForUnclosedTarget | 未閉鎖targetの次許可拒否。期限切れ・消費済みUNKNOWNでも解除しない |
| D08 | enforcesUniqueOperationAcrossPermits | operation IDの一意性 |
| D09 | commitsOnlyOneCompetingConsumption | 同permit消費競合は1件だけcommit。用途別pool＋管理接続の計8以内 |
| D10 | preservesClosureUnderCompetition | 閉鎖競合・version保護。消費と閉鎖のlock順序も照合 |
| D11 | exposesCommittedRecordsFromAnotherConnection | permit FK、commit後可視性、発行・消費・観察の許可操作を確認 |
| D12 | hidesRolledBackRecordsFromAnotherConnection | rollback後のpermit／consumption非可視、UNKNOWN保全・禁止操作後不変を確認 |

### 2.5 NotificationFoundationRegistrationTest（7）

| ID | method案 | 確認 |
|---|---|---|
| R01 | omitsFoundationWhenPropertyIsAbsent | 未設定でBean／Entity／Repository不在、既存登録を保持 |
| R02 | omitsFoundationWhenPropertyIsFalse | falseで同じ保護 |
| R03 | rejectsInvalidEnablementProperty | 不正値は診断して起動拒否 |
| R04 | rejectsOperationsWithoutScopeOrTtlPolicy | trueでもscope未接続／取得不能・TTL policy未接続は拒否。起動成立と操作拒否を区別 |
| R05 | rejectsOperationsWithoutTargetOrOperationalProof | 対象現在値・停止／突合証拠の未接続／不足／差異は拒否 |
| R06 | registersOnlyRequiredFoundationComponents | 有効時に必要型だけ登録、既存Identity／master／expense維持 |
| R07 | registersNoDeliveryOrRecoveryInfrastructure | sender／runner／registry／scheduler不在、通知送信なし |

### 2.6 NotificationFoundationMigrationTest（5）

| ID | method案 | 確認 |
|---|---|---|
| M01 | appliesFreshReferenceSchema | Framework二階層契約とV1〜V4、2 table・制約・履歴 |
| M02 | upgradesWithoutChangingExistingRows | V1〜V3からV4適用、既存row／履歴保持 |
| M03 | preservesExistingStateAfterMigrationFailure | 隔離DBの失敗適用で既存row／履歴保全と診断。production failure switchなし |
| M04 | startsDisabledWithOriginalSchema | V4なしで通常無効起動、既存機能成立 |
| M05 | startsDisabledWithUpgradedSchema | V4適用済み＋通常locationで履歴validation・無効起動成立。機能無効化で追加記録を保持 |

不成立の枝を別の成功caseへ置換して件数を合わせない。競合／timeout等の実行がBLOCKEDなら55件受入は未成立。必要確認が55件内に収まらなければ追加差分を提示する。

## 3. 検証設定・接続配分・実行／cleanupの具体化

### 3.1 設定名と用途別接続配分

設定名は正式開始票§3の`koiki.reference.notification.foundation.enabled`（既定false）と、test JVM専用`koiki.reference.verification.resource-limits.enabled`（明示trueだけ制限）を採用する案。両者を混同せず、production設定fileへtrueを追加しない。未設定／falseは従来設定、不正値は対応する設定エラーで拒否する。

新規DB testはpermit／consumer／reader各pool最大2、minimum idle0、管理接続最大2の配分案（合計8）。使わないpoolは生成せず、setup管理接続を業務操作へ流用しない。readerの拒否Auditにも同用途接続を使い、別poolを暗黙に追加しない。実Security Recorderの別transaction時も接続予算を確認する。既存回帰／E2Eは採用済み最大4／minimum idle1を維持する。

新規DBの操作各10秒はlock／statementのDB設定とSpring管理transactionのtimeoutへ対応付け、設定値と超過時のrollbackを確認する。transaction timeout設定だけであらゆるJVM処理が10秒で停止すると主張しない。SQL発行前後・競合待ちを含む実効上限が成立しない場合は停止・差分提示する。

### 3.2 実行順序

1. 文書commit・clean sourceを固定し、承認済みpreflightを行う。未準備artifact／Chromium取得・追加installが必要なら対処差分で停止する。
2. 承認範囲の検証専用設定を作成し、無効時の従来設定維持と有効時の実効制限を確認する。変更はtest fixture／E2E／READMEに限定する。
3. notificationの業務code／DDL追加前に既存25 class＋E2Eのbaselineを同じ制限付き方式で取得する。検証用変更commitとJAR checksumを記録する。
4. 初回基盤code／test／V4を作成し、新規6 class／55件を検証。既存回帰・E2Eをbaselineと同条件で再実行する。
5. 結果XML・DB／Audit・実効資源・新旧DB保護・cleanup・未達をEvidenceに突合し、Owner受入へ渡す。

baselineは追加機能なし、変更後は追加機能通常無効として比較する。V4未適用／適用済みの両通常起動はM04／M05で別に実証する。既存E2Eを新機能E2Eと呼び替えない。

### 3.3 class単位のcommandと停止確認

複数classを一度にSurefireへ渡す方式は、class間で外側からDB停止を確認できないため採用済み方式の実行手順としては使わない。**classごとにMavenを逐次呼び出し、各呼出し終了後に対象resourceの停止を確認してから次の呼出しへ進む**案。正式開始票§5の既存25 class集合を分割して欠落なく実行する。reactorを反復する時間増は60分上限の判定に含め、超過を理由に集合を縮めない。

以下は1 classの引数テンプレート。まだ実行しない。`$referenceTestClass`は事前固定した集合の1要素であり、任意の少数testを選ぶ変数にはしない。

```powershell
.\mvnw.cmd -o -pl koiki-reference-app -am "-Dtest=$referenceTestClass" "-Dsurefire.failIfNoSpecifiedTests=false" "-DforkCount=1" "-DreuseForks=false" "-DargLine=-Xmx768m" "-Djunit.jupiter.execution.parallel.enabled=false" "-Dkoiki.reference.verification.resource-limits.enabled=true" "-Dspring.datasource.hikari.maximum-pool-size=4" "-Dspring.datasource.hikari.minimum-idle=1" test
```

新規classも個別に呼び出すが、poolは§3.1の用途別test設定を使用し、上記の単一pool4引数をそのまま適用しない。Domain testは不要なDBを起動しない。upstream対象class不存在の許容はReference側XML欠落・skipを許容するものではない。

```powershell
.\mvnw.cmd -o -pl koiki-reference-app -am -DskipTests clean package
.\mvnw.cmd -o -f build-support/reference-e2e-verification/pom.xml "-DforkCount=1" "-DargLine=-Xmx768m" "-Djunit.jupiter.execution.parallel.enabled=false" "-Dkoiki.reference.verification.resource-limits.enabled=true" test
```

Maven heapは各実行区間だけMAVEN_OPTSへ-Xmx768mを設定し、既存値を保存・競合確認してfinallyで復元する。秘密を含む可能性のある既存環境変数全体をlogへ出力しない。

各class／E2E呼出しの直後に、次をすべて確認する。どれか未成立なら次を起動しない。

- Maven終了codeと当該classの新しいXML。test実行開始時刻より古いXMLを結果に使わず、失敗・skip・未作成を区別する。
- 当該runのDB container ID（Testcontainersから取得）の停止。run開始前から存在する他のcontainerを停止しない。
- 子Reference JVMとissuer／browser・portのcleanup。Ryukは終了時の残存を確認し、他runの共有resourceを任意削除しない。
- 当該XML・sanitized log・設定／資源・cleanup結果をrunごとのtarget配下へ保存し、次の実行で上書きしない。
- phase累積時間・raw総量・memory／disk条件。上限到達なら未実施分を列挙し、次の実行を止める。

cleanupが正常成立する前提で制限を満たしたと認定しない。既存fixtureは制限有効時にcontainer ID・停止確認用の非機密情報を残す実装を加える案とする。通常設定のtest操作・期待値は維持する。E2Eがfinallyで削除する一時logは、削除前に秘密を検査し、必要な安全情報だけを別のrun証拠へ残す。秘密を含む原文をrawへ保存しない。

時間上限は追加実行停止と安全終了の条件である。timeoutを過ぎた処理が終了しない場合は後続を止め、当該runで作ったprocess／containerだけを特定して終了・記録保全・cleanupする手順へ戻す。全Maven／Java／Docker processの一括killや無関係container削除を行わない。強制停止した結果は正常終了／PASSとして扱わない。

## 4. 作成・実行の対象と戻し方

作成対象は正式開始票§2・3のnotification code／test、Reference-owned V4、隔離検証用grant setup、既存Reference DB fixture・E2E検証専用設定とREADME、Evidence。既存V1〜3・既存Security・Framework code／API／Rules／POM／依存／workflowを変更しない。既存depsで成立しなければ差分提示へ戻す。

grant role物理名は前票§3.1のkkref_notification_owner／permit／consumer／reader案をこの実行資料の命名案として対応付ける。用途と列権限は確定済みgrant表を使う。管理credentialは隔離setup限定で、production credential・fixture passwordを文書／codeへ固定しない。正式運用credential配備は後段。

V4は記録済みの成立確認付き第一案。不成立ならvalidation無効化・history削除で通さず配置reviewへ戻す。機能無効化・追加記録保持を戻し方とし、破壊的down migrationを作らない。

作成・baseline・モデル／保存／migration・最終回帰の区切りで差分とEvidenceを固定する。commitはOwnerの操作または明示指示に従う。未承認push／PR／merge／publishを行わない。

## 5. F-1〜F-5とGateに提出する内容

| 提出物 | 本初回の提出先・現在 | 判定時に明記する残条件 |
|---|---|---|
| F-1 | f9ea06a、A／B／C受入、Phase 3／P4-AR既存Evidence。今回文書差分は未commit | 改訂・本資料をcommitしてclean source固定。Tooling受入と正式候補結果を分離 |
| F-2 | 正式開始票・対話採用の配置／登録／TTL／Audit／DDL／grant、本書§2〜4 | field／signature／mappingと実効権限は承認範囲の実装検証で照合。不成立で停止 |
| F-3 | 新規55件・既存25 class／E2E・新旧DB通常無効保護、既存DoD実演計画 | 初回をDoD 4-1〜4-5・4-12のPASSにしない。通知・復旧・実traceは後段 |
| F-4 | Owner＋Codex、初回24〜40標準時間・12時間相当確認／40時間相当停止、採用済み資源・検証時間・接続・raw／rerun | preflight実測・実効設定で確認。Owner待ち等は別記、後段予算は別採用 |
| F-5 | 正本改訂§9の範囲を反映済み | Gate設置・初回限定判定・作成実行開始は別判断。Docker対象追加は環境実行承認時に反映 |

Gateの対象はST-C初回保存基盤のみ。CP-F1／2の初回部分が開始可能かを判定し、全体完了は認定しない。期限はカレンダー日を推定せず、採用済み作業・検証上限とscope変更時の停止を適用する。

**Owner判定欄:** 実行資料の採用は§6に記録済み。Gate設置・初回限定判定、上記前提でのpreflight・正式作成検証開始は未判断。判定日・承認scope・source固定条件を記録する。実行資料の採用をGate判定・作成実行承認へ読み替えない。

**後続Gate承認:** §7でGate設置・初回限定判定を記録済み。上記欄は実行資料採用時点の履歴として保持し、正式作成検証開始だけを残る判断とする。

## 6. 初回実行資料のOwner採用（2026-10-06）

Ownerは本資料を確認し、「この資料の内容で初回の実行手順を確定してよいです」と示した。本資料の55件method対応、設定名・用途別接続配分、classごとの実行／DB停止確認・cleanup・証拠保全、対象・停止点を提示内容で採用し、初回実行手順を確定する。変更条件の指定はない。

Gate P4-Fの設置・初回限定判定と、source固定・preflight成立を条件とする正式作成検証開始は別判断として残す。今回の採用だけでcode／SQL生成・Maven／Dockerを開始しない。

## 7. Gate P4-F設置・初回限定判定（2026-10-06）

Ownerは「この範囲でGate P4-Fを設置し、APPROVE LIMITED STARTと判定してよいです」と明示した。Gate P4-Fを設置し、初回Reference保存・認可・Audit基盤とその検証だけをAPPROVE LIMITED STARTと判定する。

| 判定事項 | 承認範囲・条件 |
|---|---|
| 対象 | ST-C初回の許可・append-only消費保存、認可、Audit、条件付き登録、Reference-owned V4と本資料の検証 |
| 入力・source | f9ea06a＋対話採用・正本改訂・本実行資料の文書差分。正式実行前に文書commit・clean source固定を必要とする |
| 実施責任 | Architecture Owner＋Codex。Ownerが受入判定、Codexが作成・操作支援。サブエージェントなし |
| 適用条件 | 採用済み責務・安全条件、6 class／55件・既存25 class／E2E、新旧DB通常無効保護、資源・時間・接続・記録・再実行・作業量上限と停止点 |
| 後段 | 通知・復旧・Level 2接続は該当接続前reviewと個別開始判断。実scope／TTL値・provider／停止真正性・実trace等の未達を保持 |
| 完了へ読み替えないもの | CP-F1／2全体、S1全体、当初DoD、P4-AR6／AR-D10／Gate P4-AR、Phase 4全体開始・正式受渡し・remote |
| 残る開始条件 | 正式作成検証開始の別判断、文書source固定と承認範囲のpreflight成立。Docker対象追加は環境実行承認時 |

これは限定Gate判定の承認であり、code／SQL作成・Maven／Docker実行はまだ開始しない。今回の文書入力をpreflight済み・clean実行source・正式実装Evidenceとして認定しない。

## 8. source固定・preflight条件付き正式開始承認（2026-10-06）

Ownerは「文書のコミット・clean source固定後、preflightを実施し、成立した場合に採用済み範囲の正式作成・検証へ進むことを承認します」と明示した。

初回code・test・Reference-owned V4・隔離grant setup・検証専用設定の作成と、採用済みMaven／隔離Docker検証を条件付きで承認する。対象・上限・停止点は本資料と正式開始票に従う。通知・復旧・Level 2接続、Framework API／Rules／依存、artifact取得・追加install、Chromium install、remote等へ承認を拡張しない。

開始順序は以下で固定する。

1. 本承認とDocker対象追加を含む文書差分をcommitし、branch・commit ID・clean worktreeを確認してsource baselineを固定する。
2. 承認範囲のpreflightでJDK／Wrapper・offline依存／source整合・Docker／既存image／Chromium・資源・port・test集合・証拠／cleanupを確認する。
3. 全必要条件成立をEvidenceへ記録した後、§3.2の検証専用設定→baseline→初回基盤作成検証を進める。

sourceが未固定、資源・artifact不足、制限不適合・安全条件不成立なら、その時点で停止し原因・必要差分を提示して再判断する。未実施preflightをPASSとせず、条件成立時の作成検証を承認済みとして扱い、同じ範囲の開始許可を再要求しない。

現在のHEADはf9ea06aで、承認・正本改訂・実行資料の文書差分は未commit。今回の承認を文書commit済み・clean source固定済み・preflight成立済みとは記録しない。コミットはOwnerの操作または明示指示に従う。

## 9. 承認文書commit・別端末引継ぎ（2026-10-06）

Ownerのcommit `371f0245af0f1c4b58f93fbd6287961a502d9b12`で§8までの承認・正本改訂・実行資料を固定した。branchは`feature/phase4-s1-reference-foundation`、引継ぎ作成前のworktreeはcleanである。§8末尾のf9ea06a／未commit記載は承認記録作成時の履歴として保持する。

Ownerはここを作業区切りとし、再開を別タイミング・別端末へ移す。[次回引継ぎ](phase4-s1-reference-foundation-next-session-handoff-20261006.md)に再開時のsource確認・別端末preflight・初期実装リスクをまとめた。preflight・正式code／test／SQL作成・Maven／Docker検証は未実施。承認の再取得は不要だが、移送後のsource確認・環境成立は実証する。
