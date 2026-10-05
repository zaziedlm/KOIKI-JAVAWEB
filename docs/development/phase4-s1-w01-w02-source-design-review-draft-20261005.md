# S1 W01 / W02：保存・配置・規約と復旧安全性のsource照合（2026-10-05）

> **現在の参照先（2026-10-05）:** 本書はsource調査時点の根拠。現在の方式候補・初回開始は承認済み方式票へ追跡する。調査根拠から正式実装・runtime PASSへ昇格させない。 [最新引継ぎ](phase4-s1-local-verification-start-handoff-20261005.md)／[承認済み方式票](phase4-s1-method-ballot-and-local-verification-contract-20261005.md)。

**状態:** DRAFT / J2・J3中心の詳細設計review材料。正式方式・DDL / API / dependency・実装開始・Gateは未承認。
**Ownership:** Framework側の設計文書。Reference Application構成・業務通知、Tooling検証、Framework規約の変更責務を区別する。
**確認baseline:** `docs/daily-development-workflow` / `e75a70a`、着手時作業treeはclean。Owner実施のcommitに他package分解・統合・初回判断案等7文書が反映されたことを確認した。source読み取りのみ。effective POM / dependency tree、Maven / Docker、動作再検証、remote確認は未実施。
**入力:** [初回対象・残判断J1〜8](phase4-initial-scope-gate-and-open-decisions-draft-20261005.md)、[S1 scope](phase4-s1-a1-a2-d1-scope-mapping-20261005.md)、[安全条件S-01〜11](phase4-s1-safety-and-recovery-conditions-20261005.md)、[実演E01〜21](phase4-s1-dod-demonstration-plan-20261005.md)。

## 1. 今回の照合結果

1. Data StarterはApplicationのFlyway設定上書きを許すため、Referenceの`kkref` location / 履歴を維持する案はsource上成立する候補である。既存案の`customer`配置へ機械的に移す必要は確認できない。実際のStarter統合・新規DDL受入は検証が必要。
2. BOMのModulith version管理はruntime選択・採用承認ではない。JDBC構成をReferenceが選ぶ候補と、正式Rule / Level選択の互換設計は別にreviewする。
3. 復旧fixtureの対象snapshot確認・再送predicateはpublication単位だが、終端待ちはevent IDによる件数である。正式候補では対象publicationの当該試行とprovider受理・Auditを照合する必要がある。
4. 停止確認ファイル、JSONのLIKE検索、pollingでのJVM停止、DB内stubはToolingの単純化した実装である。停止の真正性・厳密識別・競合窓・実通信を保証する正式契約へ転記しない。

以下の「確認」はsourceの事実、「案」は未採用の設計提案、「残判断」は未確定事項を示す。

## 2. W01：publication保存・migration配置

| 論点 / source | 確認した事実 | 詳細設計案と残判断 |
|---|---|---|
| [Data Flyway auto-configuration](../../koiki-starters/koiki-starter-data/src/main/java/org/koikifw/starter/data/internal/KoikiDataFlywayAutoConfiguration.java) | KOIKI location / `koiki_flyway_history`を同じDataSourceで先にmigrateし、次にBoot側Flywayをmigrateする。既存`FlywayMigrationStrategy`がある場合はBean生成対象外 | 実効strategy・DataSource・Bean条件とJPA初期化順を正式Referenceで検証。独自strategyを追加して順序を迂回しない |
| [Data defaults](../../koiki-starters/koiki-starter-data/src/main/java/org/koikifw/starter/data/internal/KoikiDataDefaultsEnvironmentPostProcessor.java) | `customer` location / `flyway_schema_history`は低優先の既定値で、Application上書きを維持する設計 | Referenceの明示設定を尊重する。location名の違いは直ちに契約違反とは判断しない |
| [Reference設定](../../koiki-reference-app/src/main/resources/application.properties) / [V1〜V3](../../koiki-reference-app/src/main/resources/db/migration/kkref) | `kkref` location / `kkref_flyway_history`、baseline 0、JPA ddl-auto validate・OSIV無効 | publication / 通知logをReference-owned追加migrationへ置き、既存履歴を維持する案を優先比較。番号・table / column・SQLは未固定。既存適用済みmigrationを移動 / 書換しない |
| [Tooling設定](../../build-support/phase4-level2-verification/src/main/resources/application.properties) | JDBC自動schema作成を無効としUPDATE完了方式を使う | 正式候補でもFlywayとの二重初期化を避け、完了recordを保持してパージする案。設定・DDLを実効versionへ照合しreview後に採用 |

**保存の受入条件案:** 元承認、Business Audit、publicationのcommit / rollback整合をE01〜03で確認する。新規schemaと既存V1〜V3・Framework migrationを、fresh DBと既存履歴からのupgradeの両方で確認する。Framework側だけのupgrade、失敗migrationと残存publication、Data Starterを用いた起動も対象とする。Tooling V5の直接Flyway比較をStarter統合PASSへ置き換えない。

publication tableはSpring標準schemaを利用する候補とし、FAILED時刻trigger / columnは正式採用前に必要性・書込経路・upgrade責任をreviewする。publication年齢とFAILED滞留を別の観測として扱う。性能・他DB方言・成功済みmigrationの戻し方は今回確認していない。

## 3. W01：依存・Rule / Public API

| 論点 / source | 確認した事実 | 詳細設計案と残判断 |
|---|---|---|
| [Dependencies BOM](../../koiki-dependencies-bom/pom.xml) | `spring-modulith.version=2.1.1`とModulith BOM importを宣言 | version管理を利用する候補。既存fixture POMのJDBC starter宣言は材料であり、Referenceへの依存追加はreview後。推移依存・実効version・競合は別途確認 |
| [Reference POM](../../koiki-reference-app/pom.xml) | Level 2 runtimeの直接宣言はない | Application責任で必要最小のruntimeを選ぶ案。新しいFramework Starter / BOM変更を自動前提にしない |
| [KoikiArchitectureRules](../../koiki-archunit-rules/src/main/java/org/koikifw/archunit/KoikiArchitectureRules.java) / [BusinessModuleRuleSet](../../koiki-archunit-rules/src/main/java/org/koikifw/archunit/BusinessModuleRuleSet.java) | 現行`businessModuleRules(String)`はRule 28を含みtransactional listenerを拒否。Level選択入口は未提供 | 現行入口のLevel 0 / 1拒否を保ち、明示的にLevel 2を選ぶ契約案を比較。型・method signatureは未固定。ReferenceでRule 28だけを除外する迂回はしない |
| [Rule28And29CandidateTest](../../build-support/phase4-level2-verification/src/test/java/org/koikifw/buildsupport/phase4/Rule28And29CandidateTest.java) | test内booleanでLevel 2のModuleListenerを許し、直接 / meta transactional listenerを拒否する候補。Rule 29の直接依存はRule 1と重複し、Use Case / Port経由は見えない | booleanをそのまま正式APIへ移さない。選択scope・非対象moduleへの波及、annotation / meta判定、Rule 38位置・Rule 1維持をreview。間接I/Oは設計reviewと動作試験へ |
| [ReferenceArchitectureTest](../../koiki-reference-app/src/test/java/org/koikifw/reference/ReferenceArchitectureTest.java) | business rootはReference全体。Ownership testはFramework Identity rootとReference rootをimport | Level 2対象をnotificationへ限定する意味と、root全体の選択による拒否範囲をreview。現行Ownership testの対象をFramework全体の保証と説明しない |

正式Rule / Public APIを変更する場合は互換性・既存test・負例・japicmp等の必要チェックをreviewで確定する。対象listener列挙とregistry記録設定の照合も必要。registry対象選択は通常transactional listenerの実行そのものを止めないため、想定外listenerを設定だけで抑止したとしない。

## 4. W02：復旧fixtureの正式設計への差分

sourceは[ExclusiveRecoveryProbe](../../build-support/phase4-level2-verification/src/main/java/org/koikifw/buildsupport/phase4/ExclusiveRecoveryProbe.java)。以下は変更・動作testを実施していないreview指摘であり、既存fixture PASSを取り消す再判定ではない。正式候補への適用限界を追加整理する。

| 論点 | sourceの確認 | 正式候補に必要な条件・実演 |
|---|---|---|
| 停止入力 | `publicationId|eventId|status|attempts`という文字列と確認ファイルの一致 | process世代・発行主体・終了証拠・有効性・再起動制御を人が確認し、対象へ結ぶ。ファイル一致だけでは再送を許可しない。E12 |
| 対象の識別 | SQLはpublication ID・status・attemptsとserialized_eventのLIKEを使い、再送predicateはevent型とevent IDも確認 | publicationの一意ID、対象event / listener、当該試行を構造的に識別する案。JSON substring検索を正式識別契約としない。schema / API選択はreview |
| 終端待ち | `publicationCount(eventId, COMPLETED / FAILED)`が0でなくなれば待機を終了。件数は同eventの別publicationも含む | **対象publicationの今回の試行**を追跡し、別publicationの既存終端や再送前FAILEDで誤終了しない条件を設計。非対象にCOMPLETED / FAILEDを置く負例と対象がまだ処理中の状態をE13へ追加 |
| 排他・再照合 | 専用connectionでadvisory lock、取得後DB snapshot照合、再送predicateでstatus / attempts確認 | snapshot一致後の変化・通常listenerとの競合を別制御する。predicate再照合はDBの原子的claim / ownershipを証明しない。E11 / E13 |
| lock喪失 | 待機中connectionをpollし、喪失等で`Runtime.halt(70)`。終端待ちを抜けた後にもunlockで異常を確認 | 旧worker送信中・検知前の窓・終端誤認時を区別。fail-stopだけで外部副作用とDB更新の競合が解消したとしない。E14 |
| 待機・理由 | timeoutはこのprobeにない。複数のSQLException / RuntimeExceptionをLOCK_LOSTへ集約 | timeout・送信取消し不可・結果不明・障害理由・Audit / alertの正式契約を設計。実効値と停止点は未確定。E16 / E17 |

終端待ちの指摘はsourceからのリスク推論である。新しい負例を実行して再現した結果ではない。既存fixtureを修正する場合も対象検証・承認範囲を確認し、正式Referenceの受入判定と分けて記録する。

## 5. W02：停止・競合確認と人の判断

S1の最初の候補として、限定した検証環境で対象eventを処理し得る通常processを停止・drainし、自動再起動と別instanceへの引継ぎを制御してから専用復旧へ進む方式を比較する。元processが一意に追跡できる方式も比較対象として残す。全停止が本番推奨・実装採用済みであるとは扱わない。

担当者はprocess世代 / 終了、DB対象snapshot、provider受理、通知log / Audit、他運用者の作業を照合し、再送・保留・調査・連絡を決める。FAILEDへのstale遷移だけでは元listener停止を認定しない。Codexは証拠採取・照合・記録を支援し、人の実演と判断を代替しない。

| 方式候補 | 利点 | 必須の追加確認 |
|---|---|---|
| 関連通常processの停止 / drain | S1の限定環境で実行不在を人が確認しやすい | 停止対象一覧、遅延task・再起動制御、サービス停止の許容、専用processが意図せず初回処理しない構成 |
| publicationと実行所有者の追跡 | 対象外処理を継続できる可能性 | owner識別の永続性、process世代・引継ぎ・停止証拠。未提供のschema / APIや追加実装が必要なら別review |
| claim / lease / fencing | 生存listener / lock喪失への制御候補 | 通常・復旧の全実行経路とproviderを含む実効性、schema / upgrade・運用負担。独自基盤追加をS1の既定にしない |

どの方式も復旧lockとprovider冪等性の代わりにはならない。方式を選定するにはE11〜14の成立可能性と作業量 / 上限への影響を示す。現時点の推奨比較は限定環境停止方式から始め、成立しなければ差分をreviewすることである。

## 6. W02：通知key・provider受理・DB確定順

[ProviderStub](../../build-support/phase4-level2-verification/src/main/java/org/koikifw/buildsupport/phase4/ProviderStub.java)は同じDBの別transactionでevent IDを一意keyとしてINSERTし、重複時はDO NOTHING。独立した通信先への到達・応答不明・payload不一致・key保持期限を実装したものではない。

| 詳細設計案 | 残判断と必要証拠 |
|---|---|
| 同じ論理通知の初回・再送に安定keyを維持する | event ID単独で足りるか、宛先 / 通知種類を含む必要があるか、同key・異payloadの拒否、保持期間。retry / job IDをkeyへ転用しない。E08〜10 |
| 承認 / Audit / publicationを業務commitで確定し、通知Use Caseからproviderへ送信 | event発行位置、別transactionの意味、非commit時の非配信。provider受理とDBの原子的確定は保証しない。E01〜03 |
| provider受理 → 通知結果log → publication完了の各間を照合する | この順序を具体的実装で成立させるか、既存logの役割、通知log保存失敗、完了更新失敗。型・schema・transaction境界はreview。E06 / E07 |
| 受理不明時は同key照会または安全な冪等再要求へ | 実演用stubの通信境界と観測、照会不能時の保留・エスカレーション。不明を未送信に置き換えない。E10 / E21 |
| 完了はpublication終端だけでなく受理・通知log・操作Auditと突合 | 不一致時の再送保留と証跡回復、当該試行の終端識別、送信後Audit失敗。E13 / E16 |

通知logがあるだけで外部副作用を排除したとは扱わない。完了確認のために手動でpublicationのstatusを変更・削除する経路を設けない。

## 7. 初回設計候補と次の判断材料

**後続資料:** Ownerの確認・継続指示を受け、[方式候補・DoD解釈・運用／Audit・観測条件案](phase4-s1-method-dod-operations-observation-draft-20261005.md)へJ3〜J6の条件を具体化した。方式採用・DoD解釈の正式承認・実行検証は未実施。

| J-ID | 今回から渡す候補・追加論点 | 次の出口 |
|---|---|---|
| J2 | Application-owned JDBC＋UPDATE、Reference kkref履歴維持を優先比較、最小runtime、現行Rule入口互換と明示Level選択 | 変更module・migration / 依存 / Rule契約の詳細案と負例。実効依存・Starter統合は検証計画へ |
| J3 | 限定環境停止 / drain方式から比較、安定key・provider照合、publication / 当該試行の終端判定 | E11〜14 / E08〜10の方式案、未知要素と再見積影響。誤終端負例を実演資料へ追記 |
| J4〜6 | 復旧時間の起点・数値、非HTTP主体 / Audit、trace / sink・保持は未確定 | 今回の方式候補に合わせて選択肢・人の判断手順・実演条件を具体化 |
| J7 / J8 | 新規runtime・schema・Rule変更と追加Toolingの範囲が見えた | 環境確認の必要操作・承認範囲、再見積 / 上限、P4-F前置reviewと個別開始条件の整合へ |

W01 / W02のsource照合材料を作成したが、詳細設計・blocking reviewが完了したとは扱わない。次は候補方式をOwner確認へ提示し、核心DoD解釈・運用 / 観測条件の具体化へ接続する。初回対象・Gate経路は推奨案のままで正式採用判断を残す。production / Tooling code・POM・migration・Rules・CI・Gate規定・remoteは変更していない。
