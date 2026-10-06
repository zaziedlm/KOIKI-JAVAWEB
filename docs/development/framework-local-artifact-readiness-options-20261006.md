# Frameworkローカル開発のartifact準備：整備検討案（2026-10-06）

**状態:** 検討案。一端末・選択Toolingの最小実地検証はLOCAL PASS（§9）。全体方式の採用・Tooling実装・端末設定変更は未実施。
**Ownership:** Framework開発環境の支援文書。準備・診断を自動化する場合はTooling所有・非配布。
**目的:** 個別作業の直前にcache不足を解消する方式から、選択した開発用途に必要な資材を初回・更新時に準備し、使用可能範囲を確認できる方式へ整える。

## 1. 推奨する整備方針

**既存POMと実行入口を入力に、用途別の準備対象一覧、online準備、offline確認、更新・復旧を一組として整備する。** 全Maven artifactを先取りするのではなく、Framework通常開発を共通部分とし、独立Tooling・DB・browser・runtime互換検証を追加選択する。

準備が整えば日常の編集・testで再利用し、POM / profile / plugin / JDK / Wrapper等の変更時に必要部分を更新する。準備結果には対象POM・profile・goalと確認段階を記録する。「端末全体の準備完了」という範囲の曖昧な結果名を使わない。

[日常開発の反復方式案](daily-development-workflow-options-20261001.md)はCustomerが固定Framework artifactを利用するstageの設計。本案はFramework開発者自身のbuild / test環境を主対象とし、そのstage案・既存R2の契約を変更しない。今回のS1 cache欠落は一般的な不足検知の一例であり、対象範囲・資源上限・完了条件をS1から転用しない。

## 2. 現行資材から分かる不足

| 現行資材 | 確認した事実と整備上の課題 |
|---|---|
| [Wrapper](../../.mvn/README.md) | 公式bin型 / Wrapper 3.3.4 / Maven 3.9.16。Wrapper JARとMaven配布物の準備を分けて確認する必要がある |
| [環境診断script](../../build-support/scripts/check-build-environment.ps1) | Java / Wrapper / JAVA_HOME等を表示する。依存・plugin・image・browser準備の検査は含まない。Wrapper初回起動は取得を伴い得る |
| [bootstrap script](../../build-support/scripts/bootstrap-maven-wrapper.ps1) | Wrapperの再生成手段。開発端末のcache準備として呼ぶとtracked Wrapperが変わり得るため、通常の初回準備へ転用しない |
| [Root POM](../../pom.xml) | 正式moduleとReferenceを含む。独立`build-support` projectはRoot実行では準備されない |
| [Parent](../../koiki-parent/pom.xml) | compiler / Enforcer / toolchains / Surefire等とError Prone / NullAwayのprocessorを使用。通常のdependency一覧だけでは準備対象を表し切れない |
| [build-support](../../build-support/README.md) | 独立Consumer、profile付きfixture、browser、複数process等の用途がある。履歴再現用・負例・生成projectも存在し、全POMの一括実行は準備手順にならない |
| [アプリ開発チーム環境手順](application-team-development-environment-guide.md) | Customer向けR2・Proxy / JDK信頼・IDEの説明がある。Framework開発者用のartifact準備入口は別途必要 |

今回確認したRoot / Parent / Tooling POMと上記scriptはsource照合の入力であり、新しい準備方式の実行Evidenceではない。

## 3. 準備する資材と取得元

| 資材 | 準備対象・取得元 | 確認内容 |
|---|---|---|
| JDK / Maven起動資材 | build用JDK 21、選択時のみruntime用25、tracked Wrapperと指定Maven配布物。既存端末導入手順・公式配布元／承認済みmirror | 実際のJava / Maven home、version、配布物の出自・hash、CLI / IDE / AI実行ユーザーからの読取 |
| 外部Maven依存 | 対象POMのParent / BOM / JAR / 推移依存 / test依存。Centralまたは既存mirror等、実効設定が指定する取得元 | GAV・type・classifier・実解決先、必要POM / JAR、snapshotの扱い |
| build / 診断plugin | compiler / Surefire / Failsafe / Boot / Enforcer / toolchains、help / dependency、選択した品質検査pluginと依存・processor | 実効versionと対象goal、test provider等の実行時追加解決、offline実行の成立 |
| KOIKI自作artifact | 作業中sourceの選択moduleをlocal build / install、または承認済み固定snapshotをmanifestに従って取得 | source commitとdirty状態、座標・POM / JAR hash、local生成と配布物の区別 |
| container image | 選択したDB / IdP等とTestcontainers管理image。対象fixtureのREADME / sourceに従う | registry・tag / digest・OS / architecture、pull権限、local存在。起動試験は別段階 |
| browser等 | browser検証を選んだ場合のPlaywright binary等。採用済みToolingが指定する取得手順 | libraryとのversion対応、OS・保存先・起動確認。Maven準備と別に扱う |
| 開発者向け補助 | 必要に応じ外部dependencyのsources / javadoc | IDEでの参照。KOIKIの正式sources / javadoc配布を新設する判断にはしない |

mirror / proxy / credentials / trustは既存の端末設定と適用範囲を確認する。SSL検証の無効化やcredential入りURLを手順へ追加しない。settings実値・tokenをmanifestへ保存せず、非機密の設定識別子と取得元を記録する。

## 4. cacheとKOIKI artifactの使い分け

| 用途 | repositoryの扱い | KOIKIの更新方針 |
|---|---|---|
| Framework編集中のbuild / test | 開発者専用の作業用Maven local repository。現行`~/.m2/repository`利用と専用path利用を比較し、実装前に選ぶ | local SNAPSHOTの更新を許容。install時にsource identityを記録し、以前のhashで現在値を説明しない |
| 独立Tooling | 明示した作業用repositoryを使用。必要なKOIKI moduleを先に準備 | 現在sourceを試すのか固定baselineを再現するのかを入口で選び、必要moduleをinstall。formal配布manifestへToolingを追加しない |
| Customerの固定baseline利用 | commit別の永続stage案。方式採用・実装は既存の別判断に従う | stage内の記録対象KOIKI payloadを上書きせず、新baselineは新stageへ切替 |
| 受渡し / 空cache再現性検証 | 既存R2等の空の隔離repository | 既存契約・manifest・cleanupを維持。日常cacheを渡して合格扱いにしない |

まずFramework作業用repositoryの実効pathを可視化し、CLI / IDEの解決先を一致させる。通常cacheを使う場合も、同じ`0.1.0-SNAPSHOT`を他checkoutからinstallする取り違えを管理する。専用pathは分離しやすいが、外部依存の重複容量・設定負担を実測して選ぶ。

Maven local repository設定とWrapperのMaven配布物cacheは別に確認する。`-Dmaven.repo.local`を変更してもWrapper配布物まで準備されたとはしない。Windows / WSL / containerはpath・ユーザー・資材互換性を確認し、単一cacheの無条件共有を前提にしない。

## 5. 対象一覧と準備順序

準備対象は、次の組合せで管理する。一覧はdependencyのversionを手書きで複製せず、tracked POM / sourceを根拠として実効設定から生成する設計にする。

| 選択単位 | 入力・準備内容 | 初期方針 |
|---|---|---|
| 共通 | Wrapper / JDK、Rootの対象module、通常build・testに必要な依存とplugin | Framework開発者の基本セット |
| 独立Tooling | tool入口、対象source baseline、必要KOIKI module、POM / profile / goal | 利用するToolingだけ追加。履歴再現用は指定commitで扱う |
| DB / runtime検証 | fixtureが要求するimage、runtime JDK、資源・port | 選択用途の追加セット |
| browser / E2E | harness POM、browser資材、対象Reference JAR・起動条件 | 選択用途の追加セット |

1. source baseline、dirty差分、OS / JDK、settings識別子、実効repository、用途一覧を確定する。
2. online準備として既存Wrapperを初回起動し、指定Maven配布物を取得・確認する。Wrapper再生成は行わない。
3. 既存POMに従い外部依存・pluginを取得する。Reactor内KOIKIの解決が必要なら、依存順に選択moduleをbuild / local installする。未生成KOIKIをCentralから取得して代替しない。
4. 独立Toolingは必要KOIKI artifactの準備後、POM / profileごとに外部依存・pluginを取得する。相互排他的profileは別々に解決する。
5. image / browser等を選択セットに従って準備する。test / container起動は準備の取得操作から分けて実行範囲を示す。
6. offlineで解決・build・必要なtestを段階的に確認し、manifestへ確認できた範囲と不足を記録する。

`dependency:go-offline`は依存・plugin・reportとその依存を解決する標準候補。[Maven公式説明](https://maven.apache.org/plugins/maven-dependency-plugin/go-offline-mojo.html)。ただし本案では成功だけで全用途完了とせず、processorやtest実行時解決も対象goalのoffline実行で確認する。不要report取得の量も実測する。準備用pluginは完全座標・versionを固定する設計とし、最新versionをParentへ追加する作業へ広げない。

## 6. 完了条件と更新・復旧

| 確認段階 | 完了の条件 | その段階だけでは確認できないこと |
|---|---|---|
| 起動資材 | 選択ユーザーでJava / Wrapper起動、version / path確認 | project依存の充足 |
| 依存解決 | 対象POM / profileのoffline artifact解決成功、必須pluginの不足なし | compiler / test / runtime全体の成立 |
| build可能 | 対象module / Toolingのoffline compile・test compile・package等、明示したgoal成功 | skipしたtest、DB / browser起動、品質Gate |
| test可能 | 選択した既存testをoffline実行し、XML・終了コード・runtime資材を確認 | 未選択用途、Framework全回帰、受渡し認定 |

manifest候補は、run ID・日時・OS / architecture・Java / Wrapper・source identity / dirty・POM / profile / goal・repository識別子・取得元・KOIKI payload hash・image digest・容量 / 時間・確認段階・失敗理由。日常準備の記録をR2 PASS manifestと混同しない。

更新は初回・POM / BOM / plugin / profile変更・JDK / Wrapper更新・用途追加・端末移行時に行う。通常のtestは準備済みcacheを再利用し、毎回`-U`や全量取得を行わない。外部SNAPSHOT更新は明示し、KOIKI固定baseline利用ではmanifestを優先する。

不足時はGAV / type / classifier、plugin / goal、profile、実効path、権限／通信／本当の欠落を切り分ける。online準備を明示して不足分を補い、同じ対象をoffline再確認する。hash不一致・破損時は対象座標を限定して対処し、`~/.m2`全体削除を標準手順にしない。clean / cache削除 / 保持期間は用途と所有範囲を決める。

`-o`はMavenのoffline実行指定であり、Wrapper初回取得やtest自身のHTTP、image pullまで止めるものとして扱わない。[Maven CLI](https://maven.apache.org/ref/3.9.16/maven-embedder/cli.html)、[Wrapper公式説明](https://maven.apache.org/tools/wrapper/)。準備と検証の両方で実行環境の権限手順へ従い、拒否されたアクセスを別cacheやユーザー変更で迂回しない。

## 7. 整備を実行可能にする次の単位

最初は**Framework開発者一端末の共通セットと、選択した独立Tooling一つ**で方式を実証する案を推奨する。全Tooling・全OSの完了は約束せず、準備対象を追加できる構造を確認してから広げる。

| 段階 | 具体的な成果と出口 |
|---|---|
| 設計確定 | 作業用repository、取得元、対象POM / profile / goal、準備plugin version、local install対象、担当と容量・通信・時間上限を決める |
| 最小Tooling | 診断・準備・offline確認の入口、非機密manifest、失敗時の不足表示を作る。既存POM・Root・CIへ自動接続しない |
| 実証 | 初回準備・二回目再利用・用途追加・依存変更・不足／権限拒否からの復旧を検証。時間・容量・通信とCLI / IDE整合を記録 |
| 手順反映 | 実証済み操作をFramework開発者の入口へ追加し、既存scriptとガイドから導線を置く。Customer / R2の手順は対応判断なしに変更しない |

実装開始時には上記の具体入力と取得／install／検証の範囲を提示する。最初の考慮検討の依頼は、download・local install・設定変更・全test実行の開始判断として扱わなかった。その後の最小実地検証は§9へ記録する。remote publish / CI変更・正式配布基盤は別範囲。

## 8. 参照する正本

- [KOIKI project overview Skill](../agent/skills/koiki-project-overview/SKILL.md)、[Repository guidance](../../AGENTS.md)
- [Maven Settings Reference](https://maven.apache.org/settings.html)：local repository、mirror / proxy / serverの設定
- [Maven Install Plugin](https://maven.apache.org/plugins/maven-install-plugin/)：local installの意味
- [日常開発方式案](daily-development-workflow-options-20261001.md)：固定Frameworkを使うCustomerの日常stage
- [開発環境構築手順](application-team-development-environment-guide.md)：既存端末・Proxy・R2・IDEの説明

本案の整理・source照合は完了。最小実地検証の範囲を越える準備方式の実装・実証と採用判断は残る。

## 9. 最小実地検証の結果（2026-10-06）

Ownerから不足解消を実地検証しながら進める指示を受領し、既存一端末のMaven cacheと`phase4-level2-verification` / `jdbc`を対象にonline resolve / go-offline → offline resolve → 既存DB testを実行した。[環境整備Evidence](../architecture/validation/framework-local-artifact-readiness-20261006.md)。

既存POMの不足依存・pluginを取得し、offline依存解決と既存L1の11件が成功。続く承認済み[AのJPA検証18件](../architecture/validation/phase4-s1-additional-a-jpa-20261006.md)も成功した。POM / 権限 / artifact version変更やKOIKI再installを必要とせず、cache準備で作業再開できることを確認した。

Root共通セット・空cache・全profile / Tooling・IDE一致・更新／切戻し・容量／通信量は未実証。恒常運用のToolingは未作成で、今回の成功をFramework開発環境全体の準備完了とはしない。
