# S1追加局所検証B：専用開始票（2026-10-06）

**状態:** B COMPLETE / OWNER APPROVED（2026-10-06）。開始条件・BOM単独修復に続き、OwnerはB31件・回帰58件とレビュー点4項目を受け入れた。[B受入承認](../architecture/validation/phase4-s1-additional-b-identity-audit-20261006.md#6-owner受入承認2026-10-06)。次は[C専用開始票](phase4-s1-additional-c-start-review-20261006.md)の個別判断。
**正本:** [追加契約](phase4-s1-additional-local-verification-contract-and-estimate-draft-20261005.md)§1〜5と[候補継続文書](phase4-s1-jpa-candidate-continuation-after-a-20261006.md)§3。本票はBの具体入力を補い、A承認・正式Tier／Reference・Cの範囲を拡張しない。
**先行結果:** [A 18件 COMPLETE / OWNER APPROVED](../architecture/validation/phase4-s1-additional-a-jpa-20261006.md)。branchは`docs/daily-development-workflow`、HEADは`6a76b81`。現在のdirty差分はA code / resources / Evidence・環境整備と関連docs。上書き・commit・remote操作は行わない。

## 1. Bの目的・変更範囲

実IdentityQuery、BusinessAuditRecorder、SecurityAuditRecorderのPublic契約を、2種類記録のJPA保存候補へ接続する。現在権限と拒否、業務Auditの同transaction、Security Auditの独立transaction、送信前の失効再確認を実DBで検証する。

| 対象 | 予定差分 |
|---|---|
| Tooling POM | `s1-contract`という選択profileに既存`koiki-starter-identity` / `koiki-starter-audit`の`0.1.0-SNAPSHOT`、test scopeだけを追加。default / jdbc / jpaの内容は維持 |
| test / support | `S1IdentityAuditBoundaryTest`と既存test rootの`s1fixture/`にB専用Application / Configuration / model support |
| resources | `src/test/resources/s1-additional/`のB専用DDL / grant / ORM登録。A専用SQL / XML / Java sourceは維持 |
| 証拠 / docs | `target/s1-additional-evidence/<B-run-id>/`、実行日付のvalidation、方式票／契約／引継ぎへの結果反映 |

Root Reactor、正式source / migration、Framework Public API / Rule、CI、正式Reference、CのWeb / mode、publishは変更しない。test専用値・failure switchはToolingに閉じる。

## 2. 依存とartifact前置確認

Identityの既存POMはSecurity / Data JPA / Auditへ依存する。Securityの推移依存にBoot Security、OAuth2 Client / Resource Serverがある。Bはnon-Web・明示test構成とし、Servlet serverや認証入口を起動しない。新library versionやdependency exclusionを便宜的に追加しない。

既存cacheのread-only確認で次のPOM / JARが存在した。Maven local repositoryは`C:\Users\s-kataoka\.m2\repository`。以下は現物確認であり、全推移依存解決やsource commitの同一性の証拠ではない。

| artifact（すべて0.1.0-SNAPSHOT） | JAR SHA-256 |
|---|---|
| koiki-starter-identity | `5469a92a320b19e7a4a3962f229032d9ce0d81b51be602dff6d7a3693ff9a5f9` |
| koiki-starter-audit | `deb02c81f559bdad47149a4ae32372cc36095fed1ab3b0b1c072194da2c1e948` |
| koiki-starter-security | `35bc4890e22e30ed3fd02b79afeaff601b23743dd600da21babd3714eb7e98cd` |
| koiki-starter-data-jpa | `ef109bc1a6d7736ae581a069bc02200ba8fa805d461eb77c0877c0d420514e90` |

開始後のpreflightではprofile付きeffective POM / dependency tree / offline resolve、artifact POM / JARとmigrationのhashを保存し、既存manifest・配布元情報とsource確認の整合を照合する。出自を確かめられないartifactを現在HEAD製と認定しない。既存artifactが不足・不整合なら停止し、必要座標と取得／local installの差分を提示する。本票はKOIKI再install・snapshot取得／publishの包括許可ではない。

## 3. schema・runtime権限・構成

管理credentialで当該使い捨てDBに次の順に明示適用する。

1. 固定したAudit artifactの`db/migration/koiki/V2026090300__create_koiki_audit.sql`。
2. 固定したIdentity artifactの`db/migration/koiki/V2026090301__create_koiki_identity.sql`。
3. B専用schema / role / permit / consumption / grantと有限test user / role / permissionデータ。

1／2はartifact内の元資材を使用し、fixtureへ正式SQLを複製しない。Bの前提作成であり、Framework migration runner / upgrade契約の受入ではない。runtime Flyway・Hibernate DDL生成は無効、schema validateのみ。

| runtime主体 | 業務記録 | Framework記録 |
|---|---|---|
| 発行／確認login | A同様の発行列INSERT・閉鎖列＋version UPDATE、SELECT | Identity Queryに必要な5 tableのSELECT、Audit INSERT。Audit変更／削除とIdentity更新はなし |
| 消費login | permit SELECT / version UPDATE、consumption SELECT / INSERT | 同上 |
| 観測login | 業務／Audit／必要IdentityのSELECTだけ | row lock／UPDATE／INSERTはなし |
| 管理credential | DDL、test seed / reset、失効・障害注入、観測 | runtimeへ渡さない。owner非継承・NOINHERITとruntime DDL禁止を維持 |

Identity Query対象5 tableは`koiki_user` / `koiki_role` / `koiki_permission` / `koiki_user_role` / `koiki_role_permission`。Entity登録に伴う他tableのvalidateは必要だが、認証／管理用のDMLやpassword読取権限を追加してPASSにしない。成立しなければreviewへ戻す。

一つのruntime contextにDataSource / EntityManagerFactory / JpaTransactionManagerを各1つ置き、同じcontextのPublic Recorderへ接続する。用途別contextは逐次作成／終了し、複数DataSourceのrouting・独自transaction managerは導入しない。pool最大4、外側transaction＋Security REQUIRES_NEWの別connectionを同じ有限pool内で観測する。

Framework Bean / Entityは通常のauto-configuration発見を使い、内部classの直接import・自作Recorderへ置換しない。B modelは専用ORM XMLで明示登録し、Aと既存ProbeApplicationのscanへ混入させない。FrameworkのEntity登録と共存できることをpreflightで確認する。IdentityAdministrationのためのUserSessionInvalidator / CompromisedPasswordCheckerは供給せず、外部password checker通信を起動しない。

Modulithのschema初期化・restart再送は無効、通知listener・scheduler・送信可能Beanは登録しない。実Modulith registry接続はCの範囲へ残し、Bの送信はtest専用probeだけ。

## 4. B専用の認可・actor・target条件案

| 論点 | Bで採用する検証条件候補と限界 |
|---|---|
| 能力 | `S1_TEST_ISSUE` / `S1_TEST_READ` / `S1_TEST_EXECUTE` / `S1_TEST_CLOSE`のtest codeを使う。正式permissionコードとして固定しない |
| 消費の現在権限 | permitの発行者が実IdentityQueryでACTIVE、ISSUEとEXECUTEを保有し、fixture側scope条件も一致すること。workerは保存済み許可に拘束され、CLI user IDだけで発行しない |
| scope | test-ownedの有限対応表でuser ID → environment / publicationを拘束。Identityに業務属性を追加しない。DB読取は対象scope条件で絞り、外部応答はscope外の有無・詳細を返さない |
| 発行／閉鎖actor | Bでは準備済みtest主体としてUSERを使う。実Web本人認証はCまで未証明。入力user IDからの保存を本人認証PASSにしない |
| 消費Audit actor | 許可発行者の不変Framework user IDをUSERとして記録。委任されたworker実行であることはconsumptionのworker世代・operationと対応付け、worker本人の認証済みUSERとは表現しない |
| Security拒否actor | 現在Identityで確認できた主体はUSER、不存在／照会不能で確認できなければANONYMOUS。拒否入力のemail等を残さない。scope外のresource詳細を拒否応答へ露出しない |
| Audit相関 | resource typeは`S1_TEST_PERMIT`、resource IDはpermit ID。Business actionは`S1_TEST_ISSUED` / `S1_TEST_CONSUMED` / `S1_TEST_CLOSED`。任意metadataや未知result enumは追加しない |
| 期限 | 注入Clockで期限直前／一致／直後を検証し、`now < expiresAt`のみ有効。正式TTL・時計差は未固定 |
| attempt拘束 | B専用permitに不変`expected_attempt`を追加する案。発行権限は同列INSERTだけ、runtime UPDATEは禁止。2種類記録は維持し、第三の台帳を作らない |
| 現在snapshot | environment / publication / event / listener / attemptをB専用の明示入力として比較する。入力はtestで制御し、実registryの現在値取得や停止証拠の真正性はBのPASSで認定しない。C／正式接続で取得元・保全を実証する |
| 失効再確認 | 消費前の現在確認に加え、消費commit後・probe直前に新しいtransaction / EntityManagerで再確認。失効時は消費を保持、送信0・保留。再確認後から実I/Oまでの窓の原子的解消は主張しない |

上表はOwner review対象であり、Bへ進む指示だけを正式Reference契約の採用へ読み替えない。modelの追加列・scopeとactor条件を了承できなければ、その枝の作成前に条件を戻す。

## 5. 必須枝と予定invocation

作成前の対応案は31 invocation。class作成前に下表を最終固定し、XMLとの完全一致を照合する。skip／未作成は未達。

| 枝／method prefix | 件数 | 必須証拠 |
|---|---|---|
| B1 / b1_01 | 1 | ACTIVEと必要permissionの実Query・正常消費 |
| B1 / b1_02 | 3 | DISABLED／permission除去／不存在で消費0・送信0 |
| B1 / b1_03 | 1 | Query権限拒否の実障害で消費0・送信0 |
| B2 / b2_01 | 3 | 期限直前だけ成立、一致／直後は拒否 |
| B2 / b2_02 | 5 | environment／publication／event／listener／attempt差異は拒否 |
| B2 / b2_03 | 2 | 閉鎖済み／消費済み拒否、消費件数不増加 |
| B2 / b2_04 | 1 | scope外は詳細非露出・消費不変・送信0 |
| B3 / b3_01 | 3 | 発行／消費／閉鎖と実Business Auditがcommit前双方未可視、commit後双方可視、内容・actor・resource一致 |
| B3 / b3_02 | 1 | transactionなしの実Business Recorder拒否、Audit0 |
| B4 / b4_01 | 3 | 発行／消費／閉鎖のAudit INSERT拒否で業務変更rollback。特に消費0・送信0 |
| B5 / b5_01 | 2 | 外側transactionなし／外側rollbackの両方で拒否Security Auditだけ残る |
| B5 / b5_02 | 1 | Security Audit障害でも拒否維持・消費不増加・送信0 |
| B5 / b5_03 | 1 | 外側＋REQUIRES_NEWの有限pool・別DB接続／transaction、上限内で完了 |
| B6 / b6_01 | 1 | 初回確認後・消費前のpermission除去で再確認拒否、消費0 |
| B6 / b6_02 | 3 | 消費commit後のDISABLED／permission除去／主体不存在で消費保持・送信0／保留 |

障害は管理credentialによる当該使い捨てDBのSELECT / INSERT権限の一時剥奪で注入し、次case前に元grantへ戻す。実Recorderをmock化しない。Public AuditRecordingExceptionは原因を隠す契約なので、Public例外にSQLSTATEが残るとは仮定しない。拒否grant・DB診断の42501と前後snapshotを別証拠で照合する。

Identity失効の再読取はfirst-level cacheを共有せず、permissionの変更が実Queryへ反映されることを確認する。Security Auditはpermit lockを保持して呼ばず、Business Auditを別transactionへ分離しない。

## 6. command・回帰・上限と停止点

予定commandはRepository rootで実行。作成・具体条件review／preflight成立後に限る。

```powershell
.\mvnw.cmd -o -f build-support/phase4-level2-verification/pom.xml "-Pjdbc,s1-contract" "-Dtest=S1IdentityAuditBoundaryTest" "-DforkCount=1" "-DargLine=-Xmx768m" "-Djunit.jupiter.execution.parallel.enabled=false" "-Dsurefire.failIfNoSpecifiedTests=true" test
```

依存profileの導入後、B単独実行に加え、A18件とL1〜4の11／9／8／12件を同じprofileで逐次回帰する。auto-configurationが変化する既存S1 contextが対象。まずBean／scan／compileの影響を確認し、他の既存testへの影響が見つかれば必要な範囲を提示する。Failsafe／別JVM実演や無条件のmodule全体verifyは追加しない。

資源候補は元契約§4と同じMaven1／fork1／DB1＋Ryuk、heap768 MiB、DB1 GiB／CPU1、pool合計最大4、同時connection8以内、max_connections16。端末available memory4 GiB以上、disk10 GiB以上、lock／barrier10秒、単独class10分、出力1 GiB以内、原因未変更rerun1回まで。開始時に再測定し、値を据え置かない。

作業量は16〜32標準時間の再判断枠候補。8時間時点でPublic Beanと同transaction接続の成立を確認する。Aの短時間PASSからBの量を削減せず、上限内完了保証にしない。

制限role／Public Bean／Entity登録／同transactionが成立しない、出自／解決artifactが不整合、資源不足、上限到達、対象外依存／Framework変更が必要なら原因・必要差分・残量を示して停止する。role強化・mock Recorder・別transactionへの逃がしでPASSにしない。

## 7. 開始判断へ提示する範囲

本票で判断する対象は、B test／support／専用resourcesとtest限定profileの作成・実行、§4のtest条件、§5の31件、§6の回帰・上限・停止点。開始preflightで既存artifact／実効依存／Bean条件／schema／資源が整わなければ実行へ進めない。

承認後の出口はB1〜B6のPASS／FAIL／BLOCKED Evidenceと候補への結果反映。C・実Web認証・実provider／registry・正式Tier／Reference・DoD／Gate・remoteは含めない。現在はB fixture作成・31件実行・回帰58件とOwner結果受入を完了した。

## 8. Owner開始承認（2026-10-06）

Ownerは本票を確認し、「具体条件で、Bのfixture作成・実行・test限定POM変更と記載上限を承認いたします」と判断した。§1の変更、§4のtest条件、§5の31 invocation、§6の回帰・上限・停止点を対象に、preflight成立後の作成・実行へ進む。artifact不足／不整合や対象外変更が必要な場合の停止条件を維持し、C・正式採用・remoteへ拡張しない。

## 9. BOM修復後の実行結果（2026-10-06）

Ownerは「現在sourceのBOMだけをlocal installする対処を承認します」と判断した。BOMだけをoffline installし、cached POMとsource hash一致・Identity依存警告解消を確認した。Bは実Public Bean／EntityとSpring管理transactionで31件PASS、同profileのA／L1〜L4回帰58件PASS。[B Evidence](../architecture/validation/phase4-s1-additional-b-identity-audit-20261006.md)へ結果・修正履歴・資源・終了・残証拠を保持した。

Bは実registryを使わない範囲のため、専用起動引数でModulith event publication／JDBC registryのauto-configurationを除外した。終了時の未作成table照会を避け、registry／TaskScheduler Beanがないことを確認。Identity／Auditのauto-configurationは維持し、POM dependency exclusion・DB権限強化・正式source変更はしていない。結果のOwner受入とC開始は未判断。

## 10. B結果受入と次段階（2026-10-06）

OwnerはB結果・レビュー点4項目を確認し、「承認し次へ進めましょう」と判断した。[B Evidence§6](../architecture/validation/phase4-s1-additional-b-identity-audit-20261006.md#6-owner受入承認2026-10-06)を受入の正本とし、§9の受入待ちは承認前の履歴とする。Cは専用開始票で依存／grant／mode・38件／回帰と上限を具体化し、個別開始判断へ提示する。
