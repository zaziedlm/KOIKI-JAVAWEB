package org.koikifw.reference.notification;

import static org.assertj.core.api.Assertions.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.koikifw.referenceacceptance.notification.*;
import org.koikifw.reference.notification.application.*;
import org.koikifw.reference.notification.application.port.outbound.*;
import org.koikifw.reference.notification.application.query.RecoveryTarget;
import org.koikifw.reference.notification.adapter.outbound.configuration.FrozenRecoverySourceSettings;
import org.koikifw.reference.notification.adapter.outbound.operational.JdbcProtectedRecoveryTargetAdapter;

@EnabledIfSystemProperty(named="koiki.b2.reference-acceptance.enabled", matches="true")
class B2ProtectedIssueReadTest {
    B2ReadConnectionHarness h;
    private boolean closed;
    @BeforeEach void prepare() throws Exception {h=new B2ReadConnectionHarness();}
    @AfterEach void close() throws Exception {if(h!=null && !closed) h.close();}
    private void empty() throws Exception {assertThat(h.db.rows("SELECT count(*) FROM kkref_notification_recovery_permit")).containsExactly("0");}
    @Test void I01_realSaveAuditAndScopedRead() throws Exception {
        UUID id=h.issue();assertThat(h.protectedService().read(id,h.target().environmentId(),h.target().publicationId()).target()).isEqualTo(h.target());
        assertThat(h.db.rows("SELECT count(*) FROM kkref_notification_recovery_permit")).containsExactly("1");
        assertThat(h.db.rows("SELECT audit_type,event_type,actor_type,actor_id,resource_type,resource_id,result FROM koiki_audit_event WHERE action='PERMIT_ISSUED'"))
            .containsExactly("BUSINESS|NOTIFICATION_RECOVERY_CHANGE|USER|"+NotificationFoundationDbHarness.USER+"|NOTIFICATION_RECOVERY_PERMIT|"+id+"|SUCCESS");
        assertThat(h.adapter().current(h.target().environmentId(),h.target().publicationId())).isEmpty();
    }
    @Test void I02_issueCapabilityAbsent() throws Exception {h.db.removeCapability("ISSUE");assertThatThrownBy(h::issue).isInstanceOf(RuntimeException.class);empty();}
    @Test void I03_readCapabilityAbsent() throws Exception {UUID id=h.issue();h.db.removeCapability("READ");assertThatThrownBy(()->h.protectedService().read(id,h.target().environmentId(),h.target().publicationId())).isInstanceOf(RuntimeException.class);}
    @Test void I04_inactiveIdentity() throws Exception {h.db.sql("UPDATE koiki_user SET status='DISABLED'");assertThatThrownBy(h::issue).isInstanceOf(RuntimeException.class);empty();}
    private ProtectedRecoveryIssueService scoped(RecoveryScopePort.Decision decision) {
        return new ProtectedRecoveryIssueService(h.serviceWith((actor,capability,environment,publication)->decision,
            h.context.getBean(RecoveryTtlPolicyPort.class),h.adapter()),h.adapter());
    }
    @Test void I05_scopeOutside() throws Exception {assertThatThrownBy(()->scoped(RecoveryScopePort.Decision.OUTSIDE).issue(h.target(),"B2_TEST")).isInstanceOf(RuntimeException.class);empty();}
    @Test void I06_scopeUnavailable() throws Exception {assertThatThrownBy(()->scoped(RecoveryScopePort.Decision.UNAVAILABLE).issue(h.target(),"B2_TEST")).isInstanceOf(RuntimeException.class);empty();}
    @Test void I07_ttlUnavailableAndAdmissionExpired() throws Exception {
        RecoveryTtlPolicyPort unavailable=target -> Optional.empty();
        var service=new ProtectedRecoveryIssueService(h.serviceWith(h.context.getBean(RecoveryScopePort.class),unavailable,h.adapter()),h.adapter());
        assertThatThrownBy(()->service.issue(h.target(),"B2_TEST")).isInstanceOf(RuntimeException.class);
        h.advance(61);assertThatThrownBy(h::issue).isInstanceOf(RuntimeException.class);empty();
    }
    @Test void I08_tupleMismatch() throws Exception {var t=h.target();var wrong=new RecoveryTarget(t.environmentId(),t.publicationId(),UUID.randomUUID(),t.listenerId(),t.expectedAttempt());
        assertThatThrownBy(()->h.protectedService().issue(wrong,"B2_TEST")).isInstanceOf(RuntimeException.class);empty();}
    private JdbcProtectedRecoveryTargetAdapter wrongSource(String password) {
        Map<String,String> env=Map.of("B2_SOURCE_JDBC_URL",h.source.sourceUrl(),"B2_SOURCE_READER_USERNAME","b2_reader","B2_SOURCE_READER_PASSWORD",password);
        var p=h.source.ready;
        return new JdbcProtectedRecoveryTargetAdapter(new FrozenRecoverySourceSettings(p.getProperty("environment"),p.getProperty("run"),p.getProperty("source"),"0".repeat(64),Long.parseLong(p.getProperty("revision")),env),h.db.clock);
    }
    @Test void I09_sourceBindingMismatch() throws Exception {assertThatThrownBy(()->wrongSource(h.source.readerSecret).open(h.target())).isInstanceOf(RuntimeException.class);empty();}
    @Test void I10_providerUnknownRejects() throws Exception {h.close();closed=true;h=new B2ReadConnectionHarness("UNKNOWN");closed=false;assertThatThrownBy(h::issue).isInstanceOf(RuntimeException.class);empty();}
    @Test void I11_providerAcceptedRejects() throws Exception {h.close();closed=true;h=new B2ReadConnectionHarness("FIXTURE_ACCEPTED");closed=false;assertThatThrownBy(h::issue).isInstanceOf(RuntimeException.class);empty();}
    @Test void I12_supplierConnectionUnavailable() throws Exception {assertThatThrownBy(()->wrongSource(UUID.randomUUID().toString()).open(h.target())).isInstanceOf(RuntimeException.class);empty();}
}
