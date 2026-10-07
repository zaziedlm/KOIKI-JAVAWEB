package org.koikifw.reference.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.koikifw.referenceacceptance.notification.ManagedRecoveryTestSupport.*;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.koikifw.reference.notification.adapter.outbound.configuration.ManagedRecoveryTtlPolicyAdapter;
import org.koikifw.reference.notification.application.port.outbound.RecoveryTtlPolicyPort;
import org.koikifw.referenceacceptance.notification.NotificationFoundationDbHarness.MutableClock;

class ManagedRecoveryTtlPolicyAdapterTest {
    private final MutableClock clock = new MutableClock();
    private final ManagedRecoveryTtlPolicyAdapter adapter = new ManagedRecoveryTtlPolicyAdapter(snapshot(Duration.ofMinutes(5)), clock);
    @Test void T01ReturnsFiveMinutesOnlyForEnvironment() { assertThat(adapter.durationFor(target())).contains(Duration.ofMinutes(5)); var t = target(); assertThat(adapter.durationFor(new org.koikifw.reference.notification.application.query.RecoveryTarget("other", t.publicationId(), t.eventId(), t.listenerId(), t.expectedAttempt()))).isEmpty(); }
    @Test void T02AllowsMaximum() { assertThat(new ManagedRecoveryTtlPolicyAdapter(snapshot(Duration.ofMinutes(10)), clock).durationFor(target())).contains(Duration.ofMinutes(10)); }
    @Test void T03RejectsAboveMaximum() { assertThatThrownBy(() -> snapshot(Duration.ofMinutes(10).plusNanos(1))).isInstanceOf(IllegalArgumentException.class); }
    @Test void T04RejectsZeroNegativeAndUnconnectedPolicy() {
        assertThatThrownBy(() -> snapshot(Duration.ZERO)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> snapshot(Duration.ofSeconds(-1))).isInstanceOf(IllegalArgumentException.class);
        RecoveryTtlPolicyPort unconnected = t -> java.util.Optional.empty();
        assertThat(unconnected.durationFor(target())).isEmpty(); assertThat(unconnected.allowsIssuanceAt(target(), FROM, FROM.plusSeconds(300))).isFalse();
    }
    @Test void T05RejectsInsufficientRemainingWindow() { clock.now = FROM.plusSeconds(1500).plusNanos(1); assertThat(adapter.durationFor(target())).isEmpty(); }
    @Test void T06AllowsExpiryExactlyAtWindowEnd() { clock.now = FROM.plusSeconds(1500); assertThat(adapter.durationFor(target())).contains(Duration.ofMinutes(5)); assertThat(adapter.allowsIssuanceAt(target(), clock.now, FROM.plusSeconds(1800))).isTrue(); }
    @Test void T07RechecksActualIssueInstant() { var t = target(); clock.now = FROM.plusSeconds(1499); assertThat(adapter.durationFor(t)).isPresent(); clock.now = FROM.plusSeconds(1501); assertThat(adapter.allowsIssuanceAt(t, clock.now, clock.now.plusSeconds(300))).isFalse(); }
    @Test void T08RejectsOverflowAndAlteredExpiry() { assertThat(adapter.allowsIssuanceAt(target(), Instant.MAX, Instant.MAX)).isFalse(); assertThat(adapter.allowsIssuanceAt(target(), FROM, FROM.plusSeconds(301))).isFalse(); assertThat(adapter.allowsIssuanceAt(target(), FROM.minusNanos(1), FROM.plusSeconds(300))).isFalse(); }
}
