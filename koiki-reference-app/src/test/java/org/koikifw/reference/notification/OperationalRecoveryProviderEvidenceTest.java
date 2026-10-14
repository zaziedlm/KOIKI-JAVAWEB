package org.koikifw.reference.notification;

import static org.assertj.core.api.Assertions.*;
import static org.koikifw.referenceacceptance.notification.OperationalRecoveryTestSupport.*;
import org.junit.jupiter.api.Test;
import org.koikifw.referenceacceptance.notification.NotificationFoundationDbHarness.MutableClock;

class OperationalRecoveryProviderEvidenceTest {
    final MutableClock clock = new MutableClock();
    final Model model = new Model(clock, target());
    @Test void P01AcceptsOnlyConfirmedNonAcceptance() {
        assertThat(model.mayRetry(model.provider)).isTrue(); model.provider = model.receipt("test-logical-notification", "test-payload-1", "test-recipient", Acceptance.REJECTED);
        assertThat(model.mayRetry(model.provider)).isTrue(); model.processStopped = false;
        var body = model.save(model.collect(java.util.UUID.randomUUID(), model.observed.target(), java.util.UUID.randomUUID(), WORKER));
        assertThat(model.usable(body)).isFalse();
    }
    @Test void P02AcceptedDoesNotAuthorizeAnotherExecution() { model.provider = model.accept("test-logical-notification", "test-payload-1", "test-recipient"); assertThat(model.mayRetry(model.provider)).isFalse(); }
    @Test void P03LostAcceptanceResponseRemainsUnknown() {
        var accepted = model.accept("test-logical-notification", "test-payload-1", "test-recipient");
        model.provider = model.receipt(accepted.key(), accepted.payload(), accepted.recipient(), Acceptance.UNKNOWN);
        assertThat(model.mayRetry(model.provider)).isFalse(); assertThat(model.provider.acceptance()).isEqualTo(Acceptance.UNKNOWN);
        assertThat(model.accept(accepted.key(), accepted.payload(), accepted.recipient()).acceptanceId()).isEqualTo(accepted.acceptanceId());
    }
    @Test void P04RejectsUnavailableOrContradictoryObservation() {
        model.providerAvailable = false; assertThat(model.mayRetry(model.provider)).isFalse(); model.providerAvailable = true;
        model.provider = model.receipt("test-logical-notification", "test-payload-1", "test-recipient", Acceptance.CONTRADICTORY);
        assertThat(model.mayRetry(model.provider)).isFalse();
        model.accept(model.expectedNotificationKey, model.expectedPayload, model.expectedRecipient);
        model.provider = model.receipt(model.expectedNotificationKey, model.expectedPayload, model.expectedRecipient, Acceptance.NOT_ACCEPTED);
        assertThat(model.mayRetry(model.provider)).isFalse(); // Independent stub ledger contradicts claimed non-acceptance.
    }
    @Test void P05SameKeyAndPayloadResolveSameAcceptance() { var first = model.accept("key", "payload", "recipient"); assertThat(model.accept("key", "payload", "recipient")).isEqualTo(first); }
    @Test void P06SameKeyDifferentPayloadIsRejected() { model.accept("key", "payload", "recipient"); assertThatThrownBy(() -> model.accept("key", "different", "recipient")).isInstanceOf(IllegalStateException.class); }
    @Test void P07RejectsObservationAndKeyExpiry() {
        var p = model.provider; clock.now = FROM.plusSeconds(60); assertThat(model.mayRetry(p)).isFalse();
        clock.now = FROM; model.provider = new ProviderReceipt(p.key(), p.payload(), p.recipient(), p.acceptance(), p.acceptanceId(), window(), FROM);
        assertThat(model.mayRetry(model.provider)).isFalse();
        model.accept("key", "payload", "recipient"); clock.now = FROM.plusSeconds(600);
        assertThatThrownBy(() -> model.accept("key", "payload", "recipient")).isInstanceOf(IllegalStateException.class);
    }
    @Test void P08KeepsDifferentNotificationsDistinct() {
        var first = model.accept("key1", "payload", "recipient"); var second = model.accept("key2", "payload", "recipient");
        assertThat(first.acceptanceId()).isNotEqualTo(second.acceptanceId());
        assertThatThrownBy(() -> model.accept("key1", "payload", "other-recipient")).isInstanceOf(IllegalStateException.class);
        assertThat(model.validProvider(first)).isFalse();
        model.provider = first; assertThat(model.validProvider(first)).isFalse(); // Latest receipt cannot change the target's key binding.
        model.provider = model.receipt(model.expectedNotificationKey, model.expectedPayload, "other-recipient", Acceptance.NOT_ACCEPTED);
        assertThat(model.validProvider(model.provider)).isFalse();
        model.provider = model.receipt(model.expectedNotificationKey, "other-payload", model.expectedRecipient, Acceptance.NOT_ACCEPTED);
        assertThat(model.validProvider(model.provider)).isFalse();
    }
}
