package org.koikifw.buildsupport.phase4;

import static org.junit.jupiter.api.Assertions.*;
import static org.koikifw.buildsupport.phase4.s1fixture.S1IdentityAuditFixture.*;

import java.sql.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.koikifw.audit.AuditRecordingException;
import org.koikifw.buildsupport.phase4.s1fixture.S1IdentityAuditFixture;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** B: real Public Query/Recorders; restricted DB roles; no authentication/provider claim. */
@Timeout(600)
class S1IdentityAuditBoundaryTest {
    private static final UUID PERMIT=UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final String IMAGE="postgres@sha256:18cfe3ef5e6815560c98237d6216d1e5119702fb0f3894c8785dd58b8bbe5d73";
    private static final PostgreSQLContainer POSTGRES=new PostgreSQLContainer(IMAGE)
            .withCommand("postgres","-c","max_connections=16")
            .withCreateContainerCmdModifier(command -> command.getHostConfig()
                    .withMemory(1024L*1024*1024).withNanoCPUs(1_000_000_000L));
    private S1IdentityAuditFixture runtime;
    private static long started;

    @BeforeAll static void database() throws Exception {
        started=System.nanoTime();
        POSTGRES.start();
        try (var connection=admin()) {
            for (String name : List.of("db/migration/koiki/V2026090300__create_koiki_audit.sql",
                    "db/migration/koiki/V2026090301__create_koiki_identity.sql")) {
                var resource=new ClassPathResource(name);
                assertTrue(resource.getURL().toString().startsWith("jar:"),"Use actual artifact SQL");
                System.out.println("S1-B migration="+resource.getURL());
                ScriptUtils.executeSqlScript(connection,resource);
            }
            ScriptUtils.executeSqlScript(connection,new ClassPathResource("s1-additional/permit-identity-audit.sql"));
            try(var statement=connection.createStatement();var rows=statement.executeQuery("SHOW max_connections")) {
                assertTrue(rows.next()); assertEquals("16",rows.getString(1));
            }
        } catch (Exception | Error failure) { POSTGRES.stop(); throw failure; }
        System.out.println("S1-B container="+POSTGRES.getContainerId()+" image="+IMAGE);
        assertEquals(1024L*1024*1024,POSTGRES.getContainerInfo().getHostConfig().getMemory());
        assertEquals(1_000_000_000L,POSTGRES.getContainerInfo().getHostConfig().getNanoCPUs());
    }
    @AfterAll static void stop() {
        try { assertTrue(java.time.Duration.ofNanos(System.nanoTime()-started).toSeconds()<600,"B class ceiling"); }
        finally { POSTGRES.stop(); }
    }
    @BeforeEach void seed() throws Exception {
        assertTrue(java.time.Duration.ofNanos(System.nanoTime()-started).toSeconds()<600,"B class ceiling");
        adminSql("GRANT INSERT ON koiki_audit_event TO s1b_web,s1b_worker",
                "GRANT SELECT ON koiki_user TO s1b_web,s1b_worker",
                "TRUNCATE s1b.consumption,s1b.permit,koiki_audit_event",
                "TRUNCATE koiki_user,koiki_role,koiki_permission CASCADE",
                "INSERT INTO koiki_user(user_id,email,canonical_email,status) VALUES ('"+USER+"','fixture@example.test','fixture@example.test','ACTIVE')",
                "INSERT INTO koiki_role(role_id,role_code) VALUES ('00000000-0000-0000-0000-000000000401','S1_TEST_ROLE')",
                "INSERT INTO koiki_user_role VALUES ('"+USER+"','00000000-0000-0000-0000-000000000401')");
        int index=0;
        for (String code:List.of("ISSUE","READ","EXECUTE","CLOSE")) {
            String id="00000000-0000-0000-0000-00000000050"+(++index);
            adminSql("INSERT INTO koiki_permission(permission_id,permission_code) VALUES ('"+id+"','S1_TEST_"+code+"')",
                    "INSERT INTO koiki_role_permission VALUES ('00000000-0000-0000-0000-000000000401','"+id+"')");
        }
        try(var issuer=new S1IdentityAuditFixture(POSTGRES.getJdbcUrl(),"s1b_web")) { issuer.issue(PERMIT); }
        // Initial preparation is not the operation under test; retain no baseline audit.
        adminSql("TRUNCATE koiki_audit_event");
    }
    @AfterEach void cleanup() throws Exception {
        try {
            System.out.println("S1-B after permit="+number("SELECT count(*) FROM s1b.permit")
                    +" consumption="+number("SELECT count(*) FROM s1b.consumption")
                    +" audit="+number("SELECT count(*) FROM koiki_audit_event")
                    +" sends="+(runtime==null?0:runtime.sends));
            assertTrue(number("SELECT count(*) FROM pg_stat_activity WHERE datname=current_database()")<=8);
            assertEquals(0,number("SELECT count(*) FROM koiki_audit_event WHERE actor_type='USER' AND actor_id<>'"+USER+"'"));
            assertEquals(0,number("SELECT count(*) FROM koiki_audit_event WHERE actor_type='ANONYMOUS' AND actor_id IS NOT NULL"));
            assertEquals(0,number("SELECT count(*) FROM koiki_audit_event WHERE audit_type='SECURITY' AND resource_id IS NOT NULL"));
        } finally { if(runtime!=null) { runtime.close(); runtime=null; } }
    }
    private S1IdentityAuditFixture role(String role) {
        if(runtime!=null) runtime.close();
        runtime=new S1IdentityAuditFixture(POSTGRES.getJdbcUrl(),role); return runtime;
    }
    private S1IdentityAuditFixture worker() { return role("s1b_worker"); }
    private String execute() { return runtime.execute(PERMIT,Snapshot.matching(),()->{}); }
    private void denied() throws Exception {
        assertEquals("DENIED",execute()); assertEquals(0,number("SELECT count(*) FROM s1b.consumption"));
        assertEquals(0,runtime.sends);
    }
    private void invalidate(String mode) throws SQLException {
        switch(mode) {
            case "disabled" -> adminSql("UPDATE koiki_user SET status='DISABLED',version=version+1 WHERE user_id='"+USER+"'");
            case "noPermission" -> adminSql("DELETE FROM koiki_role_permission WHERE permission_id IN (SELECT permission_id FROM koiki_permission WHERE permission_code='S1_TEST_EXECUTE')");
            case "missing" -> adminSql("DELETE FROM koiki_user_role WHERE user_id='"+USER+"'","DELETE FROM koiki_user WHERE user_id='"+USER+"'");
            default -> throw new IllegalArgumentException(mode);
        }
    }
    @Test void b1_01_activeActualQueryConsumes() throws Exception {
        worker(); assertTrue(runtime.authorized()); assertEquals("SENT",execute());
        assertEquals(1,number("SELECT count(*) FROM s1b.consumption")); assertEquals(1,runtime.sends);
    }
    @ParameterizedTest @ValueSource(strings={"disabled","noPermission","missing"})
    void b1_02_currentIdentityDenial(String mode) throws Exception {
        worker(); invalidate(mode); denied();
        assertEquals(mode.equals("missing")?"ANONYMOUS":"USER",observer("SELECT actor_type FROM koiki_audit_event"));
    }
    @Test void b1_03_actualQueryFailure() throws Exception {
        worker(); adminSql("REVOKE SELECT ON koiki_user FROM s1b_worker");
        assertSqlState("s1b_worker","SELECT user_id FROM koiki_user","42501"); denied();
        assertEquals("ANONYMOUS",observer("SELECT actor_type FROM koiki_audit_event"));
    }
    @ParameterizedTest @ValueSource(strings={"before","equal","after"})
    void b2_01_expiryBoundary(String point) throws Exception {
        worker(); runtime.clock.set(point.equals("before")?EXPIRES.minusNanos(1000):point.equals("equal")?EXPIRES:EXPIRES.plusNanos(1000));
        if(point.equals("before")) { assertEquals("SENT",execute()); assertEquals(1,runtime.sends); }
        else denied();
    }
    @ParameterizedTest @ValueSource(strings={"environment","publication","event","listener","attempt"})
    void b2_02_targetMismatch(String field) throws Exception {
        worker(); var match=Snapshot.matching();
        if(field.equals("attempt")) assertSqlState("s1b_worker","UPDATE s1b.permit SET expected_attempt=2","42501");
        var input=new Snapshot(field.equals("environment")?"outside-env":match.environment(),
                field.equals("publication")?UUID.randomUUID():match.publication(),
                field.equals("event")?UUID.randomUUID():match.event(),
                field.equals("listener")?"outside-listener":match.listener(),field.equals("attempt")?2:1);
        assertEquals("DENIED",runtime.execute(PERMIT,input,()->{}));
        assertEquals(0,number("SELECT count(*) FROM s1b.consumption")); assertEquals(0,runtime.sends);
    }
    @ParameterizedTest @ValueSource(strings={"closed","consumed"})
    void b2_03_closedOrConsumed(String state) throws Exception {
        if(state.equals("closed")) { role("s1b_web").transaction(()->{runtime.closeInside(PERMIT);return null;}); }
        worker();
        if(state.equals("consumed")) { runtime.transaction(()->{runtime.consumeInside(PERMIT,Snapshot.matching());return null;}); }
        int baseline=number("SELECT count(*) FROM s1b.consumption");
        assertEquals("DENIED",execute()); assertEquals(baseline,number("SELECT count(*) FROM s1b.consumption")); assertEquals(0,runtime.sends);
    }
    @Test void b2_04_scopeReadDoesNotExposeExistence() throws Exception {
        worker();
        for(UUID id:List.of(PERMIT,UUID.randomUUID())) {
            var failure=assertThrows(Denied.class,()->runtime.transaction(()->runtime.readScoped(id,"outside-env",PUBLICATION)));
            assertEquals("S1_TEST_DENIED",failure.getMessage());
        }
        assertEquals("DENIED",runtime.execute(PERMIT,new Snapshot("outside-env",PUBLICATION,EVENT,"fixture-listener",1),()->{}));
        assertEquals(0,number("SELECT count(*) FROM s1b.consumption")); assertEquals(0,runtime.sends);
    }
    @ParameterizedTest @ValueSource(strings={"ISSUED","CONSUMED","CLOSED"})
    void b3_01_businessAndAuditVisibility(String action) throws Exception {
        if(action.equals("ISSUED")) adminSql("TRUNCATE s1b.consumption,s1b.permit");
        role(action.equals("CONSUMED")?"s1b_worker":"s1b_web");
        String[] transaction=new String[1];
        runtime.transaction(()->{
            change(action); transaction[0]=runtime.scalar("SELECT txid_current()");
            assertEquals(transaction[0],runtime.capture.auditTransaction);
            try {
                assertEquals(0,number("SELECT count(*) FROM koiki_audit_event"));
                assertEquals(action.equals("ISSUED")?0:1,number("SELECT count(*) FROM s1b.permit"));
                assertEquals(0,number("SELECT count(*) FROM s1b.consumption"));
                assertEquals(0,number("SELECT count(*) FROM s1b.permit WHERE closed_at IS NOT NULL"));
            } catch(SQLException failure) { throw new IllegalStateException(failure); }
            return null;
        });
        assertEquals(1,number("SELECT count(*) FROM koiki_audit_event"));
        assertEquals("BUSINESS|USER|"+USER+"|S1_TEST_PERMIT|"+PERMIT+"|S1_TEST_"+action+"|SUCCESS",
                observer("SELECT audit_type||'|'||actor_type||'|'||actor_id||'|'||resource_type||'|'||resource_id||'|'||action||'|'||result FROM koiki_audit_event"));
        assertEquals(transaction[0],observer("SELECT xmin::text FROM koiki_audit_event"));
        String businessTable=action.equals("CONSUMED")?"s1b.consumption":"s1b.permit";
        assertEquals(transaction[0],observer("SELECT xmin::text FROM "+businessTable));
        assertEquals(action.equals("CONSUMED")?1:0,number("SELECT count(*) FROM s1b.consumption"));
        assertEquals(action.equals("CLOSED")?1:0,number("SELECT count(*) FROM s1b.permit WHERE closed_at IS NOT NULL"));
    }
    private void change(String action) {
        switch(action) {
            case "ISSUED" -> runtime.issueInside(PERMIT);
            case "CONSUMED" -> runtime.consumeInside(PERMIT,Snapshot.matching());
            case "CLOSED" -> runtime.closeInside(PERMIT);
            default -> throw new IllegalArgumentException(action);
        }
    }
    @Test void b3_02_businessRequiresTransaction() throws Exception {
        worker(); assertThrows(AuditRecordingException.class,()->runtime.business.record(runtime.businessEvent(PERMIT,"S1_TEST_CONSUMED")));
        assertEquals(0,number("SELECT count(*) FROM koiki_audit_event"));
    }
    @ParameterizedTest @ValueSource(strings={"ISSUED","CONSUMED","CLOSED"})
    void b4_01_businessAuditFaultRollsBack(String action) throws Exception {
        if(action.equals("ISSUED")) adminSql("TRUNCATE s1b.consumption,s1b.permit");
        String login=action.equals("CONSUMED")?"s1b_worker":"s1b_web"; role(login);
        adminSql("REVOKE INSERT ON koiki_audit_event FROM "+login);
        assertSqlState(login,"INSERT INTO koiki_audit_event(event_id) VALUES (gen_random_uuid())","42501");
        assertThrows(AuditRecordingException.class,()->runtime.transaction(()->{change(action);return null;}));
        if(action.equals("CONSUMED")) assertEquals("DENIED",execute(),"Full orchestration must also fail closed after audit failure");
        assertEquals(action.equals("ISSUED")?0:1,number("SELECT count(*) FROM s1b.permit"));
        assertEquals(0,number("SELECT count(*) FROM s1b.permit WHERE closed_at IS NOT NULL"));
        assertEquals(0,number("SELECT count(*) FROM s1b.consumption"));
        assertEquals(0,number("SELECT count(*) FROM koiki_audit_event")); assertEquals(0,runtime.sends);
    }
    @ParameterizedTest @ValueSource(strings={"none","rollback"})
    void b5_01_securityIndependentTransaction(String outer) throws Exception {
        worker(); invalidate("noPermission");
        if(outer.equals("none")) denied();
        else runtime.rollback(()->{
            runtime.scalar("SELECT txid_current()");
            assertThrows(Denied.class,()->runtime.consumeInside(PERMIT,Snapshot.matching()));
            runtime.denyAudit();
        });
        assertEquals(1,number("SELECT count(*) FROM koiki_audit_event WHERE audit_type='SECURITY' AND result='FAILURE'"));
        assertEquals("0",observer("SELECT version::text FROM s1b.permit"));
        assertEquals(0,number("SELECT count(*) FROM s1b.consumption")); assertEquals(0,runtime.sends);
    }
    @Test void b5_02_securityAuditFaultStillDenies() throws Exception {
        worker(); invalidate("disabled"); adminSql("REVOKE INSERT ON koiki_audit_event FROM s1b_worker");
        assertSqlState("s1b_worker","INSERT INTO koiki_audit_event(event_id) VALUES (gen_random_uuid())","42501");
        denied(); assertEquals(1,runtime.auditFailures); assertEquals(0,number("SELECT count(*) FROM koiki_audit_event"));
    }
    @Test void b5_03_requiresNewUsesDifferentBoundConnection() throws Exception {
        worker(); invalidate("noPermission");
        runtime.rollback(()->{
            String outerPid=runtime.scalar("SELECT pg_backend_pid()"),outerTx=runtime.scalar("SELECT txid_current()");
            assertThrows(Denied.class,()->runtime.consumeInside(PERMIT,Snapshot.matching()));
            runtime.denyAudit();
            assertNotEquals(outerPid,runtime.capture.auditPid); assertNotEquals(outerTx,runtime.capture.auditTransaction);
            assertEquals(2,runtime.capture.activeAtAudit); assertTrue(runtime.capture.activeAtAudit<=4);
            assertEquals(outerPid,runtime.scalar("SELECT pg_backend_pid()"));
            System.out.println("S1-B outer connection="+outerPid+" transaction="+outerTx);
        });
        assertEquals(1,number("SELECT count(*) FROM koiki_audit_event"));
    }
    @Test void b6_01_revocationBetweenCheckAndConsume() throws Exception {
        worker(); assertTrue(runtime.authorized()); invalidate("noPermission"); denied();
    }
    @ParameterizedTest @ValueSource(strings={"disabled","noPermission","missing"})
    void b6_02_revocationAfterCommitHoldsConsumed(String mode) throws Exception {
        worker(); assertEquals("HOLD",runtime.execute(PERMIT,Snapshot.matching(),()->{
            try { invalidate(mode); } catch(SQLException failure) { throw new IllegalStateException(failure); }
        }));
        assertEquals(1,number("SELECT count(*) FROM s1b.consumption")); assertEquals(0,runtime.sends);
        assertEquals(1,number("SELECT count(*) FROM koiki_audit_event WHERE audit_type='BUSINESS'"));
        assertEquals(1,number("SELECT count(*) FROM koiki_audit_event WHERE audit_type='SECURITY'"));
    }
    private static Connection admin() throws SQLException { return DriverManager.getConnection(POSTGRES.getJdbcUrl(),POSTGRES.getUsername(),POSTGRES.getPassword()); }
    private static void adminSql(String... statements) throws SQLException {
        try(var connection=admin();var statement=connection.createStatement()) { for(String sql:statements) statement.execute(sql); }
    }
    private static String observer(String sql) throws SQLException {
        try(var connection=DriverManager.getConnection(POSTGRES.getJdbcUrl(),"s1b_reader","s1-fixture-only");
                var statement=connection.createStatement();var rows=statement.executeQuery(sql)) { assertTrue(rows.next()); return rows.getString(1); }
    }
    private static int number(String sql) throws SQLException { return Integer.parseInt(observer(sql)); }
    private static void assertSqlState(String role,String sql,String expected) throws SQLException {
        try(var connection=DriverManager.getConnection(POSTGRES.getJdbcUrl(),role,"s1-fixture-only");var statement=connection.createStatement()) {
            var failure=assertThrows(SQLException.class,()->statement.execute(sql)); assertEquals(expected,failure.getSQLState());
            System.out.println("S1-B DB diagnostic role="+role+" SQLSTATE="+failure.getSQLState());
        }
    }
}
