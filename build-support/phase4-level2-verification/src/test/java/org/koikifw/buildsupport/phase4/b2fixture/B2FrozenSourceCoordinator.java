package org.koikifw.buildsupport.phase4.b2fixture;

import static org.koikifw.buildsupport.phase4.b1fixture.B1ReadContract.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.*;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Properties;
import java.util.UUID;
import org.koikifw.buildsupport.phase4.b1fixture.*;

/** One fixture owner. Normal launch is permanently closed before ready. */
public final class B2FrozenSourceCoordinator implements AutoCloseable {
    public final B1SourceCollector.Fixture fixture;
    public final Manifest manifest;
    public final B1ProcessHarness children;
    public B2FrozenSourceProtocol protocol;
    public Snapshot snapshot;
    private boolean launchClosed;

    public B2FrozenSourceCoordinator() throws Exception {
        fixture=new B1SourceCollector.Fixture(true);
        Manifest prepared;
        try {prepared=fixture.prepare(Instant.now().truncatedTo(ChronoUnit.MILLIS),ENVIRONMENT);}
        catch(Exception|AssertionError failure) {fixture.close();throw failure;}
        manifest=prepared;children=new B1ProcessHarness(fixture,manifest);
    }
    public synchronized void start(boolean accepted) throws Exception {
        if(launchClosed) throw new IllegalStateException("normal launch closed");
        children.start(true,accepted);
    }
    public synchronized void prepareFrozen(String readerSecret) throws Exception {
        if(launchClosed) throw new IllegalStateException("coordinator already closed");
        children.terminate();launchClosed=true;
        Instant now=Instant.now().truncatedTo(ChronoUnit.MILLIS);
        Target target=fixture.discover(manifest);Stop stop=children.observe(now);
        snapshot=B1SourceCollector.collect(fixture,manifest,target,stop,now,true);
        protocol=new B2FrozenSourceProtocol(fixture,readerSecret);
        protocol.freeze(manifest,snapshot,stop,launchClosed,now);
    }
    @Override public void close() throws Exception {
        try {children.close();} finally {fixture.close();}
    }
    public static void main(String[] arguments) throws Exception {
        if(arguments.length!=1) throw new IllegalArgumentException("ready destination missing");
        Path ready=Path.of(arguments[0]).toAbsolutePath().normalize();
        if(!ready.startsWith(Path.of("target").toAbsolutePath().normalize()) || Files.exists(ready))
            throw new IllegalArgumentException("ready destination invalid");
        String readerSecret=System.getenv("B2_SOURCE_READER_PASSWORD");
        String adminSecret=System.getenv("B2_FIXTURE_ADMIN_PASSWORD");
        if(readerSecret==null || adminSecret==null || !adminSecret.matches("[0-9a-f-]{36}"))
            throw new IllegalArgumentException("fixture environment missing");
        try(var owner=new B2FrozenSourceCoordinator()) {
            // Parent-generated ephemeral secret; neither ready nor command line receives it.
            try(Connection c=owner.fixture.admin();Statement s=c.createStatement()) {
                s.setQueryTimeout(10);
                s.execute("ALTER ROLE "+owner.fixture.postgres.getUsername()+" PASSWORD '"+adminSecret+"'");
            }
            owner.fixture.postgres.withPassword(adminSecret);
            owner.start(false);owner.prepareFrozen(readerSecret);
            Properties result=new Properties();
            result.setProperty("protocol","1");result.setProperty("run",owner.manifest.run().toString());
            result.setProperty("port",owner.fixture.postgres.getMappedPort(5432).toString());
            result.setProperty("database",owner.fixture.postgres.getDatabaseName());
            result.setProperty("environment",ENVIRONMENT);result.setProperty("publication",owner.snapshot.target().publication().toString());
            result.setProperty("event",owner.manifest.event().toString());result.setProperty("listener",owner.manifest.listener());
            result.setProperty("attempt",Integer.toString(owner.snapshot.target().attempt()));
            result.setProperty("source",owner.manifest.source());result.setProperty("jar-sha256",owner.manifest.jarHash());
            result.setProperty("revision",Long.toString(owner.snapshot.revision()));
            Files.createDirectories(ready.getParent());
            Path partial=ready.resolveSibling(ready.getFileName()+".partial");
            try(var output=Files.newOutputStream(partial,StandardOpenOption.CREATE_NEW)) {result.store(output,"B2 finite ready");}
            Files.move(partial,ready,StandardCopyOption.ATOMIC_MOVE);
            System.out.println("B2_FROZEN_READY");
            try(var commands=new BufferedReader(new InputStreamReader(System.in,StandardCharsets.UTF_8))) {
                String command=commands.readLine();
                if(command!=null && !command.equals("finish")) throw new IllegalStateException("normal launch closed");
            }
        }
    }
}
