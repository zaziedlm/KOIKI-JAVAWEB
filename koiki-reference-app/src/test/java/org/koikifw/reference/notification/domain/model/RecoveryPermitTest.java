package org.koikifw.reference.notification.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Adopted P01-P12; finite times/identifiers are test inputs, not operational policy. */
class RecoveryPermitTest {
    private static final UUID PERMIT = UUID.randomUUID();
    private static final UUID PUBLICATION = UUID.randomUUID();
    private static final UUID EVENT = UUID.randomUUID();
    private static final UUID ACTOR = UUID.randomUUID();
    private static final Instant ISSUED = Instant.parse("2026-10-07T00:00:00.123456789Z");
    private static final Instant EXPIRES = ISSUED.plusSeconds(60);

    private static RecoveryPermit permit() {
        return RecoveryPermit.issue(PERMIT, "test-environment", PUBLICATION, EVENT,
                "test-listener", 0, ACTOR, "TEST_REASON", ISSUED, EXPIRES);
    }

    private static void check(RecoveryPermit permit, Instant now) {
        permit.requireConsumable("test-environment", PUBLICATION, EVENT, "test-listener", 0, now);
    }

    @Test
    void permitsConsumptionImmediatelyBeforeExpiry() {
        RecoveryPermit permit = permit();
        assertThat(permit.issuedAt()).isEqualTo(Instant.parse("2026-10-07T00:00:00.123456Z"));
        assertThat(permit.expiresAt()).isEqualTo(Instant.parse("2026-10-07T00:01:00.123456Z"));
        assertThat(permit.expiresAt()).isBefore(EXPIRES);
        assertThatCode(() -> check(permit, permit.expiresAt().minusNanos(1))).doesNotThrowAnyException();
        assertThat(permit.permitId()).isEqualTo(PERMIT);
        assertThat(permit.environmentId()).isEqualTo("test-environment");
        assertThat(permit.publicationId()).isEqualTo(PUBLICATION);
        assertThat(permit.eventId()).isEqualTo(EVENT);
        assertThat(permit.listenerId()).isEqualTo("test-listener");
        assertThat(permit.expectedAttempt()).isZero();
        assertThat(permit.actorId()).isEqualTo(ACTOR);
        assertThat(permit.reasonCode()).isEqualTo("TEST_REASON");
        assertThat(permit.closedAt()).isNull();
        assertThat(permit.version()).isNull();
    }

    @Test
    void rejectsConsumptionAtExpiry() {
        RecoveryPermit permit = permit();
        assertThatThrownBy(() -> check(permit, permit.expiresAt()))
                .isInstanceOf(IllegalStateException.class).hasMessage("Permit has expired");
    }

    @Test
    void rejectsConsumptionAfterExpiry() {
        RecoveryPermit permit = permit();
        assertThatThrownBy(() -> check(permit, permit.expiresAt().plusNanos(1)))
                .isInstanceOf(IllegalStateException.class).hasMessage("Permit has expired");
        // The original nanosecond deadline is already outside the truncated deadline.
        assertThatThrownBy(() -> check(permit, EXPIRES)).isInstanceOf(IllegalStateException.class);
        assertThat(permit.closedAt()).isNull();
    }

    @Test
    void rejectsDifferentEnvironment() {
        RecoveryPermit permit = permit();
        assertThatThrownBy(() -> permit.requireConsumable("other", PUBLICATION, EVENT, "test-listener", 0, ISSUED))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("Permit target does not match");
    }

    @Test
    void rejectsDifferentPublication() {
        RecoveryPermit permit = permit();
        assertThatThrownBy(() -> permit.requireConsumable("test-environment", UUID.randomUUID(), EVENT, "test-listener", 0, ISSUED))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("Permit target does not match");
    }

    @Test
    void rejectsDifferentEvent() {
        RecoveryPermit permit = permit();
        assertThatThrownBy(() -> permit.requireConsumable("test-environment", PUBLICATION, UUID.randomUUID(), "test-listener", 0, ISSUED))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("Permit target does not match");
    }

    @Test
    void rejectsDifferentListener() {
        RecoveryPermit permit = permit();
        assertThatThrownBy(() -> permit.requireConsumable("test-environment", PUBLICATION, EVENT, "other", 0, ISSUED))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("Permit target does not match");
    }

    @Test
    void rejectsDifferentAttempt() {
        RecoveryPermit permit = permit();
        assertThatThrownBy(() -> permit.requireConsumable("test-environment", PUBLICATION, EVENT, "test-listener", 1, ISSUED))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("Permit target does not match");
    }

    @Test
    void rejectsConsumptionAfterClosure() {
        RecoveryPermit permit = permit();
        permit.close(ISSUED, ACTOR, "test-proof");
        assertThatThrownBy(() -> check(permit, ISSUED))
                .isInstanceOf(IllegalStateException.class).hasMessage("Permit is closed");
    }

    @Test
    void rejectsMissingIdentityAndTargetValues() {
        assertThatThrownBy(() -> RecoveryPermit.issue(null, "env", PUBLICATION, EVENT, "listener", 0, ACTOR, "REASON", ISSUED, EXPIRES))
                .isInstanceOf(NullPointerException.class).hasMessage("permitId");
        assertThatThrownBy(() -> RecoveryPermit.issue(PERMIT, "env", PUBLICATION, EVENT, "listener", 0, null, "REASON", ISSUED, EXPIRES))
                .isInstanceOf(NullPointerException.class).hasMessage("actorId");
        assertThatThrownBy(() -> RecoveryPermit.issue(PERMIT, null, PUBLICATION, EVENT, "listener", 0, ACTOR, "REASON", ISSUED, EXPIRES))
                .isInstanceOf(NullPointerException.class).hasMessage("environmentId");
        for (String environment : new String[] {"", " \t\n", "e".repeat(129)}) {
            assertThatThrownBy(() -> RecoveryPermit.issue(PERMIT, environment, PUBLICATION, EVENT, "listener", 0, ACTOR, "REASON", ISSUED, EXPIRES))
                    .isInstanceOf(IllegalArgumentException.class);
        }
        assertThatThrownBy(() -> RecoveryPermit.issue(PERMIT, "env", null, EVENT, "listener", 0, ACTOR, "REASON", ISSUED, EXPIRES))
                .isInstanceOf(NullPointerException.class).hasMessage("publicationId");
        assertThatThrownBy(() -> RecoveryPermit.issue(PERMIT, "env", PUBLICATION, null, "listener", 0, ACTOR, "REASON", ISSUED, EXPIRES))
                .isInstanceOf(NullPointerException.class).hasMessage("eventId");
        assertThatThrownBy(() -> RecoveryPermit.issue(PERMIT, "env", PUBLICATION, EVENT, null, 0, ACTOR, "REASON", ISSUED, EXPIRES))
                .isInstanceOf(NullPointerException.class).hasMessage("listenerId");
        for (String listener : new String[] {"", " \t\n"}) {
            assertThatThrownBy(() -> RecoveryPermit.issue(PERMIT, "env", PUBLICATION, EVENT, listener, 0, ACTOR, "REASON", ISSUED, EXPIRES))
                    .isInstanceOf(IllegalArgumentException.class);
        }
        assertThatThrownBy(() -> RecoveryPermit.issue(PERMIT, "env", PUBLICATION, EVENT, "listener", -1, ACTOR, "REASON", ISSUED, EXPIRES))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsInvalidReasonAndValidityInterval() {
        assertThatThrownBy(() -> RecoveryPermit.issue(PERMIT, "env", PUBLICATION, EVENT, "listener", 0, ACTOR, null, ISSUED, EXPIRES))
                .isInstanceOf(NullPointerException.class).hasMessage("reasonCode");
        for (String reason : new String[] {"", " \t\n", "r".repeat(129)}) {
            assertThatThrownBy(() -> RecoveryPermit.issue(PERMIT, "env", PUBLICATION, EVENT, "listener", 0, ACTOR, reason, ISSUED, EXPIRES))
                    .isInstanceOf(IllegalArgumentException.class);
        }
        assertThatThrownBy(() -> RecoveryPermit.issue(PERMIT, "env", PUBLICATION, EVENT, "listener", 0, ACTOR, "REASON", null, EXPIRES))
                .isInstanceOf(NullPointerException.class).hasMessage("issuedAt");
        assertThatThrownBy(() -> RecoveryPermit.issue(PERMIT, "env", PUBLICATION, EVENT, "listener", 0, ACTOR, "REASON", ISSUED, null))
                .isInstanceOf(NullPointerException.class).hasMessage("expiresAt");
        for (Instant invalidExpiry : new Instant[] {ISSUED, ISSUED.minusSeconds(1), ISSUED.plusNanos(1)}) {
            assertThatThrownBy(() -> RecoveryPermit.issue(PERMIT, "env", PUBLICATION, EVENT, "listener", 0, ACTOR, "REASON", ISSUED, invalidExpiry))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Test
    void requiresCompleteClosureEvidence() {
        RecoveryPermit permit = permit();
        assertThatThrownBy(() -> permit.close(null, ACTOR, "test-proof"))
                .isInstanceOf(NullPointerException.class).hasMessage("closedAt");
        assertThatThrownBy(() -> permit.close(ISSUED, null, "test-proof"))
                .isInstanceOf(NullPointerException.class).hasMessage("confirmedBy");
        assertThatThrownBy(() -> permit.close(ISSUED, ACTOR, null))
                .isInstanceOf(NullPointerException.class).hasMessage("resultRef");
        for (String reference : new String[] {"", " \t\n", "r".repeat(256)}) {
            assertThatThrownBy(() -> permit.close(ISSUED, ACTOR, reference)).isInstanceOf(IllegalArgumentException.class);
        }
        assertThat(permit.closedAt()).isNull();
        assertThat(permit.confirmedBy()).isNull();
        assertThat(permit.resultRef()).isNull();
        // Expiry does not auto-close, and it must not prevent a later human resolution.
        Instant resolution = EXPIRES.plusSeconds(60);
        permit.close(resolution, ACTOR, "test-proof");
        assertThat(permit.closedAt()).isEqualTo(Instant.parse("2026-10-07T00:02:00.123456Z"));
        assertThat(permit.confirmedBy()).isEqualTo(ACTOR);
        assertThat(permit.resultRef()).isEqualTo("test-proof");
        assertThatThrownBy(() -> permit.close(resolution.plusSeconds(1), UUID.randomUUID(), "replacement-proof"))
                .isInstanceOf(IllegalStateException.class).hasMessage("Permit is already closed");
        assertThat(permit.closedAt()).isEqualTo(Instant.parse("2026-10-07T00:02:00.123456Z"));
        assertThat(permit.confirmedBy()).isEqualTo(ACTOR);
        assertThat(permit.resultRef()).isEqualTo("test-proof");
        assertThat(permit.issuedAt()).isEqualTo(Instant.parse("2026-10-07T00:00:00.123456Z"));
        assertThat(permit.expiresAt()).isEqualTo(Instant.parse("2026-10-07T00:01:00.123456Z"));
    }
}
