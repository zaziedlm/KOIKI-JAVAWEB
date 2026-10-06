# S1追加局所検証A：JPA境界（2026-10-06）

**状態:** COMPLETE / OWNER APPROVED（2026-10-06）。LOCAL PASS / 18 invocation、failures / errors / skipped各0。承認済みAの作成前test対応を下表で固定し、最終XMLと照合した。
**入力:** [追加契約§6.1](../../development/phase4-s1-additional-local-verification-contract-and-estimate-draft-20261005.md#61-a単位のowner開始判定記録2026-10-05)。artifact欠落は環境整備として既存POMのonline取得→offline resolve→既存L1の11件PASSで解消し、資源を再確認した。

## 作成前の必須枝対応

新classは`S1JpaPermitBoundaryTest`、prefix別の期待件数は以下。合計18 invocation。skip・未作成は未達。

| 必須枝 / method prefix | 件数 | 条件 |
|---|---|---|
| A1 / a1_01 | 1 | web JPA INSERT、commit前後を別connectionで観測、不変target / actor / 期限を再読取 |
| A2 / a2_01 | 1 | worker JPA pessimistic row lock・version更新のflush / commitとSQL |
| A2 / a2_02 | 1 | web JPA確認列更新・version、immutable列を更新しない |
| A3 / a3_01 | 2 | permit / operationの再使用拒否、JPA消費INSERT・SQLSTATE23505・DB件数 |
| A3 / a3_02 | 2 | web / workerのimmutable列更新拒否、JPA経由の明示SQL・SQLSTATE42501・snapshot不変 |
| A3 / a3_03 | 3 | 消費UPDATE / DELETE / TRUNCATE拒否、実JPA flush / removeおよび明示TRUNCATE |
| A4 / a4_01 | 1 | 同許可のJPA消費競合、勝者1 / 敗者送信0 |
| A4 / a4_02 | 1 | 同対象のJPA発行競合、一意制約で勝者1 / 送信0 |
| A4 / a4_03 | 2 | 消費先行のCONFIRM / CANCEL、実DB lock待ち、閉鎖保留 |
| A4 / a4_04 | 2 | 確認先行のCONFIRM / CANCEL、実DB lock待ち、後発消費拒否・送信0 |
| A4 / a4_05 | 1 | JPA消費flush後rollback、DB再読取、version / 消費不変・送信0 |
| A5 / a5_01 | 1 | reader JPA読取、pessimistic lock / version更新拒否・送信0 |

## 構成・上限

Tooling-owned test専用の明示Spring TestConfiguration、登録Entityはpermit / consumptionだけ。最終版は`@Entity`を持たないモデルを専用`orm.xml`から明示登録し、既存ProbeApplicationのEntity scanに混入させない。XMLは自動発見される`META-INF/orm.xml`へ置かない。Hibernateはvalidateのみ、Flyway / Boot auto-configurationなし。DDLは専用SQLを管理credentialで明示適用し、runtimeはweb / worker / reader loginを使用する。

許可の不変列は`updatable=false`、発行権限外の確認列 / versionは`insertable=false`。更新列を限定するHibernate `@DynamicUpdate`をtest専用モデルで検証する。消費UPDATEの負例はDB拒否を確認するために意図的な改変メソッドを持ち、正式モデルAPIへ昇格させない。

fork1 / heap768 MiB、DB1＋Ryuk / memory1 GiB / CPU1 / max_connections16。Hikari最大接続はweb2＋worker2、readerはpoolなしで合計4。各testの独立観測connectionを含め同時8以内。lock / barrier / future10秒、class10分、送信はtest専用probeのみ。初回L1〜4・専用SQL・POMは維持する。

再開前available memory7,751,544 KiB、disk free515,503,816,704 bytes。resource不足・制限role不成立・範囲拡大時は再判断し、grantを強めてPASSにしない。

## 実行結果

artifact準備は[独立した環境整備Evidence](framework-local-artifact-readiness-20261006.md)に記録した。A commandは元契約と同じoffline / jdbc / 単独class / fork1 / heap768 MiB。

```powershell
.\mvnw.cmd -o -f build-support/phase4-level2-verification/pom.xml -Pjdbc "-Dtest=S1JpaPermitBoundaryTest" "-DforkCount=1" "-DargLine=-Xmx768m" "-Djunit.jupiter.execution.parallel.enabled=false" "-Dsurefire.failIfNoSpecifiedTests=true" test
```

| run ID | 結果・修正理由 |
|---|---|
| s1-a-20261006-01 | 18件成功、Maven14.187秒 / test6.683秒、10:42:06 JST。レビューでtestモデルの自動scan混入の可能性を検出。最終版へ変更する前のsource hashes / XML / consoleを保持 |
| s1-a-20261006-02 | 明示XML登録 / TestConfigurationへ修正し、lock待ち時の接続数観測を追加。18件成功、Maven14.367秒 / test6.963秒、10:43:59 JST、exit0。開始前prefix件数と完全一致 |

原因未変更のrerunは行っていない。compiler警告なし。両runのSurefire native stream警告はdumpstreamでJDKの`cds` shared archive build差異の起動警告と確認し、保存した。XML / exitの成功を警告なしと表現しない。

- 発行は9列のみの実JPA INSERT。version / 閉鎖列をINSERTせず、DB default version0。commit前の別connectionから0、commit後1。readerの新EntityManagerでpublication / actor / 期限を確認。
- workerは`PESSIMISTIC_FORCE_INCREMENT`でrow lockとversionのみのUPDATE、commit後version1。webの閉鎖UPDATEはclosed_at / confirmed_by / result_ref / versionだけ。immutable列をUPDATEしない。
- permit / operationの重複消費と同対象発行はSQLSTATE23505。web / workerの8種類のimmutable列改変、消費UPDATE / DELETE / TRUNCATE、readerのlock / version更新は42501。DB snapshot不変を確認。immutable改変はJPA native SQL、消費UPDATE / DELETEは管理Entityのflush / removeで実DBへ到達させた。
- 同許可の競合はSENT / ALREADY_CONSUMED各1、消費1 / probe送信1。同対象発行はISSUED / DUPLICATE各1、permit1 / 送信0。
- 消費先行ではCONFIRM / CANCELともHELD、未閉鎖・次許可23505。確認先行では後発消費CLOSED、消費0 / 送信0。`pg_blocking_pids`で4件の実lock待ちを観測した。
- 消費flush / version更新後のrollbackで双方未可視を確認し、rollback後snapshotは開始時と同じ、送信0。

DB前後snapshot・実SQL・SQLSTATE・probeをconsoleへ保存した。lock待ち時の接続数4、各test終了時5、設定pool合計4、DB上限16。観測時は8以内だが連続peak採取による容量認定ではない。schema validateと制限login起動は成功。管理credentialはDDL / reset / 資源観測だけに使用した。

最終DB containerは`c7986c53122868a56ade805611ed5588baaa4245091afcd8c60e17f478256187`。EntityManagerFactory3件 / Hikari2件のcloseをlog確認し、終了後Docker一覧は0件。当該container以外を削除していない。

## 証拠と出口

rawは`target/s1-additional-evidence/s1-a-20261006-01/`と`.../s1-a-20261006-02/`。source hashes、console / exit、XML、件数照合、dumpstream、cleanup、hash一覧を保存。

| 最終証拠 | SHA-256 |
|---|---|
| S1JpaPermitBoundaryTest.java | `4d74bde67ec6c84724ca632b218f08b078b24ca4e1d5fb8511e241ab7de91250` |
| S1JpaFixture.java | `18188616a6f3e16707ace23e69e2b3ede6b463c75010a7c7a9fde82e6d220aaf` |
| permit-jpa.sql | `ac42450881c2178c72e315a9c6a9def3cecd817c3f30f5b01979ff3ba67132d0` |
| orm.xml | `01eee925a1f1a0b60bacf2a2b11287de01860c497d225bc216335f1dcd235671` |
| test.xml | `f47f11c404d2d60011e67aa21e2e279aa92bb816ac7c9c3398608db4c70a05e3` |
| console.txt | `e831807e9a24722b57df4083166ead0bcc4ae5ace2518661874260355c95dab6` |

既存POM / L1 / L2 / 専用SQLのhash不変と、既存L3 / L4 sourceへの変更がないことを確認。既存L1の11件は環境復旧時に実行したが、L2〜4 / module全体verifyは再実行していない。新model / configurationの登録先をAだけへ閉じ、既存source / POMを変更していないため無条件の全回帰は追加していない。

Aは狭いJPA共有モデル候補の保存境界が制限roleで成立することを示す。正式Tier / Reference採用ではない。固定時刻は保存比較用であり、現在期限の認可・本人認証・実Identity / Audit・実executor / provider・commit不明の再起動・DoD / Gateは未検証。A結果の受入は承認済み。B / Cの作成・実行は各単位の個別開始判断へ戻す。

## Owner受入承認（2026-10-06）

Ownerは本書を通読し、保存方式の成立、DB保護と競合制御、検証の完結と隔離、結果の適用範囲の4点をreviewしたうえで、次を承認した。

> Aの検証結果を受け入れ承認します。引き続き、狭いJPA共有モデル候補の検討の継続へ進めます。

承認対象はA1〜A5の18件の局所検証結果の受入と候補検討の継続。正式Tier / Reference採用、B / C作成・実行・依存追加、Framework Public API / Rule、DoD / Gate、remote操作は含めない。次の検討入力は[候補継続とB前置条件の整理](../../development/phase4-s1-jpa-candidate-continuation-after-a-20261006.md)。
