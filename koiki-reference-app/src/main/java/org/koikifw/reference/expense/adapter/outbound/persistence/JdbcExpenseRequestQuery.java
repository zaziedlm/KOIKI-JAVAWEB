package org.koikifw.reference.expense.adapter.outbound.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.koikifw.identity.FrameworkUserId;
import org.koikifw.reference.expense.application.query.ExpenseRequestListItem;
import org.koikifw.reference.expense.application.query.ExpenseRequestPage;
import org.koikifw.reference.expense.application.query.ExpenseRequestQuery;
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
}
