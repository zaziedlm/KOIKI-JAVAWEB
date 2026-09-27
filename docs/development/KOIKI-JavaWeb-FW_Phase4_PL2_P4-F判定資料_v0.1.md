# KOIKI-JavaWeb-FW Phase 4 PL2 — P4-F判定資料 v0.1

**状態:** WORKING DRAFT / F-1〜F-3記入済み、F-4の費用・Owner入力待ち。Gate P4-Fの設置・通過、Phase 4 production実装、正式配布を承認する資料ではない。

**入力:** [Phase 4見直し草案](KOIKI-JavaWeb-FW_Phase4実施計画_見直し草案_v0.1.md)、
[P4-F提案](KOIKI-JavaWeb-FW_Phase4_P4-F限定開始Gate提案_v0.1.md)、
[PL2非配布検証](../architecture/validation/phase4-pl2-level2-verification.md)、
[Phase 0見積§7](../architecture/KOIKI-JavaWeb-FW_Phase_Estimate_Feasibility_v0.1.md#7-phase-4--enterprise-integration)。
実案件の確定入力はNext.js/BFFとKOIKI REST連携のみ。外部IdP SSOは見込みであり、本資料のA1 / A2 / D1の見積・設計条件へ含めない。

## 0. F-1 source identityとaccepted baseline

2026-09-27のローカル照合では、作業branchは`docs/phase4-execution-plan-review`、照合開始時のHEADは
`b7d1253e0a7f54a0f1beb3bc555c8c65c6409e1a`、ローカル`main`は
`f7ad1410f42a2f838eada66e0ba5eb82fd7455f4`であった。`main`はHEADの祖先で、
作業treeはcleanであった。これはローカルsource identityであり、remoteの最新状態や
P4-F用のproduction開始点を検証した記録ではない。資料改訂後の最終HEADは次のreview時に再記録する。

| 照合する境界 | 確定している入力 | P4-Fへの扱い |
|---|---|---|
| 上位設計・DoD | [グランドデザイン§27.8](../architecture/grand-design/KOIKI-JavaWeb-FW_グランドデザイン_v0.2.md#278-phase-4-enterprise-integrationv04)のLevel 2 `notification`とDoD 4-1〜4-5・4-12。4-2 / 4-3は核心 | 見送り・用途限定でDoDを満たせない場合、完了と扱わず変更案をOwner判断へ出す |
| Phase 3 accepted baseline | 同期`@EventListener`によるcommand整合、Level 2 runtime依存なし。[P3-A4 Evidence](../architecture/validation/phase3-p3-a4-level1-synchronous-event.md)と[現行Rule 28](../../koiki-archunit-rules/src/main/java/org/koikifw/archunit/BusinessModuleRuleSet.java) | 同期veto経路を維持し、非同期通知の導入範囲だけを別reviewする |
| P4-AR Framework側 | [P4-AR1〜AR5の計画・Evidence](KOIKI-JavaWeb-FW_Pre-Phase4_Adoption_Readiness計画_v0.1.md#13-proposed-execution-sequence)、Framework側のAR6準備は完了 | Framework側で再現した結果を実チーム受入と区別する |
| P4-AR実チーム側 | [P4-AR6契約](../architecture/validation/pre-phase4-p4-ar6-actual-project-handoff-contract.md#1-status-and-boundary)は`ACTUAL-TEAM INPUT REQUIRED`。AR-D10、Gate P4-AR、Phase 4開始は未完了 | 通知の需要・provider / 運用Owner・SLA・受渡し責任を推測で埋めない |
| PL2検証 | [Tooling Evidence](../architecture/validation/phase4-pl2-level2-verification.md)のJDBC / JPA、V1〜V5と[復旧runbook案](phase4-pl2-publication-recovery-runbook-draft.md) | 正式Framework / Referenceの実装、package済み実演、DoD PASSへ自動昇格しない |

## 1. F-2 配置・契約の選択肢

### 1.1 Level 2の採用規模を先に選ぶ

復旧機構の詳細を選ぶ前に、通知の必要性と運用負荷から次の3案を比較する。
現行のA1 / A2 / D1構成は検討入力であり、全面採用の既定ではない。

| 案 | 対象と運用 | 現行DoD・計画への影響 | 開始の必要条件 |
|---|---|---|---|
| S0 見送り | Level 1を維持。Level 2 runtime、永続publication、非同期`notification`を導入しない | 4-1〜4-5・4-12は未達のまま。`accounting`の非同期連携を含むP4-A1依存を再計画。グランドデザインとPhase 4 DoDの延期・変更はOwner判断 | 通知と耐久配信が今必要かを実案件・Reference双方で確認し、見送り期間と再評価triggerを決める |
| S1 用途限定 | Reference `notification`の明示されたevent / listenerだけを対象とし、Application所有構成を第一候補とする。通常instanceの起動時自動再公開は無効。滞留検知と認可・Audit付きの1件ずつの手動復旧を検討 | 手動復旧までの遅延を許容する案。4-2の再起動後配信と4-3の二重送信防止はpackage済みReferenceで実演できた場合だけPASS。できなければDoD変更をOwnerへ提出 | provider側冪等性または同等策、運用Owner・対応時間、元process停止確認、安全な再送入口、観測・パージを確定する |
| S2 共通基盤 | 複数Consumerへ共通契約を提供する範囲をreviewし、必要ならFramework Starter / migration / Rulesを設計する。復旧自動化は別の運用要件として判断 | 当初DoDを狙う範囲が広がり、互換性・配布・support費用が増える。Tooling検証だけでは全面運用を保証しない | 独立した利用先、共通化の必要性、運用Owner、schema / Public API review、同時再送と外部副作用の安全条件 |

**PL2時点の判定案:** S2の採用根拠は未取得。S0を現行Gate維持の選択肢として残し、
Level 2が必要ならS1の受入条件を先に確認する。S1でも停止確認・provider冪等性・運用担当が
成立しなければproductionへ進めず、P4-Fは`REWORK`または`REJECT`候補とする。
S0 / S1の採用は、上位設計やDoDの黙示的な変更を意味しない。

### 1.2 A1 / A2 / D1の配置候補

| 候補 | 変更する場所の案 | blocking reviewと未決定事項 |
|---|---|---|
| A1 publication基盤 | 最小案はSpring Modulith標準のJDBC store＋UPDATEを第一候補とし、Applicationが構成する。共通化が必要ならFrameworkの既存Data Starter / migration境界を検討。Tooling fixtureは正式artifactへ移さない | JDBC / JPAは両方とも同一PostgreSQL / JPA業務transactionで復旧、再送、パージが成立。二階層FlywayのKOIKI所有 / Application所有をToolingで確認したが、正式schema所有者は未決定。両方式とも複数JVMの同時再公開で同一listenerへ入った。CP8型lockは専用復旧JVM同士に限定。通常listenerとの競合・lock接続喪失、完了record運用、DB方言と性能、migration所有をA1で判断。Starter / Java Public API追加は別review |
| A2 `notification` Reference | 既存`koiki-reference-app`内の新しい業務packageを候補とし、`expense`承認eventを公開境界として使う。通知内容、log、provider AdapterとstubはReference / Toolingの責任に分ける | event payloadに個人情報を含めない設計、同期vetoと非同期side effectの分離、実providerの冪等key契約。providerが保証しない場合のDoD 4-3解釈をOwner判断 |
| D1 非同期観測 | 既存Observability StarterのServlet `requestId` / `TaskDecorator`は再利用候補。event ID、publication ID、retry / job IDの表現とmetricはA1と同時設計し、業務語彙はApplication側 | HTTP requestを離れた再送・jobでの相関、trace / logの非露出とcardinality、FAILED状態に入ってからの経過時間、運用sink / alert Ownerを決める。exporter既定を先行固定しない |
| Architecture Rules | 現行Rule 28のLevel 0 / 1拒否とTooling限定のLevel 2候補・Rule 29直接依存候補を[ArchUnit fixture](../architecture/validation/phase4-pl2-level2-verification.md#41-v4非配布archunit-fixtureの実行結果2026-09-27)で比較。正式Rulesは未変更 | 既存`businessModuleRules(String)`の意味を変えずLevel指定をどう追加するか、Rule 29とRule 1の重複、間接I/Oをreview / 動作testへ割り当てる契約をA1で判断 |

現行Data StarterはKOIKI migrationを`db/migration/koiki` / `koiki_flyway_history`、Application側migrationを
`db/migration/customer` / `flyway_schema_history`に分ける。既存のLevel 2 fixtureの単一Flyway履歴は
この二階層の受入検証ではない。追加した[V5 Tooling試験](../architecture/validation/phase4-pl2-level2-verification.md#33-v5storeと二階層migrationの比較)は
両所有配置の二階層実行とKOIKI側の独立upgradeを確認したが、Data Starterを用いたA1統合試験ではない。
publication tableをFramework共通契約にする場合はKOIKI側migration・upgrade / rollback・DB方言責任が発生する。
Application配置にする場合は各Consumerのschema準備とversion整合が必要になる。どちらも未選定。

### 1.3 S1 / S2でLevel 2を採用する場合の復旧候補

**A1再公開の条件付き候補（Owner未判定）:** 全通常instanceで起動時自動再公開を無効にする。
再送は認可・Audit付きの専用復旧processに集約し、CP8と同じPostgreSQL session advisory lockで
復旧process同士を排他する。FAILEDは回数上限候補を適用し、PUBLISHED / PROCESSINGは前処理processの
停止確認と対象IDを伴う手動復旧から始める。停止を確認できない対象は再送しない。
自動化には生存中listenerを除外する仕組みかfencingが必要。Toolingでは元processの停止確認と
対象publication ID・観測状態・試行回数の一致を要求する入口、およびlock接続喪失時に専用JVMを停止する候補を検証した。
停止確認の真正性と検知までの競合窓は残るため、provider冪等性を維持し、fencingを別途判断する。これは
[PL2 Evidence§3.1](../architecture/validation/phase4-pl2-level2-verification.md#31-再公開の排他方針候補toolingでの検証)で
lock接続が維持された復旧worker間だけを実証した候補であり、A1 blocking review前にproductionへ採用しない。
[V2残件の復旧runbook案](phase4-pl2-publication-recovery-runbook-draft.md)は、
停止確認・対象照合・複数operator・試行上限・lock喪失・provider受理・認可 / Auditを
「確認方法・停止条件・判断Owner候補」で整理したA1 review入力である。

| 再公開方式 | PL2の評価 | A1判断 |
|---|---|---|
| 全instanceで起動時自動再公開 | 同一publicationのlistenerが2 JVMで同時実行された | 採用しない案 |
| 専用復旧process＋CP8型lock | worker間の競合skip、強制停止後の再取得、完了後の解放をJDBC / JPAで実証。guarded入口の停止確認・状態照合とlock喪失時JVM停止をfixtureで確認。確認が虚偽なら生存中listenerと重複し、lock喪失の検知前も競合窓がある | **条件付き推奨候補**。停止確認の発行・検証、検知前の競合と外部送信のfencing、運用認可を解くまで採用保留 |
| 外部schedulerの単一起動指定だけ | RepositoryのCP8判断ではDB側の競合保証・crash recoveryを満たさない | 単独保証に使わない案 |
| publicationごとのclaim / lease / fencing | 生存中listenerとの競合やlock接続喪失への対策候補 | schema・migration・運用負担を含めて別review。現時点で固定しない |

S1の「手動」は安全性の代替保証ではない。停止確認・対象再照合・provider冪等性が成立しない
`PUBLISHED` / `PROCESSING`は再送を保留する。復旧までの時間、保留件数、対応時間を
運用Ownerが受け入れられるかをF-4で判断する。runbook案の全手順をそのまま採用する場合は、
訓練・権限・証跡・代行者を含む運用コストも計上する。

| F-2配置判断 | S1の第一候補 | S2へ広げる前のblocking review |
|---|---|---|
| Maven依存 | `koiki-reference-app`がSpring ModulithのJDBC event storeをApplication責任で選ぶ案。JPA storeもToolingで動作確認済み | 正式Starterへの依存追加が必要な独立Consumerの実例と、既存BOM / Starterとの差を示す |
| publication対象 | `registry-trigger-annotation`で`@ApplicationModuleListener`だけを永続記録する構成を候補とする。Toolingでは通常`@TransactionalEventListener`の実行は続き、記録対象だけが減ることを確認 | 対象listenerの列挙、設定の配布責任、想定外のtransactional listenerの検出方法をreviewする |
| migration | Application側`db/migration/customer`と`flyway_schema_history`へのpublication表配置を候補とする。Spring側の自動schema初期化との重複を避ける | KOIKI所有`db/migration/koiki`へ移すなら共通schemaのupgrade、DB方言、配布・rollback責任を決める |
| Rules / Public API | 既存Level 0 / 1のRule 28拒否を保つ。Level 2を使うReferenceだけでToolingのRule 28 / 29候補を評価する | `businessModuleRules(String)`の互換性、Rule 1との重複、Level選択契約と間接I/Oの検査限界をreview。Java Public API追加を先行しない |
| Reference module | `expense`が値だけの承認eventを公開し、Tier 1 / JPAの`notification`がevent inbound AdapterからApplication Use Caseへ委譲。通知logとprovider outbound AdapterはReference所有 | 業務payloadの個人情報、provider冪等key、Audit・権限、送信結果不明時の扱いを確認。Customer通知は別Ownership |
| 観測 | event / publication / retry / job IDの相関とFAILED件数・滞留をApplication / Toolingで設計。既存Servlet `requestId`契約を再利用候補とする | Framework Observability Starterへ入れる汎用契約、metric cardinality、trace exporter、alert sinkと運用Ownerを決める |

## 2. F-3 DoD実演とPL2 Evidenceの差

| DoD | 非配布fixtureで確認済み | production実演前に残ること |
|---|---|---|
| 4-1 | 元transactionの承認記録は、非同期listener失敗後も残る | `expense`承認状態・Business AuditとReference通知の結合 |
| 4-2 | PUBLISHEDとPROCESSING中の別JVMを送信前 / 送信受理後で強制停止し、再起動後にCOMPLETED。複数JVMの同時再公開では同一listenerへ入る競合を確認。guarded再送入口とlock喪失時の専用JVM停止候補を確認 | 元processの停止確認の真正性、検知前の競合窓、外部送信fencing、package済みReferenceでの再現 |
| 4-3 | provider stubに一意keyがあれば再配送でも受理1件。なければ2件 | 実providerの冪等契約または同等策、通知logと送信境界の一貫性 |
| 4-4 | FAILED件数・publication年齢gauge、Tooling限定のFAILED遷移時刻gauge、stale monitor、明示再送と試行回数filter | 滞留時間の正式起点とschema所有、alert運用、運用者認可 / Audit、上限到達通知、複数instance競合 |
| 4-5 | completed publicationだけの保持期限パージ | 単一実行基盤とretention / purgeの配備・権限・同時配信競合 |
| 4-12 | 既存Servlet request相関・TaskDecoratorのsourceを確認。Toolingではevent / publication ID、job / retry ID、MDC logと別requestへの非漏えいを実測 | 実OpenTelemetry trace / exporter、別JVM・運用log sinkでの相関と個人情報境界 |

### 2.1 package済みReferenceでの実演手順案

S1 / S2を選ぶ場合は、固定したsource commitからpackageした`koiki-reference-app`、
PostgreSQL、別OS process、通知stubと観測sinkを用いる。障害注入とprovider stubは
Toolingに隔離する。実演ごとにartifact checksum、設定、publication / event ID、
DB / Business Audit / provider受理 / log / metricを同じ操作IDへ結び、実行者と判定者を残す。
S0では以下をPASSにせず、DoD変更または延期のOwner判断を記録する。

| DoD | 実演操作 | 判定する観測と失敗条件 |
|---|---|---|
| 4-1 | `expense`を承認し、通知stubを失敗させる。承認・同期vetoと非同期side effectを別々に実行 | 承認状態とBusiness Auditはcommit済み、通知失敗は別に記録。元transactionがrollbackしたらFAIL |
| 4-2 | publication保存後の`PUBLISHED`、送信前と送信受理直後の`PROCESSING`で別OS processを強制停止。停止証拠と対象IDを採取し、定めた手動復旧入口から再送 | 同一publicationが再起動後にCOMPLETED、停止前後のprovider受理を照合。生存中listener・複数復旧worker・lock喪失の競合を別負例で確認。停止の真正性や重複制御が未成立ならFAILまたはBLOCKED |
| 4-3 | 同一eventを二度配送し、さらに送信受理直後の強制停止から再送 | provider側受理が一意keyで1件となる証拠、Reference通知logとの一致。stubだけで実providerの保証を宣言しない。実providerの契約がなければBLOCKED |
| 4-4 | listener失敗とstale滞留を作り、FAILED件数・滞留起点・alert・運用者による明示再送を確認 | DB状態とmetric / alert時刻、試行回数、認可・Audit、上限到達時の停止を照合。運用者不在またはalertが届かなければBLOCKED |
| 4-5 | completedとFAILED / 未処理を混在させ、保持期限前後で単一実行パージを起動。競合する2起動も試す | 対象のCOMPLETEDだけを削除し、FAILED / 未処理は残す。保持と実行Owner、lock喪失時の結果を記録 |
| 4-12 | 初回HTTP承認、非同期listener、失敗、別processの手動再送を1件のeventで追う。無関係な次requestも実行 | event / publication / retry / job IDとtrace / logの関連、個人情報非露出、相関値の非漏えいを観測sinkで照合。MDCだけのTooling結果から実trace PASSとしない |

4-2の「再起動後に配信」を手動復旧で満たせるか、許容復旧時間とともにOwnerが解釈を決める。
単一JVMの再起動成功だけでは複数instanceの安全性を示さない。`PUBLISHED` / `PROCESSING`の
停止確認、provider受理不明、権限・Audit失敗のいずれかで復旧を止める手順も実演する。
正式実演はGateとA1 blocking review後のproduction候補を対象とし、各DoDのPASSを
fixtureのPASSから自動判定しない。

## 3. F-4 暫定工数とcommit point

Phase 0のDoD 4-1〜4-5・4-12の**既存47〜80標準人日**を、重複計上しないために以下へ仮配賦する。
これは再見積や実施予算の承認ではない。P4-Fに必要なPhase共通作業・CI費用は別途積む。

| Package | 既存DoD見積の仮配賦（標準人日） | 暫定commit point / 検証出口 |
|---|---:|---|
| A1 | 21〜37：4-2全額10〜18、4-5全額5〜8、4-4の再送運用4〜7、4-12のevent相関2〜4 | CP-F1: store / migration / Rule reviewを先に通す。CP-F2: Framework契約とTooling復旧証拠。productionの対象module・回帰コマンドはreviewで確定 |
| A2 | 18〜30：4-1全額10〜16、4-3全額8〜14 | CP-F3: A1契約後にReference通知、冪等境界とpackage済み実演。実provider依存が解けない場合は4-3を保留 |
| D1 | 8〜13：4-4のmetric / alert4〜7、4-12のtrace / log4〜6 | CP-F4: A1と並行設計。CP-F5: A1・A2・D1の統合実演で4-2 / 4-4 / 4-5 / 4-12を判定 |
| 合計 | **47〜80**（Phase 0の同じ範囲の再配列） | P4-Fの新しい総額ではない |

### 3.1 採用規模による見積の読み替え

| 案 | 47〜80標準人日の扱い | 別途必要な工数・費用 | P4-Fで判断すること |
|---|---|---|---|
| S0 見送り | 実施予算へ転用しない | DoD・依存packageの再計画、再評価時期の合意 | 現行DoDの延期・変更とP4-F不採用または再提案 |
| S1 用途限定 | 当初DoD 4-1〜4-5・4-12を狙う過去概算として保持。手動復旧でも4-2 / 4-3の実演を省略しない | 運用者の待機・訓練・認可 / Audit、incident調査、provider照合、package済み実演とCI | 安全条件、許容復旧時間、運用Owner、減額・増額を実測から再見積 |
| S2 共通基盤 | 同じ47〜80を上限や予算とみなさない | Starter / migration / Public API互換、複数Consumer検証、運用共通化・support | 独立利用先の需要とS1との差額を提示して別review |

Phase 0のPhase共通25〜40標準人日にはSAML、Storage、ECSなども含むため、全額をP4-Fへ
配賦しない。次の見積票を埋めてから、既存DoD分47〜80との重複を除いた総額を出す。
数値のない欄を0と解釈しない。

| 費用・工数の入力欄 | 範囲と算定方法 | 判断Owner候補 / 現在値 |
|---|---|---|
| A1の設計・実装・test・実演・文書 | publication store / schema、復旧入口、Rule 28 / 29、単一実行パージを対象module別に分解。既存21〜37標準人日との差分を示す | Architecture / Framework / Tooling：未積算 |
| A2の設計・実装・test・実演・文書 | `koiki-reference-app`のevent・通知log・provider Adapterと実演。既存18〜30標準人日との差分を示す | Reference / provider Owner：未積算 |
| D1の設計・実装・test・実演・文書 | event相関、FAILED metric、trace / log sink、alertを分解。既存8〜13標準人日との差分を示す | Framework / 運用Owner：未積算 |
| Phase共通からのP4-F配賦 | Modulith採用review、二階層migration、ADR / Skill、CI、runbookのうち上記A1〜D1と重ならない作業だけを抽出 | Architecture Owner：未積算 |
| 運用継続費 | 月間publication件数・FAILED率・manual復旧件数、対応時間、代行者、保持期間から人員時間とDB / log容量を算定 | 運用Owner未指名、実績値なし |
| 検証環境 / CI | PostgreSQL、別OS process、mail stub、観測sink、artifact stage、profile別jobの実行時間・保存容量・runner費用を計測 | CI / platform Owner未指名、実行単価・頻度なし |
| AI支援と外部待ち | Owner review / 実演立会いの稼働日を標準人日と分け、provider契約・運用体制・P4-AR6入力の待ち日数を別記 | Architecture / 運用 / provider Owner：未算定 |

### 3.2 commit point、停止点と戻し方

| 点 | 対象・Evidence | 停止条件と戻し方の案 |
|---|---|---|
| CP-F0 採用規模 | S0 / S1 / S2の選択、対象event、DoDへの影響、実案件需要、運用Ownerと許容復旧時間を記録 | 情報不足なら`REWORK`。S0ならLevel 2のproduction開始は行わず、DoD・依存packageの見直しへ戻す |
| CP-F1 A1 blocking review | store、schema所有、Flyway二階層、dependency、Rules / Public API、複数instance復旧の安全条件 | 設計不成立ならA1を開始しない。既存Level 1構成を維持し、未承認migration / APIを作らない |
| CP-F2 A1検証 | Toolingと正式候補を分け、保存・故障・認可・Audit・パージ・相関のEvidenceを固定 | 同時再送・lock喪失・provider受理不明が未解決なら後続A2を開始しない。公開前の候補設定とschemaはOwnerが前進migration / cleanup方法をreview |
| CP-F3 A2実証 | Reference `notification`のpackage済み実演とprovider冪等契約 | 4-1 / 4-3の成立が示せなければ通知を有効化しない。送信済み外部副作用はDB rollbackで取り消さず照合・補償を判断 |
| CP-F4 D1観測 | metric / alert / trace / logと運用sinkの連携、cardinalityと非露出 | alert未接続・相関不能ならLevel 2運用を開始しない。設定とsink側の展開を戻し、証拠を残す |
| CP-F5 統合判定 | F-3の4-1〜4-5・4-12、CI費用、運用訓練・runbook、完了記録 | 未達DoDは未達としてGateへ戻す。remote・配布・Gate P4-ARを自動実行しない |

正式DB migrationが実行済みの場合の「rollback」は履歴やpublicationを手で削除する意味ではない。
forward migration、機能停止、残存publicationの保全と再開条件をA1で設計する。
現時点では運用Owner・provider契約・実行頻度・CI単価がなく、F-4の確定総額と
AI支援Owner稼働日は算定できない。これらを埋める前にGate P4-Fを開催しない。

## 4. F-5 Gate関係と停止点

P4-Fを採用する場合でも、P4-AR6実チーム受入、AR-D10責任分担、Gate P4-ARは未完了のまま残す。
現行[P4-AR計画](KOIKI-JavaWeb-FW_Pre-Phase4_Adoption_Readiness計画_v0.1.md)と`AGENTS.md`の
「Gate P4-AR後にPhase 4開始を別判断」という規定に、Framework限定の例外Gateを追加する改訂が必要である。
改訂本文はP4-F採用判断と同時にOwner reviewし、現時点で規定を変更しない。

| 改訂候補 | 追加する内容 | 維持する条件 |
|---|---|---|
| P4-AR計画 | Gate P4-ARの前にFramework限定のGate P4-Fを置ける例外と、A1 / A2 / D1の対象commit point・Evidence・停止点を追記 | P4-AR6 / AR-D10 / Gate P4-ARの状態は未完了。正式受渡し・Phase 4全体開始は別判断 |
| `AGENTS.md` | 「OwnerがGate P4-Fを明示承認した対象commit pointに限りFramework共通範囲を開始できる」と追記 | 現在のPhase 3 / P4-AR baseline、Public API等のblocking review、remote個別承認、Customer固有作業の境界を維持 |
| Phase 4実施計画 | P4-F対象と対象外、DoDの暫定Evidence、P4-AR6後の再審査を記録 | 当初DoD 4-1〜4-12の採否・変更をP4-Fから推定しない |

停止点は、A1のstore・migration / dependency・Rules / Public API、A2のprovider冪等性とReference境界、
D1の相関 / 個人情報・運用Owner、各production commit point、remote変更、配布である。
P4-Fで許可する対象・期限・Owner・Evidenceと、停止後のrollback対象を個別に記す必要がある。

## 5. 次のPL2作業

以下は[PL2検証記録§3](../architecture/validation/phase4-pl2-level2-verification.md#3-未実施と次の確認)と
本資料のF-1〜F-5を結ぶ継続台帳である。V1 / V2とV5のTooling結果はDoDの正式PASSやP4-F通過を意味しない。
V4とV3のTooling検証、V2の復旧runbook案を順に完了した。F-1のローカルsource照合、
F-2のS0 / S1 / S2比較、F-3のDoD実演手順、F-4の見積入力欄と停止点を本資料へ記入した。
F-4の数値と実施Ownerは未確定であり、P4-F提出完了とはしない。PL1の実案件入力待ちは並行して管理する。
V4開始時点の履歴は[PL2継続作業・V4開始引継ぎ](phase4-pl2-v4-start-handoff-20260927.md)に残す。
現在の作業順と待ち条件は以下の台帳を使う。

| 順 | 状態・継続タスク | Evidenceと区切り | 判断・待ち条件 |
|---|---|---|---|
| 済 | **V1 / V2：復旧と排他のTooling検証** | `PUBLISHED`停止窓、guarded再送・lock喪失のfixtureと記録を`96796e9`で固定 | 停止確認の真正性、競合窓、外部送信fencingは残る。production成果物への昇格ではない |
| 済 | **V5：storeと二階層migrationのTooling比較** | 両storeの機能・dependency差、KOIKI / Application所有の両配置、KOIKI独立upgrade、失敗DDL rollbackを[検証記録§3.3](../architecture/validation/phase4-pl2-level2-verification.md#33-v5storeと二階層migrationの比較)へ記載し、`00f5c29`で固定。JDBC＋UPDATEをreview第一候補とした | 正式schema所有者、DB方言、成功済みmigrationのrollback、性能・運用差はA1判断へ残す |
| 済 | **V4：Rule 28 / 29の負例** | ToolingのArchUnit 4件がPASSし、JDBC / JPA各profileの全体`verify`もSurefire 11件・Failsafe 11件ずつPASS。[検証記録§4.1](../architecture/validation/phase4-pl2-level2-verification.md#41-v4非配布archunit-fixtureの実行結果2026-09-27)にLevel選択、Rule 1重複、間接I/O経路の限界を記録 | 現行`businessModuleRules(String)`、正式Rules / Public APIは変更していない。間接経路はreview / 動作試験へ割り当て、正式案はA1 blocking reviewへ渡す |
| 済 | **V3：D1観測契約の検証** | [検証記録§3.4](../architecture/validation/phase4-pl2-level2-verification.md#34-v3failed滞留と非同期相関のtooling検証)でpublication年齢とFAILED遷移滞留、初回event・job再送・別requestのMDC logを比較。JDBC / JPA各profileの全体`verify`でSurefire 12件・Failsafe 11件ずつPASS | 実tracer / exporterと別JVM相関、metric起点の正式契約、alert sink、運用Owner、個人情報・cardinalityはD1 / A1 reviewへ残す |
| 済 | **V2残件：復旧runbook案** | [runbook案](phase4-pl2-publication-recovery-runbook-draft.md)に停止確認の発行元・process識別・有効期限、複数運用者、試行上限通知、認可・Audit、lock喪失検知前の窓、provider冪等性 / fencingを「確認方法・停止条件・Owner候補」で整理 | A1 reviewへの入力。fixtureの確認ファイルは停止の真偽を証明せず、運用方式・provider契約は未決定。production手順として採用しない |
| **作業中** | **F-1〜F-4：P4-F判定資料の完成** | F-1のローカルsource identityとaccepted baseline、F-2の3案比較と配置候補、F-3のDoD別実演計画、F-4の費用入力票とcommit / stop pointを本資料へ記入 | 運用Owner、provider契約、許容復旧時間、S0 / S1 / S2選定、module別再見積、CI単価が未取得。正式schema所有・再公開安全性も未決定。現時点のP4-F提出前評価は`REWORK`候補 |
| 統合 | **Phase 4全体のPL2台帳とF-5** | P4-01〜11・optionalの採否条件と当初DoDの追跡、P4-F対象外の待ち条件を整理。P4-AR計画・`AGENTS.md`のGate改訂差分を提案として用意し、F-1〜F-5を一組でreviewできる区切りを作る | P4-B1の4-8 / 4-9とCustomer主導P4-03Bを分離。現行Gate規定はOwner判断まで変更しない |
| 最後 | **P4-FのOwner判断** | F-1〜F-5を見て限定開始の採否、対象commit point、停止条件を判断する | Gate P4-Fは未設置・未通過。A1 blocking reviewとproduction開始の承認は別途必要 |

V4 / V3の検証結果は各々の小さなコミットで固定し、V2 runbookとF-1〜F-5の資料改訂は
判断単位で区切る。コミットはGateや正式Rules、migrationの採用を意味しない。

[PL1差分台帳](KOIKI-JavaWeb-FW_Phase4_PL1_REST利用境界差分台帳_v0.1.md#3-p4-ar6へ渡す確認事項)の
Q1〜Q5とP4-AR6実チーム受入は入力待ちとして並行管理する。Next.js/BFF＋REST以外の案件要件、
外部IdP SSOの確定、P4-B2 / E1のEvidence・正式受渡しをFramework側の推測で埋めない。
PL2-V1のpackage済みReferenceでの正式実演はproduction Gate後に扱う。
