# Pre-Phase 4 Adoption Readiness Verification

このdirectoryは、P4-AR6でArchitecture Ownerが承認したR2 stage / manifest契約を実証する、
Tooling所有の非配布資材です。Root Reactor、BOM、formal release unit、Framework Public API、
Starter、migrationおよびworkflowには含めません。

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

- 既存の検証scriptを変更、wrapまたはcleanup無効化していません。
- stage rootはfilesystem root、home、通常の`.m2/repository`、Framework / Customer Repository配下、
  非空directoryを拒否します。stage rootとmanifest出力先は、既存の祖先directoryを含めてlink / reparse pointを拒否します。
- cleanupはToolingが作成したmarker、canonical pathおよびexpected commitが一致した場合だけ実行します。
- Customer buildには`--no-snapshot-updates`を指定し、実行前後の全KOIKI payloadを再照合します。
- manifestへ絶対source path、stage path、credential、Customer source、dependency tree全文を記録しません。
- manifestまたはSHA-256 sidecarが既に存在する場合は上書きしません。
- manifestのcleanup PASSはTooling所有stage rootだけを対象とします。Customer process、port、containerおよび
  一時credentialのcleanupは、実チーム受入セッションで別途確認・記録する必要があります。
- 本Toolingは正式release、managed Maven repository、P4-AR6完了、Gate P4-ARまたはPhase 4開始を証明しません。
