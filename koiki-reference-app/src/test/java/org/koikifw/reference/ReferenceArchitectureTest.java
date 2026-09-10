package org.koikifw.reference;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Test;
import org.koikifw.archunit.KoikiArchitectureRules;

class ReferenceArchitectureTest {

    @Test
    void followsBusinessModuleRules() {
        var classes = new ClassFileImporter().importPackages("org.koikifw.reference");

        KoikiArchitectureRules.businessModuleRules("org.koikifw.reference").check(classes);
    }

    @Test
    void usesOnlyFrameworkIdentityPublicBoundary() {
        var classes = new ClassFileImporter()
                .importPackages("org.koikifw.identity", "org.koikifw.reference");

        KoikiArchitectureRules.frameworkOwnershipRules(
                        "org.koikifw.identity", "org.koikifw.reference")
                .check(classes);
    }
}
