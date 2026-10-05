package org.koikifw.buildsupport.phase4;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** L1: real PostgreSQL credentials and transaction boundaries, without Spring application scanning. */
@Timeout(600)
class S1PermitStorageTest {
    private static final String PERMIT = "00000000-0000-0000-0000-000000000001";
    private static final String OPERATION = "00000000-0000-0000-0000-000000000010";
    private static final String IMAGE = "postgres@sha256:"
            + "18cfe3ef5e6815560c98237d6216d1e5119702fb0f3894c8785dd58b8bbe5d73";
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(IMAGE)
            .withCommand("postgres", "-c", "max_connections=16")
            .withCreateContainerCmdModifier(command -> command.getHostConfig()
                    .withMemory(1024L * 1024 * 1024).withNanoCPUs(1_000_000_000L));

    @BeforeAll
    static void startDatabase() throws SQLException {
        POSTGRES.start();
        try (var connection = admin()) {
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("s1-minimum/permit-storage.sql"));
            System.out.println("S1-L1 database=" + scalar(connection, "SHOW server_version")
                    + " max_connections=" + scalar(connection, "SHOW max_connections")
                    + " container=" + POSTGRES.getContainerId() + " image=" + IMAGE);
        }
        var host = POSTGRES.getContainerInfo().getHostConfig();
        assertEquals(1024L * 1024 * 1024, host.getMemory());
        assertEquals(1_000_000_000L, host.getNanoCPUs());
    }

    @AfterAll
    static void stopDatabase() {
        POSTGRES.stop();
    }

    @BeforeEach
    void seedPermit() throws SQLException {
        try (var connection = admin()) {
            execute(connection, "TRUNCATE s1.audit_probe, s1.consumption, s1.permit");
        }
        try (var connection = login("s1_web")) {
            execute(connection, issue(PERMIT));
        }
    }

    @Test
    void l1_01_workerLocksAndCommitsConsumptionAndAuditBeforeSend() throws SQLException {
        int sends = 0;
        try (var worker = login("s1_worker"); var observer = login("s1_reader")) {
            worker.setAutoCommit(false);
            assertEquals(PERMIT, scalar(worker, "SELECT permit_id FROM s1.permit FOR UPDATE"));
            execute(worker, consume(OPERATION));
            execute(worker, audit("'CONSUMED'"));
            assertEquals("0", scalar(observer, "SELECT count(*) FROM s1.consumption"));
            assertEquals("0", scalar(observer, "SELECT count(*) FROM s1.audit_probe"));
            assertEquals(0, sends);
            worker.commit();
            assertEquals("1", scalar(observer, "SELECT count(*) FROM s1.consumption"));
            assertEquals("1", scalar(observer, "SELECT count(*) FROM s1.audit_probe"));
            sends++; // sequencing probe only, never a provider call
            assertEquals(1, sends);
        }
    }

    @Test
    void l1_02_workerCannotIssueOrChangeImmutablePermitFields() throws SQLException {
        try (var worker = login("s1_worker")) {
            String before = snapshot(worker);
            denied(worker, issue("00000000-0000-0000-0000-000000000002"));
            for (String assignment : List.of("publication_id = gen_random_uuid()",
                    "event_id = gen_random_uuid()", "environment_id = 'other'", "listener_id = 'other'",
                    "actor_id = 'forged'", "reason_code = 'forged'", "expires_at = expires_at + interval '1 day'")) {
                denied(worker, "UPDATE s1.permit SET " + assignment);
            }
            assertEquals(before, snapshot(worker));
        }
    }

    @Test
    void l1_03_consumptionCannotBeUpdatedDeletedOrTruncated() throws SQLException {
        try (var worker = login("s1_worker")) {
            execute(worker, consume(OPERATION));
            String before = scalar(worker, "SELECT row_to_json(c)::text FROM s1.consumption c");
            for (String sql : List.of("UPDATE s1.consumption SET worker_generation = 'other'",
                    "DELETE FROM s1.consumption", "TRUNCATE s1.consumption")) {
                denied(worker, sql);
            }
            assertEquals(before, scalar(worker, "SELECT row_to_json(c)::text FROM s1.consumption c"));
        }
    }

    @Test
    void l1_04_duplicateConsumptionFailsEvenAfterVersionReset() throws SQLException {
        try (var worker = login("s1_worker")) {
            execute(worker, consume(OPERATION));
            execute(worker, "UPDATE s1.permit SET version = 999");
            execute(worker, "UPDATE s1.permit SET version = 0");
            failure(worker, consume("00000000-0000-0000-0000-000000000011"), "23505");
            assertEquals("1", scalar(worker, "SELECT count(*) FROM s1.consumption"));
            assertEquals(OPERATION, scalar(worker, "SELECT operation_id FROM s1.consumption"));
        }
    }

    @Test
    void l1_05_explicitRollbackRemovesBothRecordsAndDoesNotSend() throws SQLException {
        int sends = 0;
        try (var worker = login("s1_worker")) {
            worker.setAutoCommit(false);
            execute(worker, consume(OPERATION));
            execute(worker, audit("'CONSUMED'"));
            worker.rollback();
        }
        assertEmptyAndNotSent(sends);
    }

    @Test
    void l1_06_auditWriteFailureAbortsConsumptionTransaction() throws SQLException {
        int sends = 0;
        try (var worker = login("s1_worker")) {
            worker.setAutoCommit(false);
            execute(worker, consume(OPERATION));
            failure(worker, audit("NULL"), "23502");
            failure(worker, "SELECT count(*) FROM s1.consumption", "25P02");
            worker.rollback();
        }
        assertEmptyAndNotSent(sends);
    }

    @Test
    void l1_07_runtimeRolesHaveNoOwnerInheritanceOrDdlPrivileges() throws SQLException {
        for (String role : List.of("s1_web", "s1_worker", "s1_reader")) {
            try (var connection = login(role)) {
                assertEquals(role, scalar(connection, "SELECT current_user"));
                assertEquals("false", scalar(connection, "SELECT rolsuper OR rolcreaterole OR rolcreatedb"
                        + " OR rolinherit OR rolbypassrls FROM pg_roles WHERE rolname = current_user"));
                assertEquals("false", scalar(connection, "SELECT pg_has_role(current_user, 's1_owner', 'MEMBER')"));
                assertEquals("false", scalar(connection, "SELECT has_schema_privilege(current_user, 's1', 'CREATE')"));
                assertEquals("false", scalar(connection, "SELECT has_schema_privilege(current_user, 'public', 'CREATE')"));
                denied(connection, "SET ROLE s1_owner");
                denied(connection, "CREATE TABLE s1.forbidden (id int)");
                denied(connection, "ALTER TABLE s1.permit ADD COLUMN forbidden int");
            }
        }
        try (var connection = admin()) {
            assertEquals("s1_owner", scalar(connection, "SELECT pg_get_userbyid(relowner)"
                    + " FROM pg_class WHERE oid = 's1.permit'::regclass"));
            assertEquals("0", scalar(connection, "SELECT count(*) FROM pg_class"
                    + " WHERE relnamespace = 's1'::regnamespace AND relkind = 'S'"));
        }
    }

    @Test
    void l1_08_unresolvedTargetRejectsAnotherPermitUntilHumanConfirmation() throws SQLException {
        try (var web = login("s1_web")) {
            failure(web, issue("00000000-0000-0000-0000-000000000002"), "23505");
            execute(web, "UPDATE s1.permit SET closed_at = now(), confirmed_by = 'fixture-reviewer',"
                    + " result_ref = 'fixture-evidence' WHERE permit_id = '" + PERMIT + "'");
            execute(web, issue("00000000-0000-0000-0000-000000000002"));
            assertEquals("2", scalar(web, "SELECT count(*) FROM s1.permit"));
        }
    }

    @Test
    void l1_09_webCannotWriteConsumptionOrTamperWithIssuedTarget() throws SQLException {
        try (var web = login("s1_web")) {
            denied(web, consume(OPERATION));
            denied(web, "UPDATE s1.consumption SET worker_generation = 'other'");
            denied(web, "DELETE FROM s1.consumption");
            denied(web, "TRUNCATE s1.consumption");
            denied(web, "UPDATE s1.permit SET actor_id = 'forged'");
            denied(web, "UPDATE s1.permit SET publication_id = gen_random_uuid()");
        }
    }

    @Test
    void l1_10_workerCannotConfirmOrDeletePermit() throws SQLException {
        try (var worker = login("s1_worker")) {
            String before = snapshot(worker);
            denied(worker, "UPDATE s1.permit SET closed_at = now(), confirmed_by = 'forged', result_ref = 'forged'");
            denied(worker, "DELETE FROM s1.permit");
            denied(worker, "TRUNCATE s1.permit");
            assertEquals(before, snapshot(worker));
        }
    }

    @Test
    void l1_11_readOnlyRoleCannotLockOrWriteAndVersionPermissionIsColumnLimited() throws SQLException {
        try (var reader = login("s1_reader"); var worker = login("s1_worker")) {
            denied(reader, "SELECT permit_id FROM s1.permit FOR UPDATE");
            denied(reader, "UPDATE s1.permit SET version = 1");
            denied(reader, consume(OPERATION));
            denied(reader, audit("'FORGED'"));
            assertEquals("true", scalar(worker, "SELECT has_column_privilege(current_user, 's1.permit', 'version', 'UPDATE')"));
            assertEquals("false", scalar(worker, "SELECT has_table_privilege(current_user, 's1.permit', 'UPDATE')"));
            assertEquals("false", scalar(worker, "SELECT has_column_privilege(current_user, 's1.permit', 'actor_id', 'UPDATE')"));
        }
    }

    private static Connection admin() throws SQLException {
        return DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    }

    private static Connection login(String role) throws SQLException {
        var connection = DriverManager.getConnection(POSTGRES.getJdbcUrl(), role, "s1-fixture-only");
        execute(connection, "SET lock_timeout = '10s'");
        execute(connection, "SET statement_timeout = '10s'");
        return connection;
    }

    private static void execute(Connection connection, String sql) throws SQLException {
        try (var statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private static String scalar(Connection connection, String sql) throws SQLException {
        try (var statement = connection.createStatement(); var rows = statement.executeQuery(sql)) {
            assertTrue(rows.next());
            return rows.getObject(1).toString();
        }
    }

    private static String snapshot(Connection connection) throws SQLException {
        return scalar(connection, "SELECT row_to_json(p)::text FROM s1.permit p");
    }

    private static void denied(Connection connection, String sql) {
        failure(connection, sql, "42501");
    }

    private static void failure(Connection connection, String sql, String state) {
        SQLException error = assertThrows(SQLException.class, () -> execute(connection, sql));
        assertEquals(state, error.getSQLState(), sql);
        System.out.println("S1-L1 expected SQLSTATE=" + state + " statement=" + sql);
    }

    private static void assertEmptyAndNotSent(int sends) throws SQLException {
        try (var connection = login("s1_reader")) {
            assertEquals("0", scalar(connection, "SELECT count(*) FROM s1.consumption"));
            assertEquals("0", scalar(connection, "SELECT count(*) FROM s1.audit_probe"));
            assertEquals(0, sends);
        }
    }

    private static String issue(String id) {
        return "INSERT INTO s1.permit (permit_id, environment_id, publication_id, event_id, listener_id,"
                + " actor_id, reason_code, issued_at, expires_at) VALUES ('" + id + "', 'fixture-env',"
                + " '00000000-0000-0000-0000-000000000100', '00000000-0000-0000-0000-000000000200',"
                + " 'fixture-listener', 'fixture-actor', 'OWNER_REVIEW', now(), now() + interval '30 minutes')";
    }

    private static String consume(String operation) {
        return "INSERT INTO s1.consumption VALUES ('" + PERMIT + "', '" + operation + "', 'fixture-worker-1', now())";
    }

    private static String audit(String action) {
        return "INSERT INTO s1.audit_probe VALUES ('" + PERMIT + "', " + action + ")";
    }
}
