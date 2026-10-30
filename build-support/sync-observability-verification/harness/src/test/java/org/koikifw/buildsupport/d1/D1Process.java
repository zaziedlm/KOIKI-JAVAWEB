package org.koikifw.buildsupport.d1;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Properties;
import java.util.List;
import java.util.concurrent.TimeUnit;

/** Owns one child by Process object, PID, start time and the frozen JAR argument. */
final class D1Process implements AutoCloseable {
    final Process process;
    final Path log;
    final Properties ready = new Properties();
    private final Thread reader;
    private final Path cleanup;
    private volatile IOException readFailure;
    private volatile boolean closing;

    D1Process(Path java, Path jar, Path evidence, String phase, int receiverPort, boolean export, boolean virtual,
            String owner) throws Exception {
        log = evidence.resolve(phase + "-app.log");
        cleanup = evidence.resolve(phase + "-cleanup.txt");
        Path readyFile = evidence.resolve(phase + "-ready.properties");
        var args = new ArrayList<String>();
        args.addAll(List.of(java.toString(), "-Xmx512m", "-jar", jar.toString(),
                "--d1.ready-file=" + readyFile, "--spring.threads.virtual.enabled=" + virtual,
                "--management.tracing.export.otlp.enabled=" + export,
                "--management.opentelemetry.tracing.export.otlp.endpoint=http://127.0.0.1:" + receiverPort + "/v1/traces"));
        ProcessBuilder builder = new ProcessBuilder(args).redirectErrorStream(true);
        builder.environment().clear();
        builder.environment().put("LANG", "C.UTF-8");
        process = builder.start();
        Properties identity = new Properties();
        identity.setProperty("owner", owner);
        identity.setProperty("pid", Long.toString(process.pid()));
        identity.setProperty("startMillis", Long.toString(process.info().startInstant().orElseThrow().toEpochMilli()));
        identity.setProperty("jar", jar.toString());
        try (var out = Files.newOutputStream(evidence.resolve(phase + "-ownership.properties"))) {
            identity.store(out, "owned child");
        }
        reader = new Thread(() -> {
            try (var in = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
                    var out = Files.newBufferedWriter(log, StandardCharsets.UTF_8)) {
                int total = 0;
                while (true) {
                    String line;
                    try { line = in.readLine(); }
                    catch (IOException ex) {
                        // Process.destroy() may close its stdout pipe during normal owned shutdown.
                        if (closing && "Stream closed".equals(ex.getMessage())) { break; }
                        throw ex;
                    }
                    if (line == null) { break; }
                    total += line.getBytes(StandardCharsets.UTF_8).length + 1;
                    if (total > 2_097_152) { throw new IOException("App log cap exceeded"); }
                    if (line.contains(D1ConnectionTest.AUTH_CANARY) || line.contains(D1ConnectionTest.BODY_CANARY)) {
                        throw new IOException("Synthetic canary leaked into app log");
                    }
                    out.write(line); out.newLine(); out.flush();
                }
            } catch (IOException ex) { readFailure = ex; process.destroy(); }
        }, "d1-owned-log-reader");
        reader.start();
        try {
            long until = System.nanoTime() + TimeUnit.SECONDS.toNanos(50);
            while (!Files.exists(readyFile)) {
                if (!process.isAlive() || System.nanoTime() > until) {
                    throw new IOException("App failed to become ready; inspect bounded " + phase + " log");
                }
                Thread.sleep(50);
            }
            try (var in = Files.newInputStream(readyFile)) { ready.load(in); }
            if (!ready.getProperty("export").equals(Boolean.toString(export))
                    || !ready.getProperty("virtual").equals(Boolean.toString(virtual))
                    || Integer.parseInt(ready.getProperty("exporters")) != (export ? 1 : 0)) {
                throw new IOException("Effective configuration or exporter count mismatch");
            }
        } catch (Exception ex) { close(); throw ex; }
    }
    String text() throws IOException {
        if (readFailure != null) { throw readFailure; }
        return Files.readString(log);
    }
    int port() { return Integer.parseInt(ready.getProperty("port")); }
    @Override public void close() throws IOException {
        closing = true;
        process.destroy();
        try {
            if (!process.waitFor(10, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                if (!process.waitFor(5, TimeUnit.SECONDS)) { throw new IOException("Owned child remained"); }
            }
            reader.join(3000);
            if (reader.isAlive()) { throw new IOException("Log reader remained"); }
            if (readFailure != null) { throw readFailure; }
            Files.writeString(cleanup, "pid=" + process.pid() + " alive=false reader=false\n");
        } catch (InterruptedException ex) { Thread.currentThread().interrupt(); throw new IOException(ex); }
    }
}
