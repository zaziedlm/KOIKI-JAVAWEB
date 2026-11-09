# KOIKI-JavaWeb-FW Agent Guidance — Phase 3

## 現行入口：D3方式2 S2 Cloud限定技術PASS（2026-11-09）

**S2コミットポイント：** Ownerは「S2 PASSをコミットします」と指示した。S2のTooling実装・結果記録・本入口をlocal commitとして固定する。S3開始・push／PR／merge・D3全体の受入へ拡張しない。以下のcommit未実施・別判断表示はこの後続指示前の履歴。

OwnerのS2開始指示によりS1固定`3e3ec1b`からToolingだけを実装。最終project `d3-sso-s2-20261109-05`でS2-V01〜04がPASS。実Chromium6認証、library標準のID Token JWS／state／nonce／PKCE、canonical issuer、Javaの正常3操作・拒否6条件11要求とDB／Audit不変、helper終了・cleanup・secret／生成realm削除・exportが成立。[S2結果](docs/architecture/validation/phase4-d3-option2-s2-results-20261109.md)を現行入口とする。直接回帰23件・共有guard5件・OIDC境界3件もPASS。初回enum設定／観測SQLの失敗と、観測・署名検証を補強した中間結果を別Evidenceで保持し、OwnerのFramework非改変修正後再実行許可に従った。Framework／Reference本体の変更なし。S3・結果Owner受入・commit／remoteへ自動進行せず、Windows持帰り未確認と既存保持資源の条件を維持する。以下のS2未着手表示は以前の履歴。

## 現行入口：D3方式2 S1 Cloud限定技術PASS（2026-11-09）

**S1コミットポイント：** OwnerはS1 PASSをコミットポイントと認識し、S1のTooling実装・結果記録・本入口のlocal commitを指示した。今回この範囲を固定する。S2開始・push／PR／merge・D3全体の受入へ拡張しない。以下のcommit未実施・別判断表示はこの後続指示前の履歴。

**最新結果：** Owner承認後、新規project `d3-sso-s1-20261109-02`で同じS1-V01〜05を1回再実行し、全件PASS・自動cleanup・生成secret削除・証拠exportが成立した。[S1結果§8](docs/architecture/validation/phase4-d3-option2-s1-results-20261109.md#8-owner承認後の修正済み新規project再実行)を参照。既存JAR／imageをhash照合して再利用し、Framework変更なし。初回project `01`の失敗・cleanup復旧・Evidenceも保持。Windows持帰り／checksum照合は未確認。S2・結果全体受入・commit／remoteへ自動進行しない。以下のHOLD・再実行未実施表示は初回の履歴。

**後続Owner再実行指示：** Ownerは修正後の新規projectで同じS1集合1回の再実行を指示し、Framework本体の改変を伴わないS1範囲の修正後再実行を許可した。初回失敗・cleanup復旧・各試行のsource／結果を保持して進める。同一範囲の修正・再実行許可を反復要求しない。Framework変更・S2開始・commit／remoteへは拡張しない。以下の再実行未承認表示はこの指示前の履歴。

OwnerのS1実装開始指示によりTooling資材を実装。Java直接回帰2件・guard5件・backend image buildはPASSだが、初回runtimeはCompose tmpfs不備でbackend作成前に停止し、S1はHOLD。[S1結果](docs/architecture/validation/phase4-d3-option2-s1-results-20261109.md)を参照。自動cleanupのnetwork ID／名比較不備も記録し、所有label照合後に今回projectだけcleanup、残留0・生成secret削除・失敗Evidence exportまで成立。両不備は訂正・静的確認済み、動的再実行は未実施。計画の各集合初回1回を維持し、新規projectで同じS1集合1回の再実行案を提示中。S2へ自動進行せず、commit／remoteも先行しない。方式1の保持中資源は操作していない。以下のS1未着手表示は実装前の履歴とする。

## 現行入口：D3方式2の実装・検証条件具体化（2026-11-09）

**Cloud引継ぎ・成果固定の後続指示：** Ownerは[段階別計画§11](docs/development/phase4-d3-option2-staged-task-plan-20261109.md#11-cloud実装自動検証windows再現手動確認)へのCloud実装／自動検証・Windows再現／手動確認の分担、Linux build、Cloud開始前確認、証拠持帰り手順の追記と、本作業branchへのcommit・pushを指示した。今回の文書一式を固定・pushしてCloudへ渡す。以下のcommit／remote未承認表示はこの指示前の履歴とし、PR／merge／tag／Releaseは今回含めない。Cloudでは報告された実commit SHAを確認しS1から順に進め、各段階結果後に次へ判断する。現在のWindowsでは実装・動的実行を先行しない。方式1の保持中環境は維持する。

**後続Owner判断：** 具体化案・DB整理と5段階で進める方針をOwner了解済み。[段階別タスク計画](docs/development/phase4-d3-option2-staged-task-plan-20261109.md)を現行入口とする。S1 DB／基盤→S2 Keycloak／Java API→S3 BFF最小縦断→S4独立2 Client SSO→S5 refresh／障害／終了の順。今回の指示はタスク計画の具体化であり、全段階の実装開始・動的実行は未承認／未着手。次はS1の開始条件・対象／実行上限の確認と開始判断。既存KOIKI tableは構造変更なし、BFF新規3 table案、Keycloak tableは製品管理。段階ごとの結果確認・次段階判断とcommit／remoteを分け、旧一括実行枠を流用しない。

Owner指示により、local main／origin/main／GitHub mainが一致する`cf77a3c58c9dd4a7460e965ec9abe224f668c083`から、新規branch `docs/d3-option2-implementation-conditions-20261109`で方式2（Next.js BFF＋Java API＋PostgreSQL＋Keycloak）の条件具体化へ進む。[実装・有限検証条件](docs/development/phase4-d3-option2-implementation-conditions-20261109.md)を今回の入口とする。Tooling-ownedの計画初版であり、実装・依存取得・image build／起動・動的検証・commit／remoteは未実施。初回は既存Bearer APIの作成／本人詳細／提出と独立2 Clientの選択SSO、3 DB分離を対象候補とする。正確な採用版／digest・file manifest／直接回帰集合・資源／時間・Owner開始判断は同書§8で確定する。方式1の保持中手動project／DB／volume／secret／Evidenceを操作せず、旧予算・承認を流用しない。D3／Phase4全体の認定を変更しない。以下の方式1の実行記録と保持条件は継続して適用する。

## 現行入口：D3改善版の自宅Windows再検証（2026-11-07）

**後続Owner指示による手動環境を稼働保持中：** 新規project `d3-home-manual-20261107`／port18081で改善版imageをbuild・起動し、両利用者のHTTP login／選択routeと元JAR hash label照合が成功。URL `http://127.0.0.1:18081/login`、起動記録は`.tmp/d3-home-manual-20261107/manual-ready.json`。[自宅端末結果§7](docs/architecture/validation/phase4-d3-home-container-reverification-20261107.md#7-後続owner指示による手動確認環境の準備)を参照。手動打鍵・Owner結果受入は未確認。このprojectのapp／DB／volume／secretを自動停止・cleanup・seed再投入しない。下記の手動起動未実施はこの後続指示前の履歴である。

**手動操作後の点検済み：** Ownerは全行程・承認まで問題なしと報告。10:08 JSTの読取点検で5,600円の申請1件APPROVED、差戻し／再編集／再提出／承認のSUCCESS Audit5件、明細整合を確認。app／DB healthy・restart0、app ERROR・Session重複キー例外なし。LOGIN成功6件／BAD_CREDENTIALS失敗1件は区別して[結果§8](docs/architecture/validation/phase4-d3-home-container-reverification-20261107.md#8-owner手動操作報告後のログdb確認)へ記録。結果全体のOwner受入へ拡張しない。手動環境の稼働保持を継続し、上段の打鍵未確認表示は点検前の履歴とする。

Ownerの最新build・実行検証指示により、改善成果`5e01aac`からこの端末で再生成・検証した。[自宅端末結果](docs/architecture/validation/phase4-d3-home-container-reverification-20261107.md)を今回の入口とする。直接回帰25件・image build・既存7 checkpointがPASS。初回buildのsandbox子プロセス`EPERM`と、承認手順後の同範囲成功を分けて保持。project `d3-home-verify-20261107`／port18087のcontainer・DB volume・networkはcleanup済み、image・ignored Evidence／secretは保持。既存`d3-manual-20261105_database`は保持し、再起動・seed再投入していない。別端末の改善版手動環境は操作していない。手動環境起動・layer比較・secret欠落負例・通常root全件は今回未実施。結果Owner受入・commit／remoteは未実施、D3／Phase4全体の認定は維持する。以下の2026-11-06記録は別端末での履歴・保持条件として読む。

## 現行入口：D3方式1のローカル再現・Container改善計画（2026-11-06）

Ownerは別端末PC・Codex Cloudでの再検証を予定し、[引継ぎ](docs/development/phase4-d3-cross-environment-verification-handoff-20261106.md)へ留意点を保存した。次回は実際の改善成果commitと対象環境の能力・実行集合を確認し、このPCの`.tmp`・secret・旧実行予算を流用しない。別環境の実行は未実施。

**改善版手動確認・点検済み：** 後続Owner指示で新規project `d3-improved-manual-20261106`をbuild・起動し、`http://127.0.0.1:18081/login`で稼働保持。image label識別・両利用者のHTTP login／選択route確認はPASS。Ownerは案内順の打鍵でいずれも問題なしと報告し、17:12 JSTの読取点検で申請4,800円の承認済み・差戻し／再編集／再提出／承認Audit・明細整合、app／DB healthy、ERROR／Session重複キー例外なしを確認。[結果§8](docs/architecture/validation/phase4-d3-container-improvement-results-20261106.md#8-改善版owner打鍵報告後のログdb点検)を参照。結果全体の受入へ拡張しない。`.tmp/d3-improved-manual-20261106/manual-ready.json`を参照し、このprojectのapp／DB／volume／secretを自動停止・cleanupしない。旧手動DB volume等の保持も継続する。

PR #60統合後、Owner指示で同branchを継続する。HEAD `335e062`と手元`origin/main` `6bd2b0b`はtree同一。ローカルPCでJDK整合・社内CA・専用Nodeを整備し、Reference再生成／直接回帰25件、現行Containerの既存7 checkpointがPASS。証拠は`.tmp/d3-local-baseline-20261106/`。secret欠落負例は今回再実行していない。
手動project `d3-manual-local-20261106`はOwnerの整理指示で`down`（volume削除なし）を実行し、app／DB container・専用networkを削除済み。DB volume・secret・image・Evidenceは保持し、自動削除・再起動・seed再投入を行わない。Ownerは差戻し等も含む打鍵で問題ない認識を報告し、DB／Audit照合は成立。`.tmp/d3-manual-local-20261106/manual-ready.json`は起動時点の履歴で、現在稼働中を意味しない。現状は同workの`manual-environment-state.json`を参照する。別端末の`d3-manual-20261105`は今回操作していない。
Session属性`SPRING_SECURITY_SAVED_REQUEST`の重複保存例外を観測した。原因契機は未確定で、OwnerはPostgreSQL用Session customizer採用を別件で検討する方針。今回のContainer改善へ混ぜない。
Ownerは[改善計画](docs/development/phase4-d3-container-improvement-plan-20261106.md)を了解し`3b095a8`へcommit後、改修開始を指示した。[限定結果](docs/architecture/validation/phase4-d3-container-improvement-results-20261106.md)を現行入口とする。Boot標準layer化・label識別・専用設定例を適用し、layer測定5 invocation、直接回帰25件、既存7 checkpointがPASS。改善project `d3-improved-20261106`の資源cleanup済み、旧手動DB volume等は保持。自作Reactor JARはapplication区分に入り、loader／snapshot区分は空であることを結果に保持する。A4／B5／B6・Session対応は範囲外。次は結果・差分確認とOwner結果受入・成果固定判断。追加動的実行・手動project起動・commit／remoteを先行せず、過去の承認を流用しない。D3／Phase4全体の認定は変更しない。以下は各時点の履歴。

## 現行入口：D3方式1のContainer構築・限定検証結果（2026-11-05）

**成果固定・統合の現行指示：** Ownerは検証確認後のcommit・作業branch push・main向けPR／mergeを進める方針を示した。今回のReference／Tooling・設計／結果文書を対象に、差分確認・既存required checks成功後のmergeを行う。tag／Release／image公開は含めない。手動確認で報告されたUUIDエラーはTooling seed／専用DBの訂正後にOwnerが解消を確認した。結果全体の受入・D3全体の認定へは拡張しない。以下の未commit・remote未実行表示は成果固定前の履歴として扱う。

**手動確認環境を保持中：** 後続Owner指示で`d3-manual-20261105`を別途build・起動し、app／DBを手動browser確認用に稼働保持した。URLは`http://127.0.0.1:18081/login`。申請者・承認者のHTTP login／選択route、承認入口訂正後の実browser表示・UUID訂正後のMVC保存を確認済み。OwnerはUUID問題の解消を確認したが、結果全体の受入は未記録。`.tmp/d3-manual-20261105/manual-ready.json`と[手順](build-support/container-verification/integrated/README.md)を参照し、このproject／DB／secretを自動cleanupしない。下記のcleanup済みは自動検証project `d3-integrated-01／02`を指す。

Ownerは設計を`ded2698`へcommit後、方式1（SPA同梱Java app＋PostgreSQL）へ進むよう指示した。branchは`docs/d3-01-container-contract-20261105`。今回のReference／Tooling実装は未commit。
[結果](docs/architecture/validation/phase4-d3-integrated-container-results-20261105.md)と[実行資材](build-support/container-verification/integrated/README.md)を入口とする。
image build・起動・実browserの申請作成／提出・CSRF拒否／logout・DB／Audit・app再起動・DB停止回復・cleanupが限定技術成立。直接回帰25件、通常profileでのprobe Security2件成功。7 checkpoint群とsecret mount欠落の別1負例が成立し、初回のTooling失敗と訂正は結果へ保持する。
Framework本体・Root Reactor／CI・公開済みartifactは変更しない。ReferenceはMaven／runtimeの`d3-container`明示選択だけでhealthを追加し、通常Securityを維持する。
次は差分・結果／手順の確認とOwner結果受入・成果固定判断。追加動的実行・commit／remoteは自動先行しない。HTTPS／Secure Cookie・処理中drain・別案件Consumer・全通常root／CI・ECS・方式2SSOは未実施。既存S1未達／人手HOLD・C2 H07・D1結果受入未記録、V10・正式support／Phase4全体の未完了を維持する。以下は各時点の履歴。

## 現行入口：D3-01 Container構成・起動契約の検討（2026-11-05）

Owner指示により、main `c224b146ea615e61bb1b89ea6fd1e17060581cb9`から`docs/d3-01-container-contract-20261105`を作成し、文書の現状追随後にContainer構成の検討へ進む。
E1成果はPR #59でmain統合済み。同commitの通常CI・Java Runtime Compatibility・Virtual Threads Compatibilityはsuccess。
現行checkpointは`checkpoint/v0.1.8-phase4-development-baseline`で同commitを指す。旧名`checkpoint/e1-distribution-established-20261103`は現行local／remoteに存在せず、以下の旧名・未commit／未merge表示は履歴として扱う。
公開版`framework-0.1.0-preview.1`とassetは変更しない。V09限定技術PASS、V10・結果Owner受入・正式support・S1未達／人手HOLD・C2 H07・D1結果受入未記録・全DoD／Phase4全体の状態は維持する。
今回の対象はReference／Tooling-ownedのD3-01構成・起動契約案と文書整合。D3-02 local実行、D3-03実ECS、image公開・deploy、commit／push／PR／merge／tagは今回の文書作業で実行しない。
後続Owner指示で、第1候補（SPA同梱Java app＋PostgreSQL）に加え、第2候補（Java API／Java21＋PostgreSQL＋Next.js frontend／Node.js）も有力案として構成設計する。Next.jsが別案件・別環境でも接続境界を設計対象とする。BFF／BearerかJava Session中継かは未確定で、Customer-owned認証・frontendとFramework責務を分ける。第2候補を将来検討だけとして除外しない。実装・動的検証は範囲具体化後の段階とする。
次の検討入口は[D3-01構成・起動契約案](docs/development/phase4-d3-01-container-contract-20261105.md)。以下は各時点の履歴。

第2候補の後続Owner方針は、Keycloakをlocal IdPとして加えた4コンテナ（Java API／PostgreSQL／Next.js BFF／Keycloak）でのSSO検証環境。localの方式はC-BFF＋OIDCとし、[環境構成案](docs/development/phase4-d3-keycloak-sso-environment-20261105.md)でcanonical issuer、3専用DB／role、既存Reference claim、独立2 ClientでのSSO、実装／有限検証条件を具体化する。実企業IdP・正式BFF／本番採用とは分ける。Compose・BFF等の実装／動的検証は未実施で、旧予算・承認を流用しない。直前段落の方式未確定は本local方式選定前の経緯とする。

## 現行入口：E1新規JAR生成・Release公開と取得確認（2026-11-03）

Ownerは今回の成果commit・作業branch push・main向けPRと必要CI確認後のmerge・main checkpoint tag付与を明示指示した。READMEにも配布経路確立の成果と限定範囲を記載する。tag名は`checkpoint/e1-distribution-established-20261103`とし、マージ後CIを確認したmain commitへ付ける。公開済み`framework-0.1.0-preview.1`は動かさない。以下の未commit／未merge表示は成果固定前の履歴として扱う。

Owner指示で現在sourceからJAR一式を再生成し、Releasesで配布・独立Consumerで受領確認する方針を確定した。実案件パスは未準備。
[結果](docs/architecture/validation/phase4-e1-regeneration-and-publication-20261103.md)と[引継ぎ](docs/development/phase4-e1-vscode-handoff-20261030.md)を入口とする。
`3887d39`から新規生成111件PASS、14 POM＋12 JAR、ZIP SHA-256 `470a2fac4a30d3c323e38341b855e23e263c7aced501c3c8a378e06db1a8b27c`。
ローカルPCの`zaziedlm`／ADMIN確認後、`framework-0.1.0-preview.1` tagとprereleaseを既存public repoへ公開した。旧candidate-3は公開していない。
remote実取得・空の専用Maven repository登録・Consumer `-DskipTests package`成功、26 payload照合・lock残留なしでV09は限定技術PASS。今回Consumer DB journey／通常rootの追加実行はしていない。
tag対象は`3887d39`、manifestのdirtyは生成時の点検文書2ファイルによる。今回の結果文書・入口更新は未commit。作業branchの追加commit／push・PR／mergeは未実施。
V10、結果Owner受入、正式support／全DoD・Phase4全体、既存S1未達・C2 H07・D1結果受入未記録は維持する。公開済み同一版assetを差替えない。以下は各時点の履歴。

## 現行入口：E1ローカル再現結果と公開手順の整理（2026-10-30）

OwnerはCloudの別渡しZIPに依存せず、ローカルPCで設計・実装を再現確認するよう指示した。
[Windows再現結果](docs/architecture/validation/phase4-e1-local-windows-reproduction-20261030.md)と[ローカル公開手順](docs/development/phase4-e1-vscode-handoff-20261030.md)を現行入口とする。
VSCode拡張管理のTemurin21.0.13+9で、訂正後の候補生成111件・準備6確認・Consumer1 invocation／8 checkpoint・通常root449実行PASS＋100 skip、cleanup／source／ZIP照合が成立。初回失敗と訂正履歴は結果文書に保持する。
現行候補はlocal candidate-3。Cloud候補のZIP／hash／Git bundleを再開条件にせず、候補識別はWindows結果を参照する。binary・raw logはGitへ格納しない。
公開操作（tag／Release作成・asset upload）は、認証主体`zaziedlm`と対象repoへの書込権限を確認したローカルPCで行い、Codex Cloud等から実行しない。配布先は既存public `zaziedlm/KOIKI-JAVAWEB`。新規専用repoは作成しない。
候補生成と取得／Maven登録はTooling実装済み。asset公開は引継ぎ§6のGit／GitHub CLI手順で行う構成で、専用publish scriptは未実装。旧`publish-initial-release.ps1`は廃止済みの停止処理。
今回のWindows対応・文書訂正は未commit。過去のCloud成果commit／push指示を今回の新規操作へ流用せず、文書訂正に公開・commit／push・PR／mergeを含めない。
結果Owner受入・公開対象commit確定・V09／10は未完了。既存S1未達・C2 H07・D1結果受入未記録を維持する。以下は各時点の履歴。

## 履歴入口：E1をVSCodeへ引継ぎ・成果commit／push（2026-10-30）

OwnerはCloudのRelease作成／uploadを今後の実行対象から外し、一通り準備後、この作業branchをcommit／pushしてローカルVSCodeでpull・継続すると指示した。
[VSCode引継ぎ](docs/development/phase4-e1-vscode-handoff-20261030.md)を現行入口とする。branchは`docs/phase4-s1-b1-c2-guarantee-review-20261030`、remoteは`zaziedlm/KOIKI-JAVAWEB`。
今回のlocal commitと当該branchへのpushはこの指示に基づき実行する。pushは`zaziedlm` GitHub連携を使用。PR／main merge、tag／Release作成・asset upload・remote実downloadは今回含めない。
検証済み候補ZIPはGitへ入れず、元source記録・原本Git bundle・remote保存対応とともに別packageで受け渡す。旧専用repo用handoffは使用しない。
V01〜08の結果・既知制限、V09／10・結果Owner受入の未完了を維持。CloudのRelease権限／network制約を再調査せず、PC側で公開対象commitとsource対応を点検して継続する。
以下は各時点の履歴で、未承認・未push・CloudからのRelease継続等の過去表示を現行判断へ流用しない。

## 履歴入口：E1ローカル実装・限定検証（2026-10-30）

Owner指示により`docs/phase4-s1-b1-c2-guarantee-review-20261030`を継続。保証範囲・DoD整理は`8fe8473`へlocal commit済み、remote未反映。
[E1-01比較初版](docs/development/phase4-e1-artifact-distribution-options-20261030.md)で14座標（14 POM＋12 JAR）、固定版・manifest、取得／更新・独立ConsumerとR-05／06の確認条件を整理する。
OwnerはFramework本体モジュール群のRelease asset一式配布を優先候補に選定。取得は`gh release download`でrelease tag・asset名を明示し、準備スクリプトで照合・Maven local repository登録を行う方針（未実装）。全モジュール同一versionで固め、binaryはGit履歴へ格納しない。Gitは案内・manifest・checksumを管理。公開先・取得権限は未確定。第2項の命名・範囲の説明をOwner了解、§7.2に正式版`1.0.0`との対応を補足。第3項は§7.3にKOIKI Parent・独立Consumer・PostgreSQL同一DataSource／schemaとPC／CI確認の構成案を整理。Ownerの第4項への進行指示を受け、§7.4へTooling・候補生成・登録・V01〜10の段階別計画を具体化。計画・開始時点の経緯として保持し、現在は以下の結果を優先する。
Ownerは§7.4の具体計画・JAR生成からRelease asset取得までの流れを了解し、進行を指示した。V01〜08のローカルTooling・独立Consumerを実装し、[結果](docs/architecture/validation/phase4-e1-artifact-distribution-results-20261030.md)へ記録。候補14座標の既存test111 PASS、Consumer1 invocation／8 checkpoint PASS、通常root449実行PASS＋100 skip、cleanup・ZIP／source照合成功。登録系補助JSON比較の初回ERRORと局所訂正を保持。
R-05はCustomer自身のEntityScan明示で成立、R-06はData Starter明示・同一DataSource／既定schemaの限定構成。Framework本体・作業checkout POM・Root CI変更なし。版変換は専用stagingのみ。
Ownerは配布先を既存リモートへ見直すよう指示。`origin`とGitHub metadataを照合し、現行は`zaziedlm/KOIKI-JAVAWEB`（public、default main）を使用。Release assetもpublic。新規`KOIKI-JAVA-asset`は作成しない（E1§8.2）。
全モジュール同一版のZIP・hash・Maven登録は維持し、取得先だけ既存repoへ変更。sourceの成果固定・remote反映・公開tag対象を先に点検する。Framework source branchのcommit／push／PRは今回未実施。
旧private専用repo用handoff ZIPは使用しない。作業checkoutの旧publish scriptは停止処理へ変更。既存source repoのREADMEを配布専用READMEへ置換しない。
`zaziedlm`連携の管理・書込権限は確認済みだが、現行connectorにRelease作成／binary uploadのtoolはなく、Cloud CLI APIもForbidden。remote登録・取得V09、案件受領V10と結果Owner受入は未完了。
以下は各時点の履歴。S1の未達、D1結果受入未記録、個別DoDの最終受入待ちは維持する。

## 履歴入口：S1／B1／C2の保証範囲・DoD整理（2026-10-30）

Ownerの第3段階への進行指示により、main `fdfcfa5`（PR #58統合後）から`docs/phase4-s1-b1-c2-guarantee-review-20261030`を作成した。
[判定初版](docs/development/phase4-s1-b1-c2-guarantee-and-dod-review-20261030.md)は既存Evidence・受入・現行sourceの静的照合。B1のDoD4-8／4-9は選定Reference構成の技術成立・最終受入候補、C2 A限定受入とH07未実証、S1のDoD未達・人手HOLDを分けて記録する。
DoD原文・受入済み範囲を変えず、未達をPASSにしない。追加実装・動的検証・運用基盤の深掘りは行わない。非同期の深掘りは採用案件の具体化時に必要範囲で判断する。
D1はmain統合・限定技術成立、結果Owner受入の未記録を維持。同期traceの成功で非同期DoD4-12を充足しない。
OwnerはS1の既知未達・運用／追加開発リスクを認識したうえで、[整理文書§7](docs/development/phase4-s1-b1-c2-guarantee-and-dod-review-20261030.md#7-現時点のエビデンスとしてのowner採用判断2026-10-30)を現時点のエビデンスとして採用する判断を示した。個別DoDの最終受入・未達判定は変更しない。次は成果固定対象の確認と、E1の提供artifact・版識別・参照取り込み／更新の方式整理への接続。commit・remoteは別承認。以下の成果固定・remote待ち等は各時点の履歴。

## 履歴入口：D2限定受入記録・D1計画整理（2026-10-30）

Owner指示によりmain `b2ca253`から`docs/d1-sync-observability-plan-20261030`を作成し、D2の受入記録・古い表示の訂正とD1計画整理を同branchで行う。
[D2-03§7](docs/development/phase4-d2-03-ci-and-adoption-guide-20261026.md#7-限定結果dod-4-11のowner受入記録2026-10-30)へ説明済み限定範囲／DoD 4-11のOwner受入を記録。既定無効・制限を維持し、D2全体・正式提供へ拡張しない。
[D1計画](docs/development/phase4-d1-sync-observability-plan-20261030.md)はOwner承認に基づき独立Tooling11 fileを実装。初回helper ERROR、次回O02期待値FAILを保持し、各訂正後に個別承認の同範囲一回を実行した。
[結果§7](docs/architecture/validation/phase4-d1-sync-observability-results-20261030.md#7-o02期待値訂正後の同範囲一回2026-10-30)で全10 checkpoint PASS、JUnit1 PASS・cleanup／source／JAR照合成功。Framework／Reference・Root Reactor・CI変更なし。限定接続は技術成立、Owner結果受入は確認待ち。
現行の次作業は結果・Git差分と成果固定対象の確認。さらに動的実行を追加しない。結果確認後の優先順はS1／B1／C2保証範囲・DoD整理→E1→D3。
正式Collector・backend・本番運用、DB／外部client span・全拒否／全executorは今回未実証。既存S1未達・人手HOLD・DoD 4-12は維持し、非同期採用案件の具体化時に設計・運用を深掘りする。
commit・remoteは今回の実行承認に含まれず、下記の過去のmain統合承認を新規操作へ流用しない。

## 履歴入口：初期標準確立とmain統合（2026-10-23）

現行の推進計画は[統合作業計画§9](docs/development/phase4-review-integrated-task-plan-20261020.md#9-初期標準確立に向けた見直し推進計画owner-approved2026-10-23)。複数案件採用を見据え、初期標準一覧の整理→D2→D1同期→S1／B1／C2の保証範囲・DoD判定→E1→D3→要件具体化に応じた残タスク、の順をOwner承認済み。
B1の実装・選定検証とブラウザ結果受入、C2 Aの限定結果受入まで進んでいる。以下の「次はB1」等は履歴として読み、初期調査や非同期全件を自動再開しない。初期標準一覧は[提供機能ガイド§0〜1](docs/development/framework-capabilities-and-selection-guide.md#0-初期標準一覧の読み方と確定までの判断)に整理済みで、方向性はOwner承認済み。最終提供版・DoD全体・Phase4全体の認定は別に残る。
Ownerは本branchのmain向けPR・mergeを優先し、文書共有はmain統合後とする方針を示した。今回の成果固定・push・PR作成、差分レビューと必要CI確認を経たmergeを進め、共有用入口をmainへそろえる。正式release／artifact publish・tag作成・案件チームへのメッセージ送信はこの作業に含めない。

## 現行判断：非同期系の作業を区切り、他のPhase4残タスクへ移行（2026-10-17）

Owner指示により、非同期系は現時点の実装・限定検証と運用方針で今回の作業を区切る。復旧に苦慮する部分は人による照合・担当付きHOLDで補完する方針を優先し、追加の復旧機構・読取GET・提供手順の整備を他タスクへの移行条件にしない。
既存の限定受入結果とDoD未達・運用準備のOPENは保持する。S1全体完了や本番運用成立とはしない。必要が具体化した場合に対象を限定して再開を判断し、自動でS1を継続しない。
現行の作業入口は[残タスク棚卸し§8](docs/development/phase4-continuation-task-inventory-20261016.md#8-非同期系の区切りと当初phase4残タスクへの移行2026-10-17)。次は当初計画の他packageを対象に範囲・着手条件を確認する。次に進む対象はP4-B1：Session SPA＋MVC Reference。既存との差分確認から始め、実装・検証範囲はその結果に基づき定める。

## 現行追加判断：S1 R4 consume→単発復旧の接続実証の条件付き限定開始（2026-10-16）

承認文書はOwnerが`c749e44`へcommitし、cleanな新実装branch・fresh preflightを経て限定実行を終了した。
[実証記録](docs/architecture/validation/phase4-s1-r4-consume-recovery-connection-20261016.md)は新規7件/直接回帰4件PASS、通常root305実行PASS＋99 skip、cleanup成立。**限定結果はCOMPLETE / OWNER ACCEPTED（実証記録§5）**。
次は結果と成果差分の点検。以下の開始条件・予算は今回実行の根拠として保持し、追加実行へ再利用しない。成果commit・remote・後続開始は別判断。

Ownerは[接続実証開始票§7](docs/development/phase4-s1-r4-consume-recovery-connection-limited-start-review-20261016.md#7-owner承認記録と網羅性の扱い2026-10-16)を承認した。
承認文書commit（別承認／Owner操作）・新実装branchのclean source固定・fresh preflight成立後に限り、
Reference-ownedの未登録Application接続1 fileとtest/fixture2 fileを作成し、新規7件・直接回帰4件・通常root1回を実行できる。
新規60分（cleanup5分予約）、phase別資源・限定副作用・停止／cleanupは同票に従う。Ownerは今回所有の隔離DB／role・
finite fixture・二要求thread、専用roleの有限Audit INSERT権限操作とcleanupを同票内で委任する。実行環境の権限付き承認を維持する。
新規consumeの正常復帰後だけ既存recoverを一回呼び、既消費・結果不明・例外から自動再実行しない。復旧先はtest用代替を含む限定実証。
追加受付tableは保留。新table／migration／依存・既存code、Bean／runner登録・正式認証入口・FAILED整合・実provider・歴史closeを変更しない。
7件は網羅確定ではなく、不足は都度提示し追加／見直しを判断する。実行範囲・件数・path・予算を変える追加や再実行は自動で先行しない。
限定結果の受入は実証記録§5で成立。local成果commit・remote・R4／DoD／Phase4全体は別判断。副Agentは使用しない。
以下のR2／R3未承認等は各時点の履歴とし、今回承認を別作業の開始や旧予算の再利用へ拡張しない。

## 現行追加判断：S1 R2限定結果のOwner受入（2026-10-13）

R2の選択失敗分離・通常構成保護の限定結果は`COMPLETE / OWNER ACCEPTED`。
[結果文書§5](docs/architecture/validation/phase4-s1-r2-selected-seven-case-results-and-acceptance-input-20261013.md#5-r2限定結果のowner受入承認2026-10-13)を受入と現行結果の入口とする。
初回R2-04のFAIL・root停止を履歴として保持し、局所再throw後の通常root303実行＋専用92skip、通常／async JAR対照、後段7件PASSをsource／artifact／epoch別に扱う。
実provider配信・停止後再送／復旧・UNKNOWN照合・R3〜R5・DoD全体・正式受渡し・Phase 4全体へ拡張しない。
次はGit差分と成果固定対象の点検。local commitは別承認／Owner操作、remoteは個別承認とする。以下の開始条件・停止判断・未受入記述は各時点の履歴であり、追加実行や残予算の許可に流用しない。

## R2局所再throwの限定訂正承認（2026-10-13）

Ownerは[限定訂正開始票](docs/development/phase4-s1-r2-local-rethrow-limited-correction-start-review-20261013.md)の§2〜5を承認した。
expense MVCの局所AccessDenied再throwだけをproduction1 file、MVC test1 file／新規3件、既存Tooling観測1 fileの訂正差分として採用する。
既存Tooling4 fileを含む未commit採用差分をisolated snapshot・hashで固定し、preflight成立後だけ早期3件・通常root1回・package7件を実行する。
新規45分（cleanup5分予約）、phase別資源・所有／停止／cleanupは同票に従う。通常root／C01のRyuk2／client14とR2のRyuk1／client8を区別する。
Framework API／依存／Security設定／Rules／migration変更、拒否Audit新設・一般Framework修正・追加再実行・remoteは含めない。
以下のTooling-only条件への例外はこの個別承認範囲だけ。旧FAIL・比較source／artifact・予算を上書き／流用しない。

## 現行追加判断：S1 R2失敗分離の条件付き限定開始（2026-10-13）

Ownerは[R2開始票§8](docs/development/phase4-s1-r2-purpose-and-minimal-verification-owner-review-20261013.md#8-owner承認記録と文書固定対象2026-10-13)を承認した。
R1は`COMPLETE / OWNER ACCEPTED`、成果`610c589`・引継ぎ`18cb8b6`はOwnerによりcommit／push済み。
設置済みGate P4-FへR2失敗分離だけを条件付き追加する。必要正本・承認文書commit（別承認／Owner操作）、
新R2 branchのclean source固定、同票preflight成立後だけTooling4 file以内・新規1 class／8件＋直接回帰4件を作成検証できる。
新規45分（cleanup5分予約）、JVM4／PG1／Ryuk1／DB client8、各集合初回1回・追加再実行0回。
今回所有の隔離DB／role／対象限定trigger、loopback stub、HTTP認証fixture、package子JVMと有限cleanupを同票内で委任する。
同期承認・同transactionのAudit／publication保存、commit後の非同期通知という境界を維持する。故障注入はToolingに閉じる。
production／Framework API／Rules／POM／依存／migration／Security／CI変更を行わない。
対象外修正・条件不成立・上限超過・観測／cleanup不成立では停止しOwner判断へ戻す。
R3〜R5・実provider・再送／復旧・consume／close・DoD全体・正式受渡し・Phase 4全体・remoteは別判断。
旧予算・runnerを流用せず、副Agentや検証網羅の拡張を行わない。以下のR1開始／成果固定待ちは各時点の履歴として扱う。

## 現行追加判断：S1 R1限定結果のOwner受入（2026-10-13）

R1正常非同期経路・通常構成保護の限定結果は`COMPLETE / OWNER ACCEPTED`。
[結果文書§13〜15](docs/architecture/validation/phase4-s1-r1-implementation-and-early-verification-20261012.md#15-r1限定結果のowner受入承認2026-10-13)を現行結果・機能説明・受入承認の入口とする。
通常root300実行＋専用92skip、通常／async artifact静的照合、新規9件（root3＋Tooling6）と既存Critical Journey1件が成立した。
過去の停止と後続cleanup観測、source／artifact epochを区別して保持する。ローカルstubを含む選択正常経路の受入であり、
実provider配信・停止／再送／復旧・R2〜R5・全DoD・正式受渡し・Phase 4全体へ拡張しない。
次は成果固定のGit差分・対象点検と、別承認／Owner操作によるlocal commit。remote操作は個別承認を維持する。
以下の2026-10-12開始条件と過去承認は判断時点の履歴で、追加実行・残予算の許可へ流用しない。

### R1条件付き限定開始の承認条件（2026-10-12）

Ownerは[R1必要最小限の開始票§8](docs/development/phase4-s1-r1-minimal-start-owner-review-20261012.md#8-owner承認反映と文書source固定の対象2026-10-12)を承認した。
R0の規約限定改訂・最小検証・結果受入は完了。承認済み正本・ADR-051・承認記録の文書commit、
新R1 branchでのclean source固定と同票preflight成立後にだけ、Reference-owned expense承認値event、
notification非同期listener／送信Port／loopback stub接続Adapter、Reference限定Modulith JDBC依存・追加publication migration・
条件付き登録と当該有限test／Evidenceを作成・検証できる。通常無効、既存Security／Identity／master同期veto／
expense認可・Audit／既存permit契約を維持し、Framework API／Rules／依存・Root Reactor／CIを変更しない。
対象差分28 file以内、文書12 file以内、新規4 class／9 invocation候補、最終通常root1回、検証70分・cleanup5分予約。
JVM4／PG1、通常rootと既存E2EはRyuk2／client14、新規ToolingはRyuk1／client8。他の資源・phase・停止／cleanupは同票に従う。
Ownerは今回所有の使い捨てDB・専用schema／role・loopback stub・package子JVM／browser・有限cleanupを同票内で委任する。
権限付き実行は実行環境の承認手順を維持し、副Agent・旧監視74例の拡張・root自動反復を行わない。
S1／B2専用92件は通常build分離を維持し専用全件再検証をしない。直接影響が判明した場合だけ必要部分を別判断する。
不足依存取得・追加再実行は同票の個別条件に従う。local commitはOwner操作または操作前承認、remoteは個別承認。
R2〜R5、肯定consume／close、停止後再送・復旧runner、実provider送信、DoD・正式受渡し・Phase 4全体へ拡張しない。
以下の過去承認・R0旧予算は各判断時点の履歴で、再開許可・残予算として流用しない。

このRepositoryでは、`KOIKI-JavaWeb-FW グランドデザイン v0.2`の
`ACCEPTED（Phase 0 Architecture Baseline）`を上位設計とする。

Phase 0、Phase 1a Build Foundation、Phase 1b Runtime Foundationおよび
Phase 2 Security Foundationは`COMPLETE / ACCEPTED`である。Phase 3 Reference Vertical Sliceは、
`docs/development/KOIKI-JavaWeb-FW_Phase3実行計画_v0.1.md`のGate P3-1承認とCP境界に従う。
P3-CP0とP3-A0は`COMPLETE`、P3-A1〜P3-A4およびGate Aは`COMPLETE / ACCEPTED`、P3-B0は
`COMPLETE / OWNER APPROVED`、P3-B1は`COMPLETE`、P3-B2〜P3-B4は
`COMPLETE / OWNER APPROVED`、Gate Bは`COMPLETE / ACCEPTED`、P3-C0〜P3-C2は
`COMPLETE / OWNER APPROVED`である。P3-C3 MyBatis規約fixture / Rule 35〜37はArchitecture Owner判断により
`DEFERRED — MyBatis adoption trigger required`であり、`PersistenceModel.SEPARATED`、Rule 25〜27 / 30〜37、
MyBatis fixtureおよびtest dependencyを追加せず、Rule 8のMyBatis拒否を維持する。P3-C4 Journey / ADR /
Skill / DoD traceは`COMPLETE / OWNER APPROVED`である。
Remote Gate計画は`OWNER APPROVED`であり、RG-4 push、RG-5 draft PRおよびRG-6 required check追加まで
`APPROVED / EXECUTED`である。`Phase 3 Critical Journey E2E`を含むrequired checks 8件を維持する。
DoD 3-11の実CI PASS、Gate CおよびPhase 3全体は`COMPLETE / ACCEPTED`である。
PR #35はmerge commit `aa83fa578b5f689ce72e2a2540ad3ec2b659c083`として`main`へmerge済みであり、
merge後CI 7 / 7 jobsとJava Runtime Compatibility 2 / 2 jobsは成功した。Phase 3 remote closeoutは完了している。
post-merge closeoutでFramework採用実案件の始動とPhase 4作業の一部移管見込みが明らかになったため、
Phase 3を再オープンせず、Pre-Phase 4 Adoption Readiness（P4-AR）をtransition Gateとして追加した。
P4-AR0とAR-1〜AR-7は`COMPLETE / OWNER APPROVED`である。承認記録をRepositoryへ反映してから、同期済みclean `main`で
P4-AR1以降のFramework検証を開始する。Phase 4開始は未承認である。
Phase 4全体開始は未承認のまま維持する。Gate P4-Fの設置・限定開始と必要正本改訂が
Architecture Ownerにより明示承認された場合だけ、承認記録のsource、Ownership、対象、
検証上限と停止条件に従ってS1初回Reference基盤を作成・検証できる。初回はnotificationの
許可・append-only消費の保存／認可／Auditと条件付き登録、Reference-owned追加migrationに限定する。
通常起動では無効とし、既存identity／master／expenseとSecurityを保護する。
Web／CLI受付、sender、listener、復旧runner、publication schema、Modulith runtime、
Framework API／Rules／依存変更は含めない。後段は必要reviewと個別開始判断を経る。
この限定条件の正本改訂は2026-10-06にOwner承認済みである。改訂承認はGate設置／限定判定・
実行開始の承認ではない。[承認記録](docs/development/phase4-s1-reference-foundation-canonical-delta-and-start-conditions-20261006.md#9-正本改訂のowner承認反映記録2026-10-06)と
[正式開始票](docs/development/phase4-s1-reference-foundation-formal-start-review-20261006.md)に従う。
後続判断でGate P4-Fを設置し、ST-C初回Reference保存・認可・Audit基盤だけを
`APPROVE LIMITED START`とした（2026-10-06、[Gate判定記録](docs/development/phase4-s1-reference-foundation-execution-review-20261006.md#7-gate-p4-f設置初回限定判定2026-10-06)）。
正式作成・検証開始は別判断であり、文書source固定・承認済みpreflight成立前に開始しない。
後続の[正式開始承認記録](docs/development/phase4-s1-reference-foundation-execution-review-20261006.md#8-source固定preflight条件付き正式開始承認2026-10-06)で、
文書commit・clean source固定後のpreflightと、成立時の採用済み初回code／test／V4／検証専用設定の作成・
Maven／隔離Docker検証をOwner承認済み（2026-10-06）。条件未成立で作成・検証を先行しない。
2026-10-07の[scope／TTL供給の限定開始承認](docs/development/phase4-s1-managed-scope-ttl-limited-start-review-20261007.md#8-owner承認条件付き限定開始2026-10-07)により、
Reference-ownedの外部管理設定・起動固定snapshotによるscope／TTL Adapter、発行直前の期間再検査、
条件付き登録と当該test／Evidenceを、文書commit・clean source固定・採用済みpreflight成立後に作成・検証できる。
検証は新規5 class／40 case候補、初回55件と既存25 class／99件・package済みE2Eに限定し、
同票の資源・時間・作業量・cleanup・停止条件に従う。運用target／停止／provider証拠は未接続時拒否を維持する。
実設定配備・運用操作、Web／CLI受付・worker認証／委譲、sender／listener／runner、publication／Modulith runtime、
Framework API／Rules／依存・remoteへ拡張しない。local commitはOwnerへの事前確認またはOwner自身の操作とする。
2026-10-08の[対象・運用証拠ハーネスの限定開始承認](docs/development/phase4-s1-operational-evidence-harness-limited-start-review-20261007.md#9-owner承認条件付き限定開始2026-10-08)により、
test所有の可変現在対象・停止制御model・provider観測stub・証拠台帳と現行Portへの接続検証、
既存Harnessの必要最小変更・当該Evidenceを、文書commit・clean source固定・preflight成立後に作成・検証できる。
新規5 class／48 invocation候補、scope／TTL40件・初回55件・既存99件＋E2Eと、同票の資源・時間・
作業量・cleanup・停止条件に限定する。D07の隔離DB内Audit権限一時REVOKE／finally復元を含む。
本番main／Port／schema／grant定義／依存／通常構成は変更しない。同一環境・publicationの未閉鎖permit一意制約を維持し、歴史対象閉鎖・別publicationにまたがる論理通知の抑止・
照合後競合の未達は観測・記録だけで、実運用保証にしない。実運用Adapter・停止操作・provider通信、
受付／worker／送信、publication／Modulith、Framework／CI／remote・Phase 4全体への開始承認ではない。
preflightで判明したD11の前提誤りは同開始票§10へ記録し、§11でOwner訂正承認済み（2026-10-08）。
訂正・承認文書commit後のclean source再固定と成立済みpreflightからのsource／環境差分確認を経て、
訂正後D11を含む承認済みtest作成・48件検証へ進む。同じ範囲の開始承認を再要求しない。
2026-10-08の[B-1読取接続の条件付き限定開始承認](docs/development/phase4-s1-stage-b1-read-connection-limited-start-review-20261008.md#11-owner承認条件付き限定開始2026-10-08)により、
承認文書commit・clean source固定後のpreflightと、成立時のTooling test所有の読取契約／collector／process台帳／
独立schema証拠保管／SELECT専用JDBC reader、専用test SQL・新規6 class／52件・Evidenceを作成・検証できる。
対象は`build-support/phase4-level2-verification/`。本票の使い捨てDB／専用schema・role、有限fixture準備、当該子JVMの
起動・イベント発行・stub受理・当該終了待機・観測／証拠保存・cleanupを限定副作用として含む。
Ownerが限定環境管理責任を兼務し、Agentへ開始条件成立後の本票内操作を委任する。
選択PL2回帰6件・Reference関連143件・既存99件＋E2E、資源・時間・作業量・raw／再実行上限は同票に従う。
Reference／Tooling main・既存test／migration・POM／依存・Root Reactor・Security・通常構成・CIは変更しない。
B-2のReference Adapter／肯定Port登録・許可操作、外部送信／復旧runner／Reference Level 2・Phase 4全体・
DoD／正式受渡し・remoteへ拡張せず、D11／D12と網羅性懸念・未達を保持する。副Agentは使用しない。
local commitはOwner操作または操作前のOwner確認とし、実装結果の受入は別判断とする。
2026-10-08の[同開始票§13の訂正承認](docs/development/phase4-s1-stage-b1-read-connection-limited-start-review-20261008.md#13-12訂正のowner承認2026-10-08)により、
既存test-only依存を含む`jdbc,s1-contract,s1-web` profile補足と、Toolingの`PublicationRecoveryTest.java`／
`ProcessCrashRecoveryIT.java`の最小資源設定・launch差分、test-only `B1ResourceLimits.java`1件追加を許可する。
既存test変更禁止の例外はこの2ファイルの明示選択時の資源設定だけとし、assertion・件数・通常起動条件を維持する。
訂正・承認文書commitとclean source再固定・source／環境差分確認後に、実効制限確認に必要な3ファイルを
作成・限定検証する。制限成立前に新規52件のハーネス作成・検証を開始しない。
検証集合・資源／時間／作業量・raw上限・停止条件・main／POM／migration等の除外は維持し、同じ訂正承認を再要求しない。
2026-10-08の[同開始票§15のruntime profile訂正承認](docs/development/phase4-s1-stage-b1-read-connection-limited-start-review-20261008.md#15-14訂正のowner承認2026-10-08)により、
`jdbc,s1-contract,s1-web`はoffline test-compile専用とし、既存4件と選択IT2件のruntimeは`jdbc`単独とする。
§13の文書commit・clean source固定済み`fad5ded`を基点に、今回の承認記録・source／環境差分確認後、
compile済みtest-classesのSurefire／Failsafe直接goalで既存4件の正常化・実効制限を確認し、成立時に選択IT2件へ進む。
Tooling JARもjdbcのmain依存でpackage・確認する。既存失敗を保全し、code差分3件・検証集合・上限・停止条件を維持する。
制限成立前の新規52件、main／POM／依存／migration追加変更へ拡張せず、同じ訂正承認を再要求しない。
結果受入・B-2／実運用／remoteは別判断とする。
2026-10-08の[同開始票§17の診断保全承認](docs/development/phase4-s1-stage-b1-read-connection-limited-start-review-20261008.md#17-16診断保全のowner承認2026-10-08)により、
test-only `B1ResourceLimits.java`の失敗診断保全と選択IT2件の限定再検証を許可する。
credential／接続情報を除去した有限量の子log・終了状態を既存cleanup前に保存し、子終了・元log cleanupを維持する。
これまでの訂正経緯の点検は文書・差分・実行記録の照合だけとし、追加原因修正は別判断とする。
assertion・件数・資源上限・停止条件を維持し、新規52件・結果受入・実運用・remoteへ拡張しない。
同開始票§18へOwnerの点検記録確認と調査続行を記録した。helperの診断条件を変えた限定確認でも回帰失敗したため、
追加実行・原因修正は停止し、残る保持接続の診断追加は同票§19のOwner判断を経る。
2026-10-08の同開始票§20で§19採用をOwner承認済み。helperのpackage logger・起動中1回のSELECT診断と
選択IT2件1回を実施できる。保持元の完全特定を目標とせず、得られる実値・DB状態を実装判断へ整理する。
資源／migration／業務assertion・件数を維持し、診断反復・追加原因探索・原因修正を自動で先行しない。
同開始票§22で§21の子pool設定供給訂正をOwner承認済み。helperの明示選択launchだけでHikari数値を
canonical command propertyへ揃え、source／環境差分確認・compile後に選択IT2件を1回確認する。
数値・秘密のenv供給・transaction／migration／業務assertion・件数を維持し、起動初期からの実値を確認する。
不一致・回帰失敗・上限超・cleanup失敗では停止し、追加修正・新規52件・結果受入・remoteへ拡張しない。
2026-10-08の[B-2初回issue／readの条件付き限定開始承認](docs/development/phase4-s1-stage-b2-issue-read-limited-start-review-20261008.md#13-owner承認条件付き限定開始2026-10-08)により、
承認文書commit・clean source固定後のpreflightと、成立時のReference-owned保護Port／outer service／
SELECT専用JDBC target Adapter・有限設定・条件付き登録の新規main5件と既存Configuration Import1件、
Tooling test所有のcoordinator／凍結protocol／専用SQL・新規6 class／54件・当該Evidence等の作成・検証を許可する。
全差分は同票の新規18件＋既存2件、回帰301件を含む355件、最大4 JVM／DB1／接続8、検証90分と累積予算・
作業量／raw・再実行上限・cleanup／停止条件に限定する。Ownerが限定環境管理責任を兼務し、Agentへ条件成立後の
当該fixture準備・通常子終了・writer NOLOGIN／権限取消・接続確認・凍結証明登録・隔離DB／子JVM操作を委任する。
通常無効・未接続拒否、既存Identity／scope／TTL／保存・Auditを維持し、consume／closeの証拠Portはemptyのままとする。
既存B1 source／test／SQL、既存Service／Port／Repository／Domain・migration・Security／通常properties、
POM／依存・Root Reactor／Framework／CIは変更しない。肯定consume／close・送信／復旧／worker委譲・Reference Level 2・
実運用／DoD／正式受渡し／Phase 4全体・remoteに拡張しない。副Agentは使用しない。
local commitはOwner操作または操作前確認、結果受入は別判断とする。文書commit・clean source固定前に実装・検証を先行しない。
2026-10-08の[B2結果受入記録](docs/architecture/validation/phase4-s1-stage-b2-issue-read-20261008.md#19-b2限定検証結果のowner受入承認2026-10-08)により、
初回issue／readの新規54＋回帰301件・artifact整合／package／cleanupは`COMPLETE / OWNER ACCEPTED`。
保存結果と整合後検証のartifact epochを区別し、肯定consume／close・実運用／DoD・Phase 4全体へ拡張しない。
source固定対象・端末内証拠／artifactの所在・再実行条件と残課題は
[B2成果固定・次作業引継ぎ](docs/development/phase4-s1-stage-b2-fixed-results-next-session-handoff-20261008.md)を参照する。
受入はlocal commit／remoteや後続操作の開始承認を兼ねず、成果固定commitは操作前のOwner確認に従う。
P4-ARではFramework本体だけでなく、Customer-like Consumer、package済みReference Application、検証Toolingおよび
Developer Journeyを受渡し候補として棚卸しし、業務アプリ開発チームの受入側視点でbuild / run / operation / diagnosisを実証する。
正式な受渡し対象はP4-AR Evidenceを入力とする見直し後Phase 4準備で判断し、Project TemplateはPhase 5境界を維持する。
Framework Public API、migrationまたは未承認のremote変更を先行しない。

Phase 3では、承認済みbaselineを維持し、次を優先する。

1. Spring標準機能を優先する。
2. Framework / Reference / Customer / Tooling / Walking SkeletonのOwnershipを混在させない。
3. Walking Skeleton、Reference、Customerまたはtest fixtureのcode、Template、migration SQL、一時Maven座標を正式Framework成果物へ直接昇格させない。
4. P3-A1以降を実行計画の順に進め、Gateまたはblocking reviewを先行しない。
5. P3-CP0は承認記録、Agent導線、start Evidenceに限定し、production code、Public API、Maven module、migration、dependencyまたはworkflowを変更しない。
6. P3-A0で承認済みの有効master検証契約、承認者部門scopeのReference Ownership、Reference migration / FK方針とADR-049を維持する。
7. masterはTier 1 SIMPLE / JPA、expenseはTier 2 RICH / JPA共有モデルとし、単一`koiki-reference-app`内の業務packageとして分離する。別Maven artifactへ分割しない。
8. Spring Modulith Level 1ではcommand整合に同期Domain Eventを使用する。current-valueの有効master確認はADR-049の狭い同期read-only module contractをexpense Port / Adapter経由で利用し、他moduleのApplication、Domain、RepositoryまたはAdapterを直接参照しない。表示専用read modelだけはADR-038 P3-B1 fittingに従い、scopeをSQL内で先に強制し、更新・認可判断・業務不変条件へ利用しないread-only JOINからApplication所有の最終recordを直接materializeしてよい。Level 1のためのruntime依存、transactional / async eventを追加しない。
9. Phase 2のdefault deny、CSRF / Security Header、Identity、Business / Security Audit、Spring Session JDBCの承認済み契約を再利用し、弱めない。業務属性をFramework Identityへ追加しない。
10. MVC / Thymeleaf / HTMXとRESTは同じApplication Use Caseを利用するが、Controller、Form、View DTO、REST DTOを共有しない。Domain Model / JPA Entityを外部へ露出しない。server-side UIはThymeleaf HTMLを主軸とし、HTMXは効果と検証可能性を説明できる操作だけに選択適用する。
11. APIと自動testを回帰の主軸とし、操作面が成立するP3-B2以降は実browserでの目視・手動操作とlog / Audit / DB突合を組み合わせる。
12. 個別のPublic API、property、migration SQL、Starter、外部library、cacheまたはREST契約は、実行計画が指定するblocking reviewとEvidenceより前に固定・追加しない。
13. Project Template、SPA、Spring Modulith Level 2、MyBatis accounting、Oracle、AWS固有Adapter、Authorization Server、SAML、Redis、WebFluxおよびPhase 5成果物を先行しない。
    Gate P4-Fで個別承認された初回Reference基盤には上記限定条件を適用する。Spring Modulith Level 2そのもの、通知・復旧の接続、MyBatis等の対象外開始制限は維持する。
14. Security acceptance fixture、test user、test route、test key、failure switch、browser harnessを正式artifact、`koiki-testing`、Project TemplateまたはFramework Public APIへ自動昇格させない。
15. 実装で確認できる事項は文書上の推測より実装検証を優先し、結果を`docs/architecture/validation/`へ記録する。
16. Ownerの個別承認なしにremote push / PR / merge、ruleset変更、workflow dispatchまたはsnapshot publishを行わない。
17. Repository内の作業を位置づけるときは、`docs/agent/skills/koiki-project-overview/SKILL.md`を読む。
18. 業務機能を設計・実装・レビューするときは、加えて`docs/agent/skills/koiki-business-feature-work/SKILL.md`を読む。

## 検証環境の権限エラーと承認済み手順

Architecture Ownerは、今後のAIセッションで検証実行が権限エラーに妨げられた場合、
承認済みの解決手順をAgent guidanceと関連Skillへ記録することを許可している。
記録前に、実行を許可された作業範囲、失敗した操作とエラー、承認済みの再実行方法、
実際の検証結果を確認する。汎用的な判断手順はここへ、作業固有の条件と結果は
`docs/architecture/validation/`へ記録し、Skillには正本への導線だけを置く。
この記録許可は、新しい権限回避策や対象外の操作を実行する許可ではない。
実行環境が権限付き実行の承認を要求する場合はその手順に従い、拒否された操作を迂回しない。

### ローカルDocker検証

2026-10-13の[R2条件付き限定開始承認](docs/development/phase4-s1-r2-purpose-and-minimal-verification-owner-review-20261013.md#8-owner承認記録と文書固定対象2026-10-13)により、
文書commit・新R2 branchのclean source固定後のpreflightと、成立後の同票のReference packageおよび
`build-support/reference-e2e-verification/`の12件限定検証に必要な使い捨てcontainerの起動・停止を対象に含む。
対象・所有・資源・停止／cleanupは同票に従い、実行環境の権限付き承認手順を維持する。

Architecture Ownerは、Repository内の承認済み検証に必要なローカルDocker診断と
Testcontainers実行について、Docker named pipeへ接続できる権限付き実行を許可している。
この許可には、read-onlyの`docker version` / `docker info` / `docker ps` / `docker images`と、
承認済みTooling fixtureのMaven test / verifyが使う使い捨てcontainerの起動・停止を含む。
現時点の対象はP4-PL2の`build-support/phase4-level2-verification/`である。
2026-10-06の[S1初回正式開始承認](docs/development/phase4-s1-reference-foundation-execution-review-20261006.md#8-source固定preflight条件付き正式開始承認2026-10-06)により、
文書commit・clean source固定後のpreflightと、成立後の`koiki-reference-app`および
`build-support/reference-e2e-verification/`の承認済み隔離DB検証・既存回帰に必要な
使い捨てcontainerの起動・停止も対象に含む。実行環境の権限付き承認手順と、採用済み資源・
cleanup・停止条件に従う。既存P4-PL2許可の流用ではなく、この個別承認に基づく対象追加である。
2026-10-07のscope／TTL限定開始票§8で承認された条件成立後のMaven／隔離Docker検証も、
同票の対象・資源・cleanup・停止条件と実行環境の権限付き承認手順に従う。
2026-10-08の対象・運用証拠ハーネス限定開始票§9で承認された条件成立後のMaven／隔離Docker検証も、
同票の対象・資源・cleanup・停止条件と実行環境の権限付き承認手順に従う。
2026-10-08のB-1読取接続限定開始票§11で承認された条件成立後のMaven／隔離Docker・当該子JVM検証も、
同票の専用schema／role・fixture準備副作用、検証集合・上限・cleanup・停止条件に限定して対象に含む。
文書commit・clean source固定前に実行せず、環境の権限付き承認要求を迂回しない。
2026-10-08のB-2初回issue／read限定開始票§13で承認された条件成立後のMaven／隔離Docker・当該coordinator／
通常子JVM検証も、同票のfixture準備・専用schema／role・writer遮断・有限操作、355件・資源／時間／作業量・
raw／再実行上限・cleanup／停止条件に限定して対象に含む。文書commit・clean source固定前に実行しない。

通常のsandboxで`npipe:////./pipe/docker_engine`への接続を拒否された場合は、
それだけでRancher DesktopまたはDocker Engineの障害と判定しない。
対象作業の承認範囲を確認し、実行環境が提供する権限付き実行・承認手順を使って
最小限の同じ診断または検証を再試行する。Mavenの`~/.m2`書込み拒否も同様に扱う。
権限付き実行で`docker version`のServer応答と検証結果を確認し、Evidenceへ記録する。
実行環境側で権限付き実行を拒否された場合は迂回せず、その理由を報告する。

この記載はOSやAI実行環境の権限設定を変更しない。また、未承認のproduction実装、
任意のcontainer操作、image配布、remote操作、Gate通過を許可するものではない。
検証範囲が変わる場合は対応するOwner判断に従う。

`docs/agent/skills/`をKOIKI固有Skillの正本とする。`.agents/skills/`と`.claude/skills/`は、
各エージェントから正本を発見するための薄い導線とし、設計規則を複製しない。

OpenSpecは、Repositoryに採用済みのchangeが存在する場合に限り、変更固有の要求、設計、タスクの
正本として参照できる。Phase 3の必須tooling、Maven build、CIまたはConsumerの前提にはしない。

## Phase 4 S1 R0-B：Rule 28／29の条件付き限定開始（2026-10-09）

[規約限定改訂票§9.2](docs/development/phase4-s1-r0-rule28-29-level-selection-owner-review-20261009.md)のOwner承認により、
採用済み正本・ADR・承認記録の文書commitとclean source固定、同票preflight成立後に限り、
`koiki-archunit-rules`のmodule単位Level選択API・enum、Rule 28選択対応・Rule 29、当該test／fixtureとEvidenceを作成・検証できる。
旧1引数APIと未指定／Level 0／1の拒否条件を維持し、明示選択したmoduleだけLevel 2の標準listenerを許容する。
同期eventの不存在は検査しない。特定Reference listenerの違反除外を行わない。
code／test最大24 file、新規40 case候補、規約module120 invocation以内・root400以内、累積60分、追加再実行0回とし、
その他の資源・検証集合・停止／cleanupは同票§6〜7に従う。Ownerは同票内の使い捨てDB・今回所有の子JVM／browser・cleanupを委任する。
同日の[接続上限訂正承認（アーカイブ§11）](docs/archive/phase4-s1-r0-20261010/development/phase4-s1-r0-root-connection-limit-correction-owner-review-20261009.md#11-通常rootの接続上限追加1回の限定訂正owner-approved)により、通常rootだけDB client接続上限14、他phaseは8を維持する。
訂正文書4件の追加local commit1回と残56分内のpreflight再開はOwner承認済み。他上限・追加再実行0回・既存test／pool不変更を維持する。
実行環境の権限付き承認手順を維持し、副Agentを使用しない。不足依存取得、scope拡大・上限超・失敗は停止して別判断へ戻す。
Reference code／test／POM／SQL／runtime、他Framework module／依存、Root Reactor／CI／remoteを変更しない。
R0-C結果受入・成果commit・R1開始、DoD・正式受渡し・Phase 4全体は別判断。
文書source固定commitも同票§7.1のOwner別承認／操作とし、条件未成立でcode・検証を先行しない。

同訂正票§5の個別Owner承認によりpreflight上限だけ15分へ訂正する。総60分、既計上810秒・残2790秒、cleanup300秒予約と他条件を維持する。指定文書4件を追加local commit1回で固定し、既実施準備を反復せずsource／環境差分確認後、成立時に承認済み限定実装・初回検証へ進める。

2026-10-10のOwner見直しにより、R0の次作業は[現行最小検証計画](docs/development/phase4-s1-r0-root-connection-limit-correction-owner-review-20261009.md)に従う。監視pure74例・追加網羅・root自動反復を停止し、履歴の許可／残予算を再開許可に流用しない。Rules107成立済みとroot未成立を区別し、S1／B2専用92件は通常buildの分離確認だけとする。Ownerは文書2点の訂正を含むRules16＋文書8＝24ファイルのlocal成果固定commit1回を承認した。成果固定はR0全体の結果受入・R1／remote開始を意味しない。
