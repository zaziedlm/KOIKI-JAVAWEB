package org.koikifw.reference.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.koikifw.referenceacceptance.notification.ManagedRecoveryTestSupport.*;
import static org.koikifw.reference.notification.application.port.outbound.RecoveryScopePort.Decision.*;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.koikifw.reference.notification.adapter.outbound.configuration.ManagedRecoveryScopeAdapter;
import org.koikifw.reference.notification.adapter.outbound.configuration.ManagedRecoverySnapshot;
import org.koikifw.referenceacceptance.notification.NotificationFoundationDbHarness.MutableClock;

class ManagedRecoveryScopeAdapterTest {
    private final MutableClock clock = new MutableClock();
    private final ManagedRecoverySnapshot snapshot = new ManagedRecoverySnapshot("test-r1", "a".repeat(64), ENV, FROM, FROM.plusSeconds(1800),
            Duration.ofMinutes(5), ManagedRecoverySnapshot.CAPABILITIES.stream().map(code -> new ManagedRecoverySnapshot.Grant(USER, code, PUBLICATION)).toList());
    private final ManagedRecoveryScopeAdapter adapter = new ManagedRecoveryScopeAdapter(snapshot, clock);
    @Test void S01AllowsEachExplicitCapability() { for (String code : ManagedRecoverySnapshot.CAPABILITIES) assertThat(adapter.check(USER, code, ENV, PUBLICATION)).isEqualTo(ALLOWED); }
    @Test void S02RejectsOtherUser() { assertThat(adapter.check(UUID.randomUUID(), "NOTIFICATION:PERMIT:READ", ENV, PUBLICATION)).isEqualTo(OUTSIDE); }
    @Test void S03RejectsUnassignedOrUnknownCapability() {
        var reader = new ManagedRecoveryScopeAdapter(new ManagedRecoverySnapshot("test-r1", "a".repeat(64), ENV, FROM, FROM.plusSeconds(1800), Duration.ofMinutes(5),
                List.of(new ManagedRecoverySnapshot.Grant(USER, "NOTIFICATION:PERMIT:READ", PUBLICATION))), clock);
        assertThat(reader.check(USER, "NOTIFICATION:PERMIT:ISSUE", ENV, PUBLICATION)).isEqualTo(OUTSIDE);
        assertThat(reader.check(USER, "*", ENV, PUBLICATION)).isEqualTo(OUTSIDE);
    }
    @Test void S04RejectsOtherPublication() { assertThat(adapter.check(USER, "NOTIFICATION:PERMIT:READ", ENV, UUID.randomUUID())).isEqualTo(OUTSIDE); }
    @Test void S05RejectsOtherEnvironment() { assertThat(adapter.check(USER, "NOTIFICATION:PERMIT:READ", "other", PUBLICATION)).isEqualTo(OUTSIDE); }
    @Test void S06UnavailableBeforeStart() { clock.now = FROM.minusNanos(1); assertThat(adapter.check(USER, "NOTIFICATION:PERMIT:READ", ENV, PUBLICATION)).isEqualTo(UNAVAILABLE); }
    @Test void S07AllowsAtStart() { clock.now = FROM; assertThat(adapter.check(USER, "NOTIFICATION:PERMIT:READ", ENV, PUBLICATION)).isEqualTo(ALLOWED); }
    @Test void S08UnavailableAtAndAfterEnd() { for (var time : List.of(FROM.plusSeconds(1800), FROM.plusSeconds(1801))) { clock.now = time; assertThat(adapter.check(USER, "NOTIFICATION:PERMIT:READ", ENV, PUBLICATION)).isEqualTo(UNAVAILABLE); } }
}
