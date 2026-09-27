package org.koikifw.buildsupport.phase4;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.koikifw.archunit.KoikiArchitectureRules;
import org.koikifw.buildsupport.phase4.rulefixture.business.alpha.adapter.inbound.event.ListenerCases;
import org.koikifw.buildsupport.phase4.rulefixture.business.alpha.adapter.outbound.external.ExternalSender;
import org.koikifw.buildsupport.phase4.rulefixture.business.alpha.application.MisplacedModuleListener;
import org.koikifw.buildsupport.phase4.rulefixture.business.alpha.application.SendUseCase;
import org.koikifw.buildsupport.phase4.rulefixture.business.alpha.application.port.SendPort;

/** Tooling-only candidates. These rules do not modify the distributed ArchUnit artifact. */
class Rule28And29CandidateTest {

    private static final String BUSINESS_ROOT =
            "org.koikifw.buildsupport.phase4.rulefixture.business";
    private static final String INBOUND_EVENT = ".adapter.inbound.event";
    private static final String OUTBOUND = ".adapter.outbound.";
    private static final String EVENT_LISTENER =
            "org.springframework.context.event.EventListener";
    private static final String TRANSACTIONAL_EVENT_LISTENER =
            "org.springframework.transaction.event.TransactionalEventListener";
    private static final String APPLICATION_MODULE_LISTENER =
            "org.springframework.modulith.events.ApplicationModuleListener";

    @Test
    void currentCompositeKeepsLevelZeroAndOneRejectionAndListenerPlacement() {
        assertFalse(currentReport(ListenerCases.Synchronous.class)
                .contains("declares a transactional event listener"));
        assertTrue(currentReport(ListenerCases.Transactional.class)
                .contains("declares a transactional event listener"));
        assertTrue(currentReport(ListenerCases.MetaTransactionalCase.class)
                .contains("declares a transactional event listener"));
        assertTrue(currentReport(ListenerCases.ModuleListener.class)
                .contains("declares a transactional event listener"));
        assertFalse(currentReport(ListenerCases.ModuleListener.class).contains("is declared in"));
        assertTrue(currentReport(MisplacedModuleListener.class).contains("is declared in"));
    }

    @Test
    void levelTwoCandidateAllowsModuleListenerButStillRejectsDirectTransactionalListener() {
        assertFalse(candidateRule28(true).evaluate(importClasses(ListenerCases.ModuleListener.class))
                .hasViolation());
        assertFalse(candidateRule28(false).evaluate(importClasses(ListenerCases.Synchronous.class))
                .hasViolation());
        assertTrue(candidateRule28(false).evaluate(importClasses(ListenerCases.Transactional.class))
                .hasViolation());
        assertTrue(candidateRule28(false).evaluate(importClasses(ListenerCases.MetaTransactionalCase.class))
                .hasViolation());
        assertTrue(candidateRule28(false).evaluate(importClasses(ListenerCases.ModuleListener.class))
                .hasViolation());
        assertTrue(candidateRule28(true).evaluate(importClasses(ListenerCases.Transactional.class))
                .hasViolation());
        assertTrue(candidateRule28(true).evaluate(importClasses(ListenerCases.MetaTransactionalCase.class))
                .hasViolation());
    }

    @Test
    void directExternalDependencyOverlapsRuleOne() {
        var fixture = importClasses(ListenerCases.DirectExternalCall.class, ExternalSender.class);
        assertTrue(currentReport(ListenerCases.DirectExternalCall.class, ExternalSender.class)
                .contains("DirectExternalCall.onEvent(java.lang.Object)> calls method <"
                        + ExternalSender.class.getName() + ".send()"));
        assertTrue(candidateRule29().evaluate(fixture).hasViolation());
    }

    @Test
    void directDependencyRuleCannotSeeUseCasePortToExternalAdapterRoute() {
        var fixture = importClasses(
                ListenerCases.IndirectExternalCall.class,
                SendUseCase.class,
                SendPort.class,
                ExternalSender.class);
        assertFalse(currentReport(
                        ListenerCases.IndirectExternalCall.class,
                        SendUseCase.class,
                        SendPort.class,
                        ExternalSender.class)
                .contains("IndirectExternalCall.onEvent(java.lang.Object)> calls method <"
                        + ExternalSender.class.getName() + ".send()"));
        assertFalse(candidateRule29().evaluate(fixture).hasViolation());
    }

    private static String currentReport(Class<?>... types) {
        return KoikiArchitectureRules.businessModuleRules(BUSINESS_ROOT)
                .evaluate(importClasses(types))
                .getFailureReport()
                .getDetails()
                .toString();
    }

    private static com.tngtech.archunit.core.domain.JavaClasses importClasses(Class<?>... types) {
        return new ClassFileImporter().importClasses(Arrays.asList(types));
    }

    private static ArchRule candidateRule28(boolean levelTwo) {
        return classes().should(new ArchCondition<>("use listeners allowed by the selected level") {
            @Override
            public void check(JavaClass item, ConditionEvents events) {
                for (JavaMethod method : item.getMethods()) {
                    boolean moduleListener = method.isAnnotatedWith(APPLICATION_MODULE_LISTENER);
                    boolean transactional = method.isAnnotatedWith(TRANSACTIONAL_EVENT_LISTENER)
                            || method.isMetaAnnotatedWith(TRANSACTIONAL_EVENT_LISTENER);
                    if (transactional && !(levelTwo && moduleListener)) {
                        events.add(SimpleConditionEvent.violated(method,
                                "candidate Rule 28: transactional listener " + method.getDescription()));
                    }
                }
            }
        }).allowEmptyShould(true);
    }

    private static ArchRule candidateRule29() {
        return classes().should(new ArchCondition<>("keep synchronous listeners off outbound adapters") {
            @Override
            public void check(JavaClass item, ConditionEvents events) {
                if (!item.getPackageName().contains(INBOUND_EVENT)
                        || item.getMethods().stream().noneMatch(method -> method.isAnnotatedWith(EVENT_LISTENER))) {
                    return;
                }
                item.getDirectDependenciesFromSelf().stream()
                        .filter(dependency -> dependency.getTargetClass().getPackageName().contains(OUTBOUND))
                        .forEach(dependency -> events.add(SimpleConditionEvent.violated(
                                dependency,
                                "candidate Rule 29: " + dependency.getDescription())));
            }
        }).allowEmptyShould(true);
    }
}
