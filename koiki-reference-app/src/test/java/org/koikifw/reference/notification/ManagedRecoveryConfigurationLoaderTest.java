package org.koikifw.reference.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.koikifw.referenceacceptance.notification.ManagedRecoveryTestSupport.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.AclEntry;
import java.nio.file.attribute.AclEntryType;
import java.nio.file.attribute.AclEntryPermission;
import java.nio.file.attribute.AclFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.koikifw.reference.notification.adapter.outbound.configuration.ManagedRecoveryConfigurationLoader;

class ManagedRecoveryConfigurationLoaderTest {
    @TempDir Path directory;
    private void reject(String text) throws Exception {
        var path = file(directory, text);
        assertThatThrownBy(() -> load(path)).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Managed recovery configuration invalid").hasNoCause();
    }
    @Test void L01LoadsImmutableSnapshot() throws Exception {
        var path = file(directory, json(grant("READ"))); var snapshot = load(path);
        assertThat(snapshot.grants()).hasSize(1);
        assertThatThrownBy(() -> snapshot.grants().clear()).isInstanceOf(UnsupportedOperationException.class);
        Files.writeString(path, "invalid after startup");
        assertThat(snapshot.revision()).isEqualTo("test-r1");
    }
    @Test void L02RejectsUnknownSchema() throws Exception { reject(json("").replace("\"schemaVersion\":1", "\"schemaVersion\":2")); }
    @Test void L03RejectsUnknownFieldsAndTrailingContent() throws Exception {
        reject(json("").replace("\"schemaVersion\":1", "\"schemaVersion\":1,\"unknown\":true"));
        reject(json("") + " {}");
        reject(json(grant("READ").replace("\"userId\":", "\"unknown\":1,\"userId\":")));
    }
    @Test void L04RejectsDuplicateKeys() throws Exception {
        reject(json("").replace("\"schemaVersion\":1", "\"schemaVersion\":1,\"schemaVersion\":1"));
        reject(json(grant("READ").replace("\"userId\":", "\"capability\":\"NOTIFICATION:PERMIT:READ\",\"userId\":")));
    }
    @Test void L05RejectsNonCanonicalUuid() throws Exception { reject(json(grant("READ").replace(USER.toString(), "1-1-1-1-1"))); }
    @Test void L06RejectsUnknownCapability() throws Exception { reject(json(grant("ALL"))); }
    @Test void L07RejectsRepeatedTuple() throws Exception { reject(json(grant("READ") + "," + grant("READ"))); }
    @Test void L08RejectsInvalidPeriods() throws Exception {
        reject(json("").replace(FROM.plusSeconds(1800).toString(), FROM.toString()));
        reject(json("").replace(FROM.plusSeconds(1800).toString(), FROM.plusSeconds(1801).toString()));
        reject(json("").replace("PT5M", "PT0S")); reject(json("").replace("PT5M", "PT11M"));
    }
    @Test void L09RejectsOversizedOrMalformedBytes() throws Exception {
        reject(" ".repeat(65537));
        var path = directory.resolve("malformed.json"); Files.write(path, new byte[]{(byte) 0xc3, (byte) 0x28});
        assertThatThrownBy(() -> load(path)).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void L10RejectsTooManyGrants() throws Exception {
        reject(json(IntStream.range(0, 65).mapToObj(i -> grant("READ").replace(PUBLICATION.toString(),
                new java.util.UUID(0, i).toString())).collect(Collectors.joining(","))));
    }
    @Test void L11RejectsMissingNonFileAndIndirectPaths() throws Exception {
        var loader = new ManagedRecoveryConfigurationLoader();
        for (Path path : new Path[]{directory.resolve("missing"), directory, Path.of("relative.json"), directory.resolve("child/../assignment.json"), Path.of("\\\\invalid.example.test\\share\\assignment.json")}) {
            assertThatThrownBy(() -> loader.load(path, "a".repeat(64), "test-r1", ENV)).isInstanceOf(IllegalArgumentException.class);
        }
        var path = file(directory, json(""));
        var view = Files.getFileAttributeView(path, AclFileAttributeView.class);
        var digest = hash(path);
        if (view != null) {
            var original = view.getAcl(); var restricted = new ArrayList<>(original);
            restricted.addFirst(AclEntry.newBuilder().setType(AclEntryType.DENY).setPrincipal(Files.getOwner(path))
                    .setPermissions(AclEntryPermission.READ_DATA).build());
            try {
                view.setAcl(restricted);
                assertThatThrownBy(() -> loader.load(path, digest, "test-r1", ENV)).isInstanceOf(IllegalArgumentException.class).hasNoCause();
            } finally { view.setAcl(original); }
        } else {
            var original = Files.getPosixFilePermissions(path);
            try {
                Files.setPosixFilePermissions(path, Set.<PosixFilePermission>of());
                assertThatThrownBy(() -> loader.load(path, digest, "test-r1", ENV)).isInstanceOf(IllegalArgumentException.class).hasNoCause();
            } finally { Files.setPosixFilePermissions(path, original); }
        }
        // A junction on Windows, a symlink on POSIX; both stay inside the disposable test directory.
        var real = Files.createDirectory(directory.resolve("real")); var realFile = file(real, json(""));
        var junction = directory.resolve("junction");
        if (System.getProperty("os.name").startsWith("Windows")) {
            String command = "$ErrorActionPreference='Stop'; New-Item -ItemType Junction -Path '"
                    + junction.toString().replace("'", "''") + "' -Target '" + real.toString().replace("'", "''") + "' | Out-Null";
            var process = new ProcessBuilder("powershell.exe", "-NoProfile", "-NonInteractive", "-Command", command)
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD).redirectError(ProcessBuilder.Redirect.DISCARD).start();
            try {
                boolean exited = process.waitFor(10, TimeUnit.SECONDS);
                if (!exited) process.destroyForcibly();
                assertThat(exited).isTrue(); assertThat(process.exitValue()).isZero();
            } finally {
                if (process.isAlive()) { process.destroyForcibly(); process.waitFor(5, TimeUnit.SECONDS); }
            }
        } else { Files.createSymbolicLink(junction, real); }
        try {
            var actualHash = hash(realFile);
            assertThatThrownBy(() -> loader.load(junction.resolve("assignment.json"), actualHash, "test-r1", ENV)).isInstanceOf(IllegalArgumentException.class);
        } finally {
            Files.deleteIfExists(junction);
        }
    }
    @Test void L12RejectsManifestMismatch() throws Exception {
        var path = file(directory, json("")); var loader = new ManagedRecoveryConfigurationLoader(); var digest = hash(path);
        assertThatThrownBy(() -> loader.load(path, "b".repeat(64), "test-r1", ENV)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> loader.load(path, digest, "old", ENV)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> loader.load(path, digest, "test-r1", "other")).isInstanceOf(IllegalArgumentException.class);
    }
}
