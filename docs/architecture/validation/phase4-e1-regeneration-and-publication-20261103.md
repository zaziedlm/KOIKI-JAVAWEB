# E1：JAR再生成からRelease公開・取得までの検証（2026-11-03）

## 1. Owner指示と範囲

OwnerはFramework開発・改修後にJAR一式を再生成し、ZIP公開→利用側Maven repository登録の流れを検証するよう指示した。
GitHub公式仕様を確認し、ZIP配布はReleases、受領側は既存独立Consumerを使うことをOwnerが選択した。
実案件のローカルパスは未準備。実案件IDE／CI／チーム受領V10は今回のConsumer検証で代用しない。

E1／Toolingの配布経路を対象とし、Frameworkへ検証用の機能変更は加えない。Root CI・既存snapshot workflow・既存`.m2`の削除／置換は行わない。
通常root・DB journeyの追加実行は今回含めない。新規候補生成一回、専用recipientへのlocal登録一回、公開後のremote取得・別専用recipient登録・Consumer package一回を順に行う。
今回の新しい実行枠は開始から60分以内、最後10分を後始末／記録に確保する。生成はscriptの40分timeoutとheap512MiBを維持し、並列buildせず、DB／containerは起動しない。失敗時は記録して原因・remote状態を点検し、自動再生成・asset差替えをしない。

## 2. 新規候補

| 項目 | 値 |
|---|---|
| Source commit | `3887d39aa52270d6a643af199da2bfdaa11d264d` |
| Source dirty | true。点検記録・notes案2ファイルのみ未commit。Framework／POM／Wrapper／生成・準備scriptに未commit差分なし |
| 生成先 | `%TEMP%\koiki-e1-regeneration-20261103` |
| Version／tag | `0.1.0-preview.1`／`framework-0.1.0-preview.1`。未公開の初回版を使い、旧candidate-3は公開しない |
| ZIP | `koiki-framework-0.1.0-preview.1.zip`、205,900 bytes |
| ZIP SHA-256 | `470a2fac4a30d3c323e38341b855e23e263c7aced501c3c8a378e06db1a8b27c` |
| Source inventory SHA-256 | `c7fcabeaab9e3d7e6a3c6606b27c381fa68936f0d2b36bb07550c2824a531fd8` |
| Staging変換記録 SHA-256 | `0818938495832ecda4e591552cd7bed467f4ae6af2a131308728c0f398cff214` |
| Build | Temurin21.0.13+9／Maven3.9.16、offline clean package、45.054秒、BUILD SUCCESS |
| Tests | Surefire XML集計111 PASS、failure／error／skip 0 |
| Inventory | 14 POM＋12 JAR。manifest対象28ファイルのsize／hash一致 |

生成は既存`create-candidate.py`を使用し、checkout外の新stagingで全対象をclean packageした。既存ZIPの再利用ではない。
source inventoryは生成時の文書案も含む原本として保持する。後続文書更新でmanifestやinventoryを再書換えしない。
raw log・source snapshot・ZIP・専用recipientはGit外に保持する。
processの補助観測に使った`Get-CimInstance Win32_Process`はアクセス拒否だった。build結果はMaven終了・Surefire XML・script終了とhashで確認し、全瞬間のprocess上限を実測証明したとは扱わない。

## 3. 公開・取得結果

| 確認 | 結果 |
|---|---|
| Local登録 | 新規の`%TEMP%\koiki-e1-recipient-20261103-local`へ14座標登録成功 |
| 公開主体 | ローカルPCのGitHub CLI、`zaziedlm`、PUBLIC repoのADMIN権限 |
| 公開前 | 同名tag／Releaseなし、remote branchは`3887d39`で一致 |
| 公開 | [framework-0.1.0-preview.1](https://github.com/zaziedlm/KOIKI-JAVAWEB/releases/tag/framework-0.1.0-preview.1)、prerelease、draft=false。2026-11-03 10:10:47 JST |
| Tag object | `0108e91b31c67e617edb1e343fb96ba3c98c749b`。local／remote peeled commitはともに`3887d39aa52270d6a643af199da2bfdaa11d264d` |
| Asset | `koiki-framework-0.1.0-preview.1.zip`、API ID `703787545`、205,900 bytes、state uploaded |
| GitHub asset digest | `sha256:470a2fac4a30d3c323e38341b855e23e263c7aced501c3c8a378e06db1a8b27c`、生成ZIPと一致 |
| Remote実取得 | checkoutの準備scriptのRelease経路でtag／asset名を固定し、`gh release download`成功。ZIP hash／全payloadを照合後に登録 |
| Remote登録 | 空から開始した`%TEMP%\koiki-e1-recipient-20261103-remote`へ14座標登録成功、完了markerの版／hash一致 |
| Consumer | 同じremote recipientを`-Dmaven.repo.local`に指定し`-DskipTests package`成功。26.371秒、2026-11-03 10:12:22 JST終了 |
| Consumer成果 | Boot JARに含まれるKOIKI依存6 JARがすべて`0.1.0-preview.1`。Framework source相対参照を使わず固定版Parent／依存からpackage成立 |
| 最終照合 | local／remote両recipientで登録26 payloadのhashがmanifestと一致、installer lock残留なし |

**V09の限定技術結果はPASS。** 再生成→Release公開→実取得→空の専用repository登録→Consumer packageが成立した。
`-DskipTests`のため今回ConsumerのJUnit・DB／HTTP journeyは実行していない。今回のbuild成功を実案件受領V10・Owner結果受入・全DoDの完了に拡張しない。
専用recipientは既定`.m2/repository`と同じMaven local repository形式で、既定を汚さずに確認するため別pathを指定した。実案件各PCの既定`.m2`・実IDE・CIでの確認は未実施。

生成ZIPは公開後に差替えていない。DB／containerは起動せず、起動したMaven／Python／PowerShellの各実行は終了コード0で完了した。
raw log・source snapshot・ZIP・recipientは再点検用に保持し、Gitへ追加しない。source inventoryには生成時の文書案を保持し、その後の文書訂正を遡及反映しない。

## 4. 次回改修時の流れ

Ownerは次回以降もしばらく、引継ぎ文書とCodexの支援でZIP運用を試行し、実案件への配布・更新・受領を確実に行えるよう手順を見直して安定化させる方針を示した。全自動公開スクリプトの検討・実装は予定事項に含めない。目的と確認観点は[引継ぎ文書§8](../../development/phase4-e1-vscode-handoff-20261030.md#8-実案件向けzip運用の試行と手順安定化owner方針2026-11-03)を参照する。

1. Framework変更と必要な検証を完了し、公開source commitと配布範囲を固定する。commit／push・PR／mergeは各操作の判断に従う。
2. 新しい固定版を選び、新stagingで`create-candidate.py`を実行する。checkout POMは一括書換えしない。
3. ZIP・manifest・source inventory・版変換記録・test結果を照合し、新しい専用recipientで準備scriptを確認する。
4. 既存repoへ固定tagとprerelease／Releaseを作成し、対応ZIPをasset登録する。公開済み同一版の差替えはしない。
5. 利用側は版・tag／asset・SHA-256を固定して`prepare-koiki.ps1`で取得する。`-LocalRepository`省略時は利用者の`.m2/repository`、検証では専用recipientを指定する。
6. 利用プロジェクトのPOMで固定版Parent／BOMを選び、同じrepositoryをMaven／IDEに設定してbuildする。利用者ごとの`.m2`に登録し、Framework source buildを取得の前提にしない。
7. 実案件PC／IDE／CIでは案件側の実結果を記録する。cacheなしCI、DB／migration／Security等はその案件の適用範囲に応じて別確認する。

手順の実行入口は[引継ぎ文書](../../development/phase4-e1-vscode-handoff-20261030.md)、公開文面は[Release notes](../../development/phase4-e1-preview-release-notes-20261103.md)。今回の結果で正式support条件・S1未達・C2 H07・D1結果受入・Phase4全体完了は変更しない。

成果固定前の時点では、新規文書と入口更新は未commitで、作業branchのcommit／push・PR／main mergeは行っていなかった。remote変更はOwner指定の固定tagとprerelease／asset公開、実取得に限定していた。

## 5. 成果固定とmain checkpointのOwner指示（2026-11-03）

Ownerは、実案件へFramework資源を提供する手段が整った版として、ここまでの成果commit・作業branch push・main向けPR／merge・main tag付与とREADMEへの成果記載を明示指示した。
今回の目的は配布経路の成立であり、同じFramework sourceからの再生成・111件PASS・remote実取得／登録・Consumer packageの証拠により、DB／HTTP journeyの追加実行を今回の成果固定条件としない。

必要PR checkとマージ後main CIを確認してから、`checkpoint/e1-distribution-established-20261103`を検証済みmain commitへ付ける。既存`framework-0.1.0-preview.1` tag／assetの差替えは行わない。
このcheckpointは配布手段確立の到達点であり、実案件受領V10・Owner結果受入の別管理、正式support、S1未達・C2 H07・D1結果受入・全DoD／Phase4全体の状態は変更しない。
