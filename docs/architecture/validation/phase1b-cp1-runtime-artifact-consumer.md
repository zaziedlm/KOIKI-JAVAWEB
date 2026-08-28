# Phase 1b CP1 Runtime Artifact / Customer-like Consumer検証

## 1. 目的と判定

最初のPhase 1b runtime leafを正式release unitへ追加し、Framework内部testだけでなく、
Root Reactor外のCustomer-like Runtime Consumerが通常のMaven coordinatesで利用して起動できることを検証する。

| 項目 | 結果 |
|---|---|
| Phase / status | Phase 1b CP1 COMPLETE |
| Framework ownership | `koiki-starter-api` |
| Tooling ownership | `runtime-foundation-verification`、`runtime-foundation-consumer` |
| Source | `feature/phase1b-runtime-core`、`9483c796675b765b0c1f342fa974cb6732db1712`からのworking tree |
| Base main | `c87e7a5561dff24afea7452f63cce165c666df82` |
| 判定 | **COMPLETE** |

検証日時は2026年8月28日 11:41〜11:47 JSTである。

## 2. 実装境界

### 2.1 Framework artifact

`koiki-starter-api`をRoot ReactorとKOIKI BOMへ追加した。CP1ではServlet Spring MVCと
Jakarta Validationをまとめるdependency starterとし、Java class、auto-configuration、
KOIKI独自Public APIを追加していない。

Spring Boot Starterと同じくPOMの依存契約がCP1成果物である。Jackson、Resilience、API Versioning、
Problem Details等を仮実装せず、CP2／CP3で個別のpositive / negative / override証拠とともに追加する。

`koiki-starter-api/public-api.txt`は`PUBLIC_TYPES 0`であり、隔離repositoryへstageしたJARにも
class entryがないことをscriptで検査した。

### 2.2 Customer-like Consumer

ConsumerはRoot Reactorへ含めず、KOIKI Parentの`relativePath`を空にしてartifact解決だけを使用する。

```text
runtime-foundation-consumer
├── application
│   ├── Spring Boot application assembly
│   ├── startup / business invocation smoke test
│   └── Spring Modulith Level 0 / KOIKI ArchUnit test
└── workitem
    ├── adapter.outbound.persistence
    ├── application.usecase
    └── configuration
```

`workitem`はPhase 1a Feature Templateから生成したTier 1 SIMPLE moduleを開始形とする。
CP1では実行可能性のためConsumer-ownedなin-memory adapterとBean configurationを追加した。
DB、migration、Controller、Domain Eventおよび正式Reference業務は追加していない。

## 3. Root Reactor

Command:

```powershell
./mvnw.cmd --batch-mode --no-transfer-progress clean verify
```

Result: `BUILD SUCCESS`、19.167秒、6 projectすべてSUCCESS。

- Architecture Contract 4件、failure / error / skip 0。
- ArchUnit Rules 66件、failure / error / skip 0。
- `koiki-starter-api`のJAR生成と依存解決に成功した。

## 4. 隔離artifact / Consumer検証

Command:

```powershell
pwsh -NoProfile -File build-support/runtime-foundation-verification/verify-cp1-runtime-foundation.ps1
```

最終Result:

```text
CP1 runtime artifact staging, fixture, independent Consumer build,
architecture checks, and executable startup succeeded.
```

scriptはGUID付きの空のOS temporary directoryをMaven local repositoryとして使用し、最後に検証済みpathだけを削除した。

| 経路 | 結果 |
|---|---|
| KOIKI release unit stage | 6 project SUCCESS、50.229秒 |
| 細粒度Starter fixture | Spring Boot 4.1.1、random port起動、MVC / Validator確認、1 test SUCCESS |
| Consumer build | 3 project SUCCESS、18.413秒 |
| Tier 1 unit test | 2 tests SUCCESS |
| Consumer architecture / startup | 2 tests SUCCESS。Level 0、KOIKI rules、Use Case呼出 |
| runtime dependency tree | reactor `-am`を含めSUCCESS、9.775秒 |
| executable JAR | 外部processで起動し、HTTP 404応答とSpring Boot startup markerを確認 |
| cleanup | 隔離repository、negative probe、logを含むtemporary directoryを削除 |

## 5. Negative / deferred boundary

- synthetic negative sourceで`org.koikifw.*.internal`参照guardが検出できることを確認し、
  実Consumer sourceには該当参照がないことを確認した。
- runtime dependency treeに`spring-webflux`、`reactor-core`、Spring Security、Spring Data JPA、
  Spring Modulithが存在しないことを確認した。
- Spring ModulithとKOIKI ArchUnit RulesはConsumerのtest scopeだけに存在する。
- Consumerは正式Reference、Customer成果物またはProject Templateではなく、Tooling evidenceとして保持する。

## 6. 結論と次の境界

CP1の完了条件であるModulith 2.1.1回帰、最初のruntime leaf、細粒度fixture、独立Consumer骨格、
artifact経由buildおよびSpring Boot起動を満たした。既存ADRの前提変更はなく、ADR追加・改訂は不要である。

CP2では同じStarterとConsumerを育て、Jackson 3、Resilience、Spring標準API Versioningおよび
version付きHTTPからController→Use Caseへ至る経路を、override / negative testとともに追加する。
