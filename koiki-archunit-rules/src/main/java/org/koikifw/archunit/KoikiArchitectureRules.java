package org.koikifw.archunit;

import static com.tngtech.archunit.lang.SimpleConditionEvent.violated;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaConstructorCall;
import com.tngtech.archunit.core.domain.JavaField;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaMethodCall;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.domain.JavaPackage;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.CompositeArchRule;
import com.tngtech.archunit.lang.ConditionEvents;

import dev.koiki.walkingskeleton.architecture.KoikiModule;
import dev.koiki.walkingskeleton.architecture.ModuleTier;

public final class KoikiArchitectureRules {

    private static final String TRANSACTIONAL_EVENT_LISTENER =
            "org.springframework.transaction.event.TransactionalEventListener";
    private static final String EVENT_LISTENER = "org.springframework.context.event.EventListener";
    private static final String APPLICATION_MODULE_LISTENER =
            "org.springframework.modulith.events.ApplicationModuleListener";

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

    /** Phase 0 V1 rules: common 1-13, Tier 1 rule 14, Tier 2 rules 15-24, and 38-39. */
    public static ArchRule phaseZeroRules(String businessBasePackage) {
        String base = normalize(businessBasePackage);
        return CompositeArchRule.of(moduleTierMustBeDeclared(base))
                .and(layerAndTierRules(base))
                .and(internalPackagesMustNotBeReferencedByOtherModules(base))
                .and(modulesMustNotCallOtherModulesDirectly(base))
                .and(moduleCyclesAreForbidden(base))
                .and(eventListenerRules(base));
    }

    public static ArchRule layerAndTierRules(String businessBasePackage) {
        String base = normalize(businessBasePackage);
        return classes().that().resideInAPackage(base + "..")
                .should(new LayerAndTierCondition(base))
                .because("各Tierの依存方向・公開型・MVC境界を守る（ADR-022・ADR-023 / §11.3・§13.3、規則1-2・6・11-12・14-24）。"
                        + "違反は層の逆流、業務モデル流出、永続化技術との密結合を招く。"
                        + "Application Use Case、DTO/read model、所定のAdapterを介すこと。");
    }

    public static ArchRule moduleCyclesAreForbidden(String businessBasePackage) {
        String base = normalize(businessBasePackage);
        return slices().matching(base + ".(*)..").should().beFreeOfCycles()
                .because("業務モジュール間を循環依存させない（ADR-025 / §17.3、規則4）。"
                        + "変更影響と初期化順序が不明瞭になる。domain.eventで依存方向を一方向にすること。");
    }

    public static ArchRule eventListenerRules(String businessBasePackage) {
        String base = normalize(businessBasePackage);
        return classes().that().resideInAPackage(base + "..")
                .should(new EventListenerCondition(base))
                .because("イベントリスナーはadapter.inbound.eventに置き、Domain Model/Repositoryへ直結しない"
                        + "（ADR-025 / §17.3、規則38-39）。境界を迂回するとモジュール結合が強まる。"
                        + "Application Use Caseを呼び出すこと。");
    }

    public static ArchRule frameworkMustNotDependOn(
            String frameworkBasePackage, String... consumerBasePackages) {
        String framework = normalize(frameworkBasePackage);
        return classes().that().resideInAPackage(framework + "..")
                .should(new ForbiddenPackageDependencyCondition(consumerBasePackages))
                .because("FrameworkはReference/Customerへ依存しない（ADR-014 / §9、規則5）。"
                        + "所有権が逆転するとFrameworkを独立配布できない。依存をFramework API側へ反転すること。");
    }

    public static ArchRule frameworkInternalMustNotBeReferencedFromOutside(
            String frameworkBasePackage) {
        String framework = normalize(frameworkBasePackage);
        return classes().that().resideOutsideOfPackage(framework + "..")
                .should(new FrameworkInternalDependencyCondition(framework))
                .because("Framework外からorg.koikifw.<module>.internalを参照しない"
                        + "（ADR-041 / §9.6、規則13）。非公開実装への結合は更新時の破壊を招く。"
                        + "公開APIを使用すること。");
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

    private static final class ForbiddenPackageDependencyCondition extends ArchCondition<JavaClass> {
        private final String[] forbiddenPackages;

        private ForbiddenPackageDependencyCondition(String... forbiddenPackages) {
            super("not depend on consumer-owned packages");
            this.forbiddenPackages = forbiddenPackages.clone();
        }

        @Override
        public void check(JavaClass javaClass, ConditionEvents events) {
            for (Dependency dependency : javaClass.getDirectDependenciesFromSelf()) {
                for (String forbidden : forbiddenPackages) {
                    String base = normalize(forbidden);
                    if (dependency.getTargetClass().getPackageName().startsWith(base)) {
                        events.add(violated(dependency, dependency.getDescription()));
                    }
                }
            }
        }
    }

    private static final class FrameworkInternalDependencyCondition extends ArchCondition<JavaClass> {
        private final String frameworkBase;

        private FrameworkInternalDependencyCondition(String frameworkBase) {
            super("not depend on framework internal packages");
            this.frameworkBase = frameworkBase;
        }

        @Override
        public void check(JavaClass javaClass, ConditionEvents events) {
            javaClass.getDirectDependenciesFromSelf().stream()
                    .filter(dependency -> dependency.getTargetClass().getPackageName()
                            .startsWith(frameworkBase + "."))
                    .filter(dependency -> dependency.getTargetClass().getPackageName()
                            .contains(".internal"))
                    .forEach(dependency -> violation(events, dependency,
                            "[Rule 13] external code depends on framework internal package"));
        }
    }

    private static final class LayerAndTierCondition extends ArchCondition<JavaClass> {
        private static final String SPRING_REPOSITORY = "org.springframework.data.repository.Repository";
        private static final String JPA_REPOSITORY = "org.springframework.data.jpa.repository.JpaRepository";
        private final String base;

        private LayerAndTierCondition(String base) {
            super("comply with common and declared Tier architecture rules");
            this.base = base;
        }

        @Override
        public void check(JavaClass type, ConditionEvents events) {
            Optional<String> module = moduleName(base, type);
            if (module.isEmpty()) {
                return;
            }
            String root = base + "." + module.get();
            String pkg = type.getPackageName();

            for (Dependency dependency : type.getDirectDependenciesFromSelf()) {
                String target = dependency.getTargetClass().getPackageName();
                if (pkg.startsWith(root + ".adapter.inbound") && target.startsWith(root + ".adapter.outbound")) {
                    violation(events, dependency, "[Rule 1] adapter.inbound depends on adapter.outbound");
                }
                if (pkg.startsWith(root + ".application") && target.startsWith(root + ".adapter.inbound")) {
                    violation(events, dependency, "[Rule 2] application depends on adapter.inbound");
                }
                if (pkg.startsWith(root + ".domain") && (target.startsWith(root + ".adapter")
                        || target.startsWith("org.springframework.web")
                        || dependency.getTargetClass().getName().equals("jakarta.persistence.EntityManager"))) {
                    violation(events, dependency, "[Rule 15] domain depends on an adapter/Web/EntityManager");
                }
                if (dependency.getTargetClass().getName().equals("org.springframework.web.client.RestTemplate")) {
                    violation(events, dependency, "[Rule 12] RestTemplate is used");
                }
            }

            if (isController(type)) {
                type.getDirectDependenciesFromSelf().stream()
                        .filter(dependency -> dependency.getTargetClass().getPackageName().contains(".domain.repository")
                                || dependency.getTargetClass().getSimpleName().endsWith("Repository"))
                        .forEach(dependency -> violation(events, dependency,
                                "[Rule 6] Controller directly depends on Repository"));
            }
            if (pkg.contains(".domain.event")
                    && !type.getSimpleName().equals("package-info")) {
                if (!type.isRecord()) {
                    violation(events, type, "[Rule 11] Domain Event " + type.getName() + " is not a record");
                }
                for (JavaField field : type.getFields()) {
                    if (isDomainModel(field.getRawType())) {
                        violation(events, field, "[Rule 11] Domain Event contains domain.model field " + field.getFullName());
                    }
                }
            }
            if (pkg.contains(".domain.model")) {
                for (JavaMethod method : type.getMethods()) {
                    if (method.getModifiers().contains(JavaModifier.PUBLIC)
                            && method.getName().startsWith("set")
                            && method.getRawParameterTypes().size() == 1) {
                        violation(events, method, "[Rule 22] domain.model has public setter " + method.getFullName());
                    }
                }
            }

            ModuleTier tier = tierOf(type, root).orElse(null);
            if (tier == ModuleTier.SIMPLE && (pkg.contains(".domain.model")
                    || pkg.contains(".domain.service") || pkg.contains(".domain.repository")
                    || pkg.contains(".domain.gateway"))) {
                violation(events, type, "[Rule 14] Tier 1 module contains rich-domain package " + pkg);
            }
            if (tier == ModuleTier.RICH && pkg.contains(".domain.repository") && type.isInterface()) {
                if (!type.isAssignableTo(SPRING_REPOSITORY) || type.isAssignableTo(JPA_REPOSITORY)) {
                    violation(events, type,
                            "[Rule 16] domain.repository must extend Repository, not JpaRepository: " + type.getName());
                }
            }
            if (!pkg.contains(".adapter.outbound.external") && implementsDomainGateway(type, root)) {
                violation(events, type,
                        "[Rule 24] domain.gateway implementation is outside adapter.outbound.external: " + type.getName());
            }
            if (pkg.contains(".adapter.inbound")) {
                checkInbound(type, events);
            }
        }

        private void checkInbound(JavaClass type, ConditionEvents events) {
            for (JavaMethod method : type.getMethods()) {
                method.getRawParameterTypes().stream().filter(this::isDomainModel)
                        .forEach(model -> violation(events, method,
                                "[Rule 17/18] inbound/MVC argument exposes " + model.getName()));
                if (isDomainModel(method.getRawReturnType())) {
                    violation(events, method,
                            "[Rule 17/20] inbound/MVC return type exposes " + method.getRawReturnType().getName());
                }
                boolean writesModel = method.getMethodCallsFromSelf().stream().anyMatch(this::isModelWrite)
                        || method.getConstructorCallsFromSelf().stream()
                                .anyMatch(call -> call.getTargetOwner().getName().equals("org.springframework.web.servlet.ModelAndView"));
                if (writesModel && producesDomainModel(method)) {
                    violation(events, method,
                            "[Rule 19] MVC Model value can contain domain.model in " + method.getFullName());
                }
            }
        }

        private boolean isModelWrite(JavaMethodCall call) {
            return call.getTargetOwner().getName().equals("org.springframework.ui.Model")
                    && call.getName().equals("addAttribute");
        }

        private boolean producesDomainModel(JavaMethod method) {
            boolean methodResult = method.getMethodCallsFromSelf().stream()
                    .map(JavaMethodCall::getTarget).map(target -> target.resolveMember().orElse(null))
                    .filter(java.util.Objects::nonNull).anyMatch(target -> isDomainModel(target.getRawReturnType()));
            boolean constructor = method.getConstructorCallsFromSelf().stream()
                    .map(JavaConstructorCall::getTargetOwner).anyMatch(this::isDomainModel);
            return methodResult || constructor;
        }

        private boolean isDomainModel(JavaClass type) {
            return type.getPackageName().startsWith(base + ".") && type.getPackageName().contains(".domain.model");
        }

        private static boolean isController(JavaClass type) {
            return type.getSimpleName().endsWith("Controller")
                    || type.tryGetAnnotationOfType("org.springframework.stereotype.Controller").isPresent()
                    || type.tryGetAnnotationOfType("org.springframework.web.bind.annotation.RestController").isPresent();
        }

        private static Optional<ModuleTier> tierOf(JavaClass type, String root) {
            JavaPackage pkg = ModuleDeclarationCondition.findPackage(type.getPackage(), root);
            return pkg.tryGetAnnotationOfType(KoikiModule.class).map(KoikiModule::tier);
        }

        private static boolean implementsDomainGateway(JavaClass type, String root) {
            return !type.isInterface() && type.getAllRawInterfaces().stream()
                    .anyMatch(iface -> iface.getPackageName().startsWith(root + ".domain.gateway"));
        }
    }

    private static final class EventListenerCondition extends ArchCondition<JavaClass> {
        private final String base;

        private EventListenerCondition(String base) {
            super("place event listeners at the inbound event boundary");
            this.base = base;
        }

        @Override
        public void check(JavaClass type, ConditionEvents events) {
            boolean listener = type.getMethods().stream().anyMatch(method ->
                    method.tryGetAnnotationOfType(EVENT_LISTENER).isPresent()
                            || method.tryGetAnnotationOfType(APPLICATION_MODULE_LISTENER).isPresent());
            if (listener && !type.getPackageName().contains(".adapter.inbound.event")) {
                violation(events, type,
                        "[Rule 38] event listener is outside adapter.inbound.event: " + type.getName());
            }
            if (type.getPackageName().contains(".adapter.inbound.event")) {
                type.getDirectDependenciesFromSelf().stream()
                        .filter(dependency -> dependency.getTargetClass().getPackageName().startsWith(base + "."))
                        .filter(dependency -> dependency.getTargetClass().getPackageName().contains(".domain.model")
                                || dependency.getTargetClass().getPackageName().contains(".domain.repository"))
                        .forEach(dependency -> violation(events, dependency,
                                "[Rule 39] event listener directly depends on domain model/repository"));
            }
        }
    }

    private static void violation(ConditionEvents events, Object item, String message) {
        events.add(violated(item, message));
    }
}
