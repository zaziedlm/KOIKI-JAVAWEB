# Phase 3 P3-B2 MVC / Thymeleaf

## 1. Status and boundary

| Item | Result |
|---|---|
| Date | 2026年9月14日 |
| Architecture Owner | Shuichi Kataoka |
| Phase / CP | Phase 3 Reference Vertical Slice / P3-B2 |
| Status | COMPLETE / OWNER APPROVED |
| Start HEAD | `998aaa1`（P3-B1 COMPLETE） |
| Ownership | Framework Web MVC Starter、Reference MVC / View、Architecture Evidence |
| Target Maven module | `koiki-starter-web-mvc`、`koiki-reference-app` |

P3-B2は、Spring MVC / Thymeleafによるfull-page HTMLを主軸として、P3-B1までのmaster / expense
Application Use Caseとread modelを実際の画面から利用できることを実証する。HTMX interaction、browser runner、
2 Session競合、cache / TTLおよびRESTは後続CPへ残し、本CPで先行しない。

本書を実装・自動検証・最初の人系checkpointの最終Evidenceとする。Architecture Ownerのclose判断を
§10へ記録し、P3-B2を`COMPLETE / OWNER APPROVED`、P3-B3を`READY`とする。

## 2. Implemented boundary

| Area | Result |
|---|---|
| Framework artifact | `koiki-starter-web-mvc`をReactor / BOM管理対象として新設 |
| Starter responsibility | MVC / Thymeleaf / Validation、共有CSS、共通Thymeleaf fragment、HTMX 2.0.10 WebJarを同梱 |
| Java API | 外部向けPublic API 0型。auto-configurationは`internal` packageに限定 |
| Resource contract | `/koiki-web/**`でStarter所有resourceを配信 |
| Reference home | full-page HTMLの入口と業務画面へのnavigation |
| master MVC | 一覧、検索、paging、登録、名称変更、無効化、Validation、sanitized error |
| expense MVC | 自分の申請一覧、詳細、draft作成、提出と既存workflow操作、Validation、sanitized error |
| View boundary | Controller Form / View用値とApplication DTOを使用し、Domain Model / JPA Entityを露出しない |
| Security | Phase 2のlogin、default deny、Permission、CSRF、header、Sessionを再利用 |
| HTML主軸 | すべての対象画面を通常のGET / POSTとredirectで操作可能にした |

expense作成画面は、Domain aggregateの複数明細能力を弱めず、P3-B2の最小UIとして1明細入力に限定した。
CSSの意匠完成度は本CPの受入対象外とし、操作・境界・検証可能性を優先した。

## 3. MVC / Thymeleaf / HTMX boundary

- Thymeleafによるfull-page HTMLを通常経路とし、JavaScriptを画面成立の前提にしない。
- P3-B2 production templateに`hx-*`属性は0件であり、HTMX interactionを実装していない。
- HTMX WebJarはP3-B0承認どおりStarterへlocal同梱したが、選択的interactionと11契約の実証はP3-B3で行う。
- MVC Controller、Form、View表現と、後続REST Controller / DTOは共有しない。
- 共通navigationは常時表示だが、各backend routeの認証・認可が最終境界であり、表示有無を認可判断にしない。

## 4. Automated verification

| Check | Result | Evidence |
|---|---|---|
| Starter isolated build | PASS | `mvnw -pl koiki-starters/koiki-starter-web-mvc clean test` |
| Root Reactor | PASS | Docker接続可能な境界で`mvnw clean verify` |
| Reactor inventory | PASS | 16 projects、13 JAR projects |
| Test total | PASS | Surefire 189 tests、failure / error / skip 0 |
| Reference regression | PASS | 73 tests、failure / error / skip 0 |
| MVC / Security | PASS | `ReferenceBusinessUrlSecurityTest` 11 tests |
| Shared resource | PASS | 未認証GET `/koiki-web/koiki.css`が200、`text/css`、content markerを返す |
| Package inventory | PASS | Starter JARにauto-configuration、CSS、fragment、metadataを収録 |
| Public API inventory | PASS | 外部向けJava Public API 0型、実装classは`internal` package |
| PostgreSQL | PASS | Testcontainers PostgreSQL 17.11、Framework 3 migration、Reference V1〜V3 |
| HTMX interaction | NOT APPLICABLE | `hx-*` 0件。P3-B3へdefer |
| Migration / workflow | UNCHANGED | P3-B2差分0 |

最初のsandbox内Root実行ではDocker named pipeへ接続できず、PostgreSQL統合test 13件がContext初期化前に
停止した。同じsourceをDocker接続可能な実行境界で再実行し、Testcontainers、migration、全test、packageまで
成功した。この初回停止をproduction defectまたはtest skipとして扱わない。

## 5. Accepted test adjustment

### 5.1 Observed problem

Starter内部auto-configurationをmodule-local testから直接検証する当初案では、Windows上の通常Maven
`testCompile`時に当該moduleのmain class outputが参照不能となり、Root ReactorがStarterで停止した。
test packageを`internal`外へ移す限定案も実行したが同じ結果となり、仮説を棄却して完全に戻した。

### 5.2 Architecture Owner-approved alternative

module-local testを除き、実consumerであるReferenceの`@WebMvcTest`から公開resource contractをHTTPで検証する
案を採用した。追加testは未認証で`/koiki-web/koiki.css`を取得し、status、Content-TypeとCSS content markerを
確認する。これにより内部classへtestを結合せず、Starterのauto-configurationとresource配信がconsumer上で
成立する観測可能な結果を検証する。

この対応ではcompiler設定、production class、Framework Public API、dependency、migrationまたはworkflowを
回避目的で変更していない。通常Root Reactor `clean verify`が成功したため、当初のbuild blockerは解消済みである。

なお、`-Dtest=ReferenceBusinessUrlSecurityTest -am`を全upstream moduleへ適用したfocused実行は、別Starterの
main classをtest runtimeで解決できずContext起動前に停止した。採用案の受入結果は、module単体buildと通常Root
Reactorのclean実行を正本とし、この特殊なreactor-wide test selectorを標準手順または回避設定にはしない。

## 6. Human browser checkpoint

| Check | Observed result | Assessment |
|---|---|---|
| Identity user lookup | user ID、email、status、version、Role、Permissionを表示 | PASS |
| Existing expense detail | scoped requestの詳細とworkflow操作を表示 | PASS |
| New expense form | 入力とValidation errorを表示 | PASS |
| Visual design | CSS完成度は本CPでは評価対象外 | OUT OF SCOPE |
| Browser / version | Google Chrome 152.0.7977.84（公式ビルド、64ビット） | PASS |

Identity画面ではuser `b2000000-0000-0000-0000-000000000001`、email
`p3b2-user@example.test`、Role `P3B2_REVIEWER`と5 Permissionの表示を確認した。Architecture Ownerは
新規申請画面のValidation表示に問題がないことを確認した。

browser version画面の確認時点ではChrome更新が50%進行中であったため、上記は人系checkpointを実施した
browserの確認時versionとして記録する。将来のbrowser runnerまたは固定baselineのversion指定ではない。

既存申請詳細で申請者をemail表示することは機能上成立した一方、業務画面として不自然との所見を得た。
Identity v0.1に氏名属性がない事実をP3-B2の非blocking Evidenceとし、氏名をFramework Identityへ直ちに追加しない。
従業員profile等の業務所有を含む属性・Ownership判断を別の明示的reviewへ送る。

## 7. HTTP / log / Audit / DB correlation

package済み実行可能JAR、local PostgreSQL、実HTTP Sessionを使用し、login、CSS取得、expense form、
Validation、draft作成、提出を確認した。確認用request IDは
`2b82cfb9-50c7-43a4-aca3-549c273bc729`である。

| Evidence | Result |
|---|---|
| HTTP / Session | login後に対象画面へ到達し、CSRF付きPOSTとredirectが成立 |
| Application log | Controller mapping、HTTP 200、SQL bind placeholder、Spring Session動作を確認 |
| Expense DB | `SUBMITTED`、claimed amount `1000`、version `2` |
| Line DB | 1 line、amount `1000` |
| Business Audit | `EXPENSE_WORKFLOW / SUBMIT_EXPENSE / SUCCESS` |
| Migration history | Framework 3、Reference baseline 0 + V1〜V3 |
| Sensitive data | test credentialをRepositoryへ保存せず、raw passwordのEvidence記録なし |

確認後はapplication processを停止し、一時PostgreSQL containerとtest dataを破棄した。

## 8. Local run guide

`docs/reference/KOIKI-JavaWeb-FW_Reference_Application_Local_Run_Guide_v0.1.md`に、実行可能Spring Boot JAR、
local PostgreSQL、必要環境変数、起動・停止、production seedを置かない境界、Rancher Desktop / port forwardingの
切り分けを記録した。将来のapp / DB container環境整備を妨げず、現段階の手動再現経路を明文化する。

## 9. Deferred and owner review points

- P3-B3: 効果を説明できる箇所だけのHTMX、11契約、CSRF、非配布Playwright / Chromium journey。
- P3-B4: 2 BrowserContextの楽観lock競合画面、承認済みcache / TTL契約。
- Gate B: B1〜B4横断の自動browser、Owner実演、accessibility、log / Audit / DB突合。
- P3-C0以降: 最小REST API contract。
- 別review: applicant氏名等の業務属性とIdentity / employee-profile Ownership。
- 後続環境整備: app container、DB初期化・data投入を含む再現可能なlocal stack。

Architecture Ownerはclose reviewで次を判断した。

1. full-page Thymeleafを主軸とする実装と、P3-B3へのHTMX defer境界を受け入れるか。
2. Starterの正式artifact化、Public API 0型、resource contractとdependency構成を受け入れるか。
3. 自動test 189件、実browser操作、log / Audit / DB突合をP3-B2 Evidenceとして受け入れるか。
4. module-local内部実装testをconsumerからの公開挙動testへ置換した判断を受け入れるか。
5. applicant email表示を非blocking Evidenceとし、Identity属性変更を別reviewへ送るか。
6. browser名 / versionを含むEvidenceの完成を確認し、P3-B2をcloseしてP3-B3を開始可能とするか。

## 10. Architecture Owner decision

| ID | Decision | Status |
|---|---|---|
| P3-B2-D1 | full-page ThymeleafをUIの主軸とし、HTMX interactionとbrowser runnerをP3-B3へ分離する | APPROVED（2026年9月14日） |
| P3-B2-D2 | Web MVC Starterの正式artifact、resource contract、初期Java Public API 0型とdependency構成を受け入れる | APPROVED（2026年9月14日） |
| P3-B2-D3 | 自動test 189件、実browser操作、log / Audit / DB突合をP3-B2 Evidenceとして受け入れる | APPROVED（2026年9月14日） |
| P3-B2-D4 | module-local内部実装testをconsumerからの公開挙動testへ置換した判断を受け入れる | APPROVED（2026年9月14日） |
| P3-B2-D5 | applicant email表示の不自然さを非blocking Evidenceとし、属性Ownershipを別reviewへ送る | APPROVED（2026年9月14日） |
| P3-B2-D6 | browser名 / versionを含むEvidenceを完成とし、P3-B2をcloseしてP3-B3を開始可能とする | APPROVED（2026年9月14日） |

**Decision:** APPROVED — P3-B2 COMPLETE / P3-B3 READY  
**Decided by:** Shuichi Kataoka, Architecture Owner  
**Decision date:** 2026年9月14日

Architecture OwnerはP3-B2-D1〜D6を一体として承認した。full-page ThymeleafをUIの主軸とし、
HTMX interactionとbrowser runnerはP3-B3へ分離する。Web MVC Starterのartifact、resource contract、
初期Java Public API 0型、自動test、人系browser checkpoint、log / Audit / DB突合および承認済み代替testを
P3-B2 Evidenceとして受け入れる。申請者email表示の不自然さは非blocking Evidenceとし、Identityまたは
業務profileの属性Ownershipを別reviewへ送る。

この承認によりP3-B2を`COMPLETE / OWNER APPROVED`、次に開始できるproduction CPを`P3-B3`とする。
P3-B4、Gate B、P3-C0以降、workflow、required checkまたはremote操作を先行しない。
