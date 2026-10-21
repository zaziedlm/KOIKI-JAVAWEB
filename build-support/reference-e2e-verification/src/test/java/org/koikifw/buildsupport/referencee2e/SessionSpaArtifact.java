package org.koikifw.buildsupport.referencee2e;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.nimbusds.jose.util.JSONObjectUtils;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.zip.ZipFile;

/** Tooling-only check binding both packaged journeys to the same built frontend and JAR. */
final class SessionSpaArtifact {
    private SessionSpaArtifact() {}

    static void verify(Path repository, Path jar) throws Exception {
        String expected = System.getProperty("koiki.reference.spa.jar.sha256", "");
        assertTrue(expected.matches("[a-f0-9]{64}"), "Supply the pinned B1 JAR SHA-256");
        assertEquals(expected, hash(Files.readAllBytes(jar)), "B1 JAR changed between journeys");
        try (var zip = new ZipFile(jar.toFile())) {
            var recordEntry = zip.getEntry("META-INF/koiki-reference-spa-build.json");
            assertTrue(recordEntry != null, "B1 profile build record missing");
            Map<String, Object> record;
            try (var input = zip.getInputStream(recordEntry)) {
                record = JSONObjectUtils.parse(new String(input.readAllBytes(), StandardCharsets.UTF_8));
            }
            assertEquals(1, ((Number) record.get("schemaVersion")).intValue());
            assertEquals("b1-session-spa", record.get("profile"));
            Map<String, Object> inputs = JSONObjectUtils.getJSONObject(record, "inputs");
            Path frontend = repository.resolve("koiki-reference-app/frontend");
            Map<String, String> actualInputs = new TreeMap<>();
            try (var files = Files.walk(frontend)) {
                for (Path file : files.filter(Files::isRegularFile).toList()) {
                    String relative = frontend.relativize(file).toString().replace('\\', '/');
                    if (!relative.startsWith("node_modules/")) actualInputs.put(relative, hash(Files.readAllBytes(file)));
                }
            }
            assertEquals(actualInputs, inputs, "Frontend source/lock differs from the built JAR");
            assertTrue(inputs.containsKey("package-lock.json"));
            Map<String, Object> assets = JSONObjectUtils.getJSONObject(record, "assets");
            assertTrue(assets.containsKey("index.html"));
            Set<String> names = new TreeSet<>();
            String prefix = "META-INF/resources/spa/";
            for (var entry : zip.stream().filter(entry -> !entry.isDirectory()).toList()) {
                String name = entry.getName();
                assertFalse(name.contains("node_modules/") || name.endsWith(".map")
                        || name.contains("/.vite/"), "Private frontend output leaked to the JAR");
                if (name.startsWith(prefix)) {
                    String relative = name.substring(prefix.length());
                    names.add(relative);
                    assertTrue(relative.equals("index.html") || relative.matches("assets/[A-Za-z0-9_.-]+\\.(js|css)"));
                    try (var input = zip.getInputStream(entry)) {
                        assertEquals(assets.get(relative), hash(input.readAllBytes()), "Asset mismatch: " + relative);
                    }
                }
            }
            assertEquals(assets.keySet(), names, "Unexpected or missing packaged SPA resource");
            assertTrue(names.stream().anyMatch(name -> name.endsWith(".js")));
            assertTrue(names.stream().anyMatch(name -> name.endsWith(".css")));
            Path evidence = Path.of("target", "b1-session-spa");
            Files.createDirectories(evidence);
            Files.writeString(evidence.resolve("artifact.txt"), "sha256=" + expected + "\nprofile=b1-session-spa\ninputs="
                    + inputs.size() + "\nassets=" + assets.size() + "\n");
            System.out.println("B1_ARTIFACT sha256=" + expected + " inputs=" + inputs.size() + " assets=" + assets.size());
        }
    }

    private static String hash(byte[] bytes) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }
}
