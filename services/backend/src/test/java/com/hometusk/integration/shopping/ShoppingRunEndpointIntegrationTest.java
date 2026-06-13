package com.hometusk.integration.shopping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hometusk.integration.IntegrationTestBase;
import com.hometusk.shopping.domain.ShoppingItem;
import com.hometusk.shopping.domain.ShoppingList;
import com.hometusk.shopping.domain.ShoppingRun;
import com.hometusk.shopping.domain.ShoppingRunStatus;
import com.hometusk.shopping.repository.ShoppingItemRepository;
import com.hometusk.shopping.repository.ShoppingListRepository;
import com.hometusk.shopping.repository.ShoppingRunRepository;
import com.hometusk.shopping.service.ShoppingRunService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

class ShoppingRunEndpointIntegrationTest extends IntegrationTestBase {

    @Autowired
    private ShoppingListRepository shoppingListRepository;

    @Autowired
    private ShoppingItemRepository shoppingItemRepository;

    @Autowired
    private ShoppingRunRepository shoppingRunRepository;

    @Autowired
    private ShoppingRunService shoppingRunService;

    private ShoppingList shoppingList;

    @BeforeEach
    void setUpList() {
        shoppingList = new ShoppingList(testHousehold, "Groceries");
        ShoppingItem unpurchased = new ShoppingItem(shoppingList, "Milk", testUser);
        unpurchased.setCategory("groceries");
        unpurchased.setSource("Perekrestok");
        ShoppingItem purchased = new ShoppingItem(shoppingList, "Bread", testUser);
        purchased.markPurchased();
        shoppingList.addItem(unpurchased);
        shoppingList.addItem(purchased);
        shoppingList = shoppingListRepository.save(shoppingList);
    }

    @Test
    void createRun_success() throws Exception {
        var request = new com.hometusk.shopping.dto.CreateShoppingRunRequest(shoppingList.getId());

        mockMvc.perform(post("/api/v1/households/{householdId}/shopping-runs", testHousehold.getId())
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].category").value("groceries"))
                .andExpect(jsonPath("$.items[0].source").value("Perekrestok"))
                .andExpect(jsonPath("$.listName").value("Groceries"));

        List<ShoppingRun> runs = shoppingRunRepository.findByHousehold_IdOrderByCreatedAtDesc(testHousehold.getId());
        assertThat(runs).hasSize(1);
        assertThat(runs.get(0).getItems()).hasSize(1);
        assertThat(runs.get(0).getItems().get(0).getCategory()).isEqualTo("groceries");
        assertThat(runs.get(0).getItems().get(0).getSource()).isEqualTo("Perekrestok");
    }

    @Test
    void createRun_emptyList_returns400() throws Exception {
        ShoppingList emptyList = new ShoppingList(testHousehold, "Empty");
        ShoppingItem item = new ShoppingItem(emptyList, "Only", testUser);
        item.markPurchased();
        emptyList.addItem(item);
        emptyList = shoppingListRepository.save(emptyList);

        var request = new com.hometusk.shopping.dto.CreateShoppingRunRequest(emptyList.getId());

        mockMvc.perform(post("/api/v1/households/{householdId}/shopping-runs", testHousehold.getId())
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listRuns_filterByStatus() throws Exception {
        ShoppingRun activeRun =
                shoppingRunService.createRun(testHousehold.getId(), shoppingList.getId(), testUser.getId());
        ShoppingRun completedRun =
                shoppingRunService.createRun(testHousehold.getId(), shoppingList.getId(), testUser.getId());
        completedRun.close(ShoppingRunStatus.COMPLETED);
        shoppingRunRepository.save(completedRun);

        mockMvc.perform(get("/api/v1/households/{householdId}/shopping-runs", testHousehold.getId())
                        .with(jwt())
                        .param("status", "COMPLETED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(completedRun.getId().toString()));

        mockMvc.perform(get("/api/v1/households/{householdId}/shopping-runs", testHousehold.getId())
                        .with(jwt())
                        .param("status", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(activeRun.getId().toString()));
    }

    @Test
    void getRun_returnsAllItems() throws Exception {
        ShoppingRun run = shoppingRunService.createRun(testHousehold.getId(), shoppingList.getId(), testUser.getId());

        mockMvc.perform(get(
                                "/api/v1/households/{householdId}/shopping-runs/{runId}",
                                testHousehold.getId(),
                                run.getId())
                        .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void closeRun_completed() throws Exception {
        ShoppingRun run = shoppingRunService.createRun(testHousehold.getId(), shoppingList.getId(), testUser.getId());
        var request = new com.hometusk.shopping.dto.CloseShoppingRunRequest(ShoppingRunStatus.COMPLETED);

        mockMvc.perform(post(
                                "/api/v1/households/{householdId}/shopping-runs/{runId}/close",
                                testHousehold.getId(),
                                run.getId())
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.closedAt").exists());
    }

    @Test
    void closeRun_alreadyClosed_sameStatus_idempotent() throws Exception {
        ShoppingRun run = shoppingRunService.createRun(testHousehold.getId(), shoppingList.getId(), testUser.getId());
        run.close(ShoppingRunStatus.CANCELLED);
        shoppingRunRepository.save(run);

        var request = new com.hometusk.shopping.dto.CloseShoppingRunRequest(ShoppingRunStatus.CANCELLED);
        mockMvc.perform(post(
                                "/api/v1/households/{householdId}/shopping-runs/{runId}/close",
                                testHousehold.getId(),
                                run.getId())
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void closeRun_alreadyClosed_differentStatus_returns409() throws Exception {
        ShoppingRun run = shoppingRunService.createRun(testHousehold.getId(), shoppingList.getId(), testUser.getId());
        run.close(ShoppingRunStatus.COMPLETED);
        shoppingRunRepository.save(run);

        var request = new com.hometusk.shopping.dto.CloseShoppingRunRequest(ShoppingRunStatus.CANCELLED);
        mockMvc.perform(post(
                                "/api/v1/households/{householdId}/shopping-runs/{runId}/close",
                                testHousehold.getId(),
                                run.getId())
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void updateItem_purchased_syncsToList() throws Exception {
        ShoppingRun run = shoppingRunService.createRun(testHousehold.getId(), shoppingList.getId(), testUser.getId());
        UUID runItemId = run.getItems().get(0).getId();
        UUID originalItemId = run.getItems().get(0).getOriginalItemId();

        var request = new com.hometusk.shopping.dto.UpdateRunItemRequest(true, true);

        mockMvc.perform(patch(
                                "/api/v1/households/{householdId}/shopping-runs/{runId}/items/{itemId}",
                                testHousehold.getId(),
                                run.getId(),
                                runItemId)
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.purchased").value(true));

        ShoppingItem original = shoppingItemRepository.findById(originalItemId).orElseThrow();
        assertThat(original.isPurchased()).isTrue();
    }

    @Test
    void updateItem_closedRun_returns400() throws Exception {
        ShoppingRun run = shoppingRunService.createRun(testHousehold.getId(), shoppingList.getId(), testUser.getId());
        run.close(ShoppingRunStatus.COMPLETED);
        shoppingRunRepository.save(run);

        UUID runItemId = run.getItems().get(0).getId();
        var request = new com.hometusk.shopping.dto.UpdateRunItemRequest(true, true);

        mockMvc.perform(patch(
                                "/api/v1/households/{householdId}/shopping-runs/{runId}/items/{itemId}",
                                testHousehold.getId(),
                                run.getId(),
                                runItemId)
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void householdBoundary_returns403() throws Exception {
        var request = new com.hometusk.shopping.dto.CreateShoppingRunRequest(shoppingList.getId());
        UUID otherHouseholdId = householdRepository
                .save(new com.hometusk.households.domain.Household("Other"))
                .getId();

        mockMvc.perform(post("/api/v1/households/{householdId}/shopping-runs", otherHouseholdId)
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}
