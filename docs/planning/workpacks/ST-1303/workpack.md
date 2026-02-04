# Workpack: ST-1303 — Export Shopping List (Text/CSV)

## Sources of Truth
- Product Goal: `docs/planning/strategy/product-goal.md`
- Scope Anchor: `docs/planning/releases/MVP.md`
- Epic: `docs/planning/epics/EP-013/epic.md`
- Story: `docs/planning/epics/EP-013/stories/ST-1303-export-shopping-list.md`
- Contract: `docs/contracts/http/shopping-marketplaces.openapi.yaml` (lines 255-319)
- DoR: `docs/_governance/dor.md`
- DoD: `docs/_governance/dod.md`

---

## Status
**Ready** — Contract approved 2026-02-03

---

## Goal
Add export functionality for shopping lists allowing users to download/copy their list in plain text or CSV format via `GET /api/v1/households/{householdId}/shopping-lists/{listId}/export`.

---

## Scope

### In Scope
- Export endpoint with `format` query param (text, csv)
- Optional `purchased` query param to filter items
- Text format: Human-readable newline-separated list
- CSV format: RFC 4180 compliant with proper escaping
- Content-Type headers (text/plain, text/csv)
- Content-Disposition header for CSV downloads
- Household boundary enforcement
- Unit tests for formatting logic
- Integration tests for endpoint

### Out of Scope
- PDF export
- Rich formatting (markdown)
- Export history/versioning
- Bulk export (all lists at once)

---

## Files to Create/Modify

| Path | Action | Purpose |
|------|--------|---------|
| `services/backend/src/main/java/com/hometusk/shopping/service/ShoppingExportService.java` | CREATE | Export formatting logic |
| `services/backend/src/main/java/com/hometusk/shopping/api/ShoppingController.java` | MODIFY | Add export endpoint |
| `services/backend/src/test/java/com/hometusk/shopping/service/ShoppingExportServiceTest.java` | CREATE | Unit tests |
| `services/backend/src/test/java/com/hometusk/integration/shopping/ShoppingExportIntegrationTest.java` | CREATE | Integration tests |

---

## Implementation Plan

### Step 1: Create ShoppingExportService

**File:** `services/backend/src/main/java/com/hometusk/shopping/service/ShoppingExportService.java`

**Key elements:**
- `exportAsText(List<ShoppingItem> items)` - returns text format
- `exportAsCsv(List<ShoppingItem> items)` - returns CSV format
- Text format: `{name}` or `{name} - {quantity} {unit}`
- CSV format: headers (name,quantity,unit,purchased), RFC 4180 escaping
- Handle empty list gracefully

**Text format example:**
```
Milk - 2 liters
Bread
Eggs - 12 pcs
```

**CSV format example:**
```csv
name,quantity,unit,purchased
Milk,2,liters,false
"Cheese, cheddar",1,,false
Bread,1,,true
```

### Step 2: Add Export Endpoint to ShoppingController

**File:** `services/backend/src/main/java/com/hometusk/shopping/api/ShoppingController.java`

**Add method:**
```java
@GetMapping("/shopping-lists/{listId}/export")
public ResponseEntity<String> exportShoppingList(
    @PathVariable UUID householdId,
    @PathVariable UUID listId,
    @RequestParam(defaultValue = "text") String format,
    @RequestParam(required = false) Boolean purchased)
```

**Logic:**
1. Verify membership (IDOR prevention)
2. Get items (filtered by purchased if specified)
3. Validate format (text/csv) - return 400 for invalid
4. Format using ShoppingExportService
5. Return with appropriate Content-Type and headers

**Response headers:**
- Text: `Content-Type: text/plain; charset=utf-8`
- CSV: `Content-Type: text/csv`, `Content-Disposition: attachment; filename="shopping-list-{listName}-{date}.csv"`

### Step 3: Write Unit Tests

**File:** `services/backend/src/test/java/com/hometusk/shopping/service/ShoppingExportServiceTest.java`

**Test cases:**
- `exportAsText_withItems_returnsFormattedText`
- `exportAsText_withQuantityAndUnit_includesQuantityUnit`
- `exportAsText_emptyList_returnsEmptyOrMessage`
- `exportAsCsv_withItems_returnsValidCsv`
- `exportAsCsv_withSpecialChars_escapesCorrectly` (commas, quotes, newlines)
- `exportAsCsv_emptyList_returnsHeadersOnly`
- `exportAsCsv_withUnicode_handlesCorrectly`

### Step 4: Write Integration Tests

**File:** `services/backend/src/test/java/com/hometusk/integration/shopping/ShoppingExportIntegrationTest.java`

**Test cases:**
- `export_textFormat_returnsPlainText` (AC-1)
- `export_csvFormat_returnsCsvWithHeaders` (AC-2)
- `export_emptyList_returns200` (AC-3)
- `export_wrongHousehold_returns403or404` (AC-4)
- `export_invalidFormat_returns400` (AC-5)
- `export_filteredByPurchased_returnsFilteredItems`
- `export_contentTypeHeaders_areCorrect`

---

## Verification Commands

```bash
cd /home/vad/Документы/hometusk/services/backend

# Build
./gradlew build

# Spotless
./gradlew spotlessCheck
./gradlew spotlessApply

# Unit tests
./gradlew test --tests "*ShoppingExportServiceTest*"

# Integration tests
./gradlew test --tests "*ShoppingExportIntegrationTest*"

# All tests
./gradlew test
```

---

## Acceptance Criteria Mapping

| AC | Criteria | Verification |
|----|----------|--------------|
| AC-1 | Text export with quantities | Integration test |
| AC-2 | CSV export with escaping | Integration test |
| AC-3 | Empty list returns 200 | Integration test |
| AC-4 | Household boundary enforced | Integration test |
| AC-5 | Invalid format returns 400 | Integration test |

---

## Risks

| Risk | Impact | Mitigation |
|------|--------|------------|
| CSV escaping edge cases | LOW | Use RFC 4180 rules, cover in unit tests |
| Unicode handling | LOW | Use UTF-8 encoding consistently |
| Large lists performance | LOW | Streaming not needed for MVP (lists < 1000 items) |

---

## Rollback

- Revert code changes (no migration required)
- Feature flag not needed (simple endpoint addition)

---

## References

- Contract: Shopping Marketplaces OpenAPI (export endpoint)
- RFC 4180: CSV format specification
- Patterns: ShoppingController.java (existing endpoints)
