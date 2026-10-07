package org.koikifw.reference.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.koikifw.referenceacceptance.notification.OperationalRecoveryTestSupport.*;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.koikifw.reference.notification.application.query.RecoveryTarget;
import org.koikifw.referenceacceptance.notification.NotificationFoundationDbHarness.MutableClock;

class OperationalRecoveryTargetContractTest {
    final MutableClock clock = new MutableClock();
    final RecoveryTarget target = target();
    final Model model = new Model(clock, target);
    boolean matches(RecoveryTarget request) { return model.current(request.environmentId(), request.publicationId()).filter(request::equals).isPresent(); }
    RecoveryTarget changed(String env, UUID pub, UUID event, String listener, int attempt) { return new RecoveryTarget(env, pub, event, listener, attempt); }
    @Test void T01MatchesAllDimensions() { assertThat(matches(target)).isTrue(); }
    @Test void T02RejectsSameEventOtherPublication() { assertThat(matches(changed(target.environmentId(), UUID.randomUUID(), target.eventId(), target.listenerId(), 0))).isFalse(); }
    @Test void T03RejectsOtherEnvironment() { assertThat(matches(changed("outside", target.publicationId(), target.eventId(), target.listenerId(), 0))).isFalse(); }
    @Test void T04RejectsOtherEvent() { assertThat(matches(changed(target.environmentId(), target.publicationId(), UUID.randomUUID(), target.listenerId(), 0))).isFalse(); }
    @Test void T05RejectsOtherListener() { assertThat(matches(changed(target.environmentId(), target.publicationId(), target.eventId(), "other", 0))).isFalse(); }
    @Test void T06RejectsOtherAttempt() { assertThat(matches(changed(target.environmentId(), target.publicationId(), target.eventId(), target.listenerId(), 1))).isFalse(); }
    @Test void T07RejectsDeletedTarget() { model.observed = null; assertThat(matches(target)).isFalse(); }
    @Test void T08RejectsRevisionChangeAndIneligibleState() {
        model.observed = new Observation(target, 2, true, window()); assertThat(matches(target)).isFalse();
        model.observed = new Observation(target, 1, false, window()); assertThat(matches(target)).isFalse();
    }
    @Test void T09EnforcesObservationBoundaries() {
        clock.now = FROM.minusNanos(1); assertThat(matches(target)).isFalse();
        clock.now = FROM; assertThat(matches(target)).isTrue();
        clock.now = FROM.plusSeconds(60); assertThat(matches(target)).isFalse();
    }
    @Test void T10DoesNotFallbackToCachedValue() {
        assertThat(matches(target)).isTrue(); model.sourceAvailable = false; assertThat(matches(target)).isFalse();
        model.sourceAvailable = true; clock.now = FROM.plusSeconds(61); assertThat(matches(target)).isFalse();
    }
}
