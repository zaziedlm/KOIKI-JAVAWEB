package org.koikifw.walkingskeleton.tier2;

import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.EvaluationResult;
import org.koikifw.archunit.KoikiArchitectureRules;
import org.koikifw.walkingskeleton.tier2.masterdata.adapter.outbound.persistence.Category;
import org.koikifw.walkingskeleton.tier2.masterdata.application.DeactivateCategoryUseCase;
import org.koikifw.walkingskeleton.tier2.masterdata.domain.event.CategoryDeactivating;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModule;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.core.NamedInterface;

class ApplicationModuleVerificationTest {

    private static final String BUSINESS_BASE_PACKAGE =
            "org.koikifw.walkingskeleton.tier2";

    @Test
    void productionClassesSatisfyKoikiPhaseZeroRules() {
        JavaClasses productionClasses = new ClassFileImporter()
                .withImportOption(new ImportOption.DoNotIncludeTests())
                .importPackages(BUSINESS_BASE_PACKAGE);

        EvaluationResult result = KoikiArchitectureRules
                .phaseZeroRules(BUSINESS_BASE_PACKAGE)
                .evaluate(productionClasses);

        assertThat(result.hasViolation())
                .withFailMessage(result.getFailureReport().toString())
                .isFalse();
    }

    @Test
    void springModulithAcceptsApplicationModuleStructure() {
        applicationModules().verify();
    }

    @Test
    void masterdataExposesOnlyCategoryEventThroughNamedInterface() {
        ApplicationModule masterdata = applicationModules()
                .getModuleByName("masterdata")
                .orElseThrow();
        NamedInterface events = masterdata.getNamedInterfaces()
                .getByName("events")
                .orElseThrow();

        assertThat(events.contains(CategoryDeactivating.class)).isTrue();
        assertThat(masterdata.isExposed(CategoryDeactivating.class)).isTrue();
        assertThat(masterdata.isExposed(DeactivateCategoryUseCase.class)).isFalse();
        assertThat(masterdata.isExposed(Category.class)).isFalse();
    }

    private static ApplicationModules applicationModules() {
        return ApplicationModules.of(Tier2PracticalityApplication.class);
    }
}
