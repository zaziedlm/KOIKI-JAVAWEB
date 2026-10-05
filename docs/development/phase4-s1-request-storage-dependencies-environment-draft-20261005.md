# S1方式候補・復旧要求の保存構造・依存構成・検証環境上限案（2026-10-05）

> **現在の参照先（2026-10-05）:** 本書は拡張方式の比較材料。4種類記録・4 JVM・200〜392時間を初回の必須構成へ転用しない。現在の2種類記録の局所候補・Tier候補・初回作成／実行は承認済み方式票へ追跡する。 [最新引継ぎ](phase4-s1-local-verification-start-handoff-20261005.md)／[承認済み方式票](phase4-s1-method-ballot-and-local-verification-contract-20261005.md)。

> **2026-10-05 方針見直し:** Ownerは[最小構成と将来の運用管理機能の分離](phase4-s1-minimum-demonstration-scope-review-20261005.md)に同意した。本資料の要求管理一式・Tier 2・追加24〜48時間は拡張方式の比較材料へ戻す。初回S1の採用前提ではない。Tierは最小構成確認後に判断する。以下は見直し前の具体案として保持し、最新の判断順はリンク先を優先する。

**状態:** DRAFT / 実装前review具体案。Ownerの指示は本資料の具体化であり、RV票の採用判定・DDL / POM変更・環境起動・Gate / 実装開始の承認ではない。
**Ownership:** Framework側の設計資料。要求受付・復旧・通知はReference `notification`、event発行は`expense`、stub・観測sink・故障注入は非配布Toolingを候補とする。
**確認baseline:** `docs/daily-development-workflow` / `e75a70a`と未commitの設計文書。source / POM宣言をread-only確認。effective POM・依存解決・端末資源・non-Web実動は未確認。
**入力:** [実装前RV票・PF案](phase4-s1-preimplementation-review-preflight-estimate-draft-20261005.md)、[nonHTTP構成](phase4-s1-nonhttp-process-observation-review-draft-20261005.md)、[保存・規約のsource照合](phase4-s1-w01-w02-source-design-review-draft-20261005.md)、[元WBS・概算](phase4-s1-execution-resources-and-sequence-draft-20261005.md)、[実演case](phase4-s1-dod-demonstration-plan-20261005.md)。

## 1. review票に対する推奨候補

Owner一人＋Codexで順次確認し、下記を今回の比較基準とする。人はDB・process・provider・Audit / logを総合して再送 / 保留を判断する。台帳状態から再送許可を自動推定しない。

| 票 | 今回具体化する推奨候補 | 採用前に残る条件 |
|---|---|---|
| RV-1 | 既存認証済みWeb経路で要求受付、non-WebからIdentityQueryでACTIVE / permissionとApplication scopeを再確認。直接認可を第一候補 | 復旧permission code・scope・失効適用時点、受付route / CSRF。Web details偽装や自己申告principalを用いない |
| RV-2 | 要求・実行試行・判断履歴を分離し、要求の単発消費と対象の実行権を短いDB transactionで確保 | §2の状態 / 制約・DB保護・Audit分類。要求claimだけで通常listenerを停止したとは扱わない |
| RV-3 | 同一固定Reference JARの明示non-Web単発モード。対象要求IDを1件指定 | 復旧Bean / executorの実効条件、auto再送停止、対象試行終端、終了 / flush。既存起動mode名は未変更 |
| RV-4 | publicationはModulith JDBC / UPDATE、Reference-owned追加migrationとkkref履歴維持。業務の要求・通知logはJPA | Ruleの明示Level選択と既存入口互換、対象module限定。DDL / Public API / dependenciesはblocking review対象 |
| RV-5 | 要求作成・実行準備をDB commitしてから外部送信。依頼者USER actorと実行process世代を区別 | Business / Security分類、送信後記録失敗・保留判断の記録。AuditResult enumは拡張しない |
| RV-6 | Boot標準OpenTelemetry Starterによるmetric / traceのOTLP送信、Tooling Collectorで採取。logは既存structured logを保存 | §3の依存実効値・TaskDecorator共存・実span・短命process flush、採取欠落 / sink不達。alertは別途実受信を実演 |
| RV-7 | S1限定・既存Level 2限定P4-F経路を引き続き推奨。環境上限と再送制限は§4の仮値でreview | DoD 4-2 / 4-3解釈・合格数値、OR・作業8 / 9・Gate整合。試験timeoutを復旧SLAに転用しない |

全票の判定は未判定のまま。方式の具体案を示したことと、Ownerの採用判定を区別する。

## 2. 復旧要求の保存構造案

本節の4種record・Tier見直しは拡張候補であり、最小S1に不可欠と確定した構造ではない。

### 2.1 所有・永続化と論理record

`notification`が要求受付・復旧のUse CaseとOutbound Adapterを所有する案とする。他moduleのRepositoryやFramework Identity内部へ依存せず、expenseは値だけのeventを発行する。publication storeのJDBCは技術的registry保存であり、notification業務更新のJPAとは責務を分ける。

要求の状態遷移・単発消費・対象排他を含めるため、従来のnotification Tier 1開始案を**Tier 2 RICH / JPA共有モデルへ見直す候補**とする。notification内でTierを混在させない。Framework共通復旧moduleを新設する案ではない。次の名称は論理名であり、table / column / Java型の正式名称ではない。

| 論理record | 保存する最小情報案 | 制約・更新責務 |
|---|---|---|
| 復旧要求 | 要求UUID、環境ID、publication UUID、event型 / 不変event ID、listener識別、通知key / payload digest、観測status / attempt / snapshot時刻、依頼者user ID、理由、作成 / 期限、要求状態、version | 作成後に対象・依頼者・理由・期限を上書きしない。変更が必要なら新要求。外部credential・生の通知本文・メールアドレスを保存しない |
| 実行試行 | 試行UUID、要求UUID、開始 / 終了時刻、worker instance / 起動世代、実行前snapshot、認可確認時刻、provider受理識別、送信 / publication / 記録の各観測結果、結果区分 | 要求UUIDに一意制約を置き1要求1試行。操作試行IDは通知keyに使わない。provider受理不明を未送信へ変換しない |
| 対象の実行権 | 環境ID＋publication UUID、保持する試行UUID、取得時刻、version | 対象組を一意とし、別要求の同対象競合も排除。期限経過だけで自動奪取しない。異常終了時は人が停止 / providerを確認して解除 |
| 判断履歴 | 判断UUID、要求 / 試行ID、判断者user ID、再送 / 保留 / 調査 / 解除等、理由、証拠参照、記録時刻 | 追記のみ。実行直前の停止世代・drain・provider照合を結ぶ。DB内のBoolean「停止済み」だけでは実行許可にしない |

Reference追加migrationで作成し、適用済みV1〜V3は維持する。Application-owned record間には必要なFKを検討するが、Identity / Audit / publicationなど別owner tableへのFKは作らず不変IDを参照値として保持する候補。publication IDの存在・型 / event / listener・snapshot整合は実行時に検証する。digest一致は誤変更検知であり、DB改変に対する認証署名ではない。

### 2.2 状態とtransaction境界

要求状態案は「受付済み → 消費済み」、または消費前に「取消 / 期限切れ」。消費済みは送信成功を意味しない。試行結果案は「準備中 → 実行中 → 成功 / 拒否 / 保留 / 失敗」。これらはApplication-owned区分でありAuditResultの新enumではない。crashで準備中 / 実行中が残った場合も、未送信と断定しない。

1. **受付transaction:** Webの本人認証・permission / scopeと入力を確認し、要求と必要Auditを保存。commit失敗なら実行要求として成立させない。
2. **実行準備:** 人の最新照合後、non-Webが要求の未消費 / 期限 / 対象と現在user・permissionを確認。要求rowのlockまたはversion付き条件更新、試行の一意挿入、対象実行権取得、必要送信前Auditを一つの短いtransactionでcommitする。DB lock順は対象→要求等の一貫した順序に設計し、deadlock / 競合は送信せず終了する。
3. **送信直前:** 専用復旧lockの保持、現在snapshotと停止証拠、期限・再認可を再照合する。外部I/O中に業務row lockを長時間保持しない。専用lock connectionは維持するが、喪失後の送信を完全阻止するfencing保証とは説明しない。
4. **実行と記録:** 安定通知keyで対象listenerへ1回だけ復旧要求。対象publicationの今回の試行・provider・通知log・Auditを照合する。結果保存は送信前transactionと分離し、保存失敗は保留 / 証拠回収へ進む。
5. **解除 / 再判断:** 成功・安全な送信前拒否なら実行権を記録付きで解放する。受理不明 / timeout / lock喪失は保持し、自動期限解除・同じ要求の再消費をしない。人が旧worker停止と副作用を確認し、解除と新要求を判断する。

prepare後の失効や期限切れはその試行を送信前拒否として記録する。照会後失効の窓は残るため、開始済み送信の取消しや瞬時失効を保証しない。provider冪等性・通常process停止 / 再起動制御は要求一意制約と独立した安全条件である。

### 2.3 DB保護と受入条件

migration用、認証済み受付用、復旧process用credentialの分離を優先候補とし、要求の対象 / actor / 理由を復旧側から書換不可、判断履歴を通常操作から更新 / 削除不可とする。既存Audit・Identity読取・publicationの正当な更新に必要な権限を含め、全table / sequenceの権限表をreviewする。Hibernate / Flyway起動時も含め最小権限で動くかは未検証。同一汎用DB credentialでこの保護が成立したとは扱わない。管理者によるDB改変耐性は別の運用管理であり、今回暗号署名機能を追加しない。

E12 / E13 / E16へ、直接DB改変拒否、同要求2worker、別要求同対象、claim後crash、準備commit失敗、prepare後失効 / 期限切れ、送信後記録失敗、保留対象の自動解除なしを対応付ける。fresh / upgrade、通常承認rollback、非対象publicationの既存終端による誤終了も検証する。対象pollの実効API / SQLとModulith schemaに今回の試行をどう対応付けるかは実装前reviewで閉じる。

## 3. 依存構成と観測sink案

[KOIKI BOM](../../koiki-dependencies-bom/pom.xml)はBoot `4.1.1` / Modulith `2.1.1`を宣言している。これはsource上の管理値で、今回の解決済み実効依存でも採用承認でもない。[Reference POM](../../koiki-reference-app/pom.xml)を維持し、次の追加候補をreviewする。

| 所有・用途 | 直接依存 / 構成候補 | 確認条件 |
|---|---|---|
| Reference publication | `org.springframework.modulith:spring-modulith-starter-jdbc` | [既存Tooling POM](../../build-support/phase4-level2-verification/pom.xml)に宣言例がある。JPA publication starterを同時採用しない。events-apiの直接宣言要否は使用APIと依存treeから決定 |
| Reference要求・通知log | 既存JPA / transaction / Identity / Audit / Observability契約を利用 | Web Starter等からの推移依存を確認し、直接使用APIに必要な宣言を整理。独自Identity照会API、Spring Batch、新KOIKI Starterは前提にしない |
| Reference実metric / trace | `org.springframework.boot:spring-boot-starter-opentelemetry`を優先候補 | Boot BOMに従い、bridge / exporter / OTLP registryを重複・独立versionで足さない。non-WebではMicrometer Observationから実spanを作り、既存requestId decoratorとtrace context伝播を共存させる |
| Tooling観測受信 | OpenTelemetry Collector Contrib、OTLP HTTP受信＋file exporterを候補 | 選定release / image digest・file形式・rotation / 安定性を確認。metric / traceを保存して実体を閲覧。Collector単体をdashboard / alert基盤とみなさない |
| Tooling provider | 独立HTTP stubと専用受理記録 | 同DB別transactionの既存stubだけで応答喪失を実証しない。実provider・実メールへ送信しない。製品 / library・永続化方式はTooling reviewで確定 |

Spring公式の[Boot OpenTelemetry説明](https://spring.io/blog/2025/11/18/opentelemetry-with-spring-boot/)は、このStarterにOTLP metricとtracing bridge / exporterが含まれる構成を説明している。これは候補選定の根拠であり、KOIKI Boot 4.1.1の実効Bean / 依存互換の証拠ではない。file exporterは[OpenTelemetry公式source](https://github.com/open-telemetry/opentelemetry-collector-contrib/tree/main/exporter/fileexporter)を参照し、mainの内容を固定release契約へ転記しない。

logは既存structured logのfile採取を用い、OTLP log appender追加は初回の必須条件にしない。metricはOTLP送信、traceは復旧ごとに新しい実traceとしてevent / publication / operationで関連付ける。短命processは最後のmetric export・span flushとsink受領を確認して終了し、flush呼出し成功だけを保存成功としない。

alertはToolingの限定監視からローカル通知 / 記録を出し、Ownerが受信・確認・判断時刻を記録する案。外部Teams / mail送信は含めない。専用観測Web serverを復旧processに起動しない。sinkのOTLP portはloopbackまたは隔離network内だけに限定する。

PF-3の出口はeffective POM / dependency tree・重複store / exporterなし・version / checksum固定、PF-5 / 6の出口はnon-Web拒否・実span / metric受領・context非漏えい・flushとsink失敗の実演とする。POM・image / 設定は今回変更していない。

## 4. 検証環境・実施上限の数値案

以下は**Agentの仮提案値**であり端末実測・予算承認・DoD基準ではない。PF-2で成立を確認し、最小構成の1試行で測定してから競合caseへ広げる。収まらない場合は不足caseを省略せず、構成または上限を再判断する。

| 管理対象 | 初回の仮値 / 上限候補 | 超過・不成立時の扱い |
|---|---|---|
| 実演同時数 | 1実演のみ。case内は通常2＋復旧2の最大4Reference JVM | 通常停止を要求するcaseでは送信前に通常2を停止確認。競合負例だけ隔離して重ねる。別case・subagent作業は同時実行しない |
| JVM memory | Reference各最大heap 512 MiB、実RSS 1 GiBを見直し点 | heapはnative / metaspace等を含まない。起動不足・RSS超過なら中止して再設定。OOMで安全に完了したとしない |
| Tooling資源 | DB 1 GiB、Collector 512 MiB、stub 512 MiBをcontainer等の上限候補 | 最大4 JVMのRSS約4 GiBと合わせ約6 GiB。Docker VM overheadを別測定し、端末available memory 8 GiB未満なら競合実演を保留 |
| CPU | 検証群の割当目安4 logical CPU、container上限合計4 CPU | Windows JVMを含む実効CPU制御は未選定。継続的な飽和 / drain遅延なら負荷を止める。本番性能判定にしない |
| DB接続 | 通常2×pool4、復旧2×(pool3＋専用lock1)、stub2、harness2＝最大20。試験client合計24まで、管理4、DB max_connections候補32 | role / pool / 独立stub保存先に応じて実効数を確認。専用lock分をpoolへ紛れ込ませない。接続枯渇で認可 / Auditが成立しなければ送信しない |
| 保存・空き容量 | 実演data / log / telemetry合計10 GiB、うちtelemetry 2 GiBを見直し点。開始前空き20 GiB以上 | image / Maven cache / Docker VMを別採取。容量超過は採取停止・保留、未確認証拠の自動削除なし |
| 復旧単位・回数 | 1要求＝1publication＝1試行。同一論理通知の意図した復旧送信は1実演で最大2回 | 2回目は人の再判断と新要求。内部HTTP retryは明示し、無制限retryを無効化。provider受理不明なら回数枠が残っていても自動再送しない |
| 実演待機 | claim / lock取得10秒、provider応答10秒、当該試行終端待ち120秒、終了時export / flush確認30秒を候補 | timeoutは取消し / 未送信証明ではない。送信可能性が残れば保留・process / provider確認へ。DoD復旧時間の合格値とは別 |
| 要求期限 | 作成から30分を初回候補、実行前にも再確認 | 期限内でもsnapshot・停止 / provider確認が古ければ保留。期限延長の上書きなし。準備に足りなければ理由付き新要求 |
| 保存期間 | 作業証拠7日を最低確認窓とし、Owner結果確認・commit前は削除しない | 未確定副作用 / 保留対象は期限経過でパージしない。publication / 通知log / Auditの本番retentionは別review |
| 環境金銭費 | ローカル既存環境、今回新規有料サービス0円を範囲上限候補 | 電力・既存機器 / 契約費の総費用0とは説明しない。cloud / 有料sinkが必要なら別判断 |

PF-2は診断のみ、PF-4以降は選定された資源台帳と開始範囲に従う。collector / stubの製品・release / digest、DB role、port / volume、証拠保存先を台帳へ記録してから起動する。今回診断・container起動・testは未実施。

## 5. 作業量差分と確認する順序

本節の追加量・総量と確認順は拡張方式を前提とした参考値。最小構成への見直し後の現行見積ではなく、最小案の確定後に再算定する。

元176〜344標準作業時間の比較基準に対し、今回の候補ではW04の要求 / claim / Tier見直しに8〜16、W06の独立通信stub / crash制御に8〜16、W09の競合 / 失効 / 保留枝に8〜16時間を**純追加の仮枠**として提案する。W07のOTLP / sinkは元見積に含まれるため全額追加しない。W01 / W02の今回までの設計時間は実測記録がなく、残量から差し引かない。

| 比較 | 元案 | 今回候補の仮更新 | 根拠・限界 |
|---|---:|---:|---|
| W04 | 24〜48 | 32〜64 | 要求の単発消費、対象実行権、JPA状態遷移・DB保護を明示 |
| W06 | 24〜48 | 32〜64 | 通信境界stub・応答喪失・実行準備crashを追加 |
| W09 | 32〜64 | 40〜80 | 追加枝の操作・証拠突合。case数21を実行回数としない |
| 同じ比較範囲の総量 | 176〜344 | **200〜392** | 差分24〜48の低確度提案。既実施量未控除で、今からの残工数・納期・予算ではない |

上限側392時間は今回方式を採る場合の再判断点候補で、既存344時間の閾値を正式更新しない。元WBSに既に含まれる部分が判明すれば純追加枠を減らし、DB credential分離や観測が難航すれば再見積する。Owner review・人の実演参加時間は別枠未取得。代行者が現状いないことを架空の担当確保で埋めず、Owner不在時の送信 / 判断は保留する。

次は、(1) RV-1〜6の推奨候補・Tier見直し・数値仮値をOwner確認、(2) permission / scopeとAudit分類、DB権限表・DDL案・依存 / image固定案を順次review、(3) RV-7と開始Gate・個別CPを整合、(4) 選定範囲のPF-2以降の実測・検証、の順とする。残判断は方式の承認、識別 / 試行終端の実装可能性、最小権限での起動、端末資源と数値、DoD解釈である。実装・検証に必要な入力をこの資料へまとめたが、未実演事項をPASSとは記録しない。
