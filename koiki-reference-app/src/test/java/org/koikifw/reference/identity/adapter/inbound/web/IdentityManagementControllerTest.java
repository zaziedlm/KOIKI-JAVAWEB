package org.koikifw.reference.identity.adapter.inbound.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.koikifw.identity.FrameworkUserId;
import org.koikifw.identity.UserStatus;
import org.koikifw.reference.identity.application.IdentityUserManagement;
import org.koikifw.reference.identity.application.IdentityUserView;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

class IdentityManagementControllerTest {

    private IdentityUserManagement management;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        management = mock(IdentityUserManagement.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new IdentityManagementController(management))
                .build();
    }

    @Test
    void rendersLookupFormWithoutEnumeratingUsers() throws Exception {
        mockMvc.perform(get("/identity/users"))
                .andExpect(status().isOk())
                .andExpect(view().name("identity/users"));
    }

    @Test
    void redirectsCanonicalUserIdLookupToDetail() throws Exception {
        String userId = UUID.randomUUID().toString();

        mockMvc.perform(get("/identity/users").param("userId", userId))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/identity/users/" + userId));
    }

    @Test
    void rendersReferenceOwnedUserView() throws Exception {
        FrameworkUserId userId = FrameworkUserId.parse(UUID.randomUUID().toString());
        IdentityUserView view = new IdentityUserView(
                userId.toString(),
                "admin-visible@example.test",
                UserStatus.ACTIVE,
                List.of("IDENTITY_ADMIN"),
                List.of("IDENTITY:ADMIN"),
                2);
        when(management.findUser(userId)).thenReturn(Optional.of(view));

        mockMvc.perform(get("/identity/users/{userId}", userId))
                .andExpect(status().isOk())
                .andExpect(view().name("identity/user-detail"))
                .andExpect(model().attribute("user", view));
    }

    @Test
    void rejectsMalformedUserIdWithoutExposingParserFailure() throws Exception {
        mockMvc.perform(get("/identity/users/not-a-user-id"))
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertThat(result.getResolvedException())
                        .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                                assertThat(exception.getReason()).isEqualTo("Invalid user identifier.")));
        verifyNoInteractions(management);
    }

    @Test
    void returnsNotFoundForMissingUser() throws Exception {
        FrameworkUserId userId = FrameworkUserId.parse(UUID.randomUUID().toString());
        when(management.findUser(userId)).thenReturn(Optional.empty());

        mockMvc.perform(get("/identity/users/{userId}", userId))
                .andExpect(status().isNotFound())
                .andExpect(result -> assertThat(result.getResolvedException())
                        .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                                assertThat(exception.getReason()).isEqualTo("Identity user was not found.")));
    }
}
