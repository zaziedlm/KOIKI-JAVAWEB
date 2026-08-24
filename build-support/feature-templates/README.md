# KOIKI Feature Templates

Phase 1a B1で提供する、Tooling所有のFeature Template、生成ツールおよび検証資材です。
既存Applicationへ追加する1つのFeatureを生成し、Application全体を生成するPhase 5の
Project Templateとは区別します。Root Reactorおよび正式release unitには含めません。

Template、生成ツールおよびRepository内の検証fixtureはToolingが継続して所有する。一方、生成された
業務Maven moduleは生成先Applicationの成果物であり、Customer ApplicationではCustomer、
Reference ApplicationではReferenceが所有する。生成物をFrameworkまたはTooling所有へ昇格させない。

B1資材をPhase 5のProject Templateから内部利用するか、Phase 5向けに再設計するか、または正式な
顧客向け生成ツールとして配布するかはPhase 5で判断する。B1 Feature TemplateをProject Templateへ
自動的に昇格させない。

## 提供するTier

| Template | 用途 | 生成する責務 |
|---|---|---|
| `tier1-simple` | 単純CRUD、マスタ、設定管理 | Application Use Case、振る舞いを持たないJPA model、Spring Data Repository |
| `tier2-rich` | 不変条件と状態遷移を持つ機能 | Application Use Case、JPA兼用Domain Model、Domain所有Repository |

使用しないController、DTO、Domain Service、Event、Gateway、Query Port、Configurationは生成しません。
両Templateとも、モジュールrootの`package-info.java`で`@NullMarked`と`@KoikiModule`を宣言します。

### 生成コードの位置づけ

生成する`label`の空文字拒否および`DRAFT -> SUBMITTED -> APPROVED`の状態遷移は、Tierごとの責務配置を
コンパイルとtestで実証するための最小サンプルです。KOIKIの標準業務語彙または共通業務ルールでは
ありません。生成先ApplicationのOwnerは、実際の業務要件に基づいてclass名、属性、不変条件、状態、
遷移およびtestを置き換えます。

B1は構造、コンパイルおよび後続quality gateの接続点を示すTemplateであり、実行可能Applicationを
生成しません。Application Use CaseのSpring Bean登録、transaction境界、runtime Configurationは
生成せず、実Applicationへ組み込むPhaseで承認されたruntime構成に従って追加します。

## 生成方法

Repository rootからJDK 21のsource-file modeで実行します。出力先が既に存在する場合は、既存資材を
上書きせず失敗します。

### 入力契約

| option | 必須 | 値 |
|---|---|---|
| `--tier` | yes | `tier1-simple`または`tier2-rich` |
| `--module-name` | yes | 小文字で始まる小文字英数字のJava package segment |
| `--class-name` | yes | 大文字で始まる英数字のJava class名 |
| `--artifact-id` | yes | 生成moduleのMaven artifactId |
| `--base-package` | yes | 2 segment以上の小文字英数字Java package |
| `--parent-group-id` | yes | 既存Application parentのgroupId |
| `--parent-artifact-id` | yes | 既存Application parentのartifactId |
| `--parent-version` | yes | 既存Application parentのversion |
| `--parent-relative-path` | yes | 生成moduleから既存Application parent POMへの相対path |
| `--output` | yes | 新規作成するmodule directory。既存pathは指定不可 |

optionはすべて`--name=value`形式で指定します。同じoptionの重複、必須optionの欠落、許容形式に
合わない値は失敗します。

Phase 1a B1の生成ツールは未知option名を拒否せず無視する既知制約があります。入力ミスを避けるため、
上表のoptionだけを指定してください。正式な顧客向け生成ツールとして運用する前に、未知optionの
拒否と利用者向けerror表示を再検討します。

### PowerShell

```powershell
java build-support/feature-templates/GenerateFeature.java `
  --tier=tier1-simple `
  --module-name=master `
  --class-name=MasterItem `
  --artifact-id=master-feature `
  --base-package=org.example.application `
  --parent-group-id=org.example `
  --parent-artifact-id=example-application `
  --parent-version=0.1.0-SNAPSHOT `
  --parent-relative-path=../pom.xml `
  --output=modules/master
```

### Bash

```bash
java build-support/feature-templates/GenerateFeature.java \
  --tier=tier1-simple \
  --module-name=master \
  --class-name=MasterItem \
  --artifact-id=master-feature \
  --base-package=org.example.application \
  --parent-group-id=org.example \
  --parent-artifact-id=example-application \
  --parent-version=0.1.0-SNAPSHOT \
  --parent-relative-path=../pom.xml \
  --output=modules/master
```

生成後、既存ApplicationのAggregator POMへ出力moduleを明示的に登録します。生成ツールは、既存POM、
業務コード、migrationまたは設定を自動変更しません。

Tier 2では`--tier=tier2-rich`を指定します。`--module-name`は小文字英数字のJava package segment、
`--class-name`はJava class名として指定します。正式package体系は、Customer Applicationが所有する
base packageの配下で構成します。KOIKI自身の正式Public APIだけが`org.koikifw`を使用します。

### 失敗時の扱い

- 指定した最終出力先が既に存在する場合は、上書きせず失敗します。
- Phase 1a B1では最終出力先へ直接生成するため、process中断またはI/O failureにより部分出力が残る
  可能性があります。再実行前に、指定した出力先が生成途中の資材だけであることを確認し、その出力先
  だけを手動で除去してください。
- 生成のall-or-nothing化、一時directoryからのmoveおよび自動cleanupは、正式な顧客向け運用を判断する
  Phase 5で再検討します。B1では削除処理を一般利用向け生成ツールへ追加しません。

## Repository内positive verification

次のcommandはCustomer相当のAggregator配下へTier 1の`catalog`とTier 2の`approval`を生成し、
unit testとSpring Modulith Level 0を含む`mvn verify`を実行します。

```powershell
pwsh -NoProfile -File build-support/feature-templates/verify-feature-templates.ps1
```

Spring Modulithは`architecture-tests`のtest scopeだけで利用し、runtime dependency treeへ含まれない
ことも検査します。ArchUnit rulesの統合と負例、NullAwayの正式な負例はB2〜B5で追加します。
