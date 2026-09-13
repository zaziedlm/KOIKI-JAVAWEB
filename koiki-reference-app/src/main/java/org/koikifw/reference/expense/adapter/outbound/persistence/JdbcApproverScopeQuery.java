package org.koikifw.reference.expense.adapter.outbound.persistence;

import java.util.Objects;
import java.util.UUID;
import org.koikifw.identity.FrameworkUserId;
import org.koikifw.reference.expense.application.port.outbound.ApproverScopePort;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/** Reads the expense-owned exact department scope without consulting Framework Identity. */
@Repository
@Transactional(readOnly = true)
public class JdbcApproverScopeQuery implements ApproverScopePort {

    private final JdbcClient jdbc;

    public JdbcApproverScopeQuery(JdbcClient jdbc) {
        this.jdbc = Objects.requireNonNull(jdbc, "jdbc");
    }

    @Override
    public boolean includes(FrameworkUserId approverUserId, UUID departmentId) {
        Objects.requireNonNull(approverUserId, "approverUserId");
        Objects.requireNonNull(departmentId, "departmentId");
        return jdbc.sql("""
                        select count(*) > 0
                        from kkref_expense_approver_scope
                        where approver_user_id = :approverUserId
                          and department_id = :departmentId
                        """)
                .param("approverUserId", approverUserId.value())
                .param("departmentId", departmentId)
                .query(Boolean.class)
                .single();
    }
}

