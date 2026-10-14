# Phase 4 PL2 — F-5全件台帳・Gate改訂差分案

**状態:** REVIEW INPUT / 2026-09-27。P4-F Gate設置、DoD変更、Phase 4 production開始を承認した文書ではない。

**入力:** [Phase 4見直し草案§3〜4](KOIKI-JavaWeb-FW_Phase4実施計画_見直し草案_v0.1.md#3-当初phase-4案件の棚卸し)、
[P4-F判定資料](KOIKI-JavaWeb-FW_Phase4_PL2_P4-F判定資料_v0.1.md)、
[Level 1 / Level 2の業務向け説明資料](phase4-pl2-level1-level2-business-guide.md)、
[P4-F提案](KOIKI-JavaWeb-FW_Phase4_P4-F限定開始Gate提案_v0.1.md)、
[P4-AR計画](KOIKI-JavaWeb-FW_Pre-Phase4_Adoption_Readiness計画_v0.1.md)、
[グランドデザイン§27.8](../architecture/grand-design/KOIKI-JavaWeb-FW_グランドデザイン_v0.2.md#278-phase-4-enterprise-integrationv04)。

## 1. Phase 4全件の採否・DoD・待ち条件

「P4-F範囲」は限定Gateの候補を示す。P4-AR6の実チーム受入、AR-D10、Gate P4-ARと
Phase 4全体開始は未完了。以下の採否・延期・DoD変更は未承認である。

| 当初項目 / work package | 当初DoD・成果物 | P4-Fとの関係 | 次の判断と待ち条件 |
|---|---|---|---|
| P4-01 / A1・A2 | Level 2、`notification`、4-1〜4-5 | S1 / S2なら候補。S0なら開始しない | CP-F0で採用規模・対象event・復旧許容時間・運用Ownerを選ぶ。S0時はDoD延期・変更をOwnerへ提示 |
| P4-02 / C0・C1 | `accounting`、MyBatis分離、4-6。`ExpenseSettled`の非同期受信も当初Reference構成 | P4-F対象外。C1は当初A1にも依存 | C0 adoption triggerを先に判定。S0なら非同期連携の範囲とC1順序を再計画し、Rule 8拒否を維持 |
| P4-03S / B1 | Reference Session SPAとMVC併用、4-8・4-9 | P4-F対象外 | 当初DoDを維持するかOwner判断。実案件BFFの成果では代替しない |
| P4-03B / B2 | 実案件Next.js/BFF＋KOIKI REST接続。独立した当初DoD番号なし | P4-F対象外、Customer主導 | API・認証・非機密Evidence・Framework gapの責任をP4-AR6 / AR-D10で決める |
| P4-04 / X | SAML Extension。当初成果物、DoD番号なし | P4-F対象外 | 外部IdPの方式は見込み。OIDC / broker / KOIKI直接SAMLの要否を確認し、採否変更はOwner判断。認証の終端とKOIKIに届く認証情報で判定する（[見直し草案§8](KOIKI-JavaWeb-FW_Phase4実施計画_見直し草案_v0.1.md#8-新しい入口へ引き継ぐ設計観点-認証の終端とbffkoiki間の検証)） |
| P4-EDGE / X | Edge認証（ALB＋Cognito / 外部OIDC）のcloud固有Adapterと実ALB環境検証。Grand Design §13とPhase 2 test designから送られたが、当初棚卸しに行がなかった。DoD番号なし | P4-F対象外 | 実案件のALB認証採否を確認（PL1-Q6）。採用しない場合も後続Phaseへの割当をOwnerが明示 |
| P4-05 / C2 | External API Resilience、4-7 | P4-F対象外 | 接続先・失敗semantics・冪等性が必要。Resilience4jは別library review |
| P4-06 / C3 | Spring Batch、4-10 | P4-F対象外。単一実行契約はA1パージと比較対象 | Reference job・運用Owner・metadata所有を決める。S0時はpublicationパージ需要がなくなるがBatchの4-10は残る |
| P4-07 / C4 | File / Object Storage、DoD番号なし | P4-F対象外 | Reference代表use caseを選ぶ。実保存先・format・権限はCustomer入力。AWS固有Adapterを先行しない |
| P4-08 / D1 | OpenTelemetryと非同期観測、4-4・4-12の観測面 | S1 / S2の非同期部分だけA1と並行候補 | S0時はLevel 2固有metric / traceを開始せず、当初OpenTelemetry成果物の採否を別途判断。exporter / alert Ownerが必要 |
| P4-09 / D3 | Container・ECS Reference、DoD番号なし | P4-F対象外。package済みReference実演環境はF-3で別途必要 | platform、network、secret、resource Ownerと検証場所を決める。実案件環境の受入と混同しない |
| P4-10 / D2 | Virtual Threads有効化ガイド・CI、4-11 | P4-F対象外 | 既定無効を維持。Java 21 / opt-in runtimeとCI費用・required check変更を別review |
| P4-11 / E1 | 正式受渡し対象、repository、version / support、DoD番号なし | P4-F対象外 | P4-AR6実チーム受入とrelease / platform Owner判断を待つ。R2 stageや内部snapshotを正式releaseとしない |
| P4-OPT / X | KOIKI-hosted Authorization Server、Oracle | P4-F対象外・必須DoD外 | `P4-AS0` / `P4-ORACLE`の明示triggerと個別Gateまで開始しない |

当初DoD 4-1〜4-12は[グランドデザイン§27.8](../architecture/grand-design/KOIKI-JavaWeb-FW_グランドデザイン_v0.2.md#278-phase-4-enterprise-integrationv04)を正本とする。
S0で4-1〜4-5・4-12が未達となる場合や、S1の手動復旧で4-2の意味を変える場合は、
計画の未達・変更をOwnerが個別に記録する。PL2 ToolingのPASSを正式DoDへ転記しない。

## 2. Gate P4-Fの改訂差分案

**2026-10-06の具体化・反映:** 以下は9月27日時点の文案履歴。S1初回をReference保存・認可・Audit基盤に
限定する具体差分は[正本改訂§2・Owner承認§9](phase4-s1-reference-foundation-canonical-delta-and-start-conditions-20261006.md)に基づき
P4-AR計画／AGENTS／見直し草案／P4-F資料へ反映した。全件台帳の対象外・当初DoD・Customer入力待ちは維持する。
Gate設置／限定判定・実行開始は未成立であり、下記履歴の「この文書の作成では改訂しない」と今回の別承認による反映を分ける。

以下はOwnerがS1 / S2を選び、F-4のFramework / Reference / Tooling限定作業の
実施Owner・概算範囲または上限・安全条件が揃った場合に
reviewする**文案**である。現行の[P4-AR計画](KOIKI-JavaWeb-FW_Pre-Phase4_Adoption_Readiness計画_v0.1.md)、
`AGENTS.md`、[Phase 4見直し草案](KOIKI-JavaWeb-FW_Phase4実施計画_見直し草案_v0.1.md)は
この文書の作成では改訂しない。

### 2.1 P4-AR計画へ提案する文案

§2「Phase and governance positioning」に、次を追加する案。

> Architecture OwnerがGate P4-Fの設置と対象commit pointを明示承認した場合に限り、
> P4-AR6 / AR-D10 / Gate P4-ARより前に、指定したFramework共通技術契約とReference実証を
> 限定開始できる。P4-AR6実チーム受入、AR-D10、Gate P4-AR、Phase 4全体開始、
> 正式受渡しとremote変更は別判断のまま維持する。実案件固有のprovider・運用・CI・platform費は
> P4-Fの限定作業見積へ加えず、各Applicationの採用前にそのOwnerが安全条件とともに判断する。

§5「Work packages」へ追加する行の文案。承認時には対象package・期限・実施Ownerを承認記録で特定する。

```markdown
| Gate P4-F（条件付き） | Framework限定開始の別Gate | F-1〜F-5、S1 / S2の限定package、実施Owner・概算範囲または上限・Evidence・期限をOwnerが判定 | 案件固有の導入・運用総額は各Applicationへ分離。対象外のPhase 4、P4-AR6、正式受渡し、remote変更を開始しない |
```

§11「Evidence and commit points」へ追加する行の文案。CP-F0〜CP-F5の各停止条件は
[P4-F判定資料§3.3](KOIKI-JavaWeb-FW_Phase4_PL2_P4-F判定資料_v0.1.md#33-commit-point停止点と戻し方)を参照する。

```markdown
| Gate P4-F / CP-F0〜CP-F5（条件付き） | 承認対象だけの設計・実装・統合実演。各CPのEvidenceはdocs/architecture/validation/へ記録 | push / PR / merge、workflow・ruleset変更、snapshot publishは別承認 |
```

§13「Proposed execution sequence」には次の分岐を追加する案。

> P4-PL2のF-1〜F-5が揃った場合、P4-AR6実チーム入力を待つ間にGate P4-Fの採否を
> Architecture Ownerが別途審査できる。採用時も承認対象commit pointに限る。
> P4-AR6→AR-D10→Gate P4-ARの既存経路は残し、§10のAR-D1〜AR-D10は変更しない。

### 2.2 `AGENTS.md`へ提案する文案

Phase 4開始未承認の記載に続けて、Owner承認時だけ適用する例外を追加する案。

> Gate P4-FがArchitecture Ownerにより設置・承認された場合は、承認記録に列挙された
> commit point、Ownership、Evidenceと停止条件の範囲に限ってPhase 4のFramework共通技術契約・
> Reference実証を開始できる。P4-AR6 / AR-D10 / Gate P4-ARとPhase 4全体開始は
> 完了扱いにしない。Public API、dependency、migration、Starter、Rules、workflow、remote操作、
> snapshot publishは各blocking reviewと個別承認に従う。Customer固有作業と正式受渡しは対象外とする。
> 実案件のprovider契約・当番・SLA・CI / platformと継続費は当該Applicationの採用判断に残す。

この文案を承認前の`AGENTS.md`へ書き込まない。S0選択時はGate例外を設けず、
現行Gate順序を維持しながら当初DoD・依存packageの再計画案をOwnerへ出す。

### 2.3 Phase 4実施計画へ提案する差分

§4.3へ追加する文案。

> P4-Fの開始範囲は、PL2で比較したS0 / S1 / S2の採用規模、F-4の限定作業のOwner・
> 概算範囲または上限・停止条件、
> DoD 4-1〜4-5・4-12の実演条件をOwnerが確認してから確定する。S0ならP4-Fの
> Level 2開始を提案せず、当初DoDとA1に依存するC1・Batch reminderの順序を再計画する。
> S1 / S2でも実案件ごとの導入・運用総額をP4-Fへ含めず、当該Applicationの利用前に
> provider冪等性、監視・復旧担当、許容復旧時間を確認する。P4-B1の4-8 / 4-9と
> Customer主導B2のEvidenceを別々に維持する。

2026-09-26のR1〜R7判断は書き換えず、P4-F採否を新しい判断記録にする。

## 3. Owner reviewへ出す条件と未取得入力

### 3.1 CP-F0では業務上の必要性を先に判定する

[業務向け説明資料](phase4-pl2-level1-level2-business-guide.md)の区分に従い、
「処理をコミット後に分離する必要」と「障害後にも届ける必要」を、Referenceの実証目的と
実案件の要求に分けて確認する。PL2のfixtureが動いたことだけではS1 / S2を選ばない。

| 判断材料 | 現時点で分かっていること | CP-F0で必要な扱い |
|---|---|---|
| 既存の業務成立条件 | 部門廃止の拒否はLevel 1で実証済み。Phase 3の同期処理と業務監査を維持する | Level 2の採否と独立して継続 |
| Referenceの承認通知 | グランドデザインのPhase 4例とDoD 4-1〜4-5・4-12にある。正式Reference実装・実演は未実施 | 当初DoDを今実現する必要性と、延期する場合のDoD変更をOwnerが明示判断 |
| 実案件の通知・連携需要 | 確定入力はNext.js/BFF＋KOIKI RESTのみ。通知先、送信保証、SLA、当番は未取得 | 実案件がLevel 2を使う理由として推定しない。P4-AR6または当該Applicationの採用判断で確認 |
| 復旧と重複の受容性 | Toolingではprovider stubの冪等keyがないと二重送信。複数instanceの通常処理と復旧の競合も残る | S1でも「手動だから安全」としない。Reference実演の停止条件と担当を先に定義 |
| 共通提供の需要 | 複数Consumerへの正式契約を求める実例は未取得 | S2の根拠にしない。必要になった時点で共通化費用とsupportを別review |

**現時点の審査向け提案:** CP-F0ではS0（Level 1維持・Level 2のproduction開始見送り）を
暫定基準とし、当初DoDとA1依存packageの再計画をOwnerへ提示する。これはS0やDoD変更の
決定記録ではない。Referenceの承認通知を今実証すべき理由、対象event、復旧許容時間、
実施Ownerが示されればS1を再評価する。S2は独立利用先の需要とS1との差分が示されるまで
審査対象にしない。S1 / S2へ進む場合も、F-4の限定作業の概算範囲または上限が必要である。

### 3.2 Gateと後続レビューの入力

| 判断 | review可能にする材料 | 現在地 |
|---|---|---|
| CP-F0: S0 / S1 / S2とDoD | 業務上の必要性、Reference対象event、限定作業のOwner・復旧条件、S0時の延期・変更案 | 実案件の確定入力はBFF＋RESTのみ。案件通知需要・SLAは採用時へ分離。暫定基準はS0、未承認 |
| P4-F Gateを設けるか | F-1〜F-5、限定作業の概算範囲または上限・実施Owner、schema / 復旧安全条件、対象commit point・期間・停止点 | 限定作業の上限とOwnerは未確定。案件固有の総額・provider / 運用 / CI費はGate必須入力としない案。現時点は`REWORK`候補 |
| A1 blocking review | store / migration所有、Rule 28 / 29・Public API、停止確認と複数instance再送、provider冪等性 / fencing | Tooling Evidenceは揃ったがproduction契約は未決定 |
| P4-AR6 / AR-D10 | 実チームの受入結果、Framework / Customer / joint Evidence責任、受渡し範囲 | 実チーム入力待ち。P4-Fの採否から自動充足しない |
| 各ApplicationのLevel 2採用 | 実providerの冪等性、通知量・SLA、監視 / 復旧担当と訓練、CI / platform・継続費 | P4-FのReference stub Evidenceだけでは充足しない。利用前に別途停止条件として確認 |

Gate P4-Fが`REWORK`または`REJECT`なら、現行P4-AR順序を維持する。
`APPROVE LIMITED START`の場合も承認対象以外のPhase 4 packageと正式配布は開始しない。
