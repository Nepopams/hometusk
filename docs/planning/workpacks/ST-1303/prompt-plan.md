# Codex PLAN Prompt — ST-1303: Export Shopping List

## Directive
**READ-ONLY exploration.** Do NOT edit or create any files. Your output will inform the APPLY phase.

---

## Context

**Story:** ST-1303 — Export Shopping List (Text/CSV)
**Points:** 3
**Epic:** EP-013 (Shopping Marketplaces)

**Goal:** Add `GET /api/v1/households/{householdId}/shopping-lists/{listId}/export` endpoint that returns shopping list items in text or CSV format.

---

## Sources of Truth (READ these files)

```
docs/planning/workpacks/ST-1303/workpack.md          # Implementation plan
docs/contracts/http/shopping-marketplaces.openapi.yaml  # Contract (lines 255-319)
docs/planning/epics/EP-013/stories/ST-1303-export-shopping-list.md  # Story spec
```

---

## Exploration Tasks

### 1. Examine existing ShoppingController
```bash
cat services/backend/src/main/java/com/hometusk/shopping/api/ShoppingController.java
```
**Find:**
- How endpoints verify membership (IDOR prevention pattern)
- How items are fetched via ShoppingService
- ResponseEntity patterns used

### 2. Examine ShoppingService
```bash
cat services/backend/src/main/java/com/hometusk/shopping/service/ShoppingService.java
```
**Find:**
- Method to get items by list ID and household ID
- Method to filter by purchased status
- Exception handling patterns

### 3. Examine ShoppingItem entity
```bash
cat services/backend/src/main/java/com/hometusk/shopping/domain/ShoppingItem.java
```
**Find:**
- Fields: name, quantity, unit, purchased
- Getter methods

### 4. Examine existing integration test patterns
```bash
ls -la services/backend/src/test/java/com/hometusk/integration/shopping/
cat services/backend/src/test/java/com/hometusk/integration/shopping/ShoppingRunRepositoryIntegrationTest.java
```
**Find:**
- Test class structure (extends IntegrationTestBase?)
- How to set up test data (shopping lists, items)
- MockMvc usage patterns

### 5. Check for existing export/formatting utilities
```bash
rg -l "csv\|CSV\|export" services/backend/src/main/java --type java
rg "RFC.?4180\|escaping" services/backend/src/main/java --type java
```
**Find:**
- Any existing CSV utilities to reuse
- Escaping patterns

### 6. Verify contract details
```bash
sed -n '255,320p' docs/contracts/http/shopping-marketplaces.openapi.yaml
```
**Confirm:**
- Query params: format (text/csv), purchased (boolean)
- Response content types
- Error responses

---

## Expected Findings to Report

After exploration, provide:

1. **ShoppingService methods available:**
   - Method name for getting items by list/household
   - Method name for filtering by purchased

2. **IDOR prevention pattern:**
   - Exact code pattern used in ShoppingController

3. **Test setup pattern:**
   - Base class to extend
   - How to create test ShoppingList and ShoppingItem

4. **Content-Type handling:**
   - How other endpoints set Content-Type
   - Any existing produces= patterns in @GetMapping

5. **CSV library decision:**
   - Is there an existing CSV library in dependencies?
   - Or should we use manual RFC 4180 escaping?

6. **File paths confirmed:**
   - Exact package for new ShoppingExportService
   - Test file locations

---

## Allowed Commands (whitelist)

- `ls`, `find` (directory exploration)
- `cat`, `head`, `tail` (file reading)
- `rg`, `grep` (search)
- `sed -n` (extract lines)
- `git status`, `git diff` (read-only inspection)

## Forbidden

- File modifications (edit/write/move/delete)
- `./gradlew` commands
- Any network access
- Package installations

---

## Output Format

```markdown
## PLAN Findings for ST-1303

### 1. ShoppingService Methods
- Get items: `methodName(params)`
- Filter purchased: `methodName(params)` or inline filtering

### 2. IDOR Prevention Pattern
```java
// exact code snippet
```

### 3. Test Setup Pattern
- Base class: `ClassName`
- Test data setup: description

### 4. Content-Type Handling
- Pattern used: description

### 5. CSV Library
- Decision: manual escaping / library name

### 6. Confirmed Paths
- Service: `exact/path/ShoppingExportService.java`
- Test: `exact/path/ShoppingExportServiceTest.java`
- Integration: `exact/path/ShoppingExportIntegrationTest.java`

### 7. Additional Notes
- Any surprises or concerns
```

---

## STOP Condition

If you discover blocking issues (e.g., missing methods, incompatible patterns), report them and STOP.
Do NOT attempt to fix issues — that's for APPLY phase.
