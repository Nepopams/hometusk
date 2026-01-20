# ST-202 PLAN Prompt

**Mode:** PLAN ONLY — NO EDITS, NO COMMANDS

---

## Context

You are implementing ST-202: Auth Integration + Household Selector for the HomeTusk web client.

**Read these files first (mandatory):**
- `docs/planning/workpacks/ST-202/workpack.md` — implementation plan
- `docs/planning/epics/EP-003/stories/ST-202-auth-household.md` — story spec
- `docs/contracts/http/commands.openapi.yaml` — API contract (UserProfile, HouseholdSummary, GET /users/me)

**Prerequisite:**
- ST-201 completed (web foundation exists at `clients/web/`)

---

## Your Task

Create a detailed implementation plan for auth integration and household selector.

**Output format:** Markdown plan with:
1. Types to create (from OpenAPI)
2. Auth context structure
3. API client updates
4. Component implementations
5. Route changes
6. Auth flow diagram
7. Verification steps

---

## Constraints (CRITICAL)

1. **NO FILE EDITS** — This is a plan-only prompt
2. **NO COMMAND EXECUTION** — Do not run npm, etc.
3. **Work within:** `clients/web/` (already exists from ST-201)
4. **Auth mode:** Dev only (token paste), no Keycloak OIDC
5. **API endpoint:** GET /api/v1/users/me

---

## Acceptance Criteria to Plan For

- AC1: useAuth() hook provides auth state
- AC2: Dev mode token paste works
- AC3: Authorization header attached to requests
- AC4: GET /users/me integration (profile fetch, 401 handling)
- AC5: Household selector with list
- AC6: Single household auto-select
- AC7: Protected routes redirect

---

## Expected Plan Structure

```markdown
# ST-202 Implementation Plan

## 1. Types (from OpenAPI)
interface UserProfile { ... }
interface HouseholdSummary { ... }
interface AuthContext { ... }

## 2. File Structure
src/
├── context/
│   └── AuthContext.tsx
├── hooks/
│   └── useAuth.ts
├── lib/
│   ├── api.ts (update)
│   └── errors.ts
├── types/
│   └── api.ts
├── routes/
│   ├── Login.tsx (update)
│   └── HouseholdSelector.tsx
└── components/
    ├── ProtectedRoute.tsx
    └── HouseholdCard.tsx

## 3. Auth Context Design
[state, actions, persistence]

## 4. API Client Updates
[auth header, error handling]

## 5. Component Details
### Login.tsx
[token input, submit handler]

### HouseholdSelector.tsx
[list, click handler]

### ProtectedRoute.tsx
[redirect logic]

## 6. Route Changes
[new routes, protected wrappers]

## 7. Auth Flow
[sequence diagram or steps]

## 8. Verification
[manual test steps]
```

---

## STOP-THE-LINE

If you encounter:
- Unclear API contract
- Missing ST-201 foundation
- Architecture decisions needed

**STOP and ask** — do not assume.
