# 初回S1対象・Gate経路・残判断の具体化案（2026-10-05）

> **現在の参照先（2026-10-05）:** 本書のGate・正式開始判断は引き続き残条件。後続方式票B6で承認された初回Tooling局所作成／実行とは分ける。現在の次作業はpreflightとL1作成であり、下記のread-only調査順は当時の記録。 [最新引継ぎ](phase4-s1-local-verification-start-handoff-20261005.md)／[承認済み方式票](phase4-s1-method-ballot-and-local-verification-contract-20261005.md)。

**状態:** DRAFT / OR-1〜6の判断案。統合順序案の受領と本段階への継続指示を記録。対象・Gate経路の正式採用、Gate設置 / 通過、blocking review・実装開始は未承認。
**Ownership:** Framework側の計画資料。Owner一人＋Codexで、サブエージェントを使わず順次・同期的に進める。
**確認baseline:** `docs/daily-development-workflow` / `eaf944d`。他package分解・統合判断材料等は未commit差分。文書照合のみで、新しい実行検証・環境確認はない。
**入力:** [統合判断材料](phase4-forward-scope-cpf0-integrated-review-draft-20261005.md)、[P4-F提案](KOIKI-JavaWeb-FW_Phase4_P4-F限定開始Gate提案_v0.1.md)、[PL2判定資料](KOIKI-JavaWeb-FW_Phase4_PL2_P4-F判定資料_v0.1.md)、[S1実施可能性案](phase4-s1-execution-resources-and-sequence-draft-20261005.md)。

## 1. 初回対象の判断案

初回の限定開始候補はS1のA1 / A2 / D1非同期だけとすることを推奨する。Referenceのexpense承認から値だけのeventを発行し、Reference-owned notificationがToolingの模擬providerへ送る用途限定実証である。

| 対象 | 初回に含める候補 | 開始前の確認 |
|---|---|---|
| A1 | Application-owned publication構成、JDBC＋UPDATE第一候補、選別・認可 / Audit付き手動復旧、単一実行パージ、必要なRule差分のreview | store / schema・既存kkref履歴と二階層契約、依存・Level選択 / API互換、通常listener・lock喪失時の安全条件 |
| A2 | Reference承認event・notification・通知log・provider Adapterとstubでの冪等実演 | event識別 / payloadと発行位置、安定key・受理不明、DB / provider確定順、既存承認 / Security / Audit回帰 |
| D1非同期 | FAILED件数・滞留、初回 / 別process再送の相関、実trace・log / metric / alertの照合 | 滞留起点・schema所有、tracer / sink、PII・cardinality、運用者が確認できる導線 |
| Tooling | 故障注入・provider stub・sink・証拠採取・cleanupと人の判断記録 | 正式成果物への故障switch混入を避ける方式、process停止の独立証拠、隔離環境と実施権限 |

C3の全job・C4のfile連携・正式Batch依存、C2・B1・D2・D3・D1同期全体、S2共通提供、Customer結合・MyBatis・release・remoteは初回へ一括包含しない。S1パージに必要な実行契約はA1で扱い、Batch全体を前提にしない。必要な範囲追加が判明したら差分をreviewする。

初回対象の了解は全変更の包括承認ではない。Framework / Reference / Toolingの各変更・開始点はblocking reviewと限定Gate条件へ追跡する。

## 2. Gate経路の推奨案

**初回は既存の「Level 2限定P4-F案」をS1へ合わせて具体化する経路を推奨する。** 初回対象がA1 / A2 / D1なら、独立package全体へP4-Fを再定義する案2を初回から採用する必然性はない。案2はC2等を後で限定開始へ追加する必要が生じた時の別判断として保持する。

P4-Fは提案中であり、現行の承認済みGateではない。この経路を採用する場合も、P4-AR計画・AGENTS / Skill・見直し草案・F-5 / P4-F資料の例外・条件を作業9で整合し、Ownerの正式承認を得る必要がある。正本改訂とGate判定の記録がない間は現行Gateが適用される。

| 順序・工程 | 整える材料と判断 | 境界 |
|---|---|---|
| ORの対象 / 経路確認 | 初回S1限定、既存Level 2限定P4-F案の具体化、案2追加対象の保留、残判断の工程を確認 | 今回は判断案を提示した段階。OR全体の結論・限定開始ではない |
| 作業6 / 8 / 9の計画・差分 | 対象commit point、F-1〜F-5、担当実体、環境・上限、未解決安全条件、正本差分と停止点を揃える | ORの結果に合わせて準備。P4-F提出条件が未成立ならREWORK材料として残す |
| P4-Fとblocking reviewの整合 | 正式な設置 / 判定、A1のstore・schema / Rules・依存・復旧、A2 / D1個別reviewの対象・条件を明記 | 既存P4-F提案§3の「P4-F前のblocking review」とPL2資料のCP-F1以降を突合し、前置必須reviewと後続の個別開始条件を作業8・9で確定。工程を推測で入れ替えない |
| 承認commit pointの実行 | A1 / D1設計 → A1成立Evidence → A2実装 → 統合実演の既存方針へ対応 | 設置・通過と個別review条件が満たされた対象だけ開始。A2を先行しない |

P4-AR6・AR-D10・Gate P4-AR、Phase 4全体開始、正式受渡しは未完了のまま残す。P4-FでCustomerの実provider / 当番 / SLA・案件環境を確定したとは扱わない。

## 3. 残判断を閉じる工程と出口

| ID / 関連OR | 残判断 | 次に具体化する材料 | 判断工程・出口 |
|---|---|---|---|
| J1 / OR-1・3・4 | 初回対象・Gate経路 | §1 / 2の限定範囲、除外・追加判断、現行経路との変更差分 | Ownerが対象・経路を明示し、作業6 / 8 / 9の準備範囲へ渡す |
| J2 / OR-1 | 保存・配置・規約 | W01：JDBC / JPA選定、UPDATE、kkref / Framework migration履歴、実効依存、Rule 28 / 29・API互換、変更module | A1 review材料。正式DDL・API・依存をreview前に追加しない |
| J3 / OR-1・2 | 停止確認と競合・provider境界 | W02：process識別 / 世代・停止 / drain、通常listenerと復旧者、lock喪失窓、安定key・受理照合・DB確定順 | S-01〜07、負例E04〜14に追跡するA1 / A2 review。未成立時は再送を保留 |
| J4 / OR-2 | 核心DoD解釈 | 人が確認してから復旧する4-2の開始 / 終了時点・許容時間、4-3のstub受理一意と未検証範囲 | Ownerが明示。手動復旧の方針だけで4-2を満たしたとは判定しない |
| J5 / OR-1・5 | 非HTTP運用とAudit | 復旧主体・権限、送信前 / 後のAudit失敗、上限 / timeout・解除、連絡・保留引継ぎ | Security / A1 reviewへ。Owner兼務の確認・判断とCodex支援の操作範囲を記録 |
| J6 / OR-1・5 | 観測・保持・パージ | FAILED滞留起点、実tracer / sink、alertと人の確認導線、key寿命・通知log / publication保持、競合時の保全 | D1 / A1 reviewへ。C3共通基盤の利用要否を別判断 |
| J7 / OR-5 | 実施可能性・上限 | ローカル隔離構成、process / DB・権限・資源、cleanup、担当稼働、W01 / W02後の再見積・段階上限 | F-4提出条件へ。176〜344時間や48時間の案を承認済み上限にしない |
| J8 / OR-4・6 | 正本・Gateの整合と未達 | F-1〜F-5、前置review / 後続開始条件、R8以降の記録、S0 / S2・Customer / trigger待ちの再評価点 | 作業8・9へ。Owner判断と改訂承認前にAGENTS / Gate規定を変更しない |

J4 / J5 / J7の数値・稼働は未取得。便宜的な分・回数や日程を作って埋めない。必要な実環境調査・試行は対象と承認範囲を別途照合してから行う。

## 4. 次に行う具体的作業

**後続の最小構成材料（2026-10-05）:** [採用候補・変更一覧・個別開始範囲](phase4-s1-minimum-change-and-start-scope-draft-20261005.md)で許可 / 消費の最小方式・Tier等をK票、変更をCH票、開始範囲をST-A〜Eへ統合した。ST番号は既存CP / Gateの改訂ではない。J1 / J8・OR・正式開始判断は未成立で、今回資料作成からTooling / Reference実装開始を認定しない。

まずJ2 / J3の**read-only詳細設計材料**を揃える。W01の現行source・依存宣言・migration履歴・Rule互換を調べ、W02の停止確認・通常listener競合・key / 受理不明・DB確定順の候補を比較する。候補採用・復旧条件の最終判断はOwnerへ戻す。

続いてJ4〜J7を設計結果へ対応付け、実環境・稼働・再見積 / 上限の不足を閉じ、J8の作業8・9へ渡す。作業6 / 8 / 9への正式移行はORの結果に従う。初回S1準備にC2等の実装を混在させない。

今回の文書作成と調査方針は、production / Tooling実装、正式DDL / API / dependency追加、Docker / Maven新規実行、Gate通過・remoteを許可する記録ではない。

## 5. Owner確認記録

2026-10-05、Ownerは統合判断材料を確認し、「統合順序案を受領して、初回対象・Gate経路・残判断へと進めます」と示した。受領と次段階への継続指示として記録する。本書の初回S1限定・既存Level 2限定P4-F経路はAgentの推奨案であり、現時点で採用済みとは記載しない。採用判断・条件・保留はOwnerの明示後に記録する。
