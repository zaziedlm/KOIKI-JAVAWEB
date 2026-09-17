package org.koikifw.reference.expense.application.port.outbound;

import java.util.UUID;
import org.koikifw.identity.FrameworkUserId;

/** Checks the Reference-owned exact department scope assigned to an approver. */
public interface ApproverScopePort {

    boolean includes(FrameworkUserId approverUserId, UUID departmentId);
}

