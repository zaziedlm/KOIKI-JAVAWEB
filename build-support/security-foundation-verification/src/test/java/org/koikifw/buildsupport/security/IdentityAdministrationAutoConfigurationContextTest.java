package org.koikifw.buildsupport.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.Test;
import org.koikifw.audit.BusinessAuditRecorder;
import org.koikifw.audit.SecurityAuditRecorder;
import org.koikifw.identity.IdentityAdministration;
import org.koikifw.identity.IdentityQuery;
import org.koikifw.identity.UserSessionInvalidator;
import org.koikifw.identity.internal.KoikiIdentityAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.security.authentication.password.CompromisedPasswordChecker;

class IdentityAdministrationAutoConfigurationContextTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(KoikiIdentityAutoConfiguration.class))
            .withBean(EntityManagerFactory.class, () -> mock(EntityManagerFactory.class))
            .withBean(EntityManager.class, () -> mock(EntityManager.class));

    @Test
    void keepsQueryButDoesNotCreateAdministrationWhenAnyRequiredDependencyIsMissing() {
        assertAdministrationAbsent(runner
                .withBean(SecurityAuditRecorder.class, () -> mock(SecurityAuditRecorder.class))
                .withBean(UserSessionInvalidator.class, () -> mock(UserSessionInvalidator.class))
                .withBean(
                        CompromisedPasswordChecker.class,
                        () -> mock(CompromisedPasswordChecker.class)));
        assertAdministrationAbsent(runner
                .withBean(BusinessAuditRecorder.class, () -> mock(BusinessAuditRecorder.class))
                .withBean(UserSessionInvalidator.class, () -> mock(UserSessionInvalidator.class))
                .withBean(
                        CompromisedPasswordChecker.class,
                        () -> mock(CompromisedPasswordChecker.class)));
        assertAdministrationAbsent(runner
                .withBean(BusinessAuditRecorder.class, () -> mock(BusinessAuditRecorder.class))
                .withBean(SecurityAuditRecorder.class, () -> mock(SecurityAuditRecorder.class))
                .withBean(
                        CompromisedPasswordChecker.class,
                        () -> mock(CompromisedPasswordChecker.class)));
        assertAdministrationAbsent(runner
                .withBean(BusinessAuditRecorder.class, () -> mock(BusinessAuditRecorder.class))
                .withBean(SecurityAuditRecorder.class, () -> mock(SecurityAuditRecorder.class))
                .withBean(UserSessionInvalidator.class, () -> mock(UserSessionInvalidator.class)));
    }

    @Test
    void createsAdministrationWhenAllRequiredDependenciesExist() {
        runner.withBean(BusinessAuditRecorder.class, () -> mock(BusinessAuditRecorder.class))
                .withBean(SecurityAuditRecorder.class, () -> mock(SecurityAuditRecorder.class))
                .withBean(UserSessionInvalidator.class, () -> mock(UserSessionInvalidator.class))
                .withBean(
                        CompromisedPasswordChecker.class,
                        () -> mock(CompromisedPasswordChecker.class))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(IdentityQuery.class);
                    assertThat(context).hasSingleBean(IdentityAdministration.class);
                });
    }

    private static void assertAdministrationAbsent(ApplicationContextRunner candidate) {
        candidate.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(IdentityQuery.class);
            assertThat(context).doesNotHaveBean(IdentityAdministration.class);
        });
    }
}
