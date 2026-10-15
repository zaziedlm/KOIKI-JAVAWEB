package org.koikifw.archunit;

import static org.junit.jupiter.api.Assertions.*;
import static org.koikifw.archunit.ModuleEventLevelSelectionContractTest.*;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.koikifw.archunit.fixture.levelselection.business.alpha.adapter.inbound.event.Listeners;
import org.koikifw.archunit.fixture.levelselection.business.alpha.adapter.outbound.Outbound;
import org.koikifw.archunit.fixture.levelselection.business.alpha.application.UseCases;

class Rule29SynchronousSideEffectTest {
    // D01..D08
    @Test void directSyncOutboundDependencyIsRejected() { rejectsSync(Listeners.DirectOutbound.class); }
    @Test void metaSyncOutboundIsRejectedButTransactionalIsNotClassifiedAsSync() {
        rejectsSync(Listeners.MetaOutbound.class);
        BusinessModuleRuleSet.rule29(rootPackage()).check(fixtures(Listeners.RawOutbound.class, Outbound.class));
    }
    @Test void applicationRouteWithoutOutboundDependencyPasses() {
        selected(Map.of()).check(fixtures(Listeners.ApplicationRoute.class, UseCases.Plain.class));
    }
    @Test void indirectRouteIsOutsideRule29Detection() {
        // This is deliberately not a full-composite PASS or proof of side-effect safety.
        var rule = BusinessModuleRuleSet.rule29(PackageName.of("businessBasePackage", ROOT));
        rule.check(fixtures(Listeners.IndirectRoute.class, UseCases.Indirect.class, Outbound.class));
    }
    @Test void ruleOneRemainsAlongsideRule29() {
        var classes = fixtures(Listeners.DirectOutbound.class, Outbound.class);
        assertIncludedViolation(BusinessModuleRuleSet.rule1(rootPackage()), selected(Map.of()), "001", classes);
        assertIncludedViolation(BusinessModuleRuleSet.rule29(rootPackage()), selected(Map.of()), "029", classes);
    }
    @Test void levelTwoCannotBypassDirectOutboundRuleOne() {
        var classes = fixtures(Listeners.StandardOutbound.class, Outbound.class);
        assertIncludedViolation(BusinessModuleRuleSet.rule1(rootPackage()), levelTwo(), "001", classes);
        eventPolicy(Map.of("alpha", ModuleEventLevel.LEVEL_2)).check(classes);
        BusinessModuleRuleSet.rule29(rootPackage()).check(classes);
    }
    @Test void listenerOutsideConfiguredRootDoesNotTriggerRule29() {
        BusinessModuleRuleSet.rule29(PackageName.of("businessBasePackage", "com.other.business"))
                .check(new ClassFileImporter().importClasses(Listeners.DirectOutbound.class, Outbound.class));
    }
    @Test void legacyApiDoesNotAcquireRule29Diagnostics() {
        var legacy = KoikiArchitectureRules.businessModuleRules(ROOT);
        assertIncludedViolation(BusinessModuleRuleSet.rule1(rootPackage()), legacy, "001",
                fixtures(Listeners.DirectOutbound.class, Outbound.class));
        assertFalse(legacy.getDescription().contains("[KOIKI-ARCH-029]"));
    }
    private static void rejectsSync(Class<?> fixture) {
        assertIncludedViolation(BusinessModuleRuleSet.rule29(rootPackage()), selected(Map.of()), "029", fixtures(fixture, Outbound.class));
    }
}
