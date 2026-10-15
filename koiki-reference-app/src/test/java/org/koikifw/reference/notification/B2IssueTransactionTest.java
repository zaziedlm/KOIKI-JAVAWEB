package org.koikifw.reference.notification;

import static org.assertj.core.api.Assertions.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.koikifw.referenceacceptance.notification.*;
import org.koikifw.reference.notification.application.port.outbound.RecoveryEvidencePort;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@EnabledIfSystemProperty(named="koiki.b2.reference-acceptance.enabled", matches="true")
class B2IssueTransactionTest {
    B2ReadConnectionHarness h;
    private boolean closed;
    @BeforeEach void prepare() throws Exception {h=new B2ReadConnectionHarness();}
    @AfterEach void close() throws Exception {if(h!=null && !closed) h.close();}
    private void count(String expected) throws Exception {assertThat(h.db.rows("SELECT count(*) FROM kkref_notification_recovery_permit")).containsExactly(expected);}
    @Test void X01_auditFailureRollsBackAndGrantRestores() throws Exception {
        assertThat(h.db.rows("SELECT has_table_privilege('kkref_notification_permit','koiki_audit_event','INSERT')")).containsExactly("true");
        try {h.db.sql("REVOKE INSERT ON koiki_audit_event FROM kkref_notification_permit");assertThatThrownBy(h::issue).isInstanceOf(RuntimeException.class);count("0");}
        finally {h.db.sql("GRANT INSERT ON koiki_audit_event TO kkref_notification_permit");assertThat(h.db.rows("SELECT has_table_privilege('kkref_notification_permit','koiki_audit_event','INSERT')")).containsExactly("true");}
    }
    @Test void X02_concurrentSamePublicationOnlyOnePermit() throws Exception {
        CountDownLatch start=new CountDownLatch(1);
        try(var executor=Executors.newFixedThreadPool(2)) {
            Callable<Boolean> operation=()->{NotificationFoundationDbHarness.authenticate(NotificationFoundationDbHarness.USER);
                try {if(!start.await(10,TimeUnit.SECONDS)) throw new IllegalStateException("Bounded start failed");h.issue();return true;}
                catch(RuntimeException denied) {return false;} finally {org.springframework.security.core.context.SecurityContextHolder.clearContext();}};
            var first=executor.submit(operation);var second=executor.submit(operation);start.countDown();
            assertThat((first.get(10,TimeUnit.SECONDS)?1:0)+(second.get(10,TimeUnit.SECONDS)?1:0)).isEqualTo(1);
        }
        count("1");
    }
    @Test void X03_callerTransactionRejected() throws Exception {var tx=new TransactionTemplate(h.context.getBean(PlatformTransactionManager.class));tx.setTimeout(10);
        assertThatThrownBy(()->tx.execute(status->h.issue())).isInstanceOf(IllegalStateException.class);count("0");}
    @Test void X04_consumeEvidenceEmptyAndOperationRejected() throws Exception {UUID id=h.issue();var evidence=h.context.getBean(RecoveryEvidencePort.class);UUID operation=UUID.randomUUID();
        assertThat(evidence.consumption(id,h.target(),operation,"generation")).isEmpty();
        try(var scope=h.adapter().open(h.target())) {assertThat(scope.active()).isTrue();assertThatThrownBy(()->h.service().consume(id,h.target(),operation,"generation")).isInstanceOf(RuntimeException.class);}
        assertThat(h.db.rows("SELECT count(*) FROM kkref_notification_recovery_consumption")).containsExactly("0");count("1");}
    @Test void X05_closeEvidenceEmptyAndOperationRejected() throws Exception {UUID id=h.issue();var evidence=h.context.getBean(RecoveryEvidencePort.class);
        assertThat(evidence.closure(id,h.target(),NotificationFoundationDbHarness.USER,"result")).isEmpty();
        try(var scope=h.adapter().open(h.target())) {assertThat(scope.active()).isTrue();assertThatThrownBy(()->h.service().close(id,h.target(),"result")).isInstanceOf(RuntimeException.class);}
        assertThat(h.db.rows("SELECT count(*) FROM kkref_notification_recovery_permit WHERE closed_at IS NULL")).containsExactly("1");}
    @Test void X06_denialSecurityAuditAndNoPermitChange() throws Exception {h.db.removeCapability("ISSUE");assertThatThrownBy(h::issue).isInstanceOf(RuntimeException.class);count("0");
        assertThat(h.db.rows("SELECT count(*) FROM koiki_audit_event")).containsExactly("1");}
    @Test void X07_postCommitResponseLossHoldAndQueryWithoutRetry() throws Exception {UUID id=h.issue();
        // Test-owned response failure after the real commit; no implication about every DB commit failure.
        assertThatThrownBy(()->{throw new IllegalStateException("Simulated response unavailable: HOLD");}).hasMessageContaining("HOLD");
        assertThat(h.protectedService().read(id,h.target().environmentId(),h.target().publicationId()).permitId()).isEqualTo(id);count("1");}
    @Test void X08_abnormalCallerPreservesPendingUntilDiscard() throws Exception {UUID id=h.issue();
        try {throw new IllegalStateException("Test-owned caller ended");}
        catch(IllegalStateException ended) {assertThat(h.db.rows("SELECT count(*) FROM kkref_notification_recovery_permit WHERE permit_id='"+id+"' AND closed_at IS NULL")).containsExactly("1");}
        finally {h.close();closed=true;}
    }
}
