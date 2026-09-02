# Phase 2 P2-A2 T2 local session and authorization verification

## 1. Status and scope

- **Validation date:** 2026年9月2日
- **Work package:** `P2-A2 / A2-2〜A2-4`
- **Status:** `COMPLETE / COMMIT READY`
- **Branch:** `feature/phase2-security-foundation`
- **Baseline HEAD:** `e87c270`
- **Java / Maven:** Java 21.0.12.1 / Maven 3.9.16

本記録は、P2-A1のdefault deny、CSRF / Security Header既定およびPublic API境界を維持したまま、
Spring標準Form Login、HTTP Session、URL / Method Security、Role / PermissionをT2 fixtureで実証したEvidenceである。

## 2. Implemented boundary

### 2.1 Framework production scope

- 既存のServlet Security Auto ConfigurationでSpring標準Method Securityを有効化した。
- P2-A1のfallback `SecurityFilterChain`、CSRF、Security Header、401 / 403およびCustomer chain合成を変更していない。
- production dependency、Java型、configuration property、Security error codeまたはcustomization beanを追加していない。

### 2.2 Tooling fixture scope

- Springが構成する`DaoAuthenticationProvider`、fixture-owned `UserDetailsService`、delegating `PasswordEncoder`を使用した。
- `POST /login`をForm Login filterに処理させ、独自password Controllerまたはtoken APIを作成していない。
- ASCII emailをtrim + `Locale.ROOT` lowercaseでcanonicalizeし、canonical重複をfixture構築時に拒否した。
- active、disabled、locked、Permission有／無の固定identityを非配布fixtureだけに置いた。
- allowlistへ完全一致したRoleだけをPermission集合へ展開し、認可判定はPermission authorityで行った。
- application-owned URL chainとStarter-enabled Method Securityを併用した。

## 3. T2 observable results

| Requirement / path | Observable result |
|---|---|
| correct email + password | 前後空白とcase違いをcanonicalizeし、login成功後に保護処理へ到達 |
| session fixation | login前後でsession IDが変わり、変更後sessionでprincipalを維持 |
| generic login failure | unknown email、disabled、locked、bad passwordが同じstatus、redirect、body、Cookie形状 |
| CSRF / Header | CSRFなし`POST /login`は403。`nosniff`と`X-Frame-Options: DENY`を維持 |
| URL anonymous | browser profileとして`/login`へredirectし、保護処理は未実行 |
| URL insufficient Permission | 403となり、保護処理は未実行 |
| Method anonymous | `AuthenticationCredentialsNotFoundException`で拒否し、保護処理は未実行 |
| Method insufficient Permission | `AccessDeniedException`で拒否し、保護処理は未実行 |
| exact Permission | 完全一致する`fixture:read`だけ許可 |
| unknown / malformed / case違い | `fixture:unknown`、`fixture:read:extra`、`FIXTURE:READ`はいずれも拒否 |

未認証Method Securityの具体例外型はSpring Security 7.1.1の実測結果である。URL側のbrowser redirectとともに、
DoD 2-1の「拒否」を満たし、外部responseへ必要Permissionまたは内部reasonを露出しない。

## 4. Verification results

| Verification | Result |
|---|---|
| P2-A2 aggregate | isolated release 11 / 11、dependency / artifact / Public API / sensitive-content検査success |
| cumulative Security fixture | T0 / T1 / T2 16 tests、failure / error / skip 0 |
| T2 fixture | 6 tests、failure / error / skip 0 |
| root `verify` | 11 / 11 projects `BUILD SUCCESS` |
| root tests | Architecture Contract 4件 + ArchUnit Rules 66件、計70件成功 |
| Null Safety | NullAway positive成功、negative diagnostic検出、restore成功 |
| Public API fixture | package-private互換成功、return type破壊と未承認追加を期待failureとして検出 |
| Diff hygiene | `git diff --check` success |

最初のsandbox内root verifyはlocal Maven repositoryへの追跡ファイル作成をOSに拒否されて停止した。
同一commandを通常のlocal Maven repository書込み条件で再実行し、11 / 11成功したためsource failureではない。

## 5. Public API and dependency review

P2-A1 Public API inventoryは完全一致を維持した。

| Contract category | P2-A2 result |
|---|---:|
| Public Java types | 0 |
| Public configuration properties | 0 |
| Public Security error codes | 0 |
| Customer customization bean names | 1（変更なし） |

production dependencyはBoot-managed `spring-boot-starter-security`だけであり、OAuth2 Client / Resource Server、
Authorization Server、SAML、Spring Session、Redis、WebFluxまたはAWS固有dependencyを追加していない。

## 6. DoD and threat trace

| Trace | P2-A2 Evidence |
|---|---|
| DoD 2-1 | anonymous requestとUse Case direct invocationの双方を拒否 |
| DoD 2-2 | Permission不足のURL requestとController迂回direct invocationを拒否し、処理未実行を確認 |
| DoD 2-9 | login追加後もCSRFとSecurity Header既定を維持 |
| N-01 | canonical login、canonical重複拒否、unknown / disabled / locked / bad passwordのgeneric failure |
| N-05 | RoleからPermissionを展開し、unknown / malformed / case違いauthorityを拒否 |
| N-06 | URL / Method双方のpositive / negative pathと処理未実行を確認 |

N-01のaudit failure path、永続unique constraintおよびlock mutationはOwning CPであるP2-B1 / B2へ延期する。
P2-A2はDBなしT2で観測可能な範囲を完了し、後続CPの成立を偽装しない。

## 7. Excluded scope confirmation

次のproduction差分は0件である。

- User / Role / Permission / credentialの永続化、migration、attempt lock、reset
- Security audit persistenceとtransaction制御
- Spring Session JDBC、複数instance、権限変更後の全Session失効
- OIDC Login、Bearer JWT、CORS profile、SPA / BFF / Edge Authentication
- Reference `identity`、Customer業務route / policy、Framework `/me`
- test identity、credential、Permission文字列またはrouteの正式artifact / `koiki-testing`昇格

## 8. Conclusion

P2-A2はSpring標準componentと最小のFramework構成でDoD 2-1、2-2、2-9を満たし、P2-A1 contractと
後続CP境界を維持した。P2-A2差分としてcommit可能であり、P2-A3は別CPとして開始する。
