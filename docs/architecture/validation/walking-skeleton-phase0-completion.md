# Walking Skeleton — Phase 0完了レビュー

**判定:** Walking Skeleton Completed  
**最終再検証日:** 2026年8月14日  
**基準Commit:** `a58f813`（`walking-skeleton`）

## 1. 完了判定

グランドデザイン v0.2のPhase 0 DoDのうち、Walking Skeletonが担当する
「0-3: 全検証項目への回答」と「0-4: 検証結果を受けた規約調整」に必要な
実装証拠を取得し、実装不能な規約が残っていないことを確認した。実装計画 v1.0の
統合チェックリストもすべて完了したため、
Phase 0 Walking Skeletonを完了と判定する。

これはPhase 0全体の0-1〜0-8を一括して完了とする判定ではない。グランドデザインの
レビュー確定、ADR棚卸し、Ownerや見積りなどの設計統治項目は、Walking Skeletonとは
分離して確認する。

## 2. 最終再検証

| 対象 | 2026年8月14日の実結果 | 判定 |
|---|---|---|
| Root Maven Reactor | `clean verify`で9/9 project成功。37 tests、failure/error/skipはいずれも0 | PASS |
| Java build/runtime | class major version `65`、Java 21成果物がTemurin 25.0.4で起動 | PASS |
| Container | Spring Boot toolsでlayer抽出し、JREイメージ上で起動。UID/GID `999/999` | PASS |
| ArchUnit | 配布artifactの13 testsが成功し、外部Consumerの意図的違反`ADR-041`を検出 | PASS |
| Flyway | `koiki=2`、`customer=5`、履歴テーブルが独立 | PASS |
| Tier 2 practicality | 24 tests成功。OSIV境界、同期イベント、rollback、Modulith検証を確認 | PASS |
| OpenSpec | main spec 4件を`--strict`で検証し、failure 0 | PASS |
| Agent Skills | 正本2件とadapter 4件を検証し、Codex / Claude Codeの双方から呼び出し確認 | PASS |

Docker検証で作成した一時imageは検証後に削除した。Flyway検証用PostgreSQLもscriptの
`finally`処理で削除されることを確認した。

## 3. V1〜V7の証拠

| 検証 | 結論 | 証拠 |
|---|---|---|
| V1 / V3 | Tier規則を実装でき、内部参照、依存方向、イベント境界を検査できる | `walking-skeleton-archunit-distribution.md` |
| V2 | KOIKI / Customerの二階層Flyway履歴が独立して進む | `walking-skeleton-flyway-two-tier.md` |
| V4 | 小規模Tier 2を現実的な規模で実装できる | `walking-skeleton-tier2-practicality.md` |
| V5 | OSIV無効時にDTO/read model経路は成功し、Entity露出は実レンダリングでも失敗する | `walking-skeleton-tier2-practicality.md` |
| V6 | 値のみの同期イベントと受信側拒否によるrollback伝播が成立する | `walking-skeleton-tier2-practicality.md` |
| V7 | `koiki-archunit-rules`を外部projectから利用して違反を検出できる | `walking-skeleton-archunit-distribution.md` |

ビルド契約、NullAway、Java runtime、コンテナの証拠は
`walking-skeleton-build-foundation.md`、Agent SkillsとOpenSpecの責務分離は
`walking-skeleton-agent-skills.md`に記録している。

## 4. 非阻害事項

最終Reactor buildには次のwarningが残るが、Walking Skeletonの完了を阻害しない。

- 意図的なArchUnit違反fixtureに対するError Proneの`UnusedVariable`
- Mockitoのdynamic agent / self-attachに関する将来互換warning
- 一部検証時のSLF4J provider未設定warning

後二者はPhase 1aで正式なtest基盤を作る際の品質整理対象とする。PowerShell scriptが
端末の実行policyで停止する場合は、policyをPC全体へ緩和せず、検証processだけ
`-ExecutionPolicy Bypass`を指定する。

## 5. Phase 1aへの引継ぎ境界

| 扱い | 対象 |
|---|---|
| 正式成果物として再構成する | Parent / BOMの設定、Maven Wrapperとbuild-support、ArchUnit規則、Architecture Contract |
| 判断根拠として保持する | `docs/architecture/validation/`、OpenSpec main specs、KOIKI固有Agent Skills |
| 直接昇格しない | `walking-skeleton/ws-*`、`expense` / `masterdata`のJava、Template、migration SQL、一時Maven座標 |
| 後続設計へ送る | Flyway Starterの正式Ownershipと三階層化、非同期event、MyBatis、REST / Security / SPAの実装pattern |

`walking-skeleton`ブランチ全体をmainへ機械的にmergeして正式化しない。Phase 1aでは、
検証済みの設定値と判断を参照しながら、`org.koikifw`配下の正式成果物を意図的に
作り直す。Walking Skeletonの業務コードとSQLは実装例ではなく検証fixtureとして扱う。

## 6. Phase 0全体に残る確認

Walking Skeleton外には、少なくとも次の確認が残る。

- グランドデザイン v0.2の`Draft for Review`状態をどう確定するか
- DoD記載の「40 ADR」と、現行付録で確認できる43 ADR IDの差、および各ADRの
  「確定 / Phase 0で検証」分類
- 用語集、Reference Application仕様など、グランドデザインで後続作成としている文書
- Architecture Owner / Proxy、Phase規模見積りなどの一人project向け解釈

これらを別途整理しても、ここで取得したWalking Skeletonの技術的なPASS判定は変わらない。
