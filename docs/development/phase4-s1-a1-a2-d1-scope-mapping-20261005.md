# 作業7：A1 / A2 / D1とS1限定範囲の照合（2026-10-05）

> **現在の参照先（2026-10-05）:** 本書のTier 1第一候補は当初案。現在は許可＋append-only消費の2種類を局所検証し、狭いRICH / JPA共有モデルをTier候補として継続する。正式Tierは未確定。 [最新引継ぎ](phase4-s1-local-verification-start-handoff-20261005.md)／[承認済み方式票](phase4-s1-method-ballot-and-local-verification-contract-20261005.md)。

**状態:** DRAFT / 作業7の第一段階。S1検証完遂を目指す進め方の候補化はOWNER APPROVED。今回の照合は具体的採用・実装開始・Gate承認ではない。
**Ownership:** Framework側の計画文書。成果物の所有者はFramework / Reference / Toolingへ分けて記載する。
**確認baseline:** `docs/daily-development-workflow` / `781a6a2`、着手時の作業treeはclean。ローカル文書・sourceを読み取り照合。remote同期、Maven / Docker実行、新しい動作Evidenceは未取得。
**今回の出口:** A1 / A2 / D1の対象・成果物候補・Ownership・既存材料と残るreview論点の整理。安全方式の確定、実演計画の完成、担当・環境・上限の確定、全package分解とOR提出は後続作業である。

## 1. 入力と維持する決定

- [S1計画方針の決定記録](phase4-s1-completion-direction-decision-20261002.md)：承認範囲とS1検証軸。
- [前回引継ぎ](phase4-s1-next-session-handoff-20261002.md)：作業7の再開点と未承認境界。
- [CP-F0判断材料§2.1](phase4-cpf0-and-s0-replan-options-20261002.md#21-s1の対象を限定する案)：対象限定と成立条件。
- [見直し草案§4.1](KOIKI-JavaWeb-FW_Phase4実施計画_見直し草案_v0.1.md#41-gate後の実行work-package候補)：当初packageの責務。
- [PL2判定資料§1〜3](KOIKI-JavaWeb-FW_Phase4_PL2_P4-F判定資料_v0.1.md)：配置候補、DoD実演案、既存概算とcommit point。
- [PL2 Evidence](../architecture/validation/phase4-pl2-level2-verification.md)、[復旧runbook案](phase4-pl2-publication-recovery-runbook-draft.md)：Toolingの確認範囲と未解決事項。

S1を完遂する進め方の候補化は決定済みであり、S0 / S1 / S2を同じ未決状態へ戻さない。S1の具体的条件を揃え、不成立時のS0への切替と、共通提供を広げるS2はOwnerが別判断する。既存PL2資料の暫定S0は当時の記録として保持する。

## 2. 今回のS1対象案

Referenceの`expense`承認結果を値だけのeventとして公開し、Reference-owned `notification`が非同期に受け、非配布Toolingのprovider stubへ送る。通常の承認・認可・Business Auditと同期veto経路を維持する。

| 境界 | 対象候補と適用条件 |
|---|---|
| 業務起点 | `expense`の承認成功。event発行とpublication保存のtransaction整合、承認失敗時の非配信をreviewする |
| module間 | Entityや他moduleのApplication / Repositoryを渡さない値だけのevent。event名・payload・識別子・個人情報の扱いは未確定 |
| 配信と復旧 | 明示したevent / listenerだけをpublication記録対象とする候補。通常起動時の自動再公開を無効とし、専用復旧processから認可・Audit付き手動再送を行う案は採用保留 |
| 通知先 | 冪等性と受理不明窓を観測できるprovider stub。stub受理から実メール到達・実provider適合は判断しない |
| 実演対象 | 固定sourceからpackageした正式Reference候補と、PostgreSQL・別OS process・stub・観測sinkを組み合わせる案。故障注入はToolingに隔離 |
| 関連package | C3の単一実行基盤とA1パージを照合する。C3 / C4全体やBatch正式依存をS1へ一括包含しない |

## 3. 現行sourceとの照合

以下は`781a6a2`時点のsource確認であり、今回testを実行した結果ではない。

| 確認対象 | 現行sourceの事実 | S1計画へ渡す差分 |
|---|---|---|
| [ExpenseApplicationService](../../koiki-reference-app/src/main/java/org/koikifw/reference/expense/application/ExpenseApplicationService.java) | `approve`はtransaction・`EXPENSE:APPROVE`権限を持ち、scope / version確認、状態変更、flush、Business Auditを行う。承認event発行は未実装 | A2でevent発行位置とrollback境界をreview。共通mutation処理への変更が他操作へ波及しない範囲を示す |
| [同期listener](../../koiki-reference-app/src/main/java/org/koikifw/reference/expense/adapter/inbound/event/DepartmentDeactivatingListener.java) | masterの部門無効化eventを同期`@EventListener`で受け、expenseのguardへ委譲 | A1のLevel 2導入で既存同期vetoを非同期化しない。回帰対象へ残す |
| [Reference POM](../../koiki-reference-app/pom.xml) / 業務source | POMにLevel 2用Modulith runtimeの直接宣言はなく、業務sourceにnotification / 非同期承認listenerは未実装 | 正式依存・notificationを新規対象としてreview。effective POM / dependency treeは依存候補を具体化する後続段階で確認 |
| [Reference設定](../../koiki-reference-app/src/main/resources/application.properties) / [migration](../../koiki-reference-app/src/main/resources/db/migration/kkref) | Application migrationは`db/migration/kkref`、履歴は`kkref_flyway_history`。既存V1〜V3はmaster / expense / approver scope | PL2配置案の`db/migration/customer` / `flyway_schema_history`との差分をA1へ渡す。Application所有の方針候補から配置名を自動決定せず、既存履歴とFramework二階層契約の整合をreview |
| [BusinessModuleRuleSet](../../koiki-archunit-rules/src/main/java/org/koikifw/archunit/BusinessModuleRuleSet.java) | 現行Rule 28はtransactional listenerを拒否するLevel 0 / 1契約 | Rule 28 / 29候補を正式採用する前に、Level選択・互換性・検査限界をreview。Referenceだけで拒否を迂回しない |
| [Observability auto-configuration](../../koiki-starters/koiki-starter-observability/src/main/java/org/koikifw/starter/observability/internal/KoikiObservabilityAutoConfiguration.java) | `TaskDecorator`はMDCの`requestId`伝播候補を提供。Servlet correlation filterはServlet環境が条件 | 実際のlistener executorへの適用を検証する。永続event / 別process復旧の相関、実OpenTelemetry span / exporterは別途設計・実演が必要 |

## 4. package別の成果物とOwnership

各行は計画候補であり、担当者の指名・正式module変更・共通契約の採用を意味しない。

| Package | S1に含める成果物候補 | 成果物Owner / 配置候補 | 後続・別判断の範囲 |
|---|---|---|---|
| A1 publication構成 | JDBC store＋UPDATE、対象listener選別、業務transactionとの整合、publication保存とversion整合 | Reference Application構成を第一候補。`koiki-reference-app`の構成 / Application-owned migration。Frameworkは標準利用と二階層契約のreview | 共通publication Starter / Framework-owned schema、独立Consumerへの提供はS2等の別判断。JPA storeは比較材料として保持 |
| A1 復旧・パージ | 状態照合、停止確認、認可 / Audit付き再送、試行上限、単一実行パージ、保持・cleanup・前進migrationの契約案 | 運用仕様の計画はFramework側。S1実行機能のReference / Tooling配置、復旧入口の配布単位はA1 reviewで確定 | 新しいFramework運用Public APIやBatch正式依存は未承認。通常listenerとの競合、lock喪失・外部送信制御が成立するまで採用保留 |
| A1 規約 | Rule 28 / 29候補と負例、既存Level 0 / 1の維持、間接I/Oのreview割当 | 現在はTooling fixture。正式採用する場合は`koiki-archunit-rules` / Framework | `businessModuleRules(String)`の互換性、Level選択契約、Rule 1重複をblocking review。Tooling fixtureを直接昇格しない |
| A2 承認event | 値だけの承認eventと発行位置、識別・payload最小化、非commit時の非配信 | Reference `expense`。既存Tier 2 RICH / JPA共有モデルを維持 | event型・payload・schemaの固定はreview後。Customer固有eventやFramework Identity拡張へ広げない |
| A2 通知 | event inbound Adapter → Application Use Case → provider outbound Adapter、通知logと冪等境界 | Reference `notification`、Tier 1 / JPAを第一候補。状態遷移等がTier 2条件を満たす場合は設計時に再評価 | 汎用通知サービス、実メールprovider・宛先・SLA・Customer通知は別判断 |
| A2 検証支援 | provider受理記録、重複・失敗・受理直後停止を観測するstub / harness | 非配布Tooling。既存`build-support/phase4-level2-verification`は材料。正式実演用Toolingの配置は後続設計 | 故障スイッチやtest providerを正式Reference / Framework成果物へ混入させない |
| D1 非同期観測 | FAILED件数・滞留起点、event / publication / retry / jobの相関、log / trace、alertと再送後の照合 | S1はReference / Toolingでの観測構成を候補とする。既存Observability Starterの再利用可否をFramework側でreview | 汎用metric / traceのStarter拡張、実運用exporter / alert体制は別判断。D1同期・OpenTelemetry全体は別package分解で追跡 |

Spring標準の利用を優先する。Application所有のpublication構成でも、Framework依存・Rules・既存Starterとの整合reviewは必要である。Framework本体の変更が必要かは照合だけで決定しない。

## 5. DoDと既存Evidenceの対応

| DoD | S1での分担 | 既存PL2材料 | 正式候補に残る差分 |
|---|---|---|---|
| 4-1 | A2承認 / 通知、A1保存 | fixtureの元transactionはlistener失敗後も残る | 実expense承認・認可・Business Audit、同期vetoと通知失敗の統合回帰 |
| 4-2 | A1＋A2＋D1統合 | 別JVMのPUBLISHED / PROCESSING停止・再送 | package済みReference、停止証拠の真正性、通常listener競合・lock喪失窓、復旧時間と手動復旧のDoD解釈 |
| 4-3 | A2、A1復旧境界 | 冪等keyありstub受理1件、keyなし重複 | event識別、通知logとprovider受理の整合、受理不明時の照合と停止条件 |
| 4-4 | A1再送＋D1監視 | gauge、stale化、再送filter、FAILED遷移時刻のTooling trigger | publication年齢とFAILED滞留の起点を区別。schema所有、認可・Audit、alert sink、上限到達時の扱い |
| 4-5 | A1、C3基盤との照合 | 保持期限によるCOMPLETED削除 | 単一実行、同時配信 / 復旧との競合、保持Owner、FAILED / 未処理の保全、lock喪失 |
| 4-12 | A1識別＋D1、A2実演 | 初回 / 再送のMDC相関と次requestへの非漏えい | executor適用、別process・sinkの追跡、実trace / exporterと非露出・cardinality |

Tooling triggerのFAILED時刻column、metric名、監視間隔、試行回数を正式契約へ転記しない。実演手順・観測点・PASS / FAIL / BLOCKED条件は次段階で具体化する。

## 6. schema・依存・APIのreview論点

| ID | 主担当package | reviewに必要な判断材料 | 今回の状態 |
|---|---|---|---|
| M1 | A1 | JDBC＋UPDATEの正式選定、publicationと通知logのschema Owner、Reference `kkref`履歴とFramework二階層の整合、upgrade / cleanup、DB方言 | 第一候補と現行配置差を整理。正式選定・DDL未確定 |
| M2 | A1 | runtime依存の宣言先・実効version・BOM / Starter影響、自動schema初期化との重複、対象listener列挙 | Referenceが構成する案。正式座標・設定は未固定 |
| M3 | A1 | Rule 28 / 29のLevel選択・Public API互換性、Rule 1重複、間接listener / I/Oの検査限界 | 現行拒否を維持。Tooling候補をreview入力として使用 |
| M4 | A2 | event識別・payload・発行transaction、通知logとprovider受理の整合、PII、認可 / Auditと非HTTP復旧主体 | module Ownershipを整理。event / provider契約は未確定 |
| M5 | A1＋A2 | 停止確認の発行・検証、通常listener / 複数復旧者競合、lock喪失窓、受理不明時照合・provider冪等性 / fencing | 未解決の安全条件。後続の安全性・復旧具体化へ渡す |
| M6 | A1＋D1 | 滞留時刻・metric契約、別processの相関、trace / exporter、sink / alert、非露出、運用認可・Audit | 既存requestIdとTooling観測の適用限界を整理。実運用契約は未取得 |
| M7 | A1＋C3 | パージの単一実行と保持、配信 / 復旧競合、実行基盤・metadata・cleanup Owner | C3へ関連入力。`koiki-batch-fw`正式採用は未決定、今回再点検なし |

M1〜M7のreview完了は今回宣言しない。設計が成立しない対象は停止し、既存Level 1 baselineを維持する。手動復旧で核心DoD 4-2 / 4-3を省略しない。

## 7. 次段階へ渡すもの

**後続資料（2026-10-05）:** [S1安全性・復旧条件の具体化](phase4-s1-safety-and-recovery-conditions-20261005.md)で、以下1の条件案・停止点・必要Evidenceを整理した。状態はDRAFT / blocking review入力であり、安全方式の採用・review完了ではない。

今回、A1 / A2 / D1についてS1対象と成果物Owner、現行sourceとの差分、DoDとreview論点を照合した。作業7全体、S1、F-4、ORは未完了である。

1. M5を中心に安全性・復旧を具体化し、M1 / M4 / M6との依存、停止条件と必要Evidenceを整理する。
2. DoDごとの正常・拒否・競合・停止窓を実演単位へ分解し、ReferenceとToolingの構成、固定artifact・DB / Audit / stub / log / metricの照合方法を揃える。
3. 実施 / review / 復旧担当、環境・cleanup、概算と上限を取得する。既存47〜80標準人日の仮配賦を今回の予算やAI支援時間へ転用しない。
4. 他packageを同じ粒度で分解し、作業4b・5と合わせてORへ提出する。

既存R1〜R7、F-5、Gate規定、Agent guidance、production code、POM、Rules、migration、CIを今回変更しない。具体的な採用・開始経路・正本改訂とremote操作は各承認点で判断する。
