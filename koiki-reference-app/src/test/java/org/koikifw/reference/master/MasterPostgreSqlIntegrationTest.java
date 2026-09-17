package org.koikifw.reference.master;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Set;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.koikifw.identity.AuthenticationSource;
import org.koikifw.identity.FrameworkPrincipal;
import org.koikifw.identity.FrameworkUserId;
import org.koikifw.reference.ReferencePostgreSqlTestConfiguration;
import org.koikifw.reference.master.application.MasterAdministration;
import org.koikifw.reference.master.application.MasterFailure;
import org.koikifw.reference.master.application.MasterOperationException;
import org.koikifw.reference.master.contract.MasterAvailabilityQuery;
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
class MasterPostgreSqlIntegrationTest {

    private static final UUID ACTOR_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");
    private static final UUID APPLICANT_ID = UUID.fromString("20000000-0000-0000-0000-000000000002");

    @Autowired
    private MasterAdministration administration;

    @Autowired
    private MasterAvailabilityQuery availability;

    @Autowired
    private JdbcClient jdbc;

    @Autowired
    private Flyway referenceFlyway;

    @BeforeEach
    void authenticateMasterAdministrator() {
        FrameworkPrincipal principal = new TestPrincipal(
                FrameworkUserId.parse(ACTOR_ID.toString()), Set.of("MASTER:ADMIN"));
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(
                        principal,
                        "n/a",
                        Set.of(new SimpleGrantedAuthority("MASTER:ADMIN"))));
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void appliesV1AndKeepsMasterMutationAvailabilityAndAuditConsistent() {
        UUID departmentId = administration.createDepartment("FINANCE", "Finance");
        UUID categoryId = administration.createExpenseCategory("TRAVEL", "Travel");
        FrameworkUserId applicantId = FrameworkUserId.parse(APPLICANT_ID.toString());
        administration.assignUserToDepartment(applicantId, departmentId);

        assertThat(availability.isActiveDepartment(departmentId)).isTrue();
        assertThat(availability.isActiveExpenseCategory(categoryId)).isTrue();
        assertThat(availability.isUserAssignedToActiveDepartment(applicantId, departmentId))
                .isTrue();

        administration.deactivateExpenseCategory(categoryId, 0);
        assertThat(availability.isActiveExpenseCategory(categoryId)).isFalse();

        assertThat(count("kkref_department")).isEqualTo(1);
        assertThat(count("kkref_expense_category")).isEqualTo(1);
        assertThat(count("kkref_user_department_assignment")).isEqualTo(1);
        assertThat(jdbc.sql("""
                        select count(*) from koiki_audit_event
                        where event_type = 'MASTER_ADMINISTRATION'
                          and result = 'SUCCESS'
                        """)
                .query(Long.class)
                .single())
                .isEqualTo(4);

        long auditCountBeforeConflict = countMasterAuditEvents();
        assertThatThrownBy(() -> administration.createDepartment("FINANCE", "Duplicate"))
                .isInstanceOf(MasterOperationException.class)
                .extracting(exception -> ((MasterOperationException) exception).failure())
                .isEqualTo(MasterFailure.CONFLICT);
        assertThat(count("kkref_department")).isEqualTo(1);
        assertThat(countMasterAuditEvents()).isEqualTo(auditCountBeforeConflict);
    }

    @Test
    void ownsSeparateHistoryAndHasNoReferenceToFrameworkForeignKey() {
        assertThat(count("kkref_flyway_history")).isGreaterThanOrEqualTo(1);
        assertThat(count("koiki_flyway_history")).isGreaterThanOrEqualTo(1);
        assertThat(referenceFlyway.migrate().migrationsExecuted).isZero();

        Integer frameworkForeignKeys = jdbc.sql("""
                        select count(*)
                        from pg_constraint constraint_definition
                        join pg_class child
                          on child.oid = constraint_definition.conrelid
                        join pg_class parent
                          on parent.oid = constraint_definition.confrelid
                        where constraint_definition.contype = 'f'
                          and child.relname like 'kkref_%'
                          and parent.relname like 'koiki_%'
                        """)
                .query(Integer.class)
                .single();
        assertThat(frameworkForeignKeys).isZero();
    }

    private long count(String table) {
        if (!Set.of(
                        "kkref_department",
                        "kkref_expense_category",
                        "kkref_user_department_assignment",
                        "kkref_flyway_history",
                        "koiki_flyway_history")
                .contains(table)) {
            throw new IllegalArgumentException("Unexpected table");
        }
        return jdbc.sql("select count(*) from " + table).query(Long.class).single();
    }

    private long countMasterAuditEvents() {
        return jdbc.sql("""
                        select count(*) from koiki_audit_event
                        where event_type = 'MASTER_ADMINISTRATION'
                        """)
                .query(Long.class)
                .single();
    }

    private record TestPrincipal(FrameworkUserId userId, Set<String> permissions)
            implements FrameworkPrincipal {

        @Override
        public AuthenticationSource authenticationSource() {
            return AuthenticationSource.LOCAL;
        }
    }
}
