# Virtual Threadsの適用条件・有効化ガイド（開発版）

**状態（2026-10-27）：** D2-03はmain統合・実CI成功を確認済み。DoD 4-11の技術条件はPASS、D2-03結果／DoDのOwner受入は判断待ち（[判定記録§6](phase4-d2-03-ci-and-adoption-guide-20261026.md#6-dod-4-11の技術判定とowner受入)）。開発版・既定無効を維持。正式release／support付き配布はE1／Phase5の判断に従う。

## 1. 確認した構成と採用の判断

Java21 target／buildで生成したReference JARを、Java21・VT無効、Java25・VT無効、Java25・VT有効で比較した。
[D2-02結果](../architecture/validation/phase4-d2-02-java25-virtual-threads-results.md#10-最終の成立範囲計測結果)は、
Java25.0.5で選択した同期Web操作・Security／Audit／context・DB poolの成立を示す限定証拠である。
main実CIではTemurin21.0.12+1／25.0.4+1を使用し、通常・観測の6構成／43項目が成功した。ローカルとCIのpatch版を区別する。
VTを全案件で使うことや高速化を目的にせず、案件が必要なときに選択できる条件を示す。

- 最低target／buildと最低runtimeのJava21は維持する。Java25で再compileする必要はない。
- VT opt-inの案内対象はJava25以上。ただしJava25より後の版、別vendor／patch・Boot／driver等の組合せは個別確認する。
- 既定無効を維持する。特定の負荷での数値を本番SLO・最大throughput・速度向上保証へ転用しない。
- 同期HTTP処理と、独自executor・非同期・Batchを分けて判断する。property一つで全threadがVTへ切り替わるとは扱わない。

## 2. 有効化と戻し方

選定したJava25 runtimeで、Spring Boot標準propertyを明示する。

```properties
spring.threads.virtual.enabled=true
spring.main.keep-alive=true
```

`keep-alive`はVTのdaemon性を踏まえた設定。非Web／scheduled処理の生存・終了条件は別途確認する。
build JDKをJava25へ変更せず、package済みの同じJARをJava25で起動する。

```bash
/path/to/jdk-25/bin/java -jar application.jar \
  --spring.threads.virtual.enabled=true --spring.main.keep-alive=true
```

無効へ戻す場合は`spring.threads.virtual.enabled=false`を明示し、通常のアプリ再起動手順を使う。
稼働中の切替・blue/green・進行中要求の扱いは案件の配備／停止手順に従う。

### 2.1 Hikari設定の入力

pool値はapplication propertiesまたは正規のコマンド引数で渡す。以下はD2の検証値であり、案件への推奨サイズではない。

```properties
spring.datasource.hikari.maximum-pool-size=4
spring.datasource.hikari.minimum-idle=1
spring.datasource.hikari.connection-timeout=3000
```

`SPRING_DATASOURCE_HIKARI_CONNECTION_TIMEOUT`等、単語ごとにunderscoreを追加した環境変数を流用しない。
D2のToolingではbinderが`connection`をネストしたBeanとして読み、`getConnection()`を呼んで接続を未返却にした。
正規引数へ統一すると起動時設定4／1／3000、要求終了後active=0が成立した。
起動直後の実値を確認し、後から設定が変わったログだけで合格にしない。
秘密の接続情報をコマンド引数・logへ載せることは、この例に含めない。

## 3. 案件で確認すること

| 観点 | 確認内容 |
|---|---|
| 実効thread | property表示だけでなく、対象の実処理でVTが使用されたことを確認する |
| 認証・context | user／権限、Session／CSRF、requestId・Auditの相関と要求後の解放。別executorへ渡す場合はTaskDecorator等の実接続を個別に確認する |
| DB・外部I/O | pool総数、接続待機timeout、transaction中の保持、driver／native依存、外部I/Oの同時実行上限 |
| 受付制限 | VTで増える要求をどこで制限するか。設計チェックリストの`@ConcurrencyLimit`等は案件で確認する。負荷生成側の並行8要求は本番制御の証拠ではない |
| 正常・失敗 | 同じJAR／fixture／設定で有効・無効の業務結果、401／403／409、timeout・復帰、DB／Auditを比較する |
| 運用 | 実負荷・配備資源でのSLO、停止・解放、監視・調査・無効化手順 |

VTはDB接続や下流サービスの容量を増やさない。pool待機・入口での制限・timeoutを組み合わせ、
Webのplatform thread poolサイズがVT有効時にも同じ受付上限になると仮定しない。
JFRのPinnedが0でも、その記録窓・閾値で未観測という意味であり、全依存でpinningがない保証ではない。

### 3.1 現在の検証限界

- 通常ReferenceにはObservability Starterを含まない。D2の詳細観測は実Starter・runtime依存を追加した構成で行った。
  案件は必要なStarterを依存として明示する。Toolingの外付けclasspath方式を正式な導入方式へ転用しない。
- 現在のfilter順ではSecurityで先に拒否される401／403に相関IDがない場合がある。D1で拒否／ERROR dispatchを含む追跡範囲を扱う。
- DB逼迫時はMVC内の500と、Session読取り例外からの前段403が混在した。全経路を5xxへ統一する保証は含めない。
- S1の独自`ThreadPoolTaskExecutor`、非同期のcontext伝播、Batch、Container／ECS、全組合せは今回未実証。

## 4. CIの位置付け

mainへ統合済みの[Virtual Threads Compatibility workflow](../../.github/workflows/virtual-threads.yml)は、
Ubuntu24.04／Java21 build・harness／Java21・25 child runtime、通常・観測計6 invocation／43 checkpointを対象とする。
同一のReference／observer JARをchecksumで固定し、runtime側で再compileしない。
既存のJava21／25 CLI互換系統と既存required checksは維持する。

PR・main push・手動起動を入口とし、job上限25分、比較10分、cleanup2分。
一回のjobはUbuntu runner一台で、並列matrixは増やさない。main実CIの対象jobは2分58秒（build53秒・比較1分52秒）。課金plan・実請求額は未確認。
artifact保持は一日で、正式配布repository／長期保存の代わりにはしない。

main `f3c79d8376d93d9f977ff2263c768b5ce32fc3ed`の[実CI run 40354524562](https://github.com/zaziedlm/KOIKI-JAVAWEB/actions/runs/40354524562)は成功。
6 PASS／0 FAIL／0 ERROR／0 SKIP・43項目・固定source／JAR照合・所有資源cleanupが成立し、DoD 4-11の技術条件を満たす。
commit・JDK版・artifact識別・ログ根拠は[D2-03記録§5〜6](phase4-d2-03-ci-and-adoption-guide-20261026.md#5-main統合と実ciの確認記録2026-10-27)を参照する。
D2-03結果／DoDのOwner受入は未記録で、D2全体完了・案件での本格採用は別判断。§3.1の検証限界を維持する。
既存8 required checksへの追加・ruleset変更は今回行わない。新VT系統の失敗はD2受入・release判断で未解決として扱う。

## 5. 根拠と更新

- [D2計画・比較条件](phase4-d2-01-java25-virtual-threads-plan-20261026.md)
- [限定結果・後続課題D2-F01〜06](../architecture/validation/phase4-d2-02-java25-virtual-threads-results.md#11-利用条件後続課題次タスク)
- [Toolingの再現手順](../../build-support/virtual-threads-verification/README.md)
- [グランドデザイン§21.5・§23.3](../architecture/grand-design/KOIKI-JavaWeb-FW_グランドデザイン_v0.2.md)

runtime／依存の更新、実CI失敗、案件採用条件の差があれば、確認した版・条件・結果と制限を更新する。
