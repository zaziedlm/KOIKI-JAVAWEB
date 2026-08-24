package org.koikifw.templateverification;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.Modulithic;
import org.springframework.modulith.core.ApplicationModules;

@Modulithic
class ApplicationModulesVerificationTest {

    @Test
    void verifiesGeneratedTierOneAndTierTwoModules() {
        ApplicationModules.of(ApplicationModulesVerificationTest.class).verify();
    }
}
