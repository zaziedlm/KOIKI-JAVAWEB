# D1同期観測：限定接続実装・検証結果（2026-10-30）

**状態：** O02期待値訂正後、Owner承認の同範囲一回で全10 checkpoint PASS、JUnit1 PASS／0 failure／0 error／0 skip、cleanup・identity照合成功（§7）。D1の限定範囲は技術成立、Owner結果受入は確認待ち。初回・次回の失敗は履歴として保持。

## 1. 開始根拠・source・範囲

Ownerは[D1計画§8〜13](../../development/phase4-d1-sync-observability-plan-20261030.md#8-限定接続案への見直し判断2026-10-30)を確認し、
「8. 限定接続案への見直し判断（2026-10-30）を確認しました。これに従って進めましょう」と指示した。
consumer側の標準依存・設定を先行し、Framework側変更が必要なら理由・最小差分を示す範囲で着手した。

- Branch：`docs/d1-sync-observability-plan-20261030`、基点commit `b2ca253fd12842f0e0e89c827c04c6ca35aa3a29`。
- sourceは未commitの文書・Tooling差分。manifestの`dirty=true`を維持し、clean commit実績にしない。
- Tooling：[sync-observability-verification](../../../build-support/sync-observability-verification/README.md)、独立reactorの11 file。Root Reactor・publish unitへ追加なし。
- Framework／ReferenceのJava・POM・設定・公開API・CI変更なし。正式Collector、DB、Docker、browser、外部providerの起動なし。

## 2. 実装とpreflight

consumer appに既存KOIKI Observability Starter、Boot WebMVC、標準OTel tracing module／bridge／OTLP exporterを依存として追加した。
自前span・exporter・相関filterを作らず、HTTP server observationの現在spanとMDCをhandlerで確認する。
検証用OTLP receiverはJDK HttpServerとprotobuf modelで実payloadをdecodeし、正常／503／2秒遅延を制御する実装。
consumerのtest経路はloopback限定で、正式な認証・認可・業務・Audit契約は含まない。

preflight結果：

- Boot4.1.1／Micrometer Tracing1.7.1／OTel1.62.0の実効依存を確認。consumerへのOTLP metric registry追加なし。
- test用OTLP model1.11.1-alphaの要求protobuf4.36.2が、Boot BOMで4.35.1に解決されることを検出。
  harnessの**test依存のみ**protobuf-java4.36.2を明示し、modelのbinary roundtrip・linkageをJava21で確認した。Framework／consumerのBOMは変更なし。
- Java21 package／test-compile成功。preflightは約8分で15分上限内。compile時点では専用HTTP検証を実行していない。
- consumer class major65、JAR内のStarter payloadと既存Starter JARのSHA一致を確認。

| 固定物 | SHA-256 |
|---|---|
| 初回consumer app JAR | `eec79e836e5a84a8d56aa92a8171751b616f0202ceb534aabd5347b923b6e63f` |
| 既存Observability Starter JAR | `e14cf9bb04c2054537fa4e3f98c93c0fe362e69b7a4b65b2d149cf2f144b72c8` |

Starterは既存D2で生成・登録済みの不変artifactを利用。新しいFramework build実績とは扱わない。
manifestはTooling source hash、Git commit／tree・dirty状態、tracked diff hash、owner ID、JAR hash・runtime JAR一覧を記録する。

## 3. 初回一回の結果

2026-10-30 **11:20:05 JST**にMaven終了。専用Maven7.442秒、JUnit6.296秒。
JUnitは**1 invocation／0 failure／1 error／0 skip**。論理checkpointは**O04のみPASS、残9項目は未到達**。

| 対象 | 結果 |
|---|---|
| Java21 export無効対照／O04 | HTTP200、export=false、exporter Bean0、VT=false、3秒観測窓でOTLP受信0。handlerのspan／MDC・応答相関も検査を通過 |
| 同child終了 | `D1Process`のlog readerが`Process.destroy()`に伴うstdout pipeの`IOException: Stream closed`を異常と判定しERROR |
| Java21 export有効／O01〜03・05〜07 | 未起動・未到達。実OTLP配送・decode・sink障害／回復は未検証 |
| Java25 VT smoke／VT-O01・03 | 未起動・未到達 |
| O08 | 未成立。run全体の成功・証拠／終了checkpointには到達していない |
| 実行量 | HTTP要求1、OTLP要求0。実行時childはJava21一つのみ |
| 外側cleanup・identity | cleanup command0／verify command0。予期しない所有child0、所有PIDの消滅・receiver executor終了記録を確認。初回source snapshotと固定JARも照合 |

終了helperの判定不備であり、FrameworkのOTel接続可否・不具合の判定根拠にはしない。
O04成功をexport有効・全10項目・同期OpenTelemetry成果物・DoD 4-12の成立へ広げない。

## 4. 証拠・cleanup

保存先：`/workspace/koiki-cloud/logs/d1-20261030/`。

- `package-initial.log`／`package-harness.log`／`package-final.log`、dependency tree、`protocol-preflight.log`。
- `initial-execution/artifact-manifest.json`、`artifacts/`、`maven.log`、`surefire-reports/`、`resource-cleanup.json`。
- `initial-execution/evidence/`のcheckpoint、disabled ready／ownership、bounded app log、応答、run-state、receiver-cleanup。
- `initial-execution/sources/`へ初回11 fileを退避し、manifestの全source hashとの一致を確認。

初回evidenceは約5.4KiBで32MiB上限内。合成canary以外のsecretは使用していない。環境変数の全出力・実認証情報の記録なし。
終了helperはERRORを返したが、childは既に終了しており、receiverもfinallyで停止した。外側cleanupで強制停止した残存childは0。
build生成物・証拠・JARはGitへ追加しない。source退避は初回の履歴で、現在の訂正済みsourceとは区別する。

## 5. Tooling訂正・再開判断

`D1Process.close()`の所有child終了開始をflagで識別し、readerの`readLine()`だけに発生した
正常終了中の`Stream closed`を許容するよう訂正した。
それ以外の読取失敗、log上限超過、canary検知は引き続きERRORとし、終了確認・reader joinを維持する。
Framework・consumer・OTLP判定・checkpoint・資源上限は変えない。

訂正後のJava21 package／test-compileは成功（`package-helper-correction.log`）。**動的再検証は行っていない**。
初回manifestは訂正前sourceへ帰属するため上書き・流用せず、再開時には新しいrun directory・source manifestを作る。

再開案は、同じJAR・依存・設定・資源上限でexport無効対照から全3 child／10 checkpointを追加一回実行し、途中成功を合算しないこと。
consumer JARが変わっていないことを照合し、初回結果は保持する。
[D1計画§11](../../development/phase4-d1-sync-observability-plan-20261030.md#11-実装候補filepreflight実行上限)の「初回一回」に従い自動再実行を停止した。
追加一回の実行判断後、40分枠の消費・残時間と新しいsource／artifactを再確認する。

commit／push／PR・CI実行なし。D2の受入済み範囲・非同期の後続判断は変更しない。

## 6. 訂正後の同範囲追加一回（2026-10-30）

Ownerはprotobuf版の説明を了解し、「戻して、訂正後、同じ範囲で追加1回の検証を実行に進めます」と指示した。
初回とは別の`corrected-execution/`でmanifest・source・証拠を固定し、対照から全3 child／10 checkpointを一回実行する。
consumer JAR hashは初回と一致、依存・設定・合格条件は不変。Tooling差分は終了helper訂正とREADMEの履歴追記だけ。
初回実働は作業開始から結果・案内更新まで約12分（応答待ちを除く）。追加実行上限10分・cleanup5分と結果整理を40分の実働枠内で扱う。
時刻上の経過にはOwner応答待ちを含むため、実働・待機を区別して記録する。初回失敗は上書き・成功合算しない。
追加実行は下記のとおり完了しFAIL。初回・追加双方の証拠を保持する。

### 6.1 実行結果と成立した範囲

2026-10-30 **11:52:24 JST**に追加実行終了。専用Maven14.675秒、JUnit12.71秒。
**JUnit1 invocation／1 failure／0 error／0 skip。O04・O01がPASS、O02でFAIL、残7項目は未到達**。
初回のO04等を合算せず、追加epochだけの結果として記録する。

| 対象 | 追加一回の結果 |
|---|---|
| 終了helper訂正 | Java21無効対照の終了を正常に通過。有効側まで進行。両childの終了・reader joinも成功 |
| O04 export無効対照 | PASS。HTTP200、3秒観測窓で受信0 |
| O01 正常接続 | PASS。実OTLP HTTP/protobufをdecodeし、resource service名、SERVER span、HTTP handlerのtraceId／spanId／requestIdと構造化logの一致を確認 |
| O02 HTTP500 | HTTP500応答と、同じtrace／spanのOTLP・logまでは取得。検証が要求したspan `STATUS_CODE_ERROR`に対して実値`STATUS_CODE_UNSET`でFAIL |
| O03・O05〜08、VT-O01・03 | 未到達。要求分離・sink障害／回復・全体証拠checkpoint・VT smokeは未成立 |
| 実行量 | HTTP3要求、OTLP2要求、decoded server span2件。Java21 childを二つ順次起動、Java25 childは未起動 |
| 資源・同一性 | child2件・reader・HTTP server／executor終了、外側cleanup成功、予期しない所有child0。source／JAR照合成功。保存evidence15,572 bytes |

protobuf model／runtimeの版固定は、今回の実OTLP payload decodeでも成立した。test側のみの例外で、Framework BOMの変更・本番依存の採用認定とは区別する。
consumer側の標準依存・設定で正常同期HTTPの実OTLP exportが成立した範囲は確認できたが、全10項目の成功・D1受入にはしない。

### 6.2 O02の診断・期待値訂正

controllerは例外を投げず`ResponseEntity`で500を返すfixtureである。
実payloadの属性は`status="500"`、`outcome="SERVER_ERROR"`、`exception="none"`で、span statusはUNSET。
正常側のpayloadは`status="200"`、`outcome="SUCCESS"`であり、HTTPの失敗結果自体は識別できる。

採用JARの`PropagatingReceiverTracingObservationHandler.onError`はObservation contextにerrorがある時だけ`Span.error`を呼ぶ。
`TracingObservationHandler.tagSpan`は観測key/valueをspan tagへ写し、`OtelSpan.error(Throwable)`は例外を記録しOTel ERRORを設定する。
今回の例外を伴わない500応答は、HTTP属性のSERVER_ERRORとOTel span statusを同一視できない。
受信spanを確認せずOTel semconvの属性名・ERROR値を必須にした検証側の期待値が不適切だった。

ToolingのO02を、**実HTTP500＋同traceのlog／OTLP、status="500"、outcome="SERVER_ERROR"、exception="none"、span status UNSET**の確認へ訂正した。
HTTPエラー判定を省略したり、任意の値をPASSにしたりする変更ではない。例外発生時のERRORや全失敗経路は今回のfixture範囲外として残す。
業務・案件でspan ERRORへの統一が必要なら別要求として判断し、Framework側へ付加処理を先行追加しない。

訂正後のJava21 package／test-compile成功（`package-status-expectation-correction.log`、3.902秒）。動的再検証は未実施。
consumer JAR・依存・起動設定・主8＋VT2の件数・資源上限は不変。訂正はharness期待値のみで、Framework／consumer変更は不要。

### 6.3 証拠と次の判断

`corrected-execution/`にmanifest・source11 file・固定JAR・Maven／JUnit・HTTP応答・実OTLP binary／text・log・ownership／cleanupを保存した。
source snapshot全件とmanifest、固定JAR SHAを照合。採用Micrometer handler／bridgeの静的照合は`tracing-handler-bytecode.txt`へ保存した。
初回`initial-execution/`は上書きしていない。追加epochのsourceも期待値訂正前へ帰属するため、新たな実行で流用・上書きしない。

承認済みの追加一回は消費済み。次はO02の採用版に沿った期待値訂正を確認し、同じJAR・依存・設定・10 checkpointで再度一回を実行するかのOwner判断。
実行する場合は新epochで無効対照から全3 childを行い、既存成功の合算や途中再開はしない。
現在のFAILをPASSへ上書きせず、同期OpenTelemetry全体・非同期DoD 4-12・正式Collector・運用保証は未成立のまま残す。

## 7. O02期待値訂正後の同範囲一回（2026-10-30）

Ownerは「訂正後の同じ範囲で再度1回の検証へ進めてください」と指示した。
新しい`status-aligned-execution/`にsource・manifest・固定JAR・証拠を保存し、export無効対照から全3 child／10 checkpointを一回実行する。
JAR・依存・起動設定・資源上限は前回と同じ。変更はharnessのO02期待値とREADMEの履歴のみ。
既存二回の失敗は保持し、成功を合算しない。今回は上限10分、cleanup5分の有限実行とし、結果を本節へ追記する。

### 7.1 単独実行の最終結果

2026-10-30 **11:58:39 JST**終了。専用Maven21.999秒、JUnit20.795秒。
**JUnit1 invocation／0 failure／0 error／0 skip、10論理checkpointすべてPASS**。
これは今回の単独実行の結果であり、過去の部分成功を合算していない。

| 構成・checkpoint | 最終実行で確認したこと |
|---|---|
| Java21／export無効・O04 | exporter Bean0、HTTP200、3秒観測窓でOTLP受信0 |
| Java21／export有効・O01 | 標準HTTP SERVER spanをOTLP HTTP/protobufで受信・decode。service名とhandler／構造化logのtraceId・spanId・requestIdが一致 |
| O02 | 例外なしHTTP500の応答・log・spanが対応し、status="500"・outcome="SERVER_ERROR"・exception="none"・span status UNSETを確認 |
| O03 | 固定の異なるW3C traceparent／requestIdの逐次要求・並行4要求と、ID無入力の後続要求が期待どおり分離 |
| O05 | 5秒間の503応答modeでappの上限8要求が各3秒のclient deadline内に正常終了。export失敗を観測。SDKの再試行を含む失敗はtimeoutとして記録され、appは継続 |
| O06 | 2秒遅延modeでappの上限8要求が各deadline内に終了。500msのexport期限によるtimeoutを観測 |
| O07 | 受信先回復後10秒以内に新しい正常要求のspanを受信。障害中データの再送・欠損ゼロの保証は含めない |
| Java25／VT有効・VT-O01・03 | 同じJava21生成JARで正常接続・log相関・要求分離が成立。handlerが実VTであることを確認 |
| O08 | source／JAR同一性、全保存証拠の合成canary非出力、保存上限、child／reader・receiver／executorの終了が成立 |

### 7.2 資源・同一性・証拠

| 項目 | 実績 |
|---|---|
| JDK | 主検証Temurin21.0.12.1+1、VT smoke25.0.5+8 |
| source | `b2ca253`＋未commit差分、dirty=true。実行時Tooling11 fileをsnapshotし、manifestの全hashと一致 |
| consumer JAR | SHA-256 `eec79e836e5a84a8d56aa92a8171751b616f0202ceb534aabd5347b923b6e63f`、全epochで同じ |
| 既存Starter | SHA-256 `e14cf9bb04c2054537fa4e3f98c93c0fe362e69b7a4b65b2d149cf2f144b72c8`、埋込payloadと一致 |
| child | Java21無効対照→Java21有効→Java25 VTの3子processを順次起動。並走なし |
| 要求・受信 | HTTP35要求、OTLP10要求、decodeしたspan34件。503・遅延時の受信も含むため、decode数を配送成功数・重複なしの件数とは扱わない |
| 保存 | evidence112,469 bytesで32MiB上限内。固定JARは別artifact領域 |
| 外側チェック | test=0、cleanup=0、verify=0。unexpectedChildren空。3 child PIDの消滅、reader／receiver／executor終了を照合 |
| 変更範囲 | 独立Tooling11 fileと文書のみ。Framework／ReferenceのJava・POM・設定、Root Reactor・公開API・CIは変更なし |

`status-aligned-execution/`にmanifest・source snapshot・固定JAR・Maven／JUnit・checkpoint／summary・OTLP binary／text・safe log・HTTP応答・ready／ownership／cleanupを保持する。
`evidence/summary.json`の10項目成功と`resource-cleanup.json`を照合した。実行後の文書更新は、実行時snapshotとは区別する。
生の環境変数・実secret・認証情報は記録していない。通常root・既存D2 matrix・追加CIは再実行していない。

### 7.3 判定と残る境界

**D1S-02／03の今回の限定接続・動的確認は技術的に成立。Owner結果受入は未記録・確認待ち。**
Frameworkを改変せず、consumer側の標準依存・設定で同期HTTPのtrace exportとlog相関を構成できることを、このfixture・採用版・検証範囲で確認した。

- 受信fixtureへのOTLP接続まで。正式Collector製品・保存backend・cloud監視・本番SLOの成立ではない。
- 独自の常設監視、metric／logのOTLP export、DB／外部HTTP client span、認証・認可・Audit・ERROR dispatch全経路は対象外。
- 例外なしの500応答と、Java例外によるspan ERRORを区別する。任意のbackendでERROR statusだけを見てHTTP失敗を網羅できるとは保証しない。
- traceparent／requestId分離は選定要求の証拠で、全executor・全threadのcontext非漏えい保証ではない。
- 非同期配送・復旧・DoD 4-12・本番運用は未成立のまま。非同期採用案件が具体化した時に、その設計・運用を必要範囲で深掘りする。
- test model／protobufの明示固定はharness限定。Boot／model更新時は版固定の必要性・linkage・実payload decodeを再確認する。

初回helper ERROR・次回期待値FAILは履歴として保持する。さらに実行を追加せず、次は結果・Git差分と成果固定対象の確認。
Owner受入・commit／push／PR・required化・正式releaseは別判断とする。
