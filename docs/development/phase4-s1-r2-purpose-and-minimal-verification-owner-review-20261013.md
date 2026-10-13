# S1 後続工程：R2目的・最小検証範囲のOwnerレビュー案（2026-10-13）

状態：OWNER APPROVED / CONDITIONAL LIMITED START（2026-10-13）。§8にOwner承認を記録する。文書commit・新R2 branchのclean source固定・preflight成立前のcode／test／DB操作・検証開始は行わない。

## 1 入力と次工程の選択

[R1引継ぎ](phase4-s1-r1-fixed-results-cross-pc-handoff-20261013.md)、[R1結果§13〜15](../architecture/validation/phase4-s1-r1-implementation-and-early-verification-20261012.md#15-r1限定結果のowner受入承認2026-10-13)、[ADR-051](../architecture/adr/ADR-051-reference-notification-async-r1.md)、[目的・DoD対応表](phase4-s1-purpose-dod-stage-mapping-20261009.md)、[実演計画E02／E03](phase4-s1-dod-demonstration-plan-20261005.md#3-caseとdodの対応)を入力とする。古い未成立記述は各時点の履歴であり、現行R1受入を取り消さない。

sourceは`feature/phase4-s1-reference-async-r1` / `18cb8b626328e84a5ea5d51c27bba9efd2d27bee`。R1成果`610c589`を含み、開始時は新PC環境記録だけが未追跡だった。remote push完了はOwnerの引継ぎ情報、今回remote照会は未実施。

次候補を**R2：通知失敗と業務確定の分離**とする。R1受入済みの正常経路を入力に、DoD 4-1の未成立負例を先に埋める。R3以降の復旧設計へ進む前に、どの失敗で承認・Audit・publicationが残るかを実証する。

| 段階 | 目的と今回の扱い |
|---|---|
| R2 | 承認後のprovider失敗は承認を取り消さない。拒否・承認transactionのrollbackから通知しない。本票の開始候補 |
| R3 | 停止後回復・応答喪失・UNKNOWN照合・重複抑止。R2結果後に保護モデルと最小集合を別判断 |
| R4 | consume／close、操作認可／Audit、復旧者・旧worker競合。R3の方式決定後に別review |
| R5 | 保持・purge・実trace・観測回復。保持期間・key寿命・sinkを別review |

実provider配信、Framework昇格、正式受渡し、Phase 4全体は別判断を維持する。R2だけでDoD 4-1全体のPASSやS1完了を宣言しない。

## 2 実装から確認した条件

- `ExpenseApplicationService.approve`はtransaction・method securityを持ち、version／scope／状態検査→JPA flush→Business Audit→`ExpenseApprovedEventPort.publish`の順で処理する。
- `ExpenseApprovalListener`は標準`ApplicationModuleListener`から`NotificationDispatchService`へ委譲する。HTTP Adapterは200／201以外を例外とし、HTTP詳細をApplicationに置いていない。
- 現行`LocalNotificationProviderStub`は受理台帳を独立transactionで保存する。意図的な未受理503や、全到着要求の記録は現行にないため、Tooling限定の追加が必要。
- 現行`AsyncReferenceFixture.approve`はcreate／submit／approveをまとめている。拒否と承認時のrollbackを検証するには、test helper内で操作を分ける必要がある。
- 既存`ExpenseAuditRollbackPostgreSqlIntegrationTest`はAuditのmock失敗による**submit** rollbackを検査する。async有効構成での**approve**、実Audit保存失敗、publication不成立の証拠として代用しない。
- 同期vetoはmasterの部門無効化をexpenseの未完了状態で拒否する経路。approveからの同期vetoと混同せず、async有効構成でも既存module間同期契約を保つ別操作として確認する。

このsource読取はtransaction原子性やlibrary失敗状態の実行証拠ではない。FAILED遷移の記録時点・失敗logは、採用済み2.1.1の端末JARと実行結果を照合する。期待と違う結果を成功へ読み替えない。

## 3 Ownershipと変更候補

主な実装成果のOwnershipは**Tooling**。実証対象はReference-owned expense（RICH／JPA共有モデル）とnotificationの既存非同期経路。Framework API／Rules／Starter／BOM／依存version、Security／Identity、通常設定、V1〜V5 migration、permit契約は維持する。新しいread model・公開API・画面を作らない。

初回候補はcode／test合計**4 file以内**、新規test class **1**、新規invocation **8**。既存R1 testの期待値を弱めず、test専用の明示selectorで実行する。次はpath案であり採用・作成は開始判断後。

| path候補 | 必要な差分 |
|---|---|
| `build-support/reference-e2e-verification/src/test/java/org/koikifw/buildsupport/referencee2e/PackagedReferenceAsyncFailureIsolationTest.java` | §4の8ケース。通常の自動実行へ混入させない |
| 同directoryの`AsyncReferenceFixture.java` | draft／submit／approvalを分離、権限別HTTP操作、対象別DB・Audit・publication観測、bounded失敗log採取。既存R1 helper契約を保つ |
| 同directoryの`LocalNotificationProviderStub.java` | test所有の未受理503 modeと到着要求の独立記録。既定の受理・key／payload契約を維持 |
| 同directoryの`R2DatabaseFailureInjection.java`（仮称） | 今回所有DBだけのpublication INSERT／承認Audit INSERT拒否triggerの作成・撤去、対象識別、注入確認 |

初回はproduction code不変更を提案する。既存実装の不具合が判明した場合は、原因と最小修正path・必要回帰をOwnerへ戻す。本票だけで修正範囲を自動拡張しない。POM／SQL migration／Root Reactor／CI変更は対象外。

## 4 必要最小ケース案

全ケースは新PCで固定するasync artifact・async有効・V4無効・fresh隔離DBを使用する。正常1件を新しい観測系の対照とし、R1成果の追加検証や重複成果として計上しない。各caseは固有request／eventと独立したfixture lifecycleを持つ。

| ID／対応 | 操作 | 必須照合と判定 |
|---|---|---|
| R2-01／E02 | 正当な承認、providerが受理台帳保存前に明示503。応答喪失は含めない | 承認応答とDBのAPPROVED・確定version・成功承認Auditを保持。対象publication保存、listener失敗・非COMPLETED／completion_dateなしを確認。採用libraryのFAILED遷移も照合。対象要求の到着と503記録あり、受理0。再送しない |
| R2-02／E03保存原子性 | 正当な承認で対象publication INSERTを隔離DB triggerから失敗させる | 注入点到達を確認、承認操作は不成立。SUBMITTED・versionを元値に保持、成功承認Audit増分0、対象publication0、provider要求／受理0。Portのmock例外で実publication保存失敗を代用しない |
| R2-03／E03承認Audit | 正当な承認で対象APPROVE_EXPENSE Audit INSERTを隔離DB triggerから失敗させる | 注入点到達を確認、承認不成立、状態・version保持、成功承認Audit増分0、publication0、provider要求／受理0。Security Auditを含む全Audit不在は要求しない |
| R2-04／E03権限 | 認証済みでEXPENSE:APPROVE能力のない主体から承認要求 | 既存HTTP／Application拒否契約、状態・version保持、成功承認Audit増分0、publication0、provider要求／受理0。CSRF token等は正当とし別の拒否原因で代用しない |
| R2-05／E03scope | EXPENSE:APPROVEを持ち、対象部門scope外の主体から承認要求 | 既存の対象非露出・scope拒否契約を維持。その他の不成立・副作用なしをR2-04と同様に照合 |
| R2-06／E03version | 正当な主体から古いexpectedVersionで承認要求 | 既存の競合拒否契約、状態・version保持、成功承認Audit増分0、publication0、provider要求／受理0 |
| R2-07／E03同期veto | SUBMITTED expenseがある部門を正当なMASTER:ADMIN主体が無効化 | 同期veto拒否、部門active／version保持、expense保持、成功部門無効化Audit増分0。expense承認eventのpublication・provider要求／受理0。MASTER:ADMINのfixture登録だけを追加しFramework Identityは変更しない |
| R2-08／対照E01 | 故障注入なしの正当な承認1件 | APPROVED・成功承認Audit・対象event／key／payload・独立stub受理1・publication COMPLETEDを照合。新しいhelper／観測系の正常対照 |

件数は新規8 invocation。権限・scope・versionは別の拒否点なのでまとめて一つの成功にしない。未認証・CSRF不正・全HTTPエラー・全provider応答codeの網羅は初回集合へ含めず、既存Security結果を保持する。追加の必要性がsource変更・実測から判明した枝だけ別判断する。

### 故障注入と観測の成立条件

- triggerはmigration後、対象request確定後、承認操作前にToolingの管理接続で作成する。対象table・INSERT・resource／eventを限定し、SQLSTATE／安全な識別markerで実際の注入到達を記録する。transaction内の注入台帳はrollbackされうるため到達証拠にしない。自作のmigrationやproduction失敗switchへ昇格させない。
- provider modeは受理前の明示503だけ。受理後503・切断・timeoutは結果不明を含むためR3へ残す。到着記録と受理台帳を分け、受理0だけから未送信と断定しない。
- DB観測はSELECT専用role・対象request／event／listenerで絞る。検証setup・注入用管理接続と分離する。成功Auditをresource・action・actor・resultで照合し、他のAuditを消さない。
- 拒否・rollbackは応答／transaction終了後に対象publication不在、到着要求0、受理0を照合する。最大5秒の有限観測窓を記録し、瞬間count0や固定sleepだけを全期間の非送信保証にしない。子process正常終了後にも今回のstub到着・受理を最終照合する。故障注入未到達やprovider観測不能はBLOCKED。
- R2-01のlistener失敗待機は最大30秒。単にCOMPLETEDでないことだけを成功にせず、失敗点・対象publication・library状態・logを突合する。失敗logは秘密を除去し、payload全文やtokenを保存しない。

## 5 選択回帰とbuild案

R1の通常root300実行＋専用92skip、新規9件・既存Critical Journey1件を保存結果として扱う。旧tmp・固定JARは新PCにないため、後続実行用artifactは新sourceで作成・hash固定する。これは新しいartifact epochであり、旧hashや受入記録を差し替えない。

| 集合 | 初回案と実行理由 |
|---|---|
| 新規R2 | 8件を1回。個別結果とfresh XMLを対応付ける |
| 共有stub変更の直接回帰 | 既存provider契約D01／D02の2件を1回。同key同payload再利用・異payload拒否の維持 |
| 共有fixture変更の直接回帰 | 既存C02（async artifact無効保護）とC04（正常2件の対応）の2件を1回。C04はR2-08と成果上の目的・件数を区別 |
| 通常root／C01／専用92件 | Toolingだけの変更なら一律再実行しない。production／設定／artifact作成方式への直接影響が判明した場合、必要集合を訂正案へ提示 |
| C03／C05 | 初回は保存結果を保持。起動selector・starter拒否・V4 location合成を変更する必要が出た場合だけ直接回帰へ追加判断 |

初回の明示実行集合は**新規8＋直接回帰4＝12 invocation**。R2-08と既存C04は新helperの正常対照と既存2件対応契約の回帰であり、二重の機能成果へ加算しない。normal／async packageは必要なcompile・packageだけ、testの二重実行を避ける。`maven.jar.forceCreation=true`とnested依存・main／resource照合によって別artifactを固定する案。全依存／plugin充足は環境棚卸しだけで成立としない。

## 6 有限実行条件の提案と開始順

[新PC環境記録](../architecture/validation/phase4-s1-new-pc-environment-readiness-20261013.md)は棚卸し結果。後続のsource固定・実行preflightを代替しない。旧R1残3558秒・旧runner・PID・container sessionは流用しない。

**新規45分、うちcleanup5分を予約**する提案。文書作成とOwner待ちは含めない。preflight／不足一覧8分、compile・通常／async packageと照合12分、12件の直列検証15分、結果照合5分、cleanup5分。各集合初回1回、追加再実行0回。上限内での未成立は停止・差分提示とし、残時間だけで反復しない。

資源案は同時JVM4以内、PG1、Ryuk1、DB client8。Reference heap768 MiB、pool4／min idle0、executor1 thread／queue16、PG memory1 GiB／CPU1／max_connections16。起動120秒、listener失敗／completion待ち30秒、子process正常終了30秒を上限とし、既存のより短い制限を延長しない。空きmemory8 GiB／disk10 GiB、raw100 MiB／run2 GiB／子log10 MiB以内を開始前と有限sampleで確認する。観測sampleを連続保証とはしない。

1. Ownerが本票の目的・8件＋直接回帰4件・対象path・注入方式・予算／資源・停止条件を判断する。
2. 採用時だけ必要正本・承認記録の限定差分を整合し、文書commitは別承認／Owner操作で固定する。新R2 branch案は`feature/phase4-s1-reference-failure-isolation-r2`。branch／clean source固定の実施範囲も開始判断へ含める。
3. source固定後、JDK／Wrapper・必要依存／plugin・Docker Server／image・既存process／container・資源を確認する。browserを使わない本案では新たなbrowser起動probe／downloadを行わない。不足は有限一覧にし取得範囲を別判断する。
4. 新run・source／artifact／設定identity、所有PID＋creation／container、停止・cleanup方式を準備し、preflight成立後だけtest helperを作成する。今回所有の隔離DB／role／trigger／stub／HTTP認証fixture／package子JVMの操作委任を開始承認へ含める。
5. artifactを固定し、R2-08正常対照→R2負例7件→直接回帰4件を直列実行する。対照失敗時は負例へ進めない。正常対照を後から追加反復しない。
6. case名／fresh XML／sourceとartifact hash／DB・provider・Audit・log・有限資源観測／cleanupを照合し、Owner受入判断へ提出する。tmpのrawをGit成果文書と区別する。

開始前のReviewで、HTTP拒否の具体値、target別trigger SQL、2.1.1の失敗状態・log、stub要求記録、子process所有・異常終了時の有限cleanupを確定する。未成立条件をtest実行で曖昧に埋めない。

## 7 停止条件と出口判断

対象外production修正・依存更新・migration・公開契約変更が必要、注入点／対象／主体が不明、非同期失敗で承認が消える、拒否・rollbackからprovider要求が出る、失敗が不可視、規定資源・時間超過、秘密露出、所有process／container cleanup不成立では停止する。誤った挙動はFAIL、環境・注入・観測不足はBLOCKEDとして記録し、期待値を弱めない。故障DBは原本証拠保全後に今回所有のcontainerとしてcleanupし、既存Javaや他sessionのcontainerを停止しない。

出口はE02／E03の**選択した失敗分離枝**の結果、未実施枝、直接回帰とartifact・cleanupの照合、Owner受入判断。R3開始は別判断。再送・自動復旧・UNKNOWN照合・permit consume／close・保持／purge／実trace・実provider・正式受渡しは未開始を維持する。

**Owner判断欄：承認済み（2026-10-13、§8）。** 以下の承認反映を除き、本票作成時の作業は既存source・文書の読取と整理文書追加だけ。Maven／Docker検証、branch作成、commit／push／PR／mergeは行っていない。

## 8 Owner承認記録と文書固定対象（2026-10-13）

Ownerは本票を示して「確認し、承認いたします」と明示した。これにより、§1〜7のR2失敗分離・Tooling4 file以内／新規1 class・新規8＋直接回帰4 invocation・新規45分（cleanup5分予約）・有限資源／停止条件・今回所有の隔離DB／role／trigger／stub／HTTP認証fixture／package子JVMとcleanupの条件付き作成検証を採用する。設置済みGate P4-Fの限定経路へR2だけを追加し、R3〜R5・DoD全体・正式受渡し・Phase 4全体へ拡張しない。

承認順序は**必要正本・承認記録整合→別承認／Owner操作による文書commit→新R2 branchでclean source固定→preflight成立→限定Tooling作成・検証**。旧R1実行承認・残予算は流用しない。HTTP拒否値・target別trigger SQL・library失敗状態／log・provider要求記録・process所有／異常終了cleanupの具体化は、read-onlyの契約照合と開始preflightで確定し、条件未成立ならtest作成実行を先行しない。sourceや正常対照の失敗から自動再試行しない。

同期承認処理は維持する。承認状態・承認Audit・publication保存は承認transaction内、通知はcommit後の非同期処理である。R2は失敗時の境界を実証し、承認処理を非同期へ置き換えない。

文書固定対象は次の**11 file**。Ownerは後続の会話で「変更分を確認し、この11文書を、次のメッセージでlocal commitしてください」と指示し、§6.2の文書commit操作を承認した。固定メッセージは`docs: approve minimal Reference R2 failure isolation start conditions`。commit IDとclean状態は操作後のGit照合で確認する。

| path | 固定内容 |
|---|---|
| `AGENTS.md` | 現行R2の条件付き限定開始、Docker診断・検証対象と停止条件への導線 |
| `docs/agent/skills/koiki-project-overview/SKILL.md` | R1受入／固定済みとR2開始条件 |
| `docs/agent/skills/koiki-business-feature-work/SKILL.md` | 同期承認と非同期通知の境界、Tooling限定故障注入 |
| `docs/architecture/adr/ADR-051-reference-notification-async-r1.md` | R1受入を保持したR2限定実証の追記 |
| `docs/development/KOIKI-JavaWeb-FW_Phase4_P4-F限定開始Gate提案_v0.1.md` | 設置済みGateへのR2条件付き追加限定判断 |
| `docs/development/KOIKI-JavaWeb-FW_Phase4_PL2_P4-F判定資料_v0.1.md` | R2実証範囲と未成立境界 |
| `docs/development/KOIKI-JavaWeb-FW_Phase4実施計画_見直し草案_v0.1.md` | R1受入後のR2限定例外と全体未承認の維持 |
| `docs/development/KOIKI-JavaWeb-FW_Pre-Phase4_Adoption_Readiness計画_v0.1.md` | R2と実案件受入／正式受渡しの分離 |
| `docs/development/phase4-s1-purpose-dod-stage-mapping-20261009.md` | 現行R1受入・R2条件付き開始と残DoD |
| `docs/development/phase4-s1-r2-purpose-and-minimal-verification-owner-review-20261013.md` | 本票の有限契約・Owner承認・固定対象 |
| `docs/architecture/validation/phase4-s1-new-pc-environment-readiness-20261013.md` | 新PCの棚卸し結果。R2 preflightと区別 |

code／test／POM／SQL migration、R1履歴・引継ぎ、旧tmp／raw／JARはこの文書commit対象に含めない。`.agents/skills`・`.claude/skills`の薄い導線も変更不要。実行環境の権限付き承認手順を維持する。今回承認は成果commit・remote push／PR／merge、追加再実行・不足artifact取得やproduction修正の別判断を代替しない。
