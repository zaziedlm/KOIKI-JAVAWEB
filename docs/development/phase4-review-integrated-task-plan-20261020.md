# Phase4 初期標準確立・複数案件採用に向けた統合作業計画

作成日：2026-10-20  
Ownership：Framework側の計画文書。実装の所有者は各タスクに記載する。  
最終見直し：2026-10-23

状態：**§9の優先順・補足方針はOWNER APPROVED（2026-10-23）**。既存の成果・限定受入を保持し、初期標準の確立を軸に推進する。各タスクの具体差分・検証条件・実施結果は段階ごとに更新する。

**現行の推進計画・再開入口：§9。成果・残課題の棚卸し：§8。** 初期標準一覧・配布方針の整理を開始し、D2→D1同期→S1／B1／C2の保証範囲・DoD判定→E1→D3→要件具体化に応じた残タスクの順に進める。C2 Aの受入記録は`05911cc`へcommit済み。§2・§7.3のB1着手順と§8.3のC3先行案は履歴とし、繰り返さない。

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
| I-01／B1着手時の確認済み・再開時に差分確認 | 共通／Tooling | 作業branch・source、未commit文書、利用するartifactとtest入口を確認。通常／専用asyncの区別、変更対象と直接回帰を一覧化する | 作業source、対象path、実行予定集合、資源・時間・cleanup条件が追える。過去PASSを現sourceの結果にしない。次packageでは必要な差分だけ確認する | 本計画、現行sourceは§8.1 |
| R-00／限定変更・検証完了 | F5／Reference test | `ReferenceArchitectureTest`1 fileの両importへ`DO_NOT_INCLUDE_TESTS`を追加。fixture配置・Rulesは維持 | clean compile＋既存2件／規約負例5件PASS。両importともtest由来0。[限定結果](../architecture/validation/phase4-r00-production-import-results-20261020.md)にsource／XMLを対応付け | I-01成立。過去の15違反を現在の失敗とせず、検査責務を明示 |
| R-01／限定変更・検証完了 | F1／Framework API Starter | 汎用AdviceとSecurity・HTTP例外の責任を整理し、標準処理へ伝播する修正と合成testを作る。Reference局所再throwの必要性も照合 | 認証済み拒否403、未認証の経路別応答、独自404、予期せぬ500、MVC／HTMX／APIの応答が意図どおり。回避策除去は検証成立時だけ | I-01、比較するarchitecture検証はR-00後 |
| R-02／限定変更・検証完了 | F2／Reference identity構成 | 独自CookieSerializerをBoot管理へ戻すか設定反映するかを選び、logout customizerとの生成順も確認・修正 | 明示Secure／SameSiteがSet-Cookieへ反映。HTTPS判定の有無とlogout削除Cookieのname／pathを確認。property値の確認だけで閉じない | I-01、B1-02のCookie／logout要求と照合 |
| R-03／限定変更・検証完了 | F3／Reference expense | owner／approver scopeの確認をversion照合より前にする。対象外の情報を例外差から漏らさず、既存競合処理を維持 | scope外の一致／不一致／負versionと不存在が秘匿契約どおり。scope内競合は409相当。拒否時の更新・Audit・eventなしを確認 | I-01、B1-01の対象操作と照合 |

修正ごとの証拠は既存 `docs/architecture/validation/` の記録方式を使う。内部修正と公開property／API変更を混ぜず、変更が後者へ広がる場合に、その差分の契約reviewを行う。単に同じStarterだからという理由でF1とF4の採否を一括化しない。

### 3.2 P4-B1の調査から受入まで

| ID／状態 | 主担当Ownership | 作業と対象 | 成果・完了条件 | 依存 |
| --- | --- | --- | --- | --- |
| B1-01／設計反映済み | Reference | DoD 4-8／4-9に既存MVC、Bearer REST、Session、CSRF、expense Use Case・DTO・testを対応付ける。参照＋更新の代表操作を選ぶ | B1設計記録の差分表・選定操作をB1-05〜07へ反映済み。Customer BFFとは別対象 | I-01。R-00〜03の完了不要 |
| B1-02／設計反映済み | Reference、Framework Security契約review | Session SPA APIとMVCと既存Bearerのmatcher・order・認証応答・CSRF取得／送信／更新・logout・Cookie・権限／scopeを設計。F1〜F4を対応付ける | route別のSecurity／CSRF／error表をB1-05〜07へ反映。F4はReferenceの経路設定で扱い、Framework既定変更を完了条件にしない | B1-01。R-01〜03の設計へ入力 |
| B1-03／設計反映済み | Reference frontend、Tooling検証 | 最小Reactの画面、Session APIとの接続、同一originでのbuild／配置、依存・lockfile・package方法、実browserとDB／Audit照合を具体化 | 選定UI・固定依存／lock・同梱profile・検証構成をB1-06〜07へ反映済み | B1-01〜02。基盤修正の完了不要 |
| B1-04／合流点検・実装計画整理済み | Framework計画・Owner判断 | B1設計記録§9にR-00〜03との整合、Reference／Tooling対象、局所／合流検証、概算・実行条件を整理 | 次はB1-05の開始判断。test method数・通常root全体資源・frontend取得条件は実装／実行前に具体化。機能作成・実行・受入は今回未実施 | B1-01〜03、R-00〜03。F6／F7／H1／S1完遂は前提外 |
| B1-05／実装・局所検証完了 | Reference backend | Session API、Security／CSRF・login／両logout、専用10 DTO／mapper／業務Adviceを実装。既存Use Case・Framework契約を再利用 | 新規72＋既存77＝149件PASS。次はB1-06。実browser・DoD・Owner受入は未実施 | B1-04とOwnerの未commit保持での実装開始指示。B1設計記録§10 |
| B1-06／実装・局所検証完了 | Reference frontend | 選定した最小SPAを実装し、login後の参照・更新、失効／拒否／競合の表示をつなぐ | package済みReference上で代表操作が実行でき、MVCで同じ業務結果を確認できる | B1-05。画面構成の設計はB1-03で先行 |
| B1-07／Cloud検証完了 | Tooling、Reference受入 | 実browser、API、DB、Business Audit、logで統合実演。通常構成と既存経路を保護し、資源をcleanup | 下記DoD表のEvidence、直接回帰、選定した通常検証、cleanupがそろう。未達は結果として明示 | B1-05〜06。実行集合・上限はB1-04で確定 |
| B1-08／選定ブラウザ結果Owner受入済み・残判断あり | Framework計画・Owner受入 | DoD 4-8／4-9の成立範囲、残課題、利用方法を記録し、後続packageを選ぶ | [2026-10-22承認§5](../architecture/validation/phase4-b1-08-local-acceptance-input-20261022.md#5-選定ブラウザ検証結果のowner受入2026-10-22)：自宅PCブラウザ結果はCOMPLETE / OWNER ACCEPTED、cleanup済み。DoD全体受入・B1-08全体完了・次package選定は未判断。Phase4全体完了とは分ける | B1-07 |

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
| P4-D1 同期処理の観測 | §5.2、D1S-01〜03 | 現状整理→consumer側依存・設定の限定接続案→実装・有限実証の判断。今回は計画具体化まで |
| P4-D2 Virtual Threads | §5.2、D2-01〜03 | 比較条件→検証→CI・ガイド |
| P4-D3 Container・ECS | §5.2、D3-01〜03 | 起動契約→local検証→実ECS |
| P4-04／P4-EDGE SAML・認証境界 | §5.2、EDGE-01〜03 | 認証境界確認→採否→必要Adapter・検証 |
| P4-E1 正式受渡し・release | §5.2、E1-01〜03 | 対象確定→配布・診断手順→受入 |
| P4-OPT 条件付き拡張 | §5.2、OPT-01〜02 | 明示要求→採否・対象判断。採用後の実装分解は未実施 |

全テーマについて、作業段階・Ownership・必要入力・出口・関連レビューを整理している。一方、全テーマをそのまま実行できる詳細計画がそろった状態ではない。B1とC2 Aは個別設計・実装・選定検証まで進んだが、他の後続packageは対象ファイル、具体仕様、検証集合・command、工数・実施時期が未確定。現在の状態と次候補は§8を参照する。

現在の計画は、**全テーマを収録した作業台帳と、B1・レビュー是正・C2 Aの個別成果への導線**である。Phase4全体を同じ粒度で実行計画化し終えた状態ではなく、次に選ぶテーマから詳細化する。上表は§3・§5.2の索引であり、別タスクとして二重計上しない。

### 5.2 各テーマの作業単位と着手条件

以下は棚卸し§4／§9の全テーマを具体作業へ接続したもの。各行の01→02→03は同一テーマ内の順序であり、表順はテーマ間の実施順ではない。**現在の優先順と各行の着手時期は、後続のOwner承認を反映した§9に従う。**

| 元package／状態 | 作業単位 | 主担当Ownership・必要入力 | 完了条件／レビューとの接続 |
| --- | --- | --- | --- |
| P4-B1／選定構成の技術成立・DoD最終受入候補 | B1-01〜08 | 選定ブラウザ結果はOwner受入済み。[判定初版](phase4-s1-b1-c2-guarantee-and-dod-review-20261030.md#41-b1の受入候補を成立させる構成)でDoD4-8／4-9の受入範囲を提示 | DoD最終受入・B1-08全体はOwner判断待ち。会社PC等の未確認を原DoDへ新規必須条件として追加せず、案件適用範囲として残す |
| P4-C2／A限定結果COMPLETE / OWNER ACCEPTED（2026-10-23） | C2-01 A照会契約と開始承認→02 Port／Adapter・限定retry・timeout・枠2→03 同一JARの有効／無効実演・限定結果受入まで実施。Bは運用機能確定・バリエーションの必要性発生時に別判断 | Reference／Tooling。[設計・結果・受入§9〜11](phase4-c2-01-operation-candidate-selection-20261022.md#11-aの限定検証結果のowner受入2026-10-23)、[受入承認正本§7](../architecture/validation/phase4-c2-a-implementation-results-20261022.md#7-aの限定検証結果に対するowner受入承認2026-10-23)。新規29 PASS／接続timeout1 SKIP、直接回帰89＋Framework6 PASS。成果commitは87728b7 | DoD4-7全体・C2全体は未認定。通常root全test449 PASS／100 SKIP、最後の集約clean失敗と集約だけの再開SUCCESSを区別。設定override等の検証限界を保持。ローカルPC手動確認は受入の追加必須条件にしない。BのUNKNOWN・Auditは未実装／未実証で後続へ残し、A受入・次package移行の条件にしない。追加追試・S1再開・commit／remoteの開始承認を兼ねない |
| P4-C3／要件具体化待ち（§9.6） | C3-01 別Batch RepositoryのHEAD・互換性とjob選定→02 non-Web起動・永続metadata・単一実行→03 二重起動・停止後restart／rerun・Audit実演 | Reference／Tooling、Batchは別所有。job・運用主体・DB入力 | DoD 4-10。R-06はDB構成が該当する場合に接続。S1パージ完了と切り離す |
| P4-C4／要件具体化待ち（§9.6） | C4-01 代表操作・保存／DB確定境界→02 Port／Adapterと受付／公開→03 重複・path・権限・容量・途中停止・cleanup | Reference／Tooling。storage／format入力。C3連携は必要時だけ | local fileとObject Storageの実証範囲を区別。共有harnessは一度だけ計上 |
| P4-D1同期／限定接続・動的検証の技術成立、結果Owner確認待ち（§9） | D1S-01計画→02独立consumer接続→03対照・有効・VT smokeの全10項目PASS | Framework変更なし、独立Tooling11 file。[結果§7](../architecture/validation/phase4-d1-sync-observability-results-20261030.md#7-o02期待値訂正後の同範囲一回2026-10-30) | 正式Collector・backend・本番運用、非同期DoD 4-12は未実証。次は結果・Git差分と成果固定対象確認 |
| P4-D2／main実CI成功・DoD 4-11技術PASS、限定範囲Owner受入済み（2026-10-30） | D2-01比較条件→02通常／観測6構成・43項目→03専用CI・利用ガイドをmain統合済み | Framework／Tooling。[D2-03受入§7](phase4-d2-03-ci-and-adoption-guide-20261026.md#7-限定結果dod-4-11のowner受入記録2026-10-30)。受入記録はPR #58でmain統合済み | 既定無効、早期拒否の相関・逼迫時403等の制限を維持。D2全体・正式提供へ拡張しない |
| P4-D3／優先第5段階・実ECS入力待ち（§9） | D3-01 固定JAR／image・起動契約→02 local containerのhealth／診断／停止再起動→03 実ECS差分と実演 | Reference／Tooling＋platform。secret・network・費用・権限 | local／ECS別Evidence。F2の実Cookie条件とR-06のmigration構成を該当時に照合 |
| P4-C0／C1／保留維持 | C0-01 adoption trigger確認→02 分離モデル・規約／依存review→C1-01 accounting実装→02 競合・rollback・共通error実演 | Architecture／Framework規約、Reference。MyBatis採用入力 | DoD 4-6。C0前はRule8拒否維持。元計画のA1連携依存と独立検証可能部分を開始時に区別し、S1全体を自動再開しない |
| P4-B2／入力待ち | B2-01 Customer API／BFF／認証選択取得→02 gap審査→03 Customer結合Evidenceの受入 | Customer主導、Framework契約review。案件／IdP／環境入力 | B1の代替DoDにしない。F1／F4〜F7のうち採用構成に関係する項目を照合 |
| P4-04／P4-EDGE／入力待ち | EDGE-01 認証終端と到達credential確認→02 SAML／broker／OIDC等の採否→03 必要Adapterと実環境検証 | Customer／platform＋Architecture | SAMLの当初成果物としての採否を明記。ALB採用だけでAdapter追加を決めない |
| P4-E1／方式整理先行・優先第4段階（§9） | E1-01 対象artifact／version／checksum／support→02 配布・受取・診断手順→03 独立Consumerと実チーム受入 | Framework release＋platform＋Customer | 内部snapshot／checkpointと正式受渡しを分離。R-05／06を採用構成に照合。Phase5全体へ広げない |
| P4-OPT／保留維持 | OPT-01 Authorization Server／Oracleの明示要求→02 採否・対象・実環境・費用の個別判断 | Architecture＋Customer／platform | 必須DoD外。未採用を完了へ置き換えない |

### 5.3 非同期系の残課題

現行の評価時期は§9の第3段階に従い、追加機能を抑制して保証範囲・残アクションを整理する。以下の再開条件は追加実装・動的検証の扱いとして維持する。

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
| AD-03／E1先行整理へ接続・方式判断待ち | 01 開発専用Maven stage方式判断→02 IDE／CLI切替・更新・診断Tooling→03 試用 | 既存比較案を入力に恒常方式を選択 | I-01で必要な局所artifact準備と恒常方式を分ける |
| AD-04／保留維持 | 01 Customer POM検討時期と共有結果確認→02 再開判断成立時だけ誤選択検証・例外運用を具体化 | Owner了解済み検証保留の解除判断 | F6／F7があることだけで再開しない |
| AD-05／E1先行整理へ接続・恒常方式判断待ち | 01 用途別artifact一覧・不足診断→02 採用範囲の準備／offline確認→03 更新・復旧手順 | 選択Toolingの限定LOCAL PASSを入力に必要分を判断 | I-01の不足が生じた対象だけ対応。全Tooling準備をB1前提にしない |

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

**着手当時の順序（履歴）：** I-01とB1-01で既存コード／testのpath付き差分表と代表操作案を作り、B1-02のroute／CSRF／error表とR-00〜03の変更対象・最小回帰を具体化した。成果はB1設計記録へ集約済み。現在の次候補・残判断は§8を参照し、この初期調査を再度開始しない。

**2026-10-20着手・具体化記録：** [B1設計記録](phase4-b1-session-spa-mvc-design-20261020.md)へ、I-01のsource・環境・検証入口、B1-01の差分表・代表操作、R-00〜03の対象・最小回帰をまとめた。継続してB1-02の標準spa()・Cookie属性・login／logout接続と失敗応答・10 DTO、B1-03の依存version・Reference限定build profile・生成／copy順・JAR検査と追加検証を具体化した。次はR-00のclean compile・直接回帰の実行準備。実行環境preflight・基盤是正・B1実装・動的検証は未実施。本節冒頭は着手時の順序として保持し、以後も同じ設計記録を更新する。

**同日のCloud点検結果：** [環境点検記録](../architecture/validation/phase4-r00-cloud-environment-preflight-20261020.md)で保存済み構成を有効化し、ソース無変更のoffline clean test-compile・規約負例5件／Reference Architecture2件PASSを確認した。fresh importの違反はtest込み／production限定とも0。前回の環境不足記述を訂正し、R-00の検証実行準備は成立。次はproduction検査対象の明示とfixtureの役割を結果から整理する。過去の15違反を現在の失敗と扱わず、是正実装・全test受入は未実施のまま維持する。

**着手時の範囲整理：** [B1設計記録§7.4](phase4-b1-session-spa-mvc-design-20261020.md#74-r-00の作業範囲と実行結果)で、R-00は既存`ReferenceArchitectureTest`1 fileの両importへproduction対象指定を追加する案に絞った。直接回帰は既存2件＋規約負例5件の7件。fixture移動・Rules緩和・production変更・root全件反復は含めない。この範囲整理の後、下記のOwner着手指示に基づく限定変更を実施した。

**R-00実施結果：** Ownerの着手指示後にtest1 fileだけを変更し、[限定結果](../architecture/validation/phase4-r00-production-import-results-20261020.md)のclean実行で13 projects SUCCESS・7件PASS。業務178／Identity境界218 classes、test由来は双方0を確認した。次はR-01の例外境界。R-01以降・B1実装・通常root全件・全体受入へ結果を拡張しない。

**R-01実施結果：** Ownerの着手指示に基づきAPI Starterの汎用Adviceを修正し、Security／HTTP例外の標準処理と安全な500を両立した。[限定結果](../architecture/validation/phase4-r01-exception-boundary-results-20261020.md)に合成testと直接回帰を記録。Security依存・POMを追加せず、Reference局所再throwは維持。次はR-02のCookie／logout構成。作業branchは`docs/phase4-review-integrated-plan-20261020`を継続し、PR作成・pushは行わない。commitも未実施。B1実装・全体受入は未実施。

**2026-10-21 R-02実施結果：** Referenceの独自CookieSerializerへBootのCookie設定を反映し、既存Beanを維持したままlogout構成の成立を確認。Frameworkの生成順・POMは無変更。合成test、既存直接回帰、HTTP設定を訂正した実JAR Critical JourneyでCookie発行・logout削除・旧Cookie拒否を確認した。[限定結果](../architecture/validation/phase4-r02-cookie-logout-results-20261021.md)へ記録。次はR-03の認可順序。今回commit・push・PR作成は行っていない。

**2026-10-21 R-03実施結果：** 共有mutationで対象認可をversion照合より先に移し、scope外と不存在の秘匿を是正。認可済みの競合・HTTP負versionの400・既存settle権限を維持した。Application・実DB・HTTPの拒否と副作用、既存直接回帰を[限定結果](../architecture/validation/phase4-r03-authorization-order-results-20261021.md)へ記録。次はB1-04で設計とR-00〜03結果を照合。今回commit・push・PR作成、通常root全件・B1機能受入は未実施。

**2026-10-21 B1-06結果：** Owner指示でB1-05 Cloud結果文書のみを`00ce98a`へcommit後、frontend・固定lock・Reference限定build／配信入口を実装。[限定結果](../architecture/validation/phase4-b1-06-frontend-build-results-20261021.md)は65件PASS、通常／専用JARのclean packageと内容・hashを照合。B1-06のcommit・push・PRは未実施。次はB1-07の資源・集合具体化と通常／同梱構成・実JAR browser統合検証。

本書のWBS・順序・依存の詳細はこの計画案として提示する。Owner合意済みなのは§1の進め方と計画化の指示であり、各実装・実行・受入の完了を新たに記録したものではない。

**2026-10-21 B1-04合流点検：** Ownerの指示に基づき、[B1設計記録§9](phase4-b1-session-spa-mvc-design-20261020.md#9-b1-04設計と基盤是正結果の合流点検2026-10-21)へ実装対象・Use Case再利用・検証割当て・概算・実行条件を整理した。R-02既存logoutと新規SPA接続、Cloud／Windows・source／artifactの証拠を区別する。次はB1-05の開始判断であり、今回の文書更新で機能作成・検証実行・受入は行っていない。実装／実行前に具体化するOPENは同§9.4へ集約する。

**2026-10-21 B1-05実施結果：** Ownerの開始指示に基づきB1-04文書差分を保持してReference backendを実装し、[局所149件の結果](../architecture/validation/phase4-b1-05-session-api-backend-results-20261021.md)を記録した。Framework／業務層／依存・migrationは無変更、commit・remoteなし。次はB1-06のUI・同梱build・配信入口。通常root・同梱JAR・browser・DoD受入は後段へ残す。

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
- **現行入口と履歴の区別**：README等には過去のmain統合・tag手順や当初の順序も残る。再開入口は本書§9、棚卸しは§8とし、古い「次の作業」をそのまま繰り返さない。


**2026-10-21 B1-07結果：** B1-06を`2b0ed5b`へ固定後、同branchで合流検証を実施。[最終結果](../architecture/validation/phase4-b1-07-integrated-browser-results-20261021.md)は通常root／同梱構成各422実行PASS＋99 skip、同じ最終同梱JARの新旧browser計2件PASS。実演で判明したMVC日付初期値と同一明細IDの保存を最小訂正し、locale2件・実DB1件を追加。途中停止／訂正前artifactを保持し、資源・cleanupを確認した。ローカル手動確認は[計画](phase4-b1-07-local-browser-check-plan-20261021.md)へ分離し未実施。次は手動確認の実施判断とB1-08のDoD受入整理。今回commit・push・PR作成は行っていない。

## 8. C2 A受入後の棚卸しと次候補（2026-10-23）

Ownerの「次タスク、さらにそれ以降のタスクの棚卸し」「ズレをなくす」との指示に基づく更新。既存IDを維持し、新しい機能・全体Gateを増設しない。本節の成果・不足入力を保持し、§8.3の当初候補順は後続の§9の承認済み順序へ差し替える。

### 8.1 sourceと今回訂正したずれ

- 作業branch：`docs/phase4-review-integrated-plan-20261020`、HEAD `05911cc`。今回の棚卸し開始時clean、ローカルの追跡refより1 commit先行。remoteの最新状態・CI・PRは今回確認していない。
- 実装成果`87728b7`、結果確認・Owner受入記録`05911cc`。C2 Aは[結果文書§7](../architecture/validation/phase4-c2-a-implementation-results-20261022.md#7-aの限定検証結果に対するowner受入承認2026-10-23)の限定範囲でCOMPLETE / OWNER ACCEPTED。
- B1-01〜03を「調査・設計対象」のままにしていた表示と、§7.3・引継ぎの古い再開入口を訂正した。B1-04〜07等の各時点の「次は」「未実施」は当時の記録として読む。今回、新たなOwner受入や動的検証を行ったものではない。
- B1-08の「後続package未選定」は[2026-10-22のブラウザ結果受入時点](../architecture/validation/phase4-b1-08-local-acceptance-input-20261022.md#5-選定ブラウザ検証結果のowner受入2026-10-22)の履歴。C2はその後に個別開始・結果受入済み。一方、DoD4-8／4-9全体受入・B1-08全体完了の明示記録は依然未確認であり、C2着手をその承認の代用にしない。
- 2026-10-05の他package分解の概算・順序は比較履歴。実施済みB1／C2 Aの残工数として再計上せず、今後の総額・予算承認にも流用しない。

### 8.2 固定済み成果と残る判断

| 既存ID | 確認できる現在地 | 残る行為・扱い |
| --- | --- | --- |
| R-00〜03／F5・F1〜3 | 限定修正・検証は実施済み。§3と各結果記録を参照 | 同じ修正を再開しない。新たな差分に直接影響がある時だけ必要回帰を選ぶ |
| B1-01〜07・R-04／F4 | 設計・実装・選定自動検証、Session／MVC経路設定まで実施済み | 2026-10-21のbrowser成功、22日のCloud再検証とWindows実測は別source／artifact／環境として保持 |
| B1-08 | 自宅PCの選定ブラウザ結果はOwner受入済み | 既存§4.1のEvidence表を入力にDoD4-8／4-9全体の受入範囲・B1-08全体状態を判断する。会社PC・戻る／進む単独確認・新Cloud browser未実行等を明示し、全件再検証を自動追加しない。他package調査を止める新しい前提にはしない |
| C2-01〜03のA | 新規29 PASS／1 SKIP、選定回帰・通常構成保護・cleanupの限定結果をOwner受入済み | H07接続timeout、connect／retry override等の検証限界、root分割結果を保持。必要性が具体化した時に対象を限定して追試を判断。C2／DoD4-7全体は未認定 |
| C2のB | 運用機能確定／バリエーション必要性発生時の条件付き後続 | UNKNOWN・副作用あり操作・Intent／結果Audit等は未実装／未実証。A受入や次package移行の前提に戻さない |

### 8.3 棚卸し時の候補順（比較履歴・§9へ差替え済み）

**以下は複数案件採用の優先方針が示される前の比較案で、現行の着手順ではない。** C3先行案を比較履歴として保持する。各packageの作業分解・不足入力は再利用するが、C3は§9の要件具体化待ちへ移した。B1-08の残判断とR-09の導線整理も§9へ接続する。

| 候補順／既存ID | 次に行う具体作業 | 出口・後続の実装／検証 | 現在の不足・切替条件 |
| --- | --- | --- | --- |
| 1：C3-01 non-Web Batch | `koiki-batch-fw`の現在のbranch／HEAD／AGENTS、Java／Boot／Batch・座標／配布状態、永続JobRepository・排他／再実行・Auditの既存保証を確認。代表jobを1つ選び、metadataと業務結果の所有・DB構成を定める | C3-02の最小接続・non-Web起動案と、C3-03の二重起動／停止後restart・rerun／Audit検証を、差分・ケース・資源／cleanupまで具体化。必要ならR-06を接続 | 連携先は[既存決定記録§6](phase4-s1-completion-direction-decision-20261002.md#6-batch-framework点検材料と採用判断の境界)の[zaziedlm/koiki-batch-fw](https://github.com/zaziedlm/koiki-batch-fw)。このworkspaceにcloneはなく、現在の接続可否・branch／HEADは未確認。旧`c999298`を現状としない。基盤側改修が必要なら別所有の作業として切り出す |
| 2：C4-01 File／Object Storage | 代表file操作・format／storage範囲、受取完了・重複・保存／公開／DB確定境界を選定。C3との共有が必要か判断 | C4-02 Port／Adapterと受付・公開→C4-03 path／権限／容量／途中停止／cleanup。共有harnessは一度だけ計上 | C3全体完了は必須依存ではない。local fileと実Object Storageを区別し、bucket・権限等の不足は該当段階の入力待ちにする |
| 3：D1S-01 同期観測 | 同期HTTPのtrace／log／metric、tracer／exporter／sink、非露出・保持条件を選ぶ | D1S-02接続→03相関・context非漏えい・sink障害の実演。S1と共有できる準備だけ一度計上 | sink・運用入力待ちの部分を分離。S1非同期のDoD4-12をこれで閉じない |
| 4：D2-01 Virtual Threads | Java21 build／25 opt-in、同じ操作・artifactの比較条件、context／pool／負荷、CI候補を具体化 | D2-02有効／無効比較→03 CI系統・利用条件。実CI未実施をlocal結果からPASSにしない | runner・実行枠・workflow判断が不足。既定無効を維持 |
| 5：D3-01 Container | 固定JAR／base image・起動／health／secret／migration／終了の契約を選ぶ | D3-02 local起動・診断・停止再起動→03実ECSの差分・実演 | localとECSは別段階。ECSはaccount／権限／network／費用／cleanup入力待ち。image配布・deployを自動開始しない |

当時の比較案は技術上の直列依存を示すものではなかった。現在は§9の順序を採用し、C3のsource取得を直近の前提にしない。各packageの正確な残工数・実施日は未確定で、具体差分と検証集合から見積もる。

### 8.4 入力待ち・条件付き後続と横断課題

| 既存ID | 維持する状態・再開条件 | どこへ接続するか |
| --- | --- | --- |
| C0／C1：MyBatis・accounting | adoption trigger待ち。Rule8拒否・DEFERREDを維持 | 採用時に分離モデル・規約／依存review→実装・競合／rollback。S1を自動再開しない |
| B2：Customer REST／BFF | 案件API・IdP・認証profile・frontend・実環境Evidence待ち | Customer主導でgap審査と結合Evidence。B1の代替達成にしない |
| P4-04／EDGE | 認証終端・credential・SAML／broker／OIDC／ALB等の採否入力待ち | 必要なAdapterだけ個別設計。SAMLの当初成果物としての扱いを最終判定に残す |
| E1：正式受渡し | §9により配布方針・初期標準の整理を先行し、第4段階で初期提供版を固定。実チーム受入等の不足入力を管理 | R-05／06・AD-01を採用構成へ接続。内部snapshot・checkpoint、初期提供版、Phase5の正式supportを区別 |
| OPT：Authorization Server／Oracle | 明示要求・採用判断待ち。必須DoD外 | 採用時に対象・実環境・費用を具体化 |
| S1-T1〜3：非同期残課題 | 限定受入・人手HOLD・運用OPENを維持。§9の第3段階で既存成果の保証範囲・DoDを評価。追加機能・動的再実行は不足と必要性に応じて対象を限定 | DoD4-1〜4-5／4-12の成立・未達を対応付け、アクションを後続化。他packageの都度再開しない |
| R-05／F6：Entity scan | 新規Consumer／E1等で構成を選ぶ時に最小Consumer実証と導入契約判断 | AD-01／E1に一度だけ計上 |
| R-06／F7：Flyway接続・schema | C3／D3／Customerで採用DB構成が該当する時に検証 | 同一DataSource／schema・migration専用接続・strategy置換を区別。全構成対応を先行要求しない |
| R-07／H1：master並行更新 | 対象業務操作の変更・保証要求が具体化した時 | 必要な有限再現と保証範囲。未実証懸念を確定不具合として閉じない |
| R-08：非同期回帰／CI | expense／event／notificationへの直接影響、またはCI運用要求がある時 | 必要回帰のみ選定。恒常CI追加は別判断 |
| R-09：開発者向け導線 | R-00〜03・B1・C2 Aで確定した利用条件／制約のガイド反映状況を点検する文書作業が残る | 既存Journey／guideから正本へリンクし、同じ説明を新文書へ複製しない。R-05／06の未確定契約を先に正式化しない |
| R-10：変更種別の整理 | 各タスクの内部修正・公開契約・実環境／remoteの区別を継続 | 独立した機能実装・包括Gateとして二重計上しない |
| AD-01／02 | 実チームの受入Evidence、Linux／WSL試行結果待ち | B2／E1／R-05〜06。CloudでのLinux実行成功を実チーム受入の代用にしない |
| AD-03／05 | Maven stage恒常方式、用途別artifact準備・更新／復旧方式の判断待ち | 選定タスクで実際に不足する準備だけ対応。全Tooling整備を先行条件にしない |
| AD-04 | Customer依存宣言の検証保留を維持 | Customer POM検討・再開判断の時だけ具体化 |

### 8.5 以後の台帳更新と完了判定

日々の確認では本節のsource・状態・新しい差分だけを更新し、初期設計や全件調査を毎回やり直さない。タスクごとに「選定／具体化→実装→検証→受入→成果固定」のどこまで成立したかと、次の一手・不足入力を残す。限定受入、機能package全体、DoD全体、remote統合の状態は混同しない。

現行の次の一手・優先順・Owner判断は§9を更新する。本節は成果・不足入力の台帳として維持する。

Phase4全体の最終判定には、DoD4-1〜4-12と番号なし成果物（File／Object Storage、同期OpenTelemetry、Container／ECS、SAML等）の成立・未達・採否／延期判断、実チーム受入とrelease判断を対応付ける。本棚卸しで保留項目を不要・完了に変更していない。今回の変更は計画文書のみで、追加test・Batch取得・production変更・commit／pushは行っていない。

## 9. 初期標準確立に向けた見直し推進計画（OWNER APPROVED／2026-10-23）

### 9.1 背景・承認と目的

Ownerから、1件の実案件への適用が計画・進行中で、他案件にも採用見込みがあり、案件ごとのFramework要求差を踏まえて初期標準機能群を早期に明示・確立する必要があるとの状況共有を受けた。これを優先順の見直し根拠とする。未取得の案件要件や実環境の成立は推定しない。

Ownerは提示順の点検後、「提示案を維持し補う形の案を採用承認いたします。これを、見直したタスク推進計画文書にまとめていってください」と明示した。承認対象は本節の優先順と補足方針。§8.3のC3先行案に代えて採用し、既存のpackage／WBS IDへ接続する。

目的は、案件横断で参照できる提供物・適用条件・保証範囲・更新方法を定め、各案件が同一のFramework基準から必要機能を選べる状態にすること。追加の案件要望は、既存機能の利用、案件所有の拡張、共通契約の不足、Framework不具合候補に分けて判断する。一案件の要求をそのまま標準へ取り込まず、[既存の提供機能ガイド§5](framework-capabilities-and-selection-guide.md#5-フィードバックと機能拡充の判断原則)と上位設計のFramework昇格条件を用いる。

### 9.2 承認済みの順序と成果の出口

```text
初期標準一覧・配布方針の整理を開始（E1-01／R-09／AD-03・05へ接続）
  → 1. D2 Virtual Threads：実行基盤・Java対応・有効化条件
  → 2. D1同期観測：運用診断につながるtrace／log／metric
  → 3. S1残課題＋B1・C2：保証範囲・DoD判定と後続アクション
  → 4. E1：初期提供版・参照取り込み／更新方法の固定
  → 5. D3：frontend配置を含むcontainer／ECSの段階実証
             → 結果・修正をE1の提供版／適用条件へ反映
  → 6. 要件具体化・必要性に応じた残タスク
```

| 段階／既存ID | 優先理由・実施内容 | 出口・次へ渡すもの |
| --- | --- | --- |
| 先行整理：E1-01／R-09／AD-03・05 | 初期標準機能の一覧と配布方針をD2着手時から整理。既存成果・条件付き機能・Reference・対象外を区別し、案件が採用断面を識別できるようにする | §9.3の一覧初版、提供物・版識別・取得方法の論点。ここで全項目を最終確定させる新しいGateは設けず、D2以降の判断を同じ一覧へ反映 |
| 1：D2-01〜03 | Java実行基盤として優先判断。Java target／build JDK／runtimeとVT opt-inを区別し、既定無効を維持。比較条件→有効／無効の同じ操作・artifact比較→実CIと利用条件を具体化 | 対応構成・有効化条件・制限・検証根拠。高速化や全案件での有効化を完了条件にしない。D2内で必要な応答時間／thread／pool／context観測を用意し、D1全体完了は待たない |
| 2：D1S-01〜03 | consumer側の標準依存・設定による限定接続を実装し、同期HTTP・log相関・要求分離・sink障害／回復・VT smokeの全10項目が技術成立 | Owner結果確認と成果固定対象確認。Framework変更0、正式Collector・backend・非同期の成立には拡張しない |
| 3：S1-T1〜3／B1-08／C2-03 | 非同期を主軸に既存成果と未達を照合。B1・C2も含め、既存Evidenceで保証範囲・DoDの成立／未達・適用条件を評価。追加機能を抑制 | 「保証できること／できないこと／利用条件／残アクション」を記録。必要な追試・限定訂正、利用制限、後続化を区別。限定結果に理由を付けるだけで未達をPASSにせず、§9.5へ残す |
| 4：E1-01〜03＋R-05／06・R-09・AD | Framework JAR群・Parent／BOMの固定と管理、案件の参照取り込み・更新を確立。採用構成の最小外部Consumer、取得・build・起動・診断と受領側Evidenceを照合 | artifact／version／checksum／source対応、配布先、導入・更新・既知制約の案内を備えた初期提供版。Phase5のv1.0／support条件・全体完了とは分ける。D3後の変更を受ける版管理を用意 |
| 5：D3-01〜03 | Framework外で所有するfrontend moduleも配置境界に含め、Java backend containerの構成・build／run／health／診断／停止を検証。localと実ECSを段階分離 | frontend／backendの取得・build・配置と認証境界、固定JAR／image、起動・障害・cleanup Evidence。実ECSの不足入力を明示し、E1の版・対応表・導入手順へ結果を反映 |
| 6：C3／C4・B2・C0／C1・P4-04／EDGE | §9.6の需要・要件具体化に応じて着手 | 初期標準へ無条件に追加せず、要件・所有・差分・検証・保守範囲を具体化したものから選定 |

この順序はOwner＋Codexで順次推進する優先順であり、技術的に全前段を完了しなければ後段の文書整理もできないという意味ではない。E1の方式整理は先行し、D2・D1・保証範囲判断を受けて第4段階で提供版を固定する。入力待ち等で順序の見直しが必要なら、その理由・影響・代替作業を本節へ記録する。

### 9.3 初期標準機能群の明示方法

新しい機能一覧を別文書へ複製せず、[提供機能・適用条件・選択ガイド](framework-capabilities-and-selection-guide.md)と[引継ぎガイド](application-team-handoff-guide.md)を既存の提供入口として更新する。初期標準の一覧は[ガイド§0〜1の整理初版](framework-capabilities-and-selection-guide.md#0-初期標準一覧の読み方と確定までの判断)へ具体化した。D2以降の判断で更新し、現時点ですべての機能を提供可と認定したものではない。

| 区分 | 一覧へ記載すること |
| --- | --- |
| 初期標準として提供 | 対象artifact・公開API／property・対応構成・検証根拠・利用方法 |
| 条件付きで提供 | opt-in条件・既定値・制約・案件側責任・適用できる構成 |
| Referenceで限定実証済み | 実証操作・source／artifact・環境と限界。Framework JARの汎用保証とは区別 |
| 今回の標準対象外 | 対象外／保留理由、必要入力、再開条件。未提供を不要・完了へ変えない |

各項目に現時点の提供判断状態と根拠を付ける。B1のSPAやC2の模擬外部照会などのReference-owned codeを、Framework JARへ自動昇格させない。採用案件ごとの要望には、利用基準版・期待する契約・差分・必要時期を対応付け、共通基準と案件固有拡張の境界を保つ。

### 9.4 個別の依存・不整合とE1へ集約する横断課題

| 対象 | 対応方針・判断する時期 |
| --- | --- |
| D2のJava／VT条件 | D2-01〜03で正本§21.5／§23.3をJava21 build・同一成果物のJava25 runtime／VT opt-inへ整合済み。既定無効・将来版個別確認を維持。実CI成功・限定受入は[D2-03§5〜7](phase4-d2-03-ci-and-adoption-guide-20261026.md#7-限定結果dod-4-11のowner受入記録2026-10-30)を参照 |
| D1とD2／S1の観測共有 | D2の結果と既存Starterを入力に、D1は独立consumerの同期trace接続・log相関・有限障害実演が技術成立。正式Collector・backend等は別。S1の保証範囲判定は既存Evidenceで進め、非同期採用案件の具体化時に設計・運用を深掘りする。同期成功を非同期配送・復旧・相関の成功へ転用しない |
| R-05／F6：Customer Entity scan | E1の採用構成確認へ優先的に接続。Frameworkと別packageの最小Customer Entity／Repositoryが共存することを実証し、修正または明示登録の導入契約を定める。Referenceの成功だけで導入保証としない |
| R-06／F7：Flyway | E1で初期標準が対応するDataSource／schema／migration接続を絞って明示・検証。全Flyway構成への対応を先行要求しない。D3等が別構成を採用するなら差分として確認 |
| R-09・AD-01〜05 | 現在の提供機能・導入手順・既知制約の導線を更新。Maven stage等の日常開発方式と提供版の取得方式を整理し、独立Consumerおよび受領側の取得／build／起動／診断を照合する。AD-04の検証保留は維持し、恒常方式や新しい機械検査を黙って追加しない |
| 配布物・版管理 | JARだけでなくParent／BOM／Starter／Libraryの提供単位・組合せ、固定version、repository、checksumとsource、更新・互換性・migration・既知制約の通知を定める。同じ可変SNAPSHOT座標だけで複数案件の採用断面を識別しない。版番号・配布方式は今回未確定 |
| E1とPhase5 | [Baseline対応表](../architecture/KOIKI-JavaWeb-FW_Baseline_Compatibility_v0.1.md)は正式release version／support条件をPhase5で確定する記載を含む。E1で初期提供版とPhase5 Production Baseline／v1.0の対象・約束を区別し、必要な正本整合を行う。初期提供版の固定をPhase4全体・Phase5完了や本番適用承認へ読み替えない |
| frontendとD3／B2 | D3-01までに、静的assetのJava JAR同梱か別container／BFFか、origin・認証終端・Cookie／CSRF・proxy経路の最小条件を選ぶ。B2全体を先行完了させる必要はないが、未選定の配置を検証済みにしない。frontendの実装・業務責務は案件側に保つ |
| E1へのD3結果反映 | D3の構成・修正をE1の対応表・導入手順・版管理へ戻す。公開契約・JARが変わるなら必要な互換検証と新しい版の判断を行い、既に固定したartifactを同じ版で上書きしない |

### 9.5 限定結果・DoD判定と後続アクション

**第3段階の判定初版（2026-10-30）：** [S1／B1／C2保証範囲・DoD整理](phase4-s1-b1-c2-guarantee-and-dod-review-20261030.md)へ9項目の要求・Evidence・保証範囲・未達・利用条件・既存IDの残アクション／責任ロール案・再開条件を具体化した。B1の選定Reference構成は技術成立・最終受入候補、C2 Aは限定受入維持／H07等未実証、S1未達は維持。[文書§7](phase4-s1-b1-c2-guarantee-and-dod-review-20261030.md#7-現時点のエビデンスとしてのowner採用判断2026-10-30)に現時点のエビデンスとしてのOwner採用判断を記録。個別DoDの新規最終受入・原文改訂・動的実行はない。

第3段階ではS1の既存DoD対応・運用OPENと、B1-08／C2の既存Evidenceを入力に判定表を作る。各項目には、要求／保証できること／保証できないこと／適用条件／Evidence／判断状態／残アクションと担当／再開条件を記録する。既存IDを使用し、同じ課題を複数packageへ二重計上しない。

追加機能は抑制する。未達への対応は、(a)既存証拠で成立範囲を確定、(b)必要性が明確な有限追試・最小訂正、(c)利用制限または標準対象外、(d)後続課題化、から選ぶ。未達を理由の記述だけで達成扱いせず、DoD変更・採否・延期は明示したOwner判断として残す。C2 Bは必要性発生時の条件付き後続を維持する。

S1の人手照合・担当付きHOLDは、担当・照合資料・停止／再開判断を説明できる適用条件として扱う。運用入力がない部分を運用成立と記録せず、無条件に本番利用できる保証へ広げない。今回の優先順承認は既存成果の評価を進める根拠であり、過去の実行予算・runnerを再利用するものではない。

### 9.6 残タスクの条件とPhase4全体の扱い

| 残タスク | Ownerの優先判断・現在の扱い | 着手時に具体化するもの |
| --- | --- | --- |
| C3 non-Web Batch／C4 File・Object Storage | Webサイクル外の処理として必要可能性は高いが、要件具体化時に着手 | job／file操作・運用主体・永続metadata・単一実行・保存／DB確定境界。旧C3先行案やS1の都合で自動開始しない |
| B2 Customer REST／BFF | 必要可能性は高く、案件要件がより具体化した時期に着手 | API・認証方式・BFF責務・実IdP／環境・結合Evidence。D3の最小配置判断とは範囲を分ける |
| C0／C1 MyBatis・accounting／P4-04 SAML・EDGE | Ownerの現時点評価では必要可能性は低い。必要時に着手できる判断材料を維持 | 既存拡張点・不足入力・採否／再開条件の整理まで。将来用code・dependency・schemaは先行追加しない。MyBatisのadoption trigger・Rule8拒否等の既存条件を維持 |
| OPT Authorization Server／Oracle | 必須DoD外、明示要求と採用判断待ち | 既存の対象・環境・費用条件を維持 |
| その他R-07／08等 | §8.4の直接影響・利用要求・CI運用要求が具体化した時に必要範囲を判断 | 全体棚卸しのたびに未実証懸念・非同期回帰全件を再開しない |

これらは引き続きPhase4台帳で追跡する。初期標準の提供開始とPhase4全体完了は別の判定であり、当初DoD4-1〜4-12と番号なし成果物の未達・採否／延期を消さない。必要可能性が低いことを、SAML等の当初成果物の廃止承認と解釈しない。

### 9.7 直近の着手点・更新方法

**S1／B1／C2保証範囲・DoD整理（2026-10-30、現行）：** main `fdfcfa59504011e7ed9eb0d142ec67eaa98feabf`（PR #58・マージ後CI成功確認済み）から`docs/phase4-s1-b1-c2-guarantee-review-20261030`を作成。Ownerの指示に基づき、[判定初版](phase4-s1-b1-c2-guarantee-and-dod-review-20261030.md)を作成した。B1 DoD4-8／4-9は既存証拠により選定構成の技術成立、最終受入／B1-08全体完了は未判断。C2 DoD4-7はH07等を残して全体PASS未認定、S1 DoD4-1〜5／4-12の未達・限定成立を維持。追加実装・動的再実行なし。OwnerはS1の既知未達と運用／追加開発リスクを認識し、[文書§7](phase4-s1-b1-c2-guarantee-and-dod-review-20261030.md#7-現時点のエビデンスとしてのowner採用判断2026-10-30)をエビデンスとして採用する判断を示した。次は成果固定対象の確認と、E1への提供範囲・制限の引継ぎ・方式整理。個別DoDの最終受入と未達は別に追跡する。D1の限定結果受入は未記録のまま別管理し、本書で補わない。以下は各時点の履歴。

**D1限定範囲の技術成立（2026-10-30、履歴）：** O02期待値訂正後、Owner承認の同範囲一回で全3 child・10 checkpointがPASS。Java21無効対照／有効、同じJava21生成JARのJava25 VT smokeが成立し、正常・500・log相関・要求分離・sink503／遅延／回復を確認。JUnit1 PASS、cleanup・source／JAR照合成功、Maven21.999秒。[結果§7](../architecture/validation/phase4-d1-sync-observability-results-20261030.md#7-o02期待値訂正後の同範囲一回2026-10-30)へEvidenceと限界を記録。Framework／Reference・CI変更なし。今回限定範囲は技術成立、Owner結果受入は確認待ち。さらに実行を増やさず、結果・Git差分と成果固定対象を確認する。正式Collector・backend・本番運用や非同期DoD 4-12へ一般化しない。結果確認後はS1／B1／C2保証範囲・DoD整理→E1→D3。以下は各時点の履歴。

**D1追加一回の結果（2026-10-30、履歴）：** Owner承認の追加実行で、同じJAR・依存・設定のO04・O01が成功し、正常同期HTTPの実OTLP受信・decode・構造化log相関が成立。O02はHTTP500のspan statusがUNSETで期待ERRORと異なりFAIL、残7項目未到達。採用Micrometerの例外なし500ではstatus500／outcome SERVER_ERRORで識別することを静的照合し、harness期待値を訂正・compile済み。さらに動的再実行なし。両child／receiver cleanup・identity照合成功。全10項目・D1受入は未成立で、[結果§6](../architecture/validation/phase4-d1-sync-observability-results-20261030.md#6-訂正後の同範囲追加一回2026-10-30)を次の判断入口とする。Framework／Reference・CI変更なし。以下は各時点の履歴。

**D1限定実装・初回結果（2026-10-30、履歴）：** Ownerの§8確認・進行指示を受け独立Tooling11 fileを作成。consumer側の標準依存・設定のみでJava21 package／test-compile・source／JAR固定を確認。初回一回はexport無効対照O04がPASS後、子process終了時のlog readerで`Stream closed`をERROR判定し停止した。JUnit1 invocation／1 error、残9 checkpoint未到達。所有child・receiver cleanupとidentity照合は成立。helper訂正・compile済み、動的再実行は未実施。[初回結果・再開案](../architecture/validation/phase4-d1-sync-observability-results-20261030.md)を次の判断入口とし、初回source／artifact／失敗を保持する。Framework／Reference・CI変更なし。export有効・VT smoke・同期OpenTelemetryの接続成立は未確認。以下は各時点の履歴。

**D1限定接続案の具体化（2026-10-30、実行前の履歴）：** Ownerは「consumer側依存・設定で接続可能かを先に調べ、Framework変更が必要なら理由・最小差分を提示する」進め方を了承した。[D1計画§8〜13](phase4-d1-sync-observability-plan-20261030.md#8-限定接続案への見直し判断2026-10-30)にBoot4.1.1配布POM／sourceの静的照合、独立consumer・OTLP受信fixture、主8＋VT smoke2の10 checkpoint、40分上限案・cleanup・変更分岐を記載。Framework変更必須の根拠は現時点でなく、実接続成立は未確認。今回の実施は資料取得と文書訂正まで。D1S-02／03実装・動的検証は次の判断対象とし、非同期設計・運用の深掘りは採用案件の具体化時という方針を維持する。以下の「今回モジュール改変なし／計画確認後に第3段階」等は見直し前の履歴。

**限定接続案具体化前の入口（2026-10-30、履歴）：** main `b2ca253`から`docs/d1-sync-observability-plan-20261030`を作成。Ownerの受入記録・表示訂正・D1計画整理の進行指示を受け、[D2-03§7](phase4-d2-03-ci-and-adoption-guide-20261026.md#7-限定結果dod-4-11のowner受入記録2026-10-30)に説明済み限定範囲の受入を記録した。[D1計画初版](phase4-d1-sync-observability-plan-20261030.md)は現状・適用条件・不足・後続判断を整理し、Owner確認対象。今回はモジュール改変・新規動的検証なし。同期の基本整理を案件フィードバック待ちにせず、非同期連携の設計・運用の深掘りは採用案件が具体化した時に行う。計画確認後はS1／B1／C2の保証範囲・DoD整理→E1→D3の順を維持する。接続／実演・同期OpenTelemetry・非同期DoD 4-12は未成立のまま残す。この追記はローカル文書差分で、commit／remoteは未実施。以下の受入待ち・実CI待ち等は各時点の履歴。

**D2-03の現在位置（2026-10-27）：** VT成果は`0af5d77`へcommitされ、PR #55でmain統合済み。後続CI調整PR #56のmerge commit `f3c79d8376d93d9f977ff2263c768b5ce32fc3ed`に対する[VT実CI run 40354524562](https://github.com/zaziedlm/KOIKI-JAVAWEB/actions/runs/40354524562)は成功。Temurin21.0.12+1 build・21／25.0.4+1 runtimeの通常／観測6 invocationで6 PASS／0 FAIL／0 ERROR／0 SKIP・43項目、source／JAR照合・cleanupが成立した。同mainの通常CI・Java Runtime Compatibilityも成功。[D2-03記録§5〜6](phase4-d2-03-ci-and-adoption-guide-20261026.md#5-main統合と実ciの確認記録2026-10-27)へcommit・run／job URL・artifact ID／digest・実時間・判定根拠を反映した。**DoD 4-11の技術条件はPASS、D2-03結果／DoDのOwner受入は未記録で判断待ち**。今回の文書追随指示やPR mergeを明示受入として扱わない。次は実CI結果・[適用ガイド§3.1](virtual-threads-adoption-guide.md#31-現在の検証限界)の制限を入力とするOwner受入判断。既定無効を維持し、D2全体完了・正式提供・Phase4全体の認定へ拡張しない。以下の未commit・実CI待ち等は各時点の履歴であり、現行状態は本段落に従う。

**D2-03ローカル確認時点の履歴（2026-10-26）：** OwnerはD2-02結果を確認し「D2-03へ進めましょう」と指示した。専用workflow・適用ガイド・グランドデザイン整合・source／artifact照合・集約・cleanupを実装し、未commit差分でのCI専用経路は6 PASS／0 SKIP・43論理checkpoint、cleanup、9 guard test・workflow lint成功。この時点では実CI／DoD判定は未成立で、次は成果固定・PRによる実CIだった。現在は上記main実CI結果へ進んでいる。

**D2-02限定検証の現在位置（2026-10-26）：** Ownerの§14に沿う見直し・推進指示を受け、通常Referenceと実Observability追加の観測構成を分離し、Toolingの相関識別・Hikari入力・pool失敗の経路別分類を訂正した。Java21無効／Java25無効／有効の通常・観測計6構成、43論理checkpointが成立。最終続行5 PASSと同条件のA-native既存成功1を対応付け、過去の失敗・診断は保持。通常root449 PASS／100 SKIPは再実行していない。[最終結果§9〜11](../architecture/validation/phase4-d2-02-java25-virtual-threads-results.md#9-構成訂正tooling診断の経緯)を限定結果のOwner確認入口とする。次はD2-03のCI・有効化ガイド・正本整合。既定無効、早期拒否の相関欠落・逼迫時403等の制限を残し、実CI／DoD4-11・D2全体・正式提供を完了にしない。commit／pushは未実施。以下は各時点の履歴。

**D2-02追加一回後の現在位置（2026-10-26）：** Owner承認を受けた専用検証の追加一回は、A-nativeがlogin／create／read／submit204まで進み、Toolingの相関ヘッダー必須取得でERROR。後続5件はSKIP、43 checkpointは未成立、cleanup済み。通常ReferenceにはObservability Starterがないことを確認し、通常起動helperを訂正・compileのみ実施した。次は[D2計画§14](phase4-d2-01-java25-virtual-threads-plan-20261026.md#14-追加一回の結果を受けた構成訂正案2026-10-26)の、通常構成と実Observability追加の観測構成を分ける訂正案の具体化。runtime classpath追加・更なる実行は未実施。以下は各時点の履歴。

**D2-02の現在位置（2026-10-26）：** Ownerの§8〜12確認・開始指示を受け、非配布Tooling 11 fileを作成。通常rootは449 PASS／100 SKIP・失敗／エラー0。初回専用検証はA-nativeのHMAC fixture設定不備で起動FAILし、後続5 invocationをSKIP、43 checkpointは未成立。所有資源cleanup済み。標準Base64へのTooling訂正・compileと同範囲の追加一回の再開案を[結果記録§5](../architecture/validation/phase4-d2-02-java25-virtual-threads-results.md#5-訂正差分と再開案)へまとめた。自動再実行・commit／pushは未実施。以下のREVIEW READY等は開始時の履歴。

**D2-01開始（2026-10-26）：** Owner指示により、`main` / `1284ba08a9e0cb44c67487dc02148e378699ceb4`から`docs/d2-01-java25-vt-verification-plan-20261026`を作成した。目的はJava 25以降で案件がVTを本格利用する際の対応条件を確かめること。全案件への採用推奨や既定有効化を目的にしない。[D2-01対応方針・比較検証計画](phase4-d2-01-java25-virtual-threads-plan-20261026.md)へ静的調査・比較案・残判断を記録し、一次情報と既存harness・実行thread観測方法の照合から進める。Java 25より後の版は個別確認とし、D2の動的検証・CI成功は未成立。

**D2-01検証範囲の具体化（2026-10-26）：** Ownerの継続指示に基づき、同計画§8〜12へ採用版Boot 4.1.1の一次情報・保存JAR構成、通常／外付け観測の起動方法、Java21無効／Java25無効／有効の3構成、43論理checkpoint／6 invocation、Tooling 11 file、直接回帰73件を含む通常root一回、資源・55分の実行枠とcleanupを具体化した。現状はREVIEW READY。通常起動と観測起動の証拠を分け、本番流量制御・全executor・将来Java版へ一般化しない。次はこの具体案を入力とするD2-02の作成・実行範囲判断。JDK25準備・Docker preflight・観測接続成立と実結果は未確認、正本／production／POM／CI変更・動的検証は未実施。

**初期標準一覧の整理初版は作成済み（2026-10-23）。** 提供機能ガイド§0〜1へ基本候補・条件付き候補・Reference限定成果・対象外と根拠／残判断を整理。Root／各module POMの静的照合で、Root ReactorとReferenceを除く提供候補14座標＝12 JAR＋2 POMを確認した。B1／C2の進捗・Observabilityの現機能を更新し、引継ぎガイドの同じ古い案内も訂正した。artifact生成・取得・checksum照合・配布や動的検証を行った結果ではない。

E1の配布論点は同ガイド§0.2〜0.3に接続済みで、固定version・repository・更新方式・受領側Evidence等は未確定。次は一覧初版を踏まえ、**D2-01のJava／VT対応方針・比較対象・最小検証案を具体化する**。現時点でD2の実装・性能比較・CI実行結果はない。詳細設計で対象path・公開契約差分・検証集合／資源・時間・停止／cleanupと見積を定め、各段階の必要な判断と実績を記録する。

**一覧初版の方向性承認（2026-10-23）：** Ownerは提供機能ガイドの初期標準一覧を確認し「方向性を承認します」と明示した。整理方向性の承認として記録し、E1の最終提供判断とは区別する。同時に提示されたbranch文書共有とmainへのPR統合の選択は比較・相談中であり、今回の記録からcommit／push／PR／mergeを実行済み・承認済みと扱わない。

**後続判断：main統合を優先（2026-10-23）。** Ownerは「main向けPRを作成し、マージに進むのであれば、これを優先作業するので、main マージ済み時点で、文書共有で良いと思います」と指示した。上記の比較・相談を区切り、現在の成果をcommit／pushし、main向けPRの差分レビュー・必要CI確認を経てmergeへ進む。文書の共有入口はmain統合後に提示する。PRは文書だけでなくR-00〜03・B1・C2 Aの実装／test／結果を含むことを明記し、未達を全体達成へ読み替えない。正式release・artifact publish・tag作成・案件チームへの送信は含めない。実際のPR・merge・CI結果は実施後に識別可能な形で残す。

本承認は計画の採用・文書化に対するもの。今回の反映ではproduction／POM／CI／version／配布先を変更せず、追加検証・publish／deploy・commit／push／PRを行っていない。以後は本節を現行入口として状態・判断・次の一手を差分更新し、履歴の「次の作業」を繰り返さない。
