# Phase 4 S1 B2受入成果の固定・次作業引継ぎ（2026-10-08）

状態：`OWNER APPROVED LOCAL FIXATION`。B2初回issue／readは`COMPLETE / OWNER ACCEPTED`。本書は受入成果の所在・固定対象・再開条件を整理する。後続consume／close・送信／復旧の実装開始票ではない。

## 1. 現在のsourceと正本

- branch：`feature/phase4-s1-reference-foundation`
- B2開始基点：`03210795a8ffd146d0bcc10c9d1da4d62a69308c`
- 実装固定commit：`d70b883a1edface5f524272e564f715afb1bab25`（18 file）。受入記録・本引継ぎは次の文書commitへまとめ、2コミット後のHEADを次のsource基点とする。
- [B2開始票](phase4-s1-stage-b2-issue-read-limited-start-review-20261008.md)：初回§13、訂正／追加範囲§15〜§30、結果受入§31。
- [検証Evidence](../architecture/validation/phase4-s1-stage-b2-issue-read-20261008.md)：結果§17、完了§18、Owner受入§19、総評§20。
- [固定manifest](../architecture/validation/phase4-s1-stage-b2-fixed-evidence-manifest-20261008.json)：source18 file、artifact4件、証拠index266件と最終audit抜粋。

manifestは現端末のraw byte SHA-256。GitのCRLF正規化後のcheckoutではbyte hashが変わり得るため、Git blob／source commitと保存raw hashを区別する。manifestは署名された真正性証明ではない。文書は受入前の未完了履歴を残し、後段承認を現在状態として読む。

## 2. 受入集合・保証範囲

| 集合 | 件数 | 証拠の採用条件 |
|---|---:|---|
| 新規Tooling | 18 | Tooling-firstの訂正gate、2 class result／XML |
| 新規Reference登録 | 8 | Reference-registration-first result／XML |
| 新規Reference issue／境界／transaction | 28 | Reference-isolated-r2のI12／B8／X8 result／XML |
| 非影響回帰 | 189 | Regression-r2-first／continueの選択結果。aligned/reused-resultsで集合を特定 |
| 整合済み回帰 | 111 | Regression-artifact-alignedの27 class。baseline99＋registration7＋migration5 |
| packaged E2E | 1 | E2E-finishの初実行 result／fresh XML／exit |
| 合計 | 355 | 新規54＋回帰301。保存結果とartifact整合後結果を区別 |

indexには失敗attempt、訂正前summary、旧artifact結果も含まれる。全index fileをPASS結果として数えない。採用gate・非影響条件・置換関係はEvidence§17による。旧91件・失敗class内部分PASSは重複加算しない。

Tooling test所有の凍結sourceとReference-owned保護scopeを接続し、初回issueのcommit／rollback、permit保存・read・認可・Audit、UNKNOWN／ACCEPTED拒否、Audit失敗rollback、同publication競合を有限範囲で確認した。通常無効、evidence Portのempty、肯定consume／close拒否を維持する。permit発行は配送完了・再送許可を意味しない。

## 3. 固定対象と2コミット案

**Owner操作承認済み。** Ownerは「この2コミットのローカル操作を承認します。コミット進めてください」と明示した。第1コミット18 fileは上記IDで実施済み。本書を含む6 fileを第2コミットへ固定し、その完了後のHEAD／clean statusを操作結果で確認する。

| commit案 | 対象 | 件数 |
|---|---|---:|
| `feat(reference): add bounded S1 B2 protected issue/read verification` | manifestのSourceFiles全件。Reference main5＋Configuration Import1、Reference test4＋補助2、Tooling test2＋補助2＋SQL1＋script1 | 18 |
| `docs: accept S1 B2 results and record fixed handoff` | AGENTS.md、Tooling README、B2開始票、B2 Evidence、本引継ぎ、固定manifest | 6 |

合計24 file。文書と実装を2コミットで固定する案。commit1単独では結果受入・引継ぎが揃わず、2コミット後のHEADを次のsource基点とする。変更pathはmanifestと上表で限定し、`git add -A`で未知差分を巻き込まない。commit後はcommit ID／各path・clean statusを確認する。remote push／PRは含めない。

元POM／依存／migration／Security／Framework source／CI、既存Service／Port・B1 test／SQLは変更対象外。既存code差分はFoundationConfigurationのImport1件のみ。今回の固定準備でmain／test／script機能を変更せず、Maven／Docker検証も再実行しない。

## 4. 端末内証拠・artifactの保管

| 所在 | 引継ぐもの |
|---|---|
| `tmp/b2-verification-0321079-20261008/` | 新規・回帰・失敗attempt・exit／sanitized XML・最終audit、source／artifact hash台帳 |
| `tmp/b2-preflight-0321079-20261008/` | offline依存／classpath manifest、JDK／Maven／Docker等の観測 |
| `tmp/b2-isolated-compile-r2-0321079-20261008/` | Reference sourceコピー205件、隔離class／packaged JAR |
| `tmp/b2-artifact-alignment-0321079-20261008/` | Framework sourceコピー8件、全10 entry比較、生成JAR、旧cache backup／復元記録 |
| `tmp/b2-e2e-finish-0321079-20261008/` | 再installのcache before／after／backup |
| `tmp/b2-runtime-route-r2-0321079-20261008/` | 訂正・続行／finish runner、限定cache復元helper |

これらの内容はGitコミットへ含めない。manifestは所在とhashを保持するが、raw／JAR本体の代替ではない。原log／XMLには接続情報等を含み得るため、別端末への保管・受渡しでは公開範囲と秘密の扱いを確認する。今回、外部保存・配布・削除をしていない。同端末の保全は確認済み、端末喪失に耐える別媒体backupは未実施。

| artifact | SHA-256 |
|---|---|
| 隔離Reference JAR | `5F78A9C4C88ED1E6B05222B8F607F7F0637D13210FB566E9AFD5B8555DE0876D` |
| 整合済みWeb MVC JAR（保全生成／cache／Reference内nested） | `34754252439BB8B5C446122A0F173B15B139731DF80A50D77C2F96B750F66FC1` |
| Tooling JAR | `E72019D3CA6A019D22548CDDCF88EA848D181F15E6A71E31E7C6069FC9C16F3A` |
| 元Reference受入JAR（保護対象） | `604412D21CB4C17F36E110EB9ED3D3FB71E586B7D4C8E2BF03E43F1CDA78CD6E` |

Web MVC cache1座標は§30の成功時保持条件により整合済み。今回のread-only照合でafter全5 file一致。cache場所は`~/.m2/repository/org/koikifw/koiki-starter-web-mvc/`。通常sourceから標準隔離packageしたJARであり、受入nested JARの直接コピーではない。受入nestedとのZIP全体hashは異なるが、全10 entry内容一致。新しい正式Framework release／配布を意味しない。

## 5. 再見・再実行の入口

**再見：** Evidence§17〜§20→manifest→採用classのresult／sanitized XML→process-exit／source hash台帳の順に照合する。Maven起動不要。失敗原本は履歴として保持する。

**再実行：** 端末内tmp・classpath・compile済み出力が前提であり、fresh cloneで即再現するbootstrapではない。新たな有限実行判断後に以下を揃える。

1. branch／HEAD／status、AGENTS、承認済み範囲とsource差分を確認する。
2. JDK21／Wrapper Maven、offline全依存、Tooling compile profileとjdbc単独runtime、cacheと受入artifact・nested依存を照合する。原targetのNullable生成元UNKNOWNを解消したと扱わない。
3. Docker Server・cached PG17／Ryuk／Chromium、空きmemory8 GiB／disk10 GiB、baseline process／containerを新しく確認する。過去PIDを現在の停止対象にしない。
4. 新しい隔離source／出力・raw path、caseとartifact epoch、command式・期待値、exit先行保存・有限log処理をreviewする。旧runnerの固定pathをそのまま反復しない。
5. 作業量・実経過の不確かさを含め予算を保守的に再評価し、集合／再実行上限・cleanup・停止条件を確定する。B2の残枠を新操作へ自動流用しない。
6. credentialは採用済みENV境界で供給し、通常設定を変えない。権限付き実行が必要なら環境の承認手順に従う。

最終検証時container0・当該JDK残留0、ready file／検証ENV0。これは最終audit時の観測で、次回開始時の環境状態を保証しない。今回の成果固定確認はfile／hashだけであり、新しいDocker／process診断は実施しない。

## 6. 予算と残課題

回帰累積約21.70分／上限30分、B2既知実経過約34.61分＋未計測管理5分、B1保守的管理150分を保持する。B2上限90分／累積240分、作業量B2 18標準時間／累積28時間の仮定を拡張しない。文書整理・Owner待ち等を完全計測済みとはしない。現時点の数値を新しいconsume検証の確定予算に使わない。

残課題は肯定consume／close証拠の一意解決・耐久保管・使用中保護と失効、commit不明時の照会／HOLD、D11歴史対象閉鎖・別publication横断抑止、D12後続競合／網羅性、送信／worker委譲・実provider／分散fencing・backup／DR。実運用・Reference Level 2・DoD・正式受渡し・Phase 4全体開始は未承認。

## 7. 次の再開指示例

> この引継ぎを起点に、B2のsource固定状況とmanifest／artifact／環境差分を確認してください。まず肯定consume接続の設計reviewで、証拠の保管・一意解決、保護の開始から使用完了まで、失効・期限／認証／scope、commit不明時の照会・HOLDと送信側への責任境界を具体化してください。既存のB2受入を再承認対象にせず、新しい操作の具体差分・検証集合・予算・cleanupを揃えるまで実装／通信／肯定操作を開始しません。

## 8. source基点の取得

第2コミット自身のIDは本文への自己参照を避け、git log -1 --format=%H -- docs/development/phase4-s1-stage-b2-fixed-results-next-session-handoff-20261008.mdで取得する。開始時は現HEADが当該commitか、その後の差分が何かを確認する。固定manifestのSourceGitCommitは第1コミット、CommitStateは本manifestを含む第2コミットでの文書固定を表す。
