# S1：既存Referenceを保護する責務・正式接続・初回実装範囲の検討（2026-10-06）

**状態:** DRAFT / 慎重な検討の入力。Ownerから、既存Referenceの機能・品質を阻害せず、安全側へ倒した機能反映・拡張とする指示を受領。以下は推奨候補であり、正式配置・Tier・実装開始の採用票ではない。
**baseline:** `docs/daily-development-workflow` / `f5e2672`。確認時clean。A18件・B31件・C38件の局所結果はすべてCOMPLETE / OWNER APPROVED、C同profile回帰89件PASS。
**位置:** Reference-owned `notification`候補の設計、今回の変更はdocsのみ。Framework／Reference code・POM・migration・起動設定は変更しない。
**正本入力:** [変更一覧CH-01〜08・ST-C以降](phase4-s1-minimum-change-and-start-scope-draft-20261005.md)、[責務・認可・Audit・mode案](phase4-s1-responsibility-authorization-audit-mode-draft-20261005.md)、[A／B／C受入後の候補継続§7](phase4-s1-jpa-candidate-continuation-after-a-20261006.md#7-c受入後の継続2026-10-06)、[C受入§6](../architecture/validation/phase4-s1-additional-c-web-mode-20261006.md#6-owner受入承認2026-10-06)。既存CP／Gateとblocking reviewを維持する。

## 1. 三つの軸に共通する受入前提

Ownerは「正式Referenceへの配置」について、現在の機能・品質を阻害せず、安全側へ倒した反映・拡張となるよう指示した。責務、接続、初回範囲を連続する判断として扱い、既存Referenceを保護する受入条件を各段階へ通す。

通常起動でidentity／master／expenseの既存経路を維持し、追加機能の無効・未設定・設定不正を区別する。無効時は通常起動を維持し、明示的に追加modeを要求して必要条件が欠ける場合は、その起動・操作を拒否する。起動失敗を黙って通常modeへ戻して実行しない。

追加機能の障害をexpense承認の既存transactionへ接続する時点では、何をrollbackするかを個別に設計する。既存同期veto・Business Audit・認可・承認者scopeの意味を維持し、「安全側」を単に通常業務も常時停止させる設計とはしない。障害の隔離を理由に必要な記録失敗を握り潰すこともしない。

| 守る既存境界 | 新機能側の条件 | 必要な受入証拠 |
|---|---|---|
| identity認証・管理、Session、CSRF、default deny | 権限code／認証主体はPublic契約を利用。既存chainのmatcher・orderへ広範囲な変更を入れない | 既存Security test、追加routeの能力／scope／CSRF、未知routeの拒否、filter・chain一覧 |
| masterの有効値確認とexpenseの申請・承認・取消 | 他module内部／Repository／Entityへ依存せず、既存契約の意味を維持 | master／expenseの実DB・method security・MVC／REST／HTMX回帰、同期veto・Audit rollback |
| 通常起動・終了、既存DBとschema履歴 | 追加Bean／Entity／Repository／scheduler／runner登録とmigration適用をそれぞれ制御 | 追加無効での新旧DB起動、Bean・JPA metamodel・route・scheduled task一覧、起動／終了時DB差分 |
| package済みReferenceのDeveloper Journey | profile未指定でも既存操作を維持。依存・起動手順の必要差分を明示 | 固定JARで既存critical journey、設定不足時診断、追加modeの起動・終了、既存手順との整合 |

## 2. 現行sourceから分かった配置上の注意

| source | 確認事項 | 設計への帰結 |
|---|---|---|
| [ReferenceApplication](../../koiki-reference-app/src/main/java/org/koikifw/reference/ReferenceApplication.java) | `org.koikifw.reference`を起点とする`@SpringBootApplication` | 新規component／Configurationは通常scanへ入り得る。mainを追加するだけでmode分離成立としない |
| [ExpensePersistenceConfiguration](../../koiki-reference-app/src/main/java/org/koikifw/reference/expense/adapter/outbound/persistence/ExpensePersistenceConfiguration.java)、[MasterPersistenceConfiguration](../../koiki-reference-app/src/main/java/org/koikifw/reference/master/adapter/outbound/persistence/MasterPersistenceConfiguration.java)、Identity auto-configuration | 業務とFramework IdentityがそれぞれEntity登録を行う | 追加Entityの登録は明示条件に拘束し、既存Entity登録を置換しない。Repository登録も別に確認 |
| [application.properties](../../koiki-reference-app/src/main/resources/application.properties) | `ddl-auto=validate`／OSIV false、`db/migration/kkref`／`kkref_flyway_history`、既存local認証設定 | 全体のFlywayを無効化してCの検証構成へ合わせない。新Entityの無条件登録や通常locationへのDDL追加は無効modeでも影響する |
| [ReferenceSecurityConfiguration](../../koiki-reference-app/src/main/java/org/koikifw/reference/identity/configuration/ReferenceSecurityConfiguration.java) | 既存path限定chain、Session logout連携、実CompromisedPasswordChecker | Cのtest chain／Checker不在をそのまま正式Webへ移植しない。既存設定との共存・外部照会条件を正式回帰で確認 |
| [ReferenceArchitectureTest](../../koiki-reference-app/src/test/java/org/koikifw/reference/ReferenceArchitectureTest.java)、[Reference POM](../../koiki-reference-app/pom.xml) | 現行business rulesを適用。Modulith JDBC runtimeの明示依存はない | Level 2 runtime／Rule選択はCH-07等の独立review対象。Rule拒否の除外で開始条件を迂回しない |

機能の既定無効化だけでclasspathのauto-configuration、Entity登録、migration実行が止まるとは判断しない。同一JAR内での分離を第一候補として実効構成を検証し、通常起動を保護できない場合は配置方式へ戻す。別artifactや新しい復旧Starterを無承認で追加しない。

## 3. 許可・消費モデルから接続・受入へ通す責務

狭いRICH／JPA共有モデルを継続候補とする。2種類記録にまたがる一度だけの消費と未解決対象の次許可禁止、複数Use Caseで使う可否規則をTier判断の入力にする。型名・method signature・field／物理名・aggregate境界は正式reviewで固定する。

| 判断・操作 | Domain | Application | Adapter／Configuration | 正式接続で必要な検証 |
|---|---|---|---|---|
| 許可発行 | target／actor／期限の不変値と生成条件 | 本人・現在ISSUE能力・対象scope確認、保存＋Business Audit | Web principal変換、JPA INSERT・未閉鎖対象unique制約 | principal由来actor、scope外拒否、次許可競合、Audit失敗rollback |
| 消費 | 未閉鎖・期限内・対象snapshot一致、消費記録の不変性 | 現在権限・停止／運用証拠を確認、lock下で既存消費確認、消費＋Auditをcommit | Spring transactionに接続した保存、row lock・version・一意INSERT、publication版依存読取 | 実SQL・列権限・競合、現在権限／期限／attempt差異、commit不明保全 |
| 対象再送 | 許可の拘束値を保持。registry／providerを呼ばない | commit後再確認、必要証拠不足はHOLD、厳密対象のみ再送要求 | Public再送API、実proxy／executor相関、外部I/O | 対象1件・他row不変、失効時probe0／消費保持、I/O結果と試行の相関 |
| 観察 | 観察で状態を変更しない | READ能力・scope先行のQuery、最終record返却 | 更新Beanなし、read-only query、観察role | 存在情報非露出、permit／consumption／publication更新拒否、起動／終了副作用なし |
| 確認終了 | 必要な確認を満たした時だけ終了記録。不明を成功と同一視しない | CLOSE能力・scope、人の証拠突合、lockとBusiness Audit | 証拠取得／照合、閉鎖列だけUPDATE | 未停止／証拠欠落・不一致は拒否、競合時保留、Audit失敗rollback |

DomainへIdentityQuery／Recorder／EntityManager／registryを持たせない。Domain RepositoryはDomain契約として置き、複雑な現在値照会・運用証拠・再送のPortはApplication所有とする。必要な実行単位でだけ契約を作り、将来用のPort／Domain Serviceを先行生成しない。

消費は独立したworkflow集約を増やす目的ではなく、許可に対応するappend-only記録とする。メモリ上の可否判定だけで一度性を保証せず、lock／一意制約／実transactionで担保する。consumerは他moduleのDomain Modelを参照せず、モデル・EntityをWebへ露出しない。

## 4. 正式配置・認可・Audit・mode・migrationの接続条件

`koiki-reference-app`内の`notification`業務packageを配置候補とし、`domain`／`application`／`adapter`／`configuration`へ分ける。identity／master／expenseのOwnershipは維持する。既存expense event接続は後段のCH-05で判断する。

| 接続面 | 安全側の候補 | 開始前に閉じる項目 |
|---|---|---|
| 認可 | 発行／閲覧／実行／確認終了を分ける。Web actorは実principal、不変user ID。non-Webは許可発行者とworker世代を区別 | 正式permission・scope所有元、Identity障害時拒否、失効の基準時点。固定fixture user／codeを採用値にしない |
| Audit | 発行／消費／閉鎖は同Spring transactionで実Business Recorder。拒否はSecurity Recorder、記録障害でも拒否維持 | actor／action／resource／分類、flush／rollback、REQUIRES_NEW用poolとlock順序。未知結果は運用証拠と保留で説明 |
| 起動 | 通常起動は追加機能無効を既定候補。観察・更新Web・復旧を明示構成し、必要Beanだけ登録 | component／Entity／Repository／auto-configurationの条件、mode排他、不正設定の拒否、通常依存の非Web混入。property／entry pointは未固定 |
| 再送 | 標準registryの版依存SQLはAdapter内、Public再送API経由。自動全件再送・status変更・通常event投入を復旧から外す | 実registry／proxy、scheduler／runner不在、必要列権限、Modulith runtimeとCH-07独立review。CのMoments停止は正式構成でも再確認 |
| migration | V1〜V3とFramework二階層履歴を維持、追加だけで行う。初回は追加mode用の明示location候補を既存kkref履歴と整合 | version／location衝突、適用者、fresh／upgrade、既存row不変、無効時に未適用DBで起動できるEntity登録条件。DDL／role／grantのOwnershipを分ける |
| DB role | 用途別NOINHERIT／owner非継承、必要列だけ更新。認証・Session・拒否AuditのDMLと業務更新を区別 | 正式schema／権限台帳、接続credential、Queryのscope先行、無権限runtimeの拒否。Framework tableのgrantをReference migrationへ無断混在させない |

追加migration適用後に機能を無効化してもschemaは元へ戻らない。既存DBを破壊して戻すdown migrationを安全策としない。追加schemaの保持、未解決許可・消費の保持、旧／新JAR起動の対応範囲をupgrade／再開手順に明記する。旧JAR互換は実証するまで保証しない。

## 5. 最初の実装範囲候補と段階的な受入

最初は**許可・消費の保存／認可／Audit基盤を、通常起動から登録を外した構成で実装する**候補を推奨する。正式moduleと共有モデルの配置を決め、実DB integration testから明示構成を起動して確認する。DB seed／障害注入はtestへ限定する。

初回対象はモデル生成・可否規則、必要なApplication操作と保存契約、JPA Adapter、追加modeの構成条件、review済み追加migration／grant手順、対応test。既存業務画面からの入口、expense承認event、新しい通知listener／sender／registry runtime依存・復旧runnerは後段へ分ける。実行Use Caseは必要な証拠を確認できない段階で送信機能を持たせない。

| 段階候補 | 3軸をつなぐ成果物 | 受入・次へ進む条件 |
|---|---|---|
| 初回：保存・認可・Audit基盤 | §3のモデル規則／Application操作→§4の条件付きJPA登録・migration→実DB保存・拒否経路 | A／Bの必須枝を正式mapping／transactionで再確認。追加無効の通常起動を未適用／適用済みDBで比較、既存Reference回帰PASS。送信・業務連携なし |
| 次段：限定Web／観察 | 同じApplicationへ本人認証・能力／scope・CSRF付き入口、更新Beanのない観察構成 | C Web枝を正式Security／Session／Checker共存で再確認。新旧route・既存login／logout／MVC／REST／HTMX、実browser／log／Audit／DB突合 |
| 後段：限定復旧と通知接続 | 保存commit→現在値／運用証拠→実registry限定再送。通常通知はCH-05の別範囲 | Level／Rule／依存review、worker停止／provider相関・unknown保留と再起動、通常業務のtransaction契約・既存critical journeyが成立 |

これはST-C等の内部順序候補であり、新Gateを作らない。正式開始票に対象path・Tier／契約・DDL／Security／依存のblocking review、J1／J8・OR・CP-F0／P4-F整合、command・資源／時間上限・停止点・Evidence先をまとめてから作成・実行へ進む。局所検証の予算／commandを正式Referenceへそのまま流用しない。

## 6. 検証・停止点・次の具体化

実装前に固定したcommit／JAR／実効POM／DB履歴を基準に比較する。既存ReferenceのArchitecture、Identity境界・Security、master、expenseの実DB・Audit rollback・認可・MVC／REST／HTMX testを回帰対象に選び、正式開始票でclass／commandを確定する。通常構成と追加有効構成の双方で確認し、package済みReferenceのPhase 3 critical journeyも受入へ結ぶ。required checks 8件と既存workflowを維持し、remote実行・変更は個別操作の承認に従う。

機能PASSだけでなく、追加無効時に登録されないBean／Entity／Repository／route、動かないrunner／scheduler、追加migrationが未適用でも変わらない通常起動、既存業務row・Audit・publicationへの意図しない更新なしを比較する。起動／終了log、DB snapshot、設定資源と観測値をEvidenceへ残す。今回のsource調査を正式構成の実行PASSとして扱わない。

既存回帰失敗、想定外の登録／DDL／通信／再送、権限拡大が必要、追加無効でも既存起動不能、UNKNOWN保持不能を検出した場合は、その段階を受入せず設計・必要差分へ戻す。正例だけを動かすために既存認可を弱めたりRuleを除外したりしない。

次の文書作業は、初回基盤の具体型／aggregate・Application操作、mode登録条件とmigration適用経路、既存回帰のclass／command／上限を一つの正式開始票へ具体化すること。provider／旧worker停止の真正性、失効窓、正式TTL／scopeはどの段階で何を証拠に閉じるか明記し、成立前は該当する送信／確認終了を許可しない。

## 7. 新ブランチと正式開始票（2026-10-06）

Ownerは別ブランチで継続する進め方を了承し、`f5e2672`から`feature/phase4-s1-reference-foundation`を作成・switchした。4文書差分をhash不変で引継いだ後、正式開始票へまとめる作業への進行を指示した。[正式開始票案](phase4-s1-reference-foundation-formal-start-review-20261006.md)に初回の具体型・登録方式・migration候補・55 invocation／既存25 class回帰・critical journey・command／上限・判定欄をまとめた。

本書§5の段階順序は候補として維持する。正式開始経路J1／J8、具体scope／TTL／Audit／DDL採用事項と資源・実行上限の判定は未成立。今回の指示は文書化の範囲であり、正式実装開始を承認済みとしない。
