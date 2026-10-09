# Phase 4 S1 R0：Rule 28／29のLevel選択対応・限定改訂Ownerレビュー案（2026-10-09）

**状態：OWNER APPROVED / 文書source固定・preflight条件付きR0-B限定開始（2026-10-09、§9.2）。** RL-01〜06と正本整合を承認済み。文書commit・clean source固定／preflightは未成立で、code／検証は未開始。R1・Reference runtime開始は別判断。

**source：** `feature/phase4-s1-reference-foundation` / `fe93b5da76a85fd6a4c41409c725c35661fac007`。開始時は先行R0資料と目的対応表の文書差分2件だけ。これらを保全して本票と相互整合させる。local commit・remoteは別承認。

**入力：** [R0 Reference開始判断票](phase4-s1-r0-reference-async-start-review-20261009.md)、[現行Public API](../../koiki-archunit-rules/src/main/java/org/koikifw/archunit/KoikiArchitectureRules.java)、[現行Rule集合](../../koiki-archunit-rules/src/main/java/org/koikifw/archunit/BusinessModuleRuleSet.java)、[PL2候補test](../../build-support/phase4-level2-verification/src/test/java/org/koikifw/buildsupport/phase4/Rule28And29CandidateTest.java)、[P4-F提案§3](KOIKI-JavaWeb-FW_Phase4_P4-F限定開始Gate提案_v0.1.md)。

## 1 決める順序

| 段階 | 対象と出口 | 承認境界 |
|---|---|---|
| R0-A 規約契約の審査（承認済み） | 本票のAPI、module単位のLevel選択、Rule 28／29、互換性、検証・正本差分を採用 | §9.2で条件付きR0-B開始を承認。Reference非同期開始は未承認 |
| R0-B 規約の限定作成・検証 | 本票の個別開始承認、正本・承認文書source固定、clean／preflight成立後にFramework Rulesだけを改訂・検証 | §6〜7の有限対象。Reference runtime／listener／POM／SQLを先行しない |
| R0-C 規約結果の受入・source固定 | 互換性、肯定／負例、通常build、既存Reference保護をOwnerが受入。成果local commitは別判断 | Tooling候補PASSから正式規約PASSへ自動昇格しない。R1実装開始を兼ねない |
| R0-D Reference開始票の再確定 | 受入済みAPI・RuleをR1開始票へ反映し、dependency／migration／正常経路・予算を再審査 | 未成立ならR1待機。Rule 28の特定違反を除外する方式を採らない |
| R1以降 | Reference正常経路、失敗分離、停止復旧、認可／Audit、保持／traceの順に進む | 各開始・出口判断を維持。DoD・実provider・正式受渡し・Phase 4全体は別判断 |

先行R0票のReference限定違反例外案は**不採用方向へ訂正**する。この指示でFramework全体のLevel 2 runtime、S2共通基盤、Customer採用、配布／publishを開始しない。

## 2 Owner判断一覧

| ID | 採用した判断 | 実行条件／残条件 |
|---|---|---|
| RL-01 API | §3のmodule単位Map overloadとenumを採用。旧1引数APIを維持 | Public API追加はOwner明示承認後だけ。enum名・signatureを承認記録へ固定 |
| RL-02 Rule 28 | Level 0／1・未指定は現行拒否。指定Level 2では直接`ApplicationModuleListener`だけを許容 | raw／meta transactional listener、隠れた上書き・混用は拒否する |
| RL-03 Rule 29 | 新overloadに同期listener→outbound Adapterの直接依存制約を追加 | 間接外部副作用は検出範囲外と明記。既存Rule 1／38等を維持 |
| RL-04 互換性 | 旧signature・入力契約・既存Rule 28・診断を維持。新API選択時だけ追加制約 | 既存API利用者に移行を強制しない。module metadata／Tier／persistenceを変更しない |
| RL-05 正本差分 | §8を限定反映し、R0-Bだけの開始例外とADRを記録 | Reference開始制限の解除はR0-D後の個別判断 |
| RL-06 限定開始 | §6〜7、code／test最大24 file、文書16 file、40新規case候補、60分予算 | 文書sourceのcommit／clean／preflight後にだけ作成・検証。追加再実行0回、失敗時停止 |

Level 0／1の共通条件への同意は§9.1、後続のRL-01〜06と条件付きR0-B限定開始の承認は§9.2に記録する。実装は文書source固定・preflight成立後だけに限定する。local commitは§7.1の別承認／Owner操作を維持する。

## 3 Public APIとmodule単位のLevel選択案

追加場所は`koiki-archunit-rules`。`koiki-architecture-contract`の`KoikiModule`やFramework runtimeへLevel fieldを追加しない。

```java
// org.koikifw.archunit。Owner採用済みsignature、実装未了
public enum ModuleEventLevel { LEVEL_0, LEVEL_1, LEVEL_2 }

public static ArchRule businessModuleRules(
        String businessBasePackage,
        Map<String, ModuleEventLevel> moduleEventLevels);
```

Map keyはbusiness root直下の**package segment**（例：`notification`）。`KoikiModule.name`の表示名、FQCN、wildcard、sub-package、class／method allow-listを使わない。未指定moduleのevent制約はLevel 0／1の現行拒否と同じ。空Mapでも同じ拒否を維持する。enumのLevel 0／1はこのevent規約上同じ制約で、他のLevel機能の成立を表さない。

**Owner同意済みの意味整理（2026-10-09）：** `LEVEL_0`も同期eventを許容し、「同期eventが存在しない」ことは検査しない。設計上のLevel 0／1の意味を同一にする変更ではなく、今回のevent規約で共通の検査条件を採る。`LEVEL_2`だけが明示選択されたmoduleで非同期listenerの許容範囲を広げる。Level指定はruntimeの有効化や実装・配信成立の保証ではない。

将来Referenceが選択する例は次のとおり。これはR1で採用する候補であり、今回Reference testを変更しない。

```java
KoikiArchitectureRules.businessModuleRules(
        "org.koikifw.reference",
        Map.of("notification", ModuleEventLevel.LEVEL_2));
```

この場合もmaster／expense／identityは未指定のまま。expenseは値eventを発行する側であり、Level 2 listenerを許容するためにexpenseまで指定する必要はない。notification内の既存permit契約は変更しない。LevelをSpring profile、classpath、annotationの存在から自動推定しない。

### 3.1 入力・誤設定の拒否

- 旧root検証（null、不正Java 21 package、trim／wildcard補正なし）を維持する。
- Map／key／valueのnullはNPE、空key・不正segment・`.`やwildcard・予約語・余分な空白はIAEとし、黙って補正しない。
- Mapを防御copyし、rule生成後の元Map変更でLevelが変わらない。指定順に結果が左右されない。
- import対象に指定moduleのclassがなければ評価をFAILにする。綴り間違い・別rootの指定を「未指定だから安全」として見逃さない。API作成時にはimport集合がないため、これは評価時の設定違反として報告する。
- root必須class、Framework ownership、module境界・Tier／persistence・listener配置など既存規則を残す。MapにLevel 2を入れても、これらの違反を抑制しない。

## 4 Rule 28／29の契約案

### 4.1 Rule 28：許容する非同期listenerを明示

| listener宣言 | 旧API／未指定／LEVEL_0／LEVEL_1 | 新APIで当該moduleだけLEVEL_2 |
|---|---|---|
| 通常の同期`EventListener` | 現行どおり許容。他規則は適用 | 許容。他規則は適用 |
| 直接`ApplicationModuleListener` | 拒否 | 許容。ただし採用annotationの契約を上書きしない |
| raw `TransactionalEventListener` | 直接／metaとも拒否 | 直接／metaとも拒否 |
| 独自annotationで包んだ`ApplicationModuleListener` | 拒否 | 今回は拒否。独自合成annotationの許容は別review |
| `ApplicationModuleListener`にtransaction phase／propagation／asyncを上書きするannotationを混用 | 拒否 | 拒否。標準のcommit後・独立transaction・非同期契約を曖昧にしない |

標準`ApplicationModuleListener`自身のmeta annotationと、利用methodへ追加した上書きannotationを区別する。許容判定を「何らかのtransactional annotationがあるがmodule listenerなので全免除」にしない。method／class／独自metaの上書き検査範囲をfixtureで固定する。動的proxy／executor／transactionの実動作は静的規約だけで保証せず、R1／R2で実証する。

Level 2に限った制約変更はRule 28に閉じる。現行1引数APIのRule 28説明・違反detailは互換対象とし、必要な新診断は新API側へ分離する。

### 4.2 Rule 29：同期listenerの直接外部Adapter依存

新overloadの規則集合にRule 29を追加し、同期listenerを持つclassから当該business root内の`adapter.outbound`への直接依存を拒否する。直接／metaの同期listener検出と、transactional listenerとの区別をtestする。class単位の依存検査なので、listener methodから実際に呼ばれたかまでの証明とはしない。

既存Rule 1はinbound→outbound依存を既に拒否するため、Rule 29と重複する違反があり得る。Rule 1を外さず、Rule 29には同期eventの副作用分離という説明を持たせる。Rule 38の配置制約も残す。旧1引数APIにはRule 29を追加せず、既存診断・規則集合の互換性を保つ。

**検出の限界：** listener→Application Use Case→Port→外部Adapterという間接経路、任意のHTTP client・SDKによる通信、DB／file副作用、runtime条件による同期／非同期は、この直接依存Ruleだけでは判定しない。review checklistで副作用経路を追い、R1／R2の動作testで補う。`Rule 29 PASS＝外部副作用なし`とは記録しない。Level 2 listenerも直接outboundを呼べるようにはせず、既存Rule 1に従いApplication／Portを経由させる。

PL2候補testはこの方向を比較した材料であり、正式規則へのcopy・昇格根拠にはしない。正式APIの入力契約、module選択、annotation混用、互換性を別に検証する。

## 5 互換性の確認とReference安定への接続

現行[API contract test](../../koiki-archunit-rules/src/test/java/org/koikifw/archunit/KoikiArchitectureRulesContractTest.java)はpublic static methodが2件だけであることをassertしている。overload追加後は3件になるので、このassertは承認対象のAPI追加に合わせてsignatureの厳密な一覧検査へ更新する。単にmethod数検査を削除しない。内部helperの非公開性、旧signature、戻り値、null／不正root、Framework ownershipの契約を残す。

検証には旧APIの既存fixture／testをそのまま利用し、旧APIと新API空Map／LEVEL_0／LEVEL_1の許容・拒否の互換、module指定なしの他moduleの拒否を比較する。Framework mainにModulith／Spring runtime依存を追加しない。現行Rules POMにModulith APIとSpring test依存があるため、今回POM変更は不要とする案。

source互換だけでbinary互換の確認を済ませない。preflightで基点Rules JARのhash・公開descriptorを保存し、旧1引数APIを呼ぶ最小Consumer bytecodeを基点に対して1回compileして保全する。package確認でそのbytecodeを再compileせず改訂JARへ接続し、旧signatureのlinkageと既存fixture評価を1回確認する。temporary Consumerはrun所有、配布物に含めず、追加runtime・依存取得を伴わない。公開descriptor・旧Consumerと新JARのidentityをEvidenceへ記録する。

R0-BではReference code／POM／properties／SQL／Architecture testを不変とし、通常rootと既存package critical journeyで保護を確認する。R0-C受入後のR1で初めてReference Architecture testが新overloadを明示選択し、全違反0件を要求する。特定classのRule違反をfilterしない。

## 6 限定変更対象と40 case候補

| path | 作成・変更案 |
|---|---|
| `koiki-archunit-rules/src/main/java/org/koikifw/archunit/` | 新enum1件、選択検証helper最大1件、既存`KoikiArchitectureRules`／`BusinessModuleRuleSet`の追加overload・Rule 28選択・Rule 29。既存helper変更は必要な最大2件まで |
| 同module `src/test/` | 新規4 test class／40 case、fixture最大12 file、既存API contract test1 fileだけの明示signature差分 |
| `docs/`・`AGENTS.md` | §8、採用判断／ADR・Evidence・source導線。最大16 file |

code／test合計最大24 file。POM／依存、Architecture contract、他Framework module、Reference main／test、Tooling既存test／fixture、Root Reactor、CI／workflowを変更しない。範囲不足では上限内だからと対象を拡張せず停止する。

| 新規test class候補 | case候補数／内容 |
|---|---|
| `ModuleEventLevelSelectionContractTest` | 10：空Map、旧API互換、Level 0、Level 1、null Map、null key／value、key補正なし、防御copy、指定順、不存在module |
| `Rule28LevelSelectionTest` | 12：同期許容、未指定拒否、他module拒否、指定module許容、raw直接／meta拒否、module meta拒否、method／class上書き拒否、混用拒否、誤配置・既存規約維持 |
| `Rule29SynchronousSideEffectTest` | 8：直接同期依存拒否、meta同期拒否、Application経由許容、間接経路の検出限界、Rule 1重複維持、Level 2直接依存拒否、root外不干渉、旧API診断維持 |
| `LevelSelectedCompositeRegressionTest` | 10：旧signature、public型／method一覧、legacy診断、empty Mapの既存制約、Framework ownership、Tier、persistence、公開境界、module間依存、import必須条件 |

40は実行単位の上限候補で、複数のassertionを含むcaseとparameterized invocationを混同しない。実装前にIDとfixture対応を固定する。既存Rules testはannotation検索で67 method候補を確認したが、PASS件数は未測定。preflightで実行invocation台帳を確定し、新規40との合計は120件以内。超える場合は開始前に訂正判断へ戻す。

## 7 R0-B開始条件・検証予算・停止

### 7.1 source固定とpreflight

RL-01〜06の個別判断後、採用した§8正本差分・ADR・承認記録を文書だけで反映する。そのlocal commitはOwnerの別承認／操作による。clean文書sourceを固定してから、branch／差分・JDK／Maven・既存依存、test台帳・migration無変更、既存Java／container・port、空きmemory／disk・今回の所有台帳を確認する。

使用依存は既存versionだけとし、不足provider／agentの取得をこの票で包括許可しない。不足時は対象一覧と取得方法を示して停止する。Docker／m2権限は実行環境の手順に従い、拒否を迂回しない。

### 7.2 新規60分予算

旧B2／R1の枠を流用しない。検証・環境管理の累積wall timeを60分以内、追加再実行0回とする。各集合は予定した1回だけで、失敗後の再実行は別判断。cleanup5分を予約する。

| 区分 | 有限集合／上限 |
|---|---|
| preflight／classpath・source台帳 | 10分。開始条件未成立なら停止 |
| Rules module全test | 既存＋新規40、120 invocation以内、1回／15分。Docker不要 |
| 通常root `clean verify` | 1回／15分。基点349＋新規40＝389 invocation候補、旧予定無効92を維持、実行候補297。400以内、fresh XMLで集合照合 |
| PL2既存規約候補4件 | 既存`Rule28And29CandidateTest`だけ、1回／5分。Docker／runtime実験は行わない。旧候補と正式APIを区別して報告 |
| package／既存Reference critical journey | Rules JAR1回・通常Reference package1回と既存critical journey1件、10分。fixture非混入、main依存無増加、Reference source不変・設定／Security保護を確認 |
| cleanup／最終証拠 | 5分。今回所有物の終了とbaseline保持を確認 |

通常rootの予定無効S1 64／B2 process28は、規約改訂のPASS件数へ加算しない。S1／B2専用process再現は本票に含めず、そのsource不変を照合する。module testとroot重複を種類数へ二重加算しない。

最大同時JVM4、PostgreSQL container1、rootに限ってRyuk2・他phaseはRyuk1、今回所有接続8。既存testの承認済みpool／資源制限を使い、変更しない。既存critical journeyのbrowser1／context1、child1を直列実行する。memory8 GiB／disk10 GiBの空きを開始条件、raw100 MiB／run全体2 GiBを容量上限とする。追加browser／libraryのinstallは行わない。

予算・件数・資源超過、旧APIの意味変更、規約検出の抜け、公開signature不一致、source／artifact混同、Reference差分、test失敗、観測・cleanup失敗では停止する。得た結果と原因・有限訂正・残予算をOwnerへ示し、未消費時間から再実行を推定しない。

今回所有のPID・起動時刻・親子とcontainer IDを記録し、正常終了を先に要求する。必要な当該子終了だけを行い、他session／IDEのJavaや既存containerを停止しない。secretを含む一時設定は除去し、failure rawは保全する。 source／JAR／依存・設定・fresh XML・違反detail／resource samplesとcleanupを新しいR0-B Evidenceへ保存し、旧PL2／B2のEvidenceを上書きしない。

## 8 正本改訂案と承認後の適用順

§9.2の採用承認に基づき、以下を**R0-B規約改訂だけの例外**として文書整合する。code作成・検証は文書commit・clean source固定／preflight成立前に行わない。

| 正本 | 限定改訂内容 |
|---|---|
| `AGENTS.md`・project overview skill | 承認済み本票のsource固定・preflight後だけFramework Rules／Public APIの限定改訂を許容する導線。Reference runtime／POM／SQL禁止を維持 |
| business feature work skill | Rule 28／29の正式選択契約・移行手順への導線。選択APIの利用がruntime開始承認を代替しない旨 |
| P4-F提案§3／PL2 CP-F1 | Rule 28／29 blocking reviewを本票で扱う。R0-Cは規約部分だけの受入、CP-F1／S1全体の完了ではない |
| ADR台帳・グランドデザインのRule対応箇所 | ADR-005／event規約との整合、旧APIの互換、module選択、Rule 29保証限界。ADR番号は台帳確認後。runtime一括採用としない |
| Architecture Rules developer guide・Rules README | 旧APIと新APIの使用例、未指定拒否・明示選択、入力拒否、検出限界・既存ownership規則併用。新APIのコード例は採用後に記載 |
| 本票／Reference開始票／目的対応表／Evidence | 審査・規約実装・受入・R1開始の順、採否・source・未達を相互リンク |

正本の上位設計変更・API／Rulesの限定採用判断は§9.2のOwner承認による。runtimeの既存限定開始記録を流用しない。R0-C受入後にR1票を実APIへ合わせ、別途Reference非同期の正式開始判断を得る。採用済み仕様と未実装の現行artifactを区別し、検証・受入前に利用可能と案内しない。

## 9 Owner判断と今回の実施記録

2026-10-09、OwnerはReference限定例外の恒久化リスクを確認し、Level選択に対応したRule 28／29の限定改訂をR0で先に審査する順序を指示した。この方向を反映した。RL-01〜06の具体案／R0-B実行開始は未承認。

今回は現行API／Rule集合／test・既存Tooling候補／設計文書のread-only確認と、本票・R0 Reference開始票・目的対応表の文書編集だけ。code／Public API／POM／SQL／正本を変更せず、Maven／Docker・検証、git add／commit／remoteを実行していない。

### 9.1 Level 0／1共通条件・Level 2明示拡張へのOwner同意（2026-10-09）

Ownerは`LEVEL_0`指定でも同期eventの不存在を検査しない点を理解したうえで、次の整理に同意した。

> 今回の規約案では0／1の検査条件が共通、2だけ明示的に許容範囲を広げる も同意です

本同意は§3の意味整理と§4.1のLevel別基本方針へ反映する。同期eventの許容・transactional listenerの拒否について、Level 0／1の共通性を肯定・負例で確認する方針とする。API名／signature・module指定入力契約、Level 2 annotation細則、Rule 29の具体契約、検証上限・開始条件は別の審査事項として維持する。Framework実装・Public API変更・Maven／Docker検証・commit／remote・R1開始の承認には読み替えない。

### 9.2 規約レビュー案全体のOwner承認（2026-10-09）

Ownerは本票を指定して「規約レビュー案 を承認します。」と明示した。RL-01〜06、§3〜5のPublic API／Rule契約・互換性、§6〜7の有限対象・40新規case候補／60分枠・停止／cleanup、§8正本整合を採用し、文書source固定・clean／preflight成立後のR0-B限定作成・検証を承認したものとして記録する。

§7.1の文書local commitは別承認／Owner操作という条件を維持する。承認記録・ADR・正本差分を先に文書だけで反映し、そのcommit対象を提示する。承認時点でcode／Public API実装・Maven／Docker検証・commit／remoteは未実施。R0-C結果受入、成果commit、R1／Reference runtime・POM／SQL、DoD／正式受渡し／Phase 4全体は承認範囲外。
