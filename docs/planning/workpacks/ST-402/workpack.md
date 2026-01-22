# Workpack: ST-402 — Create Household Form

## Sources of Truth
- Story: `docs/planning/epics/EP-005/stories/ST-402-create-household.md`
- Epic: `docs/planning/epics/EP-005/epic.md`
- Initiative: `docs/planning/initiatives/INIT-2026Q1-household-lifecycle.md`
- OpenAPI: `docs/contracts/http/commands.openapi.yaml`
- DoR: `docs/_governance/dor.md`
- DoD: `docs/_governance/dod.md`

---

## Goal
Allow user to create a new household with a name.

## User Value
New users can create their household to start managing tasks.

---

## In Scope
- Create household page/route
- Name input with validation
- `POST /households` API call
- Success: refresh profile, auto-select, redirect
- Error handling

## Out of Scope
- Household settings/edit
- Temporary household flag
- Delete household

---

## UI Surfaces / Flows

### Flow: Create Household
1. User clicks "Create Household" (from empty state or selector)
2. Navigate to `/households/new`
3. Form with name input displayed
4. User enters name (1-80 chars)
5. User clicks "Create"
6. Loading state
7. `POST /households` called
8. On success:
   - Refetch `GET /users/me`
   - Auto-select new household in context
   - Navigate to household dashboard
9. On error:
   - Display error message
   - User can correct and retry

---

## Files to Change

| File | Action | Purpose |
|------|--------|---------|
| `clients/web/src/routes/CreateHousehold.tsx` | CREATE | Create form page |
| `clients/web/src/routes/index.tsx` | MODIFY | Add `/households/new` route |
| `clients/web/src/lib/api/households.ts` | CREATE/MODIFY | Add createHousehold function |

---

## API Dependencies

| Endpoint | Method | Request | Response |
|----------|--------|---------|----------|
| `POST /api/v1/households` | POST | `{ name: string }` | `Household` |
| `GET /api/v1/users/me` | GET | — | `UserProfile` (to refresh) |

**Request validation:**
- `name`: required, 1-80 chars, trimmed, non-blank

**Response (201 Created):**
```typescript
interface Household {
  id: string;
  name: string;
  createdAt: string;
}
```

**Error responses:**
- 400: `{ errorCode, message, validationErrors? }` — name validation failed
- 401: Not authenticated

---

## Data Contract Assumptions

- Backend trims whitespace from name
- Backend returns created household with ID
- User automatically becomes `admin` of new household

---

## Implementation Plan

### Commit 1: Create API function
- Add `createHousehold(name: string)` to `lib/api/households.ts`
- Handle 400/401 errors

### Commit 2: Create form page
- `CreateHousehold.tsx` with:
  - Name input
  - Client-side validation (required, max 80)
  - Submit button (disabled until valid)
  - Cancel button
  - Loading state
  - Error display

### Commit 3: Wire up success flow
- On success:
  - Call `refetchUser()` from AuthContext
  - Update HouseholdContext with new household
  - Navigate to `/households` or dashboard

### Commit 4: Add route
- Add `/households/new` to router

---

## Verification Commands

```bash
cd clients/web && npm run build
cd clients/web && npm run lint
```

---

## Demo Scenario (Manual)

1. Login (with or without existing households)
2. Navigate to create household
3. Enter valid name "My Home"
4. Click Create
5. See loading state
6. Redirected to dashboard
7. New household appears in selector

8. Try empty name → validation error
9. Try >80 char name → validation error

---

## Risks

| Risk | Mitigation |
|------|------------|
| Profile not refreshing after create | Explicit refetch call |
| Context not updating | Force context update after refetch |
| Network error | Display retry option |

---

## Rollback
- Remove CreateHousehold.tsx
- Remove route
- Remove API function

---

## Anti-Scope-Creep

DO NOT:
- Add household settings
- Add temporary household option
- Add household description field
- Add invite flow here
