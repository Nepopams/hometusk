# ST-203 DoD Checklist

## Story Reference
- Story: `docs/planning/epics/EP-003/stories/ST-203-tasks-list.md`
- Workpack: `docs/planning/workpacks/ST-203/workpack.md`

---

## Acceptance Criteria

- [ ] **AC1:** Tasks list loads
  - [ ] Navigate to /households/:householdId/tasks
  - [ ] GET /households/{id}/tasks called
  - [ ] Tasks displayed in list/table

- [ ] **AC2:** Task item display
  - [ ] Title shown
  - [ ] Status badge (open/in_progress/done/cancelled)
  - [ ] Assignee (name or "Unassigned")
  - [ ] Zone (name or "No zone")
  - [ ] Deadline (formatted or "No deadline")

- [ ] **AC3:** Status filter
  - [ ] Dropdown with: All, Open, In Progress, Done, Cancelled
  - [ ] Select "Open" → ?status=open in API call
  - [ ] Only matching tasks displayed

- [ ] **AC4:** Assignee filter
  - [ ] Dropdown with: All + household members
  - [ ] Members loaded from GET /households/{id}/members
  - [ ] Select member → ?assigneeId={id} in API call
  - [ ] Only matching tasks displayed

- [ ] **AC5:** Zone filter
  - [ ] Dropdown with: All + household zones
  - [ ] Zones loaded from GET /households/{id}/zones
  - [ ] Select zone → ?zoneId={id} in API call
  - [ ] Only matching tasks displayed

- [ ] **AC6:** Combined filters
  - [ ] Multiple filters can be active
  - [ ] API call includes all active filters
  - [ ] Results match all criteria

- [ ] **AC7:** Empty state
  - [ ] No tasks match → "No tasks found" shown
  - [ ] Suggestion to adjust filters

- [ ] **AC8:** Loading state
  - [ ] Spinner/skeleton while loading
  - [ ] Disappears when data loads

- [ ] **AC9:** Error handling
  - [ ] API error → error message shown
  - [ ] Retry button works
  - [ ] 403 → "Access denied" message

- [ ] **AC10:** Task row click
  - [ ] Click row → navigate to /households/:householdId/tasks/:taskId
  - [ ] (Detail view can be placeholder)

---

## Types (from OpenAPI)

- [ ] Task type matches schema
- [ ] Zone type matches schema
- [ ] HouseholdMember type matches schema
- [ ] UserSummary type matches schema
- [ ] Task.status enum matches: open, in_progress, done, cancelled

---

## Code Quality (DoD)

- [ ] TypeScript strict (no `any`)
- [ ] ESLint passes
- [ ] Prettier applied
- [ ] Components properly typed
- [ ] Hooks follow React conventions
- [ ] URL params synced with filters

---

## Verification Commands

```bash
cd clients/web
npm run lint    # → passes
npm run build   # → passes
npm run dev     # → manual test

# Manual testing:
# 1. Login + select household
# 2. Navigate to /households/:householdId/tasks
# 3. Tasks load and display
# 4. Test each filter individually
# 5. Test combined filters
# 6. Test empty state (filter that matches nothing)
# 7. Test error (disconnect backend)
# 8. Click task row → navigates to detail
```

---

## Sign-off

| Role | Name | Date | Status |
|------|------|------|--------|
| Developer | | | |
| Reviewer | | | |
