package org.koikifw.archunit;

import static org.junit.jupiter.api.Assertions.*;
import static org.koikifw.archunit.ModuleEventLevelSelectionContractTest.*;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.koikifw.archunit.fixture.levelselection.business.alpha.adapter.inbound.event.Listeners;
import org.koikifw.archunit.fixture.levelselection.business.alpha.application.UseCases;
import org.koikifw.archunit.fixture.levelselection.business.beta.adapter.inbound.event.BetaListener;

class Rule28LevelSelectionTest {
    // E01..E12
    @Test void synchronousListenerRemainsAllowed() { levelTwo().check(fixtures(Listeners.Synchronous.class)); }
    @Test void unspecifiedModuleRemainsRejected() {
        assertIncludedViolation(eventPolicy(Map.of()), selected(Map.of()), "028", fixtures(Listeners.Standard.class));
    }
    @Test void otherModuleIsNotCoveredBySelection() {
        String report = details(levelTwo(), Listeners.Standard.class, BetaListener.class);
        assertTrue(report.contains("BetaListener"), report);
        assertFalse(report.contains("Standard.handle"), report);
    }
    @Test void directStandardListenerIsAllowedInSelectedModule() { levelTwo().check(fixtures(Listeners.Standard.class)); }
    @Test void rawDirectTransactionalListenerIsRejected() { rejects(levelTwo(), Listeners.RawDirect.class); }
    @Test void rawMetaTransactionalListenerIsRejected() { rejects(levelTwo(), Listeners.RawMeta.class); }
    @Test void wrappedModuleListenerIsRejected() { rejects(levelTwo(), Listeners.WrappedModule.class); }
    @Test void methodTransactionAndAsyncOverridesAreRejected() {
        rejects(levelTwo(), Listeners.MethodTransaction.class); rejects(levelTwo(), Listeners.MethodAsync.class);
        rejects(levelTwo(), Listeners.AnnotationPropagation.class);
    }
    @Test void classTransactionOverrideIsRejected() { rejects(levelTwo(), Listeners.ClassTransaction.class); }
    @Test void methodAndClassMetaOverridesAreRejected() {
        rejects(levelTwo(), Listeners.MethodMetaTransaction.class);
        rejects(levelTwo(), Listeners.ClassMetaTransaction.class); rejects(levelTwo(), Listeners.ClassMetaAsync.class);
    }
    @Test void mixedBeforeCommitAnnotationIsRejected() { rejects(levelTwo(), Listeners.Mixed.class); }
    @Test void standardListenerStillMustBeInInboundEventPackage() {
        var classes = fixtures(UseCases.Misplaced.class);
        assertIncludedViolation(BusinessModuleRuleSet.rule38(rootPackage()), levelTwo(), "038", classes);
        eventPolicy(Map.of("alpha", ModuleEventLevel.LEVEL_2)).check(classes);
    }
    private static void rejects(com.tngtech.archunit.lang.ArchRule rule, Class<?> fixture) {
        String report = details(rule, fixture);
        assertIncludedViolation(eventPolicy(Map.of("alpha", ModuleEventLevel.LEVEL_2)), rule, "028", fixtures(fixture));
        assertTrue(report.contains(fixture.getSimpleName()), report);
    }
}
