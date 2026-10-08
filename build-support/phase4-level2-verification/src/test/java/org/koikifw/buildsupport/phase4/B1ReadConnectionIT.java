package org.koikifw.buildsupport.phase4;

import static org.junit.jupiter.api.Assertions.*;
import static org.koikifw.buildsupport.phase4.b1fixture.B1ReadContract.*;
import java.sql.*;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.koikifw.buildsupport.phase4.b1fixture.*;

@org.junit.jupiter.api.condition.EnabledIfSystemProperty(named="koiki.b1.resource-limits.enabled",matches="true")
class B1ReadConnectionIT {
    static B1SourceCollector.Fixture fixture;
    @BeforeAll static void start() throws Exception {fixture=new B1SourceCollector.Fixture(true);}
    @AfterAll static void finish() {if(fixture!=null) fixture.close();}
    @ParameterizedTest(name="{0}") @ValueSource(strings={"I01","I02","I03","I04","I05","I06","I07","I08"})
    void connectionBranches(String id) throws Exception {
        fixture.reset();Instant prepared=Instant.now().truncatedTo(ChronoUnit.MILLIS);
        Manifest manifest=fixture.prepare(prepared,ENVIRONMENT);
        try(var harness=new B1ProcessHarness(fixture,manifest)) {
            var child=harness.start(true,id.equals("I02"));
            harness.terminate();
            assertFalse(child.process().isAlive());
            Instant now=Instant.now().truncatedTo(ChronoUnit.MILLIS);
            Target target=fixture.discover(manifest);Stop stop=harness.observe(now);
            if(id.equals("I04")) {
                // Hold snapshot INSERT after the collector has read the source, then change the raw row.
                var worker=java.util.concurrent.Executors.newSingleThreadExecutor();
                try(Connection gate=fixture.admin()) {
                    gate.setAutoCommit(false);
                    try(Statement lock=gate.createStatement()) {lock.setQueryTimeout(10);lock.execute("LOCK b1_source.snapshot IN ACCESS EXCLUSIVE MODE");}
                    var pending=worker.submit(()->B1SourceCollector.collect(fixture,manifest,target,stop,now,false));
                    boolean blocked=false;long deadline=System.nanoTime()+java.util.concurrent.TimeUnit.SECONDS.toNanos(10);
                    try(Connection observer=fixture.admin();PreparedStatement query=observer.prepareStatement(
                            "SELECT count(*) FROM pg_stat_activity WHERE usename='b1_writer' AND wait_event_type='Lock'")) {
                        query.setQueryTimeout(10);
                        while(!blocked && System.nanoTime()<deadline) {
                            try(ResultSet result=query.executeQuery()) {result.next();blocked=result.getInt(1)==1;}
                            if(!blocked) Thread.sleep(50);
                        }
                    }
                    assertTrue(blocked,"collector reached snapshot save after reading source");
                    fixture.execute("UPDATE public.event_publication SET completion_attempts=completion_attempts+1 WHERE id=?",target.publication());
                    gate.commit();
                    Snapshot stale=pending.get(10,java.util.concurrent.TimeUnit.SECONDS);
                    Evidence proof=B1EvidenceLedger.save(fixture,new EvidenceKey(ENVIRONMENT,UUID.randomUUID(),UUID.randomUUID(),stop.generation()),stale,"managed-collector");
                    assertFalse(B1JdbcReadClient.read(fixture,stale,proof,stop,now).observed());
                } finally {worker.shutdownNow();assertTrue(worker.awaitTermination(10,java.util.concurrent.TimeUnit.SECONDS));}
                return;
            }
            if(id.equals("I06")) {
                fixture.execute("REVOKE INSERT ON b1_source.snapshot FROM b1_writer");
                try {assertThrows(SQLException.class,()->B1SourceCollector.collect(fixture,manifest,target,stop,now,false));}
                finally {fixture.execute("GRANT INSERT ON b1_source.snapshot TO b1_writer");}
                try(Connection r=fixture.reader();Statement s=r.createStatement();ResultSet result=s.executeQuery("SELECT count(*) FROM b1_read.evidence")) {result.next();assertEquals(0,result.getInt(1));}
                return;
            }
            Snapshot snapshot=B1SourceCollector.collect(fixture,manifest,target,stop,now,id.equals("I03"));
            Evidence evidence=B1EvidenceLedger.save(fixture,new EvidenceKey(ENVIRONMENT,UUID.randomUUID(),UUID.randomUUID(),stop.generation()),snapshot,"managed-collector");
            Read read=B1JdbcReadClient.read(fixture,snapshot,evidence,stop,now);
            assertTrue(read.observed());
            switch(id) {
                case "I01" -> {assertEquals(manifest.event(),read.snapshot().target().event());assertEquals(Provider.UNKNOWN,read.provider());}
                case "I02" -> {assertEquals(Provider.ACCEPTED,read.provider());assertNotNull(snapshot.receipt());assertEquals(evidence,B1EvidenceLedger.save(fixture,evidence.key(),snapshot,"managed-collector"));}
                case "I03" -> {assertEquals(Provider.FIXTURE_NOT_ACCEPTED,read.provider());assertTrue(stop.allEnded());assertTrue(stop.forced());assertNull(snapshot.receipt());}
                case "I05" -> {
                    fixture.execute("UPDATE public.event_publication SET completion_attempts=completion_attempts+1 WHERE id=?",target.publication());
                    assertTrue(read.observed(),"prior observation persists; no atomic protection after return");
                    assertFalse(B1JdbcReadClient.read(fixture,snapshot,evidence,stop,now).observed());
                    System.out.println("B1_KNOWN_GAP I05/D12 last-read-to-use window unprotected; no permit operation invoked");
                }
                case "I07" -> {
                    var first=harness.readInChild(evidence.reference());var second=harness.readInChild(evidence.reference());
                    assertEquals(0,first.exit());assertEquals(0,second.exit());assertNotEquals(first.pid(),second.pid());
                    assertEquals(first.observation(),second.observation());
                    B1EvidenceLedger.revoke(fixture,evidence.reference(),Instant.now().truncatedTo(ChronoUnit.MILLIS));
                    assertEquals(2,harness.readInChild(evidence.reference()).exit());
                }
                case "I08" -> {assertTrue(stop.forced());assertTrue(harness.entries().stream().noneMatch(e->e.process().isAlive()));assertTrue(fixture.postgres.isRunning(),"DB deliberately retained until class cleanup");}
                default -> fail("unmapped case");
            }
        }
        B1ResourceLimits.assertDatabase(fixture.postgres);
    }
}
