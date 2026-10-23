package org.koikifw.reference;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.koikifw.archunit.KoikiArchitectureRules;
import org.koikifw.archunit.ModuleEventLevel;

class ReferenceArchitectureTest {

    @Test
    void followsBusinessModuleRules() {
        var classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("org.koikifw.reference");

        KoikiArchitectureRules.businessModuleRules(
                "org.koikifw.reference", Map.of("notification", ModuleEventLevel.LEVEL_2)).check(classes);
    }

    @Test
    void usesOnlyFrameworkIdentityPublicBoundary() {
        var classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("org.koikifw.identity", "org.koikifw.reference");

        KoikiArchitectureRules.frameworkOwnershipRules(
                        "org.koikifw.identity", "org.koikifw.reference")
                .check(classes);
    }
}
