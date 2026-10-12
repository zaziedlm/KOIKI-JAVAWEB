# ADR-051：Reference notification正常非同期経路の限定採用

**状態：ACCEPTED DESIGN / R1 LIMITED IMPLEMENTATION COMPLETE / OWNER ACCEPTED（2026-10-13）。** Architecture Owner：Shuichi Kataoka。[R1開始票§8](../../development/phase4-s1-r1-minimal-start-owner-review-20261012.md#8-owner承認反映と文書source固定の対象2026-10-12)で2026-10-12に条件付き限定開始を承認し、正本文書commit・新R1 branchでのclean source固定・preflight成立後に作成／検証した。

[結果文書§13〜15](../validation/phase4-s1-r1-implementation-and-early-verification-20261012.md#15-r1限定結果のowner受入承認2026-10-13)に、通常root・別固定通常／async artifact・新規9件・既存Critical Journey1件の限定成立、機能説明とOwner受入承認を記録する。ローカルstubの受理とpublication completionまでの選択正常経路の受入であり、実provider配信・停止／再送／復旧・R2〜R5・全DoD・正式受渡し・Phase 4全体へ拡張しない。成果固定local commitとremote操作は別判断を維持する。

## 背景と理由

R0のmodule単位Level選択・Rule 28／29・旧API互換性・必要最小限の検証はOwner受入済み。静的規約だけでは非同期の保存・受理・completionを証明できないため、Referenceの既存expense承認から正常通知を実証する。局所的なRule違反除外を採らず、ADR-050の正式Level選択APIを使う。

業務expenseはRICH／JPA SHARED、notificationの既存permitモデルも維持する。publicationは業務モデルから独立したlibrary storeとしてJDBCを選び、同一DataSource／発行元transactionへの参加を確認する。Framework API／Starter／BOM／Parent／Rulesへのruntime変更を行わない。

## 採用判断

1. Reference compileのevents APIと、専用Maven profile `s1-reference-async`内のJDBC starterを採用する。versionは既存BOM管理値`2.1.1`を維持し、端末artifact／metadataを照合する。BOM管理をruntime互換性保証へ読み替えない。
2. 起動設定`koiki.reference.notification.async.enabled=false`を既定とし、profileと起動設定の二段階選択で有効化する。通常起動のevent発行・listener／executor・registry／追加DDL・送信を抑止する。既存設定・除外と合成し、Security／同期vetoを保護する。
3. expense-owned値eventをapproveだけから発行し、notificationのEvent Adapter→Application→provider Port／Outbound HTTP Adapterへ接続する。Entity・他module内部を共有しない。
4. Reference-owned `kkref_notification_async` schemaと追加V5 migrationでpublicationを管理する。既存V1〜V4は変更せず、V4は自動選択しない。UPDATE完了、自動DDL／再起動時再公開を無効、registry対象は標準module listener。停止復旧・purgeは後段判断とする。
5. 通知keyはexpense ID／承認後確定version／applicant通知種別から固定生成し、同keyのcanonical payloadを保持する。process／attempt／publication／trace IDをkeyへ混ぜない。
6. 実providerではなくTooling-ownedのloopback stubで独立受理を照合する。同key同payloadは受理1件、異payloadはconflict。受理台帳・故障switchを配布JARへ入れない。

## 検証と保証境界

本ADRは[R1開始票](../../development/phase4-s1-r1-minimal-start-owner-review-20261012.md)の9新規候補・既存影響回帰／Critical Journey・最終通常rootと資源／70分／停止・cleanupに限定する。R0成立結果と専用92件の通常build分離を保持し、S1／B2全件再検証や監視一般化へ拡張しない。

R1は正常枝・通常保護だけを実証する。publication保存失敗時のrollback、通知失敗時の承認保持はR2。停止／復旧とUNKNOWN照合はR3、permit consume／close・操作認可／AuditはR4、保持／purge／traceはR5。SecurityContext伝播やUUIDからの主体捏造を前提にしない。DoD・正式Reference Level 2受渡し・実配信・Phase 4全体は未達／未承認を維持する。

## 再判断条件

端末library契約不一致、V5適用後の未適用V4後付け、key／受取先変更、既存permit／Securityへの直接影響、観測・cleanup不成立、scope／資源／予算超過では停止する。必要な差分だけを開始票の訂正判断へ戻し、依存更新・out-of-order・無制限再送・監視網羅を自動追加しない。local commit／remote／結果受入は別判断。
