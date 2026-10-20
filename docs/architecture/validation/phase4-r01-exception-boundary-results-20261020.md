# R-01：汎用AdviceのSecurity／HTTP例外境界の是正結果

実行日：2026-10-20

状態：**限定変更・局所検証完了／59 tests PASS**。通常root全件・P4-B1機能・DoD全体の受入を兼ねない。

## 1. 作業branchと変更

OwnerのR-01着手指示に基づき、`docs/phase4-review-integrated-plan-20261020`上で対応した。base HEADは`047b45780ae1352970822f419c9cff0b6ad77dfa`。既存R-00・文書差分を保持したworking treeで実行し、commit・PR作成・push・remote操作は行っていない。

| 所有／変更file | 内容 |
| --- | --- |
| Framework：[KoikiProblemDetailsExceptionHandler.java](../../../koiki-starters/koiki-starter-api/src/main/java/org/koikifw/starter/api/internal/KoikiProblemDetailsExceptionHandler.java) | Security例外とそのcauseを汎用500へ吸収せず再throw。`@ResponseStatus`／`ResponseStatusException`も標準resolverへ渡す。独自`ErrorResponse`はstatus・headers・ProblemDetailを保持 |
| Reference test：[ApiExceptionBoundarySecurityTest.java](../../../koiki-reference-app/src/test/java/org/koikifw/referenceacceptance/api/ApiExceptionBoundarySecurityTest.java) | 局所Adviceなし、実Security filter＋method security＋Framework Adviceの合成13件。API／MVC／HTMXの拒否、認証失敗、wrapped拒否、安全な500、権限あり成功 |
| Tooling：[ApiProblemDetailsTest.java](../../../build-support/runtime-foundation-verification/src/test/java/org/koikifw/buildsupport/internal/runtimefoundation/ApiProblemDetailsTest.java) | 既存4件に、直接／wrapped独自404、wrapped410、独自ErrorResponseの429・Retry-After保持の4件を追加。Security型がclasspathにないこともassert |

API StarterはSecurityの型を任意classpathとして解決する。Security依存・POM・公開API・propertyは追加していない。予期せぬ例外の`KOIKI-INTERNAL-001`と安全な500本文、既存JSON／validation処理は維持した。Referenceの既存局所AccessDenied再throwは除去していない。R-02／03・frontendには着手していない。

## 2. 実行と結果

保存済み`/workspace/cloud-setup/activate.sh`を使い、JDK21・Maven3.9.16・offlineで実行した。

```bash
source /workspace/cloud-setup/activate.sh
./mvnw --batch-mode --no-transfer-progress -o -pl koiki-reference-app -am -Dtest=ApiExceptionBoundarySecurityTest,ReferenceBusinessUrlSecurityTest,ExpenseApiControllerTest,ReferenceArchitectureTest,RequiredNegativeArchitectureRulesTest -Dsurefire.failIfNoSpecifiedTests=false test
./mvnw --batch-mode --no-transfer-progress -o -f build-support/runtime-foundation-verification/pom.xml -Dtest=ApiProblemDetailsTest,ApiProblemDetailsOverrideTest,ApiProblemDetailsDisabledTest test
```

Root外Toolingの実行前に、現ソースのParent・API・Data・Data JPA・Observabilityを`-DskipTests install`でlocal cacheへ準備した。依存version・設定・ソースは変更せず、外部取得はしていない。APIのtarget JARとcacheのJARはbyte一致、SHA-256は`99e1ab96e1134dabeed7de79819de7dde273d5b55808f690bcb2fba26789154e`。

| 検証class | tests | 結果 |
| --- | ---: | --- |
| 新規`ApiExceptionBoundarySecurityTest` | 13 | PASS |
| 既存`ReferenceBusinessUrlSecurityTest` | 23 | PASS：MVC／HTMX等の既存拒否応答を保護 |
| 既存`ExpenseApiControllerTest` | 6 | PASS |
| 既存`ReferenceArchitectureTest` | 2 | PASS |
| 既存`RequiredNegativeArchitectureRulesTest` | 5 | PASS |
| 拡充`ApiProblemDetailsTest` | 8 | PASS：Security依存なし、404／410／429・header／body保持、既存安全な500等 |
| 既存`ApiProblemDetailsOverrideTest`／`ApiProblemDetailsDisabledTest` | 2 | PASS：上書き・無効化の互換性 |
| **合計** | **59** | **failure／error／skip 0** |

認証済みmethod拒否は403、未認証APIは401、未認証MVCはlogin遷移、HTMXはHTML403へ戻る。独自HTTP例外を500にせず、予期せぬ例外の内部message・型名を応答へ露出しない。XMLのclass名・実行数を照合し、空の実行を成功と数えていない。

## 3. 途中経過と記録の識別

- 修正前の新規合成testは13件中9 failure（Security例外が500へ吸収）、4 PASS。変更前の失敗として保持する。
- 最初の修正compileはNullAwayがnullable戻り値を指摘したため停止。親handlerと同じ`@Nullable`へ訂正した。NullAway設定は維持。
- 最初のReference選択回帰では新規13件等はPASSしたが、既存29件はMockitoのVM attach権限で準備エラー。同一集合を権限付きで実行し49件PASS。テスト条件・Mockito設定を弱めていない。
- Tooling初回はlocal artifact不足で実行前停止。上記local install後に10件PASS。Reference検証後のAdviceの変更はimport順の整理のみで、Tooling前に再compile・installした。

raw記録は`/workspace/cloud-setup/logs/phase4-r01-exception-boundary-20261020/`。変更前／compile停止／sandbox停止／artifact不足のlog、最終2集合のlogとXML、`selected-test-results.json`、`source.patch`、新規test source、`source.sha256`を保存した。未commitのためbase SHA＋差分＋file hashをsource識別とする。

## 4. 次作業

[B1設計記録§7.5](../../development/phase4-b1-session-spa-mvc-design-20261020.md#75-r-01の作業範囲と実行結果)へ反映済み。次は**R-02：Cookie設定反映とlogout customizer生成順の確認・是正**。同branchで継続し、PR作成・pushは行わない。通常root全件は基盤是正の合流時にまとめる計画を維持する。今回Docker／DB／browser・実application起動は行っていない。
