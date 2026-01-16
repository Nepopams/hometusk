package com.hometusk.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.hometusk.shopping.domain.ShoppingList;
import com.hometusk.shopping.repository.ShoppingListRepository;
import com.hometusk.users.domain.Membership;
import com.hometusk.users.domain.MembershipRole;
import com.hometusk.users.domain.User;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

@DisplayName("Notification Integration Tests")
class NotificationIntegrationTest extends IntegrationTestBase {

    @Autowired
    private ShoppingListRepository shoppingListRepository;

    @Test
    @DisplayName("Invite accepted creates notification for inviter")
    void inviteAcceptedCreatesNotificationForInviter() throws Exception {
        String token = createInviteToken();

        var request = Map.of("inviteToken", token);

        mockMvc.perform(post("/api/v1/invites/accept")
                        .with(jwtForUser(testUser2))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        JsonNode notifications = listNotifications(testHousehold.getId(), testUser);
        assertThat(hasType(notifications, "invite_accepted")).isTrue();
    }

    @Test
    @DisplayName("Task completed creates notifications for creator and assignee")
    void taskCompletedCreatesNotificationsForCreatorAndAssignee() throws Exception {
        membershipRepository.save(new Membership(testUser2, testHousehold, MembershipRole.member));

        User user3 = new User("test-user-sub-789", "charlie@test.local", "Charlie Test");
        user3 = userRepository.save(user3);
        membershipRepository.save(new Membership(user3, testHousehold, MembershipRole.member));

        var createCommand = Map.of(
                "householdId", testHousehold.getId().toString(),
                "type", "create_task",
                "payload", Map.of(
                        "title", "Mop the floor",
                        "assigneeId", testUser2.getId().toString()),
                "source", "web");

        MvcResult createResult = mockMvc.perform(post("/api/v1/commands")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createCommand)))
                .andExpect(status().isOk())
                .andReturn();

        UUID taskId = UUID.fromString(
                objectMapper.readTree(createResult.getResponse().getContentAsString()).get("result").get("taskId").asText());

        var completeCommand = Map.of(
                "householdId", testHousehold.getId().toString(),
                "type", "complete_task",
                "payload", Map.of("taskId", taskId.toString()),
                "source", "web");

        mockMvc.perform(post("/api/v1/commands")
                        .with(jwtForUser(user3))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(completeCommand)))
                .andExpect(status().isOk());

        JsonNode creatorNotifications = listNotifications(testHousehold.getId(), testUser);
        JsonNode assigneeNotifications = listNotifications(testHousehold.getId(), testUser2);

        assertThat(hasType(creatorNotifications, "task_completed")).isTrue();
        assertThat(hasType(assigneeNotifications, "task_completed")).isTrue();
    }

    @Test
    @DisplayName("Shopping item added creates notifications for household members")
    void shoppingItemAddedCreatesNotificationsForHouseholdMembers() throws Exception {
        membershipRepository.save(new Membership(testUser2, testHousehold, MembershipRole.member));

        ShoppingList list = shoppingListRepository.save(new ShoppingList(testHousehold, "Default"));

        var request = Map.of("name", "Milk", "quantity", 1);

        mockMvc.perform(post("/api/v1/households/{id}/shopping-lists/{listId}/items", testHousehold.getId(), list.getId())
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        JsonNode notifications = listNotifications(testHousehold.getId(), testUser2);
        assertThat(hasType(notifications, "shopping_item_added")).isTrue();
    }

    @Test
    @DisplayName("List notifications requires membership")
    void listNotificationsRequiresMembership() throws Exception {
        mockMvc.perform(get("/api/v1/households/{id}/notifications", testHousehold.getId())
                        .with(jwtForUser(testUser2)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"));
    }

    @Test
    @DisplayName("Mark read enforces ownership and is idempotent")
    void markReadEnforcesOwnershipAndIsIdempotent() throws Exception {
        membershipRepository.save(new Membership(testUser2, testHousehold, MembershipRole.member));

        ShoppingList list = shoppingListRepository.save(new ShoppingList(testHousehold, "Default"));
        var request = Map.of("name", "Bread");

        mockMvc.perform(post("/api/v1/households/{id}/shopping-lists/{listId}/items", testHousehold.getId(), list.getId())
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        JsonNode notifications = listNotifications(testHousehold.getId(), testUser2);
        assertThat(notifications.size()).isGreaterThan(0);
        UUID notificationId = UUID.fromString(notifications.get(0).get("id").asText());

        mockMvc.perform(post("/api/v1/notifications/{id}/read", notificationId)
                        .with(jwtForUser(testUser2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.readAt").exists());

        mockMvc.perform(post("/api/v1/notifications/{id}/read", notificationId)
                        .with(jwtForUser(testUser2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(notificationId.toString()));

        mockMvc.perform(post("/api/v1/notifications/{id}/read", notificationId)
                        .with(jwt()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("NOTIFICATION_NOT_FOUND"));
    }

    private String createInviteToken() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/households/{id}/invites", testHousehold.getId())
                        .with(jwt()))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString()).get("inviteToken").asText();
    }

    private JsonNode listNotifications(UUID householdId, User user) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/households/{id}/notifications", householdId)
                        .with(jwtForUser(user)))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private boolean hasType(JsonNode notifications, String type) {
        for (JsonNode notification : notifications) {
            if (type.equals(notification.get("type").asText())) {
                return true;
            }
        }
        return false;
    }
}
