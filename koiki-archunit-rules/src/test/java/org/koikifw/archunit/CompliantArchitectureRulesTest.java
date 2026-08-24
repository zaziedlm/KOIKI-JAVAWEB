package org.koikifw.archunit;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Test;

class CompliantArchitectureRulesTest {

    private static final String BUSINESS_BASE =
            "org.koikifw.archunit.fixture.compliant.business";
    private static final String OWNERSHIP_BASE =
            "org.koikifw.archunit.fixture.compliant.ownership";

    @Test
    void compliantBusinessRulesPass() {
        KoikiArchitectureRules.businessModuleRules(BUSINESS_BASE)
                .check(importPackages(BUSINESS_BASE));
    }

    @Test
    void explicitAllowancesPass() {
        KoikiArchitectureRules.businessModuleRules(BUSINESS_BASE)
                .check(importPackages(BUSINESS_BASE + ".simple", BUSINESS_BASE + ".rich"));
    }

    @Test
    void optionalResponsibilitiesMayBeAbsent() {
        KoikiArchitectureRules.businessModuleRules(BUSINESS_BASE)
                .check(importPackages(BUSINESS_BASE + ".simple"));
    }

    @Test
    void richJpaSharedBoundaryPasses() {
        KoikiArchitectureRules.businessModuleRules(BUSINESS_BASE)
                .check(importPackages(BUSINESS_BASE + ".rich"));
    }

    @Test
    void rule19AllowsDtoConversion() {
        KoikiArchitectureRules.businessModuleRules(BUSINESS_BASE)
                .check(importPackages(BUSINESS_BASE + ".rich"));
    }

    @Test
    void compliantOwnershipRulesPass() {
        KoikiArchitectureRules.frameworkOwnershipRules(
                        OWNERSHIP_BASE + ".framework",
                        OWNERSHIP_BASE + ".reference",
                        OWNERSHIP_BASE + ".customer")
                .check(importPackages(OWNERSHIP_BASE));
    }

    private static JavaClasses importPackages(String... packageNames) {
        return new ClassFileImporter().importPackages(packageNames);
    }
}
