# S1 R1：成果固定・別PC再開の引継ぎ（2026-10-13）

**状態：R1限定結果 COMPLETE / OWNER ACCEPTED、成果固定local commit済み。後続実行は未開始。**

## 1 再開の入口とGit固定点

| 項目 | 固定内容 |
|---|---|
| 作業branch | `feature/phase4-s1-reference-async-r1` |
| R1成果commit | `610c5899f47b784fadc013c60f352f859f8662f8` |
| メッセージ | `feat(reference): add opt-in async expense approval notifications` |
| 開始条件文書の基点commit | `975af1d806dd99d7b65c9b745db5adfc1e8c4ce0` |
| 成果commit対象 | Reference20＋Tooling5＋結果文書1＋状態・導線整合文書3＝29 file |
| 引継ぎ文書 | 本文書を別のlocal commitとして追加。commit IDは`git log`で確認する |
| remote | `origin`。remote到達状態は本引継ぎ作成時に未照合。push完了と断定しない |

元PCのworkspaceは`C:\Users\kataoka\Desktop\KOIKI-JAVA\KOIKI-JAVAWEB`。別PCでは実際のclone先に読み替える。
本引継ぎ作成前のHEADは上記成果commit、作業ツリーはcleanだった。Ownerが成果29件をcommit済みで、今回の会話で本引継ぎの作成・local commitも指示した。今回の指示でpush／PR／mergeは実行しない。

再開時は次を順に読む。

1. `AGENTS.md`の現行R1限定結果受入欄。
2. [結果文書§13〜15](../architecture/validation/phase4-s1-r1-implementation-and-early-verification-20261012.md#13-12承認後の残4件critical-journey成立とr1受入判断資料2026-10-13)：成立結果、機能説明、Owner受入承認。
3. [ADR-051](../architecture/adr/ADR-051-reference-notification-async-r1.md)と[Project Overview正本](../agent/skills/koiki-project-overview/SKILL.md)。
4. [R1開始票](phase4-s1-r1-minimal-start-owner-review-20261012.md)と[S1目的対応表](phase4-s1-purpose-dod-stage-mapping-20261009.md)：履歴・保証境界と後続工程の位置づけ。

開始票の未成立表記や結果文書の旧停止節は各時点の履歴。現行状態は結果文書§15および成果commit内のAGENTS／Skill／ADRを優先し、過去の予算・追加実行承認を再開許可へ流用しない。

## 2 完了した機能と検証

既存expense承認・承認Auditから値eventを発行し、JDBC publication保存、notificationの標準非同期listener、ローカルprovider stub受理、publicationのCOMPLETEDまで正常経路が成立した。異なる申請者2件の対応関係、stubの同key同payload受理再利用／異payload拒否も確認した。

通常は無効。Mavenの`s1-reference-async` profileと`koiki.reference.notification.async.enabled=true`の両方で選択する。profileだけでは有効化しない。通常artifactで有効化した場合のstarter不足早期拒否、async artifactで無効の場合の既存承認保護、V4無効でV5を選択する起動、明示V4＋V5併用を検証した。

| 集合 | 受入済み結果 |
|---|---|
| 通常root | 392＝300実行＋S1 64／B2 28skip、failure／error／予定外skip0 |
| 新規root | K01／K02／D03の3件。early8と二重加算しない |
| provider契約 | D01／D02の2件、epoch `tooling-rerun1` |
| package非同期検証 | C02〜C05の4件、epoch `tooling-remaining-four` |
| 既存Critical Journey | C01の1件、epoch `critical-journey`、固定通常JAR使用 |
| 配布artifact | 通常／async別固定、main・resource一致、profile runtime依存とtest専用依存の区別を照合 |

新規は計9件＝root3＋Tooling6、既存C01は別1件。R0 Rules107等を独立成果へ二重加算しない。S1／B2専用92件の全件再検証は行っていない。

COMPLETEDはローカルstub受理後のlistener正常終了を示す。実メール等の到達、通知全体のexactly-once、障害時の復旧保証を示さない。R1限定結果は2026-10-13にOwner受入済みであり、Phase 4全体の完了ではない。

## 3 端末内証拠・artifactとGit移送の区別

端末内runは`tmp/r1-975af1d-20261012-first/`。Git ignoredであり、push／cloneでは移送されない。

| 端末内入力／証拠 | 用途 |
|---|---|
| `implementation-inputs.json` | 検証入力1190 file・実装差分25 fileのhash。検証後の文書3点整合更新とはepochを区別 |
| `stages/root-rerun2/` | 通常root成立、case名／hashとXML実体62 file |
| `normal-artifact-inspection.json`／`async-artifact-inspection.json` | 両artifactの内容・依存照合 |
| `stages/tooling-rerun1/provider-two-fresh.xml`／`provider-two-fixed-result.json` | 保存したD2。whole-stage STOPPED／Cleanup=falseも保持 |
| `stages/tooling-remaining-four/`／`stages/critical-journey/` | 成立C4／C01の保存XML・照合・cleanup記録 |
| `tooling-six-fixed-result.json`／`r1-selected-verification-final.json` | 別epochの成立集合・artifactを結ぶ索引。生成時のOwnerAcceptance=PENDINGは当時の値で、正式受入は結果文書§15 |
| `artifact-inputs.json` | 後続用の適格通常／async JAR入力 |

固定JARは次の2つ。

- `normal-reference-fixed.jar`：SHA-256 `BF660BEF4B76FDE1F72B58A6AD958E3DDB49D4C277D661AD78FF1B862FAF488D`
- `async-rerun1-reference-fixed.jar`：SHA-256 `45B6B63ADE3BA56C5FB5C9A5B172DFA475717DDC9A5DAACE11971F24FBD9BF5C`

旧`async-reference-fixed.jar`は通常JARと同じbytesだった未成立候補。適格async JARへ取り違えない。通常targetはC01用に固定通常JARへ戻してあるが、別PCのtargetはGitから再現されない。

rawを保持したい場合は上記runを別途コピーし、XML／JAR等のhashを照合する。既存runを上書きせず、履歴保管として扱う。旧runnerの絶対path、PID／creation identity、container session、baseline、source snapshot、browser状態は新PCで有効ではない。コピーしたStart／Invoke scriptをそのまま実行しない。

## 4 別PCでの再開手順

1. 元PCで必要なcommitがremoteへ到達したことをpush結果で確認する。新PCではremoteを取得し、作業branchと成果commit、本引継ぎ追加commitが存在することを確認する。
2. 既存cloneに変更があれば破棄せず確認する。対象branchへ移り、`git status --short`と`git log -2 --oneline`でclean／固定点を確認する。
3. §1の文書を読み、R1受入済みと未承認の後続を区別する。履歴を理由にR1検証を一律再実行しない。
4. 次に必要な工程と目的・最小集合を先に決める。単なる読取整理ならbuild／Dockerを起動しない。実行が必要な段階では、新PCのJDK21、Maven、依存cache、必要image／browser、Docker Server、既存Java／container、空きmemory／diskを読取確認する。
5. 不足依存・権限・実設定などの差を有限一覧にする。元PCの「取得済み」を新PCの成立として扱わず、承認範囲と実行環境の権限付き手順に従う。他sessionのJava・containerを停止しない。
6. 新しい実行が承認された場合だけ、新PC固有のrun・source／artifact固定・所有記録・予算を準備する。停止条件・cleanupを先に成立させる。

Git確認例（remote取得後、cleanな既存cloneで使用）：

```powershell
git switch feature/phase4-s1-reference-async-r1
git status --short
git log -2 --oneline
git show --stat 610c589
```

本引継ぎ作成時に旧台帳には残3558秒があったが、これは旧端末・旧検証の履歴値。中断後／別PCの自動実行許可や新しい残予算として採用しない。

## 5 過去の停止から引き継ぐ注意

- 通常rootのDB資源制限は独立selector `koiki.reference.normal-root.db-limits.enabled=true`で指定する。専用64件のskip用flagをtrueにしてDB制限の代用をしない。
- 容量監視とMaven cleanのtarget削除は競合しうる。tmpの訂正では隔離targetのDirectoryNotFoundだけを限定分類し、権限拒否等の別errorを隠さない。
- profileだけを変更して既存repackage済みJARへpackageすると、archiveが再作成されない場合がある。今回のasync成立は`maven.jar.forceCreation=true`による再作成で確認した。再build時も通常／asyncを別固定し、test二重実行を避け、完成JARの実依存を照合する。
- Boot repackageはmetadataだけのstarterやannotation processorを省略する。starterファイル名の有無だけで選択を判定しない。Modulith由来のArchUnit runtime依存をtest専用KOIKI Rulesと混同しない。
- 短命DBの終了とSQL観測が競合しうる。SQL失敗を設定値不一致へ読み替えず、同じ所有IDの稼働状態を確認する。未観測値をPASSとしない。
- `reuseForks=false`のclass切替で旧／新Ryukが重なった。D2の成立を保持し残C4だけへ絞ることで上限を守った。上限を広げたり成立D2を反復する方法へ戻さない。
- 過去Cleanup=falseは後続読取のcontainerなしと区別して保全する。歴史summaryを上書きしない。

## 6 残作業と承認境界

R1の承認済み最小検証・Owner受入・29件のlocal成果固定は完了。本引継ぎのlocal固定後、remoteへの移送確認が別PC再開の実務上の残作業となる。push／PR／mergeをR1受入と同一判断にしない。

次工程は必要性と順序の見直しから始める。候補はR2失敗分離、R3保護モデル・停止／復旧、R4 consume／close・認可／Audit、R5保持／purge／traceであり、まだ開始承認されたものではない。実provider配信、worker委譲、全DoD、正式受渡し、Phase 4全体にも開始・受入を拡張しない。

必要最小限の目的・caseを先に確定し、直接影響や新しい要求が確認されたタイミングでだけ網羅を広げる。旧監視pure74例やアーカイブのケースを本体進捗へ戻さない。次工程開始前のOwner判断を経て進める。
