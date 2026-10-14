package org.koikifw.reference.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.koikifw.referenceacceptance.notification.NotificationFoundationDbHarness.Mode.*;
import static org.koikifw.referenceacceptance.notification.NotificationFoundationDbHarness.USER;

import jakarta.persistence.EntityManager;
import java.sql.SQLException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.koikifw.reference.notification.domain.model.RecoveryConsumption;
import org.koikifw.reference.notification.domain.model.RecoveryPermit;
import org.koikifw.reference.notification.domain.repository.RecoveryConsumptionRepository;
import org.koikifw.reference.notification.domain.repository.RecoveryPermitRepository;
import org.koikifw.referenceacceptance.notification.NotificationDbTest;
import org.koikifw.referenceacceptance.notification.NotificationFoundationDbHarness;
import org.koikifw.referenceacceptance.notification.NotificationFoundationDbHarness.Mode;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Adopted D01-D12. Setup/evidence is admin-only; business operations use restricted roles. */
@EnabledIfSystemProperty(named = "koiki.reference.verification.resource-limits.enabled", matches = "true")
class NotificationFoundationPersistenceTest extends NotificationDbTest {
    @Test void rejectsUpdatesToPermitTargetColumns() throws Exception {
        var target = db.target(); db.issue(target);
        for (String column : new String[] {"environment_id", "publication_id", "event_id", "listener_id", "expected_attempt"}) {
            for (Mode mode : Mode.values()) deniedSql(mode, "UPDATE kkref_notification_recovery_permit SET " + column + "=" + column);
        }
        deniedSql(CONSUMER, "INSERT INTO kkref_notification_recovery_permit(permit_id) VALUES (gen_random_uuid())");
        deniedSql(READER, "INSERT INTO kkref_notification_recovery_permit(permit_id) VALUES (gen_random_uuid())");
        deniedSql(READER, "CREATE TABLE s1_runtime_ddl_forbidden(id int)");
        deniedSql(PERMIT, "SELECT * FROM koiki_password_credential");
        deniedSql(CONSUMER, "SELECT * FROM koiki_login_attempt");
        deniedSql(READER, "SELECT * FROM koiki_session");
        assertThat(db.rows("SELECT count(*) FROM kkref_notification_recovery_permit")).containsExactly("1");
    }
    @Test void rejectsUpdatesToPermitIssuerAndReason() throws Exception {
        var target = db.target(); db.issue(target);
        for (String column : new String[] {"permit_id", "actor_id", "reason_code"}) {
            for (Mode mode : Mode.values()) deniedSql(mode, "UPDATE kkref_notification_recovery_permit SET " + column + "=" + column);
        }
        for (Mode mode : Mode.values()) {
            deniedSql(mode, "DELETE FROM kkref_notification_recovery_permit");
            deniedSql(mode, "TRUNCATE kkref_notification_recovery_permit CASCADE");
            deniedSql(mode, "SELECT * FROM koiki_audit_event");
        }
    }
    @Test void rejectsUpdatesToPermitValidityColumns() throws Exception {
        var target = db.target(); var id = db.issue(target);
        for (String column : new String[] {"issued_at", "expires_at"}) {
            for (Mode mode : Mode.values()) deniedSql(mode, "UPDATE kkref_notification_recovery_permit SET " + column + "=" + column);
        }
        var view = db.service(READER).read(id, target.environmentId(), target.publicationId());
        assertThat(view.issuedAt()).isEqualTo(NotificationFoundationDbHarness.NOW.truncatedTo(ChronoUnit.MICROS));
        assertThat(view.expiresAt()).isEqualTo(NotificationFoundationDbHarness.NOW.plusSeconds(60).truncatedTo(ChronoUnit.MICROS));
        var operation = UUID.randomUUID(); db.proof(id, target, operation, "test-worker");
        db.clock.now = view.expiresAt();
        assertThatThrownBy(() -> db.service(CONSUMER).consume(id, target, operation, "test-worker"))
                .isInstanceOf(IllegalStateException.class).hasMessage("Recovery operation unavailable");
        db.clock.now = view.expiresAt().plusNanos(1);
        assertThatThrownBy(() -> db.service(CONSUMER).consume(id, target, operation, "test-worker"))
                .isInstanceOf(IllegalStateException.class).hasMessage("Recovery operation unavailable");
        assertThat(db.rows("SELECT count(*) FROM kkref_notification_recovery_consumption")).containsExactly("0");
        db.clock.now = view.expiresAt().minusNanos(1);
        db.service(CONSUMER).consume(id, target, operation, "test-worker");
        assertThat(db.service(READER).read(id, target.environmentId(), target.publicationId()).expiresAt()).isEqualTo(view.expiresAt());
    }
    @Test void rejectsConsumptionUpdate() throws Exception {
        consumed();
        var before = db.rows("SELECT * FROM kkref_notification_recovery_consumption");
        for (Mode mode : Mode.values()) deniedSql(mode, "UPDATE kkref_notification_recovery_consumption SET worker_generation='replacement'");
        deniedSql(PERMIT, "INSERT INTO kkref_notification_recovery_consumption(permit_id) VALUES (gen_random_uuid())");
        deniedSql(READER, "INSERT INTO kkref_notification_recovery_consumption(permit_id) VALUES (gen_random_uuid())");
        assertThat(db.rows("SELECT * FROM kkref_notification_recovery_consumption")).isEqualTo(before);
    }
    @Test void rejectsConsumptionDelete() throws Exception {
        consumed();
        var before = db.rows("SELECT * FROM kkref_notification_recovery_consumption");
        for (Mode mode : Mode.values()) deniedSql(mode, "DELETE FROM kkref_notification_recovery_consumption");
        assertThat(db.rows("SELECT * FROM kkref_notification_recovery_consumption")).isEqualTo(before);
    }
    @Test void rejectsConsumptionTruncate() throws Exception {
        consumed();
        var before = db.rows("SELECT * FROM kkref_notification_recovery_consumption");
        for (Mode mode : Mode.values()) deniedSql(mode, "TRUNCATE kkref_notification_recovery_consumption");
        assertThat(db.rows("SELECT * FROM kkref_notification_recovery_consumption")).isEqualTo(before);
    }
    @Test void preventsNextPermitForUnclosedTarget() throws Exception {
        var target = db.target(); var id = db.issue(target);
        assertThatThrownBy(() -> db.issue(target)).isInstanceOf(RuntimeException.class);
        var operation = UUID.randomUUID(); db.proof(id, target, operation, "test-worker");
        db.service(CONSUMER).consume(id, target, operation, "test-worker");
        db.clock.now = NotificationFoundationDbHarness.NOW.plusSeconds(120);
        var records = db.rows("SELECT * FROM kkref_notification_recovery_consumption");
        assertThatThrownBy(() -> db.issue(target)).isInstanceOf(RuntimeException.class);
        assertThat(db.rows("SELECT count(*) FROM kkref_notification_recovery_permit WHERE closed_at IS NULL")).containsExactly("1");
        assertThat(db.rows("SELECT * FROM kkref_notification_recovery_consumption")).isEqualTo(records);
    }
    @Test void enforcesUniqueOperationAcrossPermits() throws Exception {
        var first = db.target(); var second = db.target(); var firstId = db.issue(first); var secondId = db.issue(second);
        var operation = UUID.randomUUID(); db.proof(firstId, first, operation, "test-worker"); db.proof(secondId, second, operation, "test-worker");
        db.service(CONSUMER).consume(firstId, first, operation, "test-worker");
        assertThatThrownBy(() -> db.service(CONSUMER).consume(secondId, second, operation, "test-worker")).isInstanceOf(RuntimeException.class);
        assertThat(db.rows("SELECT permit_id::text FROM kkref_notification_recovery_consumption")).containsExactly(firstId.toString());
        assertThat(db.rows("SELECT version FROM kkref_notification_recovery_permit WHERE permit_id='" + secondId + "'")).containsExactly("0");
    }
    @Test void commitsOnlyOneCompetingConsumption() throws Exception {
        var target = db.target(); var id = db.issue(target); var operation = UUID.randomUUID(); db.proof(id, target, operation, "test-worker");
        db.context(CONSUMER);
        assertThat(race(() -> db.service(CONSUMER).consume(id, target, operation, "test-worker"))).isEqualTo(1);
        assertThat(db.rows("SELECT count(*) FROM kkref_notification_recovery_consumption")).containsExactly("1");
        assertThat(db.rows("SELECT version FROM kkref_notification_recovery_permit")).containsExactly("1");
        assertThat(db.rows("SELECT count(*) FROM koiki_audit_event WHERE action='PERMIT_CONSUMED'")).containsExactly("1");
    }
    @Test void preservesClosureUnderCompetition() throws Exception {
        var target = db.target(); var id = db.issue(target); db.closure(id, target, "test-proof");
        assertThat(race(() -> db.service(PERMIT).close(id, target, "test-proof"))).isEqualTo(1);
        assertThat(db.rows("SELECT confirmed_by::text,result_ref,version FROM kkref_notification_recovery_permit"))
                .containsExactly(USER + "|test-proof|1");
        assertThat(db.rows("SELECT count(*) FROM koiki_audit_event WHERE action='PERMIT_CLOSED'")).containsExactly("1");
        // Both consumption and closure start with the same scoped permit row lock.
        var next = db.target(); var nextId = db.issue(next); var operation = UUID.randomUUID();
        db.proof(nextId, next, operation, "test-worker"); db.closure(nextId, next, "test-proof");
        db.context(CONSUMER);
        raceDifferent(() -> db.service(CONSUMER).consume(nextId, next, operation, "test-worker"),
                () -> db.service(PERMIT).close(nextId, next, "test-proof"));
        assertThat(db.rows("SELECT result_ref FROM kkref_notification_recovery_permit WHERE permit_id='" + nextId + "'"))
                .containsExactly("test-proof");
        assertThat(db.rows("SELECT version FROM kkref_notification_recovery_permit WHERE permit_id='" + nextId + "'"))
                .allMatch(version -> version.equals("1") || version.equals("2"));
        var staleTarget = db.target(); var staleId = db.issue(staleTarget);
        var stale = db.tx(PERMIT, () -> db.context(PERMIT).getBean(RecoveryPermitRepository.class)
                .findScoped(staleId, staleTarget.environmentId(), staleTarget.publicationId()).orElseThrow());
        db.closure(staleId, staleTarget, "confirmed-proof");
        db.service(PERMIT).close(staleId, staleTarget, "confirmed-proof");
        assertThatThrownBy(() -> db.tx(PERMIT, () -> {
            db.context(PERMIT).getBean(EntityManager.class).merge(stale);
            db.context(PERMIT).getBean(RecoveryPermitRepository.class).flush(); return true;
        })).isInstanceOf(RuntimeException.class);
        assertThat(db.rows("SELECT result_ref,version FROM kkref_notification_recovery_permit WHERE permit_id='" + staleId + "'"))
                .containsExactly("confirmed-proof|1");

    }
    @Test void exposesCommittedRecordsFromAnotherConnection() throws Exception {
        var target = db.target(); var id = db.issue(target); var operation = UUID.randomUUID(); db.proof(id, target, operation, "test-worker");
        db.service(CONSUMER).consume(id, target, operation, "test-worker");
        var view = db.service(READER).read(id, target.environmentId(), target.publicationId());
        assertThat(view.operationId()).isEqualTo(operation);
        assertThat(view.version()).isEqualTo(1);
        assertThatThrownBy(() -> db.tx(CONSUMER, () -> {
            db.context(CONSUMER).getBean(RecoveryConsumptionRepository.class).insert(
                    RecoveryConsumption.record(UUID.randomUUID(), UUID.randomUUID(), "test-worker", Instant.now()));
            db.context(CONSUMER).getBean(RecoveryPermitRepository.class).flush(); return true;
        })).isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> db.service(READER).read(id, "outside-environment", target.publicationId()))
                .isInstanceOf(IllegalStateException.class).hasMessage("Recovery operation unavailable");
    }
    @Test void hidesRolledBackRecordsFromAnotherConnection() throws Exception {
        var target = db.target(); UUID id = UUID.randomUUID();
        assertThatThrownBy(() -> db.tx(PERMIT, () -> {
            var repository = db.context(PERMIT).getBean(RecoveryPermitRepository.class);
            repository.insert(RecoveryPermit.issue(id, target.environmentId(), target.publicationId(), target.eventId(),
                    target.listenerId(), 0, USER, "TEST_REASON", NotificationFoundationDbHarness.NOW, NotificationFoundationDbHarness.NOW.plusSeconds(60)));
            repository.flush();
            throw new IllegalStateException("test rollback");
        })).isInstanceOf(IllegalStateException.class).hasMessage("test rollback");
        assertThat(db.rows("SELECT count(*) FROM kkref_notification_recovery_permit")).containsExactly("0");
        var committed = db.issue(target); var operation = UUID.randomUUID();
        assertThatThrownBy(() -> db.tx(CONSUMER, () -> {
            db.context(CONSUMER).getBean(RecoveryConsumptionRepository.class).insert(
                    RecoveryConsumption.record(committed, operation, "test-worker", NotificationFoundationDbHarness.NOW));
            db.context(CONSUMER).getBean(RecoveryPermitRepository.class).flush();
            throw new IllegalStateException("test rollback");
        })).isInstanceOf(IllegalStateException.class).hasMessage("test rollback");
        assertThat(db.service(READER).read(committed, target.environmentId(), target.publicationId()).operationId()).isNull();
        long started = System.nanoTime();
        assertThatThrownBy(() -> db.tx(CONSUMER, () -> {
            var permits = db.context(CONSUMER).getBean(RecoveryPermitRepository.class);
            var permit = permits.lockScoped(committed, target.environmentId(), target.publicationId()).orElseThrow();
            permits.advanceVersion(permit);
            db.context(CONSUMER).getBean(RecoveryConsumptionRepository.class).insert(
                    RecoveryConsumption.record(committed, operation, "test-worker", NotificationFoundationDbHarness.NOW));
            permits.flush();
            // Real ten-second SQL/transaction timeouts; a slow test SQL is not a production switch.
            db.context(CONSUMER).getBean(EntityManager.class).createNativeQuery("SELECT pg_sleep(11)").getResultList();
            return true;
        })).isInstanceOf(RuntimeException.class);
        assertThat(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started)).isBetween(8500L, 13000L);
        assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
        assertThat(db.service(READER).read(committed, target.environmentId(), target.publicationId()).operationId()).isNull();
        assertThat(db.rows("SELECT version FROM kkref_notification_recovery_permit WHERE permit_id='" + committed + "'"))
                .containsExactly("0");
    }
    private void consumed() {
        var target = db.target(); var id = db.issue(target); var operation = UUID.randomUUID(); db.proof(id, target, operation, "test-worker");
        db.service(CONSUMER).consume(id, target, operation, "test-worker");
    }
    private void deniedSql(Mode mode, String sql) throws Exception {
        try (var connection = db.runtime(mode); var statement = connection.createStatement()) {
            assertThatThrownBy(() -> statement.execute(sql)).isInstanceOf(SQLException.class)
                    .extracting(error -> ((SQLException) error).getSQLState()).isEqualTo("42501");
        }
    }
    private int race(Runnable operation) throws Exception { return raceDifferent(operation, operation); }
    private int raceDifferent(Runnable first, Runnable second) throws Exception {
        CountDownLatch ready = new CountDownLatch(2); CountDownLatch go = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var left = executor.submit(() -> attempt(first, ready, go));
            var right = executor.submit(() -> attempt(second, ready, go));
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue(); go.countDown();
            return left.get(15, TimeUnit.SECONDS) + right.get(15, TimeUnit.SECONDS);
        } finally { go.countDown(); }
    }
    private int attempt(Runnable operation, CountDownLatch ready, CountDownLatch go) throws Exception {
        NotificationFoundationDbHarness.authenticate(USER); ready.countDown();
        try {
            if (!go.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("test barrier timeout");
            try { operation.run(); return 1; } catch (IllegalStateException expectedRejection) { return 0; }
        } finally { SecurityContextHolder.clearContext(); }
    }
}
