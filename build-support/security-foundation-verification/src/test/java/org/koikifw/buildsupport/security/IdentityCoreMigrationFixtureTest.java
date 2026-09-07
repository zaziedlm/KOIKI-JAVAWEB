package org.koikifw.buildsupport.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.UUID;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.koikifw.identity.FrameworkUserId;
import org.koikifw.identity.IdentityQuery;
import org.koikifw.identity.IdentityUser;
import org.koikifw.identity.UserStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;

@SpringBootTest(
        classes = IdentityCoreMigrationFixtureTest.FixtureApplication.class,
        properties = {
            "spring.jpa.hibernate.ddl-auto=validate",
            "spring.jpa.open-in-view=false",
            "spring.main.web-application-type=none"
        })
class IdentityCoreMigrationFixtureTest {

    private static final UUID USER_ID =
            UUID.fromString("00000000-0000-4000-8000-000000000201");
    private static final UUID OTHER_USER_ID =
            UUID.fromString("00000000-0000-4000-8000-000000000202");
    private static final UUID ROLE_ID =
            UUID.fromString("00000000-0000-4000-8000-000000000211");
    private static final UUID PERMISSION_ID =
            UUID.fromString("00000000-0000-4000-8000-000000000221");

    @Autowired
    private JdbcClient jdbcClient;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private IdentityQuery identityQuery;

    @BeforeEach
    void clearIdentityState() {
        jdbcClient.sql("DELETE FROM koiki_login_attempt").update();
        jdbcClient.sql("DELETE FROM koiki_external_identity_link").update();
        jdbcClient.sql("DELETE FROM koiki_password_credential").update();
        jdbcClient.sql("DELETE FROM koiki_role_permission").update();
        jdbcClient.sql("DELETE FROM koiki_user_role").update();
        jdbcClient.sql("DELETE FROM koiki_permission").update();
        jdbcClient.sql("DELETE FROM koiki_role").update();
        jdbcClient.sql("DELETE FROM koiki_user").update();
    }

    @Test
    void appliesTheEightApprovedIdentityTablesAndIsRestartSafe() {
        List<String> tables = jdbcClient.sql(
                        """
                        SELECT table_name
                        FROM information_schema.tables
                        WHERE table_schema = 'public' AND table_name LIKE 'koiki_%'
                        ORDER BY table_name
                        """)
                .query(String.class)
                .list();

        assertThat(tables)
                .contains(
                        "koiki_user",
                        "koiki_role",
                        "koiki_permission",
                        "koiki_user_role",
                        "koiki_role_permission",
                        "koiki_password_credential",
                        "koiki_login_attempt",
                        "koiki_external_identity_link")
                .doesNotContain("koiki_password_reset");

        List<Integer> timestampPrecisions = jdbcClient.sql(
                        """
                        SELECT DISTINCT datetime_precision
                        FROM information_schema.columns
                        WHERE table_schema = 'public'
                          AND table_name IN (
                              'koiki_user', 'koiki_role', 'koiki_permission',
                              'koiki_password_credential', 'koiki_login_attempt',
                              'koiki_external_identity_link')
                          AND data_type = 'timestamp with time zone'
                        """)
                .query(Integer.class)
                .list();
        assertThat(timestampPrecisions).containsExactly(6);

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
    void rejectsCanonicalEmailCollisionAndKeepsUserIdImmutable() {
        insertUser(USER_ID, email("User"), email("user"));
        jdbcClient.sql(
                        "INSERT INTO koiki_password_credential(user_id, encoded_password) VALUES (:userId, '{fixture}encoded')")
                .param("userId", USER_ID)
                .update();

        assertThatThrownBy(() -> insertUser(
                        OTHER_USER_ID, email("user"), email("user")))
                .isInstanceOf(DataIntegrityViolationException.class);

        assertThatThrownBy(() -> jdbcClient.sql(
                        "UPDATE koiki_user SET user_id = :replacement WHERE user_id = :current")
                .param("replacement", OTHER_USER_ID)
                .param("current", USER_ID)
                .update()).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rejectsEmailAndCanonicalEmailThatDoNotRepresentTheSameValue() {
        assertThatThrownBy(() -> insertUser(
                        USER_ID, email("presented-user"), email("different-user")))
                .isInstanceOf(DataIntegrityViolationException.class);

        assertThatThrownBy(() -> insertUser(
                        USER_ID, " " + email("presented-user"), email("presented-user")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void enforcesAuthorityAndExternalLinkOwnershipConstraints() {
        insertUser(USER_ID, email("first"), email("first"));
        insertUser(OTHER_USER_ID, email("second"), email("second"));
        jdbcClient.sql("INSERT INTO koiki_role(role_id, role_code) VALUES (:id, 'OPERATOR')")
                .param("id", ROLE_ID)
                .update();
        jdbcClient.sql(
                        "INSERT INTO koiki_permission(permission_id, permission_code) VALUES (:id, 'ORDER:READ')")
                .param("id", PERMISSION_ID)
                .update();
        jdbcClient.sql("INSERT INTO koiki_user_role(user_id, role_id) VALUES (:userId, :roleId)")
                .param("userId", USER_ID)
                .param("roleId", ROLE_ID)
                .update();
        jdbcClient.sql(
                        "INSERT INTO koiki_role_permission(role_id, permission_id) VALUES (:roleId, :permissionId)")
                .param("roleId", ROLE_ID)
                .param("permissionId", PERMISSION_ID)
                .update();
        insertExternalLink(
                UUID.fromString("00000000-0000-4000-8000-000000000231"),
                USER_ID,
                issuer(),
                "subject-001");

        assertThatThrownBy(() -> insertExternalLink(
                        UUID.fromString("00000000-0000-4000-8000-000000000232"),
                        OTHER_USER_ID,
                        issuer(),
                        "subject-001"))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> insertExternalLink(
                        UUID.fromString("00000000-0000-4000-8000-000000000233"),
                        USER_ID,
                        issuer(),
                        "subject-002"))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbcClient.sql(
                        "INSERT INTO koiki_user_role(user_id, role_id) VALUES (:userId, :roleId)")
                .param("userId", UUID.randomUUID())
                .param("roleId", ROLE_ID)
                .update()).isInstanceOf(DataIntegrityViolationException.class);

        IdentityUser identity = identityQuery
                .findById(FrameworkUserId.parse(USER_ID.toString()))
                .orElseThrow();
        assertThat(identity.status()).isEqualTo(UserStatus.ACTIVE);
        assertThat(identity.roleCodes()).containsExactly("OPERATOR");
        assertThat(identity.permissionCodes()).containsExactly("ORDER:READ");
        assertThat(identity.version()).isZero();
    }

    @Test
    void storesOnlyEncodedCredentialMaterialAndEnforcesAttemptScope() {
        insertUser(USER_ID, email("credential"), email("credential"));
        String rawSecret = "fixture-raw-secret-must-not-be-stored";
        String encoded = "{fixture}encoded-value-without-secret";
        jdbcClient.sql(
                        "INSERT INTO koiki_password_credential(user_id, encoded_password) VALUES (:userId, :encoded)")
                .param("userId", USER_ID)
                .param("encoded", encoded)
                .update();

        List<String> credentialColumns = jdbcClient.sql(
                        """
                        SELECT column_name
                        FROM information_schema.columns
                        WHERE table_schema = 'public'
                          AND table_name = 'koiki_password_credential'
                        ORDER BY ordinal_position
                        """)
                .query(String.class)
                .list();
        String stored = jdbcClient.sql(
                        "SELECT encoded_password FROM koiki_password_credential WHERE user_id = :userId")
                .param("userId", USER_ID)
                .query(String.class)
                .single();
        assertThat(credentialColumns).doesNotContain("password", "raw_password", "salt");
        assertThat(stored).isEqualTo(encoded).doesNotContain(rawSecret);

        assertThatThrownBy(() -> jdbcClient.sql(
                        """
                        INSERT INTO koiki_login_attempt(
                            attempt_id, scope, user_id, source_key_id, source_fingerprint,
                            window_started_at, last_failed_at)
                        VALUES (:id, 'ACCOUNT', :userId, 'forbidden-key', :fingerprint, now(), now())
                        """)
                .param("id", UUID.randomUUID())
                .param("userId", USER_ID)
                .param("fingerprint", new byte[32])
                .update()).isInstanceOf(DataIntegrityViolationException.class);

        insertAccountAttempt();
        assertThatThrownBy(this::insertAccountAttempt)
                .isInstanceOf(DataIntegrityViolationException.class);

        insertSourceAttempt();
        assertThatThrownBy(this::insertSourceAttempt)
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private void insertUser(UUID userId, String email, String canonicalEmail) {
        jdbcClient.sql(
                        """
                        INSERT INTO koiki_user(user_id, email, canonical_email, status)
                        VALUES (:userId, :email, :canonicalEmail, 'ACTIVE')
                        """)
                .param("userId", userId)
                .param("email", email)
                .param("canonicalEmail", canonicalEmail)
                .update();
    }

    private void insertExternalLink(UUID linkId, UUID userId, String issuer, String subject) {
        jdbcClient.sql(
                        """
                        INSERT INTO koiki_external_identity_link(link_id, user_id, issuer, subject)
                        VALUES (:linkId, :userId, :issuer, :subject)
                        """)
                .param("linkId", linkId)
                .param("userId", userId)
                .param("issuer", issuer)
                .param("subject", subject)
                .update();
    }

    private void insertAccountAttempt() {
        jdbcClient.sql(
                        """
                        INSERT INTO koiki_login_attempt(
                            attempt_id, scope, user_id, window_started_at, last_failed_at)
                        VALUES (:id, 'ACCOUNT', :userId, now(), now())
                        """)
                .param("id", UUID.randomUUID())
                .param("userId", USER_ID)
                .update();
    }

    private void insertSourceAttempt() {
        jdbcClient.sql(
                        """
                        INSERT INTO koiki_login_attempt(
                            attempt_id, scope, source_key_id, source_fingerprint,
                            window_started_at, last_failed_at)
                        VALUES (:id, 'SOURCE', 'fixture-key', :fingerprint, now(), now())
                        """)
                .param("id", UUID.randomUUID())
                .param("fingerprint", new byte[32])
                .update();
    }

    private static String email(String localPart) {
        return localPart + Character.toString(64) + "invalid.example";
    }

    private static String issuer() {
        return "https://issuer.invalid.example";
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import(AuditPostgreSqlTestConfiguration.class)
    static class FixtureApplication {}
}
