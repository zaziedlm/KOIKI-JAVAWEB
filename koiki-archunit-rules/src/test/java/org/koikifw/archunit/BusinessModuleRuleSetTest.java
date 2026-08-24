package org.koikifw.archunit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.EvaluationResult;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.koikifw.archunit.fixture.gate2.business.alpha.adapter.inbound.event.ListenerFixtures;
import org.koikifw.archunit.fixture.gate2.business.alpha.adapter.inbound.web.InboundFixtures;
import org.koikifw.archunit.fixture.gate2.business.alpha.adapter.outbound.persistence.OutboundFixtures;
import org.koikifw.archunit.fixture.gate2.business.alpha.application.ApplicationFixtures;
import org.koikifw.archunit.fixture.gate2.business.alpha.application.MisplacedListener;
import org.koikifw.archunit.fixture.gate2.business.alpha.domain.repository.RepositoryFixtures;
import org.koikifw.archunit.fixture.gate2.business.beta.application.BetaApplicationFixtures;
import org.koikifw.archunit.fixture.gate2.business.beta.domain.event.EventFixtures;
import org.koikifw.archunit.fixture.gate2.business.beta.domain.model.DomainFixtures;

class BusinessModuleRuleSetTest {

    private static final PackageName BUSINESS_BASE = PackageName.of(
            "businessBasePackage",
            "org.koikifw.archunit.fixture.gate2.business");
    private static final PackageName METADATA_BASE = PackageName.of(
            "businessBasePackage",
            "org.koikifw.archunit.fixture.metadata");

    @Test
    void rule1RejectsInboundToOutboundDependency() {
        assertViolation(
                BusinessModuleRuleSet.rule1(BUSINESS_BASE),
                "KOIKI-ARCH-001",
                InboundFixtures.DependsOnOutbound.class,
                OutboundFixtures.OutboundAdapter.class);
    }

    @Test
    void rule2RejectsApplicationToInboundDependency() {
        assertViolation(
                BusinessModuleRuleSet.rule2(BUSINESS_BASE),
                "KOIKI-ARCH-002",
                ApplicationFixtures.DependsOnInbound.class,
                InboundFixtures.DependsOnOutbound.class);
    }

    @Test
    void rule3RejectsNonEventCrossModuleDependency() {
        assertViolation(
                BusinessModuleRuleSet.rule3(BUSINESS_BASE),
                "KOIKI-ARCH-003",
                ApplicationFixtures.DependsOnBetaApplication.class,
                BetaApplicationFixtures.BetaUseCase.class);
    }

    @Test
    void rule4RejectsModuleCycle() {
        assertViolation(
                BusinessModuleRuleSet.rule4(BUSINESS_BASE),
                "KOIKI-ARCH-004",
                ApplicationFixtures.DependsOnBetaApplication.class,
                ApplicationFixtures.AlphaUseCase.class,
                BetaApplicationFixtures.DependsOnAlphaApplication.class,
                BetaApplicationFixtures.BetaUseCase.class);
    }

    @Test
    void rule6RejectsControllerToRepositoryDependency() {
        String report = assertViolation(
                BusinessModuleRuleSet.rule6(BUSINESS_BASE),
                "KOIKI-ARCH-006",
                InboundFixtures.RepositoryCallingController.class,
                InboundFixtures.AnnotatedEndpoint.class,
                RepositoryFixtures.AlphaRepository.class);

        assertTrue(report.contains("RepositoryCallingController"));
        assertTrue(report.contains("AnnotatedEndpoint"));
    }

    @Test
    void rule7RejectsMissingModuleDeclaration() {
        JavaClasses classes = new ClassFileImporter().importPackages(
                "org.koikifw.archunit.fixture.metadata.undeclared");

        assertViolation(
                BusinessModuleRuleSet.rule7(METADATA_BASE),
                "KOIKI-ARCH-007",
                classes);
    }

    @Test
    void rule7AcceptsApprovedTierDeclarations() {
        JavaClasses classes = new ClassFileImporter().importPackages(
                "org.koikifw.archunit.fixture.metadata.simple",
                "org.koikifw.archunit.fixture.metadata.rich");

        assertNoViolation(BusinessModuleRuleSet.rule7(METADATA_BASE), classes);
    }

    @Test
    void rule7RejectsAModuleNameThatDiffersFromItsPackageSegment() {
        JavaClasses classes = new ClassFileImporter().importPackages(
                "org.koikifw.archunit.fixture.gate2.metadata.mismatch");

        String report = assertViolation(
                BusinessModuleRuleSet.rule7(PackageName.of(
                        "businessBasePackage",
                        "org.koikifw.archunit.fixture.gate2.metadata")),
                "KOIKI-ARCH-007",
                classes);

        assertTrue(report.contains("declared-name"));
        assertTrue(report.contains("mismatch"));
    }

    @Test
    void rule8RejectsMissingPersistenceDeclaration() {
        JavaClasses classes = new ClassFileImporter().importPackages(
                "org.koikifw.archunit.fixture.metadata.undeclared");

        assertViolation(
                BusinessModuleRuleSet.rule8(METADATA_BASE),
                "KOIKI-ARCH-008",
                classes);
    }

    @Test
    void rule8AcceptsApprovedPersistenceDeclarations() {
        JavaClasses classes = new ClassFileImporter().importPackages(
                "org.koikifw.archunit.fixture.metadata.simple",
                "org.koikifw.archunit.fixture.metadata.rich");

        assertNoViolation(BusinessModuleRuleSet.rule8(METADATA_BASE), classes);
    }

    @Test
    void rule9RejectsAnotherModuleApplicationDependency() {
        assertViolation(
                BusinessModuleRuleSet.rule9(BUSINESS_BASE),
                "KOIKI-ARCH-009",
                ApplicationFixtures.DependsOnBetaApplication.class,
                BetaApplicationFixtures.BetaUseCase.class);
    }

    @Test
    void rule10AllowsOnlyCrossModuleDomainEvents() {
        JavaClasses classes = importClasses(
                ApplicationFixtures.DependsOnBetaEvent.class,
                EventFixtures.AllowedEvent.class);

        assertNoViolation(BusinessModuleRuleSet.rule3(BUSINESS_BASE), classes);
        assertTrue(classes.get(ApplicationFixtures.DependsOnBetaEvent.class)
                .getDirectDependenciesFromSelf().stream()
                .filter(dependency -> dependency.getTargetClass()
                        .isEquivalentTo(EventFixtures.AllowedEvent.class))
                .anyMatch(dependency -> BusinessModuleRuleSet.isAllowedCrossModuleEvent(
                        dependency,
                        BUSINESS_BASE)));
    }

    @Test
    void rule11RejectsMutableEventsAndDomainModelComponents() {
        String report = assertViolation(
                BusinessModuleRuleSet.rule11(BUSINESS_BASE),
                "KOIKI-ARCH-011",
                EventFixtures.MutableEvent.class,
                EventFixtures.LeakyEvent.class,
                EventFixtures.GenericLeakyEvent.class,
                DomainFixtures.BetaModel.class);

        assertTrue(report.contains("MutableEvent"));
        assertTrue(report.contains("BetaModel"));
        assertTrue(report.contains("GenericLeakyEvent"));
    }

    @Test
    void rule12RejectsRestTemplateDependency() {
        assertViolation(
                BusinessModuleRuleSet.rule12(BUSINESS_BASE),
                "KOIKI-ARCH-012",
                ApplicationFixtures.UsesRestTemplate.class);
    }

    @Test
    void rule28RejectsDirectMetaAndApplicationModuleListeners() {
        String report = assertViolation(
                BusinessModuleRuleSet.rule28(BUSINESS_BASE),
                "KOIKI-ARCH-028",
                ListenerFixtures.TransactionalListeners.class,
                ListenerFixtures.MetaTransactionalListener.class);

        assertTrue(report.contains("directlyTransactional"));
        assertTrue(report.contains("metaTransactional"));
        assertTrue(report.contains("applicationModuleListener"));
    }

    @Test
    void rule38RejectsMisplacedDirectListeners() {
        String report = assertViolation(
                BusinessModuleRuleSet.rule38(BUSINESS_BASE),
                "KOIKI-ARCH-038",
                MisplacedListener.class);

        assertTrue(report.contains("directListener"));
        assertTrue(report.contains("applicationModuleListener"));
    }

    @Test
    void rule39RejectsListenerToDomainDependency() {
        assertViolation(
                BusinessModuleRuleSet.rule39(BUSINESS_BASE),
                "KOIKI-ARCH-039",
                ListenerFixtures.DomainDependingListener.class,
                RepositoryFixtures.AlphaRepository.class);
    }

    private static String assertViolation(
            ArchRule rule,
            String ruleId,
            Class<?>... fixtureClasses) {
        return assertViolation(rule, ruleId, importClasses(fixtureClasses));
    }

    private static String assertViolation(
            ArchRule rule,
            String ruleId,
            JavaClasses classes) {
        EvaluationResult result = rule.evaluate(classes);
        String report = result.getFailureReport().toString();

        assertTrue(result.hasViolation(), report);
        assertTrue(report.contains("[" + ruleId + "]"), report);
        assertTrue(report.contains("[ADR-"), report);
        assertTrue(report.contains("違反内容:"), report);
        assertTrue(report.contains("影響:"), report);
        assertTrue(report.contains("修正:"), report);
        return report;
    }

    private static void assertNoViolation(ArchRule rule, JavaClasses classes) {
        EvaluationResult result = rule.evaluate(classes);
        assertFalse(result.hasViolation(), result.getFailureReport().toString());
    }

    private static JavaClasses importClasses(Class<?>... fixtureClasses) {
        return new ClassFileImporter().importClasses(Arrays.asList(fixtureClasses));
    }
}
