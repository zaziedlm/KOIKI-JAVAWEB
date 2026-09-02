# KOIKI-JavaWeb-FW 利用予定プロジェクト向け Spring/Java 事前学習計画 v1.1

## 1. 目的と前提

- **目的** — KOIKI-JavaWeb-FWは実装途上であり、プロジェクトメンバーへFrameworkモジュールをまだ提供できない。本書は、その提供前に、KOIKI-FWが採用するSpring標準機能をSpring公式資料から学習してもらうための計画である。
- **前提** — KOIKI-FWは「Spring標準優先」（グランドデザイン§5 設計原則1）を最上位方針とし、認証・認可・Web・データアクセス・運用のいずれも独自プロトコルや独自抽象で覆い隠さない。したがって**Spring本体の学習が、そのままKOIKI-FW理解の前提知識になる。**
- **対象読者** — 本フレームワークを用いて業務システムを開発する予定のプロジェクトメンバー（Java実務経験はあるがSpring Boot／Spring Securityの実務経験が浅い層を想定）。
- **正本** — 本計画はKOIKI-FW側の設計文書に基づく（[docs/architecture/grand-design/KOIKI-JavaWeb-FW_グランドデザイン_v0.2.md](../architecture/grand-design/KOIKI-JavaWeb-FW_グランドデザイン_v0.2.md)、[docs/architecture/adr/README.md](../architecture/adr/README.md)）。設計自体が今後変更された場合、本書ではなくグランドデザインとADRを正とする。
- **本書のスコープ外** — KOIKI-FW自体のコード・Public API（未実装）、Oracle対応、SPA UIの具体的実装、Reference Applicationの操作手順。

## 2. 重要な前提の共有（誤解しやすい点）

学習を始める前に、次をプロジェクトメンバー全員へ明確に伝えることを推奨する。

1. **KOIKI-JavaWebはOAuth 2.0の「Client」と「Resource Server」までを提供し、Access Token／Refresh Tokenを発行する「Authorization Server」機能は提供しない**（グランドデザイン§14.2、ADR-008）。JWTアクセストークン・リフレッシュトークンの発行・更新・失効管理は、**業務アプリケーション側（またはKOIKI-JavaWeb以外に用意する認可基盤）の責務**になる。
2. React等のSPAを想定する場合、KOIKI-JavaWebは送られてきたBearer JWTを検証する側（Resource Server）であり、ログイン画面からのトークン発行は業務アプリ側の別コンポーネントが担う。
3. ID TokenはAPI認証に使わない。API認証にはAccess Tokenだけを使う（ADR-008）。
4. トークンをブラウザの`localStorage`／`sessionStorage`へ保存する設計は採用しない（グランドデザイン§14.2）。
5. これらは2026年8月時点でACCEPTED済みの設計判断であり、Phase 2の実装で変わる可能性は低いが、最終的な確定事項はKOIKI側のADRとGate承認記録を確認すること。

## 3. 学習トラックの全体構成

学習は8トラックに分け、依存関係の少ないものから順に並べる。すべてSpring公式ドキュメント・公式ガイドを一次情報源とし、各トラックには手を動かして学べる公式サンプルソースへのリンクも添える。

| Track | テーマ | 優先度 |
|---|---|---|
| 1 | Spring Framework Core / Spring Boot 基礎 | 必須・最優先 |
| 2 | Spring MVC とREST API | 必須・最優先 |
| 3 | Spring Security 基礎（認証・認可・CSRF・Session） | 必須・最優先 |
| 4 | Spring Security OAuth2（Resource Server / Client, OIDC） | 必須（React連携の核心） |
| 5 | React SPA向け認証APIの設計知識（業務アプリ側が用意する部分） | 必須（React連携の核心） |
| 6 | データアクセス（Spring Data JPA / JdbcClient / MyBatis / Transaction） | 必須 |
| 7 | Spring Modulith（モジュール構造） | 推奨 |
| 8 | 運用・可観測性・テスト（Actuator, Micrometer, Flyway, Testcontainers） | 推奨 |

Track 1〜3は他のすべての前提になるため、最初に学習する。Track 4・5はセットで学ぶ（Resource Serverの理解なしにToken発行APIの設計はできない）。Track 6〜8は並行学習可能。

## 4. Track別の学習内容

### Track 1 — Spring Framework Core / Spring Boot 基礎

**学ぶこと**
- IoC / DIコンテナ、Bean定義とライフサイクル、`@Configuration` / `@Component`系アノテーション
- Spring Bootの自動構成（Auto Configuration）の仕組みと、`@ConditionalOn*`による条件分岐
- `application.yml` / `application.properties`、Profile、外部設定の優先順位
- Spring Boot Starterの仕組み（依存関係の一括管理）
- Bean Validation（Jakarta Validation）の基本

**公式資料**
- [Spring Boot Reference Documentation](https://docs.spring.io/spring-boot/reference/)
- [Spring Framework Reference — Core Technologies (IoC Container)](https://docs.spring.io/spring-framework/reference/core/beans.html)
- [Spring Guides — Building an Application with Spring Boot](https://spring.io/guides/gs/spring-boot/)
- [spring.io/guides 一覧ページ](https://spring.io/guides)（Getting Startedガイド全体の入口）

**サンプルソース**
- [spring-guides/gs-spring-boot](https://github.com/spring-guides/gs-spring-boot) — 上記Getting Startedガイドの完成コード（`initial` / `complete` の2段階構成）
- [spring-projects/spring-boot](https://github.com/spring-projects/spring-boot) — 本体リポジトリ。`spring-boot-tests/spring-boot-smoke-tests`配下にAuto Configurationの実例が多数ある

**KOIKI-FWとの関係**
- `koiki-parent`（Maven統一）、`koiki-dependencies-bom`（依存バージョン統制）、`koiki-starter-*`はすべてSpring Bootの標準Starter機構の上に構築される。
- 設計原則「No Hidden Magic」（グランドデザイン§5）— KOIKIのAuto Configurationは予測可能で無効化・上書き可能という方針のため、まずSpring Boot標準のAuto Configuration動作を理解しておくと、KOIKI固有の設定を素直に読み解ける。

### Track 2 — Spring MVC とREST API

**学ぶこと**
- `@RestController` / `@RequestMapping`系、DTOとJakarta Validationの組み合わせ
- 例外ハンドリング（`@ControllerAdvice` / `@ExceptionHandler`）
- RFC 9457 (Problem Details for HTTP APIs) とSpringの`ProblemDetail`サポート
- Content Negotiation、Jacksonによる JSON シリアライズの基本

**公式資料**
- [Spring Framework Reference — Web on Servlet Stack (Spring MVC)](https://docs.spring.io/spring-framework/reference/web/webmvc.html)
- [Spring Framework Reference — Error Handling（`ProblemDetail`）](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-exceptionhandler.html)
- [IETF RFC 9457 — Problem Details for HTTP APIs](https://datatracker.ietf.org/doc/html/rfc9457)

**サンプルソース**
- [spring-guides/gs-rest-service](https://github.com/spring-guides/gs-rest-service) — REST APIの基本形
- [spring-guides/gs-validating-form-input](https://github.com/spring-guides/gs-validating-form-input) — Jakarta Validationの実例
- [spring-guides/tut-rest](https://github.com/spring-guides/tut-rest) — REST APIチュートリアル群（存在しない場合は`spring-guides`組織ページ https://github.com/spring-guides から関連リポジトリを検索）

**KOIKI-FWとの関係**
- `koiki-starter-api`はAPI、例外、Validation、Web共通設定を提供する（グランドデザイン§7.1）。
- KOIKIは「RFC Problem Detailsを基礎とする統一エラー形式」（§12.4）を採用しており、未処理例外の変換規約を持つ。Spring標準の`ProblemDetail`機構を理解していると、KOIKI提供時の規約差分だけを学べば済む。

### Track 3 — Spring Security 基礎

**学ぶこと**
- `SecurityFilterChain`の考え方、Authentication（認証）とAuthorization（認可）の分離
- `UserDetailsService`、Password Encoding（`PasswordEncoder`）
- Method Security（`@PreAuthorize`等）とURLベース認可の違い
- CSRF保護（Cookieベースの画面と、SPAのdouble-submitパターンの違い）
- CORS設定
- セキュリティヘッダー（HSTS、X-Content-Type-Options、CSP等）
- Spring Session（特にJDBC実装）によるHTTPセッションの外部化

**公式資料**
- [Spring Security Reference](https://docs.spring.io/spring-security/reference/) — Servlet Applications全般、特にAuthentication / Authorization / CSRF / Headers / Session Managementの各章
- [Spring Session Reference](https://docs.spring.io/spring-session/reference/) — 特にHTTP Session（JDBC実装）の章

**サンプルソース**
- [spring-guides/gs-securing-web](https://github.com/spring-guides/gs-securing-web) — フォームログインの基本形
- [spring-projects/spring-security-samples](https://github.com/spring-projects/spring-security-samples) — 公式Security構成サンプル集（`servlet/spring-boot/java`配下にCSRF、CORS、Method Security等の実例）
- [spring-projects/spring-session](https://github.com/spring-projects/spring-session) — `spring-session-samples`配下にJDBCセッションの実例

**KOIKI-FWとの関係**
- `koiki-starter-security`がSecurity共通設定を提供する。
- グランドデザイン§14（セキュリティアーキテクチャ）全体、特に§14.3（Spring Session JDBCをセッションストアの標準とする理由）、§14.7（ブラウザ保護）、§14.6（アカウント保護と認証試行制御）。
- **注意** — 認証試行制御（ブルートフォース対策）はSpring Security標準機能ではなく、KOIKIが独自にDB記録で提供する予定の機能（§3.3、§14.6）。Spring標準の範囲とKOIKI独自拡張の境界を意識すること。

### Track 4 — Spring Security OAuth2（Resource Server / Client, OIDC）

React SPAとの連携で最も重要なトラック。KOIKI-JavaWebは、業務アプリのフロントエンドやAuthorization Serverから見て**Resource Server**（Bearer JWT検証）および必要に応じて**OIDC Client**（企業SSOログイン）として動作する。

**学ぶこと**
- OAuth 2.0の基本ロール（Resource Owner / Client / Authorization Server / Resource Server）
- Authorization Code Grant（＋PKCE）の流れ
- OpenID Connect（OIDC）とID Token / Access Tokenの役割の違い
- Spring Securityの`spring-boot-starter-oauth2-resource-server`によるJWT検証（`issuer-uri`、JWK Set、audience／scopeの検証、`JwtAuthenticationConverter`による権限マッピング）
- Spring Securityの`spring-boot-starter-oauth2-client`によるOIDCログイン（企業SSO想定）

**公式資料**
- [Spring Security Reference — OAuth2 Resource Server](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/index.html)
- [Spring Security Reference — OAuth2 Client](https://docs.spring.io/spring-security/reference/servlet/oauth2/client/index.html)
- [OpenID Connect Core 1.0](https://openid.net/specs/openid-connect-core-1_0.html)
- [IETF RFC 6749 — OAuth 2.0 Authorization Framework](https://datatracker.ietf.org/doc/html/rfc6749)
- [IETF RFC 7519 — JSON Web Token (JWT)](https://datatracker.ietf.org/doc/html/rfc7519)

**サンプルソース**
- [spring-projects/spring-security-samples](https://github.com/spring-projects/spring-security-samples) — `servlet/spring-boot/java/oauth2/resource-server`、`servlet/spring-boot/java/oauth2/login`配下に公式サンプル
- [spring-guides/tut-spring-boot-oauth2](https://github.com/spring-guides/tut-spring-boot-oauth2) — OAuth2の概念理解に有用な旧チュートリアル（アーカイブ済みの可能性あり。最新実装は上記samplesを優先）

**KOIKI-FWとの関係**
- グランドデザイン§14.2（認証方式表）、ADR-006／007／008（SPAプロファイルとToken lifecycleの責務分離）。
- **KOIKIはPhase 2でOAuth2 Client / Resource Serverまでをproduction scopeとし、token発行API・refresh・revocation endpointを提供しない**（§14.2）。外部Authorization Server（Cognito等のOIDC Provider、または後述Track 5の自前Authorization Server）が前提。

### Track 5 — React SPA向け認証APIの設計知識（業務アプリ側が用意する部分）

ユーザー確認済みの前提どおり、JWTアクセストークン・リフレッシュトークンの発行・管理APIは、KOIKI-JavaWebではなく業務アプリ側で用意する。このAPIを安全に設計・実装するための学習。

**学ぶこと**
- SPAの認証プロファイル3種の違いと選定基準（グランドデザイン§13.5）
  - same-origin Session SPA（第一標準。Cookie Session＋CSRF）
  - Next.js BFF等のBFF構成（Tokenをサーバー側で管理し、ブラウザにはBFF Session Cookieのみ）
  - direct Token SPA（PKCE付きAuthorization CodeでAccess Tokenを取得しBearer送信。明示的なrisk acceptanceが必要）
- Refresh Token Rotationと再利用検知（Reuse Detection）の設計
- IETF RFC 9700（OAuth 2.0 Security Best Current Practice）— Implicit flow不採用、許可外Redirect URIのfail-closed等
- 自前でAuthorization Serverを用意する場合の選択肢としての **Spring Authorization Server**（Spring公式プロジェクト）
  - Client登録、Access Token／Refresh Token発行、JWK公開エンドポイント
  - KOIKI-JavaWeb（Resource Server）が検証できるJWTを発行するための`issuer`・`audience`・署名鍵の設計
- トークンの保管場所に関するブラウザセキュリティの基本（`localStorage`を避ける理由、HttpOnly Cookieとの比較）

**公式資料**
- [Spring Authorization Server — プロジェクトサイト](https://spring.io/projects/spring-authorization-server)
- [Spring Authorization Server Reference](https://docs.spring.io/spring-authorization-server/reference/)
- [IETF RFC 9700 — OAuth 2.0 Security Best Current Practice](https://datatracker.ietf.org/doc/html/rfc9700)
- IETF RFC 6749 / RFC 7519（再掲。Track 4と共通）
- [OWASP Cheat Sheet Series — JSON Web Token for Java](https://cheatsheetseries.owasp.org/cheatsheets/JSON_Web_Token_for_Java_Cheat_Sheet.html)
- [OWASP Cheat Sheet Series — OAuth2](https://cheatsheetseries.owasp.org/cheatsheets/OAuth2_Cheat_Sheet.html)

**サンプルソース**
- [spring-projects/spring-authorization-server](https://github.com/spring-projects/spring-authorization-server) — `samples`ディレクトリに、Authorization Code＋PKCE、Refresh Token、JWTクライアント等の公式サンプル一式
- [spring-projects/spring-authorization-server の`docs`配下のガイド](https://docs.spring.io/spring-authorization-server/reference/getting-started.html) — 最小構成のAuthorization Serverを最初から組み立てる手順

**KOIKI-FWとの関係**
- グランドデザイン§13.5（SPAプロファイル）、§14.2、ADR-006／007／008。
- 業務アプリ側で構築するAuthorization Server（またはCognito等の外部IdP）と、KOIKI-JavaWeb側のResource Server設定（`issuer-uri`等）が整合するように、双方の設計担当が同じToken契約（audience、scope、claim構造）を共有する必要がある。この整合作業はKOIKI提供後にADR・Grand Designの該当章を突き合わせながら行う。
- **KOIKI側はまだこの領域を実装していない**（本書執筆時点でP2-A1着手直前、token発行APIはKOIKIのproduction scopeに含まれない）。したがって業務アプリ側のAuthorization Server設計は、KOIKI側の確定を待たず並行して検討を開始できるが、`issuer-uri`・audience・claim名などの結線仕様は最終的にKOIKI側のADR確定後にすり合わせる。

### Track 6 — データアクセス（Spring Data JPA / JdbcClient / MyBatis / Transaction）

**学ぶこと**
- Spring Data JPAの基本（Repository、Entity、遅延ロード、N+1問題）
- `@Transactional`とトランザクション伝播（特に`REQUIRES_NEW`。KOIKIの監査設計で使用）
- Spring FrameworkのJdbcClient（参照専用クエリ向け）
- MyBatis-Spring-Boot-Starterの基本（SQL指向の更新系を選ぶモジュール向け）

**公式資料**
- [Spring Data JPA Reference](https://docs.spring.io/spring-data/jpa/reference/)
- [Spring Framework Reference — Data Access (JDBC / JdbcClient)](https://docs.spring.io/spring-framework/reference/data-access/jdbc.html)
- [MyBatis-Spring 公式ドキュメント](https://mybatis.org/spring/)

**サンプルソース**
- [spring-guides/gs-accessing-data-jpa](https://github.com/spring-guides/gs-accessing-data-jpa) — Spring Data JPAの基本形
- [spring-projects/spring-data-examples](https://github.com/spring-projects/spring-data-examples) — `jpa`モジュールに射影・ページング等の公式サンプル
- [mybatis/spring-boot-starter](https://github.com/mybatis/spring-boot-starter) — `mybatis-spring-boot-samples`配下にMyBatis-Spring-Boot-Starterの公式サンプル

**KOIKI-FWとの関係**
- グランドデザイン§16（データアーキテクチャ）。更新系はJPAが既定、SQL指向はモジュール単位でMyBatis、参照系（read model）はJdbcClientまたはMyBatis Mapper。
- 同一トランザクション内でJPA書き込み後にJdbcClient／MyBatisで同じデータを読むと未フラッシュデータを読む問題（§16.2）はSpring／JPA仕様に基づく一般的な注意点であり、先に理解しておく価値が高い。

### Track 7 — Spring Modulith（モジュール構造）

**学ぶこと**
- `ApplicationModules`によるモジュール境界の定義と検証
- モジュール間連携における`ApplicationEventPublisher`と同期`@EventListener`
- Event Publication Registry（コミット後の耐久的イベント配信、Level 2以降）

**公式資料**
- [Spring Modulith — プロジェクトサイト](https://spring.io/projects/spring-modulith)
- [Spring Modulith Reference](https://docs.spring.io/spring-modulith/reference/)

**サンプルソース**
- [spring-projects/spring-modulith](https://github.com/spring-projects/spring-modulith) — `spring-modulith-examples`配下に複数の公式サンプル（`example-full`、`example-eda`等、モジュール境界・イベント配信の実例）

**KOIKI-FWとの関係**
- グランドデザイン§6.4（採用レベルLevel 0〜3）。KOIKIはPhase 1a〜2でLevel 0（test scopeのみ、実行時依存なし）を採用しており、Level 1以降はPhase 3以降の判断。
- `koiki-archunit-rules`と組み合わせてモジュール境界を機械的に検証する設計思想（§3.7）を理解しておくと、業務モジュールの分割方針を早期に把握できる。

### Track 8 — 運用・可観測性・テスト

**学ぶこと**
- Spring Boot Actuator（ヘルスチェック、メトリクスエンドポイント）
- Micrometer（メトリクス）とOpenTelemetry（トレース）の基本概念
- Flyway（マイグレーション管理）
- Testcontainers（実DBを用いた統合テスト）
- JUnit 5の基本

**公式資料**
- [Spring Boot Reference — Actuator](https://docs.spring.io/spring-boot/reference/actuator/index.html)
- [Micrometer Reference](https://docs.micrometer.io/micrometer/reference/)
- [OpenTelemetry — Java向けドキュメント](https://opentelemetry.io/docs/languages/java/)
- [Flyway 公式ドキュメント](https://flywaydb.org/documentation/)
- [Testcontainers 公式サイト](https://testcontainers.com/)
- [JUnit 5 User Guide](https://junit.org/junit5/docs/current/user-guide/)

**サンプルソース**
- [spring-guides/gs-actuator-service](https://github.com/spring-guides/gs-actuator-service) — Actuatorの基本形
- [testcontainers/testcontainers-java](https://github.com/testcontainers/testcontainers-java) — `examples`配下に各種DB・ミドルウェア連携の公式サンプル
- [flyway/flyway](https://github.com/flyway/flyway) — 公式リポジトリ（マイグレーションスクリプトの命名規約・サンプルはドキュメントサイト内のGetting Startedを参照）

**KOIKI-FWとの関係**
- `koiki-starter-observability`、グランドデザイン§20（Observabilityと運用）。
- KOIKIはH2等のインメモリDBを統合テストの中心に使わず、Testcontainersによる実DB検証を標準とする（§16.1）。

## 5. 学習の進め方（順序の指針であり、期日を定めるものではない）

学習量やメンバーの経験に応じて調整する前提で、依存関係に基づく推奨順序のみ示す。

1. **土台固め** — Track 1 → Track 2 → Track 3 の順で進める。Spring Securityの基礎（Track 3）を理解しないままTrack 4のOAuth2へ進むと、Filter Chainや認可の仕組みの理解が浅くなりやすい。
2. **認証・React連携の核心** — Track 3の理解を前提に、Track 4（Resource Server / Client）とTrack 5（業務アプリ側のToken発行API設計）をセットで学ぶ。Track 5はTrack 4の裏返しの関係（発行する側と検証する側）にあるため、両方揃って初めて全体像が見える。
3. **並行学習可能** — Track 6（データアクセス）とTrack 8（運用・テスト）は、Track 1〜3の後であればいつ進めても支障はない。
4. **アーキテクチャ理解** — Track 7（Spring Modulith）は、業務モジュール設計の話が具体化する段階で合わせて学ぶのでよい。

## 6. KOIKI-FW対応早見表

| Spring/関連機能 | 学ぶ理由 | KOIKI-FWでの主な対応箇所 |
|---|---|---|
| Spring Boot Auto Configuration | KOIKI Starterの動作原理 | `koiki-parent`、各`koiki-starter-*`、設計原則「No Hidden Magic」（§5） |
| Spring MVC / Jakarta Validation | REST API実装の基礎 | `koiki-starter-api`、§12.4 |
| Spring Security（基礎） | 認証・認可・CSRF・Session全般の基礎 | `koiki-starter-security`、§14全般 |
| Spring Session JDBC | セッションストアの標準実装 | §14.3、Framework所有のセッションテーブル（§16.7） |
| Spring Security OAuth2 Resource Server | Bearer JWT検証（React SPA・外部API連携） | §14.2、ADR-008 |
| Spring Security OAuth2 Client / OIDC | 企業SSOログイン | §14.2、ADR-006/007 |
| Spring Authorization Server | 業務アプリ側で用意するJWT発行基盤の選択肢 | KOIKI側は非対応（§14.2よりKOIKIはtoken発行APIを提供しない） |
| Spring Data JPA | 更新系永続化の既定技術 | §16.1、§16.2、`koiki-starter-data-jpa` |
| JdbcClient | 参照専用read model | §16.3、`koiki-starter-data-jdbc` |
| MyBatis-Spring-Boot-Starter | SQL指向更新系・既存SQL資産移行 | §16.2（Starterは設けずBOMのみ） |
| Spring Modulith | モジュール境界検証・イベント配信 | §6.4（Phase 1a〜2はLevel 0） |
| Actuator / Micrometer / OpenTelemetry | 運用可観測性 | `koiki-starter-observability`、§20 |
| Flyway | マイグレーション管理 | §16.7 |
| Testcontainers / JUnit 5 | 実DBによる統合テスト | §16.1、§6.1 |

## 7. KOIKI-FW提供後に確認すべきもの

事前学習が完了し、KOIKI-FWモジュールが提供された段階で、次を読むことでオンボーディングを完了させる。

1. [AGENTS.md](../../AGENTS.md)
2. `docs/agent/skills/koiki-project-overview/SKILL.md`
3. `docs/agent/skills/koiki-business-feature-work/SKILL.md`（業務機能を実装する場合）
4. [docs/architecture/grand-design/KOIKI-JavaWeb-FW_グランドデザイン_v0.2.md](../architecture/grand-design/KOIKI-JavaWeb-FW_グランドデザイン_v0.2.md)
5. [docs/architecture/adr/README.md](../architecture/adr/README.md)（特にSecurity・SPA関連のADR-006〜ADR-008、ADR-020、ADR-028）

## 8. 特記事項（再確認）

- 本書執筆時点（2026年9月1日）で、`feature/phase2-security-foundation`ブランチはP2-A1（Security Foundationの最小実装）着手直前であり、Spring Security標準構成の最小Auto Configurationをこれから実装する段階にある。認証・認可のProduction実装は未着手。
- React SPA向けのJWTアクセストークン／リフレッシュトークン発行・管理APIは、現行設計（Phase 2）でもKOIKI-JavaWebのproduction scopeに含まれない。業務アプリ側が自前のAuthorization Server（Track 5でSpring Authorization Serverを候補として学習）を用意するか、Cognito等の外部OIDC Providerを採用するかを選ぶ。
- 本書の内容は学習の道しるべであり、KOIKI-FW側の確定仕様（ADR、Grand Design、Gate承認記録）と矛盾する場合は後者を優先する。

## 9. サンプルソースの扱いに関する注意

- 各トラックの「サンプルソース」は、公式または準公式（`spring-guides`組織、各プロジェクト公式リポジトリ）のGitHubリポジトリを一次情報源として選んでいるが、公開リポジトリの構成やパスは将来変更・アーカイブされうる。リンク切れの場合は各公式サイト（`spring.io`、`docs.spring.io`）またはGitHub上の該当組織（`spring-projects`、`spring-guides`）の検索から探し直すこと。
- サンプルソースは学習・動作確認のためのものであり、KOIKI-FWや業務アプリケーションのコードへそのまま複製・流用しない。KOIKI自身もWalking Skeletonの使い捨てコードを正式成果物へ直接昇格させない方針（`AGENTS.md`）を取っており、同様に外部サンプルもまず理解のために動かし、実装は各アプリケーションの設計・規約に沿って書き起こす。
- 特にSpring Authorization Serverのサンプル（Track 5）は、鍵管理・Client Secret・CORS設定等が学習用の簡易構成になっていることが多い。業務アプリ側で採用する場合は、OWASP Cheat SheetおよびRFC 9700のBest Current Practiceに沿って本番向けに設定を見直すこと。
