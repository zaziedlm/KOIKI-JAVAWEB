package org.koikifw.reference.expense.application;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.koikifw.identity.FrameworkUserId;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Coordinates the P3-A2 expense lifecycle without pre-empting P3-A3 authorization or audit. */
@Service
public class ExpenseApplicationService {

    private static final Logger logger = LoggerFactory.getLogger(ExpenseApplicationService.class);

    private final ExpenseRequestRepository requests;
    private final MasterAvailabilityPort masterAvailability;
    private final Clock clock;

    @Autowired
    public ExpenseApplicationService(
            ExpenseRequestRepository requests,
            MasterAvailabilityPort masterAvailability) {
        this(requests, masterAvailability, Clock.systemUTC());
    }

    ExpenseApplicationService(
            ExpenseRequestRepository requests,
            MasterAvailabilityPort masterAvailability,
            Clock clock) {
        this.requests = Objects.requireNonNull(requests, "requests");
        this.masterAvailability = Objects.requireNonNull(masterAvailability, "masterAvailability");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Transactional
    public UUID createDraft(
            FrameworkUserId applicantUserId,
            UUID departmentId,
            long claimedAmount,
            List<ExpenseLineInput> lines) {
        return execute(() -> {
            requireActiveAssignment(applicantUserId, departmentId);
            List<ExpenseLine> domainLines = toDomainLines(lines);
            requireActiveCategories(domainLines);
            UUID requestId = UUID.randomUUID();
            ExpenseRequest request = ExpenseRequest.createDraft(
                    requestId,
                    applicantUserId.value(),
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
    public void editDraft(
            UUID expenseRequestId,
            long expectedVersion,
            long claimedAmount,
            List<ExpenseLineInput> lines) {
        executeMutation(expenseRequestId, expectedVersion, request -> {
            List<ExpenseLine> domainLines = toDomainLines(lines);
            requireActiveCategories(domainLines);
            request.edit(
                    new Money(claimedAmount), domainLines, businessDate(), clock.instant());
        });
    }

    @Transactional
    public void submit(UUID expenseRequestId, long expectedVersion) {
        executeMutation(expenseRequestId, expectedVersion, request -> {
            FrameworkUserId applicant = FrameworkUserId.parse(request.applicantUserId().toString());
            requireActiveAssignment(applicant, request.departmentId());
            requireActiveCategories(request.lines());
            request.submit(businessDate(), clock.instant());
        });
    }

    @Transactional
    public void approve(UUID expenseRequestId, UUID actorUserId, long expectedVersion) {
        executeMutation(
                expenseRequestId,
                expectedVersion,
                request -> request.approve(actorUserId, clock.instant()));
    }

    @Transactional
    public void reject(
            UUID expenseRequestId, UUID actorUserId, String reason, long expectedVersion) {
        executeMutation(
                expenseRequestId,
                expectedVersion,
                request -> request.reject(actorUserId, reason, clock.instant()));
    }

    @Transactional
    public void returnForRework(
            UUID expenseRequestId, UUID actorUserId, String reason, long expectedVersion) {
        executeMutation(
                expenseRequestId,
                expectedVersion,
                request -> request.returnForRework(actorUserId, reason, clock.instant()));
    }

    @Transactional
    public void beginReedit(UUID expenseRequestId, long expectedVersion) {
        executeMutation(
                expenseRequestId,
                expectedVersion,
                request -> request.beginReedit(clock.instant()));
    }

    @Transactional
    public void settle(UUID expenseRequestId, long expectedVersion) {
        executeMutation(
                expenseRequestId,
                expectedVersion,
                request -> request.completeSettlement(clock.instant()));
    }

    private void executeMutation(
            UUID expenseRequestId,
            long expectedVersion,
            Consumer<ExpenseRequest> mutation) {
        execute(() -> {
            ExpenseRequest request = requests.findById(requireId(expenseRequestId))
                    .orElseThrow(() -> failure(ExpenseFailure.NOT_FOUND));
            requireVersion(request.version(), expectedVersion);
            mutation.accept(request);
            requests.flush();
            return null;
        });
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
