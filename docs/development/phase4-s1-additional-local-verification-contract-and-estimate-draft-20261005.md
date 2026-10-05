# S1追加局所検証契約・再見積案（2026-10-05）

**状態:** A単位のfixture作成・実行・契約上限はOWNER APPROVED（2026-10-05、§6.1）。B／CはDRAFT・個別開始判断待ち。正式Tier採用・正式Reference変更の承認ではない。本日は判断記録まで、実作業は次回開始する。
**位置:** [後続タスク](phase4-s1-follow-up-tasks-after-local-verification-20261005.md)順3、ST-B追加のTooling-owned検証候補。既存[方式票§8.3](phase4-s1-method-ballot-and-local-verification-contract-20261005.md#83-owner方針レビュー承認記録2026-10-05)の方針承認、commit `5252124`の初回L1〜4を入力とする。
**目的:** 実JPA／制限role、既存Identity／Audit Public契約、目的別構成の不足を小さな順次単位で確かめ、正式接続の判断材料にする。局所PASSから正式Reference・DoD／Gateを認定しない。
**Owner確認（2026-10-05）:** 本書の確認と、実装前コミットの区切り・残作業確認の指示を受領。契約案の確認をA／B／Cの作成・実行・上限の開始承認へ読み替えない。次の判断対象は§6のA単位。

## 1. 個別開始単位と変更対象

| 単位 | 範囲と出口 | 予定変更／前置条件 |
|---|---|---|
| A：JPA保存 | 2種類記録の実JPA SQL・flush／lock・用途別role適合。第一開始候補 | 新test `S1JpaPermitBoundaryTest`、test用モデル／明示Configuration・専用SQL。既存Tooling POM／Framework／Reference source変更なし。A成立前にBへ進めない |
| B：Identity／Audit | 実IdentityQueryと実Public Recorder、消費transaction・拒否・失効。Aのmodelを利用 | 新test `S1IdentityAuditBoundaryTest`と専用support／resources。Tooling POMに限定profile・test依存を追加する候補。依存差分・artifact解決・grant／migration入力を開始前review |
| C：Web／mode接続候補 | 実認証／CSRFと観察／確認終了の分離、復旧構成の実DB・proxy・起動副作用、不明のcontext再起動引継ぎ | 新test `S1AuthenticatedPermitWebTest`／`S1DatabaseModeBoundaryTest`と専用support。Bの依存に加え、Web testに必要な既存artifact／test依存を照合して個別review。別JVM／実OS crashは含めない |

新classは説明用の予定名。pathは`build-support/phase4-level2-verification/src/test/java/org/koikifw/buildsupport/phase4/`、専用supportは同rootの`s1fixture/`、resourcesは`src/test/resources/s1-additional/`、証拠は`target/s1-additional-evidence/<run-id>/`と実行日付の`docs/architecture/validation/`とする。

test Configuration／Entity／Repositoryは明示登録し、既存ProbeApplicationのscanへ混入させない。初回L1〜4・専用SQL・保存証拠は維持し、新SQLは制約を弱めず差分を追跡する。Root Reactor・production source・正式migration・Framework Public API／Rule・CI／remoteは対象外。既存test用secret／roleを正式成果物へ転用しない。

**依存のsource確認:** 現行[Tooling POM](../../build-support/phase4-level2-verification/pom.xml)にBoot Data JPA／JDBC／Test／TestcontainersとModulith JDBC profileが存在する。`jdbc`はpublication storeの選択であり、JPAモデル検証だから`jpa` profileへ切り替える必要はない。Audit／Identityは直接依存にない。[Identity POM](../../koiki-starters/koiki-starter-identity/pom.xml)はSecurity／Data JPA／Auditへ依存するため、B以降はServlet関連の混入も照合する。

Bのprofile名は仮に`s1-contract`とする候補で、`koiki-starter-audit`／`koiki-starter-identity`のtest依存と実効treeをreviewする。CのWeb／Security test依存は既存正式座標を照合してから列挙する。必要artifactのcache存在／checksumは開始preflightで確認し、欠落なら停止。download／install／publishを本票へ自動追加しない。Framework内部classを直接importせず、通常のauto-configuration発見とPublic Bean契約を使う。

## 2. 必須test枝と合否

下表は必須枝ID。class作成前にmethod名・parameter invocation・期待件数へ対応付け、実行後XMLと照合する。行数をtests数として扱わず、skip／未作成は未達とする。

| 単位／枝 | 必須確認 | 合格証拠／不成立時 |
|---|---|---|
| A1 発行／再読取 | web loginで実JPA INSERT、commit後別connectionで不変target／actor・期限を確認 | 発行列のみのSQLと保存内容。owner権限へ切替えてPASSにしない |
| A2 lock／更新 | worker loginでpermit row lock・必要version更新、webで確認列更新 | PostgreSQLの許可列権限内でflush／commit。immutable列をUPDATEするなら配置／Adapter方式へREWORK |
| A3 消費一意／改変拒否 | 実JPAの消費INSERT、同permit／operation再使用拒否、消費UPDATE／DELETE拒否 | SQLSTATEと実DB件数。EntityのAPI非公開だけをDB保護の証拠にしない |
| A4 競合／rollback | 2connectionで同許可消費・同対象発行、確認／消費の両順序、消費transaction rollback | barrier／実lock待ち、勝者1・敗者送信0、rollback後再読取。旧L2は比較入力で新JPA証拠とは別 |
| A5 観察権限 | reader loginで対象読取、lock／更新拒否 | 実roleで拒否し、観察に更新権限を足さない |
| B1 現在Identity | 実IdentityQueryのACTIVE・DISABLED・権限除去・不存在、照会失敗 | 現在情報と拒否結果。fixture user IDの入力を本人認証PASSとしない |
| B2 条件拒否 | 期限境界、environment／target／attempt差異、閉鎖／消費済み、scope外 | 送信probe0、消費不増加、scope外情報を返さない。コード／期限はtest専用値として明記 |
| B3 Business Audit | 発行／消費／閉鎖と実BusinessAuditRecorderが同時commit | 実Audit内容・actor・resource、commit前別接続で双方未可視。別transactionに分離しない |
| B4 Audit失敗 | Audit INSERT拒否等をtest専用roleで注入、flush／commit失敗 | 消費rollback・送信0。audit_probe／mockRecorderでは代替しない |
| B5 Security Audit | 認可拒否と実SecurityAuditRecorder、外側transaction rollback、Audit障害 | 拒否記録が別transactionで残る。記録失敗でも拒否維持。有限poolと待機を観測 |
| B6 失効窓 | 現在権限確認後に権限を除去し、選んだ送信前再確認経路が拒否 | 消費前なら消費0、消費後なら消費保持・送信0／保留。全失効窓の原子的解消をPASSにしない |
| C1 実Web本人認証 | test専用userで実認証経路・Session・SecurityFilterChainを通し許可発行 | 認証済みactor保存。`@WithMockUser`／mockprincipalだけで合格にしない。外部IdPなし |
| C2 Web負例 | 未認証／権限なし／scope外／CSRFなし、actor入力改変 | 拒否・DB不変。許可／確認終了と閲覧を別能力で確認 |
| C3 確認mode | 実DBで起動／閲覧／event投入／終了、送信可能Bean／runner不在 | publication／attempt／permit／消費不変・送信0。Session／拒否Auditの必要更新は別snapshotで説明 |
| C4 復旧mode | 実registry／listener proxyと対象ID、欠落／誤mode拒否、通常経路／scheduler抑止 | 起動だけで送信0、明示実行のみ指定対象、終了まで結果採取。実Referenceのscan回帰はST-Cへ残す |
| C5 不明引継ぎ | DBと運用証拠を保持してApplication contextを閉じ、別contextで再開。commit／rollback両方、不明証拠・証拠欠落の拒否 | 自動再送／取消／次許可なし。人の突合まで保留。同一JVM context再起動であり実process終了とは区別 |

送信は全単位でtest専用probeのみ。実provider受理／冪等性・実trace／exporter、package済みReference・別JVM・OS crash・E01〜21は別範囲。B／Cに必要なpermission・actor分類・失効基準時点・mode物理構成が決まらなければその枝はBLOCKEDとし、便宜的な仮契約で正式判断を閉じない。

## 3. 実行手順案

1. 対象単位の**作成と実行**の開始判定、branch／HEAD／dirty差分、先行結果を確認。変更source／SQL／POMをchecksum固定し、必須枝と期待件数を一覧化する。
2. Java 21・Wrapper・Docker Server・固定DB digest、空き資源とartifact解決を再preflight。B／Cは追加profileのeffective POM／dependency treeとauto-configuration一覧を保存し、初回baselineとの差分を照合する。初回環境の数値を無確認で現在値として使わない。
3. 管理credentialで使い捨てDBのDDL／roleと必要testデータを準備し、runtimeは限定loginへ切り替える。hibernate DDL生成とruntime Flyway自動実行を無効化し、validate／読取起動が制限roleで成立するか確認する。B／CのFramework schemaはartifact内既存migrationを隔離DBへ明示適用する方法を先にreviewし、正式SQLをfixtureへ複製しない。
4. 下記classを一つずつ実行し、終了コード・XML・前後DB／role／Audit／送信probe・Bean一覧・失敗／保留を保存する。Aの不成立を解消するまでB、Bの不成立を解消するまでCへ進めない。
5. 必須枝とXMLの件数を突合。既存context／POMへの影響に応じた回帰を行い、Evidenceへ未検証範囲と戻り先を明記する。cleanupは当該container／connection／証拠だけを対象とし、保留証拠は削除しない。

以下はclass作成後・承認後の予定commandで、今回は実行していない。Repository rootから実行。Aは現行POM、B／Cはprofileの追加・実効確認後に限る。

```powershell
.\mvnw.cmd -o -f build-support/phase4-level2-verification/pom.xml -Pjdbc "-Dtest=S1JpaPermitBoundaryTest" "-DforkCount=1" "-DargLine=-Xmx768m" "-Djunit.jupiter.execution.parallel.enabled=false" "-Dsurefire.failIfNoSpecifiedTests=true" test
.\mvnw.cmd -o -f build-support/phase4-level2-verification/pom.xml -Pjdbc,s1-contract "-Dtest=S1IdentityAuditBoundaryTest" "-DforkCount=1" "-DargLine=-Xmx768m" "-Djunit.jupiter.execution.parallel.enabled=false" "-Dsurefire.failIfNoSpecifiedTests=true" test
.\mvnw.cmd -o -f build-support/phase4-level2-verification/pom.xml -Pjdbc,s1-contract "-Dtest=S1AuthenticatedPermitWebTest" "-DforkCount=1" "-DargLine=-Xmx768m" "-Djunit.jupiter.execution.parallel.enabled=false" "-Dsurefire.failIfNoSpecifiedTests=true" test
.\mvnw.cmd -o -f build-support/phase4-level2-verification/pom.xml -Pjdbc,s1-contract "-Dtest=S1DatabaseModeBoundaryTest" "-DforkCount=1" "-DargLine=-Xmx768m" "-Djunit.jupiter.execution.parallel.enabled=false" "-Dsurefire.failIfNoSpecifiedTests=true" test
```

実効argLine／javaagentが変われば上書きせず合成して票を更新する。`test`は既存Failsafe IT／別JVM実演を実行しない。B／Cでprofileによるtest compile／既存contextへの影響があるため、L1〜4と影響する既存testを回帰対象として開始前に列挙する。不要なmodule全体verifyを無条件に追加しない。Docker pipe／Maven cache権限エラーはAGENTS.mdと実行環境の同じ最小操作の承認手順に従う。

## 4. 資源・時間・停止条件案

下表のAに適用する資源・待機・再試行・保持・費用条件は**OWNER APPROVED（2026-10-05）**。4class確認枠40分は全体の案であり、今回のA単独は1class10分を適用する。B／Cへの適用は未承認候補。開始前preflightで成立を再確認する。

| 項目 | 候補上限／採取 | 超過・不足時 |
|---|---|---|
| 同時資源 | Maven1・test fork1・PostgreSQL1＋管理Ryuk。競合はfork内2worker。class間は逐次 | 別JVM／collector／独立providerは追加しない |
| memory／CPU | fork heap768 MiB、DB memory1 GiB／CPU1、端末available memory4 GiB以上・空きdisk10 GiB以上 | B／CのWeb／JPA起動不足はOOMを隠さず再見積。ピーク値を採取し容量PASSとは別扱い |
| connection | pool最大4、競合／観測を含めDB同時8以内、DB max_connections16 | Security REQUIRES_NEWと外側transactionの同時使用を実測。複数poolは合計を守り、接続待ちなら設計へ戻る |
| data／出力 | publication1実演20件、test userは必要枝分の有限集合、run出力合計1 GiB | log／SQLはPII／secretを除外。image／Maven cacheは別台帳 |
| 待機 | lock／barrier10秒、listener30秒、1class10分、4class初回確認枠40分 | build／image待ちは別計測。timeoutで未送信／停止を認定せず、残worker／DBを突合 |
| 再試行／保持 | 原因未変更rerunは1回まで、証拠は結果確認まで保持・確認窓7日候補 | 同じ不成立を反復しない。保留証拠は期限でも自動削除しない |
| 費用／通信 | 有料service追加0円、外部IdP／provider不要 | 実認証testは隔離test user。既存構成の外部通信・password checkerは開始前に確認し、正式認証を弱める設定へ変更しない |

## 5. 再見積（技術作業の低確度案）

実測工数ではなく、必要な実接続・負例・証拠採取から分けたレビュー用の幅。Ownerレビュー稼働・環境／承認待ち・正式Reference実装・統合実演は含めない。過去の初回16〜32時間・拡張200〜392時間からの算術差分ではない。

| 単位 | 技術作業候補 | 見積根拠／再判断点 |
|---|---|---|
| A | 8〜16標準時間 | testモデル／DDL・制限role、JPA SQL／lock、競合・証拠。4時間時点でflush／列権限の成立を確認し、不成立なら方式reviewへ戻る |
| B | 16〜32標準時間 | dependency／schema・実Identity／Recorder、transaction／失効枝、回帰・証拠。8時間時点でPublic Bean／同transaction接続を確認 |
| C | 12〜24標準時間 | 実Web認証／CSRF、実DB mode／proxy・context再起動保全、回帰・証拠。6時間時点でmode分離と外部通信不要の成立を確認 |
| 追加局所の合計 | 36〜72標準時間 | 順次実行の技術量の合算。現在の全S1残量・予算・納期ではない |

各上限は完了保証ではなく再判断点。単位上限到達・新library／別process／Framework変更・想定外の権限要件が必要なら、原因・必要差分・残量を示して停止／再見積する。見積はA実測後にB／Cを更新し、総量を既存WBSへ二重計上しない。追加管理台帳・運用UIへ広げる変更は別レビュー。

## 6. Owner判定へ提示する最初の単位

**推奨:** Aのfixture作成・実行と上記path／test枝・資源／時間上限だけを第一開始票としてreviewする。B／Cは計画材料として受領し、必要依存・具体permission／actor・modeと追加回帰を確定してから個別判断する。

判定時には対象単位、作成／実行、開始前必須条件、上限、停止点、理由・日付を記録する。AのPASSはJPA最小保存候補の成立性に戻し、正式Tierを自動確定しない。B／CのPASSも本人認証全profile・正式Reference回帰・実OS crash・DoD／Gateへ広げない。

今回は追加検証契約・再見積案と入力確認の文書化のみ。全4 classは未作成、予定command／診断は未実行、POM・正式source／migration・環境／remoteは変更していない。

### 6.1 A単位のOwner開始判定記録（2026-10-05）

OwnerはA単位の判断内容を了解し、次の方針を承認した。

> A単位のfixture作成・実行と契約記載の上限を承認する。開始前preflight・必須枝対応・証拠固定を条件とし、不成立・上限到達・範囲拡大時は再判断する。B／C・正式Tier採用・正式Reference実装は含めない。本日は判断記録までとし、実作業は次回開始する。

| 項目 | 承認対象・条件 |
|---|---|
| baseline | `docs/daily-development-workflow` / `f145e37`。判定記録開始時clean。次回は実際のHEAD／statusと本承認を照合 |
| 作成 | §1のA：`S1JpaPermitBoundaryTest`、test専用モデル／明示Configuration／SQL、Evidenceと関連導線。test／resources／docsの記載path内 |
| 実行 | §3のA preflight・実効設定／artifact確認・source checksum固定・必須枝対応、A単独のoffline Maven test、当該使い捨てDBの準備／終了・証拠採取 |
| 検証 | §2のA1〜A5すべて。method／invocation期待件数を作成前に固定。skip／未作成をPASSにしない |
| 資源・待機 | §4のA適用条件：fork1／DB1＋Ryuk、heap768 MiB、DB1 GiB／CPU1、pool最大4・同時接続8以内・max_connections16、lock10秒・1class10分等 |
| 作業量 | §5のA8〜16標準時間を再判断枠として承認。4時間時点でflush／列権限の成立を確認。16時間以内の完了保証ではない |
| 停止／再判断 | 制限roleで不成立、資源不足・上限到達・範囲拡大時は原因／差分／残量をreview。権限を強めてPASSにしない |
| 除外 | B／C、POM変更・追加依存／artifact install／publish、正式Tier・Reference実装／migration・Framework API／Rule・Gate／remote |

実行環境の権限承認手順は本承認後も適用する。本日のpreflight・test／SQL作成・Maven／Docker実行は未実施。次回は承認範囲を再確認してpreflightから順次開始する。
