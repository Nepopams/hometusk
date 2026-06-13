package com.hometusk.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hometusk.notifications.email.service.EmailNotificationRequest;
import com.hometusk.notifications.email.service.EmailNotificationService;
import com.hometusk.tasks.repository.TaskRepository;
import com.hometusk.users.domain.Membership;
import com.hometusk.users.domain.MembershipRole;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@DisplayName("Task assignment email degraded integration tests")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class TaskAssignmentEmailDegradedIntegrationTest extends IntegrationTestBase {

    @Autowired
    private TaskRepository taskRepository;

    @MockBean
    private EmailNotificationService emailNotificationService;

    @Test
    @DisplayName("email enqueue failure does not break task assignment")
    void emailEnqueueFailure_doesNotBreakTaskAssignment() throws Exception {
        testUser2.setEmailVerified(true);
        userRepository.saveAndFlush(testUser2);
        membershipRepository.saveAndFlush(new Membership(testUser2, testHousehold, MembershipRole.member));
        doThrow(new IllegalStateException("outbox unavailable"))
                .when(emailNotificationService)
                .enqueue(any(EmailNotificationRequest.class));

        var request = Map.of(
                "householdId",
                testHousehold.getId().toString(),
                "type",
                "create_task",
                "payload",
                Map.of(
                        "title",
                        "Email degraded task",
                        "assigneeId",
                        testUser2.getId().toString()),
                "source",
                "api");

        mockMvc.perform(post("/api/v1/commands")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("executed"))
                .andExpect(
                        jsonPath("$.result.assigneeId").value(testUser2.getId().toString()));

        var tasks = taskRepository.findByHousehold_IdOrderByCreatedAtDesc(testHousehold.getId());
        assertThat(tasks).hasSize(1);
        assertThat(tasks.get(0).getAssigneeId()).isEqualTo(testUser2.getId());
        verify(emailNotificationService).enqueue(any(EmailNotificationRequest.class));
    }
}
