# 作業7：S1安全性・復旧条件の具体化（2026-10-05）

**状態:** DRAFT / A1・A2・D1 blocking reviewへの入力。S1完遂を目指す候補化判断は承認済み。具体的方式・実装開始・Gate・DoD判定は未承認。
**Ownership:** Framework側の計画文書。Reference実行機能、Tooling検証、実案件providerの保証を区別する。
**確認baseline:** `docs/daily-development-workflow` / `781a6a2`。前段の文書3ファイルは未commit差分として存在。既存文書・Tooling sourceを読み取り照合し、新しいMaven / Docker検証は行っていない。
**目的:** [S1範囲照合](phase4-s1-a1-a2-d1-scope-mapping-20261005.md)のM5を中心に、M1 / M4 / M6 / M7との依存、安全条件、停止・再開条件、必要Evidenceと未決事項を具体化する。
**入力:** [復旧runbook案](phase4-pl2-publication-recovery-runbook-draft.md)、[PL2 Evidence§3.1〜3.4](../architecture/validation/phase4-pl2-level2-verification.md)、[CP8単一実行契約](../architecture/validation/phase1b-cp8-single-execution.md)、[S1決定記録](phase4-s1-completion-direction-decision-20261002.md)。

## 1. S1の復旧方式候補と保証の分担

**Ownerの運用方針（2026-10-05）:** DB状態だけでは再送要否や処理の到達点を捉えにくく、process、provider受理、log、Audit等を合わせた総合判断になる。実運用の担当者が情報を確認し、再送・保留・調査継続・エスカレーションを判断することを前提とする。Frameworkの観測・選別・認可・競合制御と、人による確認・判断を組み合わせ、非同期復旧を運用として成立させる。この方針を捉えて計画作業を続ける指示を受けた。

人の確認・判断は正式な復旧手順の構成要素である。判断材料の取得方法、確認者、判断理由、結果の照合と記録を実演する。確認しても送信結果が不明な場合は不明として保留し、担当者の裁量で冪等性・認可等の技術条件を省略する運用にはしない。担当者数・二者承認・監視頻度・自動化水準は別途reviewで決め、今回一律には要求しない。

全通常instanceの起動時自動再公開を無効とし、対象1件を認可・Audit付きの専用復旧processから明示再送する既存案を具体化する。復旧process同士の排他はCP8型lockを候補とするが、通常listenerの停止と外部副作用の重複抑止は追加条件として扱う。手動再送も同じ安全条件を満たす必要がある。

| 層 | 安全性の条件案 | この層だけでは判断できないこと |
|---|---|---|
| 業務確定・保存 | 承認・Business Audit・publication保存のtransaction整合をreview。rollbackした承認から通知しない。通知失敗でcommit済み承認を取り消さない | provider受理とDBの原子的な確定は今回の候補で保証しない |
| 対象選別 | publication / event / listener、status、試行回数を観測し、lock取得後・送信直前に再照合 | snapshot一致だけでは他listenerの実行不在を証明しない |
| 実行競合 | 通常listenerを含む元実行の停止・drainを確認し、専用復旧者の競合を制限する | 復旧lockは通常listenerを止めず、喪失から検知までの窓が残る |
| 外部副作用 | 同じ論理通知へ安定した冪等keyを使い、provider受理不明時も照合・重複抑止する | アプリ内通知logの存在やJVM停止だけでは外部送信済みの副作用を取り消せない |
| 操作統制・観測 | 認可・Audit・上限・timeout・alert・相関を合わせ、拒否と未確定結果を記録する | logだけから送信成功やDoD PASSを推定しない |

本書の条件は設計提案であり、実装済みの保証ではない。schema、型、API、設定名、閾値、正式担当者は固定しない。

## 2. 対象状態と再送許可の条件案

`FAILED`やstale化はDB状態の観測であり、生存中listenerの停止証明ではない。PL2にはscheduled monitorによる状態変更があるため、元実行が残る可能性をstatusにかかわらず評価する。

| 状態 | 必要な確認 | 再送を保留する条件 |
|---|---|---|
| PUBLISHED | 保存された対象と元process / executorの結び付け、未処理taskが別instanceへ引き継がれないこと、停止証拠 | 経過時間だけで選別、元実行の識別不可、再起動・別instanceへの引継ぎが不明 |
| PROCESSING | 元listenerの停止、外部送信の開始・受理有無、同じ通知key、DBとproviderの照合 | listener生存・送信中・受理不明を安全に扱えない |
| FAILED | 原因・試行回数・外部副作用と元実行の終端。stale化によるFAILEDならPUBLISHED / PROCESSINGと同等の停止確認 | FAILEDだけで再送可と判断、実行終端不明、上限到達、受理結果・重複抑止不明 |
| COMPLETED | provider受理・通知logと完了記録の整合。復旧対象から除外 | 再送対象へ含まれる、完了でも結果に矛盾がある場合は読取照合へ移す |

全通常instanceの実効構成と再送入口を確認し、想定外の自動再送・scheduler・運用者の入口があれば操作を保留する。初回listener処理は自動再公開の無効化だけでは停止しない。

## 3. 安全条件と検証出口

以下のS-IDは計画上の追跡用であり、新しいruntime enum / 終了コード / Public APIではない。Owner欄は責務候補である。

| ID / 論点 | 条件・設計材料 | 必要Evidence / 負例 | 欠落時の扱い・Owner候補 |
|---|---|---|---|
| S-01 停止確認の真正性 | process / container IDと起動世代、deploy revision、listener、対象publicationを結ぶ。発行主体・証拠保管先・有効期限を定め、platform終了状態と独立したlog等で照合。PID再利用や再起動を識別 | 元process生存、別世代、虚偽 / 改変確認、期限切れ、確認後の再起動を拒否。対象taskが別processで継続しない証拠 | 停止を認定できなければ送信なしで保留。A1 / Security / 運用 |
| S-02 通常listenerとの競合 | 対象の実行所有者を追跡する方式、またはS1検証環境の関連processを停止・drainして再起動を制御する方式を比較。全停止を選ぶ場合の範囲・可用性・再開順も明示 | 生存listener中の再送、別instanceへの引継ぎ、stale化後FAILEDの再送、停止確認から送信までの変化 | 復旧worker lockだけでは許可しない。A1 / Reference / 運用 |
| S-03 複数復旧者 | 同じ環境・復旧taskのlock対象を定め、専用connectionを終端確認まで保持。lock取得後に対象snapshotを再照合。複数operatorを同じincidentへ集約 | 同時要求、古いsnapshot、同じeventの別publication、先行者異常終了後の再要求 | 競合者は送信せず終了。結果と旧processを確認して新しい要求へ。A1 / 運用 |
| S-04 lock喪失 | 喪失を検知した旧workerのfail-stop、検知前に新workerが動く窓、旧worker送信中の副作用を分けて設計。provider冪等性と必要ならfencingを比較 | 送信前・送信中・受理後それぞれでlock接続だけを失わせ、新workerとの重なり、受理数・状態更新を確認 | 新lock取得だけで安全と推定しない。旧processとproviderの照合ができるまで追加再送保留。A1 / A2 |
| S-05 通知識別・冪等性 | event / publication / 論理通知 / provider keyの関係を設計。同じ論理通知の初回・再送でkeyを維持し、別の通知を誤抑止しない。keyの保持期間、payload不一致・期限切れ・重複要求の結果も定義 | 同一通知の再配送・並行要求はstub受理1件、別通知は個別受理。同じkeyで異なるpayload、期限切れ、keyなしを負例化 | retry IDを送信keyにして毎回新規受理させない。識別・契約未成立なら保留。A2 / provider |
| S-06 provider受理不明 | 送信のtimeout / 切断時、未送信・確実な拒否・受理済み・不明を区別。同じkeyによる再要求 / 照会で受理を特定できる契約を設計。照会できない場合に安全な再要求が可能かもreview | 受理した後に応答だけ失う、受理前切断、重複応答、照会失敗、stubと通知logの矛盾 | 不明を未送信へ置き換えない。照合か安全な冪等再要求が成立しなければ保留。A2 / 運用 |
| S-07 DBと外部副作用 | publication、通知log、provider受理の各確定順・transaction境界を列挙し、各間の停止を設計。provider受理を理由にDB行を直接完了へ書き換えない | 受理後・通知log保存前、通知log保存後・publication完了前の停止から同じkeyで復旧し整合を確認。DB更新失敗も負例 | アプリ内logだけで重複防止と説明しない。不整合は照合へ。A1 / A2 |
| S-08 認可・Audit | HTTP Sessionに依存しない復旧主体、対象環境・操作権限、理由、承認要否、拒否 / before / after / 結果をPhase 2契約へ照合。送信前記録と送信後結果記録のfailure semanticsを定める | 未認証・権限不足・対象外操作、送信前Audit失敗、送信受理後Audit失敗、Audit情報の非露出 | 送信前に認可・必要Auditが成立しなければ副作用なし。送信後の記録失敗は成功とも未送信とも断定せず再送保留。Security / A1 |
| S-09 上限・timeout・alert | 再送回数、待機timeout、1操作の対象数、復旧許容時間、上限解除・代行・通知先を定める。DB接続保持時間も検証 | 上限到達・timeout・通知不達・代行不在、解除権限拒否、他publicationへ非波及 | timeout時も送信済みの可能性を扱う。閾値・担当未取得はOPEN。A1 / D1 / 運用 |
| S-10 監視・相関 | publication年齢とFAILED遷移滞留を区別。event / publicationから初回・retry / job・provider受理・Auditを追跡し、traceとlogの関連、PII / cardinalityを設計 | 初回 / 別process再送・次requestの非漏えい、metric / alert時刻とDB状態の一致、sink障害 | MDC markerだけで実trace PASSにしない。追跡不能なら統合判定保留。D1 / A1 |
| S-11 保持・パージ | 単一実行、保持期限、COMPLETEDのみの削除、復旧中・未処理の保全を定める。provider keyと通知logの保持を再送可能期間へ照合 | 期限前後・2起動・lock喪失・復旧との競合。FAILED / 未処理を残し、期限後の誤再送・key失効を確認 | パージlockと復旧lockの関係は未確定。記録保全と安全性が成立するまで有効化保留。A1 / C3 / 運用 |

S-05の「論理通知」は検討概念である。eventを唯一keyにできるか、将来の宛先 / 通知種類を含めるかはA2 reviewで決める。publication IDは配送記録、retry / job IDは操作の識別に用いる候補であり、外部重複抑止keyと自動的に同一視しない。

## 4. 外部送信境界の照合順序案

| 停止・障害の位置 | 復旧時に照合すること | 安全条件が満たされた場合の候補動作 |
|---|---|---|
| 業務transactionのcommit前 | 承認 / Audit / publicationがcommitされたか | rollbackした承認は配信しない。保存不整合はA1設計へ戻す |
| 保存後、listener処理前 | publicationと元executorの停止、初回が遅れて実行されないこと | 同じ通知識別で対象のみ再送 |
| 送信開始後、応答取得前 | provider未受理か、受理済みか、不明か | 受理照合または契約化された冪等再要求。判別も重複抑止もできなければ保留 |
| provider受理後、通知log保存前 | provider受理keyとevent、DB残存状態 | 同じkeyで重複副作用を抑止し、通常の復旧経路でDB整合を回復する案 |
| 通知log保存後、publication完了前 | log・provider受理・publicationの整合 | logだけで処理を省略せず、受理証拠と完了条件を照合する案 |
| 完了後、Audit / 観測の結果記録失敗 | 実際の完了・外部受理と欠落した操作記録 | 再送せず証跡回復をreview。外部副作用の取消しを推定しない |

この表は実行コマンドやDDLを指定しない。状態の修復方法はA1 / A2 reviewで決め、publication削除・手動status変更・key変更による再投入を復旧の代替にしない。

## 5. 停止後の再開条件案

停止時は対象publicationと同じ論理通知の追加再送を保留し、incident、最終snapshot、旧process・provider受理・Auditの結果を残す。他の独立packageの計画は継続できる。

再開には、旧実行の停止 / 引継ぎ先不在、lockとDBの新しい観測、同じ通知keyでのprovider照合、権限・必要Audit、試行上限と原因対応、担当 / 判定者の確認を揃える。旧停止確認・旧snapshotを次の試行へ使い回さない。未確定の外部副作用は保留件数と滞留として観測する。

## 6. blocking reviewに残す判断

| 判断 | 関連ID | 未取得 / 未確定の入力 |
|---|---|---|
| S1の停止確認方式 | S-01 / 02 | process所有者の識別可否、限定環境の停止・drain方式、platform証拠・発行主体・有効期限・再起動制御 |
| lock喪失窓と外部副作用 | S-03〜07 | 通知key契約・保持期間、応答不明時の照会 / 再要求、fencing要否、DB確定順・古いworkerの更新制御 |
| 正式再送入口とAudit | S-08 / 09 | 配置・配布単位、非HTTP主体、権限・承認者、Audit分類・failure semantics、timeout・上限・代行 |
| schemaと観測 | S-07 / 10 | 既存kkref履歴との整合、通知log / FAILED時刻の所有、別processの相関、実trace・sink / alert、負荷・非露出 |
| 保持・パージとC3 | S-11 | 保持期間、再送可能期間とprovider key寿命、実行基盤・lock競合、cleanup・証跡保全 |
| 核心DoDの判定解釈 | 4-2 / 4-3 | 手動復旧を4-2へ適用する条件と許容時間、stub受理一意性による4-3の検証範囲・負例。Owner判断前にPASSへ読み替えない |

## 7. 既存材料の限界と次の出口

[ProviderStub](../../build-support/phase4-level2-verification/src/main/java/org/koikifw/buildsupport/phase4/ProviderStub.java)は別transactionでevent IDを一意keyとして受理を記録する。[NotificationProbe](../../build-support/phase4-level2-verification/src/main/java/org/koikifw/buildsupport/phase4/NotificationProbe.java)は受理後の停止・失敗を模擬する。これはToolingの単純化した通知モデルであり、正式通知key・providerの保持期間・payload不一致・通信応答喪失を保証しない。

[ExclusiveRecoveryProbe](../../build-support/phase4-level2-verification/src/main/java/org/koikifw/buildsupport/phase4/ExclusiveRecoveryProbe.java)と既存Evidenceは、確認情報と対象snapshotの一致、lock維持中の復旧worker排他、送信前のlock喪失検知後停止を示す。停止確認の真正性、通常listener・検知前の窓、正式認可 / Auditは未解決である。本書の記載で既存Toolingの確認範囲を拡大しない。

次はS-01〜11をDoD 4-1〜4-5・4-12の実演単位へ対応付け、正常 / 拒否 / 競合 / 停止位置、Referenceと独立Toolingの構成、採取する証拠とPASS / FAIL / BLOCKED条件を揃える。担当・環境・cleanup・概算 / 上限の取得は続く段階で行う。

**後続資料:** [S1 DoD実演計画・人による確認判断](phase4-s1-dod-demonstration-plan-20261005.md)へ、運用方針とS-01〜11の対応、case別の操作・証拠・判定条件を整理した。実演実施とDoD判定は未実施。

今回、安全条件と検証出口を文書化したが、安全方式の採用・blocking review完了・作業7全体・S1完了は宣言しない。production code、POM、Rules、migration、CI、既存Gate・runbook正本、remote操作は変更していない。
