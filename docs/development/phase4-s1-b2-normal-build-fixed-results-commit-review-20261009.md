# Phase 4 S1 通常build対策 成果固定commit Ownerレビュー

作成日：2026-10-09。状態：`LOCAL COMMIT OWNER APPROVED — ONE COMMIT / NO REMOTE`（§6）。

通常buildと検証専用testの分離は[Evidence§24](../architecture/validation/phase4-s1-b2-normal-build-remediation-20261009.md#24-最終結果のowner受入承認)で`COMPLETE / OWNER ACCEPTED`。次作業に混在させず、受入済みsource・承認記録・証拠導線を現在のfeature branchの1 commitへ固定する案とする。Git indexへのstage／commitはまだ行わない。

## 1 固定するsourceと結果

branchは`feature/phase4-s1-reference-foundation`、現在HEADは`8b03b9f7f237d8987c637b6fc2f1a5e66e03ab4f`。実行sourceはこのGit archive＋承認14ファイルhashの隔離copy、Owner受入後の文書追記とREADME更新は検証時copyの後段epochとして区別する。

Windows通常root257 PASS／予定無効92、専用S1 64＋B2 36＝100 PASS／skip0、前提負例3件、package／artifact整合・cleanupは受入済み。登録重複8を除く349種類を照合。Linux／CIは未実測、観測欠測・旧第三JVM／旧凍結エラーの未特定部分を保持する。commit時の改行正規化とraw SHA-256は別表現であり、Git blob IDも記録して同一性の照合に使う。

## 2 local commit対象22ファイル

全path・変更区分・working file SHA-256・正規化Git blob IDは[固定manifest](../architecture/validation/phase4-s1-b2-normal-build-fixed-results-manifest-20261009.json)のSourceFilesへ列挙する。manifest自身は自己hash対象から除外する。

| 区分 | 件数／対象 |
|---|---|
| Reference test条件 | 10：B2 process3クラスとS1 DB-backed7クラス。notification test packageのimport／class条件のみ |
| test所有bridge | 1：referenceacceptance/notification/B2FrozenSourceProcess.java。明示classpath・Windows／準備前提を副作用前に確認 |
| Tooling手順 | 2：README、scripts/prepare-b2-reference-acceptance.ps1（新規）。offline準備と通常／専用の実行契約 |
| 結果・個別レビュー | 5（新規）：最終Evidence、通常／専用分離、第三JVM有限観測、所有数訂正、監視DB訂正の各レビュー文書 |
| 現行導線 | 2：S1目的対応表、通常build対策の初回引継ぎ。受入済み結果・R0へ進む順序を追記 |
| 固定票・manifest | 2（新規）：本票、固定manifest |

改修source13＋最終Evidence1＝既承認14ファイル、個別レビュー4件と導線2件・固定票／manifest2件を合わせ22件。Framework／Reference main、POM／依存、migration、Security、CI／AGENTS／Skillは対象外。別branch作成・remote・snapshot publishは含めない。

## 3 証拠保全とGitに含めないもの

端末内のrunは`tmp/b2-normal-build-8b03b9f-20261009-first/`。旧失敗raw・XML・source copy・専用m2・target・JAR・coordinator保持log・各tmp runnerはGitへ追加しない。manifestはsource、最終／失敗証拠index、資源・予算・cleanup、artifact／classpath hashと所在のmetadataを保持し、raw内容・credential・接続secretを転載しない。

検証時SourceFiles14件・artifact hash・raw indexは元のまま保存し、最終index作成後のOwner受入JSONを別に索引化する。Gitへのmetadata保存は別端末でruntimeが再現済みになったことを意味しない。再実行は新しい有限判断と環境／source・artifact照合が必要。

## 4 承認後の限定操作案

1. branch／HEAD／変更集合・indexを再確認し、本票＋manifestの22ファイル以外があれば停止する。
2. manifestのworking SHAとGit blob、受入sourceと実行時hash対応を照合する。改修／手順13ファイルのうち実行code12ファイルのhash不変と、READMEだけ後段文書追記があることを区別する。
3. 22件をpath明示でstage、staged diff／対象集合・git diff --checkを確認する。新しいMaven／Docker実行なし。
4. local commit1回。message候補：`test: separate S1 acceptance verification from normal builds`。
5. commit後の対象／blob ID・branch／HEAD・clean statusを確認する。証拠artifact／rawを更新・再生成せず、source固定結果を報告する。

commit承認はこのlocal操作1回だけ。予期しない差分・stage不一致・署名／hook等の失敗では停止し、設定変更やremoteへ進まない。Owner自身のcommitでも同じ対象・照合を用いる。

## 5 Owner判断と次作業

本票の22件を現在branchへlocal commit1回で固定するか。受入判断はcommit承認を兼ねないため、操作前確認が必要（[初回引継ぎ§8](phase4-s1-b2-normal-build-remediation-session-handoff-20261009.md)とEvidence§24）。承認後に上記操作を行い、その後R0開始判断資料へ進む。R0資料の作成方針は既存目的対応表の範囲、R1のruntime・非同期code・POM／依存・publication migration・検証開始は別判断のまま。

## 6 local commit1回のOwner承認

2026-10-09、Ownerは「この22件を現在のbranchへlocal commit 1回で固定してよいです。ここでは、コミット実行までお任せします」と明示した。§2〜§4の22件をfeature/phase4-s1-reference-foundationへlocal commit1回で固定する操作をAgentへ委任済みとする。branch／HEAD／path／hashとstage差分を照合してから実行する。承認記録に伴う本票とmanifestの文書metadata更新を同じ22件内に収め、実行code12件のhash・検証raw／artifactを変更しない。commitの実行結果はcommit IDの報告と端末内commit-result.jsonへ保存する。remote・追加検証・R1実装開始を含めない。
