package org.koikifw.archunit;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaAccess;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaConstructorCall;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaMethodCall;
import com.tngtech.archunit.core.domain.JavaModifier;
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
import java.util.stream.Stream;
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
    private static final String ENTITY_MANAGER = "jakarta.persistence.EntityManager";
    private static final String JPA_REPOSITORY =
            "org.springframework.data.jpa.repository.JpaRepository";
    private static final String REQUEST_MAPPING =
            "org.springframework.web.bind.annotation.RequestMapping";
    private static final String MODEL = "org.springframework.ui.Model";
    private static final String MODEL_AND_VIEW =
            "org.springframework.web.servlet.ModelAndView";

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
                .and(rule14(basePackage))
                .and(rule15(basePackage))
                .and(rule16(basePackage))
                .and(rule17(basePackage))
                .and(rule18(basePackage))
                .and(rule19(basePackage))
                .and(rule20(basePackage))
                .and(rule21(basePackage))
                .and(rule22(basePackage))
                .and(rule24(basePackage))
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
                List.of("ADR-022", "ADR-023", "ADR-039"),
                "永続化方式に対応する規則を選択できない、またはMyBatisで兼用モデルを誤採用する",
                "persistenceとpersistenceModelを明示し、SEPARATED提供前はJPAとSHAREDを使用する");
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

    static ArchRule rule14(PackageName basePackage) {
        RuleMessage message = RuleMessage.of(
                14,
                List.of("ADR-022"),
                "SIMPLE moduleへ未使用のRich Domain構造が入り複雑性が増す",
                "判断をApplicationへ置くか、昇格条件を満たす場合にmodule全体をRICHへ変更する");
        ArchCondition<JavaClass> condition = new ArchCondition<>(
                "not place Rich Domain packages in SIMPLE modules") {
            @Override
            public void check(JavaClass item, ConditionEvents events) {
                if (isTier(item, basePackage, ModuleTier.SIMPLE)
                        && (isInRole(item, basePackage, "domain.model")
                                || isInRole(item, basePackage, "domain.service")
                                || isInRole(item, basePackage, "domain.repository")
                                || isInRole(item, basePackage, "domain.gateway"))) {
                    addViolation(
                            events,
                            item,
                            message,
                            item.getDescription() + " is a Rich Domain type in a SIMPLE module");
                }
            }
        };
        return classes().should(condition).because(message.description()).allowEmptyShould(true);
    }

    static ArchRule rule15(PackageName basePackage) {
        RuleMessage message = RuleMessage.of(
                15,
                List.of("ADR-022", "ADR-023"),
                "DomainがAdapter／Web／永続化操作へ結合し業務規則を独立維持できない",
                "技術処理をAdapterへ移し、許容されたannotation／Repository contractだけを残す");
        return dependencyRule(
                "not depend from RICH domains on adapters, Web, MVC, or EntityManager",
                message,
                source -> isTier(source, basePackage, ModuleTier.RICH)
                        && isInRole(source, basePackage, "domain"),
                target -> isInRole(target, basePackage, "adapter")
                        || target.getPackageName().startsWith("org.springframework.web")
                        || target.getName().equals(ENTITY_MANAGER));
    }

    static ArchRule rule16(PackageName basePackage) {
        RuleMessage message = RuleMessage.of(
                16,
                List.of("ADR-024"),
                "Domain RepositoryがJPA固有操作を公開し永続化境界が漏れる",
                "Spring Data Commons Repository<T, ID>だけを継承する");
        ArchCondition<JavaClass> condition = new ArchCondition<>(
                "extend Spring Data Commons Repository but not JpaRepository") {
            @Override
            public void check(JavaClass item, ConditionEvents events) {
                if (item.getSimpleName().equals("package-info")
                        || !isTier(item, basePackage, ModuleTier.RICH)
                        || !isInRole(item, basePackage, "domain.repository")) {
                    return;
                }
                if (!item.isInterface() || !item.isAssignableTo(SPRING_DATA_REPOSITORY)) {
                    addViolation(
                            events,
                            item,
                            message,
                            item.getDescription() + " does not extend " + SPRING_DATA_REPOSITORY);
                }
                if (item.isAssignableTo(JPA_REPOSITORY)) {
                    addViolation(
                            events,
                            item,
                            message,
                            item.getDescription() + " extends " + JPA_REPOSITORY);
                }
            }
        };
        return classes().should(condition).because(message.description()).allowEmptyShould(true);
    }

    static ArchRule rule17(PackageName basePackage) {
        RuleMessage message = RuleMessage.of(
                17,
                List.of("ADR-023"),
                "Inbound APIへDomain Modelが露出し遅延loadと変更波及を招く",
                "Form、DTOまたはread modelへ変換する");
        return methodSignatureRule(
                "not expose Domain Models in inbound method signatures",
                message,
                basePackage,
                item -> isInRole(item.getOwner(), basePackage, "adapter.inbound"),
                true,
                true);
    }

    static ArchRule rule18(PackageName basePackage) {
        RuleMessage message = RuleMessage.of(
                18,
                List.of("ADR-023"),
                "HTTP bindingがDomain Modelを直接変更し不変条件を迂回する",
                "入力Form／DTOを受け取りUse Caseへ変換する");
        return methodSignatureRule(
                "not bind Domain Models as MVC handler arguments",
                message,
                basePackage,
                item -> isInRole(item.getOwner(), basePackage, "adapter.inbound") && isMvcHandler(item),
                true,
                false);
    }

    static ArchRule rule19(PackageName basePackage) {
        RuleMessage message = RuleMessage.of(
                19,
                List.of("ADR-023", "ADR-028"),
                "view描画時の遅延loadやresponse送信後の失敗を招く",
                "transaction内でDTO／read modelへ変換してからModelへ渡す");
        ArchCondition<JavaClass> condition = new ArchCondition<>(
                "not pass Domain Models directly to MVC model sinks") {
            @Override
            public void check(JavaClass item, ConditionEvents events) {
                if (!isTier(item, basePackage, ModuleTier.RICH)) {
                    return;
                }
                item.getMethods().stream()
                        .filter(BusinessModuleRuleSet::isMvcHandler)
                        .forEach(method -> checkRule19Handler(method, basePackage, message, events));
            }
        };
        return classes().should(condition).because(message.description()).allowEmptyShould(true);
    }

    static ArchRule rule20(PackageName basePackage) {
        RuleMessage message = RuleMessage.of(
                20,
                List.of("ADR-023"),
                "MVC戻り値としてDomain Modelが外部境界へ露出する",
                "DTO、view名またはread modelを返す");
        return methodSignatureRule(
                "not return Domain Models from MVC handlers",
                message,
                basePackage,
                item -> isInRole(item.getOwner(), basePackage, "adapter.inbound") && isMvcHandler(item),
                false,
                true);
    }

    static ArchRule rule21(PackageName basePackage) {
        RuleMessage message = RuleMessage.of(
                21,
                List.of("ADR-023", "ADR-025"),
                "他moduleがDomain内部表現へ結合し独立変更できない",
                "所有module内へ参照を戻し、module間はeventで連携する");
        return dependencyRule(
                "not reference a RICH Domain Model from another module",
                message,
                source -> moduleOf(source, basePackage) != null,
                target -> isTier(target, basePackage, ModuleTier.RICH)
                        && isInRole(target, basePackage, "domain.model"),
                dependency -> isCrossModule(dependency, basePackage));
    }

    static ArchRule rule22(PackageName basePackage) {
        RuleMessage message = RuleMessage.of(
                22,
                List.of("ADR-023"),
                "setterが不変条件を迂回し任意状態変更を許す",
                "意味のある状態遷移methodへ閉じ込める");
        ArchCondition<JavaClass> condition = new ArchCondition<>(
                "not declare public setters on Domain Models") {
            @Override
            public void check(JavaClass item, ConditionEvents events) {
                if (!isTier(item, basePackage, ModuleTier.RICH)
                        || !isInRole(item, basePackage, "domain.model")) {
                    return;
                }
                item.getMethods().stream()
                        .filter(method -> method.getModifiers().contains(JavaModifier.PUBLIC))
                        .filter(method -> method.getName().startsWith("set"))
                        .filter(method -> method.getRawParameterTypes().size() == 1)
                        .forEach(method -> addViolation(
                                events,
                                method,
                                message,
                                method.getDescription() + " is a public setter"));
            }
        };
        return classes().should(condition).because(message.description()).allowEmptyShould(true);
    }

    static boolean isOwnedQueryReadModel(
            Dependency dependency,
            PackageName basePackage) {
        JavaClass source = dependency.getOriginClass();
        JavaClass target = dependency.getTargetClass();
        String sourceModule = moduleOf(source, basePackage);
        String targetModule = moduleOf(target, basePackage);
        if (sourceModule == null || !sourceModule.equals(targetModule)
                || !isInRole(source, basePackage, "application.query")) {
            return false;
        }
        return target.getPackageName().equals(source.getPackageName())
                || target.getPackageName().startsWith(source.getPackageName() + ".");
    }

    static ArchRule rule24(PackageName basePackage) {
        RuleMessage message = RuleMessage.of(
                24,
                List.of("ADR-022"),
                "外部I/O実装がDomainへ混入し技術詳細に結合する",
                "実装をadapter.outbound.externalへ移す");
        ArchCondition<JavaClass> condition = new ArchCondition<>(
                "place Domain Gateway implementations in outbound external adapters") {
            @Override
            public void check(JavaClass item, ConditionEvents events) {
                if (!isTier(item, basePackage, ModuleTier.RICH)
                        || item.isInterface()
                        || item.getModifiers().contains(JavaModifier.ABSTRACT)) {
                    return;
                }
                item.getAllRawInterfaces().stream()
                        .filter(gateway -> sameModule(item, gateway, basePackage))
                        .filter(gateway -> isInRole(gateway, basePackage, "domain.gateway"))
                        .filter(gateway -> !isInRole(
                                item,
                                basePackage,
                                "adapter.outbound.external"))
                        .forEach(gateway -> addViolation(
                                events,
                                item,
                                message,
                                item.getDescription() + " implements " + gateway.getName()
                                        + " outside adapter.outbound.external"));
            }
        };
        return classes().should(condition).because(message.description()).allowEmptyShould(true);
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

    private static ArchRule methodSignatureRule(
            String description,
            RuleMessage message,
            PackageName basePackage,
            Predicate<JavaMethod> methodPredicate,
            boolean checkParameters,
            boolean checkReturnType) {
        ArchCondition<JavaClass> condition = new ArchCondition<>(description) {
            @Override
            public void check(JavaClass item, ConditionEvents events) {
                if (!isTier(item, basePackage, ModuleTier.RICH)) {
                    return;
                }
                item.getMethods().stream()
                        .filter(methodPredicate)
                        .forEach(method -> {
                            if (checkParameters) {
                                method.getRawParameterTypes().stream()
                                        .filter(type -> isInRole(type, basePackage, "domain.model"))
                                        .forEach(type -> addViolation(
                                                events,
                                                method,
                                                message,
                                                method.getDescription() + " exposes parameter "
                                                        + type.getName()));
                            }
                            if (checkReturnType
                                    && isInRole(
                                            method.getRawReturnType(),
                                            basePackage,
                                            "domain.model")) {
                                addViolation(
                                        events,
                                        method,
                                        message,
                                        method.getDescription() + " exposes return type "
                                                + method.getRawReturnType().getName());
                            }
                        });
            }
        };
        return classes().should(condition).because(message.description()).allowEmptyShould(true);
    }

    private static void checkRule19Handler(
            JavaMethod method,
            PackageName basePackage,
            RuleMessage message,
            ConditionEvents events) {
        // ArchUnit exposes calls and line numbers, but not complete argument data flow. Matching
        // source and sink on one line catches the approved direct forms without rejecting a DTO
        // conversion performed on intervening lines.
        List<JavaAccess<?>> sources = Stream.concat(
                        method.getMethodCallsFromSelf().stream()
                                .filter(call -> isInRole(
                                        call.getTarget().getRawReturnType(),
                                        basePackage,
                                        "domain.model"))
                                .map(call -> (JavaAccess<?>) call),
                        method.getConstructorCallsFromSelf().stream()
                                .filter(call -> isInRole(
                                        call.getTargetOwner(),
                                        basePackage,
                                        "domain.model"))
                                .map(call -> (JavaAccess<?>) call))
                .toList();
        List<JavaAccess<?>> sinks = Stream.concat(
                        method.getMethodCallsFromSelf().stream()
                                .filter(BusinessModuleRuleSet::isMvcModelSink)
                                .map(call -> (JavaAccess<?>) call),
                        method.getConstructorCallsFromSelf().stream()
                                .filter(BusinessModuleRuleSet::isModelAndViewSink)
                                .map(call -> (JavaAccess<?>) call))
                .toList();
        for (JavaAccess<?> source : sources) {
            sinks.stream()
                    .filter(sink -> source.getLineNumber() == sink.getLineNumber())
                    .forEach(sink -> addViolation(
                            events,
                            method,
                            message,
                            method.getDescription() + " directly combines "
                                    + source.getDescription() + " with " + sink.getDescription()));
        }
    }

    private static boolean isMvcModelSink(JavaMethodCall call) {
        String owner = call.getTargetOwner().getName();
        return (owner.equals(MODEL) && call.getName().equals("addAttribute"))
                || (owner.equals(MODEL_AND_VIEW) && call.getName().equals("addObject"));
    }

    private static boolean isModelAndViewSink(JavaConstructorCall call) {
        return call.getTargetOwner().getName().equals(MODEL_AND_VIEW)
                && call.getTarget().getRawParameterTypes().stream()
                        .map(JavaClass::getName)
                        .anyMatch(type -> type.equals(Object.class.getName())
                                || type.equals(Map.class.getName()));
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

    private static boolean isMvcHandler(JavaMethod method) {
        return method.isAnnotatedWith(REQUEST_MAPPING)
                || method.isMetaAnnotatedWith(REQUEST_MAPPING);
    }

    private static boolean isCrossModule(Dependency dependency, PackageName basePackage) {
        String sourceModule = moduleOf(dependency.getOriginClass(), basePackage);
        String targetModule = moduleOf(dependency.getTargetClass(), basePackage);
        return sourceModule != null && targetModule != null && !sourceModule.equals(targetModule);
    }

    private static boolean sameModule(
            JavaClass first,
            JavaClass second,
            PackageName basePackage) {
        String firstModule = moduleOf(first, basePackage);
        String secondModule = moduleOf(second, basePackage);
        return firstModule != null && firstModule.equals(secondModule);
    }

    private static boolean isTier(
            JavaClass item,
            PackageName basePackage,
            ModuleTier tier) {
        return metadataOf(item, basePackage)
                .map(ModuleMetadata::tier)
                .filter(tier::equals)
                .isPresent();
    }

    private static java.util.Optional<ModuleMetadata> metadataOf(
            JavaClass item,
            PackageName basePackage) {
        String module = moduleOf(item, basePackage);
        if (module == null) {
            return java.util.Optional.empty();
        }
        return findPackage(item.getPackage(), basePackage.value() + "." + module)
                .flatMap(ModuleMetadata::from);
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
                    BusinessModuleRuleSet.findPackage(
                                    item.getPackage(),
                                    basePackage.value() + "." + module)
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
                } else if (metadata.persistence() == PersistenceTechnology.MYBATIS) {
                    addViolation(
                            events,
                            modulePackage,
                            message,
                            modulePackage.getDescription()
                                    + " declares MYBATIS with "
                                    + metadata.persistenceModel()
                                    + ", but MYBATIS requires PersistenceModel.SEPARATED, which is not yet provided");
                } else if (metadata.persistence() != PersistenceTechnology.JPA
                        || metadata.persistenceModel() != PersistenceModel.SHARED) {
                    addViolation(
                            events,
                            modulePackage,
                            message,
                            modulePackage.getDescription() + " has unsupported persistence metadata");
                }
            });
        }

    }
}
