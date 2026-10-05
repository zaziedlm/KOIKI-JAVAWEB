# S1実装前review票・環境preflight案・再見積差分（2026-10-05）

> **現在の参照先（2026-10-05）:** RV票・PF案は正式統合の残論点として保持する。現在のST-B初回作成・実行は別の方式票B6で承認済み。本書の環境・上限未承認表示は初回局所契約の承認を取り消すものではない。 [最新引継ぎ](phase4-s1-local-verification-start-handoff-20261005.md)／[承認済み方式票](phase4-s1-method-ballot-and-local-verification-contract-20261005.md)。

> **最新方針（2026-10-05）:** [S1最小構成と将来の運用管理機能の分離](phase4-s1-minimum-demonstration-scope-review-20261005.md)をOwnerが了解。要求管理一式を初回必須とせず、RV-2 / RV-4と認可引渡しを再reviewする。Tier・保存・見積は最小構成を確認してから判断する。本票の判定は未実施。

**状態:** DRAFT / review入力。各票は未判定。方式・権限code・schema / dependency・環境実行・数値上限・Gate・実装開始は未承認。
**Ownership:** Framework側の設計資料。Referenceの要求・認可・復旧構成と、Toolingの検証支援を既存Framework Public契約へ接続する案。
**確認baseline:** `docs/daily-development-workflow` / `e75a70a`。W01 / W02・運用・nonHTTP資料等は未commit差分。今回Public source・承認契約のread-only照合のみ。環境preflight・Maven / Docker・実演は未実施。
**入力:** [nonHTTP review案](phase4-s1-nonhttp-process-observation-review-draft-20261005.md)、[方式 / DoD / 運用条件](phase4-s1-method-dod-operations-observation-draft-20261005.md)、[W01 / W02](phase4-s1-w01-w02-source-design-review-draft-20261005.md)、[初回判断](phase4-initial-scope-gate-and-open-decisions-draft-20261005.md)、[残作業W01〜10](phase4-s1-execution-resources-and-sequence-draft-20261005.md)。

## 1. Public契約への接続確認

| 目的 | 現行Public契約・source確認 | 接続案と限界 |
|---|---|---|
| 要求の認証済み依頼者 | [FrameworkPrincipal](../../koiki-starters/koiki-starter-identity/src/main/java/org/koikifw/identity/FrameworkPrincipal.java)に不変user ID・認証source・permissionsがある | 認証済みReference受付がuser IDを要求へ記録。入力されたuser IDやprincipal型の自己作成だけで本人認証としない |
| 実行直前のuser / 権限 | [IdentityQuery](../../koiki-starters/koiki-starter-identity/src/main/java/org/koikifw/identity/IdentityQuery.java)の`findById(FrameworkUserId)`は[IdentityUser](../../koiki-starters/koiki-starter-identity/src/main/java/org/koikifw/identity/IdentityUser.java)を返す。status、permissionCodes、versionを持つ | 存在・ACTIVE・必要permissionをApplication側で照合する候補。Framework内部Repositoryへ依存せずに接続可能。照会は認証ではなく、その時点のreadである |
| 現在状態の読み取り | [DefaultIdentityQuery](../../koiki-starters/koiki-starter-identity/src/main/java/org/koikifw/identity/internal/DefaultIdentityQuery.java)はreadOnly transactionでuser・Role / Permissionを読む | 実効Bean / transaction・読取鮮度を実演する。外側transactionやpersistence contextの影響、照会後の権限変更の窓をreview。versionを読むだけで変更をlockしていない |
| 権限の意味 | [Phase 2 Identity契約](../architecture/validation/phase2-p2-b2-contract-review.md)はbackendのPermission code判定を採用 | 復旧用permission・環境 / 対象scope・失効条件はReference / Applicationで定義するreview対象。codeを今回固定しない |
| Audit | Business / Security recorder、AuditEvent・USER actorを既存[Audit契約](../architecture/validation/phase2-p2-b1-contract-review.md)で利用 | operationをsafe resourceで関連付ける。分類・transaction・送信前後failure semanticsをreview。SYSTEM・新enum・任意Mapの先行追加は不要 |
| 相関・観測 | 既存[Observability](../../koiki-starters/koiki-starter-observability/README.md)はstructured log・requestId / TaskDecorator・healthを提供 | Application / Toolingが復旧operation相関と実tracer / exporter / sinkを構成する候補。現行Starterに汎用観測APIを追加する判断ではない |

**接続の結論:** 現在のuser有効性・permission再確認に必要なread契約は存在する。要求の真正性・消費・停止証拠・副作用の整合はApplication側の責務であり、IdentityQueryを呼ぶだけでは完成しない。上記はsource接続の確認で、non-Web実動PASSではない。

## 2. 認証済み要求から実行へ接続する最小設計案

1. 認証済みReference経路が対象1件の復旧権限・scope・CSRF等を確認し、不変user IDとoperation / 対象publication・listener・snapshot、理由・有効性をApplication-owned要求へ保存する。これが未実装の新規対象であることを明記する。
2. Ownerがprocess終了世代 / drain、provider受理、対象snapshotを総合確認する。Codexは採取・照合を支援する。要求作成時の確認と実行直前確認を分ける。
3. non-Web processが要求を読み、未使用・期限・対象・真正性を検証。`IdentityQuery`で依頼者の存在・ACTIVE・現在permissionを再確認し、Applicationのscope条件も確認する。失敗・未確定なら送信しない。
4. 復旧lockと要求の実行権を確保し、対象snapshotを再照合。必要な送信前記録のcommit成功を確認してから、同じ論理通知keyで対象listenerへ再送する。
5. 対象publicationの今回の試行、provider受理、通知log、Audit / 要求結果を照合して終了する。不明・lock喪失・送信後記録失敗は保留とし、新しい証拠で人が再判断する。

要求を知っていることは権限の証明ではない。DB role / credential等により正当な受付だけが要求を作れる条件、直接DB改変・同時消費・改変snapshotの拒否をreviewする。具体的DB権限・署名の要否・DDLは未選定。

再認可は送信直前へ近付けるが、照会後の変更を完全排除したと説明しない。失効の適用時点、既に開始した外部送信の扱いと検証条件を決める。許可後にuserが無効化された負例も設ける。

## 3. 実装前review票

Owner一人＋Codexで順次確認する。下表はreview結果ではなく、具体的な判断対象を提示する票である。判定は未判定から開始し、採用 / 条件付き / REWORK / 保留、理由・対象・残条件をOwnerの明示に基づき記録する。条件付き採用を開始前の必須条件未成立のまま実行へ転用しない。

| 票 | 推奨候補 / 変更責務 | 確認する出口・case | 未解決条件 |
|---|---|---|---|
| RV-1 主体 / 認可 | 認証済み要求＋IdentityQuery再確認、Applicationが明示認可。Reference-owned | E12 / E16に未認証受付・userなし / DISABLED・権限削除・scope外・期限切れ・照会失敗・送信直前失効を対応付け | permission / scope、認可実装方式と失効時点・Audit分類。SecurityContext自己申告で迂回しない |
| RV-2 要求・台帳 | Application-owned要求とoperation記録、1件選別・競合に耐える消費 | E13に同一要求 / 別要求の同対象競合、改変・旧snapshot・既存別publication終端、crash後不明を対応付け | schema・一意性・実行権 / transaction・DB保護。二重消費防止とprovider冪等性を別検証 |
| RV-3 専用process | 同一artifactの明示non-Web / 単発、復旧Bean / executor、CP8型lock | E04〜07 / E11〜14で通常起動・再送の区別、対象試行終端、lock喪失・終了と証拠を確認 | mode・Bean・auto処理制御、method security / 直接認可の実効性、timeout。新Framework runner APIは先行しない |
| RV-4 保存 / 規約 | JDBC＋UPDATE、kkref履歴維持を優先比較、明示Level選択・現行入口互換 | E01〜03、migration fresh / upgrade・Starter統合、Rule / 負例・互換検証へ | 正式依存・DDL、API / Levelのscope。Rule 28の単純除外で通さない |
| RV-5 Audit / 運用 | 送信前と送信後を分け、Business / Securityの意味・transaction契約を保つ | E15〜17、拒否・保留・調査 / 連絡・結果回復、DB正本Auditを突合 | 分類・actor / resource、必要記録commit、上限解除、保持・人の判断記録 |
| RV-6 実観測 | structured log＋Application metric＋標準tracing integration / exporter / 隔離sink | E15 / E20 / E21、別process span・短命process flush、sink不達・context非漏えい、PII / cardinality | library / version・sink・非Webexport、FAILED起点、alert / 保持・費用 |
| RV-7 DoD / 開始経路 | 手動復旧の全体 / 操作時間とstub範囲、初回S1限定P4-F案 | 4-2 / 4-3のOwner解釈、F-1〜5、前置review / 個別開始CP整合 | 数値・稼働・環境・上限、OR / 作業8・9 / Gate判断。今回完了扱いにしない |

## 4. 環境preflight案

**今回の状態は全項目未実施。** 各段階の操作範囲を確認してから実施する。既存PL2 fixtureの実行許可を新しいReference / collector / stub環境の許可へ広げない。read-only診断と、install / build / container起動等の状態変更を分ける。

| 段階 | 確認操作の候補・採取結果 | 停止条件・対象境界 |
|---|---|---|
| PF-1 source | branch / HEAD / status、対象文書・承認記録、artifactと設定の候補identity | dirty差分を固定sourceへ紛れ込ませない。今回review票だけで実装開始を認定しない |
| PF-2 ローカル診断 | Java version、Wrapper設定、Docker version / info / ps / images、端末CPU / memory・空き容量・port | read-only診断でも環境権限手順へ従う。接続拒否だけでEngine停止としない |
| PF-3 build / 依存 | 選定後のeffective POM / dependency tree、build・checksum、必要ローカルsnapshot | Mavenはcache / 出力へ書く。実行対象・権限を確認。Boot / Modulith / trace依存を実効値で記録、version競合なら停止 |
| PF-4 隔離資源 | PostgreSQL・通常 / 復旧process・stub / sinkのimage / version・DB / port / volume・credential・台帳 | 未承認のcontainer・実案件DBへ接続しない。通常2＋復旧2等の必要資源を実測、上限値はOPEN |
| PF-5 non-Web / 主体 | Web portなし、実効Bean・executor、要求認可 / IdentityQuery・Audit、auto処理無効、終了・flush | 認可なし・user-ID自己申告・不要なWeb / scheduler起動なら実演前停止 |
| PF-6 観測 / cleanup | 時刻・時計差、log / metric / trace / Audit・provider照合、alert受信、証拠保存 / checksum、終了資源 | 欠落材料を成功と推定しない。cleanupは当該台帳資源のみ、採取・判定・未確定副作用確認後 |

この表はコマンド実行やDocker / Maven検証の新規承認ではない。承認対象が確定した段階で再現可能なコマンド・必要権限・Evidence保管先を揃える。

## 5. 再見積差分の整理

既存残作業案176〜344標準作業時間は低確度の比較材料である。新たに明確になった作業をWBSへ割り当て、含まれていた作業と純追加を分ける。review・人の実演参加・外部待ち・環境金銭費は引き続き別枠OPEN。

| 詳細化した作業 | 元WBS | 差分の扱い / 再見積に必要な材料 |
|---|---|---|
| IdentityQuery接続と権限再確認 | W02 / W04 | 既存read APIが存在するため、新Framework user照会API開発を前提にしない。Application認可・失効窓・拒否testを具体化 |
| 認証済み要求受付・保存・消費 | W02 / W04 / W08 | 「再送入口・操作記録」の具体化であるが、新しい受付 / schema / 競合制御が必要。元range内か純追加かは設計後に判定 |
| 専用non-Web Bean / executor・認可・終了 | W04 / W06 | CP8知見を利用し、正式Reference構成・終端誤認負例・短命process観測は追加準備。probeコピーで工数を0にしない |
| tracing integration / exporter / sink | W07 / W03 / W06 | 元W07に実traceが含まれる。依存 / sink選定・flush・収集の実装差分を具体化し、全額追加して二重計上しない |
| 不明・失効・要求競合の個別枝 | W09 / W10 | E12 / E13 / E16 / E20等の既存case内の追加枝。共通setupを共有し、枝数・再現時間・証拠作業で再算定 |

**現時点で新総額・新数値上限は提示しない。** 実装方式・schema・sinkを選定していないため純追加量を算定できない。これは追加0でも既存上側以内の確定でもない。既存rangeを予算と扱わず、次のreviewで採用候補に対応したW01〜10の更新表・共有費・除外・上限案を提示する。

## 6. 次の判断と作業区切り

**保護・相関・起動条件:** [後続review入力](phase4-s1-permission-db-executor-startup-review-20261005.md)で用途別DB権限、消費巻戻し防止、requestId / operation / traceの実伝播、送信不能な確認modeと事前migrationを整理した。RV-2 / 3 / 5 / 6の実装前条件へ接続するが、実動・採用判定は未実施。

**最小方式への再review:** [許可引渡し・対象試行識別案](phase4-s1-minimum-permission-and-attempt-review-20261005.md)を最新の具体的比較入力とする。1件の許可と消費 / 確認終了事実、既存publicationの前後観測・operation相関を検討し、4種台帳と拡張方式の見積を採用前提にしない。RV票は未判定。

**後続具体案:** [方式候補・保存構造・依存・環境上限案](phase4-s1-request-storage-dependencies-environment-draft-20261005.md)にRV票の推奨候補、要求 / 試行 / 実行権 / 判断履歴、Tier見直し、依存候補と数値仮値を整理した。比較用作業量は純追加24〜48時間・同じ比較範囲200〜392時間の低確度案で、実施済み量未控除。下記の未判定・未実施状態は維持する。

今回、Public接続・具体的review票・preflight順・見積差分をまとめた。RV-1〜7は未判定で、preflightは未実施。次の中心判断は、認証済み要求方式・非Web実行構成・観測のApplication / Tooling配置を初回review候補として絞ること、その後にschema / 依存・環境 / 上限の材料を閉じることである。

Ownerの了解は対象票・条件・保留とともに記録する。詳細未確認の大枠了解を全票採用や実装開始へ変換しない。Gate設置・正本改訂・個別blocking review、DoD解釈 / 数値は独立した残判断として保持する。今回、新規code・POM・migration・Rules・CI・Gate規定・remoteは変更していない。
