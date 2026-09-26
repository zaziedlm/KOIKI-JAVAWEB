# KOIKI-JavaWeb-FW Phase 4 PL2 — P4-F判定資料 v0.1

**状態:** WORKING DRAFT。Gate P4-Fの設置・通過、Phase 4 production実装、正式配布を承認する資料ではない。

**入力:** [Phase 4見直し草案](KOIKI-JavaWeb-FW_Phase4実施計画_見直し草案_v0.1.md)、
[P4-F提案](KOIKI-JavaWeb-FW_Phase4_P4-F限定開始Gate提案_v0.1.md)、
[PL2非配布検証](../architecture/validation/phase4-pl2-level2-verification.md)、
[Phase 0見積§7](../architecture/KOIKI-JavaWeb-FW_Phase_Estimate_Feasibility_v0.1.md#7-phase-4--enterprise-integration)。
実案件の確定入力はNext.js/BFFとKOIKI REST連携のみ。外部IdP SSOは見込みであり、本資料のA1 / A2 / D1の見積・設計条件へ含めない。

## 1. F-2 配置・契約の選択肢

| 候補 | 変更する場所の案 | blocking reviewと未決定事項 |
|---|---|---|
| A1 publication基盤 | 最小案はSpring Modulith標準のJDBC store＋UPDATEを第一候補とし、Applicationが構成する。共通化が必要ならFrameworkの既存Data Starter / migration境界を検討。Tooling fixtureは正式artifactへ移さない | JDBC / JPAは両方とも同一PostgreSQL / JPA業務transactionで復旧、再送、パージが成立。二階層FlywayのKOIKI所有 / Application所有をToolingで確認したが、正式schema所有者は未決定。両方式とも複数JVMの同時再公開で同一listenerへ入った。CP8型lockは専用復旧JVM同士に限定。通常listenerとの競合・lock接続喪失、完了record運用、DB方言と性能、migration所有をA1で判断。Starter / Java Public API追加は別review |
| A2 `notification` Reference | 既存`koiki-reference-app`内の新しい業務packageを候補とし、`expense`承認eventを公開境界として使う。通知内容、log、provider AdapterとstubはReference / Toolingの責任に分ける | event payloadに個人情報を含めない設計、同期vetoと非同期side effectの分離、実providerの冪等key契約。providerが保証しない場合のDoD 4-3解釈をOwner判断 |
| D1 非同期観測 | 既存Observability StarterのServlet `requestId` / `TaskDecorator`は再利用候補。event ID、publication ID、retry / job IDの表現とmetricはA1と同時設計し、業務語彙はApplication側 | HTTP requestを離れた再送・jobでの相関、trace / logの非露出とcardinality、FAILED状態に入ってからの経過時間、運用sink / alert Ownerを決める。exporter既定を先行固定しない |
| Architecture Rules | `koiki-archunit-rules`の現行Rule 28はLevel 0 / 1の拒否を維持。Level 2選択時の規約とRule 29は別選択契約・negative fixture候補 | 既存`businessModuleRules(String)`の意味を変えずLevel指定をどう追加するか、Rule 29がRule 1と重複する範囲、間接I/O経路を静的に検査できるかをreview |

現行Data StarterはKOIKI migrationを`db/migration/koiki` / `koiki_flyway_history`、Application側migrationを
`db/migration/customer` / `flyway_schema_history`に分ける。既存のLevel 2 fixtureの単一Flyway履歴は
この二階層の受入検証ではない。追加した[V5 Tooling試験](../architecture/validation/phase4-pl2-level2-verification.md#33-v5storeと二階層migrationの比較)は
両所有配置の二階層実行とKOIKI側の独立upgradeを確認したが、Data Starterを用いたA1統合試験ではない。
publication tableをFramework共通契約にする場合はKOIKI側migration・upgrade / rollback・DB方言責任が発生する。
Application配置にする場合は各Consumerのschema準備とversion整合が必要になる。どちらも未選定。

**A1再公開の推奨案（Owner未判定）:** 全通常instanceで起動時自動再公開を無効にする。
再送は認可・Audit付きの専用復旧processに集約し、CP8と同じPostgreSQL session advisory lockで
復旧process同士を排他する。FAILEDは回数上限候補を適用し、PUBLISHED / PROCESSINGは前処理processの
停止確認と対象IDを伴う手動復旧から始める。停止を確認できない対象は再送しない。
自動化には生存中listenerを除外する仕組みかfencingが必要。Toolingでは元processの停止確認と
対象publication ID・観測状態・試行回数の一致を要求する入口、およびlock接続喪失時に専用JVMを停止する候補を検証した。
停止確認の真正性と検知までの競合窓は残るため、provider冪等性を維持し、fencingを別途判断する。これは
[PL2 Evidence§3.1](../architecture/validation/phase4-pl2-level2-verification.md#31-再公開の排他方針候補toolingでの検証)で
lock接続が維持された復旧worker間だけを実証した候補であり、A1 blocking review前にproductionへ採用しない。

| 再公開方式 | PL2の評価 | A1判断 |
|---|---|---|
| 全instanceで起動時自動再公開 | 同一publicationのlistenerが2 JVMで同時実行された | 採用しない案 |
| 専用復旧process＋CP8型lock | worker間の競合skip、強制停止後の再取得、完了後の解放をJDBC / JPAで実証。guarded入口の停止確認・状態照合とlock喪失時JVM停止をfixtureで確認。確認が虚偽なら生存中listenerと重複し、lock喪失の検知前も競合窓がある | **条件付き推奨候補**。停止確認の発行・検証、検知前の競合と外部送信のfencing、運用認可を解くまで採用保留 |
| 外部schedulerの単一起動指定だけ | RepositoryのCP8判断ではDB側の競合保証・crash recoveryを満たさない | 単独保証に使わない案 |
| publicationごとのclaim / lease / fencing | 生存中listenerとの競合やlock接続喪失への対策候補 | schema・migration・運用負担を含めて別review。現時点で固定しない |

## 2. F-3 DoD実演とPL2 Evidenceの差

| DoD | 非配布fixtureで確認済み | production実演前に残ること |
|---|---|---|
| 4-1 | 元transactionの承認記録は、非同期listener失敗後も残る | `expense`承認状態・Business AuditとReference通知の結合 |
| 4-2 | PUBLISHEDとPROCESSING中の別JVMを送信前 / 送信受理後で強制停止し、再起動後にCOMPLETED。複数JVMの同時再公開では同一listenerへ入る競合を確認。guarded再送入口とlock喪失時の専用JVM停止候補を確認 | 元processの停止確認の真正性、検知前の競合窓、外部送信fencing、package済みReferenceでの再現 |
| 4-3 | provider stubに一意keyがあれば再配送でも受理1件。なければ2件 | 実providerの冪等契約または同等策、通知logと送信境界の一貫性 |
| 4-4 | FAILED件数・publication年齢gauge、stale monitor、明示再送と試行回数filter | FAILED遷移からの経過時間の定義、運用者認可 / Audit、上限到達通知、複数instance競合 |
| 4-5 | completed publicationだけの保持期限パージ | 単一実行基盤とretention / purgeの配備・権限・同時配信競合 |
| 4-12 | 既存Servlet request相関・TaskDecoratorのsourceを確認 | event / retry / jobを跨ぐtrace・log実測、別requestへの漏えい負例 |

正式実演はpackage済みReference、PostgreSQL、別process停止 / 再起動、通知stub、観測sinkを用い、
成功・失敗・復旧時のDB / Audit / log / metricを突合する。各DoDのPASSはfixtureのPASSから自動判定しない。

## 3. F-4 暫定工数とcommit point

Phase 0のDoD 4-1〜4-5・4-12の**既存47〜80標準人日**を、重複計上しないために以下へ仮配賦する。
これは再見積や実施予算の承認ではない。P4-Fに必要なPhase共通作業・CI費用は別途積む。

| Package | 既存DoD見積の仮配賦（標準人日） | 暫定commit point / 検証出口 |
|---|---:|---|
| A1 | 21〜37：4-2全額10〜18、4-5全額5〜8、4-4の再送運用4〜7、4-12のevent相関2〜4 | CP-F1: store / migration / Rule reviewを先に通す。CP-F2: Framework契約とTooling復旧証拠。productionの対象module・回帰コマンドはreviewで確定 |
| A2 | 18〜30：4-1全額10〜16、4-3全額8〜14 | CP-F3: A1契約後にReference通知、冪等境界とpackage済み実演。実provider依存が解けない場合は4-3を保留 |
| D1 | 8〜13：4-4のmetric / alert4〜7、4-12のtrace / log4〜6 | CP-F4: A1と並行設計。CP-F5: A1・A2・D1の統合実演で4-2 / 4-4 / 4-5 / 4-12を判定 |
| 合計 | **47〜80**（Phase 0の同じ範囲の再配列） | P4-Fの新しい総額ではない |

追加のPhase共通作業はModulith方式決定、二階層migration、Rule 28 / 29、ADR / Skill、CI、
運用runbookに分けて再計上する。対象module・設計 / 実装 / test / 実演 / 文書別の標準人日、
AI支援時のOwner稼働日、外部待ち時間、CI / PostgreSQL / mail stub / 観測sinkの費用は未算定。
これらを埋める前にGate P4-Fを開催しない。

## 4. F-5 Gate関係と停止点

P4-Fを採用する場合でも、P4-AR6実チーム受入、AR-D10責任分担、Gate P4-ARは未完了のまま残す。
現行[P4-AR計画](KOIKI-JavaWeb-FW_Pre-Phase4_Adoption_Readiness計画_v0.1.md)と`AGENTS.md`の
「Gate P4-AR後にPhase 4開始を別判断」という規定に、Framework限定の例外Gateを追加する改訂が必要である。
改訂本文はP4-F採用判断と同時にOwner reviewし、現時点で規定を変更しない。

| 改訂候補 | 追加する内容 | 維持する条件 |
|---|---|---|
| P4-AR計画 | Gate P4-ARの前にFramework限定のGate P4-Fを置ける例外と、A1 / A2 / D1の対象commit point・Evidence・停止点を追記 | P4-AR6 / AR-D10 / Gate P4-ARの状態は未完了。正式受渡し・Phase 4全体開始は別判断 |
| `AGENTS.md` | 「OwnerがGate P4-Fを明示承認した対象commit pointに限りFramework共通範囲を開始できる」と追記 | 現在のPhase 3 / P4-AR baseline、Public API等のblocking review、remote個別承認、Customer固有作業の境界を維持 |
| Phase 4実施計画 | P4-F対象と対象外、DoDの暫定Evidence、P4-AR6後の再審査を記録 | 当初DoD 4-1〜4-12の採否・変更をP4-Fから推定しない |

停止点は、A1のstore・migration / dependency・Rules / Public API、A2のprovider冪等性とReference境界、
D1の相関 / 個人情報・運用Owner、各production commit point、remote変更、配布である。
P4-Fで許可する対象・期限・Owner・Evidenceと、停止後のrollback対象を個別に記す必要がある。

## 5. 次のPL2作業

以下は[PL2検証記録§3](../architecture/validation/phase4-pl2-level2-verification.md#3-未実施と次の確認)と
本資料のF-2〜F-5を結ぶ継続台帳である。順序はFramework側で独立に進められる作業を先に示す。
V1 / V2のTooling結果はDoD 4-2の正式PASSやP4-F通過を意味しない。

| 順 | 継続タスク | 次に作るEvidence・終了条件 | 判断・待ち条件 |
|---|---|---|---|
| 0 | V1 / V2の作業差分を固定 | `PUBLISHED`停止窓、guarded再送・lock喪失のfixtureと検証記録を一組として差分確認し、検証済み状態をコミットした | `96796e9`。production成果物への昇格ではない |
| 1 | **PL2-V5：A1のstore / migration選定入力** | 両storeの機能・dependency差、KOIKI / Application二階層Flywayの両配置とKOIKI独立upgrade、失敗DDL rollbackを[検証記録§3.3](../architecture/validation/phase4-pl2-level2-verification.md#33-v5storeと二階層migrationの比較)へ記載。JDBC＋UPDATEをreview第一候補とした | Tooling試験は完了。A1 blocking review前。Framework / Applicationの正式migration所有、DB方言、成功済みmigrationのrollbackと性能・運用差はOwner判断。性能値を得ないまま優劣を断定しない |
| 2 | **PL2-V4：Rule 28 / 29の負例** | Level 0 / 1の拒否を維持しつつLevel 2を選択するArchUnit fixture、Rule 29の直接・間接I/O経路の検出限界を記録 | 正式Rules / Public API変更はA1 blocking review後。既存`businessModuleRules(String)`を先に変更しない |
| 3 | **PL2-V3：D1の観測契約** | FAILED遷移からの滞留時間とpublication年齢を区別し、初回event・再送・job間の相関ID / trace / log、漏えい負例をfixtureで確認 | 滞留起点、alert sinkと運用OwnerはD1 review入力。exporter既定を先行固定しない |
| 並行 | **PL2-V2：復旧運用の残件** | 停止確認の発行元・対象process識別・有効期限、複数運用者の競合、試行上限到達時の通知、認可・Audit、検知前の競合窓と外部送信fencingの選択肢をrunbook案にする | 現fixtureの確認ファイルは停止の真偽を証明しない。追加の安全性主張は運用方式とprovider契約を決めてから検証する。A1 blocking reviewへ提出 |
| 続く | **F-2 / F-3 / F-4の判定資料完成** | A1 / A2 / D1のmodule・dependency・migration・Rules / Public API影響、DoD 4-1〜4-5 / 4-12のpackage済み実演手順、commit point / rollback、設計・実装・test・実演・文書別の工数とOwner / CI費用を記入 | V2〜V5の結果を入力する。A1 store・schema ownershipが選べなければP4-Fは`REWORK`候補 |
| 続く | **Phase 4全体のPL2台帳とF-5** | P4-01〜11・optionalの採否条件、当初DoDとの対応、P4-F対象外の待ち条件を残す。P4-AR計画・`AGENTS.md`の改訂差分を提案形で用意 | P4-B1の4-8 / 4-9とCustomer主導P4-03Bを分離。現行Gate規定はP4-F判断まで変更しない |
| 最後 | **P4-FのOwner判断** | F-1〜F-5を揃えて限定開始の採否、対象commit point、停止条件を判定する | 現時点でGate P4-Fは未設置・未通過。production開始は別承認 |

[PL1差分台帳](KOIKI-JavaWeb-FW_Phase4_PL1_REST利用境界差分台帳_v0.1.md#3-p4-ar6へ渡す確認事項)の
Q1〜Q5とP4-AR6実チーム受入は入力待ちとして並行管理する。Next.js/BFF＋REST以外の案件要件、
外部IdP SSOの確定、P4-B2 / E1のEvidence・正式受渡しをFramework側の推測で埋めない。
PL2-V1のpackage済みReferenceでの正式実演はproduction Gate後に扱う。
