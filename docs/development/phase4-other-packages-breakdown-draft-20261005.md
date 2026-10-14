# 作業7：S1以外のpackage分解・比較材料（2026-10-05）

**状態:** DRAFT / 全package分解の追加材料。具体的採用・先行対象・Gate改訂・実装開始・予算は未承認。
**Ownership:** Framework側の計画文書。Reference / Customer / Toolingを正式Frameworkへ自動昇格しない。
**確認baseline:** `docs/daily-development-workflow` / `eaf944d`、着手時作業treeはclean。S1文書4件とREADME・作業一覧のcommit反映を確認。今回、新しいMaven / Docker・browser・Batch検証、remote確認は行っていない。
**入力:** [入口・先行範囲案§2](phase4-entry-and-forward-scope-options-20261002.md#2-全packageの必要入力と開始境界)、[見直し草案§4.1](KOIKI-JavaWeb-FW_Phase4実施計画_見直し草案_v0.1.md#41-gate後の実行work-package候補)、[F-5全件台帳](phase4-pl2-f5-integration-and-gate-delta-draft.md)、[CP-F0材料](phase4-cpf0-and-s0-replan-options-20261002.md)、[S1実施可能性案](phase4-s1-execution-resources-and-sequence-draft-20261005.md)。

## 1. 対象と進め方

A1 / A2 / D1非同期はS1文書群へ委ね、B1 / C2 / C3 / C4 / D2 / D3 / D1同期、C0 / C1、B2、SAML / Edge、E1、optional項目を扱う。各packageの成果物・依存・review・正常 / 負例・環境・作業分解と待ち条件を揃える。

Framework側はOwner一人＋Codexの協働で、サブエージェントを使わずタスクを順次・同期的に進める。以下の責務は別人配置を前提にしない。Customerの業務判断・環境・受入をOwner / Codexが推測で代行しない。

区分Aは顧客入力なしで計画を進め得る対象、Bは顧客入力待ち、Cは採用trigger / Owner判断待ちを示す。どの区分も実装開始許可ではない。顧客非依存でも運用・platform・CI条件は残る。

## 2. 独立して具体化できるpackage

### 2.1 B1：Session SPA / MVC Reference（4-8・4-9）

- **対象・成果物:** 同一Referenceのexpense Use Caseを利用する最小Session SPAと既存MVCの併用。SPAのAPI route・認証・CSRF・エラー応答とbrowser操作Evidenceを作る。Referenceがfrontend・DTO・画面を所有し、FrameworkはSecurity契約、Toolingはbrowser harnessを所有する。
- **依存・review:** A1非依存。既存RESTをそのままSession経路と扱わず、Bearer / Session matcher、default deny、CSRF token取得・送信、Cookie・logout・権限、Controller / DTO分離をreview。React構成・依存・build / 配置・検証手段と当初DoD維持の判断は未確定。Customer BFFで代替しない。
- **正常実演:** Session loginからSPA参照・許可された更新、同じ業務状態をMVCで確認し、Business Audit・DBと突合。同じUse Caseの業務結果・409等をtransport別に照合。
- **負例・停止点:** 未認証・権限不足・CSRF欠落 / 不正・Session期限切れ・logout後・別user / scope外・競合。認可迂回、他userへのdata露出、MVC回帰はFAIL。route条件やbrowser環境未成立はBLOCKED。
- **環境・分解:** package済みReference＋隔離PostgreSQL＋実browser、frontend runtime / package managerはreviewで選ぶ。設計 → route / UI実装 → browser / API / DB / Audit実演 → 保証範囲・cleanup。token・Session・証拠を採取後に隔離資源を片付ける。

### 2.2 C2：外部API耐障害性（4-7）

- **対象・成果物:** Referenceの代表的な外部API操作を模擬providerで実演し、timeout・retry・同時実行制限と失敗応答を示す。ReferenceがUse Case・Port / Adapter・provider固有semanticsを所有し、Frameworkは共通境界review、Toolingはstub・負荷 / 障害制御を所有する。業務操作は候補として定義し、expenseへ未承認の外部I/Oを組み込まない。
- **依存・review:** A1に独立した模擬use caseを候補とする。read-onlyと副作用ありの操作を区別し、総待機時間、retryの層・上限、冪等key、同時実行制限、transaction / DB接続保持と失敗後の運用照合をreview。Resilience4j等のlibraryは採否・依存・配置を別判断する。
- **正常実演:** 成功応答、回復可能な失敗後の限定retry、制限内の同時実行を確認。provider受理・応答・DB / Audit・待機時間を照合する。
- **負例・停止点:** 無応答、5xx / 制限応答、受理後応答喪失、非冪等操作、連続失敗・飽和・取消し、retry多層化。副作用重複・無制限retry・Web / DB資源圧迫はFAIL。受理不明は人が確認して保留・調査し、通知S1の保証を転用しない。
- **環境・分解:** Reference＋HTTP stub＋DB / log / metric、遅延・応答喪失・並行要求のTooling。失敗契約設計 → Adapter / 制御実装 → 正常 / 負例と資源観測 → 運用条件・cleanup。

### 2.3 C3：non-Web Batch連携（4-10）

- **対象・成果物:** non-Web専用processへ既存`koiki-batch-fw`の処理を組み込むOwner想定を候補とし、Reference jobの単一実行・二重起動拒否・停止後再実行を実演する。A1非依存の月次集計再生成等を主実演候補とし、新しい会計確定・締め仕様を持ち込まない。正式job選定は未決定。
- **Ownership:** Referenceがjob・結果・業務migration、Toolingが外部起動・process停止とEvidence、Frameworkは起動・認可 / Audit・データ境界review。Batch Frameworkは別Repository所有。正式依存・互換性・schema統合は別判断。
- **依存・review:** Java / Boot / Batch version・artifact利用、永続JobRepository、metadata所有、non-Web明示、排他・crash recovery、job識別 / parameters、rerunとrestartの違い、再実行の副作用、非HTTP主体・Audit・DB接続保持をreview。2026-10-02のBatch点検結果は再検証していない。必要時に別RepositoryのAGENTS・HEADを再確認する。
- **正常実演:** 同じjobの単一実行と結果、失敗記録から人が原因・到達点を確認した再実行。A1パージへ利用する部分は別にS1の実行契約へ照合する。
- **負例・停止点:** 同じ条件の2 process起動、別条件の競合、実行中kill・結果確定直後kill、metadata DB障害、排他接続喪失、権限 / Audit失敗。二重副作用・metadataと結果の矛盾はFAIL。永続性や単一実行が確認できないならBLOCKED。照会guardやH2インメモリtestだけでPASSにしない。
- **環境・分解:** 実永続DB・別OS process・隔離job入出力、metadata / Business Audit / log突合。既存Batchの再点検・連携契約 → job / 起動構成 → process競合・停止 / 再実行 → 運用手順・cleanup。Batch全体の整備をS1開始の一括前提にしない。

### 2.4 C4：File / Object Storage代表実証（番号なし成果物）

- **対象・成果物:** Reference代表use caseのファイル受付・保存・結果確認・cleanupとPort / Adapter境界。外部ファイル→non-Web処理はC3と連携候補。local fileで検証した範囲とObject Storageで必要な差分を分ける。local fileだけで当初Object Storage成果物を充足したとしない。
- **依存・review:** A1非依存の代表操作を候補とする。転送完了・部分ファイル、識別 / 重複、path / 権限・サイズ / 内容、保存・公開・入力退避・DB記録の確定順、non-Web主体、保持・Auditをreview。SFTP / bucket / format / AWS Adapterは未選定。
- **正常実演:** 転送完了済みの受付から処理・結果公開・記録照合。C3連携時は別processへの受渡しと結果反映を独立検証する。
- **負例・停止点:** 転送中受付、重複投入、権限拒否、危険なpath、容量不足、公開前後停止、DB記録失敗、archive / error移動失敗。人がfile実体・DB・Audit・job metadataを照合し保留 / 再実行を判断。アプリ内Domain Eventの成功だけで受渡し保証としない。
- **環境・分解:** 隔離directory / filesystem、採用時は隔離Object Storage検証先・障害stub。代表操作 / 確定境界 → Adapter / 受付・公開 → 停止・重複・資源 / 接続保持 → 保証範囲・保持 / cleanup。C3との共有受付 / harnessを二重計上しない。

### 2.5 D2：Virtual Threads opt-in（4-11）

- **対象・成果物:** 既定無効を維持した有効化条件・ガイドとruntime互換、CI系統の候補。Frameworkは適用契約、Toolingは比較負荷・検証scriptを所有する。
- **依存・review:** A1非依存。Java 21 build・既存runtimeを維持し、Java 25 opt-in候補を比較。thread-local / Security context、driver・pool・pinning等の確認範囲、負荷条件、runner・時間・費用、workflow / required check変更をreview。
- **正常実演:** 有効 / 無効の同じ操作で業務・Security / Audit結果、thread / DB接続・応答を比較し、設定が意図したruntimeへ効いている証拠を採取する。
- **負例・停止点:** context漏えい、DB pool飽和、blocking処理、過負荷・timeout、非対応構成。特定負荷の速度だけで推奨しない。互換性違反はFAIL。実CI条件未確保ならローカル比較と4-11のCI判定を分離してBLOCKEDとして保持する。
- **環境・分解:** Java 21 / 25、同じReference artifact・DB・負荷harness。runtime / CI設計 → 比較構成 → 回帰 / 資源試験 → CI差分・有効化条件。CI変更は個別承認、実行回数 / 費用上限はOPEN。

### 2.6 D3：Container / ECS Reference（番号なし成果物）

- **対象・成果物:** package済みReferenceのcontainer build / run / health / diagnosis / 停止・再起動。ローカルcontainerと実ECSは別段階で、前者成功から後者をPASSにしない。Reference / Toolingがimage・実演を所有し、cloud固有AdapterをFrameworkへ入れない。
- **依存・review:** A1非依存の既存Referenceを候補とする。base image / digest、non-root等の実行条件、secret・network・resource・DB / migration・log・終了をreview。ECSにはaccount・権限・課金・deployment / cleanup責任が別途必要。
- **正常実演:** 固定JAR・imageで起動、認証付き代表操作・Audit、healthとlog、停止・再起動後のDB整合。実ECSはnetwork / secret / resource条件を付けて別実演する。
- **負例・停止点:** DB不達、migration失敗、secret欠落・露出、resource制限、kill、health異常。platformの証拠とアプリ状態を人が照合。実ECS入力未取得なら当該段階BLOCKED。
- **環境・分解:** ローカルDocker＋隔離DB → image / 起動契約 → 正常 / 異常 / 資源観測 → 手順・cleanup。ECSの方式 / 実演 / 費用は別見積で、image配布・remote操作も別承認。

### 2.7 D1同期・OpenTelemetry（番号なし成果物）

HTTP / 同期処理からlog・metric・実trace / exporter・sinkを照合する当初成果物として保持する。A1に依存しないが、採用範囲・非露出・cardinality・運用入力は残る。S1 W07と共通tracer / sink準備を共有する候補であり、独立した総額をそのまま加えない。

作業は既存Observability source・S1 W07の照合 → 同期境界・tracer適用 → 正常trace / 拒否 / context漏えい・sink障害 → 保証範囲で分解する。追加作業量は共通部分と残差を確定するまでOPEN。S1の非同期相関の成功だけで同期OpenTelemetry全体を充足したとしない。

## 3. 入力・triggerを待つpackage

| Package / Ownership | 分解と正常 / 負例の候補 | 待ち条件・開始前review / 環境・見積 |
|---|---|---|
| C0 / C1：MyBatis・accounting（4-6） / Architecture・Reference・Tooling | C0 trigger確認 → 分離モデル・規約 / 依存review → C1明示入力・永続化 → 楽観lock・共通error実演。正常保存、競合拒否、rollback、他module不正依存を検証。当初非同期精算連携はA1と別追跡 | MyBatis adoption未成立。Rule 8拒否を維持しSEPARATED・Rule 25〜27 / 30〜37・fixture / dependencyは追加しない。trigger・入力経路とA1依存分離の判断後にDB環境・作業量 / 上限を見積 |
| B2：Customer REST接続 / Customer、Frameworkはgap review | API・認証選択 → BFF責務 → 結合 → 非機密Evidence受入。通常操作・401 / 403 / 409・CSRF / 別user・logout等を選択profileで確認 | 案件API・IdP・frontend・実環境は未確定のまま保持。P4-AR6 / AR-D10の責任分担・受入条件を取得。Customer実装費をFramework残作業へ計上せず、gap review量はEvidence取得後に算定 |
| P4-04：SAML / Customer入力・Framework採否 | 認証終端 / KOIKIへ届くcredential確認 → OIDC / broker / 直接SAML比較 → 必要拡張review → 成功 / 誤issuer・audience等の方式別負例 | PL1-Q6入力待ち。SAMLを採用済みとも不要とも扱わない。test IdP・依存・Security / Identity境界・運用責任を選定後に見積 |
| P4-EDGE：ALB等 / Customer・platform、Framework境界 | 採否確認 → network・署名claim / IdP JWT・信頼境界 → Adapter要否 → 実環境結合、偽造header・直接到達・期限 / keyの負例 | ALB採用だけでAdapter必須としない。実claim・network・cloud権限 / 費用待ち。非採用でも当初項目の後続割当はOwner判断。実ALB実演はlocal stubから代替PASSにしない |
| E1：正式受渡し・release / Framework・Customer受入 | 対象inventory → Maven座標・checksum / repository・version / support → 独立Consumer受取・診断 → finding / 正式受渡し判断。誤version・欠落artifact・更新 / support不整合を確認 | P4-AR6実チーム・AR-D10・release入力待ち。R2 stage・内部snapshotを正式releaseにしない。配布 / publish / remoteは別Gate。受取環境と対象が揃ってから作業量・費用 / 上限を算定 |
| P4-OPT：Authorization Server / Architecture | 需要・Ownership確認 → P4-AS0 → Security設計 → grant / token・誤credential等の採用方式別実演 | 明示trigger・個別Gateまで開始しない。必須DoD外。方式・環境・support未取得のため数値見積はOPEN |
| P4-OPT：Oracle / Architecture・採用対象Owner | 需要確認 → P4-ORACLE → DB方言 / driver / migration・transaction / lock契約 → 実Oracleで正常 / 失敗DDL・競合等 | 明示trigger・ライセンス / 実DB・driver採否待ち。PostgreSQL PASSを転用しない。必須DoD外、数値見積はOPEN |

待ち項目をOPENとすることは0工数・採用不要の判断ではない。条件が揃った対象から設計・環境・実演・費用を更新する。

## 4. 独立候補の残作業概算と上限案

単位は標準作業時間。Agentによる低確度の比較案であり、実測・AI経過時間・予算承認ではない。各列は追加の詳細設計 / review材料、実装 / 局所回帰、正式候補の統合実演 / 証拠採取、最終文書 / cleanup整理を区別する。Ownerのreview参加・人の実演参加、外部待ち、CI / cloud等の金銭費用は別枠OPEN。

| Package | 詳細設計 | 実装・局所回帰 | 統合実演・証拠 | 最終文書 | 合計案 | 上限・範囲の注意 |
|---|---:|---:|---:|---:|---:|---|
| B1 | 16〜24 | 40〜72 | 24〜40 | 8〜16 | **88〜152** | 最小Session SPA / MVC。画面増加・frontend方式変更は再見積 |
| C2 | 8〜16 | 16〜32 | 16〜24 | 8〜12 | **48〜84** | 代表模擬API操作。汎用Starter・実provider結合は別範囲 |
| C3 | 16〜24 | 24〜48 | 24〜40 | 8〜16 | **72〜128** | Reference job 1種類とnon-Web連携。Batch Framework全体の改修費は含めず、必要改修判明時に別判断 |
| C4 | 8〜16 | 16〜32 | 16〜32 | 8〜16 | **48〜96** | 代表file操作とlocal範囲。Object Storage実接続の追加額・採否はOPEN |
| D2 | 8〜16 | 8〜16 | 16〜32 | 8〜12 | **40〜76** | local opt-in比較とCI提案。実CI構築 / 実行費はOPEN、4-11達成までの総額ではない |
| D3 local | 8〜16 | 8〜16 | 16〜24 | 8〜12 | **40〜68** | ローカルcontainer限定。ECSの構築 / 実演 / 費用はOPEN |

上側を超過する見込みがあれば実測・残作業・追加範囲を示してreviewへ戻る閾値候補とする。上限採用は未承認。安全・DoDを削って数値へ合わせない。これらは必要な全作業の予算ではなく、限定候補の比較である。

根拠は§2の代表成果物を設計・実装・統合検証・文書へ分解した作業規模と未知要素であり、既存Phase 0人日を換算していない。個別harness・frontend構成・Batch連携差分が未確定のため精度は低い。選定対象の詳細設計で再見積する。

### 4.1 共通部分の重複と未算定範囲

- C3 / C4のファイル受付、起動・結果照合、故障制御は共有候補。同じ作業を両方へ全額計上せず、選定後のWBSで担当packageへ一度だけ割り当てる。
- S1パージ / C3は単一実行と基盤、S1 W07 / D1同期はtracer・sinkを共有し得る。共通化の要否と追加差分をreviewし、S1＋本表の単純合計をPhase 4総額としない。
- B1 / D2 / D3等が同じ固定Reference artifact・環境を使う場合、setup・cleanup・回帰の共通費を抽出する。
- C0 / C1、B2、SAML / Edge、E1、optional、Object Storage実接続、D1同期残差、実CI、ECS費は未算定。作業7全体の工数確定には不足が残る。

## 5. 先行対象を比較する観点と順序案

S1中心の計画方向を維持し、独立packageを一括開始しない。Owner＋Codexは順次作業し、以下の「独立」は依存上の性質を示す。

| 対象群 | 比較・具体化の候補順 | 作業4bへ渡す判断材料 |
|---|---|---|
| S1 | A1 / A2 / D1の安全・担当 / 環境・上限を先行入力として保持 | 認可・Audit / 復旧時間・停止確認・key・環境・上限の残判断。Batch全体を中心目的に置き換えない |
| C2 | 模擬API契約と局所的な失敗実演を先に詳細化する候補 | A1非依存性と対象の狭さ。実provider不要だがlibrary / transaction / retryのreviewは必要 |
| C3 / C4 | non-Web / Batchとfile確定境界を一緒に設計し、実装・実演単位は分ける候補 | 既存Batch再点検と正式依存、job / metadata・永続性・単一実行・file受渡しのgap、共有作業の上限 |
| B1 | 最小frontendとSession / MVC経路の具体化を別単位で進める候補 | 当初4-8 / 4-9の独立実証。Securityとbrowser / frontend準備の負担、Customer BFFとは別責務 |
| D2 / D3 / D1同期 | runtime・CI / platform・観測入力が揃うものから個別比較 | localで進め得る範囲と実CI / ECS / 実traceの未取得を分離。local成功で当初全成果物をPASSにしない |
| B / Cの待ち項目 | 入力受付・trigger判断・差分整理を継続 | Customer / release / optional入力の未取得を独立計画全体の停止条件にしない |

この順序は検討の提案で、先行package選定・予算・開始承認ではない。案1では現行Gate経路を維持し、案2では選定対象をP4-Fへ追加する改訂・承認が必要。現在のP4-F提案のA1 / A2 / D1限定境界を、本書だけで拡張しない。

## 6. OR統合へ渡すものと不足

今回、S1以外の全件について対象・分解・検証出口・待ち条件を追加整理した。OR提出完了や作業7全体完了とは扱わない。

次は作業4b・5へ、S1と独立候補の対象、DoD・Ownership、依存・未取得、概算 / 上限・共有費、先行順序・Gate差分を比較表として統合する。その際、S1候補化の承認、case了解、Owner＋Codexの陣容は確定入力として保持する。具体的採用・実装・環境・上限は別判断である。

選定対象の詳細設計・実環境・数値閾値、未算定部分、共通費配賦、CP-F0残項目・Gate正本改訂は不足として明記する。既存R1〜R7、F-5、AGENTS / Skill、POM・production code・migration・Rules・CI・Batch Repository・remoteは変更していない。
