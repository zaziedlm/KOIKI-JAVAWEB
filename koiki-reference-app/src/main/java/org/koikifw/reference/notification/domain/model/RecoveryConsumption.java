package org.koikifw.reference.notification.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** Initial JPA mapping; operational creation and update entrypoints are not yet exposed. */
@Entity
@Table(name = "kkref_notification_recovery_consumption")
public class RecoveryConsumption {

    @Id
    @Column(name = "permit_id", nullable = false, updatable = false)
    private UUID permitId = new UUID(0L, 0L);

    @Column(name = "operation_id", nullable = false, updatable = false)
    private UUID operationId = new UUID(0L, 0L);

    @Column(name = "worker_generation", nullable = false, updatable = false, length = 128)
    private String workerGeneration = "";

    @Column(name = "consumed_at", nullable = false, updatable = false)
    private Instant consumedAt = Instant.EPOCH;

    protected RecoveryConsumption() {
    }
}
