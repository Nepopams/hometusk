# Codex APPLY Prompt — ST-1303: Export Shopping List

## Directive
**IMPLEMENTATION phase.** Create/modify files as specified. Follow patterns from PLAN findings.

---

## Context

**Story:** ST-1303 — Export Shopping List (Text/CSV)
**Points:** 3
**Epic:** EP-013 (Shopping Marketplaces)

**Goal:** Add `GET /api/v1/households/{householdId}/shopping-lists/{listId}/export` endpoint.

---

## Sources of Truth

```
docs/planning/workpacks/ST-1303/workpack.md
docs/contracts/http/shopping-marketplaces.openapi.yaml (lines 255-319)
```

---

## PLAN Findings (use these facts)

### Service Methods
- `shoppingService.getItemsInList(UUID listId, UUID householdId)` — all items
- `shoppingService.getUnpurchasedItemsInList(UUID listId, UUID householdId)` — unpurchased only
- **No method for purchased=true** — use in-memory filter from getItemsInList()

### IDOR Prevention Pattern
```java
CurrentUser currentUser = userResolver.resolveCurrentUser();
membershipService.requireMembership(currentUser.id(), householdId);
// service validates list belongs to household
```

### ShoppingItem Getters
- `getName()`, `getQuantity()`, `getUnit()`, `isPurchased()`, `getPurchasedAt()`

### Test Pattern
- Extend `IntegrationTestBase`
- Use `testHousehold`, `testUser` from base
- Create ShoppingList via `new ShoppingList(household, name)` + repository.save()
- Create items via `new ShoppingItem(list, name, user)` + `list.addItem(item)`
- Use `mockMvc` + `jwt()` for endpoint calls

### CSV Escaping
- **Manual RFC 4180 escaping** (no library)
- Rules: quote fields containing comma/quote/newline, double quotes for escaping

---

## Implementation Steps

### Step 1: Create ShoppingExportService

**File:** `services/backend/src/main/java/com/hometusk/shopping/service/ShoppingExportService.java`

```java
package com.hometusk.shopping.service;

import com.hometusk.shopping.domain.ShoppingItem;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ShoppingExportService {

    /**
     * Export items as plain text.
     * Format: "name" or "name - quantity unit"
     */
    public String exportAsText(List<ShoppingItem> items) {
        if (items.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (ShoppingItem item : items) {
            sb.append(formatTextLine(item)).append("\n");
        }
        // Remove trailing newline
        return sb.toString().trim();
    }

    /**
     * Export items as CSV (RFC 4180 compliant).
     * Headers: name,quantity,unit,purchased
     */
    public String exportAsCsv(List<ShoppingItem> items) {
        StringBuilder sb = new StringBuilder();
        sb.append("name,quantity,unit,purchased\n");
        for (ShoppingItem item : items) {
            sb.append(escapeCsvField(item.getName())).append(",");
            sb.append(item.getQuantity() != null ? item.getQuantity() : 1).append(",");
            sb.append(escapeCsvField(item.getUnit())).append(",");
            sb.append(item.isPurchased()).append("\n");
        }
        return sb.toString();
    }

    private String formatTextLine(ShoppingItem item) {
        String name = item.getName();
        Integer qty = item.getQuantity();
        String unit = item.getUnit();

        if ((qty == null || qty == 1) && (unit == null || unit.isBlank())) {
            return name;
        }

        StringBuilder line = new StringBuilder(name);
        line.append(" - ");
        if (qty != null) {
            line.append(qty);
        }
        if (unit != null && !unit.isBlank()) {
            if (qty != null) {
                line.append(" ");
            }
            line.append(unit);
        }
        return line.toString();
    }

    /**
     * RFC 4180 CSV field escaping.
     * Quote if contains comma, quote, or newline. Double internal quotes.
     */
    private String escapeCsvField(String value) {
        if (value == null) {
            return "";
        }
        boolean needsQuoting = value.contains(",") || value.contains("\"") || value.contains("\n");
        if (needsQuoting) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
```

### Step 2: Add Export Endpoint to ShoppingController

**File:** `services/backend/src/main/java/com/hometusk/shopping/api/ShoppingController.java`

**Add import:**
```java
import com.hometusk.shopping.service.ShoppingExportService;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
```

**Add field and constructor param:**
```java
private final ShoppingExportService exportService;

// Update constructor to include ShoppingExportService
```

**Add endpoint method after deleteItem():**
```java
@GetMapping("/shopping-lists/{listId}/export")
@Operation(
        summary = "Export shopping list",
        description = "Exports a shopping list in text or CSV format.")
@ApiResponses({
    @ApiResponse(responseCode = "200", description = "Exported list content"),
    @ApiResponse(responseCode = "400", description = "Invalid format parameter"),
    @ApiResponse(responseCode = "401", description = "Authentication required"),
    @ApiResponse(responseCode = "403", description = "Not a member of this household"),
    @ApiResponse(responseCode = "404", description = "Shopping list not found")
})
public ResponseEntity<String> exportShoppingList(
        @PathVariable UUID householdId,
        @PathVariable UUID listId,
        @RequestParam(defaultValue = "text") String format,
        @RequestParam(required = false) Boolean purchased) {
    log.debug("Exporting list: {}, household: {}, format: {}, purchased: {}",
              listId, householdId, format, purchased);

    // Verify membership (IDOR prevention)
    CurrentUser currentUser = userResolver.resolveCurrentUser();
    membershipService.requireMembership(currentUser.id(), householdId);

    // Validate format
    if (!format.equals("text") && !format.equals("csv")) {
        return ResponseEntity.badRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"errorCode\":\"INVALID_FORMAT\",\"message\":\"Format must be 'text' or 'csv'\"}");
    }

    // Get items (filtered if specified)
    List<ShoppingItem> items;
    if (Boolean.FALSE.equals(purchased)) {
        items = shoppingService.getUnpurchasedItemsInList(listId, householdId);
    } else if (Boolean.TRUE.equals(purchased)) {
        items = shoppingService.getItemsInList(listId, householdId).stream()
                .filter(ShoppingItem::isPurchased)
                .toList();
    } else {
        items = shoppingService.getItemsInList(listId, householdId);
    }

    // Export
    String content;
    HttpHeaders headers = new HttpHeaders();

    if (format.equals("csv")) {
        content = exportService.exportAsCsv(items);
        headers.setContentType(new MediaType("text", "csv", java.nio.charset.StandardCharsets.UTF_8));
        String filename = "shopping-list-" + LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE) + ".csv";
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"");
    } else {
        content = exportService.exportAsText(items);
        headers.setContentType(new MediaType("text", "plain", java.nio.charset.StandardCharsets.UTF_8));
    }

    return ResponseEntity.ok().headers(headers).body(content);
}
```

### Step 3: Create Unit Tests

**File:** `services/backend/src/test/java/com/hometusk/shopping/service/ShoppingExportServiceTest.java`

```java
package com.hometusk.shopping.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.hometusk.households.domain.Household;
import com.hometusk.shopping.domain.ShoppingItem;
import com.hometusk.shopping.domain.ShoppingList;
import com.hometusk.users.domain.User;
import java.lang.reflect.Field;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ShoppingExportServiceTest {

    private ShoppingExportService exportService;
    private ShoppingList list;
    private User user;

    @BeforeEach
    void setUp() throws Exception {
        exportService = new ShoppingExportService();
        Household household = new Household("Test Household");
        setId(household, UUID.randomUUID());
        user = new User("ext-test", "test@example.com", "Test");
        setId(user, UUID.randomUUID());
        list = new ShoppingList(household, "Groceries");
        setId(list, UUID.randomUUID());
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

        assertThat(result).startsWith("name,quantity,unit,purchased\n");
        assertThat(result).contains("Milk,2,liters,false");
        assertThat(result).contains("Bread,1,,false");
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
        assertThat(result).isEqualTo("name,quantity,unit,purchased\n");
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

    private static void setId(Object entity, UUID id) throws Exception {
        Field field = entity.getClass().getDeclaredField("id");
        field.setAccessible(true);
        field.set(entity, id);
    }
}
```

### Step 4: Create Integration Tests

**File:** `services/backend/src/test/java/com/hometusk/integration/shopping/ShoppingExportIntegrationTest.java`

```java
package com.hometusk.integration.shopping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.hometusk.households.domain.Household;
import com.hometusk.integration.IntegrationTestBase;
import com.hometusk.shopping.domain.ShoppingItem;
import com.hometusk.shopping.domain.ShoppingList;
import com.hometusk.shopping.repository.ShoppingListRepository;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MvcResult;

class ShoppingExportIntegrationTest extends IntegrationTestBase {

    @Autowired
    private ShoppingListRepository shoppingListRepository;

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

        MvcResult result = mockMvc.perform(get("/api/v1/households/{householdId}/shopping-lists/{listId}/export",
                        testHousehold.getId(), shoppingList.getId())
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
        addItem("Milk", 2, "liters");

        mockMvc.perform(get("/api/v1/households/{householdId}/shopping-lists/{listId}/export",
                        testHousehold.getId(), shoppingList.getId())
                        .param("format", "csv")
                        .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/csv"))
                .andExpect(header().exists("Content-Disposition"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("name,quantity,unit,purchased")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Milk,2,liters,false")));
    }

    @Test
    void export_emptyList_returns200() throws Exception {
        mockMvc.perform(get("/api/v1/households/{householdId}/shopping-lists/{listId}/export",
                        testHousehold.getId(), shoppingList.getId())
                        .param("format", "text")
                        .with(jwt()))
                .andExpect(status().isOk());
    }

    @Test
    void export_wrongHousehold_returns404or403() throws Exception {
        Household otherHousehold = householdRepository.save(new Household("Other"));

        mockMvc.perform(get("/api/v1/households/{householdId}/shopping-lists/{listId}/export",
                        otherHousehold.getId(), shoppingList.getId())
                        .with(jwt()))
                .andExpect(status().isForbidden());
    }

    @Test
    void export_invalidFormat_returns400() throws Exception {
        mockMvc.perform(get("/api/v1/households/{householdId}/shopping-lists/{listId}/export",
                        testHousehold.getId(), shoppingList.getId())
                        .param("format", "xml")
                        .with(jwt()))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("INVALID_FORMAT")));
    }

    @Test
    void export_filteredByPurchasedFalse_returnsUnpurchasedOnly() throws Exception {
        ShoppingItem unpurchased = addItem("Unpurchased", 1, null);
        ShoppingItem purchased = addItem("Purchased", 1, null);
        purchased.markPurchased();
        shoppingListRepository.saveAndFlush(shoppingList);

        MvcResult result = mockMvc.perform(get("/api/v1/households/{householdId}/shopping-lists/{listId}/export",
                        testHousehold.getId(), shoppingList.getId())
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

        mockMvc.perform(get("/api/v1/households/{householdId}/shopping-lists/{listId}/export",
                        testHousehold.getId(), shoppingList.getId())
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
```

---

## Verification Commands

```bash
cd /home/vad/Документы/hometusk/services/backend

# Format
GRADLE_USER_HOME=/tmp/gradle ./gradlew spotlessApply

# Unit tests
GRADLE_USER_HOME=/tmp/gradle ./gradlew test --tests "*ShoppingExportServiceTest*"

# Integration tests
GRADLE_USER_HOME=/tmp/gradle ./gradlew test --tests "*ShoppingExportIntegrationTest*"

# Full build
GRADLE_USER_HOME=/tmp/gradle ./gradlew build
```

---

## Acceptance Criteria Checklist

- [ ] AC-1: Text export with quantities — `export_textFormat_returnsPlainText`
- [ ] AC-2: CSV export with escaping — `export_csvFormat_returnsCsvWithHeaders`, `exportAsCsv_withSpecialChars_escapesCorrectly`
- [ ] AC-3: Empty list returns 200 — `export_emptyList_returns200`
- [ ] AC-4: Household boundary — `export_wrongHousehold_returns404or403`
- [ ] AC-5: Invalid format returns 400 — `export_invalidFormat_returns400`

---

## STOP-THE-LINE Rules

If any of these occur, STOP and report:
- Test failures not related to your changes
- Missing dependencies or imports that can't be resolved
- Architectural concerns (e.g., method not found, incompatible types)

Do NOT attempt to fix unrelated issues — report and stop.
