# Codex PLAN Prompt — ST-1309: Task-Shopping Navigation Surfaces

## Directive
**READ-ONLY exploration.** Do NOT edit or create any files. Your output will inform the APPLY phase.

---

## Context

**Story:** ST-1309 — Task-Shopping Navigation Surfaces
**Points:** 3
**Epic:** EP-013 (Shopping Marketplaces)

**Goal:** Add navigation links between tasks and shopping items in the web UI.

---

## Sources of Truth (READ these files)

```
docs/planning/workpacks/ST-1309/workpack.md              # Implementation plan
docs/planning/epics/EP-013/stories/ST-1309-task-shopping-navigation.md  # Story spec
```

---

## Exploration Tasks

### 1. Examine backend ShoppingItemDto
```bash
cat services/backend/src/main/java/com/hometusk/shopping/dto/ShoppingItemDto.java
```
**Find:**
- Does it include `linkedTaskId`?
- Does it include `listId` or reference to parent list?
- Full field list for mapping to frontend type

### 2. Examine backend TaskDetailDto
```bash
cat services/backend/src/main/java/com/hometusk/tasks/dto/TaskDetailDto.java
```
**Confirm:**
- `linkedShoppingItems` field exists
- What type is returned (List<ShoppingItemDto>?)

### 3. Examine frontend Task type
```bash
cat clients/web/src/types/api.ts | head -100
```
**Find:**
- Current `Task` interface definition
- `ShoppingItem` interface definition
- What fields are missing

### 4. Examine TaskDetail component structure
```bash
cat clients/web/src/routes/TaskDetail.tsx
```
**Find:**
- Where to add Shopping Items section (after metadata?)
- Existing card/section patterns
- How `householdId` is accessed
- Import patterns (Link from react-router-dom)

### 5. Examine TaskDetail CSS
```bash
cat clients/web/src/routes/TaskDetail.css
```
**Find:**
- Existing CSS variable names (--color-*, etc.)
- Card/row/divider class patterns
- Section header patterns (if any)

### 6. Examine ShoppingDetail component
```bash
cat clients/web/src/routes/ShoppingDetail.tsx
```
**Find:**
- Where to add task link in `renderItem` function
- How item info is rendered
- Navigation patterns

### 7. Examine ShoppingDetail CSS
```bash
cat clients/web/src/routes/ShoppingDetail.css
```
**Find:**
- Item styling patterns
- Link/button patterns
- CSS variable names

### 8. Check if listId is available in ShoppingItem
```bash
rg "listId" clients/web/src/types/api.ts
rg "shoppingListId" services/backend/src/main/java/com/hometusk/shopping/dto/ShoppingItemDto.java
```
**Determine:**
- How to link from TaskDetail shopping item to the correct shopping list

---

## Expected Findings to Report

After exploration, provide:

1. **Backend DTO fields:**
   - ShoppingItemDto full field list
   - Does it have listId/shoppingListId?
   - TaskDetailDto linkedShoppingItems type

2. **Frontend type gaps:**
   - Missing fields in Task interface
   - ShoppingItem fields available

3. **Component patterns:**
   - TaskDetail: where to insert shopping section
   - ShoppingDetail: where to insert task link
   - CSS variable names to use

4. **Navigation requirements:**
   - Route pattern for shopping detail: `/households/{id}/shopping/{listId}`
   - Route pattern for task detail: `/tasks/{taskId}` or `/households/{id}/tasks/{taskId}`?

5. **Potential blockers:**
   - If listId not available in ShoppingItemDto → need backend change
   - CSS variable naming inconsistencies

---

## Allowed Commands (whitelist)

- `ls`, `find` (directory exploration)
- `cat`, `head`, `tail` (file reading)
- `rg`, `grep` (search)
- `sed -n` (extract lines)

## Forbidden

- File modifications (edit/write/move/delete)
- `npm` commands
- Any network access

---

## Output Format

```markdown
## PLAN Findings for ST-1309

### 1. Backend DTO Fields
- ShoppingItemDto: [field list]
- Has listId: yes/no
- TaskDetailDto.linkedShoppingItems type: [type]

### 2. Frontend Type Gaps
- Task interface missing: [fields]
- ShoppingItem has: [fields]

### 3. Component Patterns
- TaskDetail insert point: after line [N] / after [section]
- ShoppingDetail insert point: in renderItem, after [element]
- CSS variables: [list]

### 4. Navigation Routes
- Shopping detail: [route pattern]
- Task detail: [route pattern]

### 5. Blockers / Concerns
- [list or "None"]

### 6. Confirmed File Paths
- Types: clients/web/src/types/api.ts
- TaskDetail: clients/web/src/routes/TaskDetail.tsx
- TaskDetail CSS: clients/web/src/routes/TaskDetail.css
- ShoppingDetail: clients/web/src/routes/ShoppingDetail.tsx
- ShoppingDetail CSS: clients/web/src/routes/ShoppingDetail.css
```

---

## STOP Condition

If you discover blocking issues (e.g., missing listId in backend DTO), report them and STOP.
Do NOT attempt to fix issues — that's for APPLY phase.
