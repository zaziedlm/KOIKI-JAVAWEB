# D2-02：Java 25 Virtual Threads実装・検証結果

**状態：** D2-02限定検証 COMPLETE／Owner確認済み・D2-03進行指示（2026-10-26）。§14の構成訂正・Tooling訂正後、3構成の通常／観測計6構成、43論理checkpointが成立。最終結果・制限は§9〜11。過去のFAIL／ERROR・SKIPは履歴として保持する。D2全体・実CI／DoD 4-11・正式提供の受入ではない。

**Owner確認記録：** Ownerは最終結果を確認し「結果確認しました。D2-03へ進めましょう」と指示した。限定結果・制限を次工程へ接続する確認として記録し、実CI成功・全体DoD・正式releaseの承認とは区別する。[D2-03記録](../../development/phase4-d2-03-ci-and-adoption-guide-20261026.md)へ続く。

**追加一回の承認（2026-10-26）：** Ownerは「訂正後の専用検証を、同じ範囲で追加1回実行してよいです」と承認した。§5の6 invocation／43 checkpoint、資源・時間・停止／cleanup条件を維持し、通常rootを再実行せず進める。初回証拠を保持し、追加試行は`selected-execution-corrected-1/`へ分離する。実行前に訂正source・文書14件のhash一致、Reference／observer JAR一致、Java21 classの再compile不要、両JDK・Docker・imageの一致と残存containerなしを確認した。

**日付：** 2026-10-26（JST）。Ownerは[D2計画§8〜12](../../development/phase4-d2-01-java25-virtual-threads-plan-20261026.md#8-一次情報と実際の構成の照合2026-10-26)を確認し、その範囲でD2-02の実装・検証を開始するよう指示した。

## 1. 作成したものと固定対象

作業branchは`docs/d2-01-java25-vt-verification-plan-20261026`。対象Referenceの基点は`1284ba08a9e0cb44c67487dc02148e378699ceb4`。正式アプリ／Framework／既存test／既存POM／migration／CIは変更していない。

`build-support/virtual-threads-verification/`へ、独立reactorのPOM 3件、observer 3 class＋登録resource 1件、harness 3 class、READMEの計11 fileを作成した。正式Root Reactor・publish unitへ追加していない。観測用Spring／Security／Hikari等の依存はprovided、harnessの依存はtest scopeとし、observer JARへlibraryを同梱しない。

observerは既存相関filterの外側とSession APIのhandlerを読取り観測し、Hikari MXBeanを接続借用なしで採取する設計。harnessは独立CookieManagerによる実login・CSRF・既存業務操作、有限DB lock、負荷生成、JFR・checksum・cleanup確認を持つ。これらの実装が全ケースで動作したという結果はまだない。

Git commitは行っていない。基点commitに加えて、承認文書の保存コピーとSHA-256、新規Toolingの保存コピー・ファイル別hashで実行sourceを識別した。clean／commit済みsourceとは記録しない。

| artifact／環境 | 固定値 |
|---|---|
| Reference JAR SHA-256 | `23f712ada819ffcaa07ddc926333302516fd5099cf8891315ac41730347b20cf` |
| observer JAR SHA-256 | `0a5ba43a23dc25fd212a8b63c851bb90ca0c78dfb10a265f3b389979d73c24c0` |
| build／harness JDK | 21.0.12.1、Maven 3.9.16 |
| Java25候補 | Temurin 25.0.5+8。起動version確認済み、Referenceでの実行は未着手 |
| Java25配布物 SHA-256 | `68982529869257d0ee6b824ec5aa270ff8b118bb438670da0b97ebdab178d7fb`（公式release assetのchecksumと一致） |
| Docker | 28.4.0、ローカルsocket疎通確認済み |
| PostgreSQL17 image digest | `sha256:1bcc18d81671b02906296aad470ec9778a6b71016ce953092024b680bad007b0` |
| Ryuk0.14.0 image digest | `sha256:7c1a8a9a47c780ed0f983770a662f80deb115d95cce3e2daa3d12115b8cd28f0` |

## 2. 準備・通常root回帰

Java25を公式GitHub release assetから取得し、配布物のSHA-256を照合した。Java25のJFR metadataにStart／End／Pinned／SubmitFailedイベントが存在することも確認した。これはReference上のVT動作実証ではない。

通常rootは承認済みのbounded DB設定で`clean verify`を一回実行した。2026-10-26 15:53:13 JSTから15:57:04 JSTまで、Maven実時間3分49秒、16 projectsがBUILD SUCCESS。保存XMLの合算は549 invocation＝**449 PASS／100 SKIP、失敗0・エラー0**。直接回帰73件（Session controller40、Session PostgreSQL10、Session security22、Audit rollback1）もこのroot内に含まれる。重複再実行はしていない。

Tooling準備buildの最初は、独立reactorが参照するKOIKI BOMのローカル未登録で停止した。既存BOMのローカルinstallに必要な未取得install plugin 3.1.4をMaven Centralから取得し、BOM POMをローカルMaven repositoryへ登録した。既存POMの変更・remote publishはない。その後のJava21 package／test-compileは検証をskipして成功した。fixture本体はこの時点で動的検証していない。

## 3. 初回専用検証：起動失敗と停止

初回実行は2026-10-26 **16:08:07〜16:08:20 JST**、Maven実時間13.073秒。最初のA-native（Java21／VT無効／通常`java -jar`）でReferenceが起動途中に終了し、readiness assertionがFAILとなった。

原因のlogは次の設定名を示した。秘密のキー値は保存文書へ転記しない。

```text
Invalid KOIKI Identity setting: login-attempt.source-hmac-key
```

Identityの`IdentityAuthenticationProperties.decodedSourceKey()`は`Base64.getDecoder()`で標準Base64を受け付け、32 byte以上を要求する。初回のToolingはURL-safe Base64を返す共通`random()`をHMACにも流用しており、既存設定契約に不一致だった。VTを有効にする前、observerを接続する前のfixture起動不備であり、VTの不具合を示した結果ではない。一方、VTに問題がないことの証拠でもない。

| 集計 | 初回結果 |
|---|---|
| JUnit invocation | 6報告：FAIL 1、ERROR 0、SKIP 5 |
| 実際に開始した構成 | A-native一回のみ |
| 後続構成 | A-observed、B-native／observed、C-native／observedは未実施 |
| 43論理checkpoint | PASS 0。N01未成立、V01〜12／L01〜02は未着手 |
| 負荷・pool lock・JFR recording | 未実施 |

fail-fastにより後続構成を実行しなかった。SKIP 5件を成功へ集計しない。初回のsource、manifest、application log、Surefire XML、失敗時刻を保持する。

## 4. Cleanupと実績

A-nativeのconstructor例外経路でcleanupを実行し、子process停止、アプリDB接続0、container停止、port解放、Reference／observerのhash不変、logへのfixture秘密・メール非露出を確認した。`cleanup.txt`を保存した。終了後のDocker `ps`は空、所有PIDも不在だった。残ったRyukも含め実行中containerはない。

専用検証では一つの隔離PostgreSQLと最初の子JVMだけを起動した。DB lock、業務更新、追加負荷、Java25上のReferenceは実行していない。55分枠を使い切ったことによる停止ではなく、初回起動失敗の停止条件による。

## 5. 訂正差分と再開案

`VtReferenceFixture`のHMAC生成一行を、32 byteを標準Base64でエンコードする方式へ訂正した。

```java
final String hmac = Base64.getEncoder().encodeToString(new SecureRandom().generateSeed(32));
```

Frameworkのvalidation、認証・権限、HMACの強度を変更していない。password等に用いるURL-safe乱数生成も維持する。READMEへHMAC設定契約とBOM準備を補足した。訂正後はJava21のtest-compileだけを行い、アプリ・DB・43 checkpointは再実行していない。

再開案は、訂正sourceのコピー・hashを別epochへ固定し、**専用検証6 invocation／43 checkpointを追加一回**、同じ3構成・資源・停止／cleanup条件で実施する。開始前のsource／環境差分確認は5分以内、専用検証は20分以内、cleanupは5分予約とし、元の55分枠を自動更新・拡大しない。通常rootは対象コードを変更していないため再実行しない。初回証拠を上書きせず、新しい保存先へ出力する。今回の修正はharnessだけなので、Reference／observer JARは上記hashを維持する。

上記は追加一回の承認前に提示した再開案。Owner承認後の実行結果は§7とし、更なる自動再実行は実施しない。D2-02結果の受入、Java25 VT対応認定、D2-03実CI、DoD 4-11、commit／pushは未成立。

## 6. 証拠保存先

クラウドローカルの`/workspace/koiki-cloud/logs/d2-02-20261026/`に保存した。GitHubには未公開で、このクラウド環境の外には自動保存されない。

- `source/source-manifest.json`：基点・承認時文書のhashと保存コピー
- `implementation-source-manifest.json`、`implementation-source/`：初回実装sourceとhash
- `artifact-manifest.json`：Reference／observer JARとchecksum
- `root-build.log`、`root-summary.json`、`root-reports/`：通常root結果
- `jdk25.sha256`、`jfr-event-metadata.txt`：環境準備の証拠
- `tooling-build*.log`、`local-bom-prepare*.log`：準備build・依存不足と解消履歴
- `selected-build.log`、`selected-execution/A-native/`：初回FAIL、ownership／application log／cleanup
- `hmac-correction-compile.log`：訂正後のコンパイル結果

実行コマンドと入力形式は[Tooling README](../../../build-support/virtual-threads-verification/README.md)に記録した。

## 7. 承認済み追加一回の実行結果

2026-10-26 **17:15:00〜17:15:15 JST**、Maven実時間13.954秒。§5の訂正sourceと同じReference／observer JARを用い、通常rootを再実行せず、専用検証だけを追加一回実行した。

A-nativeは起動し、実login 302、CSRF取得204、下書き作成201、詳細参照200（DRAFT:1／金額100）、提出204まで進んだ。HMAC不備は解消している。その後、`submit()` helperが応答の`X-Request-ID`を必須として取得する箇所で`NoSuchElementException`となった。MVC表示・DB／Auditの最終照合には達していないため、N01をPASSにしない。

| 集計 | 追加一回の結果 |
|---|---|
| JUnit invocation | 6報告：PASS 0、FAIL 0、ERROR 1、SKIP 5 |
| 43論理checkpoint | PASS 0。N01途中停止、V01〜12／L01〜02未着手 |
| 実際に開始した構成 | A-nativeのみ。observer／Java25／負荷／pool lockは未実施 |
| Cleanup | 子process・DB接続・container停止、port解放、hash不変、log秘密非露出を確認。終了後Docker psは空、所有PID不在 |

保存JARの`BOOT-INF/lib`とReference／各Starter POMを照合したところ、**通常Referenceは`koiki-starter-observability`を依存として採用していなかった**。Web MVC／API／Session Starterからの推移依存にもない。提供機能ガイドも各Starterを必要に応じて選択する構成としている。相関filterのsourceがRepositoryにあることを、Referenceへの組込み済みという前提にしていた計画・Toolingの誤りである。

通常起動N01は業務・認証・Auditの成立を確認する項目であり、任意Starterの相関ヘッダーを追加要件にしない。helperを、通常起動では送信したrequestIdを識別用に取得し、観測起動では応答ヘッダーとの一致も必須とする形へ訂正した。送信IDを取得しただけでMDC・Audit相関が成立したとは扱わない。V03／V09の相関確認は削除していない。

このhelper訂正だけでは、観測起動の実相関filterが不在という条件は解消しない。観測ケースを実行して同じ既知の不成立を繰り返さず、§8の構成訂正を先に行う。訂正後はtest-compileだけを実施し、アプリ／DBの再起動・専用検証の追加実行は行っていない。

追加試行の証拠は`retry-1-source-manifest.json`、`retry-1-source/`、`retry-1-artifact-manifest.json`、`retry-1-build.log`、`retry-1-reports/`、`selected-execution-corrected-1/A-native/`。helper訂正のcompile結果は`native-correlation-correction-compile.log`。初回試行の証拠とは分離して保持した。

## 8. 次に必要な計画訂正

通常Referenceの業務検証と、実Observability Starterを採用したVT観測構成を明確に分ける。[D2計画§14](../../development/phase4-d2-01-java25-virtual-threads-plan-20261026.md#14-追加一回の結果を受けた構成訂正案2026-10-26)へ、通常JAR維持、観測時のStarter・通常runtime推移依存の外付け、全JARのmanifest固定、管理endpoint非公開、証拠の適用範囲を具体化した。

この構成変更とそれに基づく追加実行は、上記の「同じ範囲で追加一回」の結果に混ぜない。以下の§9以降を、Ownerが§14に沿った見直し・推進を指示した後の新しい構成・source epochの結果とする。通常root449 PASS／100 SKIPは保持し、再実行していない。

## 9. 構成訂正・Tooling診断の経緯

Ownerは「14. 追加一回の結果を受けた構成訂正案に沿って、見直して、進めましょう」と指示した。既存Reference／Framework／既存POM／CIを変更せず、Toolingのharness・READMEと観測recordを見直した。

- consumerのruntimeをMavenで解決し、全33 JAR中19はReferenceの版・payload hashと一致して再利用、14を外付けした。Starter自身のoptional依存をconsumerへ余分に推移導入しない。管理endpointはexposureから除外し、認証付きHTTP403／404も確認した。
- 最初の構成訂正試行はA-native N01、A-observed V01〜03が成功し、V04で応答相関IDによる完了record照合がFAIL（Maven25.872秒）。現在の相関filterはSecurityより後で、先に拒否される要求には応答IDが付かない。observerへ安全性検査済み受信markerを読取り追加し、拒否の識別だけに用いた。実MDC／Auditの相関成功へ転用していない。
- 続行はA-observed V01〜09まで成功し、V10のactive=0待ちがFAIL（24.052秒）。lock解除後の8要求は200、pending=0だったがactive=1が残った。基準を緩めず、限定起動診断を行った。
- 診断1は21.326秒。起動時Hikariが既定値10／10／30000で、後から4／1／3000へ変化し、leak検知も0のままだった。診断2は21.708秒。Hikari生成前に非秘密の設定fileで上限4／1／3000とleak検知2000 msを適用したところ、借用元stackが`HikariDataSource.getConnection`→`JavaBeanBinder$BeanProperty.getValue`→`ConfigurationPropertiesBindingPostProcessor`を示した。DB上のapp接続はidle、transaction age 0。いずれもseed・login・業務操作・故障注入・負荷なし、cleanup成立。診断のJUnit成功は43項目へ数えない。
- Toolingの`SPRING_DATASOURCE_HIKARI_CONNECTION_TIMEOUT`等がネストされたconnectionのbindingを誘発していたため、正規の`--spring.datasource.hikari.*`引数へ統一した。継承Hikari環境変数を拒否し、起動ログでも最初から4／1／3000を照合する。正規入力試行でactive=1は消失し、A-native N01とA-observed V01〜10が成功した。
- 正規入力試行はV11で500以外を全て200と仮定するhelperが403でFAIL（29.707秒）。Session読取り例外から既存ERROR dispatchの拒否境界に達する経路と、MVC内で500に変換される経路を分けた。最低一件の5xx・pool timeout因果・全要求有限終了・非露出・解放を維持し、pre-MVC403を別集計とした（計画§14.7）。Security／HTTP応答は変更していない。

経緯は[計画§14.3〜14.7](../../development/phase4-d2-01-java25-virtual-threads-plan-20261026.md#143-具体化したclasspath実装差分)へも記録した。起動wrapperのtimeout表記を訂正した一回はMaven・アプリ未開始（exit125）で、`resume-1-launch-error-*`を残した。

### 9.1 最終artifact・sourceと実行の識別

Referenceは初回と同じSHA-256 `23f712ada819ffcaa07ddc926333302516fd5099cf8891315ac41730347b20cf`。受信markerを追加したobserverの最終SHA-256は`1de3a597c8cebe2eb38b5c1a41cb3dd3f17935199bdfece17dfb332754ba98f7`。実Observability Starterは`e14cf9bb04c2054537fa4e3f98c93c0fe362e69b7a4b65b2d149cf2f144b72c8`。最終Reference／observerと外付け14 JARは`observability-revision/resume-1-artifacts/`へコピーして固定し、全hashを試行後にも再照合した。

最終採用結果は、正規入力試行のA-native N01成功一件と、最終続行のA-observed〜C-observed五件から構成する。A-nativeはアプリ・引数・N01判定が不変で再利用できる。V11分類だけを変更した後の最終続行は2026-10-26 **18:10:07〜18:11:50 JST**、Maven約1分42秒、JUnit **5 PASS／失敗0・エラー0・SKIP0**。前の部分成功を重複計上せず、43項目が成立した。単一runで6 invocation全てがPASSしたとは記録しない。

構成訂正後の試行・限定診断のMaven実時間は約3分45秒の合算で、比較20分枠内。コード作成・文書整理・依存準備の時間とは区別する。初回通常rootと過去二回の停止も保存し、失敗を削除・上書きしていない。

## 10. 最終の成立範囲・計測結果

| 構成 | 通常起動 | 観測起動 | handler実測 | 結果 |
|---|---|---|---|---|
| A：Java21／VT無効 | N01 | V01〜12 | 48件すべてplatform thread | 13 checkpoint PASS |
| B：Java25／VT無効 | N01 | V01〜12・L01〜02 | 5048件すべてplatform thread | 15 checkpoint PASS |
| C：Java25／VT有効 | N01 | V01〜12・L01〜02 | 5049件すべてvirtual thread | 15 checkpoint PASS |

同じJava21生成Reference JARで、Session認証・CSRF拒否・本人scope・別user分離・競合拒否・submit／DB／Audit・MVC表示・logout／旧Cookie拒否が成立した。観測構成では実filterの応答ID／MDC／Audit相関、要求後のcontext復元を確認した。poolは全構成でactive／total最大4、pending最大4、ロック・要求終了後10秒以内のactive=0／pending=0を実測した。V10は各8要求全て200、V11は有限の失敗と回復、業務状態不変が成立した。

| pool timeout試験 | HTTP5xx | pre-MVC403 | その他 |
|---|---|---|---|
| A | 3 | 1 | 残る4件は200 |
| B | 3 | 1 | 残る4件は200 |
| C | 4 | 0 | 残る4件は200 |

403を5xxとして数えていない。経路とタイミングの差を示す一回の値であり、VTにより403が解消すると一般化しない。受信marker・handler未到達・秘密非露出を確認して別分類した。

### 10.1 Java25の有限負荷比較

各窓は最大2000要求に先に達して終了した。warmupも最大500要求で早期終了し、統計から除外した。以下は各条件一回の診断値であり、60秒耐久試験・統計的優位・本番SLO・最大throughputの保証ではない。

| Java25 | 並行要求上限 | 完了数 | 計測秒 | requests/s | p95 ms |
|---|---|---|---|---|---|
| VT無効 | 1 | 2000 | 5.881 | 340.1 | 5.45 |
| VT無効 | 8 | 2000 | 2.274 | 879.4 | 19.23 |
| VT有効 | 1 | 2000 | 6.222 | 321.4 | 5.01 |
| VT有効 | 8 | 2000 | 2.253 | 887.7 | 18.07 |

全要求が期待した詳細DTO／状態で成功し、負荷後もpool解放・DB／Audit不変を確認した。外付けobserver・JFRを含む同じ構成内の比較であり、通常Reference単体の性能へ転用しない。高速化を採用理由として結論しない。

Java25・VT無効のJFRではStart／End／Pinned／SubmitFailedは各0、VT有効ではStart5018／End5013、Pinned0／SubmitFailed0。handlerの`isVirtual()`と併せて有効化の実効を確認した。Pinnedは20 ms閾値・今回の窓で未観測という意味で、pinning不存在ではない。start／end差は記録境界を含む値で、資源解放は実process停止・DB接続0・port解放で確認した。

### 10.2 Cleanup・source保護

採用した全6構成のcleanup記録があり、子process・アプリDB接続・container停止、port解放、log秘密非露出と全JARのhash不変を確認した。全試行の所有PIDは不在、最終Docker psは空。診断で残った常駐processもない。正式Framework／Reference／既存POM／CIは未変更、commit／pushは未実施。

## 11. 利用条件・後続課題・次タスク

今回の結論は、**Java21 buildの現行ReferenceをJava25.0.5でVT opt-in実行し、選択した同期Web操作が成立する限定証拠を得た**こと。VT既定無効を維持し、案件が採用する際の比較材料にする。既定有効化、全案件への推奨、Java25より後の全版の認定、正式提供版／supportの認定ではない。

| ID | 残す条件・後続先 |
|---|---|
| D2-F01 | D2-03でJava25 VT有効の実CI系統、実行枠・費用・required check扱い、利用ガイドと正本§21.5／§23.3の整合を具体化する。DoD4-11は実CI結果待ち |
| D2-F02 | D1同期観測でSecurity前段の拒否／ERROR dispatchの相関範囲を確認する。現構成は早期401／403に実相関IDがない場合があり、全拒否・全dispatchの追跡を保証しない |
| D2-F03 | DB逼迫時、MVC内500とSession読取り例外からの403が混在する条件を運用診断・案件の障害応答要件へ接続する。全経路5xx統一は今回保証しない |
| D2-F04 | R-09／E1の利用手順でHikariの正規property入力を案内する。他の既存Toolingに同様の環境変数がある場合の点検は後続課題とし、過去S1／C2全件を自動再開しない |
| D2-F05 | 案件ごとの受付制限、DB pool・外部I/O・native library、CPU中心処理、長時間負荷、実配備SLOは採用条件として個別評価する。負荷生成側の8要求上限を本番制御の成立にしない |
| D2-F06 | S1の独自executor・非同期context伝播、Batch／Container、他Java版は今回の同期Web結果へ含めない。TaskDecoratorの存在だけで別executorへの伝播を認定しない |

次は本限定結果・制限のOwner確認を入力にD2-03へ進む。実CI・正本改訂は未実施で、D2全体の完了と区別する。

最終証拠の入口は`observability-revision/canonical-completion/summary.json`。A-nativeは`canonical-input/selected-execution/A-native/`、残る五構成は`canonical-completion/selected-execution/`。source／artifact manifest、raw latency、metrics、JFR、reports、cleanupを対応付けて保存した。診断と停止履歴は同じ親directoryの別名で残す。
