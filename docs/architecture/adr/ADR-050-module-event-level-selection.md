# ADR-050：Architecture Rulesのmodule単位event Level選択

**状態：ACCEPTED DESIGN / IMPLEMENTATION PENDING（2026-10-09）。** Architecture Owner：Shuichi Kataoka。[R0規約レビュー案§9.2](../../development/phase4-s1-r0-rule28-29-level-selection-owner-review-20261009.md)の承認に基づく限定採用。文書commit／clean source固定・preflight前に実装しない。

## 背景

ADR-005／グランドデザイン§6.4・§17.5ではmodule連携の機能採用を段階化している。現行Rule 28はLevel選択を持たず、transactional／Modulith listenerを一律拒否する。Referenceに非同期を導入するために特定listenerの違反を除外すると、規約と実装の不一致が残る。

## 採用判断

`koiki-archunit-rules`に`ModuleEventLevel { LEVEL_0, LEVEL_1, LEVEL_2 }`と、`businessModuleRules(String, Map<String, ModuleEventLevel>)`を追加する。keyはbusiness root直下のpackage segment、未指定は現行Level 0／1拒否と同じ。旧1引数APIとFramework ownership APIを維持する。

Level 0／1はevent規約上の共通条件で同期eventを許容し、同期eventの不存在を検査しない。指定Level 2では直接`ApplicationModuleListener`だけを許容し、raw／独自meta transactional listenerと標準契約の上書き・混用を拒否する。独自module listener合成annotationは今回許容しない。選択はmodule単位で、Reference全体の一括解除やclass／method違反除外をしない。

新overloadにRule 29の同期listener→outbound Adapter直接依存制約を追加する。旧APIの規則集合・診断を維持し、Rule 1／38等を外さない。入力拒否・防御copy・import対象のmodule存在・annotation検査の詳細は採用済みレビュー案§3〜5を正本とする。

## 保証限界と適用条件

静的Level指定はModulith runtimeを有効化せず、transaction・配信・provider冪等性・復旧安全性を保証しない。Rule 29はApplication／Port経由の間接副作用や任意SDK通信を判定しないため、reviewと動作testで補う。

仕様の採用と現行artifactの利用可能性を区別する。実装・互換性／肯定負例・通常build／既存Reference保護を有限検証し、Owner結果受入・source固定後にReference R1開始票を再確定する。Framework runtime／依存変更、Customer正式採用、配布／publish、DoD・Phase 4全体開始は本ADRの承認範囲外。

## 不採用案

特定Reference listenerのRule 28違反1件を許容する案は採用しない。旧APIの既定拒否を全体解除する案、runtime profile／classpathからLevelを自動推定する案も採らない。
