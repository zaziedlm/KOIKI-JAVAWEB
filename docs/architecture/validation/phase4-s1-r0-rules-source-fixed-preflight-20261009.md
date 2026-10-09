# S1 R0-B：現行検証結果（2026-10-10）

**Rules107 PASS／通常root未成立。R0全体の受入前。** 目的に必要な結果だけを載せる。監視pure74例・反復rootの途中成功・失敗診断を本体結果へ加算しない。

| 必要な確認 | 現在の結果 | 完了に必要な証拠 |
|---|---|---|
| Rules 新規40＋既存67 | 107 PASS、failure／error／skip0 | 固定sourceと保存済みXML13ファイルの整合維持 |
| PL2既存規約4件 | 未実行 | 今回の4件XML |
| package・旧Consumer互換性 | 未実行 | Rules／ReferenceのJAR hash・依存／classpath、旧bytecode実行結果 |
| Reference E2E 1件 | 未実行 | 新packageによる既存journey1件、既存assertion・cleanup |
| 通常root297件＋skip92 | 未成立 | fresh全体XML、297実行＋指定92 skip、資源／cleanup・source整合 |
| S1／B2専用92件 | 専用再検証対象外 | 通常rootで分離を照合 |

Rules107の正規compile・XML結果は `tmp/r0-rules-9c41aa4-20261009-first/evidence/isolated-rules-tests-xml/` と `isolated-rules-tests-result.json`、sourceは `isolated-source-manifest.json` に保存済み。現在のtargetや別rootの途中XMLをその成立artifactと混同しない。source／入力変更時だけ影響を再評価する。

実行順は安全な最小実行方法の選定→PL2→package・旧Consumer→Reference E2E→通常root。読取専用preflightは準備済み、build／testの実行方法は未成立。失敗時はその段階で停止・保全し、監視拡張・root反復を自動で行わない。

累計1932秒、残1668秒（27分48秒）、うちcleanup300秒予約。最終停止後の所有process／container／subscription残0・保護Java保持・source不変を確認済み。現在のOS状態を再取得したという意味ではない。今回、検証・process／container操作・code変更・commit・remoteは実施していない。

[現行計画](../../development/phase4-s1-r0-root-connection-limit-correction-owner-review-20261009.md)に集合・区切り・資源条件を集約した。過去の承認、監視74例、途中結果と診断は[アーカイブ](../../archive/phase4-s1-r0-20261010/README.md)だけで参照する。R1／非同期runtime・DoD／Gate・Phase4全体の開始判断を兼ねない。

最小準備の静的照合：snapshot1156／元source hash一致（承認文書差分除外）、保存Rules XML13件・107 PASSを確認。classpath31／129／46 entryの参照先と旧Consumer／JDK／Maven入力を確認し、`minimal-preparation-20261010/static-readiness.json`／`readiness-inputs.json` へ保存。2秒計上済み。`Start-Readiness-Owner.ps1` は読取専用・構文確認済み、Owner環境実行前。新規case・監視pure・build／test・container起動0。
