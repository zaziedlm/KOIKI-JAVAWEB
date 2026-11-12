# 利用側報告2件の是正：Windows検証結果（2026-11-12）

状態：**Owner承認後のclean再実行・通常root・packagedブラウザ検証PASS（§5）／結果のOwner受入は未記録**。初回FAILを§2〜4に保持する。その各節の「未実施」「HOLD」「再開案」は再実行承認前の履歴であり、現在の結果は§5を参照する。

## 1. 対象・環境

- Ownerの「対象テスト→通常root→packagedブラウザ検証の順で進めてください」に基づく、このWindows環境での初回検証。
- branch：`claude/eager-hawking-2pyla8`、source：`ac23657d1283955857915e1cd8fd651ff5e1b6c9`。開始時のtracked working treeはclean、local／origin一致。
- Windows PowerShell 7、Temurin JDK `21.0.13+9-LTS`、Maven Wrapper `3.9.16`、Spring Boot `4.1.1`。
- Maven実行processごとに`JAVA_HOME`を実際のJDK 21へ設定。Maven heap768 MiB、test fork1・heap768 MiB・fork再利用なし・JUnit並列無効。system PATH・POM・依存version・production sourceは変更していない。
- Docker Server `29.5.3`。開始時の`docker ps`は0件。`postgres:17-alpine`、`testcontainers/ryuk:0.14.0`とPlaywright Chromium／headless shell revision `1234`はローカルに存在した。保持中として記録されている手動環境を起動・停止・cleanup・再seedしていない。
- Docker読取はsandboxでnamed pipeへのpermission deniedとなり、承認された権限付き実行で確認した。Engine停止とは扱っていない。
- Claude Code cloudの[結果](phase4-consumer-feedback-web-fragment-session-logout-results-20261112.md)とは別証拠である。

## 2. 実行結果

時刻はJST。

| 段階 | 実行 | 結果 |
| --- | --- | --- |
| 準備 18:50:30〜18:51:11 | RootからReferenceを除く15 projectを`-DskipTests clean install` | SUCCESS。現HEADのFrameworkをローカルMaven repositoryへ登録。remote publishではなく、test PASS件数へ加算しない |
| 対象1 18:51:23〜18:51:35 | `SessionJdbcAutoConfigurationContextTest` | 7件PASS、failure／error／skip0 |
| 対象2 18:51:49〜18:52:38 | Reference／Architectureの6 classを選択してReactorの`test` | exit1。期待した51件PASSに加え、不整合classの28件がerrorとなり、実行全体はFAIL（79件、failure0／error28／skip0） |
| 通常root | `clean verify` | 未実施。前段FAILによりHOLD |
| packagedブラウザ | clean package後の`PackagedReferenceCriticalJourneyTest` | 未実施。前段FAILによりHOLD |

期待した対象2の内訳：

| class（実際のpackage） | PASS |
| --- | --- |
| `org.koikifw.referenceacceptance.web.ReferenceSessionCookieConfigurationTest` | 8 |
| `org.koikifw.referenceacceptance.web.ReferenceBusinessUrlSecurityTest` | 25 |
| `org.koikifw.reference.identity.adapter.inbound.web.IdentityUrlSecurityTest` | 8 |
| `org.koikifw.referenceacceptance.web.ReferenceTemplateOwnershipTest` | 3 |
| `org.koikifw.reference.ReferenceArchitectureTest` | 2 |
| `org.koikifw.archunit.RequiredNegativeArchitectureRulesTest` | 5 |
| 合計 | 51 |

Session7件とは別に集計する。`ReferenceHtmxJourneyTest`は別起動・credential入力を要する手動環境向けのToolingであり、今回の対象2には含めていない。packaged journeyの成立で同test実行済みとはしない。

## 3. 停止原因

対象2は`clean`なしの`test`で実行してしまった。Referenceの`target/test-classes`には、Maven実行前に作成された次の不整合classが残っていた。

- `org.koikifw.reference.web.ReferenceBusinessUrlSecurityTest`：25件error。
- `org.koikifw.reference.web.ReferenceTemplateOwnershipTest`：3件error。

同名の現行sourceは`org.koikifw.referenceacceptance.web`を宣言しており、今回Mavenが生成した正しいpackageのclassも存在し、そちらは成功した。不整合classは単純class名での選択にも一致したため、両packageが実行された。

代表error：

```text
java.lang.Error:
Unresolved compilation problem:
The declared package "org.koikifw.referenceacceptance.web" does not match the expected package "org.koikifw.reference.web"
```

`ReferenceTemplateOwnershipTest.class`の更新時刻は、不整合package側が18:46:49、Maven生成の正しいpackage側が18:52:06。Maven実行前のclass残留を確認できる。生成元processの特定は行っていない。これはClaude cloudで残ったroot権限起因failureやbrowser revision不一致とは異なる、この端末の出力状態に起因する停止である。

実行側でReference出力のcleanを先行すべきだった。現段階でproduction修正の不備とは判定しない一方、実行全体のFAILを対象51件の成功で置き換えない。

## 4. 証拠・再開点

端末内のignored Evidence：`.tmp/consumer-feedback-win-20261112/`。

- `run.ps1`：段階別commandと集計。`Stage.log`、`Session.log`、`Focused.log`と各`*-summary.json`。
- `Session-reports/`、`Focused-reports/`：各実行で更新されたSurefire XMLの保全。後続clean前にコピー済み。
- 上記はGitで転送されない。raw log／XMLの外部共有やsecretを含む資料の公開は行っていない。

[Owner判断票§6](../../development/phase4-consumer-feedback-web-fragment-session-logout-owner-review-20261112.md#6-承認後の実施条件案)の「失敗した場合は停止して報告し、自動では再実行しません」に従い、再実行・後続実行は行っていない。

再開案は、保全済みFAILを保持したうえでReferenceの出力を`clean`し、同じ対象集合を1回再実行すること。その成功後に通常root1回、clean package、packagedブラウザ1回へ進む。IDE等が出力を再生成する可能性もあるため、clean後は不整合packageのclass残留・再出現を確認する。production codeやpackage宣言を今回の環境対処として変更しない。

O-1（二重登録・全chain自動適用）の別判断、後続版の作成・公開、commit／push／PR／merge、D3／Phase4全体の認定は今回の実行範囲外である。

## 5. Owner承認後のclean再実行・後続検証

Ownerの「Referenceのclean を理解しました。再実行してください。」により、同じ対象集合をclean後に1回再実行し、成功後に通常root→clean package→packagedブラウザの順で実行した。sourceは引き続き`ac23657d1283955857915e1cd8fd651ff5e1b6c9`。再開時のworking tree差分は本Windows記録とAGENTSの文書だけで、production／test source・POM・依存versionは変更していない。

### 5.1 結果と集計

| 段階（JST） | 結果 |
| --- | --- |
| 対象再実行 18:57:24〜18:58:51 | `-pl koiki-reference-app -am`で§2と同じ6 classを`clean test`。51件PASS、failure／error／skip0 |
| 通常root 18:59:50〜19:04:59 | `clean verify`を1回。全16 project SUCCESS。**Surefire総数554件＝454件PASS＋100件skip**、failure／error0 |
| E2E用package 19:07:21〜19:08:16 | `-pl koiki-reference-app -am -DskipTests clean package`。SUCCESS。test件数へ加算しない |
| packagedブラウザ 19:10:30〜19:11:02 | `PackagedReferenceCriticalJourneyTest`を1回。1件PASS、failure／error／skip0 |
| 終了後読取 19:11:46 | 新規container残留0、既存container欠落0、稼働container0、子Java PID13856終了 |

前段で成功済みのSession context7件は再反復していない。対象51件の内訳は§2と同じで、対象実行とrootの重複を新規coverageとして足し合わせない。通常rootの554はskipを含むXMLの`tests`合計であり、「554件PASS＋100件skip」とはしない。内訳はFramework／Architecture111件PASS、Reference343件PASS＋100件skip。専用opt-in集合のskipをその検証成立として扱わない。

初回エラーとなった2つの不整合classは、clean後のコンパイル時、対象再実行後、root後のいずれも存在しなかった。再実行のSurefireは期待した6 suiteだけであり、余分な28件の実行はなかった。初回FAILのlog／XMLは元のEvidence directoryへ保持している。

Cloud側でfailureが記録された`ManagedRecoveryConfigurationLoaderTest.L11RejectsMissingNonFileAndIndirectPaths`を含む同class12件は、今回Windowsの通常rootで全PASS。Cloud側の過去結果を成功へ書き換えるものではない。

### 5.2 packagedブラウザの範囲

既存のJava Playwright `1.62.0`／実Chromium headlessで、clean packageしたJARに対する既存critical journeyを実行した。既存のresource-limits flagを明示し、DB memory1 GiB／CPU1／max_connections16、子Java heap768 MiB／pool最大4・minimum idle1、test fork heap768 MiBをharness内で照合した。ブラウザ・image・依存versionの更新はしていない。

成立した範囲：

- Bearer APIによる申請作成・取得・提出。
- 実ブラウザによるSession login、home描画、master HTMX検索とfragment応答・検索focus、同申請の承認。
- Session CookieのHttpOnly／SameSite=Lax／Path、loopback HTTP fixtureとしてSecure=false。
- CSRF付きlogout、削除Cookie、ブラウザ上のCookie消去、旧Cookie再注入後の認証拒否。
- 承認済みAPI応答、DBの`APPROVED`／version3／合計1,200円／明細1件の合計一致、Business／Security Audit、logout後のprincipal Session消去。
- process logの秘密非出力検査、子process／DB／一時process log／portのcleanup。

header自体の所有・markup・homeへのrenderは対象testの3件で確認し、packaged journeyはその画面を含む既存経路の回帰として扱う。独立した`ReferenceHtmxJourneyTest`の実施、全画面・全権限・O-1の是正、本番HTTPS運用の保証へ広げない。

実行前後で同一JARのSHA-256が一致：

```text
c5b1cc3c34187660c50cc61fb9750ec0e24bb18b657ed326d0325d41f8157df9
```

E2E直後の最初のDocker snapshotには自動終了待ちのRyukが1件残っていたため、その時点では全cleanup完了としなかった。19:11:46の最終読取でRyukを含む新規container0、開始時に存在した16 containerのID集合の欠落0、稼働0を確認した。DB停止だけを根拠に全資源消去とせず、harnessのprocess・port・一時log検査とこの読取を分けて保持する。手動環境の起動・停止・cleanup・再seedは行っていない。

### 5.3 Evidenceと残件

追加のignored Evidence：`.tmp/consumer-feedback-win-20261112-retry01/`。

- `run.ps1`、`Focused`／`Root`／`Package`／`E2e`のlogとsummary JSON。
- `Focused-reports/`、`Root-reports/`、`E2e-reports/`に各実行のSurefire XMLをコピー済み。後続packageによるtargetのcleanとは別に保全した。
- `E2e-jar-before.json`／`E2e-jar-after.json`、`E2e-containers-before.txt`／`E2e-containers-after.txt`／`E2e-containers-final.txt`、`E2e-cleanup-final.json`。
- harnessが秘密検査後に出力したheap／pool診断行は`build-support/reference-e2e-verification/target/s1-resource-limits/sanitized-process.log`にある。

このWindows環境で、承認された対象・通常root・packaged critical journeyの確認は完了した。結果のOwner受入、O-1の別判断、後続版への収録・公開は別途残る。commit／push／PR／mergeは行っていない。
