# E1：配布ZIPへのソースJAR・Javadoc JAR同梱の引継ぎ（2026-11-12）

## 現行判断：利用側是正branchへ統合（2026-11-12）

Ownerは、今回のヘッダー／Session logout是正とsource／Javadoc同梱を`0.1.0-preview.2`へまとめ、このbranchで進める方針を了承した。現行作業branchは`claude/eager-hawking-2pyla8`、開始HEADは`d0b39bd`。別branchの`e718cc8`から本書を取り込み、Git merge／cherry-pickや自動commitは行っていない。以下の元branch指定・cloud観測は引継ぎ時点の履歴である。

- A1＋B1を採用：sources11座標／Javadoc5座標。既存Javadocの記述拡充は別task。
- Maven Source Plugin `3.4.0`、Javadoc Plugin `3.12.0`を公式[plugin一覧](https://maven.apache.org/plugins/)で確認し固定。Tooling専用`classifier-inventory.json`へ記録し、歴史的Phase1aの`BUILD-BASELINE.json`と配布Parentは変更しない。
- doclintは`all,-missing`。記述不足だけを今回のB1から除き、構文・参照等の検査は残す。
- sourcesは`src/main/java`の全Javaを収録（internalもsource参照用に含む）。resourceは重複収録せず、各sourceの内容をstaging sourceと照合する。Javadocはinternalを除外し、公開型のHTMLとlocal絶対path非混入を検査する。
- stagingの通常buildを`clean install`として、明示したproducer cacheへ本体を登録後、sources11／Javadoc5のmoduleを選択してCLI goalを実行する。これによりinternal-only moduleへJavadoc生成を要求せず、公開module間の固定版依存を解決する。これはローカルproducer登録でありremote公開ではない。追加goal実行前後の本体12 JARはSHA-256完全一致を要求する。
- manifest schema2、payload42／manifest files44／bundle実ファイル46。installerはclassifierを含む独立固定一覧・全hashを検証し、同一座標を1 invocationで登録する。
- §2の`CookieSerializer`条件はpreview.1の旧観測。今回のsourceではBoot Session構成の評価順修正済みで、Applicationの独自宣言なしでもlogout beanが成立する。§1のFramework機能変更範囲外は同梱Toolingの追加差分の範囲であり、preview.2の配布対象には既に検証済みの今回の是正を含む。
- header fragment削除の互換性注意とSession修正をpreview.2 notesへ記録する。既存preview.1は変更せず、公開はOwnerの明示指示まで行わない。手動確認環境も稼働保持する。

本書は、Claude Code cloud sessionで整理した作業方針を、**ローカルPCのAI対話session**へ引き継ぐための入口である。
実装・候補生成・検証・Release公開はすべてローカルPCで行う。cloud sessionでは本書の作成と作業branchへのpushだけを行い、
Framework本体・Tooling・POMは変更していない。

## 1. Owner指示と範囲

| 項目 | 内容 |
| --- | --- |
| 指示日 | 2026-11-12 |
| 目的 | 配布ZIPにソースJARとJavadoc JARを同梱し、案件開発者がIDEでFrameworkのsource・Javadocを参照できるようにする |
| 作業branch | `feat/e1-sources-jar-20261112`（main `f6ff36d5a89da9a962cd3d5b218ffbc1ba7857f6`から作成。本書のcommitを含む） |
| 新しい固定版 | `0.1.0-preview.2`（tag `framework-0.1.0-preview.2`） |
| 同梱対象 | ソースJAR（classifier `sources`）、Javadoc JAR（classifier `javadoc`） |
| 公開済み版 | `framework-0.1.0-preview.1`は変更・差替え・削除しない |
| 公開場所 | [README（framework-distribution）](../../build-support/framework-distribution/README.md#continue-release-work-on-a-local-pc)のとおり、tag／Release作成とasset uploadは、GitHub認証と書込権限を確認したローカルPCで行う。cloud実行環境では行わない |

範囲外：Framework機能の変更、Reference・Customer側の変更、GitHub Packages／Maven Centralへの公開、全自動公開scriptの作成
（[VSCode引継ぎ§8](phase4-e1-vscode-handoff-20261030.md#8-実案件向けzip運用の試行と手順安定化owner方針2026-11-03)のOwner方針を維持）。

## 2. 背景（cloud sessionでの観測）

別repository `zaziedlm/koiki-java-scaffold`（Framework採用アプリの土台）を`framework-0.1.0-preview.1`のZIPだけから構築した際、
次の事項はFramework sourceを読まなければ判断できなかった。ZIPにはJAR・POMしかないため、IDEでもsource・Javadocを表示できない。

- `koiki-starter-session-jdbc`の`koikiSessionLogoutCustomizer`は、アプリが`CookieSerializer` beanを定義した場合だけ登録される（`@ConditionalOnBean`）。
- `IdentityAdministration`は、ログイン済みの`FrameworkPrincipal`を操作者として要求する（未認証では`DEPENDENCY_FAILURE`）。
- Framework migrationは`koiki_flyway_history`で、アプリ側Flywayとは別に実行される。

同sessionで、ダミーの`-sources.jar`をlocal repositoryへ登録してアプリを再packageしても、Spring BootのBOOT-INF/libに
sources／javadoc JARは含まれないことを確認した（classifier付きartifactは通常の依存解決対象外）。
実行JAR・container imageへの流入防止のために利用側で追加対処は不要である。

## 3. 現行の配布構造（変更前の事実）

| 対象 | 現状 | 変更時の影響 |
| --- | --- | --- |
| `publish-unit.txt` | 14座標（POMのみ2：BOM・Parent、JAR 12） | 座標は不変 |
| `create-candidate.py` | staging reactorで`clean package`。payloadはPOM 14＋JAR 12＝26。manifest `schemaVersion: 1`、`files`はpayload 26＋`prepare-koiki.ps1`＋`README.md`＝28 | sources／javadocの生成・収集・manifest記録を追加 |
| `prepare-koiki.ps1` | `manifest.files.Count -eq 28`、bundle実ファイル`-eq 30`（＋`manifest.json`・`SHA256SUMS`）を固定照合。payload pathは`\.(pom\|jar)$`。登録は`install:install-file`（`maven-install-plugin:3.1.4`）で`-Dfile`／`-DpomFile`のみ。既登録判定はPOM＋本体JARの存在 | 件数・path規則・登録引数・既登録判定・完了markerを更新 |
| `framework-distribution-consumer/pom.xml` | Parent版`0.1.0-preview.1`固定 | `0.1.0-preview.2`へ更新 |
| `framework-distribution-verification/verify-preparation.py` | `version = "0.1.0-preview.1"`、`koiki-starter-api-0.1.0-preview.1.jar`を改ざん対象に固定 | 版を更新。classifier付きpayloadの改ざん拒否を追加検討 |
| ZIP同梱`README.md` | 「14 POMs and 12 JARs」等の記述 | 同梱内容・利用方法（IDE）を追記 |
| `koiki-parent` | source／javadoc pluginの設定なし | **変更しない**（§4.1） |

JAR 12座標のうち、`internal`以外のpackage（Public API候補）を持つのは5座標だけである。

| 区分 | 座標 |
| --- | --- |
| 公開packageあり | `koiki-architecture-contract`（`org.koikifw.architecture`）、`koiki-archunit-rules`（`org.koikifw.archunit`）、`koiki-starter-audit`（`org.koikifw.audit`）、`koiki-starter-identity`（`org.koikifw.identity`）、`koiki-starter-session-jdbc`（`org.koikifw.session`） |
| `internal`のみ | `koiki-starter-api`、`koiki-starter-data`、`koiki-starter-data-jpa`、`koiki-starter-observability`、`koiki-starter-security`、`koiki-starter-web-mvc` |
| Java sourceなし | `koiki-testing`（依存集約のみ） |

## 4. 変更方針（推奨案と判断事項）

### 4.1 生成：staging buildのCLIでgoalを追加し、Parentを変えない

- `koiki-parent`は配布payloadであり、利用側のbuild policyとして継承される。Parentへsource／javadoc pluginを追加すると、
  利用側のbuildでも生成が走る。**Parentは変更せず**、`create-candidate.py`のstaging Maven commandにgoalを追加する。
  - 例：`clean package org.apache.maven.plugins:maven-source-plugin:<版>:jar-no-fork org.apache.maven.plugins:maven-javadoc-plugin:<版>:jar`
- plugin版は明示固定する。版の選定（その時点の最新安定版の確認）と、`BUILD-BASELINE.json`等の版管理文書へ記録するかはOwner判断とする。
- Javadocは`internal` packageを除外する（例：`-DexcludePackageNames=*.internal:*.internal.*`）。Public APIだけを文書化対象とする。
- doclintで既存Javadocが失敗する場合の扱い（`-Ddoclint=all,-missing`等で緩めるか、Javadocを修正するか）を初回生成で確認し、判断を記録する。
- `--offline`実行では、追加pluginを事前にlocal repositoryへ取得しておく必要がある。

### 4.2 判断事項A：classifier付きJARを持つ座標

| 案 | sources | javadoc | 特徴 |
| --- | --- | --- | --- |
| **A1（推奨）** | Java sourceを持つ11座標 | 公開packageを持つ5座標 | 内容のあるものだけ。件数は`publish-unit.txt`等に明示した固定一覧で照合する |
| A2 | JAR 12座標すべて | JAR 12座標すべて | Maven Central慣例と同形。内容がない座標は説明だけのplaceholder JARが必要 |

`maven-source-plugin`はsourceのないmoduleでarchiveを作らず、`maven-javadoc-plugin`は文書化対象がない場合に失敗または空になる。
A2を選ぶ場合はplaceholderの作り方も決める。どちらの案でも、**どの座標に何が付くかを固定一覧として持ち、manifestとscriptで厳密照合**する。

参考件数（A1）：payload＝POM 14＋JAR 12＋sources 11＋javadoc 5＝42、manifest `files`＝44、bundle実ファイル＝46。
（A2）：payload 50、manifest `files` 52、bundle実ファイル54。

### 4.3 判断事項B：Javadocの記述量

Starterの公開型19型（identity 10・audit 6・session-jdbc 3、各`public-api.txt`）にはすべて型Javadocがあるが、method単位の記述は少ない（例：`IdentityAdministration`は16 methodに対しJavadoc block 2）。

- B1（推奨）：今回は現状のJavadocをそのまま生成・同梱し、配布経路を確立する。記述の充実（前提条件、例外、必要bean、操作者要件）は別taskとする。
- B2：今回、主要Public API（`IdentityAdministration`、`IdentityQuery`、`SessionCleanup`、Audit記録API等）のmethod Javadocを補ってから生成する。

B2はFramework sourceの変更を含むため、通常CI・Public API互換検査の対象になる。

### 4.4 manifest・準備scriptの更新

- manifestを`schemaVersion: 2`とし、`artifacts[].files[]`の各entryに`classifier`（`null`／`sources`／`javadoc`）を持たせる。
- `prepare-koiki.ps1`（preview.2同梱版）：
  - `schemaVersion 2`だけを受け付け、`1`は「その版に同梱の準備scriptを使う」旨のmessageで拒否する。
  - 件数照合を§4.2の固定一覧に合わせる。payload path規則に`-(sources|javadoc)\.jar`を追加し、想定外のclassifierを拒否する。
  - 登録は`install-file`の`-Dsources=`／`-Djavadoc=`を使い、1座標1 invocationを維持する。
  - 既登録判定・事前衝突検査・登録後hash照合・完了markerにclassifier付きpayloadを含める。
- `verify-preparation.py`：版を更新し、sources／javadoc JARの改ざん・欠落・想定外追加の拒否を確認する。
- 利用側は版ごとに同梱scriptを使う運用であり、preview.1の準備scriptでpreview.2 ZIPを扱えないことは想定どおりとする。

### 4.5 内容検査（候補生成時）

- 各sources JARに、対応moduleの`src/main/java`のsourceが含まれ、testやstaging外のfileが含まれないこと。
- 各javadoc JARに`internal` packageが含まれないこと。
- sources／javadoc JARに秘密値・local絶対path（staging directory等）が含まれないこと。
- 本体JARの内容・hashが、sources／javadoc goal追加前のbuildと同等であること（goal追加が本体JARへ影響しない）。

## 5. ローカルPCでの作業手順

### 5.1 開始時の確認

```powershell
git fetch origin
git switch feat/e1-sources-jar-20261112
git status --short --branch
git log --oneline -3
pwsh --version
java --version
./mvnw.cmd -version
python --version
gh auth status
gh repo view zaziedlm/KOIKI-JAVAWEB --json nameWithOwner,visibility,viewerPermission
```

- branchが本書のcommitを含み、`origin/main`（作成時`f6ff36d`）より後のmainの変更があれば取込み方針を判断する。
- `framework-0.1.0-preview.2`のtag／Releaseが存在しないことを確認する：`gh release view framework-0.1.0-preview.2 --repo zaziedlm/KOIKI-JAVAWEB`（存在しないことが期待値）。
- Python 3.12以上（`hashlib.file_digest`使用）。

### 5.2 段階

1. **判断**：§4.1のplugin版、§4.2のA1／A2、§4.3のB1／B2をOwnerが決め、本書§7へ記録する。
2. **実装**：`create-candidate.py`、`prepare-koiki.ps1`、ZIP同梱`README.md`、`framework-distribution-consumer/pom.xml`、`verify-preparation.py`（B2の場合はFramework source）。
3. **候補生成**：checkout外の新しいdirectoryで`create-candidate.py --version 0.1.0-preview.2`を実行する（既存の流れは[再生成・公開の記録§2](../architecture/validation/phase4-e1-regeneration-and-publication-20261103.md#2-新規候補)）。
4. **local検証**：
   - 新しい空のrecipient repositoryへ`prepare-koiki.ps1 -LocalArchive`で登録し、sources／javadocがclassifier付きで登録されることを確認する。
   - `verify-preparation.py`で改ざん・欠落・衝突の拒否を確認する。
   - 同recipientで`framework-distribution-consumer`を`package`し、Boot JARにsources／javadoc JARが含まれないことを確認する。
   - IDE（VS Code／IntelliJ）でConsumerからFramework classを開き、sourceとJavadocが表示されることを確認する。
5. **成果固定**：結果文書（`docs/architecture/validation/`）、Release notes（`docs/development/`）、`AGENTS.md`／READMEの入口を更新し、Owner指示に従ってcommit・push・PR・CI確認・mergeを行う。
6. **公開**：公開対象commit・候補hash・notesを確認後、[VSCode引継ぎ§5・§6](phase4-e1-vscode-handoff-20261030.md#5-release登録前の点検)の手順を版・tagを`0.1.0-preview.2`へ置き換えて実行する。公開後はremote取得（`-Repository zaziedlm/KOIKI-JAVAWEB`）→新しい空recipient登録→Consumer packageを確認する。

### 5.3 停止条件

- 候補生成・検証が失敗した場合、ZIPやmanifestを手修正せず、原因を記録して新しい候補を作り直す。
- 公開途中で失敗した場合、作成済みtag／Releaseを点検してから再開を判断する。自動削除・差替えをしない。
- `framework-0.1.0-preview.1`のtag／Release／assetには一切触れない。

## 6. 利用側（koiki-java-scaffold）への波及

`zaziedlm/koiki-java-scaffold`はpreview.1を固定して使っている。preview.2公開後、同repositoryで次を行う（Framework repository側の作業ではない）。

- `pom.xml`の`koiki-parent`版、`tools/koiki-distribution.properties`（版、ZIP SHA-256、同梱script SHA-256）を更新する。
- `tools/prepare-koiki.ps1`（レビュー済みcopy）をpreview.2同梱版へ差し替える。
- 空のlocal repositoryから取得→`./mvnw verify`→container buildを再実証し、IDEでsource・Javadocが表示されること、Boot JARにsources／javadocが含まれないことを確認する。

## 7. 判断記録（ローカルPCで記入）

| 判断 | 結果 | 日付・判断者 |
| --- | --- | --- |
| maven-source-plugin版 | 3.4.0（公式安定版確認、Tooling固定） | 2026-11-12／Owner了承方針に基づく実装選定 |
| maven-javadoc-plugin版 | 3.12.0（公式安定版確認、Tooling固定） | 2026-11-12／Owner了承方針に基づく実装選定 |
| A：classifier付きJARの対象（A1／A2） | A1 | 2026-11-12／Owner |
| B：Javadoc記述（B1／B2） | B1 | 2026-11-12／Owner |
| doclintの扱い | all,-missing | 2026-11-12／B1に基づく実装選定 |

## 8. ローカルPCのAI sessionへの開始指示（例）

```text
KOIKI-JAVAWEBのbranch feat/e1-sources-jar-20261112をcheckoutし、
docs/development/phase4-e1-sources-javadoc-handoff-20261112.md を読んでください。
AGENTS.mdとdocs/agent/skills/koiki-project-overview/SKILL.mdに従い、§5.1の確認結果を報告したうえで、
§4.2・§4.3の判断事項について推奨と根拠を示してください。判断後に実装へ進みます。
Release公開は私の明示指示があるまで行わないでください。
```
