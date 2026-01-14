package com.hometusk.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.hometusk.households.domain.Household;
import com.hometusk.households.domain.Zone;
import com.hometusk.users.domain.Membership;
import com.hometusk.users.domain.MembershipRole;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/**
 * Integration tests for HouseholdController.
 */
@DisplayName("HouseholdController Integration Tests")
class HouseholdControllerTest extends IntegrationTestBase {

    @Nested
    @DisplayName("POST /api/v1/households")
    class CreateHouseholdTests {

        @Test
        @DisplayName("Should create household and add creator as admin")
        void createHouseholdAutoAddsMemberAsAdmin() throws Exception {
            var request = Map.of("name", "New Family House");

            var result = mockMvc.perform(post("/api/v1/households")
                            .with(jwt())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").exists())
                    .andExpect(jsonPath("$.name").value("New Family House"))
                    .andReturn();

            // Extract household ID from response
            var response = objectMapper.readTree(result.getResponse().getContentAsString());
            var householdId = java.util.UUID.fromString(response.get("id").asText());

            // Verify creator is auto-added as admin
            var memberships = membershipRepository.findByHouseholdId(householdId);
            assertThat(memberships).hasSize(1);
            assertThat(memberships.get(0).getUser().getId()).isEqualTo(testUser.getId());
            assertThat(memberships.get(0).getRole()).isEqualTo(MembershipRole.admin);
        }

        @Test
        @DisplayName("Should trim and validate household name")
        void createHouseholdTrimsName() throws Exception {
            var request = Map.of("name", "  Trimmed Name  ");

            mockMvc.perform(post("/api/v1/households")
                            .with(jwt())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.name").value("Trimmed Name"));
        }

        @Test
        @DisplayName("Should reject blank household name")
        void createHouseholdRejectsBlankName() throws Exception {
            var request = Map.of("name", "   ");

            mockMvc.perform(post("/api/v1/households")
                            .with(jwt())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should reject household name longer than 80 characters")
        void createHouseholdRejectsTooLongName() throws Exception {
            var longName = "A".repeat(81);
            var request = Map.of("name", longName);

            mockMvc.perform(post("/api/v1/households")
                            .with(jwt())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/households/{id}/members")
    class ListMembersTests {

        @Test
        @DisplayName("Should list household members")
        void listMembersReturnsHouseholdMembers() throws Exception {
            // Add testUser2 to household
            Membership membership2 = new Membership(testUser2, testHousehold, MembershipRole.member);
            membershipRepository.save(membership2);

            mockMvc.perform(get("/api/v1/households/{id}/members", testHousehold.getId())
                            .with(jwt()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].userId").exists())
                    .andExpect(jsonPath("$[0].displayName").exists())
                    .andExpect(jsonPath("$[0].role").exists());
        }

        @Test
        @DisplayName("Should reject listing members for non-member")
        void listMembersRejectsNonMember() throws Exception {
            mockMvc.perform(get("/api/v1/households/{id}/members", testHousehold.getId())
                            .with(jwtForUser(testUser2)))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/households/{id}/zones")
    class ListZonesTests {

        @Test
        @DisplayName("Should list household zones")
        void listZonesReturnsHouseholdZones() throws Exception {
            // testZone is already created in base setup
            mockMvc.perform(get("/api/v1/households/{id}/zones", testHousehold.getId())
                            .with(jwt()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].id").value(testZone.getId().toString()))
                    .andExpect(jsonPath("$[0].name").value("Kitchen"));
        }

        @Test
        @DisplayName("Should reject listing zones for non-member")
        void listZonesRejectsNonMember() throws Exception {
            mockMvc.perform(get("/api/v1/households/{id}/zones", testHousehold.getId())
                            .with(jwtForUser(testUser2)))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/households/{id}/zones")
    class CreateZoneTests {

        @Test
        @DisplayName("Should create zone in household")
        void createZoneSuccess() throws Exception {
            var request = Map.of("name", "Living Room");

            mockMvc.perform(post("/api/v1/households/{id}/zones", testHousehold.getId())
                            .with(jwt())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").exists())
                    .andExpect(jsonPath("$.name").value("Living Room"));

            // Verify zone was created
            var zones = zoneRepository.findByHouseholdId(testHousehold.getId());
            assertThat(zones).hasSize(2);
            assertThat(zones).anyMatch(z -> z.getName().equals("Living Room"));
        }

        @Test
        @DisplayName("Should trim zone name")
        void createZoneTrimsName() throws Exception {
            var request = Map.of("name", "  Bathroom  ");

            mockMvc.perform(post("/api/v1/households/{id}/zones", testHousehold.getId())
                            .with(jwt())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.name").value("Bathroom"));
        }

        @Test
        @DisplayName("Should return existing zone if duplicate name (idempotent)")
        void createZoneIsIdempotent() throws Exception {
            var request = Map.of("name", "Kitchen");

            // Create zone with same name as existing testZone
            mockMvc.perform(post("/api/v1/households/{id}/zones", testHousehold.getId())
                            .with(jwt())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(testZone.getId().toString()));

            // Verify no duplicate zones created
            var zones = zoneRepository.findByHouseholdId(testHousehold.getId());
            assertThat(zones).hasSize(1);
        }

        @Test
        @DisplayName("Should reject zone creation for non-member")
        void createZoneRejectsNonMember() throws Exception {
            var request = Map.of("name", "Hacked Zone");

            mockMvc.perform(post("/api/v1/households/{id}/zones", testHousehold.getId())
                            .with(jwtForUser(testUser2))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should reject blank zone name")
        void createZoneRejectsBlankName() throws Exception {
            var request = Map.of("name", "   ");

            mockMvc.perform(post("/api/v1/households/{id}/zones", testHousehold.getId())
                            .with(jwt())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }
}
