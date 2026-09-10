# Phase 2 P2-B2 B2-3 Authentication / attempt / lock verification

## 1. Status and scope

- **Verification date:** 2026年9月7日
- **Work package:** `P2-B2 / B2-3`
- **Status:** `COMPLETE / ARCHITECTURE OWNER APPROVED`
- **Approved by:** Shuichi Kataoka、2026年9月7日
- **Ownership:** Framework（Identity authentication / protection）+ Tooling（非配布T4 fixture）
- **Baseline:** B2-C1〜C10承認済み、B2-2 Identity core / migration承認済み

本記録は、local password認証、認証試行、credential lockおよびSecurity Audit連携の実装と検証を記録する。
Identity administration、管理解除、external link mutation、実Session全失効およびlocal password resetは対象外である。

## 2. Implemented boundary

### 2.1 Spring authentication seam

- local password認証は`koiki.identity.local-authentication.enabled=false`を既定とし、利用applicationだけが明示的に有効化する。
- Spring Securityの`DaoAuthenticationProvider`、`UserDetailsService`、`UserDetailsPasswordService`、
  `DelegatingPasswordEncoder`を利用し、独自password hashを実装しない。
- login identifierはinternal canonical emailで検索するが、認証後principalのusername / actorはimmutable `FrameworkUserId`とする。
- passwordはNFC正規化し、設定可能な最大code point数を超える入力を一般化して拒否する。認証後credentialを消去する。
- legacy encoder IDで照合に成功したhashはSpring標準判定により現在の既定encoderへ更新する。

### 2.2 Attempt and lock protection

- known ACTIVE userのACCOUNT試行はimmutable user ID、unknown identifierは保存せずSOURCEだけで集計する。
- PostgreSQL partial unique indexに対応する`INSERT ... ON CONFLICT ... DO UPDATE ... RETURNING`でcounterを原子的に更新する。
- ACCOUNTは既定5回 / 15分で30分lock、SOURCEは既定100回 / 15分で15分blockとする。
- lock期限後は再認証を許可し、成功時にACCOUNT counterと期限切れ`locked_until`をclearする。
- 並行失敗時のcredential更新は条件付きUPDATEとし、lock遷移と`ACCOUNT_LOCKED` Auditを1回だけ成立させる。
- 期限切れattempt rowは判定から除外する。物理cleanupのsingle executionは承認済みどおりP2-B3へ残す。

### 2.3 SOURCE responsibility mode

- `APPLICATION`を既定とし、trusted remote addressをdeployment secretでHMAC-SHA-256化する。raw IPを保存・記録しない。
- `APPLICATION`ではkey IDとBase64 decode後32 byte以上のsecretを必須とし、不足・不正・範囲外設定をstartupで拒否する。
- `EXTERNAL`は、実client sourceを識別してlocal login要求を最初に受ける承認済み公開境界が同等制御を所有する場合だけ使う。
  このmodeではSOURCE rowとfingerprint beanを生成せず、ACCOUNT試行制御は継続する。
- BFF / SSR採用だけでは`EXTERNAL`の根拠にならない。責務移動はCustomer deployment acceptanceで確認する。
- modeはDB schemaを変更せず、ACCOUNT / SOURCEの独立したCHECK制約とpartial unique indexを維持する。

### 2.4 Failure and Audit semantics

- unknown user、bad password、disabled user、locked credential、入力上限超過を`Bad credentials`へ一般化する。
- login failureではattempt / lockを独立transactionで先にcommitする。Security Audit失敗でも拒否と防御状態を取り消さない。
- login成功はACCOUNT状態をclearした後にSecurity Auditを記録し、Audit失敗時はcredentialを消去して認証を成立させない。
- logには固定event IDと内部例外だけを残し、email、password、raw source、HMAC key / fingerprintを含めない。

## 3. Verification result

### 3.1 Compile and static checks

```text
./mvnw -pl koiki-starters/koiki-starter-identity -am clean install -DskipTests
Reactor: 5 / 5 SUCCESS
Identity: 35 source files compiled
Error Prone / NullAway: SUCCESS
```

### 3.2 PostgreSQL individual fixture

```text
./mvnw -f build-support/security-foundation-verification/pom.xml \
  -Dtest=IdentityAuthenticationFixtureTest test
Tests run: 6, Failures: 0, Errors: 0, Skipped: 0
```

| Test | Main observation |
|---|---|
| persistent authentication | Spring `AuthenticationManager`実配線、canonical email検索、stable principal、Permission、credential消去、expired lock解除、legacy hash upgrade、成功Audit |
| generic failure | unknown、bad password、disabled、future lockが同一exception / message |
| concurrent ACCOUNT failures | 8並行失敗でthresholdを迂回せず、lock future、counter欠落なし、lock transition Audit 1件 |
| SOURCE protection | unknown試行を32-byte HMACで集計し、raw address非保持、threshold後の追加処理をblock |
| Audit failure | 成功Audit失敗をgeneric denyし、失敗Auditが失敗してもACCOUNT counterをcommit |
| SOURCE lookup failure | SOURCE遮断DB読取障害を`DEPENDENCY_FAILURE`として監査し、同じgeneric authentication failureへ変換 |

個別fixtureの6 / 6成功は、Spring Bootが`koikiIdentityAuthenticationProvider`をglobal `AuthenticationManager`へ登録し、
B2-3固有経路がSpring Securityの実配線上で成立することを示す。

### 3.3 Auto Configuration boundary

```text
./mvnw -f build-support/security-foundation-verification/pom.xml \
  -Dtest=IdentityAuthenticationAutoConfigurationContextTest test
Tests run: 3, Failures: 0, Errors: 0, Skipped: 0
```

| Test | Main observation |
|---|---|
| default disabled | property未指定ではlocal AuthenticationProvider、attempt store、SOURCE fingerprint beanを生成しない |
| EXTERNAL | HMAC設定なしで起動し、ACCOUNT保護を担うprovider / attempt storeを維持し、SOURCE fingerprint beanを生成しない |
| invalid APPLICATION | key ID欠落または32 byte未満のHMAC keyをstartup failureとする |

### 3.4 Aggregate and root regression

```text
./build-support/security-foundation-verification/verify-p2-b2-identity-core.ps1
Staged release: 13 / 13 SUCCESS
T0-T4: 48 tests, Failures: 0, Errors: 0, Skipped: 0
Configuration metadata: 12 properties / 2 AutoConfiguration imports
Dependency / deferred-artifact boundary: SUCCESS

./mvnw clean verify
Reactor: 13 / 13 SUCCESS
Architecture Contract: 4 / 4 SUCCESS
ArchUnit: 66 / 66 SUCCESS
Error Prone / NullAway: SUCCESS
```

aggregateは隔離Maven repositoryへ正式JARをstageし、B2-3 fixtureだけでなくT0〜T4累積、Public API / property inventory、
依存方向およびdeferred artifact非混入を確認した。root回帰ではrepository全体のbuild・architecture境界に退行がないことを確認した。

### 3.5 Evidence boundary

generic failure fixtureはprovider seamで同一exception / messageを確認する。B2-3はlocal login endpointを所有しないため、HTTP body、
Cookie、redirectおよびtimingの外形同一性は成立claimに含めず、実endpointを構成する後続HTTP統合検証へ残す。

## 4. Findings resolved during verification

1. fixtureが前コミット時点のlocal Maven JARを参照し、認証Auto Configurationを検出しなかったため、正式moduleを
   clean installして実装対象を揃えた。aggregateは隔離repositoryへ毎回stageするため、この取り違えを防ぐ。
2. PostgreSQL JDBCは`Instant` parameterのSQL型を一意に推論できなかったため、internal JDBC境界をUTC `OffsetDateTime`へ
   変換した。domain / JPAの時刻表現は`Instant`のまま維持する。
3. nullableな`blocked_until`の直接scalar mappingを避け、DB上で遮断中かをbooleanへ変換して判定するようにした。
4. failure保護のuser lookupを例外一般化の外に置かず、依存障害でもaccount情報を外部へ露出しないようにした。
5. SOURCE遮断状態の先行DB読取だけが例外一般化の外にあったため、固定event IDでalertし、`DEPENDENCY_FAILURE`監査後に
   generic authentication failureへ変換した。実PostgreSQLでtableを一時退避する障害注入により確認した。

## 5. Review and final acceptance

2026年9月7日、Architecture Ownerは次の設計判断を承認した。

1. local authenticationを明示opt-inとする境界が、初期SSO applicationのIdentity利用と整合するか。
2. SOURCEの`APPLICATION / EXTERNAL`が単なる防御OFFではなく、公開境界への責務移動として十分に明確か。
3. generic failure、atomic counter、lock / automatic unlock、Audit failure semanticsがB2-C4〜C6を満たすか。
4. B2-4へ管理解除、Identity administration、external link mutation、Business Auditを残す境界が妥当か。

設計承認後の証拠補強として、local認証の既定OFF、`EXTERNAL`のbean境界、`APPLICATION`のHMAC設定不備によるstartup failureを
Auto Configuration context testで確認し、SOURCE遮断DB読取障害の一般化も補正した。補正後のT0〜T4 aggregate 48 / 48と
root Reactor 13 / 13も成功した。

2026年9月7日、Architecture Ownerは実装、修正箇所、上記検証証拠および後続作業との境界を確認し、B2-3を最終承認した。
B2-3を`COMPLETE / ARCHITECTURE OWNER APPROVED`とし、次はB2-4 Identity administrationへ進む。
