package org.koikifw.archunit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaPackage;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.EvaluationResult;
import dev.koiki.walkingskeleton.architecture.KoikiModule;
import org.junit.jupiter.api.Test;
import org.jspecify.annotations.NullMarked;

class KoikiArchitectureRulesTest {

    private static final String VIOLATION_BASE = "org.koikifw.archunit.fixture.violation";
    private static final String COMPLIANT_BASE = "org.koikifw.archunit.fixture.compliant";
    private static final String V1_VIOLATION_BASE =
            "org.koikifw.archunit.fixture.v1.violation";

    private final JavaClasses violationClasses =
            new ClassFileImporter().importPackages(VIOLATION_BASE);
    private final JavaClasses compliantClasses =
            new ClassFileImporter().importPackages(COMPLIANT_BASE);

    @Test
    void detectsMissingTierDeclaration() {
        assertViolation(
                KoikiArchitectureRules.moduleTierMustBeDeclared(VIOLATION_BASE),
                "ADR-022",
                "@KoikiModule");
    }

    @Test
    void detectsDomainModelInInboundSignature() {
        assertViolation(
                KoikiArchitectureRules.domainModelMustNotAppearInInboundSignatures(VIOLATION_BASE),
                "ADR-023",
                "Expense");
    }

    @Test
    void detectsCrossModuleInternalReference() {
        assertViolation(
                KoikiArchitectureRules.internalPackagesMustNotBeReferencedByOtherModules(
                        VIOLATION_BASE),
                "ADR-041",
                "InternalStock");
    }

    @Test
    void detectsTransactionalEventListener() {
        assertViolation(
                KoikiArchitectureRules.transactionalEventListenersAreForbidden(VIOLATION_BASE),
                "ADR-005",
                "TransactionalEventListener");
    }

    @Test
    void detectsDirectCrossModuleApplicationReference() {
        assertViolation(
                KoikiArchitectureRules.modulesMustNotCallOtherModulesDirectly(VIOLATION_BASE),
                "ADR-025",
                "InventoryUseCase");
    }

    @Test
    void representativeRulesDoNotReportCompliantModules() {
        EvaluationResult result = KoikiArchitectureRules
                .representativeRules(COMPLIANT_BASE)
                .evaluate(compliantClasses);

        assertFalse(result.hasViolation(), result.getFailureReport().toString());
    }

    @Test
    void phaseZeroRulesDetectLayerTierMvcAndEventViolations() {
        JavaClasses classes = new ClassFileImporter().importPackages(V1_VIOLATION_BASE);
        EvaluationResult result = KoikiArchitectureRules.phaseZeroRules(V1_VIOLATION_BASE)
                .evaluate(classes);
        String report = result.getFailureReport().toString();

        for (String rule : new String[] {
                "[Rule 1]", "[Rule 2]", "[Rule 6]", "[Rule 11]", "[Rule 12]",
                "[Rule 14]", "[Rule 15]", "[Rule 16]", "[Rule 17/18]",
                "[Rule 17/20]", "[Rule 19]", "[Rule 22]", "[Rule 24]",
                "[Rule 38]", "[Rule 39]"
        }) {
            assertTrue(report.contains(rule), () -> "Missing " + rule + " in:\n" + report);
        }
    }

    @Test
    void detectsModuleCycle() {
        String base = "org.koikifw.archunit.fixture.v1.cycle";
        EvaluationResult result = KoikiArchitectureRules.moduleCyclesAreForbidden(base)
                .evaluate(new ClassFileImporter().importPackages(base));
        String report = result.getFailureReport().toString();

        assertTrue(result.hasViolation(), report);
        assertTrue(report.contains("ADR-025"), report);
        assertTrue(report.contains("Cycle detected"), report);
    }

    @Test
    void detectsFrameworkDependingOnCustomer() {
        String root = "org.koikifw.archunit.fixture.v1.ownership";
        JavaClasses classes = new ClassFileImporter().importPackages(root);
        EvaluationResult result = KoikiArchitectureRules.frameworkMustNotDependOn(
                root + ".framework", root + ".customer").evaluate(classes);
        String report = result.getFailureReport().toString();

        assertTrue(result.hasViolation(), report);
        assertTrue(report.contains("ADR-001"), report);
        assertTrue(report.contains("CustomerType"), report);
    }

    @Test
    void detectsExternalReferenceToFrameworkInternal() {
        String root = "org.koikifw.archunit.fixture.v1.ownership";
        JavaClasses classes = new ClassFileImporter().importPackages(root);
        EvaluationResult result = KoikiArchitectureRules
                .frameworkInternalMustNotBeReferencedFromOutside(root + ".framework")
                .evaluate(classes);
        String report = result.getFailureReport().toString();

        assertTrue(result.hasViolation(), report);
        assertTrue(report.contains("ADR-041"), report);
        assertTrue(report.contains("[Rule 13]"), report);
    }

    @Test
    void explicitDomainEventAndReadModelExceptionsRemainAllowed() {
        EvaluationResult moduleBoundary = KoikiArchitectureRules
                .modulesMustNotCallOtherModulesDirectly(COMPLIANT_BASE)
                .evaluate(compliantClasses);
        EvaluationResult tierRules = KoikiArchitectureRules
                .layerAndTierRules(COMPLIANT_BASE)
                .evaluate(compliantClasses);

        assertFalse(moduleBoundary.hasViolation(), moduleBoundary.getFailureReport().toString());
        assertFalse(tierRules.hasViolation(), tierRules.getFailureReport().toString());
    }

    @Test
    void phaseZeroRulesDoNotReportCompliantModules() {
        EvaluationResult result = KoikiArchitectureRules.phaseZeroRules(COMPLIANT_BASE)
                .evaluate(compliantClasses);

        assertFalse(result.hasViolation(), result.getFailureReport().toString());
    }

    @Test
    void nullMarkedAndKoikiModuleCoexistOnPackageInfo() {
        JavaPackage modulePackage = compliantClasses.stream()
                .filter(javaClass -> javaClass.getPackageName().startsWith(
                        COMPLIANT_BASE + ".sales."))
                .findFirst()
                .orElseThrow()
                .getPackage();
        while (!modulePackage.getName().equals(COMPLIANT_BASE + ".sales")) {
            modulePackage = modulePackage.getParent().orElseThrow();
        }

        assertTrue(modulePackage.isAnnotatedWith(NullMarked.class));
        assertTrue(modulePackage.isAnnotatedWith(KoikiModule.class));
    }

    private void assertViolation(ArchRule rule, String expectedAdr, String expectedDetail) {
        EvaluationResult result = rule.evaluate(violationClasses);
        String report = result.getFailureReport().toString();

        assertTrue(result.hasViolation(), "Expected a violation: " + rule.getDescription());
        assertTrue(report.contains(expectedAdr), report);
        assertTrue(report.contains(expectedDetail), report);
    }
}
