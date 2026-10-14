# S1追加局所検証C：実Web認証・用途別mode・不明保全（2026-10-06）

**状態:** COMPLETE / OWNER APPROVED（2026-10-06、§6）。C38件・同profile回帰89件、計127件のPASSを具体条件内の成立証拠としてOwnerが受け入れた。
**開始承認:** [C専用開始票§7](../../development/phase4-s1-additional-c-start-review-20261006.md#7-owner開始承認2026-10-06)。Tooling-owned、branch `docs/daily-development-workflow`／HEAD `6a76b81`、未commit。

## 1. 固定した構成とartifact

選択profile `s1-web`へSession JDBC Starter、Boot Web MVC／Web MVC test／Security testの4座標をtest scopeで追加した。既存`s1-contract`／jdbcを維持し、正式Framework／ReferenceのPOM・API・migrationは変更していない。

offline effective POM／dependency tree／resolveは全exit 0。Boot 4.1.1、Modulith 2.1.1、KOIKI 0.1.0-SNAPSHOTを使用した。Cでは追加取得・local install・publishなし。Bで修復済みBOMのhashはsourceと同じ`6CB0E6738694056E0A026340A438A31F6A69EE2178DA867C5F21E1933FD5E031`。Session JARは`B52B288168F49AC946842EF14CE1E4E4B6534793CA06373601E15751A14C5A68`、cached POM／migrationはsourceと改行差のみ、既存target JARの12 classと内容一致。既存targetとの一致は現在HEADからの再build provenanceを証明しない。Identity／AuditはBの照合に加えC使用JARのhashを固定した。

管理側で実JARのAudit V2026090300、Identity V2026090301、Session V2026090701とModulith v2 PostgreSQL schemaを適用した。Session SQL hashは`A0FF22E94AB2C2F5107AF47554D06B8B344999B8563C3E50EC1E3AB0801B1F75`、Modulith schemaは`4D9065E3CDB514EAFDABBDC33682F48A62900D3A5A369B987737D693990031D4`。runtime初期化は無効、JPA validateのみ。Cのpermit／consumptionは専用ORM XMLで登録し、既存component／Entity scanへ自動混入させない。

| 構成 | 実接続と制限 |
|---|---|
| 更新Web | 実local Provider、FrameworkPrincipal、Session JDBC、CSRF／Security Filter Chain。保存actorはprincipalの不変user ID、現在権限は実IdentityQueryで再確認 |
| 観察Web | 同じ実認証・Sessionだが更新Use Case／route／listener／registryなし。permit／consumption／publicationはSELECTのみ |
| 復旧non-Web | Session auto-configurationを除外し、実registry／listener proxy／executor1を登録。起動は準備のみ、明示gate呼出だけ再送。publication INSERTなし、UPDATEは開始票の4列だけ |
| observer／管理 | observerはSELECTのみ。管理credentialはschema／seed／fault／reset限定、runtimeへ渡さない |

runtimeはC専用NOINHERIT login、owner非継承。Webにpassword再hash権限を追加せず、CompromisedPasswordChecker／IdentityAdministrationは不在。APPLICATION source protectionとtest専用HMAC、Servlet由来remote addressを使用し、外部checker／IdPへ接続していない。Public Query／Recorderと通常auto-configurationを使い、Framework内部classをfixtureへimportしていない。

## 2. 必須invocationと結果

開始票のprefix別期待件数と最終XMLを完全一致照合した。failures／errors／skipsは全0。

| prefix | 件数 | 実観測 |
|---|---:|---|
| c1_01 | 1 | 実loginからCookie／CSRFを取得、Session fixationでCookie変更、Session JDBC principal名・permit actor・Business Audit actor一致。発行1／消費0 |
| c1_02 | 2 | 不正password／disabledは実Provider拒否、双方302 `/login?error`、業務DB不変 |
| c2_01 | 3 | 未認証のissue／read／closeはloginへ誘導、業務DB不変 |
| c2_02 | 3 | ISSUE／READ／CLOSE不足を403で拒否。各正規主体の成立経路も確認 |
| c2_03 | 3 | login／issue／closeのCSRF欠落は403、業務DB不変 |
| c2_04 | 3 | scope外のissue／read／closeは同じ403、詳細を返さずDB不変 |
| c2_05 | 2 | issue／closeへのactorId改変入力は400、DB不変 |
| **Web** | **17** | **default deny、nosniff／DENY Headerも確認** |
| c3_01 | 1 | 観察contextの起動／Query／event投入／終了で業務snapshot不変、sender／listener／probe不在 |
| c3_02 | 3 | 観察roleのpermit UPDATE／consumption INSERT／publication UPDATEは実SQLSTATE 42501 |
| c3_03 | 2 | 観察Webのissue／close Use Case・handler不在。実login＋CSRF付きrequestでも404、業務DB不変 |
| c4_01 | 1 | 実listenerはAOP proxy、別worker threadで対象1件のみ。対象FAILED:1→COMPLETED:2、消費1／Business Audit1／probe1。他listener・他eventの全row不変 |
| c4_02 | 5 | 必須permit／publication／operation欠落、UUID不正、誤modeでstartup失敗。業務DB不変、起動送信なし |
| c4_03 | 1 | 実transaction内の通常event投入はpublication INSERTの42501で拒否。probe0、既存row不変 |
| c4_04 | 1 | 起動／待機／終了で再送0、status／attempt不変。Application／CommandLine runner、Session、更新Web不在、scheduled taskなし |
| c5_01 | 1 | 消費＋Business Audit commitを別contextへ保持。UNKNOWN証拠でHOLD、probe0・消費1維持 |
| c5_02 | 1 | 消費＋Audit rollbackで双方0でもUNKNOWNを保持、未閉鎖・HOLD・probe0 |
| c5_03 | 2 | commit／rollbackそれぞれ証拠file欠落で拒否、再送／解除なし |
| c5_04 | 2 | publication／worker世代不一致の証拠は拒否・HOLD |
| c5_05 | 1 | 消費なしのUNKNOWNでも、別permit IDによる次許可は未閉鎖対象のunique制約で23505。未閉鎖1を保持 |
| **Mode** | **21** | **context／executor終了後も突合、C合計38 PASS** |

WebはMockMvcのServlet／filter局所検証。実ProviderをDB user／passwordで通し、実応答Cookieを次requestへ渡した。mock principal／独自Provider／mock Sessionの注入はない。実HTTP socket／browser／TLS受入を証明するものではない。

観測snapshotはpermit／consumption／publicationの全row。認証・Session・Security Auditによる必要な保存は業務snapshotと分け、保存件数をlogへ記録した。password値／Session attribute bytesは出力しない。

復旧gateは実publication rowのevent／listener／attemptを取得し、実Queryによる認可、JPA消費とBusiness Auditのcommit、再確認、Public再送APIの順に実行する。listenerでも消費／operation／worker世代と現在Identity／未閉鎖／期限を照合する。送信は有限probeであり、外部provider I/Oではない。

UNKNOWN JSONは送信前からoperation／worker世代／対象snapshot／開始時刻を保持する。実DB commit／rollback後にcontextを閉じ、別contextで証拠を照合する。復旧確認起動は明示的なresume modeで実行gateを拒否する。第三のSQL台帳、消費削除、自動閉鎖を追加していない。UNKNOWNは制御した運用入力であり、fileの真正性・OS crash／別process worker停止を証明していない。

## 3. 実行・修正履歴と回帰

```powershell
.\mvnw.cmd -o -f build-support/phase4-level2-verification/pom.xml "-Pjdbc,s1-contract,s1-web" "-Dtest=S1AuthenticatedPermitWebTest" "-DforkCount=1" "-DargLine=-Xmx768m" "-Djunit.jupiter.execution.parallel.enabled=false" "-Dsurefire.failIfNoSpecifiedTests=true" test
.\mvnw.cmd -o -f build-support/phase4-level2-verification/pom.xml "-Pjdbc,s1-contract,s1-web" "-Dtest=S1DatabaseModeBoundaryTest" "-DforkCount=1" "-DargLine=-Xmx768m" "-Djunit.jupiter.execution.parallel.enabled=false" "-Dsurefire.failIfNoSpecifiedTests=true" test
```

最終rawは`s1-c-20261006-final/<class>/`。Web終了13:54:55 JST、Maven29.647秒／test21.055秒、XML hash `A718F2EF93231B5D53F39DFB4738C49F6185559CEE90245A56818F744142875D`。Mode終了13:55:22 JST、Maven25.584秒／test22.531秒、XML hash `D8ED39EE2606946FE3FFEBD3CE9F9D7CAAB1178EE1E9355760F70E798769311A`。

Web初回17件PASS。Mode01はfixtureのimport／helper引数のcompileエラー、02はhandler Bean複数候補とscheduled taskのassert失敗、03はhandler指定修正後もscheduled taskが残った。使用中JARをread-only調査し、taskはModulith Moments由来と確認した。C専用設定`spring.modulith.moments.enabled=false`で停止し、stalenessの`published`／`processing`／`resubmitted=0s`も明示した。04は21件PASS。次許可を別ID／unique制約で確認し、保存actor由来の送信前認可を補強した最終runでCを再実行した。権限強化・mock registry・Framework修正はしていない。

| 同profile・別Maven逐次回帰 | 件数 | 結果 |
|---|---:|---|
| S1IdentityAuditBoundaryTest | 31 | PASS |
| S1JpaPermitBoundaryTest | 18 | PASS |
| S1PermitStorageTest | 11 | PASS |
| S1PermitConcurrencyTest | 9 | PASS |
| S1ResubmissionContextTest | 8 | PASS |
| S1ModeAssemblyTest | 12 | PASS |
| **計** | **89** | **全exit 0、failure／error／skip 0。終了13:56:37 JST** |

A／BのJava／SQL／ORM計8資材は承認済み最終hashと一致。POMだけは承認済みC profileを追加した。CのControllerに付けたtest configuration識別と専用XML登録を含め、既存contextへの影響はこの回帰範囲で検証した。

## 4. 資源・証拠・適用限界

Maven1／fork1／heap768 MiB、DB1＋Ryuk、DB1 GiB／CPU1／max_connections16、runtime pool最大4、executor1 thread／queue20／終了待機10秒。contextは逐次。管理seedは最大2 connection、runtimeはpool4＋独立observer、Web／復旧は同時起動しない構成。連続connection peakは未計測。statement／transaction10秒、消費transactionのlock10秒、listener待機30秒、class elapsed10分を設定した。

開始available memory8,867,180 KiB／disk514,868,191,232 bytes、終了8,825,948 KiB／519,057,432,576 bytes、各4 GiB／10 GiB以上。C raw約14.4 MBで1 GiB以内。原因／設定／assertの変更後だけ再実行した。終了後Docker診断exit 0、Testcontainers label付きcontainerの出力0。context／Hikari／executorを終了した。

rawはToolingの`target/s1-additional-evidence/`内。preflightへeffective POM／tree／resolve／artifact・schema hash／Session照合／JAR調査、最終runへXML／console／exit／dumpstream／prefix集計／source hash／A・B保持照合／回帰集計／cleanup／資源、`s1-c-operational/`へJSONを保存した。JDK CDS archive build差のnative stream／Surefire warningは保存し、抑止していない。未完了publicationの終了時一覧はregistryのread-only診断で、status変更なし。targetはGit管理外のため、重要結果とhashを本書へ保持する。

## 5. 受入レビュー点と次の出口

1. 実local認証・Session JDBC・Cookie／CSRF・principal由来actorによるWeb17件と、mode21件・回帰89件を具体条件内の成立証拠として受け入れるか。
2. 観察modeは業務更新なし、復旧modeは明示実行だけ実registry／proxy経由で対象1件、通常eventはDB拒否、という用途別境界を受け入れるか。
3. 実commit／rollbackの双方でUNKNOWNを別contextへ保留し、証拠欠落・不一致や消費なしを安全扱いしない観測を受け入れるか。
4. Tooling専用code／role／scope／固定Clock／JSON／有限probeと正式契約を区別し、provider結果・旧worker停止証拠の真正性、再確認からI/Oまでの失効窓、正式TTL／scope／配置／Reference接続を残証拠として維持するか。

次はC結果の受入判断。その後、A／B／Cを入力として狭いJPA共有モデル候補の責務・正式接続条件を整理する。今回のLOCAL PASSで正式方式／Tier／Reference／Rule／DoD／Gate／remoteを採用済みとしない。

## 6. Owner受入承認（2026-10-06）

Ownerは本書の検証結果・受入レビュー点4項目を確認し、検証経路上は安心できる結果と理解した上で、「検証C は、ここまでの結果、理解で、問題なしとして、承認いたします」と明示した。C38件・同profile回帰89件を具体条件内の成立証拠として受け入れ、CをCOMPLETE / OWNER APPROVEDとする。

実Web認証／Session／CSRFとprincipal由来actor、用途別modeの更新禁止・実registry／proxyによる限定再送、commit／rollback双方でのUNKNOWN保留を受入範囲とする。§5のTooling専用条件と残証拠を維持し、provider結果・旧worker停止証拠の真正性、再確認からI/Oまでの失効窓、正式TTL／scope／配置／Reference接続は未取得のまま保持する。

次はA／B／Cを入力とする狭いJPA共有モデル候補の責務・正式接続条件の整理。§5の受入待ちは承認前の履歴とし、raw manifestのOWNER REVIEW PENDINGはrun終了時の記録として維持する。本承認を正式方式／Tier／Reference／Rule／DoD／Gate／remoteの採用・変更承認へ拡張しない。
