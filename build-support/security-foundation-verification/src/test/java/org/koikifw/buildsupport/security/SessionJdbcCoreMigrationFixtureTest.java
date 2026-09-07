package org.koikifw.buildsupport.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.koikifw.identity.FrameworkPrincipal;
import org.koikifw.identity.FrameworkUserId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.session.Session;
import org.springframework.session.SessionRepository;

@SpringBootTest(
        classes = SessionJdbcCoreMigrationFixtureTest.FixtureApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {
            "spring.jpa.hibernate.ddl-auto=validate",
            "spring.jpa.open-in-view=false",
            "koiki.identity.local-authentication.enabled=true",
            "koiki.identity.login-attempt.source-protection=EXTERNAL"
        })
class SessionJdbcCoreMigrationFixtureTest {

    private static final UUID USER_ID =
            UUID.fromString("00000000-0000-4000-8000-000000000301");
    private static final String RAW_PASSWORD = "B3-fixture-password-value";

    @Autowired
    private JdbcClient jdbcClient;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private Environment environment;

    @Autowired
    private SessionRepository<? extends Session> sessionRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthenticationConfiguration authenticationConfiguration;

    @BeforeEach
    void clearFixtureState() {
        jdbcClient.sql("DELETE FROM koiki_session_attributes").update();
        jdbcClient.sql("DELETE FROM koiki_session").update();
        jdbcClient.sql("DELETE FROM koiki_login_attempt").update();
        jdbcClient.sql("DELETE FROM koiki_password_credential WHERE user_id = :userId")
                .param("userId", USER_ID)
                .update();
        jdbcClient.sql("DELETE FROM koiki_user WHERE user_id = :userId")
                .param("userId", USER_ID)
                .update();
    }

    @Test
    void appliesOnlyTheApprovedSessionTablesAndIsRestartSafe() {
        List<String> tables = jdbcClient.sql(
                        """
                        SELECT table_name
                        FROM information_schema.tables
                        WHERE table_schema = 'public'
                          AND table_name IN (
                              'koiki_session', 'koiki_session_attributes',
                              'spring_session', 'spring_session_attributes')
                        ORDER BY table_name
                        """)
                .query(String.class)
                .list();
        assertThat(tables).containsExactly("koiki_session", "koiki_session_attributes");

        List<String> indexes = jdbcClient.sql(
                        """
                        SELECT indexname
                        FROM pg_indexes
                        WHERE schemaname = 'public' AND tablename = 'koiki_session'
                        ORDER BY indexname
                        """)
                .query(String.class)
                .list();
        assertThat(indexes)
                .containsExactly(
                        "ix_koiki_session_expiry_time",
                        "ix_koiki_session_principal_name",
                        "pk_koiki_session",
                        "uk_koiki_session_session_id");

        int migrationsExecuted = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration/koiki")
                .table("koiki_flyway_history")
                .load()
                .migrate()
                .migrationsExecuted;
        assertThat(migrationsExecuted).isZero();
    }

    @Test
    void usesOnSaveAndWritesOnlyChangedAttributes() {
        assertThat(environment.getProperty("spring.session.jdbc.initialize-schema"))
                .isEqualTo("never");
        assertThat(environment.getProperty("spring.session.jdbc.table-name"))
                .isEqualTo("koiki_session");
        assertThat(environment.getProperty("spring.session.jdbc.cleanup-cron"))
                .isEqualTo("-");
        assertThat(environment.getProperty("spring.session.jdbc.flush-mode"))
                .isEqualTo("on-save");
        assertThat(environment.getProperty("spring.session.jdbc.save-mode"))
                .isEqualTo("on-set-attribute");

        verifyOnSaveAndChangedAttributeWrites(sessionRepository);
    }

    private <S extends Session> void verifyOnSaveAndChangedAttributeWrites(
            SessionRepository<S> repository) {
        S session = repository.createSession();
        session.setAttribute("koiki.fixture.permission", "ORDER:READ");
        assertThat(sessionRowCount(session.getId())).isZero();

        repository.save(session);
        assertThat(sessionRowCount(session.getId())).isOne();
        long originalVersion = attributeRowVersion(session.getId());

        S unchanged = Objects.requireNonNull(repository.findById(session.getId()));
        assertThat(unchanged.<String>getAttribute("koiki.fixture.permission"))
                .isEqualTo("ORDER:READ");
        repository.save(unchanged);
        assertThat(attributeRowVersion(session.getId())).isEqualTo(originalVersion);

        unchanged.setAttribute("koiki.fixture.permission", "ORDER:WRITE");
        repository.save(unchanged);
        assertThat(attributeRowVersion(session.getId())).isNotEqualTo(originalVersion);
        assertThat(Objects.requireNonNull(repository.findById(session.getId()))
                        .<String>getAttribute("koiki.fixture.permission"))
                .isEqualTo("ORDER:WRITE");
    }

    @Test
    void serializesAFrameworkPrincipalWithoutCredentialsAndIndexesTheImmutableUserId()
            throws Exception {
        String loginEmail = "session-user" + Character.toString(64) + "invalid.example";
        jdbcClient.sql(
                        """
                        INSERT INTO koiki_user(user_id, email, canonical_email, status)
                        VALUES (:userId, :email, :email, 'ACTIVE')
                        """)
                .param("userId", USER_ID)
                .param("email", loginEmail)
                .update();
        jdbcClient.sql(
                        """
                        INSERT INTO koiki_password_credential(user_id, encoded_password)
                        VALUES (:userId, :encodedPassword)
                        """)
                .param("userId", USER_ID)
                .param("encodedPassword", passwordEncoder.encode(RAW_PASSWORD))
                .update();

        Authentication authentication = authenticationConfiguration
                .getAuthenticationManager()
                .authenticate(UsernamePasswordAuthenticationToken.unauthenticated(
                        loginEmail, RAW_PASSWORD));
        assertThat(authentication.getCredentials()).isNull();

        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);
        saveAndAssertRestoredPrincipal(sessionRepository, securityContext);
    }

    private <S extends Session> void saveAndAssertRestoredPrincipal(
            SessionRepository<S> repository, SecurityContext securityContext) {
        S session = repository.createSession();
        session.setAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                securityContext);
        repository.save(session);

        String principalName = jdbcClient.sql(
                        "SELECT principal_name FROM koiki_session WHERE session_id = :sessionId")
                .param("sessionId", session.getId())
                .query(String.class)
                .single();
        assertThat(principalName).isEqualTo(USER_ID.toString());

        Session restoredSession = Objects.requireNonNull(repository.findById(session.getId()));
        SecurityContext restoredContext = Objects.requireNonNull(restoredSession.getAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY));
        assertThat(restoredContext.getAuthentication().getCredentials()).isNull();
        assertThat(restoredContext.getAuthentication().getPrincipal())
                .isInstanceOf(FrameworkPrincipal.class);
        FrameworkPrincipal restoredPrincipal =
                (FrameworkPrincipal) restoredContext.getAuthentication().getPrincipal();
        assertThat(restoredPrincipal.userId())
                .isEqualTo(FrameworkUserId.parse(USER_ID.toString()));
        assertThat(restoredPrincipal).isInstanceOf(UserDetails.class);
        assertThat(((UserDetails) restoredPrincipal).getPassword()).isEmpty();

        Integer attributesContainingPassword = jdbcClient.sql(
                        """
                        SELECT count(*)
                        FROM koiki_session_attributes
                        WHERE position(convert_to(:rawPassword, 'UTF8') in attribute_bytes) > 0
                        """)
                .param("rawPassword", RAW_PASSWORD)
                .query(Integer.class)
                .single();
        assertThat(attributesContainingPassword).isZero();
    }

    private Integer sessionRowCount(String sessionId) {
        return jdbcClient.sql("SELECT count(*) FROM koiki_session WHERE session_id = :sessionId")
                .param("sessionId", sessionId)
                .query(Integer.class)
                .single();
    }

    private Long attributeRowVersion(String sessionId) {
        return jdbcClient.sql(
                        """
                        SELECT a.xmin::text::bigint
                        FROM koiki_session_attributes a
                        JOIN koiki_session s ON s.primary_id = a.session_primary_id
                        WHERE s.session_id = :sessionId
                          AND a.attribute_name = 'koiki.fixture.permission'
                        """)
                .param("sessionId", sessionId)
                .query(Long.class)
                .single();
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import(AuditPostgreSqlTestConfiguration.class)
    static class FixtureApplication {}
}
