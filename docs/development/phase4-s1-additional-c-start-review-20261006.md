# S1追加局所検証C：専用開始票（2026-10-06）

**状態:** COMPLETE / OWNER APPROVED（2026-10-06）。開始承認範囲を実行し、C38件・同profile回帰89件の結果をOwnerが受け入れた。[C Evidence§6](../architecture/validation/phase4-s1-additional-c-web-mode-20261006.md#6-owner受入承認2026-10-06)。
**正本:** [追加契約§1〜5](phase4-s1-additional-local-verification-contract-and-estimate-draft-20261005.md)、[責務・認可・mode／DB権限案§5〜7](phase4-s1-responsibility-authorization-audit-mode-draft-20261005.md)。
**位置:** `build-support/phase4-level2-verification/`のTooling-owned検証。branch `docs/daily-development-workflow`／HEAD `6a76b81`。A／B code・resources・POM・Evidenceと関連docsの未commit差分を維持する。Owner一人＋Codexで順次進める。

## 1. 目的・作成範囲と依存差分

C1／C2は実local認証・Session・SecurityFilterChain／CSRFと認証主体由来actor、C3〜C5は実DBでの目的別mode・Modulith proxy／対象再送・context再起動時の不明保全を検証する。CをWebの1 classとmodeの1 classに分け、単独・逐次実行する。

| 対象 | 予定差分 |
|---|---|
| Java | `S1AuthenticatedPermitWebTest`／`S1DatabaseModeBoundaryTest`。既存test rootの`s1fixture/`へC専用構成・モデル・Web受付／Query・実行gate・有限probeを追加 |
| resources | `src/test/resources/s1-additional/`へC専用DDL／grant／ORM。A／Bの資材を変更・コピー昇格しない |
| POM | 新しい選択profile `s1-web`に下記4座標をtest scopeで追加。既存`s1-contract`／jdbc／jpaは維持 |
| 証拠 | `target/s1-additional-evidence/<C-run-id>/`、日付付きvalidationと既存方式票／契約／引継ぎへの結果反映 |

追加候補は`org.koikifw:koiki-starter-session-jdbc:0.1.0-SNAPSHOT`、`org.springframework.boot:spring-boot-starter-webmvc`、`org.springframework.boot:spring-boot-starter-webmvc-test`、`org.springframework.boot:spring-boot-starter-security-test`の4座標。Bootのversionは既存Parent／BOM管理値を使う。Web runtimeとWeb test支援を明示し、推移依存をeffective treeで確認する。Reference POMとSecurity Tooling POMに同じ既存座標の利用例がある。HTMX／Thymeleaf画面や正式KOIKI Web MVC Starterの採用を今回の検証へ足さない。

test compileは全test sourceを対象にするため、Cの両classと追加profile下の回帰は`jdbc,s1-contract,s1-web`を指定する。non-Web classだからprofileを外してCのWeb sourceをcompile不能にしない。

開始preflightでParent／BOM／追加JAR・推移依存・migration／schema resourceのhashと内容、effective POM／tree／offline resolve、auto-configuration条件、資源を固定する。BOM修復後のcache状態を無確認でCへ持ち込まない。欠落・不整合は必要座標と取得／install差分を提示して停止する。今回のBOM単独install承認はCの追加取得／再installへ広げない。

## 2. 実Web本人認証・能力・actorの条件

Webは明示したServlet test構成でMockMvcを使い、実SessionRepositoryFilterと実SecurityFilterChainを各1回通す。実Identityのlocal認証Providerを通常のauto-configurationで発見し、DBの有限test user／encoded passwordを使ってform loginする。`@WithMockUser`、principal注入、独自UserDetailsService／Providerで代替しない。

CookieとCSRF tokenを実GET／login応答から採取して次requestへ渡し、Session JDBC保存をDBでも確認する。MockHttpSessionの手渡しだけをSession JDBC成立証拠にしない。これはServlet／filterの局所検証で、実HTTP socket・browser・TLS・正式Reference画面の受入ではない。

| 条件 | C専用の具体案 |
|---|---|
| route／Security | `/s1-test/**`とform login／logoutだけのtest用優先chain。外側は既存KOIKI default denyを維持。CSRF／Security Headerを有効にし、無関係routeも拒否を確認 |
| 能力 | Bと同じ`S1_TEST_ISSUE`／`READ`／`EXECUTE`／`CLOSE`。発行／閲覧／確認終了を分け、保存主体は認証済みFrameworkPrincipalの不変user IDから取得。Session authorityだけで済ませず実IdentityQueryで現在権限を再確認 |
| 有限主体 | issuer（ISSUE／READ／EXECUTE）、closer（CLOSE／READ）、reader（READ）、limited（能力なし）、outsider（対象scope外）、disabled。業務scopeはfixture-owned user→environment／publication対応表 |
| 期限／actor | 注入ClockとBの期限境界条件。actor入力がある発行／閉鎖requestは400で拒否し、DB／Business Auditのactorは認証主体に拘束。入力user ID／emailをactorとして保存しない |
| 失敗応答 | 未認証はloginへ誘導、能力／scope／CSRFは403、未知actor入力は400。不正password／disabledは同じlogin失敗形状。scope外の存在・詳細を露出しない |
| 確認終了 | 有限の「人が突合済み」というtest証拠参照に拘束。閉鎖を配信成功と扱わず、実provider・旧worker停止の真正性は残条件 |
| 外部通信 | local認証をopt-in。APPLICATION source protectionを維持し、test専用HMAC key／IDとServlet由来remote addressを使う。EXTERNALへの変更やWeb details偽装はしない |

source確認で、local認証はpassword credential読取だけでなく、LoginAttemptStoreのREQUIRES_NEW更新・成功時の期限切れlock解除を必要とする。CのWeb roleはBのQuery専用roleとは別にreviewする。current encoderでseedし、encoded password再hash更新はCの範囲へ含めない。password更新権限が必要になる場合は停止する。

`CompromisedPasswordChecker`を供給せず、ReferenceのHaveIBeenPwned checkerをimportしない。Framework Identity／Audit内部classはimportせず、実Public契約と通常のauto-configurationを利用する。想定外にchecker／IdentityAdministrationが生成される場合はBean条件を提示して停止し、無条件成功checkerでPASSにしない。

## 3. schema・用途別DB権限・mode

管理credentialで隔離DBに、固定した実artifact内のAudit V2026090300、Identity V2026090301、Session V2026090701を明示適用する。Modulith JDBC artifactの`org/springframework/modulith/events/jdbc/schemas/v2/schema-postgresql.sql`も管理側で適用し、C専用permit／consumption・role／grant・有限seedを続ける。資材はartifactから読み、正式SQLをfixtureへ複製しない。runtime Flyway・Session schema初期化・Modulith schema初期化・Hibernate DDL生成は無効、JPA validateのみ。

| login／構成 | permit／consumption | publication | 認証・Framework保存 |
|---|---|---|---|
| 許可／確認終了Web | B相当の発行列INSERT、閉鎖列＋version UPDATE、SELECT。consumptionはSELECTのみ | SELECTのみ | Queryの5 table SELECT、password credential SELECT、login_attempt SELECT／INSERT／UPDATE／DELETE、credentialのlocked_until／version／updated_at UPDATE、Session2 tableの必要DML、Audit INSERT |
| 観察Web | 両方SELECTのみ。lock／更新は不許可 | SELECTのみ | 実認証に必要な上記限定権限。認証／Session／拒否Auditの更新は別snapshotで説明 |
| 単発復旧non-Web | Bの消費role相当。permit version UPDATE、consumption INSERT／SELECT | SELECT、completion_date／status／completion_attempts／last_resubmission_dateのUPDATEのみ。INSERT／DELETE／TRUNCATE禁止 | Query5 table SELECT、Audit INSERT。password／Session／login_attempt権限なし |
| observer | 必要記録のSELECTのみ | SELECTのみ | Audit／Identity／Sessionの突合に必要なSELECTのみ。password値・Session attribute bytesをlogへ出さない |
| 管理 | DDL・test seed／fault／resetのみ | 同左 | runtime credentialへ渡さない |

全runtimeはNOINHERIT、owner非継承、DDLなし。Audit UPDATE／DELETE、Identity業務属性追加、消費改変を許さない。Webの認証用更新権限をpermit／publication更新へ混同しない。C専用roleはB roleを強化して流用しない。

観察Web／更新Webにはsender・通常listener・復旧runner・registryを登録しない。Bと同じModulith publication／JDBC auto-configuration除外をtest構成内で使い、publicationはscope先行の読取だけ。

復旧non-Webはlocal認証を有効にせず、Boot／KOIKIのSession JDBC auto-configurationをC専用起動で除外し、Servlet／Session／更新Web受付を登録しない。実Modulith registryとC専用対象listener proxy・executor1を登録し、startup全件再送・schema初期化・schedulerによる再送／status変更は止める。実Bean／scheduled task一覧で不在を確認できなければ作成・実行方式へ戻す。

起動は対象permit／publication／operationを検証・準備するだけで送信しない。実行gateを明示呼出した場合だけ、実Query／permit／registryの現在snapshotを照合して消費・Business Auditをcommitし、送信直前失効を再確認して対象1件をPublic再送APIへ渡す。現在event／listener／attemptは実registry rowから取得し、Bの自由なtest snapshot入力をそのまま使わない。SQLのtable／列形式はCの版固定Adapter内に閉じ、Framework Public APIにしない。

対象listenerは実proxyと操作相関を確認し、通常eventの送信経路は拒否する。復旧roleにpublication INSERTを与えず、通常event投入がpublication生成経路へ入ってもDBで拒否される候補とする。対象以外の既存publicationのstatus／attemptは不変。標準再送が追加列／INSERTを必要とする場合、役割を強化せず原因・差分を提示して停止する。

## 4. 不明保全と必須invocation

C5は同じDBを保持し、contextを閉じて別contextを作る。operation／worker世代／対象snapshot／実行開始を送信前からtest-owned JSON証拠fileへ保全し、未知結果・停止／突合未確認のまま自動再送・消費削除・閉鎖・次許可を許さない。第三のSQL管理台帳は作らない。

commit／rollbackは実DBで両方作るが、ack喪失による不明という運用入力はfixtureで制御する。context再起動をOS crash／別process停止の真正な証拠にしない。証拠file欠落・対象／世代の不一致は拒否・保留とし、消費行なしを安全な未実行と認定しない。

| class／prefix | 件数 | 必須証拠 |
|---|---:|---|
| Web / c1_01 | 1 | 実login・Cookie／Session JDBC・CSRFを経た許可発行、認証主体actorとBusiness Audit一致 |
| Web / c1_02 | 2 | 不正password／disabledの実Provider拒否、同じ外部形状、業務DB不変 |
| Web / c2_01 | 3 | 未認証の発行／閲覧／確認終了を拒否 |
| Web / c2_02 | 3 | ISSUE／READ／CLOSEの不足を各拒否。readerの発行／閉鎖能力とは分離 |
| Web / c2_03 | 3 | login／発行／確認終了でCSRF欠落を拒否、業務DB不変 |
| Web / c2_04 | 3 | 発行／閲覧／確認終了のscope外を拒否、存在情報非露出 |
| Web / c2_05 | 2 | 発行／確認終了のactor改変入力は400・業務DB不変 |
| **Web合計** | **17** | C1／C2全枝。default deny／Security Headerも各経路で突合 |
| Mode / c3_01 | 1 | 観察contextの起動／閲覧／event投入／終了でpublication／attempt／permit／consumption不変・probe0 |
| Mode / c3_02 | 3 | 観察loginのpermit UPDATE／consumption INSERT／publication UPDATEは42501 |
| Mode / c3_03 | 2 | 観察Webに発行／確認終了Use Case／route不在、更新不可 |
| Mode / c4_01 | 1 | 実registry／対象listener proxyを通じて明示実行だけ対象1件。消費1、Business Audit1、他listener／他event不変 |
| Mode / c4_02 | 5 | permit／publication／operation欠落、UUID不正、誤modeを拒否。起動送信0 |
| Mode / c4_03 | 1 | 通常event投入を拒否。publication INSERT拒否・通常送信0・既存対象不変 |
| Mode / c4_04 | 1 | 復旧起動／待機／終了だけでは再送0・status／attempt不変、scheduler／自動runner不在 |
| Mode / c5_01 | 1 | 消費commit＋不明証拠を保持し別contextへ引継ぎ、消費保持・probe0／保留 |
| Mode / c5_02 | 1 | 消費rollback＋不明証拠でも自動再送／解除なし、未閉鎖・probe0 |
| Mode / c5_03 | 2 | commit／rollbackそれぞれ証拠欠落で拒否・保留 |
| Mode / c5_04 | 2 | 対象不一致／worker世代不一致の証拠で拒否・保留 |
| Mode / c5_05 | 1 | 不明・未閉鎖のまま次許可を拒否。消費なしだけで新許可を作らない |
| **Mode合計** | **21** | C3〜C5全枝、context／executor終了まで観測 |
| **C合計** | **38** | 作成前にこのprefix別件数を固定し、XMLと完全一致照合。skip／未作成は未達 |

初期publicationは有限3件（対象、同eventの別listener、別event）、permit1件、test user6件を基本とする。有限なtest停止／突合証拠を正式運用の真正性証明へ拡張しない。

## 5. command・回帰・上限・停止点

承認・preflight成立後、Repository rootから各classを逐次実行する。

```powershell
.\mvnw.cmd -o -f build-support/phase4-level2-verification/pom.xml "-Pjdbc,s1-contract,s1-web" "-Dtest=S1AuthenticatedPermitWebTest" "-DforkCount=1" "-DargLine=-Xmx768m" "-Djunit.jupiter.execution.parallel.enabled=false" "-Dsurefire.failIfNoSpecifiedTests=true" test
.\mvnw.cmd -o -f build-support/phase4-level2-verification/pom.xml "-Pjdbc,s1-contract,s1-web" "-Dtest=S1DatabaseModeBoundaryTest" "-DforkCount=1" "-DargLine=-Xmx768m" "-Djunit.jupiter.execution.parallel.enabled=false" "-Dsurefire.failIfNoSpecifiedTests=true" test
```

同profileでB31、A18、L1〜L4の11／9／8／12、計89件を逐次回帰する。Session／Servlet依存がnon-Web既存contextへ加わる影響を確認する。影響が広がれば対象・差分を提示し、無条件module全体verifyや正式Reference全回帰を追加しない。

資源はMaven1／fork1／DB1＋Ryuk、heap768 MiB、DB1 GiB／CPU1、pool合計最大4、同時DB connection8以内、max_connections16。contextを逐次起動・終了し、Webと復旧を同時起動しない。available memory4 GiB／disk10 GiB以上を開始時に測定する。lock／barrier10秒、listener完了30秒、各class10分（C初回2 class確認枠20分）、raw出力合計1 GiB以内、原因未変更rerun1回まで。必要executorは1thread／queue20、終了待機10秒。観測値と設定上限、連続peak未計測を区別する。

作業量は元契約のC12〜24標準時間を再判断枠とし、6時間時点で実認証の外部通信不要とmode分離／実proxyの成立を確認する。A／Bの短時間PASSから工数を削らず、完了保証にしない。別JVM・collector・実provider・外部IdP・有料serviceを追加しない。

実Provider／Session／role／実proxy・通常経路抑止／不明保全が成立しない、artifact不整合・不足、資源不足、上限到達、password再hash権限・publication INSERT等が必要、Framework／Reference／追加library変更が必要な場合は、原因・必要差分・残量を提示して停止する。mock主体・Recorder・registry、権限強化、証拠欠落の安全扱いでPASSにしない。

## 6. 今回の成果物と開始判断対象

今回はB受入記録とC具体条件の文書化、既存sourceと使用中Modulith v2 schema resourceのread-only確認まで。CのPOM／Java／SQL作成、依存解決・取得／install、Maven／DB実行は未実施。Bの結果承認を未提示のC条件の包括承認へ読み替えない。

次の個別開始判断対象は、§1のC専用fixture／test限定profile作成と実行、§2・3の認証／能力／grant／mode条件、§4の38件、§5の回帰89件・上限・停止点。これは[追加契約§6](phase4-s1-additional-local-verification-contract-and-estimate-draft-20261005.md#6-owner判定へ提示する最初の単位)の「必要依存・具体permission／actor・modeと追加回帰を確定してから個別判断する」に対応する。

出口はC1〜C5のPASS／FAIL／BLOCKED Evidenceと候補への反映。正式Reference／Tier／Public API／Rule／migration／CI／DoD／Gate／remoteの変更、実OS crash／provider／停止証拠の正式受入は含めない。

## 7. Owner開始承認（2026-10-06）

Ownerは本票を確認し、「Cのfixture作成・実行・test限定POM変更と記載上限について承認いたします」と判断した。§1のC専用資材・test限定4座標、§2・3の具体条件、§4の38件、§5の回帰89件・資源／時間／停止点を承認範囲とする。preflight成立後に作成・実行へ進む。§6の未実施状態は承認前の履歴として維持し、artifact取得／再installの包括許可や正式採用・remoteへ拡張しない。

## 8. 承認範囲の実行結果（2026-10-06）

offline preflight成立後、4座標のtest profileとC専用fixture／SQL／ORMを作成した。実Web17件・mode21件と同profile回帰89件は全PASS。prefix件数完全一致、A／Bの8資材hash不変、資源上限・context／container終了を確認した。追加取得／installなし。Modulith Momentsのscheduled taskはC専用標準設定で停止し、publication INSERT／追加UPDATE列の権限を与えていない。

[C Evidence§5](../architecture/validation/phase4-s1-additional-c-web-mode-20261006.md#5-受入レビュー点と次の出口)を次の受入判断対象とする。開始承認を結果受入へ読み替えず、正式採用と未取得の運用証拠を残す。

## 9. Owner結果受入（2026-10-06）

Ownerは検証結果・レビュー点4項目を確認し、ここまでの結果と理解について問題なしとしてCを承認した。[C Evidence§6](../architecture/validation/phase4-s1-additional-c-web-mode-20261006.md#6-owner受入承認2026-10-06)を受入の正本とする。§8の受入待ちは承認前の履歴。具体条件内の成立証拠をA／Bと合わせて候補検討へ入力し、正式運用・Reference接続の残条件を維持する。
