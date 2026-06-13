package com.hometusk.integration.shopping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hometusk.households.domain.Household;
import com.hometusk.integration.IntegrationTestBase;
import com.hometusk.shopping.domain.ShoppingItem;
import com.hometusk.shopping.domain.ShoppingList;
import com.hometusk.shopping.repository.ShoppingItemRepository;
import com.hometusk.shopping.repository.ShoppingListRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MvcResult;

class ShoppingExportIntegrationTest extends IntegrationTestBase {

    @Autowired
    private ShoppingListRepository shoppingListRepository;

    @Autowired
    private ShoppingItemRepository shoppingItemRepository;

    private ShoppingList shoppingList;

    @BeforeEach
    void setUpShoppingList() {
        shoppingList = new ShoppingList(testHousehold, "Groceries");
        shoppingList = shoppingListRepository.save(shoppingList);
    }

    @Test
    void export_textFormat_returnsPlainText() throws Exception {
        addItem("Milk", 2, "liters");
        addItem("Bread", 1, null);

        MvcResult result = mockMvc.perform(get(
                                "/api/v1/households/{householdId}/shopping-lists/{listId}/export",
                                testHousehold.getId(),
                                shoppingList.getId())
                        .param("format", "text")
                        .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/plain"))
                .andReturn();

        String content = result.getResponse().getContentAsString();
        assertThat(content).contains("Milk - 2 liters");
        assertThat(content).contains("Bread");
    }

    @Test
    void export_csvFormat_returnsCsvWithHeaders() throws Exception {
        ShoppingItem milk = addItem("Milk", 2, "liters");
        milk.setCategory("groceries");
        milk.setSource("Perekrestok");
        shoppingItemRepository.saveAndFlush(milk);

        mockMvc.perform(get(
                                "/api/v1/households/{householdId}/shopping-lists/{listId}/export",
                                testHousehold.getId(),
                                shoppingList.getId())
                        .param("format", "csv")
                        .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/csv"))
                .andExpect(header().exists("Content-Disposition"))
                .andExpect(content().string(containsString("name,quantity,unit,category,source,purchased")))
                .andExpect(content().string(containsString("Milk,2,liters,groceries,Perekrestok,false")));
    }

    @Test
    void export_emptyList_returns200() throws Exception {
        mockMvc.perform(get(
                                "/api/v1/households/{householdId}/shopping-lists/{listId}/export",
                                testHousehold.getId(),
                                shoppingList.getId())
                        .param("format", "text")
                        .with(jwt()))
                .andExpect(status().isOk());
    }

    @Test
    void export_wrongHousehold_returns403() throws Exception {
        Household otherHousehold = householdRepository.save(new Household("Other"));

        mockMvc.perform(get(
                                "/api/v1/households/{householdId}/shopping-lists/{listId}/export",
                                otherHousehold.getId(),
                                shoppingList.getId())
                        .with(jwt()))
                .andExpect(status().isForbidden());
    }

    @Test
    void export_invalidFormat_returns400() throws Exception {
        mockMvc.perform(get(
                                "/api/v1/households/{householdId}/shopping-lists/{listId}/export",
                                testHousehold.getId(),
                                shoppingList.getId())
                        .param("format", "xml")
                        .with(jwt()))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("INVALID_FORMAT")));
    }

    @Test
    void export_filteredByPurchasedFalse_returnsUnpurchasedOnly() throws Exception {
        ShoppingItem unpurchased = addItem("Unpurchased", 1, null);
        ShoppingItem purchased = addItem("Purchased", 1, null);
        purchased.markPurchased();
        shoppingItemRepository.saveAndFlush(purchased);

        MvcResult result = mockMvc.perform(get(
                                "/api/v1/households/{householdId}/shopping-lists/{listId}/export",
                                testHousehold.getId(),
                                shoppingList.getId())
                        .param("format", "text")
                        .param("purchased", "false")
                        .with(jwt()))
                .andExpect(status().isOk())
                .andReturn();

        String content = result.getResponse().getContentAsString();
        assertThat(content).contains("Unpurchased");
        assertThat(content).doesNotContain("Purchased");
    }

    @Test
    void export_defaultFormat_isText() throws Exception {
        addItem("Test", 1, null);

        mockMvc.perform(get(
                                "/api/v1/households/{householdId}/shopping-lists/{listId}/export",
                                testHousehold.getId(),
                                shoppingList.getId())
                        .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/plain"));
    }

    private ShoppingItem addItem(String name, Integer quantity, String unit) {
        ShoppingItem item = new ShoppingItem(shoppingList, name, testUser);
        item.setQuantity(quantity);
        item.setUnit(unit);
        shoppingList.addItem(item);
        shoppingListRepository.saveAndFlush(shoppingList);
        return item;
    }
}
