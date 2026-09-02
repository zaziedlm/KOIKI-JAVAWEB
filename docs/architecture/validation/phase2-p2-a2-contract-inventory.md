# Phase 2 P2-A2 T2 contract inventory

## 1. Status and scope

- **Validation date:** 2026年9月2日
- **Work package:** `P2-A2 / A2-1`
- **Status:** `IMPLEMENTATION CANDIDATE`
- **Ownership:** Framework Security構成 + Tooling-owned非配布T2 fixture
- **Production target:** `koiki-starter-security`
- **Verification target:** `build-support/security-foundation-verification`

本記録は、local Form Login、HTTP Session、URL / Method Security、Role / PermissionをP2-A2で実証する前に、
Spring標準component、production contract、fixture fakeおよびPublic APIの境界を棚卸しした結果である。

## 2. Spring component inventory

| Concern | Selected candidate | Ownership / observable contract |
|---|---|---|
| credential受付 | Form Login / `UsernamePasswordAuthenticationFilter` | application-owned chain。`POST /login`をfilterで処理し、password Controllerを作らない |
| authentication | `DaoAuthenticationProvider` | fixture-owned `UserDetailsService`と`PasswordEncoder`を接続し、独自Providerを作らない |
| password verification | `PasswordEncoderFactories.createDelegatingPasswordEncoder()` | raw passwordまたはhashをproduction artifactへ置かない |
| login identifier | fixture-owned `UserDetailsService` adapter | ASCII emailをtrim + `Locale.ROOT` lowercase。provider alias変換なし |
| session | Spring Security既定のHTTP Sessionとsession fixation protection | login前後のsession ID差と認証後requestを外部観測する |
| URL authorization | `authorizeHttpRequests` / `hasAuthority` | exact Permission authorityだけを許可し、anonymousと権限不足を拒否する |
| Method Security | `@EnableMethodSecurity` / `@PreAuthorize` | Starterが有効化し、Controllerを迂回したUse Case direct invocationも拒否する |
| default boundary | P2-A1 fallback `SecurityFilterChain` | local matcher外は引き続きdenyする |

Spring Filter、Provider、`HttpSecurity`、SecurityContextまたはtest identityをKOIKI Public APIへ露出しない。

## 3. Role / Permission semantics

- RoleはPermissionの集合としてfixture内で展開し、認可判定はPermission authorityで行う。
- allowlistへ完全一致したRoleだけを展開し、unknown、malformed、case違いからPermissionを付与しない。
- Permission文字列、fixture Role名およびrouteはP2-A2 acceptance fixtureだけが所有し、Framework既定へ昇格しない。
- Customer業務Role、resource ownershipおよび業務状態policyはP2-A2へ含めない。

## 4. Identity and failure boundary

- active、disabled、locked、Permission有／無のidentityとcredentialは非配布fixtureだけに置く。
- unknown email、disabled、locked、bad passwordは同じ外部redirect、body、Cookie形状として観測する。
- canonical email重複はfixture構築時に拒否する。
- P2-A2ではidentity persistence、login attempt、audit、reset、lock state mutationを実装しない。これらはP2-B1 / B2が所有する。

## 5. Public API inventory decision

P2-A2のT2実証はSpring標準型とapplication-owned chain、Tooling fixture fakeで成立するため、実装開始時点の候補は次とする。

| Contract category | P2-A1 baseline | P2-A2 candidate |
|---|---:|---:|
| Public Java types | 0 | 0 |
| Public configuration properties | 0 | 0 |
| Public Security error codes | 0 | 0 |
| Customer customization bean names | 1 | 1（変更なし） |

P2-A2実装中にproduction Public APIが必要と判明した場合は実装を拡張せず、型単位contract reviewへ戻る。

## 6. Deferred decisions

- Framework user / principal、identity persistence、migration、attempt lock / reset: P2-B2
- Security auditとfailure transaction: P2-B1
- Spring Session JDBC、複数instance、権限変更後のSession失効: P2-B3
- OIDC、Bearer JWT、profile matcher / property、CORS: P2-A3
- Reference `identity`とCustomer業務policy: P2-B4以降

このinventoryにより、P2-A2はP2-A1のproduction contractを変更せず、既存の非配布HarnessをT2へ拡張して開始できる。
