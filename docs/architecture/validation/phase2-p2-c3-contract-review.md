# Phase 2 P2-C3 Developer Journey / DoD closeout contract review

## 1. Status and scope

- 作成日: 2026年9月12日
- 作業パッケージ: `P2-C3 / C3-1`
- baseline commit: `cf6537955590c72e0864da3f9a7a6a5c021c8578`
- baseline branch: `feature/phase2-p2-c1-postgresql-migration`
- Architecture Owner承認日: 2026年9月12日
- status: `CONTRACT / INVENTORY REVIEW COMPLETE / ARCHITECTURE OWNER APPROVED`
- Ownership: Architecture Evidence / Tooling
- production artifact / Framework Public API / production migration change: 0

P2-C3は新しいFramework機能を作る工程ではない。承認済みPhase 2成果物を利用者視点で再現し、DoD 2-1〜2-10、
Ownership、ADR / Skill、release unit、remote Evidenceおよびdeferred scopeを最終的にtraceするcloseout工程である。
機構が動作することだけでなく、エンジニアが入口を発見し、必要な構成を選び、Framework / Customer境界を守って業務アプリを
実装・検証し、失敗を診断してcleanupできることを同格のacceptance対象とする。

C3-1のOwner承認前にproduction code、Public API、migration、workflow、required check、ruleset、environment、secret、
snapshot、push、PRまたはmainを変更しない。

## 2. Authoritative baseline and current facts

| Subject | Current fact | P2-C3 treatment |
|---|---|---|
| P2-C1 | `COMPLETE / ARCHITECTURE OWNER APPROVED` | PostgreSQL migration / table ownershipの正本として再利用 |
| P2-C2 | `COMPLETE / ARCHITECTURE OWNER APPROVED` | package / Consumer / API / OpenRewrite境界の正本として再利用 |
| Root Reactor | Referenceを含む15 projects / 104 tests | repository全体回帰単位 |
| formal release unit | 14 projects（POM 3 / JAR 11） | 増減させない |
| publish candidate | Root aggregatorを除く13 coordinates | remote publishは別Owner判断 |
| Public API | 11 JAR / aggregate 24型 | Phase 1a固定baselineとPhase 2初回候補を分離 |
| Framework migration | 3 SQL / 11 business-security tables | Customer / third-party / history境界を維持 |
| Reference | Root参加、formal release / BOM / publish非収載 | DoD 2-10の正式利用例として扱う |
| Tooling | Consumer、fixture、OpenRewrite、publish / verification script | 非配布を維持 |
| remote CI | Gate A / Gate B required checkの承認済み実績あり | final P2-C3 sourceのremote実績とは区別 |
| Phase 2 snapshot | workflow実装 / local dry runまで承認済み | remoteは`NOT APPROVED / NO-GO` |

現在のArchitecture validation indexはPhase 1bまでを掲載し、Phase 2 sectionがない。Security verification READMEは
C2-5 local dry runまでを掲載し、C2-6 / C2-7 / P2-C3の利用者向け経路が未掲載である。これらはP2-C3で閉じる文書gapであり、
新しい設計規則またはproduction機能の不足ではない。

## 3. DoD 2-1〜2-10 trace candidate

| DoD | Primary Evidence | Final journey observation |
|---:|---|---|
| 2-1 | `phase2-p2-a2-t2-verification.md` | 未認証URLとUse Case direct invocationの拒否 |
| 2-2 | `phase2-p2-a2-t2-verification.md` | UI回避requestとMethod Security迂回の拒否 |
| 2-3 | `phase2-p2-a3-t3-verification.md` | local loginとOIDC authorization-codeの共存 |
| 2-4 | `phase2-p2-a3-t3-verification.md` | Bearer JWTの署名 / issuer / audience / time / scopeと401 / 403 |
| 2-5 | `phase2-p2-b3-b3-6-closeout.md` | package済み2 process間Session共有と片系停止後継続 |
| 2-6 | `phase2-p2-b2-b2-5-verification.md` / Gate B | 失敗閾値lockと独立Security Audit |
| 2-7 | `phase2-p2-b1-t4-verification.md` / Gate B | 業務rollbackとBusiness Audit rollback |
| 2-8 | `phase2-p2-b3-b3-6-closeout.md` | non-web cleanup、single execution、競合 / crash recovery |
| 2-9 | `phase2-p2-a2-t2-verification.md` / Gate A | CSRF / Security Header既定と明示override |
| 2-10 | `phase2-p2-b4-b4-5-closeout.md` / Gate B | package済みReferenceからFramework contract経由の管理操作 |

旧DoD 2-11 / 2-12はPhase 2から除外済みであり、別要件へ番号を再利用しない。Oracleはoptional `P4-ORACLE` Gateまで
未実装・未選定を維持する。

## 4. Developer Journey contract

P2-C3では新しい「全部入り」Customer fixtureを作らず、既存の承認済み経路を同一clean HEADで次の順に合成する。

```text
formal release manifestを検査する
  -> 空の隔離Maven repositoryへ14 projectsをstageする
    -> Root外Security ConsumerをBOM / Parent / Starterからbuildする
      -> package済み同一JARをJava 21 / 25とPostgreSQLで起動する
        -> authentication / authorization / audit / identity / session / migrationを観測する
          -> package済みReferenceでDoD 2-10を観測する
            -> non-web Session cleanup / single executionを観測する
              -> Public API / signature / publish dry run / compatibilityを確認する
                -> process / container / repository / fixture targetをcleanupする
```

Gate A Consumer、C2 PostgreSQL Consumer、Gate B Referenceには異なる責務がある。P2-C3はこれらを単一fixtureへcopyせず、
既存scriptを正本として合成する。通常local Maven cache、Framework source pathまたはRoot Reactor classpathへ依存して
Consumerを成功させない。

### 4.1 Engineer-facing acceptance

Developer Journeyは、内部実装を知るFramework開発者だけがscriptを実行できる状態では完了としない。C3-2 / C3-3では
次を利用者接点としてreviewし、観測結果、迷いやすい点および手戻りをEvidenceへ記録する。

| Surface | Acceptance |
|---|---|
| entry / discoverability | Repositoryの主要READMEからPhase 2の導入・検証・Reference・制約へ辿れる |
| dependency selection | BOM / Parent / StarterとApplication所有dependencyの選択理由が分かり、不要なStarterを要求しない |
| profile selection | local Session、OIDC Client、Bearer Resource Serverの適用範囲と併存境界を判断できる |
| ownership | Framework migration / tableとCustomer route / policy / migration / credentialの置き場所を判断できる |
| implementation seam | Framework internal型やfixture APIを使わず、公開契約だけでIdentity / Audit / Sessionを利用できる |
| secure defaults | default deny、CSRF、Security Header、Session initializer禁止が導入直後から成立する |
| diagnostics | 必須設定の欠落・不正値・migration失敗・認証認可失敗で、secretを露出せず原因と修正箇所を特定できる |
| verification | 正常系だけでなく拒否、rollback、再起動no-op、2 process、cleanupを再現できる |
| runtime / packaging | 隔離repositoryのpackage済み同一JARをJava 21 / 25とPostgreSQLで検証できる |
| limits / next action | Referenceとfixtureをtemplateと誤認せず、Phase 3以降・Phase 5・optional Gateの留保を把握できる |

### 4.2 Human-operable journey evidence

C3-2では、機械検査scriptの一覧とは別に、エンジニアが上から順に読める最短のjourneyをREADMEへ用意する。

1. 前提環境と開始地点を確認する。
2. BOM / Parent / StarterおよびApplication所有dependencyを選ぶ。
3. Customer-owned Security chain、route、policy、migrationを配置する。
4. package / runし、保護route、認証、認可、CSRF / Header、DB / Session / Auditを確認する。
5. 代表的な設定失敗・認証認可拒否・migration失敗を再現し、期待する診断先を確認する。
6. test / aggregateを実行し、process、container、一時repositoryをcleanupする。
7. Reference、Consumer、Tooling、正式artifactおよびdeferred範囲を確認する。

C3-3では、この文書経路をclean HEADと隔離repositoryから再実行する。実行command、前提、期待観測点、所要時間、発生した
手戻りと修正先を記録する。所要時間は改善材料として記録し、現段階でSLAやrequired閾値にはしない。

### 4.3 Friction triage

human-operable journeyで見つかった問題を、文書だけで覆い隠さない。

| Finding | Treatment |
|---|---|
| 入口、用語、手順、期待結果の不足 | C3-2の文書差分で修正する |
| 重複実行、cleanup、Evidence採取のTooling不足 | production挙動を変えない最小ToolingとしてC3-2で提案する |
| Public API、設定契約、既定値、診断messageまたはruntime挙動の欠陥 | C3を停止し、影響、互換性、別corrective sliceをOwner reviewへ戻す |
| Project Template、code generator、正式migration / upgrade guideへの要望 | Phase 5へtraceし、C3へ先行実装しない |

READMEに回避手順を書けばよいとは判定せず、利用者がFramework内部型、source path、fixture専用設定または秘密情報へ依存する
必要が生じた場合はproduct boundaryの不成立として扱う。

## 5. Documentation, ADR and Skill contract

P2-C3で予定する文書変更は次に限定する。

1. Repositoryの主要READMEからPhase 2 Developer Journeyへの入口を明確にする。
2. Architecture READMEへPhase 2 validation indexを追加する。
3. Security Consumer / verification READMEへ依存選択、構成、期待観測点、失敗時診断、cleanup、C2-6 / C2-7とP2-C3を追加する。
4. Reference READMEで正式利用例と、Customer Templateではない境界を明確にする。
5. P2-C3 closeout EvidenceへEngineer-facing acceptance、DoD、release / Public API / migration、remote、deferredの最終台帳を作る。
6. Phase 2実行計画へC3 / Gate Cの実績状態を順に記録する。

ADR-046〜048と既存ADRが実装判断を覆っているため、P2-C3の手続きだけを理由にADRを追加しない。新しい規約を導入しない限り、
`koiki-project-overview`および`koiki-business-feature-work`の正本も変更しない。実装証拠が規則の欠落を示した場合だけ、
変更理由と適用範囲をOwnerへ再提示する。

## 6. Local verification and evidence identity

local closeoutは、C3文書 / Toolingをcommitした後のcleanな同一HEADで行う。既存scriptを変更せず合成できる場合は新しい
assertionを複製しない。最終command setは少なくとも次を覆う。

- Gate A authentication / authorization aggregate
- P2-C2 package / Consumer / Public API / local publish dry run / OpenRewrite
- P2-C1 static / PostgreSQL migration
- Gate B 6工程3ラウンド、Root Reactor、Null Safety
- sensitive-output、process / container / temporary repository / directory / fixture target cleanup
- formal 14 / Root 15 / publish 13 / JAR 11 / Public API 24 / migration 3 / table 11 inventory

C2-7の成功はP2-C3 command設計の根拠として再利用するが、P2-C3最終状態を未検証の文書だけで完了扱いしない。
aggregate wrapperを追加する場合も`build-support`配下の非配布Toolingとし、production artifactへ含めない。

## 7. Remote and Gate C boundary

P2-C3 local closeoutとGate C remote操作を分離する。

1. C3 local EvidenceとOwner承認まではpush、PR、ruleset、environment、dispatch、publishを行わない。
2. Gate C開始時に、final source commit、PR required checks、merge、main CIの順序を個別に承認する。
3. Gate A / Gate Bの過去remote成功を回帰実績として保持するが、final P2-C3 commitのremote成功とは表現しない。
4. Phase 2 snapshotはC2-5承認条件を維持し、protected environmentとfinal main commitに対する別Owner承認後だけ一度実行する。
5. snapshot publishをPhase 2最終完了の必須条件とするか、post-closeoutの内部baseline作成とするかはGate C開始前にOwnerが判断する。
6. remote publishを実施する場合は、同一workflow run / source commit、座標別resolved snapshot value、24 payload SHA-256、
   aggregate signatureおよび11 same-source `japicmp`をEvidenceにする。

## 8. Proposed work breakdown

| Slice | Scope | Commit point |
|---|---|---|
| C3-1 | contract / DoD / gap / remote boundary inventory | Owner承認済み契約とC2-7承認記録 |
| C3-2 | Engineer-facing Developer Journey / documentation / index | README、導入・診断経路、DoD ledger、必要最小限のaggregate接続 |
| C3-3 | clean-HEAD local aggregate / cleanup | local EvidenceとOwner review |
| Gate C-1 | final remote plan | push / PR / required check / main / snapshotの個別承認 |
| Gate C-2 | PR / main evidence | approved remote操作を順に実行して記録 |
| Gate C-3 | Phase 2 final decision | DoD、remote、deferredをOwnerが最終判定 |

P2-C3のlocal完了状態は`COMPLETE / LOCAL VERIFIED / GATE C READY`とし、Gate C remote Evidence前に
`PHASE 2 COMPLETE`または`ACCEPTED`と記載しない。

## 9. Architecture Owner decisions requested

1. P2-C3を新機能追加なしのDeveloper Journey / DoD / governance closeoutとし、production artifact、Public API、migrationを増減させないか。
2. 機構の成立と同格で§4.1のEngineer-facing acceptanceを評価し、発見・選択・実装・診断・検証・cleanupまでを完了条件とし、
   §4.3に従って文書gapとproduct defectを分けるか。
3. DoD 2-1〜2-10を§3のEvidenceへtraceし、旧DoD 2-11 / 2-12を除外済みのまま維持するか。
4. Developer Journeyを既存Consumer / Reference / verification scriptの合成で成立させ、新しい全部入りfixtureを作らないか。
5. 機械検査とは別に§4.2のhuman-operable journeyを整備・再実行し、command、期待観測点、所要時間、迷いや手戻りをEvidence化するか。
6. Repository / Architecture / Consumer / verification / Reference README、closeout Evidenceおよび実行計画をC3文書対象とし、規則変更なしにADR / Skillを増やさないか。
7. C3-2 commit後のclean HEADで§6のlocal aggregate、sensitive-outputおよびcleanupを実行し、C3-3 Owner reviewへ提示するか。
8. P2-C3 local closeoutとGate C remote操作を分離し、local承認だけではpush、PR、main、ruleset、environment、dispatchまたはpublishを許可しないか。
9. snapshot publishのPhase 2最終完了条件上の位置づけをGate C-1で明示判断し、それまではC2-5 remote publishを`NO-GO`に維持するか。
10. P2-C3 local完了後もGate C Evidence前はPhase 2を完了扱いせず、最終Owner判定をGate C-3に残すか。

推奨結論は上記10点を承認し、C3-2のEngineer-facing文書・Developer Journey接続へ進むことである。remote操作は
本承認に含めない。

### 9.1 Partial approval record

2026年9月12日、Architecture Ownerは§4.1 `Engineer-facing acceptance`の内容を確認し、これを承認した。
機構の成立だけでなく、entry / discoverability、dependency / profile selection、Ownership、公開契約、secure defaults、
diagnostics、verification、runtime / packagingおよびlimits / next actionをP2-C3のacceptance対象とする。

この承認は§4.1に限定する。§4.2 / §4.3を含む§9の10判断全体、C3-2開始、production / workflow変更およびremote操作は
まだ承認していない。C3-1は`CONTRACT / INVENTORY REVIEW PENDING`を維持する。

同日、Architecture Ownerは§4.1の部分承認を前提として、§9の10判断を提案どおり承認した。P2-C3を新機能追加なしの
Developer Journey / DoD / governance closeoutとし、Engineer-facing acceptance、human-operable journey、friction triage、
DoD 2-1〜2-10 trace、文書対象およびclean-HEAD local aggregateを承認する。C3-1を
`COMPLETE / ARCHITECTURE OWNER APPROVED`とし、C3-2のEngineer-facing文書・Developer Journey接続へ進めてよい。

production artifact、Framework Public API、production migration、workflow、required check、ruleset、environment、secret、
snapshot、push、PR、mainおよびその他のremote操作は本承認に含めない。C2-5 remote snapshot publishは引き続き
`NOT APPROVED / NO-GO`とする。
