# S1 R0-B：必要最小限の検証計画（現行、2026-10-10）

**現行入口。Rules107成立済み／通常root未成立。** 以下の集合・順序だけを現在の作業対象とする。過去の件数・反復停止履歴を進捗に加算しない。

[現行結果表](../architecture/validation/phase4-s1-r0-rules-source-fixed-preflight-20261009.md)。過去の承認・訂正・監視履歴は[アーカイブ](../archive/phase4-s1-r0-20261010/README.md)に分離した。旧節へのリンクはアーカイブ側で参照する。

## 必要な集合と区切り

### 1 今回の目的とOwner指示

目的はLevel選択に対応したRule28／29と旧API互換性を検証し、Referenceを保護した配布artifactが成立することを確認すること。監視ツールの一般的な完成・PID世代／event遅延の網羅をR0の目的にしない。

Ownerは「過去作業、成果にとらわれずに、必要なことに集中」「区切りをもった見直し」と指示し、以下の7集合へフォーカスして整理することを明示した。最新指示により監視の網羅拡張を止める。今回の作業は文書と履歴の分離だけ。改修・test・実環境操作は行わない。

### 2 固定する集合と成果

| 集合 | 必要性・現在位置 | 完了の証拠／扱い |
|---|---|---|
| Rules新規40＋既存67＝107件 | R0の中心、成立済み | 固定sourceと107件XML・違反検出／公開契約の保存結果を維持。source／検証入力が変わった場合だけ影響を再評価。単独107件を習慣的に再実行しない |
| PL2既存規約4件 | 旧候補との整合確認、未実行 | Rule28And29CandidateTestの4件だけ。候補の既知の検出限界も保持、正式Rulesの追加caseやModulith runtime実験へ広げない |
| package・旧Consumer互換性 | 配布artifact／旧API利用確認、未実行 | Rules JAR／通常Reference package、新JARを先頭にした旧Consumer bytecodeの実行成立。旧Consumerを再compileしない。source／main依存不変・fixture非混入・JAR hash／descriptor／実classpathを記録 |
| Reference E2E 1件 | Reference保護、未実行 | PackagedReferenceCriticalJourneyTestの既存1 journeyを新packageで実行。Security／Audit／DB／logの既存assertionを維持。画面／認証／業務caseを新規追加しない |
| 通常root297件＋予定skip92 | 最終統合確認、未成立 | 同じsource／package epochを入力にclean verify 1 sequence、今回fresh XMLで計389 invocation候補・実行297・skip92（S1 64＋B2 28）を照合。旧XML・別epoch途中成功を足さない。failure／error0とcleanup／資源条件成立を要求 |
| 監視pure74例と今後の追加例 | 補助証拠、R0本体に加算しない | 現pure結果・失敗履歴を凍結。新case追加／監視の一般化を停止。pure PASSを実監視／通常root PASSの代用にしない |
| S1／B2専用92件 | 今回は通常buildからの分離確認 | 上記rootの予定skip集合とtest source不変を照合。専用processの再現・専用92件再検証をしない |

### 3 区切りを持つ実行順

| 段階 | 内容 | 区切り・次段階へ進む条件 |
|---|---|---|
| 0 実行準備 | source／artifact epochと既存107件の整合、必要な安全な実行方法・予算を確認 | 必要な資源／所有物の識別／cleanupが成立できる手順を先に選ぶ。既知失敗のtmp runnerをそのまま再使用しない。方法が用意できなければ未実行として区切る |
| 1 PL2 | 既存4件だけ | 今回XML4件・failure／error0を保存し、この段階を固定 |
| 2 package／旧Consumer | Rules／Reference packageと旧bytecode実行 | 新artifact hash・classpath・互換性・fixture非混入を記録し、この段階を固定 |
| 3 Reference E2E | 段階2のpackageで既存journey1件 | XML1件・業務／Security／Audit／DB／log・cleanup成立を保存し、Reference保護を固定 |
| 4 通常root | 全体の最終統合・予定skip92確認 | fresh全体集合・資源／cleanupを照合。成立した場合だけR0-B検証全体を完了とし、Owner最終受入へ提出 |

本体の独立した検証を先に固定し、通常rootを最終統合確認とする。段階2は既存依存関係が必要とするcompile／packageを含むが、専用test・無関係な回帰testは追加しない。段階4が未成立でも段階1〜3の成立証拠は保持する。ただし、その保持をR0全体完了・Reference Level2開始・正式採用へ読み替えない。

各段階の初回は1回だけ。failure／UNKNOWN／監視不成立・予算不足では当該段階を停止・保全し、そこで結果を区切る。自動でcaseを増やしrootを反復しない。必要な原因整理を行うが、網羅拡張を再開する指示にはしない。段階間でsource／artifact入力が変わった場合だけ、影響する成立証拠を再評価する。

### 4 検証環境は必要最小限の実行条件へ戻す

必要な条件は、今回の実行source・起動したprocess／containerの所有・資源上限・タイムアウト・cleanup・保護baselineの保持を説明できること。あらゆるPID再利用や短命eventを分類できる汎用監視は今回の成果要求にしない。

既存の成立済み限定検証手順を再利用できるかを、用途・資源設定・Ownership・残予算の読取照合で先に判断する。再利用可能と未確認の時点で記載せず、失敗した現tmp監視を無効化するだけの実行もしない。これを新しい長期監視開発へ広げない。段階0で適用手順を選べなければ、次のroot commandやpure追加案を出す代わりに「実行方法未成立」を残課題として固定する。

上限はJVM4／PG1、通常root client14／Ryuk2、他phase client8／Ryuk1、memory8GiB／disk10GiB、raw100MiB／run2GiB、既存child heap・pool／assertion・Securityを維持する。新provider・OS権限変更・dependency取得・Reference／POM／SQL／CI等の実装変更で成立を先行しない。

### 5 残予算と完了判断

停止診断・途中XML・監視74例・過去の訂正履歴はアーカイブに分離した。通常root未成立、最終cleanup確認済み、予算は下記へ引き継ぐ。

累計1932秒、残1668秒（27分48秒）、cleanup300秒予約。予算はリセットしない。実行の仮配分は段階1最大180秒、段階2＋3合計最大480秒、段階4最大600秒、cleanup／最終保全300秒、準備等108秒。計1668秒で既存の各上限を増やさない。実測・準備負担から不足が判明した段階は未実行として区切り、時間不足を理由に件数／assertion／監視条件を減らさない。計画・文章整理はtest実行ではなく、追加実行許可を消費したとは扱わない。

R0の完了判定は「規約107」「PL2 4」「package／旧binary互換性」「Reference E2E 1」「root全体／skip92」「資源／cleanup／source・artifact整合」の成立表で示す。監視74件の成功率や過去root途中成功の累積を進捗率に使わない。root内のRules107と単独107も二重加算しない。最終結果の受入はOwner判断。R1／実非同期／DoD・Gate／Phase4全体・commit／remoteの開始判断を兼ねない。

### 6 網羅拡張を行うタイミング

| 拡張対象 | 開始の契機 | 今回の扱い |
|---|---|---|
| Rulesの新しい反例 | 0／1／2の採用契約に具体的な検出抜けが確認されたとき | 根拠・期待違反・影響を固定した変更単位で扱う。予測だけでcaseを追加しない |
| Reference非同期／復旧／provider | R1以降の個別実装開始判断と、追加する機能の契約が揃ったとき | 現R0には追加しない |
| S1／B2専用検証 | 当該source／protocol／専用実行条件が変更されたとき | 今回は分離確認だけ |
| 一般的な監視のPID世代／event遅延網羅 | 共通検証ツールとして再利用する要件・保証範囲・専用予算を決めるとき | 別作業へ分離、現在は凍結 |

段階1以降は未実行。段階0は下記の読取専用preflightまで準備済み。build／testの安全な実行方法は未成立で、監視case追加や旧runner再開へ戻らない。

### 7 最小実行準備（2026-10-10）

Ownerの「必要最小限の実行準備へ進む」指示により、入力整合の確認と読取専用preflightだけを準備した。新規case・監視pure・Java／Maven／container起動は0。

固定snapshot1156ファイルと元source（文書4件の承認済み差分を除く）はhash一致。Rules保存XML13ファイルは107件、failure／error／skip0。rules／tooling／E2E classpathは31／129／46 entryが存在し、旧Consumer bytecode・JDK／Maven入力もhash台帳へ保存した。確認は再実行PASSを意味しない。初回のWindows長いpath読取を訂正した分も含め2秒計上し、累計1932秒・残1668秒となった。

| 手順 | 今回の選定結果 |
|---|---|
| 過去のPL2限定手順 | 4件選択と証拠保存の形は参照可能。固定source・timeoutと上限が異なり、script全体を再使用しない |
| 過去のroot／E2E手順 | 旧source／artifact、旧上限やcache変更を含むためそのまま使用しない |
| 現tmp event監視runner | 使用しない。追加pure／launch probeも行わない |
| 新しい読取専用preflight | 準備済み。環境・入力・予算だけを照合し、結果保存後に終了。build／test起動方法の成立は別欄で未成立を維持 |

`tmp/r0-rules-9c41aa4-20261009-first/minimal-preparation-20261010/` に `static-readiness.json`／`readiness-inputs.json` と `Start-Readiness-Owner.ps1` を配置した。scriptは同一user管理者PowerShell7、HEAD／branch／snapshot／保存入力、保護JavaのPID＋creation、Docker Server応答／稼働container0、memory／disk／raw／run容量を確認する。event登録・権限変更・build／test／container操作はしない。Docker読取commandは各10秒、全体75秒上限（同期読取処理は完了時に超過を判定）、消費時間を既存累積台帳へ加算する。初回attemptを保持し、自動再実行はしない。

PowerShell parserによる構文確認は成立。Owner環境での実行は未実施。出力 `READINESS_ONLY_PASS` は環境読取成立だけで、段階1開始・本体PASSやroot監視成立を意味しない。停止・baseline差分・予算不足時は結果を保全して区切る。次に読むものはこのpreflight結果だけで、旧監視履歴を再開入口にしない。

### 8 成果固定commitのOwner承認（2026-10-10）

OwnerはGit点検結果に同意し、「指摘の方向で、2点の直しを含めて、ローカルコミットを進めてください」と明示した。規約票§9.5の旧停止結果をアーカイブ参照へ置換し、AGENTS.md等の承認参照をアーカイブの所在へ接続する。対象はRules main5＋test／fixture11、現行／承認文書4＋アーカイブ4＝24ファイル、現在branchへのlocal commit1回。必要なcode／testは破棄しない。tmp script／raw証拠はGit対象外として保持する。

親HEADは `e8c81fe46fcc1f3de19c66fb6c5eea180831b9b7`。成果固定はRules107成立済み・通常root未成立の中間状態を保存するもので、R0結果受入・remoteを含まない。新HEADはcommit後のGitと端末内 `minimal-preparation-20261010/local-commit-result.json` で確認する。preflightの確認対象HEADだけを新HEADへ更新し、過去のRules source manifest・XML・artifact epochを上書きしない。
