# S1方式候補・DoD解釈・運用／Audit・観測条件案（2026-10-05）

> **現在の参照先（2026-10-05）:** 本書は運用・Audit・観測の設計論点。初回ST-Bの範囲は後続の承認済み方式票に限定し、管理台帳一式・実trace・独立providerを初回へ追加しない。正式接続・数値・DoD判定は後続。 [最新引継ぎ](phase4-s1-local-verification-start-handoff-20261005.md)／[承認済み方式票](phase4-s1-method-ballot-and-local-verification-contract-20261005.md)。

**状態:** DRAFT / J3〜J6の詳細設計判断材料。OwnerはW01 / W02資料を確認し、本段階への継続を指示した。方式・DoD解釈・数値・正式API / schema・Gate・実装開始の承認ではない。
**Ownership:** Framework側の計画・review文書。実行・業務通知と操作記録はReference / Application候補、故障注入と観測支援は非配布Tooling。
**確認baseline:** `docs/daily-development-workflow` / `e75a70a`。W01 / W02等は未commit差分。既存Audit / Observability source・承認契約のread-only照合。今回Maven / Docker・動作検証・remote確認は行っていない。
**入力:** [W01 / W02 source照合](phase4-s1-w01-w02-source-design-review-draft-20261005.md)、[初回判断J1〜8](phase4-initial-scope-gate-and-open-decisions-draft-20261005.md)、[安全条件](phase4-s1-safety-and-recovery-conditions-20261005.md)、[DoD実演](phase4-s1-dod-demonstration-plan-20261005.md)。

## 1. 方式候補の比較と初回推奨

**初回候補:** Reference Application-owned JDBC＋UPDATE、既存kkref履歴を維持する配置を優先比較し、限定した検証環境の関連通常processを停止・drainしてから、認可 / Audit付き専用processで対象1件を手動復旧する。人が終了世代・対象DB・provider受理・Auditを総合確認し、同じ論理通知keyで再送または保留を判断する。

| 比較軸 | 初回推奨候補 | 保持する代替・追加判断 |
|---|---|---|
| 保存・完了 | Reference-owned JDBC＋UPDATE、kkref履歴とFramework二階層の実効整合を検証 | JPA比較材料を保持。Framework-owned共通schema / Starterは需要・upgrade責任を示して別判断 |
| 通常listenerとの競合 | 関連processの停止 / drain、再起動・別instance引継ぎを制御する限定検証方式 | 実行owner追跡、claim / lease / fencingは停止方式で成立しない場合の差分候補 |
| 復旧者の排他 | CP8型lockを専用connectionで保持し、対象publication・今回の試行を再照合 | lockは通常listener停止・外部副作用抑止の代替ではない。喪失検知前の窓を負例で確認 |
| 外部副作用 | provider stubの安定key・受理照合 / 安全な冪等再要求 | key保持・payload不一致・応答不明を設計。実providerはCustomer採用時の別判断 |
| 完了判定 | 対象publication / 試行、通知log、provider受理、操作Auditを突合 | event単位の件数・別publication・再送前FAILEDで終端を誤認しない。E13追加枝を維持 |

限定環境停止方式は本番で全停止を推奨する判断ではない。可用性、停止範囲・drain証拠、専用processで通常listenerが意図せず動かない構成をreviewする。成立しなければ方式・作業量を見直し、人の判断だけで競合を許容しない。

## 2. DoDの判定解釈案

| DoD | S1で確認する出口 | 判定条件・Ownerに残す判断 |
|---|---|---|
| 4-1 | 承認 / Business Audit・publicationが整合し、通知失敗で確定済み承認を取り消さない | E01〜03。rollbackした承認から配信しない。既存認可・同期veto・version / scopeを回帰 |
| 4-2 | 停止後、再起動と合意した手動復旧経路で対象publicationを配信完遂 | E04〜07＋競合・停止確認負例。Ownerが手動復旧の適用解釈と許容時間を明示してからPASS候補へ。安全な保留だけでは達成しない |
| 4-3 | 同じ論理通知の再配送・受理後停止・並行要求でstub側受理が1件、別通知を誤抑止しない | E06〜10 / E14。key不成立・期限外等は適用外条件と負例を残す。stub受理からメール到達・実provider保証を推定しない |
| 4-4 | FAILED件数・定めた起点の滞留とalertを照合し、人が理由を確認して認可再送・結果確認する | E15〜17 / E21。認可 / Audit・上限 / timeout・保留 / 連絡も検証。閾値・分類・代行は未確定 |
| 4-5 | 保持期限を過ぎた対象COMPLETEDだけを単一実行で削除し、未処理と復旧証拠を保全 | E18 / E19。provider key寿命と再送可能期間、通知log保持、復旧・パージlockの関係をreview |
| 4-12 | 初回request・非同期処理・別process復旧をevent / publicationで辿り、実traceとlog・Audit / providerを照合 | E20 / E21。別processに元requestのMDCが自動伝播するとしない。次requestへの非漏えいと非露出を検証 |

### 2.1 復旧時間の測定と人の確認

復旧時間は少なくとも二つに分ける案とする。**全体時間**は故障位置の停止時点から対象publicationの完了・受理整合確認まで、**操作時間**は有効な停止証拠・権限・判断材料が揃い人が復旧を許可した時点から同じ出口までを測る。検知・担当者確認・調査待ちも別に記録し、操作時間だけを示して復旧遅延を隠さない。

起点・時計差・完了条件をcaseごとに固定し、開始不能・保留は保留として経過時間を記録する。全体 / 操作の許容時間、対応時間帯・通知先・代行・上限値はOPEN。実演の単なる速さから本番SLAを宣言しない。手動復旧の4-2解釈が合意できない場合は未達または解釈判断待ちとして残し、DoDを黙示的に変更しない。

## 3. 運用者の判断と再送手順の条件案

Owner一人が確認・判断・実演運用者 / 判定者を兼務し、Codexが証拠採取・照合・実行準備を支援する。別人配置や二者承認を一律に要求しない。各操作の主体・権限・理由・結果を記録し、未知の結果を未知のまま扱う。

| 段階 | 人が確認・判断すること | システムで守る条件 |
|---|---|---|
| 検知・受付 | 対象incident、FAILED / 未処理・滞留、通知先、他の作業を確認 | 件数・年齢 / 滞留と相関材料を提供。stale / FAILEDを停止証明にしない |
| 停止・照合 | 全対象process世代・終了 / drain、再起動不在、provider受理、対象publication / 試行、Auditを確認 | 無効な停止入力、誤対象、古いsnapshot、無権限を拒否。確認できない対象は保留 |
| 再送判断 | 原因・副作用・安定key・上限を確認して再送 / 調査 / 保留 / 連絡を選ぶ | 上限解除・対象外操作を無条件で許可しない。DB statusだけで再送しない |
| 実行 | 送信前の記録と対象が判断どおりであることを確認 | 専用排他、lock取得後の再照合、同じkey、認可、必要Audit / 操作記録、timeout・喪失時停止 |
| 結果・引継ぎ | DB・provider・通知log・Auditを突合し完了 / 不明を判断 | 当該試行の終端を追跡。応答不明・送信後記録失敗では自動追加再送しない |
| 再開 | 新しいsnapshot・停止証拠、原因対応と権限を確認 | 旧確認を次の試行へ使い回さない。保留対象と証拠を残す |

## 4. Auditの現行契約と復旧への適用条件

根拠は[Audit Starter README](../../koiki-starters/koiki-starter-audit/README.md)、[Phase 2承認契約](../architecture/validation/phase2-p2-b1-contract-review.md)、[BusinessAuditTransaction](../../koiki-starters/koiki-starter-audit/src/main/java/org/koikifw/audit/internal/BusinessAuditTransaction.java)、[SecurityAuditTransaction](../../koiki-starters/koiki-starter-audit/src/main/java/org/koikifw/audit/internal/SecurityAuditTransaction.java)。sourceから、Businessは既存transaction必須・業務と同時commit / rollback、SecurityはREQUIRES_NEWで独立commit、Audit正本はDB rowと確認した。

| 操作・記録 | 適用案 | review事項・失敗時の扱い |
|---|---|---|
| 元承認のBusiness Audit | 既存expense承認契約を維持 | 失敗なら業務・publicationも整合してrollback。非同期通知結果を元Auditへ後書きして意味を変えない |
| 復旧者の認証 / 認可・拒否 | Securityとして意味が成立する操作をSecurity Audit候補とする | OS利用者・入力IDだけでFramework user本人と認定しない。独立commitでもAudit不可なら再送を許可しない条件をreview |
| 復旧要求・状態更新・通知結果の業務記録 | 同一Application transactionで更新する意味が成立する場合にBusiness Auditを使う候補 | 非transactionなrunnerから直接呼ばない。外部送信はrollbackできないため、Business Auditが失敗した後もprovider受理の照合が必要 |
| 再送前の操作記録 | 外部I/Oを待たずに記録・commitし、対象 / 試行・認可結果に結び付ける案 | Security / Businessを便宜的に選ばない。分類・短いtransactionとcommit成功の確認を定義。必要記録が成立しなければ送信なし |
| 送信後の結果記録 | provider受理・DB状態と結び付けて別に確定する案 | 失敗時は受理不明 / 証跡欠落として保留・連絡・照合。元承認を取消したり未送信と断定したりしない |

### 4.1 actor・操作台帳・相関の制約

[AuditActor](../../koiki-starters/koiki-starter-audit/src/main/java/org/koikifw/audit/AuditActor.java)のuserは不変Framework user ID、systemはFramework-owned background operationの契約である。Reference復旧を任意のSYSTEM文字列へ割り当てず、信頼できる非HTTP主体の認証・権限・actor mappingをreviewする。actorを指定するだけでは認証・認可は成立しない。Framework Identityへの運用属性追加は前提にしない。

[AuditEvent](../../koiki-starters/koiki-starter-audit/src/main/java/org/koikifw/audit/AuditEvent.java)はeventType / action / resource / reason等、[AuditResult](../../koiki-starters/koiki-starter-audit/src/main/java/org/koikifw/audit/AuditResult.java)はSUCCESS / FAILUREを提供する。保留・不明・調査継続・before / after・provider受理証拠を任意Mapや新enumとしてFrameworkへ追加しない。これらはReference / Application-owned操作台帳候補に記録し、既存Auditへ安全なresource ID等で結ぶ案とする。台帳はAudit DB正本の代替ではなく、両者の整合・failure semantics・保持をreviewする。正式schemaは未確定。

初回requestの相関がない復旧でもpublication / 操作IDからAuditを辿れる条件を設計する。Auditの内部時刻・request / trace相関をcaller任意入力へ変える方式を先行しない。password・token・raw email・外部subject等を証拠・actor・reasonへ渡さない。

## 5. 観測・alert・保持の条件案

[Observability Starter](../../koiki-starters/koiki-starter-observability/README.md)はServlet requestId・TaskDecorator・structured log・healthを提供し、SecurityContext accessor・exporter・cloud backend・業務logを含まない。既存機能の再利用と追加Application / Tooling構成を分ける。

| 観測対象 | S1条件案 | 判定・設計に残る点 |
|---|---|---|
| FAILED件数 / 滞留 | DB状態と件数を照合し、publication年齢とFAILED遷移年齢を区別して表示 | 正式な滞留起点・保存方式・閾値はOPEN。Tooling triggerを正式migrationへ転記しない |
| 識別と相関 | event / publication / listenerと復旧operation / retry / job、provider key・Auditの対応を追える | 一意IDはログ・操作台帳等に使い、metric labelに全件IDを入れない。実metric名・tag・APIは未固定 |
| 実trace | 初回の非同期伝播と別processの復旧traceを個別に採取し、永続識別子で辿れる関連を定義 | 同じtrace IDを無条件コピーしない。span link等の採否・tracer / exporter・sinkをreviewし実演。MDC markerで代替PASSにしない |
| alertと人の確認 | alert受信から対象照合・保留 / 再送判断・結果確認 / 連絡を実演 | 通知先・対応時間・代行OPEN。不達負例では代替連絡と復旧まで確認 |
| lock喪失 / timeout・不明 | 危険な追加再送を止め、旧worker・provider副作用と結果記録を確認 | timeoutを取消しと見なさない。理由・対象・保留経過を観測 |
| パージ・保持 | 未処理・復旧中・保留証拠を保全しCOMPLETEDだけを期限で削除 | 通知log・台帳・Audit・provider keyの寿命と再送期間を整合。保持数値・資源上限は未取得 |

## 6. 次に閉じる判断と実演への追加条件

**Owner確認と後続材料（2026-10-05）:** Ownerは詳細を全て見切った段階ではないが大枠を認識し、[非HTTP復旧主体・専用process・実観測構成review案](phase4-s1-nonhttp-process-observation-review-draft-20261005.md)への継続を指示した。全文・方式・数値・実装開始の承認とは扱わない。

| 対象 | 今回の具体化 | 未確定事項と次の出口 |
|---|---|---|
| J3 方式 | 限定環境停止 / drain＋専用lock＋安定key・受理照合を初回比較候補へ | 停止証拠・専用process構成・対象試行の終端判定を詳細案と負例へ |
| J4 DoD | 配信完遂と保留を区別、全体時間と操作時間を測定 | Ownerによる手動復旧適用・許容時間・stub保証範囲の判断 |
| J5 運用 / Audit | 人の総合確認、認可・分類・送信前後記録・台帳 / actor制約 | 非HTTP主体の信頼と権限、分類・transaction・記録失敗時の設計review |
| J6 観測 | 滞留起点、別process trace関連、alert・保持を明示 | 実tracer / sink・滞留保存・保持 / 閾値、E15〜21の採取手順 |
| J7 / J8 | 必要構成・追加設計範囲を絞る材料 | 環境確認の対象操作・権限、再見積・段階上限、F-1〜F-5とGate / review順の整合 |

E04〜07へ全体 / 操作時間、E16へ送信前commit / 送信後Audit失敗の副作用照合、E20へ操作台帳 / Audit・別process trace関連を採取条件として渡す。case追加・枝具体化は既存Owner了解を完全網羅承認へ広げず、結果・不足・残リスクを継続確認する。

今回、方式確認の材料とDoD / 運用・Audit・観測条件を具体化した。推奨候補を正式採用・blocking review通過と扱わない。次は非HTTP復旧主体・停止確認 / 専用process・実観測構成を実装前review案としてさらに絞り、環境・再見積 / 上限・Gate判断材料へ渡す。production / Tooling code・POM・migration・Rules・CI・Gate規定・remoteは変更していない。
