package org.koikifw.buildsupport.phase4;

import static org.junit.jupiter.api.Assertions.*;
import static org.koikifw.buildsupport.phase4.b1fixture.B1ReadContract.*;
import java.sql.*;
import java.util.UUID;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.koikifw.buildsupport.phase4.b1fixture.*;

@org.junit.jupiter.api.condition.EnabledIfSystemProperty(named="koiki.b1.resource-limits.enabled",matches="true")
class B1EvidenceLedgerTest {
    static B1SourceCollector.Fixture fixture;
    @BeforeAll static void start() throws Exception { fixture=new B1SourceCollector.Fixture(); }
    @AfterAll static void finish() { if(fixture!=null) fixture.close(); }
    @ParameterizedTest(name="{0}") @ValueSource(strings={"E01","E02","E03","E04","E05","E06","E07","E08","E09","E10"})
    void evidenceBranches(String id) throws Exception {
        var c=B1TargetReadTest.Context.create(fixture,false);
        switch(id) {
            case "E01" -> { try(Connection reader=fixture.reader()) { assertEquals(c.evidence,B1EvidenceLedger.resolve(reader,c.evidence.reference())); }assertTrue(c.read().observed()); }
            case "E02" -> assertEquals(c.evidence,B1EvidenceLedger.save(fixture,c.evidence.key(),c.snapshot,c.evidence.subject()));
            case "E03" -> {
                Snapshot newer=B1SourceCollector.collect(fixture,c.manifest,c.target,c.stop,c.now,false);
                assertThrows(SQLException.class,()->B1EvidenceLedger.save(fixture,c.evidence.key(),newer,c.evidence.subject()));
                try(Connection reader=fixture.reader()) { assertEquals(c.evidence.canonical(),B1EvidenceLedger.resolve(reader,c.evidence.reference()).canonical()); }
            }
            case "E04" -> {
                try(Connection reader=fixture.reader()) { assertThrows(SQLException.class,()->B1EvidenceLedger.resolve(reader,UUID.randomUUID())); }
                assertThrows(SQLException.class,()->fixture.execute("INSERT INTO b1_evidence.document SELECT ?,environment,permit,operation,worker_generation,run,subject,canonical,valid_until FROM b1_evidence.document WHERE reference=?",UUID.randomUUID(),c.evidence.reference()));
            }
            case "E05" -> {
                Evidence wrong=new Evidence(c.evidence.reference(),new EvidenceKey("other",c.evidence.key().permit(),c.evidence.key().operation(),1),c.evidence.run(),c.evidence.subject(),c.evidence.canonical(),c.evidence.until());
                assertFalse(B1JdbcReadClient.read(fixture,c.snapshot,wrong,c.stop,c.now).observed());
            }
            case "E06" -> {
                Evidence wrong=new Evidence(c.evidence.reference(),c.evidence.key(),UUID.randomUUID(),"other-subject",c.evidence.canonical(),c.evidence.until());
                assertFalse(B1JdbcReadClient.read(fixture,c.snapshot,wrong,c.stop,c.now).observed());
                assertThrows(SQLException.class,()->B1EvidenceLedger.save(fixture,c.evidence.key(),c.snapshot,"other-subject"));
            }
            case "E07" -> { assertFalse(c.readAt(c.evidence.until()).observed());B1EvidenceLedger.revoke(fixture,c.evidence.reference(),c.now);assertFalse(c.read().observed()); }
            case "E08" -> {
                fixture.execute("REVOKE INSERT ON b1_evidence.document FROM b1_writer");
                try { assertThrows(SQLException.class,()->B1EvidenceLedger.save(fixture,new EvidenceKey(ENVIRONMENT,UUID.randomUUID(),UUID.randomUUID(),1),c.snapshot,"test-collector")); }
                finally { fixture.execute("GRANT INSERT ON b1_evidence.document TO b1_writer"); }
                try(Connection reader=fixture.reader();Statement s=reader.createStatement();ResultSet r=s.executeQuery("SELECT count(*) FROM b1_read.evidence")) {r.next();assertEquals(1,r.getInt(1));}
            }
            case "E09" -> {
                try(Connection writer=fixture.writer();Statement s=writer.createStatement()) {
                    assertThrows(SQLException.class,()->s.execute("UPDATE b1_evidence.document SET subject='overwrite'"));
                    assertThrows(SQLException.class,()->s.execute("DELETE FROM b1_evidence.document"));
                }
                assertThrows(SQLException.class,()->fixture.execute("UPDATE b1_evidence.document SET subject='overwrite'"));
                assertThrows(SQLException.class,()->fixture.execute("DELETE FROM b1_evidence.document"));
                assertTrue(c.read().observed());
            }
            case "E10" -> {
                try(var harness=new B1ProcessHarness(fixture,c.manifest)) {
                    var first=harness.readInChild(c.evidence.reference());var second=harness.readInChild(c.evidence.reference());
                    assertEquals(0,first.exit());assertEquals(0,second.exit());assertEquals(first.observation(),second.observation());
                    assertEquals(c.evidence.reference()+":"+digest(c.evidence.canonical()),first.observation());
                    assertNotEquals(first.pid(),second.pid());
                    B1EvidenceLedger.revoke(fixture,c.evidence.reference(),c.now);
                    var revoked=harness.readInChild(c.evidence.reference());assertEquals(2,revoked.exit());assertEquals("B1_READER_REJECTED",revoked.observation());
                    assertTrue(harness.entries().stream().noneMatch(e->e.process().isAlive()));
                }
            }
            default -> fail("unmapped case");
        }
        B1ResourceLimits.assertDatabase(fixture.postgres);
    }
}
