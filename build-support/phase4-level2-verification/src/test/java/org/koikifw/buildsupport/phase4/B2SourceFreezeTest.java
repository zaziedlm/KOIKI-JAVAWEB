package org.koikifw.buildsupport.phase4;

import static org.junit.jupiter.api.Assertions.*;
import static org.koikifw.buildsupport.phase4.b1fixture.B1ReadContract.*;
import java.sql.*;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.koikifw.buildsupport.phase4.b1fixture.*;
import org.koikifw.buildsupport.phase4.b2fixture.*;

@org.junit.jupiter.api.condition.EnabledIfSystemProperty(named="koiki.b2.resource-limits.enabled",matches="true")
class B2SourceFreezeTest {
    private static Instant now() {return Instant.now().truncatedTo(ChronoUnit.MILLIS);}
    private static Snapshot seed(B1SourceCollector.Fixture f,Manifest m,Instant n) throws Exception {
        Target t=f.seed(m,ENVIRONMENT,n,false);
        Stop stop=new Stop(m.run(),1,m.source(),true,true,true,n,n.plusSeconds(60));
        return B1SourceCollector.collect(f,m,t,stop,n,true);
    }
    @Test void F01RealChildEndsBeforeFreeze() throws Exception {
        try(var owner=new B2FrozenSourceCoordinator()) {
            owner.start(false);owner.prepareFrozen(UUID.randomUUID().toString());
            assertTrue(owner.children.entries().stream().noneMatch(e->e.process().isAlive()));
            assertTrue(owner.protocol.read(owner.manifest,owner.snapshot,now()));
            B1ResourceLimits.assertDatabase(owner.fixture.postgres);
        }
    }
    @Test void F02LiveChildCannotFreeze() throws Exception {
        try(var owner=new B2FrozenSourceCoordinator()) {
            owner.start(false);Instant n=now();Target t=owner.fixture.discover(owner.manifest);
            Stop live=owner.children.observe(n);
            Snapshot s=B1SourceCollector.collect(owner.fixture,owner.manifest,t,live,n,true);
            var protocol=new B2FrozenSourceProtocol(owner.fixture,UUID.randomUUID().toString());
            assertThrows(IllegalStateException.class,()->protocol.freeze(owner.manifest,s,live,false,n));
            assertFalse(protocol.frozen());assertTrue(owner.children.entries().getFirst().process().isAlive());
        }
    }
    @Test void F03WriterConnectionMustHaveEnded() throws Exception {
        try(var f=new B1SourceCollector.Fixture()) {
            Instant n=now();Manifest m=f.prepare(n,ENVIRONMENT);Snapshot s=seed(f,m,n);
            var protocol=new B2FrozenSourceProtocol(f,UUID.randomUUID().toString());
            try(Connection held=f.writer()) {
                assertFalse(held.isClosed());
                assertThrows(IllegalStateException.class,()->protocol.freeze(m,s,s.stop(),true,n));
                assertFalse(protocol.frozen());
            }
        }
    }
    @Test void F04UnknownConnectionRejectsFreeze() throws Exception {
        try(var f=new B1SourceCollector.Fixture()) {
            Instant n=now();Manifest m=f.prepare(n,ENVIRONMENT);Snapshot s=seed(f,m,n);
            var protocol=new B2FrozenSourceProtocol(f,UUID.randomUUID().toString());
            try(Connection unexpected=f.reader()) {
                assertFalse(unexpected.isClosed());
                assertThrows(IllegalStateException.class,()->protocol.freeze(m,s,s.stop(),true,n));
                assertFalse(protocol.frozen());
            }
        }
    }
    @Test void F05DisabledWriterCannotReconnect() throws Exception {
        try(var f=new B1SourceCollector.Fixture()) {
            Instant n=now();Manifest m=f.prepare(n,ENVIRONMENT);Snapshot s=seed(f,m,n);
            var protocol=new B2FrozenSourceProtocol(f,UUID.randomUUID().toString());
            protocol.freeze(m,s,s.stop(),true,n);
            assertThrows(SQLException.class,f::writer);
            assertTrue(protocol.read(m,s,now()));
        }
    }
    @Test void F06ReaderCannotUpdateOrCreate() throws Exception {
        try(var f=new B1SourceCollector.Fixture()) {
            Instant n=now();Manifest m=f.prepare(n,ENVIRONMENT);Snapshot s=seed(f,m,n);
            var protocol=new B2FrozenSourceProtocol(f,UUID.randomUUID().toString());
            protocol.freeze(m,s,s.stop(),true,n);
            try(Connection c=protocol.reader();Statement q=c.createStatement()) {
                q.setQueryTimeout(10);
                assertThrows(SQLException.class,()->q.execute("UPDATE public.event_publication SET completion_attempts=completion_attempts+1"));
                assertThrows(SQLException.class,()->q.execute("CREATE TABLE public.b2_unapproved(id integer)"));
                assertThrows(SQLException.class,()->q.execute("INSERT INTO b2_source.freeze(run) VALUES ('00000000-0000-0000-0000-000000000001')"));
            }
            assertTrue(protocol.read(m,s,now()));
        }
    }
    @Test void F07NormalLaunchStaysClosed() throws Exception {
        try(var owner=new B2FrozenSourceCoordinator()) {
            owner.start(false);owner.prepareFrozen(UUID.randomUUID().toString());
            assertThrows(IllegalStateException.class,()->owner.start(false));
            assertTrue(owner.children.entries().stream().noneMatch(e->e.process().isAlive()));
            assertTrue(owner.protocol.read(owner.manifest,owner.snapshot,now()));
        }
    }
    @Test void F08WriterAuthorityAndOwnershipAreVerified() throws Exception {
        try(var f=new B1SourceCollector.Fixture()) {
            Instant n=now();Manifest m=f.prepare(n,ENVIRONMENT);Snapshot s=seed(f,m,n);
            var protocol=new B2FrozenSourceProtocol(f,UUID.randomUUID().toString());
            f.execute("GRANT b1_fixture TO b1_writer");
            assertThrows(IllegalStateException.class,()->protocol.freeze(m,s,s.stop(),true,n));
            f.execute("REVOKE b1_fixture FROM b1_writer");
            f.execute("ALTER TABLE public.probe_approval OWNER TO "+f.postgres.getUsername());
            assertThrows(IllegalStateException.class,()->protocol.freeze(m,s,s.stop(),true,n));
            assertFalse(protocol.frozen());
        }
    }
    @Test void F09FailedFreezeCannotYieldReady() throws Exception {
        try(var f=new B1SourceCollector.Fixture()) {
            Instant n=now();Manifest m=f.prepare(n,ENVIRONMENT);Snapshot s=seed(f,m,n);
            var protocol=new B2FrozenSourceProtocol(f,UUID.randomUUID().toString());
            f.execute("UPDATE public.event_publication SET completion_attempts=completion_attempts+1 WHERE id=?",s.target().publication());
            assertThrows(IllegalStateException.class,()->protocol.freeze(m,s,s.stop(),true,n));
            assertFalse(protocol.read(m,s,n));
            try(Connection c=f.admin();Statement q=c.createStatement();ResultSet r=q.executeQuery("SELECT count(*) FROM b2_source.freeze")) {
                r.next();assertEquals(0,r.getInt(1));
            }
        }
    }
}
