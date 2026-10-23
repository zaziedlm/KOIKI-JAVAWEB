# P4-C2 A：外部照会の実装・限定検証結果（2026-10-22）

状態：**Aの限定検証結果はCOMPLETE / OWNER ACCEPTED（2026-10-23、§7）。** 新規30候補は29 PASS／1 SKIP（接続timeout未実証）。DoD 4-7全体の成立判断は未実施。Bは条件付き後続を維持する。以下の結果受入待ち等は各確認時点の履歴として保持する。

開始根拠：[C2-01 §9のOwner承認](../../development/phase4-c2-01-operation-candidate-selection-20261022.md#9-aの実装検証に対するowner承認2026-10-22)。Aだけの実装・有限検証を承認したもので、結果受入やcommit／remoteを兼ねない。

## 1. source・環境・所有範囲

- ローカルWindows／PowerShell、checkout `C:/Users/kataoka/Desktop/KOIKI-JAVA/KOIKI-JAVAWEB`。branch `docs/phase4-review-integrated-plan-20261020`、HEAD `6fdc2a8`。今回のcode／文書は未commit。remote `https://github.com/zaziedlm/KOIKI-JAVAWEB.git`への操作なし。
- JDK Temurin 21.0.12.1、Maven Wrapper 3.9.16、Spring Boot 4.1.1／Framework 7.0.9。既存cacheでoffline実行し、dependency／POM／Root Reactor／CIは変更していない。
- Docker client 29.5.3-rd／server 29.5.3。今回所有のPostgreSQL 17-alpineとRyukを使用。開始前から存在したPostgreSQL 15の2 containerは操作しない。
- ReferenceがAのApplication／Port／Adapter／Session GETを所有。Framework差分はAPI Starter低優先度HTTP既定値2項目とmetadataのみ。test stub・隔離DB・障害注入・package実演はTooling所有で正式JARに含めない。
- main14 file、test／helper6 fileは計画どおり。計画文書2 fileに本結果文書1 fileを加え、**成果差分は23 file**。追加1 fileは検証結果を`docs/architecture/validation/`へ記録するためで、機能範囲の追加ではない。

実装・検証のsourceは端末内manifestとdiffで固定した。IDE生成物の混入を避ける訂正検証は、同じJava／resource／POMをコピーしたsnapshotで実行した。日本語名の既存文書12件がsnapshot列挙から漏れたため、最終点検で補完した。実行に使ったJava／resource／POMの不一致はない。最終rootは元checkoutの`clean verify`で実行し、snapshot内のpackage成果物で置き換えていない。

## 2. 実装したA

`koiki.reference.external-check.enabled=true`と固定numeric loopback URIの明示設定時だけ、`GET /spa-api/v1/external-checks/rate?base=USD&quote=JPY&date=2026-10-22`を登録する。Session認証と`EXTERNALCHECK:USE`を要求する。既定無効ではclient／routeを作成しない。恒常seed・新table／migration・UI・B用POSTは追加していない。

照会はUSD／EUR→JPYの模擬rateを文字列で返す参照用サンプルであり、実provider接続や業務計算へ採用したものではない。provider URIはHTTP、numeric loopback、明示port、root pathに限定し、資格情報・query・fragment・redirectを許容しない。bodyは4 KiB以内、pair／date／正の十進文字列を検査する。

| 制御 | 実装・確認範囲 |
| --- | --- |
| HTTP期限 | Spring RestClient＋JDK request factory。`koiki.api.http.connect-timeout=200ms`、`koiki.api.http.read-timeout=300ms`を当該clientが明示採用。全clientへの自動適用ではない。1ms〜2sに限定 |
| read timeout | header停止／body停止、既定300msとbody100ms overrideで期限内失敗、再試行なし、後続正常照会・transport終了を確認。testは経過時間の下限と2s上限をassert |
| connect timeout | 200msの設定供給・clientへの設定経路を確認。所有SYN blackholeを再現できず**実効期限は未実証**。接続拒否を代替証拠にしていない |
| retry | 503変換例外だけ、追加最大2回／初回含む3試行。400／404／429、不正・過大body、timeout、割込みはretryしない。その他非200は安全な失敗へ変換 |
| 同時実行 | Application全操作に枠2、待ち行列0。第3操作は外部到達前に503。2操作の解放後に正常復帰。retry待機も枠内 |
| transaction | HTTP前の既存transactionを拒否。遅延HTTP中にapp pool active=0、独立DB照会が成立することを実DBで確認 |
| 安全な応答・相関 | 専用ProblemDetailとoperationId付き成功／失敗log。provider body／秘密を応答へ転送しない。Security例外は既存境界へ委ねる |
| 通常構成保護 | 同じJARで無効時は認証後GET404・provider要求0。有効時はSession／form loginとCSRF、503→3試行目成功、no-store、expenseデータ0件を確認 |

割込みの実証は所有workerに限定する。Servlet caller切断の即時検知、任意providerの総deadline、任意負荷での総thread／socket上限、Business Audit・UNKNOWN・副作用あり操作の再送保証は今回の証拠に含めない。

## 3. 実行結果と件数

PASS数はXMLのinvocation単位。早期集合と後続集合、通常rootに重複するtestを足して独立case数とはしない。

| 集合 | 結果 | 意味 |
| --- | ---: | --- |
| 本文停止の早期gate | 1 PASS | H09を先行選択。期限・後続照会・cleanup成立後に次集合へ進んだ |
| 早期残集合＋Architecture | 14 PASS | Contract8、H01／H12、I01／I02と既存Architecture2。先行H09を含む早期13候補とは区別 |
| Reference新規 | 27 PASS／1 SKIP | Contract8、HTTP12 PASS＋H07 SKIP、Session4、実DB3 |
| 既存直接回帰 | 89 PASS | Session Security22、Expense Session API40、Business URL25、Architecture2 |
| Framework既存 | 6 PASS | API Smoke3、Override1、API Disabled1、Resilience Disabled1。今回API Starterをlocal installし、使用JAR hash一致を確認 |
| Tooling通常入口 | 2 SKIP | 明示flagなしでpackage用testをskip。application／DB／stub非起動 |
| 固定JAR実演 | 2 PASS | 同一JARで既定無効と明示有効を直列実行。新規合計は**29 PASS／1 SKIP** |
| 通常root全test | 449 PASS／100 SKIP | XML総数549、failure0／error0。既存専用99 skip＋H07未実証1。15 moduleはSUCCESS、最後の集約moduleのcleanで失敗。単一commandのBUILD SUCCESSとはしない |
| 集約moduleだけの再開 | BUILD SUCCESS | `-rf :koiki-javaweb-fw-reactor clean verify`。0.481s、test再実行なし。root全体の再走ではない |

通常rootはSPA同梱profile・S1専用profileなし。専用99 skipを検証済みへ読み替えない。今回のJDK HTTP Tooling2件はブラウザE2Eではなく、既存Critical Journey全体・frontend build・Cloud環境の再検証は行っていない。

### 3.1 失敗・訂正・epoch

1. 初回compileで`ProblemDetail.forStatusAndDetail`へintを渡した型不一致を検出。`HttpStatusCode`へ訂正し、ISO日付指定も明示して本文gateを1件再確認した。
2. 初回局所集合は142件として終了し、failure1／error26／skip1。AのH02／H06ではimplementation method上の`@Retryable`がJDK proxyで適用されず、期待3試行に届かなかった。固定Spring 7.0.9の処理を確認し、A専用の別Bean `RetryingRateLookup`のclassへannotationを移した。Frameworkのproxy設定や既存機能を変更していない。
3. 同集合にはIDEが古いpackageで生成した`ReferenceBusinessUrlSecurityTest`が25件余分に混入し、当該25件は「Unresolved compilation problem」でerror。既存test sourceは変更せず、独立source snapshotからコンパイルして排除した。訂正集合117件＝116 PASS＋H07 SKIPでfailure／error0。最終rootのcleanでも古いclassが除去され、同問題は再発していない。
4. package fixtureは初回実行前にHMACの標準Base64と相対／絶対login redirectの扱いを点検・訂正した。production source・固定JARの変更なし。Tooling通常入口2 SKIP後、明示実行2 PASS。
5. 最終rootではAgentがroot `target`内へ実行中logを置いたため、最後の集約moduleのcleanがWindowsのfile lockで失敗した。全test・15 moduleは既に成功。証拠をroot clean対象外へ保全し、集約moduleだけを再開してclean／verify成功。**最初のroot失敗を上書きせず、全testの反復もしていない。**

初回失敗、訂正後の局所、通常root、固定JAR実演は別epochとして端末内log／XMLを保持する。105分枠内で終了し、追加のroot全反復・対象外の原因修正は行っていない。局所訂正集合自体は約3分。結果受入のための再検証許可へ残時間を流用しない。

## 4. 再現入口とartifact

実際のcommandは全てoffline、`MAVEN_OPTS=-Xmx768m`、fork heap768MiB、fork1／reuseForks=false／JUnit並列なし。Reference局所は`-pl koiki-reference-app -am`で新規4 classと直接回帰4 classを選択した。Framework6件は`-f build-support/runtime-foundation-verification/pom.xml`で上記4 classを選択。

```powershell
# 通常root：今回の単一実行は最後の集約clean失敗。再走指示ではない
.\mvnw.cmd -o -B -ntp '-DargLine=-Xmx768m' '-DforkCount=1' `
  '-DreuseForks=false' '-Djunit.jupiter.execution.parallel.enabled=false' `
  '-Dkoiki.reference.normal-root.db-limits.enabled=true' clean verify
# 実施した再開：集約moduleだけ、test再実行なし
.\mvnw.cmd -o -B -ntp -rf :koiki-javaweb-fw-reactor clean verify
# 同一JARの合流確認（SHA-256を必須にする）
.\mvnw.cmd -o -B -ntp -f build-support/reference-e2e-verification/pom.xml `
  '-Dtest=PackagedReferenceExternalCheckTest' '-Dkoiki.reference.c2-journey.enabled=true' `
  '-Dkoiki.reference.c2.jar.sha256=a6bbf763e5b00a31d86290396ebd27b05a5d10fcce3035d246d5b8985fb6b7dd' `
  '-Dkoiki.reference.verification.resource-limits.enabled=true' `
  '-DargLine=-Xmx768m' '-DforkCount=1' '-DreuseForks=false' `
  '-Djunit.jupiter.execution.parallel.enabled=false' test
```

通常JARは`koiki-reference-app/target/koiki-reference-app-0.1.0-SNAPSHOT.jar`、64,693,061 byte、SHA-256 `a6bbf763e5b00a31d86290396ebd27b05a5d10fcce3035d246d5b8985fb6b7dd`。元checkoutの通常rootから生成しsnapshotへコピー、再packageせず有効／無効2件の起動前後にhash一致を確認した。C2 Configurationあり、test fixture／SPA indexなし。

端末内Evidence rootは次のdirectory。`target`以下のためGitには含めず、他PCへpullしても移らない。再実行する場合はsource／artifact／環境を再固定する。

`C:/Users/kataoka/Desktop/KOIKI-JAVA/KOIKI-JAVAWEB/build-support/reference-e2e-verification/target/c2-run-20261022/`

| Evidence | 内容 |
| --- | --- |
| `source-before.json`／`before.patch` | 開始source／未commit差分 |
| `source-epoch1.json`／`source-epoch1/` | 訂正後snapshotと選択test出力。日本語名文書の補完は`snapshot-document-completion.json` |
| `source-final-before-results.json`／`source-final.json` | 結果文書作成前と最終source照合。前者の文書欠落を後者で補完確認 |
| `early-body*.log`／`early-set.log`／`selected-and-regression.log` | 初回compile／先行gate／初回局所失敗の履歴 |
| `selected-corrected.log`／`selected-final-tests.json` | 訂正集合117件の最終結果 |
| `normal-root-complete.log`／`normal-root-tests.json`／`normal-root-reports/` | root全testと最後のclean失敗。途中copyの`normal-root.log`を完全logと扱わない |
| `aggregator-clean-resume.log` | 集約だけの再開成功 |
| `api-install.log`／`api-installed-sha256.txt`／`framework-six.log`／`framework-final-reports/` | local Maven cache更新・使用API hash一致・既存6件 |
| `packaged-default-skip.log`／`packaged-default-reports/`／`packaged-two.log`／`packaged-final-reports/` | 通常入口2 SKIP、固定JAR2 PASS |
| `source-epoch1/build-support/reference-e2e-verification/target/c2-evidence/` | 有効／無効のsanitized application log。enabled logにoperationId付きSUCCESSあり |
| `normal-jar.sha256` | 固定JAR hash |

## 5. 資源・cleanupと残判断

JVMは各heap768MiB、局所最大2／package最大3の直列構成。新規DBはmemory1GiB／CPU1／max_connections16、局所・package pool4／idle1を実効assert。通常rootは既存pool既定とclient予算を維持した。所有stub worker3・要求64上限、同時外部操作2をfixtureで確認した。総資源を全期間連続観測した証拠ではない。

新規testのtry/finally／closeでworker・transport・HTTP server・DB・子applicationを終了。packageはPID 13808（無効）／21136（有効）の終了とport再bind、PG停止をassert。検証後のDocker一覧は開始前の`koiki_v07_postgres_unified`／`osskk_postgres_unified`だけ。新規raw logは複製を含め約3.3MB、20MiB上限内。package logはpassword／HMAC／CSRF／Cookie値とSQL出力の混入なしをtest内で検査し、temp logを削除した。

残判断は次のとおり。

1. **H07 connect timeoutは未実証。** HTTP既定値全体の実効を確認済みとはしない。必要な場合、所有できる有限blackhole fixtureと対象1件だけの追試を別に具体化する。
2. Aの選択結果のOwner受入、DoD 4-7の評価範囲、単一root command SUCCESSを要するかは本結果を入力に判断する。通常rootの全test成功と集約再開成功を、単一clean verify成功へ読み替えない。
3. BのUNKNOWN／副作用あり操作・receipt・Intent／結果Audit・再送・実providerは未実装／未実証。運用機能確定またはバリエーションの必要性発生時に別設計する。A受入の前提へ戻さない。
4. commit／push／PR、S1再開、Phase4全体の完了は今回行っていない。

## 6. Cloudでの結果確認（2026-10-23）

`docs/phase4-review-integrated-plan-20261020`、成果commit `87728b7`、未commit変更なしの状態から確認した。§1〜5の「未commit」「remote操作なし」は2026-10-22の検証・記録時点の履歴であり、現在のGit状態を示さない。

今回の確認は、開始承認・A先行設計と、commitされたproduction code・test・結果記録の静的照合。Windows端末内のraw log／XML／固定JARはGit管理外で、このCloud checkoutからは直接確認できない。以下のPASS件数は既存結果記録の引用であり、Cloudでの再実行結果ではない。Maven／Docker／HTTP検証は再実行していない。

| 確認対象 | コード・testとの対応と判断 |
| --- | --- |
| 対象と所有 | Referenceの参照専用A、独立externalcheck package、Session GETのみ。Framework差分はHTTP既定値2項目とmetadata。新依存・migration・B用POSTは追加されておらず、A先行の範囲と整合する |
| retry | `RetryingRateLookup`の別Bean／class-level `@Retryable`は503変換例外だけを対象とする。H02／H06で3試行、400／404／429・不正body・timeoutで再試行しないassertionがあり、結果記録と対応する |
| 同時実行・回復 | Applicationの`@ConcurrencyLimit(limit=2, policy=REJECT)`とH12の第3要求503・provider到達2件・解放後成功が対応する。同時実行制限の証拠であり、単位時間当たりの要求数制限や複数instanceの保証ではない |
| HTTP期限 | connect／readの設定経路を確認。H08／H09は既定read300ms、H10はread100msを実HTTPで測る構造。H07は常時assumption不成立でSKIPし、接続timeoutの未実証を明示している |
| transaction・認可 | Applicationの既存transaction拒否・method認可、Session URL認可を確認。I02は外部到達前拒否、I03は遅延HTTP中のpool active=0と独立SELECT、Session4件は401／403／許可／失効時の到達有無に対応する |
| 通常構成・固定JAR | Tooling2件は同じ指定hashのJARで有効／無効を実演する構造。無効caseはflagを明示falseにしており、未設定時のpackage実演とは区別する。未設定時の無効化はproductionの`@ConditionalOnProperty(havingValue="true")`から静的に確認した |
| 件数・build | Reference27 PASS＋H07 SKIP、Tooling2 PASSの計29 PASS／1 SKIPは候補30件と整合する。通常root449 PASS／100 SKIPと、集約clean失敗後の集約のみ再開SUCCESSは別結果として保持する |

設定検証の範囲にも留意する。`ExternalCheckContractTest`はread override、connect既定値、connectの不正Durationと不正URIを確認する構造だが、設計§7.3のconnect100ms override・retry最大追加1回overrideの動作caseは今回のtestに含まれない。retry設定の上限検査はproductionに存在するものの、その起動拒否の独立testもない。これらを検証済みの枝へ含めない。

**確認結論：** 上記の静的照合では、Aの選択結果を限定成果として受入判断へ提示することを妨げる明白な設計・結果記録との不整合は確認しなかった。ただしraw Evidenceの独立照合・Cloud再検証を完了したという意味ではない。推奨する受入範囲は、既存記録に基づくAの選択29件と通常構成保護に限定し、H07未実証・設定overrideの検証範囲・rootの分割結果を残すものとする。

Owner結果受入は未実施のまま維持する。DoD4-7全体の達成、C2全体完了、B開始、追加追試は今回の確認から自動決定しない。Bは引き続き運用機能確定またはバリエーションの必要性発生時の条件付き後続とし、A受入の前提へ戻さない。

## 7. Aの限定検証結果に対するOwner受入承認（2026-10-23）

Ownerは本チャットで「了解です、ローカルPC 手動確認の有用は理解しまして、ここではC2 Aの検証結果の受け入れ承認いたします」と明示した。§1〜5の実装・限定検証結果と§6の確認・留意事項を入力とし、**C2 Aの限定検証結果をCOMPLETE / OWNER ACCEPTED**とする。

- 受入対象はReferenceの参照専用Aについて記録された新規29 PASS／1 SKIP、既存直接回帰89＋Framework6 PASS、同一固定JARでの有効／無効実演、通常構成保護とcleanup。通常root全test449 PASS／100 SKIPと集約clean失敗・集約だけの再開SUCCESSを区別した結果として受け入れる。単一root commandのBUILD SUCCESSへ読み替えない。
- H07接続timeoutの実効期限、§6のconnect／retry override等の未検証範囲、未設定時のpackage実演と明示false実演の区別を保持する。Windows端末内raw EvidenceのCloud独立照合やCloud動的再検証を実施したという意味ではない。
- ローカルPCブラウザでの手動確認は有用性を了解した上で、今回の受入の追加必須条件にしない。手動確認を実施済みとは記録しない。
- DoD 4-7全体・C2全体・Phase4全体の達成を認定するものではない。Bは運用機能確定またはバリエーションの必要性発生時の条件付き後続とし、Aの受入や次packageへの移行条件にしない。
- 本承認は結果受入であり、追加追試・B／S1開始・commit／push／PRの開始承認を兼ねない。今回の記録反映では動的検証を再実行していない。
