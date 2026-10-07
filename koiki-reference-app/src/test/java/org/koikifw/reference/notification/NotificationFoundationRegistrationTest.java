package org.koikifw.reference.notification;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManagerFactory;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.koikifw.reference.notification.application.RecoveryPermitService;
import org.koikifw.reference.notification.application.port.outbound.RecoveryEvidencePort;
import org.koikifw.reference.notification.application.port.outbound.RecoveryScopePort;
import org.koikifw.reference.notification.domain.repository.RecoveryPermitRepository;
import org.koikifw.reference.notification.domain.repository.RecoveryConsumptionRepository;
import org.koikifw.referenceacceptance.notification.NotificationFoundationDbHarness;
import org.koikifw.referenceacceptance.notification.NotificationFoundationDbHarness.Mode;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/** Adopted R01-R07; normal Servlet startup is retained alongside finite integration checks. */
class NotificationFoundationRegistrationTest {
    private static NotificationFoundationDbHarness db;
    @BeforeAll
    static void startDatabase() throws Exception {
        NotificationFoundationMigrationTest.startDatabase();
        db = new NotificationFoundationDbHarness(NotificationFoundationMigrationTest.POSTGRES);
    }

    @AfterAll
    static void stopDatabase() {
        try { if (db != null) db.close(); }
        finally { NotificationFoundationMigrationTest.stopDatabase(); }
    }

    @BeforeEach void prepare() throws Exception { db.reset(); }
    @AfterEach void closeRuntimeContexts() { db.close(); }

    @Test
    void omitsFoundationWhenPropertyIsAbsent() throws Exception {
        try (var context = NotificationFoundationMigrationTest.start(
                NotificationFoundationMigrationTest.newDatabase(), "absent", false)) {
            NotificationFoundationMigrationTest.assertDisabled(context);
        }
    }

    @Test
    void omitsFoundationWhenPropertyIsFalse() throws Exception {
        try (var context = NotificationFoundationMigrationTest.start(
                NotificationFoundationMigrationTest.newDatabase(), "false", false)) {
            NotificationFoundationMigrationTest.assertDisabled(context);
        }
    }

    @Test
    void rejectsInvalidEnablementProperty() throws Exception {
        String url = NotificationFoundationMigrationTest.newDatabase();
        assertThatThrownBy(() -> NotificationFoundationMigrationTest.start(url, "invalid", false))
                .isInstanceOf(RuntimeException.class).hasStackTraceContaining("Invalid notification foundation enabled property");
    }

    @Test
    void rejectsOperationsWithoutScopeOrTtlPolicy() throws Exception {
        var target = db.target();
        try (var context = db.open(Mode.PERMIT, false)) {
            assertThat(context.isActive()).isTrue();
            assertThat(context.getBean(RecoveryScopePort.class).check(NotificationFoundationDbHarness.USER,
                    "NOTIFICATION:PERMIT:ISSUE", target.environmentId(), target.publicationId()))
                    .isEqualTo(RecoveryScopePort.Decision.UNAVAILABLE);
            assertThatThrownBy(() -> context.getBean(RecoveryPermitService.class).issue(target, "TEST_REASON"))
                    .isInstanceOf(IllegalStateException.class).hasMessage("Recovery access denied");
        }
        db.ports.ttl = Optional.empty();
        assertThatThrownBy(() -> db.issue(target)).isInstanceOf(IllegalStateException.class).hasMessage("Recovery operation unavailable");
        assertThat(db.rows("SELECT count(*) FROM kkref_notification_recovery_permit")).containsExactly("0");
    }

    @Test
    void rejectsOperationsWithoutTargetOrOperationalProof() throws Exception {
        var target = db.target(); db.ports.snapshots.clear();
        assertThatThrownBy(() -> db.issue(target)).isInstanceOf(IllegalStateException.class).hasMessage("Recovery operation unavailable");
        db.ports.snapshots.put(target.publicationId(), target);
        var id = db.issue(target); var operation = UUID.randomUUID();
        assertThatThrownBy(() -> db.service(Mode.CONSUMER).consume(id, target, operation, "test-worker"))
                .isInstanceOf(IllegalStateException.class).hasMessage("Recovery operation unavailable");
        assertThatThrownBy(() -> db.service(Mode.PERMIT).close(id, target, "test-proof"))
                .isInstanceOf(IllegalStateException.class).hasMessage("Recovery operation unavailable");
        db.ports.consumptionProofs.put(id, new RecoveryEvidencePort.ConsumptionProof(id, target, UUID.randomUUID(), "test-worker", "test-proof"));
        db.ports.closureProofs.put(id, new RecoveryEvidencePort.ClosureProof(id, target, UUID.randomUUID(), "test-proof"));
        assertThatThrownBy(() -> db.service(Mode.CONSUMER).consume(id, target, operation, "test-worker"))
                .isInstanceOf(IllegalStateException.class).hasMessage("Recovery operation unavailable");
        assertThatThrownBy(() -> db.service(Mode.PERMIT).close(id, target, "test-proof"))
                .isInstanceOf(IllegalStateException.class).hasMessage("Recovery operation unavailable");
        assertThat(db.rows("SELECT count(*) FROM kkref_notification_recovery_consumption")).containsExactly("0");
        assertThat(db.rows("SELECT closed_at,version FROM kkref_notification_recovery_permit")).containsExactly("null|0");
    }

    @Test
    void registersOnlyRequiredFoundationComponents() throws Exception {
        try (var context = NotificationFoundationMigrationTest.start(NotificationFoundationMigrationTest.newDatabase(), "true", true)) {
            assertThat(context.getBeansOfType(RecoveryPermitService.class)).hasSize(1);
            assertThat(context.getBeansOfType(RecoveryPermitRepository.class)).hasSize(1);
            assertThat(context.getBeansOfType(RecoveryConsumptionRepository.class)).hasSize(1);
            var types = context.getBean(EntityManagerFactory.class).getMetamodel().getEntities().stream()
                    .map(entity -> entity.getJavaType().getSimpleName()).toList();
            assertThat(types).contains("RecoveryPermit", "RecoveryConsumption", "IdentityUserEntity", "DepartmentEntity", "ExpenseRequest");
            var target = db.target();
            assertThatThrownBy(() -> context.getBean(RecoveryPermitService.class).issue(target, "TEST_REASON"))
                    .isInstanceOf(IllegalStateException.class).hasMessage("Recovery access denied");
        }
    }

    @Test
    void registersNoDeliveryOrRecoveryInfrastructure() throws Exception {
        try (var context = NotificationFoundationMigrationTest.start(NotificationFoundationMigrationTest.newDatabase(), "true", true)) {
            var notificationTypes = java.util.Arrays.stream(context.getBeanDefinitionNames())
                    .map(context::getType).filter(java.util.Objects::nonNull)
                    .filter(type -> type.getName().startsWith("org.koikifw.reference.notification."))
                    .map(type -> type.getSimpleName().toLowerCase(java.util.Locale.ROOT)).toList();
            assertThat(notificationTypes).noneMatch(name -> name.contains("sender") || name.contains("runner")
                    || name.contains("registry") || name.contains("scheduler") || name.contains("listener"));
            var mappings = context.getBean(RequestMappingHandlerMapping.class).getHandlerMethods().values();
            assertThat(mappings).noneMatch(handler -> handler.getBeanType().getName().startsWith("org.koikifw.reference.notification."));
        }
    }
}
