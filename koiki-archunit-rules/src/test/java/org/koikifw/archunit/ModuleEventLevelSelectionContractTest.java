package org.koikifw.archunit;

import static org.junit.jupiter.api.Assertions.*;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.koikifw.archunit.fixture.levelselection.business.alpha.adapter.inbound.event.Listeners;
import org.koikifw.archunit.fixture.levelselection.business.beta.adapter.inbound.event.BetaListener;

class ModuleEventLevelSelectionContractTest {
    static final String ROOT = "org.koikifw.archunit.fixture.levelselection.business";

    // S01..S10: one invocation per method; fixture mapping is explicit in each method.
    @Test void emptyMapRetainsTransactionalRejection() {
        assertIncludedViolation(eventPolicy(Map.of()), selected(Map.of()), "028", fixtures(Listeners.Standard.class));
    }
    @Test void legacyApiRetainsStandardListenerRejection() {
        assertTrue(details(KoikiArchitectureRules.businessModuleRules(ROOT), Listeners.Standard.class)
                .contains("declares a transactional event listener"));
    }
    @Test void levelZeroAllowsSyncAndRejectsTransactional() { commonLevel(ModuleEventLevel.LEVEL_0); }
    @Test void levelOneAllowsSyncAndRejectsTransactional() { commonLevel(ModuleEventLevel.LEVEL_1); }
    @Test @SuppressWarnings("NullAway") void nullMapIsRejected() {
        assertThrows(NullPointerException.class, () -> selected(null));
    }
    @Test @SuppressWarnings("NullAway") void nullKeyAndValueAreRejected() {
        Map<String, ModuleEventLevel> map = new HashMap<>();
        map.put(null, ModuleEventLevel.LEVEL_2);
        assertThrows(NullPointerException.class, () -> selected(map));
        map.clear(); map.put("alpha", null);
        assertThrows(NullPointerException.class, () -> selected(map));
    }
    @Test void invalidSegmentsAreRejectedWithoutTrimming() {
        for (String key : List.of("", " ", " alpha", "alpha ", "alpha.event", "*", "class", "a-b")) {
            assertThrows(IllegalArgumentException.class, () -> selected(Map.of(key, ModuleEventLevel.LEVEL_2)));
        }
    }
    @Test void originalMapMutationDoesNotChangeTheRule() {
        Map<String, ModuleEventLevel> map = new HashMap<>();
        map.put("alpha", ModuleEventLevel.LEVEL_2);
        ArchRule rule = selected(map);
        map.put("alpha", ModuleEventLevel.LEVEL_0); map.put("missing", ModuleEventLevel.LEVEL_2);
        rule.check(fixtures(Listeners.Standard.class));
    }
    @Test void selectionOrderDoesNotChangeResults() {
        Map<String, ModuleEventLevel> first = new LinkedHashMap<>();
        first.put("alpha", ModuleEventLevel.LEVEL_1); first.put("beta", ModuleEventLevel.LEVEL_2);
        Map<String, ModuleEventLevel> second = new LinkedHashMap<>();
        second.put("beta", ModuleEventLevel.LEVEL_2); second.put("alpha", ModuleEventLevel.LEVEL_1);
        assertEquals(details(selected(first), Listeners.Standard.class, BetaListener.class),
                details(selected(second), Listeners.Standard.class, BetaListener.class));
    }
    @Test void selectedMissingModuleFailsAtEvaluationForEveryLevel() {
        for (ModuleEventLevel level : ModuleEventLevel.values()) {
            String report = details(selected(Map.of("missing", level)), Listeners.Synchronous.class);
            assertTrue(report.contains("moduleEventLevels[missing]"), report);
            assertTrue(report.contains("must be present"), report);
        }
    }

    private static void commonLevel(ModuleEventLevel level) {
        ArchRule rule = selected(Map.of("alpha", level));
        rule.check(fixtures(Listeners.Synchronous.class));
        assertIncludedViolation(eventPolicy(Map.of("alpha", level)), rule, "028", fixtures(Listeners.Standard.class));
    }
    static ArchRule selected(Map<String, ModuleEventLevel> levels) {
        return KoikiArchitectureRules.businessModuleRules(ROOT, levels);
    }
    static ArchRule levelTwo() { return selected(Map.of("alpha", ModuleEventLevel.LEVEL_2)); }
    static PackageName rootPackage() { return PackageName.of("businessBasePackage", ROOT); }
    static ArchRule eventPolicy(Map<String, ModuleEventLevel> levels) {
        return BusinessModuleRuleSet.rule28(rootPackage(), ModuleEventSelection.copyOf(rootPackage(), levels));
    }
    static void assertIncludedViolation(ArchRule individual, ArchRule composite, String id, JavaClasses classes) {
        var individualResult = individual.evaluate(classes);
        var compositeResult = composite.evaluate(classes);
        assertTrue(individualResult.hasViolation(), individualResult.getFailureReport().toString());
        assertTrue(compositeResult.hasViolation(), compositeResult.getFailureReport().toString());
        assertTrue(compositeResult.getFailureReport().getDetails().containsAll(
                individualResult.getFailureReport().getDetails()), compositeResult.getFailureReport().toString());
        assertTrue(individual.getDescription().contains("[KOIKI-ARCH-" + id + "]"));
    }
    static String details(ArchRule rule, Class<?>... types) {
        return String.join("\n", rule.evaluate(fixtures(types)).getFailureReport().getDetails());
    }
    static JavaClasses fixtures(Class<?>... types) {
        List<Class<?>> imported = new ArrayList<>(Arrays.asList(types));
        // Import only metadata for modules actually represented, so a missing-module guard
        // cannot accidentally pass because an unrelated package-info was added by the helper.
        for (String module : List.of("alpha", "beta")) {
            if (Arrays.stream(types).anyMatch(type -> type.getPackageName().startsWith(ROOT + "." + module + "."))) {
                try { imported.add(Class.forName(ROOT + "." + module + ".package-info")); }
                catch (ClassNotFoundException exception) { throw new AssertionError(exception); }
            }
        }
        return new ClassFileImporter().importClasses(imported);
    }
}
