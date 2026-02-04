package com.hometusk.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.hometusk.households.domain.HouseholdInvite;
import com.hometusk.households.domain.InviteStatus;
import com.hometusk.households.repository.HouseholdInviteRepository;
import com.hometusk.users.domain.User;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

@DisplayName("Household Invite Integration Tests")
class HouseholdInviteIntegrationTest extends IntegrationTestBase {

    @Autowired
    private HouseholdInviteRepository inviteRepository;

    @Test
    @DisplayName("Should create invite for household member")
    void createInviteHappyPath() throws Exception {
        var result = mockMvc.perform(post("/api/v1/households/{id}/invites", testHousehold.getId())
                        .with(jwt()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.inviteToken").exists())
                .andExpect(jsonPath("$.status").value("active"))
                .andReturn();

        String token = objectMapper
                .readTree(result.getResponse().getContentAsString())
                .get("inviteToken")
                .asText();
        assertThat(token).startsWith("hti_");
        assertThat(token.length()).isEqualTo(47);

        var invite = inviteRepository.findByInviteToken(token).orElseThrow();
        assertThat(invite.getStatus()).isEqualTo(InviteStatus.ACTIVE);
    }

    @Test
    @DisplayName("Should forbid invite creation for non-member")
    void createInviteForbiddenForNonMember() throws Exception {
        mockMvc.perform(post("/api/v1/households/{id}/invites", testHousehold.getId())
                        .with(jwtForUser(testUser2)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"));
    }

    @Test
    @DisplayName("Should accept invite and create membership")
    void acceptInviteHappyPath() throws Exception {
        String token = createInviteToken();

        var request = Map.of("inviteToken", token);

        mockMvc.perform(post("/api/v1/invites/accept")
                        .with(jwtForUser(testUser2))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.membership.id").exists())
                .andExpect(
                        jsonPath("$.household.id").value(testHousehold.getId().toString()));

        assertThat(membershipRepository.existsByUser_IdAndHousehold_Id(testUser2.getId(), testHousehold.getId()))
                .isTrue();

        var invite = inviteRepository.findByInviteToken(token).orElseThrow();
        assertThat(invite.getStatus()).isEqualTo(InviteStatus.REDEEMED);
    }

    @Test
    @DisplayName("Should return 410 on second accept attempt")
    void acceptInviteTwiceReturnsGone() throws Exception {
        String token = createInviteToken();
        var request = Map.of("inviteToken", token);

        mockMvc.perform(post("/api/v1/invites/accept")
                        .with(jwtForUser(testUser2))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/invites/accept")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.errorCode").value("INVITE_REDEEMED"));
    }

    @Test
    @DisplayName("Should return 200 when accepting invite as existing member")
    void acceptInviteAlreadyMemberNoOp() throws Exception {
        String token = createInviteToken();
        var request = Map.of("inviteToken", token);

        mockMvc.perform(post("/api/v1/invites/accept")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.membership.id").exists());

        var invite = inviteRepository.findByInviteToken(token).orElseThrow();
        assertThat(invite.getStatus()).isEqualTo(InviteStatus.ACTIVE);
        assertThat(invite.getRedeemedAt()).isNull();
    }

    @Test
    @DisplayName("Should return 410 for expired invite")
    void acceptInviteExpired() throws Exception {
        String token = "hti_expired_" + UUID.randomUUID();
        HouseholdInvite invite = new HouseholdInvite(
                testHousehold, testUser, token, Instant.now().minus(Duration.ofDays(1)));
        inviteRepository.save(invite);

        var request = Map.of("inviteToken", token);

        mockMvc.perform(post("/api/v1/invites/accept")
                        .with(jwtForUser(testUser2))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.errorCode").value("INVITE_EXPIRED"));

        var updated = inviteRepository.findByInviteToken(token).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(InviteStatus.EXPIRED);
    }

    @Test
    @DisplayName("Should return 404 for invalid invite token")
    void acceptInviteInvalid() throws Exception {
        var request = Map.of("inviteToken", "hti_invalid_token");

        mockMvc.perform(post("/api/v1/invites/accept")
                        .with(jwtForUser(testUser2))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("INVALID_TOKEN"));
    }

    @Test
    @Disabled("TODO: Flaky due to @Transactional isolation - concurrent threads don't see each other's changes")
    @DisplayName("Concurrent accept should result in one success and one 410")
    void concurrentAcceptIsDeterministic() throws Exception {
        String token = createInviteToken();
        String requestBody = objectMapper.writeValueAsString(Map.of("inviteToken", token));

        User user3 = new User("test-user-sub-789", "charlie@test.local", "Charlie Test");
        User savedUser3 = userRepository.save(user3);

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<Integer> first = executor.submit(() -> acceptInviteWithLatch(requestBody, testUser2, ready, start));
            Future<Integer> second =
                    executor.submit(() -> acceptInviteWithLatch(requestBody, savedUser3, ready, start));

            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            int status1 = first.get(5, TimeUnit.SECONDS);
            int status2 = second.get(5, TimeUnit.SECONDS);

            assertThat(status1).isIn(200, 410);
            assertThat(status2).isIn(200, 410);
            assertThat(status1).isNotEqualTo(status2);

        } finally {
            executor.shutdownNow();
        }
    }

    private int acceptInviteWithLatch(String requestBody, User user, CountDownLatch ready, CountDownLatch start)
            throws Exception {
        ready.countDown();
        if (!start.await(5, TimeUnit.SECONDS)) {
            return 0;
        }

        MvcResult result = mockMvc.perform(post("/api/v1/invites/accept")
                        .with(jwtForUser(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andReturn();

        return result.getResponse().getStatus();
    }

    private String createInviteToken() throws Exception {
        var result = mockMvc.perform(post("/api/v1/households/{id}/invites", testHousehold.getId())
                        .with(jwt()))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper
                .readTree(result.getResponse().getContentAsString())
                .get("inviteToken")
                .asText();
    }
}
