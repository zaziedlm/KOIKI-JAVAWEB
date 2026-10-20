# R-02：Session Cookie設定反映・logout構成の確認結果

実行日：2026-10-21（JST）

状態：**限定変更・局所検証完了／57 tests PASS**。通常root全件・Session SPA機能・DoD全体の受入を兼ねない。

## 1. sourceと変更範囲

OwnerのR-02着手指示に基づき、`docs/phase4-review-integrated-plan-20261020`で対応。base HEADは`a4e839fce2fa882a3e683a7c71c2f7d58dd5ea5f`。開始時working treeはclean。今回commit・push・PR作成は行っていない。

| 所有／file | 内容 |
| --- | --- |
| Reference：[ReferenceSecurityConfiguration.java](../../../koiki-reference-app/src/main/java/org/koikifw/reference/identity/configuration/ReferenceSecurityConfiguration.java) | 独自CookieSerializer Beanを維持し、`ServerProperties`を有効化・注入。BootのCookie設定を`PropertyMapper`で反映 |
| Reference test：[ReferenceSessionCookieConfigurationTest.java](../../../koiki-reference-app/src/test/java/org/koikifw/referenceacceptance/web/ReferenceSessionCookieConfigurationTest.java) | Boot Session／JDBC・Framework logout・Reference Securityを組み合わせる8件。mock repositoryで構成を分離し、発行／削除の実Set-Cookieをassert |
| Tooling：[PackagedReferenceCriticalJourneyTest.java](../../../build-support/reference-e2e-verification/src/test/java/org/koikifw/buildsupport/referencee2e/PackagedReferenceCriticalJourneyTest.java) | HTTP子processにSecure=falseを明示。既存journeyへCookie属性・CSRF付きlogout・削除Cookie・旧Cookie拒否・DB Session消去を追加。READMEも更新 |

反映対象はCookie名・Domain・Path・HttpOnly・Secure・Max-Age・SameSite・Partitioned。未設定値はSerializer既定を維持し、SameSiteの明示省略は属性を除去する。Framework既定Secure=true／HttpOnly=true／SameSite=Laxは変更していない。Domain共有・context-path変更をB1の採用構成へ追加したものではない。

Boot管理へ戻す案ではSerializerの登録と条件評価をBoot側へ移すことになる。今回は既存のReference Beanを維持し、設定反映だけを追加する最小修正を選んだ。Bootを含む合成testと実JAR起動でhandler／customizerの成立を確認できたため、FrameworkのAutoConfiguration順序・logout handlerは修正不要と判断した。Boot管理案そのものの実証・一般的な互換性保証は行っていない。

POM・依存version・migration・Security matcher・既定のCSRF処理・R-01 Adviceは無変更。SPA用API／CSRF Cookie・frontend・R-03は未実装。

## 2. 検証結果

保存済みCloud activation、JDK21、Maven3.9.16を使用。Referenceの選択回帰後に`-pl koiki-reference-app -am -DskipTests clean install`を実行し、13 projects SUCCESS。main／testのclean compileと実JAR生成を確認し、Root外Tooling用に現sourceの内部artifactをlocal cacheへ準備した。これは全件test実行ではない。

| 検証class | tests | 結果・範囲 |
| --- | ---: | --- |
| `ReferenceSessionCookieConfigurationTest` | 8 | PASS：既定属性、Secure明示true／false×requestSecure両条件、SameSite Strict／省略、名前・Path・Max-Age、logoutとBean成立 |
| `ReferenceBusinessUrlSecurityTest` | 23 | PASS：既存MVC／HTMX等 |
| `ApiExceptionBoundarySecurityTest` | 13 | PASS：R-01のSecurity／例外境界 |
| `ReferenceArchitectureTest` | 2 | PASS |
| `RequiredNegativeArchitectureRulesTest` | 5 | PASS |
| `SessionJdbcAutoConfigurationContextTest` | 5 | PASS：既存既定・guard・Session失効・永続store失敗時のlocal cleanup |
| `PackagedReferenceCriticalJourneyTest` | 1 | PASS：実JAR・PostgreSQL17・実Chromium、既存Bearer／MVC業務経路・Audit・logout・sanitized log・cleanup |
| **合計** | **57** | **failure／error／skip 0** |

Cookie構成testは最初7件で全件PASS後、Path上書き時にも発行・削除が一致することを確認するためlogoutを2条件へ拡充し、最終8件PASS。旧7件を重複加算していない。他のReference直接回帰43件は同じproduction sourceで実行した。

実JARではlogin後のSESSIONがHttpOnly・SameSite=Lax・Path=/、HTTP fixtureではSecureなしであることを確認。CSRF付きPOST logoutの302と削除Set-Cookie、browser Cookie消去、旧Cookieを再注入したGETのlogin遷移、DBの当該principal Session件数0をassertした。既存の申請作成・提出・承認とBusiness／Security Audit照合もPASS。

Reference JAR SHA-256：`92c2a45dde5cf8eed8550e446e6528f5bb75f934598a57054ec1ba071344e61b`。JAR内の変更classとtarget/classesのbyte一致を照合した。

## 3. 実行入口・途中経過・cleanup

- Reference選択回帰：`./mvnw --batch-mode --no-transfer-progress -o -pl koiki-reference-app -am -Dtest=ReferenceSessionCookieConfigurationTest,ReferenceBusinessUrlSecurityTest,ApiExceptionBoundarySecurityTest,ReferenceArchitectureTest,RequiredNegativeArchitectureRulesTest -Dsurefire.failIfNoSpecifiedTests=false test`。Path拡充後は新規classだけ再実行。
- Root外既存回帰：`./mvnw --batch-mode --no-transfer-progress -f build-support/security-foundation-verification/pom.xml -Dtest=SessionJdbcAutoConfigurationContextTest test`。
- 実JAR：Root外E2E POMに`-Dtest=PackagedReferenceCriticalJourneyTest -DforkCount=1 -DreuseForks=false -DargLine=-Xmx768m -Djunit.jupiter.execution.parallel.enabled=false -Dkoiki.reference.verification.resource-limits.enabled=true test`。Maven heapも768 MiB。既存image／Chromiumだけを使用。
- 修正前のCookie合成7件は7 failure。Secure／SameSite／Cookie名等の無反映を再現した。fixture配置準備時の空選択は成功件数に含めない。
- Security Tooling初回は宣言済みOAuth2 test starter2点のcache不足で実行前停止。設定済みproxy・CA・strict checksumを維持して既存依存を準備し、5件PASS。POM・version変更なし。その他のbuild・Reference・E2Eはoffline。
- Mockito attach・Docker／browser／loopbackに必要な権限付き実行を使用。DB同時1、memory1 GiB／CPU1／max_connections16、子JVM768 MiB・pool4／idle1を実効値で照合。既存S1予算を再利用したものではなく、既存fixtureの資源制限をR-02にも適用した。
- E2Eのfinallyとassertでbrowser・issuer・application・DB・一時log・portのcleanupを確認。追加の読取でも所有DB containerの消去、子PID終了、稼働containerなしを確認。他runの資源は操作していない。

raw記録：`/workspace/cloud-setup/logs/phase4-r02-cookie-logout-20261021/`。変更前・各選択集合・clean build・依存不足のlog、最終7 classのXMLと`selected-test-results.json`、`source.patch`・新規test source・`source.sha256`・`source-artifact.json`を保存。未commitのためbase SHA＋差分＋hashで識別する。

## 4. 引継ぎ

[B1設計記録§7.6](../../development/phase4-b1-session-spa-mvc-design-20261020.md#76-r-02の作業範囲と実行結果2026-10-21)へ反映済み。次は**R-03：認可をversion照合より先に行う是正**。通常root全件は基盤是正の合流時にまとめる計画を維持し、今回の57件をB1機能・DoD全体の受入へ拡張しない。
