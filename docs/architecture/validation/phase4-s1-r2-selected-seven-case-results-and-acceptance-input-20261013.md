# S1 R2後段7件の選択検証結果と受入判断入力（2026-10-13）

状態：**承認済み後段7件は7／7 PASS。R2限定結果はCOMPLETE / OWNER ACCEPTED（§5）。** 受入記録に伴うcode／test修正、commit／push、追加再実行は行っていない。

## 1 承認・固定入力・preflight

Ownerは[後段7件の限定開始判断案](../../development/phase4-s1-r2-post-jar-pair-seven-case-owner-review-20261013.md)を確認し、「Owner判断を確認し、承認します。次へ進めます」と明示した。同案の7 invocation・順序、固定source／JAR、新規30分・cleanup5分予約、資源・停止条件を採用した。

branchは`feature/phase4-s1-reference-failure-isolation-r2`。実行sourceは原固定commit`cfe9d9efb4cb2bbcdc273df578c75225b539bc08`だけでは表せず、局所再throwを含む未commitの`tmp/r2-local-rethrow-correction-20261013/source/`と`source-inputs.json`の1201 fileで固定した。全file hashは開始前・終了後とも不一致0。実行epochは`tmp/r2-seven-case-20261013/`。

| artifact | 今回runの固定copy／SHA-256 |
|---|---|
| 通常 | `normal-reference-fixed.jar`／`15C490E3439FFA43DE4E686F04F7A730698DE9B09150641A160990BA35695296` |
| async | `async-reference-fixed.jar`／`948F0C79F5D8354826A25A47015C0AD9671843B84911899B390E69DF21C15FA6` |

C01が参照するsnapshotのReference target JARも通常固定hashと一致した。空きmemory12.25 GiB／C空き496.44 GiB、Docker Server29.1.3・稼働container0、PostgreSQL17-alpine／Ryuk0.14.0のimage実体、保護対象Java30304／25248のcreation一致を開始前に確認。Playwright1.62.0用のChromium／headless-shell revision1234 cache実体を確認し、追加downloadなし。新epoch専用JVM tempのJava `toRealPath().equals(path)`はtrue。

## 2 7件の実行結果

7件を承認順に直列実行した。各caseは**1 Maven invocation／1 fresh XML／tests1・failure0・error0・skip0**。同じtest classのXMLが次の実行で上書きされる前に、各XMLを今回runへ別名でcopyした。各invocationの前にsource1201 file・両JAR・C01 targetのhashを再照合した。Maven本体／test forkはheap768 MiB、test fork1。Toolingの起動flagとJAR pathは[開始判断案](../../development/phase4-s1-r2-post-jar-pair-seven-case-owner-review-20261013.md#3-入力固定preflightと証拠)に従った。

| 順 | case | 実証結果 | fresh XML SHA-256 |
|---|---|---|---|
| 1 | 通常JAR能力拒否比較 | **PASS**。実method security拒否403、SUBMITTED／version2保持、成功承認Audit0、async schemaなし、stub到着／受理0 | `71C4A1D57998E84B7A24FA3256BF3953BDF40EF43807ECAA3B6BAB99B564AD31` |
| 2 | R2-04 async JAR能力不足 | **PASS**。403、状態／version保持、成功承認Audit・publication・stub到着／受理0 | `39346F9F60CC44D2BC2AF7B22C316A1EDEEFE2098EB588140CA30D1444D6F523` |
| 3 | D01 provider同一要求 | **PASS**。同一key／payloadの元receipt再利用 | `F4704BAC80085F4B399067C0930397760D3BA804775938A9D5B9C10CBCA228F9` |
| 4 | D02 provider異なるpayload | **PASS**。409拒否、元receipt不変 | `2B57AEABC42F70A0A0DA9BDDFBEFB07FAE8A352343A8BDE1CC8DB84B0730A61D` |
| 5 | C02 async JAR通常起動 | **PASS**。承認成立と非同期無効時のpublication不作成 | `984401CCA8EEDCA75A80FA37C5129D7268197BAB077BCFD00C653794FEE6FE3F` |
| 6 | C04独立承認2件 | **PASS**。各通知とpublication完了の対応 | `075D5108F709B6BB4369B2C944A28F7F3BF1B20A0A555D35FBA747C76A1F3AC1` |
| 7 | 既存Critical Journey C01 | **PASS**。固定通常JARのbrowser／API／Security／LOGIN Audit／DB／log契約 | `574A8F4BD1679252C2EE4BEA8137A5147AF55D86BE123F0D0F59E5E7B9518258` |

`01-normal-capability`〜`07-c01`の`*-fixed.xml`、`*-result.json`、`*-maven.log`を別epochへ保存し、全件indexを`epoch-result.json`へ固定した。case別Maven実行時間の合計上限は108秒、preflight／cleanup／記録を含むepochの保守的経過上限は記録時874秒で、新規30分・cleanup5分予約の範囲内。再実行0、root／JAR作成0。

## 3 cleanupと観測限界

各case終了時に稼働containerとJavaを読んだ。R2-04直後はcontainer1件が一時残ったが、後続読取時には一覧から消え、同IDのinspectも`no such object`となった。force停止・他container操作はしていない。他の集合後と最終読取では稼働container0、Javaは保護対象30304／25248のみ・creation不変更。最終Chromeプロセスは今回開始前の10:49 JST作成分で、今回のbrowser子processは残っていない。有限の開始前・集合後sampleであり、実行中のRyuk／DB client数の連続上限を実証したとは扱わない。

今回7件の成立は、前回のDocker `Bad chunk header`の根本原因や将来の再発防止を確定しない。R2-04の以前の500失敗と今回403 PASSは異なるsource／artifact epochで保全し、初回のFAIL記録を上書きしない。局所再throw後にR2の全8件を同一epochで一律再実行した結果とはしない。

## 4 R2限定受入への対応づけ

[初回R2選択検証](phase4-s1-r2-implementation-and-selected-verification-20261013.md#3-初回case結果)は正常対照1件と失敗分離7件の計8件中、7 PASS／R2-04のみFAILだった。今回のR2-04は、同じ期待403と副作用なしの未到達assertionまでPASSした。局所再throw後の[通常root・JAR対照](phase4-s1-r2-local-rethrow-correction-and-root-stop-20261013.md#10-9のowner承認と限定再実行結果2026-10-13)と今回の通常／async比較・直接回帰・Critical Journeyを、source／artifact／epoch別の限定EvidenceとしてOwner判断へ提示できる。

R1の受入結果を再定義しない。実provider配信、障害時の再送・復旧、R3〜R5、正式受渡し、Phase 4全体の開始は今回の受入範囲外。Framework共通例外handlerの依存変更や一般解決を今回の局所訂正から推認しない。

**次の判断：** Architecture OwnerによるR2限定結果の受入可否、成果固定対象とlocal commit／remoteの別判断。現時点でR2を`COMPLETE / ACCEPTED`と記録せず、commit／pushは未実施。

## 5 R2限定結果のOwner受入承認（2026-10-13）

Architecture Ownerは本結果文書を開いた状態で「R2限定結果 については、これを承認いたします」と明示した。これにより、§4のsource／artifact／実行epochを区別したR2選択失敗分離、局所再throw後の通常root303実行＋専用92skip、通常／async JAR静的対照、後段7件の成立とcleanup観測を、**COMPLETE / OWNER ACCEPTED** とする。初回R2-04の500 FAILと過去のroot停止は上書きせず、訂正後の403 PASSと区別して保持する。全8件を同一epochで一律再実行した結果とはしない。

受入は、承認済み最小集合が示した通知失敗時の承認保持、拒否・rollback時の副作用抑止、provider stubの直接契約、および通常構成保護の限定結果に限る。実provider配信、停止後の再送・復旧、UNKNOWN照合、R3〜R5、DoD 4-1全体、正式受渡し、Phase 4全体の開始・受入へ拡張しない。Docker応答読取エラーの根本原因や将来の非再発も確定しない。

この受入は成果固定local commit、remote push／PR／merge、追加検証の実行承認を兼ねない。次はGit差分と成果固定対象を点検し、local commitは別承認／Owner操作、remoteは個別承認に従う。§4末尾の「受入未判断」は判断前の記録であり、本節を現行判断とする。

## 6 Git差分と成果固定対象の点検（2026-10-13）

branch `feature/phase4-s1-reference-failure-isolation-r2` の差分は**18 file（追跡済み変更8、新規未追跡10）**。R2開始・停止・訂正・後段検証・Owner受入までの成果として、次をlocal commit候補とする。点検時点でstaging・commit・pushはしていない。

| 区分 | 対象・目的 | 件数 |
|---|---|---:|
| Reference | `ExpenseWebExceptionHandler.java`の用途限定AccessDenied再throw、`ReferenceBusinessUrlSecurityTest.java`のMVC3件 | 2 |
| Tooling | `AsyncReferenceFixture.java`、`LocalNotificationProviderStub.java`、新規`PackagedReferenceAsyncFailureIsolationTest.java`、新規`R2DatabaseFailureInjection.java`。選択失敗分離・観測・隔離DB注入 | 4 |
| Agent／段階導線 | `AGENTS.md`、`docs/agent/skills/koiki-project-overview/SKILL.md`、`docs/agent/skills/koiki-business-feature-work/SKILL.md`、`docs/development/phase4-s1-purpose-dod-stage-mapping-20261009.md`。現行受入と後続境界 | 4 |
| R2判断・検証文書 | `docs/architecture/validation/`の`phase4-s1-r2-source-and-preflight-20261013.md`、`phase4-s1-r2-implementation-and-selected-verification-20261013.md`、`phase4-s1-r2-local-rethrow-correction-and-root-stop-20261013.md`、本書。`docs/development/`の`phase4-s1-r2-access-denied-correction-options-20261013.md`、`phase4-s1-r2-denial-origin-assessment-20261013.md`、`phase4-s1-r2-local-rethrow-limited-correction-start-review-20261013.md`、`phase4-s1-r2-post-jar-pair-seven-case-owner-review-20261013.md` | 8 |

tracked差分の`git diff --check`は問題なし。新規10 fileも競合マーカー・末尾空白0を確認した。`tmp/`配下のXML・log・runner・固定JAR・JSONは`.gitignore`の対象で、Git移送・commit候補に含めない。Framework本体、POM／依存、migration、Security設定、CI／workflowの差分はない。初回FAIL／停止文書の当時の状態欄は履歴であり、本書§5を現行受入とする。

## 7 R2成果固定対象とlocal commitのOwner承認（2026-10-13）

Ownerは§6の18 fileと結果を確認し、「これを承認します。次はローカルコミットでしょうか」と明示した。§6に列挙した18 fileだけを同一branchでlocal commitする判断として受け、対象外file・`tmp/`・remote push／PR／mergeを含めない。commit前にはbranch・対象数・stage済み差分を再照合し、commit後にはIDとclean状態をGitで確認する。追加検証やR3開始はこの判断に含めない。
