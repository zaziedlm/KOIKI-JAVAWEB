package org.koikifw.buildsupport.referencee2e;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

/** R2 selected failures through real HTTP, packaged Reference and isolated PostgreSQL. */
@EnabledIfSystemProperty(named = "koiki.reference.async.r2.verification.enabled", matches = "true")
@Timeout(240)
class PackagedReferenceAsyncFailureIsolationTest {
    @Test
    void normalArtifactCapabilityDenialOriginComparison() throws Exception {
        try (var fixture = new AsyncReferenceFixture()) {
            fixture.launch(Path.of(System.getProperty("koiki.reference.async.verification.normal-jar")), false, false);
            fixture.readyAndSeed(false);
            var request = fixture.submittedRequest(fixture.applicant);
            fixture.loginAs(fixture.secondApplicant);
            fixture.assertNormalDenialComparison(request, fixture.approveRequest(request, 2));
        }
    }

    @Test
    void normalControlCompletesNotification() throws Exception {
        try (var fixture = ready()) {
            var request = fixture.approve(fixture.applicant);
            fixture.assertApproval(request, fixture.applicant, true);
            assertEquals(1, fixture.stub.arrivalCount());
            assertEquals(1, fixture.stub.receiptCount());
        }
    }

    @Test
    void providerFailurePreservesCommittedApprovalAndAudit() throws Exception {
        try (var fixture = ready()) {
            fixture.stub.rejectBeforeReceipt();
            var request = fixture.approve(fixture.applicant);
            fixture.assertFailedNotification(request);
        }
    }

    @Test
    void publicationInsertFailureRollsBackApprovalAndAudit() throws Exception {
        try (var fixture = ready()) {
            var request = fixture.submittedRequest(fixture.applicant);
            try (var injection = new R2DatabaseFailureInjection(fixture.stub, request, true)) {
                assertEquals(503, fixture.approveRequest(request, 2).statusCode());
                injection.assertReachedOnce();
                fixture.assertSubmittedWithoutNotification(request);
            }
        }
    }

    @Test
    void approvalAuditInsertFailureRollsBackWithoutPublication() throws Exception {
        try (var fixture = ready()) {
            var request = fixture.submittedRequest(fixture.applicant);
            try (var injection = new R2DatabaseFailureInjection(fixture.stub, request, false)) {
                assertEquals(503, fixture.approveRequest(request, 2).statusCode());
                injection.assertReachedOnce();
                fixture.assertSubmittedWithoutNotification(request);
            }
        }
    }

    @Test
    void missingApprovalCapabilityRejectsWithoutNotification() throws Exception {
        try (var fixture = ready()) {
            var request = fixture.submittedRequest(fixture.applicant);
            fixture.loginAs(fixture.secondApplicant);
            assertEquals(403, fixture.approveRequest(request, 2).statusCode());
            fixture.assertSubmittedWithoutNotification(request);
        }
    }

    @Test
    void outsideDepartmentScopeRejectsWithoutNotification() throws Exception {
        try (var fixture = ready()) {
            var request = fixture.submittedRequest(fixture.applicant);
            fixture.removeApproverScope();
            assertEquals(404, fixture.approveRequest(request, 2).statusCode());
            fixture.assertSubmittedWithoutNotification(request);
        }
    }

    @Test
    void staleApprovalVersionRejectsWithoutNotification() throws Exception {
        try (var fixture = ready()) {
            var request = fixture.submittedRequest(fixture.applicant);
            assertEquals(409, fixture.approveRequest(request, 1).statusCode());
            fixture.assertSubmittedWithoutNotification(request);
        }
    }

    @Test
    void synchronousMasterVetoRemainsEffectiveWithAsyncEnabled() throws Exception {
        try (var fixture = ready()) {
            var request = fixture.submittedRequest(fixture.applicant);
            assertEquals(409, fixture.deactivateDepartment().statusCode());
            fixture.assertDepartmentUnchanged();
            fixture.assertSubmittedWithoutNotification(request);
        }
    }

    private static AsyncReferenceFixture ready() throws Exception {
        var fixture = new AsyncReferenceFixture();
        try {
            fixture.launch(Path.of(System.getProperty("koiki.reference.async.verification.async-jar")), true, false);
            fixture.readyAndSeed(true);
            return fixture;
        } catch (Exception | Error failure) {
            try { fixture.close(); } catch (Throwable cleanup) { failure.addSuppressed(cleanup); }
            throw failure;
        }
    }
}
