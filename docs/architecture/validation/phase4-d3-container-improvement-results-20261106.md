# D3方式1：Container build・設定改善の限定結果（2026-11-06）

## 1. 判定と範囲

**限定技術成立。結果Owner受入は未記録。** Ownerが[計画](../../development/phase4-d3-container-improvement-plan-20261106.md)を了解し、`3b095a8`へのcommit後に改修開始を指示した。同branchでReference／Toolingだけを変更し、layer測定5 invocation、Reference再生成／直接回帰25件、正式検証image build、既存7 checkpointを各計画範囲で実行した。今回の実装・結果は未commit。

Framework本体、依存版、Root Reactor／CI、migration、Security chain、公開済みartifactは変更していない。A4／B5／B6とSession保存エラー対応は範囲外を維持する。今回の結果をD3全体・正式support・Phase 4全体の認定へ拡張しない。

## 2. Source・環境・artifact識別

| 項目 | 記録 |
| --- | --- |
| source | branch `docs/d3-01-container-contract-20261105`、`3b095a8`＋今回の未commit実装差分。開始時cleanを確認 |
| 実行環境 | Windows 11、Temurin `21.0.13+9`、Maven Wrapper `3.9.16`、専用Node `24.19.0`／npm `11.9.0`、Python `3.11.9` |
| Container | Engine `29.5.3` Linux amd64、Compose `5.3.1`。Java／PostgreSQL imageの既存digestは変更なし |
| 改善前JAR | SHA-256 `b160af3a1a8e80cbc9cc0fb0787046f054c21debe784a48d1ae4fb7865ba6ed7`。測定前に`.tmp/d3-local-baseline-20261106/baseline-app.jar`へ保存 |
| 改善後JAR | `b1-session-spa,d3-container`で再生成、66,896,576 bytes、SHA-256 `9c76633d930e79cc2b69562002624093973793b016f00083219ed74d0dfbc6ef` |
| 正式検証project | `d3-improved-20261106`、port18082、work `.tmp/d3-improved-20261106/` |
| 正式検証image ID | `sha256:b251d5ef036c9444f90378a5280e22f6914ba2cb7a811e9b764124739c7378cb` |

開始時資材hashは`.tmp/d3-local-baseline-20261106/pre-improvement-source.json`、実行資材hashは改善workの`source-files.json`へ保存。Reference設定を含む最終inventoryは`source-final.json`に別記録する。binary・secret・raw logはGitへ入れない。

## 3. 実装結果

| 要件 | 適用結果 |
| --- | --- |
| A1／A2 | Boot標準toolsで抽出する2 stageと、probe→4区分のCOPY。最終imageは小さな`/app/app.jar`と依存JAR配置を使い、元fat JARを重複格納しない |
| 分類上の確認 | 今回のReactor JARでは自作モジュールはapplication区分。loader／snapshot-dependencies区分は空。分類を強制するPOM変更はしない。tools抽出自体はJavaをhost実行して配置確認し、image buildでも同方式を実行 |
| A3 | prepareで計算した元JAR hashをenv→build arg→image labelへ渡し、runnerは起動containerが使うimage IDのlabelと照合。hash形式guardを追加 |
| B1 | 起動中healthcheckを2秒へ明示、通常10秒／start period90秒等は維持 |
| B2 | `d3-container`だけでTomcat最大40を設定。imageから取り出した小さなJARで設定値とSPA配置を確認 |
| B3 | 固定heap768 MiBを維持し、`ExitOnOutOfMemoryError`を追加。memory1536 MiBとの配分・比率指定の代替・OOM検出範囲を説明 |
| B4 | app／dbともjson-file、max-size10m／max-file3を設定し、container inspectで照合 |
| DOC | [実行手順](../../../build-support/container-verification/integrated/README.md)に例示値・理由・案件の見直し条件、layer／識別の限界、旧手動環境の再開時の注意を追加 |

A3は抽出後全ファイルのhash一致証明ではない。混入防止は明示JAR・専用context・allowlist・所定COPYと既存JAR検査により行う。元JARのSPA asset3件hash一致・async JDBC非同梱をprepareで確認し、実配信はbrowserで確認した。

## 4. layer測定（5 invocation）

測定用の元JARは全て改善前JAR。同じJARの`BOOT-INF/classes`へ測定専用resourceを一つ追加したvariantを別生成し、旧／新Dockerfileでそれぞれ比較した。これは業務code変更の代替観測で、元JARを上書きせず、variantではアプリを起動していない。同一sourceからの再package試験（A4）でもない。

| 構成・入力 | build経過秒 | filesystem layerの比較 |
| --- | --- | --- |
| 旧・元JAR | 1.843 | 8層 |
| 旧・application変更 | 3.985 | 元JARと先頭6層が共通。JAR COPY層は約66.9 MB |
| 新・元JAR | 5.188 | 11層 |
| 新・application変更 | 3.125 | 元JARと先頭10層が共通。application COPY層は約635 kB、dependencies COPY層約66.3 MBは再利用 |
| 新・同一元JAR再build | 1.235 | 11層とも同一。抽出とruntime COPYを含むbuild stepのCACHEDを確認 |

容量は`docker history`表示であり圧縮転送量の実測ではない。空区分にもdirectory metadataの小さな層が存在する。BuildKit attestationを含むimage manifest識別は同じfilesystem layerでも変わり得るため、再buildのimage ID一致を要件にしない。

上表は各1回でcache状態が異なる。新構成は抽出工程を追加するため、初回build時間や全build時間の大幅短縮を主張しない。改善点はapplication変更と大きな依存層の分離・再利用。registry push／pull測定はしていない。

補助scriptは`.tmp/d3-measure-layers.py`、入力context・各buildログ・image layer識別・history・時間は`.tmp/d3-layer-measure-20261106/`へ保持する。旧資材は旧手動work内`recipe-before-improvement/`から、新資材は今回sourceから取得した。

## 5. 限定機能・設定・ログ

| 確認 | 結果 |
| --- | --- |
| 静的 | Python3 script構文、`docker build --check`（警告なし）、Compose `config --quiet`成功 |
| 直接回帰 | 既存3 test class／25件、failure0・error0・skip0、Reference再生成成功。`.tmp/d3-improved-reference-build-20261106.log` |
| DC01 | 非root／read-only／PID1、memory・PID設定、image label元JAR識別、packaged thread設定40・SPA配置を確認。旧結果のimage内fat JAR byte一致とは判定方式が異なる |
| DC02 | 起動・migration・readiness成立 |
| DC03 | 実Chromiumでlogin→SPA作成→CSRF欠落403→提出→reload→logout／API拒否成立 |
| DC04 | 匿名probe正常、未公開Actuator／API拒否、専用非superuser DB role・migration成立 |
| DC07 | app再起動後の申請・Audit・migration保持 |
| DC06 | DB停止時readiness503／liveness200、DB回復後ready・再提出なし |
| DC08 | 今回所有container／volume／networkの削除と残留なし成立 |
| 実効設定・余裕 | JVM optionのcontainer設定、health start interval2秒、両serviceのログdriver／上限をinspectで確認。起動時Java thread31・cgroup PID32、上限160以内。最大負荷時の余裕保証ではない |

今回の7 checkpointは初回1回でPASS。DC05相当のsecret欠落負例、通常root全件、通常profileの別Security2件、async suite、OOM注入・ログ容量満杯・負荷試験は追加していない。`start-manual.py`のlabel識別変更は構文・source確認のみで、改善後の新しい手動projectを起動した結果とはしない。

起動参考値はアプリStartedログ初回9.180秒／再起動9.191秒、Compose初回`up --wait`は15.797秒。改善前のStartedログ11.013秒／10.438秒とはcache等の条件差があり、layer化の起動高速化効果と断定しない。Tomcat上限はpackaged propertyを確認しており、管理endpoint追加や負荷による上限到達試験はしていない。

appログにERRORはなく、旧手動環境のSession重複キー事象は今回ログに現れていない。修正していないため解消とは判定しない。WARN7件は起動／再起動の認証provider・cache統計警告各2件と、意図的DB停止時のpool接続検査2件・DataSource health1件である。

証拠は改善workの`artifact.json`／`results.json`／`browser.txt`／`spa.png`／`runtime-app.jar`／`build.log`／`runtime-sanitized.log`。secret値がruntime logへ出ていないことは既存runnerで確認した。

## 6. 保持環境・未実施・次の判断

旧手動projectの再開用資材は改修前に`.tmp/d3-manual-local-20261106/recipe-before-improvement/`へコピー済み。終了時にも同projectのDB volume `d3-manual-local-20261106_database`が同じ作成時刻で存在することを確認し、再起動・seed再投入・volume／secret／image削除を行っていない。別端末の`d3-manual-20261105`にも操作していない。

実装・測定・有限検証は今回の計画範囲で終了した。次は結果と差分の確認、Owner結果受入・成果固定の判断であり、追加動的実行・手動環境起動・commit／remoteを自動先行しない。

A4再現可能build、B5 CDS／起動高速化、B6 restart policy、Session customizer、HTTPS／Secure Cookie、処理中drain、別案件Consumer、ECS、方式2SSO、registry公開／deployは未実施。既存S1未達・人手HOLD、C2 H07、D1結果受入未記録、V10、全DoD／正式support／Phase 4全体の状態を維持する。

## 7. 改善版の手動確認環境引渡し（後続Owner指示）

Ownerが手動ブラウザ検証への進行を指示したため、新規project `d3-improved-manual-20261106`／port18081を準備し、変更後の`start-manual.py`を1回実行した。build・起動・専用DB seed・元JAR hash label識別・両利用者のHTTP login／選択route確認はPASS。元JAR hashは§2と同じ、手動image IDは`sha256:e28203ace20dc44e1fdb11c4ca230ad5cf53590b5f22414cb9723ec9b83281f2`。正式自動検証imageとは別のbuildであり、同一元JARでもmanifest ID一致を要求しない。

URLは`http://127.0.0.1:18081/login`。work `.tmp/d3-improved-manual-20261106/`の`manual-ready.json`／`manual-status.jsonl`／`manual-build.txt`に記録した。認証fixtureのIDは従来と同じだが、passwordは新規生成で、同workの`secrets/login-password`を参照する。値をGit・文書へ転記しない。

app／DB／volume／secretを手動打鍵用に保持し、自動停止・cleanupしない。旧`d3-manual-local-20261106_database`は再開・変更していない。現在はOwner打鍵結果・後続ログ／DB照合待ちであり、手動確認完了・Owner受入として数えない。§5の手動runner未実行表示と§6の追加起動未実施は、この後続指示前の状態を示す。

## 8. 改善版Owner打鍵報告後のログ・DB点検

Ownerは§7の環境で、案内した順（SPA作成・提出、差戻し、再編集・再提出・承認、MVC作成・保存、logout・再login）に確認し、いずれも問題なしと報告した。後続依頼により2026-11-06 17:12 JST、稼働を保持したままログ取得と`REPEATABLE READ READ ONLY` transactionによるDB点検を行った。

| 点検対象 | 結果 |
| --- | --- |
| 稼働状態 | app／DBともrunning・healthy |
| 申請 | 1件、4,800円、APPROVED、version7 |
| Business Audit | 同じ申請にSUBMIT→RETURN→BEGIN_REEDIT→SUBMIT→APPROVEのSUCCESS5件が時系列で存在。17:00:49提出→17:05:07差戻し→17:06:48再編集→17:07:12再提出→17:07:55承認（JST） |
| Security Audit | LOGIN SUCCESS8件。起動helperによるHTTP確認分を含むため、全件をOwner操作回数とは数えない |
| 整合性 | 明細1件合計4,800円と申請額一致。孤立明細0件、承認済み申請の成功Audit欠落0件 |
| migration | Framework3件／Reference4件、全件success |
| appログ | INFO45件・WARN2件、ERRORなし。WARNは起動時の認証provider／cache統計の既存警告 |
| DBログ | 初期化時のlocale／local trust認証警告を確認。ERROR／FATAL／PANICは検出なし |
| 既知Session事象 | 取得ログに`pk_koiki_session_attributes`の重複キー例外は現れていない。Session対応を適用していないため解消とは判定しない |

DB／Auditは保存後状態・遷移を裏付けるが、同じtableを使うSPA／MVCの操作経路を単独では区別できない。MVCを含む画面別の成立はOwner報告、永続状態の整合は今回の読取点検として分けて扱う。結果全体の最終受入・D3全体認定を自動で記録しない。

証拠は同workの`manual-review-db.txt`／`manual-review-summary.json`／`manual-review-sanitized.log`／`manual-review-status.jsonl`。secretはlog保存時に除去し、値を本文へ転記していない。DB更新・設定変更・再起動・cleanupは行わず、改善版手動環境は稼働保持中。§7の打鍵／点検待ちは今回点検前の履歴とする。
