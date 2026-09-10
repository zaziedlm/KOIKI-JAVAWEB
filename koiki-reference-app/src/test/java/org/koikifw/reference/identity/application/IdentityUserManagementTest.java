package org.koikifw.reference.identity.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.koikifw.identity.FrameworkUserId;
import org.koikifw.identity.IdentityAdministration;
import org.koikifw.identity.IdentityQuery;
import org.koikifw.identity.IdentityUser;
import org.koikifw.identity.UserStatus;

class IdentityUserManagementTest {

    @Test
    void mapsFrameworkUserToImmutableSortedReferenceView() {
        FrameworkUserId userId = FrameworkUserId.parse(UUID.randomUUID().toString());
        IdentityQuery query = mock(IdentityQuery.class);
        IdentityAdministration administration = mock(IdentityAdministration.class);
        when(query.findById(userId))
                .thenReturn(Optional.of(new IdentityUser(
                        userId,
                        "admin-visible@example.test",
                        UserStatus.ACTIVE,
                        Set.of("ROLE_Z", "ROLE_A"),
                        Set.of("WRITE", "READ"),
                        4)));

        Optional<IdentityUserView> result =
                new IdentityUserManagement(query, administration).findUser(userId);

        assertThat(result).contains(new IdentityUserView(
                userId.toString(),
                "admin-visible@example.test",
                UserStatus.ACTIVE,
                java.util.List.of("ROLE_A", "ROLE_Z"),
                java.util.List.of("READ", "WRITE"),
                4));
    }

    @Test
    void preservesNotFoundWithoutAccessingPersistenceDirectly() {
        FrameworkUserId userId = FrameworkUserId.parse(UUID.randomUUID().toString());
        IdentityQuery query = mock(IdentityQuery.class);
        IdentityAdministration administration = mock(IdentityAdministration.class);
        when(query.findById(userId)).thenReturn(Optional.empty());

        assertThat(new IdentityUserManagement(query, administration).findUser(userId)).isEmpty();
    }

    @Test
    void delegatesRoleMembershipChangesToFrameworkAdministration() {
        FrameworkUserId userId = FrameworkUserId.parse(UUID.randomUUID().toString());
        IdentityQuery query = mock(IdentityQuery.class);
        IdentityAdministration administration = mock(IdentityAdministration.class);
        IdentityUserManagement management = new IdentityUserManagement(query, administration);

        management.assignRole(userId, "EXPENSE_READER", 3);
        management.revokeRole(userId, "EXPENSE_READER", 4);

        verify(administration).assignRole(userId, "EXPENSE_READER", 3);
        verify(administration).revokeRole(userId, "EXPENSE_READER", 4);
    }
}
