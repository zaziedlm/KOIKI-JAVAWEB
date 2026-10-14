# S1 Reference基盤：正本改訂差分・残条件の審査案（2026-10-06）

**状態:** OWNER APPROVED / 正本改訂反映済み（2026-10-06、§9）。初回条件・実行資料・Gate初回限定判定・source固定／preflight条件付き正式作成検証開始を承認済み。文書commit・clean source固定／preflightは未実施。
**source baseline:** `feature/phase4-s1-reference-foundation` / `f9ea06a24a64015ee3604780995bfec862d988a5`。文書確認開始時のworktreeはclean。A／B／C受入の親baselineは`f5e2672`。今回の変更は審査資料と導線だけ。
**入力:** [D1〜D6採用記録§8](phase4-s1-reference-foundation-adoption-decisions-20261006.md#8-owner採用判断2026-10-06)、[正式開始票](phase4-s1-reference-foundation-formal-start-review-20261006.md)、[現行P4-F提案](KOIKI-JavaWeb-FW_Phase4_P4-F限定開始Gate提案_v0.1.md)、[既存F-5差分案](phase4-pl2-f5-integration-and-gate-delta-draft.md)。

## 1. 今回閉じる判断と変更の意味

推奨は、既存P4-F案の中で、**初回に承認する対象をST-CのReference保存・認可・Audit基盤だけに限定する**こと。初回ではLevel 2 runtimeを導入しない。保存基盤をLevel 2開始制限から独立した例外として扱わず、P4-Fの設置・限定判定と必要正本反映を先に成立させる。

作成時のP4-F提案§3は「P4-F前のblocking review」、PL2判定資料CP-F1はstore・Rules・複数instance復旧を一括した前置条件だった。D1の経路方針採用だけでは変更せず、本書の具体差分を対話で審査した。§9のOwner承認により、初回に実装しない部分のreviewを当該接続前へ対応付けて正本へ反映した。Gate・各対象の開始判断は別に維持する。

初回の完了は保存基盤と既存Reference保護のEvidence受入であり、CP-F1／CP-F2全体完了、S1完了、DoD 4-1〜4-5・4-12のPASSへ読み替えない。P4-AR6実チーム受入・AR-D10・Gate P4-AR、Phase 4全体開始、正式受渡し、remoteは別判断として残す。

## 2. 正本へ反映する具体文案

本節は審査時の文案として保持する。Ownerが限定条件での反映を承認し、§9に記録した正本へ反映済み。承認対象・日付・source・停止点へのリンクを併記し、過去の未承認記録を保持した。Docker対象追加は§2.2の時点条件に従い、環境実行承認時に反映する事項として残す。

### 2.1 P4-AR計画：§2・4.2・5・11・13

[P4-AR計画](KOIKI-JavaWeb-FW_Pre-Phase4_Adoption_Readiness計画_v0.1.md)§2末尾に次を追加する案。

> Architecture OwnerがGate P4-Fの設置・対象範囲の限定開始を明示承認し、必要正本反映・個別blocking review・検証上限が成立した場合は、承認対象commit pointに限りP4-AR6／AR-D10／Gate P4-ARより前に作業を開始できる。S1初回はReference-owned notificationの許可・消費保存、認可、Auditとその検証に限定し、Level 2 runtime・通知・復旧の運用経路は開始しない。P4-AR6／AR-D10／Gate P4-AR、Phase 4全体開始、正式受渡しとremoteは別判断のまま維持する。

§4.2のproduction実装除外行の後へ「Gate P4-Fによる別承認は§2の対象範囲だけに適用する。対象外の除外は維持する。」を追加する案。§5・11へそれぞれ次の行を追加する案。

| 反映先 | 追加行の内容 |
|---|---|
| §5 Work packages | Gate P4-F（条件付き）：F-1〜F-5、承認対象範囲・Owner・検証上限・Evidence・停止条件を審査。初回はReference保存基盤のみ。後段は個別開始判断 |
| §11 Evidence and commit points | Gate P4-F／CP-F0〜CP-F5（条件付き）：初回基盤の部分Evidenceと全体Evidenceを分離。未完了CP／DoDを完了扱いにしない。remote・配布は別承認 |

§13の現行P4-AR6→AR-D10→Gate P4-AR経路を残して、次を追記する案。

> 実チーム入力待ちの間も、F-1〜F-5が承認対象範囲について審査可能になればGate P4-Fを別途判定できる。初回基盤のみの限定判定では、後段の計画・未達・追加reviewと再審査点を明記し、S1全体の実行予算・開始を認定しない。

### 2.2 AGENTS.md：Phase 4未承認段落・優先事項13・Docker対象

[AGENTS.md](../../AGENTS.md)の「Phase 4開始は未承認である。」に続く追加文案。

> Phase 4全体開始は未承認のまま維持する。Gate P4-Fの設置・限定開始と必要正本改訂がArchitecture Ownerにより明示承認された場合だけ、承認記録のsource、Ownership、対象、検証上限と停止条件に従ってS1初回Reference基盤を作成・検証できる。初回はnotificationの許可・append-only消費の保存／認可／Auditと条件付き登録、Reference-owned追加migrationに限定する。通常起動では無効とし、既存identity／master／expenseとSecurityを保護する。Web／CLI受付、sender、listener、復旧runner、publication schema、Modulith runtime、Framework API／Rules／依存変更は含めない。後段は必要reviewと個別開始判断を経る。

優先事項13の末尾への追加文案。

> Gate P4-Fで個別承認された初回Reference基盤には上記限定条件を適用する。Spring Modulith Level 2そのもの、通知・復旧の接続、MyBatis等の対象外開始制限は維持する。

Docker節の対象追加は環境実行承認と同時に反映する案。

> S1初回Reference基盤の検証が別途承認された場合、その記録に指定されたkoiki-reference-appとbuild-support/reference-e2e-verificationの隔離DB検証・既存回帰に必要な使い捨てcontainerの起動・停止も対象に含む。実行環境の権限付き承認手順、資源・cleanup条件に従う。既存P4-PL2許可から対象拡大を推定しない。

### 2.3 Phase 4見直し草案：§4.3

[見直し草案§4.3](KOIKI-JavaWeb-FW_Phase4実施計画_見直し草案_v0.1.md#43-p4-ar6を待たないframework限定開始gate案)の開始対象行を次へ具体化する案。

> P4-FのS1候補はA1／A2／D1非同期。最初の承認対象はST-CのReference保存・認可・Audit基盤のみ。A1／D1全体の設計は計画資料として追跡し、publication・復旧・通知・観測の実装開始は当該review後の別判断とする。

同表の開始前Evidence行へ「初回の責務・登録・DDL／grant・既存Reference回帰・作成実行上限を採用し、後段前置条件を明記する」を追加する案。DoD全体完了行は維持する。

### 2.4 P4-F提案：§2・3・4・5

[P4-F提案](KOIKI-JavaWeb-FW_Phase4_P4-F限定開始Gate提案_v0.1.md)§2末尾に、§1の初回範囲と除外、§3のreview区分を追記する案。§3表の一括前置表記は次へ変更する案。

| review | 初回基盤開始前に必要 | 後段の該当接続前に必要 |
|---|---|---|
| Module／モデル | notification RICH／JPA SHARED、Domain／Application／Adapter責務、公開境界、既存Rules適合 | expense承認event、payload、同期vetoと非同期side effect、Rule 28／29・Level選択 |
| 保存／migration | 許可・消費2 table、JPA transaction／Audit、列権限、V4配置と既存履歴整合を実証する計画 | publication store・completion方式・schema／履歴所有・依存選択・パージ |
| 認可／安全 | 現在Identity能力、scope／TTL契約と未接続拒否、Audit、既定無効・Entity／Bean登録抑制 | 本人認証の運用受付、scope供給元・TTL値、停止／drain真正性、fencing、provider受理不明 |
| 観測／運用 | 安全なlog・DB／Audit突合、実行資源・cleanup・未知結果保全 | 実trace／sink、FAILED滞留・alert、相関、retention、単一実行パージ |

§4 F-2へ初回の配置・除外と後段reviewを対応付ける。F-3には基盤55件／既存25 class／E2E保護と、既存DoD実演計画の未達を並べる。**初回testを通知・復旧実演の代替にしない。**

F-4の判定条件へ次を追加する案。

> 初回基盤だけを承認対象とする場合、初回の概算・上限・Owner・環境を確定し、後段の見積未確定と再審査時点を明記する。初回24〜40標準時間案をS1全体の予算としない。初回の上限が未採用なら限定開始を判定しない。後段の作成・実演開始前には当該範囲の概算または上限を再審査する。

§5の判定方式APPROVE LIMITED START／REWORK／REJECTを維持し、承認範囲・残るCP／DoD・後段禁止・期限または上限を記録する。Gate設置だけで実行開始せず、初回preflightの成立を実行条件とする。

### 2.5 PL2判定資料・F-5台帳・正本Skill

[PL2判定資料§1.3・3.3・4](KOIKI-JavaWeb-FW_Phase4_PL2_P4-F判定資料_v0.1.md)への差分案：初回保存基盤はnotification RICH／JPA SHARED候補、通知全体のTierは後段追加時に再照合する。旧Tier 1案をそのまま移植しない。許可・消費のV4／kkref履歴と、未採用のpublication migration案を分離する。

CP-F0はD1／D2の経路方針採用と、正式対象・Gate判断を分けて記録する。CP-F1の初回対象部分は本書§2.4、残るstore／Rules／複数instance安全条件は当該接続前へ対応付ける。CP-F2では基盤の部分Evidenceだけを受入れ、パージ／復旧／相関を含む全体完了にはしない。CP-F3〜5の出口・停止点は維持する。新しいCP番号は作らない。

[F-5台帳§2](phase4-pl2-f5-integration-and-gate-delta-draft.md)は本書の具体文案・初回限定条件へ追跡を追加する。全件台帳の対象外／当初DoD／Customer待ちを維持する。

[project-overview正本Skill](../agent/skills/koiki-project-overview/SKILL.md)のPhase 4未承認段落へ、承認後の限定開始記録・正式開始票へのリンクと「全体開始・Level 2 runtime開始は別判断」を追記する案。[business-feature-work正本Skill](../agent/skills/koiki-business-feature-work/SKILL.md)には該当正式開始票への導線だけを追加する。規則を複製せず、`.agents/skills/`・`.claude/skills/`を変更しない。

## 3. 残る技術条件の具体審査案

| 条件 | 今回提示する具体案 | 受入・未成立時 |
|---|---|---|
| Tier | notificationをRICH／JPA SHAREDとする。許可の可否・閉鎖規則をDomain、消費との一度性をApplication transaction＋DB制約へ配置 | Rule 16等を含む現行Architecture test適合。既存Rule除外・Framework変更で通さない |
| 登録 | 条件付きConfigurationだけを登録入口とし、EntityManager-backed Adapterを明示Bean化。Repository契約はCommons Repository＋NoRepositoryBean。通常未設定／falseで追加Bean・Entityなし | 既存master／expense／Identity EntityScanを保持。有効時の不足Port／TTL policyは拒否を返す構成とし、test有限Adapterをproductionへ含めない |
| property不正 | 値はtrue／falseだけ。その他は設定不正として診断・起動拒否する案 | 未設定の正常既定falseと区別。不正値を暗黙の有効化にしない。検証のために通常false構成を変えない |
| TTL精度（採用済み） | ApplicationのissuedAt／expiresAtをmicrosecond精度へ切り捨て、調整後にexpiresAt>issuedAtを必須とする | 期限が発行時刻以下になれば発行拒否。保存・再読取後も期限一致で消費拒否。精度調整による延長を許容しない。正式Duration・最大TTLは後段 |
| migration | D6のV4別location／同じkkref履歴を第一案とし、無効＋V4適用済みでも通常validation成立を必須にする | 5 caseにhistory・既存row比較を含める。不成立なら配置案を再審査。validation無効化やhistory削除を許容しない |

これらは追加の採用案であり、D1〜D6承認へ遡及包含しない。mapping／登録成立は正式実装後のEvidenceで確認する。

### 3.1 用途別grant実行表の案

**対話採用済み:** 発行・閉鎖／消費／観察／管理の操作分離、許可INSERT・閉鎖列／version UPDATE・消費INSERT・用途別SELECT、Public IdentityQueryに必要な参照とPublic Recorderに必要なINSERTの内容で具体grant表を確定する。role物理名は下記の命名候補として扱う。正式開始条件成立前のSQL生成・実行は開始しない。禁止操作の実効拒否・既存権限経由の拡大なしは実DBで確認する。

role物理名候補は`kkref_notification_owner`（NOLOGIN）、`kkref_notification_permit`、`kkref_notification_consumer`、`kkref_notification_reader`。runtimeはNOINHERIT、owner membershipなし。通常Referenceの既存credentialを置換しない。初回は隔離DB・用途ごとの別接続構成で検証し、正式運用のcredential配備は運用入口接続前の判断とする。以下はSQL生成・実行前に審査する操作一覧。

| 対象 | permit（発行・閉鎖） | consumer（消費） | reader（観察） |
|---|---|---|---|
| permit SELECT | 必要 | 必要（scope先行・lock） | 必要（scope先行） |
| permit INSERT | permit_id／environment_id／publication_id／event_id／listener_id／expected_attempt／actor_id／reason_code／issued_at／expires_atのみ | なし | なし |
| permit UPDATE | closed_at／confirmed_by／result_ref／versionのみ | versionのみ | なし |
| consumption SELECT | 必要 | 必要 | 必要 |
| consumption INSERT | なし | permit_id／operation_id／worker_generation／consumed_atのみ | なし |
| 両table DELETE／TRUNCATE、消費UPDATE | なし | なし | なし |
| schema CREATE・table ownership・migration history更新 | なし | なし | なし |

grantは2 tableを名前で限定し、ALL TABLES／将来tableへのdefault privilegesを使わない。実際のschema USAGE、PUBLIC／既存role経由の有効権限も照合し、禁止操作が本当に失敗することを実DBで確認する。owner／DDL適用接続は隔離setupだけに用いる。

Framework-owned必要権限は別表：Public IdentityQueryの現行参照先`koiki_user`／`koiki_role`／`koiki_permission`／`koiki_user_role`／`koiki_role_permission`に3 runtime用途のSELECT、Public Recorderの`koiki_audit_event`にpermit／consumerのINSERTとreaderの拒否記録用INSERTを候補とする。readerは観察でもSecurity拒否記録が必要なのでDB完全read-onlyとはしない。Audit突合のSELECTは検証管理接続で行い、runtimeへ一括付与しない。password／login-attempt／Session・publicationへの権限は追加しない。既存Framework SQLは改変せず、Public契約の実接続と必要SQLを再照合してreviewする。B fixtureのrole／schema／credential／SQLをコピーしない。

## 4. F-1〜F-5提出状態と実行上限の不足

| 提出物 | 今回の具体化 | まだ残る成立条件 |
|---|---|---|
| F-1 | clean `f9ea06a`を入力sourceとして確認。A／B／CはTooling受入、Phase 3 accepted／P4-ARは既存Evidenceとして分離 | 本書・採用後正本改訂をcommitし、実行sourceを固定。既存accepted baselineとの差分範囲とartifactをpreflightで照合 |
| F-2 | 初回配置・除外、Tier／登録／grant案と後段reviewを§2・3へ対応付け | §3の採否とblocking review記録。実装前に55件のmethod対応を展開 |
| F-3 | 初回55件・既存25 class／package済み既存Journeyを保護検証。後段DoD実演はPL2判定資料§2.1を保持 | 前置／後続区分のOwner採用。OS crash・provider・復旧・実traceの未達を保持 |
| F-4 | 初回24〜40標準時間・12時間相当の確認・40時間相当の停止、対話で提示した資源数値・検証時間上限を採用済み（§6） | 具体実行方式と既存harnessの制限適合を確認。Owner review稼働／環境待ち・Gate整合は別枠で未算定。S1全体見積は未確定 |
| F-5 | §2に対象正本・挿入箇所・具体文案・維持義務を提示 | 差分承認・実反映、Gate設置／対象限定判定と正式開始票の整合 |

source調査で、既存[Reference DB fixture](../../koiki-reference-app/src/test/java/org/koikifw/reference/ReferencePostgreSqlTestConfiguration.java)はPostgreSQL 17を生成するだけで、提案のDB1 GiB／CPU1／max_connections16を設定していない。[既存E2E](../../build-support/reference-e2e-verification/src/test/java/org/koikifw/buildsupport/referencee2e/PackagedReferenceCriticalJourneyTest.java)もDB資源制限がなく、別JAR起動は`java -jar`でheap上限を指定していない。MavenのargLineだけではこの別processを制限できない。memory4 GiB以上という開始条件だけで全processの上限成立とは認定できない。

推奨は、**新規6 class用の制限と既存25 class／E2Eの保護実行条件を分けて採用する**こと。既存harnessを変更せず制限適合できるか、既存pool／timeout・JVM起動設定をread-onlyで詰める。成立しなければ検証用変更の対象・回帰・新上限を具体差分にして審査へ戻す。現時点ではJ7を充足済みにしない。今回Maven／Docker／artifact install・取得は実行していない。

preflightは承認された実行範囲で、JDK／Maven、reactor／実効依存・source整合、Docker／既存image・Chromium、disk／memory、資源設定、test class／件数とraw保管・cleanupを確認する。artifact不足なら座標と対処差分を提示し、Toolingの過去のinstall承認をReferenceへ流用しない。

## 5. 審査・反映・開始の順序

1. §2の正本改訂案・前置／後続区分と§3の技術案を審査し、採用／変更／保留を記録する。
2. J7の資源不整合、55件method対応、Owner稼働／期限等を具体化し、正式開始票の検証・上限と実行対象を審査可能にする。
3. F-1〜F-5を承認対象範囲へ対応付け、Gate設置・限定判定、正本反映、正式開始票の作成・検証承認を整合させる。正本反映後に文書commitを固定する。
4. 承認済みpreflightを実施し、source／環境成立をEvidenceへ記録する。成立した初回対象だけ実装・検証する。
5. 既存回帰・無効構成保護を含む初回結果を受入審査へ渡す。通知・復旧・Level 2・観測の接続は後段条件を閉じて別開始判断へ戻す。

| Owner判定単位 | 現在 |
|---|---|
| 正本改訂文案・初回／後段review区分（§2） | 区分・具体正本文案はOwner承認済み、§9の正本へ反映済み。Docker対象追加は環境実行承認時。Gate判定は未成立 |
| Tier・登録・TTL精度・具体grant案（§3） | 配置・責務・RICH／JPA SHARED、設定別動作、TTL精度調整、専用Configurationによる明示登録方式、提示操作範囲の具体grant表確定は採用済み（§6）。実装適合・権限実効の照合／検証は残る |
| 検証・資源／工数・実行対象（§4、正式開始票§4・5） | 受入三本柱・逐次実行／停止方針、提示した資源数値・検証時間上限・初回24〜40標準時間の管理条件、新規6 class／55件計画を採用済み（§6）。method対応確認・具体command／残る実行方式・環境実行開始は残る |
| Gate設置／限定判定・正本反映・正式実行開始 | 正本反映は§9の範囲で完了。Gate設置／限定判定・正式実行開始は未成立 |

本書の準備指示を上記採用・Gate・実行承認に読み替えない。既存Reference回帰失敗、無効時の追加登録・DDL・通信、既存rowの意図しない更新、未承認Framework／依存／権限拡大、UNKNOWN保全不能、上限到達は正式開始票の停止条件を適用する。

## 6. 対話による確認記録（2026-10-06）

Ownerから「案内容を確認しながら対話的に進めたい」との指示を受け、項目ごとに確認する。以下の合意を本書全体の一括承認へ拡張しない。

| 確認項目 | 提示内容 | Owner確認 |
|---|---|---|
| 初回範囲・順序 | 許可・一度性の消費記録、主体／対象／期限の確認、保存とAuditの整合を先に作る。通常起動では追加機能を無効とし、testから基盤を検証する。通知送信・実際の復旧・Web／CLI入口は後段 | 「はい、合っています」。初回範囲・順序の意向一致を確認 |
| 通常起動・既存Reference保護 | 通常は追加機能を無効とし、追加Bean／JPA Entity登録・追加migrationを通常起動から外す。専用設定で基盤を検証し、追加table未適用／適用済みの両DBで無効時の既存機能を確認する | 「『通常は無効、専用設定で検証し、新旧DBで既存機能を確認する』方針でよいです」。保護・検証方針を採用 |
| 配置・責務・Tier／モデル方式 | 単一koiki-reference-app内のReference-owned notification。Domainが許可の期限・対象一致・閉鎖規則、Applicationが認可・処理順序・transaction・Audit、AdapterがDB保存・Public IdentityQuery接続を担当。許可・消費はRICH／JPA SHAREDとする | 「この配置・責務分担と、RICH／JPA共有モデルの採用でよいです」。提示した配置・責務分担・Tier／モデル方式を採用 |
| 用途別最小DB権限 | 発行・閉鎖／消費／観察の用途別に必要操作だけを許可し、消費の変更・削除、許可の対象・期限書換えを禁止する。DDL管理接続を分離。観察も認可拒否のSecurity Audit記録用INSERTを必要とする | 「この用途別に必要最小限のDB権限を与える分け方でよいです」。用途分離・最小権限と提示したAudit記録の方針を採用 |
| migration適用の第一案 | 既存V1〜V3は変更せず、許可・消費2 tableのV4を別locationから管理設定で明示適用し、既存kkref履歴で管理する。適用後の通常設定で履歴validation・既存Reference起動が成立することを必須検証とし、不成立なら配置案を見直す。validationを無効化せず、戻す際は機能無効化・追加記録保持とする | 「適用方法を、成立確認を条件とする第一案として進めてよいです」の趣旨を確認。提示案を成立確認付き第一案として採用 |
| 有効化と操作許可 | 未設定／falseは追加機能を登録せず通常起動。trueは基盤を登録し、scope／TTL／対象証拠などの不足する操作を拒否。true／false以外は設定不正として起動拒否。testの有限入力による正常操作と未接続時の拒否を両方検証する | 「この『有効化と操作許可を分け、不足条件は拒否する』動作でよいです」。提示した設定別動作・条件不足拒否を採用 |
| 初回／後段の審査時点 | 初回基盤前にモデル・認可・Audit・DB権限、通常無効構成・migration方針、既存回帰・検証上限の必要審査を閉じる。通知／provider、復旧停止・競合、Level 2依存／Rules・非同期観測は各接続前の審査を必須として残す。基盤受入だけで後段を開始しない | 「この『初回に必要な審査を閉じ、後段は接続前の審査を必須として残す』区分でよいです」。審査時点の区分を採用 |
| 初回受入の三つの柱 | 基盤の保存・認可拒否・期限・一度性・競合・Audit rollbackを実DBで確認。既存25 test class／package済み主要操作E2Eを回帰確認。通常無効構成を新旧DB起動と不要Bean／Entity／送信処理の登録不在で確認。必要確認が未実施・失敗なら受入れない | 「この三つを受入の柱とし、必要な確認が未実施・失敗なら受入れない方針でよいです」。受入方針を採用。新規6 class／55件・command・上限・環境実行の採否は別 |
| 検証実行・停止 | 新規test／既存回帰／E2Eは逐次実行。新規testのDB／memory／connection／時間上限を設け、既存回帰／E2Eは現行条件を調査して別上限を提示。上限到達・環境不整合で停止し、原因と必要差分を確認する。上限に合わせた既存testの弱化や失敗のままの反復はしない | 「この実行・停止方針でよいです」。提示した逐次実行・上限調査・停止方針を採用。数値・具体実行方式は別判断 |
| 検証専用資源設定の具体化 | 今回の検証で明示選択した時だけ制限を適用する検証専用設定を追加する方向で、対象file・数値・実行方式を具体化する。既存testの確認内容を保持する | 「この検証専用設定の追加を、具体化する方向でよいです」。具体案の準備を了承。コード変更・具体数値・実行開始の承認は別 |
| 検証専用設定の対象範囲 | 既存Reference DB fixtureとE2Eに、明示選択時だけDB／pool／JVMの制限を適用する設定を追加し、通常の検証設定を維持する | 「この既存fixture・E2Eへ明示選択式の制限を追加する対象範囲でよいです」。提示した対象範囲・明示選択方式を採用。具体設定名・数値・fork寿命・コード変更／実行開始は引き続き審査 |
| 初回検証の資源数値・停止 | 開始時available memory8 GiB／disk10 GiB以上、DB同時1／memory1 GiB／CPU1／max_connections16、Maven・test・E2E子Reference各heap768 MiB、既存回帰／E2Eのpool最大4。資源不足・制限下不成立なら停止し再判断する | 「この数値を初回検証の条件として採用し、資源不足や制限下での不成立があれば停止・再判断する扱いでよいです」。提示した数値・停止条件を採用。minimum idle・新規接続総予算・fork寿命・時間等の未提示数値や実行開始へ拡張しない |
| 初回検証の時間上限 | 新規DB testは各class10分、既存25 class＋E2Eはbaseline／実装後それぞれ合計60分。上限到達時は追加実行を止め、進行中処理を安全に終了・cleanupし、未実施分・原因・実測時間を示して再判断する | 「この時間上限を初回条件として採用する扱いでよいです」。提示した時間上限・終了／再判断条件を採用。作業工数上限・実行開始は別 |
| 初回作業量・見直し | 設計・実装・検証・Evidence作成は24〜40標準時間の低確度概算。12時間相当で通常無効構成・登録／migration成立状況を確認し、40時間相当で未完なら停止して残作業と見積を再提示。Owner review／環境待ちは別枠 | 「この概算と見直し条件を初回の管理条件として採用する案でよいです」。概算・確認／停止／再見積条件を採用。標準作業量であり、経過時間・AI実行時間や完了期限の約束ではない |
| 新規test作成・検証計画 | 新規6 class／55 invocation：Domain15、transaction・認可・Audit16、永続化・競合・DB権限12、登録・有効化7、migration5。既存25 class＋E2E回帰を追加。実装前に確認項目をtestへ対応付け、必要な追加・変更は差分を提示する | 「この6クラス・55件を初回の作成・検証計画として採用する扱いでよいです」。予定範囲・件数と対応照合／変更時差分提示を採用。正式開始条件・具体実行方式・環境実行開始は別 |
| 既存回帰のclass間終了方式 | 並列化せず、classごとにtest JVM・DBを終了し、DB停止確認後に次へ進む。残存DBがあれば次を起動せず停止。baseline／実装後を同条件で比較する | 「クラスごとにJVM・DBを終了する方式でよいです」の趣旨を確認。提示したclass単位終了・逐次実行・停止確認方式を採用。具体command／cleanup実装と実行開始は別 |
| 新規DB testの操作時間上限 | 新規DB testだけロック待ち・SQL実行・transactionを各10秒上限とする。超過した操作は失敗として扱い、rollback・記録保全を確認する。既存回帰／E2Eの待ち時間は維持 | 「この10秒の上限を、新規DBテストの条件として採用してよいです」。対象・数値・超過時の扱いを採用。具体適用方式と実行開始は別 |
| DB接続数の管理 | 既存回帰／E2Eはpool最大4に加えminimum idle1。新規testは発行・消費・観察の各用途poolと管理接続を合算し、同時connection計8以内に制限する | 「この接続数の管理条件でよいです」。minimum idleと新規同時接続総予算を採用。用途ごとの配分・実効値は具体構成と検証で確認 |
| 検証記録量・再実行 | 生の検証記録は合計1 GiB以内。同じ原因・条件での再実行は最大1回とし、再実行前に原因を確認して継続理由を記録する。上限到達時は必要証拠を消して続行せず、保持して停止・再判断する | 「この記録量・再実行の管理条件でよいです」。提示した記録量・再実行上限・確認／保全／停止条件を採用 |
| 許可期限の保存精度 | 発行時刻・期限をDBのmicrosecond精度へ切り捨てる。調整後の期限が発行時刻以下なら発行拒否。保存・再読取後も期限ちょうどで消費拒否を確認する | 「この精度調整を初回の実装条件として採用してよいです」。提示した精度調整・不正期間拒否・再読取後の境界検証を採用。正式TTL値の採用ではない |
| 専用Configurationによる明示登録 | 新しい処理・DB Adapterを自動登録せず専用Configurationから明示登録し、許可・消費のJPA Entityも有効時だけ登録する。既存Identity／master／expenseの登録設定を維持。未設定／falseで追加Bean／Entityが不在であることを検証する | 「この専用Configurationによる明示登録方式でよいです」。提示した方式・既存登録維持・無効時不在の検証条件を採用 |
| 具体grant表の確定 | 発行・閉鎖は許可作成と閉鎖情報／version更新、消費は消費追加と許可version更新、観察は許可／消費参照と拒否Audit記録、管理はDDLを別接続で担当。IdentityはPublic Queryに必要な参照、Auditは必要なINSERTだけを与える | 「この内容で具体grant表を確定する扱いでよいです」。提示した操作・必要参照／記録の範囲で§3.1を確定。SQL実行・既存権限拡大の承認ではなく、実効権限は検証する |

初回範囲・順序、通常無効・専用検証・新旧DB確認、配置・責務・RICH／JPA SHARED、用途別最小DB権限・提示操作範囲の具体grant表、migrationの成立確認付き第一案、専用Configuration明示登録・TTL精度調整と対話で示した検証計画／上限は確認・採用済み。role物理名は命名候補として扱い、既存権限拡大を許容しない。履歴validation・登録・実効権限の成立は実装／検証で実証する。正本改訂文案・実反映、具体実行手順、Gate・実行開始は引き続き対話・審査で確認する。

## 7. 検証上限のsource調査（対話確認後・2026-10-06）

Maven／Dockerを実行せず、Reference test設定、Parent／E2E POMとE2E sourceを照合した結果。記載の数値は既存sourceまたは未採用案であり、測定値ではない。

| 対象 | sourceで確認できたこと | 上限設計への影響 |
|---|---|---|
| Maven／test JVM | ParentはSurefire versionを管理するがfork／heap上限の設定なし。Reference／E2E POMにも個別設定なし | command案のfork1／heap768 MiBはtest JVMだけの上限。Maven JVMのheapとnative memoryは別に扱う |
| 既存実DB test | 7 classがSpringBootTest＋ReferencePostgreSqlTestConfigurationを使用。fixtureはDBのmemory／CPU／max_connections指定なし。検索範囲のtestにDirtiesContext／明示pool上限指定なし | classを順次実行してもcontext保持によるDB併存を排除した証拠にならない。DB1を保証するにはcontext／forkの寿命も調整・検証する |
| E2E | test JVM内のIssuerFixture（HttpServer）＋別Reference JAR JVM＋Playwright／Chromium＋PostgreSQL。issuer専用の別JVMは起動していない | 正式開始票のissuer process記載を訂正。別Reference JVMにheap指定なし。ブラウザ等も含めた全体memoryは-Xmxだけでは制限できない |
| E2E時間・cleanup | 起動待ち30秒、HTTP応答timeout5秒、process停止待ち5秒＋強制停止後5秒。finallyでprocess／issuer／DBを停止し一時logを削除 | 全体60分案やclass10分案とは別の既存待ち。既存assertion／timeout／cleanupを保持し、外側の制限がcleanupを阻害しない方式を審査する |

既存回帰は、class間でtest JVMを再利用せず並列化しない方式をOwner採用済み。fork1／reuseForks=falseをcommand候補とし、DB停止確認後に次へ進む実行・cleanup手順まで具体化する。これらのSurefire引数だけでDB停止確認が成立したとは扱わない。既存testのassertion・認可・操作を維持し、baseline／変更後に同じ方式で比較する。DBのmemory／CPUやE2E別JVMの制限は別途明示する。

DB資源指定は既存fixture／E2Eの検証専用設定に変更が必要。推奨は、検証時に明示選択した場合だけ上限を適用する設定を設け、通常の既存検証条件を維持する案。production設定、Framework API／dependencies、既存testの期待結果は変更対象にしない。この検証用変更の範囲・方式・数値を審査してから作成する。

## 8. 検証専用設定の具体案

§6の了承を受けて準備した案。既存fixture・E2Eへ明示選択式の制限を追加する対象範囲と、§6に記録した資源数値・時間・接続・停止条件はOwner採用済み。まだcode／POMを変更せず、Maven／Dockerも実行しない。具体設定名・command／cleanup手順・用途別接続配分は審査用具体化が残る。

### 8.1 対象fileと有効化

test JVMのsystem property `koiki.reference.verification.resource-limits.enabled=true`を明示した場合だけ検証用制限を適用する案。未設定／falseでは従来動作、その他の値は検証設定エラーとして実行を拒否する。notificationのproduction有効化propertyとは別用途であり、これを指定してもnotificationは有効にしない。

| 対象 | 具体変更案 |
|---|---|
| [ReferencePostgreSqlTestConfiguration.java](../../koiki-reference-app/src/test/java/org/koikifw/reference/ReferencePostgreSqlTestConfiguration.java) | 有効時だけTestcontainersのDBへmemory／CPU制限とmax_connectionsを設定。test JVMのpool設定も照合し、不足・矛盾があればDB起動前に拒否する。既存service connection・migration・test操作は維持 |
| [PackagedReferenceCriticalJourneyTest.java](../../build-support/reference-e2e-verification/src/test/java/org/koikifw/buildsupport/referencee2e/PackagedReferenceCriticalJourneyTest.java) | 同じ有効化条件でDB制限を適用し、子Reference JARの起動引数に-Xmx768m、子process環境へpool上限4／minimum idle1を指定。Bearer／Session／browser／DB／Audit／log検証と既存cleanupを維持 |
| [E2E README](../../build-support/reference-e2e-verification/README.md)・正式開始票§5 | 明示有効化command、baseline／変更後の同条件比較、上限・停止・cleanup・証拠を追記。既存local／CI commandは維持 |
| 新規notification test fixture（正式開始後に作成） | 同じ検証用制限を適用。複数用途poolと管理接続を含む同時connection予算を計8以内に設計する |

Parent／BOM／Framework／production properties／workflow／CIを変更しない。test-only設定を共通Framework APIやkoiki-testingへ昇格させない。二つの検証artifact間で共通helper依存を新設せず、必要な設定をそれぞれの既存fixtureに閉じる。

### 8.2 数値と適用範囲

**対話採用済み:** available memory8 GiB／disk10 GiB以上、DB同時1／memory1 GiB／CPU1／max_connections16、各JVM heap768 MiB、既存回帰／E2Eのpool最大4／minimum idle1、新規同時connection総計8以内と不足・不成立時の停止。時間上限は新規DB class各10分、既存25 class＋E2Eがbaseline／実装後それぞれ合計60分、新規DB操作は各10秒。既存回帰は並列化せずclass単位終了・DB停止確認を行う。fork1／reuseForks=falseは具体command候補、raw／rerunは残る候補として区別する。

| 項目 | 初回候補 | 適用・確認 |
|---|---|---|
| Maven | 同時1、heap768 MiB | commandの実行区間だけMAVEN_OPTSを設定し、既存値を確認・保存してfinallyで復元。必要な既存JVM引数を消さず、競合するheap指定は整理して提示 |
| test JVM | 並列無効・既存回帰のclass単位終了は採用済み。command候補はfork1／reuseForks=false、heap768 MiBは採用済み | Reference／E2Eとも明示。既存25 classはclass終了時のcontext／DB停止を確認し、残存時は次を起動しない。引数だけでcleanup成立を認定しない |
| E2E子Reference JVM | heap768 MiB | ProcessBuilderのjavaと-jarの間へ明示引数を追加。Maven argLineから伝播すると仮定しない |
| DB | 同時1、memory1 GiB、CPU1、max_connections16 | PostgreSQL containerへ明示適用。各class／段階終了時に残存DBを確認し、残存したら次を起動しない |
| pool／接続 | 既存回帰・E2Eはpool最大4／minimum idle1、新規testは全用途＋管理接続の同時合計8以内（採用済み） | Reference testは明示system properties、E2E子JARは環境指定。既存fixtureは制限有効時に指定値を確認。新規用途別poolは合計予算内で設計し、用途別配分・実効値を検証 |
| DB待ち | 新規testだけlock／statement／transaction各10秒（採用済み） | 超過した操作は失敗としてrollback・記録保全を確認。既存回帰／E2EのDB待ちは維持。新規は個別DB設定・transaction境界で適用方式と実効上限を確認 |
| 実行時間 | 新規DB class各10分、既存25 class＋E2Eは1比較時点につき合計60分（採用済み） | baselineと変更後を別々に記録。上限到達時は追加実行を止め、進行中操作を安全に終了してcleanup。未実施分・原因・実測時間を示して再判断。外側からMavenを無条件killする方式は採用しない |
| 開始時資源 | available memory8 GiB、disk10 GiB以上（採用済み） | 従来memory4 GiB案を8 GiBへ改訂。JVM3つのheapだけで最大2.25 GiB、DB1 GiBに加えnative／browser／Docker等の余裕を見込む条件。実測済み必要量ではない |
| 証拠・反復 | raw合計1 GiB、同じ原因・条件でのrerun最大1回（採用済み） | 再実行前に原因と実行条件を確認して継続理由を記録。上限到達時は証拠を保持して停止・再判断。必要証拠を消して続行せず、既存失敗を無条件反復しない |

heap上限はprocess総memory上限ではなく、CPU1はDBだけの制限。Playwright driver／Chromium／Ryukを含む全processのhard cap成立は主張しない。利用量は実行前・段階終了時と実行中に観測し、開始条件不足・OOM／DB残存・制限不適合を検出したら停止して原因と必要差分を提示する。

既存testがpool4等では成立しない場合、assertionやtimeoutを緩和して通さず、資源条件へ戻す。fork分離で実行時間が増える可能性も上限再判断の入力とする。

### 8.3 commandへ追加する引数案と受入

Reference回帰は正式開始票の25 class集合を維持し、fork1／argLineに加えて以下を明示する案。新規6 classはこの集合に混ぜず別実行する。

```powershell
"-Dkoiki.reference.verification.resource-limits.enabled=true"
"-DreuseForks=false"
"-Djunit.jupiter.execution.parallel.enabled=false"
"-Dspring.datasource.hikari.maximum-pool-size=4"
"-Dspring.datasource.hikari.minimum-idle=1"
```

E2Eも有効化・fork1／heap・並列無効を明示する。poolと子JVMは§8.1のtest sourceから明示設定するため、親test JVMのSpring propertyだけで子JARの条件を指定したことにしない。MAVEN_OPTSの一時設定・復元を含む正確なcommandはpreflight用手順へ展開する。

受入では、未設定／falseで従来設定が維持されること、有効時のcontainer実効memory／CPU／max_connectionsとpool値、子JVM起動引数、class／phase間のDB残存なし、既存25 class／E2Eの確認内容と結果、環境変数復元を確認する。baselineと実装後は同じ設定で比較する。新規設定の検証だけを既存回帰成功の代替にしない。

**次の判定:** 採用済みの対象範囲・資源数値・検証時間上限・初回作業量・6 class／55件計画・既存回帰のclass単位終了・新規DB操作各10秒・接続数管理条件を前提に、method対応・設定名・用途別接続配分・具体実行／cleanup手順、検証用変更と実行対象を審査する。24〜40標準時間・12時間相当確認／40時間相当停止は採用済みだが、検証時間上限とは分けて追跡する。

## 9. 正本改訂のOwner承認・反映記録（2026-10-06）

Ownerは§2の正本改訂について「この限定条件で、§2の正本改訂文案を反映してよいです」と明示した。初回保存・認可・Audit基盤の限定条件、後段の個別審査、Phase 4全体開始・正式受渡し・remoteの別判断を維持する内容で承認・反映する。

入力sourceは`feature/phase4-s1-reference-foundation`／`f9ea06a`と、§6の対話確認を記録した未commit文書差分。今回の反映差分を含めた実行source固定は今後のcommit・clean確認で行う。

| 対象正本 | 反映内容 |
|---|---|
| P4-AR計画§2・4.2・5・11・13 | 条件付き限定経路、初回範囲、部分Evidence、実チーム入力待ちの分岐と維持する義務 |
| AGENTS.md | 初回限定条件・対象外・後段停止点と承認／開始票導線。優先事項13に限定条件の適用を追記 |
| Phase 4見直し草案§4.3 | 初回範囲・前置Evidenceを具体化、後段の個別開始・全体DoD維持 |
| P4-F提案§2〜5 | 初回／後段review区分、F-2／3／4の初回への対応、preflight条件と未成立のGate状態 |
| PL2判定資料§1.3・3.3・4 | RICH／JPA SHARED採用と旧Tier 1候補の区別、V4とpublication保存の分離、CP-F0〜2の部分／全体区分 |
| F-5台帳§2 | 旧文案履歴から今回の具体改訂・承認記録への追跡。全件台帳の対象外・当初DoD・Customer待ちは維持 |
| project-overview／business-feature-work正本Skill | 限定条件・正式開始票・承認記録への導線。規則を複製せず薄いadapterは変更しない |

**反映時点を残す事項:** AGENTSのDocker対象追加は§2.2に従い、Reference検証の環境実行承認と同時に反映する。現時点のDocker許可対象はP4-PL2のまま。改訂承認を環境実行の承認へ読み替えない。

**まだ成立していない事項:** Gate P4-Fの設置・初回限定判定、正式作成／実行開始。method対応・具体command／cleanup手順と検証専用設定の実行資料、文書commit固定・承認済みpreflightを揃える。後段の通知・復旧・Level 2・DoD実演は当該接続前条件と別開始判断を維持する。

**後続資料:** [残判断・初回実行資料](phase4-s1-reference-foundation-execution-review-20261006.md)に55件のmethod対応、設定名／用途別接続配分、classごとのMaven実行・停止確認・証拠保全とF-1〜F-5を展開した。残判断を実行資料採用／Gate設置・初回限定判定／source固定・preflight条件付き正式作成検証開始へまとめる。作成・実行は未開始。

**実行資料採用:** 後続資料§6で初回実行手順をOwner採用・確定済み。残る判断はGate設置・初回限定判定と、source固定・preflight条件付き正式作成検証開始。

**後続Gate承認:** [初回実行資料§7](phase4-s1-reference-foundation-execution-review-20261006.md#7-gate-p4-f設置初回限定判定2026-10-06)でGate P4-F設置・初回限定APPROVE LIMITED STARTをOwner承認済み。上記§9までの未成立記載は当時の履歴。現在残る判断はsource固定・preflight条件付き正式作成検証開始。

**最終開始承認・Docker反映:** [初回実行資料§8](phase4-s1-reference-foundation-execution-review-20261006.md#8-source固定preflight条件付き正式開始承認2026-10-06)で正式作成検証開始を条件付き承認済み。これに伴い§2.2のDocker対象追加をAGENTSへ反映した。上記の「環境実行承認時に残す」記録は改訂時点の履歴であり、現在の承認対象にはReference／E2Eの隔離検証を含む。文書commit・clean source固定とpreflight実施／成立は未完了。
