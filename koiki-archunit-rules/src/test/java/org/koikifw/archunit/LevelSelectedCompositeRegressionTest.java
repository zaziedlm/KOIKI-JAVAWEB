package org.koikifw.archunit;

import static org.junit.jupiter.api.Assertions.*;
import static org.koikifw.archunit.ModuleEventLevelSelectionContractTest.*;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.koikifw.archunit.fixture.levelselection.business.alpha.adapter.inbound.event.Listeners;

class LevelSelectedCompositeRegressionTest {
    // C01..C10
    @Test void oldPublicSignatureStillReturnsArchRule() throws NoSuchMethodException {
        assertEquals(ArchRule.class, KoikiArchitectureRules.class.getMethod("businessModuleRules", String.class).getReturnType());
    }
    @Test void enumAndOverloadExposeOnlyTheApprovedSelectionContract() throws NoSuchMethodException {
        assertEquals(List.of("LEVEL_0", "LEVEL_1", "LEVEL_2"), Arrays.stream(ModuleEventLevel.values()).map(Enum::name).toList());
        assertEquals(ArchRule.class, KoikiArchitectureRules.class.getMethod("businessModuleRules", String.class, Map.class).getReturnType());
        assertTrue(Modifier.isPublic(ModuleEventLevel.class.getModifiers()));
        assertFalse(Modifier.isPublic(ModuleEventSelection.class.getModifiers()));
    }
    @Test void legacyRule28DetailsAreUnchanged() {
        var classes = fixtures(Listeners.Standard.class);
        List<String> individual = BusinessModuleRuleSet.rule28(PackageName.of("businessBasePackage", ROOT))
                .evaluate(classes).getFailureReport().getDetails();
        assertEquals(individual, KoikiArchitectureRules.businessModuleRules(ROOT).evaluate(classes).getFailureReport().getDetails());
    }
    @Test void emptyMapRetainsAllExistingNonEventConstraints() {
        String root = "org.koikifw.archunit.fixture.negative.beans.business";
        var classes = new ClassFileImporter().importPackages(root);
        assertEquals(KoikiArchitectureRules.businessModuleRules(root).evaluate(classes).getFailureReport().getDetails(),
                KoikiArchitectureRules.businessModuleRules(root, Map.of()).evaluate(classes).getFailureReport().getDetails());
    }
    @Test void frameworkOwnershipStillRejectsInternalConsumer() {
        String root = "org.koikifw.archunit.fixture.negative.internal";
        assertIncludedViolation(FrameworkOwnershipRuleSet.rule13(PackageName.of("frameworkBasePackage", root + ".framework")),
                KoikiArchitectureRules.frameworkOwnershipRules(root + ".framework", root + ".customer"), "013",
                new ClassFileImporter().importPackages(root));
    }
    @Test void levelTwoDoesNotSuppressMissingTier() {
        rejectsExisting("org.koikifw.archunit.fixture.negative.missingtier.business", "undeclared", "007");
    }
    @Test void levelTwoDoesNotSuppressPersistenceDeclarationChecks() {
        rejectsExisting("org.koikifw.archunit.fixture.negative.missingtier.business", "undeclared", "008");
        String root = "org.koikifw.archunit.fixture.compliant.business";
        KoikiArchitectureRules.businessModuleRules(root, Map.of("rich", ModuleEventLevel.LEVEL_2))
                .check(new ClassFileImporter().importPackages(root));
    }
    @Test void levelTwoDoesNotSuppressPublicModelExposure() {
        rejectsExisting("org.koikifw.archunit.fixture.negative.mvc.business", "rich", "017");
    }
    @Test void levelTwoDoesNotSuppressCrossModuleCoupling() {
        rejectsExisting("org.koikifw.archunit.fixture.negative.beans.business", "alpha", "003");
    }
    @Test void configuredBusinessRootStillMustBeImported() {
        String report = String.join("\n", KoikiArchitectureRules.businessModuleRules("com.absent.business", Map.of())
                .evaluate(new ClassFileImporter().importClasses(String.class)).getFailureReport().getDetails());
        assertTrue(report.contains("businessBasePackage com.absent.business"), report);
    }
    private static void rejectsExisting(String root, String module, String id) {
        PackageName base = PackageName.of("businessBasePackage", root);
        ArchRule individual = switch (id) {
            case "007" -> BusinessModuleRuleSet.rule7(base);
            case "008" -> BusinessModuleRuleSet.rule8(base);
            case "017" -> BusinessModuleRuleSet.rule17(base);
            case "003" -> BusinessModuleRuleSet.rule3(base);
            default -> throw new IllegalArgumentException("Unexpected regression rule " + id);
        };
        assertIncludedViolation(individual, KoikiArchitectureRules.businessModuleRules(root, Map.of(module, ModuleEventLevel.LEVEL_2)),
                id, new ClassFileImporter().importPackages(root));
    }
}
