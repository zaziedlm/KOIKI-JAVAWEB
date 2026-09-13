package org.koikifw.reference.master.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.koikifw.audit.AuditEvent;
import org.koikifw.audit.BusinessAuditRecorder;
import org.koikifw.identity.AuthenticationSource;
import org.koikifw.identity.FrameworkPrincipal;
import org.koikifw.identity.FrameworkUserId;
import org.koikifw.reference.master.adapter.outbound.persistence.DepartmentEntity;
import org.koikifw.reference.master.adapter.outbound.persistence.DepartmentRepository;
import org.koikifw.reference.master.adapter.outbound.persistence.ExpenseCategoryRepository;
import org.koikifw.reference.master.adapter.outbound.persistence.UserDepartmentAssignmentRepository;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class MasterAdministrationTest {

    private static final UUID ACTOR_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final Instant NOW = Instant.parse("2026-09-13T00:00:00Z");

    @Mock
    private DepartmentRepository departments;

    @Mock
    private ExpenseCategoryRepository expenseCategories;

    @Mock
    private UserDepartmentAssignmentRepository assignments;

    @Mock
    private BusinessAuditRecorder audit;

    private MasterAdministration administration;

    @BeforeEach
    void setUp() {
        administration = new MasterAdministration(
                departments,
                expenseCategories,
                assignments,
                audit,
                Clock.fixed(NOW, ZoneOffset.UTC));
        FrameworkPrincipal principal = new TestPrincipal(
                FrameworkUserId.parse(ACTOR_ID.toString()), Set.of("MASTER:ADMIN"));
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(
                        principal,
                        "n/a",
                        Set.of(new SimpleGrantedAuthority("MASTER:ADMIN"))));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createsNormalizedDepartmentAndRecordsSafeBusinessAudit() {
        when(departments.saveAndFlush(any(DepartmentEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UUID createdId = administration.createDepartment("FINANCE", " Finance ");

        assertThat(createdId).isNotNull();
        ArgumentCaptor<AuditEvent> event = ArgumentCaptor.forClass(AuditEvent.class);
        verify(audit).record(event.capture());
        assertThat(event.getValue().eventType()).isEqualTo("MASTER_ADMINISTRATION");
        assertThat(event.getValue().action()).isEqualTo("CREATE_DEPARTMENT");
        assertThat(event.getValue().actor().id()).contains(ACTOR_ID.toString());
        assertThat(event.getValue().resourceType()).contains("DEPARTMENT");
        assertThat(event.getValue().resourceId()).contains(createdId.toString());
    }

    @Test
    void rejectsInvalidCodeBeforePersistenceOrAudit() {
        assertThatThrownBy(() -> administration.createDepartment("finance", "Finance"))
                .isInstanceOf(MasterOperationException.class)
                .extracting(exception -> ((MasterOperationException) exception).failure())
                .isEqualTo(MasterFailure.INVALID_INPUT);

        verify(departments, never()).saveAndFlush(any());
        verify(audit, never()).record(any());
    }

    @Test
    void mapsUniqueConstraintFailureToConflictWithoutAudit() {
        when(departments.saveAndFlush(any(DepartmentEntity.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate"));

        assertThatThrownBy(() -> administration.createDepartment("FINANCE", "Finance"))
                .isInstanceOf(MasterOperationException.class)
                .extracting(exception -> ((MasterOperationException) exception).failure())
                .isEqualTo(MasterFailure.CONFLICT);

        verify(audit, never()).record(any());
    }

    @Test
    void rejectsStaleVersionBeforeMutationIsFlushedOrAudited() {
        UUID departmentId = UUID.randomUUID();
        DepartmentEntity department = new DepartmentEntity(
                departmentId, "FINANCE", "Finance", NOW);
        when(departments.findById(departmentId)).thenReturn(Optional.of(department));

        assertThatThrownBy(() -> administration.renameDepartment(departmentId, "Accounting", 1))
                .isInstanceOf(MasterOperationException.class)
                .extracting(exception -> ((MasterOperationException) exception).failure())
                .isEqualTo(MasterFailure.CONCURRENT_MODIFICATION);

        verify(departments, never()).flush();
        verify(audit, never()).record(any());
    }

    private record TestPrincipal(FrameworkUserId userId, Set<String> permissions)
            implements FrameworkPrincipal {

        @Override
        public AuthenticationSource authenticationSource() {
            return AuthenticationSource.LOCAL;
        }
    }
}
