package org.koikifw.buildsupport.referencee2e;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

/** Tooling-only trigger and nontransactional sequence witness on the owned fresh database. */
final class R2DatabaseFailureInjection implements AutoCloseable {
    private final LocalNotificationProviderStub stub;
    private final String table;

    R2DatabaseFailureInjection(LocalNotificationProviderStub stub, UUID request, boolean publication)
            throws Exception {
        this.stub = stub;
        table = publication ? "kkref_notification_async.event_publication" : "public.koiki_audit_event";
        String condition = publication
                ? "NEW.event_type='org.koikifw.reference.expense.domain.event.ExpenseApprovedEvent' "
                    + "AND NEW.listener_id LIKE '%ExpenseApprovalListener%' "
                    + "AND NEW.serialized_event::jsonb->>'expenseRequestId'='" + request + "'"
                : "NEW.audit_type='BUSINESS' AND NEW.event_type='EXPENSE_WORKFLOW' "
                    + "AND NEW.action='APPROVE_EXPENSE' AND NEW.resource_id='" + request + "'";
        try (var c = stub.admin(); var s = c.createStatement()) {
            s.execute("CREATE SCHEMA kkref_r2_test");
            s.execute("CREATE SEQUENCE kkref_r2_test.injection_witness");
            s.execute("GRANT USAGE ON SCHEMA kkref_r2_test TO kkref_r1_observer");
            s.execute("GRANT SELECT ON SEQUENCE kkref_r2_test.injection_witness TO kkref_r1_observer");
            s.execute("CREATE FUNCTION kkref_r2_test.fail_target() RETURNS trigger LANGUAGE plpgsql AS $$ "
                    + "BEGIN IF " + condition + " THEN "
                    + "PERFORM nextval('kkref_r2_test.injection_witness'); "
                    + "RAISE EXCEPTION USING ERRCODE='P0001', MESSAGE='R2_"
                    + (publication ? "PUBLICATION" : "AUDIT") + "_INJECTION'; END IF; RETURN NEW; END $$");
            s.execute("CREATE TRIGGER r2_fail_target BEFORE INSERT ON " + table
                    + " FOR EACH ROW EXECUTE FUNCTION kkref_r2_test.fail_target()");
        }
        try (var c = stub.observer(); var s = c.createStatement();
                var rows = s.executeQuery("SELECT is_called FROM kkref_r2_test.injection_witness")) {
            assertTrue(rows.next()); assertFalse(rows.getBoolean(1));
        }
    }

    void assertReachedOnce() throws Exception {
        try (var c = stub.observer(); var s = c.createStatement();
                var rows = s.executeQuery("SELECT is_called,last_value FROM kkref_r2_test.injection_witness")) {
            assertTrue(rows.next()); assertTrue(rows.getBoolean(1), "Injection point was not reached");
            assertEquals(1, rows.getLong(2));
        }
    }

    @Override
    public void close() throws Exception {
        try (var c = stub.admin(); var s = c.createStatement()) {
            s.execute("DROP TRIGGER IF EXISTS r2_fail_target ON " + table);
            s.execute("DROP SCHEMA kkref_r2_test CASCADE");
        }
    }
}
