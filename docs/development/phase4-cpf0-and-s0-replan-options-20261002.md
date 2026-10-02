# CP-F0判断材料・S0時の再計画案（2026-10-02）

**状態:** DRAFT / 作業5のreview入力。S1検証完遂を目指す進め方の候補化はOWNER APPROVED（2026-10-02）。具体的な採用・実行条件、DoD変更、開始範囲は未承認。
**Ownership:** Framework側の計画文書。Reference実証・Tooling検証・Customer導入の責任は分離する。
**確認baseline:** `docs/daily-development-workflow` / `dbee2bf`。既存設計・PL2 Evidenceの文書照合。新しい実測はない。
**入力:** [入口・先行範囲案](phase4-entry-and-forward-scope-options-20261002.md)、[作業順序案](pre-phase4-framework-independent-work-review-20260930.md)、[PL2判定資料](KOIKI-JavaWeb-FW_Phase4_PL2_P4-F判定資料_v0.1.md)、[F-5台帳](phase4-pl2-f5-integration-and-gate-delta-draft.md)、[PL2 Evidence](../architecture/validation/phase4-pl2-level2-verification.md)、[復旧runbook案](phase4-pl2-publication-recovery-runbook-draft.md)。

## 1. CP-F0で判断すること

CP-F0は、Level 2を今どの範囲で実証・提供するかと、その選択が当初成果物・DoD・後続依存に及ぼす影響を決める。先行Gateの対象を決める判断とは別軸であり、同じORで両者を照合する。

**承認済みの計画方向:** Ownerは2026-10-02に、S1（用途限定のReference実証）を完遂する進め方を候補とすることを決定・承認した。この方向に沿って成立条件と実行計画を具体化する。成立条件が揃わなければS0を選択できる再計画案を備えるが、自動切替しない。S2は案件との設計初期からの適合確認で必要性を見極め、共通提供の需要・保守責任・S1との差分を示して別判断する。[決定・経緯・承認範囲の記録](phase4-s1-completion-direction-decision-20261002.md)を参照。

2026-09-27のPL2 / F-5にある暫定S0は過去時点の未承認提案として保存する。今回の候補化判断は、Ownerが示した「案件判断に連動させず、Framework側で成立する範囲を進捗させる」目的に沿う。S1の具体的採用・開始条件、以前のGate文案の改訂は後続判断として残す。

### 1.1 Framework実証とCustomer導入を分ける

| 判断対象 | 根拠・必要性 | 必要な入力 |
|---|---|---|
| Framework / Referenceの実証 | [グランドデザイン§27.8](../architecture/grand-design/KOIKI-JavaWeb-FW_グランドデザイン_v0.2.md#278-phase-4-enterprise-integrationv04)はnotificationと耐久配信・冪等性の実演を求める | 対象event、Reference通知先の模擬契約、復旧方法、実施・review担当、検証環境と作業上限 |
| Framework共通提供 | 複数Consumerへ再利用する契約、upgrade・supportの必要性 | 利用先、共通化条件、schema / API・保守責任、互換性とS1との差分 |
| CustomerのLevel 2採用 | 案件でコミット後の処理・耐久配信を必要とするか | 業務要件、実provider、冪等性、通知量・SLA、監視 / 復旧当番、導入・継続費 |

Customerの入力未取得を、Framework自身の実証が不要である根拠にはしない。一方、当初DoDがあることだけで、安全性・担当・予算が未成立の実装を始めない。Referenceの模擬providerは実案件providerの受入を代替しない。

### 1.2 実機能・想定利用場面・利用可能性で読むS0 / S1 / S2

S0 / S1 / S2は、Framework全体の機能数や品質の段階ではなく、**業務確定後の処理を分離し、未処理を保存して障害後に再実行する機能を、どこまで実証・提供するか**の選択である。認証・認可、同期業務処理、業務Audit、MVC / REST等の既存baselineは、どの選択でも維持する。

以下の「実機能」は利用者に説明する機能像である。既存baseline、今後のReference実証、将来の共通提供を区別し、計画上の機能像を現在の提供済み機能として扱わない。

| 説明する観点 | S0：同期業務処理を使い続ける | S1：限定した承認後通知を実証する | S2：複数アプリで再利用する耐久配信基盤を提供する |
|---|---|---|---|
| 実機能として何をするか | リクエスト内で業務処理・整合性確認・DB更新・業務Auditを行い、結果を返す。Level 2の未処理保存・非同期通知・再送は追加しない | 業務の承認確定後に通知を別処理で実行する。配信対象をDBへ保存し、失敗・停止で残った対象を確認・再送できる機能をReferenceで実証する | S1の保存・配信・失敗監視・再送・完了記録パージ・相関追跡を、複数Consumerが利用できる共通契約と運用条件として整備する |
| どこで使う想定か | 既存Referenceのmaster / expense、同じ同期処理の契約で成立するConsumer。処理結果をその場で確定させたい業務 | `koiki-reference-app`のexpense承認後通知。Reference-owned notificationから、非配布Toolingのprovider stubへ送る限定実演 | 耐久配信を必要とする複数の業務アプリ内のmodule間処理・承認後通知等。各アプリが固有のevent・listener・外部接続先を所有する |
| 利用者・運用者が得ること | 承認結果を画面 / APIで確認でき、同期処理の失敗をその場で扱える | 通知失敗で確定済み承認を取り消さず、未処理を追跡して復旧できることを確認する。実演担当が失敗記録と再送結果を照合できる | 各アプリが共通の導入・監視・復旧方法を利用できる。Framework側は互換性・upgrade・supportの責任を持てる範囲を定める |
| 「使える機能か」への現時点の回答 | **既存baselineの範囲は利用の根拠がある。** 耐久配信が必要な要件はS0だけでは充足しない。案件への適合・受入と正式配布は別判断 | **限定用途で使えることをこれから実証する候補。** Toolingの技術Evidenceはあるが、正式Referenceの機能は未実装・未承認。案件の実メール通知へそのまま適用できる状態ではない | **共通機能として使える状態を今後成立させる候補。** 共通契約・独立Consumerでの検証・supportは未成立で、現在提供済みとは説明できない |
| 利用成立の条件 | 同期応答とtransaction境界で要件が成立すること。独立したPhase 4機能は、それぞれ設計・検証・承認する | §2.1の復旧安全性・冪等性・運用条件と正式ReferenceのEvidence。Customer利用には実provider・業務要件・運用の別検証 | S1に加え独立利用先の需要、共通化の根拠、schema / API互換性、複数ConsumerのEvidence、保守責任。各アプリ固有の利用条件も確認する |

S0でも、SPA / MVC併用、外部API接続、Batch、Storage、Virtual Threads等の独立候補は別軸で進め得る。ただし、それらはS0を選んだだけで追加・利用可能になる機能ではない。対象と開始条件は[入口・先行範囲案§2](phase4-entry-and-forward-scope-options-20261002.md#2-全packageの必要入力と開始境界)で管理する。

### 1.3 同じ「経費承認」で利用時の動きを説明する

**S0の利用像:** 承認者が画面またはAPIから承認する。既存ReferenceのUse Caseが業務条件を確認して承認状態とAuditを確定し、応答する。承認結果を確認する既存経路は維持するが、「承認後メールが障害後も必ず送られる」という機能は含まない。業務確定に必要な同期整合性確認を、通知のために非同期へ移さない。

**S1の利用像（実証案）:** 同じ承認操作に対し、承認がコミットされた後、Referenceの通知処理が承認結果をprovider stubへ送る。送信に失敗しても承認は確定したまま残り、実演担当が未処理を確認して、合意した停止条件・認可・Auditの下で再送する。process強制停止後の復旧と重複送信の負例も確認する。stubが記録するのは模擬providerの受理であり、実メールの受信者への到達は実証しない。

**S2の利用像（共通提供案）:** 経費アプリに加え、別の業務アプリも、確定した業務eventをアプリ固有のlistenerで処理し、共通のpublication保存・観測・復旧契約を利用する。業務eventの意味、宛先、メール文面や実providerとの連携は各アプリが所有する。独立Consumerの需要は未確認であり、この例は共通提供が成立した場合の想定である。共通基盤の提供だけで、アプリ間通信、全処理の一度だけの実行、実メール到達を保証するものではない。

**判断する問い:** 「承認の同期処理で足りるか」「承認後通知の耐久性をReferenceとして実証したいか」「その仕組みを複数Consumer向けに共通提供する必要があるか」を順に確認する。再送で同じ処理が再度呼ばれる可能性があるため、重複した外部副作用を防ぐ条件は、S1 / S2ともproviderの冪等性等を含めて検証する。既存[PL2 Evidence](../architecture/validation/phase4-pl2-level2-verification.md)では、冪等keyのないstubに再送すると受理が重複した。

## 2. S0 / S1 / S2の比較と成立条件

| 観点 | S0：Level 1維持 | S1：用途限定実証 | S2：共通基盤提供 |
|---|---|---|---|
| 対象 | Level 2 productionを見送り、独立packageを進める | Reference承認通知とA1 / A2 / D1の必要最小範囲 | S1に加え独立Consumer・共通契約・upgrade / support |
| 案件入力との関係 | 案件のLevel 2需要待ちを全packageの停止条件にしない | Reference実証として閉じる範囲は案件需要なしで設計可能 | 現時点で独立利用先の需要・共通提供条件は未確認 |
| 当初DoD | 4-1〜4-5・4-12は未達。採否・延期を明示判断 | 当初の核心4-2・4-3を含む正式Reference実演を目指す。自動PASSではない | 共通提供の検証も追加。S1 Evidenceだけでsupport成立としない |
| 安全性・運用 | Level 1同期・Auditを維持 | 再送競合・停止確認・冪等性・認可 / Auditを契約化し、実演担当が復旧できること | S1の条件に複数Consumer・環境の適用条件と保守体制を追加 |
| 見積 | DoD再計画・残packageの分解に見積を付ける | 対象・作業上限・実施Ownerを作業7で具体化 | S1との差分を別見積。以前の概算を同じ予算としない |
| 今回の扱い | S1不成立時も独立作業を進めるための再計画材料 | 完遂を目指す進め方の候補化はOwner承認済み。具体的実行条件を計画する | 案件との適合確認で必要性を評価し、共通提供は別判断 |

### 2.1 S1の対象を限定する案

対象eventはReferenceのexpense承認結果を伝える値だけのevent、受信側はReference-owned notification、通知先は非配布Toolingの冪等性を観測できるprovider stubを候補とする。event名・payload、schema、API、依存座標はA1 / A2 blocking review前に固定しない。
元の承認・業務Audit・同期vetoを維持し、通知結果で承認を取り消さない。Customer通知、実メールproviderの保証、汎用通知サービスの提供を対象に広げない。

| 成立条件 | PL2で得た材料 | 正式候補を進める前に必要な確認 |
|---|---|---|
| publication保存とschema境界 | JDBC / JPAと二階層migrationのTooling比較 | 保存先Owner、Data Starterとの統合、upgrade・前進migration、対象listenerの選別 |
| 停止後の復旧 | 強制停止・guarded再送・専用復旧worker排他 | 停止確認の真正性、通常listenerとの競合、lock喪失の検知前の窓、外部送信の制御 |
| 冪等性 | stubでkeyあり1受理、keyなし重複 | event識別とprovider受理・通知logの整合、受理不明時の照合、重複の負例 |
| 観測・再送・パージ | FAILED metric、相関MDC、保持期限パージ | 正式な滞留起点、trace / sink、運用認可・Audit、単一実行とcleanup |
| 実施・復旧担当 | runbookに役割候補を整理 | 実演担当・判定者、復旧許容時間、権限・停止時の扱い、作業上限と環境 |

既存fixtureの確認ファイルやworker lockだけで安全性を保証しない。手動復旧も安全性の代替にはならない。4-2の再起動後配信を手動復旧で判定する場合は、許容復旧時間と判定解釈をOwnerが明示し、満たさないものは未達として残す。S1でも正式Rules / Public API、migration、依存変更は各blocking reviewに従う。

## 3. S0を選んだ場合のDoD・成果物再計画案

S0はLevel 2だけの判断であり、当初Phase 4全体の完了判定やMyBatis triggerを解除しない。次の表は変更の提案材料で、現行DoDはそのまま有効である。

| 当初DoD・成果物 | S0時の状態 | 再計画の提案と再評価条件 |
|---|---|---|
| 4-1：非同期通知・承認非rollback | 未達 | notification実証を延期候補とし、Reference対象とS1成立条件が揃う時点で再評価。Phase 3承認成功を代替PASSにしない |
| 4-2：停止後の配信 | 未達・核心 | 耐久配信実演を延期候補。停止確認・競合・復旧条件が成立した時点で再評価。同期処理の成功やTooling結果で置換しない |
| 4-3：二重通知防止 | 未達・核心 | 通知再送の冪等性実演を延期候補。受理不明窓と再送の観測が可能な時点で再評価 |
| 4-4：FAILED metric / 再送 | 未達 | Level 2に合わせて延期候補。同期Observabilityや一般的なmetric追加を代替にしない |
| 4-5：publicationパージ単一実行 | 未達 | Level 2に合わせて延期候補。Batch 4-10とは別に保持し、再採用時に実行基盤を接続 |
| 4-12：非同期相関 | 未達 | Level 2に合わせて延期候補。同期traceやMDC確認を代替にしない |
| 4-6：MyBatis分離の競合 / error | 独立trigger待ち。S0だけでは採否を決めない | C0でtriggerと設計をreviewし、C1の非同期連携を分離して実演可能か判断する（§4） |
| 4-7：外部API耐障害性 | 維持 | Referenceの模擬接続先でC2を計画。Level 2不要な実証範囲を定める |
| 4-8・4-9：Session SPA / MVC併用 | 維持 | B1で実証。Customer BFF / Bearerで代替しない |
| 4-10：Batch単一実行 | 維持 | 通知・publication以外のReference job候補で実証（§4） |
| 4-11：Virtual Threads CI | 維持 | D2でruntime・CI費と変更承認を具体化。既定無効は維持 |
| OpenTelemetry、File / Storage、Container / ECS | 当初成果物として保持、番号なし | 独立した実証目的と運用 / platform条件を作業7で具体化。S0で自動除外しない |
| SAML / Edge、受渡し、optional | 各入力・trigger待ちを維持 | 案件入力と責任分担による判断。S0を理由に自動採用・不採用としない |

### 3.1 延期・DoD変更をどう記録するか

**提案:** S0を選ぶ場合、まず「今回の先行範囲からLevel 2実証を外し、当初DoDを未達のまま保持する」再計画を基本とする。Phase 4の完了は宣言せず、独立packageを先行させながら再評価条件を追跡する。
Phase 4自体の終了条件を変えてLevel 2を後続へ移す場合は、移管先・判定時点・担当・元DoDとの対応を示し、グランドデザイン§27.8とADR・計画の改訂を別承認する。「延期」の一語で核心DoDを削除しない。
代替Evidenceがある場合も、何を示せて何が残るかをOwnerが記録する。PL2 PASSを正式DoDへ転記しない。

## 4. C1 accountingとC3 Batchの依存再計画

### 4.1 accounting

当初C1はMyBatis分離とA1からの非同期精算連携を含む。S0ではそのまま開始できない。次の2案を比較する。

| 案 | 意味・利点 | 判断・制約 |
|---|---|---|
| C1全体をA1とともに保留 | 当初の非同期連携構成を維持し、部分的な実証による誤認を避ける | 4-6も未達で残る。MyBatis trigger不成立なら引き続きRule 8拒否 |
| MyBatis永続化実証と非同期連携を分ける | C0の成立後、分離モデル・楽観lock・共通errorの4-6に必要なReference実証を独立して設計する | 検証用の明示入力経路と業務成立条件をreview。同期外部送信へ安易に置換しない。非同期精算連携は未達として別保持 |

Framework側で4-6を進める必要がありC0条件を満たせる場合には、後者を検討候補とする。これはMyBatis adoption trigger成立の記録ではない。承認前にSEPARATED、Rule 25〜27 / 30〜37、fixture・依存を追加しない。

### 4.2 Batch

本節のjob比較はS0時の再計画材料である。今回のS1候補化判断の中心をBatch整備へ置き換えない。Ownerが示したnon-Web専用実行＋`koiki-batch-fw`の組込み想定、Web同居を可能だが非推奨とする分岐、C3 / C4横断の検証入力は[決定記録§5〜7](phase4-s1-completion-direction-decision-20261002.md#5-http以外の入口とnon-web実行の位置づけ)に記録した。連携方式・正式依存・個別実装は未決定。

S0でも4-10の単一実行・二重起動拒否は必要である。jobの業務内容と起動基盤・metadataの契約を分けて計画する。

| Reference job候補 | A1依存 | フィージビリティと残る判断 |
|---|---|---|
| 通知reminder / publicationパージ | あり | S0では主実演にしない。A1採用時に共通実行基盤へ接続する候補として保持 |
| 月次集計の再生成 | なしとする設計候補 | Referenceの確定済み状態から集計する模擬job。業務仕様、対象期間、結果の所有と再実行方法を定める。新しい会計確定契約を持ち込まない |
| 月次締め | 通知なしで設計し得る | 業務状態遷移・同時更新の不変条件が増えるため、単一実行の実証に必要な範囲を超える可能性がある |

**提案:** S0時の主候補は、通知に依存しない月次集計の再生成とし、作業7で実演範囲を具体化する。これはjobの正式選定・業務実装の承認ではない。
同一条件の二重起動、別processのcrash後再実行、metadata・失敗記録・Auditの確認を検証計画へ入れる。実際のSpring Batch構成、schema Owner、ロック方式と運用担当はblocking reviewで定める。

## 5. 先行順序と作業7への入力

| 分岐 | 計画上の順序 | 独立対象の扱い |
|---|---|---|
| S1成立を目指す | A1 / D1同時設計 → A1 blocking review → A1成立のEvidence → A2統合実演 | B1 / C2 / C3 / C4 / D2等の独立性を作業7で確認し、A1の待ちを全体の停止条件にしない |
| S0を選ぶ | 未達DoD・再評価条件を記録 → B1 / C2 / C3 / C4 / D2等の先行候補を具体化 | C1はC0と非同期連携分離の判断。D3 / D1同期は運用・platform準備に応じて扱う |
| S2を検討する | S1との差分・独立Consumer・schema / API・support・概算を追加 | 共通基盤の成立待ちに独立Reference実証を一括で連動させない |

これらは設計と判断材料の順序である。案2による先行開始にはGate改訂・対象の承認が必要。現行規定のままproductionを始める順序ではない。

作業7では、A1 / A2 / D1の既存分解・見積を今回の限定範囲に照合し、残packageを同じ項目で分解する。対象、実施 / Evidence Owner、作業単位、検証環境・負例、未決条件、概算・上限を揃える。既存47〜80標準人日やPhase共通25〜40を、今回の予算・AI支援時の所要時間へ自動転用しない。

## 6. ORへ出す判断事項と現時点の不足

| 判断事項 | 今回の提案材料 | OR前に残る入力 |
|---|---|---|
| CP-F0：S0 / S1 / S2 | S1完遂を目指す進め方の候補化はOwner承認済み。S0再計画材料・S2別判断条件を保持 | S1の具体的採用・実行条件、対象・担当・上限・環境、安全性の未解決事項をどの段階で解くか |
| S0時のDoD | 当初DoD未達を保持し独立packageを進める。終了条件変更は別承認 | 再評価担当・時点、延期をPhase内に残すか後続へ移すか |
| C1依存分離 | C0成立後、4-6に必要な永続化実証と非同期連携を分ける候補 | MyBatis adoption判断、明示入力経路とReference成立条件 |
| C3 job | 月次集計再生成を主候補に詳細化 | metadata / 実行基盤、業務範囲、運用Owner、見積 |
| 先行Gateの対象 | 案2を検討軸として独立対象を選ぶ | 作業7の全件分解・概算、作業4bの対象 / Gate差分の最終整理 |

本書で作業5の判断材料を文書化したが、ORの提出完了ではない。次は作業7の分解と概算根拠を揃え、作業4bの対象・順序・Gate差分と合わせてreviewする。顧客・アプリチームとの協調とFAQ対応は継続し、案件へのLevel 2導入判断は必要入力が揃った時点で別途行う。
