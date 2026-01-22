# Story: ST-404 — Accept Invite Flow

## Sources of Truth
- Epic: `docs/planning/epics/EP-005/epic.md`
- Initiative: `docs/planning/initiatives/INIT-2026Q1-household-lifecycle.md`
- OpenAPI: `docs/contracts/http/commands.openapi.yaml`
- ADR-010: `docs/architecture/decisions/010-household-invites.md`
- DoR: `docs/_governance/dor.md`
- DoD: `docs/_governance/dod.md`

---

## Summary
Allow user to accept an invite via link or token input and join a household.

## User Value
As an invited user, I want to join my family's household so that I can collaborate on tasks.

---

## In Scope
- Accept invite page: `/invite` route
- Read token from URL query param: `?token=hti_xxx`
- Optional: manual token input field (fallback)
- `POST /invites/accept` integration
- Handle all status codes per ADR-010
- Success: refresh profile, select new household, redirect to dashboard
- Error states with clear messages

## Out of Scope
- Unauthenticated accept (must login first)
- Invite preview (show household name before accepting)
- Decline invite functionality

---

## Acceptance Criteria

```gherkin
Given user opens /invite?token=hti_xxx
And user is authenticated
When page loads
Then accept invite is attempted automatically
And loading state is shown

Given invite token is valid and active
When accept succeeds (200)
Then user profile is refreshed
And new household appears in selector
And user is redirected to household dashboard
And success message is shown

Given user is already a member (200, no-op)
When accept returns success
Then user is redirected to household
And message "You're already a member" is shown

Given invite token is invalid (404)
When accept fails
Then error message "Invalid invite link" is shown
And "Back to Home" link is available

Given invite is expired/redeemed/revoked (410)
When accept fails
Then error message "This invite has expired or was already used" is shown
And "Back to Home" link is available

Given user is not authenticated (401)
When they open /invite?token=xxx
Then they are redirected to login
And after login, they return to /invite?token=xxx
And accept flow continues
```

---

## UI Specification

**Accept page (loading):**
```
+-----------------------------+
|                             |
|   Accepting invite...       |
|   [spinner]                 |
|                             |
+-----------------------------+
```

**Accept page (error):**
```
+-----------------------------+
|                             |
|   ⚠ Invalid invite link     |
|                             |
|   This invite link is not   |
|   valid. Please ask for a   |
|   new invite.               |
|                             |
|   [Back to Home]            |
+-----------------------------+
```

**Accept page (expired):**
```
+-----------------------------+
|                             |
|   ⚠ Invite expired          |
|                             |
|   This invite has expired   |
|   or was already used.      |
|                             |
|   [Back to Home]            |
+-----------------------------+
```

---

## API Dependencies

| Endpoint | Method | Request | Response |
|----------|--------|---------|----------|
| `POST /api/v1/invites/accept` | POST | `{ inviteToken: string }` | `AcceptInviteResponse` |

**AcceptInviteRequest schema:**
```typescript
interface AcceptInviteRequest {
  inviteToken: string;  // hti_xxx format
}
```

**AcceptInviteResponse (200):**
```typescript
interface AcceptInviteResponse {
  membership: {
    id: string;
    role: 'admin' | 'member';
    joinedAt: string;
  };
  household: {
    id: string;
    name: string;
    createdAt: string;
  };
}
```

**Status codes (ADR-010):**
| Code | Meaning | UI Message |
|------|---------|------------|
| 200 | Success or already member | Redirect to household |
| 404 | Invalid token | "Invalid invite link" |
| 410 | Expired/redeemed/revoked | "Invite expired or already used" |
| 401 | Not authenticated | Redirect to login (preserve token) |

---

## Technical Notes

**Files to create/modify:**
- `clients/web/src/routes/AcceptInvite.tsx` — create
- `clients/web/src/routes/index.tsx` — add `/invite` route
- `clients/web/src/lib/api/invites.ts` — add acceptInvite function

**Token extraction:**
```typescript
const searchParams = new URLSearchParams(location.search);
const token = searchParams.get('token');
```

**Preserve token through login:**
- Store intended redirect in sessionStorage before login redirect
- After login, check for pending invite and process

**Anti-IDOR (ADR-010):**
- Request contains **only** `inviteToken`
- NO householdId in request (backend resolves from token)

---

## Test Strategy

**Manual tests:**
- Open valid invite link → joins household
- Open invalid token → "Invalid invite" error
- Open expired invite → "Expired" error
- Open invite when already member → success (no-op)
- Open invite when not logged in → login → accept

---

## Flags

| Flag | Value |
|------|-------|
| contract_impact | no |
| adr_needed | no |
| security_sensitive | yes (token handling, anti-IDOR) |

---

## Dependencies
- ST-401 (HouseholdContext to select new household)
- S03 complete (auth redirect handling)

## Points
3 (complex flow + error handling + auth integration)

## Priority
P1

---

## Files Consulted
- `docs/contracts/http/commands.openapi.yaml` (AcceptInviteRequest, AcceptInviteResponse)
- `docs/architecture/decisions/010-household-invites.md`
