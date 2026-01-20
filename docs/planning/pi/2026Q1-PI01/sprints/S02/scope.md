# Sprint S02 — Scope Detail

## Sources of Truth
- Sprint Plan: `docs/planning/pi/2026Q1-PI01/sprints/S02/sprint.md`
- Initiative: `docs/planning/initiatives/INIT-2026Q1-web-client.md`
- Epic: `docs/planning/epics/EP-003/epic.md`

---

## Committed Scope

### ST-201: Web Foundation (Project Setup & Build)
**Points:** 3
**Priority:** P1
**Status:** Ready
**Workpack:** `docs/planning/workpacks/ST-201/`

**What's included:**
- React 18 + TypeScript + Vite project at `clients/web/`
- Routing (react-router-dom v6)
- Layout shell (header, sidebar, content area)
- Build commands: `npm ci`, `npm run dev`, `npm run build`, `npm run lint`
- Environment config (.env.example)
- README with setup instructions

**What's NOT included:**
- No backend integration
- No real auth (just placeholder)
- No styling perfection

**DoR:** ✅ PASS
**Dependencies:** None (first story)

---

### ST-202: Auth Integration + Household Selector
**Points:** 3
**Priority:** P1
**Status:** Ready
**Workpack:** `docs/planning/workpacks/ST-202/`

**What's included:**
- Auth context (login/logout/selectHousehold)
- Dev mode auth (token paste)
- GET /users/me integration
- Household selector (list from user.households)
- Single-household auto-select
- Protected routes

**What's NOT included:**
- No Keycloak OIDC redirect (defer to target mode)
- No token refresh
- No user profile editing

**DoR:** ✅ PASS
**Dependencies:** ST-201 (must complete first)

---

### ST-203: Tasks List & Filters
**Points:** 3
**Priority:** P1
**Status:** Ready
**Workpack:** `docs/planning/workpacks/ST-203/`

**What's included:**
- GET /households/{id}/tasks integration
- Task list display (title, status, assignee, zone, deadline)
- Status filter (All, Open, In Progress, Done, Cancelled)
- Assignee filter (All + household members)
- Zone filter (All + household zones)
- Combined filters
- Empty/loading/error states
- Row click → navigate to detail (placeholder OK)

**What's NOT included:**
- No task creation/editing
- No pagination (defer until needed)
- No sorting (defer until needed)

**DoR:** ✅ PASS
**Dependencies:** ST-202 (must complete first)

---

## Stretch Scope

### ST-204: Task Detail View
**Points:** 2
**Priority:** P2
**Status:** Draft

**What would be included:**
- GET /households/{id}/tasks/{taskId}
- Task detail page with all fields
- Linked shopping items display
- Back button

**Dependencies:** ST-203

---

### ST-205: Zones Navigation
**Points:** 2
**Priority:** P2
**Status:** Draft

**What would be included:**
- Zones list in sidebar
- Zone detail view (tasks in zone)

**Dependencies:** ST-203

---

### ST-206: Notifications Inbox
**Points:** 2
**Priority:** P2
**Status:** Draft

**What would be included:**
- GET /households/{id}/notifications
- POST /notifications/{notificationId}/read
- Notification list with unread indicator
- Mark as read (idempotent)

**Dependencies:** ST-202

---

## Out of Scope (Explicit)

### Deferred to Sprint S03 / NEXT Increment
- Task creation/editing UI
- Command box (text command input)
- Complete task action
- Assign/reassign task
- Keycloak OIDC integration

### Deferred to LATER Increment
- Analytics dashboard
- Agreements view
- Weekly check-in reports
- Mobile-first design
- PWA features

### Never in Web Client Scope
- New backend microservices
- RBAC expansion beyond household membership
- Backend logic changes

---

## Acceptance Criteria Summary

**Sprint succeeds if:**
1. User can login (paste JWT token)
2. User can see their households
3. User can select a household
4. User can see tasks list
5. User can filter tasks by status/assignee/zone
6. All filters work (individually and combined)
7. npm run build/lint pass
8. No cross-household data leaks

**Sprint fails if:**
- Cannot login
- Cannot see tasks
- Filters broken
- Build fails
- Contract violations

---

## Readiness Notes

**All committed stories:**
- Have workpacks with implementation plans
- Have prompt packs (PLAN/APPLY/REVIEW)
- Have DoD checklists
- Have no known blockers
- Dependencies are sequential and clear

**Risks mitigated:**
- CORS: test early with real backend
- Contract drift: OpenAPI as source of truth
- Scope creep: explicit Out of Scope list

**Human gates:**
- Gate B: approve committed scope (this sprint)
- Gate C: approve PLAN before APPLY (each story)
- Gate D: approve merge (each story)
