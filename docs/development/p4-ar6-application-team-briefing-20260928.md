# P4-AR6 業務アプリチーム向け説明・対話資料（2026年9月28日 実施）

**状態:** 2026年9月28日に実施済み。Frameworkの概要説明と方針共有までで、実チーム環境でのbuild / run再現と受入判定は未実施。結果の要約は§8。  
**対象:** KOIKI-JavaWeb-FWを利用する可能性がある業務アプリ開発チーム。  
**説明の軸:** 業務アプリが必要とする機能、KOIKIから利用できる契約、案件側で実装する範囲、最初の開発環境。

本資料は[アプリ開発チーム向け引継ぎガイド](application-team-handoff-guide.md)を説明するための要約です。
対話の結果は[承認済みP4-AR6 worksheet](p4-ar6-actual-team-reception-worksheet.md)へ非機密情報で記録し、
RESTの詳細は[PL1差分台帳](KOIKI-JavaWeb-FW_Phase4_PL1_REST利用境界差分台帳_v0.1.md#4-9月28日-p4-ar6打ち合わせ用の確認票)のQ1〜Q5へ戻します。

## 1. 当日の進め方

| 順 | 伝える・確認すること | この場で得たい結果 |
|---|---|---|
| 1 | KOIKIの目的と現時点の実装範囲 | Framework、Reference、Tooling、Customer実装の違いを共有する |
| 2 | 業務アプリの最初の操作とfrontend構成 | 実チームが必要とする最小journeyと、未決定の認証境界を聞く |
| 3 | VS Code上のRepository配置とMaven artifactの使い方 | 別Repository＋Maven座標の入口が受入可能か確認する |
| 4 | 検証済みの試行と、その限界 | 外部projectでbuild / runできた範囲と、実案件で追加検証する範囲を分ける |
| 5 | 担当と次の行動 | Customer、Framework、共同Evidence、残件Ownerをworksheetへ記録する |

一度の説明会でAPI、認証profile、配布方式の全詳細を決める必要はありません。判断できない事項には
Ownerと次の確認時点を付け、受入の`PASS`へ読み替えません。

## 2. KOIKIは何を提供するか

KOIKIはJava 21 / Spring Bootを基盤とする業務アプリ用Frameworkです。業務moduleの分離と品質検査、
REST・MVC・永続化・Security・Audit・Observabilityの共通基盤をMaven artifactとして利用します。
業務画面、業務API、業務ルール、DB schema、外部システム接続は案件が設計・実装します。
構成の正本は[グランドデザイン](../architecture/grand-design/KOIKI-JavaWeb-FW_グランドデザイン_v0.2.md)と
[Repository Architecture](../architecture/KOIKI-JavaWeb-FW_Repository_Architecture_v0.1.md)です。

| 受け取り候補 | 現在の実装・用途 | 案件が決めること | 参照 |
|---|---|---|---|
| Parent / BOM、Architecture Contract / ArchUnit Rules | Java・依存・module構造・内部参照のbuild / test契約 | Parent継承か案件Parent＋BOM、CIでの検査と違反修正 | [受入ガイド §3](application-team-handoff-guide.md#3-koikiが現在提供できるもの)、[handoff contract §3](../architecture/validation/pre-phase4-p4-ar6-actual-project-handoff-contract.md#3-repository-topology-decision-preparation) |
| API / Data / Data JPA / Observability Starter | REST、Validation、Problem Details、Flyway、JPA、log・request相関の基礎 | API / DTO、Customer migration、DB driver、監視・alert | [Starter一覧](../../koiki-starters/README.md)、[API Starter](../../koiki-starters/koiki-starter-api/README.md) |
| Security / Identity / Audit / Session JDBC Starter | default deny、Spring Security、Identity・Audit・Sessionの契約 | route別認証、Permission、主体対応、IdP設定、provisioning、監査対象 | [Security Starter](../../koiki-starters/koiki-starter-security/README.md)、[Security開発ガイド](phase2-developer-journey.md) |
| Web MVC Starter | Thymeleaf主体のserver-side UIの基礎 | MVCを採用する場合の画面、Form、UX、route | [Web MVC Starter](../../koiki-starters/koiki-starter-web-mvc/README.md) |
| Reference Application | master・expenseの業務例、MVC / REST、DB・Auditをつないだ実行例 | 案件の業務code / DTO / Entity / migrationを新規設計する | [Reference index](../reference/README.md)、[ローカル起動ガイド](../reference/KOIKI-JavaWeb-FW_Reference_Application_Local_Run_Guide_v0.1.md) |

現行のFramework release unit**候補**は15 projects / 12 JARとして検証されています。Reference、Consumer、
検証fixture、frontendはその正式Framework JARに含まれません。Customer向け正式release、managed Maven repository、
version / support条件は後続判断です。現時点の受渡し範囲は[受入ガイド §3〜4](application-team-handoff-guide.md#3-koikiが現在提供できるもの)と
[P4-AR5 Evidence](../architecture/validation/pre-phase4-p4-ar5-developer-handoff-readiness.md)を参照してください。

## 3. FrontendとSecurityの責任境界

現時点で実案件から示された構成は**Next.js/BFFとKOIKI RESTの連携**です。KOIKIはNext.js、React、BFFの
production実装や完成済みfrontend workspaceを提供していません。[認証profileガイド](frontend-authentication-profile-guide.md#6-profile-b--nextjs-bff)の
「BFF SessionとBFF→KOIKI Bearer」は検討候補であり、案件で採用を確定した方式ではありません。

```text
利用者のBrowser
  → 案件所有のfrontend / BFF
    → 案件所有のREST Controller・業務Use Case
      → KOIKI Starterの共通契約・案件所有DB / Audit
```

| 境界 | 案件側で実装・決定すること | KOIKIから利用できること |
|---|---|---|
| Browser ↔ BFF | 画面、BFF Session Cookie、CSRF、logout、tokenの非露出、画面用error表示 | frontendの実装は提供しない。[profile比較](frontend-authentication-profile-guide.md#3-profile-catalog)を設計入力にできる |
| BFF ↔ KOIKI REST | 認証方式、credentialの送信、timeout / 再試行、API・DTO、必要な場合のCORS | [Security Starter](../../koiki-starters/koiki-starter-security/README.md)のdefault denyとSpring標準Resource Server、[API Starter](../../koiki-starters/koiki-starter-api/README.md)の共通error形式 |
| KOIKI Application内部 | 利用者主体と権限の対応、業務scope、不変条件、Customer migration、Business Audit | Identity / Audit / Dataの公開契約。Referenceのclaim変換・Permissionは案件用の既定値ではない |
| 障害・運用 | BFFとAPIの相関、調査担当、監視・alert、secret管理 | Observabilityのrequest相関など。BFFを含むend-to-endの運用成立は実案件で確認する |

RESTを選んでも、認証・認可が自動的に完成するわけではありません。KOIKIのfallbackは未定義routeをdenyし、
案件は自分のrouteに合う`SecurityFilterChain`、認証profile、権限、失敗応答を設計します。
KOIKI APIがBFFを無条件に信用する構成は採らず、選択した認証方式に応じてrequestを検証します。
[PL1差分台帳 §1](KOIKI-JavaWeb-FW_Phase4_PL1_REST利用境界差分台帳_v0.1.md#1-確認した利用境界)は
Framework契約とReference固有実装の差を示します。

Spring Modulith Level 2の非同期通知は現時点の業務アプリ向け提供機能ではありません。
暫定基準はLevel 1維持です。必要性と運用条件は[Level 1 / Level 2業務向け資料](phase4-pl2-level1-level2-business-guide.md)と
[P4-F判定資料](KOIKI-JavaWeb-FW_Phase4_PL2_P4-F判定資料_v0.1.md)で別途扱います。

## 4. VS Codeでのクイックスタート構成案

VS Codeは複数のfolderを一つのmulti-root workspaceに表示できます。**同じIDEで開くことと、同じGit Repository・
Maven Reactorに入れることは別**です。以下は配置の概念案であり、Project Templateや作成済みworkspaceではありません。

```text
development-workspace/                   # workspace親。Git Repositoryではない
├── koiki-javaweb-fw/                     # Frameworkの別Git Repository、固定commit
├── customer-backend/                     # 業務アプリの別Git Repository、Customer所有
├── customer-frontend/                    # Next.js/BFF候補。配置とGit分割は実チームで決定
├── local-artifact-stage/                 # 実行ごとに空で作るMaven local repository、Git管理外
└── evidence/                             # manifest等。sourceとstageの外
```

FrameworkとCustomer backendのGit履歴、POM、CI、credentialを分離します。frontendをbackendと同じ
Customer Repositoryに置くか別Repositoryにするかは実チームが決めます。Customer業務moduleとmigrationは
Customer Repositoryに置き、KOIKI Root ReactorやReference sourceには追加しません。
[handoff contract §3.2〜3.3](../architecture/validation/pre-phase4-p4-ar6-actual-project-handoff-contract.md#32-topology-options)が
R1（正式Maven repository）とR2（移行期の隔離local stage）の判断基準です。

VS Codeでは、FrameworkとCustomer backendのfolderを別rootとして追加し、workspace設定ファイルは
両Repositoryの外へ置く案です。Java / MavenのbuildはCustomer backendのroot POMから実行します。
Next.js/BFFの開発・buildはCustomer frontend側で行います。Framework checkoutは契約確認と固定commitの
artifact stageに使い、Customer業務codeの保存先やCustomer buildの親Reactorにはしません。
説明会後、この構成案を実際の手順として[開発環境構築手順](application-team-development-environment-guide.md)にまとめました。

### JAR参照の考え方

1. Customer POMは`org.koikifw`のParentまたはBOM、必要なStarter / Architecture artifactを**通常のMaven座標**で参照します。JARファイルを`lib/`に手動コピーしません。
2. KOIKI Parentを継承する入口では空の`<relativePath/>`を使い、Framework checkoutの親POMをfilesystemで探索しません。Customer Parent＋KOIKI BOM importも候補ですが、標準入口はP4-AR6で判断します。
3. 移行期のR2では、固定Framework commitからformal release unit候補をrun専用の空のMaven local repositoryへstageします。Customer buildにも同じrepositoryを`-Dmaven.repo.local`で指定し、artifact version・commit・checksumをmanifestで対応付けます。
4. R2 stageは受入検証用の一時成果物です。複数人・CIで継続利用する正式配布先とsupport条件は別途決定します。

次はgreenfield試行で使った**Parent継承方式を基にした依存宣言の概念例**です。Security Starterは
案件向けの追加候補であり、greenfield試行では検証していません。案件で選ぶStarterやversionは
受入時に確認します。`koiki-starter-security`を追加するだけでは案件routeの認証・認可は完成しません。

```xml
<parent>
  <groupId>org.koikifw</groupId>
  <artifactId>koiki-parent</artifactId>
  <version>0.1.0-SNAPSHOT</version>
  <relativePath/>
</parent>
<dependencies>
  <dependency>
    <groupId>org.koikifw</groupId>
    <artifactId>koiki-starter-api</artifactId>
  </dependency>
  <!-- 案件の認証profileを設計する場合の選択候補。明示的なSecurity chainが別途必要 -->
  <dependency>
    <groupId>org.koikifw</groupId>
    <artifactId>koiki-starter-security</artifactId>
  </dependency>
</dependencies>
```

具体的なPOM構成、artifact選択と実行parameterは
[P4-AR6 handoff contract §3.2.1〜3.2.2](../architecture/validation/pre-phase4-p4-ar6-actual-project-handoff-contract.md#321-isolated-maven-repositoryの意味)、
[R2 Tooling README](../../build-support/adoption-readiness-verification/README.md)、
[Starter選択表](../../koiki-starters/README.md)を参照してください。実Customer Repositoryの読取・buildは
[worksheet §4.2](p4-ar6-actual-team-reception-worksheet.md#42-実customer-repository操作の事前承認)に従う事前承認後に行います。

## 5. 外部projectで試したこと

次の2件は説明会の時点では検証ブランチだけに記録されていました。その後PR #39で`main`へ取り込み、
同じ手順を業務アプリチーム向けに並べ直した[開発環境構築手順](application-team-development-environment-guide.md)を追加しました。

| 試行 | 結果と当日の説明点 | 証拠 |
|---|---|---|
| 外部Reference境界 | Initializr生成の独立projectへ既存Referenceを一時的にmaterializeし、KOIKI Parent・Maven座標から99 test、packageをPASS。Framework sourceや通常`.m2`の偶発依存を排除した。Reference sourceを使う検証であり、Customer業務アプリの雛形ではない | [External Reference Boundary Validation](../architecture/validation/adoption-external-reference-boundary-validation.md) |
| 新規greenfield bootstrap | Reference sourceを使わないInitializr生成projectにKOIKI Parent、API / Data / Data JPA Starter、Architecture Rules、Customer所有migrationを追加。3 test、package済みJAR起動、PostgreSQL接続、Customer FlywayとREST応答をPASS。Securityは検証対象外。versioning path不適合による最初の400は契約へ合わせて修正した | [Greenfield Bootstrap Smoke Validation](../architecture/validation/adoption-greenfield-bootstrap-smoke.md) |

これで確認できたのは、外部projectからFramework artifactを利用してbackendをbuild / runする技術的な入口です。
Next.js/BFFとの結合、案件の認証・認可、実チームの環境での再現、正式release / supportはまだ確認していません。
試行ブランチのfixtureをそのままCustomer Project Templateとして配布しません。

## 6. 実チームと決める入口

| 論点 | 対話の最初の質問 | 記録先 |
|---|---|---|
| 最初の業務journey | 利用者が最初に完了させたい操作は何ですか。BFFがKOIKIへ求める最小の読取・更新は何ですか | [PL1-Q1](KOIKI-JavaWeb-FW_Phase4_PL1_REST利用境界差分台帳_v0.1.md#3-p4-ar6へ渡す確認事項) |
| Frontend・認証 | Browser→BFF→KOIKIの各境界で誰のcredentialを扱い、誰が本人・権限を検証しますか | PL1-Q2 / Q3、[worksheet §5.1](p4-ar6-actual-team-reception-worksheet.md#51-単一入口と提供境界) |
| 開発入口 | FrameworkとCustomerを別Repositoryにできますか。Parent継承とBOM importのどちらが既存buildに合いますか | [worksheet §5.4](p4-ar6-actual-team-reception-worksheet.md#54-repository-topologyとartifact受渡し) |
| 最初の実証 | ReferenceかConsumerのどちらから30〜60分のbuild / run / diagnosisを試しますか。実Customer Repository操作が必要なら承認範囲は何ですか | [worksheet §4〜5](p4-ar6-actual-team-reception-worksheet.md#4-実行前確認と承認パケット) |
| 運用と不足 | API / BFFの障害を誰が一次調査し、Frameworkの不足はどこへ戻しますか | PL1-Q4 / Q5、[worksheet §6〜7](p4-ar6-actual-team-reception-worksheet.md#6-phase-4責任分担worksheet) |

回答が得られた範囲だけ、Customer / Framework / joint Evidenceと次のOwnerを記録します。
P4-AR6実チーム受入、AR-D10、Gate P4-AR、Phase 4開始および正式artifact配布の判定は別に残します。

## 7. 参照先の早見表

| 用途 | 資料 |
|---|---|
| アプリ開発者の一本道 | [引継ぎガイド](application-team-handoff-guide.md) |
| 実チーム受入の記録と事前承認 | [P4-AR6 worksheet](p4-ar6-actual-team-reception-worksheet.md)、[P4-AR6 handoff contract](../architecture/validation/pre-phase4-p4-ar6-actual-project-handoff-contract.md) |
| REST / BFFで未確定の点 | [PL1 REST利用境界差分台帳](KOIKI-JavaWeb-FW_Phase4_PL1_REST利用境界差分台帳_v0.1.md) |
| Frontend・認証方式の比較 | [UI / Authentication Profile Selection Guide](frontend-authentication-profile-guide.md)、[Phase 2 Developer Journey](phase2-developer-journey.md) |
| 利用可能なStarterとReference | [Starter一覧](../../koiki-starters/README.md)、[Reference index](../reference/README.md) |
| 開発環境の構築 | [開発環境構築手順](application-team-development-environment-guide.md) |
| 外部project試行の証拠 | [外部Reference試行](../architecture/validation/adoption-external-reference-boundary-validation.md)、[greenfield試行](../architecture/validation/adoption-greenfield-bootstrap-smoke.md) |

## 8. 実施結果（2026年9月28日）

Frameworkの概要説明と方針共有で終わり、P4-AR6の受入判定に使える実チーム環境でのbuild / run / diagnosisは
行っていない。以下は会議要約から非機密の範囲で転記した。担当は役割名で記載する。
[P4-AR6 worksheet](p4-ar6-actual-team-reception-worksheet.md)の実チーム受入セッションは`PENDING`のまま維持する。

### 8.1 合意した方向性

| 項目 | 合意内容 | Phase 4計画での扱い |
|---|---|---|
| 開発構成 | frontend（React / Next.js等）とbackendを分離し、BFFを介して連携する | 既存の確定入力（Next.js/BFF + KOIKI REST）と一致 |
| 認証方式 | JWTを利用する。BFFでtokenを管理し、backend側で検証する | BFF→KOIKIのBearer JWTをKOIKIがResource Serverとして検証する方向。issuer、audience、claim、IdP方式は未確定 |
| 開発環境 | VS Codeのworkspace機能で、Framework Repositoryと業務アプリRepositoryを並列配置する | P4-AR6のR2構成と一致。[開発環境構築手順](application-team-development-environment-guide.md)で提供 |
| OS環境 | WSL等のUnix系環境を推奨する | 開発環境構築手順のLinux付記は未検証。Linux上でのR2 build Evidenceが必要 |
| 基盤 | Java 21（実行はJava 25も想定）、Spring Boot、Maven。構造検査にArchUnitを使う | 既存baseline（Spring Boot 4.1.1）と一致。会議要約の版表記はbaselineで読み替える |
| 非提供 | モダンfrontend向けのproduction ReferenceとProject Templateは現時点で提供しない | 既存の境界（Phase 4 R4、Phase 5）と一致 |

### 8.2 未確定事項とリスク

| 論点 | 内容 | 扱い |
|---|---|---|
| IdPの方式 | token発行元がOpenID ConnectかSAMLか等で実装負荷が変わる | 顧客確認待ち。Phase 4のP4-04と同じ論点 |
| 認証の終端（説明会では未対話） | AWS ALBの認証機能やCognitoを介したSAML連携は現実的にあり得るが、説明会では扱っていない。どこが認証を終端するかで、BFF→KOIKIの検証方式が大きく変わる | 顧客へ確認する（[PL1-Q6](KOIKI-JavaWeb-FW_Phase4_PL1_REST利用境界差分台帳_v0.1.md#3-p4-ar6へ渡す確認事項)）。観点は[見直し草案§8](KOIKI-JavaWeb-FW_Phase4実施計画_見直し草案_v0.1.md#8-新しい入口へ引き継ぐ設計観点-認証の終端とbffkoiki間の検証) |
| 依存関係の制御 | 開発者が個別に`pom.xml`へ依存関係を追加すると、Frameworkの制約を回避できる可能性がある | 継続検討。現行計画に対応する項目がないため、Phase 4棚卸しへの追加候補 |

### 8.3 アクション

| 担当（役割） | アクション | 状態（2026-09-29時点） |
|---|---|---|
| Framework Owner | 最新のFrameworkソースを社内リポジトリに公開する | 対応済み（2026-09-28、`main` / `f7ad141`）。PR #39以降の反映は別途同期する |
| Framework Owner | ArchUnitで定義している構造ルールを日本語で文書化する | 未着手 |
| Framework Owner | VS Code workspaceの具体的な設定手順を提示する | 対応済み。[開発環境構築手順](application-team-development-environment-guide.md)（PR #39） |
| 業務アプリチーム | 公開ソースを基に開発環境のセットアップを進める | 業務アプリチーム側で進行 |
| 業務アプリチーム / Framework Owner | 認証まわりの詳細設計を相談する | 未着手。IdP方式の顧客確認が入力 |

### 8.4 日程

実案件の開始は顧客都合により1か月後ろ倒しとなり、2026年11月開始の予定である。
