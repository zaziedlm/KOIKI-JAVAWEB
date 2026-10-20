# Phase4 継続と設計実装レビュー対応の統合作業計画

作成日：2026-10-20  
Ownership：Framework側の計画文書。実装の所有者は各タスクに記載する。  
状態：**進め方はOwner合意済み／作業分解は計画案**。本書作成は実装開始・検証実行・DoD受入を兼ねない。

## 1. 合意した進め方と計画の位置

Ownerは「**P4-B1の調査・設計は進めつつ、直接影響する問題だけを実装・受入より先に解決する**」方針に合意し、レビューとPhase4残タスクを双方組み込んだ作業計画化を指示した。レビュー全件の解消をPhase4継続の条件にしない。

本書は次の二つを接続する実作業の台帳とする。既存のDoD、受入結果、保留理由の正本を置き換えない。

- [mainの設計・実装レビュー](../architecture/validation/main-framework-design-implementation-review-20261020.md)：F1〜F7、H1、Claude所見との照合と限定再現結果。
- [Phase4残タスク棚卸し](phase4-continuation-task-inventory-20261016.md)：S1の区切り、他package12区分、採用支援5区分、入力待ちと既存成果。

当初の対象・依存は[Phase4見直し草案§4.1・§7.1](KOIKI-JavaWeb-FW_Phase4実施計画_見直し草案_v0.1.md)、後続の作業分解は[他package分解](phase4-other-packages-breakdown-draft-20261005.md)を参照する。DoD 4-8／4-9をSession SPA＋MVCで確認する判断は2026-09-26に承認済みであり、同じ方針を改めて未決定に戻さない。

計画確認時のlocal sourceは `main` / `521572d195a5a854c57ef58b2b61c59ce5c5a0b8`。前回レビュー文書・probe2件は未commitのまま保持した。remote同期、tag、CIの現在状態を今回新たに確認したものではない。

### 状態の読み方

| 状態 | 意味 |
| --- | --- |
| 調査・設計対象 | 今回合意した方向で具体化する。実装開始を意味しない |
| 計画 | 対象・出口を定めた後続作業。まだ実行・受入していない |
| 入力待ち | 指定した案件・運用・platform入力を取得してから具体化する |
| 保留維持 | 既存の延期・保留判断を維持し、再開条件成立時に選び直す |
| 完了 | 当該成果と検証・必要な受入記録がそろった場合だけ記入する |

本書のWBS行は作業の区切りで、同一人数・工数を表すものではない。Owner＋Codexで順次作業し、副Agentは使わない。「並行」は設計を修正完了待ちにしない依存関係を意味する。

## 2. 直近の順序と合流点

```text
I-01 現行source・変更範囲・検証入口の確認
  ├─ B1-01 既存との差分
  │    → B1-02 Session／MVC／Bearer境界設計
  │    → B1-03 最小React・build／配置・検証設計
  │
  └─ R-00 F5検証対象の確認・是正
       → R-01 F1例外境界
       → R-02 F2 Cookie適用
       → R-03 F3対象認可順序
                     ↓
       B1-04 設計と基盤是正結果の合流・実装範囲確定
                     ↓
       B1-05 Session API → B1-06 最小SPA
                     ↓
       B1-07 統合検証 → B1-08 受入・次package選定
```

R-01〜03の間に技術的な直列依存はない。上図は一人の作業順の案であり、B1設計の確認事項に合わせて順を変えられる。R-00はarchitecture検証を比較可能にする準備で、B1-01〜03の文書・source調査を止めない。

**B1の新規実装へ進む合流点**では、R-00〜03の選択範囲の結果とB1-01〜03の設計をそろえる。F4はB1-02で扱い、Framework既定値変更を必須にしない。F6／F7、H1、非同期CI、S1全体の未達はこの合流点の一律条件にしない。

## 3. 直近の作業タスク

### 3.1 共通準備と基盤是正

以下の修正行は計画であり、本書ではproduction／既存testを変更していない。限定probeをそのまま正式な回帰testの代替にはしない。

| ID／状態 | 元項目・主担当Ownership | 作業と対象 | 成果・完了条件 | 依存 |
| --- | --- | --- | --- | --- |
| I-01／調査・設計対象 | 共通／Tooling | 作業branch・source、未commit文書、利用するartifactとtest入口を確認。通常／専用asyncの区別、変更対象と直接回帰を一覧化する | 作業source、対象path、実行予定集合、資源・時間・cleanup条件が追える。過去PASSを現sourceの結果にしない | 本計画 |
| R-00／限定変更・検証完了 | F5／Reference test | `ReferenceArchitectureTest`1 fileの両importへ`DO_NOT_INCLUDE_TESTS`を追加。fixture配置・Rulesは維持 | clean compile＋既存2件／規約負例5件PASS。両importともtest由来0。[限定結果](../architecture/validation/phase4-r00-production-import-results-20261020.md)にsource／XMLを対応付け | I-01成立。過去の15違反を現在の失敗とせず、検査責務を明示 |
| R-01／計画 | F1／Framework API Starter | 汎用AdviceとSecurity・HTTP例外の責任を整理し、標準処理へ伝播する修正と合成testを作る。Reference局所再throwの必要性も照合 | 認証済み拒否403、未認証の経路別応答、独自404、予期せぬ500、MVC／HTMX／APIの応答が意図どおり。回避策除去は検証成立時だけ | I-01、比較するarchitecture検証はR-00後 |
| R-02／計画 | F2／Reference identity構成 | 独自CookieSerializerをBoot管理へ戻すか設定反映するかを選び、logout customizerとの生成順も確認・修正 | 明示Secure／SameSiteがSet-Cookieへ反映。HTTPS判定の有無とlogout削除Cookieのname／pathを確認。property値の確認だけで閉じない | I-01、B1-02のCookie／logout要求と照合 |
| R-03／計画 | F3／Reference expense | owner／approver scopeの確認をversion照合より前にする。対象外の情報を例外差から漏らさず、既存競合処理を維持 | scope外の一致／不一致／負versionと不存在が秘匿契約どおり。scope内競合は409相当。拒否時の更新・Audit・eventなしを確認 | I-01、B1-01の対象操作と照合 |

修正ごとの証拠は既存 `docs/architecture/validation/` の記録方式を使う。内部修正と公開property／API変更を混ぜず、変更が後者へ広がる場合に、その差分の契約reviewを行う。単に同じStarterだからという理由でF1とF4の採否を一括化しない。

### 3.2 P4-B1の調査から受入まで

| ID／状態 | 主担当Ownership | 作業と対象 | 成果・完了条件 | 依存 |
| --- | --- | --- | --- | --- |
| B1-01／調査・設計対象 | Reference | DoD 4-8／4-9に既存MVC、Bearer REST、Session、CSRF、expense Use Case・DTO・testを対応付ける。参照＋更新の代表操作を選ぶ | 「存在するもの／再利用／変更／不足」の差分表。SPA対象操作と対象外を確定。Customer BFFを代替としない | I-01。R-00〜03の完了不要 |
| B1-02／調査・設計対象 | Reference、Framework Security契約review | Session SPA APIとMVCと既存Bearerのmatcher・order・認証応答・CSRF取得／送信／更新・logout・Cookie・権限／scopeを設計。F1〜F4を対応付ける | route別のSecurity／CSRF／error表。API versionはF4の既存回避策継続を基準案とし、変更が必要な場合だけ根拠を示す。default deny維持 | B1-01。R-01〜03の設計へ入力 |
| B1-03／調査・設計対象 | Reference frontend、Tooling検証 | 最小Reactの画面、Session APIとの接続、同一originでのbuild／配置、依存・lockfile・package方法、実browserとDB／Audit照合を具体化 | 画面・操作一覧、build／配布構成、依存差分、検証表、概算と実行条件。frontendの詳細方式は既存資材を調べて決める | B1-01〜02。基盤修正の完了不要 |
| B1-04／計画 | Framework計画・Owner判断 | B1-01〜03とR-00〜03の結果を合流し、最小の実装差分・必要な契約変更・検証範囲を確定 | 実装対象、完了条件、必要入力・環境、資源／時間／停止・cleanup、適用する開始判断が一つの設計記録から追える | B1-01〜03、R-00〜03。F6／F7／H1／S1完遂は前提外 |
| B1-05／計画 | Reference backend | 選定したSession API、Security／CSRF接続、API DTO／errorを実装。同じApplication Use Caseを利用し、MVC FormとDTOを共有しない | API成功・認証／権限／CSRF／scope／競合の拒否test、既存BearerとMVCの直接回帰が成立 | B1-04と当該実装開始条件 |
| B1-06／計画 | Reference frontend | 選定した最小SPAを実装し、login後の参照・更新、失効／拒否／競合の表示をつなぐ | package済みReference上で代表操作が実行でき、MVCで同じ業務結果を確認できる | B1-05。画面構成の設計はB1-03で先行 |
| B1-07／計画 | Tooling、Reference受入 | 実browser、API、DB、Business Audit、logで統合実演。通常構成と既存経路を保護し、資源をcleanup | 下記DoD表のEvidence、直接回帰、選定した通常検証、cleanupがそろう。未達は結果として明示 | B1-05〜06。実行集合・上限はB1-04で確定 |
| B1-08／計画 | Framework計画・Owner受入 | DoD 4-8／4-9の成立範囲、残課題、利用方法を記録し、後続packageを選ぶ | 個別の受入記録、Journeyへの利用条件、次packageと必要入力。Phase4全体完了とは分ける | B1-07 |

### 3.3 B1受入へ結ぶ検証

| 要求 | 必要な観測 | 関連レビュー |
| --- | --- | --- |
| DoD 4-8：SPAのCookie Session認証とCSRF double-submit | login後の参照／更新、token取得・送信、欠落／不正拒否、失効・logout後、Cookie属性とCSRF tokenの役割分離 | F1・F2 |
| DoD 4-9：同一アプリ内のMVC／SPAと経路別CSRF | MVC formとSPA APIの成功・拒否、matcher優先度、version、既存Bearer経路保護、未定義経路deny | F1・F4 |
| 対象認可・競合 | scope外／別user／不存在の秘匿、権限不足、scope内の競合応答、画面回復 | F1・F3 |
| 同じ業務結果・副作用境界 | 選定操作のDB状態とAuditをMVC／SPAで突合。拒否では更新・意図しないeventを生まない | F3 |
| 検証の信頼性 | source／artifact一致、architecture対象、実行／skip区別、通常構成回帰とcleanup | F5 |

新規session APIの具体path、CSRF実装の詳細、frontend依存version、test件数はB1-01〜03の出力とする。未調査の値を本計画で固定しない。通常rootを修正ごとに繰り返さず、影響別の局所検証と合流後の統合検証を分ける。

## 4. レビュー項目を後続作業へ組み込む

| ID／元項目 | 作業単位・主担当Ownership | 着手契機 | 出口・B1との関係 |
| --- | --- | --- | --- |
| R-04／F4 | B1-02に統合。API／MVC共存設定と利用条件をReferenceで確認し、Framework既定変更の要否を判断 | B1設計 | 回避策維持または契約変更の根拠と検証。別の重複修正タスクを作らない |
| R-05／F6 | Framework＋Tooling：最小Customer Entity／Repositoryでscan範囲を実証→拡張実装か明示登録契約を選択→導入例・診断を更新 | 新規Consumer構成を確認する時。AD-01またはE1の該当構成へ接続 | Customer独自EntityとFramework Entityが共存する。現行Reference B1を止めない |
| R-06／F7 | Framework＋Tooling：同一DataSource／schema、migration専用接続、strategy置換の対応表→採用構成だけDB検証→拒否／対応と責任を明文化 | Customer／Container／BatchのDB構成選定時 | 採用構成のmigration順・実行先・権限・historyを確認。baseline=falseへの単純変更はしない。非標準構成を使わないB1の前提にしない |
| R-07／H1 | Reference：部門無効化とcreateDraft並行時の業務保証を確認→必要なら2 transactionの限定再現→結果に応じ修正案 | 当該master／expense操作の変更・利用条件確認時 | 再現結果または明示した保証範囲。未実証の懸念を確定不具合として修正しない。B1の一律前提外 |
| R-08／Claude R6-1 | Tooling：expense/event/notification境界変更時に専用回帰の必要部分を選定。恒常CI追加は別に評価 | 具体的な影響差分またはCI運用要求が生じた時 | 選択回帰と通常／専用分離の根拠。job追加はworkflow個別判断。非同期全件再開を前提にしない |
| R-09／Claude R10-1 | Framework文書：各修正で確定した制約を既存Journey／開発者ガイドへ反映 | R-01〜06の各出口、B1-08 | Cookie、例外、scan、Flywayの採用構成へ辿れる。全レビュー終了を待つ巨大文書作業にしない |
| R-10／Claude R10-2・3 | Framework計画：内部修正、公開契約、実環境／remote操作をタスク単位で区別 | 各実装範囲確定時 | 必要な判断だけを記録。本計画から新しい包括Gateや恒常的な承認工程を増設しない |

R-05／06はレビューと採用支援の共通作業として一度だけ計上し、別packageに同じ検証を複製しない。既存Evidenceで同一条件が満たされる場合はその適用範囲を照合して使う。

## 5. B1以降のPhase4作業台帳

### 5.1 棚卸しの12テーマとの対応と具体化の範囲

[残タスク棚卸し§9.2](phase4-continuation-task-inventory-20261016.md#92-phase4のテーマと着手条件)の**12テーマはすべて本計画へ組み込み済み**。ただし、詳細化の程度は異なる。以下は棚卸しと同じ順序の対応表であり、実施順を示すものではない。

| 棚卸し§9.2のテーマ | 本計画内の対応 | 現在の具体化 |
| --- | --- | --- |
| P4-B1 Session SPA＋MVC | §3、B1-01〜08 | 調査→設計→実装→検証→受入まで分解。レビュー是正との依存・合流点も設定 |
| P4-B2 Customer REST・BFF | §5.2、B2-01〜03 | 入力取得→gap審査→結合Evidence受入 |
| P4-C0/C1 MyBatis・accounting | §5.2、C0-01〜02／C1-01〜02 | 採用判断→設計review→実装→検証。trigger待ち |
| P4-C2 外部API耐障害性 | §5.2、C2-01〜03 | 代表操作・失敗契約→実装→障害実演 |
| P4-C3 non-Web Batch | §5.2、C3-01〜03 | 既存基盤確認→実装→二重起動・再実行検証 |
| P4-C4 File・Object Storage | §5.2、C4-01〜03 | 確定境界設計→実装→失敗・cleanup検証 |
| P4-D1 同期処理の観測 | §5.2、D1S-01〜03 | 観測契約→接続→相関・障害実演 |
| P4-D2 Virtual Threads | §5.2、D2-01〜03 | 比較条件→検証→CI・ガイド |
| P4-D3 Container・ECS | §5.2、D3-01〜03 | 起動契約→local検証→実ECS |
| P4-04／P4-EDGE SAML・認証境界 | §5.2、EDGE-01〜03 | 認証境界確認→採否→必要Adapter・検証 |
| P4-E1 正式受渡し・release | §5.2、E1-01〜03 | 対象確定→配布・診断手順→受入 |
| P4-OPT 条件付き拡張 | §5.2、OPT-01〜02 | 明示要求→採否・対象判断。採用後の実装分解は未実施 |

全テーマについて、作業段階・Ownership・必要入力・出口・関連レビューを整理している。一方、個々の作業をそのまま実行できる詳細計画までは確定していない。特にB1以外は、対象ファイル、具体的な実装仕様、検証ケース・コマンド、工数・実施時期、テーマ間の実施順が未確定である。B1も対象ファイル・仕様・検証・工数などを調査・設計で具体化する部分が残る。

現在の計画は、**全テーマを収録した作業台帳と、直近B1・レビュー是正の詳細分解**である。Phase4全体を同じ粒度で実行計画化し終えた状態ではなく、次に選ぶテーマから詳細化する。上表は§3・§5.2の索引であり、別タスクとして二重計上しない。

### 5.2 各テーマの作業単位と着手条件

以下は棚卸し§4／§9の全テーマを具体作業へ接続したもの。**B1後の実施順は未確定**で、B1-08で必要性・入力・検証可能性から次の一つを選ぶ。各行の01→02→03は同一テーマ内の順序であり、テーマ間の直列順序ではない。

| 元package／状態 | 作業単位 | 主担当Ownership・必要入力 | 完了条件／レビューとの接続 |
| --- | --- | --- | --- |
| P4-B1／直近 | B1-01〜08 | §3参照 | DoD 4-8／4-9。F1〜F5を対応付け済み |
| P4-C2／後続候補 | C2-01 代表外部操作と失敗契約→02 Port／Adapterと限定retry・timeout・流量制御→03 stub障害・受理不明・資源の実演 | Reference／Tooling。副作用の意味、上限、library採否 | DoD 4-7。F1の応答境界を再利用。S1 provider保証を転用しない |
| P4-C3／後続候補 | C3-01 別Batch RepositoryのHEAD・互換性とjob選定→02 non-Web起動・永続metadata・単一実行→03 二重起動・停止後restart／rerun・Audit実演 | Reference／Tooling、Batchは別所有。job・運用主体・DB入力 | DoD 4-10。R-06はDB構成が該当する場合に接続。S1パージ完了と切り離す |
| P4-C4／後続候補 | C4-01 代表操作・保存／DB確定境界→02 Port／Adapterと受付／公開→03 重複・path・権限・容量・途中停止・cleanup | Reference／Tooling。storage／format入力。C3連携は必要時だけ | local fileとObject Storageの実証範囲を区別。共有harnessは一度だけ計上 |
| P4-D1同期／後続候補 | D1S-01 同期trace／log／metricとsink契約→02 tracer／exporter接続→03 相関・context非漏えい・sink障害実演 | Framework／Tooling。sink・保持・運用入力 | 同期OpenTelemetry成果物のEvidence。S1非同期のDoD 4-12を閉じない |
| P4-D2／後続候補 | D2-01 Java21 build／25 opt-inと比較条件→02 同じ操作の有効／無効・context／pool比較→03 CI系統と利用ガイド | Framework／Tooling。runner・負荷・時間・workflow判断 | DoD 4-11は実CI結果で判定。既定無効維持、local比較だけで完了にしない |
| P4-D3／後続候補＋platform入力待ち | D3-01 固定JAR／image・起動契約→02 local containerのhealth／診断／停止再起動→03 実ECS差分と実演 | Reference／Tooling＋platform。secret・network・費用・権限 | local／ECS別Evidence。F2の実Cookie条件とR-06のmigration構成を該当時に照合 |
| P4-C0／C1／保留維持 | C0-01 adoption trigger確認→02 分離モデル・規約／依存review→C1-01 accounting実装→02 競合・rollback・共通error実演 | Architecture／Framework規約、Reference。MyBatis採用入力 | DoD 4-6。C0前はRule8拒否維持。元計画のA1連携依存と独立検証可能部分を開始時に区別し、S1全体を自動再開しない |
| P4-B2／入力待ち | B2-01 Customer API／BFF／認証選択取得→02 gap審査→03 Customer結合Evidenceの受入 | Customer主導、Framework契約review。案件／IdP／環境入力 | B1の代替DoDにしない。F1／F4〜F7のうち採用構成に関係する項目を照合 |
| P4-04／P4-EDGE／入力待ち | EDGE-01 認証終端と到達credential確認→02 SAML／broker／OIDC等の採否→03 必要Adapterと実環境検証 | Customer／platform＋Architecture | SAMLの当初成果物としての採否を明記。ALB採用だけでAdapter追加を決めない |
| P4-E1／入力待ち | E1-01 対象artifact／version／checksum／support→02 配布・受取・診断手順→03 独立Consumerと実チーム受入 | Framework release＋platform＋Customer | 内部snapshot／checkpointと正式受渡しを分離。R-05／06を採用構成に照合。Phase5全体へ広げない |
| P4-OPT／保留維持 | OPT-01 Authorization Server／Oracleの明示要求→02 採否・対象・実環境・費用の個別判断 | Architecture＋Customer／platform | 必須DoD外。未採用を完了へ置き換えない |

### 5.3 非同期系の残課題

P4-A1／A2・D1非同期とS1-01〜10は、[棚卸し§6〜8](phase4-continuation-task-inventory-20261016.md)の限定受入・人手HOLD・未達を保持する。次の三つを区別して管理する。

| 作業ID／状態 | 次の行為が発生する条件 | 作業・出口 |
| --- | --- | --- |
| S1-T1／保留維持 | 実案件の不足・不具合・復旧要求が具体化 | 該当S1-IDと現在の証拠を照合し、必要範囲だけ再開判断。新GET・復旧機構・全件実証をB1に抱き合わせない |
| S1-T2／入力待ち | 実provider・停止統制・担当・記録先・保持等の運用入力が得られる | 棚卸し§7.2の該当OPENを更新し、限定結果との不足を明記。文書方針を運用成立へ読み替えない |
| S1-T3／最終判定時 | Phase4全体の成果・DoDを判定する時 | 4-1〜4-5／4-12の限定成立と未達を対応付ける。採否・延期・DoD変更は明示判断。B1完了で自動closeしない |

## 6. 横断する採用支援

| 既存ID／状態 | 作業単位 | 必要入力・出口 | レビュー／機能タスクとの関係 |
| --- | --- | --- | --- |
| AD-01／実チーム入力待ち | 01 worksheetと構成確認→02 build/run/operation/diagnosis Evidence照合→03 findingと受入判断 | Customer実行結果。Framework準備と実チーム受入を分ける | R-05／06、B2、E1。共通Consumer検証を重複計上しない |
| AD-02／実チーム入力待ち | 01 Linux／WSL試行結果取得→02 失敗条件の再現と環境ガイド更新 | チーム環境の非機密Evidence。Windows PASSを代替にしない | B1 frontendの環境要件はB1-03で確認し、AD-02全体の完了は待たない |
| AD-03／方式採用待ち | 01 開発専用Maven stage方式判断→02 IDE／CLI切替・更新・診断Tooling→03 試用 | 既存比較案を入力に恒常方式を選択 | I-01で必要な局所artifact準備と恒常方式を分ける |
| AD-04／保留維持 | 01 Customer POM検討時期と共有結果確認→02 再開判断成立時だけ誤選択検証・例外運用を具体化 | Owner了解済み検証保留の解除判断 | F6／F7があることだけで再開しない |
| AD-05／恒常方式判断待ち | 01 用途別artifact一覧・不足診断→02 採用範囲の準備／offline確認→03 更新・復旧手順 | 選択Toolingの限定LOCAL PASSを入力に必要分を判断 | I-01の不足が生じた対象だけ対応。全Tooling準備をB1前提にしない |

## 7. 作業の管理と次の着手点

### 7.1 成果の区切り

| 区切り | まとめるもの | 次へ渡すもの |
| --- | --- | --- |
| 設計整理 | B1-01〜03の差分・route表・frontend／検証構成 | B1-04の実装判断材料。F1〜03修正を待たず作成 |
| 基盤是正 | R-00、R-01、R-02、R-03を各問題単位で整理 | 個別差分と限定結果。共通回帰は合流時にまとめる |
| B1機能 | B1-05〜06の選定操作とUI | packageされた検証対象 |
| B1受入 | B1-07〜08 | DoD Evidence、残課題、次packageの選定 |

この区切りはレビューしやすい差分の単位であり、commit／PR操作の承認ではない。履歴を固定する際にはsourceと結果の対応を残す。

### 7.2 実行前に埋める事項

設計時点で必要な対象path・直接回帰・概算を具体化し、実行時にはsource／branch、test集合、資源・時間、停止／cleanupを決める。旧S1の予算・runnerを流用しない。未確定の工数を全packageの総額へ加算せず、I-01とB1-01〜03で直近分を見積もる。

失敗時は、失敗した問題の修正と原因確認に範囲を絞る。例外処理修正からFramework全体のSecurity改造、Cookie修正から認証基盤の作り直しへ自動で広げない。契約変更・migration／dependency・workflow等の必要性が判明した場合は、その具体差分と影響を示す。

### 7.3 次の具体作業

**次はI-01とB1-01を行い、既存コード／testのpath付き差分表と代表操作案を作る。** 続けてB1-02のroute／CSRF／error表と、R-00〜03の変更対象・最小回帰を具体化する。成果は一つのB1設計記録へまとめ、内容の重複する開始票を連続して増やさない。

**2026-10-20着手・具体化記録：** [B1設計記録](phase4-b1-session-spa-mvc-design-20261020.md)へ、I-01のsource・環境・検証入口、B1-01の差分表・代表操作、R-00〜03の対象・最小回帰をまとめた。継続してB1-02の標準spa()・Cookie属性・login／logout接続と失敗応答・10 DTO、B1-03の依存version・Reference限定build profile・生成／copy順・JAR検査と追加検証を具体化した。次はR-00のclean compile・直接回帰の実行準備。実行環境preflight・基盤是正・B1実装・動的検証は未実施。本節冒頭は着手時の順序として保持し、以後も同じ設計記録を更新する。

**同日のCloud点検結果：** [環境点検記録](../architecture/validation/phase4-r00-cloud-environment-preflight-20261020.md)で保存済み構成を有効化し、ソース無変更のoffline clean test-compile・規約負例5件／Reference Architecture2件PASSを確認した。fresh importの違反はtest込み／production限定とも0。前回の環境不足記述を訂正し、R-00の検証実行準備は成立。次はproduction検査対象の明示とfixtureの役割を結果から整理する。過去の15違反を現在の失敗と扱わず、是正実装・全test受入は未実施のまま維持する。

**着手時の範囲整理：** [B1設計記録§7.4](phase4-b1-session-spa-mvc-design-20261020.md#74-r-00の作業範囲と実行結果)で、R-00は既存`ReferenceArchitectureTest`1 fileの両importへproduction対象指定を追加する案に絞った。直接回帰は既存2件＋規約負例5件の7件。fixture移動・Rules緩和・production変更・root全件反復は含めない。この範囲整理の後、下記のOwner着手指示に基づく限定変更を実施した。

**R-00実施結果：** Ownerの着手指示後にtest1 fileだけを変更し、[限定結果](../architecture/validation/phase4-r00-production-import-results-20261020.md)のclean実行で13 projects SUCCESS・7件PASS。業務178／Identity境界218 classes、test由来は双方0を確認した。次はR-01の例外境界。R-01以降・B1実装・通常root全件・全体受入へ結果を拡張しない。

本書のWBS・順序・依存の詳細はこの計画案として提示する。Owner合意済みなのは§1の進め方と計画化の指示であり、各実装・実行・受入の完了を新たに記録したものではない。

## 付記 再開前の気づきメモ

2026-10-20の点検で挙げた参考メモ。**タスク、スケジュール、着手条件・完了条件への追加ではない。** 読者・AIエージェントはこの一覧を必須チェックリストや実装指示として扱わず、関連する設計に触れる際の参考とする。未実証の懸念を含み、作業順は§2・§7.3のままとする。

- **認証の実証範囲**：Profile SのガイドはOIDC login、現行Referenceはローカルform login。Session SPAの実証からSSO整備まで無意識に範囲を広げない。
- **数値表現**：既存APIの金額・versionはJavaのlong／Long。SPAのJavaScript Numberで扱う範囲と、大きな整数の丸めはDTO設計時の論点。
- **重複操作**：新規draft作成の二重クリック・応答不達後の再操作は、version競合とは別問題。送信中・結果不明の画面動作を考える際に意識する。
- **画面に残る情報**：Session失効後もSPAの取得済みデータや入力状態は別に残り得る。再login、別ユーザー、複数タブ、戻る操作での表示は検討の観点。
- **開発時と配置後の差**：frontend開発serverとpackage済みReferenceではorigin・proxy・Cookie・asset配信の条件が異なり得る。SPA URLの直接アクセスや再読み込みも画面内遷移と区別する。
- **機能の組合せ**：Batch、Virtual Threads、Observability、Container等の個別成立だけでは組合せの成立を保証しない。共有設定への影響を見る観点であり、全組合せ検証を求めるものではない。

引継ぎでは次の二点に留意する。

- **ReviewProbeは不具合再現の記録**：問題挙動を期待値にしているため修正後に失敗し得る。既存Surefire XML・compiled classに依存し、clean checkout単独で動く恒常的な回帰testではない。
- **現行入口と履歴の区別**：README等には過去のmain統合・tag手順や当初の順序も残る。再開入口は本書I-01／B1-01とし、古い「次の作業」をそのまま繰り返さない。
