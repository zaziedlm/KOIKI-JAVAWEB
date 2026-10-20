# R-00：Reference Architectureのproduction限定import結果

実行日：2026-10-20（JST）

状態：**限定変更・局所検証完了／7 tests PASS**。R-01以降・通常root全件・P4-B1機能・DoDの受入を兼ねない。

## 1. 変更とsource

OwnerのR-00着手指示と[B1設計記録§7.4](../../development/phase4-b1-session-spa-mvc-design-20261020.md#74-r-00の作業範囲と実行結果)に従い、Reference test所有の変更を実施した。

- branch：`docs/phase4-review-integrated-plan-20261020`、base HEAD：`047b45780ae1352970822f419c9cff0b6ad77dfa`。文書差分を保持したworking treeで実行。
- code変更は[ReferenceArchitectureTest.java](../../../koiki-reference-app/src/test/java/org/koikifw/reference/ReferenceArchitectureTest.java)の1 file。両`ClassFileImporter`へ`withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)`を追加し、必要なimportを追加した。
- business module Rules、notificationの`LEVEL_2`選択、Framework Identity境界、対象package、既存2 assertionは維持。production／Rules／fixture配置／POM／依存／Security／CIは変更していない。新規testは追加していない。
- 実行sourceのfile SHA-256：`cda9961ccfdc1354d9a4aebcec82dc2f2d09ee6515b8713ade982ab9c37e0e9a`。patch・hashをraw記録へ保存。

変更前も[Cloud点検時の7件](phase4-r00-cloud-environment-preflight-20261020.md)はPASSしていた。本変更は、test出力をproduction検査へ含めないという責務を明示するもの。過去の15違反を現在の失敗として修正したものではない。

## 2. clean実行の結果

保存済みCloud activation・JDK21・Maven3.9.16・依存cacheを使い、次の集合をofflineで1回実行した。環境再構築・install・新規取得はしていない。

```bash
source /workspace/cloud-setup/activate.sh
./mvnw --batch-mode --no-transfer-progress -o -pl koiki-reference-app -am -Dtest=RequiredNegativeArchitectureRulesTest,ReferenceArchitectureTest -Dsurefire.failIfNoSpecifiedTests=false clean test
```

| 検証 | 結果 |
| --- | --- |
| 対象reactor | 13 projects SUCCESS、exit 0、約78秒 |
| clean compile | 既存Error Prone／NullAway設定でmain／testをcompile。Reference main150／test56 source。既存ReferenceEquality warning 2件は保持 |
| `RequiredNegativeArchitectureRulesTest` | 5 tests PASS、failure／error／skip 0。意図的な規約違反の検出を既存assertionで確認 |
| `ReferenceArchitectureTest` | 2 tests PASS、failure／error／skip 0。業務規約とFramework Identity境界を確認 |
| 合計 | **7 tests PASS、failure／error／skip 0**。指定2 classの新XMLと実行数を照合 |

他moduleに指定testがないことを許容する`failIfNoSpecifiedTests=false`で空の実行を成功認定せず、上記2 classが実行されたことを確認した。通常root全件は合流時にまとめる計画を維持し、今回は実行していない。

## 3. import対象とfixtureの責務

今回のSurefire classpathで、Repository外の一時probeから変更後と同じ両import条件を観測した。probeは製品testへ追加せず、終了後に一時source／classを削除した。

| 対象 | importされたclasses | test-classes由来 |
| --- | ---: | ---: |
| 業務規約：`org.koikifw.reference` | 178 | **0** |
| Identity境界：`org.koikifw.identity`＋`org.koikifw.reference` | 218 | **0** |

いずれも空集合ではない。class数は当該sourceの観測値であり、恒久的な固定件数assertionは追加しない。意図的な違反fixtureの検査は`koiki-archunit-rules`の既存負例testが担当し、Referenceの業務／Security test自体は無効化していない。

## 4. 保全と次作業

文書以外のtracked file 921件をCloud点検時のhashと比較し、差分は上記test1 fileだけ。production・Rules・fixture・build設定の差分なし。Docker／DB／browser・application processは今回起動していない。Mavenは終了し、probeの一時directoryも削除した。

raw記録：`/workspace/cloud-setup/logs/phase4-r00-production-import-20261020/`。`source.patch`、`source.sha256`、`clean-selected-tests.log`、指定2 classの`TEST-*.xml`、`selected-test-results.json`（XML hash・件数）、`production-import-observation.log`。base SHA＋patch＋file hashを今回のsource識別とし、未実施のcommitへ結果を付け替えない。

次は**R-01：汎用AdviceとSecurity／HTTP例外の境界是正**。R-00の追加実行・root反復・fixture再配置を次の着手条件にしない。R-01のcode変更はまだ行っていない。commit・remote操作も行っていない。
