# Phase 4 S1 R0：Reference非同期経路の限定採用・R1開始判断レビュー案（2026-10-09）

**状態：DRAFT / OWNER REVIEW PENDING。** R0資料作成の指示に基づく提案。Reference Level 2 runtime、依存、code、SQL、正本改訂、検証の開始承認は未取得。本書の作成で開始制限を解除しない。

**現行位置（2026-10-10）：規約限定改訂・必要最小限の検証はOwner受入済み。** 次は[区切り・残作業引継ぎ](phase4-s1-r0-closeout-remaining-tasks-handoff-20261010.md)に従い本草案を再確定する。以下のsourceは草案作成時の基点で、次回開始sourceではない。§6〜7の件数・回帰範囲・資源／予算は未承認候補であり、現行実装・最小ケース方針へ合わせ直す。

**source：** `feature/phase4-s1-reference-foundation` / `fe93b5da76a85fd6a4c41409c725c35661fac007`。資料作成開始時のworktreeはclean。通常build対策22件のlocal commit後を基点とする。remote操作は本作業に含まない。

**入力：** [S1目的対応表](phase4-s1-purpose-dod-stage-mapping-20261009.md)、[軌道修正のOwner採用§9](phase4-s1-async-reference-direction-correction-draft-20261008.md#9-ownerによる方針採用2026-10-09)、[S1完遂方針](phase4-s1-completion-direction-decision-20261002.md)、[DoD実演計画](phase4-s1-dod-demonstration-plan-20261005.md)、[通常build対策の最終受入§24](../architecture/validation/phase4-s1-b2-normal-build-remediation-20261009.md#24-最終結果のowner受入承認)。

**順序訂正（2026-10-09）：** Owner指示によりReference限定のRule違反例外を推奨しない。[Rule 28／29 Level選択対応レビュー案](phase4-s1-r0-rule28-29-level-selection-owner-review-20261009.md)の審査→規約限定実装・検証→Owner結果受入・source固定→本票のR1開始条件再確定→R1個別開始判断の順に進む。本票のR1 case・予算は後段候補で、規約改訂の実行枠と混用しない。

## 1 Owner判断の対象と推奨案

ReferenceだけにModulith JDBC publication registryを限定採用し、既存expense承認から値eventを発行してnotificationへ接続する。最初はpackage済みReferenceと独立したローカルprovider stubで正常経路を成立させる。既存permitのconsume／close接続はR4へ残す。

**残る開始前事項：本票を受入済みLevel選択契約へ合わせ、R1の採用条件・必要ケース・資源枠を再確定する。** 未指定／Level 0／1の拒否を維持する規約改訂と、指定moduleのLevel 2選択は成立・Owner受入済み。規約検証を開始前の未成立事項として残さない。

| 判断ID | 今回の推奨案 | 承認される範囲／後段の境界 |
|---|---|---|
| R0-01 Ownership／依存 | Reference限定、既存BOMのModulith `2.1.1`、publicationはJDBC、expenseは従来のJPAを維持 | Framework Starter／API／Rules／BOM／Parent／Root Reactorを変更しない。Customer採用は別判断 |
| R0-02 通常構成保護 | Maven profile `s1-reference-async`と明示起動設定の二段階選択。通常はevent発行・listener・registry・追加migrationを無効 | §3の4構成を実測し、無効時の副作用を防ぐ。profileを付けただけで通常起動を有効化しない |
| R0-03 event／通知key | expense所有の値record、業務承認と同じtransactionでpublicationを保存。通知keyは承認事象から導出 | §4のkey／payload／相関契約をR1の基本契約にする。復旧・保持条件はR3／R5で追加判断 |
| R0-04 publication／migration | Reference-owned専用schema、Reference Flyway追加migration、UPDATE完了方式 | 既存V1〜V4とFramework schemaを変更しない。B1／B2 viewの正式昇格をしない |
| R0-05 provider接続 | Referenceの送信Port／ローカルHTTP Adapterと、Tooling test所有の独立受理stub | 外部provider通信・実送信先・秘密設定・配信完遂の実運用保証は対象外 |
| R0-06 正本／ADR | §8の限定差分を承認後に反映し、Reference限定採用のADRを作成 | Phase 4全体、Framework Level 2正式採用、R2〜R5の開始を承認しない |
| R0-07 R1限定開始 | 正本・承認記録のsource固定、clean source、R0-09成立・preflight成立後、§6〜7の範囲で作成・検証 | 新規5 class／20 case候補、既存回帰と専用S1を有限実行。結果受入・local commit・remoteは別判断 |
| R0-08 reviewの粒度 | 本票に開始条件・上限・対象を集約。小さな実装調整は承認範囲内で処理、段階出口でEvidence／manifestを集約 | 不一致・失敗・scope拡大・予算超過は停止して訂正案を提示。追加実行は残時間だけで許可しない |
| R0-09 Rule 28／29 blocking review | §3.3の正式Level選択対応を先に審査する。未指定Level 0／1の拒否を維持し、指定moduleだけLevel 2を許容 | 別票でFramework Rules／Public APIの限定改訂を個別判断。受入・本票再確定まではR1停止。Reference違反例外を採らない |

R0-09の規約契約と条件付きR0-B限定開始は別票§9.2で承認済み。規約実装・必要検証と結果受入は2026-10-10に完了した。本票R0-01〜08／R1開始は未承認。規約の承認・結果受入からReference開始を推定しない。

## 2 現行実装からの追加点

現行[ExpenseApplicationService](../../koiki-reference-app/src/main/java/org/koikifw/reference/expense/application/ExpenseApplicationService.java)の`approve`は権限・approver scope・expectedVersionを確認し、状態変更、flush、`APPROVE_EXPENSE` Business Auditを同じtransaction内で実行する。承認通知eventはない。[Reference POM](../../koiki-reference-app/pom.xml)にModulith runtime依存はない。既存[DepartmentDeactivating](../../koiki-reference-app/src/main/java/org/koikifw/reference/master/domain/event/DepartmentDeactivating.java)と同期listenerは承認済みLevel 1 vetoとして維持する。

追加は次の経路に限定する。

```text
既存MVC／REST承認 → expense Application［権限・scope・version・状態変更・flush・Audit］
                   └ 同一transactionで値event → JDBC publication保存 → commit
                       → notification event Adapter → notification Application
                         → provider Port → ローカルHTTP Adapter → Tooling受理stub
                         → listener正常終了 → publication COMPLETED
```

承認transaction内のpublication保存失敗は、通知を記録できない状態で承認だけを確定させないためrollback対象とする。commit後のlistener／provider失敗と区別する。失敗分離の実証はR2。R1正常経路だけでDoD 4-1全体をPASSにしない。

expenseの承認経路にだけ発行hookを追加し、共通`executeMutation`を利用するreject／return／settle等で通知を発行しない。expense-ownedの発行Portを経由し、無効時はno-op、選択時はSpring `ApplicationEventPublisher` Adapterを登録する。既存public use case signature、Security annotation、同期veto、Audit必須性を維持する。constructor変更に必要なtestの配線差分は許容する。Architecture testは受入済みの正式Level選択APIへ切り替え、全違反0件を要求する。既存assertionの削除・違反filterを行わない。

notificationのlistenerは`adapter.inbound.event`、実処理はnotification Application、HTTP送信は`adapter.outbound`に置く。expenseのEntity／Application／Repositoryをnotificationから参照せず、共有は公開値eventだけとする。既存permit Application／Port／Repository／DomainとB2凍結Adapterを送信経路へ流用しない。

## 3 依存・選択条件・transaction

### 3.1 依存と起動の案

`spring-modulith-events-api`をReferenceのcompile依存、`spring-modulith-starter-jdbc`をReference専用Maven profile `s1-reference-async`内に置く。versionは既存`koiki-dependencies-bom`の`2.1.1`を利用し、BOMのversion・managed dependencyを変更しない。JPA registry、broker externalization、Modulith observability、追加Framework依存はR1に入れない。

Reference-owned設定候補を`koiki.reference.notification.async.enabled`（既定false）とする。起動初期のReference設定gateで、無効時は次のregistry auto-configurationを除外し、listener／executor／追加Flyway locationも登録しない。除外は既存の他の除外設定と合成する。選択時はstarter存在・専用schema・stub endpoint・資源設定を検査してから登録する。

- `org.springframework.modulith.events.config.EventPublicationAutoConfiguration`
- `org.springframework.modulith.events.config.EventExternalizationAutoConfiguration`
- `org.springframework.modulith.events.jdbc.JdbcEventPublicationAutoConfiguration`

上記の登録名は端末内`2.1.1` JARの`AutoConfiguration.imports`をread-onlyで確認済み。起動gateはまだ実装・検証していない。無効時の除外漏れや、選択時のregistry不成立を見つけた場合、別方式へ自動変更せず停止する。

| artifactと起動設定 | 必須動作 | R1確認 |
|---|---|---|
| 通常artifact／async無効 | 従来起動。追加schema・registry・listener・送信なし | C01 |
| async profileでpackage／async無効 | 従来起動。JARにstarterがあっても追加DDL・送信なし | C02 |
| 通常artifact／async有効 | starter不足を起動初期に拒否。準備・承認操作へ進まない | C03 |
| async profileでpackage／async有効 | 必須設定成立時だけregistryとlistenerを有効化 | C04、P01 |

単なる`@ConditionalOnProperty`によるlistenerの無効化だけでは、starter auto-configurationの起動・DDL抑止の証拠にならない。通常artifactにもAPI型は含まれるが、runtime registry starterは含めない。package検査で区別する。

### 3.2 選択時の設定と分離

publicationに同一DataSource／業務transactionを使用する。JPA承認transactionにJDBC登録が参加することをP01で検査し、独立commitや別DBのpublicationを使わない。listenerは`@ApplicationModuleListener`で別transaction、Reference専用の有限executorで動作させる。既存master同期listenerへ`@Async`を付けない。

選択時の候補設定は`spring.modulith.events.jdbc.schema=kkref_notification_async`、`spring.modulith.events.jdbc.schema-initialization.enabled=false`、`spring.modulith.events.completion-mode=UPDATE`、`spring.modulith.events.republish-outstanding-events-on-restart=false`。registry対象を`ApplicationModuleListener`に限定する。stalenessの3期間はzeroのまま、R1では自動FAILED化・再送scheduler・purge・復旧API受付を登録しない。

Spring Modulith `2.1.1`の公式文書では、JDBC starterはJPA applicationでも利用でき、registry記録は発行元transactionに参加する。listenerの非同期・独立transactionとregistryの関係は[Events](https://docs.spring.io/spring-modulith/reference/events.html)、schema設定・初期化既定値・registry対象設定は[Appendix](https://docs.spring.io/spring-modulith/reference/appendix.html)、listenerの契約は[2.1.1 API](https://docs.spring.io/spring-modulith/docs/current/api/org/springframework/modulith/events/ApplicationModuleListener.html)を確認した（2026-10-09）。公式仕様からの構成提案であり、Reference上の成立証拠はR1で得る。

### 3.3 Rule 28／29：正式Level選択対応を先行させる

草案作成時はLevel選択とRule 29が未実装だった。現行source `c11dbbceb0241f1257f9bfc153105d8f55125c1c`では[BusinessModuleRuleSet](../../koiki-archunit-rules/src/main/java/org/koikifw/archunit/BusinessModuleRuleSet.java)を含む規約改訂・旧API互換性・必要最小限の検証が成立し、Owner受入済み。Level 0／1の共通拒否を維持し、明示選択したmoduleにLevel 2を許容する。[P4-F提案§3](KOIKI-JavaWeb-FW_Phase4_P4-F限定開始Gate提案_v0.1.md)の規約blocking reviewを再度未実施として扱わず、以下の経緯と採用済み契約をR1へ反映する。

当初は指定listenerのRule 28違反1件を認めるReference限定案を推奨候補にしたが、Ownerは局所的な対処の恒久化と非同期取り込みの安定を重視した。2026-10-09の指示を受け、その案は不採用方向へ訂正する。特定FQCN／methodの違反filter・package除外・test無効化を実装しない。

[規約限定改訂レビュー案](phase4-s1-r0-rule28-29-level-selection-owner-review-20261009.md)で、旧1引数APIとLevel 0／1拒否の互換を保ち、module単位の明示Level選択、Rule 28／29、Public API・negative fixtureを先に審査する。Framework変更は本票のR1範囲へ混ぜず、別票の個別開始判断に従う。

規約結果をOwnerが受入・source固定した後、本票を実APIへ合わせる。R1ではnotificationだけLevel 2を選択し、master／expense／identityの未指定拒否と全既存規則を維持する。既存Framework ownership testは変更しない。規約PASSは非同期の実動作・transaction・provider副作用保証を代替せず、R1〜R5の実証を続ける。

2026-10-09、Ownerは[規約票§9.2](phase4-s1-r0-rule28-29-level-selection-owner-review-20261009.md)を承認した。規約契約と文書source固定・preflight条件付きR0-B限定開始は承認済み。規約実装・検証・結果受入・source固定は未了であり、本票のR1開始候補・予算は未承認のまま維持する。

## 4 値event・通知key・provider受理の基本契約案

event候補名は`expense.domain.event.ExpenseApproved`。最小payloadは`eventId`、`expenseRequestId`、承認後の`approvalVersion`、`applicantUserId`、`approvedAt`、payload versionとする。Entity、SecurityContext、認証token、氏名、メールaddress、申請明細、金額、承認理由を含めない。申請者UUIDは識別情報として扱い、raw証拠への不用意な出力を避ける。受取先の実address解決は未実施。

| 項目 | R1基本契約案 |
|---|---|
| 通知key | `expense-approved:v1:{expenseRequestId}:{approvalVersion}:applicant`。承認後の確定versionから生成。publication ID／attempt／process世代／trace IDを含めない |
| 安定性 | 同じ保存eventの再公開では同じkey。別expense・別承認versionは別key。listener増設や受取先追加はkey規則の再review対象 |
| event ID | 承認transaction内で1回生成しpayloadに保存。rollbackした未成立事象は通知しない。keyの主材料にはしない |
| payload整合 | 同keyは同じ業務payloadだけを許可。hashの対象は固定field・version・canonical表現。再要求ごとのtrace／attempt等は対象外 |
| 同key・同payload | stubは原子的な一意登録で初回だけ受理recordを作り、以後は既存受理IDを返す。listenerへ安全に成功扱いできる応答を定義 |
| 同key・異payload | conflictとして拒否し、初回recordを変更しない。送信成功・publication完了とみなさない |
| 受理の意味 | stubの独立した受理記録だけ。最終受取人の配信・閲覧・外部provider保証に読み替えない |
| 相関 | HTTP操作ID → expense ID／承認version → event ID → publication ID／listener ID → 通知key → stub受理ID。run ID／process世代は観測台帳へ記録 |

R1ではstubを別transaction・専用role／schemaで動かし、受理をlistenerのDB transactionと一緒にrollbackさせない。HTTP serverはToolingのtest JVM内に置き、loopbackの動的portだけにbindする。Reference Adapterはこの限定構成でloopback endpointだけを受け入れる。stubの故障switch・受理台帳・provider重複拒否codeをReference JARへ入れない。

async threadで元のSecurityContext／ThreadLocalやB2 caller scopeが使えるとは想定しない。listenerの正当性は内部eventと限定設定で成立させ、既存Auditの採用済み主体区分を利用する。UUIDからFrameworkPrincipalを捏造しない。適合する主体表現が既存Audit APIで作れなければ停止してreviewへ戻す。R1は承認Auditとevent／受理相関を確認し、復旧操作の認可／AuditはR4、実trace伝播の採用はR5へ残す。MDCだけでDoD 4-12をPASSにしない。

## 5 publication所有・migration・ADR

publicationの運用OwnershipはReference notification。製品library由来のtable仕様はModulithに従うが、配置・migration・保持・アクセス権はReferenceが管理する。専用schema候補は`kkref_notification_async`、tableはschema-qualified `event_publication`。既存Toolingの同名tableと分離する。

Referenceの追加location候補を`db/migration/kkref-notification-async`、追加migration候補を`V5__create_notification_async_publications.sql`とする。現在のV1〜V3、選択時のpermit V4、同じReference履歴`kkref_flyway_history`とversion衝突しないことをpreflightで確認する。async選択時だけ既存locationへ追加し、V4を自動選択しない。V4選択済み構成とのlocation併用も確認する。通常propertiesと既存V1〜V4のbytesを変更しない。

DDLは端末内`spring-modulith-events-jdbc-2.1.1.jar`の`schemas/v2/schema-postgresql.sql`を基準に、schema-qualified配置を明示する。確認済みcolumnは`id`、`listener_id`、`event_type`、`serialized_event`、`publication_date`、`completion_date`、`status`、`completion_attempts`、`last_resubmission_date`。最終SQL作成前にJAR hash・採用DDL・indexを記録し、v1 DDLやTooling fixtureをコピーして成立扱いしない。UPDATE方式なのでarchive tableは追加しない。

本票はSQLではない。承認後の実装に際し、Flywayの実行順、既存JPA validation、migration履歴、runtime roleのDML、観測roleのSELECT専用化を確認する。使い捨てDB内の限定role作成・grant・cleanupを許可対象とする案で、本番権限変更は含まない。

ADR作成を推奨する。番号は既存ADR台帳との重複確認後に割り当て、内容はReference限定採用、JDBCとJPA業務保存の責務、二段階選択、schema／migration、key、provider stubの保証限界とする。既存ADR-049の同期read-only契約を変更しない。Framework正式Level 2採用ADRに昇格させない。

## 6 R1の変更対象・有限case

### 6.1 pathと作業量

| 対象 | 許容する差分 |
|---|---|
| `koiki-reference-app/pom.xml` | §3のReference限定依存／Maven profileだけ |
| `koiki-reference-app/src/main/java/org/koikifw/reference/expense/` | 値event、発行Port／Adapter／条件付き設定、`ExpenseApplicationService`のapprove hook・配線。新規4 file以内 |
| `koiki-reference-app/src/main/java/org/koikifw/reference/notification/` | listener、Application、provider Port／HTTP Adapter、条件付き設定。新規5 file以内。既存permit codeは変更しない |
| `koiki-reference-app/src/main/java/org/koikifw/reference/`直下の設定領域 | 起動初期gate 1 file以内。既存Security／ReferenceApplicationを変更しない |
| `koiki-reference-app/src/main/resources/` | gate登録metadata 1 file、選択時設定1 file、追加V5 SQL1 file以内。既存通常properties／migrationは変更しない |
| `koiki-reference-app/src/test/` | 下記新規4 class／16 case。既存constructor配線変更は既存3 file以内、`ReferenceArchitectureTest`1 fileの正式Level選択APIへの切替を対象とする。既存件数・全違反0件要求・他のassertion維持 |
| `build-support/reference-e2e-verification/src/test/` | 新規1 class／4 caseとstub／観測helper最大2 file。既存critical journeyとPOMを変更しない |
| `docs/`と`AGENTS.md` | §8正本差分、ADR、R1 Evidence。現行受入結果を上書きしない |

実装／設定／test／SQL／POMは合計28 file以内（既存変更を含む）、文書は12 file以内。候補class名は配置review用で、新規公開Framework API名を固定するものではない。上限内の内部private型分割・命名調整はEvidenceに記録する。対象directory外、既存permit／Security／Framework／Rules／CI変更は停止・別判断。

### 6.2 case候補（新規5 class／20 case）

| class候補／ID | 確認する条件 |
|---|---|
| `ExpenseApprovedEventContractTest` K01〜K04（4） | 値だけのpayload、不正／欠落値拒否、同じ事象のkey安定、別expense／承認versionの識別 |
| `NotificationAsyncActivationTest` C01〜C04（4） | §3.1の4構成。無効時bean／DDL／送信なし、starter不足拒否、有効構成登録 |
| `NotificationProviderContractTest` D01〜D04（4） | 初回受理、同key同payload再要求で受理1件、同key異payload拒否、loopback外endpoint拒否 |
| `ReferenceAsyncArchitectureLevelSelectionTest` A01〜A04（4） | 正式APIでnotification listener許容、未指定moduleのtransactional listener拒否、生annotation／誤配置拒否、他の規約違反拒否。fixtureはproduction packageの通常importに混入させない |
| `PackagedReferenceAsyncNotificationTest` P01〜P04（4） | P01既存HTTP認証・権限で承認→Audit→publication→stub受理→COMPLETED整合。P02別申請者／別expenseの取り違えなし。P03通常artifact／無効起動で従来journey・追加schemaなし。P04 async artifact／無効起動で従来journey・追加schema／送信なし |

case一覧はR1の有限対象であり網羅性証明ではない。D02はprovider契約の直接再要求で、process停止・復旧の実証ではない。E01／E08の正常枝に対応し、E02／E03の失敗分離はR2、E04〜E10の停止／復旧はR3。

通常rootでは新規Reference16件を通常testとして実行し、Root Reactor外のE2E moduleに置くpackage process4件は専用propertyの明示選択だけで実行する。既存S1 64／B2 process28の通常無効を維持し、専用caseのskipをPASSへ加算しない。

## 7 R1 preflight・検証・停止条件の提案

### 7.1 開始前の条件

1. Rule 28／29限定改訂の個別開始・実装検証・Owner結果受入・source固定を先に成立させる。本票を実契約に合わせて再審査し、OwnerがR1限定開始を別途明示した場合だけ、採用された§8正本差分・ADR・承認記録を反映する。
2. その文書sourceのlocal commitは別途Ownerが承認・操作し、branch／HEAD／cleanを固定する。`fe93b5d`の旧artifactを新sourceのpackage証拠として使わない。
3. Git／承認範囲／環境差分を確認する。R1用run ID・専用tmp／m2／classpath・JAR identityを作り、B2固定tmpと混用しない。source hash台帳を作成する。
4. Docker Server、既存container／Java PIDのbaseline、空きmemory／disk、使用port・migration履歴を確認する。必要なnamed-pipe／m2権限は実行環境の承認手順に従う。
5. 依存の存在を確認する。不足取得は既存versionの上記2 artifactと不可欠なtransitiveだけを一覧化し、取得の承認がなければ停止する。別version・provider・agentを自動追加しない。
6. 実効pool／executor／HTTP timeout、process親子・所有台帳、観測接続設定・observer database、秘密の非出力を確認する。条件成立をEvidenceへ記録してから作成・検証する。

### 7.2 新規予算と実行集合

**再確定が必要（2026-10-10）。** 下表は旧候補で、実行指示ではない。通常rootの現行基点は389 invocation＝297実行＋92skip。新規20件とS1専用64件の必要性・実行時期、通常rootと選択非同期経路の資源枠（旧案Ryuk1／client8に対してR0通常rootはRyuk2／client14）を再評価し、R1個別開始票で有限条件を確定する。

旧B2の残時間・再実行枠は利用しない。R1に新規90分を提案する。Owner待ち／文書作成を除く検証・環境管理の累積wall timeで、cleanup予約5分を必ず残す。

| 区分 | 有限実行／時間上限 |
|---|---|
| preflight／準備 | 10分。必要取得は承認済み一覧だけ各1回。準備失敗を反復しない |
| 新規Reference16件・影響回帰 | 15分。新規4 classと既存expense／Architecture／Security関連を1回ずつ。rootと重複したcaseを独立種類へ加算しない |
| 通常root `clean verify` | 旧候補1回／15分。349＋16＝365／実行273の旧集計は失効。現行389を基点に採用する新規case・予定無効を再集計する。専用E2Eはroot件数へ含めない |
| 既存S1専用64件 | 1回／10分。共通専用harnessを明示選択。64全件成立・skip0を確認 |
| 通常／async packageとP01〜P04 | 各artifact準備1回、専用4件1回／25分。2種類のJAR・dependencies・migration identityを区別。既存package済みcritical journey1件も明示実行 |
| 最終package／証拠／cleanup | 10分＋cleanup予約5分。fixture非混入、source／artifact hash、今回のprocess・DB終了を確認 |

合計90分。通常rootで既存B2登録8件を維持し、Tooling B1新規52・B2 process28の専用再実行はR1に含めない。B2対象sourceは不変を照合する。既存critical journeyとS1専用の結果を含めて回帰範囲を報告する。新規20件を成立させるほか、root／影響回帰の集合・重複・予定無効をEvidenceへ明示する。

最大同時JVM4（Maven／Surefire／coordinator／Reference子を含む）、PostgreSQL container1、Ryuk1、今回所有DB接続8、Reference Hikari最大4・最小idle0、observer最大2、stub最大1、準備1。各phaseを直列化し、pool／実接続双方を観測する。専用executor1 thread／queue16、HTTP接続timeout2秒・応答5秒、通常completion待機30秒、起動120秒・正常終了30秒を上限案とする。検証値であり本番SLAではない。

空きmemory8 GiB以上・disk10 GiB以上を開始条件とし、raw合計100 MiB／run全体2 GiB以内、logは各process10 MiB以内とする。準備m2の増分も容量予算へ含める。秘密・接続credential・payload全文を保存しない。run別のsource、JAR、classpath、設定・DDL identity、fresh XML、provider台帳、DB snapshot、Audit相関、process／resource samplesを保存する。

同じ検証の追加再実行は0回を基本とする。code変更後の最初の検証と、予定したroot／package検証の重複は台帳で区別する。失敗、条件不一致、上限到達、cleanup失敗では進行を停止し、原因・有限訂正・残予算・追加回数をOwnerへ提示する。残予算があることを自動再実行の根拠にしない。

### 7.3 cleanupと出口

runが所有するPID／起動時刻／親子／generationだけを管理し、正常終了を先に要求する。未終了時は承認範囲の当該子だけを終了し、結果を保全する。他session／IDEのJavaや既存containerを停止しない。今回のDB／Ryuk／stub・observer接続を閉じ、baselineを保持したことを確認する。秘密を含む一時設定・credentialを除去し、failure rawを後続runで上書きしない。

P01〜P04、K／C／D、新規・既存回帰、通常無効、artifact非混入とcleanupの証拠を1つのR1 Evidenceへ集約する。R1成立候補はOwnerの結果受入へ提示する。DoD 4-1〜4-5／4-12完了、実provider配信、Reference Level 2正式受渡し、Phase 4全体完了は判定しない。

## 8 承認時に反映する正本差分案

以下は変更案であり、この資料作成では正本を変更しない。承認後に限定条件とsource導線を揃える。

| 正本 | 提案する最小改訂 |
|---|---|
| `AGENTS.md` | 既存Level 2開始制限へ「Reference R1のみの個別承認例外」を追加。旧ST-C／B1／B2承認範囲を拡大解釈しない |
| `docs/agent/skills/koiki-project-overview/SKILL.md` | Phase 4限定開始の位置づけへ本票・承認記録・R1開始条件の導線を追加 |
| `docs/agent/skills/koiki-business-feature-work/SKILL.md` | Reference R1だけは承認された非同期値event／listenerを採用できる旨と本票への導線。Framework・Customer一般規則は維持 |
| [P4-F提案§3・Gate判定](KOIKI-JavaWeb-FW_Phase4_P4-F限定開始Gate提案_v0.1.md) | Rule 28／29限定改訂の先行受入を入力に、設置済みGate P4-FのR1追加限定判定を行う。store／依存、後段安全条件を部分審査し、Gate全体・CP-F1全体完了へ読み替えない |
| [PL2判定資料のCP-F1／CP-F2](KOIKI-JavaWeb-FW_Phase4_PL2_P4-F判定資料_v0.1.md) | R1正常経路は部分開始／部分Evidence、停止・復旧未達のままR2〜R5を別判断とする。既存「未達なら後続A2を開始しない」との例外をこの範囲だけ明記 |
| [Phase 4見直し計画](KOIKI-JavaWeb-FW_Phase4実施計画_見直し草案_v0.1.md)・[P4-AR計画](KOIKI-JavaWeb-FW_Pre-Phase4_Adoption_Readiness計画_v0.1.md) | 正式Reference Level 2開始前制限へのR1限定例外、R0〜R5の出口判断、DoD未成立を明記。Reference notificationの現行RICH／JPA SHAREDは維持し、過去のTier 1候補を採用しない |
| Reference限定ADR／本票／目的対応表 | 依存・schema・選択条件の採用理由とOwner判断、source固定、残OPENを相互リンク |

AGENTS追記候補文：

> Architecture OwnerがR0開始判断票のR1限定開始を明示承認した場合に限り、承認済み正本・ADR・承認記録の文書commitとclean source固定、同票preflight成立後、Reference-ownedのexpense承認値event、notification非同期listener／送信Port／ローカルstub接続Adapter、Reference限定Modulith JDBC依存・追加publication migration・条件付き登録と当該有限test／Evidenceを作成・検証できる。通常起動は無効とし、既存Identity／Security／master同期veto／expense認可・Audit・既存permit契約を維持する。Tooling test所有のローカルstub受理・専用schema／role・今回のpackage子JVM・使い捨てDB操作は同票の有限副作用だけに限定する。肯定consume／close、停止後再送・復旧runner、実provider送信、Framework API／Rules／依存・CI・remote、DoD／正式受渡し／Phase 4全体へ拡張しない。検証集合・予算・資源・停止／cleanupは同票に従い、副Agentを使用しない。結果受入・local commit・remoteは別判断とする。

> Rule 28／29のblocking reviewは別の規約限定改訂票に従い、正式Level選択対応の実装・検証・Owner受入・source固定をR1開始前に成立させる。Reference Architecture testはnotificationだけを明示選択し、全規則の違反0件を要求する。特定listenerの違反例外を認めず、未指定moduleのLevel 0／1拒否を維持する。

この文面だけを承認記録なしに挿入して開始可能扱いにしない。開始条件の不一致を発見した場合は改訂を止め、差分をOwnerへ示す。

## 9 R2〜R5に残す判断と未達

| 時点 | 残す判断／証拠 |
|---|---|
| R2開始前 | provider失敗・listener例外と承認保持、認可拒否／Audit失敗／rollback時に通知なし。故障注入を正式codeへ混入させない方式と有限case |
| R3開始前 | 全停止メンテナンス型／稼働中個別復旧型の選択、許容復旧時間・Web停止影響・担当／代行。B2方式を理由に全停止型を既定扱いしない。後者は旧worker fencing等の送信境界保護が必要 |
| R3異常系 | event class／listener ID互換、payload固定・key寿命、provider照会／再要求条件、受理不明の照合、停止位置・終了証拠・起動世代と実配信完遂 |
| R4開始前 | B-C01〜10とD11／D12、旧worker生存／lock喪失・歴史対象閉鎖・別publication横断抑止・照合後競合。consume／close、worker認証／委譲、送信前／受理後Audit失敗、監視／alert／上限／担当の個別契約 |
| R5開始前 | COMPLETED保持／purge単一実行、FAILED／未処理保全、provider key保持期間と再要求可能期間、Audit・観測証拠の寿命、実trace sink・関連伝播と漏えい防止 |

provider受理不明はUNKNOWNとして扱い、DB status・FAILED・lockだけで元listener終了や安全な再送を断定しない。照合後の同key再要求またはHOLD・調査・エスカレーションを、人の判断主体・理由・次の確認とともに記録する。R1の正常結果でD11／D12や復旧の未達を閉じない。

## 10 作成時点の確認とOwner判断欄

今回行ったのはsource・文書・POM・設定・承認経路・Architecture test・端末内Modulith JAR metadata／DDLのread-only確認と公式`2.1.1`文書照合、および本票・目的対応表の文書編集だけ。Maven／Docker・runtime・test実行、code／POM／SQL／正本変更、git add／commit／remoteは行っていない。

2026-10-09、Ownerは「R0でLevel選択に対応したRule 28／29の限定改訂を先に審査する方向」へ順序を踏んで進めると指示した。§3.3の推奨・順序を訂正し、別の規約限定改訂票を作成した。これは規約変更・R1実行開始の承認ではない。

規約限定改訂票の具体案・条件付き実行開始は別票§9.2に承認記録済み。本票R0-01〜08の採否と規約受入後の再確定、R1限定開始の可否・修正条件は未記入。規約票の承認をReference実装承認に読み替えない。
