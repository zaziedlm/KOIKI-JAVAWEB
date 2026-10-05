# 作業7：S1担当・検証環境・作業量／上限・実施順案（2026-10-05）

**状態:** DRAFT / 作業7の実施可能性整理。担当者の指名、予算・上限、環境確保、実装・実演開始、Gateは未承認。
**Ownership:** Framework側の計画資料。Framework / Reference / Toolingの成果物と、運用担当者の総合判断を分けて追跡する。
**確認baseline:** `docs/daily-development-workflow` / `781a6a2`。作業7の先行文書とREADME・作業一覧は未commit差分。今回は文書・既存fixture sourceの読み取り照合であり、環境稼働や新しい実行結果は確認していない。
**入力:** [S1範囲照合](phase4-s1-a1-a2-d1-scope-mapping-20261005.md)、[安全条件](phase4-s1-safety-and-recovery-conditions-20261005.md)、[DoD実演計画](phase4-s1-dod-demonstration-plan-20261005.md)、[既存F-4見積境界](KOIKI-JavaWeb-FW_Phase4_PL2_P4-F判定資料_v0.1.md#3-f-4-暫定工数とcommit-point)、[Tooling README](../../build-support/phase4-level2-verification/README.md)。

## 1. 今回の具体化と未取得事項

責務別の担当割当案、ローカル隔離検証の構成、残作業10単位、標準作業時間による低確度概算とreviewへ戻る上限案、実施順・停止点を示す。Ownerが了解した21 caseは出発点とし、実装・検証で不足を確認して見直す。

担当者名・稼働可能時間・代行者、利用端末の資源・Docker稼働・権限・費用、運用閾値、実施予算と期限はOPENである。以下の数値はAgentが今回の残作業から提示する概算案であり、実測やOwner見積・予算承認ではない。

## 2. 担当と責務の割当案

**実際の陣容・Owner合意（2026-10-05）:** 本節の役割整理はOwner合意済み。開発はOwner一人とCodex AIコーディングエージェントの協働で進め、本計画も同じ陣容で行う。表は責務の区分であり、別担当者の配置を前提にしない。Ownerが確認・判断・運用者 / 判定者の責務を兼務し、Codexが設計・実装・検証・文書作成を承認範囲で支援する。現時点ではサブエージェントを使わず、タスクを順次・同期的に進め、Ownerが理解を保つ方針を利点として採用する。表中の担当者OPENは別人の募集・配置待ちではなく、各作業時の分担・稼働・権限等の具体化を示す。実装・実演開始や数値上限の承認境界は維持する。

| 役割 | 担当する成果物・判断 | 現在の割当と取得する入力 |
|---|---|---|
| Architecture Owner | CP-F0残項目、S1対象・開始経路、DoD解釈、blocking review、上限と範囲変更、結果・不足caseの継続確認 | Ownerの継続注視方針を記録済み。review枠・確認時点・委任先はOPEN |
| Framework設計・規約担当 | Spring標準利用、二階層migration整合、Level選択・Rule互換性、既存Starterの適用境界 | 役割候補。担当者・稼働OPEN。Agentは承認済み範囲の調査・案作成を支援 |
| Reference実装担当 | publication Application構成、expense event、notification・通知log・provider Adapter、復旧機能の実装配置 | 役割候補。担当者・実装開始はOPEN。Frameworkへの昇格は別review |
| Tooling・環境担当 | 実process制御、stub / sink、故障注入、証拠採取、実演手順、環境台帳・cleanup | 役割候補。担当者・端末と利用権限OPEN。既存fixtureの直接配布・昇格はしない |
| Security / Audit review担当 | 非HTTP復旧主体、認可、操作Audit、失敗時の扱い、PII非露出 | 役割候補。担当者・review枠OPEN。必要な権限は個別に確認 |
| 復旧運用者 | DB・process・provider・Auditを総合確認し、再送・保留・調査・連絡を判断。理由と結果を記録 | 実演では人がこの役割を担う。担当者・代行・時間帯・連絡先はOPEN。Agentの自動実行だけで人の実演を代替しない |
| Evidence判定者 | caseの結果・証拠・未検証枝、DoD対応と適用条件を照合しOwnerへ提出 | 役割候補。同一人による兼務の可否・必要な別承認はreviewで決める |

役割の兼務は可能性として残し、二者承認を一律には要求しない。実演時には誰が実行し、誰が判断・判定したかを明記する。Agent支援の実装・実行は対象と開始条件が承認された後に位置づける。

## 3. 検証環境の構成案

ローカルWindows / PowerShellを第一候補とし、案件環境・実provider・AWS・Linux / WSLをS1実演の必須入力へ加えない。別環境が必要と判明した場合は差分・費用・担当をreviewする。ローカル環境が確保済みとは扱わない。

| 要素 | 構成・再利用の候補 | 開始前に取得・確認すること |
|---|---|---|
| build / runtime | Repository Maven Wrapper、Java 21 build、固定sourceのReference JAR。既存Tooling POMとREADMEを参照 | 実際のJava / Maven・実効依存、snapshotの整合、JAR checksum。Java 25 / Virtual ThreadsはD2で別分解 |
| Docker / DB | ローカルDockerと使い捨てPostgreSQLを候補。既存fixtureは`postgres:17-alpine`とTestcontainersを使用 | Engine稼働・接続権限、採用imageのdigest / DB version、隔離DB・port・volume、migration履歴。floating tagだけで再現性確定としない |
| process群 | 通常processを複数起動でき、旧・新の復旧processを区別する構成。競合caseは通常2＋復旧2 processを検証上の起点とする | 必要heap / CPU / 接続数の実測、instance・世代・PID・port・lock接続、停止と再起動の制御。これは本番容量見積ではない |
| provider stub | 受理記録と同じ通知key、応答喪失・照会不能・payload不一致を観測できる非配布stub | DB内既存stubの再利用範囲、通信境界を別stubで検証する必要性、key寿命と照会仕様。実provider接続は対象外 |
| 観測sink | log / metric / 実trace / alertを隔離sinkへ保存。人が確認可能な操作・閲覧手段を用意 | 実tracer / exporter・sinkの選定review、接続・容量・retention・非露出。MDC markerだけでは代替不可 |
| 故障注入 | Toolingからprocess停止、lock接続喪失、provider / DB / Audit / sink失敗を制御 | 指定位置到達を独立確認する方法、正式codeを汚さない注入方式、権限と影響範囲。実装不能な枝はBLOCKEDとして設計へ戻す |
| 人の確認環境 | 対象snapshot、終了証拠、provider受理、Audit・観測を閲覧し、判断を記録する導線 | 実演運用者の権限・手順・連絡先・代行。専用管理UI新設を必須にはしない |

過去のPL2 EvidenceにあるDocker / Java / DB versionとPASSは時点記録である。今回の稼働確認へ転記しない。権限エラーはAGENTS.mdの承認済み手順へ照合し、既存PL2 fixtureの許可を新しい正式Reference検証の無条件実行許可に広げない。

### 3.1 環境台帳・cleanupの出口

実演IDごとに所有container / process / DB / volume / port / 出力先を台帳へ登録する。故障前後の時刻、source・artifact・設定、証拠の保管先とchecksumを残す。容量・DB接続・所要時間を最初の試行で測り、環境上限へ反映する。

cleanupは追加再送の停止、全関連processと外部副作用の照合、証拠保存・判定、残る保留対象の引継ぎを経て、台帳上の当該実演資源だけを対象にする。証拠やpublicationを先に消さず、既存開発DB・他containerへ一括削除を適用しない。削除・停止コマンドは実施環境と権限が確定した手順で用意する。環境費用・ディスク容量・保持期間はOPEN。

## 4. 残作業の分解と概算案

単位は**標準作業時間（時間）**。設計・実装・test・証拠整理を通常の技術作業として見積もる低確度の案で、AIの応答時間・実行待ちの壁時計時間ではない。Owner・他担当者のreview参加時間と外部待ち日は別枠OPENとする。作成済みの照合・安全条件・21 case計画を再作成する時間は含めない。

| ID | 残作業・主な成果物 / 主責務 | 概算案（時間） | 根拠・出口と待ち条件 |
|---|---|---:|---|
| W01 | 配置・schema・依存・Ruleの詳細設計 / Framework・Reference | 8〜16 | 現行kkrefとpublication整合、変更module・API差分、実効依存調査、review材料。正式採用判断待ち |
| W02 | 復旧・通知key・確定順・Auditの詳細設計 / A1・A2・Security | 16〜32 | S-01〜09の方式、各停止窓、復旧主体・認可・不明時手順を具体化。人による判断を含む。未解決安全条件は停止 |
| W03 | 隔離環境・台帳・証拠採取・cleanup手順 / Tooling | 8〜16 | 既存fixtureの環境構造を再利用し、Reference実演用構成を用意。資源実測は承認後。端末・権限待ち |
| W04 | A1 publication構成・復旧・パージと規約差分 / Reference・Framework | 24〜48 | 保存整合、選別・競合・上限、Rule互換と二階層migrationの意味ある回帰。blocking review後に実装 |
| W05 | A2承認event・notification・通知log / Reference | 24〜40 | event発行位置、冪等key・provider Adapter、承認・同期veto回帰。A1契約とA2 review後に実装 |
| W06 | 正式Reference実演用stub・故障注入harness / Tooling | 24〜48 | 既存停止・競合の構造を利用し、指定停止位置、応答喪失・key失効・Audit失敗等の不足を補う。fixture全面作り直しは計上しない |
| W07 | D1 metric・実trace・sink・alert / Reference・Tooling | 16〜32 | 既存requestId再利用、FAILED起点と別process相関、実tracer・exporter、非露出。汎用Starter追加は別判断 |
| W08 | 人による復旧訓練準備・判断記録・代行導線 / Tooling・運用 | 8〜16 | 取得・照合・再送 / 保留・連絡・結果確認の手順と閲覧導線。管理UI新設を含めない。担当・権限待ち |
| W09 | E01〜21の実演・個別枝・証拠突合 / Tooling・Reference | 32〜64 | 共通環境・操作をまとめ、DoD別に証拠を採取。各枝の初回実演、通常の再観測・修正確認を含む。新方式への設計変更は範囲外 |
| W10 | Evidence・適用条件・runbook差分・判定資料 / Framework・Tooling | 16〜32 | case結果、未達・不足case・保証範囲、運用判断記録、Owner向けDoD判定材料。実案件運用設計は別枠 |
| 合計 | S1限定範囲の残技術作業 | **176〜344** | 未承認の低確度概算。担当者稼働・review待ちを含むカレンダー納期ではない |

この数値は今回の10作業単位の規模と未知要素に基づくAgentの提案であり、既存47〜80標準人日の換算・値引きではない。8時間を単なる比較単位とすれば22〜43人日相当だが、1日の実稼働や納期を定める値ではない。詳細設計・故障注入方式・担当の確認後に再見積する。実測根拠がないため、現在の数値だけでP4-F提出条件が成立したとは扱わない。

21 caseは各case内に複数枝を持ち、21回の実行ではない。W09は共通setup・障害制御を再利用する前提であり、実行行数・再現待ち時間はW06で枝別に確定する。再現不能や追加caseで範囲が増える場合は見積を更新する。

### 4.1 二重計上と範囲追加の扱い

- W01 / W02は詳細設計・review資料の作成、W04 / W05 / W07は承認後の実装・局所回帰、W09はpackage済み統合実演、W10は最終証拠・判定整理へ分ける。
- Rule正式差分と負例はW04へ一度だけ計上する。別のPhase共通費へ重ねない。
- 人の実演参加時間・Owner / Security review参加時間は未取得であり、上表に含めたと推定しない。W08は手順準備、W09は技術実演作業の見積である。
- S2共通Starter / API、別DB方言、本番provider、C3 / C4全体、CI変更・正式配布・remote、管理UI新設、専用性能benchmarkは別範囲。必要性が見えた時点で差分・担当・追加Evidence・費用を示す。

## 5. 作業上限と見直し条件の案

| 管理対象 | 上限・確認点の提案 | 現在の承認状態 |
|---|---|---|
| 残詳細設計 W01＋W02 | 合計24〜48時間を概算の起点とし、方式が揃った段階で残りを再見積。48時間で未解決なら追加着手前に範囲・残課題をreview | 未承認の確認点案。現在の計画作業継続指示を予算承認へ変換しない |
| 各W単位 | 表の上側をreviewへ戻る閾値候補とする。超過見込みが出た時点で実測・残作業・理由・追加見積を提出 | 未承認。安全条件・caseを省略して閾値へ合わせない |
| 技術作業総量 | 344時間を現案の再判断閾値候補とし、全体上限を採るか、段階上限を採るかOwnerが判断 | 未承認。契約上限・完了保証・AI実行予算ではない |
| review / 人の実演稼働 | 各review回と実演参加の必要時間を技術作業から分離して取得 | 時間・担当・代行OPEN |
| process / DB / 保存容量・環境費 | 最初の承認済み試行で実測し、heap / 接続 / ディスク・費用・保持の上限を決める | 数値OPEN。環境の無制限利用は想定しない |
| 再送回数・timeout・復旧時間 | S-09に沿って設定と解除権限・連絡をreview。未取得のまま実演の成功基準を固定しない | 数値OPEN。fixtureの回数・秒数を採用しない |

上限案は見直しのための停止点であり、未承認の追加作業を自動許可するものではない。安全方式が不成立、証拠不足、担当 / 権限未確保なら、時間を消費して進める前に当該対象を保留する。

## 6. 実施順と段階出口

以下は候補順序であり、既存CP-F1〜5・Gateを改訂しない。設計の並行と実装開始の条件を分ける。

| 段階 | 対象・実施順 | 出口・次段階への条件 |
|---|---|---|
| P0 計画材料 | W01 / W02の設計案、W03 / W06の方式検討、W07 / W08の構成・役割案を並行整理 | 担当・環境・概算 / 上限・未決事項を揃え、他package分解と作業4b・5に統合してORへ。新規fixture実装や実環境操作はこの表だけでは開始しない |
| P1 採用・開始経路の判断 | OR、必要な作業8・9、CP-F0残項目、P4-F設置 / 限定開始または現行Gate経路の判断 | 対象package・担当・上限・停止点・開始点が個別に承認される。今回まだ未実施 |
| P2 blocking review | W01 / W02、A1 schema・依存・Rules・復旧安全性、A2 event / provider境界、D1起点・相関をreview | CP-F1等の条件を満たす。未成立の方式は再設計。P1 / P2の具体的な開催順は既存Gate規定と正本改訂で整合 |
| P3 A1・観測・harness | 承認範囲でW03 / W04 / W06、W07 / W08を進める。A2詳細設計は並行可能 | CP-F2の保存・再送安全条件とTooling証拠。D1は観測できる状態へ。これが成立するまでA2実装を先行しない |
| P4 A2・初期実演 | A1契約とA2 review後にW05。固定ReferenceをpackageしE01〜03、E15 / E16の送信前拒否、E20の初回観測を確認 | 承認・Audit・同期veto、通常通知と非rollback、権限外送信なし。FAILなら停止復旧caseへ進めず原因修正 |
| P5 正常復旧と冪等境界 | W09でE04〜10、E15の再送とE20の別process追跡。W08で人の判断・保留を実演 | 元実行停止と安定key、配信完遂・受理一意・結果整合、判断記録。手動復旧のDoD解釈と時間条件を適用 |
| P6 競合・運用負例 | E11〜14、E16の送信後Audit失敗、E17、E21。枝別に隔離し証拠採取 | 通常listener・複数復旧者・lock喪失窓の安全性、人の保留・調査・連絡・再開。未解決の競合はBLOCKED / FAILとして保持 |
| P7 保持・パージ | E18 / E19。A1とC3実行基盤の関係をreview済みの範囲で確認 | 保持期限・単一実行、未処理保全、競合時の整合。C3全体のDoD 4-10達成とは別判定 |
| P8 統合確認 | W09のcase結果・不足枝を再確認しW10へ。CP-F3 / F4の結果とCP-F5統合判定へ対応付け | DoD別証拠、適用条件、未達・残リスク、運用判断をOwner確認へ。S1完了・正式受渡し・Phase 4全体完了は各判断で区別 |

実演は正常・安全な拒否を先に確かめ、危険な停止・競合はその後に隔離して行う。人の判断結果と技術Evidenceを各段階でOwnerへ提示し、最後の一括reviewだけに集中させない。caseを完全網羅と宣言せず、追加・省略・未検証を記録する。

## 7. 現時点で必要な確定入力

| 入力 | 提案材料と残る判断 |
|---|---|
| 担当・稼働 | §2の役割を誰が兼務 / 分担するか、実施・review・復旧者と代行の稼働を取得 |
| 環境 | §3のローカル構成の利用可否、端末資源・権限・費用・cleanup責任、停止証拠と観測sinkの実施可能性 |
| 作業量・上限 | §4の低確度概算を詳細設計で更新し、§5の段階 / 総量上限の採否を判断。review稼働と外部待ちは別取得 |
| 復旧条件 | 停止確認方式、通知keyとprovider stub、非HTTP権限 / Audit、保持とパージ、復旧時間・回数 / timeoutをreview |
| 開始経路 | 他package分解と4b・5のOR材料、作業8・9・Gate整合、対象commit pointの個別判断。現在は計画・文書整理の範囲 |

今回、S1の実施可能性を判断する材料を作成した。担当・資源・上限の確保を完了扱いにせず、Gate P4-Fの提出条件は未成立として保持する。次は他packageを同じ様式で分解し、Framework先行対象・優先順・Gate差分と合わせてOR材料へ統合する。S1のOPEN入力はその間も並行管理する。
