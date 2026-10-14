package org.koikifw.reference.notification.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Adopted C01-C03; database permissions/uniqueness are verified in the persistence stage. */
class RecoveryConsumptionTest {
    private static final UUID PERMIT = UUID.randomUUID();
    private static final UUID OPERATION = UUID.randomUUID();
    private static final Instant NOW = Instant.parse("2026-10-07T00:00:00.123456789Z");

    @Test
    void rejectsMissingOperationId() {
        assertThatThrownBy(() -> RecoveryConsumption.record(PERMIT, null, "test-worker", NOW))
                .isInstanceOf(NullPointerException.class).hasMessage("operationId");
        RecoveryConsumption consumption = RecoveryConsumption.record(PERMIT, OPERATION, "test-worker", NOW);
        assertThat(consumption.permitId()).isEqualTo(PERMIT);
        assertThat(consumption.operationId()).isEqualTo(OPERATION);
        assertThat(consumption.workerGeneration()).isEqualTo("test-worker");
        assertThat(consumption.consumedAt()).isEqualTo(Instant.parse("2026-10-07T00:00:00.123456Z"));
    }

    @Test
    void rejectsMissingWorkerGeneration() {
        assertThatThrownBy(() -> RecoveryConsumption.record(PERMIT, OPERATION, null, NOW))
                .isInstanceOf(NullPointerException.class).hasMessage("workerGeneration");
        for (String worker : new String[] {"", " \t\n", "w".repeat(129)}) {
            assertThatThrownBy(() -> RecoveryConsumption.record(PERMIT, OPERATION, worker, NOW))
                    .isInstanceOf(IllegalArgumentException.class);
        }
        assertThatThrownBy(() -> RecoveryConsumption.record(PERMIT, OPERATION, "test-worker", null))
                .isInstanceOf(NullPointerException.class).hasMessage("consumedAt");
    }

    @Test
    void rejectsMissingPermitId() {
        assertThatThrownBy(() -> RecoveryConsumption.record(null, OPERATION, "test-worker", NOW))
                .isInstanceOf(NullPointerException.class).hasMessage("permitId");
    }
}
