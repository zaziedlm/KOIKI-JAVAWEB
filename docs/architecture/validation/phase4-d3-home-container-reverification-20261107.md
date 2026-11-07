# D3方式1：自宅Windows端末での改善版Container再検証（2026-11-07）

## 1. 判定・実行範囲

**この端末での限定技術成立。結果Owner受入は未記録。** Ownerの「最新コミット内容を確認し、この端末でもbuild・実行検証を進める」指示を受け、改善成果commit `5e01aaccc27970f98ed5dc8299161f8981584de7`からReference JARを再生成し、直接回帰25件・image build・既存7 checkpointが成功した。

[別環境引継ぎ](../../development/phase4-d3-cross-environment-verification-handoff-20261106.md)に従い、新しいwork／project・port・secret・DBを用意した。所有はReference／Tooling。実装source・Dockerfile・Compose・Framework・Root Reactor／CIを変更せず、過去のraw EvidenceやDBを今回の検証入力にしていない。端末内の指定版toolchain・依存cacheは再利用した。

開始時にReference再生成／直接回帰25件、Compose image build、既存runnerの7 checkpointを各1回と定めた。初回JAR buildはsandbox制約でSPA生成前に停止し、環境の承認手順を経て同じbuildを権限付きで1回再実行した。image buildと7 checkpointは初回1回で成功。layer比較5 invocation、通常root全件、secret欠落負例、手動環境起動は今回の実行集合に含めていない。

## 2. 最新commitと実行環境

開始時のbranchは`docs/d3-01-container-contract-20261105`。HEADと手元のupstream参照はともに`5e01aac`で、checkoutはclean。今回、新たなfetch／pullは行っていない。

最新commitはBoot標準toolsによる2 stage・4区分COPY、元JAR SHA-256のimage label識別、起動中healthcheck間隔、Tomcat thread上限・OOM終了・ログ上限の設定例と検証資材の更新を含む。今回、その更新後資材で実行した。

| 項目 | この端末の確認値 |
| --- | --- |
| host | Windows 11／amd64 |
| Java／javac／Maven使用JDK | Temurin 21.0.13+9、同じ拡張管理JDK21 |
| Maven | Repository Wrapper 3.9.16 |
| Node／npm | `.tmp/d3-tools/pinned`の24.19.0／11.9.0。frontendの指定に一致 |
| Python | 3.13.3、pyenv配下の実行実体を確認して使用 |
| Docker | Engine 29.5.3、Linux amd64、cgroup v2、6 CPU／約15.6 GiB |
| Compose | 5.1.4。引継ぎ元の5.3.1とは異なるが、今回の構文・実効設定確認は成立 |
| browser | 既存E2E ToolingのPlaywright 1.62.0、対応Chromium cache v1234でjourney成立 |

Docker named pipeはsandbox内からアクセス拒否。権限付き診断でServer応答を確認した。Engine停止ではない。sandbox内の初回buildはNodeの`spawnSync cmd.exe EPERM`でnpm版確認に失敗したが、同じhelperを権限付きで実行するとSPA生成・package・回帰が成功した。初回失敗ログは上書きせず保持している。証明書検証の無効化・依存版変更は行っていない。

## 3. Artifact・実行識別

| 項目 | 値 |
| --- | --- |
| profile | `b1-session-spa,d3-container` |
| 元JAR | `koiki-reference-app/target/koiki-reference-app-0.1.0-SNAPSHOT.jar`、66,896,576 bytes |
| JAR SHA-256 | `efcc91f3269a6bc2ec943a1a0f98c4d8a4cb6f9390c781e6847b991e67c18e21` |
| work／project | `.tmp/d3-home-verify-20261107/`／`d3-home-verify-20261107` |
| port | loopback 18087 |
| image | `d3-home-verify-20261107:local` |
| 起動image ID | `sha256:0c939304170c7e3db829c11fd990353d92f7b18db2fcb4d7097815ac718628cf` |

prepareでSPA asset hash一致・async JDBC非同梱を確認した。起動containerのimage label `org.koikifw.d3.source-jar-sha256`は今回の元JAR hashと一致。抽出後全ファイルのbyte一致証明ではない。引継ぎ元JARとのhash一致は要求せず、同一sourceの再現可能build成立とも判定しない。

## 4. 検証結果

| 集合／checkpoint | 結果 |
| --- | --- |
| 直接回帰 | 3 class／25件、failure0・error0・skip0。Container Security2件、通常Security1件、Session API22件 |
| 静的・build | Python構文・Compose `config --quiet`成功、改善Dockerfileのimage build成功 |
| DC02 | 起動・Framework／Reference migration・readiness成立 |
| DC01 | UID10001、read-only、Java PID1、memory1536 MiB、PID上限160、image label照合、抽出JARのSPA配置・Tomcat設定40を確認 |
| DC04 | liveness／readiness公開、他Actuator／匿名API拒否、非superuser DB role成立 |
| DC03 | 実Chromiumのlogin→SPA作成→CSRF欠落拒否→提出→reload→logout成功 |
| DC07 | SIGTERMによるapp再起動後も申請・Audit・migration保持 |
| DC06 | DB停止時readiness503／liveness200、DB回復後readiness成功、提出Audit1件のまま |
| DC08 | 今回所有container／volume／network cleanupと残留なし |

browserでは1,200円の申請がSUBMITTEDとなり、DB／Audit照合が成立した。保存された`spa.png`も提出済み・1,200円の画面として確認した。Owner手動打鍵の結果とは数えない。

実効設定は、healthcheck起動中2秒、heap768 MiB＋`ExitOnOutOfMemoryError`、app／DBのjson-file上限10m×3をrunnerがinspectで照合。Java thread31・cgroup PID32で上限160以内だった。Tomcat40は抽出JAR内propertyの確認であり、最大負荷・実際の上限到達試験ではない。

参考時間はimage build6.404秒、Compose初回`up --wait`19.037秒、Startedログは初回11.290秒／再起動11.388秒。cache・端末条件が異なるため、引継ぎ元との速度優劣やlayer cache再利用を今回新たに実証したとはしない。

## 5. ログ・保持資源・証拠

appログはINFO83件・WARN7件・ERROR0件。WARNは起動／再起動の認証provider・cache統計警告各2件、意図的DB停止に伴うpool接続検査2件・DataSource health1件。既知Session重複キー事象は取得ログに現れていないが、対応を実装していないため解消とは判定しない。

DBログには初期化時のlocale／local trust警告と、DB停止時の`terminating connection due to administrator command`のFATAL2件がある。後者はDC06の意図的停止時刻に対応し、回復checkpointは成功した。DBのERROR／PANICは検出なし。secretがruntime logへ現れていないことも既存runnerで確認した。

検証前後の既存container名集合とD3 volume名集合は一致。既存の`d3-manual-20261105_database`（作成日時2026-11-05 18:33:57 JST）を保持し、旧手動projectの再起動・seed再投入・cleanupはしていない。この端末には引継ぎ元の`d3-improved-manual-20261106`は存在せず、別端末資源を操作していない。

今回のimage、ignored Evidence、専用workのsecretは調査用に保持。自動検証app／DBはcleanup済みで、port18087に手動確認環境は稼働保持していない。

証拠の保存先：

- `.tmp/d3-home-evidence-20261107/`：source／hash・toolchain・Docker環境、初回`build.log`と`build-elevated.log`、直接回帰集計、classpath／image build／verifyログ、image識別、ログ集計、資源の前後記録。
- `.tmp/d3-home-verify-20261107/`：`artifact.json`、`results.json`、`browser.txt`、`spa.png`、`runtime-app.jar`、`runtime-sanitized.log`、専用context・secret。

## 6. 未実施・次の判断

layer比較・cache再build、DC05 secret欠落負例、通常root全件・async suite、手動打鍵・承認／差戻し、OOM注入・負荷・ログ満杯試験、A4再現可能build、B5 CDS、B6 restart policy、Session対応、HTTPS／Secure Cookie、処理中drain、別案件Consumer、ECS、方式2SSO、registry公開／deployは今回未実施。

今回の限定結果確認とOwner受入判断を次の入口とする。commit・remoteは未実施。既存S1未達・人手HOLD、C2 H07、D1結果受入未記録、V10、正式support・全DoD・D3／Phase4全体の判定は変更しない。

## 7. 後続Owner指示による手動確認環境の準備

Ownerがこの端末でも手動ブラウザ操作を行うための環境整備を指示した。§1〜6の自動検証後、同じ元JARをhash再照合し、新しい専用project `d3-home-manual-20261107`／work `.tmp/d3-home-manual-20261107/`／port18081をprepareし、`start-manual.py`を1回実行した。既存project・portとの衝突なしを事前確認した。

image build・app／DB起動・専用fixture seed・元JAR hash label照合・申請者／承認者のHTTP loginと選択route確認は成功。image IDは`sha256:57b0073500147a1fd5a794a6c4510817a4ca9c834906db0e8df67e2ba79773e4`。元JAR hashは§3と同じ。自動検証imageとは別buildである。

- login：`http://127.0.0.1:18081/login`、SPA：`http://127.0.0.1:18081/spa/`
- 申請者：`d3-applicant@example.test`
- 承認者：`d3-approver@example.test`。承認入口はホームの「承認対象の経費申請」または`/expenses/approvals`。
- 両者共通の生成password：`.tmp/d3-home-manual-20261107/secrets/login-password`を参照。値を共有文書・Gitへ記載しない。

起動記録は`manual-ready.json`、`manual-status.jsonl`、`manual-build.txt`、`source.json`に保存。起動時Composeとinit scriptも同workに控えた。app／DB／volume／secretは手動確認用に保持し、指示なしに停止・cleanup・seed再投入しない。旧`d3-manual-20261105_database`や他projectを操作していない。

手動操作の案内順は、申請者のSPA作成→提出、承認者の差戻し、申請者の再編集→再提出、承認者の承認、必要に応じ申請者のMVC作成・保存とlogout／再login。利用者切替はlogoutまたは別browser profileを使い、originを`127.0.0.1`へ統一する。

この準備成功をOwner打鍵の完了や受入とは数えない。手動確認後の報告と必要なDB／Audit・ログ点検は未実施。§1〜6の手動起動未実施・port18087環境cleanupは、この後続指示前の範囲を表す。

## 8. Owner手動操作報告後のログ・DB確認

Ownerは全行程を実施し、承認まで問題なかったと報告した。依頼により2026-11-07 10:08:53 JSTから、`d3-home-manual-20261107`だけを対象に稼働状態・ログを取得し、`REPEATABLE READ READ ONLY` transactionで申請・明細・Audit・migrationを照合した。app／DBの停止・再起動・DB更新は行っていない。

| 点検 | 結果 |
| --- | --- |
| 稼働 | app／DBともrunning・healthy、restart count0。app image IDは§7の起動記録と一致 |
| 申請 | `95a41b65-0883-40b8-a5e7-7faabf62de6e`の1件、5,600円、APPROVED、version7 |
| 明細 | 1件、合計5,600円。申請額不一致0件、孤立明細0件 |
| Business Audit | 同一申請のSUBMIT→RETURN→BEGIN_REEDIT→SUBMIT→APPROVEがSUCCESS5件。申請者／承認者のactorも各操作に対応 |
| 承認時刻 | 10:06:51 JST。先行操作は09:57:15提出、09:58:25差戻し、10:00:06再編集、10:03:25再提出 |
| 承認整合 | APPROVEDで成功APPROVE Auditを欠く申請0件 |
| Security Audit | LOGIN SUCCESS6件、FAILURE／BAD_CREDENTIALS1件。成功件数には起動helperの両利用者HTTP確認を含み、全件をOwner操作回数とは数えない。失敗の入力内容・原因はこの点検では確定しない |
| migration | Framework3件／Reference4件、すべてsuccess |
| appログ | INFO45件、WARN2件、ERRORなし。WARNは起動時の認証provider／cache統計警告 |
| DBログ | 初期化時のlocale／local trust警告。ERROR／FATAL／PANICなし |
| 既知Session事象／secret | Session属性重複キー例外は取得ログに現れず、生成secret値のログ露出なし。Session事象の解消とは認定しない |

Ownerの手動操作報告と、DB／Audit・ログによる限定照合が成立した。DBだけではSPA／MVCの操作経路や全画面操作までは識別しない。D3全体・正式support・Phase4全体や、結果全体のOwner受入へ拡張しない。

証拠は`.tmp/d3-home-manual-20261107/inspection-20261107-100853/`の`db-readonly.json`、`summary.json`、`app-sanitized.log`、`db-sanitized.log`。読取補助scriptは同workの`inspect-after-manual.py`。secret・認証Cookie・Session属性の内容は取得・記載していない。

点検後も手動環境・DB volume・secretを保持している。§7の打鍵結果・DB／Audit点検未実施表示はこの後続点検前の履歴とする。停止・cleanup、成果commit／remoteは未実施。
