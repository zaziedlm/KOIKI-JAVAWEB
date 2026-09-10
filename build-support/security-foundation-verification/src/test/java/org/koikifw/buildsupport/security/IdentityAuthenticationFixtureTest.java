package org.koikifw.buildsupport.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.sql.Types;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.koikifw.audit.AuditEvent;
import org.koikifw.audit.AuditRecordingException;
import org.koikifw.audit.SecurityAuditRecorder;
import org.koikifw.identity.AuthenticationSource;
import org.koikifw.identity.FrameworkPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(
        classes = IdentityAuthenticationFixtureTest.FixtureApplication.class,
        properties = {
            "spring.jpa.hibernate.ddl-auto=validate",
            "spring.jpa.open-in-view=false",
            "spring.main.web-application-type=servlet",
            "koiki.identity.local-authentication.enabled=true",
            "koiki.identity.login-attempt.account-threshold=3",
            "koiki.identity.login-attempt.source-threshold=10",
            "koiki.identity.login-attempt.source-hmac-key-id=fixture-v1"
        })
@Import({
    AuditPostgreSqlTestConfiguration.class,
    IdentityAuthenticationFixtureTest.AuditFailureConfiguration.class
})
class IdentityAuthenticationFixtureTest {

    private static final UUID USER_ID =
            UUID.fromString("00000000-0000-4000-8000-000000000301");
    private static final String SOURCE_HMAC_KEY =
            "MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDE=";

    @DynamicPropertySource
    static void sourceProtectionProperties(DynamicPropertyRegistry registry) {
        registry.add(
                "koiki.identity.login-attempt.source-hmac-key",
                () -> SOURCE_HMAC_KEY);
    }

    @Autowired
    @Qualifier("koikiIdentityAuthenticationProvider")
    private AuthenticationProvider authenticationProvider;

    @Autowired
    private AuthenticationConfiguration authenticationConfiguration;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JdbcClient jdbcClient;

    @Autowired
    private SwitchableSecurityAuditRecorder auditRecorder;

    private final ExecutorService executor = Executors.newFixedThreadPool(8);

    @BeforeEach
    void prepare() {
        auditRecorder.allowRecording();
        jdbcClient.sql("DELETE FROM koiki_audit_event").update();
        jdbcClient.sql("DELETE FROM koiki_login_attempt").update();
        jdbcClient.sql("DELETE FROM koiki_external_identity_link").update();
        jdbcClient.sql("DELETE FROM koiki_password_credential").update();
        jdbcClient.sql("DELETE FROM koiki_role_permission").update();
        jdbcClient.sql("DELETE FROM koiki_user_role").update();
        jdbcClient.sql("DELETE FROM koiki_permission").update();
        jdbcClient.sql("DELETE FROM koiki_role").update();
        jdbcClient.sql("DELETE FROM koiki_user").update();
    }

    @AfterEach
    void stopExecutor() {
        executor.shutdownNow();
    }

    @Test
    void authenticatesPersistentUserAndUpgradesHashAfterAutomaticUnlock() throws Exception {
        insertLocalUser(
                USER_ID,
                "User@example.test",
                "correct-password-value",
                "ACTIVE",
                Instant.now().minusSeconds(60));
        jdbcClient.sql(
                        "UPDATE koiki_password_credential SET encoded_password = :password WHERE user_id = :userId")
                .param("password", "{noop}correct-password-value")
                .param("userId", USER_ID)
                .update();
        assignPermission(USER_ID, "ORDER:READ");

        Authentication result = authenticationConfiguration.getAuthenticationManager().authenticate(
                request("user@example.test", "correct-password-value", "192.0.2.10"));

        assertThat(result.isAuthenticated()).isTrue();
        assertThat(result.getCredentials()).isNull();
        assertThat(result.getPrincipal()).isInstanceOf(FrameworkPrincipal.class);
        FrameworkPrincipal principal = (FrameworkPrincipal) result.getPrincipal();
        assertThat(principal.userId().toString()).isEqualTo(USER_ID.toString());
        assertThat(principal.authenticationSource()).isEqualTo(AuthenticationSource.LOCAL);
        assertThat(principal.permissions()).containsExactly("ORDER:READ");
        assertThat(jdbcClient
                        .sql("SELECT encoded_password FROM koiki_password_credential WHERE user_id = :userId")
                        .param("userId", USER_ID)
                        .query(String.class)
                        .single())
                .startsWith("{bcrypt}")
                .doesNotContain("correct-password-value");
        assertThat(jdbcClient
                        .sql("SELECT locked_until IS NULL FROM koiki_password_credential WHERE user_id = :userId")
                        .param("userId", USER_ID)
                        .query(Boolean.class)
                        .single())
                .isTrue();
        assertThat(jdbcClient
                        .sql("SELECT count(*) FROM koiki_audit_event WHERE event_type = 'IDENTITY_LOGIN' AND result = 'SUCCESS'")
                        .query(Long.class)
                        .single())
                .isEqualTo(1L);
    }

    @Test
    void returnsTheSameGenericFailureForUnknownBadDisabledAndLockedUsers() {
        UUID badUser = USER_ID;
        UUID disabledUser = UUID.fromString("00000000-0000-4000-8000-000000000302");
        UUID lockedUser = UUID.fromString("00000000-0000-4000-8000-000000000303");
        insertLocalUser(badUser, "bad@example.test", "correct-password-value", "ACTIVE", null);
        insertLocalUser(disabledUser, "disabled@example.test", "correct-password-value", "DISABLED", null);
        insertLocalUser(
                lockedUser,
                "locked@example.test",
                "correct-password-value",
                "ACTIVE",
                Instant.now().plusSeconds(600));

        List<UsernamePasswordAuthenticationToken> requests = List.of(
                request("unknown@example.test", "wrong-password-value", "192.0.2.11"),
                request("bad@example.test", "wrong-password-value", "192.0.2.12"),
                request("disabled@example.test", "correct-password-value", "192.0.2.13"),
                request("locked@example.test", "correct-password-value", "192.0.2.14"));

        for (UsernamePasswordAuthenticationToken candidate : requests) {
            assertThatThrownBy(() -> authenticationProvider.authenticate(candidate))
                    .isExactlyInstanceOf(BadCredentialsException.class)
                    .hasMessage("Bad credentials");
        }
    }

    @Test
    void locksExactlyOnceWhenConcurrentFailuresReachTheAccountThreshold() throws Exception {
        insertLocalUser(USER_ID, "parallel@example.test", "correct-password-value", "ACTIVE", null);
        int attempts = 8;
        CountDownLatch ready = new CountDownLatch(attempts);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<?>> futures = new ArrayList<>();
        for (int index = 0; index < attempts; index++) {
            int sourceSuffix = index + 20;
            futures.add(executor.submit(() -> {
                ready.countDown();
                start.await();
                try {
                    authenticationProvider.authenticate(request(
                            "parallel@example.test",
                            "wrong-password-value",
                            "192.0.2." + sourceSuffix));
                } catch (BadCredentialsException expected) {
                    return null;
                }
                throw new AssertionError("Authentication unexpectedly succeeded.");
            }));
        }
        ready.await();
        start.countDown();
        for (Future<?> future : futures) {
            future.get();
        }

        Instant lockedUntil = jdbcClient
                .sql("SELECT locked_until FROM koiki_password_credential WHERE user_id = :userId")
                .param("userId", USER_ID)
                .query(OffsetDateTime.class)
                .single()
                .toInstant();
        int failureCount = jdbcClient
                .sql("SELECT failure_count FROM koiki_login_attempt WHERE scope = 'ACCOUNT' AND user_id = :userId")
                .param("userId", USER_ID)
                .query(Integer.class)
                .single();
        long lockTransitions = jdbcClient
                .sql("SELECT count(*) FROM koiki_audit_event WHERE reason_code = 'ACCOUNT_LOCKED'")
                .query(Long.class)
                .single();
        assertThat(lockedUntil).isAfter(Instant.now());
        assertThat(failureCount).isGreaterThanOrEqualTo(3);
        assertThat(lockTransitions).isEqualTo(1L);
    }

    @Test
    void hashesUnknownSourceAndBlocksItWithoutRetainingTheRawAddress() {
        String source = "198.51.100.40";
        for (int index = 0; index < 10; index++) {
            assertThatThrownBy(() -> authenticationProvider.authenticate(
                            request("unknown@example.test", "wrong-password-value", source)))
                    .isExactlyInstanceOf(BadCredentialsException.class);
        }

        byte[] fingerprint = jdbcClient
                .sql("SELECT source_fingerprint FROM koiki_login_attempt WHERE scope = 'SOURCE'")
                .query(byte[].class)
                .single();
        Instant blockedUntil = jdbcClient
                .sql("SELECT blocked_until FROM koiki_login_attempt WHERE scope = 'SOURCE'")
                .query(OffsetDateTime.class)
                .single()
                .toInstant();
        assertThat(fingerprint).hasSize(32);
        assertThat(new String(fingerprint, StandardCharsets.UTF_8)).doesNotContain(source);
        assertThat(blockedUntil).isAfter(Instant.now());

        assertThatThrownBy(() -> authenticationProvider.authenticate(
                        request("another@example.test", "wrong-password-value", source)))
                .isExactlyInstanceOf(BadCredentialsException.class)
                .hasMessage("Bad credentials");
        assertThat(jdbcClient
                        .sql("SELECT failure_count FROM koiki_login_attempt WHERE scope = 'SOURCE'")
                        .query(Integer.class)
                        .single())
                .isEqualTo(10);
    }

    @Test
    void deniesSuccessWhenAuditFailsAndKeepsFailureProtectionState() {
        insertLocalUser(USER_ID, "audit@example.test", "correct-password-value", "ACTIVE", null);
        auditRecorder.failRecording();

        assertThatThrownBy(() -> authenticationProvider.authenticate(
                        request("audit@example.test", "correct-password-value", "203.0.113.50")))
                .isExactlyInstanceOf(BadCredentialsException.class)
                .hasMessage("Bad credentials");
        assertThatThrownBy(() -> authenticationProvider.authenticate(
                        request("audit@example.test", "wrong-password-value", "203.0.113.51")))
                .isExactlyInstanceOf(BadCredentialsException.class)
                .hasMessage("Bad credentials");

        assertThat(jdbcClient
                        .sql("SELECT failure_count FROM koiki_login_attempt WHERE scope = 'ACCOUNT' AND user_id = :userId")
                        .param("userId", USER_ID)
                        .query(Integer.class)
                        .single())
                .isEqualTo(1);
    }

    @Test
    void generalizesSourceProtectionLookupFailure() {
        jdbcClient.sql("ALTER TABLE koiki_login_attempt RENAME TO koiki_login_attempt_unavailable")
                .update();
        try {
            assertThatThrownBy(() -> authenticationProvider.authenticate(
                            request("unknown@example.test", "wrong-password-value", "203.0.113.60")))
                    .isExactlyInstanceOf(BadCredentialsException.class)
                    .hasMessage("Bad credentials");
        } finally {
            jdbcClient.sql("ALTER TABLE koiki_login_attempt_unavailable RENAME TO koiki_login_attempt")
                    .update();
        }

        assertThat(jdbcClient
                        .sql("SELECT count(*) FROM koiki_audit_event WHERE reason_code = 'DEPENDENCY_FAILURE'")
                        .query(Long.class)
                        .single())
                .isEqualTo(1L);
    }

    private void insertLocalUser(
            UUID userId, String email, String rawPassword, String status, Instant lockedUntil) {
        jdbcClient.sql(
                        """
                        INSERT INTO koiki_user(user_id, email, canonical_email, status)
                        VALUES (:userId, :email, :canonicalEmail, :status)
                        """)
                .param("userId", userId)
                .param("email", email)
                .param("canonicalEmail", email.toLowerCase(Locale.ROOT))
                .param("status", status)
                .update();
        JdbcClient.StatementSpec statement = jdbcClient.sql(
                        """
                        INSERT INTO koiki_password_credential(user_id, encoded_password, locked_until)
                        VALUES (:userId, :encodedPassword, :lockedUntil)
                        """)
                .param("userId", userId)
                .param("encodedPassword", passwordEncoder.encode(rawPassword));
        if (lockedUntil == null) {
            statement.param("lockedUntil", null, Types.TIMESTAMP_WITH_TIMEZONE).update();
        } else {
            statement.param("lockedUntil", lockedUntil.atOffset(ZoneOffset.UTC)).update();
        }
    }

    private void assignPermission(UUID userId, String permissionCode) {
        UUID roleId = UUID.randomUUID();
        UUID permissionId = UUID.randomUUID();
        jdbcClient.sql("INSERT INTO koiki_role(role_id, role_code) VALUES (:id, 'OPERATOR')")
                .param("id", roleId)
                .update();
        jdbcClient.sql("INSERT INTO koiki_permission(permission_id, permission_code) VALUES (:id, :code)")
                .param("id", permissionId)
                .param("code", permissionCode)
                .update();
        jdbcClient.sql("INSERT INTO koiki_user_role(user_id, role_id) VALUES (:userId, :roleId)")
                .param("userId", userId)
                .param("roleId", roleId)
                .update();
        jdbcClient.sql("INSERT INTO koiki_role_permission(role_id, permission_id) VALUES (:roleId, :permissionId)")
                .param("roleId", roleId)
                .param("permissionId", permissionId)
                .update();
    }

    private static UsernamePasswordAuthenticationToken request(
            String email, String password, String remoteAddress) {
        UsernamePasswordAuthenticationToken token =
                UsernamePasswordAuthenticationToken.unauthenticated(email, password);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr(remoteAddress);
        token.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        return token;
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class FixtureApplication {}

    @TestConfiguration(proxyBeanMethods = false)
    static class AuditFailureConfiguration {

        @Bean
        @Primary
        SwitchableSecurityAuditRecorder switchableSecurityAuditRecorder(
                @Qualifier("koikiSecurityAuditRecorder") SecurityAuditRecorder delegate) {
            return new SwitchableSecurityAuditRecorder(delegate);
        }
    }

    static final class SwitchableSecurityAuditRecorder implements SecurityAuditRecorder {

        private final SecurityAuditRecorder delegate;
        private volatile boolean failing;

        SwitchableSecurityAuditRecorder(SecurityAuditRecorder delegate) {
            this.delegate = delegate;
        }

        void allowRecording() {
            failing = false;
        }

        void failRecording() {
            failing = true;
        }

        @Override
        public void record(AuditEvent event) {
            if (failing) {
                throw new AuditRecordingException();
            }
            delegate.record(event);
        }
    }
}
