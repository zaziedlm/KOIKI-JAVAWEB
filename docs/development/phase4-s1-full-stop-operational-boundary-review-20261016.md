# S1-06：全停止型の最小確認とHOLD境界（2026-10-16）

状態：**SOURCE/既存契約に基づく方針・制限はOWNER APPROVED（§6）**。E08並行/並走方針はOwnerが`586db9b`へcommitし、clean sourceを確認した。今回は運用確認の最低条件を文書化する。全停止型を実運用へ配備した認定や、process停止/復旧の実行開始票ではない。

## 1. 目的と適用範囲

採用済みの「通常listenerと復旧を並走させず、停止/受理が不明なら人手HOLD」を実務で判断できる形にする。新しい分散lock・fencing・停止管理table・自動復旧を作らない。対象一件の環境と、その通知を処理し得るReference instance/再実行入口を確認範囲とし、無関係な別環境まで停止させる前提を置かない。

現在のFAILED受理済み一件は、人手HOLDを維持する方針である。本書の確認が揃ってもAの実行や現行runner起動へは進まない。全停止は将来の承認済み復旧の必要条件であり、実行許可・肯定proof・consumeの代わりではない。

## 2. 現行sourceの確認範囲

- [隔離runner設定](../../koiki-reference-app/src/main/java/org/koikifw/reference/notification/configuration/IsolatedPublicationRecoveryConfiguration.java)は指定PIDの不存在、開始時刻文字列のparse、non-Web/async/自動再公開無効、期待JDBC URL等を検査する。指定PID以外のinstance、再配置、停止確認の出所/真正性を検査する実装ではない。
- [Reference executor](../../koiki-reference-app/src/main/java/org/koikifw/reference/notification/configuration/NotificationAsyncConfiguration.java)はprocess内1 worker、queue16、shutdown時のtask終了待ち/30秒待機を設定する。この設定だけで全task終了、別instanceへの引継ぎなし、provider側の受理処理終了を証明しない。
- [復旧lease](../../koiki-reference-app/src/main/java/org/koikifw/reference/notification/adapter/outbound/operational/JdbcIsolatedPublicationAdapter.java)のsession lockと接続検査は通常listenerを停止させず、喪失検知までの窓も残る。詳細は[既存S-01/02等](phase4-s1-safety-and-recovery-conditions-20261005.md)と[E08§7〜8](phase4-s1-e08-sequential-replay-boundary-review-20261016.md)を引き継ぐ。

今回はsource読取であり、実platformの停止・全instance列挙・drain・再起動抑止の観測や追加テストは行っていない。

## 3. 人が確認する四点

既存の一件の運用記録へ、次の確認結果・出所・時点・担当を参照として残す。新しいチェック台帳や停止状態enumは増やさない。

| 確認点 | 必要な内容 | 不足した場合 |
| --- | --- | --- |
| 対象範囲 | 対象環境/publication/listenerと、それを処理し得るinstance・起動世代・再実行入口の対応。確認元は運用platform等の管理情報 | 一つのPIDが消えたことだけで全停止とせずHOLD。instance一覧/所有者の確認へ引継ぐ |
| 元実行の終了 | 関連instanceの終了状態と、実行中/待機taskの扱い。送出済み要求はprovider受理証拠と照合 | timeout・取消し要求・終了待ち設定だけで未送信とせずHOLD。provider処理が残る可能性を記録 |
| 確認後の非並走維持 | 復旧を行う区間の新規処理流入・自動再起動/再配置・別経路再実行をどう防ぐか、管理責任者と確認可能な根拠 | 再起動抑止等を確認できなければ復旧を開始しない。新しい並走モデルへ切り替えない |
| 再確認と引継ぎ | 操作直前の現在状態、監視/連絡の担当、条件変化時の連絡先、復旧後またはHOLD時の通常再開判断者 | 古い停止記録を現在の肯定proofへ流用せずHOLD。担当不在/観測不能も同様に扱う |

platform・担当・確認手段・時点/有効期間・停止範囲と業務影響は未取得でOPEN。特定のクラウド、scheduler、container運用を推測で前提にしない。人が記録しただけで、非並走が技術的に保証されたとは扱わない。

## 4. 条件が崩れた場合の最小処置

| 判明した時点 | 扱い |
| --- | --- |
| 復旧開始前 | 開始しない。consumeや新permit発行で確認不足を埋めず、人手HOLDへ戻す |
| consume後・復旧開始前 | 消費済み・実行未確認として保持。読取やprocess再起動で自動再開せず、対象/停止/受理を照合する |
| 復旧開始後・通知送信/受理の前後が不明 | 追加の復旧要求を出さない。既存処理の終了と受理を担当者が照合する。停止・取消し要求を副作用停止の証拠にせず、必要な実操作は運用責任者の権限/判断に従う |

送信前/中/受理後を自動的に完全判定する仕組みを新設しない。未確定なら結果をHOLDへ残し、[人手引継ぎ](phase4-s1-r4-human-hold-handoff-guide-20261016.md)の証拠参照・禁止事項・担当/次確認を更新する。lease喪失・TTL切れ・permit消費は、旧処理の無害化や再投入許可ではない。

## 5. 通常再開と残課題

**未解決HOLDのために全環境を無期限停止し続ける運用は採用しない。** 一方、未解決publicationを抱えたままの通常再開を本書で自動許可もしない。通常起動時の再公開/滞留task/別instanceへの引継ぎと、対象通知が意図せず再処理されないかを運用責任者が確認し、業務影響と合わせて別判断する。現行runnerの自動再公開無効検査は隔離復旧processについてのもので、通常再開全体の保証にはならない。

**Owner承認方針（§6）：四点の確認が揃わない場合は復旧を開始せず、人手HOLDを維持する。条件変化後は追加要求を止め、既存処理と受理の照合へ戻す。** 正式な停止維持・通常再開手段が必要になった際にだけ、実platformの資料を入力に不足を限定して設計する。

S1-06/E11〜14全体、D12/fencing、遅延要求の無害化、実provider、E08/DoD等は未達を維持する。これは運用提供用の境界案であり、運用配備・停止操作・復旧実行・追加実証・成果commit・remoteの承認ではない。

## 6. 最小確認・HOLD境界のOwner承認（2026-10-16）

Ownerは本書を確認し、確認不足なら開始せずHOLD、開始後に条件が崩れたら追加要求を出さず照合へ戻すことを了解し、承認した。これを**本書の最小確認・HOLD境界の方針と制限のOWNER APPROVED**として記録する。

§3の四点と§4の時点別の扱いを判断材料とし、全停止の確認を実行許可・肯定proof・consumeに置き換えない。HOLDによる無期限停止を前提とせず、通常再開も別判断とする。既存記録の人手HOLDと不明時の照合を維持し、新しい並走/停止管理機構を追加しない。

実platform・担当/連絡先・停止維持/通常再開の具体手段等はOPEN。S1-06/E11〜14全体や技術的保証の成立、運用配備/引渡し済みとは扱わない。process/DB/provider操作・実装・追加検証・成果commit・remoteの開始承認は含まない。
