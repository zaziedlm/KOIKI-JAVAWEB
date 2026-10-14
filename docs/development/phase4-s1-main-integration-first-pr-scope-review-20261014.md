# Phase 4 S1：main統合計画と最初のPR範囲の点検案（2026-10-14）

状態：**LOCAL REVIEW DRAFT / OWNER INTEGRATION DECISION PENDING**。Architecture Ownerは、R3-Bを待たずに統合計画と最初のPR範囲の点検を始める方針を採用した。本書はローカルGit履歴に基づく判断入力であり、push・PR作成・merge・CI変更・R3-B開始の承認ではない。

現行判断は§10を参照する。上の状態は本書作成時点の履歴である。

## 1. 点検基点と統合の意味

2026-10-14のローカル点検開始時、`main` / `origin/main`のローカル参照は`217cd0c`、現branch `feature/phase4-s1-reference-full-stop-recovery-r3a`は`c8d406b`、working treeはcleanだった。本書作成後は本書が未追跡差分である。`main`は現branchの祖先で、差分は52 commit。続くread-only `git ls-remote --heads`でremote `main`も`217cd0c`と一致し、当初R3-A branchのremote参照は存在しなかった。Ownerの個別承認後に成果コミット`c8d406b713fed97269ca5843ecf31b79824593ad`を新規remote branch `feature/phase4-s1-reference-full-stop-recovery-r3a`へ非forceでpushし、`git ls-remote`の参照hash一致を確認した。本書の未追跡差分はpushに含めていない。

R3断面までの累積sourceをmainへ統合すると、R1のReference-owned opt-in非同期通知実装、R2失敗分離、R3-Aの隔離・単発全停止復旧入口も取り込まれる。ただし、経費承認自体は同期処理で、承認とAudit／publication保存は同一transaction、通知はcommit後の非同期処理という境界を維持する。通常構成では非同期経路は無効。mainへの取り込みは、実provider配信、一般運用の復旧保証、R4の認可・Audit／permit接続、R5、DoD全体、正式受渡し、Phase 4全体の受入を意味しない。

R3-BはE07通知log保存境界、E09のkey異常、E10の応答喪失・照会不能、E08残枝について別票で目的・契約・最小検証を判断する。R3-B成果が受入済みになればR3統合断面の候補となるが、その完了を最初のPR範囲の点検条件にはしない。

## 2. 履歴上の自然な境界

| 基点からの出口候補 | 累積commit数 | 役割 | 現時点の扱い |
|---|---:|---|---|
| `dbee2bf` | 5 | Phase 4準備に先立つ開発ガイド・依存管理・認証profile等の文書 | 最初のPR候補。`main..dbee2bf`は9 file、876 insertions／11 deletions、`docs/development/`のみ |
| `be4bce1` | 9 | S1の目的・方式・Gate等の計画文書 | 次の文書PR候補。`dbee2bf..be4bce1`は4 commit、26 file、2,658 insertions／6 deletions |
| `f5e2672` | 13 | 追加局所検証A／B／CのToolingとEvidence | 独立したTooling検証PR候補。`be4bce1..f5e2672`の検証差分・通常buildへの影響を別審査 |
| `5f31d41` | 44 | S1 Reference基盤、B1／B2、通常build対策、R0規約と受入文書 | さらに受入済み段階で分割する。単一PRを前提にしない |
| `18cb8b6` | 47 | R1正常非同期経路・受入／引継ぎ | R1のPR候補境界 |
| `66d9b8f` | 49 | R2失敗分離・受入 | R2のPR候補境界 |
| `c8d406b` | 52 | R3-A全停止型の限定結果・受入 | R3-AのPR候補境界。R3-Bの扱いは成果と差分確定後に判断 |

この表はレビュー順序の候補であり、後続PRの確定分割ではない。特に`f5e2672..5f31d41`は31 commitに及び、Reference基盤、B1／B2、R0の受入境界と各差分を追加点検してから切る。前段PRのmergeを待って後段PRのbaseを更新する方法と、未mergeの前段branchをbaseにしたstacked PRの方法を比較する。mainへの最終merge順は依存順を守る。個々のPRで文書の「未開始」「未受入」が記録時点の履歴か現行状態かを明示する。

## 3. 最初のPR候補

第一候補は、**base `main`／head `dbee2bf`相当の専用integration branch**で、先頭5 commitの文書9 fileだけを扱うPRとする。remote `main`の参照一致をread-onlyで確認した。`gh pr list --state all`では既存PRは#42までで、本候補と同じheadのPRはなかった。現R3-A branchをそのままmain向けの最初のPRにすると52 commitが全て差分へ入るため、この候補のheadにはしない。既存の作業branchをrewind／force-pushせず、履歴上のcommitから別branchを作る方法を採る。

目的は、開発ガイド、日々の開発手順、依存管理判断、frontend認証profileとPre-Phase 4検討の記録をmainへ取り込むこと。Reference非同期実装・migration・Tooling検証は含めない。`git diff --check main..dbee2bf`は成立し、差分は文書9 fileのみ。候補headの9 fileにある相対Markdown linkのtargetを同headのGit treeで確認し、欠落0件だった。PR作成前に状態表記、secretや環境依存path、main最新状態との整合を再点検する。文書のみの候補なので新しいMaven／Docker検証は原則要求せず、PR上の既存required checksとreview結果を確認する。CI失敗や新しいmain差分があれば原因を分類して別判断する。

候補としては小さいが、それでも文書差分は約865 net linesある。文書9 fileを一度にreviewできるかをOwnerが確認し、難しければ先頭commitからさらに分割する。`main..dbee2bf`の5 commitは時系列と依存を保ったまま扱い、後続内容を先取りして混ぜない。

## 4. 次の判断順

1. R3-A成果branchのremote保全と参照hash一致はOwner個別承認後に完了。commit済み成果に対する追加差分はなく、統合計画・R3-B契約／開始票の文書だけが未追跡で残る。tmpのJAR／XML／logはGitでは移送されない。
2. [R3-B契約レビュー](phase4-s1-r3b-purpose-log-provider-unknown-owner-review-20261014.md)の第一候補は設計基線としてOwner採用済み。[R3-B最小開始票案](phase4-s1-r3b-contract-and-minimal-start-owner-review-20261014.md)のblocking条件・数値は引き続きOwner review対象。実装開始はその票の判断後とする。
3. 読取可能なmain最新参照、既存PR／ruleset／required checksを確認し、最初のPRのbase・head・9 fileを確定する。この段階ではPR作成やworkflow dispatchを行わない。
4. 最初のPR作成・mergeを各々のOwner個別判断で進める。後続は受入済み断面ごとに差分・CI・通常構成保護を確認し、R3-B完了時にR3断面の最終統合範囲を再判定する。

本書の点検ではremote `main`参照・既存PR一覧をread-only確認した。R3-A成果branchのpushは別のOwner個別承認により実行・参照hashを照合した。統合用branch作成、最初のPR、merge、追加検証は行っていない。

## 5. 最初のPR統合後と次の文書commit境界の点検（2026-10-14）

上記§1〜4は最初のPR実行前の判断履歴である。後続のOwner指示により、R3-B成果と手動HTTP確認記録をremote branch `feature/phase4-s1-reference-unknown-recovery-r3b`の`1d32b43`まで非force pushした。最初の文書PRは計画どおり`dbee2bf`をheadとする専用branchから[PR #43](https://github.com/zaziedlm/KOIKI-JAVAWEB/pull/43)を作成し、文書9 file／5 commitだけを`main`へmerge commit `c6b77f9c4f80218ab4d55b0c3d1f8b9322263fda`で統合した。PRのrequired checksは全件成功。merge後の同じmain commitに対し[CI 7／7](https://github.com/zaziedlm/KOIKI-JAVAWEB/actions/runs/38543968370)と[Java Runtime Compatibility 2／2](https://github.com/zaziedlm/KOIKI-JAVAWEB/actions/runs/38543968450)も成功した。**Reference非同期実装はまだmain未統合**である。

次の履歴断面`dbee2bf..be4bce1`は**4 commit／文書26 file／2,658追加・6削除行**。一括PRは最初の9 file／876追加行より大きいため、以下のcommit境界で段階的な文書PRとすることを第一候補にする。各headは先行headの子孫で、既存履歴のrewind・force pushを必要としない。各PRのbaseは先行PRをmainへmergeした時点の最新mainとし、前段未mergeのうちに後段をmain向けへ直接出して累積差分を混ぜない。

| 次のPR候補 | 履歴範囲・head | 差分 | レビュー主題 |
|---|---|---:|---|
| 第2候補 | `dbee2bf..781a6a2`、1 commit | 6 file、501追加・5削除行 | Phase 4入口／CP-F0・S0再計画案、S1完遂方向のOwner判断とhandoff。計画方針の承認を実装開始へ読み替えない |
| 第3候補 | `781a6a2..e75a70a`、2 commit | 11 file、823追加行 | S1範囲・安全／復旧条件・DoD実演と、Phase 4初回範囲／Gate統合の判断材料。DRAFTと正式Gate判断を区別する |
| 第4候補 | `e75a70a..be4bce1`、1 commit | 20 file、1,336追加・3削除行 | S1最小候補、方式票、初回Tooling局所検証契約とhandoff。2026-10-05時点の「未作成／未実行」は履歴であり、現行R3-B成果の否定と読まない |

静的点検では各候補差分の`git diff --check`は指摘0。候補head`781a6a2`／`eaf944d`／`e75a70a`／`be4bce1`で、`dbee2bf`以降に変更したMarkdownの相対リンク先をそれぞれのGit treeで確認し、file／directoryの欠落は順に**0／0／0／0**（点検対象リンク112／155／181／374）。この照合はanchorの正しさ・外部URLの到達性・記述内容の現行性までは証明しない。第3候補は必要なら`eaf944d`でさらに分けられるが、第4候補は単独でも20 fileである。第4候補のレビュー負担が大きければ、commit内部のfile単位再構成を別判断する。

ここでは次のintegration branch作成・push・PR・mergeを行っていない。まず第2候補の6 fileを内容点検し、現行`main`との比較、状態記述と文書リンクを確認してから、次の個別Owner判断へ進む。後続のReference／Tooling統合境界、R3-B既知FAILを含むCI選択とDoD未達の表示は引き続き別点検とする。

## 6. 文書4 commitをまとめるPR案の再評価（2026-10-14）

§5の3 PR案はcommit境界を見つけるための判断履歴である。Ownerの「分割整理を理解した上で、PR／CIを経由するならまとめたPRを検討したい」という意向を受け、**次の第一候補を文書4 commitの一括PR**へ改める。baseは最初のPR #43統合後の`main`、headは既存履歴の`be4bce1`相当を指す専用integration branchとする。`origin/main`と`be4bce1`のmerge-baseは`dbee2bf`で、PR差分は`dbee2bf..be4bce1`の**文書26 file、2,658追加・6削除行**だけである。Reference／Toolingのcode、migration、設定は含まれない。既存のR3-B作業branchを巻き戻さず、この履歴commitからintegration branchを作る。

一括PRのレビュー単位は維持する。PR本文とGitHubのCommitsタブで、①`781a6a2`（Phase 4入口／S1完遂方向、6 file）、②`eaf944d`（S1範囲・安全／復旧・DoD、6 file）、③`e75a70a`（Phase 4範囲／Gate統合、7 file）、④`be4bce1`（S1最小範囲・Tooling局所検証契約、20 file）の順に点検する。file数はcommitごとの変更数で、複数commitに現れる文書があるため合計は一括PRのunique 26 fileとは一致しない。§5の3 PR分割は、Owner reviewで一括差分が扱いにくいと判明した場合の代替案として残す。

この4 commitの文書は2026-10-02〜05時点の判断・未決事項を記録する。DRAFT、当時の「未作成／未実行」、Owner承認済みの計画方針またはTooling局所契約を、現行R3-B限定結果の否定やPhase 4全体開始／DoD達成の承認へ読み替えない。PR本文には**履歴文書の統合**と明記し、後続のR1〜R3-B成果・既知FAIL・人手HOLD・未達の評価は別の統合段階に残す。

一括差分の`git diff --check origin/main...be4bce1`は指摘0。§5で各commit head時点の相対Markdown linkのtarget欠落0を確認済みで、これはanchorや外部URL、記述の現行性の検証ではない。文書のみのため新たなMaven／Docker実行は求めず、PRのreviewとrequired checksを確認する。一括化によりPR／CIの往復を1回にできる一方、約2,658追加行の読解負担は残る。レビューで意味上の矛盾やリンク問題が見つかれば、当該PRで訂正範囲を判断し、必要なら§5の境界へ分割する。

この再評価ではintegration branch作成、remote push、PR作成、mergeを行わない。それぞれのremote操作はOwnerの個別判断に従う。現R3-B branch上の本書の未commit差分は`be4bce1`から切る文書PRには含めず、レビュー上の説明はPR本文へ反映する。

## 7. 文書26件の後に残るsourceとFramework本体の統合順序（2026-10-14）

Owner指示により文書26件／4 commitを専用branch `integration/phase4-s1-scope-planning-docs`の`be4bce1`としてpushし、[PR #44](https://github.com/zaziedlm/KOIKI-JAVAWEB/pull/44)を作成した。作成時点のbaseは`main`の`c6b77f9`で、PR head・filesを照合済み。**この節の記録時点ではCI確認・mergeは未完了**である。§6までの未実行記述は各時点の履歴として扱う。

`be4bce1..1d32b43`の残差分は46 commit／unique 278 file。source ownership別では`koiki-archunit-rules`17 file、`koiki-reference-app`89 file、`build-support`63 fileがあり、残りは文書・Agent guidance等である。**Framework本体側のsource変更は現行履歴ではR0の`koiki-archunit-rules`だけ**。Core／Starter／Framework APIの追加やroot POM変更はこの残差分にない。ReferenceのPOM／migration／通知・復旧codeはReference-owned、`build-support`は検証Tooling-ownedとして区別する。

| 履歴境界（候補） | 統合する主題 | PR reviewでの焦点 |
|---|---|---|
| `be4bce1..f5e2672` | S1局所検証A／B／CのToolingとEvidence | test-onlyの所有、通常build影響、履歴時点の検証結果 |
| `f5e2672..fe93b5d` | Reference通知・復旧許可基盤、scope／TTL、B1／B2、通常build分離 | 31 commit・大きな差分のため、基盤・B1／B2・build対策の内部境界を別途点検してPRを切る |
| `fe93b5d..5f31d41` | **Framework規約R0**：`koiki-archunit-rules`のRule 28／29とLevel選択、ADR／受入文書 | Framework-owned変更を明示した独立PR候補。旧APIの互換、未指定／Level 0／1拒否、選択Level 2の許容、規約testを確認する |
| `5f31d41..18cb8b6` | R1：Reference経費承認後のopt-in非同期通知 | 同期承認transaction境界と通常構成保護、Reference依存／migration、R1受入範囲 |
| `18cb8b6..66d9b8f` | R2：失敗分離と局所訂正 | 初回FAILと訂正後の結果を区別する |
| `66d9b8f..c8d406b` | R3-A：単発全停止復旧 | 限定した並行・lock証拠と通常構成保護 |
| `c8d406b..1d32b43` | R3-B：受理log、UNKNOWN照合、人手HOLD、手動HTTP証拠 | B03a自動完了FAIL、E08残枝、DoD未達を受入境界に明記する |

推奨する順序は、PR #44の結果を確定した後、ToolingとReference基盤／B1／B2の依存断面を審査し、**R0規約PRをR1非同期実装PRより先にmainへ統合する**こと。その後にR1〜R3-Bを受入済みの履歴境界で審査する。これはFramework変更をR3-Bの最後まで保留する計画ではない。R1〜R3-Bを何本のPRへまとめるかは、前段の差分・CI結果・既知FAILの表示を見て決める。いずれも文書上の限定受入を、Phase 4全体・実provider・一般運用復旧保証へ拡張しない。

この表は**PR境界の判断入力**であり、後続PRの作成・mergeや追加検証の承認ではない。`f5e2672..fe93b5d`の詳細な分割、各PRのbase／head、CIで実行する範囲は当該段階で確定する。既存R3-B branchをrewind／force pushせず、履歴commitから専用integration branchを切る方針を維持する。

## 8. 安定統合を優先した実行順の第一候補（2026-10-14）

残sourceのPR数を減らすだけでなく、各headを受入・通常buildが成立した履歴断面に置く。`f5e2672..fe93b5d`を1 PRにすると129 file／約15,635追加行となるため分ける。一方、B2を通常build分離の前で切ることは避け、`fe93b5d`をB2の統合出口とする。R1とR2、R3-AとR3-Bはそれぞれ同じReference非同期経路としてまとめ、個々の受入境界はPR本文内で区別する。以下の**7 PRはPR #44がmerge・main CI確認済みとなった後の順序案**であり、PR番号や作成を確定するものではない。

| 順 | 範囲・head | 内容 | 差分規模 |
|---:|---|---|---:|
| 1 | `be4bce1..f5e2672` | Tooling局所検証A／B／C | 45 file／約5,007追加行 |
| 2 | `f5e2672..08f4740` | Reference初回基盤、管理scope／TTL、対象・運用Evidence harness | 72 file／約5,739追加行 |
| 3 | `08f4740..6af0d3e` | B1読取接続と選択検証 | 23 file／約2,550追加行 |
| 4 | `6af0d3e..fe93b5d` | B2保護issue／read、結果受入、**通常build分離まで** | 47 file／約7,347追加行 |
| 5 | `fe93b5d..5f31d41` | **Framework規約R0**、Rule 28／29・Level選択 | 41 file／約3,098追加行 |
| 6 | `5f31d41..66d9b8f` | R1正常非同期経路＋R2失敗分離 | 53 file／約3,322追加行 |
| 7 | `66d9b8f..1d32b43` | R3-A単発全停止復旧＋R3-B受理log／UNKNOWN／人手HOLD、手動HTTP証拠 | 40 file／約3,396追加行 |

運用順は各段階で同じにする。直前PRのmergeとmain CI結果を確認し、次の候補headまでの差分・受入境界・通常構成保護を点検する。専用integration branchは当該履歴commitから作り、最新mainとの祖先関係とPR差分を確認する。GitHubが`BEHIND`または最新baseを要求する場合は、履歴をrewind／rebase／force pushせず、当該integration branchへ最新mainをmergeして差分を再照合し、必要なCIを確認する。PRは1件ずつ進め、失敗・予期しない差分・conflictがあれば次段を開始せず原因と修正範囲を判断する。

特に順5のR0はFramework-ownedの規約codeであり、順6のReference非同期実装に埋め込まず個別reviewする。順6はR1・R2の限定結果、順7はR3-BのB03a自動完了FAILを残した人手HOLDとE08残枝をPR説明に明記する。追加の広範な検証を自動的に起動せず、各PRのrequired checksと既存Evidenceが示す範囲を対照する。PR本文・CI・mergeの個別判断は、各段階の具体的な差分確認後に行う。

## 9. Ownerの統合順序了承と今回の作業境界（2026-10-14）

Architecture Ownerは§8の順序で進める方針と、最後のReference非同期実装を**R1＋R2、続いてR3-A＋R3-Bの2段階でmainへ統合する方針**を了承した。CIに時間がかかることを認識した上で、PRごとの確認を維持する。これにより、Framework規約R0を非同期実装より先に独立して統合する。最終段まで成立すれば、mainにはコミット済みR3-B成果`1d32b43`までが反映されるが、未commit差分、R4／R5、Phase 4全体受入は含めない。

今回の作業は、本書の了承記録を現R3-B branchでlocal commit・remote pushして保全し、既存の文書26件[PR #44](https://github.com/zaziedlm/KOIKI-JAVAWEB/pull/44)のCI完了・PR差分・merge条件を確認して、成立時に当該PRをmainへmergeするところまでとする。§8の後続7 PRは次回以降に各差分とCIを確認しながら順次進め、今回作成・push・mergeしない。PR #44のCI失敗、意図外差分、merge条件不成立があればmergeせず停止し、原因と次の判断を記録する。

## 10. Owner採用：残sourceを5段階で統合（2026-10-14）

§8〜9の7段階・非同期2段階案は判断時点の履歴である。Architecture Ownerは、**旧②＋③を1 PR、旧⑥＋⑦を1 PRにまとめ、残sourceを次の5段階で統合する**方針を採用した。文書26件の[PR #44](https://github.com/zaziedlm/KOIKI-JAVAWEB/pull/44)はCI全9件成功を確認後、merge commit `fcb1fcc1ab00a485c6f438fcdbdc1c88f291bda9`としてmainへ統合済み。下表はその後の順序であり、後続PRの作成・実行をこの記録だけで開始しない。

| 順 | 履歴範囲・head候補 | 統合する主題 | 差分規模 |
|---:|---|---|---:|
| 1 | `be4bce1..f5e2672` | S1局所検証A／B／CのToolingとEvidence | 4 commit／45 file／5,007追加・15削除行 |
| 2 | `f5e2672..6af0d3e` | Reference初回基盤、scope／TTL、対象・運用Evidence、B1読取接続 | 17 commit／93 file／8,289追加・21削除行 |
| 3 | `6af0d3e..fe93b5d` | B2保護issue／read、結果受入、通常build分離まで | 9 commit／47 file／7,347追加・1削除行 |
| 4 | `fe93b5d..5f31d41` | Framework規約R0：Rule 28／29とLevel選択 | 5 commit／41 file／3,098追加・8削除行 |
| 5 | `5f31d41..1d32b43` | Reference非同期R1正常経路、R2失敗分離、R3-A全停止復旧、R3-B受理log／UNKNOWN／人手HOLD、手動HTTP証拠 | 11 commit／86 file／6,694追加・21削除行 |

順2と順5は複数の限定結果を1 PRへまとめるが、各commitとOwner受入・未達の境界をPR本文で個別に示す。特に順5ではR3-BのB03a自動完了FAILを残した人手HOLD、E08残枝、DoD 4-2／4-3未達、通常rootの通常構成保護という限界を明記する。順3はB2を通常build分離まで含む成立断面で切り、順4はFramework-owned規約codeをReference非同期実装と分けて先に統合する。

実行方法は§8の1 PRずつの確認を維持する。各段階で差分とhead・base、required checks、mainへのmerge後CIを確認し、意図外差分・失敗・conflictでは次段を止める。順2の93 fileと順5の86 fileはレビュー負荷が大きいため、内容やCIで切り分けが困難になった場合は§8の元の境界へ戻す。既存branchをrewind／force pushしない。今回の採用は後続PR操作・追加検証・R4／R5・Phase 4全体受入の一括承認ではない。
