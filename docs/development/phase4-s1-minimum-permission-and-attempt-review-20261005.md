# S1最小方式：non-Web許可引渡し・対象試行識別案（2026-10-05）

> **現在の参照先（2026-10-05）:** 本書の1件許可record優先比較は検討履歴。現在の初回優先候補は許可＋append-only消費の2種類。Tier候補・方向性了解・局所開始は承認済み方式票へ追跡し、正式採用・runtime PASSとは区別する。 [最新引継ぎ](phase4-s1-local-verification-start-handoff-20261005.md)／[承認済み方式票](phase4-s1-method-ballot-and-local-verification-contract-20261005.md)。

**状態:** DRAFT / 最小方式の比較・source照合。方式・Tier・permission / schema・実装 / 実演開始は未判定。
**方針:** [最小構成と将来管理機能の分離](phase4-s1-minimum-demonstration-scope-review-20261005.md)に従う。Ownerは同資料を確認し、本段階への継続を指示した。人が停止・DB・provider・Auditを総合確認する。
**Ownership:** 許可・復旧実行と最小記録はReference `notification`候補、採取・故障注入・provider stubはTooling。共通Frameworkの許可発行API / Starterを追加する前提は置かない。
**確認:** `docs/daily-development-workflow` / `e75a70a`＋未commit文書。Repository sourceとローカルMaven cacheのModulith `2.1.1` JARをread-only確認した。`javap`は署名確認のみで、dependency tree・runtime動作・Maven testではない。

## 1. sourceで接続できる部分と不足

| 対象 | 確認した事実 | 最小方式への適用・限界 |
|---|---|---|
| [IdentityQuery](../../koiki-starters/koiki-starter-identity/src/main/java/org/koikifw/identity/IdentityQuery.java) / [IdentityUser](../../koiki-starters/koiki-starter-identity/src/main/java/org/koikifw/identity/IdentityUser.java) | 不変user IDから現在status / permission集合を読める | 認証済み発行者を実行前に再認可できる。照会自体は本人認証でも許可真正性の証明でもない |
| [BusinessAuditRecorder](../../koiki-starters/koiki-starter-audit/src/main/java/org/koikifw/audit/BusinessAuditRecorder.java) / [AuditEvent](../../koiki-starters/koiki-starter-audit/src/main/java/org/koikifw/audit/AuditEvent.java) | 既存transaction必須。resource ID・reason codeを指定可能 | 許可ID / operation IDで関連付ける候補。許可を消費したAudit SUCCESSを通知成功と説明しない。Business / Security分類は別review |
| Modulith EventPublication `2.1.1` | `getIdentifier` / `getEvent` / `getStatus` / `getCompletionAttempts` / `getLastResubmissionDate` / `getCompletionDate`がある | 対象publicationと観測前後を照合可能。公開interfaceにlistener ID取得methodはない。actor・停止証拠・外部受理・今回operation IDも提供しない |
| Modulith再送API `2.1.1` | IncompleteEventPublications / FailedEventPublicationsはfilterで選別できる。返り値は`void` | 復旧対象を限定する接続候補。呼出し終了は再送受付数やlistener完了の証明ではない |
| [ExclusiveRecoveryProbe](../../build-support/phase4-level2-verification/src/main/java/org/koikifw/buildsupport/phase4/ExclusiveRecoveryProbe.java) | predicateはpublication ID等を照合するが、待機は同event全体のCOMPLETED / FAILED件数を使う | 正式候補では対象行の今回の変化を確認する。既存probeをそのまま正式実装にしない |
| [Tooling schema](../../build-support/phase4-level2-verification/src/main/resources/db/migration/V1__probe_schema.sql) | publication行にlistener_id・completion_attempts・last_resubmission_date等を持つ | listener照合・終端読取のAdapter候補。LIKEによるevent ID部分一致を正式識別にせず、eventを型として照合する。schema互換と読取権限はreview |

Spring公式の[EventPublication API](https://docs.spring.io/spring-modulith/docs/current/api/org/springframework/modulith/events/EventPublication.html)と[イベント処理説明](https://docs.spring.io/spring-modulith/reference/events.html)も照合した。参照時の表示versionは2.1.1で、current URLの将来変更を避けるため実装時は対象JAR / 依存値を再固定する。completion attemptsや再送時刻は処理の識別材料であり、送信回数・元worker停止・provider成功の証明ではない。既存[PL2 Evidence](../architecture/validation/phase4-pl2-level2-verification.md)にも実listener実行前のattempt増加が記録されている。

## 2. non-Webへの許可引渡し比較

| 候補 | 必要な構成 | 今回の位置づけ |
|---|---|---|
| A：DBに1件の実行許可を保存 | 既存Web認証・CSRF / 権限確認で許可を作成。non-Webが現在権限と対象を再確認し原子的に消費 | **最小候補として優先。** 4種台帳・受付画面・承認workflowを必須とせず、1件の許可と消費事実に絞る。DB保護は必要 |
| B：署名付きfile / token | 信頼できる発行、鍵管理、対象 / 期限拘束、失効、永続的な再使用防止 | fileだけでは単発消費にならず、消費記録も必要。初回既定にはしない。独自Framework token機能を作らない |
| C：non-WebからWebへオンライン確認 | 認証できるmachine経路と、対象1件の許可を返す既存または新しい契約 | Web停止 / drain方式との整合、通信不能時、machine認証の追加が必要。単なるuser ID照会では成立しない |
| D：OS起動権限・user ID引数・確認fileのみ | 操作は少ないがApplication本人 / 現在permission / 真正な許可へ接続できない | 正式再送許可に使わない。停止確認fileは人の確認材料としてのみ使用 |

Aは専用管理画面を前提としない。既存認証に接続する小さな受付経路を候補とし、具体route・DTO・permission codeはreviewで定める。認証済みの人が対象1件について許可した事実を引き渡すのであり、復旧processが人として新たにloginしたとは説明しない。実行processは固定artifact・制御された起動 / DB credential・限定環境に属する信頼対象として別に記録する。

## 3. 候補Aの最小記録と順序

### 3.1 追加記録は「実行許可」一種類から検討する

次は論理項目案で、DDL / Entity / 型の確定ではない。

| 項目群 | 最小情報案 | 目的 |
|---|---|---|
| 発行事実 | 許可UUID、認証済み発行者user ID、理由code、発行 / 期限、証拠参照 | user IDの自己申告を排除。人の判断理由・停止 / provider確認へ結ぶ |
| 対象拘束 | 環境ID、publication UUID、event型 / ID、listener識別、観測status / attempts / 最終再送時刻 | 他環境・別publication・古いsnapshotへの使用を拒否 |
| 消費事実 | 消費時刻、operation UUID、worker世代、実行前認可確認時刻 | 1許可1実行。operation IDはこの行へ保存し、再起動して同許可を再消費しない |
| 確認終了事実 | 人の結果確認参照、確認者user ID、確認終了時刻 | 未確認の旧操作を残したまま別許可へ進めない。終了記録は通知成功とは限らず、未解決副作用が残る場合は閉じない |

許可の対象・発行者・理由・期限は発行後に上書きしない。消費・確認終了の事実だけを権限付き経路で追記する候補。要求一覧・状態enum一式、試行専用table、実行権専用table、判断履歴専用tableは初回前提から外す。既存通知log / AuditとTooling Evidenceを併用し、必要情報が不足する場合だけ追加する。

同一環境＋publicationで未確認終了の許可を複数作れない一意制約等を候補とする。未消費の期限切れでも自動的に制約から外す方式は採らず、認証済みの取消 / 確認を経て閉じる。消費済み・結果不明は単なる取消で解除しない。この制約は新たな台帳サービスではなく、再使用・別許可による同対象競合を防ぐ最小条件として必要性をreviewする。

### 3.2 実行の順序案

1. **発行:** 認証済みReference経路で現在権限・scope・対象を確認し、許可と必要Auditをcommit。失敗なら有効な許可は作らない。人の停止 / provider確認は必要に応じ発行後、送信直前にも更新する。
2. **準備:** non-Webに許可IDを1件渡す。IDは検索keyであり秘密の認証tokenではない。信頼されたprocess構成とDB保護下で許可を読み、現在user / permission・期限・環境 / 対象snapshotを再照合する。
3. **排他・消費:** 復旧lock保持・通常process停止確認後、未消費かつ有効な許可を条件付きで消費する。operation ID・worker世代と必要送信前Auditを同じ短いtransactionでcommit。更新1件でない、またはcommit不明なら送信せず保留する。
4. **再送:** lock・snapshot・認可 / 期限の送信直前条件を再確認し、対象1件を再送する。消費済みは「実行権を使用した」であり、listener開始や外部受理の証明ではない。
5. **照合:** §4に従い対象試行・provider・通知log・Auditを確認する。crash / timeout / lock喪失は許可を未消費に戻さず、旧worker停止・副作用の照合を人へ引き渡す。
6. **確認終了:** 人が証拠を突合し、必要記録を権限付き経路で保存してから次の許可へ進む。送信成功の場合と、送信前拒否 / 未送信確認の場合を区別して記録する。結果不明なら閉じず再送保留。

確認終了の記録経路が必要な分だけWebを再起動する場合、通常listener / auto再送を意図せず再開しない起動条件をreviewする。停止方式と両立できないなら候補AはREWORKであり、無認可のDB直接更新へ置き換えない。

DB roleは、発行経路が許可を作成でき、実行経路が対象 / actorを改変できない条件を必要とする。Framework / ReferenceのJPA・Audit・Flywayに必要な権限を含め実効testする。保護できない共有credentialを使うなら、この候補の真正性は成立していない。

## 4. 対象の「今回の試行」の識別

識別を三つに分ける。**通知key**は初回と復旧で同じ論理通知、**publication ID**はeventを特定listenerへ届ける記録、**operation ID**は人が許可した今回の復旧操作を表す。operation IDを通知keyへ使って冪等性を失わせない。

| 時点 | 確認する情報・条件 | 誤判定を避ける点 |
|---|---|---|
| 再送前 | 対象publication ID・listener・型 / event ID、status、attempts=N、最終再送時刻R、旧worker停止・provider受理状況 | 同eventの別publicationやJSON部分一致だけで選ばない。FAILEDだけで停止 / 未送信としない |
| 再送選別 | 許可snapshotと同じ対象をpredicateで指定し、非対象の処理を起動しない | predicateは実行lockや原子的なpublication claimの代替ではない |
| 再送後 | 同じpublicationで最終再送時刻の新しい変化、attemptsの前後、実listenerのoperation / worker相関を観測 | 再送前FAILEDや別publicationのCOMPLETEDを今回終端に使わない。voidの返却だけで実行済みにしない |
| 今回終端 | 対象行の今回の変化に対応する終端とlistener記録、同じ通知keyのprovider受理、通知log / Auditを突合 | 回数増加だけでlistener / provider成功としない。時刻のみも重複・精度問題がある。stale機構によるFAILEDは生存worker停止を証明しない |
| 期待外変化 | 予想外の複数試行、snapshot再変化、相関欠落、当該終端未確認 | 他worker / auto再送混入の可能性として保留。都合のよい最終statusを拾わない |

N→N+1を期待値候補とするが、attempt更新時点とexecutor / registryの実効挙動を実演で確認するまで固定契約にしない。最終再送時刻Rは未再送ならnullがあり得る。速い処理では途中PROCESSINGをpollで見逃すため、「途中状態を見た」ことを成功の必須条件にせず、永続前後と実listener記録を併用する。

**追加調査が必要な接続:** 標準listenerへ渡るeventだけではpublication ID / operation IDを必ず取得できると確認できていない。単発復旧processのoperation contextを実executorへ伝え、対象event / listenerと照合してlog / 通知結果へ結ぶ候補を調査する。MDCに値を置いただけで伝播成立としない。既存TaskDecoratorとの共存、対象外taskへの非漏えい、短命process終了も確認する。標準経路で結べなければBLOCKEDとして方式を再reviewし、未承認のregistry内部API / 独自aspectを先行追加しない。

publication終端の参照はUPDATE方式の対象行を読むAdapterを候補とし、status / attemptsの手動更新はしない。public APIは再送選別に使い、必要な読取SQLは対象schema / listener照合・鮮度・migration互換をreviewする。provider受理をpublicationのCOMPLETEDだけから推定しない。

## 5. 不明時に残す最小証拠とcase対応

最低限、許可 / operation・対象publication・再送前snapshot・最後の観測時刻 / snapshot・worker世代 / 停止状況・通知keyのprovider受理 / 不明・通知log / Auditの有無・人の保留理由 / 次の調査を残す。operation消費後に何も残らなくても送信可能性は不明として扱う。Tooling logは支援証拠であり、必須DB Auditの代替ではない。

| case | 最小方式での追加確認枝 |
|---|---|
| E12 / E15 / E16 | user ID自己申告、許可改変・scope外・無効user / 権限失効・期限切れ、送信前commit失敗を拒否 |
| E11 / E13 | 同許可2process、別許可同対象、旧FAILED・別publication終端、予想外attempt / auto再送混入、相関欠落 |
| E06 / E07 / E14 / E16 | 消費commit後送信前crash、provider受理後記録失敗、lock喪失、commit結果不明。未消費に戻さず人へ |
| E20 / E21 | operation contextの実伝播 / 非漏えい、provider照会不能、短命processの証拠採取、保留判断と調査 |

各枝は未実演。今回の方式で満たせると確定したcase一覧ではない。元E01〜21の目的を維持し、方式変更に伴う枝対応は採用候補が揃った段階で更新する。

## 6. 次のreview出口

**後続具体化:** [DB保護・executor相関・結果確認起動条件](phase4-s1-permission-db-executor-startup-review-20261005.md)で、列権限だけでは消費巻戻しを防げない点、現行decoratorがrequestId限定である点、確認Webで送信Beanを起動しない条件を整理した。1 recordの狭いDB保護とappend-only消費recordを比較し、table数だけを目的としない。DB-1 / 2・EX-1 / 2・ST-1は未実演。

最小候補は「1件の許可record＋既存publication / 通知log / Audit＋Tooling Evidence」とした。これでも発行 / 消費 / 確認終了の安全条件は残り、無条件にTier 1へ収まるとは説明しない。まず許可のDB保護と単発消費、対象試行への実相関、確認終了時の起動条件をsource / 設計で閉じる。その後にnotificationのTier・DDL / 依存・工数 / 上限を判断する。今回、新たな数値見積やTier採用は提示しない。

RV-1：認証済み発行＋現在権限、RV-2：1種類の最小許可recordと不明保全、RV-3：単発modeと確認終了の起動条件、RV-4：publication選別 / 読取互換、RV-5：送信前Audit・結果照合、RV-6：実相関、RV-7：DoD / Gateに対応するreview入力であり、票判定ではない。code・POM・migration・環境起動・remoteは変更していない。
