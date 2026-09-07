package org.koikifw.buildsupport.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.koikifw.audit.SecurityAuditRecorder;
import org.koikifw.identity.internal.KoikiIdentityAuthenticationAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.transaction.PlatformTransactionManager;

class IdentityAuthenticationAutoConfigurationContextTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    KoikiIdentityAuthenticationAutoConfiguration.class))
            .withBean(EntityManager.class, () -> mock(EntityManager.class))
            .withBean(JdbcClient.class, () -> mock(JdbcClient.class))
            .withBean(
                    PlatformTransactionManager.class,
                    () -> mock(PlatformTransactionManager.class))
            .withBean(SecurityAuditRecorder.class, () -> mock(SecurityAuditRecorder.class));

    @Test
    void keepsLocalAuthenticationDisabledByDefault() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean("koikiIdentityAuthenticationProvider");
            assertThat(context).doesNotHaveBean(AuthenticationProvider.class);
            assertThat(context).doesNotHaveBean("koikiIdentityLoginAttemptStore");
            assertThat(context).doesNotHaveBean("koikiIdentitySourceFingerprintFactory");
        });
    }

    @Test
    void externalSourceProtectionRequiresNoHmacAndKeepsAccountProtectionBeans() {
        runner.withPropertyValues(
                        "koiki.identity.local-authentication.enabled=true",
                        "koiki.identity.login-attempt.source-protection=EXTERNAL")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasBean("koikiIdentityAuthenticationProvider");
                    assertThat(context).hasBean("koikiIdentityLoginAttemptStore");
                    assertThat(context).doesNotHaveBean("koikiIdentitySourceFingerprintFactory");
                });
    }

    @Test
    void rejectsMissingOrShortApplicationHmacConfiguration() {
        runner.withPropertyValues("koiki.identity.local-authentication.enabled=true")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasRootCauseMessage(
                                    "Invalid KOIKI Identity setting: login-attempt.source-hmac-key-id");
                });

        runner.withPropertyValues(
                        "koiki.identity.local-authentication.enabled=true",
                        "koiki.identity.login-attempt.source-hmac-key-id=fixture-v1",
                        "koiki.identity.login-attempt.source-hmac-key=c2hvcnQ=")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasRootCauseMessage(
                                    "Invalid KOIKI Identity setting: login-attempt.source-hmac-key");
                });
    }
}
