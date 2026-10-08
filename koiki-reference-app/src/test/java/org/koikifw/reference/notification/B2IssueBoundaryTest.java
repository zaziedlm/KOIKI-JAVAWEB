package org.koikifw.reference.notification;

import static org.assertj.core.api.Assertions.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.*;
import org.koikifw.referenceacceptance.notification.*;
import org.koikifw.reference.notification.application.ProtectedRecoveryIssueService;
import org.koikifw.reference.notification.application.port.outbound.*;
import org.springframework.transaction.support.*;

class B2IssueBoundaryTest {
    B2ReadConnectionHarness h;
    @BeforeEach void prepare() throws Exception {h=new B2ReadConnectionHarness();}
    @AfterEach void close() throws Exception {if(h!=null) h.close();}
    @Test void B01_directIssueWithoutScopeRejects() {assertThatThrownBy(()->h.service().issue(h.target(),"B2_TEST")).isInstanceOf(RuntimeException.class);}
    @Test void B02_threadAndNestedScopeReject() throws Exception {
        try(var scope=h.adapter().open(h.target())) {
            assertThatThrownBy(()->h.adapter().open(h.target())).isInstanceOf(IllegalStateException.class);
            try(var executor=Executors.newSingleThreadExecutor()) {
                assertThat(executor.submit(()->h.adapter().current(h.target().environmentId(),h.target().publicationId()).isEmpty()).get(10,TimeUnit.SECONDS)).isTrue();
                assertThat(executor.submit(scope::active).get(10,TimeUnit.SECONDS)).isFalse();
            }
            assertThat(scope.active()).isTrue();
        }
    }
    @Test void B03_writerBlockedAfterCheck() {
        try(var scope=h.adapter().open(h.target())) {assertThat(scope.active()).isTrue();assertThat(h.adapter().current(h.target().environmentId(),h.target().publicationId())).contains(h.target());
            assertThatThrownBy(()->{try(var c=h.source.writerAttempt()) {c.createStatement().execute("UPDATE event_publication SET completion_attempts=1");}}).isInstanceOf(java.sql.SQLException.class);}
    }
    @Test void B04_normalRestartIdentityBlocked() {assertThatThrownBy(h.source::writerAttempt).isInstanceOf(java.sql.SQLException.class);}
    private ProtectedRecoveryIssueService observed(boolean rollback,AtomicBoolean completed) {
        AtomicBoolean registered=new AtomicBoolean();
        RecoveryTargetPort targets=(environment,publication)-> {
            if(registered.compareAndSet(false,true)) TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void beforeCommit(boolean readOnly) {assertThat(h.adapter().current(environment,publication)).contains(h.target());}
                @Override public void afterCompletion(int status) {assertThat(status).isEqualTo(rollback ? STATUS_ROLLED_BACK : STATUS_COMMITTED);
                    assertThat(h.adapter().current(environment,publication)).contains(h.target());completed.set(true);}
            });
            return h.adapter().current(environment,publication);
        };
        return new ProtectedRecoveryIssueService(h.serviceWith(h.context.getBean(RecoveryScopePort.class),h.context.getBean(RecoveryTtlPolicyPort.class),targets),h.adapter());
    }
    @Test void B05_scopeHeldThroughCommit() {AtomicBoolean completed=new AtomicBoolean();observed(false,completed).issue(h.target(),"B2_TEST");assertThat(completed).isTrue();assertThat(h.adapter().current(h.target().environmentId(),h.target().publicationId())).isEmpty();}
    @Test void B06_scopeHeldThroughRollback() {AtomicBoolean completed=new AtomicBoolean();assertThatThrownBy(()->observed(true,completed).issue(h.target(),"")).isInstanceOf(RuntimeException.class);assertThat(completed).isTrue();}
    @Test void B07_expiredAdmissionDoesNotUnfreeze() {h.advance(61);assertThatThrownBy(h::issue).isInstanceOf(RuntimeException.class);assertThatThrownBy(h.source::writerAttempt).isInstanceOf(java.sql.SQLException.class);}
    @Test void B08_callFailureDoesNotEnableWriter() {assertThatThrownBy(()->h.protectedService().issue(h.target(),"")).isInstanceOf(RuntimeException.class);assertThatThrownBy(h.source::writerAttempt).isInstanceOf(java.sql.SQLException.class);}
}
