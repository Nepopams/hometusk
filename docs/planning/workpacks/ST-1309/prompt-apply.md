# Codex APPLY Prompt — ST-1309: Task-Shopping Navigation Surfaces

## Directive
**IMPLEMENTATION phase.** Create/modify files as specified. Run verification commands. Stop on failure.

---

## Context

**Story:** ST-1309 — Task-Shopping Navigation Surfaces
**Points:** 3
**Epic:** EP-013 (Shopping Marketplaces)

**Goal:** Add navigation links between tasks and shopping items in web UI.

---

## Sources of Truth

```
docs/planning/workpacks/ST-1309/workpack.md
docs/planning/epics/EP-013/stories/ST-1309-task-shopping-navigation.md
```

---

## PLAN Findings Summary

1. **Blocker resolved:** ShoppingItemDto needs `listId` field (backend fix required)
2. **Task type:** Missing `linkedShoppingItems` in frontend types
3. **CSS variables:** `--color-text-secondary`, `--color-brand`, `--color-bg-hover-subtle`, `--color-success`
4. **Routes:** Task detail = `/tasks/:taskId`, Shopping detail = `/households/:householdId/shopping/:listId`
5. **Insert points:** TaskDetail after metadata, ShoppingDetail in item-info div

---

## Implementation Steps

### PART 1: Backend Fix (Add listId to ShoppingItemDto)

#### Step 1.1: Update ShoppingItemDto

**File:** `services/backend/src/main/java/com/hometusk/shopping/dto/ShoppingItemDto.java`

Add `listId` field to the record:

```java
package com.hometusk.shopping.dto;

import com.hometusk.shopping.domain.ShoppingItem;
import com.hometusk.tasks.dto.UserSummaryDto;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "Shopping item")
public record ShoppingItemDto(
        @Schema(description = "Item ID") UUID id,
        @Schema(description = "Shopping list ID") UUID listId,
        @Schema(description = "Item name") String name,
        @Schema(description = "Quantity") Integer quantity,
        @Schema(description = "Unit of measurement") String unit,
        @Schema(description = "Whether item has been purchased") boolean purchased,
        @Schema(description = "Linked task ID (if any)") UUID linkedTaskId,
        @Schema(description = "User who added the item") UserSummaryDto addedBy,
        @Schema(description = "Creation timestamp") Instant createdAt,
        @Schema(description = "Purchase timestamp") Instant purchasedAt) {

    public static ShoppingItemDto from(ShoppingItem item) {
        return new ShoppingItemDto(
                item.getId(),
                item.getShoppingList().getId(),
                item.getName(),
                item.getQuantity(),
                item.getUnit(),
                item.isPurchased(),
                item.getLinkedTaskId(),
                UserSummaryDto.from(item.getAddedBy()),
                item.getCreatedAt(),
                item.getPurchasedAt());
    }
}
```

#### Step 1.2: Run Backend Tests

```bash
cd /home/vad/Документы/hometusk/services/backend
./gradlew spotlessApply
./gradlew test
```

---

### PART 2: Frontend Changes

#### Step 2.1: Update Types

**File:** `clients/web/src/types/api.ts`

Add `listId` to ShoppingItem and `linkedShoppingItems` to Task:

```typescript
// Update ShoppingItem interface (around line 258):
export interface ShoppingItem {
  id: string;
  listId: string;  // ADD THIS
  name: string;
  quantity?: number;
  unit?: string;
  purchased: boolean;
  linkedTaskId?: string;
  addedBy?: UserSummary;
  createdAt: string;
  purchasedAt?: string;
}

// Update Task interface (around line 74):
export interface Task {
  id: string;
  householdId: string;
  title: string;
  description?: string;
  status: TaskStatus;
  assignee?: UserSummary;
  zone?: Zone;
  deadline?: string;
  createdBy: UserSummary;
  commandId?: string;
  createdVia: 'command' | 'fallback' | 'direct';
  createdAt: string;
  updatedAt: string;
  completedAt?: string;
  linkedShoppingItems?: ShoppingItem[];  // ADD THIS
}
```

#### Step 2.2: Update TaskDetail.tsx

**File:** `clients/web/src/routes/TaskDetail.tsx`

Add Shopping Items section after the details card (before closing `</div>` of wrapper, around line 320):

Insert after the `{/* Details Card */}` section closes (after line ~320, before the final closing divs):

```tsx
        {/* Shopping Items Section */}
        {task.linkedShoppingItems && task.linkedShoppingItems.length > 0 && (
          <>
            <div className="task-detail__section-header">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <circle cx="9" cy="21" r="1" />
                <circle cx="20" cy="21" r="1" />
                <path d="M1 1h4l2.68 13.39a2 2 0 0 0 2 1.61h9.72a2 2 0 0 0 2-1.61L23 6H6" />
              </svg>
              <span>Shopping Items ({task.linkedShoppingItems.length})</span>
            </div>
            <div className="task-detail__card">
              {task.linkedShoppingItems.map((item, idx) => (
                <div key={item.id}>
                  {idx > 0 && <div className="task-detail__divider" />}
                  <Link
                    to={`/households/${householdId}/shopping/${item.listId}`}
                    className="task-detail__shopping-item"
                  >
                    <span className={`task-detail__shopping-name ${item.purchased ? 'task-detail__shopping-name--purchased' : ''}`}>
                      {item.name}
                    </span>
                    {(item.quantity || item.unit) && (
                      <span className="task-detail__shopping-meta">
                        {item.quantity}{item.unit ? ` ${item.unit}` : ''}
                      </span>
                    )}
                    {item.purchased && (
                      <svg className="task-detail__shopping-check" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                        <polyline points="20 6 9 17 4 12" />
                      </svg>
                    )}
                  </Link>
                </div>
              ))}
            </div>
          </>
        )}
```

**Note:** Import `ShoppingItem` type if needed (should already be available via Task type).

#### Step 2.3: Update TaskDetail.css

**File:** `clients/web/src/routes/TaskDetail.css`

Add at the end of the file:

```css
/* Shopping Items Section */
.task-detail__section-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 24px;
  margin-bottom: 12px;
  color: var(--color-text-secondary);
  font-size: 14px;
  font-weight: 500;
}

.task-detail__shopping-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  text-decoration: none;
  color: inherit;
  transition: background-color var(--transition-fast);
}

.task-detail__shopping-item:hover {
  background-color: var(--color-bg-hover-subtle);
}

.task-detail__shopping-name {
  flex: 1;
  font-size: 14px;
}

.task-detail__shopping-name--purchased {
  text-decoration: line-through;
  opacity: 0.6;
}

.task-detail__shopping-meta {
  font-size: 12px;
  color: var(--color-text-secondary);
}

.task-detail__shopping-check {
  color: var(--color-success);
}
```

#### Step 2.4: Update ShoppingDetail.tsx

**File:** `clients/web/src/routes/ShoppingDetail.tsx`

In the `renderItem` function, add task link inside the `.shopping-detail__item-info` div, after the meta span (around line 283):

Find:
```tsx
        <div className="shopping-detail__item-info">
          <span className={`shopping-detail__item-name ${item.purchased ? 'shopping-detail__item-name--purchased' : ''}`}>
            {item.name}
          </span>
          {(item.quantity || item.unit) && (
            <span className="shopping-detail__item-meta">
              {item.quantity && item.quantity > 1 ? `${item.quantity}` : ''}
              {item.quantity && item.unit ? ' ' : ''}
              {item.unit || ''}
            </span>
          )}
        </div>
```

Replace with:
```tsx
        <div className="shopping-detail__item-info">
          <span className={`shopping-detail__item-name ${item.purchased ? 'shopping-detail__item-name--purchased' : ''}`}>
            {item.name}
          </span>
          {(item.quantity || item.unit) && (
            <span className="shopping-detail__item-meta">
              {item.quantity && item.quantity > 1 ? `${item.quantity}` : ''}
              {item.quantity && item.unit ? ' ' : ''}
              {item.unit || ''}
            </span>
          )}
          {item.linkedTaskId && (
            <Link
              to={`/tasks/${item.linkedTaskId}`}
              className="shopping-detail__task-link"
              onClick={(e) => e.stopPropagation()}
            >
              For task
              <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <path d="M5 12h14M12 5l7 7-7 7" />
              </svg>
            </Link>
          )}
        </div>
```

**Note:** Ensure `Link` is imported from 'react-router-dom' (should already be imported).

#### Step 2.5: Update ShoppingDetail.css

**File:** `clients/web/src/routes/ShoppingDetail.css`

Add at the end of the file:

```css
/* Task Link */
.shopping-detail__task-link {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: var(--color-brand);
  text-decoration: none;
  padding: 2px 6px;
  border-radius: var(--radius-sm);
  transition: background-color var(--transition-fast);
  margin-left: 8px;
}

.shopping-detail__task-link:hover {
  background-color: var(--color-bg-hover-subtle);
}
```

---

## Verification Commands

### Backend

```bash
cd /home/vad/Документы/hometusk/services/backend
./gradlew spotlessApply
./gradlew build
```

### Frontend

```bash
cd /home/vad/Документы/hometusk/clients/web
npm run typecheck
npm run lint
npm run build
```

---

## Files Checklist

| File | Action |
|------|--------|
| `services/backend/src/main/java/com/hometusk/shopping/dto/ShoppingItemDto.java` | MODIFY (add listId) |
| `clients/web/src/types/api.ts` | MODIFY (add listId, linkedShoppingItems) |
| `clients/web/src/routes/TaskDetail.tsx` | MODIFY (add shopping section) |
| `clients/web/src/routes/TaskDetail.css` | MODIFY (add shopping styles) |
| `clients/web/src/routes/ShoppingDetail.tsx` | MODIFY (add task link) |
| `clients/web/src/routes/ShoppingDetail.css` | MODIFY (add task link styles) |

---

## STOP Conditions

- Stop if backend tests fail
- Stop if frontend typecheck/lint/build fails
- Stop if you need to deviate from the plan
