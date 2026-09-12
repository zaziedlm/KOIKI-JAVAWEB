package com.example.koikiconsumer.c2;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.koikifw.audit.AuditEvent;
import org.koikifw.audit.BusinessAuditRecorder;
import org.koikifw.identity.FrameworkUserId;
import org.koikifw.identity.IdentityAdministration;
import org.koikifw.identity.IdentityQuery;
import org.koikifw.identity.IdentityUser;
import org.koikifw.identity.UserStatus;

class C2PublicApiProbeTest {

    @Test
    void delegatesThroughFrameworkPublicApis() {
        IdentityAdministration administration = mock(IdentityAdministration.class);
        IdentityQuery query = mock(IdentityQuery.class);
        BusinessAuditRecorder recorder = mock(BusinessAuditRecorder.class);
        FrameworkUserId userId = FrameworkUserId.parse(UUID.randomUUID().toString());
        String email = "unit-" + UUID.randomUUID() + "@" + "invalid.example";
        IdentityUser user = new IdentityUser(
                userId, email, UserStatus.ACTIVE, Set.of(), Set.of(), 0L);
        when(administration.createUser(email)).thenReturn(userId);
        when(query.findById(userId)).thenReturn(Optional.of(user));

        new C2PublicApiProbe(administration, query, recorder).run(email);

        verify(administration).createUser(email);
        verify(query).findById(userId);
        verify(recorder).record(any(AuditEvent.class));
    }
}
