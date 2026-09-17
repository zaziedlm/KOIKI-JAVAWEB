package org.koikifw.reference.master;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.koikifw.identity.AuthenticationSource;
import org.koikifw.identity.FrameworkPrincipal;
import org.koikifw.identity.FrameworkUserId;
import org.koikifw.reference.ReferencePostgreSqlTestConfiguration;
import org.koikifw.reference.master.application.MasterAdministration;
import org.koikifw.reference.master.application.MasterFailure;
import org.koikifw.reference.master.application.MasterOperationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

@SpringBootTest(properties = {
    "koiki.identity.local-authentication.enabled=false",
    "spring.session.jdbc.initialize-schema=never"
})
@Import(ReferencePostgreSqlTestConfiguration.class)
class DepartmentDeactivationPostgreSqlIntegrationTest {

    private static final UUID ACTOR_ID = UUID.fromString(
            "32000000-0000-0000-0000-000000000001");
    private static final UUID APPLICANT_ID = UUID.fromString(
            "32000000-0000-0000-0000-000000000002");

    @Autowired
    private MasterAdministration administration;

    @Autowired
    private JdbcClient jdbc;

    @BeforeEach
    void setUp() {
        cleanReferenceData();
        FrameworkPrincipal principal = new TestPrincipal(
                FrameworkUserId.parse(ACTOR_ID.toString()), Set.of("MASTER:ADMIN"));
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(
                        principal,
                        "n/a",
                        Set.of(new SimpleGrantedAuthority("MASTER:ADMIN"))));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        cleanReferenceData();
    }

    @Test
    void rollsBackDepartmentDeactivationForAllFourPendingExpenseStates() {
        for (String status : Set.of("DRAFT", "SUBMITTED", "RETURNED", "APPROVED")) {
            UUID departmentId = administration.createDepartment(
                    "PENDING_" + status, "Pending " + status);
            insertExpense(departmentId, status);

            assertThatThrownBy(() -> administration.deactivateDepartment(departmentId, 0))
                    .isInstanceOf(MasterOperationException.class)
                    .extracting(exception -> ((MasterOperationException) exception).failure())
                    .isEqualTo(MasterFailure.CONFLICT);

            assertDepartment(departmentId, true, 0);
            assertThat(deactivationAuditCount(departmentId)).isZero();
        }
    }

    @Test
    void deactivatesDepartmentWithNoExpenseOrOnlyTerminalExpenseStates() {
        UUID emptyDepartmentId = administration.createDepartment("EMPTY", "Empty");
        administration.deactivateDepartment(emptyDepartmentId, 0);
        assertDepartment(emptyDepartmentId, false, 1);
        assertThat(deactivationAuditCount(emptyDepartmentId)).isOne();

        for (String status : Set.of("REJECTED", "SETTLED")) {
            UUID departmentId = administration.createDepartment(
                    "TERMINAL_" + status, "Terminal " + status);
            insertExpense(departmentId, status);

            administration.deactivateDepartment(departmentId, 0);

            assertDepartment(departmentId, false, 1);
            assertThat(deactivationAuditCount(departmentId)).isOne();
        }
    }

    private void insertExpense(UUID departmentId, String status) {
        jdbc.sql("""
                        insert into kkref_expense_request
                            (expense_request_id, applicant_user_id, department_id,
                             claimed_amount, status, version, created_at, updated_at)
                        values (:requestId, :applicantId, :departmentId,
                                100, :status, 0, now(), now())
                        """)
                .param("requestId", UUID.randomUUID())
                .param("applicantId", APPLICANT_ID)
                .param("departmentId", departmentId)
                .param("status", status)
                .update();
    }

    private void assertDepartment(UUID departmentId, boolean active, long version) {
        assertThat(jdbc.sql("""
                        select active || ':' || version
                        from kkref_department
                        where department_id = :departmentId
                        """)
                .param("departmentId", departmentId)
                .query(String.class)
                .single())
                .isEqualTo(active + ":" + version);
    }

    private long deactivationAuditCount(UUID departmentId) {
        return jdbc.sql("""
                        select count(*)
                        from koiki_audit_event
                        where event_type = 'MASTER_ADMINISTRATION'
                          and action = 'DEACTIVATE_DEPARTMENT'
                          and resource_id = :departmentId
                        """)
                .param("departmentId", departmentId.toString())
                .query(Long.class)
                .single();
    }

    private void cleanReferenceData() {
        jdbc.sql("delete from koiki_audit_event where event_type in ('MASTER_ADMINISTRATION', 'EXPENSE_WORKFLOW')")
                .update();
        jdbc.sql("delete from kkref_expense_approver_scope").update();
        jdbc.sql("delete from kkref_expense_line").update();
        jdbc.sql("delete from kkref_expense_request").update();
        jdbc.sql("delete from kkref_user_department_assignment").update();
        jdbc.sql("delete from kkref_expense_category").update();
        jdbc.sql("delete from kkref_department").update();
    }

    private record TestPrincipal(FrameworkUserId userId, Set<String> permissions)
            implements FrameworkPrincipal {

        @Override
        public AuthenticationSource authenticationSource() {
            return AuthenticationSource.LOCAL;
        }
    }
}
