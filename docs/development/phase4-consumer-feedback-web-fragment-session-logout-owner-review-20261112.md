# 利用側報告2件：共通header fragment・Session logout customizerの是正方針（Owner判断票）

作成日：2026-11-12（JST）

状態：**Owner判断済み（2026-11-12、§7）**。以下の「本体未変更」等は判断票作成時点の記述です。判断前の状態：**DRAFT／Owner判断待ち**。この票は事実確認と是正案だけをまとめています。Framework／Reference／案内文書の本体は変更していません。local commit・remote・PR・新しい版の公開も行っていません。

## 1. 位置づけ

`framework-0.1.0-preview.1`のRelease ZIPから別application（`zaziedlm/koiki-java-scaffold`）を構築した際に、利用側の問題が2件見つかりました。

| ID | 対象 | 報告内容 |
| --- | --- | --- |
| F-A | `koiki-starter-web-mvc`の`templates/koiki/fragments.html` | 配布JARの`header` fragmentにReference固有の表題・業務routeが入っている |
| F-B | `koiki-starter-session-jdbc`の`KoikiSessionJdbcAutoConfiguration` | Applicationが`CookieSerializer`を自分で宣言しないと`koikiSessionLogoutCustomizer`が登録されず、注入するCustomer chainが起動失敗する |

この票は、2件それぞれについて採る方針をOwnerが選ぶためのものです。承認後の実施範囲・検証集合も提示します（§6）。公開済み`framework-0.1.0-preview.1`の再公開・差替えはしません（[E1 release notes](phase4-e1-preview-release-notes-20261103.md)の方針を維持します）。是正は後続版へ載せます。

所有：F-AはFramework（Starter resource）とReference（画面）、F-BはFramework（Starter auto-configuration）です。どちらもJava Public API・POM・依存version・migrationの変更は含みません。

## 2. sourceと確認方法

- 確認したHEAD：`f6ff36d`（branch `claude/eager-hawking-2pyla8`、開始時のworking treeはclean）
- preview.1の公開対象commit `3887d39`からHEADまで、`koiki-starter-web-mvc`と`koiki-starter-session-jdbc`の差分は0です。したがって以下の事実はpreview.1にもそのまま当てはまります。
- 動的確認は、scratch領域の使い捨てgit worktree（HEADから作成）とprobe classで行いました。Repositoryのworking treeは変更していません。probe sourceは[support/consumer-feedback-20261112](../architecture/validation/support/consumer-feedback-20261112/)に置いています。
- 使用環境：JDK 21.0.12、Maven 3.9.16、Spring Boot 4.1.1、Spring Session 4.1.1、Spring Security 7.1.1。`compile`のために、不足していたannotation processor依存（Error Prone／NullAway）を通常のMaven repositoryから取得しました。POM・versionは変更していません。test suiteは実行していません。

## 3. F-A：共通header fragmentのReference依存

### 3.1 事実

- `koiki/fragments.html`は2つのfragmentを定義しています。`head(title)`は汎用で、CSRF meta、`/koiki-web/koiki.css`、HTMX、`koiki-htmx.js`を読み込むだけです。`header`には、表題「KOIKI Reference」と、`/expenses`・`/master/departments`・`/master/expense-categories`・`/identity/users`へのnavがあります。
- 配布JARには`header`も含まれます。そのため、`koiki/fragments :: header`を使うCustomer appには、存在しないrouteのmenuが表示されます。
- Repository内で`header`を使っているのはReferenceの13 templateだけです（`head(title)`の利用も同じ13件）。testは`header`の内容をassertしていません。
- Starter READMEは「`koiki/fragments`を共通head / navigation fragmentとして提供する」と説明しています。一方、同じREADMEには「業務routeを自動構成しない」「Customer固有theme…を所有しない」ともあります。また[P3-C4 closeout](../architecture/validation/phase3-p3-c4-traceability-closeout.md)は、この部品を「業務routeではない」と記録しています。実際のresourceはこれらの記述と食い違っています。
- これはOwnership規則（AGENTS「Reference…のcode、Template…を正式Framework成果物へ直接昇格させない」）にも反しています。

### 3.2 選択肢

| 案 | 内容 | 評価 |
| --- | --- | --- |
| **A1（推奨）** | Starterには`head(title)`だけを残し、`header`を削除する。Referenceは自分の`templates/reference/fragments.html :: header`を持ち、13 templateの参照をそこへ切り替える | Framework側に業務語彙もlayoutの約束も残らない。Spring／Thymeleaf標準の「layoutはApplicationが所有する」に合う。変更はReference内とREADMEに収まる |
| A2 | Starterに`header(appName, items)`のような引数付きの汎用fragmentを用意する | navigation構造・active表示・a11y・i18nといったlayout契約をFrameworkが持つことになる。template単位の互換性も約束しなければならず、確定範囲を超える。採用案件の具体的な要求が出てから判断するほうがよい |
| A3 | 現状のままにして「headerは使わない」と案内だけする | 配布JARにReference語彙が残り続けるため不採用 |

A1の注意点を2つ挙げます。

- ReferenceがStarterと同じ`templates/koiki/fragments.html`を置くと、classpathの順序によって片方がもう片方を隠します。そのため、別のpathにします。
- `header`の削除は、classpath resourceの互換性を壊す変更です（japicmpの対象外）。後続版のrelease notesに「`koiki/fragments :: header`は削除。navigationはApplicationが所有」と明記します。scaffoldは自前のheaderへ切り替える必要があります。

### 3.3 A1の変更対象（承認された場合）

| 所有 | file | 内容 |
| --- | --- | --- |
| Framework | `koiki-starters/koiki-starter-web-mvc/src/main/resources/templates/koiki/fragments.html` | `header` fragmentを削除 |
| Framework doc | `koiki-starters/koiki-starter-web-mvc/README.md` | 「共通head fragment（`head(title)`）だけを提供。header／navigationはApplicationが所有」と改める |
| Reference | `koiki-reference-app/src/main/resources/templates/reference/fragments.html`（新規） | 現行のheaderをそのまま移す（表示は変えない） |
| Reference | 同`templates/**`の13 file | `koiki/fragments :: header` → `reference/fragments :: header` |
| Reference test | 新規1 class（例：`ReferenceTemplateOwnershipTest`） | Starterの`koiki/fragments.html`に`th:fragment="header"`とReference routeが無いこと、Referenceのheaderが既存画面でrenderされることを確認する |
| doc | `docs/development/application-team-handoff-guide.md` | Web MVC Starter行に「共通headだけ。header／navigationはCustomer所有」を追記する |

## 4. F-B：`koikiSessionLogoutCustomizer`が登録されない

### 4.1 事実（報告の原因を訂正）

報告では「Spring Bootは既定で`CookieSerializer`を提供しない」とされていました。確認すると、これは**Boot 4.1.1では当てはまりません**。実際の原因はauto-configurationを評価する順序です。

- `spring-boot-session` 4.1.1の`SessionAutoConfiguration$ServletSessionConfiguration$EmbeddedWebServerConfiguration`は、`server.servlet.session.cookie.*`を反映した`DefaultCookieSerializer` bean（名前は`cookieSerializer`）を登録します。条件はApplicationが`HttpSessionIdResolver`／`CookieSerializer`を宣言していないことです。
- `KoikiSessionJdbcAutoConfiguration`は`afterName = JdbcSessionAutoConfiguration`だけを指定しており、`SessionAutoConfiguration`との順序を指定していません。Bootのsorterはまずclass名の順で並べるため、`org.koikifw…`が`org.springframework…`より先に評価されます。その結果、入れ子の`ServletLogoutConfiguration`にある`@ConditionalOnBean(CookieSerializer.class)`は、Bootのserializerが登録される前に判定されてback offします。
- Referenceは`ReferenceSecurityConfiguration#referenceCookieSerializer`をuser configurationとして宣言しています（user configurationはauto-configurationより先に処理されます）。このため条件が成立し、この欠陥が表に出ませんでした。Customer-like Consumer（`build-support/security-foundation-consumer`）はlogoutを検証しておらず、ここでも検出されていません。既存の`SessionJdbcAutoConfigurationContextTest`も、mockの`CookieSerializer`を直接beanにしています。

probe結果（`LogoutCustomizerOrderProbe`：Boot `SessionAutoConfiguration`＋`JdbcSessionAutoConfiguration`＋KOIKI auto-configurationをWeb contextで合成）：

| 条件 | `CookieSerializer` bean | `koikiSessionLogoutHandler` | `koikiSessionLogoutCustomizer` |
| --- | --- | --- | --- |
| 現行・Applicationの宣言なし | `cookieSerializer`（Boot） | **無し** | **無し** |
| 現行・Applicationが宣言（Referenceと同じ形） | `appCookieSerializer` | 有り | 有り |
| 是正案B1・Applicationの宣言なし | `cookieSerializer`（Boot） | 有り | 有り |
| 是正案B1・Applicationが宣言 | `appCookieSerializer`（Bootはback off） | 有り | 有り |

### 4.2 選択肢

| 案 | 内容 | 評価 |
| --- | --- | --- |
| **B1（推奨）** | `KoikiSessionJdbcAutoConfiguration`の`afterName`に`org.springframework.boot.session.autoconfigure.SessionAutoConfiguration`を加える（1行）。後続版の診断表にも注記する | Spring標準のserializerがそのまま使われ、`server.servlet.session.cookie.*`とKOIKI既定（Secure／HttpOnly／SameSite=Lax）が反映される。Applicationが自分で宣言すれば従来どおりそちらが優先される。順序に循環はない（Hibernate→Audit→Identityの連鎖と交差しない） |
| B2 | Starterが独自の既定`CookieSerializer`を登録する | Bootと同じbeanを重複して実装することになり、Spring標準優先に反する。property反映を自前で維持する必要もある（R-02でReferenceが行った作業の再現）。不採用 |
| B3 | 文書だけで対応する（「`CookieSerializer`を宣言すること」） | journey §6自身が「READMEの回避策で隠さずFramework側のcorrective workとして戻す」としている。欠陥を要件として固定してしまうため、主対応にはしない。ただしpreview.1利用者向けの暫定案内としては必要 |

B1の影響：Applicationが`CookieSerializer`を宣言していなかった環境でも、KOIKIのlogout handlerが有効になります（Session store障害時にlocal stateを消去し、503扱いにする契約）。これはFrameworkが本来意図していた動作です。ただし、その環境の現在のlogout挙動（Spring Security標準の`invalidateHttpSession(true)`）は変わります。

### 4.3 B1の変更対象（承認された場合）

| 所有 | file | 内容 |
| --- | --- | --- |
| Framework | `KoikiSessionJdbcAutoConfiguration.java` | `afterName`へ`SessionAutoConfiguration`を追加 |
| Tooling test | `build-support/security-foundation-verification/.../SessionJdbcAutoConfigurationContextTest.java` | 2件追加。(1) Boot Session構成を含み、Applicationの宣言なしでhandler／customizerが成立し、logout時の削除CookieにSecure／HttpOnly／SameSite=Laxと設定した名前・pathが反映される。(2) Applicationが宣言した場合はそのserializerが使われ、Bootのbeanは登録されない |
| doc | `docs/development/phase2-developer-journey.md` §6 | 診断行を追加：「`koikiSessionLogoutCustomizer`を注入できない（`NoSuchBeanDefinitionException`）」→ First check：KOIKIの版と`CookieSerializer` beanの有無 → Action：preview.1では、Boot `ServerProperties`を反映した`CookieSerializer`をApplicationで宣言する（[R-02](../architecture/validation/phase4-r02-cookie-logout-results-20261021.md)の形）。是正版では宣言は不要 |
| doc | `docs/development/application-team-handoff-guide.md`、`koiki-starters/koiki-starter-session-jdbc/README.md` | 上と同じ注意点と、是正版での扱いを1行ずつ |

## 5. 関連する観測（今回の範囲外として別判断）

O-1：Spring Security 7.1の`HttpSecurityConfiguration`は、`Customizer<HttpSecurity>` beanをすべて、prototypeの`HttpSecurity`へ自動適用します（`applyHttpSecurityCustomizers`）。`LogoutHandlerDuplicationProbe`で次のことを確かめました。

- Referenceのようにchain内で`koikiSessionLogoutCustomizer.customize(http)`を明示的に呼ぶと、`LogoutFilter`内に`koikiSessionLogoutHandler`が**2回**登録されます（5件中2件）。明示的に呼ばなければ1回です（4件中1件）。
- 2回目の実行は、Session再無効化がno-opになるほか、削除`Set-Cookie`がもう一度出るだけです。致命的ではありませんが、意図した構成ではありません。
- 同じ仕組みにより、このbeanが登録されていれば、Bearer／API chainを含む**すべての**chainにKOIKI logout設定が入ります。Referenceでは現在もそうなっています。B1後は、Applicationが`CookieSerializer`を宣言していないCustomerでも同じになります。

選択肢は2つあります。(a) beanのまま維持し、明示呼出しは不要と案内し、Referenceの2箇所の明示呼出しを外す。(b) 自動適用されない型（KOIKI所有のcustomizer型、またはchainごとに明示適用する形）へ変える。(b)はPublic API・設計判断を伴います。そこでこの票ではF-Bと切り離し、**別のOwner判断**とすることを提案します。§6の検証では、2重登録が現状のまま変わらないことを記録するだけにします。

## 6. 承認後の実施条件（案）

- branch：Owner指定のbranch（既定は`claude/eager-hawking-2pyla8`）。clean sourceを固定してから着手します。
- 差分：F-Aはmain resource／template 15 file＋test1＋doc2、F-Bはmain1＋test1＋doc3。合計おおむね23 file以内です。POM・依存・migration・Security matcher・CIは変更しません。
- 検証：
  1. `SessionJdbcAutoConfigurationContextTest`（既存5件＋新規2件）
  2. `ReferenceSessionCookieConfigurationTest`（8件）
  3. `ReferenceBusinessUrlSecurityTest`、`IdentityUrlSecurityTest`、`ReferenceHtmxJourneyTest`、新規template所有test
  4. `ReferenceArchitectureTest`、`RequiredNegativeArchitectureRulesTest`
  5. 通常rootを1回
  6. 可能なら`PackagedReferenceCriticalJourneyTest`を1回（実JARのlogin／logout・画面header）
  失敗した場合は停止して報告し、自動では再実行しません。
- 結果は`docs/architecture/validation/`に記録し、結果の受入は別判断とします。local commitはOwnerの操作または操作前の承認、remote・PR・後続版の公開は個別承認とします。
- scaffold側の修正は、そのRepositoryのOwnerの判断とします。preview.1を使っている間の暫定策は、自前のheaderを使うことと、`CookieSerializer`を宣言することです。

## 7. Owner判断欄

| 判断 | 選択肢 | 推奨 | 判断 |
| --- | --- | --- | --- |
| D-1 F-Aの方針 | A1／A2／A3 | A1 | **A1**（2026-11-12 Owner） |
| D-2 F-Bの方針 | B1／B2／B3（B1・B2を選んだ場合もpreview.1向けの診断行は追加する） | B1 | **B1**（2026-11-12 Owner） |
| D-3 O-1の扱い | 今回の範囲外として別票／今回(a)まで含める | 別票 | **別票**（2026-11-12 Owner） |
| D-4 実施条件 | §6のまま／訂正 | §6のまま | **§6のまま**（2026-11-12 Owner） |
