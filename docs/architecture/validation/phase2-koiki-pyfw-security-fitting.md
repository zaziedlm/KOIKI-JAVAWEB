# Phase 2 KOIKI-PYFW security fitting inventory

## 1. Status and purpose

- **Inventory date:** 2026年8月31日
- **Source repository:** `C:\Users\kataoka\Desktop\KOIKI-FW\KOIKI-FW-VSCodeProj\koiki-pyfw`
- **Source identity:** branch `dev/v0.8`、HEAD `cdd96d8`、version `0.8.1`
- **Target:** KOIKI-JavaWeb-FW Phase 2 Security Foundation
- **Change type:** read-only source inventory / fitting plan。Python repositoryは変更していない

本記録は、KOIKI-PYFWが提供する認証・認可・token・browser protection・security loggingの能力を、
KOIKI-JavaWeb-FWへ機械移植せず、Spring標準とKOIKIのOwnershipへ割り当て直すためのEvidenceである。
Pythonのclass、endpoint、tableまたはmigrationをJavaのPublic APIやproduction SQLへ直接昇格させない。
React SPA / Next.js BFF / SSOのdeployment profileは`phase2-spa-sso-security-fitting.md`を正本とする。

## 2. Sources inspected

実装を正本として、主に次を確認した。

- `components/libkoiki/src/libkoiki/api/v1/endpoints/auth*.py`、`users.py`
- `components/libkoiki/src/libkoiki/services/auth_service.py`、`login_security_service.py`、
  `password_reset_service.py`、`user_service.py`
- `components/libkoiki/src/libkoiki/core/security.py`、`auth_cookies.py`、`csrf.py`、
  `security_logger.py`、`security_metrics.py`
- User、Role、Permission、Refresh Token、Password Reset、Login Attemptのmodel / repository
- `components/koiki_ref_app/alembic/versions/20260715001_vnext_baseline.py`
- libkoikiのunit testとkoiki_ref_appの認証integration test
- koiki_ref_appのOIDC SSO / SAML実装とtest

## 3. Capability inventory

Python版で確認できた利用者向け能力は次である。

1. email + password login、self registration、current user、logout
2. access JWTとopaque refresh tokenの発行、refresh rotation、revoked token reuse時の全失効、全device失効
3. JWTのBearerまたはHttpOnly Cookieによる認証、Cookie経路だけに対するCSRF検証
4. User / Role / Permissionとpermission authorityによる認可
5. password change、存在有無を返さないreset request、one-time reset token、password policy
6. email / IP単位の失敗記録、閾値lock、段階的遅延、履歴cleanup
7. token / password / PIIを通常logへ出さないsecurity eventとmetrics
8. Reference application固有のOIDC SSO / SAMLと内部identity link

DBにはraw refresh tokenとraw reset tokenではなくSHA-256 hashを保存し、refresh tokenにはexpiry、revoked、
device情報、last-usedを持つ。unit / integration testではrotation、reuse検知、Cookie非露出、CSRF、Bearerとの共存、
permission parity、通常logの機密値非露出を確認している。

## 4. Fitting principles

### 4.1 Functional parity, not implementation parity

| Python implementation | Java fitting decision |
|---|---|
| bcrypt helper | Spring Security `PasswordEncoder` / `DelegatingPasswordEncoder`を使用し、独自hash frameworkを作らない |
| 自前JWT encode / decode | Resource Serverでは`JwtDecoder`とvalidatorを使用する。token発行側は採否決定後にSpring Authorization Serverを第一候補とする |
| JWTをaccess / refresh Cookieへ格納した「session」 | Phase 2 browser loginはHTTP Session + Spring Session JDBCへ置換する。JWT Cookie方式を移植しない |
| FastAPI dependency / decorator | `SecurityFilterChain`、AuthenticationProvider、Method Security、Spring Security eventへ割り当てる |
| 独自CSRF署名token | Spring SecurityのCSRF機構を利用する。Cookie認証とBearer専用pathの境界をchain / matcherで明示する |
| 独自metrics endpoint | Micrometer / Actuatorへ統合し、管理用HTTP APIを先行公開しない |
| SQLAlchemy model / Alembic migration | Java側のOwnership、PostgreSQL / Oracle共通DDL、Flyway規約から再設計する |

### 4.2 Identity decision

利用者が入力する**ユーザーIDはemail addressをlogin identifierとして扱える**。ただしemailは変更され得るため、
Framework内部の主キー、監査actor ID、外部identity link、session principalの永続識別子には使わない。
Python版もinteger `id`とemail / usernameを分離している。Java版は次をCP P2-F2で確定する。

- immutableなFramework user ID
- normalized email login identifierと一意性 / case folding
- login identifier変更時のsession、audit、lock attempt、external identity linkへの影響
- email列をPIIとして扱うretention / masking

`username`を別に持つか、emailだけをlogin identifierとするかはJava要件として決め、Python schemaを理由に増やさない。

## 5. Source-to-target fitting matrix

| KOIKI-PYFW capability | Java target / owner | Phase allocation | Required evidence |
|---|---|---|---|
| default protected route、current user | SecurityFilterChain / Framework、application matcher / Customer | Phase 2 A | unmatched deny、URL / Method双方の401 / 403 |
| email + password authentication | Spring AuthenticationProvider + Framework identity contract | Phase 2 A / B | unknown user timing差の抑制、inactive、lock、success / failure |
| password hash / policy | PasswordEncoder + KOIKI policy configuration | Phase 2 B | raw password非保存・非log、legacy hash migration方針 |
| User / Role / Permission | Framework identity contract / persistence | Phase 2 B | Permission authority、optimistic lock、Referenceは公開contract経由 |
| login attempt / account・IP lock | Spring auth event / handler + KOIKI DB service | Phase 2 B | 2 instance並行閾値、`REQUIRES_NEW`、cleanup、retention |
| password change / reset | Framework credential / reset use case | Phase 2 B | account enumeration防止、one-time hash、expiry、競合、session / credential失効policy |
| browser cookie login | Spring Session JDBC | Phase 2 B | fixation、Secure / HttpOnly / SameSite、CSRF、logout、2 instance |
| OIDC SSO | Session profileはSpring OAuth2 Client、BFF / direct Token profileはResource Server + Framework link Port | Phase 2 A | PKCE / state / nonce責務、issuer + subject link、local login共存。claim mappingはCustomer |
| Bearer access JWT validation | Spring Resource Server | Phase 2 A | signature、`iss`、`aud`、`exp`、`nbf`、scope、Clock |
| access / refresh token issuance | Authorization Server capability | **Phase 2ではfitting / contract gap分析のみ** | provider選定、grant、key lifecycle、client、issuer、audience、consentのOwner判断 |
| rotation / reuse detection / token list / revoke-all | Authorization Server token lifecycle | **実装phaseをOwner判断。Phase 4を含む後続phase候補** | token family、atomic rotation、reuse時失効、hash、cleanup、device privacy |
| Cookie内JWT access / refresh | 移植しない | Phase 2対象外 | Spring Session、Next.js BFF session、Bearer header経路を混同しない |
| security events / safe logging | KOIKI Security Audit + existing observability | Phase 2 B | business rollbackとの対比、機密値 / PII非露出、failure semantics |
| security metrics | Micrometer / Actuator | Phase 2 A / B | low-cardinality tag。email / IPをmetric tagにしない |
| self registration | Reference / Customer policy | Phase 2ではdefault提供しない | invite / admin create / self registrationのOwner判断 |
| SAML | extension | Phase 4 | Spring標準 / library評価、metadata / key / replay / logout |

## 6. Invariants promoted to Java acceptance

次はPythonの実装詳細ではなく、Java側で維持すべき候補要件とする。

1. raw password、access / refresh / reset token、client secret、private keyをDB、通常log、Problem Details、test artifactへ出さない。
2. reset requestはaccount存在有無で外部responseを変えず、unknown userでもtiming side channelを抑える。
3. login失敗・lock・認可拒否のsecurity auditは外側業務transactionのrollbackへ巻き込まれない。
4. Cookie認証のunsafe requestはCSRFを必須とし、Bearer header専用pathへCookie認証をfallbackさせない。
5. logout、password change / reset、account disable時に何を失効させるかを明示し、browser sessionと将来tokenを区別する。
6. email / IP単位の認証試行は複数instanceの共有DBで競合しても閾値を迂回できない。
7. security metricsへemail、IP、user ID等のhigh-cardinality / PII tagを入れない。
8. 外部IdPのissuer + subjectを永続identityへlinkし、email claimだけで同一人物と確定しない。

## 7. Gaps and cautions found in Python source

Python実装をそのまま仕様としない理由として、次を確認した。

- access JWTは対称鍵で`sub` / `exp`を中心に生成し、Java Phase 2の`iss` / `aud` / scope検証契約とは異なる。
- browser「session」はserver-side sessionではなくJWT Cookieであり、Spring Session JDBCのDoD 2-5 / 2-8とは異なる。
- password reset完了時のコメントはrefresh token失効を示すが、実呼出はpassword reset tokenの失効である。
- repository内`commit`とendpoint内`commit`があり、Javaのbusiness / security audit transaction境界へ直訳できない。
- Python migrationはPostgreSQL partial indexやBooleanを使用するため、Oracle共通DDLへ転用できない。
- progressive delayをrequest処理中のsleepで実現している。Javaではthread / connection占有、DoS影響をfixtureで比較する。
- metrics呼出にemail / IPが渡されるため、JavaではcardinalityとPII境界を別途強制する。

これらはPython版の否定ではなく、Java / Springの標準機構と現行Grand Designへ合わせるための差分である。

## 8. Phase 2 fitting work package

production実装より先に次を完了する。

| CP | Work | Exit criteria |
|---:|---|---|
| P2-F1 | capability / invariant inventory | 本記録、source commit、採用 / 非採用 / deferredが全項目で明示される |
| P2-F2 | identity / API semantics fitting | email login identifier、immutable ID、status、authority、error / audit semanticsをOwner reviewできる |
| P2-F3 | Spring replacement test design | Spring component mapping、test topology、threat / negative-path matrix、dependency候補、stop conditionを文書化する |
| P2-F4 | token lifecycle phase decision | Resource ServerとAuthorization Serverを分離し、発行 / refresh / revokeのphase・dependency・Public API非公開境界をOwnerが判断する |
| Gate F | fitting acceptance | matrix、negative tests、threats、deferred backlogが承認され、Gate P2-2 / P2-A1開始可否を判断できる |

P2-F3ではcode / dependencyを変更しない。実証fixtureはGate P2-2後のP2-A1〜A3で非配布Toolingとして作り、
Python endpoint互換API、production migration、公開Java typeを作らない。

## 9. Token lifecycle scope decision required

現行Phase 2 DoD 2-4は**外部から受け取ったBearer JWTをResource Serverとして検証すること**であり、
Authorization Server、token発行、refresh、revokeを含まない。Phase feasibilityもこれらをResource Server scopeへ
混入しないよう明記している。一方、Grand Design §14.6は「Token方式利用時」のrotation / reuse検知を要求し、
KOIKI-PYFWは実装Evidenceを持つ。

したがってPhase 2では、後続実装を阻害しないcontract / table / audit / session失効境界までfittingし、実装phaseを
Gate P2-2で明示決定する。Phase 2で実装まで前倒しする場合は、DoD、依存（Spring Authorization Server候補）、
key / client運用、threat model、工数、CIを追加するGrand Design / ADR変更として扱い、暗黙にscopeを拡大しない。

## 10. Gate conclusion

KOIKI-PYFWとのfittingはPhase 2の前提作業として必要であり、P2-F1〜F4を正式taskへ追加する。
ただし、Python版とのendpoint互換や自前JWTの移植を目標にしない。Phase 2 production実装はGate FとGate P2-2の
Owner承認後に開始する。
