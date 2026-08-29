# Runtime Foundation Consumer

Phase 1bのKOIKI artifactを実顧客アプリと同じ依存方向で利用する、Tooling-ownedの独立Maven buildである。
Root Reactorへ含めず、KOIKI Parent、BOM経由の依存管理、StarterおよびArchitecture Rulesを通常の
Maven coordinatesで解決する。

CP1ではTier 1 `workitem` module、application assembly、Spring Boot executable JARおよび
architecture / startup smoke testを追加した。

CP2では同じmoduleにServlet MVC inbound adapterを追加し、`POST /api/1/work-items`からController、
Application Use Case、in-memory adapterへ至るCustomer-like経路を実行する。API Versioning、Jackson 3、
Resilienceは`koiki-starter-api`から適用し、ControllerはRepositoryや永続化modelを直接参照しない。

CP3では実serverへ`RestTestClient`で接続し、同じworkitem endpointのValidation、異常JSON、未対応version、
存在しないpath、およびtest fixtureへ隔離した未処理例外が、内部情報を露出しないRFC 9457 Problem Detailsに
統一されることを確認する。失敗専用endpointはproduction sourceへ追加しない。

CP4では`koiki-starter-data`と`koiki-testing`を通常のMaven artifactとして利用し、Tier 1 `workitem`を
Spring Data JPA経由でPostgreSQL 17へ永続化する。Customer migrationは
`classpath:db/migration/customer`に置き、KOIKI migrationとの実行順と履歴分離はtest-only probeで検証する。

CP5では`koiki-starter-observability`を利用し、HTTPの`X-Request-ID`からMDCへ設定した相関IDを
`@Async` Application Use Caseへ伝播する。Spring Boot組込みstructured loggingのJSON項目、Customer
`TaskDecorator`との共存および同一async thread再利用時の相関ID漏えい防止を実証する。

CP6では`koiki-starter-data-jpa`とActuatorを利用し、総合health、liveness、DB必須Applicationとしての
readinessを実serverで確認する。専用PostgreSQLをpause／unpauseしてUP／DOWN／restoreを隔離し、
OSIV falseでtest-only Entity露出がresponse生成時に失敗することと、明示override時の検出低下を対比する。

CP7ではTier 1 `workitem`が値だけの`WorkItemCreated`を同期発行し、Tier 2 `workreview`がinbound event
adapterからApplication Use Caseへ委譲して同じtransaction内にreviewを作成する。成功時の両module保存と、
受信側invariant違反時の安全な422 Problem Detailsと両module rollbackをPostgreSQLで実証する。Tier 2共有
Entityは識別子同一性とnullable wrapper `@Version`を持ち、実Hibernate未初期化proxy、新規`persist`、
競合状態遷移の拒否を確認する。
Spring ModulithのNamed Interface検証はtest scopeだけに置き、production artifactとruntime dependencyへ
Modulithを追加しない。

CP8では同じexecutable JARを明示的なmaintenance modeで`WebApplicationType.NONE`として起動し、
`ApplicationRunner`の結果をsuccess `0`、contended `10`、invalid argument `64`、failure `1`へ変換する。
Customer-likeな`workitem` taskは専用JDBC connection上のPostgreSQL session advisory lockを処理中だけ保持し、
同一task keyの複数processを排他する。task副作用とmigrationはConsumer所有であり、Framework artifact、
Public Java API、production migrationは追加しない。

正式Reference業務、SecurityおよびFramework内部型は後続Phaseまたは後続CPの成果物であり、
このConsumerへ先行追加しない。
