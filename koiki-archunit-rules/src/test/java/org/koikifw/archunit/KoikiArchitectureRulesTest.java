package org.koikifw.archunit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.EvaluationResult;
import dev.koiki.walkingskeleton.architecture.KoikiModule;
import org.junit.jupiter.api.Test;
import org.jspecify.annotations.NullMarked;

class KoikiArchitectureRulesTest {

    private static final String VIOLATION_BASE = "org.koikifw.archunit.fixture.violation";
    private static final String COMPLIANT_BASE = "org.koikifw.archunit.fixture.compliant";

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
    void nullMarkedAndKoikiModuleCoexistOnPackageInfo() {
        var modulePackage = compliantClasses.stream()
                .filter(javaClass -> javaClass.getPackageName().startsWith(
                        COMPLIANT_BASE + ".sales."))
                .findFirst()
                .orElseThrow()
                .getPackage()
                .getParent()
                .orElseThrow();

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
