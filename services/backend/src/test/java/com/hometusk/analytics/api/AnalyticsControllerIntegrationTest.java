package com.hometusk.analytics.api;

import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hometusk.integration.IntegrationTestBase;
import com.hometusk.tasks.domain.Task;
import com.hometusk.tasks.repository.TaskRepository;
import com.hometusk.users.domain.Membership;
import com.hometusk.users.domain.MembershipRole;
import java.lang.reflect.Field;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@DisplayName("AnalyticsController Integration Tests")
class AnalyticsControllerIntegrationTest extends IntegrationTestBase {

    @Autowired
    private TaskRepository taskRepository;

    @Test
    @DisplayName("Should return analytics for household member")
    void getAnalytics_asMember_returnsData() throws Exception {
        Membership membership2 = new Membership(testUser2, testHousehold, MembershipRole.member);
        membershipRepository.save(membership2);

        Instant now = Instant.now();

        Task completed = new Task(testHousehold, "Completed Task", testUser);
        completed.setAssignee(testUser);
        completed.setZone(testZone);
        completed.complete();
        setCompletedAt(completed, now.minus(1, ChronoUnit.DAYS));
        taskRepository.save(completed);

        Task overdue = new Task(testHousehold, "Overdue Task", testUser);
        overdue.setAssignee(testUser2);
        overdue.setZone(testZone);
        overdue.setDeadline(now.minus(2, ChronoUnit.DAYS));
        taskRepository.save(overdue);

        Task open = new Task(testHousehold, "Open Task", testUser);
        open.setAssignee(testUser);
        open.setZone(testZone);
        open.setDeadline(now.plus(2, ChronoUnit.DAYS));
        taskRepository.save(open);

        mockMvc.perform(get("/api/v1/households/{id}/analytics", testHousehold.getId())
                        .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.period").value("7d"))
                .andExpect(jsonPath("$.perMember.length()").value(2))
                .andExpect(jsonPath("$.perMember[?(@.memberName == 'Alice Test')].completedCount")
                        .value(contains(1)))
                .andExpect(jsonPath("$.perMember[?(@.memberName == 'Alice Test')].openCount")
                        .value(contains(1)))
                .andExpect(jsonPath("$.perMember[?(@.memberName == 'Alice Test')].overdueCount")
                        .value(contains(0)))
                .andExpect(jsonPath("$.perMember[?(@.memberName == 'Bob Test')].completedCount")
                        .value(contains(0)))
                .andExpect(jsonPath("$.perMember[?(@.memberName == 'Bob Test')].openCount")
                        .value(contains(0)))
                .andExpect(jsonPath("$.perMember[?(@.memberName == 'Bob Test')].overdueCount")
                        .value(contains(1)))
                .andExpect(jsonPath("$.perZone.length()").value(1))
                .andExpect(jsonPath("$.perZone[?(@.zoneName == 'Kitchen')].completedCount")
                        .value(contains(1)))
                .andExpect(jsonPath("$.perZone[?(@.zoneName == 'Kitchen')].overdueCount")
                        .value(contains(1)));
    }

    @Test
    @DisplayName("Should reject analytics for non-member")
    void getAnalytics_notMember_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/households/{id}/analytics", testHousehold.getId())
                        .with(jwtForUser(testUser2)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should require authentication")
    void getAnalytics_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/households/{id}/analytics", testHousehold.getId()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should apply period filter for completed tasks")
    void getAnalytics_periodFilter_filtersCorrectly() throws Exception {
        Instant now = Instant.now();

        Task recent = new Task(testHousehold, "Recent Completed", testUser);
        recent.setAssignee(testUser);
        recent.complete();
        setCompletedAt(recent, now.minus(10, ChronoUnit.DAYS));
        taskRepository.save(recent);

        Task old = new Task(testHousehold, "Old Completed", testUser);
        old.setAssignee(testUser);
        old.complete();
        setCompletedAt(old, now.minus(40, ChronoUnit.DAYS));
        taskRepository.save(old);

        mockMvc.perform(get("/api/v1/households/{id}/analytics", testHousehold.getId())
                        .with(jwt())
                        .param("period", "30d"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.period").value("30d"))
                .andExpect(jsonPath("$.perMember[?(@.memberName == 'Alice Test')].completedCount")
                        .value(contains(1)));
    }

    @Test
    @DisplayName("Should limit overdueTop to 5 tasks ordered by days overdue")
    void getAnalytics_overdueTop_limitedTo5() throws Exception {
        Instant now = Instant.now();

        for (int days = 1; days <= 6; days++) {
            Task overdue = new Task(testHousehold, "Overdue " + days, testUser);
            overdue.setAssignee(testUser);
            overdue.setDeadline(now.minus(days, ChronoUnit.DAYS));
            taskRepository.save(overdue);
        }

        mockMvc.perform(get("/api/v1/households/{id}/analytics", testHousehold.getId())
                        .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.overdueTop.length()").value(5))
                .andExpect(jsonPath("$.overdueTop[*].daysOverdue").value(contains(6, 5, 4, 3, 2)));
    }

    @Test
    @DisplayName("Should return null balance when no completed tasks")
    void getAnalytics_noTasks_returnsNullBalance() throws Exception {
        mockMvc.perform(get("/api/v1/households/{id}/analytics", testHousehold.getId())
                        .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fairness.gini").doesNotExist())
                .andExpect(jsonPath("$.fairness.balance").doesNotExist());
    }

    @Test
    @DisplayName("Should return perfect balance for equal distribution")
    void getAnalytics_equalDistribution_returns100Balance() throws Exception {
        Membership membership2 = new Membership(testUser2, testHousehold, MembershipRole.member);
        membershipRepository.save(membership2);

        Instant now = Instant.now();

        Task t1 = new Task(testHousehold, "Done A", testUser);
        t1.setAssignee(testUser);
        t1.complete();
        setCompletedAt(t1, now.minus(1, ChronoUnit.DAYS));
        taskRepository.save(t1);

        Task t2 = new Task(testHousehold, "Done B", testUser);
        t2.setAssignee(testUser2);
        t2.complete();
        setCompletedAt(t2, now.minus(1, ChronoUnit.DAYS));
        taskRepository.save(t2);

        mockMvc.perform(get("/api/v1/households/{id}/analytics", testHousehold.getId())
                        .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fairness.balance").value(100));
    }

    private void setCompletedAt(Task task, Instant completedAt) {
        try {
            Field field = Task.class.getDeclaredField("completedAt");
            field.setAccessible(true);
            field.set(task, completedAt);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
