# Phase 4 S1 R3-B：受理観測・UNKNOWN照合の最小開始票案（2026-10-14）

状態：**§7 OWNER APPROVED CONDITIONAL START / STATIC PATH FOUND / SOURCE FIX PENDING**。[R3-B契約レビュー§8〜9](phase4-s1-r3b-purpose-log-provider-unknown-owner-review-20261014.md#8-第一候補の設計基線採用2026-10-14)の第一候補は設計基線としてOwner採用済み。§1〜6は承認前に提示した候補と条件、§7はその後のOwner承認・blocking静的点検を記録する。承認は文書source固定・clean branch・fresh preflightの順を含み、条件成立前のcode／SQL／test作成・Maven／Docker検証、結果受入やremote操作を先取りしない。

## 1. 目的・成果境界

Referenceの非同期通知について、provider受理とReference保存・publication完了がずれた場合を、R3-Aの全停止・単一対象復旧モデルの中で安全に照合する。E07（受理観測log保存後・publication完了前）、E09（key欠落／異内容／期限外）、E10（受理後応答喪失／受理前切断／照会不能）の**新規7枝**を第一候補とする。同一通知の逐次再要求は必要な直接回帰へ含めるが、E08のstub並行要求は故障位置と競合成立条件を別判断し、通常listenerと復旧processの並走は含めない。

同期のexpense承認と同transactionのAudit／publication保存、commit後の非同期通知、通常構成での無効を維持する。Reference-owned受理観測logは「受理ID付き応答をReferenceが耐久保存した」事実であり、provider独立台帳の代用品ではない。実provider配信、実運用のkey保持・purge、正式operator認可／Audit／permit、無停止復旧、R4／R5、DoD全体・正式受渡し・Phase 4全体は除外する。

## 2. 実装前に固定する契約

| 項目 | 固定候補 | 未成立時 |
|---|---|---|
| provider結果 | Reference `NotificationProviderPort`は受理ID付き確定成功、明示拒否、受理不明を区別する。IOException／timeout／応答喪失を未受理へ変換しない。照会もloopback専用Adapterに閉じる | 分類不能はUNKNOWN／HOLD。例外だけを根拠にPOSTしない |
| 照会と期限 | stubは対象key・payload hash・論理宛先の登録済み契約と固定時刻／期限を持つ。受理行がない照会は「照会時点で未記録」だけを意味する。同key同内容の有効期間内冪等保証と全停止・対象照合がそろう場合に限り再要求候補となる | `NOT_FOUND`単独・期限証拠なし・key欠落／異内容・照会不能はHOLD。期限後の過去receiptは独立証拠に残す |
| 受理観測log | event ID、key、payload hash、論理宛先、受理ID、観測時刻をReference `notification`に保存し、後で対象publication ID／listener IDと照合する。provider確定成功後、publication完了より前に独立commitする | 保存がrollbackする方式、logだけによる受理断定・未送信断定ならBLOCKED |
| 復旧 | 全停止証拠・advisory lock・対象snapshot再照合とprovider照会を送信前に維持。受理一致なら追加POSTなしでlistener完了へ進める方式を検討し、Modulith完了契約で実証する | 完了API／listener契約で安全に実装できなければSTOP。publication DBの直接UPDATEで代用しない |
| HOLD | 理由code・publication／event・key hash・観測時刻・次の確認先を復旧子から構造化してTooling独立Evidenceへ残す。異常終了時もTooling側で不明を保持する | test内assertionや子logだけで消えるHOLDは不可。正式運用台帳・Auditは未達を明記 |

`@ApplicationModuleListener`のtransactionとpublication完了記録順、event IDからpublicationへの対応、provider照会後に追加POSTなしで完了できる経路は**実装前のblocking静的点検**とする。これらが成立しない場合は別案の差分・停止位置・case数を示し、本票の数値のまま実装しない。HTTP 503や接続断を一般に「受理前の明示拒否」と扱わず、stub台帳で受理前拒否を独立確認できた枝だけその結果を採用する。stubの照会認証・意味は隔離loopbackだけに限定し、実provider契約へ転記しない。

## 3. Source・差分候補

入力source候補はR3-A受入済み`c8d406b713fed97269ca5843ecf31b79824593ad`を祖先とするclean commit。未追跡の[main統合点検案](phase4-s1-main-integration-first-pr-scope-review-20261014.md)、[R3-B契約レビュー](phase4-s1-r3b-purpose-log-provider-unknown-owner-review-20261014.md)と本票の承認記録を、Ownerによる対象点検・local commitで固定してから、新R3-B branchを作る。branch名候補は`feature/phase4-s1-reference-unknown-recovery-r3b`。文書commitはOwner操作または操作前の個別承認、remoteは別承認。R3-A成果branchの既存remote参照をrewind／force-pushしない。

変更候補はReference `notification`のprovider Port／dispatch／loopback Adapter、受理観測logのPort／保存Adapter・Application境界、単発復旧Service／条件付きConfiguration、Reference-owned `kkref-notification-async`の**新規migration**、必要なReference testとTooling `reference-e2e-verification`のstub／故障gate／package済みcaseに限定する。新migrationはV5を書き換えず、async opt-inのFlyway locationだけへ追加する。通常properties、既存V4／V5、Framework Public API／Rules、Customer、Security、Root Reactor／CI、POM／依存変更は初回候補外。必要が判明したら実装前にSTOPして改訂判断へ戻す。

候補上限はmain／test／migrationの変更・新規あわせて**28 file以内**、文書**4 file以内**。この数は対象path精査前の上限案であり、file数だけを満たせば対象外の変更を許す意味ではない。

## 4. 検証候補と順序

| phase | 候補集合 | 順序・判定 |
|---|---|---|
| 契約・compile | Referenceのlog保存順、provider結果分類、通常無効を確認する新規最大3 method。offline test-compileと通常／async package各1回 | 静的blocking点検とfresh preflight成立後のみ。通常／async JARを別hashで固定し、classes／runtime依存を対照。契約失敗でpackage済み故障caseへ進まない |
| E07 | B01 1件 | log独立commit後・publication完了前で停止。元process終了、log／provider受理、同一対象と別対象、復旧完了を独立照合。停止位置が作れなければBLOCKED |
| E09 | B02a〜c 3件 | key欠落、同key異内容、期限外を別fresh caseに分け、送信増分0・理由付きHOLDを確認。期限後の過去受理証拠を消さない |
| E10 | B03a〜c 3件 | 受理後応答喪失、受理前切断、照会不能を別fresh caseに分ける。照合できる枝だけ受理総数1・終端、確認不能は送信増分0・HOLD |
| 直接回帰 | 既存A01／A03／D01／C04を候補4件 | 変更したlistener・provider・復旧の正常送信前／受理後・同receipt・別通知を選択確認。実行前に対象methodを固定し、旧PASSを再計上しない |
| 通常root | 最大1 Maven invocation | 上記が成立した後だけoffline通常`clean package`。fresh XML、通常無効、専用skip、通常JARと固定通常JARのclass／runtime内容を照合 |

候補は**新規最大10 method（契約3＋package7）＋既存4 method＋通常root最大1回**。各集合は初回1回、追加再実行0回。E08並行枝、新規R4／R5 case、過去R3-A10件の全件反復は含めない。JAR外側のbyte hash一致を通常／async対照の要件にせず、app entry／runtime依存の内容を比較する。Xml・子log・DB／stub証拠をrun epoch別に保持し、履歴FAILを新PASSで上書きしない。

## 5. 有限資源・停止／cleanupの候補

**新規150分、うちcleanup10分予約**。Tooling選択caseは所有JVM最大4、PG1／Ryuk1／DB client8、各JVM heap上限768 MiB。通常rootはJVM4、PG1／Ryuk2／DB client14。開始前に空きmemory8 GiB・disk10 GiB、今回raw log100 MiB・作業領域2 GiB、子起動120秒・通常終了30秒・所有子強制停止5秒を候補とする。数値はR3-A残予算の流用ではなく、R3-B専用の新規上限案。連続観測できない最大使用量は終了時0から推定しない。

開始前にJDK21／Wrapper・offline依存、Docker Server／必要image、保護process、所有container0、branch／HEAD／source hash、通常構成の対象範囲をfresh preflightで確認する。今回所有の隔離DB・schema／role、loopback stub、子JVM・故障gateだけを操作し、他sessionや実環境を触らない。想定外のprovider副作用、logとreceiptの不整合、`NOT_FOUND`からの盲目的送信、期限不明、誤対象、通常構成の入口露出、lock喪失、資源／時間超過、cleanup未成立では直ちにSTOPし、追加修正・再実行を自動で進めない。tmpの固定JAR／XML／logはGit移送されないので所在・hash・保全方法をEvidenceに記録する。

## 6. Ownerが確認する点

1. §2のprovider照会・受理観測log・HOLD・復旧完了の契約が、現行ModulithとReferenceの境界に適合するか。特に追加POSTなしの完了経路はblocking確認とする。
2. E07／E09／E10の新規7枝と契約3件・既存4件・通常root最大1回という候補集合、E08並行枝を今回外すこと。
3. 28 file／文書4 file、150分・資源／cleanup・再実行0回の候補上限と、必要正本・承認文書のlocal commit→clean新branch→preflightという開始順序。

上記の確認と必要な訂正が済むまで**実行開始は未承認**。特にblocking静的点検が未成立のまま、実装・migration・Maven／Docker検証を開始しない。結果受入、local commit、remote push、最初のPR・main mergeはそれぞれ別判断とする。

## 7. Owner承認とblocking静的点検（2026-10-14）

Ownerは本票を確認して承認した。§2〜5の限定範囲と上限、文書固定→clean branch→fresh preflight→成立時の作成・検証を**条件付き開始**として扱う。実行途中のSTOP条件、追加再実行0回、結果受入・remote操作の別判断を維持する。上記§6の「未承認」は本承認より前の履歴である。local commitは今回承認した文書固定手順の対象3件を点検してから実施し、commit／source hashを別記録する。

承認直後にcode／test／SQLは変更せず、現行sourceと採用済みSpring Modulith **2.1.1**のlocal JARを読み取り点検した。`ExpenseApprovalListener`はeventだけを受け、`SinglePublicationRecoveryService`は対象publication IDに限定した`resubmitIncompletePublications`を呼ぶ。R3-AのJDBC Adapterは対象行のeventを復号し、event ID・通知key・payload hashと選択snapshotを比較する。これにより、log書込時にpublication IDを推測せず、復旧時に対象ID・event IDと照合する経路がある。

`ApplicationModuleListener`の採用JAR注釈には`@Async`、`@Transactional(propagation=REQUIRES_NEW)`、`@TransactionalEventListener`が付く。採用JARの`CompletionRegisteringAdvisor$CompletionRegisteringMethodInterceptor`はlistener `MethodInvocation.proceed()`の正常返却後に完了遷移を登録し、例外時は未完了のまま扱う。したがって、provider照会が`ACCEPTED_MATCH`ならlistener bodyでPOSTを省き正常返却させる**候補経路は静的に存在する**。ただしAdviceとtransaction commitの実行順、実際のDB完了時点、非同期failure時の状態はこの静的読取だけでPASSとしない。受理観測logをlistener transactionから独立commitさせる実装と、fresh DB上でlog commit後・publication完了前に止める故障位置が必要である。

**blocking静的点検の結論：実装候補の経路あり／実動作未証明。** 開始票の順序どおり文書source固定・clean branch・preflightへ進める。package済みE07でlogの耐久保存とpublication未完了を同時に観測できない、照会後無POSTで完了できない、または対象publicationを誤選別する場合はBLOCKEDとし、直接DB UPDATEや期待値緩和へ切り替えずOwner判断へ戻す。使用JAR・source／artifact epoch・実結果をEvidenceへ記録する。
