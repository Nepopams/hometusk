# ST-203 REVIEW Prompt

**Mode:** CODE REVIEW — Verify implementation against spec

---

## Context

Review the ST-203 implementation: Tasks List & Filters.

**Read these files (mandatory):**
- `docs/planning/workpacks/ST-203/workpack.md` — implementation plan
- `docs/planning/workpacks/ST-203/checklist.md` — DoD checklist
- `docs/planning/epics/EP-003/stories/ST-203-tasks-list.md` — story spec
- `docs/contracts/http/commands.openapi.yaml` — API contract

**Review these files:**
- `clients/web/src/types/api.ts`
- `clients/web/src/lib/api.ts`
- `clients/web/src/hooks/useTasks.ts`
- `clients/web/src/hooks/useZones.ts`
- `clients/web/src/hooks/useMembers.ts`
- `clients/web/src/routes/TasksList.tsx`
- `clients/web/src/components/tasks/*.tsx`
- `clients/web/src/components/ui/*.tsx`

---

## Review Checklist

### 1. Types
- [ ] Task type matches OpenAPI schema exactly
- [ ] TaskStatus enum: open, in_progress, done, cancelled
- [ ] Zone type matches schema
- [ ] HouseholdMember type matches schema
- [ ] UserSummary type matches schema
- [ ] No `any` types

### 2. API Methods
- [ ] getTasks accepts householdId and optional filters
- [ ] getTasks builds query params correctly
- [ ] getZones fetches /households/{id}/zones
- [ ] getMembers fetches /households/{id}/members
- [ ] Error handling consistent

### 3. Custom Hooks
- [ ] useTasks returns tasks, isLoading, error, refetch
- [ ] useTasks refetches when filters change
- [ ] useZones caches/memoizes appropriately
- [ ] useMembers caches/memoizes appropriately
- [ ] No memory leaks (cleanup on unmount)

### 4. UI Components
- [ ] TasksTable renders task rows
- [ ] TaskRow displays all required fields
- [ ] TaskStatusBadge shows correct colors/labels
- [ ] TaskFilters has 3 dropdowns
- [ ] EmptyTasks shows when no results
- [ ] Spinner shows during loading
- [ ] ErrorMessage shows on error

### 5. Filter Logic
- [ ] Status filter: All + 4 statuses
- [ ] Assignee filter: All + members from API
- [ ] Zone filter: All + zones from API
- [ ] Filters sync to URL params
- [ ] Combined filters work

### 6. States Handling
- [ ] Loading: spinner shown
- [ ] Empty: message shown
- [ ] Error: message + retry button
- [ ] 403: access denied message
- [ ] Success: tasks displayed

### 7. Navigation
- [ ] Row click navigates to /households/:householdId/tasks/:taskId
- [ ] Filter changes update URL
- [ ] URL params initialize filters on mount

---

## Contract Compliance

Verify types match OpenAPI exactly:
- [ ] Task.status is 'open' | 'in_progress' | 'done' | 'cancelled'
- [ ] Task.assignee is optional UserSummary
- [ ] Task.zone is optional Zone
- [ ] HouseholdMember has userId (not id)
- [ ] All date fields are string (ISO format)

---

## Verification Commands

```bash
cd clients/web
npm run lint    # Should pass
npm run build   # Should pass
npm run dev     # Manual testing

# Manual test flow:
# 1. Login → select household → /households/:householdId/tasks
# 2. Tasks load from API
# 3. Test status filter (each option)
# 4. Test assignee filter (each member)
# 5. Test zone filter (each zone)
# 6. Test combined filters
# 7. Test empty state (filter with no results)
# 8. Test error state (stop backend)
# 9. Click task row → navigates to detail
# 10. Refresh page → filters preserved from URL
```

---

## Output Format

```markdown
# ST-203 Code Review Report

## Summary
[1-2 sentence summary]

## Verification Results
| Command | Result | Notes |
|---------|--------|-------|
| npm run lint | PASS/FAIL | |
| npm run build | PASS/FAIL | |
| Manual testing | PASS/FAIL | |

## Checklist Results
| Category | Status | Notes |
|----------|--------|-------|
| Types | PASS/FAIL | |
| API Methods | PASS/FAIL | |
| Custom Hooks | PASS/FAIL | |
| UI Components | PASS/FAIL | |
| Filter Logic | PASS/FAIL | |
| States Handling | PASS/FAIL | |
| Navigation | PASS/FAIL | |
| Contract Compliance | PASS/FAIL | |

## Must-Fix Issues
[Critical issues]

## Should-Fix Issues
[Non-critical improvements]

## Verdict
**GO / NO-GO**

[Justification]
```

---

## GO/NO-GO Criteria

**GO if:**
- All verification commands pass
- Filters work correctly
- Types match contract
- All states handled
- Navigation works

**NO-GO if:**
- Build/lint fails
- Filters broken
- Types don't match
- Missing states
- Contract violations
