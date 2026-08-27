package org.koikifw.buildsupport.internal.runtime;

public final class RuntimeCompatibilityProbe {

    private static final String SUCCESS_MARKER = "KOIKI_RUNTIME_COMPATIBILITY_SUCCESS";

    private RuntimeCompatibilityProbe() {
    }

    public static void main(String[] arguments) {
        if (arguments.length != 1) {
            System.err.println("Expected exactly one Java feature version argument.");
            System.exit(2);
        }

        final int expectedFeature;
        try {
            expectedFeature = Integer.parseInt(arguments[0]);
        } catch (NumberFormatException exception) {
            System.err.println("Expected Java feature version must be an integer.");
            System.exit(2);
            return;
        }

        int actualFeature = Runtime.version().feature();
        if (actualFeature != expectedFeature) {
            System.err.printf(
                    "Java runtime feature mismatch: expected=%d actual=%d%n",
                    expectedFeature,
                    actualFeature);
            System.exit(3);
        }

        System.out.printf(
                "%s expected=%d actual=%d vendor=%s version=%s%n",
                SUCCESS_MARKER,
                expectedFeature,
                actualFeature,
                System.getProperty("java.vendor", "unknown"),
                System.getProperty("java.version", "unknown"));
    }
}
