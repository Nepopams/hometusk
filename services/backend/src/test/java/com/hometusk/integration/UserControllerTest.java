package com.hometusk.integration;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.hometusk.households.domain.Household;
import com.hometusk.users.domain.EmailSource;
import com.hometusk.users.domain.Membership;
import com.hometusk.users.domain.MembershipRole;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for UserController.
 */
@DisplayName("UserController Integration Tests")
class UserControllerTest extends IntegrationTestBase {

    @Nested
    @DisplayName("GET /api/v1/users/me")
    class GetCurrentUserTests {

        @Test
        @Transactional(propagation = Propagation.NOT_SUPPORTED)
        @DisplayName("Should return user profile with household memberships")
        void authenticatedUserCanGetProfile() throws Exception {
            mockMvc.perform(get("/api/v1/users/me").with(jwt()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(testUser.getId().toString()))
                    .andExpect(jsonPath("$.email").value(testUser.getEmail()))
                    .andExpect(jsonPath("$.emailVerified").value(true))
                    .andExpect(jsonPath("$.emailSource").value("idp_claim"))
                    .andExpect(jsonPath("$.emailUpdatedAt").exists())
                    .andExpect(jsonPath("$.emailNotificationEligible").value(true))
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
        @DisplayName("Should preserve verified email state when JWT email claim is missing")
        void missingEmailClaimDoesNotClearExistingVerifiedEmail() throws Exception {
            testUser.setEmailVerified(true);
            testUser.setEmailSource(EmailSource.IDP_CLAIM);
            testUser = userRepository.saveAndFlush(testUser);

            mockMvc.perform(get("/api/v1/users/me")
                            .with(jwtWithoutEmail(testUser.getExternalId(), testUser.getDisplayName())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value(testUser.getEmail()))
                    .andExpect(jsonPath("$.emailVerified").value(true))
                    .andExpect(jsonPath("$.emailSource").value("idp_claim"))
                    .andExpect(jsonPath("$.emailNotificationEligible").value(true));
        }

        @Test
        @DisplayName("Should mark unverified email claim as not eligible for email notifications")
        void unverifiedEmailClaimIsNotNotificationEligible() throws Exception {
            mockMvc.perform(get("/api/v1/users/me")
                            .with(jwtWithEmail(testUser.getExternalId(), testUser.getEmail(), false)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value(testUser.getEmail()))
                    .andExpect(jsonPath("$.emailVerified").value(false))
                    .andExpect(jsonPath("$.emailSource").value("idp_claim"))
                    .andExpect(jsonPath("$.emailNotificationEligible").value(false));
        }

        @Test
        @DisplayName("Should normalize email claim before storing profile email")
        void emailClaimIsNormalized() throws Exception {
            String mixedCaseEmail = "  Alice." + testUser.getId() + "@Example.COM  ";
            String expectedEmail = ("alice." + testUser.getId() + "@example.com").toLowerCase();

            mockMvc.perform(get("/api/v1/users/me").with(jwtWithEmail(testUser.getExternalId(), mixedCaseEmail, true)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value(expectedEmail))
                    .andExpect(jsonPath("$.emailVerified").value(true))
                    .andExpect(jsonPath("$.emailSource").value("idp_claim"))
                    .andExpect(jsonPath("$.emailNotificationEligible").value(true));
        }

        @Test
        @DisplayName("Should treat changed email without verification claim as unverified")
        void changedEmailWithoutVerificationClaimIsUnverified() throws Exception {
            testUser.setEmailVerified(true);
            testUser.setEmailSource(EmailSource.IDP_CLAIM);
            testUser = userRepository.saveAndFlush(testUser);

            String changedEmail = "changed-" + testUser.getId() + "@test.local";

            mockMvc.perform(get("/api/v1/users/me")
                            .with(jwtWithEmailWithoutVerification(
                                    testUser.getExternalId(), changedEmail, testUser.getDisplayName())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value(changedEmail))
                    .andExpect(jsonPath("$.emailVerified").value(false))
                    .andExpect(jsonPath("$.emailSource").value("idp_claim"))
                    .andExpect(jsonPath("$.emailNotificationEligible").value(false));
        }

        @Test
        @DisplayName("Should create profile from social broker JWT using Keycloak subject")
        void socialBrokerJwtCreatesProfileFromKeycloakSubject() throws Exception {
            String externalId = socialSubject("yandex");

            mockMvc.perform(get("/api/v1/users/me")
                            .with(socialJwtWithoutVerification(
                                    externalId, "  Social.User@Yandex.RU  ", "Yandex Social User")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.externalId").value(externalId))
                    .andExpect(jsonPath("$.email").value("social.user@yandex.ru"))
                    .andExpect(jsonPath("$.emailVerified").value(false))
                    .andExpect(jsonPath("$.emailSource").value("idp_claim"))
                    .andExpect(jsonPath("$.emailNotificationEligible").value(false))
                    .andExpect(jsonPath("$.displayName").value("Yandex Social User"))
                    .andExpect(jsonPath("$.households").isArray())
                    .andExpect(jsonPath("$.households.length()").value(0));
        }

        @Test
        @DisplayName("Should not merge social profiles by email")
        void socialBrokerJwtDoesNotMergeUsersByEmail() throws Exception {
            String externalId = socialSubject("yandex");

            mockMvc.perform(get("/api/v1/users/me")
                            .with(socialJwt(externalId, testUser.getEmail(), true, "Another Social User")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(not(testUser.getId().toString())))
                    .andExpect(jsonPath("$.externalId").value(externalId))
                    .andExpect(jsonPath("$.email").value(testUser.getEmail()))
                    .andExpect(jsonPath("$.emailVerified").value(true))
                    .andExpect(jsonPath("$.emailNotificationEligible").value(true))
                    .andExpect(jsonPath("$.households.length()").value(0));
        }

        @Test
        @DisplayName("Should allow social login when email claim is missing")
        void socialBrokerJwtWithMissingEmailStillLogsIn() throws Exception {
            String externalId = socialSubject("yandex");

            mockMvc.perform(get("/api/v1/users/me").with(jwtWithoutEmail(externalId, "Yandex No Email")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.externalId").value(externalId))
                    .andExpect(jsonPath("$.email").value(nullValue()))
                    .andExpect(jsonPath("$.emailVerified").value(false))
                    .andExpect(jsonPath("$.emailSource").value("unknown"))
                    .andExpect(jsonPath("$.emailNotificationEligible").value(false))
                    .andExpect(jsonPath("$.displayName").value("Yandex No Email"));
        }

        @Test
        @DisplayName("Should reject unauthenticated request")
        void unauthenticatedRequestRejected() throws Exception {
            mockMvc.perform(get("/api/v1/users/me")).andExpect(status().isUnauthorized());
        }

        private RequestPostProcessor jwtWithEmail(String externalId, String email, boolean emailVerified) {
            return SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt -> jwt.subject(externalId)
                    .claim("email", email)
                    .claim("email_verified", emailVerified)
                    .claim("name", "Test User"));
        }

        private RequestPostProcessor jwtWithEmailWithoutVerification(
                String externalId, String email, String displayName) {
            return SecurityMockMvcRequestPostProcessors.jwt()
                    .jwt(jwt -> jwt.subject(externalId).claim("email", email).claim("name", displayName));
        }

        private RequestPostProcessor jwtWithoutEmail(String externalId, String displayName) {
            return SecurityMockMvcRequestPostProcessors.jwt()
                    .jwt(jwt -> jwt.subject(externalId).claim("name", displayName));
        }

        private RequestPostProcessor socialJwt(
                String externalId, String email, boolean emailVerified, String displayName) {
            return SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt -> jwt.subject(externalId)
                    .claim("email", email)
                    .claim("email_verified", emailVerified)
                    .claim("name", displayName));
        }

        private RequestPostProcessor socialJwtWithoutVerification(String externalId, String email, String displayName) {
            return SecurityMockMvcRequestPostProcessors.jwt()
                    .jwt(jwt -> jwt.subject(externalId).claim("email", email).claim("name", displayName));
        }

        private String socialSubject(String provider) {
            return "kc-social-" + provider + "-" + UUID.randomUUID();
        }
    }
}
