# Reference Session SPA

Reference所有のReact／TypeScript UI。既存Session APIを同一originで使用し、申請一覧・1明細のdraft作成・詳細・submitを提供する。承認・会計操作は既存MVCを利用する。

Node 24.19.0／npm 11.9.0を使用する。依存versionはpackage.jsonとlockで固定する。lockの自動修正・自動POST再送・ブラウザー永続storageは使用しない。

```bash
# リポジトリrootから、専用profileで同梱JARを作成
./mvnw -pl koiki-reference-app -am -Pb1-session-spa clean package

# build guardの負例（依存準備後）
node koiki-reference-app/frontend/scripts/build.test.mjs

# bundleに対するUI fixture（実JAR・DBの統合検証とは別）
./mvnw -f build-support/reference-e2e-verification/pom.xml -Dtest=ReferenceSpaFrontendTest -Dkoiki.reference.spa.frontend-fixture.enabled=true test
```

通常のprofile未指定buildはNodeを呼ばず、SPAを同梱しない。同梱JARでは既存のlogin後にhomeからSPAを開く。`/spa/`配下のhash routingを使い、未知pathへのshell fallbackは設けない。

frontendの生成物は`koiki-reference-app/target/b1-frontend/`、入力・lock・assetのSHA-256記録は`target/koiki-reference-spa-build.json`。同梱JARにはindexとassets、静的配信外の`META-INF/koiki-reference-spa-build.json`を含める。Node、source map、node_modules、frontend sourceはJARへ含めない。

更新の通信断・500／503では結果不明として表示し、自動再送しない。再読込して業務状態を照合する。logout開始・401・ページ離脱で表示・入力を破棄する。logout失敗時にはサーバー側Sessionの失効を成立済みと扱わない。

B1-07で、通常root／専用profileの全件回帰と、同一JARの新旧browser journey・実DB／Audit照合を実施する。
