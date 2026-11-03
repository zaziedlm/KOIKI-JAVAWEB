# E1：ローカルWindowsでの再生成・再現確認（2026-10-30）

**状態：Temurin環境復旧、WindowsのV01〜08相当は訂正後に限定技術成立。結果Owner受入・Release公開／実取得V09・実案件受領V10は未完了。**

## 1. 今回の根拠と範囲

OwnerはCloudの別渡しZIPに依存せず、ローカルPCで設計・実装を再検証するよう指示した。
基点は `docs/phase4-s1-b1-c2-guarantee-review-20261030` / `78a8887efb1c02888c69976bee8171ab99e94c2f`。
CloudのV01〜08を上書きせず、新規候補のsource inventory・staging変換・ZIP hash・実行結果を別に記録する。

今回はWindows互換のTooling修正、V01〜08相当の候補生成・空の専用Maven repositoryへの登録・拒否6確認・独立Consumer・通常root一回を対象にする。
Framework本体・checkout POM・Root CIは変更しない。commit／push、tag／Release作成・uploadはこの再現確認に含めず、結果と公開候補を点検した後に判断する。
V09の実downloadは公開後、V10のIDE／実案件CIは別確認。今回のPC上のCLI成功だけで実案件受領完了にしない。

新規の実行枠は100分以内、最後10分をcleanup用に確保。候補生成・Consumer・rootは逐次実行。
同時子JVM4、PostgreSQL1、Ryuk2、DB client14を上限とし、既存normal-root制限・single fork・非並列を使用する。
所有DB・専用role・loopback portのみ使用し、既存container／利用者のMaven cacheを削除しない。
失敗・hash不整合・想定外のFramework差分・資源超過・cleanup不成立では後段を停止し、証拠と必要訂正を残す。

## 2. PreflightとWindows対応

- 作業開始時checkoutはclean、local HEADとGitHub branch先端は一致。mainは `fdfcfa5`。
- PowerShell 7、Python 3.13.3、Maven Wrapper 3.9.16が導入済み。
- 当初のVSCode提供JDKにはmodulesとVMの版不整合警告があり、Correttoを一時代替として選択したが、OwnerのTemurin継続方針により採用しない。Correttoで候補buildは実行していない。
- OwnerがPleiades拡張をアンインストール／再インストールした後、Java／javacはTemurin 21.0.13、Runtime／VMは21.0.13+9で一致し、警告なし。Maven 3.9.16も同じ拡張管理のTemurinを参照する。環境修復後から新規100分の有限検証枠で再開する。
- sandboxのDocker named pipe失敗後、権限付きread-only診断ではDocker 29.5.3 / Rancher Desktopの接続が成立。GitHub CLIの認証主体は `zaziedlm`。
- 候補生成／ConsumerはOS別に `mvnw.cmd` / `mvnw` を選択。Docker CLIはUnix socket固定をやめ、端末の既存context／DOCKER_HOSTを使う。staging POMはUTF-8を明示する。

## 3. 結果

初回candidate-1はsource snapshot中にWinError 3で停止し、Mavenは未開始。
コピー先268文字のfixture pathがWindows CopyFile2の通常path上限を超えていた。
sourceの省略はせず、snapshotコピーにWindows extended pathを指定する局所訂正を行う。
失敗directoryを保持し、別candidate-2で確認する。candidate-2の前回実行要求は中断され、directory／build logは作成されていない。Framework内容・検証集合は増やさない。

Temurin修復後のcandidate-2生成は成功。14座標／26 payload、ZIP SHA-256は `650744482d2c9f52c3ea54c7ceb30a1c387e1c69c16d6e91abc2d23b5b9d3735`。
最初の登録はBOMで停止。Windows Wrapperが一時作業directoryを基点に `.mvn` を探し、設定／Wrapper JARを見つけられない。
checkout外の `mvnw.cmd -version` でも設定欠落を再現。準備とConsumerのWindows Wrapper呼出し時だけ `MAVEN_BASEDIR` をWrapper配置先へ明示し、呼出し元の環境は復元する。
Framework source・POMは変更しない。旧ZIPは旧準備scriptを含むため公開候補にはせず、訂正後のcandidate-3を生成してZIP／内部scriptを一致させる。

### 3.1 Windows補助の訂正と後続確認

candidate-3で選定moduleの111件がPASS（failure/error/skip 0）。空の専用recipientへ14座標を登録できた。
拒否検証の初回はPowerShell出力のUTF-8復号ERRORと一時directoryの権限／cleanup ERRORで停止。
権限付きで当該所有directoryをcleanupし、同範囲を再実行したが復号ERRORは残った。
子PowerShellのOutputEncodingとPython decoderをUTF-8へそろえ、第三回の6確認がPASS。
旧ERRORを保全し、一回で無訂正PASSしたとは扱わない。補助の変更は配布payloadに含まれない。

Consumer初回はDB setup用psqlがexit 2で停止、JUnitは未開始、所有containerのcleanupはPASS。
当初の補助はstderrを保持していなかったため、直接の原因は確定していない。
コード点検でUnix socketのpg_isreadyがイメージの一時初期化serverを準備完了と扱い得ることを確認し、最終TCP serverを待つ局所訂正と秘密値を除いた有限診断を追加。
二回目は同じ候補のJUnit1 invocation／8 checkpoint PASS、dependency treeのKOIKI座標は固定版のみ、cleanup PASS。
Consumerの第三者依存取得を含むbuildは40.898秒、treeは7.073秒。DB接続数は25 sample中最大3（監視を含む）。

### 3.2 採用したローカル候補の識別

| 項目 | 値 |
|---|---|
| 元source | `78a8887efb1c02888c69976bee8171ab99e94c2f` ＋ source inventoryの未commit差分 |
| 候補 | candidate-3 / `0.1.0-preview.1` / 14 POM＋12 JAR |
| ZIP | `koiki-framework-0.1.0-preview.1.zip` / 205,619 bytes |
| SHA-256 | `e3cfe45514cd0af67177416467c052bc61b815bda375c57a4a12cc7798992928` |
| source inventory SHA-256 | `06c6f8d8b70de307f3addc8be0400aa844b540aa51eaaad1eb71783cdd246d49` |
| staging変換記録 SHA-256 | `0818938495832ecda4e591552cd7bed467f4ae6af2a131308728c0f398cff214` |
| 実測build | Temurin21.0.13+9 / Maven3.9.16 / 43.618秒 |
| raw記録 | `C:\Users\kataoka\AppData\Local\Temp\koiki-e1-local-20261030-2300` |

候補ZIP・snapshot・recipient・有限logはGit外に保持する。Cloudの候補bytes／hashとは別物。
登録した26 payloadはmanifestと一致、installer lock残留なし。Framework・Root POM・CI等の非Tooling／非文書sourceは生成前inventoryから変更なし。
candidate生成後の検証補助訂正は配布Frameworkの内容へ混ぜず、本記録で別に識別する。

### 3.3 通常root

single fork・非並列・heap768MiB・normal-root DB制限を指定し、Temurinで一回実行。

```powershell
./mvnw.cmd -B -ntp '-DargLine=-Xmx768m' '-DforkCount=1' '-DreuseForks=false' `
  '-Djunit.jupiter.execution.parallel.enabled=false' `
  '-Dkoiki.reference.normal-root.db-limits.enabled=true' clean verify
```

Maven processには `MAVEN_OPTS=-Xmx768m` を与えた。
2026-10-30 23:34:22〜23:39:30 JST、BUILD SUCCESS／exit 0、Maven表示5分7秒。
JUnit XML74 reportの総数549のうち449実行PASS＋100 skip、failure/error 0。
skipを実行成功へ加算しない。root自動反復はしていない。

2秒間隔を目安とした144 sampleで、検証JVM最大3／PostgreSQL最大1／Ryuk最大2／DB client最大11（監視を含む）、sample error 0。
既存VSCode Language Serverの同梱JRE25 process二つは検証JVMに含めず、停止もしていない。
これらはsampleの観測値で、全瞬間の連続証明ではない。
終了後のcontainer残留0、検証JVM残留0、installer一時directory／lock残留なしを確認。
source／ZIP／登録26 payloadの最終照合は成功、Framework本体・checkout POM・Root CI・wrapperの変更なし。

## 4. 今回の結果と残件

| 対象 | ローカル結果 |
|---|---|
| V01 | Temurin21.0.13+9／Maven3.9.16／Python3.13.3／PowerShell7、Docker・既存DB image・所有directory確認 |
| V02 | candidate-3の111件PASS、14座標／14 POM＋12 JAR・版／hash検査成功 |
| V03 | 空から開始した専用recipientに実POM付きで全座標登録。初回Wrapper失敗後の訂正あり |
| V04 | 拒否・再利用・衝突の6確認PASS。補助の初回／第二回ERRORと訂正履歴を保持 |
| V05〜07 | Consumer 1 invocation／8 checkpoint PASS、固定版の依存tree・条件付きR-05／限定R-06成立。初回DB setup失敗を保持 |
| V08 | 通常root449実行PASS＋100 skip、cleanup・source／ZIP／登録照合成功 |
| V09 | 未実施。tag／Release作成・asset upload・実downloadはしていない |
| V10 | このPCのCLI再現確認のみ成立。実IDE／実案件CI／実チーム受領は未確認 |

R-05はCustomer側の明示EntityScan、R-06はData Starter明示・同一DataSource／public schema・分離historyというCloud時点と同じ限定条件。
ConsumerのHTTP確認はtest application contextであり、packaged JARの別process受入ではない。
今回の結果だけで任意schema／複数DataSource・本番運用・全DoDへ拡張しない。既存S1未達・人手HOLD、C2 H07、D1結果受入未記録を維持する。

最終の補助source hashはrawの `tooling-final-sha256.json`、集計は `summary.json`、root時刻は `root-result.json` に保存。
Cloudの原本ZIPや別渡しpackageは今回の再現条件にしていない。公開には今回の候補bytesを使うか改めて判断し、公開対象commit・未commit差分との対応を先に固定する。
今回のTooling4 fileと案内・結果は未commit。commit／push・PR／merge・tag／Release・Owner結果受入は行っていない。

## 5. 後続の文書整合訂正

Ownerの点検・訂正指示により、引継ぎ・配布README・関連入口をlocal candidate-3とローカルPCでの公開手順へそろえた。
公開は認証・書込権限を確認したPCからGit／GitHub CLIで行い、Codex Cloud等では行わない。生成・取得／Maven登録scriptと公開手順を区別し、廃止済みの旧publish scriptを現行ツールとして案内しない。
Cloud候補／旧別渡しpackage・Windows未確認等の記述は履歴へ整理した。今回の文書訂正でcandidate-3のZIPを改変・再生成していない。同梱READMEの旧例示とcheckoutの訂正版は別bytesであり、実際の取得先は既存`zaziedlm/KOIKI-JAVAWEB`を明示する。
この訂正は文書のみ。動的検証の追加実行・成果commit／remote公開は行わない。
