# S1正式接続条件：責務・認可／失効・Audit・mode／DB権限案（2026-10-05）

**状態:** DRAFT / 具体化・追加検証契約への入力。4項目の方針継続は[方式票§8.3](phase4-s1-method-ballot-and-local-verification-contract-20261005.md#83-owner方針レビュー承認記録2026-10-05)でOWNER APPROVED。本書の具体案・正式Tier・code／POM／migration・追加検証開始は未承認。
**Owner確認（2026-10-05）:** 本書を確認し、追加検証契約・再見積へ進める指示を受領。[追加契約案](phase4-s1-additional-local-verification-contract-and-estimate-draft-20261005.md)へ渡す。確認を個別実装方式・物理名固定・検証開始の包括承認としない。
**位置:** [後続タスク](phase4-s1-follow-up-tasks-after-local-verification-20261005.md)順2。設計対象はReference-owned `notification`候補、今回の変更はdocsのみ。Owner一人＋Codexで順次進める。
**入力:** commit `5252124`の[局所Evidence](../architecture/validation/phase4-s1-minimum-local-verification-20261005.md)、[変更一覧CH-01〜08](phase4-s1-minimum-change-and-start-scope-draft-20261005.md)。正式名称・property／route・DDL番号・permission／Auditコード・期限値・追加資源上限は本書で固定しない。

## 1. source確認と設計上の制約

| 確認済みsource | 現在の契約と設計への入力 |
|---|---|
| [IdentityQuery](../../koiki-starters/koiki-starter-identity/src/main/java/org/koikifw/identity/IdentityQuery.java)／[IdentityUser](../../koiki-starters/koiki-starter-identity/src/main/java/org/koikifw/identity/IdentityUser.java) | `findById(FrameworkUserId)`はOptional。userId・status・permissionCodes・versionを照会できる。ACTIVE／DISABLEDを使い、emailをactorにしない。照会は認証・権限失効のlockではない |
| [Identity AutoConfiguration](../../koiki-starters/koiki-starter-identity/src/main/java/org/koikifw/identity/internal/KoikiIdentityAutoConfiguration.java) | IdentityQueryはJPA EntityManagerFactory条件。管理Beanは別の依存条件を持つ。non-Webの読取用途へIdentity管理画面／内部Entity操作を取り込まない |
| [Audit AutoConfiguration](../../koiki-starters/koiki-starter-audit/src/main/java/org/koikifw/audit/internal/KoikiAuditAutoConfiguration.java) | JPA／transactionを必要とする。BusinessはMANDATORYでpersistAndFlush、SecurityはREQUIRES_NEW。Bean存在だけでは消費と同一transactionの成立を証明しない |
| [AuditEvent](../../koiki-starters/koiki-starter-audit/src/main/java/org/koikifw/audit/AuditEvent.java)／[AuditActor](../../koiki-starters/koiki-starter-audit/src/main/java/org/koikifw/audit/AuditActor.java) | eventType／action／resultと任意subject／resource／reason。任意metadata mapはない。USERは不変user ID、SYSTEMはFramework-owned背景処理用。Reference workerの分類は別途確認 |
| [Security構成](../../koiki-reference-app/src/main/java/org/koikifw/reference/identity/configuration/ReferenceSecurityConfiguration.java) | HttpSecurity依存、既存matcherに新復旧routeはない。non-Web root scanをそのまま使わず、Web経路・default deny／CSRFを明示reviewする |
| [局所SQL](../../build-support/phase4-level2-verification/src/test/resources/s1-minimum/permit-storage.sql) | 未解決対象のpartial unique、permit／operation一意消費、web／worker／readerの権限がLOCAL PASS。正式JPA適合・正式role／migrationは未確認。audit_probeは実Recorderを代替しない |

## 2. 最小構成の責務配置案

notification全体は狭いRICH／JPA共有モデルを優先候補とし、許可＋append-only消費の2種類を維持する。下表の機能名は説明用で、新規Java型の生成指示ではない。

| 場所／所有者 | 責務 | 境界・追加証拠 |
|---|---|---|
| Inbound Web／Reference | 認証済みprincipalからactorを取得、形式検証、発行／確認Use Caseへの委譲、DTO応答 | actorをrequest bodyから採用しない。Entityを画面／APIへ出さない。permissionとCSRFを実Webで検証 |
| Inbound単発runner／Reference | modeとpermit／publication／operation IDを検証、復旧Use Caseを1回呼び、結果を採取して終了 | IDは選択情報。真正性／権限／送信可否をrunner単独で決めない。欠落・誤modeは起動拒否 |
| Application／Reference | 現在権限・scope・停止証拠・対象snapshotの確認、transaction、Domain操作、Audit、commit後の標準再送調整 | HTTP／SQLを置かない。消費確定後の失敗は消費を巻き戻さず保留。送信を行う実listenerでも相関と許可を照合 |
| Domain Model／Reference | 対象／actor不変、期限内・未閉鎖の消費可否、確認終了の可否を表現 | 権限照会／DB lock／provider照会はApplicationとAdapter。未解決対象の次許可禁止はDB制約でも守り、単一Modelだけで保証しない |
| Persistence Adapter／Reference | 2種類の保存、permit row lock、一意INSERT、対象読取／有限snapshot、意味を変えない制約違反の変換 | JPA flush／version／列権限を実DB確認。JPA共有モデル不成立ならSQL境界を独立reviewし、MyBatis／モデル分離を自動導入しない |
| Identity／Audit接続Adapter／Reference | 自moduleの必要なPortから既存Public契約へ接続 | Framework内部Entity、他moduleのRepository／Applicationを参照しない |
| publication／executor／provider Adapter／Reference | 標準Modulith再送、publication／event／listener突合、不変operation伝播、通知key付き外部I/O | Framework decoratorを変更しない。相関情報が欠落／不一致なら送信0。正常listenerと復旧listenerは識別可能にする |
| Configuration／Reference | mode別Bean／scan／Adapter／DB接続を組み立てる | 業務判断を置かない。機能不在をBean・route・DBの各面で検証 |
| 運用判断／Owner、証拠整理／Codex | 停止・受理・commit不明の総合判断、保留引継ぎ・確認終了 | 人の判断も対象ID・証拠参照・判断者を残す。消費済みを自動で未消費へ戻さない |

操作順の候補は、真正な許可と現在権限を確認 → 対象停止・snapshot確認 → permit lock下で条件再確認・消費INSERT＋Business Audit → commit成功確認 → operationを実executorへ渡す → listenerで相関照合 → 送信 → 人の結果確認である。再送APIが対象を選定しなかった場合も既存消費を削除しない。再起動して同permitを通常再実行する経路は設けない。

## 3. 認可・失効条件案

最小候補は、発行者が復旧許可の責任主体となり、workerはその許可を限定実行する方式。確認者は別に認証し、同一Ownerが兼務してよい。二人承認・運用workflowは必須化しない。権限コードは未定だが、能力を分けてreviewする。

| 操作 | 必須条件候補 | 拒否／保留条件 |
|---|---|---|
| 発行 | Web本人認証、ACTIVE、発行能力、環境／publication scope、停止と再送要否の根拠、期限、未解決許可なし | 主体不明、対象外、根拠不足、重複許可、Audit失敗は発行不成立 |
| 閲覧 | Web本人認証、閲覧能力、対象scope | Scope外の存在・詳細を漏らさない。閲覧だけでは消費／確認終了できない |
| 消費 | 信頼するDB接続と保存済み許可、発行者ACTIVE・現在の復旧許可能力／scope、期限内／未閉鎖／未消費、environment一致、停止証拠・対象状態／attempt一致、worker世代識別 | Identity照会失敗、期限切れ、失効、対象差異、不明な旧processは送信拒否。CLI user ID／operation IDだけで認可しない |
| 未消費取消 | 認証済み確認主体の取消能力／scope、permit lock下で消費なし、commit不明・旧workerがない証拠、Business Audit | 消費行なしだけで取消しない。不明なら未閉鎖を維持 |
| 消費後の確認終了 | 認証済み確認主体の確認能力／scope、worker終端／停止・provider／DB／Audit突合、判断と証拠参照、permit lockとBusiness Audit | 結果不明、旧worker稼働・候補選定不明は保留。終了は当該許可の解決記録で、送信成功の自動認定ではない |

発行時の権限を永久保存して使わず、消費transactionへ入る直前に現在情報を照会し、lock後の期限／閉鎖／消費を再確認する。IdentityUser.versionは観測値で、permission変更との原子的な失効を保証しない。**権限確認後から外部I/Oまでの失効窓は残条件**。送信直前再確認の必要性・許可を有効とする基準時点・運用停止手順を次の検証契約で明示し、再照会だけで窓が消えるとは説明しない。

期限切れ／DISABLED／権限除去は新しい消費を拒否する。消費済み許可・不明な結果の証拠は期限で削除せず、次許可禁止を維持する。送信直前の権限確認で拒否した場合も消費は保持し、送信0と保留を記録する。期限値・時計差許容・発行者変更時の扱いは未決。

## 4. Audit対応案

下表の分類・意味をreview対象とし、eventType／action／reasonの文字列は未固定。Business成功はその操作の成立を意味し、通知の最終配信成功と区別する。

| 事象 | 分類／actor候補 | transaction・失敗時 |
|---|---|---|
| 許可発行 | Business／認証済み発行者USER | permit INSERTと同一transaction。実Recorder失敗なら発行rollback |
| 消費確定 | Business／真正な許可に結び付く発行者USERを優先review。worker世代は実行情報として別に追跡 | consumption INSERTと同一transaction。Audit／commit失敗・不明なら送信しない。actor候補の適合をreviewする |
| 取消／確認終了 | Business／認証済み確認者USER | 閉鎖更新と同一transaction。失敗なら未閉鎖を保持し、解決済みと表示しない |
| 未認証／権限／scope拒否 | Security／認証済みUSERまたは主体未確認ならANONYMOUS | REQUIRES_NEWで拒否を残す。記録失敗でも拒否を解除しない。PIIや提示IDを無条件に保存しない |
| 一意制約競合／対象差異／期限切れ | 運用証拠を必須、Audit分類は原因ごとにreview | transaction失敗後に成功Auditを残さない。単なる競合を一律Security侵害と分類しない |
| provider受理不明／commit結果不明／送信後記録失敗 | 運用証拠で保留を記録。通知結果Auditの要否／分類は別review | 外部副作用をDB rollbackで取り消せない。再送・消費削除・次許可を自動実施しない |

相関は、Auditのresourceにpermit IDを置く候補とし、permitからpublication／event／listener、consumptionからoperation／worker世代を辿る。AuditEventに任意metadataを追加したりreasonへ全情報を詰め込んだりしない。DB消費前のoperation・拒否は運用logと証拠参照で補う。AuditResultの新enumは追加せず、未知／保留は対象・運用証拠で説明する。

原子性の検証は同じDB・整合したJPA transaction managerでpermit／consumptionと既存BusinessAuditRecorderを使う候補。JDBC publication接続とのflush／transaction共有、Security Auditの別接続とpool／lock待ちは実測対象。Security Auditをpermit lock保持中に呼び出して不要な待ちを作らない配置を検討する。複数DataSource／独自transaction managerを先行追加しない。

## 5. mode別Bean構成案

「確認」は観察、「確認終了」は人の判断を記録する更新であり、同じDB権限では扱わない。下表は機能上の構成候補で、5 JVM常時起動の計画ではない。確認Webと許可／確認終了Webの物理的な起動分割は、最小変更量とDB権限が両立する方式をreviewする。

| Bean／機能 | 通常Web | 許可／確認終了Web | 確認Web（観察） | 単発復旧non-Web | migration作業 |
|---|---|---|---|---|---|
| Servlet／Session／SecurityFilterChain／CSRF | 既存維持 | 必須・限定route | 必須・閲覧route限定 | 不在 | 不在 |
| IdentityQuery／Audit Public Recorder | 既存維持 | 必要 | 認証／拒否Auditに必要な範囲 | Query＋Business／Security | runtime操作なし |
| 許可発行／取消／確認終了Use Case | 通常への露出は別判断 | 能力別に必要 | 不在 | 不在 | 不在 |
| 観察Query | 露出は別判断 | 必要範囲 | 必要 | 有限対象照合のみ | 不在 |
| 通常通知listener／sender | A2正式review後 | 不在 | 不在 | 通常listenerの自動経路は不在。対象復旧用listener／senderのみ | 不在 |
| 復旧runner／再送入口 | 不在 | 不在 | 不在 | 対象ID必須、単発1回。完了待機／終了条件は別途定義 | 不在 |
| notification Entity／Repository | 通常処理に必要な範囲 | 許可／確認更新用 | 読取用構成 | 消費用構成 | DDLだけ |
| Modulith registry／scheduler／初期化 | A1／A2 reviewに従う | 読取に不要なruntime機能は不在 | 同左 | 標準registryと対象再送のみ。自動全件再送なし | schema管理のみ |
| Flyway／DDL実行 | runtimeでは無効候補 | 無効 | 無効 | 無効 | 正式review済みmigrationを明示実行 |

復旧対象listenerのproxy／registrationを成立させつつ通常eventの自動受付を閉じる構成は未実証。単にBean名を分けたことを安全証拠にせず、実proxy・event dispatch・scheduler・startup／shutdownを検証する。全modeでcomponent scanだけでなくEntity／Repository scanとauto-configuration条件を照合する。non-Webの認可はApplicationの明示確認を必須とし、Servlet条件付きmethod securityの自動継承へ依存しない。

## 6. DB権限表案

正式role名・grantは未固定。以下は論理権限であり、Framework内部tableにReference migrationで勝手にgrantを追加する指示ではない。複数schemaの必要権限を既存migration／運用Ownershipに従ってreviewする。

| データ／操作 | 通常runtime | 許可／確認終了Web | 確認Web | 復旧worker | migration管理主体 |
|---|---|---|---|---|---|
| permit読取 | 必要性を確認 | scope内SELECT | scope内SELECT | 指定許可SELECT | 管理 |
| permit発行・閉鎖 | 通常経路では不要候補 | INSERT発行列、UPDATE閉鎖／確認列＋version | 不許可 | 不許可 | DDLのみ目的を区別 |
| permit lock用version更新 | 不要候補 | 必要 | 不許可 | version列だけ | 管理 |
| consumption | 必要性を確認 | SELECTのみ | SELECTのみ | SELECT＋INSERTのみ | schema管理。保持／削除は別review |
| publication | 正式通知用の標準registry権限 | 読取のみ | 読取のみ | 標準再送に必要な限定DML | 正式schema管理 |
| Identity | 既存契約維持 | 認証／Queryに必要な読取 | 同左 | Query読取のみ | Framework-owned管理 |
| Session | 既存契約維持 | Web認証に必要なDML | 同左 | 不要 | Framework-owned管理 |
| Audit | 既存契約維持 | Recorderに必要なINSERT等、scope付き閲覧 | 認証／拒否Recorder用INSERT等、scope付き閲覧 | Recorder用INSERT等 | Framework-owned管理 |
| DDL／owner継承／消費改変 | runtimeには付与しない候補 | 同左 | 同左 | 同左 | 管理権限はruntime credentialと分離 |

確認modeは**通知・許可・publicationへの更新を禁止**するが、Session／Security AuditまでDB全体read-onlyにはしない。DBのscope制御はgrantだけでは実現せず、Application／Query条件で環境・対象scopeを強制する。用途別login／NOINHERIT・owner非継承・UPDATE／DELETE／TRUNCATE拒否を実DBで確認する。

## 7. 残判断と次の追加検証契約へ渡す項目

| 残判断 | 次に示す証拠／成果物 |
|---|---|
| JPAモデルと制限roleの適合 | 発行／lock／消費／閉鎖の実SQLとflush、immutable列非更新、append-only、一意競合、rollback。局所JDBC成功を流用しない |
| permission／scope・失効窓 | 能力別コード候補と対象範囲、ACTIVE／権限除去／照会障害／期限境界、権限照会から送信までの基準時点と拒否枝 |
| actor／Audit分類・原子性 | 既存Public Recorderでの保存内容・同transaction、失敗注入、Securityの別transaction／pool条件。SYSTEM流用なし |
| modeの物理構成 | entry point・Bean／Entity／Repository／auto-configuration一覧、実Web拒否／CSRF、復旧proxy、確認起動／終了副作用0、通常Web回帰 |
| commit不明の再起動保全 | 許可実行前から対象ID／worker世代／実行開始の運用証拠を保全し、再起動前に人が未解決を照合する手順。証拠欠落でも送信拒否。DB消費行なしでの自動解除なし |
| 停止・対象状態・確認終了 | 通常process／旧worker停止の証拠と対象snapshot再確認、未選定／provider不明時の保留。人の終了判断を記録し、次許可を安全に出せる条件を確認 |
| 正式変更と上限 | CH-01〜04／07の必要差分・独立review、追加test／dependency／path／command・資源／時間／回数上限、失敗時の戻り先 |

不明状態の永続引継ぎが2種類記録＋運用証拠で成立しない場合は方式を戻してreviewする。新管理台帳・自動再判定を説明なく足さない。本書の条件を次の追加検証契約へまとめて個別開始判断を受ける。今回は文書・source照合のみで、正式実装・fixture作成／実行・環境／remote操作は行っていない。
