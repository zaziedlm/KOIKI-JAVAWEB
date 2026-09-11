package com.example.koikiconsumer.c2;

import org.koikifw.audit.AuditActor;
import org.koikifw.audit.AuditEvent;
import org.koikifw.audit.AuditResult;
import org.koikifw.audit.BusinessAuditRecorder;
import org.koikifw.identity.FrameworkUserId;
import org.koikifw.identity.IdentityAdministration;
import org.koikifw.identity.IdentityQuery;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Tier 1 Consumer use case that exercises only Framework Public API types. */
@Service
class C2PublicApiProbe {

    private final IdentityAdministration identityAdministration;
    private final IdentityQuery identityQuery;
    private final BusinessAuditRecorder businessAuditRecorder;

    C2PublicApiProbe(
            IdentityAdministration identityAdministration,
            IdentityQuery identityQuery,
            BusinessAuditRecorder businessAuditRecorder) {
        this.identityAdministration = identityAdministration;
        this.identityQuery = identityQuery;
        this.businessAuditRecorder = businessAuditRecorder;
    }

    @Transactional
    FrameworkUserId run(String syntheticEmail) {
        FrameworkUserId userId = identityAdministration.createUser(syntheticEmail);
        if (identityQuery.findById(userId).isEmpty()) {
            throw new IllegalStateException("The created Framework user was not queryable.");
        }
        businessAuditRecorder.record(AuditEvent.of(
                "C2_CONSUMER_PROBE",
                AuditActor.system("C2_CONSUMER"),
                "VERIFY_PUBLIC_API",
                AuditResult.SUCCESS));
        return userId;
    }
}
