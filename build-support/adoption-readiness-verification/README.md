# Pre-Phase 4 Adoption Readiness Verification

このdirectoryは、P4-AR6でArchitecture Ownerが承認したR2 stage / manifest契約を実証する、
Tooling所有の非配布資材です。Root Reactor、BOM、formal release unit、Framework Public API、
Starter、migrationおよびworkflowには含めません。

共有source baselineのtag名と現在状態は[Repository Top README](../../README.md)を正本とします。本Toolingには
tag名ではなく、検証対象checkoutで`git rev-parse HEAD`した40桁commitを`ExpectedFrameworkCommit`として渡し、
検証対象のsource identityを実行前に固定します。tagを使用する場合は、実行者がcheckoutとの一致を確認します。
source milestone tagは正式Maven release、managed repositoryまたは実チーム受入完了を意味しません。

## R2 handoff verification

`invoke-p4-ar6-r2-handoff.ps1`は、cleanな固定Framework commitから現行formal release unitを
空のisolated Maven repositoryへstageし、coordinate、packaging、payload SHA-256および非混入境界を検査します。
任意の`CustomerPom`を指定すると、同じrepositoryからCustomer-like buildを解決し、KOIKI artifactが
build前後で変化していないことも確認します。検証終了時には所有markerを照合してstage rootを削除し、
stage外・source Repository外の指定先へfinal manifestとSHA-256 sidecarだけを残します。manifestには実行した
Tooling script自身のSHA-256も記録し、Framework source identityと検証処理identityを分けて追跡します。

Customer検証ではconsumer-visibleなKOIKI依存が1件以上と`koiki-archunit-rules`が解決されたことに加え、Customer test sourceと
compiled test classが`KoikiArchitectureRules`を使用し、対応するSurefire test caseが実際に成功したことを確認します。一般の
`clean verify`が成功しても、KOIKI依存またはArchitecture Rulesの実行証拠がなければFAILとします。

### 入力境界

| Parameter | Meaning |
|---|---|
| `ExpectedFrameworkCommit` | 必須。検証対象Framework checkoutの40桁commit |
| `StageRoot` | 必須。Framework / Customer Repository外にある、存在しないか空の絶対path |
| `ManifestOutput` | 必須。stage / source Repository外に新規作成するJSONの絶対path |
| `CustomerPom` | 任意。Customer-like buildを検証する場合のroot POM。ファイルは変更しません |
| `CustomerSourceIdentity` | 任意。記録を許可された非機密・非pathの識別子 |
| `ExpectedFrameworkVersion` | 任意。root POMから取得したversionとの二重確認 |
| `FrameworkRepository` | 任意。既定はこのRepository。別のclean checkoutを検証するときに指定 |

`CustomerPom`を指定しない場合、Customer verificationは`NOT_REQUESTED`になります。この実行だけでは
P4-AR6のCustomer受入条件を満たしません。実Customer Repositoryに対する実行には、対象path、command、
出力および影響を示した別承認が必要です。

### 実行例

`StageRoot`と`ManifestOutput`は、Repository外の明示pathを指定します。次は構文例であり、実行前に
`ExpectedFrameworkCommit`と各pathを確認してください。Windowsではclone、Maven local repositoryおよびJava packageの
組合せでpathが長くなるため、Framework checkoutとstage rootには十分短いpathを使用してください。

```powershell
$commit = git rev-parse HEAD
pwsh -NoProfile -File build-support/adoption-readiness-verification/invoke-p4-ar6-r2-handoff.ps1 `
  -ExpectedFrameworkCommit $commit `
  -StageRoot 'C:\work\koiki-r2-stage' `
  -ManifestOutput 'C:\work\koiki-r2-evidence\manifest.json'
```

Repository内のCustomer-like Consumerを使うrehearsalでは、次のように指定できます。このfixtureによる
成功は、実アプリ開発チームの受入を代替しません。

```powershell
$commit = git rev-parse HEAD
pwsh -NoProfile -File build-support/adoption-readiness-verification/invoke-p4-ar6-r2-handoff.ps1 `
  -ExpectedFrameworkCommit $commit `
  -StageRoot 'C:\work\koiki-r2-stage' `
  -ManifestOutput 'C:\work\koiki-r2-evidence\manifest.json' `
  -CustomerPom 'C:\src\KOIKI-JAVAWEB\build-support\runtime-foundation-consumer\pom.xml'
```

## Safety and non-impact boundary

- stage / manifest / cleanup契約を維持し、cleanupを無効化しません。JDKがCDS警告を先行出力する環境でも
  manifestへ実際のJava / Maven version行を記録するようversion取得を防御しています。
- stage rootはfilesystem root、home、通常の`.m2/repository`、Framework / Customer Repository配下、
  非空directoryを拒否します。stage rootとmanifest出力先は、既存の祖先directoryを含めてlink / reparse pointを拒否します。
- cleanupはToolingが作成したmarker、canonical pathおよびexpected commitが一致した場合だけ実行します。
- Customer buildには`--no-snapshot-updates`を指定し、実行前後の全KOIKI payloadを再照合します。
- manifestへ絶対source path、stage path、credential、Customer source、dependency tree全文を記録しません。
- manifestまたはSHA-256 sidecarが既に存在する場合は上書きしません。
- manifestのcleanup PASSはTooling所有stage rootだけを対象とします。Customer process、port、containerおよび
  一時credentialのcleanupは、実チーム受入セッションで別途確認・記録する必要があります。
- 本Toolingは正式release、managed Maven repository、P4-AR6完了、Gate P4-ARまたはPhase 4開始を証明しません。

## External Reference boundary verification

`invoke-external-reference-boundary-verification.ps1`は、Git未管理のInitializr生成projectを一時的な
外部Referenceへ変換して検証する補足wrapperです。既存のR2 stage / manifest / cleanup契約を複製せず、
full modeでは`invoke-p4-ar6-r2-handoff.ps1`へ委譲します。

POM変換前は`BaselinePreflight`で、source Repository外に保存したInitializr baseline manifest、sidecar、
外部projectのfile count / size / SHA-256およびFramework commitを再照合します。このmodeはMavenを実行せず、
stage rootを作成しません。

```powershell
pwsh -NoProfile -File build-support/adoption-readiness-verification/invoke-external-reference-boundary-verification.ps1 `
  -Mode BaselinePreflight `
  -ExpectedFrameworkCommit '<40-character-framework-commit>' `
  -ExternalProject 'C:\KOIKI-JAVAWEB-BIZ-APP\EXT-REFERENCE' `
  -BaselineManifest 'C:\KOIKI-JAVAWEB-BIZ-APP\evidence\initializr-baseline-20260925.json' `
  -BaselineManifestSha256 'C:\KOIKI-JAVAWEB-BIZ-APP\evidence\initializr-baseline-20260925.json.sha256'
```

Reference source materialize、POM変換および外部Reference固有testのPublic / internal境界補正後は、`Full`で
変換済みPOM、production sourceのinternal参照0件を先に検査し、R2 Toolingへstage / build / dependency / Architecture
Rules / artifact不変 / cleanup検証を委譲します。`StageRoot`はFramework、外部project、通常の`.m2/repository`の
外側にある空または未作成のrun専用pathを指定します。

```powershell
pwsh -NoProfile -File build-support/adoption-readiness-verification/invoke-external-reference-boundary-verification.ps1 `
  -Mode Full `
  -ExpectedFrameworkCommit '<40-character-framework-commit>' `
  -ExternalProject 'C:\KOIKI-JAVAWEB-BIZ-APP\EXT-REFERENCE' `
  -BaselineManifest 'C:\KOIKI-JAVAWEB-BIZ-APP\evidence\initializr-baseline-20260925.json' `
  -BaselineManifestSha256 'C:\KOIKI-JAVAWEB-BIZ-APP\evidence\initializr-baseline-20260925.json.sha256' `
  -StageRoot 'C:\KOIKI-JAVAWEB-BIZ-APP\local-artifact-stages\run-<timestamp>' `
  -ManifestOutput 'C:\KOIKI-JAVAWEB-BIZ-APP\evidence\external-reference-run-<timestamp>.json' `
  -ExternalSourceIdentity '<approved-non-path-source-identity>'
```

このwrapperはReferenceをformal release unit、BOM、Root Reactor、Customer成果物またはProject Templateへ追加しません。
