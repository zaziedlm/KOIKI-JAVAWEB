# Phase 4 S1 目的とDoDと実証段階の対応表

作成日：2026-10-09。状態：`DRAFT / OWNER REVIEW INPUT`。

S1の出口は、Referenceのexpense承認からnotification・provider stubへつなぎ、通知失敗の分離、停止後の配信完遂、重複抑止、復旧操作の統制、保持・追跡を用途限定で実証することである。2026-10-09のOwner採用方針に従い、最小の非同期経路を先に組み込み、正常系から異常系・停止後回復へ進める。permit基盤はR4の認可・Audit付き復旧へ接続する。

B1／B2は限定範囲でOwner受入済みである。Referenceの配送・復旧を通したDoD 4-1／4-2／4-3／4-4／4-5／4-12のPASSは本表では認定しない。本表は計画整理であり、後続のcode・POM・SQL・検証・local commit・remote操作の開始承認ではない。

確認source：`feature/phase4-s1-reference-foundation` / `6939799a48ed0c4e876bf92e74c22a7dc7453918`。OwnershipはFramework側の計画文書であり、実証成果物のOwnershipは§2で分ける。

## 1 成立済みの範囲とS1への再利用

| 成果 | 成立済みの範囲と根拠 | S1での役割 | 残る限界 |
|---|---|---|---|
| Reference保存・認可・Audit、scope／TTL、対象・運用証拠ハーネス | 既存の限定基盤。保存・認可・Auditと未接続拒否を維持。[振り返り§2・§6](phase4-s1-b1-b2-purpose-retrospective-20261008.md)参照 | R4の復旧操作統制へ再利用する基礎 | consume記録の保存を、送信許可・配信成功へ読み替えない |
| B1読取接続 | Tooling test所有の読取52件と採用済み回帰・package・cleanup。2026-10-08 Owner受入。[Evidence§23〜24](../architecture/validation/phase4-s1-stage-b1-read-connection-20261008.md#24-b1完了記録のowner承認2026-10-08) | R3の照合・UNKNOWN扱い・process世代観測、R4の対象確認へ知見を渡す | Tooling供給元の限定結果。停止・FAILED・lockだけで許可を肯定できない |
| B2初回issue／read | 有限凍結sourceから初回issue transaction完了までの保護、保存・読取・認可・Audit。新規54＋回帰301＝355件、artifact整合・package・cleanupをOwner受入。[Evidence§17〜20](../architecture/validation/phase4-s1-stage-b2-issue-read-20261008.md#19-b2限定検証結果のowner受入承認2026-10-08) | R4の許可発行・拒否・rollback・Auditの土台 | 全355件を同一classpathで反復した結果ではない。ThreadLocal保護は非同期送信へ持ち越せず、肯定consume／closeは未接続 |
| PL2非同期Tooling | 既存Toolingで停止・再公開・並走・stub冪等性等を検証。[軌道修正案§1](phase4-s1-async-reference-direction-correction-draft-20261008.md#1-点検で確認した事実)と[PL2 Evidence](../architecture/validation/phase4-pl2-level2-verification.md)参照 | R1〜R3の実験設計と故障位置制御の入力 | Tooling PASSをReference DoD PASSへ転記しない |

旧B2受入結果と[固定manifest](../architecture/validation/phase4-s1-stage-b2-fixed-evidence-manifest-20261008.json)を保持する。新端末でのtmp・target・cacheの欠落は旧受入結果の失敗ではない。新sourceの結果は別Evidenceへ記録し、旧hashや受入記録を差し替えない。

## 2 目的とDoDと必要証拠の対応

「未成立」はS1のReference経路として出口証拠が揃っていないことを示す。基盤の受入済み結果を未承認へ戻す意味ではない。case IDは[DoD実演計画§3](phase4-s1-dod-demonstration-plan-20261005.md#3-caseとdodの対応)に従う。

| 目的／DoD | 現在成立している材料 | Reference経路で未成立のこと | 主に担う機構／Ownership | 次に必要な実証・証拠／段階 |
|---|---|---|---|---|
| 業務確定と通知失敗の分離／4-1 | 既存同期veto・Security・業務Auditのbaselineと採用済み回帰 | 承認commit後の非同期通知、provider失敗時の承認保持、承認不成立時の通知抑止 | expenseの業務確定・値だけのevent、notification listenerとpublication。Reference所有、provider stubはTooling所有 | E01：承認・Audit・publication・stub受理1件の整合（R1）。E02／E03：provider失敗・拒否・rollbackでも同期契約を維持（R2） |
| 停止後の配信完遂／4-2 | B1の対象・停止・provider観測、B2の限定凍結方式、PL2停止実験の知見 | Reference自身のpublicationを対象とした検出・再公開・完了、許容復旧時間内の配信 | publication registryと再送経路はReference。停止注入・独立観測はTooling。運用者が到達点を照合 | E04〜E07・E10：停止位置、元process終了・世代、同一通知key、受理総数1件、publication完了、復旧時間と判断理由（R3）。E11〜E14の統制負例はR4でも照合 |
| 冪等性・重複抑止／4-3 | PL2 stubの冪等性知見。permitのpublication単位一意化 | 論理通知ごとの固定key、再送・並行要求・受理不明時の副作用一意性、別通知の誤抑止防止 | 固定keyと同keyのpayload整合はReference。key保持と重複拒否を実証するstubはTooling。permitは主機構にしない | E08正常枝：key契約と重複拒否の土台（R1）。E05〜E10：停止・再要求・応答喪失・keyなし／不一致／期限切れの負例（R3）。E14：旧workerとの競合（R4）。key寿命はR5の保持判断へ |
| 監視・認可／Audit付き復旧／4-4 | permit・Identity・能力・管理scope・TTL・Audit・HOLD分類、B1読取、B2初回issue／read | Reference復旧経路への肯定consume／close接続、監視・alertから判断・再送・結果照合までの運用 | Referenceの復旧操作と既存permit基盤。Toolingの観測sink・負例注入。操作・判断責務は開始票で指定 | E11〜E17：生存／無効停止証拠／複数復旧者／lock喪失／認可／Audit失敗／上限・timeout、終端結果と操作Audit（R4）。E21：観測不能からHOLD・照合回復・引継ぎ（R4〜R5） |
| 保持・単一実行パージ／4-5 | 既存Tooling・実行基盤は設計入力 | Reference publicationの保持期限・削除対象・単一実行・復旧との競合、keyと証跡の寿命整合 | Referenceの保持・パージ。Toolingの競合／障害注入と観測。保持方針はOwner判断 | E18／E19：期限超過COMPLETEDのみ削除、FAILED／未処理・復旧証跡の保全、二重起動・lock喪失・復旧競合時の停止（R5） |
| 非同期・別process追跡／4-12 | permit・対象の識別項目、既存Auditとprocess世代の記録 | HTTP承認から非同期・別process復旧への実trace関連と、DB・provider・Auditとの相関 | Referenceの相関伝播。Toolingのtrace／log観測。Framework IdentityやPublic APIへの業務項目追加はしない | 相関項目をR0で整理しR1から採取。E20：event／publication／通知key／permit／試行／process世代を追跡、別requestへの漏えいなし、MDCと実traceを区別。E21：観測回復後の再判断（R5） |

S1のstub受理は実メール到達・実provider保証ではない。Customerのprovider契約・運用条件・適合検証は別責務として残す。Framework共通API・Rules・正式配布への昇格はS2の別判断である。

## 3 実証段階と開始条件

| 段階 | 対象と出口 | 開始条件・判断点 |
|---|---|---|
| R0 開始判断資料 | Reference限定runtime・依存・publication schema／migration、ADR要否、Ownership、AGENTS／Skillの限定解除差分、R1のpath・検証集合・上限・cleanup・停止条件を提示 | 資料作成は2026-10-09の方針採用に含む。runtime・code・POM・SQL・検証開始はOwner個別判断。review軽量化の具体案も提示 |
| R1 正常経路 | package済みReferenceのexpense承認→値だけのevent→notification listener→stub。固定key・payloadと重複拒否の土台、E01／E08正常枝、相関記録 | R0の対象・開始承認、必要正本の整合、source固定・環境preflight。非同期code追加前のB2通常build対策を§4の順で扱う |
| R2 失敗分離 | E02／E03。通知失敗で承認を取り消さず、拒否・rollbackから通知しない | R1出口の結果と残課題を提示し、R2開始判断・故障注入と観測の条件を確定 |
| R3 停止後回復 | E04〜E10。停止後の実配信、固定keyによる受理総数1件、受理不明時のHOLDと照合 | R2出口と個別開始判断。全停止型／稼働中個別復旧型をR3前に選択し、復旧時間・停止真正性・provider契約・観測を確定 |
| R4 復旧操作の統制 | E11〜E17、E21の運用統制部分。permit基盤をReference復旧経路へ接続し、認可・Auditと肯定／負例を実証 | R3結果と保護モデルを入力に、consume／close・操作主体・権限／委譲・結果照合を個別review。B-C01〜B-C10とD11／D12を照合 |
| R5 保持・追跡 | E18〜E21。保持・パージ・実trace・観測不能からの回復／正式引継ぎを実証 | 保持期間、key有効期間、必要証跡、sink、単一実行方式と競合時の処置をreviewし、個別開始判断 |

各段階の出口でcase結果・未達・適用条件を集約し、次段階の開始を判断する。回帰選択、開始票の粒度、固定manifestの作成時点はR0で軽量化案を提示するが、現時点で採用済みとはしない。相関やkey寿命の設計は早期に行い、R5まで持ち越さない。

## 4 B2通常build対策を含む作業順

[2026-10-09通常build対策引継ぎ](phase4-s1-b2-normal-build-remediation-session-handoff-20261009.md)の対応を、非同期code追加前の準備に組み込む。これは今回の作業順の提案であり、同引継ぎの実装開始未承認を変更しない。

1. 本目的対応表を整理し、B1／B2の受入済み範囲とR0〜R5の目的を確認する。
2. B2通常build対策のOwnerレビュー案を作る。A案を第一候補とし、対象3テストの既定実行制御、classpath所在の明示入力、Java起動pathのOS対応、Tooling compile／package・coordinator test-classes・runtime classpathの準備手順を具体化する。変更path、検証集合、資源・時間・作業量／再実行上限、cleanup・停止条件を提示する。
3. 個別承認・source固定・preflight成立後に対策を実装・検証する。通常root buildの成立と、B2の明示実行・前提不足時の明確な失敗を分けて確認する。skipをB2 PASSへ数えない。登録テストは対象3クラスと分け、回帰集合を確定する。
4. R0資料を提示し、OwnerのReference非同期経路開始判断へ進む。R0資料作成は手順2と並行可能だが、実装は承認と必要前提の成立後とする。
5. R1→R2→R3→R4→R5の出口判断を経て進める。

通常build対策は現在のfeature branchで独立した差分・commit単位にまとめる案とする。local commitは操作前のOwner確認、remoteは個別承認に従う。広いTooling整理・履歴script保管・B2方式の置換／廃止は別作業に分ける。R4で必ず廃止するとは決めず、保護モデルに応じ再利用・置換・保管・未判断を分ける。

確認基点では、`B2FrozenSourceProcess`に固定tmp classpathと`java.exe`への依存が残る。この端末には当該classpath・Tooling target・Reference target・B2 raw directoryが存在するが、内容同一性・可搬性・clean root build PASSの証拠とはしない。CI失敗は実測していない。通常build対策の引継ぎ文書は未追跡であり、clean source固定前の保全・commit判断対象として残る。

## 5 R0以降へ渡す未決事項と安全条件

| 項目 | 現在の扱い | 決める時点 |
|---|---|---|
| runtime依存、publication所有・読取契約、migration、ADR | Reference限定の採否・具体構成はOPEN。Tooling viewを正式契約へ直接昇格させない | R0 |
| 論理通知key・payload整合・provider重複拒否 | 4-3の主機構として方針採用済み。生成規則・別通知識別・有効期間・照会／再要求条件はOPEN | R0〜R1で基本契約、R3前に異常系条件、R5前に保持整合 |
| 復旧時の保護モデル | 全停止メンテナンス型／稼働中個別復旧型の選択はOPEN。B2実装を理由に全停止型を既定扱いしない。稼働中型では旧worker拒否のfencing等も必要 | R3開始前 |
| 手動復旧とDoD 4-2の解釈 | 許容復旧時間、Web停止影響、実施／判断担当・代行はOPEN | R3開始前 |
| D11／D12・網羅性 | 歴史対象閉鎖、別publication横断抑止、照合後競合、送信までの保護は未達を保持。受入case一覧を網羅性証明にしない | R3の保護モデル判断、R4の契約reviewと実証 |
| 認可・Audit・worker委譲 | 既存Identity・Security・Auditを維持。送信前記録失敗と受理後記録失敗を区別。肯定consume／closeとworker委譲は未開始 | R4開始前 |
| 監視・上限・保持・追跡 | FAILED滞留起点、alert先、試行／timeout、sink、保持・key寿命、実traceはOPEN | R0で段階へ割当て、R4／R5開始前に具体条件 |
| 検証予算・cleanup | 旧B2残予算を新作業へ流用しない。各作業の有限集合・資源／時間・raw・再実行上限、停止・証拠保全・cleanupを別途確定 | 通常build対策と各段階の開始前 |

DB status・FAILED・lock・通知logだけで処理位置や再送可否を断定しない。provider受理不明はUNKNOWNのまま扱い、照合できる場合は同じkeyで安全な再要求、判断材料が欠ける場合はHOLD・調査・エスカレーションと担当・理由・次の確認を記録する。拒否／保留の成功だけで配信完遂をPASSにしない。

S1のDoD PASS候補提出には、正常・負例の結果、人による照合判断、未解決事項、検証構成・負荷・復旧条件・provider適用条件を集約する。Framework正式採用、Customer本番採用、正式受渡し、Phase 4全体の開始・完了は別判断として維持する。

## 6 根拠と次の資料

- [S1完遂方針§3](phase4-s1-completion-direction-decision-20261002.md#3-s1で完遂を目指すもの)：S1の目的と適用限界。
- [DoD実演計画](phase4-s1-dod-demonstration-plan-20261005.md)：E01〜E21、安全条件S-01〜S-11、人の判断、caseとDoDの判定区分。
- [軌道修正案§3〜§9](phase4-s1-async-reference-direction-correction-draft-20261008.md)：担う機構、R0〜R5、2026-10-09のOwner採用範囲。
- [B1／B2振り返り](phase4-s1-b1-b2-purpose-retrospective-20261008.md)と[別端末引継ぎ](phase4-s1-b1-b2-cross-device-session-handoff-20261008.md)：再利用・知見・置換見込みと承認境界。
- [段階B契約](phase4-s1-stage-b-operational-integration-contract-review-draft-20261008.md)と[B2方式review](phase4-s1-stage-b2-use-boundary-review-draft-20261008.md)：B-C01〜B-C10、D11／D12と操作別開始条件。
- [B2通常build対策引継ぎ](phase4-s1-b2-normal-build-remediation-session-handoff-20261009.md)：A案の具体化と独立した対策・検証。

次に具体化する資料はB2通常build対策のOwnerレビュー案とR0開始判断資料である。本表の文書整理に伴う新しい実行検証、code・test・POM・SQL・CI変更、commit・remote操作は行っていない。

## 7 通常build対策の完了と次作業（2026-10-09）

後続の個別承認により、通常buildからS1専用64件・B2 process28件を分離し、必要なfixture準備・明示classpathと専用再現を実施した。Windows通常root257 PASS／予定無効92、専用100 PASS／skip0、前提負例・package整合・cleanupは`COMPLETE / OWNER ACCEPTED`。[最終Evidenceと受入§23〜24](../architecture/validation/phase4-s1-b2-normal-build-remediation-20261009.md#24-最終結果のowner受入承認)を正本とする。§4と§6の未承認・未実施表現は資料作成時点の履歴で、今回完了を上書き否定しない。

次は[成果固定commitレビュー](phase4-s1-b2-normal-build-fixed-results-commit-review-20261009.md)でsource・承認記録・hash導線を固定し、その後にR0開始判断資料を具体化する。今回の結果受入はcommit／remoteやR1のruntime・code・依存・migration・検証開始を兼ねない。R0資料作成の方針承認とR1実行開始を区別し、R0〜R5・DoD・未決事項の境界を維持する。

## 8 R0開始判断資料の具体化（2026-10-09）

Owner承認による通常build対策22件のlocal commit `fe93b5da76a85fd6a4c41409c725c35661fac007`を基点に、[R0：Reference非同期経路の限定採用・R1開始判断レビュー案](phase4-s1-r0-reference-async-start-review-20261009.md)を作成した。Reference限定依存、二段階選択、値event／通知key、publication所有と追加migration、Rule 28／29 blocking review、ADR／正本差分、R1の対象path・20 case候補・新規予算・回帰・停止／cleanupを提案する。現行Rule 28はModulith listenerを拒否するため、規約対応の先行成立をR1開始前の必須判断とする。

状態は`DRAFT / OWNER REVIEW PENDING`。作成指示は資料整理の承認であり、runtime・code・POM・SQL・正本変更・検証の開始承認ではない。R3の保護モデル選択、R4のconsume／close・D11／D12、R5の保持／実trace、DoD・Phase 4全体の判断を維持する。

## 9 Rule 28／29の正式Level選択対応を先行（2026-10-09）

OwnerはReference限定例外の恒久化リスクを確認し、Level選択対応の限定改訂をR0で先に審査する順序を指示した。[規約限定改訂Ownerレビュー案](phase4-s1-r0-rule28-29-level-selection-owner-review-20261009.md)を作成し、Reference開始票を同じ順序へ訂正した。

順序は、規約契約審査→個別開始承認・文書source固定・preflight→規約限定実装／検証→Owner結果受入・source固定→R1開始票再確定・個別開始判断。module単位の明示選択、旧APIと未指定Level 0／1の拒否維持、Rule 29の検出限界、40 case候補・新規60分枠を提案する。Referenceだけの特定違反例外は不採用方向とする。今回の方向採用はFramework code／Public API改訂・検証の実行承認ではなく、R1・後段・DoD・remoteの開始を兼ねない。

同日の追加確認で、Ownerは`LEVEL_0`でも同期eventの不存在を検査しない点と、「Level 0／1の検査条件は共通、Level 2だけ明示指定で許容範囲を広げる」整理に同意した。[規約票§9.1](phase4-s1-r0-rule28-29-level-selection-owner-review-20261009.md#91-level-01共通条件level-2明示拡張へのowner同意2026-10-09)に記録する。これはLevel選択の意味整理への同意であり、具体API・細則・予算・実装開始の一括承認ではない。

## 10 規約レビュー案の承認・文書source固定待ち（2026-10-09）

Ownerは規約レビュー案全体を承認した。[規約票§9.2](phase4-s1-r0-rule28-29-level-selection-owner-review-20261009.md)へRL-01〜06、正本整合と文書commit／clean source固定・preflight条件付きR0-B限定開始を記録し、ADR-050・正本・ガイドを文書だけで整合した。規約code／Public API実装・Maven／Docker検証は未開始。文書commitは同票§7.1の別Owner承認／操作待ち。

次は[文書source固定commitレビュー](phase4-s1-r0-rules-document-source-fixed-commit-review-20261009.md)の対象を固定し、clean sourceのpreflightを経て承認済み規約作成・検証へ進む。R0-C結果受入とR1開始は別判断であり、今回の規約票承認からReference runtime・POM・SQLやremoteへ拡張しない。
