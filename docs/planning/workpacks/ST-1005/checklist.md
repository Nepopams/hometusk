# Checklist: ST-1005 — Routines Page (List + Create/Edit Form)

## Sources of Truth
- Workpack: `docs/planning/workpacks/ST-1005/workpack.md`
- Story: `docs/planning/epics/EP-010/stories/ST-1005-routines-page.md`
- DoD: `docs/_governance/dod.md`

---

## Acceptance Criteria

### AC-1: Routines page accessible
- [ ] Route `/households/{id}/routines` exists
- [ ] Page renders without errors
- [ ] Navigation shows "Routines" as active

### AC-2: List shows routines
- [ ] Title displayed for each routine
- [ ] Zone displayed (or "—" if none)
- [ ] Frequency text displayed (Daily, Mon/Wed/Fri, etc.)
- [ ] Status badge displayed (ACTIVE/PAUSED)
- [ ] Policy indicator displayed (round-robin icon, fixed user, manual)

### AC-3: Create routine form opens
- [ ] "Create Routine" button visible
- [ ] Click opens form/modal
- [ ] Form has empty fields
- [ ] Cancel closes form

### AC-4: Create routine - DAILY
- [ ] Select "Daily" frequency
- [ ] No additional config needed
- [ ] Save creates routine with `{ "type": "DAILY" }`

### AC-5: Create routine - WEEKLY
- [ ] Select "Weekly" frequency
- [ ] Day-of-week checkboxes appear
- [ ] Can select multiple days
- [ ] Save creates routine with `{ "type": "WEEKLY", "daysOfWeek": [...] }`

### AC-6: Create routine - MONTHLY
- [ ] Select "Monthly" frequency
- [ ] Day-of-month selector appears (1-31)
- [ ] Save creates routine with `{ "type": "MONTHLY", "dayOfMonth": N }`

### AC-7: Create routine - Every N days
- [ ] Select "Every N days" frequency
- [ ] Interval input appears
- [ ] Validates minimum >= 2
- [ ] Save creates routine with `{ "type": "EVERY_N_DAYS", "interval": N }`

### AC-8: Policy FIXED requires user selection
- [ ] Select "Fixed" policy
- [ ] User selector dropdown appears
- [ ] Cannot save without selecting user
- [ ] Selected user stored as fixedAssigneeId

### AC-9: Edit routine form pre-populated
- [ ] Click edit on existing routine
- [ ] Form opens with title filled
- [ ] Frequency preset selected correctly
- [ ] Policy selected correctly
- [ ] Save updates routine via PATCH

### AC-10: Delete routine with confirmation
- [ ] Delete button visible on each routine
- [ ] Click shows confirmation dialog
- [ ] Dialog text: "Delete routine? Pending tasks will remain."
- [ ] Cancel closes dialog
- [ ] Confirm deletes routine (soft delete)
- [ ] Routine removed from list

### AC-11: Validation errors shown
- [ ] Submit without title shows error
- [ ] Submit WEEKLY without days shows error
- [ ] Submit FIXED without user shows error
- [ ] Errors clear when corrected

### AC-12: Empty state
- [ ] No routines -> empty state shown
- [ ] Text: "No routines yet. Create your first routine..."
- [ ] Create button visible in empty state

---

## Technical Checklist

### Types & API
- [ ] `Routine` type added to `api.ts`
- [ ] `RoutineStatus` type added
- [ ] `RecurrenceRule` type added
- [ ] `AssignmentPolicy` type added
- [ ] `CreateRoutineRequest` type added
- [ ] `UpdateRoutineRequest` type added
- [ ] `getRoutines()` function added
- [ ] `getRoutine()` function added
- [ ] `createRoutine()` function added
- [ ] `updateRoutine()` function added
- [ ] `deleteRoutine()` function added

### Hooks
- [ ] `useRoutines.ts` created
- [ ] Returns `{ routines, isLoading, error, refetch }`
- [ ] Handles null householdId

### Components
- [ ] `RoutineRow.tsx` created
- [ ] `RoutineForm.tsx` created
- [ ] `RoutineStatusBadge.tsx` created
- [ ] `FrequencyDisplay.tsx` / `formatFrequency()` created
- [ ] `index.ts` barrel export created

### Routes & Navigation
- [ ] `Routines.tsx` route component created
- [ ] `Routines.css` styles created
- [ ] Route added to `index.tsx`
- [ ] Sidebar link added to `Sidebar.tsx`

---

## DoD Checklist

### Code Quality
- [ ] TypeScript strict mode passes
- [ ] ESLint passes (`npm run lint`)
- [ ] No unused imports/variables
- [ ] Consistent code style

### Build
- [ ] `npm run build` passes
- [ ] `npm run typecheck` passes
- [ ] No console errors in dev mode

### Testing
- [ ] Manual testing: create routine (all 4 frequencies)
- [ ] Manual testing: edit routine
- [ ] Manual testing: delete routine
- [ ] Manual testing: empty state
- [ ] Manual testing: validation errors

---

## Evidence

| Criterion | Evidence |
|-----------|----------|
| Route exists | Screenshot of `/routines` page |
| Create works | API call in network tab |
| Edit works | PATCH request in network tab |
| Delete works | DELETE request + removal from list |
| Build passes | `npm run build` output |
