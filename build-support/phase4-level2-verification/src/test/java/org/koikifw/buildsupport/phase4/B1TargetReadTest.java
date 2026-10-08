package org.koikifw.buildsupport.phase4;

import static org.junit.jupiter.api.Assertions.*;
import static org.koikifw.buildsupport.phase4.b1fixture.B1ReadContract.*;

import java.sql.SQLException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.koikifw.buildsupport.phase4.b1fixture.*;

/** Focused real-DB branches. Seeded rows are distinguished from the ordinary-child IT. */
@org.junit.jupiter.api.condition.EnabledIfSystemProperty(named="koiki.b1.resource-limits.enabled",matches="true")
class B1TargetReadTest {
    static B1SourceCollector.Fixture fixture;
    @BeforeAll static void start() throws Exception { fixture=new B1SourceCollector.Fixture(); }
    @AfterAll static void finish() { if(fixture!=null) fixture.close(); }
    @ParameterizedTest(name="{0}")
    @ValueSource(strings={"T01","T02","T03","T04","T05","T06","T07","T08","T09","T10"})
    void targetBranches(String id) throws Exception {
        Context c=Context.create(fixture,false);
        switch(id) {
            case "T01" -> { assertTrue(c.read().observed()); assertEquals(Provider.UNKNOWN,c.read().provider()); }
            case "T02" -> {
                UUID other=UUID.randomUUID();
                fixture.execute("INSERT INTO public.event_publication(id,listener_id,event_type,serialized_event,publication_date,status,completion_attempts) SELECT ?,listener_id,event_type,serialized_event,publication_date,status,completion_attempts FROM public.event_publication WHERE id=?",other,c.target.publication());
                assertThrows(SQLException.class,()->fixture.seal(c.manifest,new Target(ENVIRONMENT,other,c.target.event(),EVENT_TYPE,c.target.listener(),1)));
                assertTrue(c.read().observed(),"second publication cannot replace sealed target");
            }
            case "T03" -> { fixture.execute("UPDATE b1_source.binding SET environment='another-environment' WHERE run=?",c.manifest.run()); assertFalse(c.read().observed()); }
            case "T04" -> {
                fixture.execute("UPDATE public.event_publication SET event_type='other-event' WHERE id=?",c.target.publication()); assertFalse(c.read().observed());
                fixture.execute("UPDATE public.event_publication SET event_type=?,serialized_event=? WHERE id=?",EVENT_TYPE,"{\"eventId\":\""+UUID.randomUUID()+"\"}",c.target.publication()); assertFalse(c.read().observed());
                assertThrows(IllegalArgumentException.class,()->B1SourceCollector.eventId("{\"eventId\":\""+c.target.event()+"\",\"extra\":true}"));
            }
            case "T05" -> { fixture.execute("UPDATE public.event_publication SET listener_id='other-listener' WHERE id=?",c.target.publication()); assertFalse(c.read().observed()); }
            case "T06" -> { fixture.execute("UPDATE public.event_publication SET completion_attempts=2 WHERE id=?",c.target.publication()); assertFalse(c.read().observed()); }
            case "T07" -> { fixture.execute("DELETE FROM public.event_publication WHERE id=?",c.target.publication()); assertFalse(c.read().observed()); }
            case "T08" -> {
                fixture.execute("UPDATE public.event_publication SET status='CANCELLED' WHERE id=?",c.target.publication()); assertFalse(c.read().observed());
                fixture.execute("UPDATE public.event_publication SET status='PROCESSING' WHERE id=?",c.target.publication());
                B1SourceCollector.collect(fixture,c.manifest,c.target,c.stop,c.now,false);
                assertFalse(c.read().observed(),"old observation generation rejected");
            }
            case "T09" -> {
                assertFalse(c.readAt(c.now.minusSeconds(1)).observed());
                assertFalse(c.readAt(c.snapshot.until()).observed());
            }
            case "T10" -> {
                assertFalse(c.readAt(c.now.plusSeconds(61)).observed());
                fixture.execute("REVOKE SELECT ON b1_read.target FROM b1_reader");
                try { assertEquals("SUPPLY_UNAVAILABLE_OR_INVALID",c.read().reason()); }
                finally { fixture.execute("GRANT SELECT ON b1_read.target TO b1_reader"); }
            }
            default -> fail("unmapped case");
        }
        B1ResourceLimits.assertDatabase(fixture.postgres);
    }
    /** Shared nested test setup; not another helper or distributed artifact. */
    static final class Context {
        final B1SourceCollector.Fixture fixture;
        final Instant now;
        final Manifest manifest;
        final Target target;
        final Stop stop;
        final Snapshot snapshot;
        final Evidence evidence;
        Context(B1SourceCollector.Fixture fixture,Instant now,Manifest manifest,Target target,Stop stop,Snapshot snapshot,Evidence evidence) {
            this.fixture=fixture;this.now=now;this.manifest=manifest;this.target=target;this.stop=stop;this.snapshot=snapshot;this.evidence=evidence;
        }
        static Context create(B1SourceCollector.Fixture fixture,boolean accepted) throws Exception {
            fixture.reset();
            Instant now=Instant.now().truncatedTo(ChronoUnit.MILLIS);
            Manifest manifest=fixture.prepare(now,ENVIRONMENT);
            Target target=fixture.seed(manifest,ENVIRONMENT,now,accepted);
            // Focused DB tests use an explicit stop model. S/IT must obtain real process observations.
            Stop stop=new Stop(manifest.run(),1,SOURCE,true,true,true,now,now.plusSeconds(60));
            Snapshot snapshot=B1SourceCollector.collect(fixture,manifest,target,stop,now,false);
            Evidence evidence=B1EvidenceLedger.save(fixture,new EvidenceKey(ENVIRONMENT,UUID.randomUUID(),UUID.randomUUID(),1),snapshot,"test-collector");
            return new Context(fixture,now,manifest,target,stop,snapshot,evidence);
        }
        Read read() { return readAt(now); }
        Read readAt(Instant time) { return B1JdbcReadClient.read(fixture,snapshot,evidence,stop,time); }
    }
}
