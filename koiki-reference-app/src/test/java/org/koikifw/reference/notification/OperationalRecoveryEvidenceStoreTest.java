package org.koikifw.reference.notification;

import static org.assertj.core.api.Assertions.*;
import static org.koikifw.referenceacceptance.notification.OperationalRecoveryTestSupport.*;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.koikifw.reference.notification.application.query.RecoveryTarget;
import org.koikifw.referenceacceptance.notification.NotificationFoundationDbHarness.MutableClock;

class OperationalRecoveryEvidenceStoreTest {
    final MutableClock clock = new MutableClock();
    final RecoveryTarget target = target();
    final Model model = new Model(clock, target);
    final UUID permit = UUID.randomUUID(), operation = UUID.randomUUID();
    final Body body = model.collect(permit, target, operation, WORKER);
    boolean consumption() { return model.consumption(permit, target, operation, WORKER).isPresent(); }
    @Test void E01PersistsAndResolvesExactEvidence() { model.save(body); assertThat(consumption()).isTrue(); assertThat(model.resolve(body.key())).contains(body); }
    @Test void E02SameBodyCanBeResolvedAgain() { assertThat(model.save(body)).isEqualTo(model.save(body)); assertThat(model.bodies).hasSize(1); }
    @Test void E03RejectsReplacementAndDetectsTampering() {
        model.save(body); var changed = new Body(body.key(), body.target(), body.revision(), body.stop(), body.provider(), "changed", body.window());
        assertThatThrownBy(() -> model.save(changed)).isInstanceOf(IllegalStateException.class);
        model.bodies.put(body.key(), changed); assertThat(consumption()).isFalse();
    }
    @Test void E04MissingAndAmbiguousReferencesAreRejected() { assertThat(consumption()).isFalse(); model.save(body); model.ambiguous = true; assertThat(consumption()).isFalse(); model.ambiguous = false; model.storeAvailable = false; assertThat(consumption()).isFalse(); }
    @Test void E05BindsPermitAndFullTarget() { model.save(body); assertThat(model.consumption(UUID.randomUUID(), target, operation, WORKER)).isEmpty(); assertThat(model.consumption(permit, target(), operation, WORKER)).isEmpty(); }
    @Test void E06BindsOperationAndWorkerGeneration() { model.save(body); assertThat(model.consumption(permit, target, UUID.randomUUID(), WORKER)).isEmpty(); assertThat(model.consumption(permit, target, operation, "next-worker")).isEmpty(); }
    @Test void E07RejectsRevokedAndExpiredEvidence() { model.save(body); model.revoked.add(body.key()); assertThat(consumption()).isFalse(); model.revoked.clear(); clock.now = FROM.plusSeconds(60); assertThat(consumption()).isFalse(); }
    @Test void E08StorageFailureCannotBecomeConsumption() { model.saveFails = true; assertThatThrownBy(() -> model.save(body)).isInstanceOf(IllegalStateException.class); assertThat(model.resolve(body.key())).isEmpty(); model.saveFails = false; model.save(body); assertThat(model.resolve(body.key())).contains(body); /* Presence alone is not a committed consumption. */ }
    @Test void E09ClosureRequiresActorTargetAndRecordedReason() {
        model.provider = model.accept("test-logical-notification", "test-payload-1", "test-recipient");
        model.saveResolution(new Resolution(permit, target, USER, "result", "PROVIDER_RECONCILED", model.provider, window()));
        assertThat(model.closure(permit, target, USER, "result")).isPresent();
        assertThat(model.closure(permit, target, UUID.randomUUID(), "result")).isEmpty();
        assertThat(model.closure(permit, target, USER, "missing")).isEmpty();
        assertThat(model.closure(permit, target(), USER, "result")).isEmpty();
        model.saveResolution(new Resolution(permit, target, USER, "blank", "", model.provider, window()));
        assertThat(model.closure(permit, target, USER, "blank")).isEmpty();
    }
    @Test void E10UnknownResolutionMustRemainOpen() { model.provider = model.receipt("test-logical-notification", "test-payload-1", "test-recipient", Acceptance.UNKNOWN); model.saveResolution(new Resolution(permit, target, USER, "unknown", "INVESTIGATE", model.provider, window())); assertThat(model.closure(permit, target, USER, "unknown")).isEmpty(); }
}
