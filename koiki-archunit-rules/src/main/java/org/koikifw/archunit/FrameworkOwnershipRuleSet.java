package org.koikifw.archunit;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.CompositeArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import java.util.List;
import java.util.function.Predicate;

/** Package-private rules that protect Framework and Consumer ownership. */
final class FrameworkOwnershipRuleSet {

    private FrameworkOwnershipRuleSet() {
    }

    static ArchRule rules(
            PackageName frameworkBasePackage,
            List<PackageName> consumerBasePackages) {
        return CompositeArchRule.of(rule5(frameworkBasePackage, consumerBasePackages))
                .and(rule13(frameworkBasePackage));
    }

    static ArchRule rule5(
            PackageName frameworkBasePackage,
            List<PackageName> consumerBasePackages) {
        RuleMessage message = RuleMessage.of(
                5,
                List.of("ADR-014"),
                "Frameworkの独立配布とCustomer差替えができなくなる",
                "Consumer依存を除去し、必要な契約をFramework側へ反転する");
        return dependencyRule(
                "not depend from Framework on Consumers",
                message,
                source -> frameworkBasePackage.containsPackage(source.getPackageName()),
                target -> consumerBasePackages.stream()
                        .anyMatch(root -> root.containsPackage(target.getPackageName())));
    }

    static ArchRule rule13(PackageName frameworkBasePackage) {
        RuleMessage message = RuleMessage.of(
                13,
                List.of("ADR-041"),
                "Framework内部実装への依存がPublic API互換性を迂回する",
                "公開APIだけを利用する");
        return dependencyRule(
                "not depend from outside Framework on internal Framework types",
                message,
                source -> !frameworkBasePackage.containsPackage(source.getPackageName()),
                target -> frameworkBasePackage.isInternalPackage(target.getPackageName()));
    }

    private static ArchRule dependencyRule(
            String description,
            RuleMessage message,
            Predicate<JavaClass> sourcePredicate,
            Predicate<JavaClass> targetPredicate) {
        ArchCondition<JavaClass> condition = new ArchCondition<>(description) {
            @Override
            public void check(JavaClass item, ConditionEvents events) {
                if (!sourcePredicate.test(item)) {
                    return;
                }
                item.getDirectDependenciesFromSelf().stream()
                        .filter(dependency -> targetPredicate.test(dependency.getTargetClass()))
                        .forEach(dependency -> addViolation(events, dependency, message));
            }
        };
        return classes().should(condition).because(message.description()).allowEmptyShould(true);
    }

    private static void addViolation(
            ConditionEvents events,
            Dependency dependency,
            RuleMessage message) {
        events.add(SimpleConditionEvent.violated(
                dependency,
                message.violation(dependency.getDescription())));
    }
}
