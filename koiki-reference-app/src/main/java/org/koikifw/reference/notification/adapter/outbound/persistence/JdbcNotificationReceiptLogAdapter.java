package org.koikifw.reference.notification.adapter.outbound.persistence;

import java.util.Objects;
import java.util.UUID;
import javax.sql.DataSource;
import org.koikifw.reference.expense.domain.event.ExpenseApprovedEvent;
import org.koikifw.reference.notification.application.port.outbound.NotificationProviderPort;
import org.koikifw.reference.notification.application.port.outbound.NotificationReceiptLogPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/** Persists Reference's acceptance observation independently of listener completion. */
public final class JdbcNotificationReceiptLogAdapter implements NotificationReceiptLogPort {
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transaction;

    public JdbcNotificationReceiptLogAdapter(DataSource dataSource, PlatformTransactionManager manager) {
        this.jdbc = new JdbcTemplate(Objects.requireNonNull(dataSource));
        this.transaction = new TransactionTemplate(Objects.requireNonNull(manager));
        this.transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Override
    public void record(ExpenseApprovedEvent event, NotificationProviderPort.Receipt receipt) {
        Objects.requireNonNull(event);
        Objects.requireNonNull(receipt);
        transaction.executeWithoutResult(ignored -> {
            jdbc.update("""
                    INSERT INTO kkref_notification_async.notification_receipt_observation
                    (notification_key, event_id, recipient_id, payload_sha256, acceptance_id, observed_at)
                    VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
                    ON CONFLICT (notification_key) DO NOTHING
                    """, event.notificationKey(), event.eventId(), event.applicantUserId(),
                    event.payloadSha256(), receipt.acceptanceId());
            var stored = Objects.requireNonNull(jdbc.queryForObject("""
                    SELECT event_id, recipient_id, payload_sha256, acceptance_id
                    FROM kkref_notification_async.notification_receipt_observation
                    WHERE notification_key = ?
                    """, (row, number) -> new Stored(row.getObject(1, UUID.class), row.getObject(2, UUID.class),
                            row.getString(3), row.getObject(4, UUID.class)), event.notificationKey()));
            if (!event.eventId().equals(stored.eventId())
                    || !event.applicantUserId().equals(stored.recipientId())
                    || !event.payloadSha256().equals(stored.payloadSha256())
                    || !receipt.acceptanceId().equals(stored.acceptanceId())) {
                throw new IllegalStateException("Notification receipt observation conflict");
            }
        });
    }

    private record Stored(UUID eventId, UUID recipientId, String payloadSha256, UUID acceptanceId) { }
}
