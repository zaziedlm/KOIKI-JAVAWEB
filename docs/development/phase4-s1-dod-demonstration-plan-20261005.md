# 作業7：S1 DoD実演計画・人による確認判断（2026-10-05）

**状態:** DRAFT / 作業7の実演分解。実行・blocking review・Gate・DoD判定は未実施。S1完遂を目指す候補化判断は承認済み。
**Ownership:** Framework側の計画文書。正式Reference候補の実演と、非配布Toolingの故障注入・観測を分ける。
**確認baseline:** `docs/daily-development-workflow` / `781a6a2`。前段の作業7文書は未commit差分。既存資料を照合し、今回新しいMaven / Docker検証を実行していない。
**入力:** [S1範囲照合](phase4-s1-a1-a2-d1-scope-mapping-20261005.md)、[安全性・復旧条件 S-01〜11](phase4-s1-safety-and-recovery-conditions-20261005.md)、[PL2判定資料§2.1](KOIKI-JavaWeb-FW_Phase4_PL2_P4-F判定資料_v0.1.md#21-package済みreferenceでの実演手順案)、[既存Tooling Evidence](../architecture/validation/phase4-pl2-level2-verification.md)。

## 1. 人の総合判断を含む実演方針

Ownerは、DB状態だけでは再送要否や処理進行を捉えにくく、実運用の人の目と判断を前提に、Framework機能と組み合わせて復旧を成立させる方針を示した。本計画はその指示に沿う。

各caseで担当者がDB、process終了・起動世代、provider受理、通知log、Audit、metric / traceを照合し、再送・保留・調査継続・エスカレーションの判断と理由を記録する。自動観測は判断材料を揃え、対象取り違え・権限外操作・危険な再送を抑える。運用者が確認できない結果は不明として残す。

検証のために全事象を自動判定することを必須にしない。一方、同一通知の重複抑止、認可、保存整合等は人の判断だけで代替しない。担当者数、二者承認、運用時間帯、許容復旧時間、alert先は実施条件の取得段階とblocking reviewで決める。

## 2. 実演構成と記録の単位

| 構成 | 役割と確認事項 |
|---|---|
| package済みReference候補 | 固定sourceの`koiki-reference-app`で承認、publication、notification、認可 / Auditを実行。既存同期vetoとSecurity baselineを維持 |
| PostgreSQL | 実行単位ごとに隔離。Framework / Reference migrationのversionと履歴、対象recordを読取採取。履歴・DDLを本計画で固定しない |
| 通常process / 専用復旧process | 独立OS processとしてinstance・起動世代・設定を記録。初回、元実行停止、復旧者競合、lock接続喪失を識別 |
| provider stub / 独立観測 | 非配布Toolingとして受理key・件数・payload照合・応答不明を観測。既存DB内stubと同等か、通信境界を持つ別stubが必要かを実装reviewで決める |
| 故障注入・制御 | Toolingから停止位置を同期して実processを停止し、指定位置に到達した証拠を残す。正式codeへtest switchを混入させない方式をreview |
| log / metric / trace / alert sink | 初回・復旧とDB / provider / Auditを照合。実traceの採取とMDC markerを区別。sinkの配置・version・費用は未確定 |
| 運用者 / 判定者 | 権限のある運用者が照合・判断・操作。判定責務を記録するが別人を一律には要求しない。役割・担当者・代行はOPEN |

全caseに、実演ID、source SHA、JAR checksum、設定・依存・migration identity、時刻 / 時計差、実行者・判定者、環境とprocess世代を残す。対象event / publication / listener、論理通知key、retry / job / incidentの対応表を採取する。秘密・PIIは証拠へ露出させない。

共通の観測セットは、実行前後のDB snapshot、停止位置とprocess終了証拠、provider受理、Business / 操作Audit、log / metric / trace / alert、人の判断理由と操作結果である。独立した観測手段を用い、fixtureの停止確認ファイルだけを停止証拠にしない。

## 3. caseとDoDの対応

**Owner確認（2026-10-05）:** Ownerはcaseが多様・複雑であり、この時点のOwner reviewだけでは網羅判定を言い切れないとしたうえで、後続検証で十分に確認されていくことへ注視・集中する意向を示し、**本節のcase構成を了解**した。これはcase構成の了解であり、網羅性の確定、全文の一括承認、実行開始、blocking review通過、DoD PASSを意味しない。

後続検証ではcaseごとの結果・証拠、不足または追加すべきcase、検証できなかった枝、残るリスクをOwnerへ示して継続確認する。21 caseを固定の完全網羅リストとせず、実装・検証で得た事実に応じて見直す。実行範囲・工数・承認境界への影響がある追加は対応するreviewで判断する。

以下は実演case IDであり、runtime契約ではない。caseはまだ実行していない。安全条件の依存と担当を揃えた後、各caseを実行可能な手順へ落とす。

| Case / DoD | 操作・故障位置 | 観測と人の確認判断 | caseのPASS条件案 / FAILとなる事象 |
|---|---|---|---|
| E01 / 4-1 | 通常の承認・通知を完了 | 承認、Business Audit、publication、通知logとstub受理を照合 | 全て整合して対象通知が受理1件。未通知・不整合はFAIL |
| E02 / 4-1 | 承認commit後にproviderを失敗させる | 承認を取り消さず、失敗を担当者が確認して原因調査へ | 承認 / Auditを保持し通知失敗を観測。承認rollback・失敗の不可視化はFAIL |
| E03 / 4-1 | 権限・scope・version拒否、同期vetoの回帰。保存整合を確認する承認rollbackも別操作で試す | 拒否 / rollbackの理由と、通知が発生していないことを確認 | 承認不成立からprovider副作用なし、既存同期契約維持。誤配信はFAIL |
| E04 / 4-2 | publication保存後PUBLISHEDで元process停止 | 停止位置・終了世代・DB・他実行不在を確認し、復旧を判断 | 定めた再起動 / 手動復旧経路で同一publication完了、受理1件、許容時間内。欠落・誤対象・二重受理はFAIL |
| E05 / 4-2・4-3 | PROCESSINGの送信前で元process停止 | 未送信を独立観測し、停止証拠と同じkeyを照合して再送 | 同じ通知が完了し受理1件。旧実行との危険な並走・二重受理はFAIL |
| E06 / 4-2・4-3 | provider受理直後、通知log保存前で停止 | DBだけでは未送信とせず、受理照合または安全な冪等再要求を判断 | 同じkeyで受理総数1件、通知logとpublicationが整合。key変更・二重副作用はFAIL |
| E07 / 4-2・4-3 | 通知log保存後、publication完了前で停止 | log・provider・publicationの到達点を照合して復旧 | 受理総数1件で完了整合。logだけから誤って成功 / 未送信を断定する場合はFAIL |
| E08 / 4-3 | 同じ論理通知を再配送・並行要求し、別の通知も送る | key・payload・通知種別の対応を確認 | 同一通知は1件、別通知は個別受理。重複副作用・別通知の誤抑止はFAIL |
| E09 / 4-3 | stubでkeyなし、payload不一致、key保持期限切れを個別に模擬 | 適用条件外と認識し、再送保留とエスカレーションを選ぶ | 負例を独立観測し、正式候補が危険な要求を拒否 / 保留。Toolingで意図的に生じた重複は負例証拠であり正常caseの成功ではない |
| E10 / 4-2・4-3 | 受理後の応答喪失と受理前の切断、照会不能を個別に模擬 | 不明と拒否を区別。照合可能なら同じkeyで復旧、判別不能なら保留 | 照合可能な枝で受理1件・整合。不明の枝で追加副作用なし、判断・未確定結果を記録。盲目的再送はFAIL |
| E11 / 4-2・4-4 | 元listener生存中の再送要求。生存したままstale化でFAILEDとなる枝も実施 | statusだけで停止を認定せず、担当者がprocess証拠を確認 | 危険な再送を拒否・保留、復旧による追加受理なし。FAILEDだけで再送許可ならFAIL |
| E12 / 4-2・4-4 | 停止確認なし・虚偽・改変・期限切れ・別世代・旧試行の再利用・確認後再起動を個別に試す | 発行主体・process世代・対象・有効性を確認 | 無効な確認を拒否し送信なし。虚偽確認を正当として進めるならFAIL |
| E13 / 4-2・4-4 | 複数運用者 / 復旧processの同時要求、古いsnapshot、同一eventの別publication | lock所有者・incident・対象を照合し、先行作業の完了後に再観測 | 対象だけ処理、競合者は送信せず終了。誤対象更新・並走による重複はFAIL |
| E14 / 4-2・4-3・4-4 | lock接続だけを送信前・送信中・受理後で失わせ、新workerの取得と重ねる | 旧workerの継続可能性とprovider受理を確認。再取得だけで再送しない | fail-stop等と重複抑止を合わせ副作用1件・整合、未確定なら保留。検知前の窓で二重副作用 / 古いworkerの不正更新があればFAIL |
| E15 / 4-4 | listener失敗・FAILED滞留を作り、alert受信から運用者が照合・認可再送 | 件数、publication年齢とFAILED滞留起点、試行数、理由、before / after、通知先を突合 | 起点が契約どおり、alert・判断・再送・終端結果・Auditが追える。metric不整合・権限外送信はFAIL |
| E16 / 4-4 | 未認証・権限不足・対象外操作、送信前Audit失敗、受理後Audit失敗を個別に試す | 送信前拒否と送信後の結果不明を区別し、後者は再送保留・証跡回復へ | 前者で送信なし、後者で副作用を照合し判断と記録回復を実演。記録失敗から重複再送すればFAIL |
| E17 / 4-4 | 試行上限、完了待ちtimeout、解除権限拒否、alert不達・代行への連絡 | 上限 / timeoutを送信取消しと誤認せず、保留・調査・エスカレーション | 上限を越えず、未確定結果を保全、連絡と解除条件を記録。無制限再送・保留放置はFAIL |
| E18 / 4-5 | COMPLETED・FAILED・未処理を混在し保持期限前後でパージ | 対象・保持契約・通知key寿命と証跡保全を確認 | 期限を過ぎた対象COMPLETEDのみ削除。FAILED / 未処理の削除や期限前削除はFAIL |
| E19 / 4-5 | パージ2起動、lock喪失、配信 / 復旧との競合 | 実行者・削除対象・復旧中記録を照合し、曖昧ならパージ停止 | 単一実行と整合維持、復旧対象・必要証跡を保全。競合で誤削除・復旧不能ならFAIL |
| E20 / 4-12 | HTTP承認→非同期失敗→別process復旧→完了と次の無関係requestを実行 | 運用者がeventからDB・provider・Audit・log / traceを追跡 | 同じ通知を追跡でき、trace関連と非漏えい・非露出を確認。MDCだけでtrace成立と判定、追跡断絶・PII露出はFAIL |
| E21 / 4-4・4-12 | 必要観測sinkや判断材料を利用不能にし、回復させる | 情報不足を認識して保留・連絡し、回復後に新しい証拠で再判断 | 危険な再送をせず、保留対象・担当・次の確認を記録。回復後に照合して解決。情報不足のまま成功と断定すればFAIL |

E04〜07は停止後の実配信を完遂するcaseである。E09〜14等の拒否・保留が正しく動くことだけでDoD 4-2 / 4-3をPASSにしない。E16 / E21等では「停止して終わる」に加え、照合・証跡回復・再開または正式エスカレーションまでの運用を確認する。

## 4. 安全条件のcoverage

| 条件 | 主な実演case |
|---|---|
| S-01 停止確認の真正性 | E04〜07、E12 |
| S-02 通常listener競合 | E11、E12、E14 |
| S-03 複数復旧者 | E13、E14 |
| S-04 lock喪失 | E14、E19 |
| S-05 通知識別・冪等性 | E06〜10、E14、E18 |
| S-06 provider受理不明 | E06、E10、E14、E17 |
| S-07 DBと外部副作用 | E01〜07、E10、E14、E16 |
| S-08 認可・Audit | E03、E15、E16 |
| S-09 上限・timeout・alert | E15、E17、E21 |
| S-10 監視・相関 | E15、E20、E21 |
| S-11 保持・パージ | E18、E19 |

同じ停止・再送から複数DoDの証拠を採取してよいが、case・観測・判定を分けて記録し、作業量を二重計上しない。列挙した個別枝を省略する場合は代替証拠と残る範囲をreviewする。

## 5. case判定とDoD判定の区別

| 判定 | 適用条件 |
|---|---|
| case PASS | 固定した実演構成・個別枝で期待する配信 / 拒否 / 保留と人の確認判断を実証し、照合可能な証拠が揃った |
| case FAIL | 安全条件違反、誤対象、重複副作用、保存不整合、誤判断を確認した。担当者が後で気付いても違反を成功へ変更しない |
| case BLOCKED | 実装・権限・担当・契約・故障位置制御・必要観測がなく、期待動作を検証できない。想定した情報不足を安全に扱うE21の成功とは区別 |
| 未実施 | 計画だけで実演していない。現時点は全caseが未実施 |
| DoD PASS候補 | 該当正常caseと負例群、人による判断・結果照合、未解決事項を集約し、Ownerが合意した解釈・適用条件を満たした場合に提出 |

不達のalertが通常caseで必要な通知を妨げた場合はFAIL、alert構成自体が未準備ならBLOCKEDとする。意図的な不達caseでは代替連絡と担当者の行動を期待結果として評価する。

DoD 4-2へ手動復旧を適用する解釈・許容復旧時間、4-3でstub受理一意性を判定する適用範囲はOwner判断が残る。S1 stubの成功を実メール到達や実provider保証へ拡大しない。実案件provider契約の未取得はCustomer採用の待ち条件であり、S1 stub実演を一律に停止させる条件にはしない。

## 6. 人の確認判断を残す記録様式案

各case・incidentに、次を記入する。記録様式はTooling / 運用資料の候補であり、Framework IdentityやPublic APIへの項目追加ではない。

| 項目 | 記録する内容 |
|---|---|
| 判断者と時点 | 運用者・判定者・必要な承認者、確認時刻、権限とincident |
| 到達点の照合 | DB状態、元process世代 / 終了、provider受理、通知log / Auditと観測先。確定・推定・不明を分ける |
| 判断 | 再送 / 保留 / 調査継続 / エスカレーション、根拠、満たした技術条件、未取得材料 |
| 操作と結果 | 対象・同じ通知key・試行、送信数、DB・Audit・観測の結果、矛盾の有無 |
| 継続・引継ぎ | 保留対象・理由、担当 / 代行・連絡先、次の確認時点・再開条件、解決または正式引継ぎ先 |
| 判定 | case / DoDの対応、PASS / FAIL / BLOCKEDと証拠参照、適用条件と残課題 |

## 7. 開始前の入力と後片付け

GateとA1 / A2 / D1 reviewによる実行範囲の承認、Reference候補の実装、担当・代行・環境・権限、停止確認方式、provider key契約、Audit failure semantics、復旧時間・上限、sink / alert、保持方針が必要である。現時点で未確定の項目を完了扱いにしない。

環境は隔離したDB / process / stub / sinkを用いる案とし、case開始時に対象を初期化して固有IDを付ける。証拠採取前にpublicationやprovider記録を消さない。FAIL / BLOCKED時は追加送信を止め、元・復旧processと未確定副作用を確認する。cleanupは証拠保管・判定後に所有範囲を確認して行い、既存開発DBや案件環境を対象にしない。具体的コマンド・費用・実施Ownerは次段階で確定する。

## 8. 次の作業

**後続資料:** [S1担当・検証環境・作業量／上限・実施順案](phase4-s1-execution-resources-and-sequence-draft-20261005.md)で、担当責務・環境構成・残作業概算 / 上限・段階出口を整理した。具体的担当・資源・数値上限の承認は未取得である。

本書でDoD実演単位・証拠・人の判断と判定条件を分解した。次は実施 / review / 復旧担当、検証環境・cleanup、作業量・概算 / 上限、実施順と停止点を揃える。未取得はOPENとし、AI支援時間や既存47〜80標準人日を今回の予算へ自動転用しない。

続いて他packageを分解し、作業4b・5・7をOR材料へ統合する。本書作成で実演実施・DoD PASS・S1完了・作業7全体完了・Gate通過を宣言しない。production code、POM、Rules、migration、CI、remoteは変更していない。
