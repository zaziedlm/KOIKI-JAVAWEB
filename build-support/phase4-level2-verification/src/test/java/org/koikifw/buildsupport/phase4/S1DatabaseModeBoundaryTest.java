package org.koikifw.buildsupport.phase4;

import static org.junit.jupiter.api.Assertions.*;
import static org.koikifw.buildsupport.phase4.s1fixture.S1CStore.*;
import static org.koikifw.buildsupport.phase4.s1fixture.S1CRecoveryFixture.CURRENT;

import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.koikifw.buildsupport.phase4.s1fixture.*;
import org.koikifw.buildsupport.phase4.s1fixture.S1CRecoveryFixture.*;
import org.springframework.aop.support.AopUtils;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.config.ScheduledTaskHolder;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.session.SessionRepository;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/** C3-C5: actual mode roles/registry/async proxy and context restart, never an OS-crash claim. */
@Timeout(600)
class S1DatabaseModeBoundaryTest {
    private static final S1CDatabase DB=new S1CDatabase();
    private static long started;
    private Path proof;
    @BeforeAll static void start(){started=System.nanoTime();DB.start();}
    @AfterAll static void stop(){try{assertTrue(Duration.ofNanos(System.nanoTime()-started).toSeconds()<600);}finally{DB.close();}}
    @BeforeEach void seed() throws Exception {
        DB.seed();proof=Path.of("target/s1-additional-evidence/s1-c-operational",UUID.randomUUID()+".json").toAbsolutePath();
    }
    @AfterEach void evidence(){System.out.println("S1-C Mode permit="+DB.count("s1c.permit")+" consumption="+DB.count("s1c.consumption")
            +" audit="+DB.count("koiki_audit_event")+" target="+target()+" file="+proof);assertNull(CURRENT.get());}
    private String target(){return DB.observe("SELECT status||':'||completion_attempts FROM event_publication WHERE id='"+PUBLICATION+"'");}
    private String others(){return DB.observe("SELECT string_agg(row_to_json(e)::text,'|' ORDER BY id) FROM event_publication e WHERE id<>'"+PUBLICATION+"'");}
    private static void idle(ConfigurableApplicationContext context) {
        assertTrue(context.getBeansOfType(ApplicationRunner.class).isEmpty());assertTrue(context.getBeansOfType(CommandLineRunner.class).isEmpty());
        assertTrue(context.getBeansOfType(SessionRepository.class).isEmpty());
        context.getBeansOfType(ScheduledTaskHolder.class).forEach((name,holder)->{
            System.out.println("S1-C scheduled="+name+":"+holder.getScheduledTasks());assertTrue(holder.getScheduledTasks().isEmpty());});
        assertTrue(context.getBeansOfType(S1CWebFixture.Actions.class).isEmpty());
    }
    private static void drain(ConfigurableApplicationContext context) {
        try{context.getBean(ThreadPoolTaskExecutor.class).submit(()->assertNull(CURRENT.get())).get(10,TimeUnit.SECONDS);}
        catch(Exception failure){throw new IllegalStateException(failure);}
    }
    @Test void c3_01_observationStartupReadEventAndShutdownInvariant() {
        String before=DB.snapshot();
        S1CWebFixture.run(DB.url(),true,context->{
            assertTrue(context.getBeansOfType(Probe.class).isEmpty());assertTrue(context.getBeansOfType(Listener.class).isEmpty());
            assertEquals(PERMIT,context.getBean(S1CStore.class).read(user("reader"),"fixture-env",PUBLICATION).id());
            context.publishEvent(new Event(EVENT));assertEquals(before,DB.snapshot());
        });assertEquals(before,DB.snapshot());
    }
    @ParameterizedTest @ValueSource(strings={"permit","consumption","publication"})
    void c3_02_observationDatabaseWritesDenied(String table) {
        String before=DB.snapshot();String sql=switch(table){
            case "permit"->"UPDATE s1c.permit SET version=version+1";
            case "consumption"->"INSERT INTO s1c.consumption VALUES ('"+PERMIT+"','"+S1CDatabase.OPERATION+"','s1-c-worker-1',now())";
            default->"UPDATE event_publication SET status='COMPLETED'";};
        assertEquals("42501",DB.refused("s1c_check",sql));assertEquals(before,DB.snapshot());
    }
    @ParameterizedTest @ValueSource(strings={"issue","close"})
    void c3_03_observationUpdateUseCaseAndRouteAbsent(String operation) {
        String before=DB.snapshot();S1CWebFixture.run(DB.url(),true,context->{
            assertTrue(context.getBeansOfType(S1CWebFixture.Actions.class).isEmpty());
            assertTrue(context.getBeansOfType(S1CWebFixture.UpdateController.class).isEmpty());
            var mappings=context.getBean("requestMappingHandlerMapping",RequestMappingHandlerMapping.class).getHandlerMethods();
            assertTrue(mappings.keySet().stream().noneMatch(mapping->mapping.getPatternValues().contains("/s1-test/"+operation)));
            try{var mvc=S1CWebFixture.mvc(context);var browser=S1AuthenticatedPermitWebTest.login(mvc,operation.equals("issue")?"issuer":"closer");
                assertEquals(404,S1AuthenticatedPermitWebTest.request(mvc,browser,operation,true).getResponse().getStatus());}
            catch(Exception failure){throw new IllegalStateException(failure);}
        });assertEquals(before,DB.snapshot());
    }
    @Test void c4_01_explicitGateActualProxyExactPublicationOnly() {
        String others=others();S1CRecoveryFixture.run(DB.url(),context->{
            idle(context);assertTrue(AopUtils.isAopProxy(context.getBean(Listener.class)));
            var probe=context.getBean(Probe.class);assertEquals(0,probe.sends.size());assertEquals("FAILED:1",target());
            context.getBean(Gate.class).execute(proof);
            long deadline=System.nanoTime()+Duration.ofSeconds(30).toNanos();
            while(!target().equals("COMPLETED:2") && System.nanoTime()<deadline)LockSupport.parkNanos(Duration.ofMillis(10).toNanos());
            drain(context);assertEquals("COMPLETED:2",target());assertEquals(1,probe.sends.size());
            assertEquals(context.getBean(Gate.class).operation,probe.sends.getFirst());
            assertTrue(probe.threads.getFirst().startsWith("s1-c-real-worker-"));
            assertEquals(1,DB.count("s1c.consumption"));assertEquals(1,DB.count("koiki_audit_event"));assertEquals(others,others());
        });assertEquals(others,others());
    }
    @ParameterizedTest @ValueSource(strings={"permit","publication","operation","uuid","mode"})
    void c4_02_invalidStartupRefusesWithoutSend(String field) {
        String before=DB.snapshot();String override=switch(field){case "uuid"->"s1c.operation=not-a-uuid";case "mode"->"s1c.mode=web";default->"s1c."+field+"=";};
        S1CRecoveryFixture.runner(DB.url(),override).run(context->assertNotNull(context.getStartupFailure()));assertEquals(before,DB.snapshot());
    }
    @Test void c4_03_ordinaryPublicationInsertDeniedBeforeSend() {
        String before=DB.snapshot();S1CRecoveryFixture.run(DB.url(),context->{
            var failure=assertThrows(RuntimeException.class,()->context.getBean(S1CStore.class).transaction(()->{context.publishEvent(new Event(EVENT));return null;}));
            Throwable cause=failure;while(cause!=null && !(cause instanceof java.sql.SQLException))cause=cause.getCause();
            assertNotNull(cause);assertEquals("42501",((java.sql.SQLException)cause).getSQLState());
            drain(context);assertEquals(0,context.getBean(Probe.class).sends.size());assertEquals(before,DB.snapshot());
        });assertEquals(before,DB.snapshot());
    }
    @Test void c4_04_recoveryStartupWaitAndCloseNeverReplay() {
        String before=DB.snapshot();S1CRecoveryFixture.run(DB.url(),context->{idle(context);drain(context);
            LockSupport.parkNanos(Duration.ofMillis(100).toNanos());assertEquals(0,context.getBean(Probe.class).sends.size());assertEquals(before,DB.snapshot());});
        assertEquals(before,DB.snapshot());
    }
    private void unknown(boolean commit) {
        S1CRecoveryFixture.run(DB.url(),context->{var gate=context.getBean(Gate.class);gate.preserve(proof);var store=context.getBean(S1CStore.class);
            if(commit)store.transaction(()->{store.consume(gate.operation.operation());return null;});
            else store.rollback(()->store.consume(gate.operation.operation()));
            assertEquals(0,context.getBean(Probe.class).sends.size());});
        assertEquals(commit?1:0,DB.count("s1c.consumption"));assertEquals(commit?1:0,DB.count("koiki_audit_event"));
    }
    private void hold(boolean valid) {
        String before=DB.snapshot();S1CRecoveryFixture.run(DB.url(),context->{idle(context);var gate=context.getBean(Gate.class);
            if(valid)assertEquals("HOLD",gate.resume(proof));else assertThrows(Denied.class,()->gate.resume(proof));
            assertThrows(Denied.class,()->gate.execute(proof));drain(context);assertEquals(0,context.getBean(Probe.class).sends.size());
        },"s1c.resume=true");assertEquals(before,DB.snapshot());assertEquals("FAILED:1",target());
        assertEquals("0",DB.observe("SELECT count(*) FROM s1c.permit WHERE closed_at IS NOT NULL"));
    }
    @Test void c5_01_committedUnknownSurvivesContextRestart(){unknown(true);hold(true);assertEquals(1,DB.count("s1c.consumption"));}
    @Test void c5_02_rolledBackUnknownStillHeld(){unknown(false);hold(true);assertEquals(0,DB.count("s1c.consumption"));}
    @ParameterizedTest @ValueSource(booleans={true,false})
    void c5_03_missingEvidenceNeverProvesUnexecuted(boolean commit) throws Exception {unknown(commit);Files.delete(proof);hold(false);}
    @ParameterizedTest @ValueSource(strings={"publication","generation"})
    void c5_04_targetOrWorkerGenerationMismatchHeld(String field) throws Exception {
        unknown(false);String json=Files.readString(proof);Files.writeString(proof,json.replace(field.equals("publication")?PUBLICATION.toString():"s1-c-worker-1",field.equals("publication")?S1CDatabase.OTHER.toString():"s1-c-worker-2"));hold(false);
    }
    @Test void c5_05_unknownWithoutConsumptionBlocksNextPermit() {
        unknown(false);hold(true);String before=DB.snapshot();S1CWebFixture.run(DB.url(),false,context->{
            var failure=assertThrows(RuntimeException.class,()->context.getBean(S1CStore.class).issue(UUID.fromString("00000000-0000-0000-0000-000000000002"),user("issuer"),"fixture-env",PUBLICATION));
            Throwable cause=failure;while(cause!=null && !(cause instanceof java.sql.SQLException))cause=cause.getCause();
            assertNotNull(cause);assertEquals("23505",((java.sql.SQLException)cause).getSQLState());
            assertTrue(cause.getMessage().contains("s1c_unresolved_target"));});
        assertEquals(0,DB.count("s1c.consumption"));assertEquals(1,DB.count("s1c.permit"));assertEquals(before,DB.snapshot());
    }
}
