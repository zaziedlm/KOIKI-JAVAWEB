package org.koikifw.buildsupport.phase4;

import static org.junit.jupiter.api.Assertions.*;
import static org.koikifw.buildsupport.phase4.b1fixture.B1ReadContract.*;
import java.sql.*;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.*;
import org.koikifw.buildsupport.phase4.b1fixture.*;
import org.koikifw.buildsupport.phase4.b2fixture.*;

@org.junit.jupiter.api.condition.EnabledIfSystemProperty(named="koiki.b2.resource-limits.enabled",matches="true")
class B2FrozenSourceProtocolTest {
    B1SourceCollector.Fixture fixture;
    Manifest manifest;
    Snapshot snapshot;
    B2FrozenSourceProtocol protocol;
    Instant now;
    @BeforeEach void prepare() throws Exception {
        fixture=new B1SourceCollector.Fixture();now=Instant.now().truncatedTo(ChronoUnit.MILLIS);
        try {
            manifest=fixture.prepare(now,ENVIRONMENT);Target t=fixture.seed(manifest,ENVIRONMENT,now,false);
            Stop stop=new Stop(manifest.run(),1,manifest.source(),true,true,true,now,now.plusSeconds(60));
            snapshot=B1SourceCollector.collect(fixture,manifest,t,stop,now,true);
            protocol=new B2FrozenSourceProtocol(fixture,UUID.randomUUID().toString());
        } catch(Exception|AssertionError failure) {fixture.close();throw failure;}
    }
    @AfterEach void close() {if(fixture!=null) fixture.close();}
    private void freeze() throws Exception {protocol.freeze(manifest,snapshot,snapshot.stop(),true,now);}
    private Snapshot expected(Target t,long revision,String fingerprint,String key,String payload,String recipient) {
        return new Snapshot(t,snapshot.run(),revision,snapshot.source(),fingerprint,key,payload,recipient,snapshot.status(),
            snapshot.provider(),snapshot.receipt(),snapshot.stop(),snapshot.observed(),snapshot.until(),snapshot.keyUntil());
    }
    @Test void P01FrozenViewIsUniqueAndReadable() throws Exception {
        freeze();assertTrue(protocol.read(manifest,snapshot,now));
        try(Connection c=protocol.reader();Statement q=c.createStatement();ResultSet r=q.executeQuery("SELECT count(*) FROM b2_read.frozen_target")) {
            r.next();assertEquals(1,r.getInt(1));
        }
    }
    @Test void P02VersionRunAndSourceMustMatch() throws Exception {
        String definition;
        try(Connection c=fixture.admin();Statement q=c.createStatement();ResultSet r=q.executeQuery("SELECT pg_get_viewdef('b2_read.frozen_target'::regclass,true)")) {
            r.next();definition=r.getString(1);
        }
        assertTrue(definition.contains("f.protocol_version"));
        fixture.execute("CREATE OR REPLACE VIEW b2_read.frozen_target AS "+definition.replace("f.protocol_version","2 AS protocol_version"));
        freeze();assertFalse(protocol.read(manifest,snapshot,now));
        Manifest otherRun=new Manifest(UUID.randomUUID(),manifest.event(),manifest.key(),manifest.payload(),manifest.recipient(),manifest.source(),manifest.listener(),manifest.jarHash(),manifest.keyUntil());
        assertFalse(protocol.read(otherRun,snapshot,now));
        Manifest otherSource=new Manifest(manifest.run(),manifest.event(),manifest.key(),manifest.payload(),manifest.recipient(),"other-source",manifest.listener(),manifest.jarHash(),manifest.keyUntil());
        assertFalse(protocol.read(otherSource,snapshot,now));
    }
    @Test void P03EveryTargetDimensionIsBound() throws Exception {
        freeze();Target t=snapshot.target();
        for(Target other:new Target[]{new Target("other",t.publication(),t.event(),t.eventType(),t.listener(),t.attempt()),
            new Target(t.environment(),UUID.randomUUID(),t.event(),t.eventType(),t.listener(),t.attempt()),
            new Target(t.environment(),t.publication(),UUID.randomUUID(),t.eventType(),t.listener(),t.attempt()),
            new Target(t.environment(),t.publication(),t.event(),"other-type",t.listener(),t.attempt()),
            new Target(t.environment(),t.publication(),t.event(),t.eventType(),"other-listener",t.attempt()),
            new Target(t.environment(),t.publication(),t.event(),t.eventType(),t.listener(),t.attempt()+1)}) {
            assertFalse(protocol.read(manifest,expected(other,snapshot.revision(),snapshot.sourceFingerprint(),snapshot.key(),snapshot.payload(),snapshot.recipient()),now));
        }
    }
    @Test void P04RevisionAndFingerprintAreStrict() throws Exception {
        freeze();
        assertFalse(protocol.read(manifest,expected(snapshot.target(),snapshot.revision()+1,snapshot.sourceFingerprint(),snapshot.key(),snapshot.payload(),snapshot.recipient()),now));
        assertFalse(protocol.read(manifest,expected(snapshot.target(),snapshot.revision(),"other-fingerprint",snapshot.key(),snapshot.payload(),snapshot.recipient()),now));
    }
    @Test void P05AllNotificationBindingsAreStrict() throws Exception {
        freeze();
        for(String[] binding:new String[][]{{"other-key",snapshot.payload(),snapshot.recipient()},
            {snapshot.key(),"other-payload",snapshot.recipient()},{snapshot.key(),snapshot.payload(),"other-recipient"}})
            assertFalse(protocol.read(manifest,expected(snapshot.target(),snapshot.revision(),snapshot.sourceFingerprint(),binding[0],binding[1],binding[2]),now));
    }
    @Test void P06AcceptedAndUnknownNeverAuthorizeIssue() throws Exception {
        Snapshot unknown=B1SourceCollector.collect(fixture,manifest,snapshot.target(),snapshot.stop(),now,false);
        assertEquals(Provider.UNKNOWN,unknown.provider());
        assertThrows(IllegalStateException.class,()->protocol.freeze(manifest,unknown,unknown.stop(),true,now));
        fixture.execute("INSERT INTO public.probe_provider_send(event_id,idempotency_key) VALUES (?,?)",manifest.event(),manifest.event());
        Snapshot accepted=B1SourceCollector.collect(fixture,manifest,snapshot.target(),snapshot.stop(),now,true);
        assertEquals(Provider.ACCEPTED,accepted.provider());
        assertThrows(IllegalStateException.class,()->protocol.freeze(manifest,accepted,accepted.stop(),true,now));
        assertFalse(protocol.frozen());
    }
    @Test void P07FutureClockAndExactDeadlineRejectNewAdmission() throws Exception {
        freeze();assertFalse(protocol.read(manifest,snapshot,now.minusSeconds(2)));
        assertFalse(protocol.read(manifest,snapshot,now.plusSeconds(60)));
        assertFalse(protocol.read(manifest,snapshot,manifest.keyUntil()));
        assertTrue(protocol.frozen(),"clock expiry does not reopen writers");
    }
    @Test void P08FreezeCertificateIsImmutable() throws Exception {
        freeze();
        assertThrows(SQLException.class,()->fixture.execute("UPDATE b2_source.freeze SET revision=revision+1"));
        assertThrows(SQLException.class,()->fixture.execute("DELETE FROM b2_source.freeze"));
        assertTrue(protocol.read(manifest,snapshot,now));
    }
    @Test void P09ReaderCannotSeeRawTablesOrRecoverMissingSupply() throws Exception {
        freeze();
        try(Connection c=protocol.reader();Statement q=c.createStatement()) {
            q.setQueryTimeout(10);
            assertThrows(SQLException.class,()->q.executeQuery("SELECT * FROM public.event_publication"));
            assertThrows(SQLException.class,()->q.executeQuery("SELECT * FROM b2_source.freeze"));
        }
        fixture.close();assertFalse(protocol.read(manifest,snapshot,now));
    }
}
