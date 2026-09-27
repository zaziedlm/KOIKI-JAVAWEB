# Phase 4 PL2 — event publication復旧runbook案

**状態:** REVIEW INPUT / 非配布Tooling Evidenceに基づく運用案。A1 blocking review、Gate P4-F、運用Ownerによる承認前であり、productionの操作手順ではない。

**対象:** Spring Modulith Level 2候補の`FAILED`、停止後の`PUBLISHED` / `PROCESSING` publicationを1件ずつ選別して復旧する場合。

**Ownership:** 手順案はPL2 Architecture資料。実運用の担当者・権限・Audit・provider契約・実装配置は未決定。

[PL2検証記録§3.1〜3.2](../architecture/validation/phase4-pl2-level2-verification.md#31-再公開の排他方針候補toolingでの検証)と
[P4-F判定資料§1](KOIKI-JavaWeb-FW_Phase4_PL2_P4-F判定資料_v0.1.md#1-f-2-配置契約の選択肢)を根拠とする。
Phase 1bの[CP8単一実行契約](../architecture/validation/phase1b-cp8-single-execution.md)は専用process同士の排他候補であり、
通常listenerの生存判定や外部送信のfencingを提供しない。

## 1. 復旧の安全境界

通常instanceの起動時自動再公開を全て無効にし、復旧を専用入口へ集約する案とする。
この構成が実際に全instanceへ適用されている証拠がなければ、復旧作業を開始しない。
専用入口は対象publication ID、event ID、観測status、`completion_attempts`を1組として受け取り、
lock取得後にもDBの同じ1行と照合する。`FAILED`は試行回数と障害原因を確認し、
`PUBLISHED` / `PROCESSING`は元の処理processの停止を別途確認する。
発行からの経過時間やstale判定だけでprocess停止を認定しない。

Toolingで確認したのは、確認ファイルの欠落や古いDB観測を拒否できること、lockが保たれる間の
専用復旧process同士の排他、lock接続喪失を検知した復旧JVMの停止までである。
確認ファイルの存在は停止の証明ではない。lock喪失から検知までの競合窓があり、
外部送信を開始した後はJVMを止めても送信済みの副作用を取り消せない。
したがってprovider側の同一eventに対する冪等keyまたは同等のfencingが確認できない場合、
重複送信の可能性がある対象の再送を保留する。

## 2. 操作前に残す確認記録

| 確認する情報 | 確認方法の案 | 欠落・不一致時の停止 | 判断Owner候補 |
|---|---|---|---|
| 作業の単位 | incident / change ID、環境、対象service、実行予定時刻、担当者と承認者を記録 | 他の運用者・自動jobの作業と重なる場合は単一の作業責任者へ集約するまで停止 | 運用Owner（未指名） |
| 元processの識別 | instance / container ID、process起動世代、deploy revision、対象listener、event / publication IDを紐付ける。platformの終了状態と時刻、process log等の独立した証拠を照合する案 | `PUBLISHED` / `PROCESSING`で元processの停止を特定できなければ`STOP_UNCONFIRMED`として再送しない | 運用Owner、A1 Owner |
| 停止確認の有効期間 | 発行者、発行時刻、対象process世代、対象publicationと観測試行回数、有効期限を持つ記録を候補とする。lock取得後、再送直前に再確認する | 期限切れ、発行者不明、対象不一致、旧試行の使い回しは停止。必要な有効期間と証拠保管先は未決定 | 運用Owner、Security Owner |
| DBの対象snapshot | read-onlyでpublication ID、event ID、listener ID、status、`completion_attempts`、発行 / 失敗時刻を取得し、1行だけを選ぶ | 0件、複数件、COMPLETED、またはlock取得後の値の変化は`STALE_SELECTION`として停止。DB行を手作業で書き換えない | A1 Owner、運用Owner |
| 外部送信の状態 | event IDからprovider側の受理結果と冪等key契約を確認する。応答不明・送信中も想定する | providerの受理有無を判断できず、重複抑止も確認できない場合は再送を保留して照合へ回す | provider / A2 Owner |
| operator権限・Audit | 実行者の認証・対象環境への権限、別承認の要否、操作理由、before / after状態、結果・拒否理由をDB正本のAuditへ残す案。Phase 2の既存契約との適合をreviewする | 認可・Auditの正式入口が未整備ならproduction再送を開始しない。fixtureの確認ファイルで代用しない | Security Owner、運用Owner |

上表のOwnerは**責務候補**であり、人員配置や権限付与の決定ではない。正式担当・代行・エスカレーション先は
F-4で割り当てる。停止証拠の取得方法、有効期限、承認者数、Auditの分類とfailure semanticsもA1 reviewで決める。

## 3. 状態別の暫定判断

| 観測status | 再送前の条件案 | 条件を満たさない場合 |
|---|---|---|
| `FAILED` | 失敗原因、前回の外部送信結果、試行回数と上限、他の再送job不在を確認。専用入口で対象1件を再照合 | 原因不明、上限到達、他workerとの競合、provider重複抑止不明なら保留して通知・調査 |
| `PUBLISHED` | 元processが停止し、新規listenerへ渡されていないことを確認。停止確認を対象のprocess世代とpublicationへ結び付ける | 発行からの経過時間だけでは再送しない |
| `PROCESSING` | 元process停止を確認。送信前・送信受理後の両窓を想定し、provider受理記録と冪等性を照合 | listenerの生存・送信状態が曖昧なら再送しない |
| `COMPLETED` | 復旧対象外。保持・パージは別の運用判断 | 再送しない。通常のパージ手順へ分離 |

fixtureの`completionAttempts < 2`はfilterの成立を示す値であり、運用の推奨上限ではない。
上限、上限到達時の通知、手動解除条件、承認者をA1 / 運用Ownerが決める。

## 4. 復旧操作の提案順序と停止時の扱い

1. 運用者はincidentを作成し、全通常instanceの自動再公開が無効であること、他の復旧jobと運用者が対象を処理中でないことを確認する。曖昧なら停止する。
2. 読取専用のDB観測とprocess / providerの証拠を集め、§2の対象snapshotと停止確認を記録する。`PUBLISHED` / `PROCESSING`では停止確認を必須とする。
3. 正式に承認された実行者が対象1件の復旧を要求する。専用processはCP8型lockを取得し、DBのstatus・試行回数と停止確認を再照合する。lock競合時は処理せず終了する。
4. 対象だけを明示再送し、lock connectionを終端状態の確認まで保持する。status、試行回数、provider受理、log / metricを同じevent・publication IDで照合する。無期限待機や一括再送は行わず、timeoutとbatch上限を正式設計で決める。
5. COMPLETEDまたは再FAILEDの結果、provider側の受理件数、他publicationが変化していないこと、Auditの結果を記録する。送信結果が不明なまま同じ確認を使って再試行しない。

| 観測・結果 | 即時の扱い | 再開条件の案 |
|---|---|---|
| `STOP_UNCONFIRMED` | 再送なし。元processの停止証拠を取得し直す | 対象世代・publicationに結び付く新しい確認 |
| `STALE_SELECTION` | 再送なし。DBと他の運用者の操作を再照合 | 新しいsnapshot、必要なら新しい承認 |
| `CONTENDED` | 別workerがlockを保持。並走せず作業責任者へ通知 | 保持workerの終端確認後に再観測 |
| 試行上限到達またはprovider受理不明 | 自動・手動の追加再送を保留し、通知と外部受理記録の照合 | 運用Owner / provider Ownerの判断、新しい操作記録 |
| `LOCK_LOST`または復旧process異常終了 | 対象と同一eventの追加再送を停止。旧processが外部送信中だった可能性を扱う | 旧process停止とprovider副作用を再確認し、新しいsnapshot / 停止確認 / 承認を取得 |
| 完了待ちtimeout・Audit書込失敗 | 成功と推定せず停止。DBとproviderを読取専用で照合 | 障害原因と残存processを確認し、A1 / Security Ownerが再開判断 |

`LOCK_LOST`のfixtureでは送信前の位置でJVM終了コード70とstub送信0件を観測した。
実providerの送信中や検知前の競合窓については安全性を実証していない。
送信受理後の副作用はrollbackできないため、失敗時はpublication行の削除や手動status変更で
「戻す」ことを禁止し、providerとの照合と冪等再送の可否を判断する。

## 5. A1 blocking reviewへ渡す決定と実証条件

| 決めること | 現在のEvidence / 不足 | 判断Owner候補 |
|---|---|---|
| 停止確認の発行元、process世代の識別、有効期限 | fixtureのファイルは内容一致だけを検査。真正性・生存判定・複数operatorの取得競合は未検証 | 運用Owner、Security Owner |
| 再送入口の認可、Audit、複数operatorの単一作業管理 | Phase 2 Auditは利用候補。PL2 Toolingは権限・Auditを実装していない | Security Owner、A1 Owner |
| lock喪失検知前の窓と外部送信fencing | fail-stop後の送信前0件は確認。検知前の重複と送信受理後の副作用は残る | A1 Owner、provider / A2 Owner |
| provider冪等key、応答不明、重複受理の扱い | stubではkeyあり1件、なし2件。実provider契約は未取得 | provider / A2 Owner |
| 試行上限、timeout、batch、alert、結果の運用照合 | filterとFAILED gaugeはToolingで成立。閾値・通知先・運用Ownerは未決定 | A1 / D1 / 運用Owner |
| package済みReferenceでの復旧実演 | PL2はRoot Reactor外fixture。DoD 4-2 / 4-3 / 4-4の正式PASSではない | A1 / A2 / D1 Owner |

正式設計では、少なくとも複数operatorの同時要求、確認期限切れ、元process生存、
lock接続だけの喪失、外部送信前・受理直後の停止、provider応答不明、Audit失敗を
package済みReferenceと独立した検証手段で再現し、各停止条件と担当者の動作を確かめる。
上記のblocking issueが閉じるまで、このrunbookをproductionへ適用したり、ToolingのPASSを
P4-F通過またはDoD PASSへ読み替えたりしない。
