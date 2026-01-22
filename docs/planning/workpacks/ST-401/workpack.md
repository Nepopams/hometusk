# Workpack: ST-401 — Household Selector & Empty State

## Sources of Truth
- Story: `docs/planning/epics/EP-005/stories/ST-401-household-selector.md`
- Epic: `docs/planning/epics/EP-005/epic.md`
- Initiative: `docs/planning/initiatives/INIT-2026Q1-household-lifecycle.md`
- OpenAPI: `docs/contracts/http/commands.openapi.yaml`
- DoR: `docs/_governance/dor.md`
- DoD: `docs/_governance/dod.md`

---

## Goal
Display user's households with selector and handle empty state gracefully.

## User Value
Users can see and switch between their households, or get guided to create one if none exist.

---

## In Scope
- HouseholdContext for managing selected household
- Household selector dropdown in header
- Empty state component with CTA
- Persist selection in sessionStorage

## Out of Scope
- Create household form (ST-402)
- Invite functionality (ST-403)
- Household settings/edit/delete

---

## UI Surfaces / Flows

### Flow 1: User with households
1. User logs in
2. `GET /users/me` returns households array
3. If stored selection valid → auto-select
4. Else select first household
5. Selector in header shows current household
6. User can click dropdown to switch

### Flow 2: User with no households
1. User logs in
2. `GET /users/me` returns empty households
3. Empty state displayed
4. User clicks "Create Household"
5. Navigate to `/households/new`

---

## Files to Change

| File | Action | Purpose |
|------|--------|---------|
| `clients/web/src/context/HouseholdContext.tsx` | CREATE | Household state management |
| `clients/web/src/components/HouseholdSelector.tsx` | CREATE | Dropdown component |
| `clients/web/src/components/EmptyHouseholdState.tsx` | CREATE | Empty state UI |
| `clients/web/src/components/Layout.tsx` | MODIFY | Add selector to header |
| `clients/web/src/routes/Households.tsx` | MODIFY | Use HouseholdContext |

---

## API Dependencies

| Endpoint | Method | Used For |
|----------|--------|----------|
| `GET /api/v1/users/me` | GET | Fetch households list (already used in AuthContext) |

**Response field:**
```typescript
// UserProfile.households: HouseholdSummary[]
interface HouseholdSummary {
  id: string;
  name: string;
  role: 'admin' | 'member';
}
```

---

## Data Contract Assumptions

- `GET /users/me` always returns `households` array (may be empty)
- Each household has `id`, `name`, `role`
- Roles: `admin` | `member` (display as badge)

---

## Implementation Plan

### Commit 1: Create HouseholdContext
- Create `HouseholdContext.tsx`
- Provide: `selectedHousehold`, `setSelectedHousehold`, `households`
- Persist selection to `sessionStorage`
- Restore on mount (validate against current user's households)

### Commit 2: Create HouseholdSelector
- Dropdown component
- Shows current household name + arrow
- Lists all households with role badges
- "Create Household" link at bottom
- Click to switch (updates context)

### Commit 3: Create EmptyHouseholdState
- "Welcome" message
- "Create your first household" CTA button
- Navigate to `/households/new`

### Commit 4: Integrate into Layout
- Add HouseholdSelector to header
- Conditionally show based on households.length > 0

---

## Verification Commands

```bash
# Build passes
cd clients/web && npm run build

# Lint passes
cd clients/web && npm run lint

# Dev server starts
cd clients/web && npm run dev
```

---

## Demo Scenario (Manual)

1. Login with user who has 2+ households
2. See selector in header with first household
3. Click selector → dropdown shows all households
4. Click different household → context switches
5. Refresh page → selection persists

6. Login with user who has 0 households
7. See empty state with "Create Household" button
8. Click button → navigate to create form

---

## Risks

| Risk | Mitigation |
|------|------------|
| Context not updating child routes | Use React context properly, children re-render on change |
| Selection lost on refresh | Persist to sessionStorage, restore on load |
| Invalid stored selection | Validate against current user's households on load |

---

## Rollback
- Remove new components
- Remove HouseholdContext provider from App
- Revert Layout changes

---

## Anti-Scope-Creep

DO NOT:
- Create household form (ST-402)
- Add invite button (ST-403)
- Add household settings
- Add leave household
