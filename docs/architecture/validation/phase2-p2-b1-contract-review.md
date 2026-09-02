# Phase 2 P2-B1 Audit contract review

## 1. Status and scope

- **Review date:** 2026年9月2日
- **Work package:** `P2-B1 / B1-1`
- **Status:** `COMPLETE / ARCHITECTURE OWNER APPROVED — B1-2 READY`
- **Approved by:** Shuichi Kataoka、2026年9月2日
- **Ownership:** Framework（Audit contract / internal persistence）+ Tooling（T4 PostgreSQL fixture）
- **Production change at approval:** 0。B1-2より前にPublic API、artifact、migrationを追加していない

本書は、グランドデザイン§15のBusiness audit / Security audit transaction semanticsを、P2-B1で初めて
production contractへ具体化するための型単位reviewである。Application logとAudit logの2分類を維持し、
CloudWatch、ファイル出力、通知または非同期外部連携をAudit transactionへ含めない。

## 2. Existing constraints and observations

| Concern | Existing fact / constraint |
|---|---|
| Business audit | 業務transactionと同一。Audit書込み失敗時は業務処理もrollbackする |
| Security audit | `REQUIRES_NEW`。外側transactionのrollbackに巻き込まれない |
| Side effect | Auditではない。Spring Modulith Level 2、通知、外部exportはP2-B1対象外 |
| Persistence | PostgreSQLを第一標準とし、`koiki_audit_event`はFrameworkが所有する |
| Migration | production MigrationはP2-C1。P2-B1はfixture-owned schemaでtransactionを実証する |
| Existing artifacts | Audit runtime artifact / APIは0。`koiki-framework`はREADMEだけで空moduleを生成していない |
| Security Starter | Public Java型0、data dependencyなし。Business auditをここへ混在させない |
| Observability | console structured loggingは成立済み。cloud backendと業務log abstractionを所有しない |
| Identity | actorはimmutable Framework user IDを使い、email / external subject / display nameを使わない。IDのproduction型と物理列型は未確定 |

## 3. Artifact placement comparison

| Candidate | Shape | Evaluation |
|---|---|---|
| **A — single Audit Starter** | `koiki-starters/koiki-starter-audit`にPublic API、internal Auto Configuration、transaction実装、persistenceを置く | **Recommended.** P2-B1でAPIと実装を同時に利用し、空artifactを作らない。SecurityやObservabilityへBusiness auditを混在させない |
| B — Security Starterへ追加 | `koiki-starter-security`へAudit API / persistenceを追加 | Reject。Business auditのOwnershipがSecurityへ入り、data dependencyとmigration責務を既存Security基盤へ混在させる |
| C — `-api` / `-impl`分割 | `koiki-audit-api`とimplementation / starterを分離 | Defer。重要SPIとして分割する実需要がなく、初期Public API 6型に対してartifactとversion境界が過剰になる |
| D — generic Framework runtime | `koiki-framework`配下に複数横断機能をまとめる | Reject for P2-B1。Audit以外の未使用機能を持つgeneric runtimeを先行生成する理由がない |

Candidate Aは単一の配布leaf artifactとする。初期dependency候補はJSpecifyとBoot管理下のJPA / transaction機構に限定し、
Security Starter、Observability Starter、Spring Modulith、cloud SDKへ依存しない。正確なdependency treeはB1-2開始時に
focused fixtureで検証する。

## 4. Entry point comparison

| Candidate | Public shape | Evaluation |
|---|---|---|
| **A — two recorder interfaces** | `BusinessAuditRecorder.record(AuditEvent)` / `SecurityAuditRecorder.record(AuditEvent)` | **Recommended.** 呼出時にtransaction分類が型で明示され、classification enumの値間違いを防ぐ |
| B — one interface / two methods | `AuditRecorder.recordBusiness(...)` / `recordSecurity(...)` | Viableだが、単一実装でself-invocationを作りやすく、bean単位のtransaction責務が曖昧になる |
| C — one method + classification | `record(AuditClassification, ...)` | Reject。runtime値だけで`REQUIRED` / `REQUIRES_NEW`を切り替える設計となり、誤分類と分岐漏れを招く |
| D — Domain Event | Application eventをpublishしてlistenerが保存 | Reject。グランドデザイン§15.2の直接／同期Audit APIと、業務監査を非同期にしない方針に反する |

Public interfaceへSpringの`@Transactional`、`Propagation`、`TransactionTemplate`またはJPA型を露出しない。

## 5. Immutable Audit value comparison

| Candidate | Shape | Evaluation |
|---|---|---|
| **A — immutable value + copy-on-write optional methods** | 必須4項目をfactoryで受け、`withSubject`等は新しいinstanceを返す | **Recommended.** builder型を増やさず、将来optional項目をbinary-compatibleに追加できる |
| B — Java recordの全項目constructor | §15.4候補をrecord componentへ列挙 | Reject。引数が多く、optional項目追加がconstructor / record componentの破壊変更になる |
| C — mutable builder / bean | setterで任意項目を設定 | Reject。record呼出後の変更、再利用、並行実行時の状態混入を許す |
| D — `Map<String, Object>` details | 任意key-valueを受ける | Reject。秘密値、PII、型不整合、schema不整合をPublic APIで許容する |

推奨する概念上のAPIは次のとおりである。これは承認対象の型とmethodを示す疑似signatureであり、まだsourceではない。

```java
public interface BusinessAuditRecorder {
    void record(AuditEvent event);
}

public interface SecurityAuditRecorder {
    void record(AuditEvent event);
}

public final class AuditEvent {
    public static AuditEvent of(
            String eventType, AuditActor actor, String action, AuditResult result);

    public AuditEvent withSubject(String subjectId);
    public AuditEvent withResource(String resourceType, String resourceId);
    public AuditEvent withReason(String reasonCode);
}

public final class AuditActor {
    public static AuditActor user(String immutableFrameworkUserId);
    public static AuditActor system(String systemId);
    public static AuditActor anonymous();
}

public enum AuditResult {
    SUCCESS,
    FAILURE
}

public final class AuditRecordingException extends RuntimeException {
    // Safe fixed-category message; event payload and persistence detail are not exposed.
}
```

`AuditEvent`はactor、event type、action、resultだけをP2-B1の必須入力とする。subject、resource、reasonはoptionalとし、
before / after要約、client IP、User-Agentは利用経路とprivacy / retention方針を確認するOwning CPまでPublic methodを追加しない。
`occurredAt`、request / trace correlationおよび永続event IDはcaller入力にせず、internal implementationが信頼できる
Clock / contextから付与する。

## 6. Actor boundary

`AuditActor.user(String)`の値は、認証済みFramework principalから取得したimmutable Framework user IDの
opaque表現に限定する。Stringを選ぶ理由は、P2-B1でUser entity、採番方式またはPostgreSQL物理列型を先行固定せず、
Audit contractをDB vendorとidentity persistenceから分離するためである。

- email、OIDC subject、Cognito username、employee number、display nameまたはrequest parameterをuser actor IDにしない。
- authentication前のlogin失敗は`anonymous()`とし、raw login identifierをactor / subjectへ設定しない。
- framework-owned background operationだけが`system(systemId)`を利用する。外部入力をsystem IDへ渡さない。
- Public APIは任意の外部入力がtrusted actorであることを証明できないため、B2 / B4 integration testでprincipalからの導出を検証する。
- `toString()`、exception messageまたはvalidation errorへactor IDとevent payloadを含めない。

将来`FrameworkUserId` Public valueを導入する場合は、`AuditActor.user(FrameworkUserId)`のadditive overloadを別途reviewする。
P2-B1でIdentity APIを同時生成しない。

## 7. Transaction implementation

推奨するinternal構成は、public recorder interfaceを実装するfaçade、その内側で別Spring beanとなる2つの
transaction executor、および共有のpackage-private storeである。

```text
Application Use Case
  -> BusinessAuditRecorder façade
       -> BusinessAuditTransaction proxy [MANDATORY]
            -> internal AuditStore -> EntityManager.persist + flush

Security flow / caller
  -> SecurityAuditRecorder façade
       -> SecurityAuditTransaction proxy [REQUIRES_NEW]
            -> internal AuditStore -> EntityManager.persist + flush
```

- Business implementationは`MANDATORY`で既存transactionへの参加を要求し、Audit単独commitを防ぐ。
- Security implementationは`REQUIRES_NEW`で外側transactionをsuspendし、独立commitする。
- 2 transaction executorをfaçadeとは別beanにしてself-invocationによるproxy bypassを避ける。
- transaction annotationはinternal transaction executor methodにだけ置く。
- `persist`後に明示flushし、constraint / write failureをtransaction内で顕在化させる。
- façadeはtransaction proxyから返るflush / commit failureをsafeな`AuditRecordingException`へ変換して再throwし、
  元event payloadをmessageへ含めない。
- Business側でcallerが例外をcatchしても外側transactionをcommitできないことを実PostgreSQLで確認する。
- Security側はcallerが例外をcatchし、承認済みO-4に従いfail closedまたは防御操作継続を選択できる。
- `AuditRecordingException`を返り値へ置換しない。戻り値の確認漏れによる監査欠損とBusiness transaction commitを防ぐ。

具体的なJPA Entity、Repository / EntityManager利用、transaction manager選択、schema初期化方法はinternal implementationであり、
Public APIにしない。P2-B1 fixtureはproduction Migrationを追加せず、test-owned schemaで`koiki_audit_event`相当rowを観測する。

### 7.1 Persistence implementation comparison

| Candidate | Shape | Evaluation |
|---|---|---|
| **A — JPA `EntityManager`** | internal Entityを`persist + flush`し、Spring transactionへ参加させる | **Recommended.** 更新系の既定をJPAとするグランドデザインに従い、Public Repositoryを増やさず最小実装にできる |
| B — Spring Data JPA Repository | internal Repositoryから`save + flush`する | Viableだが、単純なappend 1操作にRepository interfaceとscan設定を追加する利点が小さい |
| C — `JdbcTemplate` / SQL insert | internal SQLで直接insertする | Defer。AuditだけをJPA更新既定の例外とし、production Migration前に列名とSQLを実装へ固定する理由がない |
| D — async writer / external backend | queueまたはlog backendへ送信する | Reject。transaction rollback対比とDB正本の要件を満たさない |

P2-B1はApplicationとAuditが同じprimary DataSource / transaction managerを利用する構成だけをclaimする。複数DataSource、
複数transaction manager、XAまたは別Audit DBは要件がないため、qualifier / routing SPIをPublic APIへ追加しない。

## 8. Application log boundary

- Application logとAudit logは別責務とし、Audit成立はDB rowとtransaction結果で判定する。
- Security関連事象はApplication log、Audit logまたは双方へ現れ得るが、同一payloadを機械copyしない。
- `AuditRecordingException`の運用記録は安全な固定categoryとcorrelationだけをApplication logへ出し、Audit payloadを含めない。
- log collector、ファイル、CloudWatch等の外部backend障害をAudit transactionへ参加させない。
- Audit DBから外部backendへの完全配送、after-commit投影、outbox / CDCはP2-B1対象外とする。

## 9. Public API inventory delta

推奨案の初期inventory増分は次の6型である。

| Type | Kind | Purpose |
|---|---|---|
| `org.koikifw.audit.BusinessAuditRecorder` | interface | same-transaction記録入口 |
| `org.koikifw.audit.SecurityAuditRecorder` | interface | independent-transaction記録入口 |
| `org.koikifw.audit.AuditEvent` | final immutable class | callerが指定する監査内容 |
| `org.koikifw.audit.AuditActor` | final immutable class | user / system / anonymous actor |
| `org.koikifw.audit.AuditResult` | enum | `SUCCESS` / `FAILURE` |
| `org.koikifw.audit.AuditRecordingException` | final runtime exception | callerが識別可能な安全な保存失敗 |

- Public configuration property: 0
- Public error-code string: 0
- Public Spring / JPA / transaction type: 0
- Public persistence Entity / Repository: 0
- Public Application-log API: 0

## 10. T4 evidence required before acceptance

1. Business changeとBusiness auditのcommitで両rowが残る。
2. 外側rollbackでBusiness changeとBusiness auditが共に消える。
3. Business audit保存失敗をcallerがcatchしてもBusiness changeをcommitできない。
4. Security audit commit後に外側transactionをrollbackしてもSecurity audit rowだけが残る。
5. Security audit保存失敗を`AuditRecordingException`として識別し、fail-closedとcontinue + alert fixtureを分けられる。
6. Business recorderをtransaction外から呼ぶと保存せず失敗する。
7. self-invocationなしで実際のSpring proxyと実PostgreSQL transactionが機能する。
8. raw email、external subject、password、token、secret、claim全文がrow、log、exception、report、artifactへ露出しない。
9. Public API inventoryが承認済み6型と一致し、internal JPA / transaction型が外部へ露出しない。
10. root verify、Null Safety、ArchUnit、T0〜T4 aggregateとcleanupが成功する。

## 11. Decisions requested

| ID | Decision | Approved choice | Alternative / impact | Status |
|---|---|---|---|---|
| B1-C1 | Artifact placement | 単一`koiki-starter-audit` | Security混在、または初期`-api` / `-impl`分割 | **APPROVED** |
| B1-C2 | Entry point | `BusinessAuditRecorder` / `SecurityAuditRecorder`の2 interface | 単一interfaceではclassification誤りとproxy bypass riskが増える | **APPROVED** |
| B1-C3 | Immutable value | `AuditEvent` factory + copy-on-write optional methods、`AuditActor` factory | wide record constructor、mutable builder、Mapは避ける | **APPROVED** |
| B1-C4 | Failure contract | safeなunchecked `AuditRecordingException` | result返却は確認漏れ、Spring persistence例外露出はimplementation coupling | **APPROVED** |
| B1-C5 | Transaction implementation | recorder façade + internal別beanの`MANDATORY` / `REQUIRES_NEW`、明示flush | Public annotation、runtime classification分岐、self-invocationは避ける | **APPROVED** |
| B1-C6 | Persistence implementation | internal JPA `EntityManager.persist + flush`、単一primary transaction manager | Spring Data Repositoryは過剰、JDBC例外化と複数DB対応は要件成立時に再review | **APPROVED** |
| B1-C7 | Log boundary | DB rowをAudit正本とし、Application log / external backendへ依存しない | CloudWatch / fileへの完全配送は後続要件で再設計 | **APPROVED** |

2026年9月2日、Architecture OwnerはB1-C1〜C7を推奨案どおり承認した。次工程はB1-2として最小artifactと
T4 fixtureを実装する。実PostgreSQLでの実測が承認案を否定した場合は実装を固定せず、本reviewへ戻る。

本review承認だけではADR registerを変更しない。B1-2の実PostgreSQL Evidenceが承認案を支持し、Ownerがcontractをacceptした時点で、
Phase 2計画§7に従いAudit contract / transaction判断をADR候補として記録する。
