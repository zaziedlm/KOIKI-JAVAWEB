package org.koikifw.archunit;

import static java.lang.reflect.Modifier.isFinal;
import static java.lang.reflect.Modifier.isPrivate;
import static java.lang.reflect.Modifier.isPublic;
import static java.lang.reflect.Modifier.isStatic;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.koikifw.archunit.fixture.compliant.ownership.customer.CustomerConsumer;
import org.koikifw.archunit.fixture.compliant.ownership.framework.api.FrameworkApi;
import org.koikifw.archunit.fixture.compliant.ownership.reference.ReferenceConsumer;

class KoikiArchitectureRulesContractTest {

    @Test
    void exposesOneFinalFacadeWithTwoPublicStaticMethodsAndNoPublicConstructor() {
        assertTrue(isPublic(KoikiArchitectureRules.class.getModifiers()));
        assertTrue(isFinal(KoikiArchitectureRules.class.getModifiers()));
        assertTrue(Arrays.stream(KoikiArchitectureRules.class.getDeclaredConstructors())
                .allMatch(constructor -> isPrivate(constructor.getModifiers())));

        List<Method> publicMethods = Arrays.stream(KoikiArchitectureRules.class.getDeclaredMethods())
                .filter(method -> isPublic(method.getModifiers()))
                .toList();
        assertEquals(2, publicMethods.size());
        assertTrue(publicMethods.stream().allMatch(method -> isStatic(method.getModifiers())));
        assertEquals(
                List.of("businessModuleRules", "frameworkOwnershipRules"),
                publicMethods.stream().map(Method::getName).sorted().toList());
        assertTrue(publicMethods.stream().allMatch(method -> method.getReturnType().equals(ArchRule.class)));

        assertFalse(isPublic(BusinessModuleRuleSet.class.getModifiers()));
        assertFalse(isPublic(FrameworkOwnershipRuleSet.class.getModifiers()));
        assertFalse(isPublic(ModuleMetadata.class.getModifiers()));
        assertFalse(isPublic(PackageName.class.getModifiers()));
        assertFalse(isPublic(RootPackageRule.class.getModifiers()));
        assertFalse(isPublic(RuleMessage.class.getModifiers()));
    }

    @Test
    @SuppressWarnings("NullAway")
    void rejectsInvalidBusinessBasePackagesWithoutCorrection() {
        NullPointerException nullFailure = assertThrows(
                NullPointerException.class,
                () -> KoikiArchitectureRules.businessModuleRules(null));
        assertTrue(Objects.requireNonNull(nullFailure.getMessage()).contains("businessBasePackage"));

        for (String value : List.of("", " ", " com.example", "com.example ", "com.*", "com.class")) {
            IllegalArgumentException failure = assertThrows(
                    IllegalArgumentException.class,
                    () -> KoikiArchitectureRules.businessModuleRules(value));
            assertTrue(Objects.requireNonNull(failure.getMessage()).contains("businessBasePackage"));
        }
    }

    @Test
    @SuppressWarnings("NullAway")
    void rejectsInvalidConsumerRootsAndOverlaps() {
        assertThrows(
                NullPointerException.class,
                () -> KoikiArchitectureRules.frameworkOwnershipRules(null, "com.example"));
        assertThrows(
                NullPointerException.class,
                () -> KoikiArchitectureRules.frameworkOwnershipRules("org.koikifw", (String[]) null));
        assertThrows(
                NullPointerException.class,
                () -> KoikiArchitectureRules.frameworkOwnershipRules("org.koikifw", "com.example", null));

        IllegalArgumentException emptyFailure = assertThrows(
                IllegalArgumentException.class,
                () -> KoikiArchitectureRules.frameworkOwnershipRules("org.koikifw"));
        assertTrue(Objects.requireNonNull(emptyFailure.getMessage()).contains("at least one"));

        assertThrows(
                IllegalArgumentException.class,
                () -> KoikiArchitectureRules.frameworkOwnershipRules(
                        "org.koikifw",
                        "org.koikifw.customer"));
        assertThrows(
                IllegalArgumentException.class,
                () -> KoikiArchitectureRules.frameworkOwnershipRules(
                        "org.koikifw",
                        "com.example",
                        "com.example.customer"));
    }

    @Test
    void failsWhenConfiguredRootsAreMissingFromImportedClasses() {
        var unrelated = new ClassFileImporter().importClasses(String.class);

        String businessReport = KoikiArchitectureRules.businessModuleRules("com.example.business")
                .evaluate(unrelated)
                .getFailureReport()
                .toString();
        String ownershipReport = KoikiArchitectureRules.frameworkOwnershipRules(
                        "org.example.framework",
                        "org.example.reference",
                        "org.example.customer")
                .evaluate(unrelated)
                .getFailureReport()
                .toString();

        assertTrue(businessReport.contains("businessBasePackage com.example.business"));
        assertTrue(ownershipReport.contains("frameworkBasePackage org.example.framework"));
        assertTrue(ownershipReport.contains("consumerBasePackages[0] org.example.reference"));
        assertTrue(ownershipReport.contains("consumerBasePackages[1] org.example.customer"));
    }

    @Test
    void defensivelyCopiesConsumerVarargs() {
        String[] consumers = {
            "org.koikifw.archunit.fixture.compliant.ownership.reference",
            "org.koikifw.archunit.fixture.compliant.ownership.customer"
        };
        ArchRule rules = KoikiArchitectureRules.frameworkOwnershipRules(
                "org.koikifw.archunit.fixture.compliant.ownership.framework",
                consumers);
        consumers[0] = "com.changed";

        var classes = new ClassFileImporter().importClasses(
                FrameworkApi.class,
                ReferenceConsumer.class,
                CustomerConsumer.class);
        String report = rules.evaluate(classes).getFailureReport().toString();

        assertFalse(report.contains("must be present in imported classes"), report);
        assertFalse(report.contains("com.changed"), report);
    }
}
