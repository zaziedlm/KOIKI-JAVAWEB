# 依存関係の制御の論点整理（2026-10-01）

**状態:** OWNER APPROVED — 文書承認 / 2026-10-02。論点整理は完了。制御方式・追加検査の採用と実装は未決定。
**位置付け:** [Pre-Phase 4作業順序案§3](pre-phase4-framework-independent-work-review-20260930.md#3-framework側で単独に進める作業と順序案)の作業3。作業4bへ渡す分析資料。
**Ownership:** Framework側の採用支援文書。Customerの依存追加とCI運用はCustomer所有。
**確認baseline:** `docs/daily-development-workflow` / `27017ca`。POMとArchitecture Rulesのsourceを確認した。新しいfixtureによる実測は未実施。

## 1. 問題と整理の結論

[説明会§8.2](p4-ar6-application-team-briefing-20260928.md#82-未確定事項とリスク)の「開発者がPOMへ依存を追加してFramework制約を回避できる可能性」を、次の3つに分ける。

- **依存の解決:** Parent / BOMはbuild設定と管理versionを提供する。Customerが追加できるlibraryの一覧や、変更不可能なversionを定める仕組みにはなっていない。
- **利用コードの検査:** Architecture Rulesはimportしたclassの依存・annotation・package構造を検査する。POMの追加そのものや、すべての代替技術の利用を検出するものではない。
- **採用と運用の判断:** buildやArchitecture Rulesが成功しても、技術選択、Security契約、support対象の変更が承認されたことにはならない。

2026-10-02のOwner reviewで、アプリチームの懸念は、**Framework由来のモジュール／BOMをCustomer appの`pom.xml`へ不用意に記載し、アプリの構成に適さない選択をしてしまうこと**と確認した。Customer側が記載と利用の責任を持つことを前提に、Framework側で誤選択を防ぐ制約を設けられないか、またはアプリ型に対応したFramework定義を選び、そのType宣言だけをCustomer側へ記載する方式にできないかを検討する（§4.1）。

依存変更のreviewと既存検査の実行は運用上の基本案とし、上記の誤選択防止・構成選択の仕組みも比較対象に加える。全libraryの許可リスト化を先に決めない。制御方式の採用は未決定である。

## 2. 現行実装から確認できること

| 対象 | 確認した事実 | 保証の限界 |
|---|---|---|
| [KOIKI Parent](../../koiki-parent/pom.xml) | KOIKI BOMをimport。Enforcerの`requireMavenVersion`と`requireJavaVersion`、compilerのError Prone / NullAway設定を提供 | `bannedDependencies`等の追加依存を禁止するruleはない。Customerの設定変更を全面的に封じる仕組みではない |
| [KOIKI BOM](../../koiki-dependencies-bom/pom.xml) | KOIKI座標、Spring Boot / Modulith、MyBatis等のversionを管理 | 管理対象の列挙は利用許可リストではない。BOMだけをimportしてもParentのbuild plugin設定は引き継がれない |
| [Architecture Rules入口](../../koiki-archunit-rules/src/main/java/org/koikifw/archunit/KoikiArchitectureRules.java) | `businessModuleRules`と`frameworkOwnershipRules`を提供 | Customer testが両方を正しい対象packageで実行することが必要 |
| [業務構造ruleの実装](../../koiki-archunit-rules/src/main/java/org/koikifw/archunit/BusinessModuleRuleSet.java) | MyBatis宣言の拒否、RestTemplate依存の拒否、commit後listenerの拒否等を検査 | 座標単位の依存禁止や、全実装方式の意味的な判定ではない |

Mavenでは、子POMの依存宣言・dependencyManagement・exclusionにより解決結果を変更できる。BOM管理下のversionも、Customer側の明示指定等によって変更できるため、実際の結果はeffective POMとdependency treeで確認する（[Maven公式：Dependency Mechanism](https://maven.apache.org/guides/introduction/introduction-to-dependency-mechanism.html)）。

MyBatisはBOMに管理versionがあるが、現行の`@KoikiModule`で`MYBATIS`を宣言するとRule 008の違反となる。libraryを追加するだけではこの検査は発動しない。MyBatis採用trigger、分離モデル、規約fixtureの承認境界は維持する。

## 3. 変更例と検出範囲

以下はsourceから整理した範囲であり、変更POMを使った実行結果ではない。Architecture Rulesの詳細と対象外は[説明§5](architecture-rules-developer-guide.md#5-ルールが検出しないもの)を参照する。

| Customer側の変更例 | 現行検査の扱い | 補う確認 |
|---|---|---|
| 新しい業務用libraryをPOMへ追加 | 追加だけではArchitecture Rulesの違反にならない | 目的、代替案、transitive依存、license・脆弱性、運用責任をCustomer側でreview |
| Spring / KOIKI等のversionを明示指定、依存をexclude | version変更・除去自体をArchitecture Rulesは検出しない | effective POM / dependency treeの差分、互換性test、Framework側への影響確認 |
| MyBatis starterを追加 | library追加だけではRule 008の違反にならない。`@KoikiModule`のMYBATIS宣言は違反 | 宣言と実利用の一致をreview。JPA宣言のまま別経路を導入することを適合と扱わない |
| RestTemplateを利用 | 検査対象の業務classの直接依存はRule 012が検出 | 他のHTTP client、reflection、対象外package等は別途review。HTTP Service Interface方針への適合を確認 |
| Modulith runtimeを追加し、非同期処理を導入 | 依存追加自体は検出しない。対象methodの`@TransactionalEventListener` / `@ApplicationModuleListener`はRule 028が検出 | 任意のexecutor等による非同期化、運用・transaction意味はreviewとruntime test。Level 2採用判断を先行しない |
| Framework `internal`を直接参照 | `frameworkOwnershipRules`のRule 013が検出 | testの呼出し・import対象が正しいことを確認。reflection等の間接利用はreview |
| Architecture testを削除・無効化、対象を狭める、testをskip | 実行されない検査は違反を報告できない | Customer CIとreviewで検査実行・対象を確認。必要な承認者と強制方法はCustomer側と合意する |
| Security設定や独自filterで既定挙動を変更 | Architecture Rulesだけでは認証・認可等の正しさを保証しない | default deny、CSRF、Header、Identity、Audit等の契約に対するHTTP / runtime testとreview |

依存追加は正当なCustomer拡張にも必要である。追加依存があることだけでFramework制約違反とはしない。目的と変更後の利用方法、契約への影響で判断する。

## 4. 制御方法の比較と検討案

| 方法 | 効果 | 限界・追加判断 |
|---|---|---|
| POM変更のreviewと依存解決結果の記録 | 新規library、version上書き、exclusion、plugin変更をまとめて判断できる | review責任者と例外手順、確認Evidenceを合意する必要がある |
| 既存Architecture Rulesと契約testをCustomer CIで実行 | 利用コードの構造違反と、testで表現した振る舞いの回帰を検出 | CI実行を止められる権限や対象変更は別途運用で制御。Customer CIの現状は未確認 |
| Enforcer等で特定座標を禁止 | 禁止libraryを直接・推移依存から検出できる | 正式な禁止対象、scope、例外、配置先とOwnerを決める必要がある。利用方法や振る舞いまでは判定しない |
| version整合・依存差分の追加検査 | 管理baselineからの逸脱や競合を可視化できる | 互換性を保証するものではない。許容する上書きと例外を定義する必要がある |
| 全libraryの許可リスト | 未登録libraryの導入を止められる | 推移依存・更新・Customer固有libraryの維持負担が大きい。現段階の基本案にはしない |

座標禁止の機能例は[Apache Maven Enforcer：Banned Dependencies](https://maven.apache.org/enforcer/enforcer-rules/bannedDependencies.html)を参照する。これは追加検査の候補であり、現行Parentで有効な検査ではない。

推奨する検討順は次のとおり。

1. Customer側で依存変更の目的・影響・解決結果をreviewし、既存Architecture Rulesと必要な契約testを実行する運用を相談する。
2. 実際の変更や問い合わせから、検出漏れ・繰り返し発生する問題を特定する。
3. 機械検査が有効な対象だけを選び、Toolingで検証する範囲、Owner、例外処理を提案する。
4. Evidenceを踏まえ、Customer側のCI支援に留めるか、Framework Parent / Rulesへ追加するかを個別に判断する。

### 4.1 アプリ型に応じたFramework構成の選択と誤記載防止（追加検討案）

**追加日:** 2026-10-02。Ownerが提示したアプリチームの懸念を検討対象へ追加した。方式の採用承認ではない。

Customer開発者がFramework由来のモジュール／BOMを個別に選んでPOMへ記載する際に、用途に合わないものや組合せを不用意に指定してしまう懸念がある。個別記載の責任はCustomer側にあるが、Framework側の定義・検査で誤りを減らせるかを検討する。

| 比較する案 | 狙い | 検討事項 |
|---|---|---|
| 個別記載に対する制約・検査 | アプリ構成に適さないモジュール／BOMや組合せを検出する | 対象座標、BOM importと実依存の検査範囲、必須・禁止・併用条件、例外、検査を実行する場所 |
| アプリ型に対応するFramework定義とType宣言 | Customer側はアプリ型を選ぶ宣言を記載し、対応するモジュール構成をFramework側の定義から利用する | 型の分類、宣言方法、BOM / Parent / Starter等の役割分担、追加・除外・version上書きの扱い、個別記載を併用した場合の検出 |
| 構成選択と検査の組合せ | 選択を簡単にするとともに、選択した型からの逸脱を検出する | 宣言と実際の依存構成の一致、Customer固有の拡張、更新時の互換性と保守負担 |

ここでの「Type」はアプリ型を選択する概念上の宣言を指す。Mavenの`<type>`要素をそのまま使う意味には固定しない。具体的な型名、Maven座標、POM記法と実現方法は未定義とする。

検討時には、次を明らかにする。

- 懸念の対象がBOMのimport、Starter / moduleのdependency追加、またはその両方か。実際に誤選択が心配される座標とPOM例を確認する。
- アプリ型ごとに必要・任意・禁止となるFramework構成と、Customer固有依存を追加できる範囲。
- 型の宣言だけでどこまで構成できるかと、検査を組み合わせて初めて制約できる範囲。宣言の簡略化と強制力を別々に評価する。
- Framework定義と検査の保守Owner、Customer側の例外手続、型を変更・更新する際の互換性確認。

型別構成定義がProject Templateに相当する成果物を必要とする場合は、Phase 5境界との関係を別途整理する。現段階では比較・設計論点の追加に留め、新しいBOM、Parent、Starter、Maven module、検査を実装しない。

## 5. 依存変更reviewで残す情報（案）

- 変更する座標、version、scope、exclusion、plugin設定と、目的・標準機能で代替できない理由
- effective POMとdependency treeの変更前後の差分（認証情報や端末固有pathは除く）
- Framework baseline、利用package、Security・transaction・migration・配布への影響
- Architecture Rulesと契約testの実行結果、必要な互換性確認
- Customer側の保守担当、例外の理由・期限・見直し条件、Framework側への相談要否

本様式を全変更の必須手続とするか、どの変更をFramework側のreviewへ戻すかは未決定。Customerの通常の業務依存追加とFramework契約変更を同じ承認手続にすることは先に決めない。

## 6. 作業4bへ渡す論点とOwner確認事項

| 論点 | 次に判断すること | 現状 |
|---|---|---|
| Framework構成の誤選択防止 | モジュール／BOMの個別記載への制約、アプリ型に対応するFramework定義とType宣言、両者の組合せを比較するか（§4.1） | Owner reviewで検討対象へ追加（2026-10-02）。方式・型・宣言方法は未決定 |
| 採用支援での扱い | 依存変更reviewと既存検査の実行を、まず運用案としてアプリチームへ提示するか | 提案。チーム合意未取得 |
| Framework側への戻し条件 | 管理versionの変更、Starter除外、Security契約変更、MyBatis / Level 2等の採用変更をどこまで必須相談にするか | 方針未決定。既存Gateは維持 |
| 新しい機械検査 | 具体的な禁止対象・逸脱検査が必要か、Tooling検証を別タスクにするか | 実装未承認 |
| 配置・責任 | Customer CI、支援Tooling、正式Parent / Rulesのどこに置くか | Customer運用の入力とOwner判断が必要 |
| Phase 4 packageへの対応 | 採用支援の論点として保持するか、具体的なFramework gapがある場合に本体packageへ追加するか | 作業4bの材料。新しいpackageは未確定 |

作業3はこの論点整理までを対象とする。Parent / BOM / Public API / Architecture Rules / CIの変更、方式採用、正式配布、Phase 4開始を承認するものではない。

## 7. Owner review承認記録（2026-10-02）

Architecture Ownerは、§4.1の追加検討案を含む本書全般の内容を確認し、論点整理の文書として承認した。作業3は論点整理の文書化までを完了とする。
この承認は特定の制御方式の採用、アプリチームの運用合意、追加検査やFramework成果物の実装承認を意味しない。

次回は別端末へ引き継ぎ、今回の経緯とアプリチームの懸念を起点に案を深掘りし、実効性のある方式にするための分析・設計へ進む。
再開手順、検討順序、未決事項と承認境界は[次回作業引継ぎ](dependency-control-next-session-handoff-20261002.md)に記録する。
