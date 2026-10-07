package org.koikifw.reference.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.koikifw.referenceacceptance.notification.ManagedRecoveryTestSupport.*;

import java.nio.file.Path;
import java.time.Clock;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.koikifw.reference.notification.adapter.outbound.configuration.ManagedRecoverySnapshot;
import org.koikifw.reference.notification.application.RecoveryPermitService;
import org.koikifw.reference.notification.application.port.outbound.RecoveryEvidencePort;
import org.koikifw.reference.notification.application.port.outbound.RecoveryScopePort;
import org.koikifw.reference.notification.application.port.outbound.RecoveryTtlPolicyPort;
import org.koikifw.reference.notification.configuration.ManagedRecoveryConfiguration;
import org.koikifw.referenceacceptance.notification.NotificationDbTest;
import org.koikifw.referenceacceptance.notification.NotificationFoundationDbHarness.Mode;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class ManagedRecoveryConfigurationRegistrationTest extends NotificationDbTest {
    @TempDir Path directory;
    @Test void R01DisabledNeverReadsManifest() {
        new ApplicationContextRunner().withUserConfiguration(ManagedRecoveryConfiguration.class)
                .withPropertyValues("koiki.reference.notification.foundation.enabled=false",
                        ManagedRecoveryConfiguration.PREFIX + "mode=invalid", ManagedRecoveryConfiguration.PREFIX + "path=missing")
                .run(context -> { assertThat(context).hasNotFailed(); assertThat(context).doesNotHaveBean(ManagedRecoverySnapshot.class); assertThat(context).doesNotHaveBean(RecoveryScopePort.class); });
    }
    @Test void R02DefaultRetainsUnconnectedPorts() {
        try (var context = db.open(Mode.PERMIT, false)) {
            assertThat(context.getBean(RecoveryScopePort.class).check(USER, "NOTIFICATION:PERMIT:READ", ENV, PUBLICATION))
                    .isEqualTo(RecoveryScopePort.Decision.UNAVAILABLE);
            assertThat(context.getBean(RecoveryTtlPolicyPort.class).durationFor(target())).isEmpty();
        }
    }
    @Test void R03RegistersOneManagedAdapterPerPort() throws Exception {
        var path = file(directory, json(grant("READ")));
        try (var context = db.open(Mode.PERMIT, false, properties(path), false)) {
            assertThat(context.getBeansOfType(RecoveryScopePort.class)).hasSize(1);
            assertThat(context.getBeansOfType(RecoveryTtlPolicyPort.class)).hasSize(1);
            assertThat(context.getBean(RecoveryScopePort.class).check(USER, "NOTIFICATION:PERMIT:READ", ENV, PUBLICATION)).isEqualTo(RecoveryScopePort.Decision.ALLOWED);
            assertThat(context.getBean(Clock.class)).isSameAs(db.clock);
        }
    }
    @Test void R04RejectsInvalidModeAndExplicitMissingFile() {
        assertThatThrownBy(() -> db.open(Mode.PERMIT, false, java.util.Map.of(ManagedRecoveryConfiguration.PREFIX + "mode", "other"), false)).isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> db.open(Mode.PERMIT, false, java.util.Map.of(ManagedRecoveryConfiguration.PREFIX + "mode", "file"), false)).isInstanceOf(RuntimeException.class);
    }
    @Test void R05DoesNotSupplyOperationalTargetOrEvidence() throws Exception {
        var path = file(directory, json(grant("ISSUE")));
        try (var context = db.open(Mode.PERMIT, false, properties(path), false)) {
            assertThat(context.getBean(RecoveryEvidencePort.class).consumption(java.util.UUID.randomUUID(), target(), java.util.UUID.randomUUID(), "worker")).isEmpty();
            assertThatThrownBy(() -> context.getBean(RecoveryPermitService.class).issue(target(), "TEST_REASON")).isInstanceOf(IllegalStateException.class);
            assertThat(db.rows("SELECT count(*) FROM kkref_notification_recovery_permit")).containsExactly("0");
        }
    }
    @Test void R06PreservesExistingEntityAndExcludesEntrypoints() throws Exception {
        var path = file(directory, json(grant("READ")));
        try (var context = db.open(Mode.PERMIT, false, properties(path), false, ExistingPersistence.class)) {
            var entities = context.getBean(jakarta.persistence.EntityManagerFactory.class).getMetamodel().getEntities();
            assertThat(entities.stream().map(entity -> entity.getJavaType().getSimpleName()).toList())
                    .contains("IdentityUserEntity", "DepartmentEntity", "ExpenseRequest", "RecoveryPermit", "RecoveryConsumption");
            for (String name : context.getBeanDefinitionNames()) assertThat(name.toLowerCase(java.util.Locale.ROOT)).doesNotContain("notificationrunner", "notificationsender", "notificationscheduler", "notificationlistener");
            assertThat(context.getBeansOfType(org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping.class)).isEmpty();
        }
    }
}
