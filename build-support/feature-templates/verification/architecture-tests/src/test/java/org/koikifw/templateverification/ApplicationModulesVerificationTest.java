package org.koikifw.templateverification;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.Modulithic;
import org.springframework.modulith.core.ApplicationModules;

@Modulithic
class ApplicationModulesVerificationTest {

    @Test
    void verifiesGeneratedTierOneAndTierTwoModules() {
        ApplicationModules modules = ApplicationModules.of(ApplicationModulesVerificationTest.class);

        assertTrue(modules.getModuleByName("catalog").isPresent(), "Tier 1 catalog module was not discovered");
        assertTrue(modules.getModuleByName("approval").isPresent(), "Tier 2 approval module was not discovered");

        modules.verify();
    }
}
