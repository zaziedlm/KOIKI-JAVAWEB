package org.koikifw.runtimeconsumer;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Test;
import org.koikifw.archunit.KoikiArchitectureRules;
import org.springframework.modulith.core.ApplicationModules;

class RuntimeFoundationConsumerArchitectureTest {

    private static final String BUSINESS_BASE = "org.koikifw.runtimeconsumer";

    @Test
    void verifiesCustomerOwnedModuleBoundaries() {
        ApplicationModules modules = ApplicationModules.of(RuntimeFoundationConsumerApplication.class);

        assertTrue(modules.getModuleByName("workitem").isPresent(), "workitem module was not discovered");
        modules.verify();

        KoikiArchitectureRules.businessModuleRules(BUSINESS_BASE)
                .check(new ClassFileImporter().importPackages(BUSINESS_BASE + ".workitem"));
    }
}
