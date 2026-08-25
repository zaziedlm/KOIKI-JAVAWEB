package org.koikifw.templateverification;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Test;
import org.koikifw.archunit.KoikiArchitectureRules;

class GeneratedFeatureArchitectureRulesTest {

    private static final String BUSINESS_BASE = "org.koikifw.templateverification";

    @Test
    void generatedTierOneAndTierTwoComplyWithKoikiRules() {
        var generatedFeatures = new ClassFileImporter().importPackages(
                BUSINESS_BASE + ".catalog",
                BUSINESS_BASE + ".approval");

        KoikiArchitectureRules.businessModuleRules(BUSINESS_BASE)
                .check(generatedFeatures);
    }
}
