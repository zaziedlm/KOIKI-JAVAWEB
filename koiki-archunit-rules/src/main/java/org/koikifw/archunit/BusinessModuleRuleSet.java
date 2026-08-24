package org.koikifw.archunit;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaPackage;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.CompositeArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import org.jspecify.annotations.Nullable;
import org.koikifw.architecture.ModuleTier;
import org.koikifw.architecture.PersistenceModel;
import org.koikifw.architecture.PersistenceTechnology;

/** Package-private rules applied to a tree of business modules. */
final class BusinessModuleRuleSet {

    private static final String CONTROLLER = "org.springframework.stereotype.Controller";
    private static final String REST_CONTROLLER =
            "org.springframework.web.bind.annotation.RestController";
    private static final String SPRING_DATA_REPOSITORY =
            "org.springframework.data.repository.Repository";
    private static final String REST_TEMPLATE =
            "org.springframework.web.client.RestTemplate";
    private static final String EVENT_LISTENER =
            "org.springframework.context.event.EventListener";
    private static final String TRANSACTIONAL_EVENT_LISTENER =
            "org.springframework.transaction.event.TransactionalEventListener";
    private static final String APPLICATION_MODULE_LISTENER =
            "org.springframework.modulith.events.ApplicationModuleListener";

    private BusinessModuleRuleSet() {
    }

    static ArchRule rules(PackageName basePackage) {
        return CompositeArchRule.of(rule1(basePackage))
                .and(rule2(basePackage))
                .and(rule3(basePackage))
                .and(rule4(basePackage))
                .and(rule6(basePackage))
                .and(rule7(basePackage))
                .and(rule8(basePackage))
                .and(rule9(basePackage))
                .and(rule11(basePackage))
                .and(rule12(basePackage))
                .and(rule28(basePackage))
                .and(rule38(basePackage))
                .and(rule39(basePackage));
    }

    static ArchRule rule1(PackageName basePackage) {
        RuleMessage message = RuleMessage.of(
                1,
                List.of("ADR-022"),
                "Inboundが技術実装へ結合し、依存方向と差替え境界が崩れる",
                "Application Use CaseとPortを介する");
        return dependencyRule(
                "not depend from inbound adapters on outbound adapters",
                message,
                source -> isInRole(source, basePackage, "adapter.inbound"),
                target -> isInRole(target, basePackage, "adapter.outbound"));
    }

    static ArchRule rule2(PackageName basePackage) {
        RuleMessage message = RuleMessage.of(
                2,
                List.of("ADR-022"),
                "Applicationが入力技術へ依存し、Use Caseを別入口から再利用できない",
                "InboundからApplicationを呼ぶ方向へ戻す");
        return dependencyRule(
                "not depend from application on inbound adapters",
                message,
                source -> isInRole(source, basePackage, "application"),
                target -> isInRole(target, basePackage, "adapter.inbound"));
    }

    static ArchRule rule3(PackageName basePackage) {
        RuleMessage message = RuleMessage.of(
                3,
                List.of("ADR-041"),
                "他moduleの非公開実装に結合し、内部変更が波及する",
                "公開されたdomain.eventを利用するか所有moduleへ処理を戻す");
        return dependencyRule(
                "only depend on another module through domain events",
                message,
                source -> moduleOf(source, basePackage) != null,
                target -> moduleOf(target, basePackage) != null,
                dependency -> isCrossModule(dependency, basePackage)
                        && !isAllowedCrossModuleEvent(dependency, basePackage));
    }

    static ArchRule rule4(PackageName basePackage) {
        RuleMessage message = RuleMessage.of(
                4,
                List.of("ADR-004", "ADR-025"),
                "module間の変更方向と初期化順序が循環する",
                "event等で依存を一方向にする");
        return classes()
                .should(new ModuleCycleCondition(basePackage, message))
                .because(message.description())
                .allowEmptyShould(true);
    }

    static ArchRule rule6(PackageName basePackage) {
        RuleMessage message = RuleMessage.of(
                6,
                List.of("ADR-022"),
                "Controllerがtransactionと業務処理順序を迂回する",
                "Application Use Case経由でRepositoryを利用する");
        return dependencyRule(
                "not depend from controllers on repositories",
                message,
                source -> isController(source, basePackage),
                target -> isInRole(target, basePackage, "domain.repository")
                        || target.isAssignableTo(SPRING_DATA_REPOSITORY));
    }

    static ArchRule rule7(PackageName basePackage) {
        RuleMessage message = RuleMessage.of(
                7,
                List.of("ADR-022"),
                "Tier別規則を選択できずmodule ownershipも曖昧になる",
                "module rootのpackage-info.javaへ正しいname／tierの@KoikiModuleを付与する");
        return moduleDeclarationRule(basePackage, message, true);
    }

    static ArchRule rule8(PackageName basePackage) {
        RuleMessage message = RuleMessage.of(
                8,
                List.of("ADR-022", "ADR-023"),
                "永続化方式に対応する規則を選択できない",
                "persistenceとpersistenceModelを明示する");
        return moduleDeclarationRule(basePackage, message, false);
    }

    static ArchRule rule9(PackageName basePackage) {
        RuleMessage message = RuleMessage.of(
                9,
                List.of("ADR-025"),
                "他moduleのUse Case／Domainへ直接結合し境界が形骸化する",
                "値だけを持つdomain.eventで連携する");
        return dependencyRule(
                "not depend on another module's application or domain model",
                message,
                source -> moduleOf(source, basePackage) != null,
                target -> isInRole(target, basePackage, "application")
                        || isInRole(target, basePackage, "domain.model"),
                dependency -> isCrossModule(dependency, basePackage));
    }

    static boolean isAllowedCrossModuleEvent(
            Dependency dependency,
            PackageName basePackage) {
        return isCrossModule(dependency, basePackage)
                && isInRole(dependency.getTargetClass(), basePackage, "domain.event");
    }

    static ArchRule rule11(PackageName basePackage) {
        RuleMessage message = RuleMessage.of(
                11,
                List.of("ADR-025"),
                "eventが可変またはDomain Modelを露出しmodule間を密結合にする",
                "値だけをcomponentに持つrecordへ変更する");
        ArchCondition<JavaClass> condition = new ArchCondition<>("be immutable value events") {
            @Override
            public void check(JavaClass item, ConditionEvents events) {
                if (!isInRole(item, basePackage, "domain.event")) {
                    return;
                }
                if (!item.isRecord()) {
                    addViolation(events, item, message, item.getDescription() + " is not a record");
                }
                item.getFields().stream()
                        .forEach(field -> field.getAllInvolvedRawTypes().stream()
                                .filter(type -> isInRole(type, basePackage, "domain.model"))
                                .forEach(type -> addViolation(
                                        events,
                                        field,
                                        message,
                                        field.getDescription() + " exposes " + type.getName())));
            }
        };
        return classes().should(condition).because(message.description()).allowEmptyShould(true);
    }

    static ArchRule rule12(PackageName basePackage) {
        RuleMessage message = RuleMessage.of(
                12,
                List.of("ADR-033"),
                "新旧HTTP client方式が混在し横断方針が分裂する",
                "承認済みHTTP Service Interface方式へ置き換える");
        return dependencyRule(
                "not depend on RestTemplate",
                message,
                source -> basePackage.containsPackage(source.getPackageName()),
                target -> target.getName().equals(REST_TEMPLATE));
    }

    static ArchRule rule28(PackageName basePackage) {
        RuleMessage message = RuleMessage.of(
                28,
                List.of("ADR-005"),
                "未確定のtransaction phase／非同期運用をPhase 1aへ先行導入する",
                "Level 0／1ではtransactional listenerを使わず同期@EventListenerを使用する");
        ArchCondition<JavaClass> condition = new ArchCondition<>(
                "not declare transactional event listeners") {
            @Override
            public void check(JavaClass item, ConditionEvents events) {
                if (!basePackage.containsPackage(item.getPackageName())) {
                    return;
                }
                item.getMethods().stream()
                        .filter(BusinessModuleRuleSet::isTransactionalEventListener)
                        .forEach(method -> addViolation(
                                events,
                                method,
                                message,
                                method.getDescription() + " declares a transactional event listener"));
            }
        };
        return classes().should(condition).because(message.description()).allowEmptyShould(true);
    }

    static ArchRule rule38(PackageName basePackage) {
        RuleMessage message = RuleMessage.of(
                38,
                List.of("ADR-025"),
                "listener入口が散在しmodule連携の監査と変更が難しくなる",
                "listenerをadapter.inbound.eventへ移す");
        ArchCondition<JavaClass> condition = new ArchCondition<>(
                "locate event listeners in inbound event adapters") {
            @Override
            public void check(JavaClass item, ConditionEvents events) {
                if (!basePackage.containsPackage(item.getPackageName())
                        || isInRole(item, basePackage, "adapter.inbound.event")) {
                    return;
                }
                item.getMethods().stream()
                        .filter(BusinessModuleRuleSet::isDirectEventListener)
                        .forEach(method -> addViolation(
                                events,
                                method,
                                message,
                                method.getDescription() + " is declared in " + item.getPackageName()));
            }
        };
        return classes().should(condition).because(message.description()).allowEmptyShould(true);
    }

    static ArchRule rule39(PackageName basePackage) {
        RuleMessage message = RuleMessage.of(
                39,
                List.of("ADR-025"),
                "listenerがUse Caseとtransaction調整を迂回する",
                "listenerからApplication Use Caseだけを呼び出す");
        return dependencyRule(
                "not depend from inbound event adapters on domain internals",
                message,
                source -> isInRole(source, basePackage, "adapter.inbound.event"),
                target -> isInRole(target, basePackage, "domain.model")
                        || isInRole(target, basePackage, "domain.repository"));
    }

    private static ArchRule moduleDeclarationRule(
            PackageName basePackage,
            RuleMessage message,
            boolean checkNameAndTier) {
        return classes()
                .should(new ModuleDeclarationCondition(basePackage, message, checkNameAndTier))
                .because(message.description())
                .allowEmptyShould(true);
    }

    private static ArchRule dependencyRule(
            String description,
            RuleMessage message,
            Predicate<JavaClass> sourcePredicate,
            Predicate<JavaClass> targetPredicate) {
        return dependencyRule(
                description,
                message,
                sourcePredicate,
                targetPredicate,
                dependency -> true);
    }

    private static ArchRule dependencyRule(
            String description,
            RuleMessage message,
            Predicate<JavaClass> sourcePredicate,
            Predicate<JavaClass> targetPredicate,
            Predicate<Dependency> dependencyPredicate) {
        ArchCondition<JavaClass> condition = new ArchCondition<>(description) {
            @Override
            public void check(JavaClass item, ConditionEvents events) {
                if (!sourcePredicate.test(item)) {
                    return;
                }
                item.getDirectDependenciesFromSelf().stream()
                        .filter(dependency -> targetPredicate.test(dependency.getTargetClass()))
                        .filter(dependencyPredicate)
                        .forEach(dependency -> addViolation(
                                events,
                                dependency,
                                message,
                                dependency.getDescription()));
            }
        };
        return classes().should(condition).because(message.description()).allowEmptyShould(true);
    }

    private static void addViolation(
            ConditionEvents events,
            Object item,
            RuleMessage message,
            String detail) {
        events.add(SimpleConditionEvent.violated(item, message.violation(detail)));
    }

    private static boolean isController(JavaClass item, PackageName basePackage) {
        return item.isAnnotatedWith(CONTROLLER)
                || item.isMetaAnnotatedWith(CONTROLLER)
                || item.isAnnotatedWith(REST_CONTROLLER)
                || (isInRole(item, basePackage, "adapter.inbound")
                        && item.getSimpleName().endsWith("Controller"));
    }

    private static boolean isTransactionalEventListener(JavaMethod method) {
        return method.isAnnotatedWith(TRANSACTIONAL_EVENT_LISTENER)
                || method.isMetaAnnotatedWith(TRANSACTIONAL_EVENT_LISTENER)
                || method.isAnnotatedWith(APPLICATION_MODULE_LISTENER);
    }

    private static boolean isDirectEventListener(JavaMethod method) {
        return method.isAnnotatedWith(EVENT_LISTENER)
                || method.isAnnotatedWith(APPLICATION_MODULE_LISTENER);
    }

    private static boolean isCrossModule(Dependency dependency, PackageName basePackage) {
        String sourceModule = moduleOf(dependency.getOriginClass(), basePackage);
        String targetModule = moduleOf(dependency.getTargetClass(), basePackage);
        return sourceModule != null && targetModule != null && !sourceModule.equals(targetModule);
    }

    private static boolean isInRole(
            JavaClass item,
            PackageName basePackage,
            String role) {
        String module = moduleOf(item, basePackage);
        if (module == null) {
            return false;
        }
        String roleRoot = basePackage.value() + "." + module + "." + role;
        return item.getPackageName().equals(roleRoot)
                || item.getPackageName().startsWith(roleRoot + ".");
    }

    private static @Nullable String moduleOf(JavaClass item, PackageName basePackage) {
        String packageName = item.getPackageName();
        String prefix = basePackage.value() + ".";
        if (!packageName.startsWith(prefix)) {
            return null;
        }
        String relativeName = packageName.substring(prefix.length());
        int separator = relativeName.indexOf('.');
        return separator < 0 ? relativeName : relativeName.substring(0, separator);
    }

    private record ModuleEdge(String source, String target) {
    }

    private static final class ModuleCycleCondition extends ArchCondition<JavaClass> {

        private final PackageName basePackage;
        private final RuleMessage message;
        private Set<ModuleEdge> cyclicEdges = Set.of();

        private ModuleCycleCondition(PackageName basePackage, RuleMessage message) {
            super("be free of module cycles");
            this.basePackage = basePackage;
            this.message = message;
        }

        @Override
        public void init(Collection<JavaClass> allObjectsToTest) {
            Map<String, Set<String>> graph = new HashMap<>();
            for (JavaClass item : allObjectsToTest) {
                for (Dependency dependency : item.getDirectDependenciesFromSelf()) {
                    String source = moduleOf(dependency.getOriginClass(), basePackage);
                    String target = moduleOf(dependency.getTargetClass(), basePackage);
                    if (source != null && target != null && !source.equals(target)) {
                        graph.computeIfAbsent(source, ignored -> new HashSet<>()).add(target);
                    }
                }
            }
            Set<ModuleEdge> result = new HashSet<>();
            graph.forEach((source, targets) -> targets.stream()
                    .filter(target -> hasPath(graph, target, source))
                    .map(target -> new ModuleEdge(source, target))
                    .forEach(result::add));
            cyclicEdges = Set.copyOf(result);
        }

        @Override
        public void check(JavaClass item, ConditionEvents events) {
            item.getDirectDependenciesFromSelf().stream()
                    .filter(dependency -> {
                        String source = moduleOf(dependency.getOriginClass(), basePackage);
                        String target = moduleOf(dependency.getTargetClass(), basePackage);
                        return source != null
                                && target != null
                                && cyclicEdges.contains(new ModuleEdge(source, target));
                    })
                    .forEach(dependency -> addViolation(
                            events,
                            dependency,
                            message,
                            "module cycle contains " + dependency.getDescription()));
        }

        private static boolean hasPath(
                Map<String, Set<String>> graph,
                String start,
                String target) {
            ArrayDeque<String> pending = new ArrayDeque<>();
            Set<String> visited = new HashSet<>();
            pending.add(start);
            while (!pending.isEmpty()) {
                String current = pending.removeFirst();
                if (current.equals(target)) {
                    return true;
                }
                if (visited.add(current)) {
                    pending.addAll(graph.getOrDefault(current, Set.of()));
                }
            }
            return false;
        }
    }

    private static final class ModuleDeclarationCondition extends ArchCondition<JavaClass> {

        private final PackageName basePackage;
        private final RuleMessage message;
        private final boolean checkNameAndTier;
        private Map<String, JavaPackage> modulePackages = Map.of();

        private ModuleDeclarationCondition(
                PackageName basePackage,
                RuleMessage message,
                boolean checkNameAndTier) {
            super("declare module architecture metadata");
            this.basePackage = basePackage;
            this.message = message;
            this.checkNameAndTier = checkNameAndTier;
        }

        @Override
        public void init(Collection<JavaClass> allObjectsToTest) {
            Map<String, JavaPackage> result = new LinkedHashMap<>();
            for (JavaClass item : allObjectsToTest) {
                String module = moduleOf(item, basePackage);
                if (module != null) {
                    findPackage(item.getPackage(), basePackage.value() + "." + module)
                            .ifPresent(modulePackage -> result.putIfAbsent(module, modulePackage));
                }
            }
            modulePackages = Map.copyOf(result);
        }

        @Override
        public void check(JavaClass item, ConditionEvents events) {
            // Module roots are checked once in finish().
        }

        @Override
        public void finish(ConditionEvents events) {
            modulePackages.forEach((module, modulePackage) -> {
                @Nullable ModuleMetadata metadata = ModuleMetadata.from(modulePackage).orElse(null);
                if (metadata == null) {
                    addViolation(
                            events,
                            modulePackage,
                            message,
                            modulePackage.getDescription() + " has no runtime @KoikiModule declaration");
                    return;
                }
                if (checkNameAndTier) {
                    if (!metadata.name().equals(module)) {
                        addViolation(
                                events,
                                modulePackage,
                                message,
                                modulePackage.getDescription() + " declares name " + metadata.name()
                                        + " but module segment is " + module);
                    }
                    if (!Set.of(ModuleTier.SIMPLE, ModuleTier.RICH).contains(metadata.tier())) {
                        addViolation(
                                events,
                                modulePackage,
                                message,
                                modulePackage.getDescription() + " has an unsupported tier");
                    }
                } else if (!Set.of(PersistenceTechnology.JPA, PersistenceTechnology.MYBATIS)
                                .contains(metadata.persistence())
                        || metadata.persistenceModel() != PersistenceModel.SHARED) {
                    addViolation(
                            events,
                            modulePackage,
                            message,
                            modulePackage.getDescription() + " has unsupported persistence metadata");
                }
            });
        }

        private static java.util.Optional<JavaPackage> findPackage(
                JavaPackage start,
                String packageName) {
            JavaPackage current = Objects.requireNonNull(start);
            while (true) {
                if (current.getName().equals(packageName)) {
                    return java.util.Optional.of(current);
                }
                java.util.Optional<JavaPackage> parent = current.getParent();
                if (parent.isEmpty()) {
                    return java.util.Optional.empty();
                }
                current = parent.orElseThrow();
            }
        }
    }
}
