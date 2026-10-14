# Frameworkローカルartifact準備：最小実地検証（2026-10-06）

**同日追補:** [B preflight／BOM単独修復](phase4-s1-additional-b-preflight-20261006.md)で、座標とPOM／JARの存在だけでは依存準備を保証しない例を確認した。同一SNAPSHOTのcached BOMが古いと、Maven BUILD SUCCESSでもIdentity推移依存が欠落する。Owner承認によりBOMだけをoffline installし、hash一致・警告解消・tree展開・resolve成功を確認した。以下の初回実証は当時の対象／操作として維持する。

**状態: LOCAL PASS — 選択Toolingのjdbc profileで不足解消・offline検証再開。** Framework開発環境全般の準備完了や整備方式の正式採用ではない。

## 実施範囲と入力

Ownerの「作業を阻害したartifact不足を解消対処して進められるものか、実地検証しながら進める」という指示を受領し、[整備検討案](../../development/framework-local-artifact-readiness-options-20261006.md)から、既存cache・一端末・独立Tooling一つを選んで実証した。

対象は`build-support/phase4-level2-verification/pom.xml` / `jdbc`。既存Wrapper、Maven local repository `C:\Users\s-kataoka\.m2\repository`と既存設定を使用し、必要外部依存・pluginを取得した。prep用dependency pluginは既存cacheにあった`3.7.0`を完全座標で指定。`--no-snapshot-updates`で更新を抑止し、KOIKI local install / remote publishは行っていない。Rootの全資材・全profile・全Toolingの取得は対象にしていない。

source baselineは`docs/daily-development-workflow` / `6a76b81b034f5d841699cea03d779c62c0cce009`。開始時dirty差分は前段のpreflight / 検討文書のみ。POM / 正式sourceは変更していない。開始承認をAの元契約へ遡って加えず、環境整備として実施し、結果を承認済みAの再開入力へ渡した。

## 不足から復旧まで

前段の[preflight Evidence](phase4-s1-additional-a-preflight-20261006.md)は必須`spring-modulith-starter-jdbc:2.1.1` JAR / POM欠落を確認して停止している。今回は同じcacheへ取得し、同じPOM / profileでoffline解決を再確認した。実行はMaven cache / Dockerアクセスが可能な権限付き実行で、環境の承認手順に従った。

```powershell
# Repository root。online準備。
.\mvnw.cmd --batch-mode --no-transfer-progress --no-snapshot-updates -f build-support/phase4-level2-verification/pom.xml -Pjdbc org.apache.maven.plugins:maven-dependency-plugin:3.7.0:resolve
.\mvnw.cmd --batch-mode --no-transfer-progress --no-snapshot-updates -f build-support/phase4-level2-verification/pom.xml -Pjdbc org.apache.maven.plugins:maven-dependency-plugin:3.7.0:go-offline
# 同じ対象のoffline解決と既存DB test。
.\mvnw.cmd -o --batch-mode --no-transfer-progress -f build-support/phase4-level2-verification/pom.xml -Pjdbc org.apache.maven.plugins:maven-dependency-plugin:3.7.0:resolve
.\mvnw.cmd -o -f build-support/phase4-level2-verification/pom.xml -Pjdbc "-Dtest=S1PermitStorageTest" "-DforkCount=1" "-DargLine=-Xmx768m" "-Djunit.jupiter.execution.parallel.enabled=false" "-Dsurefire.failIfNoSpecifiedTests=true" test
```

raw console・resolved artifactsの出力先は下記run directoryへ指定して保存した。上記は操作の本体であり、consoleリダイレクト・exit記録は省略している。

| 操作 | 結果 / Maven時間 / 終了JST |
|---|---|
| online resolve | exit0、2.898秒、10:18:16 |
| online go-offline | exit0、24.326秒、10:19:03 |
| offline resolve | exit0、1.573秒、10:19:45 |
| offline既存L1 | exit0、11 tests / failures0 / errors0 / skipped0。Maven14.207秒、test4.376秒、10:20:01 |
| 続く承認済みA | 最終18 tests成功。詳細は[A Evidence](phase4-s1-additional-a-jpa-20261006.md) |

取得物のrepository IDはResolver記録で`central`。`--no-snapshot-updates`をhash不変性保証とはせず、今回KOIKI再installやpublishをしていないことと分けて記録する。

| 欠落から取得した資材 | SHA-256 |
|---|---|
| spring-modulith-starter-jdbc-2.1.1.jar | `e074243cc659b3c8846515de8c20ce589e1fce7a811a7fbc140f6b1eb4f1cac9` |
| spring-modulith-starter-jdbc-2.1.1.pom | `be4960da8728debe0ce480452a56032b0fe3e5de54a1f1883ae5385b8fa76f0a` |

同じWrapper / POMでtest compileと実DB testまで進められ、POM / version / 権限の変更による代替を必要としなかった。L1 source / 専用SQLは既存のまま使用。DB / Ryukの終了後一覧は0件。

## 現在値・警告・限界

Java21 / Maven3.9.16、Docker Server29.1.3、PostgreSQL17.11 / 固定digest、Boot4.1.1 / Hibernate7.4.5.Final / Modulith2.1.1を使用。A再開前のavailable memory7,751,544 KiB、disk free515,503,816,704 bytes。

Surefireはnative stream警告を出した。dumpstreamはJDKの`cds` shared archive build差異の起動警告であり、XMLとexitで成功を確認した。JDK / archive / JVM optionの変更へは広げず、警告は保存した。

これは既存cacheの不足を補った実証であり、空cache初回準備、Root全体のoffline build、jdbc以外、browser、IDE / CLI一致、別OS、KOIKI sourceからのlocal install / 固定stage更新は未検証。総download量・cache増分容量・processの連続peak資源は未計測。検討案の全受入条件を満たしたとはしない。

## 保存と次の整備

run IDは`local-20261006-01`。rawは`build-support/phase4-level2-verification/target/artifact-readiness-evidence/local-20261006-01/`へ保存。online / offline console・exit、解決artifact一覧、L1 XML、取得hash / repository ID、既存source hashes、Docker / cleanup・memory / disk、manifestとhash一覧を保持する。Git管理外のrawは文書checksumの代替として自動削除しない。

今回、標準Mavenによる「online準備→offline resolve→限定test」で復旧できることを確認した。次の環境整備ではRoot共通セットの対象goalと独立Toolingの選択一覧を確定し、初回 / 再利用 / 更新の容量・時間を測り、実証済み範囲だけを恒常手順へ反映する。prep専用script、settings / Parent / CI変更、全Tooling実行は未実施。
