package org.koikifw.referenceacceptance.notification;

import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.sql.*;
import com.github.dockerjava.api.command.InspectContainerResponse;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.koikifw.reference.notification.application.query.RecoveryTarget;

/** Test-owned process bridge; no Tooling Java type crosses the Reference boundary. */
public final class B2FrozenSourceProcess implements AutoCloseable {
    public final Properties ready=new Properties();
    public final String readerSecret=UUID.randomUUID().toString();
    private final String adminSecret=UUID.randomUUID().toString();
    private final Path directory;
    private final Path readyFile;
    private final Path log;
    private final Process child;
    public B2FrozenSourceProcess() throws Exception {
        Path root=Path.of("").toAbsolutePath();
        while(!Files.isDirectory(root.resolve("build-support/phase4-level2-verification"))) {
            root=root.getParent();if(root==null) throw new IllegalStateException("Repository unavailable");
        }
        Path tooling=root.resolve("build-support/phase4-level2-verification");
        boolean windows=System.getProperty("os.name","").startsWith("Windows");
        Path javaExecutable=Path.of(System.getProperty("java.home"),"bin",windows?"java.exe":"java");
        if(!windows) throw new IllegalStateException("B2_PRECONDITION_WINDOWS_REQUIRED");
        if(!Files.isRegularFile(javaExecutable)) throw new IllegalStateException("B2_PRECONDITION_JAVA_UNAVAILABLE");
        String dependencies=classpath();
        for(String required:List.of("target/classes","target/test-classes",
                "target/phase4-level2-verification-0.1.0-SNAPSHOT.jar",
                "target/test-classes/org/koikifw/buildsupport/phase4/b2fixture/B2FrozenSourceCoordinator.class",
                "target/test-classes/org/koikifw/buildsupport/phase4/b1fixture/B1ResourceLimits.class",
                "target/test-classes/s1-b1/read-source.sql","target/test-classes/s1-b2/frozen-source.sql")) {
            if(!Files.exists(tooling.resolve(required))) throw new IllegalStateException("B2_PRECONDITION_TOOLING_UNPREPARED");
        }
        directory=tooling.resolve("target/s1-b2-reference-"+UUID.randomUUID());
        Files.createDirectory(directory);readyFile=directory.resolve("ready.properties");log=directory.resolve("coordinator.log");
        String cp=tooling.resolve("target/test-classes")+java.io.File.pathSeparator+tooling.resolve("target/classes")+java.io.File.pathSeparator+dependencies;
        var builder=new ProcessBuilder(javaExecutable.toString(),
                "-Xmx768m","-Dkoiki.b1.resource-limits.enabled=true","-Dkoiki.b2.resource-limits.enabled=true",
                "-cp",cp,"org.koikifw.buildsupport.phase4.b2fixture.B2FrozenSourceCoordinator",readyFile.toString());
        builder.directory(tooling.toFile()).redirectErrorStream(true).redirectOutput(log.toFile());
        builder.environment().put("B2_SOURCE_READER_PASSWORD",readerSecret);
        builder.environment().put("B2_FIXTURE_ADMIN_PASSWORD",adminSecret);
        try {child=builder.start();}
        catch(java.io.IOException failure) {
            Files.deleteIfExists(log);Files.deleteIfExists(directory);
            throw new IllegalStateException("B2_PRECONDITION_LAUNCH_FAILED");
        }
        try {
            long deadline=System.nanoTime()+Duration.ofSeconds(90).toNanos();
            while(!Files.exists(readyFile) && child.isAlive() && System.nanoTime()<deadline) {
                if(Files.exists(log) && Files.size(log)>1048576) throw new IllegalStateException("Finite child log exceeded");
                Thread.sleep(50);
            }
            if(!Files.exists(readyFile) || !child.isAlive()) throw new IllegalStateException("B2 coordinator not ready; finite log retained");
            if(Files.size(readyFile)>8192) throw new IllegalStateException("Ready protocol exceeded");
            try(var input=Files.newInputStream(readyFile)) {ready.load(input);}
            if(!"1".equals(ready.getProperty("protocol")) || !ready.getProperty("database").matches("[a-zA-Z0-9_]+"))
                throw new IllegalStateException("Ready protocol invalid");
        } catch(Exception|AssertionError failure) {close();throw failure;}
    }
    private static String classpath() {
        String supplied=System.getProperty("koiki.b2.tooling.classpath-file");
        if(supplied==null || supplied.isBlank()) throw new IllegalStateException("B2_PRECONDITION_CLASSPATH_MISSING");
        try {
            Path file=Path.of(supplied);
            if(!file.isAbsolute() || !Files.isRegularFile(file)) throw new IllegalStateException("B2_PRECONDITION_CLASSPATH_UNAVAILABLE");
            if(Files.size(file)>1048576) throw new IllegalStateException("B2_PRECONDITION_CLASSPATH_LIMIT");
            String value=Files.readString(file,StandardCharsets.UTF_8).replaceFirst("^\\uFEFF","").trim();
            if(value.isEmpty()) throw new IllegalStateException("B2_PRECONDITION_CLASSPATH_EMPTY");
            String[] entries=value.split(java.util.regex.Pattern.quote(java.io.File.pathSeparator),-1);
            if(entries.length>512) throw new IllegalStateException("B2_PRECONDITION_CLASSPATH_LIMIT");
            for(String entry:entries) {
                Path dependency=Path.of(entry);
                if(!dependency.isAbsolute() || !Files.isRegularFile(dependency))
                    throw new IllegalStateException("B2_PRECONDITION_CLASSPATH_ENTRY_UNAVAILABLE");
            }
            return value;
        } catch(java.io.IOException|InvalidPathException failure) {
            throw new IllegalStateException("B2_PRECONDITION_CLASSPATH_UNAVAILABLE");
        }
    }
    public String sourceUrl() {return "jdbc:postgresql://127.0.0.1:"+Integer.parseInt(ready.getProperty("port"))+"/"+ready.getProperty("database");}
    public RecoveryTarget target() {return new RecoveryTarget(ready.getProperty("environment"),UUID.fromString(ready.getProperty("publication")),
            UUID.fromString(ready.getProperty("event")),ready.getProperty("listener"),Integer.parseInt(ready.getProperty("attempt")));}
    public Map<String,Object> environment() {return Map.of("B2_SOURCE_JDBC_URL",sourceUrl(),"B2_SOURCE_READER_USERNAME","b2_reader","B2_SOURCE_READER_PASSWORD",readerSecret);}
    public Map<String,Object> settings() {
        String prefix=org.koikifw.reference.notification.configuration.FrozenRecoverySourceConfiguration.PREFIX;
        return Map.of(prefix+"mode","tooling-jdbc-v1",prefix+"environment-id",ready.getProperty("environment"),prefix+"run-id",ready.getProperty("run"),
            prefix+"source-id",ready.getProperty("source"),prefix+"expected-jar-sha256",ready.getProperty("jar-sha256"),prefix+"expected-revision",ready.getProperty("revision"));
    }
    public Connection writerAttempt() throws SQLException {return DriverManager.getConnection(sourceUrl()+"?connectTimeout=10&socketTimeout=10","b1_writer","unavailable");}
    /** Read-only inspect of this child's container, never a second container or a start call. */
    public PostgreSQLContainer attachedDatabase() throws Exception {
        String text=Files.readString(log);
        var matcher=java.util.regex.Pattern.compile("B1_RESOURCE dbId=([a-f0-9]{64})").matcher(text);
        if(!matcher.find()) throw new IllegalStateException("Owned container identity unavailable");
        String id=matcher.group(1);
        InspectContainerResponse info=DockerClientFactory.instance().client().inspectContainerCmd(id).exec();
        return new PostgreSQLContainer("postgres:17-alpine") {
            @Override public String getJdbcUrl() {return sourceUrl();}
            @Override public String getUsername() {return "test";}
            @Override public String getPassword() {return adminSecret;}
            @Override public String getContainerId() {return id;}
            @Override public InspectContainerResponse getContainerInfo() {return info;}
        };
    }
    @Override public void close() throws Exception {
        boolean orderly=true;
        if(child.isAlive()) {
            try {child.getOutputStream().write("finish\n".getBytes(StandardCharsets.UTF_8));child.getOutputStream().flush();}
            catch(java.io.IOException ended) {orderly=false;}
            if(!child.waitFor(10,TimeUnit.SECONDS)) {orderly=false;child.destroyForcibly();if(!child.waitFor(10,TimeUnit.SECONDS)) throw new IllegalStateException("Owned coordinator remains");}
        }
        child.getOutputStream().close();child.getInputStream().close();child.getErrorStream().close();
        if(Files.exists(log)) {
            if(Files.size(log)>1048576) throw new IllegalStateException("Finite child log exceeded");
            String text=Files.readString(log).replaceAll("(?im)^.*(?:password|username|jdbc|datasourceurl).*$(?:\\R)?","[CONNECTION_LINE_REDACTED]\n");
            Files.writeString(log,text);
        }
        Files.deleteIfExists(readyFile);Files.deleteIfExists(directory.resolve("ready.properties.partial"));
        if(!orderly || child.exitValue()!=0) throw new IllegalStateException("Coordinator cleanup failed; finite log retained");
    }
}
