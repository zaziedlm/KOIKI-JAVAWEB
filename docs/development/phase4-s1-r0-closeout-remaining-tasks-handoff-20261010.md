# S1 R0区切り・残作業と次工程の引継ぎ（2026-10-10）

**R0の規約限定改訂・必要最小限の検証はCOMPLETE / OWNER ACCEPTED。R0の追加検証タスクは0件。** Ownerは2026-10-10に「最終結果を確認し、Owner受け入れを承認します。」と明示した。次の作業はR1開始案の再確定であり、Reference非同期実装の開始とは区別する。

## 1 今回閉じる範囲

[受入票](phase4-s1-r0-root-connection-limit-correction-owner-review-20261009.md)と[成立結果](../architecture/validation/phase4-s1-r0-rules-source-fixed-preflight-20261009.md)を根拠とする。

| 必要集合 | 受入結果 |
|---|---|
| Rules新規40＋既存67 | 107 PASS、rootとの重複加算なし |
| PL2既存規約 | 4 PASS |
| package・旧Consumer | 配布artifact検査・旧bytecode不変／互換性PASS |
| Reference既存E2E | 1 PASS、既存assertion維持 |
| 通常root・専用test分離 | 297実行＋92skip、失敗0。S1 64／B2 28だけskip、専用再検証なし |

source固定branchは`feature/phase4-s1-reference-foundation`、HEADは`c11dbbceb0241f1257f9bfc153105d8f55125c1c`。規約code／testはcommit済み。最終cleanup・資源・source照合も成立した。package／E2E入力とroot再packageは別artifact epochとして保持する。

監視pure74例・途中失敗・旧予算は[アーカイブ](../archive/phase4-s1-r0-20261010/README.md)、最小実行の途中経過は[専用アーカイブ](../archive/phase4-s1-r0-20261010/minimal-execution/README.md)に閉じる。現行進捗へ加算せず、追加網羅・専用92件の再検証を残作業にしない。初回SQL観測失敗の詳細原因UNKNOWNは履歴に残し、現在の追加原因探索タスクにはしない。

## 2 区切り時点の残作業

| 項目 | 状態・完了条件 |
|---|---|
| Owner受入の文書反映・次作業入口の整理 | 本書・受入票・結果表・規約票・目的対応表・R1草案へ反映済み |
| 受入記録と履歴分離のGit固定 | 未実施。文書差分を点検し、Ownerの操作前承認またはOwner操作でlocal commitする。codeの再commitや変更破棄は不要 |
| 端末内証拠の保持 | 固定manifest／raw／JARを保持。受入前manifestは変更せず、受入記録を別ファイルへ保存。別端末へ移る場合だけ証拠移送と環境preflightを別途準備 |

証拠所在は`tmp/r0-rules-9c41aa4-20261009-first/minimal-preparation-20261010/`。Gitだけでは端末内証拠・artifact・実行状態は移らない。最終検証予算は累計2567秒・残1033秒、cleanup予約300秒の時点で閉じる。残時間・旧再実行枠をR1へ引き継がない。

## 3 次に行う作業：R1開始案の再確定

[既存R1開始草案](phase4-s1-r0-reference-async-start-review-20261009.md)を更新し、Ownerが個別に開始判断できる有限な案にする。R0-09の規約改訂・検証・結果受入は成立済み。R0-01〜08のReference限定採用／R1開始判断は未承認である。

1. 受入済みLevel選択APIに草案を合わせる。Level 0／1の共通拒否を維持し、notificationだけLevel 2を選択する候補を具体化する。
2. Reference正常非同期経路と通常構成保護に必要なケースを目的へ対応させる。草案の新規5 class／20 caseと既存S1専用64件は未採用候補として再評価し、影響のない集合を機械的に再実行しない。
3. 通常rootの現行基点389 invocation＝297実行＋92skipを用い、変更後の集合を再集計する。草案の349／365／273は旧基点であり使わない。
4. 各段階の資源・時間・再実行・cleanup／停止条件を確定する。R0通常rootの上限JVM4／PG1／Ryuk2／client14と、R1草案のRyuk1／client8はそのまま併用できない。通常統合確認と選択非同期検証の必要条件を区別して審査する。
5. Reference-owned依存・設定・event／key・migration・provider stubと正本改訂の最小差分を提示する。Owner個別承認・必要文書のsource固定・clean source／preflight成立後にだけ作成・検証へ進む。

この整理で規約107件の再審査・再実行を繰り返さない。既存業務・Security・Audit・同期vetoの保護を開始条件とする。R1の資源枠・ケース数・回帰範囲を、本書によって承認済みにしない。

## 4 以降の見通しと判断時期

| 順序 | 主目的 | 判断するタイミング |
|---|---|---|
| R1 | Reference正常経路、通常無効、同一transactionのpublication保存、通知keyとprovider stub受理 | 次の開始案再確定・個別承認後 |
| R2 | provider失敗と業務rollback／拒否の分離、誤送信防止 | R1結果と残課題を確認後、必要ケースを選択 |
| R3 | 停止／復旧・安定key・重複受理抑止・UNKNOWN時の保留 | 開始前に全面停止／drain方式か、live復旧とfencing方式かをOwner判断。現在は未選択 |
| R4 | 許可issue／consume／closeと復旧の認可・Audit接続 | 復旧契約確定後。B-C01〜10／D11／D12等の未達を維持 |
| R5 | 保持／purge・event／publication／key／Auditの相関 | 保存・復旧・運用契約が整ってから |

後段の候補ケースは各段階の開始時に必要性を確認する。監視の網羅拡張は具体的な未観測リスクが実行を妨げる場合だけ別途判断する。R0受入でDoD 4-1〜4-5／4-12、実provider配信、正式受渡し、Phase 4全体を完了扱いにしない。

今回の区切り作業は文書反映・証拠の受入追記・差分点検まで。build／test・R1実装・local commit・remoteは実行しない。
