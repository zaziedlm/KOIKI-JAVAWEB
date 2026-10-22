# C2-01：代表外部操作の候補比較と調査・設計範囲（2026-10-22）

状態：**Aの実装・限定検証終了、新規29 PASS／1 SKIP・結果受入待ち（§10）。** 開始承認は§9、具体設計はAだけへ改訂した§7。Bは§8に従い条件付き後続、§6のA＋最小B案は比較履歴。§1〜7の「未実行」等は調査・設計時点の記録として保持し、現行結果は§10と結果文書を入口にする。接続timeout未実証、通常rootの最終clean失敗と集約だけの再開を区別する。結果受入・commit・remoteは別判断。

確認source：`docs/phase4-review-integrated-plan-20261020`／`6fdc2a8`、開始時clean。ツールから確認できるcheckoutはWindowsの`C:/Users/kataoka/Desktop/KOIKI-JAVA/KOIKI-JAVAWEB`。本調査をCloud上の動的検証結果として記録しない。Gitの追跡refに対して1 commit先行しているが、今回remote更新はしていない。

## 1. 目的と既存成果

[統合計画§5.2](phase4-review-integrated-task-plan-20261020.md#52-各テーマの作業単位と着手条件)のC2-01を具体化する。[他package分解§2.2](phase4-other-packages-breakdown-draft-20261005.md#22-c2外部api耐障害性4-7)に従い、Referenceが代表操作・Use Case・provider契約、Toolingが模擬providerと障害注入、Frameworkが共通境界のreviewを所有する。Customerの実案件や実providerの採用を推定しない。

DoD4-7は「外部APIの障害時にリトライと流量制限が働き、タイムアウトが既定値で機能する」。read-onlyと副作用ありを区別し、外部I/Oを業務transactionに含めない。実装方式はグランドデザイン§18.2〜18.4へ照合して後続設計で選ぶ。

| 現行source | 読取で確認したもの | C2へ残る差分 |
| --- | --- | --- |
| `koiki-starters/koiki-starter-api/src/main/java/org/koikifw/starter/api/internal/KoikiApiResilienceAutoConfiguration.java` | Servlet・有効化property条件下でSpring resilient methodsを有効化 | 代表操作の実HTTP呼出、同時実行・飽和時の動作をまだ実証していない |
| 同directoryの`KoikiApiDefaultsEnvironmentPostProcessor.java` | retryのmax-retries=2、delay=10ms、timeout=1sを低優先度の既定値として供給 | このtimeout値だけでHTTP接続・読取timeoutが実効設定済みとは判断しない。標準HTTP構成の設定源と実効値の調査が必要 |
| `build-support/runtime-foundation-verification/.../ApiStarterSmokeTest.java` | ローカルprobeの選択例外で3試行、対象外例外で1試行をassertするtest source | 今回未実行。HTTP無応答・body遅延・資源保持・総待機時間の証拠の代用にしない |
| `koiki-reference-app/.../notification/adapter/outbound/external/HttpNotificationProviderAdapter.java` | numeric loopback限定、接続2秒／request5秒、通知key、受理／UNKNOWN／照会契約 | S1-ownedの限定実装。汎用C2実証・実provider契約に転用しない |

Starter内の`RestClient`／HTTP timeout customizer／`ConcurrencyLimit`等の名前によるsource検索では該当実装を確認できなかった。これはBoot依存を含む全runtime挙動の不存在証明ではない。現在の標準設定源・HTTP request factory・proxy・接続資源を次の設計で確認する。

## 2. 操作候補の比較

以下はいずれもReference用の設計候補であり、採用済みの業務要件ではない。

| 候補 | 操作と副作用 | 障害・検証可能性 | 依存・範囲 | 評価 |
| --- | --- | --- | --- | --- |
| A：模擬の外部換算レート照会 | 明示した通貨対・基準日の参考値をGETで取得。provider契約上、業務更新・課金・予約を発生させない前提。金額確定には使用しない | 503後の回復、無応答、遅延、429、並行上限、取消しと資源解放を有限stubで観測しやすい | expense／master／notificationの更新・認可・イベントへ接続せず、独立したReference操作とする | **初回の推奨候補**。C2のretry・timeout・流量制限を小さく実演できる |
| B：模擬の外部受付登録 | POSTで受付記録を作成しreceiptを返す、独立した模擬command。現実の外部業務や配信は行わない | 受理後応答喪失、非冪等POSTの非再送、同key／同payload・異payloadをstubの受理台帳で観測可能 | 受付の意味・key・照会の有無・保持期間が追加設計。正式operator受付や復旧機構は作らない | **副作用・受理不明の補完候補**。Aの成功だけでこの保証を成立済みにしない |
| C：既存通知providerを拡張 | S1の通知POST／照会を利用 | 資材はあるが、配信・復旧・publication・UNKNOWNとの整合へ影響する | S1区切り判断・既受入結果に接続する。今回の独立C2選定の範囲が拡大 | 今回は選ばない案。既存実装を修正・再実行しない |
| D：Customerの実外部API | 実業務の参照／更新 | 実SLA・認証・課金・副作用のEvidenceが必要 | 接続先・資格情報・所有者・試験権限の入力なし | 実案件入力後の候補。今回は待ち条件を作らずAを具体化する |

候補比較ではA＋最小B案も検討したが、**現行判断はAを先行完成させ、Bを条件付き後続開発とする（§8）**。Aは単なるHTTP疎通や固定値testにせず、実際のclient・proxy・timeout・concurrency制御を通したHTTP stub検証へ進める。Bの受理不明・副作用あり操作は今回の未実証範囲として明示し、その実装をAの完了条件にしない。DoD4-7の文言は維持し、Aの実測結果から成立範囲を判断する。現時点で達成済みとはしない。

## 3. Aを選ぶ場合の失敗契約案

| 条件 | 動作案 | 必要な証拠 |
| --- | --- | --- |
| 正常応答 | 有効期間・通貨対・値の形式を検査して参考値を返す。前回値や0を成功として補わない | 呼出数、返却内容、相関ID。実業務金額の確定には接続しない |
| 対象外入力・providerの確定4xx・不正body | 自動retryしない。意味を分類して呼出側へ返す | 試行1回、不正値が後段へ進まないこと |
| 明示した一時障害 | retryは一層だけ。対象例外、追加試行数、待機、総予算を限定する | stub受信時刻・試行数・最終結果。例外全件retryを行わない |
| 429 | 無条件に即時再送しない。retry可否と待機指示が総予算内かを設計する | 待機量・上限超時の停止。429を503と一律同義にしない |
| 接続失敗・応答遅延／無応答 | 接続・応答待機・全試行の総予算を別に定める。読取でも有限に停止する | 設定実効値と壁時計時間、残存処理・接続の解放。アノテーション値だけで成立扱いにしない |
| 並行上限到達 | 同時実行、待ち行列、待機期限、超過時の応答を有限化。retry待機と制限獲得の順序を明示する | provider最大同時数、待機中を含むWeb thread・接続数、飽和後の回復 |
| 呼出元取消し・割込み | 新しい試行を開始せず、保有資源を解放する契約を選ぶ | 呼出元の終了だけでprovider側停止も成立したとしない。残存処理・枠の解放を観測 |

Bを補完採用する場合は、POST送信後の通信断をUNKNOWNとし自動再送しないことを基準案とする。stable keyがあるという理由だけで安全なretryを認定せず、providerの同key・payload整合・保持・照会保証を先に定める。人が結果を照合して次操作を判断する有限手順を示し、S1のprovider保証や新しい復旧runnerを持ち込まない。

## 4. 次に具体化する設計と検証

候補選定後、同じ記録を更新して以下を埋める。未定義の新規property・endpoint・Public API・依存versionを今回固定しない。

1. 操作の入力／出力と利用者、Reference内のbusiness package、Tier、入口・認可・エラー応答。既存expense Application／Domainへの外部I/O挿入は行わない。
2. グランドデザインの標準HTTP client構成、retry／concurrencyの適用箇所とproxy通過、設定源・override・defaultの実効値。現行Starterとの差分が必要ならその範囲を提示する。
3. 接続／応答・総時間、retry回数、同時実行・待機上限、body上限、接続解放。外部I/O中に業務transactionやDB connectionを保持しない構成とその観測方法。
4. Spring標準で満たす範囲とCircuit Breakerの必要性を評価する。Resilience4jは名称だけで採用せず、必要な場合に限り第三者library基準・依存／配置を比較する。
5. 正常、一時失敗後成功、非retry例外、連続失敗、各timeout、429、飽和・取消し、設定override、通常構成無効時の保護を検証集合へ落とす。B採否に応じて受理不明・重複の集合を追加／除外理由と対応付ける。
6. source・artifact、対象file、実行case／件数、Cloudの利用可能なHTTP stub／DB、JVM・接続・時間予算、停止／cleanupを実装・実行前に具体化する。旧S1予算・runnerを流用しない。

追加DB／migration、UI、別artifact、共通Starter・Framework Public APIは必要性を示してから扱う。新しい業務状態管理のためにそれらを自動追加しない。模擬providerの障害switchはToolingに閉じ、Application JARへ混入させない。

## 5. 今回の出口

- 現行sourceとC2の差分、候補A〜Dの比較、A推奨とBの補完位置を提示した。
- 次の判断は代表操作Aを選定し、§4の設計を具体化するか。Bの実装・受入範囲やC2全体の網羅はまだ確定していない。
- B1の選定ブラウザ検証結果は既にOwner受入済み。DoD全体の受入・B1-08全体完了はこの調査開始だけで自動確定しない。
- 今回は文書のみ。コード・依存・migration変更、実通信・test、commit・pushは実施していない。

## 6. C2全般を発散させずに進める対応方針案（選定前の比較履歴）

Ownerは「C2全般として、発散しない見通し範囲で対応する方策、方針をまとめたい」と指示した。以下はその時点のA＋最小B比較案。後続の§8の判断で対象はA先行へ変更し、B関連の実装・検証と旧予算は採用しない。有限制御・停止／cleanup・DoD対応の方針を引き継ぎ、現行の件数・file・予算は§7へ従う。

### 6.1 一回の対応範囲

**目的は、選定した外部操作について、失敗時も呼出回数・待機・同時実行・副作用が説明できるReference利用例とEvidenceをそろえること。** あらゆる外部連携に使える汎用基盤の完成を今回の出口にしない。

| 区分 | 今回そろえる提案 | 今回へ自動追加しないもの |
| --- | --- | --- |
| 主操作A | 模擬の外部レート照会1操作。read-only・表示参考専用。成功、限定retry、timeout、飽和と回復 | 実相場・換算業務、expense／masterの確定判断や更新への接続、複数provider切替 |
| 補完B | 模擬受付POST1操作。非冪等操作は自動retryなし、受理後応答喪失はUNKNOWN、送信・受理回数をstubで照合 | 冪等再送の全面実装、provider履歴の長期保持、復旧worker・受付管理画面・正式operator入口 |
| 共通制御 | Spring標準を第一案として、設定源と実効値、有限時間・回数・同時実行、例外分類を示す | 新Starter・独自resilience framework・汎用DSL・一律の全API改修 |
| 入口 | 正式に採用する入口を一つに絞る。既存Security・認可・例外境界を利用し、必要なHTTP/APIで実演する | SPA／MVCの新画面、IdP／BFF／Customer構成の新規設計。test専用入口の本体混入 |
| 状態／証拠 | 現行契約で必要なlog／Audit、有限stub受理台帳、資源・試行記録。DBを使う場合は何を証明するか明示 | 本体の新table／migrationを前提にした履歴・照合管理。既存Audit契約を迂回する省略 |
| 実行環境 | 同一sourceのReference＋一つの所有HTTP stub。有限fixtureと必要な隔離DBだけ | 実provider・実資格情報・実課金・AWS配備・複数hostの性能保証 |

Bのstub台帳は検証中の有限な観測用で、正式な耐久台帳やprovider保証ではない。UNKNOWNは相関情報と安全な状態を示し、人による照合・保留へ渡す。手動の照合材料と停止判断が分かる短い手順までを用意し、人が安全に再送できるという保証は追加しない。共通Port／DTOを先に汎用化せず、今回2操作の必要範囲で責務を決める。

### 6.2 技術選択を広げない規則

1. まず既存StarterとSpring標準の実効動作・差分を確認する。retryの時間制限とHTTP I/Oのtimeoutを混同せず、接続・応答待ち・全試行総時間を別に定める。
2. retryは一層・選択例外・有限回数。並行制御との適用順、待機中に保持する枠、超過時の応答を設計で決める。非冪等Bには自動retryを付けない。
3. Circuit Breakerは必要性と不採用時の限界を評価する項目として閉じる。今回はtimeout・流量制限で選定要求を満たすなら追加しない。Resilience4j実装を自動的な完了条件にしない。標準で不足する具体要件が判明した場合だけ、第三者library基準による採否を判断する。
4. DoD4-7の「既定値でtimeoutが機能する」はReferenceに数値を書いただけでは成立としない。設定源・利用条件・overrideと実効timeoutを対応付ける。Frameworkの既定値／公開契約に不足があればその差分だけを提示し、Referenceのローカル設定で代替充足したことにはしない。
5. S1のsender・listener・publication・permit・復旧契約は変更しない。非同期回復、File／Batch／OpenTelemetry全体、Virtual Threadsは各packageへ残す。既存資源の再利用は現source・適用範囲を照合した必要部分に限る。

### 6.3 実施順と段階ごとの区切り

| 段階 | 今回の出口 | 次へ持ち越すもの |
| --- | --- | --- |
| C2-01：選定・契約設計 | A／最小Bの採否、入出力・認可／失敗表、制御方式、対象file、検証集合、概算、資源・時間・停止条件を同じ文書へ具体化 | 未決定はOPENとし、関係する実装を先行しない。全packageの再設計へ広げない |
| C2-02：最小実装・早期確認 | Referenceの2操作と必要なAdapter／構成、Tooling stub。まず正常と主要失敗、非retryと実効timeoutを確認 | Framework差分・依存追加が必要なら内容と影響を示して判断。新しい包括Gateを増やさない |
| C2-03：限定実証・統合・受入整理 | §6.4の集合、必要な直接回帰、最終の通常構成と選定構成、資源・cleanup、利用方法・残課題・Owner判断記録 | 未解決は範囲・影響・次条件を添えて残す。限定結果をC2全体／DoD4-7 PASSへ自動昇格しない |

各段階で結果を整理し、**同じ問題への修正・再実行は事前に決めた予算内の原因に直結した一巡を原則**とする。開始前に各集合の初回・訂正後の再実行枠を決める。原因が分からないまま反復しない。計画予算を超える場合は未成立として止め、得られた結果と不足を提示する。時間の経過や残予算を追加実行の許可にしない。

### 6.4 初期の検証集合案

以下は**12の検証目的群**であり、test method／invocation数や網羅性の確定値ではない。設計時に各群を具体caseへ対応付け、不足は追加理由・費用・既存集合への影響とともに判断する。ケース数だけを先に固定して必要な安全確認を落とさない。

| 群 | 確認すること |
| --- | --- |
| V01 | A／Bの正常応答と適切な入出力、要求数・相関 |
| V02 | Aの選択一時障害→有限retry→成功、retry一層・試行数 |
| V03 | Aの対象外例外／確定4xx／不正bodyは再試行せず安全に返す |
| V04 | Aの連続失敗が回数・総予算内に終了し、成功fallbackを捏造しない |
| V05 | 接続失敗・応答／body遅延のtimeout、既定値の適用とoverride、実効時間 |
| V06 | 429の方針と待機上限。総予算を超えて再送・待機しない |
| V07 | 同時実行制限・超過時拒否／有限待機・飽和後の回復 |
| V08 | 取消し／割込み後の再試行停止、枠・接続・所有processの解放。provider側停止保証と区別 |
| V09 | Bは失敗種別にかかわらず自動retryなし。受理後応答喪失はUNKNOWN・要求1回で追加副作用なし |
| V10 | 外部I/Oを囲む業務transaction・DB接続の不要な保持なし、通常入口の認証／認可・安全な例外応答・秘密非出力 |
| V11 | 既存MVC／SPA／Bearer・Architectureへの直接回帰、通常構成と選定構成のartifact分離 |
| V12 | 有限fixture終了、所有stub／接続／DB／process／portのcleanup、source・artifact・raw対応 |

接続blackholeやbody遅延は環境依存性があるため、再現方式を実行前に決める。別の失敗を観測して対象timeoutの成功に読み替えない。新しい試験環境を無制限に作ることもせず、再現できなければその枝を未実証と記録する。

標準の通常rootを細かな変更のたびに反復しない。局所・直接回帰を先行し、固定sourceで最終合流を行う。合流後にsourceを修正した場合は影響する集合とartifactの再作成範囲を示す。Cloudの実行環境と証拠を記録し、手動browser確認は新UIやCookie・経路等への具体的影響がある場合に対象を絞る。C2のためにB1手動全項目を自動反復しない。

### 6.5 拡張を止める判断

| 判明したこと | 今回の扱い |
| --- | --- |
| 副作用重複、無制限retry／待機、上限超過、cleanup不能 | 対象実行を止める。所有資源の停止・保全を優先し、結果を未成立として提示。安全条件が戻るまで次集合へ進めない |
| 想定内の局所不具合 | 原因・修正file・必要回帰を示し、決めた訂正枠内で一巡。根拠のない追加探索をしない |
| S1再開・業務変更・新しい永続状態管理が必要 | C2から切り出す。該当操作の不成立／対象変更案としてOwnerへ戻し、今回の完了条件へ黙って加えない |
| 実provider・資格情報・Cloud通信許可等の入力不足 | 必要入力を明示。模擬実証で進められる範囲と、実環境未成立を分ける |
| Resilience4j等の新依存・Framework公開契約変更が必要 | 具体的な不足と最小差分を別に判断。単に将来使えそうという理由で追加しない |
| 新たな障害枝・組合せが発見された | 今回の要求を壊す直接影響か、残課題かを分類。必要な追加は集合・予算を更新して判断し、12群を網羅性の証明にしない |

### 6.6 今回の完了判断と後続へ残すもの

今回の出口は、次の五点を一つの結果記録へまとめ、Ownerが受入範囲を判断できる状態とする。

1. 選定A／Bの意味・今回提供する利用例・失敗契約・制御値が確定し、必要な実装とEvidenceが対応する。
2. retry・流量制限・HTTP timeoutの実効値、通常構成保護とcleanupが成立する。未成立の必須項目をPASSにしない。
3. 正常／拒否／UNKNOWNの呼出側応答と、人が照合・保留すべき条件が説明できる。保証していない再送／配送を明示する。
4. DoD4-7の各要求に、成立・未達・適用条件を対応付ける。必須の不足が残れば限定成果だけを受け入れ、C2全体やDoDの達成は未認定と記録する。採否・延期・DoD変更はOwner判断を必要とする。
5. 利用手順、残課題、再開triggerと次packageへの入力を記録し、受入判断をその場で反映する。commit／pushとは別に状態を確定する。

実provider・実SLA・実負荷、複数instanceでの制限、永続冪等台帳・安全な再送保証・正式運用の復旧、Circuit Breakerの実装、Customerの各接続先は、必要性が具体化した時の後続候補として保持する。これらをすべて満たすまで他のPhase4 packageへ進めない、という条件は置かない。一方、今回選定した要求の不足を「残課題」と呼ぶだけでDoD達成にしない。

### 6.7 次の一手

§6の対象・区切り方をOwnerと確認した後、**A＋最小Bを想定したC2-01の具体契約と実装・検証見積り**を埋める。初回の実装file・検証case数・command・資源／時間はその設計結果から定める。本方針案自体を実装開始や旧予算再利用の根拠にしない。

## 7. C2-01具体化：A先行の契約・実装対象・検証案

Ownerの具体化指示と§8のA先行判断に基づく設計結果。旧A＋最小B案の38件・23file・120分枠はA先行の30件・22file・105分枠へ差し替えた。以下のpath・property・数値はreview可能な実装案であり、既存Public APIや実行済み結果として固定したものではない。C2-02のコード作成や動的検証はまだ開始しない。

### 7.1 所有・責務と入口

- Reference内の独立business packageを`org.koikifw.reference.externalcheck`とする案。Tier1 SIMPLE、外部照会の調整をApplicationへ置き、他業務moduleを参照しない。Application所有PortをHTTP Adapterが実装し、transport DTOとApplication DTOを分ける。Bの将来用method・DTO・Beanは追加しない。
- module metadataは現行規約が許容するJPA／SHAREDを既定として宣言する案。今回はbusiness Entity／Repository／table・Business Audit追加はない。JPAを用いた業務永続化を実証したという意味にはしない。永続化なし用enum・Rules改訂は追加しない。この宣言と新packageの適合は早期Architecture検査で確認する。
- `koiki.reference.external-check.enabled=true`でReference-ownedの構成・入口を明示有効化し、既定false。通常起動で新規Bean／HTTP client／入口を有効化しない。C2専用Maven profile、module、JARは増設しない。
- 入口は既存Session chain下の`/spa-api/v1/external-checks/**`に限定。新しいReference permission案`EXTERNALCHECK:USE`をURL・Application双方で確認する。testのpermission付与はTooling fixtureだけ。既存のEXPENSE権限を新操作へ流用せず、実利用者のprovisioningは今回実行しない。
- Session／CSRF取得・logoutは既存を再利用する。Bearer入口・MVC／SPA画面は追加しない。C2はGETだけを追加し未認証は既存401。既存POSTのCSRF保護はB1の直接回帰で維持を確認し、C2のPOSTを新設しない。

| 操作 | 入力案 | providerと結果案 |
| --- | --- | --- |
| A：`GET /spa-api/v1/external-checks/rate` | `base`・`quote`は大文字3字、`date`はISO日付。模擬の対応pairはUSD/JPY・EUR/JPYに限定し、その他は入力拒否。業務金額へ接続しない | 固定base URIの`GET /rates`。200でoperationId・pair・date・rateを返す。rateは正の十進文字列、長さ32以下・小数点以下8桁以下。通貨対／日付の一致とbody上限を検査 |

provider URIは起動固定、数値loopbackのみ、userinfo／query／fragmentなし。requestからURLを受け付けずredirectも追わない。入力queryの総量・応答body上限は各4KiB。providerの生body／Cookie／認証情報をlogへ出さない。Aにはserver生成operationIdを付ける。固定fixture値は実レートではない。受付POST routeは追加しない。

### 7.2 Aの失敗契約とtransaction境界

| 条件 | 呼出側への応答案 | 外部試行 |
| --- | --- | --- |
| 不正入力 | 400、Reference専用ProblemDetail code | 0 |
| A：providerの確定400／404、不正・過大body | 502、`PROVIDER_REJECTED`または`INVALID_PROVIDER_RESPONSE` | 1、retryなし |
| A：503 | 選択例外`TransientRateFailure`へ変換し有限retry。枯渇時503 | 最大3 |
| A：その他5xx、429 | 503、`PROVIDER_UNAVAILABLE`／`PROVIDER_RATE_LIMITED`。429のRetry-Afterを理由に今回の内部待機を延長しない | 1、retryなし |
| A：接続／応答timeout・通信断 | 504、`PROVIDER_TIMEOUT`。今回のretry対象は503だけとし、timeout時の重い再試行を避ける | 1 |
| 同時実行枠超過 | 503、`CONCURRENCY_LIMIT` | 0 |

取消し・割込みは再試行せず、割込み状態を保持して所有資源を解放する。HTTP caller切断で取消しを検知できるとは仮定せず、Servlet非同期化を導入しない。割込み検証は所有workerに限定し、実caller切断時はI/O期限による有限解放の範囲と区別する。

AのApplicationは認可・入力検証後、transaction外でHTTPを呼ぶ。外側のtransactionが既にある呼出は外部到達前に拒否し、transactionやDB connectionをI/Oへ持ち込ませない。業務変更なしの照会なのでBusiness Auditを新設せず、operationId付きの相関logと既存Security Auditを利用する。B用のIntent／結果Audit・UNKNOWN応答・receipt契約は今回実装しない。

### 7.3 制御方式・設定案と早期停止点

| 制御 | 初期値／構成案 | 実効確認 |
| --- | --- | --- |
| HTTP方式 | Spring `RestClient`＋JDK request factory。既存Java21、追加libraryなし。HTTP Service Interfaceの導入は今回不要 | 実HTTP stubを通し、connection／body／executorの解放を確認 |
| FrameworkのHTTP既定値候補 | `koiki.api.http.connect-timeout=200ms`、`koiki.api.http.read-timeout=300ms`を既存低優先度defaultsへ追加 | Reference clientが明示採用する契約。任意の外部client全体への自動適用とはしない。既存利用者のclient挙動を一律変更しない |
| Aのretry | 既存propertyのmax-retries=2、delay=10ms、timeout=1sを使用。503変換例外だけをincludesへ指定。jitter／backoff増幅は今回なし | 初回含む最大3試行、対象外は1試行。self invocationを避けた別Beanを経由 |
| 同時実行 | Application service Beanに`@ConcurrencyLimit(limit=2, policy=REJECT)`。Aの同時照会2件。待ち行列0 | 操作全体で枠を保有しretry待機も枠内。3番目は外部到達前に拒否 |
| 時間予算 | Aのretry判断は1s、HTTP単発read300ms。provider区間の観測上限2s、test watchdog5s | retry timeoutが実行中I/Oを強制停止するとは仮定しない。接続／本文deadlineと最終試行を含む実測を別採取 |
| 設定override | HTTP read100ms・connect100msの短縮case。retry overrideは最大追加1回。concurrencyは初回2固定 | 低優先度既定値との優先関係、ゼロ／負値・過大値の起動拒否を確認 |

固定dependencyのSpring Framework7.0.9 JARを`javap`で読取確認し、ConcurrencyLimitの`policy`属性とBLOCK／REJECT、Retryableの属性、JDK factoryのread timeout setterを確認した。API存在の確認であって、動作PASSではない。公式資料：[resilience](https://docs.spring.io/spring-framework/reference/core/resilience.html)、[RetryPolicy timeout](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/core/retry/RetryPolicy.Builder.html)、[JDK factory](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/http/client/JdkClientHttpRequestFactory.html)。current資料と固定7.0.9の差は実装時にも確認する。

**早期停止点：** C2-02の最初に、固定transportの「header後の本文停止」がread期限内に終了し、connection・枠を解放できるかを実HTTPで確認する。成立しなければReference28件全体やpackage実演へ進めない。外側Futureだけをtimeoutにして裏のI/Oを残す回避はしない。transport差分の局所比較は訂正枠内に限り、追加依存が必要なら具体的な不足を提示して止める。connect blackholeを再現できない場合は接続拒否で代替せず、その枝を未実証として残す。

### 7.4 変更対象案

新規Reference mainは以下11 fileを初期案とする（全て`koiki-reference-app/src/main/java/org/koikifw/reference/externalcheck/`配下）。実装上の型分割が必要なら変更理由を示して更新し、無言で増やさない。

| 相対path | 責務 |
| --- | --- |
| `package-info.java` | module metadata・NullMarked |
| `application/ExternalCheckService.java` | 認可、同時実行枠、Aの処理順・相関 |
| `application/ExternalCheckFailure.java` | Reference内の照会失敗分類 |
| `application/dto/ExternalCheckData.java` | Application用record群。HTTP DTOと共有しない |
| `application/port/outbound/ExternalCheckPort.java` | A照会のみのApplication所有Port |
| `adapter/outbound/external/HttpExternalCheckAdapter.java` | RestClient、サイズ・応答形式検証・失敗変換・resource解放 |
| `adapter/outbound/external/RetryingRateLookup.java` | Aのretry proxy |
| `adapter/inbound/sessionapi/ExternalCheckController.java` | 明示GET 1route、専用HTTP record、入力と結果変換 |
| `adapter/inbound/sessionapi/ExternalCheckApiExceptionHandler.java` | 当該Controller限定ProblemDetail。Security例外は捕捉しない |
| `configuration/ExternalCheckProperties.java` | 有効化・固定URI・HTTP値の検査 |
| `configuration/ExternalCheckConfiguration.java` | 条件付きBean登録・client作成／終了 |

既存main変更は3 file：Frameworkの`KoikiApiDefaultsEnvironmentPostProcessor.java`と`additional-spring-configuration-metadata.json`（新HTTP値2項目の候補）、Referenceの`ReferenceSessionApiSecurityConfiguration.java`（新routeのpermission matcher）。Framework Java Public API・新依存・migration・Domain・既存expense／notificationは変更しない。新しいruntime propertyは公開契約差分なので、コード作成前に名称・意味・適用範囲を確認する。

testは新規JUnit test 5 class／30 invocation候補、stub helper1 file、文書は本書と統合計画の2 fileを初期範囲とする。概算22 file（main14＋test／helper6＋文書2）。Root Reactor・CI・POM・恒常seed・DB grant・Rule緩和は対象外。30件の内訳は次節で具体化する。

### 7.5 検証case・command案

| class案／場所 | invocation候補 | 内容 |
| --- | ---: | --- |
| `ExternalCheckContractTest`／Reference test | 8 | A正常入力1、入力拒否5、rate精度・形式1、既定値／override／不正値の設定契約1。内部比較を別件数に加算しない |
| `ExternalCheckHttpIntegrationTest`／Reference test | 13 | H01正常A、H02 503→成功、H03 400、H04 404、H05不正／過大body、H06連続503、H07接続timeout、H08 header待機timeout、H09 body待機timeout、H10 override、H11 429、H12枠超過と回復、H13割込み |
| `ExternalCheckSessionSecurityTest`／Reference test | 4 | 未認証GET401、権限不足403、許可GET到達、失効Session401。拒否はいずれもprovider到達0。既存CSRF回帰は直接回帰集合へ保持 |
| `ExternalCheckContextPostgreSqlTest`／Reference test | 3 | I01 HTTP中transactionなし、I02 caller既存transactionを到達前拒否、I03遅延HTTP中にapp所有pool接続を不要保持しないことを実DBで観測 |
| `PackagedReferenceExternalCheckTest`／既存E2E Tooling | 2 | E01同一JAR明示有効で実Session・A・DB／相関logの合流、E02同じJARの既定無効で認証済GET route404・client非起動。Bのhandler／Bean登録なし |
| **計** | **30** | Reference28＋Tooling2。最小stub helperはReference test側へ置くTooling所有fixtureとし正式JARへ含めない。Toolingからは有限stubを既存fixture構成と比較して必要部分だけ実装 |

H07が環境上再現不能ならSKIP／未実証理由を記録し、30件PASSとしない。H05等の内部枝をまとめたmethodはその比較枝を記録し、網羅確定とはしない。早期集合はContract8＋H01/H09/H12の3＋Context I01/I02の2、計13 invocationを上記30から選択する。残る17を初期早期集合へ無断追加しない。

commandは実装後の実package・method名に合わせる。PowerShellは`-D...`を各々quoteし、heapは`MAVEN_ARGS`へ入れない。

```powershell
# Reference側28件（専用class選択・資源値はtest helperの実効assertで確認）
.\mvnw.cmd -B -ntp -pl koiki-reference-app -am `
  '-Dtest=ExternalCheckContractTest,ExternalCheckHttpIntegrationTest,ExternalCheckSessionSecurityTest,ExternalCheckContextPostgreSqlTest' `
  '-Dsurefire.failIfNoSpecifiedTests=false' '-DargLine=-Xmx768m' '-DforkCount=1' `
  '-DreuseForks=false' '-Djunit.jupiter.execution.parallel.enabled=false' test
# 最終合流：同じsourceで通常rootを1回。現在の通常DB selectorを利用
.\mvnw.cmd -B -ntp '-DargLine=-Xmx768m' '-DforkCount=1' '-DreuseForks=false' `
  '-Djunit.jupiter.execution.parallel.enabled=false' `
  '-Dkoiki.reference.normal-root.db-limits.enabled=true' clean verify
```

既存直接回帰は現sourceのannotation／値集合を読取で数え、`ReferenceSessionApiSecurityTest`22＋`SessionExpenseApiControllerTest`40＋`ReferenceBusinessUrlSecurityTest`25＋`ReferenceArchitectureTest`2＝**89 invocation候補**。実XMLのmethod数・invocation数を実行時に照合し、新規30件へ加算しない。Framework defaultsの回帰は`ApiStarterSmokeTest`3＋`ApiStarterOverrideTest`1＋`ApiStarterDisabledTest`1＋`ApiResilienceDisabledTest`1＝**6件**を別集計する。どちらも今回未実行。

最終rootのJARをE01／E02へ固定SHA-256付きで渡し、runtime flagだけを変えて直列起動する。同梱SPA buildや新C2 build profileは不要。Toolingのflag案`koiki.reference.c2-journey.enabled=true`はtest選択専用とし、通常入口で2件skip・process非起動を確認する。testは既存Tooling同様repositoryから固定pathのJARを解決し、新test-only引数`koiki.reference.c2.jar.sha256`との一致を起動前後に検査する。

```powershell
# 上記root生成物を使い、再packageせずE01／E02を実行する計画command
$c2JarHash = (Get-FileHash -LiteralPath '.\koiki-reference-app\target\koiki-reference-app-0.1.0-SNAPSHOT.jar' -Algorithm SHA256).Hash.ToLowerInvariant()
.\mvnw.cmd -B -ntp -f build-support/reference-e2e-verification/pom.xml `
  '-Dtest=PackagedReferenceExternalCheckTest' '-Dkoiki.reference.c2-journey.enabled=true' `
  "-Dkoiki.reference.c2.jar.sha256=$c2JarHash" `
  '-Dkoiki.reference.verification.resource-limits.enabled=true' `
  '-DargLine=-Xmx768m' '-DforkCount=1' '-DreuseForks=false' `
  '-Djunit.jupiter.execution.parallel.enabled=false' test
```

これは未作成classに対する計画commandであり、今回実行していない。新Tooling testはJDK HttpClientの所有CookieManagerで実form login／CSRFを扱い、AのHTTP・DB接続非保持・相関logの合流を検証する。Business Audit処理は追加しない。UIのないC2で新たにChromiumを要求しない。Cookie・tokenはlog／rawへ保存せず、E01／E02のapplication／client／DB／stubは直列cleanupする。

### 7.6 新規の予算・実行資源案

| 項目 | 計画値・停止条件案 |
| --- | --- |
| 実装工数の概算 | Aのみ4〜7時間（実装＋test作成＋Evidence整理、実測ではない）。本体の方式変更や新library調査は含めない。超過時に残差を提示し、際限なく延長しない |
| 動的検証枠 | 新規105分（preflight10、compile／早期15、局所28件20、直接回帰10、最終root15、固定JAR2件15、結果照合10、cleanup10）。同じ集合の反復を加算で隠さず累積管理 |
| 再実行 | 各集合初回1回。訂正枠は合計1巡・最大20分を上記実行枠内で使用。無変更でのroot自動反復0回。追加必要時は停止し範囲・時間を見直す |
| JVM | Maven／test／application各heap768MiB。局所は最大2 JVM、実JARは最大3 JVM、直列・JUnit／reactor並列なし |
| DB／container | 今回所有の使い捨てPostgreSQL17 1件、memory1GiB／CPU1／max_connections16。局所・packageのapp pool4／idle1、client予算8。通常rootは既存pool既定を保ちclient14、Ryuk2。局所はRyuk1を基本とし、実際のfixture共有をpreflightで照合 |
| stub／thread | loopbackのみ、provider最大同時2、要求worker最大3、所有stub worker最大3、有限request台帳最大64件／case。制御外executor・HTTP接続の実数も観測し、枠2だけで総資源保証にしない |
| 時間上限 | 各HTTP test watchdog5s、fixture操作／cleanup期限を別指定。fixtureのDB取得期限はprovider待機と区別。総枠超過・通信残存・所有資源終了失敗では次集合を止める |
| raw | sanitized log合計20MiB・case台帳64件上限・個別HTTP body4KiB。秘密・生Cookie・token・全payloadを保存しない。sourcehash／設定実効値／XML／JARhash／資源／cleanupを保存 |
| 環境 | Cloud／ローカルのどちらで実行するかをその時点で記録し、実際のJDK／cache／Docker／network許可を確認。旧環境・旧S1の承認／残予算を流用しない |

開始前にsource固定と未commit範囲、HTTP既定値2項目の契約、実在class／method・command・全集合件数、利用環境と限定副作用・cleanupを照合する。C2独自のDB権限取消・新schema・本番接続は含めない。必要fixture・container・HTTP／process起動は実装開始時にこの具体範囲で判断し、実行環境の権限承認を維持する。

### 7.7 今回の結論と残る確認

1. A先行の設計をSession GET 1route・独立Reference module・Spring標準・30新規invocation候補・新規105分枠へ具体化した。Bは後続trigger成立時に別に設計する。
2. **実装前の主な確認は、Framework HTTP既定値2項目とReference新permissionの採否、予算・限定副作用の範囲。** 実装途中の早期確認でtransportの本文停止が成立しなければ、その時点で止める。
3. E2E計画commandと既存回帰89＋6件は具体化済み。残る実行前OPENは、connect timeoutの再現方式、固定transportの本文期限・停止動作、実行環境とfixtureの資源実効値。実行準備・早期確認で有限に埋め、不成立なら該当集合を未実施として止める。
4. 追加のUI／browser手動確認は現時点では計画しない。既存Session／CSRFを変える差分や利用上の問題が判明した場合だけ影響部分を選ぶ。
5. 本節は設計の具体化結果。C2-02実装、Maven／Docker／HTTP実行、commit・pushは行っていない。

## 8. Owner判断：A先行完成・Bの条件付き後続開発（2026-10-22）

Ownerは「まずAを完成させ、Bは運用機能の確定やバリエーションの必要性の発生を判断して後続で開発する」と判断した。これを現行の対象選定・順序として採用する。

- **今回の対象はAのみ。** timeout・限定retry・流量制限、相関・安全な失敗応答、通常構成保護・cleanupと利用例を完成させる。DoD4-7の評価はその実測結果へ対応付ける。
- **Bは条件付き後続開発。** 運用機能の内容が確定した時、または副作用あり操作・再送判断・provider差等のバリエーションの必要性が具体化した時に、対象・契約・検証範囲を改めて判断する。Bの実装をAの完了や次のPhase4 packageへの移行条件にしない。
- BのUNKNOWN・受理後応答喪失・Intent／結果Audit・receipt・再送保証は今回未実装／未実証として残す。必要性が生じるまで将来用code／schema／fixtureを追加しない。
- §6のA＋最小B案とその検証群は比較履歴。現行範囲は改訂後§7の11新規Reference main・既存main3・新規test／helper6・文書2＝22file案、新規30invocation候補・105分枠案とする。旧38件・120分の実行許可は存在せず、残予算として扱わない。
- 今回のOwner判断はA先行とB後続の対象・順序についてのもの。Framework HTTP property・permissionの具体契約、C2-02開始・実行予算・環境操作は未承認のまま内容確認へ進む。Aの動作成立・受入、C2／Phase4全体の完了を認定したものではない。

## 9. Aの実装・検証に対するOwner承認（2026-10-22）

Ownerは「C2-01：代表外部操作の候補比較と調査・設計範囲（2026-10-22）を確認し、Aの範囲で実装検証する内容を承認します」と明示した。§7のAのみの対象、Framework HTTP既定値候補2項目・Reference権限、初期22file・新規30invocation候補・既存直接回帰89＋6・105分検証枠と限定資源／停止／cleanupを今回の実装・検証の根拠とする。

開始時は`6fdc2a8`＋本設計・統合計画の未commit差分。承認記録反映後にsource manifest／diffを端末内へ保存し、preflightを行う。文書commitを今回の新たな前提にはしない。本文停止・connect timeoutの実行前OPENは§7の有限確認で解消し、不成立時は該当集合で停止・未実証を記録する。使い捨てDB・loopback stub・有限HTTP／所有JVM・test専用fixtureとcleanupに限定し、実行環境の権限承認に従う。

B、既存S1、実provider・新依存・新migration・新UI・CI、commit／push／PRは含めない。結果受入・DoD4-7成立・Phase4全体は結果に基づく別判断。副Agentは使用しない。

## 10. Aの実装・限定検証結果（2026-10-22）

[実装・検証結果](../architecture/validation/phase4-c2-a-implementation-results-20261022.md)へsource／artifact、失敗と訂正、集合別件数、資源・cleanup、利用入口を記録した。main14とtest／helper6は計画内、計画文書2に結果文書1を加え、成果差分は23 file。追加1 fileは所定のvalidation directoryへの結果記録で、機能追加ではない。

- 新規30候補は**29 PASS／1 SKIP**。Reference27 PASS＋H07接続timeout未実証1、固定通常JARの有効／無効実演2 PASS。既存直接回帰89＋Framework6もPASS。本文停止の早期gate、枠2の飽和／回復、DB接続非保持、実Session境界を確認した。
- H07は所有SYN blackholeがなく、接続拒否で代替せず未実証とした。DoD4-7全体の成立を認定しない。
- 通常rootは全test449 PASS／100 SKIP、15 module SUCCESS。最後の集約cleanだけが実行中logのfile lockで失敗した。証拠保全後、集約moduleだけを再開してSUCCESS。単一root commandのBUILD SUCCESSとは記録しない。全testは反復していない。
- 固定JAR SHA-256は`a6bbf763e5b00a31d86290396ebd27b05a5d10fcce3035d246d5b8985fb6b7dd`。SPA同梱や再packageなし。検証終了後の所有DB／stub／worker／子applicationはcleanup済み。
- 次は選択結果の受入範囲と残判断の確認。B・S1・新UI・実provider・commit／remoteへ進めていない。
