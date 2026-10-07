# S1対象・運用証拠ハーネス：preflight・検証記録

**最新状態（2026-10-08）：環境preflightは成立。開始票D11の前提訂正はOWNER APPROVED（§5）。新規test作成・48件検証は未開始。** 訂正・承認文書commit後のclean source再固定とsource／環境差分確認を経て進める。filenameは開始票の指定を維持し、実確認日は2026-10-08とする。

## 1. 承認・clean source固定

- [開始票§9](../../development/phase4-s1-operational-evidence-harness-limited-start-review-20261007.md#9-owner承認条件付き限定開始2026-10-08)の条件付き限定開始承認を確認。Ownerが文書4件をcommitした。
- branch：`feature/phase4-s1-reference-foundation`。固定HEAD：`ea07badc5720949a2cc7b03c9f522e011183c539`（`docs: S1対象・運用証拠ハーネスの条件付き限定開始承認を記録`）。開始時clean、upstream比ahead8、`git diff --check`成功。Agentはadd／commit／pushしていない。
- 対象はtest所有の段階A。本番main／Port／schema／依存／通常構成、実運用Adapter・停止操作・provider通信・送信は変更しない。

## 2. 端末・cache・実効制限の確認（2026-10-08 JST）

| 項目 | 実確認結果 |
|---|---|
| 開始空き資源（05:08:19） | memory16,435,508 KiB（15.67 GiB）、disk47,627,243,520 bytes（44.36 GiB）。開始条件8／10 GiB以上 |
| Java／Maven | Temurin21.0.12.1、Repository Wrapper Maven3.9.16。新規installなし |
| Docker | Server29.5.3到達、稼働container0。`postgres:17-alpine` image ID `18cfe3ef5e68`、`testcontainers/ryuk:0.14.0` ID `7c1a8a9a47c7`が既存cacheにある。pullなし |
| offline compile | heap768 MiBのMavenで`-o -B -ntp -pl koiki-reference-app -am -DskipTests test-compile` SUCCESS、10.295秒。test未実行 |
| E2E POM | 同じheap設定で`-o -B -ntp -f build-support/reference-e2e-verification/pom.xml validate` SUCCESS、0.797秒。E2E未実行 |
| browser | 既存Playwright classpathの全JAR存在を確認。非配布の既存`BrowserPreflight.java`をheap768 MiB、download禁止で使用。Chromium151.0.7922.34のheadless起動、静的title、browser／driver終了がPASS |
| 隔離DB smoke | 既存`NotificationFoundationPersistenceTest#preventsNextPermitForUnclosedTarget` 1件PASS、failure／error／skip0。Maven22.608秒、command23.91秒。新規48件・全55件／40件／99件のPASSには数えない |
| smoke実効制限 | Harnessのcontainer inspect・DB SHOW・JVM・pool assertion成功。DB memory1,073,741,824 bytes、nanoCPUs1,000,000,000、max_connections16、lock／statement／transaction timeout10秒、fork heap805,306,368 bytes、用途別pool2／idle0・connection timeout10秒、connection予算8 |
| 終了空き資源（05:15:35） | memory16,857,896 KiB（約16.08 GiB）、disk47,390,539,776 bytes（約44.14 GiB）。連続資源monitorの最小値ではなく開始／終了sample |

compileとsmokeではMAVEN_OPTSを実行区間だけ`-Xmx768m`に設定し、finallyで元の値へ復元。smokeはfork1／reuseForks=false、JUnit並列無効、resource-limits有効、offline。全既存testはまだ実行していない。今回の新規classも作成していない。

通常権限ではCIMアクセス拒否とDocker named pipe接続拒否を確認。AGENTSの承認済み権限エラー手順と本限定開始承認に基づき、実行環境の権限付き承認で同じread-only診断を再実行して成功した。Docker障害とは判定していない。browserも通常権限で`spawn EPERM`、権限付きの同じ既存browser起動・終了で成功。権限拒否を迂回した実行やOS権限変更は行っていない。

## 3. cleanup・source・記録

smoke DB ID：`a5b16cc6bdbe8716c7df4c454acbb6011d231639c7722a3342dd7378de5025f0`。test終了直後にはRyukが短時間残っていたが、後続read-only診断で当該DB不存在と稼働container0を確認。既存停止containerを変更していない。

browser helperは`browserAndDriverClosed=true`を出力。最終process診断でchrome-headless-shellなし、当該BrowserPreflight／Surefire／Maven processなしを確認。既存Java2件は前日12:02起動・VSCodeと同じparentのprocessで、操作していない。引数全体や秘密は出力しない。

HEADは固定値を維持し、本番main tracked145件について`git diff --name-only -- koiki-reference-app/src/main`は0件。新規test／Harness変更もなし。preflight後に生じた差分はD11訂正文書・AGENTSと本Evidenceだけ。

非配布rawは`build-support/reference-e2e-verification/target/operational-evidence-20261007/preflight-20261008/`のcompile／E2E validate log、sanitized DB smoke log／XMLへ保存。確認時合計69,797 bytes、1 GiB以内。後段runのraw予算へ含める。DB smoke XMLからsystem-properties／system-out／system-errを除去した。

実効制限の意味は開始票に従う。heapはprocess全memory、CPU1は端末全process CPU、transaction10秒は全外部I/O／JVMの強制停止を保証しない。新規D08の2 task・失効台帳・証拠追跡は未実装／未検証で、既存smokeから実証済みとしない。

## 4. D11の前提訂正と次の条件

preflightのsource確認で、Agentが開始票D11に記載した「同一対象へ複数permitが発行可能」という前提誤りを発見した。実際にはV4の`uk_kkref_notification_permit_unclosed_target`がenvironment・publicationごとの未閉鎖許可を1件に制限する。今回の既存smokeでも、未閉鎖・消費後・期限経過後の次許可拒否を確認した。

[開始票§10](../../development/phase4-s1-operational-evidence-harness-limited-start-review-20261007.md#10-preflightでのd11前提訂正2026-10-08owner確認待ち)に訂正案を記録。D11は既存一意保証と古い対象閉鎖の未達を分けて検証し、別publicationにまたがる同じ論理通知の横断抑止を未達として残す。5 class／48件、main変更なし、上限は変更しない。

環境条件は成立したが、承認されたD11と異なるtestを無断で作成せず、本訂正箇所のOwner確認後に新規ハーネス作成へ進む。同じ承認済み範囲の全面的な再承認は求めない。実装・検証結果のOwner受入、実運用接続／DoD／remoteは未成立。

## 5. D11訂正承認と開始条件（2026-10-08）

Ownerは[開始票§11](../../development/phase4-s1-operational-evidence-harness-limited-start-review-20261007.md#11-d11訂正のowner承認2026-10-08)で§10の訂正を了承・承認した。§4の確認待ちは解消し、訂正後D11・新規48件と既存承認条件を採用する。preflightの成功・制約・raw記録は変更しない。

次は訂正・承認記録を含む文書4件のcommitによるclean source再固定。その後はsource／環境差分を確認し、同じ承認範囲のハーネス作成へ進む。main／test変更や新たな懸念がないcompile／browser／既存smokeは無条件に再実行しない。Agentはgit add／commit／pushを行っていない。
