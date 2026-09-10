package org.koikifw.reference.identity.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.password.CompromisedPasswordChecker;
import org.springframework.security.web.authentication.password.HaveIBeenPwnedRestApiPasswordChecker;

class ReferenceSecurityConfigurationTest {

    private final ReferenceSecurityConfiguration configuration =
            new ReferenceSecurityConfiguration();

    @Test
    void usesSpringCompromisedPasswordCheckerForIdentityAdministration() {
        CompromisedPasswordChecker checker = configuration.referenceCompromisedPasswordChecker();

        assertThat(checker).isExactlyInstanceOf(HaveIBeenPwnedRestApiPasswordChecker.class);
    }
}
