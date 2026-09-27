# Phase 4 PL2 Level 2 非配布検証

**状態:** IN PROGRESS — JDBC / JPAのDB内復旧、別JVMの強制停止・再起動、複数JVMでの同一publication再公開競合と専用復旧JVMの排他候補・限界、stale定期監視、FAILED gauge、再送回数filter、Registry記録対象設定、V5二階層migration候補を検証済み

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
| JDBC profile | `mvnw -f build-support/phase4-level2-verification/pom.xml -Pjdbc verify` | PASS。Surefire 7 tests、Failsafe 11 IT、ともにfailures / errors 0。競合testは重複listener実行を観測する性質の検査であり、安全性のPASSではない |
| JPA profile | 同じコマンドの`-Pjpa` | PASS。Surefire 7 tests、Failsafe 11 IT、ともにfailures / errors 0。同上 |
| 元transactionとlistener失敗 | 承認をDBへ保存しevent発行後、provider stub送信成功直後にlistenerを失敗させる | 承認recordは残り、publicationはFAILED、provider stubのsend recordは1件 |
| 再送と冪等key | `FailedEventPublications.resubmit`で再送 | 両profileでCOMPLETED。provider側にevent IDの一意keyがある場合、send recordは1件のまま |
| 冪等keyがない場合 | 同じ失敗・再送を一意keyなしで繰り返す | 両profileでsend recordは2件。publication再送だけでは外部副作用の重複を防げない |
| 再送回数の候補制限 | 同じeventを2回失敗させ、`ResubmissionOptions.withFilter`で`completionAttempts < 2`だけを再送対象とする。さらに明示的な制限解除で再送 | 両profileで2回目以後はFAILED / attempts 2のまま、解除後はCOMPLETED / attempts 3。provider stubの受理は冪等keyで1件。これはfixtureの運用filterであり、組込みの自動上限・認可を実証しない |
| 完了publication | `CompletedEventPublications.deletePublicationsOlderThan`を実行 | 両profileで対象のCOMPLETED recordが削除された |
| 送信前のOS process強制停止・再起動 | 別JVMのlistenerがPROCESSINGへ入った時点で`destroyForcibly()`し、同じDBで別JVMを起動 | 両profileで停止前のprovider stub記録0件、再配信後1件、publicationがCOMPLETED。再起動時の再公開optionを明示指定 |
| 送信受理直後のOS process強制停止・再起動 | provider stubが別transactionで送信受理を記録した直後、listener完了前に別JVMを`destroyForcibly()`し、同じDBで再起動 | 両profileで停止前の送信記録1件、再配信後も冪等keyにより1件、publicationがCOMPLETED |
| PUBLISHEDのOS process強制停止・再起動 | fixture専用の条件付き`taskExecutor`で非同期listenerの実行前に停止。承認・publicationのDB保存とPUBLISHED、stub送信0件を確認してJVMを`destroyForcibly()`し、同じDBで自動再公開optionを明示した別JVMを単独起動 | 両profileで再公開後COMPLETED、stub送信1件。停止前の`completion_attempts`は1、再公開後は2だった。試行回数1をlistener実行済みの証拠とは扱えない。複数instanceでの自動再公開の安全性は示さない |
| 複数JVMでの同一publication再公開 | 1つ目のJVMをPROCESSING中に強制停止。その後JVM-Aが同じpublicationを再公開してlistener内で停止している間に、JVM-Bを同じDBで起動 | 両profileでJVM-Bも同じlistenerへ入り、`completion_attempts`は少なくとも3。両listenerを強制停止してさらに再起動するとCOMPLETED / provider stub受理1件。**デフォルトの再起動時再公開optionだけでは複数instanceの排他を保証できない**。最初の安全性assertionは両instanceの同時処理を検出して失敗し、観測を固定したcharacterization testへ変更 |
| 専用復旧JVMの排他候補 | 通常JVMの自動再公開を無効化。PROCESSING中に強制停止した記録を、専用復旧JVM-AがPostgreSQL session advisory lockを取得して明示再送し、listener内で停止。JVM-Bは同じlockへ競合。Aを強制停止してJVM-Cが復旧 | 両profileでAはACQUIRED / attempts 2、BはCONTENDED・listener未実行 / attempts 2。A停止後CがACQUIREDしてCOMPLETED / attempts 3、provider stub受理1件。終了後のlock解放も確認。**復旧worker同士**の排他候補として成立 |
| 生存中の通常listenerと専用復旧JVM | 通常JVMのlistenerをPROCESSING中・送信前で停止させたまま、専用復旧JVMが同じevent IDを明示再送 | 両profileで専用JVMはlockをACQUIREDし、同じlistenerへ入った。`completion_attempts`は2、送信は停止位置のため0件。通常listenerと専用JVMの排他は成立しない。両JVM停止後の再復旧はCOMPLETED / stub受理1件 |
| lock接続だけの喪失 | 専用復旧JVM-Aがlockを保持してlistener内で停止中、使い捨てPostgreSQLのlock保持backendだけを`pg_terminate_backend`で終了。AのJVM・listenerは生存したまま、専用復旧JVM-Bを起動 | 両profileでBもlockをACQUIREDし同じlistenerへ入った。`completion_attempts`は3以上、送信は停止位置のため0件。A・B停止後の再復旧はCOMPLETED / stub受理1件。**session lock喪失をprocess停止と同一視できない** |
| 再送対象の保守的な選別候補 | Toolingのguarded入口へ対象publication ID・event ID、観測状態、試行回数を指定。元processの停止確認ファイルを付けず、生存中の通常listenerへ再送要求。元process停止後に古い試行回数、次に一致する確認情報で要求。同じeventの別publicationを検証用に追加 | 両profileで未確認はSTOP_UNCONFIRMED・再送なし、DB状態と合わない選択はSTALE_SELECTION・再送なし。一致したpublication IDだけ明示再送しCOMPLETED、別publicationはPROCESSINGのまま。停止確認ファイルは外部入力であり、内容の真偽をfixtureは証明しない |
| guarded復旧中のlock接続喪失 | 元process停止を確認した対象をguarded入口で再送し、listenerを送信前で停止。lock保持backendだけ終了 | 両profileで復旧JVMはLOCK_LOSTを記録して終了コード70で停止し、送信は0件。古い確認情報は次の試行回数に使用不可。新しい停止確認で再復旧しCOMPLETED / stub受理1件。監視間隔中に新workerがlockを得る競合窓や外部provider送信中のfencingは解消していない |
| FAILED観測候補 | fixture内にDB照会のMicrometer gaugeを置き、listener失敗・再送後の値を照合。FAILED recordの`publication_date`を人工的に5分前へずらす | 両profileでFAILED件数が1→0、最古FAILEDのpublicationからの経過時間が約5分以上→0。現行gaugeはpublicationからの年齢であり、FAILED状態へ入ってからの滞留時間を測るものではない。metric名・実装方式は未承認の候補 |
| stale判定候補 | 失敗済みpublicationを検証用SQLで古い`PROCESSING`と新しい`PROCESSING`へ変更し、1分の閾値で`markStalePublicationsFailed`を実行 | 両profileで古い記録だけFAILED、新しい記録はPROCESSING。FAILEDを明示再送するとCOMPLETEDになり、冪等keyでprovider stub記録は1件のまま |
| stale定期監視 | 失敗済みpublicationを検証用SQLで5分前の`PUBLISHED`と`PROCESSING`へ変更し、両状態の閾値1分・監視間隔200msをfixtureで設定 | 両profileでscheduled monitorが両記録をFAILEDへ移した。運用閾値として1分・200msを推奨する結果ではない |
| dependency tree | `mvnw -f build-support/phase4-level2-verification/pom.xml -Pjdbc/-Pjpa dependency:tree`をprofile別に取得 | 共通のSpring Boot JPA / JDBC / Flyway / Micrometerに加え、JDBC profileは`spring-modulith-starter-jdbc`と`spring-modulith-events-jdbc`、JPA profileは`spring-modulith-starter-jpa`と`spring-modulith-events-jpa`。いずれもSpring Modulith 2.1.1 |
| V5二階層migration | `TwoTierMigrationIT`でKOIKI / Applicationの別履歴を使い、publication表をそれぞれの所有側に配置。KOIKI側の独立V2と失敗V3も実行 | 両profileでPASS。両配置でKOIKI先行・Application baseline 0・再実行0件。V2はKOIKI履歴だけ進み、失敗V3のDDLはPostgreSQLでrollback。詳細と限界は§3.3 |
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
| PL2-V1 | publication保存後・listener中に実OS processを強制終了し、再起動後の未処理配信を確認 | fixture範囲ではPASS。両profileでPUBLISHED、送信前PROCESSING、送信受理直後PROCESSINGから強制停止・再起動後にCOMPLETED。正式Referenceでの実演は別途必要 |
| PL2-V2 | stale PUBLISHED / PROCESSINGとFAILEDの運用手順、再送回数・同時実行を確認 | 古いPROCESSINGの明示再送、PUBLISHED / PROCESSINGのscheduled monitor、試行回数filter、複数JVM競合を検証。guarded入口は停止確認と正確な状態・試行回数を要求し、不一致を拒否。lock喪失時のJVM停止も検証した。停止確認の真正性、検知前の競合窓・外部副作用fencing、複数運用者の再送、operator権限・監査は未解決 |
| PL2-V3 | FAILED件数・滞留時間metric、event / retry / job間の相関IDとtrace / logを確認 | DB照会gaugeの件数とpublication年齢は検証済み。FAILED遷移からの時間、alert運用、非同期相関とtrace / logは未実施 |
| PL2-V4 | Rule 28のLevel 1拒否を保ち、Level 2だけを許す条件とRule 29のnegative fixtureを設計・確認 | §4にsource照合と検証fixture案を記録。Registry記録対象設定の実動作は両方式でPASS。ArchUnit negative fixture実行と正式Rules変更は未実施・別review |
| PL2-V5 | JDBC / JPAのdependency tree、migration配置、性能・運用差を比較して選定する | 両storeの既存機能試験とdependency差を確認。二階層FlywayのKOIKI所有 / Application所有の両配置、KOIKI側だけのV2追加、失敗したV3のPostgreSQLでのDDL rollbackをToolingで確認（§3.3）。JDBC＋UPDATEをA1 review候補とする。正式schema所有者、成功済みmigrationのrollback、DB方言と性能差は未決定 |

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

### 3.2 再送対象選別とlock喪失の追加検証

Toolingのguarded入口を追加し、対象publication ID・event ID、観測したpublication状態・`completion_attempts`、
停止確認ファイルの組を要求した。確認ファイルは`publication ID|event ID|状態|試行回数`に結び、
次の試行への使い回しを拒否する。さらに専用復旧lock取得後、DBの観測状態と試行回数が
一致する記録1件だけを再送候補とし、再送predicateもpublication ID・状態・試行回数を照合する。
同じevent IDを持つ別publicationを追加した試験では、対象だけCOMPLETEDとなり、別記録はPROCESSINGを維持した。
欠落した停止確認は`STOP_UNCONFIRMED`、
DBとの不一致は`STALE_SELECTION`として副作用なしで止める。これはoperatorや運用基盤が
元processの停止を正しく確認したという**外部入力を前提**にした選別であり、
ファイルを置いたこと自体は元processの停止証明にならない。

guarded復旧は待機中にlock connectionを確認し、喪失時に`Runtime.halt(70)`で
専用JVM全体を停止する。lock保持backendだけを終了した別JVM試験では、
送信前の停止位置で`LOCK_LOST`、終了コード70、stub送信0件を両profileで確認した。
ただしconnection喪失から検知までの間はlockを再取得したworkerと重なり得る。
listenerが外部送信の実行中なら即時停止でも副作用を取り消せない。正しい停止確認の発行元、
確認対象のprocess識別・有効期限、複数運用者の競合、provider側の冪等性またはfencing、
認可・AuditをA1 blocking reviewで決める。このprobeをproductionの安全保証と扱わない。

### 3.3 V5：storeと二階層migrationの比較

既存のLevel 2 fixtureでは、JDBC / JPAの両profileが同じPostgreSQL schemaとJPA業務transactionで
publication保存、復旧、再送、完了record削除まで通過した。dependency treeでは、共通のBoot JPA / JDBC / Flywayに対し、
Modulith 2.1.1の`starter-jdbc` / `events-jdbc`と`starter-jpa` / `events-jpa`だけがprofile固有だった。
[Spring Modulith公式events文書](https://docs.spring.io/spring-modulith/reference/events.html)によれば、
JDBC storeはJPAを使うApplicationでもpublication永続化をJPA providerから分離できる。
この性質と既存の機能試験から、**JDBC＋UPDATEをA1 blocking reviewの第一候補**とする。
UPDATEは完了recordを保持し、検証済みの明示的な削除を運用設計へつなげられる。
DELETE / ARCHIVE、性能、障害時の運用負荷を横並びで測定した結果ではないため、正式選定ではない。

| store | PL2で確認した範囲 | A1で比較する残件 |
|---|---|---|
| JDBC | `events-jdbc`を追加し、JPA業務transactionと同じDBで復旧・再送・UPDATE完了recordの削除が成立 | publication SQLの所有者、DB方言、運用時の照会・更新負荷 |
| JPA | `events-jpa`を追加し、同じ業務transaction・schema・復旧操作が成立 | JPA providerとの結合を採る理由、entity mapping変更とupgrade責任、運用時の負荷 |

両storeの機能差や性能優劣を示す結果は得ていない。schema所有者の選択はstoreの選択から自動的には決まらない。

追加した`TwoTierMigrationIT`はData Starterと同じKOIKI先行順、履歴表名、Application側baseline version 0を
Flywayの直接呼び出しで再現した。`koiki_flyway_history`と`flyway_schema_history`は別表である。

| 配置候補 | 実行順と確認結果 | 残る判断 |
|---|---|---|
| KOIKI所有 | KOIKI V1がpublication表を、Application V1が業務表を作成。各履歴にSQL 1件、Application履歴にbaseline 0が残る。再実行は両方0件。KOIKI V2でpublication indexを追加してもApplication履歴はV1のまま | Frameworkがschema / version / upgradeを共通契約として引き受けるか。DB方言と配布経路 |
| Application所有 | KOIKI V1はmarkerのみ、Application V1がpublication表と業務表を作成。履歴は分離され、Application baseline 0と再実行0件を確認 | 各Consumerがschema準備とversion整合を負う契約にするか。ReferenceではApplication履歴名のoverrideも考慮 |

KOIKI所有候補で、V3が表を作った直後に失敗するSQLを実行した。PostgreSQLではその表が残らず、
KOIKIの成功済みV1 / V2とApplication V1の履歴は維持された。これは**失敗したDDLのtransaction rollback**であり、
成功済みversionを戻す運用や他DB方言でのrollbackを証明しない。
両配置のSQLはTooling専用であり、正式migrationではない。Data Starter自体を組み込んだ起動検証は
[Phase 1b CP4](phase1b-cp4-data-runtime.md)の別Evidenceを参照する。
V5はその実装を使ったA1 production統合試験でも、store性能比較でもない。

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

### 4.1 V4：非配布ArchUnit fixtureの実行結果（2026-09-27）

`docs/phase4-execution-plan-review`の`def1f58`を開始点とし、cleanな作業ツリーから
`build-support/phase4-level2-verification/`へTooling専用のArchUnit fixtureを追加した。
現行の`koiki-archunit-rules`をtest依存として公開`businessModuleRules(String)`を評価し、
同じtest内の局所的なLevel選択Rule 28候補・直接依存Rule 29候補と比較した。
候補は正式artifactへ含めていない。

| fixture | 現行Ruleと候補の観測結果 | 解釈 |
|---|---|---|
| Level 0 / 1の同期`@EventListener`、直接・meta transactional、`@ApplicationModuleListener` | 現行Rule 28は同期を拒否せず、残る3種を拒否。候補もLevel 0 / 1の拒否を維持 | 既存`businessModuleRules(String)`の意味を保持する必要がある |
| Level 2のinbound event `@ApplicationModuleListener`と直接・meta transactional | Tooling候補はmodule listenerを許容し、直接・meta transactionalを拒否。現行Rule 28はmodule listenerも拒否 | Level選択契約とdirect listenerの許容条件をA1でreviewする。現行Rule 38は正しい配置を拒否せず、application内の誤配置を拒否 |
| 同期listener→`adapter.outbound.external`の直接参照 | 現行Rule 1と候補Rule 29の両方が検出 | 現行Rule 1と対象が重なる。Rule 29には同期listener固有の説明・診断以上の検出範囲が現状ない |
| 同期listener→Application Use Case→Port→外部Adapter | Rule 1も直接依存Rule 29候補も拒否せず | listenerからの静的な直接参照を禁じても、この経路の外部I/O禁止は証明できない |

実行コマンドは
`.\mvnw.cmd -f build-support/phase4-level2-verification/pom.xml -Pjdbc -Dtest=Rule28And29CandidateTest test`。
JDK 21.0.12.1 / Maven WrapperでSurefire **4件PASS**。DBやDockerは使わない規約検証であり、
JDBC / JPA store差の検証ではない。最初の試行では合成Ruleの`FailureReport.toString()`に
全Ruleの説明が入るため、説明中のRule IDを違反IDと誤認した。違反detailだけを読むよう修正して再実行した。
この経緯は合成Ruleを使うtestでの誤判定を防ぐため残す。

Rule 29候補はlistenerを持つ**class単位**の直接依存だけを走査するため、listener以外のmethodが
同じclass内で外部Adapterを参照しても検出し得る。一方、Use Case / Portの実行先はBean構成や
分岐にも依存し、単純なclass依存グラフからlistener実行時のI/Oを断定できない。
正式案ではRule 1との重複、method単位の精度、同期listenerの識別、Level指定のPublic APIと
後方互換をA1 blocking reviewへ渡す。間接経路はUse Caseの呼出先とPort実装をreviewし、
同期listenerを実行する動作testで外部送信・File・Messagingの呼出有無を確認する。
このToolingのPASSを正式Rule採用、DoD 4-1〜4-5 / 4-12のPASS、Gate P4-F通過と扱わない。

全体回帰の`-Pjdbc verify`は通常権限、権限付き実行の順に試したが、両方とも
TestcontainersがDocker Engineへ接続できずSurefireで停止した。権限付きの`docker version`は
Client 29.5.3-rdのみを返し、`npipe:////./pipe/docker_engine`は「指定されたファイルが見つかりません」。
OwnerがRancher Desktop未起動を確認した。起動後、通常権限の`docker version`はnamed pipeへの
接続を拒否したが、権限付きでServer 29.5.3の応答を確認した。AGENTS.mdの承認済み手順に従い、
同じTooling fixtureの全体検証を権限付きで再実行した。

| profile / 実行コマンド | Surefire | Failsafe | 結果 |
|---|---:|---:|---|
| `-Pjdbc verify` | 11件PASS（V4の4件を含む） | 11件PASS | `BUILD SUCCESS` |
| `-Pjpa verify` | 11件PASS（V4の4件を含む） | 11件PASS | `BUILD SUCCESS` |

両profileともTestcontainersがRancher Desktop Engine 29.5.3と`postgres:17-alpine`へ接続した。
初回のDocker停止による失敗は検証環境の未起動が原因で、fixtureの失敗ではない。

## 5. 暫定判断

JDBC / JPAの両方式で、同じJPA業務transaction、Flyway schema、FAILED→再送→COMPLETED、パージ、PUBLISHEDと送信前・送信受理直後のPROCESSING中の強制停止・再起動後配信、人工的なstale判定、Registry記録対象のannotation種別選択が成立した。
運用filterで再送試行を抑制できること、fixtureのFAILED gaugeが測るのはpublicationからの年齢であることも確認した。
複数JVMの同時再公開では同一publicationを別listener invocationが処理した。provider側の冪等性を前提にしても、
同一eventの処理競合が許されるか、再公開を単一実行に制約するかをA1 blocking reviewで決める必要がある。
どちらをKOIKIの正式基盤に置くかは、残る停止窓、通常listenerとの再送競合、運用・migration ownershipのEvidence後に判断する。
外部送信の重複抑止はpublication storeの選択だけでは解決しない。
