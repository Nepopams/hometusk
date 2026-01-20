package com.hometusk.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.hometusk.households.domain.Household;
import com.hometusk.users.domain.Membership;
import com.hometusk.users.domain.MembershipRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for UserController.
 */
@DisplayName("UserController Integration Tests")
class UserControllerTest extends IntegrationTestBase {

    @Nested
    @DisplayName("GET /api/v1/users/me")
    class GetCurrentUserTests {

        @Test
        @DisplayName("Should return user profile with household memberships")
        void authenticatedUserCanGetProfile() throws Exception {
            mockMvc.perform(get("/api/v1/users/me").with(jwt()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(testUser.getId().toString()))
                    .andExpect(jsonPath("$.email").value(testUser.getEmail()))
                    .andExpect(jsonPath("$.displayName").value(testUser.getDisplayName()))
                    .andExpect(jsonPath("$.households").isArray())
                    .andExpect(jsonPath("$.households[0].id")
                            .value(testHousehold.getId().toString()))
                    .andExpect(jsonPath("$.households[0].name").value(testHousehold.getName()))
                    .andExpect(jsonPath("$.households[0].role").value("admin"));
        }

        @Test
        @DisplayName("Should include multiple household memberships")
        void profileIncludesMultipleHouseholdMemberships() throws Exception {
            // Add testUser to another household
            Household anotherHousehold = new Household("Another Household");
            anotherHousehold = householdRepository.save(anotherHousehold);

            Membership membership2 = new Membership(testUser, anotherHousehold, MembershipRole.member);
            membershipRepository.save(membership2);

            mockMvc.perform(get("/api/v1/users/me").with(jwt()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.households.length()").value(2));
        }

        @Test
        @DisplayName("Should return empty households array for user with no memberships")
        void profileWithNoMembershipsReturnsEmptyHouseholds() throws Exception {
            // testUser2 has no memberships
            mockMvc.perform(get("/api/v1/users/me").with(jwtForUser(testUser2)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(testUser2.getId().toString()))
                    .andExpect(jsonPath("$.households").isArray())
                    .andExpect(jsonPath("$.households.length()").value(0));
        }

        @Test
        @DisplayName("Should reject unauthenticated request")
        void unauthenticatedRequestRejected() throws Exception {
            mockMvc.perform(get("/api/v1/users/me")).andExpect(status().isUnauthorized());
        }
    }
}
