package org.koikifw.reference.expense;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.koikifw.audit.AuditRecordingException;
import org.koikifw.audit.BusinessAuditRecorder;
import org.koikifw.identity.AuthenticationSource;
import org.koikifw.identity.FrameworkPrincipal;
import org.koikifw.identity.FrameworkUserId;
import org.koikifw.reference.ReferencePostgreSqlTestConfiguration;
import org.koikifw.reference.expense.application.ExpenseApplicationService;
import org.koikifw.reference.expense.application.ExpenseFailure;
import org.koikifw.reference.expense.application.ExpenseLineInput;
import org.koikifw.reference.expense.application.ExpenseOperationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(properties = {
    "koiki.identity.local-authentication.enabled=false",
    "spring.session.jdbc.initialize-schema=never"
})
@Import(ReferencePostgreSqlTestConfiguration.class)
class ExpenseAuditRollbackPostgreSqlIntegrationTest {

    private static final UUID APPLICANT_ID = UUID.fromString(
            "33000000-0000-0000-0000-000000000001");
    private static final UUID DEPARTMENT_ID = UUID.fromString(
            "33000000-0000-0000-0000-000000000002");
    private static final UUID CATEGORY_ID = UUID.fromString(
            "33000000-0000-0000-0000-000000000003");

    @Autowired
    private ExpenseApplicationService expenses;

    @Autowired
    private JdbcClient jdbc;

    @MockitoBean
    private BusinessAuditRecorder audit;

    @BeforeEach
    void prepareDataAndActor() {
        cleanData();
        jdbc.sql("""
                        insert into kkref_department
                            (department_id, department_code, department_name,
                             active, version, created_at, updated_at)
                        values (:id, 'ROLLBACK', 'Rollback', true, 0, now(), now())
                        """)
                .param("id", DEPARTMENT_ID)
                .update();
        jdbc.sql("""
                        insert into kkref_expense_category
                            (expense_category_id, expense_category_code, expense_category_name,
                             active, version, created_at, updated_at)
                        values (:id, 'ROLLBACK', 'Rollback', true, 0, now(), now())
                        """)
                .param("id", CATEGORY_ID)
                .update();
        jdbc.sql("""
                        insert into kkref_user_department_assignment
                            (user_id, department_id, version, created_at, updated_at)
                        values (:userId, :departmentId, 0, now(), now())
                        """)
                .param("userId", APPLICANT_ID)
                .param("departmentId", DEPARTMENT_ID)
                .update();
        FrameworkPrincipal principal = new TestPrincipal(
                FrameworkUserId.parse(APPLICANT_ID.toString()), Set.of("EXPENSE:APPLY"));
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(
                        principal,
                        "n/a",
                        List.of(new SimpleGrantedAuthority("EXPENSE:APPLY"))));
    }

    @AfterEach
    void cleanUp() {
        SecurityContextHolder.clearContext();
        cleanData();
    }

    @Test
    void rollsBackStateWhenBusinessAuditCannotBeRecorded() {
        UUID requestId = expenses.createDraft(
                DEPARTMENT_ID,
                100,
                List.of(new ExpenseLineInput(
                        UUID.randomUUID(),
                        CATEGORY_ID,
                        LocalDate.of(2026, 9, 12),
                        "Taxi",
                        "Client visit",
                        100)));
        long version = currentVersion(requestId);
        doThrow(new AuditRecordingException()).when(audit).record(org.mockito.ArgumentMatchers.any());

        assertThatThrownBy(() -> expenses.submit(requestId, version))
                .isInstanceOf(ExpenseOperationException.class)
                .extracting(exception -> ((ExpenseOperationException) exception).failure())
                .isEqualTo(ExpenseFailure.DEPENDENCY_FAILURE);

        assertThat(jdbc.sql("""
                        select status || ':' || version
                        from kkref_expense_request where expense_request_id = :id
                        """)
                .param("id", requestId)
                .query(String.class)
                .single())
                .isEqualTo("DRAFT:" + version);
    }

    private long currentVersion(UUID requestId) {
        return jdbc.sql("select version from kkref_expense_request where expense_request_id = :id")
                .param("id", requestId)
                .query(Long.class)
                .single();
    }

    private void cleanData() {
        jdbc.sql("delete from kkref_expense_line").update();
        jdbc.sql("delete from kkref_expense_request").update();
        jdbc.sql("delete from kkref_expense_approver_scope").update();
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
