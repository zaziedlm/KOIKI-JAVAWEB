package org.koikifw.reference.master.application;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;
import org.koikifw.audit.AuditActor;
import org.koikifw.audit.AuditEvent;
import org.koikifw.audit.AuditResult;
import org.koikifw.audit.BusinessAuditRecorder;
import org.koikifw.identity.FrameworkPrincipal;
import org.koikifw.identity.FrameworkUserId;
import org.koikifw.reference.master.adapter.outbound.persistence.DepartmentEntity;
import org.koikifw.reference.master.adapter.outbound.persistence.DepartmentRepository;
import org.koikifw.reference.master.adapter.outbound.persistence.ExpenseCategoryEntity;
import org.koikifw.reference.master.adapter.outbound.persistence.ExpenseCategoryRepository;
import org.koikifw.reference.master.adapter.outbound.persistence.UserDepartmentAssignmentEntity;
import org.koikifw.reference.master.adapter.outbound.persistence.UserDepartmentAssignmentRepository;
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

/** Coordinates audited Tier 1 Reference master administration. */
@Service
@PreAuthorize("hasAuthority('MASTER:ADMIN')")
public class MasterAdministration {

    private static final Logger logger = LoggerFactory.getLogger(MasterAdministration.class);
    private static final String CODE_PATTERN = "^[A-Z][A-Z0-9_]{0,99}$";
    private static final int MAXIMUM_NAME_LENGTH = 200;

    private final DepartmentRepository departments;
    private final ExpenseCategoryRepository expenseCategories;
    private final UserDepartmentAssignmentRepository assignments;
    private final BusinessAuditRecorder businessAuditRecorder;
    private final Clock clock;

    @Autowired
    public MasterAdministration(
            DepartmentRepository departments,
            ExpenseCategoryRepository expenseCategories,
            UserDepartmentAssignmentRepository assignments,
            BusinessAuditRecorder businessAuditRecorder) {
        this(departments, expenseCategories, assignments, businessAuditRecorder, Clock.systemUTC());
    }

    MasterAdministration(
            DepartmentRepository departments,
            ExpenseCategoryRepository expenseCategories,
            UserDepartmentAssignmentRepository assignments,
            BusinessAuditRecorder businessAuditRecorder,
            Clock clock) {
        this.departments = Objects.requireNonNull(departments, "departments");
        this.expenseCategories = Objects.requireNonNull(expenseCategories, "expenseCategories");
        this.assignments = Objects.requireNonNull(assignments, "assignments");
        this.businessAuditRecorder =
                Objects.requireNonNull(businessAuditRecorder, "businessAuditRecorder");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Transactional
    public UUID createDepartment(String code, String name) {
        String validatedCode = validCode(code);
        String validatedName = validName(name);
        return execute(() -> {
            UUID id = UUID.randomUUID();
            DepartmentEntity department = new DepartmentEntity(
                    id, validatedCode, validatedName, clock.instant());
            departments.saveAndFlush(department);
            record("CREATE_DEPARTMENT", "DEPARTMENT", id);
            return id;
        });
    }

    @Transactional
    public void renameDepartment(UUID departmentId, String name, long expectedVersion) {
        UUID id = requireId(departmentId);
        String validatedName = validName(name);
        requireVersion(expectedVersion);
        execute(() -> {
            DepartmentEntity department = departments.findById(id)
                    .orElseThrow(() -> failure(MasterFailure.NOT_FOUND));
            requireVersion(department.version(), expectedVersion);
            department.rename(validatedName, clock.instant());
            departments.flush();
            record("RENAME_DEPARTMENT", "DEPARTMENT", id);
            return null;
        });
    }

    @Transactional
    public UUID createExpenseCategory(String code, String name) {
        String validatedCode = validCode(code);
        String validatedName = validName(name);
        return execute(() -> {
            UUID id = UUID.randomUUID();
            ExpenseCategoryEntity category = new ExpenseCategoryEntity(
                    id, validatedCode, validatedName, clock.instant());
            expenseCategories.saveAndFlush(category);
            record("CREATE_EXPENSE_CATEGORY", "EXPENSE_CATEGORY", id);
            return id;
        });
    }

    @Transactional
    public void renameExpenseCategory(
            UUID expenseCategoryId, String name, long expectedVersion) {
        UUID id = requireId(expenseCategoryId);
        String validatedName = validName(name);
        requireVersion(expectedVersion);
        execute(() -> {
            ExpenseCategoryEntity category = expenseCategories.findById(id)
                    .orElseThrow(() -> failure(MasterFailure.NOT_FOUND));
            requireVersion(category.version(), expectedVersion);
            category.rename(validatedName, clock.instant());
            expenseCategories.flush();
            record("RENAME_EXPENSE_CATEGORY", "EXPENSE_CATEGORY", id);
            return null;
        });
    }

    @Transactional
    public void deactivateExpenseCategory(UUID expenseCategoryId, long expectedVersion) {
        UUID id = requireId(expenseCategoryId);
        requireVersion(expectedVersion);
        execute(() -> {
            ExpenseCategoryEntity category = expenseCategories.findById(id)
                    .orElseThrow(() -> failure(MasterFailure.NOT_FOUND));
            requireVersion(category.version(), expectedVersion);
            category.deactivate(clock.instant());
            expenseCategories.flush();
            record("DEACTIVATE_EXPENSE_CATEGORY", "EXPENSE_CATEGORY", id);
            return null;
        });
    }

    @Transactional
    public void assignUserToDepartment(FrameworkUserId userId, UUID departmentId) {
        FrameworkUserId validatedUserId = requireUserId(userId);
        UUID validatedDepartmentId = requireId(departmentId);
        execute(() -> {
            requireActiveDepartment(validatedDepartmentId);
            if (assignments.existsById(validatedUserId.value())) {
                throw failure(MasterFailure.CONFLICT);
            }
            Instant now = clock.instant();
            assignments.saveAndFlush(new UserDepartmentAssignmentEntity(
                    validatedUserId.value(), validatedDepartmentId, now));
            recordAssignment("ASSIGN_USER_DEPARTMENT", validatedUserId, validatedDepartmentId);
            return null;
        });
    }

    @Transactional
    public void changeUserDepartment(
            FrameworkUserId userId, UUID departmentId, long expectedVersion) {
        FrameworkUserId validatedUserId = requireUserId(userId);
        UUID validatedDepartmentId = requireId(departmentId);
        requireVersion(expectedVersion);
        execute(() -> {
            requireActiveDepartment(validatedDepartmentId);
            UserDepartmentAssignmentEntity assignment = assignments
                    .findById(validatedUserId.value())
                    .orElseThrow(() -> failure(MasterFailure.NOT_FOUND));
            requireVersion(assignment.version(), expectedVersion);
            assignment.changeDepartment(validatedDepartmentId, clock.instant());
            assignments.flush();
            recordAssignment("CHANGE_USER_DEPARTMENT", validatedUserId, validatedDepartmentId);
            return null;
        });
    }

    private void requireActiveDepartment(UUID departmentId) {
        if (!departments.existsByDepartmentIdAndActiveTrue(departmentId)) {
            throw failure(MasterFailure.NOT_FOUND);
        }
    }

    private void record(String action, String resourceType, UUID resourceId) {
        businessAuditRecorder.record(AuditEvent.of(
                        "MASTER_ADMINISTRATION",
                        currentActor(),
                        action,
                        AuditResult.SUCCESS)
                .withResource(resourceType, resourceId.toString()));
    }

    private void recordAssignment(
            String action, FrameworkUserId userId, UUID departmentId) {
        businessAuditRecorder.record(AuditEvent.of(
                        "MASTER_ADMINISTRATION",
                        currentActor(),
                        action,
                        AuditResult.SUCCESS)
                .withSubject(userId.toString())
                .withResource("DEPARTMENT", departmentId.toString()));
    }

    private static AuditActor currentActor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken
                || !(authentication.getPrincipal() instanceof FrameworkPrincipal principal)) {
            throw failure(MasterFailure.DEPENDENCY_FAILURE);
        }
        return AuditActor.user(principal.userId().toString());
    }

    private static UUID requireId(UUID id) {
        if (id == null) {
            throw failure(MasterFailure.INVALID_INPUT);
        }
        return id;
    }

    private static FrameworkUserId requireUserId(FrameworkUserId userId) {
        if (userId == null) {
            throw failure(MasterFailure.INVALID_INPUT);
        }
        return userId;
    }

    private static String validCode(String code) {
        if (code == null || !code.matches(CODE_PATTERN)) {
            throw failure(MasterFailure.INVALID_INPUT);
        }
        return code;
    }

    private static String validName(String name) {
        if (name == null) {
            throw failure(MasterFailure.INVALID_INPUT);
        }
        String normalized = name.strip();
        if (normalized.isEmpty()
                || normalized.length() > MAXIMUM_NAME_LENGTH
                || normalized.codePoints().anyMatch(Character::isISOControl)) {
            throw failure(MasterFailure.INVALID_INPUT);
        }
        return normalized;
    }

    private static void requireVersion(long version) {
        if (version < 0) {
            throw failure(MasterFailure.INVALID_INPUT);
        }
    }

    private static void requireVersion(long actual, long expected) {
        if (actual != expected) {
            throw failure(MasterFailure.CONCURRENT_MODIFICATION);
        }
    }

    private <T> T execute(Supplier<T> operation) {
        try {
            return operation.get();
        } catch (MasterOperationException exception) {
            throw exception;
        } catch (OptimisticLockingFailureException exception) {
            throw failure(MasterFailure.CONCURRENT_MODIFICATION);
        } catch (DataIntegrityViolationException exception) {
            throw failure(MasterFailure.CONFLICT);
        } catch (RuntimeException exception) {
            logger.error("KOIKI-REF-MST-001: master dependency failed", exception);
            throw failure(MasterFailure.DEPENDENCY_FAILURE);
        }
    }

    private static MasterOperationException failure(MasterFailure failure) {
        return new MasterOperationException(failure);
    }
}
