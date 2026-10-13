# Phase 4 S1 R3：目的・復旧方式・最小検証範囲のOwnerレビュー案（2026-10-13）

状態：**R3-A FULL-STOP MODEL OWNER ADOPTED / MINIMAL START CONTRACT UNDER REVIEW（§7）**。R2限定結果の受入・成果commit／pushを入力に、R3の方式と初回実証範囲を整理した。全停止型の採用判断と、具体的な実装・検証開始条件の成立を区別する。

## 1 入力と現状

[R2結果§5](../architecture/validation/phase4-s1-r2-selected-seven-case-results-and-acceptance-input-20261013.md#5-r2限定結果のowner受入承認2026-10-13)は、選択した通知失敗と業務確定の分離、拒否・rollbackの副作用抑止を`COMPLETE / OWNER ACCEPTED`とした。成果sourceは`feature/phase4-s1-reference-failure-isolation-r2` / `66d9b8fb686405148269aa6eefed4819b8c53370`で、同branchをoriginへpushしremoteの参照hash一致とclean working treeを確認した。PR・mergeは行っていない。R2の初回FAILと訂正後PASSは別epochのまま保持する。

[段階対応表§3〜5](phase4-s1-purpose-dod-stage-mapping-20261009.md#3-実証段階と開始条件)では、R3を停止後の実配信・重複抑止・受理結果不明の照合、R4を認可・Audit付き復旧操作、R5を保持・追跡に分けている。[DoD実演計画](phase4-s1-dod-demonstration-plan-20261005.md#3-caseとdodの対応)のE04〜E10は検証候補の母集団であり、R3開始時に全枝の実行を自動承認するリストではない。

現行Referenceは`ExpenseApprovedEvent`の通知keyを申請IDと承認versionから安定生成し、async設定では起動時の自動再公開を無効にしている。[PL2 Evidence](../architecture/validation/phase4-pl2-level2-verification.md#31-再公開の排他方針候補toolingでの検証)では、専用復旧process同士のlock排他は確認されたが、生存中の通常listenerやlock接続喪失後の送信境界まで保護された結果ではない。PL2のPASSをReferenceのR3 PASSへ転記しない。

R1の`ExpenseApprovalListener`は現時点でproviderへの送信を呼び出すだけで、**Reference-owned通知logの保存点は存在しない**。そのためE07の「通知log保存後・publication完了前」は、既存実装の停止位置として直ちに検証できない。logの必要性、保存transaction、provider受理後の記録失敗とUNKNOWNの扱い、追加migrationの要否を別の設計判断へ出す。

## 2 R3の目的と出口

目的は、package済みReferenceが保存した**同じ対象publication**を、元の実行が停止した後に安全な判断を経て完了させること。停止位置、元processの世代・終了、DB状態／試行回数、provider stubの到着・受理、固定通知keyとpayload、復旧process、最終publicationを相関させる。受理済み／受理不明をDB statusや通知logだけから未送信と決めず、同じkeyの照会または契約上安全な再要求ができなければHOLDとする。

限定成功枝では、承認・Auditを変更せず、対象publicationをCOMPLETEDへ進め、stubの**論理受理総数1件**と別対象の不変更を確認する。拒否・HOLD枝は追加副作用0と理由・次の照合先を確認するが、それだけでDoD 4-2の配信完遂PASSにしない。所要時間は全体復旧時間と操作開始後時間を分けて記録し、許容値はOwner判断前に固定しない。

実provider配信、無停止での一般復旧保証、permit consume／close・正式なoperator認可／Audit、保持／purge、実trace、正式受渡し、Phase 4全体の開始は今回のR3整理から除く。R3の限定検証用入口を正式運用入口へ自動昇格させない。

## 3 R3開始前の方式判断

| 候補 | R3で必要な保護 | 残る負担・判断 |
|---|---|---|
| 全停止メンテナンス型 | 対象を処理し得る通常process／executorを全て停止・drainし、再起動・別instance移譲を抑止。世代付き終了証拠とprovider受理を照合してから、対象1件だけを専用復旧processで扱う | Web停止範囲・許容復旧時間・担当／代行、停止確認の真正性、停止から再開までの制御を定義する。B2の全停止実証をそのままR3の停止証明には使わない |
| 稼働中個別復旧型 | 他通知を継続しつつ対象の旧workerを排除する所有／世代fencingと送信境界の保護。専用復旧process同士のlockに加え、通常listenerとの競合を閉じる | 既存PL2 lockだけでは不足。外部副作用まで含むfencing・provider契約と負例の設計／実証が必要 |

**初回限定R3の推奨候補は全停止メンテナンス型。** 理由は、現行Referenceに通常listenerを対象単位でfenceする機構がなく、PL2で通常listenerとの並走が実測されているため。これは方式の提案であり、B2が全停止だったことによる自動採用ではない。OwnerがWeb停止を許容できない場合は、この候補でR3を開始せず、稼働中型のfencing設計を先にreviewする。

全停止型を選ぶ場合も、PIDや確認fileの存在だけで停止を証明しない。関連process・起動世代・対象listener／publicationの対応、platform終了と独立した観測、別instanceへの引継ぎ停止、復旧中の再起動抑止、再開順と破れた場合のHOLDを開始前に具体化する。専用lockは専用復旧process同士の排他に限り、provider側の安定key・同key同内容の重複受理防止を併用する。

## 4 最小実証を二段階で定義する案

まず**R3-A**を、全停止型をOwnerが採用し、停止・provider・対象選別契約が具体化した場合の最小開始候補とする。各caseはpackage済みReference／隔離DB／独立stub／別processを通し、今回だけの故障位置制御をToolingに閉じる。対象1件・1起動世代・1復旧processを基本とし、同じ固定sourceとartifact、fresh XML・子log・DB／stub照合を保存する。以下は実行許可ではなく開始票のcase候補である。

| 候補 | 最小確認と失敗時の停止 |
|---|---|
| A01 / E04 | publication保存後PUBLISHED・送信前0件で元processを停止。世代と実終了、対象snapshotを確認して対象1件を再公開。COMPLETED／受理1件、承認・Audit不変、別対象不変更 |
| A02 / E05 | PROCESSING・provider到着前の停止。DB statusだけで未送信とせずstub独立観測と元process終了を確認。同じevent／keyで完了・受理1件 |
| A03 / E06 | provider受理直後・listener完了前の停止。別transactionのstub受理を確認し、同じkey／payloadの安全な再要求または照合で受理総数1件・publication完了を確認。受理不明を未受理へ読み替えない |
| A04 / 安全負例 | 元listener生存または停止証拠なしでは対象の再公開をせずHOLD。試行回数／statusが変わった古いsnapshotと別publication選択を拒否し、復旧側のprovider要求0を確認。生存中の元listenerが処理する可能性と区別する |
| A05 / 保護回帰 | async無効の通常構成で復旧入口・追加送信が起動しないこと、既存正常通知と同key／同payloadでstubの元receipt再利用・受理総数1件を選択確認。Reference production変更があれば通常rootを最終1回の候補とする |

R3-Aの最小出口はA01〜A05の成立、source／artifact・停止世代・時間とcleanupの照合、および残るE07／E09／E10と運用境界を明記したOwner判断とする。A04は「元process生存」と「古い対象」の別負例として実行数を数え、開始票でmethod名・invocation数を確定する。成功枝だけ、またはHOLD枝だけではR3全体やDoD 4-2／4-3のPASSにしない。通常rootの範囲も変更pathと既存回帰の影響に合わせて開始票で確定し、過去のR1／R2実行回数を再利用しない。

**R3-B**としてE07の通知log保存境界、E09のkey欠落／異内容／期限切れ、E10の受理後応答喪失／受理前切断／照会不能を扱う。R3-Aの結果、通知logの所有・保存契約、stub照会／key保持期間、UNKNOWNから同key安全再要求できる条件、HOLD時の記録・担当を入力に、枝ごとの件数・予算・停止条件を別票で判断する。現状のlog未実装や照会契約未確定を、test期待値の緩和や「受理0」の推定で埋めない。E08の並行・別通知識別もR1の正常枝だけで完了扱いせず、R3-Bで必要な差分をreviewする。

## 5 実装・環境を開始する前に確定する契約

1. **方式・停止範囲：** 全停止型か稼働中型か。前者なら対象全process・executor・再配置経路、Web停止影響、停止証拠の発行元・世代・有効期限、再開条件、担当と代行。後者なら旧workerと外部送信を含むfencing契約と負例。
2. **対象・送信：** Reference-owned publication／event／listenerの厳密な対応、statusと`completion_attempts`の読取・lock後再照合、対象1件の再公開API、初回listenerと専用復旧processの分離。DB行の直接UPDATEを復旧手段としない。
3. **provider・UNKNOWN：** stubの独立到着／受理、key・payload・宛先の同一性、照会応答の意味・遅延、同key受理結果再利用と保持期間。実providerへこの保証を転記せず、確認不能時はHOLD。
4. **運用入口：** R3の隔離実演に使うReference-owned明示opt-inの単発復旧方式とTooling故障注入・process制御を分ける。非HTTP認証・operator権限・Audit・permitを欠いた入口を通常起動や正式配布で使用可能にしない。R4への接続は別review。
5. **有限実行：** 新しいclean source固定、JDK／Wrapper・依存、Docker／image・browser要否、通常／async artifact、所有PID／container、JVM／DB／Ryuk／client・heap・log／raw・総時間、case順序と再試行0回、失敗時停止・cleanupを開始票に数値で定める。R2のrunner・残予算・固定JARをR3入力として流用しない。

R3-Aでも停止位置のTooling switchやschema／migration・Reference復旧入口が必要となる可能性がある。候補path・依存／SQL差分・回帰件数は方式選択後の設計reviewで固定し、今回先行生成しない。Framework Public API／Starter／Rules、CI／workflow、実provider接続は含めない。

## 6 Owner判断の順序

今回の判断入力は、①R3の目的とR3-A／B分割、②初回方式を全停止型で検討するか、③§5の未確定契約をどの資料で閉じるか、である。**本書の確認だけではR3-Aの作成・検証開始を意味しない。** 方式を選んだ後、対象path・E04〜E06と安全負例の正確なinvocation、回帰、有限予算・資源・停止／cleanupを記した正式開始票をOwnerが別途判断する。

R3-Aが成立してもE07／E09／E10・E08残枝、R4のoperator認可／Audit・permit consume／close、R5の保持・実trace、DoD全体、正式受渡し、Phase 4全体は未達として維持する。R2ブランチのPR／mergeやR3 branch作成・commit／pushも個別判断とする。本書の作成ではcode／test／POM／SQLを変更せず、Maven／Docker検証は行わない。

## 7 OwnerによるR3-A全停止型の採用と契約具体化指示（2026-10-13）

Ownerは本書を確認し、「R3-A の全停止型を採用として、停止・provider・対象選別契約を具体化し、最小開始と定めます」と明示した。これにより§3の全停止メンテナンス型をR3-A初回方式とし、§4のA01〜A05を最小開始範囲として具体化する。[R3-A契約・開始票](phase4-s1-r3a-full-stop-contract-and-minimal-start-20261013.md)に、全停止の確認、stub受理、対象1件の選別、故障位置と必要回帰、有限資源・停止／cleanupを落とす。

この判断はR3-B、実provider、無停止個別復旧、R4／R5や正式受渡しを含めない。個別の開始票で入力source・実装path・故障制御・検証回数・予算が確定し、その前提を満たした限定作成・検証だけを対象とする。未確定条件を既存R2予算やPL2 ToolingのPASSで補わない。commit／push／PR／mergeは個別判断を維持する。
