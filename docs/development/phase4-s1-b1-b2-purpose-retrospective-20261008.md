# Phase 4 S1 B1／B2の振り返りと目的適合確認（2026-10-08）

状態：`SESSION REFLECTION RECORDED / DIRECTION REVISED（2026-10-09、§5〜§6）`。Ownerとの振り返りを記録する。B1・B2初回issue／readは受入済み。目的適合の総評と今後の進め方の提案であり、後続操作・Phase 4全体・DoDの開始／完了承認を追加しない。

## 1. Phase 4とS1の位置づけ

Phase 4 Enterprise Integrationは、Phase 3で成立した業務・Security基盤に、非同期通知、外部連携、Batch、観測、実行環境等を加え、利用・運用できる条件を実証する段階。対象棚卸しは[見直し計画§4.1](KOIKI-JavaWeb-FW_Phase4実施計画_見直し草案_v0.1.md#41-gate後の実行work-package候補)を参照する。Customerの案件固有実装、Referenceの利用実証、Frameworkの共通契約、Toolingの故障注入・検証支援を分離する。

現在はPhase 4全体の一括開始ではなく、Gate P4-F下の個別限定承認経路。S1は主にP4-01の通知・耐久配信・復旧とP4-08の非同期観測へつながる用途限定Reference実証である。S1成果を複数Consumer向け共通契約・正式提供へ広げるS2は、案件適合と必要性に応じた別判断。

[S1方針§3](phase4-s1-completion-direction-decision-20261002.md#3-s1で完遂を目指すもの)の出口は、expense承認結果からnotification・provider stubへつなぎ、次を実証すること。

- 通知失敗で確定済み承認を取り消さない（DoD 4-1）。
- 保存後・処理中・provider受理直後の停止から、合意した復旧条件で配信を完遂する（4-2）。
- 再送・並行要求・受理不明時の外部副作用を重複させない条件と負例を示す（4-3）。
- 監視、認可／Audit付き復旧、保持・単一実行パージ（4-4／4-5）、非同期・別processの追跡（4-12）を確認する。
- 人がDB・process世代・provider受理・Audit等を照合し、再送／HOLD／調査／エスカレーションの理由と結果を残す。

## 2. B1／B2までの流れと役割

| 段階 | 確認したこと | S1への意味 |
|---|---|---|
| 保存・認可・Audit基盤／段階A | permit・append-only消費、一度性・rollback、対象と証拠の照合 | 復旧操作を無条件に実行させない基礎 |
| B1読取接続 | Tooling供給元から対象・停止・provider観測・証拠を読取り、曖昧・不明・矛盾を拒否 | 復旧判断の材料を接続できるか確認 |
| B2初回issue／read | 凍結sourceの保護、issueのcommit／rollback、permit保存・read・認可・Audit | 読取結果を実際の許可発行へ安全に使えるか確認 |
| 後続（未開始） | consume／closeと送信・復旧・結果照会／照合の接続 | S1の耐久配信・冪等性を実証 |

B1は[受入記録§24](../architecture/validation/phase4-s1-stage-b1-read-connection-20261008.md#24-b1完了記録のowner承認2026-10-08)、B2は[受入記録§19](../architecture/validation/phase4-s1-stage-b2-issue-read-20261008.md#19-b2限定検証結果のowner受入承認2026-10-08)による。B2は新規54＋回帰301＝355件成立。artifact epoch・保存結果の非影響条件を区別し、全件を同一classpathで今回反復したと扱わない。

B1で一度読んだ肯定値は、使用前に対象・停止状態が変われば使えない。B2は限定凍結sourceから初回issueのtransaction完了まで保護する方式を実証した。この前進は妥当だが、送信時・consume使用中までの全保護を実証した結果ではない。

## 3. 総合判断と進め方

**目的に即した方向で進んでいる。B1・B2はS1完遂のための安全基盤であり、通知配信・停止後復旧の中心実証はこれから。** 安全な拒否／保留の成立と、合意した条件での配信・復旧完遂を別々に評価する。

今後は許可基盤や検証harnessの整備を目的化せず、各タスクをS1の検証出口へ結び付ける。consumeの保存成立を送信許可・配送成功に読み替えず、送信までの保護・責任境界を設計reviewで明示する。

次回の入口として、S1全体の「成立済み／未成立／次に必要な証拠」を一枚の対応表へ整理し、その中でconsume reviewの範囲を決めることを提案した。この対応表は本セッションでは未作成。後続方式・差分・検証上限を採用した記録ではない。

B2途中のAgent側test／script前提誤り・環境artifact不一致は[総評§20](../architecture/validation/phase4-s1-stage-b2-issue-read-20261008.md#20-b2検証全般の総評2026-10-08)に記録済み。実行前の期待値・command、受入artifactと解決依存の照合、隔離compile・exit先行保存・有限log処理を必要範囲で入口に織り込む価値がある。

## 4. 残課題とsession区切り

肯定consume／close、D11歴史対象閉鎖・別publication横断抑止、D12後続競合／網羅性、実運用停止／drain・分散fencing、worker認証／委譲、provider delivery、backup／DRを保持する。Reference Level 2・Framework API／Rules／依存・正式配布・DoD・Phase 4全体開始は個別判断。

Ownerは本振り返りの記録と、別端末・別AIセッション向け引継ぎ文書の作成、2文書のcommitとここまでの作業のremote pushを指示した。[次session引継ぎ](phase4-s1-b1-b2-cross-device-session-handoff-20261008.md)を再開入口とする。raw／JAR／cacheはGitで転送されず、別端末での即時再実行保証はない。

## 5. 振り返り後の方針見直し（2026-10-09）

本振り返りの記録後、非同期処理の検証として定まった方向へ進んでいるかを、source・文書の読取りで追加点検した。

- Referenceには非同期の実装（Modulith依存・listener・event publication）がまだなく、S1以降のReference main追加は全てpermit制御面である。B1／B2の対象はTooling所有のview経由の`event_publication`である。
- permitは復旧許可の一意化と認可・Auditを担うが、旧processや並走workerの送信は止めない。DoD 4-3の重複抑止を主に担うのは送信境界の冪等keyである。
- B2の保護は全停止型で、ThreadLocalの保護scopeは非同期の送信へ持ち越せない。

§3の総合判断（目的に即した方向、中心実証はこれから）は維持する。そのうえでOwnerは、consume／close接続の深掘りより先に、Reference appへ最小の非同期経路を早期に組み込み、正常系→異常系→停止後回復の順に取り組む方向へ修正することを採用した。§3で次回入口とした「対応表の中でconsume reviewの範囲を決める」は、「対応表に担う機構を加え、R0開始判断資料を作る」へ置き換える。根拠・段階案R0〜R5・維持する境界・採用範囲は[軌道修正案](phase4-s1-async-reference-direction-correction-draft-20261008.md)（§9）に記録する。

この方針採用は、Reference Level 2 runtime・依存・code／migration・検証の開始承認ではない。R0の開始判断はOwnerが個別に行う。

## 6. 方針見直し後のB1／B2成果の位置づけ（2026-10-09）

Ownerは方針変更を判断したうえで、B1／B2までの作業は無駄ではないとの見方を示した。Claude Codeの評価も同じであり、成果を次の3つに分けて位置づける。評価はsource・文書の読取りに基づき、新しい検証結果ではない。

| 区分 | 対象 | 後続での使い道 |
|---|---|---|
| そのまま使う | `RecoveryPermitService`の実Identity・能力・管理scope認可、Business Audit・拒否時Security Audit、HOLD分類、transaction外境界。V4の追記のみの消費記録と、publication単位の未閉鎖permit一意制約。管理設定によるscope・TTL供給。`RecoveryTarget`の5項目（Modulith `event_publication`と対応） | R4（復旧操作の統制、DoD 4-4）でReferenceの復旧経路へ接続する。E11〜E17の受け皿 |
| 知見として使う | B1の読取・拒否規則（曖昧・不明・矛盾を肯定にしない、UNKNOWNを未送信としない） | R3のE06・E10で運用者が判断する基準 |
| 〃 | process台帳・起動世代・停止観測・子JVM資源制限 | R3の停止注入実験の土台 |
| 〃 | B2で確定した事実：照合直後の変化（D12）は再読取や短いTTLでは閉じず、書込み全停止でのみ安全になった | 軌道修正案§5の保護モデル選択の実証済み根拠 |
| 〃 | B2総評§20の手順上の教訓、既存回帰約300件 | R1以降の検証入口、Reference非同期組込み時の回帰安全網 |
| 置換見込み | B2凍結protocol（`b2_read.frozen_target` view、role NOLOGIN凍結）、`JdbcProtectedRecoveryTargetAdapter`、ThreadLocal保護scope | Reference自身のpublicationへ接続する際に作り直す。当初からToolingを供給元とする限定実証の暫定物である |

**総評：** 成果は無駄ではない。ただし順序の結果、割高になった部分がある。非同期経路を先にReferenceへ入れていれば、B1／B2は本物のpublicationを対象にでき、Tooling用viewや凍結protocolの一部は不要だった。段階ごとの開始票・訂正承認・固定manifestの負担も、成果物の量に比べて大きかった。

一方、統制側を先に固めたことで、R1〜R3では認可・Auditの設計を後回しにし、非同期の検証に集中できる。今回の方針変更はB1／B2を否定するものではなく、**成果をR4へ置き直し、順序を正すもの**と位置づける。

**教訓：** 統制や保護の仕組みは、対象となる本物の処理経路を先に用意してから作る。対象が仮の段階では、検証の上限とreviewの細かさを軽くし、置換見込みの暫定物への投資を抑える。
