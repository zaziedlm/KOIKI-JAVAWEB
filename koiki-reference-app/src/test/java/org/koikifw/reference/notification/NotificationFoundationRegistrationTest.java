package org.koikifw.reference.notification;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Initial R01-R03 only; service/adapter registration and denial checks follow later. */
class NotificationFoundationRegistrationTest {
    @BeforeAll
    static void startDatabase() throws Exception {
        NotificationFoundationMigrationTest.startDatabase();
    }

    @AfterAll
    static void stopDatabase() {
        NotificationFoundationMigrationTest.stopDatabase();
    }

    @Test
    void omitsFoundationWhenPropertyIsAbsent() throws Exception {
        try (var context = NotificationFoundationMigrationTest.start(
                NotificationFoundationMigrationTest.newDatabase(), "absent", false)) {
            NotificationFoundationMigrationTest.assertDisabled(context);
        }
    }

    @Test
    void omitsFoundationWhenPropertyIsFalse() throws Exception {
        try (var context = NotificationFoundationMigrationTest.start(
                NotificationFoundationMigrationTest.newDatabase(), "false", false)) {
            NotificationFoundationMigrationTest.assertDisabled(context);
        }
    }

    @Test
    void rejectsInvalidEnablementProperty() throws Exception {
        String url = NotificationFoundationMigrationTest.newDatabase();
        assertThatThrownBy(() -> NotificationFoundationMigrationTest.start(url, "invalid", false))
                .isInstanceOf(RuntimeException.class).hasStackTraceContaining("Invalid notification foundation enabled property");
    }
}
