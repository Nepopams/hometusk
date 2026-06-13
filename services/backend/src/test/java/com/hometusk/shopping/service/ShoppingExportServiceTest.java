package com.hometusk.shopping.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.hometusk.households.domain.Household;
import com.hometusk.shopping.domain.ShoppingItem;
import com.hometusk.shopping.domain.ShoppingList;
import com.hometusk.users.domain.User;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ShoppingExportServiceTest {

    private ShoppingExportService exportService;
    private ShoppingList list;
    private User user;

    @BeforeEach
    void setUp() {
        exportService = new ShoppingExportService();
        Household household = new Household("Test Household");
        user = new User("ext-test", "test@example.com", "Test User");
        list = new ShoppingList(household, "Groceries");
    }

    @Test
    void exportAsText_withItems_returnsFormattedText() {
        ShoppingItem item1 = createItem("Milk", 2, "liters");
        ShoppingItem item2 = createItem("Bread", 1, null);
        ShoppingItem item3 = createItem("Eggs", 12, "pcs");

        String result = exportService.exportAsText(List.of(item1, item2, item3));

        assertThat(result).isEqualTo("Milk - 2 liters\nBread\nEggs - 12 pcs");
    }

    @Test
    void exportAsText_emptyList_returnsEmpty() {
        String result = exportService.exportAsText(List.of());
        assertThat(result).isEmpty();
    }

    @Test
    void exportAsText_itemWithQuantityNoUnit_showsQuantity() {
        ShoppingItem item = createItem("Bananas", 5, null);

        String result = exportService.exportAsText(List.of(item));

        assertThat(result).isEqualTo("Bananas - 5");
    }

    @Test
    void exportAsCsv_withItems_returnsValidCsv() {
        ShoppingItem item1 = createItem("Milk", 2, "liters");
        ShoppingItem item2 = createItem("Bread", 1, null);

        String result = exportService.exportAsCsv(List.of(item1, item2));

        assertThat(result).startsWith("name,quantity,unit,category,source,purchased\n");
        assertThat(result).contains("Milk,2,liters,,,false");
        assertThat(result).contains("Bread,1,,,,false");
    }

    @Test
    void exportAsText_withCategoryAndSource_includesReadableLabels() {
        ShoppingItem item = createItem("Milk", 1, null);
        item.setCategory("groceries");
        item.setSource("Perekrestok");

        String result = exportService.exportAsText(List.of(item));

        assertThat(result).isEqualTo("Milk [category: groceries, source: Perekrestok]");
    }

    @Test
    void exportAsCsv_withCategoryAndSource_returnsMetadataColumns() {
        ShoppingItem item = createItem("Milk", 2, "liters");
        item.setCategory("groceries");
        item.setSource("Perekrestok");

        String result = exportService.exportAsCsv(List.of(item));

        assertThat(result).contains("Milk,2,liters,groceries,Perekrestok,false");
    }

    @Test
    void exportAsCsv_withSpecialChars_escapesCorrectly() {
        ShoppingItem item = createItem("Cheese, cheddar", 1, null);

        String result = exportService.exportAsCsv(List.of(item));

        assertThat(result).contains("\"Cheese, cheddar\"");
    }

    @Test
    void exportAsCsv_withQuotes_doublesQuotes() {
        ShoppingItem item = createItem("12\" pizza", 1, null);

        String result = exportService.exportAsCsv(List.of(item));

        assertThat(result).contains("\"12\"\" pizza\"");
    }

    @Test
    void exportAsCsv_emptyList_returnsHeadersOnly() {
        String result = exportService.exportAsCsv(List.of());
        assertThat(result).isEqualTo("name,quantity,unit,category,source,purchased\n");
    }

    @Test
    void exportAsCsv_purchasedItem_showsTrue() {
        ShoppingItem item = createItem("Done item", 1, null);
        item.markPurchased();

        String result = exportService.exportAsCsv(List.of(item));

        assertThat(result).contains(",true\n");
    }

    private ShoppingItem createItem(String name, Integer quantity, String unit) {
        ShoppingItem item = new ShoppingItem(list, name, user);
        item.setQuantity(quantity);
        item.setUnit(unit);
        return item;
    }
}
