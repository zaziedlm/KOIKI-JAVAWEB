# KOIKI利用案件へのコンテナ設計情報：方式1／方式2の検証からの引継ぎ（2026-11-10）

## 1. 目的と位置づけ

KOIKI Frameworkを依存として取り込む実案件がコンテナアプリケーションを開発する際、**アプリケーションとコンテナ運用の設計主体は案件側にある。KOIKI側はFrameworkをコンテナ上で正しく使うための利用条件と、検証済みの参考情報を提供できる。** 本書は、その情報を今後の利用案内へつなげるための整理である。

Ownerの追加文書作成指示に基づく、Phase 4 D3のTooling検証からの引継ぎ資料。新しいFramework契約・正式support範囲・標準コンテナ構成・配布Templateを定めるものではない。Framework／Reference／Customer／Toolingの所有境界を維持し、案件の設計をKOIKI側で代行しない。D3／Phase4全体の受入状態も変更しない。

今回の方式1・方式2は構成の実現可能性を調べた例であり、案件の選択肢をこの2つに限定しない。Next.js／Keycloak／PostgreSQLを全KOIKIアプリの必須依存にしない。本書では既存の設計・結果文書を参照し、新たな動的検証は行っていない。

## 2. 案件への情報提供と所有分担

提供する情報は、次の3種類を区別する。

| 種類 | KOIKI側から提供できる内容 | 読み方 |
| --- | --- | --- |
| Frameworkの利用条件 | 採用版の対応環境、依存・設定契約、利用するStarterや機能の前提、migration／Security／Audit等の使い方 | 対象版の正本を参照する。検証fixture固有の値を必須条件に含めない |
| 参考構成・手順 | Dockerfile／Compose、buildから実行物への対応付け、secret供給、readiness、停止・再起動、診断手順の例 | 案件のJAR・設定・実行基盤へ置換して再検証する |
| 検証証拠と限界 | 確認環境・source／artifact、正常／拒否／障害の観測、未検証事項・既知の問題 | 当該環境・対象・条件に限定した成立結果として利用する |

| 項目 | KOIKI側の情報提供 | 案件側の設計・実装責任 |
| --- | --- | --- |
| build・版 | Framework依存方法、Java等のbaseline、検証時のtoolchain／image識別 | 案件repoでのbuild、採用版固定、依存／image更新、CIとregistry |
| 実行設定 | Starter設定、Reference例、secretが必要な箇所 | profile構成、環境別設定、secretの保管・供給・権限・更新 |
| DB | migration所有、接続・Session保存・Auditに関する前提 | DB配置・可用性、migration実行主体、role／pool／接続数・容量 |
| 認証・認可 | Java Session方式／Bearer方式の接続例、claimと現在権限の照合例 | 認証方式・IdP・BFF採用、利用者対応、SSO／logout・期限の要件 |
| コンテナ | 非root、read-only、layer、health、ログ・資源上限の参考例 | 本番image、platform、ネットワーク、TLS／proxy、資源見積り |
| 運用 | SIGTERM、同volume／同鍵再起動、障害回復の限定結果 | 配備基盤、drain、複数instance、監視、backup／restore、鍵更新 |

正式な利用条件の入口は[baseline互換性](../architecture/KOIKI-JavaWeb-FW_Baseline_Compatibility_v0.1.md)と[Security利用案内](phase2-developer-journey.md)。本書の検証版・設定値を最新対応版やsupport保証の代わりにしない。

## 3. 2つの方式の違い

| 観点 | 方式1：SPA同梱Java＋DB | 方式2：Next.js BFF＋Java API＋DB＋IdP |
| --- | --- | --- |
| 検証時の常設サービス | Java app、PostgreSQLの2つ | frontend、backend、PostgreSQL、Keycloakの4つ。一時検証helper等は別計上 |
| 画面配信 | Java JARにSPAを同梱し、同じappから配信。ReferenceのMVCも利用 | Next.jsが画面／SSR／BFFを担当、Javaは業務APIを提供 |
| Browserの認証状態 | Java側のSessionを使用。今回の構成ではSpring Session JDBC | BFFのopaque Session CookieとBFF専用store。JavaのBearer API chainはstateless |
| Browserからの業務要求 | JavaへSession付き要求。Session認証とCSRFの境界を維持 | BFFへSession付き要求。BFFからJavaへserver側Access Tokenを付与 |
| IdP | 今回の2サービス構成には含めない | KeycloakでCode＋PKCE認証・SSO・token発行。実案件IdPの採用を決める例ではない |
| Session／token保存 | Java Session・業務／Auditを今回の専用PostgreSQL構成で扱う | 同一PostgreSQL上の業務／BFF／IdPの3 DB・roleを分離。tokenはBFF側で暗号化保存 |
| build成果物 | SPAを含む実行可能Java JAR→Java image | Java JAR→backend image、Next standalone→frontend image。対応sourceをそれぞれ識別 |
| 主な判断点 | 配備単位の集約、Java Sessionの永続化・複数instance時の扱い、SPA／MVC提供 | BFF／Javaの責務、公開／内部URLとissuer、refresh・鍵管理・logout範囲、複数runtimeの運用 |

方式2でJava Sessionを使わないという説明は、当該Bearer API経路についてのもの。FrameworkやReferenceのすべてのSession機能を無効化するという意味ではない。また、Java APIのstatelessと、システム全体が状態を持たないことは異なる。方式2でもBFF／IdP／業務DBの状態を設計する。

実案件の構成選択では、画面とJavaの配備・更新をまとめるか、frontend／BFFを独立させる必要があるか、認証と運用の責任をどこへ置くかを判断材料にできる。今回の結果は両方式の性能・費用・可用性を比較した測定ではなく、方式2が方式1の後継・上位方式という結論は出していない。

## 4. 方式別に提供できる検証情報と注意点

### 4.1 方式1

提供できるのは、hostで生成したJARをruntime imageへ渡す手順、Boot標準layerによる格納、元JAR hash labelによる識別、非root／read-only、secret file、専用DB role、readiness／ログ／資源上限の構成例である。案件では固定Framework依存から自分の実行可能JARを生成し、Referenceのprofile・DB名・seed・業務journeyを置換する。DockerfileだけでSPA同梱・認証・health契約が追加されるわけではない。

改善版では直接回帰25件と既存7 checkpoint、別Windows端末での再生成・再現、Ownerの差戻し・再編集・再提出・承認操作とDB／Auditの限定照合を記録した。layer分割の確認を起動高速化の証明とはしない。Spring Session属性の重複キー事象は別の既知事項で、コンテナ改善で解消したとは扱わない。

- [方式1の資材・案件への置換境界](../../build-support/container-verification/integrated/README.md)
- [Container改善結果と限界](../architecture/validation/phase4-d3-container-improvement-results-20261106.md)
- [別端末Windows再検証・Owner操作後の点検](../architecture/validation/phase4-d3-home-container-reverification-20261107.md)

### 4.2 方式2

提供できるのは、BFFのBrowser SessionとJava Bearer APIの境界、issuer／audience／利用者対応・現在権限検証、state／nonce／PKCE、CSRF／Origin、server側token保護、refresh競合・logout競合・障害回復の確認例である。Next.js BFFとSession storeはTooling-owned fixtureであり、Frameworkが提供する正式BFF／Session製品ではない。Referenceのclaim名・scope・権限語彙も案件仕様へ置換する。

特に次の点が案件設計の判断材料になる。

- **利用者SessionとSSOを分ける。** 同一ブラウザprofile内のA／BはIdP状態を共有しつつ、独立したBFF Sessionを作る。別利用者の試験はChrome／Edge等の独立Cookie領域を使う。同じChrome profileのシークレット窓を2つ開くだけでは分離にならない。
- **logoutの範囲を決める。** local logout、IdP SSO logout、別Clientの即時失効は別要件。今回backchannel logoutは未実装で、別Clientの既存tokenは有効期間内に使える場合がある。token期限後のrefresh拒否と即時失効を混同しない。
- **期限と更新を設計する。** Access Token／Refresh Token／BFF Session／IdP Sessionは別の期限。約60秒のAccess TokenはS5の検証設定であり案件推奨値ではない。交換結果不明時の再送抑止、並行更新、logout後のSession非復活を考慮する。
- **URLと通信経路を整合させる。** 今回のlocalhost／idp.localhostはlocal専用。実案件のTLS／proxy／DNSを含め、Browser・BFF・Javaから到達するissuer・discovery・JWKS・callbackを整合させる。
- **画面の回復導線も検証する。** 手動確認で初回認証transactionエラーがあり原因は未確定。flow Cookieと認証途中recordの期限差等が候補で、単純な再login成功を解消扱いにしない。

S1〜S5の段階別Cloud結果、Windows S5限定PASS、Windows修正後のCloud S5回帰PASSを参照できる。手動確認ではapplicant-a／bの保存・提出・Business Audit、別ブラウザの認証・logout分離と整合する観測を得た。A／Bは同一Origin・同一Next process内のRPであり、別Origin・別アプリ配備の隔離実証ではない。Cookie Pathを強い隔離境界とはしない。

- [方式2の資材と再現手順](../../build-support/container-sso-verification/README.md)
- [段階別計画・検証集合・Cloud／Windows分担](phase4-d3-option2-staged-task-plan-20261109.md)
- [S5 Cloud結果・段階別証拠への追跡](../architecture/validation/phase4-d3-option2-s5-results-20261109.md)
- [Windows S5結果](../architecture/validation/phase4-d3-option2-windows-reproduction-20261109.md)
- [修正後Cloud S5再検証](../architecture/validation/phase4-d3-option2-cloud-harness-reverification-results-20261110.md)
- [Windows手動確認・追加利用者と残件](../architecture/validation/phase4-d3-option2-windows-manual-20261110.md)

## 5. 共通して伝える実行・運用上の条件

両方式の資材から、source→JAR／frontend→imageの識別、digest／lock固定、secretをbuild contextへ含めない構成、必要な書込先、readinessと起動順、ログ容量、CPU／メモリ／pool上限、SIGTERMと同volumeでの再起動を説明できる。ただし検証時の上限値を案件の容量設計へそのまま採用しない。

案件側では、採用する機能・実行環境に応じて次を具体化する。

| 判断対象 | 検証情報を使う際の注意 |
| --- | --- |
| readiness・監視 | Referenceの限定profile／endpointをFrameworkの全アプリ共通契約とみなさず、案件の依存・公開範囲を定義する |
| 永続化・migration | 業務／Framework／BFF／IdPのschema所有と実行順を区別。fixtureのseedは本番migrationではない。今回のDB owner兼用roleを本番の権限分離例としない |
| secret・鍵 | local Compose file mountと本番secret管理を区別。同鍵での復元確認は鍵rotation・紛失・侵害時復旧の検証ではない |
| 停止・再起動 | 静穏時SIGTERM・同volume再開の結果を、処理中drain・業務POSTの再実行安全性・rolling updateの保証にしない |
| build・配布 | host側buildは今回の再現方法。案件CIのmulti-stage build等を制限しない。署名・SBOM・image scan・registry公開等の要否を案件で定める |
| データ・証拠 | Framework／Tooling sourceと、案件データ・secret・DB dump・Browser状態を分離。既存環境へseed／cleanupを誤適用しない |

HTTPS／Secure Cookie・信頼proxy、複数instance／負荷／可用性、backup／restore、本番IdP・実行基盤、別案件の独立build／稼働、運用全体の受入は今回の限定証拠から保証しない。個別結果の未実施・失敗・既知問題を参照したうえで案件に必要な追加検証を決める。

## 6. 今後の利用案内へのつなぎ方

KOIKI側からは、対象Framework版の利用条件を入口に、選択した構成の参考資材、確認環境・正常／拒否／障害の結果、未検証事項を対応付けて案内できる。案件側はその情報を入力として、自分のJAR／画面／IdP／DB／実行基盤に置き換えて受入確認する。

本書で「利用条件」「参考実装」「検証証拠」の案内をまとめたが、正式Developer Guideや配布Templateへの昇格・support追加は行っていない。既存資材の品質評価や本番適合審査を新たに記録する文書でもない。今回の追加成果物はこの引継ぎ整理と入口へのリンクであり、アプリ・Framework・コンテナ設定・依存の変更はない。
