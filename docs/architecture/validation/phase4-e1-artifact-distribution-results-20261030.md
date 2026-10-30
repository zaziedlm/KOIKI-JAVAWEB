# Phase4 E1：固定版asset・Maven登録・独立Consumerの検証結果

**状態（2026-10-30）：V01〜08のローカル実証は限定技術成立、結果Owner受入は未確認。通常root回帰成功。remote公開／取得と実チーム受領は未実施。**

## 1. 対象と実行根拠

Ownerは[配布方式計画§7.4](../../development/phase4-e1-artifact-distribution-options-20261030.md#74-第4項実装検証の具体計画初版2026-10-30)と、候補JARを生成→一式ZIPをローカル検証→タグに紐づくRelease assetへ登録→`gh release download`で取得する流れを了解し、「これで進めましょう」と指示した。
V01〜08の実装・ローカル実証を進める根拠として記録する。配布repositoryは後続の見直しにより既存`zaziedlm/KOIKI-JAVAWEB`（public）を使用。V09／10は未完了（§8）。commit／push・repository／tag／release作成も未実施。

| 識別 | 今回の対象 |
|---|---|
| 作業branch | `docs/phase4-s1-b1-c2-guarantee-review-20261030` |
| 基点commit | `8fe8473defe8abe66a79de461a6e3c65924498fb`、未commitのE1文書・Toolingを含む |
| Framework本体 | Java／POM／SQL／Root CIは変更なし。版変換は専用staging copyのみ |
| 候補版 | 全14座標`0.1.0-preview.1`。公開済み版ではない |
| 実行 | Codex Cloud Linux、JDK21.0.12.1、Maven3.9.16、PowerShell7.6.0、PostgreSQL17 |
| raw Evidence | `/workspace/koiki-cloud/logs/e1-20261030/`。環境内保存で、Gitへbinary・raw logを登録しない |

Repository overview／cloud-environment-runtimeの指示に従い、network policyと実行環境を確認。既存JDK・Maven・DB imageを使用した。
PowerShellは未準備のため不足を報告し、公式7.6.0 portable版を作業領域へ取得、公式checksumを照合した。SHA-256は`04517472cf57d7f9cbd93897da9bed467c73ca6063c29d7655ebc20aa1d6023f`。
公式hash一覧のUTF-16読取り、read-onlyな既定cacheの設定で初回準備が停止したが、文字コード判定と作業領域内のXDG cache／config／data設定で訂正。システムinstall・HOME変更は行っていない。
Dockerは通常sandboxで接続を拒否されたため、実行環境の権限付き手順で既存daemonと使い捨て資源を利用した。

## 2. 作成した資材

- [配布Tooling](../../../build-support/framework-distribution/README.md)：14座標inventory、staging版整合・既存testを含むJAR生成、ZIP／manifest／checksum生成、準備スクリプト。
- [独立Consumer](../../../build-support/framework-distribution-consumer/README.md)：KOIKI Parent、API／Identity／Dataと必要な推移依存、Customer Entity・Repository・migration。
- [検証Tooling](../../../build-support/framework-distribution-verification/README.md)：拒否・再利用／衝突確認、専用DB roleと有限Consumer journey・cleanup。

Root Reactorへ追加せず、Consumer・harness・fixtureは配布ZIPへ含めない。ZIPには14 POM＋12 JARと、manifest・checksum・準備script・READMEだけを収録した。

## 3. 同じ候補payloadの識別

| 項目 | 値 |
|---|---|
| ZIP | `koiki-framework-0.1.0-preview.1.zip` |
| ZIP size | 204,898 bytes |
| ZIP SHA-256 | `c3eee716a8ba8e56e5a450dbb40bf6cdb87ab20214368801bd472b1345a8ddf6` |
| 生成先 | raw下`candidate-1/` |
| source inventory SHA-256 | `b529cb88f650f3380196b9bb94a40e06ecb0bcc60e25409a2d5c8c63c359a675` |
| staging POM変換記録 SHA-256 | `00d347fb1e2f10fd6a939ab0d0e0c85943270af2f7846765496eb009ce069a6a` |

source inventoryは未commit差分を含むファイル別hashを記録。stagingでは14提供POMの版参照を変換し、配布対象だけに絞ったRoot module一覧も記録した。元commitの未変更buildとは扱わない。
候補生成後に追加・訂正した検証補助／案内の差分は候補のFramework source・payloadへ混ぜていない。公開前にscript／README等の同梱内容を変える場合は、同じZIPと扱わず、候補を更新して必要確認を行う。

## 4. 実行結果

| ID | 結果 | 確認したこと |
|---|---|---|
| V01 | PASS | source inventory・環境・Docker・既存image・独立した登録先を確認 |
| V02 | PASS | 14座標を新規生成、既存test111実行・failure/error/skip 0、43.062秒。14 POM＋12 JAR・共通版・JAR埋込み版を照合 |
| V03 | PASS | 最初は空の専用Maven repositoryへ、実POM付きで全座標を登録。必要なMaven plugin等の外部依存は通常の取得先から取得 |
| V04 | PASS／補助訂正あり | ZIP hash不一致・内部payload不一致・不正path・模擬download失敗を拒否。同一一式の再利用と既存同一座標の異内容拒否が成立 |
| V05 | PASS | 専用repositoryに登録した候補版だけで独立Consumerをbuild／package。KOIKI依存treeに旧SNAPSHOT・Framework source参照なし |
| V06 | PASS・条件付き | Customer自身の明示的EntityScanによりCustomer／Identity／Audit Entity共存、public service Bean、Repository保存／読取を確認 |
| V07 | PASS・限定構成 | 同一DataSource・public schema・分離history、Framework先行、非superuser接続、HTTP401、同じDBでの再起動・データ保持・SQL migration非重複 |
| V08 | PASS | 通常root449実行PASS＋100 skip、failure/error 0、4分46秒。所有資源cleanup・source／ZIP照合成功 |
| V09 | BLOCKED／未実施 | 現行配布先は既存`zaziedlm/KOIKI-JAVAWEB`・public。connectorの作成／upload機能不足とCLI APIアクセスForbiddenによりremote処理未実施。§7参照 |
| V10 | 未実施 | 実チームWindows／IDE・案件GitHub Actionsの受領は未実施 |

ConsumerはJUnit1 invocation、failure/error/skip 0、8 checkpoint。Maven43.539秒、依存tree取得8.784秒。
HTTP起動は候補JARを依存に持つtest application contextで実施。生成したConsumer実行JARを別processで受け入れた実証ではない。
ConsumerのDB clientは2秒間隔のsample最大3（監視接続込み）、専用role・同時pool2、DB一つで実行。所有container削除・消滅確認はPASS。

### 4.1 V04の補助ERRORと訂正

初回は4拒否確認と同一一式の再利用操作が成功した後、検証補助が完了記録JSONをbyte列で比較し、項目順の違いでAssertionErrorとなった。
配布スクリプト・候補ZIPは変更せず、補助の比較をJSONの意味の比較へ訂正。未完了の登録系2確認だけを実行して、再利用・衝突拒否がPASS。先の4拒否確認は再実行していない。
初回ERRORを保持し、6確認を一度に無訂正でPASSしたとは記録しない。模擬`gh`の失敗はremote認証・実downloadの証拠ではない。

## 5. R-05／R-06の導入条件

R-05はConsumer側にSpring Boot公開annotationの`@EntityScan(basePackageClasses = CustomerRecord.class)`を置くことで成立した。Frameworkのinternal型をimportせず、Framework側のIdentity／Audit登録と共存する。
同じ候補JAR・DBでそのConsumer設定を無効にすると、Customer Repositoryの初期化が`Not a managed type: CustomerRecord`で失敗することを対照確認した。これは期待した境界検査で、JUnitの失敗件数ではない。
今回の導入契約は「Customer自身のEntity packageを明示する」で、任意のpackageを自動発見する保証ではない。Framework本体のscan変更は不要だった。

R-06は`koiki-starter-data`をConsumerが明示し、同一DataSource／既定public schemaで確認した。Data JPAだけでmigration機構まで導入されるとはしない。
Frameworkがschemaを非空にした後にCustomer Flywayを開始するため、Consumer側にもbaseline version 0・baseline-on-migrateを明示。これは今回所有の新規DBでの構成で、任意の既存schemaへのbaseline許可ではない。
Frameworkのhistoryは`koiki_flyway_history`、Customerは`flyway_schema_history`。Customer migration内でFramework tableを参照して順序を確認した。
別schema・migration専用接続・複数DataSource・独自strategy・他DBは未実証。既存Framework strategyのschema指定やback-offの制約は残る。

## 6. 次の確認と受入範囲

通常rootは2026-10-30 08:59:05 UTCに成功終了。JUnitの報告総数549のうち100 skip、実行449、failure/error 0。専用の保留・skipを成功実行へ加算しない。
2秒間隔の112 sampleでJava process最大3、PostgreSQL最大1、Ryuk最大2、DB client最大12（監視接続込み）。連続全時点の上限証明ではない。通常rootには既存のnormal-root DB制限・single fork・非並列設定を使用し、Consumerとは並走していない。
終了時Docker稼働残留0、installer一時directory 0、installer lockなし。Framework／Root POM・CI・wrapperは実行前hashから変更なし。配布ZIP hashも生成時と一致。専用の候補・結果・recipient cacheは再確認用に環境内へ保持する。

検証終了時source一覧と配布ZIP／登録payloadの最終照合記録をrawへ保存。ローカル結果は限定技術成立で、Owner結果受入は本書の確認後に別途記録する。
ローカル限定の技術成立と、Ownerによる結果受入、Git成果固定、公開先確定、remote登録・取得、案件PC／CI受領を分けて扱う。
S1既知未達、D1結果受入未記録、個別DoDの最終受入待ちは本結果で補わない。初期版公開・本番適用・Phase4全体完了とはしない。

## 7. 履歴：専用配布先の確認・handoff（2026-10-30、§8により見直し）

Ownerは新規`zaziedlm/KOIKI-JAVA-asset`・privateを指定した。`zaziedlm` connectorのloginは確認済み（user id63864292）。当該repo取得は404で存在／アクセスを確認できない。
利用可能tool全体にrepo作成・Release作成・binary uploadはなく、network権限付きCLIの`gh api user`も`https://api.github.com/user: Forbidden`となった。環境の許可hostに`api.github.com`がない。これはprivate assetの権限不足を実証したものではなく、publicへ変更しても作成機能／CLIのAPI経路は解決しない。
Cloud内の`gh auth status`のinvalid表示はnetwork未許可の状況で得たため、connectorの認証失効とは扱わない。

同じ検証済みcandidate ZIPを保持し、PC向け`publish-initial-release.ps1`とGit用metadata・source inventory・版変換記録を含むhandoff ZIPを準備した。Framework本体や検証済みZIPは再生成・変更していない。

| Handoff | 値 |
|---|---|
| file | raw下`koiki-e1-private-release-handoff.zip` |
| size | 285,527 bytes |
| SHA-256 | `22287353b8656f61b4f500fdde312aef1b1602c789bc759465d6c066bdae0f92` |
| 内容 | `assets/`の検証済みZIP、`metadata/`のGit用案内・manifest・hash・準備script・source対応、publish script・HOWTO |

追加publish scriptの構文とOwner以外の認証主体の拒否を点検。remoteの作成／upload処理は未実証で、実行成功とは扱わない。handoffのZIPをGitへcommitする方式ではなく、scriptは`metadata/`だけをcommitする。
認証済みPCでの作成・upload・download/hash結果を受けてV09へ追記する。download後の登録・Consumer確認と実案件PC／IDE／Actions受領は別確認。repository新設・Release登録・privateからpublicへの変更はCloudでは行っていない。

## 8. 既存リモートを配布先とする見直し（2026-10-30）

Ownerは配布先を現在のリモートに設定するよう指示。`origin`のURLは`https://github.com/zaziedlm/KOIKI-JAVAWEB.git`。`zaziedlm` connectorでdefault branch main・visibility public・管理／書込権限を確認した。
既存repoのRelease assetを使い、新規`KOIKI-JAVA-asset`は作成しない。assetもpublicとなり、repoの可視性変更は行わない。
旧handoff ZIPは新規専用repo作成と専用READMEの登録を含むため使用対象外。checkout上の旧publish scriptは誤実行を避ける停止処理にした。旧ZIPのhashは履歴として保持し、既存repo向けに実行済みとは扱わない。

候補ZIPは未変更、V01〜08の技術結果は維持。公開対象source commit・tag・既存repo向け公開手順は成果固定と合わせて点検する。
connectorのRelease作成／upload機能不足とCloud CLI API Forbiddenは残る。今回remote書込・tag／Release作成・upload／downloadは実施していない。

## 9. VSCodeでの後続実行へ切替（2026-10-30）

OwnerはCloudのRelease作成／uploadを考慮対象から外し、準備後のcommit／pushとPC側での取得・継続を指示した。[引継ぎ手順](../../development/phase4-e1-vscode-handoff-20261030.md)を次の入口とする。
local実証の候補ZIPは不変。元source inventory・staging記録・原本commitのbundle・GitHub保存時のtree対応と合わせて、Gitにbinaryを入れず別packageで受け渡す。
今回のcommit／pushは成果引継ぎであり、結果Owner受入・V09／10成功・Release公開を意味しない。Cloud API制約を解除するための再調査は行わず、実Release操作と受領確認はPCで継続する。
