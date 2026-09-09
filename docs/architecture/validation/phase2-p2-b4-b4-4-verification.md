# Phase 2 P2-B4 B4-4 packaged Reference journey verification

## 1. Status and scope

- **Verification date:** 2026年9月9日
- **Work package:** `P2-B4 / B4-4`
- **Status:** `COMPLETE / ARCHITECTURE OWNER APPROVED — B4-5 NOT STARTED`
- **Approved by:** Shuichi Kataoka、2026年9月9日
- **Ownership:** Reference（package済みruntime evidence）/ Tooling（非配布Harness）
- **Baseline:** B4-C1〜C8、B4-2およびB4-3 Architecture Owner承認済み

本記録は、package済みReference JAR、実PostgreSQL、標準Form Loginおよび複数のHTTP Sessionを一続きに外部観測し、
Reference Application Specificationの`AC-P2-01 / 02`とGrand Design DoD 2-10を確認したB4-4の実装証拠である。

B4-4ではReference production code、template、POM、Framework Public APIまたはproduction migrationを変更していない。
新規成果物はTooling-owned verification Harnessとその説明および本検証記録に限定した。

## 2. Verification boundary

### 2.1 Package and runtime assembly

1. Root ReactorからReferenceを除く正式Framework release unit 14 projectを隔離Maven repositoryへstageする。
2. Reference applicationを別Maven invocationでtest後にpackageし、同repositoryから依存を解決する。
3. Harness所有の一時`postgres:17-alpine`をloopbackの動的portで起動する。
4. runtime専用のapp role、Audit table、admin / target / control identity、credentialおよびSOURCE HMAC keyを生成する。
5. 同一のpackage済みReference JARをloopbackの動的portで起動し、HTTP、Cookie、DB、Auditおよびprocess状態を観測する。
6. 正常系と失敗系の終了後、Harness所有のprocess、container、隔離repositoryおよび一時directoryだけを消去する。

PostgreSQL app roleはschema生成後に全object ownershipを`postgres`へ移し、schema CREATEを剥奪してDMLだけを許可した。
Audit production migrationはP2-C1へ明示的にdefer済みのため、`koiki_audit_event`は一時PostgreSQLだけへToolingが作成する。
identity、credential、Audit schema、失敗注入用権限変更はReference artifact、migration、route、configurationまたは固定fixtureへ入れない。

PostgreSQL公式imageの初期化用serverから本serverへの切替中に、一度だけschema setup接続が失敗するraceを観測した。
setup全体を単一transactionのまま有界再試行するようHarnessを補強し、acceptance failureと環境初期化の揺らぎを分離した。

### 2.2 Authorization fixture choice

formal AC-P2-01の対象Roleには`IDENTITY_ADMIN`を使用した。承認済みReference route `/identity/**`が要求する
`IDENTITY:ADMIN`を実際に獲得・喪失させるためであり、B4-3の補助確認で使用した`EXPENSE_READER`の表示確認だけでは
authorization変化の証拠にしない。新しい業務routeまたは検証専用routeは追加していない。

AC-P2-02は、認証済みだが`IDENTITY:ADMIN`を持たないcontrol userから、取得済みの正しいCSRF tokenを付けて
Role assign POSTを直接送信した。したがって403はCSRF拒否ではなくpermission不足によるbackend拒否である。

## 3. Verification results

### 3.1 AC-P2-01 normal journey

| Sequence | HTTP / Session observation | DB / Audit observation |
|---|---|---|
| initial state | adminはprotected GET 200、target / controlは403。targetは独立した旧Sessionを2件保持 | target version 0、`IDENTITY_ADMIN`なし |
| admin assign | POST成功後、target旧Cookie 2件はいずれもloginへredirect。adminは200、controlは403を維持 | version 1、Role 1件、`ASSIGN_ROLE` SUCCESS Audit 1件、target Session 0件 |
| target re-login | 新しいtarget Sessionでprotected GET 200 | login時の最新Permission snapshotを反映 |
| admin revoke | target旧Cookieはloginへredirect。adminは200、controlは403を維持 | version 2、Role 0件、`REVOKE_ROLE` SUCCESS Auditを追加、target Session 0件 |

Auditのactorはadmin immutable user ID、subject / resourceはtarget immutable user IDであり、email形式PIIをidentifierへ使用していない。
Role mutation、optimistic version、Business Auditおよびtarget-only全Session失効が同じpackage済みprocessとDBで成立した。

### 3.2 AC-P2-02 direct protected operation

control userのvalid CSRF付きdirect assign POSTはHTTP 403となった。応答前後でtarget version 0、Role 0件、
mutation Audit 0件、target Session件数不変をDBから確認した。control Sessionも継続し、UI非表示に依存せずbackendで拒否した。

### 3.3 Bounded failure journeys

各失敗系はfresh identity stateと事前確立したadmin / target Sessionで実行した。

| Injected failure | HTTP result | Transaction / Session result |
|---|---:|---|
| `koiki_audit_event` INSERT権限剥奪 | 503、固定message、identifier非露出 | Role 0、version 0、Audit 0へrollback。target Session継続 |
| `koiki_session` DELETE権限剥奪 | 503、固定message、identifier非露出 | Role 0、version 0、Audit 0へrollback。target Session継続 |

各権限は検査直後とfinally cleanupの双方で復旧可能な構成とし、package済みReference processが両失敗後も生存することを確認した。

### 3.4 Tests, artifact and inventory

```text
Reference Maven package tests: 34 / 34 SUCCESS
Formal Framework release staging: Reactor 14 / 14 SUCCESS
Reference artifact in isolated Framework repository: 0
P2-B4 B4-4 packaged journey: SUCCESS
```

| Boundary | Observed result |
|---|---:|
| Framework BOM Reference entry | 0 |
| Identity Public Java types | 10（変更なし） |
| Reference production Java files | 12 |
| Reference templates | 3 |
| Reference controller mappings | 5（GET 3、POST 2） |
| Reference unique route paths | 4（承認済み範囲） |
| Reference migration / SQL | 0 |
| Reference `@Transactional` / Repository declaration | 0 |
| Reference import of Framework `internal` package | 0 |
| deferred dependency（WebFlux / Redis / SAML / AWS / Oracle / HTMX / React） | 0 |
| JAR内Tooling fixture / migration SQL | 0 |
| Maven / process log内runtime secret、Authorization header、Set-Cookie | 0 |

B4-3で同一production baselineのRoot `clean verify` 15 / 15、Surefire 104 / 104、NullAway SUCCESSを確認済みである。
B4-4はToolingと文書だけの追加であり、Root aggregate、NullAway正負fixture、3連続実行はB4-5 closeoutで再実行する。

### 3.5 Owned-resource cleanup

成功時・失敗時とも、Harnessが生成した正確な`koiki-b44-*` container名と
`koiki-reference-b44-*` temporary directoryだけをcleanup対象にした。最終成功後の残存数は次のとおりである。

```text
B4-4 owned Java process: 0
B4-4 owned PostgreSQL container: 0
B4-4 owned temporary directory: 0
```

B4-2 / B4-3の手動確認用に保持している既存DB containerはB4-4の所有物ではなく、本Harnessから変更・削除していない。

## 4. Explicitly deferred work

| Work package | Remaining work |
|---|---|
| B4-5 | P2-B1〜B4 aggregate closeout、Root / NullAway、3連続実行、全Harness inventory、sensitive scan、cleanup、Gate B handoff |
| P2-C1 | Business Audit production migration |

## 5. Architecture Owner review points

1. formal AC-P2-01で`IDENTITY_ADMIN`を使用し、承認済みrouteだけでRole変更前後の実authorizationを証明する判断が妥当か。
2. runtime identity / credential / HMAC keyと一時Audit schemaをToolingだけが生成し、Reference production fixture / migrationへ昇格させない境界が妥当か。
3. AC-P2-02をpermission不足userのvalid CSRF付きdirect POST 403とし、Identity / Audit / Session無変更をDBで確認する証拠が十分か。
4. 正常系のtarget全Session失効とadmin / control継続、およびAudit / Session障害時のtransaction rollbackと既存Session継続がAC-P2-01 / 02とDoD 2-10を満たすか。
5. formal 14 project、Reference別package、artifact / Public API / dependency / source inventory、sensitive-outputおよび所有資源cleanupをB4-4境界とし、aggregate closeoutをB4-5へ残す判断が妥当か。

2026年9月9日、Architecture Ownerは上記5点をすべて承認した。

承認により、package済みReference JARと実PostgreSQLによる`AC-P2-01 / 02`およびDoD 2-10の外部観測は
十分であり、Reference / Framework / Toolingの所有境界も維持されていると判断した。明示された残作業は
B4-5およびP2-C1へ引き継ぐ。
