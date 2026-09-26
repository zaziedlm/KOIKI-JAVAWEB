# Phase 4 PL2 Level 2 非配布検証

**状態:** IN PROGRESS — JDBC / JPAのDB内復旧、別JVMの強制停止・再起動、複数JVMでの同一publication再公開競合と専用復旧JVMの排他候補・限界、stale定期監視、FAILED gauge、再送回数filter、Registry記録対象設定を検証済み

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
| JDBC profile | `mvnw -f build-support/phase4-level2-verification/pom.xml -Pjdbc verify` | PASS。Surefire 7 tests、Failsafe 6 IT、ともにfailures / errors 0。競合testは重複listener実行を観測する性質の検査であり、安全性のPASSではない |
| JPA profile | 同じコマンドの`-Pjpa` | PASS。Surefire 7 tests、Failsafe 6 IT、ともにfailures / errors 0。同上 |
| 元transactionとlistener失敗 | 承認をDBへ保存しevent発行後、provider stub送信成功直後にlistenerを失敗させる | 承認recordは残り、publicationはFAILED、provider stubのsend recordは1件 |
| 再送と冪等key | `FailedEventPublications.resubmit`で再送 | 両profileでCOMPLETED。provider側にevent IDの一意keyがある場合、send recordは1件のまま |
| 冪等keyがない場合 | 同じ失敗・再送を一意keyなしで繰り返す | 両profileでsend recordは2件。publication再送だけでは外部副作用の重複を防げない |
| 再送回数の候補制限 | 同じeventを2回失敗させ、`ResubmissionOptions.withFilter`で`completionAttempts < 2`だけを再送対象とする。さらに明示的な制限解除で再送 | 両profileで2回目以後はFAILED / attempts 2のまま、解除後はCOMPLETED / attempts 3。provider stubの受理は冪等keyで1件。これはfixtureの運用filterであり、組込みの自動上限・認可を実証しない |
| 完了publication | `CompletedEventPublications.deletePublicationsOlderThan`を実行 | 両profileで対象のCOMPLETED recordが削除された |
| 送信前のOS process強制停止・再起動 | 別JVMのlistenerがPROCESSINGへ入った時点で`destroyForcibly()`し、同じDBで別JVMを起動 | 両profileで停止前のprovider stub記録0件、再配信後1件、publicationがCOMPLETED。再起動時の再公開optionを明示指定 |
| 送信受理直後のOS process強制停止・再起動 | provider stubが別transactionで送信受理を記録した直後、listener完了前に別JVMを`destroyForcibly()`し、同じDBで再起動 | 両profileで停止前の送信記録1件、再配信後も冪等keyにより1件、publicationがCOMPLETED |
| 複数JVMでの同一publication再公開 | 1つ目のJVMをPROCESSING中に強制停止。その後JVM-Aが同じpublicationを再公開してlistener内で停止している間に、JVM-Bを同じDBで起動 | 両profileでJVM-Bも同じlistenerへ入り、`completion_attempts`は少なくとも3。両listenerを強制停止してさらに再起動するとCOMPLETED / provider stub受理1件。**デフォルトの再起動時再公開optionだけでは複数instanceの排他を保証できない**。最初の安全性assertionは両instanceの同時処理を検出して失敗し、観測を固定したcharacterization testへ変更 |
| 専用復旧JVMの排他候補 | 通常JVMの自動再公開を無効化。PROCESSING中に強制停止した記録を、専用復旧JVM-AがPostgreSQL session advisory lockを取得して明示再送し、listener内で停止。JVM-Bは同じlockへ競合。Aを強制停止してJVM-Cが復旧 | 両profileでAはACQUIRED / attempts 2、BはCONTENDED・listener未実行 / attempts 2。A停止後CがACQUIREDしてCOMPLETED / attempts 3、provider stub受理1件。終了後のlock解放も確認。**復旧worker同士**の排他候補として成立 |
| 生存中の通常listenerと専用復旧JVM | 通常JVMのlistenerをPROCESSING中・送信前で停止させたまま、専用復旧JVMが同じevent IDを明示再送 | 両profileで専用JVMはlockをACQUIREDし、同じlistenerへ入った。`completion_attempts`は2、送信は停止位置のため0件。通常listenerと専用JVMの排他は成立しない。両JVM停止後の再復旧はCOMPLETED / stub受理1件 |
| lock接続だけの喪失 | 専用復旧JVM-Aがlockを保持してlistener内で停止中、使い捨てPostgreSQLのlock保持backendだけを`pg_terminate_backend`で終了。AのJVM・listenerは生存したまま、専用復旧JVM-Bを起動 | 両profileでBもlockをACQUIREDし同じlistenerへ入った。`completion_attempts`は3以上、送信は停止位置のため0件。A・B停止後の再復旧はCOMPLETED / stub受理1件。**session lock喪失をprocess停止と同一視できない** |
| FAILED観測候補 | fixture内にDB照会のMicrometer gaugeを置き、listener失敗・再送後の値を照合。FAILED recordの`publication_date`を人工的に5分前へずらす | 両profileでFAILED件数が1→0、最古FAILEDのpublicationからの経過時間が約5分以上→0。現行gaugeはpublicationからの年齢であり、FAILED状態へ入ってからの滞留時間を測るものではない。metric名・実装方式は未承認の候補 |
| stale判定候補 | 失敗済みpublicationを検証用SQLで古い`PROCESSING`と新しい`PROCESSING`へ変更し、1分の閾値で`markStalePublicationsFailed`を実行 | 両profileで古い記録だけFAILED、新しい記録はPROCESSING。FAILEDを明示再送するとCOMPLETEDになり、冪等keyでprovider stub記録は1件のまま |
| stale定期監視 | 失敗済みpublicationを検証用SQLで5分前の`PUBLISHED`と`PROCESSING`へ変更し、両状態の閾値1分・監視間隔200msをfixtureで設定 | 両profileでscheduled monitorが両記録をFAILEDへ移した。運用閾値として1分・200msを推奨する結果ではない |
| dependency tree | `mvnw -f build-support/phase4-level2-verification/pom.xml -Pjdbc/-Pjpa dependency:tree`をprofile別に取得 | 共通のSpring Boot JPA / JDBC / Flyway / Micrometerに加え、JDBC profileは`spring-modulith-starter-jdbc`と`spring-modulith-events-jdbc`、JPA profileは`spring-modulith-starter-jpa`と`spring-modulith-events-jpa`。いずれもSpring Modulith 2.1.1 |
| Registry記録対象設定 | 同一eventへtest-onlyの通常`@TransactionalEventListener`と`@ApplicationModuleListener`を登録し、`spring.modulith.events.registry-trigger-annotation`の有無を比較 | 両profileで既定はCOMPLETED publication 2件。`@ApplicationModuleListener`へ限定すると1件。通常listenerは限定後も1回実行されるため、この設定は配信停止ではなく永続記録対象の選択 |

このprovider stubは別transactionで送信受理を保存する模擬境界であり、送信直後のOS停止試験を含めても実mail providerの保証を証明しない。
DoD 4-3を実providerに対して主張するには、providerの冪等key契約または同等の外部副作用抑止策が必要である。
fixtureは単一packageの最小構成であり、Referenceのmodule間イベント境界やArchUnit Rule 28 / 29の成立を証明しない。
stale判定と定期監視のtestはDB状態と時刻を人工的に設定したもので、実process停止からの自動判定までの連続操作、運用閾値の妥当性を検証していない。
複数instanceで同時に再公開した場合の重複listener実行は再現した。送信前で止めたため、実providerの二重副作用を示す試験ではない。
専用復旧JVM同士の正常なlock排他は、既に停止したpublisherからの回復を対象にした。生存中の通常listener、lock接続だけの喪失では重複実行を再現した。送信前の停止なので実providerへの二重送信は測っていない。
再送filterは`completionAttempts`に基づくfixture上の選別である。専用復旧worker間の排他以外の競合、複数運用者からの明示再送、operator認可、上限到達時の通知・監査は未確認。
FAILED gaugeが参照する`publication_date`は初回発行時刻であり、FAILED遷移時刻ではない。DoD 4-4の「滞留」をどの起点で定義するかを運用Ownerと決め、必要なら別の遷移時刻記録を設計する。

## 3. 未実施と次の確認

| ID | 確認事項 | 現在地 |
|---|---|---|
| PL2-V1 | publication保存後・listener中に実OS processを強制終了し、再起動後の未処理配信を確認 | PASS。両profileで送信前と送信受理直後のPROCESSINGから強制停止・再起動後にCOMPLETED。PUBLISHEDでの停止窓は未確認 |
| PL2-V2 | stale PUBLISHED / PROCESSINGとFAILEDの運用手順、再送回数・同時実行を確認 | 古いPROCESSINGの手動判定・明示再送、PUBLISHED / PROCESSINGのscheduled monitor、`completionAttempts` filterによる2回上限候補は検証済み。複数JVMの自動再公開で重複listenerを確認。CP8型lockは復旧worker間で成立するが、生存中の通常listenerとlock接続喪失では重複実行を確認。安全な対象選別・fencing、複数運用者の明示再送、operator権限・監査は未解決 |
| PL2-V3 | FAILED件数・滞留時間metric、event / retry / job間の相関IDとtrace / logを確認 | DB照会gaugeの件数とpublication年齢は検証済み。FAILED遷移からの時間、alert運用、非同期相関とtrace / logは未実施 |
| PL2-V4 | Rule 28のLevel 1拒否を保ち、Level 2だけを許す条件とRule 29のnegative fixtureを設計・確認 | §4にsource照合と検証fixture案を記録。Registry記録対象設定の実動作は両方式でPASS。ArchUnit negative fixture実行と正式Rules変更は未実施・別review |
| PL2-V5 | JDBC / JPAのdependency tree、migration配置、性能・運用差を比較して選定する | dependency treeと同じfixture Flyway schemaでの機能成立を確認。Framework / Referenceのmigration所有、性能・運用差は未確認。選定は保留 |

[Spring Modulith公式events文書](https://docs.spring.io/spring-modulith/reference/events.html)はpublication lifecycle、stale判定、再送・パージAPIを定義する。
[公式設定一覧](https://docs.spring.io/spring-modulith/reference/appendix.html)ではstaleness閾値の既定は0で、monitorは既定で無効である。正式運用で閾値と再送方法を選ぶ必要がある。
[Spring Modulith公式schema一覧](https://docs.spring.io/spring-modulith/reference/appendix.html#_event_publication_registry_schemas)をfixture migrationの照合元とした。

### 3.1 再公開の排他方針候補（Toolingでの検証）

[Spring Modulith設定一覧](https://docs.spring.io/spring-modulith/reference/appendix.html)は
`republish-outstanding-events-on-restart`の既定を`false`とし、複数instance環境での有効化を推奨しない。
[Spring Modulith開発者のmulti-instance回答](https://github.com/spring-projects/spring-modulith/discussions/727)も、
`IncompleteEventPublications`の再送は単一instanceから行うようdistributed lockで制約する方針を示す。
KOIKIでは[Phase 1b CP8](phase1b-cp8-single-execution.md)のPostgreSQL session advisory lock契約を再利用候補とする。

```text
通常instance群: 起動時自動再公開=false。業務eventの初回listener処理は従来どおり。
専用復旧process: 同じDBで固定task keyのadvisory lockを取得。
  ├─ CONTENDED: 再送せず終了・再試行を運用に委ねる。
  └─ ACQUIRED: 復旧対象として選んだ記録だけを明示再送し、対象の終端状態までlock connectionを保持。
                  終了時は同じconnectionでunlock、process kill時はsession切断で解放。
```

Spring Modulith 2.1.1の実JARを`javap -c -p`で照合したところ、
`resubmitIncompletePublications(ResubmissionOptions)`は`processFailedPublications`へ進み、
PROCESSING停止記録を再送しなかった。最初の専用復旧試験はこの選択でlistener未起動となり失敗した。
`resubmitIncompletePublications(Predicate<EventPublication>)`へ変更するとPROCESSINGの再公開が成立した。
fixtureの`ExclusiveRecoveryProbe`はこの差を確かめるための検証用実装であり、正式Framework APIではない。

この候補で保証できたのは**lock connectionが維持される間の専用復旧process同士**の排他である。
生存中の通常listenerへの再送と、lock接続だけを失った復旧processの継続処理は、どちらも別JVMの重複listenerを実際に生んだ。
PROCESSINGのstale判定・閾値だけでは生存判定の証明にならない。運用者による元process停止確認か、別のownership / fencingが必要。
lock接続喪失時のfail-stop / fencing、外部送信の冪等key、運用者認可・Audit、
完了待ちtimeoutと大量件数・batch制御もA1 blocking reviewに残す。
CP8 fixtureや今回のprobeをFramework成果物へ昇格しない。

**A1 reviewへ提出する暫定方針:** 全通常instanceで起動時自動再公開を無効にし、再送入口を認可・Audit付きの
専用復旧処理へ集約する。専用処理同士はCP8型lockで排他する。FAILEDは試行回数filterと運用判断で再送できる。
PUBLISHED / PROCESSINGは、発行時刻だけを根拠に自動再送しない。前処理processの停止確認と対象IDの明示を要する
手動復旧を暫定案とし、停止を確認できない場合は再送を保留する。自動化するなら生存中listenerの判別またはfencingを追加検証する。
lock接続喪失でもJVMが継続する所見により、CP8型lockだけをA1の排他完成条件としない。
送信境界の冪等keyはこの排他方針の下でも必須である。上記はToolingでの推薦案であり、正式運用契約・DoD PASSではない。

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
運用filterで再送試行を抑制できること、fixtureのFAILED gaugeが測るのはpublicationからの年齢であることも確認した。
複数JVMの同時再公開では同一publicationを別listener invocationが処理した。provider側の冪等性を前提にしても、
同一eventの処理競合が許されるか、再公開を単一実行に制約するかをA1 blocking reviewで決める必要がある。
どちらをKOIKIの正式基盤に置くかは、残る停止窓、通常listenerとの再送競合、運用・migration ownershipのEvidence後に判断する。
外部送信の重複抑止はpublication storeの選択だけでは解決しない。
