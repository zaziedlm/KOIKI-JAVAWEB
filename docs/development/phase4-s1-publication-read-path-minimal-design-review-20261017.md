# S1：既存Referenceの処理状態・関連記録を確認する読取経路の最小案（2026-10-17）

状態：**人手照合の参照資料 / GET実装は今回見送り**。今回行ったのは既存sourceの読取と文書整理。実装・Bean/入口登録・DB操作・検証実行はしていない。

**現行の扱い（2026-10-17）：** Ownerは§9を確認後、非同期系を現時点で区切り、復旧は人系の補完を優先して当初Phase4の他タスクへ移行すると指示した。§9は人手照合・HOLD引継ぎの参照として残し、GET案や追加の読取機能の実装は進めない。本案の完成・実装を他タスクへの移行条件にはしない。担当・権限・記録先等のOPENは保持する。

## 1. 目的と位置

[提供判断材料§7〜8](phase4-s1-dod-integrated-status-owner-review-20261016.md#7-当初phase4タスク内での対応付け2026-10-17)を受け、P4-01/P4-A1/A2/D1、S1-R4/R5の範囲で、一件の非同期処理について保存状態と照合に使える記録を確認する。OwnershipはReference `notification`。Framework Public APIや新しい汎用状態管理を作らない。

**今回採るのは、権限を持つ担当者が対象一件を読み、既存記録と照合する手順（§9）。** §3〜8の読取Use Case・GET・権限追加は未実装の設計参考として保持する。読取結果から実行・再投入の可否は判定しない。

## 2. sourceで確認した再利用と不足

| 既存source | 確認した事実と扱い |
| --- | --- |
| [RecoveryPermitService](../../koiki-reference-app/src/main/java/org/koikifw/reference/notification/application/RecoveryPermitService.java) / [RecoveryPermitView](../../koiki-reference-app/src/main/java/org/koikifw/reference/notification/application/query/RecoveryPermitView.java) | permit ID・環境・publicationを指定した認可付き照会がある。publication通常状態は返さない。permitがない通常処理にも使える新しい読取が必要 |
| [V5](../../koiki-reference-app/src/main/resources/db/migration/kkref-notification-async/V5__create_notification_async_publications.sql) | publication ID、listener/type、serialized event、status、attempt、発行/完了/再要求時刻。環境ID・失敗理由・FAILED開始時刻の専用列はない。status/attemptはNULLを許容する |
| [V6](../../koiki-reference-app/src/main/resources/db/migration/kkref-notification-async/V6__create_notification_receipt_observations.sql) / [受理記録Adapter](../../koiki-reference-app/src/main/java/org/koikifw/reference/notification/adapter/outbound/persistence/JdbcNotificationReceiptLogAdapter.java) | notification keyを主キーにevent/recipient/payload hash/acceptance ID/観測時刻を保存。publication IDとの直接FKはない。記録がないことは外部未受理を意味しない |
| [V4](../../koiki-reference-app/src/main/resources/db/migration/kkref-notification/V4__create_notification_recovery_records.sql) | 環境・publicationごとの未閉鎖permitは最大一件。消費をpermit IDで対応付けられる。期限切れ/消費済みでも未閉鎖は残る |
| [既存復旧Adapter](../../koiki-reference-app/src/main/java/org/koikifw/reference/notification/adapter/outbound/operational/JdbcIsolatedPublicationAdapter.java) | event型を限定してdecodeする材料はある。ただし復旧lock/隔離fixtureの認定tableも扱うため、照会用にそのまま呼ばない |
| [Identity Adapter](../../koiki-reference-app/src/main/java/org/koikifw/reference/notification/adapter/outbound/identity/PublicRecoveryIdentityAdapter.java) / [scope Adapter](../../koiki-reference-app/src/main/java/org/koikifw/reference/notification/adapter/outbound/configuration/ManagedRecoveryScopeAdapter.java) / [管理snapshot](../../koiki-reference-app/src/main/java/org/koikifw/reference/notification/adapter/outbound/configuration/ManagedRecoverySnapshot.java) | 現在Identityと環境/publication単位の割当という構造は再利用候補。ただし現行capability集合はpermit操作のみで、新しいpublication照会を許可するものではない |

## 3. 一回の照会範囲と返却内容

入力はpublication ID一つ。環境は信頼する配備設定と接続DBの対応から決める。呼出側の環境文字列を信用して接続先を切り替えない。event IDだけで全publicationを検索する入口、FAILED一覧、任意SQL、期間検索は初回に含めない。publication IDの取得は既存の観測/引継ぎ記録を使い、IDを取得できない案件は人手調査に残す。

| 返す材料 | 最小内容・制限 |
| --- | --- |
| 照会対象/時点 | 環境、publication ID、観測時刻。DBで読んだ時点の情報であり、その後の状態を保証しない |
| publication保存状態 | listener ID、許可対象event型、statusの保存値、attempt、発行/完了/最終再要求時刻。NULLは不明として保持し、0や成功に補完しない |
| eventの関連付け | 型を許可リストで限定して内部decodeしevent IDを取得。初回は既存ExpenseApprovedEvent経路だけ。serialized eventや業務payloadを応答/通常logへ出さない |
| ローカル受理観測 | 内部でkey検索し、event ID・recipient・payload hashを既存eventと照合。一致時にacceptance ID/観測時刻を返す。不一致は「関連付け不一致」、行なしは「保存記録なし」。相手側行の内容は出さない |
| 未閉鎖permit | 環境/publicationで限定し、一件のpermit IDだけ返す。詳細・消費/operation IDは既存RecoveryPermitService.readで別途認可して確認。閉鎖済み履歴の一覧は追加しない |
| 読み取れない材料 | event型対象外/decode不能、記録なし、不一致、未知のstatusや状態/完了時刻の矛盾を区別する。保存されていない失敗理由は「この記録では確認できない」とする |

notification key、recipient ID、payload hashは内部照合に使うが、初回の返却項目には含めない。event ID/acceptance ID/permit IDも認可成立後だけ返し、secretや接続情報は含めない。

publicationの型が対象外ならpayloadをdecodeしない。許可対象だがdecode不能なら取得済みpublicationの最小状態と不足を示し、受理記録を推測で結び付けない。publication未取得時には関連記録だけを別検索して返さない。

## 4. 認可・読取順序の最小案

1. 既存の認証済みFrameworkPrincipalから現在Identityを引き直し、ACTIVEと読取権限を確認する。
2. 配備環境が確定し、有効な当該user/環境/publicationのscope割当がある場合だけ読む。判定不能/期限切れは拒否。拒否時は対象DB内容を返さない。
3. 読取専用の短いtransaction内でpublication一件を取得し、型を限定して内部decodeする。
4. 同じDB snapshot内で、対応する受理記録一件と環境/publicationに属する未閉鎖permit一件をSELECTし、最終recordへ変換する。複数SELECTを使う場合はsnapshotの一貫性を保つ分離レベルを明示し、実DBで確認する。
5. transaction終了後に値だけを返す。DB障害/timeoutは「照会失敗」とし、空結果や正常へ変換しない。呼出側の書込transactionに混在させない。

**読取権限はpermit READと別のReference-owned capabilityを候補とする。** 既存READの意味を黙って拡張せず、権限名/供給元/scope設定形式と既存設定への影響は、実装を再検討する場合の確認事項とする。IdentityQuery/RecoveryScopePortの構造は候補にできるが、ManagedRecoverySnapshotの許可集合には追加対応が必要であり、現行設定だけでは動かない。未接続時拒否を維持する。

認可拒否は既存Security Audit方針へ対応付け、監査記録失敗でも許可に変えない。成功照会用の新Business Audit種別/tableは初回に設けない。読取専用とは対象のpublication/受理/permitを変更しないという意味であり、拒否監査まで無書込とする意味ではない。

DB読取にはbind変数、対象ID/環境条件、有限timeoutを使い、publicationの変更・SELECT FOR UPDATE・復旧advisory lock・provider通信を行わない。読取transactionの宣言だけをDB権限制御の代替とはせず、必要なSELECT範囲を検証環境と配備設計で確認する。

## 5. 判断できること・人に残すこと

| 観測結果 | この入口で言えること / 次の扱い |
| --- | --- |
| COMPLETED＋整合する完了時刻＋受理記録一致 | listener完了とローカル受理観測が対応する。外部処理の最終完了まで保証しない |
| FAILED | DBに失敗状態がある。受理記録を併記して照合へ進む。原因/外部未受理/安全な再送は断定しない |
| PUBLISHED/PROCESSING等 | 保存上の未完了状態。元実行の生存や停止は判定しない。経過時間だけで再投入しない |
| 受理記録なし/不一致、型不明、状態矛盾 | 判断材料が不足する。実行判断は人手照合/HOLDへ戻す。新しい永続HOLD状態は作らない |
| 未閉鎖permitあり | 未閉鎖記録の存在だけを示す。実行中/実行可能/未消費とは推定しない。既存permit照会で詳細確認 |
| 対象なし | 当該DB/snapshotでpublicationを取得できない。発行拒否/rollback/誤ID/削除等の原因は別途照合。権限外と不存在は外向け応答で存在確認に使えないようにする |

任意のログ/Audit本文の取込、失敗理由の新規保存、実providerの受理照会、全停止証明、permit発行/consume/close、再送・パージは含めない。関連AuditやHOLD記録はevent/publication/permit IDから既存の手順で人が確認する。

## 6. 変更候補と提供入口

実装する場合は、`notification/application`の読取Use Case、`application/query`の最終record、`application/port/outbound`の読取契約、`adapter/outbound/persistence`のJDBC読取実装を候補とする。既存permit照会を変更せず、schema/migration/依存/Public APIを追加しない。型判定/受理照合はAdapter内部に閉じ、他moduleのRepositoryやEntityを参照しない。

これに読取認可/scopeの設定と条件付き登録、実際の利用入口が必要になる。**未登録のUse CaseやToolingの直接呼出だけで、開発者向け照会機能の提供完了とはしない。** 最初の入口は既存認証方式に沿う一件GETを候補とし、Session MVC/既存RESTのどちらに置くかはSecurity経路との整合を確認して決める。新しい認証方式や管理consoleは作らない。通常無効の既存構成を保護する。

権限/入口の追加が必要性に比べて大きくなる場合は、認可を省いたAPIを作らず、権限を持つ担当者による対象限定SELECTと既存permit照会の手順へ倒す。その場合は一般利用者向け機能が未提供であることを明示する。

## 7. 読取機能を再検討する場合の確認事項

読取権限・scope供給と一件GETの設計候補は§8に残す。今回の実装・検証は行わない。将来、具体的な必要により実装を再検討する場合は、変更path/既存設定の互換性と以下の必要検証を対象にする。

- 現在Identity/権限/scopeの拒否と取得不能、別環境/別publicationの非漏えい。認可成立前に対象データを読まないこと。
- 完了/失敗/未完了/NULL等の保存値、受理記録の一致/なし/不一致、対象型以外/decode不能、対象なし/DB障害を誤って成功へ寄せないこと。
- 未閉鎖permit一件と既存詳細照会の対応、消費/期限切れを実行許可に読み替えないこと。
- 読取途中の状態変更に対するsnapshot、有限timeout、対象tableの非更新・provider/復旧の非呼出、payload非露出、通常構成の保護。

全組合せを作らず、判定と非漏えいに必要な代表ケースを選ぶ。旧接続実証の予算・実行許可を流用しない。

## 8. 読取権限・scope供給・一件GETの配置案

Ownerはこの三点の具体化と、複雑化への注意を指示した。以下はsource照合による候補の絞込みで、正式採用/実装開始の承認記録ではない。

### 8.1 配置と読取権限は既存機構に寄せる

| 項目 | 具体案と理由 |
| --- | --- |
| GET配置 | `GET /notifications/publications/{publicationId}`、既存Session認証を使うReference MVC。一件のHTML詳細だけを候補とする。新しいJSON API/CLI、一覧/検索、自動polling、操作ボタンを加えない |
| 所有/構成 | `notification/adapter/inbound/web`のControllerと表示DTO、`templates/notification/publication-detail.html`から§6の読取Use Caseへ接続。Application recordをEntityやpayloadの代わりにそのまま全公開しない |
| Security経路 | [ReferenceSecurityConfiguration](../../koiki-reference-app/src/main/java/org/koikifw/reference/identity/configuration/ReferenceSecurityConfiguration.java)に上記パスだけをSession chainの対象として追加する候補。現在はそのパスを扱っておらず、追加なしでSession照会できるとはしない。既存CSRF/Header/logoutを維持し、認可はUse Case内でも毎回行う |
| `/api/**`を選ばない理由 | [ReferenceApiSecurityConfiguration](../../koiki-reference-app/src/main/java/org/koikifw/reference/identity/configuration/ReferenceApiSecurityConfiguration.java)は任意有効化のstateless Bearer経路で、expense向け適用権限を要求する。今回の照会のためにJWT契約やそのchainの権限を変える必要はない |
| 読取capability | `NOTIFICATION:PUBLICATION:READ`一つを候補とする。`PERMIT:READ`や`IDENTITY:ADMIN`を代用せず、既存permitの発行/実行/閉鎖権限を付けない。Sessionの古い権限だけで通さず、IdentityQueryから現在のACTIVE/permissionを確認する |
| 権限の供給 | [既存IdentityAdministration](../../koiki-starters/koiki-starter-identity/src/main/java/org/koikifw/identity/IdentityAdministration.java)のregisterPermission/grantPermission/assignRoleを使う管理手順を候補とする。APIは存在するが、Reference画面は既存roleの付与/取消が中心。権限管理画面、Framework変更、権限seedの自動追加は作らない。実user/roleへの登録・付与は管理責任者の別操作で、今回は行わない |

未認証は既存Sessionのlogin経路へ戻す。認証済みのscope外と対象不存在は同じ404/固定文言とし、対象の存在を出さない。Identity/scope照会不能やDB障害は503等の取得不能として区別し、空の成功応答にしない。外向け詳細にDB例外や権限設定を出さない。詳細ページはno-storeとし、値はHTML escapeする。GET以外の操作handlerを作らず、意図しないmethodから読取Use Caseを呼ばない。

既存permit詳細のHTTP入口は今回作らない。ページにpermit IDが出ても、新しいpermit操作へのリンクやボタンは置かず、既存の認可付きApplication照会/担当者の照合手順に渡す。

### 8.2 scopeの既存供給には常用上の制約がある

現行[ManagedRecoverySnapshot](../../koiki-reference-app/src/main/java/org/koikifw/reference/notification/adapter/outbound/configuration/ManagedRecoverySnapshot.java)と[Loader](../../koiki-reference-app/src/main/java/org/koikifw/reference/notification/adapter/outbound/configuration/ManagedRecoveryConfigurationLoader.java)は、次を固定している。

- 一環境、user/capability/publicationの完全一致。最大64 grant、wildcardなし。
- 有効期間は最大30分。起動時に一回読み、動的更新しない。
- SHA-256/revision/環境を管理側manifestと突合し、未知capabilityや不正な設定を拒否する。

したがって、限定診断だけなら許可capability集合へ読取一つを加え、同じ形式/Port/Adapterを再利用する案が最小になる。旧4 capabilityの意味、期間、permit TTL、grant構造は変えない。旧設定は新しいGETを許可しない。新capabilityを含む設定は旧binaryでは拒否されるため、配備を戻す場合は旧互換設定へ戻す必要がある。

ただし、**新しいpublicationの照会や期限後の再照会のたびに、設定の準備・確認・再読込が必要になり得る。これは通常の状態確認にそのまま適する仕組みではない。** 現行は再読込にアプリ再起動が伴う。照会のために稼働中の非同期処理を停止/再起動する手順にはしない。既存30分上限の延長、全publicationの許可、動的grant更新/新しいscope tableを今回追加しない。

またpublication側には環境列がない。scopeの環境文字列だけではDBの取り違えを防げない。配備環境と接続DBの対応を管理側で確認できない場合は開始しない。requestから環境/接続先を指定する機能は作らない。

### 8.3 現段階の推奨：GETは候補に留め、担当者の限定照会を使う

権限一つとSession詳細画面の追加自体は限定できる。しかし常用照会にするにはscopeの供給/更新と、読取だけで起動する構成の判断が残る。**現段階ではGET実装を先行せず、担当者による対象限定読取を提供手順へまとめる案を推奨する。** これは恒久的なGET不採用でも、Ownerの保留承認済みでもない。複雑化を避けるための今回のレビュー結論案である。

読取のためだけに`async.enabled=true`を要求しない。[既存async有効化](../../koiki-reference-app/src/main/java/org/koikifw/reference/NotificationAsyncEnvironmentPostProcessor.java)はModulith/追加migrationの選択にも関わり、[非同期Configuration](../../koiki-reference-app/src/main/java/org/koikifw/reference/notification/configuration/NotificationAsyncConfiguration.java)はexecutor/listener/providerを登録する。将来GETを採用するなら、既存tableへの読取登録を送信/復旧登録から分け、通常無効と起動時の非副作用を確認する必要がある。今回これを新しい起動modeやprofileの実装へ広げない。

| 人手の限定照会で残すもの | 最小の扱い |
| --- | --- |
| 誰がどこを読むか | 管理責任者が環境/DB/対象publicationと閲覧担当者を照合する。既存の許可済みSELECT経路だけを使う。DB権限があることをアプリのscope認可成立とは呼ばない |
| 何を読むか | §3の保存状態と未閉鎖permitを一件に限定。受理照合に必要なevent内部値は許可担当者の作業内だけで扱い、raw payload/key/宛先を引継ぎ記録へ転載しない。安全な抽出/照合ができない材料は確認不能として残す |
| 不明・取得不能 | 担当付きHOLDへ戻し、既存の記録/引継ぎ方針に従う。権限追加、再起動、provider照会、再送へ自動で進まない |
| 提供状態の表示 | 「担当者の照会手順で補完。一般利用者向け一件GETは未実装」と明記する。通常監視や原因診断が自動化済みとはしない |

この方法にも実際の担当者/接続経路/閲覧権限の確認が必要であり、既に利用可能とは断定しない。担当者向けの対象一件の読取順・照合項目・確認不能時の戻し先は§9に整理済み。DBを実際に照会する際は環境と操作範囲を確認し、今回の文書整理を操作許可に流用しない。

## 9. 担当者向け：一件の読取・照合・HOLDへの戻し方

状態：**手順レビュー案 / OWNER REVIEW INPUT**。Owner指示により、既存記録を使う手順を具体化した。§8のGET案全体の採否を確定した記録ではない。今回DB/providerへの照会、権限付与、復旧、引継ぎ連絡は行っていない。

### 9.1 始める前に一件を固定する

[人手HOLD引継ぎ案§2/7](phase4-s1-r4-human-hold-handoff-guide-20261016.md#2-既存運用記録に残す最小項目)の既存インシデント記録等を起点にする。新しい受付tableや一件管理台帳は作らない。対象環境/接続DB、publication ID、閲覧担当者、許可された読取経路、既存のHOLD/再投入禁止事項を確認する。実際の記録先・担当・権限が未設定なら、未設定の内容を残して運用責任者へ戻し、照会を開始しない。

publication IDが分からない場合は既存の観測/引継ぎ記録を確認する。別IDの全件探索やevent IDだけからの推定で対象を決めない。原文payload・宛先・credential・通知keyを共有記録や通常logへ転載しない。

### 9.2 読取順と照合項目

以下は許可済みの読取経路で担当者が確認する順序であり、新しいSQL実行ツールや一括collectorを作るものではない。取得元・取得時点・取得者と必要範囲の証拠参照を、同じ既存記録に残す。

| 順序 / 既存記録 | 読む・照合する項目 | 不足した場合の扱い |
| --- | --- | --- |
| 1. publication（V5） | 指定ID一件のlistener/event型、status/attempt、発行/完了/最終再要求時刻を保存値のまま確認。環境は接続DBとの対応で確認する。NULLを0や成功に補わない | 対象なし/別環境/対象取り違えなら関連記録の探索へ進まず、原因未確認として戻す。DB障害/権限拒否は「取得不能」であり「対象なし」ではない |
| 2. eventとReference受理観測（V6） | 既存ExpenseApprovedEvent型だけを対象に、許可された手段でevent ID/key/recipient/payload hashの対応を確認。keyで得た受理行のevent/recipient/hashをすべて照合し、一致時のacceptance ID/観測時刻と元証拠参照を残す | 安全なevent抽出手段がない、型不明、decode不能、行なし、不一致を分ける。手計算や目視によるpayload hashの推定で一致としない。受理行なしも外部未受理とはしない |
| 3. 未閉鎖permit（V4） | 同じ環境/publicationの未閉鎖permitの有無/IDを確認。ある場合、既存readが許可済みの経路から呼べる時だけ発行時target・期限・消費operation/worker世代/時刻を照会。現在publicationと発行時targetの差も残す | HTTP入口は未実装。既存readを呼べなければ利用可能と見なさない。別途許可された対象限定SELECTで確認できる項目だけ記録し、残りは取得不能。行なしと未確認を区別。引継ぎのための発行/consume/closeをしない |
| 4. 既存Audit/log/HOLD証拠 | permit IDからpermit Audit、operation IDがあれば結果Audit、event/publicationから既存log・一件記録をたどる。既存のprovider受理証拠があれば出所/時点と対応を照合 | 結果Auditなしを未実行と断定しない。取得できない証拠は未確認とする。追加のprovider通信、trace基盤導入、無関係な全log取得へ広げない |
| 5. 今回の確認結果 | 過去のsnapshot/禁止事項と比較し、変化・不一致・未完了を追記する。正常観測だけの場合も「どの時点の何が一致したか」を限定して記す | 状態変化や観測時点のずれを推測で埋めず、判断不能ならHOLDへ戻す。最新値で過去の記録を上書きしない |

受理記録がない等の不足だけなら、権限/対象が確かな残りの読取で補足してよい。ただし対象の取り違えや権限不明が判明したら、その先の読取を止める。同keyの別eventや別publicationを同じ一件へ自動的に統合しない。関連が疑われる場合は元の一件記録に関連IDと不一致/未確認だけを追記し、別publication横断の扱いは[引継ぎ案§9](phase4-s1-r4-human-hold-handoff-guide-20261016.md#9-未閉鎖permitの保証範囲と別publicationの扱い2026-10-16)に従う。

各読取が別時点なら、それぞれの取得時点を残す。同一DB snapshotを使ったと確認できる場合だけ、その範囲を同時点の材料として扱う。DB外のlog/Audit/provider証拠まで同一snapshotとしない。現行processの生存/停止・停止維持も、この読取順からは保証しない。状態が動き続ける時は自動pollingや一致するまでの反復をせず、今回確認できた事実と不足を残して戻す。

既存HOLD logの`keyHash`にはpayload hashが渡る既知の制限がある。[追跡境界§3](phase4-s1-r5-correlation-observation-recovery-review-20261016.md#3-既存hold-logのhashを取り違えない)に従い、通知keyのhashと混同しない。独立した元証拠で確かめられなければ照合不能とする。

### 9.3 HOLDへ戻す時の一回の引継ぎ

不足や矛盾があれば、次の三つを**同じ既存の一件記録**へ追記する。既存HOLDがある場合は維持し、新たな疑義の場合も復旧判断を保留して担当者へ戻す。正常な一件まで一律にHOLD化するものではない。

1. **分かった事実と不足**：環境/publication、観測時のstatus/attempt、照合できたID、各取得時点/証拠参照、取れなかった材料と理由。「なし」「未確認」「取得不能」「不一致」を文章で区別する。新しいDB状態enumは作らない。
2. **未完了と再投入禁止**：例「受理記録なし。外部未受理は未確認。追加通知送信・再公開・新permit/operationでのやり直しは行わない」。FAILED/長時間経過/permit期限切れ/消費済み/Auditなしを再試行の理由にしない。既存未閉鎖permitを閉鎖・削除して入口を空けない。
3. **次の担当と確認**：主担当/引継ぎ先、次回確認時点、誰が何を確認するかを残し、引受けを確認する。担当不在/未割当/連絡不能ならその事実を残して運用責任者へ引継ぎ判断を求める。確認間隔や連絡期限は運用側で決め、ここで数値を創作しない。

確認先は不足に合わせて絞る。環境/接続/閲覧権限は運用・DB管理担当、publication/受理観測/permit/Auditの対応はReference担当、既存の外部受理証拠はprovider側の確認担当、現行機構で解消できないFAILED完遂や処置方式はArchitecture/運用Ownerへ戻す。実際の担当名/連絡先はOPENであり、今回連絡や引受けを実施した記録ではない。

取得可能性が回復した場合は[追跡境界§4](phase4-s1-r5-correlation-observation-recovery-review-20261016.md#4-観測不能から回復した時の最小手順案)に従い、新しい取得時点の材料で前後を照合する。**照会できたこと、一致したこと、担当者が交代したことだけでは、既存HOLD・再投入禁止・未閉鎖permitを解除しない。** 案件の調査終了、publication完了、permit閉鎖、復旧実行の許可はそれぞれ別に扱い、本手順から操作を開始しない。

### 9.4 既存記録への最小追記例

以下は既存インシデント記録等へ追記する文章例で、新しい帳票/台帳の必須化ではない。

```text
対象：環境=<確認済み環境> / publication=<対象ID>
確認：<取得者・取得時点・取得元/証拠参照>
事実：status/attempt=<保存値または未確認>、受理照合=<一致/なし/不一致/取得不能等>
関連：event/permit/operation=<確認できたIDのみ。ない・未確認を区別>
不足：<確認できない材料と理由、前回からの変化>
扱い：<未解決ならHOLD継続。追加送信・再公開等を行わない理由>
引継ぎ：<主担当/確認先、次回確認内容・時点、引受け状況。未割当なら責任者への確認先>
```

端末tmpだけの証拠を引継ぎ可能とせず、既存の権限制御された保管先への到達性を確認する。source/artifact/runに依存する検証証拠はその文脈も添える。保管先/保持/担当や安全なevent抽出手段が未確定なら、その不足を残し、手順の文書化を運用準備完了としない。
