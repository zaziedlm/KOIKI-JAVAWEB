package org.koikifw.buildsupport.phase4.b1fixture;

import static org.koikifw.buildsupport.phase4.b1fixture.B1ReadContract.*;

import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

/** All children of one managed run, including sequential evidence readers. No drain guarantee. */
public final class B1ProcessHarness implements AutoCloseable {
    public record Entry(Process process,long pid,Instant started,UUID run,long generation,String source,String codeHash,String kind) { }
    public record ChildRead(int exit,String observation,long pid) { }
    private final B1SourceCollector.Fixture fixture;
    private final Manifest manifest;
    private final List<Entry> entries=new ArrayList<>();
    private final Path directory;
    private long generation;
    private boolean controlled=true;
    private boolean forced;
    public B1ProcessHarness(B1SourceCollector.Fixture fixture,Manifest manifest) throws Exception {
        this.fixture=fixture;this.manifest=manifest;
        directory=Files.createTempDirectory(Path.of("target"),"b1-process-");
    }
    private void requireIdle() {
        if(!controlled || entries.stream().anyMatch(e->e.process().isAlive())) throw new IllegalStateException("managed child unavailable");
    }
    private Entry register(Process process,String hash,String kind) {
        Entry entry=new Entry(process,process.pid(),process.info().startInstant().orElse(Instant.now()),manifest.run(),generation,manifest.source(),hash,kind);
        entries.add(entry);
        System.out.println("B1_PROCESS pid="+entry.pid()+" generation="+generation+" kind="+kind+" codeHash="+hash);
        return entry;
    }
    public Entry start(boolean publish,boolean accepted) throws Exception {
        requireIdle();++generation;
        Path jar=Path.of("target","phase4-level2-verification-0.1.0-SNAPSHOT.jar").toAbsolutePath();
        if(!manifest.jarHash().equals(hashFile(jar))) throw new IllegalStateException("JAR identity changed");
        Path log=directory.resolve("child-"+generation+".log");
        Path marker=directory.resolve("marker-"+generation);
        List<String> command=new ArrayList<>(List.of(javaExecutable(),"-jar",jar.toString(),"--probe.keep-alive=true",
                "--spring.modulith.events.republish-outstanding-events-on-restart=false"));
        if(publish) {
            command.add("--probe.approve.id="+manifest.event());
            command.add((accepted?"--probe.pause.after-send.file=":"--probe.pause.file=")+marker.toAbsolutePath());
        }
        ProcessBuilder builder=new ProcessBuilder(command).redirectErrorStream(true).redirectOutput(log.toFile());
        fixture.fixtureEnvironment(builder);
        Process process=B1ResourceLimits.start(builder,fixture.postgres,log);
        Entry entry=register(process,hashFile(jar),"ORDINARY");
        if(publish) await(()->Files.exists(marker),process);
        fixture.initializeViews();
        return entry;
    }
    public void terminate() throws Exception {
        Exception failure=null;
        for(Entry entry:entries) {
            if(entry.process().isAlive()) forced=true;
            try { finishProcess(entry.process()); }
            catch(Exception exception) { failure=combine(failure,exception); }
        }
        if(failure!=null) throw failure;
    }
    public Stop observe(Instant now) {
        boolean ended=!entries.isEmpty() && entries.stream().allMatch(e->!e.process().isAlive());
        return new Stop(manifest.run(),generation,manifest.source(),ended,forced,controlled,now,now.plusSeconds(60));
    }
    public void loseControl() { controlled=false; }
    public List<Entry> entries() { return List.copyOf(entries); }
    public ChildRead readInChild(UUID reference) throws Exception {
        requireIdle();++generation;
        Path output=directory.resolve("reader-"+generation+".log");
        String driver=Path.of(org.postgresql.Driver.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toString();
        String classes=Path.of(B1EvidenceLedger.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toString();
        ProcessBuilder builder=new ProcessBuilder(javaExecutable(),"-Xmx768m","-cp",classes+java.io.File.pathSeparator+driver,
                B1EvidenceLedger.class.getName(),reference.toString()).redirectErrorStream(true).redirectOutput(output.toFile());
        fixture.readerEnvironment(builder);
        Process process=builder.start();register(process,hashFile(Path.of(classes,B1EvidenceLedger.class.getName().replace('.',java.io.File.separatorChar)+".class")),"EVIDENCE_READER");
        ChildRead result=null;
        Exception failure=null;
        try {
            if(!process.waitFor(10,TimeUnit.SECONDS)) throw new IllegalStateException("reader timeout");
            String observation=Files.readString(output).trim();
            if(observation.length()>256 || !observation.matches("B1_READER_REJECTED|[0-9a-f-]{36}:[0-9a-f]{64}"))
                throw new IllegalStateException("unexpected reader output");
            result=new ChildRead(process.exitValue(),observation,process.pid());
        } catch(Exception exception) { failure=exception;
        } finally {
            try { cleanupReader(process,output); }
            catch(Exception cleanupFailure) { if(failure!=null) failure.addSuppressed(cleanupFailure);else failure=cleanupFailure; }
        }
        if(failure!=null) throw failure;
        return Objects.requireNonNull(result);
    }
    private void cleanupReader(Process process,Path output) throws Exception {
        Exception failure=null;
        try { finishProcess(process); } catch(Exception exception) { failure=exception; }
        try { deleteKnown(List.of(output),false); }
        catch(Exception exception) { failure=combine(failure,exception); }
        if(failure!=null) throw failure;
    }
    private static Exception combine(Exception first,Exception next) {
        if(first==null) return next;
        first.addSuppressed(next);return first;
    }
    private static void finishProcess(Process process) throws Exception {
        Exception failure=null;
        try {
            if(process.isAlive()) process.destroyForcibly();
            if(!process.waitFor(10,TimeUnit.SECONDS) || process.isAlive())
                throw new IllegalStateException("child cleanup timeout");
        } catch(Exception exception) { failure=exception; }
        for(java.io.Closeable stream:List.of(process.getOutputStream(),process.getInputStream(),process.getErrorStream())) {
            try { stream.close(); } catch(Exception exception) { failure=combine(failure,exception); }
        }
        if(failure!=null) throw failure;
    }
    private void deleteKnown(List<Path> files,boolean removeDirectory) throws Exception {
        Path owned=directory.toAbsolutePath().normalize();
        for(Path file:files) {
            if(!owned.equals(file.toAbsolutePath().normalize().getParent())
                    || !file.getFileName().toString().matches("(?:child-[0-9]+\\.log|reader-[0-9]+\\.log|marker-[0-9]+)"))
                throw new IllegalStateException("unexpected cleanup file");
        }
        long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(10);
        Exception last=null;
        do {
            last=null;
            for(Path file:files) {
                try { Files.deleteIfExists(file); } catch(Exception exception) { last=combine(last,exception); }
            }
            if(last==null && removeDirectory) {
                try { Files.deleteIfExists(directory); } catch(Exception exception) { last=exception; }
            }
            if(last==null) return;
            if(System.nanoTime()>=deadline) break;
            Thread.sleep(100);
        } while(System.nanoTime()<deadline);
        try { preserveCleanup(files); } catch(Exception exception) { last=combine(last,exception); }
        throw Objects.requireNonNull(last);
    }
    private void preserveCleanup(List<Path> files) throws Exception {
        Path saved=Files.createDirectories(Path.of("target","s1-b1-read-20261008","cleanup-failure-"+UUID.randomUUID()));
        StringBuilder states=new StringBuilder();
        for(Entry entry:entries) states.append("pid=").append(entry.pid()).append(" alive=").append(entry.process().isAlive())
                .append(" exit=").append(entry.process().isAlive()?"UNKNOWN":entry.process().exitValue())
                .append(" generation=").append(entry.generation()).append(" run=").append(entry.run()).append('\n');
        Files.writeString(saved.resolve("exit-state.txt"),states);
        for(Path file:files) if(file.getFileName().toString().endsWith(".log") && Files.exists(file)) {
            // Bound both input and output. Never retain connection/credential lines.
            String sanitized=Files.size(file)>1048576?"LOG_OMITTED_INPUT_LIMIT":Files.readString(file)
                    .replaceAll("(?im)^.*(?:password|username|jdbc|datasourceurl).*(?:\\R|$)","[CONNECTION_LINE_REDACTED]\n");
            byte[] bytes=sanitized.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            if(bytes.length>65536) {
                byte[] bounded=new byte[65536];
                System.arraycopy(bytes,0,bounded,0,16384);
                System.arraycopy(bytes,bytes.length-49152,bounded,16384,49152);bytes=bounded;
            }
            Files.write(saved.resolve(file.getFileName()),bytes);
        }
    }
    public static void await(BooleanSupplier condition,Process process) throws Exception {
        long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(10);
        while(!condition.getAsBoolean()) {
            if(!process.isAlive() || System.nanoTime()>deadline) throw new IllegalStateException("fixture wait timeout");
            Thread.sleep(50);
        }
    }
    private static String javaExecutable() { return Path.of(System.getProperty("java.home"),"bin","java.exe").toString(); }
    private static String hashFile(Path path) throws Exception {
        return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path)));
    }
    @Override public void close() throws Exception {
        Exception failure=null;
        try { terminate(); } catch(Exception exception) { failure=exception; }
        try {
            List<Path> known;
            try(var files=Files.list(directory)) { known=files.toList(); }
            deleteKnown(known,true);
        } catch(Exception exception) { failure=combine(failure,exception); }
        if(failure!=null) throw failure;
    }
}
