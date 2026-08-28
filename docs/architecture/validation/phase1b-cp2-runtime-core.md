# Phase 1b CP2 Runtime Core検証

## 1. 目的と判定

`koiki-starter-api`からCore Configuration、Jackson 3、Spring Framework Resilience、Spring MVC
API Versioningを利用し、Customer-like Consumerのversion付きHTTPからController、Application Use Caseへ
到達できることを検証する。

| 項目 | 結果 |
|---|---|
| Phase / status | Phase 1b CP2 COMPLETE |
| Framework ownership | `koiki-starter-api`のinternal auto-configurationと既定値 |
| Tooling ownership | 細粒度fixture、Customer-like Runtime Consumer、隔離検証script |
| Start commit | `78c11a5`（Phase 1b CP1完了） |
| 判定 | **COMPLETE** |

検証日時は2026年8月28日 12:27〜12:28 JSTである。

## 2. 設計判断

### 2.1 Spring標準境界

- path API VersioningはSpring Boot標準の`spring.mvc.apiversion.*`を利用し、versionをpath segment 1から解決する。
- Controller mappingは`/api/{version}/work-items`と`version = "1"`を使用する。
- ResilienceはSpring Framework標準の`@EnableResilientMethods`と`@Retryable`を使用する。
- Jackson 3はBoot標準の`JsonMapper`と`JsonMapperBuilderCustomizer`経路を維持する。
- KOIKI独自annotation、version resolver、retry engine、HTTP DTO共通基底型は作成しない。

根拠はSpring Frameworkの[API Versioning](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-config/api-version.html)、
[Resilience Features](https://docs.spring.io/spring-framework/reference/core/resilience.html)およびSpring Bootの
[JsonMapperBuilderCustomizer](https://docs.spring.io/spring-boot/api/java/org/springframework/boot/jackson/autoconfigure/JsonMapperBuilderCustomizer.html)
である。

### 2.2 既定値とoverride

Starterはlow-precedence property sourceから次を既定化し、Customerの`application.properties`等を優先する。

- Jackson module自動検出無効、unknown property／trailing token拒否
- path segment 1、version必須、明示supported version `1`
- retryは2 retries、10ms delay、1s total timeout

`koiki.api.enabled=false`で全既定値とResilienceを無効化でき、
`koiki.api.resilience.enabled=false`ではJackson／Versioningを維持してResilienceだけを無効化できる。

Public API inventoryはCustomer向けJava型0、KOIKI設定property 5件である。auto-configurationと
environment post-processorは`org.koikifw.starter.api.internal`だけに配置する。

## 3. Customer-like業務HTTP経路

```text
POST /api/1/work-items
  -> WorkItemController
    -> CreateWorkItemUseCase
      -> WorkItemRepository
        -> InMemoryWorkItemRepository
```

Controller、request／response DTOはConsumerの`workitem.adapter.inbound.mvc`が所有する。
ControllerはRepositoryを直接参照せず、JPA Entityをresponseへ露出しない。Tier 1 SIMPLE、in-memory adapter、
DB未導入というCP1境界は維持する。

## 4. 隔離artifact検証

Command:

```powershell
pwsh -NoProfile -File build-support/runtime-foundation-verification/verify-cp2-runtime-core.ps1
```

Final result:

```text
CP2 isolated artifact, defaults, override, retry, versioned HTTP,
and dependency checks succeeded.
```

| 経路 | 結果 |
|---|---|
| KOIKI release unit | 6 projects SUCCESS、50.713秒 |
| Root contract | Architecture Contract 4件、ArchUnit Rules 66件 SUCCESS |
| CP2細粒度fixture | 6 tests SUCCESS、10.785秒 |
| Starter back-off | 全体無効／Resilience単独無効ともSUCCESS |
| Jackson | module自動検出無効、strict unknown property負例、Customer override SUCCESS |
| Resilience | transient failureを合計3回で成功、対象外例外は1回で失敗、fail-silentなし |
| Consumer unit | Tier 1 Use Case 2 tests SUCCESS |
| Consumer architecture / HTTP | 5 tests SUCCESS、19.302秒 |
| version付きHTTP | version 1は201、非対応version 2は400、version欠落pathは404 |
| runtime dependency tree | 3 projects SUCCESS、9.274秒 |
| dependency boundary | Jackson 3／MVC／Validationあり、WebFlux／Reactor／Security／JPA／Modulith runtimeなし |
| cleanup | 隔離repositoryとnegative probeを削除 |

## 5. Negative boundary

- Consumerから`org.koikifw.*.internal`への参照がない。
- synthetic sourceではinternal参照guardが失敗を検出する。
- Starter JARのJava classはすべて`internal` packageにあり、Customer向けPublic Java APIを追加していない。
- version segment自体がない`/api/work-items`はmapping不一致として404、存在する非対応versionは
  Spring API Versioningが`InvalidApiVersionException`として400にする。
- Problem Details、validation error統一、Jackson異常応答の共通化はCP3へ残す。

## 6. 結論と次の境界

CP2の完了条件であるCore Configuration、Jackson 3、Resilience、API Versioning、override／retry負例、
version付きHTTP→Controller→Use Caseを満たした。既存ADRの前提変更はなく、ADR追加・改訂は不要である。

CP3では同じHTTP経路にProblem Details、Validation、`JacksonException`の統一error mappingを追加し、
入力不正、異常JSON、業務相当例外、内部情報非露出を検証する。
