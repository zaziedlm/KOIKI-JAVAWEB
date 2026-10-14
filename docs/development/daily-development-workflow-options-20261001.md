# 作業2：日常開発の反復方式の比較・推奨案（2026-10-01）

**状態:** OWNER APPROVED — 文書承認 / 2026-10-01。比較・推奨案の文書化は完了。方式の採用・Tooling実装は別途承認。
**位置付け:** [Pre-Phase 4作業順序案§3](pre-phase4-framework-independent-work-review-20260930.md#3-framework側で単独に進める作業と順序案)の作業2。
**Ownership:** Framework側の採用支援文書。想定するstage処理は非配布Tooling、Customer code・test・端末設定はCustomer所有。
**確認baseline:** `docs/daily-development-workflow` / `7653d16`。比較と推奨案の作成までを行い、実行時間・IDEでの再現性は未実測。

## 1. 推奨案

移行期の日常開発には、**固定Framework commitごとに開発者専用の永続Maven local repository（日常用stage）を用意し、IDEとCLIで同じstageを使用する方式**を推奨する。
Customerの編集・testごとにFrameworkをbuildし直さず、Frameworkの更新を取り込むときに新しいstageへ切り替える。
受渡しの合否は引き続き、空のrepositoryから実行する既存R2で確認する。

この推奨は、CLIとIDEの解決先一致を承認後の検証で実証できることを条件とする。日常用stageは端末ごとの作業cacheであり、チームやCIへ配布するrepositoryにはしない。
複数人・CIの共通配布は、[受渡し契約§3.2.1](../architecture/validation/pre-phase4-p4-ar6-actual-project-handoff-contract.md#321-isolated-maven-repositoryの意味)のR1（managed Maven repository）として別途判断する。

### 1.1 C案を第一候補とする理由と試用の位置付け

C案は、既存の外部Reference検証で使用した「固定Framework commitから隔離repositoryへstageし、別projectからMaven座標で参照する」仕組みを、日常開発へ拡張する案である。
既存の[外部Reference検証](../architecture/validation/adoption-external-reference-boundary-validation.md)はbuild後にstageを削除し、生成済みJARを起動する。C案ではstageを保持し、IDE接続、Customerのtest反復、Framework更新・切戻しを追加する。
Repository内ReferenceのRoot Reactorによるpackage・起動手順とは区別する。Reference Appを動かした経験は説明の土台になるが、日常開発の運用を受け入れたEvidenceとは扱わない。

Owner reviewでは、C案を第一候補としてFramework側の小規模検証へ進めることを提案する。
技術的に成立した後、アプリ開発チームが初回準備・日常test・更新・復旧を試用し、理解と操作負担を確認してから採用を判断する。実チームの試用担当・時期・環境はOPENとする。

## 2. 現行手順から確認したこと

| 確認事項 | 根拠と日常開発への影響 |
|---|---|
| R2は使い捨てrepositoryを使う | [R2 script](../../build-support/adoption-readiness-verification/invoke-p4-ar6-r2-handoff.ps1)は空のstageを要求し、`finally`で所有markerを確認して削除する。保持用parameterはない |
| Framework stageとCustomer検証を同じrepositoryで実行する | R2はformal unitを`clean install -DskipTests`し、Customerを`--no-snapshot-updates clean verify`する。stage時のtest省略をFramework回帰PASSと扱わない |
| 通常のlocal repositoryから隔離する | R2は`-Dmaven.repo.local`を渡し、通常の`~/.m2/repository`配下をstageとして拒否する |
| R2終了後のIDE依存解決は未提供 | [開発環境構築手順§8.3](application-team-development-environment-guide.md#83-ide上のkoiki依存解決と日常操作)は、古いSNAPSHOTの参照とIDE未解決を既知の制約としている |
| 日常用stageは既存R2契約に含まれない | 同§8.3は、所有権・更新・破棄を別Toolingとして設計することを求める。R2のcleanupを無効にして流用しない |

## 3. 選択肢の比較

| 案 | IDE・日常test | identity・更新 | Ownership・破棄 | 評価 |
|---|---|---|---|---|
| A：毎回R2 | CLIの隔離検証は提供済み。終了後のIDE解決は改善しない | 毎回固定commitとchecksumを検査 | 既存Toolingが実行ごとに削除 | 受渡し確認として維持。日常反復の課題は残る |
| B：通常の`~/.m2`へinstall | 多くの既定設定と合わせやすい。KOIKIを入れた後はCustomerだけでtestできる見込み | 同じ`0.1.0-SNAPSHOT`を別commitで上書きすると出自を追いにくい。専用の記録・照合が必要 | 開発者の共用cache。ほかのprojectのKOIKI利用と競合し、削除範囲を限定する必要がある | 設定の負担は小さいが、複数baselineの併存と取り違えに弱い |
| C：commit別の日常用stage | CLIとIDEの解決先設定が必要。設定後はCustomerだけで反復する | 新しいcommitは新しいstageに作成し、検査後に切替。同一stageのKOIKIを上書きしない | Toolingが作成・検査・所有確認後の削除を担当。使用開始・更新時期はCustomer側が判断 | **推奨候補**。設定とToolingの追加が必要。IDE接続の実証が採用条件 |
| D：managed Maven repository | IDE・CLI・CIからversion付きartifactを取得する正式到達形 | release identity・publish権限・retention等を決定する必要がある | release / platform Ownerが運用 | E1 / R1の別判断。作業2を理由に配布基盤を先行実装しない |

案Bを選ぶ場合も、formal unitだけのinstall、Framework commitとpayload hashの記録、更新時の整合確認が必要になる。
IDEから案Cのstageを安定して参照できない場合は、症状と設定を記録して案Bを再比較し、Ownerへ戻す。

## 4. 案Cの作成・更新・破棄の設計案

### 4.1 配置とidentity

Framework / CustomerのGit Repository外に、開発者が所有する日常用stageの親directoryを置く。
各stageは40桁Framework commitと生成IDで区別する。同じcommitの再作成でも既存stageを上書きしない。

```text
作業directory/
├── KOIKI-JAVAWEB/            Framework clean checkout
├── customer-app/             Customer Git Repository
├── daily-artifact-stages/    開発者専用。Git管理・他人への配布をしない
│   └── <commit>-<生成ID>/    Maven local repository
└── evidence/                 日常用stageの記録とR2 Evidenceを区別して保管
```

作成時は固定commit・clean checkout・JDK 21を確認し、空のstageへ現行formal unitをbuild / installする。
15 projects / 12 JARのinventory、非配布artifactの非混入、KOIKIのPOM / JARのSHA-256を確認する設計とする。
source commit、Framework version、tool identity、artifact一覧・hash、生成ID、検査結果を記録する。
日常用stageの記録はR2 PASS manifestと区別し、同じschema・結果名を無条件に流用しない。

外部dependency cacheとCustomer buildによるmetadataは更新され得る。保持中に不変とするのは、記録対象のKOIKI座標とPOM / JAR payloadである。
Customerの`install`も`org.koikifw`を上書きしないよう境界を確認する。

### 4.2 利用と更新

1. 作成・検査に成功したstageだけを利用対象にする。途中失敗したstageは利用しない。
2. Customer CLIとIDEのMaven importに同じlocal repositoryを明示する。Framework sourceへの`relativePath` / `systemPath`やRoot Reactorへの組込みを使わない。
3. 日常反復ではCustomer sourceを編集し、対象unit testとArchitecture Rules testを実行する。まとまった変更ではCustomerの`clean verify`も行う。
4. Framework更新時は取り込むcommitを明示し、新規stageの検査とCustomer検証が成功してからIDE・CLIの参照先を切り替える。自動で最新mainへ追随しない。
5. 元のstageは切替確認まで保持する。切戻しは元stageを再指定し、Customer sourceとの組合せも記録する。

同じstageへのFramework再installや複数Framework buildの同時書込みを禁止する設計とする。
IDE・CLIの更新後にKOIKI hashと解決先が一致しない場合、日常testの結果をそのbaselineの結果として採用せず、設定とstageを確認する。

### 4.3 破棄

使用終了は開発者が判断する。別のIDE session・processが使っていないことを確認してから、Toolingが所有markerと解決済みpathを照合して、そのstageだけを削除する。
filesystem root、home、通常のlocal repository、Framework / Customer Repository、link / reparse point、所有を確認できないdirectoryは削除対象にしない。
古いstageの自動期限削除は初期案に含めない。Evidenceはstage外へ残す。保存期間はOwner判断事項とする。

## 5. CLIとIDEの解決先を揃える方法

Mavenは`settings.xml`の`localRepository`で解決先を指定できる（[Maven Settings Reference](https://maven.apache.org/settings.html)）。
VS Code Java拡張には、Maven user settingsを指定する`java.configuration.maven.userSettings`がある（[拡張の公式README](https://github.com/redhat-developer/vscode-java#supported-vs-code-settings)）。
これを使い、Customer用の端末local settingsで案Cのstageを指定する方法を検証候補にする。

CLIは同じsettingsを明示する案とする。以下は設計例であり、現在の実行手順ではない。

```powershell
# Customer Repositoryで実行する設計例。settingsの作成・stage準備は承認後。
.\mvnw.cmd -s '<Customer用local settings.xml>' --no-snapshot-updates test
.\mvnw.cmd -s '<Customer用local settings.xml>' --no-snapshot-updates clean verify
```

既存のproxy / mirror / server設定を保持する方法を設計し、credentialや端末絶対pathをCustomer POMや共有設定へcommitしない。
user全体の`~/.m2/settings.xml`を自動で書き換えず、Customer用local設定の適用範囲を確認する。
FrameworkとCustomerを並べるVS Code multi-rootでは、Java import設定がFramework側にも及ぶかを確認する。分離できない場合はCustomer専用windowを検証候補とする。
Maven拡張のterminal command、Java Language Serverのimport、IDEのtest実行は別々に解決先を確認する。他IDEやLinux / WSLでの再現性はOPENとする。

`--no-snapshot-updates`だけでpayloadの不変性を保証したとは扱わない。日常用ToolingでのKOIKI hash照合を別途設計する。
IDE上の成功だけではMaven compilerのNullAway等の実行を保証できないため、CLIのtest / verifyを検証に含める。

### 5.1 アプリ開発者の操作と支援の分担案

開発者へは「準備・開発・更新」の3つの場面で説明し、日常の編集ごとにstageやchecksumを手作業で管理させない。
次の表は承認後に提供する操作の設計目標であり、現在使えるcommand一覧ではない。

| 場面 | アプリ開発者が行うこと | Tooling・Framework側が支援すること |
|---|---|---|
| 準備 | チームが選んだbaselineを指定して準備し、IDEの接続とCustomer testの成功を確認する | formal unitのstage・検査、使用baselineの表示、IDE / CLI設定の案内 |
| 開発 | Customer sourceを編集し、対象testと必要なverifyを実行する | 同じstageを使い、KOIKI identityの確認をまとめて実行する。Framework再buildは不要とする |
| 更新 | チーム内で決めた更新時期に新baselineを準備し、検証後に切り替える | 新旧stageを区別し、切替確認と切戻しの手順を提供する |
| 失敗・使用終了 | 症状と使用baselineを確認し、相談・切戻し・削除のいずれかを選ぶ | 失敗理由、次の確認先、所有確認付きの削除を提供する |

Framework側は候補baselineとToolingの仕様・診断方法を提示し、Customer側は利用するbaseline、更新のタイミングと担当を決める。
チーム内の担当者・相談先は試用開始前に確認する。別baselineへの自動切替や、解決失敗時の通常`~/.m2`への暗黙fallbackは行わない設計とする。

## 6. 日常testとR2検証の使い分け案

| 場面 | 案C採用後の確認 | 結果の意味 |
|---|---|---|
| Customerの編集からtestまで | 同一stageで対象test。構造変更時はArchitecture Rulesの2検査も行う | 開発中の局所確認 |
| まとまった変更・review提出前 | 同一stageのCustomer `clean verify`、KOIKI identity照合 | そのstageとCustomerの組合せでの回帰結果 |
| Framework commit切替・受渡し・受入Evidence作成 | 同じFramework commitとCustomer sourceを対象に、別の空stageで既存R2を実行 | 偶発cacheに依存しない既存R2契約の合否。実チームのrun / diagnosis等は別Evidence |

受渡しに日常用stageを配ったり、日常のverify成功をR2 PASSへ読み替えたりしない。
承認・実装・実証・ガイド更新が済むまでは、公開済みの開発環境構築手順とアプリケーション開発者ガイドの現行手順を維持する。

## 7. Owner判断事項と承認後の検証案

Ownerには次を判断いただく。

1. 案Cを移行期の日常開発の検証対象とするか。案Bとの運用負担の比較を含める。
2. 専用Toolingの作成・検査・照合・明示削除までの実装範囲と担当Owner。既存R2のcleanup契約は維持する。
3. 初期の検証環境・IDE、stageの更新を決める担当、Evidence保存期間。実チーム側の担当は未取得として扱う。

承認後には以下を実証し、結果を`docs/architecture/validation/`へ記録する。

| 検証 | 受入条件 |
|---|---|
| 初回作成と反復 | 固定commit・inventory・hashを記録し、Frameworkを再buildせずCustomerのtestを反復できる |
| CLI / IDEの一致 | Parent / BOM / Starter / Architecture Rulesの参照先とversionが一致し、補完とCLI testが成立する。通常の`~/.m2`にある別baselineへ依存しない |
| 更新と切戻し | 新規stageへ切替後、IDE cacheも更新される。元stageへ戻すと元のidentityで解決する |
| 失敗・破棄 | 作成失敗、hash不一致、所有marker・path不一致では停止する。削除範囲が対象stageに限定される |
| R2との整合 | 同じFramework / Customer sourceで既存R2がPASSする。日常用stageをR2へ渡さない |
| 負担の比較 | stage初回作成、Customerの反復test、R2の所要時間とdisk使用量を計測する。現時点で短縮率を主張しない |

### 7.1 試用後の採否条件

Framework側の技術検証を満たした後、実チームの試用では次を確認する。回答と実行結果を記録し、チームの理解を推測で完了扱いにしない。

- 開発者が、使用baselineの確認方法と、日常test・R2検証の使い分けを説明できる。
- 提供手順を使って、Frameworkを再buildせずCustomerの編集・testを反復できる。
- チームが選んだ担当者が、新baselineへの更新と元baselineへの切戻しを行える。
- 依存解決の失敗時に、確認する情報と相談先が分かり、別baselineへ無意識に切り替わらない。
- 初回設定、通常操作、disk管理、問い合わせ対応の負担をチームが確認し、残るfindingと担当が記録される。

結果はOwner reviewへ戻し、C案の採用、修正して再試用、案Bとの再比較を判断する。
承認後に実装・検証できた操作だけを開発環境構築手順§8.3とアプリケーション開発者ガイド§4.2へ反映する。現段階では追加の運用マニュアルを先行作成しない。

本案の方式採用、Tooling実装、IDE設定変更、Maven cacheへの書込み・削除は未実施。Phase 4 production開始、正式配布、Public API変更を承認するものではない。

## 8. 文書承認記録（2026-10-01）

Architecture Ownerは本書の内容を確認し、文書を承認した。作業2は比較・推奨案の文書化までを完了とし、本承認をcommit pointとする。

| 承認範囲 | 後続の承認・調整事項 |
|---|---|
| 選択肢の比較、C案の推奨理由と設計案、開発者の操作・責任分担案、検証・試用後の採否条件 | C案を検証対象として進める判断、Tooling実装範囲と担当Owner、初期検証環境・IDE・Evidence保存方針 |
| 検討資料の提供から、承認後の実装・Framework側検証、実チーム試用、採用判断へ進む展開方針 | 実チームの担当・時期・環境の合意、方式の正式採用、実行手順の提供 |

文書承認を方式採用・Tooling実装承認へ読み替えない。アプリチームとの合意成立と試用結果は、先方の回答・Evidenceを受けて記録する。
