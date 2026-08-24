package org.koikifw.archunit;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import java.util.Collection;

/** Package-private guard against a configured root being omitted from an import. */
final class RootPackageRule {

    private RootPackageRule() {
    }

    static ArchRule containsImportedClass(String parameterName, PackageName rootPackage) {
        ArchCondition<JavaClass> condition = new ArchCondition<>(
                "contain an imported class under configured " + parameterName) {
            private boolean discovered;

            @Override
            public void init(Collection<JavaClass> allObjectsToTest) {
                discovered = false;
            }

            @Override
            public void check(JavaClass item, ConditionEvents events) {
                discovered |= rootPackage.containsPackage(item.getPackageName());
            }

            @Override
            public void finish(ConditionEvents events) {
                if (!discovered) {
                    String detail = parameterName + " " + rootPackage
                            + " must be present in imported classes";
                    events.add(SimpleConditionEvent.violated(rootPackage, detail));
                }
            }
        };
        return classes().should(condition).allowEmptyShould(true);
    }
}
