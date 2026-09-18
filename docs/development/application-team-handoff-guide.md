# KOIKI-JavaWeb-FW アプリ開発チーム向け引継ぎガイド v0.1

## 1. 本書の目的、対象読者、現在の位置付け

本書は、KOIKI-JavaWeb-FWのPhase履歴を知らない業務アプリ開発者が、現在受け取れるものと受け取れないものを理解し、
Referenceを動かし、最初のCustomer-owned業務moduleを設計するための単一入口です。

まず次の表で、本書が誰のための文書で、読み終えた時点でどこまで到達することを想定しているかを確認します。
ここに含まれないものは、本書を読んだだけで提供済みまたは承認済みになったとは扱いません。

| 項目 | 現在の位置付け |
|---|---|
| 対象読者 | KOIKIを採用候補とする業務アプリ開発チーム |
| 現在の状態 | P4-AR5でOWNER APPROVED。P4-AR6で実際のアプリ開発チームが確認するための入力です |
| 最初の到達目標 | 30〜60分でReferenceまたはConsumerの成立を確認し、最初の業務moduleのOwnershipとTierを説明できる |
| 本書に含まれないもの | Project Template、正式release、案件固有実装、production deploymentまたはsupport契約 |

本書は既存の設計・実装・検証文書を、アプリ開発者の行動順に案内します。リンク先の契約を置き換えるものではなく、
Framework Public API、Starter、migration、dependencyまたはSecurity既定を新しく承認するものでもありません。

## 2. アプリ開発者がたどる一本道

次の順番で進みます。途中で判断できない項目があれば、後続を推測で実装せず、§11の境界へ戻って確認します。
この順番は、KOIKIの機能一覧を暗記するためのものではありません。提供範囲を把握し、構成を選び、実物を動かし、
自分たちが所有する最初の業務moduleへ安全に着手するまでを、一つの流れとして扱うためのものです。

1. KOIKIから何が提供されるかを確認します。
2. 現時点で提供されないものを確認します。
3. MVC、REST、React / BFFのどこから始めるかを選びます。
4. 必要なStarterを選びます。
5. Referenceを動かして、FrameworkとApplicationの接続を観察します。
6. 最初のCustomer-owned業務moduleを設計します。
7. Referenceからコピーせず、自案件で設計するものを確認します。
8. Framework境界付近の機能を分類します。
9. 失敗時は回避設定を足す前に原因を切り分けます。
10. アプリチームで決めることと、Framework側へ戻すことを分けます。

## 3. KOIKIが現在提供できるもの

### 3.1 Frameworkの利用候補

次の表は、Framework側に存在するものをすべて導入するための一覧ではありません。左列で提供候補を特定し、中央列で
アプリチームが何に利用できるかを確認したうえで、右列の境界を守れるものだけを選びます。特に、利用可能であることと、
正式releaseまたは案件必須機能であることを混同しないようにします。

| 提供候補 | アプリチームでの用途 | 利用上の境界 |
|---|---|---|
| Parent / BOM | Java、Spring Boot、dependencyおよびbuild基準を揃えます | 現在は正式なCustomer向けreleaseではありません |
| API / Data / Data JPA / Observability Starter | Web API、永続化、JPA、観測性の基礎を構成します | 必要なStarterだけを選びます |
| Security / Audit / Identity / Session JDBC Starter | default deny、Audit、local Identity、server-side Sessionを構成します | route、Role / Permission、provisioning、IdP設定はCustomer Ownershipです |
| Web MVC Starter | Thymeleaf HTML主軸のMVCとKOIKI Web resourceを利用します | Customer画面、Form、View DTO、UXはCustomer Ownershipです |
| Architecture Contract / ArchUnit Rules | module、Tier、依存方向、internal参照違反を検出します | 規則回避のための除外を案件側で暗黙に追加しません |
| `koiki-testing` | 公開契約に対するtest支援を利用します | production runtime、Reference fixtureまたは万能test kitではありません |

現行formal release unit候補は15 projects / 12 JARとして検証済みですが、一般提供repository、正式version、support条件は
未確定です。利用開始前にP4-AR6と後続release Gateの判断を確認してください。

### 3.2 実行して学べる資材

次の表は、実際に動かしてKOIKIの使い方を理解するための資材を示します。中央列は「ここから学ぶこと」、右列は
「この資材を何として扱ってはいけないか」です。ReferenceやConsumerが動くことを確認しても、そのcodeや構成を
そのままCustomer Applicationへコピーしてよいという意味にはなりません。

| 資材 | ここから確認・学習すること | このようには扱わない |
|---|---|---|
| [Reference Application](../reference/README.md) | MVC、REST、Identity、master、expense、DB、Audit、migrationの接続 | Project TemplateまたはCustomer業務のコピー元 |
| [Runtime Consumer](../../build-support/runtime-foundation-consumer/README.md) | Root外Consumer、HTTP、JPA、event、health、maintenance process | 配布artifactまたはCustomer CIの必須aggregate |
| [Security Consumer](../../build-support/security-foundation-consumer/README.md) | Customer Security chain、fallback、Identity / Audit / Session | production user、Role体系、IdPまたはcredential提供物 |
| [Feature Templates](../../build-support/feature-templates/README.md) | Tier 1 / 2の最小構造とquality gate接続点 | Application全体を作るProject Template |

Reference、Consumer、browser / API harness、demo seedおよびFeature Templateは、理解・検証用の資材です。
正式Framework release unitには含めません。

## 4. 現時点で提供しないもの

未提供であることは、案件で採用できないことや、将来も提供しないことを直ちに意味するものではありません。一方で、提供時期や責任境界が
決まっていない機能を「存在する前提」で設計すると、後からFramework本体のfork、Security既定の迂回、案件固有実装の
共通化といった問題につながります。このため、現在利用できるものと同じ重要度で非提供範囲を確認します。

次の項目は、「すでにKOIKIから受け取れる」とは解釈しません。

- Customer Project Template、完成済みApplication skeletonまたは正式code generator
- 一般提供Framework release、Customer向けartifact repository、versioning / support policy
- React / Next.js production referenceまたは統合済みfrontend workspace
- production user provisioning、固定user / password、test issuer / key / token
- ALB / Cognito / enterprise SAML案件統合またはAWS固有Adapter
- KOIKI-hosted Authorization Server
- MyBatis accounting規約、Oracle、Redis、WebFlux、Spring Modulith Level 2
- production deployment、ECS、network、secret managerまたは監視runbook
- 正式Upgrade / Migration Guideまたは正式OpenRewrite recipe

未提供物が案件に必要な場合も、ReferenceやFramework内部実装をコピーして穴埋めすることは避けてください。
§10の分類を行い、§11のOwnerへ相談します。

## 5. アプリ構成と認証方式を先に選ぶ

最初に画面libraryではなく、browser、frontend server、KOIKI backendおよびIdPのdeployable境界を選びます。詳細は
[UI / Authentication Profile Selection Guide](frontend-authentication-profile-guide.md)を使用してください。

次の表は、画面技術の好みではなく、BrowserからKOIKIまでのcredentialの流れと責任境界を選ぶために使用します。
左列から候補構成を選び、中央の2列でcredentialと現在の実装状況を確認し、右列の判断事項を未決定のまま実装へ
進まないようにします。現在未実装の構成は、Referenceに存在する構成へ無理に寄せず、後続判断へ明示的に残します。

| 開始候補となる構成 | Browserが保持するcredential | 現在の準備状況 | 最初に決めること |
|---|---|---|---|
| Single JAR MVC | KOIKI server-side Session Cookie | Referenceで実装・検証済みです | KOIKI単体でUI / UXまで提供する場合の最初の候補です |
| Same-origin React + KOIKI Session | KOIKI Session Cookie | Architecture上の選択肢ですが、production referenceは未実装です | CSRF、static resource、route境界をP4-AR6で決めます |
| Next.js BFF + KOIKI REST | BFF Session、BFFからBearer JWT | 有力候補ですが、production referenceは未実装です | BFF、Token、logout、Audit、障害時Ownerを決めます |
| Direct Token React SPA | Browser-held access token | Resource Serverの基礎はありますが、SPA production referenceは未実装です | token storage、CORS、renewal、XSS riskを承認できる場合に選びます |
| ALB edge authentication | ALB Session Cookie / injected claims | AWS固有統合は未実装です | edge claimをKOIKI Identityへ接続するAdapterとtrust境界を別途reviewします |

UI topologyが未決定でも、§7のReference MVC journeyはFrameworkの現在地を理解するために実行できます。
選択したprofileが未実装の場合は、Referenceに無理に合わせずF5またはF6として扱います。

## 6. 必要なStarterだけを選ぶ

[Phase 2 Developer Journey](phase2-developer-journey.md)と
[Starter selection guide](../../koiki-starters/README.md)を正本として、Application needから逆算します。

次の表では、左列で案件の必要性を一つずつ確認し、該当する行のStarterを開始候補とします。右列は、そのStarterを
追加してもFrameworkが引き受けない責任を示します。複数行に該当する場合も、将来必要になりそうという理由ではなく、
現在実装する機能に必要な組合せだけを選びます。

| アプリケーションの必要性 | Starterの開始候補 | 守るべき境界 |
|---|---|---|
| Bearer REST API | API + Security。永続化があればData / Data JPA | Bearer-only APIへSessionを自動追加しません |
| Server-side MVC Session | Web MVC + Security + Session JDBC。local Identityを使う場合はIdentity / Audit | login UI、provisioning、Role / Permission、Customer routeは案件側で設計します |
| OIDC login + Session | Security + Session JDBC。Identity linkが必要ならIdentity | registration、issuer、redirect URI、claim mappingはCustomer Ownershipです |
| Business / Security Audit | Audit | 何を業務Auditにするかとtransaction分類はCustomerが決めます |
| JPA persistence | Data + Data JPA | Customer migrationはCustomerが所有し、JPA schema generationへ依存しません |
| Health / log / metric | Observability | 実監視基盤、alert、retentionはCustomer / operations Ownershipです |

依存が解決できることと、そのStarterが案件に必要であることは別です。まず最小構成を選び、未使用Starterを将来用に追加しません。

## 7. 最初の30〜60分でReferenceを動かして読む

### 7.1 時間配分を決めた実行手順

次の表は厳密な性能目標ではなく、初回確認で一か所に時間を使い続けないための目安です。上から順番に実行し、
右列の観測点が得られなければ、次へ進まず§11の診断へ移ります。環境依存のdownload時間などは所要時間から切り分けて記録します。

| 時間の目安 | 実施すること | 成功時に確認できること |
|---|---|---|
| 0〜5分 | §3〜§6を読み、提供物、非提供物、開始shapeを仮置きします | ReferenceとFramework、Toolingと正式artifactを区別できます |
| 5〜10分 | Java 21、Maven Wrapper、Docker、portをpreflightします | 暗黙の端末前提がないことを確認できます |
| 10〜35分 | [Reference Local Run Guide](../reference/KOIKI-JavaWeb-FW_Reference_Application_Local_Run_Guide_v0.1.md)に従い、clean PostgreSQLとpackage済みJARを起動します | Framework / Reference migration後にApplicationが起動します |
| 35〜45分 | 使い捨てdemo dataでSession login、Identity参照、経費一覧を確認します | Session、Permission、master、expense、Auditが同じApplication / DBでつながります |
| 45〜55分 | Referenceのmasterとexpenseを見て、Tier 1 / 2、Controller / Use Case / Domain / Adapterを区別します | 自案件へコピーせず、責務配置を説明できます |
| 55〜60分 | §8のworksheetで最初のCustomer moduleを仮設計します | Ownership、Tier、入口、永続化、検証を言語化できます |

Bearer APIやBrowser / API / DB / Audit aggregateを確認する場合は[Reference index](../reference/README.md)から
focused Toolingを選びます。これらは理解・回帰検証用であり、Customerの日常build commandへそのまま移植しません。

### 7.2 コードをコピーせずに観察する点

Referenceを読む目的は、業務名やclass構成をそのまま持ち帰ることではなく、責務とOwnershipがどのように接続されるかを
具体例で確かめることです。次の点を、自案件ではどの業務語彙と境界に置き換えるかを考えながら観察します。

- MVCとRESTが同じApplication Use Caseを利用し、Controller / Form / REST DTOを共有していないこと
- Tier 1 masterとTier 2 expenseで責務配置が異なること
- Framework migrationとReference migrationが別Ownershipであること
- local SessionとBearer Resource Serverが認証境界を共有していないこと
- DBへ保存した同じ業務IDと状態を通じて、MVCとRESTのjourneyが接続されること
- Identity、Permission、Business / Security Auditが業務処理から迂回されていないこと

## 8. 最初のCustomer-owned業務moduleを設計する

次の順序で1枚の設計メモを作ります。業務用語と判断理由はCustomer repository側へ残します。

配置先はCustomer application repositoryのCustomer base package配下とし、`orders`、`billing`のような業務能力名で
module境界を作ります。Framework repository、`org.koikifw.*`、Starter、Referenceまたは`build-support`配下にはCustomer業務codeを
置きません。単一Application内のpackage moduleから開始し、別Maven artifactや別deployableへの分割は、独立release、運用、
Securityまたはscaling境界が必要になった場合に別途判断します。

```text
Ownership
  -> Business module
    -> Public boundary
      -> Tier
        -> Responsibility placement
          -> Persistence / read model
            -> Module collaboration
              -> View / API boundary
                -> Verification
```

この順序は、classやpackage構造から先に決めることを避けるためのものです。次の表を上から順に確認し、左列の
判断項目ごとに右列の問いへ答えます。答えはCustomer側の設計記録へ残し、FrameworkやReferenceの実装を見たという
理由だけで選択を固定しないようにします。

| 判断項目 | アプリチームが答える問い |
|---|---|
| Ownership / module | どの業務能力がこの変更の正本を持つか。技術レイヤー名ではなく業務名で分けられるか |
| Public boundary | REST、MVC、event、batchのどれを公開し、他moduleへ何を見せないか |
| Tier | 単純CRUDならTier 1から始めます。3状態以上の遷移、複数Entity不変条件、複数Use Caseで共有する業務規則があればTier 2を検討します |
| Responsibility | Controllerは受付、Use Caseはtransactionと調整、Tier 2 Domainは不変条件、AdapterはDB / 外部I/Oになっているか |
| Persistence | 更新系はJPAを現在の既定とします。MyBatisはadoption triggerとblocking review前には選びません |
| Read model | 単一集約はJPA射影、複数集約・集計はJdbcClientを候補とし、更新・認可判断へは流用しません |
| Collaboration | module間command整合は同期Domain Eventを基本とし、他moduleのRepository / Adapterを直接呼びません |
| View / API | Domain Model / JPA Entityを外部へ出さず、MVC DTOとREST DTOを用途別に所有します |
| Verification | Domain / Application / MVC or REST / Persistence / Eventの成功・拒否経路とArchitecture Rulesをどう確認するか |

生成構造を評価したい場合に限り、[Feature Templates](../../build-support/feature-templates/README.md)を参照してください。生成moduleは
Customer Ownershipですが、現在のTemplateは正式なCustomer向けProject Templateではありません。

## 9. ReferenceやToolingからコピーせず、自案件で設計するもの

次の表は、再利用を一律に禁止するものではなく、OwnershipやSecurity境界を壊すコピーを防ぐために使用します。
左列に該当するものを利用したくなった場合は、中央列で問題となる理由を確認し、右列の代替行動へ置き換えます。
Referenceの設計意図を学ぶことと、Reference固有の成果物を自案件の正本にすることは別です。

| コピーしないもの | コピーしてはいけない理由 | 代わりに行うこと |
|---|---|---|
| Referenceの業務class、DTO、Entity、Form | Reference固有の業務語彙と受入条件を持っています | §8でCustomer業務から設計します |
| Reference migration / seed /固定UUID | schemaとdataのOwnershipが混在します | Customer migrationとprovisioningはCustomerが所有します |
| demo user、password、test key、issuer、token | 使い捨てtest fixtureであり、production契約ではありません | IdP / provisioning / secret供給を案件環境で設計します |
| browser / API / E2E harness | 非配布Toolingであり、案件のtest strategyそのものではありません | 案件riskに応じたunit、application、integration、browser testを選びます |
| `org.koikifw.*.internal`またはFramework Repository / Entity | Public APIではなく、upgrade互換性を持ちません | Starter、Public API、Spring標準または承認済み拡張点を使います |
| Framework sourceのforkまたは同責務の横付け実装 | Security、Audit、migration、upgradeの正本が二重化します | §10で分類し、Framework gapであればblocking reviewへ戻します |

## 10. Framework境界付近の作業を分類する

Framework境界に近い要求が出たときは、すぐにFramework改修またはDIによる差替えを選ばないようにします。次の表を上から確認し、
最初に成立する分類を採用します。左列は分類名、中央列は適用できる条件、右列はアプリチームが取る行動です。
`Framework gap`となったものが自動的にFramework実装になるわけではありません。必要性と共通性をblocking reviewで判断します。

| 分類 | この分類を使う条件 | アプリチームが取る行動 |
|---|---|---|
| Standard use | Spring標準、Starter、公開property、Public APIで実現できます | 通常のCustomer実装として進め、Architecture Rulesとtestを通します |
| Approved extension | Frameworkがinterface / SPI / conditional defaultとsemanticsを明示しています | 公開された差替条件とcontract testを守ります。DI可能という理由だけでは差し替えません |
| Customer isolation | 案件固有の業務、Adapter、BFF、cloud設定として分離できます | Customer module / deployableへ隔離し、protocol、Security、Audit、運用Ownerを明示します |
| Framework gap | 安全な公開境界がない、またはFramework変更が必要です | 無理に統合せず、影響と必要契約を記録してFramework側のblocking reviewへ戻します |

`@Primary`、bean name shadowing、bean definition overriding、内部class継承または起動順序依存は、承認済み拡張点ではありません。
暫定bridgeが必要な場合は、Customer隔離、Owner、期限、削除trigger、既知riskおよび再review日を記録します。

## 11. まず診断し、その後で対応するOwnerを決める

### 11.1 最初に確認する診断入口

次の表は、症状から最初の確認先を選ぶためのものです。左列で症状を特定し、中央列の順で事実を集めます。
右列は、一時的に動かすためであっても避けるべき回避策を示します。原因をFramework、Customer実装、環境、Toolingの
いずれかへ分類できるまでは、Securityやmigrationの既定を無効化しません。

| 症状 | 最初に確認すること | 避けること |
|---|---|---|
| dependency / build failure | JDK、Wrapper、repository、BOM / Parent、artifact version、Architecture / NullAway message | source JARやFramework classをCustomer repositoryへコピーしません |
| startup / DB failure | Docker / DB、datasource、port、Flyway、JPA validation、required property | migration、JPA validationまたはhealth checkを無効化しません |
| 401 / 403 | 選択profile、SecurityFilterChain matcher / order、issuer / audience、Session / CSRF | fallback deny、CSRFまたはclaim検証を広域に無効化しません |
| browser 409 | version、独立操作、再読込導線、Domain状態 | optimistic lockを迂回して後発更新を上書きしません |
| Audit不足 | actor、resource、action、result、transaction分類、rollback | application logだけを監査正本にはしません |
| framework境界で実装不能 | Standard use / Approved extension / Customer isolationの有無 | internal参照や偶発的DI overrideでは接続しません |

### 11.2 判断のOwnership

次の表は、アプリチームが自律的に決められる範囲と、Framework / Architecture reviewへ戻す境界を対にして示します。
左列の判断に見えても、実現のために右列の変更が必要になった時点で、Customer内部の通常実装として進めるのを止めます。

| アプリチームが決めること | Framework / Architecture reviewへ戻す条件 |
|---|---|
| 業務module、Tier、画面、API DTO、状態遷移、Customer migration | Public API、Starter、Framework property / default、Framework migrationの変更が必要 |
| route、Role / Permission、claim / scope、IdP client設定 | default deny、Identity、Audit、Session、CSRF / CORS契約を変える必要がある |
| Customer Adapter、外部API、BFF、deployment、monitoring | Framework内部実装へ依存しないと接続できない |
| 案件固有test、SLA、runbook、provisioning | 複数案件へ共通化する契約またはsupport義務が生じる |
| Customer隔離した暫定bridge | bridgeの期限、risk、削除triggerまたはupgrade影響を管理できない |

迷った場合は、実装案だけでなく、必要な業務能力、選択済みprofile、期待するPublic contract、Security / Audit / migrationへの
影響、Customer隔離の可否、期限を添えて相談してください。

## 12. P4-AR5引継ぎ受入チェックリスト

P4-AR5では本書が次を満たすかをArchitecture Ownerが確認し、P4-AR6では実際のアプリ開発チームが同じ項目を確認します。
次のチェックはP4-AR5のOwner確認結果です。P4-AR6の実チーム確認へ完了状態を引き継ぐものではありません。

- [x] Phase履歴を知らない開発者が、本書を単一入口として開始できます。
- [x] 利用可能なFramework artifactと、Reference / Consumer / Toolingの検証専用資材を区別できます。
- [x] 30〜60分程度でReferenceまたはConsumerを起動し、成功観測点とcleanupを説明できます。
- [x] MVC、REST、same-origin React、Next.js BFFまたはdirect Token SPAの次の判断先を選べます。
- [x] Customer Ownershipの最初の業務moduleについて、Customer repository内の配置、Ownership、Tier、責務、永続化、公開境界を説明できます。
- [x] ReferenceのDTO、Entity、migration、fixtureをコピーしない理由と代替行動を説明できます。
- [x] 不足機能をStandard use、Approved extension、Customer isolation、Framework gapへ分類できます。
- [x] Security既定を弱めずに診断し、Framework側へ戻す条件を説明できます。

## 13. P4-AR6で使用する実チーム受入記録

実際の受入確認ではCustomer機密情報を記録せず、次の内容だけをEvidenceへ残します。

次の表は、合否だけを付けるものではありません。左列の観点ごとに、実際に起きたことを右列の範囲で記録し、迷い、欠落、
環境差またはFramework gapを次の改善へつなげます。個人名や案件機密ではなく、再現可能な事実と次のOwnerを残します。

| 記録する観点 | Evidenceへ残す内容 |
|---|---|
| 参加者の前提 | 担当roleとKOIKI経験有無。個人情報は不要 |
| 最初の入口 | 最初に開いた文書と、単一入口として迷わなかったか |
| 実行したjourney | Reference / Consumer、所要時間、成功観測点、cleanup結果 |
| 選択した方向性 | MVC / REST / frontend profileと、未決定事項 |
| 最初のmoduleの説明 | Ownership、Tier、責務、persistence、verificationの説明結果 |
| 境界の理解 | copy禁止、Tooling非配布、Customer / Framework判断境界の理解 |
| blockerまたは曖昧点 | 再現不能、用語不明、手順欠落、Framework gapと影響 |
| 後続対応のOwner | Customer、Framework、joint、Architecture reviewのいずれか |

P4-AR6では理解不足を開発者の責任にせず、文書、配布、環境、Framework gapまたは案件判断のどこに原因があるかを分類します。

## 14. 続けて参照する正本文書

本書は入口であり、詳細な契約の正本ではありません。構成や実装方針を決めるときは、該当する次の文書へ進んでください。
記述が競合して見える場合は、本書だけで解釈を固定せず、上位Architecture、ADRおよび最新Evidenceを確認してください。

- [Phase 2 Developer Journey](phase2-developer-journey.md): Starter、Security profile、Public seam、診断、検証
- [UI / Authentication Profile Selection Guide](frontend-authentication-profile-guide.md): MVC / React / BFF / Token / ALBの選択
- [Reference index](../reference/README.md): 業務仕様、Local Run Guide、focused / critical verification
- [Repository Architecture](../architecture/KOIKI-JavaWeb-FW_Repository_Architecture_v0.1.md): Repository、Ownership、Public / internal境界
- [Grand Design](../architecture/grand-design/KOIKI-JavaWeb-FW_グランドデザイン_v0.2.md): 上位ArchitectureとFramework昇格条件
- [P4-AR5 Evidence](../architecture/validation/pre-phase4-p4-ar5-developer-handoff-readiness.md): handoff inventory、rehearsal、finding、close review
