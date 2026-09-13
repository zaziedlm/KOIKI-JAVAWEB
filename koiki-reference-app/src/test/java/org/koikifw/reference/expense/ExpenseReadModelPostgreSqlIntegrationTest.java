package org.koikifw.reference.expense;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.koikifw.identity.AuthenticationSource;
import org.koikifw.identity.FrameworkPrincipal;
import org.koikifw.identity.FrameworkUserId;
import org.koikifw.reference.ReferencePostgreSqlTestConfiguration;
import org.koikifw.reference.expense.application.ExpenseReadService;
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
class ExpenseReadModelPostgreSqlIntegrationTest {

    private static final UUID APPLICANT_A = id(1);
    private static final UUID APPLICANT_B = id(2);
    private static final UUID APPROVER = id(3);
    private static final UUID ACCOUNTANT = id(4);
    private static final UUID DEPARTMENT_A = id(11);
    private static final UUID DEPARTMENT_B = id(12);
    private static final UUID CATEGORY = id(13);
    private static final UUID LINE = id(14);
    private static final UUID DRAFT_A = id(21);
    private static final UUID SUBMITTED_A = id(22);
    private static final UUID APPROVED_A = id(23);
    private static final UUID SUBMITTED_B = id(24);
    private static final UUID APPROVED_B = id(25);

    @Autowired
    private ExpenseReadService reads;

    @Autowired
    private JdbcClient jdbc;

    @BeforeEach
    void prepare() {
        cleanData();
        insertUser(APPLICANT_A, "applicant-a@example.test");
        insertUser(APPLICANT_B, "applicant-b@example.test");
        insertUser(APPROVER, "approver@example.test");
        insertUser(ACCOUNTANT, "accountant@example.test");
        insertDepartment(DEPARTMENT_A, "FINANCE", "Finance");
        insertDepartment(DEPARTMENT_B, "SALES", "Sales");
        insertCategory(CATEGORY, "TRAVEL", "Travel");
        jdbc.sql("""
                        insert into kkref_user_department_assignment
                            (user_id, department_id, version, created_at, updated_at)
                        values (:userId, :departmentId, 0, now(), now())
                        """)
                .param("userId", APPLICANT_A)
                .param("departmentId", DEPARTMENT_A)
                .update();
        insertRequest(DRAFT_A, APPLICANT_A, DEPARTMENT_A, "DRAFT", 100, 1);
        insertRequest(SUBMITTED_A, APPLICANT_A, DEPARTMENT_A, "SUBMITTED", 200, 2);
        insertRequest(APPROVED_A, APPLICANT_A, DEPARTMENT_A, "APPROVED", 300, 3);
        insertRequest(SUBMITTED_B, APPLICANT_B, DEPARTMENT_B, "SUBMITTED", 400, 4);
        insertRequest(APPROVED_B, APPLICANT_B, DEPARTMENT_B, "APPROVED", 500, 5);
        jdbc.sql("""
                        insert into kkref_expense_line
                            (expense_line_id, expense_request_id, expense_category_id,
                             usage_date, description, purpose, amount)
                        values (:lineId, :requestId, :categoryId,
                                :usageDate, 'Train', 'Customer visit', 200)
                        """)
                .param("lineId", LINE)
                .param("requestId", SUBMITTED_A)
                .param("categoryId", CATEGORY)
                .param("usageDate", LocalDate.of(2026, 9, 12))
                .update();
        jdbc.sql("""
                        insert into kkref_expense_approver_scope
                            (approver_user_id, department_id, created_at)
                        values (:approverId, :departmentId, now())
                        """)
                .param("approverId", APPROVER)
                .param("departmentId", DEPARTMENT_A)
                .update();
    }

    @AfterEach
    void cleanUp() {
        SecurityContextHolder.clearContext();
        cleanData();
    }

    @Test
    void enforcesApplicantApproverAndAccountingScopeBeforeMaterialization() {
        authenticate(APPLICANT_A, "EXPENSE:APPLY");
        var own = reads.findOwnRequests(0, 20);
        assertThat(own.totalElements()).isEqualTo(3);
        assertThat(own.content())
                .extracting(row -> row.expenseRequestId())
                .containsExactlyInAnyOrder(DRAFT_A, SUBMITTED_A, APPROVED_A);
        assertThat(own.content()).allSatisfy(row -> {
            assertThat(row.applicantEmail()).isEqualTo("applicant-a@example.test");
            assertThat(row.departmentCode()).isEqualTo("FINANCE");
            assertThat(row.departmentName()).isEqualTo("Finance");
        });

        authenticate(APPROVER, "EXPENSE:APPROVE");
        var approvalQueue = reads.findApprovalQueue(0, 20);
        assertThat(approvalQueue.totalElements()).isEqualTo(2);
        assertThat(approvalQueue.content())
                .extracting(row -> row.expenseRequestId())
                .containsExactlyInAnyOrder(SUBMITTED_A, APPROVED_A)
                .doesNotContain(DRAFT_A, SUBMITTED_B, APPROVED_B);

        authenticate(ACCOUNTANT, "EXPENSE:SETTLE");
        var accountingQueue = reads.findAccountingQueue(0, 1);
        assertThat(accountingQueue.totalElements()).isEqualTo(2);
        assertThat(accountingQueue.content()).hasSize(1);
        assertThat(accountingQueue.content().getFirst().expenseRequestId()).isEqualTo(APPROVED_B);
        assertThat(accountingQueue.content().getFirst().claimedAmount()).isEqualTo(500);
        assertThat(accountingQueue.content().getFirst().status()).isEqualTo("APPROVED");
        assertThat(accountingQueue.content().getFirst().version()).isEqualTo(5);
    }

    @Test
    void materializesScopedDetailAndCurrentFormOptionsWithoutDomainEntities() {
        authenticate(APPLICANT_A, "EXPENSE:APPLY");
        var detail = reads.findOwnRequest(SUBMITTED_A).orElseThrow();
        assertThat(detail.applicantEmail()).isEqualTo("applicant-a@example.test");
        assertThat(detail.departmentName()).isEqualTo("Finance");
        assertThat(detail.lines()).singleElement().satisfies(line -> {
            assertThat(line.expenseLineId()).isEqualTo(LINE);
            assertThat(line.expenseCategoryCode()).isEqualTo("TRAVEL");
            assertThat(line.description()).isEqualTo("Train");
        });
        assertThat(reads.findOwnRequest(SUBMITTED_B)).isEmpty();
        assertThat(reads.findAvailableDepartments())
                .extracting(option -> option.id())
                .containsExactly(DEPARTMENT_A);
        assertThat(reads.findAvailableExpenseCategories())
                .extracting(option -> option.id())
                .containsExactly(CATEGORY);

        authenticate(APPROVER, "EXPENSE:APPROVE");
        assertThat(reads.findApprovalRequest(SUBMITTED_A)).isPresent();
        assertThat(reads.findApprovalRequest(SUBMITTED_B)).isEmpty();
        assertThat(reads.findApprovalRequest(DRAFT_A)).isEmpty();

        authenticate(ACCOUNTANT, "EXPENSE:SETTLE");
        assertThat(reads.findAccountingRequest(APPROVED_A)).isPresent();
        assertThat(reads.findAccountingRequest(SUBMITTED_A)).isEmpty();
    }

    private void insertUser(UUID userId, String email) {
        jdbc.sql("""
                        insert into koiki_user
                            (user_id, email, canonical_email, status, version)
                        values (:userId, :email, :email, 'ACTIVE', 0)
                        """)
                .param("userId", userId)
                .param("email", email)
                .update();
    }

    private void insertDepartment(UUID departmentId, String code, String name) {
        jdbc.sql("""
                        insert into kkref_department
                            (department_id, department_code, department_name,
                             active, version, created_at, updated_at)
                        values (:id, :code, :name, true, 0, now(), now())
                        """)
                .param("id", departmentId)
                .param("code", code)
                .param("name", name)
                .update();
    }

    private void insertCategory(UUID categoryId, String code, String name) {
        jdbc.sql("""
                        insert into kkref_expense_category
                            (expense_category_id, expense_category_code, expense_category_name,
                             active, version, created_at, updated_at)
                        values (:id, :code, :name, true, 0, now(), now())
                        """)
                .param("id", categoryId)
                .param("code", code)
                .param("name", name)
                .update();
    }

    private void insertRequest(
            UUID requestId,
            UUID applicantId,
            UUID departmentId,
            String status,
            long amount,
            long version) {
        Instant updatedAt = Instant.parse("2026-09-13T00:00:00Z").plusSeconds(version);
        jdbc.sql("""
                        insert into kkref_expense_request
                            (expense_request_id, applicant_user_id, department_id,
                             claimed_amount, status, version, created_at, updated_at)
                        values (:requestId, :applicantId, :departmentId,
                                :amount, :status, :version, :updatedAt, :updatedAt)
                        """)
                .param("requestId", requestId)
                .param("applicantId", applicantId)
                .param("departmentId", departmentId)
                .param("amount", amount)
                .param("status", status)
                .param("version", version)
                .param("updatedAt", Timestamp.from(updatedAt))
                .update();
    }

    private void cleanData() {
        jdbc.sql("delete from kkref_expense_approver_scope").update();
        jdbc.sql("delete from kkref_expense_line").update();
        jdbc.sql("delete from kkref_expense_request").update();
        jdbc.sql("delete from kkref_user_department_assignment").update();
        jdbc.sql("delete from kkref_expense_category").update();
        jdbc.sql("delete from kkref_department").update();
        jdbc.sql("""
                        delete from koiki_user
                         where user_id in (:applicantA, :applicantB, :approver, :accountant)
                        """)
                .param("applicantA", APPLICANT_A)
                .param("applicantB", APPLICANT_B)
                .param("approver", APPROVER)
                .param("accountant", ACCOUNTANT)
                .update();
    }

    private static void authenticate(UUID userId, String permission) {
        FrameworkPrincipal principal = new TestPrincipal(
                FrameworkUserId.parse(userId.toString()), Set.of(permission));
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(
                        principal,
                        "n/a",
                        Set.of(new SimpleGrantedAuthority(permission))));
    }

    private static UUID id(long suffix) {
        return new UUID(0x3400000000000000L, suffix);
    }

    private record TestPrincipal(FrameworkUserId userId, Set<String> permissions)
            implements FrameworkPrincipal {

        @Override
        public AuthenticationSource authenticationSource() {
            return AuthenticationSource.LOCAL;
        }
    }
}
