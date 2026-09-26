# KOIKI-JavaWeb-FW Phase 4 PL1 REST利用境界差分台帳 v0.1

**状態:** PL1 SOURCE REVIEW / 実案件入力待ち  
**作成日:** 2026年9月26日  
**適用範囲:** 実案件から示されたNext.js/BFFとKOIKI REST連携。外部IdP SSOは見込みに留める。

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
