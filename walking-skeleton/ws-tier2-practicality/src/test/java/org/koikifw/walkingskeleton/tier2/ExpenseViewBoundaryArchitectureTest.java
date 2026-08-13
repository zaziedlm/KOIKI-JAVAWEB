package org.koikifw.walkingskeleton.tier2;

import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.EvaluationResult;
import org.koikifw.archunit.KoikiArchitectureRules;
import org.koikifw.walkingskeleton.fixture.v5.adapter.inbound.mvc.BadExpenseExposureController;
import org.koikifw.walkingskeleton.fixture.v5.adapter.inbound.mvc.BadExpenseExposureSource;
import org.koikifw.walkingskeleton.tier2.expense.domain.model.ExpenseRequest;
import org.junit.jupiter.api.Test;

class ExpenseViewBoundaryArchitectureTest {

    private static final String BASE_PACKAGE = "org.koikifw.walkingskeleton";

    @Test
    void detectsIntentionalEntityExposureThroughMvcModel() {
        JavaClasses classes = new ClassFileImporter().importClasses(
                BadExpenseExposureController.class,
                BadExpenseExposureSource.class,
                ExpenseRequest.class);

        EvaluationResult result = KoikiArchitectureRules.layerAndTierRules(BASE_PACKAGE)
                .evaluate(classes);
        String report = result.getFailureReport().toString();

        assertThat(result.hasViolation()).isTrue();
        assertThat(report).contains("[Rule 19]", "BadExpenseExposureController");
    }
}
