# E1：VSCodeローカルPCへの引継ぎ（2026-10-30）

## 1. 現在地と作業範囲

OwnerはCloudでのRelease操作を今後の実行対象から外し、準備後に作業branchをcommit／pushして、VSCode側で取得・継続するよう指示した。
対象は`zaziedlm/KOIKI-JAVAWEB`、branchは`docs/phase4-s1-b1-c2-guarantee-review-20261030`。
この引継ぎでPR／main merge・release tag・Release作成・asset uploadは行わない。

- [配布計画§8.2〜8.3](phase4-e1-artifact-distribution-options-20261030.md#82-現行判断既存source-repositoryのreleaseを配布先にする2026-10-30)：既存public repositoryのRelease assetを使用。
- [ローカル結果](../architecture/validation/phase4-e1-artifact-distribution-results-20261030.md)：候補生成111件PASS、準備の6確認、Consumer1 invocation／8 checkpoint、通常root449実行PASS＋100 skip。
- [準備スクリプト](../../build-support/framework-distribution/prepare-koiki.ps1)：指定tag／asset取得・hash照合・実POM付きMaven登録。
- [Consumer導入条件](../../build-support/framework-distribution-consumer/README.md)：R-05のCustomer EntityScan明示、R-06のData Starter・同一DataSource／schema。

CloudのV01〜08は限定技術成立。結果Owner受入、V09のremote実取得、V10の実チームPC／IDE／案件CI受領は別に残る。commit／pushは結果受入や本番適用の認定とはしない。

## 2. VSCode側でbranchを取得する

作業中の変更がないことを確認し、`origin`が`zaziedlm/KOIKI-JAVAWEB`である端末で行う。別名remoteならその名前を使う。

```powershell
git status --short
git remote -v
git fetch origin
git switch --track origin/docs/phase4-s1-b1-c2-guarantee-review-20261030
# 同名local branchが既にある場合はgit switchで選び、次を実行
git pull --ff-only origin docs/phase4-s1-b1-c2-guarantee-review-20261030
git log -3 --oneline
```

このbranchには前段のS1／B1／C2整理とE1のTooling・計画・結果を含む。GitHub連携APIで保存したcommitはCloud原本とSHAが異なる場合があるため、引継ぎ時にtree一致を照合する。raw Evidenceが参照する元SHAを、後から別SHAへ読み替えない。

## 3. binaryと元source記録はpullとは別に受け取る

GitにはJAR／ZIP・raw log・cacheを登録しない。Cloudの引継ぎ用`koiki-e1-vscode-continuation.zip`を別途取得し、repository外の新規directoryへ展開する。
これは旧`koiki-e1-private-release-handoff.zip`とは別物。旧専用repo作成用package／scriptは使用しない。

引継ぎpackageは検証済み候補ZIP、元manifest・source inventory・staging POM変換記録、結果の写し、Cloud原本commitのGit bundleとremote対応記録を含む。元候補のZIPは次の値で照合する。

| 対象 | 値 |
|---|---|
| 候補版 | `0.1.0-preview.1` |
| asset名 | `koiki-framework-0.1.0-preview.1.zip` |
| ZIP size | 204,898 bytes |
| SHA-256 | `c3eee716a8ba8e56e5a450dbb40bf6cdb87ab20214368801bd472b1345a8ddf6` |
| 元source commit | `8fe8473defe8abe66a79de461a6e3c65924498fb`＋生成時の未commit Tooling／文書（inventoryで識別） |
| source inventory SHA-256 | `b529cb88f650f3380196b9bb94a40e06ecb0bcc60e25409a2d5c8c63c359a675` |
| staging変換記録 SHA-256 | `00d347fb1e2f10fd6a939ab0d0e0c85943270af2f7846765496eb009ce069a6a` |

必要ならGit bundleを非作業refへfetchして原本commitを参照できる。現在の作業branchを原本へresetしない。

```powershell
git fetch <package内のcloud-original.bundleのパス> HEAD:refs/koiki/e1-cloud-original
```

packageが受け取れない場合、pullしたsourceからの再生成は可能だが、別の候補bytesになる。同じ検証済みZIPと呼ばず、hash・版整合・登録／Consumer確認を更新する。候補生成・DB runnerは現時点でCloud Linuxのみ実証済みで、Windows nativeでの生成・DB検証は未確認。再生成が必要ならWSL／Linuxで既存手順を使うか、端末対応を先に確認する。

## 4. PCで最初に確認する

PowerShell7、GitHub CLI、JDK21、Maven3.9.16または案件のMaven Wrapperを準備する。秘密値をチャットやGitへ貼らない。

```powershell
pwsh --version
gh auth status
gh api user --jq .login
gh repo view zaziedlm/KOIKI-JAVAWEB --json nameWithOwner,visibility
java -version
./mvnw.cmd -version
```

公開側の認証主体は`zaziedlm`を使う。必要ならPC上の既存ログインから`gh auth switch --hostname github.com --user zaziedlm`で選択する。PCのproxy／組織policyによるAPI接続は、その端末で確認する。

まず同じ候補ZIPを専用local repositoryへ登録する。利用者の既存`.m2`を削除・置換しない。

```powershell
$candidate = '<repository外の展開先>'
$archive = Join-Path $candidate 'assets/koiki-framework-0.1.0-preview.1.zip'
$expected = 'c3eee716a8ba8e56e5a450dbb40bf6cdb87ab20214368801bd472b1345a8ddf6'
$recipient = Join-Path $candidate 'pc-recipient-repository'
$maven = (Resolve-Path ./mvnw.cmd).Path
if ((Get-FileHash $archive -Algorithm SHA256).Hash.ToLowerInvariant() -ne $expected) { throw 'ZIP hash mismatch' }
pwsh -NoProfile -File ./build-support/framework-distribution/prepare-koiki.ps1 `
  -Version 0.1.0-preview.1 -LocalArchive $archive -ExpectedSha256 $expected `
  -LocalRepository $recipient -Maven $maven
if ($LASTEXITCODE -ne 0) { throw 'Preparation failed' }
./mvnw.cmd -B -ntp "-Dmaven.repo.local=$recipient" `
  -f build-support/framework-distribution-consumer/pom.xml -DskipTests package
if ($LASTEXITCODE -ne 0) { throw 'Consumer build failed' }
```

このPC向けcommandは引継ぎ用で、Windowsで実行済みとは扱わない。最後の`-DskipTests`は依存解決・package確認だけで、CloudのDB journeyを再実行した意味ではない。

## 5. 既存repositoryへRelease登録する前の点検

1. Cloud結果と既知制限を確認し、公開するsource commitを選ぶ。main統合を先行する場合は、別途PR／CI／mergeを行ってからそのcommitを使う。この引継ぎはmain更新の指示ではない。
2. 元候補は未commit Toolingを含むsource断面から生成された。公開対象commitと全sourceが同じとはしない。`source-inventory.json`と対象checkoutのFramework本体・POM・wrapper等のhashを照合し、module追加／削除もGit差分で確認する。
3. 同じ候補を使う場合、公開対象commit・元manifestのsource SHA・staging変換・module内容の同等性をRelease notesへ記録する。commit SHAの対応だけを理由に元manifestを書換えない。Framework内容が変わったら新候補を生成して必要な検証をやり直す。
4. 取得／登録script・READMEをZIP内で訂正する場合もZIP hashが変わるため、元候補のままの公開とは扱わない。
5. 既存の`framework-0.1.0-preview.1` tag／Releaseがないことを確認する。存在すれば内容を照合して判断し、上書きしない。

元source commitとGitHub保存commitの対応はpackageの`git-handoff.json`を参照する。Git bundleの原本と、対応先のtree同一性も確認できる。
候補ZIP内READMEの`OWNER/koiki-artifacts`は作成時点の説明例。実際の配布先は`zaziedlm/KOIKI-JAVAWEB`とし、scriptの`-Repository`引数で指定する。

## 6. Release登録・実取得（PCでの後続作業）

公開対象commit・結果／制限・notesを確認した後のcommand例。commitの完全SHAとnotesの実ファイルを指定する。
同じrepoのRelease assetなのでpublicになる。新規repoを作成せず、source READMEの置換・binaryのGit commitは行わない。

```powershell
$repo = 'zaziedlm/KOIKI-JAVAWEB'
$tag = 'framework-0.1.0-preview.1'
$releaseCommit = '<確認した公開対象の40桁SHA>'
$notes = '<確定したRelease notesのパス>'
git tag -a $tag $releaseCommit -m 'KOIKI Framework 0.1.0-preview.1'
if ($LASTEXITCODE -ne 0) { throw 'Tag creation failed' }
git push origin "refs/tags/$tag"
if ($LASTEXITCODE -ne 0) { throw 'Tag push failed' }
gh release create $tag $archive --repo $repo --verify-tag --prerelease `
  --title 'KOIKI Framework 0.1.0-preview.1' --notes-file $notes
if ($LASTEXITCODE -ne 0) { throw 'Release creation/upload failed; inspect remote state' }
```

以後はV09として、remoteから取得したbytesを使う。既存の成功cacheに隠れないよう、新しい専用登録先を使う。

```powershell
$downloadRecipient = Join-Path $candidate 'download-recipient-repository'
pwsh -NoProfile -File ./build-support/framework-distribution/prepare-koiki.ps1 `
  -Version 0.1.0-preview.1 -Repository zaziedlm/KOIKI-JAVAWEB `
  -ExpectedSha256 $expected -LocalRepository $downloadRecipient -Maven $maven
if ($LASTEXITCODE -ne 0) { throw 'Remote download / preparation failed' }
./mvnw.cmd -B -ntp "-Dmaven.repo.local=$downloadRecipient" `
  -f build-support/framework-distribution-consumer/pom.xml -DskipTests package
if ($LASTEXITCODE -ne 0) { throw 'Downloaded distribution Consumer build failed' }
```

準備script内部で`gh release download`を実行し、ZIP／内部payloadを照合する。download後のDB journey、実IDE・実案件CIは必要な対象環境で追加確認し、上記package確認だけでV10完了とはしない。
Release URL・tag対象SHA・ZIP hash・登録結果・Consumer結果・cleanupを結果文書へ追記する。失敗時は作成済みtag／Releaseを確認してから再開し、自動削除・差替えをしない。

## 7. 保持する未完了事項

V09／10、結果Owner受入、公開版の最終認定、PR／main統合・CI確認、正式support条件、D3は未完了。S1の既知未達・D1結果受入未記録等も維持する。
Cloud側でReleaseの認証・network設定変更を再調査しない。公開・取得・PC／案件CIの実行はVSCode側から継続する。
