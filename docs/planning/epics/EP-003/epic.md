# Epic: EP-003 — Web Foundation (NOW increment)

## Sources of Truth
- Initiative: `docs/planning/initiatives/INIT-2026Q1-web-client.md`
- Product Goal: `docs/planning/strategy/product-goal.md`
- Roadmap: `docs/planning/strategy/roadmap.md`
- MVP Exit Review: `docs/planning/releases/reviews/MVP-exit-review.md`
- DoR: `docs/_governance/dor.md`
- DoD: `docs/_governance/dod.md`
- OpenAPI (authoritative REST API spec for web MVP): `docs/contracts/http/commands.openapi.yaml`
- API Coverage: `docs/mvp/api-coverage.md`

---

## Status
**Draft** (pending Gate A approval)

## Initiative Alignment
This epic implements the **NOW** increment of INIT-2026Q1-web-client:
- Web shell (routing/layout), environment config, build/deploy baseline
- Auth/session + household selector
- Tasks views (list/detail), zones navigation
- Minimal filters/search (status/assignee/zone)
- Notifications inbox + mark read

---

## Epic Goal
Deliver a working web client that allows a user to:
1. Authenticate and select a household
2. View tasks with filters (status/assignee/zone)
3. Navigate zones
4. View notifications and mark as read

This is the **minimal E2E user journey** for web.

---

## In Scope

### Web Foundation
- Project setup: React/TypeScript + Vite (or similar modern stack)
- Build/lint/test commands (`npm ci`, `npm run dev`, `npm run build`)
- Environment config (API base URL, auth config)
- Routing (react-router or equivalent)
- Layout shell (header, sidebar, content area)

### Auth & Household
- Token-based auth (dev: token paste; target: Keycloak OIDC redirect)
- `GET /api/v1/users/me` integration
- Household selector (single-household focus, multi-household ready)

### Tasks
- `GET /api/v1/households/{id}/tasks` with filters
- Task list view with columns: title, status, assignee, zone, deadline
- Task detail view (`GET /api/v1/households/{id}/tasks/{taskId}`)
- Query params: `status`, `assigneeId`, `zoneId`

### Zones
- `GET /api/v1/households/{id}/zones`
- Zone list in navigation/sidebar
- Zone filter for tasks

### Notifications
- `GET /api/v1/households/{id}/notifications`
- `POST /api/v1/notifications/{notificationId}/read`
- Notifications inbox with unread indicator
- Mark as read (idempotent)

### Error Handling
- HTTP error display (401, 403, 404, 410, 500)
- Loading states
- Retry on transient errors (where safe)

---

## Out of Scope (explicit)

- **No new backend code** — web consumes existing API
- **No new microservices** — single monolith stays
- **No RBAC expansion** — existing household membership is sufficient
- **No mobile-first design** — desktop-first per initiative
- **No domain write operations** (task creation/editing deferred to NEXT increment) — except idempotent notification mark-as-read
- **No command box** — deferred to NEXT increment
- **No analytics dashboard** — deferred to LATER increment
- **No agreements view** — deferred to NEXT/LATER increment

---

## Stories

| ID | Title | Status | Priority |
|----|-------|--------|----------|
| ST-201 | Web Foundation (Project Setup & Build) | Ready | P1 |
| ST-202 | Auth Integration + Household Selector | Ready | P1 |
| ST-203 | Tasks List & Filters | Ready | P1 |
| ST-204 | Task Detail View | Draft | P2 |
| ST-205 | Zones Navigation | Draft | P2 |
| ST-206 | Notifications Inbox | Draft | P2 |

### Sprint Mapping
- **Sprint S02 (committed):** ST-201, ST-202, ST-203
- **Sprint S03 (planned):** ST-204, ST-205, ST-206

---

## Dependencies

| Dependency | Type | Status | Notes |
|------------|------|--------|-------|
| Backend API | Internal | Ready | MVP closed, endpoints stable |
| Keycloak | External | Ready | Local dev instance available |
| OpenAPI contract | Internal | Ready | `docs/contracts/http/commands.openapi.yaml` |

---

## Risks

| Risk | Impact | Mitigation |
|------|--------|------------|
| Scope creep to writes | Delays NOW increment | Strict Out of Scope enforcement |
| CORS/auth issues | Blocks integration | Test early with real backend |
| Contract drift | Breaks web | Contract-first + OpenAPI as source of truth |

---

## Exit Criteria (NOW delivered)

From initiative INIT-2026Q1-web-client:

1. User can access web and authenticate (dev token paste acceptable)
2. User can see household context (zones list, tasks list with filters)
3. User can complete E2E workflow: authenticate → see tasks/zones
4. Notifications inbox: GET + mark read works
5. No cross-household leaks (403/404 on unauthorized access)
6. OpenAPI remains source of truth

---

## Flags

| Flag | Value | Notes |
|------|-------|-------|
| contract_impact | no | Consuming existing API |
| adr_needed | no | No architectural decisions needed |
| diagrams_needed | maybe | Simple component diagram if helpful |

---

## Related Artifacts

| Artifact | Path |
|----------|------|
| Initiative | `docs/planning/initiatives/INIT-2026Q1-web-client.md` |
| OpenAPI | `docs/contracts/http/commands.openapi.yaml` |
| API Coverage | `docs/mvp/api-coverage.md` |
| Service Catalog | `docs/architecture/service-catalog.md` |
