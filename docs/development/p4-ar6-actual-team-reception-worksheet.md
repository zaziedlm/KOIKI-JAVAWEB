# P4-AR6 実チーム受入セッション worksheet v0.1

| 項目 | 状態 |
|---|---|
| Worksheet | `PREPARED / OWNER APPROVED` |
| Actual-team session | `PENDING` |
| Ownership | Framework / Customer / Architecture joint Evidence preparation |
| Customer data | 未記入。機密情報を本書へ持ち込みません |
| Phase 4 start | 未承認 |

## 1. 目的と使い方

本書は、実際のアプリ開発チームがKOIKI-JavaWeb-FWの提供物、利用境界および不足機能の戻し先を理解し、
自分たちの環境でbuild / run / operation / diagnosis / cleanupを再現できるか確認するための実施用worksheetです。

最初に[アプリ開発チーム向け引継ぎガイド](application-team-handoff-guide.md)を読み、本書へ進んでください。
R2 Toolingの安全条件とRepository topologyの正本は
[P4-AR6 Actual-project Handoff Contract](../architecture/validation/pre-phase4-p4-ar6-actual-project-handoff-contract.md)です。

本書の表は、左側から「確認すること」「非機密Evidence」「結果」の順に読みます。`結果`には`PASS`、`FAIL`、
`PENDING`または`NOT APPLICABLE`を記入し、判断不能を`PASS`にしません。未決定事項は無理に埋めず、Ownerと次Gateを記録します。

本書はFramework Repositoryで管理する空の様式です。実施結果をRepositoryへ反映するときは、Customer機密情報を除いたsummaryだけを
P4-AR6 Evidenceへ転記します。Customer側で詳細記録が必要な場合は、Customer管理下の記録へ分離してください。

## 2. 記録してよい情報と記録しない情報

次の表は、再現性に必要な情報と、Framework Repositoryへ持ち込まない情報を分けるために使います。

| 記録してよい非機密情報 | 本書・P4-AR6 Evidenceへ記録しない情報 |
|---|---|
| 実施日、参加role、KOIKI経験の有無 | 個人名、メールアドレス、組織内の個人識別子 |
| Framework commit、Tooling SHA-256、manifest SHA-256 | token、Cookie、password、private key、接続文字列 |
| Customer sourceの承認済み非機密identityまたは承認ID | Customer Repositoryの絶対path、remote URL、source本文 |
| JDK / Maven / Dockerのversion、OS種別 | Customer業務data、schema詳細、SQL値、個人情報 |
| PASS / FAIL、所要時間、件数、sanitized finding | log全文、stack trace全文、dependency tree全文 |
| profile / Starter / topologyの選択理由 | IdP、network、host、account等の機密設定値 |

機密情報がなければ原因を説明できない場合は、Framework Repositoryへの転記を停止し、Customer側の承認済み経路で扱います。

## 3. セッション識別情報

この表には、セッションを一意に識別できる最小限の情報だけを記録します。

| 項目 | 記入欄 |
|---|---|
| Session ID | `PENDING` |
| 実施日 | `PENDING` |
| 参加role | `PENDING` |
| KOIKI経験 | `PENDING` |
| Framework source commit | `PENDING` |
| Tooling script SHA-256 | `PENDING` |
| Customer source identity kind | `PENDING` |
| Customer source identity / approval ID | `PENDING` |
| 記録担当role | `PENDING` |

## 4. 実行前確認と承認パケット

### 4.1 環境とRepository境界

次の表は、実行不能をFramework不具合へ誤分類しないためのpreflightです。実値ではなく、version、可否、承認状態だけを残します。

| 確認項目 | 必要な非機密Evidence | 結果 |
|---|---|---|
| JDK | Java 21のvendor / exact version、`java` / `javac`利用可 | `PENDING` |
| Maven | KOIKI Maven Wrapperを実行可能 | `PENDING` |
| Docker | journeyで必要な場合だけengine / compose利用可 | `PENDING` |
| Network | Maven repositoryへの到達可否。proxy値やcredentialは記録しない | `PENDING` |
| Framework Repository | Customer Repositoryとは別Git Repository、固定commit、clean | `PENDING` |
| Customer Repository | KOIKI Root Reactor外、Customer Ownership | `PENDING` |
| Workspace | multi-root利用時もGit / POM / lifecycle / credentialを分離 | `PENDING` |
| Stage root | 両Repository外、短いpath、空または未作成、non-link | `PENDING` |
| Manifest出力先 | stage / source Repository外、retentionと削除Ownerを確認 | `PENDING` |

### 4.2 実Customer Repository操作の事前承認

実Customer Repositoryを読み取る、buildする、またはR2 Toolingへ渡す前に、この表を完成させてOwner承認を得ます。
正確なlocal pathやCustomer remote URLが必要な承認記録はCustomer管理下へ置き、本書には承認IDだけを残します。

| 承認項目 | 事前に示す内容 | 記入欄 |
|---|---|---|
| 対象 | Repository識別子、branch / commit、対象root POM | `PENDING` |
| read-only inventory | POM、module、test、JDK / Maven / Docker前提の確認範囲 | `PENDING` |
| 実行command | command、parameter、実行主体。credentialを含めない | `PENDING` |
| 生成物 | `target/`、isolated stage、manifest、sidecar、local logの範囲 | `PENDING` |
| source影響 | Customer POM / sourceを変更しないこと、fingerprint確認方法 | `PENDING` |
| resource影響 | process、port、container、一時credentialの可能性 | `PENDING` |
| cleanup | Tooling所有stageとsession resourceを誰がどう確認するか | `PENDING` |
| Evidence | Frameworkへ転記する非機密summaryとCustomer側保管物 | `PENDING` |
| Owner approval | 承認者role、承認ID、承認範囲 | `PENDING` |

次のいずれかが残る場合は実行を開始しません。

- 対象Repository、commitまたはroot POMが未確定です。
- command、生成物またはcleanup対象を説明できません。
- Customer source、credentialまたは業務dataをFramework Repositoryへ持ち込む必要があります。
- Public API、Starter、dependency、migration、Security既定またはworkflow変更が必要です。
- 実Customer Repository操作のOwner承認がありません。

## 5. 受入セッション実行記録

### 5.1 単一入口と提供境界

この表では、説明を受けたかではなく、実チームがガイドから自分で判断できたかを確認します。

| 確認内容 | 非機密Evidence | 結果 |
|---|---|---|
| Handoff Guideを最初の入口として開始できました | 最初に迷った箇所、追加で参照した文書 | `PENDING` |
| Framework artifactとReference / Consumer / Toolingを区別できました | 提供候補と非配布物の説明結果 | `PENDING` |
| 現時点で提供されないものを説明できました | React / BFF、正式repository、Project Template等の理解 | `PENDING` |
| MVC / REST / frontend profileの次の判断先を選べました | 選択候補、理由、未決定事項 | `PENDING` |
| KOIKI Parent継承またはCustomer Parent + BOM importを評価できました | 選択候補、理由、空の`<relativePath/>`等の境界 | `PENDING` |
| 必要なStarter候補を選べました | artifactIdだけを記録。versionは単一identity | `PENDING` |

### 5.2 ReferenceまたはConsumer journey

実チームが選んだ一方を30〜60分の目安で実行します。時間超過だけでFAILにはしませんが、迷いまたは暗黙前提をfindingにします。

| 項目 | 記入欄 |
|---|---|
| 選択したjourney | `PENDING` |
| 開始／終了時刻または所要時間 | `PENDING` |
| build結果 | `PENDING` |
| run結果 | `PENDING` |
| representative operation | `PENDING` |
| 成功観測点 | `PENDING` |
| diagnosisで参照した場所 | `PENDING` |
| 迷った箇所／暗黙前提 | `PENDING` |
| cleanup結果 | `PENDING` |

### 5.3 最初のCustomer-owned module

ここではproduction codeを生成せず、最初の業務moduleの配置と責務を説明できるかを確認します。

| 判断項目 | 実チームの説明 | 結果 |
|---|---|---|
| Customer Repository内の配置 | `PENDING` | `PENDING` |
| 業務能力とmodule名 | `PENDING` | `PENDING` |
| Tier 1 / Tier 2 | `PENDING` | `PENDING` |
| Inbound / Application / Domain / Outbound責務 | `PENDING` | `PENDING` |
| persistenceとCustomer migration Ownership | `PENDING` | `PENDING` |
| module外へ公開する境界 | `PENDING` | `PENDING` |
| ReferenceからcopyしないDTO / Entity / migration / fixture | `PENDING` | `PENDING` |
| Architecture / application test方針 | `PENDING` | `PENDING` |

### 5.4 Repository topologyとartifact受渡し

次の表はR1〜R3を比較するために使います。R4のRoot Reactor混在とR5のFramework copy / forkは通常案として選びません。

| 候補 | 評価 | 理由／必要条件／Owner |
|---|---|---|
| R1 Customer別Repository + managed Maven repository | `PENDING` | `PENDING` |
| R2 別Repository + local isolated Maven stage | `PENDING` | `PENDING` |
| R3 integration workspace / orchestration Repository | `PENDING` | `PENDING` |
| 採用候補 | `PENDING` | `PENDING` |

R2を実行する場合だけ、次を記録します。full manifestや絶対pathはFramework Repositoryへ貼り付けません。

| R2 Evidence | 記入欄 |
|---|---|
| Owner approval ID | `PENDING` |
| Framework commit | `PENDING` |
| Tooling SHA-256 | `PENDING` |
| Manifest schema / contract version | `PENDING` |
| Manifest SHA-256 | `PENDING` |
| Formal inventory / payload count | `PENDING` |
| KOIKI dependency / Architecture Rules結果 | `PENDING` |
| Customer verify前後のpayload不変 | `PENDING` |
| Tooling所有stage cleanup | `PENDING` |
| Session resource cleanup | `PENDING` |

## 6. Phase 4責任分担worksheet

この表は初期案をそのまま承認するためのものではありません。実案件で必要か、誰が実装し、誰がacceptance Evidenceを持つかを
明らかにします。不要な領域は`NOT APPLICABLE`と理由を記録します。

| Phase 4領域 | 採用trigger／必要性 | Framework責任 | Customer責任 | Joint Evidence | Owner／次Gate |
|---|---|---|---|---|---|
| Notification / Level 2 | `PENDING` | `PENDING` | `PENDING` | `PENDING` | `PENDING` |
| Accounting / MyBatis | `PENDING` | `PENDING` | `PENDING` | `PENDING` | `PENDING` |
| MVC / SPA coexistence | `PENDING` | `PENDING` | `PENDING` | `PENDING` | `PENDING` |
| External API resilience | `PENDING` | `PENDING` | `PENDING` | `PENDING` | `PENDING` |
| Batch / file / object storage | `PENDING` | `PENDING` | `PENDING` | `PENDING` | `PENDING` |
| Observability | `PENDING` | `PENDING` | `PENDING` | `PENDING` | `PENDING` |
| Container / ECS / VT | `PENDING` | `PENDING` | `PENDING` | `PENDING` | `PENDING` |

## 7. Finding記録

findingは個人の理解不足として扱わず、再現可能な事実と改善Ownerを記録します。1件のfindingへ複数分類を混在させず、
Framework変更が必要か不明な場合は実装を始めずArchitecture reviewへ戻します。

| Finding ID | 分類 | 非機密な事実／影響 | Priority | Owner | 次の対応／Gate |
|---|---|---|---|---|---|
| `PENDING` | F1〜F6 | `PENDING` | `PENDING` | `PENDING` | `PENDING` |

分類は次の意味で使用します。

- F1: Framework defect。P4-AR blocking候補です。
- F2: Developer-experience gap。文書またはTooling改善候補です。
- F3: Environment-specific issue。Customer環境または組織Securityへ分離します。
- F4: Customer requirement。Customer backlogへ置きます。
- F5: Phase 4 feature。見直し後Phase 4計画へ入力します。
- F6: Optional Gate。個別Gate承認前には開始しません。

## 8. Cleanupと非露出確認

R2 Toolingのcleanup PASSはTooling所有stage rootだけを対象とします。次のsession resourceは実チーム側でも確認します。

| 確認項目 | Evidence | 結果 |
|---|---|---|
| application / helper process | 開始前との差分と停止確認 | `PENDING` |
| listener / port | 使用予定portの解放確認 | `PENDING` |
| container / volume / network | 本session所有resourceの残存0 | `PENDING` |
| 一時credential / environment variable | 無効化または除去を確認。値は記録しない | `PENDING` |
| isolated stage root | marker確認後に削除、stage residual 0 | `PENDING` |
| temporary log / manifest | retentionまたは安全な破棄 | `PENDING` |
| Customer source | 意図しない変更0 | `PENDING` |
| secret / personal / business data | Framework Evidenceへの混入0 | `PENDING` |

cleanup不能、Customer sourceの意図しない変更、またはsecret非露出違反はblockingです。対象を推測して削除せず、Ownerへ戻します。

## 9. 実チーム受入結果

次のチェックは、Framework側rehearsalから完了状態を引き継ぎません。実チームが確認した結果だけを記録します。

- [ ] Handoff Guideを単一入口として利用し、迷った箇所を記録しました。
- [ ] ReferenceまたはConsumerのbuild / run / representative operation / diagnosis / cleanupを確認しました。
- [ ] R1〜R3を評価し、Customer業務codeをFramework Repositoryへ混在させていません。
- [ ] Framework artifact、Reference、検証専用Toolingおよび非提供物を区別できました。
- [ ] Customer-owned moduleの配置、Tier、責務、persistence、公開境界を説明できました。
- [ ] ReferenceのDTO、Entity、migration、fixtureをcopyしない理由を説明できました。
- [ ] 不足機能をStandard use / Approved extension / Customer isolation / Framework gapへ分類できました。
- [ ] Security既定を弱めず、Frameworkへ戻す条件を説明できました。
- [ ] Phase 4成果物のFramework / Customer / joint Evidence責任を整理しました。
- [ ] findingへF1〜F6、priority、Ownerおよび次Gateを割り当てました。
- [ ] Customer機密情報を記録せず、session resourceをcleanupしました。

## 10. Close review入力

| 判断項目 | 記入欄 |
|---|---|
| 実チーム受入結果 | `PENDING` |
| F1 blocking件数 | `PENDING` |
| F2〜F6のOwner／priority／次Gate | `PENDING` |
| AR-D10責任分担案 | `PENDING` |
| P4-AR7へ送る項目 | `PENDING` |
| Gate P4-ARへ進める条件 | `PENDING` |
| Architecture Owner判断 | `PENDING` |

このworksheetの完了だけでは、P4-AR6、Gate P4-ARまたはPhase 4開始を自動的に承認しません。非機密summaryを
P4-AR6 Evidenceへ反映し、Architecture OwnerがAR-D10と後続Gateへの進行可否を判断します。

## 11. Worksheet承認記録

2026-09-18、Architecture Ownerは、本書がHandoff Guideを単一入口とし、実行前承認、環境確認、受入journey、
Customer-owned module、Repository topology、artifact受渡し、Phase 4責任分担、finding、cleanupおよびclose review入力を
一貫して記録できることを確認し、P4-AR6実チーム受入の実施様式として承認しました。

Customer機密情報をFramework Repositoryへ持ち込まず、Framework側rehearsalを実チーム受入の代替とせず、検査不能または
cleanup不能をPASSにしない境界を維持します。本承認は、実Customer Repository操作、個別実行command、正式artifact配布、
P4-AR6完了、Gate P4-AR acceptanceまたはPhase 4開始を承認するものではありません。
