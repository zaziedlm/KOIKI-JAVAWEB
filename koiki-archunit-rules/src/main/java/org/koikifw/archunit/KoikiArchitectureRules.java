package org.koikifw.archunit;

import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.CompositeArchRule;
import java.util.List;
import java.util.Map;

/** Entry point for the Phase 1a KOIKI architecture rules. */
public final class KoikiArchitectureRules {

    private KoikiArchitectureRules() {
    }

    /**
     * Returns the composite rules for business modules directly below a parent package.
     *
     * <p>The imported classes supplied to the returned rule must contain at least one class
     * below {@code businessBasePackage}. The value names the parent of the business module
     * roots, not one individual module, and is used without trimming or wildcard correction.
     *
     * @param businessBasePackage fully qualified parent package containing business modules
     * @return a new composite rule for the configured business package tree
     * @throws NullPointerException if {@code businessBasePackage} is {@code null}
     * @throws IllegalArgumentException if it is not a valid Java 21 package name
     */
    public static ArchRule businessModuleRules(String businessBasePackage) {
        PackageName root = PackageName.of("businessBasePackage", businessBasePackage);
        return CompositeArchRule.of(
                        RootPackageRule.containsImportedClass("businessBasePackage", root))
                .and(BusinessModuleRuleSet.rules(root));
    }

    /**
     * Returns business rules with an explicit event level per direct child package segment.
     * Unspecified modules, Level 0 and Level 1 retain transactional-listener rejection.
     * Only Level 2 permits the unmodified standard ApplicationModuleListener. This overload
     * also checks synchronous listeners for direct dependencies on business outbound adapters.
     * Neither level selection nor a passing rule proves runtime or indirect side-effect safety.
     *
     * @param businessBasePackage fully qualified parent of the business modules
     * @param moduleEventLevels module package segment to level; defensively copied, no correction
     * @return rules requiring the root and every explicitly selected module to be imported
     * @throws NullPointerException if the root, map, key or value is null
     * @throws IllegalArgumentException if the root or a module segment is invalid
     */
    public static ArchRule businessModuleRules(
            String businessBasePackage, Map<String, ModuleEventLevel> moduleEventLevels) {
        PackageName root = PackageName.of("businessBasePackage", businessBasePackage);
        ModuleEventSelection selection = ModuleEventSelection.copyOf(root, moduleEventLevels);
        CompositeArchRule rules = CompositeArchRule.of(
                RootPackageRule.containsImportedClass("businessBasePackage", root));
        for (String module : selection.levels().keySet()) {
            rules = rules.and(RootPackageRule.containsImportedClass(
                    "moduleEventLevels[" + module + "]", selection.moduleRoot(module)));
        }
        return rules.and(BusinessModuleRuleSet.rules(root, selection));
    }

    /**
     * Returns rules that protect one Framework root from Consumer ownership violations.
     *
     * <p>The imported classes supplied to the returned rule must contain each configured root.
     * At least one Consumer root is required. Framework and Consumer roots must be mutually
     * different and non-overlapping, are used without correction, and the varargs array is
     * defensively copied.
     *
     * @param frameworkBasePackage fully qualified Framework-owned package root
     * @param consumerBasePackages fully qualified Consumer-owned package roots
     * @return a new composite rule for the configured ownership roots
     * @throws NullPointerException if an argument, the array, or an array element is {@code null}
     * @throws IllegalArgumentException if a package name is invalid, no Consumer is supplied,
     *         or configured roots overlap
     */
    public static ArchRule frameworkOwnershipRules(
            String frameworkBasePackage,
            String... consumerBasePackages) {
        PackageName frameworkRoot = PackageName.of(
                "frameworkBasePackage",
                frameworkBasePackage);
        List<PackageName> consumerRoots = PackageName.copyOf(
                "consumerBasePackages",
                consumerBasePackages);
        if (consumerRoots.isEmpty()) {
            throw new IllegalArgumentException(
                    "consumerBasePackages must contain at least one package root");
        }
        for (int index = 0; index < consumerRoots.size(); index++) {
            PackageName consumerRoot = consumerRoots.get(index);
            PackageName.requireDisjoint(
                    "frameworkBasePackage",
                    frameworkRoot,
                    "consumerBasePackages[" + index + "]",
                    consumerRoot);
            for (int previous = 0; previous < index; previous++) {
                PackageName.requireDisjoint(
                        "consumerBasePackages[" + previous + "]",
                        consumerRoots.get(previous),
                        "consumerBasePackages[" + index + "]",
                        consumerRoot);
            }
        }

        CompositeArchRule rules = CompositeArchRule.of(
                RootPackageRule.containsImportedClass(
                        "frameworkBasePackage",
                        frameworkRoot));
        for (int index = 0; index < consumerRoots.size(); index++) {
            rules = rules.and(RootPackageRule.containsImportedClass(
                    "consumerBasePackages[" + index + "]",
                    consumerRoots.get(index)));
        }
        return rules.and(FrameworkOwnershipRuleSet.rules(frameworkRoot, consumerRoots));
    }
}
