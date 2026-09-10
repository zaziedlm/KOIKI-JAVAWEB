package org.koikifw.buildsupport.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.koikifw.audit.AuditEvent;
import org.koikifw.audit.AuditRecordingException;
import org.koikifw.audit.BusinessAuditRecorder;
import org.koikifw.audit.SecurityAuditRecorder;
import org.koikifw.identity.AuthenticationSource;
import org.koikifw.identity.FrameworkPrincipal;
import org.koikifw.identity.FrameworkUserId;
import org.koikifw.identity.IdentityAdministration;
import org.koikifw.identity.IdentityFailure;
import org.koikifw.identity.IdentityOperationException;
import org.koikifw.identity.IdentityQuery;
import org.koikifw.identity.UserSessionInvalidator;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.password.CompromisedPasswordChecker;
import org.springframework.security.authentication.password.CompromisedPasswordDecision;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootTest(
        classes = IdentityAdministrationFixtureTest.FixtureApplication.class,
        properties = {
            "spring.jpa.hibernate.ddl-auto=validate",
            "spring.jpa.open-in-view=false",
            "spring.main.web-application-type=none"
        })
@Import({
    AuditPostgreSqlTestConfiguration.class,
    IdentityAdministrationFixtureTest.AdministrationDependencies.class
})
class IdentityAdministrationFixtureTest {

    private static final FrameworkUserId ACTOR_ID = FrameworkUserId.parse(
            "00000000-0000-4000-8000-000000000401");

    @Autowired
    private IdentityAdministration administration;

    @Autowired
    private IdentityQuery identityQuery;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JdbcClient jdbcClient;

    @Autowired
    private RecordingSessionInvalidator invalidator;

    @Autowired
    private SwitchableBusinessAuditRecorder businessAudit;

    @Autowired
    private SwitchableAdministrationSecurityAuditRecorder securityAudit;

    @BeforeEach
    void prepare() {
        clearDatabase();
        invalidator.reset();
        businessAudit.allow();
        securityAudit.allow();
        FrameworkPrincipal principal = new FixturePrincipal(ACTOR_ID);
        SecurityContextHolder.getContext()
                .setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                        principal, "", List.of()));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void administersUserLifecycleWithoutExposingEmailInAudit() {
        FrameworkUserId userId = administration.createUser(" User@example.test ");
        assertThat(identityQuery.findById(userId).orElseThrow().email())
                .isEqualTo("User@example.test");

        administration.changeEmail(userId, "Renamed@example.test", 0);
        administration.disable(userId, 1);

        assertThat(identityQuery.findById(userId).orElseThrow().status().name())
                .isEqualTo("DISABLED");
        assertThat(invalidator.userIds()).containsExactly(userId);
        administration.enable(userId, 2);
        assertThat(identityQuery.findById(userId).orElseThrow().status().name())
                .isEqualTo("ACTIVE");
        assertThat(jdbcClient
                        .sql("SELECT count(*) FROM koiki_audit_event WHERE actor_id = :actorId")
                        .param("actorId", ACTOR_ID.toString())
                        .query(Long.class)
                        .single())
                .isEqualTo(4L);
        assertThat(jdbcClient
                        .sql("SELECT count(*) FROM koiki_audit_event WHERE actor_id LIKE '%@%' OR subject_id LIKE '%@%'")
                        .query(Long.class)
                        .single())
                .isZero();
    }

    @Test
    void appliesPasswordPolicyAndInvalidatesSessions() {
        FrameworkUserId userId = administration.createUser("password@example.test");

        assertFailure(
                () -> administration.setLocalPassword(userId, "too-short".toCharArray(), 0),
                IdentityFailure.INVALID_INPUT);
        assertFailure(
                () -> administration.setLocalPassword(
                        userId, "compromised-password-value".toCharArray(), 0),
                IdentityFailure.INVALID_INPUT);

        char[] password = "valid-password-value".toCharArray();
        administration.setLocalPassword(userId, password, 0);

        String encoded = jdbcClient
                .sql("SELECT encoded_password FROM koiki_password_credential WHERE user_id = :userId")
                .param("userId", userId.value())
                .query(String.class)
                .single();
        assertThat(encoded).isNotEqualTo(new String(password));
        assertThat(passwordEncoder.matches(new String(password), encoded)).isTrue();
        assertThat(invalidator.userIds()).containsExactly(userId);
        assertThat(identityQuery.findById(userId).orElseThrow().version()).isEqualTo(1);
    }

    @Test
    void changesRolePermissionsAndInvalidatesAffectedUsers() {
        FrameworkUserId first = administration.createUser("first@example.test");
        FrameworkUserId second = administration.createUser("second@example.test");
        administration.createRole("ORDER_ADMIN");
        administration.registerPermission("ORDER:READ");

        administration.assignRole(first, "ORDER_ADMIN", 0);
        administration.assignRole(second, "ORDER_ADMIN", 0);
        invalidator.reset();
        administration.grantPermission("ORDER_ADMIN", "ORDER:READ", 0);

        assertThat(invalidator.userIds()).containsExactlyInAnyOrder(first, second);
        assertThat(identityQuery.findById(first).orElseThrow().permissionCodes())
                .containsExactly("ORDER:READ");
        assertThat(jdbcClient
                        .sql("SELECT resource_id FROM koiki_audit_event "
                                + "WHERE action = 'GRANT_PERMISSION'")
                        .query(String.class)
                        .single())
                .isEqualTo("ORDER_ADMIN|ORDER:READ");
        assertFailure(
                () -> administration.revokeRole(first, "ORDER_ADMIN", 0),
                IdentityFailure.CONCURRENT_MODIFICATION);

        administration.revokeRole(first, "ORDER_ADMIN", 1);
        invalidator.reset();
        administration.revokePermission("ORDER_ADMIN", "ORDER:READ", 1);
        assertThat(invalidator.userIds()).containsExactly(second);
        administration.revokeRole(second, "ORDER_ADMIN", 1);
        administration.deletePermission("ORDER:READ");
        administration.deleteRole("ORDER_ADMIN", 2);

        assertThat(jdbcClient
                        .sql("SELECT count(*) FROM koiki_role")
                        .query(Long.class)
                        .single())
                .isZero();
        assertThat(jdbcClient
                        .sql("SELECT count(*) FROM koiki_permission")
                        .query(Long.class)
                        .single())
                .isZero();
    }

    @Test
    void preservesExactExternalIdentityAndAllowsLastLinkUnlink() {
        FrameworkUserId userId = administration.createUser("external@example.test");
        String issuer = "https://issuer.example.test/";
        administration.linkExternalIdentity(userId, issuer, "opaque-subject", 0);

        assertThat(jdbcClient
                        .sql("SELECT issuer FROM koiki_external_identity_link WHERE user_id = :userId")
                        .param("userId", userId.value())
                        .query(String.class)
                        .single())
                .isEqualTo(issuer);
        assertFailure(
                () -> administration.linkExternalIdentity(
                        userId, issuer, "other-subject", 1),
                IdentityFailure.CONFLICT);

        administration.disable(userId, 1);
        assertFailure(
                () -> administration.linkExternalIdentity(
                        userId, "https://other-issuer.example.test", "other-subject", 2),
                IdentityFailure.CONFLICT);
        invalidator.reset();
        administration.unlinkExternalIdentity(userId, issuer, 2);
        assertThat(jdbcClient
                        .sql("SELECT count(*) FROM koiki_external_identity_link WHERE user_id = :userId")
                        .param("userId", userId.value())
                        .query(Long.class)
                        .single())
                .isZero();
        assertThat(invalidator.userIds()).containsExactly(userId);
    }

    @Test
    void rollsBackBusinessMutationWhenAuditOrInvalidationFails() {
        FrameworkUserId userId = administration.createUser("rollback@example.test");

        businessAudit.fail();
        assertFailure(
                () -> administration.changeEmail(userId, "changed@example.test", 0),
                IdentityFailure.DEPENDENCY_FAILURE);
        businessAudit.allow();
        assertThat(identityQuery.findById(userId).orElseThrow().email())
                .isEqualTo("rollback@example.test");

        invalidator.fail();
        assertFailure(
                () -> administration.setLocalPassword(
                        userId, "valid-password-value".toCharArray(), 0),
                IdentityFailure.DEPENDENCY_FAILURE);
        assertThat(jdbcClient
                        .sql("SELECT count(*) FROM koiki_password_credential WHERE user_id = :userId")
                        .param("userId", userId.value())
                        .query(Long.class)
                        .single())
                .isZero();

        invalidator.reset();
        administration.createRole("ROLLBACK_ROLE");
        invalidator.fail();
        assertFailure(
                () -> administration.assignRole(userId, "ROLLBACK_ROLE", 0),
                IdentityFailure.DEPENDENCY_FAILURE);
        assertThat(identityQuery.findById(userId).orElseThrow().version()).isZero();
        assertThat(jdbcClient
                        .sql("SELECT count(*) FROM koiki_user_role WHERE user_id = :userId")
                        .param("userId", userId.value())
                        .query(Long.class)
                        .single())
                .isZero();

        FrameworkUserId secondUser = administration.createUser("rollback-second@example.test");
        invalidator.reset();
        administration.assignRole(userId, "ROLLBACK_ROLE", 0);
        administration.assignRole(secondUser, "ROLLBACK_ROLE", 0);
        administration.registerPermission("ROLLBACK:READ");
        invalidator.reset();
        invalidator.failOnCall(2);
        assertFailure(
                () -> administration.grantPermission(
                        "ROLLBACK_ROLE", "ROLLBACK:READ", 0),
                IdentityFailure.DEPENDENCY_FAILURE);
        assertThat(jdbcClient
                        .sql("SELECT count(*) FROM koiki_role_permission")
                        .query(Long.class)
                        .single())
                .isZero();

        FrameworkUserId externalUser = administration.createUser("rollback-link@example.test");
        administration.linkExternalIdentity(
                externalUser, "https://rollback-issuer.example.test", "subject", 0);
        invalidator.reset();
        invalidator.fail();
        assertFailure(
                () -> administration.unlinkExternalIdentity(
                        externalUser, "https://rollback-issuer.example.test", 1),
                IdentityFailure.DEPENDENCY_FAILURE);
        assertThat(identityQuery.findById(externalUser).orElseThrow().version()).isEqualTo(1);
        assertThat(jdbcClient
                        .sql("SELECT count(*) FROM koiki_external_identity_link "
                                + "WHERE user_id = :userId")
                        .param("userId", externalUser.value())
                        .query(Long.class)
                        .single())
                .isOne();
    }

    @Test
    void distinguishesDisableAndUnlockSecurityAuditFailureSemantics() {
        FrameworkUserId userId = administration.createUser("protection@example.test");
        administration.setLocalPassword(userId, "valid-password-value".toCharArray(), 0);
        jdbcClient.sql(
                        "UPDATE koiki_password_credential SET locked_until = :lockedUntil WHERE user_id = :userId")
                .param(
                        "lockedUntil",
                        OffsetDateTime.ofInstant(Instant.now().plusSeconds(600), ZoneOffset.UTC))
                .param("userId", userId.value())
                .update();

        securityAudit.fail();
        assertFailure(
                () -> administration.unlockLocalCredential(userId),
                IdentityFailure.DEPENDENCY_FAILURE);
        assertThat(jdbcClient
                        .sql("SELECT locked_until IS NOT NULL FROM koiki_password_credential WHERE user_id = :userId")
                        .param("userId", userId.value())
                        .query(Boolean.class)
                        .single())
                .isTrue();

        invalidator.reset();
        administration.disable(userId, 1);
        assertThat(identityQuery.findById(userId).orElseThrow().status().name())
                .isEqualTo("DISABLED");
        assertThat(invalidator.userIds()).containsExactly(userId);
    }

    private void clearDatabase() {
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

    private static void assertFailure(Runnable operation, IdentityFailure failure) {
        assertThatThrownBy(operation::run)
                .isExactlyInstanceOf(IdentityOperationException.class)
                .extracting(value -> ((IdentityOperationException) value).failure())
                .isEqualTo(failure);
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class FixtureApplication {}

    @TestConfiguration(proxyBeanMethods = false)
    static class AdministrationDependencies {

        @Bean
        CompromisedPasswordChecker compromisedPasswordChecker() {
            return password -> new CompromisedPasswordDecision(
                    "compromised-password-value".equals(password));
        }

        @Bean
        RecordingSessionInvalidator recordingSessionInvalidator() {
            return new RecordingSessionInvalidator();
        }

        @Bean
        @Primary
        SwitchableBusinessAuditRecorder switchableBusinessAuditRecorder(
                @Qualifier("koikiBusinessAuditRecorder") BusinessAuditRecorder delegate) {
            return new SwitchableBusinessAuditRecorder(delegate);
        }

        @Bean
        @Primary
        SwitchableAdministrationSecurityAuditRecorder switchableAdministrationSecurityAuditRecorder(
                @Qualifier("koikiSecurityAuditRecorder") SecurityAuditRecorder delegate) {
            return new SwitchableAdministrationSecurityAuditRecorder(delegate);
        }
    }

    private record FixturePrincipal(FrameworkUserId userId) implements FrameworkPrincipal {
        @Override
        public AuthenticationSource authenticationSource() {
            return AuthenticationSource.LOCAL;
        }

        @Override
        public Set<String> permissions() {
            return Set.of("IDENTITY:ADMIN");
        }
    }

    static final class RecordingSessionInvalidator implements UserSessionInvalidator {

        private final List<FrameworkUserId> userIds = new ArrayList<>();
        private boolean failing;
        private int failOnCall;
        private int calls;

        @Override
        public void invalidateAll(FrameworkUserId userId) {
            calls++;
            if (failing || (failOnCall > 0 && calls == failOnCall)) {
                throw new IllegalStateException("fixture session failure");
            }
            userIds.add(userId);
        }

        List<FrameworkUserId> userIds() {
            return List.copyOf(userIds);
        }

        void fail() {
            failing = true;
        }

        void failOnCall(int invocation) {
            failOnCall = invocation;
        }

        void reset() {
            failing = false;
            failOnCall = 0;
            calls = 0;
            userIds.clear();
        }
    }

    static final class SwitchableBusinessAuditRecorder implements BusinessAuditRecorder {

        private final BusinessAuditRecorder delegate;
        private boolean failing;

        SwitchableBusinessAuditRecorder(BusinessAuditRecorder delegate) {
            this.delegate = delegate;
        }

        @Override
        public void record(AuditEvent event) {
            if (failing) {
                throw new AuditRecordingException();
            }
            delegate.record(event);
        }

        void fail() {
            failing = true;
        }

        void allow() {
            failing = false;
        }
    }

    static final class SwitchableAdministrationSecurityAuditRecorder
            implements SecurityAuditRecorder {

        private final SecurityAuditRecorder delegate;
        private boolean failing;

        SwitchableAdministrationSecurityAuditRecorder(SecurityAuditRecorder delegate) {
            this.delegate = delegate;
        }

        @Override
        public void record(AuditEvent event) {
            if (failing) {
                throw new AuditRecordingException();
            }
            delegate.record(event);
        }

        void fail() {
            failing = true;
        }

        void allow() {
            failing = false;
        }
    }
}
