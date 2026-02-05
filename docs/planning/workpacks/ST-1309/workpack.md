# Workpack: ST-1309 — Task-Shopping Navigation Surfaces

## Sources of Truth
- Product Goal: `docs/planning/strategy/product-goal.md`
- Scope Anchor: `docs/planning/releases/MVP.md`
- Epic: `docs/planning/epics/EP-013/epic.md`
- Story: `docs/planning/epics/EP-013/stories/ST-1309-task-shopping-navigation.md`
- DoR: `docs/_governance/dor.md`
- DoD: `docs/_governance/dod.md`

---

## Status
**Ready**

---

## Goal
Add navigation links between tasks and their linked shopping items in the web UI. Users can see related shopping items from task detail and navigate to the linked task from shopping items.

---

## Scope

### In Scope
- Update `Task` type to include `linkedShoppingItems` field (backend already returns it)
- TaskDetail page: "Shopping Items" section showing linked items
- ShoppingDetail page: "For task: X" link on items with linkedTaskId
- Empty states when no links exist
- Navigation between pages

### Out of Scope
- Creating links from UI (via AI/commands only)
- Bulk link management
- Link history/audit

---

## Files to Create/Modify

| Path | Action | Purpose |
|------|--------|---------|
| `clients/web/src/types/api.ts` | MODIFY | Add `linkedShoppingItems` to Task type |
| `clients/web/src/routes/TaskDetail.tsx` | MODIFY | Add Shopping Items section |
| `clients/web/src/routes/TaskDetail.css` | MODIFY | Add styles for shopping section |
| `clients/web/src/routes/ShoppingDetail.tsx` | MODIFY | Add task link on items |
| `clients/web/src/routes/ShoppingDetail.css` | MODIFY | Add styles for task link |

---

## Implementation Plan

### Step 1: Update Task Type

**File:** `clients/web/src/types/api.ts`

Add `linkedShoppingItems` field to `Task` interface:

```typescript
export interface Task {
  // ... existing fields ...
  linkedShoppingItems?: LinkedShoppingItem[];
}

export interface LinkedShoppingItem {
  id: string;
  name: string;
  quantity?: number;
  unit?: string;
  purchased: boolean;
  listId: string;
  listName?: string;
}
```

Note: Backend `TaskDetailDto` returns full `ShoppingItemDto` objects. We can reuse `ShoppingItem` type or create a simplified one.

### Step 2: Update TaskDetail — Add Shopping Items Section

**File:** `clients/web/src/routes/TaskDetail.tsx`

After the metadata section (around line 320), add:

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

### Step 3: Update TaskDetail CSS

**File:** `clients/web/src/routes/TaskDetail.css`

Add styles for the shopping items section:

```css
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
  transition: background-color 0.15s;
}

.task-detail__shopping-item:hover {
  background-color: var(--color-bg-hover);
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

### Step 4: Update ShoppingDetail — Add Task Link

**File:** `clients/web/src/routes/ShoppingDetail.tsx`

In the `renderItem` function, after item name/meta, add task link:

```tsx
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
```

### Step 5: Update ShoppingDetail CSS

**File:** `clients/web/src/routes/ShoppingDetail.css`

Add styles for task link:

```css
.shopping-detail__task-link {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: var(--color-primary);
  text-decoration: none;
  padding: 4px 8px;
  border-radius: 4px;
  transition: background-color 0.15s;
}

.shopping-detail__task-link:hover {
  background-color: var(--color-primary-bg);
}
```

---

## Verification Commands

```bash
cd /home/vad/Документы/hometusk/clients/web

# Install deps (if needed)
npm install

# Type check
npm run typecheck

# Lint
npm run lint

# Build
npm run build

# Dev server (manual testing)
npm run dev
```

---

## Acceptance Criteria Mapping

| AC | Criteria | Verification |
|----|----------|--------------|
| AC-1 | Task Detail shows shopping items | Manual test |
| AC-2 | Task without items: section hidden | Manual test |
| AC-3 | Shopping item shows task link | Manual test |
| AC-4 | Shopping item without task: no link | Manual test |
| AC-5 | Navigation works both ways | Manual test |

---

## Risks

| Risk | Impact | Mitigation |
|------|--------|------------|
| Backend doesn't return listId in ShoppingItemDto | Medium | Check backend DTO, may need to add field |
| CSS variable names differ | Low | Check existing CSS for variable names |

---

## Rollback

- Revert frontend code changes
- No backend changes needed

---

## References

- Backend: `TaskDetailDto.java` (already has linkedShoppingItems)
- Backend: `ShoppingItemDto.java` (has linkedTaskId)
- Patterns: TaskDetail.tsx, ShoppingDetail.tsx (existing components)
