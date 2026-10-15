package org.koikifw.buildsupport.referencee2e;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

@EnabledIfSystemProperty(named="koiki.reference.async.verification.enabled", matches="true")
class PackagedReferenceAsyncNotificationTest {
    @Test
    void asyncArtifactDisabledPreservesApprovalWithoutPublications() throws Exception {
        try (var fixture = new AsyncReferenceFixture()) {
            fixture.launch(jar("async"), false, false);
            fixture.readyAndSeed(false);
            var request = fixture.approve(fixture.applicant);
            fixture.assertApproval(request, fixture.applicant, false);
            assertEquals(0, fixture.schemas("kkref_notification_async"));
            assertEquals(0, fixture.stub.receiptCount());
        }
    }

    @Test
    void normalArtifactRejectsAsyncBeforeApplicationMigration() throws Exception {
        try (var fixture = new AsyncReferenceFixture()) {
            fixture.launch(jar("normal"), true, false);
            fixture.assertMissingStarterRejected();
        }
    }

    @Test
    void correlatesTwoIndependentApprovalNotificationsToCompletedPublications() throws Exception {
        try (var fixture = new AsyncReferenceFixture()) {
            fixture.launch(jar("async"), true, false);
            fixture.readyAndSeed(true);
            var first = fixture.approve(fixture.applicant);
            var second = fixture.approve(fixture.secondApplicant);
            fixture.assertApproval(first, fixture.applicant, true);
            fixture.assertApproval(second, fixture.secondApplicant, true);
            assertEquals(2, fixture.stub.receiptCount());
            assertEquals(0, fixture.migrationVersion("4"));
            assertEquals(1, fixture.migrationVersion("5"));
        }
    }

    @Test
    void explicitlySelectedFoundationAndAsyncMigrationsCoexistOnFreshDatabase() throws Exception {
        try (var fixture = new AsyncReferenceFixture()) {
            fixture.launch(jar("async"), true, true);
            fixture.readyAndSeed(true);
            var request = fixture.approve(fixture.applicant);
            fixture.assertApproval(request, fixture.applicant, true);
            assertEquals(1, fixture.stub.receiptCount());
            assertEquals(1, fixture.migrationVersion("4"));
            assertEquals(1, fixture.migrationVersion("5"));
        }
    }

    private static Path jar(String kind) {
        return Path.of(System.getProperty("koiki.reference.async.verification." + kind + "-jar"));
    }
}
