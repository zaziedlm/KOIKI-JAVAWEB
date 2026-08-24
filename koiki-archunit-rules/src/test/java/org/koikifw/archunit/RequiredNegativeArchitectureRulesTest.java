package org.koikifw.archunit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.EvaluationResult;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.junit.jupiter.api.Test;

class RequiredNegativeArchitectureRulesTest {

    @Test
    void missingTierDeclarationFailsOnlyRules7And8() {
        assertIsolatedBusinessViolation(
                "org.koikifw.archunit.fixture.negative.missingtier.business",
                Set.of("007", "008"));
    }

    @Test
    void controllerDomainModelExposureFailsOnlyRules17Through20() {
        assertIsolatedBusinessViolation(
                "org.koikifw.archunit.fixture.negative.mvc.business",
                Set.of("017", "018", "019", "020"));
    }

    @Test
    void externalInternalReferenceFailsOnlyRule13() {
        String root = "org.koikifw.archunit.fixture.negative.internal";
        JavaClasses classes = new ClassFileImporter().importPackages(root);
        ArchRule publicRule = KoikiArchitectureRules.frameworkOwnershipRules(
                root + ".framework",
                root + ".customer");
        PackageName frameworkRoot = PackageName.of(
                "frameworkBasePackage",
                root + ".framework");
        List<PackageName> consumers = List.of(PackageName.of(
                "consumerBasePackages[0]",
                root + ".customer"));
        Map<String, ArchRule> individualRules = Map.of(
                "005", FrameworkOwnershipRuleSet.rule5(frameworkRoot, consumers),
                "013", FrameworkOwnershipRuleSet.rule13(frameworkRoot));

        assertIsolatedViolation(publicRule, classes, individualRules, Set.of("013"));
    }

    @Test
    void transactionalListenerInTheApprovedPackageFailsOnlyRule28() {
        assertIsolatedBusinessViolation(
                "org.koikifw.archunit.fixture.negative.transactional.business",
                Set.of("028"));
    }

    @Test
    void directCrossModuleBeanReferenceFailsOnlyRules3And9() {
        assertIsolatedBusinessViolation(
                "org.koikifw.archunit.fixture.negative.beans.business",
                Set.of("003", "009"));
    }

    private static void assertIsolatedBusinessViolation(
            String basePackage,
            Set<String> expectedRuleIds) {
        JavaClasses classes = new ClassFileImporter().importPackages(basePackage);
        PackageName root = PackageName.of("businessBasePackage", basePackage);
        assertIsolatedViolation(
                KoikiArchitectureRules.businessModuleRules(basePackage),
                classes,
                businessRules(root),
                expectedRuleIds);
    }

    private static void assertIsolatedViolation(
            ArchRule publicRule,
            JavaClasses classes,
            Map<String, ArchRule> individualRules,
            Set<String> expectedRuleIds) {
        EvaluationResult publicResult = publicRule.evaluate(classes);
        assertTrue(publicResult.hasViolation(), publicResult.getFailureReport().toString());

        Set<String> actualRuleIds = individualRules.entrySet().stream()
                .filter(entry -> entry.getValue().evaluate(classes).hasViolation())
                .map(Map.Entry::getKey)
                .collect(java.util.stream.Collectors.toSet());
        assertEquals(expectedRuleIds, actualRuleIds);

        for (String ruleId : expectedRuleIds) {
            String report = Objects.requireNonNull(individualRules.get(ruleId))
                    .evaluate(classes)
                    .getFailureReport()
                    .toString();
            assertTrue(report.contains("[KOIKI-ARCH-" + ruleId + "]"), report);
            assertTrue(report.contains("[ADR-"), report);
            assertTrue(report.contains("違反内容:"), report);
            assertTrue(report.contains("影響:"), report);
            assertTrue(report.contains("修正:"), report);
        }
    }

    private static Map<String, ArchRule> businessRules(PackageName root) {
        Map<String, ArchRule> rules = new LinkedHashMap<>();
        rules.put("001", BusinessModuleRuleSet.rule1(root));
        rules.put("002", BusinessModuleRuleSet.rule2(root));
        rules.put("003", BusinessModuleRuleSet.rule3(root));
        rules.put("004", BusinessModuleRuleSet.rule4(root));
        rules.put("006", BusinessModuleRuleSet.rule6(root));
        rules.put("007", BusinessModuleRuleSet.rule7(root));
        rules.put("008", BusinessModuleRuleSet.rule8(root));
        rules.put("009", BusinessModuleRuleSet.rule9(root));
        rules.put("011", BusinessModuleRuleSet.rule11(root));
        rules.put("012", BusinessModuleRuleSet.rule12(root));
        rules.put("014", BusinessModuleRuleSet.rule14(root));
        rules.put("015", BusinessModuleRuleSet.rule15(root));
        rules.put("016", BusinessModuleRuleSet.rule16(root));
        rules.put("017", BusinessModuleRuleSet.rule17(root));
        rules.put("018", BusinessModuleRuleSet.rule18(root));
        rules.put("019", BusinessModuleRuleSet.rule19(root));
        rules.put("020", BusinessModuleRuleSet.rule20(root));
        rules.put("021", BusinessModuleRuleSet.rule21(root));
        rules.put("022", BusinessModuleRuleSet.rule22(root));
        rules.put("024", BusinessModuleRuleSet.rule24(root));
        rules.put("028", BusinessModuleRuleSet.rule28(root));
        rules.put("038", BusinessModuleRuleSet.rule38(root));
        rules.put("039", BusinessModuleRuleSet.rule39(root));
        return Map.copyOf(rules);
    }
}
