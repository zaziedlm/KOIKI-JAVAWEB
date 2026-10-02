# S1検証完遂を目指す計画方針・案件協働とnon-Web境界の記録（2026-10-02）

**状態:** S1検証完遂を目指す進め方を計画上の候補とする判断は **DECIDED / OWNER APPROVED**。個別実行計画・Gate・実装開始は未承認。
**文書review:** 本書の内容は2026-10-02にArchitecture Ownerが確認し、問題なしとして承認（§8）。
**Ownership:** Framework側の計画・判断記録。Reference、Customer、Batch Framework、Toolingの成果物所有を分ける。
**記録の根拠:** 2026-10-02のArchitecture Ownerとの対話。技術材料は既存PL2 Evidenceと`koiki-batch-fw`のローカルsource点検。新しい実行検証はない。
**関連:** [CP-F0判断材料](phase4-cpf0-and-s0-replan-options-20261002.md)、[入口・先行範囲案](phase4-entry-and-forward-scope-options-20261002.md)、[作業一覧](pre-phase4-framework-independent-work-review-20260930.md)。

## 1. 今回の決定と承認範囲

Ownerは次を明示した。

> S1を完遂する進め方を、候補とすることは決定事項とし承認します

この承認により、**S1の用途限定Reference検証を完遂する進め方を、以後の計画具体化の候補とすることは決定済み**とする。S0 / S1 / S2を全て同じ未決状態へ戻して説明しない。S1の成立条件、検証出口、担当・環境・作業上限を、この方向に沿って具体化する。

「候補とする判断の承認」と「具体的な実行範囲・開始の承認」は区別する。今回の承認は、S1の完了、CP-F0の全項目確定、Gate P4-Fの設置・通過、A1 / A2 blocking review通過、Phase 4 production開始を意味しない。DoD、Public API、schema、dependency、Rules、CI、配布およびremote操作の個別判断も残る。

| 対象 | 今回の位置づけ |
|---|---|
| S1を完遂する進め方を候補として計画を具体化 | 決定 / OWNER APPROVED |
| S1の具体的対象・復旧契約・担当・工数 / 上限・環境・開始経路 | 後続の計画・reviewで確定 |
| S0 | S1成立条件が揃わない場合の再計画・判断材料として保持。自動切替しない |
| S2 | 案件との適合確認で必要性を判断する後続候補。今回の採用・実装承認ではない |
| non-WebとWeb同居の分岐 | §5のOwner設計意図を計画入力として記録。具体的構成・例外採用は未決定 |
| `koiki-batch-fw`との組み合わせ | Ownerが示した想定と点検結果を記録。正式依存・連携方式・本番適用は別判断 |

本書は今回の承認範囲の記録先である。グランドデザイン、既存R1〜R7、F-5、Gate規定はこの記録だけで書き換えず、後続の正本改訂へ差分を渡す。

## 2. この判断へ至った経緯

1. Ownerは、案件アプリチーム・顧客判断との協調とFAQ対応を続けながら、Framework側で成立する範囲を案件の判断時期に連動させず進める目的を示した。
2. 作業4b・5で、Framework自身の当初成果物・DoDに基づく実証目的と、Customerが実案件でLevel 2を採用する条件を分けた。案件の通知需要が未確定であることだけでは、Frameworkの実証を見送る根拠として十分ではないと整理した。
3. S0 / S1 / S2を実機能・利用場面・利用可能性で言語化した。S1は承認後通知の保存・配信・復旧を限定Referenceで実証する候補、S2は複数Consumer向けの共通契約・保守を成立させる候補とした。
4. Ownerは、S1を完遂して土台を作り、案件の設計・実装でS2の必要性が見えた場合にFramework側が並走協働して補完する方策を提示した。適合確認を設計初期から行い、保証範囲・変更影響を確認する進め方を対話で整理した（§4）。
5. ファイル到着・SFTP等のHTTP以外の起点について、S1の内部event配信だけでは外部受付・起動・再実行まで充足したと説明できないことを整理した。Ownerはnon-Web専用実行を推奨経路とし、Web内への限定的な組込みは可能だが推奨しない分岐を明確にした（§5）。
6. OwnerはBatch本体を別途Spring Batchベースで整備中であると説明した。ローカル`koiki-batch-fw`を点検し、既存Batchを組み合わせる検証の土台と残課題を確認した（§6）。
7. 対話の中心をS1へ戻し、Ownerが§1の候補化判断を明示承認した。Batch整備をS1の目的へ置き換えず、本書に経緯・決定・残る判断を集約する。

## 3. S1で完遂を目指すもの

具体的な対象案は[CP-F0判断材料§2.1](phase4-cpf0-and-s0-replan-options-20261002.md#21-s1の対象を限定する案)を用いる。Referenceのexpense承認結果を値だけのeventで伝え、Reference-owned notificationから非配布Toolingのprovider stubへ送る案である。event名・payload・API・schemaを今回固定しない。

| 完遂へ向けた検証軸 | 確認すること |
|---|---|
| 業務確定と通知失敗の分離 | 同期整合性・承認・業務Auditを維持し、通知失敗で確定済み承認を取り消さない（4-1） |
| 耐久配信 | 保存後・処理中・provider受理直後のprocess停止から、合意した復旧条件で配信できる（4-2・核心） |
| 冪等性 | 同じeventの再送・重複実行・受理不明時を扱い、通知の外部副作用が重複しない条件と負例を確認する（4-3・核心） |
| 監視・復旧・保持 | FAILED件数・滞留、認可 / Audit付き再送、完了publicationの単一実行パージ（4-4・4-5） |
| 追跡 | 非同期処理・再送のevent / publication / job等を追跡できる（4-12） |
| 適用条件の明文化 | 検証した構成・負荷・復旧条件、providerの冪等性、運用責任、未検証事項、Customer側で確認すべき条件を残す |

PL2 Toolingの既存成功を正式ReferenceのDoD PASSへ転記しない。停止確認の真正性、通常listenerとの再送競合、lock喪失検知前の窓、外部送信境界は未解決事項として扱う。手動復旧を選ぶ場合も核心DoDを省略せず、判定解釈・許容復旧時間を明示reviewする。

stubの受理は実メールの受信者への到達保証ではない。実providerへの適合は案件側の追加検証対象とする。S1完了をPhase 4全体の完了や一般向けsupport開始と説明しない。

## 4. S1を土台とする案件協働・S2判断の整理

以下はOwnerが提示した方策を対話で整理した計画入力であり、S2の採用承認ではない。

- アプリの設計初期からS1の適用条件と案件要件を共同照合する。実装・試験中もfindingを確認し、Framework側が並走して契約gapを補完する。
- 案件固有のevent・通知先・ファイルformat・Adapterで充足する要求と、Framework共通契約の拡張が必要な要求を分ける。案件要件の増加やHTTP以外の起点だけでS2と判断しない。
- S2が必要と見えた場合は、S1との差分、利用先、成果物Owner、互換性・migration、追加Evidence、実施 / 運用責任、作業上限、案件納期への影響を示して別判断する。
- 共通拡張が承認・検証されるまでは、S1の保証範囲を拡大して説明しない。S2の検討待ちを独立したFramework作業全体の停止条件にしない。

納期直前に重大な保証不足が判明することを避けるため、復旧・外部副作用・DB資源・監視 / Auditを適合確認の初期項目とする。具体的な共同review担当・時点は後続計画で定める。

## 5. HTTP以外の入口とnon-Web実行の位置づけ

**Ownerが示した設計意図:** non-Web専用実行へ`koiki-batch-fw`で設計・実装したバッチを組み込む。Web process内にも限定したファイル連携等のバッチ的処理を組み込むことは可能だが、推奨経路としない。

| 実行形態 | 計画上の扱い |
|---|---|
| non-Web専用process | Batch本体の推奨経路。起動・配置・停止・復旧・資源管理をWebと分離し、non-Webモードを明示する |
| Webが受付してnon-Webへ処理依頼 | WebとBatchの責務を分ける構成候補。受付・起動・結果反映の受渡し契約を別途検証する |
| Web内へ限定処理を同居 | 可能性は残すが非推奨。個別の範囲・資源上限・停止 / 再実行・Web影響をreviewする例外候補 |

分離は常駐処理の件数だけで決めず、長時間待機、処理量、同時実行、CPU / memory / DB接続、独立した停止・再起動の必要性で判断する。具体的な数値上限は未決定。

non-Webモード自体は長期transactionやDB接続占有を防がない。ファイル到着待ち・SFTP転送・待機ループを業務transactionで包まず、DB更新単位を限定し、transaction時間と接続保持時間を別々に検証する。chunk処理でもcursor等の接続保持を確認する。

外部ファイルの受信・転送完了・受付記録・重複判定と、業務確定後のLevel 2内部event配信は別の責務である。Spring Modulithのアプリ内配信が別processへの受渡しを自動的に保証すると説明しない。処理主体・権限・Audit・相関IDもHTTP Sessionへ暗黙依存させない。

## 6. Batch Framework点検材料と採用判断の境界

Ownerが示した連携先は[koiki-batch-fw](https://github.com/zaziedlm/koiki-batch-fw)。公開URLの取得は成功しなかったが、Ownerが提示したローカルclone `C:\KOIKI\koiki-java\koiki-batch-fw`を読み取り専用で点検した。

**点検baseline:** HEAD `c999298`、`git status --short`の変更表示なし。remote最新状態は未確認。Java 21、POMのSpring Boot 4.0.6、文書のSpring Batch 6.0.x（判断記録には6.0.3照合）を確認した。今回Mavenを実行して解決version・test PASSを再確認していない。

| 確認事項 | sourceから得た材料・残課題 |
|---|---|
| Ownership・実ジョブ | `components/libkoiki-batch` / Reference / `apps/*`を分離。Tasklet、DB取込chunk、file-to-file chunkの実装あり |
| non-Web起動 | Reference mainは実行後に`SpringApplication.exit`と`System.exit`を使用。`WebApplicationType.NONE` / `spring.main.web-application-type=none`の明示は今回の検索で確認できず |
| transaction・DB接続 | `CustomerImportJobConfig`がchunkとTMを明示。Readerは`JdbcCursorItemReader`。接続保持・pool・timeoutを実運用条件で確認する必要あり |
| 永続・復旧 | Reference既定はresourceless、JDBC repositoryはprofileで明示opt-in。ただし両profileのDBはH2インメモリ。本番DB方言・process再起動のEvidenceが必要 |
| 排他 | `JobRepositoryConcurrencyGuardService`は実行中Jobの照会でありlock取得ではない。listenerはJobごとのopt-inで、DB取込・file Jobには登録なし。process跨ぎの単一実行を充足したとは判定しない |
| ファイル | 存在・非空検査、archive / error移動、一時出力の昇格を実装。判断記録は頭から再投入するrerun方式。途中再開は別設計。転送完了判定・SFTP受付は未確認 / 追加検討 |
| 出力確定 | `AtomicFileOutput`はatomic move非対応時に通常moveへfallback。実filesystemでの公開条件と、出力・入力退避・実行metadata間の停止時照合を検証する |
| Audit | 既定publisherはログ出力。監査永続化はdeferred。Web側のDB Auditと同等の保証として扱わない |

追跡に用いたBatch Repository内の主なsourceは、`README.md`、`docs/batch/decision-log.md`、`docs/batch/db-management-architecture.ja.md`、`docs/plans/80-io-support.md`、各Reference JobとIT、`execution` / `io`の実装である。別Repositoryへのリンク切れを避け、上表はRepository内の名称で追跡する。

Agentは既存Batchを基盤候補として整合を補強する方を推奨した。これは新規Batch構築・既存Batch正式採用のOwner決定ではない。Ownerの組込み想定をもとに、version整合・連携契約・Evidenceを計画する。Batch側のcode・文書は変更していない。

## 7. 既存計画へ渡すものと次の作業

| 作業・package | 今回から渡す入力 |
|---|---|
| 作業5 / CP-F0 | S1完遂を目指す候補化判断は承認済み。対象・安全性・DoD判定・担当・上限・環境の具体化を続ける |
| 作業4b / 先行範囲 | S1と独立packageを並べ、案件入力待ちを全体停止条件にしない。Gate経路・先行対象は別判断 |
| 作業7 / A1・A2・D1 | §3のReference検証と未解決安全条件を分解し、成果物・正常 / 負例Evidence・停止点・概算を揃える |
| 作業7 / C3・C4 | non-Web＋Batch Frameworkを主たる連携検証候補とし、外部受付→実行→業務確定→結果確認の境界を具体化。Web同居は例外候補 |
| C3 / DoD 4-10 | Batch Repositoryの存在や単体testで代替せず、組み合わせた単一実行・二重起動拒否・再実行をEvidence化する |
| A1パージ・単一実行 | 既存CP8等と実行基盤を照合。Batch側の整備全体をS1の必須前提とせず、必要な依存と未承認契約を明示する |
| 案件協働 / S2 | §4の適合確認・gap分類・別判断へ渡す。担当・共同review時点は今後具体化 |
| 後続OR・作業6 / 8 / 9 / 10 | 承認済み候補化判断を入力に残る採否・実行条件を判断し、タスク・正本・統合計画を整合させる |

次は作業7の具体化である。本書作成をもってS1検証開始、S1完遂、作業4b / 5 / 7全体の承認とは扱わない。未成立の対象は停止点で個別に判断し、独立した計画作業とアプリチームへのFAQ対応は継続する。

## 8. 文書承認と作業区切り（2026-10-02）

Architecture Ownerは本書を確認し、「問題ありません、内容承認します」と明示した。§1の候補化判断に加え、判断経緯・S1検証軸・案件協働 / S2判断・non-Web境界・Batch点検材料・後続作業を記録した本書の内容を **OWNER APPROVED** とする。§1の個別実行・Gate等の承認境界は維持する。関連する作業4b・5の案全体やBatch本番適用を一括承認したとは扱わない。

Ownerは本件を大きな決定事項のcommit pointとし、ここで作業を区切る意向を示した。[次回作業引継ぎ](phase4-s1-next-session-handoff-20261002.md)を作成し、作業7の具体化へ渡す。本記録時点ではcommit前であり、commit実行・remote反映の完了を記録していない。
