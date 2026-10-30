# E1-01：初期提供artifact・版識別・配布／参照更新方式の検討

**現行状態（2026-10-30）：** Ownerの進行指示に基づき§7.4のTooling・独立Consumerを実装し、V01〜08のローカル実証が限定技術成立。[結果文書](../architecture/validation/phase4-e1-artifact-distribution-results-20261030.md)に候補ZIP・test・補助訂正・R-05／06導入条件を記録。結果Owner受入、読取認証、V09／10は未完了。公開先はOwnerの見直し指示により既存`origin`の`zaziedlm/KOIKI-JAVAWEB`（public）へ変更。§8.2の配布先と§8.3のVSCode引継ぎを現行判断とし、§8.1の新規private repo案・旧handoffは採用しない。Framework本体／作業checkoutのPOM・Root CIは変更なし。以下の未実施記述は計画作成時点の履歴として読み、現在の結果は結果文書を優先する。

**作業branch：** `docs/phase4-s1-b1-c2-guarantee-review-20261030`を継続。機能保証範囲整理は`8fe8473`へcommit済み、main未統合。
Ownerは同branchでE1へ進む方針を示した。本書は[保証範囲・DoD整理§7](phase4-s1-b1-c2-guarantee-and-dod-review-20261030.md#7-現時点のエビデンスとしてのowner採用判断2026-10-30)を入力にする。
初期標準の基準一覧は[提供機能ガイド§0〜1](framework-capabilities-and-selection-guide.md#0-初期標準一覧の読み方と確定までの判断)。機能一覧を別に定義し直さない。

## 1. 提案の要点と前提

**配布先は未定。初期運用の管理負担を抑える「成果物専用リポジトリ」から、managed Maven repositoryまでを比較する。**
Ownerの最新回答（2026-10-30）は「候補を整理するところから」「成果物Jarのみ単体管理のリポ構築など、シンプルな見通し良い管理手法も考慮」。先のGitHub Packagesを主候補とする回答を更新するものとして扱う。
GitHub Packagesの先行採用や新しい配布serverの導入は前提にしない。

**現行判断：Framework本体モジュール群のRelease asset一式配布を優先候補として進める。**
OwnerはbinaryのGit履歴増加を懸念し、各モジュールを個別版管理せず、統一断面のassetとして固める方針を示した。
成果物専用repositoryのGitには案内・manifest・checksumを置き、全モジュールを同じ固定versionでまとめたZIPをRelease assetとして配布する案を具体化する。
Gitへのbinary直接格納は優先案から外す。公開先のowner／repository・可視性・取得認証は未確定。registryは将来の移行候補として残す。

E1は配布設定だけではない。Customer Entity scan（R-05）・Flyway構成（R-06）等の採用確認も含む。
必要なFramework変更が判明した場合は、理由・最小差分・影響と必要検証を示し、別commitで扱う。今回は変更しない。

## 2. 現状から確認したこと

| 入力 | 現在の状況・E1で補うこと |
|---|---|
| Root／module POMと提供機能一覧 | 候補14座標＝12 JAR＋2 POMを再照合。Root Reactor・Reference・build-supportは候補に含めない |
| [KOIKI Parent](../../koiki-parent/pom.xml) | 現版`0.1.0-SNAPSHOT`、BOM importも明示的に同版。Parentだけの版変更では不十分 |
| [KOIKI BOM](../../koiki-dependencies-bom/pom.xml) | KOIKI module・外部依存の管理を保持。案件がStarterを選び、Parent／BOMの整合を維持する |
| [Phase1b publish workflow](../../.github/workflows/publish-snapshot.yml) | GitHub Packages向けの旧9座標snapshot。現行14座標用の一般release workflowではない |
| [Phase2 publish workflow](../../.github/workflows/publish-phase2-snapshot.yml)／[unit](../../build-support/security-foundation-verification/p2-c2-publish-unit.txt) | 旧13座標の内部snapshot専用。web-mvcを含む現行14座標への適用は未確認。既存workflowをそのまま実行しない |
| [受渡し契約R1／R2](../architecture/validation/pre-phase4-p4-ar6-actual-project-handoff-contract.md#32-topology-options) | R1はmanaged repositoryの到達形、R2は固定sourceから空のlocal repositoryへstageする移行期検証。R2を配布serverと混同しない |
| [日常開発方式案](daily-development-workflow-options-20261001.md) | commit別の永続local stageは推奨候補だが、IDE／CLI・実チーム利用までの全面的な採用実証ではない |

今回の再照合はPOM・文書・workflowの静的確認。現行14座標のremote公開状態・package権限・実downloadや受領側の利用成立は確認していない。

## 3. 配布先・移行方式の比較

| 選択肢 | 適性 | 確認が必要な条件 | 今回の評価 |
|---|---|---|---|
| 成果物専用Git repository | sourceを含めず、版別のJAR・POM・manifestを一覧できる。既存Git運用を使える | 読取／書込権限、容量・版数、固定commitからの取得、過去版を変更しない運用 | 比較履歴として保持。binaryのGit履歴増加を懸念し、優先案から外す |
| 成果物専用repositoryのRelease asset（ZIP） | Maven配置の一式を版別にまとめ、取得・保管しやすい。binaryをGit履歴へ直接積まない | assetの取得認証、容量制限、上書き・削除の管理、展開・hash確認の手順 | **Owner選定の優先候補**。全モジュールを同一版のZIPに固定。Git上には案内・manifest等を管理 |
| 社内Nexus／Artifactory等 | 既存基盤が利用可能なら、複数案件・CIのMaven取得を集約できる | 利用可否、格納先、権限、network／proxy／CA、同版再deploy禁止・保持 | 既存基盤があれば比較。新設を初期条件にしない |
| GitHub Packages Maven | 通常のMaven取得。KOIKIに内部snapshot用資材・実績あり | 案件PC／CIのtoken方針、repositoryを跨ぐ読取権限、所有主体・保全 | 比較候補の一つ。Git操作の認証とは別に取得条件を確認 |
| 固定sourceからlocal stage | 一端末・移行期の開発／受入確認に使える | 端末ごとのbuild、source／version／hash固定、CLI／IDEの解決先 | 従来の暫定候補。提供側で固めたbinaryを配る方式とは区別 |

JAR中心の管理は可能。ただしJARだけではParent／BOMや推移依存が解決できないため、各JARのPOMを含める。
`systemPath`、依存情報を省略した自動生成POM、Framework sourceのCustomerへのcopyを標準参照方法にはしない。

### 3.1 GitHub Packagesを選ぶ際の取得条件

[GitHub公式Maven registry文書](https://github.com/github/docs/blob/main/content/packages/working-with-a-github-packages-registry/working-with-the-apache-maven-registry.md)と
[認証説明](https://github.com/github/docs/blob/main/data/reusables/package_registry/authenticate-packages.md)を読み取り確認した。
public packageを含めtokenによる認証が必要とされ、端末のMavenではPAT classicをsettingsに設定する説明がある。
Actionsの`GITHUB_TOKEN`は公開先repositoryとの関連やpackageアクセス許可に依存し、他のprivate repositoryのpackage取得では`read:packages`を持つPAT classic等の条件がある。

実際の案件側アカウント・package可視性・組織policyで取得できるかは未確認。
Codexの`zaziedlm` GitHub連携、shellのGit認証、Maven／案件CIのregistry認証は別に確認する。
token実値をPOM・Git・manifest・ログに入れず、publish用の書込権限と案件側の読取権限を分ける。
今回の検討からtokenの発行・設定変更・package取得・workflow dispatchを実行しない。

### 3.2 成果物専用リポジトリの最小構成案

成果物専用repositoryはFramework source repositoryとは別にする案を維持する。名称・版番号・作成先は説明例で未確定。配置案は次のとおり。

```text
Gitで管理：koiki-artifacts/
  README.md
  versions/0.1.0-preview.1/
    manifest.json
    SHA256SUMS
    release-notes.md

Release asset：koiki-framework-0.1.0-preview.1.zip
  koiki-framework-0.1.0-preview.1/
    manifest.json
    SHA256SUMS
    release-notes.md
    maven-repository/
      org/koikifw/<artifactId>/0.1.0-preview.1/
        <artifactId>-0.1.0-preview.1.pom
        <artifactId>-0.1.0-preview.1.jar  （JAR座標のみ）
```

- **提供側：** 同じbuildで検証・固定した14 POM＋12 JARを一つのZIPへまとめる。全モジュールのversionを統一し、個別モジュールだけを新版として配布しない。source完全SHA・payloadのhash・検証結果をmanifestへ記録する。
- **受領側：** §3.3の準備スクリプトから`gh release download`を実行し、指定版assetの取得・hash照合・一時展開・Maven local repositoryへの登録を行う。利用者の手動展開・Framework sourceのbuildは不要。
- **登録先：** 既定は各人の`.m2/repository`。CI・検証では明示したMaven local repositoryも選べる設計とし、後続buildも同じ場所を使う。ZIPをcacheへ直接上書きせず、付属POMを使って対象座標だけをMavenで登録する。
- **外部依存：** bundleはKOIKIの成果物だけを収録する。Boot等は許可された通常のMaven取得先から解決するため、完全offline利用は保証しない。`.m2`全体や第三者JARは一括配布しない。

ZIP内の`SHA256SUMS`は内部payload用とし、ZIP自身のhashはZIP外のGit側記録に保持する。manifestの最終項目・checksum対象と生成順は実装計画で確定する。
private repositoryのasset取得には読取認証が必要だが、GitHub PackagesのMaven registry認証とは条件が異なる。
GitにはJAR／ZIPをcommitせず、Release assetとして保持する。公開済みassetは差し替えず、一部のモジュールだけの訂正でも全体を新しい統一版として提供する。
assetの差替え／削除を防ぐ管理方法、容量制限、各端末の登録処理とCI設定は未確認。専用installerや配布serverの開発は初期案に含めない。

既存の[受渡し契約R1／R2](../architecture/validation/pre-phase4-p4-ar6-actual-project-handoff-contract.md#32-topology-options)では共通配布の到達形をmanaged repositoryとしている。
本案は今回のOwner意向に基づく**初期配布方式の追加比較案**で、既存R1成立・R2実証済みとは扱わない。採用時には受渡し契約の適用区分・手順も整合させる。

### 3.3 指定releaseの取得と環境準備（2026-10-30）

**Ownerは`gh release download`による、指定releaseの明示的な取得を採用する意向を示した。**
配布repository・release tag・asset名を固定し、最新版の自動選択や広いwildcardを使わない。以下は命名未確定の説明例で、実在する配布先・公開済み版を示さない。

```sh
gh release download framework-0.1.0-preview.1   --repo OWNER/koiki-artifacts   --pattern koiki-framework-0.1.0-preview.1.zip   --dir ./koiki-download
```

このcommandは取得部分だけを表す。案件向けの準備スクリプトは次の流れをまとめ、開発者がZIPを手で展開・登録しなくてよい形を目指す。スクリプト実装は未着手。

1. 案件が固定した配布repository・release tag・asset名・期待するchecksumを読み込む。期待hashは案件が固定した記録等から得て、最新版へ自動追随させない。
2. `gh release download`で指定ZIPを専用の作業directoryへ取得する。
3. ZIPのhash照合後に一時directoryへ展開し、manifestの版・座標一覧と内部POM／JARのhashを確認する。不一致・不足は登録前に停止する。
4. `maven-install-plugin`等のMaven登録処理で、付属する実際のPOMとJARを指定したlocal repositoryへ登録する。Parent／BOM等の参照関係に沿った登録順を検証する。推移依存を失う自動生成POMは使わない。
5. 全対象座標の登録結果を確認して準備完了とし、今回の一時directoryをcleanupする。同じ版の再実行では一致を確認し、異なる内容を検出した同一座標を無条件に上書きしない。

登録処理はKOIKI Parentがまだない案件POMを読み込まない独立した作業directoryから起動できる形にする。途中失敗を成功扱いせず、再実行と不完全登録の扱いを実装計画で定める。
外部依存・Maven pluginの取得は通常の許可された取得先を使う。この準備だけで完全offline buildを保証しない。

| 実行場所 | 取得認証と利用 |
|---|---|
| 開発PC | publicなら通常は認証不要。privateなら読取権限を持つ利用者でGitHub CLIへログインして取得。SSHでcloneできるだけではHTTPS側の認証を省略できない |
| 案件GitHub Actions | `gh`の利用可否を確認し、privateなら配布repositoryの読取認証を`GH_TOKEN`等で渡す。案件repositoryの標準`GITHUB_TOKEN`が別private repositoryを読めるとは扱わない。準備後に同じlocal repositoryで案件buildを行う |

認証情報はPOM・script・manifest・logへ記録しない。具体的な公開範囲・CI用credentialの管理は未確定。cacheは高速化用途とし、空のcacheから取得・登録・buildできることを検証する。
PCとCIで同じ版・取得／検証／登録手順を使う方針とし、対象shell・Windows対応・JDK／Maven／ghの準備方法は実装計画で具体化する。

## 4. 提供物・固定版・来歴

### 4.1 提供単位

提供候補の正式な14座標一覧は提供機能ガイド§0.2を維持する。
Maven repositoryへは、2つのPOM座標に加え、12 JAR座標それぞれのPOMも必要となる。
すなわち主payloadは**POM14件＋JAR12件**。JARだけをコピーして配布完了とはしない。
`koiki-testing`・ArchUnit等は案件のtest用途とし、全12 JARをproduction依存へ一括追加しない。
sources／javadoc classifierの追加は現行配布実績と分けて必要性を判断し、今回は必須成果物として新設しない。

Referenceの同梱SPA、S1／C2の業務例、D1／D2等のharness、seed、test認証情報・鍵は正式Framework artifactへ混入させない。
必要なら別の参照資材としてsource commit・利用条件を案内する。

### 4.2 版の候補

**初期提供版は、同じ座標を上書きしないnon-SNAPSHOTの固定versionを第一案とする。**
第2項の提案：`0.1.0-preview.1`（§7.2）。当初の説明例`0.1.0-e1.1`から、内部タスク名に依存しない命名へ見直す。これは命名候補で、未承認・POM未変更。Maven上の固定版であることと、Phase5のv1.0／support付き正式提供を区別する。
**配布単位・採用断面は全モジュールをまとめた一式とし、全提供座標に同一versionを適用する。** 各モジュールの独立した版系列は設けない。一部修正時も一式を新版にし、案件側で異なる一式のモジュールを混在させない。必要なStarterだけを依存宣言することとは両立する。
版番号は全提供座標とParent／BOM内の参照まで整合させる。各案件は固定versionを明示し、version range・LATEST・可変SNAPSHOTへの自動追随を使わない。

| 識別子 | 役割 |
|---|---|
| Git commit／tree | 公開内容のsource識別。候補をmainへ統合後、最終の完全SHAを固定 |
| Git tag／checkpoint | source断面の入口。既存checkpoint番号をMaven版番号と自動的に同一視しない |
| Maven GAV＋type／classifier | 案件が解決するbinary／POMの識別。配布先と必要認証も対応付ける |
| SHA-256・manifest | 実際に検証・配布したpayloadとsourceの対応。tagだけでbinary同一性を保証しない |

同じGAVの再build・再deployで内容を入れ替えず、修正は次の固定版へ進める。
一度生成・検証したpayloadを公開し、公開前に再buildした場合は別候補としてhash・必要な検証をやり直す。
repository側の再deploy禁止・削除権限・保持の可否は、配布先選定時に確認する。

manifestには、全座標のPOM／JAR hash・size、source commit／tree、版、JDK／Maven／Boot、build条件、検証結果、配布先、既知制限を含める。
時刻・担当主体・取得経路を記録し、秘密情報・credentialそのものは含めない。

### 4.3 暫定source stageとの使い分け

共通配布先未定の間、既存の固定commit・manifest方式によるlocal stageを利用候補として残す。
可変`0.1.0-SNAPSHOT`を使うならcommit別stageを分け、同一stageのKOIKI payloadを上書きしない。
これは暫定のsourceからの準備であり、共通の固定binary配布が成立したという意味ではない。
通常の`.m2`全体の共有・配布や、使い捨てR2のcleanupを無効にした恒常利用へ変更しない。

## 5. 案件側の参照・取り込み・更新

Customerは別Git repository・別Maven buildで管理し、Root Reactorのmodule・Framework source相対参照・internal実装依存を使わない。
asset取得認証はGitHub CLI／CIのsecret管理等で設定する。§3.3の準備スクリプトでParent／BOMを含めて登録してから案件buildを開始し、CLI／IDE／CIで同じ版・Maven local repositoryを使う。

Parentを採用する案件の依存宣言の形は次のとおり。これは候補versionによる説明例であり、現時点で取得できる座標・実行可能な導入手順として案内しない。

```xml
<parent>
  <groupId>org.koikifw</groupId>
  <artifactId>koiki-parent</artifactId>
  <version>0.1.0-preview.1</version>
  <relativePath/>
</parent>
<!-- Parentが同版のKOIKI BOMをimportする設計を維持する -->
<dependencies>
  <dependency>
    <groupId>org.koikifw</groupId>
    <artifactId>koiki-starter-api</artifactId>
  </dependency>
</dependencies>
```

社内共通Parentが必須の場合はKOIKI BOMだけのimportを候補とし、Java21 release・compiler／NullAway・Architecture Rules等の設定を何が所有するかを別途確認する。
BOM importだけでKOIKI Parentのbuild設定が適用されるとは扱わない。

更新は、現行版・manifestを保持→変更点／互換性／migration差分を確認→新しい固定版へ明示変更→案件のbuild／必要test／起動確認→受入後に採用、の順とする。
Framework提供側は版・変更点・既知制限を提示し、案件側が更新時期・採用構成と受領結果を管理する。
binary版の切戻しとDB schema／データの切戻しは別であり、破壊的migration後にPOMを戻すだけで復旧可能とはしない。

## 6. E1の最小確認と本体へのフィードバック

以下は計画時点の最小確認範囲。現在の実装・限定結果は冒頭の結果文書へ接続する。全OS・全Starter組合せ・全DB構成・全運用の網羅を先行条件にはしない。

| 既存ID | 最小確認の候補・出口 |
|---|---|
| E1-01／R-09 | 配布先・固定版・全提供座標・manifest・既知制限と対象外を確定。旧snapshot用inventory／workflowとの違いを明示 |
| E1-02／AD-01 | 空の案件用Maven local cacheで、指定assetの取得・hash照合・Maven登録を行い、Parent／BOM・必要artifactを解決。Framework checkoutのlocal installやsource相対参照に頼らず、独立Consumerをbuild／必要test／起動する |
| R-05／F6 | `com.example...`等のCustomer独自Entity／Repositoryと、採用するFramework Entityの共存・保存／読取を確認。必要なら明示登録契約またはFramework最小修正案を提示 |
| R-06／F7 | 最初の採用対象DataSource／schema構成を一つ選び、migration順・実行先・history・権限を確認。別schema・migration専用接続・strategy置換は対応条件を明記し、未検証の全構成対応にしない |
| E1-03／AD-01・02 | 案件PC／CIの取得・build・起動・診断と必要なIDE／CLI一致を受領側で確認。Cloudの成功を実チーム受入へ転記しない |
| D3との接続 | Container／ECS検証でartifact・公開契約が変われば、新版と必要な互換確認をE1へ戻す。同版payloadの上書きはしない |

ローカルで準備したfile repositoryのdry run、成果物専用repository／assetからの実取得、remote Maven registryからの取得は別の実証として記録する。
既存cache・同じ作業checkoutからの成功だけで、案件側の取得・利用成立を判定しない。
具体的なfile・command・資源／時間・cleanup・必要権限は、この案の方式選定後に実装・検証計画へ落とす。

## 7. 未確定事項と次の判断

1. **優先候補選定済み：Release asset一式配布。** 未確定は配布repository・管理主体・可視性・案件PC／CIの取得認証・asset保持の条件。§7.1参照。
2. **整理方向をOwner確認済み：** `0.1.0-preview.1`、全14座標を同一版で一式提供する案（§7.2）。開発／評価向けの初期提供とし、既知制限・Phase5のsupport条件を区別する。
3. **計画具体化へ進行：** KOIKI Parentを使う独立Consumer、PostgreSQLの同一DataSource／schema、PC／CIの確認分担（§7.3）。
4. **ローカル実装・限定実証済み：** §7.4のV01〜08は結果文書を参照。候補生成111件PASS、Consumer1 invocation／8 checkpoint、通常root449実行PASS＋100 skip。V09／10とOwner結果受入は未完了。既存snapshot workflowは未変更。

Owner選定により「全モジュールの同一版をRelease assetで一式配布」を優先候補とする。最終の公開先・版番号・公開は未確定。
第2項に続き、第3項の構成案を入力とした第4項の具体計画作成へOwnerが進行を指示した。現行の確認対象は結果文書と、§7.4のV09／10へ進むための配布先・認証条件。正式版の命名例は§7.2.1に補足。第1項の取得・権限等の残確認は保持し、第4項の実装計画へ接続する。
この検討は配布server新設・POM変更・dependency取得／build・publish・workflow dispatch・Git commit／remote操作の承認を兼ねない。

### 7.1 第1項：Release asset一式配布を優先候補に選定（2026-10-30）

**Owner判断：Framework本体モジュール群のRelease asset配布を優先候補として進める。**
当初のGit直接格納案は、binary履歴の蓄積への懸念により見直した。各モジュールを個別バージョンで管理せず、同一版の検証済み断面を一つのassetにまとめる。

| 項目 | 今回の方針 | 残確認 |
|---|---|---|
| 保管 | Gitは案内・manifest・checksum。JAR／POMはRelease assetのZIP内 | 実際の配布repository、owner／organization、可視性 |
| 版と更新 | 全モジュール同一version、一式を更新。公開済みZIPは差し替えない | 第2項の命名・収録範囲、assetの差替え／削除を防ぐ管理 |
| 提供主体 | Framework提供側が生成・検証・登録。案件側は読取・採用版管理 | 具体的な管理担当、公開用と読取用の権限 |
| 案件参照 | `gh release download`でtag・assetを明示し、準備スクリプトで照合・展開・Maven登録。必要なStarterを参照 | PC／CIの取得認証・network・登録処理の実証 |
| 将来の移行 | 必要に応じて同じGAVのMaven registry配布を検討 | 案件数・更新頻度・取得手順の負担を踏まえて判断 |

方式の最小実証候補は「指定Release assetを`gh release download`で取得→ZIPと内部payloadのhash照合→空の案件用Maven local repositoryへ登録→独立Consumerの依存解決・build」とする。
ローカルの模擬ZIPによる確認とremote assetの取得成立は区別する。Framework checkoutのinstallに頼らないことを確認する。
今回の判断は優先候補の選定であり、repository新設・公開・検証実行の完了ではない。第2〜4項は引き続き一つずつ検討する。

### 7.2 第2項：版命名・初期提供範囲の判断案（2026-10-30）

**初期案：初回を`0.1.0-preview.1`とし、Framework提供候補14座標を同一版の一式として固める。**
Ownerは正式版の命名との対応説明を了解し、次項への進行を指示した。整理方向の確認として記録し、公開版の生成・最終提供認定とは区別する。
これは案件の開発・評価に用いる固定版の提案で、Phase5のv1.0・support期間付き提供や、案件の本番適用の承認ではない。

#### 7.2.1 一つの版を各識別子へ対応させる

| 対象 | 初回の命名案 | 意味 |
|---|---|---|
| Maven version | `0.1.0-preview.1` | 全14座標に同じ固定版を設定。`SNAPSHOT`を含めない |
| 配布repositoryのrelease tag | `framework-0.1.0-preview.1` | `gh release download`へ指定する識別子 |
| ZIP asset名 | `koiki-framework-0.1.0-preview.1.zip` | 14 POM＋12 JARとmanifest・導入用資料をまとめる |
| manifestの版 | `0.1.0-preview.1` | 各payloadのGAV・hashとFramework sourceの完全SHA／treeを対応付ける |
| 次の固定断面 | `0.1.0-preview.2` | 一部モジュールだけの変更でも全体を次の同一版で提供 |

正式提供版を`1.0.0`と定める場合は、Maven版`1.0.0`／release tag `framework-1.0.0`／asset `koiki-framework-1.0.0.zip`とする。Ownerはこの対応説明を了解した。案件の開発・テスト・本番で同じ固定成果物を使い、環境ごとに版名を変えない。今回`1.0.0`の公開・本番適用を決定したものではない。

`preview.N`は初期提供断面の通番とする。番号の増加だけで互換性を保証せず、変更点・破壊的変更・migration影響を各版へ明記する。自動的な版の大小判定やversion rangeによる更新はせず、案件が版を明示指定する。
将来の正式版への移行時は命名・対応範囲・support条件を別途確定する。今回`1.0.0`への移行時期や互換性は約束しない。

既存のcheckpoint tagと新しい配布release tagは別の役割を持つ。成果物専用repositoryのrelease tagが指すcommitは案内・manifest等のcommitであり、Framework sourceのcommitとは別である。両方の完全SHAを対応付け、tag名だけで同一sourceからの生成と判定しない。
GitHub Releaseは初期版であることを示す`pre-release`として案内する案とする。`gh release download`ではtagを明示し、Latest扱いに依存しない。

現POMは`0.1.0-SNAPSHOT`のまま。既存snapshot JARのファイル名や登録時のGAVだけを変更して固定版にしない。Parent／BOM・各moduleの参照と実際の生成物内の版が整合する状態で、公開候補を生成・検証する必要がある。
具体的なPOM変更・build手順は第4項で計画する。公開候補の生成後にFramework修正が必要になれば候補と検証を更新し、公開済み版は上書きしない。

#### 7.2.2 初期収録の候補

提供機能ガイド§0.2の候補を維持し、配布内容と利用scopeを次のように対応付ける。

| 区分 | 収録候補 | 主payload／利用の位置付け |
|---|---|---|
| Build・依存基準 | `koiki-parent`、`koiki-dependencies-bom` | 2 POM。ParentまたはBOM利用の選択は第3項で具体化 |
| Architecture | `koiki-architecture-contract`、`koiki-archunit-rules` | 2 JAR＋各POM。契約宣言とtestで呼ぶ規約検査を区別 |
| Starter | `koiki-starter-api`、`koiki-starter-data`、`koiki-starter-data-jpa`、`koiki-starter-observability`、`koiki-starter-security`、`koiki-starter-audit`、`koiki-starter-identity`、`koiki-starter-session-jdbc`、`koiki-starter-web-mvc` | 9 JAR＋各POM。案件が必要なStarterを選択 |
| Test支援 | `koiki-testing` | 1 JAR＋POM。案件test用途、production runtimeへ一括追加しない |
| 一式の案内 | manifest、checksum、導入／更新案内、変更点、既知制限・検証結果への参照 | 配布版と採用条件を識別する資料。取得・登録スクリプトの同梱／配布場所は第4項で具体化 |

合計は14座標、14 POM＋12 JAR。全座標を同じ版で用意し、準備スクリプトで一式を登録する案とする。案件POMへ全JARを依存追加する意味ではない。
Root Reactor・Reference app・同梱SPA・build-support・検証harness・seed・test認証情報／鍵、第三者依存の実体は収録しない。sources／javadocの追加は初回必須にしない。

#### 7.2.3 提供範囲と保証の境界

- Java21 target／build、Boot4.1.1を現行基準とする。Java25 runtime／VTはD2の既定無効・opt-in・限定確認範囲を継承し、初期配布版そのものの成立はE1検証で確認する。
- D1の同期観測接続は限定技術成立のEvidenceを参照し、正式Collectorや運用成立の保証へ拡張しない。
- S1／B1／C2は保証範囲・DoD整理の限定受入／未達をそのまま引き継ぐ。特にS1の配送・復旧・二重送信抑止の既知未達を、asset提供によって解消済みとはしない。ReferenceのS1業務例は収録しない。
- R-05のCustomer Entity共存、R-06のDataSource／schema構成、独立Consumerでの取得・登録・build／起動と受領側確認は未完了。結果を踏まえて対応条件または収録内容を確定する。
- C3／C4・B2・MyBatis／accounting・SAML等の保留機能や、D3 Container／ECS対応をこの初回案で新たに保証しない。

一式への収録と機能の無条件利用可は別に判定する。初期収録候補を確定する際は、各moduleの現行Evidence・既知制限と、最終payloadの版整合・取得／Consumer検証を対応付ける。
本項は命名・範囲の判断案のみ。POM変更・生成・登録試験・release作成・tag作成・commit／pushは未実施。

### 7.3 第3項：参照方式・最小Consumer・確認環境の判断案（2026-10-30）

**提案：KOIKI Parentを使う独立したWeb Consumerを一つ用意し、PostgreSQLの同一DataSource／同一schema構成でR-05／06を確認する。構成案として第4項の計画へ接続する。**
Frameworkの導入成立を確認するTooling-ownedの検証資材とし、案件の業務実装・Project Template・新しい正式Framework moduleとして配布しない。

#### 7.3.1 Parent／BOMの使い分け

| 経路 | 今回の扱い | 確認すること |
|---|---|---|
| KOIKI Parentを継承 | 最初の検証経路。`relativePath`を空にし、配布一式から登録したParentを解決 | ParentとBOM・全KOIKI依存の版一致、Java21 target、必要なbuild／品質設定 |
| 社内Parent＋KOIKI BOM import | 社内Parent必須の案件向け代替経路。初回Consumerの二重構成にはしない | compiler・NullAway・Architecture検査等の所有と設定差分。案件条件が具体化した時に別確認 |

BOMのみの利用を非対応とはしないが、Parentのbuild設定が自動適用されるという説明はしない。Parent／BOM選択を理由にAD-04の保留検証を自動再開しない。

#### 7.3.2 最小ConsumerとDB構成

| 対象 | 最初の構成・確認案 |
|---|---|
| 境界 | Frameworkとは別directory・別Maven build。独立した`com.example...` package、Framework source相対参照・Root module追加なし |
| 依存 | `koiki-starter-api`と`koiki-starter-identity`を入口とし、Identityが参照するSecurity／Audit／Data JPA等を依存treeで確認。PostgreSQL driver・DB固有Flyway moduleはConsumer所有として宣言。必要なtest依存を限定追加 |
| 最小動作 | Consumer固有Entity／Repositoryを一つ設け、migration・保存／読取とWeb起動を確認。SPA・外部IdP・非同期送信・外部HTTP呼出しは追加しない。既存のdefault deny等のSecurity契約を維持 |
| R-05 | FrameworkのIdentity／Audit Entityと、別packageのConsumer Entityが同時に管理対象となり、必要なRepositoryが使えることを確認。現状の明示的`EntityScan`を静的根拠とし、実際の共存成立は未判定 |
| R-06 | 使い捨てPostgreSQL DB一つ・同一DataSource・同一の既定schemaで開始。FrameworkとCustomerのmigration実行先・順序・履歴分離、起動時と同じDBへの再起動時の状態を確認 |
| migration | Frameworkは`db/migration/koiki`／`koiki_flyway_history`、Customerは`db/migration/customer`／`flyway_schema_history`。Framework先行を、実行結果・履歴とCustomer migrationの前提確認で検証 |
| DB権限 | 検証所有の専用roleに必要なDDL／DML権限を限定。アプリとmigrationは同じ接続で確認。具体的権限・schema初期化・cleanupを第4項で定義。本番用権限設計とは区別 |
| 今回の対象外 | 別schema／複数DataSource、migration専用接続、独自`FlywayMigrationStrategy`、他DB。既存strategyのback-offとschema指定の扱いは制約へ記録し、全構成対応とはしない |

R-05はまず案件／検証側の設定で接続できるかを調べる。成立しない場合は、明示登録の導入契約かFramework修正かを、理由・最小差分・影響と検証案で判断する。Framework internal classへの依存を案件の標準手順にしない。
R-06では`baselineOnMigrate`の現行挙動を維持して確認し、単純な無効化で対処しない。同一schemaでの成立を別schema構成へ一般化しない。

#### 7.3.3 開発PC／CIと受入の分担

| 場所 | 最小確認の案 |
|---|---|
| Codex Cloud | 隔離したMaven local repositoryと所有された使い捨てDBで、候補一式の登録・依存解決・Consumer build／test／起動とR-05／06を確認。ローカルZIPの確認とremote asset実取得を区別 |
| 案件開発PC | 初期対象は既存のWindows／PowerShell利用を想定。`gh`のログイン・指定版取得・準備スクリプト・CLI build、IDEで同じ版／local repositoryを参照できることを確認。実チームのshell・JDK／Maven条件は確認が必要 |
| 案件GitHub Actions | 最初の検証候補はLinux runner＋Java21。cacheなしから`gh release download`・hash照合・登録・同じConsumerのbuildを確認。private配布時は配布repositoryへのCI用読取認証を個別設定 |

空cacheでの確認は専用directoryで行い、利用者の既存`.m2`を削除しない。PCとCIで同じrelease tag・asset名・版・hashを使う。Java25／VTは既存D2のEvidenceを引き継ぎ、Consumerへ別のVT検証一式を自動追加しない。
Cloudの技術成立、案件CIの取得成立、実チームPC／IDEの受領確認を別々に記録する。未実施環境は未確認のまま残す。

次は第4項で、必要file・準備スクリプト配布場所・version整合・有限の検証集合／資源・cleanup・実行順を計画する。remote repository新設・release公開とローカル実証は分けて定める。
本項は構成の判断案であり、Consumer／script／workflow／POMの作成・改変や動的検証は行っていない。

### 7.4 第4項：実装・検証の具体計画初版（2026-10-30）

**計画作成時点：Ownerの指示により具体計画を作成。以下はその計画。後続の進行指示によりV01〜08を実装・検証済み（冒頭の結果文書参照）。remote公開は未実施。**
初期実証はローカルの候補一式で行い、配布先未定でも進められる範囲を先に成立させる。remote実取得・案件PC／CI受入は別の出口として残す。

#### 7.4.1 作成対象と所有

| 配置案 | 作成する資材と役割 |
|---|---|
| `build-support/framework-distribution/` | 配布対象14座標のinventory、候補生成・ZIP化、manifest／checksum生成・点検、導入案内と準備スクリプトの正本。既存snapshot inventoryは履歴として維持 |
| 同directory内の`prepare-koiki.ps1` | PowerShell 7共通スクリプト。配布repository・tag・asset名・期待ZIP hash・登録先を入力。`gh release download`→照合→一時展開→実POMによるMaven登録→完了記録。検証用に明示的なローカルZIP入力も設ける |
| `build-support/framework-distribution-consumer/` | §7.3の独立Consumer。Parentの`relativePath`は空、Root Reactorへ登録しない。固有Entity／Repository、migration、最小Web起動、共存・DB確認の限定test |
| `build-support/framework-distribution-verification/` | 所有directoryでの有限実行・取得境界の代替fixture・checksum拒否・cleanup確認。正式artifactへ混入させない |
| `docs/architecture/validation/phase4-e1-artifact-distribution-results-20261030.md` | 実行開始時に結果票を作成。source／生成差分／payload／実行条件・成功／失敗・cleanup・未実施段階を記録 |
| 関連guide | 受入後に導入／更新の入口を正本へ接続。既存受渡し契約のR1／R2に対する追加方式を明記 |

PowerShell 7はWindowsとLinuxで同じscriptを使う案。Windows PowerShell 5.1は初回対応に含めない。`pwsh`・`gh`・Java21・Maven3.9.16の存在／versionをpreflightで確認し、不足は自動インストールせず報告する。
準備スクリプトはZIP内にも同梱するが、初回取得用には成果物専用repositoryのGit側に、対応版のscriptと使用方法を置く案とする。案件はそのcommitを固定して取得・保持し、script自体も固定hashで識別する。ZIPの中のscriptを取得前に使う循環を作らない。
案件CI例は最初は文書snippetとし、案件repositoryのworkflow変更・Root CIへの新job追加はこの段階で行わない。

#### 7.4.2 版整合と候補生成

1. 実装・必要な訂正後のFramework sourceの完全commit／treeと、今回のTooling差分を固定する。候補生成先は専用staging copyとし、作業checkoutの既存POMを配布版へ一括置換しない。
2. 選定14座標のPOMと、それらのParent／BOM内のKOIKI参照を候補版`0.1.0-preview.1`へ整合させる。stagingの変更対象・生成差分・hashをmanifestへ記録する。単なるJAR名変更や登録時だけのGAV変更は行わない。
3. Rootのmodule一覧から選定14座標を明示したMaven buildを行う。Reference・build-supportは配布build対象へ追加しない。Parent／BOM・推移依存・JAR内のMaven情報に古いKOIKI SNAPSHOT参照が残らないことを検査する。外部依存の版は変更しない。
4. そのbuildで生成した14 POM＋12 JARだけをMaven配置へ収集し、ZIPと内部manifest・payload checksumを生成する。Git側記録用にZIP hashを別途生成する。秘密情報・fixture・Reference混入も点検する。
5. 候補ZIPを検証に使用し、公開用に再buildしない。失敗して修正・再生成した場合は候補hashを更新し、旧失敗・旧候補との対応を保持する。検証済みpayloadをそのまま公開する。

生成版は「元source SHA＋staging版整合差分＋生成条件」で識別する。元のSNAPSHOT source commitから未変更で生成した、と誤記しない。
Maven pluginは具体実装時にversionを固定する。登録はBOM→Parent→内部依存順を基本にPOM参照を照合し、案件POMを読まない独立directoryから`install-file`を実行する。
全座標の照合完了時だけ準備完了記録を出す。同一版の同一payloadは再実行可能とし、不一致は停止する。Maven登録は一括transactionではないため、途中失敗時には未完了を表示し、既存`.m2`の無関係な成果物を削除しない。

#### 7.4.3 検証集合と順序

| ID | 段階と確認対象 | 成立の証拠 |
|---|---|---|
| E1-V01 | preflightとsource固定 | branch／commit／差分、Java／Maven／pwsh／gh、依存取得経路・Docker・DB image、所有directoryと空き容量を記録。秘密値は出力しない |
| E1-V02 | 候補一式のbuild・整合検査 | 選定moduleの既存test、14座標・14 POM＋12 JAR、共通版・内部参照・JAR内容、manifestとZIP hash |
| E1-V03 | ローカルZIP→空local repositoryへの準備 | 付属POMによる全座標登録、依存情報保持、完了記録。既存cacheを隠した専用directoryで成立 |
| E1-V04 | 準備処理の拒否・再実行 | ZIP改変／内部payload不一致は登録前拒否、同版同一内容の再実行成立、同版別内容・取得失敗は成功扱いしない。最小の自動testで確認し、実credential障害は注入しない |
| E1-V05 | 独立Consumerの依存解決・build | 配布一式を登録したlocal repositoryのみからKOIKI依存を解決。別sourceのinstall・相対参照・SNAPSHOT混在なし。依存tree・build結果を記録 |
| E1-V06 | R-05のEntity／Repository共存 | Consumer Entityの保存／読取、Identity／Audit Entityの管理対象への登録と必要Repositoryの生成を確認。Framework内部型への案件依存で回避しない |
| E1-V07 | R-06とWeb起動・再起動 | 同一DBのFramework→Customer migration順、二つのhistory、実行先schema、Consumer操作、同じDBで一回再起動して重複migrationなし。既存default denyを維持 |
| E1-V08 | cleanup・既存通常経路への影響確認 | 今回所有の子process／container／directoryを片付け、残留とsource差分を確認。版変換をstagingに閉じたことを確認。通常root回帰は下記の条件で実行 |
| E1-V09 | remote Release assetの実取得 | 公開先・読取主体確定後、指定tag／assetを`gh release download`で実取得。同一ZIP hashと登録・Consumer build成立を確認。ローカルfixtureの成功では代用しない |
| E1-V10 | 案件PC／CIでの受領 | Windows／PowerShell 7のCLI・IDEと案件Linux CIで、固定版取得・登録・build。CIはcacheなしで成立。実チーム結果とCloud結果を別記録 |

V01〜08をローカル実証、V09をremote取得、V10を受領側確認とする。段階ごとに未実施を明記し、V01〜08成功だけで「案件配布完了」にしない。
V04は取得境界の有限fixtureを使用し、実際の`gh`通信はV09で確認する。R-05／06で問題が出た場合はその時点の失敗を保全し、Consumer設定による接続案またはFramework最小修正案を提示する。
通常root回帰は、初回ローカル実証の最終確認として一回行う。これは元source／Tooling追加による既存経路の回帰で、候補版14座標の検証はV02・V05〜07で別に扱う。Framework本体の訂正が必要になった場合は、差分に応じた局所回帰と最終rootの実行時点を見直し、反復を自動追加しない。

#### 7.4.4 実行範囲・資源・停止とcleanup

- ローカル実証は初回一回、preflight・候補生成・通常rootを含め合計100分以内（最後の10分をcleanup用に確保）を計画上限とする。実測見積は実装差分とpreflightで見直す。超過・追加再実行は結果と原因を示して判断する。
- 子JVMは同時4まで、PostgreSQLは`postgres:17-alpine`の使い捨てcontainer一つ、TestcontainersのRyukは同時2まで、DB client接続は合計14まで。通常rootは既存の有限設定を維持し、独立Consumerとは並走させない。全体上限を満たせない場合は起動前に計画を見直す。
- DBは今回所有の使い捨てDB・専用role・既定schemaに限定する。管理接続は初期化とcleanup用、アプリroleは当該DBの接続・当該schemaのUSAGE／CREATEと自己所有table／sequenceのDDL・DMLに限定し、superuser・他DB・他schemaへの権限を付与しない。Framework／Customer migration後の必要権限は実行結果で確認する。
- Web／DB portはloopbackに限定し、空きportを割り当てる。外部provider・本番DB・実案件データには接続しない。container image／Maven依存の不足があれば取得経路・必要対象を示し、利用可能な環境設定と実行権限の範囲で準備する。
- staging source・ZIP・local repository・DB／container・子processは今回の所有を識別する。既存`.m2`、既存container、無関係な作業directoryを削除しない。cleanupは終了確認後に行い、結果・hash・有限logを保全する。秘密値は保存しない。
- hash／版不一致、想定外のFramework差分、資源上限超過、DB接続先不一致、権限不足、cleanup不成立では後段へ進まず記録する。remote操作・workflow dispatchをローカル実証の代替にしない。

数値上限は本計画の初期案で、現時点の実行許可や実測値ではない。具体的test method・file一覧・起動command・期限付き停止／cleanupは実装着手時に固定し、予算を過去のD1／S1から流用しない。

#### 7.4.5 公開・受入へ進む条件

1. ローカル実証の結果・制限・必要な訂正を確認し、候補payloadとsource対応を固定する。結果の受入判断と文書／Toolingのcommitは区別する。
2. 配布repositoryのowner・名称・可視性・管理担当、PC／CI用読取方法、公開assetの差替え／削除を防ぐ管理、初回release版・tagを確定する。
3. 成果固定後、個別のremote操作指示に基づきrepository新設・Git側案内の公開・pre-release／asset登録を行う。現段階で公開済みとはしない。既存snapshot publish workflowはdispatch・改変しない。
4. V09、V10の結果を取得する。公開先や実チーム入力が未確定なら、その段階を未実施として残し、ローカル技術成立から勝手に補わない。

計画確認後の次の作業候補は、V01〜08に必要なTooling・最小Consumerと有限検証の実装。Framework本体修正・公開先新設・正式support整備・D3の先行実装は含めない。

## 8. ローカル実装・検証後の再開点（2026-10-30）

[結果文書](../architecture/validation/phase4-e1-artifact-distribution-results-20261030.md)にV01〜08を記録。候補版は`0.1.0-preview.1`、全14座標同一版の14 POM＋12 JARを生成・照合した。
取得・登録scriptは[配布Tooling](../../build-support/framework-distribution/README.md)を正本とし、PowerShell7共通、tag／asset固定、hash照合後の実POM登録を実装。準備スクリプトはローカルZIPで実証し、実際のGitHub downloadは未実施。
R-05はCustomer自身の`@EntityScan`で共存が成立。R-06はData Starterを明示し、同一DataSource／既定schema・分離historyの限定構成で成立。Framework本体修正なし。
次は結果確認と成果固定対象の点検、配布repositoryのowner／名称・可視性・読取認証の確定。remote公開・V09、案件受領V10を別に追跡する。既存のDoD未達・保留事項は維持する。

### 8.1 履歴：新規専用配布先の指定と操作制約（2026-10-30、§8.2により見直し）

Ownerは新規`zaziedlm/KOIKI-JAVA-asset`、所有者`zaziedlm`、privateを指定した。回答中の`zazidlm`は、併記されたfull name・既存GitHub連携に合わせた表記として扱う。
支障時publicの意向も示されたが、今回の制約はprivate読取権限ではなく操作機能／networkであるため、公開範囲は変更しない。

- `zaziedlm` connectorのloginは確認済み。指定repoのmetadata取得は404で、存在／アクセスは確認できない。
- 現行connectorの全toolを探索したが、repository作成・Release作成・binary asset uploadは提供されていない。
- Cloud CLIのnetwork権限付き`gh api user`も`https://api.github.com/user: Forbidden`。環境policyに`api.github.com`の許可はない。sandbox内`gh auth status`のinvalid表示だけで、OwnerのGitHub連携が失効したとは判定しない。
- このためrepo／Release／tag作成・asset upload・remote実downloadは実行できていない。別アカウントや別経路による迂回は行わない。

認証済みPC向けの[初回publish script](../../build-support/framework-distribution/publish-initial-release.ps1)を追加。対象repo・private・候補版／ZIP hashを固定し、同じ検証済みZIPとGit用metadataを環境内handoff ZIPへまとめた。
scriptは`zaziedlm`認証を確認後、private repo作成または確認→metadataのみcommit／push→annotated tag→pre-releaseと既存ZIPのupload→`gh release download`とhash照合を行う。Git全体の認証設定・可視性は変更せず、既存releaseは上書きしない。
scriptは構文・誤った実行主体の拒否を点検したが、remote処理自体は未実証。途中失敗時はmetadata／tagが作成済みの場合があるため、状態を確認して再開する。
PCでの実行後も、downloadしたbytesからのMaven登録・Consumer確認と案件PC／IDE／CIの受入は別に記録する。Cloud側のFramework source branchのcommit／pushは今回実施していない。

### 8.2 現行判断：既存source repositoryのReleaseを配布先にする（2026-10-30）

Ownerは「配布先は、現在のリモートリポに設定しましょう」と指示した。ローカル`origin`と`zaziedlm`連携のmetadataを照合し、配布先を既存`zaziedlm/KOIKI-JAVAWEB`へ変更する。
GitHub側のdefault branchは`main`、visibilityは`public`、連携主体の管理・書込権限は確認できた。Release assetもpublicになる。repositoryの可視性は変更しない。

- 新規`KOIKI-JAVA-asset`は作成しない。既存のsourceと同じrepoで、`framework-<version>`のrelease tagとZIPを管理する。JAR／ZIPはGit履歴へcommitしない。
- 準備scriptの取得先は`-Repository zaziedlm/KOIKI-JAVAWEB`とする。script自体をsource repoの固定commitから得られ、専用repoの別checkoutは不要。全モジュール同一版・一式ZIP・hash照合・Maven登録は維持する。
- 例えば`gh release download framework-0.1.0-preview.1 --repo zaziedlm/KOIKI-JAVAWEB --pattern koiki-framework-0.1.0-preview.1.zip`と指定する。これは未公開の命名例で、現時点でdownload成立とはしない。
- 配布タグは対応するFramework sourceの確定commitへ付ける。staging版変換と生成payloadはmanifest／hashで対応付ける。候補は未commit Toolingを含むため、sourceの成果固定・remote反映と公開対象commitを先に点検し、tag対象を推測しない。
- source repoのREADMEを専用配布用READMEへ置換しない。配布案内は既存docs／build-supportから接続する。

既存repoの再利用で新設は不要になるが、現行connectorにRelease作成／binary uploadのtoolがないこと、Cloud CLIのAPI経路Forbiddenは解消しない。remote公開・実取得は未実施のまま。
§8.1の旧handoff ZIPは新規専用repo作成を含むため使用しない。作業checkoutの`publish-initial-release.ps1`は誤実行を防ぐ停止処理へ変更した。既存repo向けの公開手順はsource断面とtag対象を確定してから用意する。
検証済み候補ZIPは変更・再生成していない。V01〜08の結果は維持し、公開先が変わったV09と受領側V10を引き続き確認する。

### 8.3 現行の進め方：commit／push後にVSCodeで継続（2026-10-30）

OwnerはCloudのRelease作成・uploadを対象外とし、準備後にこのbranchをcommit／pushして、ローカルVSCodeで取得・継続する方針を示した。
[VSCode引継ぎ手順](phase4-e1-vscode-handoff-20261030.md)へbranch取得、検証済みZIPの別受渡し、前提確認・Maven登録、source／公開commit対応、Release登録とremote実取得のcommand例を整理した。
今回のcommit／当該branch pushはこの明示指示に基づく。PR／main更新・tag／Release作成・uploadは行わない。CloudのAPI権限・network設定の再調査は継続条件にしない。
候補ZIPは不変のまま、元manifest／source inventory・staging変換記録とともにGit外のPC継続packageへまとめる。GitHub連携APIの保存でcommit SHAが異なる場合はtree一致と原本SHA対応を記録し、原本Git bundleも保持する。
結果Owner受入、V09／10・正式提供版・support判断は未完了のまま。PCでは公開対象commitと元候補の内容対応を確認してから公開する。
