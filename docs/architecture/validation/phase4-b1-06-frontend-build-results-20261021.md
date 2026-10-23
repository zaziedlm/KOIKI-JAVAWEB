# B1-06：Reference frontend・build・配信入口の実装と局所結果

実行日：2026-10-21（JST）

状態：**実装・局所検証完了／65 tests PASS**。通常root全件・実JAR＋DBのbrowser統合実演・DoD 4-8／4-9受入はB1-07〜08に残る。

## 1. 指示・source・変更範囲

Ownerの指示に基づき、B1-05 Cloud再検証文書1 fileだけを`00ce98a`へcommitし、その後にB1-06へ進んだ。branchは`docs/phase4-review-integrated-plan-20261020`、B1-06開始時working treeはclean。B1-06成果のcommit・push・PR作成は行っていない。

| 所有／所在 | 実装 |
| --- | --- |
| Reference frontend：[frontend/](../../../koiki-reference-app/frontend/) | React／TypeScriptの一覧・paging・1明細draft作成・詳細／submit・logout。固定npm依存とlock、型検査・bundle・build検査 |
| Reference build：[pom.xml](../../../koiki-reference-app/pom.xml)、`.gitignore` | 専用`b1-session-spa` profileのNode起動・resource copy。node_modulesのみ追加ignore、生成物はtarget内 |
| Reference配信：[ReferenceSpaWebConfiguration.java](../../../koiki-reference-app/src/main/java/org/koikifw/reference/expense/configuration/ReferenceSpaWebConfiguration.java)、HomeController・home.html | 同梱indexがある場合だけ明示shell forwardとhome link。indexはno-store、未知pathは非forward |
| Reference test：[ReferenceSpaWebConfigurationTest.java](../../../koiki-reference-app/src/test/java/org/koikifw/reference/expense/configuration/ReferenceSpaWebConfigurationTest.java) | isolated classloader／resourceで同梱有無・link・forward・no-store・未知path・不足assetを確認。通常testはnpm成果物に依存しない |
| Tooling：[ReferenceSpaFrontendTest.java](../../../build-support/reference-e2e-verification/src/test/java/org/koikifw/buildsupport/referencee2e/ReferenceSpaFrontendTest.java) | 実Chromium＋loopbackの代替APIでUIを検証。明示propertyで選択し、既存Tooling通常入口を保護 |

B1-05のSession API・Security／CSRF、Application／Domain／Repository、Framework、migration、Root Reactor・workflowは変更していない。新規Java runtime依存・Framework Public API／runtime propertyは追加していない。

金額・version・totalElementsはStringを維持し、金額表示・上限比較にBigIntを使う。POSTを直列化し、409では最新詳細をGETして人の再操作へ渡す。400／422は入力を保持しvalidation項目を表示する。通信断・500／503は結果不明として入力を破棄し、POSTを自動再送しない。401・logout・pagehide・非表示で表示／入力を破棄し、GET abortと世代番号で遅延応答を除外する。Session Cookie・tokenを永続storageへ保存しない。

## 2. 依存・build・JAR境界

Node24.19.0／npm11.9.0、React／React DOM19.3.1、TypeScript7.0.2、Vite8.3.4、型定義19.3.0をregistryの実在・engine／peer条件から再確認し、lockfileVersion3を生成した。exec-maven-plugin3.6.3／maven-resources-plugin3.3.1はReference専用profile内だけで使用する。取得時は既存proxy・CA・checksum／npm integrityを維持した。脆弱性検査を実施済みとはしない。

profileはclean→generate-resourcesでversion／lock照合→npm ci→tsc --noEmit→Vite bundle→asset／入力hash検査→process-resourcesでcopy→既存Boot repackageを行う。lock・versionの自動修正や失敗時fallbackは設けていない。

| 構成 | 結果 |
| --- | --- |
| 通常構成 `-pl koiki-reference-app -am -DskipTests clean package` | 13 projects SUCCESS。Nodeを呼ぶとexit87になるPATHで成功し、JARにSPA／build記録がないことをZIPで確認 |
| 同梱構成 `-pl koiki-reference-app -am -Pb1-session-spa -DskipTests clean package` | 13 projects SUCCESS。index・JS・CSS、静的配信外の`META-INF/koiki-reference-spa-build.json`を確認 |
| 同梱内容 | frontend入力15 fileとlock、index・2 assetのhashを現在source／ZIP bytesと照合。node_modules・frontend source・source map・公開manifest／build記録の混入なし |

同梱build記録のschemaVersionは1。profile・node・npm、`inputs`（frontend相対path→SHA-256、package-lock.jsonを含む）、`assets`（index.html／assets相対path→SHA-256）を持つ。Boot repackage後のMETA-INFはJAR rootに置かれるため、ZIP検査も実際の配置へ合わせた。

両packageはskipTestsであり、全件verifyの代用ではない。通常JARと同梱JARは同名になるため、rawの`normal-jar.json`／`spa-jar.json`にhashと検査結果を別々に保存した。後続B1-07は正式な合流buildのsource／profile／JAR epochを新たに記録する。

確認したJAR SHA-256：通常`e14bd7df972da01ec06a499b491fec8aa8fc51942090561cb2cf851bd1e18682`、最終同梱`fddc42f8e07393e849dc9061d2af5205ada7ad8638db19f483290bbab2ed2abf`。

## 3. 局所検証

| 集合 | tests | 結果 |
| --- | ---: | --- |
| 新規`ReferenceSpaWebConfigurationTest` | 2 | PASS：resource有無の2条件。実bundleが存在する状態でも条件を分離できることを確認 |
| 既存`ReferenceSessionApiSecurityTest` | 22 | PASS：Session／CSRF・SPA認証境界 |
| 既存`ReferenceBusinessUrlSecurityTest` | 23 | PASS：MVC／HTMX |
| 既存`ReferenceArchitectureTest`／`RequiredNegativeArchitectureRulesTest` | 7 | PASS |
| 新規`ReferenceSpaFrontendTest` | 5 | PASS：作成／submitと整数精度、409再取得・最新versionでの明示再操作・401破棄、logout後の遅延GET除外、pagehide／pageshow、結果不明時の入力破棄・非再送 |
| `frontend/scripts/build.test.mjs` | 6 | PASS：lock欠落／不一致、Node／npm不一致、欠落asset・source map・外部参照、古い入力／asset hash、TypeScript errorによる停止 |
| **合計** | **65** | **failure／error／skip 0**。補強前の実行や各case内の比較を重複加算しない |

UI fixtureは`-f build-support/reference-e2e-verification/pom.xml -Dtest=ReferenceSpaFrontendTest -Dkoiki.reference.spa.frontend-fixture.enabled=true -DargLine=-Xmx768m test`で選択する。flagなしでは5件skip・browser非起動を別に確認し、このskipを上記PASSへ加算していない。新規実JAR journeyを実装したものではなく、B1-07の実Session／DB／Audit照合を代替しない。

build負例は`node koiki-reference-app/frontend/scripts/build.test.mjs`で実行し、所有する一時directoryだけを変更・削除した。正式source／lockは故障注入で書き換えていない。desktop／mobileのfixture画面を確認し、PNGはToolingの`target/spa-frontend/`に保存した。

初回frontend型検査はCSS side-effect importの型不足で停止。Vite標準`vite/client`を指定して訂正し、型検査設定を弱めていない。最終package再確認のsandbox実行ではNode→npm起動がEPERMで停止したため、同一commandを権限付きで実行した。ソース・依存versionを変える環境回避は行っていない。ZIP検査器の初回META-INF検索pathは実JARの配置へ訂正し、本体のcopy処理は変更不要だった。

Maven／Node・Chromiumは保存済み環境を使用し、必要な依存を取得した後のMavenはoffline。既存Error Prone／NullAwayを維持した。owned browser context・browser・Playwright・loopback server／executor、一時fixtureを終了した。DBは今回起動していない。Windows用の固定npm command分岐は実装したが、Windowsでのfrontend buildは未実行。

## 4. 証拠と次作業

raw：`/workspace/cloud-setup/logs/phase4-b1-06-20261021/`。各build・選択test・途中停止のlog、最終JUnit XMLとNode負例log、source hash、通常／同梱JARの内容・hash検査を保管する。B1-05 Cloud再検証は別文書・`00ce98a`に保持し、この65件へ混ぜていない。

[B1設計記録§11](../../development/phase4-b1-session-spa-mvc-design-20261020.md#11-b1-06frontendbuild配信入口2026-10-21)へ反映済み。次は**B1-07：通常root・同梱profileの合流回帰と実JARのbrowser統合実演**。実行前に全contextの資源・新規journey・時間／cleanupを具体化し、同じB1 JARで新旧journeyを実行する。DoD受入はB1-08で判定する。
