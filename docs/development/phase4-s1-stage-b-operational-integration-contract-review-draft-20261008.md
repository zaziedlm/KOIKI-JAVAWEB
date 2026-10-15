# S1段階B：実運用接続の追加契約と限定範囲案（2026-10-08）

**状態：方式・追加契約・限定範囲はOWNER APPROVED（2026-10-08、§9）。** 網羅性の懸念を保持し、接続設計・検証で不足を追加洗い出しする前提で採用する。実運用Adapterの作成・通信・配備・停止操作・肯定操作の実行開始は未承認。本書のB-0〜B-2／B-C01〜B-C10は文書上の追跡IDであり、新Gate・runtime enum・propertyではない。

**確認source：** `feature/phase4-s1-reference-foundation` / `08f47402e470d72dc8538452f5766331bb4dc323`（`test: S1対象・運用証拠ハーネスと検証結果を追加`）。本整理開始時はclean、upstream比ahead10。Owner操作で段階Aの7ファイルをcommit済み。

**位置／Ownership：** Phase 4 S1個別限定経路の後段。接続契約はReference `notification`（既存Tier 2、JPA共有モデル）が所有する。限定実演のpublication・停止観測・provider供給元には既存Toolingを採用する（§10）。具体的な読取契約・補強・保管先・実操作担当は後続で確定する。Framework／Customer／他業務moduleへ責務を移さず、Toolingは非配布境界を維持する。

**入力：** [接続契約案E-01〜E-08](phase4-s1-target-operational-evidence-contract-review-draft-20261007.md)、[段階A検証記録§6〜§10](../architecture/validation/phase4-s1-operational-evidence-harness-20261007.md#6-訂正後source固定とtest所有ハーネス作成)。段階Aは`COMPLETE / OWNER APPROVED`。本書は実コードとそのEvidenceに基づく追加案で、実供給元の仕様を取得した記録ではない。

## 1. 結論案：供給元確認・読取接続・肯定操作を分ける

推奨は、まず**実供給元と管理責任を特定し、その契約を採用してから、送信を伴わない限定読取接続を検証する**こと。段階Aの可変modelを本番へ移すことや、管理設定の停止booleanで肯定証拠を作ることは採用しない。

| 区分 | 到達点候補 | 進める条件／境界 |
|---|---|---|
| B-0：供給元・追加契約の確定 | §3〜§5の供給元資料、責任者、信頼境界、保証と不足を具体化 | 今回は文書整理。実担当への連絡・credential取得・環境操作は別に許可を確認。取得できない項目はOPENのまま |
| B-1：限定読取接続 | 明示的に選んだ環境で、実target・停止観測・provider照会・保管先を読取り、照合結果と拒否理由を検証 | 読取API／権限／観測範囲／証拠保持／数値上限を採用し、個別開始票を承認してから。issue／consume／closeを呼ばず、本番Serviceへ肯定Portを登録しない。取得値の読取PASSと実行許可を区別 |
| B-2：保存・認可・Auditへの肯定接続 | 採用した操作に限り、実運用Adapterを既存Applicationへ接続して検証 | B-1結果と使用時の保護を確認し、対象操作別に開始判断。issue／consume／closeを一括解禁しない。Port／Service／migrationの変更が必要なら差分を独立review |

これらは段階Bの内訳案で、全体の開始承認ではない。B-1でも照会が相手のquota・課金・logへ影響し得るため、環境・許可先・回数を限定する。sender／listener／runner／worker認証・委譲／publication runtime／Modulith Level 2は別の開始対象。B-2の消費commitから送信許可を自動導出しない。

## 2. 段階Aで確定した基礎と、段階Bへ残る条件

| 根拠 | 現在確認できること | 段階Bで必要な追加判断 |
|---|---|---|
| 新規48件＋関連・既存回帰 | test modelを現行Serviceへ接続し、対象・証拠照合、実Identity／JPA／Audit、一度性・rollbackを検証 | 実供給元の信頼性・耐久性・権限・停止継続・使用中の保護は未実証 |
| D11／V4部分unique index | 同じenvironment・publicationの未閉鎖permitは1件。消費・期限経過だけでは次permitを発行できない | attempt変更／対象削除後の旧permitは現行closeで拒否。別publication間の同じ論理通知は現行indexの保証外 |
| D12 | 照合中に検知した失効は拒否。最後の肯定照合後に停止制御を失うと消費がcommitし得る | 再読取・permit lock・短いTTLだけではこの窓を閉じない。供給元側で変更を防ぐ期間／制御、または使用側が旧世代を拒否する仕組みを採用・実証する |
| D09／D10 | 消費は送信成功を示さない。UNKNOWNは閉鎖肯定にせず、人の照合済み結果で閉鎖 | 実provider照合、人の判断責任、commit不明時の照会・保持・エスカレーションを決める |
| Provider補強／証拠台帳 | test内で固定通知key・payload・宛先、一意参照、本文差替え拒否、受理台帳との矛盾拒否 | stubのidempotency・sealed原本を実provider／保管先の保証へ昇格しない |

現行[RecoveryPermitService](../../koiki-reference-app/src/main/java/org/koikifw/reference/notification/application/RecoveryPermitService.java)は保存処理を調整し、consume／closeではpermit lock後にPortを呼ぶ。targetは5項目、proofは参照値を含むが、revision・有効期限・制御世代をServiceが独立検査する契約ではない。[通常構成](../../koiki-reference-app/src/main/java/org/koikifw/reference/notification/configuration/NotificationFoundationConfiguration.java)のtarget／evidence未接続拒否は維持する。

## 3. 実供給元から取得する接続契約

| ID／対象 | 取得・合意すべき契約 | 肯定を拒否する条件 |
|---|---|---|
| B-C01 現在対象 | 正式所有者、取得API・対象範囲、5項目とrevision／適格性／payload識別の対応、削除・attempt変更の意味、観測時刻・更新遅延、使用時の変更保護 | 別環境・対象差異・古いcache・revision不明・削除・取得不能。FAILEDだけで適格扱いしない |
| B-C02 停止制御 | 対象となる全process／instance、起動世代・deploy revision、drain・終了確認、移譲の有無、再起動／自動再配置抑止、制御主体、失効検知、再開手順と影響 | PID／boolean／復旧lockだけ、未終了・移譲・再起動・世代差異・制御喪失・期限一致。手動停止でも他instanceや遅延要求の不在は別に証明 |
| B-C03 provider観測 | 受理分類・照会整合性・反映遅延、受理ID、同じ論理通知の固定key／payload／宛先対応、未受理判断の時点と遅延要求の扱い | ACCEPTEDは追加実行用証拠にしない。UNKNOWN／照会障害／矛盾／期限切れはHOLD。応答喪失・DB rollbackから未受理としない |
| B-C04 論理通知の抑止 | 初回と再要求で同じkey、key生成所有者・保持期間、同key同内容の結果、異内容拒否、並行要求、別publicationとの対応、期限後の扱い | retry／operationごとの新key、対応不明、保持保証切れ。UNKNOWNの同key安全再要求は独立採用・実証まで使用しない |
| B-C05 証拠の耐久保管 | `environment + permitId + operationId + workerGeneration`の一意解決、immutable本文、採取主体・信頼元・時刻・対象、ACL、管理者の変更能力、改変検知、保持・復元・失効の別管理 | 参照不能・曖昧・差替え・保管失敗・保持不足・出所未確認。process内mapやfileの存在だけで真正性・耐久性を保証しない |
| B-C06 本人・能力・scope | 既存実Identity／現在能力／管理scopeの適用。採取主体・閉鎖確認者・認証主体を識別し、秘密を保持する境界を明示 | 入力user IDやworkerGenerationだけで本人扱いしない。現行の発行者本人消費を維持。worker委譲は別契約 |
| B-C07 時間・失効 | 使用時計の責任者、最大時計差・観測遅延、証拠寿命／制御期間／key保持の関係、時計異常・期限検知不能時の動作 | `validFrom <= now < validUntil`不成立、時計健全性不明、制御失効。testの60秒／10分を運用値にしない |
| B-C08 I/O・transaction | 外部採取と耐久保存はpermit lock前。lock中の照合方法、接続／応答timeout・取消・接続数・最大lock保持、障害時rollbackを明示 | 遅い通信の無制限実行、失効を確認できないcache、lock中自動retry。Spring transaction10秒を全I/O停止保証にしない |
| B-C09 人の照合・閉鎖 | 確認者の実認証・現在CLOSE能力／scope、permit／歴史対象／resultRef／理由／provider結果／次処置の追跡。未解決時のHOLD・調査・エスカレーション担当 | UNKNOWNを解決済みにしない。現行の対象比較を緩和して歴史閉鎖を通さない。閉鎖は配送成功や次送信許可を意味しない |
| B-C10 使用中の保護 | 保護開始・終了、対象／停止／失効世代の所有者、再起動・移譲・制御断への対応。旧worker／遅延要求が実副作用を起こせない範囲を明示 | 照合直後の変化を未保護のまま肯定接続しない。lease文字列だけをfencingとしない。送信時の保護は別途実証まで未達 |

信頼元の認証、通信保護、ACL、署名／digest等の具体方式は、実供給元の機能と脅威・変更権限に基づき選ぶ。digest単独は作成者の真正性を証明しない。秘密のpayload・宛先実値・credentialをAudit／エラー／rawへ出力しない。

## 4. D11／D12への方式候補と採用条件

**歴史対象の閉鎖：** 現行closeは現在対象一致を要求するため、削除・attempt更新後の旧permitを閉じられない。限定実演で対象を不変に保つ方式なら、その期間・担当・違反時HOLDを明示する。変更後も閉鎖する要件がある場合は、許可発行時の歴史対象・解決証拠を照合する専用契約案を別途作る。現在対象の確認削除、任意の閉鎖UPDATE、消費／Audit削除で次permitを空けない。

**別publication間の論理通知：** 論理通知識別をpublication所有者・provider keyと対応させ、横断的な未解決状態の抑止と実受理の重複抑止を分けて定義する。限定対象を1論理通知に絞る案でも、別の入口・通常listenerから同通知が実行されないことを確認する。V4一意制約を横断保証として扱わず、別台帳・schemaが必要なら独立reviewへ戻す。

**照合後の変更：** 限定実演では関連全processの停止・drainと再起動／移譲制御の継続を第一候補とする。実基盤がその不変期間を保証できるか確認し、対象変更・失効・制御断の検知と旧実行の抑止を含める。保証できない場合は、供給元の予約／世代制御と使用側拒否の案を比較する。方式が未成立ならB-1読取だけに留め、consume肯定接続へ進まない。

**保管とDBの境界：** 証拠を先に耐久保存し、一意keyから後日解決可能にする候補を維持する。DB rollback時は未使用証拠、commit結果不明時は保留・実消費照会とする。保管先とDBの原子的commitは仮定しない。採用証拠と消費の対応を保持・復元できなければ、Reference追加記録／migration案を独立reviewする。

## 5. 供給元確認票（未取得はOPEN）

**最新の具体化（2026-10-08）：** Ownerの意図確認を受け、今回の接続先をRepository内のToolingによるReference限定実演へ具体化した。[供給元・環境・管理責任の確認票](phase4-s1-stage-b-tooling-source-selection-20261008.md)に現行source、補強、担当案を記録する。実案件の外部システム連携は今回の必須条件にしない。

| 確認項目 | 現在値 | 次に必要な資料／判断 |
|---|---|---|
| 限定環境・対象・管理担当 | 現在Windows端末・隔離DB／processを候補。判断責任はOwner、環境管理兼務は案 | 対象一覧・通常経路との隔離・実操作担当・上限を開始票で確定 |
| publication所有者・取得契約 | 既存PL2 Toolingのpublicationを接続素材に採用。Referenceに実publication runtimeなし | Tooling所有read contract、厳密tuple・revision・適格状態・変更制御を補強。Reference runtime／schema追加は含めない |
| 停止基盤・全instanceの把握 | 既存Toolingの子JVM制御・終了待機を接続素材に採用 | 全process台帳・世代、drain／停止・再起動抑止、証拠取得・管理主体。確認fileだけでは肯定しない |
| provider・照会機能 | 既存PL2のDB-backed ProviderStubを限定実演の供給元に採用 | send実装に不足する受理照会・payload／宛先binding・key保持・遅延要求の契約を具体化。外部provider選定は後続 |
| 証拠保管先・責任者 | Tooling所有の独立保管先を追加候補。具体方式・実担当はOPEN | 段階Aのmapを耐久保管へ昇格しない。DB／file方式、ACL・一意登録・保持／復元・失効照会とroleをreview |
| 時間・I/O・検証資源の数値 | OPEN | 時計差・寿命・各timeout・quota・回数・総時間・raw上限・接続数。段階A上限は新しい実通信許可へ流用しない |
| 機能の初回操作範囲 | B-1読取先行の方式は§9で採用済み。実行開始は未承認 | Toolingのfixture準備副作用・実読取接続対象と肯定操作へ移る条件を開始票に明示 |

各資料にはsource、版／確認日、責任者、保証範囲、不足を残す。実資料のない欄をtest sourceや設計上の期待で埋めない。部分取得なら取得済み範囲だけでB-1案を具体化し、未取得のPortへ肯定fallbackを置かない。

## 6. 限定実装差分の候補

| 対象 | 候補責務・範囲 | 採用前に確定するもの |
|---|---|---|
| Reference Outbound Adapter | 自module Portから正式供給契約を利用。読取・照合・保管解決の技術詳細を閉じ込める | 実供給元・class／path・field／型・拒否契約。ここでは候補クラスを先行生成しない |
| Application／Port | B-1は現行Serviceへの肯定接続なし。B-2で原子的保護・歴史閉鎖を現行契約に表現できるか判断 | 必要差分、操作別保証、既存呼出し互換。通常無効・未接続拒否を保護 |
| Configuration | 承認環境・方式でだけ明示登録。欠落・不正設定・未選択時に肯定値なし | mode／property・credential供給・失敗動作。管理scope／TTL設定を動的対象・停止証拠へ流用しない |
| 保存／migration | 既存permit・append-only消費・Audit・未閉鎖一意制約を維持 | 独立証拠保管で足りるか、不足時のReference-owned追加案。現時点でSQL／grantは変更しない |
| test／非配布Tooling | 実Adapter契約testと隔離実DB、実取得結果のsanitized Evidence | 対象path・実／modelの区別・件数・上限・cleanup。48件のmodel PASSを実接続PASSに読み替えない |

他moduleの内部Entity／Repository／Modulith内部tableを直接参照する方式は既定にしない。既存の表示専用JOIN例外をcurrent-value／認可判断へ流用しない。Framework API／Rules／依存、通常Security、CI、配布物、remoteはこの限定案の対象外。

## 7. 検証・開始票へ渡す条件

| 検証群 | 必須確認候補 |
|---|---|
| B-1実読取 | 正式供給元認証・最小権限・対象隔離、全tuple／revision／鮮度、停止世代・移譲、provider key／payload／宛先対応、取得不能・timeout・期限・矛盾の拒否。観測だけでは変更抑止をPASSにしない |
| 保管・時計 | 再起動後の一意追跡、同key異本文拒否、権限外拒否、失効・保持・復元・時計異常。失敗注入は許可された隔離環境だけ |
| B-2使用境界 | D12相当の照合直後変更・制御断・再起動・移譲を採用実方式で検証。D11の歴史閉鎖と別publication抑止を、採用範囲と未達に分ける |
| B-2保存・認可・Audit | 現在能力／scope／本人、証拠先行保存失敗、Audit rollback、同permit競合、commit不明のHOLD、消費後失効時の消費保持。肯定する操作だけに限定 |
| 回帰・package | 段階A48件、管理scope／TTL40件、初回55件、既存25 class／99件＋package済みE2E1件をbaseline入力として影響に応じて選択。Architecture重複計数なし、fixture・credentialのJAR非混入、cleanup |

case一覧はcoverage候補で、件数・完全性・PASSの認定ではない。実供給元が特定されてから、class／method・資源・検証時間・作業量・raw上限・再実行回数・接続先許可・cleanup・戻し方を限定開始票へ具体化する。文書commit・clean source固定とpreflight成立の条件も、その開始票で個別判断する。

停止条件候補は、信頼元／対象が未確認、原子的保護不足、UNKNOWNを肯定へ変換する必要、認可／Audit迂回、想定外のschema／依存変更、既存回帰失敗、秘密出力、接続先・quota・資源・時間上限超過、cleanup不成立。停止後も証拠・消費・Auditを保持し、無断reset／delete・down migration・一律再送を行わない。

## 8. Ownerレビュー対象と次の作業

レビュー対象は、B-1読取先行の分割、§3の追加契約、D11／D12への方式候補、§5のOPEN項目、§6〜§7の限定差分・検証条件。**方式・追加契約・限定範囲は承認済み（§9）、実装開始は未承認。** 実publication／停止基盤／provider／保管先の資料が必要なため、現時点では具体Adapter開始票を成立済みとしない。

次の作業は、採用方式に沿った§5の供給元確認票の具体化。最初に取得可能な実供給元・限定環境・管理担当を特定し、その範囲のB-1接続設計・限定開始票を作る。実供給元が用意できない場合は不足を記録し、追加test modelを実運用接続として扱わない。

今回の変更は開発文書のみ。Maven／Docker・外部通信・環境変更、code／test／SQLの変更、git add／commit／pushは実行していない。段階Aの結果承認は保持し、Phase 4全体・DoD・正式受渡しへの承認へ拡張しない。

## 9. Owner承認・網羅性懸念の保持（2026-10-08）

Ownerは本書を確認し、次の発言で承認した。

> 網羅性に懸念あるものの、検証洗い出しされる想定も鑑みてこれを承認します

§1〜§7の方式・追加契約・限定範囲を採用する。承認は契約・coverageの完全性の認定ではない。§5のOPEN項目、D11／D12の未達、実供給元の保証不足を保持し、供給元資料の取得・接続設計・今後個別承認される検証で不足する条件と枝を追加洗い出しする。

追加で判明した事項は、根拠source、影響する契約ID、拒否／HOLDとなる条件、検証枝、残るリスクとOwner判断の要否を記録する。採用範囲内のcoverage補足と、方式・権限・schema／依存・肯定条件・実行上限を変える差分を区別し、後者は実行前に差分reviewへ戻す。網羅性への懸念をPASSで解消したと自動判断せず、結果レビューでOwnerへ残存事項を提示する。

この承認は方式採用であり、具体Adapterの作成・通信・配備・停止操作・肯定操作の実行開始承認ではない。次は§5の供給元確認票の具体化と、取得済み範囲に限定したB-1接続設計・限定開始票の作成。git add／commit／pushは行っていない。コミット操作はOwner自身の操作または操作前のOwner確認に従う。

## 10. Tooling供給元の採用方向・適用範囲の明確化（2026-10-08）

Ownerは、未定という回答が実際のシステム連携・機能連携を想定したものであり、Repository内の整備済みToolingを供給元として利用できるなら採用すると明示した。今回のReference限定実演ではToolingを接続素材として利用できるため、その供給元選択を採用する。[具体化票](phase4-s1-stage-b-tooling-source-selection-20261008.md)に現行能力と不足を分けて記録した。

本書の「実運用供給元」は、この限定単位ではTooling所有の隔離process・DB・provider stubから実値を取得する接続を意味する。実案件の本番供給元の取得を今回の必須条件とした説明は修正する。実案件の連携は後続対象であり、Toolingの所有権・非配布境界を維持する。

既存Toolingの停止file・advisory lock・event IDだけのprovider登録・process内証拠mapには不足がある。供給元の採用だけで肯定接続可能とは扱わず、B-C01〜B-C10・D11／D12と網羅性懸念を保持し、補強差分・接続方式・実操作担当・上限を次にreviewする。今回の指示は供給元選択と文書具体化であり、code／SQL・process停止／起動・通信の開始承認ではない。

## 11. B1受入後のB2 review資料（2026-10-08）

B1の限定検証結果は[Evidence§24](../architecture/validation/phase4-s1-stage-b1-read-connection-20261008.md#24-b1完了記録のowner承認2026-10-08)で `COMPLETE / OWNER APPROVED`。§5〜§10の未着手・未承認表記は当該提出時の履歴であり、B1の現在状態は同EvidenceとB1開始票の承認記録で確認する。実運用・B2の不足まで解消したとは扱わない。

Ownerの次工程への指示を受け、[B2使用境界・操作別接続review案](phase4-s1-stage-b2-use-boundary-review-draft-20261008.md)を作成した。現行Service／PortとB1観測契約を照合し、初回の使用境界実証＋issue／read、限定fixtureの不変期間を第一候補として提出する。方式・初回操作範囲はreview待ち、具体実装・検証開始は個別開始票で判断する。D11／D12・網羅性懸念・UNKNOWNを保持する。

後続の方式review§9で上記2点はOwner承認済み。具体path・protocol・保護手順・新規54件＋回帰301件・予算・cleanupを[B2限定開始票案](phase4-s1-stage-b2-issue-read-limited-start-review-20261008.md)へ記録した。具体開始は同票§10のOwner判断待ちであり、方式承認を実行開始へ読み替えない。
