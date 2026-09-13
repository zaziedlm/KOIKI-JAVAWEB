package org.koikifw.reference.expense;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
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
    }

    @AfterEach
    void cleanUpMasterData() {
        cleanReferenceData();
    }

    private void cleanReferenceData() {
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
        expenses.approve(requestId, APPROVER_ID, version + 1);
        assertState(requestId, "APPROVED", version + 2, 1);
        expenses.settle(requestId, version + 2);
        assertState(requestId, "SETTLED", version + 3, 1);
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
        assertThatThrownBy(() -> expenses.reject(
                        requestId, APPLICANT_ID, "self", version + 1))
                .isInstanceOf(ExpenseOperationException.class)
                .extracting(exception -> ((ExpenseOperationException) exception).failure())
                .isEqualTo(ExpenseFailure.SELF_DECISION);
        assertState(requestId, "SUBMITTED", version + 1, 1);

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
    void migrationKeepsOnlySameModuleExpenseForeignKey() {
        Integer crossModuleForeignKeys = jdbc.sql("""
                        select count(*)
                        from pg_constraint constraint_definition
                        join pg_class child
                          on child.oid = constraint_definition.conrelid
                        join pg_class parent
                          on parent.oid = constraint_definition.confrelid
                        where constraint_definition.contype = 'f'
                          and child.relname in ('kkref_expense_request', 'kkref_expense_line')
                          and parent.relname not in ('kkref_expense_request', 'kkref_expense_line')
                        """)
                .query(Integer.class)
                .single();
        assertThat(crossModuleForeignKeys).isZero();
        assertThat(count("kkref_flyway_history")).isGreaterThanOrEqualTo(2);
    }

    private UUID createDraft(long lineAmount) {
        return expenses.createDraft(
                FrameworkUserId.parse(APPLICANT_ID.toString()),
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

    private long count(String table) {
        if (!List.of(
                        "kkref_expense_request",
                        "kkref_expense_line",
                        "kkref_flyway_history")
                .contains(table)) {
            throw new IllegalArgumentException("Unexpected table");
        }
        return jdbc.sql("select count(*) from " + table).query(Long.class).single();
    }
}
