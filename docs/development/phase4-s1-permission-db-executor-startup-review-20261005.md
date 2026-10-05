# S1最小方式：許可DB保護・executor相関・結果確認起動条件（2026-10-05）

> **現在の参照先（2026-10-05）:** 本書は比較・source調査時点の記録。現在のDB保護・相関・起動の局所候補と初回作成／実行は承認済み方式票へ追跡する。正式実装・実動PASSを認定しない。 [最新引継ぎ](phase4-s1-local-verification-start-handoff-20261005.md)／[承認済み方式票](phase4-s1-method-ballot-and-local-verification-contract-20261005.md)。

**状態:** DRAFT / source照合と実装前review入力。具体方式・DB role / DDL・mode / Bean変更・Tier・依存・開始は未判定。
**Ownership:** Reference-owned実行許可・復旧 / 確認構成、Tooling-owned故障注入とEvidence。Framework Public API / 汎用復旧機能の追加を前提にしない。
**確認:** `docs/daily-development-workflow` / `e75a70a`と未commit文書。Repository source・既存Evidence・ローカルModulith 2.1.1 JARのread-only照合。DB権限・起動・相関の実動検証は未実施。
**入力:** [最小方式・許可と試行](phase4-s1-minimum-permission-and-attempt-review-20261005.md)、[最小構成方針](phase4-s1-minimum-demonstration-scope-review-20261005.md)、[RV / PF](phase4-s1-preimplementation-review-preflight-estimate-draft-20261005.md)。

## 1. 今回のsource確認と結論

| Source | 確認した事実 | 設計へ反映すること |
|---|---|---|
| [KOIKI Observability構成](../../koiki-starters/koiki-starter-observability/src/main/java/org/koikifw/starter/observability/internal/KoikiObservabilityAutoConfiguration.java) | 独立したContextRegistryに`requestId`のSlf4j accessorだけを登録するTaskDecoratorを作る | operation / publication / traceがこのBeanだけで伝わるとは扱わない。Application側で必要なcontextを明示して共存させる |
| [Phase 1b CP5 Evidence](../architecture/validation/phase1b-cp5-observability.md) | requestId伝播とCustomer追加decoratorの共存を検証した時点記録がある | 構造を利用する。ただし今回のReference executor / tracer構成のPASSへ転記しない |
| [Tooling correlation executor](../../build-support/phase4-level2-verification/src/main/java/org/koikifw/buildsupport/phase4/CorrelationTaskExecutorProbe.java) | fixtureはMDC全体をcopy / restoreする | そのまま正式移植せず必要項目のみ伝播。markerの存在を許可証明にしない |
| Modulith ApplicationModuleListener 2.1.1 | ローカルJARの公開annotationにはid / condition等があるがexecutor qualifier属性はない | 任意のexecutor名をannotationへ渡せると推定しない。実際のasync executor選択を確認する |
| [Data Flyway構成](../../koiki-starters/koiki-starter-data/src/main/java/org/koikifw/starter/data/internal/KoikiDataFlywayAutoConfiguration.java) | Primary DataSourceを使ってKOIKI→Applicationをmigrateする | Boot側Flywayの別user設定だけでKOIKI migrationも別credentialになると推定しない |
| [Reference main](../../koiki-reference-app/src/main/java/org/koikifw/reference/ReferenceApplication.java) / [設定](../../koiki-reference-app/src/main/resources/application.properties) | mainは通常のroot scan起動。JPA validate、Flyway kkref指定。専用modeは未実装 | profile名追加だけで復旧 / 確認Beanを分離できたとはしない。modeとscan / Bean境界を設計する |

最小候補は1種類の許可recordを維持する。その保護に必要な制約、実行相関、送信不能な確認modeは独立した条件として閉じる。新しい常設管理台帳サービスや自動復旧基盤へ拡大しない。

## 2. 許可DB保護の役割と権限案

role名は論理名。PostgreSQL 17の[権限説明](https://www.postgresql.org/docs/17/ddl-priv.html)と[GRANT](https://www.postgresql.org/docs/17/sql-grant.html)で列単位INSERT / UPDATEを確認した。table-level権限、owner / superuser・継承roleによる権限も調べる。列GRANTだけを見て改変不可と判定しない。

| 論理role / 用途 | 許可recordに認める操作候補 | 認めない操作 |
|---|---|---|
| migration / owner | review済みDDL、制約 / grant、fresh / upgrade | credentialをruntimeへ配布しない。runtimeがownerへSET ROLEできない条件を確認 |
| 認証済み受付・確認 | 必要SELECT、発行項目だけINSERT、確認終了項目だけUPDATE | 消費情報の偽造 / 消去、対象 / 発行者の更新、DELETE / TRUNCATE、DDL |
| 復旧process | 必要SELECT、消費事実の列だけUPDATE | 発行・対象 / 発行者 / 理由 / 期限変更、確認終了による次許可解放、DELETE / TRUNCATE、DDL |
| Tooling閲覧 | 必要な安全項目 / viewのSELECT | 発行・消費・確認終了更新。故障注入用管理権限は通常の閲覧credentialと分離 |

受付roleにINSERTできることは、SQL clientによる本人認証を意味しない。credentialを持つ信頼されたReference経路が、既存Web認証・permission / scopeで発行者を確定する。DB管理者や侵害された発行processからの許可偽造まで防ぐ暗号署名方式は初回範囲に加えない。保証範囲を運用手順に明記する。

### 2.1 単発消費の保証を二段に分ける

**Applicationの実行契約:** 未消費・有効期限・未確認終了・対象snapshot等を条件に1件だけ消費し、operation / worker情報と必要Auditを同じtransactionでcommitする。更新0件 / 競合 / commit結果不明なら送信しない。競合2workerで一方だけが実行準備を成立させることを実DBで検証する。

**DB直接改変への保護:** 消費列UPDATEのgrantは「消費済みをNULLへ戻す」ことも自動で禁止するわけではない。CHECKも通常、行の現在の値だけでは旧値からの巻戻しを判定できない。したがって列権限だけで「直接SQLからも再消費不能」と説明しない。

1 record案を保つ場合、消費事実・確認終了事実の一度だけの追記をDBで保護する狭いtrigger等を比較候補とする。発行時は消費 / 確認終了を空にし、一度記録したoperation / 消費時刻を変更・消去不可、確認終了も巻戻し不可とする。これはReference追加migrationのreview対象で、今回trigger SQLを追加しない。DBは人のprovider判断の正しさまで判定しない。

代案は消費事実を別のappend-only recordへ一意挿入する方式。tableが1つ増えるが、INSERT-only権限で消費取消しを防げる可能性がある。triggerの複雑さ / JPA適合と比較し、**table数1を守るために過剰な仕組みを作らない**。4種管理台帳へ戻す提案ではない。どちらも未採用であり、直接DB巻戻し拒否の受入条件を保ったまま選ぶ。

### 2.2 ORMと周辺tableへの権限

JPAがimmutable列までUPDATEへ含める場合、列権限と衝突する。発行項目を更新対象から除外し、消費 / 確認終了は限定したRepository更新にする候補。操作結果件数・version・persistence contextの鮮度をreviewする。SQL詳細をUse CaseやControllerへ置かない。

許可tableだけでは起動しない。IdentityQueryの正当な読取、Audit appendとUUID / sequence、通知logの必要更新、publicationのModulith管理更新、認証 / Sessionの必要更新、schema USAGE等を用途ごとに棚卸しする。publication権限はregistry動作に必要な範囲を実測し、SQL直接status更新を復旧操作として提供しない。

migrationは専用credentialで事前実施し、runtime起動でmigrateしない候補を優先する。Bootの`spring.flyway.enabled=false`とKOIKIの`koiki.data.flyway.enabled=false`を併せて実効確認し、JPA validateは維持する。Boot propertyは[公式一覧](https://docs.spring.io/spring-boot/appendix/application-properties/index.html)を確認したが、対象Boot 4.1.1でのBean条件と既存二階層手順を検証してから採用する。履歴version / checksumをpreflightで照合し、必要migration未適用なら起動を止める。Framework契約のmigration順序を無効化してよいという承認ではない。

## 3. 実executorへoperation相関を結ぶ最小候補

復旧processは1操作だけ扱う。許可消費commit後に、operation UUID・許可UUID・publication UUID・event型 / ID・listener識別・worker世代を持つ不変のApplication-owned実行contextを作り、標準再送APIを呼ぶ範囲に設定する。認証credentialやSecurityContextはcontextへ含めない。

| 境界 | 必要条件・構成候補 | 検証する証拠 |
|---|---|---|
| 再送呼出し→task投入 | 実際に選ばれるasync executorでcontext snapshotをcapture | executor Bean / task投入thread・実行thread、同じoperationの記録。Common Pool等の別経路があれば列挙 |
| task→listener | Application追加TaskDecoratorで不変contextを設定・finallyで元へrestore / clear。既存requestId decoratorと標準trace伝播を合成 | 再利用threadで旧operation非残留、相関なしtaskへ非漏えい、例外 / cancellation時も掃除 |
| listener→通知Use Case | 復旧modeではcontextの対象event / listenerと実際の受信を照合してからproviderへ | 不一致・context欠落は送信前拒否。通常modeは正常な通常通知として動き、復旧contextを必須にしない |
| 通知Use Case→log / Audit / provider | operationとpublicationを記録に結ぶ。provider冪等keyは元の論理通知keyを維持 | 同keyの受理と当該operation、通知結果・対象publicationの前後を照合 |

MDCは上記contextをlogへ表示する補助に限定する。MDCのoperation値だけで許可済みと判定しない。traceは標準tracerの実contextを伝播し、復旧ごとに新しいtraceを作る。KOIKIの独立registryへ任意値を登録しただけで他のtracer accessorまで利用できるとはしない。構成の根拠は[SpringのTaskDecorator説明](https://docs.spring.io/spring-framework/reference/integration/scheduling.html)と既存CP5で、今回の実相関は未検証。

Modulith内部multicasterは調査対象としてのみ参照し、Referenceの正式依存先にしない。公開APIのfilter→実listenerまでcontextが届く経路と、listener ID / publicationの一意対応は実効構成で確認する。現在は**成立候補でありPASSではない**。届かない境界が判明した場合はBLOCKED / REWORKとして最小方式を見直し、Framework内部aspectや独自event envelopeを即追加しない。

## 4. 結果確認時の起動条件案

同一固定JARの実行目的を下表の論理modeへ分ける。名称・property / profileは未固定。確認modeもWebを使うが、通常Web業務を一緒に再開しない。送信禁止は画面を隠すことやregistry対象設定だけでは成立しない。

| 論理mode | 有効にするもの | 起動させないもの・DB権限 |
|---|---|---|
| 通常 | 既存Web業務・認証・通常通知 | 復旧runnerは無効。既存業務の権限 / transaction契約を維持 |
| 許可発行 / 結果確認Web | 既存認証・Session / CSRF、IdentityQuery、限定許可 / 確認経路、Audit、証拠読取 | expense更新経路、通知event listener、provider送信Adapter、再送runner、stale / auto再送 / schedulerは無効。許可発行 / 確認roleを使用 |
| 単発復旧non-Web | 1許可のrunner、限定notification listener / Use Case / provider Adapter、必要executor・読取 / Audit | Web server、通常業務更新、許可発行 / 確認終了、起動時自動再公開 / schedulerは無効。復旧roleを使用 |

Reference root scanは現状全体を対象とするため、modeによるComponent / Configuration / Bean境界を明示する必要がある。property値が相矛盾、mode不明、要求ID欠落ならstartupで拒否する。Security / Identityは正当な認証用Beanを維持し、単に全面excludeして確認routeを無認証へ変えない。Bearer構成などopt-in Web設定のmode適用も調べる。

**確認の順序:** 復旧processの終了・残listener停止とproviderを確認 → 確認専用Webを起動 → Ownerが対象snapshot・受理・通知log・Audit・operationを照合 → 不明なら保留、解決済みなら認証済み経路で確認終了とAuditをcommit → 確認Webを停止 → 通常再開可否を判断。通常再開は「結果表示のための起動」と別の操作として記録する。

確認modeのstartup / HTTP操作だけでprovider受理件数・publication状態 / attemptsが変わらないことを実証する。必要な認証 / Session / Audit書込は許容するため、DB完全read-only modeではない。送信Bean不在・再送APIを呼ぶroute不在と、DBのpublication更新権限なしを確認して多層で誤起動を防ぐ。暗黙のFlyway・schema初期化も無効条件を検証する。

## 5. 実装前に閉じる条件と検証枝

**最新の検証順・権限:** [Tier・保存／権限・V0〜7](phase4-s1-minimum-tier-storage-verification-review-20261005.md)に2種類record案とrow lockに必要な技術version列UPDATE権限を整理した。本資料の1 record消費UPDATE案と、2種類案の消費INSERT-onlyを混同しない。Tier・DDL / 権限と全検証は未判定 / 未実施。

**後続照合:** [成立性確認](phase4-s1-minimum-boundary-feasibility-review-20261005.md)で許可＋append-only消費の2種類を優先比較し、標準再送のpredicate / options経路、後者のfilter前batch制限、Reference Web SecurityのServlet条件不足を確認した。source上の根拠であり、下記条件のruntime PASSではない。

| ID | reviewの出口 | case / 必要負例 |
|---|---|---|
| DB-1 | role・schema / table / 列 / sequence権限表とowner継承なし、runtime credentialとmigrationの分離 | E12 / E16：対象 / actor変更、発行偽造、確認終了、DELETE / DDLを役割別に拒否 |
| DB-2 | 単発消費、巻戻し不可の方式選択とJPA適合、未解決対象の別許可制約 | E13 / E16：同許可2worker、消費NULL戻し / operation変更、commit不明・crash後再消費拒否 |
| EX-1 | 標準再送APIから実executor / listenerへのcontext経路・一意対応 | E13 / E20：相関欠落 / 別対象 / 旧thread残留、予想外の複数attemptを拒否 / 保留 |
| EX-2 | requestIdと実traceの共存、log / Audit / provider相関と終了時採取 | E16 / E20 / E21：例外・sink不達・短命process・provider結果不明 |
| ST-1 | 目的別Bean / scanと起動拒否、確認modeで送信・auto再送・migrationなし | E12 / E13 / E16：誤mode・無認証確認・通常Bean混入、startup前後とHTTP後のprovider / publication不変 |

この資料は検証条件を具体化したもので全項目未実演。許可recordを増やさない案でも、DB巻戻し保護とmode境界の実装量は残る。次はDB-2の狭い保護方式とEX-1 / ST-1の成立性を先に確認し、必要最小構成が揃ってからTier・DDL / 依存・再見積へ戻す。今回code・POM・migration・権限設定・環境起動・remoteは変更していない。
