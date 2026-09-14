package org.koikifw.reference.expense.adapter.outbound.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.koikifw.identity.FrameworkUserId;
import org.koikifw.reference.expense.application.query.ExpenseRequestListItem;
import org.koikifw.reference.expense.application.query.ExpenseRequestDetail;
import org.koikifw.reference.expense.application.query.ExpenseRequestLineView;
import org.koikifw.reference.expense.application.query.ExpenseRequestPage;
import org.koikifw.reference.expense.application.query.ExpenseRequestQuery;
import org.koikifw.reference.expense.application.query.ExpenseSelectionOption;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** Materializes display-only expense rows while enforcing actor scope in SQL. */
@Repository
public class JdbcExpenseRequestQuery implements ExpenseRequestQuery {

    private static final String SELECT = """
            select request.expense_request_id,
                   request.applicant_user_id,
                   applicant.email as applicant_email,
                   request.department_id,
                   department.department_code,
                   department.department_name,
                   request.claimed_amount,
                   request.status,
                   request.version,
                   request.updated_at
              from kkref_expense_request request
              join koiki_user applicant
                on applicant.user_id = request.applicant_user_id
              join kkref_department department
                on department.department_id = request.department_id
            """;

    private static final String COUNT = """
            select count(*)
              from kkref_expense_request request
              join koiki_user applicant
                on applicant.user_id = request.applicant_user_id
              join kkref_department department
                on department.department_id = request.department_id
            """;

    private static final String APPLICANT_SCOPE = """
             where request.applicant_user_id = :actorUserId
            """;

    private static final String APPROVER_SCOPE = """
             where request.status <> 'DRAFT'
               and exists (
                   select 1
                     from kkref_expense_approver_scope approver_scope
                    where approver_scope.approver_user_id = :actorUserId
                      and approver_scope.department_id = request.department_id)
            """;

    private static final String ACCOUNTING_SCOPE = """
             where request.status in ('APPROVED', 'SETTLED')
            """;

    private static final String ORDER_AND_PAGE = """
             order by request.updated_at desc, request.expense_request_id
             limit :limit offset :offset
            """;

    private static final String DETAIL_SUFFIX = """
               and request.expense_request_id = :expenseRequestId
            """;

    private static final String LINES = """
            select line.expense_line_id,
                   line.expense_category_id,
                   category.expense_category_code,
                   category.expense_category_name,
                   line.usage_date,
                   line.description,
                   line.purpose,
                   line.amount
              from kkref_expense_line line
              join kkref_expense_category category
                on category.expense_category_id = line.expense_category_id
             where line.expense_request_id = :expenseRequestId
             order by line.usage_date, line.expense_line_id
            """;

    private final JdbcClient jdbc;

    public JdbcExpenseRequestQuery(JdbcClient jdbc) {
        this.jdbc = Objects.requireNonNull(jdbc, "jdbc");
    }

    @Override
    public ExpenseRequestPage findForApplicant(
            FrameworkUserId applicantUserId, int page, int size) {
        return query(APPLICANT_SCOPE, applicantUserId, page, size);
    }

    @Override
    public ExpenseRequestPage findForApprover(
            FrameworkUserId approverUserId, int page, int size) {
        return query(APPROVER_SCOPE, approverUserId, page, size);
    }

    @Override
    public ExpenseRequestPage findForAccounting(int page, int size) {
        return query(ACCOUNTING_SCOPE, null, page, size);
    }

    @Override
    public Optional<ExpenseRequestDetail> findForApplicantById(
            FrameworkUserId applicantUserId, UUID expenseRequestId) {
        return detail(APPLICANT_SCOPE, applicantUserId, expenseRequestId);
    }

    @Override
    public Optional<ExpenseRequestDetail> findForApproverById(
            FrameworkUserId approverUserId, UUID expenseRequestId) {
        return detail(APPROVER_SCOPE, approverUserId, expenseRequestId);
    }

    @Override
    public Optional<ExpenseRequestDetail> findForAccountingById(UUID expenseRequestId) {
        return detail(ACCOUNTING_SCOPE, null, expenseRequestId);
    }

    @Override
    public List<ExpenseSelectionOption> findAvailableDepartments(
            FrameworkUserId applicantUserId) {
        return jdbc.sql("""
                        select department.department_id as option_id,
                               department.department_code as option_code,
                               department.department_name as option_name
                          from kkref_user_department_assignment assignment
                          join kkref_department department
                            on department.department_id = assignment.department_id
                         where assignment.user_id = :actorUserId
                           and department.active = true
                         order by department.department_code
                        """)
                .param("actorUserId", applicantUserId.value())
                .query(this::mapOption)
                .list();
    }

    @Override
    @Cacheable(
            cacheNames = "koiki:reference:expense-category-options",
            key = "'active'",
            sync = true)
    public List<ExpenseSelectionOption> findAvailableExpenseCategories() {
        List<ExpenseSelectionOption> options = jdbc.sql("""
                        select category.expense_category_id as option_id,
                               category.expense_category_code as option_code,
                               category.expense_category_name as option_name
                          from kkref_expense_category category
                         where category.active = true
                         order by category.expense_category_code
                        """)
                .query(this::mapOption)
                .list();
        return List.copyOf(options);
    }

    private ExpenseRequestPage query(
            String scope, @Nullable FrameworkUserId actorUserId, int page, int size) {
        long offset = (long) page * size;
        JdbcClient.StatementSpec rows = jdbc.sql(SELECT + scope + ORDER_AND_PAGE)
                .param("limit", size)
                .param("offset", offset);
        JdbcClient.StatementSpec count = jdbc.sql(COUNT + scope);
        if (actorUserId != null) {
            rows = rows.param("actorUserId", actorUserId.value());
            count = count.param("actorUserId", actorUserId.value());
        }
        List<ExpenseRequestListItem> content = rows.query(this::map).list();
        long totalElements = count.query(Long.class).single();
        return new ExpenseRequestPage(content, totalElements, page, size);
    }

    private ExpenseRequestListItem map(ResultSet resultSet, int rowNumber) throws SQLException {
        return new ExpenseRequestListItem(
                resultSet.getObject("expense_request_id", java.util.UUID.class),
                resultSet.getObject("applicant_user_id", java.util.UUID.class),
                resultSet.getString("applicant_email"),
                resultSet.getObject("department_id", java.util.UUID.class),
                resultSet.getString("department_code"),
                resultSet.getString("department_name"),
                resultSet.getLong("claimed_amount"),
                resultSet.getString("status"),
                resultSet.getLong("version"),
                resultSet.getTimestamp("updated_at").toInstant());
    }

    private Optional<ExpenseRequestDetail> detail(
            String scope, @Nullable FrameworkUserId actorUserId, UUID expenseRequestId) {
        JdbcClient.StatementSpec statement = jdbc.sql(SELECT + scope + DETAIL_SUFFIX)
                .param("expenseRequestId", expenseRequestId);
        if (actorUserId != null) {
            statement = statement.param("actorUserId", actorUserId.value());
        }
        Optional<ExpenseRequestListItem> header = statement.query(this::map).optional();
        if (header.isEmpty()) {
            return Optional.empty();
        }
        List<ExpenseRequestLineView> lines = jdbc.sql(LINES)
                .param("expenseRequestId", expenseRequestId)
                .query(this::mapLine)
                .list();
        ExpenseRequestListItem item = header.orElseThrow();
        String reason = jdbc.sql("""
                        select decision_reason
                          from kkref_expense_request
                         where expense_request_id = :expenseRequestId
                        """)
                .param("expenseRequestId", expenseRequestId)
                .query(String.class)
                .optional()
                .orElse(null);
        return Optional.of(new ExpenseRequestDetail(
                item.expenseRequestId(), item.applicantUserId(), item.applicantEmail(),
                item.departmentId(), item.departmentCode(), item.departmentName(),
                item.claimedAmount(), item.status(), reason, item.version(), item.updatedAt(), lines));
    }

    private ExpenseRequestLineView mapLine(ResultSet resultSet, int rowNumber) throws SQLException {
        return new ExpenseRequestLineView(
                resultSet.getObject("expense_line_id", UUID.class),
                resultSet.getObject("expense_category_id", UUID.class),
                resultSet.getString("expense_category_code"),
                resultSet.getString("expense_category_name"),
                resultSet.getObject("usage_date", java.time.LocalDate.class),
                resultSet.getString("description"),
                resultSet.getString("purpose"),
                resultSet.getLong("amount"));
    }

    private ExpenseSelectionOption mapOption(ResultSet resultSet, int rowNumber) throws SQLException {
        return new ExpenseSelectionOption(
                resultSet.getObject("option_id", UUID.class),
                resultSet.getString("option_code"),
                resultSet.getString("option_name"));
    }
}
