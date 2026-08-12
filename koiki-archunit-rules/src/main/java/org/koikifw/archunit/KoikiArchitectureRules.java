package org.koikifw.archunit;

import static com.tngtech.archunit.lang.SimpleConditionEvent.violated;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaPackage;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.CompositeArchRule;
import com.tngtech.archunit.lang.ConditionEvents;

import dev.koiki.walkingskeleton.architecture.KoikiModule;

public final class KoikiArchitectureRules {

    private static final String TRANSACTIONAL_EVENT_LISTENER =
            "org.springframework.transaction.event.TransactionalEventListener";

    private KoikiArchitectureRules() {
    }

    public static ArchRule representativeRules(String businessBasePackage) {
        String basePackage = normalize(businessBasePackage);
        return CompositeArchRule.of(moduleTierMustBeDeclared(basePackage))
                .and(domainModelMustNotAppearInInboundSignatures(basePackage))
                .and(internalPackagesMustNotBeReferencedByOtherModules(basePackage))
                .and(transactionalEventListenersAreForbidden(basePackage))
                .and(modulesMustNotCallOtherModulesDirectly(basePackage));
    }

    public static ArchRule moduleTierMustBeDeclared(String businessBasePackage) {
        String basePackage = normalize(businessBasePackage);
        return classes()
                .that().resideInAPackage(basePackage + "..")
                .should(new ModuleDeclarationCondition(basePackage))
                .because("すべての業務モジュールはTierと永続化方式を@KoikiModuleで宣言する"
                        + "（ADR-022 / §11.3、規則7-8）。宣言がないと適用すべきTier別規則を選べない。"
                        + "モジュールrootのpackage-info.javaへ@KoikiModuleを追加すること。");
    }

    public static ArchRule domainModelMustNotAppearInInboundSignatures(String businessBasePackage) {
        String basePackage = normalize(businessBasePackage);
        return classes()
                .that().resideInAPackage(basePackage + "..adapter.inbound..")
                .should(new InboundSignatureCondition(basePackage))
                .allowEmptyShould(true)
                .because("業務モデルをInbound Adapterの入出力型に使わない"
                        + "（ADR-023 / §11.6 規約4、規則17-20）。遅延ロードや業務モデルの外部露出を招く。"
                        + "Form、DTO、またはread modelへ変換すること。");
    }

    public static ArchRule internalPackagesMustNotBeReferencedByOtherModules(
            String businessBasePackage) {
        String basePackage = normalize(businessBasePackage);
        return classes()
                .that().resideInAPackage(basePackage + "..")
                .should(new CrossModuleDependencyCondition(
                        basePackage,
                        KoikiArchitectureRules::isInternalTarget,
                        "internal package"))
                .because("他モジュールのinternal packageを参照しない"
                        + "（ADR-041 / §9.6、規則3・13）。非公開実装への結合はKOIKI更新時の破壊を招く。"
                        + "公開APIまたはdomain.eventを使用すること。");
    }

    public static ArchRule transactionalEventListenersAreForbidden(String businessBasePackage) {
        String basePackage = normalize(businessBasePackage);
        return classes()
                .that().resideInAPackage(basePackage + "..")
                .should(new NoMethodAnnotationCondition(TRANSACTIONAL_EVENT_LISTENER))
                .because("Level 1期間中は@TransactionalEventListenerを使用しない"
                        + "（ADR-005 / §17.5、規則28）。Level 2移行時に意図しない永続化対象となる。"
                        + "同期@EventListenerを使用するかLevel 2移行を判断すること。");
    }

    public static ArchRule modulesMustNotCallOtherModulesDirectly(String businessBasePackage) {
        String basePackage = normalize(businessBasePackage);
        return classes()
                .that().resideInAPackage(basePackage + "..")
                .should(new CrossModuleDependencyCondition(
                        basePackage,
                        KoikiArchitectureRules::isOtherModuleImplementationTarget,
                        "application/domain.model"))
                .because("他モジュールのapplicationまたはdomain.modelを直接参照しない"
                        + "（ADR-025 / §17.3、規則9-10）。直接Bean呼出はモジュール境界を形骸化させる。"
                        + "識別子と値だけを持つdomain.eventを介して連携すること。");
    }

    private static String normalize(String basePackage) {
        String normalized = basePackage.strip();
        while (normalized.endsWith(".")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("businessBasePackage must not be blank");
        }
        return normalized;
    }

    private static Optional<String> moduleName(String basePackage, JavaClass javaClass) {
        String prefix = basePackage + ".";
        String packageName = javaClass.getPackageName();
        if (!packageName.startsWith(prefix)) {
            return Optional.empty();
        }
        String relativeName = packageName.substring(prefix.length());
        int separator = relativeName.indexOf('.');
        return Optional.of(separator < 0 ? relativeName : relativeName.substring(0, separator));
    }

    private static boolean isInternalTarget(String basePackage, String moduleName, JavaClass target) {
        return target.getPackageName().startsWith(basePackage + "." + moduleName + ".internal");
    }

    private static boolean isOtherModuleImplementationTarget(
            String basePackage,
            String moduleName,
            JavaClass target) {
        String moduleRoot = basePackage + "." + moduleName;
        String packageName = target.getPackageName();
        return packageName.startsWith(moduleRoot + ".application")
                || packageName.startsWith(moduleRoot + ".domain.model");
    }

    @FunctionalInterface
    private interface ForbiddenTarget {
        boolean test(String basePackage, String targetModule, JavaClass target);
    }

    private static final class ModuleDeclarationCondition extends ArchCondition<JavaClass> {

        private final String basePackage;
        private final Map<String, JavaClass> representatives = new LinkedHashMap<>();

        private ModuleDeclarationCondition(String basePackage) {
            super("declare @KoikiModule at every module root");
            this.basePackage = basePackage;
        }

        @Override
        public void init(Collection<JavaClass> allClasses) {
            representatives.clear();
            allClasses.stream()
                    .filter(javaClass -> moduleName(basePackage, javaClass).isPresent())
                    .sorted(Comparator.comparing(JavaClass::getName))
                    .forEach(javaClass -> representatives.putIfAbsent(
                            moduleName(basePackage, javaClass).orElseThrow(),
                            javaClass));
        }

        @Override
        public void check(JavaClass javaClass, ConditionEvents events) {
            Optional<String> moduleName = moduleName(basePackage, javaClass);
            if (moduleName.isEmpty() || !javaClass.equals(representatives.get(moduleName.get()))) {
                return;
            }

            String moduleRoot = basePackage + "." + moduleName.get();
            JavaPackage modulePackage = findPackage(javaClass.getPackage(), moduleRoot);
            if (!modulePackage.isAnnotatedWith(KoikiModule.class)) {
                events.add(violated(
                        javaClass,
                        "Module '" + moduleName.get() + "' has no @KoikiModule declaration at "
                                + moduleRoot + ".package-info"));
            }
        }

        private static JavaPackage findPackage(JavaPackage start, String packageName) {
            JavaPackage current = start;
            while (!current.getName().equals(packageName)) {
                current = current.getParent().orElse(start);
                if (current == start) {
                    break;
                }
            }
            return current;
        }
    }

    private static final class InboundSignatureCondition extends ArchCondition<JavaClass> {

        private final String basePackage;

        private InboundSignatureCondition(String basePackage) {
            super("not expose domain.model in inbound method signatures");
            this.basePackage = basePackage;
        }

        @Override
        public void check(JavaClass javaClass, ConditionEvents events) {
            for (JavaMethod method : javaClass.getMethods()) {
                for (JavaClass parameterType : method.getRawParameterTypes()) {
                    addViolationIfDomainModel(method, parameterType, events);
                }
                addViolationIfDomainModel(method, method.getRawReturnType(), events);
            }
        }

        private void addViolationIfDomainModel(
                JavaMethod method,
                JavaClass type,
                ConditionEvents events) {
            if (type.getPackageName().startsWith(basePackage + ".")
                    && type.getPackageName().contains(".domain.model")) {
                events.add(violated(
                        method,
                        method.getDescription() + " exposes domain model " + type.getName()));
            }
        }
    }

    private static final class CrossModuleDependencyCondition extends ArchCondition<JavaClass> {

        private final String basePackage;
        private final ForbiddenTarget forbiddenTarget;

        private CrossModuleDependencyCondition(
                String basePackage,
                ForbiddenTarget forbiddenTarget,
                String targetDescription) {
            super("not depend on another module's " + targetDescription);
            this.basePackage = basePackage;
            this.forbiddenTarget = forbiddenTarget;
        }

        @Override
        public void check(JavaClass javaClass, ConditionEvents events) {
            Optional<String> sourceModule = moduleName(basePackage, javaClass);
            if (sourceModule.isEmpty()) {
                return;
            }
            for (Dependency dependency : javaClass.getDirectDependenciesFromSelf()) {
                JavaClass target = dependency.getTargetClass();
                Optional<String> targetModule = moduleName(basePackage, target);
                if (targetModule.isPresent()
                        && !sourceModule.get().equals(targetModule.get())
                        && forbiddenTarget.test(basePackage, targetModule.get(), target)) {
                    events.add(violated(dependency, dependency.getDescription()));
                }
            }
        }
    }

    private static final class NoMethodAnnotationCondition extends ArchCondition<JavaClass> {

        private final String annotationType;

        private NoMethodAnnotationCondition(String annotationType) {
            super("not declare methods annotated with " + annotationType);
            this.annotationType = annotationType;
        }

        @Override
        public void check(JavaClass javaClass, ConditionEvents events) {
            for (JavaMethod method : javaClass.getMethods()) {
                if (method.tryGetAnnotationOfType(annotationType).isPresent()) {
                    events.add(violated(
                            method,
                            method.getDescription() + " is annotated with " + annotationType));
                }
            }
        }
    }
}
