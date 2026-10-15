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
class B1StopObservationTest {
    static B1SourceCollector.Fixture fixture;
    @BeforeAll static void start() throws Exception {fixture=new B1SourceCollector.Fixture(true);}
    @AfterAll static void finish() {if(fixture!=null) fixture.close();}
    @ParameterizedTest(name="{0}") @ValueSource(strings={"S01","S02","S03","S04","S05","S06","S07","S08"})
    void stopBranches(String id) throws Exception {
        fixture.reset();Instant now=Instant.now().truncatedTo(ChronoUnit.MILLIS);
        Manifest manifest=fixture.prepare(now,ENVIRONMENT);
        try(var harness=new B1ProcessHarness(fixture,manifest)) {
            if(id.equals("S08")) {
                Stop absent=harness.observe(now);assertFalse(absent.allEnded());
                var stopFile=java.nio.file.Files.createTempFile(java.nio.file.Path.of("target"),"b1-stop-", ".txt");
                try {
                    java.nio.file.Files.writeString(stopFile,"stopped");
                    assertFalse(harness.observe(now).allEnded(),"stop file alone does not create process observations");
                } finally {java.nio.file.Files.deleteIfExists(stopFile);}
                try(Connection admin=fixture.admin();Statement s=admin.createStatement()) {
                    try(ResultSet r=s.executeQuery("SELECT pg_try_advisory_lock(81008)")) {r.next();assertTrue(r.getBoolean(1));}
                    assertFalse(harness.observe(now).matches(manifest.run(),absent.generation(),SOURCE,now),"advisory lock does not create process evidence");
                    s.execute("SELECT pg_advisory_unlock(81008)");
                }
                return;
            }
            var child=harness.start(true,false);
            assertTrue(child.process().isAlive());
            assertFalse(harness.observe(now).allEnded());
            if(id.equals("S02")) assertFalse(harness.observe(now).matches(manifest.run(),child.generation(),SOURCE,now));
            harness.terminate();now=Instant.now().truncatedTo(ChronoUnit.MILLIS);
            Stop stop=harness.observe(now);
            switch(id) {
                case "S01" -> {assertTrue(stop.matches(manifest.run(),child.generation(),SOURCE,now));assertFalse(child.process().isAlive());assertEquals(1,harness.entries().size());}
                case "S02" -> {assertTrue(stop.forced());assertTrue(stop.allEnded());assertTrue(stop.controlled());}
                case "S03" -> assertFalse(stop.matches(manifest.run(),stop.generation()+1,SOURCE,now));
                case "S04" -> {assertFalse(stop.matches(UUID.randomUUID(),stop.generation(),SOURCE,now));assertFalse(stop.matches(manifest.run(),stop.generation(),"other-source",now));assertEquals(manifest.jarHash(),child.codeHash());}
                case "S05" -> assertFalse(stop.matches(manifest.run(),stop.generation(),SOURCE,stop.until()));
                case "S06" -> {harness.start(false,false);Stop restarted=harness.observe(now);assertNotEquals(stop.generation(),restarted.generation());assertFalse(restarted.matches(manifest.run(),stop.generation(),SOURCE,now));}
                case "S07" -> {harness.loseControl();assertFalse(harness.observe(now).matches(manifest.run(),stop.generation(),SOURCE,now));assertThrows(IllegalStateException.class,()->harness.start(false,false));}
                default -> fail("unmapped case");
            }
        }
        B1ResourceLimits.assertDatabase(fixture.postgres);
    }
}
