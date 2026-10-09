> ARCHIVED / HISTORICAL ONLY（2026-10-10）。現在の実行指示・進捗判定には使用しない。

# S1 R0-B：通常rootの接続上限限定訂正Ownerレビュー（2026-10-09）

**現在の導線：§37「R0最小検証集合・区切りと実行順の見直し」（2026-10-10）。** Ownerの最新指示で監視caseの網羅拡張とroot反復を停止し、R0本体の検証へ集中する。Rules107件は成立済み、通常rootは未成立。§1〜36は履歴として保持し、次の実行指示には流用しない。最終結果受入はOwner判断。[最新Evidence](../validation/phase4-s1-r0-rules-source-fixed-preflight-20261009.md#44-r0最小検証集合への集中監視拡張停止2026-10-10)を参照。

## 1 訂正案

通常root `clean verify`だけ、今回所有PostgreSQLのclient connection上限を**8→12**へ訂正する。他phaseの接続上限8、JVM4／PostgreSQL1、root限定Ryuk2／他phase Ryuk1、memory／disk／raw／file／件数・各集合1回・追加再実行0回、既存test／pool不変更を維持する。

根拠は、同日受入済みrootの観測最大11（8超14 sample）。12は今回の停止上限案であって、現在sourceの実測PASSや本番値ではない。今回rootで12超・観測不能・他上限超を見つければ停止する。observer／管理接続も数え、定義を狭めて上限内に見せない。

新provider／依存／agent取得、既存testのpool削減、検証追加、Reference code・POM・SQL変更は採用案に含めない。依存closure・classpath・旧Consumer bytecode等の残preflightは成立を確認してからcodeへ進む。不足取得が必要なら一覧を提示して停止する。

## 2 残予算と再開

総60分のうち240秒計上、残56分。preflight残6分、Rules15分／root15分／PL2候補5分／package・既存E2E10分／cleanup5分を維持する。追加再実行を増やさない。ここまでtestは0件、再開後の予定した初回集合だけを実行する。

訂正承認後に`AGENTS.md`と規約票§7.2のroot限定接続上限を反映し、承認sourceと保全済みbaselineの差分を確認する。scope／資源・依存・preflight条件不成立ではcodeへ進まない。

## 3 訂正文書のsource固定案（別承認）

先の承認による文書14件のlocal commit1回は既に完了した。本訂正で必要になる追加commitは別判断とし、先の許可を流用しない。

承認後の文書4件だけを現在branchへ**追加local commit1回**で固定する案：

1. `AGENTS.md`：root接続だけ12へ訂正、他条件維持。
2. `docs/development/phase4-s1-r0-rule28-29-level-selection-owner-review-20261009.md`：§7.2のroot限定接続訂正と実際のOwner承認記録。
3. `docs/architecture/validation/phase4-s1-r0-rules-source-fixed-preflight-20261009.md`：完了commitとpreflight停止のEvidence。
4. `docs/development/phase4-s1-r0-root-connection-limit-correction-owner-review-20261009.md`：本訂正案・採否・固定対象。

全体の文書対象は既存14＋新規2＝16で、規約票の上限内。codeはstageしない。message案は`docs: correct R0 root connection ceiling`。親`9c41aa4b3fc93cd0c6afb2c767171a771e42616b`・対象4件・新HEAD・cleanを確認し、そのsourceで承認済み残preflightへ戻る。remoteは行わない。

## 4 判断欄

Ownerは本票を指定して「確認、承認いたします」と明示した。root限定接続12、他上限・残予算維持の訂正採用、および§3文書4件の追加local commit1回と残preflight再開を承認済み。規約結果受入・成果commit・R1／Reference runtime・remoteの承認は含まない。commit後のSHA・clean・preflight実測はrun Evidenceへ記録し、本票の追加commitを発生させない。

## 5 再開後のpreflight時間計上・限定訂正（OWNER APPROVED）

§1〜4の接続上限訂正と4件commitは実行済み（`671fd17`）。本節はOwnerの個別承認を受けた追加訂正である。[Evidence§5](../validation/phase4-s1-r0-rules-source-fixed-preflight-20261009.md)の技術条件は成立したが、時間guardのUTC二重変換とOwner待ちの分離計測不足により、preflight枠の成立が確認できずcode前で停止した。

### 5.1 時間枠だけの訂正

preflight上限を10→15分とする案。総60分は増やさず、既計上810秒・残46分30秒、cleanup5分予約を維持する。現段階の技術条件確認は成立しているため、未済のsource固定後差分確認だけを行い、新たなclasspath探索・依存取得・既実施準備の反復をしない。既存依存補完の失敗2回を保全し、予定のvalidation各集合はまだ0回で、実行はそれぞれ初回だけ。

Rules15分・root15分・PL2候補5分・package／既存E2E10分の個別上限を維持するが、総残予算が優先する。各枠の最大値を使い切れる許可とはせず、cleanup予約を残せない場合は停止する。接続root12／他8、他資源・file／case・raw・追加再実行0回・既存test不変更・remote除外を維持する。

時間guardはraw timestampをDateTimeOffsetで扱い、今後の各実行はprocessの実行開始・終了／実効timeoutを計測し、Owner待ちと文書作成を混入させない。タイマ訂正はrun所有の一時harnessだけで、Framework code／依存を変更しない。9時間多い元recordを消さない。

### 5.2 訂正source固定案

本節の採用時に、§3と同じ文書4件（AGENTS、規約票、本票、preflight Evidence）だけへ時間条件・実際の承認記録を反映し、現在branchへ追加local commit1回で固定する案。親は`671fd171a7c2ce70eff571995a5f20b3c598c1ae`。先のcommit許可は実行済みなので、今回の追加固定は別判断とする。全体文書対象16 fileを維持する。

Ownerには、preflight15分・総60分／残46分30秒・他上限維持、上記文書4件の追加固定と承認済み初回validationへ向けた条件付きcode作成を判断対象として提示する。Ownerは「この限定訂正で進めてよいです」と明示した。preflight15分、総60分・既計上810秒／残46分30秒・cleanup300秒予約、他上限維持と文書4件の追加local commit1回、source固定後の差分確認および成立時の承認済みcode／初回validationを承認済み。成果commit・結果受入・R1／remoteは含まない。

## 6 初回compile失敗の有限訂正・Rules追加1回（OWNER APPROVED）

### 6.1 原因と修正対象

初回Rules `test`はmain compileでexit1、test0。私の実装でArchUnit enum accessorを誤り、`getName()`と記述した。既存依存の実効APIは`name()`である。boolean／Objectの型診断も同じOptional式から出ているが、未解決methodに伴う二次診断かどうかはcompile未成立のため断定しない。依存不足・provider取得・Docker障害ではない。

変更は既存承認対象の`BusinessModuleRuleSet.java`1 fileのpropagation判定だけ。`JavaAnnotation<?>`を明示し、値取得と`JavaEnumConstant.name()`比較を分ける。標準`ApplicationModuleListener`の`REQUIRES_NEW`条件と既存負例を維持する。案は次のとおり（まだ適用しない）：

```java
private static boolean hasStandardPropagation(JavaMethod method) {
    JavaAnnotation<?> listener = method.getAnnotationOfType(APPLICATION_MODULE_LISTENER);
    @Nullable Object propagation = listener.get("propagation").orElse(null);
    return propagation instanceof JavaEnumConstant constant
            && constant.name().equals("REQUIRES_NEW")
            && constant.getDeclaringClass().getName().equals(
                    "org.springframework.transaction.annotation.Propagation");
}
// direct standard annotation確認済みの場合だけ呼ぶ。
boolean propagationOverride = standard && !hasStandardPropagation(method);
```

同fileへ`JavaAnnotation` importを追加する。raw／meta拒否、他Rule、旧API診断、test assertion／40件、POM・Referenceは変更しない。run所有scriptのconsole要約はHashtableを`[pscustomobject]`へ変換して表示する（保存済みresult JSONは正常、時間／exit／所有台帳を訂正しない）。新規tracked fileを増やさない。

### 6.2 再開案と残予算

限定修正後、Rules module全testだけを**追加1回**許可する案。上限120件／15分を維持し、残2783秒・総60分・cleanup300秒予約が優先する。最初の失敗log・resultを上書きせず、別stage名で実行する。追加compile／test失敗、検出の抜け、scope／上限／cleanup不成立では再度停止し、さらなる実行を自動で増やさない。

Rulesが成立した場合に限り、既承認で未実行のroot1回／PL2候補4件1回／package・旧Consumer・既存E2E1回へ進む。各集合の個別上限と残総時間、JVM4／PG1／root接続12・他8／rootRyuk2・他1、既存test／pool不変更、件数・容量上限を維持する。

前回source固定は完了している。本案は未commit Rules作成分の上記1 file修正とrun所有scriptの表示訂正・追加Rules1回だけの判断であり、新たな文書commit／code成果commitを求めない。新HEADやcleanを装わず、文書source `e8c81fe`＋作成済み差分のhashを固定して再開する。規約結果受入・成果commit・R1／runtime・remoteは別判断。

### 6.3 Owner判断欄

Ownerは本節を指定して「確認し、承認します」と明示した。§6.1の1 file限定修正・tmp表示訂正、§6.2のRules追加1回と成功時の既承認未実行集合への継続を承認済み。残2783秒・cleanup300秒予約・他上限を維持し、さらなる失敗時は停止する。新たなcommit／成果受入／R1／remoteは含まない。

## 7 診断ID登録・新規test assertionの有限訂正（OWNER APPROVED）

### 7.1 確認済み結果・原因

§6の修正とRules追加1回を実施した。compileは成立、107件で74 PASS／2 failure／31 error／skip0。既存67件は全PASS、新規40件は7 PASS／2 failure／31 error。root・PL2・package・旧Consumer・E2Eは未実行。元XML13 fileとhashをrun所有Evidenceへコピーした。

31 errorはすべて`RuleMessage.of(29, ...)`が投げた`ruleId must identify a Phase 1a failure rule`。既存helperの許可集合へ29を登録していなかったため、新overloadの規則生成時点で拒否された。Rule29の検出結果ではなく、規則生成失敗である。

2 failureは新規`frameworkOwnershipStillRejectsInternalConsumer`と`legacyApiDoesNotAcquireRule29Diagnostics`。実際の違反detailには正しくConsumer→internal、listener→outbound依存があるが、testがdetail内の`KOIKI-ARCH-013`／`001`を期待していた。既存`RuleMessage`はID／根拠／影響／修正をrule descriptionへ、実際の違反をdetailへ分ける契約で、detailにはIDがない。diagnostic形式を変えるのでなく新規testを訂正する。

静的点検で、errorに隠れて未到達だった他の新規assertionにも同じID検索があることを確認した。実行済み2 failureだけを個別に直すのではなく、作成済み4 test class全体の同種誤りを今回の有限訂正に含める。ただし訂正後の検出成立は未検証であり、PASSを先取りしない。

### 7.2 修正対象と確認方法

| 対象 | 訂正内容／維持すること |
|---|---|
| `RuleMessage.java` | `FAILURE_RULE_IDS`へ29だけ追加。message本文・旧ID／旧API診断・allowance10／unknown99の拒否は維持。既存helper変更枠の1件、tracked codeは15→16 fileで24以内 |
| 新規`ModuleEventLevelSelectionContractTest` | 空Map／Level0／1の拒否は選択Rule28の実際の`hasViolation()`とcompositeへの当該detail包含で確認。null・防御copy・指定順・不存在guard・40件内のcase数を維持 |
| 新規`Rule28LevelSelectionTest` | 同じselectionの個別Rule28の評価と公開compositeの違反detailを照合。標準許容／各raw・meta・override拒否を維持。誤配置は個別Rule38がFAIL・選択Rule28がPASS・compositeへのRule38 detail包含で確認 |
| 新規`Rule29SynchronousSideEffectTest` | direct／metaのRule29 FAIL・transactionalのRule29 PASSを個別評価。Rule1重複は1と29の両方が実違反しcompositeへ両detailが含まれることを確認。Level2は1 FAIL／28と29 PASS。旧APIはcomposite descriptionに29が追加されないことと既存Rule1 detailの保持を確認。間接経路・root外の限界は維持 |
| 新規`LevelSelectedCompositeRegressionTest` | Ownership個別Rule13の実FAILとpublic composite detail包含を照合。Tier／persistence／公開境界／module境界は対応する個別7／8／17／3の実FAILと選択composite detail包含を確認。signature・旧診断比較・root必須・既存制約の保持を維持 |

case method数10／12／8／10、計40を維持し、既存67件・既存contract test・fixture・Rules本体の検出predicateを変更しない。新testの肯定／負例やRule単位識別を削除しない。descriptionだけのID検索でcompositeの実違反を判定しない。

確認の共通形は以下。既存`RequiredNegativeArchitectureRulesTest`と同じ個別rule評価を利用する。本文はまだ適用しない。

```java
EvaluationResult individualResult = individualRule.evaluate(classes);
EvaluationResult compositeResult = publicRule.evaluate(classes);
assertTrue(individualResult.hasViolation());
assertTrue(compositeResult.hasViolation());
assertTrue(compositeResult.getFailureReport().getDetails().containsAll(
        individualResult.getFailureReport().getDetails()));
// ID表示の検査は、実FAILを確認した個別ruleのdescriptionで別に行う。
assertTrue(individualRule.getDescription().contains("[KOIKI-ARCH-029]"));
```

否定条件は対応する個別ruleの`hasViolation()==false`で確認する。legacyへのRule29非追加はdescriptionの非存在検査が適切で、detailだけのID不存在を証拠にしない。旧messageのdetailへIDを追加する案は採らない。

### 7.3 有限再開・残予算と判断欄

残2741秒（45分41秒）、総60分・cleanup300秒予約。他の資源・件数・容量・source／Ownership境界を維持する。上記helper1＋作成済み新test4の訂正、Rules全testの追加1回（120以内／15分）を判断対象とする。成功時のみ、既承認で未実行のroot1回／PL2候補4件1回／package・旧Consumer・既存E2E1回へ進む。総残時間が個別枠より優先し、さらなる失敗では停止する。

前回2回のRules失敗log／XMLを保全し、stage名を別にする。新たな依存取得・POM／Reference／既存test／Ruleの検出条件変更は含まない。doc対象16・code16・fixture6・新case40を維持し、成果commit／remote／R1・受入を許可しない。文書source `e8c81fe`＋採用差分hashを記録する。新たな文書commitは求めない。

Ownerは本節を指定して「確認し、承認いたします」と明示した。§7.2のhelper1＋新規test4の有限訂正、Rules追加1回、成立時の既承認未実行集合への継続を承認済み。残2741秒・cleanup300秒予約・他上限を維持し、さらなる失敗時は停止する。commit／受入／R1／remoteは含まない。

## 8 生成物分離・Rules追加1回（OWNER APPROVED）

### 8.1 確認済み事実と未確定事項

§7のhelper1＋新規test4を訂正しsource hashを保存した。しかし追加Rules実行ではmain／test双方が`Nothing to compile - all classes are up to date`となり、JUnit探索で`NoClassDefFoundError: PackageName`、exit1、今回test0で停止した。前回107件のXMLは今回の実績として扱わない。

`javap -v`で、既存targetのFacadeと新testに`Unresolved compilation problems`を投げるbytecodeと不正descriptor `()LPackageName;`を確認した。正しい型は`org.koikifw.archunit.PackageName`であり、同packageの`PackageName.class`自体は存在する。依存を追加して直す問題ではない。

今回対象のclassは19:02:51 JSTに更新され、Maven開始19:03:01.421より前だった。Mavenがcompileしなかったため、今回Maven以外が生成したclassを再利用したことは確定。IDE等の別compilerによる生成を疑うが、書込みprocessは特定していない。IDE／別sessionの停止・設定変更は実施せず、必要な承認対象にも含めない。

当初の検証環境準備で、同じtargetへの別生成者混入を十分に防げていなかった。§7訂正後のsource hashは不変だが、そのsourceから正規Maven compileしたartifactの成立は未証明。これはRule検出PASSでもsourceの新たなcompile error確定でもない。

### 8.2 限定的な検証環境訂正

run所有`tmp/r0-rules-9c41aa4-20261009-first/source-isolated/`へ、文書source `e8c81fe`と承認済み未commit差分のsource／POMを1回コピーする。Gitのtracked＋非ignored作成ファイルを正確に列挙し、`.git`／`target`／既存tmp／cache／端末設定を持ち込まない。新source／API変更ではなく、同一入力の検証出力だけをrun所有の新しい場所へ分離する。

元・コピー先のpath／SHA256を全件manifest化し、コピー前後と各phase前後で差分0を要求する。既存target・failure class・log／XMLは削除・上書きせず保全する。snapshot内だけに新targetを生成し、旧target／旧JARを今回の検証へ使わない。既存run所有m2と既存browser cacheを使い、新規取得0、raw100MiB／run2GiBを維持する。

Rules再開は以下の有限順序とし、**合計15分以内**・総残予算優先とする。

1. 新snapshotのRulesでoffline `clean test-compile`を1回。testは起動しない。正規JDK21／Maven／Error Prone／NullAway設定をそのまま用いる。
2. snapshotのcompile結果を静的検査。Facadeの公開descriptor、`rootPackage()`の正しいFQCN descriptor、class内の`Unresolved compilation problem`非存在、source／artifact hashとcompile logを記録。検査失敗ではtestを開始しない。
3. 同snapshotで既存Surefire `3.5.6:test`直接goalによるRules全test追加1回、120件以内。compileと静的検査の実時間を15分枠から差し引く。fresh XML／hashを保全し、旧epochのXMLを混ぜない。

規約source・assertion・40 case・POM／依存・Referenceを追加変更しない。実行専用tmp harnessのみ、snapshotをworking directoryに指定し、source identity／compile epoch／総phase時間を照合する。既存のprocess所有・resource／cleanup条件を維持する。snapshotへの別生成者混入、source／artifact不整合、compile／test失敗では停止し、再実行を増やさない。

Rules成立時だけ、既承認で未実行のroot `clean verify`1回・PL2候補4件1回・package／旧binary Consumer／既存E2E1回も同snapshotで行う。既存Consumer bytecodeは再compileせず、snapshotの新Rules JARを先頭に接続する。E2Eもsnapshotの新Reference JARを指す。通常root集合／予定無効92・保護Reference sourceを維持し、異なるsourceや旧artifactで代替しない。

### 8.3 残予算・Owner判断欄

今回process3.352秒を4秒計上、証拠保全・cleanupを5秒計上。累計868秒、残2732秒（45分32秒）、cleanup300秒予約、総60分を維持する。root接続12／他8、JVM4／PG1／rootRyuk2・他1、件数・code16／doc16／fixture6と各集合上限を増やさない。

Ownerは本節を指定して「確認し、案を承認します」と明示した。§8.2のsourceコピー・manifest・snapshot内compileと静的artifact検査、Rules追加1回および成立時の既承認未実行集合をsnapshotで継続することを承認済み。新たな文書commit／code成果commit・IDE操作・remote・R1開始・結果受入は求めない。既承認scopeの不整合artifactを再利用せず、検証環境の訂正に限定する。

## 9 tmp runner所有判定の訂正・通常root追加1回（OWNER APPROVED）

### 9.1 成立と停止原因

§8で同一source1156 fileをsnapshotへコピー・hash照合した。新compileと173 classの静的artifact検査は成立、Rules追加107件は全PASS（既存67＋新40、failure／error／skip0）。source／class hashも一致。Rules追加実行は不要とする。

続く通常root初回は100.082秒でtmp監視runnerが`Unexpected container; ownership cannot be established`として停止した。残る通常testが未完了なのでroot PASSではない。保存済み部分XMLは117件、failure／error／skip0。停止containerのsampleは記録前にguardで拒否したため、root全体の資源成立を証明しない。

所有判定はRyukにも`org.testcontainers.sessionId` labelを必須としていた。既存cacheのTestcontainers2.0.5実効bytecodeでは、`RyukResourceReaper.register`がRyuk自身に対してlabel登録を迂回しており、同labelをRyuk必須条件にしたのが誤り。Ryuk名は同sessionのIDを使う。停止containerはowned JVM28600の起動logにfull ID `1d592e0ad531dcde9b3e67f24e1107ef4ff91f6e30abf239ccd134249cd8b046`で記録されている。

停止後そのcontainerは既に消えていたため、当該instanceの実labelsを後から採取することはできなかった。guardがどのsubconditionに該当したかを保存していない限界も保持する。実効libraryで成立しないRyuk label前提と、今回所有JVMによる起動IDは確認済み。Docker／test不具合とは断定しない。

### 9.2 tmpだけの限定訂正

`Run-Bounded.ps1`だけを訂正する。tracked Rules／Reference／test／pool／POMを変えない。

- PostgreSQLは従来のsession labelを維持する。Ryukはimage・作成時刻・`testcontainers-ryuk-<session UUID>`のname、今回所有JVMの起動logにある同じfull container IDを照合する。所有PIDと起動時刻をprocess台帳と照合し、imageだけ／他Javaとの差分だけで所有としない。
- Ryukのsession UUIDと後続PostgreSQLのsession labelを照合する。起動logが採取できない場合に限り、短い有限待機内で同sessionのPG labelとの突合を待つ。未確定containerも数に含めて上限監視し、所有確定前の停止／削除対象としない。5秒以内に対応づけ不能なら停止する。
- Docker inspect timestampは`ConvertFrom-Json -DateKind String`でraw文字列を保ちDateTimeOffsetで比較する。今後の拒否時にはID／image／name／created／許可したtestcontainers labels／拒否条件をcredentialなしで保存する。今回は失われた実labelsを捏造しない。
- 実containerなしのrun所有harness確認6例（Ryuk labelなし＋所有log一致、PG対応session、他PID、異なるID、旧時刻、対応づけ不能）で所有判定を確認してからrootへ進む。Repository test／40 caseや検証moduleの追加ではなく、今回の監視scriptの有限確認に限定する。

### 9.3 有限実行と判断欄

累計1030秒、残2570秒（42分50秒）、cleanup300秒予約・総60分を維持する。copy5秒／Rules compile12秒＋静的確認2秒＋test32秒／root101秒／cleanup5秒＋静的原因照合5秒を計上。正規Rules集合のPASS107はrootとの重複を種類数へ二重加算しない。

Ownerは本節を指定して「承認します」と明示した。§9.2のtmp限定訂正と6例確認、snapshotの通常root `clean verify`追加1回（15分・400以内、旧予定無効92維持）、成立時の既承認未実行PL2／package・Consumer・E2Eへの継続を承認済み。Rules単独の追加実行はしない。資源root接続12／他8・JVM4／PG1／rootRyuk2・他1、容量・他停止条件を維持し、さらなる失敗は停止する。未消費時間から追加実行を推定しない。成果commit・受入・R1／remoteは含まない。

## 10 tmp監視の正常終了競合訂正・通常root追加1回（OWNER APPROVED）

### 10.1 結果・原因

§9の所有判定訂正と実containerなし6例確認は全PASS。追加rootはRyuk所有判定・PostgreSQL回帰を通過したが、168.556秒で`Container identity observation failed`として停止。`docker ps`が列挙したID `b614daf637b5`が直後の`docker inspect`では`no such object`となった。full IDは保存済み所有台帳の`b614daf637b5812ab45a615a9faa4f7e730b5108cb93f5be47573ee1fee2306f`と一致し、imageはRyuk、`OWNED_LOG_ID`成立済み。列挙とinspectの間の当該終了／削除を、tmp runnerが一律に観測失敗扱いしていた。

部分fresh XML168件、failure／error／skip0。ただしroot完了・予定無効92の最終照合は未了。今回の観測最大はJVM2／PG1／Ryuk2／client接続11で既存上限内。累計所有Ryuk4件等の履歴を同時最大に取り違えない。Rules単独107 PASSは既に成立しており、その追加実行は不要。

monitorの終了時競合への備えが不足していた。これはtest assertion失敗や接続上限超ではない。正常終了の確認と「存続中に観測できない」状態を区別して有限に扱う。source／Rule／Reference／既存test／poolの修正は不要。

### 10.2 tmp runnerだけの有限修正

`Run-Bounded.ps1`のみ変更する案：

1. container列挙を`docker ps --no-trunc -q`に統一し、保存済みfull IDで対応づける。
2. inspect失敗またはPG ready／接続query失敗時、当該IDについて**追加read-only確認1回だけ**を行う。既に所有が成立していて、fresh running一覧から消えた／inspectでnon-runningを確認できた場合だけ、`OWNED_CONTAINER_RETIRED`を時刻・full ID・既知のimage／所有根拠付きで記録し、そのsampleのactive countから外す。過去の資源ピーク・samples・失敗logは変更しない。
3. IDがなおrunning・所有未確定・Docker一覧自体を読めない場合は従来どおり停止。containerを勝手に削除・停止して成立させない。無期限の再照会、DB observerを上限から除く操作、観測不能のPASS扱いは行わない。
4. 実containerなしの有限確認4例（既知owned IDの消失、既知owned IDのnon-running、未知ID消失の拒否、存続IDの観測失敗拒否）を実施してから追加rootへ進む。Repository test／新case40には追加しない。

概略判定は以下。まだ適用しない。

```powershell
$freshRunningIds = @(docker ps --no-trunc -q) # failed observationなら停止
if ($containersOwned.ContainsKey($fullId) -and $fullId -notin $freshRunningIds) {
    # 正常終了／削除を記録。既知の当該IDだけをactive countから外す。
} else {
    throw 'Container remains running or ownership is unknown; observation failed'
}
```

追加のstate確認も同じ有限1回枠に含め、競合のたびに無制限pollしない。source hash・artifact epoch・cleanup・resource上限の維持を確認する。

### 10.3 残予算・Owner判断欄

§9前1030秒＋harness6例／source確認2秒＋root169秒＋証拠／cleanup5秒＝累計1206秒、残2394秒（39分54秒）。総60分・cleanup300秒予約を維持する。今回所有process／container残0、既存Java24852／10280保持、snapshot／元code hash一致。

Ownerは本節を指定して「案を承認します」と明示した。tmp runnerの上記終了競合だけの修正・有限4例確認、同snapshotの通常root `clean verify`追加1回（15分／400以内、旧予定無効92維持）、成立時の既承認未実行PL2候補4件／package・旧Consumer・既存E2Eへの継続を承認済み。Rules単独追加なし、root接続12／他8・JVM4／PG1／rootRyuk2・他1・容量／code／doc上限維持、さらなる失敗では停止。成果commit・受入・R1／remoteは含まない。

## 11 通常rootの接続上限・追加1回の限定訂正（OWNER APPROVED）

### 11.1 実測と判断の根拠

§10のtmp正常終了競合訂正と実containerなし4例は全PASS。その後の通常rootは124.372秒でresource guardにより停止した。2026-10-09 19:32:08.844 JSTのsampleはJVM2／PG1／Ryuk1／DB client13、直前sampleは同PGの起動待ち。接続13は承認上限12の実超過であり、単なるrunner誤停止とは扱わない。

queryは当該PGの`pg_stat_activity`の`backend_type='client backend'`を数え、observer接続を含む。PGは今回所有1件、別sessionのDBを加算していない。アプリ・migration等の保持元内訳はこのaggregateだけでは確定せず、13の原因をpoolだけに断定しない。失敗前logは当該Reference PostgreSQL integration testの起動・migration中。保持元調査の追加queryや診断反復を先行しない。

他資源の観測最大JVM2／PG1／Ryuk1、最小memory17,080,401,920 bytes／disk50,571,608,064 bytes、raw5,265,012 bytes／run565,196,442 bytesは既存上限内。部分XML128件、failure／error／skip0だが、root完了・予定無効92・全Reference回帰は未成立。Rules単独107 PASSは別epochの成立済み結果として維持する。

### 11.2 訂正案

通常rootだけのclient connection上限を**12→14**へ訂正する。今回実測13に1接続の余裕を置く有限停止上限案で、本番設定値・今後の最大14保証ではない。他phaseの接続8、PG1／JVM4、rootRyuk2・他1、observerを含む数え方、既存pool／test／Framework／Reference source／POM不変更を維持する。14超・観測不能・他上限超・test失敗・cleanup不成立なら停止する。数え方を変えて13を12以内に見せない。

承認時は既存AGENTS・規約票の現行root上限と本票／Evidenceの実際の承認記録を整合する。文書対象16の既存4 file内、code／test16 file・fixture6・新case40を維持する。実行はtmp runnerの当該root選択argument `-ConnectionLimit 14`だけとし、default8を維持、source snapshotのcode／POMを変更しない。新たな文書commit・成果commitは求めず、`e8c81fe`＋採用差分／harness／snapshotのhash identityを保持する。

### 11.3 有限再開・残予算・判断欄

§10前1206秒＋4例／source確認2秒＋root125秒＋証拠／cleanup5秒＝累計1338秒、残2262秒（37分42秒）、cleanup300秒予約・総60分を維持する。通常root `clean verify`追加1回だけ（15分・400以内、旧予定無効92維持）、成立時の既承認未実行PL2候補4件／package・旧Consumer・既存E2Eへの継続を判断対象とする。各phaseの残総時間が優先する。

Ownerは本節を指定して「確認、承認いたします」と明示した。root接続上限14・他上限／境界維持、上記文書整合と同snapshotのroot追加1回、成立時の既承認未実行集合への継続を承認済み。Rules単独追加・接続保持元の追加調査・新規依存取得・既存pool／test変更、commit／受入／R1／remoteは含まない。今回所有process／container残0、元code／snapshot hash一致を確認済み。過去のresource超過recordを削除・PASSへ置換しない。

## 12 tmp監視の所有記録維持・短命launch捕捉の限定訂正（OWNER APPROVED）

### 12.1 §11追加1回の結果と確定した不足

通常root追加1回は165.116秒で`Container ownership rejected: PENDING_SESSION_PAIR`停止。接続上限14の超過を検出した停止ではない。部分fresh XML25 file／154件、failure／error／skip0を保全したが、root完了・予定無効92の最終照合は未成立。Rules単独107 PASSは維持し、追加実行しない。

拒否されたRyuk full ID `af546844fbd2263316660d41fbdded4c3730a6ea7012eaad1a783a9f7f0aa400`は、同じ実行の所有台帳では既に`OWNED_SESSION_PAIR`成立済み。対応PG `d76f0ed8555cb475143b16658b0ccb6ebbf9ffecbd10495e5c45c8c73d5c2bf5`は19:41:48.608 JSTにnon-runningを確認・終了記録済みだった。tmp runnerは毎sampleでlive pairを要求し直すため、PG終了後に所有済みRyukを未確定へ戻し、5秒待ち後に拒否した。確立した当該instanceの所有記録を維持できていない。

もう1点、当該Ryuk起動logはPID30512を示すが、そのPIDはprocess所有台帳にない。logには当該forkの起動からtest完了までが存在する。CIM現在一覧をpollし、同じ一覧内にparentが残っている場合だけ子を登録する方式では、短命の中間launch processを捕捉できない可能性がある。中間parentの実PID／終了時刻は記録されておらず、この原因は仮説に留める。ただし**実行済み子JVMの台帳欠落は確定**であり、今回sampleのJVM最大2を全JVM捕捉成立の証明には用いない。

sample最大はJVM2／PG1／Ryuk2／client11、memory最小16,747,028,480 bytes／disk最小50,581,037,056 bytes。停止時にsample未記録の区間と上記PID欠落の限界を保持する。raw8,486,406 bytes／run569,209,752 bytes。今回所有process残0／Docker running container0、既存Java24852／10280保持、snapshot全1156 fileおよび元code hash不変を確認した。`isolated-root-limit14-retry-ownership-rejection.json`／result／resources／retired-containers／log、`root-limit14-partial-xml/`、`root-limit14-cleanup.json`に保存済み。

### 12.2 tmp runnerだけの訂正・成立条件案

tracked Rule／Reference／test／pool／POMの追加変更なし。`Run-Bounded.ps1`だけで、次の2不足をまとめて訂正する承認時の案。§12承認後に適用し、下記実行結果を追記した。

1. **所有済みcontainerのidentity維持**：full IDと保存済みimage／name／Createdを毎回照合し、同一instanceなら確立済み所有根拠を維持する。PGの消失やlog PIDの未登録を理由に、そのRyukを未確定へ戻さない。identity不一致、未知ID、古いinstanceは従来どおり拒否する。active count・PG query・既承認の有限retirement確認は省略せず、所有済みだから観測不能をPASSにする操作はしない。
2. **launch履歴の捕捉**：Maven起動前から今回限りのWindows process start eventを登録し、PID／parent PID／発生時刻／process名の有限台帳を保持する。今回起動rootから辿れるparent chainだけを所有候補にする。CIMで生存instanceのcreation ticksを照合して正式登録し、PID再利用を区別する。台帳にないlog PIDをlog文字列だけで所有としない。短命parentの終了後も、確認済み起動関係を保持して生存子を上限計数する。別Javaや全Javaをcleanup対象へ取り込まない。
3. **実行前harness確認**：実containerなしの有限8例（所有済みRyukのPG終了後維持、identity不一致拒否、未知ID拒否、既知retirement維持、短命parent履歴から子を捕捉、他root拒否、PID再利用拒否、未確認log PID拒否）と、今回所有の短命shell→既存JDK `java -version`1回だけを行う。実launch確認は10秒以内・新規依存なし・Dockerなし。eventの登録／parent関係／creation照合と解除の成立を確認できない場合はrootを開始しない。確認全体は60秒以内で残総時間から差し引く。OS権限変更や別方式での回避は行わない。
4. **実行中・終了時の照合**：current CIMとevent履歴を合流し、所有test logに現れたfork PIDが実launch台帳で説明できることを確認する。欠落・identity不整合・event受信失敗は停止して保全する。全終了経路で今回のevent subscriptionを解除し、所有process／container・既存Java保持を照合する。イベントにはcredential／command lineを記録しない。

起動eventは検証監視の補助手段で、瞬間的な全resource peakや本番運用保証を新たに主張しない。既存のsample方式・数値上限は維持し、今回判明した子JVM台帳欠落を解消してから追加rootを許可する案とする。

### 12.3 有限再開・残予算・Owner判断欄

§11前1338秒＋source確認2秒＋root166秒＋保全／cleanup5秒＝累計1511秒、残2089秒（34分49秒）、cleanup300秒予約・総60分を維持する。harness確認上限60秒をこの残時間から消費し、新たな時間枠を追加しない。

上記2不足の訂正とharness成立後だけ、同snapshotの通常root `clean verify`追加1回（最大15分／400以内、予定無効92維持）を提案する。root成立時に限り、既承認未実行PL2候補4件（最大5分）、package／旧binary Consumer／既存E2E1回（合計最大10分）へ進む。各phaseはその時点の残総時間からcleanup300秒を引いた値を優先し、上限を足し合わせて総60分を延ばさない。

接続root14／他8、JVM4／PG1／rootRyuk2・他1、code16／doc16／fixture6／新case40、容量・source snapshot・artifact epoch・他停止条件を維持する。Rules単独追加、Reference／test／pool変更、接続保持元調査、新規依存取得、commit／結果受入／R1／remoteを含まない。

Ownerは本節を指定して「承認します」と明示した。上記tmp監視2不足の訂正、有限8例と短命shell→既存JDK確認1回、成立時のroot追加1回と既承認未実行集合への継続を承認済み。数値上限・残総時間・cleanup予約・source／artifact境界を維持する。harness不成立ではrootを開始せず、さらなる失敗では停止する。新たなcommit／受入／R1／remoteの承認は含まない。

### 12.4 承認後の適用・harness権限エラー停止

tmp runnerへcontainer identity維持と起動event履歴による親子追跡／CIM creation照合を適用し、PowerShell parser・`git diff --check`は成立。tracked code／test／pool／POM追加変更0。最初の呼出しは必須`Arguments`への空文字bindingでscript本体起動前に終了したため、未実行harnessを非空の未使用引数で開始した。短命shell／Javaを2回起動したものではない。

権限付き実行によるharnessは0.220秒で`アクセスは拒否されました`を記録して停止。8例の不一致ではなく起動event登録を含む外部操作段階の権限エラー。probe出力fileは作成されておらず、短命shell→Java確認は未開始、root追加1回も未開始。個別8例のJSONはprobe成功後に保存する構成だったため未作成で、harness全体のPASS証拠として扱わない。catchがmessageだけを保存したため、原exceptionの失敗行・HRESULTは未採取という診断上の限界も保持する。

`launch-probe-result.json`のsubscription残0、停止後Docker container0／追加Java0、既存Java24852／10280保持。snapshot全1156 file／元code hash一致、harness SHA256 `DC7626A2AF2830A2420347BA40C645A3F71978FCA968792648D3F86B8E0D4525`。`section12-stop-cleanup.json`へ保存済み。harness1秒＋保全／cleanup5秒を計上し累計1517秒、残2083秒（34分43秒）・cleanup300秒予約。Rules単独107 PASS維持、通常root未成立、PL2／専用package／旧Consumer／E2E未実行。

§12の「harness不成立ではrootを開始せず、さらなる失敗では停止」に従い停止した。権限付き実行というAI実行環境の承認が、Windowsの管理者tokenやWMI event権限成立を証明するものとは扱わない。OS権限変更、別event経路での迂回、harness／rootの追加再試行を行っていない。

## 13 起動event権限エラーの限定診断（OWNER APPROVED）

次はroot再実行ではなく、§12harnessが必要とする起動event登録を現検証環境で実行できるかを確定する。追加test・Java／shell launch・container操作・OS設定変更なし、tmpだけの診断1回・最大60秒を提案する。現remaining2083秒から実時間を差し引き、cleanup300秒・総60分・他上限を維持する。

診断内容は、現processのtokenがWindows管理者として有効かのboolean確認と、同じ`Register-CimIndicationEvent`／`Win32_ProcessStartTrace`登録1回。try/catchで失敗call名・原exception型・HRESULT・失敗行をcredential／command lineなしで保存する。登録が成立した場合も直ちに当該subscriptionを解除し、残0を確認する。権限付き実行の承認手順に従い、拒否時に別provider・権限変更・管理者起動を自動で試さない。

Ownerは本節を指定して「承認します」と明示した。上記token確認と同event登録の限定診断1回・最大60秒を承認済み。harness全体の再実行・root追加1回の再開は含めない。失敗行と実行権限を確定してから、現環境で可能な修正か、Ownerによる検証環境対応が必要かを整理する。§12の未消費root1回を無条件の再開許可へ読み替えない。

### 13.1 限定診断の結果（2026-10-09）

**COMPLETE / OWNER ACCEPTED（診断結果のみ）**。Ownerは本節を「確認し、承認しますので、次に進めましょう」と明示した。診断の受入と、Windows管理者起動／harness再実行／root再開の実行承認を区別し、次の実行環境・再開票を具体化する。

承認済み診断1回を実行環境の`require_escalated`手順で実施した。実測0.092秒、60秒以内。現在processのWindows管理者token有効性は`false`。`Register-CimIndicationEvent -Namespace root/cimv2 -Query 'SELECT * FROM Win32_ProcessStartTrace'`の登録が`PermissionDenied`で拒否された。診断scriptの失敗行14、原exceptionは`Microsoft.Management.Infrastructure.CimException`。WMIエラーは`FullyQualifiedErrorId`の`HRESULT 0x80041003`、例外object自身のCLR HRESULTは`0x80131500`で、両者を混同しない。

`section13-event-permission-diagnosis.json`へcall名・原exception型／HRESULT／失敗行・token booleanを保存した。登録成功false、subscription残0、子Java／shell・root・container操作なし。§12の厳密な拒否点未確定を、この同操作の限定診断で補完した。§12の元logに失われたexception情報を後から復元したものではない。

管理者token不成立とWMI登録拒否は確認済み。ただし管理者tokenだけが唯一の原因か、WMI namespace／providerの具体的権限、管理者実行なら必ず成功するかは、この診断では確定しない。AI実行環境の権限付き承認はWindows管理者tokenへの昇格を保証しない。現tokenの同登録を繰り返してharnessが成立すると見込まず、OS設定変更や別providerによる迂回を行わない。

診断1秒＋snapshot1156 file／元code照合1秒を計上し、累計1519秒、残2081秒（34分41秒）、cleanup300秒予約・総60分を維持。`section13-source-check.json`でhash一致を確認した。Rules単独107 PASS維持、通常root未成立、PL2／package／旧Consumer／既存E2E未実行。commit／remoteなし。

### 13.2 次の判断点

限定診断は完了。次はOwnerによるWindows側の検証実行権限／実行環境の判断となる。同event登録を許可できる環境とする場合も、Ownerが環境を指定し、登録成立・解除を実証してからharness全体の再確認／root再開条件を審査する。管理者起動・WMI権限変更・別監視方式の採用をAgentが自動で先行しない。

本節の承認は診断だけであり、harness再実行／root開始を含まない。既存source snapshot・成立済みRules証拠・未完了集合・残予算を維持して、実行環境と再開条件の判断を待つ。

## 14 Owner管理者PowerShellによる限定検証環境・再開（OWNER APPROVED）

### 14.1 選択する環境と権限境界

Microsoftの[Register-CimIndicationEvent公式文書 Example 1](https://learn.microsoft.com/en-us/powershell/module/cimcmdlets/register-cimindicationevent?view=powershell-7.5#example-1-register-the-events-generated-by-a-class)は、今回の`Win32_ProcessStartTrace`登録をPowerShellの管理者実行で行うよう明記している。[UACとWMI](https://learn.microsoft.com/en-us/windows/win32/wmisdk/user-account-control-and-wmi)も、管理者group所属と、実行processの管理者tokenを区別している。この方式の要件確認を提案前に済ませていなかった点を訂正する。WMI event登録一般の管理者限定を意味するものではない。

採用候補は、**Ownerが同じWindowsユーザーで管理者PowerShell 7を手動起動し、既存tmp runnerの承認済み限定操作だけをそこで実行する方式**。AgentがUACを操作して昇格したり、WMI ACL／OS policyを変更したりしない。別ユーザーcredentialは使わない。同ユーザーの通常terminalとのSID一致、実tokenのAdministrator=true、既存JDK／Maven／cache／snapshotの所在とhashを開始時に確認する。別アカウント起動・所在差異・hash不一致なら開始しない。

既存runnerはMavenを子processとして起動するため、この方式ではMaven・test fork・既存E2E childも管理者tokenを継承する。監視だけを管理者に分離した構成とは案内しない。承認対象は今回の既承認fixture・検証command・使い捨てDB・有限cleanupだけであり、一般の管理者操作・常用buildの管理者実行を含めない。Agentの通常セッションは現tokenのままとし、資料・結果照合を担当する。

監視だけを管理者helperに分離して通常権限Mavenへ接続する方式は、PID／creation／event時刻の別process間受渡しとcleanup責任の新規設計が必要となる。今回は採用せず、既存runnerの実行権限を明示してscope・source変更を最小にする。管理者実行でも登録が必ず成功するとは保証せず、下記harnessを開始条件とする。

### 14.2 承認時の準備・有限確認

対象は既存4文書とtmp runner／実行手順のみ。tracked code／test16 file、fixture6、新case40、Reference／pool／POM／依存は不変。新たなcommitは含めない。

1. AgentはtmpにOwner実行用の有限手順を準備する。管理者token・同ユーザーSID・source manifest・既存cacheとbaselineを確認するgateを置き、確認不成立でMavenを開始しない。現在runnerのharness8例をJava確認より先に個別保存し、失敗call／原exception型／HRESULT／行を保存する。旧`launch-probe-result.json`などを上書きせず、`section14-`の新epochへ保存する。
2. Ownerが同一ユーザーの管理者PowerShell 7でその手順を実行する。Agentによる管理者起動は行わない。実行前8例＋同event登録・短命shell→既存JDK `java -version`1回、全体60秒以内／実launch10秒以内。§12と同じ例・parent chain・creation照合・event解除を確認する。独立した登録診断をさらに反復せず、このharness1回に登録確認を含める。
3. event登録拒否、8例失敗、短命launch／creation照合不成立、subscription残、source／環境不一致では停止。別providerへ切替、設定変更、再試行を先行しない。今回processのtokenと、AI実行環境の`require_escalated`承認を区別して記録する。

### 14.3 成立時の実行順と予算

累計1519秒、残2081秒（34分41秒）、cleanup300秒予約・総60分を維持する。開始gate・source確認・harness・結果保全の実時間も残予算から引く。承認待ちやOwnerのterminal起動待ちは検証実時間へ混ぜない。

| 順序 | 成立条件と有限実行 | 上限 |
|---|---|---|
| 1 | 上記環境gate・8例／起動event／短命launch確認1回 | harness60秒、launch10秒。gate実時間も総予算へ計上 |
| 2 | 1の全成立後、同snapshotの通常root `clean verify`1回。§12で未起動だった1回の再開として扱い、回数を上乗せしない | 900秒／400以内、予定無効92の照合 |
| 3 | root全成立後だけ、未実行PL2 `Rule28And29CandidateTest`4件をjdbcで1回 | 300秒 |
| 4 | 3成立後、Rules／Reference package、旧Consumer bytecodeを新Rules JARへ接続、既存critical journey E2E1回 | この集合合計600秒以内 |
| 5 | source／fresh XML／artifact epoch／PID／container／event解除を照合・保全 | cleanup300秒を確保 |

各phaseは上限と、その時点の残総時間からcleanup300秒を引いた値の小さい方を使う。全phase上限を加算して総60分を延長しない。Rules単独107 PASSは再実行しない。旧binary Consumerを再compileせず、root XMLとRules単独XMLの重複を件数へ二重加算しない。

rootだけclient14／Ryuk2、他phase client8／Ryuk1、JVM4／PG1、memory8 GiB／disk10 GiB、raw100 MiB／run2 GiB、既存E2E browser1／context1／child1を維持する。未確定PID／container・観測不能、test失敗・上限超・cleanup不成立で停止し、管理者tokenであることを所有証明やcleanup範囲の拡大理由にしない。既存Java24852／10280を保持する。

### 14.4 Owner判断対象

上記の**同ユーザー管理者PowerShellで既存runnerと承認済み子検証を実行する権限境界**、tmp準備、有限harness再確認1回、成立時の未起動root1回／未実行後続集合への再開を判断対象とする。OS設定変更、別event方式、一般管理者操作、新規依存取得、code／test／pool変更、commit／結果受入／R1／remoteを含めない。

Ownerは本節のリンクを指定して「承認します」と明示した。同ユーザーのOwner管理者PowerShellでの既存runnerと子検証の権限境界、tmp準備、有限harness再確認1回、成立時の未起動root1回と未実行後続集合への再開を承認済み。管理者起動はOwnerが行う条件を維持し、Agentは準備・結果照合を担当する。OS設定変更・WMI ACL変更・自動UAC操作・別方式・成果commit／結果受入／R1／remoteは含まない。

### 14.5 Owner実行用手順の準備結果

`tmp/r0-rules-9c41aa4-20261009-first/Start-Section14-Owner.ps1`を準備した。Owner管理者PowerShell 7で同scriptを1回実行すると、同ユーザーSID／管理者token／branch・HEAD／snapshot全source／元code／依存・runner artifact hash／保護Java baseline／Docker空状態／容量／予算を確認してから、有限8例・event登録・短命launch1回、成立時の通常root1回へ進む。管理者起動・ExecutionPolicy変更をscriptが行うことはない。

`Run-Bounded.ps1`は承認どおり8例をprobe前に保存し、失敗call・原exception型・HRESULT・行を保存する。新epochを`section14-harness-*`／`section14-root-*`とし、既存§12／13失敗記録を上書きしない。harness／root各resultの存在を二重実行guardにし、同sequenceのsummaryが既にある場合も拒否する。

Owner手順はroot終了後のfresh XMLをSHA付きで保存し、全件400以内・failure／error0・予定無効S1 64＋B2 28＝92・その他skip0を照合する。source hash・PID／container残・subscription解除の結果を`section14-owner-root-summary.json`へ保存して一旦終了する。Agentがrootの集合・資源・所有証拠を確認してから、既承認PL2／package／旧Consumer／E2Eの具体的な続行手順を提示する。後続の承認を再要求するものではなく、成立条件の実証を挟む順序である。

既存依存JAR／POM、classpath入力、JDK／Maven binary、旧Consumer bytecode、runnerとOwner手順など1543 artifactのhashを`section14-expected-environment.json`へ記録。manifest準備2.962秒→3秒計上、累計1522秒、残2078秒（34分38秒）・cleanup300秒予約。PowerShell parserと`git diff --check`は成立。管理者起動・harness／rootはAgentから未実行で、Ownerによる手動起動を待つ。

Ownerは**同じWindowsユーザーでPowerShell 7を「管理者として実行」**し、次を1回だけ実行する。別アカウントのcredentialでは開始しない。

```powershell
Set-Location 'C:\Users\kataoka\Desktop\KOIKI-JAVA\KOIKI-JAVAWEB'
& '.\tmp\r0-rules-9c41aa4-20261009-first\Start-Section14-Owner.ps1'
```

終了後は「完了」または「停止」とOwnerから連絡する。Agentが同workspace内の保存resultを読み取るので、credential／raw logのchat貼付は不要。停止した場合は同commandを再実行しない。管理者起動は§14のOwner担当条件によるもので、新たな承認要求ではない。

### 14.6 Owner実行の保存結果と二重実行guard（2026-10-09）

Ownerから`Section14 initial sequence already attempted. Do not rerun.`の表示を受領した。保存済み`section14-owner-root-summary.json`には、21:09頃の先行実行の結果がある。今回表示されたguardはその結果の存在を検出したもので、guardを外して再実行する操作は行わない。

先行実行では同ユーザー・管理者token・branch／HEAD・source／artifact hash・Docker空baselineが成立。有限8例は全一致し、`section14-harness-eight-cases.json`に保存されている。起動eventの権限拒否は発生せず、既存JDKの`java -version`が実行された。harnessは0.366秒で`Owned PID reused; creation identity mismatch`により停止した。失敗callはlaunch event parent／creation照合、runner行76。root result／XMLは作成されず、root／PL2／package／Consumer／E2Eは未起動。

`section14-harness-probe-result.json`はsubscription残1を記録した。これはharness child PowerShell内の解除確認失敗であり、cleanup成立として扱わない。Owner手順はchild PowerShell終了後にDocker container0／owned process残0・既存Java保持・source hash不変を保存したが、その後の値でchildの残1記録を0へ置換しない。

source照合ではなくtmp監視の時刻比較と解除処理に不足がある。短命probe rootは.NET `Process.StartTime.Ticks`をseedにし、同一PIDのCIM `CreationDate`と全tick一致を要求している。WMIの[CIM_DATETIME公式仕様](https://learn.microsoft.com/en-us/windows/win32/wmisdk/cim-datetime)はmicrosecond精度を定めている。異なる時刻表現の端数差をPID再利用と判定する可能性がある。ただし失敗時の実PID・両ticksを保存していないため、今回の差の数値と実PID再利用の有無は未確定。エラー文字列だけでPID再利用を事実としない。

解除は登録commandの返値変数がtruthyの場合だけ行う構造で、実際のSourceIdentifier登録を独立に確認していない。Microsoftの[subscription管理例](https://devblogs.microsoft.com/scripting/use-powershell-to-monitor-for-process-startup/)は`Get-EventSubscriber`から登録を確認し`Unregister-Event`する手順を示す。実登録を確認して当該sourceだけを解除する設計へ訂正する必要がある。登録返値の実型は旧recordに保存されていない。

Owner台帳は累計1528秒、残2072秒（34分32秒）、cleanup300秒予約。Rules単独107 PASSを維持し、§14の「未確定PID／container・観測不能、test失敗・上限超・cleanup不成立で停止」に従い後続を停止する。既存result／guard／source／artifactを削除・上書きして再開しない。

## 15 tmp時刻identity・SourceIdentifier解除の有限訂正（OWNER APPROVED）

### 15.1 tmpだけの修正案

tracked code／test／pool／POM／依存、Windows権限・event providerは不変。既存の同ユーザーOwner管理者PowerShell方式を維持して、tmpの次の2点だけを訂正する。

1. **creation identityの供給元を区別する**：.NET seedはその型と元ticksを保存し、CIMとの最初の照合をWMIのmicrosecond精度へ明示的に揃えて行う。数秒の許容幅でcreation identity不一致を消さない。CIMで確認後の正式creation identityはCIM値で固定し、以降のCIM同士は厳密一致を維持する。真正の不一致時はPID・供給元・元ticks／照合後値・差をcredentialなしで保存して停止する。短命でCIM未確認のprocessを生存子の正式所有値に使わない。
2. **解除は登録返値に依存させない**：harnessと通常runnerのfinallyで、今回SourceIdentifierに一致する実subscriberを`Get-EventSubscriber`で確認し、そのsubscriberだけを`Unregister-Event`で解除する。今回sourceのqueueだけを除去し、残0を必須とする。他session・別sourceのsubscriptionを解除しない。元のfailureとcleanup failureをそれぞれ保存し、failureがあっても解除を省略しない。

元8例に、時刻精度の正規化成立／CIMでの真正identity不一致拒否／登録返値が空でも今回sourceだけを解除する判定の3例を足した有限11例を提案する。実containerなし、新Repository testなし。既存JDK短命launch1回・同event登録1回と合わせて全体60秒以内、launch10秒以内。実launchで元.NET／CIM値・identity判定とsubscription残0を保存し、未成立ならrootを開始しない。

### 15.2 新epoch・再開範囲と残予算

旧§14結果は保存したまま、新たな`section15-`のharness／Owner手順resultに分ける。既存guardを削除・無効化せず、新epochも各1回で二重実行を拒否する。tmp変更後のrunner／Owner手順hashを新環境manifestへ記録し、他1543入力のうち変更許可のないartifactは旧hashと照合する。予算台帳は同じものを継続し、初期化しない。

累計1528秒、残2072秒（34分32秒）、cleanup300秒予約・総60分、root client14／他8、JVM4／PG1／rootRyuk2・他1・その他上限を維持する。修正確認・manifest更新・gate実時間も残予算から差し引く。harness追加確認1回の成立後だけ、§14で未起動のroot1回を再開する。root成立時に未実行PL2候補4件／package・旧binary Consumer・既存E2Eへ進む。各phase上限と残総時間の小さい方を使う。

Ownerは本節を指定して「承認します」と明示した。tmp時刻identity・SourceIdentifier解除の訂正、新epochの有限11例／短命launch確認1回、成立時の未起動root1回と未実行後続集合への再開を承認済み。Rules単独追加、実PID再利用の追加探索、権限変更・別監視方式、新規依存取得、既存test／pool変更、commit／受入／R1／remoteは含まない。既存上限・残予算・Owner管理者terminal条件を維持し、さらなる監視／cleanup不成立で停止する。

### 15.3 承認後のtmp訂正・Owner実行手順

tmp runnerへ供給元付きcreation照合を適用した。.NET値は整数ticksの端数をmicrosecond境界へ揃え、最初のCIM値と照合する。CIMで確認後はCIM値・供給元を固定し、次のCIM比較は厳密一致。元.NET ticks／CIM ticks／正規化値／差／判定をroot identityへ保存する。生存instanceの正式所有台帳にはCIM確認値を登録する。起動eventの発生時刻照合幅とcreation identityの精度処理を混同しない。

解除は実subscriberを今回SourceIdentifierで選択し、当該SubscriptionIdだけを解除・queue除去する。harness／通常runnerのfinally双方で実行し、元failureと解除failureを別保存、before／after countを記録する。有限11例をprobeより先に保存し、実probe／rootとも残0を必須とする。Parser成立、実harnessはOwnerの管理者terminalでの1回だけとし、Agentから追加登録・Java起動をしていない。

旧Owner手順・旧§14の7 result／manifest fileはhash付きで保全し、`Start-Section15-Owner.ps1`を新規tmp手順として準備。全新resultは`section15-`、新harness／summaryの二重実行guardを維持する。旧1543 artifactのうち許可したrunnerだけの更新と、新Owner手順1件の追加で新manifest1544件。許可していないartifactのhash差分0を確認した。

manifest照合・作成1.039秒→2秒計上。累計1530秒、残2070秒（34分30秒）、cleanup300秒予約。`section15-expected-environment.json`／`section15-preparation.json`へ記録。tracked code／test／POM追加変更0、root・後続集合未起動、成果commit／remote0。

Ownerは先ほどと同じユーザーの**管理者PowerShell 7**で、次を1回だけ実行する。

```powershell
Set-Location 'C:\Users\kataoka\Desktop\KOIKI-JAVA\KOIKI-JAVAWEB'
& '.\tmp\r0-rules-9c41aa4-20261009-first\Start-Section15-Owner.ps1'
```

gate→有限11例／短命launch→成立時の未起動root1回→fresh XML・cleanup保全の順。Ownerから「完了」または「停止」の連絡後、Agentが新resultを読み、root成立時だけ既承認PL2／package／Consumer／E2Eへ進む。停止時は同commandを再実行しない。これは§15の承認済みOwner担当操作で、追加承認要求ではない。

### 15.4 Owner実行結果：11例・解除成立、実launch照合停止

Ownerのコマンド実行終了の連絡後、保存resultを確認した。続く提示outputの`Launch harness stopped: Finite launch probe failed`／`STOPPED: finite launch harness`も同recordと一致する。コマンドの終了と検証PASSを区別する。

同ユーザー／管理者token／source・artifact・Docker baselineのgateは成立。有限11例は全一致、`section15-harness-eleven-cases.json`へ保存。JDK java-versionも実行された。harnessは10.367秒で`Finite launch probe failed`停止し、root result／XMLは未作成。root／後続集合は未起動。

解除は実subscriber before1→after0、failureなし。Owner summaryのowned process残0／Docker container0、既存Java24852／10280保持・source hash不変も成立。§14のsubscription残1や今回のfailureをPASSへ置換しない。

失敗recordのRootIdentityはPID12240／parent24856、`Owned=false`／`CreationTicks=null`。既知rootのseedが、起動eventを処理した同PIDのrecordで上書きされている。現codeはroot eventを飛ばす条件にも`abs(event TIME_CREATED - root StartTime) <= 2秒`を要求し、その条件外ではparentが所有外のroot eventを通常の子と同じ分類へ通している。確認済みrootを「所有外」に戻す構造的不足を確認した。

当該event ticksはUTC12:24:53.7114558。probe出力はJST21:24:51に作成されている。ただし失敗recordが元seedを上書きしたため、元.NET／CIM seed ticksと当該差の正確な値は失われた。[Win32_ProcessStartTrace公式仕様](https://learn.microsoft.com/en-us/previous-versions/windows/desktop/krnlprov/win32-processstarttrace)の`TIME_CREATED`は**eventの生成時刻**であり、processのcreation identityと同一であるとの契約ではない。event生成とprocess開始を2秒以内とする前提は採用できない。処理遅延の詳細・実PID再利用・OS不具合は断定しない。

保存台帳累計1546秒、残2054秒（34分14秒）、cleanup300秒予約。§15のさらなる監視不成立停止に従い、既存guard／recordを消さず、同commandを再実行しない。次の§16はtmpのevent時刻の扱いとroot seed維持の有限訂正案。

## 16 tmp起動event時刻・既知root維持の有限訂正（OWNER APPROVED）

### 16.1 訂正対象と判定

同ユーザーOwner管理者PowerShell・同じWMI providerを維持し、tmp runner／Owner手順のみを対象とする。§15のmicrosecond初回照合、CIM確認後の厳密identity、今回SourceIdentifierだけの解除・残0は維持する。

1. **起動rootのidentityを独立保持**：Start-Processで得たPID／元.NET値、CIM確認値／供給元、起動元PID・process名をroot anchorに固定し、event用Latest tableの更新で上書きしない。rootの開始eventは、当該PID・起動元PID・process名の照合と、生存時のcanonical creation再確認を行って別recordへ保存する。root eventの生成時刻とcreationの2秒差を所有条件にしない。root event identity不一致・同PIDへの複数開始event等の曖昧さは拒否し、単にPIDが一致しただけで別instanceを所有としない。
2. **event生成時刻とcreationを分離**：`TIME_CREATED`はevent生成時刻としてraw値・変換UTC値・受信時刻を保存する。生存子の正式identityはCIM `CreationDate`・name／parent PIDと承認済み親子台帳を照合し、当該launch期間内のinstanceであることを確認する。`abs(CIM creation - event generation) <= 2秒`による正式登録や拒否をやめる。短命parentのevent履歴は親子追跡に保持するが、CIM未確認の値を生存子の正式creationへ代用しない。PID重複・generation曖昧・未確認log PIDは停止する。
3. **失敗時もanchor・event履歴を保存**：元root identity・実launch終了値・受信済みevent・parent chain・CIM照合結果を、probe成否にかかわらず保存する。固定anchorと派生Latestの値を分ける。今回失われた元ticksを復元したように記録せず、新epochで実値を採取する。

Repository code／test／pool／POM／依存、Windows権限／WMI ACLの変更は含めない。時刻条件を無制限に緩めて未知PIDを所有とする訂正でもない。

### 16.2 有限確認・未起動集合の再開

既存11例に「生成時刻がcreationより遅いroot eventでも確認済みanchorを維持」「遅いeventでも生存子のCIM identityを独立照合」「rootの真正identity不一致を拒否」の3例を加え、実containerなし有限14例を提案する。既存JDK短命launch1回・同event登録1回と合わせて全体60秒以内／launch10秒以内。14例・実launchと親子／canonical creation・anchor維持・解除残0の全成立後だけ、既承認で未起動のroot1回を再開する。root成立時に限り未実行PL2／package／旧Consumer／既存E2Eへ進む。

旧§14／15のrecordとguardは保全し、新epoch`section16-`でも各1回のguardを設ける。予算台帳・snapshotは同じものを使用する。変更するtmp runner／新Owner手順のhashだけを更新許可し、それ以外の旧manifest inputは不変を照合する。

累計1546秒、残2054秒（34分14秒）、cleanup300秒予約・総60分を維持。準備確認・harness・gate実時間もこの残時間から引く。root client14／他8、JVM4／PG1／rootRyuk2・他1、件数・容量・source／artifact境界、各phase上限と停止条件は不変。Rules単独追加、診断反復・実PID再利用の探索、別event方式／権限変更、依存取得、commit／受入／R1／remoteを含まない。

Ownerは本節を指定して「確認、承認いたします」と明示した。tmpのroot anchor保持・event生成時刻とCIM creationの分離・成否を問わない元identity／履歴保存、新epochの有限14例／短命launch確認1回、成立時の未起動root1回と後続未実行集合への継続を承認済み。旧guard／記録・snapshot・残予算・他上限とOwner管理者terminal条件を維持し、さらなる不成立で停止する。新規依存取得・OS権限変更・成果commit／受入／R1／remoteを含まない。

### 16.3 承認後のtmp訂正・Owner実行準備

root anchorをevent用Latestから独立させ、root開始eventはPID・name／parent PID・生存時のcanonical identityを照合して別保存する。root eventの2秒条件と、そのeventによるanchor上書きを除去した。生存子はname／parent PID・当該launch期間・CIM creationを独立照合し、event生成時刻をcreation identityへ代用しない。元11例＋承認済み3例の有限14例、根拠の曖昧な重複PID／root eventの拒否を維持する。

event raw FILETIME／生成UTC／受信UTC、固定anchorの元.NET値・CIM確認値／供給元、派生identity、受信event／親子履歴、実launch終了値を成否どちらでも保存する。時刻精度の初回正規化・CIM厳密比較、今回SourceIdentifierだけの解除／before-after count／残0を維持した。

`Start-Section16-Owner.ps1`を新epochで準備。gate→14例・実launch1回→成立時の未起動root1回→fresh XML／cleanup保存の順。`ROOT_SEQUENCE_COMPLETE`はcleanup保存・failureなしの確認後だけ表示する。旧§14／15の15 fileをhash付きで保全、旧1544 artifact中の許可したrunner更新以外はhash不変、新Owner手順1件追加で新manifest1545件。script parserとdiff checkは成立、Agentからevent／Java／root／container起動なし。

manifest照合・作成2秒＋最終script identity／parser確認1秒を計上し、累計1549秒、残2051秒（34分11秒）、cleanup300秒予約。予算台帳・source snapshot・上限は維持する。Ownerは先ほどと同じユーザーの**管理者PowerShell 7**で次を1回だけ実行する。

```powershell
Set-Location 'C:\Users\kataoka\Desktop\KOIKI-JAVA\KOIKI-JAVAWEB'
& '.\tmp\r0-rules-9c41aa4-20261009-first\Start-Section16-Owner.ps1'
```

`ROOT_SEQUENCE_COMPLETE`または`STOPPED`の表示をOwnerから知らせる。Agentは保存resultを確認し、root成立時だけ既承認後続集合へ進む。停止時は同commandを再実行しない。管理者terminalの起動・実行は既承認のOwner担当条件による。

### 16.4 Owner実行結果：harness成立・通常rootの所有外PID履歴衝突

Ownerが管理者PowerShellで実行し、gate成立、有限14例全PASS・java-version probe終了0を確認した。probeは2.622秒、元.NET ticksからCIMへの初回正規化は差-8 ticksで一致、root anchor保持・event履歴保存・subscription before1→after0が成立した。通常rootは7.433秒で`Ambiguous duplicate launch PID=26068`として停止。root完了・fresh XML・予定skip92件照合は未成立。PL2／package／旧Consumer／E2Eは未起動。

受信履歴には同PID26068について、先行`git.exe`／parent10272（生成UTC12:50:16.6124209）と、後続`cmd.exe`／parent20768（生成UTC12:50:22.6333987）の2開始eventがある。後続parent20768は今回root14400のMaven Java。先行eventは所有外でLatest tableへ保存され、後続の所有対象eventが来た際に`Latest.ContainsKey(id)`だけで停止した。判定は先行recordのOwned／name／parentと、別経路のCIM所有台帳を区別していない。

後続event受信前のCIM親子poll経路ではPID26068をcreation ticks `639271470206878650`でOwnedProcessesへ登録済み。開始eventの重複配信を確認した事実ではなく、異なるname／parentの開始eventが同PIDで記録されたことと、監視tableの衝突判定を確認した。先行gitのCIM creation／終了実値、および停止瞬間の後続CIM name／parentは保存されていないため、完全な2世代identity・終了順の実証として扱わない。

所有process残0・running container0・subscription残0、既存Java24852／10280保持、source hash不変をOwner summaryで確認。root sample最大JVM2／PG0／Ryuk0／client0は途中sampleだけであり、全期間の資源成立へ昇格しない。Agentの保存照合でもsnapshot1156 file・元code hash、manifest1545 artifact hashは不変、今回rootのfresh Surefire XMLは0。残っている旧XMLは今回root結果へ加算しない。

`section16-stop-preservation.json`に旧§16 recordのSHA・source／artifact照合・fresh XML0を保存し、保全2秒計上。累計1569秒、残2031秒（33分51秒）、cleanup300秒予約。§16のさらなる不成立停止条件に従い、同command再実行・tmp訂正・追加launch／root／container操作を実施していない。Rules単独107 PASSと通常root未成立を維持する。

## 17 tmp所有外PID履歴と確認済み所有identityの統合・有限訂正（OWNER APPROVED）

### 17.1 対象と許可条件

tmp `Run-Bounded.ps1`の子開始eventとCIM親子pollの統合、および新epochのOwner手順だけを提案する。通常rootのcode／test／pool／POM／依存は変更しない。root anchor、CIM厳密creation照合、event生成時刻とcreationの分離、SourceIdentifier限定解除を維持する。

所有外の先行Latest recordが存在する場合、単にPIDが一致しただけで停止／許可する判定を改め、次の**全条件成立時だけ**新しい所有event recordへ移行する。

1. 先行recordはOwned=false。後続eventは生成時刻順が新しく、nameまたはparent PIDが先行と異なる。同じname／parentで世代を区別できない場合、既所有recordの重複、rootの複数開始eventは引き続き拒否する。
2. 後続PIDの生存CIM instanceが1件あり、eventのname／parentと一致し、creationは当該launch期間内。CIM親子pollが既に確認したOwned台帳の同PID creationと厳密一致する。未確認PID・creation不一致・CIM未捕捉の場合は停止する。
3. 後続parentも今回所有の生存CIM instanceとして1件確認し、親record／Owned台帳のcanonical creation・name／parentと厳密一致する。子creationは親creation以降。親の再利用／消失／不一致では停止し、PIDだけの親子関係を採用しない。
4. 先行所有外recordは履歴として保全し、既存受信eventも残す。過去の所有外子を後から所有へ変更せず、今回照合した後続instanceだけを新recordとして登録する。

これにより「所有外event履歴」と「CIM確認済みの今回所有instance」を統合する。先行instanceの実終了を推測して正当化する案ではない。任意の所有済みPID再利用の受容、同identityへの複数開始eventの受容、CIM未確認creationの代用は含めない。

判定成否にかかわらず、先行record・後続event・対象子／親のCIM name／parent／creation・既存Owned値・各照合結果を有限量保存する。CommandLine／credentialを含めない。停止直前の比較値が保存されない現在の不足も、この判定箇所だけで訂正する。

### 17.2 有限確認・root追加1回と残予算

元14例を維持し、次の8例を加えた有限22例を提案する：所有外の異なるname／parentから確認済み所有instanceへの移行、先行所有外履歴とその子の非昇格、既所有record重複拒否、子creation不一致拒否、子CIM未捕捉拒否、親creation不一致拒否、子name／parent不一致拒否、先行と同name／parentの曖昧な世代拒否。実containerなし、既存JDK短命launch1回・同event登録1回と合わせて全体60秒以内／launch10秒以内とする。

有限22例・実launch・anchor／canonical identity・解除残0が全成立した場合だけ、**通常root追加1回**を提案する。§16のrootは起動済みなので、未起動分の流用とは記載しない。root900秒以内／400 invocation以内、成立時だけ未実行PL24件・専用package／旧Consumer／既存E2Eへ進む。Rules単独追加検証は行わない。さらなる不成立で停止し、反復を自動許可しない。

新epoch `section17-`に各1回のguardを設け、旧§14〜16のguard／record／hashを保全する。snapshotと予算台帳は同じものを使用し、runner更新・新Owner手順のhash差分だけを許可候補とする。他manifest input・sourceは不変を照合する。同ユーザーOwner管理者PowerShellでの実行を維持し、Agentの自動昇格・OS／WMI権限変更・別providerを含めない。

現在累計1569秒、残2031秒（33分51秒）、cleanup300秒予約・総60分は不変。準備・gate・harness・実検証・保存cleanupも残予算へ計上する。root client14／Ryuk2、他phase client8／Ryuk1、JVM4／PG1、容量・件数・source／artifact・各phase上限を維持する。残時間不足時は開始せず停止する。新規依存取得、成果commit／結果受入／R1／remoteを含めない。

Ownerは本節を確認し「承認します」と明示した。tmp限定訂正・有限22例／短命launch1回・成立時の通常root追加1回、root成立時の未実行後続集合への継続を承認済み。旧記録・上限・停止条件・残予算・Owner管理者terminal条件を維持する。同じ承認を再要求せず、本節内の訂正と手順準備へ進む。

### 17.3 承認後の訂正・Owner実行準備

tmp runnerに所有外履歴から所有instanceへの照合を追加した。先行Owned=false／event順／異なるnameまたはparent、子の生存CIM1件・name／parent・既存Owned creation厳密一致・launch期間、親の生存CIM1件・canonical identity厳密一致、子creationが親以降、の全条件が成立した場合だけ移行する。先行所有外recordを履歴へ保存し、その子を昇格しない。既所有重複・未確認／不一致・同signatureの曖昧な世代は拒否する。比較値は成功／失敗とも対象子・親のname／parent／creationと既存台帳値・各条件だけを保存し、CommandLine／credentialを含めない。

元14例＋承認済み8例をOwner実行時の有限22例として準備。有限例の比較証拠と実launch／rootの比較証拠を別保存する。Agentから有限例・event登録・Java／root／containerを実行していない。旧runnerへの許可済み更新以外の1545 inputはhash不変、新Owner手順1件追加で1546件。旧§14〜16の30 recordをSHA付き保全し、新Owner gateでも照合する。parser／diff check成立。

新epoch `Start-Section17-Owner.ps1`はgate→有限22例・短命launch1回→成立時の通常root追加1回→fresh XML／cleanup保存の順で、後続未実行集合はroot証拠確認後に進む。各stageの二重実行guard・旧guard／記録・snapshot・他上限は維持する。準備確認2秒を計上し、累計1571秒、残2029秒（33分49秒）、cleanup300秒予約。

Ownerは先ほどと同じユーザーの**管理者PowerShell 7**、Repository rootにいる状態で、次の**1行だけ**を1回実行する。

```powershell
& '.\tmp\r0-rules-9c41aa4-20261009-first\Start-Section17-Owner.ps1'
```

`ROOT_SEQUENCE_COMPLETE`または`STOPPED`の表示を知らせる。停止時は同commandを再実行しない。これは既承認のOwner担当による管理者terminal実行であり、新しい承認要求ではない。

### 17.4 Owner実行結果：有限22例成立・子CIM未捕捉による停止

同ユーザー／管理者token・source／artifact gate成立。有限22例全一致、java-version probe終了0／2.942秒・subscription残0。通常rootは10.661秒でPID28576の衝突照合に失敗し停止した。比較値は`section17-root-transition-comparisons.json`に保存され、子CIM1件／name-parent／既知creation／launch期間／親以降creationの5条件がfalse。親Java19260のcanonical creation `639271476353969990`は一致した。

先行recordは所有外git.exe／parent10272（生成UTC13:00:38.6430941）、後続eventはcmd.exe／parent19260（UTC13:00:43.6660868、受信UTC13:00:45.6293820）。後続子の生存CIMは0件、Owned台帳の既知creationはnull。§16の「CIM親子poll登録済みなのにLatest衝突だけで拒否」とは条件が異なる。今回は§17が明示したCIM未捕捉拒否の指定どおりであり、子が終了済み・実PID再利用・event重複であると断定しない。

Owner cleanupはprocess残0／container残0・既存Java24852／10280保持・source不変、root subscription残0。途中sample5件の最大JVM2／PG0／Ryuk0／client0は全期間保証ではない。今回のfresh部分XML2 file／4件、failure／error／skip0を`section17-root-partial-xml/`へSHA付き保存した。通常root全体・Rulesの今回root内検証・予定skip92件照合は未成立。旧Rules単独107 PASSを維持し、旧XMLと混算しない。PL2／package／旧Consumer／E2E未起動。

snapshot1156 file・元code hash、1546 artifact・旧記録30 fileのhash不変を再確認し、`section17-stop-preservation.json`へ保存。保全2秒を計上して累計1594秒、残2006秒（33分26秒）、cleanup300秒予約。§17停止条件に従い再実行・追加訂正・launch／container操作を行っていない。

## 18 tmp短命shellのevent系譜と生存identity分離・有限訂正（OWNER APPROVED）

### 18.1 先に見直す監視契約

§17はCIM未捕捉を一律拒否するため、短命shellの開始eventだけが届く経路を再開できない。生存processの正式identityを弱めて通すのではなく、次の2つを明示的に分けるtmp訂正を提案する。

- **開始eventの系譜record**：PID・name／parent・raw event生成値・受信値・親generationを保持する。短命shellのlaunch関係を追跡するための記録で、CIM creationや終了証明ではない。
- **生存processの正式Owned台帳**：CIMのPID／name／parent／canonical creationを照合して登録する。資源sample・生存identity確認・残process確認にはこちらを使用し、event時刻をcreationへ代用しない。

先行所有外recordとの衝突時、§17の全条件を満たす生存instanceの移行は維持する。追加する例外は**後続cmd.exeの生存CIM0件・既知Owned creationなしの場合だけ**のevent系譜recordであり、次の全条件を要求する。

1. 先行は所有外で、新しいeventのraw生成順が後、nameまたはparentが異なる。同signature・既所有／既系譜recordとの重複、root重複は引き続き停止する。
2. 後続parentは生存CIM1件の今回所有processで、親record／正式Owned台帳とname／parent／canonical creationが厳密一致する。parentが未捕捉／再利用／不一致の場合は停止する。
3. 新recordを`TraceOnly`・creation未確認として保持し、正式Owned台帳へPIDを追加しない。終了済みと推測せず、「開始eventあり・当該sampleではCIM未捕捉」と記録する。PID／raw event生成値によるgenerationと親generationを固定し、旧所有外record／旧子を昇格しない。
4. 後続子の追跡には、曖昧さのないgeneration chainだけを使用する。子が生存時はその子自身のCIM name／parent／canonical creation・launch期間を照合する。短命parentのevent時刻を親または子のcreationへ代用しない。世代を一意に結べない場合、既知creationとの不一致、未説明log PIDは停止する。

この衝突例外をjava.exe／javaw.exe／javac.exe／未知名へ適用しない。生存CIMが存在するが不一致の場合もTraceOnlyへ降格して通さない。TraceOnly PIDをkill対象にしない。既存の今回launch rootと実descendantsだけのcleanup・subscription残0は維持する。短命parentのCIM creation／終了実値はUNKNOWNとして残し、正式identityの確認に成功したようには記録しない。資源sampleの限界も保持する。

受信eventはbatch全体を分類前に保存する。現在はRead-LaunchEventsがbatchを取り出した後、分類中のthrowまで処理したeventだけをReceivedへ保存するため、失敗後の同batch未処理分を保存できない。旧欠落を復元したとは記録せず、新epochでbatch全件・判定対象・未処理状態を保全する。

### 18.2 有限確認・再開と予算

元22例のうち「子CIM未捕捉は拒否」の期待だけを、上記のcmd.exe／確認済み生存parent／新generation条件下では「TraceOnly、正式登録なし」へ明示訂正する。他の拒否条件を維持する。次の6例を追加した有限28例を提案する：TraceOnlyが正式Ownedへ入らない、短命parent系譜経由の生存子の独立CIM照合、後着CIM不一致拒否、parent CIM未捕捉拒否、Java関連の衝突CIM未捕捉拒否、分類失敗時も受信batch全件保存。旧所有外子の非昇格・既所有重複・親creation不一致等は元集合で維持する。

同ユーザーOwner管理者PowerShellで、実containerなし有限28例＋既存JDK短命launch1回・同event登録1回を全体60秒以内／launch10秒以内で確認する。全成立時だけ**通常root追加1回**を提案する。root900秒以内／400 invocation以内、成立時だけ未実行PL2候補4件／package／旧Consumer／既存E2Eへ進む。有限例または実検証がさらに不成立なら停止し、次の追加実行を自動許可しない。

新epoch `section18-`の各1回guard、旧§14〜17のrecord／guard／hash保全、同snapshot・予算台帳を使用する。変更対象はtmp runner／新Owner手順だけ。Repository code／test／pool／POM／依存、OS／WMI権限・別event providerを変更しない。Rules単独の追加実行・新規依存取得・commit／結果受入／R1／remoteを含めない。

累計1594秒、残2006秒（33分26秒）、cleanup300秒予約・総60分を維持する。準備・gate・harness・実検証・保存も残予算へ計上し、残時間により実効上限を短縮する。root client14／Ryuk2、他client8／Ryuk1、JVM4／PG1、容量・件数・各phase上限を維持する。残時間不足時は開始しない。

Ownerは本節を指定して「承認します」と明示した。§17とは別に、上記の監視契約訂正・有限28例／短命launch1回・成立時の通常root追加1回と、root成立後の未実行集合への継続を承認済み。旧guard／記録・source／artifact・上限・残予算・Owner管理者terminal条件を維持し、さらなる不成立で停止する。新規依存取得・OS／WMI権限変更・成果commit／結果受入／R1／remoteを含めない。同じ承認を再要求せず、本節内のtmp訂正と具体的実行手順の準備へ進む。

### 18.3 承認後のtmp訂正・Owner実行準備

tmp runnerに、CIM未捕捉・既知creationなしのcmd.exeだけを対象とするTraceOnly衝突移行を追加した。先行所有外／新event順／異なるsignature、確認済み生存parentの厳密identity一致を要求し、正式Owned台帳には登録しない。generationと親generation・creation未確認・終了UNKNOWNを保持する。生存時の子はCIMで独立照合し、既知creation不一致・親generationの曖昧さで停止する。event recordの既存Owned欄は系譜所属を表し、別の正式Owned台帳・CimConfirmed／TraceOnlyと区別する。Java関連の衝突や、生存CIM不一致への例外は追加していない。

受信batchは全件を分類前に保存し、Pending／Classifying／Classifiedで未処理状態を記録する。有限28例は旧22例の子未捕捉期待だけを承認どおり変更し、承認済み6例を追加した。旧記録の欠落を復元したように記録せず、新epochの実値で確認する。Agentから有限例・event登録・Java／root／container起動は行っていない。

`Start-Section18-Owner.ps1`をgate→有限28例／短命launch1回→成立時の通常root追加1回→fresh XML／cleanup保存の順で準備した。許可されたrunner更新以外の旧1546 inputはhash不変、新Owner手順1件追加でmanifest1547件。旧§14〜17のrecord／部分XML49 fileをSHA付き保全し、新Owner gateで照合する。parser／diff check成立、各1回guard・source snapshot・旧guardと上限は不変。準備確認2秒を計上し、累計1596秒、残2004秒（33分24秒）、cleanup300秒予約。

Ownerは同じユーザーの**管理者PowerShell 7**、Repository rootにいる状態で、次の**1行だけ**を1回実行する。

```powershell
& '.\tmp\r0-rules-9c41aa4-20261009-first\Start-Section18-Owner.ps1'
```

`ROOT_SEQUENCE_COMPLETE`または`STOPPED`の表示を知らせる。停止時は同commandを再実行しない。これは承認済みのOwner担当による実行手順であり、同じ承認を再要求するものではない。root証拠確認後だけ既承認の未実行後続集合へ進む。

### 18.4 Owner実行結果：有限28例成立・旧event recordと生存CIMの照合停止

gate成立、有限28例全一致、java-version probe終了0／2.916秒・subscription残0。通常rootは8.822秒で`Live launch name/parent mismatch: PID=4784`停止。所有process残0／container残0・既存Java24852／10280保持・source不変、root subscription残0を確認した。PL2／package／旧Consumer／E2E未起動。

PID4784のeventはcmd.exe／parent20960、生成UTC13:13:20.5302249・受信13:13:20.9338246。event recordは分類済みで、creation null／CimConfirmed=false／TraceOnly=false。正式Owned ticks表には同PIDのCIM creation `639271483986047520`が残っている。子conhost4248／javac30288のeventも分類済み。受信190 eventはすべてClassified、途中sample4件の最大JVM2／PG0／Ryuk0／client0。sampleを全期間保証へ昇格しない。

今回は§17の所有外Latest衝突でも、§18のTraceOnly衝突例外でもなく、旧event recordのname／parentと、その後の生存CIM一覧との照合で停止した。実装は生存一覧の各PIDについてLatestの系譜所属recordを引いてConfirm-LiveLaunchIdentityへ渡し、既知canonical creationとの世代分類前にname／parent不一致でthrowする。正式ticks表とevent recordは別経路で登録されるが、正式CIM観測時のname／parentや停止時のCIM側実値が保存されていない。現在recordを別instanceとして除外すべきだった可能性はあるものの、実PID再利用・currentのname／parent／creation・旧instanceの終了を断定しない。

移行比較fileは今回未作成。衝突移行の比較経路と、一般の生存identity照合経路の診断保存が別で、後者の原比較値が欠ける。旧欠落値の復元や、今回の失敗を単純な「PID再利用」とする記録は行わない。

fresh部分XML2 file／4件（failure／error／skip0）を`section18-root-partial-xml/`へSHA付き保存。root全体／Rulesの今回root内結果／予定skip92照合は未成立、旧Rules単独107 PASSと混算しない。snapshot1156 file／元code hash、1547 artifact／旧記録49 fileのhash不変を確認し、`section18-stop-preservation.json`へ保全。保全2秒計上で累計1617秒、残1983秒（33分03秒）、cleanup300秒予約。停止条件に従い追加訂正・root再実行なし。

## 19 tmp監視のPID世代・生存照合・診断保存の有限再整理（OWNER APPROVED、root追加0回）

### 19.1 root再実行より先に固定する契約

個別の停止messageごとにrootを再実行する進め方を止め、CIM親子poll→event→次の生存一覧という観測順を含む世代契約を、tmp内の有限fixtureで先に確認する案とする。§18のTraceOnly限定条件・root anchor・厳密identity・generation chain・batch保全は維持する。

1. **canonical観測を完全なrecordで保管**：CIM親子pollおよび生存event照合で正式登録した時点のPID／name／parent PID／canonical creation・観測元／時刻を、PIDとcreationによるinstance keyで保持する。既存ticks表は資源sample・log照合・cleanupとの互換のため残すが、正式instance情報の代替にしない。event系譜とcanonical記録の対応は一意に確認できる場合だけ結び、PIDだけ・name／parentだけで過去creationをeventへ移植しない。曖昧なら停止する。
2. **生存一覧を世代分類してから照合**：current CIMが記録済みinstanceのcanonical creationと同じ場合、name／parentも厳密一致を要求する。同creationで不一致なら停止する。別creationのcurrentは旧instanceと同一として扱わず、旧instanceの履歴・currentの別instance情報を保存し、旧instanceの権限でcurrentを所有／killしない。root anchorのinstance変更は停止する。既知canonicalのないTraceOnlyはこの除外を流用せず、後着不一致では停止する。
3. **新parent generationの取り扱い**：所有外current instanceや、その開始eventが観測された場合に旧parent系譜を継承させない。既存子の履歴と新instanceの子を区別し、世代を一意に結べないevent／未説明log PIDは停止する。CIM未捕捉から終了済みを推測せず、現在一覧からの不在と、別creationの観測を分けて記録する。
4. **すべてのidentity照合で比較値保存**：root・生存子・衝突・後着CIMの各経路で、throw前に期待instance／実CIMのPID・name／parent／creation、既知canonical、generation・判定条件・failure位置を保存する。空の比較集合もJSON `[]`として保存し、未保存／空集合を区別する。CommandLine／credentialを含めず、旧欠落実値を復元したとは記録しない。

変更対象はtmp runnerとfixture専用入口だけ。Repository code／test／pool／POM／依存、OS／WMI権限・event providerは変更しない。正式Ownedとして未知PIDを登録する許容範囲を広げず、currentが別instanceであると確認できない場合は停止する。有限集合は監視全体の完全網羅や通常root成立の証明ではない。

### 19.2 有限fixtureだけの確認と次の判断

既存28例を維持し、6例を追加した有限34例を提案する：CIM先行・event受信時未捕捉の観測順で完全なcanonical履歴を保持、記録済み子とは別creationのcurrentを旧所有へ昇格しない、同creationのname／parent矛盾拒否、root canonical変更拒否、所有外の新parent instanceの子への旧系譜継承拒否、一般照合failureの実値と空比較JSON保存。

確認は**synthetic dataを使う純粋fixture1回・60秒以内**に限定する。Agentが通常PowerShellで実行できる入口を準備し、実CIM query／event登録／Java・shell・Maven／container／browserを起動しない。旧guard／record・hash、snapshot・予算台帳を維持し、新epoch `section19-`へ記録する。failure／scope拡張／予算不足では停止し、fixture反復を自動許可しない。

**この案では短命launch・通常root追加・後続検証は0回**。有限34例と契約差分・残リスクの結果をOwnerへ示し、その後の実環境probe／root再開は別の具体的判断を経る。§18の失敗したroot開始枠を流用しない。

累計1617秒、残1983秒（33分03秒）、cleanup300秒予約・総60分を維持する。準備・fixture・証拠保存の実時間も計上する。JVM／DB／接続・Ryuk・件数・容量・各phase上限は変更しない。新規依存取得・成果commit／結果受入／R1／remoteを含めない。

Ownerは本節を確認し「承認いたします」と明示した。上記tmp契約の訂正・診断保存と有限34例1回を承認済み。実root／短命launchの再実行承認ではなく、結果提示後の実環境再開は別判断とする。旧guard／記録・上限・停止条件・残予算を維持し、同じ承認を再要求しない。

### 19.3 承認後のtmp訂正・純粋fixture1回の結果

tmp runnerにcanonical instanceのPID／name／parent／creation・観測元／時刻の保存、eventへの一意な結び付け、creationによる世代分類、生存identity照合の比較値保存を追加した。別creationのcurrentには旧所有を継承させず、旧履歴を保持する。同creationのname／parent矛盾、root変更、既知canonicalのないTraceOnly後着不一致、parent generationの曖昧さは拒否する。空集合も明示JSON `[]`で保存する。

`PureGenerationSelfTest`入口と`Run-Section19-Pure.ps1`を準備した。snapshot／source・artifact／旧記録hash gateを前後に行い、実CIM・event・Start-Process・Java／Docker等のcommandはfixture内で呼ばれた時点で拒否する検査を置いた。旧1547 inputは許可済みrunner更新以外hash不変、新手順1件追加でmanifest1548件、旧記録／部分XML67 fileを保全。parser／diff check成立、準備2秒を計上した。

承認済み純粋fixture1回をAgent通常PowerShellで実行。34例の生成・比較は0.381秒、**33一致・1不一致**で停止。追加6例は全一致したが、全体PASSへ昇格しない。不一致は旧例`Unowned transition batch-failure`で、batch全件の保存自体ではなく「先頭がClassifying／後続がPending」の状態期待が不成立。外部操作の呼出attempt0、実CIM query／event登録／fixture process・Java／Maven／container起動／root実行0、probe出力なし。

原因はAgentのfixture準備におけるcanonical台帳の例間分離不足。旧例はPID401を再利用するsynthetic dataで、各例のLatest／Owned／historyは作り直していたが、新設canonical台帳を共有していた。先行例のcmd.exe／parent400／creation `639271368002000000`が残る一方、batch-failureの先行recordはOwned=trueのgit.exe／parent999で、generation未設定。保存された`canonical-event-binding`診断のCandidateCount0により、分類前の生存世代点検で拒否された。このためbatchの先頭もPendingのままとなった。意図した「分類中の重複拒否」へ到達していない。別の旧否定例も意図外の早期拒否経路を通った可能性があり、33一致を全要求成立として扱わない。

停止時の比較実値・canonical記録・空JSONは保存され、今回の原因は既存file／sourceの読取照合だけで確認した。一般監視の同creation矛盾拒否や別instance除外を緩める根拠ではない。新6例の一致はsynthetic dataの限定結果で、実rootや完全網羅の成立ではない。

`section19-pure-summary.json`に停止・外部操作attempt0・RootStarted=false、`section19-stop-preservation.json`に34例33一致／1不一致・旧record SHA・snapshot1156 file／元code hash・1548 artifact／旧記録hash不変を保存。準備2秒＋gate／fixture／保存5秒＋保全2秒で、累計1626秒、残1974秒（32分54秒）、cleanup300秒予約。§19の不成立停止条件で追加訂正・fixture再実行なし。通常root未成立・旧Rules単独107 PASSを維持、tracked code／test／POM変更・commit／remote0。

## 20 tmp純粋fixtureのcanonical台帳分離・限定再確認（OWNER APPROVED、root追加0回）

変更候補はtmp runnerの**synthetic scenario開始時にcanonical台帳を新規化する1箇所**と、pure実行時のfailure Callラベルを34例へ揃える箇所だけ。各例内の複数観測は同じ台帳を使用し、観測順・親子・世代の検査を維持する。実監視中のcanonical台帳を消去する処理は追加しない。判定条件・既存28例／追加6例の期待値・34件を変更せず、不一致を期待値変更で通さない。

旧§19 guard／recordを保全し、新epoch `section20-`で同じ純粋fixture34例を**追加1回・60秒以内**だけ確認する。新手順のhashを追加し、runnerの上記変更だけを許可候補とする。他input・旧記録・sourceはhash不変を照合する。外部操作拒否検査、成否どちらの比較記録／空JSON保存、Agent通常PowerShellでの実行を維持する。

成功時も確認対象はpure fixtureだけ。**実CIM query・event登録・短命launch・root追加・後続検証は0回**。結果・意図した拒否位置・残リスクを提示し、実環境probe／root再開は別判断とする。不一致・外部操作attempt・hash差分・時間超過で停止し、反復を自動許可しない。

累計1626秒、残1974秒（32分54秒）、cleanup300秒予約・総60分と他上限を維持する。準備・fixture・保全の実時間も計上する。Repository code／test／pool／POM／依存・OS権限・event provider・新規依存取得・成果commit／結果受入／R1／remoteを含めない。

Ownerは本節を確認し「承認します」と明示した（2026-10-10）。上記fixture台帳分離・failureラベルの最小訂正とpure34例追加1回を承認済み。実環境probe／root追加は0回のまま維持し、旧guard／記録・上限・停止条件・残予算に従って実施する。

### 20.1 承認後の訂正・純粋34例の結果（2026-10-10）

tmp runnerのsynthetic scenario開始時1箇所へcanonical台帳の新規化を追加し、pure failure Callラベルを34例へ揃えた。各例内の観測順と共有台帳は維持し、実監視側の台帳・判定条件・期待値を変更していない。旧1548 inputは許可したrunner更新以外hash不変、新pure手順追加でmanifest1549件、旧記録／部分XML77 fileをSHA付き保全。parser／diff check成立、準備2秒計上。

新epoch `section20-pure`で承認済み追加1回を実行し、**34例全一致／0不一致、0.513秒、pure fixture PASS**。旧batch-failureは「先頭Classifying／後続Pending」の状態を含めて一致。旧owned-duplicateとbatch-failureの移行比較はともにPriorUnowned=false／ChildCimUnique=true／Accepted=falseで、意図した既系譜重複拒否へ到達した。前回の意図外canonical-event-binding早期拒否は0。一般照合の比較値保存・空JSON `[]`も成立した。

source／snapshot1156 file・artifact1549件・旧記録のhash gateは実行前後とも成立、Failure=null。外部操作attempt0、実CIM query／event登録／fixture process・Java／Maven／container起動／root実行0。実probe出力なし。`section20-pure-summary.json`、34例JSON、identity／transition比較記録、canonical記録、`section20-result-preservation.json`へSHA付き保全。

準備2秒＋gate／fixture／保存5秒＋結果保全1秒を計上し、累計1634秒、残1966秒（32分46秒）、cleanup300秒予約。日付変更で予算をリセットしていない。tracked code／test／POM変更・commit／remote0、旧guard／記録を維持。§20で許可された訂正・pure追加1回は完了した。

このPASSはsynthetic監視契約の限定結果で、実OSのevent遅延／短命process／PID世代・全期間資源上限・通常root成立の証明ではない。過去PID4784のcurrent実値は復元できず、CIM先行／event後着の実経路、同PID新instanceや親generationの曖昧さ、実fixtureのcleanupは再開時の観測対象として残る。旧Rules単独107 PASS、通常root／予定skip92照合未成立、PL2／package／旧Consumer／E2E未実行・結果受入別判断を維持する。

## 21 pure成立後の実環境probe・通常root追加1回の再開（OWNER APPROVED）

pure34例の成立を入力とし、現在のtmp監視訂正を実環境で確認する限定再開案。§20は実launch／root0回の承認であり、本節を承認前に起動しない。

1. 同ユーザーOwner管理者PowerShell 7で新epoch `section21-`のgateを確認する。branch／HEAD `e8c81fe`・snapshot1156 file／元code・artifact／旧記録・pure34例の同runner epochとの整合、SID／token、保護対象Java identity、Docker空baseline、memory／disk／容量・残予算を照合する。端末状態の変化を旧証拠から推測せず、不一致で停止する。旧guard・recordを削除しない。
2. 同じ既存Win32_ProcessStartTrace providerで、現行runnerの有限28例と既存JDK短命java-version launch1回を全体60秒以内／実launch10秒以内で確認する。pure34例の再実行は追加せず、その保存結果をgate入力とする。canonical／event／世代分類・一般照合比較・batch・解除残0を保存し、全成立時だけ次へ進む。
3. **通常root追加1回**を900秒以内／400 invocation以内で提案する。client14／Ryuk2、JVM4／PG1、source／artifact・容量・cleanup・停止条件を維持する。新規canonical観測値と失敗位置を保存し、成否どちらでも今回process／container／subscription残・既存Java保持を照合する。root XMLは今回fresh分だけを保存し、予定無効92件（S1の7 class／64件＋B2の3 class／28件）を照合する。旧XML・Rules107を加算しない。
4. root成立時だけ、未実行PL2候補4件（300秒以内）、専用package／再compileしない旧Consumer／既存package E2E（合計600秒以内）へ進む。各段階で残予算とcleanup予約を優先して実効上限を短縮する。他phaseのclient8／Ryuk1等の既存上限と集合を維持する。残時間不足・failure／UNKNOWN・hash差分・上限超・cleanup不成立では停止し、追加訂正・反復を自動で先行しない。

現在累計1634秒、残1966秒（32分46秒）、cleanup300秒予約・総60分を維持し、準備・gate・harness・root・後続検証・保全を計上する。対象はtmp runnerを変更せず新Owner手順／manifest・新epoch証拠を準備して使う範囲。Repository code／test／pool／POM／依存、OS／WMI権限・providerを変更しない。新規依存取得、Rules単独追加、成果commit／R0-C結果受入／R1／remoteを含めない。

Ownerは本節を確認し「承認します」と明示した（2026-10-10）。上記gate・有限28例／実短命launch1回と、全成立時の通常root追加1回・root成立時の未実行後続集合への継続を承認済み。pure結果の限定性・監視不成立時の停止、旧guard／record・上限・残予算・Owner管理者terminal条件を保持し、具体的な1行commandを準備する。成果commit／R0-C結果受入／R1／remoteを含めない。

### 21.1 承認後のOwner手順準備（2026-10-10）

`Start-Section21-Owner.ps1`を新epochで準備した。runnerはpure34例成立時から変更0。旧1549 artifact／旧記録はhash不変、新Owner手順1件追加でmanifest1550件。旧§14〜20のrecord／部分XML89 fileをSHA付き保全し、pure summary／34例JSONと同runner hashをgateで明示照合する。parser／diff check成立。

Owner gateは同ユーザー／管理者token、source／snapshot・artifact／旧記録、pure34例、保護対象Java、Docker空baseline・資源／残予算を実環境で確認する。有限28例・実短命launch1回→全成立時の通常root追加1回→今回開始時刻以降のfresh XML・予定skip92件照合→cleanup／保護対象Java identity確認の順。`ROOT_SEQUENCE_COMPLETE`はfailureなし・cleanup保存後だけ表示する。後続未実行集合はAgentのroot証拠確認後に、既承認内で具体化して進む。

準備確認2秒計上、累計1636秒、残1964秒（32分44秒）、cleanup300秒予約。Agentから実CIM query／event登録／launch／root／container操作なし。Ownerは同じユーザーの**管理者PowerShell 7**、Repository rootで、次の**1行だけ**を1回実行する。

```powershell
& '.\tmp\r0-rules-9c41aa4-20261009-first\Start-Section21-Owner.ps1'
```

`ROOT_SEQUENCE_COMPLETE`または`STOPPED`の表示を知らせる。停止時は同commandを再実行しない。旧guardを消さず、端末状態の変化やUNKNOWNを推測で補わない。これは既承認のOwner管理者terminal手順であり、同じ承認を再要求するものではない。

### 21.2 Owner実行結果：生存CIM子の正式登録前に移行判定が停止（2026-10-10）

実環境gate成立、有限28例全一致、短命java-version probeは1.773秒で成立、subscription残0。通常rootは22.670秒、`Ambiguous duplicate launch PID=5456; failed=ChildKnownCreation`で停止した。比較記録では子CIMが1件あり、cmd.exe／parent13924／creation `639271671436269350`。親Java13924のCIM creation `639271671231316200`は正式台帳と一致する。10条件中9条件がtrueで、子の正式台帳値だけがnull。名前／親の矛盾や子CIM未捕捉を今回の原因としない。

現行root loopはCIM取得→event分類・移行判定→親子pollによる正式登録の順。今回の子は分類時にcurrent CIMで見えているが、後段pollの登録機会に到達する前にChildKnownCreationで停止した。旧pure例は衝突子を事前登録しており、この「current CIMあり・子未登録」の経路を確認していなかった。先行所有外conhost eventと後続cmd eventのPID一致だけから、OSのPID再利用や旧instance終了を確定しない。

受信batchの停止eventはClassifying、後続conhost27016／java30984はPendingで保存された。今回の他3件の移行比較は成立しているが、監視全体の成立とはしない。cleanupはowned process0／container0／subscription0、保護対象Java24852／10280のcreation保持、source不変。11 sampleの最大JVM2／PG0／Ryuk0／client0は全期間保証ではない。

今回開始時刻以降のfresh XML3 file／23件（contract4＋BusinessModuleRuleSetTest19、failure／error／skip0）を`section21-root-partial-xml/`へSHA付き保存した。通常root全体・予定skip92照合は未成立、旧Rules単独107 PASSと混算しない。PL2／package／旧Consumer／E2E未起動。

snapshot／元source1156 file（承認済み文書4件を除く）、artifact1550件・旧記録89 fileのhash不変を確認し、`section21-stop-preservation.json`へ記録を保全。Owner実行後1668秒に保全3秒を加え、累計1671秒、残1929秒（32分09秒）、cleanup300秒予約。runner追加訂正・再実行・tracked code／test／POM変更・commit／remote0。§21の追加root1回は消費済みで、残予算を再実行許可と扱わない。

## 22 生存CIM衝突子の検証付き登録・pure契約追加確認案（OWNER APPROVED、root追加0回）

対象はtmp `Run-Bounded.ps1`の衝突移行判定とpure fixture／新epoch手順・証拠だけ。全体loopを単純に並べ替えず、現行の子正式登録要求を満たすための独立CIM証明を限定追加する案。

1. 先行所有外・新event順・異なるsignature、子／親CIM各1件、子name／parent一致・root期間内・親以後、確認済み親のname／parent／creationとcanonical record・正式台帳の完全一致、event生成時刻が子creation以後、子の既知creationとの矛盾なしを**登録前に全て**確認する。子未登録の場合だけ、同一CIM snapshotの完全な子identityをcanonical記録／正式台帳へ登録してから、従来のChildKnownCreationを含む移行条件を評価する。登録前後値と登録根拠を比較記録に残す。既知creationを上書きせず、PIDだけで登録しない。
2. 生存CIMなしの既存TraceOnly経路、既所有重複・同signature・矛盾・親generation不一致の停止、受信batch全件保全と旧履歴非昇格を維持する。独立CIM登録は当該生存instanceの証明であり、先行eventやその子の所有昇格・旧instance終了の証明にはしない。root／probe／最終drainで共通の移行処理を使う。
3. 旧pure34例の期待値を維持し、新規6例を追加する。肯定1例は§21相当の「所有外履歴あり、current子CIMと既知親が整合、子未登録」で登録・移行・旧履歴保持を確認。負例5例は子CIM重複、子creationがroot期間外、eventが子creationより前、親canonical metadata矛盾、子既知creation矛盾で、拒否と子台帳の非追加／非上書きを確認する。旧34例が崩れた場合も停止し、期待値を自動変更しない。
4. source／artifact／旧記録hashと残予算を照合後、外部操作を拒否するpure専用手順で**40例1回、gate・fixture・保存全体60秒以内**を提案する。実CIM query／event登録／実launch／Java／Maven／Docker／root追加は0回。snapshot・Repository実装／test／POM／依存・OS権限・旧guard／証拠を変更しない。既存総60分・cleanup予約・作業量／raw上限を維持し、準備・検証・保全を計上する。

本案は監視契約の不足経路をpureで先に確認する限定訂正。全例成立しても実root成立や資源保証へ昇格しない。実環境再開・追加rootは結果と残予算を入力とする別判断、成果commit／R0-C受入／R1／remoteも別判断。承認前に訂正・40例実行を先行しない。

Ownerは§22を確認し「承認いたします」と明示した（2026-10-10）。上記tmp限定訂正、旧34例の期待値を維持した新規6例追加・pure40例1回を承認済み。実CIM／event／launch／root追加0回、上限・停止条件・実環境再開別判断を維持する。

### 22.1 承認後の限定訂正・pure40例結果（2026-10-10）

tmp移行処理に登録前の検証と`validated-transition-cim`の完全identity保存を追加した。既存9条件、親canonical recordとの完全一致、event生成が子creation以後、既存子canonical metadata／event bindingとの矛盾なしを全成立させた場合だけ、未登録子を正式台帳へ追加する。既知creationの上書きは禁止。比較記録に登録前後・成立条件・登録有無を保存する。全体loop・既存TraceOnly条件・旧34例の期待値を変更しない。

新pure手順`Run-Section22-Pure.ps1`、manifest1551 artifact・旧記録324 fileをSHA付き準備。旧1550 inputは承認済みrunner更新以外hash不変。parser成立。前後gateはbranch／HEAD・snapshot／元source1156 file（文書4件除外）・artifact／旧記録hash整合。純粋fixtureの外部command拒否手順を維持した。

承認済みpure40例1回は**40例全一致・0不一致、0.454秒、PASS**。旧34例の名前・Actual・Expectedは保存済み§20結果と一致。新肯定例ではKnownChildBefore=null→canonical creation登録、ChildKnownCreation=true、移行成立・先行所有外履歴保持・旧子非昇格を確認。負例5件は全て拒否し、子正式台帳の非追加／既知値非上書き・新canonical子非追加を確認した。比較記録6件中登録1件、非登録5件。

summaryのFailure=null・外部操作attempt0、実CIM query／event登録／process・Java／Maven／Docker／root0回。probe実出力なし。`section22-result-preservation.json`へ新証拠SHAと旧34例不変・登録件数を保全。準備2秒＋gate／fixture／保存5秒＋保全1秒を計上し、累計1679秒、残1921秒（32分01秒）、cleanup300秒予約。tracked code／test／POM追加変更・commit／remote0、旧guard／記録を維持する。

§22の承認範囲は完了。pure PASSは実OSのevent／CIM観測順やPID世代、通常root・資源上限を証明しない。旧Rules単独107 PASS、通常root／予定skip92未成立、後続PL2／package／旧Consumer／E2E未実行・R0-C受入別判断を維持する。

## 23 pure40例成立後の実環境再開案（OWNER APPROVED）

1. 新epoch `section23-`、同ユーザーOwner管理者PowerShell 7で、branch／HEAD `e8c81fe`・snapshot1156 file／元source・現artifact／旧証拠hash、pure40例と現runner hashの一致、SID／管理者token・保護Java identity・Docker空baseline・memory／disk／raw／run容量・残予算を実照合する。§21時点のruntime状態を推測せず、不一致で停止する。旧guardを削除しない。
2. 現runnerで既存有限28例と短命java-version launch1回を全体60秒以内／実launch10秒以内で確認する。pure40例の追加実行は0回とし、同runner epochの保存結果をgate入力とする。登録前後・canonical／identity／受信batch・subscription解除残0を保存する。全成立時だけ通常rootへ進む。
3. **通常root追加1回、900秒以内／400 invocation以内**を提案する。client14／Ryuk2、JVM4／PG1を維持し、今回fresh XMLだけで予定skip92（S1の7 class64＋B2の3 class28）を照合する。成功／停止どちらでも今回process／container／subscription残0・保護Java identity保持・source不変と比較記録を確認する。failure／UNKNOWN／上限超／cleanup不成立では停止し、追加訂正・再実行を先行しない。
4. root成立時だけ、未実行PL2候補4件（300秒以内）、専用package／再compileしない旧Consumer／既存package E2E（合計600秒以内）へ進む。各段階でcleanup300秒を予約し、残予算から実効上限を短縮する。後続全てに最大時間を割り当てる余裕はないため、実測で残時間が不足した集合は未実行として停止・記録する。他phaseのclient8／Ryuk1等の上限を維持する。

累計1679秒・残1921秒（32分01秒）、総60分・cleanup300秒予約、既存作業量・raw上限を維持。準備／gate／harness／root／後続／保全を全て計上する。対象は現runnerを変更せず、新Owner手順・manifest・新epoch証拠を準備して実行する範囲。Repository code／test／pool／POM／依存、OS／WMI権限、provider、新規依存取得・Rules単独再実行・commit／R0-C受入／R1／remoteを含めない。§22のroot0回承認を流用せず、承認前に実環境操作を先行しない。

Ownerは§23を確認し「承認します」と明示した（2026-10-10）。現runner・pure40例の同epoch照合、実環境gate・有限28例／短命launch1回、全成立時の通常root追加1回と、root成立時の未実行後続集合を残予算内で進めることを承認済み。上限・停止条件・Owner管理者terminal・旧guard保全・成果受入別判断を維持する。

### 23.1 承認後のOwner再開手順準備（2026-10-10）

新epoch `Start-Section23-Owner.ps1`を準備した。runnerはpure40例PASS時とhash一致・変更0。旧1551 artifact／旧証拠hash不変、新Owner手順追加でmanifest1552件。旧epoch証拠336 fileをSHA付き保全、snapshot／元source1156 file（文書4件除外）不変、parser成立。pure40例のsummary・全例JSON・同runner hashを実gate入力として固定した。

同ユーザー／管理者token・実source／artifact／旧記録・保護Java identity・Docker空baseline・資源／予算のgate→有限28例・短命launch1回→全成立時の通常root1回→fresh XML／予定skip92確認→cleanup／baseline保持の順。`ROOT_SEQUENCE_COMPLETE`はfailureなし・cleanup保存後だけ表示する。PL2／package／旧Consumer／E2EはAgentのroot証拠確認後、既承認範囲と残予算内で具体化する。

準備確認3秒計上、累計1682秒、残1918秒（31分58秒）、cleanup300秒予約。Agentから実CIM query／event登録／launch／Maven／root／container操作0、tracked code／test／POM追加変更・commit／remote0。Ownerは同じユーザーの**管理者PowerShell 7**、Repository rootで次の**1行だけを1回**実行する。

```powershell
& '.\tmp\r0-rules-9c41aa4-20261009-first\Start-Section23-Owner.ps1'
```

`ROOT_SEQUENCE_COMPLETE`または`STOPPED`の表示を知らせる。停止時は同commandを再実行せず、旧guardを消さない。現在のbaselineがgateと一致しない場合も停止して実値を記録する。既承認のOwner管理者terminal手順であり、同じ承認の再要求ではない。

### 23.2 Owner実行結果：確認済みcanonical親子のevent後着時に停止（2026-10-10）

gate成立、有限28例全一致、短命java-version probe終了0／1.695秒・subscription残0。通常rootは9.174秒、PID17264の移行判定で停止。incomingはconhost.exe／parent24292、ChildCim=[]／ParentCim=[]だが、KnownChildTicks=`639271682668393670`、KnownParentTicks=`639271682668336200`。先行所有外git.exe／parent5440から別signatureへの移行で、新規登録対象ではない。§22の生存CIM子未登録経路を再現した失敗ではなく、同経路の実成功／失敗とも認定しない。

保存済みcanonical記録は子17264＝conhost.exe／parent24292／creation `639271682668393670`、親24292＝cmd.exe／parent15364／creation `639271682668336200`で、双方`parent-chain-cim-poll`から18:44:27 UTCに保存。後続eventは18:44:28.461462 UTC生成・同28.869010受信。親eventは既にcached canonicalへ結合済み、子は未結合。生存CIMが今回なくても過去の完全な観測記録はある一方、移行判定はcurrent CIMを必須にして拒否している。終了を実証したとはせず、欠落current CIMを推測値で埋めない。

停止子eventはClassifying、後続java28580はPendingで保存。TraceOnlyはcmd.exe限定・既知子なし等のため今回不成立であり、その対象をconhostへ広げる案とはしない。cleanupはowned process0／container0／subscription0、保護Java24852／10280のcreation保持、source不変。4 resource sampleの最大JVM2／PG0／Ryuk0／client0は全期間保証ではない。

fresh部分XML2 file／contract4件、failure／error／skip0をSHA付き保全。snapshot／元source1156 file（文書4件除外）・artifact1552件・旧証拠336 fileのhash不変を確認し、`section23-stop-preservation.json`へ保存した。Owner実行後1700秒＋保全3秒＝累計1703秒、残1897秒（31分37秒）、cleanup300秒予約。通常root／予定skip92未成立、旧Rules単独107 PASSとpure40 PASSは別証拠、後続未実行。追加訂正・再実行・code／test／POM変更・commit／remote0。

## 24 確認済みcanonical親子と後着eventの衝突照合・pure契約再整理案（OWNER APPROVED、root追加0回）

current CIMだけで証明する経路と、同じroot実行中に既にCIMで証明・保存したidentityを後着eventへ結合する経路を分ける。PID17264の個別例外やTraceOnly対象拡大ではなく、保存済み証明を用いる移行契約を有限fixtureで先に確認する。対象はtmp runnerの衝突照合・比較記録、pure fixture／新手順・証拠だけ。

1. 保存済み親子を使う移行経路を限定追加する。先行所有外・新event順・異なるsignatureに加え、正式台帳の子／親creationと完全なcanonical recordが一致し、子name／parent・親name／parentがevent系譜と整合、親recordがcanonicalへ結合済みでgeneration一致、子creationがroot期間内・親以後、event生成が子creation以後であることを要求する。同一root epochのroot anchorへ辿るCIM親子証拠を一意に照合し、候補複数・証拠欠落・metadata／generation矛盾で停止する。既存子event結合が別generationなら拒否する。曖昧なPID一致だけを証拠にしない。
2. current CIMがある側は保存済みidentityとの厳密一致を要求し、異なるcurrent instanceを旧instanceへ昇格しない。current CIMなしの側を終了済みと断定しない。新しい正式台帳値の追加／既知値上書きは行わず、完全に確認済みの子event結合と旧所有外履歴保持だけを行う。既存生存CIM登録・TraceOnly・既所有重複拒否・外国親の非継承・batch保全を維持する。判断根拠となるcache／current／親generation・root証明・拒否位置を保存する。
3. 旧pure40例の期待値を維持し、新規10例を追加する。肯定3例は確認済みcanonical親子について「両方current CIMなし」「子だけなし」「親だけなし」で後着eventを結合し、旧履歴保持・旧子非昇格・台帳不変・終了UNKNOWNを確認。負例7例は子cache欠落、親cache欠落、cached metadata矛盾、結合候補複数、eventが子creationより前、子eventが別generationへ結合済み、current instance相違で拒否・非昇格・台帳非上書きを確認する。各例の台帳を分離し、既存期待値を自動変更しない。
4. source／artifact／旧証拠hash・残予算確認後、外部操作を拒否するpure専用手順で**50例追加1回、gate／fixture／保存全体60秒以内**を提案する。実CIM／event登録／実launch／Java／Maven／Docker／root追加0回。旧記録・guard・snapshotは変更しない。総60分・cleanup300秒予約・作業量／容量／停止条件を維持し、準備・検証・保全を計上する。

本案の成立は監視pure契約の限定確認で、実root／資源保証・完全網羅ではない。実再開・追加rootは結果と残予算による別判断。Repository実装／test／pool／POM／依存・OS権限・provider・成果commit／R0-C受入／R1／remoteへ拡張しない。未承認・未適用・未実行。個別停止のたびにrootを反復する承認は提案せず、上記観測順3形態と否定条件をpureでまとめて確認する。

Ownerは§24を確認し「承認いたします」と明示した（2026-10-10）。tmp限定訂正と旧40例の期待値を維持する新規10例追加・pure50例1回を承認済み。実CIM／event／launch／root追加0回、既存上限・停止条件・実再開別判断を維持する。

### 24.1 承認後のtmp訂正・pure確認停止（2026-10-10）

tmpにcached移行契約を追加。子／親canonical候補・正式creation、親のevent結合とmetadata、CIM親子cacheからroot anchorまでの一意chain、event時刻・既存結合・current CIMがある側の完全一致を検査し、全成立時だけ後着eventの結合へ進む。正式台帳の新規追加／上書きは行わない。比較記録へcache／root chain／成立条件・拒否位置を保存する。既存40例の期待値を維持し、新10例を追加した。

新pure手順・manifest1553 artifact／旧記録361 fileをSHA付き準備。旧1552 inputは承認済みrunner更新以外不変、parser成立、準備2秒計上。承認済みpure50例1回はgate成立後0.528秒で停止した。追加例の最初の監視関数呼出し前、runner line504の`$ledger | ConvertTo-Json`が整数PIDキーを拒否し、`NonStringKeyInDictionary`となった。Agentが追加したfixture比較処理の不備であり、実監視の拒否やcached移行の成否ではない。

50例JSONは保存位置に到達しておらず、旧40例も今回epochのPASS件数として認定しない。旧§22のpure40 PASSは旧runner epochの結果として保全し、変更後runnerの全体成立へ流用しない。新10例の監視判定未確認・全体PASS未成立。summary外部操作attempt0、実CIM／event／launch／Java／Maven／Docker／root0回。旧guardと証拠を保持した。

`section24-stop-preservation.json`へ失敗位置・証拠SHA・artifact1553件／旧記録361 fileのhash不変を保全。準備2秒＋gate／fixture／保存5秒＋保全2秒＝累計1712秒、残1888秒（31分28秒）、cleanup300秒予約。停止後の追加訂正・再実行・tracked code／test／POM変更・commit／remote0。通常root／予定skip92・後続集合未成立を維持する。

## 25 pure fixture台帳比較だけの有限訂正・追加1回案（OWNER APPROVED、root追加0回）

対象は新10例内の正式台帳比較2箇所だけ。整数PIDキーのHashtableを直接JSON化せず、実行前に同じPIDキーとcreation値を別Hashtableへコピーし、実行後に件数・全キー存在・各creation値の一致で不変を比較する。PIDの型と値を維持し、実監視の正式台帳・cached移行条件・root証明・50例の期待値を変更しない。旧記録／guard／snapshotを保全する。

新pure epochとmanifest・証拠を準備し、source／artifact／旧記録hashと残予算確認後、外部操作を拒否する既存手順で**同じ50例を追加1回、gate／fixture／保存全体60秒以内**だけ確認する。実CIM／event／実launch／Java／Maven／Docker／root追加0回。総60分・cleanup300秒予約・作業量／容量／停止条件を維持し、準備・検証・保全を計上する。不一致・例外・証拠不足で停止し、追加修正／反復を先行しない。

Repository code／test／POM／依存・OS／provider・実環境再開・成果commit／R0-C受入／R1／remoteを含めない。未承認・未適用・未実行。本案成立後も実環境再開は別判断とする。

Ownerは§25を確認し「承認いたします」と明示した（2026-10-10）。fixture台帳比較2箇所だけの訂正と同じpure50例の追加1回を承認済み。実監視条件・期待値・上限・停止条件を維持し、実CIM／event／launch／root追加0回、実環境再開は別判断とする。

### 25.1 承認後の台帳比較訂正・pure50例全成立（2026-10-10）

tmp新10例内の台帳比較2箇所だけを訂正した。整数PIDキーとcreation値を別Hashtableへコピーし、実行後の件数・全キー存在・値一致で不変を検査する。JSON直接変換を除去し、実監視・cached移行・root証明・50例の期待値は不変。

新pure手順`Run-Section25-Pure.ps1`・manifest1554 artifact／旧記録370 fileをSHA付き準備。旧1553 inputは承認済みrunner更新以外hash不変、parser成立、準備2秒計上。承認済みpure追加1回は**50例全一致・0不一致、0.559秒、PASS**。旧40例は§22保存結果と名前／Actual／Expectedが一致。追加10例の比較記録はcached移行成立3件・拒否7件で、肯定3形態の結合・旧履歴保持・旧子非昇格・終了UNKNOWN・台帳不変、負例7件の拒否・非昇格・台帳非上書きを確認した。

前後source／snapshot1156 file（文書4件除外）・artifact／旧証拠hash gate成立、summary Failure=null・外部操作attempt0。実CIM／event登録／process・Java／Maven／Docker／root0回、実probe出力なし。`section25-result-preservation.json`へ証拠SHA・旧40例不変・成立／拒否件数を保全。準備2秒＋gate／fixture／保存5秒＋保全2秒＝累計1721秒、残1879秒（31分19秒）、cleanup300秒予約。tracked code／test／POM追加変更・commit／remote0、旧guard／証拠保持。

§25の範囲は完了。現在runnerのpure契約50例は成立したが、実OSのevent／CIM観測・全期間資源・通常root／予定skip92は未成立。旧Rules単独107 PASS・後続PL2／package／旧Consumer／E2E未実行・R0-C受入別判断を維持する。

## 26 pure50例成立後の実環境再開案（OWNER APPROVED）

1. 新epoch `section26-`、同ユーザーOwner管理者PowerShell 7でbranch／HEAD `e8c81fe`・snapshot／元source1156 file・現artifact／旧証拠hash、pure50例と同runner hash、SID／管理者token・保護Java identity・Docker空baseline・資源／容量・残予算を実照合する。旧runtime状態を推測せず、不一致で停止、旧guardを削除しない。
2. 現runnerの既存有限28例＋短命java-version launch1回を全体60秒以内／実launch10秒以内で確認する。pure50例の追加実行0回、保存結果をgate入力とする。登録／cached移行・root証明・比較実値／受信batch・subscription残0を保存し、全成立時だけ通常rootへ進む。
3. **通常root追加1回、900秒以内／400 invocation以内**。client14／Ryuk2・JVM4／PG1を維持し、fresh XMLだけで予定skip92（S1の7 class64＋B2の3 class28）を照合する。成否どちらでも今回process／container／subscription残0・保護Java保持・source不変を確認。failure／UNKNOWN／上限超／cleanup不成立で停止し、追加訂正・再実行を先行しない。
4. root成立時だけ未実行PL2候補4件（300秒以内）、専用package／再compileしない旧Consumer／既存package E2E（合計600秒以内）へ進む。他phaseのclient8／Ryuk1等の既存上限と集合を維持し、段階ごとに残予算からcleanup300秒を引いて実効上限を短縮する。残時間不足では未実行として停止・記録し、完了を推測しない。

累計1721秒・残1879秒（31分19秒）、総60分・cleanup300秒予約・作業量／容量／停止条件を維持。準備／gate／harness／root／後続／保全を全て計上する。対象は現runnerを変更せず新Owner手順・manifest・証拠を準備して使う範囲。Repository code／test／pool／POM／依存、OS／WMI権限・provider、依存取得・Rules単独追加・commit／R0-C受入／R1／remoteを含めない。§25のroot0回承認を流用せず、未承認・未準備・実環境操作未実行。

Ownerは§26を確認し「承認いたします」と明示した（2026-10-10）。同時に「同種の問題改修については、確認承認は経ずに継続を許可」「最終的な結果に対する承認のみ」と指示した。§26の実環境再開・root成立時の後続集合を承認済みとし、以後の同種tmp監視／pure fixture不備は、原因・有限訂正・検証結果・残予算を記録し、個別の訂正／再開承認を再要求せず継続できる。過去の同種訂正に関する停止後再審査条件は、この新しいOwner指示に従って更新する。

継続許可は本R0検証のtmp監視・fixtureと同じ検証集合を既存上限内で成立させる範囲に適用する。失敗epochを停止・保全し、原因に対応する有限確認を成立させてから新epochへ進む。総60分・cleanup300秒予約、資源／作業量／容量・source固定・旧guard／証拠保全、Reference／Security等の除外は維持する。上限超やcleanup不成立・source不整合を無視して実行しない。OS権限変更・新provider・依存取得・Repository実装拡張・commit／remote／R1を承認したとは扱わない。最終結果の受入は別途Owner判断とする。実環境操作には引き続き同ユーザーOwner管理者PowerShell手順を用い、Agentが権限を迂回しない。

### 26.1 承認後のOwner手順準備（2026-10-10）

新epoch `Start-Section26-Owner.ps1`を準備した。runnerはpure50例PASS時とhash一致・変更0。旧1554 artifact／旧証拠hash不変、新Owner手順追加でmanifest1555件、旧証拠382 fileをSHA付き保全。snapshot／元source1156 file（文書4件除外）不変、parser成立。pure50例のsummary・全例JSON・同runner hashを実gate入力として固定した。

同ユーザー／管理者token・実source／artifact／旧記録・保護Java・Docker空baseline・資源／予算gate→有限28例・短命launch1回→全成立時の通常root1回→fresh XML／予定skip92照合→cleanup／baseline保持の順。ROOT_SEQUENCE_COMPLETEはfailureなし・cleanup保存後だけ表示。後続PL2／package／旧Consumer／E2EはAgentのroot証拠確認後、既承認範囲と残予算内で具体化する。

準備3秒計上、累計1724秒、残1876秒（31分16秒）、cleanup300秒予約。Agentから実CIM／event／launch／Maven／root／container操作0、tracked code／test／POM追加変更・commit／remote0。Ownerは同じユーザーの管理者PowerShell 7、Repository rootで次の1行を1回実行する。

```powershell
& '.\tmp\r0-rules-9c41aa4-20261009-first\Start-Section26-Owner.ps1'
```

ROOT_SEQUENCE_COMPLETEまたはSTOPPEDの表示を知らせる。停止した同epochを再実行せず、旧guardを消さない。同種不備は§26の継続許可に基づきAgentが原因・有限訂正・確認を記録して新epochを準備する。これは個別承認の再要求ではなく、実管理者terminalを必要とする既承認の操作手順である。

### 26.2 Owner実行：旧PID creation固定により新instanceを所有外とした停止（2026-10-10）

gate・有限28例／短命launch成立後、rootは32.026秒、PID4580の衝突移行で停止した。同PIDのgit event→conhost／parent31396→conhost／parent21696が保存されている。旧canonical creation639271694690404940／parent31396は保持されたが、新current CIM creation639271694805093440／parent21696をForeignObservedCimとして記録し、正式PID台帳は旧creationのままだった。停止eventは新current recordと同signatureで、そのrecordには旧event生成時刻がないためNewerEvent／DistinctSignatureも不成立。子・親current CIMは停止時には空。欠落値から終了を推測しない。

同じroot中にcached移行が成立した別PIDの比較記録はあるが、監視全体PASSとはしない。cleanupはowned process／container／subscription残0・保護Java保持・source不変。fresh部分XMLは56件、failure／error／skip0。16 resource sampleの最大JVM2は全期間保証ではない。snapshot／元source1156 file（文書4件除外）・artifact1555件／旧証拠382 fileのhash不変を確認し、section26-stop-preservation.jsonへSHA付き保全。Owner実行後1766秒＋保全3秒＝1769秒、残1831秒。通常root／予定skip92・後続集合は未成立。

## 27 同種継続許可によるPID instance世代の限定訂正・pure58例確認（2026-10-10）

§26の継続許可に基づき、tmpだけを訂正。旧canonicalと旧event履歴を保持したまま、より新しいcreationのcurrent子CIMが1件・current親の完全identityが既知・CIM親子cacheがroot anchorへ整合するときだけ、新canonical instanceを保存し正式PID索引を新creationへ進める。旧cacheは変更せず、root／未知親・矛盾・古いcreation・current候補複数を昇格しない。登録前後値・条件・root証明をidentity比較へ保存する。

cached移行はeventのname／parentに合うcanonical候補を一意照合する。今回観測済みForeignObservedCimの完全identityと、新canonicalが一致する場合には、欠落旧event時刻の推測やsignature変更要求を使わず、独立CIM証明・event時刻・親generation・既存結合・current一致の全条件で結合する。既所有eventの重複拒否は維持する。

旧50例の期待値を維持し、同PID新instanceの生存先行／消失後event・旧履歴保持、同event再送拒否、未知親／親creation矛盾／古いcreation／current重複を扱う新8例を追加。manifest1556 artifact／旧証拠413 file・parser成立。pure58例1回は57一致・1不一致で停止（外部操作0）。不一致は親creation矛盾で子が所有外に分類され台帳登録が拒否されたのに、fixtureが例外停止を必須にしていたもの。監視の拒否条件を緩めず、section27-stop-preservation.jsonへ失敗を保全した。準備2秒＋実行等6秒＋保全1秒で累計1778秒。

## 28 新fixtureの非所有分類確認訂正・pure58例全成立（2026-10-10）

同種継続許可に基づき、新8例のうち親creation矛盾例の判定だけを、例外停止または子の非所有分類・台帳不変・新canonical非追加の確認へ訂正。実監視と旧50例の期待値は不変。旧epochを上書きせず、新手順／manifest1557件・旧証拠423 fileで同じ58例を追加1回確認した。

pure58例は0.709秒、58一致・0不一致でPASS。旧50例の名前／Actual／Expectedは§25保存結果と一致、新8例全一致。前後source／snapshot1156 file（文書4件除外）・artifact／旧証拠hash gate成立、Failure=null・外部操作attempt0、実CIM／event／launch／Java／Maven／Docker／root0回。section28-result-preservation.jsonへ証拠SHA・旧50例不変を保全。準備2秒＋実行等6秒＋保全1秒で累計1787秒、残1813秒、cleanup300秒予約。tracked code／test／POM追加変更・commit／remote0。実root／全期間資源／予定skip92・成果受入は未成立。

## 29 同種継続許可による実環境再開手順準備（2026-10-10）

§26の継続許可に基づき、Start-Section29-Owner.ps1を新epochで準備した。runnerはpure58例PASS時から変更0、旧1557 artifact／旧証拠不変、新手順追加でmanifest1558件・旧証拠435 fileをSHA付き固定、parser成立。pure58例の同runner hashと証拠、source・保護Java等をOwner実gateで照合する。

同ユーザーOwner管理者PowerShell 7でgate→有限28例・短命java-version launch1回（全体60秒／launch10秒）→全成立時root追加1回（900秒／400 invocation以内、client14／Ryuk2／JVM4／PG1）→fresh XML／予定skip92照合→cleanup／保護Java保持の順。root成立時だけAgent証拠確認後に、既承認PL2候補4件・package／再compileしない旧Consumer／既存E2Eへ残予算内で進む。他phase client8／Ryuk1、cleanup300秒予約・資源／作業量／容量・除外対象は維持する。

準備2秒計上、累計1789秒、残1811秒（30分11秒）、cleanup300秒予約。Agentから実環境操作0、追加code／test／POM変更・commit／remote0。これは新規承認要求ではなく、同種継続許可内の操作手順。Ownerは同じユーザーの管理者PowerShell 7、Repository rootで次の1行を1回実行する。

```powershell
& '.\tmp\r0-rules-9c41aa4-20261009-first\Start-Section29-Owner.ps1'
```

ROOT_SEQUENCE_COMPLETEまたはSTOPPEDを共有する。同epochの再実行・旧guard削除はせず、停止証拠はAgentが確認し必要な限定訂正を継続する。最終結果の受入だけOwner判断とする。

### 29.1 Owner実行結果：current／canonical未捕捉の短命conhost系譜で停止（2026-10-10）

gate・有限28例／短命launch成立後、rootは9.011秒、PID31408のconhost／parent13444衝突移行で停止。child／parent current CIMもcanonicalもなく、正式台帳値は双方null。親cmd13444のeventは既知Java23848→root29060の系譜に分類済みだがCIM未確認。子の先行所有外conhost／parent26608と後着conhost／parent13444は生成時刻・signatureが異なる。直前には別PID30812でcached移行成立記録があり、その実経路成立と監視全体未成立を分ける。短命親子の終了や完全なOS instanceをeventから断定しない。

cleanupはowned process／container／subscription残0、保護Java24852／10280保持・source不変。fresh部分XML2 file／contract4件、failure／error／skip0をSHA付き保全。4 sampleの最大JVM2は全期間保証ではない。source／snapshot1156 file（文書4件除外）・artifact1558件・旧証拠435 fileのhash不変を確認、section29-stop-preservation.jsonへ保存。Owner実行後1808秒＋保全3秒＝1811秒、残1789秒、cleanup300秒予約。通常root／skip92・後続未成立を維持。

## 30 同種継続許可による補助conhostのevent系譜保持・pure66例全成立（2026-10-10）

§26継続許可でtmpに補助conhost限定経路を追加。先行所有外・後続新event／異signature、current子CIMなし・既知子creationなし、child raw FILETIMEと生成ticks一致、親generation連鎖がLatest記録を介して固定root anchorへ一意に整合し、各親eventが子より前・親current CIMがあれば完全一致することを要求する。generation循環／不一致・root非所有・時刻矛盾を拒否。Javaには適用しない。

成立時はconhostをTraceOnlyとしてevent系譜に保存し、正式PID台帳へ登録せず、creation未確認・終了UNKNOWNを維持する。親cmdのCIMなしを補完したとは扱わない。Javaの生存CIMは独立照合し、後から観測したconhost CIMの不一致は拒否する。比較記録へ条件・root event chain・拒否位置を保存する。

旧58例不変、新8例はroot系譜のconhost保持／Java独立CIM、Javaへの適用拒否・親generation不一致・時刻逆転・既知子矛盾・root非所有・後着CIM不一致。manifest1559 artifact／旧証拠460 file、parser成立。pure66例1回は0.781秒、66一致・0不一致でPASS。旧58例の名前／Actual／Expectedは§28保存結果と一致。前後source／snapshot1156 file（文書4件除外）・artifact／旧証拠hash gate成立、Failure=null・外部操作attempt0、実CIM／event／launch／Java／Maven／Docker／root0回。

section30-result-preservation.jsonへ証拠SHA・旧58例不変を保全。準備2秒＋実行等6秒＋保全1秒＝累計1820秒、残1780秒、cleanup300秒予約。旧epoch・guard保持、tracked code／test／POM追加変更・commit／remote0。pure PASSは実OSの全instance・終了・監視完全網羅・全期間資源・root成立の証明ではない。

## 31 同種継続許可内の実環境再開手順（2026-10-10）

Start-Section31-Owner.ps1を新epochで準備。runnerはpure66例epochから変更0、manifest1560 artifact／旧証拠472 fileをSHA付き固定、parser成立。Owner同ユーザー管理者PowerShell 7で実gate（source／artifact／pure同epoch・保護Java・Docker空baseline／資源／残予算）→有限28例／短命java-version1回（全体60秒／launch10秒）→全成立時root追加1回（900秒／400 invocation以内、client14／Ryuk2／JVM4／PG1）→fresh XML／skip92照合→cleanup／baseline保持を確認する。

root成立時だけAgent証拠確認後、既承認PL2候補4件・package／再compileしない旧Consumer／既存E2Eへ残予算内で進む。他phase client8／Ryuk1、総60分・cleanup300秒予約・作業量／容量・除外対象を維持する。failure epochは停止・保全し、同種原因の有限訂正は個別承認なしで継続、最終受入はOwner判断とする。

準備2秒計上、累計1822秒、残1778秒（29分38秒）、cleanup300秒予約。Agentの実環境操作0、tracked code／test／POM追加変更・commit／remote0。Ownerは同じユーザーの管理者PowerShell 7、Repository rootで次の1行を1回実行する。

```powershell
& '.\tmp\r0-rules-9c41aa4-20261009-first\Start-Section31-Owner.ps1'
```

ROOT_SEQUENCE_COMPLETEまたはSTOPPEDを共有する。同epochを再実行せず旧guardを消さない。新規の承認要求ではなく、同種継続許可内の実管理者terminal操作手順である。

### 31.1 Owner実行：新instance初回登録拒否後、親成立時の再照合不足（2026-10-10）

有限28例／短命launch・gate成立後、rootは20.351秒、PID22948のNewerEvent／DistinctSignature／ChildKnownCreation不成立で停止。旧javac canonical creation639271709547594930／parent23392から、新conhost current creation639271709677489500／parent26392を観測した。初回には親26392が正式未登録で再利用登録のCurrentParentProof／RootEpochChainがfalse。後段pollで親が登録されたが、Latest子は既にForeignObservedCimで再照合対象外、正式PID索引は旧creationのままだった。停止時は子／親current CIM各1件・名前／親／期間・親identity成立。時刻・signature不成立はForeignObservedCim recordに旧event時刻がなくincomingと同じ新instance metadataだったことによる。

cleanup残0・保護Java保持・source不変。fresh部分XML2 file／contract4件、failure／error／skip0。10 sample最大JVM2は全期間保証ではない。source1156 file（文書4件除外）・artifact1560／旧証拠472 hash不変をsection31-stop-preservation.jsonへ保全。Owner後1853秒＋保全3秒＝1856秒。root／skip92・後続未成立。

## 32 同種継続許可による親成立後の再照合・pure70例成立（2026-10-10）

§26許可でtmpを限定訂正。ForeignObservedCimの完全identityとcurrent CIMが一致し、正式PID索引が旧creationの場合に再利用登録を再照合する。主rootの既存親子pollでも親登録後に同じCIM snapshotから再照合し、登録成立時だけ有限pollを継続する。新creation／current一意・生存親完全identity・root証明の条件は維持し、旧cache／event履歴を変更しない。

旧66例不変、新4例（親が次pollで成立／親current欠落／親current矛盾／子消失）を追加。manifest1561 artifact／旧証拠497 file・parser成立。pure70例は0.787秒、70一致・0不一致PASS、前後hash gate成立・外部attempt0、実環境操作0。準備2秒＋実行等7秒で累計1865秒。実root成立とはしない。

## 33 固定root anchor参照の統一・pure71例成立（2026-10-10）

実導線点検でrootの初期Latest recordにはgeneration keyがなく、別に保持した固定root anchorへ親generationが結合されることを確認。event chainのroot参照を固定anchorへ統一した。root証明を緩めず、非rootはLatestのgeneration厳密照合を維持する。実構成相当の初期root Latest placeholder例を1件追加した。

manifest1562 artifact／旧証拠508 file・parser成立。pure71例1回は0.837秒、71一致・0不一致PASS。旧70例と旧66例は保存結果との名前／Actual／Expected一致を確認。前後source／snapshot1156 file（文書4件除外）・artifact／旧証拠hash gate成立、Failure=null・外部attempt0、実CIM／event／launch／Java／Maven／Docker／root0回。section33-result-preservation.jsonへ証拠SHA・旧70例不変を保存。準備2秒＋実行等6秒＋保全1秒＝累計1874秒、残1726秒、cleanup300秒予約。code／test／POM追加変更・commit／remote0。

## 34 同種継続許可内の実環境再開手順（2026-10-10）

Start-Section34-Owner.ps1を新epochで準備。runnerはpure71例PASS時から変更0、manifest1563 artifact／旧証拠520 file・parser成立。実gateでsource／artifact／旧証拠・pure71同runner・同ユーザー管理者token／保護Java・Docker空baseline／資源・残予算を照合する。

同ユーザーOwner管理者PowerShell 7でgate→有限28例・短命java-version1回（全体60秒／launch10秒）→全成立時root追加1回（900秒／400 invocation以内、client14／Ryuk2／JVM4／PG1）→fresh XML／予定skip92→cleanup／保護Java保持の順。root成立時だけAgent証拠確認後、既承認PL2候補4件・package／再compileしない旧Consumer／既存E2Eへ残予算内で進む。他phase client8／Ryuk1・総60分・cleanup300秒予約・作業量／容量／除外対象を維持。

準備2秒、累計1876秒、残1724秒（28分44秒）、cleanup300秒予約。Agent実環境操作0。新規承認を要求せず、失敗epochは停止・保全し同種訂正を継続、最終結果受入はOwner判断。Ownerは同じユーザーの管理者PowerShell 7、Repository rootで次の1行を1回実行する。

```powershell
& '.\tmp\r0-rules-9c41aa4-20261009-first\Start-Section34-Owner.ps1'
```

ROOT_SEQUENCE_COMPLETEまたはSTOPPEDを共有する。同epochを再実行せず旧guardを消さない。

### 34.1 Owner実行：CIM未捕捉Javaの指定どおりの拒否（2026-10-10）

有限28例／短命launch・gate成立後、rootは9.007秒、PID28356のjava.exe／parent16848で停止。子・親current CIM／canonical・正式creationは未捕捉。親cmd event→既知Java27968→固定root22772のevent chainは成立、AuxiliaryTraceの拒否位置はAuxiliaryConhostだけ。先行所有外conhost／parent30484と後着Java eventは新順・別signature。補助conhost経路をJavaへ適用しない条件に従った停止であり、JavaのCIM未確認を許容して進めない。

cleanup残0・保護Java保持・source不変。fresh部分XML2 file／contract4件、failure／error／skip0。source1156 file（文書4件除外）・artifact1563件／旧証拠520 fileのhash不変をsection34-stop-preservation.jsonへ保全。Owner後1895秒＋保全3秒＝1898秒、残1702秒、cleanup300秒予約。root／skip92・後続未成立。

## 35 同種継続許可によるevent回収後のCIM観測・pure74例成立（2026-10-10）

現行main loopがCIMを取得してからeventを回収していたことをsourceで確認。停止対象Javaが実際にこのsnapshot間隙に起動したか、既に終了したかは未確定。拒否条件を緩めず、回収済みeventの後にfresh CIMを取得する共通観測処理へmain／probe／最終drainを揃えた。eventはCIM query前にPendingで受信台帳へ保存し、CIM query例外でもbatchを失わない。Add分類時の二重追加を防ぐ。

main loopの終了待機1000msを100msへ短縮。これは待機時間であり、CIM／Docker／DB／容量観測を含む実sample周期を100msと保証しない。総wall time・資源／容量上限は維持、JavaのCIM未確認／矛盾・正式creation登録条件は不変。短命Java捕捉の改善を図るが、捕捉できない場合は停止する。新provider／OS権限／dependency／Repository実装は変更しない。

旧71例不変、新3例はevent reader→fresh CIM reader順、CIM例外時の受信Pending保全、event reader例外時のCIM非実行。manifest1564 artifact／旧証拠545 file・parser成立。pure74例1回は0.846秒、74一致・0不一致PASS。旧71例の名前／Actual／Expectedは§33保存結果と一致。前後source／snapshot1156 file（文書4件除外）・artifact／旧証拠hash gate成立、Failure=null・外部attempt0、実CIM／event／launch／Java／Maven／Docker／root0回。

section35-result-preservation.jsonへ証拠SHA・旧71不変を保全。準備2秒＋実行等6秒＋保全1秒＝累計1907秒、残1693秒、cleanup300秒予約。旧epoch／guard維持、tracked code／test／POM追加変更・commit／remote0。pure成立を実root／全期間資源／完全網羅へ昇格しない。

## 36 同種継続許可内の実環境再開手順（2026-10-10）

Start-Section36-Owner.ps1を新epochで準備。runnerはpure74例PASS時から変更0、manifest1565 artifact／旧証拠557 file・parser成立。Owner同ユーザー管理者PowerShell 7で実gate（source／artifact／旧証拠・pure74同runner・token／保護Java・Docker空baseline・資源／残予算）→有限28例・短命java-version1回（全体60秒／launch10秒）→全成立時root追加1回（900秒／400 invocation以内、client14／Ryuk2／JVM4／PG1）→fresh XML／skip92→cleanup／保護Java保持の順。

root成立時だけAgent証拠確認後、既承認PL2候補4件・package／再compileしない旧Consumer／既存E2Eへ残予算内で進む。他phase client8／Ryuk1、総60分・cleanup300秒予約・資源／作業量／容量／除外対象は維持する。Javaの未確認拒否条件は維持し、未確認をPASSに読み替えない。失敗epochを停止・保全して同種訂正を継続する許可を適用し、最終結果の受入はOwner判断とする。

準備2秒計上、累計1909秒、残1691秒（28分11秒）、cleanup300秒予約。Agent実環境操作0、追加tracked code／test／POM変更・commit／remote0。新規承認は不要。Ownerは同じユーザーの管理者PowerShell 7、Repository rootで次の1行を1回実行する。

```powershell
& '.\tmp\r0-rules-9c41aa4-20261009-first\Start-Section36-Owner.ps1'
```

ROOT_SEQUENCE_COMPLETEまたはSTOPPEDを共有する。同epochを再実行せず旧guardを消さない。

## 37 R0最小検証集合・区切りと実行順の見直し（2026-10-10）

### 37.1 今回の目的とOwner指示

目的はLevel選択に対応したRule28／29と旧API互換性を検証し、Referenceを保護した配布artifactが成立することを確認すること。監視ツールの一般的な完成・PID世代／event遅延の網羅をR0の目的にしない。

Ownerは「過去作業、成果にとらわれずに、必要なことに集中」「区切りをもった見直し」と指示し、以下の7集合へフォーカスして整理することを明示した。監視網羅拡張を止める本指示は§26の同種改修継続許可より新しく、次の作業を規定する。今回は見直し文書の作成と最終停止証拠の保全だけで、改修・test・実環境操作を先行しない。

### 37.2 固定する集合と成果

| 集合 | 必要性・現在位置 | 完了の証拠／扱い |
|---|---|---|
| Rules新規40＋既存67＝107件 | R0の中心、成立済み | 固定sourceと107件XML・違反検出／公開契約の保存結果を維持。source／検証入力が変わった場合だけ影響を再評価。単独107件を習慣的に再実行しない |
| PL2既存規約4件 | 旧候補との整合確認、未実行 | Rule28And29CandidateTestの4件だけ。候補の既知の検出限界も保持、正式Rulesの追加caseやModulith runtime実験へ広げない |
| package・旧Consumer互換性 | 配布artifact／旧API利用確認、未実行 | Rules JAR／通常Reference package、新JARを先頭にした旧Consumer bytecodeの実行成立。旧Consumerを再compileしない。source／main依存不変・fixture非混入・JAR hash／descriptor／実classpathを記録 |
| Reference E2E 1件 | Reference保護、未実行 | PackagedReferenceCriticalJourneyTestの既存1 journeyを新packageで実行。Security／Audit／DB／logの既存assertionを維持。画面／認証／業務caseを新規追加しない |
| 通常root297件＋予定skip92 | 最終統合確認、未成立 | 同じsource／package epochを入力にclean verify 1 sequence、今回fresh XMLで計389 invocation候補・実行297・skip92（S1 64＋B2 28）を照合。旧XML・別epoch途中成功を足さない。failure／error0とcleanup／資源条件成立を要求 |
| 監視pure74例と今後の追加例 | 補助証拠、R0本体に加算しない | 現pure結果・失敗履歴を凍結。新case追加／監視の一般化を停止。pure PASSを実監視／通常root PASSの代用にしない |
| S1／B2専用92件 | 今回は通常buildからの分離確認 | 上記rootの予定skip集合とtest source不変を照合。専用processの再現・専用92件再検証をしない |

### 37.3 区切りを持つ実行順

| 段階 | 内容 | 区切り・次段階へ進む条件 |
|---|---|---|
| 0 実行準備 | source／artifact epochと既存107件の整合、必要な安全な実行方法・予算を確認 | 必要な資源／所有物の識別／cleanupが成立できる手順を先に選ぶ。既知失敗のtmp runnerをそのまま再使用しない。方法が用意できなければ未実行として区切る |
| 1 PL2 | 既存4件だけ | 今回XML4件・failure／error0を保存し、この段階を固定 |
| 2 package／旧Consumer | Rules／Reference packageと旧bytecode実行 | 新artifact hash・classpath・互換性・fixture非混入を記録し、この段階を固定 |
| 3 Reference E2E | 段階2のpackageで既存journey1件 | XML1件・業務／Security／Audit／DB／log・cleanup成立を保存し、Reference保護を固定 |
| 4 通常root | 全体の最終統合・予定skip92確認 | fresh全体集合・資源／cleanupを照合。成立した場合だけR0-B検証全体を完了とし、Owner最終受入へ提出 |

旧§21〜36の「root成立をPL2／package／E2E開始の必須条件にする」順を見直し、本体の独立した検証を先に固定する構成へ変更する。段階2は既存依存関係が必要とするcompile／packageを含むが、専用test・無関係な回帰testは追加しない。段階4が未成立でも段階1〜3の成立証拠は保持する。ただし、その保持をR0全体完了・Reference Level2開始・正式採用へ読み替えない。

各段階の初回は1回だけ。failure／UNKNOWN／監視不成立・予算不足では当該段階を停止・保全し、そこで結果を区切る。自動でcaseを増やしrootを反復しない。必要な原因整理を行うが、網羅拡張を再開する指示にはしない。段階間でsource／artifact入力が変わった場合だけ、影響する成立証拠を再評価する。

### 37.4 検証環境は必要最小限の実行条件へ戻す

必要な条件は、今回の実行source・起動したprocess／containerの所有・資源上限・タイムアウト・cleanup・保護baselineの保持を説明できること。あらゆるPID再利用や短命eventを分類できる汎用監視は今回の成果要求にしない。

既存の成立済み限定検証手順を再利用できるかを、用途・資源設定・Ownership・残予算の読取照合で先に判断する。再利用可能と未確認の時点で記載せず、失敗した現tmp監視を無効化するだけの実行もしない。これを新しい長期監視開発へ広げない。段階0で適用手順を選べなければ、次のroot commandやpure追加案を出す代わりに「実行方法未成立」を残課題として固定する。

上限はJVM4／PG1、通常root client14／Ryuk2、他phase client8／Ryuk1、memory8GiB／disk10GiB、raw100MiB／run2GiB、既存child heap・pool／assertion・Securityを維持する。新provider・OS権限変更・dependency取得・Reference／POM／SQL／CI等の実装変更で成立を先行しない。

### 37.5 残予算と完了判断

§36は9.087秒で一般生存identity不一致PID2860により停止。期待cmd／parent12776はcreation未確認、currentはconhost／parent13168／creation639271716171089260だった。終了や同一instanceを断定しない。Owner cleanupはprocess／container／subscription残0、保護Java24852／10280保持、source不変。fresh部分XML2 file／contract4件・failure／error／skip0、artifact1565／旧証拠557 hash不変をsection36-stop-preservation.jsonへ保存。保全2秒を計上した。

累計1930秒、残1670秒（27分50秒）、cleanup300秒予約。予算はリセットしない。実行の仮配分は段階1最大180秒、段階2＋3合計最大480秒、段階4最大600秒、cleanup／最終保全300秒、準備等110秒。計1670秒で既存の各上限を増やさない。実測・準備負担から不足が判明した段階は未実行として区切り、時間不足を理由に件数／assertion／監視条件を減らさない。計画・文章整理はtest実行ではなく、追加実行許可を消費したとは扱わない。

R0の完了判定は「規約107」「PL2 4」「package／旧binary互換性」「Reference E2E 1」「root全体／skip92」「資源／cleanup／source・artifact整合」の成立表で示す。監視74件の成功率や過去root途中成功の累積を進捗率に使わない。root内のRules107と単独107も二重加算しない。最終結果の受入はOwner判断。R1／実非同期／DoD・Gate／Phase4全体・commit／remoteの開始判断を兼ねない。

### 37.6 網羅拡張を行うタイミング

| 拡張対象 | 開始の契機 | 今回の扱い |
|---|---|---|
| Rulesの新しい反例 | 0／1／2の採用契約に具体的な検出抜けが確認されたとき | 根拠・期待違反・影響を固定した変更単位で扱う。予測だけでcaseを追加しない |
| Reference非同期／復旧／provider | R1以降の個別実装開始判断と、追加する機能の契約が揃ったとき | 現R0には追加しない |
| S1／B2専用検証 | 当該source／protocol／専用実行条件が変更されたとき | 今回は分離確認だけ |
| 一般的な監視のPID世代／event遅延網羅 | 共通検証ツールとして再利用する要件・保証範囲・専用予算を決めるとき | 別作業へ分離、現在は凍結 |

本節はOwner指定の集合に基づく見直し成果。実行順・仮配分は具体化済みだが、段階0の安全な実行方法は未選定・未検証で、段階1以降は未実行。新Owner runner／pure fixture／追加rootは作成・実行していない。次の作業は段階0の最小実行準備だけで、監視case追加へ戻らない。
