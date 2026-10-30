# E1：ローカルPCでの再現確認とRelease公開手順（2026-10-30）

## 1. 現在地と実行環境

公開操作（tag／Release作成・asset upload）は、GitHub認証と対象repositoryへの書込権限を確認したローカルPCから行う。Codex Cloud等のクラウド実行環境からは行わず、CloudのRelease権限／network制約の再調査を継続条件にしない。
配布先は既存の `zaziedlm/KOIKI-JAVAWEB`（public）、認証主体は `zaziedlm`。新規 `KOIKI-JAVA-asset` は作成しない。Release assetもpublicになり、JAR／ZIPをGit履歴へ格納しない。

Owner指示に基づき、Cloudの別渡しZIPに依存せず、このローカルPCで候補を再生成・再現確認した。
[Windows再現記録](../architecture/validation/phase4-e1-local-windows-reproduction-20261030.md)を現行の結果・候補識別の入口とする。
Temurin21.0.13+9／Maven3.9.16で、候補生成111件、準備の6確認、Consumer1 invocation／8 checkpoint、通常root449実行PASS＋100 skipが訂正後に成立し、cleanup・source／ZIP照合も成功した。
結果Owner受入、公開対象commitの確定、Release公開・実取得V09、実案件PC／IDE／CI受領V10は未完了。
この文書訂正は、公開・commit／push・PR／mergeの実行ではない。

## 2. branchとツールの役割

作業branchは `docs/phase4-s1-b1-c2-guarantee-review-20261030`。先端 `78a8887` はGitHubへ反映済みで、その後のWindows対応・文書訂正は未commit。別PCではこの未commit差分をpullできないため、成果固定と反映状況を先に確認する。

| 資材 | 役割 |
|---|---|
| [create-candidate.py](../../build-support/framework-distribution/create-candidate.py) | stagingだけで版を変換し、14座標をbuildしてZIP／manifest／hashを生成。公開はしない |
| [prepare-koiki.ps1](../../build-support/framework-distribution/prepare-koiki.ps1) | ローカルZIPまたは指定Release assetを取得・照合し、実POM付きでMaven local repositoryへ登録。uploadはしない |
| [検証Tooling](../../build-support/framework-distribution-verification/README.md)／[独立Consumer](../../build-support/framework-distribution-consumer/README.md) | 拒否・再利用／衝突、固定版依存・Entity／migration／Webの有限確認 |
| 本書§6のGit／GitHub CLI | 権限のあるローカルPCでtag・Release・assetを登録する手順。専用publish scriptは未実装 |
| [旧publish-initial-release.ps1](../../build-support/framework-distribution/publish-initial-release.ps1) | 廃止済み。必ず停止し、remote操作をしない |

取得・Maven登録は利用側PC／CIでも行う。公開側をローカルPCに限定する方針と、利用側CIからの取得は区別する。

## 3. 現行のローカル候補

| 項目 | 値 |
|---|---|
| 候補版／asset | `0.1.0-preview.1`／`koiki-framework-0.1.0-preview.1.zip` |
| 生成元 | local candidate-3、`78a8887efb1c02888c69976bee8171ab99e94c2f`＋source inventoryの未commit差分 |
| ZIP size | 205,619 bytes |
| ZIP SHA-256 | `e3cfe45514cd0af67177416467c052bc61b815bda375c57a4a12cc7798992928` |
| source inventory SHA-256 | `06c6f8d8b70de307f3addc8be0400aa844b540aa51eaaad1eb71783cdd246d49` |
| staging変換記録 SHA-256 | `0818938495832ecda4e591552cd7bed467f4ae6af2a131308728c0f398cff214` |

このPCの生成先は `%TEMP%\koiki-e1-local-20261030-2300\candidate-3`。ZIPはこのdirectory直下にあり、`assets/`配下ではない。
候補とsource inventory・staging記録・有限logはGit外で保持している。一時領域から退避する際も同じbytesとhashを保つ。再生成したものは新候補として識別し、同一版の公開済みassetを差し替えない。

Cloudの `koiki-e1-vscode-continuation.zip`／Git bundleは今回の必要条件ではない。旧専用repo用packageも使用しない。
[Cloud結果](../architecture/validation/phase4-e1-artifact-distribution-results-20261030.md)の元SHA・hash・失敗履歴は当時の証拠として保持し、現行候補へ読み替えない。

## 4. ローカルPCで確認すること

PowerShell7、GitHub CLI、Temurin JDK21、Maven Wrapper3.9.16を使用する。秘密値はチャットやGitへ貼らない。

```powershell
git status --short --branch
git remote -v
pwsh --version
java --version
javac --version
./mvnw.cmd -version
gh auth status
gh api user --jq .login
gh repo view zaziedlm/KOIKI-JAVAWEB --json nameWithOwner,visibility,viewerPermission
```

`origin`が対象repoであること、CLI主体が `zaziedlm`、公開に必要な書込権限があることをPCで確認する。必要なら既存ログインから `gh auth switch --hostname github.com --user zaziedlm` で選択する。
Java／Mavenは拡張管理のTemurinを参照し、版不整合警告がないことを確認する。

次は現行候補の照合・登録を行う場合の例。Windowsでは既にDB journeyを含む再現確認が成立しているが、このcommand例の `-DskipTests` 自体は依存解決・package確認に限定される。

```powershell
$verificationRoot = Join-Path $env:TEMP 'koiki-e1-local-20261030-2300'
$candidate = Join-Path $verificationRoot 'candidate-3'
$archive = Join-Path $candidate 'koiki-framework-0.1.0-preview.1.zip'
$expected = 'e3cfe45514cd0af67177416467c052bc61b815bda375c57a4a12cc7798992928'
$recipient = Join-Path $verificationRoot 'pc-recipient-repository'
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

ZIPを別の場所へ退避した場合は `$candidate` を実際のcandidate directoryへ変更する。利用者の既存 `.m2` を削除・置換しない。

## 5. Release登録前の点検

1. ローカル結果・既知制限を確認し、公開対象commitを選ぶ。必要な成果固定・push・PR／CI／mergeは各操作の判断に従い、未commit差分を公開済みsourceと扱わない。
2. candidate-3の `source-inventory.json` と公開対象checkoutのFramework本体・POM・wrapper等のhashを照合し、module追加／削除もGit差分で確認する。
3. 元manifestのsource SHAと公開対象commitが異なる場合は、staging変換・module内容の同等性をRelease notesへ記録する。元manifestを別SHAへ書換えない。Framework内容が変われば新候補と必要確認を用意する。
4. ZIP内のscript／README等を変更してもhashは変わる。checkoutの文書訂正を理由に、検証済みZIPの中身を差し替えない。
5. `framework-0.1.0-preview.1` tag／Releaseの有無を確認する。存在すれば内容を照合して判断し、自動上書き・削除しない。

candidate-3に同梱済みのREADMEには作成時の `OWNER/koiki-artifacts` 等の説明例が残る。実際には `-Repository zaziedlm/KOIKI-JAVAWEB` を明示する。訂正後のcheckout READMEと、既存ZIP内READMEは別のbytesである。

## 6. Release登録・実取得（PCでの後続作業）

以下は公開対象commit・結果／制限・notesを確認し、公開操作の実行指示を得た後にローカルPCで使うcommand例。現時点で実行済みではない。
§4の `$archive`・`$expected` を使用し、完全SHAとnotesの実ファイルを指定する。既存public repoへ登録し、可視性変更・source READMEの配布専用READMEへの置換は行わない。

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

続いてV09としてremoteのbytesを実取得し、新しい専用登録先で確認する。

```powershell
$downloadRecipient = Join-Path $verificationRoot 'download-recipient-repository'
pwsh -NoProfile -File ./build-support/framework-distribution/prepare-koiki.ps1 `
  -Version 0.1.0-preview.1 -Repository zaziedlm/KOIKI-JAVAWEB `
  -ExpectedSha256 $expected -LocalRepository $downloadRecipient -Maven $maven
if ($LASTEXITCODE -ne 0) { throw 'Remote download / preparation failed' }
./mvnw.cmd -B -ntp "-Dmaven.repo.local=$downloadRecipient" `
  -f build-support/framework-distribution-consumer/pom.xml -DskipTests package
if ($LASTEXITCODE -ne 0) { throw 'Downloaded distribution Consumer build failed' }
```

準備scriptが指定tag／assetの `gh release download` と照合を行う。Release URL・tag対象SHA・ZIP hash・登録／Consumer結果・cleanupを記録する。
失敗時は作成済みtag／Releaseを点検してから再開し、自動削除・差替えしない。実IDE・実案件CIは別途確認し、package成功だけでV10完了にしない。

## 7. 保持する未完了事項

V09／10、結果Owner受入、公開版の最終認定、PR／main統合・CI確認、正式support条件、D3は未完了。
S1の既知未達・人手HOLD、C2 H07未実証、D1結果受入未記録も維持する。
Cloudの公開操作は再開せず、権限のあるローカルPCで、成果固定・公開・受領を段階別に継続する。
