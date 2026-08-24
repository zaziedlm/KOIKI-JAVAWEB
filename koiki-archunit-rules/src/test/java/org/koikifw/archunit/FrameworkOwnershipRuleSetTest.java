package org.koikifw.archunit;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.EvaluationResult;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.koikifw.archunit.fixture.gate2.ownership.consumer.ConsumerFixtures;
import org.koikifw.archunit.fixture.gate2.ownership.framework.api.FrameworkFixtures;
import org.koikifw.archunit.fixture.gate2.ownership.framework.sample.internal.InternalFixtures;

class FrameworkOwnershipRuleSetTest {

    private static final PackageName FRAMEWORK_BASE = PackageName.of(
            "frameworkBasePackage",
            "org.koikifw.archunit.fixture.gate2.ownership.framework");
    private static final PackageName CONSUMER_BASE = PackageName.of(
            "consumerBasePackages[0]",
            "org.koikifw.archunit.fixture.gate2.ownership.consumer");

    @Test
    void rule5RejectsFrameworkToConsumerDependency() {
        assertViolation(
                FrameworkOwnershipRuleSet.rule5(FRAMEWORK_BASE, List.of(CONSUMER_BASE)),
                "KOIKI-ARCH-005",
                FrameworkFixtures.DependsOnConsumer.class,
                ConsumerFixtures.ConsumerApi.class);
    }

    @Test
    void rule13RejectsExternalReferenceToFrameworkInternalType() {
        assertViolation(
                FrameworkOwnershipRuleSet.rule13(FRAMEWORK_BASE),
                "KOIKI-ARCH-013",
                ConsumerFixtures.DependsOnFrameworkInternal.class,
                InternalFixtures.InternalType.class);
    }

    private static void assertViolation(
            ArchRule rule,
            String ruleId,
            Class<?>... fixtureClasses) {
        JavaClasses classes = new ClassFileImporter().importClasses(Arrays.asList(fixtureClasses));
        EvaluationResult result = rule.evaluate(classes);
        String report = result.getFailureReport().toString();

        assertTrue(result.hasViolation(), report);
        assertTrue(report.contains("[" + ruleId + "]"), report);
        assertTrue(report.contains("[ADR-"), report);
        assertTrue(report.contains("違反内容:"), report);
        assertTrue(report.contains("影響:"), report);
        assertTrue(report.contains("修正:"), report);
    }
}
