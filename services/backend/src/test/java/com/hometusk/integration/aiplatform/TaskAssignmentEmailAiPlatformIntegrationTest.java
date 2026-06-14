package com.hometusk.integration.aiplatform;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hometusk.notifications.email.domain.EmailNotificationStatus;
import com.hometusk.notifications.email.repository.EmailNotificationOutboxRepository;
import com.hometusk.users.domain.Membership;
import com.hometusk.users.domain.MembershipRole;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@DisplayName("Task assignment email AI Platform integration tests")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class TaskAssignmentEmailAiPlatformIntegrationTest extends AiPlatformIntegrationTestBase {

    @Autowired
    private EmailNotificationOutboxRepository outboxRepository;

    @Test
    @DisplayName("AI start_job assignment queues same pending email as manual command path")
    void aiStartJobAssignment_queuesPendingEmail() throws Exception {
        testUser2.setEmailVerified(true);
        userRepository.saveAndFlush(testUser2);
        membershipRepository.saveAndFlush(new Membership(testUser2, testHousehold, MembershipRole.member));
        stubStartJobDecision(testUser2.getId().toString(), "AI assigned task");

        var request = Map.of(
                "type",
                "create_task",
                "householdId",
                testHousehold.getId(),
                "source",
                "web",
                "payload",
                Map.of("title", "AI assigned task"));

        var response = mockMvc.perform(post("/api/v1/commands")
                        .header("X-Correlation-ID", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("executed"))
                .andExpect(
                        jsonPath("$.result.assigneeId").value(testUser2.getId().toString()))
                .andReturn();

        UUID taskId = UUID.fromString(objectMapper
                .readTree(response.getResponse().getContentAsString())
                .get("result")
                .get("taskId")
                .asText());
        var emails = outboxRepository.findByContextTypeAndContextId("task", taskId);

        assertThat(emails).hasSize(1);
        assertThat(emails.get(0).getStatus()).isEqualTo(EmailNotificationStatus.PENDING);
        assertThat(emails.get(0).getRecipientEmail()).isEqualTo(testUser2.getEmail());
        assertThat(emails.get(0).getBodyText()).contains("AI assigned task");
    }
}
