package org.koikifw.archunit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.EvaluationResult;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.koikifw.archunit.fixture.gate3.business.other.application.OtherUseCase;
import org.koikifw.archunit.fixture.gate3.business.rich.adapter.inbound.mvc.InboundApiFixtures;
import org.koikifw.archunit.fixture.gate3.business.rich.adapter.inbound.mvc.ViewControllerFixtures;
import org.koikifw.archunit.fixture.gate3.business.rich.adapter.outbound.external.ValidGateway;
import org.koikifw.archunit.fixture.gate3.business.rich.adapter.outbound.persistence.PersistenceAdapter;
import org.koikifw.archunit.fixture.gate3.business.rich.application.MisplacedGateway;
import org.koikifw.archunit.fixture.gate3.business.rich.application.RichUseCase;
import org.koikifw.archunit.fixture.gate3.business.rich.application.query.QueryFixtures;
import org.koikifw.archunit.fixture.gate3.business.rich.domain.gateway.ExternalGateway;
import org.koikifw.archunit.fixture.gate3.business.rich.domain.model.RichModel;
import org.koikifw.archunit.fixture.gate3.business.rich.domain.repository.RepositoryFixtures;
import org.koikifw.archunit.fixture.gate3.business.rich.domain.service.DomainDependencyFixtures;
import org.koikifw.archunit.fixture.gate3.business.simple.domain.event.SimpleEvent;
import org.koikifw.archunit.fixture.gate3.business.simple.domain.model.SimpleDomainModel;

class TierAndMvcRuleSetTest {

    private static final PackageName BUSINESS_BASE = PackageName.of(
            "businessBasePackage",
            "org.koikifw.archunit.fixture.gate3.business");

    @Test
    void rule14RejectsRichDomainPackagesInSimpleModulesButAllowsEvents() {
        assertViolation(
                BusinessModuleRuleSet.rule14(BUSINESS_BASE),
                "KOIKI-ARCH-014",
                importClasses(SimpleDomainModel.class));

        assertNoViolation(
                BusinessModuleRuleSet.rule14(BUSINESS_BASE),
                importClasses(SimpleEvent.class));
    }

    @Test
    void rule15RejectsAdapterWebAndEntityManagerDependencies() {
        String report = assertViolation(
                BusinessModuleRuleSet.rule15(BUSINESS_BASE),
                "KOIKI-ARCH-015",
                importClasses(DomainDependencyFixtures.class, PersistenceAdapter.class));

        assertTrue(report.contains(PersistenceAdapter.class.getName()), report);
        assertTrue(report.contains("org.springframework.web.context.request.WebRequest"), report);
        assertTrue(report.contains("jakarta.persistence.EntityManager"), report);
    }

    @Test
    void rule15AllowsJpaAnnotationsAndSpringDataCommonsContracts() {
        assertNoViolation(
                BusinessModuleRuleSet.rule15(BUSINESS_BASE),
                importClasses(RichModel.class, RepositoryFixtures.ValidRepository.class));
    }

    @Test
    void rule16RejectsPlainAndJpaSpecificRepositories() {
        String report = assertViolation(
                BusinessModuleRuleSet.rule16(BUSINESS_BASE),
                "KOIKI-ARCH-016",
                importClasses(
                        RepositoryFixtures.PlainRepository.class,
                        RepositoryFixtures.JpaSpecificRepository.class,
                        RichModel.class));

        assertTrue(report.contains("PlainRepository"), report);
        assertTrue(report.contains("JpaSpecificRepository"), report);
    }

    @Test
    void rule16AllowsSpringDataCommonsRepository() {
        assertNoViolation(
                BusinessModuleRuleSet.rule16(BUSINESS_BASE),
                importClasses(RepositoryFixtures.ValidRepository.class, RichModel.class));
    }

    @Test
    void rule16IgnoresNullMarkedPackageMetadata() {
        PackageName compliantBase = PackageName.of(
                "businessBasePackage",
                "org.koikifw.archunit.fixture.compliant.business");

        assertNoViolation(
                BusinessModuleRuleSet.rule16(compliantBase),
                new ClassFileImporter().importPackages(compliantBase.value() + ".rich"));
    }

    @Test
    void rule17RejectsDomainModelsInInboundSignatures() {
        String report = assertViolation(
                BusinessModuleRuleSet.rule17(BUSINESS_BASE),
                "KOIKI-ARCH-017",
                importClasses(InboundApiFixtures.class, RichModel.class));

        assertTrue(report.contains("exposes parameter"), report);
        assertTrue(report.contains("exposes return type"), report);
    }

    @Test
    void rule18RejectsDomainModelsInMappedHandlerArguments() {
        assertViolation(
                BusinessModuleRuleSet.rule18(BUSINESS_BASE),
                "KOIKI-ARCH-018",
                importClasses(InboundApiFixtures.class, RichModel.class));
    }

    @Test
    void rule19RejectsDirectModelAndModelAndViewHandoffs() {
        String report = assertViolation(
                BusinessModuleRuleSet.rule19(BUSINESS_BASE),
                "KOIKI-ARCH-019",
                importClasses(
                        ViewControllerFixtures.class,
                        ViewControllerFixtures.RichView.class,
                        RichUseCase.class,
                        RichModel.class));

        assertTrue(report.contains("directModel"), report);
        assertTrue(report.contains("directConstructor"), report);
        assertTrue(report.contains("directModelAndView"), report);
        assertTrue(report.contains("directAddObject"), report);
    }

    @Test
    void rule19AllowsDtoConversionAndItsDocumentedHelperBoundary() {
        JavaClasses classes = importClasses(
                ViewControllerFixtures.class,
                ViewControllerFixtures.RichView.class,
                RichUseCase.class,
                RichModel.class);
        EvaluationResult result = BusinessModuleRuleSet.rule19(BUSINESS_BASE).evaluate(classes);
        String report = result.getFailureReport().toString();

        assertFalse(report.contains("converted()"), report);
        assertFalse(report.contains("helper()"), report);
        assertFalse(report.contains("field()"), report);
        assertFalse(report.contains("reflection("), report);
    }

    @Test
    void rule20RejectsDomainModelsAsMappedHandlerReturns() {
        assertViolation(
                BusinessModuleRuleSet.rule20(BUSINESS_BASE),
                "KOIKI-ARCH-020",
                importClasses(InboundApiFixtures.class, RichModel.class));
    }

    @Test
    void rule21RejectsCrossModuleRichDomainModelReferences() {
        assertViolation(
                BusinessModuleRuleSet.rule21(BUSINESS_BASE),
                "KOIKI-ARCH-021",
                importClasses(OtherUseCase.class, RichModel.class));
    }

    @Test
    void rule22RejectsPublicSingleArgumentSetters() {
        assertViolation(
                BusinessModuleRuleSet.rule22(BUSINESS_BASE),
                "KOIKI-ARCH-022",
                importClasses(RichModel.class));
    }

    @Test
    void rule23RecognizesOnlyOwnedApplicationQueryReadModels() {
        JavaClasses classes = importClasses(
                QueryFixtures.class,
                QueryFixtures.OwnedReadModel.class,
                OtherUseCase.class,
                RichModel.class);

        assertTrue(classes.get(QueryFixtures.class).getDirectDependenciesFromSelf().stream()
                .filter(dependency -> dependency.getTargetClass()
                        .isEquivalentTo(QueryFixtures.OwnedReadModel.class))
                .anyMatch(dependency -> BusinessModuleRuleSet.isOwnedQueryReadModel(
                        dependency,
                        BUSINESS_BASE)));
        assertFalse(classes.get(OtherUseCase.class).getDirectDependenciesFromSelf().stream()
                .filter(dependency -> dependency.getTargetClass().isEquivalentTo(RichModel.class))
                .anyMatch(dependency -> BusinessModuleRuleSet.isOwnedQueryReadModel(
                        dependency,
                        BUSINESS_BASE)));
    }

    @Test
    void rule24RejectsMisplacedGatewayImplementationsAndAllowsExternalAdapters() {
        JavaClasses classes = importClasses(
                MisplacedGateway.class,
                ValidGateway.class,
                ExternalGateway.class);
        String report = assertViolation(
                BusinessModuleRuleSet.rule24(BUSINESS_BASE),
                "KOIKI-ARCH-024",
                classes);

        assertTrue(report.contains("MisplacedGateway"), report);
        assertFalse(report.contains("ValidGateway implements"), report);
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
        List<Class<?>> classes = new ArrayList<>(Arrays.asList(fixtureClasses));
        classes.add(packageInfo("org.koikifw.archunit.fixture.gate3.business.simple.package-info"));
        classes.add(packageInfo("org.koikifw.archunit.fixture.gate3.business.rich.package-info"));
        classes.add(packageInfo("org.koikifw.archunit.fixture.gate3.business.other.package-info"));
        return new ClassFileImporter().importClasses(classes);
    }

    private static Class<?> packageInfo(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException exception) {
            throw new AssertionError("Missing fixture package metadata: " + className, exception);
        }
    }
}
