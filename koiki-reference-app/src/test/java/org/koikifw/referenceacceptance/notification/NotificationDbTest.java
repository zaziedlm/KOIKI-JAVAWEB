package org.koikifw.referenceacceptance.notification;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.security.core.context.SecurityContextHolder;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** Class-per-fork lifecycle. Each invocation starts from finite isolated test data. */
public abstract class NotificationDbTest {
    protected static final PostgreSQLContainer POSTGRES = NotificationFoundationDbHarness.database();
    protected static NotificationFoundationDbHarness db;
    @BeforeAll static void start() throws Exception {
        POSTGRES.start();
        try { db = new NotificationFoundationDbHarness(POSTGRES); }
        catch (Exception failure) { POSTGRES.stop(); throw failure; }
    }
    @BeforeEach void prepare() throws Exception { db.reset(); }
    @AfterEach void clearPrincipal() { SecurityContextHolder.clearContext(); }
    @AfterAll static void finish() {
        try { if (db != null) db.close(); }
        finally { POSTGRES.stop(); }
    }
}
