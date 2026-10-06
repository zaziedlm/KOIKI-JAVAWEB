# S1 Reference基盤：初回採用判断表（2026-10-06）

**状態:** D1〜D6の推奨案・初回／後段条件区分はOWNER APPROVED（2026-10-06、§8）。Gate設置／通過・正本改訂・実装／環境実行開始は未承認。
**baseline:** `feature/phase4-s1-reference-foundation`／`f5e2672`＋未commitの検討・正式開始票・関連導線。今回もdocs／read-only source確認まで。
**対応:** [正式開始票§6・7](phase4-s1-reference-foundation-formal-start-review-20261006.md#6-開始経路blocking-review残条件)、[安全側の接続検討](phase4-s1-reference-safe-integration-design-draft-20261006.md)、[J1／J8正本](phase4-initial-scope-gate-and-open-decisions-draft-20261005.md)。番号D1〜D6は本表の判断IDであり、新Gateではない。

## 1. 採用判断表

「後段へ残す」は、条件・閉じる時点・未成立時の拒否を採用する判断。初回で未接続の運用Adapterを実装済み／PASSと認定するものではない。肯定側integration testに使う有限scope・Clock・対象／運用証拠はtest所有として識別する。

| 判断ID／項目 | 推奨案 | 理由 | 初回での必須条件 | 後段へ残す条件 | Owner判定欄 |
|---|---|---|---|---|---|
| **D1／J1：開始経路** | 既存のLevel 2限定P4-F提案をS1へ具体化する経路を採用候補とし、最初の実装対象はST-Cの保存・認可・Audit基盤に限定する | 既存計画に追跡でき、基盤だけを別例外として先行する解釈を避ける | 初回範囲・除外の採用、必要な前置reviewの位置を明示。Gate設置／判定と正本整合が必要なら実装前に成立させる | A2通知・D1非同期観測・復旧／パージの個別開始。P4-AR6・AR-D10・Gate P4-ARの未達とPhase 4全体判断 | **採用（2026-10-06）**：経路・初回範囲の方針。Gate判定は別 |
| **D2／J8：正本整合** | D1判断をP4-AR計画・Phase 4見直し草案・P4-F資料・AGENTS等の差分案へ対応付け、承認後に反映する | 技術票の了承とAgentの実行権限が食い違わないようにする | F-1〜F-5の提出状態、前置review／後続条件、限定開始の対象・停止点を示す。必要正本の改訂承認・反映とsource固定 | Customer入力・S0／S2再評価、DoD未達の最終判断。正式受渡し／remoteは別操作 | **採用（2026-10-06）**：差分準備・反映手順の方針。具体改訂は別 |
| **D3／scope** | Reference-ownedの照合契約で主体＋environment＋publicationを拘束。初回は契約・実Identity能力確認・供給不能時拒否を実装し、運用割当Adapterは後段 | Identityへ業務属性を持ち込まず、能力だけで全対象を操作可能にしない | §2の入力／結果・scope先行Query・未接続拒否を採用。testの有限割当をproductionへ含めず、scope外／照会障害を検証 | 運用者の割当・管理者・信頼できる取得元・変更／失効。**運用Web受付を作る前**に採用する | **採用（2026-10-06）**：初回契約・拒否と運用供給元の後段化 |
| **D4／TTL** | 初回は注入Clock・UTC時刻・期限不変・`now < expiresAt`を採用。正式TTL／最大TTL／時計差許容は未固定、運用policy未接続なら発行拒否 | 候補の判定規則を実装でき、test値を根拠なく正式運用値へ変えない | §3のApplication生成・期限境界・期限切れでも未閉鎖／消費保持を採用。固定Clock／TTL値はtestだけ | 発行TTL／上限・発行に許される時間・時計差／基準時点。**運用許可発行を有効化する前**に採用する | **採用（2026-10-06）**：初回規則・拒否と運用値の後段化 |
| **D5／Audit** | 実Public Recorder、発行／消費／閉鎖は同transaction、拒否はSecurity別transaction。§4のコード・actor／resourceを初回候補とする | 保存基盤の成功・失敗の意味とrollbackを実装前に確定できる | §4対応表の採用、Business失敗rollback／Security失敗でも拒否、scope外でresource非露出、pool／lock順序の実DB検証 | provider結果・送信後記録・復旧／パージの分類、真正なworker停止／結果証拠。**各操作を接続する前**に採用する | **採用（2026-10-06）**：§4対応表。後段分類は維持 |
| **D6／DDL** | 許可＋append-only消費の2 table、§5のcolumn／制約案。追加locationと既存kkref履歴を両立させる案、正式grantは用途別 | 一度性と不変性をDBで担保し、既存schema／機能を保護する | §5の具体DDL設計とgrant方針の採用。fresh／upgrade、無効時新旧DB起動・履歴validation、既存row不変を実証。役割別grant実行案は実行前review | publication schema／保持／パージ・通常通知logと正式process運用。**該当migration／運用追加前**に独立review | **採用（2026-10-06）**：§5設計・grant方針。具体実行表は別 |

## 2. D3：scopeと能力の具体契約案

照合要求は、不変user ID、能力、environment ID、publication ID。scope契約は「許可範囲を確認できた」「範囲外」「取得不能」を区別し、後二者を拒否する。scope外と対象不存在の外部応答で存在情報を漏らさない。取得不能を全対象許可へfallbackしない。

能力候補は次の4 code：`NOTIFICATION:PERMIT:ISSUE`、`NOTIFICATION:PERMIT:READ`、`NOTIFICATION:PERMIT:EXECUTE`、`NOTIFICATION:PERMIT:CLOSE`。能力を実IdentityQueryで現在確認し、scope条件を保存／Queryの前に確認する。保存済み許可でも環境・対象を拘束して取得し、全件読取後のfilterで代替しない。SQL grantは行scopeの代わりにはならない。

初回に運用者のpermission seedやscope台帳を作らない。新しい第三の管理tableを追加せず、正式供給元は運用受付前の個別判断とする。初回の有効構成でscope供給が未接続の場合は、必要なApplication操作を拒否する構成を検証する。起動不成立と操作拒否の違いをRegistration testで明示する。

## 3. D4：期限規則と未決の運用値

発行Applicationが注入Clockから`issuedAt`を取得し、採用されたpolicyのDurationから`expiresAt`を決める。Domainは正の期間・不変期限と実行時の`now < expiresAt`を判定し、外部DTOの任意期限をそのまま信頼しない。productionへ固定Clock／fixture期限／無期限を追加しない。

初回は運用policy未接続の発行拒否と、test policyでの期限直前／一致／直後を確認する。正式のDurationと最大Durationは後段採用事項として未設定を維持する。この後段化をOwnerが採用した場合、初回Domain／Application契約の実装に運用TTL値の決定を必須としない。

保存は`timestamp(6) with time zone`／Java Instantの候補。DB精度で再読取した境界を比較し、nanosecond値の丸めを暗黙の期限延長としない。精度調整方針はmapping実装と実DBtestで確認する。期限切れは新しい消費を拒否する条件であり、未閉鎖対象の次許可禁止・消費保持を解除する条件ではない。

## 4. D5：初回Audit対応表

全codeはReference-owned候補。[AuditEvent](../../koiki-starters/koiki-starter-audit/src/main/java/org/koikifw/audit/AuditEvent.java)のPublic契約と既存schemaの長さを満たす案。Framework enum／API／migrationは変えない。

| 操作 | Recorder／eventType／action／result案 | actor・resource | 保存・失敗の意味 |
|---|---|---|---|
| 発行 | Business／`NOTIFICATION_RECOVERY_CHANGE`／`PERMIT_ISSUED`／SUCCESS | 確認済み発行者USER、resourceType=`NOTIFICATION_RECOVERY_PERMIT`／permit ID | permitと同commit、記録失敗で発行rollback |
| 消費 | Business／同eventType／`PERMIT_CONSUMED`／SUCCESS | 許可発行者USER、同resource。worker世代／operationはconsumptionで辿る | 消費と同commit。成功は消費確定であり、配信成功ではない |
| 確認終了 | Business／同eventType／`PERMIT_CLOSED`／SUCCESS | 確認済み確認者USER、同resource | 閉鎖と同commit。人の解決判断の記録であり、配信成功と同一視しない |
| 主体／能力／scope拒否 | Security／`NOTIFICATION_RECOVERY_ACCESS`／`PERMIT_ACCESS_DENIED`／FAILURE、reason=`ACCESS_DENIED` | 確認済み主体だけUSER、未確認はANONYMOUS。scope外／未確認targetはresourceを付けない | 別transaction、外側rollback後も拒否記録。記録失敗でも拒否継続 |
| Identity／scope依存確認不能 | Security／同eventType・action／FAILURE、reason=`AUTHORIZATION_UNAVAILABLE` | 確認できた範囲だけactorを分類、未知をUSER扱いしない | 確認不能による拒否として記録。確認処理失敗後に成功記録なし |
| 期限切れ／一意競合／target差異／証拠不足 | 成功Businessは記録しない。初回は安全な運用log／test Evidenceで原因・HOLDを区別 | 外部応答へ詳細を漏らさず、logもscope確認済み対象だけ | 自動消費削除／閉鎖／再送なし。技術競合を一律Security侵害にしない |

拒否のSecurity Recorderはpermit lock保持中に呼ばず、保存transactionを終了してから呼ぶ。失敗時の例外／応答と記録不能のlogを定義し、成功応答へ変えない。新しいUNKNOWN enumや任意metadataをAuditへ追加せず、運用証拠へ分ける。callerからのIDだけで「確認済みUSER」を生成しない。初回principal入力のtestは将来の本人認証経路の受入を代替しない。

## 5. D6：DDLの具体採用案

正式SQLはまだ生成しない。physical nameはReferenceの既存prefixへ合わせる候補であり、Toolingのschema／roleを移植しない。

| table案 | column案 | 必須制約／索引案 |
|---|---|---|
| `kkref_notification_recovery_permit` | `permit_id` UUID、`environment_id` varchar(128)、`publication_id`／`event_id` UUID、`listener_id` text、`expected_attempt` integer、`actor_id` UUID、`reason_code` varchar(128)、`issued_at`／`expires_at` timestamptz(6)、nullable `closed_at` timestamptz(6)／`confirmed_by` UUID／`result_ref` varchar(255)、`version` bigint | permit PK、required列NOT NULL／文字列非空、attempt非負、expires>issued、version非負・default0、閉鎖3列が全NULLまたは全非NULL。未閉鎖rowに限る(environment,publication) partial unique |
| `kkref_notification_recovery_consumption` | `permit_id` UUID、`operation_id` UUID、`worker_generation` varchar(128)、`consumed_at` timestamptz(6) | 全列NOT NULL、permit PK＋自module permit FK、operation UNIQUE、worker世代非空。更新／削除Repository操作なし、runtime UPDATE／DELETE／TRUNCATE拒否 |

Identity／publicationへのcross-owner FKは追加せず、Public Query／現在値照合契約で確認する。閉鎖の意味・証拠の真正性はDBのNOT NULLだけで保証しない。reason／resultは安全なcode／opaque参照に限定し、provider payload・秘密値を保存しない。初回は未閉鎖uniqueと相関unique以外の将来用索引・trigger・消費取消機能を追加しない。

grantは、発行列INSERT／閉鎖列＋version UPDATE／versionだけUPDATE／consumption INSERT／観察SELECTを用途別に割り当てる。immutable列UPDATEをgrantしない。roleはNOINHERIT・owner非継承、管理credentialはDDL／隔離DB setup限定。正式role物理名・既存Identity／Audit等への必要grantは別表で確定し、実行前の権限reviewに含める。DDL設計採用を任意grant実行の許可へ拡張しない。

開始票の`V4__create_notification_recovery_records.sql`／`db/migration/kkref-notification/`／既存`kkref_flyway_history`案を維持する。既存V1〜V3、既存default location、Framework二階層契約は変更しない。管理側の適用では両locationを含め、通常起動で追加locationを指定しない場合でもV4適用履歴とvalidationが両立することを**実DB必須証拠**にする。両立しない場合はmigration配置の判断へ戻し、global validation無効化・既存history削除・既存V1〜V3改変で通さない。

fresh／upgrade／適用失敗／無効＋旧schema／無効＋適用済みschemaの5 caseには履歴の比較を含める。追加無効時はEntity／Repositoryが不在であり、未適用tableをvalidateしないことも確認する。既存row不変、無効化後の記録保持、旧JARの対応範囲はEvidenceで確認し、破壊的down migrationを戻し方としない。

## 6. 初回採用で進められる範囲と開始条件

| 判断結果の組合せ | 進められる作業 | まだ進められない作業 |
|---|---|---|
| D1経路・D2整合方針を採用 | F-1〜F-5／正本改訂差分／前置reviewを具体化する文書準備 | 正本未整合・Gate未成立の正式実装 |
| D3〜D6と正式開始票のTier／登録方式を採用 | 正式開始条件とDDL／grant差分を確定する設計。正式開始条件成立後はモデル・条件付き構成・保存／認可／Auditのcodeとtestを作成 | 運用scope／TTL未決の実運用発行、Web／CLI受付、送信・閉鎖の運用証拠を未接続のまま有効化 |
| 必要正本反映・Gate／blocking review・検証上限・source固定・preflightが成立 | 開始票の初回基盤作成・隔離DB検証・既存25 class／E2E回帰。実行範囲は明示された対象だけ | 後段の新規runtime依存／Framework Rule／通知・復旧・provider接続／remote |

D3／D4の後段化を採用すると、**初回で確定するのは契約・拒否・判定規則、後段で確定するのは実運用の供給元・値**となる。初回の肯定側testは有限Adapterとtest policyを明記し、正式接続の未達をEvidenceへ残す。この区分を正式開始票へ反映し、scope／TTLの全運用条件を初回基盤の必須条件として一括要求しない。

初回の55 invocation／既存25 class／E2Eと資源・工数上限は[正式開始票§4・5](phase4-s1-reference-foundation-formal-start-review-20261006.md#4-必須検証の作成計画)の**別の採用判断**。本表の採用だけで上限・環境実行を承認済みとしない。後段化により必須枝を削らず、Registration testの未接続拒否と肯定側testの限定条件を照合する。

## 7. J1／J8を閉じるための提出物とOwner記録

| 提出物 | 現在の入力 | 追加して審査する内容 |
|---|---|---|
| F-1 | `f5e2672`とA／B／C受入、既存Phase 3／P4-AR Evidence | 設計文書commit後のclean source identity、既存accepted baselineとの差分。実チーム入力の未達を保持 |
| F-2 | 本表と正式開始票の責務・配置・依存／Rule除外範囲 | D1〜D6採否、必要なDDL／grant review、A1／A2／D1全体との境界・責任分担 |
| F-3 | 既存実演計画とTooling結果、初回保存の検証案 | package実演の残条件を明示し、初回前置と後段前置の境界を正式に判断。初回testを全DoD実演の代替にしない |
| F-4 | 初回24〜40時間等の未承認案・Owner＋Codex体制 | 初回と後段の概算／上限・Owner稼働／環境待ち、commit／戻し方・実行資源を提出範囲に整合。初回値でS1全体を充足としない |
| F-5 | 現行P4-AR／AGENTSとP4-F提案、J1／J8 | 対象を明記した正本改訂差分、前置review・停止点・残す義務、Ownerの改訂／Gate判定記録 |

本表はD1／D2の推奨経路を提示するが、F-1〜F-5を充足済みと記録しない。前置reviewと後段条件の工程をAgent判断で入れ替えず、必要な計画・正本差分をOwner審査へ渡す。

Owner判定はD1〜D6ごとに「採用／条件変更／保留」、変更点、後段条件と閉じる時点、日付を記録する。経路方針の採用、Gate設置／通過、正本改訂、実装／実行開始の承認はそれぞれ区別する。今回の「採用判断表の整理へ進める」という指示は作成指示であり、表の内容の採用記録ではない。

## 8. Owner採用判断（2026-10-06）

Ownerは本表D1〜D6を確認し、「問題ないと判断します」と示した。D1〜D6の推奨案・理由・初回必須条件・後段条件を提示内容のまま採用する判断として記録する。変更条件の指定はない。

D1は既存P4-F提案をS1へ具体化する経路方針と初回限定範囲、D2は必要提出物・正本差分を整えて承認後に反映する方針の採用。未作成の正本改訂差分の承認やGate設置／通過を認定しない。D3／D4は契約・規則・未接続時拒否を初回、運用供給元・割当・TTL値等を運用受付前へ残す区分を採用。D5は§4のAudit対応、D6は§5のDDL設計・grant方針とmigration履歴validation条件を採用する。

必要grantの具体実行表、正式開始票のTier／登録方式・検証上限、source固定・preflight・実行開始判断は残る。§7のF-1〜F-5を充足済みとせず、次は採用済み方針に沿ったJ1／J8の提出物・正本改訂差分の準備へ進む。正式code／SQL生成、環境実行、Framework変更・remoteへ本採用を拡張しない。§7末尾の未採用状態は表作成時の履歴として維持する。
