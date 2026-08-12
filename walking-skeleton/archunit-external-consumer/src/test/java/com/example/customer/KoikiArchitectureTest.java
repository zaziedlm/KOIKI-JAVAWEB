package com.example.customer;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Test;
import org.koikifw.archunit.KoikiArchitectureRules;

class KoikiArchitectureTest {

    @Test
    void enforcesKoikiRulesFromExternalArtifact() {
        KoikiArchitectureRules
                .representativeRules("com.example.customer")
                .check(new ClassFileImporter().importPackages("com.example.customer"));
    }
}
