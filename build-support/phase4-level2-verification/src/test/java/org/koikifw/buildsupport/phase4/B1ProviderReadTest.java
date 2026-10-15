package org.koikifw.buildsupport.phase4;

import static org.junit.jupiter.api.Assertions.*;
import static org.koikifw.buildsupport.phase4.b1fixture.B1ReadContract.*;
import java.sql.SQLException;
import java.util.UUID;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.koikifw.buildsupport.phase4.b1fixture.*;

@org.junit.jupiter.api.condition.EnabledIfSystemProperty(named="koiki.b1.resource-limits.enabled",matches="true")
class B1ProviderReadTest {
    static B1SourceCollector.Fixture fixture;
    @BeforeAll static void start() throws Exception { fixture=new B1SourceCollector.Fixture(); }
    @AfterAll static void finish() { if(fixture!=null) fixture.close(); }
    @ParameterizedTest(name="{0}") @ValueSource(strings={"P01","P02","P03","P04","P05","P06","P07","P08"})
    void providerBranches(String id) throws Exception {
        var c=B1TargetReadTest.Context.create(fixture,!id.equals("P02")&&!id.equals("P03"));
        switch(id) {
            case "P01" -> { assertTrue(c.read().observed());assertEquals(Provider.ACCEPTED,c.read().provider());assertNotNull(c.snapshot.receipt()); }
            case "P02" -> { assertTrue(c.read().observed());assertEquals(Provider.UNKNOWN,c.read().provider()); }
            case "P03" -> {
                fixture.execute("REVOKE SELECT ON public.probe_provider_send FROM b1_writer");
                try { assertThrows(SQLException.class,()->B1SourceCollector.collect(fixture,c.manifest,c.target,c.stop,c.now,false)); }
                finally { fixture.execute("GRANT SELECT ON public.probe_provider_send TO b1_writer"); }
                assertEquals(Provider.UNKNOWN,c.read().provider(),"unavailable receipt is not proof of absence");
            }
            case "P04" -> {
                fixture.execute("INSERT INTO public.probe_provider_send(event_id) VALUES (?)",c.target.event());
                assertFalse(c.read().observed());
                assertThrows(SQLException.class,()->B1SourceCollector.collect(fixture,c.manifest,c.target,c.stop,c.now,false));
            }
            case "P05" -> { assertEquals(c.evidence,B1EvidenceLedger.save(fixture,c.evidence.key(),c.snapshot,c.evidence.subject()));assertTrue(c.read().observed()); }
            case "P06" -> {
                fixture.execute("UPDATE b1_source.binding SET payload_identity='different-payload' WHERE run=?",c.manifest.run());assertFalse(c.read().observed());
                fixture.execute("UPDATE b1_source.binding SET payload_identity=?,recipient_identity='different-recipient' WHERE run=?",c.manifest.payload(),c.manifest.run());assertFalse(c.read().observed());
            }
            case "P07" -> {
                assertFalse(c.readAt(c.snapshot.until()).observed());
                fixture.execute("UPDATE b1_source.binding SET key_until=? WHERE run=?",java.sql.Timestamp.from(c.now),c.manifest.run());assertFalse(c.read().observed());
            }
            case "P08" -> {
                fixture.execute("UPDATE public.probe_provider_send SET event_id=? WHERE idempotency_key=?",UUID.randomUUID(),c.target.event());assertFalse(c.read().observed());
                assertThrows(SQLException.class,()->B1SourceCollector.collect(fixture,c.manifest,c.target,c.stop,c.now,false));
            }
            default -> fail("unmapped case");
        }
        B1ResourceLimits.assertDatabase(fixture.postgres);
    }
}
