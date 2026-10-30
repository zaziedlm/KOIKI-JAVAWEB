# D1：同期観測の現状・適用条件・後続判断の整理

**状態（2026-10-30）：** 限定Tooling実装・訂正を経て、Owner承認の同範囲一回が全10 checkpoint PASS、cleanup・identity照合成功。Framework変更なしで限定接続の技術成立。Owner結果受入は確認待ち。[実行結果§7](../architecture/validation/phase4-d1-sync-observability-results-20261030.md#7-o02期待値訂正後の同範囲一回2026-10-30)・§16を現行入口とする。

**基点・branch：** main `b2ca253fd12842f0e0e89c827c04c6ca35aa3a29`から`docs/d1-sync-observability-plan-20261030`を作成。
D2の限定受入は[D2-03記録§7](phase4-d2-03-ci-and-adoption-guide-20261026.md#7-限定結果dod-4-11のowner受入記録2026-10-30)を参照する。

## 1. 計画初版のOwner方針と出口（見直し前の履歴）

Ownerは「D1計画からはモジュール改変の予定はありません。運用寄りに行きすぎずで、今後の実案件対応からフィードバックを得る」と指示した。
その後Ownerは、非同期連携を案件が採用しない可能性もあり、採用案件が具体化した時に設計・運用を深掘りする趣旨と補足した。
同期観測の基本整理は実案件のフィードバック待ちにせず、今回は現在の機能・適用条件・不足と後続判断を整理する。
Framework／Reference／ToolingのJava、POM、設定、CI、依存、公開API、filter順序は変更しない。
新しい収集基盤、dashboard、alert、運用手順群や独自観測APIを先行作成しない。

出口は、案件が現在の利用可能範囲と追加確認事項を理解でき、次のS1／B1／C2保証範囲整理へ進める計画文書とする。
D1S-02／03の完了は次タスクへの移行条件にせず、同期OpenTelemetryの実装・実証完了にも読み替えない。

## 2. 現行実装と証拠の範囲

以下は基点sourceの静的確認と既存D2結果の照合である。今回、新たな起動・通信・負荷・収集先接続は実行していない。

| 対象 | 現在提供するもの | 境界・不足 |
|---|---|---|
| [Observability Starter](../../koiki-starters/koiki-starter-observability/README.md) | Boot構造化console log、service／environment付加、Servlet requestId、Actuator healthの低優先度既定 | exporter・cloud backend・SecurityContext accessor・独自HealthIndicatorは含まない |
| [相関filter](../../koiki-starters/koiki-starter-observability/src/main/java/org/koikifw/starter/observability/internal/KoikiCorrelationFilter.java) | `X-Request-ID`を安全文字・長さで検証し、不正／不在ならUUID。MDC `requestId`をfinallyで復元／削除 | requestIdはtraceId／spanIdとは別。認証・認可の根拠に使わない。Security前段拒否・ERROR dispatch全経路の相関は保証しない |
| [AutoConfiguration](../../koiki-starters/koiki-starter-observability/src/main/java/org/koikifw/starter/observability/internal/KoikiObservabilityAutoConfiguration.java) | requestIdのContext Propagation用TaskDecorator Bean。既定有効、無効化propertyあり | Beanの存在だけで全executorへの適用やSecurityContext伝播を保証しない |
| [既定設定](../../koiki-starters/koiki-starter-observability/src/main/java/org/koikifw/starter/observability/internal/KoikiObservabilityDefaultsEnvironmentPostProcessor.java) | healthのみWeb公開、component／status表示、detail非表示、probe有効の低優先度既定 | endpoint公開の認証・到達制御、readinessに含める依存はApplication側判断。metrics endpointの外部公開は今回追加しない |
| [Starter POM](../../koiki-starters/koiki-starter-observability/pom.xml) | Actuator・Micrometer context-propagationの依存 | それだけでtrace exporterやmetric backendへの配送成立とは扱わない |
| Reference／D2観測 | 通常ReferenceはObservability Starterを含まず、D2では実Starter・runtime依存を外付けして詳細観測 | D2の観測fixtureやHikari samplerを案件用の標準監視機能へ昇格させない。案件は必要なStarter依存を明示する |

## 3. 案件が利用・確認する観点

案件の主要な同期Web要求を起点とし、下表から実際の要求に該当するものを選ぶ。全項目の新規実装・実証を今回の必須作業にしない。

| 観点 | 既存材料で確認できること／案件へ確認すること | 現時点の限界 |
|---|---|---|
| 正常要求 | どのHTTP経路・業務操作か、requestIdで対象ログへ辿れるか | 業務ログやBusiness Auditとの対応はApplication所有。全処理区間のspan生成は未実証 |
| 認証・認可拒否 | HTTP結果、経路、時刻、既存Security Audit等を照合する | 早期拒否では相関IDがない場合がある。IDだけを唯一の調査手掛かりにしない |
| 遅延・DB待機 | 応答時間・pool待機・timeout等、案件で必要な診断材料を挙げる | D2の有限負荷は本番SLOや常設metric提供の保証ではない |
| 失敗・ERROR dispatch | 応答statusと発生箇所を区別する。D2ではMVC内500とSession読取り例外経由403が混在した | 全失敗の5xx統一・全dispatchの相関は未保証。今回その挙動を修正しない |
| Java25 VT | VT有効／無効、JDK版・依存・対象操作を明記する | D2の選定同期経路以外や独自executorは個別確認。S1非同期・Batchの検証へ流用しない |
| trace／metric収集 | 既存の案件側収集方式、必要なtrace区間・metricと未取得項目を提示してもらう | tracer／exporter／sink接続・配送・sink障害時の影響は今回未実証 |

## 4. 提供側と案件側の分担

Framework側は現在のStarterの契約・既定値・既知制限と、共通要求が得られた場合のgap判断を所有する。
Application側はStarter採用、業務ログ・Auditとの対応、独自executor、依存・設定の組合せと案件固有の検証を所有する。
収集先・閲覧権限・保持期間・alertの担当と閾値は案件／platform側で決め、Frameworkの初期標準として固定しない。

既存のhealth公開既定を超えるendpoint公開や、秘密・個人情報・request body・認証情報のログ追加を本計画で推奨しない。
requestId／user ID等を無制限なmetricラベルへ追加しない。sampling率・送信queue・timeout・容量は、実際の収集構成を採用する時に必要範囲で判断する。
今回、共通の数値や運用サービス水準を先行して約束しない。

## 5. 今後の具体化と非同期採用時の深掘り

同期の標準利用条件は本書で整理する。実案件の相談は不足を具体化する入力であり、同期観測全体の着手条件にはしない。
非同期連携は全案件への採用を前提にせず、採用案件が具体化した段階で、配送・復旧・相関・停止／再開等の設計と運用を必要範囲で深掘りする。
新しい常設台帳や申請システムは作らず、既存の相談・issue等で次の材料を扱う。

- 利用するFramework基準版／commit、JDK／Boot、Starter・独自executor等の構成。
- 調べたい一つの同期処理、期待する診断と現在できないこと、業務影響・必要時期。
- 秘密を除いた再現条件・HTTP結果・時刻・利用可能な相関ID／既存ログ、既存収集方式の有無。
- 案件設定で解決できるか、Framework共通契約・既定設定の不足かを判断するための条件。

まず既存機能・標準設定の利用を確認し、案件固有の設定・連携は案件側で扱う。
同期の未実施接続については、標準機能としての必要性・実案件要求・共通契約の不具合等を入力に、対象・Ownership・依存／設定差分・必要検証を限定して後続案を作る。複数案件からの要求集約を必須条件にはしない。
共通化の要否が未確定のまま、モジュール改変や観測基盤を追加しない。

## 6. 初版時点のWBS・後続タスク（現行は§8〜13）

| ID／対象 | 今回の状態・扱い | 再開時に決めるもの |
|---|---|---|
| D1S-01 | 本書による現状・適用条件・不足・案件フィードバック計画の整理初版。Owner確認対象 | 案件から得た具体的要求に応じた追記 |
| D1S-02 | tracer／exporter接続は未実施、今回対象外。後続の必要性を判断 | 採用構成、依存・property、Framework／案件の責任分担と必要変更 |
| D1S-03 | 相関・context非漏えい・sink障害実演は未実施、今回対象外。後続の必要性を判断 | 実対象・合格条件、資源・時間上限・cleanup・証拠 |
| 同期OpenTelemetry成果物 | 原計画の対象として台帳に残す。計画整理だけで成立にしない | 実装・接続実証の必要性と開始判断 |
| 非同期DoD 4-12／S1 | 既存未達・人手HOLD条件を維持。D1整理では閉じない | 後続の保証範囲・DoD判定で既存Evidenceと対応付け、非同期採用案件の具体化時に設計・運用を深掘り |

今回の文書を確認後、[統合計画§9](phase4-review-integrated-task-plan-20261020.md#9-初期標準確立に向けた見直し推進計画owner-approved2026-10-23)の次段階へ進む。

1. S1残課題とB1／C2の保証範囲・DoD判定：既存結果・利用条件・未達・後続課題を整理し、追加機能を抑制する。
2. E1：初期提供版、JAR・Parent／BOM、取得・参照取り込み・更新方法を確立する。対象構成のEntity scan／Flyway等を必要範囲で確認する。
3. D3：frontend配置を含むJava containerのbuild・起動・health・停止を確認し、localと実ECSを分ける。
4. C3／C4、B2、MyBatis・SAML等：既存の要件具体化・採用条件を維持する。

## 7. 初版整理時の確認と保留（履歴）

確認はsource／POM／既存結果の静的照合、文書の差分・参照先・状態表記の点検に限る。
モジュール・CIを変えないため、Maven／Docker／新規CI・収集先通信の再実行は行わない。
計画整理の完了とOwner確認、D1接続の技術成立、正式release・本番運用成立は別に扱う。
commit・push・PR・merge・required check変更は今回の文書作成に含まない。

## 8. 限定接続案への見直し判断（2026-10-30）

§8〜13は実行前の計画具体化記録。実装・訂正・実行の推移は§14〜16、現行結果は§16を参照する。

Ownerは、案件／検証側の依存・設定で接続可能かを先に調べ、Framework側の変更が必要なら理由と最小差分を提示する進め方を了承し、限定接続案の具体化を指示した。
§1の「モジュール改変なし」は計画初版時点の方針として保持し、現行の実装候補・判定範囲は§8〜13に従う。
今回の指示で行うのは静的調査と計画訂正まで。以下のTooling作成・起動・動的検証はまだ行っていない。

**静的調査の結論：** Boot 4.1.1の標準auto-configurationを検証アプリ側で有効化する案が成立候補。
現時点でFramework側変更を必須とする根拠はなく、実接続の成立はD1S-02／03で確認する。
KOIKI固有のtracer／exporter APIやfilter変更を先行追加しない。

### 8.1 照合した一次資料と条件

リポジトリBOMと、Maven CentralのBoot **4.1.1**配布POM／source JARを照合した。
静的取得物は`/tmp/koiki-d1-static-review/`に保持した一時資料で、動的検証の結果ではない。

| 入力 | 確認したこと・採用判断 |
|---|---|
| [Boot BOM 4.1.1](https://repo.maven.apache.org/maven2/org/springframework/boot/spring-boot-dependencies/4.1.1/spring-boot-dependencies-4.1.1.pom) | Micrometer Tracing 1.7.1、OTel 1.62.0を管理。既存KOIKI BOM経由の管理を利用し、Framework BOMを変更しない |
| [Tracing OTel POM](https://repo.maven.apache.org/maven2/org/springframework/boot/spring-boot-micrometer-tracing-opentelemetry/4.1.1/spring-boot-micrometer-tracing-opentelemetry-4.1.1.pom)／[source](https://repo.maven.apache.org/maven2/org/springframework/boot/spring-boot-micrometer-tracing-opentelemetry/4.1.1/spring-boot-micrometer-tracing-opentelemetry-4.1.1-sources.jar) | OTel tracer bridge・SDK等の条件付き構成、OTLP exporterのclasspath条件、endpoint・transport・timeout・batch propertyを確認 |
| [OTel共通POM](https://repo.maven.apache.org/maven2/org/springframework/boot/spring-boot-opentelemetry/4.1.1/spring-boot-opentelemetry-4.1.1.pom) | 共通OTel API／SDK。これだけでHTTP観測やexportが成立するとは扱わない |
| [Starter OTel POM](https://repo.maven.apache.org/maven2/org/springframework/boot/spring-boot-starter-opentelemetry/4.1.1/spring-boot-starter-opentelemetry-4.1.1.pom) | tracingに加えOTLP metric registry等を含むため、今回は下表のtraceに限定した依存を選ぶ |
| [Micrometer Tracing source](https://repo.maven.apache.org/maven2/org/springframework/boot/spring-boot-micrometer-tracing/4.1.1/spring-boot-micrometer-tracing-4.1.1-sources.jar) | `management.tracing.sampling.probability`、`management.tracing.export.otlp.enabled`等を確認。Boot3時代のOTLP endpoint名を転用しない |
| [OTLP Java model POM](https://repo.maven.apache.org/maven2/io/opentelemetry/proto/opentelemetry-proto/1.11.1-alpha/opentelemetry-proto-1.11.1-alpha.pom) | 受信側test専用のprotobuf decode候補。公開座標の存在・protobuf-java依存を確認。alpha表記のこのmodelをFramework／検証appの正式依存にしない。Boot管理protobufとの実効版・API整合はpreflightで確認 |

照合sourceのSHA-256：Tracing OTel `c66aa521d74e94dae0c48ccad371de2258ef6c4b403c8d3755dc04d6aa0121da`、
Micrometer Tracing `1f19fd474cd84d2c3502c4a5f92c5e0878233f4955996cb7a6ac823436519e19`。
HTTP server observationとKOIKI requestIdの同居・traceId／spanIdのMDC相関はruntimeで確認する事項であり、source照合だけで成功としない。

## 9. 案件／検証側だけで組む接続構成

独立Tooling reactor `build-support/sync-observability-verification/`を候補とする。
Root Reactor／publish unitや通常Referenceへ追加せず、案件が依存・設定を選ぶ構成を小さなconsumer appで再現する。

| 所有・構成 | 依存・役割 |
|---|---|
| consumer app | KOIKI Parent／BOM、既存`koiki-starter-observability`、Boot `spring-boot-starter-webmvc`。Java21でpackage。DB・業務module・S1依存なし |
| consumer appのtrace追加 | `org.springframework.boot:spring-boot-micrometer-tracing-opentelemetry`、`io.micrometer:micrometer-tracing-bridge-otel`、`io.opentelemetry:opentelemetry-exporter-otlp`。版はKOIKI経由のBoot BOMに従い、推移依存・重複を照合する |
| harness／受信fixture | JUnit、JDK HttpServer、test専用`io.opentelemetry.proto:opentelemetry-proto:1.11.1-alpha`。OTLP HTTP/protobuf `/v1/traces`をdecodeし、成功／503／遅延の応答を制御する |
| 通信境界 | app・受信fixtureとも`127.0.0.1`の空きportを束縛し記録。認証情報・外部collector・cloud backend不要 |

受信fixtureはexportされたspanの内容を照合するための試験用endpointで、正式Collector製品・保存backendの接続実証ではない。
OTLPのprotobufとしてdecodeし、resource／scope／span、traceId／spanId、HTTP結果を検査する。HTTP POSTを受けただけではPASSにしない。
OTLP JSON送信を仮定した独自decoderや、架空のspanを生成する代替exporterは用いない。

appのtest専用経路は正常`/d1/ok`・500応答`/d1/fail`の二つを基本とし、handlerで現在のMicrometer span・MDCと安全なtest markerを記録する。
独自のspan生成やFramework interceptorを追加して標準HTTP観測の不足を隠さない。
このappはloopback限定の非配布fixtureであり、正式な認証・認可・Business Auditの実証、業務操作・DB trace・外部HTTP client spanは含まない。
Security前段拒否・ERROR dispatch全経路の相関という既知制限は別に残す。

### 9.1 候補の設定値

```properties
server.address=127.0.0.1
server.port=0
spring.application.name=koiki-d1-consumer
spring.threads.virtual.enabled=false
management.tracing.sampling.probability=1.0
management.tracing.export.otlp.enabled=true
management.opentelemetry.tracing.export.otlp.endpoint=http://127.0.0.1:<receiver-port>/v1/traces
management.opentelemetry.tracing.export.otlp.transport=http
management.opentelemetry.tracing.export.otlp.compression=none
management.opentelemetry.tracing.export.otlp.connect-timeout=300ms
management.opentelemetry.tracing.export.otlp.timeout=500ms
management.opentelemetry.tracing.export.timeout=1s
management.opentelemetry.tracing.export.schedule-delay=200ms
management.opentelemetry.tracing.export.max-queue-size=64
management.opentelemetry.tracing.export.max-batch-size=16
```

`<receiver-port>`は起動時に割り当てる値で、上記はそのまま配布する設定fileではない。
sampling 100%・短い期限・小queueは有限検証用で、本番推奨値にはしない。
管理endpointのWeb公開は既存health既定のまま。OTLP logging／metrics exporterやbaggage remote fieldを追加しない。
依存解決後に実効classpathと条件評価を確認し、意図しないexporter・外向き送信が見つかれば開始前に訂正する。

## 10. 限定動的検証と合格条件

同一Java21生成app JARで、主検証はJava21／VT無効、補足smokeはJava25／VT有効とする。
export無効対照→有効主検証→VT smokeの計3子processを順次起動し、並走させない。Java25再compileなし。
主8＋smoke2＝**10論理checkpoint**を予定する。JUnit method／invocation数とは区別し、実装時に対応表を保存する。

| ID | 主検証Java21での合格条件 |
|---|---|
| O01 接続・正常 | 正常200、標準server spanが実OTLPで受信・decodeできる。resourceのservice識別とhandlerのtraceId／spanId・requestIdが対象ログに対応する |
| O02 失敗 | 制御した500応答のserver spanとログが同じtraceへ対応し、正常要求の結果と区別できる。error status／HTTP attributeの実効名は採用版で照合し記録 |
| O03 要求分離 | 固定の異なるW3C traceparent・requestIdを持つ逐次要求と並行4要求が、それぞれの期待traceへ対応し、他要求のIDを取り込まない。無入力の後続要求も新規IDとなる。全threadの非漏えい保証には広げない |
| O04 export無効対照 | 同一JARを`management.tracing.export.otlp.enabled=false`で実行し、正常HTTPを維持。実効設定を確認し、有限観測窓3秒で受信0。tracerまで無効になることは要求しない |
| O05 sink 503 | fixtureが5秒間503を返す間、上限8要求がそれぞれclient deadline3秒以内に正常終了。export失敗を観測し、appを停止・異常終了させない。exporter内部の試行は実数を記録し無限に待たない |
| O06 sink遅延 | fixtureは受信後2秒遅延、上限8要求。export timeoutを観測し、app要求はclient deadline3秒以内に終了。実測時間・失敗を記録し、本番性能SLOへ読み替えない |
| O07 sink回復 | fixture正常化後10秒以内に新規正常要求のspanを受信。障害中spanの全件再送・永続配送・欠損ゼロは要求せず、保証もしない |
| O08 証拠・終了 | source／JAR同一性、fixtureの受信内容・ログ・判定を保存。合成canaryで認証情報・request bodyの非出力を点検し、所有app／HTTP server／executorの終了と残存0を確認 |

Java25／VT有効smokeはO01とO03を同じJARで確認し、handlerの`isVirtual()`も記録する。
D2の業務・DB pool・全負荷比較を再実行せず、非同期DoD 4-12へ成功を転用しない。

canaryは実secretを使わず、ログ・OTLPの対象全件を検査する。payload非露出・source一致・cleanupが不成立なら、他checkpointが成立しても全体をPASSにしない。
証拠は有限のrun別ディレクトリに保持し、生の環境変数・認証header・request bodyを診断へ出力しない。

## 11. 実装候補file・preflight・実行上限

実装候補は独立Toolingのroot／app／harness POM、app main／controller／設定、harness test／OTLP receiver／process helper、README、manifest生成・照合scriptの**最大12 file**。
正確なfile一覧・case対応・依存treeは実装開始時に確定し、Framework／Reference・公開API・CIを変更しない。

preflightでは以下を確認する。

1. 基点commit・dirty差分とそのhash、JDK21／25・Maven、対象Starter JAR・consumer app JAR・実効runtime依存を固定する。未commitならdirty=trueと記録し、clean commit実績に見せない。
2. Root／正式moduleのPOM・Java・設定が不変で、Toolingが通常reactorへ混入しないことを確認する。
3. BOM管理版・proto側protobuf依存・実効classpathに衝突がなく、BootのHTTP observation／tracer／OTLP exporterの条件が揃うことを確認する。
4. loopback portの所有、受信cap・timeout、期限付き子process停止とcleanup経路を確認する。

| 資源・時間 | 上限案 |
|---|---|
| JVM | Maven1＋harness1＋app1、各heap最大512MiB。appは一つずつ起動 |
| DB／Docker／browser | 0。外部provider・cloud serviceへの実行時通信0 |
| 負荷 | HTTP総数100以内、並行4以内。受信OTLP request100以内、decoded span500以内、request body1MiB以内 |
| 保存 | app log・OTLP・集計等の合計32MiB以内。超過は停止して不成立を記録 |
| 枠 | preflight・依存／package15分、初回検証10分、診断整理10分、cleanup5分＝40分以内 |
| 失敗・再実行 | 初回一回。途中失敗は資源を終了して証拠・理由を記録。範囲を変えた自動再実行はしない |

今回の資料取得は上記動的実行枠に含めず、既存S1・D2の実行予算を流用しない。
processはPID・起動時刻・固定JAR引数とrun IDで所有確認し、harnessのfinallyと外側の有限cleanupを用意する。
外部のprocessを一括停止しない。JDK HttpServer・遅延handler・executorにも終了期限を設け、残存はFAILとして記録する。

## 12. Framework側変更の要否を判断する分岐

| 調査・検証結果 | 対応 |
|---|---|
| consumer側の依存・設定だけで成立 | Framework変更0の結果として記録し、案件向けに必要依存・property・限界を案内する |
| Boot設定・classpath・fixture不備 | consumer／Tooling側を訂正。Framework不具合へ読み替えず、失敗epochを保持する |
| KOIKI共通契約が標準HTTP観測を妨げる | 原因と最小再現、期待／実際、対象Framework file・最小差分、既定挙動・公開API・直接回帰への影響を提示する。今回先行修正しない |
| Security前段拒否・非同期・DB等の対象外不足 | 既知制限／後続課題へ残す。限定接続の成功条件を黙って拡張しない |

現時点ではFramework変更案は0件。これを「不要と実証済み」とは扱わない。

## 13. この具体化の出口と次の実行判断

D1S-01の現行成果は§8〜12の限定接続案。依存候補・設定・10 checkpoint・資源枠・変更分岐をOwner確認対象とする。
D1S-02はconsumer側接続実装、D1S-03は上記有限実証へ具体化したが、いずれも未着手。
最初の実行結果から成立・不足を判定し、Framework変更が必要な場合は§12の理由・最小差分を検討する。

同期OpenTelemetry成果物の認定は実際に成立した範囲に限定する。受信fixtureだけの成功で正式Collector・本番運用の完成とはしない。
非同期の配送・復旧・運用設計の深掘りは、採用案件が具体化した時に必要範囲で行う方針を維持する。
commit・push・PR・merge・CI実行・required化は、この計画具体化に含まない。

## 14. 限定実装・初回実行と停止（2026-10-30）

Ownerは§8を確認し「これに従って進めましょう」と指示したため、D1S-02／03の限定実装・初回一回を実行した。
Toolingは独立reactor11 file。Framework／Reference・Root Reactor・CIに変更なし。
preflightでOTLP model1.11.1-alphaとBoot管理protobuf4.35.1の差を検出し、harnessのtest依存だけprotobuf4.36.2へ固定した。
Java21 package／test-compile、protobuf linkage・binary roundtrip、source／JAR固定が成立した。

初回はJUnit1 invocation・0 failure／1 error／0 skip、O04だけPASS、残9 checkpoint未到達。
export無効のJava21 childを終了する際、Toolingのlog readerが正常終了中のstdout pipe閉鎖をERRORとして扱った。
child／receiverの終了と外側cleanup・identity照合は成立し、予期しない所有child残存0。
helperを訂正してcompileのみ確認し、初回一回の条件に従って自動再実行を停止した。

詳細・固定hash・初回証拠・訂正差分・同範囲追加一回の再開案は[実行結果](../architecture/validation/phase4-d1-sync-observability-results-20261030.md)を参照。
export有効接続・OTLP実受信・sink障害・VT smokeは未検証であり、Framework変更不要と実証済みにはしない。
初回epochは保持し、再開判断後は訂正後sourceの新manifestで対照から全10項目を確認する。
今回の開始指示をcommit・remote・追加実行の承認へ拡張しない。

## 15. 追加一回の結果とO02期待値の訂正（2026-10-30）

Ownerはprotobuf版の整理を了解し、終了helper訂正後の同範囲追加一回を承認した。
同じconsumer JAR・依存・設定で新manifestを固定して実行。O04・O01はPASS、O02でFAILし、残7 checkpointは未到達。
正常同期HTTPのOTLP export・decode・構造化log相関、無効対照、終了helper訂正の成立を追加epochで確認した。
両child・receiver cleanupとsource／JAR照合は成功した。

O02のfixtureは例外なしのHTTP500応答。採用Micrometerではstatus="500"・outcome="SERVER_ERROR"・exception="none"がspan属性に付き、span statusはUNSETだった。
HTTP500をOTel span ERRORと同一視したharness期待値を、上記実効属性とHTTP／trace／logの一致を確認するものへ訂正した。
§10 O02の「error status／HTTP attributeの実効名は採用版で照合」の具体値はこの組合せとする。Java例外を投げる経路・全失敗経路のERROR統一は対象外。
Framework／consumer JAR・依存・設定・10項目の件数・資源上限は変えない。

訂正後package／test-compile成功、動的再実行なし。[結果§6](../architecture/validation/phase4-d1-sync-observability-results-20261030.md#6-訂正後の同範囲追加一回2026-10-30)に証拠・静的診断・同範囲再度一回の判断案を記録。
追加一回の枠は消費済みで自動再実行しない。初回・追加の失敗履歴を保持し、全10項目・D1受入は未成立のままとする。

## 16. O02訂正後の全10項目成立（2026-10-30）

OwnerはO02期待値訂正後の同範囲一回を承認した。新しいsource／manifestの単独実行で、Java21無効対照→Java21有効→Java25 VTの全3 child、10 checkpointがPASS。
JUnit1 PASS／0 failure／0 error／0 skip、Maven21.999秒、cleanup・source／JAR同一性成立。
consumer JAR・依存・設定・checkpoint・資源上限は前回と同じ。初回・次回の部分成功は合算せず、失敗記録を保持する。

[結果§7](../architecture/validation/phase4-d1-sync-observability-results-20261030.md#7-o02期待値訂正後の同範囲一回2026-10-30)にJDK・hash・要求数・証拠・限界を記録した。
D1S-02／03の今回限定範囲は技術成立、Owner結果受入は未記録。Framework改変なしでconsumer側依存・設定による同期HTTP traceの構成可能性を確認した。
正式Collector・backend・本番SLO、metric／log export、DB／外部client span、認証・拒否・全dispatch／全executorは今回の保証対象に含めない。
非同期DoD 4-12・配送／復旧・運用設計の深掘りは別のまま。結果確認後はS1／B1／C2の保証範囲・DoD整理→E1→D3の優先順を維持する。
今回の実行承認にcommit・push・PR・releaseは含めず、次は結果とGit差分・成果固定対象の確認。
