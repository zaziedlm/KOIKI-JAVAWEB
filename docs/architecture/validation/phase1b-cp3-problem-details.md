# Phase 1b CP3 Problem Details検証

## 1. 目的と判定

`koiki-starter-api`からSpring Framework標準のRFC 9457 Problem Detailsを適用し、Validation、
異常JSON、直接発生したJackson 3 `JacksonException`、未処理例外およびSpring MVC例外を、
Customer-like Consumerの実HTTP経路で一貫したerror responseへ変換できることを検証する。

| 項目 | 結果 |
|---|---|
| Phase / status | Phase 1b CP3 COMPLETE |
| Milestone | A Runtime Core COMPLETE / PR CI COMPLETE |
| Framework ownership | `koiki-starter-api`のinternal Problem Details handler |
| Tooling ownership | `MockMvc` fixture、Customer-like Consumer、隔離検証script、CI step |
| Start commit | `4d39c8e`（Phase 1b CP2完了） |
| 判定 | **CP3 COMPLETE。remote PR CIを含むMilestone Aの検証完了** |

検証日時は2026年8月28日 13:40〜13:41 JSTである。

## 2. 設計判断

### 2.1 Spring標準境界

- Spring Frameworkの`ProblemDetail`、`ErrorResponse`、`ErrorResponseException`、
  `ResponseEntityExceptionHandler`を利用する。
- `ProblemDetail`のextension propertyへ安定した`code`を追加する。
- `type`未設定時は`about:blank`を明示し、`type`、`title`、`status`、`detail`、`instance`を
  常に同じtop-level schemaで返す。
- Validationだけは`field`と`message`からなる`violations`を追加し、拒否値は含めない。
- Application固有errorはSpring標準`ErrorResponse`へ変換でき、その`code`と安全なdetailを保持する。
- KOIKI独自の公開Problem Details型、業務例外基底型、annotationまたはController基底型は追加しない。

Spring Frameworkは全Spring MVC例外が`ErrorResponse`を実装し、
`ResponseEntityExceptionHandler`を`@ControllerAdvice`として登録することでRFC 9457応答を有効化できるとしている。
extension propertyはJackson mix-inによりtop-level JSON fieldとしてrenderされる。

根拠はSpring Framework 7.0.9の
[Error Responses](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-ann-rest-exceptions.html)と、
Spring Boot 4.1.1の
[Servlet Web Applications](https://docs.spring.io/spring-boot/reference/web/servlet.html)である。

### 2.2 Error contract

| 経路 | HTTP | `code` | 公開detail |
|---|---:|---|---|
| Jakarta Validation | 400 | `KOIKI-VALIDATION-001` | `Request validation failed.` |
| Jackson起因の入力body異常 | 400 | `KOIKI-JSON-001` | `Request body is not valid JSON.` |
| 直接発生したJackson処理例外 | 500 | `KOIKI-JSON-002` | `JSON processing failed.` |
| 未処理例外 | 500 | `KOIKI-INTERNAL-001` | `An unexpected error occurred.` |
| その他Spring MVC error | Spring標準status | `KOIKI-HTTP-{status}` | Spring `ErrorResponse`の安全なdetail |
| Application-owned `ErrorResponse` | Application定義 | Application定義 | Application定義 |

5xx responseへexception message、exception class名、stack traceまたはJackson parser detailを含めない。
例外はserver logへ記録するが、構造化log、相関ID、trace IDはCP5の責務とする。

### 2.3 Back-offとPublic API

- `koiki.api.enabled=false`ではProblem DetailsとResilienceを含むKOIKI API auto-configurationを無効化する。
- `koiki.api.problem-details.enabled=false`ではProblem Details handlerだけを無効化する。
- Applicationが`ResponseEntityExceptionHandler`をBeanとして提供した場合、KOIKI handlerはback offする。
- Resilienceを独立auto-configurationへ分離し、`koiki.api.resilience.enabled=false`でもProblem Detailsを維持する。
- Public API inventoryはCustomer向けJava型0、configuration property 6件である。

## 3. Customer-like業務HTTP経路

```text
POST /api/1/work-items
  -> WorkItemController
    -> Jakarta Validation / Jackson request conversion
      -> CreateWorkItemUseCase
        -> RFC 9457 response on failure
```

実serverへ`RestTestClient`で接続し、次を確認した。

- 正常入力は201とLocation／response DTOを返す。
- label欠落は400、`KOIKI-VALIDATION-001`、`violations[0].field=label`を返す。
- malformed JSONとunknown propertyは400、`KOIKI-JSON-001`を返す。
- 非対応versionは400、存在しないpathは404で、ともにProblem Detailsを返す。
- test sourceへ隔離した処理例外は500、`KOIKI-INTERNAL-001`を返す。
- parser detail、unknown field名、内部exception message、exception class名をresponseへ露出しない。

失敗専用endpointはConsumerのproduction sourceへ追加していない。Controller→Use Caseの依存方向、Tier 1、
in-memory adapterおよびDB未導入というCP2境界を維持する。

## 4. 隔離artifact検証

Command:

```powershell
pwsh -NoProfile -File build-support/runtime-foundation-verification/verify-cp3-runtime-core.ps1
```

Final result:

```text
CP3 isolated Problem Details, Validation, JacksonException, Consumer HTTP,
and dependency checks succeeded.
```

| 経路 | 結果 |
|---|---|
| KOIKI release unit | 6 projects SUCCESS、52.981秒 |
| Root contract | Architecture Contract 4件、ArchUnit Rules 66件 SUCCESS |
| CP3細粒度fixture | 12 tests SUCCESS |
| Problem Details back-off | 全体無効、Problem Details単独無効、Application-owned handler置換 SUCCESS |
| Error paths | Validation、malformed／unknown JSON、直接Jackson、未処理例外、Application `ErrorResponse` SUCCESS |
| Consumer unit | Tier 1 Use Case 2 tests SUCCESS |
| Consumer architecture / HTTP | 8 tests SUCCESS、Consumer全体20.918秒 |
| runtime dependency tree | 3 projects SUCCESS、9.833秒 |
| dependency boundary | Jackson 3／MVC／Validationあり、WebFlux／Reactor／Security／JPA／Modulith runtimeなし |
| cleanup | 隔離repositoryとnegative probeを削除 |

CDS archive差、Surefire native stream、Mockito dynamic agentおよびcompile-only JPA provider不在の既知warningは
test failureではない。全testはfailure、error、skip 0で終了した。

## 5. CI境界

`.github/workflows/ci.yml`の`Verify (ubuntu-24.04)`へCP3隔離検証scriptを追加した。
Pull Request上では既存Root、Feature Template、NullAwayに続き、Starter細粒度fixtureと独立Consumerを
空のMaven repository経由で検証する。

Draft PR #24へpushしたcommit `cdfdebe783d2bb6808c10916235e2ff6b8ddf436`を対象として、
2026年8月28日 13:57 JSTまでに次の4 checkがすべて成功した。PR head、remote branchおよびlocal HEADが
同一commitであることも確認した。

- `Verify (ubuntu-24.04)`: SUCCESS。`Verify Phase 1b Runtime Core` stepを含む
- `Public API Compatibility`: SUCCESS
- `Build Runtime Fixture (Java 21)`: SUCCESS
- `Java Runtime Compatibility`: SUCCESS

未完了または失敗したPR checkはなく、Milestone Aのremote CI条件を満たした。

## 6. 結論と次の境界

CP3の完了条件であるProblem Details、Validation、`JacksonException`、未処理例外、情報非露出、
positive／negative／restoreおよびCustomer-like実HTTP経路を満たした。既存ADRの前提変更はなく、
ADR追加・改訂は不要である。

remote PR CI成功によりMilestone Aはreview可能である。文書更新commitのCI成功後にDraft PRを
Ready for reviewへ変更する。次の実装CPは、Milestone Aのreview／merge後、最新mainから
`feature/phase1b-data-runtime-integration`を分岐して開始するCP4 PostgreSQL／Flyway二階層である。
