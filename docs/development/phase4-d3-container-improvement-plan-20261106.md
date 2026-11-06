# D3方式1：Container build・設定改善の実施計画案（2026-11-06）

**現行状態：** Ownerは本計画を了解し`3b095a8`へcommit後、改修開始を指示した。Reference／Toolingの改善・layer測定5 invocation・直接回帰25件・7 checkpointの限定検証を終了し、[結果](../architecture/validation/phase4-d3-container-improvement-results-20261106.md)へ記録した。結果Owner受入・成果commit／remoteは未実施。以下の未着手表示は計画固定時点の条件として保持する。

## 1. 目的・状態・Ownership

方式1（SPA同梱Java app＋PostgreSQL）の成立済み構成を基点に、image更新時のlayer再利用と、実案件で設計・調整すべき項目の見通しを改善する。
本構成はReferenceである。実案件は独自にContainer設計するか、本資材を参考に設計する。今回の値を推奨容量・最適値・本番標準として保証しない。

| 項目 | 今回の位置づけ |
| --- | --- |
| Phase／status | D3方式1の追加改善計画案。今回の文書作成で実装・動的検証を開始しない |
| Ownership | Tooling：Dockerfile／Compose／検証script・証拠。Reference：専用profileの設定例 |
| 対象 | `build-support/container-verification/integrated/`、`koiki-reference-app`の`d3-container`設定、関連文書 |
| 維持する境界 | Framework本体、Root Reactor／CI、公開済みartifactを変更しない。D3全体・正式support・Phase 4全体の認定へ拡張しない |
| 次の判断 | 本計画の確認後、実装・有限検証の開始指示を受けて進める。commit／push／PR／merge／tag／image公開は今回の計画作成に含めない |

入力はOwner提示のClaude Code点検と、その後のOwner／Codex対話である。端末固有の原本は`C:\Users\s-kataoka\.claude\plans\dockerfile-docker-compose-java-happy-nest.md`。本書へ必要な判断を移し、この絶対pathへのアクセスを他端末の再開条件にしない。

## 2. 現在地と再利用するコンテキスト

### 2.1 Source・設計・既存結果

- 作業branchは`docs/d3-01-container-contract-20261105`。文書作成前HEADは`335e062ee7608061567c5e231a36724e1d2f9082`。
- 手元の`origin/main`はPR #60 mergeの`6bd2b0b3690ac9cb6a5ec171ec95cb5031350893`で、HEADとのtree差分なしを確認した。commit自体は異なる。ローカル`main`参照の更新や新たなremote同期を実行済みとはしない。
- [構成・起動契約](phase4-d3-01-container-contract-20261105.md)、[2026-11-05限定結果](../architecture/validation/phase4-d3-integrated-container-results-20261105.md)、[実行手順](../../build-support/container-verification/integrated/README.md)を引き継ぐ。過去結果は変更前のEvidenceとして残す。

### 2.2 このPCでの改善前確認（2026-11-06）

| 対象 | 確認済み内容・証拠 |
| --- | --- |
| build環境 | 拡張管理Temurin `21.0.13+9`でjava／javac／Maven使用JDKが一致。Maven Wrapper `3.9.16` |
| frontend | ignored専用領域`.tmp/d3-toolchain/node-v24.19.0-win-x64`にNode `24.19.0`／npm `11.9.0`。端末の既定Nodeを変更していない |
| Python | 導入済み`3.11.9`を直接指定。pyenv shimの既定版は未設定のため、単なる`python`呼出しを前提にしない |
| Docker | Engine `29.5.3`／Linux amd64、Compose `v5.3.1`。sandboxからのpipe拒否とEngine停止を区別する |
| JDK／CA | JDK再取得で版不整合を解消後、初回buildがPKIXで停止。既存`import-corporate-root-ca.ps1`でWindows信頼済み社内CAを対象JDKへ登録し、次のbuild成功。JDK置換時には再確認する |
| Reference JAR | `b1-session-spa,d3-container`で再生成、直接回帰25件PASS。SPA asset 3件hash一致、container設定・Actuator同梱、async JDBC非同梱 |
| JAR識別 | `koiki-reference-app/target/koiki-reference-app-0.1.0-SNAPSHOT.jar`、66,896,420 bytes、SHA-256 `b160af3a1a8e80cbc9cc0fb0787046f054c21debe784a48d1ae4fb7865ba6ed7` |
| 自動確認 | `.tmp/d3-local-baseline-20261106/`の`results.json`／`source.json`／`build.log`／`runtime-sanitized.log`。既存runnerの7 checkpoint（DC01／02／03／04／06／07／08）PASS、専用資源cleanup済み。DC05相当のsecret欠落負例は今回再実行していない |
| 起動参考値 | アプリのStartedログは初回11.013秒／再起動10.438秒。Compose待機時間とは別の値。別端末の結果との厳密な性能比較には使わない |
| browser前提 | Playwright `1.62.0`用Chromium v1234を取得。初回実行時にはPlaywrightが他browser資材も自動取得したが、実行したjourneyはChromiumのみ |
| 手動確認 | `.tmp/d3-manual-local-20261106/manual-ready.json`。Ownerは作成・提出・承認に加えて差戻し等を操作し、問題ない認識を報告。DB点検は申請2件の承認済み状態、差戻し→再編集→再提出→承認Audit、明細金額一致を確認 |

ローカルEvidenceのpathは追跡用であり、Gitへbinary・secret・raw logを格納しない。他端末では同じ`.tmp`の存在を要求せず、新しい資材・識別子・証拠を用意する。手動確認の報告をD3全体の最終受入へ読み替えない。

### 2.3 手動環境の保持と別件

手動projectは`d3-manual-local-20261106`、起動時URLは`http://127.0.0.1:18081/login`。計画作成後のOwner整理指示で`down`（volume削除なし）を実行し、app／DB container・専用networkは削除済み。DB volume・secret・image・Evidenceを保持し、自動削除・再起動・seed再投入しない。`manual-ready.json`は起動時点の履歴、同workの`manual-environment-state.json`を現状記録とする。別端末の`d3-manual-20261105`は今回操作していない。

同手動環境の14:44:01 JSTに、`SPRING_SECURITY_SAVED_REQUEST`保存の重複キー例外を観測した。関連するappログ3件は同一threadの元例外・エラー処理中の例外であり、3回の独立故障とは数えない。同時要求競合は候補で、要求URL・契機は未確定。Ownerは`PostgreSqlJdbcIndexedSessionRepositoryCustomizer`採用を別件で検討する方針。今回の改善で修正・再現試験・request cache変更を行わず、既知事象として残す。

## 3. 改善要件と判断

| ID | 要件・採用方針 | 成立確認 |
| --- | --- | --- |
| A1 | Spring Boot公式例に沿い、生成済みJARを第1 stageの`tools`で抽出し、第2 stageで4区分を別COPYする | 最終imageの起動配置とlayer再利用を確認。最終imageへ元fat JARを重複格納しない |
| A2 | probeを安定した資材として先に配置し、dependencies→spring-boot-loader→snapshot-dependencies→applicationの順にCOPYする | applicationだけの変更で依存layerを再利用できる。自作SNAPSHOT依存を変更すれば、その区分も更新されることを説明 |
| A3 | 元JARのSHA-256を`artifact.json`と`compose.env`へ記録し、build argからimage labelへ渡す。起動したcontainerのimage情報と照合する | 元JARの取り違え・古いimage利用を検出。labelは識別情報であり、展開後全ファイルの一致証明とはしない |
| B1 | app healthcheckに`start_interval: 2s`を明示。90秒のstart period、通常10秒間隔、timeout／retriesは維持 | 対応Engine／Composeで構文・実効設定を確認。対応Engineのstart interval既定は5秒であり、未指定なら10秒という旧指摘の説明は採用しない |
| B2 | `d3-container`専用profileへTomcat thread上限40を例示値として設定 | PID上限160にはJVM等の他thread・probeの余裕が必要であること、DB pool 4から最適thread数は決まらないことを記載。負荷試験による最適値保証はしない |
| B3 | 今回の計画案は固定`-Xmx768m`を維持し、container memory 1536 MiBとの配分を明記する。`-XX:+ExitOnOutOfMemoryError`追加を提案する | 比率指定`MaxRAMPercentage`は代替設計として説明。heap以外のmemoryも必要。終了設定はJava heap OOM時の方針であり、container memory枯渇を全て検出する設定ではない |
| B4 | app／dbに`json-file`、`max-size: 10m`／`max-file: 3`を明示する | 実効設定と通常ログ取得を確認。実案件の収集・保持方式に置換する設計箇所として説明 |
| DOC | 各設定の例示値・選択理由・実案件で見直す条件を、READMEと必要な設定付近のコメントへ記載する | 必須起動契約と任意のcapacity／運用設計を区別。値を全案件の標準へ一般化しない |

B2／B3／B4は本書の具体案であり、未適用。実装開始前の計画確認で採否を確定する。通常GCは変更しない。thread上限はvirtual threads有効時には同じ制御にならないため、今回virtual threadsを有効化せず、案件での切替時の見直し条件として説明する。

### 3.1 A1／A3の構造と混入防止

抽出は`java -Djarmode=tools -jar app.jar extract --layers --destination extracted`を使用する。依存JARを全てclassへ解体する方式ではない。実行stageでは抽出後の起動用JARと依存配置を使い、JavaをPID1とするexec形式の`java -jar /app/app.jar`を維持する。

- base imageは既存digest固定のJava 21 JREを両stageで使用し、Maven／Node buildはContainerへ移さない。
- `prepare.py --jar`で指定した一つのJAR、専用context、`.dockerignore` allowlist、所定directoryの明示COPYを維持する。wildcardでcheckout全体を取り込まない。
- 既存の元JARに対するSPA build record／asset hash・async JDBC非同梱検査を維持する。現在この検査はprepare時の元JARが対象であり、image内でのSPA hash再照合を実装済みとはしない。
- SHA-256用ARG／LABELはruntimeの依存COPY後に置き、artifact識別の変更で依存layerを不要に無効化しない。欠落値・不正hash形式は準備／検証時に拒否する。
- 抽出によってSPA資材の配置・配信が変わらないことをimage内容確認とbrowser journeyで確かめる。全class／resourceのhash台帳、署名・attestation基盤は追加しない。

公式根拠：[Spring Boot Dockerfiles](https://docs.spring.io/spring-boot/reference/packaging/container-images/dockerfiles.html)。参照例のJava版・imageをそのまま転記せず、今回のJava 21と固定digestを維持する。

## 4. 実装対象と維持する契約

| 資材 | 予定差分 |
| --- | --- |
| `integrated/Dockerfile` | 2 stage抽出、4区分COPY、probe順、元JAR hash label |
| `integrated/compose.yaml` | build arg、start interval、ログ上限、採用時のOOM終了設定・説明 |
| `integrated/prepare.py` | 計算済みhashをenvへ供給。既存JAR検査と新規work条件を維持 |
| `integrated/verify.py`／`start-manual.py` | 元fat JAR byte照合をcontainerのimageに記録したhash labelとの照合へ変更。image IDをEvidenceへ保持 |
| `application-d3-container.properties` | 専用profileだけのthread上限・調整意図 |
| `integrated/README.md`、新しい結果文書・入口 | 起動配置、識別の限界、調整項目、測定・検証結果、既知事象 |

`integrated/`は`build-support/container-verification/integrated/`を指す。実装時のtest／測定補助が必要ならTooling内に限定し、対象と必要性を差分に示す。Framework・依存版・Root POM／CI・migration・Security chainは変更しない。

非root UID10001、read-only＋tmpfs、cap drop、no-new-privileges、loopback公開、file secret／configtree、DB role／migration順、Session／CSRF／認可、graceful shutdownの既存契約を維持する。変更する設定以外の資源上限を自動で増減しない。

## 5. 対応外・保留一覧

| ID／対象 | 今回対応しない内容・再開条件 |
| --- | --- |
| A4：再現可能build | 同一sourceの再package比較と`project.build.outputTimestamp`等の導入を今回の実装・測定範囲に含めない。layer測定で説明できない依存差が出た場合に報告し、必要性を別判断する |
| B5：CDS／起動高速化 | AppCDSの学習・archive保存、AOT、start period短縮を実施しない。参考起動時間から必要性が具体化した場合に検討する |
| B6：restart policy | 自動再起動・手動環境overrideを追加しない。DB停止回復検証を維持し、実運用要望が出た時に別判断する |
| Session保存エラー | PostgreSQL用Session customizer・SQL／request cache変更・再現試験は別件 |
| その他 | HTTPS／Secure Cookie、処理中drain、別案件Consumer、全通常root／CI・async suite、ECS、方式2SSO、registry公開・deployは含めない |

## 6. 実施順序と有限検証

### 6.1 開始前確認

1. 本計画の確認と実装・限定実行の指示を得る。文書作成指示を実行承認へ拡張しない。
2. 開始時のsource commit・差分・対象file hash、JAR hash、toolchain／Docker版を記録する。旧検証のsource／JARと混同しない。
3. 手動環境の現行Compose／起動資材を、その専用ignored workへ控え、保存済みimage識別・環境状態と対応づける。改善後資材による誤recreateを避ける。既存secret内容は出力しない。
4. 新しい測定work・検証project・空きportを確保する。同名project資源・結果がある場合は拒否する。固定の`18081`を改善検証に使わない。

### 6.2 作成・静的確認・layer測定

- 対象差分を作成し、Python構文、`docker build --check`、Compose `config --quiet`を確認する。
- 同一の改善前JARと、applicationのclass領域だけへ測定用dummy resourceを追加した別JARを専用contextへ用意する。原本JARを上書きせず、改変JARで業務検証しない。
- 旧／新Dockerfileそれぞれで原本・改変JARのbuildを行い、新Dockerfileでは同一原本JARの再buildも確認する。測定用image buildは最大5 invocationを計画し、原本JARの比較はA4の同一source再packageとは区別する。
- buildログの`CACHED`、image layerの識別・容量、経過時間を記録する。依存区分の再利用とapplication変更の分離を主な判定とし、「1 MB未満」「何秒短縮」を合否条件にしない。`docker history`の表示だけで転送量削減を実測済みとしない。registry push／pull測定はしない。
- 最終imageの起動用JAR／依存／probe／SPA資材の配置を確認する。標準抽出以外の独自再構成が必要になった場合は自動で広げない。

### 6.3 機能・実効設定の確認

- 専用profile変更を含むReference JAR再生成は既存build helperで1回、既存直接回帰25件を実行する。通常root全件を追加しない。
- 改善後の正式検証用imageを、新しいwork／projectでbuildする。上記測定imageと役割を分ける。
- `prepare.py`→image build→`verify.py`の既存7 checkpointを1回実行する。DC01のartifact判定はlabel識別へ置換したことを結果に明記する。DC05 secret欠落負例を再実行したとはしない。
- 非root／read-only／PID1、SPA操作・CSRF／logout、DB／Audit、migration、app再起動、DB停止回復・二重提出なし、所有資源cleanupを確認する。
- heap／OOM終了flag、Tomcat上限、PIDの余裕、healthcheck・ログ上限の実効値を確認する。OOM故障注入、最大負荷、ログ容量満杯の試験は追加しない。
- StartedログとCompose `up --wait`経過を分けて参考記録する。別端末・cache・DB初期化の違いを含む値から厳密な性能向上を主張しない。

各集合は計画した初回のみ。失敗・追加測定・再実行が必要なら結果と必要差分を示して範囲を判断し、旧予算や自動検証成功を再実行根拠にしない。改善後の手動環境起動・Owner打鍵は自動で追加せず、この結果を見て判断する。

### 6.4 資源・停止・cleanup

buildと新規検証runtimeを並走させない。手動環境のcontainerは整理済みのため現時点のruntime資源には計上しない。今後別指示で再起動した場合は、そのapp 1.5 GiB／CPU1・DB 1 GiB／CPU1を別に計上する。新規検証も同じ既存上限、app PID160／DB最大16接続、browser・host JVMは別計上とする。PCの余裕が足りなければ開始を見合わせ、他環境を勝手に停止しない。

所有外資源との衝突、secret出力、契約外変更、起動配置不成立、限定実行の失敗、観測・cleanup不成立では新しい検証へ進まず、証拠を保存する。既知Session事象が再発した場合も隠さず今回の結果へ記録し、承認外修正・再試行で消さない。

成功／失敗とも既存runnerのfinallyで今回所有のcontainer／volume／networkだけをcleanupし、残留を照合する。失敗でrunnerがcleanupできない場合はproject label・絶対work pathを照合して対象を限定する。既存手動project、Docker全体、他projectのimage／volumeを一括cleanupしない。測定／検証imageとignored Evidenceは識別して保持し、削除は不要判断後に行う。

## 7. 完了条件と残す成果

1. A1／A2のlayer分離・再利用、A3のlabel識別・既存混入防止、採用したB項目の実効設定が確認できる。
2. 専用profileの直接回帰と既存7 checkpointが成立し、今回資源cleanup・既存手動環境保持を確認できる。失敗・未実施・既知Session事象を分けて記録する。
3. READMEから「例示値／理由／案件の見直し条件」が読み取れ、A4／B5／B6の保留と適用限界が残る。
4. 新しい結果文書へsource／JAR／image識別、環境、測定方法・結果、検証件数、差分、cleanup、未実施を記録する。旧結果を上書きしない。

以上はReference／Toolingの限定改善の完了条件である。既存S1未達・人手HOLD、C2 H07、D1結果受入未記録、V10、正式support／全DoD・Phase 4全体の状態を変更しない。

本書は計画案であり、実装・改善後動的実行・成果commit／remote操作は未実施。
