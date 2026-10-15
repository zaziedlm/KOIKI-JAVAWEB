package org.koikifw.buildsupport.phase4;

import static org.junit.jupiter.api.Assertions.*;
import java.sql.*;
import java.nio.file.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.koikifw.buildsupport.phase4.b1fixture.*;

@org.junit.jupiter.api.condition.EnabledIfSystemProperty(named="koiki.b1.resource-limits.enabled",matches="true")
class B1ReaderPrivilegeTest {
    static B1SourceCollector.Fixture fixture;
    @BeforeAll static void start() throws Exception { fixture=new B1SourceCollector.Fixture(); }
    @AfterAll static void finish() { if(fixture!=null) fixture.close(); }
    @ParameterizedTest(name="{0}") @ValueSource(strings={"R01","R02","R03","R04","R05","R06","R07","R08"})
    void privilegeBranches(String id) throws Exception {
        var c=B1TargetReadTest.Context.create(fixture,false);
        switch(id) {
            case "R01" -> { try(Connection reader=fixture.reader();Statement s=reader.createStatement();ResultSet r=s.executeQuery("SELECT current_user,count(*) FROM b1_read.target GROUP BY current_user")) {r.next();assertEquals("b1_reader",r.getString(1));assertEquals(1,r.getInt(2));}assertTrue(c.read().observed()); }
            case "R02" -> { try(Connection reader=fixture.reader();Statement s=reader.createStatement()) { for(String table:new String[]{"public.event_publication","public.probe_provider_send","b1_source.snapshot","b1_evidence.document"}) assertThrows(SQLException.class,()->s.executeQuery("SELECT * FROM "+table)); } }
            case "R03" -> { try(Connection reader=fixture.reader();Statement s=reader.createStatement()) {
                assertThrows(SQLException.class,()->s.execute("INSERT INTO b1_read.revocation VALUES (NULL,NULL)"));
                assertThrows(SQLException.class,()->s.execute("UPDATE b1_read.evidence SET subject='changed'"));
                assertThrows(SQLException.class,()->s.execute("DELETE FROM b1_read.evidence"));
            } }
            case "R04" -> {
                fixture.execute("UPDATE b1_source.binding SET environment='other' WHERE run=?",c.manifest.run());assertFalse(c.read().observed());
                try(Connection reader=fixture.reader();Statement s=reader.createStatement()) {assertThrows(SQLException.class,()->s.execute("CREATE TABLE b1_source.unauthorized(id int)"));}
            }
            case "R05" -> {
                assertThrows(SQLException.class,()->{try(Connection ignored=fixture.wrongReader()) { fail("wrong credential accepted"); }});
                fixture.execute("UPDATE b1_source.binding SET source='other-source' WHERE run=?",c.manifest.run());assertFalse(c.read().observed());
            }
            case "R06" -> {
                try(Connection reader=fixture.reader();Statement s=reader.createStatement()) {
                    s.setQueryTimeout(1);long started=System.nanoTime();
                    assertThrows(SQLException.class,()->s.executeQuery("SELECT pg_sleep(2) FROM b1_read.target"));
                    assertTrue(java.time.Duration.ofNanos(System.nanoTime()-started).toSeconds()<10);
                }
                try(Connection reader=fixture.reader();Statement s=reader.createStatement();ResultSet r=s.executeQuery("SHOW statement_timeout")) {r.next();assertEquals("10s",r.getString(1));}
                assertTrue(fixture.url().contains("connectTimeout=10&socketTimeout=10"));
            }
            case "R07" -> {
                fixture.execute("REVOKE SELECT ON b1_read.revocation FROM b1_reader");
                try {assertEquals("SUPPLY_UNAVAILABLE_OR_INVALID",c.read().reason());assertFalse(c.read().observed());}
                finally {fixture.execute("GRANT SELECT ON b1_read.revocation TO b1_reader");}
            }
            case "R08" -> {
                Path jar=Path.of("target","phase4-level2-verification-0.1.0-SNAPSHOT.jar");
                try(var zip=new java.util.zip.ZipFile(jar.toFile())) {assertTrue(zip.stream().noneMatch(e->e.getName().contains("b1fixture")||e.getName().contains("s1-b1")||e.getName().contains("B1Target")));}
                fixture.execute("REVOKE SELECT ON b1_read.target FROM b1_reader");
                try {String reason=c.read().reason();assertEquals("SUPPLY_UNAVAILABLE_OR_INVALID",reason);assertFalse(reason.contains("jdbc:"));}
                finally {fixture.execute("GRANT SELECT ON b1_read.target TO b1_reader");}
            }
            default -> fail("unmapped case");
        }
        B1ResourceLimits.assertDatabase(fixture.postgres);
    }
}
