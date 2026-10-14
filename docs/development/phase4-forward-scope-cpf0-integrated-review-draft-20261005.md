# 作業4b・5・7：先行範囲／CP-F0統合判断材料（2026-10-05）

**状態:** DRAFT / ORの比較・判断材料。具体的採用、上限・開始経路、Gate設置 / 通過、blocking review、production開始は未承認。OR全体は未実施。
**Ownership:** Framework側の計画文書。Customer採用・実チーム受入・正式受渡しの判断を代替しない。
**確認baseline:** `docs/daily-development-workflow` / `eaf944d`。S1計画4文書はcommit済み。他package分解とREADME・作業一覧は未commit差分。今回、新しい実行検証・remote確認はない。
**目的:** 作業7のS1・他package材料を、作業4bの先行対象・Gate比較と、作業5のCP-F0残判断・S0再計画へ統合する。既存R1〜R7、F-5、Gate規定を本書だけで改訂しない。

## 1. 判断に使う入力と確定事項

| 入力 | 本書での位置づけ |
|---|---|
| [S1決定記録](phase4-s1-completion-direction-decision-20261002.md) | S1完遂を目指す進め方の候補化はDECIDED / OWNER APPROVED。S0 / S1 / S2を同じ未決状態へ戻さない |
| [作業4b入口案](phase4-entry-and-forward-scope-options-20261002.md) | 顧客非依存packageの分類と案1 / 案2。案2は検討軸であり正式採用ではない |
| [作業5 CP-F0材料](phase4-cpf0-and-s0-replan-options-20261002.md) | S1成立条件、S0時の未達DoD・依存再計画、S2追加判断条件 |
| [S1範囲照合](phase4-s1-a1-a2-d1-scope-mapping-20261005.md) | Reference Application構成、成果物Owner、現行sourceとmigration / Rules / 観測の差分 |
| [安全条件](phase4-s1-safety-and-recovery-conditions-20261005.md) / [実演計画](phase4-s1-dod-demonstration-plan-20261005.md) | 人による総合判断を復旧の正式要素とするOwner方針。21 case構成はOwner了解。網羅確定・DoD PASSではなく後続検証で継続確認 |
| [S1実施可能性案](phase4-s1-execution-resources-and-sequence-draft-20261005.md) | Owner一人＋Codex、サブエージェントなし、順次・同期的な協働はOwner合意。役割は兼務する責務区分。環境・稼働・数値上限は残判断 |
| [他package分解](phase4-other-packages-breakdown-draft-20261005.md) | 全件の対象・検証出口・低確度概算・入力待ちを追加整理。Ownerの文書確認と統合継続指示を受けたが、採用・開始・予算承認ではない |

## 2. CP-F0と先行範囲を分けて決める

CP-F0ではS1の具体的対象・復旧条件・DoD解釈と作業規模を判断し、作業4bでは開始Gateの経路・対象package・順序を判断する。同じORで照合するが、一方の了解から他方の実装開始を推定しない。

| 判断軸 | 現在の入力 | 残る判断・条件 |
|---|---|---|
| S1の限定対象 | expense承認event → Reference notification → Tooling provider stub、Application-owned JDBC＋UPDATEを第一候補 | 正式store / schema / 依存・Rule・event、対象listener、既存kkref履歴との整合はA1 / A2 review |
| 復旧と人の判断 | 専用復旧process・認可 / Audit付き手動再送、人がDB / process / provider / log等を照合 | 停止確認方式、通常listener競合、lock喪失窓、安定key・受理不明、非HTTP主体・Audit failure semanticsを具体化 |
| 核心DoD | 4-2停止後配信、4-3重複副作用防止。安全な保留と配信完遂を分ける | 手動復旧を4-2に適用する解釈・許容時間、stub受理を4-3で判定する範囲。核心DoDを省略しない |
| 実施条件 | Owner＋Codexの陣容、ローカル隔離環境案、10作業単位・176〜344標準作業時間の低確度案 | 実環境・権限・資源・稼働、詳細設計後の再見積、段階 / 総量上限・運用閾値の判断。AI所要時間・予算承認ではない |
| S0への再計画 | S1が成立しない場合の材料として保持 | 自動切替しない。未成立条件・未達4-1〜4-5 / 4-12・再評価時点、C1 / C3依存を示してOwner判断 |
| S2追加判断 | 案件との設計初期からの適合確認・gap分類 | 独立利用先・共通化・support、S1との差分・互換 / migration・Evidence・作業量を別判断。今回含めない |

技術方式の全詳細をORだけで確定させることは求めない。ORでは選定範囲・未決事項を解く工程・開始前の必須条件を明示し、A1 / A2 / D1 blocking reviewで閉じる事項を割り当てる。ただし、環境・上限・担当稼働が未確保のまま限定開始Gateを通過したとは扱わない。

## 3. 先行対象の比較と順序案

計画上の第一中心はS1である。他packageの独立性は代替候補と次の準備順を示し、Owner＋Codexによる複数package同時実装を提案するものではない。

| 対象 | DoD・成果物 / 独立性 | 概算材料と不足 | 本統合案での扱い |
|---|---|---|---|
| A1 / A2 / D1非同期：S1 | 4-1〜4-5・4-12。A2はA1成立後、D1は同時設計 | 176〜344時間の残技術作業案。安全方式・稼働・環境・上限未確定 | 最初の限定開始候補の中心。詳細設計・reviewを揃える |
| C2：外部API | 4-7。A1非依存の模擬操作 | 48〜84時間案、操作・library・retry / transaction境界未確定 | 独立候補の最初の準備対象案。S1の待ちがある場合にも別の計画単位で具体化 |
| C3 / C4：non-Web / Batch・file | 4-10とFile / Object Storage成果物。主jobをA1非依存にし得る | C3 72〜128、C4 local 48〜96時間案。Batch依存・永続性・単一実行・file確定、共有費 / 実Storage未確定 | 連携設計を揃え、実演・開始単位は分ける。Batch全体へS1中心を置き換えない |
| B1：Session SPA / MVC | 4-8・4-9、A1非依存。Customer BFFと別 | 88〜152時間案、frontend・route / CSRF・browser条件未確定 | 次の独立準備候補。Customer成果からDoDを代替しない |
| D2：Virtual Threads | 4-11、A1非依存、既定無効 | local比較40〜76時間案。CI構築 / 費用・実CI判定は未算定 | CI入力と別承認が揃う場合に候補。local成功だけで4-11 PASSにしない |
| D3：Container / ECS | 当初成果物、A1非依存のReferenceを候補 | local 40〜68時間案。ECS環境・費用 / 実演未算定 | platform条件別に段階化。local containerからECSへ保証を拡大しない |
| D1同期 | 当初OpenTelemetry、A1非依存 | S1 W07と共通費、残差未算定 | tracer / sinkの再利用を照合。独立全額をS1に重ねない |
| C0 / C1 | 4-6・accounting、MyBatis triggerとA1連携の判断待ち | 見積OPEN、Rule 8拒否・既存DEFERRED維持 | trigger判断前の実装 / fixture追加対象にしない |
| B2、SAML / Edge、E1 | Customer入力・実受入・release待ち | 実条件・作業量 / 費用OPEN | 情報取得とgap整理を継続。先行限定範囲・実装総額に含めない |
| Authorization Server / Oracle | 個別trigger・必須DoD外 | 作業量・環境OPEN | 個別Gateまで開始しない |

先行順の提案は、S1中心 → C2の独立準備 → C3 / C4の連携設計 → B1 → D2 / D3 / D1同期の条件別選択である。実装順は選定・review・環境条件に応じてOwnerが判断する。S1の保留から他packageの実装へ自動切替しない。

## 4. Gate経路の比較と差分

| 経路 | 候補対象と開始条件 | 正本への差分・判断 |
|---|---|---|
| 現行経路を維持 | 計画・設計を継続し、実チーム受入・AR-D10 / 統合計画・Gate P4-AR・P4-STARTの条件に従って開始 | 新しい例外は設けない。S1候補化の承認だけで現行のproduction開始条件を変更しない |
| 既存のLevel 2限定P4-F案を具体化 | 最初はA1 / A2 / D1のS1だけを対象に限定開始を別審査 | P4-Fは既に設置済みではない。CP-F0残項目、F-4 / F-5、作業8・9、Gate設置 / 通過と個別blocking reviewを整合 |
| 案2：顧客非依存packageへP4-Fを再定義 | S1を中心とし、条件が揃った独立packageを個別の開始単位へ追加し得る | 対象ごとのDoD・Owner・実測 / 上限・環境・停止点、P4-AR計画 / AGENTS / Skill / 見直し草案 / F-5 / P4-F資料の改訂・承認が必要 |

**統合案の提案:** 案2を将来の独立package選定に使う検討軸として保持しつつ、最初の限定開始対象はS1へ絞って材料を仕上げる。C2等は次の追加候補として計画を整え、S1と全独立候補を一括で開始しない。これによりOwner＋Codexの順次実施と段階確認に合わせる。案2の正式採用を本書で決定しない。

案2が必要か、初回は既存Level 2限定P4-F案で足りるかもORで比較する。条件不足なら現行経路のまま計画を進める。P4-Fを設ける場合でも、P4-AR6 / AR-D10 / Gate P4-AR、Phase 4全体開始・正式受渡しは別の未完了項目として残す。

### 4.1 判断後に作業9へ渡す改訂項目

- 見直し草案：選定範囲・順序・CP-F0判断をR8以降へ記録し、既存R1〜R7を保持する。
- P4-AR計画 / AGENTS / project-overview Skill：採用した限定Gateの対象・条件・停止点だけを例外として整合する。実チーム受入・remote個別承認を維持する。
- F-5 / P4-F提案・判定資料：当時の暫定S0と今回の判断を区別し、A1 / A2 / D1限定か独立package追加か、担当実体・上限・Evidenceを合わせる。
- DoD / ADR：判定解釈の確認と、DoD変更が必要な場合の正式改訂を区別する。S0延期や手動復旧の了解から核心DoDを削除しない。

本書ではこれらの正本を改訂しない。具体的採用結果を入力として作業9で差分を示す。

## 5. 共通費・概算と実施可能性の統合

S1・他packageの数値は低確度の残技術作業案であり、Ownerの確認・実演参加時間、外部待ち・platform / CI費は別枠OPEN。既存Phase 0人日を今回の予算やAI稼働へ転用しない。

| 共有対象 | 配賦方法の候補 | 未確定事項 |
|---|---|---|
| S1 W07 / D1同期 | tracer・sink準備を先行する一方へ一度だけ計上し、もう一方は追加の適用・実演分を計上 | 共通採用範囲・同期残差 |
| S1パージ / C3 | 単一実行基盤の契約・準備を一度だけ計上し、publication保持とjob実証の差分を分離 | 正式Batch依存、基盤Owner・lock関係 |
| C3 / C4 | file受付・確定 / 結果照合・harnessを担当packageへ配賦 | job / file操作選定、Storage範囲 |
| Reference artifact / 環境 / 回帰 | 同じsourceと環境なら共通setupを抽出し、変更後の必要な回帰と証拠を個別計上 | 採用範囲・setup再利用可否 |

この配賦が未確定のため、数値の単純合計をPhase 4総額・予算上限として提示しない。初回対象をS1だけにする場合でも176〜344時間を確定予算とせず、詳細設計後の再見積と段階上限を判断する。

## 6. ORで確認する事項と未成立の開始条件

| ID | ORの論点 | 本案と残る入力 |
|---|---|---|
| OR-1 | S1の具体的検証範囲と残判断の工程 | §2のReference / stub範囲を選ぶか。安全方式・schema / API等はblocking reviewへ割り当て、工程別の必須条件を定める |
| OR-2 | 手動復旧と核心DoDの解釈 | 運用者の総合確認を前提に、4-2許容時間、4-3の受理一意・適用範囲を明示。実provider保証はCustomer採用へ残す |
| OR-3 | 最初の先行対象と順序 | S1中心・初回対象限定、C2等を次の準備候補とする案を比較。未成立対象は保留し、独立計画を続ける |
| OR-4 | Gate経路と正本改訂 | 現行経路、Level 2限定P4-F案、案2を比較し、必要な作業8・9と改訂・承認点を指定 |
| OR-5 | 稼働・環境・概算 / 上限 | Owner＋Codexの各段階分担 / review枠、実環境、詳細再見積・上限・費用を取得。概算と上限案の文書確認を予算承認へ変換しない |
| OR-6 | 未達・適用外・待ち項目 | S0再計画、MyBatis / Customer / release / optional待ち、未算定分、追加case・残リスクをどの時点で再評価するか |

ORの議論材料は統合したが、具体的開始を承認できる条件が全て揃ったとは宣言しない。選定案への確認と、環境・上限等が揃った後の限定開始判定は区別する。ORの結果は判断者・日付・対象・条件・保留理由を記録し、必要な正本改訂へ渡す。

## 7. 現在の作業状態と次の出口

**Owner受領と後続材料（2026-10-05）:** Ownerは本書を確認し、統合順序案を受領して初回対象・Gate経路・残判断へ進む意向を示した。[初回S1対象・Gate経路・残判断の具体化案](phase4-initial-scope-gate-and-open-decisions-draft-20261005.md)に限定範囲・経路の推奨案と判断工程を整理した。受領・継続指示は具体的採用・Gate承認ではない。

作業4bは対象・順序・Gate比較の統合案、作業5はS1具体化とS0 / S2・核心DoDの残判断、作業7はS1と他package分解・概算・実演材料が揃った段階である。選定対象の詳細設計・実環境・数値上限、未算定・共通費、OR判断が残り、4b / 5 / 7全体・S1・ORを完了扱いにしない。

次の出口は、Ownerによる本統合案の確認を踏まえ、初回対象・Gate経路・残判断の工程を絞り、その対象の詳細設計・環境条件・再見積 / 上限を具体化することである。議論のために全packageの実装を先行しない。CustomerへのFAQ・入力受付は継続する。

今回、計画文書と導線だけを更新した。production / Tooling code、POM・migration・Rules・CI、Batch Repository、既存Gate規定・remoteは変更していない。
