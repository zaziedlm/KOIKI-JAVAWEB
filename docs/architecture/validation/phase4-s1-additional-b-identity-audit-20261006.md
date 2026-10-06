# S1追加局所検証B：Identity／Audit境界（2026-10-06）

**結果:** COMPLETE / OWNER APPROVED（2026-10-06）。B1〜B6の31 invocation、A／L1〜L4回帰58 invocationはすべてfailure 0／error 0／skip 0。Ownerは§5のレビュー点4項目を確認し、結果を受け入れた。
**承認範囲:** [B専用開始票§8](../../development/phase4-s1-additional-b-start-review-20261006.md#8-owner開始承認2026-10-06)。[BOM修復承認とpreflight](phase4-s1-additional-b-preflight-20261006.md)に従って依存不整合を解消して再開した。
**位置:** `build-support/phase4-level2-verification/`のTooling検証。branch `docs/daily-development-workflow`、HEAD `6a76b81`。正式Framework／Reference／Public API／migration／CI／remoteの変更ではない。

## 1. 実接続と変更物

- `S1IdentityAuditBoundaryTest`：開始票の31件と、実DB観測・障害注入・payload／transactionのassert。
- `s1fixture/S1IdentityAuditFixture`：non-Webの明示test構成。既存IdentityQuery／BusinessAuditRecorder／SecurityAuditRecorderは通常のauto-configuration発見を使い、内部classのimportやmock Recorderは使わない。
- `s1-additional/permit-identity-audit.sql`／`orm-identity-audit.xml`：B専用2種類記録、制限role／grant、XMLでのみ登録するJPA model。追加`expected_attempt`は発行列INSERTだけ、runtime UPDATEは拒否する。
- Tooling POM：選択`s1-contract` profileへIdentity／Auditの既存正式座標をtest scopeで追加。jdbc／jpaの定義は維持し、commandでは`jdbc,s1-contract`を明示する。

AのJava／SQL／XML計4資材はA最終runのSHA-256と一致し、既存S1 test sourceは変更していない。

実Identity／Audit migrationをcache JAR内resourceから読み込み、管理credentialで使い捨てDBへ適用した。resource URLが`jar:`であることもassertした。正式SQLをfixtureへ複製していない。続いてB専用DDL／grantとtest seedを適用。runtime Flywayは無効、Hibernateはvalidateのみ。実Identity／Audit EntityとBのXML modelが同じEntityManagerFactoryで成立した。

各runtime contextはDataSource／EntityManagerFactory／Spring JpaTransactionManager各1つ、Hikari最大4。発行用contextを終了後に消費用contextを開始し、role routingや同時多用途poolは使わない。runtime login／NOINHERIT、Identity更新・password credential SELECT・Audit UPDATE／DELETEがないことをassertした。IdentityAdministration／外部password checkerを供給せず、本人認証やpassword policyの検証とはしない。

## 2. 必須枝と実測結果

| prefix | invocation | 結果／観測 |
|---|---:|---|
| b1_01 | 1 | 実QueryでACTIVE・必要permissionを確認。消費1・probe1 |
| b1_02 | 3 | DISABLED／EXECUTE除去／主体不存在で消費0・probe0。確認できた主体はUSER、不存在はANONYMOUS |
| b1_03 | 1 | runtimeのIdentity SELECT剥奪。別DB診断で42501、実Query失敗、消費0・probe0、Security actor ANONYMOUS |
| b2_01 | 3 | Clockで期限直前だけ成立。一致／直後は消費0・probe0 |
| b2_02 | 5 | environment／publication／event／listener／attempt差異は拒否。attempt列UPDATEもDBで42501 |
| b2_03 | 2 | 閉鎖済み／消費済みは拒否、消費件数は不増加、probe0 |
| b2_04 | 1 | scope外の存在／不存在は同じ拒否。消費0・probe0、拒否Auditへresource IDを残さない |
| b3_01 | 3 | 発行／消費／閉鎖とBusiness Auditはcommit前双方未可視、commit後双方可視。actor／resource／action／result一致、双方のxminとSpring transactionのtxid一致 |
| b3_02 | 1 | transactionなしの実Business RecorderがAuditRecordingException、Audit0 |
| b4_01 | 3 | Audit INSERT剥奪を42501で診断。発行／消費／閉鎖はrollback。消費について実行経路全体もDENIED・消費0・probe0 |
| b5_01 | 2 | transactionなし／外側rollbackの双方で拒否Security Auditだけ残り、業務変更なし・probe0 |
| b5_02 | 1 | Security Audit INSERT障害でも拒否維持、消費0・probe0。記録障害数1、Audit0 |
| b5_03 | 1 | 外側とREQUIRES_NEWの別connection／transaction、同じ有限pool内で成立。外側へ戻ったconnectionも一致 |
| b6_01 | 1 | 初回Query成立後・消費前のEXECUTE除去を新transactionで再読取し拒否、消費0 |
| b6_02 | 3 | 消費commit後のDISABLED／EXECUTE除去／主体不存在を新transaction／EntityManagerで確認しHOLD。消費1を保持、probe0、Business／Security各1 |
| **合計** | **31** | **全PASS、skipなし。XMLのprefix別件数を開始票と完全一致照合** |

Business Auditの同transactionはSpring TransactionTemplate内のJPA変更と実Recorderを接続し、独立したSELECT専用observerからcommit前後を読んで確認した。raw EntityTransactionをSpring MANDATORYの証拠として流用していない。

B5_03では外側backend PID `570`／txid `1244`、Security Audit PID `572`／txid `1245`を観測した。Audit INSERT時のSpringにbindされた同じconnectionをStatementInspectorからread-onlyで確認し、別の観測用connectionのPIDを取り違えない構成にした。pool activeは2、設定上限4。permit lockを保持してSecurity Recorderを呼ばない。

障害は管理credentialによる当該DBのgrant剥奪。case前に元grantへ戻す。実RecorderのPublic例外は原因を隠すため、例外からSQLSTATEを取得せず、同roleによる独立DB診断の42501と保存件数を突合した。

## 3. 実行・回帰と修正履歴

```powershell
.\mvnw.cmd -o -f build-support/phase4-level2-verification/pom.xml "-Pjdbc,s1-contract" "-Dtest=S1IdentityAuditBoundaryTest" "-DforkCount=1" "-DargLine=-Xmx768m" "-Djunit.jupiter.execution.parallel.enabled=false" "-Dsurefire.failIfNoSpecifiedTests=true" test
```

最終run `s1-b-20261006-04`はexit 0、Maven 33.486秒／test 25.61秒、終了12:45:44 JST。XML SHA-256は`7DA23C07E3915845D692BA3A581F0C4E138A6ED45D10EAAD7B3CDF7C106DE64A`。

| run | 判定／差分 |
|---|---|
| 01 | compile失敗、test未実行。fixture helperのchecked exception宣言を修正 |
| 02 | 31件PASS。ただしcontext終了時に未作成EVENT_PUBLICATION照会の警告を検出し、最終証拠に採用しない |
| 03 | B専用non-Web構成でModulith event publication／JDBC registryのauto-configurationを除外。31件PASS、終了時照会警告なし。外側transactionの実拒否とattempt列拒否を補強 |
| 04 | 制限role／資源上限／安全なAudit payload、Audit障害後の実行経路全体のprobe0を明示assert。31件PASS、最終採用 |

02の終了時照会は使用中Modulith classの`destroy()`が無条件で未完了publicationを読むため、schema初期化／restart再送を無効にするだけでは止まらなかった。B専用起動引数で`EventPublicationAutoConfiguration`／`JdbcEventPublicationAutoConfiguration`を除外し、registry／TaskScheduler Beanがないことをassertした。新しいDB権限やEVENT_PUBLICATION tableを足して警告を消していない。POM dependency exclusionやFramework設定の変更ではない。実registry接続はCの残証拠として維持する。

同じprofileで以下を別Maven実行・逐次回帰した。04の追加assertはB専用fixture／testに限定し、他classが使う構成は03から変えていない。

| class | tests | failures／errors／skips |
|---|---:|---|
| S1JpaPermitBoundaryTest | 18 | 0／0／0 |
| S1PermitStorageTest | 11 | 0／0／0 |
| S1PermitConcurrencyTest | 9 | 0／0／0 |
| S1ResubmissionContextTest | 8 | 0／0／0 |
| S1ModeAssemblyTest | 12 | 0／0／0 |
| **回帰合計** | **58** | **全exit 0** |

## 4. 資源・終了と証拠

fork1／heap768 MiB、DB1＋Ryuk、DB memory1 GiB／CPU1／max_connections16を指定・assertした。contextごとのpool最大4、role contextは逐次。case終了時のDB connection数は8以内、単独class10分のelapsed assertとtransaction／statement／lock 10秒を設定した。連続peak監視の実証ではない。

preflight時のavailable memory9,289,340 KiB／disk514,930,016,256 bytes、終了時8,895,076 KiB／514,892,877,824 bytes。各4 GiB／10 GiB以上。B関連raw合計は約30.4 MBで1 GiB以内。全runは原因またはassert変更後に実行し、原因未変更の反復はしていない。

終了後の`docker ps --filter label=org.testcontainers=true`はexit 0／出力0。JPA context／Hikariの終了をlogで確認した。JDKのCDS archive build差によるnative stream／Surefire warningは残る。最終`fork.dumpstream`で原因を確認し、testのfailure／error／skipは0。JDK設定やwarning抑止は変更していない。

rawは`target/s1-additional-evidence/`配下のpreflight、01〜04、`s1-b-20261006-regression/<class>/`に保持。最終04にはXML／console／exit／prefix件数／source hash／A保持照合／回帰集計／cleanup／memory／disk／manifest／hash一覧／dumpstreamを保存した。targetはGit管理外であり、必要な結果・範囲・hashは本書へ保持する。

## 5. 候補継続への入力・受入レビュー点

狭い2種類JPA共有モデル候補について、現在Identity／permission、期限・target拘束、実Business／Security Auditのtransaction境界、失効時の消費保全がToolingの具体条件で成立した。

受入レビュー点は次の4点。

1. 実Public Query／Recorderと同じSpring JPA transaction stackで、Bの31件・回帰58件を成立証拠として受け入れるか。
2. Business記録失敗は業務rollback、Security記録失敗でも拒否維持、消費commit後の失効は消費保持・probe0／HOLDという観測を受け入れるか。
3. B専用の有限scope／permission code／USER actor／expected_attempt／Clock／registry除外を検証条件として扱い、正式契約へ自動昇格しないことを了解するか。
4. Cの実Web本人認証・実registry／現在snapshotの取得元、provider／worker停止証拠、再確認後からI/Oまでの失効窓、正式TTL／scope／配置・正式Reference接続を残証拠として維持するか。

BはCLI入力からの本人認証、実送信／復旧完遂、同一性のある現在registry値、再確認とI/Oの原子性、正式artifact provenance／正式scan構成をPASSにしていない。正式Tier／Reference／DoD／Gate／C開始は別判断であり、Bの受入承認を今回の作成・実行承認へ読み替えない。

## 6. Owner受入承認（2026-10-06）

Ownerは本書の検証結果・レビュー点4項目を確認し、「承認し次へ進めましょう」と判断した。B31件・回帰58件を具体条件内の成立証拠として受け入れ、BをCOMPLETE / OWNER APPROVEDとする。Business／Securityの失敗時動作、B専用条件と正式契約の区別、§5の残証拠を維持する。

次は[C専用開始票](../../development/phase4-s1-additional-c-start-review-20261006.md)の具体化へ進む。今回の受入はCの未提示依存／grant／fixture作成・実行／上限、正式Reference／Tier／DoD／Gate／remoteの包括承認ではない。raw manifestの受入欄はrun終了時の履歴として維持し、本節を最新の受入記録とする。
