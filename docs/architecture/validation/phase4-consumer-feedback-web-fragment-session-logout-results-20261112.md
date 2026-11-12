# 利用側報告2件（共通header fragment・Session logout customizer）の是正結果（2026-11-12）

状態：**実装・限定検証済み／結果のOwner受入は未記録**

## 1. 位置づけ

[Owner判断票](../../development/phase4-consumer-feedback-web-fragment-session-logout-owner-review-20261112.md)§7で、
2026-11-12にOwnerがD-1＝A1、D-2＝B1、D-3＝O-1は別票、D-4＝§6の実施条件のまま、を選んだ。
本書はその実施結果である。作業branchは`claude/eager-hawking-2pyla8`（判断票commit `47f77b3`、base main `f6ff36d`）。
実施はClaude Code cloud session。後続版の作成・公開は本書の範囲外とし、公開済み`framework-0.1.0-preview.1`は変更しない。

## 2. 変更内容

### 2.1 F-A（A1）：共通header fragmentをReferenceへ移す

| 所有 | file | 内容 |
| --- | --- | --- |
| Framework | `koiki-starters/koiki-starter-web-mvc/src/main/resources/templates/koiki/fragments.html` | `header` fragmentを削除。`head(title)`だけを残す |
| Framework doc | `koiki-starters/koiki-starter-web-mvc/README.md` | 共通head fragmentだけを提供し、header／navigationはApplication所有と記載。preview.1までの`header`削除を明記 |
| Reference | `koiki-reference-app/src/main/resources/templates/reference/fragments.html`（新規） | 旧`header`のmarkupをそのまま移設（表示は不変） |
| Reference | 同`templates/`の13 file | `~{koiki/fragments :: header}` → `~{reference/fragments :: header}` |
| Reference test | `ReferenceTemplateOwnershipTest`（新規、3件） | Starterの`koiki/fragments.html`がclasspath上に1つだけで`head(title)`だけを持ち、`header`・Reference route・表題を含まない。Reference templateに`koiki/fragments :: header`参照がなく、`reference/fragments :: header`参照が13件。home画面にReferenceのheaderと共通styleがrenderされる |
| doc | `docs/development/application-team-handoff-guide.md` | Web MVC Starter行に「共通templateは`head(title)`だけ、header／navigationはCustomer所有」を追記 |

### 2.2 F-B（B1）：Session JDBC auto-configurationの評価順

| 所有 | file | 内容 |
| --- | --- | --- |
| Framework | `KoikiSessionJdbcAutoConfiguration.java` | `afterName`に`org.springframework.boot.session.autoconfigure.SessionAutoConfiguration`を追加（Javadocで理由を記載） |
| Tooling test | `build-support/security-foundation-verification/.../SessionJdbcAutoConfigurationContextTest.java` | 2件追加（§3.1） |
| doc | `docs/development/phase2-developer-journey.md` §6 | 診断行「`koikiSessionLogoutCustomizer`を注入できない」を追加。preview.1ではApplicationで`CookieSerializer`を宣言、是正版では不要 |
| doc | `docs/development/application-team-handoff-guide.md` | Session JDBC Starter行にpreview.1の注意を追記 |
| doc | `koiki-starters/koiki-starter-session-jdbc/README.md` | logout handler／customizerが使う`CookieSerializer`と評価順、preview.1との差を追記 |

POM・依存version・migration・Security matcher・CIは変更していない。

## 3. 検証結果

環境：Claude Code cloud session（Linux、OpenJDK 21.0.12、Maven Wrapper 3.9.16、Docker 29.9.2）。
TestcontainersのRyuk containerは`TESTCONTAINERS_RYUK_DISABLED=true`で無効化した（Docker Hubの匿名pull制限への環境対処）。

### 3.1 `SessionJdbcAutoConfigurationContextTest`（判断票§6-1）

| 実行 | 結果 |
| --- | --- |
| 是正後 | 7件PASS（既存5＋新規2） |
| 負の対照：`afterName`の追加だけを一時的に戻してStarterを再install | 新規`usesBootSessionCookieSerializerWhenTheApplicationDeclaresNone`だけがFAIL（6 PASS／1 FAIL）。是正を戻して再installし、上の7件PASSの状態に復帰 |

新規2件：

1. Boot `SessionAutoConfiguration`＋`JdbcSessionAutoConfiguration`＋KOIKIを組込みserver context（`server.port=0`）で合成し、Applicationの`CookieSerializer`宣言なしで、Bootの`cookieSerializer`、`koikiSessionLogoutHandler`、`koikiSessionLogoutCustomizer`が成立する。logout時の削除Cookieに、設定したcookie名・path、`Max-Age=0`、`Secure`、`HttpOnly`、`SameSite=Lax`が反映される。
2. Applicationが`CookieSerializer`を宣言した場合、そのserializerだけが登録され（Bootの`cookieSerializer`はback off）、削除Cookieはそのserializerの名前で出力される。

テスト作成時の訂正：初回、新規1件目がFAILした。原因は`WebApplicationContextRunner`既定のmock ServletContextで、
BootがWAR配備とみなして`ServerProperties`を反映しない`WarDeploymentConfiguration`のserializerを登録したことである
（customizer／handlerはこの時点で成立していた）。実Applicationと同じ組込みserver context
（`AnnotationConfigServletWebServerApplicationContext`＋`TomcatServletWebServerAutoConfiguration`）へ変えて再実行した。
production codeは変えていない。

### 3.2 Reference・Architecture（判断票§6-2〜4）

| test | 結果 |
| --- | --- |
| `ReferenceSessionCookieConfigurationTest` | 8件PASS |
| `ReferenceBusinessUrlSecurityTest` | 25件PASS |
| `IdentityUrlSecurityTest` | 8件PASS |
| `ReferenceTemplateOwnershipTest`（新規） | 3件PASS |
| `ReferenceArchitectureTest` | 2件PASS |
| `RequiredNegativeArchitectureRulesTest` | 5件PASS |
| `ReferenceHtmxJourneyTest` | **未実施**。`build-support/reference-browser-verification`の手動browser検証で、起動済みApplication・demo seed・login passwordの手入力を要するため |

### 3.3 通常root（判断票§6-5）

`./mvnw clean verify`を1回実行した。554件実行、failure 1、error 0、skip 100。

- 是正前の同環境の実行（2026-11-11、main `f6ff36d`）は551件、failure 1、skip 100。増分3件は`ReferenceTemplateOwnershipTest`。
- failure 1件は`ManagedRecoveryConfigurationLoaderTest.L11RejectsMissingNonFileAndIndirectPaths`。読取権限を外したfileの読込拒否を確かめる負例で、cloud containerがroot（uid 0）で動くため権限が効かずに失敗する。同じJDKで非rootユーザーでは`AccessDeniedException`になることを確認済み（2026-11-11）。今回の変更とは無関係。
- 上記によりReactorはReference ApplicationのSurefireで停止し、root aggregatorはSKIPPEDとなった。Reference以外の全moduleはSUCCESS。

### 3.4 packaged Reference E2E（判断票§6-6、可能なら）

`PackagedReferenceCriticalJourneyTest`は**未成立（環境要因）**。Reference JARのpackageは成功したが、
Playwright Java 1.62.0が要求するChromium headless shell（revision 1234）がcloud環境にない（導入済みは1243）。
browserの追加downloadやtestの起動設定変更は行わず停止した。同testはGitHub Actionsの
「Phase 3 Critical Journey E2E」jobで実行されるため、PRのCIで確認する。

### 3.5 O-1の現状記録（D-3）

判断票同梱のprobeを、是正後のReference test classpathでad hocにcompile・実行した（build・repositoryへは追加しない）。

| probe | 結果 |
| --- | --- |
| `LogoutHandlerDuplicationProbe` | chain内で明示`customize(http)`：`LogoutFilter`内の`koikiSessionLogoutHandler`が5件中2件。明示呼出しなし：4件中1件。**是正前と同じ**（O-1は未変更） |
| `LogoutCustomizerOrderProbe` | A（Applicationの宣言なし）：`cookieSerializer`（Boot）、handler／customizerとも**有り**（是正前は無し）。B（Applicationが宣言）：`appCookieSerializer`、handler／customizerとも有り |

O-1（`Customizer<HttpSecurity>` beanの全chain自動適用と2重登録）は別のOwner判断票で扱う。B1により、
Applicationが`CookieSerializer`を宣言していないCustomerでも、KOIKI logout設定が全chainへ自動適用される点は判断票§4.2・§5のとおり。

## 4. 未完了・後続

- 結果のOwner受入。
- PRのCI（特に「Phase 3 Critical Journey E2E」）での確認。
- O-1の別判断票。
- 後続版への収録（release notesに「`koiki/fragments :: header`削除、navigationはApplication所有」「Session logoutはBoot既定serializerで成立」を記載）。ソースJAR・Javadoc JAR同梱の`0.1.0-preview.2`（[引継ぎ](../../development/phase4-e1-sources-javadoc-handoff-20261112.md)）へまとめるかは別判断。
- 利用側`zaziedlm/koiki-java-scaffold`は、自前headerを既に使用しており、`CookieSerializer`宣言は是正版の取込み後に削除できる。
