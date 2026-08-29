package org.koikifw.runtimeconsumer;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Test;
import org.koikifw.archunit.KoikiArchitectureRules;
import org.koikifw.runtimeconsumer.workitem.domain.event.WorkItemCreated;
import org.springframework.modulith.core.ApplicationModules;

class RuntimeFoundationConsumerArchitectureTest {

    private static final String BUSINESS_BASE = "org.koikifw.runtimeconsumer";

    @Test
    void verifiesCustomerOwnedModuleBoundaries() {
        String property = "spring.modulith.detection-strategy";
        String previous = System.getProperty(property);
        System.setProperty(property, KoikiDomainEventDetectionStrategy.class.getName());

        try {
            ApplicationModules modules =
                    ApplicationModules.of(RuntimeFoundationConsumerApplication.class);

            assertTrue(modules.getModuleByName("workitem").isPresent(),
                    "workitem module was not discovered");
            assertTrue(modules.getModuleByName("workreview").isPresent(),
                    "workreview module was not discovered");
            assertTrue(modules.getModuleByName("workitem").orElseThrow()
                            .getNamedInterfaces().getByName("domain.event").orElseThrow()
                            .contains(WorkItemCreated.class),
                    "workitem domain event was not exposed as a named interface");
            modules.verify();
        } finally {
            if (previous == null) {
                System.clearProperty(property);
            } else {
                System.setProperty(property, previous);
            }
        }

        KoikiArchitectureRules.businessModuleRules(BUSINESS_BASE)
                .check(new ClassFileImporter().importPackages(BUSINESS_BASE + ".workitem",
                        BUSINESS_BASE + ".workreview"));
    }
}
