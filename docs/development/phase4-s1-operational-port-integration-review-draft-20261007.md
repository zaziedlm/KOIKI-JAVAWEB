# S1後段：scope／TTL供給元・停止／provider証拠の運用接続レビュー案（2026-10-07）

**状態：DRAFT / Owner review入力。** 本書は後段の設計整理であり、候補の採用・code／migration／環境実行の開始承認ではない。

**確認source：** `feature/phase4-s1-reference-foundation`／`a18a76d`。作成前の作業ツリーclean。初回限定保存・認可・Audit基盤は[検証記録§13](../architecture/validation/phase4-s1-reference-foundation-20261007.md#13-初回限定基盤の差分レビューlocal検証結果のowner承認2026-10-07)でOwner承認済み。新規55件・既存99件・package済みE2E1件のPASSは初回の範囲に限定する。

**Ownership：** 運用接続はReference `notification`、provider stub・障害注入・有限入力は非配布Tooling／test。実案件providerの保証・運用担当は未取得。Framework Identityへ業務属性を追加せず、Public API／Rules／依存変更を先行しない。

**入力正本：** [採用判断D3〜D6](phase4-s1-reference-foundation-adoption-decisions-20261006.md)、[安全側の接続検討](phase4-s1-reference-safe-integration-design-draft-20261006.md)、[S-01〜11安全条件](phase4-s1-safety-and-recovery-conditions-20261005.md)、[許可と試行の照合](phase4-s1-minimum-permission-and-attempt-review-20261005.md)、[正式開始承認と後段境界](phase4-s1-reference-foundation-execution-review-20261006.md)。本書の判断ID O-01〜O-08はレビュー追跡用で、新Gate／runtime enumではない。

## 1. 現在ある契約と運用未接続の部分

| 接続面 | 現行sourceで確認したこと | 後段で埋める部分 |
|---|---|---|
| Identity／scope | `RecoveryPermitService.authorize`が認証済みprincipal、現在ACTIVE状態と4操作の能力を確認。`RecoveryScopePort`はuser・能力・environment・publicationを受け、ALLOWED／OUTSIDE／UNAVAILABLEを返す | 割当の所有者・正本・管理者・失効時点。既定はUNAVAILABLE |
| TTL／Clock | `RecoveryTtlPolicyPort`からDuration、ClockからUTC時刻を取得。正のTTL、DB精度、期限一致時拒否を実装 | 正式TTL・最大TTL・基準時計・時計差・policy変更／失効。既定policyはempty |
| 現在対象 | `RecoveryTargetPort`はenvironment・publicationに対するevent・listener・attemptを含む5項目snapshot | 信頼できる取得元、状態適格性、更新／削除／再試行との競合、鮮度。既定はempty |
| 消費証拠 | `ConsumptionProof`はpermit・target・operation・worker世代・opaque evidenceRef。Applicationは一致と参照の形式を検査 | 元listener停止、移譲／再起動抑止、provider状態、証拠発行者・期限・改変／再利用防止。一致だけでは真正性を証明しない |
| 閉鎖証拠 | `ClosureProof`はpermit・target・confirmedBy・resultRef。Applicationで一致を照合し、閉鎖＋Auditを保存 | 人の判断理由・照合結果・UNKNOWN保持、閉鎖してよい終端条件、次許可との関係 |
| 保存・Audit | 消費はappend-only、permit versionと共通row lock、Business Audit同commit。拒否は保存transaction終了後のSecurity Audit | 運用証拠の永続保管・送信前後の分類・worker本人認証／委譲。初回は消費主体＝発行者を要求 |

`RecoveryTarget`にはpublication status・最終再送時刻・payload識別がない。`ConsumptionProof`には停止観測時刻・証拠期限・provider受理分類がない。これらを現行recordだけで確認済みと扱わず、信頼できるAdapter内部で判定するのか、必要な値契約を拡張するのかを接続前に判断する。

## 2. scope供給元の候補と判断（O-01）

推奨は、**Reference-ownedの明示割当を、管理された正本から現在値として取得する方式**。能力確認と行scopeを分け、既存expenseの部門scopeやFramework roleをnotification全対象の許可へ流用しない。

| 候補 | 適用・利点 | 採用前の条件 |
|---|---|---|
| 管理された設定／台帳をread-onlyで取得 | 小規模な限定運用の候補。新管理画面を必須にしない | 改変権限、配備経路、environmentとの対応、割当version、更新／失効の反映時間、取得障害時拒否。任意の確認fileは正本にしない |
| Reference-owned永続割当 | 継続運用で管理履歴・失効を追える候補 | 管理責任者、追加schema／migration／grantの独立review、管理操作の認可・Audit。初回2tableへ無断追加しない |
| 組織の既存認可サービス | 実在し、対象scopeの意味が一致する場合の候補 | 所有者・API・本人認証・鮮度・障害／timeoutの契約取得。現時点ではサービスの存在自体がOPEN |

採用に必要な入力：割当責任者、環境一覧、対象範囲をpublication単位／集合のどちらで管理するか、操作ごとの割当、grant／revoke手順、変更履歴と失効反映上限。二者承認や担当人数は本書で一律要求しない。

scopeは取得前・更新前に確認し、Queryのscope条件も維持する。cacheを使う場合は失効窓を明示し、古い肯定結果で供給元障害を覆わない。実行直前の失効再確認は後段実行契約で定める。

## 3. TTL policyと時計（O-02）

推奨は、Reference-ownedの管理policyからDurationを生成し、Applicationで発行する方式。受付から任意の期限・無期限を渡させず、testの1分を正式値に採用しない。

| 決める項目 | 条件／現時点の状態 |
|---|---|
| 発行TTL・最大TTL | 運用の確認所要時間、証拠の寿命、provider重複抑止期間を入力に採用。数値はOPEN。正数だけでなく最大値をどこで強制するかを決める |
| policy正本・変更 | 管理者、配備／参照経路、version、欠落・不正・取得失敗時拒否。発行済み期限は変更しない |
| 発行を許す時点 | 停止／provider調査の前後と、証拠取得待ちによる失効を整理。期限延長や消費取消で回避しない |
| 時計・許容差 | 発行／消費processの基準時計、同期監視、逆行・許容差超過時の保留を決める。`systemUTC`だけで同期を保証しない |
| 期限と証拠の関係 | permit有効でも停止／provider証拠が失効したら保留。期限切れは未閉鎖uniqueや消費記録を解除しない |

## 4. 現在対象とpublicationの境界（O-03）

信頼できるpublication取得元と対象適格性の判定場所を定める。event／listener／attemptの一致だけからFAILEDや再送可を推測しない。版依存読取を採用する場合はReference Adapter内に閉じ、対象schema・version・権限を固定する。publication状態を手動UPDATEする方式は採らない。

本Repositoryの通常Referenceには初回でModulith publication runtimeを追加していないため、現時点で運用publication供給元を実装済みとして選べない。対象取得の設計整理と、publication schema／Modulith依存・runtime／Ruleの接続reviewを別の判断として追跡する。

取得後に対象が変わる窓を設計する。permit row lockはpublicationや通常listenerをlockしない。消費commit後・送信直前にも必要な現在値と安全条件を再確認し、差異や取得不能なら送信せず消費を保持する。

## 5. 元listener停止と証拠の真正性（O-04）

限定環境では、関連する通常processを停止・drainし、再起動／task移譲を制御する方式を最初のレビュー候補とする。実運用で対象workerだけを識別できる方式と比較し、全停止の可用性影響・対象範囲・再開順も判断する。方式は未採用。

必要な証拠は、対象publication／listener、元process／containerと起動世代、deploy revision、採取者と信頼元、観測時刻・有効期限、drain／終了結果、別instanceへの移譲有無、確認後の再起動制御。PIDのみ・任意boolean・FAILED状態・復旧worker lockだけでは停止認定しない。

証拠取得後に通常listenerが再開し得るなら、照合だけでは足りない。実行期間の再開抑止／fencing等の必要性をreviewし、維持を確認できなければ保留する。新workerがlockを取っても旧workerの送信中副作用は消えない。

## 6. provider受理・閉鎖判断・証拠保管（O-05／O-06）

providerは未選定。Tooling stubのevent ID一意受理を実案件保証に読み替えず、まずReference実演用stubの契約と実案件providerへ求める契約を分ける。

| 判断材料 | 採用前に確認する契約 |
|---|---|
| 安定した通知key | 初回・再送で同じ論理通知のkeyを維持。retry／operation IDを毎回新しいprovider keyにしない。通知種別・宛先・payload同一性、key保持期間、期限後の扱いを定める |
| 受理照合 | 対象key・受理ID・payload識別を結ぶ。受理後の応答喪失、確実な拒否、照会障害を区別。APIなしでも安全な同key再要求が成立するかは別途証明する |
| UNKNOWN | 不明を未送信へ変えず、保留・調査・エスカレーションと理由を残す。消費を削除／未消費化せず、DB完了だけでprovider受理としない |
| 閉鎖 | 人の確認主体、判断理由・結果・次の処置と停止／provider証拠を照合。未解決のまま閉鎖して次許可を出せる設計にしない。配送成功・調査終了・再送不要等の意味と条件はOPEN |
| 保管 | immutableな証拠／判断記録へopaque参照で辿れるようにする。管理者、アクセス制限、保持期間・改変検知・監査、provider秘密やpayloadを保存しない方針を定める |

**現行契約で特に審査する点：** `consume`は`evidenceRef`を検査するが、`RecoveryConsumption`はpermit・operation・worker世代・時刻の4項目のみ保存し、Business Auditにも証拠参照を追加していない。閉鎖はpermitに`resultRef`を保存する。消費証拠をoperation等から独立保管先で一意に辿れる契約とするか、Reference記録／migrationを追加するかは未決。参照の存在だけで真正性・保持・改変防止を保証しない。

## 7. transactionと実行主体の接続（O-07）

現行scope／target／evidence呼出しは`TransactionTemplate`内にあり、消費／閉鎖のtarget・evidence確認はpermit row lock取得後。ここへ無制限なplatform／provider通信を直結するとlock・poolを長時間保持し得る。Springの10秒transaction設定は全外部I/Oの強制終了保証ではない。

候補は、遅い証拠採取をtransaction外で行い、短いtransaction内では信頼できる保存済み証拠の鮮度・対象・世代・失効を再照合する方式。採取と使用の間の変化も管理し、外で採取しただけで停止を永続保証しない。同期通信が必要ならtimeout・取消・connection予算・失敗時rollbackを別途実証する。

現在の消費はSecurityContextの実USERが発行者と一致することを要求する。non-Web workerへuser IDを引数で渡すだけでは本人認証／委譲にならない。Web受付、worker認証、権限再確認、操作許可の拘束と失効を接続前にreviewする。観察modeの更新Bean不在、既存Session／CSRF／Security共存も当該modeを作る前に確認する。

## 8. 検証候補と受入条件（O-08）

以下は追加coverage案。既存55件のPASSに合算せず、class数・case数・実行資源／時間／接続・cleanup・raw／再実行上限は後段開始票で採用する。初回上限を後段の包括予算として流用しない。

| 検証面 | 必須候補の正例・負例 | 出口 |
|---|---|---|
| scope | 割当あり、能力／範囲別拒否、失効、正本障害、古いcache、別環境、存在情報非露出 | 確認不能は更新／取得拒否、対象漏えいなし |
| TTL／時計 | 正式値・上限、欠落／過大／不正、発行後policy変更、精度境界、時計逆行／許容差超過、証拠先行失効 | 期限延長なし、証拠期限も独立強制 |
| 現在対象 | 同eventの別publication、listener／attempt変更、状態不適格、取得後変化、送信直前失効 | 別対象送信0、消費済みなら記録保持 |
| 停止 | 生存・別世代・偽造／改変・期限切れ・再起動・task移譲・lock喪失 | 停止を確認できない追加送信0、保留根拠が辿れる |
| provider | 同key並行要求・payload差異・key期限切れ、受理後応答喪失、照会失敗、DB／Auditとの矛盾 | UNKNOWN保持。stub結果と実案件保証を分離 |
| 証拠／閉鎖 | 参照不能、対象／actor／operation差異、保存障害、未解決閉鎖、次許可競合、後日再参照 | 必要記録失敗で成功にしない。未解決対象の保護維持 |
| transaction／主体 | 遅延・timeout・pool枯渇、採取後失効、委譲偽装・失効、消費commit不明 | 長期lockを抑え、commit不明時送信0・消費保全 |
| 既存保護 | 通常無効の新旧DB、登録一覧、既存Security／master／expense、固定JAR E2E | 初回と同様に既存経路を保護。対象外登録／通信なし |

## 9. 推奨する判断順と次の提出物

1. **O-01／O-02：scope正本とTTL policyの採用。** 運用責任者・実在する供給元・値と失効条件を取得し、設定方式と必要変更を具体化する。
2. **O-03〜O-06：現在対象と停止／provider証拠の採用。** Reference実演環境の停止方式・stub契約・証拠保管案を決め、実案件への未達を残す。
3. **O-07：transaction・本人認証／委譲の設計。** 受付／観察／workerのmodeと、消費commit後・送信直前再確認を含めて接続前reviewへ提出する。
4. **O-08：限定実装開始票。** 採用対象だけのpath／code・SQL・property／依存差分、既存CP／Gateへの対応、検証集合・資源・作業量上限、停止・戻し方をまとめる。必要正本整合・文書source固定・preflightを確認し、個別開始判断後に実装する。

scope／TTLのAdapter単独で先行できるかは、選んだ正本がpublication・証拠・認証経路に依存するかで判断する。供給元未取得のまま架空のAPIや台帳を確定しない。sender／listener／runner／Web／CLI受付、publication schema／Modulith runtime、正式grant配備、Framework／remoteは本整理で開始しない。

**Owner確認事項：** O-01〜O-08ごとの採用／条件変更／保留と、OPEN入力の提供者・取得方法。現在は全項目未採用。まずscope／TTL正本の現実的な候補と、Reference実演環境での停止・provider証拠保管方式を選ぶための入力を揃える。

本書作成では文書のみ追加し、code／SQL／設定・環境を変更していない。Maven／Docker実行、git add／commit／pushは行っていない。

**O-01／O-02の具体化（2026-10-07）：** Ownerの指示により、[限定実演の管理設定によるscope／TTL供給案](phase4-s1-managed-scope-ttl-configuration-proposal-20261007.md)を作成した。起動固定snapshot、完全一致割当、停止・再起動での失効、TTL／設定期間の候補値と検証枝を提示する。具体化指示は方式・値の採用や実装開始の承認とは区別し、本書O-01／O-02の未採用状態を維持する。

**後続の採用判断（2026-10-07）：** Ownerは[供給案§8](phase4-s1-managed-scope-ttl-configuration-proposal-20261007.md#8-owner採用承認2026-10-07)で限定実演向けscope／TTL供給案を採用した。O-01／O-02は当該限定条件で採用済み、実担当・実配備・時計監視等のOPENと実装開始判断は残る。O-03〜O-08の未採用状態、停止／provider証拠・通知／復旧の別reviewを維持する。

**後続の限定開始承認（2026-10-07）：** [scope／TTL限定開始票§8](phase4-s1-managed-scope-ttl-limited-start-review-20261007.md#8-owner承認条件付き限定開始2026-10-07)で、設定供給・発行直前再検査と当該検証に限り、文書commit・clean source固定・preflight成立を条件とする作成／隔離検証をOwner承認済み。O-07／O-08のうち同票のscope／TTL再検査・検証条件だけを採用し、worker認証・外部証拠取得・送信前再確認およびO-03〜O-06は別判断を維持する。

**後続の結果承認・次の整理（2026-10-07）：** scope／TTL実装・検証は[検証記録§7](../architecture/validation/phase4-s1-managed-scope-ttl-20261007.md#7-実装検証結果のowner承認2026-10-07)でOwner承認済み、Ownerのcommit `6cf029e`で固定した。続いて[現在対象・停止／provider証拠の接続契約と限定範囲案](phase4-s1-target-operational-evidence-contract-review-draft-20261007.md)を作成した。O-03〜O-06と後段O-07／O-08の方式採用・実装開始は未成立で、本案はOwner review入力。

**後続のtest限定開始承認（2026-10-08）：** [対象・運用証拠ハーネス開始票§9](phase4-s1-operational-evidence-harness-limited-start-review-20261007.md#9-owner承認条件付き限定開始2026-10-08)で、接続契約案の段階Aと新規48件・回帰・検証上限をOwner承認済み。O-03〜O-08のtest所有modelによる契約照合に限り、文書commit・clean source固定・preflight成立後に作成／隔離検証できる。実運用Adapter・供給元、停止操作・provider通信、worker認証・送信、本番契約／schema変更の開始判断は未成立を維持する。
