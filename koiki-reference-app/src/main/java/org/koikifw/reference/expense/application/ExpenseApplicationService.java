package org.koikifw.reference.expense.application;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;
import org.koikifw.audit.AuditActor;
import org.koikifw.audit.AuditEvent;
import org.koikifw.audit.AuditResult;
import org.koikifw.audit.BusinessAuditRecorder;
import org.koikifw.identity.FrameworkPrincipal;
import org.koikifw.identity.FrameworkUserId;
import org.koikifw.reference.expense.application.port.outbound.ApproverScopePort;
import org.koikifw.reference.expense.application.port.outbound.MasterAvailabilityPort;
import org.koikifw.reference.expense.domain.model.ExpenseDomainException;
import org.koikifw.reference.expense.domain.model.ExpenseLine;
import org.koikifw.reference.expense.domain.model.ExpenseRequest;
import org.koikifw.reference.expense.domain.model.ExpenseRuleViolation;
import org.koikifw.reference.expense.domain.model.Money;
import org.koikifw.reference.expense.domain.repository.ExpenseRequestRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Coordinates the authorized and audited expense lifecycle. */
@Service
public class ExpenseApplicationService {

    private static final Logger logger = LoggerFactory.getLogger(ExpenseApplicationService.class);

    private final ExpenseRequestRepository requests;
    private final MasterAvailabilityPort masterAvailability;
    private final ApproverScopePort approverScope;
    private final BusinessAuditRecorder businessAuditRecorder;
    private final Clock clock;

    @Autowired
    public ExpenseApplicationService(
            ExpenseRequestRepository requests,
            MasterAvailabilityPort masterAvailability,
            ApproverScopePort approverScope,
            BusinessAuditRecorder businessAuditRecorder) {
        this(
                requests,
                masterAvailability,
                approverScope,
                businessAuditRecorder,
                Clock.systemUTC());
    }

    ExpenseApplicationService(
            ExpenseRequestRepository requests,
            MasterAvailabilityPort masterAvailability,
            ApproverScopePort approverScope,
            BusinessAuditRecorder businessAuditRecorder,
            Clock clock) {
        this.requests = Objects.requireNonNull(requests, "requests");
        this.masterAvailability = Objects.requireNonNull(masterAvailability, "masterAvailability");
        this.approverScope = Objects.requireNonNull(approverScope, "approverScope");
        this.businessAuditRecorder =
                Objects.requireNonNull(businessAuditRecorder, "businessAuditRecorder");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Transactional
    @PreAuthorize("hasAuthority('EXPENSE:APPLY')")
    public UUID createDraft(
            UUID departmentId,
            long claimedAmount,
            List<ExpenseLineInput> lines) {
        return execute(() -> {
            FrameworkUserId applicant = currentActor();
            requireActiveAssignment(applicant, departmentId);
            List<ExpenseLine> domainLines = toDomainLines(lines);
            requireActiveCategories(domainLines);
            UUID requestId = UUID.randomUUID();
            ExpenseRequest request = ExpenseRequest.createDraft(
                    requestId,
                    applicant.value(),
                    departmentId,
                    new Money(claimedAmount),
                    domainLines,
                    businessDate(),
                    clock.instant());
            requests.save(request);
            requests.flush();
            return requestId;
        });
    }

    @Transactional
    @PreAuthorize("hasAuthority('EXPENSE:APPLY')")
    public void editDraft(
            UUID expenseRequestId,
            long expectedVersion,
            long claimedAmount,
            List<ExpenseLineInput> lines) {
        FrameworkUserId actor = currentActor();
        executeMutation(
                expenseRequestId,
                expectedVersion,
                request -> requireOwner(request, actor),
                request -> {
                    List<ExpenseLine> domainLines = toDomainLines(lines);
                    requireActiveCategories(domainLines);
                    request.edit(
                            new Money(claimedAmount),
                            domainLines,
                            businessDate(),
                            clock.instant());
                },
                null,
                actor);
    }

    @Transactional
    @PreAuthorize("hasAuthority('EXPENSE:APPLY')")
    public void submit(UUID expenseRequestId, long expectedVersion) {
        FrameworkUserId actor = currentActor();
        executeMutation(
                expenseRequestId,
                expectedVersion,
                request -> requireOwner(request, actor),
                request -> {
                    FrameworkUserId applicant =
                            FrameworkUserId.parse(request.applicantUserId().toString());
                    requireActiveAssignment(applicant, request.departmentId());
                    requireActiveCategories(request.lines());
                    request.submit(businessDate(), clock.instant());
                },
                "SUBMIT_EXPENSE",
                actor);
    }

    @Transactional
    @PreAuthorize("hasAuthority('EXPENSE:APPROVE')")
    public void approve(UUID expenseRequestId, long expectedVersion) {
        FrameworkUserId actor = currentActor();
        executeMutation(
                expenseRequestId,
                expectedVersion,
                request -> requireApproverScope(request, actor),
                request -> request.approve(actor.value(), clock.instant()),
                "APPROVE_EXPENSE",
                actor);
    }

    @Transactional
    @PreAuthorize("hasAuthority('EXPENSE:APPROVE')")
    public void reject(
            UUID expenseRequestId, String reason, long expectedVersion) {
        FrameworkUserId actor = currentActor();
        executeMutation(
                expenseRequestId,
                expectedVersion,
                request -> requireApproverScope(request, actor),
                request -> request.reject(actor.value(), reason, clock.instant()),
                "REJECT_EXPENSE",
                actor);
    }

    @Transactional
    @PreAuthorize("hasAuthority('EXPENSE:APPROVE')")
    public void returnForRework(
            UUID expenseRequestId, String reason, long expectedVersion) {
        FrameworkUserId actor = currentActor();
        executeMutation(
                expenseRequestId,
                expectedVersion,
                request -> requireApproverScope(request, actor),
                request -> request.returnForRework(actor.value(), reason, clock.instant()),
                "RETURN_EXPENSE",
                actor);
    }

    @Transactional
    @PreAuthorize("hasAuthority('EXPENSE:APPLY')")
    public void beginReedit(UUID expenseRequestId, long expectedVersion) {
        FrameworkUserId actor = currentActor();
        executeMutation(
                expenseRequestId,
                expectedVersion,
                request -> requireOwner(request, actor),
                request -> request.beginReedit(clock.instant()),
                "BEGIN_REEDIT_EXPENSE",
                actor);
    }

    @Transactional
    @PreAuthorize("hasAuthority('EXPENSE:SETTLE')")
    public void settle(UUID expenseRequestId, long expectedVersion) {
        FrameworkUserId actor = currentActor();
        executeMutation(
                expenseRequestId,
                expectedVersion,
                request -> {},
                request -> request.completeSettlement(clock.instant()),
                "SETTLE_EXPENSE",
                actor);
    }

    private void executeMutation(
            UUID expenseRequestId,
            long expectedVersion,
            Consumer<ExpenseRequest> authorization,
            Consumer<ExpenseRequest> mutation,
            @Nullable String auditAction,
            FrameworkUserId actor) {
        execute(() -> {
            ExpenseRequest request = requests.findById(requireId(expenseRequestId))
                    .orElseThrow(() -> failure(ExpenseFailure.NOT_FOUND));
            requireVersion(request.version(), expectedVersion);
            authorization.accept(request);
            mutation.accept(request);
            requests.flush();
            if (auditAction != null) {
                record(auditAction, request.expenseRequestId(), actor);
            }
            return null;
        });
    }

    private void requireOwner(ExpenseRequest request, FrameworkUserId actor) {
        if (!request.applicantUserId().equals(actor.value())) {
            throw failure(ExpenseFailure.NOT_FOUND);
        }
    }

    private void requireApproverScope(ExpenseRequest request, FrameworkUserId actor) {
        if (!approverScope.includes(actor, request.departmentId())) {
            throw failure(ExpenseFailure.NOT_FOUND);
        }
    }

    private void record(String action, UUID expenseRequestId, FrameworkUserId actor) {
        businessAuditRecorder.record(AuditEvent.of(
                        "EXPENSE_WORKFLOW",
                        AuditActor.user(actor.toString()),
                        action,
                        AuditResult.SUCCESS)
                .withResource("EXPENSE_REQUEST", expenseRequestId.toString()));
    }

    private static FrameworkUserId currentActor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken
                || !(authentication.getPrincipal() instanceof FrameworkPrincipal principal)) {
            throw failure(ExpenseFailure.DEPENDENCY_FAILURE);
        }
        return principal.userId();
    }

    private List<ExpenseLine> toDomainLines(List<ExpenseLineInput> lines) {
        if (lines == null || lines.stream().anyMatch(Objects::isNull)) {
            throw failure(ExpenseFailure.INVALID_INPUT);
        }
        LocalDate businessDate = businessDate();
        return lines.stream()
                .map(line -> ExpenseLine.create(
                        requireId(line.expenseLineId()),
                        requireId(line.expenseCategoryId()),
                        line.usageDate(),
                        line.description(),
                        line.purpose(),
                        new Money(line.amount()),
                        businessDate))
                .toList();
    }

    private void requireActiveAssignment(FrameworkUserId applicantUserId, UUID departmentId) {
        if (applicantUserId == null || departmentId == null) {
            throw failure(ExpenseFailure.INVALID_INPUT);
        }
        if (!masterAvailability.isActiveDepartment(departmentId)
                || !masterAvailability.isUserAssignedToActiveDepartment(
                        applicantUserId, departmentId)) {
            throw failure(ExpenseFailure.MASTER_UNAVAILABLE);
        }
    }

    private void requireActiveCategories(List<ExpenseLine> lines) {
        if (lines.stream().map(ExpenseLine::expenseCategoryId).distinct()
                .anyMatch(categoryId -> !masterAvailability.isActiveExpenseCategory(categoryId))) {
            throw failure(ExpenseFailure.MASTER_UNAVAILABLE);
        }
    }

    private LocalDate businessDate() {
        return LocalDate.now(clock);
    }

    private <T> T execute(Supplier<T> operation) {
        try {
            return operation.get();
        } catch (ExpenseOperationException exception) {
            throw exception;
        } catch (ExpenseDomainException exception) {
            throw failure(switch (exception.violation()) {
                case INVALID_CONTENT -> ExpenseFailure.INVALID_INPUT;
                case INVALID_TRANSITION -> ExpenseFailure.INVALID_TRANSITION;
                case SELF_DECISION -> ExpenseFailure.SELF_DECISION;
            });
        } catch (OptimisticLockingFailureException exception) {
            throw failure(ExpenseFailure.CONCURRENT_MODIFICATION);
        } catch (DataIntegrityViolationException exception) {
            throw failure(ExpenseFailure.CONFLICT);
        } catch (RuntimeException exception) {
            logger.error("KOIKI-REF-EXP-001: expense dependency failed", exception);
            throw failure(ExpenseFailure.DEPENDENCY_FAILURE);
        }
    }

    private static UUID requireId(UUID id) {
        if (id == null) {
            throw failure(ExpenseFailure.INVALID_INPUT);
        }
        return id;
    }

    private static void requireVersion(long actual, long expected) {
        if (expected < 0) {
            throw failure(ExpenseFailure.INVALID_INPUT);
        }
        if (actual != expected) {
            throw failure(ExpenseFailure.CONCURRENT_MODIFICATION);
        }
    }

    private static ExpenseOperationException failure(ExpenseFailure failure) {
        return new ExpenseOperationException(failure);
    }
}
