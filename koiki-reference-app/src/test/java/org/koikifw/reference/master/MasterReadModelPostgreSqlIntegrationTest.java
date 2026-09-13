package org.koikifw.reference.master;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.koikifw.identity.AuthenticationSource;
import org.koikifw.identity.FrameworkPrincipal;
import org.koikifw.identity.FrameworkUserId;
import org.koikifw.reference.ReferencePostgreSqlTestConfiguration;
import org.koikifw.reference.master.application.MasterAdministration;
import org.koikifw.reference.master.application.MasterCatalogQuery;
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
class MasterReadModelPostgreSqlIntegrationTest {

    private static final UUID ACTOR_ID = UUID.fromString("33000000-0000-0000-0000-000000000001");

    @Autowired
    private MasterAdministration administration;

    @Autowired
    private MasterCatalogQuery catalog;

    @Autowired
    private JdbcClient jdbc;

    @BeforeEach
    void prepare() {
        cleanMasterData();
        FrameworkPrincipal principal = new TestPrincipal(
                FrameworkUserId.parse(ACTOR_ID.toString()), Set.of("MASTER:ADMIN"));
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(
                        principal,
                        "n/a",
                        Set.of(new SimpleGrantedAuthority("MASTER:ADMIN"))));
    }

    @AfterEach
    void cleanUp() {
        SecurityContextHolder.clearContext();
        cleanMasterData();
    }

    @Test
    void materializesTierOneApplicationDtosWithSearchAndPaging() {
        UUID financeId = administration.createDepartment("FINANCE", "Finance");
        administration.createDepartment("SALES", "Sales");
        UUID travelId = administration.createExpenseCategory("TRAVEL", "Business Travel");
        administration.createExpenseCategory("SUPPLIES", "Office Supplies");

        var departments = catalog.findDepartments("fin", 0, 1);
        assertThat(departments.totalElements()).isEqualTo(1);
        assertThat(departments.content()).singleElement().satisfies(department -> {
            assertThat(department.departmentId()).isEqualTo(financeId);
            assertThat(department.departmentCode()).isEqualTo("FINANCE");
            assertThat(department.departmentName()).isEqualTo("Finance");
            assertThat(department.active()).isTrue();
            assertThat(department.version()).isZero();
        });

        var categories = catalog.findExpenseCategories("travel", 0, 1);
        assertThat(categories.totalElements()).isEqualTo(1);
        assertThat(categories.content()).singleElement().satisfies(category -> {
            assertThat(category.expenseCategoryId()).isEqualTo(travelId);
            assertThat(category.expenseCategoryCode()).isEqualTo("TRAVEL");
            assertThat(category.expenseCategoryName()).isEqualTo("Business Travel");
        });
    }

    private void cleanMasterData() {
        jdbc.sql("delete from koiki_audit_event where event_type = 'MASTER_ADMINISTRATION'").update();
        jdbc.sql("delete from kkref_user_department_assignment").update();
        jdbc.sql("delete from kkref_expense_category").update();
        jdbc.sql("delete from kkref_department").update();
    }

    private record TestPrincipal(FrameworkUserId userId, Set<String> permissions)
            implements FrameworkPrincipal {

        @Override
        public AuthenticationSource authenticationSource() {
            return AuthenticationSource.LOCAL;
        }
    }
}
