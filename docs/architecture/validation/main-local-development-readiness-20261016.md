# mainの端末内開発再開検証（2026-10-16）

状態：**通常root clean verify・Critical Journey・NullAway・Feature Template検証成立。この端末での通常開発再開に必要な選択回帰はPASS。**

別端末でのPR統合後、この端末のmainが最新で通常開発に利用できるかを確認するというOwner指示に基づく新しい検証。過去のS1限定検証の実行枠を流用していない。

## Sourceと環境

- 対象：clean `main`、HEAD `af9fa01c56df372559db99e23f495323114f8f93`（PR #51 merge）。開始・終了時の`git ls-remote origin refs/heads/main`で同じhashを確認。追跡対象の作業差分は開始・検証終了時とも0。
- Windows 11、Temurin JDK 21.0.12.1、Maven Wrapper 3.9.16、Docker Server 29.5.3。systemの`mvn`はPATHになく、全Maven実行にRepositoryのWrapperを利用。
- 開始時の空きmemory約19.1 GiB、C空き約43.7 GiB、稼働container 0。既存Chromiumを利用し、browserの追加取得なし。
- sandbox内ではGitHub接続とDocker named pipeが拒否された。実行環境の権限付き承認を経て同じ読取診断と検証を実行し、Server応答を確認した。権限拒否をDocker障害とは扱っていない。

## 実行と結果

| 対象 | 結果 |
| --- | --- |
| 通常root offline `clean verify` | **BUILD SUCCESS、16/16 projects成功**。fresh Surefire XML 62 file、397件＝**305 PASS＋専用92 skip**、failure/error 0。17:34〜17:38 JST、Maven実行3分59秒 |
| 通常JARで`PackagedReferenceCriticalJourneyTest` | **1 PASS**、failure/error/skip 0。実ChromiumのSession操作、Bearer API、DB、Audit・sanitized log突合。17:45 JST、Maven実行28秒 |
| `build-support/null-safety/verify-null-safety.ps1` | **exit 0**。正常compile、意図的NullAway違反の期待失敗、正常source復元が成立 |
| `build-support/feature-templates/verify-feature-templates.ps1` | **exit 0**。Tier 1/2生成の正常系、各TierのArchUnit metadata違反とNullAway違反の期待失敗、再生成後の正常系、runtime依存境界の照合が成立。17:49 JST完了 |

通常rootはMaven/test fork heap768 MiB、fork1、reuseなし、JUnit並列無効、`koiki.reference.normal-root.db-limits.enabled=true`、JAR強制作成を直接引数で供給した。async profileと専用92件の有効化flagは供給していない。run専用の実体directoryをTEMP/TMPへ設定し、環境変数はfinallyで復元した。

E2Eは同じ通常rootでpackageされたJARを利用し、資源制限flagを明示した。実測出力はDB memory1 GiB／CPU1／max_connections16、test/child heap768 MiB、子pool maximum4／minimum idle1。通常JARのModulith events-jdbc runtime entry数は0。

NullAway・Feature TemplateはCIの既存scriptを変更せず使用した。内部の負例BUILD FAILUREはscriptが所定diagnosticを照合する期待結果であり、全体の失敗とは集計しない。両scriptではMaven heap768 MiBを供給したが、下記Wrapper制約により共通MAVEN_ARGSのfork設定が適用されたとは扱わない。

## E2E初回停止と実行引数の訂正

初回E2Eはtest JVM heapの自己検査で**1 failure**、DB起動前に停止した。期待805306368 bytesに対し実値8547991552 bytes。Agentが共通設定を`MAVEN_ARGS`へ置いたが、このRepositoryの`mvnw.cmd`は同変数を取り込まず、`MAVEN_OPTS`はMaven本体だけに適用されたことが原因である。

初回log・exit・XMLを別名で保全し、READMEのとおり`-DargLine=-Xmx768m`、fork1／reuseなし／JUnit並列無効・offlineを直接引数で供給してE2Eだけを1回再実行した。その結果が上表の1 PASSであり、初回FAILを上書きしていない。production/test/POMの訂正、期待値変更、rootの反復は行っていない。

## 証拠と最終確認

端末内証拠：`tmp/main-af9fa01-local-readiness-20261016/`。root実引数・log・exit、`root-reports/`の62 XML・集計、固定通常JAR、E2E初回と訂正後のlog/exit/XML、NullAway・Feature Templateのlog/exit、JAR静的照合、cleanup JSONを保存した。tmp資材はGitでは移送されない。

固定通常JAR SHA-256：`AB97D4D0E2D6823BCA9687D6E95E35C608858D650949FD0A91FD402B89138281`。

2026-10-16 17:50 JSTの最終読取確認で稼働container0、検証用Java／Surefire／Playwright process残存0。停止済みの他project containerは保持した。mainのremote tipは引き続き同じhash、`git diff --check`成功。本記録作成前のGit差分0であり、成果差分は本記録1 fileだけである。commit・tag・push・PR・workflow dispatchは実施していない。

この結果は、最新mainの通常構成をこの端末でbuild/testし、選択した開発者向け検証とbrowser/API journeyが成立したという判断材料である。専用92件は今回未実行であり、通常rootのskipをPASSへ加算しない。別端末の専用検証記録とはsource/artifact/端末のepochを分ける。全CI job・全runtime組合せ・非同期復旧の全経路を再検証した結果ではなく、既知B03a自動完了FAIL・E08残枝・R4/R5・実provider・全体DoD・正式受渡し・Phase 4全体の判断を変更しない。
