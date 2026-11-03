# S1／B1／C2：保証範囲・DoD判定と後続課題の整理

**状態（2026-10-30）：** 既存Evidence・受入記録と現行sourceを照合した整理文書。S1の既知未達・運用／追加開発リスクを認識したうえで、現時点のエビデンスとして採用するOwner判断を§7へ記録。個別DoDの新規最終受入・原文改訂・追加実装・動的検証は行っていない。

**基点・branch：** main `fdfcfa59504011e7ed9eb0d142ec67eaa98feabf`（PR #58統合後）から`docs/phase4-s1-b1-c2-guarantee-review-20261030`を作成。
Ownerの第3段階への進行指示に基づき、[統合計画§9.5](phase4-review-integrated-task-plan-20261020.md#95-限定結果dod判定と後続アクション)を具体化する。

## 1. 結論と今回の判断単位

- **B1：** 同一origin・同梱ReferenceのCookie Session／CSRFとMVC／SPA併存は既存証拠で技術成立。DoD 4-8／4-9の受入候補とする。選定ブラウザ結果の既存受入を維持し、DoD最終受入・B1-08全体完了は今回の整理で代行しない。
- **C2 A：** 既存の限定受入を維持。retry・同時実行制限・read timeoutは選定操作で成立したが、connect timeoutの実効期限が未実証のためDoD 4-7全体はPASSにしない。
- **S1：** R1／R2等の限定結果と人手HOLD方針は受入済み。耐久復旧・二重送信抑止・監視／再送・パージ・非同期相関のDoD未達は残す。非同期採用案件が具体化した時に設計・運用を必要範囲で深掘りする。

S1全体・C2 B・未確認browser環境の追加検証を、E1へ進む一律の前提にはしない。
E1では実際に提供するartifact・機能・利用条件をこの判定へ対応付け、未達機能を無条件の標準保証へ含めない。
この整理はPhase4全体完了・本番採用・正式releaseの認定ではない。

## 2. 原文・証拠・受入の区別

DoDの正本は[グランドデザイン§27.8](../architecture/grand-design/KOIKI-JavaWeb-FW_グランドデザイン_v0.2.md#278-phase-4-enterprise-integrationv04)。下表は原文を要約した対応表で、要求を削除・書換えしない。
「技術成立」は既存検証で成立した構成・操作についての評価、「Owner受入」は明示された対象の受入、「未達／未実証」は不足を残す状態とする。
採否・延期・DoD変更は明示判断が必要であり、文書上の理由付けだけで未達をPASSにしない。

| 証拠群 | 今回参照する範囲 | 扱い |
|---|---|---|
| [S1現品質§2〜8](phase4-s1-dod-integrated-status-owner-review-20261016.md) | R1／R2／R3-A／R3-B／consume→recover接続、方針承認、既知FAIL・人手HOLD | 各正本のsource／artifact／epochを維持。今回raw XML・JAR・実providerを再照合していない |
| [B1-07統合結果](../architecture/validation/phase4-b1-07-integrated-browser-results-20261021.md) | 10月21日の通常／同梱各422 PASS＋99 SKIP、同じ最終同梱JARの新旧browser journey2件PASS | 途中の失敗・訂正前artifactを保持。10月22日のWindows JARと混同しない |
| [B1-07 Cloud再検証](../architecture/validation/phase4-b1-07-cloud-revalidation-20261022.md) | 10月22日の通常／同梱検証。新環境browserは配信元HTTP403で未実行 | 前日のbrowser成功を新環境の成功へ転記しない |
| [B1-08§5の受入](../architecture/validation/phase4-b1-08-local-acceptance-input-20261022.md#5-選定ブラウザ検証結果のowner受入2026-10-22) | 自宅Windows／Chrome155.0.8059.85の選定操作、固定JAR `284548e6…` | 選定ブラウザ結果はOWNER ACCEPTED。DoD全体受入とは別 |
| [C2 A§6〜7](../architecture/validation/phase4-c2-a-implementation-results-20261022.md#7-aの限定検証結果に対するowner受入承認2026-10-23) | 成果`87728b7`、選定29 PASS／1 SKIP、通常構成保護・固定JAR実演・cleanup | A限定結果はOWNER ACCEPTED。Windows rawのCloud独立照合・Cloud再実行ではない |

10月30日のmain CI成功は既存構成の保護を確認する材料。専用flag／profileで選ぶS1・B1 SPA・C2の全経路を今回main CIが再実行したとは扱わない。
D1の同期HTTP／OTLP10項目成功も、非同期listener・別process相関やS1の実sink回復へ転用しない。

## 3. DoD対応・判定初版

以下9項目を今回の対象とする。4-6／4-10等は従来台帳、4-11はD2受入記録を維持する。

| DoD・要求要約 | 保証できる限定範囲・Evidence | 保証できないこと／残る条件 | 今回の判定 |
|---|---|---|---|
| 4-1 承認時の非同期メール通知・送信失敗でも承認非rollback | S1 R1のcommit後stub通知、R2の失敗分離・拒否／rollback保護を限定受入 | 実メールprovider・実配送の最終完了は未実証。genericな非同期基礎の利用と原文のメール通知達成を区別 | 限定成立、DoD全体PASSは未認定 |
| 4-2 強制終了後、未処理イベントが再起動後に配信 | R3-A／Bの選択停止・手動復旧・完了とHOLD | B03a／E10aのFAILED完遂、一般的な再起動復旧・停止維持／再開の包括保証なし | 未達 |
| 4-3 二回配信でも二重送信しない | 安定key、stubの同内容受理再利用／異内容拒否、選択復旧の受理一意性 | Reference逐次全経路・内部競合・通常listenerとの並走・別publication横断等は未実証 | 未達。exactly-once配送保証にはしない |
| 4-4 FAILED件数・滞留metricと実行可能な再送手順 | permit／Identity／Audit、consume→recover一回→結果Auditの限定実証、監視・引継ぎ方針 | metric・alert・正式operator入口・肯定proof・認可処置／終端結果の統合実演は未完。実担当等もOPEN | 未達。人手HOLD方針を再送成立にしない |
| 4-5 完了publicationの単一実行パージjob | 保全・COMPLETED削除候補の除外条件の方針承認 | job実装・保持値・競合・削除／復元は未実証 | 未達。C3 Batch着手を今回へ抱き合わせない |
| 4-12 非同期listenerへの相関ID伝播・log追跡 | R1／R3の相関材料、既存ID照合・観測回復後の照合方針 | HTTP→非同期→別processの統合追跡・非漏えい等が未完。実trace条件はE20／21の別実演条件として未実証 | DoD全体PASSは未認定。原文を「実trace必須」へ書き換えない |
| 4-8 SPAのCookie Session API・CSRF double-submit | B1-05の認証／CSRF検証、B1-07の実browser負例を含む実演、B1-08選定手動結果の受入 | 同一originの選定Reference構成。HTTPS配備・別origin／BFF・実IdP・任意browserの成立を含めない | 選定構成の技術条件は成立。DoD最終受入・B1-08全体はOwner判断待ち |
| 4-9 同一appのThymeleaf／SPA併存・経路別CSRF | B1-07同一JARの新旧journey、MVC／SPA間の同一申請更新、経路別成功・拒否と設定照合 | 任意Customer構成への一般化なし。Bearer保護は補助回帰であり、DoD原文へ要求を追加しない | 選定構成の技術条件は成立。DoD最終受入・B1-08全体はOwner判断待ち |
| 4-7 外部API障害時のretry・流量制限・既定timeout | C2 Aの503限定最大3試行、操作枠2／待機0、第3操作拒否、read既定300msとoverride、回復・transaction外HTTP | connect既定200msは設定経路確認のみ、実効期限未実証。枠2は同時実行制限で、単位時間rate limit・複数instance制御ではない | Aの限定受入維持。DoD全体PASSは未認定 |

## 4. B1・C2の利用条件と未確認事項の位置付け

### 4.1 B1の受入候補を成立させる構成

現行Referenceは`/spa-api/**`をSession専用chain（order -10）で扱い、CookieCsrfTokenRepositoryと`csrf.spa()`を利用する。
MVC側はorder 0の別chainで`csrf(withDefaults())`を維持し、`/api/**`はBearer専用の別chainとして分離する。
[Session Security test](../../koiki-reference-app/src/test/java/org/koikifw/reference/identity/configuration/ReferenceSessionApiSecurityTest.java)には実Cookie／headerの組合せ、誤token、SessionとBearerの非混同等のassertionが存在する。
今回はsourceを静的照合し、過去の検証結果と対応付けただけで、新規test実行ではない。

- SPA同梱は`b1-session-spa` profile、通常buildは非同梱。Node／npm・lock・asset hashの照合は各実測に帰属する。
- 手動受入は自宅PCと固定JARの範囲。会社PC・更新後Chrome・認証中の一覧／詳細間の戻る／進む単独操作は未確認として残す。
- これらの未確認環境は、原DoDに新たに加える必須条件ではない。案件の採用構成・不具合に関係した時に追加確認を判断する。
- HTTPS・Cookie属性・proxy／origin・IdPは案件側条件。ReferenceのHTTP fixtureやTooling seedを本番導入手順へ転用しない。

**受入案：** DoD 4-8／4-9を検証済みの同梱Reference・同一origin Session構成の成立として受け入れる。
会社PC等の未確認を無条件の受入阻害条件へ追加せず、任意案件構成の保証も付けない。
本案はOwner判断対象であり、既存ブラウザ結果の受入範囲を今回勝手に拡大しない。

### 4.2 C2 Aに限った利用条件

Aは模擬rateの参照専用GET。既定無効、Session認証・権限、numeric loopback URI等の限定条件で実証したReference-owned例である。
HTTP既定値を任意clientへ自動適用せず、Springのretryと同時実行制御を当該操作が明示採用する。
read timeoutの成立をconnect timeoutの成立へ転用しない。接続拒否・read停止をSYN blackholeの代替証拠にもしない。
connect100ms override・retry追加1回override・retry上限拒否等の未検証枝は[C2結果§6](../architecture/validation/phase4-c2-a-implementation-results-20261022.md#6-cloudでの結果確認2026-10-23)に従って残す。

**判定案：** 今回はAの限定結果受入を維持し、H07・設定枝を残課題として追跡する。DoD 4-7全体は未認定を維持。
BのUNKNOWN・副作用あり操作・Intent／結果Audit・実providerは条件付き後続のまま。AやE1への移行条件には戻さない。
将来DoD 4-7全体を閉じる判断が必要なら、H07等の必要な有限実証、または要求の採否／延期の明示判断へ戻す。

## 5. 既存IDによる残アクション・再開条件

新しいpackage・全体Gate・独立した課題番号は追加しない。下表の担当は責任ロール案で、実担当者の確定ではない。

| 既存ID | 残す行為・再開条件 | 責任ロール案・E1等への渡し方 |
|---|---|---|
| S1-T1／R3・R4 | 非同期採用案件の復旧・重複抑止等の要求が具体化した時に、該当case・既知FAIL・不明点だけを再開判断 | Framework／Reference担当＋案件設計担当。一般的な自動復旧・二重送信抑止を現時点の保証から除外 |
| S1-T2／R4・R5 | 実provider、担当／代行、許可された照合経路、保管先・保持・停止／再開条件が具体化した時にOPENを更新 | 案件／platform・運用担当。人手HOLDの方針と実運用体制の成立を分ける |
| S1-T3／4-1〜5・4-12 | Phase4の最終判定時に各未達の採否・延期・必要実証を明示する | Architecture Owner。今回の文書整理で一括PASS・DoD削除を行わない |
| S1-R5／4-5 | 保持・削除が実際に必要な時に、保全条件・単一実行・競合／復元の対象を限定する | Framework／Reference＋案件運用。パージ未提供を明示し、C3へ自動着手しない |
| B1-08／R-04・F4 | §4.1の構成・制限を入力にDoD 4-8／4-9の受入範囲とB1-08全体状態を決める | Architecture Owner。E1へ同梱profile・認証構成・既知制限を渡す。追加browser全件再検証を前提にしない |
| C2-03／H07 | 接続期限の保証またはDoD全体判定が必要な時に、所有可能な有限fixture・停止条件を具体化する | Reference／Tooling担当。未実証を明示し、今回SYN blackholeやnetwork操作を実行しない |
| C2-03／設定枝 | 案件でconnect／retry overrideや上限拒否の保証が必要な時に不足枝だけを対象にする | Reference／Framework設定担当。既存のread override結果を全設定枝へ一般化しない |
| C2 B | 副作用あり操作・受理不明・receipt／Audit等が必要な案件／運用要求の具体化時 | Customer主導＋Framework契約担当。A受入やS1の人手HOLDから自動開始しない |
| E1-01〜03／R-09 | この表と初期標準一覧を、提供artifact・固定version・取得／更新・受領側確認へ結ぶ | Framework提供側。Reference／Toolingを正式Framework JARへ昇格させない |

既決の全停止型・不明時HOLD・未閉鎖permit保持を維持する。期限切れ・記録欠落を新しい再投入許可にしない。
非同期を採用しない案件へ、その運用基盤や追加復旧実装を一律に要求しない。

## 6. E1へ進むための判断と今回の出口

本書の文書整理は完了し、現時点のエビデンスとしての採用判断は§7へ記録した。個別DoDの最終受入・未達の採否を決める際は、次の区別を維持する。

1. B1の選定構成について、§3〜4.1をDoD 4-8／4-9の最終受入範囲として採るか。受入の場合は対象・制限・指示日を記録する。
2. S1の未達・C2のH07等を保持したまま、提供範囲と制限をE1へ渡すこと。既承認のS1区切り・C2 A受入を再承認の対象にはしない。
3. 既存IDで後続追跡し、具体的な必要のない追加実装・全件再検証を一律の前提にしないこと。

E1は初期提供artifact・版識別・repository・参照取り込み／更新を具体化し、必要構成の最小Consumer・Entity scan／Flyway等の確認へ進む。
S1の未達を残したまま初期提供範囲を定めることと、Phase4全体を完了させることは別判断とする。
D1の限定技術成立・main統合と、結果Owner受入の未記録も別管理のまま維持し、本書で受入済みに変えない。

今回の確認は文書・現行sourceの静的照合とリンク／差分点検のみ。Maven・Docker・browser・外部HTTP・CIを新規実行していない。
code・依存・設定・workflow・DoD原文の変更なし。commit・push・PR・mergeも未実施。

## 7. 現時点のエビデンスとしてのOwner採用判断（2026-10-30）

OwnerはS1の実案件採用を注意深く検討し、復旧・二重送信抑止等の既知未達が運用や追加開発へ大きく影響し得るリスクを認識していると説明した。
そのうえで「現時点ではこの文書を、他文書と同様のエビデンスとして採用することに問題ない」と判断した。
これを**本書のエビデンスとしての採用了解**として記録する。

- 採用対象は、§1〜6の既存成立範囲・未達・利用条件・後続課題と、参照Evidenceを対応付けた整理内容。
- 開発・テスト・運用開始時の検討材料として参照する。案件ごとの採用機能・必要保証・結合結果・残る制限の許容判断は別に行う。
- S1の既知未達・人手HOLD・運用入力OPENは維持する。文書採用を耐久復旧・二重送信防止・実運用体制の成立へ読み替えない。
- B1の選定ブラウザ結果とC2 Aの既存受入は維持する。B1 DoD4-8／4-9最終受入、C2 DoD4-7全体、S1各DoDの未達・未認定は、この文書採用だけで変更しない。
- 非同期の設計・運用の深掘りは、採用案件が具体化した時に必要範囲で判断する。非同期を採用しない案件に運用基盤・追加復旧実装を一律要求しない。
- 正式release、本番採用、Phase4全体完了、D1の結果受入、commit・remoteの承認は含めない。

次は本書の成果固定対象を確認し、既定の優先順に従ってE1の提供artifact・版識別・参照取り込み／更新の方式整理へ接続する。
未達の後続判断と個別DoD受入を追跡し、E1の方式整理開始に追加実装・全件再検証を一律の前提として置かない。
この記録反映は文書のみで、コード変更・検証再実行・commit・push・PRは行っていない。
