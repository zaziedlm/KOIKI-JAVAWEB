# Phase 4 PL2 Level 2 非配布検証

**状態:** IN PROGRESS — JDBC / JPAのDB内復旧、別JVMの強制停止・再起動、stale定期監視、FAILED gauge、Registry記録対象設定を検証済み

**開始baseline:** `7aa8669`（Phase 4 review decisions and planning baseline）

**Ownership:** Tooling fixture。正式Framework / Reference / Customer成果物ではない。

## 1. 承認範囲と環境

[P4-F提案§4.2](../../development/KOIKI-JavaWeb-FW_Phase4_P4-F限定開始Gate提案_v0.1.md#42-pl2の非配布検証案実施承認済み)のOwner承認範囲で、
Root Reactor外の`build-support/phase4-level2-verification/`に独立fixtureを作成した。
Framework Public API、Starter、正式migration、Reference業務code、remote設定は変更していない。

| 項目 | 確認結果 |
|---|---|
| Maven / Java | Maven Wrapper 3.9.16、Temurin 21.0.12.1 |
| Docker | Rancher Desktop Engine 29.5.3。通常のsandbox権限ではnamed pipe接続を拒否されるが、権限付き実行ではDocker Serverへ接続できる |
| PostgreSQL | Testcontainers 2.0.5、local image `postgres:17-alpine`。テストごとに使い捨てcontainer |
| Spring | Repository BOMのBoot 4.1.1 / Spring Modulith 2.1.1。JDBC / JPAをMaven profileで切替 |
| migration | fixture内のFlyway `V1__probe_schema.sql`がevent publication、承認状態、provider stub記録を作成。Spring Modulith JDBCの自動schema作成は無効 |
| cleanup | JDBC / JPAの`verify`後の`docker ps`は0件。Testcontainers containerの残存なし |

## 2. 実施済み検証

| 対象 | 操作 | 結果 |
|---|---|---|
| JDBC profile | `mvnw -f build-support/phase4-level2-verification/pom.xml -Pjdbc verify` | PASS。Surefire 5 tests、Failsafe 2 IT、ともにfailures / errors 0 |
| JPA profile | 同じコマンドの`-Pjpa` | PASS。Surefire 5 tests、Failsafe 2 IT、ともにfailures / errors 0 |
| 元transactionとlistener失敗 | 承認をDBへ保存しevent発行後、provider stub送信成功直後にlistenerを失敗させる | 承認recordは残り、publicationはFAILED、provider stubのsend recordは1件 |
| 再送と冪等key | `FailedEventPublications.resubmit`で再送 | 両profileでCOMPLETED。provider側にevent IDの一意keyがある場合、send recordは1件のまま |
| 冪等keyがない場合 | 同じ失敗・再送を一意keyなしで繰り返す | 両profileでsend recordは2件。publication再送だけでは外部副作用の重複を防げない |
| 完了publication | `CompletedEventPublications.deletePublicationsOlderThan`を実行 | 両profileで対象のCOMPLETED recordが削除された |
| 送信前のOS process強制停止・再起動 | 別JVMのlistenerがPROCESSINGへ入った時点で`destroyForcibly()`し、同じDBで別JVMを起動 | 両profileで停止前のprovider stub記録0件、再配信後1件、publicationがCOMPLETED。再起動時の再公開optionを明示指定 |
| 送信受理直後のOS process強制停止・再起動 | provider stubが別transactionで送信受理を記録した直後、listener完了前に別JVMを`destroyForcibly()`し、同じDBで再起動 | 両profileで停止前の送信記録1件、再配信後も冪等keyにより1件、publicationがCOMPLETED |
| FAILED観測候補 | fixture内にDB照会のMicrometer gaugeを置き、listener失敗・再送後の値を照合 | 両profileでFAILED件数が1→0、最古FAILEDの滞留秒数が0以上を確認。metric名・実装方式は未承認の候補 |
| stale判定候補 | 失敗済みpublicationを検証用SQLで古い`PROCESSING`と新しい`PROCESSING`へ変更し、1分の閾値で`markStalePublicationsFailed`を実行 | 両profileで古い記録だけFAILED、新しい記録はPROCESSING。FAILEDを明示再送するとCOMPLETEDになり、冪等keyでprovider stub記録は1件のまま |
| stale定期監視 | 失敗済みpublicationを検証用SQLで5分前の`PUBLISHED`と`PROCESSING`へ変更し、両状態の閾値1分・監視間隔200msをfixtureで設定 | 両profileでscheduled monitorが両記録をFAILEDへ移した。運用閾値として1分・200msを推奨する結果ではない |
| dependency tree | `mvnw -f build-support/phase4-level2-verification/pom.xml -Pjdbc/-Pjpa dependency:tree`をprofile別に取得 | 共通のSpring Boot JPA / JDBC / Flyway / Micrometerに加え、JDBC profileは`spring-modulith-starter-jdbc`と`spring-modulith-events-jdbc`、JPA profileは`spring-modulith-starter-jpa`と`spring-modulith-events-jpa`。いずれもSpring Modulith 2.1.1 |
| Registry記録対象設定 | 同一eventへtest-onlyの通常`@TransactionalEventListener`と`@ApplicationModuleListener`を登録し、`spring.modulith.events.registry-trigger-annotation`の有無を比較 | 両profileで既定はCOMPLETED publication 2件。`@ApplicationModuleListener`へ限定すると1件。通常listenerは限定後も1回実行されるため、この設定は配信停止ではなく永続記録対象の選択 |

このprovider stubは別transactionで送信受理を保存する模擬境界であり、送信直後のOS停止試験を含めても実mail providerの保証を証明しない。
DoD 4-3を実providerに対して主張するには、providerの冪等key契約または同等の外部副作用抑止策が必要である。
fixtureは単一packageの最小構成であり、Referenceのmodule間イベント境界やArchUnit Rule 28 / 29の成立を証明しない。
stale判定と定期監視のtestはDB状態と時刻を人工的に設定したもので、実process停止からの自動判定までの連続操作、複数instance競合、運用閾値の妥当性を検証していない。

## 3. 未実施と次の確認

| ID | 確認事項 | 現在地 |
|---|---|---|
| PL2-V1 | publication保存後・listener中に実OS processを強制終了し、再起動後の未処理配信を確認 | PASS。両profileで送信前と送信受理直後のPROCESSINGから強制停止・再起動後にCOMPLETED。PUBLISHEDでの停止窓は未確認 |
| PL2-V2 | stale PUBLISHED / PROCESSINGとFAILEDの運用手順、再送回数・同時実行を確認 | 古いPROCESSINGの手動判定・明示再送と、PUBLISHED / PROCESSINGのscheduled monitorは検証済み。再送回数制限、複数instance競合とoperator権限は未実施 |
| PL2-V3 | FAILED件数・滞留時間metric、event / retry / job間の相関IDとtrace / logを確認 | DB照会gaugeの値は検証済み。滞留秒数の増加・alert運用、非同期相関とtrace / logは未実施 |
| PL2-V4 | Rule 28のLevel 1拒否を保ち、Level 2だけを許す条件とRule 29のnegative fixtureを設計・確認 | §4にsource照合と検証fixture案を記録。Registry記録対象設定の実動作は両方式でPASS。ArchUnit negative fixture実行と正式Rules変更は未実施・別review |
| PL2-V5 | JDBC / JPAのdependency tree、migration配置、性能・運用差を比較して選定する | dependency treeと同じfixture Flyway schemaでの機能成立を確認。Framework / Referenceのmigration所有、性能・運用差は未確認。選定は保留 |

[Spring Modulith公式events文書](https://docs.spring.io/spring-modulith/reference/events.html)はpublication lifecycle、stale判定、再送・パージAPIを定義する。
[公式設定一覧](https://docs.spring.io/spring-modulith/reference/appendix.html)ではstaleness閾値の既定は0で、monitorは既定で無効である。正式運用で閾値と再送方法を選ぶ必要がある。
[Spring Modulith公式schema一覧](https://docs.spring.io/spring-modulith/reference/appendix.html#_event_publication_registry_schemas)をfixture migrationの照合元とした。

## 4. Rule 28 / 29のsource照合と検証案（未採用）

[グランドデザイン§21.3](../grand-design/KOIKI-JavaWeb-FW_グランドデザイン_v0.2.md#213-archunit--spring-modulith)では、
Rule 28をLevel 1期間のtransactional listener禁止、Rule 29をLevel 2以降の同期listenerから外部Adapterへの依存禁止とする。
現行`KoikiArchitectureRules.businessModuleRules(String)`には採用Levelの入力がなく、合成ruleは常にRule 28を含む。
したがってRule 28を単に削除するとLevel 0 / 1の拒否が消え、維持するとLevel 2の`@ApplicationModuleListener`を拒否する。
Levelを明示する新しいrule選択契約と、既存APIの意味を維持する移行方式をblocking reviewで決める必要がある。

| 検証fixture案 | 期待する判定と狙い |
|---|---|
| Level 0 / 1の同期`@EventListener` | 許容。現行baselineを保つ |
| Level 0 / 1の直接・meta `@TransactionalEventListener`と`@ApplicationModuleListener` | Rule 28で拒否。現行testの意味を保つ |
| Level 2の`adapter.inbound.event`に置く`@ApplicationModuleListener` | Level 2選択時だけRule 28の拒否対象から外し、Rule 38の配置検査は維持する候補 |
| Level 2の直接`@TransactionalEventListener` | 追跡対象を意図せず増やす恐れがあるため、許容条件をOwner reviewする。現時点では自動許容しない候補 |
| 同期listenerから`adapter.outbound.external` / `file` / `messaging`を直接参照 | Rule 29の負例候補。ただし現行Rule 1がInbound→Outboundの直接依存を既に拒否するため、検出範囲と違反報告の重複を評価する |
| 同期listener→Application Use Case→Port→外部Adapter | 当初Rule 29の直接依存検査では捕捉できない代表例。経路を静的に検出できるか試し、できなければreview checklistと動作testの責任を明示する |

[グランドデザイン§17.5](../grand-design/KOIKI-JavaWeb-FW_グランドデザイン_v0.2.md#175-spring-modulith-level-1-期間の暫定規約)はRegistryの対象を部分選択できない前提を記すが、
[Spring Modulith 2.1.1の設定一覧](https://docs.spring.io/spring-modulith/reference/appendix.html)には
`spring.modulith.events.registry-trigger-annotation`がある。test-onlyの通常`@TransactionalEventListener`と
`@ApplicationModuleListener`を同じeventに登録して比較すると、JDBC / JPAとも既定のpublicationは2件、
`@ApplicationModuleListener`を指定した場合は1件だった。一方、通常listener自体は指定後も実行された。
したがって**annotation種別によるRegistry記録対象の選択は成立するが、listenerの実行を禁止する設定ではない**。
§17.5の「一部だけを除外する段階移行はできない」という前提は、少なくともSpring Modulith 2.1.1の
annotation種別選択に関して見直し候補となる。個別method単位の選択、運用時の設定逸脱、
direct listenerを許すかどうかは別論点であり、既存Rule 28をこの結果だけで緩和しない。

Rule 29の静的検査が直接依存に留まる場合、同期listenerで外部I/Oを実行しないという
[グランドデザイン§17.3](../grand-design/KOIKI-JavaWeb-FW_グランドデザイン_v0.2.md#173-モジュール間連携)の要求を
ArchUnitだけで証明したとは扱わない。上記fixtureと、呼出先Use Caseのreview・失敗経路testを組み合わせる。
ここでは正式Rules、Public API、Reference moduleを変更していない。

## 5. 暫定判断

JDBC / JPAの両方式で、同じJPA業務transaction、Flyway schema、FAILED→再送→COMPLETED、パージ、送信前と送信受理直後のPROCESSING中の強制停止・再起動後配信、人工的なstale判定、Registry記録対象のannotation種別選択が成立した。
どちらをKOIKIの正式基盤に置くかは、残る停止窓、再送競合、運用・migration ownershipのEvidence後に判断する。
外部送信の重複抑止はpublication storeの選択だけでは解決しない。
