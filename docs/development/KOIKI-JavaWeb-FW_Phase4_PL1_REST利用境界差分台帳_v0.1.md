# KOIKI-JavaWeb-FW Phase 4 PL1 REST利用境界差分台帳 v0.1

**状態:** PL1 SOURCE REVIEW / 実案件入力待ち  
**作成日:** 2026年9月26日  
**適用範囲:** 実案件から示されたNext.js/BFFとKOIKI REST連携。外部IdP SSOは見込みに留める。

**2026年9月27日の整理:** Repositoryの既存契約とReference実装を照合済み。PL1-Q1〜Q5の実案件入力は未取得。
本書の§4は翌9月28日のP4-AR6実チーム打ち合わせへ持参するPL1確認票であり、
[承認済みP4-AR6 worksheet](p4-ar6-actual-team-reception-worksheet.md)の受入結果を先取りしない。

## 1. 確認した利用境界

| 領域 | Frameworkの正式契約 | Reference / Toolingの実装例と限界 | 実案件側の設計・PL1 gap |
|---|---|---|---|
| REST runtime | [API Starter](../../koiki-starters/koiki-starter-api/README.md)はServlet MVC、Validation、Jackson 3、path versioning既定、Problem Detailsを提供。KOIKI独自Public Java APIは増やさない | [ExpenseApiController](../../koiki-reference-app/src/main/java/org/koikifw/reference/expense/adapter/inbound/api/ExpenseApiController.java)はReference所有の`GET /api/v1/expense-requests/{id}`、`POST /api/v1/expense-requests`、`POST /api/v1/expense-requests/{id}/submit`だけを実装 | BFFが必要とする業務API、DTO、version、一覧・更新・承認等の範囲は未取得。Reference endpointをCustomer契約とみなさない |
| Error | API StarterはSpring標準`ProblemDetail` / `ErrorResponse`を用い、安定`code`、Validation `violations`、5xx詳細非露出を提供 | Referenceの業務例外handlerとSecurity problem writerはReference専用 | BFFが利用者向け表示・再試行判断に使う業務`code`、401 / 403、競合応答を案件側で定義。共通欠陥だけFramework gapへ送る |
| 認証・route | [Security Starter](../../koiki-starters/koiki-starter-security/README.md)はdefault deny fallback、Method Security、OAuth2 Client / Resource Server依存を提供。route別chain、issuer / audience、scope、CORSはApplicationが構成 | [Reference API chain](../../koiki-reference-app/src/main/java/org/koikifw/reference/identity/configuration/ReferenceApiSecurityConfiguration.java)は`/api/**`のBearer JWT、stateless、Reference固有claim変換と権限を実証。Reference propertyで明示有効化する | REST連携だけではBearer / Session、token発行者、claim、BFF Session、CORS、logoutを確定できない。BFFとKOIKI間の信頼境界はCustomer設計・P4-AR6確認事項 |
| Identity / 認可 | `koiki-starter-identity`の公開契約とSpring Security標準認可を利用し、業務属性・Permission設計はApplicationが所有 | Referenceの`koiki_user_id` claim変換とexpense permissionは正式profileではない | 案件利用者IDとKOIKI Identityの対応、失効、業務権限、監査主体の対応を設計。SSO見込みを実装前提にしない |
| Audit / 相関 | `koiki-starter-audit`のBusiness / Security Audit契約、[Observability Starter](../../koiki-starters/koiki-starter-observability/README.md)のServlet request相関と構造化logを利用 | Referenceのexpense journeyはpackage済みJARで検証済み。BFFとの連結は未検証 | BFFとKOIKIのcorrelation ID受渡し、監査責任、PII・token非露出、障害時の調査Ownerを決める |
| artifact受渡し | P4-AR3のCustomer-like Consumerは隔離stageから正式release unit候補を利用し、PostgreSQL integrationまで検証済み | [P4-AR3 Evidence](../architecture/validation/pre-phase4-p4-ar3-runtime-security-consumer-baseline.md)はNext.js/BFFや実案件APIのE2Eではない | 実チームのbuild / run / API / operation受入とEvidence責任分担はP4-AR6。正式配布repository・version / supportはP4-E1で判断 |

## 2. gapの振り分け規則

| 分類 | 例 | 判断Owner / 入口 |
|---|---|---|
| 既存契約の利用 | Application-owned REST DTO、Security chain、業務Permission | Customerが設計・実装。Frameworkは契約を説明 |
| Framework defect | 既存Starterの公開契約が再現可能な条件で破綻 | Evidence、影響範囲、回帰を添えてFrameworkへ起票。修正は別commit point |
| Framework共通候補 | 複数案件へ提供する必要があり、Spring標準では代替しにくい機能 | [Grand Design §9.2](../architecture/grand-design/KOIKI-JavaWeb-FW_グランドデザイン_v0.2.md#92-framework-への昇格チェックリスト)の昇格審査とOwner判断。Customer codeをコピーしない |
| 案件固有 | BFF Session、画面、案件API、IdP設定、provider連携 | Customer所有。非機密の契約差分だけP4-AR6へ入力 |
| 未取得 | 認証方式、API範囲、運用環境、受渡し担当 | `OPEN`で保持し、想定でPASSにしない |

## 3. P4-AR6へ渡す確認事項

| ID | 実チームから必要な入力 | この台帳での状態 |
|---|---|---|
| PL1-Q1 | BFFから呼ぶKOIKI APIの最小journey、path / method、DTO / errorの期待 | OPEN |
| PL1-Q2 | BFFとKOIKI間の認証方式、主体ID / Permissionの対応、認可失敗の扱い | OPEN |
| PL1-Q3 | BFF Session、Cookie / CSRF、KOIKI側SessionまたはBearerとの境界、logout | OPEN |
| PL1-Q4 | correlation ID、Security / Business Audit、機密情報非露出、障害時の問い合わせOwner | OPEN |
| PL1-Q5 | package済みartifactの受入、build / run / diagnosisとjoint Evidenceの再現条件 | OPEN |

この台帳はCustomer API仕様や認証profileを確定しない。PL1-Q1〜Q5が未取得の間も、
Frameworkの既存契約とReference例の差分整理をPL2の技術設計へ渡せる。

## 4. 9月28日 P4-AR6打ち合わせ用の確認票

最初に「利用者が最初に完了させたい業務操作」を一つ選び、Browser → BFF → KOIKI REST → DB / Auditの
どこで何を確定するかを聞く。回答できない項目は`OPEN`のまま、回答を持つ役割と次の確認時点を記録する。
機密のpath、token、claim値、利用者情報、業務dataは本Repositoryへ記録しない。

| 順 | ID | 実チームへの確認 | 得たい非機密の記録 | P4-AR6 worksheetでの記録先 |
|---|---|---|---|---|
| 1 | PL1-Q1 | 最初の業務操作は何か。BFFはKOIKIにどの種類の読取・更新を求めるか。正常時と入力不備・権限不足・競合・処理先障害では利用者に何を伝え、再試行や重複送信をどう扱うか | 代表journey、操作の種類、API / DTO / errorの設計Owner、未決事項。具体的な案件API契約はCustomer管理下 | §5.1のREST選択、§5.3の最初のmodule、§7のfinding |
| 2 | PL1-Q2 | Browser、BFF、KOIKIの各境界で誰を認証するか。BFF→KOIKIはBearerか別方式か。KOIKIで利用者主体・権限・業務scopeをどう復元するか | 採用候補の認証profile、本人とserviceの区別、Identity / Permissionの設計Owner。IdPやclaim値は書かない | §5.1のprofile、§6の責任分担、§7のfinding |
| 3 | PL1-Q3 | Browser Cookie / BFF Session、CSRF、token保管・更新・失効、logoutのOwnerは誰か。BrowserがKOIKIを直接呼ぶ経路はあるか | Browser→BFFとBFF→KOIKIのcredential境界、直通の有無、残る設計判断。BFFのserver-to-server呼出しだけならBrowser向けCORS要否も区別する | §5.1のprofile、§6の責任分担 |
| 4 | PL1-Q4 | BFFの要求とKOIKIのlog / Business・Security Auditを何で突合するか。障害時に最初に調べる担当は誰か | 相関IDの受渡し方針、監査主体・保管Owner、一次切分け先、非露出条件。実log本文は書かない | §5.2のdiagnosis、§6のObservability、§7のfinding |
| 5 | PL1-Q5 | 実チームはどのartifactと手順でbuild / run / representative operation / diagnosis / cleanupを再現するか。Framework / Customer / joint Evidenceを誰が持つか | 選ぶjourney、再現可否と障害点、受渡し・Evidenceの担当役割、次Gate。実Customer Repository操作はworksheet §4.2の事前承認後 | §4〜§5.4、§6、§8〜§10 |

この確認票は業務操作と責任者の確認を優先する。全APIの一覧や詳細OpenAPI、IdP設定、運用手順の完成を
一度の打ち合わせの終了条件にしない。未取得は未取得として残し、設計を代行して埋めない。

## 5. Source reviewからの具体的な境界と次の判定

| 確認済みの事実 | PL1での扱い |
|---|---|
| [API Starter](../../koiki-starters/koiki-starter-api/README.md)は標準`ProblemDetail`へ共通入力・未処理例外を変換し、業務例外の`code`とdetailはApplicationが定義する | BFFに必要な業務error code、再試行可否、画面表示はPL1-Q1で確認する。Framework共通の欠陥が再現するまで新しい共通例外契約を提案しない |
| [Reference expense API](../../koiki-reference-app/src/main/java/org/koikifw/reference/expense/adapter/inbound/api/ExpenseApiController.java)は詳細取得、下書き作成、提出のみ。[Reference業務error変換](../../koiki-reference-app/src/main/java/org/koikifw/reference/expense/adapter/inbound/api/ExpenseApiExceptionHandler.java)は422 / 404 / 409 / 503を使う | 一覧、編集、承認、pagination、再送時の冪等性は実案件要件から判断する。Referenceのpath、DTO、error codeをCustomer仕様へコピーしない |
| [Security Starter](../../koiki-starters/koiki-starter-security/README.md)は未一致routeをdenyし、認証方式・chain・issuer等はApplicationが設定する。[Reference API chain](../../koiki-reference-app/src/main/java/org/koikifw/reference/identity/configuration/ReferenceApiSecurityConfiguration.java)は明示有効化したstateless Bearerの例 | Next.js BFF + Bearerは[認証profileガイド](frontend-authentication-profile-guide.md#6-profile-b--nextjs-bff)の候補であり、案件の採用確定ではない。CookieをKOIKIへ透過させる、またはBFFを認証の迂回路にする設計とは区別する |
| [ObservabilityのServlet filter](../../koiki-starters/koiki-starter-observability/src/main/java/org/koikifw/starter/observability/internal/KoikiCorrelationFilter.java)は`X-Request-ID`を検証して応答へ返し、`requestId`をlog contextへ置く | BFF側のID伝播、Auditとの突合、調査OwnerをPL1-Q4で確認する。BFFを含むend-to-end観測の成立はまだ未検証 |
| P4-AR3の隔離stageとCustomer-like ConsumerはFramework artifact利用の証拠。[P4-AR6 handoff contract](../architecture/validation/pre-phase4-p4-ar6-actual-project-handoff-contract.md)は実チーム受入の境界を定める | R2 stageを正式releaseやBFF結合済みと解釈しない。実チームの再現結果と正式受渡しの判断を分ける |

打ち合わせ後はPL1-Q1〜Q5を`確認済み / 部分確認 / OPEN`のいずれかで更新し、各項目へ回答した
役割、非機密の判断根拠、残件Owner、次の確認時点を添える。Frameworkの既存契約で足りる事項は
Customer設計へ渡し、再現可能な共通不具合はP4-AR6 worksheetのF1、文書・Tooling不足はF2、
案件固有要件はF4、Phase 4機能候補はF5へ振り分ける。P4-PL3へ渡すのは、その分類と責任分担の差分である。
