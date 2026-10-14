package org.koikifw.buildsupport.referencee2e;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

/** R3-B: one fresh database and one isolated recovery child for each selected branch. */
@EnabledIfSystemProperty(named = "koiki.reference.async.r3b.verification.enabled", matches = "true")
@Timeout(240)
class PackagedReferenceAsyncUnknownRecoveryTest {
    @Test
    void B01DurableObservationBeforeCompletionRecoversWithoutSecondPost() throws Exception {
        try (var fixture = new R3RecoveryFixture()) {
            fixture.start(fixture.base.stub.endpoint());
            UUID request = fixture.base.submittedRequest(fixture.base.applicant);
            UUID otherRequest = fixture.base.submittedRequest(fixture.base.secondApplicant);
            R3RecoveryTarget target;
            R3RecoveryTarget otherTarget;
            R3RecoveryFixture.StopReceipt stop;
            try (var gate = new R3ReceiptCompletionGate(fixture.base.stub, request)) {
                assertEquals(302, fixture.base.approveRequest(request, 2).statusCode());
                String key = "expense-approved:v1:" + request + ":3:applicant";
                gate.awaitBlockedWithReceipt(key);
                target = fixture.snapshot(request, fixture.base.applicant);
                assertEquals(302, fixture.base.approveRequest(otherRequest, 2).statusCode());
                otherTarget = fixture.snapshot(otherRequest, fixture.base.secondApplicant);
                assertEquals("PUBLISHED", otherTarget.status());
                stop = fixture.stopOriginal();
            }
            fixture.authorize(target);
            fixture.recover(target, stop);
            assertEquals(1, fixture.base.stub.arrivalCount());
            assertEquals(1, fixture.base.stub.receiptCount());
            assertEquals(otherTarget, fixture.snapshot(otherRequest, fixture.base.secondApplicant),
                    "Other publication changed during selected recovery");
        }
    }

    @Test
    void B02aMissingKeyContractHoldsWithoutPost() throws Exception {
        try (var fixture = new R3RecoveryFixture()) {
            var pending = preSend(fixture);
            fixture.authorize(pending.target());
            fixture.base.stub.removeContract(pending.target().key());
            fixture.expectHold(pending.target(), pending.stop(), "CONFLICT");
        }
    }

    @Test
    void B02bChangedPayloadContractHoldsWithoutPost() throws Exception {
        try (var fixture = new R3RecoveryFixture()) {
            var pending = preSend(fixture);
            fixture.authorize(pending.target());
            fixture.base.stub.registerContract(pending.target().key(), "0".repeat(64),
                    pending.target().recipientId(), fixture.base.stub.verificationTime().plusSeconds(300));
            fixture.expectHold(pending.target(), pending.stop(), "CONFLICT");
        }
    }

    @Test
    void B02cExpiredKeyHoldsWithoutPost() throws Exception {
        try (var fixture = new R3RecoveryFixture()) {
            var pending = preSend(fixture);
            fixture.authorize(pending.target());
            Instant beforeExpiry = fixture.base.stub.verificationTime();
            String historicalKey = "historical-receipt:" + UUID.randomUUID();
            UUID historicalEvent = UUID.randomUUID();
            UUID historicalRecipient = UUID.randomUUID();
            String historicalBody = "{\"event\":\"" + historicalEvent + "\"}";
            String historicalHash = LocalNotificationProviderStub.sha256(historicalBody);
            fixture.base.stub.registerContract(historicalKey, historicalHash, historicalRecipient,
                    beforeExpiry.plusSeconds(60));
            try (var client = HttpClient.newHttpClient()) {
                var response = client.send(HttpRequest.newBuilder(fixture.base.stub.endpoint())
                        .timeout(Duration.ofSeconds(5))
                        .header("Idempotency-Key", historicalKey)
                        .header("X-Koiki-Event-Id", historicalEvent.toString())
                        .header("X-Payload-SHA256", historicalHash)
                        .header("X-Recipient-Id", historicalRecipient.toString())
                        .POST(HttpRequest.BodyPublishers.ofString(historicalBody)).build(),
                        HttpResponse.BodyHandlers.ofString());
                assertEquals(201, response.statusCode());
                assertEquals(response.body(), fixture.base.stub.receipt(historicalKey));
            }
            String historicalReceipt = fixture.base.stub.receipt(historicalKey);
            fixture.base.stub.setVerificationTime(beforeExpiry.plusSeconds(120));
            fixture.base.stub.registerContract(pending.target().key(), pending.target().payloadHash(),
                    pending.target().recipientId(), beforeExpiry.plusSeconds(60));
            fixture.expectHold(pending.target(), pending.stop(), "EXPIRED");
            assertEquals(historicalReceipt, fixture.base.stub.receipt(historicalKey),
                    "Expired contract removed an earlier accepted receipt");
        }
    }

    @Test
    void B03aAcceptedButLostResponseCompletesWithoutSecondPost() throws Exception {
        try (var fixture = new R3RecoveryFixture()) {
            fixture.base.stub.loseAfterReceipt();
            fixture.start(fixture.base.stub.endpoint());
            UUID request = fixture.base.submittedRequest(fixture.base.applicant);
            assertEquals(302, fixture.base.approveRequest(request, 2).statusCode());
            Instant deadline = Instant.now().plusSeconds(20);
            R3RecoveryTarget target;
            do {
                target = fixture.snapshot(request, fixture.base.applicant);
                if (fixture.base.stub.receiptCount() == 1 && "FAILED".equals(target.status())) break;
                Thread.sleep(100);
            } while (Instant.now().isBefore(deadline));
            assertEquals(1, fixture.base.stub.receiptCount());
            assertEquals(1, fixture.base.stub.arrivalCount());
            assertEquals("FAILED", target.status());
            String providerReceipt = fixture.base.stub.receipt(target.key());
            assertObservation(fixture, target, null);
            var stop = fixture.stopOriginal();
            fixture.authorize(target);
            fixture.recover(target, stop);
            assertEquals(1, fixture.base.stub.arrivalCount());
            assertEquals(providerReceipt, fixture.base.stub.receipt(target.key()));
            assertObservation(fixture, target, providerReceipt);
        }
    }

    @Test
    void B03aFailedAfterAcceptedResponseLossIsHeldForHumanReconciliation() throws Exception {
        try (var fixture = new R3RecoveryFixture()) {
            fixture.base.stub.loseAfterReceipt();
            fixture.start(fixture.base.stub.endpoint());
            UUID request = fixture.base.submittedRequest(fixture.base.applicant);
            assertEquals(302, fixture.base.approveRequest(request, 2).statusCode());
            R3RecoveryTarget target = awaitFailed(fixture, request, fixture.base.applicant, 1);
            String providerReceipt = assertProviderAcceptance(fixture, target);
            assertObservation(fixture, target, null);

            UUID otherRequest = fixture.base.submittedRequest(fixture.base.secondApplicant);
            assertEquals(302, fixture.base.approveRequest(otherRequest, 2).statusCode());
            R3RecoveryTarget otherTarget = awaitFailed(fixture, otherRequest, fixture.base.secondApplicant, 2);
            assertProviderAcceptance(fixture, otherTarget);
            assertObservation(fixture, otherTarget, null);
            long arrivalsBefore = fixture.base.stub.arrivalCount();
            long receiptsBefore = fixture.base.stub.receiptCount();
            assertEquals(2, arrivalsBefore);
            assertEquals(2, receiptsBefore);

            var stop = fixture.stopOriginal();
            assertEquals(target, fixture.snapshot(request, fixture.base.applicant));
            assertEquals(otherTarget, fixture.snapshot(otherRequest, fixture.base.secondApplicant));
            assertObservation(fixture, target, null);
            assertEquals(providerReceipt, assertProviderAcceptance(fixture, target));
            assertEquals(arrivalsBefore, fixture.base.stub.arrivalCount());
            assertEquals(receiptsBefore, fixture.base.stub.receiptCount());
            assertTrue(fixture.base.recoveryLogPath() == null, "HOLD must not start a recovery child");

            Instant observedAt = Instant.now();
            Path evidence = Files.createTempFile(fixture.asyncJar.getParent(),
                    "r3b-failed-hold-" + target.publicationId() + "-", ".json");
            String json = "{\"reason\":\"FAILED_RECONCILIATION_REQUIRED\",\"publicationId\":\""
                    + target.publicationId() + "\",\"eventId\":\"" + target.eventId()
                    + "\",\"notificationKeySha256\":\"" + LocalNotificationProviderStub.sha256(target.key())
                    + "\",\"providerAcceptanceId\":\"" + providerReceipt
                    + "\",\"previousPid\":" + stop.pid() + ",\"previousStart\":\"" + stop.start()
                    + "\",\"previousStopped\":true,\"observedAt\":\"" + observedAt
                    + "\",\"referenceReceiptLogPresent\":false,\"publicationStatus\":\"" + target.status()
                    + "\",\"completionAttempts\":" + target.attempts()
                    + ",\"arrivalsBefore\":" + arrivalsBefore + ",\"arrivalsAfter\":"
                    + fixture.base.stub.arrivalCount() + ",\"receiptsBefore\":" + receiptsBefore
                    + ",\"receiptsAfter\":" + fixture.base.stub.receiptCount()
                    + ",\"next\":\"inspect-provider-ledger-and-reference-publication-before-resume\"}";
            Files.writeString(evidence, json);
            assertEquals(json, Files.readString(evidence), "HOLD evidence was not retained");
            System.out.println("R3B_FAILED_HOLD_EVIDENCE path=" + evidence);
        }
    }

    @Test
    void B03bDisconnectedBeforeArrivalResendsSameKeyOnce() throws Exception {
        try (var fixture = new R3RecoveryFixture();
                var gate = new R3ProviderGate(R3ProviderGate.Position.BEFORE_ARRIVAL,
                        fixture.base.stub.endpoint())) {
            fixture.start(gate.endpoint());
            UUID request = fixture.base.submittedRequest(fixture.base.applicant);
            assertEquals(302, fixture.base.approveRequest(request, 2).statusCode());
            gate.awaitGate();
            var target = fixture.snapshot(request, fixture.base.applicant);
            assertEquals(0, providerCount(fixture, "arrived_requests", target.key()));
            assertEquals(0, providerCount(fixture, "accepted_notifications", target.key()));
            assertObservation(fixture, target, null);
            UUID otherRequest = fixture.base.submittedRequest(fixture.base.secondApplicant);
            assertEquals(302, fixture.base.approveRequest(otherRequest, 2).statusCode());
            var stop = fixture.stopOriginal();
            gate.abortAfterStop();
            var otherBefore = publicationTuple(fixture, otherRequest);
            fixture.authorize(target);
            fixture.recover(target, stop);
            assertEquals(1, providerCount(fixture, "arrived_requests", target.key()));
            assertEquals(1, providerCount(fixture, "accepted_notifications", target.key()));
            assertObservation(fixture, target, fixture.base.stub.receipt(target.key()));
            assertEquals(otherBefore, publicationTuple(fixture, otherRequest),
                    "Other publication changed during selected recovery");
        }
    }

    @Test
    void B03cUnavailableQueryHoldsWithoutPost() throws Exception {
        try (var fixture = new R3RecoveryFixture()) {
            var pending = preSendWithOther(fixture);
            var otherBefore = publicationTuple(fixture, pending.otherRequest());
            fixture.authorize(pending.target());
            fixture.base.stub.queryUnavailable();
            fixture.expectHold(pending.target(), pending.stop(), "UNKNOWN");
            assertObservation(fixture, pending.target(), null);
            assertEquals(0, providerCount(fixture, "arrived_requests", pending.target().key()));
            assertEquals(0, providerCount(fixture, "accepted_notifications", pending.target().key()));
            assertEquals(otherBefore, publicationTuple(fixture, pending.otherRequest()),
                    "Other publication changed during UNKNOWN hold");
            String hold = "{\"reason\":\"UNKNOWN\",\"publicationId\":\""
                    + pending.target().publicationId() + "\",\"eventId\":\""
                    + pending.target().eventId() + "\",\"notificationKeySha256\":\""
                    + LocalNotificationProviderStub.sha256(pending.target().key())
                    + "\",\"next\":\"inspect-provider-ledger-and-reference-publication-before-resume\"}";
            Path evidence = Files.createTempFile(fixture.asyncJar.getParent(), "r3b-unknown-hold-", ".json");
            Files.writeString(evidence, hold);
            assertEquals(hold, Files.readString(evidence));
            System.out.println("R3B_UNKNOWN_HOLD_EVIDENCE path=" + evidence);
        }
    }

    private PendingWithOther preSendWithOther(R3RecoveryFixture fixture) throws Exception {
        fixture.start(fixture.base.stub.endpoint());
        UUID request = fixture.base.submittedRequest(fixture.base.applicant);
        try (var gate = new R3DatabaseGate(fixture.base.stub, request)) {
            assertEquals(302, fixture.base.approveRequest(request, 2).statusCode());
            gate.awaitBlockedBackend();
            var target = fixture.snapshot(request, fixture.base.applicant);
            assertEquals("PUBLISHED", target.status());
            assertEquals(0, providerCount(fixture, "arrived_requests", target.key()));
            UUID otherRequest = fixture.base.submittedRequest(fixture.base.secondApplicant);
            assertEquals(302, fixture.base.approveRequest(otherRequest, 2).statusCode());
            var stop = fixture.stopOriginal();
            return new PendingWithOther(target, stop, otherRequest);
        }
    }

    private long providerCount(R3RecoveryFixture fixture, String table, String key) throws Exception {
        if (!"arrived_requests".equals(table) && !"accepted_notifications".equals(table)) {
            throw new IllegalArgumentException("Unsupported provider table");
        }
        try (var connection = fixture.base.stub.observer(); var statement = connection.prepareStatement(
                "SELECT count(*) FROM kkref_r1_provider_stub." + table + " WHERE notification_key = ?")) {
            statement.setString(1, key);
            try (var rows = statement.executeQuery()) {
                assertTrue(rows.next());
                return rows.getLong(1);
            }
        }
    }

    private PublicationTuple publicationTuple(R3RecoveryFixture fixture, UUID request) throws Exception {
        try (var connection = fixture.base.stub.observer(); var statement = connection.prepareStatement("""
                SELECT id, listener_id, serialized_event, status, completion_attempts, completion_date
                FROM kkref_notification_async.event_publication
                WHERE serialized_event::jsonb->>'expenseRequestId' = ?
                """)) {
            statement.setString(1, request.toString());
            try (var rows = statement.executeQuery()) {
                assertTrue(rows.next(), "Other publication absent");
                var completed = rows.getTimestamp(6);
                var tuple = new PublicationTuple(rows.getObject(1, UUID.class), rows.getString(2),
                        rows.getString(3), rows.getString(4), rows.getInt(5),
                        completed == null ? null : completed.toInstant());
                assertTrue(!rows.next(), "Other publication not unique");
                return tuple;
            }
        }
    }

    private Pending preSend(R3RecoveryFixture fixture) throws Exception {
        fixture.start(fixture.base.stub.endpoint());
        UUID request = fixture.base.submittedRequest(fixture.base.applicant);
        try (var gate = new R3DatabaseGate(fixture.base.stub, request)) {
            assertEquals(302, fixture.base.approveRequest(request, 2).statusCode());
            gate.awaitBlockedBackend();
            var target = fixture.snapshot(request, fixture.base.applicant);
            assertEquals("PUBLISHED", target.status());
            assertEquals(0, fixture.base.stub.arrivalCount());
            var stop = fixture.stopOriginal();
            return new Pending(target, stop);
        }
    }

    private R3RecoveryTarget awaitFailed(R3RecoveryFixture fixture, UUID request, UUID recipient,
            long expectedReceipts) throws Exception {
        Instant deadline = Instant.now().plusSeconds(20);
        R3RecoveryTarget target;
        do {
            target = fixture.snapshot(request, recipient);
            if (fixture.base.stub.receiptCount() == expectedReceipts && "FAILED".equals(target.status())) {
                return target;
            }
            Thread.sleep(100);
        } while (Instant.now().isBefore(deadline));
        assertEquals(expectedReceipts, fixture.base.stub.receiptCount());
        assertEquals("FAILED", target.status());
        return target;
    }

    private String assertProviderAcceptance(R3RecoveryFixture fixture, R3RecoveryTarget target) throws Exception {
        try (var connection = fixture.base.stub.observer(); var statement = connection.prepareStatement("""
                SELECT canonical_payload_hash, acceptance_id, event_id, recipient_id
                FROM kkref_r1_provider_stub.accepted_notifications
                WHERE notification_key = ?
                """)) {
            statement.setString(1, target.key());
            try (var rows = statement.executeQuery()) {
                assertTrue(rows.next(), "Provider acceptance absent");
                assertEquals(target.payloadHash(), rows.getString(1));
                String acceptanceId = rows.getString(2);
                assertEquals(target.eventId(), rows.getObject(3, UUID.class));
                assertEquals(target.recipientId(), rows.getObject(4, UUID.class));
                assertTrue(!rows.next(), "Duplicate provider acceptance");
                return acceptanceId;
            }
        }
    }

    private void assertObservation(R3RecoveryFixture fixture, R3RecoveryTarget target,
            String expectedReceipt) throws Exception {
        try (var connection = fixture.base.stub.observer(); var statement = connection.prepareStatement("""
                SELECT event_id, recipient_id, payload_sha256, acceptance_id
                FROM kkref_notification_async.notification_receipt_observation
                WHERE notification_key = ?
                """)) {
            statement.setString(1, target.key());
            try (var rows = statement.executeQuery()) {
                if (expectedReceipt == null) {
                    assertTrue(!rows.next(), "Unexpected Reference receipt observation");
                    return;
                }
                assertTrue(rows.next(), "Reference receipt observation absent");
                assertEquals(target.eventId(), rows.getObject(1, UUID.class));
                assertEquals(target.recipientId(), rows.getObject(2, UUID.class));
                assertEquals(target.payloadHash(), rows.getString(3));
                assertEquals(UUID.fromString(expectedReceipt), rows.getObject(4, UUID.class));
                assertTrue(!rows.next(), "Duplicate Reference receipt observation");
            }
        }
    }

    private record Pending(R3RecoveryTarget target, R3RecoveryFixture.StopReceipt stop) { }

    private record PendingWithOther(R3RecoveryTarget target, R3RecoveryFixture.StopReceipt stop,
            UUID otherRequest) { }

    private record PublicationTuple(UUID id, String listenerId, String serializedEvent,
            String status, int attempts, Instant completionDate) { }
}
