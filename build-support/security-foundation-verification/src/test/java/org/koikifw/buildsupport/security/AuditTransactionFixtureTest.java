package org.koikifw.buildsupport.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.koikifw.audit.AuditActor;
import org.koikifw.audit.AuditEvent;
import org.koikifw.audit.AuditRecordingException;
import org.koikifw.audit.AuditResult;
import org.koikifw.audit.BusinessAuditRecorder;
import org.koikifw.audit.SecurityAuditRecorder;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.UnexpectedRollbackException;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(
        classes = AuditTransactionFixtureTest.FixtureApplication.class,
        properties = {
            "spring.jpa.hibernate.ddl-auto=validate",
            "spring.jpa.open-in-view=false",
            "spring.main.web-application-type=none"
        })
class AuditTransactionFixtureTest {

    private static final UUID BUSINESS_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000101");

    @Autowired
    private JdbcClient jdbcClient;

    @Autowired
    private FixtureOperations operations;

    @Autowired
    private BusinessAuditRecorder businessAuditRecorder;

    @Autowired
    private SecurityFailurePaths securityFailurePaths;

    @BeforeEach
    void clearFixtureState() {
        jdbcClient.sql("DELETE FROM fixture_business_change").update();
        jdbcClient.sql("DELETE FROM koiki_audit_event").update();
        securityFailurePaths.reset();
        MDC.clear();
    }

    @Test
    void commitsBusinessChangeAndAuditTogether() {
        operations.commitBusiness();

        assertThat(businessCount()).isEqualTo(1);
        assertThat(auditCount("BUSINESS", "BUSINESS_CREATED")).isEqualTo(1);
    }

    @Test
    void rollsBackBusinessChangeAndAuditTogether() {
        assertThatThrownBy(operations::rollbackBusiness)
                .isInstanceOf(IntentionalRollback.class);

        assertThat(businessCount()).isZero();
        assertThat(auditCount("BUSINESS", "BUSINESS_ROLLED_BACK")).isZero();
    }

    @Test
    void auditFailureMarksBusinessTransactionRollbackOnlyEvenWhenCaught() {
        assertThatThrownBy(operations::catchBusinessAuditFailure)
                .isInstanceOf(UnexpectedRollbackException.class);

        assertThat(businessCount()).isZero();
        assertThat(auditCount("BUSINESS", "FORCE_AUDIT_FAILURE")).isZero();
    }

    @Test
    void securityAuditSurvivesOuterTransactionRollback() {
        assertThatThrownBy(operations::recordSecurityThenRollback)
                .isInstanceOf(IntentionalRollback.class);

        assertThat(businessCount()).isZero();
        assertThat(auditCount("SECURITY", "LOGIN_FAILED")).isEqualTo(1);
    }

    @Test
    void distinguishesFailClosedAndContinueAfterSecurityAuditFailure() {
        assertThatThrownBy(securityFailurePaths::failClosed)
                .isInstanceOf(AuditRecordingException.class)
                .hasMessage("Audit recording failed.")
                .hasNoCause();
        assertThat(securityFailurePaths.failClosedContinuation()).isFalse();

        securityFailurePaths.continueAndAlert();
        assertThat(securityFailurePaths.defensiveOperationCompleted()).isTrue();
        assertThat(securityFailurePaths.alertRaised()).isTrue();
        assertThat(auditCount("SECURITY", "FORCE_AUDIT_FAILURE")).isZero();
    }

    @Test
    void rejectsBusinessAuditOutsideTransaction() {
        AuditEvent event = AuditEvent.of(
                "BUSINESS_OUTSIDE_TRANSACTION",
                AuditActor.system("fixture-system"),
                "CREATE",
                AuditResult.SUCCESS);

        assertThatThrownBy(() -> businessAuditRecorder.record(event))
                .isInstanceOf(AuditRecordingException.class)
                .hasMessage("Audit recording failed.")
                .hasNoCause();
        assertThat(auditCount("BUSINESS", "BUSINESS_OUTSIDE_TRANSACTION")).isZero();
    }

    @Test
    void capturesCorrelationWithoutAcceptingItFromAuditPayload() {
        MDC.put("requestId", "request-t4-001");
        MDC.put("traceId", "trace-t4-001");
        try {
            operations.recordSecurity();
        } finally {
            MDC.clear();
        }

        String requestId = jdbcClient.sql(
                        "SELECT request_id FROM koiki_audit_event WHERE event_type = 'AUTHORIZATION_DENIED'")
                .query(String.class)
                .single();
        String traceId = jdbcClient.sql(
                        "SELECT trace_id FROM koiki_audit_event WHERE event_type = 'AUTHORIZATION_DENIED'")
                .query(String.class)
                .single();
        assertThat(requestId).isEqualTo("request-t4-001");
        assertThat(traceId).isEqualTo("trace-t4-001");
    }

    @Test
    void rejectsRawEmailActorAndKeepsValueToStringRedacted() {
        String rawEmail = String.join("", "presented-user", Character.toString(64), "invalid.example");

        assertThatThrownBy(() -> AuditActor.user(rawEmail))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("A user audit actor must not be an email address.");

        AuditEvent event = AuditEvent.of(
                        "SAFE_EVENT",
                        AuditActor.user("framework-user-001"),
                        "READ",
                        AuditResult.SUCCESS)
                .withResource("SAFE_RESOURCE", "resource-001");
        assertThat(event.toString()).isEqualTo("AuditEvent[redacted]");
        assertThat(event.actor().toString()).isEqualTo("AuditActor[redacted]");
    }

    private int businessCount() {
        return jdbcClient.sql("SELECT COUNT(*) FROM fixture_business_change")
                .query(Integer.class)
                .single();
    }

    private int auditCount(String auditType, String eventType) {
        return jdbcClient.sql(
                        "SELECT COUNT(*) FROM koiki_audit_event WHERE audit_type = :auditType AND event_type = :eventType")
                .param("auditType", auditType)
                .param("eventType", eventType)
                .query(Integer.class)
                .single();
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import(AuditPostgreSqlTestConfiguration.class)
    static class FixtureApplication {

        @Bean
        FixtureOperations fixtureOperations(
                JdbcClient jdbcClient,
                BusinessAuditRecorder businessAuditRecorder,
                SecurityAuditRecorder securityAuditRecorder) {
            return new FixtureOperations(jdbcClient, businessAuditRecorder, securityAuditRecorder);
        }

        @Bean
        SecurityFailurePaths securityFailurePaths(SecurityAuditRecorder securityAuditRecorder) {
            return new SecurityFailurePaths(securityAuditRecorder);
        }
    }

    static class FixtureOperations {

        private final JdbcClient jdbcClient;
        private final BusinessAuditRecorder businessAuditRecorder;
        private final SecurityAuditRecorder securityAuditRecorder;

        FixtureOperations(
                JdbcClient jdbcClient,
                BusinessAuditRecorder businessAuditRecorder,
                SecurityAuditRecorder securityAuditRecorder) {
            this.jdbcClient = jdbcClient;
            this.businessAuditRecorder = businessAuditRecorder;
            this.securityAuditRecorder = securityAuditRecorder;
        }

        @Transactional
        public void commitBusiness() {
            insertBusiness("commit");
            businessAuditRecorder.record(businessEvent("BUSINESS_CREATED"));
        }

        @Transactional
        public void rollbackBusiness() {
            insertBusiness("rollback");
            businessAuditRecorder.record(businessEvent("BUSINESS_ROLLED_BACK"));
            throw new IntentionalRollback();
        }

        @Transactional
        public void catchBusinessAuditFailure() {
            insertBusiness("audit-failure");
            try {
                businessAuditRecorder.record(businessEvent("FORCE_AUDIT_FAILURE"));
            } catch (AuditRecordingException expected) {
                // The transaction must remain rollback-only even when a caller catches this failure.
            }
        }

        @Transactional
        public void recordSecurityThenRollback() {
            insertBusiness("outer-rollback");
            securityAuditRecorder.record(securityEvent("LOGIN_FAILED"));
            throw new IntentionalRollback();
        }

        public void recordSecurity() {
            securityAuditRecorder.record(securityEvent("AUTHORIZATION_DENIED"));
        }

        private void insertBusiness(String value) {
            jdbcClient.sql(
                            "INSERT INTO fixture_business_change(change_id, change_value) VALUES (:id, :value)")
                    .param("id", BUSINESS_ID)
                    .param("value", value)
                    .update();
        }

        private static AuditEvent businessEvent(String eventType) {
            return AuditEvent.of(
                            eventType,
                            AuditActor.user("framework-user-001"),
                            "CREATE",
                            AuditResult.SUCCESS)
                    .withResource("FIXTURE_BUSINESS", BUSINESS_ID.toString());
        }

        private static AuditEvent securityEvent(String eventType) {
            return AuditEvent.of(
                            eventType,
                            AuditActor.anonymous(),
                            "AUTHENTICATE",
                            AuditResult.FAILURE)
                    .withReason("SAFE_INTERNAL_REASON");
        }
    }

    static class SecurityFailurePaths {

        private final SecurityAuditRecorder securityAuditRecorder;
        private final AtomicBoolean failClosedContinuation = new AtomicBoolean();
        private final AtomicBoolean defensiveOperationCompleted = new AtomicBoolean();
        private final AtomicBoolean alertRaised = new AtomicBoolean();

        SecurityFailurePaths(SecurityAuditRecorder securityAuditRecorder) {
            this.securityAuditRecorder = securityAuditRecorder;
        }

        void failClosed() {
            securityAuditRecorder.record(failingEvent());
            failClosedContinuation.set(true);
        }

        void continueAndAlert() {
            try {
                securityAuditRecorder.record(failingEvent());
            } catch (AuditRecordingException expected) {
                alertRaised.set(true);
            }
            defensiveOperationCompleted.set(true);
        }

        boolean failClosedContinuation() {
            return failClosedContinuation.get();
        }

        boolean defensiveOperationCompleted() {
            return defensiveOperationCompleted.get();
        }

        boolean alertRaised() {
            return alertRaised.get();
        }

        void reset() {
            failClosedContinuation.set(false);
            defensiveOperationCompleted.set(false);
            alertRaised.set(false);
        }

        private static AuditEvent failingEvent() {
            return AuditEvent.of(
                    "FORCE_AUDIT_FAILURE",
                    AuditActor.anonymous(),
                    "AUTHENTICATE",
                    AuditResult.FAILURE);
        }
    }

    static final class IntentionalRollback extends RuntimeException {

        private static final long serialVersionUID = 1L;
    }
}
