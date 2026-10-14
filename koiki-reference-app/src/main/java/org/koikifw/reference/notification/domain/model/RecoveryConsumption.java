package org.koikifw.reference.notification.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.UUID;

/** Append-only consumption record; creation does not prove commit or delivery acceptance. */
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

    public static RecoveryConsumption record(
            UUID permitId, UUID operationId, String workerGeneration, Instant consumedAt) {
        RecoveryConsumption consumption = new RecoveryConsumption();
        consumption.permitId = Objects.requireNonNull(permitId, "permitId");
        consumption.operationId = Objects.requireNonNull(operationId, "operationId");
        Objects.requireNonNull(workerGeneration, "workerGeneration");
        if (workerGeneration.isBlank() || workerGeneration.codePointCount(0, workerGeneration.length()) > 128) {
            throw new IllegalArgumentException("workerGeneration must be nonblank and within column length");
        }
        consumption.workerGeneration = workerGeneration;
        consumption.consumedAt = Objects.requireNonNull(consumedAt, "consumedAt").truncatedTo(ChronoUnit.MICROS);
        return consumption;
    }

    public UUID permitId() { return permitId; }
    public UUID operationId() { return operationId; }
    public String workerGeneration() { return workerGeneration; }
    public Instant consumedAt() { return consumedAt; }
}
