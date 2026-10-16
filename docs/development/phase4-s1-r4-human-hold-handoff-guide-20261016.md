# S1 R4：受理済みFAILED一件の人手HOLD引継ぎ案（2026-10-16）

状態：**§1〜6はOWNER確認済み。既存記録点検§7の結論・制限はOWNER APPROVED（§8）**。方針の根拠は[R4レビュー§10](phase4-s1-r4-failed-resolution-purpose-and-boundary-review-20261016.md#10-aの設計候補留保人手hold維持のowner承認2026-10-16)。Aは設計候補に留め、人の照合・担当付きHOLDを維持する。これは新たな復旧操作の許可や、運用配備済みの認定ではない。

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
