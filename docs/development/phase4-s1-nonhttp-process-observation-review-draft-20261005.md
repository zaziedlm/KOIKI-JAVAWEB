# S1非HTTP復旧主体・専用process・実観測構成review案（2026-10-05）

> **現在の参照先（2026-10-05）:** 本書はnon-Web・実観測の比較材料。現在の初回局所契約には実Web認証・新tracer / exporter・別JVMを含めない。正式構成の残論点として保持し、初回開始は承認済み方式票を読む。 [最新引継ぎ](phase4-s1-local-verification-start-handoff-20261005.md)／[承認済み方式票](phase4-s1-method-ballot-and-local-verification-contract-20261005.md)。

**状態:** DRAFT / 実装前review入力。Ownerは前段の詳細を全て見切った承認ではなく、大枠を認識して本段階へ進む指示を示した。方式・API / schema・認証・観測依存・数値・実装開始は未承認。
**Ownership:** Framework側の設計資料。要求・業務実行構成はReference / Application、実演harness・stub・sinkはTooling。既存Framework契約の変更は別review。
**確認baseline:** `docs/daily-development-workflow` / `e75a70a`。前段W01 / W02・運用条件等は未commit差分。source読み取りのみ。非Web起動・認証・Maven / Docker・実traceは今回検証していない。
**入力:** [方式・DoD・運用条件案](phase4-s1-method-dod-operations-observation-draft-20261005.md)、[W01 / W02](phase4-s1-w01-w02-source-design-review-draft-20261005.md)、[初回判断J1〜8](phase4-initial-scope-gate-and-open-decisions-draft-20261005.md)、[実演計画](phase4-s1-dod-demonstration-plan-20261005.md)。

## 1. source確認で明らかになった条件

| Source | 確認した事実 | 設計への影響 |
|---|---|---|
| [Security auto-configuration](../../koiki-starters/koiki-starter-security/src/main/java/org/koikifw/starter/security/internal/KoikiSecurityAutoConfiguration.java) | Servlet web条件をclassに持ち、`@EnableMethodSecurity`も同class上にある | non-Webではこの構成からmethod securityが有効になると推定しない。Application側の明示的有効化 / 直接認可と実効testが必要 |
| [Identity認証構成](../../koiki-starters/koiki-starter-identity/src/main/java/org/koikifw/identity/internal/KoikiIdentityAuthenticationAutoConfiguration.java) | local認証Providerの生成はproperty等が条件で、Servlet限定とは記載されていない | Provider Beanが存在し得ることとnonHTTP認証の成立を区別する |
| [SourceFingerprintFactory](../../koiki-starters/koiki-starter-identity/src/main/java/org/koikifw/identity/internal/SourceFingerprintFactory.java) / [local Provider](../../koiki-starters/koiki-starter-identity/src/main/java/org/koikifw/identity/internal/KoikiLocalAuthenticationProvider.java) | APPLICATION source protectionはWebAuthenticationDetailsと信頼されたremote addressを要求し、取得不能時は拒否する | CLIでuser ID / passwordだけ渡す構成は同じ契約で成立しない。Web details偽装やEXTERNALへの便宜変更で迂回しない |
| [Reference main](../../koiki-reference-app/src/main/java/org/koikifw/reference/ReferenceApplication.java) | 通常のSpringApplication起動で、専用復旧モード分岐は未実装 | 起動・Bean・executor・終了・認可を正式設計する必要がある |
| [CP8 Consumer main](../../build-support/runtime-foundation-consumer/application/src/main/java/org/koikifw/runtimeconsumer/RuntimeFoundationConsumerApplication.java) / [runner](../../build-support/runtime-foundation-consumer/workitem/src/main/java/org/koikifw/runtimeconsumer/workitem/adapter/inbound/command/WorkItemMaintenanceRunner.java) | non-Web明示・単発runner・終了結果のTooling実例がある | 構造と知見を利用候補にするが、その認証・Audit保証や終了コードをS1へ転用しない |
| [AuditStore](../../koiki-starters/koiki-starter-audit/src/main/java/org/koikifw/audit/internal/AuditStore.java) | DB Auditは内部ClockとMDC requestId / traceIdを読み、UUIDを生成してpersist / flush | nonHTTPではMDCが空であり得る。操作resource等からの追跡を設計し、caller任意traceIdの挿入で保証を作らない |

これらはread-only sourceの確認であり、non-Web環境で実際に有効なBean・認可・拒否を実行確認した結果ではない。

## 2. 非HTTP復旧主体の候補比較

| 候補 | 特徴と追加条件 | 初回S1での位置づけ |
|---|---|---|
| A：認証済み要求を記録し専用processが実行 | 既存の認証済みReference経路で対象1件・理由・期限等をApplication-owned要求へ保存。実行側は要求の真正性・対象・未使用・期限・現在の権限 / user有効性を再検証 | **優先候補。** 操作者の信頼をWeb認証に寄せ、実処理はnon-Webへ分離。既存画面に復旧機能があるという意味ではなく、受付・実行とも未実装 |
| B：直接nonHTTP認証 | Application認証Adapterが信頼できるcredential / 発行主体を検証し、FrameworkPrincipalと権限へ接続 | 非Web本人確認・source protection・credential保護・失敗Auditの契約が必要。独自token発行やprovider内部参照を先行しない。初回の既定にはしない |
| C：OS実行者 / user ID引数だけで許可 | 起動は簡単だがFramework user本人・権限と結び付かず、監査actorを自己申告できる | 正式再送許可には使わない。OS権限はprocess / DB資源保護であり、Application認可の代替ではない |

候補Aでは、人が要求を認可して記録する時点と、実行直前の停止証拠・DB / provider照合を分ける。要求受付後に通常processを停止 / drainする場合、非Web側から必要な再検証を行える構成をreviewする。受付権限があったことだけで、後の無効user・権限失効・期限切れを通過させない。

Web受付を使う場合もCSRF・default deny・route / 権限を維持し、専用管理UIの新設を必須にはしない。要求recordの作成経路は実装reviewで選ぶ。request / publication IDを引数で指定するだけでは真正性・実行権限を証明しない。

### 2.1 要求・実行とactorの条件

要求はApplication-ownedの型 / 台帳候補で、認証済み不変Framework user ID、環境・対象publication / listener・観測試行、理由・期限、実行結果等を結ぶ。field名・schema・APIは未固定。停止証拠とprovider結果は実行直前の新しい観測へ結ぶ。

要求の未使用確認と実行権取得は競合に耐える必要がある。advisory lockだけで要求の二重消費・同一対象への別要求を排除したとせず、状態更新・一意性・transaction境界をreviewする。実行後失敗 / crash時の「使用済み」は送信成功と同義ではなく、再開は人が照合して新しい操作単位で判断する。

Auditは認証済み依頼者のUSER actorに結び付ける案をreviewし、実行processのidentity / 世代は操作台帳へ分ける。これは人が承認した要求をprocessが実行した記録で、processが本人へなりすます方式ではない。Application復旧をFramework-owned SYSTEMへ自動分類しない。非HTTPからの現在user / permission再検証に必要な既存Public契約の適用可否はOPEN。Framework内部Identity Repositoryへ直接依存しない。

## 3. 専用processの構成・ライフサイクル案

初回候補は同じ固定Reference artifactの専用実行モードから単発復旧を行う構成とする。Spring標準のnon-Web起動、runner、終了処理を利用し、新しいFramework runner API / Batch依存を既定にしない。

| 段階 | 構成・処理条件案 | review・負例 |
|---|---|---|
| 起動 | non-Webを明示。復旧用Bean / runnerだけをopt-inし、通常業務起動・scheduler・自動再公開は制御 | Web portが開かない、通常 / 復旧モードのBean差分、誤引数・想定外起動を検証。profile名・property・終了コードは未固定 |
| 実行準備 | 要求の再検証、明示認可、短いtransactionで送信前記録、process停止 / provider / snapshotを人が確認 | non-Webでのmethod security・proxy適用または直接認可を実効test。HTTPの自動認可やSecurityContext伝播を期待しない |
| 排他・選別 | 専用lockを取得し、対象publication / 今回の試行・要求の条件を再照合 | 競合・古い要求・別publication終端・無効user / 権限 / 期限の拒否。lock接続の保持とpool負荷を観測 |
| 配信 | 対象listenerだけを実行可能にし、安定keyでproviderへ送る。必要executorは有効化する | 全listener無効化で再送も不能にしない。起動時の通常初回処理と意図した再送を区別。隠れたauto再送・stale monitorを列挙 |
| 終端・結果 | 当該publication / 試行、provider、通知log / Audit・操作台帳を照合。timeout / lock喪失は結果不明を扱う | 実行受付とCOMPLETEDを区別し、listenerが残るのにrunner成功で終了しない。外部副作用の取消しを推定しない |
| 終了・引継ぎ | executor / contextを閉じ、lock保持sessionを解放。成功・保留 / 競合・失敗を結果と証拠で区別 | 異常停止時も残processと受理を人が確認。診断logはDB Audit / 操作台帳の代替にしない |

Applicationの認証・認可は既存contractへ接続する案を作る。単に`@EnableMethodSecurity`を追加するだけでは非HTTP主体・認証・権限は成立しない。user IDを受けてSecurityContextへ自己申告の認証情報を置く方式は採用しない。

## 4. 実観測構成の最小案

[Observability source](../../koiki-starters/koiki-starter-observability/src/main/java/org/koikifw/starter/observability/internal/KoikiObservabilityAutoConfiguration.java)はrequestIdのTaskDecoratorとServlet filterを提供する。[POM](../../koiki-starters/koiki-starter-observability/pom.xml)はActuator・context-propagation等を宣言し、直接のOpenTelemetry exporter宣言はない。推移依存の実効構成とtracer実動は未検証。

| 構成 | S1最小候補 | 実演の出口 / 残判断 |
|---|---|---|
| log | 既存structured logを使い、操作・event / publication / 試行・process世代を安全に関連付ける | 初回・復旧・保留 / 結果を人が辿れる。secret・PIIは非露出 |
| metric | FAILED件数・選定した滞留起点、復旧 / 保留をApplicationで観測し隔離収集 | DB / 時刻と突合。全件IDのlabel化を避ける。metric名・起点・閾値はreview |
| trace | Boot / Micrometer標準のtracing integration＋OpenTelemetry exporter、隔離した受信sinkをApplication / Tooling側で選ぶ候補 | 初回async・専用processの実spanを採取。bridge / exporter / sink製品・依存・version・配置は未選定、BOM・互換review後に決める |
| 別processの関連 | 復旧操作に新しいtraceを作り、event / publication / operationで元処理と結ぶ | span link等の採否・保持をreview。同じtrace / request MDCを便宜コピーして伝播成功としない |
| alert | 隔離環境で担当Ownerへ通知し、受信・照合・判断・連絡まで実演 | dashboardだけでalert受信としない。通知経路・時間帯・上限値OPEN |
| Audit / 操作台帳 | DB正本AuditとApplication-owned台帳を安全なresource / 操作識別で追跡 | requestIdが空でも追える。recordの結果不明とAudit SUCCESS / FAILURE分類は前段契約に沿ってreview |

metric / trace endpointは専用processで公開できるとは推定しない。non-Webを維持した送信 / export、短命process終了時のflush・配送失敗・再現可能な証拠保存をreviewする。観測のためにWeb serverを暗黙に起動しない。collector等を採用する場合はTooling所有・隔離network・保持 / 費用・cleanupを明記する。

## 5. reviewで決める項目と次の出口

**後続材料:** [実装前review票・環境preflight案・再見積差分](phase4-s1-preimplementation-review-preflight-estimate-draft-20261005.md)で、IdentityQueryから現在user / permissionを読めるPublic接続を確認し、要求真正性・消費・認可と分けてRV票 / PF段階へ整理した。実動・票判定は未実施。

| ID | 推奨案・判断材料 | まだ成立していない条件 |
|---|---|---|
| R-N1 主体・要求 | 認証済みReference経路で対象1件要求 → nonHTTP実行直前再検証を優先候補 | 要求受付 / 消費・期限、現在user / 権限の既存Public契約、actor・認可 / Audit分類。方式採用未承認 |
| R-N2 process | 同一artifact・明示non-Web・単発、復旧Bean / executorと通常processを分離 | 通常listener停止 / drain・再起動制御、auto処理の実効構成、当該試行完了と終了条件 |
| R-N3 観測 | structured log＋Application metric＋実tracing integration / exporter / 隔離sink | 正式依存・version・sink・export / flushとalert / retention、実動証拠 |
| R-N4 運用実演 | Ownerが判断・Codexが支援し、要求 / user失効・競合・不明・記録失敗も確認 | E12 / E13 / E16 / E20へ追加枝・証拠を対応付け、実行条件と権限を確保 |
| R-N5 環境・上限 | 追加Application要求構成と非Web認可、export / sinkの作業差分を再見積へ | 既存176〜344時間案に追加費が収まるとは推定しない。環境確認操作・許可・資源・上限はOPEN |

今回、候補とsource上の制約を絞った。全ての詳細をOwnerが既に承認したとは記載しない。次はR-N1〜3の方式・変更責務・既存Public契約への接続をさらに確認し、実装前review票、環境preflight案と再見積差分へまとめる。Gate設置・前置 / 後続reviewの整合はJ8で別追跡する。正式API / schema / dependency・code・新規実行検証・remoteは変更していない。
