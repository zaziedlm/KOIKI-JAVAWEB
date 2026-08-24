package org.koikifw.buildsupport.featuretemplate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/** Generates one KOIKI business-module skeleton from a Tier-specific template. */
public final class GenerateFeature {

    private static final Pattern MODULE_NAME = Pattern.compile("[a-z][a-z0-9]*");
    private static final Pattern BASE_PACKAGE =
            Pattern.compile("[a-z][a-z0-9]*(\\.[a-z][a-z0-9]*)+");
    private static final Pattern CLASS_NAME = Pattern.compile("[A-Z][A-Za-z0-9]*");
    private static final Pattern MAVEN_COORDINATE = Pattern.compile("[A-Za-z0-9_.-]+");
    private static final Pattern RELATIVE_PATH = Pattern.compile("[A-Za-z0-9_./\\\\-]+");

    private GenerateFeature() {
    }

    public static void main(String[] arguments) throws IOException {
        Map<String, String> options = parse(arguments);
        String tier = require(options, "tier");
        if (!tier.equals("tier1-simple") && !tier.equals("tier2-rich")) {
            fail("--tier must be tier1-simple or tier2-rich");
        }

        String moduleName = validated(options, "module-name", MODULE_NAME);
        String basePackage = validated(options, "base-package", BASE_PACKAGE);
        String className = validated(options, "class-name", CLASS_NAME);
        String artifactId = validated(options, "artifact-id", MAVEN_COORDINATE);
        String parentGroupId = validated(options, "parent-group-id", MAVEN_COORDINATE);
        String parentArtifactId = validated(options, "parent-artifact-id", MAVEN_COORDINATE);
        String parentVersion = validated(options, "parent-version", MAVEN_COORDINATE);
        String parentRelativePath = validated(options, "parent-relative-path", RELATIVE_PATH);
        Path output = Path.of(require(options, "output")).toAbsolutePath().normalize();

        if (Files.exists(output)) {
            fail("output already exists: " + output);
        }

        Path template = Path.of("build-support", "feature-templates", "templates", tier)
                .toAbsolutePath()
                .normalize();
        if (!Files.isDirectory(template)) {
            fail("template directory not found; run from the repository root: " + template);
        }

        String featurePackage = basePackage + "." + moduleName;
        Map<String, String> replacements = new LinkedHashMap<>();
        replacements.put("__PACKAGE_PATH__", featurePackage.replace('.', '/'));
        replacements.put("__BASE_PACKAGE__", basePackage);
        replacements.put("__FEATURE_PACKAGE__", featurePackage);
        replacements.put("__MODULE_NAME__", moduleName);
        replacements.put("__CLASS_NAME__", className);
        replacements.put("__CLASS_VARIABLE__", lowerCamel(className));
        replacements.put("__ARTIFACT_ID__", artifactId);
        replacements.put("__PARENT_GROUP_ID__", parentGroupId);
        replacements.put("__PARENT_ARTIFACT_ID__", parentArtifactId);
        replacements.put("__PARENT_VERSION__", parentVersion);
        replacements.put("__PARENT_RELATIVE_PATH__", parentRelativePath.replace('\\', '/'));

        try (var paths = Files.walk(template)) {
            for (Path source : paths.sorted().toList()) {
                Path relative = template.relativize(source);
                String renderedPath = replace(relative.toString().replace('\\', '/'), replacements);
                if (renderedPath.endsWith(".template")) {
                    renderedPath = renderedPath.substring(0, renderedPath.length() - ".template".length());
                }
                Path destination = output.resolve(renderedPath).normalize();
                if (!destination.startsWith(output)) {
                    fail("template path escapes output: " + renderedPath);
                }
                if (Files.isDirectory(source)) {
                    Files.createDirectories(destination);
                } else {
                    String content = Files.readString(source, StandardCharsets.UTF_8);
                    Files.createDirectories(destination.getParent());
                    Files.writeString(destination, replace(content, replacements), StandardCharsets.UTF_8);
                }
            }
        }

        System.out.println("Generated " + tier + " feature module at " + output);
    }

    private static Map<String, String> parse(String[] arguments) {
        Map<String, String> options = new LinkedHashMap<>();
        for (String argument : arguments) {
            if (!argument.startsWith("--") || !argument.contains("=")) {
                fail("arguments must use --name=value: " + argument);
            }
            int separator = argument.indexOf('=');
            String name = argument.substring(2, separator);
            String value = argument.substring(separator + 1);
            if (options.putIfAbsent(name, value) != null) {
                fail("duplicate option: --" + name);
            }
        }
        return options;
    }

    private static String validated(Map<String, String> options, String name, Pattern pattern) {
        String value = require(options, name);
        if (!pattern.matcher(value).matches()) {
            fail("invalid --" + name + ": " + value);
        }
        return value;
    }

    private static String require(Map<String, String> options, String name) {
        String value = options.get(name);
        if (value == null || value.isBlank()) {
            fail("missing --" + name);
        }
        return value;
    }

    private static String replace(String value, Map<String, String> replacements) {
        String rendered = value;
        for (Map.Entry<String, String> replacement : replacements.entrySet()) {
            rendered = rendered.replace(replacement.getKey(), replacement.getValue());
        }
        return rendered;
    }

    private static String lowerCamel(String className) {
        return Character.toLowerCase(className.charAt(0)) + className.substring(1);
    }

    private static void fail(String message) {
        throw new IllegalArgumentException(message);
    }
}
