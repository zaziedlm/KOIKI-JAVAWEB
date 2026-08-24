package org.koikifw.archunit;

import static javax.lang.model.SourceVersion.RELEASE_21;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.lang.model.SourceVersion;
import org.jspecify.annotations.Nullable;

/** Validated Java package name used by the package-private rule implementation. */
final class PackageName {

    private final String value;

    private PackageName(String value) {
        this.value = value;
    }

    static PackageName of(String parameterName, @Nullable String value) {
        String candidate = Objects.requireNonNull(
                value,
                parameterName + " must not be null");
        if (!SourceVersion.isName(candidate, RELEASE_21)) {
            throw new IllegalArgumentException(
                    parameterName + " must be a valid Java package name");
        }
        return new PackageName(candidate);
    }

    static List<PackageName> copyOf(
            String parameterName,
            @Nullable String @Nullable [] values) {
        @Nullable String[] source = Objects.requireNonNull(
                values,
                parameterName + " must not be null");
        List<PackageName> result = new ArrayList<>(source.length);
        for (int index = 0; index < source.length; index++) {
            result.add(of(parameterName + "[" + index + "]", source[index]));
        }
        return List.copyOf(result);
    }

    static void requireDisjoint(
            String firstParameter,
            PackageName first,
            String secondParameter,
            PackageName second) {
        if (first.contains(second) || second.contains(first)) {
            throw new IllegalArgumentException(
                    firstParameter + " and " + secondParameter
                            + " must be different non-overlapping package roots");
        }
    }

    String value() {
        return value;
    }

    boolean containsPackage(String packageName) {
        return value.equals(packageName) || packageName.startsWith(value + ".");
    }

    boolean isInternalPackage(String packageName) {
        if (!containsPackage(packageName) || value.equals(packageName)) {
            return false;
        }
        String relativeName = packageName.substring(value.length() + 1);
        return ("." + relativeName + ".").contains(".internal.");
    }

    private boolean contains(PackageName other) {
        return value.equals(other.value) || other.value.startsWith(value + ".");
    }

    @Override
    public boolean equals(Object other) {
        return this == other
                || (other instanceof PackageName packageName && value.equals(packageName.value));
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public String toString() {
        return value;
    }
}
