package org.koikifw.archunit;

import com.tngtech.archunit.core.domain.JavaClass;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;
import javax.lang.model.SourceVersion;

/** Validated, immutable selection; keys are direct child package segments. */
final class ModuleEventSelection {
    private final PackageName root;
    private final SortedMap<String, ModuleEventLevel> levels;

    private ModuleEventSelection(PackageName root, SortedMap<String, ModuleEventLevel> levels) {
        this.root = root;
        this.levels = Collections.unmodifiableSortedMap(levels);
    }

    static ModuleEventSelection copyOf(PackageName root, Map<String, ModuleEventLevel> levels) {
        Objects.requireNonNull(levels, "moduleEventLevels");
        SortedMap<String, ModuleEventLevel> copy = new TreeMap<>();
        levels.forEach((key, value) -> {
            Objects.requireNonNull(key, "moduleEventLevels key");
            Objects.requireNonNull(value, "moduleEventLevels value");
            if (key.contains(".") || !SourceVersion.isName(key, SourceVersion.RELEASE_21)) {
                throw new IllegalArgumentException("moduleEventLevels key must be a Java 21 package segment: " + key);
            }
            copy.put(key, value);
        });
        return new ModuleEventSelection(root, copy);
    }

    boolean allowsLevel2(JavaClass item) {
        return levels.entrySet().stream().anyMatch(entry -> entry.getValue() == ModuleEventLevel.LEVEL_2
                && moduleRoot(entry.getKey()).containsPackage(item.getPackageName()));
    }

    Map<String, ModuleEventLevel> levels() {
        return levels;
    }

    PackageName moduleRoot(String module) {
        return PackageName.of("moduleEventLevels module", root.value() + "." + module);
    }
}
