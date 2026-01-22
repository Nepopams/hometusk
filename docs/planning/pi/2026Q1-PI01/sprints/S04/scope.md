# Sprint S04 — Scope Detail

## Sources of Truth
- Sprint Plan: `docs/planning/pi/2026Q1-PI01/sprints/S04/sprint.md`
- Initiative: `docs/planning/initiatives/INIT-2026Q1-household-lifecycle.md`
- Epic: `docs/planning/epics/EP-005/epic.md`

---

## Committed Scope

### ST-401: Household Selector & Empty State
**Points:** 2
**Priority:** P1
**Status:** Ready
**Workpack:** `docs/planning/workpacks/ST-401/`

**What's included:**
- HouseholdContext for state management
- Household selector dropdown in header
- Empty state with "Create Household" CTA
- Persist selection in sessionStorage

**What's NOT included:**
- Create form (ST-402)
- Invite functionality (ST-403)

**DoR:** PASS
**Dependencies:** S03 complete

---

### ST-402: Create Household Form
**Points:** 2
**Priority:** P1
**Status:** Ready
**Workpack:** `docs/planning/workpacks/ST-402/`

**What's included:**
- Create household page/route
- Name input with validation (1-80 chars)
- POST /households integration
- Success: refresh profile, auto-select, redirect

**What's NOT included:**
- Household settings/edit
- Temporary household flag

**DoR:** PASS
**Dependencies:** ST-401

---

### ST-403: Create Invite & Share
**Points:** 2
**Priority:** P1
**Status:** Ready
**Workpack:** `docs/planning/workpacks/ST-403/`

**What's included:**
- "Invite Member" button
- POST /invites integration
- Display token and link
- Copy to clipboard functionality
- Expiry info (7 days)

**What's NOT included:**
- Invite revoke
- Invite history
- Email/SMS delivery

**DoR:** PASS
**Dependencies:** ST-401, ST-402

---

### ST-404: Accept Invite Flow
**Points:** 3
**Priority:** P1
**Status:** Ready
**Workpack:** `docs/planning/workpacks/ST-404/`

**What's included:**
- Accept invite page (/invite route)
- Read token from URL query
- POST /invites/accept integration
- Handle 200, 404, 410 status codes
- Auth redirect flow (login if not authenticated)
- Success: join household, redirect

**What's NOT included:**
- Manual token input
- Invite preview
- Decline invite

**DoR:** PASS
**Dependencies:** ST-401, S03 (auth redirect)

---

### ST-405: Members List View
**Points:** 2
**Priority:** P2
**Status:** Ready
**Workpack:** `docs/planning/workpacks/ST-405/`

**What's included:**
- Members page/route
- GET /members integration
- Display: name, email, role, joined date
- "Invite Member" button

**What's NOT included:**
- Edit member role
- Remove member
- Leave household

**DoR:** PASS
**Dependencies:** ST-401, ST-403

---

## Out of Scope (Explicit)

### Deferred to NEXT Increment
- Temporary households (kind, endsAt, archive)
- Invite revoke functionality
- Leave household
- Invite history/list

### Deferred to LATER Increment
- Role management (change admin/member)
- Member removal
- Membership expiry

### Never in Household Scope
- New backend microservices
- RBAC expansion
- Email/SMS invite delivery

---

## Acceptance Criteria Summary

**Sprint succeeds if:**
1. User can see household selector (multi-household) or empty state (no households)
2. User can create new household
3. User can generate invite link and copy it
4. Invited user can accept and join household
5. Members list shows all household members
6. No cross-household data leaks
7. npm run build/lint pass

**Sprint fails if:**
- Cannot create household
- Invite flow broken (create or accept)
- Cross-household data leak
- Critical functionality missing

---

## Readiness Notes

**All committed stories:**
- Have workpacks with implementation plans
- Have checklists
- Dependencies are sequential and clear

**Key decisions documented:**
- ADR-010: Invite token semantics (404/410/200)
- Anti-IDOR: Accept request contains only token

**Risks mitigated:**
- Auth integration: S03 handles this
- Invite UX: clear copy button + error messages
- Scope creep: explicit Out of Scope list

**Human gates:**
- Gate B: approve committed scope (this sprint)
- Gate C: approve PLAN before APPLY (each story)
- Gate D: approve merge (each story)

---

## Story Dependency Graph

```
ST-401 (Household Selector)
    │
    ├── ST-402 (Create Household)
    │       │
    │       └── ST-403 (Create Invite)
    │               │
    │               └── ST-405 (Members List)
    │
    └── ST-404 (Accept Invite)
```

**Critical path:** ST-401 → ST-402 → ST-403 → ST-405
**Parallel:** ST-404 can start after ST-401
