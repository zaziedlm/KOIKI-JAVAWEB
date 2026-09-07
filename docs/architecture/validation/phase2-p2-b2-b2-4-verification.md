# Phase 2 P2-B2 B2-4 Identity administration verification

## 1. Status and scope

- **Verification date:** 2026年9月7日
- **Work package:** `P2-B2 / B2-4`
- **Status:** `COMPLETE / ARCHITECTURE OWNER APPROVED`
- **Approved by:** Shuichi Kataoka、2026年9月7日
- **Ownership:** Framework（Identity administration）+ Tooling（非配布PostgreSQL fixture）
- **Baseline:** B2-C1〜C10承認済み、B2-2 / B2-3 Architecture Owner承認済み

本記録は、承認済み`IdentityAdministration`をproduction JPA実装へ接続し、user、Role、Permission、local credential、
external identity linkの管理操作とAudit / Session失効semanticsを検証した結果を記録する。

Spring Session JDBC adapter、実Session row削除、旧Cookie再利用拒否、local password resetおよびReference管理画面は対象外である。

## 2. Implemented boundary

### 2.1 Administration activation and actor

- Public API 10型と`IdentityAdministration`のmethod集合はB2-1承認内容から変更しない。
- production beanはJPAに加え、Business / Security Audit recorder、`CompromisedPasswordChecker`および
  `UserSessionInvalidator`がすべて存在する場合だけ構成する。no-op checker / invalidatorは提供しない。
- いずれかが欠ける場合もapplication全体を無条件に起動失敗させるのではなく、`IdentityQuery`を維持して
  `IdentityAdministration`だけを構成しない。P2-B4等の管理機能がこれを必須注入する構成ではstartup時に欠落を検出する。
- Audit actorには認証済み`FrameworkPrincipal`のimmutable `FrameworkUserId`だけを用いる。email、raw password、
  external issuer / subjectをAudit eventへ複製しない。
- HTTP endpointと管理Permission判定はReference `identity`を実装するP2-B4のInbound boundaryに残す。

### 2.2 User and credential administration

- create、email変更、enable、disableを実装し、email canonicalizationとoptimistic user versionを維持する。
- passwordはNFC正規化、15 code points以上、設定可能な最大長以下、必須`CompromisedPasswordChecker`通過後に
  Spring `PasswordEncoder`で符号化する。raw passwordをDB / Audit / logへ保存しない。
- management unlockはlocal credentialのlockとACCOUNT attemptを解除する。credentialがないuserは`NOT_FOUND`とする。
- disableとpassword変更は同期Session失効を必須とし、失効失敗時はIdentity mutationをrollbackする。

### 2.3 Role and Permission administration

- code形式、一意性、FK関係を維持し、Role / Permissionの作成・削除、userへのRole付与・解除、RoleへのPermission付与・解除を実装する。
- 関係が残るRole / Permissionの削除は`CONFLICT`、重複付与は`CONFLICT`、存在しない関係の解除は`NOT_FOUND`とする。
- Role Permission変更Auditは`ROLE_PERMISSION` resourceに両方の安定codeを記録し、変更対象を一意に識別できるようにする。
- user Role変更は対象user、Role Permission変更はそのRoleを持つ全userのSession失効を同期実行する。
- user / Roleのversionを更新し、stale expected versionを`CONCURRENT_MODIFICATION`として拒否する。

### 2.4 External identity administration

- issuer / subjectをopaqueな検証済み値として完全一致で保存し、末尾slash除去等の独自正規化をしない。
- `(issuer, subject)`および`(user, issuer)`の競合を拒否し、disabled userへのlinkを許可しない。
- 管理unlinkは最後の認証手段でも許可する。self-service unlinkは提供しない。
- link / unlinkをBusiness Auditと同じtransactionに置き、unlinkでは対象userの同期Session失効を必須とする。

### 2.5 Audit failure matrix

| Operation | Audit | Failure result |
|---|---|---|
| user / email / enable、Role / Permission、password、external link / unlink | Business / same transaction | mutation rollback |
| account disable | Security / `REQUIRES_NEW` | Audit失敗をalertし、disableとSession失効は継続 |
| management unlock | Security / `REQUIRES_NEW` | unlockとACCOUNT attempt解除をrollback |
| Session失効必須操作 | synchronous `UserSessionInvalidator` | 失効失敗を`DEPENDENCY_FAILURE`へ変換し、mutation rollback |

## 3. Verification result

### 3.1 PostgreSQL administration fixture

```text
IdentityAdministrationFixtureTest
Tests run: 6, Failures: 0, Errors: 0, Skipped: 0
```

| Test | Main observation |
|---|---|
| user lifecycle and audit PII | create、canonical email変更、enable / disable、stable ID / version、Auditへのemail非複製 |
| password policy and invalidation | 最小長、compromised password拒否、encoded password保存、user version、Session失効 |
| Role / Permission propagation | 2 userへのRole付与、Permission反映、全対象user失効、stale version拒否 |
| exact external identity | issuer完全一致、同一user / issuer競合、最後のlinkの管理unlink、Session失効 |
| Business / invalidator failure | Business Audit失敗時のemail rollback、password・User Role・Role Permission・external unlinkの失効失敗時rollback |
| Security Audit failure | management unlockのrollback、account disableの継続とSession失効 |

fixtureは実PostgreSQL、production Flyway migration、production JPA entity、production Audit recorderと
`IdentityAdministration` Auto Configurationを使用する。failure switch、固定actor、checker、Session invalidatorはTooling-ownedであり、
正式JAR、`koiki-testing`またはReferenceへ昇格させない。

### 3.2 Auto Configuration boundary

```text
IdentityAdministrationAutoConfigurationContextTest
Tests run: 2, Failures: 0, Errors: 0, Skipped: 0
```

- checker、Business Audit、Security Audit、Session invalidatorの各依存を1つずつ欠落させ、application contextが起動し、
  `IdentityQuery`を維持しながら`IdentityAdministration`を生成しないことを確認した。
- 4依存がすべて存在する場合は`IdentityQuery`と`IdentityAdministration`を各1 bean生成することを確認した。

これは「依存欠落時にIdentity管理機能を成立させない」境界の証拠であり、Identityを管理用途に使わないapplicationまで
一律に起動失敗させることを意味しない。

### 3.3 Aggregate verification

```text
./build-support/security-foundation-verification/verify-p2-b2-identity-core.ps1
Staged release: 13 / 13 SUCCESS
T0-T4: 56 tests, Failures: 0, Errors: 0, Skipped: 0
Identity Public API: 10 types
Identity properties: 12
Identity Auto Configuration imports: 2
Dependency / deferred-artifact boundary: SUCCESS
```

aggregateは隔離Maven repositoryへ正式release unitをstageし、B2-4の8件を含む13 suite 56件、Public API / property / migration、
production dependency、reset / Spring Session非混入、fixture非配布および一時領域cleanupを検査した。

### 3.4 Root regression

```text
./mvnw clean verify
Reactor: 13 / 13 SUCCESS
Architecture Contract: 4 / 4 SUCCESS
ArchUnit: 66 / 66 SUCCESS
Error Prone / NullAway: SUCCESS
```

## 4. Findings resolved during verification

1. administrationでもlocal認証のpassword policyだけを検証できるよう、HMACを含む認証全設定検証とpassword policy検証を分離した。
2. local認証が無効でも管理password設定にSpring標準encoderが必要なため、base Identity Auto Configurationがencoderを提供する。
   local認証単独context testとの互換性はauthentication側のconditional fallbackで維持する。
3. PostgreSQL JDBCがfixtureの`Instant` parameter型を推論できなかったため、test setup値だけをUTC `OffsetDateTime`として渡した。
   production JPAの時刻表現は`Instant`のまま変更していない。
4. enableとdisableのAudit分類を別経路に固定し、disableだけがSecurity Audit failure時の継続規則を持つ構造とした。
5. Owner reviewで指摘された証拠不足を補い、4依存の有無によるbean境界と、password、User Role、複数userに影響する
   Role Permission、external unlinkのSession失効失敗時rollbackを直接検証した。複数user失効の途中失敗では、Identity変更を
   rollbackしつつ、既に失効したSessionは復元しない安全側の結果とする。

## 5. Evidence boundary and remaining work

- B2-4が証明するSession境界は、失効必須操作が同期SPIを正しいuser IDで呼び、例外時にIdentity transactionをcommitしないことまでである。
- 実Spring Session row削除、2 processからの失効、旧Cookie再利用拒否、logout、cleanup / single executionはP2-B3で検証する。
- issuerはOIDC adapter等の承認済み境界で検証済みであることを前提とする。B2-4は任意入力からのissuer discoveryやallowlist設定を提供しない。
- `IdentityAdministration`を公開するHTTP / MVC境界、Method Security、管理一覧・検索はP2-B4で実需要に合わせて設計する。
- reset token、mail delivery、reset table / endpointは初期SSO適用要件に不要なため、引き続き将来CPへdeferする。

## 6. Architecture Owner review and final acceptance

1. Identity管理beanをchecker / Audit / Session invalidator必須とし、欠落時はapplication全体ではなく管理機能だけを
   成立させない境界が妥当か。
2. disableとmanagement unlockをSecurity protection操作として扱い、disableだけはAudit失敗時にもSession失効と
   disableを継続する判断が§2.5どおりか。
3. password、Role / Permission、external unlinkでSession失効失敗時にmutationをrollbackする判断がB2-C10どおりか。
4. external issuer完全一致、disabled user link拒否、最後の認証手段の管理unlink許可、self-service unlink除外がB2-C8どおりか。
5. B2-4の証拠を同期SPIまでに限定し、実Spring Session効果をP2-B3へ残す境界が過大claimになっていないか。

2026年9月7日、Architecture Ownerは上記5点をreviewし、依存欠落時のbean境界、Security protection操作の分類、
Session失効失敗時rollbackの追加証拠を確認した。B2-4を最終承認し、次はB2-5 regression / evidenceへ進む。
