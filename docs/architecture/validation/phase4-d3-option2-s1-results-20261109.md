# D3方式2 S1：Cloud実装・初回停止と再実行結果（2026-11-09）

## 1. 判定

**最新結果はS1限定技術PASS。** Owner承認後の新規project `d3-sso-s1-20261109-02`で、S1-V01〜05がすべて成立し、自動cleanup・生成secret削除・証拠exportも成功した（§8）。Framework本体の変更はない。Ownerの「S1実装へ進めます。」に基づき、[段階別計画](../../development/phase4-d3-option2-staged-task-plan-20261109.md)のS1-T01〜05をTooling配下へ実装した。S2〜S5は未実装・未実行。

初回runtimeはCompose設定不備でbackend作成前に停止し、HOLDとなった。初回失敗を成功に上書きせず、§2〜7とproject `01`のEvidenceを保持する。計画§8に従い訂正・再実行案を提示した後、Ownerが修正後の同じS1集合の再実行を指示し、Framework本体改変を伴わないS1範囲の修正後再実行を許可した。以下の再実行未実施・未承認の表示は、この後続指示前の履歴である。結果全体のOwner受入・S2開始・commit／remoteは別判断。

## 2. Source・環境・範囲

| 項目 | 今回の識別 |
| --- | --- |
| branch／開始source | `docs/d3-option2-implementation-conditions-20261109`／`68ab796ddda77635864572dd4911473b85076218`、開始時clean |
| 実装所有 | Tooling：`build-support/container-sso-verification/`新規22 file。Reference main／POM、Framework、既存migration、Root Reactor／CI、方式1資材の変更なし |
| 記録 | 本結果文書・AGENTSの現行入口を更新。commit／remote操作は未実施 |
| host | Debian13 Linux amd64、kernel6.18.57。Docker表示5 CPUに対しcgroup実効4 CPU、memory上限16 GiB。開始前disk空き約30 GiB |
| Docker | Engine28.4.0、Compose2.40.3、Buildx0.31.0／BuildKit0.24.0、vfs、cgroup v2 |
| build | Temurin JDK／javac21.0.12.1+1、Maven Wrapper3.9.16、Node24.19.0／npm11.9.0、Python3.12.14 |
| project／work | `d3-sso-s1-20261109-01`／`.tmp/d3-sso-s1-20261109-01/` |
| build・準備Evidence | `.tmp/d3-option2-s1-build-20261109/`、Cloud環境設定は`.tmp/d3-option2-prestart-20261109-01/` |
| 元JAR | SHA-256 `12cec4df84c048546f6de035fb788eda3b6c387ec64439616df75f2b8b230409`、SPA／async JDBC非同梱を検査 |
| backend image | `sha256:60d15067ba8729304cfdfa8efc5064e59e8c5d209d11db9d18761145341ee643`、元JAR label照合は起動前preflightで成功 |

S1のruntime設定はbackend1.5 GiB／1 CPU、PG1 GiB／1 CPU、PG接続32、backend pool最大4／最小1、host公開port0。buildとruntimeは並走していない。後段の最大4.5 CPU案はこの環境の4 CPU上限を超えるため、開始判断で資源条件を見直す。

計画のbuild／取得枠60分・動的40分（cleanup10分予約）に対し、Java buildは47.072秒、初回runtime runnerは12.886秒で停止した。build・取得・実装・確認を合わせた経過は約15分以内で、旧方式1の予算は流用していない。

## 3. 実装内容

| task | 実装と現在の確認範囲 |
| --- | --- |
| S1-T01 | `versions.json`、専用package／lock。4 imageのdigest／amd64 metadataとpackage engines／peer条件を取得。Browserも固定・取得 |
| S1-T02 | 新規work・source／recipe／JAR／lock hash、所有名台帳、secret生成／redaction、preflight、cleanup。既存project・他資源・shellへ渡せないDB selectorを拒否 |
| S1-T03 | 3 DB／専用owner兼runtime role、PUBLIC権限取消し、BFF3 table。transaction＋advisory migration lock、checksum不一致拒否、再適用時刻保持 |
| S1-T04 | `build-reference.py`と薄いLinux／Windows入口、SPAなしJAR、layer抽出Dockerfile、DB＋backend Compose、専用4利用者・権限・master seed |
| S1-T05 | S1-V01〜05 runner、成功／失敗時のredacted log・結果schema、所有確認付きcleanup、allowlist方式の証拠export |

Windows入口はsource実装のみでWindows動作は未検証。BFFはDDL／依存lockのみで、画面・OIDC・暗号化処理・Session制御・refreshは未実装。Keycloakサービスも起動していない。

BFF DDLは`bff-migration.sql`、historyの作成・適用は`migrate-bff.py`に配置した。3 tableは`bff_schema_history`／`bff_auth_transaction`／`bff_session`。client複合PK、state／browser binding／Session ID／CSRFのhash、暗号化payload／tokenとAES-256-GCM metadata、期限・消費・versionと期限indexを定義する。平文token列、IdP sid列、業務複製・DB間FKは作らない。DDLの実DB適用は今回未到達。

## 4. 固定版とsecurity情報

| 対象 | 採用／確認 |
| --- | --- |
| package | Next16.4.5、React／React DOM19.3.1、openid-client6.8.9、pg8.23.1、TypeScript7.0.2、Playwright1.62.0。型定義もpackage.json／lockへ厳密固定 |
| Java image | `eclipse-temurin:21-jre-noble@sha256:0eda5830ed7b669abc246a8dc520ea8a23cab06f9a8d4b6964109e2f2394e46f` |
| Node image | `node:24.19.0-bookworm-slim@sha256:a9f5f7c91a432850b2a8a7797adf5eadb6c733ceed61167806cee7ea7fbc29df` |
| PG image | `postgres:17-alpine@sha256:18cfe3ef5e6815560c98237d6216d1e5119702fb0f3894c8785dd58b8bbe5d73` |
| Keycloak image | `quay.io/keycloak/keycloak:26.8.1@sha256:3f3c5f8e03c614774b4bd9173b88391fefa4153bfc010d8012d59ad825377332` |
| Browser | Chromium／Headless Shell151.0.7922.34、revision1234。Linux用を取得 |

資料の候補だったNext16.2.9を選定中に`npm audit`したところcritical1／high2のpackageが報告された。候補を採用せず、auditの修正版提示とmetadataを照合してNext16.4.5へ明示変更した。最終lockで`npm ci --ignore-scripts`とauditが成功し、検出は全severity0。候補版／最終版のlock生成・取得は各1回で、両audit結果を別fileに保持した。S3開始時はsecurity情報を再確認する。

4 imageのregistry metadata取得／Linux amd64対応を確認し、各platform digestも`versions.json`へ保存した。実際にpull／buildしたのはJavaとPGだけ。Node／Keycloakのruntime適合は後段へ残す。Keycloak advisories、Temurin release、Node securityページは取得できたが、PostgreSQL公式securityページはHTTP403だった。Trivy／Grypeはなく、image全OS packageの脆弱性scanを行ったとはしない。

Chromium起動と`localhost:13000`／`idp.localhost:18080`への到達は、その場限りのloopback HTTPサーバーで200を確認し、終了した。これは環境能力probeで、実IdP Cookie／canonical discovery／SSOの成立ではない。Keycloak Cookieの実属性・送受信はS2に残す。

## 5. 初回実行結果・失敗・訂正

| 集合 | 結果 |
| --- | --- |
| Java直接回帰 | PASS、`ReferenceContainerSecurityConfigurationTest`2件、failure0／error0／skip0。build1回 |
| Tooling guard | PASS、新規5件、1回。既存project・別work・別project cleanup・未知volume・DB selectorの拒否 |
| 静的／build | Python AST／JSON／shell構文、Compose config、backend image build各成功。configの構文成功だけではmount semantics不備を検出できなかった |
| S1-V01 | 未成立。起動前のJAR／lock／image label・platform preflightは成功したが、2サービス稼働確認前に停止 |
| S1-V02 | 未実施。3 role自DB操作・他DB拒否6組・PUBLIC CONNECT確認へ未到達 |
| S1-V03 | 未実施。Java／BFF migration、seed、Keycloak DB空確認へ未到達 |
| S1-V04 | 未実施。再適用／checksum負例・Java再起動／保持へ未到達 |
| S1-V05 | 未成立。probeへ未到達。後続cleanup／secret削除・証拠exportだけは成立 |

初回`compose up`で`tmpfs: [/tmp:size=134217728,mode=1777]`がYAML flow sequenceとして2要素に分かれ、Dockerが`invalid mount path: 'mode=1777' mount path must be absolute`でbackend作成を拒否した。DB containerはCreated止まりで、DB初期化・アプリ要求は実行していない。

自動cleanupにも、`docker network ls -q`が返すnetwork IDと予定network名を比較する誤りがあり、所有不一致として安全側で拒否した。最初の`results.json`はHOLDとこの失敗を保持している。containerのproject／service／working_dir、volume／networkのproject labelと予定名を読み取り、今回projectのsnapshot Composeだけで`down --volumes`を実行した。残留0を確認後、今回生成secret5 fileを削除した。既存の匿名volume2個や他projectは操作していない。

訂正は以下だけを行い、動的再実行はしていない。

- Composeのtmpfsをblock sequenceの単一文字列へ訂正し、正規化JSONで単一の絶対mount指定になることを確認。
- preflightへtmpfs正規化結果の照合を追加。
- network台帳の列挙を`--format '{{.Name}}'`へ訂正し、mockでcommandを確認。
- 現行source hash照合を実行前preflightへ限定。cleanup／exportは不変のrecipe snapshotを照合し、訂正前Evidenceも扱えるようにした。

これらの訂正後はAST／Compose semantics／network列挙の静的確認と、初回HOLD結果のJSON schema検証が成功。動的5群はPASS扱いにしない。

## 6. 初回のcleanup・証拠export

今回projectのcontainer／volume／network残留0、生成secret削除済み。local image／JAR・npm／Browser cache・redacted Evidenceは保持する。先行方式1の保持条件は変更していない。

allowlistは`manifest.json`、`execution.json`、`results.json`、`cleanup.json`、`runtime-redacted.log`の5 file。archive内に各fileのchecksumを持つ`archive-manifest.json`も追加した。秘密入りenv・JAR・DB dump・raw trace／HAR・node_modulesは含めない。今回runtime logはサービス未起動のため空で、生tokenは発行していない。

| export項目 | 値 |
| --- | --- |
| archive | `.tmp/d3-sso-exports/d3-sso-s1-20261109-01-evidence.zip` |
| size | 5,340 bytes |
| SHA-256 | `4315c3038757c3fd23eb90742c7eb2fffe6d801d7c61dfa8eac14b6a665268a1` |
| 内容 | 上記5 file＋archive manifest、計6 entries |
| 持帰り状態 | export済み。Windows側の取得・checksum照合は未確認。利用者向けdownload経路の成立も未確認のためOPEN |

補助Evidenceはbuild結果・command／log、5件guard結果、候補／最終package audit、各registry metadata、Browser取得／loopback probe、開始manifest。再現用の実行snapshotとTooling hashはworkのmanifestに記録する。これらのbinary・cache・未点検rawをGitへ入れない。

## 7. 再実行提案と次段階（承認前の記録）

今回の訂正だけを反映した新規project `d3-sso-s1-20261109-02`で、S1-V01〜05を初回と同じ集合・各1回、動的40分（preflight10／DB・migration20／cleanup10）を上限として再実行する案を提示する。既存JARとbackend imageの内容は変わらないため、hash／labelを再照合して再利用し、Java build／直接回帰2件を反復しない。新しいsecret・DB・recipe snapshotを作り、旧workを上書きしない。結果・exportを別記録し、初回失敗を保持する。再実行の開始指示はまだ受けていない。

S1の実DB検証が成立するまでS2を開始しない。S2の実IdP／API・S3 BFF・S4 SSO・S5 refresh／障害を今回の成果へ含めず、D3／Phase4全体・正式support・Owner結果受入の状態も変更しない。

**実案件への示唆：** DB owner／migration／runtime兼用は検証限定の簡略化であり、案件の権限分離は別設計とする。Compose構文確認に加えmountや公開portの正規化結果を照合し、cleanup台帳ではIDと名前のどちらを比較するかを統一する。

## 8. Owner承認後の修正済み新規project再実行

Ownerは「修正後の新規projectで、同じS1検証集合を1回再実行してください。フレームワーク本体の改変を伴うような場合でなければ、修正後再実行は許可」と指示した。今回のS1範囲で同一許可を反復要求しない。S2の開始やFramework変更・remote操作へ拡張しない。

新規work／project `d3-sso-s1-20261109-02`をprepareし、新しいsecret・DB／volume／network・訂正後recipe snapshotを使用した。元JARのSHA-256とbackend image IDは§2の成功buildと同一で、imageに新規project用tagを付けて再利用した。JAR再生成・Java直接回帰・image再build・初回guard5件の反復は不要と判断し、実行していない。

動的集合はS1-V01〜05、同じ資源条件・40分枠（cleanup10分予約）で1回実行し、48.189秒で自動cleanupまで成功した。実行時のTooling全22 file hash、recipe／JAR／lock hash、source `68ab796`との対応は新workの`manifest.json`に固定した。初回Evidenceは変更していない。

| 検証 | 再実行結果 |
| --- | --- |
| S1-V01 | PASS。専用2サービス、JAR／lock／base digest／実imageの識別、非root／read-only／CPU／memory、host公開port0、backendへの管理credential非供給を確認 |
| S1-V02 | PASS。3 roleのsuperuser／DB作成／role作成／replication／bypassRLSはすべてfalse。自DB owner・rollback付きDDL／INSERT／SELECTが3組成立。他2 DBへの接続拒否6組、PUBLIC CONNECTなし、PG接続上限32を確認 |
| S1-V03 | PASS。KOIKI3件・Reference4件migration成功。4利用者、role割当3件、所属4件のseed成立。BFF3 tableの実column／NULL条件・indexを記録。Keycloak DBの製品tableは0 |
| S1-V04 | PASS。同じBFF migration再適用でhistory内容・適用時刻不変。checksum変更は明示拒否しhistory不変。Java再起動後、利用者行hashと両Flyway historyを保持、seed再投入なし |
| S1-V05 | PASS。GET live／readyは200で本文はstatus UPのみ、health親／envは401。自動cleanup成功、container／volume／network残留0、今回の生成secret5 fileを削除 |

実測の`pg_stat_activity`は内部4、koiki_app3、postgres1の計8行で、app3には観測用psql1接続を含む。これはその時点の観測であり負荷時pool最大の実証ではない。各roleは自DB内のowner／migration／runtime兼用で、DDL権限を持つ簡略構成を維持する。

backendの構造化ログはINFO83／WARN4／ERROR0。WARNは初回起動・再起動それぞれの既存認証provider／cache統計警告。Startedログは14.569秒／16.183秒で、起動性能比較はしない。DBのFATAL6件は予定どおりの他DB接続拒否、ERROR1件はchecksum不一致の負例で、どれも検証結果と対応する。Session属性重複キー例外・生成secret値のログ露出は検出していない。Session事象の解消とは判定しない。

今回のbackend／DB container・volume・networkは自動削除済み。旧匿名volume2個・方式1資源は操作していない。image／元JAR・依存cache・redacted Evidenceは保持する。結果はJSON schemaで検査し、PASSと5 checkpointのIDを照合した。

| export項目 | 再実行分 |
| --- | --- |
| archive | `.tmp/d3-sso-exports/d3-sso-s1-20261109-02-evidence.zip` |
| size | 11,680 bytes |
| SHA-256 | `84ad9a314286665cd4467f65cd477be840a25a0e57bf120ab2d0af4328c04978` |
| 内容 | manifest／execution／results／cleanup／runtime-redacted.log＋archive manifest、計6 entries |
| 持帰り状態 | export済み。Windowsの取得・checksum照合は未確認。Cloud download経路の成立も未確認としてOPENを保持 |

元のproject `01`の失敗archiveも§6のSHA-256のまま保持し、成功archiveで置き換えていない。補助記録は`verify-02.log`／`runtime-review-02.json`／`export-02.json`。secret入りenv・鍵・DB dump・trace／HARはexport対象外。

S1の限定技術成立を報告し、ここで停止する。S2のKeycloak実接続、BrowserによるOIDC／API journey、BFF／SSO・refresh／障害、Windows再現・手動操作は今回未実施。Framework本体の変更、結果全体のOwner受入・D3／Phase4全体認定、commit／push／PR／mergeも行っていない。

## 9. S1コミットポイントのOwner判断

Ownerは「S1 PASSを、ここでコミットポイントと認識しますので、コミットしてください」と指示した。S1のTooling実装22 file、本結果記録、AGENTSの現行入口をlocal commitとして固定する。上記のcommit未実施表示はこの後続指示前の履歴であり、実際のcommit SHAは操作後の報告とGit履歴で確認する。

今回の判断はS1 PASSのコミットポイントであり、S2開始・push／PR／mergeや、D3／Phase4全体・正式supportの受入を含めない。初回失敗・成功の両Evidence、Windows持帰り未確認を保持する。
