package org.koikifw.reference.expense;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

@SpringBootTest(properties = {
    "koiki.identity.local-authentication.enabled=false",
    "spring.session.jdbc.initialize-schema=never"
})
@Import(ReferencePostgreSqlTestConfiguration.class)
class ExpensePostgreSqlIntegrationTest {

    private static final UUID APPLICANT_ID = UUID.fromString(
            "31000000-0000-0000-0000-000000000001");
    private static final UUID APPROVER_ID = UUID.fromString(
            "31000000-0000-0000-0000-000000000002");
    private static final UUID OUTSIDER_ID = UUID.fromString(
            "31000000-0000-0000-0000-000000000005");
    private static final UUID DEPARTMENT_ID = UUID.fromString(
            "31000000-0000-0000-0000-000000000003");
    private static final UUID CATEGORY_ID = UUID.fromString(
            "31000000-0000-0000-0000-000000000004");

    @Autowired
    private ExpenseApplicationService expenses;

    @Autowired
    private JdbcClient jdbc;

    @BeforeEach
    void prepareMasterData() {
        cleanReferenceData();
        jdbc.sql("""
                        insert into kkref_department
                            (department_id, department_code, department_name,
                             active, version, created_at, updated_at)
                        values (:id, 'FINANCE', 'Finance', true, 0, now(), now())
                        """)
                .param("id", DEPARTMENT_ID)
                .update();
        jdbc.sql("""
                        insert into kkref_expense_category
                            (expense_category_id, expense_category_code, expense_category_name,
                             active, version, created_at, updated_at)
                        values (:id, 'TRAVEL', 'Travel', true, 0, now(), now())
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
        insertApproverScope(APPROVER_ID);
        insertApproverScope(APPLICANT_ID);
        authenticate(APPLICANT_ID, "EXPENSE:APPLY");
    }

    @AfterEach
    void cleanUpMasterData() {
        SecurityContextHolder.clearContext();
        cleanReferenceData();
    }

    private void cleanReferenceData() {
        jdbc.sql("delete from koiki_audit_event where event_type = 'EXPENSE_WORKFLOW'").update();
        jdbc.sql("delete from kkref_expense_approver_scope").update();
        jdbc.sql("delete from kkref_expense_line").update();
        jdbc.sql("delete from kkref_expense_request").update();
        jdbc.sql("delete from kkref_user_department_assignment").update();
        jdbc.sql("delete from kkref_expense_category").update();
        jdbc.sql("delete from kkref_department").update();
    }

    @Test
    void persistsAcceptedLifecycleWithSharedJpaModelAndOptimisticVersion() {
        UUID requestId = createDraft(100);
        long version = currentVersion(requestId);

        assertState(requestId, "DRAFT", version, 1);
        expenses.submit(requestId, version);
        assertState(requestId, "SUBMITTED", version + 1, 1);
        authenticate(APPROVER_ID, "EXPENSE:APPROVE");
        expenses.approve(requestId, version + 1);
        assertState(requestId, "APPROVED", version + 2, 1);
        authenticate(OUTSIDER_ID, "EXPENSE:SETTLE");
        expenses.settle(requestId, version + 2);
        assertState(requestId, "SETTLED", version + 3, 1);
        assertThat(countExpenseAuditEvents()).isEqualTo(3);
        assertThat(expenseAuditActions())
                .containsExactlyInAnyOrder(
                        "SUBMIT_EXPENSE", "APPROVE_EXPENSE", "SETTLE_EXPENSE");
    }

    @Test
    void rollsBackRejectedOperationsAndRejectsUnavailableMaster() {
        assertThatThrownBy(() -> createDraft(101))
                .isInstanceOf(ExpenseOperationException.class)
                .extracting(exception -> ((ExpenseOperationException) exception).failure())
                .isEqualTo(ExpenseFailure.INVALID_INPUT);
        assertThat(count("kkref_expense_request")).isZero();

        UUID requestId = createDraft(100);
        long version = currentVersion(requestId);
        expenses.submit(requestId, version);
        authenticate(APPLICANT_ID, "EXPENSE:APPLY", "EXPENSE:APPROVE");
        assertThatThrownBy(() -> expenses.reject(requestId, "self", version + 1))
                .isInstanceOf(ExpenseOperationException.class)
                .extracting(exception -> ((ExpenseOperationException) exception).failure())
                .isEqualTo(ExpenseFailure.SELF_DECISION);
        assertState(requestId, "SUBMITTED", version + 1, 1);
        assertThat(countExpenseAuditEvents()).isEqualTo(1);

        jdbc.sql("update kkref_expense_category set active = false where expense_category_id = :id")
                .param("id", CATEGORY_ID)
                .update();
        assertThatThrownBy(() -> createDraft(100))
                .isInstanceOf(ExpenseOperationException.class)
                .extracting(exception -> ((ExpenseOperationException) exception).failure())
                .isEqualTo(ExpenseFailure.MASTER_UNAVAILABLE);
        assertState(requestId, "SUBMITTED", version + 1, 1);
    }

    @Test
    void enforcesPermissionOwnershipAndExactDepartmentScopeWithoutAuditLeakage() {
        UUID requestId = createDraft(100);
        long version = currentVersion(requestId);

        authenticate(OUTSIDER_ID);
        assertThatThrownBy(() -> expenses.submit(requestId, version))
                .isInstanceOf(AccessDeniedException.class);

        authenticate(OUTSIDER_ID, "EXPENSE:APPLY");
        assertThatThrownBy(() -> expenses.submit(requestId, version))
                .isInstanceOf(ExpenseOperationException.class)
                .extracting(exception -> ((ExpenseOperationException) exception).failure())
                .isEqualTo(ExpenseFailure.NOT_FOUND);

        authenticate(APPLICANT_ID, "EXPENSE:APPLY");
        expenses.submit(requestId, version);
        authenticate(OUTSIDER_ID, "EXPENSE:APPROVE");
        assertThatThrownBy(() -> expenses.approve(requestId, version + 1))
                .isInstanceOf(ExpenseOperationException.class)
                .extracting(exception -> ((ExpenseOperationException) exception).failure())
                .isEqualTo(ExpenseFailure.NOT_FOUND);

        assertState(requestId, "SUBMITTED", version + 1, 1);
        assertThat(countExpenseAuditEvents()).isEqualTo(1);
    }

    @Test
    void recordsApproveRejectReturnAndReeditSuccessesWithinTheBusinessTransaction() {
        UUID approvedId = createDraft(100);
        long approvedVersion = currentVersion(approvedId);
        expenses.submit(approvedId, approvedVersion);
        UUID rejectedId = createDraft(100);
        long rejectedVersion = currentVersion(rejectedId);
        expenses.submit(rejectedId, rejectedVersion);
        UUID returnedId = createDraft(100);
        long returnedVersion = currentVersion(returnedId);
        expenses.submit(returnedId, returnedVersion);

        authenticate(APPROVER_ID, "EXPENSE:APPROVE");
        expenses.approve(approvedId, approvedVersion + 1);
        expenses.reject(rejectedId, "duplicate", rejectedVersion + 1);
        expenses.returnForRework(returnedId, "clarify purpose", returnedVersion + 1);
        authenticate(APPLICANT_ID, "EXPENSE:APPLY");
        expenses.beginReedit(returnedId, returnedVersion + 2);

        assertState(approvedId, "APPROVED", approvedVersion + 2, 1);
        assertState(rejectedId, "REJECTED", rejectedVersion + 2, 1);
        assertState(returnedId, "DRAFT", returnedVersion + 3, 1);
        assertThat(expenseAuditActions())
                .containsExactlyInAnyOrder(
                        "SUBMIT_EXPENSE",
                        "SUBMIT_EXPENSE",
                        "SUBMIT_EXPENSE",
                        "APPROVE_EXPENSE",
                        "REJECT_EXPENSE",
                        "RETURN_EXPENSE",
                        "BEGIN_REEDIT_EXPENSE");
    }

    @Test
    void migrationKeepsOnlySameModuleExpenseForeignKey() {
        Integer crossModuleForeignKeys = jdbc.sql("""
                        select count(*)
                        from pg_constraint constraint_definition
                        join pg_class child
                          on child.oid = constraint_definition.conrelid
                        join pg_class parent
                          on parent.oid = constraint_definition.confrelid
                        where constraint_definition.contype = 'f'
                          and child.relname in (
                              'kkref_expense_request',
                              'kkref_expense_line',
                              'kkref_expense_approver_scope')
                          and parent.relname not in (
                              'kkref_expense_request',
                              'kkref_expense_line',
                              'kkref_expense_approver_scope')
                        """)
                .query(Integer.class)
                .single();
        assertThat(crossModuleForeignKeys).isZero();
        assertThat(count("kkref_flyway_history")).isGreaterThanOrEqualTo(3);
    }

    private UUID createDraft(long lineAmount) {
        return expenses.createDraft(
                DEPARTMENT_ID,
                100,
                List.of(new ExpenseLineInput(
                        UUID.randomUUID(),
                        CATEGORY_ID,
                        LocalDate.of(2026, 9, 12),
                        "Taxi",
                        "Client visit",
                        lineAmount)));
    }

    private void insertApproverScope(UUID approverId) {
        jdbc.sql("""
                        insert into kkref_expense_approver_scope
                            (approver_user_id, department_id, created_at)
                        values (:approverId, :departmentId, now())
                        """)
                .param("approverId", approverId)
                .param("departmentId", DEPARTMENT_ID)
                .update();
    }

    private static void authenticate(UUID userId, String... permissions) {
        Set<String> permissionSet = Set.of(permissions);
        FrameworkPrincipal principal = new TestPrincipal(
                FrameworkUserId.parse(userId.toString()), permissionSet);
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(
                        principal,
                        "n/a",
                        permissionSet.stream().map(SimpleGrantedAuthority::new).toList()));
    }

    private void assertState(UUID requestId, String status, long version, long lineCount) {
        assertThat(jdbc.sql("""
                        select status || ':' || version
                        from kkref_expense_request
                        where expense_request_id = :id
                        """)
                .param("id", requestId)
                .query(String.class)
                .single())
                .isEqualTo(status + ":" + version);
        assertThat(jdbc.sql("""
                        select count(*) from kkref_expense_line
                        where expense_request_id = :id
                        """)
                .param("id", requestId)
                .query(Long.class)
                .single())
                .isEqualTo(lineCount);
    }

    private long currentVersion(UUID requestId) {
        return jdbc.sql("""
                        select version from kkref_expense_request
                        where expense_request_id = :id
                        """)
                .param("id", requestId)
                .query(Long.class)
                .single();
    }

    private long countExpenseAuditEvents() {
        return jdbc.sql("""
                        select count(*) from koiki_audit_event
                        where event_type = 'EXPENSE_WORKFLOW'
                        """)
                .query(Long.class)
                .single();
    }

    private List<String> expenseAuditActions() {
        return jdbc.sql("""
                        select action from koiki_audit_event
                        where event_type = 'EXPENSE_WORKFLOW'
                        """)
                .query(String.class)
                .list()
                .stream()
                .map(Objects::requireNonNull)
                .toList();
    }

    private long count(String table) {
        if (!List.of(
                        "kkref_expense_request",
                        "kkref_expense_line",
                        "kkref_expense_approver_scope",
                        "kkref_flyway_history")
                .contains(table)) {
            throw new IllegalArgumentException("Unexpected table");
        }
        return jdbc.sql("select count(*) from " + table).query(Long.class).single();
    }

    private record TestPrincipal(FrameworkUserId userId, Set<String> permissions)
            implements FrameworkPrincipal {

        @Override
        public AuthenticationSource authenticationSource() {
            return AuthenticationSource.LOCAL;
        }
    }
}
