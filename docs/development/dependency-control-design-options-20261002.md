# 依存関係制御：構成宣言・検査の比較と検証方式案（2026-10-02）

**状態:** 検証方式はDRAFT。案の共有・認識合わせを進め、検証作業を保留する方針はOWNER AGREED / 2026-10-02（§8）。方式採用・検証実行・正式実装は未承認。
**位置付け:** [引継ぎ](dependency-control-next-session-handoff-20261002.md)§3の分析・設計。承認済み[論点整理](dependency-control-issues-20261001.md)を深掘りし、作業4bへ渡す。
**Phase / Ownership:** Pre-Phase 4採用支援 / Framework側の設計文書。Customer POM、業務依存、CIの運用権限はCustomer所有。
**確認baseline:** `docs/daily-development-workflow` / `357cd46`。Repository sourceとMaven公式仕様を確認。Maven実行・fixture実測・実チーム確認は未実施。

## 1. 推奨する検討方向

**A案（個別記載への検査）を有力候補として検証方式案を保持し、今はアプリチームへの案の共有・認識合わせまで進めて、検証作業を保留する。**
アプリチームはまだ初動で、アプリ要件を固める段階にない。今回の分析・方式案の整理を区切りとし、Framework側の追加検証設計、検証用Tooling・fixtureの実装と実行、協働検証には進まない。最初のCustomer POM構成・Starter選択を具体的に検討し始める前段で再開を判断する（§8）。

将来、アプリチームが要件と構成を検討できる段階になったら、Starterの機能充足確認、Customer POMの確定支援、確定後の変更管理を協働で確認する。型宣言や構成選択と検査の組合せは、A案の検証結果とその段階での必要性に応じて再評価する。

本書の出口は、仮のCustomer POM例、構成条件候補、検出範囲、例外運用と検証の承認対象である。型名・座標・記法は決定しない。実チームの誤記載事例が未取得のため、以下の例はすべて**分析用の仮例**とする。

### 1.1 アプリチームの不安の捉え方（2026-10-02の補足）

Ownerから、Customer POMを整備する責務はアプリチームにある一方、Framework Starterで提供される機能の理解と、アプリに必要な機能を満たせるかへの不安が背景にあるのではないか、という解釈が提示された。実チームの直接回答として確定せず、以後の検討の起点とする。

この解釈では、必要な安心を次の2つに分ける。

- **確定前の充足確認:** 要件に対して、どのStarterが何を提供し、どこからCustomer実装・設定が必要かを説明し、選択した構成で必要な機能が成立することを確認する。
- **確定後の維持:** 確認したPOMが日常開発で不用意に変更されず、変更が必要なときは担当者の判断と再確認を経るようにする。

POMを確定後に「通常は編集不可の管理ファイル」と扱う運用も安心を得る候補になる。変更制限は充足確認の後に置く。POMが固定されても、機能の不足や設定・実装の責任が解消されるとは扱わない。

### 1.2 POM確定支援と変更制限の運用案

| 段階 | 行うこと | 責任と残す情報 |
|---|---|---|
| 要件との照合 | 要件ごとに提供Starter、Customer実装・設定、未提供機能、確認方法を対応させる | Customerが要件とPOMを所有し、Framework側が提供範囲・前提・限界を説明する |
| 構成の確定 | Parent、Starter、BOM、version、scope、profileと必要なbuild / test / run結果をreviewする | Customer側の構成責任者とFramework条件の相談先を決める。確定commitと未確認事項を記録 |
| 日常開発 | 対象POMを通常変更しない管理ファイルとして扱い、変更の取込みを担当者の承認対象にする | CustomerのRepository運用で対象pathと承認者を定める。新規moduleのPOMも対象に含めるか明示 |
| 正当な変更 | 業務library追加、version更新、脆弱性対応等を理由・影響・再検証結果とともにreviewする | Customer通常変更とFramework条件変更の相談先を分ける。承認後に確定baselineを更新 |

「編集不可」をどの地点で効かせるかを区別する。

| 方法 | 効果 | 条件・限界 |
|---|---|---|
| ローカルの読み取り専用属性・IDE設定 | 日常の誤編集を減らす補助 | 各端末の設定であり、チーム全体の変更取込み制限は別に必要。採用IDE / OSでの確認は未実施 |
| Repository上のPOM変更承認 | 変更案を指定担当者の承認を経て共有branchへ取り込む | 編集自体を止めるのではなく、確定構成の更新を管理する。Customerのhosting機能と権限を確認 |
| 確定POMとの差分確認 | 変更を検出して承認・再確認へ戻す | 取込み制限と組み合わせる候補。内容の適合性を自動判定する検査より小さく始められる |

Gitの`assume-unchanged` / `skip-worktree`はindex・作業ツリーの扱いを変える機能であり、共有Repositoryの編集権限設定には用いない（[Git update-index](https://git-scm.com/docs/git-update-index)）。GitHubの場合はCODEOWNERSと保護branchの承認要求を組み合わせる方法があるが、CODEOWNERS単体を編集禁止として扱わない。社内Repositoryの製品・機能・権限は未確認であり、同じ方式を使えるとは決めない（[GitHub Code Owners](https://docs.github.com/en/repositories/managing-your-repositorys-settings-and-features/customizing-your-repository/about-code-owners)）。

POM外の親・設定・実行引数やSNAPSHOTの内容変更で実効構成が変わる可能性は残るため、確定POMと併せてFramework baselineと検証条件を記録する。初期の目的が不用意なPOM変更の防止なら、全面的な依存適合検査まで直ちに導入する必要はない。実効構成のずれが実際の問題になった場合に追加検査を検討する。

## 2. 現行構成と誤選択が起きる場所

### 2.1 Sourceから確認した構成

| 対象 | 現行の事実 | 設計への影響 |
|---|---|---|
| [Parent](../../koiki-parent/pom.xml) / [BOM](../../koiki-dependencies-bom/pom.xml) | ParentがKOIKI BOMをimport。BOMはKOIKI、Boot、Modulith、MyBatis等のversionを管理。ParentのEnforcerはJava / Maven条件だけを検査 | BOMだけを選ぶとParentのbuild検査を継承しない。管理座標を利用許可と扱わない |
| [API](../../koiki-starters/koiki-starter-api/pom.xml) / [Web MVC](../../koiki-starters/koiki-starter-web-mvc/pom.xml) | 両方がBoot Web MVCを導入。Web MVCにはThymeleafとHTMXも含む。相互依存はない | RESTとHTMLを提供するappで両Starterを併用することは直ちに誤選択ではない |
| [Security](../../koiki-starters/koiki-starter-security/pom.xml) | Boot Security、OAuth2 Client、Resource Serverを導入 | OIDC / Bearerを依存の有無だけで分けられない。片方のlibraryを一律禁止すると現行Starter自身に抵触する |
| [Identity](../../koiki-starters/koiki-starter-identity/pom.xml) | Security、Data JPA、Auditを導入 | Identity選択でDB / JPAも入る。単純な認証libraryの選択として扱わない |
| [Session JDBC](../../koiki-starters/koiki-starter-session-jdbc/pom.xml) | Identity、Data、Boot Session JDBCを導入 | Session共有の選択はIdentity / JPA / Audit / migrationにも影響する |
| [Data JPA](../../koiki-starters/koiki-starter-data-jpa/pom.xml) / [Data](../../koiki-starters/koiki-starter-data/pom.xml) | Data JPAはBoot Data JPAを導入し、KOIKI Dataへの依存はない。DataはFlywayとPostgreSQL moduleを導入 | JPAが解決されてもKOIKIのmigration契約が揃うとは限らない。別々の必須条件が必要 |
| [Audit](../../koiki-starters/koiki-starter-audit/pom.xml) / [Observability](../../koiki-starters/koiki-starter-observability/pom.xml) | AuditはData JPAを導入。ObservabilityはActuator等を導入し、spring-web / Servlet APIはoptional | AuditにはDB前提がある。Web関連classが見えるだけでWeb server起動とは判定しない |
| [Rules入口](../../koiki-archunit-rules/src/main/java/org/koikifw/archunit/KoikiArchitectureRules.java) | 業務構造とFramework Ownershipの検査を提供。Root packageの存在確認を含む | 呼出しと検査対象を確認する必要がある。PASSだけでは全Customer classの網羅を証明しない |

以上はPOM宣言の確認であり、解決済み依存graphやruntimeの実測ではない。[Reference POM](../../koiki-reference-app/pom.xml)はAPI / MVC / Session JDBCを併用する具体例として読む。Referenceのcache等を型の必須構成に転用しない。

### 2.2 仮のCustomer POM例

現行方式のAPI + JPA例（抜粋、完全なbootstrap POMではない）。認証・Audit・運用条件は別途選ぶ。

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
  <dependency>
    <groupId>org.koikifw</groupId>
    <artifactId>koiki-starter-data-jpa</artifactId>
  </dependency>
  <dependency>
    <groupId>org.koikifw</groupId>
    <artifactId>koiki-starter-data</artifactId>
  </dependency>
  <dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
  </dependency>
  <dependency>
    <groupId>org.koikifw</groupId>
    <artifactId>koiki-archunit-rules</artifactId>
    <scope>test</scope>
  </dependency>
</dependencies>
```

| 仮例 | POMの変更箇所 | 防止したいこと / 判定候補 |
|---|---|---|
| F1: 別ParentでKOIKI BOMだけをimport | `dependencyManagement`へKOIKI BOMを追加 | Parentのbuild契約欠落。別Parent利用の承認条件と同等検査がなければ不適合候補 |
| F2: Parent利用中にBoot / Modulith BOMを再import | 子の`dependencyManagement`へ別BOMを追加 | Framework管理baselineと異なる解決や不要な宣言。import自体と解決versionの両方を確認 |
| F3: Bearer-onlyなのにSession JDBCを追加 | `dependencies`へ`koiki-starter-session-jdbc`を追加 | 不要なSession / Identity / DB構成。Bearer-only条件に対する逸脱候補 |
| F4: JPAだけを追加してDataを省略 | `koiki-starter-data-jpa`だけを宣言 | KOIKI migration契約の欠落。DBを持つ型ならDataも必須候補 |
| F5: Security等をStarterからexclude | `exclusions`へ必須座標を追加 | 必須graphの欠落。別経路で復活してもexclude宣言はreview対象にできる |
| F6: 管理versionを明示して変更 | dependencyの`version`または子の管理entryを変更 | baseline逸脱。KOIKI SNAPSHOTはversion文字列だけで同一内容を証明しない |
| F7: 管理されているからMyBatisを追加 | `mybatis-spring-boot-starter`を追加 | 採用trigger前の導入。現行Rule 008はMYBATIS宣言を拒否するが、追加だけを拒否しない |

F1を自動で禁止するか、F2の同一version再importを警告にするか等の重大度は未承認。実例を得たら、この表の仮例と置換せず、案件のfindingとして出典・条件を追加する。

## 3. アプリ型と構成条件の候補

型の分類は画面技術だけで決めず、**実行単位・HTTP入口・認証・Session・永続化**の組合せから考える。Tier 1 / 2は業務module内の設計区分であり、app全体の依存型と一対一にしない。以下は比較用の条件候補であり、案件の型やsupport対象を確定しない。

| 構成条件 | 必須候補 | 任意・併用候補 | 禁止・追加判断候補 |
|---|---|---|---|
| RESTを提供するServlet app | API、Security。DBを持つ場合はData、JPA、driver | Observability、必要なIdentity / Audit | Bearer-onlyならSession JDBC不要・禁止候補。MVC画面を併設するならWeb MVC併用可 |
| HTML + SessionのServlet app | Web MVC、Security。Identity永続化ならIdentityとData / driver | REST併設ならAPI。共有Session要件がある場合にSession JDBC | JDBC Sessionを全HTML appに強制しない。local / OIDCの設定とSession責任を確認 |
| DBを扱うnon-web process | Data、JPA、driver。web application typeはnone | Observability、必要なAudit | API / Web MVCは不要候補。ただしSession cleanupはSession JDBCとIdentity等を必要とする例外 |
| 全構成に共通する条件 | 承認baseline、適切なArchitecture Rules test、業務で使うArchitecture Contract | `koiki-testing`はtest用途。Customer固有libraryは用途に応じて追加 | MyBatis採用、Level 2 runtime、WebFlux等は既存Gateに従う。BOM掲載を許可根拠にしない |

必須は「直接POM記載が必要」と「解決graphに存在すればよい」を分けて定義する。複数module構成では、deployable app、業務JAR、test moduleそれぞれに条件を割り当て、すべての子moduleへWeb / Securityを要求しない。非配布Tooling fixtureは正式Customer構成の許可例にしない。

Security Starterが両OAuth libraryを含むため、Bearer-onlyの検査はSession Starter・設定・HTTP testを組み合わせる。non-webは依存だけで決めず、実際にHTTP listenerが起動しないことも確認する。詳細な認証契約は[Developer Journey](phase2-developer-journey.md)と[Profile Guide](frontend-authentication-profile-guide.md)を維持する。

## 4. 3案をMavenの仕組みへ落とす

### 4.1 仕様から確認できる範囲

BOM importは管理entryの展開であり、依存追加やbuild plugin継承にはならない。子の管理entry、直接依存のversion、exclusionは解決結果に影響する。親子の管理entryと複数importの優先関係を区別する。これは選択支援の仕組みが改変不能な制約にはならない根拠である（[Maven Dependency Mechanism](https://maven.apache.org/guides/introduction/introduction-to-dependency-mechanism.html)）。

Maven profileは依存等を切り替えられるが、有効化条件と実行時指定に依存する。親profileは定義そのものではなく、有効化の結果が継承されるため、子propertyによる型選択はmodel構築時の評価を含めて検証で確認する（[Maven Build Profiles](https://maven.apache.org/guides/introduction/introduction-to-profiles.html)）。

### 4.2 Customer記載とFramework側の役割

| 案 | Customer POMの形 | Framework側に必要なもの | 効果 / 限界 |
|---|---|---|---|
| A: 個別記載への検査 | §2.2のParentと個別Starterを維持。構成条件を検査入力として指定 | 条件表、宣言 / graph検査、既存Rulesの実行確認 | 選択の手間は残る。新しい組合せartifactを作らず、誤選択を検出する検証に向く |
| B1: 型別Parent | `parent`のartifactIdで型を選択。型別ParentはKOIKI Parentを継承し依存を定義 | 型別Parentと保守・配布契約 | 継承によって依存と検査設定を提供できる。Customerの別Parentとの両立、業務JARへの不要依存、型の増殖が課題 |
| B2: 型別集約Starter | 共通Parentに加え、型を表すStarterを1つdependencyとして指定 | 複数Starterを束ねるartifact | runtime構成を簡単に選べるがbuild検査を導入する役割は持たない。test scopeの検査依存は別に必要 |
| B3: 共通Parent内のprofile | propertyまたはprofile IDで型選択を宣言 | profileごとの依存定義、未選択 / 多重選択の検査 | 新座標は減らせるがIDEとCLIの有効profileの一致が課題。宣言propertyだけで選択可能かは未実証 |
| B4: 型別BOM | 型に対応するBOMをimport | 型別管理entry | version選択はできるがruntime依存を追加しない。Type宣言だけで構成を導入する要求には単独で届かない |
| C: 型選択 + 検査 | B1〜B3のいずれかで選択し、宣言と実際の構成を検査 | 選択定義と検査が参照する共通の条件、CI実行の合意 | 簡略化と逸脱検出を両立する候補。検査を停止する権限への対策は別途必要 |

B2を用いる場合も、直接使うAPIを推移依存へ隠すことによる保守への影響を評価する。集約Starterを追加しても、Customerが個別Starterを併記・excludeできるので、検査なしで構成の一致を保証しない。

型宣言の比較例（**いずれも未存在の仮記法**。コピーして利用する手順ではない）：

```xml
<!-- B1: Parent座標で選ぶ（仮のartifactId） -->
<parent>
  <groupId>example.proposal</groupId>
  <artifactId>application-kind-parent</artifactId>
  <version>PROPOSAL</version>
  <relativePath/>
</parent>
```

```xml
<!-- B2: 現行KOIKI Parentを使い、集約dependencyで選ぶ（仮座標） -->
<dependency>
  <groupId>example.proposal</groupId>
  <artifactId>application-kind-starter</artifactId>
  <version>PROPOSAL</version>
</dependency>
```

```xml
<!-- A / B3 / C: 検査入力またはprofile選択の候補（未実装） -->
<properties>
  <proposal.application.kind>candidate-kind</proposal.application.kind>
</properties>
```

Aでは最後のpropertyは検査入力にすぎず、依存は増えない。B3ではこれを読んでprofileが有効化される設計・実測が必要。未知の型や多重選択はFAIL候補とする。Mavenの`<type>`要素はartifact種別であり、このアプリ型の宣言に流用しない。

どの方式でもdriver、Customer業務library、migration、test、認証設定の責任は残る。「型宣言だけ」はFramework構成の選択部分を簡略化する意味とし、Customer POM・設定すべてが不要になるとは扱わない。

## 5. 検査の入力と検出範囲

検査は次の3層を分ける。BOM importは解決graphに残らないため、dependency treeだけでは不用意なimportを検出できない。

| 入力 | 判定候補 | 限界 |
|---|---|---|
| Customerの宣言POM、Customer親子POM、profile | 不要BOM import、version指定、exclusion、型宣言の重複、plugin変更 | 単純なXML文字列検索では継承 / property / namespaceを誤る。外部Parent由来も確認する |
| effective POMと全対象moduleの解決graph | 型ごとの必須 / 禁止、scope / classifier / version、推移依存からの導入 | 現在有効なprofileの結果。別profileの組合せやruntime設定を証明しない |
| Rulesのreport、package対象、runtime / HTTP test | コード構造、Security・Session・migration契約、実行状態 | report件数だけでtest内容や網羅性を証明しない。設定・対象のreviewが必要 |

effective POMは有効profileを反映し、verbose出力は設定の由来確認を補助する。元のBOM import記法の確認には宣言POMも残す（[Maven Help effective-pom](https://maven.apache.org/plugins/maven-help-plugin/effective-pom-mojo.html)）。

| 変更・回避例 | Aの検査候補 | Bだけ | Cの検査候補と残る限界 |
|---|---|---|---|
| 個別Starter / library追加 | 条件表の禁止・併用違反を宣言 / graphで検出 | 追加できる | 型条件とgraphを比較。Customer libraryを一律拒否しない |
| 別BOM import | 宣言を検出し管理version差分も比較 | importできる | import理由と結果を両方判定。同じ結果でも記載上の警告候補 |
| version上書き | baselineとの解決version差を検出 | 上書きできる | versionに加え使用commit / artifact識別情報を記録。SNAPSHOT同名差替えはversion比較だけでは不足 |
| exclusion / scope変更 | 宣言と必須graph欠落を検出 | 変更できる | 型の必須scopeに照合。復活する別経路も確認 |
| Spring / Security設定変更 | POM検査だけでは不足 | 判定できない | 契約testとreviewで補完。runtime設定全体の意味判定は保証しない |
| Enforcer / testのskip・削除 | POM内検査は停止し得る | 停止できる | Customer POMから独立したCI検査と実行Evidenceを候補とする。CI変更権限を持つ人の回避は運用合意が必要 |
| 別profile / moduleだけをbuild | 対象漏れを検査入力と照合 | 構成が変わる | 実際の配布対象profile / moduleを明示し全対象を確認。未申告の組合せは未検証 |

Enforcerの`bannedDependencies`は推移依存も対象にできるため、明確な禁止座標の検出に適する。ただし必須構成・BOM import・設定の意味まではこのruleだけで扱えない（[Banned Dependencies](https://maven.apache.org/enforcer/enforcer-rules/bannedDependencies.html)）。
また、Enforcerにはskipやrule選択のpropertyがあるため、Parentに置くだけでは実行を担保しない（[enforcer:enforce](https://maven.apache.org/enforcer/maven-enforcer-plugin/enforce-mojo.html)）。

CI案は、固定した検査Tooling / 条件versionをCustomerのPOMから独立して実行し、対象commit・型・profile・module・例外・結果を記録する。失敗またはreport欠落は受入不可候補とする。CI自体を変更できる人への強制には、Customer側のreviewと保護設定が必要になる。現行CIやrequired checksへ追加する判断は本書に含めない。

## 6. 拡張、例外と保守責任

| 対象 | 判断・保守Ownerの案 | 記録・見直し条件 |
|---|---|---|
| Customer固有library、業務moduleの依存 | Customer | 用途、推移依存、契約影響をreview。Framework条件に抵触しなければ通常のCustomer判断 |
| 確定POMの変更管理 | Customerの構成責任者 / Repository管理者 | 管理対象path、確定commit、変更理由、承認者、再確認結果。Framework側がCustomerファイルの所有者になるものではない |
| 型の条件、Framework管理version、Starter構成 | Framework Architecture Owner | 条件version、変更理由、互換性と検証Evidence。型の更新が全appへ及ぶ影響を確認 |
| Framework条件からの例外 | Customerが申請し、Framework条件はFramework Ownerが判断。CI反映はCustomer | 対象app / module / 座標 / scope / 型 / baseline、理由、期限、代替検証、判断者。全体skipを例外としない |
| 検査Tooling / 実行Evidence | Tooling保守はFramework側候補、CI運用はCustomer | Tooling version、対象commit、実行引数、report。責任分担は受入前に合意 |
| MyBatis / Level 2 / Security契約 / 正式配布変更 | 既存GateのOwner | 一般的な依存例外で既存の採用Gateを解除しない |

例外が増える場合は型の粒度を見直す。型の変更時は依存差分、認証・Session・DB・配布への影響を再確認し、以前のPASSを引き継がない。共有の条件定義から構成選択と検査期待値を対応させ、二重管理によるずれを防ぐ。ただし検証testの期待値は独立したケースで検証する。

## 7. 推奨案の評価と段階的な検証方式案

Framework側で整理したA案の検証方式を再開時の材料として保持する。検証作業は§8の方針により保留する。§1.2の運用案は、アプリ要件とPOMの構成を検討できる段階で協働確認する候補であり、その運用で安心が得られる場合は型宣言等の追加を必須にしない。

| 観点 | A: 個別記載 + 検査 | B: 型選択のみ | C: 型選択 + 検査 |
|---|---|---|---|
| 選択の簡単さ | 現行と同程度 | Framework選択は簡単になる候補 | Bと同程度 |
| 誤選択の検出 | 条件と実行範囲を定めれば可能 | 宣言後の変更を止められない | 条件との一致を確認できる候補 |
| 正式成果物への影響 | Toolingに留める段階を設けられる | Parent / Starter等の新定義が必要 | 選択方式の正式採用段階で影響あり |
| 保守負担 | 条件・検査と案件例外 | 型ごとの構成・配布・更新 | Bに加えて検査。共通条件化が必要 |
| 現時点の判断 | 有力候補。検証方式案を保持し作業保留 | 後続の選択簡略化候補。現時点の検証対象にしない | 後続候補。Aの検証結果と案件側の必要性で再評価する |

### 7.1 再開時に判断する検証範囲（未実施・保留）

**段階V1（再開時の検証候補・作業保留）:** 非配布ToolingでA案の実効性と限界を検証する。既存Parent / BOM / Starter / Rulesを変更せず、承認baselineに対する宣言POM・effective POM・graphの比較を行う。Framework側で用意する正常・異常ケースで、期待結果との一致と誤検出・検出漏れを確認する。実案件のPOMやアプリ要件は前提にしない。Dockerとruntime検証はV1に含めない。

**段階V2（後続候補）:** V1のEvidenceと、アプリチームが構成検討に進んだ時点の入力から、型宣言の検証が必要かを判断する。必要な場合にB1〜B3の候補を絞り、Tooling専用の仮座標で構成宣言と検査の組合せを検証する。正式Parentへのprofile追加や新Starter配布は行わない。V1承認をV2の実行承認に読み替えない。

| 検証対象 | 正常 / 異常ケースと期待する観測 |
|---|---|
| 基本構成 | 承認した構成候補のPASSと、F1〜F7の各逸脱のFAIL / 警告。候補条件による判定であり正式適合を意味しない |
| 推移依存 | 禁止座標をCustomer library経由で導入、必須をexclude、別経路で復活。宣言と解決結果を区別してreport |
| version / BOM | 子の管理entry、直接version指定、複数BOMの順序、同一version再import。解決差と宣言差を観測 |
| scope / module | test限定Modulithとruntime導入、Rulesのruntime混入、業務JAR / deployableの分離、未検査module |
| 実行停止 | Enforcer skip、test skip、対象package縮小、report欠落。依存検査とRules実行確認を別結果にする |
| 型宣言（V2） | 未選択、未知の型、複数選択、型と個別依存の不一致。B3なら親子property評価とIDE / CLIのprofile一致 |
| 正当な拡張・例外 | Customer library追加は条件非抵触ならPASS。承認した範囲だけ例外適用し期限切れ / 別appへの流用はFAIL |

検証の承認提案では、Tooling配置先、仮座標、対象条件、実行command、Maven repository書込み先、通信・cleanup、担当Ownerを具体化する。通常の`~/.m2`、日常用stageや既存R2の契約を変更しない。実測結果は`docs/architecture/validation/`へ記録する。
V1の出口は、検証条件と実行手順、正常・異常ケースの期待結果と実測結果、検出漏れ・誤検出、検査実行の前提と運用負担を説明できるEvidenceとする。仮例で確認した技術的な実効性と、案件の機能充足・運用受入を分けて判定する。現段階では検証方式案として提示し、fixture作成・Maven実行は行っていない。

### 7.2 次の判断と作業4bへの入力

1. Architecture Ownerが整理の経緯、A案の位置付け、検証作業の保留と再開条件をアプリチームへ報告し、認識合わせを進める。報告・認識合わせの実施結果は未確認。
2. 最初のCustomer POM構成・Starter選択を具体的に検討し始める前段で本件を再オープンする。問題の発生やPOM確定後まで待たない。
3. その時点の候補POM・構成条件・不安を確認し、POM確定支援と変更管理で満たせる範囲、追加検査が必要な範囲を判断する。必要なV1の具体的範囲・実行条件を提示して承認を得てから、Framework側の検証と協働確認へ進む。
4. 型宣言による選択簡略化も必要ならV2を検討し、正式成果物・配布・supportへの影響を別reviewする。A→Cを必須の進路にしない。

作業4bへは「採用支援Toolingの候補」と「型別Parent / Starterまたは正式Rules等へ進む場合のFramework gap候補」を分けて渡す。Phase 4 packageの追加は作業4b以降の判断とする。Customer projectの生成・template化が必要ならPhase 5境界を別途reviewする。

本書は検証方式のreview材料と§8の方針合意記録であり、検証方式全般の採用、新しいGate通過、検証実行、Phase 4開始またはremote操作の許可ではない。

## 8. 整理の経緯、作業保留と再開方針（2026-10-02）

**Decision:** OWNER AGREED。Architecture Ownerが、案の共有・認識合わせを進めて検証作業を保留し、今回の検討を区切る方針を了解した。検証方式全般の承認・採用や、アプリチームの合意取得を意味しない。

| 整理の経緯 | 得られた判断 |
|---|---|
| 引継ぎを受け、個別記載への検査、型宣言、両者の組合せを現行POMとMaven仕様から比較 | 機能の選択支援と、確定構成からの変更の検査を分けて整理した |
| Ownerが、POM整備の責務とStarterの機能理解・充足への不安を補足 | 機能充足の確認、POM確定支援、確定後の変更管理も対応候補へ加えた |
| アプリチームは初動で、要件を固める段階にないことを確認 | A案を有力候補とし、技術的な実効性・限界を確認する検証方式案を整理した |
| 今すぐ検証へ進むかを検討 | 仮の案件条件への作り込みを避け、今は案の共有・認識合わせまで進めて作業保留とする方針をOwnerが了解した |

今回の到達点は、本書の分析・比較・検証方式案と保留方針の文書化である。Framework側の追加検証設計、fixture / Tooling実装、Maven実行、協働検証は保留する。検証PASS、案件の機能充足、POM確定、運用受入は未達であり、完了扱いにしない。

**再開条件:** アプリチームが最初のCustomer POM構成・Starter選択を具体的に検討し始める前段に達したことを、Ownerとアプリチームが確認する。候補構成が見え始めた時点で本件を開き、必要な検証範囲と責任分担を再判断する。現在の案をそのまま実行する前提にはしない。

### アプリチームへの報告要点

以下はArchitecture Ownerが報告に利用するための要点であり、送信・報告済みの記録ではない。

- Customer側のPOM整備責務を前提に、Starterの提供範囲・機能充足の確認をFramework側が支援する方向で整理した。
- 個別記載への検査（A案）を有力候補とし、POM確定後の変更承認・管理ファイル扱いも運用候補とした。方式採用は未決定。
- チームはまだ初動のため、検証方式案を保持して作業を保留する。今回の分析・方式案の整理を区切りとし、認識合わせまで進める。
- 最初のPOM構成・Starter選択を具体化する前段で再開する。問題が発生してからの対応にはせず、その時点の構成と課題に合わせて検証・協働確認を判断する。
