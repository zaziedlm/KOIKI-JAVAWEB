package org.koikifw.reference.notification.adapter.outbound.configuration;

import java.nio.ByteBuffer;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.Set;
import java.util.UUID;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Reads once; hash and strict JSON validation use the exact same bounded bytes. */
public final class ManagedRecoveryConfigurationLoader {
    public static final int MAX_BYTES = 65536;
    private static final Set<String> ROOT_KEYS = Set.of("schemaVersion", "revision", "environmentId", "validFrom",
            "validUntil", "permitTtl", "grants");
    private static final Set<String> GRANT_KEYS = Set.of("userId", "capability", "publicationId");

    public ManagedRecoverySnapshot load(Path path, String expectedHash, String expectedRevision, String expectedEnvironment) {
        try {
            if (!path.isAbsolute() || path.toString().startsWith("\\\\") || !path.normalize().equals(path) || !path.toRealPath().equals(path)
                    || !Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) throw invalid();
            for (Path cursor = path; cursor != null; cursor = cursor.getParent()) {
                if (Files.isSymbolicLink(cursor)) throw invalid();
            }
            byte[] bytes;
            try (var input = Files.newInputStream(path, LinkOption.NOFOLLOW_LINKS)) {
                bytes = input.readNBytes(MAX_BYTES + 1);
            }
            if (bytes.length > MAX_BYTES) throw invalid();
            String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
            if (!hash.equals(expectedHash)) throw invalid();
            String json = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString();
            var mapper = JsonMapper.builder().enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
                    .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS).build();
            JsonNode root = mapper.readTree(json);
            fields(root, ROOT_KEYS);
            var schema = root.get("schemaVersion");
            if (schema == null || !schema.isIntegralNumber() || !schema.canConvertToInt() || schema.intValue() != 1) throw invalid();
            String revision = text(root, "revision"); String environment = text(root, "environmentId");
            if (!revision.equals(expectedRevision) || !environment.equals(expectedEnvironment)) throw invalid();
            var assignments = root.get("grants");
            if (assignments == null || !assignments.isArray() || assignments.size() > 64) throw invalid();
            var grants = new ArrayList<ManagedRecoverySnapshot.Grant>();
            for (JsonNode grant : assignments) {
                fields(grant, GRANT_KEYS);
                grants.add(new ManagedRecoverySnapshot.Grant(uuid(text(grant, "userId")), text(grant, "capability"),
                        uuid(text(grant, "publicationId"))));
            }
            return new ManagedRecoverySnapshot(revision, hash, environment, instant(text(root, "validFrom")),
                    instant(text(root, "validUntil")), Duration.parse(text(root, "permitTtl")), grants);
        } catch (Exception failure) {
            // Do not expose paths, assignments, JSON fragments or parser cause chains.
            throw invalid();
        }
    }

    private static void fields(JsonNode node, Set<String> expected) {
        if (node == null || !node.isObject() || node.size() != expected.size()) throw invalid();
        for (String name : node.propertyNames()) if (!expected.contains(name)) throw invalid();
    }

    private static String text(JsonNode node, String name) {
        JsonNode value = node.get(name);
        if (value == null || !value.isString()) throw invalid();
        return value.stringValue();
    }

    private static UUID uuid(String value) {
        UUID result = UUID.fromString(value);
        if (!result.toString().equalsIgnoreCase(value)) throw invalid();
        return result;
    }

    private static Instant instant(String value) {
        if (!value.endsWith("Z")) throw invalid();
        return Instant.parse(value);
    }

    private static IllegalArgumentException invalid() { return new IllegalArgumentException("Managed recovery configuration invalid"); }
}
