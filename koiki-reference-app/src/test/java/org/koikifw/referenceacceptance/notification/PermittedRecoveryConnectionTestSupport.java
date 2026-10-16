package org.koikifw.referenceacceptance.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.koikifw.referenceacceptance.notification.NotificationFoundationDbHarness.Mode.CONSUMER;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import org.koikifw.audit.BusinessAuditRecorder;
import org.koikifw.reference.expense.domain.event.ExpenseApprovedEvent;
import org.koikifw.reference.notification.application.PermittedSinglePublicationRecoveryService;
import org.koikifw.reference.notification.application.RecoveryPermitService;
import org.koikifw.reference.notification.application.SinglePublicationRecoveryService;
import org.koikifw.reference.notification.application.port.outbound.IsolatedPublicationPort;
import org.koikifw.reference.notification.application.port.outbound.NotificationProviderPort;
import org.koikifw.reference.notification.application.query.IsolatedRecoverySelection;
import org.koikifw.reference.notification.application.query.RecoveryTarget;
import org.springframework.modulith.events.EventPublication;
import org.springframework.modulith.events.IncompleteEventPublications;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Test-owned doubles beneath the real recovery service; no provider delivery or publication completion. */
public final class PermittedRecoveryConnectionTestSupport implements AutoCloseable {
    public static final String WORKER = "connection-test-worker";
    public final NotificationFoundationDbHarness db;
    public final RecoveryTarget target;
    public final IsolatedRecoverySelection selection;
    public final UUID permit;
    public final UUID operation = UUID.randomUUID();
    public final AtomicInteger entries = new AtomicInteger();
    public final AtomicInteger resubmissions = new AtomicInteger();
    public boolean hold;
    public boolean failResultAudit;
    private boolean auditRevoked;
    private final ExpenseApprovedEvent selectedEvent;

    public PermittedRecoveryConnectionTestSupport(NotificationFoundationDbHarness db) throws Exception {
        this.db = db;
        db.reset();
        var event = new ExpenseApprovedEvent(UUID.randomUUID(), UUID.randomUUID(), NotificationFoundationDbHarness.USER, 1, 100);
        selectedEvent = event;
        target = new RecoveryTarget("connection-test", UUID.randomUUID(), event.eventId(),
                "ExpenseApprovalListener.onApproval", 0);
        selection = new IsolatedRecoverySelection(target.environmentId(), target.publicationId(), target.eventId(),
                target.listenerId(), "PUBLISHED", target.expectedAttempt(), event.notificationKey(), event.payloadSha256());
        db.ports.snapshots.put(target.publicationId(), target);
        permit = db.issue(target);
        db.proof(permit, target, operation, WORKER);
        // Pre-open the shared context before the two request threads; never mutate its cache concurrently.
        db.context(CONSUMER);
    }

    public PermittedSinglePublicationRecoveryService connection(boolean failAfterConsume) {
        var context = db.context(CONSUMER);
        var realPermits = db.service(CONSUMER);
        RecoveryPermitService permits = realPermits;
        if (failAfterConsume) {
            permits = mock(RecoveryPermitService.class);
            doAnswer(call -> {
                realPermits.consume(call.getArgument(0), call.getArgument(1), call.getArgument(2), call.getArgument(3));
                throw new IllegalStateException("Test-only failure after committed consumption");
            }).when(permits).consume(any(), any(), any(), any());
        }
        // Use the exact immutable event snapshot selected above, including key and payload hash.
        var publications = mock(IncompleteEventPublications.class);
        doAnswer(call -> {
            Predicate<EventPublication> filter = call.getArgument(0);
            var selected = mock(EventPublication.class);
            var other = mock(EventPublication.class);
            when(selected.getIdentifier()).thenReturn(target.publicationId());
            when(other.getIdentifier()).thenReturn(UUID.randomUUID());
            assertThat(filter.test(selected)).isTrue();
            assertThat(filter.test(other)).isFalse();
            resubmissions.incrementAndGet();
            return null;
        }).when(publications).resubmitIncompletePublications(org.mockito.ArgumentMatchers.<Predicate<EventPublication>>any());
        IsolatedPublicationPort port = requested -> {
            entries.incrementAndGet();
            assertThat(requested).isEqualTo(selection);
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            assertCounts(1, 1, 0);
            return new IsolatedPublicationPort.Lease() {
                @Override public void assertUnchanged() { }
                @Override public ExpenseApprovedEvent event() { return selectedEvent; }
                @Override public void awaitCompleted(Duration timeout) {
                    assertThat(timeout).isEqualTo(Duration.ofSeconds(30));
                    if (failResultAudit) revokeAudit();
                }
                @Override public void close() { }
            };
        };
        NotificationProviderPort provider = new NotificationProviderPort() {
            @Override public Receipt deliver(ExpenseApprovedEvent ignored) { throw new AssertionError("No provider delivery allowed"); }
            @Override public Inspection inspect(ExpenseApprovedEvent inspected) {
                assertThat(inspected).isEqualTo(selectedEvent);
                return new Inspection(hold ? InspectionState.UNKNOWN : InspectionState.ACCEPTED_MATCH,
                        hold ? Optional.empty() : Optional.of(UUID.randomUUID()), Optional.empty());
            }
        };
        return new PermittedSinglePublicationRecoveryService(permits,
                new SinglePublicationRecoveryService(port, publications, provider),
                context.getBean(BusinessAuditRecorder.class), context.getBean(PlatformTransactionManager.class));
    }

    public void run(PermittedSinglePublicationRecoveryService connection) {
        connection.recover(permit, target, operation, WORKER, selection);
    }

    public void revokeAudit() {
        try {
            db.sql("REVOKE INSERT ON koiki_audit_event FROM kkref_notification_consumer");
            auditRevoked = true;
        } catch (Exception failure) { throw new IllegalStateException("Test grant change failed", failure); }
    }

    public void assertCounts(int consumption, int consumedAudit, int resultAudit) {
        try {
            assertThat(db.rows("SELECT count(*) FROM kkref_notification_recovery_consumption")).containsExactly("" + consumption);
            assertThat(db.rows("SELECT count(*) FROM koiki_audit_event WHERE action='PERMIT_CONSUMED'"))
                    .containsExactly("" + consumedAudit);
            assertThat(db.rows("SELECT count(*) FROM koiki_audit_event WHERE resource_type='NOTIFICATION_RECOVERY_OPERATION'"))
                    .containsExactly("" + resultAudit);
        } catch (Exception failure) { throw new IllegalStateException("Independent observation failed", failure); }
    }

    public void assertResult(String action, String result) throws Exception {
        assertThat(db.rows("SELECT action,result,actor_id,resource_id FROM koiki_audit_event "
                + "WHERE resource_type='NOTIFICATION_RECOVERY_OPERATION'"))
                .containsExactly(action + "|" + result + "|" + NotificationFoundationDbHarness.USER + "|" + operation);
    }

    @Override public void close() throws Exception {
        try {
            if (auditRevoked) db.sql("GRANT INSERT ON koiki_audit_event TO kkref_notification_consumer");
            assertThat(db.rows("SELECT has_table_privilege('kkref_notification_consumer','koiki_audit_event','INSERT')"))
                    .containsExactly("true");
        } finally { db.close(); }
    }
}
