# KOIKI-JavaWeb-FW Phase 4 PL2 — P4-F判定資料 v0.1

**状態:** WORKING DRAFT / F-1〜F-3記入済み、F-4の見積境界とF-5改訂差分案を作成。Gate P4-Fの設置・通過、Phase 4 production実装、正式配布を承認する資料ではない。

**入力:** [Phase 4見直し草案](KOIKI-JavaWeb-FW_Phase4実施計画_見直し草案_v0.1.md)、
[P4-F提案](KOIKI-JavaWeb-FW_Phase4_P4-F限定開始Gate提案_v0.1.md)、
[Level 1 / Level 2の業務向け説明資料](phase4-pl2-level1-level2-business-guide.md)、
[PL2非配布検証](../architecture/validation/phase4-pl2-level2-verification.md)、
[Phase 0見積§7](../architecture/KOIKI-JavaWeb-FW_Phase_Estimate_Feasibility_v0.1.md#7-phase-4--enterprise-integration)。
実案件の確定入力はNext.js/BFFとKOIKI REST連携のみ。外部IdP SSOは見込みであり、本資料のA1 / A2 / D1の見積・設計条件へ含めない。

## 現在地と判断の順序（2026-09-27）

**現在はPhase 4 production開始前のP4-PL2計画・検証段階。ここでGate P4-Fの承認を求める状態ではない。**
V1〜V5は非配布Toolingの検証であり、Level 2を正式実装した結果ではない。

| 段階 | 状態 | 次へ進む条件 |
|---|---|---|
| PL2-V1〜V5とV2復旧runbook案 | 完了。技術的にできることと残る競合をEvidence化 | DoD PASSや運用方式の採用とは区別する |
| F-1〜F-3 | 判定資料の草案を記入済み。source境界、S0 / S1 / S2比較、DoD実演計画 | 採用規模と実案件・運用入力を照合する |
| **F-4（現在の作業）** | Framework / Reference / Toolingの限定作業と案件固有の実装・運用を分離する見積境界案を記入。前者の対象・上限・実施Ownerは未決定 | 限定作業だけを見積・reviewする。案件ごとの総額はP4-Fの入力にしない案をOwnerへ提出 |
| F-5とPhase 4全体PL2台帳 | [全件台帳・Gate改訂差分案](phase4-pl2-f5-integration-and-gate-delta-draft.md)を作成。F-4境界を反映する改訂案、採否は未承認 | 限定Gateと実案件受入Gateの責務を分けてOwner reviewへ出す |
| Gate P4-F / A1 blocking review / production | いずれも未実施・未承認 | 限定範囲のOwner・工数・安全条件を揃え、各判断を別々に行う |

**今必要なOwner承認はない。** F-5のreview入力は作成済みで、CP-F0の業務要件とF-4の
限定作業のOwner・上限が残る。採用規模を早めに絞るなら、
現時点の計画上の暫定基準はS0（Level 1維持）とし、具体的な通知需要・運用体制が示された時に
S1を評価する。S2を推す独立利用先は確認できていない。この暫定基準はDoD変更・Level 2見送りの承認ではない。
CP-F0では[業務向け説明資料](phase4-pl2-level1-level2-business-guide.md)に沿って、
処理を分離する業務上の理由と障害後の配信要件を先に確認する。
[F-5の判断材料](phase4-pl2-f5-integration-and-gate-delta-draft.md)に
既知・未取得・暫定提案を整理した。Toolingの成功を採用根拠にはしない。

次にOwnerへ判断を求める順序は、(1) CP-F0でS0 / S1 / S2、Referenceでの対象event・復旧条件と
DoDへの影響、(2) F-4の限定作業とF-5のGate改訂案を確認した上でP4-Fの採否、
(3) 採用する場合もA1のschema・依存・Rules / Public API・復旧安全性のblocking review、である。
S0により当初DoDを延期・変更する場合も、(1)で明示的なOwner判断を記録する。

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
| S1 用途限定 | Reference `notification`の明示されたevent / listenerだけを対象とし、Application所有構成を第一候補とする。通常instanceの起動時自動再公開は無効。滞留検知と認可・Audit付きの1件ずつの手動復旧を検討 | 手動復旧までの遅延を許容する案。4-2の再起動後配信と4-3の二重送信防止はpackage済みReferenceで実演できた場合だけ判定。実案件の採用には別の送信先・運用判断が必要 | Reference stubで冪等境界・停止条件・再送入口を実演し、正式通知の利用前に各Applicationがprovider・運用Owner・許容復旧時間を確定する |
| S2 共通基盤 | 複数Consumerへ共通契約を提供する範囲をreviewし、必要ならFramework Starter / migration / Rulesを設計する。復旧自動化は別の運用要件として判断 | 当初DoDを狙う範囲が広がり、互換性・配布・support費用が増える。Tooling検証だけでは全面運用を保証しない | 独立した利用先、共通化の必要性、運用Owner、schema / Public API review、同時再送と外部副作用の安全条件 |

**PL2時点の判定案:** S2の採用根拠は未取得。S0を現行Gate維持の選択肢として残し、
Level 2が必要ならS1のReference実証条件を先に確認する。実案件のprovider契約や当番体制を
Framework側で推定せず、当該Applicationの採用判断まで保留する。Referenceでも停止確認・
冪等境界・実演担当が成立しなければ、P4-Fは`REWORK`または`REJECT`候補とする。
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

**2026-10-06の限定範囲:** [正本改訂承認・反映記録](phase4-s1-reference-foundation-canonical-delta-and-start-conditions-20261006.md#9-正本改訂のowner承認反映記録2026-10-06)に基づき、
初回は[正式開始票](phase4-s1-reference-foundation-formal-start-review-20261006.md)のReference保存・認可・Audit基盤に限定する。
notificationの初回はRICH／JPA SHAREDを採用済み。以下のTier 1通知案は当時の候補として保持し、
後段通知追加時にmodule全体のTierを再照合する。許可・消費はV4別location／既存kkref履歴の成立確認付き第一案、
publication migrationは後段未採用事項として分離する。Gate設置／限定判定・実行開始は未成立。

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
| 4-3 | 同一eventを二度配送し、さらに送信受理直後の強制停止から再送 | Reference stub側受理が一意keyで1件となる証拠、通知logとの一致。実案件providerの保証へ転用しない。実案件への適用はprovider契約が得られるまでBLOCKED |
| 4-4 | listener失敗とstale滞留を作り、FAILED件数・滞留起点・alert・運用者による明示再送を確認 | DB状態とmetric / alert時刻、試行回数、認可・Audit、上限到達時の停止を照合。運用者不在またはalertが届かなければBLOCKED |
| 4-5 | completedとFAILED / 未処理を混在させ、保持期限前後で単一実行パージを起動。競合する2起動も試す | 対象のCOMPLETEDだけを削除し、FAILED / 未処理は残す。保持と実行Owner、lock喪失時の結果を記録 |
| 4-12 | 初回HTTP承認、非同期listener、失敗、別processの手動再送を1件のeventで追う。無関係な次requestも実行 | event / publication / retry / job IDとtrace / logの関連、個人情報非露出、相関値の非漏えいを観測sinkで照合。MDCだけのTooling結果から実trace PASSとしない |

4-2の「再起動後に配信」を手動復旧で満たせるか、許容復旧時間とともにOwnerが解釈を決める。
単一JVMの再起動成功だけでは複数instanceの安全性を示さない。`PUBLISHED` / `PROCESSING`の
停止確認、provider受理不明、権限・Audit失敗のいずれかで復旧を止める手順も実演する。
正式実演はGateとA1 blocking review後のproduction候補を対象とし、各DoDのPASSを
fixtureのPASSから自動判定しない。P4-Fはこの実演計画と限定開始を審査するGateであり、
実案件provider・運用費の確定や正式DoD PASSを先取りしない。

## 3. F-4 暫定工数とcommit point

**F-4の区切り案（Owner未判定）:** P4-FではFramework / Reference / Toolingが所有する
限定作業の対象、概算範囲または上限、実施Owner、検証出口を示す。実案件ごとに変わる
provider契約、通知量、当番・SLA、Customer CI / platform、継続運用費まで足した
「Level 2導入総額」はP4-Fの必須入力としない。各Applicationの採用判断で別途確認する。
この区切りは技術安全条件を緩めない。停止確認、冪等境界、失敗時の停止条件は
Reference実演と契約で示し、実providerの保証がなければ当該Applicationの通知を有効化しない。

| Ownership | P4-Fで扱う責務 | 各Applicationの採用へ残す責務 |
|---|---|---|
| Framework / Tooling | Spring Modulith標準の使い方、Rule / migration境界、重複・停止・再送の検証条件、失敗時に止める契約 | provider固有retry、通知量、当番、alert通知先、実環境の費用をFramework設定へ固定しない |
| Reference | stubを用いた`notification`の業務例とpackage済み故障・復旧実演 | Reference stubの結果を実案件providerの保証へ転用しない |
| Customer Application / 運用 | P4-Fでは要件入力待ちとして明示 | 自身のprovider冪等性・送信結果照会、SLA、監視・復旧担当、schema / platform / CIの適用と費用を利用前に判断 |

Phase 0のDoD 4-1〜4-5・4-12の**既存47〜80標準人日**を、重複計上しないために以下へ仮配賦する。
これは限定作業を見積もる起点であり、実施予算や実案件の導入総額ではない。
P4-Fに含めるPhase共通作業だけを別途積み、CI workflow変更は個別reviewへ分ける。

| Package | 既存DoD見積の仮配賦（標準人日） | 暫定commit point / 検証出口 |
|---|---:|---|
| A1 | 21〜37：4-2全額10〜18、4-5全額5〜8、4-4の再送運用4〜7、4-12のevent相関2〜4 | CP-F1: store / migration / Rule reviewを先に通す。CP-F2: Framework契約とTooling復旧証拠。productionの対象module・回帰コマンドはreviewで確定 |
| A2 | 18〜30：4-1全額10〜16、4-3全額8〜14 | CP-F3: A1契約後にReference通知、stubでの冪等境界とpackage済み実演。実案件providerの採用可否は別判断 |
| D1 | 8〜13：4-4のmetric / alert4〜7、4-12のtrace / log4〜6 | CP-F4: A1と並行設計。CP-F5: A1・A2・D1の統合実演で4-2 / 4-4 / 4-5 / 4-12を判定 |
| 合計 | **47〜80**（Phase 0の同じ範囲の再配列） | P4-Fの新しい総額ではない |

module別の再見積では、次の行に設計・実装・test・実演・文書を分けて記入する。
同じ作業を上表のA1 / A2 / D1とPhase共通へ二重に入れない。

| 対象module / Ownership | S1での作業候補 | 現在の数値上の扱い |
|---|---|---|
| `koiki-reference-app` / Reference | Application所有のJDBC publication構成、`expense` event、Tier 1 `notification`・通知log・provider Adapter、package済み実演 | A1 21〜37のうちApplication構成分とA2 18〜30を分離して再見積。module単独額は未算定 |
| `build-support/phase4-level2-verification` / Tooling | 故障・再送・重複・観測の非配布fixture。正式実演の故障注入は別Toolingとして設計 | A1 / A2 / D1のtest・実演分を割り出す。既存fixtureの再実装を自動計上しない |
| `koiki-archunit-rules` / Framework | Level 2選択時のRule 28 / 29候補と負例。既存Level 0 / 1の意味は維持 | A1またはPhase共通のどちらに計上するかreviewで一つに決める。正式変更は未承認 |
| `koiki-starter-data` / Framework | S1は既存二階層Flyway契約の利用を第一候補とする。S2で共通publication migrationが必要なら変更候補 | S1の追加額は未算定。S2のStarter / migration費は47〜80に含まれると推定しない |
| `koiki-starter-observability` / Framework | S1は既存request相関の再利用可否を検証。S2で共通metric / trace契約が必要なら変更候補 | D1 8〜13と共通化費用を分離して再見積。正式変更は未承認 |
| 検証環境 / Tooling | PostgreSQL・別process・stub・sinkでReference実演を再現。workflow変更は別review | P4-F範囲の準備作業をTooling見積へ含める。実案件のCI / platform・当番費は含めない |

### 3.1 採用規模による見積の読み替え

| 案 | 47〜80標準人日の扱い | 別途必要な工数・費用 | P4-Fで判断すること |
|---|---|---|---|
| S0 見送り | 実施予算へ転用しない | DoD・依存packageの再計画、再評価時期の合意 | 現行DoDの延期・変更とP4-F不採用または再提案 |
| S1 用途限定 | 当初DoD 4-1〜4-5・4-12を狙う限定作業の起点として保持。手動復旧でも4-2 / 4-3のReference実演を省略しない | Reference / Toolingの実演準備と汎用契約のreview。実案件のprovider・当番・platform費は別判断 | 対象module、担当、概算範囲または上限、Referenceでの安全条件と停止点をreview |
| S2 共通基盤 | 同じ47〜80を上限や予算とみなさない | Starter / migration / Public API互換、独立Consumer検証、Framework supportを追加で見積もる | 独立利用先の需要とS1との差額を提示して別review |

Phase 0のPhase共通25〜40標準人日にはSAML、Storage、ECSなども含むため、全額をP4-Fへ
配賦しない。次の表でP4-F限定作業と各Application採用時の費用を分ける。
未取得を0や「安全性確認済み」と解釈しない。

| 見積の区分 | 算定・確認する範囲 | 判断時点とOwner候補 |
|---|---|---|
| P4-F限定作業 A1 | publication方式・schema境界・復旧入口の契約、Rule 28 / 29、Tooling検証・文書を既存21〜37標準人日から分解 | Gate前：Architecture / Framework / Toolingが対象・概算範囲または上限と実施Ownerを示す |
| P4-F限定作業 A2 | `koiki-reference-app`のevent・通知log・stub Adapterとpackage済み実演を既存18〜30標準人日から分解 | Gate前：Reference / Toolingが対象・概算範囲または上限と実施Ownerを示す |
| P4-F限定作業 D1 | event相関、FAILED metricとReferenceの観測実演を既存8〜13標準人日から分解 | Gate前：Framework / Reference / Toolingが対象・概算範囲または上限と実施Ownerを示す |
| P4-Fに配賦するPhase共通 | Modulith採用review、二階層migration、ADR / Skill、runbookのうちA1〜D1と重ならない作業 | Gate前：Architecture Ownerが対象と概算範囲または上限を示す |
| 実案件の導入・継続費 | provider契約、通知量、SLA、当番・代行、incident、DB / log容量、Customer CI / platform | 各Application採用前：Customer / 運用 / provider Ownerが見積・受入。P4-F総額へ加えない |
| Framework CI変更・正式配布費 | required check、runner、workflow、artifact repository、supportの追加費 | 該当変更を提案する別review：CI / release Owner。P4-Fで未承認の変更を先行しない |

### 3.2 見積の根拠とGateごとの判断材料

| 入力 | 現在の根拠 | P4-F / 後続の扱い |
|---|---|---|
| 限定作業の起点 | [Phase 0見積§7](../architecture/KOIKI-JavaWeb-FW_Phase_Estimate_Feasibility_v0.1.md#7-phase-4--enterprise-integration)の4-1〜4-5・4-12＝47〜80標準人日。上表のA1 / A2 / D1は同じ範囲の仮配賦 | P4-FではS1またはS2の対象・実施Owner・概算範囲または上限を示す。実施予算への読み替えはOwner判断 |
| Phase共通 | 同§7の25〜40標準人日はSAML / Storage / Container等も含むPhase全体の概算 | P4-Fに含めるレビュー・migration / Rules・運用契約文書だけを抽出し、A1〜D1と二重計上しない |
| 検証環境 | [現行CI](../../.github/workflows/ci.yml)にPL2 fixtureは組み込まれていない。ToolingはPostgreSQL・別process・stubで非配布検証済み | P4-FにはReference実演に必要な環境・Evidence手順を示す。workflowやrequired check追加を選ぶ場合だけ、CI Ownerが別reviewで実測時間・費用を示す |
| provider・運用 | fixtureは冪等keyの効果と復旧の停止条件を実証。実provider保証、通知量、当番、SLAは未取得 | P4-FではConsumerが満たすべき冪等・監視・停止条件を定義。実際のprovider契約と継続費は各Applicationの採用前に担当Ownerが判断 |
| Owner稼働・外部待ち | 標準人日はAI支援時のreview時間や実チーム待ち日数と同一ではない | P4-F限定作業の実施Owner・review責任を明示。実案件の待ち日数はP4-AR6 / Customer計画で扱う |

**P4-Fの提出条件案:** 選んだ限定範囲について対象module、作業別概算範囲または上限、
実施Owner、検証出口、停止点を示す。案件ごとに変動する導入総額・継続費は記入不要とし、
利用前に各Applicationが埋める条件を別表で明示する。上表の47〜80を無条件の予算とせず、
限定作業にも上限やOwnerが付かない場合はP4-Fを`REWORK`候補とする。

### 3.3 commit point、停止点と戻し方

| 点 | 対象・Evidence | 停止条件と戻し方の案 |
|---|---|---|
| CP-F0 採用規模 | D1／D2のS1経路方針採用を記録し、正式対象・Gate判定と分ける。Reference対象event、DoD影響、Owner・復旧条件は後段も追跡 | 限定範囲が曖昧ならREWORK。初回基盤の了承をS1全体開始・実案件需要確定にしない。S0／S2再評価と対象外条件は維持 |
| CP-F1 A1 blocking review | 初回保存基盤部分はP4-F提案§3のモデル／登録／DDL・grant／認可・Audit／検証上限を前置。store／Rules／依存／複数instance復旧安全条件は当該後段接続前に審査 | 初回部分の成立をCP-F1全体完了にしない。未成立の接続を開始せず、既存Level 1構成を維持。未承認migration／APIを作らない |
| CP-F2 A1検証 | Toolingと正式候補を分け、初回保存基盤・認可／Audit・既存Reference保護の部分Evidenceを固定。故障・復旧・パージ・相関は後段で検証 | 初回部分だけでCP-F2全体完了にしない。同時再送・lock喪失・受理不明が未解決なら後続A2を開始しない。schema／cleanupのreviewを維持 |
| CP-F3 A2実証 | Reference `notification`をpackageし、stubの冪等key・送信受理不明・再送を実演 | Reference実演が成立しなければ通知候補を有効化しない。実案件providerの契約・費用は当該Applicationの採用前に判断 |
| CP-F4 D1観測 | metric / alert / trace / logをReference検証sinkで照合し、cardinalityと非露出を確認 | 検証sinkで相関不能なら次へ進めない。実運用sink・alert体制は各Application採用前に判断 |
| CP-F5 統合判定 | F-3のReference 4-1〜4-5・4-12実演、runbookの利用条件、未達と適用外の記録 | 未達DoDは未達として次Gateへ渡す。実案件CI費用、provider、当番体制を確定済みと扱わない |

正式DB migrationが実行済みの場合の「rollback」は履歴やpublicationを手で削除する意味ではない。
forward migration、機能停止、残存publicationの保全と再開条件をA1で設計する。
現時点では限定作業の再見積と実施Ownerが未確定であり、Gate P4-Fはまだ開催できない。
実案件のprovider契約・運用費・CI単価はP4-Fの必須見積から外す提案とする。
それらを不要とする意味ではなく、各ApplicationがLevel 2を採用する前の停止条件とする。

## 4. F-5 Gate関係と停止点

**正本整合の反映（2026-10-06）:** 初回限定範囲・前置／後段区分をP4-AR計画／AGENTS／P4-F提案／
見直し草案へ反映した。詳細と現在の未成立条件は[改訂承認記録§9](phase4-s1-reference-foundation-canonical-delta-and-start-conditions-20261006.md#9-正本改訂のowner承認反映記録2026-10-06)を参照。
新しいCP番号は作らず、CP-F3〜5の出口・停止点は維持する。

[F-5全件台帳・Gate改訂差分案](phase4-pl2-f5-integration-and-gate-delta-draft.md)に
P4-01〜11・optionalのDoD / P4-F範囲 / 待ち条件と、P4-AR計画・`AGENTS.md`・
Phase 4実施計画の提案文案を記載した。これはreview資料であり、現行規定の改訂ではない。

P4-Fを採用する場合でも、P4-AR6実チーム受入、AR-D10責任分担、Gate P4-ARは未完了のまま残す。
現行[P4-AR計画](KOIKI-JavaWeb-FW_Pre-Phase4_Adoption_Readiness計画_v0.1.md)と`AGENTS.md`の
「Gate P4-AR後にPhase 4開始を別判断」という規定に、Framework限定の例外Gateを追加する改訂が必要である。
上記は従来の改訂前の課題として保持する。2026-10-06に初回限定条件の正本改訂本文をOwner承認・反映済み。
Gate設置／限定判定は別途未成立であり、改訂の実施とGate判定を分ける。

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
F-2のS0 / S1 / S2比較、F-3のDoD実演手順、F-4の見積境界と停止点、F-5の全件台帳と
Gate改訂差分案を記入した。限定作業の概算範囲・実施Ownerは未確定であり、P4-F提出完了とはしない。
PL1の実案件入力待ちは並行して管理する。
V4開始時点の履歴は[PL2継続作業・V4開始引継ぎ](phase4-pl2-v4-start-handoff-20260927.md)に残す。
現在の作業順と待ち条件は以下の台帳を使う。

| 順 | 状態・継続タスク | Evidenceと区切り | 判断・待ち条件 |
|---|---|---|---|
| 済 | **V1 / V2：復旧と排他のTooling検証** | `PUBLISHED`停止窓、guarded再送・lock喪失のfixtureと記録を`96796e9`で固定 | 停止確認の真正性、競合窓、外部送信fencingは残る。production成果物への昇格ではない |
| 済 | **V5：storeと二階層migrationのTooling比較** | 両storeの機能・dependency差、KOIKI / Application所有の両配置、KOIKI独立upgrade、失敗DDL rollbackを[検証記録§3.3](../architecture/validation/phase4-pl2-level2-verification.md#33-v5storeと二階層migrationの比較)へ記載し、`00f5c29`で固定。JDBC＋UPDATEをreview第一候補とした | 正式schema所有者、DB方言、成功済みmigrationのrollback、性能・運用差はA1判断へ残す |
| 済 | **V4：Rule 28 / 29の負例** | ToolingのArchUnit 4件がPASSし、JDBC / JPA各profileの全体`verify`もSurefire 11件・Failsafe 11件ずつPASS。[検証記録§4.1](../architecture/validation/phase4-pl2-level2-verification.md#41-v4非配布archunit-fixtureの実行結果2026-09-27)にLevel選択、Rule 1重複、間接I/O経路の限界を記録 | 現行`businessModuleRules(String)`、正式Rules / Public APIは変更していない。間接経路はreview / 動作試験へ割り当て、正式案はA1 blocking reviewへ渡す |
| 済 | **V3：D1観測契約の検証** | [検証記録§3.4](../architecture/validation/phase4-pl2-level2-verification.md#34-v3failed滞留と非同期相関のtooling検証)でpublication年齢とFAILED遷移滞留、初回event・job再送・別requestのMDC logを比較。JDBC / JPA各profileの全体`verify`でSurefire 12件・Failsafe 11件ずつPASS | 実tracer / exporterと別JVM相関、metric起点の正式契約、alert sink、運用Owner、個人情報・cardinalityはD1 / A1 reviewへ残す |
| 済 | **V2残件：復旧runbook案** | [runbook案](phase4-pl2-publication-recovery-runbook-draft.md)に停止確認の発行元・process識別・有効期限、複数運用者、試行上限通知、認可・Audit、lock喪失検知前の窓、provider冪等性 / fencingを「確認方法・停止条件・Owner候補」で整理 | A1 reviewへの入力。fixtureの確認ファイルは停止の真偽を証明せず、運用方式・provider契約は未決定。production手順として採用しない |
| 作業中 | **F-1〜F-4：P4-F判定資料の完成** | F-1のlocal source、F-2の3案比較、F-3のDoD実演計画、F-4のFramework / Reference / Tooling限定見積境界とcommit / stop pointを記入 | CP-F0の業務要件、限定作業の上限・実施Owner、schema所有・再公開安全条件は未決定。実案件のprovider・当番・CI費は各Application採用へ分離する案。現時点のP4-F提出前評価は`REWORK`候補 |
| review入力作成済 | **Phase 4全体のPL2台帳とF-5** | [全件台帳・Gate改訂差分案](phase4-pl2-f5-integration-and-gate-delta-draft.md)にP4-01〜11・optional、当初DoD、P4-F対象外、Gate改訂文案、CP-F0の業務要件・暫定S0基準を記録 | S0時のC1 / Batch依存を明示。P4-B1の4-8 / 4-9とCustomer主導B2を分離。現行Gate規定はOwner判断まで変更しない |
| 最後 | **P4-FのOwner判断** | F-1〜F-5を見て限定開始の採否、対象commit point、停止条件を判断する | Gate P4-Fは未設置・未通過。A1 blocking reviewとproduction開始の承認は別途必要 |

V4 / V3の検証結果は各々の小さなコミットで固定し、V2 runbookとF-1〜F-5の資料改訂は
判断単位で区切る。コミットはGateや正式Rules、migrationの採用を意味しない。

[PL1差分台帳](KOIKI-JavaWeb-FW_Phase4_PL1_REST利用境界差分台帳_v0.1.md#3-p4-ar6へ渡す確認事項)の
Q1〜Q5とP4-AR6実チーム受入は入力待ちとして並行管理する。Next.js/BFF＋REST以外の案件要件、
外部IdP SSOの確定、P4-B2 / E1のEvidence・正式受渡しをFramework側の推測で埋めない。
PL2-V1のpackage済みReferenceでの正式実演はproduction Gate後に扱う。
