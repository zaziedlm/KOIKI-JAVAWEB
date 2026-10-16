# S1 R4：受理済みFAILED一件の人手HOLD引継ぎ案（2026-10-16）

状態：**§1〜6はOWNER確認済み。既存記録点検§7とclose/別publication整理§9の結論・制限はOWNER APPROVED（§8・§10）**。方針の根拠は[R4レビュー§10](phase4-s1-r4-failed-resolution-purpose-and-boundary-review-20261016.md#10-aの設計候補留保人手hold維持のowner承認2026-10-16)。Aは設計候補に留め、人の照合・担当付きHOLDを維持する。これは新たな復旧操作の許可や、運用配備済みの認定ではない。

## 1. 対象と出口

対象はB03a型の「provider受理あり、Reference受理観測logなし、publicationはFAILED」の一件。[既存の人手HOLD実証](../architecture/validation/phase4-s1-r3b-source-preflight-and-blocking-review-20261014.md)を入力とし、HOLDを人へ引き継ぐ手順だけを整理する。

出口は、担当と確認先が分かる未解決案件として保持すること。provider受理の確認だけではpublication完了・DoD達成・処置済みにしない。人が照合できた場合も、未承認の手動DB更新や復旧runner起動へ進まない。

## 2. 既存運用記録に残す最小項目

一件の既存インシデント記録等に、次の情報をまとめる。新table・受付API・新しい状態enum・自動受付の仕組みは追加しない。実際に使う記録先は未指定であり、運用Ownerが選定する。

| 項目 | 残す内容 |
| --- | --- |
| 対象 | 環境、publication/event/listener、観測時のstatus/attempt。通知keyは原文を広く転載せずhashまたは権限制御された参照で識別 |
| 照合材料 | provider受理IDと通知key/payload/宛先の対応、Reference受理記録の有無、観測日時・証拠保管先。原文payload・宛先・credentialは共有記録へ複製しない |
| 未完了と禁止 | 例：「provider受理は一致。Reference記録とpublication完了は未整合。追加通知送信・再公開・新permitによるやり直しは禁止」 |
| 既存操作との対応 | permit/operationがあればIDと消費有無・結果Audit参照。存在しなければ「なし／未確認」とし、引継ぎのためだけに発行/consumeしない |
| 担当と次の確認 | 主担当、連絡/引継ぎ先、次回確認時点と確認する内容。実名・連絡先・時刻は運用側が設定し、不明なら未割当として責任者へ通知する |

## 3. 一回の確認手順

1. **引受ける**：既存記録と証拠の所在を確認し、対象一件・担当・禁止事項を引継ぎ先と確認する。証拠が端末tmpだけなら、Gitでは移送されないことを明示し、所在/取得可否を確認する。新しい保存基盤の構築は本手順の範囲外。
2. **照合する**：許可された読取手段で、対象publication、provider受理、Reference記録、既存permit/operation/Auditの対応と観測時点を確認する。元process停止が不明ならその旨を残す。FAILEDやlog不存在から未送信を推定しない。
3. **引継ぐ**：確認結果と未完了箇所を同じ記録へ追記し、次の担当・確認時点を定める。照合不能・不一致・停止不明は、理由付きHOLDのまま運用責任者と必要なprovider/Reference担当へ判断を依頼する。担当不在や連絡不能も解決済みにせず、管理責任者へ引継ぎの判断を求める。

受理一致の確認が取れても、現在の方針ではHOLDを維持し、必要ならArchitecture/運用Ownerへ処置方式の再検討を依頼する。記録を読んだだけで消費済みoperationを再開しない。確認頻度や連絡期限の数値は本書で創作しない。

## 4. 再投入を止め続ける条件

この対象は受理済みの可能性を排除できず、かつpublication完了が未確認である限り、通知送信・再公開・新しいpermit/operationによる再試行を行わない。人の照合完了、担当交代、時間経過、permit期限切れ、照会結果のNOT_FOUNDへの変化は、その禁止を解除する根拠にしない。

許可済みの受理照会は通知送信と区別する。現行fixtureの受理照会は`POST /notifications/receipt`であり、「通知送信POST禁止」を「照会済みだから安全に送信可能」と読み替えない。本書自体は実provider通信やDB操作を許可しない。

## 5. 運用側で埋める未決事項と受入範囲

- 記録先とアクセス権、主担当/代行/運用責任者、provider/Reference確認先。
- 証拠の保管先・参照権限・保持、次回確認時点と連絡できない場合の引継ぎ方法。
- 正式な処置方式を再検討する必要が生じた際の判断窓口。

上記は実案件/運用情報が未取得のためOPEN。担当者が設定して引継ぎを確認するまでは、運用体制成立とは認定しない。文書を承認しても、FAILED完遂/E10a/DoD未達、Aの未実装、正式入口・proof供給等の残課題は変わらない。

本案は既存のHOLDを扱う最小引継ぎ手順のreview対象。code/test・migration・追加実証・運用操作・commit/remoteは含まない。

## 6. Owner確認・未確定事項の了解（2026-10-16）

Ownerは本案を確認し、現時点で確定しない点があることも了解した。これを本書の内容確認と、§5の未決事項をOPENとして維持することへの了解として記録する。

実際の記録先・担当/代行・連絡先・証拠保管条件・次回確認時点を確定したとは扱わない。運用体制成立、実際の引継ぎ実施、FAILED解決、Aの実装/実証開始を認定するものではない。承認済みの人の照合・担当付きHOLD方針と再投入禁止を維持し、未確定値を推測で補わない。

## 7. 既存記録で必要十分かの点検（2026-10-16）

状態：**SOURCE/既存Evidence照合による結論・制限はOWNER APPROVED（§8）**。Owner指示により、S1-03の初回整理を「一件を照合し、再投入せず担当へ引き継ぐ」用途に限定した。新しい証拠保管基盤・肯定proof・復旧操作の実装へは進まない。以下はschema・code・既存検証記録の読取であり、実運用DBやproviderの現物確認ではない。

### 7.1 確認した記録と役割

| 既存記録 | 確認できること | 一件の引継ぎで補うこと・限界 |
| --- | --- | --- |
| `kkref_notification_async.event_publication`（V5） | publication ID、listener/event型・serialized event、status/attempt、発行/完了/再公開時刻 | 環境と観測時点を運用記録に結ぶ。行は現在値なので、過去のFAILED/attemptの証拠には当時のsnapshotも必要。FAILEDだけでは未送信と分からない |
| providerの受理証拠 | R3-Bではstubの独立transactionによる受理ID・通知key・event・宛先・payload hashを照合した | 実providerの記録/照会先、信頼できる取得者、取得時点・保持条件はOPEN。stub台帳はtest所有DB内であり、本番providerの耐久保管を証明しない |
| `notification_receipt_observation`（V6） | Referenceが観測した受理IDとevent/宛先/key/payload、観測時刻。Adapterは既存行との不一致を拒否する | B03aでは不存在が既知。不存在を受理なしとしない。引継ぎのためだけに成功行を挿入しない。provider証拠の代わりにはしない |
| permit/消費記録（V4） | 環境と対象、発行者・有効期限・閉鎖、operation/worker世代・消費時刻。既存readはscope/権限を検査 | 存在する場合だけ参照。ない場合は「なし／未確認」。発行/消費をHOLD記録作成の前提にしない。消費済みは実行完了の証明ではない |
| `koiki_audit_event`と結果Audit | 既存permit操作とactor/result、permit resource、接続実証のoperation resourceをたどれる | 未実行のHOLDでは結果Auditはない。担当・次回確認・恒久的な再投入禁止が自動保存されるわけではない。新しいAudit eventの実装は不要とする案 |
| B03aのHOLD JSON・XML・結果文書 | 理由、対象ID/key hash、受理ID、元process世代/停止観測、観測時刻、status/attempt、前後件数、次の照合先を保持した限定実証 | JSON単独には環境/listenerや照合材料の全内容、担当、保持条件が揃わない。run/source/artifactの文脈と証拠への参照を合わせる。端末ファイルは本番の保管契約でも改ざん不能な台帳でもない |

source根拠：[V4](../../koiki-reference-app/src/main/resources/db/migration/kkref-notification/V4__create_notification_recovery_records.sql)、[V5](../../koiki-reference-app/src/main/resources/db/migration/kkref-notification-async/V5__create_notification_async_publications.sql)、[V6](../../koiki-reference-app/src/main/resources/db/migration/kkref-notification-async/V6__create_notification_receipt_observations.sql)、[受理記録Adapter](../../koiki-reference-app/src/main/java/org/koikifw/reference/notification/adapter/outbound/persistence/JdbcNotificationReceiptLogAdapter.java)、[permit Service](../../koiki-reference-app/src/main/java/org/koikifw/reference/notification/application/RecoveryPermitService.java)、[接続Service](../../koiki-reference-app/src/main/java/org/koikifw/reference/notification/application/PermittedSinglePublicationRecoveryService.java)、[HOLD作成test](../../build-support/reference-e2e-verification/src/test/java/org/koikifw/buildsupport/referencee2e/PackagedReferenceAsyncUnknownRecoveryTest.java)、[provider stub](../../build-support/reference-e2e-verification/src/test/java/org/koikifw/buildsupport/referencee2e/LocalNotificationProviderStub.java)。

### 7.2 必要な補完は、一件の運用記録への参照集約

**新tableを必要とする欠落は、今回の引継ぎ用途では確認できていない。** 技術的な照合材料は既存記録を利用し、§2の既存運用記録に次の三つを補う案とする。

1. 対象環境・対象ID・観測時点と、証拠保管先/run/sourceへの参照。同じ通知の材料を照合したことと、矛盾/未確認箇所を記す。
2. 「未解決HOLD・追加通知送信/再公開禁止」とその理由。担当交代・時間経過・NOT_FOUNDへの変化では解除しないことを引き継ぐ。
3. 担当/引継ぎ先と次の確認内容・時点。未割当や取得不能は空欄で済ませず、その事実と責任者への確認先を残す。

既存運用記録の実際の仕組みは未取得なので、更新履歴・参照権限・添付保管・引継ぎ確認ができるかはOPEN。別の一件管理台帳を新設する判断ではない。原文payload/宛先を共有記録へ集めず、必要な担当者が権限制御された元証拠へ到達できる形を優先する。

### 7.3 保管と使用時の最低条件

- **後から参照できること**：対象一件の調査/引継ぎが続く間、元記録または必要範囲の証拠を失わない保管先・担当・参照権限を定める。端末tmpの所在だけでは別端末への引継ぎ成立としない。現物の保存/取得可能性は未確認であり、本点検で複写・移送を実施したわけではない。
- **時点と出所が分かること**：観測時点、対象環境、取得者/取得元、必要なsource/artifact epochを区別する。既存のhashがある場合は照合に利用するが、hash単独で出所の真正性や停止維持を保証しない。
- **証拠が不足したら止まること**：欠落・期限切れ・取得不能・対象不一致・証拠間矛盾・状態変化はHOLDを継続し、理由と次の確認先を更新する。古いsnapshotを現在の実行許可や肯定ConsumptionProofへ流用しない。

保持日数・backup/復元・provider側の保持・削除権限は未決で、実運用入力とR5保持/パージの別判断へ残す。未解決HOLDの材料を消してよい根拠は本書では与えないが、既存の全削除経路に保護実装があるとも認定しない。

### 7.4 今回の結論（§8でOwner承認）

**照合材料の種類は既存記録で足りる。一件の運用記録への参照・禁止事項・担当/次確認の補完で進め、新table・新Audit・新保存基盤は追加しない。** ただし、実際の保管先・権限・保持・引継ぎ確認がOPENなので、運用上の必要十分性は条件付きであり、成立済みとはしない。

運用側の既存記録で上記が満たせない具体的事実が出た時だけ、その不足を限定して再検討する。正式復旧時の証拠使用中保護、fencing、肯定proof、S1-03全体の完了は今回の結論に含めない。追加実装・DB/provider操作・Maven/Docker検証・証拠移送は実施していない。

## 8. 既存記録点検の結論・制限のOwner承認（2026-10-16）

Ownerは§7を確認し、運用上の不十分さも了解したうえで承認した。これを**一件の人手HOLD引継ぎに関する既存記録点検の結論と制限のOWNER APPROVED**として記録する。

照合材料の種類は既存記録を利用し、対象/観測時点/証拠参照・未完了/再投入禁止・担当/次確認を既存運用記録へ補う方針とする。今回、新table・新Audit・新保存基盤は追加しない。

実際の保管先・参照権限・保持・引継ぎ確認等はOPENを維持する。この承認を運用上の必要十分性の成立、運用配備・引継ぎ実施、FAILED解決、S1-03全体/DoD完了、Aの開始と扱わない。具体的な不足が判明した場合に限り、その影響と必要範囲を示して再検討する。成果commit・remote・追加実装/検証・実運用操作は本承認に含めない。

## 9. 未閉鎖permitの保証範囲と、別publicationの扱い（2026-10-16）

状態：**S1-04の初回整理の結論・制限はOWNER APPROVED（§10）**。既存記録点検と承認はOwnerが`dbbe399`へcommitし、clean sourceを確認してから本整理へ進んだ。今回もcode・既存契約を読取確認した文書であり、close契約の変更や追加実証は行わない。

### 9.1 既存機構が止めるもの

[V4の部分unique index](../../koiki-reference-app/src/main/resources/db/migration/kkref-notification/V4__create_notification_recovery_records.sql)は、`closed_at IS NULL`の`(environment_id, publication_id)`を一意にする。消費済み・有効期限切れになっても、この条件から外れない。同じ環境・publicationに未閉鎖permitがある限り、通常の追加発行は制約で拒否される。

[現行close](../../koiki-reference-app/src/main/java/org/koikifw/reference/notification/application/RecoveryPermitService.java)は現在のCLOSE権限/scope、現在target一致、closure proofと保存済みtargetの全項目一致を要求する。attempt変更や対象削除後の歴史targetを閉じる仕組みではない。closeはpublication更新や通知送信を行わず、配送成功や次送信の許可を意味しない。

根拠となる[既存D11 test](../../koiki-reference-app/src/test/java/org/koikifw/reference/notification/OperationalRecoveryEvidenceDbTest.java)には、重複発行拒否、attempt変更/対象不存在時のclose拒否、期限経過後の未閉鎖維持とindex定義の確認がある。今回はsourceを読んだもので再実行結果ではない。別publication横断の実運用抑止を同testのPASSから推定しない。

### 9.2 最小ケースでの扱い案

| 観測したこと | 現行機構の境界 | 人が引き継ぐこと |
| --- | --- | --- |
| 同じ環境・publicationに未閉鎖permitがあり、消費済み/期限切れになった | 通常の新permit発行は一意制約で拒否。期限切れは枠解放ではない | 未解決なら保持し、新operation/close/削除で再試行の入口を空けない。消費記録と結果未確認を分けて記録 |
| 同じpublicationのattempt等が変化、または対象が見つからない | 現在target一致を満たせず歴史closeは拒否される。対象消失を解決とは判定しない | 発行時targetと現在の観測/不存在・観測日時を既存記録へ併記し、未閉鎖のまま照合担当へ。比較条件を緩めない |
| 別publicationが同じ論理通知に見える | 既存一意制約では新publicationのpermit発行を横断抑止しない。event ID一致/不一致だけでは同一論理通知を断定できない | 既存通知keyと業務上の通知識別、payload/宛先・受理証拠を権限のある担当が照合する。同じ又は対応不明なら既存の一件記録へ関連IDを追記し、追加送信/復旧操作を保留。新台帳は作らない |
| 元processの生存/再起動、照合後の条件変更、停止不明が判明した | permitの未閉鎖・lock・TTLだけでは通常listenerや旧workerを止めない。D12の照合後の窓も未解決 | 停止状態を保証済みとせず、処置をHOLDして運用責任者へ引き継ぐ。本手順からprocess停止や設定変更を自動実行しない |

別publicationについての人手保留は、既存インシデント記録を介した手続上の抑止に留まる。通常listener・別入口・管理者操作を技術的に遮断する機構を実装したわけではなく、同一論理通知の横断的な二重送信防止は未達とする。providerのidempotency保持条件が未確定の状態で、keyが同じなら安全と判断しない。

### 9.3 今回の結論と残課題（§10でOwner承認）

**現在の人手HOLD対象は、未閉鎖permitがあればそのまま保持する。歴史closeを新設せず、別publicationの関連付けも既存運用記録で行う。** permitがないHOLDには、抑止用permitを新たに発行する仕組みを足さない。closeやpermitの有無を運用記録上の調査終了と同じ状態として扱わない。

未閉鎖記録の蓄積、保持/パージ、実際の運用記録への到達性はOPEN。記録の整理や容量確保を理由にclose/削除を許可しない一方、全記録を永久保存する実装を採用したとも扱わない。歴史対象の閉鎖が実際に必要になった時に、閉鎖の意味・責任・証拠・次発行への影響を限定して再検討する。

本案でS1-04のD11/D12全体、別publication横断の技術的抑止、停止維持/fencing、E08の残枝を解決済みとはしない。既存の比較条件・index・Audit/消費記録を維持し、新table・schema・close API・追加テストは作らない。

## 10. 未閉鎖permit・別publicationの扱いのOwner承認（2026-10-16）

Ownerは§9を確認し、承認した。これを**S1-04初回整理の結論と制限のOWNER APPROVED**として記録する。

未解決のpermitは存在する場合に未閉鎖のまま保持し、歴史closeは新設しない。別publicationが同じ論理通知に見える場合は既存運用記録で関連付け、人の照合とHOLDを維持する。permitのないHOLDに抑止用permitを新設しない。

同一環境・publicationの一意制約を、別publication横断や通常listenerの技術的な送信抑止へ拡張して解釈しない。D11/D12全体、停止維持/fencing、E08の残枝、保持/パージと実運用体制のOPENは維持する。実装・追加検証・実運用操作・成果commit・remoteの開始承認は含まない。
