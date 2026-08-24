package org.koikifw.archunit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.Test;

class PackageNameTest {

    @Test
    void acceptsAJava21PackageNameWithoutCorrection() {
        PackageName packageName = PackageName.of(
                "businessBasePackage",
                "com.example.application_21");

        assertEquals("com.example.application_21", packageName.value());
        assertEquals("com.example.application_21", packageName.toString());
    }

    @Test
    void rejectsNullWithTheParameterName() {
        NullPointerException failure = assertThrows(
                NullPointerException.class,
                () -> PackageName.of("businessBasePackage", null));

        assertTrue(Objects.requireNonNull(failure.getMessage()).contains("businessBasePackage"));
    }

    @Test
    void rejectsInvalidPackageNamesWithoutTrimmingOrWildcardCorrection() {
        List<String> invalidNames = List.of(
                "",
                " ",
                " com.example",
                "com.example ",
                ".com.example",
                "com.example.",
                "com..example",
                "com.*",
                "com.class",
                "1com.example");

        for (String invalidName : invalidNames) {
            IllegalArgumentException failure = assertThrows(
                    IllegalArgumentException.class,
                    () -> PackageName.of("businessBasePackage", invalidName),
                    invalidName);
            String message = Objects.requireNonNull(failure.getMessage());
            assertTrue(message.contains("businessBasePackage"));
            assertTrue(message.contains("valid Java package name"));
        }
    }

    @Test
    void rejectsDuplicateAndContainingRootsInEitherDirection() {
        PackageName framework = PackageName.of("frameworkBasePackage", "org.koikifw");
        PackageName duplicate = PackageName.of("consumerBasePackages[0]", "org.koikifw");
        PackageName child = PackageName.of("consumerBasePackages[0]", "org.koikifw.customer");
        PackageName consumer = PackageName.of("consumerBasePackages[0]", "com.example");
        PackageName nestedConsumer = PackageName.of(
                "consumerBasePackages[1]",
                "com.example.customer");

        IllegalArgumentException duplicateFailure = assertThrows(
                IllegalArgumentException.class,
                () -> PackageName.requireDisjoint(
                        "frameworkBasePackage", framework,
                        "consumerBasePackages[0]", duplicate));
        IllegalArgumentException childFailure = assertThrows(
                IllegalArgumentException.class,
                () -> PackageName.requireDisjoint(
                        "frameworkBasePackage", framework,
                        "consumerBasePackages[0]", child));
        IllegalArgumentException parentFailure = assertThrows(
                IllegalArgumentException.class,
                () -> PackageName.requireDisjoint(
                        "consumerBasePackages[0]", child,
                        "frameworkBasePackage", framework));
        IllegalArgumentException consumerFailure = assertThrows(
                IllegalArgumentException.class,
                () -> PackageName.requireDisjoint(
                        "consumerBasePackages[0]", consumer,
                        "consumerBasePackages[1]", nestedConsumer));

        String duplicateMessage = Objects.requireNonNull(duplicateFailure.getMessage());
        assertTrue(duplicateMessage.contains("frameworkBasePackage"));
        assertTrue(duplicateMessage.contains("consumerBasePackages[0]"));
        assertTrue(Objects.requireNonNull(childFailure.getMessage()).contains("non-overlapping"));
        assertTrue(Objects.requireNonNull(parentFailure.getMessage()).contains("non-overlapping"));
        String consumerMessage = Objects.requireNonNull(consumerFailure.getMessage());
        assertTrue(consumerMessage.contains("consumerBasePackages[0]"));
        assertTrue(consumerMessage.contains("consumerBasePackages[1]"));
    }

    @Test
    void copiesVarargsWithoutRetainingMutableCallerState() {
        String[] source = {"com.example.customer", "com.example.reference"};

        List<PackageName> copy = PackageName.copyOf("consumerBasePackages", source);
        source[0] = "com.example.changed";

        assertEquals("com.example.customer", copy.getFirst().value());
        assertThrows(
                UnsupportedOperationException.class,
                () -> copy.add(PackageName.of("other", "com.example.other")));
    }

    @Test
    void rejectsNullVarargsAndElementsWithTheirParameterNames() {
        NullPointerException arrayFailure = assertThrows(
                NullPointerException.class,
                () -> PackageName.copyOf("consumerBasePackages", null));
        NullPointerException elementFailure = assertThrows(
                NullPointerException.class,
                () -> PackageName.copyOf(
                        "consumerBasePackages",
                        new String[] {"com.example.customer", null}));

        assertTrue(Objects.requireNonNull(arrayFailure.getMessage()).contains("consumerBasePackages"));
        assertTrue(Objects.requireNonNull(elementFailure.getMessage()).contains("consumerBasePackages[1]"));
    }
}
