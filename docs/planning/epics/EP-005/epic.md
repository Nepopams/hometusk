# Epic: EP-005 — Household Lifecycle (Create/Join/Invites)

## Sources of Truth
- Initiative: `docs/planning/initiatives/INIT-2026Q1-household-lifecycle.md`
- Product Goal: `docs/planning/strategy/product-goal.md`
- Roadmap: `docs/planning/strategy/roadmap.md`
- DoR: `docs/_governance/dor.md`
- DoD: `docs/_governance/dod.md`
- OpenAPI: `docs/contracts/http/commands.openapi.yaml`
- ADR-010: `docs/architecture/decisions/010-household-invites.md`

---

## Status
**Done** — All stories implemented, reviewed, fixes applied (2026-01-23)

## Initiative Alignment
This epic implements the **NOW** increment of INIT-2026Q1-household-lifecycle:
- Household selector + empty state handling
- Create household form
- Invite creation & sharing (copy link/token)
- Accept invite flow
- Members view (read-only)

---

## Epic Goal
Enable a user to:
1. See their households and switch between them
2. Create a new household
3. Invite another user via shareable link/token
4. Accept an invite and join a household
5. View household members (read-only)

This completes the "household team formation" journey.

---

## In Scope

### Household Selector (ST-401)
- Display list of user's households from `GET /users/me` response
- Household switcher in header/sidebar
- Empty state: "No households yet" with CTA to create
- Store selected household in context/state

### Create Household (ST-402)
- Create household form (name input)
- `POST /households` integration
- Success: refresh user profile, select new household
- Validation: name required, 1-80 chars

### Invite Creation (ST-403)
- "Invite member" button in household context
- `POST /households/{householdId}/invites` integration
- Display invite token/link
- Copy to clipboard functionality
- Show expiry (7 days default)

### Accept Invite (ST-404)
- Accept invite page (`/invite?token=...` route)
- `POST /invites/accept` integration
- Handle all status codes (200, 404, 410)
- Success: redirect to household
- Error: clear messages for invalid/expired/redeemed

### Members View (ST-405)
- Members list page/section
- `GET /households/{householdId}/members` integration
- Display: name, email, role, joined date
- Read-only (no edit/remove in NOW)

---

## Out of Scope (explicit)

### Deferred to NEXT
- **Temporary households** (kind: TEMPORARY, endsAt, archive)
- **Invite revoke** UI
- **Leave household** functionality
- **Transfer ownership**

### Deferred to LATER
- **Role management** (изменение admin↔member) — роль отображаем как строку из API, но управление ролями — LATER
- **Membership expiry** (temporary membership)
- **Merge/transfer household**

### Never in Scope
- New backend microservices
- RBAC expansion
- Email/SMS invite delivery

---

## Security & Data Boundaries

### Membership Enforcement
- All household-scoped endpoints require membership (403 if not member)
- `GET /users/me` returns only user's own households
- Household context must be validated before any operation

### Anti-IDOR (ADR-010)
- Accept invite: request contains **only** `inviteToken` (no householdId)
- Backend resolves household from token
- Prevents guessing household IDs

### Status Codes (Invites)
| Code | Meaning | UI Handling |
|------|---------|-------------|
| 200 | Success OR already member (no-op) | Redirect to household |
| 401 | Not authenticated | Redirect to login |
| 403 | Not a member of household | Access denied message |
| 404 | Invalid token | "Invalid invite link" |
| 410 | Expired/redeemed/revoked | "Invite has expired or already used" |

---

## API Dependencies (from OpenAPI)

| Endpoint | Method | Used By |
|----------|--------|---------|
| `GET /api/v1/users/me` | GET | ST-401 (households list) |
| `POST /api/v1/households` | POST | ST-402 (create) |
| `POST /api/v1/households/{householdId}/invites` | POST | ST-403 (create invite) |
| `POST /api/v1/invites/accept` | POST | ST-404 (accept) |
| `GET /api/v1/households/{householdId}/members` | GET | ST-405 (list members) |

**Contract gaps:** None identified. All required endpoints exist in OpenAPI.

---

## Stories

| ID | Title | Status | Priority | Points |
|----|-------|--------|----------|--------|
| ST-401 | Household Selector & Empty State | Ready | P1 | 2 |
| ST-402 | Create Household Form | Ready | P1 | 2 |
| ST-403 | Create Invite & Share | Ready | P1 | 2 |
| ST-404 | Accept Invite Flow | Ready | P1 | 3 |
| ST-405 | Members List View | Ready | P2 | 2 |

**Total:** 11 points

### Sprint Mapping
- **Sprint S04 (committed):** ST-401, ST-402, ST-403, ST-404, ST-405

---

## Dependencies

| Dependency | Type | Status | Notes |
|------------|------|--------|-------|
| S03 (Registration/Sign-in) | Internal | Done | Auth/session working |
| Backend API | Internal | Ready | All endpoints exist |
| ADR-010 (Invites) | Decision | Accepted | Token semantics defined |

---

## Risks

| Risk | Impact | Mitigation |
|------|--------|------------|
| Scope creep to NEXT features | Delays NOW | Strict Out of Scope enforcement |
| Invite link UX confusion | Poor conversion | Clear copy button + instructions |
| Empty state not discoverable | Users stuck | Prominent CTA to create household |
| Race condition on accept | Data integrity | Backend handles (ADR-010) |

---

## Exit Criteria (NOW delivered)

From initiative INIT-2026Q1-household-lifecycle:

1. User can create household in web
2. User can see list of their households and switch
3. User can generate invite link/token
4. Invited user can accept and join
5. Members list is visible
6. No cross-household leaks
7. Docs/contracts up to date

---

## Flags

| Flag | Value | Notes |
|------|-------|-------|
| contract_impact | no | Consuming existing API |
| adr_needed | no | ADR-010 covers invites |
| diagrams_needed | no | No structural changes |
| security_sensitive | yes | Membership boundaries |

---

## Related Artifacts

| Artifact | Path |
|----------|------|
| Initiative | `docs/planning/initiatives/INIT-2026Q1-household-lifecycle.md` |
| OpenAPI | `docs/contracts/http/commands.openapi.yaml` |
| ADR-010 | `docs/architecture/decisions/010-household-invites.md` |
| Service Catalog | `docs/architecture/service-catalog.md` |

---

## Files Consulted
- `docs/planning/initiatives/INIT-2026Q1-household-lifecycle.md`
- `docs/contracts/http/commands.openapi.yaml`
- `docs/architecture/decisions/010-household-invites.md`
- `docs/_governance/dor.md`
- `docs/_governance/dod.md`
